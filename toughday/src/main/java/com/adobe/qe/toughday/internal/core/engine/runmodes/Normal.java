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

import com.adobe.qe.toughday.api.core.*;
import com.adobe.qe.toughday.internal.core.*;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.internal.core.engine.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Description(desc = "Runs tests normally.")
public class Normal implements RunMode {

    private static final Logger LOG = LoggerFactory.getLogger(Normal.class);

    public static final String DEFAULT_CONCURRENCY_STRING = "200";
    public static final int DEFAULT_CONCURRENCY = Integer.parseInt(DEFAULT_CONCURRENCY_STRING);

    public static final String DEFAULT_WAIT_TIME_STRING = "300";
    public static final long DEFAULT_WAIT_TIME = Long.parseLong(DEFAULT_WAIT_TIME_STRING);

    private ExecutorService testsExecutorService;

    private final List<AsyncTestWorker> testWorkers = new ArrayList<>();
    private final List<RunMap> runMaps = new ArrayList<>();

    private int concurrency = DEFAULT_CONCURRENCY;
    private long waitTime = DEFAULT_WAIT_TIME;

    @ConfigArgSet(required = false, desc = "The number of concurrent threads that Tough Day will use", defaultValue = DEFAULT_CONCURRENCY_STRING, order = 5)
    public void setConcurrency(String concurrencyString) {
        this.concurrency = Integer.parseInt(concurrencyString);
    }

    @ConfigArgSet(required = false, desc = "The wait time between two consecutive test runs for a specific thread. Expressed in milliseconds",
            defaultValue = DEFAULT_WAIT_TIME_STRING, order = 7)
    public void setWaitTime(String waitTime) {
        this.waitTime = Integer.parseInt(waitTime);
    }

    @ConfigArgGet
    public int getConcurrency() {
        return concurrency;
    }

    @ConfigArgGet
    public long getWaitTime() {
        return waitTime;
    }

    @Override
    public void runTests(Engine engine) throws Exception {
        
        Configuration configuration = engine.getConfiguration();
        TestSuite testSuite = configuration.getTestSuite();
        GlobalArgs globalArgs = configuration.getGlobalArgs();
        testsExecutorService = Executors.newFixedThreadPool(concurrency);

        // Create the test worker threads
        for (int i = 0; i < concurrency; i++) {
            AsyncTestWorkerImpl testWorker = new AsyncTestWorkerImpl(engine, testSuite, engine.getGlobalRunMap().newInstance());
            try {
                testsExecutorService.execute(testWorker);
            } catch (OutOfMemoryError e) {
                LOG.warn("Could not create the required number of threads. Number of created threads : " + String.valueOf(i - 1) + ".");
                break;
            }
            synchronized (testWorkers) {
                testWorkers.add(testWorker);
            }
            synchronized (runMaps) {
                runMaps.add(testWorker.getLocalRunMap());
            }
        }
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
                for(AsyncTestWorker testWorker : testWorkers) {
                    if (!testWorker.isFinished())
                        return false;
                }
                return true;
            }
        };
    }

    @Override
    public void finishExecutionAndAwait() {
        for (AsyncTestWorker testWorker : testWorkers) {
            testWorker.finishExecution();
        }

        boolean allExited = false;
        while(!allExited) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            allExited = true;
            for (AsyncTestWorker testWorker : testWorkers) {
                if (!testWorker.hasExited()) {
                    if(!testWorker.getMutex().tryLock()) {
                        continue;
                    }
                    allExited = false;
                    testWorker.getWorkerThread().interrupt();
                    testWorker.getMutex().unlock();
                }
            }
        }

    }

    @Override
    public ExecutorService getExecutorService() {
        return testsExecutorService;
    }

    private class AsyncTestWorkerImpl extends AsyncTestWorker {
        protected final Engine engine;
        private HashMap<TestId, AbstractTest> localTests;
        private TestSuite testSuite;
        private RunMap localRunMap;
        private boolean exited = false;

        /**
         * Constructor
         *
         * @param engine
         * @param testSuite   the test suite
         * @param localRunMap a deep clone a the global run map.
         */
        public AsyncTestWorkerImpl(Engine engine, TestSuite testSuite, RunMap localRunMap) {
            this.engine = engine;
            this.testSuite = testSuite;
            localTests = new HashMap<>();
            for(AbstractTest test : testSuite.getTests()) {
                AbstractTest localTest = test.clone();
                localTests.put(localTest.getId(), localTest);
            }
            this.localRunMap = localRunMap;
        }

        /**
         * Method for running tests.
         */
        @Override
        public void run() {
            workerThread = Thread.currentThread();
            LOG.debug("Thread running: " + workerThread);
            mutex.lock();
            try {
                while(!isFinished()) {
                    currentTest = Engine.getNextTest(this.testSuite, engine.getCounts(), engine.getEngineSync());
                    // if no test available, finish
                    if (null == currentTest) {
                        LOG.info("Thread " + workerThread + " died! :(");
                        this.finishExecution();
                        continue;
                    }

                    //get the worker's local test to run
                    currentTest = localTests.get(currentTest.getId());

                    // else, continue with the run

                    AbstractTestRunner runner = RunnersContainer.getInstance().getRunner(currentTest);

                    lastTestStart = System.nanoTime();
                    mutex.unlock();
                    try {
                        runner.runTest(currentTest, localRunMap);
                    } catch (Throwable e) {
                        LOG.warn("Exceptions from tests should not reach this point", e);
                    }
                    mutex.lock();
                    Thread.interrupted();
                    Thread.sleep(waitTime);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.error("InterruptedException(s) should not reach this point", e);
            } catch (Throwable e) {
                LOG.error("Unexpected exception caught", e);
            } finally {
                mutex.unlock();
                this.exited = true;
            }
        }

        public RunMap getLocalRunMap() {
            return localRunMap;
        }

        @Override
        public boolean hasExited() {
            return exited;
        }
    }
 }
