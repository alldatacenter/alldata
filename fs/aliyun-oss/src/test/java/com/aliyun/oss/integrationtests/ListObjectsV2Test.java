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

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.OSSClient;
import com.aliyun.oss.OSSErrorCode;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.auth.DefaultCredentials;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import junit.framework.Assert;
import org.junit.Test;
import java.io.ByteArrayInputStream;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aliyun.oss.integrationtests.TestUtils.batchPutObject;
import static com.aliyun.oss.integrationtests.TestUtils.waitForCacheExpiration;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_CHARSET_NAME;

public class ListObjectsV2Test extends TestBase {
    private static final int DEFAULT_MAX_RETURNED_KEYS = 100;

    private OSSClient ossClient;
    private String bucketName;
    private String endpoint;

    public void setUp() throws Exception {
        super.setUp();

        bucketName = super.bucketName + "-list-v2";
        endpoint = "http://oss-ap-southeast-2.aliyuncs.com";

        // create client
        ClientConfiguration conf = new ClientConfiguration().setSupportCname(false);
        Credentials credentials = new DefaultCredentials(TestConfig.OSS_TEST_ACCESS_KEY_ID, TestConfig.OSS_TEST_ACCESS_KEY_SECRET);
        ossClient = new OSSClient(endpoint, new DefaultCredentialProvider(credentials), conf);

        ossClient.createBucket(bucketName);
        waitForCacheExpiration(2);
    }

    public void tearDown() throws Exception {
        if (ossClient != null) {
            ossClient.shutdown();
            ossClient = null;
        }
        super.tearDown();
    }

