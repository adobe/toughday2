package com.adobe.qe.toughday.internal.core.distributedtd.redistribution.runmodes;

import com.adobe.qe.toughday.internal.core.engine.AsyncEngineWorker;
import com.adobe.qe.toughday.internal.core.engine.AsyncTestWorker;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.runmodes.Normal;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.RedistributionInstructions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class NormalRunModeBalancer extends AbstractRunModeBalancer<Normal> {
    protected static final Logger LOG = LogManager.getLogger(Engine.class);

    @Override
    public void before(RedistributionInstructions redistributionInstructions, Normal runMode) {
        if (runMode.isVariableConcurrency()) {
            /* We must cancel the scheduled task for updating concurrency and reschedule it with the new values
             * for interval and initial delay.
             */
            boolean cancelled = runMode.cancelPeriodicTask();
            if (!cancelled) {
                LOG.warn("Periodic task for updating the number of threads could not be cancelled. Normal run mode" +
                        " properties might not be respected.");
            }
        }

        Map<String, String> runModeProperties = redistributionInstructions.getRunModeProperties();
        runModeProperties.forEach((propertyName, propValue) -> this.beforeChangingValueOfProperty(propertyName, propValue, runMode));
    }

    private void beforeChangingValueOfProperty(String property, String newValue, Normal runMode) {
        if (property.equals("concurrency")) {
            long currentConcurrency = runMode.isVariableConcurrency() ? runMode.getActiveThreads() :
                    runMode.getConcurrency();
            long newConcurrency = Long.parseLong(newValue);
            long difference = currentConcurrency - newConcurrency;

            if (difference > 0) {
                reduceConcurrency(difference, runMode);
            } else {
                increaseConcurrency(Math.abs(difference), runMode);
            }

            concurrencySanityChecks(runMode, newConcurrency);
        }
    }

    private void concurrencySanityChecks(Normal runMode, long newConcurrency) {
        // check that we have the exact number of active test workers
        if (runMode.getRunContext().getTestWorkers().size() != newConcurrency) {
            throw new IllegalStateException("[redistribution] TestWorkers size is "
                    + runMode.getRunContext().getTestWorkers().size() + " but" + " new value for concurrency is " + newConcurrency);
        }

        // check that all test workers are active
        if (runMode.getRunContext().getTestWorkers().stream().anyMatch(AsyncEngineWorker::isFinished)) {
            throw new IllegalStateException("[redistribution] " +
                    "There are finished test workers in the list of active workers.");
        }
    }

    private void reduceConcurrency(long reduce, Normal runMode) {
        List<AsyncTestWorker> workerList = new ArrayList<>(runMode.getRunContext().getTestWorkers());

        for (int i = 0; i < reduce; i++) {
            LOG.info("[Agent - redistribution] Finished test worker " + workerList.get(i).getWorkerThread().getId());
            runMode.finishAndDeleteWorker(workerList.get(i));
        }
    }

    private void increaseConcurrency(long increase, Normal runMode) {
        for (int i = 0; i < increase; i++) {
            runMode.createAndExecuteWorker(runMode.getEngine(), runMode.getEngine().getCurrentPhase().getTestSuite());
        }
    }

    @Override
    public void after(RedistributionInstructions redistributionInstructions, Normal runMode) {
        if (runMode.isVariableConcurrency()) {
            runMode.schedulePeriodicTask();
            LOG.info("Task responsible for updating the number threads used to run tests was rescheduled");
        }
    }
}
