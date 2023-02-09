package com.obs.test.objects;

import com.obs.services.ObsClient;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.CopyPartRequest;
import com.obs.services.model.CopyPartResult;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PartEtag;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.PutObjectResult;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;
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

public class MultipartTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();


    @Test
    public void test_initiate_multipart_upload_with_chinese_metadata_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_initiate_multipart_upload_with_chinese_metadata_001";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("测试中文");
        metadata.addUserMetadata("test-chinese", "测试中文");
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest();
        request.setBucketName(bucketName);
        request.setMetadata(metadata);
        request.setObjectKey(objectKey);
        InitiateMultipartUploadResult initiateResult = obsClient.initiateMultipartUpload(request);

        assertEquals(200, initiateResult.getStatusCode());

        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setUploadId(initiateResult.getUploadId());
        uploadPartRequest.setPartNumber(1);
        uploadPartRequest.setBucketName(bucketName);
        uploadPartRequest.setObjectKey(objectKey);
        uploadPartRequest.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));
        UploadPartResult uploadResult = obsClient.uploadPart(uploadPartRequest);

        assertEquals(200, uploadResult.getStatusCode());

        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest();
        completeRequest.setObjectKey(objectKey);
        completeRequest.setBucketName(bucketName);
        completeRequest.setUploadId(initiateResult.getUploadId());
        PartEtag partEtag = new PartEtag();
        partEtag.setPartNumber(uploadResult.getPartNumber());
        partEtag.setEtag(uploadResult.getEtag());
        completeRequest.getPartEtag().add(partEtag);
        CompleteMultipartUploadResult completeResult = obsClient.completeMultipartUpload(completeRequest);
        assertEquals(200, completeResult.getStatusCode());


        GetObjectMetadataRequest get_request = new GetObjectMetadataRequest();
        get_request.setIsEncodeHeaders(false);
        get_request.setObjectKey(objectKey);
        get_request.setBucketName(bucketName);
        ObjectMetadata getResult = obsClient.getObjectMetadata(get_request);

        assertEquals(200, getResult.getStatusCode());
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", getResult.getUserMetadata("test-chinese"));
        assertEquals("%E6%B5%8B%E8%AF%95%E4%B8%AD%E6%96%87", getResult.getContentDisposition());
    }

    @Test
    public void test_initiate_multipart_upload_with_chinese_metadata_002() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_initiate_multipart_upload_with_chinese_metadata_002";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("【】，");
        metadata.addUserMetadata("test-chinese", "【】，");

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest();
        request.setBucketName(bucketName);
        request.setMetadata(metadata);
        request.setObjectKey(objectKey);

        try {
            obsClient.initiateMultipartUpload(request);
            fail("No exception thrown.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unexpected char "));
        }
    }

    @Test
    public void test_initiate_multipart_upload_with_chinese_metadata_003() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_initiate_multipart_upload_with_chinese_metadata_003";

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentDisposition("测试中文");
        metadata.addUserMetadata("test-chinese", "测试中文");

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest();
        request.setBucketName(bucketName);
        request.setMetadata(metadata);
        request.setObjectKey(objectKey);
        request.setIsEncodeHeaders(false);

        try {
            obsClient.initiateMultipartUpload(request);
            fail("No exception thrown.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("Unexpected char "));
        }
    }

    @Test
    public void test_copy_part_001() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_copy_part_001";

        PutObjectRequest putRequest = new PutObjectRequest();
        putRequest.setObjectKey(objectKey);
        putRequest.setBucketName(bucketName);
        putRequest.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));

        PutObjectResult putResult = obsClient.putObject(putRequest);
        assertEquals(200, putResult.getStatusCode());

        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest();
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        InitiateMultipartUploadResult initiateResult = obsClient.initiateMultipartUpload(request);

        assertEquals(200, initiateResult.getStatusCode());

        CopyPartRequest copyRequest = new CopyPartRequest();
        copyRequest.setPartNumber(1);
        copyRequest.setSourceObjectKey(objectKey);
        copyRequest.setSourceBucketName(bucketName);
        copyRequest.setBucketName(bucketName);
        copyRequest.setUploadId(initiateResult.getUploadId());
        copyRequest.setDestinationBucketName(bucketName);
        copyRequest.setDestinationObjectKey(objectKey);
        CopyPartResult result = obsClient.copyPart(copyRequest);

        assertEquals(200, result.getStatusCode());

    }
}
