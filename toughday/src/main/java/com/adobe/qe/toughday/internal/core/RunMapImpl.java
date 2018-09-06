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
package com.adobe.qe.toughday.internal.core;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.RunMap;
import com.adobe.qe.toughday.api.core.SkippedTestException;
import com.adobe.qe.toughday.internal.core.benckmark.AdHocTest;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;
import org.HdrHistogram.SynchronizedHistogram;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Triple;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Map for storing benchmarks. Thread safe for benchmarking operations. Not thread safe for  adding and removing tests.
 */
public class RunMapImpl implements RunMap {

    private long startNanoTime;
    private long startMillisTime;
    /*
        The map should remain unordered (hash map) for faster access, that is why we are using a second
        data structure - list - to keep the order of the tests in output, just for the global run map.
        For now this is an internal implementation detail, we don't need to expose this unless
        a future run mode would require it.
     */
    private Map<AbstractTest, TestEntry> runMap;
    private ReadWriteLock runMapLock = new ReentrantReadWriteLock();
    private List<AbstractTest> orderedTests;

    /*
        This is not the usual read/write scenario, actually quite the opposite, we want concurrent writes, exclusive read.
        We want to allow concurrent writes in the test results collection, while threads execute tests - use readLock
        We want that when the aggregator reads the results, nobody is writing new results              - use writeLock
     */
    private ReadWriteLock testResultsLock = new ReentrantReadWriteLock();
    private ConcurrentLinkedQueue<TestResult> currentTestResults = new ConcurrentLinkedQueue<>();

    public RunMapImpl() {
        runMap = new HashMap<>();
        orderedTests = Collections.synchronizedList(new ArrayList<>());
    }

    private RunMapImpl (List<AbstractTest> orderedTests) {
        this();
        this.orderedTests.addAll(orderedTests);
        for (AbstractTest test : orderedTests) {
            runMap.put(test, new TestEntry(test));
        }
    }

    public void addTest(AbstractTest test) {
        runMapLock.writeLock().lock();
        try {
            //This test was already added by a previous thread
            if(runMap.containsKey(test)) {
                return;
            }
            TestEntry entry = new TestEntry(test);
            runMap.put(test, entry);
            if (test instanceof AdHocTest) {
                insertPartiallyOrderedAdHocTest(test);
            } else {
                orderedTests.add(test);
            }
        } finally {
            runMapLock.writeLock().unlock();
        }
    }


    public TestStatistics getRecord(AbstractTest test) {
        runMapLock.readLock().lock();
        try {
            return runMap.get(test);
        } finally {
            runMapLock.readLock().unlock();
        }
    }

    public Collection<TestResult> getCurrentTestResults() {
        return currentTestResults;
    }

    /**
     * Returns a list that contains all tests(including the child tests of a composite test) in the exact order in which they were
     * added to the suite.
     * @return
     */
    public Collection<AbstractTest> getTests() {
        return orderedTests;
    }

    public void record(TestResult testResult) {
        testResultsLock.readLock().lock();
        currentTestResults.add(testResult);
        testResultsLock.readLock().unlock();

        runMapLock.readLock().lock();
        try {
            if (testResult.isShowInAggregatedView()) {
                AbstractTest test = testResult.getTest();
                TestEntry entry = runMap.get(test);
                if (entry == null) {
                    runMapLock.readLock().unlock();
                    addTest(test);
                    runMapLock.readLock().lock();
                    entry = runMap.get(test);
                }
                entry.record(testResult);
            }
        } finally {
            runMapLock.readLock().unlock();
        }
    }

    public Map<AbstractTest, Long> aggregateAndReinitialize(RunMap otherRunmap) {
        RunMapImpl other = (RunMapImpl) otherRunmap;
        other.testResultsLock.writeLock().lock();
        try {
            this.testResultsLock.writeLock().lock();
            try {
                TestResult testResult = other.currentTestResults.poll();
                while (testResult != null) {
                    this.currentTestResults.add(testResult);
                    testResult = other.currentTestResults.poll();
                }
            } finally {
                this.testResultsLock.writeLock().unlock();
            }
        } finally {
            other.testResultsLock.writeLock().unlock();
        }

        this.runMapLock.writeLock().lock();
        try {
            other.runMapLock.writeLock().lock();
            try {
                Map<AbstractTest, Long> counts = new HashMap<>();
                synchronized (orderedTests) {
                    for (AbstractTest test : other.orderedTests) {
                        TestEntry otherEntry = other.runMap.get(test);
                        TestEntry thisEntry = this.runMap.get(test);
                        if (thisEntry == null) {
                            addTest(test.clone());
                            thisEntry = this.runMap.get(test);
                        }
                        long count = thisEntry.aggregateAndReinitialize(otherEntry);
                        counts.put(test, count);
                    }
                }
                return counts;
            } finally {
                other.runMapLock.writeLock().unlock();
            }
        } finally {
            this.runMapLock.writeLock().unlock();
        }
    }

