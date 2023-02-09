package com.qcloud.cos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.CopyObjectRequest;
import com.qcloud.cos.model.CopyResult;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.UploadResult;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.transfer.Copy;
import com.qcloud.cos.transfer.Download;
import com.qcloud.cos.transfer.MultipleFileDownload;
import com.qcloud.cos.transfer.MultipleFileUpload;
import com.qcloud.cos.transfer.TransferManager;
import com.qcloud.cos.transfer.Upload;
import com.qcloud.cos.utils.Md5Utils;

public class TransferManagerTest extends AbstractCOSClientTest {
    private static TransferManager transferManager = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
        if (cosclient != null) {
            transferManager = new TransferManager(AbstractCOSClientTest.cosclient,
                    Executors.newFixedThreadPool(32));
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        if (transferManager != null) {
            transferManager.shutdownNow(false);
        }
        AbstractCOSClientTest.destoryCosClient();
    }


    @Test
    public void testTransferManagerUploadDownCopySmallFile()
            throws IOException, CosServiceException, CosClientException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        TransferManager transferManager = new TransferManager(cosclient);
        File localFile = buildTestFile(1024 * 1024 * 2L);
        File downFile = new File(localFile.getAbsolutePath() + ".down");
        String key = "ut/" + localFile.getName();
        String destKey = key + ".copy";
        try {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, key, localFile);
            Upload upload = transferManager.upload(putObjectRequest);
            UploadResult uploadResult = upload.waitForUploadResult();
            // head object
            headSimpleObject(key, localFile.length(), Md5Utils.md5Hex(localFile));
            assertEquals(Md5Utils.md5Hex(localFile), uploadResult.getETag());
            assertFalse(upload.isResumeableMultipartUploadAfterFailed());
            assertNull(upload.getResumeableMultipartUploadId());
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
            // clear object
            clearObject(key);
            // clear dest object
            clearObject(destKey);
            // delete smallfile
            if (localFile.exists()) {
                assertTrue(localFile.delete());
            }
            if (downFile.exists()) {
                assertTrue(downFile.delete());
            }
        }

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
            objectMetadata.setServerSideEncryption("AES256");
            putObjectRequest.setMetadata(objectMetadata);
            Upload upload = transferManager.upload(putObjectRequest);
            UploadResult uploadResult = upload.waitForUploadResult();
            assertTrue(uploadResult.getETag().contains("-"));
            assertTrue(upload.isResumeableMultipartUploadAfterFailed());
            assertNotNull(upload.getResumeableMultipartUploadId());
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

    @Test
    public void testTransferManagerUploadLocalDir()
            throws IOException, CosServiceException, CosClientException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        String folderPrefix = "ut_uploaddir/";
        File localFile1 = buildTestFile(1L);
        File localFile2 = buildTestFile(1024L);
        String key1 = folderPrefix + localFile1.getName();
        String key2 = folderPrefix + localFile2.getName();
        try {
            MultipleFileUpload multipleFileUpload =
                    transferManager.uploadDirectory(bucket, folderPrefix, tmpDir, true);
            multipleFileUpload.waitForCompletion();
            headSimpleObject(key1, localFile1.length(), Md5Utils.md5Hex(localFile1));
            headSimpleObject(key2, localFile2.length(), Md5Utils.md5Hex(localFile2));
        } finally {
            if (localFile1.exists()) {
                assertTrue(localFile1.delete());
            }
            if (localFile2.exists()) {
                assertTrue(localFile2.delete());
            }
            clearObject(key1);
            clearObject(key2);
        }
    }

    @Test
    public void testTransferManagerUploadDownloadDir()
            throws IOException, CosServiceException, CosClientException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        String folderPrefix = "ut_uploaddir/";
        File localFile1 = buildTestFile(1L);
        File localFile2 = buildTestFile(1024L);
        String key1 = folderPrefix + localFile1.getName();
        String key2 = folderPrefix + localFile2.getName();
        String downloadDirName = "ut_download_dir";
        File downloaddir = new File(downloadDirName);
        if (!downloaddir.exists()) {
            downloaddir.mkdir();
        }
        File downloadFile1 = new File(downloadDirName + "/" +  folderPrefix + localFile1.getName());
        File downloadFile2 = new File(downloadDirName + "/" + folderPrefix + localFile2.getName());
        try {
            MultipleFileUpload multipleFileUpload =
                    transferManager.uploadDirectory(bucket, folderPrefix, tmpDir, true);
            multipleFileUpload.waitForCompletion();
            headSimpleObject(key1, localFile1.length(), Md5Utils.md5Hex(localFile1));
            headSimpleObject(key2, localFile2.length(), Md5Utils.md5Hex(localFile2));
            MultipleFileDownload multipleFileDownload =
                    transferManager.downloadDirectory(bucket, folderPrefix, downloaddir);
            multipleFileDownload.waitForCompletion();
            assertTrue(downloadFile1.exists());
            assertTrue(downloadFile2.exists());
            assertEquals(Md5Utils.md5Hex(localFile1), Md5Utils.md5Hex(downloadFile1));
            assertEquals(Md5Utils.md5Hex(localFile2), Md5Utils.md5Hex(downloadFile2));
        } finally {
            if (localFile1.exists()) {
                assertTrue(localFile1.delete());
            }
            if (localFile2.exists()) {
                assertTrue(localFile2.delete());
            }
            if (downloadFile1.exists()) {
                assertTrue(downloadFile1.delete());
            }
            if (downloadFile2.exists()) {
                assertTrue(downloadFile2.delete());
            }

            clearObject(key1);
            clearObject(key2);
            deleteDir(downloaddir);
        }
    }

    // transfer manager对不同园区5G以上文件进行分块拷贝
    @Ignore
    public void testTransferManagerCopyBigFileFromDiffRegion()
            throws CosServiceException, CosClientException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        COSCredentials srcCred = new BasicCOSCredentials(secretId, secretKey);
        String srcRegion = "ap-guangzhou";
        ClientConfig srcClientConfig = new ClientConfig(new Region(srcRegion));
        COSClient srcCOSClient = new COSClient(srcCred, srcClientConfig);
        String srcBucketName = "chengwus3gz-1251668577";
        String srcKey = "ut_copy/len10G_1.txt";
        String destKey = "ut_copy_dest/len10G_1.txt";
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(new Region(srcRegion),
                srcBucketName, srcKey, bucket, destKey);
        Copy copy = transferManager.copy(copyObjectRequest, srcCOSClient, null);
        copy.waitForCompletion();
        clearObject(destKey);
    }

    // transfer manager对不同园区5G以下文件进行使用put object copy
    @Ignore
    public void testTransferManagerCopySmallFileFromDiffRegion()
            throws CosServiceException, CosClientException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        COSCredentials srcCred = new BasicCOSCredentials(secretId, secretKey);
        String srcRegion = "ap-guangzhou";
        ClientConfig srcClientConfig = new ClientConfig(new Region(srcRegion));
        COSClient srcCOSClient = new COSClient(srcCred, srcClientConfig);
        String srcBucketName = "chengwus3gz-1251668577";
        String srcKey = "ut_copy/len1G.txt";
        String destKey = "ut_copy_dest/len1G.txt";
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(new Region(srcRegion),
                srcBucketName, srcKey, bucket, destKey);
        Copy copy = transferManager.copy(copyObjectRequest, srcCOSClient, null);
        CopyResult copyResult = copy.waitForCopyResult();
        assertNotNull(copyResult.getRequestId());
        assertNotNull(copyResult.getDateStr());
        clearObject(destKey);
    }

    // transfer manager对相同园区使用put object copy
    @Ignore
    public void testTransferManagerCopyBigFileFromSameRegion()
            throws CosServiceException, CosClientException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        COSCredentials srcCred = new BasicCOSCredentials(secretId, secretKey);
        String srcRegion = region;
        ClientConfig srcClientConfig = new ClientConfig(new Region(srcRegion));
        COSClient srcCOSClient = new COSClient(srcCred, srcClientConfig);
        String srcBucketName = bucket;
        String srcKey = "ut_copy/len10G_1.txt";
        String destKey = "ut_copy_dest/len10G_2.txt";
        CopyObjectRequest copyObjectRequest = new CopyObjectRequest(new Region(srcRegion),
                srcBucketName, srcKey, bucket, destKey);
        Copy copy = transferManager.copy(copyObjectRequest, srcCOSClient, null);
        CopyResult copyResult = copy.waitForCopyResult();
        assertNotNull(copyResult.getRequestId());
        assertNotNull(copyResult.getDateStr());
    }

    // transfer manager对相同园区使用put object copy
    @Ignore
    public void testTransferManagerCopySmallFileFromSameRegion()
            throws CosServiceException, CosClientException, InterruptedException {
        if (!judgeUserInfoValid()) {
            return;
        }
        COSCredentials srcCred = new BasicCOSCredentials(secretId, secretKey);
        String srcRegion = region;
        ClientConfig srcClientConfig = new ClientConfig(new Region(srcRegion));
        COSClient srcCOSClient = new COSClient(srcCred, srcClientConfig);
        String srcBucketName = bucket;
        String srcKey = "ut_copy/len1G.txt";
        String destKey = "ut_copy_dest/len1G_2.txt";
        CopyObjectRequest copyObjectRequest =
                new CopyObjectRequest(srcBucketName, srcKey, bucket, destKey);
        Copy copy = transferManager.copy(copyObjectRequest, srcCOSClient, null);
        CopyResult copyResult = copy.waitForCopyResult();
        assertNotNull(copyResult.getRequestId());
        assertNotNull(copyResult.getDateStr());
    }

}
