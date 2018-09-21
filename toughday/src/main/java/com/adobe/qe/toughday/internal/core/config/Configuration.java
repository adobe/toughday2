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
package com.adobe.qe.toughday.internal.core.config;

import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.internal.core.Timestamp;
import com.adobe.qe.toughday.internal.core.config.parsers.yaml.GenerateYamlConfiguration;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.TestSuite;
import com.adobe.qe.toughday.internal.core.config.parsers.cli.CliParser;
import com.adobe.qe.toughday.internal.core.config.parsers.yaml.YamlParser;
import com.adobe.qe.toughday.internal.core.engine.PublishMode;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.metrics.Metric;
import com.adobe.qe.toughday.publishers.CSVPublisher;
import com.adobe.qe.toughday.publishers.ConsolePublisher;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.reflections.Reflections;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * An object that has all that configurations parsed and objects instantiated.
 */
public class Configuration {
    private static final Logger LOGGER = LogManager.getLogger(Configuration.class);

    private static final String DEFAULT_RUN_MODE = "normal";
    private static final String DEFAULT_PUBLISH_MODE = "simple";
    PredefinedSuites predefinedSuites = new PredefinedSuites();
    private GlobalArgs globalArgs;
    private TestSuite suite;
    private RunMode runMode;
    private PublishMode publishMode;
    private boolean defaultSuiteAddedFromConfigExclude = false;
    private boolean anyMetricAdded = false;
    private boolean anyPublisherAdded = false;
    private boolean allTestsExcluded = false;
    private static Map<Object, HashSet<String>> requiredFieldsForClassAdded = new HashMap<>();
    private static String TIMESTAMP = Timestamp.START_TIME;

