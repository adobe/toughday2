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
package com.adobe.qe.toughday.internal.core.config.parsers.cli;

import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Name;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.feeders.FeederGet;
import com.adobe.qe.toughday.api.annotations.feeders.FeederSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.api.feeders.Feeder;
import com.adobe.qe.toughday.api.feeders.InputFeeder;
import com.adobe.qe.toughday.api.feeders.OutputFeeder;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.internal.core.SuiteSetup;
import com.adobe.qe.toughday.internal.core.TestSuite;
import com.adobe.qe.toughday.internal.core.config.*;
import com.adobe.qe.toughday.internal.core.engine.Engine;
import com.adobe.qe.toughday.internal.core.engine.PublishMode;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.distributedtd.cluster.DistributedConfig;
import com.adobe.qe.toughday.metrics.Metric;
import com.google.common.base.Joiner;
import net.jodah.typetools.TypeResolver;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

/**
 * Parser for the command line arguments. It also prints the help message.
 */
public class CliParser implements ConfigurationParser {
    public final static List<Object> parserArgs = new ArrayList<>();
    private static final Logger LOGGER = LogManager.getLogger(CliParser.class);

    private static final String HELP_HEADER_FORMAT_WITH_TAGS = "   %-42s %-75s %-20s   %s";
    private static final String HELP_HEADER_FORMAT_NO_TAGS = "   %-42s %-75s   %s";
    private static final String TEST_CLASS_HELP_HEADER = String.format(HELP_HEADER_FORMAT_WITH_TAGS, "Class", "Fully qualified domain name", "Tags", "Description");
    private static final String PUBLISH_CLASS_HELP_HEADER = String.format(HELP_HEADER_FORMAT_NO_TAGS, "Class", "Fully qualified domain name", "Description");
    private static final String METRIC_CLASS_HELP_HEADER = PUBLISH_CLASS_HELP_HEADER;
    private static final String SUITE_HELP_HEADER = String.format("   %-40s %-40s   %s", "Suite", "Tags", "Description");
    private static Map<Integer, Map<String, ConfigArgSet>> availableGlobalArgs = new HashMap<>();
    private static Map<Integer, Map<String, ConfigArgSet>> availableDistributedConfigArgs = new HashMap<>();
    private static List<ParserArgHelp> parserArgHelps = new ArrayList<>();


