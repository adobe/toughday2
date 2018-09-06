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
package com.adobe.qe.toughday.internal.core.engine.runmodes;

import com.adobe.qe.toughday.api.annotations.labels.NotNull;
import com.adobe.qe.toughday.api.annotations.labels.Nullable;
import com.adobe.qe.toughday.api.core.*;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.internal.core.TestSuite;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.engine.AsyncEngineWorker;
import com.adobe.qe.toughday.internal.core.engine.AsyncTestWorker;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Description(desc = "Generates a constant load of test executions, regardless of their execution time.")
public class ConstantLoad implements RunMode {
    private static final Logger LOG = LoggerFactory.getLogger(ConstantLoad.class);

    private static final String DEFAULT_LOAD_STRING = "50";
    private static final int DEFAULT_LOAD = Integer.parseInt(DEFAULT_LOAD_STRING);
    private AtomicBoolean loggedWarning = new AtomicBoolean(false);

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private Collection<AsyncTestWorker> testWorkers = Collections.synchronizedSet(new HashSet<AsyncTestWorker>());
    private AsyncTestWorkerScheduler scheduler;
    private final List<RunMap> runMaps = new ArrayList<>();
    private int load = DEFAULT_LOAD;
    private TestCache testCache;

    @ConfigArgSet(required = false, defaultValue = DEFAULT_LOAD_STRING, desc = "Set the load, in requests per second for the \"constantload\" runmode.")
    public void setLoad(String load) { this.load = Integer.parseInt(load); }

    @ConfigArgGet
    public int getLoad() { return this.load; }

    private static class TestCache {
        public Map<TestId, Queue<AbstractTest>> cache = new HashMap<>();

        public TestCache(TestSuite testSuite) {
            for(AbstractTest test : testSuite.getTests()) {
                cache.put(test.getId(), new ConcurrentLinkedQueue());
            }
        }

        public void add(@NotNull AbstractTest test) {
            cache.get(test.getId()).add(test);
        }

        public @Nullable AbstractTest getCachedValue(@NotNull TestId testID) {
            return cache.get(testID).poll();
        }
    }

    @Override
    public void runTests(Engine engine) throws Exception {
        Configuration configuration = engine.getConfiguration();
        TestSuite testSuite = configuration.getTestSuite();
        this.testCache = new TestCache(testSuite);

        for(int i = 0; i < load; i++) {
            synchronized (runMaps) {
                runMaps.add(engine.getGlobalRunMap().newInstance());
            }
        }

        this.scheduler = new AsyncTestWorkerScheduler(engine);
        executorService.execute(scheduler);
    }

    public RunContext getRunContext() {
        return new RunContext() {
            @Override
            public Collection<AsyncTestWorker> getTestWorkers() {
                return testWorkers;
            }

            @Override
            public Collection<RunMap> getRunMaps() {
                return runMaps;
            }

            @Override
            public boolean isRunFinished() {
                return scheduler.isFinished();
            }
        };
    }

    @Override
    public void finishExecutionAndAwait() {
        scheduler.finishExecution();

        synchronized (testWorkers) {
            for(AsyncTestWorker testWorker : testWorkers) {
                testWorker.finishExecution();
            }
        }

        boolean allExited = false;
        while(!allExited) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
            allExited = true;

            synchronized (testWorkers) {
                for (AsyncTestWorker testWorker : testWorkers) {
                    if (!testWorker.hasExited()) {
                        if(!testWorker.getMutex().tryLock())
                            continue;
                        allExited = false;
                        testWorker.getWorkerThread().interrupt();
                        testWorker.getMutex().unlock();
                    }
                }
            }
        }

    }

    private class AsyncTestWorkerImpl extends AsyncTestWorker {
        private AbstractTest test;
        private RunMap runMap;
        private boolean exited = false;

        public AsyncTestWorkerImpl(AbstractTest test, RunMap runMap) {
            this.test = test;
            this.runMap = runMap;
        }

        @Override
        public void run() {
            mutex.lock();
            lastTestStart = System.nanoTime();
            workerThread = Thread.currentThread();
            currentTest = test;
            mutex.unlock();
            try {
                AbstractTestRunner runner = RunnersContainer.getInstance().getRunner(test);
                runner.runTest(test, runMap);
            } catch (Throwable e) {
                LOG.warn("Exceptions from tests should not reach this point", e);
            }

            mutex.lock();
            currentTest = null;
            exited = true;
            testCache.add(test);
            Thread.interrupted();
            mutex.unlock();
        }

        @Override
        public boolean hasExited() {
            return exited;
        }
    }


    private class AsyncTestWorkerScheduler extends AsyncEngineWorker {
        private Engine engine;
        public AsyncTestWorkerScheduler(Engine engine) {
            this.engine = engine;
        }

        @Override
        public void run() {
            try {
                while (!isFinished()) {
                    ArrayList<AbstractTest> nextRound = new ArrayList<>();
                    long start = System.nanoTime();
                    for (int i = 0; i < load; i++) {
                        AbstractTest nextTest = Engine.getNextTest(engine.getConfiguration().getTestSuite(),
                                engine.getCounts(),
                                engine.getEngineSync());
                        if (null == nextTest) {
                            LOG.info("Constant load scheduler thread finished, because there were no more tests to execute.");
                            this.finishExecution();
                            return;
                        }

                        //Use a cache test if available
                        AbstractTest localNextTest = testCache.getCachedValue(nextTest.getId());
                        if(localNextTest == null) {
                            localNextTest = nextTest.clone();
                        }

                        nextRound.add(localNextTest);
                    }


                    for (int i = 0; i < load && !isFinished(); i++) {
                        AsyncTestWorkerImpl worker = new AsyncTestWorkerImpl(nextRound.get(i), runMaps.get(i));
                        try {
                            executorService.execute(worker);
                        } catch (OutOfMemoryError e) {
                            if (!loggedWarning.getAndSet(true)) {
                                LOG.warn("The desired load could not be achieved. We are creating as many threads as possible.");
                            }
                            break;
                        }
                        synchronized (testWorkers) {
                            testWorkers.add(worker);
                        }
                    }

                    //TODO use this
                    long elapsed = System.nanoTime() - start;
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                finishExecution();
                LOG.warn("Constant load scheduler thread was interrupted.");
            }
        }
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }
}
