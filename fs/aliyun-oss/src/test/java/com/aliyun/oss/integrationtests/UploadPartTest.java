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

import static com.aliyun.oss.integrationtests.TestConfig.OSS_TEST_ENDPOINT;
import static com.aliyun.oss.integrationtests.TestConstants.BUCKET_NOT_EMPTY_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.ENTITY_TOO_SMALL_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.INVALID_PART_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_BUCKET_ERR;
import static com.aliyun.oss.integrationtests.TestConstants.NO_SUCH_UPLOAD_ERR;
import static com.aliyun.oss.integrationtests.TestUtils.calcMultipartsETag;
import static com.aliyun.oss.integrationtests.TestUtils.claimUploadId;
import static com.aliyun.oss.integrationtests.TestUtils.composeLocation;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthFile;
import static com.aliyun.oss.integrationtests.TestUtils.genFixedLengthInputStream;
import static com.aliyun.oss.integrationtests.TestUtils.removeFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import com.aliyun.oss.model.*;
import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;

import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;

public class UploadPartTest extends TestBase {

    private static final int LIST_PART_MAX_RETURNS = 1000;
    private static final int LIST_UPLOAD_MAX_RETURNS = 1000;

    @Test
    public void testNormalUploadSinglePart() {
        final String key = "normal-upload-single-part-object";
        final int partSize = 128 * 1024;     //128KB

        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);
            InputStream instream = genFixedLengthInputStream(partSize);

            // Upload single part
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(2);
            uploadPartRequest.setPartSize(partSize);
            uploadPartRequest.setUploadId(uploadId);
            ossClient.uploadPart(uploadPartRequest);

