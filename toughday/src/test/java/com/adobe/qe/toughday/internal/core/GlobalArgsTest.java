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
import com.adobe.qe.toughday.internal.core.config.GlobalArgs;
import com.adobe.qe.toughday.metrics.Average;
import com.adobe.qe.toughday.metrics.Median;
import com.adobe.qe.toughday.metrics.Percentile;
import com.adobe.qe.toughday.publishers.ConsolePublisher;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;

public class GlobalArgsTest {

    private GlobalArgs globalArgs = new GlobalArgs();

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("logFileName", ".");
    }

    @Test
    public void testLogPath() {
        globalArgs.setLogPath(".");
        Assert.assertEquals(globalArgs.getLogPath(), ".");

        globalArgs.setLogPath("~");
        Assert.assertEquals(globalArgs.getLogPath(), System.getProperty("user.home"));

        globalArgs.setLogPath("~/logs");
        Assert.assertNotEquals(globalArgs.getLogPath(), System.getProperty("user.home") + "/logs/");
        Assert.assertEquals(globalArgs.getLogPath(), System.getProperty("user.home") + "/logs");

        globalArgs.setLogPath("/");
        Assert.assertEquals(globalArgs.getLogPath(), "/");

        globalArgs.setLogPath("logs/");
        Assert.assertNotEquals(globalArgs.getLogPath(), "logs/");
        Assert.assertEquals(globalArgs.getLogPath(), "logs");

        globalArgs.setLogPath(".");
    }

    @Test
    public void testDuration() {
        globalArgs.setDuration(GlobalArgs.DEFAULT_DURATION);
        Assert.assertEquals(globalArgs.getDuration(), 24 * 60 * 60);

        globalArgs.setDuration("10s");
        Assert.assertEquals(globalArgs.getDuration(), 10);

        globalArgs.setDuration("5m");
        Assert.assertEquals(globalArgs.getDuration(), 60 * 5);

        globalArgs.setDuration("1m20s");
        Assert.assertEquals(globalArgs.getDuration(), 60 + 20);

        globalArgs.setDuration("1h1s");
        Assert.assertEquals(globalArgs.getDuration(), 3601);

        globalArgs.setDuration("1h59m");
        Assert.assertEquals(globalArgs.getDuration(), 3600 + 59 * 60);

        globalArgs.setDuration("1d12h30m30s");
        Assert.assertEquals(globalArgs.getDuration(), 30 + 30 * 60 + 12 * 60 * 60 + 24 * 60 * 60);
    }

    @Test
    public void testMetrics() {
        Assert.assertFalse(globalArgs.getMetrics().isEmpty());
        Assert.assertEquals(globalArgs.getMetrics().size(), 2);

        globalArgs.addMetric(new Median().setName("med"));
        Assert.assertEquals(globalArgs.getMetrics().size(), 3);

        globalArgs.addMetric(new Average());
        globalArgs.addMetric(new Percentile().setName("per"));
        Assert.assertEquals(globalArgs.getMetrics().size(), 5);

        Assert.assertNotNull(globalArgs.getMetric("per"));
        Assert.assertTrue(globalArgs.containsMetric("med"));

        globalArgs.updateMetricName("med", "median");
        globalArgs.updateMetricName("per", "percentile");

        Assert.assertNotNull(globalArgs.getMetric("median"));

        try {
            globalArgs.removeMetric("med");
            Assert.fail("Metric with name \"med\" should not have been present.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testPublisher() {
        Assert.assertTrue(globalArgs.getPublishers().isEmpty());

        ConsolePublisher consolePublisher = new ConsolePublisher();
        consolePublisher.setName("cpub");
        globalArgs.addPublisher(consolePublisher);

        Assert.assertTrue(globalArgs.containsPublisher("cpub"));
        Assert.assertEquals(globalArgs.getPublishers().size(), 1);

        globalArgs.updatePublisherName("cpub", "newcpub");
        Assert.assertTrue(globalArgs.containsPublisher("newcpub"));

        globalArgs.removePublisher("newcpub");

        try {
            globalArgs.removePublisher("cpub");
            Assert.fail("Publisher with name \"cpub\" should not have been present.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @After
    public void deleteLogs() {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
