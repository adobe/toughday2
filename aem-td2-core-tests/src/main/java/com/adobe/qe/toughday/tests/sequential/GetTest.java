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
package com.adobe.qe.toughday.tests.sequential;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import org.apache.http.HttpStatus;

@Tag(tags = { "author", "publish" })
@Description(desc = "GET a specific page.")
public class GetTest extends AEMTestBase {

    public static final String DEFAULT_PATH = "/crx/de";
    private String path;


    public GetTest() {
        this.path = DEFAULT_PATH;
    }

    /**
     * Setter for the name
     */
    @ConfigArgSet(required = false, defaultValue = DEFAULT_PATH, desc = "The path at which to perform a GET request")
    public AbstractTest setPath(String path) {
        this.path = path;
        return this;
    }

    @ConfigArgGet
    public String getPath() {
        return this.path;
    }

    @Override
    public void test() throws Throwable {
        try {
            logger().debug("{}: Trying to GET path={}", Thread.currentThread().getName(), path);
            benchmark().measure(this, "GET page", getDefaultClient()).doGet(path, HttpStatus.SC_OK);
        } catch (Throwable e) {
            logger().warn("{}: Failed to GET path={}", Thread.currentThread().getName(), path);
            logger().debug(Thread.currentThread().getName() + ": ERROR :", e);
            throw e;
        }
    }

    @Override
    public AbstractTest newInstance() {
        GetTest test = new GetTest();
        test.setPath(this.path);
        return test;
    }
}
