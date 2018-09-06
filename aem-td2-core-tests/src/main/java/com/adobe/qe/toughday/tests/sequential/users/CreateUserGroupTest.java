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
package com.adobe.qe.toughday.tests.sequential.users;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.Before;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Setup;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.commons.html.impl.HtmlParserImpl;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;
import org.w3c.dom.Document;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Tag(tags = { "author" })
@Description(desc = "Create groups of users. Similar to group editor console (/libs/granite/security/content/groupEditor.html)")
public class CreateUserGroupTest extends AEMTestBase {

    private String id;
    private String groupName = DEFAULT_GROUP_NAME;
    private String description = DEFAULT_GROUP_DESCRIPTION;
    private AtomicInteger increment;

    private String extraGroup; //A group created before running the tests the will be communicated.

    public static final String DEFAULT_GROUP_NAME = "ToughDay";
    public static final String DEFAULT_GROUP_DESCRIPTION = "A group for ToughDay users";

    public CreateUserGroupTest() {
        increment = new AtomicInteger(0);
    }

    public CreateUserGroupTest(AtomicInteger increment, String groupName, String description, String extraGroup) {
        this.groupName = groupName;
        this.description = description;
        this.increment = increment;
    }

    @Setup
    private void setup() {
        try {
            extraGroup = createGroup(getDefaultClient(), id, groupName, description, false);
        } catch (Throwable e) {
            //TODO
            extraGroup = null;
        }
    }

    @Before
    private void before() {
        id = RandomStringUtils.randomAlphanumeric(20);
    }

    @Override
    public void test() throws Throwable {
        String groupName = this.groupName;
        if(increment != null) {
            groupName += increment.getAndIncrement();
        }

        try {
            logger().debug("{}: Trying to create user group={}, with id={}", Thread.currentThread().getName(), groupName, id);

            String groupPath = createGroup(getDefaultClient(), id, groupName, description, true);
            communicate("groups", extraGroup != null ? Arrays.asList(extraGroup, groupPath) : Arrays.asList(groupPath));
        } catch (Throwable e) {
            logger().warn("{}: Failed to create user group={}{}", Thread.currentThread().getName(), groupName, id);
            logger().debug(Thread.currentThread().getName() + "ERROR: ", e);

            throw e;
        }

        logger().debug("{}: Successfully created user group={}, with id={}", Thread.currentThread().getName(), groupName, id);
    }

    /**
     * Create a group
     * @return path to the created group
     */
    private String createGroup(SlingClient client, String id, String groupName, String description, boolean addToRunmap) throws Throwable {
        HtmlParserImpl htmlParser = htmlParser = new HtmlParserImpl();
        FormEntityBuilder entityBuilder = FormEntityBuilder.create()
                .addParameter("authorizableId", id)
                .addParameter("./profile/givenName", groupName)
                .addParameter("./profile/aboutMe", description)
                .addParameter("createGroup", "1")
                .addParameter("_charset_", "utf-8");

        client = addToRunmap ? benchmark().measure(this, "CreateGroup", client) : client;
        SlingHttpResponse response = client.doPost("/libs/granite/security/post/authorizables.html", entityBuilder.build(), HttpStatus.SC_CREATED);
        Document responseHtml = htmlParser.parse(null, IOUtils.toInputStream(response.getContent()), "utf-8");
        return responseHtml.getElementsByTagName("title").item(0).getTextContent().split(" ")[2];
    }

    @Override
    public AbstractTest newInstance() {
        return new CreateUserGroupTest(increment, groupName, description, extraGroup);
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_GROUP_NAME)
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    @ConfigArgGet
    public String getGroupName() {
        return groupName;
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_GROUP_DESCRIPTION)
    public void setDescription(String description) {
        this.description = description;
    }

    @ConfigArgGet
    public String getDescription() {
        return this.description;
    }

    @ConfigArgSet(required = false, desc = "Increment the group name", defaultValue = "true")
    public void setIncrement(String value) {
        if(!Boolean.valueOf(value))
            increment = null;
    }

    @ConfigArgGet
    public boolean getIncrement() {
        return this.increment != null;
    }
}
