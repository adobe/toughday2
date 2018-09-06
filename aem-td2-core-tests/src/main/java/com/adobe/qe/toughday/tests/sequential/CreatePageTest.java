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

import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.tests.composite.AuthoringTest;
import com.adobe.qe.toughday.tests.samplecontent.SampleContent;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.util.FormEntityBuilder;

import java.util.concurrent.atomic.AtomicInteger;

@Tag(tags = { "author" })
@Description(desc = "This test creates pages under the same parent path." +
        " Due to OAK limitations, performance will decrease over time." +
        " If you are not looking for this specific scenario, please consider using CreatePageTreeTest.")
public class CreatePageTest extends AEMTestBase {

    private String rootParentPath = DEFAULT_PARENT_PATH;
    private String template = DEFAULT_TEMPLATE;
    private String title = AuthoringTest.DEFAULT_PAGE_TITLE;

    public static ThreadLocal<String> lastCreated = new ThreadLocal<String>();

    public CreatePageTest() {
    }

    protected CreatePageTest(String parentPath, String template, String title) {
        this.rootParentPath = parentPath;
        this.template = template;
        this.title = title;
    }

    public static final AtomicInteger nextNumber = new AtomicInteger(0);
    public static final String CMD_CREATE_PAGE = "createPage";
    public static final String DEFAULT_PARENT_PATH = SampleContent.TOUGHDAY_SITE;
    public static final String DEFAULT_TEMPLATE = SampleContent.TOUGHDAY_TEMPLATE;

    @Override
    public void test() throws Throwable {

        String nextTitle = title + nextNumber.getAndIncrement();
        lastCreated.set(nextTitle);

        FormEntityBuilder feb = FormEntityBuilder.create()
                .addParameter("cmd", CMD_CREATE_PAGE)
                .addParameter("parentPath", rootParentPath)
                .addParameter("title", nextTitle)
                .addParameter("template", template);

        try {
            logger().debug("{}: Trying to create page={}{}, with template={}", Thread.currentThread().getId(), rootParentPath, nextTitle, template);

            benchmark().measure(this, "Create Page", getDefaultClient()).doPost("/bin/wcmcommand", feb.build(), HttpStatus.SC_OK);
        } catch (Throwable e) {
            logger().warn("{}: Failed to create page={}{}, with template={}", Thread.currentThread().getId(), rootParentPath, nextTitle, template);
            logger().debug(Thread.currentThread().getName() + "ERROR: ", e);

            throw e;
        }

        logger().debug("{}: Successfully created page={}{}, with template={}", Thread.currentThread().getId(), rootParentPath, nextTitle, template);
    }

    @Override
    public AbstractTest newInstance() {
        return new CreatePageTest(rootParentPath, template, title);
    }


    @ConfigArgSet(required = false, defaultValue = AuthoringTest.DEFAULT_PAGE_TITLE,
            desc = "The title of the page. Internally, this is incremented")
    public AbstractTest setTitle(String title) {
        this.title = title.toLowerCase();
        return this;
    }

    @ConfigArgGet
    public String getTitle() {
        return this.title;
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_PARENT_PATH,
            desc = "The path prefix for all pages.")
    public AbstractTest setParentPath(String parentPath) {
        this.rootParentPath = StringUtils.stripEnd(parentPath, "/");
        return this;
    }

    @ConfigArgGet
    public String getParentPath() {
        return this.rootParentPath;
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_TEMPLATE, desc = "The template of the page.")
    public AbstractTest setTemplate(String template) {
        this.template = template;
        return this;
    }

    @ConfigArgGet
    public String getTemplate() {
        return this.template;
    }

}
