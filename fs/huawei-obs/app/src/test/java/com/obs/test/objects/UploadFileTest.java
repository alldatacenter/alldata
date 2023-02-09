package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.UploadFileRequest;
import com.obs.test.tools.PrepareTestBucket;
import com.obs.test.TestTools;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;

import java.io.IOException;
import java.util.Locale;

import static com.obs.test.TestTools.genTestFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class UploadFileTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();

    @Test
    public void test_uploadFile_with_chinese_metadata_001() throws IOException {
        genTestFile("test_uploadFile_with_chinese_metadata_001", 1024 * 100);
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_uploadFile_with_chinese_metadata_001";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("测试中文");
        metadata.addUserMetadata("test-chinese", "测试中文");

        UploadFileRequest request = new UploadFileRequest(bucketName, objectKey);
        request.setObjectMetadata(metadata);
        request.setUploadFile("test_uploadFile_with_chinese_metadata_001");
        CompleteMultipartUploadResult upload_result = obsClient.uploadFile(request);

        assertEquals(200, upload_result.getStatusCode());

        GetObjectMetadataRequest get_request = new GetObjectMetadataRequest();
        get_request.setIsEncodeHeaders(false);
        get_request.setObjectKey(objectKey);
        get_request.setBucketName(bucketName);
        ObjectMetadata get_result = obsClient.getObjectMetadata(get_request);

        assertEquals(200, get_result.getStatusCode());
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", get_result.getUserMetadata("test-chinese"));
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", get_result.getContentDisposition());
    }

    @Test
    public void test_uploadFile_with_chinese_metadata_002() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_uploadFile_with_chinese_metadata_002";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("【】，");
        metadata.addUserMetadata("test-chinese", "【】，");

        UploadFileRequest request = new UploadFileRequest(bucketName, objectKey);
        request.setObjectMetadata(metadata);
        request.setUploadFile("test_uploadFile_with_chinese");

        try {
            obsClient.uploadFile(request);
            fail("No exception thrown.");
        } catch (ObsException e) {
            assertTrue(e.getMessage().contains("Unexpected char "));
        }
    }

    @Test
    public void test_uploadFile_with_chinese_metadata_003() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_uploadFile_with_chinese_metadata_003";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("测试中文");
        metadata.addUserMetadata("test-chinese", "测试中文");

        UploadFileRequest request = new UploadFileRequest(bucketName, objectKey);
        request.setObjectMetadata(metadata);
        request.setUploadFile("test_uploadFile_with_chinese");
        request.setIsEncodeHeaders(false);

        try {
            obsClient.uploadFile(request);
            fail("No exception thrown.");
        } catch (ObsException e) {
            assertTrue(e.getMessage().contains("Unexpected char "));
        }
    }

}
