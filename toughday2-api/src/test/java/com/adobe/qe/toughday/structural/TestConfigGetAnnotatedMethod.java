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
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.AfterClass;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@Category(TestTDConstraints.class)
public class TestConfigGetAnnotatedMethod extends TestCase {
    private Method TdConfigAnnotatedMethod;

    public TestConfigGetAnnotatedMethod(String testName, Method method) {
        super(testName);
        this.TdConfigAnnotatedMethod = method;
    }

    /**
     *  Added to supress JUnit warning.
     *  The actual test suite is created in TestSuiteStructural
     *  */
    public static TestSuite suite() { return new TestSuite(); }

    public void testModifier() {
        assertTrue("ToughDay2 config get method \"" + TdConfigAnnotatedMethod + "\" must be public",
                (TdConfigAnnotatedMethod.getModifiers() & Modifier.PUBLIC) != 0);
    }

    public void testArguments() {
        assertTrue( "ToughDay2 config get method \"" + TdConfigAnnotatedMethod + "\" must have no parameters",
                TdConfigAnnotatedMethod.getParameterTypes().length == 0);
    }

    @AfterClass
    public static void deleteFile()  {
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
