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

import com.adobe.qe.toughday.api.feeders.InputFeeder;
import com.adobe.qe.toughday.api.feeders.OutputFeeder;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.jodah.typetools.TypeResolver;
import org.junit.Assert;
import org.junit.experimental.categories.Category;

/**
 * Test the constraints of feeder classes
 */
@Category(TestTDConstraints.class)
public class TestFeederClass extends TestCase {
    Class classType;

    public TestFeederClass(String testName, Class classType) {
        super(testName);
        this.classType = classType;
    }


    /**
     *  Added to supress JUnit warning.
     *  The actual test suite is created in TestSuiteStructural
     *  */
    public static TestSuite suite() { return new TestSuite(); }

    public void testClassConstrains() {
        if(InputFeeder.class.isAssignableFrom(classType)) {
            Assert.assertTrue("Feeder class: \"" + classType + "\" may not use generics",
                    TypeResolver.resolveRawArgument(InputFeeder.class, classType) != TypeResolver.Unknown.class);
        }
        if(OutputFeeder.class.isAssignableFrom(classType)) {
            Assert.assertTrue("Feeder class: \"" + classType + "\" may not use generics",
                    TypeResolver.resolveRawArgument(OutputFeeder.class, classType) != TypeResolver.Unknown.class);
        }

        Assert.assertTrue("Feeder class: \"" + classType + "\" may not use generics", classType.getTypeParameters().length == 0);
    }

}
