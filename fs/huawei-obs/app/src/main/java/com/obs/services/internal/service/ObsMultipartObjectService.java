/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


package com.obs.services.internal.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.obs.services.internal.Constants;
import com.obs.services.internal.Constants.CommonHeaders;
import com.obs.services.internal.Constants.ObsRequestParams;
import com.obs.services.internal.RepeatableRequestEntity;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.handler.XmlResponsesSaxParser;
import com.obs.services.internal.io.HttpMethodReleaseInputStream;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.JSONChange;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AbortMultipartUploadRequest;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.CompleteMultipartUploadRequest;
import com.obs.services.model.CompleteMultipartUploadResult;
import com.obs.services.model.CopyPartRequest;
import com.obs.services.model.CopyPartResult;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.InitiateMultipartUploadResult;
import com.obs.services.model.ListMultipartUploadsRequest;
import com.obs.services.model.ListPartsRequest;
import com.obs.services.model.ListPartsResult;
import com.obs.services.model.MultipartUploadListing;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.UploadPartResult;

import okhttp3.Response;

public abstract class ObsMultipartObjectService extends ObsObjectBaseService {
    protected InitiateMultipartUploadResult initiateMultipartUploadImpl(InitiateMultipartUploadRequest request)
            throws ServiceException {

        TransResult result = this.transInitiateMultipartUploadRequest(request);

        this.prepareRESTHeaderAcl(request.getBucketName(), result.getHeaders(), request.getAcl());

        NewTransResult newTransResult = transObjectRequestWithResult(result, request);
        Response response = performRequest(newTransResult, true, false, false, false);

        this.verifyResponseContentType(response);

        InitiateMultipartUploadResult multipartUpload = getXmlResponseSaxParser()
                .parse(new HttpMethodReleaseInputStream(response),
                        XmlResponsesSaxParser.InitiateMultipartUploadHandler.class, true)
                .getInitiateMultipartUploadResult();
        setHeadersAndStatus(multipartUpload, response);
        return multipartUpload;
    }

    protected HeaderResponse abortMultipartUploadImpl(AbortMultipartUploadRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(ObsRequestParams.UPLOAD_ID, request.getUploadId());

        Response response = performRestDelete(request.getBucketName(), request.getObjectKey(), requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return build(response);
    }

    protected CompleteMultipartUploadResult completeMultipartUploadImpl(CompleteMultipartUploadRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(ObsRequestParams.UPLOAD_ID, request.getUploadId());
        if (request.getEncodingType() != null) {
            requestParams.put(ObsRequestParams.ENCODING_TYPE, request.getEncodingType());
        }

        Map<String, String> headers = new HashMap<>();

        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));
        String xml = this.getIConvertor(request.getBucketName()).transCompleteMultipartUpload(request.getPartEtag());
        headers.put(CommonHeaders.CONTENT_LENGTH, String.valueOf(xml.length()));
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        if (request.getCallback() != null) {
            headers.put((this.getProviderCredentials().getLocalAuthType(request.getBucketName()) != AuthTypeEnum.OBS
                            ? Constants.V2_HEADER_PREFIX : Constants.OBS_HEADER_PREFIX) + CommonHeaders.CALLBACK,
                    ServiceUtils.toBase64(JSONChange.objToJson(request.getCallback()).getBytes(StandardCharsets.UTF_8)));
        }

