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
import org.apache.http.HttpStatus;

@Tag(tags = { "author", "publish" })
@Description(desc = "GET the home page.")
public class GetHomepageTest extends AEMTestBase {

    @Override
    public void test() throws Throwable {
        benchmark().measure(this, "GET Homepage", getDefaultClient()).doGet("/", HttpStatus.SC_OK);
    }

    @Override
    public AbstractTest newInstance() {
        return new GetHomepageTest();
    }
}
