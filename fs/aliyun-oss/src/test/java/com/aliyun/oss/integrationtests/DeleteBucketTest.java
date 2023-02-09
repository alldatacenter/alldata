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

import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.BUCKET_NOT_EMPTY_ERR;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import static com.aliyun.oss.integrationtests.TestUtils.batchPutObject;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;

public class DeleteBucketTest extends TestBase {

    @Test
    public void testDeleteExistingBucket() {
        final String bucketName = "delete-existing-bucket";
        
        try {
            ossClient.createBucket(bucketName);
            ossClient.deleteBucket(bucketName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testDeleteNonexistentBucket() {
        final String bucketName = "delete-nonexistent-bucket";
        
        try {
            ossClient.deleteBucket(bucketName);
            Assert.fail("Delete bucket should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }
    }
    
    @Test
    public void testDeleteNonemptyBucket() {
        final String bucketName = "delete-nonempty-bucket";
        final String key = "delete-nonempty-bucket-key";
        
        try {
            ossClient.createBucket(bucketName);
            
            List<String> keys = new ArrayList<String>();
            keys.add(key);
            if (!batchPutObject(ossClient, bucketName, keys)) {
                Assert.fail("batch put object failed");
            }
            
            ossClient.deleteBucket(bucketName);
            Assert.fail("Delete bucket should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.BUCKET_NOT_EMPTY, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(BUCKET_NOT_EMPTY_ERR));
        } finally {
            deleteBucketWithObjects(ossClient, bucketName);
        }
    }
    
    @Test
    public void testDeleteBucketWithoutOwnership() {
        final String bucketWithoutOwnership = "oss";
        
        try {
            ossClient.deleteBucket(bucketWithoutOwnership);
            Assert.fail("Delete bucket should not be successful.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }
    }
}
