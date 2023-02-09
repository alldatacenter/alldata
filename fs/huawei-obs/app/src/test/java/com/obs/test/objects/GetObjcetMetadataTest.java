package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.ObjectMetadata;
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

public class GetObjcetMetadataTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();


    @Test
    public void test_getObjectMetadata_with_url_encode_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_getObjectMetadata_with_url_encode_001";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("test-url-encode", "%^&");
        metadata.setContentDisposition("%^&");
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)), metadata);

        assertEquals(200, putResult.getStatusCode());

        GetObjectMetadataRequest request = new GetObjectMetadataRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        try {
            obsClient.getObjectMetadata(request);
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("URLDecoder: Illegal hex characters in escape (%) pattern"));
        }
    }

    @Test
    public void test_getObjectMetadata_with_url_encode_002() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_getObjectMetadata_with_url_encode_002";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("test-url-encode", "%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87");
        metadata.setContentDisposition("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87");
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)), metadata);

        assertEquals(200, putResult.getStatusCode());

        GetObjectMetadataRequest request = new GetObjectMetadataRequest();
        request.setObjectKey(objectKey);
        request.setBucketName(bucketName);
        ObjectMetadata getResult = obsClient.getObjectMetadata(request);
        assertEquals("测试中文", getResult.getUserMetadata("test-url-encode"));
        assertEquals("测试中文", getResult.getResponseHeaders().get("content-disposition"));
    }
}
