package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.MockTest;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.*;

public class DistributedPhaseMonitorTest {

    private List<String> cmdLineArgs;
    private final DistributedPhaseMonitor distributedPhaseMonitor = new DistributedPhaseMonitor();
    private static ReflectionsContainer reflections = ReflectionsContainer.getInstance();

    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");
        ((LoggerContext) LogManager.getContext(false)).reconfigure();

        reflections.getTestClasses().put("MockTest", MockTest.class);
    }


    @Before
    public void before() {
        cmdLineArgs = new ArrayList<>(Collections.singletonList("--host=localhost"));
    }

    @Test
    public void testIsPhaseExecuting() throws Exception {
        // test when phase is null
        Assert.assertFalse(this.distributedPhaseMonitor.isPhaseExecuting());

        // test when no agent is executing TD
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        this.distributedPhaseMonitor.setPhase(configuration.getPhases().get(0));

        Assert.assertFalse(this.distributedPhaseMonitor.isPhaseExecuting());

        // test when phase is executed by agents
        this.distributedPhaseMonitor.registerAgentRunningTD("Agent1");
        Assert.assertTrue(this.distributedPhaseMonitor.isPhaseExecuting());

        this.distributedPhaseMonitor.removeAgentFromActiveTDRunners("Agent1");
    }

    @Test
    public void testUpdateCountPerTest() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTest", "count=400"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2");
        this.distributedPhaseMonitor.setPhase(configuration.getPhases().get(0));

        // simulate executions per test
        Map<String, Map<String, Long>> executionsPerTest = this.distributedPhaseMonitor.getExecutions();
        Map<String, Long> executionsPerAgent = new HashMap<String, Long>() {{
           put(mockAgents.get(0), 100L);
           put(mockAgents.get(1), 100L);
        }};

        executionsPerTest.put("MockTest", executionsPerAgent);
        Assert.assertEquals(200L, (long) this.distributedPhaseMonitor.getExecutionsPerTest().get("MockTest"));
    }

    @Test
    public void testResetExecutions() {
        this.distributedPhaseMonitor.getExecutions().clear();
        Map<String, Long> executionsPerAgent = new HashMap<String, Long>() {{
           put("Agent1", 100L);
        }};

        this.distributedPhaseMonitor.getExecutions().put("MockTest", executionsPerAgent);
        this.distributedPhaseMonitor.resetExecutions();

        Assert.assertEquals(0L,
                (long) this.distributedPhaseMonitor.getExecutionsPerTest().get("MockTest"));

        this.distributedPhaseMonitor.getExecutions().clear();
    }


    @Test
    public void testGetExecutionsPerTest() {
        this.distributedPhaseMonitor.getExecutions().clear();

        Map<String, Long> executionsPerAgent = new HashMap<String, Long>() {{
            put("Agent1", 100L);
            put("Agent2", 60L);
            put("Agent3", 34L);
        }};

        this.distributedPhaseMonitor.getExecutions().put("MockTest", executionsPerAgent);
        Assert.assertEquals(194L, (long) this.distributedPhaseMonitor.getExecutionsPerTest().get("MockTest"));
    }

    @Test
    public void testRemoveAgentFromActiveTDRunners() {
        this.distributedPhaseMonitor.getExecutions().clear();

        this.distributedPhaseMonitor.registerAgentRunningTD("Agent1");
        this.distributedPhaseMonitor.registerAgentRunningTD("Agent2");

        this.distributedPhaseMonitor.getExecutions()
                .forEach((key, value) -> Assert.assertTrue(value.containsKey("Agent2")));

        this.distributedPhaseMonitor.removeAgentFromActiveTDRunners("Agent2");

        this.distributedPhaseMonitor.getExecutions()
                .forEach((key, value) -> Assert.assertFalse(value.containsKey("Agent2")));
    }


}
