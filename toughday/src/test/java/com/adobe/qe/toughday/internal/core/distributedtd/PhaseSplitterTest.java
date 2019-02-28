package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.MockTest;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.engine.Phase;
import com.adobe.qe.toughday.internal.core.distributedtd.splitters.PhaseSplitter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;

import java.util.*;
import java.util.stream.Collectors;

public class PhaseSplitterTest {
    private final PhaseSplitter phaseSplitter = new PhaseSplitter();
    private List<String> cmdLineArgs;
    private static ReflectionsContainer reflections = ReflectionsContainer.getInstance();


    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");

        reflections.getTestClasses().put("MockTest", MockTest.class);
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
    }


    @Before
    public void before() {
        cmdLineArgs = new ArrayList<>(Collections.singletonList("--host=localhost"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullPhaseThrowsException() throws CloneNotSupportedException {
        phaseSplitter.splitPhase(null, new ArrayList<>());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullListOfAgentsThrowsException() throws CloneNotSupportedException {
        phaseSplitter.splitPhase(new Phase(), null);
    }

    @Test(expected = IllegalStateException.class)
    public void testNoAvailableAgentsThrowsException() throws CloneNotSupportedException {
        phaseSplitter.splitPhase(new Phase(), new ArrayList<>());
    }

    @Test
    public void testNumberOfTasksIsEqualToTheNumberOfAgents() throws Exception {
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2");
        configuration.getPhases().forEach(phase -> {
            try {
                Map<String, Phase> taskMap = phaseSplitter.splitPhase(phase, mockAgents);
                Assert.assertEquals(mockAgents.size(), taskMap.keySet().size());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });
    }

    @Test
    public void testCountIsDistributedForTestsInTestSuite() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTest", "name=Test1", "count=201"));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2");

        Phase phase = new Configuration(cmdLineArgs.toArray(new String[0])).getPhases().get(0);
        Map<String, Phase> taskMap = phaseSplitter.splitPhase(phase, mockAgents);

        taskMap.get("Agent1").getTestSuite().getTests().forEach(test -> {
            Assert.assertEquals(101, test.getCount());
        });

        taskMap.get("Agent2").getTestSuite().getTests().forEach(test -> {
            Assert.assertEquals(100, test.getCount());
        });
    }

    @Test
    public void testEachAgentRunsTheCompleteTestSuite() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTest", "name=Test1", "--add", "MockTest", "name=Test2"));
        List<String> mockAgents = Arrays.asList("Agent1", "Agent2", "Agent3");

        Phase phase = new Configuration(cmdLineArgs.toArray(new String[0])).getPhases().get(0);
        Set<String> testNames = phase.getTestSuite().getTests().stream()
                .map(AbstractTest::getName)
                .collect(Collectors.toSet());

        Map<String, Phase> taskMap = phaseSplitter.splitPhase(phase, mockAgents);
        taskMap.forEach((key, value) -> {
            Set<String> namesDiff = value.getTestSuite().getTests().stream()
                    .map(AbstractTest::getName)
                    .collect(Collectors.toSet());
            namesDiff.removeAll(testNames);

            Assert.assertEquals(0, namesDiff.size());
        });
    }

}
