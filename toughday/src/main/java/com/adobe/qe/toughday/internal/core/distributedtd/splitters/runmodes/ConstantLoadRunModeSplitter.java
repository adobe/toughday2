package com.adobe.qe.toughday.internal.core.distributedtd.splitters.runmodes;

import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.runmodes.ConstantLoad;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Knows how to split the constant load run mode into multiple run modes to be distributed to the agents
 * running in the cluster.
 */
public class ConstantLoadRunModeSplitter implements RunModeSplitter<ConstantLoad> {

    protected static final Logger LOG = LogManager.getLogger(Engine.class);

    private ConstantLoad setParamsForDistributedRunMode(ConstantLoad runMode, int nrAgents, int rateRemainder,
                                                        int startRemainder, int endRemainder,
                                                        int loadRemainder, int agentId) throws CloneNotSupportedException {
        ConstantLoad clone = (ConstantLoad) runMode.clone();


        if (runMode.isVariableLoad()) {
            if (runMode.getRate() >= nrAgents) {
                clone.setStart(String.valueOf(runMode.getStart()/ nrAgents + startRemainder));
                clone.setEnd(String.valueOf(runMode.getEnd()/ nrAgents + endRemainder));
                clone.setRate(String.valueOf(runMode.getRate()/ nrAgents + rateRemainder));
            } else {
                clone.setInitialDelay(agentId * GlobalArgs.parseDurationToSeconds(runMode.getInterval()) * 1000);
                clone.setOneAgentRate(runMode.getRate());
                clone.setStart(String.valueOf(runMode.getStart()+ agentId * runMode.getRate()));
                clone.setRate(String.valueOf(nrAgents * runMode.getRate()));
                long interval = GlobalArgs.parseDurationToSeconds(runMode.getInterval());
                clone.setInterval(String.valueOf(interval * nrAgents) + 's');
            }

            return clone;
        }

        /* we must distribute the load between the agents */
        clone.setLoad(String.valueOf(runMode.getLoad() / nrAgents + loadRemainder));

        return clone;
    }

    @Override
    public Map<String, ConstantLoad> distributeRunMode(ConstantLoad runMode, List<String> agents)
            throws CloneNotSupportedException {
        int nrAgents = agents.size();
        Map<String, ConstantLoad> runModes = new HashMap<>();

        ConstantLoad firstTask = setParamsForDistributedRunMode(runMode, nrAgents, runMode.getRate() % nrAgents,
                runMode.getStart() % nrAgents, runMode.getEnd() % nrAgents, runMode.getLoad() % nrAgents, 0);
        runModes.put(agents.get(0), firstTask);

        for (int i = 1; i < nrAgents; i++) {
            ConstantLoad task = setParamsForDistributedRunMode(runMode, nrAgents, 0, 0, 0, 0, i);
            runModes.put(agents.get(i), task);
        }

        return runModes;
    }

    @Override
    public Map<String, ConstantLoad> distributeRunModeForRebalancingWork(ConstantLoad runMode, List<String> existingAgents,
                                                                         List<String> newAgents, long phaseStartTime) throws CloneNotSupportedException {
        List<String> agents = new ArrayList<>(existingAgents);
        agents.addAll(newAgents);

        Map<String, ConstantLoad> taskRunModes = distributeRunMode(runMode, agents);
        newAgents.forEach(agentName -> taskRunModes.get(agentName).setStart("0"));

        if (!runMode.isVariableLoad()) {
            return taskRunModes;
        }

        // compute the current load to determine the new values for start/current load
        long endTime = System.currentTimeMillis();
        long diff = (endTime - phaseStartTime) / 1000; // convert to seconds
        int estimatedCurrentLoad = ((int)(diff / GlobalArgs.parseDurationToSeconds(runMode.getInterval())))
                * runMode.getRate() + runMode.getStart();

        LOG.info("Phase was executed for " + diff + " seconds");
        LOG.info("Estimated current load " + estimatedCurrentLoad);

        // set start property for new agents
        newAgents.forEach(agentName -> taskRunModes.get(agentName)
                .setStart(String.valueOf(estimatedCurrentLoad / agents.size())));

        // set current load for old agents
        taskRunModes.get(existingAgents.get(0)).setCurrentLoad(estimatedCurrentLoad / agents.size() +
                 estimatedCurrentLoad % agents.size());
        for (int i = 1; i < existingAgents.size(); i++) {
            taskRunModes.get(existingAgents.get(i)).setCurrentLoad(estimatedCurrentLoad / agents.size());
        }

        return taskRunModes;

    }

}
