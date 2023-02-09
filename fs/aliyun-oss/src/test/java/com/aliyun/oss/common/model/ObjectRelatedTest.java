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

package com.aliyun.oss.common.model;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.internal.OSSHeaders;
import com.aliyun.oss.model.*;
import org.junit.Test;

import java.net.URL;
import java.util.*;

import static org.junit.Assert.*;


public class ObjectRelatedTest {

    @Test
    public void testCallback() {
        Callback callback = new Callback();
        assertEquals(false, callback.hasCallbackVar());

        Map<String, String> callbackVar = new HashMap<String, String>();
        callback.setCallbackVar(callbackVar);
        assertEquals(false, callback.hasCallbackVar());

        callbackVar.put("key1", "value1");
        callback.setCallbackVar(callbackVar);
        assertEquals(true, callback.hasCallbackVar());

        callback.setCallbackVar(null);
        assertEquals(false, callback.hasCallbackVar());

        assertEquals("2", Callback.CalbackBodyType.JSON.toString());
    }

    @Test
    public void testCopyObjectRequest() {
        CopyObjectRequest request = new CopyObjectRequest("src-bucket", "src-key", "dst-bucket", "dst-key");

        List<String> matchingETagConstraints = new ArrayList<String>();
        assertEquals(0, request.getMatchingETagConstraints().size());
        request.setMatchingETagConstraints(matchingETagConstraints);
        assertEquals(0, request.getMatchingETagConstraints().size());
        matchingETagConstraints.add("123");
        request.setMatchingETagConstraints(matchingETagConstraints);
        assertEquals(matchingETagConstraints, request.getMatchingETagConstraints());
        request.setMatchingETagConstraints(null);
        assertEquals(0, request.getMatchingETagConstraints().size());

        List<String> nonmatchingEtagConstraints = new ArrayList<String>();
        assertEquals(0, request.getNonmatchingEtagConstraints().size());
        request.setNonmatchingETagConstraints(nonmatchingEtagConstraints);
        assertEquals(0, request.getNonmatchingEtagConstraints().size());
        nonmatchingEtagConstraints.add("123");
        request.setNonmatchingETagConstraints(nonmatchingEtagConstraints);
        assertEquals(nonmatchingEtagConstraints, request.getNonmatchingEtagConstraints());
        request.setNonmatchingETagConstraints(null);
        assertEquals(0, request.getNonmatchingEtagConstraints().size());

        request.setNonmatchingETagConstraints(nonmatchingEtagConstraints);
        request.clearNonmatchingETagConstraints();
        assertEquals(0, request.getNonmatchingEtagConstraints().size());

        request = new CopyObjectRequest("src-bucket", "src-key", "id", "dst-bucket", "dst-key");
        request.clearMatchingETagConstraints();
    }

    @Test
    public void testDeleteObjectsRequest() {
        List<String> keys = new ArrayList<String>();
        keys.add("key1");
        keys.add("key2");

        DeleteObjectsRequest request = new DeleteObjectsRequest("bucket")
                .withQuiet(true).withEncodingType("type").withKeys(keys);
        assertTrue(request.isQuiet());
        assertEquals("type", request.getEncodingType());
        assertEquals(keys, request.getKeys());

        try {
            request.setKeys(null);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            keys = new ArrayList<String>();
            request.setKeys(keys);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            keys = new ArrayList<String>();
            keys.add(null);
            request.setKeys(keys);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            keys = new ArrayList<String>();
            keys.add("");
            request.setKeys(keys);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            keys = new ArrayList<String>();
            keys.add("//aaaa");
            request.setKeys(keys);
            assertTrue(true);
        } catch (Exception e) {
            assertTrue(false);
        }
    }

