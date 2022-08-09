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

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.web.TestUtils;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.hadoop.minikdc.MiniKdc;
import org.apache.hadoop.security.ssl.SSLFactory;
import org.apache.hadoop.security.ssl.SSLHostnameVerifier;
import org.apache.zookeeper.Environment;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.testng.Assert;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.nio.file.Files;
import java.util.Locale;
import java.util.Properties;

import static org.apache.atlas.security.SecurityProperties.CERT_STORES_CREDENTIAL_PROVIDER_PATH;
import static org.apache.atlas.security.SecurityProperties.KEYSTORE_FILE_KEY;
import static org.apache.atlas.security.SecurityProperties.TLS_ENABLED;
import static org.apache.atlas.security.SecurityProperties.TRUSTSTORE_FILE_KEY;
import static org.apache.atlas.security.SecurityProperties.CLIENT_AUTH_KEY;
import static org.apache.atlas.security.SecurityProperties.SSL_CLIENT_PROPERTIES;
import org.apache.commons.io.FileUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.atlas.AtlasException;
import org.apache.hadoop.security.alias.CredentialProviderFactory;

/**
 *
 */
public class BaseSecurityTest {
    private static final String JAAS_ENTRY = "%s { \n" + " %s required\n"
            // kerberos module
            + " keyTab=\"%s\"\n" + " debug=true\n" + " principal=\"%s\"\n" + " useKeyTab=true\n"
            + " useTicketCache=false\n" + " doNotPrompt=true\n" + " storeKey=true;\n" + "}; \n";
    protected MiniKdc kdc;

    protected void generateTestProperties(Properties props) throws ConfigurationException, IOException {
        PropertiesConfiguration config =
                new PropertiesConfiguration(System.getProperty("user.dir") +
                  "/../src/conf/" + ApplicationProperties.APPLICATION_PROPERTIES);
        for (String propName : props.stringPropertyNames()) {
            config.setProperty(propName, props.getProperty(propName));
        }
        File file = new File(System.getProperty("user.dir"), ApplicationProperties.APPLICATION_PROPERTIES);
        file.deleteOnExit();
        Writer fileWriter = new FileWriter(file);
        config.save(fileWriter);
    }

    protected void startEmbeddedServer(Server server) throws Exception {
        WebAppContext webapp = new WebAppContext();
        webapp.setContextPath("/");
        webapp.setWar(getWarPath());
        server.setHandler(webapp);

        server.start();
    }

    protected File startKDC() throws Exception {
        File target = Files.createTempDirectory("sectest").toFile();
        File kdcWorkDir = new File(target, "kdc");
        Properties kdcConf = MiniKdc.createConf();
        kdcConf.setProperty(MiniKdc.DEBUG, "true");
        kdc = new MiniKdc(kdcConf, kdcWorkDir);
        kdc.start();

        Assert.assertNotNull(kdc.getRealm());
        return kdcWorkDir;
    }

    public String createJAASEntry(String context, String principal, File keytab) {
        String keytabpath = keytab.getAbsolutePath();
        // fix up for windows; no-op on unix
        keytabpath = keytabpath.replace('\\', '/');
        return String.format(Locale.ENGLISH, JAAS_ENTRY, context, getKerberosAuthModuleForJVM(), keytabpath, principal);
    }

    protected String getKerberosAuthModuleForJVM() {
        if (System.getProperty("java.vendor").contains("IBM")) {
            return "com.ibm.security.auth.module.Krb5LoginModule";
        } else {
            return "com.sun.security.auth.module.Krb5LoginModule";
        }
    }

    protected void bindJVMtoJAASFile(File jaasFile) {
        String path = jaasFile.getAbsolutePath();
        System.setProperty(Environment.JAAS_CONF_KEY, path);
        disableZookeeperSecurity();
    }

    /* We only want Atlas to work in secure mode for the tests
     * for otherwise a lot more configuration is required to
     * make other components like Kafka run in secure mode.
     */
    private void disableZookeeperSecurity() {
        System.setProperty("zookeeper.sasl.client", "false");
        System.setProperty("zookeeper.sasl.clientconfig", "");
    }

    protected File createKeytab(MiniKdc kdc, File kdcWorkDir, String principal, String filename) throws Exception {
        File keytab = new File(kdcWorkDir, filename);
        kdc.createPrincipal(keytab, principal, principal + "/localhost", principal + "/127.0.0.1");
        return keytab;
    }

    protected String getWarPath() {
        return TestUtils.getWarPath();
    }

    protected PropertiesConfiguration getSSLConfiguration(String providerUrl) {
        String projectBaseDirectory = System.getProperty("projectBaseDir");
        final PropertiesConfiguration configuration = new PropertiesConfiguration();
        configuration.setProperty("atlas.services.enabled", false);
        configuration.setProperty(TLS_ENABLED, true);
        configuration.setProperty(TRUSTSTORE_FILE_KEY, projectBaseDirectory + "/webapp/target/atlas.keystore");
        configuration.setProperty(KEYSTORE_FILE_KEY, projectBaseDirectory + "/webapp/target/atlas.keystore");
        configuration.setProperty(CERT_STORES_CREDENTIAL_PROVIDER_PATH, providerUrl);
        configuration.setProperty(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY,
                SSLHostnameVerifier.DEFAULT_AND_LOCALHOST.toString());
        return  configuration;
    }

