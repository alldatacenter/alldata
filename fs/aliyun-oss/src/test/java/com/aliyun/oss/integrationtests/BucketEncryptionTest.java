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
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import junit.framework.Assert;

import org.junit.Test;

import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

import com.aliyun.oss.OSSException;

import java.io.File;
import java.util.Map;

public class BucketEncryptionTest extends TestBase {

    private OSSClient ossClient;
    private String bucketName;
    private String endpoint;

    public void setUp() throws Exception {
        super.setUp();

        bucketName = super.bucketName + "-encryption";
        endpoint = "http://oss-ap-southeast-5.aliyuncs.com";

        //create client
        ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        ossClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);

        ossClient.createBucket(bucketName);
        waitForCacheExpiration(2);
    }

    public void tearDown() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
            ossClient = null;
        }
        super.tearDown();
    }

    private void testSetBucketEncryptionInternal(SSEAlgorithm algorithm, DataEncryptionAlgorithm dataEncryptionAlgorithm) {

        try {
            // set
            ServerSideEncryptionByDefault applyServerSideEncryptionByDefault =
                    new ServerSideEncryptionByDefault(algorithm.toString());
            if (algorithm == SSEAlgorithm.KMS && dataEncryptionAlgorithm != null) {
                applyServerSideEncryptionByDefault.setKMSDataEncryption(dataEncryptionAlgorithm.toString());
            }
            ServerSideEncryptionConfiguration setConfiguration = new ServerSideEncryptionConfiguration();
            setConfiguration.setApplyServerSideEncryptionByDefault(applyServerSideEncryptionByDefault);
            SetBucketEncryptionRequest setRequest = new SetBucketEncryptionRequest(bucketName, setConfiguration);

            ossClient.setBucketEncryption(setRequest);

            // get
            ServerSideEncryptionConfiguration getConfiguration = ossClient.getBucketEncryption(bucketName);
            Assert.assertEquals(algorithm.toString(),
                    getConfiguration.getApplyServerSideEncryptionByDefault().getSSEAlgorithm());
            Assert.assertNull(getConfiguration.getApplyServerSideEncryptionByDefault().getKMSMasterKeyID());
            Assert.assertEquals(dataEncryptionAlgorithm,
                DataEncryptionAlgorithm.fromString(getConfiguration.getApplyServerSideEncryptionByDefault().getKMSDataEncryption()));
            String fileName = TestUtils.genFixedLengthFile(1024);
            String objectName = "encryption-" + TestUtils.genRandomString(10);
            ossClient.putObject(bucketName, objectName, new File(fileName));

            Map<String, String> headers = ossClient.getObject(bucketName, objectName).getResponse().getHeaders();
            Assert.assertEquals(algorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_ENCRYPTION));
            if (algorithm == SSEAlgorithm.KMS && dataEncryptionAlgorithm != null) {
                Assert.assertEquals(dataEncryptionAlgorithm.toString(), headers.get(OSSHeaders.OSS_SERVER_SIDE_DATA_ENCRYPTION));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testSetBucketEncryption() {
        testSetBucketEncryptionInternal(SSEAlgorithm.AES256, null);
        testSetBucketEncryptionInternal(SSEAlgorithm.SM4, null);
        testSetBucketEncryptionInternal(SSEAlgorithm.KMS, null);
        testSetBucketEncryptionInternal(SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
    }

    private void testDeleteBucketEncryptionInternal(SSEAlgorithm algorithm, DataEncryptionAlgorithm dataEncryptionAlgorithm) {

        try {
            // set
            ServerSideEncryptionByDefault applyServerSideEncryptionByDefault =
                    new ServerSideEncryptionByDefault().withSSEAlgorithm(algorithm);
            if (algorithm == SSEAlgorithm.KMS)
                applyServerSideEncryptionByDefault.setKMSMasterKeyID("test-kms-master-key-id");
            if (algorithm == SSEAlgorithm.KMS && dataEncryptionAlgorithm != null) {
                applyServerSideEncryptionByDefault.setKMSDataEncryption(dataEncryptionAlgorithm.toString());
            }
            ServerSideEncryptionConfiguration setConfiguration = new ServerSideEncryptionConfiguration()
                    .withApplyServerSideEncryptionByDefault(applyServerSideEncryptionByDefault);
            setConfiguration.setApplyServerSideEncryptionByDefault(applyServerSideEncryptionByDefault);
            SetBucketEncryptionRequest setRequest = new SetBucketEncryptionRequest(bucketName)
                    .withServerSideEncryptionConfiguration(setConfiguration);

            ossClient.setBucketEncryption(setRequest);

            // get
            ServerSideEncryptionConfiguration getConfiguration = ossClient.getBucketEncryption(bucketName);
            Assert.assertEquals(algorithm.toString(),
                    getConfiguration.getApplyServerSideEncryptionByDefault().getSSEAlgorithm());
            if (algorithm == SSEAlgorithm.KMS)
                Assert.assertEquals("test-kms-master-key-id",
                        getConfiguration.getApplyServerSideEncryptionByDefault().getKMSMasterKeyID());
            Assert.assertEquals(dataEncryptionAlgorithm,
                DataEncryptionAlgorithm.fromString(getConfiguration.getApplyServerSideEncryptionByDefault().getKMSDataEncryption()));
            // delete
            ossClient.deleteBucketEncryption(bucketName);
            waitForCacheExpiration(3);

            // check
            try {
                ossClient.getBucketEncryption(bucketName);
            } catch (OSSException e) {
                Assert.assertEquals("NoSuchServerSideEncryptionRule", e.getErrorCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDeleteBucketEncryption() {
        testDeleteBucketEncryptionInternal(SSEAlgorithm.AES256, null);
        testDeleteBucketEncryptionInternal(SSEAlgorithm.SM4, null);
        testDeleteBucketEncryptionInternal(SSEAlgorithm.KMS, null);
        testDeleteBucketEncryptionInternal(SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
    }

    public void testBucketInfoInternal(SSEAlgorithm algorithm, DataEncryptionAlgorithm dataEncryptionAlgorithm) {

        try {
            // set 1
            ServerSideEncryptionByDefault applyServerSideEncryptionByDefault =
                    new ServerSideEncryptionByDefault(algorithm);
            if (algorithm == SSEAlgorithm.KMS && dataEncryptionAlgorithm != null) {
                applyServerSideEncryptionByDefault.setKMSDataEncryption(dataEncryptionAlgorithm.toString());
            }
            ServerSideEncryptionConfiguration setConfiguration = new ServerSideEncryptionConfiguration();
            setConfiguration.setApplyServerSideEncryptionByDefault(applyServerSideEncryptionByDefault);
            SetBucketEncryptionRequest setRequest = new SetBucketEncryptionRequest(bucketName, setConfiguration);

            ossClient.setBucketEncryption(setRequest);

            // get
            BucketInfo bucketInfo = ossClient.getBucketInfo(bucketName);
            Assert.assertEquals(algorithm.toString(), bucketInfo.getServerSideEncryptionConfiguration()
                    .getApplyServerSideEncryptionByDefault().getSSEAlgorithm());
            if (algorithm != SSEAlgorithm.KMS)
                Assert.assertNull(bucketInfo.getServerSideEncryptionConfiguration()
                    .getApplyServerSideEncryptionByDefault().getKMSMasterKeyID());
            Assert.assertEquals(dataEncryptionAlgorithm,
                    DataEncryptionAlgorithm.fromString(bucketInfo.getServerSideEncryptionConfiguration()
                            .getApplyServerSideEncryptionByDefault().getKMSDataEncryption()));

            // delete
            ossClient.deleteBucketEncryption(bucketName);
            waitForCacheExpiration(3);

            // set 2
            applyServerSideEncryptionByDefault = new ServerSideEncryptionByDefault().withSSEAlgorithm(SSEAlgorithm.KMS.toString());
            applyServerSideEncryptionByDefault.setKMSMasterKeyID("test-kms-master-key-id");
            setConfiguration = new ServerSideEncryptionConfiguration();
            setConfiguration.setApplyServerSideEncryptionByDefault(applyServerSideEncryptionByDefault);
            setRequest = new SetBucketEncryptionRequest(bucketName, setConfiguration);

            ossClient.setBucketEncryption(setRequest);

            // get
            bucketInfo = ossClient.getBucketInfo(bucketName);
            Assert.assertEquals(SSEAlgorithm.KMS.toString(), bucketInfo.getServerSideEncryptionConfiguration()
                    .getApplyServerSideEncryptionByDefault().getSSEAlgorithm());
            Assert.assertEquals("test-kms-master-key-id", bucketInfo.getServerSideEncryptionConfiguration()
                    .getApplyServerSideEncryptionByDefault().getKMSMasterKeyID());

            // delete
            ossClient.deleteBucketEncryption(bucketName);
            waitForCacheExpiration(3);

            // get
            bucketInfo = ossClient.getBucketInfo(bucketName);
            Assert.assertEquals("None", bucketInfo.getServerSideEncryptionConfiguration()
                    .getApplyServerSideEncryptionByDefault().getSSEAlgorithm());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testBucketInfo() {
        testBucketInfoInternal(SSEAlgorithm.AES256, null);
        testBucketInfoInternal(SSEAlgorithm.SM4, null);
        testBucketInfoInternal(SSEAlgorithm.KMS, null);
        testBucketInfoInternal(SSEAlgorithm.KMS, DataEncryptionAlgorithm.SM4);
    }
}
