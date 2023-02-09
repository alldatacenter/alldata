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

import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_REGION;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.Bucket;
import com.aliyun.oss.model.BucketList;
import com.aliyun.oss.model.ListBucketsRequest;

public class ListBucketsTest extends TestBase {

    private static final int MAX_BUCKETS_ALLOWED = 10;
    
    @Test
    public void testNormalListBuckets() {
        final String bucketNamePrefix = super.bucketName + "-normal-list-buckets-";
        
        try {
            List<Bucket> returnedBuckets = ossClient.listBuckets();
            for (Bucket bkt : returnedBuckets) {
                if (bkt.getName().startsWith(bucketNamePrefix)) {
                    ossClient.deleteBucket(bkt.getName());
                }
            }
            waitForCacheExpiration(5);
            
            List<String> existingBuckets = new ArrayList<String>();
            returnedBuckets = ossClient.listBuckets();
            for (Bucket bkt : returnedBuckets) {
                if (bkt.getName().startsWith(bucketNamePrefix)) {
                    existingBuckets.add(bkt.getName());
                }
            }
            
            int remaindingAllowed = MAX_BUCKETS_ALLOWED - existingBuckets.size();            
            List<String> newlyBuckets = new ArrayList<String>();
            for (int i = 0; i < remaindingAllowed; i++) {
                String bucketName = bucketNamePrefix + i;
                try {
                    ossClient.createBucket(bucketName);
                    newlyBuckets.add(bucketName);
                    waitForCacheExpiration(5);
                    String loc = ossClient.getBucketLocation(bucketName);
                    Assert.assertEquals(OSS_TEST_REGION, loc);
                } catch (Exception e) {
                    Assert.fail(e.getMessage());
                }
            }
            
            waitForCacheExpiration(5);
            
            // List all existing buckets
            returnedBuckets = ossClient.listBuckets();
            existingBuckets.clear();
            for (Bucket bkt : returnedBuckets) {
                if (bkt.getName().startsWith(bucketNamePrefix)) {
                    existingBuckets.add(bkt.getName());
                }
            }
            Assert.assertEquals(MAX_BUCKETS_ALLOWED, existingBuckets.size());
            
            // List all existing buckets prefix with 'normal-list-buckets-'
            BucketList bucketList = ossClient.listBuckets(bucketNamePrefix, null, null);
            Assert.assertEquals(remaindingAllowed, bucketList.getBucketList().size());
            for (Bucket bkt : bucketList.getBucketList()) {
                Assert.assertTrue(bkt.getName().startsWith(bucketNamePrefix));
            }
            
            // List 'max-keys' buckets each time
            final int maxKeys = 3;
            bucketList = ossClient.listBuckets(bucketNamePrefix, null, maxKeys);
            Assert.assertTrue(bucketList.getBucketList().size() <= 3);
            returnedBuckets.clear();
            returnedBuckets.addAll(bucketList.getBucketList());
            while (bucketList.isTruncated()) {
                bucketList = ossClient.listBuckets(
                        new ListBucketsRequest(bucketNamePrefix, bucketList.getNextMarker(), maxKeys));                
                Assert.assertTrue(bucketList.getBucketList().size() <= 3);
                returnedBuckets.addAll(bucketList.getBucketList());
            }
            Assert.assertEquals(remaindingAllowed, returnedBuckets.size());
            for (Bucket bkt : returnedBuckets) {
                Assert.assertTrue(bkt.getName().startsWith(bucketNamePrefix));
            }
            
            for (String bkt : newlyBuckets) {
                ossClient.deleteBucket(bkt);
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } 
    }

    @Test
    public void testUnormalListBuckets() {
        final String nonexistentBucketNamePrefix = "nonexistent-bucket-name-prefix-";
        
        try {            
            // List all existing buckets prefix with 'nonexistent-bucket-name-prefix-'
            BucketList bucketList = ossClient.listBuckets(nonexistentBucketNamePrefix, null, null);
            Assert.assertEquals(0, bucketList.getBucketList().size());
            
            // Set 'max-keys' equal zero(MUST be between 1 and 1000)
            bucketList = ossClient.listBuckets(null, null, 0);
            Assert.fail("List bucket should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.INVALID_ARGUMENT, e.getErrorCode());
        } 
    }
    
}
