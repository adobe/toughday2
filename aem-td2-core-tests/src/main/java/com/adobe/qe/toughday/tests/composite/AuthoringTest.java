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

import com.adobe.qe.toughday.api.annotations.Description;
import com.adobe.qe.toughday.api.annotations.Tag;
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.api.core.CompositeTest;
import com.adobe.qe.toughday.tests.sequential.CreatePageTest;
import com.adobe.qe.toughday.tests.sequential.image.DeleteImageTest;
import com.adobe.qe.toughday.tests.sequential.DeletePageTest;
import com.adobe.qe.toughday.tests.sequential.image.UploadImageTest;
import org.apache.logging.log4j.Logger;

@Tag(tags = { "author" })
@Description(desc = "Authoring test. Steps: create page, upload asset, delete page, delete asset.")
public class AuthoringTest extends CompositeTest {
    public static final String DEFAULT_PAGE_TITLE = "toughday_title";
    public static final String DEFAULT_ASSET_NAME = "toughday_png_asset";
    public static final String DEFAULT_MIME_TYPE = "image/png";
    public static final String DEFAULT_RESOURCE_PATH = "image.png";

    private CreatePageTest createPageTest;
    private UploadImageTest uploadAssetTest;
    private DeletePageTest deletePageTest;
    private DeleteImageTest deleteAssetTest;

    public AuthoringTest() {
        this(true);
    }

    public AuthoringTest(boolean createChildren) {
        if (createChildren) {
            this.createPageTest = new CreatePageTest();
            this.createPageTest.setGlobalArgs(this.getGlobalArgs());

            this.uploadAssetTest = new UploadImageTest();
            this.uploadAssetTest.setGlobalArgs(this.getGlobalArgs());

            this.deletePageTest = new DeletePageTest();
            this.deletePageTest.setGlobalArgs(this.getGlobalArgs());

            this.deleteAssetTest = new DeleteImageTest();
            this.deleteAssetTest.setGlobalArgs(this.getGlobalArgs());

            this.addChild(createPageTest);
            this.addChild(uploadAssetTest);
            this.addChild(deletePageTest);
            this.addChild(deleteAssetTest);

            this.deletePageTest.setForce(Boolean.toString(true));
            this.deleteAssetTest.setForce(Boolean.toString(true));

            this.setPageTitle(DEFAULT_PAGE_TITLE);
            this.setPageTemplate(CreatePageTest.DEFAULT_TEMPLATE);
            this.setParentPath(CreatePageTest.DEFAULT_PARENT_PATH);
            this.setImageName(DEFAULT_ASSET_NAME);
            this.setMimeType(DEFAULT_MIME_TYPE);
            this.setResourcePath(DEFAULT_RESOURCE_PATH);
        }
    }

    @Override
    public AbstractTest newInstance() {
        return new AuthoringTest(false);
    }

    @ConfigArgSet(required = false, defaultValue = CreatePageTest.DEFAULT_TEMPLATE)
    public AuthoringTest setPageTemplate(String template) {
        createPageTest.setTemplate(template);
        return this;
    }

    @ConfigArgGet
    public String getPageTemplate() {
        return this.createPageTest.getTemplate();
    }

    @ConfigArgSet(required = false, defaultValue = CreatePageTest.DEFAULT_PARENT_PATH,
            desc = "The path prefix for all pages.")
    public AuthoringTest setParentPath(String parentPath) {
        createPageTest.setParentPath(parentPath);
        deletePageTest.setParentPath(parentPath);
        uploadAssetTest.setParentPath(parentPath);
        deleteAssetTest.setParentPath(parentPath);
        return this;
    }

    @ConfigArgGet
    public String getParentPath() {
        return this.createPageTest.getTemplate();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_RESOURCE_PATH)
    public AuthoringTest setResourcePath(String resourcePath) {
        uploadAssetTest.setResourcePath(resourcePath);
        return this;
    }

    @ConfigArgGet
    public String getResourcePath() {
        return this.uploadAssetTest.getResourcePath();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_MIME_TYPE)
    public AuthoringTest setMimeType(String mimeType) {
        uploadAssetTest.setMimeType(mimeType);
        return this;
    }

    @ConfigArgGet
    public String getMimeType() {
        return this.uploadAssetTest.getMimeType();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_PAGE_TITLE,
            desc = "The title of the page. Internally, this is incremented")
    public AuthoringTest setPageTitle(String title) {
        this.createPageTest.setTitle(title);
        this.deletePageTest.setTitle(title);
        return this;
    }

    @ConfigArgGet
    public String getPageTitle() {
        return this.createPageTest.getTitle();
    }

    @ConfigArgSet(required = false, defaultValue = DEFAULT_ASSET_NAME)
    public AuthoringTest setImageName(String name) {
        this.uploadAssetTest.setFileName(name);
        this.deleteAssetTest.setTitle(name);
        return this;
    }

    @ConfigArgGet
    public String getImageName() {
        return this.uploadAssetTest.getFileName();
    }
}
