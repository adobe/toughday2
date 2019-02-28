package com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver;

import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.internal.core.distributedtd.YamlDumpConfigurationAsTaskForTDAgents;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.Agent;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests.RequestProcessorDispatcher;
import com.adobe.qe.toughday.internal.core.distributedtd.splitters.PhaseSplitter;
import com.adobe.qe.toughday.internal.core.distributedtd.tasks.MasterHeartbeatTask;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.distributedtd.DistributedPhaseMonitor;
import com.adobe.qe.toughday.internal.core.distributedtd.tasks.HeartbeatTask;
import com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.TaskBalancer;
import com.adobe.qe.toughday.internal.core.engine.Phase;
import org.apache.http.HttpResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;

import static com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils.*;
import static spark.Spark.*;

/**
 * Driver component for the cluster.
 */
public class Driver {
    // routes
    public static final String EXECUTION_PATH = "/config";
    private static final String REGISTER_PATH = "/registerAgent";
    private static final String PHASE_FINISHED_BY_AGENT_PATH = "/phaseFinished";
    private static final String HEALTH_PATH = "/health";
    private static final String SAMPLE_CONTENT_ACK_PATH = "/contentAck";
    private static final String MASTER_ELECTION_PATH = "/masterElection";
    private static final String ASK_FOR_UPDATES_PATH = "/driverUpdates";
    private static final String GET_NR_DRIVERS_PATH = "/getNrDrivers";
    private static final String HEARTBEAT_PATH = "/heartbeat";
    private static final String AGENT_FAILURE_PATH = "/agentFailure";

    protected static final Logger LOG = LogManager.getLogger(Engine.class);

    private final ScheduledExecutorService heartbeatScheduler = Executors.newSingleThreadScheduledExecutor();
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final HttpUtils httpUtils = new HttpUtils();
    private final TaskBalancer taskBalancer = TaskBalancer.getInstance();
    private DistributedPhaseMonitor distributedPhaseMonitor = new DistributedPhaseMonitor();
    private Configuration configuration;
    private DriverState driverState;
    private MasterElection masterElection;
    private ScheduledFuture<?> scheduledFuture = null;

    public DistributedPhaseMonitor getDistributedPhaseMonitor() {
        return this.distributedPhaseMonitor;
    }

    /**
     * Getter for the task balancer instance.
     */
    public TaskBalancer getTaskBalancer() {
        return this.taskBalancer;
    }

    /**
     * Getter for the TD configuration which must be executed in distributed mode.
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Getter for the state of the driver.
     */
    public DriverState getDriverState() {
        return this.driverState;
    }

    /**
     * Getter for master election instance.
     */
    public MasterElection getMasterElection() {
        return this.masterElection;
    }

    /**
     * Constructor
     * @param configuration : configuration given when the driver was deployed into the cluster.
     */
    public Driver(Configuration configuration) {
        try {
            String hostname = InetAddress.getLocalHost().getHostName();
            this.driverState = new DriverState(hostname, configuration);
        } catch (UnknownHostException e) {
            System.exit(-1);
        }

        this.masterElection = MasterElection.getInstance(this.driverState.getNrDrivers());
        this.masterElection.electMasterWhenDriverJoinsTheCluster(this);

        LOG.info("Driver " + this.driverState.getHostname() + " elected as master " + this.driverState.getMasterId());
    }

    /**
     * Returns the http URL that should be used for triggering the distributed execution in the cluster.
     * @param driverIdentifier : the identifier of the driver that will received this execution query.
     * @param port : the port on which the query must be sent.
     * @param forwardReq : true if the request must be forwarded to all the other drivers running in the cluster; false
     *                   otherwise.
     */
    public static String getExecutionPath(String driverIdentifier, String port, boolean forwardReq) {
        return HttpUtils.URL_PREFIX + driverIdentifier + ":" + port + Driver.EXECUTION_PATH +
                HttpUtils.FORWARD_QUERY_PARAM + forwardReq;
    }

    /**
     * Returns the http URL that should be used by the agents whenever they finished executing the task received from
     * the driver.
     */
    public static String getPhaseFinishedByAgentPath(String driverIdentifier, String port, boolean forwardReq) {
        return URL_PREFIX + driverIdentifier + ":" + port + PHASE_FINISHED_BY_AGENT_PATH +
                HttpUtils.FORWARD_QUERY_PARAM  + forwardReq;
    }

    /**
     * Returns the http URL that should be used by the agent chosen to install the TD sample content package for
     * informing the driver that the installation was completed successfully.
     */
    public static String getSampleContentAckPath(String driverIdentifier, String port) {
        return URL_PREFIX + driverIdentifier + ":" + port + SAMPLE_CONTENT_ACK_PATH;
    }

    /**
     * Returns the http URL that should be used by the driver which detected that the current Master died to inform all
     * the other drivers running in the cluster that the master election process must be triggered.
     * @param driverHostname : hostname of the driver exposing this http endpoint
     */
    public static String getMasterElectionPath(String driverHostname, String port) {
        return URL_PREFIX + driverHostname + ":" + port + MASTER_ELECTION_PATH;
    }

