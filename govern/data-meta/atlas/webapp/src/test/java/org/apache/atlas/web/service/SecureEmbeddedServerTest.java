/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.service;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.web.TestUtils;
import org.apache.atlas.web.security.BaseSecurityTest;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URL;

import static org.apache.atlas.security.SecurityProperties.CERT_STORES_CREDENTIAL_PROVIDER_PATH;

public class SecureEmbeddedServerTest extends SecureEmbeddedServerTestBase {
    @Test
    public void testServerConfiguredUsingCredentialProvider() throws Exception {
        // setup the configuration
        final PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty(CERT_STORES_CREDENTIAL_PROVIDER_PATH, providerUrl);
        configuration.setProperty("atlas.services.enabled", false);
        configuration.setProperty("atlas.notification.embedded", "false");
        // setup the credential provider
        setupCredentials();

        String persistDir = BaseSecurityTest.writeConfiguration(configuration);
        String originalConf = System.getProperty("atlas.conf");
        System.setProperty("atlas.conf", persistDir);

        ApplicationProperties.forceReload();
        SecureEmbeddedServer secureEmbeddedServer = null;
        try {
            secureEmbeddedServer = new SecureEmbeddedServer(EmbeddedServer.ATLAS_DEFAULT_BIND_ADDRESS,
                21443, TestUtils.getWarPath()) {
                @Override
                protected PropertiesConfiguration getConfiguration() {
                    return configuration;
                }

                @Override
                protected WebAppContext getWebAppContext(String path) {
                    WebAppContext application = new WebAppContext(path, "/");
                    application.setDescriptor(
                            System.getProperty("projectBaseDir") + "/webapp/src/test/webapp/WEB-INF/web.xml");
                    application.setClassLoader(Thread.currentThread().getContextClassLoader());
                    return application;
                }

            };
            secureEmbeddedServer.server.start();

            URL url = new URL("https://localhost:21443/api/atlas/admin/status");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            // test to see whether server is up and root page can be served
            Assert.assertEquals(connection.getResponseCode(), 200);
        } catch(Throwable e) {
            Assert.fail("War deploy failed", e);
        } finally {
            secureEmbeddedServer.server.stop();

            if (originalConf == null) {
                System.clearProperty("atlas.conf");
            } else {
                System.setProperty("atlas.conf", originalConf);
            }
        }
    }
}
