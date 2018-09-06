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
import com.adobe.qe.toughday.api.annotations.Internal;
import com.adobe.qe.toughday.tests.sequential.AEMTestBase;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.sling.testing.clients.util.FormEntityBuilder;

import java.util.List;

@Internal
public class AddTagToResourceTest extends AEMTestBase {

    @Override
    public void test() throws Throwable {
        String resourcePath = getCommunication("resource", null);
        List<String> tags = getCommunication("tags", null);

        if(resourcePath == null || tags == null) {
            throw new IllegalStateException("Either the resource or the tags were missing");
        }
        resourcePath = resourcePath.endsWith("_jcr_content") ? resourcePath : StringUtils.stripEnd(resourcePath, "/") + "/_jcr_content";

        FormEntityBuilder builder = FormEntityBuilder.create();

        for(String tag : tags) {
            builder.addParameter("./cq:tags", tag);
        }

        builder.addParameter("./cq:tags@TypeHint", "String[]");

        try {
            logger().debug("{}: Trying to add tags {} to the resource {}", Thread.currentThread().getName(), tags, resourcePath);
            getDefaultClient().doPost(resourcePath, builder.build(), HttpStatus.SC_OK);
        } catch (Throwable e) {
            logger().warn("{}: Failed to add tags {} to the resource {}", Thread.currentThread().getName(), tags, resourcePath);
            logger().debug(Thread.currentThread().getName() + "ERROR: ", e);

            throw e;
        }
        logger().debug("{}: Successfully added tags {} to the resource {}", Thread.currentThread().getName(), tags, resourcePath);
    }

    @Override
    public AbstractTest newInstance() {
        return new AddTagToResourceTest();
    }
}
