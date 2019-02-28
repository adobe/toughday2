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
import com.adobe.qe.toughday.api.feeders.Feeder;
import com.adobe.qe.toughday.internal.core.Timestamp;
import com.adobe.qe.toughday.internal.core.config.parsers.yaml.GenerateYamlConfiguration;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.TestSuite;
import com.adobe.qe.toughday.internal.core.config.parsers.cli.CliParser;
import com.adobe.qe.toughday.internal.core.config.parsers.yaml.YamlParser;
import com.adobe.qe.toughday.internal.core.engine.Phase;
import com.adobe.qe.toughday.internal.core.engine.PublishMode;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.DistributedConfig;
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

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * An object that has all that configurations parsed and objects instantiated.
 */
public class Configuration {
    private static final Logger LOGGER = LogManager.getLogger(Configuration.class);

    private static final String DEFAULT_RUN_MODE = "normal";
    private static final String DEFAULT_PUBLISH_MODE = "simple";
    private static Map<Object, HashSet<String>> requiredFieldsForClassAdded = new HashMap<>();
    private static String TIMESTAMP = Timestamp.START_TIME;

    private PredefinedSuites predefinedSuites = new PredefinedSuites();
    private GlobalArgs globalArgs;
    private DistributedConfig distributedConfig;
    private RunMode runMode;
    private PublishMode publishMode;
    private TestSuite globalSuite;
    private Map<String, Publisher> globalPublishers = new HashMap<>();
    private Map<String, Metric> globalMetrics = new LinkedHashMap<>();
    private List<Phase> phases = new ArrayList<>();
    private Set<Phase> phasesWithoutDuration = new HashSet<>();
    private ConfigParams configParams;
    private boolean defaultSuiteAddedFromConfigExclude = false;
    private boolean anyMetricAdded = false;
    private boolean anyPublisherAdded = false;
    private boolean allTestsExcluded = false;
    private Map<String, Feeder> feeders = new LinkedHashMap<>();
    private Map<String, Object> objects = new HashMap<>();

    public Configuration(String yamlConfig)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IOException, IllegalAccessException {
        ConfigParams configParams = new YamlParser().parse(yamlConfig);
        buildConfiguration(configParams);
    }

