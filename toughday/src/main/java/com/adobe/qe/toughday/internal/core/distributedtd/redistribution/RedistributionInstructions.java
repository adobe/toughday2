package com.adobe.qe.toughday.internal.core.distributedtd.redistribution;

import java.util.Map;

/** Contains all the information needed by the agents for updating their configuration when the work needs to be
 * rebalanced.
 */
public class RedistributionInstructions {
    private Map<String, Long> counts;
    private Map<String, String> runModeProperties;

    // dummy constructor, required for dumping the class
    public RedistributionInstructions() { }

    public RedistributionInstructions(Map<String, Long> counts, Map<String, String> runModeProperties) {
        this.counts = counts;
        this.runModeProperties = runModeProperties;
    }

    /**
     * Getter for counts.
     */
    public Map<String, Long> getCounts() {
        return this.counts;
    }

    /**
     * Setter for counts.
     */
    public void setCounts(Map<String, Long> counts) {
        this.counts = counts;
    }

    /**
     * Getter for run mode properties that must be redistributed.
     */
    public Map<String, String> getRunModeProperties() { return this.runModeProperties; }

    /**
     * Setter for run mode properties that must be redistributed.
     */
    public void setRunModeProperties(Map<String, String> runModeProperties) {
        this.runModeProperties = runModeProperties;
    }
}