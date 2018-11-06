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
import com.adobe.qe.toughday.api.annotations.Internal;
import com.adobe.qe.toughday.api.annotations.feeders.FeederGet;
import com.adobe.qe.toughday.api.annotations.feeders.FeederSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.Publisher;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.feeders.InputFeeder;
import com.adobe.qe.toughday.api.feeders.OutputFeeder;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.junit.AfterClass;
import org.junit.experimental.categories.Category;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

@Category(TestTDConstraints.class)
public class TestSuiteStructural extends TestCase {

    public static TestSuite suite() {
        Reflections reflections = new Reflections("");
        TestSuite suite = new TestSuite();

        Set<Class<? extends AbstractTest>> testClasses = reflections.getSubTypesOf(AbstractTest.class);
        testClasses.add(AbstractTest.class);
        for(Class TDtestClass : testClasses) {
            if (excludeClass(TDtestClass)) continue;

            suite.addTest(new TestConstructor("test", TDtestClass));
            for (Method method : TDtestClass.getDeclaredMethods()) {
                if (method.getAnnotation(ConfigArgSet.class) != null) {
                    suite.addTest(new TestConfigSetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestConfigSetAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(ConfigArgGet.class) != null) {
                    suite.addTest(new TestConfigGetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestConfigGetAnnotatedMethod("testArguments", method));
                }
                if (TestAnnotatedMethod.hasAnnotation(method)) {
                    suite.addTest(new TestAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(FeederSet.class) != null) {
                    suite.addTest(new TestFeederInjectAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestFeederInjectAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(FeederGet.class) != null) {
                    suite.addTest(new TestFeederGetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestFeederGetAnnotatedMethod("testArguments", method));
                }
            }
        }

        Set<Class<? extends Publisher>> publisherClasses = reflections.getSubTypesOf(Publisher.class);
        publisherClasses.add(Publisher.class);
        for(Class TDpublisherClass : publisherClasses) {
            if (excludeClass(TDpublisherClass)) continue;

            suite.addTest(new TestConstructor("test", TDpublisherClass));
            for (Method method : TDpublisherClass.getDeclaredMethods()) {
                if (method.getAnnotation(ConfigArgSet.class) != null) {
                    suite.addTest(new TestConfigSetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestConfigSetAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(ConfigArgGet.class) != null) {
                    suite.addTest(new TestConfigGetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestConfigGetAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(FeederSet.class) != null) {
                    suite.addTest(new TestFeederInjectAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestFeederInjectAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(FeederGet.class) != null) {
                    suite.addTest(new TestFeederGetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestFeederGetAnnotatedMethod("testArguments", method));
                }
            }
        }

        Set<Class> feeders = new HashSet<>();
        feeders.addAll(reflections.getSubTypesOf(InputFeeder.class));
        feeders.addAll(reflections.getSubTypesOf(OutputFeeder.class));
        for(Class TDFeederClass : feeders) {
            if (excludeClass(TDFeederClass)) continue;

            suite.addTest(new TestFeederClass("testClassConstrains", TDFeederClass));
            suite.addTest(new TestConstructor("test", TDFeederClass));
            for(Method method : TDFeederClass.getDeclaredMethods()) {
                if (method.getAnnotation(ConfigArgSet.class) != null) {
                    suite.addTest(new TestConfigSetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestConfigSetAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(ConfigArgGet.class) != null) {
                    suite.addTest(new TestConfigGetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestConfigGetAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(FeederSet.class) != null) {
                    suite.addTest(new TestFeederInjectAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestFeederInjectAnnotatedMethod("testArguments", method));
                }
                if (method.getAnnotation(FeederGet.class) != null) {
                    suite.addTest(new TestFeederGetAnnotatedMethod("testModifier", method));
                    suite.addTest(new TestFeederGetAnnotatedMethod("testArguments", method));
                }
            }
        }
        return suite;
    }

    @AfterClass
    public static void deleteFile()  {
        LogFileEraser.deteleFiles(((LoggerContext) LogManager.getContext(false)).getConfiguration());
    }

    //TODO: Remove this duplicated code once ReflectionsContainer moves into the API.
    private static boolean excludeClass(Class klass) {
        return Modifier.isAbstract(klass.getModifiers())
                || klass.isAnonymousClass()
                || !Modifier.isPublic(klass.getModifiers())
                || klass.isAnnotationPresent(Internal.class);
    }
}
