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

import com.adobe.qe.toughday.api.annotations.Before;
import com.adobe.qe.toughday.api.annotations.After;
import com.adobe.qe.toughday.api.annotations.CloneSetup;
import com.adobe.qe.toughday.api.core.benchmark.TestResult;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;

/**
 * Base class of all runners. For each test only one runner will be instantiated by the Engine and placed into the
 * RunnersContainer. In this way it is ensured that the CloneSetup method is executed only once, even though the test is
 * replicated for each thread. Aside from setup step execution, this class is not thread safe. Any other required
 * synchronization must be implemented by subtypes, but it could affect the throughput of the executed tests. Ideally
 * runners should have no state.
 */
public abstract class AbstractTestRunner<T extends AbstractTest> {
    private volatile boolean cloneSetupExecuted;
    private Method[] setupMethods;
    private Method[] beforeMethods;
    private Method[] afterMethods;

    /**
     * Constructor
     * @param testClass
     */
    public AbstractTestRunner(Class<? extends AbstractTest> testClass) {
        cloneSetupExecuted = true;
        LinkedList<Method> setupMethodList = new LinkedList<>();
        LinkedList<Method> beforeMethodList = new LinkedList<>();
        LinkedList<Method> afterMethodList = new LinkedList<>();

        Class currentClass = testClass;

        while(!currentClass.getName().equals(AbstractTest.class.getName())) {
            for (Method method : currentClass.getDeclaredMethods()) {
                for (Annotation annotation : method.getAnnotations()) {
                    if (annotation.annotationType() == CloneSetup.class) {
                        AssumptionUtils.validateAnnotatedMethod(method, CloneSetup.class);
                        setupMethodList.addFirst(method);
                        method.setAccessible(true);
                        cloneSetupExecuted = false;
                    } else if (annotation.annotationType() == Before.class) {
                        AssumptionUtils.validateAnnotatedMethod(method, Before.class);
                        method.setAccessible(true);
                        beforeMethodList.addFirst(method);
                    } else if (annotation.annotationType() == After.class) {
                        AssumptionUtils.validateAnnotatedMethod(method, After.class);
                        method.setAccessible(true);
                        afterMethodList.addLast(method);
                    }
                }
            }
            currentClass = currentClass.getSuperclass();
        }

        if (setupMethodList.size() > 0)
            this.setupMethods = setupMethodList.toArray(new Method[setupMethodList.size()]);

        if (beforeMethodList.size() >0)
            this.beforeMethods = beforeMethodList.toArray(new Method[beforeMethodList.size()]);

        if (afterMethodList.size() > 0)
            this.afterMethods = afterMethodList.toArray(new Method[afterMethodList.size()]);
    }

    /**
     * Method for executing the setup method using reflection for the specified instance of the test class.
     * The setup is guaranteed to be executed once even if the test is replicated(cloned) for multiple threads.
     * @param testObject
     */
    private boolean executeCloneSetup(AbstractTest testObject, RunMap runMap) throws Throwable{
        /* The synchronized block, the second if and the assignation of the variable cloneSetupExecuted only after
        the call of the method, are to ensure that the setup is executed exactly once, even if this runner is used
        by multiple threads. The first if is to ensure that no bottleneck occurs due to synchronization. */
        if (!cloneSetupExecuted) {
            synchronized (this) {
                if (!cloneSetupExecuted) {
                    try {
                        executeMethods(testObject, setupMethods, CloneSetup.class);
                    } catch (Throwable e) {
                        testObject.logger().error("Failure in @CloneSetup: ", e);
                        if(testObject.getParent() != null) {
                            throw e;
                        }
                        return false;
                    } finally {
                        cloneSetupExecuted = true;
                    }
                }
            }
        }
        return true;
    }

    /**
     * Method for executing the before method using reflection for the specified instance of the test class.
     * It will run before each test run.
     * @param testObject
     */
    private void executeBefore(AbstractTest testObject, RunMap runMap) throws Throwable {
        if (beforeMethods != null) {
            try {
                executeMethods(testObject, beforeMethods, Before.class);
            }
            catch (Throwable e) {
                TestResult testResult = new TestResult(testObject).markAsSkipped(new SkippedTestException(e));
                runMap.record(testResult);
                testObject.logger().debug("Failure in @Before: ", e);
                throw e;
            }
        }
    }

    /**
     * Method for executing the after method using reflection for the specified instance of the test class.
     * The after method is guaranteed to run after every test run, even if exceptions occur.
     * @param testObject
     */
    private void executeAfter(AbstractTest testObject) {
        if (afterMethods != null) {
            try{
                executeMethods(testObject, afterMethods, After.class);
            } catch (Throwable e) {
                testObject.logger().debug("Failure in @After: ", e);
            }
        }
    }

    /**
     * Runs a test and benchmarks its execution.
     * @param testObject instance of the test to run
     * @param runMap the run map in which the benchmark will be recorded.
     * @throws Throwable any throwable occurred in the test and was propagated upstream by the implementation runner
     */
    public final void runTest(AbstractTest testObject, RunMap runMap) throws Throwable {
        testObject.benchmark().setRunMap(runMap);
        if(!executeCloneSetup(testObject, runMap)) { return; }

        Throwable throwable = null;
        try {
            executeBefore(testObject, runMap);
            run((T) testObject, runMap);
        } catch (Throwable e) {
            throwable = e;
        } finally {
            executeAfter(testObject);
        }

        if(throwable != null) {
            if(testObject.getParent() != null) {
                throw throwable;
            }
            testObject.logger().debug("Test failed with error:", throwable);
        }
    }

    /**
     * Method for delegating the responsibility of correctly running and benchmarking the test to subclasses.
     * @param testObject instance of the test to run
     * @param runMap the run map in which the benchmark will be recorded.
     * @throws Throwable any throwable occurred in the test and was propagated upstream by the implementation runner
     */
    protected abstract void run(T testObject, RunMap runMap) throws Throwable;

    /**
     * Run a annotated method using reflections.
     * @param testObject instance of the test for which the method will be executed
     * @param methods that will be invoked by using reflections
     * @param annotation what annotation caused this method to be ran
     */
    private void executeMethods(AbstractTest testObject, Method[] methods, Class<? extends Annotation> annotation)
            throws Throwable {
        for (Method method : methods) {
            try {
                method.invoke(testObject);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

}
