package com.adobe.qe.toughday.internal.core.distributedtd;

import com.adobe.qe.toughday.MockTest;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.RedistributionInstructions;
import com.adobe.qe.toughday.internal.core.distributedtd.redistribution.RedistributionInstructionsProcessor;
import com.adobe.qe.toughday.internal.core.engine.Phase;
import com.adobe.qe.toughday.internal.core.engine.runmodes.Normal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

public class RedistributionInstructionsProcessorTest {
    private List<String> cmdLineArgs;
    private final RedistributionInstructionsProcessor instructionsProcessor = new RedistributionInstructionsProcessor();
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

    @Test(expected = IllegalArgumentException.class)
    public void testNullPhaseThrowsExceptions() throws IOException {
        this.instructionsProcessor.processInstructions("", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullInstructionsThrowsException() throws Exception{
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        this.instructionsProcessor.processInstructions(null, configuration.getPhases().get(0));
    }

    @Test
    public void testProcessInstructions() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTest", "--runmode", "type=normal", "rate=20", "interval=2s",
                "end=20", "start=15"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        Phase phase = configuration.getPhases().get(0);
        RedistributionInstructions instructions = new RedistributionInstructions();

        Map<String, String> runModeProperties = new HashMap<String, String>() {{
            put("rate", "10");
            put("interval","5s");
            put("end", "200");
        }};
        Map<String, Long> counts = new HashMap<String, Long>() {{
            put("MockTest", 1200L);
        }};

        instructions.setRunModeProperties(runModeProperties);
        instructions.setCounts(counts);

        ObjectMapper mapper = new ObjectMapper();
        String jsonInstructions = mapper.writeValueAsString(instructions);

        this.instructionsProcessor.processInstructions(jsonInstructions, phase);

        // test run mode properties were changed
        Normal runmode = (Normal)phase.getRunMode();
        Assert.assertEquals(10, runmode.getRate());
        Assert.assertEquals("5s", runmode.getInterval());
        Assert.assertEquals(200, runmode.getEnd());

        // test count properties were changed
        Assert.assertEquals(1200, phase.getTestSuite().getTest("MockTest").getCount());

    }

}
