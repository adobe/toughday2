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

import com.adobe.qe.toughday.internal.core.Timestamp;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.runmodes.ConstantLoad;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestConstantLoadMode {
    private ArrayList<String> cmdLineArgs;

    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
    }

    @Before
    public void before() {
        cmdLineArgs = new ArrayList<>(Collections.singletonList("--host=localhost"));
    }

    @Test
    public void testDefault() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=constantload"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), ConstantLoad.class);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getLoad(), 50);
    }

    @Test
    public void testCtLoadSimplePass() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=constantload", "load=100"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), ConstantLoad.class);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getLoad(), 100);
        Assert.assertEquals(configuration.getGlobalArgs().getDuration(), 20);
    }

    @Test
    public void testCtLoadSimpleFail() {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=constantload", "concurrency=100"));
        try {
            new Configuration(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Concurrency should not be configurable for Constant Load.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testCtLoadStartEnd() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=constantload", "start=10", "end=50"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), ConstantLoad.class);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getStart(), 10);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getEnd(), 50);
        Assert.assertEquals(configuration.getGlobalArgs().getDuration(), 20);
    }

    @Test
    public void testCtLoadStartConcurrency() {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=constantload", "start=10", "load=100"));
        try{
            Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
            configuration.getRunMode().runTests(new Engine(configuration));
            Assert.fail("Should not be able to have both start/end and load.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testCtLoadEndConcurrency() {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=constantload", "end=50", "load=40"));
        try{
            Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
            configuration.getRunMode().runTests(new Engine(configuration));
            Assert.fail("Should not be able to have both start/end and load.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testCtLoadStartEndConcurrency() {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=constantload", "start=10", "end=50", "load=40"));
        try{
            Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
            configuration.getRunMode().runTests(new Engine(configuration));
            Assert.fail("Should not be able to have both start/end and load.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testCtLoadStartEndRate() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=constantload", "start=10", "end=100", "rate=5"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), ConstantLoad.class);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getStart(), 10);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getEnd(), 100);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getRate(), 5);
    }

    @Test
    public void testCtLoadStartEndRateInterval() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=constantload", "start=10", "end=100", "rate=5", "interval=1m"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), ConstantLoad.class);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getStart(), 10);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getEnd(), 100);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getRate(), 5);
        Assert.assertEquals(((ConstantLoad)configuration.getRunMode()).getInterval(), "60s");
    }

    @After
    public void after() {
        new File("toughday_" + Timestamp.START_TIME + ".yaml").delete();
    }

    @AfterClass
    public static void deleteLogs() {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
