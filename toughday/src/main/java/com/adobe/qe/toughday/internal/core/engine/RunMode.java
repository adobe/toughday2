/*
Copyright 2015 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.internal.core.engine;

import com.adobe.qe.toughday.api.core.RunMap;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.runmodes.RunModeBalancer;
import com.adobe.qe.toughday.internal.core.distributedtd.splitters.runmodes.RunModeSplitter;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

public interface RunMode {
    void runTests(Engine engine) throws Exception;
    void finishExecutionAndAwait();
    ExecutorService getExecutorService();
    RunContext getRunContext();

    /**
     * Returns the RunModeSplitter instance which knows how to split the run mode for running ToughDay distributed.
     * @param <T> type of the run mode.
     */
    <T extends RunMode> RunModeSplitter<T> getRunModeSplitter();

    /**
     * Returns the RunModeBalancer instance which knows how to process the redistribution instructions received from
     * the driver when the number of agents running in the cluster modifies and the work must be redistributed.
     * @param <T> type of the run node.
     */
    <T extends RunMode> RunModeBalancer<T> getRunModeBalancer();

    interface RunContext {
        Collection<AsyncTestWorker> getTestWorkers();
        Collection<RunMap> getRunMaps();
        boolean isRunFinished();
    }
}
