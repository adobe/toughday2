package com.adobe.qe.toughday.internal.core;

import com.adobe.qe.toughday.LogFileEraser;
import com.adobe.qe.toughday.internal.core.config.Actions;
import com.adobe.qe.toughday.internal.core.config.ConfigParams;
import com.adobe.qe.toughday.internal.core.config.parsers.cli.CliParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

public class CliParserTest {
    private ArrayList<String> cmdLineArgs = new ArrayList<>();
    private CliParser cliParser = new CliParser();

    @BeforeClass
    public static void beforeAll() {
        System.setProperty("logFileName", ".");
    }

    @Before
    public void before() {
        cmdLineArgs.clear();
    }

    @Test
    public void testRandomWordsBefore() {
        cmdLineArgs.addAll(Arrays.asList("random1", "random2", "--host=localhost"));

        try {
            cliParser.parse(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Random words should not be accepted as parameters before the first correct --argument.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testRandomWordsAfter() {
        cmdLineArgs.addAll(Arrays.asList("--host=localhost", "random"));

        try {
            cliParser.parse(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Random words should not be accepted as parameters after correct --arguments.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void testRunMode() {
        cmdLineArgs.addAll(Arrays.asList("--runmode", "type=normal", "concurrency=200", "--duration=10s", "--host=localhost", "--add",
                                            "CreateUserTest", "weight=5"));
        ConfigParams configParams = cliParser.parse(cmdLineArgs.toArray(new String[0]));
        Map<String, Object> runModeParams = configParams.getRunModeParams();

        Assert.assertEquals(runModeParams.size(), 2);
        Assert.assertEquals(runModeParams.get("type"), "normal");
        Assert.assertEquals(runModeParams.get("concurrency").toString(), "200");

        cmdLineArgs.clear();
        cmdLineArgs.addAll(Arrays.asList("--host=localhost", "--add", "CreateUserTest", "weight=5",
                "--runmode", "type=constantload", "start=0", "end=50", "rate=10", "interval=2s"));
        configParams = cliParser.parse(cmdLineArgs.toArray(new String[0]));
        runModeParams = configParams.getRunModeParams();

        Assert.assertEquals(runModeParams.size(), 5);
        Assert.assertEquals(runModeParams.get("type"), "constantload");
        Assert.assertEquals(runModeParams.get("start").toString(), "0");
        Assert.assertEquals(runModeParams.get("end").toString(), "50");
        Assert.assertEquals(runModeParams.get("rate").toString(), "10");
        Assert.assertEquals(runModeParams.get("interval").toString(), "2s");
    }

    @Test
    public void testPublishMode() {
        cmdLineArgs.addAll(Arrays.asList("--host=localhost", "--duration=10s", "--timeout=10", "--add", "ConsolePublisher",
                "--publishmode", "type=simple", "--add", "Average", "name=avg", "decimals=3"));

        ConfigParams configParams = cliParser.parse(cmdLineArgs.toArray(new String[0]));
        Map<String, Object> publishModeParams = configParams.getPublishModeParams();

        Assert.assertEquals(publishModeParams.size(), 1);
        Assert.assertEquals(publishModeParams.get("type"), "simple");

        cmdLineArgs.clear();
        cmdLineArgs.addAll(Arrays.asList("--host=localhost", "--duration=10s", "--timeout=10", "--add", "ConsolePublisher",
                "--publishmode", "type=intervals", "interval=2s", "--add", "Average", "name=avg", "decimals=3"));
        configParams = cliParser.parse(cmdLineArgs.toArray(new String[0]));
        publishModeParams = configParams.getPublishModeParams();

        Assert.assertEquals(publishModeParams.size(), 2);
        Assert.assertEquals(publishModeParams.get("type"), "intervals");
        Assert.assertEquals(publishModeParams.get("interval"), "2s");
    }

    @Test
    public void testActions() {
        cmdLineArgs.addAll(Arrays.asList("--host=localhost", "--duration=10s", "--timeout=10", "--add",  "ConsolePublisher",
                "--publishmode", "type=simple", "--add", "Average", "name=avg", "decimals=3", "--runmode", "type=constantload",
                "--config", "ConsolePublisher", "name=cp", "--add", "CreateUserTest", "--add", "CreatePageTreeTest", "--exclude", "CreateUserTest"));
        ConfigParams configParams = cliParser.parse(cmdLineArgs.toArray(new String[0]));

        Assert.assertEquals(configParams.getItems().size(), 6);

        int add, config, exclude;
        add = config = exclude = 0;
        for (Map.Entry<Actions, ConfigParams.MetaObject> item : configParams.getItems()) {
            switch (item.getKey()) {
                case ADD:
                    ++add;
                    break;
                case CONFIG:
                    ++config;
                    break;
                case EXCLUDE:
                    ++exclude;
                    break;
            }
        }

        Assert.assertEquals(add, 4);
        Assert.assertEquals(config, 1);
        Assert.assertEquals(exclude, 1);
    }

    @Test
    public void testGlobalArgs() {
        cmdLineArgs.addAll(Arrays.asList("--host=localhost", "--duration=10s", "--timeout=10", "--add",  "ConsolePublisher",
                "--publishmode", "type=simple", "--add", "Average", "name=avg", "decimals=3", "--runmode", "type=constantload",
                "--config", "ConsolePublisher", "name=cp", "--add", "CreateUserTest", "--add", "CreatePageTreeTest",
                "--exclude", "CreateUserTest", "--loglevel=debug"));

        ConfigParams configParams = cliParser.parse(cmdLineArgs.toArray(new String[0]));
        Map<String, Object> globalParams = configParams.getGlobalParams();

        Assert.assertEquals(globalParams.size(), 4);
        Assert.assertEquals(globalParams.get("host"), "localhost");
        Assert.assertEquals(globalParams.get("duration"), "10s");
        Assert.assertEquals(globalParams.get("timeout").toString(), "10");
        Assert.assertEquals(globalParams.get("loglevel"), "debug");
    }

    @After
    public  void deleteFiles() {
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
