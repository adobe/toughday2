package com.adobe.qe.toughday.internal.core.distributedtd.redistribution.runmodes;

import com.adobe.qe.toughday.internal.core.engine.runmodes.ConstantLoad;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.RedistributionInstructions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Map;


public class ConstantLoadRunModeBalancer extends AbstractRunModeBalancer<ConstantLoad> {
    protected static final Logger LOG = LogManager.getLogger(ConstantLoadRunModeBalancer.class);
    private static final String CURRENT_LOAD_PROPERTY = "currentload";

    @Override
    public Map<String, String> getRunModePropertiesToRedistribute(Class type, ConstantLoad runMode) {
        Map<String, String> runModeProps = super.getRunModePropertiesToRedistribute(type, runMode);

        if (runMode.isVariableLoad()) {
            runModeProps.put(CURRENT_LOAD_PROPERTY, String.valueOf(runMode.getCurrentLoad()));
        }

        return runModeProps;
    }

    @Override
    public void before(RedistributionInstructions redistributionInstructions, ConstantLoad runMode) {
        if (runMode.isVariableLoad()) {
            // We must cancel the scheduled task and reschedule it with the new values for 'period' and initial delay.
            boolean cancelled = runMode.cancelPeriodicTask();
            if (!cancelled) {
                LOG.warn("[Driver] Periodic task for updating the load could not be cancelled. Run mode properties " +
                        "might not be respected.");
            } else {
                LOG.info("[Driver] Periodic task for updating the load was cancelled.");
            }
        }

        Map<String, String> runModeProperties = redistributionInstructions.getRunModeProperties();
        runModeProperties.forEach((property, propValue) ->
                beforeChangingValueOfProperty(property, propValue, runMode));
    }

    private void beforeChangingValueOfProperty(String property, String newValue, ConstantLoad runMode) {
        if (property.equals("load") && !runMode.isVariableLoad()) {
            long newLoad = Long.parseLong(newValue);
            long difference = runMode.getLoad() - newLoad;

            if  (difference > 0 ) {
                runMode.removeRunMaps(difference);
            } else {
                runMode.addRunMaps(Math.abs(difference));
            }
        }
    }

    @Override
    public void processRunModeInstructions(RedistributionInstructions redistributionInstructions, ConstantLoad runMode) {
        super.processRunModeInstructions(redistributionInstructions, runMode);

        Map<String, String> runModeProps = redistributionInstructions.getRunModeProperties();
        if (runModeProps.containsKey(CURRENT_LOAD_PROPERTY)) {
            LOG.info("[Driver[ Setting current load to " + runModeProps.get(CURRENT_LOAD_PROPERTY));
            runMode.setCurrentLoad(Integer.parseInt(runModeProps.get(CURRENT_LOAD_PROPERTY)));
        }
    }

    @Override
    public void after(RedistributionInstructions redistributionInstructions, ConstantLoad runMode) {
        if (runMode.isVariableLoad()) {
            runMode.schedulePeriodicTask();
            LOG.info("Periodic task for updating the load was rescheduled with interval " + runMode.getInterval() +
                    " and initial delay " + runMode.getInitialDelay());
        }
    }
}