    /**
     * Returns the http URL that should be used by the agents to register themselves after joining the cluster.
     * @param driverIdentifier : specifies what kind of pods should received this request.
     * @param port : the port on which the driver is listening for this type of request.
     * @param forwardReq : true if this request should be forwarded to all the drivers running in the cluster; false
     *                   otherwise
     */
    public static String getAgentRegisterPath(String driverIdentifier, String port, boolean forwardReq) {
        return URL_PREFIX + driverIdentifier + ":" + port + REGISTER_PATH + HttpUtils.FORWARD_QUERY_PARAM  + forwardReq;
    }


    /**
     * Returns the http URL that should be used by the Candidates to periodically check if the current Master is running
     * properly.
     * @param driverHostName : hostname that uniquely identifies the driver which should receive the heartbeat message
     */
    public static String getHeartbeatPath(String driverHostName) {
        return URL_PREFIX + driverHostName + ":" + HttpUtils.SPARK_PORT + HEARTBEAT_PATH;
    }

    /**
     * Returns the http URL that should be used by the drivers to get information about the current state of the
     * execution.
     * @param driverHostName : hostname of the driver component exposing this http endpoint
     */
    public static String getAskForUpdatesPath(String driverHostName) {
        return URL_PREFIX + driverHostName + ":" + SPARK_PORT + ASK_FOR_UPDATES_PATH;
    }

    /**
     * Returns the http URL that should be used by the current master to announce that a certain agent failed to respond
     * to heartbeat messages.
     * @param driverHostName : hostname of the driver component that should receive the http request
     */
    public static String getAgentFailurePath(String driverHostName) {
        return URL_PREFIX + driverHostName + ":" + SPARK_PORT + AGENT_FAILURE_PATH;
    }

    /**
     * Getter for the executor service used to submit the task of executing ToughDay distributed. The execution request
     * must be handled in a different thread in order to be able to quickly send a response for this request.
     */
    public ExecutorService getExecutorService() {
        return this.executorService;
    }

    /**
     * Setter for the TD configuration to be executed in distributed mode.
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Method used for announcing the agents that the TD configuration was successfully executed and they should stop
     * their execution.
     */
    public void finishAgents() {
        this.driverState.getRegisteredAgents().forEach(agentIp -> {
            LOG.info("[Driver] Finishing agent " + agentIp);
            HttpResponse response =
                    httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, "", Agent.getFinishPath(agentIp), HTTP_REQUEST_RETRIES);
            if (response == null) {
                // the assumption is that the agent will be killed when he fails to respond to heartbeat request
                LOG.warn("Driver could not finish the execution on agent " + agentIp + ".");
            }
        });