    @Test
    public void testDownloadFileRequest() {
        DownloadFileRequest request = new DownloadFileRequest("bucket", "key", "filePath", 0);
        request.setTaskNum(1);
        request = new DownloadFileRequest("bucket", "key", "filePath", 0, 1, false);
        request.setTaskNum(1);
        request = new DownloadFileRequest("bucket", "key", "filePath", 0, 1, true, "checkfile");

        List<String> eTagList = new ArrayList<String>();
        eTagList.add("item1");
        request.setMatchingETagConstraints(eTagList);
        assertEquals(eTagList, request.getMatchingETagConstraints());
        request.clearMatchingETagConstraints();
        assertEquals(0, request.getMatchingETagConstraints().size());

        request.setMatchingETagConstraints(eTagList);
        assertEquals(eTagList, request.getMatchingETagConstraints());
        request.setMatchingETagConstraints(null);
        assertEquals(0, request.getMatchingETagConstraints().size());

        request.setMatchingETagConstraints(eTagList);
        assertEquals(eTagList, request.getMatchingETagConstraints());
        eTagList.clear();
        request.setMatchingETagConstraints(eTagList);
        assertEquals(0, request.getMatchingETagConstraints().size());

        eTagList.clear();
        eTagList.add("item1");
        request.setNonmatchingETagConstraints(eTagList);
        assertEquals(eTagList, request.getNonmatchingETagConstraints());
        request.clearNonmatchingETagConstraints();
        assertEquals(0, request.getNonmatchingETagConstraints().size());

        request.setNonmatchingETagConstraints(eTagList);
        assertEquals(eTagList, request.getNonmatchingETagConstraints());
        request.setNonmatchingETagConstraints(null);
        assertEquals(0, request.getNonmatchingETagConstraints().size());

        request.setNonmatchingETagConstraints(eTagList);
        assertEquals(eTagList, request.getNonmatchingETagConstraints());
        eTagList.clear();
        request.setNonmatchingETagConstraints(eTagList);
        assertEquals(0, request.getNonmatchingETagConstraints().size());

        request.setUnmodifiedSinceConstraint(null);
        request.setModifiedSinceConstraint(null);
        request.setResponseHeaders(null);
    }

    @Test
    public void testGetObjectRequest() {
        GetObjectRequest request = new GetObjectRequest("bucket", "key").withRange(10, 20);
        assertEquals(10, request.getRange()[0]);
        assertEquals(20, request.getRange()[1]);

        request.setUseUrlSignature(true);
        assertEquals(true, request.isUseUrlSignature());

        List<String> eTagList = new ArrayList<String>();
        eTagList.add("tag1");
        request.setNonmatchingETagConstraints(eTagList);
        assertEquals(eTagList, request.getNonmatchingETagConstraints());
        request.clearNonmatchingETagConstraints();
        assertEquals(0, request.getNonmatchingETagConstraints().size());

        eTagList = new ArrayList<String>();
        eTagList.add("tag2");
        request.setMatchingETagConstraints(eTagList);
        assertEquals(eTagList, request.getMatchingETagConstraints());
        request.clearMatchingETagConstraints();
        assertEquals(0, request.getMatchingETagConstraints().size());

        request = new GetObjectRequest((URL) null, null);
        assertEquals(null, request.getAbsoluteUri());

        Map<String, String> requestHeaders = new HashMap<String, String>();
        request = new GetObjectRequest((URL) null, requestHeaders);
        assertEquals(null, request.getAbsoluteUri());
    }

    @Test
    public void testHeadObjectRequest() {
        HeadObjectRequest request = new HeadObjectRequest("bucket", "key");
        request.setBucketName("new-bucket");
        request.setKey("new-key");
        request.setVersionId("new-id");
        assertEquals("new-bucket", request.getBucketName());
        assertEquals("new-key", request.getKey());
        assertEquals("new-id", request.getVersionId());

        List<String> eTagList = new ArrayList<String>();
        request.setMatchingETagConstraints(null);
        request.setMatchingETagConstraints(eTagList);
        request.setNonmatchingETagConstraints(null);
        request.setNonmatchingETagConstraints(eTagList);
    }

