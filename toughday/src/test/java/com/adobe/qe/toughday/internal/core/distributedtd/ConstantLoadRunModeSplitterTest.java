package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.engine.runmodes.ConstantLoad;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

public class ConstantLoadRunModeSplitterTest {
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
    public void testLoadDistribution() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=constantload", "load=182"));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2", "Agent3");
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        RunMode runMode = configuration.getRunMode();

        Map<String, RunMode> runModes = runMode.getRunModeSplitter().distributeRunMode(runMode, mockAgents);
        Assert.assertEquals(62, ((ConstantLoad) runModes.get("Agent1")).getLoad());
        Assert.assertEquals(60, ((ConstantLoad) runModes.get("Agent2")).getLoad());
        Assert.assertEquals(60, ((ConstantLoad) runModes.get("Agent3")).getLoad());
    }

    @Test
    public void testVariableLoadDistribution() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=constantload", "start=3", "end=12", "rate=3"));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2");

        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        RunMode runMode = configuration.getRunMode();
        Map<String, RunMode> runModes = runMode.getRunModeSplitter().distributeRunMode(runMode, mockAgents);

        ConstantLoad firstAgentRunMode = (ConstantLoad) runModes.get("Agent1");
        Assert.assertEquals(2, firstAgentRunMode.getStart());
        Assert.assertEquals(6, firstAgentRunMode.getEnd());
        Assert.assertEquals(2, firstAgentRunMode.getRate());

        ConstantLoad secondAgentRunMode = (ConstantLoad) runModes.get("Agent2");
        Assert.assertEquals(1, secondAgentRunMode.getStart());
        Assert.assertEquals(6, secondAgentRunMode.getEnd());
        Assert.assertEquals(1, secondAgentRunMode.getRate());
    }

    @Test
    public void testVariableLoadWithRateLowerThanTheNumberOfAgents() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=constantload", "start=10", "end=20", "rate=2", "interval=2s"));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2", "Agent3");

        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        RunMode runMode = configuration.getRunMode();

        Map<String, RunMode> runModes = runMode.getRunModeSplitter().distributeRunMode(runMode, mockAgents);
        ConstantLoad initialRunMode = (ConstantLoad) configuration.getPhases().get(0).getRunMode();

        runModes.forEach((key, value) -> {
            ConstantLoad taskRunMode = (ConstantLoad) value;
            Assert.assertEquals(initialRunMode.getEnd(), taskRunMode.getEnd());
            Assert.assertEquals(initialRunMode.getRate(), taskRunMode.getOneAgentRate());
            Assert.assertEquals(6, taskRunMode.getRate());
            Assert.assertEquals("6s", taskRunMode.getInterval());

        });

        /* the values for rate property are checked for the first complete cycle
        (nr_agents * interval period of time) */
        ConstantLoad firstAgentRunMode = (ConstantLoad) runModes.get("Agent1");
        Assert.assertEquals(10, firstAgentRunMode.getStart());
        Assert.assertEquals(0, firstAgentRunMode.getInitialDelay());

        ConstantLoad secondAgentRunMode = (ConstantLoad) runModes.get("Agent2");
        Assert.assertEquals(12, secondAgentRunMode.getStart());
        Assert.assertEquals(2000, secondAgentRunMode.getInitialDelay());

        ConstantLoad thirdAgentRunMode = (ConstantLoad) runModes.get("Agent3");
        Assert.assertEquals(14, thirdAgentRunMode.getStart());
        Assert.assertEquals(4000, thirdAgentRunMode.getInitialDelay());

    }

}
