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

public

class MockInheritanceTest extends MockTest {

    private boolean failSubclassBefore;
    private boolean failSubclassTest;
    private boolean failSubclassAfter;

    @Before
    public final void subclassBefore() throws MockThrowable {
        execute(MockTestRunner.SUBCLASS_BEFORE_METHOD);
        if(failSubclassBefore) { throw new MockThrowable(MockTestRunner.SUBCLASS_BEFORE_METHOD); }
    }

    public void run() throws MockThrowable {
        execute(MockTestRunner.SUBCLASS_TEST_METHOD);
        if(failSubclassTest) { throw new MockThrowable(MockTestRunner.SUBCLASS_TEST_METHOD); }
    }

    @After
    public final void subclassAfter() throws MockThrowable {
        execute(MockTestRunner.SUBCLASS_AFTER_METHOD);
        if(failSubclassAfter) { throw new MockThrowable(MockTestRunner.SUBCLASS_AFTER_METHOD); }
    }

    public void doFailSubclassBefore() {
        this.failSubclassBefore = true;
    }

    public void doFailSubclassTest() {
        this.failSubclassTest = true;
    }

    public void doFailSubclassAfter() {
        this.failSubclassAfter = true;
    }
}