package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
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

public class PutObjectTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();


    @Test
    public void test_putObject_with_chinese_metadata_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_putObject_with_chinese_metadata_001";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("测试中文");
        metadata.addUserMetadata("test-chinese", "测试中文");

        PutObjectRequest request = new PutObjectRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        request.setMetadata(metadata);
        request.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));
        PutObjectResult putResult = obsClient.putObject(request);

        assertEquals(200, putResult.getStatusCode());

        GetObjectMetadataRequest getRequest = new GetObjectMetadataRequest();
        getRequest.setIsEncodeHeaders(false);
        getRequest.setObjectKey(objectKey);
        getRequest.setBucketName(bucketName);
        ObjectMetadata get_result = obsClient.getObjectMetadata(getRequest);

        assertEquals(200, get_result.getStatusCode());
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", get_result.getUserMetadata("test-chinese"));
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", get_result.getContentDisposition());
    }

    @Test
    public void test_putObject_with_chinese_metadata_002() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_putObject_with_chinese_metadata_002";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("【】，");
        metadata.addUserMetadata("test-chinese", "【】，");

        PutObjectRequest request = new PutObjectRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        request.setMetadata(metadata);
        request.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        try {
            obsClient.putObject(request);
            fail("No exception thrown.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unexpected char "));
        }
    }

    @Test
    public void test_putObject_with_chinese_metadata_003() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_putObject_with_chinese_metadata_003";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("测试中文");
        metadata.addUserMetadata("test-chinese", "测试中文");

        PutObjectRequest request = new PutObjectRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        request.setMetadata(metadata);
        request.setIsEncodeHeaders(false);
        request.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        try {
            obsClient.putObject(request);
            fail("No exception thrown.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unexpected char "));
        }
    }
}
