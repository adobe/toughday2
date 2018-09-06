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
package com.adobe.qe.toughday.api.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;

/**
 * Container for runners.
 */
public class RunnersContainer {
    private static final Logger LOG = LoggerFactory.getLogger(RunnersContainer.class);
    private static RunnersContainer instance = new RunnersContainer();
    public static RunnersContainer getInstance() { return instance; }

    private HashMap<AbstractTest, AbstractTestRunner> testRunners;
    private RunnersContainer() {
        this.testRunners = new HashMap<>();
    }

    /**
     * Adds a runner to the container for the specified test.
     * @param test the test for which a runner will be added to the container
     * @throws IllegalAccessException caused by reflection
     * @throws InvocationTargetException caused by reflection
     * @throws InstantiationException caused by reflection
     * @throws NoSuchMethodException caused by reflection
     */
    public void addRunner(AbstractTest test)
            throws IllegalAccessException, InvocationTargetException, InstantiationException, NoSuchMethodException {
        if (!testRunners.containsKey(test)) {
            Class<? extends AbstractTestRunner> runnerClass = test.getTestRunnerClass();
            try {
                Constructor<? extends AbstractTestRunner> constructor = runnerClass.getConstructor(Class.class);
                testRunners.put(test, constructor.newInstance(test.getClass()));
            } catch (NoSuchMethodException e) {
                LOG.error("Cannot run test " + test.getFullName() + " because the runner doesn't have the appropriate constructor");
                throw new NoSuchMethodException("Test runners must have a constructor with only one parameter, the test Class");
            }
        }
    }

    /**
     * Method for getting the runner for the specified test, only if the test was previously added.
     * @param test
     * @return the runner for the specified test if it was previously added, null otherwise.
     */
    public AbstractTestRunner getRunner(AbstractTest test) {
        return testRunners.get(test);
    }
}
