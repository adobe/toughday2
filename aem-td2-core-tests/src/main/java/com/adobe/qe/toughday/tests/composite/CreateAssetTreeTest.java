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
import com.adobe.qe.toughday.tests.sequential.CreateFolderTreeTest;
import com.adobe.qe.toughday.tests.sequential.image.UploadImageTest;
import com.adobe.qe.toughday.tests.utils.TreePhaser;

@Tag(tags = { "author" })
@Description(desc="This test creates folders with assets hierarchically. " +
        "Each child on each level has \"base\" folder children and \"base\" asset children")
public class CreateAssetTreeTest extends CompositeTest {

    private CreateFolderTreeTest createFolderTreeTest;
    private UploadImageTest uploadImageTest;

    public CreateAssetTreeTest() { this(true); }

    public CreateAssetTreeTest(boolean createChildren) {
        if (createChildren) {
            createFolderTreeTest = new CreateFolderTreeTest();
            uploadImageTest = new UploadImageTest();

            createFolderTreeTest.setGlobalArgs(this.getGlobalArgs());
            uploadImageTest.setGlobalArgs(this.getGlobalArgs());

            this.addChild(createFolderTreeTest);
            this.addChild(uploadImageTest);
        }
    }

    @Override
    public AbstractTest newInstance() {
        return new CreateAssetTreeTest(false);
    }

    @ConfigArgSet(required = false, defaultValue = CreateFolderTreeTest.DEFAULT_TITLE,
            desc = "The title of the folders. Internally, this is incremented")
    public CreateAssetTreeTest setFolderTitle(String title) {
        createFolderTreeTest.setTitle(title);
        return this;
    }

    @ConfigArgGet
    public String getFolderTitle() {
        return createFolderTreeTest.getTitle();
    }

    @ConfigArgSet(required = false, defaultValue = AuthoringTreeTest.DEFAULT_ASSET_NAME,
            desc = "The title of the assets. Internally, this is incremented")
    public CreateAssetTreeTest setAssetTitle(String title) {
        uploadImageTest.setFileName(title);
        return this;
    }

    @ConfigArgGet
    public String getAssetTitle() {
        return this.uploadImageTest.getFileName();
    }

    @ConfigArgSet(required = false, defaultValue = CreateFolderTreeTest.DEFAULT_PARENT_PATH,
            desc = "The path prefix for the asset tree.")
    public CreateAssetTreeTest setParentPath(String parentPath) {
        createFolderTreeTest.setParentPath(parentPath);
        return this;
    }

    @ConfigArgGet
    public String getParentPath() {
        return this.createFolderTreeTest.getParentPath();
    }

    @ConfigArgSet(required = false, defaultValue = AuthoringTest.DEFAULT_RESOURCE_PATH,
            desc = "The image resource path either in the classpath or the filesystem")
    public void setAssetResourcePath(String resourcePath) {
        uploadImageTest.setResourcePath(resourcePath);
    }

    @ConfigArgGet
    public String getAssetResourcePath() {
        return uploadImageTest.getResourcePath();
    }

    @ConfigArgSet(required = false, defaultValue = TreePhaser.DEFAULT_BASE)
    public CreateAssetTreeTest setBase(String base) {
        createFolderTreeTest.setBase(base);
        return this;
    }

    @ConfigArgGet
    public int getBase() {
        return this.createFolderTreeTest.getBase();
    }
}
