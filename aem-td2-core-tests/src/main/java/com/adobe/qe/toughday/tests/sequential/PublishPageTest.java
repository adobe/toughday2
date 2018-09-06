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
import com.adobe.qe.toughday.api.annotations.Internal;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.apache.sling.testing.clients.util.HttpUtils;

import java.util.ArrayList;
import java.util.List;

@Internal
@Tag(tags = { "author" })
@Description(desc= "Publishes a page")
public class PublishPageTest extends AEMTestBase {
    private static final String REFERENCES_SERVLET = "/libs/wcm/core/content/reference.json?_charset_=utf-8";
    private static final Gson GSON = new Gson();
    public static final String DEFAULT_WITH_REFERENCES = "false";

    private boolean withReferences = Boolean.parseBoolean(DEFAULT_WITH_REFERENCES);

    public PublishPageTest() {}

    public PublishPageTest(boolean withReferences) {
        this.withReferences = withReferences;
    }

    @Override
    public void test() throws Throwable {
        String pagePath = getCommunication("resource", null);

        if (pagePath == null)
            throw new Exception("No page was created. Cannot activate it");

        logger().debug("{}: Trying to publish page={}", Thread.currentThread().getName(), pagePath);
        try {
            publish(pagePath, withReferences, HttpStatus.SC_OK);
        } catch (Throwable e) {
            logger().warn("{}: Failed to publish page={}: {}", Thread.currentThread().getName(), pagePath, e.getMessage());
            logger().debug(Thread.currentThread().getName() + ": ERROR: ", e);
            
            throw e;
        }

        logger().debug("{}: Successfully published page={}", Thread.currentThread().getId(), pagePath);
    }

    @Override
    public AbstractTest newInstance() {
        return new PublishPageTest(withReferences);
    }

    private List<String> getPageReferences(String pagePath) throws Throwable {
        FormEntityBuilder form = FormEntityBuilder.create()
                .addParameter("_charset_", "UTF-8")
                .addParameter("path", pagePath);
        SlingHttpResponse content =benchmark().measure(this, "GetPageReferences", getDefaultClient()).doPost(REFERENCES_SERVLET, form.build(), HttpStatus.SC_OK);

        List<String> paths = new ArrayList<>();

        JsonArray references = GSON.fromJson(content.getContent(), JsonObject.class)
                .get("assets")
                .getAsJsonArray();

        for(JsonElement reference : references) {
            paths.add(((JsonObject)reference).get("path").getAsString());
        }

        return paths;

    }

    private SlingHttpResponse publish(String pagePath, boolean withReferences, int... expectedStatus) throws Throwable {
        FormEntityBuilder form = FormEntityBuilder.create()
                .addParameter("_charset_", "UTF-8")
                .addParameter("cmd", "Activate")
                .addParameter("path", pagePath);

        if (withReferences) {
            List<String> references = this.getPageReferences(pagePath);
            for (String reference : references) {
                form.addParameter("path", reference);
            }
        }

        return benchmark().measure(this, "Publish", getDefaultClient()).doPost("/bin/replicate", form.build(), HttpUtils.getExpectedStatus(HttpStatus.SC_OK, expectedStatus));
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_WITH_REFERENCES, desc = "Publish references along with the page")
    public PublishPageTest setWithReferences(String withReferences) {
        this.withReferences = Boolean.parseBoolean(withReferences);
        return this;
    }

    @ConfigArgGet
    public boolean getWithReferences() {
        return withReferences;
    }


}
