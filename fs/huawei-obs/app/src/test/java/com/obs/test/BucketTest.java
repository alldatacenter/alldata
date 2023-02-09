/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.test;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.AvailableZoneEnum;
import com.obs.services.model.BucketCustomDomainInfo;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.ListBucketsResult;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.ReplicationConfiguration.Destination;
import com.obs.services.model.ReplicationConfiguration.Rule;
import com.obs.services.model.RuleStatusEnum;
import com.obs.services.model.SetBucketReplicationRequest;
import com.obs.services.model.SetBucketVersioningRequest;
import com.obs.services.model.VersioningStatusEnum;
import com.obs.services.model.fs.NewBucketRequest;
import com.obs.services.model.fs.ObsFSBucket;
import com.obs.test.tools.PropertiesTools;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class BucketTest {
    private static final File file = new File("./app/src/test/resource/test_data.properties");
    private static final ArrayList<String> createdBuckets = new ArrayList<>();

    @BeforeClass
    public static void create_demo_bucket() throws IOException {
        String beforeBucket = PropertiesTools.getInstance(file).getProperties("beforeBucket");
        String location = PropertiesTools.getInstance(file).getProperties("environment.location");
        CreateBucketRequest request = new CreateBucketRequest();
        request.setBucketName(beforeBucket);
        request.setBucketType(BucketTypeEnum.OBJECT);
        request.setLocation(location);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        HeaderResponse response = obsClient.createBucket(request);
        assertEquals(200, response.getStatusCode());
        createdBuckets.add(beforeBucket);
    }

    @AfterClass
    public static void delete_created_buckets() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        for (String bucket : createdBuckets) {
            obsClient.deleteBucket(bucket);
        }
    }

    @Test
    public void test_create_bucket_obs() throws IOException {
        String bucketName = PropertiesTools.getInstance(file).getProperties("bucketPrefix")
                + "creat-bucket001";
        String location = PropertiesTools.getInstance(file).getProperties("environment.location");
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        CreateBucketRequest request = new CreateBucketRequest();
        request.setBucketName(bucketName);
        request.setBucketType(BucketTypeEnum.OBJECT);
        request.setLocation(location);
        HeaderResponse response = obsClient.createBucket(request);

        assertEquals(200, response.getStatusCode());
        createdBuckets.add(bucketName);
    }

    @Test
    public void test_create_bucket_3az() throws IOException {
        String bucketName = PropertiesTools.getInstance(file).getProperties("bucketPrefix")
                + "test-sdk-obs-3az";
        String location = PropertiesTools.getInstance(file).getProperties("location");
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        CreateBucketRequest request = new CreateBucketRequest();
        request.setBucketName(bucketName);
        request.setBucketType(BucketTypeEnum.OBJECT);
        request.setAvailableZone(AvailableZoneEnum.MULTI_AZ);
        request.setLocation(location);
        HeaderResponse response = obsClient.createBucket(request);

        assertEquals(200, response.getStatusCode());
        createdBuckets.add(bucketName);
    }

    @Test
    public void test_create_bucket_pfs() throws IOException {
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String bucketName = PropertiesTools.getInstance(file).getProperties("bucketPrefix")
                + "test-sdk-pfs";
        String location = PropertiesTools.getInstance(file).getProperties("environment.location");

        CreateBucketRequest request = new CreateBucketRequest();
        request.setBucketName(bucketName);
        request.setBucketType(BucketTypeEnum.PFS);
        request.setLocation(location);
        HeaderResponse response = obsClient.createBucket(request);

        assertEquals(200, response.getStatusCode());
        createdBuckets.add(bucketName);
    }

    @Test
    public void test_create_bucket_new_bucket() throws IOException {
        String bucketName = PropertiesTools.getInstance(file).getProperties("bucketPrefix")
                + "test-sdk-pfs-2";
        String location = PropertiesTools.getInstance(file).getProperties("environment.location");
//        ObsClient obsClient = TestTools.getExternalEnvironment();
        ObsClient obsClient = TestTools.getPipelineEnvironment();

//        CreateBucketRequest request = new CreateBucketRequest();
//        request.setBucketName("test-sdk-pfs-0000003");
//        request.setBucketType(BucketTypeEnum.PFS);
//        request.setLocation("cn-north-4");

        NewBucketRequest request = new NewBucketRequest(bucketName, location);
        ObsFSBucket response = obsClient.newBucket(request);

        assertEquals(bucketName, response.getBucketName());
        createdBuckets.add(bucketName);
    }

    @Test
    public void test_head_bucket() throws IOException {
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String beforeBucket = PropertiesTools.getInstance(file).getProperties("beforeBucket");
        boolean result = obsClient.headBucket(beforeBucket);

        assertTrue(result);
    }

    @Test
    public void test_list_bucket() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        ListBucketsResult result = obsClient.listBucketsV2(null);

        assertEquals(result.getBuckets().size(), 5);
    }

    @Test
    public void test_list_objects_in_bucket() throws IOException {

        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String beforeBucket = PropertiesTools.getInstance(file).getProperties("beforeBucket");

        ObjectListing result = obsClient.listObjects(beforeBucket);

        assertEquals(result.getBucketName(), beforeBucket);

        assertEquals(result.getObjects().size(), 1000);
    }

    @Test
    public void test_set_bucket_version() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        String bucketName = "putobject-bucket-0603051314";

        SetBucketVersioningRequest request = new SetBucketVersioningRequest(bucketName, VersioningStatusEnum.ENABLED);
        HeaderResponse result = obsClient.setBucketVersioning(bucketName, new BucketVersioningConfiguration(VersioningStatusEnum.ENABLED));
        assertEquals(result.getStatusCode(), 200);
        BucketVersioningConfiguration config = obsClient.getBucketVersioning(bucketName);
        assertEquals(config.getVersioningStatus().getCode(), "Enabled");

        result = obsClient.setBucketVersioning(bucketName, new BucketVersioningConfiguration(VersioningStatusEnum.SUSPENDED));
        assertEquals(result.getStatusCode(), 200);
        config = obsClient.getBucketVersioning(bucketName);
        assertEquals(config.getVersioningStatus().getCode(), "Suspended");
    }

    @Test(expected = ObsException.class)
    public void test_set_bucket_version_exception() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        String bucketName = "putobject-bucket-0603051314";

        HeaderResponse result = obsClient.setBucketVersioning(bucketName, new BucketVersioningConfiguration(VersioningStatusEnum.getValueFromCode("ERROR")));
    }

    @Test
    public void test_set_bucket_replication() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        ReplicationConfiguration replicationConfiguration = new ReplicationConfiguration();
        Rule rule = new Rule();
        rule.setId("test_rule11"); // id为一个自定义的唯一字符串
        rule.setPrefix("test");    // 前缀
        rule.setStatus(RuleStatusEnum.ENABLED);
        Destination dest = new Destination();
        dest.setBucket("sadfxx-xxx-xxx"); // 目标桶名称
        rule.setDestination(dest);
        replicationConfiguration.getRules().add(rule);
        replicationConfiguration.setAgency("obs-mirror"); // IAM委托名称

        SetBucketReplicationRequest request = new SetBucketReplicationRequest("wwwwwwweeee", replicationConfiguration);


        obsClient.setBucketReplication(request);
    }

    @Test
    public void test_get_bucket_replication() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();

        ReplicationConfiguration replicationConfiguration = obsClient.getBucketReplication("wwwwwwweeee");
        System.out.println(replicationConfiguration.toString());
    }
