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

package com.aliyun.oss.integrationtests;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSSEncryptionClient;
import com.aliyun.oss.OSSEncryptionClientBuilder;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.crypto.CryptoConfiguration;
import com.aliyun.oss.crypto.EncryptionMaterials;
import com.aliyun.oss.crypto.SimpleRSAEncryptionMaterials;
import com.aliyun.oss.model.BucketInfo;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;

import junit.framework.Assert;
import org.junit.Test;

public class EncryptionClientBuilderTest extends TestBase {

    private final static String TEST_KEY = "test/test.txt";
    private final static String TEST_CONTENT = "Hello OSS.";
    private static EncryptionMaterials encryptionMaterials;

    public void setUp() throws Exception {
        super.setUp();
        KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("RSA");
        keyGenerator.initialize(512, new SecureRandom());
        KeyPair keyPair = keyGenerator.generateKeyPair();
        Map<String, String> desc = new HashMap<String, String>();
        desc.put("Desc1-Key1", "Desc1-Value1");
        encryptionMaterials = new SimpleRSAEncryptionMaterials(keyPair, desc);
    }

    @Test
    public void testEncryptionClientBuilderWithCredentials() {
        try {
            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().build(
                    TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID,
                    TestConfig.OSS_TEST_ACCESS_KEY_SECRET, encryptionMaterials);

            Assert.assertFalse(ossEncryptionClient.getClientConfiguration().isSupportCname());

            BucketInfo info = ossEncryptionClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(TEST_CONTENT.getBytes().length);
            ossEncryptionClient.putObject(bucketName, TEST_KEY, new ByteArrayInputStream(TEST_CONTENT.getBytes()), metadata);

            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, TEST_KEY);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossEncryptionClient.deleteObject(bucketName, TEST_KEY);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEncryptionClientBuilderWithStsCredentials() {
        try {
            String stsTokent = null;
            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().build(
                    TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID,
                    TestConfig.OSS_TEST_ACCESS_KEY_SECRET, stsTokent, encryptionMaterials);

            Assert.assertFalse(ossEncryptionClient.getClientConfiguration().isSupportCname());

            BucketInfo info = ossEncryptionClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(TEST_CONTENT.getBytes().length);
            ossEncryptionClient.putObject(bucketName, TEST_KEY, new ByteArrayInputStream(TEST_CONTENT.getBytes()), metadata);

            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, TEST_KEY);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossEncryptionClient.deleteObject(bucketName, TEST_KEY);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEncryptionClientBuilderWithCredentialsProvider() {
        try {

            Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID,
                    TestConfig.OSS_TEST_ACCESS_KEY_SECRET);

            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().build(
                    TestConfig.OSS_TEST_ENDPOINT, new DefaultCredentialProvider(credentials), encryptionMaterials);

            Assert.assertFalse(ossEncryptionClient.getClientConfiguration().isSupportCname());

            BucketInfo info = ossEncryptionClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(TEST_CONTENT.getBytes().length);
            ossEncryptionClient.putObject(bucketName, TEST_KEY, new ByteArrayInputStream(TEST_CONTENT.getBytes()), metadata);

            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, TEST_KEY);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossEncryptionClient.deleteObject(bucketName, TEST_KEY);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEncryptionClientBuilderWithNoneSTS() {
        try {
            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().build(
                    TestConfig.OSS_TEST_ENDPOINT, new DefaultCredentialProvider(TestConfig.OSS_TEST_ACCESS_KEY_ID,
                            TestConfig.OSS_TEST_ACCESS_KEY_SECRET, null),
                    encryptionMaterials);

            Assert.assertFalse(ossEncryptionClient.getClientConfiguration().isSupportCname());

            BucketInfo info = ossEncryptionClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(TEST_CONTENT.getBytes().length);
            ossEncryptionClient.putObject(bucketName, TEST_KEY, new ByteArrayInputStream(TEST_CONTENT.getBytes()), metadata);

            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, TEST_KEY);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossEncryptionClient.deleteObject(bucketName, TEST_KEY);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testEncryptionClientBuilderWithSTS() {
        try {
            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().build(
                    TestConfig.OSS_TEST_ENDPOINT, new DefaultCredentialProvider(TestConfig.OSS_TEST_ACCESS_KEY_ID,
                            TestConfig.OSS_TEST_ACCESS_KEY_SECRET, "TOKEN"),
                    encryptionMaterials);

            Assert.assertFalse(ossEncryptionClient.getClientConfiguration().isSupportCname());
            Credentials cred = ossEncryptionClient.getCredentialsProvider().getCredentials();
            Assert.assertEquals(cred.getAccessKeyId(), TestConfig.OSS_TEST_ACCESS_KEY_ID);
            Assert.assertEquals(cred.getSecretAccessKey(), TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
            Assert.assertEquals(cred.getSecurityToken(), "TOKEN");
            Assert.assertTrue(cred.useSecurityToken());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testClientBuilderWithBuilderConfiguration() {
        try {
            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().
                        build(TestConfig.OSS_TEST_ENDPOINT, 
                              new DefaultCredentialProvider(TestConfig.OSS_TEST_ACCESS_KEY_ID,
                                      TestConfig.OSS_TEST_ACCESS_KEY_SECRET),
                              encryptionMaterials,
                              new ClientBuilderConfiguration());

            Assert.assertFalse(ossEncryptionClient.getClientConfiguration().isSupportCname());

            BucketInfo info = ossEncryptionClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(TEST_CONTENT.getBytes().length);
            ossEncryptionClient.putObject(bucketName, TEST_KEY, new ByteArrayInputStream(TEST_CONTENT.getBytes()), metadata);

            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, TEST_KEY);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossEncryptionClient.deleteObject(bucketName, TEST_KEY);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testClientBuilderWithCryptoConfig() {
        try {
            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().
                        build(TestConfig.OSS_TEST_ENDPOINT, 
                              new DefaultCredentialProvider(TestConfig.OSS_TEST_ACCESS_KEY_ID,
                                      TestConfig.OSS_TEST_ACCESS_KEY_SECRET),
                              encryptionMaterials,
                              new CryptoConfiguration());

            Assert.assertFalse(ossEncryptionClient.getClientConfiguration().isSupportCname());

            BucketInfo info = ossEncryptionClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(TEST_CONTENT.getBytes().length);
            ossEncryptionClient.putObject(bucketName, TEST_KEY, new ByteArrayInputStream(TEST_CONTENT.getBytes()), metadata);

            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, TEST_KEY);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossEncryptionClient.deleteObject(bucketName, TEST_KEY);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testClientBuilderWithAll() {
        try {
            ClientBuilderConfiguration clientConfig = new ClientBuilderConfiguration();
            clientConfig.setSupportCname(true);
            clientConfig.setConnectionTimeout(10000);
            CryptoConfiguration cryptoConfig =  new CryptoConfiguration();
            
            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().
                    build(TestConfig.OSS_TEST_ENDPOINT, 
                          new DefaultCredentialProvider(TestConfig.OSS_TEST_ACCESS_KEY_ID,
                                  TestConfig.OSS_TEST_ACCESS_KEY_SECRET),
                          encryptionMaterials,
                          clientConfig,
                          cryptoConfig);
            
            Assert.assertTrue(ossEncryptionClient.getClientConfiguration().isSupportCname());
            Assert.assertEquals(ossEncryptionClient.getClientConfiguration().getConnectionTimeout(), 10000);

            BucketInfo info = ossEncryptionClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(TEST_CONTENT.getBytes().length);
            ossEncryptionClient.putObject(bucketName, TEST_KEY, new ByteArrayInputStream(TEST_CONTENT.getBytes()), metadata);

            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, TEST_KEY);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossEncryptionClient.deleteObject(bucketName, TEST_KEY);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testClientBuilderWithConfigNull() {
        try {
            OSSEncryptionClient ossEncryptionClient = new OSSEncryptionClientBuilder().
                    build(TestConfig.OSS_TEST_ENDPOINT, 
                          new DefaultCredentialProvider(TestConfig.OSS_TEST_ACCESS_KEY_ID,
                                  TestConfig.OSS_TEST_ACCESS_KEY_SECRET),
                          encryptionMaterials,
                          null,
                          null);

            Assert.assertFalse(ossEncryptionClient.getClientConfiguration().isSupportCname());

            BucketInfo info = ossEncryptionClient.getBucketInfo(bucketName);
            Assert.assertEquals(info.getBucket().getName(), bucketName);

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(TEST_CONTENT.getBytes().length);
            ossEncryptionClient.putObject(bucketName, TEST_KEY, new ByteArrayInputStream(TEST_CONTENT.getBytes()), metadata);

            OSSObject ossObject = ossEncryptionClient.getObject(bucketName, TEST_KEY);
            InputStream inputStream = ossObject.getObjectContent();
            inputStream.close();

            ossEncryptionClient.deleteObject(bucketName, TEST_KEY);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