    @Test
    public void testNormalListObjects() {
        final String bucketName = super.bucketName + "-normalist-objectsv2";

        try {
            ossClient.createBucket(bucketName);

            // List objects under empty bucket
            ListObjectsV2Result result = ossClient.listObjectsV2(bucketName);
            Assert.assertEquals(0, result.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getMaxKeys());
            Assert.assertEquals(0, result.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(0, result.getKeyCount());
            Assert.assertNull(result.getEncodingType());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNull(result.getNextContinuationToken());
            Assert.assertNull(result.getStartAfter());
            Assert.assertNull(result.getDelimiter());
            Assert.assertNull(result.getPrefix());
            Assert.assertFalse(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            // 99 files under sub-dir
            String dir1 = "test-dir/";
            String subDir = "sub-dir/";
            String keyPrefix = dir1 + subDir + "test-file";
            for (int i = 0; i < 99; i++) {
                String key = keyPrefix + i + ".txt";
                ossClient.putObject(bucketName, key, new ByteArrayInputStream("1".getBytes()));
            }

            // 1 file under dir1
            ossClient.putObject(bucketName, dir1 + "sub-file.txt", new ByteArrayInputStream("1".getBytes()));

            // 3 top dir files
            String bigLetterPrefix = "z";
            ossClient.putObject(bucketName, bigLetterPrefix + "1.txt", new ByteArrayInputStream("1".getBytes()));
            ossClient.putObject(bucketName, bigLetterPrefix +"2.txt", new ByteArrayInputStream("1".getBytes()));
            ossClient.putObject(bucketName, bigLetterPrefix + "3.txt", new ByteArrayInputStream("1".getBytes()));

            // List objects under nonempty bucket
            result = ossClient.listObjectsV2(bucketName);
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getMaxKeys());
            Assert.assertEquals(0, result.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getKeyCount());
            Assert.assertNull(result.getEncodingType());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNotNull(result.getNextContinuationToken());
            Assert.assertNull(result.getStartAfter());
            Assert.assertNull(result.getDelimiter());
            Assert.assertNull(result.getPrefix());
            Assert.assertTrue(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            // List object with continuationToken.
            String continuationToken = result.getNextContinuationToken();
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request(bucketName)
                    .withContinuationToken(continuationToken);
            result = ossClient.listObjectsV2(listObjectsV2Request);
            Assert.assertEquals(3, result.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getMaxKeys());
            Assert.assertEquals(0, result.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(3, result.getKeyCount());
            Assert.assertNull(result.getEncodingType());
            Assert.assertEquals(continuationToken, result.getContinuationToken());
            Assert.assertNull(result.getNextContinuationToken());
            Assert.assertNull(result.getDelimiter());
            Assert.assertNull(result.getPrefix());
            Assert.assertNull(result.getStartAfter());
            Assert.assertFalse(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            // List objects with prefix
            result = ossClient.listObjectsV2(bucketName, bigLetterPrefix);
            Assert.assertEquals(3, result.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getMaxKeys());
            Assert.assertEquals(0, result.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(3, result.getKeyCount());
            Assert.assertNull(result.getEncodingType());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNull(result.getNextContinuationToken());
            Assert.assertNull(result.getStartAfter());
            Assert.assertNull(result.getDelimiter());
            Assert.assertEquals(bigLetterPrefix, result.getPrefix());
            Assert.assertFalse(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            // List objects with prefix and delimiter
            listObjectsV2Request = new ListObjectsV2Request(bucketName, bigLetterPrefix);
            listObjectsV2Request.setDelimiter("/");
            result = ossClient.listObjectsV2(listObjectsV2Request);
            Assert.assertEquals(3, result.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getMaxKeys());
            Assert.assertEquals(0, result.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(3, result.getKeyCount());
            Assert.assertNull(result.getEncodingType());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNull(result.getNextContinuationToken());
            Assert.assertEquals("/", result.getDelimiter());
            Assert.assertEquals(bigLetterPrefix, result.getPrefix());
            Assert.assertNull(result.getStartAfter());
            Assert.assertFalse(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            // List object with prefix, delimiter, encodeType
            result = ossClient.listObjectsV2(bucketName, dir1, null, null,
                    "/", null, DEFAULT_ENCODING_TYPE, false);
            Assert.assertEquals(1, result.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getMaxKeys());
            Assert.assertEquals(1, result.getCommonPrefixes().size());
            Assert.assertEquals(HttpUtil.urlEncode(dir1 + subDir,DEFAULT_CHARSET_NAME), result.getCommonPrefixes().get(0));
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(2, result.getKeyCount());
            Assert.assertEquals(DEFAULT_ENCODING_TYPE, result.getEncodingType());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNull(result.getNextContinuationToken());
            String delimiter = result.getDelimiter();
            Assert.assertEquals(HttpUtil.urlEncode("/",DEFAULT_CHARSET_NAME), delimiter);
            Assert.assertEquals("%2F", delimiter);
            Assert.assertEquals(HttpUtil.urlEncode(dir1, DEFAULT_CHARSET_NAME), result.getPrefix());
            Assert.assertNull(result.getStartAfter());
            Assert.assertFalse(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);

            // List objects with startAfter
            listObjectsV2Request = new ListObjectsV2Request(bucketName);
            listObjectsV2Request.setStartAfter(bigLetterPrefix);
            result = ossClient.listObjectsV2(listObjectsV2Request);
            Assert.assertEquals(3, result.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getMaxKeys());
            Assert.assertEquals(0, result.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(3, result.getKeyCount());
            Assert.assertNull(result.getEncodingType());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNull(result.getNextContinuationToken());
            Assert.assertNull(result.getDelimiter());
            Assert.assertNull(result.getPrefix());
            Assert.assertEquals(bigLetterPrefix, result.getStartAfter());
            Assert.assertFalse(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, bucketName);
        }
    }

    @Test
    public void testNormalListObjectsFetchOwner() {
        final String bucketName = super.bucketName + "list-objects-v2-fetch-owner";

        try {
            ossClient.createBucket(bucketName);

            ossClient.putObject(bucketName, "1.txt", new ByteArrayInputStream("1".getBytes()));
            ossClient.putObject(bucketName, "2.txt", new ByteArrayInputStream("1".getBytes()));

            // List objects without fetch-owner
            ListObjectsV2Result result = ossClient.listObjectsV2(bucketName);
            Assert.assertEquals(2, result.getKeyCount());
            List<OSSObjectSummary> ossObjectSummaries = result.getObjectSummaries();
            Assert.assertEquals(2, ossObjectSummaries.size());
            for (OSSObjectSummary obj : ossObjectSummaries) {
                Assert.assertEquals(bucketName, obj.getBucketName());
                Assert.assertNotNull(obj.getKey());
                Assert.assertNotNull(obj.getType());
                Assert.assertEquals(1, obj.getSize());
                Assert.assertNotNull(obj.getETag());
                Assert.assertNotNull(obj.getLastModified());
                Assert.assertNotNull(obj.getStorageClass());
                Assert.assertNull(obj.getOwner());
            }

            // List objects with fetch-owner
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request(bucketName);
            listObjectsV2Request.setFetchOwner(true);
            result = ossClient.listObjectsV2(listObjectsV2Request);
            Assert.assertEquals(2, result.getKeyCount());
            ossObjectSummaries = result.getObjectSummaries();
            Assert.assertEquals(2, ossObjectSummaries.size());
            for (OSSObjectSummary obj : ossObjectSummaries) {
                Assert.assertEquals(bucketName, obj.getBucketName());
                Assert.assertNotNull(obj.getKey());
                Assert.assertNotNull(obj.getType());
                Assert.assertEquals(1, obj.getSize());
                Assert.assertNotNull(obj.getETag());
                Assert.assertNotNull(obj.getLastModified());
                Assert.assertNotNull(obj.getStorageClass());
                Assert.assertNotNull(obj.getOwner());
                Assert.assertNotNull(obj.getOwner().getId());
                Assert.assertNotNull(obj.getOwner().getDisplayName());
            }
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, bucketName);
        }
    }

    @Test
    public void testNormalListObjectsWithMaxKeys() {
        final String bucketName = super.bucketName + "-normalist-objects-v2-maxkeys";

        try {
            ossClient.createBucket(bucketName);

            ossClient.putObject(bucketName,  "1.txt", new ByteArrayInputStream("1".getBytes()));
            ossClient.putObject(bucketName, "2.txt", new ByteArrayInputStream("1".getBytes()));
            ossClient.putObject(bucketName, "3.txt", new ByteArrayInputStream("1".getBytes()));

            // List objects under nonempty bucket
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request(bucketName);
            listObjectsV2Request.setMaxKeys(2);
            ListObjectsV2Result result = ossClient.listObjectsV2(listObjectsV2Request);
            Assert.assertEquals(2, result.getObjectSummaries().size());
            Assert.assertEquals(2, result.getMaxKeys());
            Assert.assertEquals(0, result.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(2, result.getKeyCount());
            Assert.assertNull(result.getEncodingType());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNotNull(result.getNextContinuationToken());
            Assert.assertNull(result.getStartAfter());
            Assert.assertNull(result.getDelimiter());
            Assert.assertNull(result.getPrefix());
            Assert.assertTrue(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        } finally {
            deleteBucketWithObjects(ossClient, bucketName);
        }
    }

    @Test
    public void testListObjectsWithEncodingType() {
        final String objectPrefix = "object-with-special-characters";

        try {
            // Add several objects with special characters into bucket.
            List<String> existingKeys = new ArrayList<String>();
            existingKeys.add(objectPrefix + "\001\007");
            existingKeys.add(objectPrefix + "\002\007");
            
            if (!batchPutObject(ossClient, bucketName, existingKeys)) {
                Assert.fail("batch put object failed");
            }
            
            ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request(bucketName);
            try {
                ossClient.listObjectsV2(listObjectsV2Request);
            } catch (Exception e) {
                Assert.assertTrue(e instanceof OSSException);
                Assert.assertEquals(OSSErrorCode.INVALID_RESPONSE, ((OSSException)e).getErrorCode());
            }
            
            // List objects under nonempty bucket
            listObjectsV2Request = new ListObjectsV2Request(bucketName);
            listObjectsV2Request.setEncodingType(DEFAULT_ENCODING_TYPE);
            ListObjectsV2Result result = ossClient.listObjectsV2(listObjectsV2Request);
            for (OSSObjectSummary s : result.getObjectSummaries()) {
                String decodedKey = URLDecoder.decode(s.getKey(), "UTF-8");
                Assert.assertTrue(existingKeys.contains(decodedKey));
            }
            Assert.assertEquals(2, result.getObjectSummaries().size());
            Assert.assertEquals(DEFAULT_MAX_RETURNED_KEYS, result.getMaxKeys());
            Assert.assertEquals(0, result.getCommonPrefixes().size());
            Assert.assertEquals(bucketName, result.getBucketName());
            Assert.assertEquals(2, result.getKeyCount());
            Assert.assertEquals(DEFAULT_ENCODING_TYPE, result.getEncodingType());
            Assert.assertNull(result.getContinuationToken());
            Assert.assertNull(result.getNextContinuationToken());
            Assert.assertNull(result.getDelimiter());
            Assert.assertNull(result.getPrefix());
            Assert.assertNull(result.getStartAfter());
            Assert.assertFalse(result.isTruncated());
            Assert.assertEquals(result.getRequestId().length(), REQUEST_ID_LEN);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void testRequestMethodChaining() {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix("fun")
                .withContinuationToken("tokenTest")
                .withStartAfter("A")
                .withDelimiter("/")
                .withMaxKeys(10)
                .withEncodingType("url")
                .withFetchOwner(true);
        Assert.assertEquals(bucketName, request.getBucketName());
        Assert.assertEquals("fun", request.getPrefix());
        Assert.assertEquals("tokenTest", request.getContinuationToken());
        Assert.assertEquals("A", request.getStartAfter());
        Assert.assertEquals("/", request.getDelimiter());
        Assert.assertEquals(Integer.valueOf(10), request.getMaxKeys());
        Assert.assertEquals("url", request.getEncodingType());
        Assert.assertTrue(request.isFetchOwner());
    }

    @Test
    public void testUnnormalListObjects() {
        final String bucketName = super.bucketName + "-unormal-list-objects";

        try {
            ossClient.createBucket(bucketName);
            ossClient.putObject(bucketName,  "1.txt", new ByteArrayInputStream("1".getBytes()));
            ossClient.putObject(bucketName, "2.txt", new ByteArrayInputStream("1".getBytes()));
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        // List object with nonexistent continueToken.
        final String nonexistentContinueToken = "none-exist-continue-token";
        try {
            ListObjectsV2Request request = new ListObjectsV2Request(bucketName);
            request.setContinuationToken(nonexistentContinueToken);
            ossClient.listObjectsV2(request);
            Assert.fail("should be failed here.");
        } catch (OSSException e) {
            Assert.assertEquals(e.getErrorCode(),OSSErrorCode.INVALID_ARGUMENT);
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

            ListObjectsV2Request listObjectsRequest = new ListObjectsV2Request(bucketName);
            ListObjectsV2Result objectListing = ossClient.listObjectsV2(listObjectsRequest);
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
                objectListing = ossClient.listObjectsV2(listObjectsRequest);
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
