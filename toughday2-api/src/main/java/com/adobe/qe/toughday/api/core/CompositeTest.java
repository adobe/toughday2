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

import com.adobe.qe.toughday.api.core.config.GlobalArgs;
import com.adobe.qe.toughday.api.runners.CompositeTestRunner;

import java.util.*;

/**
 * Convenience class for creating composite steps. Extend this class to create composite tests. See CompositeDemoTest
 * for more details.
 * Each step will be executed sequentially, in order, individually benchmarked and the whole test will be benchmarked
 * as well. You can have any number of child steps and each step can be either a simple, or a composite test. Also, the
 * steps can have different runners.
 */
public abstract class CompositeTest extends AbstractTest {
    private List<AbstractTest> children;

    private HashMap<String, Object> communications = new HashMap<>();

    @Override
    protected  <T> T getCommunication(String key, T defaultValue) {
        Object value = communications.get(key);
        return value != null ? (T) value : (getParent() != null ? getParent().getCommunication(key, defaultValue) : defaultValue);
    }

    @Override
    protected void communicate(String key, Object message) {
        communications.put(key, message);
        // TODO: add super.communicate() ?
    }

    /**
     * Constructor.
     */
    public CompositeTest() {
        children = new ArrayList<>();
    }

    /**
     * Getter for the children list.
     * @return a list of the children.
     */
    @Override
    public List<AbstractTest> getChildren() {
        return children;
    }

    /**
     * Method for adding a child test.
     * @param child
     * @return this test. (builder pattern)
     */
    protected AbstractTest addChild(AbstractTest child) {
        child.setParent(this);
        child.setGlobalArgs(this.getGlobalArgs());
        children.add(child);
        return this;
    }

    /**
     * Getter for a child if the id is known.
     * @param id
     * @return the child with the id given, or null if it cannot be found.
     */
    public AbstractTest getChild(TestId id) {
        for (AbstractTest child : children) {
            if (child.getId().equals(id))
                return child;
        }
        return null;
    }

    /**
     * Getter for the runner of the test.
     * @return CompositeTestRunner.class
     */
    @Override
    public Class<? extends AbstractTestRunner> getTestRunnerClass() {
        return CompositeTestRunner.class;
    }

    /**
     * Method for setting the children list. This will replace the existing list of children, without keeping the
     * previous ones.
     * @param children
     */
    private void setChildren(List<AbstractTest> children) {
        this.children = children;
    }

    /**
     * Method for cloning a composite test. The newInstance method is left abstract for the subclasses to implement.
     * @return a deep clone of this test, including its children.
     */
    @Override
    public AbstractTest clone() {
        CompositeTest newComposite = (CompositeTest) super.clone();

        List<AbstractTest> clonedChildren = new ArrayList<>();
        for (AbstractTest test : children) {
            AbstractTest newTest = test.clone();
            newTest.setParent(newComposite);
            clonedChildren.add(newTest);
        }

        newComposite.setChildren(clonedChildren);
        return newComposite;
    }


    /**
     * Setter for global args
     * @param globalArgs
     */
    @Override
    public CompositeTest setGlobalArgs(GlobalArgs globalArgs) {
        super.setGlobalArgs(globalArgs);
        for (AbstractTest test: children) {
            test.setGlobalArgs(globalArgs);
        }
        return this;
    }

}
