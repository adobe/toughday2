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
package com.adobe.qe.toughday.tests.utils;

import com.adobe.qe.toughday.api.core.AbstractTest;
import com.adobe.qe.toughday.tests.samplecontent.SampleContent;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.FormEntityBuilder;

public class WcmUtils {

    public static final String CMD_CREATE_PAGE = "createPage";
    public static final String CMD_CREATE_LIVECOPY = "createLiveCopy";
    public static final String CMD_ROLLOUT = "rollout";

    public static final String DEFAULT_PARENT_PATH = SampleContent.TOUGHDAY_SITE;
    public static final String DEFAULT_TEMPLATE = SampleContent.TOUGHDAY_TEMPLATE;

    public static SlingHttpResponse createPage(SlingClient client, String parentPath, String title, String template, int... expectedStatus)
            throws ClientException {
        FormEntityBuilder feb = FormEntityBuilder.create()
                .addParameter("cmd", CMD_CREATE_PAGE)
                .addParameter("parentPath", parentPath)
                .addParameter("title", title)
                .addParameter("template", template);

        return client.doPost("/bin/wcmcommand", feb.build(), expectedStatus);
    }

    public static SlingHttpResponse createLiveCopy(AbstractTest test, SlingClient client, String label, String title, String destPath, String srcPath,
                                                   boolean shallow, String[] rolloutConfigs, String[] missingPages, boolean excludeSubPages,
                                                   int... expectedStatus)
            throws Throwable {
        FormEntityBuilder feb = FormEntityBuilder.create()
                .addParameter("cmd", CMD_CREATE_LIVECOPY)
                .addParameter("title", title)
                .addParameter("label", label)
                .addParameter("destPath", destPath)
                .addParameter("srcPath", srcPath)
                .addParameter("missingPage@Delete", "true");
        fillParameters(feb, "missingPage", missingPages);
        feb.addParameter("excludeSubPages@Delete", "true");

        // in case if missing pages are created the parameter excludeSubPages is used
        feb.addParameter("excludeSubPages", Boolean.valueOf(excludeSubPages).toString());
        feb.addParameter("shallow@Delete", "true");
        feb.addParameter("shallow", Boolean.valueOf(shallow).toString());
        feb.addParameter("cq:rolloutConfigs@Delete", "true");
        fillParameters(feb, "cq:rolloutConfigs", rolloutConfigs);

        return test.benchmark().measure(test, "CreateLiveCopy", client).doPost("/bin/wcmcommand", feb.build(), expectedStatus);
    }

    private static void fillParameters(FormEntityBuilder formEntityBuilder, String paramName, String... values) {
        if (values != null && values.length > 0) {
            for (String value : values)
                formEntityBuilder.addParameter(paramName, value);
        }
    }

    public static SlingHttpResponse rolloutPage(SlingClient client, String type, boolean background, String[] sourcePaths,
                                                String[] paragraphPaths, String[] targetPaths, int... expectedStatus)
            throws ClientException {
        FormEntityBuilder feb = FormEntityBuilder.create()
                .addParameter("cmd", CMD_ROLLOUT)
                .addParameter("type", type)
                .addParameter("sling:bg", Boolean.valueOf(background).toString())
                .addParameter("msm:async", Boolean.valueOf(background).toString());
        fillParameters(feb, "path", sourcePaths);
        fillParameters(feb, "paras", paragraphPaths);
        fillParameters(feb, "msm:targetPath", targetPaths);
        return client.doPost("/bin/wcmcommand", feb.build(), expectedStatus);
    }
}
