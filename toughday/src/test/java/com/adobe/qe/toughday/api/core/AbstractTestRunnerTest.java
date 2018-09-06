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
package com.adobe.qe.toughday.api.core;

import com.adobe.qe.toughday.LogFileEraser;
import com.adobe.qe.toughday.api.core.config.GlobalArgs;
import com.adobe.qe.toughday.api.core.runnermocks.MockInheritanceTest;
import com.adobe.qe.toughday.api.core.runnermocks.MockTest;
import com.adobe.qe.toughday.api.core.runnermocks.MockTestRunner;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mockito;

import java.util.List;

public class AbstractTestRunnerTest {

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("logFileName", ".");
    }

    @Test
    public void testExecutionOrder() {
        MockTest test = new MockTest();
        execute(test, null, true);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 3, executedMethods.size());
        Assert.assertEquals("Before method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Test method expected", MockTestRunner.BASE_TEST_METHOD, executedMethods.get(1));
        Assert.assertEquals("After method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(2));
    }

    @Test
    public void testExecutionOrderFailBefore() {
        MockTest test = new MockTest();
        test.doFailBefore();
        test.setGlobalArgs(Mockito.mock(GlobalArgs.class));
        execute(test, MockTestRunner.BASE_BEFORE_METHOD, true);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 2, executedMethods.size());
        Assert.assertEquals("Before method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("After method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(1));
    }

    @Test
    public void testExecutionOrderFailTest() {
        MockTest test = new MockTest();
        test.doFailTest();
        execute(test, MockTestRunner.BASE_TEST_METHOD, true);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 3, executedMethods.size());
        Assert.assertEquals("Before method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Test method expected", MockTestRunner.BASE_TEST_METHOD, executedMethods.get(1));
        Assert.assertEquals("After method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(2));
    }

    @Test
    public void testExecutionOrderFailAfter() {
        MockTest test = new MockTest();
        test.doFailAfter();
        execute(test, null, true);  //TODO make the @After methods fail tests and propagate exceptions

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 3, executedMethods.size());
        Assert.assertEquals("Before method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Test method expected", MockTestRunner.BASE_TEST_METHOD, executedMethods.get(1));
        Assert.assertEquals("After method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(2));
    }

    @Test
    public void testExecutionOrderFailBeforeNoParent() {
        MockTest test = new MockTest();
        test.doFailBefore();
        //when the test doesn't have a parent, throwables should not be propagated by the runner
        execute(test, null, false);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 2, executedMethods.size());
        Assert.assertEquals("Before method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("After method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(1));
    }

    @Test
    public void testExecutionOrderFailTestNoParent() {
        MockTest test = new MockTest();
        test.doFailTest();
        //when the test doesn't have a parent, throwables should not be propagated by the runner
        execute(test, null, false);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 3, executedMethods.size());
        Assert.assertEquals("Before method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Test method expected", MockTestRunner.BASE_TEST_METHOD, executedMethods.get(1));
        Assert.assertEquals("After method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(2));
    }

    @Test
    public void testExecutionOrderFailAfterNoParent() {
        MockTest test = new MockTest();
        test.doFailAfter();
        //when the test doesn't have a parent, throwables should not be propagated by the runner
        execute(test, null, false);  //TODO make the @After methods fail tests and propagate exceptions

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 3, executedMethods.size());
        Assert.assertEquals("Before method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Test method expected", MockTestRunner.BASE_TEST_METHOD, executedMethods.get(1));
        Assert.assertEquals("After method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(2));
    }

    @Test
    public void testInheritanceExecutionOrder() {
        MockTest test = new MockInheritanceTest();
        execute(test, null, true);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 5, executedMethods.size());
        Assert.assertEquals("Before base method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Before subclass method expected", MockTestRunner.SUBCLASS_BEFORE_METHOD, executedMethods.get(1));
        Assert.assertEquals("Test subclass method expected", MockTestRunner.SUBCLASS_TEST_METHOD, executedMethods.get(2));
        Assert.assertEquals("After subclass method expected", MockTestRunner.SUBCLASS_AFTER_METHOD, executedMethods.get(3));
        Assert.assertEquals("After base method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(4));
    }

    @Test
    public void testInheritanceExecutionOrderFailBefore() {
        MockTest test = new MockInheritanceTest();
        test.doFailBefore();
        test.setGlobalArgs(Mockito.mock(GlobalArgs.class));
        execute(test, MockTestRunner.BASE_BEFORE_METHOD, true);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 3, executedMethods.size());
        Assert.assertEquals("Before base method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("After subclass method expected", MockTestRunner.SUBCLASS_AFTER_METHOD, executedMethods.get(1));
        Assert.assertEquals("After base method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(2));
    }

    @Test
    public void testInheritanceExecutionOrderFailSubclassBefore() {
        MockInheritanceTest test = new MockInheritanceTest();
        test.doFailSubclassBefore();
        test.setGlobalArgs(Mockito.mock(GlobalArgs.class));
        execute(test, MockTestRunner.SUBCLASS_BEFORE_METHOD, true);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 4, executedMethods.size());
        Assert.assertEquals("Before base method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Before subclass method expected", MockTestRunner.SUBCLASS_BEFORE_METHOD, executedMethods.get(1));
        Assert.assertEquals("After subclass method expected", MockTestRunner.SUBCLASS_AFTER_METHOD, executedMethods.get(2));
        Assert.assertEquals("After base method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(3));
    }

    @Test
        public void testInheritanceExecutionOrderFailTest() {
        MockInheritanceTest test = new MockInheritanceTest();
        test.doFailTest();
        execute(test, null, true); //since the test method is not executed, because it is overriden, then there will be no exception

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 5, executedMethods.size());
        Assert.assertEquals("Before base method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Before subclass method expected", MockTestRunner.SUBCLASS_BEFORE_METHOD, executedMethods.get(1));
        Assert.assertEquals("Test subclass method expected", MockTestRunner.SUBCLASS_TEST_METHOD, executedMethods.get(2));
        Assert.assertEquals("After subclass method expected", MockTestRunner.SUBCLASS_AFTER_METHOD, executedMethods.get(3));
        Assert.assertEquals("After base method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(4));
    }

    @Test
    public void testInheritanceExecutionOrderFailSubclassTest() {
        MockInheritanceTest test = new MockInheritanceTest();
        test.doFailSubclassTest();
        execute(test, MockTestRunner.SUBCLASS_TEST_METHOD, true);

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 5, executedMethods.size());
        Assert.assertEquals("Before base method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Before subclass method expected", MockTestRunner.SUBCLASS_BEFORE_METHOD, executedMethods.get(1));
        Assert.assertEquals("Test subclass method expected", MockTestRunner.SUBCLASS_TEST_METHOD, executedMethods.get(2));
        Assert.assertEquals("After subclass method expected", MockTestRunner.SUBCLASS_AFTER_METHOD, executedMethods.get(3));
        Assert.assertEquals("After base method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(4));
    }

    @Test
    public void testInheritanceExecutionOrderFailAfter() {
        MockInheritanceTest test = new MockInheritanceTest();
        test.doFailAfter();
        execute(test, null, true);
        //execute(test, MockTestRunner.SUBCLASS_TEST_METHOD); TODO make the @After methods fail tests and propagate exceptions

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 5, executedMethods.size());
        Assert.assertEquals("Before base method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Before subclass method expected", MockTestRunner.SUBCLASS_BEFORE_METHOD, executedMethods.get(1));
        Assert.assertEquals("Test subclass method expected", MockTestRunner.SUBCLASS_TEST_METHOD, executedMethods.get(2));
        Assert.assertEquals("After subclass method expected", MockTestRunner.SUBCLASS_AFTER_METHOD, executedMethods.get(3));
        Assert.assertEquals("After base method expected", MockTestRunner.BASE_AFTER_METHOD, executedMethods.get(4));
    }

    @Test
    public void testInheritanceExecutionOrderFailSubclassAfter() {
        MockInheritanceTest test = new MockInheritanceTest();
        test.doFailSubclassAfter();
        execute(test, null, true);
        //execute(test, MockTestRunner.SUBCLASS_TEST_METHOD); TODO make the @After methods fail tests and propagate exceptions

        List<String> executedMethods = test.getExecutedMethods();
        Assert.assertEquals("Unexpected number of executed methods", 4, executedMethods.size());
        Assert.assertEquals("Before base method expected", MockTestRunner.BASE_BEFORE_METHOD, executedMethods.get(0));
        Assert.assertEquals("Before subclass method expected", MockTestRunner.SUBCLASS_BEFORE_METHOD, executedMethods.get(1));
        Assert.assertEquals("Test subclass method expected", MockTestRunner.SUBCLASS_TEST_METHOD, executedMethods.get(2));
        Assert.assertEquals("After subclass method expected", MockTestRunner.SUBCLASS_AFTER_METHOD, executedMethods.get(3));
    }

    private void execute(MockTest test, String expectedErrorMessage, boolean addParent) {
        boolean exceptionCaught = false;
        RunMap runMap = Mockito.mock(RunMap.class);

        if (addParent) {
            test.setParent(new MockTest());
        }

        AbstractTestRunner runner = new MockTestRunner(test.getClass());
        try {
            runner.runTest(test, runMap);
        } catch (Throwable t) {
            exceptionCaught = true;
            Assert.assertEquals("Unexpected error message", expectedErrorMessage, t.getMessage());
        }
        if (expectedErrorMessage != null) Assert.assertTrue("Expected an exception with message \"" + expectedErrorMessage + "\"", exceptionCaught);
        Mockito.verify(runMap, Mockito.times(1)).record(Mockito.any());
    }

    @After
    public void deleteFile()  {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}