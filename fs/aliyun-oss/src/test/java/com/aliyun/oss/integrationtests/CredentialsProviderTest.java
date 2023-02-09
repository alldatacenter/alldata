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

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.*;
import com.aliyun.oss.model.OSSObject;
import junit.framework.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.Date;


/**
 * Test CredentialsProvider
 */
public class CredentialsProviderTest extends TestBase {

    private BasicCredentials credentials;


    public void setUp() throws Exception {
        super.setUp();

        CredentialsProvider credentialsProvider = CredentialsProviderFactory
                .newSTSAssumeRoleSessionCredentialsProvider(TestConfig.RAM_REGION, TestConfig.RAM_ACCESS_KEY_ID,
                        TestConfig.RAM_ACCESS_KEY_SECRET, TestConfig.RAM_ROLE_ARN)
                .withExpiredDuration(900);

        credentials = (BasicCredentials) credentialsProvider.getCredentials();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void testEcsRamRoleCredentialsProvider() {
        String NORMAL_METADATA = "{" + "\"AccessKeyId\" : \"" + credentials.getAccessKeyId() + "\","
                + "\"AccessKeySecret\" : \"" + credentials.getSecretAccessKey() + "\","
                + "\"Expiration\" : \"2022-11-11T16:10:03Z\","
                + "\"SecurityToken\" : \"" + credentials.getSecurityToken() + "\","
                + "\"Code\" : \"Success\"" + "}";

        String key = "EcsRamRole";

        ossClient.putObject(bucketName, key, new ByteArrayInputStream(NORMAL_METADATA.getBytes()));

        Date expiration = new Date(new Date().getTime() + 3600 * 1000);
        URL url = ossClient.generatePresignedUrl(bucketName, key,expiration);

        CredentialsProvider provider = new EcsRamRoleCredentialsProvider(url.toString());

        OSSClient  client = new OSSClient(TestConfig.OSS_TEST_ENDPOINT, provider, new ClientConfiguration());

        OSSObject obj = client.getObject(bucketName, key);

        Assert.assertEquals(obj.getObjectMetadata().getContentLength(), NORMAL_METADATA.length());
    }

    @Test
    public void testCustomSessionCredentialsProvider() {

        String NORMAL_METADATA = "{" + "\"AccessKeyId\" : \"" + credentials.getAccessKeyId() + "\","
                + "\"AccessKeySecret\" : \"" + credentials.getSecretAccessKey() + "\","
                + "\"Expiration\" : \"2022-11-11T16:10:03Z\","
                + "\"SecurityToken\" : \"" + credentials.getSecurityToken() + "\","
                + "\"StatusCode\" : \"200\"" + "}";

        String key = "CustomSession";

        ossClient.putObject(bucketName, key, new ByteArrayInputStream(NORMAL_METADATA.getBytes()));

        Date expiration = new Date(new Date().getTime() + 3600 * 1000);
        URL url = ossClient.generatePresignedUrl(bucketName, key,expiration);

        CredentialsProvider provider = new CustomSessionCredentialsProvider(url.toString());

        OSSClient  client = new OSSClient(TestConfig.OSS_TEST_ENDPOINT, provider, new ClientConfiguration());

        OSSObject obj = client.getObject(bucketName, key);

        Assert.assertEquals(obj.getObjectMetadata().getContentLength(), NORMAL_METADATA.length());
    }
}
