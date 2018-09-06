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
package com.adobe.qe.toughday.tests.composite.demo;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.CompositeTest;
import com.adobe.qe.toughday.api.annotations.*;
import com.adobe.qe.toughday.api.annotations.Internal;
import com.adobe.qe.toughday.tests.sequential.demo.DemoTest;

@Internal // <-- remove this to see the test in cli/help and to be able to run it
@Description(desc = "Demo composite description")
public class CompositeDemo extends CompositeTest {
    DemoTest test1;
    DemoTest test2;
    DemoTest test3;

    public CompositeDemo() {
        this(true);
    }

    @Override
    public AbstractTest newInstance() {
        return new CompositeDemo(false);
    }

    public CompositeDemo(boolean createChildren) {
        if(createChildren) {
            this.test1 = new DemoTest();
            this.test2 = new DemoTest();
            this.test3 = new DemoTest();

            test1.setName("Child1");
            test2.setName("Child2");
            test3.setName("Child3");

            this.addChild(test1);
            this.addChild(test2);
            this.addChild(test3);
        }
    }


    @Setup
    private void setupMethod() {
        logger().info(getFullName() + " Setup");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Before
    private void beforeMethod() {
        logger().info(getFullName() + " Before");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @After
    private void afterMethod() {
        logger().info(getFullName() + " After");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
