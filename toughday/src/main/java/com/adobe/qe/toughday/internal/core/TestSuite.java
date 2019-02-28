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

import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.AbstractTest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

/**
 * Test suite class.
 */
public class TestSuite implements Cloneable {
    private List<SuiteSetup> setupStep;
    private String description = "";
    private List<String> tags = new ArrayList<>();
    private ArrayList<AbstractTest> orderedTests;
    private HashMap<String, AbstractTest> nameMap = new HashMap<>();
    private int totalWeight;
    private long minTimeout;

    /**
     * Constructor.
     */
    public TestSuite() {
        setupStep = new ArrayList<>();
        orderedTests = new ArrayList<>();
        totalWeight = 0;
    }

    /**
     *  Creates a copy of the current test suite. All tests contained by the test suite are cloned.
     * @throws CloneNotSupportedException  if the object to be cloned does not implement the Cloneable interface.
     */
    public TestSuite clone() throws CloneNotSupportedException {
        TestSuite newInstance = (TestSuite) super.clone();

        /* clone all the tests in the TestSuite */
        newInstance.orderedTests = new ArrayList<>();
        newInstance.nameMap = new HashMap<>();

        for (AbstractTest test : this.getTests()) {
            newInstance.add(test.clone());
        }

        return newInstance;
    }

    /**
     * Method for adding a test.
     * @param test
     * @return this object. (builder pattern)
     */
    public TestSuite add(AbstractTest test) {
        return add(test, orderedTests.size());
    }

    public TestSuite add(AbstractTest test, int index) {
        if (nameMap.containsKey(test.getName())) {
            throw new IllegalArgumentException("Suite already contains a test named: \"" + test.getName() + "\". " +
                    "Please provide a different name using the \"name\" property.");
        }

        nameMap.put(test.getName(), test);
        orderedTests.add(index, test);
        totalWeight += test.getWeight();

        return this;
    }

    public TestSuite addAll(TestSuite testSuite) {
        this.setupStep.addAll(testSuite.setupStep);
        this.orderedTests.addAll(testSuite.orderedTests);

        for (AbstractTest test : testSuite.orderedTests) {
            totalWeight += test.getWeight();
            nameMap.put(test.getName(), test);
        }
        return this;
    }

    /**
     * Setter for the setup step, as seen from the configuration.
     * @param setupStepClassNames a list with suite setup names separated by commas
     * @return this object. (builder pattern)
     * @throws ClassNotFoundException caused by reflection
     * @throws NoSuchMethodException caused by reflection
     * @throws IllegalAccessException caused by reflection
     * @throws InvocationTargetException caused by reflection
     * @throws InstantiationException caused by reflection
     */
    @ConfigArgSet(required = false)
    public TestSuite setSuiteSetup(String setupStepClassNames)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        String[] suiteSetups = setupStepClassNames.split(",");

        for (String setupStepClassName : suiteSetups) {
            Class<? extends SuiteSetup> setupStepClass = null;
            setupStepClass = ReflectionsContainer.getInstance().getSuiteSetupClasses().get(setupStepClassName);

            if (setupStepClass == null) {
                throw new ClassNotFoundException("Could not find class " + setupStepClassName + " for suite setup step");
            }
            withSetupStep(setupStepClass);
        }
        return this;
    }

    /**
     * Overload of withSetupStep.
     * @param setupStepClass
     * @return this object. (builder pattern)
     * @throws ClassNotFoundException caused by reflection
     * @throws NoSuchMethodException caused by reflection
     * @throws IllegalAccessException caused by reflection
     * @throws InvocationTargetException caused by reflection
     * @throws InstantiationException caused by reflection
     */
    public TestSuite withSetupStep(Class<? extends SuiteSetup> setupStepClass)
            throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException,
            InvocationTargetException, InstantiationException {
        Constructor constructor = setupStepClass.getConstructor(null);
        withSetupStep((SuiteSetup) constructor.newInstance());
        return this;
    }

    /**
     * Overload of withSetupStep.
     * @param setupStep
     * @return
     */
    public TestSuite withSetupStep(SuiteSetup setupStep) {
        this.setupStep.add(setupStep);
        return this;
    }

    /**
     * Method for setting the description
     * @param description
     * @return
     */
    public TestSuite setDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * Getter for the description
     * @return
     */
    public String getDescription() {
        return description;
    }

    /**
     * Method for adding a tag
     * @param tag
     * @return
     */
    public TestSuite addTag(String tag) {
        this.tags.add(tag);
        return this;
    }

    /**
     * Method for adding tags
     * @param tags
     * @return
     */
    public TestSuite addTags(Collection<String> tags) {
        this.tags.addAll(tags);
        return this;
    }

    /**
     * Method for adding tags
     * @param tags
     * @return
     */
    public TestSuite addTags(String[] tags) {
        this.tags.addAll(Arrays.asList(tags));
        return this;
    }

    /**
     * Getter for the tags
     * @return
     */
    public List<String> getTags() { return tags; }

    /**
     * Getter for the setup step.
     * @return a SetupStep object if configured, null otherwise.
     */
    public List<SuiteSetup> getSetupStep() {
        return setupStep;
    }

    /**
     * Getter for the total weight.
     */
    public int getTotalWeight() {
        return totalWeight;
    }

    /**
     * Getter for the test set.
     */
    public Collection<AbstractTest> getTests() {
        return orderedTests;
    }

    /**
     * Getter for a name given a test
     * @param testName
     * @return
     */
    public AbstractTest getTest(String testName) {
        return nameMap.get(testName);
    }

    /**
     * Method for removing a test given it's name
     * @param testName
     */
    public int remove(String testName) {
        AbstractTest test = nameMap.get(testName);
        return remove(test);
    }

    /**
     * Method for removing a test
     * @param test
     */
    public int remove(AbstractTest test) {
        AbstractTest previous = nameMap.remove(test.getName());
        totalWeight -= (previous == null? 0 : previous.getWeight());

        for (int i = 0; i < orderedTests.size(); ++i) {
            if (orderedTests.get(i).equals(test)) {
                orderedTests.remove(test);
                return i;
            }
        }

        throw new IllegalStateException("Test not found in the suite.");
    }

    /**
     * Method for finding if the suite contains a test with the given name
     * @param testName
     * @return
     */
    public boolean contains(String testName) {
        return nameMap.containsKey(testName);
    }

    public long getMinTimeout() {
        return minTimeout;
    }

    public void setMinTimeout(long minTimeout) {
        this.minTimeout = minTimeout;
    }
}

