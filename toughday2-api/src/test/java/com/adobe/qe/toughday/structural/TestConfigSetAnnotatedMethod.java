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
public class TestConfigSetAnnotatedMethod extends TestCase {
    private Method TdConfigAnnotatedMethod;
    private Class classType;

    public TestConfigSetAnnotatedMethod(String testName, Method method) {
        super(testName);
        this.TdConfigAnnotatedMethod = method;
    }

    public TestConfigSetAnnotatedMethod(String testName, Class classType) {
        super(testName);
        this.classType = classType;
    }

    /**
     *  Added to supress JUnit warning.
     *  The actual test suite is created in TestSuiteStructural
     *  */
    public static TestSuite suite() { return new TestSuite(); }

    public void testModifier() {
        assertTrue("ToughDay2 config method \"" + TdConfigAnnotatedMethod + "\" must be public",
                (TdConfigAnnotatedMethod.getModifiers() & Modifier.PUBLIC) != 0);
    }

    public void testArguments() {
        assertTrue( "ToughDay2 config method \"" + TdConfigAnnotatedMethod + "\" must have only one parameter",
                TdConfigAnnotatedMethod.getParameterTypes().length == 1);

        assertTrue( "ToughDay2 config method's \"" + TdConfigAnnotatedMethod + "\" parameter must be of type String",
                TdConfigAnnotatedMethod.getParameterTypes()[0].isAssignableFrom(String.class));
    }

    /* This test code was only required to make sure we don't break anything when implementing NPR-15849. However we're
       keeping it just a bit more, in case we needed. If we find that it doesn't help with anything, it can be deleted.
    /**
     *
     * @param testClass
     * @param testInstance
     * @return a map that contains the default value for each property that the test instance has.
     *

    private HashMap<String, String> callGetters(Class testClass, Object testInstance) {
        HashMap<String, String> defaultValues = new HashMap<>();
        for (Method method : testClass.getMethods()) {
            ConfigArgGet getAnnotation = method.getAnnotation(ConfigArgGet.class);
            if (getAnnotation == null) {
                continue;
            }

            String defaultValue = "";
            try {
                defaultValue = String.valueOf(method.invoke(testInstance));
            } catch (Throwable e) {
                fail("Getter fot property " + Configuration.propertyFromMethod(method.getName()) + " could not be invoked.");
            }

            if (!(defaultValue.compareTo("") == 0)) {
                String key = Configuration.propertyFromMethod(method.getName());
                defaultValues.put(key, defaultValue);
            }
        }

        return defaultValues;
    }

    /**
     * Sets the properties of the test to their default value.
     * @param classType
     * @param classInstance
     *

    private void callSetters(Class classType, Object classInstance) {

        if (classInstance instanceof CompositeTest) {
            for (AbstractTest test: ((CompositeTest) classInstance).getChildren()) {
                callSetters(test.getClass(), test);
            }
        }

        for (Method method : classType.getMethods()) {
            ConfigArgSet setAnnotation = method.getAnnotation(ConfigArgSet.class);

            if (setAnnotation == null) {
                continue;
            }

            String defaultValue = setAnnotation.defaultValue();
            if (defaultValue.compareTo("") == 0) {
                continue;
            }

            try {
                method.invoke(classInstance, defaultValue);
            } catch (Throwable e) {
                fail("Setter for property " + Configuration.propertyFromMethod(method.getName()) + " could not be invoked.");
            }
        }

    }

    /**
     *  This test verifies that setting the default and calling the setters with the default value has the same result.
     *

    public void testSettingDefaultValues() {

        if (Modifier.isAbstract(classType.getModifiers())) {
            return;
        }
        if(classType == AdHocTest.class) {
            return;
        }

        Constructor constructor = null;

        try {
            constructor = classType.getConstructor();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }

        Object testInstance = null;

        try {
            if (!(constructor.isAccessible())) {
                constructor.setAccessible(true);
            }
            testInstance = constructor.newInstance();
        } catch (Throwable e) {
            e.printStackTrace();
        }

        HashMap<String, String> defaultValues = callGetters(classType, testInstance);
        callSetters(classType,testInstance);
        HashMap<String, String> settedValues = callGetters(classType, testInstance);

        for (String key : defaultValues.keySet()) {
            Assert.assertEquals("Default values differ for class " + classType.getName(),defaultValues.get(key),settedValues.get(key));
        }
    }
    */

    @AfterClass
    public static void deleteFile()  {
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }
}