    @Test
    public void testObjectMetadata() {
        ObjectMetadata meta = new ObjectMetadata();
        Map<String, String> userMetadata = new HashMap<String, String>();
        Date date = new Date();

        meta.setUserMetadata(null);
        meta.setUserMetadata(userMetadata);
        meta.setLastModified(date);

        assertEquals(date, meta.getLastModified());
        assertNull(meta.getContentMD5());
        assertNull(meta.getContentDisposition());
        assertNull(meta.getServerSideEncryptionKeyId());

        try {
            meta.getExpirationTime();
        } catch (Exception e) {
        }
        meta.setExpirationTime(date);

        try {
            meta.getServerCRC();
        } catch (Exception e) {
        }

        try {
            meta.getObjectStorageClass();
        } catch (Exception e) {
        }

        try {
            meta.isRestoreCompleted();
        } catch (Exception e) {
        }
        meta.setHeader(OSSHeaders.OSS_RESTORE, "");
        assertEquals(true, meta.isRestoreCompleted());

        meta.setHeader(OSSHeaders.OSS_RESTORE, OSSHeaders.OSS_ONGOING_RESTORE);
        assertEquals(false, meta.isRestoreCompleted());

        meta.setContentDisposition("disposition");
        assertEquals("disposition", meta.getContentDisposition());

        Map<String, String> tags = new HashMap<String, String>();
        meta.setObjectTagging(null);
        assertEquals(null, meta.getRawMetadata().get(OSSHeaders.OSS_TAGGING));

        meta.setObjectTagging(tags);
        assertEquals(null, meta.getRawMetadata().get(OSSHeaders.OSS_TAGGING));

        try {
            tags.clear();
            tags.put(null, "value");
            meta.setObjectTagging(tags);
        } catch (Exception e) {
        }

        try {
            tags.clear();
            tags.put("key", null);
            meta.setObjectTagging(tags);
        } catch (Exception e) {
        }

        try {
            tags.clear();
            tags.put("", "");
            meta.setObjectTagging(tags);
        } catch (Exception e) {
        }

        try {
            tags.clear();
            tags.put("key", "");
            meta.setObjectTagging(tags);
        } catch (Exception e) {
        }
    }

    @Test
    public void testListObjectsRequest() {
        ListObjectsRequest request = new ListObjectsRequest().withDelimiter("#").withEncodingType("url").withMarker("marker")
                .withPrefix("prefix").withMaxKeys(30);
        request.setBucketName("bucket");
        assertEquals("#", request.getDelimiter());
        assertEquals("prefix", request.getPrefix());

        ObjectListing list = new ObjectListing();
        List<OSSObjectSummary> objectSummaries = new ArrayList<OSSObjectSummary>();
        List<String> commonPrefixes = new ArrayList<String>();
        list.setObjectSummaries(null);
        list.setObjectSummaries(objectSummaries);
        objectSummaries.add(new OSSObjectSummary());
        list.setObjectSummaries(objectSummaries);

        list.setCommonPrefixes(null);
        list.setCommonPrefixes(commonPrefixes);
        commonPrefixes.add("prefix");
        list.setCommonPrefixes(commonPrefixes);
    }

