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

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.sling.testing.clients.ClientException;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.SlingClientConfig;
import org.apache.sling.testing.clients.SlingHttpResponse;
import org.apache.sling.testing.clients.util.HttpUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.InputStream;
import java.net.URI;

public class PackageManagerClient extends SlingClient {

    public PackageManagerClient(URI serverUrl, String user, String password) throws ClientException {
        super(serverUrl, user, password);
    }

    // We need this for adaptTo
    public PackageManagerClient(CloseableHttpClient http, SlingClientConfig config) throws ClientException {
        super(http, config);
    }

    public SlingHttpResponse uploadPackage(InputStream is, String fileName) throws ClientException {
        HttpEntity mpe = MultipartEntityBuilder.create().addPart("package", new InputStreamBody(is, fileName)).addTextBody("_charset_", "UTF-8").build();
        SlingHttpResponse exec = this.doPost("/crx/packmgr/service/exec.json?cmd=upload&jsonInTextarea=true", mpe, new int[]{200});
        ObjectMapper mapper = new ObjectMapper();

        JsonNode root;
        try {
            root = mapper.readTree(exec.getContent().replaceAll("</?textarea>", ""));
        } catch (Exception var8) {
            throw new ClientException("Unable to parse JSON response to upload request.", var8);
        }

        if(!root.get("success").getBooleanValue()) {
            throw new ClientException(root.get("msg").getTextValue());
        }

        return exec;
    }

    public boolean isPackageCreated(String packageName, String groupName) throws ClientException {
        return this.exists("/etc/packages/" + groupName + "/" + packageName);
    }

    public SlingHttpResponse installPackage(String packageName, String groupName, int... expectedStatus) throws ClientException {
        String postURL = "/crx/packmgr/service/script.html/etc/packages/" + groupName + "/" + packageName;
        HttpEntity multiPartEntity = MultipartEntityBuilder.create().addTextBody("cmd", "install").addTextBody("callback", "window.parent.Ext.Ajax.Stream.callback").addTextBody("autosave", "1024").addTextBody("recursive", "true").build();
        return this.doPost(postURL, multiPartEntity, HttpUtils.getExpectedStatus(200, expectedStatus));
    }

    public SlingHttpResponse uninstallPackage(String packageName, String groupName, int... expectedStatus) throws ClientException {
        String postURL = "/crx/packmgr/service/script.html/etc/packages/" + groupName + "/" + packageName;
        HttpEntity multiPartEntity = MultipartEntityBuilder.create().addTextBody("cmd", "uninstall").addTextBody("callback", "window.parent.Ext.Ajax.Stream.callback").build();
        return this.doPost(postURL, multiPartEntity, HttpUtils.getExpectedStatus(200, expectedStatus));
    }

    public SlingHttpResponse deletePackage(String packageName, String groupName, int... expectedStatus) throws ClientException {
        String url = "/crx/packmgr/service/script.html/etc/packages/" + groupName + "/" + packageName;
        MultipartEntityBuilder multiPartEntity = MultipartEntityBuilder.create().addTextBody("cmd", "delete").addTextBody("callback", "window.parent.Ext.Ajax.Stream.callback");
        return this.doPost(url, multiPartEntity.build(), HttpUtils.getExpectedStatus(200, expectedStatus));
    }
}
