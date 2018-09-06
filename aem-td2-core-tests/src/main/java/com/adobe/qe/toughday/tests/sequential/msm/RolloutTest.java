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
import com.adobe.qe.toughday.api.annotations.Internal;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;
import com.adobe.qe.toughday.tests.utils.WcmUtils;

@Internal
@Tag(tags = { "author" })
@Name(name = "rollout_source")
@Description(desc = "Rollout the source page/ blueprint")
public class RolloutTest extends AEMTestBase {

    private String sourcePage = null;
    private String destinationPage = null;
    private String type;
    private boolean background;

    public RolloutTest() {
        this.type = "page"; // or "deep"
        this.background = false;
    }

    public RolloutTest(String sourcePage, String destinationPage, String type, boolean background) {
        this.sourcePage = sourcePage;
        this.destinationPage = destinationPage;
        this.type = type;
        this.background = background;
    }

    @Before
    private void before() throws Throwable {
        this.sourcePage = getCommunication("resource", sourcePage);
        this.destinationPage = getCommunication("livecopy", destinationPage);
    }

    @Override
    public void test() throws Throwable {
        try {
            logger().debug("{}: Trying to rollout page={}, from source={}", Thread.currentThread().getName(), destinationPage, sourcePage);

            WcmUtils.rolloutPage(getDefaultClient(), type, background,
                    new String[]{sourcePage}, null, new String[]{destinationPage}, 200);
        } catch (Throwable e) {
            logger().warn("{}: Failed to rollout page={}{}, from source={}", Thread.currentThread().getName(), destinationPage, sourcePage);
            logger().debug(Thread.currentThread().getName() + ": ERROR: ", e);

            throw e;
        }

        logger().debug("{}: Successfully rollout page={}, from source={}", Thread.currentThread().getName(), destinationPage, sourcePage);
    }

    @Override
    public AbstractTest newInstance() {
        return new RolloutTest(sourcePage, destinationPage, type, background);
    }

    @ConfigArgSet(required = true, desc = "The source page to rollout")
    public AbstractTest setSourcePage(String page) {
        this.sourcePage = page;
        return this;
    }

    @ConfigArgGet
    public String getSourcePage() {
        return this.sourcePage;
    }

    @ConfigArgSet(required = true, desc = "The destination page to rollout to")
    public AbstractTest setDestinationPage(String page) {
        this.destinationPage = page;
        return this;
    }

    @ConfigArgGet
    public String getDestinationPage() {
        return this.destinationPage;
    }

    @ConfigArgSet(required = false, desc = "page / deep", defaultValue = "page")
    public AbstractTest setType(String type) {
        this.type = type;
        return this;
    }

    @ConfigArgGet
    public String getType() {
        return this.type;
    }

    @ConfigArgSet(required = false, desc = "true/false - Whether to rollout in the background", defaultValue = "false")
    public AbstractTest setBackground(String background) {
        this.background = Boolean.parseBoolean(background);
        return this;
    }

    @ConfigArgGet
    public boolean getBackground() {
        return this.background;
    }
}
