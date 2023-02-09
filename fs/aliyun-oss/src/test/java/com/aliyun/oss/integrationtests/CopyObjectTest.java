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

import static com.aliyun.oss.integrationtests.TestConstants.INVALID_ENCRYPTION_ALGO_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NOT_MODIFIED_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_KEY_ERR;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_OBJECT_CONTENT_TYPE;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.CopyObjectRequest;
import com.aliyun.oss.model.CopyObjectResult;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectResult;

public class CopyObjectTest extends TestBase {
    
    @Test
    public void testCopyExistingObject() {        
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
            
            // Set source object different with target object and copy source bucket orignal metadata(default behavior).
            byte[] content = { 'A', 'l', 'i', 'y', 'u', 'n' };
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            metadata.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
            metadata.addUserMetadata(userMetaKey0, userMetaValue0);
            
            PutObjectResult putObjectResult = ossClient.putObject(sourceBucket, sourceKey, 
                    new ByteArrayInputStream(content), metadata);
            CopyObjectResult copyObjectResult = ossClient.copyObject(sourceBucket, sourceKey, 
                    targetBucket, targetKey);
            String sourceETag = putObjectResult.getETag();
            String targetETag = copyObjectResult.getETag();
            Assert.assertEquals(sourceETag, targetETag);
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            
            OSSObject ossObject = ossClient.getObject(targetBucket, targetKey);
            ObjectMetadata newObjectMetadata = ossObject.getObjectMetadata();
            Assert.assertEquals(DEFAULT_OBJECT_CONTENT_TYPE, newObjectMetadata.getContentType());
            Assert.assertEquals(userMetaValue0, newObjectMetadata.getUserMetadata().get(userMetaKey0));
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
            
            // Set source object same as target object and replace source bucket orignal metadata.
            final String sourceBucketAsTarget = sourceBucket;
            final String sourceKeyAsTarget = sourceKey;
            newObjectMetadata = new ObjectMetadata();
            newObjectMetadata.setContentLength(content.length);
            newObjectMetadata.setContentType(contentType);
            newObjectMetadata.addUserMetadata(userMetaKey1, userMetaValue1);
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucket, sourceKey,
                    sourceBucketAsTarget, sourceKeyAsTarget);
            copyObjectRequest.setNewObjectMetadata(newObjectMetadata);
            copyObjectResult = ossClient.copyObject(copyObjectRequest);
            Assert.assertEquals(sourceETag, copyObjectResult.getETag());
            
