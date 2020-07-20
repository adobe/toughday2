package com.adobe.qe.toughday.internal.core.distributedtd.splitters.runmodes;

import com.adobe.qe.toughday.internal.core.engine.RunMode;

import java.util.List;
import java.util.Map;

/**
 * Common interface for all run mode splitters. Implement this interface to specify how a certain type of run mode
 * must be partitioned into multiple run modes to be distributed to the agents running in the cluster.
 * @param <T> Type of the run mode to be partitioned
 */
public interface RunModeSplitter<T extends RunMode> {
    /**
     * Used for distributing the run mode when before sending TD execution requests to the agents
     * running in the cluster.
     * @param runMode object to be partitioned
     * @param agents names of the agents waiting to receive TD execution requests from the driver.
     */
    Map<String, T> distributeRunMode(T runMode, List<String> agents) throws CloneNotSupportedException;

    /**
     * Used for redistributing the run mode when the process of rebalancing the work is triggered.
     * @param runMode object to be partitioned
     * @param existingAgents list of agents that were running TD before starting the work redistribution process.
     * @param newAgents list of agents which recently joined the cluster
     * @param phaseStartTime : time when the phase execution started
     */
    Map<String, T> distributeRunModeForRebalancingWork(T runMode, List<String> existingAgents, List<String> newAgents,
                                                       long phaseStartTime) throws CloneNotSupportedException;
}
