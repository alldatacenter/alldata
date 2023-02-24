/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.security;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.web.TestUtils;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.alias.JavaKeyStoreProvider;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;

import static org.apache.atlas.security.SecurityProperties.TLS_ENABLED;

/**
 * Perform all the necessary setup steps for client and server comm over SSL/Kerberos, but then don't estalish a
 * kerberos user for the invocation.  Need a separate use case since the Jersey layer cached the URL connection handler,
 * which indirectly caches the kerberos delegation token.
 */
public class NegativeSSLAndKerberosTest extends BaseSSLAndKerberosTest {

    private TestSecureEmbeddedServer secureEmbeddedServer;
    private String originalConf;
    private AtlasClient dgiClient;

    //@BeforeClass
    public void setUp() throws Exception {
        jksPath = new Path(Files.createTempDirectory("tempproviders").toString(), "test.jks");
        providerUrl = JavaKeyStoreProvider.SCHEME_NAME + "://file/" + jksPath.toUri();

        String persistDir = TestUtils.getTempDirectory();

        setupKDCAndPrincipals();
        setupCredentials();

        // client will actually only leverage subset of these properties
        final PropertiesConfiguration configuration = getSSLConfiguration(providerUrl);

        persistSSLClientConfiguration(configuration);

        TestUtils.writeConfiguration(configuration, persistDir + File.separator +
            ApplicationProperties.APPLICATION_PROPERTIES);

        String confLocation = System.getProperty("atlas.conf");
        URL url;
        if (confLocation == null) {
            url = NegativeSSLAndKerberosTest.class.getResource("/" + ApplicationProperties.APPLICATION_PROPERTIES);
        } else {
            url = new File(confLocation, ApplicationProperties.APPLICATION_PROPERTIES).toURI().toURL();
        }
        configuration.load(url);

        configuration.setProperty(TLS_ENABLED, true);
        configuration.setProperty("atlas.authentication.method.kerberos", "true");
        configuration.setProperty("atlas.authentication.keytab",userKeytabFile.getAbsolutePath());
        configuration.setProperty("atlas.authentication.principal","dgi/localhost@"+kdc.getRealm());

        configuration.setProperty("atlas.authentication.method.file", "false");
        configuration.setProperty("atlas.authentication.method.kerberos", "true");
        configuration.setProperty("atlas.authentication.method.kerberos.principal", "HTTP/localhost@" + kdc.getRealm());
        configuration.setProperty("atlas.authentication.method.kerberos.keytab", httpKeytabFile.getAbsolutePath());
        configuration.setProperty("atlas.authentication.method.kerberos.name.rules",
                "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nDEFAULT");

        configuration.setProperty("atlas.authentication.method.file", "true");
        configuration.setProperty("atlas.authentication.method.file.filename", persistDir
                + "/users-credentials");
        configuration.setProperty("atlas.auth.policy.file",persistDir
                + "/policy-store.txt" );
        configuration.setProperty("atlas.authentication.method.trustedproxy", "false");

        TestUtils.writeConfiguration(configuration, persistDir + File.separator +
                ApplicationProperties.APPLICATION_PROPERTIES);

        setupUserCredential(persistDir);
        setUpPolicyStore(persistDir);

        // save original setting
        originalConf = System.getProperty("atlas.conf");
        System.setProperty("atlas.conf", persistDir);

        dgiClient = new AtlasClient(configuration, DGI_URL);


        secureEmbeddedServer = new TestSecureEmbeddedServer(21443, getWarPath()) {
            @Override
            public Configuration getConfiguration() {
                return configuration;
            }
        };
        secureEmbeddedServer.getServer().start();
    }

    //@AfterClass
    public void tearDown() throws Exception {
        if (secureEmbeddedServer != null) {
            secureEmbeddedServer.getServer().stop();
        }

        if (kdc != null) {
            kdc.stop();
        }

        if (originalConf != null) {
            System.setProperty("atlas.conf", originalConf);
        }
    }

    @Test(enabled = false)
    public void testUnsecuredClient() throws Exception {
        try {
            dgiClient.listTypes();
            Assert.fail("Should have failed with GSSException");
        } catch(Exception e) {
            e.printStackTrace();
            Assert.assertTrue(e.getMessage().contains("Mechanism level: Failed to find any Kerberos tgt"));
        }
    }
}
