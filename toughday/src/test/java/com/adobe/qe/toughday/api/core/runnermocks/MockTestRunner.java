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

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.AbstractTestRunner;
import com.adobe.qe.toughday.api.core.RunMap;
import org.junit.Assert;

public class MockTestRunner extends AbstractTestRunner<MockTest> {
    public static final String BASE_BEFORE_METHOD = "Base Before";
    public static final String SUBCLASS_BEFORE_METHOD = "Subclass Before";

    public static final String BASE_AFTER_METHOD  = "Base After";
    public static final String SUBCLASS_AFTER_METHOD = "Subclass Ater";

    public static final String BASE_TEST_METHOD = "Base Test Method";
    public static final String SUBCLASS_TEST_METHOD   = "Subclass Test Method";

    /**
     * Constructor
     *
     * @param testClass
     */
    public MockTestRunner(Class<? extends AbstractTest> testClass) {
        super(testClass);
    }

    @Override
    protected void run(MockTest testObject, RunMap runMap) throws Throwable {
        Assert.assertNotNull("RunMap should not be null at this point", testObject.benchmark().getRunMap());
        testObject.benchmark().measure(testObject, ()-> {
            testObject.run();
        });
    }
}
