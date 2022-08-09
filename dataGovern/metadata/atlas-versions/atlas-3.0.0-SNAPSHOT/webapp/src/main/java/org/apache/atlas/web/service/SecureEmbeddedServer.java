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

package org.apache.atlas.web.service;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasException;
import org.apache.atlas.security.SecurityUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.alias.CredentialProvider;
import org.apache.hadoop.security.alias.CredentialProviderFactory;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import static org.apache.atlas.security.SecurityProperties.ATLAS_SSL_EXCLUDE_CIPHER_SUITES;
import static org.apache.atlas.security.SecurityProperties.CERT_STORES_CREDENTIAL_PROVIDER_PATH;
import static org.apache.atlas.security.SecurityProperties.CLIENT_AUTH_KEY;
import static org.apache.atlas.security.SecurityProperties.DEFATULT_TRUSTORE_FILE_LOCATION;
import static org.apache.atlas.security.SecurityProperties.DEFAULT_CIPHER_SUITES;
import static org.apache.atlas.security.SecurityProperties.DEFAULT_KEYSTORE_FILE_LOCATION;
import static org.apache.atlas.security.SecurityProperties.KEYSTORE_FILE_KEY;
import static org.apache.atlas.security.SecurityProperties.KEYSTORE_PASSWORD_KEY;
import static org.apache.atlas.security.SecurityProperties.SERVER_CERT_PASSWORD_KEY;
import static org.apache.atlas.security.SecurityProperties.TRUSTSTORE_FILE_KEY;
import static org.apache.atlas.security.SecurityProperties.TRUSTSTORE_PASSWORD_KEY;
import static org.apache.atlas.security.SecurityProperties.ATLAS_SSL_EXCLUDE_PROTOCOLS;
import static org.apache.atlas.security.SecurityProperties.DEFAULT_EXCLUDE_PROTOCOLS;
import static org.apache.atlas.security.SecurityProperties.KEYSTORE_TYPE;
import static org.apache.atlas.security.SecurityProperties.TRUSTSTORE_TYPE;
import static org.apache.atlas.security.SecurityUtil.getPassword;


/**
 * This is a jetty server which requires client auth via certificates.
 */
public class SecureEmbeddedServer extends EmbeddedServer {

    private static final Logger LOG = LoggerFactory.getLogger(SecureEmbeddedServer.class);

    public static final String ATLAS_KEYSTORE_FILE_TYPE_DEFAULT = "jks";
    public static final String ATLAS_TRUSTSTORE_FILE_TYPE_DEFAULT = "jks";
    public static final String ATLAS_TLS_CONTEXT_ALGO_TYPE = "TLS";
    public static final String ATLAS_TLS_KEYMANAGER_DEFAULT_ALGO_TYPE = KeyManagerFactory.getDefaultAlgorithm();
    public static final String ATLAS_TLS_TRUSTMANAGER_DEFAULT_ALGO_TYPE = TrustManagerFactory.getDefaultAlgorithm();



    public SecureEmbeddedServer(String host, int port, String path) throws IOException {
        super(host, port, path);
    }

