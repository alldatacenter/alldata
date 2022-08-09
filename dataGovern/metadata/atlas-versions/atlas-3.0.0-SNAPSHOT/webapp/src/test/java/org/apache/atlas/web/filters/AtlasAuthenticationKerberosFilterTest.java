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
package org.apache.atlas.web.filters;

import org.apache.atlas.RequestContext;
import org.apache.atlas.web.security.BaseSecurityTest;
import org.apache.atlas.web.service.EmbeddedServer;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.hdfs.web.URLConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.testng.annotations.Test;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PrivilegedExceptionAction;

import static org.testng.Assert.assertEquals;

/**
 *
 */
public class AtlasAuthenticationKerberosFilterTest extends BaseSecurityTest {
    public static final String TEST_USER_JAAS_SECTION = "TestUser";
    public static final String TESTUSER = "testuser";
    public static final String TESTPASS = "testpass";

    private File userKeytabFile;
    private File httpKeytabFile;

    class TestEmbeddedServer extends EmbeddedServer {
        public TestEmbeddedServer(int port, String path) throws IOException {
            super(ATLAS_DEFAULT_BIND_ADDRESS, port, path);
        }

        Server getServer() {
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

    @Test(enabled = false)
    public void testKerberosBasedLogin() throws Exception {
        String originalConf = System.getProperty("atlas.conf");

        setupKDCAndPrincipals();
        TestEmbeddedServer server = null;

        try {
            // setup the atlas-application.properties file
            String confDirectory = generateKerberosTestProperties();
            System.setProperty("atlas.conf", confDirectory);

            // need to create the web application programmatically in order to control the injection of the test
            // application properties
            server = new TestEmbeddedServer(23000, "webapp/target/apache-atlas");

            startEmbeddedServer(server.getServer());

            final URLConnectionFactory connectionFactory = URLConnectionFactory.DEFAULT_SYSTEM_CONNECTION_FACTORY;
            // attempt to hit server and get rejected
            URL url = new URL("http://localhost:23000/");
            HttpURLConnection connection = (HttpURLConnection) connectionFactory.openConnection(url, false);
            connection.setRequestMethod("GET");
            connection.connect();

            assertEquals(connection.getResponseCode(), 401);

            // need to populate the ticket cache with a local user, so logging in...
            Subject subject = loginTestUser();

            Subject.doAs(subject, new PrivilegedExceptionAction<Object>() {
                @Override
                public Object run() throws Exception {
                    // attempt to hit server and get rejected
                    URL url = new URL("http://localhost:23000/");
                    HttpURLConnection connection = (HttpURLConnection) connectionFactory.openConnection(url, true);
                    connection.setRequestMethod("GET");
                    connection.connect();

                    assertEquals(connection.getResponseCode(), 200);
                    assertEquals(RequestContext.get().getUser(), TESTUSER);
                    return null;
                }
            });
        } finally {
            server.getServer().stop();
            kdc.stop();

            if (originalConf != null) {
                System.setProperty("atlas.conf", originalConf);
            } else {
                System.clearProperty("atlas.conf");
            }

        }
    }

    protected Subject loginTestUser() throws LoginException, IOException {
        LoginContext lc = new LoginContext(TEST_USER_JAAS_SECTION, new CallbackHandler() {

            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback callback : callbacks) {
                    if (callback instanceof PasswordCallback) {
                        PasswordCallback passwordCallback = (PasswordCallback) callback;
                        passwordCallback.setPassword(TESTPASS.toCharArray());
                    }
                    if (callback instanceof NameCallback) {
                        NameCallback nameCallback = (NameCallback) callback;
                        nameCallback.setName(TESTUSER);
                    }
                }
            }
        });
        // attempt authentication
        lc.login();
        return lc.getSubject();
    }

    protected String generateKerberosTestProperties() throws Exception {
        PropertiesConfiguration props = new PropertiesConfiguration();
        props.setProperty("atlas.http.authentication.enabled", "true");
        props.setProperty("atlas.http.authentication.type", "kerberos");
        props.setProperty("atlas.http.authentication.kerberos.principal", "HTTP/localhost@" + kdc.getRealm());
        props.setProperty("atlas.http.authentication.kerberos.keytab", httpKeytabFile.getAbsolutePath());
        props.setProperty("atlas.http.authentication.kerberos.name.rules",
                "RULE:[1:$1@$0](.*@EXAMPLE.COM)s/@.*//\nDEFAULT");

        return writeConfiguration(props);
    }

    public void setupKDCAndPrincipals() throws Exception {
        // set up the KDC
        File kdcWorkDir = startKDC();

        userKeytabFile = createKeytab(kdc, kdcWorkDir, "dgi", "dgi.keytab");
        httpKeytabFile = createKeytab(kdc, kdcWorkDir, "HTTP", "spnego.service.keytab");

        // create a test user principal
        kdc.createPrincipal(TESTUSER, TESTPASS);

        StringBuilder jaas = new StringBuilder(1024);
        jaas.append("TestUser {\n" +
                "    com.sun.security.auth.module.Krb5LoginModule required\nuseTicketCache=true;\n" +
                "};\n");
        jaas.append(createJAASEntry("Client", "dgi", userKeytabFile));
        jaas.append(createJAASEntry("Server", "HTTP", httpKeytabFile));

        File jaasFile = new File(kdcWorkDir, "jaas.txt");
        FileUtils.write(jaasFile, jaas.toString());
        bindJVMtoJAASFile(jaasFile);
    }

}
