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
package com.adobe.qe.toughday.internal.core.config;

import com.adobe.qe.toughday.internal.core.TestSuite;
import com.adobe.qe.toughday.tests.composite.AuthoringTest;
import com.adobe.qe.toughday.tests.composite.AuthoringTreeTest;
import com.adobe.qe.toughday.tests.composite.CreateAssetTreeTest;
import com.adobe.qe.toughday.tests.composite.msm.CreateLiveCopyTreeTest;
import com.adobe.qe.toughday.tests.sequential.GetHomepageTest;
import com.adobe.qe.toughday.tests.sequential.GetTest;
import com.adobe.qe.toughday.tests.sequential.image.DeleteImageTest;
import com.adobe.qe.toughday.tests.sequential.search.QueryBuilderTest;

import java.util.HashMap;

/**
 * Class for holding predefined suites. Don't turn this into Singleton, or make a static map, otherwise two Configuration
 * objects will change the same suite and it will result in unexpected behaviour.
 */
public class PredefinedSuites extends HashMap<String, TestSuite> {
    public static final String DEFAULT_SUITE_NAME = "toughday";

    public PredefinedSuites() {
        put("get_tests", new TestSuite()
                        .add(new GetHomepageTest().setName("Get Homepage").setWeight(Integer.toString(10)))
                        .add(new GetTest().setPath("/sites.html").setName("Get /sites.html").setWeight(Integer.toString(5)))
                        //.add(new GetTest().setPath("/projects.html").setName("Get /projects.html"), 5)
                        .add(new GetTest().setPath("/assets.html").setName("Get /assets.html").setWeight(Integer.toString(5)))
                        // maybe more here?
                        .setDescription("Executes GET requests on common paths")
                        .addTag("author")
        );
        put("tree_authoring", new TestSuite()
                        .add(new AuthoringTreeTest().setName("Tree Authoring").setWeight(Integer.toString(2)))
                        .setDescription("A full authoring test with \"create hierarchical pages\", \"upload asset\", " +
                                "\"delete asset\". The pages are not deleted.")
                        .addTag("author")
        );
        put("authoring", new TestSuite()
                        .add(new AuthoringTest().setName("Authoring").setWeight(Integer.toString(2)))
                        .setDescription("A full authoring test with \"create page\", \"upload asset\", " +
                                "\"delete asset\", \"delete page\" steps. " +
                                "The pages are deleted.")
                        .addTag("author")
        );
        put("publish", new TestSuite()
                        .add(new GetHomepageTest().setWeight(Integer.toString(1)))
                        .add(new GetTest().setPath("/").setName("Get1").setWeight(Integer.toString(1)))
                        .add(new GetTest().setPath("/").setName("Get2").setWeight(Integer.toString(1)))
                        .addTag("publish")
                        .setDescription("Publish suite")
        );
        put("toughday", new TestSuite()
                        .add(new CreateLiveCopyTreeTest()
                                .setBase(String.valueOf(5))
                                .setSourcePageTitle("IAmAPage")
                                .setName("CreateLiveCopy").setWeight(Integer.toString(5)).setTimeout(Long.toString(-1)).setCount(Long.toString(80000)))
                        .add(new CreateAssetTreeTest()
                                .setAssetTitle("IAmAnAsset")
                                .setFolderTitle("IAmAFolder")
                                .setBase(String.valueOf(3))
                                .setName("UploadAsset").setWeight(Integer.toString(5)).setTimeout(Long.toString(-1)).setCount(Long.toString(20000)))
                        .add(new DeleteImageTest()
                                .setName("DeleteAsset").setWeight(Integer.toString(5)).setTimeout(Long.toString(-1)).setCount(Long.toString(20000)))
                        .add(new QueryBuilderTest()
                                .setQuery("type=nt:unstructured&group.1_path=/libs&orderby=@jcr:score&orderby.sort=desc")
                                .setName("Query").setWeight(Integer.toString(10)))
                        .add(new GetHomepageTest()
                                .setName("GetHomepage").setWeight(Integer.toString(75)))
                        .setDescription("A heavy duty suite of AEM use cases. " +
                                "It performs operations like: search, upload assets, delete assets, create pages, live copies and folders and gets the home page. " +
                                "It has a proportion of 15% writes vs 85% reads.")
                        .addTag("author")
        );
    }

    public TestSuite getDefaultSuite() {
        return this.get(DEFAULT_SUITE_NAME);
    }
}
