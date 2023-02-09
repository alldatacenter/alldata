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

import com.aliyun.oss.model.GenericRequest;
import com.aliyun.oss.model.SetBucketRequestPaymentRequest;
import junit.framework.Assert;

import java.util.Date;
import java.util.Random;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.GetBucketRequestPaymentResult;
import com.aliyun.oss.model.Payer;

import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;

public class BucketRequestPaymentTest extends TestBase {

    @Test
    public void testNormalSetRequestPayment() {

        try {
            Payer payer = Payer.Requester;

            // Get default payer
            GetBucketRequestPaymentResult result = ossClient.getBucketRequestPayment(bucketName);
            Assert.assertEquals(Payer.BucketOwner, result.getPayer());

            // Set payer
            ossClient.setBucketRequestPayment(bucketName, payer);

            // Get payer
            result = ossClient.getBucketRequestPayment(new GenericRequest(bucketName));
            Assert.assertEquals(payer, result.getPayer());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

    }

    @Test
    public void testUnnormalSetRequestPayment() {
        Payer payer = Payer.Requester;

        // Set non-existent bucket
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        final String notExsiteBucketName = BUCKET_NAME_PREFIX + ticks;
        try {
            ossClient.setBucketRequestPayment(notExsiteBucketName, payer);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Set bucket without ownership
        final String bucketWithoutOwnership = "oss";//AccessDenied
        try {
            SetBucketRequestPaymentRequest request = new SetBucketRequestPaymentRequest(bucketWithoutOwnership);
            request.setPayer(payer);
            ossClient.setBucketRequestPayment(request);
            Assert.fail("Set bucket request payment should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }

    }


    @Test
    public void testUnnormalGetRequestPayment() {

        // Get non-existent bucket
        long ticks = new Date().getTime() / 1000 + new Random().nextInt(5000);
        final String notExsiteBucketName = BUCKET_NAME_PREFIX + ticks;
        try {
            ossClient.getBucketRequestPayment(notExsiteBucketName);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Get bucket without ownership
        final String bucketWithoutOwnership = "oss";//AccessDenied
        try {
            ossClient.getBucketRequestPayment(bucketWithoutOwnership);
            Assert.fail("Get bucket request payment should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }

    }

}
