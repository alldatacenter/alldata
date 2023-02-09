package com.obs.test;

import com.obs.services.ObsClient;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.PartEtag;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;
import com.obs.test.tools.PrepareTestBucket;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserHeaderTest {

    @Rule
    public PrepareTestBucket prepareTestBucket = new PrepareTestBucket();

    @Rule
    public TestName testName = new TestName();

    @Test
    public void test_add_user_headers() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_add_user_headers";
        String headerKey = "test-user-headers";

        // 初始化 log
        StringWriter writer = new StringWriter();

        TestTools.initLog(writer);

        // 测试覆盖所有 performRequest 和 trans 函数
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest();
        initiateMultipartUploadRequest.setBucketName(bucketName);
        initiateMultipartUploadRequest.setObjectKey(objectKey);
        initiateMultipartUploadRequest.addUserHeaders(headerKey, "test-value-initiateMultipartUpload");
        InitiateMultipartUploadResult initResult = obsClient.initiateMultipartUpload(initiateMultipartUploadRequest);
        assertTrue(writer.toString().contains("|" + headerKey + ": test-value-initiateMultipartUpload|"));
        writer.flush();
        assertEquals(200, initResult.getStatusCode());

        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setUploadId(initResult.getUploadId());
        uploadPartRequest.setPartNumber(1);
        uploadPartRequest.setBucketName(bucketName);
        uploadPartRequest.setObjectKey(objectKey);
        uploadPartRequest.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));
        uploadPartRequest.addUserHeaders(headerKey, "test-value-uploadPart");
        UploadPartResult uploadResult = obsClient.uploadPart(uploadPartRequest);
        assertTrue(writer.toString().contains("|" + headerKey + ": test-value-uploadPart|"));
        writer.flush();
        assertEquals(200, uploadResult.getStatusCode());

        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest();
        completeRequest.setObjectKey(objectKey);
        completeRequest.setBucketName(bucketName);
        completeRequest.setUploadId(initResult.getUploadId());
        completeRequest.addUserHeaders(headerKey, "test-value-completeMultipartUpload");
        PartEtag partEtag = new PartEtag();
        partEtag.setPartNumber(uploadResult.getPartNumber());
        partEtag.setEtag(uploadResult.getEtag());
        completeRequest.getPartEtag().add(partEtag);
        CompleteMultipartUploadResult completeResult = obsClient.completeMultipartUpload(completeRequest);
        assertTrue(writer.toString().contains("|" + headerKey + ": test-value-completeMultipartUpload|"));
        writer.flush();
        assertEquals(200, completeResult.getStatusCode());
    }

    @Test
    public void test_add_user_headers_need_auth() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_add_user_headers";
        String headerKey = TestTools.getAuthType().equals("v2") ? "x-amz-test-auth-header"
                : "x-obs-test-auth-header";

        // 初始化 log
        StringWriter writer = new StringWriter();

        TestTools.initLog(writer);

        // 测试覆盖所有 performRequest 和 trans 函数
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest();
        initiateMultipartUploadRequest.setBucketName(bucketName);
        initiateMultipartUploadRequest.setObjectKey(objectKey);
        initiateMultipartUploadRequest.addUserHeaders(headerKey, "test-value-initiateMultipartUpload");
        InitiateMultipartUploadResult initResult = obsClient.initiateMultipartUpload(initiateMultipartUploadRequest);
        // 签名计算字段
        assertTrue(writer.toString().contains("|" + headerKey + ":test-value-initiateMultipartUpload|"));
        // 请求头域
        assertTrue(writer.toString().contains("|" + headerKey + ": test-value-initiateMultipartUpload"));
        writer.flush();
        assertEquals(200, initResult.getStatusCode());

        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setUploadId(initResult.getUploadId());
        uploadPartRequest.setPartNumber(1);
        uploadPartRequest.setBucketName(bucketName);
        uploadPartRequest.setObjectKey(objectKey);
        uploadPartRequest.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));
        uploadPartRequest.addUserHeaders(headerKey, "test-value-uploadPart");
        UploadPartResult uploadResult = obsClient.uploadPart(uploadPartRequest);
        // 签名计算字段
        assertTrue(writer.toString().contains("|" + headerKey + ":test-value-uploadPart|"));
        // 请求头域
        assertTrue(writer.toString().contains("|" + headerKey + ": test-value-uploadPart"));
        writer.flush();
        assertEquals(200, uploadResult.getStatusCode());

        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest();
        completeRequest.setObjectKey(objectKey);
        completeRequest.setBucketName(bucketName);
        completeRequest.setUploadId(initResult.getUploadId());
        completeRequest.addUserHeaders(headerKey, "test-value-completeMultipartUpload");
        PartEtag partEtag = new PartEtag();
        partEtag.setPartNumber(uploadResult.getPartNumber());
        partEtag.setEtag(uploadResult.getEtag());
        completeRequest.getPartEtag().add(partEtag);
        CompleteMultipartUploadResult completeResult = obsClient.completeMultipartUpload(completeRequest);
        // 签名计算字段
        assertTrue(writer.toString().contains("|" + headerKey + ":test-value-completeMultipartUpload|"));
        // 请求头域
        assertTrue(writer.toString().contains("|" + headerKey + ": test-value-completeMultipartUpload"));
        writer.flush();
        assertEquals(200, completeResult.getStatusCode());
    }

    @Test
    public void test_add_empty_user_headers() {
        String bucketName = testName.getMethodName().replace("_", "-").toLowerCase(Locale.ROOT);
        ObsClient obsClient = TestTools.getPipelineEnvironment();
        String objectKey = "test_add_empty_user_headers";
        String headerKey = "test-add-empty-user-headers";

        // 初始化 log
        StringWriter writer = new StringWriter();

        TestTools.initLog(writer);

        // 测试覆盖所有 performRequest 和 trans 函数
        InitiateMultipartUploadRequest initiateMultipartUploadRequest = new InitiateMultipartUploadRequest();
        initiateMultipartUploadRequest.setBucketName(bucketName);
        initiateMultipartUploadRequest.setObjectKey(objectKey);
        initiateMultipartUploadRequest.addUserHeaders(headerKey, "");
        InitiateMultipartUploadResult initResult = obsClient.initiateMultipartUpload(initiateMultipartUploadRequest);
        assertTrue(writer.toString().contains("|" + headerKey + ": |"));
        writer.flush();
        assertEquals(200, initResult.getStatusCode());

        UploadPartRequest uploadPartRequest = new UploadPartRequest();
        uploadPartRequest.setUploadId(initResult.getUploadId());
        uploadPartRequest.setPartNumber(1);
        uploadPartRequest.setBucketName(bucketName);
        uploadPartRequest.setObjectKey(objectKey);
        uploadPartRequest.setInput(new ByteArrayInputStream("testObject".getBytes(StandardCharsets.UTF_8)));
        uploadPartRequest.addUserHeaders("test-user-headers", "");
        UploadPartResult uploadResult = obsClient.uploadPart(uploadPartRequest);
        assertTrue(writer.toString().contains("|" + headerKey + ": |"));
        writer.flush();
        assertEquals(200, uploadResult.getStatusCode());

        CompleteMultipartUploadRequest completeRequest = new CompleteMultipartUploadRequest();
        completeRequest.setObjectKey(objectKey);
        completeRequest.setBucketName(bucketName);
        completeRequest.setUploadId(initResult.getUploadId());
        completeRequest.addUserHeaders("test-user-headers", "");
        PartEtag partEtag = new PartEtag();
        partEtag.setPartNumber(uploadResult.getPartNumber());
        partEtag.setEtag(uploadResult.getEtag());
        completeRequest.getPartEtag().add(partEtag);
        CompleteMultipartUploadResult completeResult = obsClient.completeMultipartUpload(completeRequest);
        assertTrue(writer.toString().contains("|" + headerKey + ": |"));
        writer.flush();
        assertEquals(200, completeResult.getStatusCode());
    }

}
