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
import com.adobe.qe.toughday.internal.core.benckmark.BenchmarkImpl;
import com.adobe.qe.toughday.mocks.MockRunMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

public class BenchmarkLambdasTest {

    private static final String STEP_NAME = "STEP_NAME";
    private static final Object RETURN_VALUE = "RETURN_VALUE";
    private static final int SLEEP_TIME = 40;
    private static final int EPS = 10;
    private static final Throwable FAIL_EXCEPTION = new Exception();
    private static final Throwable SKIP_EXCEPTION = new SkippedTestException(new Exception());
    private static final Map DATA = Collections.singletonMap("KEY", "VALUE");

    private Benchmark benchmark;

    private MockRunMap runMap;
    private AbstractTest test;
    private GlobalArgs globalArgs;
    private Throwable caughtException;


    @Before
    public void before() {
        benchmark = new BenchmarkImpl();
        runMap = new MockRunMap();
        benchmark.setRunMap(runMap);
        test = new MockTest();
        globalArgs = Mockito.mock(GlobalArgs.class);
        test.setGlobalArgs(globalArgs);
    }

    @Test
    public void testSimpleLambdaPass() throws Throwable {
        benchmark.measure(test, STEP_NAME, () -> {
            Thread.sleep(SLEEP_TIME);
        });
        validate(SLEEP_TIME, EPS, TestResult.Status.PASSED, null, null);
    }

    @Test
    public void testSimpleLambdaFail() throws Throwable {

        try {
            benchmark.measure(test, STEP_NAME, () -> {
                Thread.sleep(SLEEP_TIME);
                throw FAIL_EXCEPTION;
            });
        } catch (Exception e) {
            caughtException = e;
        }

        validate(SLEEP_TIME, EPS, TestResult.Status.FAILED, null, FAIL_EXCEPTION);
    }

    @Test
    public void testSimpleLambdaSkip() throws Throwable {
        try {
            benchmark.measure(test, STEP_NAME, () -> {
                Thread.sleep(SLEEP_TIME);
                throw SKIP_EXCEPTION;
            });
        } catch (Exception e) {
            caughtException = e;
        }

        validate(SLEEP_TIME, EPS, TestResult.Status.SKIPPED, null, SKIP_EXCEPTION);
    }

    @Test
    public void testLambdaWithReturnValuePass() throws Throwable {
        Object returnedValue = benchmark.measure(test, STEP_NAME, () -> {
            Thread.sleep(SLEEP_TIME);
            return RETURN_VALUE;
        });

        Assert.assertEquals("Unexpected return value", RETURN_VALUE, returnedValue);
        validate(SLEEP_TIME, EPS, TestResult.Status.PASSED, null, null);
    }

    @Test
    public void testLambdaWithReturnValueFail() throws Throwable {
        Object returnValue = null;

        try {
            returnValue = benchmark.measure(test, STEP_NAME, () -> {
                Thread.sleep(SLEEP_TIME);
                if(true) throw FAIL_EXCEPTION;
                return RETURN_VALUE;
            });
        } catch (Exception e) {
            caughtException = e;
        }

        Assert.assertNull("Return value should be null", returnValue);
        validate(SLEEP_TIME, EPS, TestResult.Status.FAILED, null,FAIL_EXCEPTION);
    }

    @Test
    public void testLambdaWithReturnValueSkip() throws Throwable {
        Object returnValue = null;

        try {
            returnValue = benchmark.measure(test, STEP_NAME, () -> {
                Thread.sleep(SLEEP_TIME);
                if(true) throw SKIP_EXCEPTION;
                return RETURN_VALUE;
            });
        } catch (Exception e) {
            caughtException = e;
        }

        Assert.assertNull("Return value should be null", returnValue);
        validate(SLEEP_TIME, EPS, TestResult.Status.SKIPPED, null,SKIP_EXCEPTION);
    }

    @Test
    public void testInjectLambdaPass() throws Throwable {
        Object returnValue = benchmark.measure(test, STEP_NAME, (TestResult<Map> result) -> {
            result.withData(DATA);
            Thread.sleep(SLEEP_TIME);
            return RETURN_VALUE;
        });

        Assert.assertEquals(RETURN_VALUE, returnValue);
        validate(SLEEP_TIME, EPS, TestResult.Status.PASSED, DATA, null);
    }

    @Test
    public void testInjectLambdaFail() throws Throwable {
        Object returnValue = null;

        try {
            returnValue = benchmark.measure(test, STEP_NAME, (TestResult<Map> result) -> {
                result.withData(DATA);
                Thread.sleep(SLEEP_TIME);
                if(true) throw FAIL_EXCEPTION;
                return RETURN_VALUE;
            });
        } catch (Exception e) {
            caughtException = e;
        }

        Assert.assertNull("Return value should be null", returnValue);
        validate(SLEEP_TIME, EPS, TestResult.Status.FAILED, DATA, FAIL_EXCEPTION);
    }

    @Test
    public void testInjectLambdaSkip() throws Throwable {
        Object returnValue = null;

        try {
            returnValue = benchmark.measure(test, STEP_NAME, (TestResult<Map> result) -> {
                result.withData(DATA);
                Thread.sleep(SLEEP_TIME);
                if(true) throw SKIP_EXCEPTION;
                return RETURN_VALUE;
            });
        } catch (Exception e) {
            caughtException = e;
        }

        Assert.assertNull("Return value should be null", returnValue);
        validate(SLEEP_TIME, EPS, TestResult.Status.SKIPPED, DATA, SKIP_EXCEPTION);
    }

    @Test
    public void testInjectVoidLambdaPass() throws Throwable {
        benchmark.measure(test, STEP_NAME, (TestResult<Map> result) -> {
            result.withData(DATA);
            Thread.sleep(SLEEP_TIME);
        });
        validate(SLEEP_TIME, EPS, TestResult.Status.PASSED, DATA, null);
    }

    @Test
    public void testLambdaInterruptThread() throws Throwable {
        Object returnValue = null;

        try {
             returnValue = benchmark.measure(test, STEP_NAME, (TestResult<Map> result) -> {
                result.withData(DATA);
                Thread.sleep(SLEEP_TIME);
                Thread.currentThread().interrupt();
                return RETURN_VALUE;
            });
        } catch (Exception e) {
            caughtException = e;
        }

        Assert.assertNull(returnValue);
        validate(SLEEP_TIME, EPS, TestResult.Status.FAILED, DATA, caughtException);
    }

    private void validate(int expectedDuration, int eps, TestResult.Status expectedStatus, Map expectedData, Throwable expectedException) {
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

        if(expectedData != null) {
            Assert.assertEquals("Data does not match", expectedData, result.getData());
        } else {
            Assert.assertNull(result.getData());
        }
    }

    @After
    public void deleteLogs()  {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
