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
package com.adobe.qe.toughday.api.core.benchmark;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.RunMap;
import com.adobe.qe.toughday.api.core.SkippedTestException;
import com.adobe.qe.toughday.api.annotations.labels.Nullable;

import java.util.Date;

/**
 * Holds the benchmark information for one test run.
 * @param <K> The type of the additional data
 * Typical usage:
 * <pre>
 *     <code>
 *        ...
 *        TestResult<MyDataClass> testResult = new TestResult(myTest)
 *          .beginBenchmark();
 *
 *        ... //test execution
 *
 *        testResult.endBenchmark();
 *
 *        // SkippedException?
 *        testResult.markAsSkipped(e);
 *        // Other Throwable or Thread interruption occurred?
 *        testResult.markAsFailed(e);
 *
 *        testResult.withData(myDataObject);
 *     </code>
 * </pre>
 */
public class TestResult<K> {
    public enum Status {
        PASSED, FAILED, SKIPPED
    }

    private AbstractTest test;
    private String testFullName; //cached value
    private long startMillis = -1;
    private long startNano = -1;
    private long endMillis = -1;
    private String threadName;
    private long threadId;
    private boolean showInAggregatedView = true;
    private @Nullable K data;

    private double duration = Double.NaN;
    private @Nullable SkippedTestException skippedCause;
    private @Nullable Throwable failCause;

    private Status status = Status.PASSED;

    public TestResult(AbstractTest test) {
        withThread(Thread.currentThread());
        this.test = test;
        this.testFullName = test.getFullName();
    }

    /**
     * Getter for the test
     */
    public AbstractTest getTest() {
        return test;
    }

    /**
     * Getter for the test's name
     */
    public String getTestFullName() {
        return testFullName;
    }

    /**
     * Getter for the duration
     * @return The duration if the benchmarking operation finished. NaN otherwise
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Getter for the skip cause
     */
    public @Nullable SkippedTestException getSkippedCause() { return skippedCause; }

    /**
     * Getter for the skip cause
     */
    public @Nullable Throwable getFailCause() { return failCause; }

    /**
     * Getter for the formatted start timestamp
     */
    public String getFormattedStartTimestamp() {
        return RunMap.TIME_STAMP_FORMAT.format(new Date(startMillis));
    }

    /**
     * Getter for the formatted end timestamp
     */
    public String getFormattedEndTimestamp() {
        return RunMap.TIME_STAMP_FORMAT.format(new Date(endMillis));
    }

    /**
     * Getter for the raw start timestamp in millis
     */
    public long getStartTimestamp() {
        return this.startMillis;
    }

    /**
     * Getter for the raw end timestamp in millis
     */
    public long getEndTimestamp() {
        return this.endMillis;
    }

    /**
     * Getter for the Thread's name
     */
    public String getThreadName() {
        return threadName;
    }

    /**
     * Getter for the Thread's id
     */
    public long getThreadId() {
        return threadId;
    }

    /**
     * Getter for the data
     */
    public @Nullable K getData() {
        return data;
    }

    /**
     * Getter for the status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * Getter for whether to show in the aggregated view or not
     */
    public boolean isShowInAggregatedView() {
        return showInAggregatedView;
    }

    /**
     * Setter for whether to show in the aggregated view or not
     * @param showInAggregatedView
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T withShowInAggregatedView(boolean showInAggregatedView) {
        this.showInAggregatedView = showInAggregatedView;
        return (T) this;
    }

    /**
     * Setter for the thread
     * @param t the thread
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T withThread(Thread t) {
        this.threadName = t.getName();
        this.threadId = t.getId();
        return (T) this;
    }

    /**
     * Mark the test result as passed
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T markAsPassed() {
        this.status = Status.PASSED;
        return (T) this;
    }

    /**
     * Mark the test result as failed
     * @param e the cause of failing
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T markAsFailed(Throwable e) {
        this.failCause = e;
        this.status = Status.FAILED;
        return (T) this;
    }

    /**
     * Mark the test result as failed
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T markAsFailed() {
        return markAsFailed(null);
    }

    /**
     * Mark the test result as skipped
     * @param e the cause of skipping
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T markAsSkipped(SkippedTestException e) {
        this.skippedCause = e;
        this.status = Status.SKIPPED;
        return (T) this;
    }

    /**
     * Mark the test result as skipped
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T markAsSkipped() {
        return markAsSkipped(null);
    }

    /**
     * Add additional data to the test result
     * @param data additional data
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T withData(K data) {
        this.data = data;
        return (T) this;
    }

    /**
     * Begin the benchmarking. Records the thread and the timestamp
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T beginBenchmark() {
        withThread(Thread.currentThread());
        withStartTimestamp(System.currentTimeMillis());
        this.startNano = System.nanoTime();
        return (T) this;
    }

    /**
     * Conclude the benchmarking. Records the end timestamp and the duration. Requires {@code beginBenchmark} to be executed before.
     * @param <T> return type
     * @return Builder pattern. Returns {@code this}
     */
    public <T extends TestResult<K>> T endBenchmark() {
        if(startNano == -1)
            throw new IllegalStateException("beginBenchmark() was not called");

        withEndTimestamp(System.currentTimeMillis());
        withDuration(milliDurationFromNano(startNano, System.nanoTime()));
        return (T) this;
    }

    private void withDuration(double duration) {
        this.duration = duration;
    }

    private void withStartTimestamp(long startMillis) {
        this.startMillis = startMillis;
    }

    private void withEndTimestamp(long endMillis) {
        this.endMillis = endMillis;
    }

    /**
     * Converts a duration from nano seconds to milliseconds
     * @param start the start nano time
     * @param end the end nano time
     * @return the duration between {@code start} and {@code end} in milliseconds
     */
    public static double milliDurationFromNano(long start, long end) {
        return ((end - start)/1000000l);
    }
}