    @Override
    protected Connector getConnector(String host, int port) throws IOException {
        org.apache.commons.configuration.Configuration config = getConfiguration();

        SSLContext sslContext = getSSLContext();
        if (sslContext != null) {
            SSLContext.setDefault(sslContext);
        }

        SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
        sslContextFactory.setKeyStoreType(config.getString(KEYSTORE_TYPE , ATLAS_KEYSTORE_FILE_TYPE_DEFAULT));
        sslContextFactory.setKeyStorePath(config.getString(KEYSTORE_FILE_KEY,
                System.getProperty(KEYSTORE_FILE_KEY, DEFAULT_KEYSTORE_FILE_LOCATION)));
        sslContextFactory.setKeyStorePassword(getPassword(config, KEYSTORE_PASSWORD_KEY));
        sslContextFactory.setKeyManagerPassword(getPassword(config, SERVER_CERT_PASSWORD_KEY));
        sslContextFactory.setTrustStoreType(config.getString(TRUSTSTORE_TYPE , ATLAS_TRUSTSTORE_FILE_TYPE_DEFAULT));
        sslContextFactory.setTrustStorePath(config.getString(TRUSTSTORE_FILE_KEY,
                System.getProperty(TRUSTSTORE_FILE_KEY, DEFATULT_TRUSTORE_FILE_LOCATION)));
        sslContextFactory.setTrustStorePassword(getPassword(config, TRUSTSTORE_PASSWORD_KEY));
        sslContextFactory.setWantClientAuth(config.getBoolean(CLIENT_AUTH_KEY, Boolean.getBoolean(CLIENT_AUTH_KEY)));

        List<Object> cipherList = config.getList(ATLAS_SSL_EXCLUDE_CIPHER_SUITES, DEFAULT_CIPHER_SUITES);
        sslContextFactory.setExcludeCipherSuites(cipherList.toArray(new String[cipherList.size()]));
        sslContextFactory.setRenegotiationAllowed(false);

        String[] excludedProtocols = config.containsKey(ATLAS_SSL_EXCLUDE_PROTOCOLS) ?
                config.getStringArray(ATLAS_SSL_EXCLUDE_PROTOCOLS) : DEFAULT_EXCLUDE_PROTOCOLS;
        if (excludedProtocols != null && excludedProtocols.length > 0) {
            sslContextFactory.addExcludeProtocols(excludedProtocols);
        }

        // SSL HTTP Configuration
        // HTTP Configuration
        HttpConfiguration http_config = new HttpConfiguration();
        http_config.setSecureScheme("https");
        final int bufferSize = AtlasConfiguration.WEBSERVER_REQUEST_BUFFER_SIZE.getInt();
        http_config.setSecurePort(port);
        http_config.setRequestHeaderSize(bufferSize);
        http_config.setResponseHeaderSize(bufferSize);
        http_config.setSendServerVersion(false);
        http_config.setSendDateHeader(false);

        HttpConfiguration https_config = new HttpConfiguration(http_config);
        https_config.addCustomizer(new SecureRequestCustomizer());
        https_config.setSendServerVersion(false);

        // SSL Connector
        ServerConnector sslConnector = new ServerConnector(server,
            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
            new HttpConnectionFactory(https_config));
        sslConnector.setPort(port);
        server.addConnector(sslConnector);

        return sslConnector;
    }



    /**
     * Returns the application configuration.
     * @return
     */
    protected org.apache.commons.configuration.Configuration getConfiguration() {
        try {
            return ApplicationProperties.get();
        } catch (AtlasException e) {
            throw new RuntimeException("Unable to load configuration: " + ApplicationProperties.APPLICATION_PROPERTIES);
        }
    }

    /**
     * creates the SSLContext using the available keystore and truststore.
     * @return
     */
    private SSLContext getSSLContext() {
        KeyManager[] kmList = getKeyManagers();
        TrustManager[] tmList = getTrustManagers();
        SSLContext sslContext = null;
        if (tmList != null) {
            try {
                sslContext = SSLContext.getInstance(ATLAS_TLS_CONTEXT_ALGO_TYPE);
                sslContext.init(kmList, tmList, new SecureRandom());
            } catch (NoSuchAlgorithmException e) {
                LOG.error("SSL algorithm is not available in the environment. Reason: " + e.toString());
            } catch (KeyManagementException e) {
                LOG.error("Unable to initials the SSLContext. Reason: " + e.toString());
            }
        }
        return sslContext;
    }