    public Configuration(String[] cmdLineArgs)
            throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException, IOException {
        ConfigParams configParams = collectConfigurations(cmdLineArgs);
        buildConfiguration(configParams);

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

    public static Map<Object, HashSet<String>> getRequiredFieldsForClassAdded() {
        return requiredFieldsForClassAdded;
    }

    public ConfigParams getConfigParams() {
        return this.configParams;
    }

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
     * Creates a jar file for each extension file that should be loaded.
     *
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
     * Creates an URL for each jar file, using its filename.
     *
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

    public boolean executeInDitributedMode() {
        return !configParams.getDistributedConfigParams().isEmpty() &&
                !this.getDistributedConfig().getAgent() &&
                !this.getDistributedConfig().getDriver();
    }

    private void buildConfiguration(ConfigParams configParams) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException, IOException {
        this.configParams = ConfigParams.deepClone(configParams);
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

        this.distributedConfig = createObject(DistributedConfig.class, configParams.getDistributedConfigParams());
        this.globalArgs = createObject(GlobalArgs.class, globalArgsMeta);

        configureLogPath(globalArgs.getLogPath());

        applyLogLevel(globalArgs.getLogLevel());

        this.runMode = getRunMode(new HashMap<>(configParams.getRunModeParams()));
        this.publishMode = getPublishMode(new HashMap<>(configParams.getPublishModeParams()));
        globalSuite = getTestSuite(globalArgsMeta);

        convertActionItems(configParams.getItems(), items, globalSuite, globalPublishers, globalMetrics);

        createPhases(configParams, globalSuite, items);

        checkInvalidArgs(globalArgsMeta, CliParser.parserArgs);

        // Check if we should create a configuration file for this run.
        if (this.getGlobalArgs().getSaveConfig()) {
            GenerateYamlConfiguration generateYaml = new GenerateYamlConfiguration(copyOfConfigParams, items);
            generateYaml.createYamlConfigurationFile();
        }

        objects = null;
    }

    private void createPhases(ConfigParams configParams, TestSuite globalSuite, Map<String, Class> items) throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, InvocationTargetException {

        // if there were no phases configured, create a phase that has the global configuration
        if (configParams.getPhasesParams().isEmpty()) {
            Phase phase = createPhase(configParams, new PhaseParams(), globalSuite, globalPublishers, globalMetrics, items);
            phases.add(phase);
            configureDurationForPhases();

            return;
        }

        // map names to phases to keep track of them
        for (PhaseParams phaseParams : configParams.getPhasesParams()) {
            if (phaseParams.getProperties().get("name") != null) {
                if (PhaseParams.namedPhases.containsKey(phaseParams.getProperties().get("name").toString())) {
                    throw new IllegalArgumentException("There is already a phase named \"" + phaseParams.getProperties().get("name") + "\".");
                }

                PhaseParams.namedPhases.put(phaseParams.getProperties().get("name").toString(), phaseParams);
            }
        }

        // configure all the phases based on their params
        for (PhaseParams phaseParams : configParams.getPhasesParams()) {
            defaultSuiteAddedFromConfigExclude = false;
            allTestsExcluded = false;
            anyMetricAdded = false;
            anyPublisherAdded = false;

            getConfigurationFromAnotherPhase(phaseParams);

            TestSuite suite = new TestSuite();
            for (AbstractTest test : globalSuite.getTests()) {
                suite.add(test.clone());
            }

            Map<String, Publisher> publishers = new HashMap<>(globalPublishers);
            Map<String, Metric> metrics = new LinkedHashMap<>(globalMetrics);

            convertActionItems(phaseParams.getItems(), items, suite, publishers, metrics);

            Phase phase = createPhase(configParams, phaseParams, suite, publishers, metrics, items);

            phases.add(phase);
        }

        configureDurationForPhases();
    }

    private Phase createPhase(ConfigParams configParams, PhaseParams phaseParams, TestSuite suite, Map<String, Publisher> publishers,
                              Map<String, Metric> metrics, Map<String, Class> items) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        // if no run mode was provided in the phase, get the global one
        if (phaseParams.getRunmode().isEmpty()) {
            phaseParams.setRunmode(configParams.getRunModeParams());
        }

        // if no publish mode was provided in the phase, get the global one
        if (phaseParams.getPublishmode().isEmpty()) {
            phaseParams.setPublishmode(configParams.getPublishModeParams());
        }

        // Add a default suite of tests if no test is added or no predefined suite is chosen.
        if (!defaultSuiteAddedFromConfigExclude && suite.getTests().size() == 0) {
            // Replace the empty suite with the default predefined suite if no test has been configured,
            // either by selecting a suite or manually using --add
            suite = predefinedSuites.getDefaultSuite();
        }

        RunMode runMode = getRunMode(new HashMap<>(phaseParams.getRunmode()));
        PublishMode publishMode = getPublishMode(new HashMap<>(phaseParams.getPublishmode()));

        Phase phase = createObject(Phase.class, phaseParams.getProperties());
        checkInvalidArgs(phaseParams.getProperties());

        phase.setTestSuite(suite);
        phase.setRunMode(runMode);
        phase.setPublishMode(publishMode);

        // compute the minimum timeout of the phase
        phase.getTestSuite().setMinTimeout(globalArgs.getTimeoutInSeconds());
        for (AbstractTest test : phase.getTestSuite().getTests()) {
            // set the count (the number of executions since the beginnin of the run) of each test to 0
            phase.getCounts().put(test, new AtomicLong(0));

            // set the global args
            test.setGlobalArgs(globalArgs);

            items.put(test.getName(), test.getClass());

            if (test.getTimeout() < 0) {
                continue;
            }

            // update test suite minimum timeout
            phase.getTestSuite().setMinTimeout(Math.min(phase.getTestSuite().getMinTimeout(), test.getTimeout()));
        }

        // Add default publishers if none is specified
        if (!anyPublisherAdded && publishers.isEmpty()) {
            Publisher publisher = createObject(ConsolePublisher.class, new HashMap<String, Object>());
            items.put(publisher.getName(), publisher.getClass());
            publishers.put(publisher.getName(), publisher);
            publisher = createObject(CSVPublisher.class, new HashMap<String, Object>() {{
                put("append", "true");
            }});
            items.put(publisher.getName(), publisher.getClass());
            publishers.put(publisher.getName(), publisher);
        }

        // Add default metrics if no metric is specified.
        // TODO add better fix here?
        if (!anyMetricAdded && metrics.isEmpty()) {
            Collection<Metric> defaultMetrics = Metric.defaultMetrics;
            for (Metric metric : defaultMetrics) {
                metrics.put(metric.getName(), metric);
            }
        }

        phase.setPublishers(publishers);
        phase.setMetrics(metrics);

        return phase;
    }

