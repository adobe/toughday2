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
import com.adobe.qe.toughday.api.annotations.ConfigArgGet;
import com.adobe.qe.toughday.api.annotations.ConfigArgSet;
import com.adobe.qe.toughday.tests.samplecontent.SampleContent;
import com.adobe.qe.toughday.tests.sequential.CreatePageTreeTest;
import com.adobe.qe.toughday.tests.sequential.PublishPageTest;
import com.adobe.qe.toughday.tests.utils.TreePhaser;

@Description(desc = "This test creates pages hierarchically and activates them. Each child on each level has \"base\" children. " +
        "Each author thread fills in a level in the pages tree, up to base^level")
public class ActivatePagesTreeTest extends CompositeTest {
    private CreatePageTreeTest createPageTreeTest;
    private PublishPageTest publishPageTest;

    public ActivatePagesTreeTest() { this(true); }

    public ActivatePagesTreeTest(boolean createChildren) {
        if(createChildren) {
            createPageTreeTest = new CreatePageTreeTest();
            publishPageTest = new PublishPageTest();

            this.addChild(createPageTreeTest);
            this.addChild(publishPageTest);
        }
    }

    @Override
    public AbstractTest newInstance() {
        return new ActivatePagesTreeTest(false);
    }

    @ConfigArgSet(required = false, defaultValue = AuthoringTreeTest.DEFAULT_PAGE_TITLE,
            desc = "The title of the page. Internally, this is incremented")
    public void setTitle(String title) {
        createPageTreeTest.setTitle(title);
    }

    @ConfigArgGet
    public String getTitle() {
        return createPageTreeTest.getTitle();
    }

    @ConfigArgSet(required = false, defaultValue = SampleContent.TOUGHDAY_SITE,
            desc = "The path prefix for all pages.")
    public void setParentPath(String parentPath) {
        createPageTreeTest.setParentPath(parentPath);
    }

    @ConfigArgGet
    public String getParentPath() {
        return createPageTreeTest.getParentPath();
    }

    @ConfigArgSet(required = false, defaultValue = SampleContent.TOUGHDAY_TEMPLATE, desc = "Template for all the pages created.")
    public void setTemplate(String template) {
        createPageTreeTest.setTemplate(template);
    }

    @ConfigArgGet
    public String getTemplate() {
        return createPageTreeTest.getTemplate();
    }

    @ConfigArgSet(required = false, desc = "How many direct child pages will a page have.", defaultValue = TreePhaser.DEFAULT_BASE)
    public void setBase(String base) {
        createPageTreeTest.setBase(base);
    }

    @ConfigArgGet
    public int getBase() {
        return createPageTreeTest.getBase();
    }

    @ConfigArgSet(required = false, defaultValue = PublishPageTest.DEFAULT_WITH_REFERENCES, desc = "Publish references along with the page")
    public void setWithReferences(String withReferences) {
        publishPageTest.setWithReferences(withReferences);
    }

    @ConfigArgGet
    public boolean getWithReferences() {
        return publishPageTest.getWithReferences();
    }
}
