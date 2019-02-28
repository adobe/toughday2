package com.adobe.qe.toughday.internal.core.config.parsers.yaml;

import com.adobe.qe.toughday.MockTest;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import com.adobe.qe.toughday.internal.core.distributedtd.DummyRunMode;
import com.adobe.qe.toughday.mocks.MockMetric;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.Assert;
import org.junit.rules.ExpectedException;

import java.util.*;

public class YamlDumpConfigurationTest {
    private List<String> cmdLineArgs;
    private static ReflectionsContainer reflections = ReflectionsContainer.getInstance();
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();


    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");

        reflections.getTestClasses().put("MockTest", MockTest.class);
        reflections.getMetricClasses().put("MockMetric", MockMetric.class);
        reflections.getRunModeClasses().put("DummyRunMode", DummyRunMode.class);
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
    }

    @Before
    public void before() {
        cmdLineArgs = new ArrayList<>(Collections.singletonList("--host=localhost"));
    }

    @Test
    public void testNullCofigurationThrowsException() {
        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("Configuration must not be null.");

        new YamlDumpConfiguration(null);
    }

    @Test()
    public void testCollectConfigurablePropertiesWithIncompatibleTypeThrowsException() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTest"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        YamlDumpConfiguration yamlDumpConfiguration = new YamlDumpConfiguration(configuration);

        expectedEx.expect(IllegalArgumentException.class);
        expectedEx.expectMessage("The object must have the specified type.");

        yamlDumpConfiguration.collectConfigurableProperties(DummyRunMode.class, configuration.getTestSuite().getTest("MockTest"));
    }

    @Test
    public void testCollectConfigurablePropertiesContainsOnlyModifiedProperties() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=dummyrunmode", "property1=value1", "property2=value2"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        YamlDumpConfiguration yamlDumpConfiguration = new YamlDumpConfiguration(configuration);


        Map<String, Object> actualCollectedPropsForDummyRunMode =
                yamlDumpConfiguration.collectConfigurableProperties(DummyRunMode.class, configuration.getRunMode());

        Map<String, Object> expectedCollectedPropsForMockTest =
                new HashMap<String, Object>() {{
                    put("property1", "value1");
                    put("property2", "value2");
                }};

        Assert.assertEquals(expectedCollectedPropsForMockTest, actualCollectedPropsForDummyRunMode);
    }

    @Test
    public void testDumpedConfiguration() throws Exception{
        cmdLineArgs.addAll(Arrays.asList("--duration=10s", "--phase", "name=phase123", "--add", "MockTest", "name=Test1", "count=201",
                "--add" , "MockMetric", "name=Metric123", "--runmode", "type=dummyrunmode", "--add", "CSVPublisher"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        YamlDumpConfiguration yamlDumpConfiguration = new YamlDumpConfiguration(configuration);

        String expectedYaml =
                "globals:\n" +
                "  duration: 10s\n" +
                "  host: localhost\n" +
                "phases:\n" +
                "- duration: 10s\n" +
                "  metrics:\n" +
                "  - add: MockMetric\n" +
                "    properties:\n" +
                "      name: Metric123\n" +
                "  name: phase123\n" +
                "  publishers:\n" +
                "  - add: CSVPublisher\n" +
                "    properties:\n" +
                "      name: CSVPublisher\n" +
                "      aggregatedpublish: false\n" +
                "  publishmode:\n" +
                "    type: simple\n" +
                "  runmode:\n" +
                "    type: dummyrunmode\n" +
                "  tests:\n" +
                "  - add: MockTest\n" +
                "    properties:\n" +
                "      name: Test1\n" +
                "      count: 201\n";

        Assert.assertEquals(expectedYaml, yamlDumpConfiguration.generateConfigurationObject());
    }

    @Test
    public void testGetGlobals() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--duration=10s", "--port=1000", "--user=testUser", "--timeout=10"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        YamlDumpConfiguration yamlDumpConfiguration = new YamlDumpConfiguration(configuration);

        Map<String, Object> actualGlobals = yamlDumpConfiguration.getGlobals();

        Map<String, Object> expectedGlobals = new HashMap<String, Object>() {{
            put("host", "localhost");
            put("duration", "10s");
            put("port", 1000);
            put("user", "testUser");
            put("timeout", 10L);
        }};

        Assert.assertEquals(expectedGlobals, actualGlobals);
    }

    @Test
    public void testGetDistributedConfig() throws Exception {
        cmdLineArgs.addAll(Arrays.asList("--distributedconfig", "driverip=10.0.0.1", "heartbeatinterval=10s"));
        Configuration configuration = new Configuration(cmdLineArgs.toArray(new String[0]));
        YamlDumpConfiguration yamlDumpConfiguration = new YamlDumpConfiguration(configuration);

        Map<String, Object> actualDistributedConfig = yamlDumpConfiguration.getDistributedConfig();
        Map<String, Object> expectedDistributedConfig = new HashMap<String, Object>() {{
            put("driverip", "10.0.0.1");
            put("heartbeatinterval", "10s");
        }};

        Assert.assertEquals(expectedDistributedConfig, actualDistributedConfig);
    }


}
