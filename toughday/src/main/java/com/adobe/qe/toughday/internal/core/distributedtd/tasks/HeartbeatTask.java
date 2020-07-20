package com.adobe.qe.toughday.internal.core.distributedtd.tasks;

import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.distributedtd.DistributedPhaseMonitor;
import com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.Agent;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.TaskBalancer;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;

import static com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils.HTTP_REQUEST_RETRIES;

/**
 * Class used for implementing the heartbeat protocol for monitoring the state of the agents running in the cluster.
 */
public class HeartbeatTask implements Runnable {
    protected static final Logger LOG = LogManager.getLogger(Driver.class);

    private final Queue<String> agents;
    private DistributedPhaseMonitor distributedPhaseMonitor;
    private Configuration configuration;
    private Configuration driverConfiguration;
    private final HttpUtils httpUtils = new HttpUtils();
    private final TaskBalancer taskBalancer = TaskBalancer.getInstance();

    public HeartbeatTask(Queue<String> agents, DistributedPhaseMonitor distributedPhaseMonitor,
                         Configuration configuration, Configuration driverConfiguration) {
        this.agents = agents;
        this.distributedPhaseMonitor = distributedPhaseMonitor;
        this.configuration = configuration;
        this.driverConfiguration = driverConfiguration;
    }

    private void processHeartbeatResponse(String agentIp, HttpResponse agentResponse) throws IOException {
        // the agent has sent his statistic for executions/test => aggregate counts
        Gson gson = new Gson();
        String yamlCounts =  EntityUtils.toString(agentResponse.getEntity());

        // gson treats numbers as double values by default
        Map<String, Double> doubleAgentCounts = gson.fromJson(yamlCounts, Map.class);
        LOG.info("[Heartbeat] Received execution state from agent " + agentIp + ": " + doubleAgentCounts.toString());

        // recently added agents might not execute tests yet
        if (doubleAgentCounts.isEmpty()) {
            return;
        }

        this.distributedPhaseMonitor.getExecutions().forEach((testName, executionsPerAgent) ->
                executionsPerAgent.put(agentIp, doubleAgentCounts.get(testName).longValue()));
    }

    /**
     * Method used for sending heartbeat messages to all the agents running in the cluster. This method should be
     * called periodically during the entire distributed execution of ToughDay.
     */
    @Override
    public void run() {
        List<String> activeAgents = new ArrayList<>(agents);
        // remove agents which previously failed to respond to heartbeat request
        this.taskBalancer.getInactiveAgents().forEach(activeAgents::remove);

        for (String agentIp : activeAgents) {
            String URI = Agent.getHeartbeatPath(agentIp);
            HttpResponse agentResponse = httpUtils.sendHttpRequest(HttpUtils.GET_METHOD, "", URI, HTTP_REQUEST_RETRIES);
            if (agentResponse != null) {
                try {
                    processHeartbeatResponse(agentIp, agentResponse);
                } catch (IOException e) {
                    // the system may reach the limit of available IO resources
                    LOG.warn("Could not process heartbeat information received form agent " + agentIp);
                }
                continue;
            }

            if (!this.distributedPhaseMonitor.isPhaseExecuting()) {
                agents.remove(agentIp);
                continue;
            }

            this.taskBalancer.scheduleWorkRedistributionProcess(this.distributedPhaseMonitor, this.agents,
                                            this.configuration, this.driverConfiguration.getDistributedConfig(),
                                            agentIp, false);
        }

        LOG.info("Number of executions per test: " + this.distributedPhaseMonitor.getExecutionsPerTest());
    }
}
