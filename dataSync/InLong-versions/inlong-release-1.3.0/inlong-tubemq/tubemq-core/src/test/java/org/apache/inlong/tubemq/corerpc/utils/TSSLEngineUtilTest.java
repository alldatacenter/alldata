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

import java.io.InputStream;

import javax.net.ssl.SSLEngine;

import org.junit.Assert;

/**
 * TSSLEngineUtil test.
 */
public class TSSLEngineUtilTest {

    @org.junit.Test
    public void createSSLEngine() {
        // key store file path
        InputStream keyStoreStream = this.getClass().getResourceAsStream("tubeServer.keystore");
        // key store file password
        String keyStorePassword = "tubeserver";
        // trust store file path
        InputStream trustStoreStream = this.getClass()
            .getResourceAsStream("tubeServerTrustStore.keystore");
        // trust store file password
        String trustStorePassword = "tubeserver";
        SSLEngine sslEngine = null;
        try {
            // create engine
            sslEngine = TSSLEngineUtil.createSSLEngine(keyStoreStream, keyStorePassword,
                trustStoreStream, trustStorePassword, true, false);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        Assert.assertNotNull(sslEngine);
        boolean needClientAuth = sslEngine.getNeedClientAuth();
        Assert.assertFalse(needClientAuth);
    }
}