    private void handleExtensions(ConfigParams configParams) {
        List<String> extensionList = new ArrayList<>();

        // look for extension jar files that should be loaded.
        Iterator<Map.Entry<Actions, ConfigParams.MetaObject>> metaObjectIterator = configParams.getItems().iterator();
        while (metaObjectIterator.hasNext()) {
            Map.Entry<Actions, ConfigParams.MetaObject> entry = metaObjectIterator.next();

            if (entry.getKey() == Actions.ADD && ((ConfigParams.ClassMetaObject)entry.getValue()).getClassName().endsWith(".jar")) {
                extensionList.add(((ConfigParams.ClassMetaObject)entry.getValue()).getClassName());
                metaObjectIterator.remove();
            }
        }

        if (extensionList.isEmpty()) {
            return;
        }
        // look for extension jar files that should be excluded.
        /*for (String itemToExclude : itemsToExcludeCopy) {
            if (itemToExclude.endsWith(".jar")) {
                configParams.getItemsToExclude().remove(itemToExclude);
                if (!extensionList.contains(itemToExclude)) {
                    throw new IllegalStateException("No extension found with name \"" + itemToExclude + "\", so we can't exclude it.");
                }
                extensionList.remove(itemToExclude);
            }
        }*/

        ReflectionsContainer.getInstance(); //make sure that the core classes are loaded by the default class loader
        List<JarFile> jarFiles = createJarFiles(extensionList);
        ClassLoader classLoader = null;
        try {
            classLoader = processJarFiles(jarFiles, formJarURLs(extensionList));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Reflections reflections = new Reflections(classLoader);

        // make reflection container aware of the new dynamically loaded classes
        ReflectionsContainer.getInstance().merge(reflections);
    }

    /**
     *  Creates a jar file for each extension file that should be loaded.
     * @param extensionList A list of names representing the jar files that should be loaded.
     */
    private List<JarFile> createJarFiles(List<String> extensionList) {
        List<JarFile> jarFiles = new ArrayList<>();
        for (String extensionFileName : extensionList) {
            try {
                JarFile jarFile = new JarFile(extensionFileName);
                jarFiles.add(jarFile);
            } catch (IOException e) {
                throw new IllegalArgumentException("Unable to find " + extensionFileName + " file.");
            }
        }

        return jarFiles;
    }

    /**
     *  Creates an URL for each jar file, using its filename.
     * @param extensionsFileNames
     * @return
     */
    private URL[] formJarURLs(List<String> extensionsFileNames) {
        List<URL> urls = new ArrayList<>();
        for (String filename : extensionsFileNames) {
            try {
                urls.add(new URL("file:" + Paths.get(filename).toAbsolutePath().toString()));
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }

        return urls.toArray(new URL[0]);
    }

    // loads all classes from the extension jar files using a new class loader.

    private ClassLoader processJarFiles(List<JarFile> jarFiles, URL[] urls) throws MalformedURLException {
        ToughdayExtensionClassLoader classLoader = new ToughdayExtensionClassLoader(urls, Thread.currentThread().getContextClassLoader());
        
        Map<String, String> newClasses = new HashMap<>();
        Thread.currentThread().setContextClassLoader(classLoader);

        for (JarFile jar : jarFiles) {
            Enumeration<JarEntry> jarContent = jar.entries();
            while (jarContent.hasMoreElements()) {
                JarEntry jarEntry = jarContent.nextElement();
                if (jarEntry.isDirectory() || !(jarEntry.getName().endsWith(".class"))) {
                    continue;
                }
                String className = jarEntry.getName().replace(".class", "");
                className = className.replaceAll("/", ".");

                // check if a class with this name already exists
                if (newClasses.containsKey(className)) {
                    throw new IllegalStateException("A class named " + className + " already exists in the jar file named " + newClasses.get(className));
                } else if (ReflectionsContainer.getInstance().containsClass(className)) {
                    throw new IllegalStateException("A class named " + className + " already exists in toughday default package.");
                } else {
                    newClasses.put(className, jar.getName());
                }

                // load class
                try {
                    classLoader.loadClass(className);
                } catch (Throwable e) {
                    LOGGER.error("Class " + className + " could not be loaded from your extension jar. If this is an extension class, you will not be able to use it.", e);
                }
            }
        }

        return classLoader;
    }


    public Configuration(String[] cmdLineArgs)
            throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, IOException {
        ConfigParams configParams = collectConfigurations(cmdLineArgs);
        ConfigParams copyOfConfigParams = ConfigParams.deepClone(configParams);
        Map<String, Class> items = new HashMap<>();

        // we should load extensions before any configuration object is created.
        handleExtensions(configParams);

        Map<String, Object> globalArgsMeta = configParams.getGlobalParams();
        for (String helpOption : CliParser.availableHelpOptions) {
           if (globalArgsMeta.containsKey(helpOption)) {
               return;
           }
        }

        this.globalArgs = createObject(GlobalArgs.class, globalArgsMeta);

        configureLogPath(globalArgs.getLogPath());

        applyLogLevel(globalArgs.getLogLevel());

        this.runMode = getRunMode(configParams);
        this.publishMode = getPublishMode(configParams);
        suite = getTestSuite(globalArgsMeta);

        for (AbstractTest abstractTest : suite.getTests()) {
            items.put(abstractTest.getName(), abstractTest.getClass());
        }

        for (Map.Entry<Actions, ConfigParams.MetaObject> item : configParams.getItems()) {
            switch (item.getKey()) {
                case ADD:
                    addItem((ConfigParams.ClassMetaObject) item.getValue(), items);
                    break;
                case CONFIG:
                    configItem((ConfigParams.NamedMetaObject) item.getValue(), items);
                    break;
                case EXCLUDE:
                    excludeItem(((ConfigParams.NamedMetaObject)item.getValue()).getName());
                    break;
            }
        }

        // Add default publishers if none is specified
        if (!anyPublisherAdded) {
            Publisher publisher = createObject(ConsolePublisher.class, new HashMap<String, Object>());
            items.put(publisher.getName(), publisher.getClass());
            this.globalArgs.addPublisher(publisher);
            publisher = createObject(CSVPublisher.class, new HashMap<String, Object>() {{
                put("append", "true");
            }});
            items.put(publisher.getName(), publisher.getClass());
            this.globalArgs.addPublisher(publisher);
        }

        // Add a default suite of tests if no test is added or no predefined suite is choosen.
        if (!defaultSuiteAddedFromConfigExclude && suite.getTests().size() == 0) {
            // Replace the empty suite with the default predefined suite if no test has been configured,
            // either by selecting a suite or manually using --add
            this.suite = predefinedSuites.getDefaultSuite();
        }

        // Add default metrics if no metric is specified.
        // TODO add better fix here?
        if (!anyMetricAdded) {
            Collection<Metric> defaultMetrics = Metric.defaultMetrics;
            for (Metric metric : defaultMetrics) {
                this.globalArgs.addMetric(metric);
            }
        }

        checkInvalidArgs(globalArgsMeta, CliParser.parserArgs);
        for (AbstractTest test : suite.getTests()) {
            test.setGlobalArgs(this.globalArgs);
        }

        // Check if we should create a configuration file for this run.
        if (this.getGlobalArgs().getSaveConfig()) {
            GenerateYamlConfiguration generateYaml = new GenerateYamlConfiguration(copyOfConfigParams, items);
            generateYaml.createYamlConfigurationFile();
        }
    }

    private void addItem(ConfigParams.ClassMetaObject itemToAdd, Map<String, Class> items) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (ReflectionsContainer.getInstance().isTestClass(itemToAdd.getClassName())) {
            if (defaultSuiteAddedFromConfigExclude) {
                throw new IllegalStateException("Configuration/exclusion of test ahead of addition");
            }
            allTestsExcluded = false;

            AbstractTest test = createObject(ReflectionsContainer.getInstance().getTestClass(itemToAdd.getClassName()), itemToAdd.getParameters());
            suite.add(test);
            items.put(test.getName(), test.getClass());
            checkInvalidArgs(itemToAdd.getParameters());
        } else if (ReflectionsContainer.getInstance().isPublisherClass(itemToAdd.getClassName())) {
            Publisher publisher = createObject(
                    ReflectionsContainer.getInstance().getPublisherClass(itemToAdd.getClassName()),
                    itemToAdd.getParameters());
            items.put(publisher.getName(), publisher.getClass());

            checkInvalidArgs(itemToAdd.getParameters());
            this.globalArgs.addPublisher(publisher);
            anyPublisherAdded = true;
        } else if (ReflectionsContainer.getInstance().isMetricClass(itemToAdd.getClassName())) {
            Metric metric = createObject(ReflectionsContainer.getInstance().getMetricClass(itemToAdd.getClassName()),
                    itemToAdd.getParameters());
            items.put(metric.getName(), metric.getClass());

            checkInvalidArgs(itemToAdd.getParameters());
            this.globalArgs.addMetric(metric);
            anyMetricAdded = true;
        } else if (itemToAdd.getClassName().equals("BASICMetrics")) {
            Collection<Metric> basicMetrics = Metric.basicMetrics;
            for (Metric metric : basicMetrics) {
                this.globalArgs.addMetric(metric);
                items.put(metric.getName(), metric.getClass());
            }
            anyMetricAdded = true;
        } else if (itemToAdd.getClassName().equals("DEFAULTMetrics")) {
            Collection<Metric> defaultMetrics = Metric.defaultMetrics;

            for (Metric metric : defaultMetrics) {
                this.globalArgs.addMetric(metric);
                items.put(metric.getName(), metric.getClass());
            }
            anyMetricAdded = true;
        } else {
            throw new IllegalArgumentException("Unknown publisher, test or metric class: " + itemToAdd.getClassName());
        }
    }

    private void configItem(ConfigParams.NamedMetaObject itemMeta, Map<String, Class> items) throws InvocationTargetException, IllegalAccessException {
        // if the suite does not contain the provided name, it might be
        // a publisher/metric (class) name OR, in case no tests have been added,
        // the name of a test from the default suite
        // if the latter is the case, the default suite has to be added in the configuration
        // defaultSuiteAddedFromConfigExclude will mark this occurrence and, if it is set to
        // true, attempting to --add a test after this will cause an exception to be thrown
        if (!suite.contains(itemMeta.getName())
                && !ReflectionsContainer.getInstance().isMetricClass(itemMeta.getName())
                && !ReflectionsContainer.getInstance().isPublisherClass(itemMeta.getName())
                && !globalArgs.containsPublisher(itemMeta.getName())
                && !globalArgs.containsMetric(itemMeta.getName())
                && !allTestsExcluded
                && suite.getTests().isEmpty()) {
            this.suite = predefinedSuites.getDefaultSuite();
            defaultSuiteAddedFromConfigExclude = true;
        }

        if (suite.contains(itemMeta.getName())) {

            //check if all were excluded
            AbstractTest testObject = suite.getTest(itemMeta.getName());
            int index = suite.remove(testObject);
            setObjectProperties(testObject, itemMeta.getParameters(), false);
            suite.add(testObject, index);
            items.put(testObject.getName(), testObject.getClass());
        } else if (globalArgs.containsPublisher(itemMeta.getName())) {
            Publisher publisherObject = globalArgs.getPublisher(itemMeta.getName());
            String name = publisherObject.getName();
            setObjectProperties(publisherObject, itemMeta.getParameters(), false);
            if (!name.equals(publisherObject.getName())) {
                this.getGlobalArgs().updatePublisherName(name, publisherObject.getName());
            }

            items.put(name, publisherObject.getClass());
        } else if (globalArgs.containsMetric(itemMeta.getName())) {
            Metric metricObject = globalArgs.getMetric(itemMeta.getName());
            String name = metricObject.getName();
            setObjectProperties(metricObject, itemMeta.getParameters(), false);
            if (!name.equals(metricObject.getName())) {
                this.getGlobalArgs().updateMetricName(name, metricObject.getName());
            }

            items.put(name, metricObject.getClass());
        } else {
            throw new IllegalStateException("No test/publisher/metric found with name \"" + itemMeta.getName() + "\", so we can't configure it.");
        }

        checkInvalidArgs(itemMeta.getParameters());
    }

    private void excludeItem(String itemName) {
        if (!suite.contains(itemName) && !allTestsExcluded && suite.getTests().isEmpty()
                && !ReflectionsContainer.getInstance().isPublisherClass(itemName)
                && !ReflectionsContainer.getInstance().isMetricClass(itemName)
                && !globalArgs.containsMetric(itemName)
                && !globalArgs.containsPublisher(itemName)) {
            this.suite = predefinedSuites.getDefaultSuite();
            defaultSuiteAddedFromConfigExclude = true;
        }

        if (suite.contains(itemName)) {
            suite.remove(itemName);
            if (suite.getTests().isEmpty()) {
                allTestsExcluded = true;
            }
        } else if (globalArgs.containsPublisher(itemName)) {
            globalArgs.removePublisher(itemName);
        } else if (getGlobalArgs().containsMetric(itemName)) {
            globalArgs.removeMetric(itemName);
        } else {
            throw new IllegalStateException("No test/publisher/metric found with name \"" + itemName + "\", so we can't exclude it.");
        }
    }

    private void configureLogPath(String logPath) throws IOException {
        if (!logPath.equals(".")) {
            final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
            final org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();

            for (Map.Entry<String, Appender> appenderEntry : config.getAppenders().entrySet()) {
                appenderEntry.getValue().stop();
            }

            File newFolder = new File(logPath + "/logs_" + TIMESTAMP + "/");
            File folder = new File("logs_" + TIMESTAMP + "/");

            Path movefrom = FileSystems.getDefault().getPath(folder.getPath());
            Path target = FileSystems.getDefault().getPath(newFolder.getPath());

            Files.move(movefrom, target, StandardCopyOption.REPLACE_EXISTING);

            for (Map.Entry<String, Appender> appenderEntry : config.getAppenders().entrySet()) {
                appenderEntry.getValue().start();
            }
            ctx.reconfigure();
        }
    }

    /**
     * Method for getting the property from a setter method
     *
     * @param methodName
     * @return
     */
    public static String propertyFromMethod(String methodName) {
        return methodName.startsWith("set") || methodName.startsWith("get") ? StringUtils.lowerCase(methodName.substring(3)) : StringUtils.lowerCase(methodName);
    }

    /**
     * Method for setting an object properties annotated with ConfigArgSet using reflection
     *
     * @param object
     * @param args
     * @param applyDefaults whether to apply default values from properties or not
     * @param <T>
     * @return the object with the properties set
     * @throws InvocationTargetException caused by reflection
     * @throws IllegalAccessException    caused by reflection
     */
    public static <T> T setObjectProperties(T object, Map<String, Object> args, boolean applyDefaults) throws InvocationTargetException, IllegalAccessException {
        Class classObject = object.getClass();
        LOGGER.info("Configuring object of class: " + classObject.getSimpleName()+" ["+classObject.getName()+"]");
        for (Method method : classObject.getMethods()) {
            ConfigArgSet annotation = method.getAnnotation(ConfigArgSet.class);
            if (annotation == null) {
                continue;
            }

            String property = propertyFromMethod(method.getName());
            Object value = args.remove(property);
            if (value == null) {
                if (requiredFieldsForClassAdded.containsKey(object)
                        && requiredFieldsForClassAdded.get(object).contains(property)) {
                    continue;
                }

                if (annotation.required()) {
                    throw new IllegalArgumentException("Property \"" + property + "\" is required for class " + classObject.getSimpleName());
                } else if(applyDefaults) {
                    String defaultValue = annotation.defaultValue();
                    if (defaultValue.compareTo("") != 0) {
                        LOGGER.info("\tSetting property \"" + property + "\" to default value: \"" + defaultValue + "\"");
                        method.invoke(object, defaultValue);
                    }
                }
            } else {
                if (annotation.required()) {
                    if (requiredFieldsForClassAdded.containsKey(object)) {
                        requiredFieldsForClassAdded.get(object).add(property);
                    } else {
                        requiredFieldsForClassAdded.put(object,
                                new HashSet<>(Collections.singletonList(property)));
                    }
                }
                LOGGER.info("\tSetting property \"" + property + "\" to: \"" + value + "\"");
                //TODO fix this ugly thing: all maps should be String -> String, but snake yaml automatically converts Integers, etc. so for now we call toString.
                method.invoke(object, value.toString());
            }
        }
        return object;
    }

    /**
     * Method for creating and configuring an object using reflection
     *
     * @param classObject
     * @param args
     * @param <T>
     * @return
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     * @throws NoSuchMethodException
     */
    public static <T> T createObject(Class<? extends T> classObject, Map<String, Object> args)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {

        Constructor constructor = null;
        try {
            constructor = classObject.getConstructor(null);
        } catch (NoSuchMethodException e) {
            NoSuchMethodException explicitException = new NoSuchMethodException(classObject.getSimpleName()
                    + " class must have a constructor without arguments");
            explicitException.initCause(e);
            throw explicitException;
        }

        T object = (T) constructor.newInstance();
        setObjectProperties(object, args, true);
        return object;
    }

    private TestSuite getTestSuite(Map<String, Object> globalArgsMeta)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (!globalArgsMeta.containsKey("suite"))
            return createObject(TestSuite.class, globalArgsMeta);

        /* TODO allow multiple predefined test suites.
         What happens with the setup step if two or more suites have setup steps? */

        TestSuite testSuite = new TestSuite();
        String[] testSuiteNames = String.valueOf(globalArgsMeta.remove("suite")).split(",");
        for (String testSuiteName : testSuiteNames) {
            if (!predefinedSuites.containsKey(testSuiteName)) {
                throw new IllegalArgumentException("Unknown suite: " + testSuiteName);
            }
            testSuite.addAll(predefinedSuites.get(testSuiteName));
        }
        return testSuite;
    }

