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
import com.adobe.qe.toughday.internal.core.benchmarkmocks.*;
import com.adobe.qe.toughday.internal.core.benckmark.BenchmarkImpl;
import com.adobe.qe.toughday.mocks.MockRunMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class BenchmarkCustomProxyTest {

    private static final String STEP_NAME = "STEP_NAME";
    private static final int SLEEP_TIME = 110;
    private static final int EPS = 10;
    private static final Throwable FAIL_EXCEPTION = new Exception();
    private static final Throwable SKIP_EXCEPTION = new SkippedTestException(new Exception());
    private static final Map DATA = Collections.singletonMap("KEY", "VALUE");

    private Benchmark benchmark;

    private MockRunMap runMap;
    private AbstractTest test;
    private GlobalArgs globalArgs;
    private Throwable caughtException;
    private MockWorker worker;
    private MockProxy customProxy;

    @Before
    public void before() {
        benchmark = new BenchmarkImpl();
        runMap = new MockRunMap();
        benchmark.setRunMap(runMap);
        test = new MockTest();
        globalArgs = Mockito.mock(GlobalArgs.class);
        test.setGlobalArgs(globalArgs);
        worker = new MockWorker();
        customProxy = new MockProxy();
    }

    @Test
    public void customProxyAsParamClassTest() throws Throwable {
        Assert.assertEquals(MockProxy.class, benchmark.measure(test, STEP_NAME, worker, customProxy).getClass());
    }

    @Test
    public void customProxyAsParam1Pass() throws Throwable {
        Object returnValue = benchmark.measure(test, STEP_NAME, worker, customProxy).methodWithReturnValue();
        Assert.assertEquals(MockWorker.RETURN_VALUE, returnValue);
        validate(MockWorker.METHOD6, Arrays.asList(), SLEEP_TIME, EPS, TestResult.Status.PASSED, null);
    }

    @Test
    public void customProxyAsParam1Fail() throws Throwable {
        worker.fail(FAIL_EXCEPTION);
        try {
            benchmark.measure(test, STEP_NAME, worker, customProxy).methodWithReturnValue();
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD6, Arrays.asList(), SLEEP_TIME, EPS, TestResult.Status.FAILED, FAIL_EXCEPTION);
    }

    @Test
    public void customProxyAsParam1Skip() throws Throwable {
        worker.skip(SKIP_EXCEPTION);
        try {
            benchmark.measure(test, STEP_NAME, worker, customProxy).methodWithReturnValue();
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD6, Arrays.asList(), SLEEP_TIME, EPS, TestResult.Status.SKIPPED, SKIP_EXCEPTION);
    }

    @Test
    public void customProxyAsParam1Interrupt() throws Throwable {
        worker.interrupt();
        try {
            benchmark.measure(test, STEP_NAME, worker, customProxy).methodWithReturnValue();
        } catch (Exception e) {
            caughtException = e;
        }
        validate(MockWorker.METHOD6, Arrays.asList(), SLEEP_TIME, EPS, TestResult.Status.FAILED, caughtException);
    }

    @Test
    public void customProxyConfiguredClassTest() throws Throwable {
        benchmark.registerClassProxy(MockWorker.class, MockProxy.class);
        Assert.assertEquals(MockProxy.class, benchmark.measure(test, STEP_NAME, worker).getClass());
    }

    @Test
    public void customProxyConfigured() throws Throwable {
        benchmark.registerClassProxy(MockWorker.class, MockProxy.class);
        benchmark.measure(test, STEP_NAME, worker).methodWithReturnValue(1L, 2L, 3L);
        validate(MockWorker.METHOD6, Arrays.asList(1L, 2L, 3L), SLEEP_TIME, EPS, TestResult.Status.PASSED, null);
    }

    @Test
    public void customProxyFactoryClassTest() throws Throwable {
        benchmark.registerClassProxyFactory(MockWorker.class, new MockProxyFactory());
        Assert.assertEquals(MockProxyFromFactory.class, benchmark.measure(test, STEP_NAME, worker).getClass());
    }

    @Test
    public void customProxyFactoryConfigured() throws Throwable {
        benchmark.registerClassProxyFactory(MockWorker.class, new MockProxyFactory());
        benchmark.measure(test, STEP_NAME, worker).methodWithReturnValue(1L, 2L, 3L);
        validate(MockWorker.METHOD6, Arrays.asList(1L, 2L, 3L), SLEEP_TIME, EPS, TestResult.Status.PASSED, null);
    }

    @Test
    public void customProxyHierarchyFactoryClassTest1() throws Throwable {
        benchmark.registerHierarchyProxyFactory(MockWorker.class, new MockProxyFactory());
        Assert.assertEquals(MockProxyFromFactory.class, benchmark.measure(test, STEP_NAME, worker).getClass());
    }

    @Test
    public void customProxyHierarchyFactoryClassTest2() throws Throwable {
        benchmark.registerHierarchyProxyFactory(MockWorker.class, new MockDynamicProxyFactory());
        Assert.assertNotEquals(MockProxyFromFactory.class, benchmark.measure(test, STEP_NAME, new MockWorkerSubclass()).getClass());
        Assert.assertNotEquals(MockProxy.class, benchmark.measure(test, STEP_NAME, new MockWorkerSubclass()).getClass());
        Assert.assertNotEquals(MockWorkerSubclass.class, benchmark.measure(test, STEP_NAME, new MockWorkerSubclass()).getClass());
    }

    @Test
    public void customProxyHierarchyFactory() throws Throwable {
        worker = new MockWorkerSubclass();
        benchmark.registerHierarchyProxyFactory(MockWorker.class, new MockDynamicProxyFactory());
        benchmark.measure(test, STEP_NAME, worker).methodWithReturnValue(1L, 2L, 3L);
        validate(MockWorker.METHOD6, Arrays.asList(1L, 2L, 3L), SLEEP_TIME, EPS, TestResult.Status.PASSED, null);
    }

    @Test
    public void proxyResolutionTest1() throws Throwable {
        benchmark.registerClassProxyFactory(MockWorker.class, new MockProxyFactory());
        benchmark.registerClassProxy(MockWorker.class, MockProxy.class);
        benchmark.registerHierarchyProxyFactory(MockWorker.class, new MockProxyFactory());

        Assert.assertEquals(MockProxy.class, benchmark.measure(test, STEP_NAME, worker).getClass());
    }

    @Test
    public void proxyResolutionTest2() throws Throwable {
        MockProxyFactory proxyFactory = new MockProxyFactory();
        MockProxyFactory proxyHierarchyFactory = new MockProxyFactory();
        benchmark.registerHierarchyProxyFactory(MockWorker.class, proxyHierarchyFactory);
        benchmark.registerClassProxyFactory(MockWorker.class, proxyFactory);

        Assert.assertEquals(MockProxyFromFactory.class, benchmark.measure(test, STEP_NAME, worker).getClass());
        Assert.assertTrue(proxyFactory.isInvoked());
        Assert.assertFalse(proxyHierarchyFactory.isInvoked());
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

        Assert.assertTrue("Duration is not in range: " + result.getDuration(), result.getDuration() >= expectedDuration - eps && result.getDuration() < expectedDuration + eps);
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