    private void getConfigurationFromAnotherPhase(PhaseParams phaseParams) {
        String useconfig = null;
        // if 'useconfig' is given as input
        if (phaseParams.getProperties().get("useconfig") != null) {
            useconfig = phaseParams.getProperties().get("useconfig").toString();
            if (!PhaseParams.namedPhases.containsKey(useconfig)) {
                throw new IllegalArgumentException("Could not find phase named \"" + useconfig + "\".");
            }

            String name = phaseParams.getProperties().get("name").toString();

            // merge the current phase with the one whose name is the value of 'useconfig'
            phaseParams.merge(PhaseParams.namedPhases.get(useconfig),
                    new HashSet<>(Arrays.asList(name, useconfig)));

        }
    }

    private void convertActionItems(List<Map.Entry<Actions, ConfigParams.MetaObject>> actionItems,
                                    Map<String, Class> items, TestSuite testSuite,
                                    Map<String, Publisher> publishers, Map<String, Metric> metrics) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {
        for (Map.Entry<Actions, ConfigParams.MetaObject> item : actionItems) {
            switch (item.getKey()) {
                case ADD:
                    addItem((ConfigParams.ClassMetaObject) item.getValue(), items, testSuite, publishers, metrics);
                    break;
                case CONFIG:
                    configItem((ConfigParams.NamedMetaObject) item.getValue(), items, testSuite, publishers, metrics);
                    break;
                case EXCLUDE:
                    excludeItem(((ConfigParams.NamedMetaObject) item.getValue()).getName(), testSuite, publishers, metrics);
                    break;
            }
        }
    }

    private void configureDurationForPhases() {
        long durationLeft = globalArgs.getDuration();

        // subtract from durationLeft the duration of each phase
        // if it was given as input
        for (Phase phase : phases) {
            if (phase.getDuration() == null) {
                phasesWithoutDuration.add(phase);
            } else {
                durationLeft -= GlobalArgs.parseDurationToSeconds(phase.getDuration());
            }
        }

        if (durationLeft < 0) {
            throw new IllegalArgumentException("The sum of the phase durations is greater than the global one.");
        }

        // if there is at least one phase whose duration was not specified,
        // share the duration left between them equally
        if (phasesWithoutDuration.size() != 0) {
            long durationPerPhase = durationLeft / phasesWithoutDuration.size();
            if (durationPerPhase < 1) {
                throw new IllegalArgumentException("The duration left for the phases for which it is not specified is too small. Please make sure there is enough time left for those, as well.");
            }
            for (Phase phase : phasesWithoutDuration) {
                phase.setDuration(String.valueOf(durationPerPhase) + "s");
            }
        }
    }