    public static String writeConfiguration(final PropertiesConfiguration configuration) throws Exception {
        String confLocation = System.getProperty("atlas.conf");
        URL url;
        if (confLocation == null) {
            url = BaseSecurityTest.class.getResource("/" + ApplicationProperties.APPLICATION_PROPERTIES);
        } else {
            url = new File(confLocation, ApplicationProperties.APPLICATION_PROPERTIES).toURI().toURL();
        }
        PropertiesConfiguration configuredProperties = new PropertiesConfiguration();
        configuredProperties.load(url);

        configuredProperties.copy(configuration);

        String persistDir = TestUtils.getTempDirectory();
        configuredProperties.setProperty("atlas.authentication.method.file", "true");
        configuredProperties.setProperty("atlas.authentication.method.file.filename", persistDir
                + "/users-credentials");
        configuredProperties.setProperty("atlas.auth.policy.file",persistDir
                + "/policy-store.txt" );
        TestUtils.writeConfiguration(configuredProperties, persistDir + File.separator +
                ApplicationProperties.APPLICATION_PROPERTIES);
        setupUserCredential(persistDir);
        setUpPolicyStore(persistDir);
        ApplicationProperties.forceReload();
        return persistDir;
    }

    public static void setupUserCredential(String tmpDir) throws Exception {

        StringBuilder credentialFileStr = new StringBuilder(1024);
        credentialFileStr.append("admin=ADMIN::8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918\n");
        credentialFileStr.append("michael=DATA_SCIENTIST::95bfb24de17d285d734b9eaa9109bfe922adc85f20d2e5e66a78bddb4a4ebddb\n");
        credentialFileStr.append("paul=DATA_STEWARD::e7c0dcf5f8a93e93791e9bac1ae454a691c1d2a902fc4256d489e96c1b9ac68c\n");
        credentialFileStr.append("testuser=DATA_STEWARD::e7c0dcf5f8a93e93791e9bac1ae454a691c1d2a902fc4256d489e96c1b9ac68c\n");
        File credentialFile = new File(tmpDir, "users-credentials");
        FileUtils.write(credentialFile, credentialFileStr.toString());
    }

    public static void setUpPolicyStore(String tmpDir) throws Exception {
        StringBuilder policyStr = new StringBuilder(1024);
        policyStr.append("adminPolicy;;admin:rwud;;ROLE_ADMIN:rwud;;type:*,entity:*,operation:*\n");
        policyStr.append("dataStewardPolicy;;testuser:rwud;;DATA_STEWARD:rwu;;type:*,entity:*\n");

        File policyFile = new File(tmpDir, "policy-store.txt");
        FileUtils.write(policyFile, policyStr.toString());
    }

    public static void persistSSLClientConfiguration(org.apache.commons.configuration.Configuration clientConfig)
            throws AtlasException, IOException {
        //trust settings
        Configuration configuration = new Configuration(false);
        File sslClientFile = getSSLClientFile();
        if (!sslClientFile.exists()) {
            configuration.set("ssl.client.truststore.type", "jks");
            configuration.set("ssl.client.truststore.location", clientConfig.getString(TRUSTSTORE_FILE_KEY));
            if (clientConfig.getBoolean(CLIENT_AUTH_KEY, false)) {
                // need to get client key properties
                configuration.set("ssl.client.keystore.location", clientConfig.getString(KEYSTORE_FILE_KEY));
                configuration.set("ssl.client.keystore.type", "jks");
            }
            // add the configured credential provider
            configuration.set(CredentialProviderFactory.CREDENTIAL_PROVIDER_PATH,
                    clientConfig.getString(CERT_STORES_CREDENTIAL_PROVIDER_PATH));
            String hostnameVerifier = clientConfig.getString(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY);
            if (hostnameVerifier != null) {
                configuration.set(SSLFactory.SSL_HOSTNAME_VERIFIER_KEY, hostnameVerifier);
            }

            configuration.writeXml(new FileWriter(sslClientFile));
        }
    }

    private static File getSSLClientFile() throws AtlasException {
        File sslDir;
        try {
            String persistDir = null;
            URL resource = BaseSecurityTest.class.getResource("/");
            if (resource != null) {
                persistDir = resource.toURI().getPath();
            }
            assert persistDir != null;
            sslDir = new File(persistDir);

            // LOG.info("ssl-client.xml will be created in {}", sslDir);
        } catch (Exception e) {
            throw new AtlasException("Failed to find client configuration directory", e);
        }
        return new File(sslDir, SSL_CLIENT_PROPERTIES);
    }
}
