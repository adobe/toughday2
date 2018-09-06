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
package com.adobe.qe.toughday.tests.composite;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.CompositeTest;
import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.tests.sequential.CreatePageTreeTest;
import com.adobe.qe.toughday.tests.sequential.image.UploadImageTest;
import com.adobe.qe.toughday.tests.utils.TreePhaser;
import com.adobe.qe.toughday.tests.utils.WcmUtils;

@Tag(tags = { "author" })
@Description(desc = "Authoring test. Steps: create page, upload asset.")
public class AuthoringTreeTest extends CompositeTest {
    public static final String DEFAULT_PAGE_TITLE = "toughday_tree_title";
    public static final String DEFAULT_ASSET_NAME = "toughday_png_asset";
    public static final String DEFAULT_MIME_TYPE = "image/png";
    public static final String DEFAULT_RESOURCE_PATH = "image.png";

    private CreatePageTreeTest createPageTest;
    private UploadImageTest uploadImageTest;

    public AuthoringTreeTest() {
        this(true);
    }

    public AuthoringTreeTest(boolean createChildren) {
        if (createChildren) {
            this.createPageTest = new CreatePageTreeTest();
            this.createPageTest.setGlobalArgs(this.getGlobalArgs());

            this.uploadImageTest = new UploadImageTest();
            this.uploadImageTest.setGlobalArgs(this.getGlobalArgs());

            this.addChild(createPageTest);
            this.addChild(uploadImageTest);

            this.setPageTitle(DEFAULT_PAGE_TITLE);
            this.setImageName(DEFAULT_ASSET_NAME);
            this.setMimeType(DEFAULT_MIME_TYPE);
            this.setResourcePath(DEFAULT_RESOURCE_PATH);
        }
    }

    @Override
    public AbstractTest newInstance() {
        return new AuthoringTreeTest(false);
    }

    @ConfigArgSet(required = false, defaultValue = WcmUtils.DEFAULT_TEMPLATE)
    public AuthoringTreeTest setPageTemplate(String template) {
        createPageTest.setTemplate(template);
        return this;
    }

    @ConfigArgGet
    public String getPageTemplate() {
        return createPageTest.getTemplate();
    }

    @ConfigArgSet(required = false, defaultValue = WcmUtils.DEFAULT_PARENT_PATH,
            desc = "The path prefix for all pages.")
    public AuthoringTreeTest setParentPath(String parentPath) {
        createPageTest.setParentPath(parentPath);
        uploadImageTest.setParentPath(parentPath);
        return this;
    }

    @ConfigArgGet
    public String getParentPath() {
        return createPageTest.getParentPath();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_RESOURCE_PATH)
    public AuthoringTreeTest setResourcePath(String resourcePath) {
        uploadImageTest.setResourcePath(resourcePath);
        return this;
    }

    @ConfigArgGet
    public String getResourcePath() {
        return uploadImageTest.getResourcePath();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_MIME_TYPE)
    public AuthoringTreeTest setMimeType(String mimeType) {
        uploadImageTest.setMimeType(mimeType);
        return this;
    }

    @ConfigArgGet
    public String getMimeType() {
        return uploadImageTest.getMimeType();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_PAGE_TITLE,
            desc = "The title of the page. Internally, this is incremented")
    public AuthoringTreeTest setPageTitle(String title) {
        this.createPageTest.setTitle(title);
        return this;
    }

    @ConfigArgGet
    public String getPageTitle() {
        return this.createPageTest.getTitle();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_ASSET_NAME)
    public AuthoringTreeTest setImageName(String name) {
        this.uploadImageTest.setFileName(name);
        return this;
    }

    @ConfigArgGet
    public String getImageName() {
        return this.uploadImageTest.getFileName();
    }

    @ConfigArgSet(required = false, defaultValue = TreePhaser.DEFAULT_BASE)
    public AuthoringTreeTest setBase(String base) {
        this.createPageTest.setBase(base);
        return this;
    }

    @ConfigArgGet
    public int getBase() {
        return this.createPageTest.getBase();
    }
}
