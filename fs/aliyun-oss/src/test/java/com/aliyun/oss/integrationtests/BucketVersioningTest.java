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
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import junit.framework.Assert;

import org.junit.Test;
import com.aliyun.oss.model.BucketVersioningConfiguration;
import com.aliyun.oss.model.SetBucketVersioningRequest;

import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

public class BucketVersioningTest extends TestBase {

    @Test
    public void testSetBucketVersioning() {
        OSSClient ossClient = null;

        try {
            final String endpoint = TestConfig.OSS_TEST_ENDPOINT;
            final String bucketName = super.bucketName + "-bucket-versioning";

            //create client
            ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
            Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
            ossClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);

            ossClient.createBucket(bucketName);
            waitForCacheExpiration(2);

            // start versioning
            BucketVersioningConfiguration configuration = new BucketVersioningConfiguration();
            configuration.setStatus(BucketVersioningConfiguration.ENABLED);
            SetBucketVersioningRequest request = new SetBucketVersioningRequest(bucketName, configuration);

            ossClient.setBucketVersioning(request);

            BucketVersioningConfiguration versionConfiguration = ossClient.getBucketVersioning(bucketName);
            Assert.assertTrue(versionConfiguration.getStatus().equals(BucketVersioningConfiguration.ENABLED));

            // stop versioning
            configuration.setStatus(BucketVersioningConfiguration.SUSPENDED);
            request = new SetBucketVersioningRequest(bucketName, configuration);

            ossClient.setBucketVersioning(request);

            versionConfiguration = ossClient.getBucketVersioning(bucketName);
            Assert.assertTrue(versionConfiguration.getStatus().equals(BucketVersioningConfiguration.SUSPENDED));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
                ossClient = null;
            }
        }
    }

}