        this.driverState.getRegisteredAgents().clear();
    }

    /**
     * Method used to finish the distributed execution. The agents will be informed that there are no more phases to be
     * executed and they will stop running.
     */
    public void finishDistributedExecution() {
        finishAgents();

        this.executorService.shutdownNow();
        // finish tasks
        this.heartbeatScheduler.shutdownNow();
    }

    /**
     * Method used for scheduling the periodic task of sending heartbeat messages to all the agents running in the
     * cluster to be executed at every 'heartbeatInterval' seconds.
     */
    public void scheduleHeartbeatTask() {
        // we should periodically send heartbeat messages from driver to all the agents
        heartbeatScheduler.scheduleAtFixedRate(new HeartbeatTask(this),
                0, this.driverState.getDriverConfig().getDistributedConfig().getHeartbeatIntervalInSeconds(), TimeUnit.SECONDS);
    }

    /**
     * Method used for scheduling the periodic task of sending heartbeat messages to the driver playing the role of the
     * Master to be executed at every 10 seconds.
     */
    public void scheduleMasterHeartbeatTask() {
        // we should periodically send heartbeat messages from candidates to check id the master is still running
        this.scheduledFuture = this.heartbeatScheduler.scheduleAtFixedRate(new MasterHeartbeatTask(this), 0,
                GlobalArgs.parseDurationToSeconds("10s"), TimeUnit.SECONDS);
    }

    /**
     * Method used for cancelling the periodic task of sending heartbeat messages to the driver running as Master in the
     * cluster.
     */
    public void cancelMasterHeartBeatTask() {
        if (this.scheduledFuture != null) {
            if(!this.scheduledFuture.cancel(true) && !this.scheduledFuture.isDone()) {
                LOG.warn("Could not cancel task used to periodically send heartbeat messages to the Master.");
            }
        }
    }

    /**
     * Method used for cancelling the periodic task of sending heartbeat messaged to the agents running in the cluster.
     */
    public void cancelHeartbeatTask() {
        this.heartbeatScheduler.shutdownNow();
    }

    /**
     * Method used to resume the distributed execution of ToughDay whenever a new master is elected in the cluster.
     */
    public void resumeExecution() {
        if (this.configuration == null) {
            return;
        }

        LOG.info("Resuming execution...");

        // wait until current phase is successfully finished
        if (!distributedPhaseMonitor.waitForPhaseCompletion(3)) {
            finishDistributedExecution();
            return;
        }

        LOG.info("Phase " + distributedPhaseMonitor.getPhase().getName() + " finished execution successfully.");
        List<Phase> remainingPhases = new ArrayList<>(configuration.getPhases());
        remainingPhases.remove(remainingPhases.get(0));

        LOG.info("Phases left to be executed: " + remainingPhases.toString());
        this.configuration.setPhases(remainingPhases);

        // continue executing all the other phases
        executePhases();
    }


    /**
     * Method used for running the phases of the TD configuration received to be executed distributed. This method
     * should only be called once, when the master receives the query to execute TD. Whenever a new master is elected,
     * the {@link #resumeExecution()} method should be used.
     */
    public void executePhases() {
        PhaseSplitter phaseSplitter = new PhaseSplitter();

        LOG.info("[Driver] Executing phases + " + this.configuration.getPhases().toString());
        for (Phase phase : configuration.getPhases()) {
            try {
                Map<String, Phase> tasks = phaseSplitter.splitPhase(phase, new ArrayList<>(this.driverState.getRegisteredAgents()));
                this.distributedPhaseMonitor.setPhase(phase);

                for (String agentIp : this.driverState.getRegisteredAgents()) {
                    configuration.setPhases(Collections.singletonList(tasks.get(agentIp)));

                    // convert configuration to yaml representation
                    YamlDumpConfigurationAsTaskForTDAgents dumpConfig
                            = new YamlDumpConfigurationAsTaskForTDAgents(configuration);
                    String yamlTask = dumpConfig.generateConfigurationObject();

                    /* send query to agent and register running task */
                    String URI = Agent.getSubmissionTaskPath(agentIp);
                    HttpResponse response = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, yamlTask, URI, HTTP_REQUEST_RETRIES);

                    if (response != null) {
                        this.distributedPhaseMonitor.registerAgentRunningTD(agentIp);
                        LOG.info("Task was submitted to agent " + agentIp);
                    } else {
                        /* the assumption is that the agent is no longer active in the cluster and he will fail to respond
                         * to the heartbeat request sent by the driver. This will automatically trigger process of
                         * redistributing the work
                         * */
                        LOG.info("Task\n" + yamlTask + " could not be submitted to agent " + agentIp +
                                ". Work will be rebalanced once the agent fails to respond to heartbeat request.");
                    }
                }

                // al execution queries were sent => set phase execution start time
                this.distributedPhaseMonitor.setPhaseStartTime(System.currentTimeMillis());

                // we should wait until all agents complete the current tasks in order to execute phases sequentially
                if (!this.distributedPhaseMonitor.waitForPhaseCompletion(3)) {
                    break;
                }

                LOG.info("Phase " + phase.getName() + " finished execution successfully.");

            } catch (CloneNotSupportedException e) {
                LOG.error("Phase " + phase.getName() + " could not de divided into tasks to be sent to the agents.", e);

                LOG.info("Finishing agents");
                finishAgents();

                System.exit(-1);
            }
        }

        finishDistributedExecution();
    }

    /**
     * Starts the execution of the driver.
     */
    public void run() {
        RequestProcessorDispatcher dispatcher = RequestProcessorDispatcher.getInstance();
        /* expose http endpoint for running TD with the given configuration */
        post(EXECUTION_PATH, ((request, response) -> dispatcher.getRequestProcessor(this)
                .processExecutionRequest(request, this)
        ));

        /* health check http endpoint */
        get(HEALTH_PATH, ((request, response) -> "Healthy"));

        /* expose http endpoint to allow agents to get the number of drivers running in the cluster */
        get(GET_NR_DRIVERS_PATH, ((request, response) -> String.valueOf(this.getDriverState().getNrDrivers())));

        /* http endpoint used by the agent installing the sample content to announce if the installation was
         * successful or not. */
        post(SAMPLE_CONTENT_ACK_PATH, ((request, response) ->
                    dispatcher.getRequestProcessor(this)
                            .acknowledgeSampleContentSuccessfulInstallation(request, this, response)));

        /* expose http endpoint to allow agents to announce when they finished executing the current phase */
        post(PHASE_FINISHED_BY_AGENT_PATH, ((request, response) ->
                dispatcher.getRequestProcessor(this).processPhaseCompletionAnnouncement(request)));

        /* expose http endpoint for triggering the master election process */
        post(MASTER_ELECTION_PATH, ((request, response) ->
                dispatcher.getRequestProcessor(this).processMasterElectionRequest(request, this)));

        /* expose http endpoint for sending information about the current state of the distributed execution */
        get(ASK_FOR_UPDATES_PATH, ((request, response) -> dispatcher.getRequestProcessor(this)
                .processUpdatesRequest(request, this)));

        get(HEARTBEAT_PATH, ((request, response) -> dispatcher.getRequestProcessor(this)
                .processHeartbeatRequest(request, this)));

        post(AGENT_FAILURE_PATH, ((request, response) -> dispatcher.getRequestProcessor(this)
                .processAgentFailureAnnouncement(request, this)));

        /* expose http endpoint for registering new agents in the cluster */
        post(REGISTER_PATH, (request, response) -> dispatcher.getRequestProcessor(this)
                .processRegisterRequest(request, this));
    }
}
