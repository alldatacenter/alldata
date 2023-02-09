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

import java.util.List;

import org.junit.Ignore;

import com.aliyun.oss.model.CreateBucketVpcipRequest;
import com.aliyun.oss.model.CreateVpcipRequest;
import com.aliyun.oss.model.CreateVpcipResult;
import com.aliyun.oss.model.DeleteBucketVpcipRequest;
import com.aliyun.oss.model.DeleteVpcipRequest;
import com.aliyun.oss.model.GenericRequest;
import com.aliyun.oss.model.VpcPolicy;
import com.aliyun.oss.model.Vpcip;

import junit.framework.Assert;
import org.junit.Test;

public class VpcipTest extends TestBase {

    private static final String VPCIP_NAME = "vsw_id_test";
    private static final String VPCIP_LABEL = "oss_java_test";

    @Ignore
    public void testAutoVpcip() {
        try {
            String region = TestConfig.OSS_TEST_REGION;
            CreateVpcipRequest createVpcipRequest = new CreateVpcipRequest();
            createVpcipRequest.setRegion(region);
            createVpcipRequest.setVSwitchId(VPCIP_NAME);
            createVpcipRequest.setLabel(VPCIP_LABEL);
            CreateVpcipResult createVpcipResult = ossClient.createVpcip(createVpcipRequest);
            Vpcip vpcipResult = createVpcipResult.getVpcip();
            Assert.assertNotNull(vpcipResult.getRegion());
            Assert.assertNotNull(vpcipResult.getVpcId());
            Assert.assertNotNull(vpcipResult.getVip());
            Assert.assertNotNull(vpcipResult.getLabel());

            List<Vpcip> vpcipList = ossClient.listVpcip();

            for (Vpcip vpcip : vpcipList) {
                Assert.assertNotNull(vpcip.getRegion());
                Assert.assertNotNull(vpcip.getVpcId());
                Assert.assertNotNull(vpcip.getVip());
                Assert.assertNotNull(vpcip.getLabel());
            }

            DeleteVpcipRequest deleteVpcipRequest = new DeleteVpcipRequest();
            VpcPolicy vpcPolicy = new VpcPolicy();
            vpcPolicy.setRegion(vpcipResult.getRegion());
            vpcPolicy.setVpcId(vpcipResult.getVpcId());
            vpcPolicy.setVip(vpcipResult.getVip());
            deleteVpcipRequest.setVpcPolicy(vpcPolicy);
            ossClient.deleteVpcip(deleteVpcipRequest);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Ignore
    public void testAutoBucketVpcip() {
        try {
            CreateVpcipRequest createVpcipRequest = new CreateVpcipRequest();
            createVpcipRequest.setRegion(TestConfig.OSS_TEST_REGION);
            createVpcipRequest.setVSwitchId(VPCIP_NAME);
            CreateVpcipResult createVpcipResult = ossClient.createVpcip(createVpcipRequest);
            Vpcip vpcipResult = createVpcipResult.getVpcip();
            Assert.assertNotNull(vpcipResult.getRegion());
            Assert.assertNotNull(vpcipResult.getVpcId());
            Assert.assertNotNull(vpcipResult.getVip());

            try {
                CreateBucketVpcipRequest vpcipRequest = new CreateBucketVpcipRequest();
                vpcipRequest.setBucketName(bucketName);
                VpcPolicy vpcPolicy = new VpcPolicy();
                vpcPolicy.setRegion(vpcipResult.getRegion());
                vpcPolicy.setVpcId(vpcipResult.getVpcId());
                vpcPolicy.setVip(vpcipResult.getVip());
                vpcipRequest.setVpcPolicy(vpcPolicy);
                System.out.println(vpcipRequest.toString());
                ossClient.createBucketVpcip(vpcipRequest);

                GenericRequest genericRequest = new GenericRequest();
                genericRequest.setBucketName(bucketName);
                List<VpcPolicy> vpcPolicys = ossClient.getBucketVpcip(genericRequest);
                Assert.assertEquals(vpcPolicys.size(), 1);
                for (VpcPolicy vpcPolicyBucket : vpcPolicys) {
                    Assert.assertNotNull(vpcPolicyBucket.getRegion());
                    Assert.assertNotNull(vpcPolicyBucket.getVpcId());
                    Assert.assertNotNull(vpcPolicyBucket.getVip());
                    System.out.println(vpcPolicyBucket.toString());
                }

                DeleteBucketVpcipRequest deleteBucketVpcipRequest = new DeleteBucketVpcipRequest();
                deleteBucketVpcipRequest.setBucketName(bucketName);
                deleteBucketVpcipRequest.setVpcPolicy(vpcPolicy);
                ossClient.deleteBucketVpcip(deleteBucketVpcipRequest);

                GenericRequest genericRequest2 = new GenericRequest();
                genericRequest.setBucketName(bucketName);
                List<VpcPolicy> vpcPolicys2 = ossClient.getBucketVpcip(genericRequest2);
                Assert.assertEquals(vpcPolicys2.size(), 0);
                for (VpcPolicy vpcPolicyBucket : vpcPolicys2) {
                    Assert.assertNotNull(vpcPolicyBucket.getRegion());
                    Assert.assertNotNull(vpcPolicyBucket.getVpcId());
                    Assert.assertNotNull(vpcPolicyBucket.getVip());
                    System.out.println(vpcPolicyBucket.toString());
                }

            } catch (Exception e) {
                e.printStackTrace();
                Assert.fail(e.getMessage());
            } finally {
                if (null != vpcipResult) {
                    DeleteVpcipRequest deleteVpcipRequest = new DeleteVpcipRequest();
                    VpcPolicy vpcPolicy = new VpcPolicy();
                    vpcPolicy.setRegion(vpcipResult.getRegion());
                    vpcPolicy.setVpcId(vpcipResult.getVpcId());
                    vpcPolicy.setVip(vpcipResult.getVip());
                    deleteVpcipRequest.setVpcPolicy(vpcPolicy);
                    ossClient.deleteVpcip(deleteVpcipRequest);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNonVpcEnvironment() {
        // update coverage.
        try {
            CreateVpcipRequest createVpcipRequest = new CreateVpcipRequest();
            createVpcipRequest.setRegion(TestConfig.OSS_TEST_REGION);
            createVpcipRequest.setVSwitchId(VPCIP_NAME);
            ossClient.createVpcip(createVpcipRequest);
        } catch (Exception e) {
            // expected exception
        }

        try {
            CreateBucketVpcipRequest vpcipRequest = new CreateBucketVpcipRequest();
            vpcipRequest.setBucketName(bucketName);
            VpcPolicy vpcPolicy = new VpcPolicy();
            vpcPolicy.setVpcId("test-vpc-id");
            vpcPolicy.setVip("test-vip");
            vpcipRequest.setVpcPolicy(vpcPolicy);
            ossClient.createBucketVpcip(vpcipRequest);
        } catch (Exception e) {
            // expected exception
        }

        try {
            GenericRequest genericRequest = new GenericRequest();
            genericRequest.setBucketName(bucketName);
            ossClient.getBucketVpcip(genericRequest);
        } catch (Exception e) {
            // expected exception
        }

        try {
            DeleteBucketVpcipRequest deleteBucketVpcipRequest = new DeleteBucketVpcipRequest();
            deleteBucketVpcipRequest.setBucketName(bucketName);
            VpcPolicy vpcPolicy = new VpcPolicy();
            vpcPolicy.setVpcId("test-vpc-id");
            vpcPolicy.setVip("test-vip");
            deleteBucketVpcipRequest.setVpcPolicy(vpcPolicy);
            ossClient.deleteBucketVpcip(deleteBucketVpcipRequest);
        } catch (Exception e) {
            // expected exception
        }

        try {
            DeleteVpcipRequest deleteVpcipRequest = new DeleteVpcipRequest();
            VpcPolicy vpcPolicy = new VpcPolicy();
            vpcPolicy.setVpcId("test-vpc-id");
            vpcPolicy.setVip("test-vip");
            deleteVpcipRequest.setVpcPolicy(vpcPolicy);
            ossClient.deleteVpcip(deleteVpcipRequest);
        } catch (Exception e) {
            // expected exception
        }

        try {
            ossClient.listVpcip();
        } catch (Exception e) {
            // expected exception
        }
    }

}
