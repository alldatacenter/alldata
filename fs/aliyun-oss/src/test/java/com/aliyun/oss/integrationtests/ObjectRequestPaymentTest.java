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
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import com.aliyun.oss.*;
import com.aliyun.oss.internal.RequestParameters;
import junit.framework.Assert;
import org.junit.Test;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.common.utils.BinaryUtil;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import com.aliyun.oss.utils.ResourceUtils;

public class ObjectRequestPaymentTest extends TestBase {

    private OSSClient ossPayerClient;

    public void setUp() throws Exception {
        super.setUp();

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

        // Create payer client
        ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_PAYER_ACCESS_KEY_ID, TestConfig.OSS_TEST_PAYER_ACCESS_KEY_SECRET);
        ossPayerClient = new OSSClient(TestConfig.OSS_TEST_ENDPOINT, new DefaultCredentialProvider(credentials), conf);
    }

    public void tearDown() throws Exception {

        if (ossPayerClient != null) {
            ossPayerClient.shutdown();
        }
        super.tearDown();
    }

    public void prepareObject(String key) {
        long inputStreamLength = 10;
        InputStream instream = genFixedLengthInputStream(inputStreamLength);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            ossClient.putObject(putObjectRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }  
    }

    public void prepareObject(String key, long inputStreamLength) {
        InputStream instream = genFixedLengthInputStream(inputStreamLength);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            ossClient.putObject(putObjectRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }  
    }

    @Test
    public void testPutObject() {
        String key = "requestpayment-test-put-object";
        long inputStreamLength = 10;
        InputStream instream = genFixedLengthInputStream(inputStreamLength);

        // Put object without request payer, should be failed.
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            PutObjectResult putResult = ossPayerClient.putObject(putObjectRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Put object with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            putObjectRequest.setRequestPayer(payer);
            ossPayerClient.putObject(putObjectRequest);

            //Check by ossClient            
            Assert.assertTrue(ossClient.doesObjectExist(bucketName, key));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testGetObject() throws Throwable {
        String key = "requestpayment-test-get-object";

        prepareObject(key);


        // Get object without payer setting, should be failed.
        try {
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            OSSObject ossObject = ossPayerClient.getObject(getObjectRequest);
            ossObject.close();
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Get object with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucketName, key);
            getObjectRequest.setRequestPayer(payer);
            OSSObject ossObject = ossPayerClient.getObject(getObjectRequest);
            ossObject.close();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }
    
    @Test
    public void testDeleteObject() {
        String key = "requestpayment-test-put-object";
        
        prepareObject(key);

        // Delete object without payer setting, should be failed.
        try {
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            ossPayerClient.deleteObject(genericRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Delete object with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            GenericRequest genericRequest = new GenericRequest(bucketName, key).withRequestPayer(payer);
            ossPayerClient.deleteObject(genericRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDeleteObjects() {
        String key1 = "requestpayment-test-delete-objects-key1";
        String key2 = "requestpayment-test-delete-objects-key2";

        prepareObject(key1);
        prepareObject(key2);

        List<String> keys = new ArrayList<String>();
        keys.add(key1);
        keys.add(key2);

        // Delete objects without payer setting, should be failed.
        try {
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
            deleteObjectsRequest.setKeys(keys);
            ossPayerClient.deleteObjects(deleteObjectsRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Delete objects with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            DeleteObjectsRequest deleteObjectsRequest = new DeleteObjectsRequest(bucketName);
            deleteObjectsRequest.setKeys(keys);
            deleteObjectsRequest.setRequestPayer(payer);
            ossPayerClient.deleteObjects(deleteObjectsRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testListObjects() {
        String key1 = "requestpayment-test-list-objects-key1";
        String key2 = "requestpayment-test-list-objects-key2";
        
        prepareObject(key1);
        prepareObject(key2);

        // List object withou payer setting, should be failed.
        try {
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
            ossPayerClient.listObjects(listObjectsRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // List objects with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setRequestPayer(payer);
            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            Assert.assertEquals(objectListing.getObjectSummaries().size(), 2);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key1);
            ossClient.deleteObject(bucketName, key2);
        }
    }

    @Test
    public void testSetObjectAcl() {
        String key = "requestpayment-test-set-object-acl-object";

        prepareObject(key);

        // Set object acl without payer setting, should be failed.
        try {
            SetObjectAclRequest setObjectAclRequest  = new SetObjectAclRequest(bucketName, key, CannedAccessControlList.PublicRead);
            ossPayerClient.setObjectAcl(setObjectAclRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Set object acl with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            CannedAccessControlList acl = CannedAccessControlList.PublicRead;
            SetObjectAclRequest setObjectAclRequest  = new SetObjectAclRequest(bucketName, key, acl);
            setObjectAclRequest.setRequestPayer(payer);
            ossPayerClient.setObjectAcl(setObjectAclRequest);

            // Check by ossClient
            ObjectAcl returnedAcl = ossClient.getObjectAcl(bucketName, key);
            Assert.assertEquals(acl.toString(), returnedAcl.getPermission().toString());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testGetObjectAcl() {
        String key = "requestpayment-test-get-object-acl-object";

        prepareObject(key);

        // Get object acl without payer setting, should be failed.
        try {
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            ossPayerClient.getObjectAcl(genericRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Get object acl with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            genericRequest.setRequestPayer(payer);
            ossPayerClient.getObjectAcl(genericRequest);   
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testAppendObject() {
        String key = "requestpayment-test-append-object";
        long inputStreamLength = 10;
        InputStream instream = genFixedLengthInputStream(inputStreamLength);

        // First append by ossClient, should be successful
        AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream);
        appendObjectRequest.setPosition(0L);
        ossClient.appendObject(appendObjectRequest);

        // Append object with out payer setting, should be failed.
        try {
            instream = genFixedLengthInputStream(inputStreamLength);
            appendObjectRequest = new AppendObjectRequest(bucketName, key, instream);
            appendObjectRequest.setPosition(inputStreamLength);
            ossPayerClient.appendObject(appendObjectRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Append object with payer setting, should be successful.
        try {
            instream = genFixedLengthInputStream(inputStreamLength);
            Payer payer = Payer.Requester;
            appendObjectRequest = new AppendObjectRequest(bucketName, key, instream);
            appendObjectRequest.setPosition(inputStreamLength);
            appendObjectRequest.setRequestPayer(payer);
            ossPayerClient.appendObject(appendObjectRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testCopyObject() {
        String srcObject = "requestpayment-test-copy-object-src";
        String destObject = "requestpayment-test-copy-object-dest";

        prepareObject(srcObject);

        // Copy object with out payer setting, should be failed.
        try {
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, srcObject, bucketName, destObject);
            ossPayerClient.copyObject(copyObjectRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Copy object with payer setting payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(bucketName, srcObject, bucketName, destObject);
            copyObjectRequest.setRequestPayer(payer);
            ossPayerClient.copyObject(copyObjectRequest);
            
            // Check by bucket owner
            SimplifiedObjectMeta srcMeta = ossClient.getSimplifiedObjectMeta(bucketName, srcObject);
            SimplifiedObjectMeta destMeta = ossClient.getSimplifiedObjectMeta(bucketName, destObject);
            Assert.assertEquals(srcMeta.getSize(), destMeta.getSize());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, srcObject);
            ossClient.deleteObject(bucketName, destObject);
        }
    }

    @Test
    public void testCreateSymlink() {
        String targetObjectName = "requestpayment-test-create-symlink-target";
        String symLink = "requestpayment-test-create-symlink";

        prepareObject(targetObjectName);

        // Create symlink without payer setting, should be failed.
        try {
            CreateSymlinkRequest createSymlinkRequest = new CreateSymlinkRequest(bucketName, symLink, targetObjectName);
            ossPayerClient.createSymlink(createSymlinkRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Create symlink with payer setting payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            CreateSymlinkRequest createSymlinkRequest = new CreateSymlinkRequest(bucketName, symLink, targetObjectName);
            createSymlinkRequest.setRequestPayer(payer);
            ossPayerClient.createSymlink(createSymlinkRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, targetObjectName);
            ossClient.deleteObject(bucketName, symLink);
        }
    } 

    @Test
    public void testGetSymlink() {
        String targetObjectName = "requestpayment-test-get-symlink-target";
        String symLink = "requestpayment-test-create-symlink";

        prepareObject(targetObjectName);

        // prepare symlink
        CreateSymlinkRequest createSymlinkRequest = new CreateSymlinkRequest(bucketName, symLink, targetObjectName);
        ossClient.createSymlink(createSymlinkRequest);

        // Get symlink without payer setting, should be failed.
        try {
            GenericRequest genericRequest = new GenericRequest(bucketName, symLink);  
            ossPayerClient.getSymlink(genericRequest); 
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Get symlink with payer setting payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            GenericRequest genericRequest = new GenericRequest(bucketName, symLink);
            genericRequest.setRequestPayer(payer);
            OSSSymlink symbolicLink = ossPayerClient.getSymlink(genericRequest);

            Assert.assertEquals(symbolicLink.getSymlink(), symLink);
            Assert.assertEquals(symbolicLink.getTarget(), targetObjectName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, targetObjectName);
            ossClient.deleteObject(bucketName, symLink);
        }
    }    

    @Test
    public void testDoesObjectExist() {
        String key = "requestpayment-test-dose-exist-object";

        prepareObject(key);

        // Put request without payer setting
        // isOnlyInOSS flag is true
        try {
            boolean isOnlyInOSS = true;
            GenericRequest genericRequest = new GenericRequest(bucketName, key);  
            boolean isExist = ossPayerClient.doesObjectExist(genericRequest, isOnlyInOSS);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_FORBIDDEN, e.getErrorCode());
            //Assert.assertTrue(e.getMessage().startsWith(BUCKET_ACCESS_DENIED_ERR));
        }

        // Put request without payer setting
        // isOnlyInOSS flag is false
        try {
            boolean isOnlyInOSS = false;
            GenericRequest genericRequest = new GenericRequest(bucketName, key);  
            boolean isExist = ossPayerClient.doesObjectExist(genericRequest, isOnlyInOSS);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            //Assert.assertTrue(e.getMessage().startsWith(BUCKET_ACCESS_DENIED_ERR));
        }

        // Put request with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            boolean isOnlyInOSS = false;

            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            genericRequest.setRequestPayer(payer);

            boolean isExist = ossPayerClient.doesObjectExist(genericRequest, isOnlyInOSS);
            Assert.assertEquals(isExist, true);

            isOnlyInOSS = true;
            isExist = ossPayerClient.doesObjectExist(genericRequest, isOnlyInOSS);
            Assert.assertEquals(isExist, true);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testGetSimplifiedObjectMetadata() {
        String key = "requestpayment-test-get-simplified-meta-object";
        
        prepareObject(key);
        
        // Get simplified meta without payer setting, should be failed.
        try {
            GenericRequest genericRequest = new GenericRequest(bucketName, key);  
            ossPayerClient.getSimplifiedObjectMeta(genericRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_FORBIDDEN, e.getErrorCode());
            //Assert.assertTrue(e.getMessage().startsWith(BUCKET_ACCESS_DENIED_ERR));
        }

         // Get simplified meta with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            genericRequest.setRequestPayer(payer);
            ossPayerClient.getSimplifiedObjectMeta(genericRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testGetObjectMetadata() {
        String key = "requestpayment-test-get-metadata-object";

        prepareObject(key);

        // Get object meta data without payer setting, should be failed.
        try {            
            GenericRequest genericRequest = new GenericRequest(bucketName, key);  
            ossPayerClient.getObjectMetadata(genericRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            System.out.println("Expected OSSException.");
        }

         // Get object meta data with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            genericRequest.setRequestPayer(payer);
            ossPayerClient.getObjectMetadata(genericRequest);  
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }
    
    @Test
    public void testHeadObject() {
        String key = "requestpayment-test-head-object";

        prepareObject(key);

        // Head object meta data without payer setting, should be failed.
        try {            
            HeadObjectRequest headObjectRequest = new HeadObjectRequest(bucketName, key);  
            ossPayerClient.headObject(headObjectRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            System.out.println("Expected OSSException.");
        }

         // Get object meta data with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            HeadObjectRequest headObjectRequest = new HeadObjectRequest(bucketName, key);  
            headObjectRequest.setRequestPayer(payer);
            ossPayerClient.headObject(headObjectRequest);  
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testUploadFile() throws Throwable {
        String key = "requestpayment-test-get-simple-meta-object";
        File file = createSampleFile(key, 1024 * 500);

        prepareObject(key);

        // Upload file without payer setting, should be failed.
        try {
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);          
            ossPayerClient.uploadFile(uploadFileRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Upload file with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            UploadFileRequest uploadFileRequest = new UploadFileRequest(bucketName, key);
            uploadFileRequest.setUploadFile(file.getAbsolutePath());
            uploadFileRequest.setTaskNum(10);
            uploadFileRequest.setRequestPayer(payer);
            ossPayerClient.uploadFile(uploadFileRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testDownloadFile() throws Throwable {
        String key = "requestpayment-test-download-file-object";
        String downFileName = key + "-down.txt";

        prepareObject(key);

        // Download file without payer setting, should be failed.
        try {
            // download file
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(downFileName);
            ossPayerClient.downloadFile(downloadFileRequest);

            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_FORBIDDEN, e.getErrorCode());
            //Assert.assertTrue(e.getMessage().startsWith(BUCKET_ACCESS_DENIED_ERR));
        }

         // Upload file with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            DownloadFileRequest downloadFileRequest = new DownloadFileRequest(bucketName, key);
            downloadFileRequest.setDownloadFile(downFileName);
            downloadFileRequest.setRequestPayer(payer);
            ossPayerClient.downloadFile(downloadFileRequest);

            File downFile = new File(downFileName);
            downFile.delete();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testUploadPart() throws Throwable {
        String key = "requestpayment-test-upload-part-object";
        File file = createSampleFile(key, 1024 * 500);

        // Upload part without payer setting, should be failed.
        try {
            // Set initiateMultipartUploadRequest without payer setting, should be failed.
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
            ossPayerClient.initiateMultipartUpload(initiateMultipartUploadRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Upload part with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;

            // Set initiateMultipartUploadRequest with payer setting
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
            initiateMultipartUploadRequest.setRequestPayer(payer);
            InitiateMultipartUploadResult upresult = ossPayerClient.initiateMultipartUpload(initiateMultipartUploadRequest);

            String uploadId = upresult.getUploadId();

            // Create partETags
            List<PartETag> partETags =  new ArrayList<PartETag>();

            // Calc partsize
            final long partSize = 1 * 1024 * 1024L;   // 1MB

            long fileLength = file.length();
            int partCount = (int) (fileLength / partSize);
            if (fileLength % partSize != 0) {
                partCount++;
             }
            // Upload part
            for (int i = 0; i < partCount; i++) {
                long startPos = i * partSize;
                long curPartSize = (i + 1 == partCount) ? (fileLength - startPos) : partSize;
                InputStream instream = new FileInputStream(file);
                instream.skip(startPos);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setUploadId(uploadId);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartSize(curPartSize);
                uploadPartRequest.setPartNumber( i + 1);
                // Set uploadPartRequest with payer setting
                uploadPartRequest.setRequestPayer(payer);
                UploadPartResult uploadPartResult = ossPayerClient.uploadPart(uploadPartRequest);
                partETags.add(uploadPartResult.getPartETag());
            }

            // Set completeMultipartUploadRequest with payer setting
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            completeMultipartUploadRequest.setRequestPayer(payer);

            ossPayerClient.completeMultipartUpload(completeMultipartUploadRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }  

    @Test
    public void testUploadPartCopy() {
        String sourceObjectName = "requestpayment-test-upload-part-copy-src-object";
        String destinationObjectName = "requestpayment-test-upload-part-copy-dest-object";

        prepareObject(sourceObjectName, 10 * 1024 * 1024);

        // Upload part copy without payer setting, should be failed.
        try {
            // Set initiateMultipartUploadRequest without payer setting, should be failed.
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, destinationObjectName);
            ossPayerClient.initiateMultipartUpload(initiateMultipartUploadRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

        // Upload part with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            GenericRequest genericRequest = new GenericRequest(bucketName, sourceObjectName);
            genericRequest.setRequestPayer(payer);
            ObjectMetadata objectMetadata = ossPayerClient.getObjectMetadata(genericRequest);
            // Get length
            long contentLength = objectMetadata.getContentLength();
            // Set part unit size 
            long partSize = 1024 * 1024 * 10;
            // Calc part count
            int partCount = (int) (contentLength / partSize);
            if (contentLength % partSize != 0) {
                partCount++;
            }

            // Set initiateMultipartUploadRequest with payer setting
            InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, destinationObjectName);
            initiateMultipartUploadRequest.setRequestPayer(payer);
            InitiateMultipartUploadResult initiateMultipartUploadResult = ossPayerClient.initiateMultipartUpload(initiateMultipartUploadRequest);
            // Get uploadid
            String uploadId = initiateMultipartUploadResult.getUploadId();

            // Upload part copy
            List<PartETag> partETags = new ArrayList<PartETag>();
            for (int i = 0; i < partCount; i++) {
                long skipBytes = partSize * i;
                long size = partSize < contentLength - skipBytes ? partSize : contentLength - skipBytes;

                UploadPartCopyRequest uploadPartCopyRequest = 
                        new UploadPartCopyRequest(bucketName, sourceObjectName, bucketName, destinationObjectName);
                uploadPartCopyRequest.setUploadId(uploadId);
                uploadPartCopyRequest.setPartSize(size);
                uploadPartCopyRequest.setBeginIndex(skipBytes);
                uploadPartCopyRequest.setPartNumber(i + 1);
                // Set uploadPartCopyRequest with payer setting
                uploadPartCopyRequest.setRequestPayer(payer);
                UploadPartCopyResult uploadPartCopyResult = ossPayerClient.uploadPartCopy(uploadPartCopyRequest);
                partETags.add(uploadPartCopyResult.getPartETag());
            }

            // Set completeMultipartUploadRequest with payer setting
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(
                             bucketName, destinationObjectName, uploadId, partETags);
            completeMultipartUploadRequest.setRequestPayer(payer);

            ossPayerClient.completeMultipartUpload(completeMultipartUploadRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, sourceObjectName);
            ossClient.deleteObject(bucketName, destinationObjectName);
        }
    }

    @Test
    public void testListParts() {
        String key = "requestpayment-test-list-parts-object";

        // Get upload id
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
        InitiateMultipartUploadResult upresult = ossClient.initiateMultipartUpload(initiateMultipartUploadRequest);
        String uploadId = upresult.getUploadId();

        // List parts without payer setting, should be failed.
        try {
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            ossPayerClient.listParts(listPartsRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // List parts with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            listPartsRequest.setRequestPayer(payer);
            ossPayerClient.listParts(listPartsRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testAbortMultipartUpload() {
        String key = "requestpayment-test-abort-multi-part-object";

        // Get upload id
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest(bucketName, key);
        InitiateMultipartUploadResult upresult = ossClient.initiateMultipartUpload(initiateMultipartUploadRequest);
        String uploadId = upresult.getUploadId();

        // Abort multi part without payer setting, should be failed.
        try {
            AbortMultipartUploadRequest abortMultipartUploadRequest =new AbortMultipartUploadRequest(bucketName, key, uploadId);
            ossPayerClient.abortMultipartUpload(abortMultipartUploadRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         //  Abort multi part with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;

            AbortMultipartUploadRequest abortMultipartUploadRequest =new AbortMultipartUploadRequest(bucketName, key, uploadId);
            abortMultipartUploadRequest.setRequestPayer(payer);
            ossPayerClient.abortMultipartUpload(abortMultipartUploadRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }    

    @Test
    public void testListMultiUploads() {
        // List multi uploads without payer setting, should be failed.
        try {
            ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            ossPayerClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // List multi uploads with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            listMultipartUploadsRequest.setRequestPayer(payer);
            ossPayerClient.listMultipartUploads(listMultipartUploadsRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRestoreObject() {
        String achiveObject = "requestpayment-test-restore-object";

        // Prepare achive object
        InputStream instream = genFixedLengthInputStream(10);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setHeader(OSSHeaders.OSS_STORAGE_CLASS, StorageClass.Archive);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, achiveObject, instream, metadata);
        ossClient.putObject(putObjectRequest);

        // Restore object without payer setting, should be failed.
        try {
            GenericRequest generirequest = new GenericRequest(bucketName, achiveObject);
            ObjectMetadata objectMetadata = ossClient.getObjectMetadata(generirequest);
            
            // Check whether the object is archive class
            StorageClass storageClass = objectMetadata.getObjectStorageClass();
            if (storageClass == StorageClass.Archive) {
                // Restore object without payer setting
                ossPayerClient.restoreObject(bucketName, achiveObject);
            }
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Restore object with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            GenericRequest generirequest = new GenericRequest(bucketName, achiveObject);
            generirequest.setRequestPayer(payer);
            ObjectMetadata objectMetadata = ossPayerClient.getObjectMetadata(generirequest);

            // Check whether the object is archive class
            StorageClass storageClass = objectMetadata.getObjectStorageClass();
            if (storageClass == StorageClass.Archive) {
                // restore object with payer setting
                ossPayerClient.restoreObject(generirequest);
            } else {
                Assert.fail("test restore object has some wrong, should be archive class.");
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, achiveObject);
        }
    }

    @Test
    public void testProcessObject() {
        String originalImage = "oss/example.jpg";
        String saveAsKey = "saveaskey-process.jpg";

        ossClient.putObject(bucketName, originalImage, new File(ResourceUtils.getTestFilename(originalImage)));

        StringBuilder styleBuilder = new StringBuilder();
        styleBuilder.append("image/resize,m_fixed,w_100,h_100");  // resize
        styleBuilder.append("|sys/saveas,");
        styleBuilder.append("o_" + BinaryUtil.toBase64String(saveAsKey.getBytes()));
        styleBuilder.append(",");
        styleBuilder.append("b_" + BinaryUtil.toBase64String(bucketName.getBytes()));

        // Process object without payer setting, should be failed.
        try {
            ProcessObjectRequest request = new ProcessObjectRequest(bucketName, originalImage, styleBuilder.toString());
            ossPayerClient.processObject(request);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Process object with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            ProcessObjectRequest request = new ProcessObjectRequest(bucketName, originalImage, styleBuilder.toString());
            request.setRequestPayer(payer);
            ossPayerClient.processObject(request);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, originalImage);
        }
    }  

    @Test
    public void testSetObjectTagging() throws Throwable {
        String key = "requestpayment-test-set-object-tagging-object";

        prepareObject(key);

        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");

        // Set object tagging without payer setting, should be failed.
        try {
            SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(bucketName, key, tags);
            ossPayerClient.setObjectTagging(setObjectTaggingRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Set object tagging with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(bucketName, key, tags);
            setObjectTaggingRequest.setRequestPayer(payer);
            ossPayerClient.setObjectTagging(setObjectTaggingRequest);

            // Check result
            TagSet tagSet = ossClient.getObjectTagging(bucketName, key);
            Assert.assertEquals(tagSet.getAllTags().size(), tags.size());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    
    @Test
    public void testGetObjectTagging() {
        String key = "requestpayment-test-get-object-tagging-object";

        prepareObject(key);

        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");
        
        SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(bucketName, key, tags);
        ossClient.setObjectTagging(setObjectTaggingRequest);

        // Get object tagging without payer setting, should be failed.
        try {
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            ossPayerClient.getObjectTagging(genericRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Set object tagging with payer setting, should be successful.
        try {
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            Payer payer = Payer.Requester;
            genericRequest.setRequestPayer(payer);
            TagSet tagSet2 = ossPayerClient.getObjectTagging(genericRequest);
            Assert.assertEquals(tagSet2.getAllTags().size(), tags.size());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testDeleteObjectTagging() {
        String key = "requestpayment-test-delete-object-tagging-object";

        prepareObject(key);

        Map<String, String> tags = new HashMap<String, String>();
        tags.put("key1", "value1");
        tags.put("key2", "value2");

        ossClient.setObjectTagging(bucketName, key, tags);

        GenericRequest genericRequest = new GenericRequest(bucketName, key);
        TagSet tagSet = ossClient.getObjectTagging(genericRequest);
        Assert.assertEquals(tagSet.getAllTags().size(), tags.size());

        // Delete object tagging without payer setting, should be failed.
        try {
            ossPayerClient.deleteObjectTagging(genericRequest);
            Assert.fail("no RequestPayer, should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(ACCESS_DENIED_MSG_REQUESTER_PAY_BUCKET));
        }

         // Delete object tagging with payer setting, should be successful.
        try {
            Payer payer = Payer.Requester;
            genericRequest.setRequestPayer(payer);
            ossPayerClient.deleteObjectTagging(genericRequest);

            TagSet tagSet2 = ossPayerClient.getObjectTagging(genericRequest);
            Assert.assertEquals(tagSet2.getAllTags().size(), 0);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }

    @Test
    public void testPutSignUrl() {
        String key = "requestpayment-test-put-sign-url";
        long inputStreamLength = 10;
        InputStream instream = genFixedLengthInputStream(inputStreamLength);

        Date date = new Date();
        date.setTime(date.getTime() + 60 * 1000);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.PUT);
        request.setExpiration(date);

        Map<String, String> queryParam = new HashMap<String, String>();
        queryParam.put(RequestParameters.OSS_REQUEST_PAYER, Payer.Requester.toString().toLowerCase());
        request.setQueryParameter(queryParam);

        URL signedUrl = ossPayerClient.generatePresignedUrl(request);
        try {
            ossPayerClient.putObject(signedUrl, instream, -1, null, true);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testGetSignUrl() {
        String key = "requestpayment-test-get-sign-url";
        InputStream instream = genFixedLengthInputStream(10);
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, instream);
            ossClient.putObject(putObjectRequest);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

        Date date = new Date();
        date.setTime(date.getTime()+ 60*1000);

        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, key, HttpMethod.GET);
        request.setExpiration(date);

        Map<String, String> queryParam = new HashMap<String, String>();
        queryParam.put(RequestParameters.OSS_REQUEST_PAYER, Payer.Requester.toString().toLowerCase());
        request.setQueryParameter(queryParam);

        URL signedUrl = ossPayerClient.generatePresignedUrl(request);
        try {
            ossPayerClient.getObject(signedUrl, null);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void testAccessMonitor() throws Throwable {
        String key = "requestpayment-test-get-object";

        prepareObject(key);

        // Verify access monitor
        try {
            Payer payer = Payer.Requester;
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            genericRequest.setRequestPayer(payer);

            ossClient.putBucketAccessMonitor(bucketName, AccessMonitor.AccessMonitorStatus.Enabled.toString());

            AccessMonitor accessMonitor = null;

            long startTime = System.currentTimeMillis();
            while (true){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                accessMonitor = ossClient.getBucketAccessMonitor(bucketName);
                if("Enabled".equals(accessMonitor.getStatus())){
                    break;
                }
                long endTime = System.currentTimeMillis();
                if(endTime - startTime > 1000 * 60){
                    Assert.assertFalse(true);
                }
            }
            Assert.assertTrue(true);
        } catch (OSSException e) {
            System.out.println("Accessmonitor execution failed.");
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }


    @Test
    public void testAccessMonitorWithGetObjectMeta() {
        String key = "test-get-object-last-access-time";

        prepareObject(key);

        // Verify x-oss-last-access-time response header
        try {
            Payer payer = Payer.Requester;
            GenericRequest genericRequest = new GenericRequest(bucketName, key);
            genericRequest.setRequestPayer(payer);
            Object accessTime = ossPayerClient.getSimplifiedObjectMeta(genericRequest).getHeaders().get("x-oss-last-access-time");
            Assert.assertNull(accessTime);

            ossClient.putBucketAccessMonitor(bucketName, AccessMonitor.AccessMonitorStatus.Enabled.toString());

            Object accessTime2 = ossPayerClient.getSimplifiedObjectMeta(genericRequest).getHeaders().get("x-oss-last-access-time");
            Assert.assertNotNull(accessTime2);
        } catch (OSSException e) {
            System.out.println("Accessmonitor x-oss-last-access-time execution failed.");
        } finally {
            ossClient.deleteObject(bucketName, key);
        }
    }
}