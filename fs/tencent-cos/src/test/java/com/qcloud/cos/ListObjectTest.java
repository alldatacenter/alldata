package com.qcloud.cos;

import java.io.File;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.qcloud.cos.model.COSObjectSummary;
import com.qcloud.cos.model.ListNextBatchOfObjectsRequest;
import com.qcloud.cos.model.ListObjectsRequest;
import com.qcloud.cos.model.ObjectListing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ListObjectTest extends AbstractCOSClientTest {

    private static final int arrayNum = 10;
    private static File[] localFileArray = new File[arrayNum];
    private static String keyPrefix = "ut/list测试中文";
    private static String[] keyArray = new String[arrayNum];


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        AbstractCOSClientTest.initCosClient();
        for (int i = 0; i < arrayNum; ++i) {
            long localFileSize = i * 1024L;
            String key = String.format("%s/%dk.txt", keyPrefix, i);
            File localFile = buildTestFile(localFileSize);
            localFileArray[i] = localFile;
            keyArray[i] = key;
            putObjectFromLocalFile(localFile, key, null, null);
        }
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
        for (int i = 0; i < arrayNum; ++i) {
            if (localFileArray[i] != null && localFileArray[i].exists()) {
                assertTrue(localFileArray[i].delete());
            }
            if (keyArray[i] != null) {
                clearObject(keyArray[i]);
            }
        }
        AbstractCOSClientTest.destoryCosClient();
    }

    @Test
    public void ListObjectNoDelimiterTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        ObjectListing objectListing = cosclient.listObjects(bucket, keyPrefix);
        assertEquals(0L, objectListing.getCommonPrefixes().size());
        assertEquals(arrayNum, objectListing.getObjectSummaries().size());

        List<COSObjectSummary> objectSummaries = objectListing.getObjectSummaries();
        for (int i = 0; i < arrayNum; ++i) {
            COSObjectSummary cosObjectSummary = objectSummaries.get(i);
            String expectedKey = String.format("%s/%dk.txt", keyPrefix, i);
            assertEquals(expectedKey, cosObjectSummary.getKey());
            assertEquals(i * 1024L, cosObjectSummary.getSize());
        }
    }

    @Test
    public void ListObjectWithDelimiterTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        ListObjectsRequest listObjectsRequest =
                new ListObjectsRequest(bucket, keyPrefix, null, "/", 100);
        ObjectListing objectListing = cosclient.listObjects(listObjectsRequest);
        assertEquals(1L, objectListing.getCommonPrefixes().size());
        assertEquals(0L, objectListing.getObjectSummaries().size());
    }

    @Test
    public void ListObjectStartWithDelimiterTest() {
        if (!judgeUserInfoValid()) {
            return;
        }
        ListObjectsRequest listObjectsRequest =
                new ListObjectsRequest(bucket, keyPrefix, null, "/", 100);
        ObjectListing objectListing = cosclient.listObjects(listObjectsRequest);
        assertEquals(1L, objectListing.getCommonPrefixes().size());
        assertEquals(0L, objectListing.getObjectSummaries().size());
    }

    @Test
    public void ListObjectStartWithNoMaxKey() {
        if (!judgeUserInfoValid()) {
            return;
        }
        ListObjectsRequest listObjectsRequest =
                new ListObjectsRequest().withBucketName(bucket).withDelimiter("/");
        ObjectListing objectListing = cosclient.listObjects(listObjectsRequest);
        assertEquals(1L, objectListing.getCommonPrefixes().size());
        assertEquals(0L, objectListing.getObjectSummaries().size());
    }

    @Test
    public void ListNextBatchObjectWithNoTrunCated() {
        if (!judgeUserInfoValid()) {
            return;
        }
        ObjectListing objectListingPrev = new ObjectListing();
        objectListingPrev.setBucketName(bucket);
        objectListingPrev.setPrefix(keyPrefix);
        objectListingPrev.setNextMarker("");
        objectListingPrev.setMaxKeys(100);
        objectListingPrev.setDelimiter("");
        objectListingPrev.setTruncated(true);
        ListNextBatchOfObjectsRequest listNextBatchOfObjectsRequest =
                new ListNextBatchOfObjectsRequest(objectListingPrev);
        ObjectListing objectListingNext =
                cosclient.listNextBatchOfObjects(listNextBatchOfObjectsRequest);
        assertEquals(0L, objectListingNext.getCommonPrefixes().size());
        assertEquals(arrayNum, objectListingNext.getObjectSummaries().size());

        List<COSObjectSummary> objectSummaries = objectListingNext.getObjectSummaries();
        for (int i = 0; i < arrayNum; ++i) {
            COSObjectSummary cosObjectSummary = objectSummaries.get(i);
            String expectedKey = String.format("%s/%dk.txt", keyPrefix, i);
            assertEquals(expectedKey, cosObjectSummary.getKey());
            assertEquals(i * 1024L, cosObjectSummary.getSize());
        }
    }

    @Test
    public void ListNextBatchObjectWithTrunCated() {
        if (!judgeUserInfoValid()) {
            return;
        }
        ObjectListing objectListingPrev = new ObjectListing();
        objectListingPrev.setBucketName(bucket);
        objectListingPrev.setPrefix(keyPrefix);
        objectListingPrev.setNextMarker("");
        objectListingPrev.setMaxKeys(100);
        objectListingPrev.setDelimiter("");
        objectListingPrev.setTruncated(false);
        ListNextBatchOfObjectsRequest listNextBatchOfObjectsRequest =
                new ListNextBatchOfObjectsRequest(objectListingPrev);
        ObjectListing objectListingNext =
                cosclient.listNextBatchOfObjects(listNextBatchOfObjectsRequest);
        assertEquals(bucket, objectListingNext.getBucketName());
        assertEquals(objectListingPrev.isTruncated(), objectListingNext.isTruncated());
        assertEquals(objectListingPrev.getMaxKeys(), objectListingNext.getMaxKeys());
        assertEquals(objectListingPrev.getNextMarker(), objectListingNext.getMarker());
        assertEquals(0L, objectListingNext.getCommonPrefixes().size());
        assertEquals(0L, objectListingNext.getObjectSummaries().size());
    }

}