            ossObject = ossClient.getObject(sourceBucketAsTarget, sourceKeyAsTarget);
            newObjectMetadata = ossObject.getObjectMetadata();
            Assert.assertEquals(contentType, newObjectMetadata.getContentType());
            Assert.assertEquals(userMetaValue1, newObjectMetadata.getUserMetadata().get(userMetaKey1));
            Assert.assertEquals(putObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(copyObjectResult.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(ossObject.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            waitForCacheExpiration(5);
            deleteBucketWithObjects(ossClient, sourceBucket);
            deleteBucketWithObjects(ossClient, targetBucket);
        }
    }
    
    @Ignore
    public void testCopyNonexistentObject() {
        final String existingSourceBucket = "copy-nonexistent-object-existing-source-bucket";
        final String existingSourceKey = "copy-nonexistent-object-existing-source-object";
        final String nonexistentSourceBucket = "copy-nonexistent-object-nonexistent-source-bucket";
        final String nonexistentSourceKey = "copy-nonexistent-object-nonexistent-source-object";
        
        final String existingTargetBucket = "copy-nonexistent-object-existing-target-bucket";
        final String nonexistentTargetBucket = "copy-nonexistent-object-nonexistent-target-bucket";
        final String targetKey = "copy-nonexistent-object-target";
        
        try {
            ossClient.createBucket(existingSourceBucket);
            ossClient.createBucket(existingTargetBucket);
            
            // Try to copy object under non-existent source bucket
            try {
                ossClient.copyObject(nonexistentSourceBucket, nonexistentSourceKey, existingTargetBucket, targetKey);
                Assert.fail("Copy object should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
            }
            
            // Try to copy non-existent object under existing bucket
            try {
                ossClient.copyObject(existingSourceBucket, nonexistentSourceKey, existingTargetBucket, targetKey);
                Assert.fail("Copy object should not be successful");                
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_KEY, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_KEY_ERR));
            }
    
            try {
                byte[] content = { 'A', 'l', 'i', 'y', 'u', 'n' };
                ossClient.putObject(existingSourceBucket, existingSourceKey, new ByteArrayInputStream(content), null);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
            
            // Try to copy existing object to non-existent target bucket
            try {
                ossClient.copyObject(existingSourceBucket, existingSourceKey, nonexistentTargetBucket, targetKey);
                Assert.fail("Copy object should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
            }
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, existingSourceBucket);
            deleteBucketWithObjects(ossClient, existingTargetBucket);
        }
    }
    
    @Ignore
    public void testCopyObjectWithSpecialChars() {        
        final String sourceBucket = "copy-existing-object-source-bucket";
        final String targetBucket = "copy-existing-object-target-bucket";
        final String sourceKey = "测\\r试-中.~,+\"'*&￥#@%！（文）+字符|？/.zip";
        final String targetKey = "测\\r试-中.~,+\"'*&￥#@%！（文）+字符|？/-2.zip";
        
        final String userMetaKey0 = "user";
        final String userMetaValue0 = "阿里人";
        //TODO: With chinese characters will be failed.
        final String userMetaKey1 = "tag";
        final String userMetaValue1 = "标签1";
        final String contentType = "application/txt";
        
        try {
            ossClient.createBucket(sourceBucket);
            ossClient.createBucket(targetBucket);
            
            // Set source object different with target object and copy source bucket orignal metadata(default behavior).
            byte[] content = { 'A', 'l', 'i', 'y', 'u', 'n' };
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(content.length);
            metadata.setContentType(DEFAULT_OBJECT_CONTENT_TYPE);
            metadata.addUserMetadata(userMetaKey0, userMetaValue0);
            
            PutObjectResult putObjectResult = ossClient.putObject(sourceBucket, sourceKey, 
                    new ByteArrayInputStream(content), metadata);
            CopyObjectResult copyObjectResult = ossClient.copyObject(sourceBucket, sourceKey, 
                    targetBucket, targetKey);
            String sourceETag = putObjectResult.getETag();
            String targetETag = copyObjectResult.getETag();
            Assert.assertEquals(sourceETag, targetETag);
            
            OSSObject ossObject = ossClient.getObject(targetBucket, targetKey);
            ObjectMetadata newObjectMetadata = ossObject.getObjectMetadata();
            Assert.assertEquals(DEFAULT_OBJECT_CONTENT_TYPE, newObjectMetadata.getContentType());
            Assert.assertEquals(userMetaValue0, newObjectMetadata.getUserMetadata().get(userMetaKey0));
            
            // Set source object same as target object and replace source bucket orignal metadata.
            final String sourceBucketAsTarget = sourceBucket;
            final String sourceKeyAsTarget = sourceKey;
            newObjectMetadata = new ObjectMetadata();
            newObjectMetadata.setContentLength(content.length);
            newObjectMetadata.setContentType(contentType);
            newObjectMetadata.addUserMetadata(userMetaKey1, userMetaValue1);
            CopyObjectRequest copyObjectRequest = new CopyObjectRequest(sourceBucket, sourceKey,
                    sourceBucketAsTarget, sourceKeyAsTarget);
            copyObjectRequest.setNewObjectMetadata(newObjectMetadata);
            copyObjectResult = ossClient.copyObject(copyObjectRequest);
            Assert.assertEquals(sourceETag, copyObjectResult.getETag());
            
            ossObject = ossClient.getObject(sourceBucketAsTarget, sourceKeyAsTarget);
            newObjectMetadata = ossObject.getObjectMetadata();
            Assert.assertEquals(contentType, newObjectMetadata.getContentType());
            Assert.assertEquals(userMetaValue1, newObjectMetadata.getUserMetadata().get(userMetaKey1));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, sourceBucket);
            deleteBucketWithObjects(ossClient, targetBucket);
        }
    }
    
