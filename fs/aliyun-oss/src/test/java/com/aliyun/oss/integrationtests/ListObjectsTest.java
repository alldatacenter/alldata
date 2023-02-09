/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.integrationtests;

import static com.aliyun.oss.integrationtests.TestUtils.batchPutObject;

import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.PutObjectRequest;
import junit.framework.Assert;

import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.model.ListObjectsRequest;
import com.aliyun.oss.model.OSSObjectSummary;
import com.aliyun.oss.model.ObjectListing;

public class ListObjectsTest extends TestBase {

    private static final int DEFAULT_MAX_RETURNED_KEYS = 100;
    private static final int MAX_RETURNED_KEYS_LIMIT = 1000;
    
    @Test
    public void testNormalListObjects() {
        final String bucketName = super.bucketName + "-normal-list-objects";
        
        try {
            ossClient.createBucket(bucketName);
            
            // List objects under empty bucket
            ObjectListing objectListing = ossClient.listObjects(bucketName);
            Assert.assertEquals(0, objectListing.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getMaxKeys());
            Assert.assertEquals(0, objectListing.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, objectListing.getBucketName());
            Assert.assertNull(objectListing.getDelimiter());
            Assert.assertNull(objectListing.getPrefix());
            Assert.assertNull(objectListing.getMarker());
            Assert.assertNull(objectListing.getNextMarker());
            Assert.assertFalse(objectListing.isTruncated());
            Assert.assertEquals(objectListing.getRequestId().length(), REQUEST_ID_LEN);
            
            // Add MAX_RETURNED_KEYS_LIMIT + 1 + lv2KeyCount objects to bucket
            List<String> existingKeys = new ArrayList<String>();
            final int lv1KeyCount = 102;
            final int lv2KeyCount = 11;
            final int keyCount = MAX_RETURNED_KEYS_LIMIT + 1 + lv2KeyCount;
            final String lv0KeyPrefix = "normal-list-lv0-objects-";
            final String lv1KeyPrefix = "normal-list-lv0-objects/lv1-objects-";
            final String lv2KeyPrefix = "normal-list-lv0-objects/lv1-objects/lv2-objects-";
            for (int i = 0; i < keyCount; i++) {
                if (i % 10 != 0) {
                    existingKeys.add(lv0KeyPrefix + i);
                } else {
                    existingKeys.add(lv1KeyPrefix + i);
                    if (i % 100 == 0) {                        
                        existingKeys.add(lv2KeyPrefix + i);
                    }
                }
            }
            
            if (!batchPutObject(ossClient, bucketName, existingKeys)) {
                Assert.fail("batch put object failed");
            }
            
            // List objects under nonempty bucket
            objectListing = ossClient.listObjects(bucketName);
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getMaxKeys());
            Assert.assertEquals(0, objectListing.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, objectListing.getBucketName());
            Assert.assertNull(objectListing.getDelimiter());
            Assert.assertNull(objectListing.getPrefix());
            Assert.assertNull(objectListing.getMarker());
            Assert.assertNotNull(objectListing.getNextMarker());
            Assert.assertTrue(objectListing.isTruncated());
            Assert.assertEquals(objectListing.getRequestId().length(), REQUEST_ID_LEN);
            
            // List objects with lv1KeyPrefix under nonempty bucket
            objectListing = ossClient.listObjects(bucketName, lv1KeyPrefix);
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getMaxKeys());
            Assert.assertEquals(0, objectListing.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, objectListing.getBucketName());
            Assert.assertNull(objectListing.getDelimiter());
            Assert.assertEquals(lv1KeyPrefix, objectListing.getPrefix());
            Assert.assertNull(objectListing.getMarker());
            Assert.assertNotNull(objectListing.getNextMarker());
            Assert.assertTrue(objectListing.isTruncated());
            Assert.assertEquals(objectListing.getRequestId().length(), REQUEST_ID_LEN);
            
