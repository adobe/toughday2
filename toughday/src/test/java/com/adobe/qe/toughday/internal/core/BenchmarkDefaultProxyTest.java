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
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.SkippedTestException;
import com.adobe.qe.toughday.api.core.benchmark.Benchmark;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;
import com.adobe.qe.toughday.api.core.config.GlobalArgs;
import com.adobe.qe.toughday.api.core.runnermocks.MockTest;
import com.adobe.qe.toughday.internal.core.benchmarkmocks.MockWorker;
import com.adobe.qe.toughday.internal.core.benckmark.BenchmarkImpl;
import com.adobe.qe.toughday.mocks.MockRunMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

public class BenchmarkDefaultProxyTest {

    private final String TEST_STRING = "TEST_STRING";
    private final String STEP_NAME = "STEP_NAME";

    private Benchmark benchmark;

    private MockRunMap runMap;
    private AbstractTest test;
    private GlobalArgs globalArgs;
    private MockWorker worker;
    private Throwable failThrowable;
    private Throwable skipThrowable;
    private Throwable caughtException;

    @Before
    public void before() {
        benchmark = new BenchmarkImpl();
        runMap = new MockRunMap();
        benchmark.setRunMap(runMap);
        test = new MockTest();
        globalArgs = Mockito.mock(GlobalArgs.class);
        test.setGlobalArgs(globalArgs);
        worker = new MockWorker();
        failThrowable = new Exception();
        skipThrowable = new SkippedTestException(new Exception());
    }

