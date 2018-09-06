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

import com.adobe.qe.toughday.api.annotations.*;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.FluentLogging;
import com.adobe.qe.toughday.tests.composite.AuthoringTreeTest;
import com.adobe.qe.toughday.tests.samplecontent.SampleContent;
import com.adobe.qe.toughday.tests.utils.TreePhaser;
import com.adobe.qe.toughday.tests.utils.WcmUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.apache.sling.testing.clients.util.FormEntityBuilder;

import java.util.UUID;

@Tag(tags = { "author" })
@SuppressWarnings("Duplicates")
@Description(desc=
                "This test creates pages hierarchically. Each child on each level has \"base\" children. " +
                "Each author thread fills in a level in the pages tree, up to base^level")
public class CreatePageTreeTest extends AEMTestBase {

    private final TreePhaser phaser;

    public String rootParentPath = SampleContent.TOUGHDAY_SITE;
    private String template = SampleContent.TOUGHDAY_TEMPLATE;
    private String title = AuthoringTreeTest.DEFAULT_PAGE_TITLE;

    private Integer nextChild;

    private String parentPath;
    private String nodeName;
    private boolean failed = false;

    public CreatePageTreeTest() {
        phaser = new TreePhaser();
    }

    protected CreatePageTreeTest(TreePhaser phaser, String parentPath, String template, String title) {
        this.phaser = phaser;
        this.rootParentPath = parentPath;
        this.template = template;
        this.title = title;
    }

    @Setup
    private void setupContent() {
        String isolatedFolder = "toughday" + UUID.randomUUID();
        try {
            getDefaultClient().createFolder(isolatedFolder, isolatedFolder, rootParentPath);
            rootParentPath = rootParentPath + "/" + isolatedFolder;
        } catch (Throwable e) {
            logger().debug("Could not create isolated folder for running " + getFullName());
        }
    }

    @Before
    private void setup() {
        phaser.register();

        // this gets the next node on the level and potentially waits for other threads to reset the level
        // save those values for later use
        this.nextChild = phaser.getNextNode();
        this.parentPath = TreePhaser.computeParentPath(nextChild, phaser.getLevel(), phaser.getBase(), title, rootParentPath);
        this.nodeName = TreePhaser.computeNodeName(nextChild, phaser.getBase(), title);
        this.failed = false;
    }

    @Override
    public void test() throws Throwable {
        FluentLogging.create(logger())
                .before(Level.DEBUG, "{}: Trying to create page={}{}, with template={}", Thread.currentThread().getName(), parentPath, nodeName, template)
                .onThrowable(Level.WARN, "{}: Failed to create page={}{}, with template={}", Thread.currentThread().getName(), parentPath, nodeName, template)
                .onThrowable(Level.DEBUG, Thread.currentThread().getName() + ": ERROR: ", true)
                .run(() -> createPage());
    }

    @After
    private void after() {
        // make sure the page was created
        for (int i=0; i<5; i++) {
            try {
                // If operation was marked as failed and the path really does not exist,
                // try and create it, as it is needed as the parent path for the children on the next level
                if (!failed || benchmark().measure(this, "Check Page Created", getDefaultClient()).exists(this.parentPath + nodeName)) {
                    logger().debug("{}: Successfully created page={}{}, with template={}", Thread.currentThread().getName(), parentPath, nodeName, template);
                    break;
                } else {
                    logger().debug("{}: Retrying to create page={}{}, with template={}", Thread.currentThread().getName(), parentPath, nodeName, template);
                    createPage();
                }
            } catch (Throwable e) {
                logger().warn("{}: Failed to create after retry page={}{}, with template={}", Thread.currentThread().getName(), parentPath, nodeName, template, e.getMessage());
                logger().debug(Thread.currentThread().getName() + "ERROR: ", e);
            }
        }

        // de-register
        phaser.arriveAndDeregister();
    }

    private void createPage() throws Throwable {
        FormEntityBuilder feb = FormEntityBuilder.create()
                .addParameter("cmd", WcmUtils.CMD_CREATE_PAGE)
                .addParameter("parentPath", parentPath)
                .addParameter("label", nodeName)
                .addParameter("title", title)
                .addParameter("template", template);
        HttpEntity entity = feb.build();
        benchmark().measure(this, "CreatePage", getDefaultClient()).doPost("/bin/wcmcommand", entity, HttpStatus.SC_OK);
        communicate("resource", parentPath + nodeName);
    }

    @Override
    public AbstractTest newInstance() {
        return new CreatePageTreeTest(phaser, rootParentPath, template, title);
    }

    @ConfigArgSet(required = false, defaultValue = AuthoringTreeTest.DEFAULT_PAGE_TITLE,
            desc = "The title of the page. Internally, this is incremented")
    public AbstractTest setTitle(String title) {
        this.title = title.toLowerCase();
        return this;
    }

    @ConfigArgGet
    public String getTitle() {
        return this.title;
    }

    @ConfigArgSet(required = false, defaultValue = SampleContent.TOUGHDAY_SITE,
            desc = "The path prefix for all pages.")
    public AbstractTest setParentPath(String parentPath) {
        this.rootParentPath = StringUtils.stripEnd(parentPath, "/");
        return this;
    }

    @ConfigArgGet
    public String getParentPath() {
        return this.rootParentPath;
    }

    @ConfigArgSet(required = false, defaultValue = SampleContent.TOUGHDAY_TEMPLATE, desc = "Template for all the pages created.")
    public AbstractTest setTemplate(String template) {
        this.template = template;
        return this;
    }

    @ConfigArgGet
    public String getTemplate() {
        return this.template;
    }

    @ConfigArgSet(required = false, desc = "How many direct child pages will a page have.",defaultValue = TreePhaser.DEFAULT_BASE)
    public AbstractTest setBase(String base) {
        this.phaser.setBase(Integer.parseInt(base));
        return this;
    }

    @ConfigArgGet
    public int getBase() {
        return this.phaser.getBase();
    }
}
