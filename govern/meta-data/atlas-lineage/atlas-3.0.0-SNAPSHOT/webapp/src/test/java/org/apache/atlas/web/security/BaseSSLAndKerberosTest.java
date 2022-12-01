/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
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

import org.apache.atlas.security.SecurityProperties;
import org.apache.atlas.web.service.SecureEmbeddedServer;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class BaseSSLAndKerberosTest extends BaseSecurityTest {
    public static final String TEST_USER_JAAS_SECTION = "TestUser";
    public static final String TESTUSER = "testuser";
    public static final String TESTPASS = "testpass";
    protected static final String DGI_URL = "https://localhost:21443/";
    protected Path jksPath;
    protected String providerUrl;
    protected File httpKeytabFile;
    protected File userKeytabFile;

    protected BaseSSLAndKerberosTest() {
        System.setProperty("https.protocols", "TLSv1.2");
    }

    class TestSecureEmbeddedServer extends SecureEmbeddedServer {

        public TestSecureEmbeddedServer(int port, String path) throws IOException {
            super(ATLAS_DEFAULT_BIND_ADDRESS, port, path);
        }

        public Server getServer() {
            return server;
        }

        @Override
        protected WebAppContext getWebAppContext(String path) {
            WebAppContext application = new WebAppContext(path, "/");
            application.setDescriptor(System.getProperty("projectBaseDir") + "/webapp/src/test/webapp/WEB-INF/web.xml");
            application.setClassLoader(Thread.currentThread().getContextClassLoader());
            return application;
        }
    }

    protected void setupCredentials() throws Exception {
        Configuration conf = new Configuration(false);

        File file = new File(jksPath.toUri().getPath());
        file.delete();
        conf.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH, providerUrl);

        CredentialProvider provider = CredentialProviderFactory.getProviders(conf).get(0);

        // create new aliases
        try {

            char[] storepass = {'k', 'e', 'y', 'p', 'a', 's', 's'};
            provider.createCredentialEntry(SecurityProperties.KEYSTORE_PASSWORD_KEY, storepass);

            char[] trustpass = {'k', 'e', 'y', 'p', 'a', 's', 's'};
            provider.createCredentialEntry(SecurityProperties.TRUSTSTORE_PASSWORD_KEY, trustpass);

            char[] trustpass2 = {'k', 'e', 'y', 'p', 'a', 's', 's'};
            provider.createCredentialEntry("ssl.client.truststore.password", trustpass2);

            char[] certpass = {'k', 'e', 'y', 'p', 'a', 's', 's'};
            provider.createCredentialEntry(SecurityProperties.SERVER_CERT_PASSWORD_KEY, certpass);

            // write out so that it can be found in checks
            provider.flush();
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }

    public void setupKDCAndPrincipals() throws Exception {
        // set up the KDC
        File kdcWorkDir = startKDC();

        userKeytabFile = createKeytab(kdc, kdcWorkDir, "dgi", "dgi.keytab");
        //createKeytab(kdc, kdcWorkDir, "zookeeper", "dgi.keytab");
        httpKeytabFile = createKeytab(kdc, kdcWorkDir, "HTTP", "spnego.service.keytab");

        // create a test user principal
        kdc.createPrincipal(TESTUSER, TESTPASS);

        StringBuilder jaas = new StringBuilder(1024);
        jaas.append(TEST_USER_JAAS_SECTION + " {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\nuseTicketCache=true;\n" +
                "};\n");
        jaas.append(createJAASEntry("Client", "dgi", userKeytabFile));
        jaas.append(createJAASEntry("Server", "HTTP", httpKeytabFile));

        File jaasFile = new File(kdcWorkDir, "jaas.txt");
        FileUtils.write(jaasFile, jaas.toString());
        bindJVMtoJAASFile(jaasFile);
    }
}