            // List single multipart upload under this bucket
            ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            MultipartUploadListing multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertFalse(multipartUploadListing.isTruncated());
            Assert.assertEquals(key, multipartUploadListing.getNextKeyMarker());
            Assert.assertEquals(uploadId, multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertNull(multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            List<MultipartUpload> multipartUploads = multipartUploadListing.getMultipartUploads();
            Assert.assertEquals(1, multipartUploads.size());
            Assert.assertEquals(key, multipartUploads.get(0).getKey());
            Assert.assertEquals(uploadId, multipartUploads.get(0).getUploadId());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
            // Abort multipart upload
            AbortMultipartUploadRequest abortMultipartUploadRequest = new AbortMultipartUploadRequest(bucketName, key, uploadId);
            ossClient.abortMultipartUpload(abortMultipartUploadRequest);

            // List single multipart upload under this bucket again
            listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(0, multipartUploadListing.getMultipartUploads().size());
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertFalse(multipartUploadListing.isTruncated());
            Assert.assertNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertNull(multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalUploadMultiparts() {
        final String key = "normal-upload-multiparts-object";
        final int partSize = 128 * 1024;     //128KB
        final int partCount = 10;

        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);

            // Upload parts
            List<PartETag> partETags = new ArrayList<PartETag>();
            for (int i = 0; i < partCount; i++) {
                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                partETags.add(uploadPartResult.getPartETag());
            }

            // List parts
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            listPartsRequest.setUploadId(uploadId);
            PartListing partListing = ossClient.listParts(listPartsRequest);
            Assert.assertEquals(partCount, partListing.getParts().size());
            for (int i = 0; i < partCount; i++) {
                PartSummary ps = partListing.getParts().get(i);
                PartETag eTag = partETags.get(i);
                Assert.assertEquals(eTag.getPartNumber(), ps.getPartNumber());
                Assert.assertEquals(eTag.getETag(), ps.getETag());
            }
            Assert.assertEquals(bucketName, partListing.getBucketName());
            Assert.assertEquals(key, partListing.getKey());
            Assert.assertEquals(uploadId, partListing.getUploadId());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, partListing.getMaxParts().intValue());
            Assert.assertEquals(partCount, partListing.getNextPartNumberMarker().intValue());
            Assert.assertFalse(partListing.isTruncated());
            Assert.assertEquals(partListing.getRequestId().length(), REQUEST_ID_LEN);
            // List single multipart upload under this bucket
            ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            MultipartUploadListing multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertFalse(multipartUploadListing.isTruncated());
            Assert.assertEquals(key, multipartUploadListing.getNextKeyMarker());
            Assert.assertEquals(uploadId, multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertNull(multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            List<MultipartUpload> multipartUploads = multipartUploadListing.getMultipartUploads();
            Assert.assertEquals(1, multipartUploads.size());
            Assert.assertEquals(key, multipartUploads.get(0).getKey());
            Assert.assertEquals(uploadId, multipartUploads.get(0).getUploadId());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);

            // Complete multipart upload
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                    completeMultipartUploadResult.getLocation());
            Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
            Assert.assertEquals(key, completeMultipartUploadResult.getKey());
            Assert.assertEquals(calcMultipartsETag(partETags), completeMultipartUploadResult.getETag());
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

            // List single multipart uploads under this bucket again
            listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(0, multipartUploadListing.getMultipartUploads().size());
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertFalse(multipartUploadListing.isTruncated());
            Assert.assertNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertNull(multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);

            // Get uploaded object
            OSSObject o = ossClient.getObject(bucketName, key);
            final long objectSize = partCount * partSize;
            Assert.assertEquals(objectSize, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(calcMultipartsETag(partETags), o.getObjectMetadata().getETag());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalListParts() {
        final String key = "normal-list-parts-object";
        final int partSize = 128 * 1024;     //128KB
        final int partCount = 25;

        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);

            // List parts under empty bucket
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            PartListing partListing = ossClient.listParts(listPartsRequest);
            Assert.assertEquals(0, partListing.getParts().size());
            Assert.assertEquals(bucketName, partListing.getBucketName());
            Assert.assertEquals(key, partListing.getKey());
            Assert.assertEquals(uploadId, partListing.getUploadId());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, partListing.getMaxParts().intValue());
            Assert.assertNull(partListing.getNextPartNumberMarker());
            Assert.assertFalse(partListing.isTruncated());
            Assert.assertEquals(partListing.getRequestId().length(), REQUEST_ID_LEN);

            // Upload parts
            List<PartETag> partETags = new ArrayList<PartETag>();
            for (int i = 0; i < partCount; i++) {
                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                partETags.add(uploadPartResult.getPartETag());
            }

            // List parts without any special conditions
            listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            partListing = ossClient.listParts(listPartsRequest);
            Assert.assertEquals(partCount, partListing.getParts().size());
            for (int i = 0; i < partCount; i++) {
                PartSummary ps = partListing.getParts().get(i);
                PartETag eTag = partETags.get(i);
                Assert.assertEquals(eTag.getPartNumber(), ps.getPartNumber());
                Assert.assertEquals(eTag.getETag(), ps.getETag());
            }
            Assert.assertEquals(bucketName, partListing.getBucketName());
            Assert.assertEquals(key, partListing.getKey());
            Assert.assertEquals(uploadId, partListing.getUploadId());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, partListing.getMaxParts().intValue());
            Assert.assertEquals(partCount, partListing.getNextPartNumberMarker().intValue());
            Assert.assertFalse(partListing.isTruncated());
            Assert.assertEquals(partListing.getRequestId().length(), REQUEST_ID_LEN);

            // List 'max-parts' parts each time
            final int maxParts = 15;
            listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            listPartsRequest.setMaxParts(maxParts);
            partListing = ossClient.listParts(listPartsRequest);
            Assert.assertEquals(maxParts, partListing.getParts().size());
            for (int i = 0; i < maxParts; i++) {
                PartSummary ps = partListing.getParts().get(i);
                PartETag eTag = partETags.get(i);
                Assert.assertEquals(eTag.getPartNumber(), ps.getPartNumber());
                Assert.assertEquals(eTag.getETag(), ps.getETag());
                Assert.assertEquals(partSize, ps.getSize());
            }
            Assert.assertEquals(bucketName, partListing.getBucketName());
            Assert.assertEquals(key, partListing.getKey());
            Assert.assertEquals(uploadId, partListing.getUploadId());
            Assert.assertEquals(maxParts, partListing.getMaxParts().intValue());
            Assert.assertEquals(maxParts, partListing.getNextPartNumberMarker().intValue());
            Assert.assertTrue(partListing.isTruncated());
            Assert.assertEquals(partListing.getRequestId().length(), REQUEST_ID_LEN);

            // List 'max-parts' parts with 'part-number-marker' 
            final int partNumberMarker = 20;
            listPartsRequest.setPartNumberMarker(partNumberMarker);
            partListing = ossClient.listParts(listPartsRequest);
            Assert.assertEquals(partCount - partNumberMarker, partListing.getParts().size());
            for (int i = 0; i < (partCount - partNumberMarker); i++) {
                PartSummary ps = partListing.getParts().get(i);
                PartETag eTag = partETags.get(partNumberMarker + i);
                Assert.assertEquals(eTag.getPartNumber(), ps.getPartNumber());
                Assert.assertEquals(eTag.getETag(), ps.getETag());
                Assert.assertEquals(partSize, ps.getSize());
            }
            Assert.assertEquals(bucketName, partListing.getBucketName());
            Assert.assertEquals(key, partListing.getKey());
            Assert.assertEquals(uploadId, partListing.getUploadId());
            Assert.assertEquals(maxParts, partListing.getMaxParts().intValue());
            Assert.assertEquals(partCount, partListing.getNextPartNumberMarker().intValue());
            Assert.assertFalse(partListing.isTruncated());
            Assert.assertEquals(partListing.getRequestId().length(), REQUEST_ID_LEN);

            // Complete multipart upload
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                    completeMultipartUploadResult.getLocation());
            Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
            Assert.assertEquals(key, completeMultipartUploadResult.getKey());
            Assert.assertEquals(calcMultipartsETag(partETags), completeMultipartUploadResult.getETag());
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

