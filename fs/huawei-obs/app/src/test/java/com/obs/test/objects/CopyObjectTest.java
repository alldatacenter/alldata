package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.model.CopyObjectRequest;
import com.obs.services.model.CopyObjectResult;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.SetObjectMetadataRequest;
import com.obs.test.tools.PrepareTestBucket;
import com.obs.test.TestTools;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class CopyObjectTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();

    @Test
    public void test_copyObject_with_chinese_metadata_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_copyObject_with_chinese_metadata_001";
        String copyObjectKey = "test_copyObject_with_chinese_metadata_001_copy";
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        assertEquals(200, putResult.getStatusCode());

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("测试中文");
        metadata.addUserMetadata("test-chinese", "测试中文");
        CopyObjectRequest request = new CopyObjectRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(copyObjectKey);
        request.setSourceBucketName(bucketName);
        request.setSourceObjectKey(objectKey);
        request.setNewObjectMetadata(metadata);
        request.setReplaceMetadata(true);
        CopyObjectResult copyResult = obsClient.copyObject(request);
        assertEquals(200, copyResult.getStatusCode());

        GetObjectMetadataRequest getRequest = new GetObjectMetadataRequest();
        getRequest.setIsEncodeHeaders(false);
        getRequest.setObjectKey(copyObjectKey);
        getRequest.setBucketName(bucketName);
        ObjectMetadata getResult = obsClient.getObjectMetadata(getRequest);

        assertEquals(200, getResult.getStatusCode());
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", getResult.getUserMetadata("test-chinese"));
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", getResult.getContentDisposition());
    }

    @Test
    public void test_copyObject_with_chinese_metadata_002() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_copyObject_with_chinese_metadata_002";
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        assertEquals(200, putResult.getStatusCode());

        SetObjectMetadataRequest request = new SetObjectMetadataRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        request.addUserMetadata("test-chinese", "【】，");
        request.setContentDisposition("【】，");
        try {
            obsClient.setObjectMetadata(request);
            fail("No exception thrown.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unexpected char "));
        }
    }

    @Test
    public void test_copyObject_with_chinese_metadata_003() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_copyObject_with_chinese_metadata_003";
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        assertEquals(200, putResult.getStatusCode());

        SetObjectMetadataRequest request = new SetObjectMetadataRequest();
        request.setObjectKey(objectKey);
        request.setIsEncodeHeaders(false);
        request.setBucketName(bucketName);
        request.addUserMetadata("test-chinese", "测试中文");
        request.setContentDisposition("测试中文");
        try {
            obsClient.setObjectMetadata(request);
            fail("No exception thrown.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unexpected char "));
        }
    }

}
