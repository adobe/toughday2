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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Async worker for running tests. There will be GlobalArgs.Concurrency test workers.
 */
public abstract class AsyncTestWorker extends AsyncEngineWorker {
    protected static final Logger LOG = LogManager.getLogger(AsyncTestWorker.class);
    protected Thread workerThread;
    protected long lastTestStart;
    protected AbstractTest currentTest;
    protected ReentrantLock mutex;

    /**
     * Constructor
     */
    public AsyncTestWorker() {
        this.mutex = new ReentrantLock();
    }

    /**
     * Getter for the thread running this worker.
     * @return thread running this worker, if the worker has started running, null otherwise.
     */
    public Thread getWorkerThread() {
        return workerThread;
    }

    /**
     * Getter for the nanoTime of the last time a test has started running.
     */
    public long getLastTestStart() {
        return lastTestStart;
    }

    /**
     * Getter for the current running test.
     * @return the running test, or null if no test has started running.
     */
    public AbstractTest getCurrentTest() { return currentTest; }

    /**
     * Getter for the mutex that is only unlocked when the test is running.
     */
    public ReentrantLock getMutex() { return mutex; }

    public abstract boolean hasExited();
}