            // Get uploaded object
            OSSObject o = ossClient.getObject(bucketName, key);
            final long objectSize = partCount * partSize;
            Assert.assertEquals(objectSize, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(calcMultipartsETag(partETags), o.getObjectMetadata().getETag());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalListPartsWithEncoding() {
        final String key = "normal-list-parts-常记溪亭日暮，沉醉不知归路";
        final int partSize = 128 * 1024;
        final int partCount = 25;

        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);

            // List parts under empty bucket
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            PartListing partListing = ossClient.listParts(listPartsRequest);
            Assert.assertEquals(0, partListing.getParts().size());
            Assert.assertEquals(bucketName, partListing.getBucketName());
            Assert.assertEquals(key, partListing.getKey());
            Assert.assertEquals(uploadId, partListing.getUploadId());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, partListing.getMaxParts().intValue());
            Assert.assertNull(partListing.getNextPartNumberMarker());
            Assert.assertFalse(partListing.isTruncated());
            Assert.assertEquals(partListing.getRequestId().length(), REQUEST_ID_LEN);

            // Upload parts
            List<PartETag> partETags = new ArrayList<PartETag>();
            for (int i = 0; i < partCount; i++) {
                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                partETags.add(uploadPartResult.getPartETag());
            }

            // List parts with encoding
            listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            listPartsRequest.setEncodingType(DEFAULT_ENCODING_TYPE);
            partListing = ossClient.listParts(listPartsRequest);
            Assert.assertEquals(partCount, partListing.getParts().size());
            for (int i = 0; i < partCount; i++) {
                PartSummary ps = partListing.getParts().get(i);
                PartETag eTag = partETags.get(i);
                Assert.assertEquals(eTag.getPartNumber(), ps.getPartNumber());
                Assert.assertEquals(eTag.getETag(), ps.getETag());
            }
            Assert.assertEquals(bucketName, partListing.getBucketName());
            Assert.assertEquals(key, URLDecoder.decode(partListing.getKey(), "UTF-8"));
            Assert.assertEquals(uploadId, partListing.getUploadId());
            Assert.assertEquals(LIST_PART_MAX_RETURNS, partListing.getMaxParts().intValue());
            Assert.assertEquals(partCount, partListing.getNextPartNumberMarker().intValue());
            Assert.assertFalse(partListing.isTruncated());
            Assert.assertEquals(partListing.getRequestId().length(), REQUEST_ID_LEN);

