package com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.requests;

import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils;
import com.adobe.qe.toughday.internal.core.distributedtd.YamlDumpConfigurationAsTaskForTDAgents;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.Agent;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import spark.Request;
import spark.Response;

import java.util.ArrayList;
import java.util.List;

import static com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils.HTTP_REQUEST_RETRIES;
import static com.adobe.qe.toughday.internal.core.engine.Engine.logGlobal;

/**
 * Specifies how the master will process the HTTP requests received from the other components running in the cluster.
 */
public class MasterRequestProcessor extends AbstractRequestProcessor {
    private final Object object = new Object();
    private static MasterRequestProcessor instance = null;

    /**
     * Returns an instance of this class.
     * @param driver : the driver instance that will use this class for processing HTTP requests.
     */
    public static MasterRequestProcessor getInstance(Driver driver) {
        if (instance == null || !instance.driverInstance.equals(driver)) {
            instance = new MasterRequestProcessor(driver);
        }

        return instance;
    }

    private MasterRequestProcessor(Driver driverInstance) {
        super(driverInstance);
    }

    private void waitForSampleContentToBeInstalled(Driver currentDriver) {
        synchronized (object) {
            try {
                object.wait();
            } catch (InterruptedException e) {
                LOG.error("Failed to install ToughDay sample content package. Execution will be stopped.");
                currentDriver.finishDistributedExecution();
                System.exit(-1);
            }
        }
    }

    private void installToughdayContentPackage(Configuration configuration, Driver currentDriver) {
        logGlobal("Installing ToughDay 2 Content Package...");
        GlobalArgs globalArgs = configuration.getGlobalArgs();

        if (globalArgs.getDryRun() || !globalArgs.getInstallSampleContent()) {
            return;
        }

        HttpResponse agentResponse = null;
        List<String> agentsCopy = new ArrayList<>(this.driverInstance.getDriverState().getRegisteredAgents());

        while (agentResponse == null && agentsCopy.size() > 0) {
            // pick one agent to install the sample content
            String agentIpAddress = agentsCopy.remove(0);
            String URI = Agent.getInstallSampleContentPath(agentIpAddress);
            LOG.info("Installing sample content request was sent to agent " + agentIpAddress);

            YamlDumpConfigurationAsTaskForTDAgents dumpConfig
                    = new YamlDumpConfigurationAsTaskForTDAgents(configuration);
            String yamlTask = dumpConfig.generateConfigurationObject();

            agentResponse = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, yamlTask, URI, HTTP_REQUEST_RETRIES);
        }

        // we should wait until the we receive confirmation that the sample content was successfully installed
        waitForSampleContentToBeInstalled(currentDriver);

        logGlobal("Finished installing ToughDay 2 Content Package.");
        globalArgs.setInstallSampleContent("false");

    }

    private void mergeDistributedConfigParams(Configuration configuration, Driver currentDriver) {
        DriverState driverState = currentDriver.getDriverState();

        if (driverState.getDriverConfig().getDistributedConfig().getHeartbeatIntervalInSeconds() ==
                currentDriver.getConfiguration().getDistributedConfig().getHeartbeatIntervalInSeconds()) {

            driverState.getDriverConfig().getDistributedConfig().merge(configuration.getDistributedConfig());
            return;
        }

        // cancel heartbeat task and reschedule it with the new period
        currentDriver.cancelHeartbeatTask();
        driverState.getDriverConfig().getDistributedConfig().merge(configuration.getDistributedConfig());
        currentDriver.scheduleHeartbeatTask();
    }


    private void handleExecutionRequest(Configuration configuration, Driver currentDriver) {
        installToughdayContentPackage(configuration, currentDriver);
        mergeDistributedConfigParams(configuration, currentDriver);

        currentDriver.executePhases();
    }

    @Override
    public String acknowledgeSampleContentSuccessfulInstallation(Request request, Driver driverInstance, Response response) {
        boolean installed = Boolean.parseBoolean(request.body());

        if (!installed) {
            LOG.error("Failed to install the ToughDay sample content package. Execution will be stopped.");
            driverInstance.finishDistributedExecution();
        }

        synchronized (this.object) {
            this.object.notify();
        }

        return "";
    }

    @Override
    public String processExecutionRequest(Request request, Driver driverInstance) throws Exception {
        super.processExecutionRequest(request, driverInstance);

        // handle execution in a different thread to be able to quickly respond to this request
        driverInstance.getExecutorService().submit(() -> handleExecutionRequest(driverInstance.getConfiguration(), driverInstance));

        return "";
    }

    @Override
    public String processHeartbeatRequest(Request request, Driver driverInstance) {
        // send executions/test to candidates to keep them updated
        Gson gson = new Gson();
        return gson.toJson(driverInstance.getDistributedPhaseMonitor().getExecutions());
    }

    @Override
    public String processAgentFailureAnnouncement(Request request, Driver driverInstance) {
        throw new IllegalStateException("Master should never receive this type of request.");
    }

    @Override
    public String processRegisterRequest(Request request, Driver driverInstance) {
        super.processRegisterRequest(request, driverInstance);
        String agentIp = request.body();
        DriverState driverState = this.driverInstance.getDriverState();
        LOG.info("[driver] Registered agent with ip " + agentIp);

        if (!this.driverInstance.getDistributedPhaseMonitor().isPhaseExecuting()) {
            driverState.registerAgent(agentIp);
            LOG.info("[driver] active agents " + driverState.getRegisteredAgents().toString());
            return "";
        }

        // master must schedule the work redistribution process when a new agent is registering
        this.driverInstance.getTaskBalancer().scheduleWorkRedistributionProcess(this.driverInstance, agentIp, true);

        return "";
    }
}
