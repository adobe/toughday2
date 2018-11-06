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
package com.adobe.qe.toughday.internal.core;

import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.api.annotations.Internal;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.feeders.Feeder;
import com.adobe.qe.toughday.metrics.Metric;
import com.adobe.qe.toughday.internal.core.engine.RunMode;
import com.adobe.qe.toughday.internal.core.engine.PublishMode;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Wrapper for the reflections library. Uses singleton.
 */
public class ReflectionsContainer {
    private static final Pattern toughdayContentPackagePattern = Pattern.compile("toughday_sample-.*.zip");
    private static Reflections reflections = new Reflections("");
    private static ReflectionsContainer instance = new ReflectionsContainer();

    /**
     * Getter for the container.
     */
    public static ReflectionsContainer getInstance() {
        return instance;
    }

    private HashMap<String, Class<? extends AbstractTest>> testClasses;
    private HashMap<String, Class<? extends Publisher>> publisherClasses;
    private HashMap<String, Class<? extends SuiteSetup>> suiteSetupClasses;
    private HashMap<String, Class<? extends PublishMode>> publishModeClasses;
    private HashMap<String, Class<? extends RunMode>> runModeClasses;
    private HashMap<String, Class<? extends Metric>> metricClasses;
    private HashMap<String, Class<? extends Feeder>> feederClasses;

    private Set<String> classRegister;

    private String toughdayContentPackage;

    private static boolean excludeClass(Class klass) {
        return Modifier.isAbstract(klass.getModifiers())
                || klass.isAnonymousClass()
                || !Modifier.isPublic(klass.getModifiers())
                || klass.isAnnotationPresent(Internal.class);
    }

    /**
     * Constructor.
     */

    private ReflectionsContainer() {

        updateContainerContent();

        Reflections reflections = new Reflections("", new ResourcesScanner());
        Iterator<String> iterator = reflections.getResources(toughdayContentPackagePattern).iterator();
        if (iterator.hasNext()) {
            toughdayContentPackage = iterator.next();
        }
    }

