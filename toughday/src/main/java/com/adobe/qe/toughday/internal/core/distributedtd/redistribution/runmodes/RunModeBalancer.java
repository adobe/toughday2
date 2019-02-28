package com.adobe.qe.toughday.internal.core.distributedtd.redistribution.runmodes;

import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.RedistributionInstructions;

import java.util.Map;

/**
 * Contains all the methods that should be implemented in order to describe how a certain
 * run mode must be redistributed when the number of agents running in the cluster modifies.
 * @param <T> : type of the run mode
 */
public interface RunModeBalancer<T extends RunMode> {
    /**
     * Collects all the properties that must be taken into consideration when redistributing
     * the work between the agents.
     * @param type Class of the run mode
     * @param runMode
     * @return
     */
    Map<String, String> getRunModePropertiesToRedistribute(Class type, T runMode);

    /**
     * This method is called before changing the actual value of the properties to be
     * modified. It should be used for creating the appropriate environment for running
     * with the run mode configuration.
     * @param redistributionInstructions instructions for redistributing the work
     * @param runMode
     */
    void before(RedistributionInstructions redistributionInstructions, T runMode);

    /**
     * This method is responsible for changing the values of the properties that must be
     * redistributed according to the redistribution instructions received as a parameter.
     * @param redistributionInstructions instructions for redistributing the work
     * @param runMode
     */
    void processRunModeInstructions(RedistributionInstructions redistributionInstructions, T runMode);

    /**
     * The last method called when the process of redistributing the work is
     * executing.
     * @param redistributionInstructions instructions for redistributing the work
     * @param runMode
     */
    void after(RedistributionInstructions redistributionInstructions, T runMode);
}
