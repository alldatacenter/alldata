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

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ExtendBucketWormRequest;
import com.aliyun.oss.model.GetBucketWormResult;
import com.aliyun.oss.model.InitiateBucketWormRequest;
import com.aliyun.oss.model.InitiateBucketWormResult;
import junit.framework.Assert;
import org.junit.Test;


public class BucketWormTest extends TestBase {

    @Test
    public void testBucketWormNormal() {
        String bucketName = super.bucketName + "test-bucket-worm-normal";

        try {
            ossClient.createBucket(bucketName);
            InitiateBucketWormRequest initiateBucketWormRequest = new InitiateBucketWormRequest(bucketName);

            initiateBucketWormRequest.setRetentionPeriodInDays(1);
            InitiateBucketWormResult initiateBucketWormResult = ossClient.initiateBucketWorm(initiateBucketWormRequest);
            String wormId = initiateBucketWormResult.getWormId();

            GetBucketWormResult getBucketWormResult = ossClient.getBucketWorm(bucketName);
            Assert.assertEquals(wormId, getBucketWormResult.getWormId());
            Assert.assertEquals("InProgress", getBucketWormResult.getWormState());
            Assert.assertEquals(1, getBucketWormResult.getRetentionPeriodInDays());
            Assert.assertNotNull(getBucketWormResult.getCreationDate());

            ossClient.completeBucketWorm(bucketName, wormId);
            getBucketWormResult = ossClient.getBucketWorm(bucketName);
            Assert.assertEquals(wormId, getBucketWormResult.getWormId());
            Assert.assertEquals("Locked", getBucketWormResult.getWormState());
            Assert.assertEquals(1, getBucketWormResult.getRetentionPeriodInDays());
            Assert.assertNotNull(getBucketWormResult.getCreationDate());

            ossClient.extendBucketWorm(bucketName, wormId, 2);
            getBucketWormResult = ossClient.getBucketWorm(bucketName);
            Assert.assertEquals(wormId, getBucketWormResult.getWormId());
            Assert.assertEquals("Locked", getBucketWormResult.getWormState());
            Assert.assertEquals(2, getBucketWormResult.getRetentionPeriodInDays());
            Assert.assertNotNull(getBucketWormResult.getCreationDate());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }

    @Test
    public void testAbortBucketWorm() {
        final String bucketName = super.bucketName + "test-abort-bucket-worm";
        ossClient.createBucket(bucketName);

        try {
            InitiateBucketWormRequest initiateBucketWormRequest = new InitiateBucketWormRequest(bucketName);
            initiateBucketWormRequest.setRetentionPeriodInDays(1);
            ossClient.initiateBucketWorm(initiateBucketWormRequest);
            ossClient.abortBucketWorm(bucketName);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        try {
            InitiateBucketWormRequest initiateBucketWormRequest = new InitiateBucketWormRequest(bucketName, 2);
            InitiateBucketWormResult result = ossClient.initiateBucketWorm(initiateBucketWormRequest);
            ossClient.completeBucketWorm(bucketName, result.getWormId());
            ossClient.abortBucketWorm(bucketName);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.WORM_CONFIGURATION_LOCKED, e.getErrorCode());
        }
        ossClient.deleteBucket(bucketName);
    }

    @Test
    public void testExtendBucketWormIllegal() {
        final String bucketName = super.bucketName + "test-extent-bucket-worm-illegal";
        ossClient.createBucket(bucketName);

        try {
            InitiateBucketWormRequest initiateBucketWormRequest = new InitiateBucketWormRequest(bucketName, 1);
            InitiateBucketWormResult result = ossClient.initiateBucketWorm(initiateBucketWormRequest);

            ossClient.completeBucketWorm(bucketName, result.getWormId());

            ExtendBucketWormRequest extendBucketWormRequest = new ExtendBucketWormRequest(bucketName)
                    .withRetentionPeriodInDays(2)
                    .withWormId(result.getWormId());
            ossClient.extendBucketWorm(extendBucketWormRequest);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_WORM_CONFIGURATION, e.getErrorCode());
        }

        ossClient.deleteBucket(bucketName);
    }

}
