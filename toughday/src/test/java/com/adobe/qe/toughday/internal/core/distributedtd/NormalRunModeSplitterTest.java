package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.MockTest;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.engine.runmodes.Normal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

public class NormalRunModeSplitterTest {
    private ArrayList<String> cmdLineArgs = new ArrayList<>();

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
    public void testConcurrencyDistribution() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=normal", "concurrency=320"));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2", "Agent3");
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        RunMode runMode = configuration.getRunMode();

        Map<String, RunMode> runModes = runMode.getRunModeSplitter().distributeRunMode(runMode, mockAgents);

        Assert.assertEquals(108, ((Normal) runModes.get("Agent1")).getConcurrency());
        Assert.assertEquals(106, ((Normal) runModes.get("Agent2")).getConcurrency());
        Assert.assertEquals(106, ((Normal) runModes.get("Agent3")).getConcurrency());
    }

    @Test
    public void testVariableConcurrencyDistribution() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=normal", "start=3", "end=12", "rate=3"));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2");

        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        RunMode runMode = configuration.getRunMode();

        Map<String, RunMode> runModes = runMode.getRunModeSplitter().distributeRunMode(runMode, mockAgents);
        Normal initialRunMode = (Normal) configuration.getRunMode();

        Normal firstAgentRunMode = (Normal) runModes.get("Agent1");
        Assert.assertEquals(2, firstAgentRunMode.getStart());
        Assert.assertEquals(6, firstAgentRunMode.getEnd());
        Assert.assertEquals(2, firstAgentRunMode.getRate());
        // check that the interval property does not change
        Assert.assertEquals(initialRunMode.getInterval(), firstAgentRunMode.getInterval());

        Normal secondAgentRunMode = (Normal) runModes.get("Agent2");
        Assert.assertEquals(1, secondAgentRunMode.getStart());
        Assert.assertEquals(6, secondAgentRunMode.getEnd());
        Assert.assertEquals(1, secondAgentRunMode.getRate());
        // check that the interval property does not change
        Assert.assertEquals(initialRunMode.getInterval(), secondAgentRunMode.getInterval());
    }


    @Test
    public void testVariableConcurrencyWithRateLowerThanNumberOfAgents() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=normal", "start=3", "end=9", "interval=1s", "rate=1"));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2");

        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        RunMode initialRunMode = configuration.getRunMode();
        Map<String, RunMode> runModes = initialRunMode.getRunModeSplitter().distributeRunMode(initialRunMode, mockAgents);

        Normal firstAgentRunMode = (Normal) runModes.get("Agent1");
        Assert.assertEquals(3, firstAgentRunMode.getStart());
        Assert.assertEquals(6, firstAgentRunMode.getEnd());
        Assert.assertEquals("2s", firstAgentRunMode.getInterval());
        Assert.assertEquals(0, firstAgentRunMode.getInitialDelay());

        Normal secondAgentRunMode = (Normal) runModes.get("Agent2");
        Assert.assertEquals(0, secondAgentRunMode.getStart());
        Assert.assertEquals(3, secondAgentRunMode.getEnd());
        Assert.assertEquals("2s", secondAgentRunMode.getInterval());
        Assert.assertEquals(1000, secondAgentRunMode.getInitialDelay());

        // check that the initial rate is not modified
        Assert.assertEquals(((Normal)initialRunMode).getRate(), firstAgentRunMode.getRate());
        Assert.assertEquals(((Normal)initialRunMode).getRate(), secondAgentRunMode.getRate());
    }

    @Test
    public void testWaitTimeIsNotModified() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=normal", "waittime=1000"));

        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        RunMode initialRunMode = configuration.getPhases().get(0).getRunMode();
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2", "Agent3");

        Map<String, RunMode> runModes = initialRunMode.getRunModeSplitter().distributeRunMode(initialRunMode, mockAgents);
        runModes.forEach((key, value) ->
                Assert.assertEquals(((Normal)initialRunMode).getWaitTime(),
                        ((Normal) value).getWaitTime()));
    }

}
