/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.corerpc.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.TrustManagerFactory;

/**
 * SSL-related general function classes
 */
public class TSSLEngineUtil {

    /**
     * Create a SSL engine
     *
     * @param keyStoreStream        the key-store stream
     * @param keyStorePassword      the key-sored password
     * @param trustStoreStream      the trust-store stream
     * @param trustStorePassword    the trust-store password
     * @param isClientMode          whether client mode
     * @param needTwoWayAuth        Whether require two-way authentication
     * @return                      the SSL engine
     * @throws Exception            the exception information while creating
     */
    public static SSLEngine createSSLEngine(InputStream keyStoreStream,
                                            String keyStorePassword,
                                            InputStream trustStoreStream,
                                            String trustStorePassword,
                                            boolean isClientMode,
                                            boolean needTwoWayAuth)
        throws Exception {

        KeyManagerFactory kmf = null;
        TrustManagerFactory tmf = null;
        if (isClientMode || needTwoWayAuth) {
            KeyStore ts = KeyStore.getInstance("JKS");
            try {
                ts.load(trustStoreStream, trustStorePassword.toCharArray());
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ts);
            } finally {
                if (trustStoreStream != null) {
                    trustStoreStream.close();
                }
            }

        }
        if (!isClientMode || needTwoWayAuth) {
            KeyStore ks = KeyStore.getInstance("JKS");
            try {
                ks.load(keyStoreStream, keyStorePassword.toCharArray());
                kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(ks, keyStorePassword.toCharArray());
            } finally {
                if (keyStoreStream != null) {
                    keyStoreStream.close();
                }
            }
        }

        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(kmf == null ? null : kmf.getKeyManagers(),
            tmf == null ? null : tmf.getTrustManagers(), null);
        SSLEngine sslEngine = serverContext.createSSLEngine();
        sslEngine.setUseClientMode(isClientMode);
        sslEngine.setNeedClientAuth(needTwoWayAuth);

        return sslEngine;
    }

    /**
     * Create a SSL engine
     *
     * @param keyStorePath           the key-store file path
     * @param trustStorePath         the trust-store file path
     * @param keyStorePassword       the key-store password
     * @param trustStorePassword     the trust-store password
     * @param isClientMode          whether client mode
     * @param needTwoWayAuth        Whether require two-way authentication
     * @return                      the SSL engine
     * @throws Exception            the exception information while creating
     */
    public static SSLEngine createSSLEngine(String keyStorePath, String trustStorePath,
                                            String keyStorePassword, String trustStorePassword,
                                            boolean isClientMode, boolean needTwoWayAuth)
        throws Exception {

        InputStream keyStoreStream = new FileInputStream(new File(keyStorePath));
        InputStream trustStoreStream = new FileInputStream(new File(trustStorePath));

        return createSSLEngine(keyStoreStream, keyStorePassword, trustStoreStream,
            trustStorePassword, isClientMode, needTwoWayAuth);
    }
}