    private void addItem(ConfigParams.ClassMetaObject itemToAdd, Map<String, Class> items, TestSuite suite,
                         Map<String, Publisher> publishers, Map<String, Metric> metrics) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        if (ReflectionsContainer.getInstance().isTestClass(itemToAdd.getClassName()) && suite != null) {
            if (defaultSuiteAddedFromConfigExclude) {
                throw new IllegalStateException("Configuration/exclusion of test ahead of addition");
            }
            allTestsExcluded = false;

            AbstractTest test = createObject(ReflectionsContainer.getInstance().getTestClass(itemToAdd.getClassName()), itemToAdd.getParameters(), feeders);
            suite.add(test);
            items.put(test.getName(), test.getClass());
            objects.put(test.getName(), test);
        } else if (ReflectionsContainer.getInstance().isPublisherClass(itemToAdd.getClassName())) {
            Publisher publisher = createObject(
                    ReflectionsContainer.getInstance().getPublisherClass(itemToAdd.getClassName()),
                    itemToAdd.getParameters());
            items.put(publisher.getName(), publisher.getClass());

            if (publishers.containsKey(publisher.getName())) {
                throw new IllegalStateException("There is already a publisher named \"" + publisher.getName() + "\"." +
                        "Please provide a different name using the \"name\" property.");
            }
            publishers.put(publisher.getName(), publisher);
            anyPublisherAdded = true;
        } else if (ReflectionsContainer.getInstance().isMetricClass(itemToAdd.getClassName())) {
            Metric metric = createObject(ReflectionsContainer.getInstance().getMetricClass(itemToAdd.getClassName()),
                    itemToAdd.getParameters());
            items.put(metric.getName(), metric.getClass());

            if (metrics.containsKey(metric.getName())) {
                LOGGER.warn("A metric with this name was already added. Only the last one is taken into consideration.");
            }
            metrics.put(metric.getName(), metric);
            anyMetricAdded = true;
        } else if (ReflectionsContainer.getInstance().isFeederClass(itemToAdd.getClassName())) {
            Feeder feeder = createObject(
                    ReflectionsContainer.getInstance().getFeederClass(itemToAdd.getClassName()),
                    itemToAdd.getParameters(), feeders);
            items.put(feeder.getName(), feeder.getClass());

            if (feeders.containsKey(feeder.getName())) {
                throw new IllegalStateException("There is already a feeder named \"" + feeder.getName() + "\"." +
                        "Please provide a different name using the \"name\" property.");
            }
            feeders.put(feeder.getName(), feeder);
        } else if (itemToAdd.getClassName().equals("BASICMetrics")) {
            Collection<Metric> basicMetrics = Metric.basicMetrics;
            for (Metric metric : basicMetrics) {
                if (metrics.containsKey(metric.getName())) {
                    LOGGER.warn("A metric with this name was already added. Only the last one is taken into consideration.");
                }
                metrics.put(metric.getName(), metric);
                items.put(metric.getName(), metric.getClass());
            }
            anyMetricAdded = true;
        } else if (itemToAdd.getClassName().equals("DEFAULTMetrics")) {
            Collection<Metric> defaultMetrics = Metric.defaultMetrics;

            for (Metric metric : defaultMetrics) {
                if (metrics.containsKey(metric.getName())) {
                    LOGGER.warn("A metric with this name was already added. Only the last one is taken into consideration.");
                }
                metrics.put(metric.getName(), metric);
                items.put(metric.getName(), metric.getClass());
            }
            anyMetricAdded = true;
        } else {
            throw new IllegalArgumentException("Unknown publisher, test or metric class: " + itemToAdd.getClassName());
        }