    public void reinitialize() {
        runMapLock.writeLock().lock();
        try {
            for (TestEntry testEntry : runMap.values()) {
                testEntry.init();
                testEntry.reinitTime();
            }
            clearCurrentTestResults();
        } finally {
            runMapLock.writeLock().unlock();
        }
    }

    //TODO refactor this
    public void reinitStartTimes() {
        runMapLock.writeLock().lock();
        try {
            this.startNanoTime = System.nanoTime();
            this.startMillisTime = System.currentTimeMillis();
            for (TestEntry entry : runMap.values()) {
                entry.reinitTime();
            }
        } finally {
            runMapLock.writeLock().unlock();
        }
    }

    public void clearCurrentTestResults() {
        try {
            runMapLock.writeLock().lock();

            currentTestResults.clear();
        } finally {
            runMapLock.writeLock().unlock();
        }
    }

    public RunMap newInstance() {
        try {
            runMapLock.readLock().lock();

            return new RunMapImpl(orderedTests);
        } finally {
            runMapLock.readLock().unlock();
        }
    }


    /**
     * TODO: Optimize this?
     * @param test
     */
    private void insertPartiallyOrderedAdHocTest(AbstractTest test) {
        //Shouldn't happen, but better safe than sorry
        if(test.getParent() == null) {
            orderedTests.add(test);
            return;
        }

        int ancestorLevel = -1;
        int index = -1;
        int i = 0;
        for(AbstractTest other : orderedTests) {
            i++;
            Triple<AbstractTest, Integer, Integer> commonAncestorAndLevel = lowestCommonAncestor(test, other);
            if(commonAncestorAndLevel.getLeft() != null &&
                    (commonAncestorAndLevel.getMiddle() > ancestorLevel || (commonAncestorAndLevel.getRight() >= 0 && commonAncestorAndLevel.getMiddle() == ancestorLevel))) {
                index = i + (commonAncestorAndLevel.getRight() < 0 ? -1 : 0);
                ancestorLevel = commonAncestorAndLevel.getMiddle();
            }
        }

        if(index != -1 && index < orderedTests.size()) {
            orderedTests.add(index, test);
        } else {
            orderedTests.add(test);
        }
    }

    private Triple<AbstractTest, Integer, Integer> lowestCommonAncestor(AbstractTest test1, AbstractTest test2) {
        LinkedList<AbstractTest> test1Ancestors = new LinkedList<>();
        LinkedList<AbstractTest> test2Ancestors = new LinkedList<>();
        for(AbstractTest p = test1; p != null; p = p.getParent()) {
            test1Ancestors.addFirst(p);
        }
        int test1AncestorsSize = test1Ancestors.size();
        if(test1AncestorsSize == 0) {
            return new ImmutableTriple<>(null, -1, 0);
        }

        for(AbstractTest p = test2; p != null; p = p.getParent()) {
            test2Ancestors.addFirst(p);
        }

        int test2AncestorsSize = test2Ancestors.size();
        if(test2AncestorsSize == 0) {
            return new ImmutableTriple<>(null, -1, 0);
        }

        //No common ancestor
        if(!test1Ancestors.peekFirst().equals(test2Ancestors.peekFirst())) {
            return new ImmutableTriple<>(null, -1, 0);
        }

        int level = 0;
        AbstractTest lowestCommonAncestor = null;
        while (test1Ancestors.peekFirst() != null && test2Ancestors.peekFirst() != null && test1Ancestors.peekFirst().equals(test2Ancestors.peekFirst())) {
            ++level;
            lowestCommonAncestor = test1Ancestors.removeFirst();
            test2Ancestors.removeFirst();
        }
        return new ImmutableTriple<>(lowestCommonAncestor, level, level == test1AncestorsSize ? -1 : 1);
    }


