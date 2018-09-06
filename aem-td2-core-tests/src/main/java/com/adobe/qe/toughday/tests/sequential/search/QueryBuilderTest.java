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
package com.adobe.qe.toughday.tests.sequential.search;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;

@Tag(tags = { "author" })
@Description(desc = "Search that uses the Query Builder Json Rest Api")
public class QueryBuilderTest extends AEMTestBase {
    private static final String DEFAULT_QUERY = "type=cq:Page&group.1_path=/content&orderby=@jcr:score&orderby.sort=desc";
    private String query = DEFAULT_QUERY;

    public QueryBuilderTest() {
    }

    public QueryBuilderTest(String query) {
        this.query = query;
    }

    @Override
    public void test() throws Throwable {
        benchmark().measure(this, "RunQuery", getDefaultClient()).doGet("/bin/querybuilder.json?" + query);
    }

    @Override
    public AbstractTest newInstance() {
        return new QueryBuilderTest(query);
    }

    @ConfigArgSet(required = false, desc = "Query to be executed by the Query Builder servlet", defaultValue = DEFAULT_QUERY)
    public QueryBuilderTest setQuery(String query) {
        this.query = query;
        return this;
    }

    @ConfigArgGet
    public String getQuery() {
        return this.query;
    }
}
