package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.Phase;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Class responsible for monitoring the execution of a distributed phase.
 */
public class DistributedPhaseMonitor {
    protected static final Logger LOG = LogManager.getLogger(Engine.class);

    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final List<String> agentsRunningTD = new ArrayList<>();
    // key = name of the test; value = map(key = name of the agent, value = nr of tests executed)
    private Map<String, Map<String, Long>> executions = new HashMap<>();
    private Phase phase;
    private long phaseStartTime = 0;

    /**
     * Returns true is the exections of the phase has started.
     */
    public boolean isPhaseExecuting() {
        return this.phase != null && !this.agentsRunningTD.isEmpty();
    }

    /**
     * Setter for the execution start time of the phase.
     */
    public void setPhaseStartTime(long phaseStartTime) {
        this.phaseStartTime = phaseStartTime;
    }

    /**
     * Getter for the execution start time of the phase
     */
    public long getPhaseStartTime() {
        return this.phaseStartTime;
    }

    /**
     * Setter for the phase being monitored
     */
    public void setPhase(Phase phase) {
        this.phase = phase;
        this.phase.getTestSuite().getTests().forEach(test -> executions.put(test.getName(), new HashMap<>()));
    }

    /**
     * Adds the agent received as a parameter to the list of agents which are currently executing the
     * phase being monitored.
     * @param agentIdentifier : ip address that identifies the agent inside the cluster
     */
    public void registerAgentRunningTD(String agentIdentifier) {
        this.agentsRunningTD.add(agentIdentifier);

    }

    /**
     * Removes tha agent received as a parameter from the list of agents which are currently executing
     * the phase being monitored.
     * @param agentIdentifier : ip address that identifies the agent inside the cluster
     */
    public void removeAgentFromActiveTDRunners(String agentIdentifier) {
        this.agentsRunningTD.remove(agentIdentifier);
        // remove agent from executions map
        this.executions.forEach((test, executionsPerAgent) -> this.executions.get(test).remove(agentIdentifier));
    }

    /**
     * Method for waiting until all agents finish executing the phase being monitored.
     */
    public boolean waitForPhaseCompletion(int retries) {
        while (retries > 0) {
            Future<?> waitPhaseCompletion = executorService.submit(() -> {
                while (!this.agentsRunningTD.isEmpty()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // this will not cause further problems => it can be ignored
                    }
                }
            });

            try {
                waitPhaseCompletion.get();
                return true;
            } catch (InterruptedException | ExecutionException e) {
                retries--;
            }
        }

        LOG.warn("Exception occurred while waiting for the completion of the current phase. The rest " +
                "of the phases will no longer be executed");
        return false;

    }

    /**
     * Updates the number of executions/test that must be executed by the agents running in the cluster. This method is
     * called during the work redistribution process.
     */
    public void updateCountPerTest() {
        phase.getTestSuite().getTests().forEach(test -> {
            long remained = test.getCount() - this.getExecutionsPerTest().get(test.getName());
            if (remained < 0) {
                // set this to 0 so that the agents will know to delete the test from the test suite
                test.setCount("0");
            } else {
                test.setCount(String.valueOf(remained));
            }
        });
    }

    /**
     * Returns the number of executions/test/agent.
     */
    public Map<String, Map<String, Long>> getExecutions() {
        return this.executions;
    }

    /**
     * Resets the number of execution/test for each agent executing the phase being monitored.
     */
    public void resetExecutions() {
        this.executions.forEach((testName, executionsPerAgent) ->
                executionsPerAgent.keySet().forEach(agentName -> executionsPerAgent.put(agentName, 0L)));
    }

    /**
     * Returns the number of executions/test executed by all the agents running in the cluster.
     */
    public Map<String, Long> getExecutionsPerTest() {
        Map<String, Long> executionsPerTest = new HashMap<>();

        this.executions.forEach((testName, executionsPerAgent) ->
                executionsPerTest.put(testName, executionsPerAgent.values().stream().mapToLong(x -> x).sum()));

        return executionsPerTest;
    }

    /**
     * Getter for the phase being monitored by this class.
     */
    public Phase getPhase() {
        return this.phase;
    }

}