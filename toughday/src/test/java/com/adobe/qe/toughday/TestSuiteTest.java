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
package com.adobe.qe.toughday;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.internal.core.TestSuite;

import java.util.ArrayList;

public class TestSuiteTest {
    private TestSuite suite;
    private int totalWeight;
    private long totalTimeout;
    private long totalCount;

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("logFileName", ".");
    }

    @Before
    public void before() {
        suite = new TestSuite();
        totalCount = totalTimeout = totalWeight = 0;

        suite.add((new MockTest()).setWeight(Integer.toString(10)).setCount(Long.toString(5)).setTimeout(Long.toString(20)).setName("a"));
        suite.add((new MockTest()).setWeight(Integer.toString(5)).setCount(Long.toString(50)).setTimeout(Long.toString(80)).setName("b"));
        suite.add((new MockTest()).setWeight(Integer.toString(1)).setCount(Long.toString(10)).setTimeout(Long.toString(50)).setName("c"));
        suite.add((new MockTest()).setWeight(Integer.toString(15)).setCount(Long.toString(20)).setTimeout(Long.toString(30)).setName("d"));
        suite.add((new MockTest()).setWeight(Integer.toString(50)).setCount(Long.toString(20)).setTimeout(Long.toString(10)).setName("e"));

        totalWeight = 81;
        totalTimeout = 190 * 1000;
        totalCount = 105;
    }

    @Test
    public void testTotalWeight() {
        Assert.assertEquals(suite.getTotalWeight(), totalWeight);
    }

    @Test
    public void testCount() {
        long total = 0;
        for (AbstractTest abstractTest : suite.getTests()) {
            total += abstractTest.getCount();
        }

        Assert.assertEquals(totalCount, total);
    }
    @Test
    public void testTimeout() {
        long total = 0;
        for (AbstractTest abstractTest : suite.getTests()) {
            total += abstractTest.getTimeout();
        }

        Assert.assertEquals(totalTimeout, total);
    }

    @Test
    public void testRemove() {
        AbstractTest abstractTest = suite.getTest("e");
        Assert.assertTrue(suite.contains(abstractTest.getName()));
        Assert.assertEquals(suite.remove(abstractTest.getName()), 4);
        update(abstractTest, '-');

        abstractTest = suite.getTest("a");
        Assert.assertTrue(suite.contains(abstractTest.getName()));
        Assert.assertEquals(suite.remove(abstractTest.getName()), 0);
        update(abstractTest, '-');

        abstractTest = suite.getTest("c");
        Assert.assertTrue(suite.contains(abstractTest.getName()));
        Assert.assertEquals(suite.remove(abstractTest.getName()), 1);
        update(abstractTest, '-');
    }

    @Test
    public void testAdd() {
        AbstractTest abstractTest = (new MockTest()).setWeight(Integer.toString(30)).setCount(Long.toString(65)).setTimeout(Long.toString(35)).setName("f");
        Assert.assertTrue(!suite.contains(abstractTest.getName()));

        suite.add(abstractTest);
        update(abstractTest, '+');

        abstractTest = (new MockTest()).setWeight(Integer.toString(1)).setCount(Long.toString(10)).setTimeout(Long.toString(1)).setName("g");
        suite.add(abstractTest);
        update(abstractTest, '+');

        abstractTest = (new MockTest()).setWeight(Integer.toString(12)).setCount(Long.toString(101)).setTimeout(Long.toString(13)).setName("h");
        suite.add(abstractTest, 3);
        update(abstractTest, '+');
        Assert.assertEquals(abstractTest, ((ArrayList)suite.getTests()).get(3));

        abstractTest = (new MockTest()).setWeight(Integer.toString(2)).setCount(Long.toString(11)).setTimeout(Long.toString(3)).setName("i");
        suite.add(abstractTest, 0);
        update(abstractTest, '+');
        Assert.assertEquals(abstractTest, ((ArrayList)suite.getTests()).get(0));
    }

    @Test
    public void testAddAll() {
        TestSuite secondSuite = new TestSuite();
        secondSuite.add((new MockTest()).setWeight(Integer.toString(15)).setCount(Long.toString(50)).setTimeout(Long.toString(200)).setName("x"));
        secondSuite.add((new MockTest()).setWeight(Integer.toString(50)).setCount(Long.toString(30)).setTimeout(Long.toString(10)).setName("y"));
        secondSuite.add((new MockTest()).setWeight(Integer.toString(10)).setCount(Long.toString(20)).setTimeout(Long.toString(40)).setName("z"));
        secondSuite.add((new MockTest()).setWeight(Integer.toString(10)).setCount(Long.toString(25)).setTimeout(Long.toString(70)).setName("w"));
        secondSuite.add((new MockTest()).setWeight(Integer.toString(10)).setCount(Long.toString(25)).setTimeout(Long.toString(10)).setName("v"));

        suite.addAll(secondSuite);

        for (AbstractTest test : secondSuite.getTests()) {
            totalWeight += test.getWeight();
            totalCount += test.getCount();
            totalTimeout += test.getTimeout();
        }

        testTotalWeight();
        testTimeout();
        testCount();
    }

    private void update(AbstractTest test, char op) {
        if (op == '-') {
            totalCount -= test.getCount();
            totalTimeout -= test.getTimeout();
            totalWeight -= test.getWeight();
            Assert.assertTrue(!suite.contains(test.getName()));
        } else {
            totalCount += test.getCount();
            totalTimeout += test.getTimeout();
            totalWeight += test.getWeight();
            Assert.assertTrue(suite.contains(test.getName()));

        }

        testTotalWeight();
        testTimeout();
        testCount();
    }

    @After
    public void deleteLogs()  {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