    @Test
    public void testProxyOverload1Pass() throws Throwable {
        benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L, 1);
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L, 1), 50, 10, TestResult.Status.PASSED, null);
    }

    @Test
    public void testProxyOverload1Fail() throws Throwable {
        worker.fail(failThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L, 1);
        } catch (Exception e) {
            this.caughtException = e;
        }
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L, 1), 50, 10, TestResult.Status.FAILED, failThrowable);
    }

    @Test
    public void testProxyOverload1Skip() throws Throwable {
        worker.skip(skipThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L, 1);
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L, 1), 50, 10, TestResult.Status.SKIPPED, skipThrowable);
    }

    @Test
    public void testProxyOverload2Pass() throws Throwable {
        benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L, 1, 2, 3);
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L, 1, 2, 3), 50, 10, TestResult.Status.PASSED, null);
    }

    @Test
    public void testProxyOverload2Fail() throws Throwable {
        worker.fail(failThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L, 1, 2, 3);
        } catch (Exception e) { caughtException = e;}
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L, 1, 2, 3), 50, 10, TestResult.Status.FAILED, failThrowable);
    }

    @Test
    public void testProxyOverload2Skip() throws Throwable {
        worker.fail(skipThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L, 1, 2, 3);
        } catch (Exception e) { caughtException = e;}
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L, 1, 2, 3), 50, 10, TestResult.Status.SKIPPED, skipThrowable);
    }

    @Test
    public void testProxyOverload3Pass() throws Throwable {
        benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L);
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L), 50, 10, TestResult.Status.PASSED, null);
    }

    @Test
    public void testProxyOverload3Fail() throws Throwable {
        worker.fail(failThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L);
        } catch (Exception e) { caughtException = e; }
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L), 50, 10, TestResult.Status.FAILED, failThrowable);
    }

    @Test
    public void testProxyOverload3Skip() throws Throwable {
        worker.skip(skipThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10L);
        } catch (Exception e) { caughtException = e; }
        validate(MockWorker.METHOD3, Arrays.asList(TEST_STRING, 10L), 50, 10, TestResult.Status.SKIPPED, skipThrowable);
    }

    @Test
    public void testProxyOverload5Pass() throws Throwable {
        benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10);
        validate(MockWorker.METHOD2, Arrays.asList(TEST_STRING, 10), 30, 10, TestResult.Status.PASSED, null);
    }

    @Test
    public void testProxyOverload5Fail() throws Throwable {
        worker.fail(failThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10);
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD2, Arrays.asList(TEST_STRING, 10), 30, 10, TestResult.Status.FAILED, failThrowable);
    }

    @Test
    public void testProxyOverload5Skip() throws Throwable {
        worker.skip(skipThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING, 10);
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD2, Arrays.asList(TEST_STRING, 10), 30, 10, TestResult.Status.SKIPPED, skipThrowable);
    }

    @Test
    public void testProxyOverload4Pass() throws Throwable {
        benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING);
        validate(MockWorker.METHOD1, Arrays.asList(TEST_STRING), 10, 10, TestResult.Status.PASSED, null);
    }

    @Test
    public void testProxyOverload4Fail() throws Throwable {
        worker.fail(failThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING);
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD1, Arrays.asList(TEST_STRING), 10, 10, TestResult.Status.FAILED, failThrowable);
    }

    @Test
    public void testProxyOverload4Skip() throws Throwable {
        worker.skip(skipThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(TEST_STRING);
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD1, Arrays.asList(TEST_STRING), 10, 10, TestResult.Status.SKIPPED, skipThrowable);
    }

    @Test
    public void testProxyOverload6Pass() throws Throwable {
        benchmark.measure(test, STEP_NAME, worker).method(1, 2, 3);
        validate(MockWorker.METHOD4, Arrays.asList(1, 2, 3), 70, 10, TestResult.Status.PASSED, null);
    }

    @Test
    public void testProxyOverload6Fail() throws Throwable {
        worker.fail(failThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(1, 2, 3);
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD4, Arrays.asList(1, 2, 3), 70, 10, TestResult.Status.FAILED, failThrowable);
    }

    @Test
    public void testProxyOverload6Skipped() throws Throwable {
        worker.skip(skipThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method(1, 2, 3);
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD4, Arrays.asList(1, 2, 3), 70, 10, TestResult.Status.SKIPPED, skipThrowable);
    }

    @Test
    public void testProxyOverload7Pass() throws Throwable {
        benchmark.measure(test, STEP_NAME, worker).method();
        validate(MockWorker.METHOD5, Arrays.asList(), 90, 10, TestResult.Status.PASSED, null);
    }

    @Test
    public void testProxyOverload7Fail() throws Throwable {
        worker.fail(failThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method();
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD5, Arrays.asList(), 90, 10, TestResult.Status.FAILED, failThrowable);
    }

    @Test
    public void testProxyOverload7Skipped() throws Throwable {
        worker.skip(skipThrowable);
        try {
            benchmark.measure(test, STEP_NAME, worker).method();
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD5, Arrays.asList(), 90, 10, TestResult.Status.SKIPPED, skipThrowable);
    }

    @Test
    public void testProxyOverloadWithReturnPass() throws Throwable {
        Object returnValue = benchmark.measure(test, STEP_NAME, worker).methodWithReturnValue();
        Assert.assertEquals(MockWorker.RETURN_VALUE, returnValue);
        validate(MockWorker.METHOD6, Arrays.asList(), 110, 10, TestResult.Status.PASSED, null);
    }

    @Test
    public void testProxyOverloadWithReturnFail() throws Throwable {
        Object returnValue = null;
        worker.fail(failThrowable);
        try {
           returnValue = benchmark.measure(test, STEP_NAME, worker).methodWithReturnValue();
        } catch (Exception e) {
            caughtException = e;
        }
        Assert.assertNull(returnValue);
        validate(MockWorker.METHOD6, Arrays.asList(), 110, 10, TestResult.Status.FAILED, failThrowable);
    }

    @Test
    public void testProxyOverloadWithReturnSkip() throws Throwable {
        Object returnValue = null;
        worker.skip(skipThrowable);
        try {
            returnValue = benchmark.measure(test, STEP_NAME, worker).methodWithReturnValue();
        } catch (Exception e) {
            caughtException = e;
        }
        Assert.assertNull(returnValue);
        validate(MockWorker.METHOD6, Arrays.asList(), 110, 10, TestResult.Status.SKIPPED, skipThrowable);
    }

    @Test
    public void testProxyInterrupt() throws Throwable {
        worker.interrupt();
        try {
            benchmark.measure(test, STEP_NAME, worker).method(1, 2, 3);
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD4, Arrays.asList(1, 2, 3), 70, 10, TestResult.Status.FAILED, caughtException);
    }

    private void validate(String expectedMethod,
                          List expectedArguments,
                          int expectedDuration,
                          int eps,
                          TestResult.Status expectedStatus,
                          Throwable expectedException) {
        Assert.assertEquals(expectedMethod, worker.getCalledMethod());
        List actualArguments = worker.getArguments();
        Assert.assertEquals(expectedArguments.size(), actualArguments.size());

        for(int i = 0; i < expectedArguments.size(); i++) {
            Assert.assertEquals(expectedArguments.get(i), actualArguments.get(i));
        }

        Assert.assertEquals(1, runMap.getResults().size());
        TestResult result = runMap.getResults().get(0);

        Assert.assertTrue("Duration is not in range", result.getDuration() >= expectedDuration - eps && result.getDuration() < expectedDuration + eps);
        Assert.assertEquals("Incorrect name", test.getFullName() + "." + STEP_NAME, result.getTest().getFullName());
        Assert.assertEquals("Incorrect Thread ID", Thread.currentThread().getId(), result.getThreadId());
        Assert.assertEquals("Incorrect Thread Name", Thread.currentThread().getName(), result.getThreadName());
        Assert.assertEquals("Incorrect Status", expectedStatus, result.getStatus());
        if(expectedException != null) {
            Assert.assertEquals("Incorrect exception thrown", expectedException, caughtException);
        } else {
            Assert.assertNull("No exception should be thrown", caughtException);
        }
    }

    @After
    public void deleteLogs()  {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