/**
     * 测试获取自定义域名
     */
    @Test
    public void test_get_bucket_customdomain() {
        ObsClient obsClient = TestTools.getExternalEnvironment();

        BucketCustomDomainInfo bucketCustomDomainInfo = obsClient.getBucketCustomDomain("testBucket");
        System.out.println(bucketCustomDomainInfo.toString());
    }

    /**
     * 测试设置自定义域名
     */
    @Test
    public void test_set_bucket_customdomain() {
        ObsClient obsClient = TestTools.getExternalEnvironment();

//        BucketCustomDomainInfo bucketCustomDomainInfo = obsClient.getBucketCustomDomain("shenqing-oms-test-003");

        HeaderResponse response = obsClient.setBucketCustomDomain("testBucket", "test.huawei.com");
        System.out.println(response.toString());
    }

    /**
     * 测试删除桶自定义域名
     */
    @Test
    public void test_delete_bucket_customdomain() {
        ObsClient obsClient = TestTools.getExternalEnvironment();

        HeaderResponse response = obsClient.deleteBucketCustomDomain("testBucket", "test.huawei.com");
        System.out.println(response.toString());
    }


    @Test
    public void test_close_obsclient() {
        ObsClient obsClient = TestTools.getPipelineEnvironment();


        String bucketName = "putobject-bucket-0603051314";

        obsClient.headBucket(bucketName);

        try {
            obsClient.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        obsClient.headBucket(bucketName);
    }
}
