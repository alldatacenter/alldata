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

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.BasicCredentials;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.CustomSessionCredentialsFetcher;
import com.aliyun.oss.common.auth.CustomSessionCredentialsProvider;
import com.aliyun.oss.common.provider.mock.CustomSessionCredentialsFetcherMock;
import com.aliyun.oss.common.provider.mock.CustomSessionCredentialsFetcherMock.ResponseCategory;
import junit.framework.Assert;
import org.junit.Test;

public class CustomSessionCredentialsProviderTest extends TestBase {

    @Test
    public void testGetNormalCredentials() {
        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.Normal);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 536);
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetNormalWithoutExpirationCredentials() {
        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.NormalWithoutExpiration);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertFalse(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetNormalWithoutTokenCredentials() {
        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.NormalWithoutToken);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertFalse(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetExpiredCredentials() {
        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.Expired);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 536);
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertTrue(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetCredentialsServerHalt() {
        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ServerHalt);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetExceptionalCredentials() {
        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.Exceptional);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ExceptionalWithoutStatus);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ExceptionalFailStatus);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ExceptionalWithoutAK);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ExceptionalWithoutSK);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRefreshCredentials() {
        try {
            CustomSessionCredentialsFetcher credentialsFetcher = new CustomSessionCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.Expired);
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertTrue(credentials.willSoonExpire());

            credentialsFetcher = new CustomSessionCredentialsFetcherMock(TestConfig.OSS_AUTH_SERVER_HOST)
                    .withResponseCategory(ResponseCategory.Normal);
            credentialsProvider = new CustomSessionCredentialsProvider(TestConfig.ECS_ROLE_NAME)
                    .withCredentialsFetcher(credentialsFetcher);

            credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertFalse(credentials.willSoonExpire());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetCredentialsNegative() {
        try {
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST + "/noteixst");
            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    public void testGetCredentialsFromAuthInOss() {
        try {
            CustomSessionCredentialsProvider credentialsProvider = new CustomSessionCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST);
            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId().length(), 29);
            Assert.assertEquals(credentials.getSecretAccessKey().length(), 44);
            Assert.assertEquals(credentials.getSecurityToken().length(), 516);
            Assert.assertTrue(credentials.useSecurityToken());

            String key = "test.txt";
            String content = "HelloOSS";
            String bucketName = getRandomBucketName();

            OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_ENDPOINT, credentials.getAccessKeyId(),
                    credentials.getSecretAccessKey(), credentials.getSecurityToken());
            ossClient.createBucket(bucketName);
            waitForCacheExpiration(2);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.deleteObject(bucketName, key);
            ossClient.deleteBucket(bucketName);
            ossClient.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    public void testGetCredentialsUseInOss() {
        try {
            CustomSessionCredentialsProvider credentialsProvider = CredentialsProviderFactory
                    .newCustomSessionCredentialsProvider(TestConfig.OSS_AUTH_SERVER_HOST);

            String key = "test.txt";
            String content = "HelloOSS";
            String bucketName = getRandomBucketName();

            OSS ossClient = new OSSClientBuilder().build(TestConfig.OSS_ENDPOINT, credentialsProvider);
            ossClient.createBucket(bucketName);
            waitForCacheExpiration(2);
            ossClient.putObject(bucketName, key, new ByteArrayInputStream(content.getBytes()));
            ossClient.deleteObject(bucketName, key);
            ossClient.deleteBucket(bucketName);
            ossClient.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
