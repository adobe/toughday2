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
package com.adobe.qe.toughday.tests.sequential.demo;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.*;
import com.adobe.qe.toughday.api.annotations.Internal;
import org.apache.sling.testing.clients.ClientException;

@Internal // <-- remove this to see the test in cli/help and to be able to run it
@Description(desc = "Demo derived description")
public class DerivedDemoTest extends DemoTest {

    public DerivedDemoTest() {
    }

    public DerivedDemoTest(String property) {
        super(property);
    }

    @Setup
    private void setupMethod() {
        logger().info(getFullName() + " Derived Setup");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Before
    private void beforeMethod() {
        logger().info(getFullName() + " Derived Before");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @After
    private void afterMethod() {
        logger().info(getFullName() + " Derived After");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void test() throws ClientException {
        logger().info(getFullName() + " Running derived test");
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    @Override
    public AbstractTest newInstance() {
        return new DerivedDemoTest(getProperty());
    }
}
