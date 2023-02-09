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

import static com.aliyun.oss.integrationtests.TestUtils.batchPutObject;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;

public class DeleteObjectTest extends TestBase {
    
    @Test
    public void testExistingBucketAndObject() {
        List<String> existingKeys = new ArrayList<String>();
        final String existingKey = "existing-bucket-and-key\r\n<>&";
        existingKeys.add(existingKey);
        
        if (!batchPutObject(ossClient, bucketName, existingKeys)) {
            Assert.fail("batch put object failed");
        }
        
        // Delete existing object
        try {
            ossClient.deleteObject(bucketName, existingKey);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertFalse(ossClient.doesObjectExist(bucketName, existingKey));
    }
    
    @Test
    public void testExistingBucketAndNonExistentObject() {
        final String nonexistentKey = "existing-bucket-and-nonexistent-key";
        
        try {
            ossClient.deleteObject(bucketName, nonexistentKey);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testNonExistentBucketAndObject() {
        final String nonexistentBucketName = "nonexistent-bucket";
        final String nonexistentKey = "nonexistent-bucket-and-key";
        
        try {
            ossClient.deleteObject(nonexistentBucketName, nonexistentKey);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }
    }
}