    @Test
    public void testPayer() {
        Payer payer = Payer.parse("BucketOwner");
        assertEquals(Payer.BucketOwner, payer);
        try {
            payer = Payer.parse("UN");
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testDeleteVersionRequest() {
        DeleteVersionRequest reqeust = new DeleteVersionRequest("bucket", "key", "id");
        assertEquals("id", reqeust.getVersionId());

        reqeust.setVersionId("new id");
        assertEquals("new id", reqeust.getVersionId());

        reqeust = new DeleteVersionRequest("bucket", "key", "id").withVersionId("new id1");
        assertEquals("new id1", reqeust.getVersionId());
    }

    @Test
    public void testDeleteVersionsRequest() {
        DeleteVersionsRequest reqeust = new DeleteVersionsRequest("bucket").withQuiet(true);
        assertEquals(true, reqeust.getQuiet());

        reqeust.setQuiet(false);
        assertEquals(false, reqeust.getQuiet());

        reqeust = reqeust.withBucketName("new-bucket");
        assertEquals("new-bucket", reqeust.getBucketName());

        DeleteVersionsRequest.KeyVersion keyVersion = new DeleteVersionsRequest.KeyVersion("key");
        assertEquals(null, keyVersion.getVersion());
        assertEquals("key", keyVersion.getKey());
    }

    @Test
    public void testDeleteVersionsResult() {
        DeleteVersionsResult.DeletedVersion version = new DeleteVersionsResult.DeletedVersion();
        version.setDeleteMarker(false);
        version.setDeleteMarkerVersionId("markerId");
        version.setKey("key");
        version.setVersionId("versionid");

        assertEquals(false, version.isDeleteMarker());
        assertEquals("markerId", version.getDeleteMarkerVersionId());
        assertEquals("key", version.getKey());
        assertEquals("versionid", version.getVersionId());

        DeleteVersionsResult result = new DeleteVersionsResult(null);

        List<DeleteVersionsResult.DeletedVersion> deletedVersions = Arrays.asList();
        result = new DeleteVersionsResult(deletedVersions);
    }

    @Test
    public void testOSSVersionSummary() {
        OSSVersionSummary summary = new OSSVersionSummary();
        summary.setBucketName("bucket");
        summary.setETag("etag");
        summary.setIsLatest(true);
        summary.setIsDeleteMarker(true);
        summary.setSize(100);
        summary.setStorageClass("IA");

        assertEquals("bucket", summary.getBucketName());
        assertEquals("etag", summary.getETag());
        assertEquals(true, summary.isLatest());
        assertEquals(true, summary.isDeleteMarker());
        assertEquals(100, summary.getSize());
        assertEquals("IA", summary.getStorageClass());
    }

    @Test
    public void testOptionsRequest() {
        OptionsRequest request = new OptionsRequest().withOrigin("origin").withRequestMethod(HttpMethod.DELETE).withRequestHeaders("header");
        assertEquals("origin", request.getOrigin());
        assertEquals(HttpMethod.DELETE, request.getRequestMethod());
        assertEquals("header", request.getRequestHeaders());
    }

    @Test
    public void testCSVFormat() {
        CSVFormat format = new CSVFormat();
        format.setAllowQuotedRecordDelimiter(false);
        assertEquals(false, format.isAllowQuotedRecordDelimiter());

        format = new CSVFormat().withAllowQuotedRecordDelimiter(false);
        assertEquals(false, format.isAllowQuotedRecordDelimiter());

        format.setCommentChar(null);
        assertEquals(null, format.getCommentChar());
        format.setCommentChar("");
        assertEquals(null, format.getCommentChar());

        format.setFieldDelimiter(null);
        assertEquals(null, format.getFieldDelimiter());
        format.setFieldDelimiter("");
        assertEquals(null, format.getFieldDelimiter());

        format.setQuoteChar(null);
        assertEquals(null, format.getQuoteChar());
        format.setQuoteChar("");
        assertEquals(null, format.getQuoteChar());
    }

    @Test
    public void testCreateSelectObjectMetadataRequest() {
        CreateSelectObjectMetadataRequest request =
                new CreateSelectObjectMetadataRequest("bucket", "key").withProcess("process");
        assertEquals("process", request.getProcess());

        SelectObjectMetadata meta = new SelectObjectMetadata();
        meta.setContentType("type");
        assertEquals("type", meta.getContentType());
    }

    @Test
    public void testJsonFormat() {
        JsonFormat format = new JsonFormat();
        format.setRecordDelimiter("#");
        assertEquals("#", format.getRecordDelimiter());
    }

    @Test
    public void testSelectObjectException() {
        SelectObjectException e = new SelectObjectException("error", "message", "id");
        assertEquals("error", e.getErrorCode());
        assertEquals("id", e.getRequestId());
        assertFalse(e.toString().isEmpty());
    }

    @Test
    public void testSelectObjectRequest() {
        SelectObjectRequest request = new SelectObjectRequest("bucket", "key").withMaxSkippedRecordsAllowed(0);
        long[] range = new long[2];

        range[0] = 10;
        range[1] = 20;
        String value = request.splitRangeToString(range);
        System.out.println(value);
        assertEquals("split-range=10-20", value);

        range[0] = -1;
        range[1] = 10;
        value = request.splitRangeToString(range);
        System.out.println(value);
        assertEquals("split-range=-10", value);

        range[0] = 10;
        range[1] = -1;
        value = request.splitRangeToString(range);
        System.out.println(value);
        assertEquals("split-range=10-", value);
    }

    @Test
    public void testGeneratePresignedUrlRequest() {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest("bucket", "key");
        Map<String, String> userMetadata = new HashMap<String, String>();
        Map<String, String> queryParam = new HashMap<String, String>();
        Map<String, String> headers = new HashMap<String, String>();

        try {
            request.setMethod(HttpMethod.DELETE);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        request.setKey("new-key");
        request.setBucketName("new-bucket");
        request.setContentMD5("md5");
        request.setUserMetadata(userMetadata);
        request.setQueryParameter(queryParam);
        request.setHeaders(headers);
        request.setAdditionalHeaderNames(null);

        assertEquals("new-key", request.getKey());
        assertEquals("new-bucket", request.getBucketName());
        assertEquals("md5", request.getContentMD5());
        assertEquals(userMetadata, request.getUserMetadata());
        assertEquals(queryParam, request.getQueryParameter());
        assertEquals(headers, request.getHeaders());
        assertEquals(null, request.getAdditionalHeaderNames());

        try {
            request.setUserMetadata(null);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            request.setQueryParameter(null);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }

        try {
            request.setHeaders(null);
            assertTrue(false);
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testCreateSymlinkRequest() {
        CreateSymlinkRequest request = new CreateSymlinkRequest("bucket", "symlink", "target");
        assertEquals("symlink", request.getSymlink());
        assertEquals("target", request.getTarget());

        request.setSymlink("new symlink");
        request.setTarget("new target");
        assertEquals("new symlink", request.getSymlink());
        assertEquals("new target", request.getTarget());

        OSSSymlink symlink = new OSSSymlink("symlink", "target");
        symlink.setTarget("new target");
        assertEquals("new target", symlink.getTarget());
        assertFalse(symlink.toString().isEmpty());
    }

    @Test
    public void testDeleteObjectsResult() {
        List<String> deletedObjects = new ArrayList<String>();
        DeleteObjectsResult resutl = new DeleteObjectsResult(null);
        assertEquals(null, resutl.getEncodingType());

        resutl = new DeleteObjectsResult(deletedObjects);
        assertEquals(null, resutl.getEncodingType());

        deletedObjects.add("key");
        resutl = new DeleteObjectsResult(deletedObjects);
        assertEquals(1, resutl.getDeletedObjects().size());
    }

    @Test
    public void testOSSObject() {
        OSSObject object = new OSSObject();
        object.setKey("");
        assertEquals(true, object.toString().contains("Unknown"));

        object.setBucketName("test-bucket");
        assertEquals(true, object.toString().contains("test-bucket"));

        try {
            object.close();
        } catch (Exception e) {}
    }

    @Test
    public void testResponseHeaderOverrides() {
        ResponseHeaderOverrides overides = new ResponseHeaderOverrides();
        overides.setContentType("content");
        assertEquals("content", overides.getContentType());

        overides.setContentLangauge("lang");
        assertEquals("lang", overides.getContentLangauge());

        overides.setExpires("expire");
        assertEquals("expire", overides.getExpires());

        overides.setContentDisposition("contentDisposition");
        assertEquals("contentDisposition", overides.getContentDisposition());

        overides.setContentEncoding("contentEncoding");
        assertEquals("contentEncoding", overides.getContentEncoding());
    }

    @Test
    public void testVersionListing() {
        VersionListing list = new VersionListing();
        assertEquals(0, list.getVersionSummaries().size());

        list.setVersionSummaries(null);
        assertEquals(null, list.getVersionSummaries());

        list.setKeyMarker("marker");
        assertEquals("marker", list.getKeyMarker());

        list.setVersionIdMarker("idmarker");
        assertEquals("idmarker", list.getVersionIdMarker());
    }
}
