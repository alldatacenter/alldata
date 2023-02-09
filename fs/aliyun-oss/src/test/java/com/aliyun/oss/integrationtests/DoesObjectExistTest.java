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

import static com.aliyun.oss.integrationtests.TestConstants.NOT_MODIFIED_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.PRECONDITION_FAILED_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.batchPutObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.HeadObjectRequest;
import com.aliyun.oss.model.PutObjectResult;

public class DoesObjectExistTest extends TestBase {
    
    @SuppressWarnings("deprecation")
    @Test
    public void testExistingBucketAndObject() {
        List<String> existingKeys = new ArrayList<String>();
        final String existingKey = "existing-bucket-and-key";
        existingKeys.add(existingKey);
        
        if (!batchPutObject(ossClient, bucketName, existingKeys)) {
            Assert.fail("batch put object failed");
        }
        
        try {
            boolean exist = ossClient.doesObjectExist(bucketName, existingKey);
            Assert.assertTrue(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        
        try {
            boolean exist = ossClient.doesObjectExist(bucketName, existingKey, true);
            Assert.assertTrue(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        
        try {
            boolean exist = ossClient.doesObjectExist(bucketName, existingKey, false);
            Assert.assertTrue(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        
        // Test another overrided interface
        try {
            boolean exist = ossClient.doesObjectExist(new HeadObjectRequest(bucketName, existingKey));
            Assert.assertTrue(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testExistingBucketAndNonExistentObject() {
        final String nonexistentKey = "existing-bucket-and-nonexistent-key";
        
        try {
            boolean exist = ossClient.doesObjectExist(bucketName, nonexistentKey);
            Assert.assertFalse(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            boolean exist = ossClient.doesObjectExist(bucketName, nonexistentKey, false);
            Assert.assertFalse(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testNonExistentBucketAndObject() {
        final String nonexistentBucketName = "nonexistent-bucket";
        final String nonexistentKey = "nonexistent-bucket-and-key";
        
        try {
            boolean exist = ossClient.doesObjectExist(nonexistentBucketName, nonexistentKey);
            Assert.assertFalse(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @SuppressWarnings("deprecation")
    public void testObjectWithMiscConstraints() throws Exception {
        final Date beforeModifiedTime = new Date();
        Thread.sleep(3000);
        
        final String existingKey = "object-with-misc-constraints";
        String eTag = null;
        try {
            PutObjectResult result = ossClient.putObject(bucketName, existingKey, 
                    TestUtils.genFixedLengthInputStream(1024), null);
            eTag = result.getETag();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        
        // Matching ETag Constraints
        HeadObjectRequest headObjectRequest = new HeadObjectRequest(bucketName, existingKey);
        List<String> matchingETagConstraints = new ArrayList<String>();
        matchingETagConstraints.add(eTag);
        headObjectRequest.setMatchingETagConstraints(matchingETagConstraints);
        try {
            boolean exist = ossClient.doesObjectExist(headObjectRequest);
            Assert.assertTrue(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            headObjectRequest.setMatchingETagConstraints(null);
        }
        
        matchingETagConstraints.clear();
        matchingETagConstraints.add("nonmatching-etag");
        headObjectRequest.setMatchingETagConstraints(matchingETagConstraints);
        try {
            ossClient.doesObjectExist(headObjectRequest);
            Assert.fail("Check object exist should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.PRECONDITION_FAILED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(PRECONDITION_FAILED_ERR));
        } finally {
            headObjectRequest.setMatchingETagConstraints(null);
        }
        
        // Non-Matching ETag Constraints
        List<String> nonmatchingETagConstraints = new ArrayList<String>();
        nonmatchingETagConstraints.add("nonmatching-etag");
        headObjectRequest.setNonmatchingETagConstraints(nonmatchingETagConstraints);
        try {
            boolean exist = ossClient.doesObjectExist(headObjectRequest);
            Assert.assertTrue(exist);
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            headObjectRequest.setNonmatchingETagConstraints(null);
        }
        
        nonmatchingETagConstraints.clear();
        nonmatchingETagConstraints.add(eTag);
        headObjectRequest.setNonmatchingETagConstraints(nonmatchingETagConstraints);
        try {
            ossClient.doesObjectExist(headObjectRequest);
            Assert.fail("Check object exist should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NOT_MODIFIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NOT_MODIFIED_ERR));
        } finally {
            headObjectRequest.setNonmatchingETagConstraints(null);
        }
        
        // Unmodified Since Constraint
        Date unmodifiedSinceConstraint = new Date();
        headObjectRequest.setUnmodifiedSinceConstraint(unmodifiedSinceConstraint);
        try {
            boolean exist = ossClient.doesObjectExist(headObjectRequest);
            Assert.assertTrue(exist);
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            headObjectRequest.setUnmodifiedSinceConstraint(null);
        }
        
        unmodifiedSinceConstraint = beforeModifiedTime;
        headObjectRequest.setUnmodifiedSinceConstraint(unmodifiedSinceConstraint);
        try {
            ossClient.doesObjectExist(headObjectRequest);
            Assert.fail("Check object exist should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.PRECONDITION_FAILED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(PRECONDITION_FAILED_ERR));
        } finally {
            headObjectRequest.setUnmodifiedSinceConstraint(null);
        }
        
        // Modified Since Constraint
        Date modifiedSinceConstraint = beforeModifiedTime;
        headObjectRequest.setModifiedSinceConstraint(modifiedSinceConstraint);
        try {
            boolean exist = ossClient.doesObjectExist(headObjectRequest);
            Assert.assertTrue(exist);
        } catch (OSSException e) {
            Assert.fail(e.getMessage());
        } finally {
            headObjectRequest.setModifiedSinceConstraint(null);
        }
        
        modifiedSinceConstraint = new Date();
        headObjectRequest.setModifiedSinceConstraint(modifiedSinceConstraint);
        try {
            ossClient.doesObjectExist(headObjectRequest);
            Assert.fail("Check object exist should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NOT_MODIFIED, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NOT_MODIFIED_ERR));
        } finally {
            headObjectRequest.setModifiedSinceConstraint(null);
        }
    }
    
    @Test
    public void testUnormalDoesObjectExist() {
        final String nonexistentKey = "test-unormal-does-object-exist";
        
        // SignatureDoesNotMatch 
        OSS client = new OSSClientBuilder().build(TestConfig.OSS_TEST_ENDPOINT, TestConfig.OSS_TEST_ACCESS_KEY_ID, 
                TestConfig.OSS_TEST_ACCESS_KEY_SECRET + " ");
        try {
            client.doesObjectExist(bucketName, nonexistentKey);
            Assert.fail("Does object exist should not be successful");
        } catch (OSSException ex) {
            Assert.assertEquals(OSSErrorCode.ACCESS_FORBIDDEN, ex.getErrorCode());
        } finally {
            client.shutdown();
        } 
    }

    @Test
    public void testSpecialCharDoesObjectExist() {
        final String nonexistentKey = "nonexistent-bucket-and-key+123";
        try {
            boolean exist = ossClient.doesObjectExist(bucketName, nonexistentKey);
            Assert.assertFalse(exist);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
