package com.qcloud.cos;

import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.model.*;
import com.qcloud.cos.utils.Md5Utils;
import java.io.File;
import static org.junit.Assert.assertEquals;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class SSECustomerTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void testSSECustomerUploadDownload() throws Exception{
        clientConfig.setHttpProtocol(HttpProtocol.https);

        String key = "images/tiger.jpg";
        File localFile = buildTestFile(1024);
        String expectedMd5 = Md5Utils.md5Hex(localFile);
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, localFile);
        SSECustomerKey sseCustomerKey = new SSECustomerKey("MDEyMzQ1Njc4OUFCQ0RFRjAxMjM0NTY3ODlBQkNERUY=");
        putObjectRequest.setSSECustomerKey(sseCustomerKey);
        cosclient.putObject(putObjectRequest);

        GetObjectMetadataRequest getObjectMetadataRequest = new GetObjectMetadataRequest(bucket, key);
        getObjectMetadataRequest.setSSECustomerKey(sseCustomerKey);
        ObjectMetadata objectMetadata = cosclient.getObjectMetadata(getObjectMetadataRequest);

        File downloadFile = new File(localFile.getAbsolutePath() + ".down");
        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
        getObjectRequest.setSSECustomerKey(sseCustomerKey);
        cosclient.getObject(getObjectRequest, downloadFile);
        String resultMd5 = Md5Utils.md5Hex(downloadFile);
        assertEquals(expectedMd5, resultMd5);
        cosclient.deleteObject(bucket, key);
    }
}