        checkInvalidArgs(itemToAdd.getParameters());
    }

    private void configItem(ConfigParams.NamedMetaObject itemMeta, Map<String, Class> items, TestSuite suite,
                            Map<String, Publisher> publishers, Map<String, Metric> metrics) throws InvocationTargetException, IllegalAccessException {
        // if the suite does not contain the provided name, it might be
        // a publisher/metric (class) name OR, in case no tests have been added,
        // the name of a test from the default suite
        // if the latter is the case, the default suite has to be added in the configuration
        // defaultSuiteAddedFromConfigExclude will mark this occurrence and, if it is set to
        // true, attempting to --add a test after this will cause an exception to be thrown
        if (suite != null && !suite.contains(itemMeta.getName())
                && !ReflectionsContainer.getInstance().isMetricClass(itemMeta.getName())
                && !ReflectionsContainer.getInstance().isPublisherClass(itemMeta.getName())
                && !publishers.containsKey(itemMeta.getName())
                && !metrics.containsKey(itemMeta.getName())
                && !allTestsExcluded
                && suite.getTests().isEmpty()) {
            suite = predefinedSuites.getDefaultSuite();
            defaultSuiteAddedFromConfigExclude = true;
        }

        if (suite != null && suite.contains(itemMeta.getName())) {

            //check if all were excluded
            AbstractTest testObject = suite.getTest(itemMeta.getName());
            objects.remove(testObject.getName());
            int index = suite.remove(testObject);
            setObjectProperties(testObject, itemMeta.getParameters(), false, feeders);
            suite.add(testObject, index);

            items.put(testObject.getName(), testObject.getClass());
            objects.put(testObject.getName(), testObject);
        } else if (publishers.containsKey(itemMeta.getName())) {
            Publisher publisherObject = publishers.get(itemMeta.getName());
            String name = publisherObject.getName();
            setObjectProperties(publisherObject, itemMeta.getParameters(), false, feeders);
            if (!name.equals(publisherObject.getName())) {
                publishers.put(publisherObject.getName(), publishers.remove(name));
            }

            items.put(name, publisherObject.getClass());
        } else if (metrics.containsKey(itemMeta.getName())) {
            Metric metricObject = metrics.get(itemMeta.getName());
            String name = metricObject.getName();
            setObjectProperties(metricObject, itemMeta.getParameters(), false, feeders);
            if (!name.equals(metricObject.getName())) {
                metrics.put(metricObject.getName(), metrics.remove(name));
            }

            items.put(name, metricObject.getClass());
        } else if (feeders.containsKey(itemMeta.getName())) {
            Feeder feederObject = feeders.get(itemMeta.getName());
            String name = feederObject.getName();
            setObjectProperties(feederObject, itemMeta.getParameters(), false, feeders);
            if (!name.equals(feederObject.getName())) {
                feeders.put(feederObject.getName(), feeders.remove(name));
            }
            items.put(name, feederObject.getClass());
        } else {
            throw new IllegalStateException("No test/publisher/metric found with name \"" + itemMeta.getName() + "\", so we can't configure it.");
        }

        checkInvalidArgs(itemMeta.getParameters());
    }

    private void excludeItem(String itemName, TestSuite suite, Map<String, Publisher> publishers, Map<String, Metric> metrics) {
        if (suite != null && !suite.contains(itemName) && !allTestsExcluded && suite.getTests().isEmpty()
                && !ReflectionsContainer.getInstance().isPublisherClass(itemName)
                && !ReflectionsContainer.getInstance().isMetricClass(itemName)
                && !metrics.containsKey(itemName)
                && !publishers.containsKey(itemName)) {
            suite = predefinedSuites.getDefaultSuite();
            defaultSuiteAddedFromConfigExclude = true;
        }

        if (suite != null && suite.contains(itemName)) {
            suite.remove(itemName);
            if (suite.getTests().isEmpty()) {
                allTestsExcluded = true;
            }
            objects.remove(itemName);
        } else if (publishers.containsKey(itemName)) {
            publishers.remove(itemName);
        } else if (metrics.containsKey(itemName)) {
            metrics.remove(itemName);
        } else if (feeders.containsKey(itemName)) {
            feeders.remove(itemName);
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
    //TODO figure out if we can make this public static again
    public <T> T setObjectProperties(T object, Map<String, Object> args, boolean applyDefaults, Map<String, Feeder> feedersContext) throws InvocationTargetException, IllegalAccessException {
        Class classObject = object.getClass();
        LOGGER.info("Configuring object of class: " + classObject.getSimpleName() + " [" + classObject.getName() + "]");
        for (Method method : classObject.getMethods()) {
            callConfigArgSet(method, object, args, applyDefaults);
            FeederInjector.injectFeeder(method, object, args, applyDefaults, feedersContext, objects);
        }
        return object;
    }

    private static void callConfigArgSet(Method method, Object object, Map<String, Object> args, boolean applyDefaults) throws InvocationTargetException, IllegalAccessException {
        ConfigArgSet annotation = method.getAnnotation(ConfigArgSet.class);
        if (annotation == null) {
            return;
        }

        String property = propertyFromMethod(method.getName());
        Object value = args.remove(property);
        if (value == null) {
            if (requiredFieldsForClassAdded.containsKey(object)
                    && requiredFieldsForClassAdded.get(object).contains(property)) {
                return;
            }

            if (annotation.required()) {
                throw new IllegalArgumentException("Property \"" + property + "\" is required for class " + object.getClass().getSimpleName());
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


    public <T> T createObject(Class<? extends T> classObject, Map<String, Object> args)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        return createObject(classObject, args, null);
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
    public <T> T createObject(Class<? extends T> classObject, Map<String, Object> args, Map<String, Feeder> feederContext)
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
        setObjectProperties(object, args, true, feederContext);
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

    private RunMode getRunMode(Map<String, Object> runModeParams)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
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

    private PublishMode getPublishMode(Map<String, Object> publishModeParams)
            throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
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
     * Getter for the global args
     *
     * @return
     */
    public GlobalArgs getGlobalArgs() {
        return globalArgs;
    }

    /**
     * Getter for kubernetes config args
     * @return
     */
    public DistributedConfig getDistributedConfig() {
        return this.distributedConfig;
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

    public List<Phase> getPhases() {
        return phases;
    }

    public void setPhases(List<Phase> phases) {
        this.phases = phases;
    }

    public TestSuite getTestSuite() {
        return globalSuite;
    }

    public Set<Phase> getPhasesWithoutDuration() {
        return phasesWithoutDuration;
    }

    public Collection<Feeder> getFeeders() {
        return feeders.values();
    }
}
