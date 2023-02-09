package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.DownloadFileRequest;
import com.obs.services.model.DownloadFileResult;
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

public class DownloadFileTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();


    @Test
    public void test_downloadFile_with_url_encode_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_downloadFile_with_url_encode_001";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("test-url-encode", "%^&");
        metadata.setContentDisposition("%^&");
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)), metadata);

        assertEquals(200, putResult.getStatusCode());

        DownloadFileRequest request = new DownloadFileRequest(bucketName, objectKey);
        request.setDownloadFile("test_downloadFile_with_url_encode_001");
        try {
            obsClient.downloadFile(request);
        } catch (ObsException e) {
            assertTrue(e.getMessage().contains("URLDecoder: Illegal hex characters in escape (%) pattern"));
        }
        request.setIsEncodeHeaders(false);
        DownloadFileResult downloadResult = obsClient.downloadFile(request);

        assertEquals("%^&", downloadResult.getObjectMetadata().getUserMetadata("test-url-encode"));
        assertEquals("%^&", downloadResult.getObjectMetadata().getResponseHeaders().get("content-disposition"));
    }

    @Test
    public void test_downloadFile_with_url_encode_002() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_downloadFile_with_url_encode_002";
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.addUserMetadata("test-url-encode", "%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87");
        metadata.setContentDisposition("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87");
        PutObjectResult putResult = obsClient.putObject(bucketName, objectKey,
                new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)), metadata);

        assertEquals(200, putResult.getStatusCode());

        DownloadFileRequest request = new DownloadFileRequest(bucketName, objectKey);
        request.setDownloadFile("test_downloadFile_with_url_encode_002");
        DownloadFileResult downloadResult = obsClient.downloadFile(request);

        assertEquals("测试中文", downloadResult.getObjectMetadata().getUserMetadata("test-url-encode"));
        assertEquals("测试中文",
                downloadResult.getObjectMetadata().getResponseHeaders().get("content-disposition"));
    }
}
