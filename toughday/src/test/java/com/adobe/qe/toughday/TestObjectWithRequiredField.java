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

import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.Timestamp;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class TestObjectWithRequiredField {
    private List<String> cmdLineArgs;

    @BeforeClass
    public static void onlyOnce() {
        System.setProperty("logFileName", ".");

        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
        ctx.reconfigure();

        for (Map.Entry<String, Appender> appenderEntry : config.getAppenders().entrySet()) {
            appenderEntry.getValue().start();
        }

        ReflectionsContainer.getInstance().getTestClasses().put("MockTestRequiredField", MockTestRequiredField.class);
        ReflectionsContainer.getInstance().getTestClasses().put("MockTestTwoRequiredFields", MockTestTwoRequiredFields.class);
    }

    @Before
    public void before() {
        cmdLineArgs = new ArrayList<>(Collections.singletonList("--host=localhost"));
    }

    @Test
    public void testSimple() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        new Configuration(cmdLineArgs.toArray(new String[0]));
    }

    @Test
    public void addSimple() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, InterruptedException, IOException {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTestRequiredField", "name=RandomTestName"));
        new Configuration(cmdLineArgs.toArray(new String[0]));
    }

    @Test
    public void addSimpleTwo() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTestTwoRequiredFields", "name=RandomTestName", "mock=Something"));
        new Configuration(cmdLineArgs.toArray(new String[0]));
    }

    @Test
    public void addWithoutName() {
        try {
            cmdLineArgs.addAll(Arrays.asList("--add", "MockTestRequiredField"));
            new Configuration(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Added test without the required field.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void addWithoutNameTwo() {
        try {
            cmdLineArgs.addAll(Arrays.asList("--add", "MockTestTwoRequiredFields", "mock=Something"));
            new Configuration(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Added test without all the required fields.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void addWithoutNameConfig() {
        try {
            cmdLineArgs.addAll(Arrays.asList("--add", "MockTestRequiredField"));
            cmdLineArgs.addAll(Arrays.asList("--config", "MockTestRequiredField", "name=RandomTestName"));
            new Configuration(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Adding required field from --config without first adding it with --add.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void configFail() {
        try {
            cmdLineArgs.addAll(Arrays.asList("--add", "MockTestRequiredField", "name=RandomTestName"));
            cmdLineArgs.addAll(Arrays.asList("--config", "MockTestRequiredField", "name=RandomTestNameAgain"));
            new Configuration(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Configuring test using class name after naming it at addition.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void configPass() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTestRequiredField", "name=RandomTestName"));
        cmdLineArgs.addAll(Arrays.asList("--config", "RandomTestName", "name=RandomTestNameAgain"));
        new Configuration(cmdLineArgs.toArray(new String[0]));
    }

    @Test
    public void configPassTwo() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTestTwoRequiredFields", "name=RandomTestName", "mock=Something"));
        cmdLineArgs.addAll(Arrays.asList("--config", "RandomTestName", "name=RandomTestNameAgain"));
        new Configuration(cmdLineArgs.toArray(new String[0]));
    }

    @Test
    public void excludeFail() {
        try {
            cmdLineArgs.addAll(Arrays.asList("--add", "MockTestRequiredField", "name=RandomTestName"));
            cmdLineArgs.addAll(Arrays.asList("--exclude", "MockTestRequiredField"));
            new Configuration(cmdLineArgs.toArray(new String[0]));
            Assert.fail("Excluding test using class name after naming it at addition.");
        } catch (Exception e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void excludePass() throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException, IOException {
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTestRequiredField", "name=SomeName"));
        cmdLineArgs.addAll(Arrays.asList("--add", "MockTestRequiredField", "name=RandomTestName"));
        cmdLineArgs.addAll(Arrays.asList("--exclude", "RandomTestName"));
        new Configuration(cmdLineArgs.toArray(new String[0]));
    }

    @After
    public void afterEach() {
        Configuration.getRequiredFieldsForClassAdded().clear();
    }

    @AfterClass
    public static void deleteFiles() {
        new File("toughday_" + Timestamp.START_TIME + ".yaml").delete();
        ((LoggerContext) LogManager.getContext(false)).reconfigure();
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