    @Ignore
    public void testCopyObjectWithInvalidEncryptionAlgo() {
        final String sourceBucket = "copy-object-with-invalid-encryption-algo-source-bucket";
        final String targetBucket = "copy-object-with-invalid-encryption-algo-target-bucket";
        final String sourceKey = "copy-object-with-invalid-encryption-algo-source-key";
        final String targetKey = "copy-object-with-invalid-encryption-algo-target-key";
        
        try {
            ossClient.createBucket(sourceBucket);
            ossClient.createBucket(targetBucket);
            
            final String invalidEncryptionAlgo = "Invalid-Encryption-Algo";
            try {
                CopyObjectRequest request = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
                request.setServerSideEncryption(invalidEncryptionAlgo);
                ossClient.copyObject(request);
                Assert.fail("Copy object should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.INVALID_ENCRYPTION_ALGORITHM_ERROR, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(INVALID_ENCRYPTION_ALGO_ERR));
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, sourceBucket);
            deleteBucketWithObjects(ossClient, targetBucket);
        }
    }
    
    @Ignore
    public void testCopyObjectWithMiscConstraints() throws Exception {
        final String sourceBucket = "copy-object-with-misc-constraints-source-bucket";
        final String targetBucket = "copy-object-with-misc-constraints-target-bucket";
        final String sourceKey = "copy-object-with-misc-constraints-source-key";
        final String targetKey = "copy-object-with-misc-constraints-target-key";
        
        try {
            ossClient.createBucket(sourceBucket);
            ossClient.createBucket(targetBucket);
            
            final Date beforeModifiedTime = new Date();
            Thread.sleep(1000);
            
            String eTag = null;
            try {
                PutObjectResult result = ossClient.putObject(sourceBucket, sourceKey, 
                        TestUtils.genFixedLengthInputStream(1024), null);
                eTag = result.getETag();
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
            
            // Matching ETag Constraints
            List<String> matchingETagConstraints = new ArrayList<String>();
            matchingETagConstraints.add(eTag);
            CopyObjectRequest request = new CopyObjectRequest(sourceBucket, sourceKey, targetBucket, targetKey);
            request.setMatchingETagConstraints(matchingETagConstraints);
            CopyObjectResult result = null;
            try {
                result = ossClient.copyObject(request);
                Assert.assertEquals(eTag, result.getETag());
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            } finally {
                request.clearMatchingETagConstraints();
            }
            
            matchingETagConstraints.clear();
            matchingETagConstraints.add("nonmatching-etag");
            request.setMatchingETagConstraints(matchingETagConstraints);
            try {
                result = ossClient.copyObject(request);
                Assert.fail("Copy object should not be successful.");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.PRECONDITION_FAILED, e.getErrorCode());
                //Assert.assertTrue(e.getMessage().startsWith(PRECONDITION_FAILED_ERR));
            } finally {
                request.clearMatchingETagConstraints();
            }
            
            // Non-Matching ETag Constraints
            List<String> nonmatchingETagConstraints = new ArrayList<String>();
            nonmatchingETagConstraints.add("nonmatching-etag");
            request.setNonmatchingETagConstraints(nonmatchingETagConstraints);
            try {
                result = ossClient.copyObject(request);
                Assert.assertEquals(eTag, result.getETag());
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            } finally {
                request.clearNonmatchingETagConstraints();
            }
            
            nonmatchingETagConstraints.clear();
            nonmatchingETagConstraints.add(eTag);
            request.setNonmatchingETagConstraints(nonmatchingETagConstraints);
            try {
                result = ossClient.copyObject(request);
                Assert.fail("Copy object should not be successful.");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NOT_MODIFIED, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NOT_MODIFIED_ERR));
            } finally {
                request.clearNonmatchingETagConstraints();
            }
            
            // Unmodified Since Constraint
            Date unmodifiedSinceConstraint = new Date();
            request.setUnmodifiedSinceConstraint(unmodifiedSinceConstraint);
            try {
                result = ossClient.copyObject(request);
                Assert.assertEquals(eTag, result.getETag());
            } catch (OSSException e) {
                Assert.fail(e.getMessage());
            } finally {
                request.setUnmodifiedSinceConstraint(null);
            }
            
            unmodifiedSinceConstraint = beforeModifiedTime;
            request.setUnmodifiedSinceConstraint(unmodifiedSinceConstraint);
            try {
                result = ossClient.copyObject(request);
                Assert.fail("Copy object should not be successful.");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.PRECONDITION_FAILED, e.getErrorCode());
                //Assert.assertTrue(e.getMessage().startsWith(PRECONDITION_FAILED_ERR));
            } finally {
                request.setUnmodifiedSinceConstraint(null);
            }
            
            // Modified Since Constraint
            Date modifiedSinceConstraint = beforeModifiedTime;
            request.setModifiedSinceConstraint(modifiedSinceConstraint);
            try {
                result = ossClient.copyObject(request);
                Assert.assertEquals(eTag, result.getETag());
            } catch (OSSException e) {
                Assert.fail(e.getMessage());
            } finally {
                request.setModifiedSinceConstraint(null);
            }
            
            modifiedSinceConstraint = new Date();
            request.setModifiedSinceConstraint(modifiedSinceConstraint);
            try {
                result = ossClient.copyObject(request);
                Assert.fail("Copy object should not be successful.");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NOT_MODIFIED, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NOT_MODIFIED_ERR));
            } finally {
                request.setModifiedSinceConstraint(null);
            }
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, sourceBucket);
            deleteBucketWithObjects(ossClient, targetBucket);
        }
    }
}
