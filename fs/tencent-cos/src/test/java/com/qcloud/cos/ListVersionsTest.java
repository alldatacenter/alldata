package com.qcloud.cos;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.model.BucketVersioningConfiguration;
import com.qcloud.cos.model.COSVersionSummary;
import com.qcloud.cos.model.GetObjectMetadataRequest;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.ListVersionsRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.SetBucketVersioningConfigurationRequest;
import com.qcloud.cos.model.VersionListing;
import com.qcloud.cos.utils.Md5Utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

public class ListVersionsTest extends AbstractCOSClientTest {
    private static final String keyPrefix = "ut/list_versions";
    private static final int fileNum = 1;
    private static final int eachFileVersionNum = 17;

    private static final File[] localFileArray = new File[fileNum * eachFileVersionNum];
    private static final String[] keyArray = new String[fileNum * eachFileVersionNum];

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        AbstractCOSClientTest.destoryCosClient();
    }

    public void putTestFile(boolean retVersionFlag) throws Exception {
        for (int i = 0; i < fileNum; ++i) {
            for (int j = 0; j < eachFileVersionNum; ++j) {
                long localFileSize = (i + 1) * 1024L;
                String key = String.format("%s/%06dk", keyPrefix, i);
                File localFile = buildTestFile(localFileSize);
                localFileArray[i * eachFileVersionNum + j] = localFile;
                keyArray[i * eachFileVersionNum + j] = key;
                PutObjectResult putObjectResult = putObjectFromLocalFile(localFile, key);
                if (retVersionFlag) {
                    assertNotNull("put object for version enalbed bucket should return versonId",
                            putObjectResult.getVersionId());
                } else {
                    assertEquals(null, putObjectResult.getVersionId());
                }
            }
        }
    }

    public void delTestFile() throws Exception {
        for (int i = 0; i < fileNum; ++i) {
            for (int j = 0; j < eachFileVersionNum; ++j) {
                File localFile = localFileArray[i * eachFileVersionNum + j];
                assertTrue(localFile.delete());
            }
        }
    }

    private void delVersion(String key, String versionId) {
        cosclient.deleteVersion(bucket, key, versionId);
    }

    private void headAndGetVersion(String key, String versionId, String expectedEtag,
            long expectedLength, File downloadLocalFile)
                    throws CosServiceException, FileNotFoundException, IOException {
        GetObjectMetadataRequest getObjectMetadataRequest =
                new GetObjectMetadataRequest(bucket, key, versionId);
        ObjectMetadata objectMetadata = cosclient.getObjectMetadata(getObjectMetadataRequest);
        assertFalse(objectMetadata.isDeleteMarker());
        assertEquals(expectedLength, objectMetadata.getContentLength());
        assertEquals(expectedEtag, objectMetadata.getETag());

        GetObjectRequest getObjectRequest = new GetObjectRequest(bucket, key, versionId);
        ObjectMetadata objectMetadata2 = cosclient.getObject(getObjectRequest, downloadLocalFile);
        assertEquals(expectedLength, downloadLocalFile.length());
        assertEquals(expectedEtag, Md5Utils.md5Hex(downloadLocalFile));
        assertFalse(objectMetadata2.isDeleteMarker());
        assertEquals(expectedLength, objectMetadata2.getContentLength());
        assertEquals(expectedEtag, objectMetadata2.getETag());
        assertTrue(downloadLocalFile.delete());
    }

    @Test
    public void testListVersionsForEnabledVersions() throws Exception {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketVersioningConfiguration bucketVersioningEnabled =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.ENABLED);
        cosclient.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersioningEnabled));

        Thread.sleep(5000L);

        putTestFile(true);

        final int maxKeyNum = 20;
        ListVersionsRequest listVersionsRequest = new ListVersionsRequest();
        listVersionsRequest.withBucketName(bucket).withPrefix(keyPrefix).withMaxResults(maxKeyNum);

        VersionListing versionListing = cosclient.listVersions(listVersionsRequest);
        int keyIndex = 0;

        while (true) {
            List<COSVersionSummary> versionSummaries = versionListing.getVersionSummaries();
            assertTrue(versionSummaries.size() <= maxKeyNum);
            for (COSVersionSummary versionInfo : versionSummaries) {
                // 对相同可key的文件list出来的顺序是版本号由近到远
                int localFileIndex = (keyIndex / eachFileVersionNum + 1) * eachFileVersionNum - 1
                        - (keyIndex % eachFileVersionNum);

                File downFile =
                        new File(localFileArray[localFileIndex].getAbsolutePath() + ".down");
                String versionId = versionInfo.getVersionId();
                String key = versionInfo.getKey();
                long expectedLength = versionInfo.getSize();
                String expectedEtag = versionInfo.getETag();
                headAndGetVersion(key, versionId, expectedEtag, expectedLength, downFile);
                delVersion(versionInfo.getKey(), versionInfo.getVersionId());

                // 对于开启了多版本的 versionid不是null
                assertFalse(versionInfo.getVersionId().equals("null"));
                assertFalse(versionInfo.isDeleteMarker());
                assertEquals(bucket, versionInfo.getBucketName());
                assertEquals(keyArray[localFileIndex], versionInfo.getKey());
                assertEquals(Md5Utils.md5Hex(localFileArray[localFileIndex]),
                        versionInfo.getETag());
                assertEquals(localFileArray[localFileIndex].length(), versionInfo.getSize());
                ++keyIndex;
            }
            if (!versionListing.isTruncated()) {
                break;
            }
            versionListing = cosclient.listNextBatchOfVersions(versionListing);
        }
        assertEquals(fileNum * eachFileVersionNum, keyIndex);
        delTestFile();
    }

    @Test
    public void testListVersionsForSuspendedVersions() throws Exception {
        if (!judgeUserInfoValid()) {
            return;
        }
        BucketVersioningConfiguration bucketVersioningEnabled =
                new BucketVersioningConfiguration(BucketVersioningConfiguration.SUSPENDED);
        cosclient.setBucketVersioningConfiguration(
                new SetBucketVersioningConfigurationRequest(bucket, bucketVersioningEnabled));

        Thread.sleep(5000L);

        putTestFile(false);

        final int maxKeyNum = 2;
        ListVersionsRequest listVersionsRequest = new ListVersionsRequest();
        listVersionsRequest.withBucketName(bucket).withPrefix(keyPrefix).withMaxResults(maxKeyNum);

        VersionListing versionListing = cosclient.listVersions(listVersionsRequest);
        int keyIndex = 0;

        while (true) {
            List<COSVersionSummary> versionSummaries = versionListing.getVersionSummaries();
            assertTrue(versionSummaries.size() <= maxKeyNum);
            for (COSVersionSummary versionInfo : versionSummaries) {
                delVersion(versionInfo.getKey(), versionInfo.getVersionId());
                assertTrue(versionInfo.getVersionId().equals("null"));
                assertFalse(versionInfo.isDeleteMarker());
                assertEquals(bucket, versionInfo.getBucketName());
                int localFileIndex = (keyIndex + 1) * eachFileVersionNum - 1;

                assertEquals(keyArray[localFileIndex], versionInfo.getKey());
                // 应该取该尺寸大小的最后一个覆盖的文件的Etag信息
                assertEquals(Md5Utils.md5Hex(localFileArray[localFileIndex]),
                        versionInfo.getETag());
                assertEquals(localFileArray[localFileIndex].length(), versionInfo.getSize());
                ++keyIndex;
            }
            if (!versionListing.isTruncated()) {
                break;
            }
            versionListing = cosclient.listNextBatchOfVersions(versionListing);
        }
        assertEquals(fileNum, keyIndex);

        delTestFile();
    }
}
