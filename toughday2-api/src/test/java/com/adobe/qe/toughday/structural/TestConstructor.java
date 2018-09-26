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
package com.adobe.qe.toughday.structural;

import com.adobe.qe.toughday.LogFileEraser;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.internal.core.benckmark.AdHocTest;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.AfterClass;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Constructor;

@Category(TestTDConstraints.class)
public class TestConstructor extends TestCase {
    private Class TdClass;

    public TestConstructor(String testName, Class TDTestClass) {
        super(testName);
        this.TdClass = TDTestClass;
    }

    /**
     *  Added to supress JUnit warning.
     *  The actual test suite is created in TestSuiteStructural
     *  */
    public static TestSuite suite() { return new TestSuite(); }

    public void test() throws NoSuchMethodException {
        //The AdHocTest is an exception from the rule
        if(TdClass == AdHocTest.class) {
            return;
        }

        try {
            Constructor constructor = TdClass.getConstructor(null);
        } catch (NoSuchMethodException e) {
            fail("ToughDay2 class \"" + TdClass.getName() + "\" doesn't have a public constructor with no arguments, or it is not public");
        }
    }

    @AfterClass
    public static void deleteFile()  {
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
