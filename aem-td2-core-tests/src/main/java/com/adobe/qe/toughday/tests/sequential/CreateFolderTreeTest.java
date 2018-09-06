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
import com.adobe.qe.toughday.tests.samplecontent.SampleContent;
import com.adobe.qe.toughday.tests.utils.TreePhaser;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.logging.log4j.Level;
import org.apache.sling.testing.clients.util.FormEntityBuilder;

@Tag(tags = { "author" })
@Name(name="create_folder_tree")
@Description(desc="This test creates folders hierarchically. Each child on each level has \"base\" children. " +
                "Each author thread fills in a level in the folder tree, up to base^level")
public class CreateFolderTreeTest extends AEMTestBase {

    public static final String FOLDER_RESOURCE_TYPE = "sling:Folder";
    public static final String DEFAULT_PARENT_PATH = SampleContent.TOUGHDAY_DAM_FOLDER;
    public static final String DEFAULT_TITLE = "toughday";

    // needed for synchronizing
    private TreePhaser phaser;

    private String rootParentPath = DEFAULT_PARENT_PATH;
    private String title = DEFAULT_TITLE;
    private String resourceType = FOLDER_RESOURCE_TYPE;

    private int nextChild;

    private String parentPath;
    private String nodeName;
    private boolean failed = false;

    public CreateFolderTreeTest() {
        phaser = new TreePhaser();
        AbstractTest.addExtraThread(phaser.mon);
    }

    protected CreateFolderTreeTest(TreePhaser phaser, String parentPath, String title, String resourceType) {
        this.phaser = phaser;
        this.rootParentPath = parentPath;
        this.title = title;
        this.resourceType = resourceType;
    }

    @Setup
    private void setup() {
        try {
            String isolatedRoot = "tree_" + RandomStringUtils.randomAlphanumeric(5);
            createFolder(isolatedRoot, rootParentPath + "/");
            rootParentPath = rootParentPath + "/" + isolatedRoot;
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Before
    private void before() {
        phaser.register();

        // this gets the next node on the level and potentially waits for other threads to reset the level
        // save those values for later use
        this.nextChild = phaser.getNextNode();
        this.parentPath = rootParentPath + TreePhaser.computeParentPath(nextChild, phaser.getLevel(), phaser.getBase(), title);
        this.nodeName = TreePhaser.computeNodeName(nextChild, phaser.getBase(), title);
        this.failed = false;
    }

    @Override
    public void test() throws Throwable {
        FluentLogging.create(logger())
                .before(Level.DEBUG, "{}: Trying to create folder={}{}", Thread.currentThread().getId(), parentPath, nodeName)
                .onThrowable(Level.WARN, "{}: Failed to create folder={}{}", Thread.currentThread().getId(), parentPath, nodeName)
                .onThrowable(Level.DEBUG, Thread.currentThread().getId() + "ERROR: ", true)
                .run(() -> createFolder());
    }

    @After
    private void after() {
        // make sure the page was created
        for (int i=0; i<5; i++) {
            try {
                // If operation was marked as failed and the path really does not exist,
                // try and create it, as it is needed as the parent path for the children on the next level
                if (!failed || benchmark().measure(this, "Check Folder Created", getDefaultClient()).exists(this.parentPath + nodeName)) {
                    logger().debug("{}: Successfully created folder={}{}", Thread.currentThread().getId(), parentPath, nodeName);
                    break;
                } else {
                    logger().debug("{}: Retrying to create folder={}{}", Thread.currentThread().getId(), parentPath, nodeName);
                    createFolder();
                }
            } catch (Throwable e) {
                logger().warn("{}: Failed to create after retry folder={}{}", Thread.currentThread().getId(), parentPath, nodeName);
                logger().debug(Thread.currentThread().getId() + "ERROR: ", e);
            }
        }
        communicate("parentPath", parentPath);
        // de-register
        phaser.arriveAndDeregister();
    }

    private void createFolder() throws Throwable {
        createFolder(nodeName, parentPath);
    }

    private void createFolder(String nodeName, String parentPath) throws Throwable {
        FormEntityBuilder feb = FormEntityBuilder.create()
                .addParameter(":name", nodeName)
                .addParameter("./jcr:primaryType", resourceType);

        benchmark().measure(this, "Create Folder", getDefaultClient()).doPost(parentPath, feb.build(), HttpStatus.SC_CREATED);
    }


    @Override
    public AbstractTest newInstance() {
        return new CreateFolderTreeTest(phaser, rootParentPath, title, resourceType);
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_TITLE,
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
        return rootParentPath;
    }

    @ConfigArgSet(required = false, desc = "How many direct child folders will a folder have.", defaultValue = TreePhaser.DEFAULT_BASE)
    public AbstractTest setBase(String base) {
        this.phaser.setBase(Integer.parseInt(base));
        return this;
    }

    @ConfigArgGet
    public int getBase() {
        return this.phaser.getBase();
    }

    @ConfigArgSet(required = false, desc = "Reasource type for folders.", defaultValue = FOLDER_RESOURCE_TYPE)
    public AbstractTest setResourceType(String resourceType){
        this.resourceType = resourceType;
        return this;
    }

    @ConfigArgGet
    public String getResourceType() {
        return this.resourceType;
    }

}
