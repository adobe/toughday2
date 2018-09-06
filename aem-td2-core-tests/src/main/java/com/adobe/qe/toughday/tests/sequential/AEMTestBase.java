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
package com.adobe.qe.toughday.tests.sequential;

import com.adobe.qe.sling.tests.sequential.SlingTestBase;
import com.adobe.qe.toughday.api.core.SequentialTest;
import com.adobe.qe.toughday.api.core.config.GlobalArgs;
import com.adobe.qe.sling.tests.utils.SlingClientsProxyFactory;
import org.apache.http.auth.AuthSchemeProvider;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.config.Lookup;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.apache.sling.testing.clients.SlingClient;
import org.apache.sling.testing.clients.interceptors.FormBasedAuthInterceptor;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Base class for AEM sequential tests.
 */
public abstract class AEMTestBase extends SlingTestBase {

    /**
     * Creates a client builder using the information from the Global Args and returns it in order to override any
     * default properties
     *
     * @param args the GlobalArgs
     * @return the builder for either building the default client or overwriting properties before
     * @throws URISyntaxException
     */
    public static SlingClient.Builder createClientBuilder(GlobalArgs args) throws Exception {
        return SlingTestBase.createClientBuilder(args);
    }

    public static SlingClient createClient(GlobalArgs args) throws Exception {
        return createClientBuilder(args).build();
    }

    /**
     * Creates a client builder using the information from the Global Args, but with different user/pass and returns
     * it in order to override any default properties
     *
     * @param args the GlobalArgs
     * @param user username to be used
     * @param password the password for the user
     * @return the client builder
     * @throws Exception
     */
    public static SlingClient.Builder createClientBuilder(GlobalArgs args, String user, String password) throws Exception {
        SlingClient.Builder builder = createClientBuilder(args);
        builder.setUser(user);
        builder.setPassword(password);

        return builder;
    }
}
