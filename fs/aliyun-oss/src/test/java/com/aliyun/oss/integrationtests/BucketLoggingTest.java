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
import static com.aliyun.oss.integrationtests.TestConstants.INVALID_TARGET_BUCKET_FOR_LOGGING_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.BucketLoggingResult;
import com.aliyun.oss.model.SetBucketLoggingRequest;

public class BucketLoggingTest extends TestBase {

    @Test
    public void testNormalSetBucketLogging() {
        final String sourceBucket = "normal-set-bucket-logging-source-1";
        final String targetBucket = "normal-set-bucket-logging-target-1";
        final String targetPrefix = "normal-set-bucket-logging-prefix";

        try {
            ossClient.createBucket(sourceBucket);
            ossClient.createBucket(targetBucket);

            // Set target bucket not same as source bucket
            SetBucketLoggingRequest request = new SetBucketLoggingRequest(sourceBucket);
            request.setTargetBucket(targetBucket);
            request.setTargetPrefix(targetPrefix);
            ossClient.setBucketLogging(request);

            BucketLoggingResult result = ossClient.getBucketLogging(sourceBucket);
            Assert.assertEquals(targetBucket, result.getTargetBucket());
            Assert.assertEquals(targetPrefix, result.getTargetPrefix());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            ossClient.deleteBucketLogging(sourceBucket);

            // Set target bucket same as source bucket
            request.setTargetBucket(sourceBucket);
            request.setTargetPrefix(targetPrefix);
            ossClient.setBucketLogging(request);

            waitForCacheExpiration(5);

            result = ossClient.getBucketLogging(sourceBucket);
            Assert.assertEquals(sourceBucket, result.getTargetBucket());
            Assert.assertEquals(targetPrefix, result.getTargetPrefix());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            ossClient.deleteBucketLogging(sourceBucket);

            // Set target prefix null
            request.setTargetBucket(targetBucket);
            request.setTargetPrefix(null);
            ossClient.setBucketLogging(request);

            result = ossClient.getBucketLogging(sourceBucket);
            Assert.assertEquals(targetBucket, result.getTargetBucket());
            Assert.assertTrue(result.getTargetPrefix().isEmpty());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            ossClient.deleteBucketLogging(sourceBucket);

            // Close bucket logging functionality
            request.setTargetBucket(null);
            request.setTargetPrefix(null);
            ossClient.setBucketLogging(request);

            result = ossClient.getBucketLogging(sourceBucket);
            Assert.assertTrue(result.getTargetBucket() == null);
            Assert.assertTrue(result.getTargetPrefix() == null);
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(sourceBucket);
            ossClient.deleteBucket(targetBucket);
        }
    }

    @Test
    public void testUnormalSetBucketLogging() {
        final String sourceBucket = "unormal-set-bucket-logging-source";
        final String targetBucket = "unormal-set-bucket-logging-target";
        final String targetPrefix = "unormal-set-bucket-logging-prefix";

        try {
            ossClient.createBucket(sourceBucket);
            ossClient.createBucket(targetBucket);

            // Set non-existent source bucket 
            final String nonexistentSourceBucket = "nonexistent-source-bucket";
            try {
                SetBucketLoggingRequest request = new SetBucketLoggingRequest(nonexistentSourceBucket)
                        .withTargetBucket(targetBucket)
                        .withTargetPrefix(targetPrefix);
                request.setTargetBucket(targetBucket);
                request.setTargetPrefix(targetPrefix);
                ossClient.setBucketLogging(request);

                Assert.fail("Set bucket logging should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
            }

            // Set non-existent target bucket 
            final String nonexistentTargetBucket = "nonexistent-target-bucket";
            try {
                SetBucketLoggingRequest request = new SetBucketLoggingRequest(sourceBucket);
                request.setTargetBucket(nonexistentTargetBucket);
                request.setTargetPrefix(targetPrefix);
                ossClient.setBucketLogging(request);

                Assert.fail("Set bucket logging should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.INVALID_TARGET_BUCKET_FOR_LOGGING, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(INVALID_TARGET_BUCKET_FOR_LOGGING_ERR));
            }

        } finally {
            ossClient.deleteBucket(sourceBucket);
            ossClient.deleteBucket(targetBucket);
        }
    }

    @Test
    public void testUnormalGetBucketLogging() {
        // Get non-existent bucket
        final String nonexistentBucket = "unormal-get-bucket-logging";
        try {
            ossClient.getBucketLogging(nonexistentBucket);
            Assert.fail("Get bucket logging should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Get bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            ossClient.getBucketLogging(bucketWithoutOwnership);
            Assert.fail("Get bucket logging should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }

        // Get bucket without setting logging rule
        final String bucketWithoutLoggingRule = "bucket-without-logging-rule";
        try {
            ossClient.createBucket(bucketWithoutLoggingRule);

            BucketLoggingResult result = ossClient.getBucketLogging(bucketWithoutLoggingRule);
            Assert.assertTrue(result.getTargetBucket() == null);
            Assert.assertTrue(result.getTargetPrefix() == null);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketWithoutLoggingRule);
        }
    }

    @Test
    public void testUnormalDeleteBucketLogging() {
        // Delete non-existent bucket
        final String nonexistentBucket = "unormal-delete-bucket-logging";
        try {
            ossClient.deleteBucketLogging(nonexistentBucket);
            Assert.fail("Delete bucket logging should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Delete bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            ossClient.deleteBucketLogging(bucketWithoutOwnership);
            Assert.fail("Delete bucket logging should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }

        // Delete bucket without setting logging rule
        final String bucketWithoutLoggingRule = "bucket-without-logging-rule";
        try {
            ossClient.createBucket(bucketWithoutLoggingRule);

            ossClient.deleteBucketLogging(bucketWithoutLoggingRule);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketWithoutLoggingRule);
        }
    }
}