    private void checkInvalidArgs(Map<String, Object> args, List<Object>... whitelisted) {
        Map<String, Object> argsCopy = new HashMap<>();
        argsCopy.putAll(args);
        args = argsCopy;

        for (int i = 0; i < whitelisted.length; i++) {
            List<Object> whitelist = whitelisted[i];
            for (Object whitelistedArg : whitelist) {
                args.remove(whitelistedArg);
            }
        }

        if (args.size() == 0) return;

        for (String key : args.keySet()) {
            LOGGER.error("Invalid property \"" + key + "\"");
        }

        throw new IllegalStateException("There are invalid properties in the configuration. Please check thoughday.log.");
    }


    private RunMode getRunMode(ConfigParams configParams)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Map<String, Object> runModeParams = configParams.getRunModeParams();
        if (runModeParams.size() != 0 && !runModeParams.containsKey("type")) {
            throw new IllegalStateException("The Run mode doesn't have a type");
        }


        String type = runModeParams.size() != 0 ? String.valueOf(runModeParams.get("type")) : DEFAULT_RUN_MODE;
        Class<? extends RunMode> runModeClass = ReflectionsContainer.getInstance().getRunModeClasses().get(type);

        if (runModeClass == null) {
            throw new IllegalStateException("A run mode with type \"" + type + "\" does not exist");
        }

