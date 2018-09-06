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
import com.adobe.qe.toughday.api.core.runnermocks.MockTest;
import com.adobe.qe.toughday.internal.core.UUIDTestId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mockito;

import java.io.File;

public class AbstractTestTest {

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("logFileName", ".");
    }

    @Test
    public void testsWithSameIDAreEqual() {
        TestId testId = new UUIDTestId();
        AbstractTest test1 = new MockTest();
        AbstractTest test2 = new MockTest();

        test1.setID(testId);
        test2.setID(testId);

        Assert.assertTrue(test1.equals(test2));
        Assert.assertTrue(test2.equals(test1));
        Assert.assertTrue(test1.equals(test1));
    }

    @Test
    public void testCloneOfATestIsEqual() {
        AbstractTest test = new MockTest();
        test.setGlobalArgs(Mockito.mock(GlobalArgs.class));
        AbstractTest cloneTest = test.clone();

        Assert.assertTrue(test.equals(cloneTest));
        Assert.assertTrue(cloneTest.equals(test));
    }

    @Test
    public void testWithSameClassButDifferentIdAreDifferent() {
        TestId testId1 = new UUIDTestId();
        AbstractTest test1 = new MockTest();
        test1.setID(testId1);

        TestId testId2 = new UUIDTestId();
        AbstractTest test2 = new MockTest();
        test2.setID(testId2);

        Assert.assertFalse(test1.equals(test2));
        Assert.assertFalse(test2.equals(test1));
    }

    @Test
    public void testIsAncestorOfItself() {
        AbstractTest test = new MockTest();
        Assert.assertTrue(test.isAncestorOf(test));
    }

    @Test
    public void parentIsAncestorOfTest() {
        AbstractTest test = new MockTest();
        AbstractTest parent = new MockTest();
        test.setParent(parent);

        Assert.assertTrue(parent.isAncestorOf(test));
        Assert.assertFalse(test.isAncestorOf(parent));
    }

    @Test
    public void testAncestor() {
        AbstractTest test = new MockTest();

        AbstractTest test1 = new MockTest();
        AbstractTest test2 = new MockTest();
        test1.setParent(test);
        test2.setParent(test);

        AbstractTest test11 = new MockTest();
        AbstractTest test12 = new MockTest();
        test11.setParent(test1);
        test12.setParent(test1);

        AbstractTest test21 = new MockTest();
        AbstractTest test22 = new MockTest();
        test21.setParent(test2);
        test22.setParent(test2);

        Assert.assertTrue(test.isAncestorOf(test1));
        Assert.assertTrue(test.isAncestorOf(test2));
        Assert.assertTrue(test.isAncestorOf(test11));
        Assert.assertTrue(test.isAncestorOf(test12));
        Assert.assertTrue(test.isAncestorOf(test21));
        Assert.assertTrue(test.isAncestorOf(test22));

        Assert.assertTrue(test1.isAncestorOf(test11));
        Assert.assertTrue(test1.isAncestorOf(test12));
        Assert.assertFalse(test1.isAncestorOf(test21));
        Assert.assertFalse(test1.isAncestorOf(test22));

        Assert.assertTrue(test2.isAncestorOf(test21));
        Assert.assertTrue(test2.isAncestorOf(test22));
        Assert.assertFalse(test2.isAncestorOf(test11));
        Assert.assertFalse(test2.isAncestorOf(test12));
    }

    @Test
    public void testClone() {
        File workspace = new File("Dummy");
        TestId testId = new UUIDTestId();
        AbstractTest parent = new MockTest();
        GlobalArgs mockGlobalArgs = Mockito.mock(GlobalArgs.class);

        AbstractTest test = new MockTest();
        test.setName("TestClone");
        test.setParent(parent);
        test.setID(testId);
        test.setGlobalArgs(mockGlobalArgs);
        test.setShowSteps(Boolean.TRUE.toString());
        test.setWorkspace(workspace);

        AbstractTest cloneTest = test.clone();
        Assert.assertEquals(test.getName(), cloneTest.getName());
        Assert.assertEquals(test.getFullName(), cloneTest.getFullName());
        Assert.assertEquals(test.getParent(), cloneTest.getParent());
        Assert.assertEquals(test.getId(), cloneTest.getId());
        Assert.assertEquals(test.getGlobalArgs(), cloneTest.getGlobalArgs());
        Assert.assertEquals(test.getShowSteps(), cloneTest.getShowSteps());
        Assert.assertNotNull(cloneTest.benchmark()); //TODO verify equality
        Assert.assertEquals(test.logger(), cloneTest.logger());
        Assert.assertEquals(test.getWorkspace(), cloneTest.getWorkspace());

    }

    @Test
    public void testParent() {
        AbstractTest test = new MockTest();
        AbstractTest parent = new MockTest();

        test.setParent(parent);

        Assert.assertEquals(parent, test.getParent());
    }

    @Test
    public void testName() {
        AbstractTest test = new MockTest();
        Assert.assertEquals(MockTest.class.getSimpleName(), test.getName());

        test.setName("TestName");

        Assert.assertEquals("TestName", test.getName());
    }

    @Test
    public void testFullName() {
        AbstractTest test = new MockTest();
        AbstractTest parent = new MockTest();
        parent.setName("parent");

        AbstractTest ancestor = new MockTest();
        ancestor.setName("ancestor");
        parent.setParent(ancestor);

        Assert.assertEquals(MockTest.class.getSimpleName(), test.getFullName());

        //When a test has no parent, full name and name are equal
        Assert.assertEquals(test.getName(), test.getFullName());

        test.setName("test");
        Assert.assertEquals("test", test.getFullName());

        //When a test has no parent, full name and name are equal
        Assert.assertEquals(test.getName(), test.getFullName());

        test.setParent(parent);

        Assert.assertEquals("ancestor.parent.test", test.getFullName());
        Assert.assertFalse(test.getName().equals(test.getFullName()));
    }

    @Test
    public void globalArgsTest() {
        MockTest ancestor = new MockTest();
        MockTest parent = new MockTest();
        MockTest child1 = new MockTest();
        MockTest child2 = new MockTest();

        ancestor.addChild(parent);
        parent.addChild(child1);
        parent.addChild(child2);

        GlobalArgs args = Mockito.mock(GlobalArgs.class);
        ancestor.setGlobalArgs(args);

        Assert.assertEquals(args, ancestor.getGlobalArgs());
        Assert.assertEquals(args, parent.getGlobalArgs());
        Assert.assertEquals(args, child1.getGlobalArgs());
        Assert.assertEquals(args, child2.getGlobalArgs());
    }


    @Test
    public void testChooseGlobalTimeout() {
        MockTest test = new MockTest();
        GlobalArgs globalArgs = Mockito.mock(GlobalArgs.class);
        Mockito.when(globalArgs.getTimeout()).thenReturn(180L);

        Long testTimeout = test.getTimeout();
        Assert.assertEquals(testTimeout >= 0 ? testTimeout : globalArgs.getTimeout(), 180L);
    }

    @Test
    public void testChooseGlobalTimeoutAgain() {
        MockTest test = new MockTest();
        GlobalArgs globalArgs = Mockito.mock(GlobalArgs.class);
        Mockito.when(globalArgs.getTimeout()).thenReturn(180L);

        test.setTimeout("-5");
        Long testTimeout = test.getTimeout();
        Assert.assertEquals(testTimeout >= 0 ? testTimeout : globalArgs.getTimeout(), 180L);
    }

    @Test
    public void testChooseSetTimeout() {
        MockTest test = new MockTest();
        GlobalArgs globalArgs = Mockito.mock(GlobalArgs.class);
        Mockito.when(globalArgs.getTimeout()).thenReturn(180L);
        
        test.setTimeout("1");
        Long testTimeout = test.getTimeout();
        Assert.assertEquals(testTimeout >= 0 ? testTimeout : globalArgs.getTimeout(), 1000);
    }

    @After
    public  void deleteFiles() {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
