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

import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.AbstractTestRunner;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class MockTestTwoRequiredFields extends AbstractTest {
    private List<AbstractTest> noChildren = new ArrayList<>();
    private String mock;

    @Override
    public List<AbstractTest> getChildren() {
        return noChildren;
    }

    @Override
    public Class<? extends AbstractTestRunner> getTestRunnerClass() {
        return null;
    }

    @Override
    public AbstractTest newInstance() {
        return null;
    }

    @Override
    @ConfigArgSet(required = true)
    public AbstractTest setName(String name) {
        return super.setName(name);
    }

    @ConfigArgSet(required = true)
    public AbstractTest setMock(String mock) {
        this.mock = mock;
        return this;
    }

    @Override
    public Logger logger() {
        return null;
    }
}
