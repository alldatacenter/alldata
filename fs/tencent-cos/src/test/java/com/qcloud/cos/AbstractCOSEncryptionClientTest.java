package com.qcloud.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.UploadResult;
import com.qcloud.cos.transfer.Copy;
import com.qcloud.cos.transfer.Download;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;
import com.qcloud.cos.utils.Md5Utils;

public abstract class AbstractCOSEncryptionClientTest extends AbstractCOSClientTest {
    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        if (cryptoConfiguration == null && qcloudkms == null && encryptionMaterials == null) {
            return;
        }
        useClientEncryption = true;
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
        useClientEncryption = false;
    }
    
    
    // 测试从本地上传文件
    @Test
    public void testPutGetDelObjectEmpty() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(0L);
    }

    @Test
    public void testPutGetDelObject256k() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(256 * 1024L);
    }

    @Test
    public void testPutGetDelObject1M() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(1024 * 1024L);
    }

    @Test
    public void testPutGetDelObject4M() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(4 * 1024 * 1024L);
    }

    @Test
    public void testPutGetDelObject32M() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(32 * 1024 * 1024L);
    }

    @Test
    public void testPutGetDelObject100M() throws CosServiceException, IOException {
        testPutGetDelObjectDiffSize(100 * 1024 * 1024L);
    }
    
    @Test
    public void testMultipartUploadObjectSize_32M_Part_1M() throws IOException {
        testMultiPartUploadObject(4 * 1024 * 1024L, 1 * 1024 * 1024L);
    }
    
    @Test
    public void testMultipartUploadObjectSize_32M_Part_3M() throws IOException {
        testMultiPartUploadObject(32 * 1024 * 1024L, 32 * 1024 * 1024L);
    }
    
    @Test
    public void testStreamUpload_10M() throws IOException {
        testPutObjectByStreamDiffSize(10 * 1024 * 1024, new ObjectMetadata());
    }
    
    @Test
    public void testTransferManagerUploadDownBigFile()
            throws IOException, CosServiceException, CosClientException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        TransferManager transferManager = new TransferManager(cosclient);
        File localFile = buildTestFile(1024 * 1024 * 10L);
        File downFile = new File(localFile.getAbsolutePath() + ".down");
        String key = "ut/" + localFile.getName();
        String destKey = key + ".copy";
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, localFile);
            ObjectMetadata objectMetadata = new ObjectMetadata();
            putObjectRequest.setMetadata(objectMetadata);
            Upload upload = transferManager.upload(putObjectRequest);
            UploadResult uploadResult = upload.waitForUploadResult();
            assertTrue(uploadResult.getETag().contains("-"));
            assertNotNull(uploadResult.getRequestId());
            assertNotNull(uploadResult.getDateStr());
            GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key);
            Download download = transferManager.download(getObjectRequest, downFile);
            download.waitForCompletion();
            // check file
            assertEquals(Md5Utils.md5Hex(localFile), Md5Utils.md5Hex(downFile));

            CopyObjectRequest copyObjectRequest =
                    new CopyObjectRequest(bucket, key, bucket, destKey);
            Copy copy = transferManager.copy(copyObjectRequest, cosclient, null);
            copy.waitForCompletion();
        } finally {
            // delete file on cos
            clearObject(key);
            // delete file on cos
            clearObject(destKey);
            if (localFile.exists()) {
                assertTrue(localFile.delete());
            }
            if (downFile.exists()) {
                assertTrue(downFile.delete());
            }
        }
    }
}