            // List objects with lv0KeyPrefix under nonempty bucket
            objectListing = ossClient.listObjects(bucketName, lv0KeyPrefix);
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getMaxKeys());
            Assert.assertEquals(0, objectListing.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, objectListing.getBucketName());
            Assert.assertNull(objectListing.getDelimiter());
            Assert.assertEquals(lv0KeyPrefix, objectListing.getPrefix());
            Assert.assertNull(objectListing.getMarker());
            Assert.assertNotNull(objectListing.getNextMarker());
            Assert.assertTrue(objectListing.isTruncated());
            Assert.assertEquals(objectListing.getRequestId().length(), REQUEST_ID_LEN);
            
            // List object with 'prefix' and 'marker' under nonempty bucket
            String marker = objectListing.getNextMarker();
            objectListing = ossClient.listObjects(new ListObjectsRequest(bucketName, lv0KeyPrefix, marker, null, null));
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getMaxKeys());
            Assert.assertEquals(0, objectListing.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, objectListing.getBucketName());
            Assert.assertNull(objectListing.getDelimiter());
            Assert.assertEquals(lv0KeyPrefix, objectListing.getPrefix());
            Assert.assertEquals(marker, objectListing.getMarker());
            Assert.assertNotNull(objectListing.getNextMarker());
            Assert.assertTrue(objectListing.isTruncated());
            Assert.assertEquals(objectListing.getRequestId().length(), REQUEST_ID_LEN);

            // List object with 'prefix' and 'delimiter' under nonempty bucket
            final String delimiter = "/";
            final String keyPrefix0 = "normal-list-lv0-objects/";
            final String keyPrefix1 = "normal-list-lv0-objects/lv1-objects/";
            objectListing = ossClient.listObjects(
                    new ListObjectsRequest(bucketName, keyPrefix0, null, delimiter, MAX_RETURNED_KEYS_LIMIT));
            Assert.assertEquals(lv1KeyCount, objectListing.getObjectSummaries().size());
            Assert.assertEquals(MAX_RETURNED_KEYS_LIMIT, objectListing.getMaxKeys());
            Assert.assertEquals(1, objectListing.getCommonPrefixes().size());
            Assert.assertEquals(keyPrefix1, objectListing.getCommonPrefixes().get(0));
            Assert.assertEquals(bucketName, objectListing.getBucketName());
            Assert.assertEquals(delimiter, objectListing.getDelimiter());
            Assert.assertEquals(keyPrefix0, objectListing.getPrefix());
            Assert.assertNull(objectListing.getMarker());
            Assert.assertNull(objectListing.getNextMarker());
            Assert.assertFalse(objectListing.isTruncated());
            Assert.assertEquals(objectListing.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, bucketName);
        }
    }

    @Test
    public void testUnormalListObjects() {
        final String bucketName = super.bucketName + "-unormal-list-objects";
        
        try {
            ossClient.createBucket(bucketName);
            
            // List objects under non-existent bucket
            final String nonexistentBucket = super.bucketName + "-unormal-list-objects-bucket";
            try {
                ossClient.listObjects(nonexistentBucket);
                Assert.fail("List objects should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            }
            
            // List objects under bucket without ownership
            final String bucketWithoutOwnership = "oss";
            try {
                ossClient.listObjects(bucketWithoutOwnership);
                Assert.fail("List objects should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.ACCESS_DENIED, e.getErrorCode());
            }
            
            // Add DEFAULT_MAX_RETURNED_KEYS - 1 objects to bucket
            List<String> existingKeys = new ArrayList<String>();
            final int keyCount = DEFAULT_MAX_RETURNED_KEYS;
            final String keyPrefix = "unormal-list-objects-";
            final int unluckyNumber = 13;
            for (int i = 0; i <= keyCount; i++) {
                if (i != unluckyNumber) {
                    existingKeys.add(keyPrefix + i);
                }
            }
            
            if (!batchPutObject(ossClient, bucketName, existingKeys)) {
                Assert.fail("batch put object failed");
            }
            
            // List object with nonexistent marker
            final String nonexistentMarker = keyPrefix + unluckyNumber;
            try {
                ListObjectsRequest request = new ListObjectsRequest(bucketName, null, nonexistentMarker, null, null);
                ObjectListing objectListing = ossClient.listObjects(request);
                Assert.assertTrue(objectListing.getObjectSummaries().size() < keyCount);
                Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getMaxKeys());
                Assert.assertEquals(0, objectListing.getCommonPrefixes().size());
                Assert.assertEquals(bucketName, objectListing.getBucketName());
                Assert.assertNull(objectListing.getDelimiter());
                Assert.assertNull(objectListing.getPrefix());
                Assert.assertEquals(nonexistentMarker, objectListing.getMarker());
                Assert.assertNull(objectListing.getNextMarker());
                Assert.assertFalse(objectListing.isTruncated());
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
            
            // Set 'max-keys' less than zero
            final int maxKeysExceedLowerLimit = -1;
            try {
                ListObjectsRequest request = new ListObjectsRequest(bucketName);
                request.setMaxKeys(maxKeysExceedLowerLimit);
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }
            
            // Set 'max-keys' exceed MAX_RETURNED_KEYS_LIMIT
            final int maxKeysExceedUpperLimit = MAX_RETURNED_KEYS_LIMIT + 1;
            try {
                ListObjectsRequest request = new ListObjectsRequest(bucketName);
                request.setMaxKeys(maxKeysExceedUpperLimit);
            } catch (Exception e) {
                Assert.assertTrue(e instanceof IllegalArgumentException);
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, bucketName);
        }
    }
    
    @Test
    public void testListObjectsWithEncodingType() {
        final String objectPrefix = "object-with-special-characters-";
        
        try {
            // Add several objects with special characters into bucket.
            List<String> existingKeys = new ArrayList<String>();
            existingKeys.add(objectPrefix + "\001\007");
            existingKeys.add(objectPrefix + "\002\007");
            
            if (!batchPutObject(ossClient, bucketName, existingKeys)) {
                Assert.fail("batch put object failed");
            }
            
            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
            try {
                ossClient.listObjects(listObjectsRequest);
            } catch (Exception e) {
                Assert.assertTrue(e instanceof OSSException);
                Assert.assertEquals(OSSErrorCode.INVALID_RESPONSE, ((OSSException)e).getErrorCode());
            }
            
            // List objects under nonempty bucket
            listObjectsRequest = new ListObjectsRequest(bucketName);
            listObjectsRequest.setEncodingType(DEFAULT_ENCODING_TYPE);
            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                String decodedKey = URLDecoder.decode(s.getKey(), "UTF-8");
                Assert.assertTrue(existingKeys.contains(decodedKey));
            }
            Assert.assertEquals(DEFAULT_ENCODING_TYPE, objectListing.getEncodingType());
            Assert.assertEquals(existingKeys.size(), objectListing.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, objectListing.getMaxKeys());
            Assert.assertEquals(0, objectListing.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, objectListing.getBucketName());
            Assert.assertNull(objectListing.getDelimiter());
            Assert.assertNull(objectListing.getPrefix());
            Assert.assertNull(objectListing.getMarker());
            Assert.assertNull(objectListing.getNextMarker());
            Assert.assertFalse(objectListing.isTruncated());
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testListObjectsWithRestoreInfo() {
        String objectPrefix = "object-with-special-restore";
        String content = "abcde";

        try {
            // First upload the archive file, and then unfreeze it to obtain the returned RestoreInfo
            Map<String, String> header = new HashMap<String, String>();
            header.put(OSSHeaders.STORAGE_CLASS, "Archive");

            PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, objectPrefix, new ByteArrayInputStream(content.getBytes()));
            putObjectRequest.setHeaders(header);
            ossClient.putObject(putObjectRequest);

            ossClient.restoreObject(bucketName, objectPrefix);

            ListObjectsRequest listObjectsRequest = new ListObjectsRequest(bucketName);
            ObjectListing objectListing = ossClient.listObjects(listObjectsRequest);
            for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                String restoreInfo = s.getRestoreInfo();
                Assert.assertEquals(restoreInfo, "ongoing-request=\"true\"");
            }

            boolean flag = true;
            long startTime = System.currentTimeMillis();
            while (flag){
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                objectListing = ossClient.listObjects(listObjectsRequest);
                for (OSSObjectSummary s : objectListing.getObjectSummaries()) {
                    if(s.getRestoreInfo().contains("ongoing-request=\"false\"")){
                        flag = false;
                        Assert.assertTrue(true);
                        break;
                    }
                    long endTime = System.currentTimeMillis();
                    if(endTime - startTime > 1000 * 120){
                        Assert.assertFalse(true);
                    }
                }
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}
