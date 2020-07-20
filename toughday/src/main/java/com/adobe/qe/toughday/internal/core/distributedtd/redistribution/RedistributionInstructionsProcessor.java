package com.adobe.qe.toughday.internal.core.distributedtd.redistribution;

import com.adobe.qe.toughday.internal.core.TestSuite;
import com.adobe.qe.toughday.internal.core.engine.Phase;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.runmodes.ConstantLoadRunModeBalancer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  Class responsible for processing the rebalancing request received by an agent from the driver when the
 *  work must be rebalanced.
 */
public class RedistributionInstructionsProcessor {
    protected static final Logger LOG = LogManager.getLogger(ConstantLoadRunModeBalancer.class);

    private void processTestSuiteChanges(Map<String, Long> counts, Phase phase) {
        TestSuite testSuite = phase.getTestSuite();
        long nr = testSuite.getTests().stream()
                .filter(test -> !counts.containsKey(test.getName()))
                .count();
        if (nr > 0) {
            throw new IllegalStateException("Instructions were not received for each test in the test suite.");
        }

        testSuite.getTests()
                .stream()
                .filter(test -> counts.containsKey(test.getName()))
                .forEach(test -> {
                    // remove tests for which the count property was achieved
                    if (counts.get(test.getName()) == 0) {
                        phase.getCounts().remove(test);
                    } else {
                        // reset number of tests executed so far
                        phase.getCounts().put(test, new AtomicLong(0));
                        // update number of executions left for this test
                        test.setCount(String.valueOf(counts.get(test.getName())));
                    }
                });
    }

    private void processRunModeChanges(RedistributionInstructions redistributionInstructions, RunMode runMode) {
        runMode.getRunModeBalancer().before(redistributionInstructions, runMode);

        runMode.getRunModeBalancer().processRunModeInstructions(redistributionInstructions, runMode);

        runMode.getRunModeBalancer().after(redistributionInstructions, runMode);
    }

    /**
     * Method used for processing the rebalance request.
     * @param jsonInstructions : redistribution instructions received from the driver
     * @param phase : the current phase being executed by the agents.
     * @throws IOException : if the instructions don't have the appropriate format.
     */
    public void processInstructions(String jsonInstructions, Phase phase) throws IOException {
        if (phase == null || jsonInstructions == null) {
            throw new IllegalArgumentException("Phase and redistribution instructions must not be null during work " +
                    "redistribution process.");
        }

        ObjectMapper objectMapper = new ObjectMapper();
        RedistributionInstructions redistributionInstructions =
                objectMapper.readValue(jsonInstructions, RedistributionInstructions.class);

        // update values for each modified property
        processRunModeChanges(redistributionInstructions, phase.getRunMode());
        processTestSuiteChanges(redistributionInstructions.getCounts(), phase);
    }
}
