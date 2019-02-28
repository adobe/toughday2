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

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.internal.core.TestSuite;

import java.util.Collection;

/**
 * Worker for checking timeout and interrupting test worker threads when timeout is exceeded.
 * It uses Thread.interrupt for letting workers know the
 */
public class AsyncTimeoutChecker extends AsyncEngineWorker {
    private Engine engine;
    private final Thread mainThread;

    /**
     * Constructor.
     */
    public AsyncTimeoutChecker(Engine engine, Thread mainThread) {
        this.engine = engine;
        this.mainThread = mainThread;
    }

    /**
     * Method for checking timeout and interrupting test worker threads if timeout is exceeded.
     * It uses Thread.interrupt for letting worker threads know the timeout has exceeded. Runners must know
     * how to correctly handled all outcomes of a Thread.interrupt see:
     * http://docs.oracle.com/javase/7/docs/api/java/lang/Thread.html#interrupt()
     */
    private void interruptWorkerIfTimeout(AsyncTestWorker worker) {
        AbstractTest currentTest = worker.getCurrentTest();
        if(currentTest == null)
            return;

        Long testTimeout = currentTest.getTimeout();
        long timeout = testTimeout >= 0 ? testTimeout : engine.getGlobalArgs().getTimeoutInSeconds();

        if (!worker.getMutex().tryLock()) {
            /* nothing to interrupt. if the test was running
               the mutex would've been successfully acquired. */
            return;
        }

        try {
            if (((System.nanoTime() - worker.getLastTestStart()) / 1000000l > timeout)
                    && currentTest == worker.getCurrentTest()) {
                worker.getWorkerThread().interrupt();
            }
        } finally {
            worker.getMutex().unlock();
        }
    }

    /**
     * Implementation of Runnable interface.
     */
    @Override
    public void run() {
        try {
            while(!isFinished()) {
                long minTimeout = engine.getCurrentPhase().getTestSuite().getMinTimeout();

                try {
                    Thread.sleep(Math.round(Math.ceil(minTimeout * Engine.TIMEOUT_CHECK_FACTOR)));

                    engine.getCurrentPhaseLock().readLock().lock();
                    RunMode.RunContext context =  engine.getCurrentPhase().getRunMode().getRunContext();
                    Collection<AsyncTestWorker> testWorkers = context.getTestWorkers();
                    synchronized (testWorkers) {
                        for (AsyncTestWorker worker : testWorkers) {
                            interruptWorkerIfTimeout(worker);
                        }
                    }
                    if (context.isRunFinished()) {
                        if(engine.areTestsRunning() && mainThread.getState() == Thread.State.TIMED_WAITING) {
                            mainThread.interrupt();
                        }
                    }
                } finally {
                    engine.getCurrentPhaseLock().readLock().unlock();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Engine.LOG.info ("Timeout thread interrupted");
        } catch (Throwable e) {
            Engine.LOG.error("Unexpected exception caught", e);
        }
    }
}
