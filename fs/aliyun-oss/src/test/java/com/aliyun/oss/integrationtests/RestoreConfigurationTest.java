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
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.junit.Test;
import java.io.ByteArrayInputStream;

public class RestoreConfigurationTest extends TestBase {
    private OSSClient ossClient;
    private String bucketName;
    private String endpoint;

    @Override
    public void setUp() throws Exception {
        super.setUp();

        bucketName = super.bucketName + "-cold";
        endpoint = "http://oss-ap-southeast-2.aliyuncs.com";

        //create client
        ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        ossClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);

        ossClient.createBucket(bucketName);
        Thread.sleep(2000);
    }

    public void tearDown() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
            ossClient = null;
        }
        super.tearDown();
    }

    @Test
    public void testRestoreObjectWithConfig() {
        try {
            String prefix = "test-restore-with-config";
            for (RestoreTier restoreTier : RestoreTier.values()) {
                String objectName = prefix + restoreTier.toString() + ".txt";
                PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName,
                        new ByteArrayInputStream("123".getBytes()));

                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.ColdArchive.toString());
                putObjectRequest.setMetadata(metadata);
                ossClient.putObject(putObjectRequest);

                metadata = ossClient.getObjectMetadata(bucketName, objectName);
                Assert.assertEquals(StorageClass.ColdArchive, metadata.getObjectStorageClass());

                RestoreJobParameters jobParameters = new RestoreJobParameters(restoreTier);
                RestoreConfiguration configuration = new RestoreConfiguration(5, jobParameters);
                ossClient.restoreObject(bucketName, objectName, configuration);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRestoreObjectWithNoneJobParameter() {
        try {
            String objectName = "test-restore-with-none-job-parameter";
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName,
                    new ByteArrayInputStream("123".getBytes()));

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.ColdArchive.toString());
            putObjectRequest.setMetadata(metadata);
            ossClient.putObject(putObjectRequest);

            metadata = ossClient.getObjectMetadata(bucketName, objectName);
            Assert.assertEquals(StorageClass.ColdArchive, metadata.getObjectStorageClass());

            RestoreConfiguration configuration = new RestoreConfiguration(5);

            RestoreObjectRequest restoreObjectRequest = new RestoreObjectRequest(bucketName, objectName)
                    .withRestoreConfiguration(configuration);
            ossClient.restoreObject(restoreObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void testRestoreArchiveWithJobParameters() {
        String objectName = "test-restore-archive-object-with-job-parameters";
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectName,
                    new ByteArrayInputStream("123".getBytes()));

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Archive.toString());
            putObjectRequest.setMetadata(metadata);
            ossClient.putObject(putObjectRequest);

            metadata = ossClient.getObjectMetadata(bucketName, objectName);
            Assert.assertEquals(StorageClass.Archive, metadata.getObjectStorageClass());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        try {
            RestoreJobParameters jobParameters = new RestoreJobParameters(RestoreTier.RESTORE_TIER_BULK);
            RestoreConfiguration configuration = new RestoreConfiguration(5).withRestoreJobParameters(jobParameters);
            RestoreObjectRequest restoreObjectRequest = new RestoreObjectRequest(bucketName, objectName).withRestoreConfiguration(configuration);
            ossClient.restoreObject(restoreObjectRequest);
            Assert.fail("Restore job parameters is not math Archive storage class, should be failed.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.MALFORMED_XML, e.getErrorCode());
        }
    }

}
