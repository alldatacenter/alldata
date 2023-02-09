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

import static com.aliyun.oss.integrationtests.TestConstants.BUCKET_ALREADY_EXIST_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.model.AccessControlList;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.Grant;
import com.aliyun.oss.model.GroupGrantee;
import com.aliyun.oss.model.Permission;
import com.aliyun.oss.model.SetBucketAclRequest;

@SuppressWarnings("deprecation")
public class BucketAclTest extends TestBase {

    private static final CannedAccessControlList[] acls = {
        null, 
        CannedAccessControlList.Private, 
        CannedAccessControlList.PublicRead, 
        CannedAccessControlList.PublicReadWrite 
    };
    
    @Test
    public void testNormalSetBucketAcl() {
        final String bucketName = super.bucketName + "normal-set-bucket-acl";
        
        try {
            ossClient.createBucket(bucketName);
            
            for (CannedAccessControlList acl : acls) {
                ossClient.setBucketAcl(bucketName, acl);
                
                AccessControlList returnedAcl = ossClient.getBucketAcl(bucketName);
                if (acl != null && !acl.equals(CannedAccessControlList.Private)) {
                    Set<Grant> grants = returnedAcl.getGrants();
                    Assert.assertEquals(1, grants.size());
                    Grant grant = (Grant) grants.toArray()[0];
                    
                    if (acl.equals(CannedAccessControlList.PublicRead)) {
                        Assert.assertEquals(GroupGrantee.AllUsers, grant.getGrantee());
                        Assert.assertEquals(Permission.Read, grant.getPermission());
                    } else if (acl.equals(CannedAccessControlList.PublicReadWrite)) {                        
                        Assert.assertEquals(GroupGrantee.AllUsers, grant.getGrantee());
                        Assert.assertEquals(Permission.FullControl, grant.getPermission());
                    }
                }
                
                Assert.assertEquals(returnedAcl.getRequestId().length(), REQUEST_ID_LEN);
                if (acl != null ) {
                    Assert.assertEquals(returnedAcl.getCannedACL(), acl);
                }
            }
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketName);
        }
    }
    
    @Test
    public void testUnormalSetBucketAcl() {
        final String nonexistentBucket = super.bucketName + "unormal-set-bucket-acl";
        
        try {            
            // set non-existent bucket
            try {
                SetBucketAclRequest request = new SetBucketAclRequest(nonexistentBucket)
                        .withCannedACL(CannedAccessControlList.Default);
                request.setCannedACL(CannedAccessControlList.Private);
                ossClient.setBucketAcl(request);
                //Assert.fail("Set bucket acl should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
            }
            
            // Set bucket without ownership
            final String bucketWithoutOwnership = "oss";
            try {
                ossClient.setBucketAcl(bucketWithoutOwnership, CannedAccessControlList.Private);
                Assert.fail("Set bucket acl should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.BUCKET_ALREADY_EXISTS, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(BUCKET_ALREADY_EXIST_ERR));
            }
            
            // Set illegal acl
            final String illegalAcl = "IllegalAcl";
            try {
                CannedAccessControlList.parse(illegalAcl);
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }
            
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(nonexistentBucket);
        }
    }
    
    @Test
    public void testUnormalGetBucketAcl() {
        // Get non-existent bucket
        final String nonexistentBucket = super.bucketName + "unormal-get-bucket-acl";
        try {
            ossClient.getBucketAcl(nonexistentBucket);
            Assert.fail("Get bucket acl should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }
        
        // Get bucket without ownership
        final String bucketWithoutOwnership = "oss";
        try {
            ossClient.getBucketAcl(bucketWithoutOwnership);
            Assert.fail("Get bucket referer should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
        }
        
        // Get bucket using default acl
        final String bucketUsingDefaultAcl = "bucket-using-default-acl";
        try {
            ossClient.createBucket(bucketUsingDefaultAcl);
            
            AccessControlList returnedACL = ossClient.getBucketAcl(bucketUsingDefaultAcl);
            Set<Grant> grants = returnedACL.getGrants();
            // No grants when using default acl
            Assert.assertEquals(0, grants.size());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(bucketUsingDefaultAcl);
        }
    }
    
    @Test
    public void testUnormalDoesBucketExist() {
        final String nonexistentBucket = super.bucketName + "unormal-does-bucket-exist";
        
        try {
            Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
            OSSClient ossClient = new OSSClient("http://oss-cn-taikang.aliyuncs.com", new DefaultCredentialProvider(credentials));
            ossClient.doesBucketExist(nonexistentBucket);
            Assert.fail("Does bucket exist should not be successful");
        } catch (Exception e) {
            Assert.assertEquals(nonexistentBucket + ".oss-cn-taikang.aliyuncs.com\n" +
                    "[ErrorCode]: UnknownHost\n" +
                    "[RequestId]: Unknown", e.getMessage());
        }
    }
        
}