    public static final List<String> availableHelpOptions = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add("help");
                add("help_full");
                add("help_tests");
                add("help_publish");
                add("help_metrics");
            }});

    public static final List<String> helpOptionsParameters = Collections.unmodifiableList(
            new ArrayList<String>() {{
                add("tag");
            }});

    private static void collectAvailableConfigurationOptions(Class type, Map<Integer, Map<String, ConfigArgSet>> availableArgs) {
        Arrays.stream(type.getMethods())
                .filter(method -> method.isAnnotationPresent(ConfigArgSet.class))
                .forEach(method -> {
                    ConfigArgSet annotation = method.getAnnotation(ConfigArgSet.class);
                    int order = annotation.order();
                    if (!availableArgs.containsKey(order)) {
                        availableArgs.put(order, new HashMap<>());
                    }

                    availableArgs.get(order)
                            .put(Configuration.propertyFromMethod(method.getName()), annotation);
                });
    }

    static {
        collectAvailableConfigurationOptions(GlobalArgs.class, availableGlobalArgs);
        collectAvailableConfigurationOptions(DistributedConfig.class, availableDistributedConfigArgs);

        for (Class parserClass : ReflectionsContainer.getSubTypesOf(ConfigurationParser.class)) {
            for (Field field : parserClass.getDeclaredFields()) {
                if(field.getType().isAssignableFrom(ParserArgHelp.class)) {
                    try {
                        ParserArgHelp parserArg = (ParserArgHelp) field.get(null);
                        parserArgHelps.add(parserArg);
                        parserArgs.add(parserArg.name());
                    } catch (Exception e) {
                        throw new IllegalStateException("All parser arg help objects must be public and static", e);
                    }
                }
            }
        }
    }

    private static Object getObjectFromString(String string) {
        if (string == null) {
            return string;
        }

        try {
            GlobalArgs.parseDurationToSeconds(string);
            return string;
        } catch (IllegalArgumentException e) {
        }

        if (NumberUtils.isNumber(string)) {
            try {
                return Integer.valueOf(string);
            } catch (NumberFormatException e) {
            }

            try {
                return Long.valueOf(string);
            } catch (NumberFormatException e) {
            }

            try {
                return Float.valueOf(string);
            } catch (NumberFormatException e) {
            }

            try {
                return Double.valueOf(string);
            } catch (NumberFormatException e) {
            }
        }

        if (string.equals("true")) return true;
        if (string.equals("false")) return false;

        return string;

    }

    interface TestFilter {
        Collection<Class<? extends AbstractTest>> filterTests(Collection<Class<? extends AbstractTest>> testClasses);
    }

    interface SuiteFilter {
        Map<String, TestSuite> filterSuites(Map<String, TestSuite> testSuites);
    }

    private static class TagFilter implements TestFilter, SuiteFilter {
        private String tag;

        public TagFilter(String tag) {
            this.tag = tag;
        }

        @Override
        public Collection<Class<? extends AbstractTest>> filterTests(Collection<Class<? extends AbstractTest>> testClasses) {
            Collection<Class<? extends AbstractTest>> filteredTestClasses = new ArrayList<>();
            for (Class<? extends AbstractTest> testClass : testClasses) {
                if(!testClass.isAnnotationPresent(Tag.class))
                    continue;

                if(Arrays.asList(testClass.getAnnotation(Tag.class).tags()).contains(this.tag))
                    filteredTestClasses.add(testClass);
            }
            return filteredTestClasses;
        }

        @Override
        public Map<String, TestSuite> filterSuites(Map<String, TestSuite> testSuites) {
            Map<String, TestSuite> filteredTestSuites = new HashMap<>();
            for (Map.Entry<String, TestSuite> entry : testSuites.entrySet()) {
                if (entry.getValue().getTags().contains(this.tag)) {
                    filteredTestSuites.put(entry.getKey(), entry.getValue());
                }
            }
            return filteredTestSuites;
        }
    }

    private static class AllowAllFilter implements TestFilter, SuiteFilter {
        @Override
        public Map<String, TestSuite> filterSuites(Map<String, TestSuite> testSuites) {
            return testSuites;
        }

        @Override
        public Collection<Class<? extends AbstractTest>> filterTests(Collection<Class<? extends AbstractTest>> testClasses) {
            return testClasses;
        }
    }

    /**
     * Method for parsing and adding a property to the args map.
     * @param propertyAndValue string that contains both the property name and the property value separated by "="
     * @param args map in which the parsed property should be put
     */
    private void parseAndAddProperty(String propertyAndValue, HashMap<String, Object> args) {
        String[] res = parseProperty(propertyAndValue);
        String propertyValueAsString = res[1];
        Object propertyValue = getObjectFromString(propertyValueAsString);
        args.put(res[0], propertyValue);
    }

    private String[] parseProperty(String propertyAndValue) {
        //TODO handle spaces.
        String[] optionValue = propertyAndValue.split("=", 2);
        if (optionValue.length != 1 && optionValue.length != 2) {
            throw new IllegalArgumentException("Properties must have the following form: --property=value or --property. Found: "
                    + propertyAndValue);
        }
        // make the property name lowercase TODO why?
        //String prop = StringUtils.lowerCase(optionValue[0].trim());
        String prop = optionValue[0].trim();
        // default to true if there is no "=" or no value after "="
        String val = (optionValue.length == 2) ? optionValue[1] : "true";

        return new String[] {prop, val};
    }

    private int parseObjectProperties(int startIndex, String[] cmdLineArgs, HashMap<String, Object> args) {
        int j;
        for (j = startIndex; j < cmdLineArgs.length && !cmdLineArgs[j].startsWith("--"); j++) {
            parseAndAddProperty(cmdLineArgs[j], args);
        }

        return j - startIndex;
    }

    private boolean isGlobalArg(String paramName) {
        return availableGlobalArgs.values().stream().anyMatch(map -> map.containsKey(paramName));
    }

    /**
     * Implementation of parser interface
     * @param cmdLineArgs command line arguments
     * @return a populated ConfigParams object
     */
    public ConfigParams parse(String[] cmdLineArgs) {
        HashMap<String, Object> globalArgs = new HashMap<>();
        ConfigParams configParams = new ConfigParams();

        // action parameters
        for (int i = 0; i < cmdLineArgs.length; i++) {
            if (cmdLineArgs[i].startsWith("--")) {
                HashMap<String, Object> args = new HashMap<>();
                int skip = 0;

                String arg = cmdLineArgs[i].substring(2);
                if (arg.equals("phase")) {
                    skip = parseObjectProperties(i + 1, cmdLineArgs, args);
                    configParams.createPhasewWithProperties(args);
                    configParams.setGlobalLevel(false);
                } else if(Actions.isAction(arg)) {
                    Actions action = Actions.fromString(arg);
                    String identifier = cmdLineArgs[i + 1];
                    skip = parseObjectProperties(i + 2, cmdLineArgs, args) + 1;
                    action.apply(configParams, identifier, args);
                } else if (arg.equals("publishmode")) {
                    skip = parseObjectProperties(i + 1, cmdLineArgs, args);
                    configParams.setPublishModeParams(args);
                } else if (arg.equals("runmode")) {
                    skip = parseObjectProperties(i + 1, cmdLineArgs, args);
                    configParams.setRunModeParams(args);
                } else if (arg.equals("distributedconfig")) {
                    skip = parseObjectProperties(i + 1, cmdLineArgs, args);
                    configParams.setDistributedConfigParams(args);
                } else if (arg.equals("help")) {
                    skip = 1;
                    globalArgs.put("host", "N/A"); //TODO remove ugly hack
                } else {
                    String[] res = parseProperty(arg);
                    String key = res[0];
                    Object val = getObjectFromString(res[1]);
                    // if global param or distributed td config param does not exist
                    boolean found = isGlobalArg(key);

                   if (!found && !parserArgs.contains(key) && !availableHelpOptions.contains(key) && !helpOptionsParameters.contains(key)
                            && !key.equals("suite")  && !key.equals("suitesetup")) {
                        throw new IllegalArgumentException("Unrecognized argument --" + key);
                   }

                   globalArgs.put(key, val);
                }

                i += skip;
            } else {
                throw new IllegalArgumentException("Unrecognized argument " + cmdLineArgs[i]);
            }
        }
        configParams.setGlobalParams(globalArgs);
        return configParams;
    }

    /**
     * Get the complete description for SuiteSetup parameter.
     */
    private String getSuiteSetupDescription() {
        String suiteSetupDesc = "Setup step for the test suite, where \"val\" can be: ";
        String suiteSetupList = new String();
        for(Class<? extends SuiteSetup> suiteSetupClass : ReflectionsContainer.getInstance().getSuiteSetupClasses().values()) {
            suiteSetupList += " " + suiteSetupClass.getSimpleName();
        }
        return suiteSetupDesc  + (suiteSetupList.isEmpty() ? "(none)" : suiteSetupList);
    }

    /**
     * Method for printing the help message
     */
    public void printHelp() {
        // print the shorter part of the help
        printShortHelp(true);

        // Follow up with the rest
        printExtraHelp();
    }

    private static  <T> Collection<T> unique(Collection<T> collection, boolean allowNull) {
        HashSet<T> hashSet = new HashSet<>(collection);
        if(!allowNull) {
            hashSet.remove(null);
        }
        return hashSet;
    }

    public static void printTestClasses(TestFilter filter) {
        System.out.println();
        System.out.println("Available test classes:");
        System.out.println(TEST_CLASS_HELP_HEADER);
        for (Class<? extends AbstractTest> testClass : filter.filterTests(unique(ReflectionsContainer.getInstance().getTestClasses().values(), false))){
            printClass(testClass, false, true, false);
        }
    }

    public static void printPublisherClasses() {
        System.out.println();
        System.out.println("Available publisher classes:");
        System.out.println(PUBLISH_CLASS_HELP_HEADER);
        for (Class<? extends Publisher> publisherClass : unique(ReflectionsContainer.getInstance().getPublisherClasses().values(), false)) {
            printClass(publisherClass, false, false, false);
        }
    }

    public static void printMetricClasses() {
        final String basicMetrics = "Name, Timestamp, Passed, Failed, Skipped";
        final String defaultMetrics = basicMetrics + ", Average, Median, StdDev, 90p, 99p, 99.9p, Min, Max";

        System.out.println();
        System.out.println("Available metric classes:");
        System.out.println(METRIC_CLASS_HELP_HEADER);

        for (Class<? extends Metric> metricClass : new HashSet<>(ReflectionsContainer.getInstance().getMetricClasses().values())) {
            printClass(metricClass, false, false, false);
        }

        System.out.println();

        System.out.println("Available metric categories:");
        System.out.println(String.format(" - %-40s - %s", "BASICMetrics", basicMetrics));
        System.out.println(String.format(" - %-40s - %s", "DEFAULTMetrics", defaultMetrics));

    }

    public static void printExtraHelp() {
        printTestClasses(new AllowAllFilter());
        printPublisherClasses();
        printMetricClasses();
    }

    public boolean printHelp(String[] cmdLineArgs) {
        if (cmdLineArgs.length == 0) {
            return false;
        }

        if (cmdLineArgs[0].equals("--help_full")) {
            printHelp();
            return true;
        } else if (cmdLineArgs[0].equals("--help_tests")) {
            printTestClasses(new AllowAllFilter());
            return true;
        } else if (cmdLineArgs[0].equals("--help_publish")) {
            printPublisherClasses();
            return true;
        } else if (cmdLineArgs[0].equals("--help_metrics")) {
            printMetricClasses();
            return true;
        } else if ( (cmdLineArgs[0].equals("--help") && cmdLineArgs.length > 1 )) {
            if (ReflectionsContainer.getInstance().isTestClass(cmdLineArgs[1])) {
                Class<? extends AbstractTest> testClass = ReflectionsContainer.getInstance().getTestClass(cmdLineArgs[1]);
                System.out.println(TEST_CLASS_HELP_HEADER);
                printClass(testClass, true, true, false);
            } else if (ReflectionsContainer.getInstance().isPublisherClass(cmdLineArgs[1])) {
                Class<? extends Publisher> publisherClass = ReflectionsContainer.getInstance().getPublisherClass(cmdLineArgs[1]);
                System.out.println(PUBLISH_CLASS_HELP_HEADER);
                printClass(publisherClass, true, false, false);
            } else if (ReflectionsContainer.getInstance().isMetricClass(cmdLineArgs[1])) {
                Class<? extends Metric> metricClass = ReflectionsContainer.getInstance().getMetricClass(cmdLineArgs[1]);
                printClass(metricClass, true, false, false);
            } else if (ReflectionsContainer.getInstance().isFeederClass(cmdLineArgs[1])) {
                Class<? extends Feeder> feederClass = ReflectionsContainer.getInstance().getFeederClass(cmdLineArgs[1]);
                printClass(feederClass, true, false, false);
            }
            else if (cmdLineArgs[1].startsWith("--suite=")) {
                System.out.println(SUITE_HELP_HEADER);
                printTestSuite(new PredefinedSuites(), cmdLineArgs[1].split("=")[1], true, true);
            } else if (cmdLineArgs[1].startsWith("--tag=")) {
                printTagHelp(cmdLineArgs[1].split("=")[1]);
            } else if (cmdLineArgs[1].equals("--runmode")) {
                if (cmdLineArgs.length < 3) {
                    throw new IllegalArgumentException("Wrong help command format.");
                }
                String[] tmp = cmdLineArgs[2].split("=");
                if(!tmp[0].equals("type"))
                    throw new IllegalArgumentException("Cannot print information about a run mode if no type is specified");
                Class klass = ReflectionsContainer.getInstance().getRunModeClasses().get(tmp[1]);
                if(klass == null) {
                    throw new IllegalArgumentException("No run mode found with type: \"" + tmp[1] + "\"");
                }
                printClass(klass, true, false, true);
                return true;
            } else if (cmdLineArgs[1].equals("--publishmode")) {
                if (cmdLineArgs.length < 3) {
                    throw new IllegalArgumentException("Wrong help command format.");
                }

                String[] tmp = cmdLineArgs[2].split("=");
                if(!tmp[0].equals("type"))
                    throw new IllegalArgumentException("Cannot print information about a publish mode if no type is specified");

                Class klass = ReflectionsContainer.getInstance().getPublishModeClasses().get(tmp[1]);
                if(klass == null) {
                    throw new IllegalArgumentException("No publish mode found with type: \"" + tmp[1] + "\"");
                }
                printClass(klass, true, false, true);
                return true;

            } else {
                System.out.println("Could not find any test or publisher \"" + cmdLineArgs[1] + "\"");
            }
            return true;
        }

        for (String cmdLineArg : cmdLineArgs) {
            if (cmdLineArg.equals("--help")) {
                printShortHelp();
                return true;
            }
        }
        return false;
    }

    public void printShortHelp(boolean printSuitesTests) {
        System.out.println("Usage: java -jar toughday.jar [--help | --help_full | --help_tests | --help_publish] [<global arguments> | <actions>]");
        System.out.println("Running the jar with no parameters or '--help' prints the help.");
        System.out.println("Use '--help_full' to print full help.");
        System.out.println("Use '--help_tests' to print all the test classes.");
        System.out.println("Use '--help_publish' to print all the publisher classes.");
        System.out.println("Use '--help $TestClass/$PublisherClass' to view all configurable properties for that test/publisher");
        System.out.println("Use '--help --suite=$SuiteName' to find information about a test suite");
        System.out.println("Use '--help --tag=$Tag' to find all items that have a the specified tag");
        System.out.println("The above options can also be used with [--add extension.jar]");
        System.out.println("Use '--help --runmode/publishmode type=$Mode' to find information about a run/publish mode");

        System.out.println("\r\nExamples: \r\n");
        System.out.println("\t java -jar toughday.jar --host=localhost --port=4502");
        System.out.println("\t java -jar toughday.jar --runmode type=normal concurrency=20 --host=localhost --port=4502");
        System.out.println("\t java -jar toughday.jar --host=localhost --add extension.jar --add com.adobe.qe.toughday.tests.extensionTest");
        System.out.println("\t java -jar toughday.jar --suite=toughday --add BASICMetrics --add Average decimals=3 --exclude Failed");

        System.out.println("\r\nExamples for running TD distributed: \r\n");
        System.out.println("\t java -jar toughday.jar --host=localhost --distributedconfig driverip=1.1.1.1");
        System.out.println("\t java -jar toughday.jar --host=localhost --distributedconfig driverip=1.1.1.1 heartbeatinterval=10s --suite=toughday");

        System.out.println("\r\nGlobal arguments:");
        availableGlobalArgs.forEach((order, paramGroup) ->
                paramGroup.forEach((key, value) -> System.out.printf("\t--%-32s\t Default: %s - %s\r\n",
                        key + "=val", value.defaultValue(), value.desc())));
        for (ParserArgHelp parserArgHelp : parserArgHelps) {
            System.out.printf("\t--%-32s\t Default: %s - %s\r\n",
                    parserArgHelp.name() + "=val", parserArgHelp.defaultValue(), parserArgHelp.description());
        }
        //System.out.printf("\t%-32s\t %s\r\n", "--suitesetup=val", getSuiteSetupDescription());
        System.out.printf("\t%-32s\t %s\r\n", "--suite=val",
                "Default: toughday - Where \"val\" can be one or a list (separated by commas) of the predefined suites");

        System.out.println("\r\n Distributed run arguments (--distributedconfig):");
        availableDistributedConfigArgs.forEach((order, paramGroup) ->
                paramGroup.forEach((key, value) -> System.out.printf("\t%-32s\t Default: %s - %s\r\n",
                        key + "=val", value.defaultValue(), value.desc())));

        System.out.println("\r\nAvailable run modes (--runmode):");
        for(Map.Entry<String, Class<? extends RunMode>> runMode : ReflectionsContainer.getInstance().getRunModeClasses().entrySet()) {
            Description description = runMode.getValue().getAnnotation(Description.class);
            System.out.printf("\ttype=%-71s %s\r\n", runMode.getKey(), description != null ? description.desc() : "");
        }

        System.out.println("\r\nAvailable publish modes (--publishmode):");
        for(Map.Entry<String, Class<? extends PublishMode>> publishMode : ReflectionsContainer.getInstance().getPublishModeClasses().entrySet()) {
            Description description = publishMode.getValue().getAnnotation(Description.class);
            System.out.printf("\ttype=%-71s %s\r\n", publishMode.getKey(), description != null ? description.desc() : "");
        }

        System.out.println("\r\nAvailable actions:");
        for (Actions action : Actions.values()) {
            System.out.printf("\t--%-71s %s\r\n", action.value() + " " + action.actionParams(), action.actionDescription());
        }

        printTestSuites(new AllowAllFilter(), printSuitesTests);
    }

    private static void printTagHelp(String tag) {
        if(StringUtils.isEmpty(tag)) {
            throw new IllegalArgumentException("Tag was empty");
        }

        TagFilter tagFilter = new TagFilter(tag);
        printTestSuites(tagFilter, false);

        printTestClasses(tagFilter);
    }

    public void printShortHelp() {
        printShortHelp(false);
    }

    private static void printClassProperty(String name, boolean required, String defaultValue, String description) {
        System.out.println(String.format("\t%-32s %-64s %-32s",
                name + "=val" + (required ? "" : " (optional)"),
                defaultValue,
                description));
    }

    private static void printClass(Class klass, boolean printProperties, boolean printTags, boolean lowerCaseClass) {
        String name = lowerCaseClass ? klass.getSimpleName().toLowerCase() : klass.getSimpleName();
        String full_name = lowerCaseClass ? klass.getName().toLowerCase() : klass.getName();
        String desc = "";
        String tags = "";
        if (klass.isAnnotationPresent(Name.class)) {
            Name d = (Name) klass.getAnnotation(Name.class);
            name = name + " [" + d.name() + "]";
        }
        if (klass.isAnnotationPresent(Description.class)) {
            Description d = (Description) klass.getAnnotation(Description.class);
            desc = d.desc();
        }

        if (klass.isAnnotationPresent(Tag.class)) {
            Tag tag = (Tag) klass.getAnnotation(Tag.class);
            tags = Joiner.on(", ").join(tag.tags());
        }

        if(!printTags) {
            System.out.println(String.format(" - %-42s %-75s - %s", name, full_name, desc));
        } else {
            System.out.println(String.format(" - %-42s %-75s %-20s - %s", name, full_name, tags, desc));
        }

        if (printProperties) {
            System.out.println();
            System.out.println(String.format("\t%-32s %-64s %-32s", "Property", "Default", "Description"));
            for (Method method : klass.getMethods()) {
                if (method.getAnnotation(ConfigArgSet.class) != null) {
                    ConfigArgSet annotation = method.getAnnotation(ConfigArgSet.class);
                    printClassProperty(Configuration.propertyFromMethod(method.getName()),
                            annotation.required(),
                            annotation.defaultValue(),
                            annotation.desc());
                }
            }
            printInputFeederSetters(klass);
            printOutputFeederSetters(klass);
            printOutputFeederGetters(klass);
        }
    }

    private static void printOutputFeederGetters(Class klass) {
        System.out.println();
        System.out.println(String.format("\t%-32s %-64s", "Output Feeder Getters", "Type"));
        for (Method method : klass.getMethods()) {
            if(method.getAnnotation(FeederGet.class) != null) {
                FeederGet annotation = method.getAnnotation(FeederGet.class);
                if (OutputFeeder.class.isAssignableFrom(method.getReturnType())) {
                    String type = TypeResolver.resolveGenericType(OutputFeeder.class, method.getGenericReturnType()).getTypeName();
                    type = type.substring(34);
                    System.out.println(String.format("\t%-32s %-64s",
                            Configuration.propertyFromMethod(method.getName()),
                            type));
                }
            }
        }
    }

    private static void printOutputFeederSetters(Class klass) {
        System.out.println();
        System.out.println(String.format("\t%-32s %-64s %-32s", "Output Feeder Setters", "Type", "Description"));
        for (Method method : klass.getMethods()) {
            if(method.getAnnotation(FeederSet.class) != null) {
                FeederSet annotation = method.getAnnotation(FeederSet.class);
                if (OutputFeeder.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    String type = TypeResolver.resolveGenericType(OutputFeeder.class, method.getGenericParameterTypes()[0]).getTypeName();
                    type = type.substring(34);
                    System.out.println(String.format("\t%-32s %-64s %-32s",
                            Configuration.propertyFromMethod(method.getName()) + "=val" + (annotation.required() ? "" : " (optional)"),
                            type,
                            annotation.desc()));
                }
            }
        }
    }

    private static void printInputFeederSetters(Class klass) {
        System.out.println();
        System.out.println(String.format("\t%-32s %-64s %-32s", "Input Feeder Setters", "Type", "Description"));
        for (Method method : klass.getMethods()) {
            if(method.getAnnotation(FeederSet.class) != null) {
                FeederSet annotation = method.getAnnotation(FeederSet.class);
                if (InputFeeder.class.isAssignableFrom(method.getParameterTypes()[0])) {
                    String type = TypeResolver.resolveGenericType(InputFeeder.class, method.getGenericParameterTypes()[0]).getTypeName();
                    type = type.substring(34);
                    System.out.println(String.format("\t%-32s %-64s %-32s",
                            Configuration.propertyFromMethod(method.getName()) + "=val" + (annotation.required() ? "" : " (optional)"),
                            type,
                            annotation.desc()));
                }
            }
        }
    }

    private static void printTestSuites(SuiteFilter filter, boolean withTests) {
        PredefinedSuites predefinedSuites = new PredefinedSuites();
        System.out.println("\r\nPredefined suites");
        System.out.println(SUITE_HELP_HEADER);
        for (String testSuiteName : filter.filterSuites(predefinedSuites).keySet()) {
            printTestSuite(predefinedSuites, testSuiteName, withTests, false);
        }
    }

    private static void printTestSuite(PredefinedSuites predefinedSuites, String testSuiteName, boolean withTests, boolean withTestProperties) {
        TestSuite testSuite = predefinedSuites.get(testSuiteName);
        if(testSuite == null) {
            System.out.println("Cannot find a test predefined test suite named " + testSuiteName);
            return;
        }

        System.out.println(String.format(" - %-40s %-40s - %s", testSuiteName, Joiner.on(", ").join(testSuite.getTags()), testSuite.getDescription()));
        if (withTests) {
            for (AbstractTest test : testSuite.getTests()) {
                System.out.printf("\t%-32s\r\n", test.getFullName() + " [" + test.getClass().getSimpleName() + "]");
                if (withTestProperties) {
                    try {
                        Engine.printObject(System.out, test);
                    } catch (Exception e) {
                        LOGGER.error("Exception while printing the test suite.", e);
                    }
                }
            }
        }
    }
}