        NewTransResult transResult = transObjectRequest(request);
        transResult.setParams(requestParams);
        transResult.setHeaders(headers);
        transResult.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));

        Response response = performRequest(transResult, true, false, false, false);

        CompleteMultipartUploadResult ret;
        if (request.getCallback() == null) {
            this.verifyResponseContentType(response);
            XmlResponsesSaxParser.CompleteMultipartUploadHandler handler = getXmlResponseSaxParser().parse(
                    new HttpMethodReleaseInputStream(response), XmlResponsesSaxParser.CompleteMultipartUploadHandler.class,
                    true);

            String versionId = response.header(this.getIHeaders(request.getBucketName()).versionIdHeader());

            ret = new CompleteMultipartUploadResult(handler.getBucketName(),
                    handler.getObjectKey(), handler.getEtag(), handler.getLocation(), versionId,
                    this.getObjectUrl(handler.getBucketName(), handler.getObjectKey(), request.getIsIgnorePort()));
        } else {
            ret = new CompleteMultipartUploadResult(request.getBucketName(),
                    request.getObjectKey(), null, null, null, this.getObjectUrl(request.getBucketName(),
                    request.getObjectKey(), request.getIsIgnorePort()));
            try {
                ret.setCallbackResponseBody(Objects.requireNonNull(response.body()).byteStream());
            } catch (Exception e) {
                throw new ServiceException(e);
            }
        }
        setHeadersAndStatus(ret, response);

        return ret;
    }

    protected MultipartUploadListing listMultipartUploadsImpl(ListMultipartUploadsRequest request)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.UPLOADS.getOriginalStringCode(), "");
        if (request.getPrefix() != null) {
            requestParameters.put(ObsRequestParams.PREFIX, request.getPrefix());
        }
        if (request.getDelimiter() != null) {
            requestParameters.put(ObsRequestParams.DELIMITER, request.getDelimiter());
        }
        if (request.getMaxUploads() != null) {
            requestParameters.put(ObsRequestParams.MAX_UPLOADS, request.getMaxUploads().toString());
        }
        if (request.getKeyMarker() != null) {
            requestParameters.put(ObsRequestParams.KEY_MARKER, request.getKeyMarker());
        }
        if (request.getUploadIdMarker() != null) {
            requestParameters.put(ObsRequestParams.UPLOAD_ID_MARKER, request.getUploadIdMarker());
        }
        if (request.getEncodingType() != null) {
            requestParameters.put(ObsRequestParams.ENCODING_TYPE, request.getEncodingType());
        }

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        XmlResponsesSaxParser.ListMultipartUploadsHandler handler = getXmlResponseSaxParser().parse(
                new HttpMethodReleaseInputStream(httpResponse), XmlResponsesSaxParser.ListMultipartUploadsHandler.class,
                true);

        MultipartUploadListing listResult = new MultipartUploadListing.Builder()
                .bucketName(handler.getBucketName() == null
                        ? request.getBucketName() : handler.getBucketName())
                .keyMarker(handler.getKeyMarker() == null
                        ? request.getKeyMarker() : handler.getKeyMarker())
                .uploadIdMarker(handler.getUploadIdMarker() == null
                        ? request.getUploadIdMarker() : handler.getUploadIdMarker())
                .nextKeyMarker(handler.getNextKeyMarker())
                .nextUploadIdMarker(handler.getNextUploadIdMarker())
                .prefix(handler.getPrefix() == null
                        ? request.getPrefix() : handler.getPrefix())
                .maxUploads(handler.getMaxUploads())
                .truncated(handler.isTruncated())
                .multipartTaskList(handler.getMultipartUploadList())
                .delimiter(handler.getDelimiter() == null
                        ? request.getDelimiter() : handler.getDelimiter())
                .commonPrefixes(handler.getCommonPrefixes().toArray(
                        new String[handler.getCommonPrefixes().size()]))
                .builder();
        setHeadersAndStatus(listResult, httpResponse);
        return listResult;
    }

    protected ListPartsResult listPartsImpl(ListPartsRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(ObsRequestParams.UPLOAD_ID, request.getUploadId());
        if (null != request.getMaxParts()) {
            requestParameters.put(ObsRequestParams.MAX_PARTS, request.getMaxParts().toString());
        }
        if (null != request.getPartNumberMarker()) {
            requestParameters.put(ObsRequestParams.PART_NUMBER_MARKER, request.getPartNumberMarker().toString());
        }
        if (null != request.getEncodingType()) {
            requestParameters.put(ObsRequestParams.ENCODING_TYPE, request.getEncodingType());
        }

        Response httpResponse = performRestGet(request.getBucketName(), request.getObjectKey(), requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        XmlResponsesSaxParser.ListPartsHandler handler = getXmlResponseSaxParser().parse(
                new HttpMethodReleaseInputStream(httpResponse), XmlResponsesSaxParser.ListPartsHandler.class, true);

        ListPartsResult result = new ListPartsResult.Builder()
                .bucket(handler.getBucketName() == null ? request.getBucketName() : handler.getBucketName())
                .key(handler.getObjectKey() == null ? request.getObjectKey() : handler.getObjectKey())
                .uploadId(handler.getUploadId() == null ? request.getUploadId() : handler.getUploadId())
                .initiator(handler.getInitiator())
                .owner(handler.getOwner())
                .storageClass(StorageClassEnum.getValueFromCode(handler.getStorageClass()))
                .multipartList(handler.getMultiPartList())
                .maxParts(handler.getMaxParts())
                .isTruncated(handler.isTruncated())
                .partNumberMarker(handler.getPartNumberMarker() == null
                        ? (request.getPartNumberMarker() == null ? null : request.getPartNumberMarker().toString())
                        : handler.getPartNumberMarker())
                .nextPartNumberMarker(handler.getNextPartNumberMarker()).builder();

        setHeadersAndStatus(result, httpResponse);
        return result;
    }

    protected UploadPartResult uploadPartImpl(UploadPartRequest request) throws ServiceException {
        TransResult result = null;
        Response response;
        try {
            result = this.transUploadPartRequest(request);
            NewTransResult newTransResult = transObjectRequestWithResult(result, request);
            response = performRequest(newTransResult);
        } finally {
            if (result != null && result.getBody() != null && request.isAutoClose()) {
                RepeatableRequestEntity entity = (RepeatableRequestEntity) result.getBody();
                ServiceUtils.closeStream(entity);
            }
        }
        UploadPartResult ret = new UploadPartResult();
        ret.setEtag(response.header(CommonHeaders.ETAG));
        ret.setPartNumber(request.getPartNumber());
        setHeadersAndStatus(ret, response);
        return ret;
    }

    protected CopyPartResult copyPartImpl(CopyPartRequest request) throws ServiceException {

        TransResult result = this.transCopyPartRequest(request);
        NewTransResult newTransResult = transObjectRequestWithResult(result, request);
        Response response = this.performRequest(newTransResult, true, false, false, false);
        this.verifyResponseContentType(response);

        CopyPartResult ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(response),
                XmlResponsesSaxParser.CopyPartResultHandler.class, true).getCopyPartResult(request.getPartNumber());

        setHeadersAndStatus(ret, response);
        return ret;
    }
}
