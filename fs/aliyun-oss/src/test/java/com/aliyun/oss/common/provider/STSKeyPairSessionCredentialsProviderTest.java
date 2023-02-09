/*
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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.common.provider;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.BasicCredentials;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.PublicKey;
import com.aliyun.oss.common.utils.AuthUtils;
import com.aliyuncs.exceptions.ClientException;
import junit.framework.Assert;
import org.junit.Test;

public class STSKeyPairSessionCredentialsProviderTest extends TestBase {

    @Test
    public void testStsKeyPairCredentialsProvider() {
        try {
            PublicKey publicKey = AuthUtils.uploadPublicKey(TestConfig.RAM_REGION_ID, TestConfig.ROOT_ACCESS_KEY_ID,
                    TestConfig.ROOT_ACCESS_KEY_SECRET, AuthUtils.loadPublicKeyFromFile(TestConfig.PUBLIC_KEY_PATH));

            CredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newSTSKeyPairSessionCredentialsProvider(TestConfig.RAM_REGION_ID, publicKey.getPublicKeyId(),
                            AuthUtils.loadPrivateKeyFromFile(TestConfig.PRIVATE_KEY_PATH))
                    .withExpiredDuration(900);

            Thread.sleep(2000);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertFalse(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());
            Assert.assertTrue(credentials.getAccessKeyId().startsWith("TMPSK."));
            Assert.assertEquals(credentials.getAccessKeyId().length(), 130);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testStsKeyPairCredentialsProviderExpire() {
        try {
            PublicKey publicKey = AuthUtils.uploadPublicKey(TestConfig.RAM_REGION_ID, TestConfig.ROOT_ACCESS_KEY_ID,
                    TestConfig.ROOT_ACCESS_KEY_SECRET, AuthUtils.loadPublicKeyFromFile(TestConfig.PUBLIC_KEY_PATH));

            CredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newSTSKeyPairSessionCredentialsProvider(TestConfig.RAM_REGION_ID, publicKey.getPublicKeyId(),
                            AuthUtils.loadPrivateKeyFromFile(TestConfig.PRIVATE_KEY_PATH))
                    .withExpiredFactor(0.001).withExpiredDuration(2000);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertFalse(credentials.willSoonExpire());

            Thread.sleep(3000);

            Assert.assertTrue(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testStsKeyPairCredentialsProviderNegative() throws ClientException, IOException {
        try {
            PublicKey publicKey = AuthUtils.uploadPublicKey(TestConfig.RAM_REGION_ID, TestConfig.ROOT_ACCESS_KEY_ID,
                    TestConfig.ROOT_ACCESS_KEY_SECRET, AuthUtils.loadPublicKeyFromFile(TestConfig.PUBLIC_KEY_PATH));

            CredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newSTSKeyPairSessionCredentialsProvider(TestConfig.RAM_REGION_ID, publicKey.getPublicKeyId(),
                            AuthUtils.loadPrivateKeyFromFile(TestConfig.PRIVATE_KEY_PATH))
                    .withExpiredDuration(899);

            Assert.assertNull(credentialsProvider.getCredentials());

            credentialsProvider = CredentialsProviderFactory.newSTSKeyPairSessionCredentialsProvider(TestConfig.RAM_REGION_ID,
                    publicKey.getPublicKeyId(), AuthUtils.loadPrivateKeyFromFile(TestConfig.PRIVATE_KEY_PATH))
                    .withExpiredDuration(100);

            Assert.assertNull(credentialsProvider.getCredentials());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testStsKeyPairCredentialsProviderInOss() {
        try {
            PublicKey publicKey = AuthUtils.uploadPublicKey(TestConfig.RAM_REGION_ID, TestConfig.ROOT_ACCESS_KEY_ID,
                    TestConfig.ROOT_ACCESS_KEY_SECRET, AuthUtils.loadPublicKeyFromFile(TestConfig.PUBLIC_KEY_PATH));

            CredentialsProvider credentialsProvider = CredentialsProviderFactory.newSTSKeyPairSessionCredentialsProvider(
                    TestConfig.RAM_REGION_ID, publicKey.getPublicKeyId(),
                    AuthUtils.loadPrivateKeyFromFile(TestConfig.PRIVATE_KEY_PATH));

            String key = "test.txt";
            String content = "HelloOSS";

            OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_ENDPOINT, credentialsProvider);
            ossClient.putObject(TestConfig.OSS_BUCKET, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