            // Complete multipart upload
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                    completeMultipartUploadResult.getLocation());
            Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
            Assert.assertEquals(key, completeMultipartUploadResult.getKey());
            Assert.assertEquals(calcMultipartsETag(partETags), completeMultipartUploadResult.getETag());
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

            ossClient.deleteObject(bucketName, key);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUnormalListParts() {
        final String key = "unormal-list-parts-object";

        // Try to list parts with non-existent part id
        final String nonexistentUploadId = "nonexistent-upload-id";
        try {
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, nonexistentUploadId);
            ossClient.listParts(listPartsRequest);
            Assert.fail("List parts should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_UPLOAD, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_UPLOAD_ERR));
        }

        // Try to list LIST_PART_MAX_RETURNS + 1 parts each time
        String uploadId = null;
        try {
            uploadId = claimUploadId(ossClient, bucketName, key);
            ListPartsRequest listPartsRequest = new ListPartsRequest(bucketName, key, uploadId);
            listPartsRequest.setMaxParts(LIST_PART_MAX_RETURNS + 1);
            ossClient.listParts(listPartsRequest);
            Assert.fail("List parts should not be successful");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
            AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(bucketName, key, uploadId);
            ossClient.abortMultipartUpload(abortMultipartUploadRequest);
        }
    }

    @Test
    public void testUnormalAbortMultipartUpload() {
        final String key = "unormal-abort-multipart-upload-object";

        // Try to abort multipart upload with non-existent part id
        final String nonexistentUploadId = "nonexistent-upload-id";
        try {
            AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(bucketName, key, nonexistentUploadId);
            ossClient.abortMultipartUpload(abortMultipartUploadRequest);
            Assert.fail("Abort multipart upload should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_UPLOAD, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_UPLOAD_ERR));
        }

        // Try to delete bucket with incompleted multipart uploads
        final String existingBucket = "unormal-abort-multipart-upload-existing-bucket-test";
        try {
            ossClient.createBucket(existingBucket);

            String uploadId = claimUploadId(ossClient, existingBucket, key);

            try {
                ossClient.deleteBucket(existingBucket);
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.BUCKET_NOT_EMPTY, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith("The bucket has Multipart Uploads."));
            }

            AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(existingBucket, key, uploadId);
            ossClient.abortMultipartUpload(abortMultipartUploadRequest);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            ossClient.deleteBucket(existingBucket);
        }
    }

    @Test
    public void testNormalListMultipartUploads() {
        try {
            // Add LIST_UPLOAD_MAX_RETURNS + 1 + lv2KeyCount objects to bucket
            List<String> existingKeys = new ArrayList<String>();
            final int lv1KeyCount = 101;
            final int lv2KeyCount = 11;
            final int multipartUploadCount = LIST_UPLOAD_MAX_RETURNS + 1 + lv2KeyCount;
            final String lv0KeyPrefix = "normal-list-multiparts-lv0-objects-";
            final String lv1KeyPrefix = "normal-list-multiparts-lv0-objects/lv1-objects-";
            final String lv2KeyPrefix = "normal-list-multiparts-lv0-objects/lv1-objects/lv2-objects-";
            for (int i = 0; i <= LIST_UPLOAD_MAX_RETURNS; i++) {
                if (i % 10 != 0) {
                    existingKeys.add(lv0KeyPrefix + i);
                } else {
                    existingKeys.add(lv1KeyPrefix + i);
                    if (i % 100 == 0) {
                        existingKeys.add(lv2KeyPrefix + i);
                    }
                }
            }

            // Upload single part for each multipart upload
            final int partSize = 128;     //128B
            List<String> uploadIds = new ArrayList<String>(multipartUploadCount);
            for (int i = 0; i < multipartUploadCount; i++) {
                String key = existingKeys.get(i);

                String uploadId = claimUploadId(ossClient, bucketName, key);
                uploadIds.add(uploadId);

                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                Assert.assertEquals(1, uploadPartResult.getPartNumber());
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
            }

            // List multipart uploads without any conditions
            ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            MultipartUploadListing multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_UPLOAD_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertTrue(multipartUploadListing.isTruncated());
            Assert.assertNotNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNotNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertNull(multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
            List<MultipartUpload> multipartUploads = multipartUploadListing.getMultipartUploads();
            Assert.assertEquals(LIST_UPLOAD_MAX_RETURNS, multipartUploads.size());
            for (int i = 0; i < LIST_UPLOAD_MAX_RETURNS; i++) {
                Assert.assertTrue(existingKeys.contains(multipartUploads.get(i).getKey()));
                Assert.assertTrue(uploadIds.contains(multipartUploads.get(i).getUploadId()));
            }

            String keyMarker = multipartUploadListing.getNextKeyMarker();
            String uploadIdMarker = multipartUploadListing.getNextUploadIdMarker();
            listMultipartUploadsRequest.setKeyMarker(keyMarker);
            listMultipartUploadsRequest.setUploadIdMarker(uploadIdMarker);
            multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_UPLOAD_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertFalse(multipartUploadListing.isTruncated());
            Assert.assertNotNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNotNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertNull(multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            multipartUploads = multipartUploadListing.getMultipartUploads();
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
            Assert.assertEquals(multipartUploadCount - LIST_UPLOAD_MAX_RETURNS, multipartUploads.size());
            for (int i = 0; i < (multipartUploadCount - LIST_UPLOAD_MAX_RETURNS); i++) {
                Assert.assertTrue(existingKeys.contains(multipartUploads.get(i).getKey()));
                Assert.assertTrue(uploadIds.contains(multipartUploads.get(i).getUploadId()));
            }

            // List 'max-uploads' multipart uploads with 'prefix'
            final int maxUploads = 100;
            listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            listMultipartUploadsRequest.setMaxUploads(maxUploads);
            listMultipartUploadsRequest.setPrefix(lv1KeyPrefix);
            multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(maxUploads, multipartUploadListing.getMaxUploads());
            Assert.assertTrue(multipartUploadListing.isTruncated());
            Assert.assertNotNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNotNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertEquals(lv1KeyPrefix, multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
            multipartUploads = multipartUploadListing.getMultipartUploads();
            Assert.assertEquals(maxUploads, multipartUploads.size());
            for (int i = 0; i < maxUploads; i++) {
                Assert.assertTrue(existingKeys.contains(multipartUploads.get(i).getKey()));
                Assert.assertTrue(uploadIds.contains(multipartUploads.get(i).getUploadId()));
            }

            keyMarker = multipartUploadListing.getNextKeyMarker();
            uploadIdMarker = multipartUploadListing.getNextUploadIdMarker();
            listMultipartUploadsRequest.setKeyMarker(keyMarker);
            listMultipartUploadsRequest.setUploadIdMarker(uploadIdMarker);
            multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(maxUploads, multipartUploadListing.getMaxUploads());
            Assert.assertFalse(multipartUploadListing.isTruncated());
            Assert.assertNotNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNotNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertEquals(lv1KeyPrefix, multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
            multipartUploads = multipartUploadListing.getMultipartUploads();
            Assert.assertEquals(lv1KeyCount - maxUploads, multipartUploads.size());
            for (int i = 0; i < (lv1KeyCount - maxUploads); i++) {
                Assert.assertTrue(existingKeys.contains(multipartUploads.get(i).getKey()));
                Assert.assertTrue(uploadIds.contains(multipartUploads.get(i).getUploadId()));
            }

            // List object with 'prefix' and 'delimiter'
            final String delimiter = "/";
            final String keyPrefix0 = "normal-list-multiparts-lv0-objects/";
            final String keyPrefix1 = "normal-list-multiparts-lv0-objects/lv1-objects/";
            listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            listMultipartUploadsRequest.setPrefix(keyPrefix0);
            listMultipartUploadsRequest.setDelimiter(delimiter);
            multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_UPLOAD_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertFalse(multipartUploadListing.isTruncated());
            Assert.assertNotNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNotNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertEquals(delimiter, multipartUploadListing.getDelimiter());
            Assert.assertEquals(keyPrefix0, multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
            multipartUploads = multipartUploadListing.getMultipartUploads();
            Assert.assertEquals(lv1KeyCount, multipartUploads.size());
            for (int i = 0; i < lv1KeyCount; i++) {
                Assert.assertTrue(existingKeys.contains(multipartUploads.get(i).getKey()));
                Assert.assertTrue(uploadIds.contains(multipartUploads.get(i).getUploadId()));
            }
            Assert.assertEquals(1, multipartUploadListing.getCommonPrefixes().size());
            Assert.assertEquals(keyPrefix1, multipartUploadListing.getCommonPrefixes().get(0));

            // Abort all incompleted multipart uploads 
            for (int i = 0; i < multipartUploadCount; i++) {
                AbortMultipartUploadRequest abortMultipartUploadRequest =
                        new AbortMultipartUploadRequest(bucketName, existingKeys.get(i), uploadIds.get(i));
                ossClient.abortMultipartUpload(abortMultipartUploadRequest);
            }

            // List all incompleted multipart uploads under this bucket again
            listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(0, multipartUploadListing.getMultipartUploads().size());
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_UPLOAD_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertFalse(multipartUploadListing.isTruncated());
            Assert.assertNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertNull(multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalListMultipartUploadsWithEncoding() {
        try {
            // Add LIST_UPLOAD_MAX_RETURNS + 1 + lv2KeyCount objects to bucket
            List<String> existingKeys = new ArrayList<String>();
            final int lv2KeyCount = 11;
            final int multipartUploadCount = LIST_UPLOAD_MAX_RETURNS + 1 + lv2KeyCount;
            final String lv0KeyPrefix = "常记溪亭日暮，沉醉不知归路。";
            final String lv1KeyPrefix = "常记溪亭日暮，沉醉不知归路。/昨夜雨疏风骤，浓睡不消残酒。-";
            final String lv2KeyPrefix = "常记溪亭日暮，沉醉不知归路。/昨夜雨疏风骤，浓睡不消残酒。/湖上风来波浩渺，秋已暮、红稀香少。-";
            for (int i = 0; i <= LIST_UPLOAD_MAX_RETURNS; i++) {
                if (i % 10 != 0) {
                    existingKeys.add(lv0KeyPrefix + i);
                } else {
                    existingKeys.add(lv1KeyPrefix + i);
                    if (i % 100 == 0) {
                        existingKeys.add(lv2KeyPrefix + i);
                    }
                }
            }

            // Upload single part for each multipart upload
            final int partSize = 128;     //128B
            List<String> uploadIds = new ArrayList<String>(multipartUploadCount);
            for (int i = 0; i < multipartUploadCount; i++) {
                String key = existingKeys.get(i);

                String uploadId = claimUploadId(ossClient, bucketName, key);
                uploadIds.add(uploadId);

                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                Assert.assertEquals(1, uploadPartResult.getPartNumber());
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
            }

            // List multipart uploads without any conditions
            ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            listMultipartUploadsRequest.setEncodingType(DEFAULT_ENCODING_TYPE);
            MultipartUploadListing multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.assertEquals(bucketName, multipartUploadListing.getBucketName());
            Assert.assertEquals(LIST_UPLOAD_MAX_RETURNS, multipartUploadListing.getMaxUploads());
            Assert.assertTrue(multipartUploadListing.isTruncated());
            Assert.assertNotNull(multipartUploadListing.getNextKeyMarker());
            Assert.assertNotNull(multipartUploadListing.getNextUploadIdMarker());
            Assert.assertNull(multipartUploadListing.getDelimiter());
            Assert.assertNull(multipartUploadListing.getPrefix());
            Assert.assertNull(multipartUploadListing.getKeyMarker());
            Assert.assertNull(multipartUploadListing.getUploadIdMarker());
            Assert.assertEquals(multipartUploadListing.getRequestId().length(), REQUEST_ID_LEN);
            List<MultipartUpload> multipartUploads = multipartUploadListing.getMultipartUploads();
            Assert.assertEquals(LIST_UPLOAD_MAX_RETURNS, multipartUploads.size());
            for (int i = 0; i < LIST_UPLOAD_MAX_RETURNS; i++) {
                Assert.assertTrue(existingKeys.contains(URLDecoder.decode(multipartUploads.get(i).getKey(), "UTF-8")));
                Assert.assertTrue(uploadIds.contains(multipartUploads.get(i).getUploadId()));
            }

            // Abort all incompleted multipart uploads 
            for (int i = 0; i < multipartUploadCount; i++) {
                AbortMultipartUploadRequest abortMultipartUploadRequest =
                        new AbortMultipartUploadRequest(bucketName, existingKeys.get(i), uploadIds.get(i));
                ossClient.abortMultipartUpload(abortMultipartUploadRequest);
            }

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUnormalListMultipartUploads() {
        final String key = "unormal-list-multipart-uploads-object";

        // Try to list multipart uploads under non-existent bucket
        final String nonexistentBucket = "nonexistent-upload-id";
        try {
            ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(nonexistentBucket);
            ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.fail("List multipart uploads should not be successful");
        } catch (OSSException e) {
            Assert.assertEquals(OSSErrorCode.NO_SUCH_BUCKET, e.getErrorCode());
            Assert.assertTrue(e.getMessage().startsWith(NO_SUCH_BUCKET_ERR));
        }

        // Try to list LIST_UPLOAD_MAX_RETURNS + 1 parts each time
        final int maxUploads = LIST_UPLOAD_MAX_RETURNS + 1;
        String uploadId = null;
        try {
            uploadId = claimUploadId(ossClient, bucketName, key);
            ListMultipartUploadsRequest listMultipartUploadsRequest = new ListMultipartUploadsRequest(bucketName);
            listMultipartUploadsRequest.setMaxUploads(maxUploads);
            ossClient.listMultipartUploads(listMultipartUploadsRequest);
            Assert.fail("List multipart uploads should not be successful");
        } catch (Exception e) {
            Assert.assertTrue(e instanceof IllegalArgumentException);
            AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(bucketName, key, uploadId);
            ossClient.abortMultipartUpload(abortMultipartUploadRequest);
        }
    }

    @Test
    public void testNormalCompleteMultipartUpload() {
        final String key = "normal-complete-multipart-upload-object";
        final int partSize = 128 * 1024;     //128KB
        final int partCount = 10;

        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);

            // Upload parts
            List<PartETag> partETags = new ArrayList<PartETag>();
            for (int i = 0; i < partCount; i++) {
                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                partETags.add(uploadPartResult.getPartETag());
            }

            // Complete multipart upload with all uploaded parts
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                    completeMultipartUploadResult.getLocation());
            Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
            Assert.assertEquals(key, completeMultipartUploadResult.getKey());
            Assert.assertEquals(calcMultipartsETag(partETags), completeMultipartUploadResult.getETag());
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

            // Get uploaded object
            OSSObject o = ossClient.getObject(bucketName, key);
            long objectSize = partCount * partSize;
            Assert.assertEquals(objectSize, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(calcMultipartsETag(partETags), o.getObjectMetadata().getETag());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);

            // Reclaim upload id
            uploadId = claimUploadId(ossClient, bucketName, key);

            // Upload parts again
            partETags.clear();
            for (int i = 0; i < partCount; i++) {
                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                partETags.add(uploadPartResult.getPartETag());
            }

            // Complete multipart upload with some discontinuous parts
            List<PartETag> discontinuousPartETags = new ArrayList<PartETag>();
            discontinuousPartETags.add(partETags.get(0));
            discontinuousPartETags.add(partETags.get(4));
            discontinuousPartETags.add(partETags.get(7));
            completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucketName, key, uploadId, discontinuousPartETags);
            completeMultipartUploadResult = ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                    completeMultipartUploadResult.getLocation());
            Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
            Assert.assertEquals(key, completeMultipartUploadResult.getKey());
            Assert.assertEquals(calcMultipartsETag(discontinuousPartETags), completeMultipartUploadResult.getETag());
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

            // Get uploaded object again
            o = ossClient.getObject(bucketName, key);
            objectSize = discontinuousPartETags.size() * partSize;
            Assert.assertEquals(objectSize, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(calcMultipartsETag(discontinuousPartETags), o.getObjectMetadata().getETag());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUnormalCompleteMultipartUpload() {
        final String key = "unormal-complete-multipart-upload-object";
        final int partSize = 128 * 1024;     //128KB
        final int partCount = 10;

        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);

            // Set length of parts less than minimum limit(100KB)
            final int invalidPartSize = 64 * 1024;
            List<PartETag> partETags = new ArrayList<PartETag>();
            for (int i = 0; i < partCount; i++) {
                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setPartSize(invalidPartSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                partETags.add(uploadPartResult.getPartETag());
            }

            // Try to complete multipart upload with all uploaded parts
            try {
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
                ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.ENTITY_TOO_SMALL, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(ENTITY_TOO_SMALL_ERR));
            }

            // Abort the incompleted multipart upload
            AbortMultipartUploadRequest abortMultipartUploadRequest =
                    new AbortMultipartUploadRequest(bucketName, key, uploadId);
            ossClient.abortMultipartUpload(abortMultipartUploadRequest);

            // Reclaim upload id
            uploadId = claimUploadId(ossClient, bucketName, key);

            // Upload parts again
            partETags.clear();
            for (int i = 0; i < partCount; i++) {
                InputStream instream = genFixedLengthInputStream(partSize);
                UploadPartRequest uploadPartRequest = new UploadPartRequest();
                uploadPartRequest.setBucketName(bucketName);
                uploadPartRequest.setKey(key);
                uploadPartRequest.setInputStream(instream);
                uploadPartRequest.setPartNumber(i + 1);
                uploadPartRequest.setPartSize(partSize);
                uploadPartRequest.setUploadId(uploadId);
                UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                partETags.add(uploadPartResult.getPartETag());
            }

            // Given part's ETag not match with actual ETag
            final String invalidETag = "Invalid-ETag";
            final int partNumber = 4;
            PartETag originalPartETag = partETags.get(partNumber - 1);
            PartETag invalidPartETag = new PartETag(partNumber, invalidETag);
            partETags.set(partNumber - 1, invalidPartETag);
            try {
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
                ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                Assert.fail("Complete multipart upload should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.INVALID_PART, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(INVALID_PART_ERR));
            } finally {
                partETags.set(partNumber - 1, originalPartETag);
            }

            // Try to complete multipart upload with non-existent part
            PartETag nonexistentPartETag = new PartETag(partCount + 1, invalidETag);
            partETags.add(nonexistentPartETag);
            try {
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
                ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                Assert.fail("Complete multipart upload should not be successful");
            } catch (OSSException e) {
                Assert.assertEquals(OSSErrorCode.INVALID_PART, e.getErrorCode());
                Assert.assertTrue(e.getMessage().startsWith(INVALID_PART_ERR));
            } finally {
                partETags.remove(partCount);
            }

            // Complete multipart upload with all uploaded parts
            CompleteMultipartUploadRequest completeMultipartUploadRequest =
                    new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);
            Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                    completeMultipartUploadResult.getLocation());
            Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
            Assert.assertEquals(key, completeMultipartUploadResult.getKey());
            Assert.assertEquals(calcMultipartsETag(partETags), completeMultipartUploadResult.getETag());
            Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

            // Get uploaded object
            OSSObject o = ossClient.getObject(bucketName, key);
            long objectSize = partCount * partSize;
            Assert.assertEquals(objectSize, o.getObjectMetadata().getContentLength());
            Assert.assertEquals(calcMultipartsETag(partETags), o.getObjectMetadata().getETag());
            Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testUploadPartWithChunked() {
        final String key = "upload-part-with-chunked-object";
        final int partSize = 128 * 1024;     //128KB
        final int partCount = 10;

        String uploadId = null;
        String filePath = null;

        {
            try {
                uploadId = claimUploadId(ossClient, bucketName, key);

                // Upload parts
                List<PartETag> partETags = new ArrayList<PartETag>();
                for (int i = 0; i < partCount; i++) {
                    InputStream instream = genFixedLengthInputStream(partSize);
                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(bucketName);
                    uploadPartRequest.setKey(key);
                    uploadPartRequest.setInputStream(instream);
                    uploadPartRequest.setPartNumber(i + 1);
                    uploadPartRequest.setUseChunkEncoding(true);
                    uploadPartRequest.setUploadId(uploadId);
                    UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                    Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                    partETags.add(uploadPartResult.getPartETag());
                }

                // Complete multipart upload with all uploaded parts
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
                CompleteMultipartUploadResult completeMultipartUploadResult =
                        ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                        completeMultipartUploadResult.getLocation());
                Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
                Assert.assertEquals(key, completeMultipartUploadResult.getKey());
                Assert.assertEquals(calcMultipartsETag(partETags), completeMultipartUploadResult.getETag());
                Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

                // Get uploaded object
                OSSObject o = ossClient.getObject(bucketName, key);
                long objectSize = partCount * partSize;
                Assert.assertEquals(objectSize, o.getObjectMetadata().getContentLength());
                Assert.assertEquals(calcMultipartsETag(partETags), o.getObjectMetadata().getETag());
                Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            }
        }

        {
            try {
                uploadId = claimUploadId(ossClient, bucketName, key);
                filePath = genFixedLengthFile(partSize * partCount);

                // Upload parts
                List<PartETag> partETags = new ArrayList<PartETag>();
                for (int i = 0; i < partCount; i++) {
                    InputStream instream = new FileInputStream(new File(filePath));
                    instream.skip(i * partSize);
                    UploadPartRequest uploadPartRequest = new UploadPartRequest();
                    uploadPartRequest.setBucketName(bucketName);
                    uploadPartRequest.setKey(key);
                    uploadPartRequest.setInputStream(instream);
                    uploadPartRequest.setPartNumber(i + 1);
                    uploadPartRequest.setPartSize(partSize);
                    uploadPartRequest.setUseChunkEncoding(true);
                    uploadPartRequest.setUploadId(uploadId);
                    UploadPartResult uploadPartResult = ossClient.uploadPart(uploadPartRequest);
                    Assert.assertEquals(uploadPartResult.getRequestId().length(), REQUEST_ID_LEN);
                    partETags.add(uploadPartResult.getPartETag());
                }

                // Complete multipart upload with all uploaded parts
                CompleteMultipartUploadRequest completeMultipartUploadRequest =
                        new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
                CompleteMultipartUploadResult completeMultipartUploadResult =
                        ossClient.completeMultipartUpload(completeMultipartUploadRequest);
                Assert.assertEquals(composeLocation(ossClient, OSS_TEST_ENDPOINT, bucketName, key),
                        completeMultipartUploadResult.getLocation());
                Assert.assertEquals(bucketName, completeMultipartUploadResult.getBucketName());
                Assert.assertEquals(key, completeMultipartUploadResult.getKey());
                Assert.assertEquals(calcMultipartsETag(partETags), completeMultipartUploadResult.getETag());
                Assert.assertEquals(completeMultipartUploadResult.getRequestId().length(), REQUEST_ID_LEN);

                // Get uploaded object
                OSSObject o = ossClient.getObject(bucketName, key);
                long objectSize = partCount * partSize;
                Assert.assertEquals(objectSize, o.getObjectMetadata().getContentLength());
                Assert.assertEquals(calcMultipartsETag(partETags), o.getObjectMetadata().getETag());
                Assert.assertEquals(o.getRequestId().length(), REQUEST_ID_LEN);
            } catch (Exception e) {
                Assert.fail(e.getMessage());
            } finally {
                removeFile(filePath);
            }
        }
    }

    @Ignore
    public void testDeleteAllBuckets() {
        try {
            List<Bucket> returnedBuckets = ossClient.listBuckets();
            for (Bucket bkt : returnedBuckets) {
                String bktName = bkt.getName();
                String keyMarker = null;
                String uploadIdMarker = null;
                ListMultipartUploadsRequest listMultipartUploadsRequest = null;
                MultipartUploadListing multipartUploadListing = null;
                List<MultipartUpload> multipartUploads = null;
                do {
                    listMultipartUploadsRequest = new ListMultipartUploadsRequest(bktName);
                    listMultipartUploadsRequest.setKeyMarker(keyMarker);
                    listMultipartUploadsRequest.setUploadIdMarker(uploadIdMarker);

                    multipartUploadListing = ossClient.listMultipartUploads(listMultipartUploadsRequest);
                    multipartUploads = multipartUploadListing.getMultipartUploads();
                    for (MultipartUpload mu : multipartUploads) {
                        String key = mu.getKey();
                        String uploadId = mu.getUploadId();
                        ossClient.abortMultipartUpload(new AbortMultipartUploadRequest(bktName, key, uploadId));
                    }

                    keyMarker = multipartUploadListing.getKeyMarker();
                    uploadIdMarker = multipartUploadListing.getUploadIdMarker();
                } while (multipartUploadListing != null && multipartUploadListing.isTruncated());

                deleteBucketWithObjects(ossClient, bktName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalUploadWithEmptyPart() {
        final String key = "normal-upload-empty-part-object";
        final int partSize = 128 * 1024;     //128KB

        try {
            String uploadId = claimUploadId(ossClient, bucketName, key);
            List<PartETag> partETags = new ArrayList();

            // Complete multipart upload
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucketName, key, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult =
                    ossClient.completeMultipartUpload(completeMultipartUploadRequest);

        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testNormalUploadWithCompleteAllFlag() {
        final String key1 = "normal-upload-empty-part-object-1";
        final String key2 = "normal-upload-empty-part-object-2";
        final int partSize1 = 128 * 1024;     //128KB
        final int partSize2 = 63 * 1024;     //128KB

        try {
            String uploadId = claimUploadId(ossClient, bucketName, key1);

            List<PartETag> partETags = new ArrayList<PartETag>();

            // Upload part1 part
            InputStream instream = genFixedLengthInputStream(partSize1);
            UploadPartRequest uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key1);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(1);
            uploadPartRequest.setPartSize(partSize1);
            uploadPartRequest.setUploadId(uploadId);
            UploadPartResult uploadPartResult =  ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());

            // Upload part2 part
            instream = genFixedLengthInputStream(partSize2);
            uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key1);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(2);
            uploadPartRequest.setPartSize(partSize2);
            uploadPartRequest.setUploadId(uploadId);
            uploadPartResult =   ossClient.uploadPart(uploadPartRequest);
            partETags.add(uploadPartResult.getPartETag());

            // Complete multipart upload
            CompleteMultipartUploadRequest completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucketName, key1, uploadId, partETags);
            CompleteMultipartUploadResult completeMultipartUploadResult1 = ossClient.completeMultipartUpload(completeMultipartUploadRequest);


            uploadId = claimUploadId(ossClient, bucketName, key2);
            instream = genFixedLengthInputStream(partSize1);
            uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key2);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(1);
            uploadPartRequest.setPartSize(partSize1);
            uploadPartRequest.setUploadId(uploadId);
            ossClient.uploadPart(uploadPartRequest);

            // Upload part2 part
            instream = genFixedLengthInputStream(partSize2);
            uploadPartRequest = new UploadPartRequest();
            uploadPartRequest.setBucketName(bucketName);
            uploadPartRequest.setKey(key2);
            uploadPartRequest.setInputStream(instream);
            uploadPartRequest.setPartNumber(2);
            uploadPartRequest.setPartSize(partSize2);
            uploadPartRequest.setUploadId(uploadId);
            ossClient.uploadPart(uploadPartRequest);

            completeMultipartUploadRequest = new CompleteMultipartUploadRequest(bucketName, key2, uploadId, null);
            completeMultipartUploadRequest.addHeader("x-oss-complete-all", "yes");
            CompleteMultipartUploadResult completeMultipartUploadResult2 = ossClient.completeMultipartUpload(completeMultipartUploadRequest);

            ObjectMetadata meta1 = ossClient.getObjectMetadata(bucketName, key1);
            ObjectMetadata meta2 = ossClient.getObjectMetadata(bucketName, key2);

            Assert.assertEquals(meta1.getContentLength(), meta2.getContentLength());
            Assert.assertEquals(meta1.getServerCRC(), meta2.getServerCRC());
            Assert.assertEquals(meta1.getETag(), meta2.getETag());


        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

}
