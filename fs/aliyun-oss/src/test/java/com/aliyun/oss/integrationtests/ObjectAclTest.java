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

import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_ENDPOINT;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_KEY_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.calcMultipartsETag;
import static com.aliyun.oss.integrationtests.TestUtils.claimUploadId;
import static com.aliyun.oss.integrationtests.TestUtils.composeLocation;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_OBJECT_CONTENT_TYPE;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.AppendObjectRequest;
import com.aliyun.oss.model.AppendObjectResult;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectAcl;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.ObjectPermission;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PutObjectResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.aliyun.oss.model.SetObjectAclRequest;

public class ObjectAclTest extends TestBase {

    private static final CannedAccessControlList[] ACLS = {
        CannedAccessControlList.Default,
        CannedAccessControlList.Private,
        CannedAccessControlList.PublicRead,
        CannedAccessControlList.PublicReadWrite
    };

    @Test
    public void testNormalSetObjectAcl() {
        final String key = "normal-set-object-acl";
        final long inputStreamLength = 128 * 1024; //128KB

        try {
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            ossClient.putObject(bucketName, key, instream);

            for (CannedAccessControlList acl : ACLS) {
                ossClient.setObjectAcl(bucketName, key, acl);

                ObjectAcl returnedAcl = ossClient.getObjectAcl(bucketName, key);
                Assert.assertEquals(acl.toString(), returnedAcl.getPermission().toString());
                Assert.assertEquals(returnedAcl.getRequestId().length(), REQUEST_ID_LEN);
                Assert.assertNotNull(returnedAcl.toString());

                OSSObject object = ossClient.getObject(bucketName, key);
                Assert.assertEquals(inputStreamLength, object.getObjectMetadata().getContentLength());
                Assert.assertEquals(object.getRequestId().length(), REQUEST_ID_LEN);
                object.getObjectContent().close();
            }

            // Set to default acl again
            ossClient.setObjectAcl(bucketName, key, CannedAccessControlList.Default);
            ObjectAcl returnedAcl = ossClient.getObjectAcl(bucketName, key);
            Assert.assertEquals(ObjectPermission.Default, returnedAcl.getPermission());
            Assert.assertEquals(returnedAcl.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUnormalSetObjectAcl() {
        try {
            // Set non-existent object
            final String nonexistentObject = "unormal-set-object-acl";
            try {
                SetObjectAclRequest request = new SetObjectAclRequest(bucketName, nonexistentObject)
                        .withCannedACL(CannedAccessControlList.Default);
                request.setCannedACL(CannedAccessControlList.Private);
                ossClient.setObjectAcl(request);
                Assert.fail("Set object acl should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_KEY, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_KEY_ERR));
            }

            // Set unknown permission
            final String unknownPermission = "UnknownPermission";
            try {
                ObjectPermission permission = ObjectPermission.parsePermission(unknownPermission);
                Assert.assertEquals(ObjectPermission.Unknown, permission);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUnormalGetObjectAcl() {
        // Get non-existent object acl
        final String nonexistentObject = "unormal-get-object-acl";
        try {
            ossClient.getObjectAcl(bucketName, nonexistentObject);
            Assert.fail("Get object acl should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_KEY, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_KEY_ERR));
        }

        // Get object using default acl
        final String objectUsingDefaultAcl = "object-using-default-acl";
        final long inputStreamLength = 128 * 1024; //128KB
        try {
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            ossClient.putObject(bucketName, objectUsingDefaultAcl, instream);
            ObjectAcl returnedACL = ossClient.getObjectAcl(bucketName, objectUsingDefaultAcl);
            Assert.assertEquals(ObjectPermission.Default, returnedACL.getPermission());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testPutObjectWithACLHeader() throws IOException {
        final String key = "put-object-with-acl-header";
        final long inputStreamLength = 128 * 1024; //128KB

        try {
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectAcl(CannedAccessControlList.PublicRead);
            ossClient.putObject(bucketName, key, instream, metadata);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);

            // Verify uploaded objects acl
            ObjectAcl returnedACL = ossClient.getObjectAcl(bucketName, key);
            Assert.assertEquals(ObjectPermission.PublicRead, returnedACL.getPermission());
            Assert.assertEquals(returnedACL.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testAppendObjectWithACLHeader() throws IOException {
        final String key = "append-object-with-acl-header";
        final long inputStreamLength = 128 * 1024; //128KB

        try {
            // Append at first
            InputStream instream = genFixedLengthInputStream(inputStreamLength);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectAcl(CannedAccessControlList.PublicReadWrite);
            AppendObjectRequest appendObjectRequest = new AppendObjectRequest(bucketName, key, instream, metadata);
            appendObjectRequest.setPosition(0L);
            AppendObjectResult appendObjectResult = ossClient.appendObject(appendObjectRequest);
            OSSObject o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(key, o.getKey());
            Assert.assertEquals(inputStreamLength, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(APPENDABLE_OBJECT_TYPE, o.getObjectMetadata().getObjectType());
            if (appendObjectResult.getNextPosition() != null) {
                Assert.assertEquals(inputStreamLength, appendObjectResult.getNextPosition().longValue());
            }

            // Append at twice
            final String filePath = genFixedLengthFile(inputStreamLength);
            appendObjectRequest = new AppendObjectRequest(bucketName, key, new File(filePath));
            appendObjectRequest.setPosition(appendObjectResult.getNextPosition());
            appendObjectResult = ossClient.appendObject(appendObjectRequest);
            o = ossClient.getObject(bucketName, key);
            Assert.assertEquals(inputStreamLength * 2, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(APPENDABLE_OBJECT_TYPE, o.getObjectMetadata().getObjectType());
            if (appendObjectResult.getNextPosition() != null) {
                Assert.assertEquals(inputStreamLength * 2, appendObjectResult.getNextPosition().longValue());
            }

            // Verify uploaded objects acl
            ObjectAcl returnedACL = ossClient.getObjectAcl(bucketName, key);
            Assert.assertEquals(ObjectPermission.PublicReadWrite, returnedACL.getPermission());
            Assert.assertEquals(returnedACL.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCopyObjectWithACLHeader() throws IOException {
        final String sourceBucket = "copy-existing-object-source-bucket";
        final String targetBucket = "copy-existing-object-target-bucket";
        final String sourceKey = "copy-existing-object-source-object";
        final String targetKey = "copy-existing-object-target-object";

        final String userMetaKey0 = "user";
        final String userMetaValue0 = "aliy";
        final String userMetaKey1 = "tag";
        final String userMetaValue1 = "copy-object";
        final String contentType = "application/txt";

        try {
            ossClient.createBucket(sourceBucket);
            ossClient.createBucket(targetBucket);

            byte[] content = {'A', 'l', 'i', 'y', 'u', 'n'};
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            metadata.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
            metadata.addUserMetadata(userMetaKey0, userMetaValue0);
            PutObjectResult putObjectResult = ossClient.putObject(sourceBucket, sourceKey,
                    new ByteArrayInputStream(content), metadata);

            ObjectMetadata newObjectMetadata = new ObjectMetadata();
            newObjectMetadata.setContentLength(content.length);
            newObjectMetadata.setContentType(contentType);
            newObjectMetadata.addUserMetadata(userMetaKey1, userMetaValue1);
            newObjectMetadata.setObjectAcl(CannedAccessControlList.PublicRead);
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucket, sourceKey,
                    targetBucket, targetKey);
            copyObjectRequest.setNewObjectMetadata(newObjectMetadata);
            CopyObjectResult copyObjectResult = ossClient.copyObject(copyObjectRequest);
            String sourceETag = putObjectResult.getETag();
            String targetETag = copyObjectResult.getETag();
            Assert.assertEquals(sourceETag, targetETag);
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);

            OSSObject ossObject = ossClient.getObject(targetBucket, targetKey);
            newObjectMetadata = ossObject.getObjectMetadata();
            Assert.assertEquals(contentType, newObjectMetadata.getContentType());
            Assert.assertEquals(userMetaValue1, newObjectMetadata.getUserMetadata().get(userMetaKey1));

            // Verify uploaded objects acl
            ObjectAcl returnedACL = ossClient.getObjectAcl(targetBucket, targetKey);
            Assert.assertEquals(ObjectPermission.PublicRead, returnedACL.getPermission());
            Assert.assertEquals(returnedACL.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            waitForCacheExpiration(5);
            deleteBucketWithObjects(ossClient, sourceBucket);
            deleteBucketWithObjects(ossClient, targetBucket);
        }
    }

    @Test
    public void testUploadMultipartsWithAclHeader() {
        final String key = "normal-upload-multiparts-with-acl-header";
        final int partSize = 128 * 1024;     //128KB
        final int partCount = 10;

        try {
            // Initial multipart upload
            String uploadId = claimUploadId(ossClient, bucketName, key);

            // Upload parts
            List<PartETag> partETags = new ArrayList<PartETag>();
            for (int i = 0; i < partCount; i++) {
                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                partETags.add(uploadPartResult.getPartETag());
            }

            // Complete multipart upload
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            completeMultipartUploadRequest.setObjectACL(CannedAccessControlList.PublicRead);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                    completeMultipartUploadResult.getLocation());
            Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
            Assert.assertEquals(key, completeMultipartUploadResult.getKey());
            Assert.assertEquals(calcMultipartsETag(partETags), completeMultipartUploadResult.getETag());
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

            // Get uploaded object
            OSSObject o = ossClient.getObject(bucketName, key);
            final long objectSize = partCount * partSize;
            Assert.assertEquals(objectSize, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(calcMultipartsETag(partETags), o.getObjectMetadata().getETag());

            // Verify uploaded objects acl
            ObjectAcl returnedACL = ossClient.getObjectAcl(bucketName, key);
            Assert.assertEquals(ObjectPermission.PublicRead, returnedACL.getPermission());
            Assert.assertEquals(returnedACL.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testIllegalObjectAcl() {
        final String dummyKey = "test-illegal-object-acl";
        try {
            ossClient.setObjectAcl(bucketName, dummyKey, null);
        } catch (Exception e) {
            Assert.assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testIgnoredObjectAclHeader() {
        final String dummyKey = "test-ignored-object-acl-header";
        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setObjectAcl(null);
            ossClient.putObject(bucketName, dummyKey, new ByteArrayInputStream(new byte[0]), metadata);
            ObjectAcl objectAcl = ossClient.getObjectAcl(bucketName, dummyKey);
            Assert.assertEquals(ObjectPermission.Default, objectAcl.getPermission());
            Assert.assertEquals(objectAcl.getRequestId().length(), REQUEST_ID_LEN);

            metadata.setObjectAcl(CannedAccessControlList.Private);
            ossClient.putObject(bucketName, dummyKey, new ByteArrayInputStream(new byte[0]), metadata);
            objectAcl = ossClient.getObjectAcl(bucketName, dummyKey);
            Assert.assertEquals(ObjectPermission.Private, objectAcl.getPermission());
            Assert.assertEquals(objectAcl.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