    /**
     * Generating the KeyManager using the provided keystore.
     * @return
     */
    private KeyManager[] getKeyManagers() {
        KeyManager[] kmList = null;
        try {

            String keyStoreFile = getConfiguration().getString(KEYSTORE_FILE_KEY,
              System.getProperty(KEYSTORE_FILE_KEY, DEFAULT_KEYSTORE_FILE_LOCATION));
            String keyStoreFilepwd = getPassword(getConfiguration(), KEYSTORE_PASSWORD_KEY);

            if (StringUtils.isNotEmpty(keyStoreFile) && StringUtils.isNotEmpty(keyStoreFilepwd)) {
                InputStream in = null;

                try {
                    in = getFileInputStream(keyStoreFile);

                    if (in != null) {
                        KeyStore keyStore = KeyStore.getInstance(getConfiguration().getString(KEYSTORE_TYPE , ATLAS_KEYSTORE_FILE_TYPE_DEFAULT));

                        keyStore.load(in, keyStoreFilepwd.toCharArray());

                        KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(ATLAS_TLS_KEYMANAGER_DEFAULT_ALGO_TYPE);

                        keyManagerFactory.init(keyStore, keyStoreFilepwd.toCharArray());

                        kmList = keyManagerFactory.getKeyManagers();
                    } else {
                        LOG.error("Unable to obtain keystore from file [" + keyStoreFile + "]");
                    }
                } catch (KeyStoreException e) {
                    LOG.error("Unable to obtain from KeyStore :" + e.getMessage(), e);
                } catch (NoSuchAlgorithmException e) {
                    LOG.error("SSL algorithm is NOT available in the environment", e);
                } catch (CertificateException e) {
                    LOG.error("Unable to obtain the requested certification ", e);
                } catch (FileNotFoundException e) {
                    LOG.error("Unable to find the necessary TLS Keystore Files", e);
                } catch (IOException e) {
                    LOG.error("Unable to read the necessary TLS Keystore Files", e);
                } catch (UnrecoverableKeyException e) {
                    LOG.error("Unable to recover the key from keystore", e);
                } finally {
                    close(in, keyStoreFile);
                }
            }

        }catch (IOException exception) {
            LOG.error(exception.getMessage());
        }
        return kmList;
    }

    /**
     * Generating the TrustManager using the provided truststore
     * @return
     */
    private TrustManager[] getTrustManagers() {
        TrustManager[] tmList = null;
        try {
            String truststoreFile = getConfiguration().getString(TRUSTSTORE_FILE_KEY,
              System.getProperty(TRUSTSTORE_FILE_KEY, DEFATULT_TRUSTORE_FILE_LOCATION));
            String trustStoreFilepwd = getPassword(getConfiguration(), TRUSTSTORE_PASSWORD_KEY);

            if (StringUtils.isNotEmpty(truststoreFile) && StringUtils.isNotEmpty(trustStoreFilepwd)) {
                InputStream in = null;

                try {
                    in = getFileInputStream(truststoreFile);

                    if (in != null) {
                        KeyStore trustStore = KeyStore.getInstance(getConfiguration().getString(TRUSTSTORE_TYPE , ATLAS_TRUSTSTORE_FILE_TYPE_DEFAULT));

                        trustStore.load(in, trustStoreFilepwd.toCharArray());

                        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(ATLAS_TLS_TRUSTMANAGER_DEFAULT_ALGO_TYPE);

                        trustManagerFactory.init(trustStore);

                        tmList = trustManagerFactory.getTrustManagers();
                    } else {
                        LOG.error("Unable to obtain truststore from file [" + truststoreFile + "]");
                    }
                } catch (KeyStoreException e) {
                    LOG.error("Unable to obtain from KeyStore", e);
                } catch (NoSuchAlgorithmException e) {
                    LOG.error("SSL algorithm is NOT available in the environment :" + e.getMessage(), e);
                } catch (CertificateException e) {
                    LOG.error("Unable to obtain the requested certification :" + e.getMessage(), e);
                } catch (FileNotFoundException e) {
                    LOG.error("Unable to find the necessary TLS TrustStore File:" + truststoreFile, e);
                } catch (IOException e) {
                    LOG.error("Unable to read the necessary TLS TrustStore Files :" + truststoreFile, e);
                } finally {
                    close(in, truststoreFile);
                }
            }

        }catch (IOException exception) {
            LOG.error(exception.getMessage());
        }
        return tmList;
    }

    private InputStream getFileInputStream(String fileName) throws IOException {
        InputStream in = null;
        if (StringUtils.isNotEmpty(fileName)) {
            File f = new File(fileName);
            if (f.exists()) {
                in = new FileInputStream(f);
            } else {
                in = ClassLoader.getSystemResourceAsStream(fileName);
            }
        }
        return in;
    }

    /**
     * Closing file-stream.
     * @param str
     * @param filename
     */
    private void close(InputStream str, String filename) {
        if (str != null) {
            try {
                str.close();
            } catch (IOException excp) {
                LOG.error("Error while closing file: [" + filename + "]", excp);
            }
        }
    }
}
