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
package com.adobe.qe.toughday.tests.sequential.msm;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.annotations.*;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;
import com.adobe.qe.toughday.tests.samplecontent.SampleContent;
import com.adobe.qe.toughday.tests.utils.TreePhaser;
import com.adobe.qe.toughday.tests.utils.WcmUtils;

import java.util.UUID;

@Tag(tags = { "author" })
@Name(name = "create_lc")
@Description(desc = "Creates live copies from pages")
public class CreateLiveCopyFromPageTest extends AEMTestBase {

    public static final String SOURCE_PAGE_NAME = "create_lc_source";
    public static final String DESTINATION_PAGE_NAME = "create_lc_dest";
    public static  final String DEFAULT_SOURCE_ROOT_PAGE = "/content/toughday/language-master/en/toughday";


    public static final String DEFAULT_DESTINATION_ROOT_PAGE = SampleContent.TOUGHDAY_SITE;
    public static final String DEFAULT_PAGE_TITLE = "lc";

    private final TreePhaser phaser;

    private String title;
    private String sourcePage;
    private String destinationPage;
    private String destinationRoot;
    private String nodeName;
    private boolean failed = false;
    private int nextChild;


    public CreateLiveCopyFromPageTest() {
        this.phaser = new TreePhaser();
        this.sourcePage = DEFAULT_SOURCE_ROOT_PAGE;
        this.destinationRoot = DEFAULT_DESTINATION_ROOT_PAGE;
        this.title = DEFAULT_PAGE_TITLE;
    }

    public CreateLiveCopyFromPageTest(TreePhaser phaser, String title, String sourcePage, String destinationRoot) {
        this.phaser = phaser;
        this.sourcePage = sourcePage;
        this.destinationRoot = destinationRoot;
        this.title = title;
    }

    @Setup
    private void setup() throws Throwable {
        String isolatedFolder = "toughday_lc" + UUID.randomUUID();
        try {
            getDefaultClient().createFolder(isolatedFolder, isolatedFolder, destinationRoot);
            destinationRoot = destinationRoot + "/" + isolatedFolder;
        } catch (Throwable e) {
            logger().debug("Could not create isolated folder for running " + getFullName());
        }
    }

    @Before
    private void before() throws Throwable {
        this.sourcePage = getCommunication("resource", sourcePage);
        phaser.register();

        // this gets the next node on the level and potentially waits for other threads to reset the level
        // save those values for later use
        this.nextChild = phaser.getNextNode();
        this.destinationPage = TreePhaser.computeParentPath(nextChild, phaser.getLevel(),
                phaser.getBase(), title, destinationRoot);
        this.nodeName = TreePhaser.computeNodeName(nextChild, phaser.getBase(), title);
        this.failed = false;
    }

    @Override
    public void test() throws Throwable {
        try {
            logger().debug("{}: Trying to create live copy={}{}, from page={}", Thread.currentThread().getName(), destinationPage, nodeName, sourcePage);

            createLC();
        } catch (Throwable e) {
            this.failed = true;
            // log and throw. It's normally an anti-pattern, but we don't log exceptions anywhere on the upper level,
            // we're just count them.
            logger().warn("{}: Failed to create live copy={}{}, from page={}", Thread.currentThread().getName(), destinationPage, nodeName, sourcePage);
            logger().debug(Thread.currentThread().getName() + "ERROR: ", e);

            throw e;
        }
    }

    private void createLC() throws Throwable {
        WcmUtils.createLiveCopy(this, getDefaultClient(), nodeName, title, destinationPage, sourcePage, false, null, null, false, 200);
        communicate("livecopy", destinationPage + nodeName);
    }

    @After
    private void after() {
        // make sure the page was created
        for (int i=0; i<5; i++) {
            try {
                // If operation was marked as failed and the path really does not exist,
                // try and create it, as it is needed as the parent path for the children on the next level
                if (!failed || getDefaultClient().exists(this.destinationPage + nodeName)) {
                    logger().debug("{}: Successfully created live copy={}{}, from page={}", Thread.currentThread().getName(), destinationPage, nodeName, sourcePage);
                    break;
                } else {
                    logger().debug("{}: Retrying to create live copy={}{}, from page={}", Thread.currentThread().getName(), destinationPage, nodeName, sourcePage);
                    createLC();
                }
            } catch (Throwable e) {
                logger().warn("{}: Failed to create after retry live copy={}{}, from page={}", Thread.currentThread().getName(), destinationPage, nodeName, sourcePage);
                logger().debug(Thread.currentThread().getName() + ": ERROR: ", e);
            }
        }

        // de-register
        phaser.arriveAndDeregister();
    }

    @Override
    public AbstractTest newInstance() {
        return new CreateLiveCopyFromPageTest(phaser, title, sourcePage, destinationRoot);
    }

    @ConfigArgSet(required = false, desc = "The source page for live copies", defaultValue = DEFAULT_SOURCE_ROOT_PAGE)
    public AbstractTest setSourcePage(String page) {
        this.sourcePage = page;
        return this;
    }

    @ConfigArgGet
    public String getSourcePage() {
        return this.sourcePage;
    }

    @ConfigArgSet(required = false, desc = "Default root for live copies", defaultValue = DEFAULT_DESTINATION_ROOT_PAGE)
    public AbstractTest setDestinationRoot(String page) {
        this.destinationRoot = page;
        return this;
    }

    @ConfigArgGet
    public String getDestinationRoot() {
        return this.destinationRoot;
    }

    @ConfigArgSet(required = false, desc = "Title for livecopies", defaultValue = DEFAULT_PAGE_TITLE)
    public AbstractTest setTitle(String title) {
        this.title = title;
        return this;
    }

    @ConfigArgGet
    public String getTitle() {
        return this.title;
    }

    @ConfigArgSet(required = false, desc = "How many direct child pages will a page have.", defaultValue = TreePhaser.DEFAULT_BASE)
    public AbstractTest setBase(String base) {
        this.phaser.setBase(Integer.parseInt(base));
        return this;
    }

    @ConfigArgGet
    public int getBase() {
        return this.phaser.getBase();
    }
}
