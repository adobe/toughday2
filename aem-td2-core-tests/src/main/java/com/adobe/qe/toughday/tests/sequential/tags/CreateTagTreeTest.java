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
package com.adobe.qe.toughday.tests.sequential.tags;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.*;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;
import com.adobe.qe.toughday.tests.utils.TreePhaser;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.util.FormEntityBuilder;

import java.util.Arrays;

@Tag(tags = { "author" })
@Name(name="create_tag_tree")
@Description(desc=
        "This test creates tags hierarchically. Each child on each level has \"base\" children. " +
                "Each author thread fills in a level in the tag tree, up to base^level")
public class CreateTagTreeTest extends AEMTestBase {

    public static final String CREATE_TAG_CMD = "createTag";
    public static final String TAG_COMMAND_URL = "/bin/tagcommand";

    public static final String DEFAULT_NAMESPACE = "ToughDay";
    public static final String DEFAULT_TAG_TITLE = "ToughDay";

    public static final String EXTRA_TAG_TITLE = "ToughDayTag";

    private static final String NAMEPSPACE_DESCRIPTION = "Isolated namespace for ToughDay testing";
    private static final String TAG_DESCRIPTION = "Toughday Tag";

    private TreePhaser phaser;
    private boolean failed;
    private String parentPath;
    private String title = DEFAULT_TAG_TITLE; //TODO use this
    private String nodeName;
    private String namespace = DEFAULT_NAMESPACE;
    private int nextChild;
    private String extra_tag;

    public CreateTagTreeTest() {
        phaser = new TreePhaser();
    }

    public CreateTagTreeTest(TreePhaser phaser, String namespace, String title, String extra_tag) {
        this.phaser = phaser;
        this.namespace = namespace;
        this.title = title;
        this.extra_tag = extra_tag;
    }

    @Setup
    private void setup() {
        try {
            String isolatedNameSpace = namespace + "_" + RandomStringUtils.randomAlphanumeric(5);
            createNamespace(isolatedNameSpace, isolatedNameSpace, NAMEPSPACE_DESCRIPTION);
            namespace = isolatedNameSpace;
        } catch (Throwable e) {
            logger().warn("Failed to create namespace {}", namespace);
            namespace = "default";
        }

        //Create one initial tag to be used for adding it to all pages, so we have a very large index.
        try {
            createTag(EXTRA_TAG_TITLE, EXTRA_TAG_TITLE, "ToughDay extra tag", namespace + ":", false);
            extra_tag = namespace + ":" + EXTRA_TAG_TITLE;
        } catch (Throwable e) {
            //TODO
        }
    }

    @Before
    private void before() {
        phaser.register();

        // this gets the next node on the level and potentially waits for other threads to reset the level
        // save those values for later use
        this.nextChild = phaser.getNextNode();
        this.parentPath = namespace + ":" + StringUtils.stripStart(TreePhaser.computeParentPath(nextChild, phaser.getLevel(), phaser.getBase(), title), "/");
        this.nodeName = TreePhaser.computeNodeName(nextChild, phaser.getBase(), title);
        this.failed = false;
    }

    @Override
    public void test() throws Throwable {
        try {
            logger().debug("{}: Trying to create tag={}{}", Thread.currentThread().getName(), parentPath, nodeName);
            createTag();
        } catch (Throwable e) {
            this.failed = true;
            // log and throw. It's normally an anti-pattern, but we don't log exceptions anywhere on the upper level,
            // we just count them.
            logger().warn("{}: Failed to create tag={}{}", Thread.currentThread().getName(), parentPath, nodeName);
            logger().debug(Thread.currentThread().getName() + "ERROR: ", e);
            throw e;
        }
    }

    @After
    private void after() {
        // make sure the page was created
        for (int i=0; i<5; i++) {
            try {
                // If operation was marked as failed and the path really does not exist,
                // try and create it, as it is needed as the parent path for the children on the next level
                if (!failed || getDefaultClient().exists(this.parentPath + nodeName)) {
                    logger().debug("{}: Successfully created tag={}{}", Thread.currentThread().getName(), parentPath, nodeName);
                    break;
                } else {
                    logger().debug("{}: Retrying to create tag={}{}", Thread.currentThread().getName(), parentPath, nodeName);
                    createTag();
                }
            } catch (Throwable e) {
                logger().warn("{}: Failed to create after retry tag={}{}", Thread.currentThread().getName(), parentPath, nodeName);
                logger().debug(Thread.currentThread().getName() + "ERROR: ", e);
            }
        }

        // de-register
        phaser.arriveAndDeregister();
    }

    private void createNamespace(String title, String tag, String description) throws Throwable {
        FormEntityBuilder feb = FormEntityBuilder.create()
                .addParameter("jcr:title", title)
                .addParameter("tag", tag)
                .addParameter("jcr:description", description)
                .addParameter("cmd", CREATE_TAG_CMD);
        getDefaultClient().doPost(TAG_COMMAND_URL, feb.build(), HttpStatus.SC_OK);
    }

    private void createTag() throws Throwable {
        createTag(nodeName, nodeName, TAG_DESCRIPTION, parentPath, false);
    }

    private void createTag(String title, String tag, String description, String parentTagID, boolean addToRunMap) throws Throwable {
        SlingClient client = addToRunMap ? benchmark().measure(this, "CreateTag", getDefaultClient()) : getDefaultClient();
        FormEntityBuilder feb = FormEntityBuilder.create()
                .addParameter("jcr:title", title)
                .addParameter("tag", tag)
                .addParameter("jcr:description", description)
                .addParameter("parentTagID", parentTagID)
                .addParameter("cmd", CREATE_TAG_CMD);
        client.doPost(TAG_COMMAND_URL, feb.build(), HttpStatus.SC_OK);
        communicate("tags", Arrays.asList(extra_tag, parentTagID + title));
    }

    @Override
    public AbstractTest newInstance() {
        return new CreateTagTreeTest(phaser, namespace, title, extra_tag);
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_NAMESPACE,
            desc = "The title of the namespace where the tags will be created. A random string will be added internally to make it unique")
    public AbstractTest setNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    @ConfigArgGet
    public String getNamespace() {
        return this.namespace;
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_NAMESPACE,
            desc = "The title of the tags. Internally, this will be incremented")
    public AbstractTest setTitle(String title) {
        this.title = title;
        return this;
    }

    @ConfigArgGet
    public String getTitle() {
        return this.title;
    }

    @ConfigArgSet(required = false, desc = "How many direct child tags will a tag have.", defaultValue = TreePhaser.DEFAULT_BASE)
    public AbstractTest setBase(String base) {
        this.phaser.setBase(Integer.parseInt(base));
        return this;
    }

    @ConfigArgGet
    public int getBase() {
        return this.phaser.getBase();
    }
}
