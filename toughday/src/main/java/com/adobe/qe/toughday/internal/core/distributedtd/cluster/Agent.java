package com.adobe.qe.toughday.internal.core.distributedtd.cluster;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.internal.core.config.PhaseParams;
import com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.RedistributionInstructionsProcessor;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.*;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;


import static com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils.HTTP_REQUEST_RETRIES;
import static com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils.URL_PREFIX;
import static com.adobe.qe.toughday.internal.core.engine.Engine.installToughdayContentPackage;
import static spark.Spark.*;

/**
 * Agent component for running TD distributed.
 */
public class Agent {
    private static final String PORT = "4567";
    protected static final Logger LOG = LogManager.getLogger(Engine.class);
    private final ExecutorService tdExecutorService = Executors.newFixedThreadPool(1);
    private final ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);

    public enum Status {
        IDLE, /* Agent is waiting to receive tasks from the driver */
        BUILDING_CONFIG, /* Agent has received one task from the driver and it is building the TD configuration */
        RUNNING /* Agent is running TD tests */
    }

    // available routes
    private static final String INSTALL_SAMPLE_CONTENT_PATH = "/sampleContent";
    private static final String SUBMIT_TASK_PATH = "/submitTask";
    private static final String FINISH_PATH = "/finish";
    private static final String HEARTBEAT_PATH = "/heartbeat";
    private static final String REBALANCE_PATH = "/rebalance";
    private static final String HEALTH_PATH = "/health";
    private static final String GET_STATUS_PATH = "/status";

    private HttpUtils httpUtils = new HttpUtils();
    private Status status = Status.IDLE;
    private final ReentrantReadWriteLock statusLock = new ReentrantReadWriteLock();

    /**
     * Returns the http URL that the driver should use for finished the execution of the agent.
     * @param agentItAddress : the ip address that uniquely identifies the agent in the cluster
     */
    public static String getFinishPath(String agentItAddress) {
        return URL_PREFIX + agentItAddress + ":" + PORT + FINISH_PATH;
    }

    /**
     * Returns the http URL that the driver should use for sending heartbeat messages to the agent.
     * @param agentIpAddress : the ip address that uniquely identifies the agent in the cluster
     */
    public static String getHeartbeatPath(String agentIpAddress) {
        return URL_PREFIX + agentIpAddress + ":" + PORT + HEARTBEAT_PATH;
    }

    /**
     * Returns the http URL that the driver should use for sending a task to the agent.
     * @param agentIpAdress : the ip address that uniquely identifies the agent in the cluster
     */
    public static String getSubmissionTaskPath(String agentIpAdress) {
        return URL_PREFIX + agentIpAdress + ":" + PORT + SUBMIT_TASK_PATH;
    }

    /**
     * Returns the http URL that the driver should use for sending redistribution instructions to the agent.
     * @param agentIpAddress : the ip address that uniquely identifies the agent in the cluster
     */
    public static String getRebalancePath(String agentIpAddress) {
        return URL_PREFIX + agentIpAddress + ":" +  PORT + REBALANCE_PATH;
    }

    /**
     * Returns the http URL that the driver should use for requiring the agent to install the TD sample content package.
     * @param agentIpAddress : the ip address that uniquely identifies the agent in the cluster
     */
    public static String getInstallSampleContentPath(String agentIpAddress) {
        return URL_PREFIX + agentIpAddress + ":" + PORT + INSTALL_SAMPLE_CONTENT_PATH;
    }

    /**
     * Returns the http URL that the driver should use for getting the current status of the agent
     * @param agentIpAddress : the ip address that uniquely identifies the agent in the cluster
     */
    public static String getGetStatusPath(String agentIpAddress) {
        return URL_PREFIX + agentIpAddress + ":" + PORT + GET_STATUS_PATH;
    }

    private void updateStatus(Status newStatus) {
        this.statusLock.writeLock().lock();
        this.status = newStatus;
        this.statusLock.writeLock().unlock();
    }

    private Engine engine;
    private static String ipAddress;

    static {
        try {
            ipAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            System.exit(-1);
        }
    }

    private final RedistributionInstructionsProcessor redistributionInstructionsProcessor = new RedistributionInstructionsProcessor();

    public static boolean announcePhaseCompletion() {
        /* the current master might be dead so we should retry this for a certain amount of time before shutting
         * down the execution.
         */
        HttpUtils httpUtils = new HttpUtils();
        HttpResponse response = null;
        long duration = GlobalArgs.parseDurationToSeconds("60s");

        while (duration > 0 && response == null) {
            response = httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, ipAddress,
                    Driver.getPhaseFinishedByAgentPath(), HTTP_REQUEST_RETRIES);

            try {
                Thread.sleep(10 * 1000L); // try again in 10 seconds
            } catch (InterruptedException e) {
                // skip this since this thread is not interrupted by anyone
            } finally {
                duration -= GlobalArgs.parseDurationToSeconds("10s");
            }
        }

        return response != null;
    }

    public void start() {
        register();

        /* expose http endpoint to allow the driver to ask for ToughDay sample content package to be installed */
        post(INSTALL_SAMPLE_CONTENT_PATH, ((request, response) -> {
            String yamlConfig = request.body();
            Configuration configuration = new Configuration(yamlConfig);

            this.tdExecutorService.submit(() -> {
                boolean installed = true;

                try {
                    installToughdayContentPackage(configuration.getGlobalArgs());
                } catch (Exception e) {
                    installed = false;
                    LOG.error("Error encountered when installing TD sample content", e);
                }

                HttpResponse driverResponse = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, String.valueOf(installed),
                        Driver.getSampleContentAckPath(), HTTP_REQUEST_RETRIES);
                if (driverResponse == null) {
                    LOG.error("Agent " + ipAddress + " could not announce the driver that Toughday sample content" +
                            " package was installed.");
                    System.exit(-1);
                }

            });

            // clear all phases
            PhaseParams.namedPhases.clear();
            return "";
        }));

        /* Expose http endpoint for receiving ToughDay execution request from the driver */
        post(SUBMIT_TASK_PATH, ((request, response) ->  {
            this.statusLock.readLock().lock();
            if (this.status != Status.IDLE) {
                LOG.info("Agent is currently executing a different task. Request will be ignored.");
                this.statusLock.readLock().unlock();
                return "";
            }
            this.statusLock.readLock().unlock();

            // update current status
            updateStatus(Status.BUILDING_CONFIG);
            String yamlTask = request.body();
            LOG.info("[Agent] Received task:\n" + yamlTask);

            Configuration configuration = new Configuration(yamlTask);
            this.engine = new Engine(configuration);
            configuration.getDistributedConfig().setAgent("true");

            tdExecutorService.submit(() ->  {
                updateStatus(Status.RUNNING);
                this.engine.runTests();

                if (!announcePhaseCompletion()) {
                    LOG.error("Agent " + ipAddress + " could not inform driver that phase was executed.");
                    System.exit(-1);
                }

                updateStatus(Status.IDLE);
            });

            return "";
        }));

        /* Expose http endpoint to be used by the driver for heartbeat messages */
        get(HEARTBEAT_PATH, ((request, response) ->
        {
            // send to driver the total number of executions/test
            Gson gson = new Gson();
            Map<String, Long> currentCounts = new HashMap<>();

            // check if execution has started
            if (this.status != Status.RUNNING) {
                return gson.toJson(currentCounts);
            }

            Map<AbstractTest, AtomicLong> phaseCounts = engine.getCurrentPhase().getCounts();
            phaseCounts.forEach((test, count) -> currentCounts.put(test.getName(), count.get()));

            return gson.toJson(currentCounts);
        }));



        /* expose http endpoint for receiving redistribution requests from the driver */
        post(REBALANCE_PATH, (((request, response) ->  {
            statusLock.readLock().lock();
            if (this.status == Status.IDLE) {
                LOG.warn("Rebalance was requested before submitting a task. Request will be ignored.");
                statusLock.readLock().unlock();
                return "";
            }
            statusLock.readLock().unlock();

            String instructionsMessage = request.body();
            LOG.info("[Agent] Received " + instructionsMessage  + " from driver");
            this.redistributionInstructionsProcessor.processInstructions(instructionsMessage, this.engine.getConfiguration().getPhases().get(0));

            return "";
        })));


        /* expose http endpoint for health checks */
        get(HEALTH_PATH, ((request, response) -> "Healthy"));

        /* expose http endpoint for getting the current status of the agent */
        get(GET_STATUS_PATH, ((request, response) -> {
            this.statusLock.readLock().lock();
            Status status = this.status;
            this.statusLock.readLock().unlock();

            return status;
        }));

        /* expose http endpoint for finishing the execution of the agent */
        post(FINISH_PATH, ((request, response) -> {
            scheduledExecutorService.schedule(() -> {
                this.tdExecutorService.shutdown();
                this.scheduledExecutorService.shutdown();

                System.exit(0);
            }, GlobalArgs.parseDurationToSeconds("3s"), TimeUnit.SECONDS);

            return "";
        }));
    }

    /* Method responsible for registering the current agent to the driver. It should be the
     * first method executed.
     */
    private void register() {
        HttpResponse response =
                this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, ipAddress,
                                                Driver.getAgentRegisterPath(), HTTP_REQUEST_RETRIES);
        if (response == null) {
            System.out.println("could not register");
            System.exit(-1);
        }

    }
}