        runModeParams.remove("type");

        RunMode runMode = createObject(runModeClass, runModeParams);
        checkInvalidArgs(runModeParams);

        return runMode;
    }

    private PublishMode getPublishMode(ConfigParams configParams)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Map<String, Object> publishModeParams = configParams.getPublishModeParams();
        if (publishModeParams.size() != 0 && !publishModeParams.containsKey("type")) {
            throw new IllegalStateException("The Publish mode doesn't have a type");
        }

        String type = publishModeParams.size() != 0 ? String.valueOf(publishModeParams.get("type")) : DEFAULT_PUBLISH_MODE;
        Class<? extends PublishMode> publishModeClass = ReflectionsContainer.getInstance().getPublishModeClasses().get(type);

        if (publishModeClass == null) {
            throw new IllegalStateException("A publish mode with type \"" + type + "\" does not exist");
        }

        publishModeParams.remove("type");

        PublishMode publishMode = createObject(publishModeClass, publishModeParams);
        checkInvalidArgs(publishModeParams);

        return publishMode;
    }

    private void applyLogLevel(Level level) {
        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
        for (LoggerConfig loggerConfig : config.getLoggers().values()) {
            // we must keep logging only errors from reflections, in order to avoid irrelevant warning messages when loading an extension into TD.
            if (!loggerConfig.getName().equals("org.reflections.Reflections")) {
                loggerConfig.setLevel(level);
            }
        }
        System.setProperty("toughday.log.level", level.name());
        ctx.updateLoggers();  // This causes all Loggers to refetch information from their LoggerConfig.
    }

    private ConfigParams collectConfigurations(String[] cmdLineArgs) {
        ConfigParams configs = new YamlParser().parse(cmdLineArgs);
        configs.merge(new CliParser().parse(cmdLineArgs));
        return configs;
    }

    /**
     * Getter for the predefined suites
     *
     * @return
     */
    public HashMap<String, TestSuite> getPredefinedSuites() {
        return predefinedSuites;
    }

    /**
     * Getter for the suite
     *
     * @return
     */
    public TestSuite getTestSuite() {
        return suite;
    }

    /**
     * Getter for the global args
     *
     * @return
     */
    public GlobalArgs getGlobalArgs() {
        return globalArgs;
    }

    /**
     * Getter for the run mode
     *
     * @return
     */
    public RunMode getRunMode() {
        return runMode;
    }

    /**
     * Getter for the publish mode
     *
     * @return
     */
    public PublishMode getPublishMode() {
        return publishMode;
    }

    /**
     * Method for getting the parser for the configuration.
     *
     * @param args
     * @return
     */
    private ConfigurationParser getConfigurationParser(String[] args) {
        //TODO Insert logic here to select from other types of parsers
        return new CliParser();
    }

    public static Map<Object, HashSet<String>> getRequiredFieldsForClassAdded() {
        return requiredFieldsForClassAdded;
    }
}
