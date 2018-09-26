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
import com.adobe.qe.toughday.api.annotations.After;
import com.adobe.qe.toughday.api.annotations.Before;
import com.adobe.qe.toughday.api.annotations.CloneSetup;
import com.adobe.qe.toughday.api.annotations.Setup;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.AfterClass;
import org.junit.experimental.categories.Category;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Created by tuicu on 13/06/16.
 */

@Category(TestTDConstraints.class)
public class TestAnnotatedMethod extends TestCase {
    private Method TdAnnotatedMethod;

    public TestAnnotatedMethod(String testName, Method method) {
        super(testName);
        this.TdAnnotatedMethod = method;
    }

    public static boolean hasAnnotation(Method method) {
        return method.getAnnotation(Before.class) != null
                || method.getAnnotation(After.class) != null
                || method.getAnnotation(CloneSetup.class) != null
                || method.getAnnotation(Setup.class) != null;
    }

    /**
     *  Added to supress JUnit warning.
     *  The actual test suite is created in TestSuiteStructural
     *  */
    public static TestSuite suite() {
        return new TestSuite();
    }

    public void testModifier() {
        assertTrue("ToughDay2 annotated method \"" + TdAnnotatedMethod + "\" must be final or private",
                Modifier.isFinal(TdAnnotatedMethod.getModifiers()) || Modifier.isPrivate(TdAnnotatedMethod.getModifiers()));
    }

    public void testArguments() {
        assertTrue("ToughDay2 annotated method \"" + TdAnnotatedMethod + "\" is not allowed to have parameters",
                TdAnnotatedMethod.getParameterTypes().length == 0);
    }

    @AfterClass
    public static void deleteFile()  {
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
