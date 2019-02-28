package com.adobe.qe.toughday.internal.core.distributedtd.redistribution;

import com.adobe.qe.toughday.internal.core.TestSuite;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.distributedtd.YamlDumpConfigurationAsTaskForTDAgents;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.Agent;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.Driver;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.driver.DriverState;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.Phase;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.distributedtd.DistributedPhaseMonitor;
import com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.DistributedConfig;
import com.adobe.qe.toughday.internal.core.distributedtd.splitters.PhaseSplitter;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

import static com.adobe.qe.toughday.internal.core.distributedtd.HttpUtils.HTTP_REQUEST_RETRIES;

/**
 * Class responsible for balancing the work between the agents running the cluster
 * whenever the number of agents is changing.
 */
public class TaskBalancer {
    protected static final Logger LOG = LogManager.getLogger(Engine.class);
    private static TaskBalancer instance = null;

    private RedistributionStatus status = RedistributionStatus.UNNECESSARY;
    private final PhaseSplitter phaseSplitter = new PhaseSplitter();
    private final HttpUtils httpUtils = new HttpUtils();
    private final ConcurrentLinkedQueue<String> inactiveAgents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<String> recentlyAddedAgents = new ConcurrentLinkedQueue<>();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public enum RedistributionStatus {
        UNNECESSARY,            // no need to redistribute the work
        SCHEDULED,              // redistribution is scheduled
        RESCHEDULE_REQUIRED,   /* nr of agents running in the cluster has changed while the work was redistributed so
                                 * we must reschedule the process */
        EXECUTING               // work redistribution process is executing
    }

    /**
     * Returns a list with all the agents that failed to respond to the heartbeat request sent
     * by the driver.
     */
    public ConcurrentLinkedQueue<String> getInactiveAgents() {
        return this.inactiveAgents;
    }

    private TaskBalancer() {}

    /**
     * Returns a singleton instance of this class.
     */
    public static TaskBalancer getInstance() {
        if (instance == null) {
            instance = new TaskBalancer();
        }

        return instance;
    }

    private Map<String, Long> getTestSuitePropertiesToRedistribute(TestSuite taskTestSuite) {
        HashMap<String, Long> map = new HashMap<>();
        taskTestSuite.getTests().forEach(test -> map.put(test.getName(), test.getCount()));

        return map;
    }

    private void addNewAgent(String agentIdentifier) {
        this.recentlyAddedAgents.add(agentIdentifier);
    }

    private void addInactiveAgent(String agentIdentifier) {
        this.inactiveAgents.add(agentIdentifier);
    }

    private void waitUntilAllAgentsAreReadyForRebalancing(List<String> agents) {
        boolean ready = false;

        while(!ready) {
            ready = true;

            // get status of all new agents
            for (String agentIpAddress : agents) {
                HttpResponse agentResponse = this.httpUtils.sendHttpRequest(HttpUtils.GET_METHOD, "",
                        Agent.getGetStatusPath(agentIpAddress), HTTP_REQUEST_RETRIES);

                if (agentResponse != null) {
                    try {
                        String response = EntityUtils.toString(agentResponse.getEntity());
                        if (!response.equals(Agent.Status.RUNNING.toString())) {
                            ready = false;
                            break;
                        }
                    } catch (IOException e) {
                        LOG.warn("Could not confirm that agent " + agentIpAddress + " is ready for processing " +
                                "redistribution instructions") ;
                    }
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // skip this and continue waiting for all agents to be ready for redistribution
            }
        }
    }

    private void sendInstructionsToExistingAgents(Map<String, Phase> phases, List<String> activeAgents) {
        ObjectMapper mapper = new ObjectMapper();

        // send instructions to agents that are already running TD
        activeAgents.stream()
            .filter(ipAddress -> !this.inactiveAgents.contains(ipAddress)) // filter agents that become inactive
            .forEach(ipAddress -> {
                String agentURI = Agent.getRebalancePath(ipAddress);
                TestSuite testSuite = phases.get(ipAddress).getTestSuite();
                RunMode runMode = phases.get(ipAddress).getRunMode();

                // build redistribution instructions
                Map<String, Long> testSuiteProperties = getTestSuitePropertiesToRedistribute(testSuite);
                Map<String, String> runModeProperties = runMode.getRunModeBalancer()
                        .getRunModePropertiesToRedistribute(runMode.getClass(), runMode);
                RedistributionInstructions redistributionInstructions =
                        new RedistributionInstructions(testSuiteProperties, runModeProperties);
                try {
                    String message = mapper.writeValueAsString(redistributionInstructions);
                    LOG.info("[Redistribution] Sending " + message + " to agent " + ipAddress);

                    HttpResponse response = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, message,
                            agentURI, HTTP_REQUEST_RETRIES);
                    if (response == null) {
                        /* redistribution will be automatically triggered as soon as this agent fails to respond to
                         * heartbeat request
                         */
                        LOG.warn("Redistribution instructions could not be sent to agent " + ipAddress + ".");
                    }

                } catch (JsonProcessingException e) {
                    LOG.error("Unexpected exception while sending redistribution instructions", e);
                    LOG.warn("Agent " + ipAddress + " will continue to run with the configuration it had before " +
                            "the process of redistribution was triggered.");
                }

        });
    }

