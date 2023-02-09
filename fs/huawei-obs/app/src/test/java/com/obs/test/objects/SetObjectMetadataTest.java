package com.obs.test.objects;

import com.obs.services.ObsClient;
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

public class SetObjectMetadataTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();

    @Test
    public void test_setObjectMetadata_with_chinese_metadata_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_setObjectMetadata_with_chinese_001";
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        assertEquals(200, putResult.getStatusCode());

        SetObjectMetadataRequest request = new SetObjectMetadataRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        request.addUserMetadata("test-chinese", "测试中文");
        request.setContentDisposition("测试中文");
        ObjectMetadata setResult = obsClient.setObjectMetadata(request);

        assertEquals(200, setResult.getStatusCode());

        GetObjectMetadataRequest getRequest = new GetObjectMetadataRequest();
        getRequest.setIsEncodeHeaders(false);
        getRequest.setObjectKey(objectKey);
        getRequest.setBucketName(bucketName);
        ObjectMetadata getResult = obsClient.getObjectMetadata(getRequest);

        assertEquals(200, getResult.getStatusCode());
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", getResult.getUserMetadata("test-chinese"));
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", getResult.getContentDisposition());
    }

    @Test
    public void test_setObjectMetadata_with_chinese_metadata_002() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_setObjectMetadata_with_chinese_002";
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
    public void test_setObjectMetadata_with_chinese_metadata_003() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_setObjectMetadata_with_chinese_002";
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

    @Test
    public void test_setObjectMetadata_with_url_encode_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_setObjectMetadata_with_url_encode_001";
        PutObjectResult put_result = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        assertEquals(200, put_result.getStatusCode());

        SetObjectMetadataRequest request = new SetObjectMetadataRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        request.addUserMetadata("test-url-encode", "%^&");
        request.setContentDisposition("%^&");
        try {
            obsClient.setObjectMetadata(request);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("URLDecoder: Illegal hex characters in escape (%) pattern"));
        }

        GetObjectMetadataRequest getRequest = new GetObjectMetadataRequest();
        getRequest.setIsEncodeHeaders(false);
        getRequest.setObjectKey(objectKey);
        getRequest.setBucketName(bucketName);
        ObjectMetadata getResult = obsClient.getObjectMetadata(getRequest);

        assertEquals(200, getResult.getStatusCode());
        assertEquals("%^&", getResult.getUserMetadata("test-url-encode"));
        assertEquals("%^&", getResult.getContentDisposition());

        request.setIsEncodeHeaders(false);
        ObjectMetadata setResult = obsClient.setObjectMetadata(request);

        assertEquals(200, setResult.getStatusCode());
        assertEquals("%^&", setResult.getUserMetadata("test-url-encode"));
        assertEquals("%^&", setResult.getResponseHeaders().get("content-disposition"));
    }

    @Test
    public void test_setObjectMetadata_with_url_encode_002() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_setObjectMetadata_with_url_encode_002";
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        assertEquals(200, putResult.getStatusCode());

        SetObjectMetadataRequest request = new SetObjectMetadataRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        request.addUserMetadata("test-url-encode", "%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87");
        request.setContentDisposition("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87");
        ObjectMetadata setResult = obsClient.setObjectMetadata(request);

        assertEquals(200, setResult.getStatusCode());
        assertEquals("测试中文", setResult.getUserMetadata("test-url-encode"));
        assertEquals("测试中文", setResult.getResponseHeaders().get("content-disposition"));

        GetObjectMetadataRequest getRequest = new GetObjectMetadataRequest();
        getRequest.setIsEncodeHeaders(false);
        getRequest.setObjectKey(objectKey);
        getRequest.setBucketName(bucketName);
        ObjectMetadata getResult = obsClient.getObjectMetadata(getRequest);

        assertEquals(200, getResult.getStatusCode());
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", getResult.getUserMetadata("test-url-encode"));
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", getResult.getContentDisposition());
    }
}