    private void updateContainerContent() {

        testClasses = new HashMap<>();
        publisherClasses = new HashMap<>();
        suiteSetupClasses = new HashMap<>();
        publishModeClasses = new HashMap<>();
        runModeClasses = new HashMap<>();
        metricClasses = new HashMap<>();
        feederClasses = new HashMap<>();
        classRegister = new HashSet<>();

        for(Class<? extends AbstractTest> testClass : reflections.getSubTypesOf(AbstractTest.class)) {
            if(excludeClass(testClass))
                continue;

            addToClassRegister(testClass.getName());
            testClasses.put(testClass.getName(), testClass);

            if(testClasses.containsKey(testClass.getSimpleName())) {
                testClasses.put(testClass.getSimpleName(), null);
            }
            else {
                testClasses.put(testClass.getSimpleName(), testClass);
            }
        }

        for (Class<? extends Publisher> publisherClass : reflections.getSubTypesOf(Publisher.class)) {
            if (excludeClass(publisherClass))
                continue;

            addToClassRegister(publisherClass.getName());
            publisherClasses.put(publisherClass.getName(), publisherClass);

            if (publisherClasses.containsKey(publisherClass.getSimpleName())) {
                publisherClasses.put(publisherClass.getSimpleName(), null);
            }
            else {
                publisherClasses.put(publisherClass.getSimpleName(), publisherClass);
            }
        }

        for (Class<? extends SuiteSetup> suiteSetupClass : reflections.getSubTypesOf(SuiteSetup.class)) {
            if (Modifier.isAbstract(suiteSetupClass.getModifiers()))
                continue;
            if (suiteSetupClasses.containsKey(suiteSetupClass.getSimpleName()))
                throw new IllegalStateException("A suite class with this name already exists here: "
                        + suiteSetupClasses.get(suiteSetupClass.getSimpleName()).getName());
            addToClassRegister(suiteSetupClass.getSimpleName());
            suiteSetupClasses.put(suiteSetupClass.getSimpleName(), suiteSetupClass);
        }

        for (Class<? extends PublishMode> publishModeClass : reflections.getSubTypesOf(PublishMode.class)) {
            if(excludeClass(publishModeClass)) continue;
            String identifier = publishModeClass.getSimpleName().toLowerCase();
            if(publishModeClasses.containsKey(identifier)) {
                throw new IllegalStateException("A publish mode class with this name already exists here: "
                        + publishModeClasses.get(identifier).getName());
            }
            addToClassRegister(identifier);
            publishModeClasses.put(identifier, publishModeClass);
        }

        for(Class<? extends RunMode> runModeClass : reflections.getSubTypesOf(RunMode.class)) {
            if(excludeClass(runModeClass)) continue;
            String identifier = runModeClass.getSimpleName().toLowerCase();
            if(runModeClasses.containsKey(identifier)) {
                throw new IllegalStateException("A run mode class with this name already exists here: " +
                        runModeClasses.get(identifier).getName());
            }
            addToClassRegister(identifier);
            runModeClasses.put(identifier, runModeClass);
        }

        for (Class<? extends Metric> metricClass : reflections.getSubTypesOf(Metric.class)) {
            if (excludeClass(metricClass)) { continue; }

            addToClassRegister(metricClass.getName());
            metricClasses.put(metricClass.getName(), metricClass);

            if (metricClasses.containsKey(metricClass.getSimpleName())) {
                metricClasses.put(metricClass.getSimpleName(), null);
            }
            else {
                metricClasses.put(metricClass.getSimpleName(), metricClass);
            }
        }

        for (Class<? extends Feeder> feederClass : reflections.getSubTypesOf(Feeder.class)) {
            if (excludeClass(feederClass)) { continue; }
            addToClassRegister(feederClass.getName());
            feederClasses.put(feederClass.getName(), feederClass);

            if (feederClasses.containsKey(feederClass.getSimpleName())) {
                feederClasses.put(feederClass.getSimpleName(), null);
            }
            else {
                feederClasses.put(feederClass.getSimpleName(), feederClass);
            }
        }
    }

    // Two classes with different types should not be allowed to have the same name.
    private void addToClassRegister(String classIdentifier) {
        if (!classRegister.contains(classIdentifier)) {
            classRegister.add(classIdentifier);
        } else {
            throw new IllegalArgumentException("A class with this name already exists. Please provide a different name for your class.");
        }
    }

    /**
     * Verifies if the given name is a test class
     * @param testClass the name of a test class
     * @return true if the given name is a test class. false otherwise.
     */
    public boolean isTestClass(String testClass) {
        return testClasses.containsKey(testClass);
    }

    /**
     * Get the test class corresponding to the name
     * @param testClass the name of a test class
     * @return the test class corresponding to the name
     * @throws IllegalArgumentException if the name is either ambiguous (more than one test class has the same name and a FQDN wasn't used) or there is no
     *  test class with the specified name
     */
    public Class<? extends AbstractTest> getTestClass(String testClass) {
        if(isTestClass(testClass)) {
            if(testClasses.get(testClass) == null) {
                throw new IllegalArgumentException("There is more than one test named: " + testClass + ". Please use the fully qualified domain name.");
            }
            return testClasses.get(testClass);
        }
        throw new IllegalArgumentException("Unknown test: " + testClass);
    }

    /**
     * Getter for the map of test classes.
     */
    public HashMap<String, Class<? extends AbstractTest>> getTestClasses() {
        return testClasses;
    }

    /**
     * Verifies if the given name is a publisher class
     * @param publisherClass the name of a publisher class
     * @return true if the given name is a publisher class. false otherwise.
     */
    public boolean isPublisherClass(String publisherClass) { return publisherClasses.containsKey(publisherClass); }

