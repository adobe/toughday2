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
import com.adobe.qe.toughday.tests.sequential.*;
import com.adobe.qe.toughday.tests.sequential.tags.AddTagToResourceTest;
import com.adobe.qe.toughday.tests.sequential.tags.CreateTagTreeTest;
import com.adobe.qe.toughday.tests.utils.TreePhaser;
import com.adobe.qe.toughday.tests.utils.WcmUtils;

@Tag(tags = { "author" })
@Description(desc=
        "This test creates tags and pages hierarchically. Each page gets assigned two tags. " +
                "One from the corresponding node in the tag tree and one that is the same for the whole page tree. " +
                "Each child on each level has \"base\" children. " +
                "Each author thread fills in a level in the tag tree, up to base^level")
public class CreateTaggedPagesTree extends CompositeTest {

    private CreateTagTreeTest createTagTreeTest;
    private CreatePageTreeTest createPageTreeTest;
    private AddTagToResourceTest addTagToResourceTest;

    public CreateTaggedPagesTree() { this(true); }

    public CreateTaggedPagesTree(boolean createChildren) {
        if (createChildren) {
            createTagTreeTest = new CreateTagTreeTest();
            createPageTreeTest = new CreatePageTreeTest();
            addTagToResourceTest = new AddTagToResourceTest();

            createTagTreeTest.setGlobalArgs(this.getGlobalArgs());
            createPageTreeTest.setGlobalArgs(this.getGlobalArgs());
            addTagToResourceTest.setGlobalArgs(this.getGlobalArgs());

            this.addChild(createTagTreeTest);
            this.addChild(createPageTreeTest);
            this.addChild(addTagToResourceTest);
        }
    }

    @Override
    public AbstractTest newInstance() {
        return new CreateTaggedPagesTree(false);
    }

    @ConfigArgSet(required = false, defaultValue = AuthoringTreeTest.DEFAULT_PAGE_TITLE,
            desc = "The title of the page. Internally, this is incremented")
    public AbstractTest setPageTitle(String title) {
        createPageTreeTest.setTitle(title);
        return this;
    }

    @ConfigArgGet
    public String getPageTitle() {
        return this.createPageTreeTest.getTitle();
    }

    @ConfigArgSet(required = false, defaultValue = WcmUtils.DEFAULT_PARENT_PATH,
            desc = "The path prefix for all pages.")
    public AbstractTest setParentPath(String parentPath) {
        createPageTreeTest.setParentPath(parentPath);
        return this;
    }

    @ConfigArgGet
    public String getParentPath() {
        return createPageTreeTest.getParentPath();
    }

    @ConfigArgSet(required = false, defaultValue = WcmUtils.DEFAULT_TEMPLATE,
        desc = "The title of the pages. Internally, this will be incremented")
    public AbstractTest setTemplate(String template) {
        createPageTreeTest.setTemplate(template);
        return this;
    }

    @ConfigArgGet
    public String getTemplate() {
        return createPageTreeTest.getTemplate();
    }

    @ConfigArgSet(required = false, defaultValue = CreateTagTreeTest.DEFAULT_NAMESPACE,
            desc = "The title of the tags. Internally, this will be incremented")
    public AbstractTest setTagTitle(String title) {
        createTagTreeTest.setTitle(title);
        return this;
    }

    @ConfigArgGet
    public String getTagTitle() {
        return createTagTreeTest.getTitle();
    }

    @ConfigArgSet(required = false, defaultValue = TreePhaser.DEFAULT_BASE)
    public AbstractTest setBase(String base) {
        createPageTreeTest.setBase(base);
        createTagTreeTest.setBase(base);
        return this;
    }

    @ConfigArgGet
    public int getBase() {
        return createPageTreeTest.getBase();
    }
}
