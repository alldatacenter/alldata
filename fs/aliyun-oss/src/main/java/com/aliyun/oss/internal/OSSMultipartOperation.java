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

package com.aliyun.oss.internal;

import static com.aliyun.oss.internal.RequestParameters.*;
import static com.aliyun.oss.common.parser.RequestMarshallers.completeMultipartUploadRequestMarshaller;
import static com.aliyun.oss.common.utils.CodingUtils.assertParameterNotNull;
import static com.aliyun.oss.common.utils.CodingUtils.assertStringNotNullOrEmpty;
import static com.aliyun.oss.common.utils.CodingUtils.checkParamRange;
import static com.aliyun.oss.common.utils.IOUtils.newRepeatableInputStream;
import static com.aliyun.oss.common.utils.LogUtils.logException;
import static com.aliyun.oss.event.ProgressPublisher.publishProgress;
import static com.aliyun.oss.internal.OSSUtils.OSS_RESOURCE_MANAGER;
import static com.aliyun.oss.internal.OSSUtils.addDateHeader;
import static com.aliyun.oss.internal.OSSUtils.addStringListHeader;
import static com.aliyun.oss.internal.OSSUtils.ensureCallbackValid;
import static com.aliyun.oss.internal.OSSUtils.populateRequestCallback;
import static com.aliyun.oss.internal.OSSUtils.removeHeader;
import static com.aliyun.oss.internal.OSSUtils.ensureBucketNameValid;
import static com.aliyun.oss.internal.OSSUtils.ensureObjectKeyValid;
import static com.aliyun.oss.internal.OSSUtils.populateRequestMetadata;
import static com.aliyun.oss.internal.OSSUtils.trimQuotes;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_FILE_SIZE_LIMIT;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_CHARSET_NAME;
import static com.aliyun.oss.internal.ResponseParsers.completeMultipartUploadResponseParser;
import static com.aliyun.oss.internal.ResponseParsers.completeMultipartUploadProcessResponseParser;
import static com.aliyun.oss.internal.ResponseParsers.initiateMultipartUploadResponseParser;
import static com.aliyun.oss.internal.ResponseParsers.listMultipartUploadsResponseParser;
import static com.aliyun.oss.internal.ResponseParsers.listPartsResponseParser;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSSException;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.comm.ResponseHandler;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.common.comm.ServiceClient;
import com.aliyun.oss.common.comm.io.FixedLengthInputStream;
import com.aliyun.oss.common.utils.CRC64;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.event.ProgressEventType;
import com.aliyun.oss.event.ProgressListener;
import com.aliyun.oss.internal.ResponseParsers.UploadPartCopyResponseParser;
import com.aliyun.oss.model.AbortMultipartUploadRequest;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.ListMultipartUploadsRequest;
import com.aliyun.oss.model.ListPartsRequest;
import com.aliyun.oss.model.MultipartUploadListing;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.PartListing;
import com.aliyun.oss.model.UploadPartCopyRequest;
import com.aliyun.oss.model.UploadPartCopyResult;
import com.aliyun.oss.model.UploadPartRequest;
import com.aliyun.oss.model.UploadPartResult;
import com.aliyun.oss.model.Payer;
import com.aliyun.oss.model.VoidResult;
import com.aliyun.oss.model.WebServiceRequest;

/**
 * Multipart operation.
 */
public class OSSMultipartOperation extends OSSOperation {

    private static final int LIST_PART_MAX_RETURNS = 1000;
    private static final int LIST_UPLOAD_MAX_RETURNS = 1000;
    private static final int MAX_PART_NUMBER = 10000;

    public OSSMultipartOperation(ServiceClient client, CredentialsProvider credsProvider) {
        super(client, credsProvider);
    }

    @Override
    protected boolean isRetryablePostRequest(WebServiceRequest request) {
        if (request instanceof InitiateMultipartUploadRequest) {
            return true;
        }
        return super.isRetryablePostRequest(request);
    }