    /**
     * A test statistics entry
     */
    public class TestEntry implements TestStatistics {
        public static final double ONE_BILLION_D = 1000 * 1000 * 1000.0d;
        private static final long ONE_MILION = 1000000;
        private AbstractTest test;
        private double totalDuration;
        private long failRuns;
        private long skippedRuns;
        private Map<Class<? extends Throwable>, Long> failsMap;
        private long lastNanoTime;
        private SynchronizedHistogram histogram;

        private void init() {
            totalDuration = 0;
            failRuns = 0;
            skippedRuns = 0;
            histogram.reset();
            failsMap = new HashMap<>();
        }

        /**
         *
         * @param test
         */
        public TestEntry(AbstractTest test) {
            this.test = test;
            histogram = new SynchronizedHistogram(3600000L /* 1h */, 3);
            reinitTime();
            init();
        }


        public synchronized void record(TestResult testResult) {
            switch (testResult.getStatus()) {
                case PASSED:
                    recordRun(testResult.getDuration());
                    break;
                case SKIPPED:
                    recordSkipped(testResult.getSkippedCause());
                    break;
                case FAILED:
                    recordFail(testResult.getFailCause());
                    break;
            }
        }

        /**
         * Mark a skipped run
         * @param e
         */
        public synchronized void recordSkipped(SkippedTestException e) {
            lastNanoTime = System.nanoTime();
            skippedRuns++;
        }

        /**
         * Mark a failed run
         * @param e
         */
        public synchronized void recordFail(Throwable e) {
            lastNanoTime = System.nanoTime();
            if (!failsMap.containsKey(e.getClass())) {
                failsMap.put(e.getClass(), 0L);
            }
            failsMap.put(e.getClass(), failsMap.get(e.getClass()) + 1);

            failRuns++;
        }

        /**
         * Record numbers for a successful run
         * @param duration
         */
        public synchronized void recordRun(double duration) {
            long endTimestamp = System.currentTimeMillis();
            histogram.recordValue((long) duration);
            lastNanoTime = System.nanoTime();
            totalDuration += duration;
        }

        //TODO refactor this?
        public synchronized void reinitTime() {
            this.lastNanoTime = System.nanoTime();
        }

        @Override
        public AbstractTest getTest() {
            return test;
        }

        @Override
        public String getTimestamp() {
            return TIME_STAMP_FORMAT.format(new Date(startMillisTime + ((lastNanoTime - startNanoTime) / ONE_MILION)));
        }

        @Override
        public double getTotalDuration() {
            return totalDuration;
        }

        @Override
        public long getTotalRuns() {
            return histogram.getTotalCount();
        }

        @Override
        public double getRealThroughput() {
            return ((double) histogram.getTotalCount() * ONE_BILLION_D) / (lastNanoTime - startNanoTime);
        }

        /*
        @Override
        public double getExecutionThroughput() {
            return 1000 * threads / (getAverageDuration() + test.getGlobalArgs().getWaitTime());
        }*/

        @Override
        public long getMinDuration() {
            return histogram.getMinValue();
        }

        @Override
        public long getMaxDuration() {
            return histogram.getMaxValue();
        }

        @Override
        public double getAverageDuration() {
            return histogram.getMean();
        }

        @Override
        public long getFailRuns() {
            return failRuns;
        }

        @Override
        public long getSkippedRuns() {
            return skippedRuns;
        }

        public long getValueAtPercentile(double percentile) {
            return histogram.getValueAtPercentile(percentile);
        }

        @Override
        public double getStandardDeviation() {
            return histogram.getStdDeviation();
        }

        /* TODO delete if we don't find a use case, or figure out how to compute it when the number of threads is not fixed
        public double getDurationPerUser() {
            return totalDuration / threads;
        }*/

        @Override
        public long getMedianDuration() {
            return histogram.getValueAtPercentile(50);
        }

        public synchronized long aggregateAndReinitialize(TestEntry other) {
            long totalRuns = 0;
            synchronized (other) {
                totalRuns = other.histogram.getTotalCount();
                this.histogram.add(other.histogram);
                this.lastNanoTime = Math.max(this.lastNanoTime, other.lastNanoTime);
                this.totalDuration += other.totalDuration;
                this.failRuns += other.failRuns;
                this.skippedRuns += other.skippedRuns;
                other.init();
            }
            return totalRuns;
        }
    }
}