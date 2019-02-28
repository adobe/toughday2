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
import com.adobe.qe.toughday.internal.core.engine.runmodes.Normal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class TestNormalMode {
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
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), Normal.class);
        Assert.assertEquals(((Normal)configuration.getRunMode()).getConcurrency(), 200);
    }

    @Test
    public void testNormalSimplePass() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=normal", "concurrency=100"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), Normal.class);
        Assert.assertEquals(((Normal)configuration.getRunMode()).getConcurrency(), 100);
        Assert.assertEquals(configuration.getGlobalArgs().getDuration(), 20);
    }

    @Test
    public void testNormalSimpleFail() {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=normal", "load=10"));
        try {
            new Configuration(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Load should not be configurable for Normal.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNormalStartEnd() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=normal", "start=10", "end=50"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), Normal.class);
        Assert.assertEquals(((Normal)configuration.getRunMode()).getStart(), 10);
        Assert.assertEquals(((Normal)configuration.getRunMode()).getEnd(), 50);
        Assert.assertEquals(configuration.getGlobalArgs().getDuration(), 20);
    }

    @Test
    public void testNormalStartConcurrency() {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=normal", "start=10", "concurrency=100"));
        try{
            Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
            configuration.getRunMode().runTests(new Engine(configuration));
            Assert.fail("Should not be able to have both start/end and concurrency.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNormalEndConcurrency() {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=normal", "end=50", "concurrency=100"));
        try{
            Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
            configuration.getRunMode().runTests(new Engine(configuration));
            Assert.fail("Should not be able to have both start/end and concurrency.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNormalStartEndConcurrency() {
        cmdLineArgs.addAll(Arrays.asList("--duration=20s", "--runmode", "type=normal", "start=10", "end=50", "concurrency=100"));
        try{
            Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
            configuration.getRunMode().runTests(new Engine(configuration));
            Assert.fail("Should not be able to have both start/end and concurrency.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testNormalStartEndRate() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=normal", "start=10", "end=100", "rate=5"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), Normal.class);
        Assert.assertEquals(((Normal)configuration.getRunMode()).getStart(), 10);
        Assert.assertEquals(((Normal)configuration.getRunMode()).getEnd(), 100);
        Assert.assertEquals(((Normal)configuration.getRunMode()).getRate(), 5);
    }

    @Test
    public void testNormalStartEndRateInterval() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=normal", "start=10", "end=100", "rate=5", "interval=1m"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configuration.getRunMode().getClass(), Normal.class);

        Normal runMode = (Normal) configuration.getRunMode();

        Assert.assertEquals(runMode.getStart(), 10);
        Assert.assertEquals(runMode.getEnd(), 100);
        Assert.assertEquals(runMode.getRate(), 5);
        Assert.assertEquals(runMode.getInterval(), "60s");
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