    /**
     * Abort multipart upload.
     */
    public VoidResult abortMultipartUpload(AbortMultipartUploadRequest abortMultipartUploadRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(abortMultipartUploadRequest, "abortMultipartUploadRequest");

        String key = abortMultipartUploadRequest.getKey();
        String bucketName = abortMultipartUploadRequest.getBucketName();
        String uploadId = abortMultipartUploadRequest.getUploadId();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(key, "key");
        ensureObjectKeyValid(key);
        assertStringNotNullOrEmpty(uploadId, "uploadId");

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UPLOAD_ID, uploadId);

        Map<String, String> headers = new HashMap<String, String>();
        populateRequestPayerHeader(headers, abortMultipartUploadRequest.getRequestPayer());

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(abortMultipartUploadRequest))
                .setMethod(HttpMethod.DELETE).setBucket(bucketName).setKey(key).setHeaders(headers).setParameters(parameters)
                .setOriginalRequest(abortMultipartUploadRequest).build();

        return doOperation(request, requestIdResponseParser, bucketName, key);
    }

    /**
     * Complete multipart upload.
     */
    public CompleteMultipartUploadResult completeMultipartUpload(
            CompleteMultipartUploadRequest completeMultipartUploadRequest) throws OSSException, ClientException {

        assertParameterNotNull(completeMultipartUploadRequest, "completeMultipartUploadRequest");

        String key = completeMultipartUploadRequest.getKey();
        String bucketName = completeMultipartUploadRequest.getBucketName();
        String uploadId = completeMultipartUploadRequest.getUploadId();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(key, "key");
        ensureObjectKeyValid(key);
        assertStringNotNullOrEmpty(uploadId, "uploadId");
        ensureCallbackValid(completeMultipartUploadRequest.getCallback());

        Map<String, String> headers = new HashMap<String, String>();
        populateCompleteMultipartUploadOptionalHeaders(completeMultipartUploadRequest, headers);
        populateRequestCallback(headers, completeMultipartUploadRequest.getCallback());

        populateRequestPayerHeader(headers, completeMultipartUploadRequest.getRequestPayer());

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put(UPLOAD_ID, uploadId);

        List<PartETag> partETags = completeMultipartUploadRequest.getPartETags();
        FixedLengthInputStream requestInstream;
        if (partETags != null) {
            Collections.sort(partETags, new Comparator<PartETag>() {
                @Override
                public int compare(PartETag p1, PartETag p2) {
                    return p1.getPartNumber() - p2.getPartNumber();
                }
            });
            requestInstream = completeMultipartUploadRequestMarshaller.marshall(completeMultipartUploadRequest);
        } else {
            requestInstream = new FixedLengthInputStream(new ByteArrayInputStream("".getBytes()), 0);
        }

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(completeMultipartUploadRequest))
                .setMethod(HttpMethod.POST).setBucket(bucketName).setKey(key).setHeaders(headers)
                .setParameters(parameters)
                .setInputStreamWithLength(requestInstream)
                .setOriginalRequest(completeMultipartUploadRequest).build();

        List<ResponseHandler> reponseHandlers = new ArrayList<ResponseHandler>();
        reponseHandlers.add(new OSSCallbackErrorResponseHandler());

        CompleteMultipartUploadResult result = null;
        if (!isNeedReturnResponse(completeMultipartUploadRequest)) {
            result = doOperation(request, completeMultipartUploadResponseParser, bucketName, key, true);
        } else {
            result = doOperation(request, completeMultipartUploadProcessResponseParser, bucketName, key, true, null,
                    reponseHandlers);
        }

        if (partETags != null) {
            result.setClientCRC(calcObjectCRCFromParts(partETags));
        }
        if (getInnerClient().getClientConfiguration().isCrcCheckEnabled()) {
            OSSUtils.checkChecksum(result.getClientCRC(), result.getServerCRC(), result.getRequestId());
        }

        return result;
    }

    /**
     * Initiate multipart upload.
     */
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest initiateMultipartUploadRequest) throws OSSException, ClientException {

        assertParameterNotNull(initiateMultipartUploadRequest, "initiateMultipartUploadRequest");

        String key = initiateMultipartUploadRequest.getKey();
        String bucketName = initiateMultipartUploadRequest.getBucketName();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(key, "key");
        ensureObjectKeyValid(key);

        Map<String, String> headers = new HashMap<String, String>();
        if (initiateMultipartUploadRequest.getObjectMetadata() != null) {
            populateRequestMetadata(headers, initiateMultipartUploadRequest.getObjectMetadata());
        }

        populateRequestPayerHeader(headers, initiateMultipartUploadRequest.getRequestPayer());

        // Be careful that we don't send the object's total size as the content
        // length for the InitiateMultipartUpload request.
        removeHeader(headers, OSSHeaders.CONTENT_LENGTH);

        Map<String, String> params = new HashMap<String, String>();
        params.put(SUBRESOURCE_UPLOADS, null);

        Boolean sequentialMode = initiateMultipartUploadRequest.getSequentialMode();
        if (sequentialMode != null && sequentialMode.equals(true)) {
            params.put(SEQUENTIAL, null);
        }

        // Set the request content to be empty (but not null) to avoid putting
        // parameters
        // to request body. Set HttpRequestFactory#createHttpRequest for
        // details.
        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(initiateMultipartUploadRequest))
                .setMethod(HttpMethod.POST).setBucket(bucketName).setKey(key).setHeaders(headers).setParameters(params)
                .setInputStream(new ByteArrayInputStream(new byte[0])).setInputSize(0)
                .setOriginalRequest(initiateMultipartUploadRequest).build();

        return doOperation(request, initiateMultipartUploadResponseParser, bucketName, key, true);
    }

    /**
     * List multipart uploads.
     */
    public MultipartUploadListing listMultipartUploads(ListMultipartUploadsRequest listMultipartUploadsRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(listMultipartUploadsRequest, "listMultipartUploadsRequest");

        String bucketName = listMultipartUploadsRequest.getBucketName();
        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);

        // Use a LinkedHashMap to preserve the insertion order.
        Map<String, String> params = new LinkedHashMap<String, String>();
        populateListMultipartUploadsRequestParameters(listMultipartUploadsRequest, params);

        Map<String, String> headers = new HashMap<String, String>();
        populateRequestPayerHeader(headers, listMultipartUploadsRequest.getRequestPayer());

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(listMultipartUploadsRequest))
                .setMethod(HttpMethod.GET).setBucket(bucketName).setHeaders(headers).setParameters(params)
                .setOriginalRequest(listMultipartUploadsRequest).build();

        return doOperation(request, listMultipartUploadsResponseParser, bucketName, null, true);
    }

    /**
     * List parts.
     */
    public PartListing listParts(ListPartsRequest listPartsRequest) throws OSSException, ClientException {

        assertParameterNotNull(listPartsRequest, "listPartsRequest");

        String key = listPartsRequest.getKey();
        String bucketName = listPartsRequest.getBucketName();
        String uploadId = listPartsRequest.getUploadId();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(key, "key");
        ensureObjectKeyValid(key);
        assertStringNotNullOrEmpty(uploadId, "uploadId");

        // Use a LinkedHashMap to preserve the insertion order.
        Map<String, String> params = new LinkedHashMap<String, String>();
        populateListPartsRequestParameters(listPartsRequest, params);

        Map<String, String> headers = new HashMap<String, String>();
        populateRequestPayerHeader(headers, listPartsRequest.getRequestPayer());

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(listPartsRequest))
                .setMethod(HttpMethod.GET).setBucket(bucketName).setKey(key).setHeaders(headers).setParameters(params)
                .setOriginalRequest(listPartsRequest).build();

        return doOperation(request, listPartsResponseParser, bucketName, key, true);
    }

    /**
     * Upload part.
     */
    public UploadPartResult uploadPart(UploadPartRequest uploadPartRequest) throws OSSException, ClientException {

        assertParameterNotNull(uploadPartRequest, "uploadPartRequest");

        String key = uploadPartRequest.getKey();
        String bucketName = uploadPartRequest.getBucketName();
        String uploadId = uploadPartRequest.getUploadId();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(key, "key");
        ensureObjectKeyValid(key);
        assertStringNotNullOrEmpty(uploadId, "uploadId");

        if (uploadPartRequest.getInputStream() == null) {
            throw new IllegalArgumentException(OSS_RESOURCE_MANAGER.getString("MustSetContentStream"));
        }

        InputStream repeatableInputStream = null;
        try {
            repeatableInputStream = newRepeatableInputStream(uploadPartRequest.buildPartialStream());
        } catch (IOException ex) {
            logException("Cannot wrap to repeatable input stream: ", ex);
            throw new ClientException("Cannot wrap to repeatable input stream: ", ex);
        }

        int partNumber = uploadPartRequest.getPartNumber();
        if (!checkParamRange(partNumber, 0, false, MAX_PART_NUMBER, true)) {
            throw new IllegalArgumentException(OSS_RESOURCE_MANAGER.getString("PartNumberOutOfRange"));
        }

        Map<String, String> headers = new HashMap<String, String>();
        populateUploadPartOptionalHeaders(uploadPartRequest, headers);

        populateRequestPayerHeader(headers, uploadPartRequest.getRequestPayer());
        populateTrafficLimitHeader(headers, uploadPartRequest.getTrafficLimit());

        // Use a LinkedHashMap to preserve the insertion order.
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put(PART_NUMBER, Integer.toString(partNumber));
        params.put(UPLOAD_ID, uploadId);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(uploadPartRequest))
                .setMethod(HttpMethod.PUT).setBucket(bucketName).setKey(key).setParameters(params).setHeaders(headers)
                .setInputStream(repeatableInputStream).setInputSize(uploadPartRequest.getPartSize())
                .setUseChunkEncoding(uploadPartRequest.isUseChunkEncoding()).setOriginalRequest(uploadPartRequest)
                .build();

        final ProgressListener listener = uploadPartRequest.getProgressListener();
        ResponseMessage response = null;
        try {
            publishProgress(listener, ProgressEventType.TRANSFER_PART_STARTED_EVENT);
            response = doOperation(request, emptyResponseParser, bucketName, key);
            publishProgress(listener, ProgressEventType.TRANSFER_PART_COMPLETED_EVENT);
        } catch (RuntimeException e) {
            publishProgress(listener, ProgressEventType.TRANSFER_PART_FAILED_EVENT);
            throw e;
        }

        UploadPartResult result = new UploadPartResult();
        result.setPartNumber(partNumber);
        result.setETag(trimQuotes(response.getHeaders().get(OSSHeaders.ETAG)));
        result.setRequestId(response.getRequestId());
        result.setPartSize(uploadPartRequest.getPartSize());
        result.setResponse(response);
        ResponseParsers.setCRC(result, response);

        if (getInnerClient().getClientConfiguration().isCrcCheckEnabled()) {
            OSSUtils.checkChecksum(result.getClientCRC(), result.getServerCRC(), result.getRequestId());
        }

        return result;
    }

    /**
     * Upload part copy.
     */
    public UploadPartCopyResult uploadPartCopy(UploadPartCopyRequest uploadPartCopyRequest)
            throws OSSException, ClientException {

        assertParameterNotNull(uploadPartCopyRequest, "uploadPartCopyRequest");

        String key = uploadPartCopyRequest.getKey();
        String bucketName = uploadPartCopyRequest.getBucketName();
        String uploadId = uploadPartCopyRequest.getUploadId();

        assertParameterNotNull(bucketName, "bucketName");
        ensureBucketNameValid(bucketName);
        assertParameterNotNull(key, "key");
        ensureObjectKeyValid(key);
        assertStringNotNullOrEmpty(uploadId, "uploadId");

        Long partSize = uploadPartCopyRequest.getPartSize();
        if (partSize != null) {
            if (!checkParamRange(partSize, 0, true, DEFAULT_FILE_SIZE_LIMIT, true)) {
                throw new IllegalArgumentException(OSS_RESOURCE_MANAGER.getString("FileSizeOutOfRange"));
            }
        }

        int partNumber = uploadPartCopyRequest.getPartNumber();
        if (!checkParamRange(partNumber, 0, false, MAX_PART_NUMBER, true)) {
            throw new IllegalArgumentException(OSS_RESOURCE_MANAGER.getString("PartNumberOutOfRange"));
        }

        Map<String, String> headers = new HashMap<String, String>();
        populateCopyPartRequestHeaders(uploadPartCopyRequest, headers);

        // Use a LinkedHashMap to preserve the insertion order.
        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put(PART_NUMBER, Integer.toString(partNumber));
        params.put(UPLOAD_ID, uploadId);

        RequestMessage request = new OSSRequestMessageBuilder(getInnerClient()).setEndpoint(getEndpoint(uploadPartCopyRequest))
                .setMethod(HttpMethod.PUT).setBucket(bucketName).setKey(key).setParameters(params).setHeaders(headers)
                .setOriginalRequest(uploadPartCopyRequest).build();

        return doOperation(request, new UploadPartCopyResponseParser(partNumber), bucketName, key, true);
    }

    private static void populateListMultipartUploadsRequestParameters(
            ListMultipartUploadsRequest listMultipartUploadsRequest, Map<String, String> params) {

        // Make sure 'uploads' be the first parameter.
        params.put(SUBRESOURCE_UPLOADS, null);

        if (listMultipartUploadsRequest.getDelimiter() != null) {
            params.put(DELIMITER, listMultipartUploadsRequest.getDelimiter());
        }

        if (listMultipartUploadsRequest.getKeyMarker() != null) {
            params.put(KEY_MARKER, listMultipartUploadsRequest.getKeyMarker());
        }

        Integer maxUploads = listMultipartUploadsRequest.getMaxUploads();
        if (maxUploads != null) {
            if (!checkParamRange(maxUploads, 0, true, LIST_UPLOAD_MAX_RETURNS, true)) {
                throw new IllegalArgumentException(
                        OSS_RESOURCE_MANAGER.getFormattedString("MaxUploadsOutOfRange", LIST_UPLOAD_MAX_RETURNS));
            }
            params.put(MAX_UPLOADS, listMultipartUploadsRequest.getMaxUploads().toString());
        }

        if (listMultipartUploadsRequest.getPrefix() != null) {
            params.put(PREFIX, listMultipartUploadsRequest.getPrefix());
        }

        if (listMultipartUploadsRequest.getUploadIdMarker() != null) {
            params.put(UPLOAD_ID_MARKER, listMultipartUploadsRequest.getUploadIdMarker());
        }

        if (listMultipartUploadsRequest.getEncodingType() != null) {
            params.put(ENCODING_TYPE, listMultipartUploadsRequest.getEncodingType());
        }
    }

    private static void populateListPartsRequestParameters(ListPartsRequest listPartsRequest,
            Map<String, String> params) {

        params.put(UPLOAD_ID, listPartsRequest.getUploadId());

        Integer maxParts = listPartsRequest.getMaxParts();
        if (maxParts != null) {
            if (!checkParamRange(maxParts, 0, true, LIST_PART_MAX_RETURNS, true)) {
                throw new IllegalArgumentException(
                        OSS_RESOURCE_MANAGER.getFormattedString("MaxPartsOutOfRange", LIST_PART_MAX_RETURNS));
            }
            params.put(MAX_PARTS, maxParts.toString());
        }

        Integer partNumberMarker = listPartsRequest.getPartNumberMarker();
        if (partNumberMarker != null) {
            if (!checkParamRange(partNumberMarker, 0, false, MAX_PART_NUMBER, true)) {
                throw new IllegalArgumentException(OSS_RESOURCE_MANAGER.getString("PartNumberMarkerOutOfRange"));
            }
            params.put(PART_NUMBER_MARKER, partNumberMarker.toString());
        }

        if (listPartsRequest.getEncodingType() != null) {
            params.put(ENCODING_TYPE, listPartsRequest.getEncodingType());
        }
    }

    private static void populateCopyPartRequestHeaders(UploadPartCopyRequest uploadPartCopyRequest,
            Map<String, String> headers) {

        if (uploadPartCopyRequest.getPartSize() != null) {
            headers.put(OSSHeaders.CONTENT_LENGTH, Long.toString(uploadPartCopyRequest.getPartSize()));
        }

        if (uploadPartCopyRequest.getMd5Digest() != null) {
            headers.put(OSSHeaders.CONTENT_MD5, uploadPartCopyRequest.getMd5Digest());
        }

        String copySource = "/" + uploadPartCopyRequest.getSourceBucketName() + "/"
                + HttpUtil.urlEncode(uploadPartCopyRequest.getSourceKey(), DEFAULT_CHARSET_NAME);
        if (uploadPartCopyRequest.getSourceVersionId() != null) {
            copySource += "?versionId=" + uploadPartCopyRequest.getSourceVersionId();
        }
        headers.put(OSSHeaders.COPY_OBJECT_SOURCE, copySource);

        if (uploadPartCopyRequest.getBeginIndex() != null && uploadPartCopyRequest.getPartSize() != null) {
            String range = "bytes=" + uploadPartCopyRequest.getBeginIndex() + "-"
                    + Long.toString(uploadPartCopyRequest.getBeginIndex() + uploadPartCopyRequest.getPartSize() - 1);
            headers.put(OSSHeaders.COPY_SOURCE_RANGE, range);
        }

        if (uploadPartCopyRequest.getRequestPayer() != null) {
            headers.put(OSSHeaders.OSS_REQUEST_PAYER, uploadPartCopyRequest.getRequestPayer().toString().toLowerCase());
        }

        addDateHeader(headers, OSSHeaders.COPY_OBJECT_SOURCE_IF_MODIFIED_SINCE,
                uploadPartCopyRequest.getModifiedSinceConstraint());
        addDateHeader(headers, OSSHeaders.COPY_OBJECT_SOURCE_IF_UNMODIFIED_SINCE,
                uploadPartCopyRequest.getUnmodifiedSinceConstraint());

        addStringListHeader(headers, OSSHeaders.COPY_OBJECT_SOURCE_IF_MATCH,
                uploadPartCopyRequest.getMatchingETagConstraints());
        addStringListHeader(headers, OSSHeaders.COPY_OBJECT_SOURCE_IF_NONE_MATCH,
                uploadPartCopyRequest.getNonmatchingEtagConstraints());
    }

    private static void populateUploadPartOptionalHeaders(UploadPartRequest uploadPartRequest,
            Map<String, String> headers) {

        if (!uploadPartRequest.isUseChunkEncoding()) {
            long partSize = uploadPartRequest.getPartSize();
            if (!checkParamRange(partSize, 0, true, DEFAULT_FILE_SIZE_LIMIT, true)) {
                throw new IllegalArgumentException(OSS_RESOURCE_MANAGER.getString("FileSizeOutOfRange"));
            }

            headers.put(OSSHeaders.CONTENT_LENGTH, Long.toString(partSize));
        }

        if (uploadPartRequest.getMd5Digest() != null) {
            headers.put(OSSHeaders.CONTENT_MD5, uploadPartRequest.getMd5Digest());
        }
    }

    private static void populateCompleteMultipartUploadOptionalHeaders(
            CompleteMultipartUploadRequest completeMultipartUploadRequest, Map<String, String> headers) {

        CannedAccessControlList cannedACL = completeMultipartUploadRequest.getObjectACL();
        if (cannedACL != null) {
            headers.put(OSSHeaders.OSS_OBJECT_ACL, cannedACL.toString());
        }
    }

    private static void populateRequestPayerHeader (Map<String, String> headers, Payer payer) {
        if (payer != null && payer.equals(Payer.Requester)) {
            headers.put(OSSHeaders.OSS_REQUEST_PAYER, payer.toString().toLowerCase());
        }
    }

    private static void populateTrafficLimitHeader(Map<String, String> headers, int limit) {
        if (limit > 0) {
            headers.put(OSSHeaders.OSS_HEADER_TRAFFIC_LIMIT, String.valueOf(limit));
        }
    }

    private static Long calcObjectCRCFromParts(List<PartETag> partETags) {
        long crc = 0;
        for (PartETag partETag : partETags) {
            if (partETag.getPartCRC() == null || partETag.getPartSize() <= 0) {
                return null;
            }
            crc = CRC64.combine(crc, partETag.getPartCRC(), partETag.getPartSize());
        }
        return new Long(crc);
    }

    private static boolean isNeedReturnResponse(CompleteMultipartUploadRequest completeMultipartUploadRequest) {
        if (completeMultipartUploadRequest.getCallback() != null
                || completeMultipartUploadRequest.getProcess() != null) {
            return true;
        }
        return false;
    }
}
