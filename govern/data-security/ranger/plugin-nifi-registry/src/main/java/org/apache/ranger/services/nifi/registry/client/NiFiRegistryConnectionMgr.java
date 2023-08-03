/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.ranger.services.nifi.registry.client;


import org.apache.commons.lang.StringUtils;
import org.apache.ranger.plugin.client.BaseClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;

/**
 * Creates a NiFiRegistryClient and provides method to test a connection to NiFi Registry.
 */
public class NiFiRegistryConnectionMgr {

    private static final Logger LOG = LoggerFactory.getLogger(NiFiRegistryConnectionMgr.class);
    private static final String SSL_ALGORITHM = "TLSv1.2";

    private static final String API_RESOURCES_PATH = "/nifi-registry-api/policies/resources";
    static final String INVALID_URL_MSG =  "NiFi Registry URL must be a valid URL of the form " +
            "http(s)://<hostname>(:<port>)" + API_RESOURCES_PATH;


    static public NiFiRegistryClient getNiFiRegistryClient(String serviceName, Map<String, String> configs) throws Exception {
        final String url = configs.get(NiFiRegistryConfigs.NIFI_REG_URL);
        validateNotBlank(url, "NiFi Registry URL is required for " + serviceName);
        validateUrl(url);

        final String authTypeStr = configs.get(NiFiRegistryConfigs.NIFI_REG_AUTHENTICATION_TYPE);
        validateNotBlank(authTypeStr, "Authentication Type is required for " + serviceName);

        final NiFiRegistryAuthType authType = NiFiRegistryAuthType.valueOf(authTypeStr);
        if (LOG.isDebugEnabled()) {
            LOG.debug("NiFiRegistryAuthType is " + authType.name());
        }

        SSLContext sslContext = null;

        if (authType == NiFiRegistryAuthType.SSL) {

            if (!url.startsWith("https")) {
                throw new IllegalArgumentException("Authentication Type of SSL requires an https URL");
            }

            final String keystore = configs.get(NiFiRegistryConfigs.NIFI_REG_SSL_KEYSTORE);
            final String keystoreType = configs.get(NiFiRegistryConfigs.NIFI_REG_SSL_KEYSTORE_TYPE);
            final String keystorePassword = configs.get(NiFiRegistryConfigs.NIFI_REG_SSL_KEYSTORE_PASSWORD);

            final String truststore = configs.get(NiFiRegistryConfigs.NIFI_REG_SSL_TRUSTSTORE);
            final String truststoreType = configs.get(NiFiRegistryConfigs.NIFI_REG_SSL_TRUSTSTORE_TYPE);
            final String truststorePassword = configs.get(NiFiRegistryConfigs.NIFI_REG_SSL_TRUSTSTORE_PASSWORD);

            final String useDefaultSSLContext = configs.get(NiFiRegistryConfigs.NIFI_REG_SSL_USER_DEFAULT_CONTEXT);

            if (!StringUtils.isBlank(useDefaultSSLContext) && "true".equalsIgnoreCase(useDefaultSSLContext)) {

                if (!StringUtils.isBlank(keystore) || !StringUtils.isBlank(keystoreType) || !StringUtils.isBlank(keystorePassword)
                        || !StringUtils.isBlank(truststore) || !StringUtils.isBlank(truststoreType) || !StringUtils.isBlank(truststorePassword)) {
                    throw new IllegalArgumentException("Keystore and Truststore configuration cannot be provided when using default SSL context");
                }

                sslContext = SSLContext.getDefault();
            } else {

                validateNotBlank(keystore, "Keystore is required for " + serviceName + " with Authentication Type of SSL");
                validateNotBlank(keystoreType, "Keystore Type is required for " + serviceName + " with Authentication Type of SSL");
                validateNotBlank(keystorePassword, "Keystore Password is required for " + serviceName + " with Authentication Type of SSL");

                validateNotBlank(truststore, "Truststore is required for " + serviceName + " with Authentication Type of SSL");
                validateNotBlank(truststoreType, "Truststore Type is required for " + serviceName + " with Authentication Type of SSL");
                validateNotBlank(truststorePassword, "Truststore Password is required for " + serviceName + " with Authentication Type of SSL");

                LOG.debug("Creating SSLContext for NiFi Registry connection");

                sslContext = createSslContext(
                        keystore.trim(),
                        keystorePassword.trim().toCharArray(),
                        keystoreType.trim(),
                        truststore.trim(),
                        truststorePassword.trim().toCharArray(),
                        truststoreType.trim(),
                        SSL_ALGORITHM);
            }
        }

        return new NiFiRegistryClient(url.trim(), sslContext);
    }

    public static HashMap<String, Object> connectionTest(String serviceName, Map<String, String> configs) throws Exception {
        NiFiRegistryClient client;
        try {
            client = getNiFiRegistryClient(serviceName, configs);
        } catch (Exception e) {
            final HashMap<String,Object> ret = new HashMap<>();
            BaseClient.generateResponseDataMap(false, "Error creating NiFi Registry client", e.getMessage(), null, null, ret);
            return ret;
        }

        return client.connectionTest();
    }

    private static void validateNotBlank(final String input, final String message) {
        if (input == null || input.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    private static void validateUrl(String url) {
        URI nifiUri;
        try {
            nifiUri = new URI(url);
            if (!nifiUri.getPath().endsWith(API_RESOURCES_PATH)) {
                throw new IllegalArgumentException(INVALID_URL_MSG);
            }
        } catch (URISyntaxException urie) {
            throw new IllegalArgumentException(INVALID_URL_MSG);
        }
    }

    private static SSLContext createSslContext(
            final String keystore, final char[] keystorePasswd, final String keystoreType,
            final String truststore, final char[] truststorePasswd, final String truststoreType,
            final String protocol)
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException,
            UnrecoverableKeyException, KeyManagementException {

        // prepare the keystore
        final KeyStore keyStore = KeyStore.getInstance(keystoreType);
        try (final InputStream keyStoreStream = new FileInputStream(keystore)) {
            keyStore.load(keyStoreStream, keystorePasswd);
        }
        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keystorePasswd);

        // prepare the truststore
        final KeyStore trustStore = KeyStore.getInstance(truststoreType);
        try (final InputStream trustStoreStream = new FileInputStream(truststore)) {
            trustStore.load(trustStoreStream, truststorePasswd);
        }
        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        // initialize the ssl context
        final SSLContext sslContext = SSLContext.getInstance(protocol);
        sslContext.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
        return sslContext;
    }

}
