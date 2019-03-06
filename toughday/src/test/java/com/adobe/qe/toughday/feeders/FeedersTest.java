/*
Copyright 2018 Adobe. All rights reserved.
This file is licensed to you under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License. You may obtain a copy
of the License at http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under
the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
OF ANY KIND, either express or implied. See the License for the specific language
governing permissions and limitations under the License.
*/
package com.adobe.qe.toughday.feeders;

import com.adobe.qe.toughday.api.feeders.Feeder;
import com.adobe.qe.toughday.api.feeders.InputFeeder;
import com.adobe.qe.toughday.api.feeders.OutputFeeder;
import com.adobe.qe.toughday.internal.core.config.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FeedersTest {

    private static final String INPUT_FEEDER1_METHOD = "setInputFeeder1";
    private static final String INPUT_FEEDER2_METHOD = "setInputFeeder2";
    private static final String OUTPUT_FEEDER1_METHOD = "setOutputFeeder1";
    private static final String OUTPUT_FEEDER2_METHOD = "setOutputFeeder2";
    private static final String REQUIRED_INPUT_FEEDER = "setRequiredInputFeeder";


    private MockFeederTest test1;
    private MockFeederTest2 test2;
    private MockFeederTest3 test3;

    private Map<String, Feeder> mockFeederContext;
    private Map<String, Object> args;

    @Before
    public void before() {
        mockFeederContext = new HashMap<>();
        test1 = new MockFeederTest();
        test2 = new MockFeederTest2();
        test3 = new MockFeederTest3();
        args = new HashMap<>();
    }

    /**
     * Test the binding of an input feeder.
     * Expected: the configured feeder should be the one bound to the object
     */
    @Test
    public void testInputFeederBind1() throws Exception {
        Feeder feeder = new InputFeeder() {
            @Override
            public Object get(Object... keys) throws Exception {
                return null;
            }

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(INPUT_FEEDER1_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
        Assert.assertTrue("The configured feeder does not match!", feeder == test1.getInputFeeder1());
    }

    /**
     * Test the binding of an input feeder with generics to a method that accepts for a feeder without generics.
     * Expected: the configured feeder should be the one bound to the object
     */
    @Test
    public void testInputFeederBind2() throws Exception {
        Feeder feeder = new InputFeeder<String>() {
            @Override
            public String get(Object... keys) throws Exception {
                return null;
            }

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(INPUT_FEEDER1_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
        Assert.assertTrue("The configured feeder does not match!", feeder == test1.getInputFeeder1());
    }

    /**
     * Test the binding of an input feeder with generics.
     * Expected: the configured feeder should be the one bound to the object
     */
    @Test
    public void testInputFeederWithGenericsBind() throws Exception {
        Feeder feeder = new InputFeeder<Map>() {
            @Override
            public Map get(Object... keys) throws Exception {
                return null;
            }

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(INPUT_FEEDER2_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
        Assert.assertTrue("The configured feeder does not match!", feeder == test1.getInputFeeder2());
    }

    /**
     * Test mismatch in the generics when binding input feeders
     * Expected: an illegal state exception. The value for the generics
     * must match
     */
    @Test(expected = IllegalStateException.class)
    public void testInputFeederWithGenericMismatch() throws Exception {
        Feeder feeder = new InputFeeder<List<List>>() {
            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }

            @Override
            public List<List> get(Object... keys) throws Exception {
                return null;
            }
        };

        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(INPUT_FEEDER2_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
    }

    /**
     * Test mismatch between input and output feeders when binding
     * Expected: an illegal state exception. Input feeders can only be
     * bound to input feeders.
     */
    @Test(expected = IllegalStateException.class)
    public void testInputOutputMismatch() throws Exception {
        Feeder feeder = new OutputFeeder() {

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }

            @Override
            public void push(Object item, Object... keys) throws Exception {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(INPUT_FEEDER1_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
    }

    /**
     * Test the binding of an output feeder.
     * Expected: the configured feeder should be the one bound to the object
     */
    @Test
    public void testOutputFeederBind1() throws Exception {
        Feeder feeder = new OutputFeeder() {
            @Override
            public void push(Object item, Object... keys) throws Exception {

            }

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(OUTPUT_FEEDER1_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
        Assert.assertTrue("The configured feeder does not match!", feeder == test1.getOutputFeeder1());
    }

    /**
     * Test the binding of an output feeder with generics to a method that accepts for a feeder without generics.
     * Expected: the configured feeder should be the one bound to the object
     */
    @Test
    public void testOutputFeederBind2() throws Exception {
        Feeder feeder = new OutputFeeder<String>() {

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }

            @Override
            public void push(String item, Object... keys) throws Exception {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(OUTPUT_FEEDER1_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
        Assert.assertTrue("The configured feeder does not match!", feeder == test1.getOutputFeeder1());
    }

    /**
     * Test the binding of an output feeder with generics.
     * Expected: the configured feeder should be the one bound to the object
     */
    @Test
    public void testOutputFeederWithGenericsBind() throws Exception {
        Feeder feeder = new OutputFeeder<List>() {
            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }

            @Override
            public void push(List item, Object... keys) throws Exception {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(OUTPUT_FEEDER2_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
        Assert.assertTrue("The configured feeder does not match!", feeder == test1.getOutputFeeder2());
    }

    /**
     * Test mismatch in the generics when binding output feeders
     * Expected: an illegal state exception. The value for the generics
     * must match
     */
    @Test(expected = IllegalStateException.class)
    public void testOutputFeederWithGenericMismatch() throws Exception {
        Feeder feeder = new OutputFeeder<Map>() {

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }

            @Override
            public void push(Map item, Object... keys) throws Exception {

            }
        };

        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(OUTPUT_FEEDER2_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
    }

    /**
     * Test mismatch between output and input feeders when binding
     * Expected: an illegal state exception. Output feeders can only be
     * bound to output feeders.
     */
    @Test(expected = IllegalStateException.class)
    public void testOutputInputMismatch() throws Exception {
        Feeder feeder = new InputFeeder() {
            @Override
            public Object get(Object... keys) throws Exception {
                return null;
            }

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(OUTPUT_FEEDER1_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
    }

    /**
     * Test that when the mentioned feeder is not found in the feeder context we get an exception
     */
    @Test(expected = IllegalStateException.class)
    public void testFeederNotFound() throws Exception {
        args.put(Configuration.propertyFromMethod(OUTPUT_FEEDER1_METHOD), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test1, args, true, mockFeederContext);
    }

    /**
     * Test required input feeder configuration present
     * Expected: No exception thrown.
     */
    @Test
    public void testRequiredInputFeeder() throws Exception {
        Feeder feeder = new InputFeeder() {
            @Override
            public Object get(Object... keys) throws Exception {
                return null;
            }

            @Override
            public void init() throws Exception {

            }

            @Override
            public String getName() {
                return null;
            }

            @Override
            public void setName(String name) {

            }
        };
        mockFeederContext.put("feeder", feeder);
        args.put(Configuration.propertyFromMethod(REQUIRED_INPUT_FEEDER), "feeder");
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test2, args, true, mockFeederContext);
    }

    /**
     * Test required input feeder configuration missing
     * Expected: an illegal state exception. configuration for required input feeders should be present.
     */
    @Test(expected = IllegalStateException.class)
    public void testRequiredInputFeederMissing() throws Exception {
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test2, args, true, mockFeederContext);
    }


    @Test
    public void testNoopReplacement1() throws Throwable {
        new Configuration(new String[] {"--host=localhost"}).setObjectProperties(test3, args, true, mockFeederContext);
        Assert.assertTrue("Feeder was not replaced with NoopFeeder", test3.getOptionalNoopReplacementInputFeeder1().equals(NoopFeeder.INSTANCE));
        Assert.assertTrue("Feeder with generics was not replaced with NoopFeeder", test3.getOptionalNoopReplacementInputFeeder2().equals(NoopFeeder.INSTANCE));
        Assert.assertTrue("Feeder with allowNoopReplacement=false should not be replaced", test3.getOptionalNoReplacementInputFeeder() == null);
        test3.test();
    }

}