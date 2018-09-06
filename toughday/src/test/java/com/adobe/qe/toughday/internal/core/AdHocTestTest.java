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
import com.adobe.qe.toughday.api.core.TestId;
import com.adobe.qe.toughday.api.core.config.GlobalArgs;
import com.adobe.qe.toughday.api.core.runnermocks.MockTest;
import com.adobe.qe.toughday.internal.core.benckmark.AdHocTest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;
import org.mockito.Mockito;

public class AdHocTestTest {

    private MockTest test;

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("logFileName", ".");
    }

    @Before
    public void before() {
        this.test = new MockTest();
    }

    @Test
    public void testEquality() {
        AdHocTest adHocTest1 = new AdHocTest(test, "Test");
        Assert.assertTrue(adHocTest1.equals(adHocTest1));
        adHocTest1.setGlobalArgs(Mockito.mock(GlobalArgs.class));

        AdHocTest adHocTest2 = new AdHocTest(test, "Test");
        Assert.assertTrue(adHocTest1.equals(adHocTest2));
        Assert.assertTrue(adHocTest2.equals(adHocTest2));

        AdHocTest cloneAdHocTest = (AdHocTest) adHocTest1.clone();
        Assert.assertTrue(cloneAdHocTest.equals(adHocTest1));
        Assert.assertTrue(adHocTest1.equals(cloneAdHocTest));

        AdHocTest adHocTest3 = new AdHocTest(test, "DifferentTest");
        Assert.assertFalse(adHocTest1.equals(adHocTest3));
        Assert.assertFalse(adHocTest3.equals(adHocTest1));
    }

    @Test
    public void testHashCode() {
        AdHocTest adHocTest1 = new AdHocTest(test, "Test");
        Assert.assertEquals("Test".hashCode() * test.hashCode(), adHocTest1.hashCode());

        TestId testId = new UUIDTestId();
        AdHocTest adHocTest2 = new AdHocTest(testId, test, "Test");
        Assert.assertEquals(adHocTest2.hashCode(), testId.hashCode());
    }

    @After
    public void deleteLogs() {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
