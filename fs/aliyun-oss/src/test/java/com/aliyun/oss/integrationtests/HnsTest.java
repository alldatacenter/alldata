package com.aliyun.oss.integrationtests;


import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.BucketInfo;
import com.aliyun.oss.model.CreateBucketRequest;
import com.aliyun.oss.model.CreateDirectoryRequest;
import com.aliyun.oss.model.DeleteDirectoryRequest;
import com.aliyun.oss.model.DeleteDirectoryResult;
import com.aliyun.oss.model.HnsStatus;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.Payer;
import com.aliyun.oss.model.RenameObjectRequest;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.List;

public class HnsTest extends TestBase {
    private String testHnsBucket;
    private OSS testClient;

    @Test
    public void testBucketRelatedConstructor() {
        String bucketName = "tes-bucket-hns";
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName);
        Assert.assertNull(createBucketRequest.getHnsStatus());
        createBucketRequest.setHnsStatus(HnsStatus.Enabled);
        Assert.assertEquals(HnsStatus.Enabled.toString(), createBucketRequest.getHnsStatus());
        createBucketRequest.setHnsStatus(HnsStatus.Enabled.toString());
        Assert.assertEquals(HnsStatus.Enabled.toString(), createBucketRequest.getHnsStatus());

        createBucketRequest = new CreateBucketRequest(bucketName).withHnsStatus(HnsStatus.Enabled);
        Assert.assertEquals(HnsStatus.Enabled.toString(), createBucketRequest.getHnsStatus());
        createBucketRequest = new CreateBucketRequest(bucketName).withHnsStatus(HnsStatus.Enabled.toString());
        Assert.assertEquals(HnsStatus.Enabled.toString(), createBucketRequest.getHnsStatus());
    }

    @Test
    public void testObjectRelatedConstructor() {
        String bucketName = "tes-bucket-hns";
        String objectName = "test-obj";
        String directory = "test-dir";

        CreateDirectoryRequest createDirectoryRequest = new CreateDirectoryRequest(bucketName, directory);
        Assert.assertEquals(directory, createDirectoryRequest.getDirectoryName());
        Assert.assertEquals(directory, createDirectoryRequest.getKey());
        createDirectoryRequest.setDirectoryName("123");
        Assert.assertEquals("123", createDirectoryRequest.getDirectoryName());

        DeleteDirectoryRequest deleteDirectoryRequest = new DeleteDirectoryRequest(bucketName, directory);
        Assert.assertEquals(bucketName, deleteDirectoryRequest.getBucketName());
        Assert.assertEquals(directory, deleteDirectoryRequest.getDirectoryName());
        Assert.assertEquals(directory, deleteDirectoryRequest.getKey());
        Assert.assertFalse(deleteDirectoryRequest.isDeleteRecursive());
        Assert.assertNull(deleteDirectoryRequest.getNextDeleteToken());

        deleteDirectoryRequest = new DeleteDirectoryRequest(bucketName, directory, true, "test-token");
        Assert.assertEquals(bucketName, deleteDirectoryRequest.getBucketName());
        Assert.assertEquals(directory, deleteDirectoryRequest.getDirectoryName());
        Assert.assertEquals(directory, deleteDirectoryRequest.getKey());
        Assert.assertTrue(deleteDirectoryRequest.isDeleteRecursive());
        Assert.assertEquals("test-token", deleteDirectoryRequest.getNextDeleteToken());
        deleteDirectoryRequest.setDirectoryName("d1");
        Assert.assertEquals("d1", deleteDirectoryRequest.getDirectoryName());
        Assert.assertEquals("d1", deleteDirectoryRequest.getKey());
        deleteDirectoryRequest = new DeleteDirectoryRequest(bucketName, directory).withDeleteRecursive(true).withNextDeleteToken("123");
        Assert.assertEquals(directory, deleteDirectoryRequest.getDirectoryName());
        Assert.assertTrue(deleteDirectoryRequest.isDeleteRecursive());
        Assert.assertEquals("123", deleteDirectoryRequest.getNextDeleteToken());


        String dstObjectName = objectName + "-dst";
        RenameObjectRequest renameObjectRequest = new RenameObjectRequest(bucketName, objectName, dstObjectName);
        Assert.assertEquals(objectName, renameObjectRequest.getSourceObjectName());
        Assert.assertEquals(dstObjectName, renameObjectRequest.getKey());
        Assert.assertEquals(dstObjectName, renameObjectRequest.getDestinationObjectName());
        renameObjectRequest.setSourceObjectName("src-obj");
        renameObjectRequest.setDestinationObjectName("dst-obj");
        Assert.assertEquals("src-obj", renameObjectRequest.getSourceObjectName());
        Assert.assertEquals("dst-obj", renameObjectRequest.getKey());
        Assert.assertEquals("dst-obj", renameObjectRequest.getDestinationObjectName());
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        this.testHnsBucket = bucketName + "-test-hns";
        this.testClient = new OSSClientBuilder().build("oss-ap-southeast-2.aliyuncs.com", TestConfig.OSS_TEST_ACCESS_KEY_ID,
                TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        CreateBucketRequest createBucketRequest = new CreateBucketRequest(testHnsBucket);
        createBucketRequest.setHnsStatus(HnsStatus.Enabled);
        testClient.createBucket(createBucketRequest);
    }

    @Override
    public void tearDown() throws Exception {
        ObjectListing objectListing = testClient.listObjects(testHnsBucket);
        List<OSSObjectSummary> sums = objectListing.getObjectSummaries();
        for (OSSObjectSummary s : sums) {
            System.out.println("\t" + s.getKey());
            if (s.getETag() == null || s.getETag().isEmpty()) {
                try {
                    testClient.deleteDirectory(testHnsBucket, s.getKey().substring(0, s.getKey().length() - 1), true, null);
                } catch (Exception e) {
                }
            } else {
                testClient.deleteObject(testHnsBucket, s.getKey());
            }
        }
        super.tearDown();
    }

    @Test
    public void testGetHnsStatus() {
        BucketInfo info =  testClient.getBucketInfo(this.testHnsBucket);
        Assert.assertEquals(HnsStatus.Enabled.toString(), info.getBucket().getHnsStatus());
    }

    @Test
    public void testRequestWithPayerHeader() {
        String dirName = "test-dir";
        String dirNameNew = "new-" + dirName ;
        CreateDirectoryRequest createDirectoryRequest = new CreateDirectoryRequest(testHnsBucket, dirName);
        createDirectoryRequest.setRequestPayer(Payer.Requester);
        testClient.createDirectory(createDirectoryRequest);
        ObjectMetadata meta = testClient.getObjectMetadata(testHnsBucket, dirName);
        Assert.assertEquals("application/x-directory", meta.getContentType());

        RenameObjectRequest renameObjectRequest = new RenameObjectRequest(testHnsBucket, dirName, dirNameNew);
        renameObjectRequest.setRequestPayer(Payer.Requester);
        testClient.renameObject(renameObjectRequest);
        meta = testClient.getObjectMetadata(testHnsBucket, dirNameNew);
        Assert.assertEquals("application/x-directory", meta.getContentType());
        try {
            meta = testClient.getObjectMetadata(testHnsBucket, dirName);
            Assert.fail("should be failed here");
        } catch (Exception e) {
        }

        DeleteDirectoryRequest deleteDirectoryRequest = new DeleteDirectoryRequest(testHnsBucket, dirNameNew);
        deleteDirectoryRequest.setRequestPayer(Payer.Requester);
        DeleteDirectoryResult deleteDirectoryResult = testClient.deleteDirectory(deleteDirectoryRequest);
        Assert.assertEquals(dirNameNew, deleteDirectoryResult.getDirectoryName());
        Assert.assertEquals(1, deleteDirectoryResult.getDeleteNumber());
    }

    @Test
    public void testRenameObject() {
        String objectName = "test-obj";
        String objectNameNew ="new-" + objectName;

        testClient.putObject(testHnsBucket, objectName, new ByteArrayInputStream("123".getBytes()));
        testClient.renameObject(testHnsBucket, objectName, objectNameNew);
        ObjectMetadata meta = testClient.getObjectMetadata(testHnsBucket, objectNameNew);
        Assert.assertEquals(3, meta.getContentLength());


        objectName = "test-obj-1-#+<>中文测试";
        objectNameNew ="new-test-obj-1";
        testClient.putObject(testHnsBucket, objectName, new ByteArrayInputStream("1234".getBytes()));
        testClient.renameObject(testHnsBucket, objectName, objectNameNew);
        meta = testClient.getObjectMetadata(testHnsBucket, objectNameNew);
        Assert.assertEquals(4, meta.getContentLength());
    }

    @Test
    public void testDeleteDirectory() {
        String dirName = "test-dir";
        String objectName = "test-obj";
        testClient.createDirectory(testHnsBucket, dirName);


        for ( int i = 0; i < 100; i ++) {
            testClient.putObject(testHnsBucket, dirName + "/" + objectName + "-" + i, new ByteArrayInputStream("123".getBytes()));
        }

        try {
            DeleteDirectoryResult deleteDirectoryResult = testClient.deleteDirectory(testHnsBucket, dirName);
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.FILE_ALREADY_EXISTS, e.getErrorCode());
        }

        DeleteDirectoryResult deleteDirectoryResult = testClient.deleteDirectory(testHnsBucket, dirName, true, null);
        String nextToken = deleteDirectoryResult.getNextDeleteToken();
        Assert.assertEquals(100, deleteDirectoryResult.getDeleteNumber());
        Assert.assertEquals(dirName, deleteDirectoryResult.getDirectoryName());
        Assert.assertNotNull(nextToken);

        DeleteDirectoryRequest deleteDirectoryRequest = new DeleteDirectoryRequest(testHnsBucket, dirName)
                .withDeleteRecursive(true)
                .withNextDeleteToken(nextToken);
        deleteDirectoryResult = testClient.deleteDirectory(deleteDirectoryRequest);
        Assert.assertTrue(deleteDirectoryResult.getDeleteNumber() > 0);
        Assert.assertEquals(dirName, deleteDirectoryResult.getDirectoryName());

    }

}
