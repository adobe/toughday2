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
package com.adobe.qe.toughday.api.core.runnermocks;

import com.adobe.qe.toughday.api.annotations.After;
import com.adobe.qe.toughday.api.annotations.Before;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.AbstractTestRunner;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MockTest extends AbstractTest {

    private List<String> executedMethods = new ArrayList<>();
    private List<AbstractTest> children = new ArrayList<>();
    private static Logger logger;

    boolean failBefore;
    boolean failTest;
    boolean failAfter;

    @Override
    public List<AbstractTest> getChildren() {
        return this.children;
    }

    @Override
    public Class<? extends AbstractTestRunner> getTestRunnerClass() {
        return MockTestRunner.class;
    }

    @Override
    public AbstractTest newInstance() {
        return new MockTest();
    }

    @Before
    public final void before() throws MockThrowable {
        execute(MockTestRunner.BASE_BEFORE_METHOD);
        if(failBefore) { throw new MockThrowable(MockTestRunner.BASE_BEFORE_METHOD); }
    }

    public void run() throws MockThrowable {
        execute(MockTestRunner.BASE_TEST_METHOD);
        if(failTest) { throw new MockThrowable(MockTestRunner.BASE_TEST_METHOD); }
    }

    @After
    public final void after() throws MockThrowable {
        execute(MockTestRunner.BASE_AFTER_METHOD);
        if(failAfter) { throw new MockThrowable(MockTestRunner.BASE_AFTER_METHOD); }
    }

    protected void execute(String methodName) {
        this.executedMethods.add(methodName);
    }

    public List<String> getExecutedMethods() {
        return executedMethods;
    }

    public void doFailBefore() {
        this.failBefore = true;
    }

    public void doFailTest() {
        this.failTest = true;
    }

    public void doFailAfter() {
        this.failAfter = true;
    }

    public void addChild(AbstractTest test) {
        children.add(test);
        test.setParent(this);
    }
}