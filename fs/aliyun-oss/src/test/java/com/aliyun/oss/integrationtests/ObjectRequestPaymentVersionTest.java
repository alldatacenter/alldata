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

import static com.aliyun.oss.integrationtests.TestConstants.ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET;
import static com.aliyun.oss.integrationtests.TestConstants.BUCKET_ACCESS_DENIED_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import junit.framework.Assert;
import org.junit.Test;
import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.model.*;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.DeleteVersionsRequest.KeyVersion;


public class ObjectRequestPaymentVersionTest extends TestBase {

    private OSSClient ossPayerClient;
    private OSSClient ossClient;
    private String bucketName;
    private String endpoint;

    public void setUp() throws Exception {
        super.setUp();

        bucketName = super.bucketName + "-request-payment-version";
        endpoint = TestConfig.OSS_TEST_ENDPOINT;

        //create client
        ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        ossClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);

        ossClient.createBucket(bucketName);
        waitForCacheExpiration(2);

        // Set bucket policy
        StringBuffer strBuffer = new StringBuffer();
        strBuffer.append("{");
        strBuffer.append("\"Version\":\"1\",");
        strBuffer.append("\"Statement\":[{");
        strBuffer.append("\"Action\":[\"oss:*\"],");
        strBuffer.append("\"Effect\":\"Allow\",");
        strBuffer.append("\"Principal\":[\"").append(TestConfig.OSS_TEST_PAYER_UID).append("\"],");
        strBuffer.append("\"Resource\": [").append("\"acs:oss:*:*:").append(bucketName).append("\"").append(",\"acs:oss:*:*:").append(bucketName).append("/*\"]");;
        strBuffer.append("}]}");
        ossClient.setBucketPolicy(bucketName, strBuffer.toString());

        // Enable bucket requestpayment
        ossClient.setBucketRequestPayment(bucketName, Payer.Requester);

        // Enable Versioning
        BucketVersioningConfiguration configuration = new BucketVersioningConfiguration();
        configuration.setStatus(BucketVersioningConfiguration.ENABLED);
        SetBucketVersioningRequest request = new SetBucketVersioningRequest(bucketName, configuration);
        ossClient.setBucketVersioning(request);

        // Create payer client
        credentials = new DefaultCredentials(TestConfig.OSS_TEST_PAYER_ACCESS_KEY_ID, TestConfig.OSS_TEST_PAYER_ACCESS_KEY_SECRET);
        ossPayerClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);
    }

    public void tearDown() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
            ossClient = null;
        }

        if (ossPayerClient != null) {
            ossPayerClient.shutdown();
            ossPayerClient = null;
        }
        super.tearDown();
    }

    @Test
    public void testDeleteVersion() {
        String key = "requestpayment-test-delete-version-object";
        long inputStreamLength = 10;
        InputStream instream = genFixedLengthInputStream(inputStreamLength);

        //Put object version1
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
        PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
        String version1 = putObjectResult.getVersionId();

        // Put object version2
        putObjectResult = ossClient.putObject(putObjectRequest);
        String version2 = putObjectResult.getVersionId();

        // Delete version without payer setting, should be failed.
        try {
            DeleteVersionRequest deleteVersionRequest = new DeleteVersionRequest(bucketName,key, version1);
            ossPayerClient.deleteVersion(deleteVersionRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Delete version with payer setting, should be successful.
        try {
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            genericRequest.setVersionId(version1);
            boolean isExist = ossClient.doesObjectExist(genericRequest);
            Assert.assertEquals(isExist, true);

            Payer payer = Payer.Requester;
            DeleteVersionRequest deleteVersionRequest = new DeleteVersionRequest(bucketName,key, version1);
            deleteVersionRequest.setRequestPayer(payer);
            ossPayerClient.deleteVersion(deleteVersionRequest);

            // Check version1 is not exsit
            try {
                genericRequest = new GenericRequest(bucketName, key); 
                genericRequest.setVersionId(version1);
                ossPayerClient.doesObjectExist(genericRequest);
                Assert.fail("no such version , should not be successful");
            } catch(OSSException e) {
                Assert.assertEquals(OSSErrorCode.ACCESS_FORBIDDEN, e.getErrorCode());
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteVersion(bucketName, key, version2);
        }
    }

    @Test
    public void testDeleteVersions() {
        String key = "requestpayment-test-delete-versions-object";
        long inputStreamLength = 10;
        InputStream instream = genFixedLengthInputStream(inputStreamLength);

        // Put object version1
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
        PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
        String version1 = putObjectResult.getVersionId();

        // Put object version2
        putObjectResult = ossClient.putObject(putObjectRequest);
        String version2 = putObjectResult.getVersionId();

        List<KeyVersion> keyVersionsList = new ArrayList<KeyVersion>();
        keyVersionsList.add(new KeyVersion(key, version1));
        keyVersionsList.add(new KeyVersion(key, version2));

        // Delete versions without payer setting, should be failed.
        try {
            DeleteVersionsRequest delVersionsRequest = new DeleteVersionsRequest(bucketName);
            delVersionsRequest.setKeys(keyVersionsList);
            ossPayerClient.deleteVersions(delVersionsRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Delete versons with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            DeleteVersionsRequest delVersionsRequest = new DeleteVersionsRequest(bucketName);
            delVersionsRequest.setKeys(keyVersionsList);
            delVersionsRequest.setRequestPayer(payer);
            DeleteVersionsResult delVersionsResult = ossPayerClient.deleteVersions(delVersionsRequest);
            Assert.assertEquals(delVersionsResult.getDeletedVersions().size(), 2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testListVersions() {
        String key = "requestpayment-test-list-versions-object";
        long inputStreamLength = 10;
        InputStream instream = genFixedLengthInputStream(inputStreamLength);

        // Put version1
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
        PutObjectResult putObjectResult = ossClient.putObject(putObjectRequest);
        String version1 = putObjectResult.getVersionId();

        // Put version2
        putObjectResult = ossClient.putObject(putObjectRequest);
        String version2 = putObjectResult.getVersionId();

        // Check version1 is exsit
        GenericRequest genericRequest = new GenericRequest(bucketName, key);
        genericRequest.setVersionId(version1);
        boolean isExist = ossClient.doesObjectExist(genericRequest);
        Assert.assertEquals(isExist, true);

        // Check verison2 exist
        genericRequest = new GenericRequest(bucketName, key);
        genericRequest.setVersionId(version2);
        isExist = ossClient.doesObjectExist(genericRequest);
        Assert.assertEquals(isExist, true);

        // List versions without payer setting, should be failed.
        try {
            ListVersionsRequest listVersionsRequest = new ListVersionsRequest().withBucketName(bucketName);
            ossPayerClient.listVersions(listVersionsRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // List versons with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            ListVersionsRequest listVersionsRequest = new ListVersionsRequest().withBucketName(bucketName);
            listVersionsRequest.setRequestPayer(payer);
            VersionListing versionListing = ossPayerClient.listVersions(listVersionsRequest);
            Assert.assertEquals(versionListing.getVersionSummaries().size(), 2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            List<KeyVersion> keyVersionsList = new ArrayList<KeyVersion>();
            keyVersionsList.add(new KeyVersion(key, version1));
            keyVersionsList.add(new KeyVersion(key, version2));
            DeleteVersionsRequest delVersionsRequest = new DeleteVersionsRequest(bucketName).withKeys(keyVersionsList);
            ossClient.deleteVersions(delVersionsRequest);
        }
    }

}