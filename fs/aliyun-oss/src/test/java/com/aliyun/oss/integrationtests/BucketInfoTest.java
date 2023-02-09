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
import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.model.BucketInfo;
import com.aliyun.oss.model.BucketList;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.Grant;
import com.aliyun.oss.model.GroupGrantee;
import com.aliyun.oss.model.ListBucketsRequest;
import com.aliyun.oss.model.Permission;

public class BucketInfoTest extends TestBase {

    @SuppressWarnings("deprecation")
	@Test
    public void testGetBucketInfo() {
        try {
            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicRead);

            BucketInfo info = ossClient.getBucketInfo(bucketName);
            Assert.assertNotNull(info.getComment());
            Assert.assertNotNull(info.getDataRedundancyType());
            Assert.assertEquals(info.getBucket().getName(), bucketName);
            Assert.assertEquals(info.getBucket().getLocation(), TestConfig.OSS_TEST_REGION);
            Assert.assertNotNull(info.getBucket().getCreationDate());
            Assert.assertTrue(info.getBucket().getExtranetEndpoint().length() > 0);
            Assert.assertTrue(info.getBucket().getIntranetEndpoint().length() > 0);
            Assert.assertTrue(info.getBucket().getOwner().getId().length() > 0);
            Assert.assertEquals(CannedAccessControlList.PublicRead, info.getCannedACL());
            Assert.assertEquals(info.getBucket().getOwner().getDisplayName(), info.getBucket().getOwner().getId());
            Assert.assertEquals(info.getGrants().size(), 1);
            Assert.assertEquals(info.getRequestId().length(), REQUEST_ID_LEN);
            for (Grant grant : info.getGrants()) {
                Assert.assertEquals(grant.getGrantee(), GroupGrantee.AllUsers);
                Assert.assertEquals(grant.getPermission(), Permission.Read);
            }

            ossClient.setBucketAcl(bucketName, CannedAccessControlList.PublicReadWrite);
            info = ossClient.getBucketInfo(bucketName);
            Assert.assertEquals(CannedAccessControlList.PublicReadWrite, info.getCannedACL());

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testListBucketWithEndpoint() {
        try {            
            ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
            listBucketsRequest.setPrefix(bucketName);
            listBucketsRequest.setMaxKeys(1);
            
            BucketList buckets = ossClient.listBuckets(listBucketsRequest);
            Assert.assertEquals(buckets.getBucketList().size(), 1);
            Assert.assertNotNull(buckets.getBucketList().get(0).getExtranetEndpoint());
            Assert.assertNotNull(buckets.getBucketList().get(0).getIntranetEndpoint());
            Assert.assertEquals(buckets.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    @Test
    public void testListBucketWithBid() {
        try {
            
            ListBucketsRequest listBucketsRequest = new ListBucketsRequest();
            listBucketsRequest.setPrefix(bucketName);
            listBucketsRequest.setMaxKeys(1);
            listBucketsRequest.setBid("26842");
            
            BucketList buckets = ossClient.listBuckets(listBucketsRequest);
            Assert.assertEquals(buckets.getBucketList().size(), 1);
            Assert.assertEquals(buckets.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testCreateBucket() {
        final String testBucketName = bucketName + "-test-_1";
        try {
            ossClient.createBucket(testBucketName);
            Assert.fail("should fail.");
        } catch (IllegalArgumentException e) {
        }

        try {
            ossClient.getBucketInfo(testBucketName);
            Assert.fail("should fail.");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
        }
    }

}
