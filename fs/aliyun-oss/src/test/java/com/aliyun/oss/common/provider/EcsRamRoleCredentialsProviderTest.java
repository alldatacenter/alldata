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

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.common.provider.mock.EcsRamRoleCredentialsFetcherMock;
import com.aliyun.oss.common.provider.mock.EcsRamRoleCredentialsFetcherMock.ResponseCategory;
import com.aliyun.oss.common.utils.DateUtil;
import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.Date;

public class EcsRamRoleCredentialsProviderTest extends TestBase {

    @Test
    public void testGetNormalCredentials() {
        try {
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.Normal);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
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
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.NormalWithoutExpiration);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
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
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.NormalWithoutToken);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
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
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.Expired);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
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
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ServerHalt);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
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
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.Exceptional);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ExceptionalWithoutStatus);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ExceptionalFailStatus);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ExceptionalWithoutAK);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            Credentials credentials = credentialsProvider.getCredentials();
            Assert.assertNull(credentials);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.ExceptionalWithoutSK);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
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
            EcsRamRoleCredentialsFetcher credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.Expired);
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertTrue(credentials.willSoonExpire());

            credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(TestConfig.OSS_AUTH_SERVER_HOST)
                    .withResponseCategory(ResponseCategory.Normal);
            credentialsProvider = new EcsRamRoleCredentialsProvider(TestConfig.ECS_ROLE_NAME)
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
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
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
            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
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

    @Test
    public void testExpireTime() {

        String NORMAL_METADATA_1 = "{" + "\"AccessKeyId\" : \"STS.EgnR2nX****FAf9uuqjHS8Ddt\","
                + "\"AccessKeySecret\" : \"CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
                + "\"SecurityToken\" : \"CAISigJ1q6Ft5B2yfSjIpKTbGYjatahPg6CtQ0CIkXUkZsd/14HPljz2IHBE****AOEetfs2lW1T6P0TlrRtTtpfTEmBbI569s1WqQW+Z5fT5JHo4LZfhoGoRzB9keMGTIyADd/iRfbxJ92PCTmd5AIRrJ****K9JS/HVbSClZ9gaPkOQwC8dkAoLdxKJwxk2qR4XDmrQp****PxhXfKB0dFoxd1jXgFiZ6y2cqB8BHT/jaYo603392ofsj1NJE1ZMglD4nlhbxMG/CfgHIK2X9j77xriaFIwzDDs+yGDkNZixf8aLqEqIM/dV4hPfdjSvMf8qOtj5t1sffJnoHtzBJAIexOT****FVtcH5xchqAAXp1d/dYv+2L+dJDW+2pm1vACD/UlRk93prPkyuU3zH2wnvXBxEi26QnoQSCA+T1yE2wo41V2mS+LSGYN/PC+2Ml1q+JX5DzKgfGrUPt7kU4FeXJDzGh2YaXRGpO7yERKgAc/NukkDNqthMaHntyTeix08DYBuTT6gd3V8XmN8vF\","
                + "\"Code\" : \"Success\","
                + "\"Expiration\" :" ;

        String NORMAL_METADATA_2 = "{" + "\"AccessKeyId\" : \"STS.AgnR2nX****FAf9uuqjHS8Ddt\","
                + "\"AccessKeySecret\" : \"AJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV\","
                + "\"SecurityToken\" : \"AAISigJ1q6Ft5B2yfSjIpKTbGYjatahPg6CtQ0CIkXUkZsd/14HPljz2IHBE****AOEetfs2lW1T6P0TlrRtTtpfTEmBbI569s1WqQW+Z5fT5JHo4LZfhoGoRzB9keMGTIyADd/iRfbxJ92PCTmd5AIRrJ****K9JS/HVbSClZ9gaPkOQwC8dkAoLdxKJwxk2qR4XDmrQp****PxhXfKB0dFoxd1jXgFiZ6y2cqB8BHT/jaYo603392ofsj1NJE1ZMglD4nlhbxMG/CfgHIK2X9j77xriaFIwzDDs+yGDkNZixf8aLqEqIM/dV4hPfdjSvMf8qOtj5t1sffJnoHtzBJAIexOT****FVtcH5xchqAAXp1d/dYv+2L+dJDW+2pm1vACD/UlRk93prPkyuU3zH2wnvXBxEi26QnoQSCA+T1yE2wo41V2mS+LSGYN/PC+2Ml1q+JX5DzKgfGrUPt7kU4FeXJDzGh2YaXRGpO7yERKgAc/NukkDNqthMaHntyTeix08DYBuTT6gd3V8XmN8vF\","
                + "\"Code\" : \"Success\","
                + "\"Expiration\" :" ;

        try {

            EcsRamRoleCredentialsFetcherMock credentialsFetcher = new EcsRamRoleCredentialsFetcherMock(
                    TestConfig.OSS_AUTH_SERVER_HOST).withResponseCategory(ResponseCategory.FromExternal);

            String meta = NORMAL_METADATA_1 + "\"" + DateUtil.formatAlternativeIso8601Date(new Date()) + "\"}";
            credentialsFetcher.setExternalData(meta);

            EcsRamRoleCredentialsProvider credentialsProvider = new EcsRamRoleCredentialsProvider(
                    TestConfig.OSS_AUTH_SERVER_HOST).withCredentialsFetcher(credentialsFetcher);

            BasicCredentials credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId(), "STS.EgnR2nX****FAf9uuqjHS8Ddt");
            Assert.assertEquals(credentials.getSecretAccessKey(), "CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV");
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertTrue(credentials.willSoonExpire());

            meta = NORMAL_METADATA_2 + "\"" + DateUtil.formatAlternativeIso8601Date(new Date()) + "\"}";
            credentialsFetcher.setExternalData(meta);

            credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId(), "STS.AgnR2nX****FAf9uuqjHS8Ddt");
            Assert.assertEquals(credentials.getSecretAccessKey(), "AJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV");
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertTrue(credentials.willSoonExpire());

            long currTime = new Date().getTime() + (3600 * 6 * 15/100 - 100) * 1000;
            meta = NORMAL_METADATA_1 + "\"" + DateUtil.formatAlternativeIso8601Date(new Date(currTime)) + "\"}";
            credentialsFetcher.setExternalData(meta);
            credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId(), "STS.EgnR2nX****FAf9uuqjHS8Ddt");
            Assert.assertEquals(credentials.getSecretAccessKey(), "CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV");
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertTrue(credentials.willSoonExpire());

            currTime = new Date().getTime() + (3600 * 6 * 15/100 + 100) * 1000;
            meta = NORMAL_METADATA_1 + "\"" + DateUtil.formatAlternativeIso8601Date(new Date(currTime)) + "\"}";
            credentialsFetcher.setExternalData(meta);
            credentials = (BasicCredentials) credentialsProvider.getCredentials();
            Assert.assertEquals(credentials.getAccessKeyId(), "STS.EgnR2nX****FAf9uuqjHS8Ddt");
            Assert.assertEquals(credentials.getSecretAccessKey(), "CJ7G63EhuZuN8rfSg2Rd****qAgHMhmDuMkp****NPUV");
            Assert.assertTrue(credentials.useSecurityToken());
            Assert.assertFalse(credentials.willSoonExpire());

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
}
