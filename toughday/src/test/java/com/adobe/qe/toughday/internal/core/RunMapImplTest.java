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

import com.adobe.qe.toughday.LogFileEraser;
import com.adobe.qe.toughday.api.core.*;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;
import com.adobe.qe.toughday.internal.core.benckmark.AdHocTest;
import com.adobe.qe.toughday.mocks.MockTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class RunMapImplTest {

    private RunMapImpl runMap;

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("logFileName", ".");
    }

    @Before
    public void before() {
        runMap = new RunMapImpl();
    }

    @Test
    public void addTestToRunMap() {
        AbstractTest test = new MockTest();

        runMap.addTest(test);
        Assert.assertEquals("Expected just 1 test", 1, runMap.getTests().size());
        Assert.assertTrue("Test should be in the run map", runMap.getTests().contains(test));
    }

    @Test
    public void addTestsToRunMap() {
        List<AbstractTest> tests = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            AbstractTest test = new MockTest();
            test.setName("Name" + i);

            tests.add(test);
            runMap.addTest(test);
        }

        Assert.assertEquals("Different sizes", tests.size(), runMap.getTests().size());
        int i = 0;
        for(AbstractTest currentTest : runMap.getTests()) {
            Assert.assertEquals("Expected different test", currentTest, tests.get(i++));
        }
    }

    @Test
    public void addTestWithSameIdTwice() {
        TestId id = new UUIDTestId();
        AbstractTest test1 = new MockTest();
        AbstractTest test2 = new MockTest();

        //If two test instances have the same ID, they are equal. One is usually a clone of the other
        test1.setID(id);
        test2.setID(id);

        runMap.addTest(test1);
        runMap.addTest(test2);

        Assert.assertEquals("Expected just 1 test", 1, runMap.getTests().size());
        Assert.assertTrue(runMap.getTests().contains(test1));
        Assert.assertTrue(runMap.getTests().contains(test2));
    }

    @Test
    public void addTestWithAdHocChildren1() {
        AbstractTest test1 = new MockTest();
        AbstractTest test2 = new MockTest();
        AbstractTest test3 = new MockTest();

        AbstractTest child = new AdHocTest(new UUIDTestId(), test2, "Child");
        AbstractTest subchild = new AdHocTest(new UUIDTestId(), child, "SubChild");

        runMap.addTest(test1);
        runMap.addTest(test2);
        runMap.addTest(test3);

        runMap.addTest(child);
        runMap.addTest(subchild);

        AbstractTest[] runMapTests = runMap.getTests().toArray(new AbstractTest[5]);

        Assert.assertEquals(test1, runMapTests[0]);
        Assert.assertEquals(test2, runMapTests[1]);
        Assert.assertEquals(child, runMapTests[2]);
        Assert.assertEquals(subchild, runMapTests[3]);
        Assert.assertEquals(test3, runMapTests[4]);
    }

    @Test
    public void addTestWithAdHocChildren2() {
        AbstractTest test1 = new MockTest();
        AbstractTest test2 = new MockTest();
        AbstractTest test3 = new MockTest();

        AbstractTest child = new AdHocTest(new UUIDTestId(), test2, "Child");
        AbstractTest subchild = new AdHocTest(new UUIDTestId(), child, "SubChild");

        runMap.addTest(test1);
        runMap.addTest(test2);
        runMap.addTest(test3);

        runMap.addTest(subchild);
        runMap.addTest(child);

        AbstractTest[] runMapTests = runMap.getTests().toArray(new AbstractTest[5]);

        Assert.assertEquals(test1, runMapTests[0]);
        Assert.assertEquals(test2, runMapTests[1]);
        Assert.assertEquals(child, runMapTests[2]);
        Assert.assertEquals(subchild, runMapTests[3]);
        Assert.assertEquals(test3, runMapTests[4]);
    }

    @Test
    public void addTestWithAdHocChildren3() {
        AbstractTest test1 = new MockTest();
        AbstractTest test2 = new MockTest();
        AbstractTest test3 = new MockTest();

        AbstractTest child = new AdHocTest(new UUIDTestId(), test3, "Child");
        AbstractTest subchild = new AdHocTest(new UUIDTestId(), child, "SubChild");

        runMap.addTest(test1);
        runMap.addTest(test2);
        runMap.addTest(test3);

        runMap.addTest(subchild);
        runMap.addTest(child);

        AbstractTest[] runMapTests = runMap.getTests().toArray(new AbstractTest[5]);

        Assert.assertEquals(test1, runMapTests[0]);
        Assert.assertEquals(test2, runMapTests[1]);
        Assert.assertEquals(test3, runMapTests[2]);
        Assert.assertEquals(child, runMapTests[3]);
        Assert.assertEquals(subchild, runMapTests[4]);
    }

    @Test
    public void recordPassedTestResult() throws InterruptedException {
        recordResultTest(TestResult.Status.PASSED);
    }

    @Test
    public void recordFailedTestResult() throws InterruptedException {
        recordResultTest(TestResult.Status.FAILED);
    }

    @Test
    public void recordSkippedTestResult() throws InterruptedException {
        recordResultTest(TestResult.Status.SKIPPED);
    }

    private void recordResultTest(TestResult.Status status) throws InterruptedException {
        AbstractTest test = new MockTest();
        TestResult testResult = createTestResult(test, status);
        runMap.addTest(test);

            runMap.record(testResult);

        Assert.assertEquals(1, runMap.getCurrentTestResults().size());
        TestResult[] testResults = runMap.getCurrentTestResults().toArray(new TestResult[1]);
        Assert.assertEquals(testResult, testResults[0]);

        RunMap.TestStatistics testStatistics = runMap.getRecord(test);
        if (status == TestResult.Status.PASSED) {
            Assert.assertEquals(1, testStatistics.getTotalRuns());
            Assert.assertEquals(0, testStatistics.getFailRuns());
            Assert.assertEquals(0, testStatistics.getSkippedRuns());
            Assert.assertTrue(testStatistics.getMinDuration() >= 30 && testStatistics.getMinDuration() < 50);
            Assert.assertTrue(testStatistics.getMaxDuration() >= 40 && testStatistics.getMaxDuration() < 50);
            Assert.assertTrue(testStatistics.getAverageDuration() >= 40 && testStatistics.getAverageDuration() < 50);
            Assert.assertTrue(testStatistics.getMedianDuration() >= 40 && testStatistics.getMedianDuration() < 50);
            Assert.assertTrue(testStatistics.getTotalDuration() >= 40 && testStatistics.getTotalDuration() < 50);
            Assert.assertTrue(testStatistics.getStandardDeviation() < 10);
        } else if (status == TestResult.Status.FAILED) {
            Assert.assertEquals(0, testStatistics.getTotalRuns());
            Assert.assertEquals(1, testStatistics.getFailRuns());
            Assert.assertEquals(0, testStatistics.getSkippedRuns());
        } else if (status == TestResult.Status.SKIPPED) {
            Assert.assertEquals(0, testStatistics.getTotalRuns());
            Assert.assertEquals(0, testStatistics.getFailRuns());
            Assert.assertEquals(1, testStatistics.getSkippedRuns());
        }
    }

    private TestResult createTestResult(AbstractTest test, TestResult.Status status) throws InterruptedException {
        TestResult testResult = new TestResult(test);
        testResult.beginBenchmark();
        Thread.sleep(40);
        testResult.endBenchmark();
        switch (status) {
            case PASSED:
                testResult.markAsPassed();
                break;
            case FAILED:
                testResult.markAsFailed(new Exception());
                break;
            case SKIPPED:
                testResult.markAsSkipped(new SkippedTestException(new Exception()));
                break;
        }

        return testResult;
    }

    @Test
    public void addRecordTestResultsWithEveryStatus() throws InterruptedException {
        AbstractTest test = new MockTest();
        for(TestResult.Status status : TestResult.Status.values()) {
            runMap.record(createTestResult(test, status));
        }

        RunMap.TestStatistics testStatistics = runMap.getRecord(test);
        Assert.assertEquals(1, testStatistics.getTotalRuns());
        Assert.assertEquals(1, testStatistics.getFailRuns());
        Assert.assertEquals(1, testStatistics.getSkippedRuns());
    }

    @Test
    public void recordWithMultiThreading() throws ExecutionException, InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(33);
        AbstractTest test = new MockTest();
        runMap.addTest(test);
        AtomicInteger passed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        final int TOTAL = 9999;
        List<Future<Boolean>> futures = new ArrayList<>();
        boolean callablePassed = true;

        ExecutorService executorService = Executors.newFixedThreadPool(33);
        for (int i = 0; i < TOTAL; i++) {
            final int finalI = i;
            futures.add(executorService.submit(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    try {
                        barrier.await(); //Force result recording to happen in parallel
                        switch (finalI % 3) {
                            case 0:
                                runMap.record(createTestResult(test, TestResult.Status.PASSED));
                                passed.incrementAndGet();
                                break;
                            case 1:
                                runMap.record(createTestResult(test, TestResult.Status.FAILED));
                                failed.incrementAndGet();
                                break;
                            case 2:
                                runMap.record(createTestResult(test, TestResult.Status.SKIPPED));
                                skipped.incrementAndGet();
                                break;
                        }
                    } catch (Exception e) {
                        return false;
                    }

                    return true;
                }
            }));
        }

        for(int i = 0; i < TOTAL; i++) {
            callablePassed = callablePassed && futures.get(i).get();
        }

        Assert.assertTrue("All records were successful", callablePassed);
        Assert.assertEquals(TOTAL, runMap.getCurrentTestResults().size());
        Assert.assertEquals(TOTAL / 3, runMap.getRecord(test).getTotalRuns());
        Assert.assertEquals(TOTAL / 3, runMap.getRecord(test).getFailRuns());
        Assert.assertEquals(TOTAL / 3, runMap.getRecord(test).getSkippedRuns());
    }

    @Test
    public void testAggregateAndReinitialize() throws InterruptedException {
        RunMapImpl secondRunMap = new RunMapImpl();

        AbstractTest test1 = new MockTest();
        AbstractTest test2 = new MockTest();

        runMap.record(createTestResult(test1, TestResult.Status.PASSED));
        runMap.record(createTestResult(test1, TestResult.Status.FAILED));
        runMap.record(createTestResult(test1, TestResult.Status.SKIPPED));
        runMap.record(createTestResult(test2, TestResult.Status.PASSED));
        runMap.record(createTestResult(test2, TestResult.Status.FAILED));
        runMap.record(createTestResult(test2, TestResult.Status.SKIPPED));

        secondRunMap.record(createTestResult(test1, TestResult.Status.PASSED));
        secondRunMap.record(createTestResult(test1, TestResult.Status.FAILED));
        secondRunMap.record(createTestResult(test1, TestResult.Status.SKIPPED));
        secondRunMap.record(createTestResult(test2, TestResult.Status.PASSED));
        secondRunMap.record(createTestResult(test2, TestResult.Status.FAILED));
        secondRunMap.record(createTestResult(test2, TestResult.Status.SKIPPED));

        runMap.aggregateAndReinitialize(secondRunMap);
        Assert.assertEquals(2, runMap.getTests().size());
        Assert.assertTrue(runMap.getTests().contains(test1));
        Assert.assertTrue(runMap.getTests().contains(test2));

        Assert.assertEquals(2, runMap.getRecord(test1).getTotalRuns());
        Assert.assertEquals(2, runMap.getRecord(test1).getSkippedRuns());
        Assert.assertEquals(2, runMap.getRecord(test1).getFailRuns());
        Assert.assertEquals(2, runMap.getRecord(test2).getTotalRuns());
        Assert.assertEquals(2, runMap.getRecord(test2).getSkippedRuns());
        Assert.assertEquals(2, runMap.getRecord(test2).getFailRuns());

        Assert.assertEquals(0, secondRunMap.getRecord(test1).getTotalRuns());
        Assert.assertEquals(0, secondRunMap.getRecord(test1).getSkippedRuns());
        Assert.assertEquals(0, secondRunMap.getRecord(test1).getFailRuns());
        Assert.assertEquals(0, secondRunMap.getRecord(test2).getTotalRuns());
        Assert.assertEquals(0, secondRunMap.getRecord(test2).getSkippedRuns());
        Assert.assertEquals(0, secondRunMap.getRecord(test2).getFailRuns());

        Assert.assertEquals(12, runMap.getCurrentTestResults().size());
        Assert.assertEquals(0, secondRunMap.getCurrentTestResults().size());
    }

    @Test
    public void testClearTestResults() throws InterruptedException {
        AbstractTest test = new MockTest();
        runMap.addTest(test);
        for (int i = 0; i < 100; i++) {
            runMap.record(createTestResult(test, TestResult.Status.PASSED));
        }

        Assert.assertEquals(100, runMap.getCurrentTestResults().size());
        Assert.assertEquals(100, runMap.getRecord(test).getTotalRuns());

        runMap.clearCurrentTestResults();

        Assert.assertEquals(0, runMap.getCurrentTestResults().size());
        Assert.assertEquals(100, runMap.getRecord(test).getTotalRuns());
    }

    @Test
    public void testReinitialize() throws InterruptedException {
        AbstractTest test = new MockTest();
        runMap.addTest(test);
        for (int i = 0; i < 100; i++) {
            runMap.record(createTestResult(test, TestResult.Status.PASSED));
        }

        Assert.assertEquals(100, runMap.getCurrentTestResults().size());
        Assert.assertEquals(100, runMap.getRecord(test).getTotalRuns());

        runMap.reinitialize();

        Assert.assertEquals(0, runMap.getCurrentTestResults().size());
        Assert.assertEquals(0, runMap.getRecord(test).getTotalRuns());
    }

    @Test
    public void testNewInstance() throws InterruptedException {
        AbstractTest test1 = new MockTest();
        AbstractTest test2 = new MockTest();
        AbstractTest test3 = new MockTest();

        runMap.addTest(test1);
        runMap.addTest(test2);
        runMap.addTest(test3);

        runMap.record(createTestResult(test1, TestResult.Status.PASSED));
        runMap.record(createTestResult(test2, TestResult.Status.PASSED));
        runMap.record(createTestResult(test3, TestResult.Status.PASSED));

        RunMapImpl clone = (RunMapImpl) runMap.newInstance();

        Assert.assertEquals(runMap.getTests().size(), clone.getTests().size());
        AbstractTest[] cloneTests = clone.getTests().toArray(new AbstractTest[3]);

        Assert.assertEquals(test1, cloneTests[0]);
        Assert.assertEquals(test2, cloneTests[1]);
        Assert.assertEquals(test3, cloneTests[2]);
        Assert.assertEquals(0, clone.getCurrentTestResults().size());
        Assert.assertEquals(0, clone.getRecord(test1).getTotalRuns());
    }

    @After
    public void deleteLogs()  {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
