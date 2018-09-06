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

import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.internal.core.ReflectionsContainer;
import com.adobe.qe.toughday.metrics.Metric;
import com.adobe.qe.toughday.structural.TestConfigGetAnnotatedMethod;
import com.adobe.qe.toughday.structural.TestConfigSetAnnotatedMethod;
import com.adobe.qe.toughday.structural.TestConstructor;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import java.io.IOException;
import java.lang.reflect.Method;

public class TestInternalSuiteStructural extends TestCase {

    public static TestSuite suite() throws IOException {
        System.setProperty("logFileName", ".");

        TestSuite suite = new TestSuite();

        for (Class TDMetricClass : ReflectionsContainer.getSubTypesOf(Metric.class)) {
            suite.addTest(new TestConstructor("test", TDMetricClass));
            for (Method method : TDMetricClass.getDeclaredMethods()) {
                if (method.getAnnotation(ConfigArgSet.class) != null) {
                    suite.addTest(new TestConfigSetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestConfigSetAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(ConfigArgGet.class) != null) {
                    suite.addTest(new TestConfigGetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestConfigGetAnnotatedMethod("testArguments", method));
                }
            }

        }

        return suite;
    }
}