    /**
     * Get the publisher class corresponding to the name
     * @param publisherClass the name of a publisher class
     * @return the publisher class corresponding to the name
     * @throws IllegalArgumentException if the name is either ambiguous (more than one publisher class has the same name and a FQDN wasn't used) or there is no
     *  publisher class with the specified name
     */
    public Class<? extends Publisher> getPublisherClass(String publisherClass) {
        if(isPublisherClass(publisherClass)) {
            if (publisherClasses.get(publisherClass) == null) {
                throw new IllegalArgumentException("There is more than one publisher named: " + publisherClass + ". Please use the fully qualified domain name.");
            }
            return publisherClasses.get(publisherClass);
        }
        throw new IllegalArgumentException("Unknown publisher: " + publisherClass);
    }

    /**
     * Getter for the map of publisher classes.
     */
    public HashMap<String, Class<? extends Publisher>> getPublisherClasses() {
        return publisherClasses;
    }

    /**
     * Getter for the map of SuiteSetup classes.
     */
    public HashMap<String, Class<? extends SuiteSetup>> getSuiteSetupClasses() {
        return suiteSetupClasses;
    }

    /**
     * Getter for the map of PublishMode classes.
     */
    public Map<String, Class<? extends PublishMode>> getPublishModeClasses() { return publishModeClasses; }

    public String getToughdayContentPackage() {
        return toughdayContentPackage;
    }

    /**
     * Getter for the map of RunMode classes.
     */

    public HashMap<String,Class<? extends RunMode>> getRunModeClasses() {
        return runModeClasses;
    }

    /**
     * Verifies if the given name is a metric class
     * @param metricClass the name of a metric class
     * @return true if the given name is a metric class. false otherwise.
     */
    public boolean isMetricClass(String metricClass) {
        return metricClasses.containsKey(metricClass);
    }

    /**
     * Get the metric class corresponding to the name
     * @param metricClass the name of a metric class
     * @return the metric class corresponding to the name
     * @throws IllegalArgumentException if the name is either ambiguous (more than one metric class has the same name and a FQDN wasn't used) or there is no
     *  metric class with the specified name
     */
    public Class<? extends Metric> getMetricClass(String metricClass) {
        if(isMetricClass(metricClass)) {
            if (metricClasses.get(metricClass) == null) {
                throw new IllegalArgumentException("There is more than one metric named: " + metricClass + ". Please use the fully qualified domain name.");
            }
            return metricClasses.get(metricClass);
        }
        throw new IllegalArgumentException("Unknown metric: " + metricClass);
    }

    /**
     * Getter for the map of Metric classes.
     */
    public HashMap<String, Class<? extends Metric>> getMetricClasses() { return metricClasses; }

    /**
     *  Checks if the Reflection Container contains a class with the given name.
     */

    public boolean containsClass(String className) {
        return classRegister.contains(className);
    }

    /**
     * This method makes the Reflections instance aware about the new classes dynamically loaded from the jar files.
     * @param reflections
     */

    public void merge(Reflections reflections) {
        this.reflections = reflections;
        updateContainerContent();
    }

    public static <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
       return reflections.getSubTypesOf(type);
    }
    /**
     * Verifies if the given name is a feeder class
     * @param feederClass the name of a feeder class
     * @return true if the given name is a feeder class. false otherwise.
     */
    public boolean isFeederClass(String feederClass) { return feederClasses.containsKey(feederClass); }

    /**
    * Get the feeder class corresponding to the name
    * @param feederClass the name of a feeder class
    * @return the feeder class corresponding to the name
    * @throws IllegalArgumentException if the name is either ambiguous (more than one feeder class has the same name and a FQDN wasn't used) or there is no
    *  feeder class with the specified name
    */
    public Class<? extends Feeder> getFeederClass(String feederClass) {
        if (isFeederClass(feederClass)) {
            if(feederClasses.get(feederClass) == null) {
                throw new IllegalArgumentException("There is more than one feeder named: " + feederClass + ". Please use the fully qualified domain name.");
            }
            return feederClasses.get(feederClass);
        }
        throw new IllegalArgumentException("Unkown feeder: " + feederClass);
    }
}