    private void sendExecutionRequestsToNewAgents(List<String> recentlyAddedAgents, Map<String, Phase> phases,
                                                  Configuration configuration, DriverState driverState) {
        // for the recently added agents, send execution queries
        recentlyAddedAgents.forEach(newAgentIpAddress -> {
            configuration.setPhases(Collections.singletonList(phases.get(newAgentIpAddress)));
            YamlDumpConfigurationAsTaskForTDAgents dumpConfig
                    = new YamlDumpConfigurationAsTaskForTDAgents(configuration);
            String yamlTask = dumpConfig.generateConfigurationObject();


            LOG.info("Sending execution request + " + yamlTask + " to new agent " + newAgentIpAddress);
            HttpResponse response = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, yamlTask,
                    Agent.getSubmissionTaskPath(newAgentIpAddress),  3);

            if (response == null) {
                /* the assumption is that the agent will fail to respond to heartbeat request => work will be
                 * automatically redistributed when this happens.
                 */
                LOG.info("Failed to send task to new agent " + newAgentIpAddress + ".");
            }
        });
    }

    private void updateStateOfAgents(Driver driverInstance, List<String> recentlyAddedAgents) throws CloneNotSupportedException {
        DistributedPhaseMonitor distributedPhaseMonitor = driverInstance.getDistributedPhaseMonitor();
        DriverState driverState = driverInstance.getDriverState();

        if (!distributedPhaseMonitor.isPhaseExecuting()) {
            return;
        }

        Phase phase = distributedPhaseMonitor.getPhase();
        // update number of tests that are left to be executed by the agents
        distributedPhaseMonitor.updateCountPerTest();
        Map<String, Phase> phases = this.phaseSplitter.splitPhaseForRebalancingWork(phase,
                new LinkedList<>(driverState.getRegisteredAgents()),
                new ArrayList<>(recentlyAddedAgents), distributedPhaseMonitor.getPhaseStartTime());

        // wait until all agents are able to process redistribution instructions
        waitUntilAllAgentsAreReadyForRebalancing(distributedPhaseMonitor.getAgentsRunningTD());
        sendInstructionsToExistingAgents(phases, distributedPhaseMonitor.getAgentsRunningTD());
        sendExecutionRequestsToNewAgents(recentlyAddedAgents, phases, driverInstance.getConfiguration(), driverState);

    }

    private void after(Driver driverInstance, List<String> newAgents, List<String> inactiveAgents) {
        DistributedPhaseMonitor distributedPhaseMonitor = driverInstance.getDistributedPhaseMonitor();
        DriverState driverState = driverInstance.getDriverState();

        distributedPhaseMonitor.resetExecutions();

        // mark recently added agents as active agents executing tasks
        newAgents.forEach(driverState::registerAgent);
        newAgents.forEach(distributedPhaseMonitor::registerAgentRunningTD);
        newAgents.forEach(this.recentlyAddedAgents::remove);

        // inform all the other drivers about the agents which failed to respond to heartbeat request
        inactiveAgents.forEach(agentIp -> {
            for (int i = 0; i < driverInstance.getDriverState().getNrDrivers(); i++) {
                if (i == driverInstance.getDriverState().getId()) {
                    continue;
                }

                HttpResponse driverResponse = this.httpUtils.sendHttpRequest(HttpUtils.POST_METHOD, agentIp,
                        Driver.getAgentFailurePath(driverInstance.getDriverState().getPathForId(i)), HTTP_REQUEST_RETRIES);
                if (driverResponse == null) {
                    /* the assumption is that if this driver will become the master it will detect this change once the
                     * heartbeat task is scheduled so no further actions should be done at this moment
                     */
                    LOG.warn("Failed to announce driver-" + i + " that agent " + agentIp + " died.");
                }

                LOG.info("Successfully announced driver-" + i + " that the agent " + agentIp + " died");
            }

        });

        LOG.info("[Redistribution] Finished redistributing the work");

        if (this.status == RedistributionStatus.RESCHEDULE_REQUIRED) {
            this.status = RedistributionStatus.SCHEDULED;
            LOG.info("[driver] Scheduling delayed redistribution process for agents " +
                    this.recentlyAddedAgents.toString());
            this.scheduler.schedule(() -> {
                LOG.info("[Redistribution] starting delayed work redistribution process");
                rebalanceWork(driverInstance);
            }, driverState.getDriverConfig().getDistributedConfig().getRedistributionWaitTimeInSeconds(), TimeUnit.SECONDS);
        } else {
            this.status = RedistributionStatus.UNNECESSARY;
        }


    }

    private void excludeInactiveAgents(List<String> agentsToBeExcluded, Driver driverInstance) {
        agentsToBeExcluded.forEach(driverInstance.getDriverState()::removeAgent);
        agentsToBeExcluded.forEach(this.inactiveAgents::remove);

        // we should not wait for task completion since the agent running it left the cluster
        agentsToBeExcluded.forEach(driverInstance.getDistributedPhaseMonitor()::removeAgentFromActiveTDRunners);
    }

    private void rebalanceWork(Driver driverInstance) {
        this.status = RedistributionStatus.EXECUTING;

        List<String> newAgents = new ArrayList<>(recentlyAddedAgents);
        List<String> inactiveAgents = new ArrayList<>(this.inactiveAgents);

        // remove all agents who failed answering the heartbeat request in the past
        excludeInactiveAgents(inactiveAgents, driverInstance);
        LOG.info("[Redistribution] Starting the process....");

        try {
            updateStateOfAgents(driverInstance, newAgents);
        } catch (CloneNotSupportedException e) {
            LOG.warn("");
        }

        after(driverInstance, newAgents, inactiveAgents);
    }

    /**
     * Method used for scheduling the process of redistributing the work between the agents running in the cluster. This
     * method is called whenever the number of active agents is changing.
     * @param driverInstance : the driver instance that will schedule the work redistribution process
     * @param agentIdentifier : the identifier of the agent that generated this call
     * @param activeAgent : true when the agent represented by agentIdentifier has joined the cluster; false if the
     *                    agent has become inactive(he failed to respond to heartbeat request)
     */
    public void scheduleWorkRedistributionProcess(Driver driverInstance, String agentIdentifier, boolean activeAgent) {
        DriverState driverState = driverInstance.getDriverState();

        if (activeAgent) {
            this.addNewAgent(agentIdentifier);
        } else {
            this.addInactiveAgent(agentIdentifier);
        }

        DistributedConfig distributedConfig = driverState.getDriverConfig().getDistributedConfig();
        if (this.status == RedistributionStatus.UNNECESSARY) {
            this.status = RedistributionStatus.SCHEDULED;
            LOG.info("Scheduling redistribution process to start in " +
                    distributedConfig.getRedistributionWaitTimeInSeconds() + " seconds.");

            // schedule work redistribution process
            this.scheduler.schedule(() -> rebalanceWork(driverInstance),
                    distributedConfig.getRedistributionWaitTimeInSeconds(), TimeUnit.SECONDS);
        } else if (this.status == RedistributionStatus.EXECUTING) {
            LOG.info("Work redistribution process must be rescheduled.");
            this.status = RedistributionStatus.RESCHEDULE_REQUIRED;
        }

    }
}
