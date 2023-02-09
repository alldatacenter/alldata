package com.qcloud.cos;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.AccessControlList;
import com.qcloud.cos.model.Bucket;
import com.qcloud.cos.model.BucketVersioningConfiguration;
import com.qcloud.cos.model.CannedAccessControlList;
import com.qcloud.cos.model.CreateBucketRequest;
import com.qcloud.cos.model.Grantee;
import com.qcloud.cos.model.HeadBucketRequest;
import com.qcloud.cos.model.Permission;
import com.qcloud.cos.model.UinGrantee;

public class CreateDeleteHeadBucketTest extends AbstractCOSClientTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void testCreateDeleteBucketPublicRead() throws Exception {
        if (!judgeUserInfoValid()) {
            return;
        }
        try {
            String bucketName = String.format("java-pubr-%s", appid);
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            createBucketRequest.setCannedAcl(CannedAccessControlList.PublicRead);
            Bucket bucket = cosclient.createBucket(createBucketRequest);
            assertEquals(bucketName, bucket.getName());

            cosclient.headBucket(new HeadBucketRequest(bucketName));

            BucketVersioningConfiguration bucketVersioningConfiguration =
                    cosclient.getBucketVersioningConfiguration(bucketName);
            assertEquals(BucketVersioningConfiguration.OFF,
                    bucketVersioningConfiguration.getStatus());

            cosclient.deleteBucket(bucketName);
            // 删除bucket后, 由于server端有缓存 需要稍后查询, 这里sleep 5 秒
            Thread.sleep(5000L);
            assertFalse(cosclient.doesBucketExist(bucketName));
        } catch (CosServiceException cse) {
            fail(cse.toString());
        }
    }

    @Test
    public void testCreateDeleteBucketPublicReadWrite() throws Exception {
        if (!judgeUserInfoValid()) {
            return;
        }
        try {
            String bucketName = String.format("java-pubrw-%s", appid);
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            createBucketRequest.setCannedAcl(CannedAccessControlList.PublicReadWrite);
            AccessControlList accessControlList = new AccessControlList();
            Grantee grantee = new UinGrantee("730123456");
            accessControlList.grantPermission(grantee, Permission.Write);
            createBucketRequest.setAccessControlList(accessControlList);
            Bucket bucket = cosclient.createBucket(createBucketRequest);
            assertEquals(bucketName, bucket.getName());

            assertTrue(cosclient.doesBucketExist(bucketName));

            BucketVersioningConfiguration bucketVersioningConfiguration =
                    cosclient.getBucketVersioningConfiguration(bucketName);
            assertEquals(BucketVersioningConfiguration.OFF,
                    bucketVersioningConfiguration.getStatus());

            cosclient.deleteBucket(bucketName);
            // 删除bucket后, 由于server端有缓存 需要稍后查询, 这里sleep 5 秒
            Thread.sleep(5000L);
            assertFalse(cosclient.doesBucketExist(bucketName));
        } catch (CosServiceException cse) {
            fail(cse.toString());
        }
    }

    @Test
    public void testCreateDeleteBucketPrivate() throws Exception {
        if (!judgeUserInfoValid()) {
            return;
        }
        try {
            String bucketName = String.format("java-pri-%s", appid);
            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
            createBucketRequest.setCannedAcl(CannedAccessControlList.Private);
            Bucket bucket = cosclient.createBucket(createBucketRequest);
            assertEquals(bucketName, bucket.getName());

            assertTrue(cosclient.doesBucketExist(bucketName));

            BucketVersioningConfiguration bucketVersioningConfiguration =
                    cosclient.getBucketVersioningConfiguration(bucketName);
            assertEquals(BucketVersioningConfiguration.OFF,
                    bucketVersioningConfiguration.getStatus());

            cosclient.deleteBucket(bucketName);
            // 删除bucket后, 由于server端有缓存 需要稍后查询, 这里sleep 5 秒
            Thread.sleep(5000L);
            assertFalse(cosclient.doesBucketExist(bucketName));
        } catch (CosServiceException cse) {
            fail(cse.toString());
        }
    }
    
    @Test
    public void testCreateBucketWithNameWithUpperCaseLetter() throws Exception {
        if (!judgeUserInfoValid()) {
            return;
        }
        try {
            String bucketName = "Awwww123";
            cosclient.createBucket(bucketName);
        } catch (IllegalArgumentException ilegalException) {
            return;
        } catch (Exception e) {
            fail(e.toString());
        }
    }
    
    @Test
    public void testCreateBucketWithNameStartWithDelimiter() throws Exception {
        if (!judgeUserInfoValid()) {
            return;
        }
        try {
            String bucketName = String.format("-hello-%s", appid);
            cosclient.createBucket(bucketName);
        } catch (IllegalArgumentException iae) {
            return;
        } catch (Exception e) {
            fail(e.toString());
        }
    }

}
