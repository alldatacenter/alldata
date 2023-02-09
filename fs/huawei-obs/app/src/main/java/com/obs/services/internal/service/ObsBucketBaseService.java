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

import java.io.IOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.internal.Constants;
import com.obs.services.internal.Constants.CommonHeaders;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.handler.XmlResponsesSaxParser;
import com.obs.services.internal.io.HttpMethodReleaseInputStream;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.BaseBucketRequest;
import com.obs.services.model.BucketLocationResponse;
import com.obs.services.model.BucketMetadataInfoRequest;
import com.obs.services.model.BucketMetadataInfoResult;
import com.obs.services.model.BucketPolicyResponse;
import com.obs.services.model.BucketStorageInfo;
import com.obs.services.model.BucketStoragePolicyConfiguration;
import com.obs.services.model.BucketVersioningConfiguration;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.ListBucketsRequest;
import com.obs.services.model.ListBucketsResult;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ListVersionsResult;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsBucket;
import com.obs.services.model.OptionsInfoRequest;
import com.obs.services.model.SetBucketPolicyRequest;
import com.obs.services.model.SetBucketStoragePolicyRequest;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.VersionOrDeleteMarker;
import com.obs.services.model.fs.GetBucketFSStatusResult;
import com.obs.services.model.fs.SetBucketFSStatusRequest;

import okhttp3.Response;

public abstract class ObsBucketBaseService extends RequestConvertor {

    private static final ILogger log = LoggerBuilder.getLogger(ObsBucketBaseService.class);

    protected ObsBucket createBucketImpl(CreateBucketRequest request) throws ServiceException {
        TransResult result = this.transCreateBucketRequest(request);
        String bucketName = request.getBucketName();
        AccessControlList acl = request.getAcl();

        boolean isExtraAclPutRequired = !prepareRESTHeaderAcl(request.getBucketName(), result.getHeaders(), acl);
        NewTransResult newTransResult = transRequestWithResult(result, request);
        Response response = performRequest(newTransResult);

        if (isExtraAclPutRequired && acl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Creating bucket with a non-canned ACL using REST, so an extra ACL Put is required");
            }
            try {
                putAclImpl(bucketName, null, acl, null, false);
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Try to set bucket acl error", e);
                }
            }
        }

        ObsBucket bucket = new ObsBucket();
        bucket.setBucketName(bucketName);
        bucket.setLocation(request.getLocation());
        bucket.setAcl(acl);
        bucket.setBucketStorageClass(request.getBucketStorageClass());
        setHeadersAndStatus(bucket, response);
        return bucket;
    }

    protected HeaderResponse deleteBucketImpl(BaseBucketRequest request) throws ServiceException {
        Response response = performRestDelete(request.getBucketName(), null, null,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return this.build(response);
    }

    protected ListBucketsResult listAllBucketsImpl(ListBucketsRequest request) throws ServiceException {
        Map<String, String> headers = new HashMap<>();
        if (request != null && request.isQueryLocation()) {
            this.putHeader(headers, this.getIHeaders("").locationHeader(), Constants.TRUE);
        }
        if (request != null && request.getBucketType() != null) {
            this.putHeader(headers, this.getIHeaders("").bucketTypeHeader(), request.getBucketType().getCode());
        }
        Response httpResponse = performRestGetForListBuckets("", null, null, headers);

        this.verifyResponseContentType(httpResponse);

        XmlResponsesSaxParser.ListBucketsHandler handler = getXmlResponseSaxParser().parse(
                new HttpMethodReleaseInputStream(httpResponse), XmlResponsesSaxParser.ListBucketsHandler.class, true);

        ListBucketsResult result = new ListBucketsResult(handler.getBuckets(), handler.getOwner());
        setHeadersAndStatus(result, httpResponse);

        return result;
    }

    protected boolean headBucketImpl(BaseBucketRequest request) throws ServiceException {
        try {
            performRestHead(request.getBucketName(), null, null,
                    transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())), null, true);
            return true;
        } catch (ServiceException e) {
            if (e.getResponseCode() == 404) {
                return false;
            }
            throw e;
        }
    }

    protected GetBucketFSStatusResult getBucketMetadataImpl(BucketMetadataInfoRequest bucketMetadataInfoRequest)
            throws ServiceException {
        GetBucketFSStatusResult output = null;
        String origin = bucketMetadataInfoRequest.getOrigin();
        List<String> requestHeaders = bucketMetadataInfoRequest.getRequestHeaders();
        if (origin != null && requestHeaders != null && requestHeaders.size() > 0) {
            for (String value : requestHeaders) {
                Map<String, String> headers = new HashMap<>();
                headers.put(CommonHeaders.ORIGIN, origin);
                headers.put(CommonHeaders.ACCESS_CONTROL_REQUEST_HEADERS, value);
                transRequestPaymentHeaders(bucketMetadataInfoRequest, headers,
                        this.getIHeaders(bucketMetadataInfoRequest.getBucketName()));

                Response response = performRestHead(bucketMetadataInfoRequest.getBucketName(), null,
                        null, headers, bucketMetadataInfoRequest.getUserHeaders(), true);

                if (output == null) {
                    output = this.getOptionInfoResult(bucketMetadataInfoRequest.getBucketName(), response);
                } else {
                    String header = response.header(CommonHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
                    if (header != null) {
                        if (!output.getAllowHeaders().contains(header)) {
                            output.getAllowHeaders().add(header);
                        }
                    }
                }
                response.close();
            }
        } else {
            Map<String, String> headers = new HashMap<>();
            if (origin != null) {
                headers.put(Constants.CommonHeaders.ORIGIN, origin);
            }
            transRequestPaymentHeaders(bucketMetadataInfoRequest, headers,
                    this.getIHeaders(bucketMetadataInfoRequest.getBucketName()));

            Response response = performRestHead(bucketMetadataInfoRequest.getBucketName(), null,
                    null, headers, bucketMetadataInfoRequest.getUserHeaders(), true);
            output = this.getOptionInfoResult(bucketMetadataInfoRequest.getBucketName(), response);
            response.close();
        }

        return output;
    }

    protected HeaderResponse setBucketFSStatusImpl(SetBucketFSStatusRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.FILEINTERFACE.getOriginalStringCode(), "");

        Map<String, String> headers = transRequestPaymentHeaders(request, null,
                this.getIHeaders(request.getBucketName()));

        String xml = this.getIConvertor(request.getBucketName()).transBucketFileInterface(request.getStatus());
        headers.put(CommonHeaders.CONTENT_LENGTH, String.valueOf(xml.length()));
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);

        return build(response);
    }

    protected BucketStoragePolicyConfiguration getBucketStoragePolicyImpl(BaseBucketRequest request)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(this.getSpecialParamForStorageClass(request.getBucketName()).getOriginalStringCode(), "");

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketStoragePolicyConfiguration ret = getXmlResponseSaxParser()
                .parse(new HttpMethodReleaseInputStream(httpResponse),
                        XmlResponsesSaxParser.BucketStoragePolicyHandler.class, false)
                .getStoragePolicy();
        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected HeaderResponse setBucketStorageImpl(SetBucketStoragePolicyRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(this.getSpecialParamForStorageClass(request.getBucketName()).getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();
        String xml = request.getBucketStorage() == null ? ""
                : this.getIConvertor(request.getBucketName()).transStoragePolicy(request.getBucketStorage());

        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        headers.put(CommonHeaders.CONTENT_LENGTH, String.valueOf(xml.length()));
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));

        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected BucketStorageInfo getBucketStorageInfoImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.STORAGEINFO.getOriginalStringCode(), "");

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketStorageInfo ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.BucketStorageInfoHandler.class, false).getStorageInfo();
        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected BucketLocationResponse getBucketLocationImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.LOCATION.getOriginalStringCode(), "");

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketLocationResponse ret = new BucketLocationResponse(
                getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                        XmlResponsesSaxParser.BucketLocationHandler.class, false).getLocation());

        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected HeaderResponse setBucketPolicyImpl(SetBucketPolicyRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.POLICY.getOriginalStringCode(), "");

        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_TEXT_PLAIN);
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));
        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_TEXT_PLAIN, request.getPolicy()));
        Response response = performRequest(result);
        return build(response);
    }

    protected BucketPolicyResponse getBucketPolicyImpl(BaseBucketRequest request) throws ServiceException {
        try {
            Map<String, String> requestParameters = new HashMap<>();
            requestParameters.put(SpecialParamEnum.POLICY.getOriginalStringCode(), "");

            Response response = performRestGet(request.getBucketName(), null, requestParameters,
                    transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                    request.getUserHeaders());
            BucketPolicyResponse ret = new BucketPolicyResponse(response.body().string());

            setHeadersAndStatus(ret, response);
            return ret;
        } catch (IOException e) {
            throw new ServiceException(e);
        }
    }

    protected HeaderResponse deleteBucketPolicyImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.POLICY.getOriginalStringCode(), "");
        Response response = performRestDelete(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return build(response);
    }

    protected ListVersionsResult listVersionsImpl(ListVersionsRequest request) throws ServiceException {

        TransResult result = this.transListVersionsRequest(request);

        Response response = performRestGet(request.getBucketName(), null, result.getParams(),
                result.getHeaders(), request.getUserHeaders());

        this.verifyResponseContentType(response);

        XmlResponsesSaxParser.ListVersionsHandler handler = getXmlResponseSaxParser().parse(
                new HttpMethodReleaseInputStream(response), XmlResponsesSaxParser.ListVersionsHandler.class, true);
        List<VersionOrDeleteMarker> partialItems = handler.getItems();

        ListVersionsResult listVersionsResult = new ListVersionsResult.Builder()
                .bucketName(handler.getBucketName() == null ? request.getBucketName() : handler.getBucketName())
                .prefix(handler.getRequestPrefix() == null ? request.getPrefix() : handler.getRequestPrefix())
                .keyMarker(handler.getKeyMarker() == null ? request.getKeyMarker() : handler.getKeyMarker())
                .nextKeyMarker(handler.getNextKeyMarker())
                .versionIdMarker(handler.getVersionIdMarker() == null
                        ? request.getVersionIdMarker() : handler.getVersionIdMarker())
                .nextVersionIdMarker(handler.getNextVersionIdMarker())
                .maxKeys(String.valueOf(handler.getRequestMaxKeys()))
                .isTruncated(handler.isListingTruncated())
                .versions(partialItems.toArray(new VersionOrDeleteMarker[partialItems.size()]))
                .commonPrefixes(handler.getCommonPrefixes())
                .location(response.header(this.getIHeaders(request.getBucketName()).bucketRegionHeader()))
                .delimiter(handler.getDelimiter() == null ? request.getDelimiter() : handler.getDelimiter())
                .builder();

        setHeadersAndStatus(listVersionsResult, response);
        return listVersionsResult;
    }

    protected ObjectListing listObjectsImpl(ListObjectsRequest listObjectsRequest) throws ServiceException {

        TransResult result = this.transListObjectsRequest(listObjectsRequest);

        Response httpResponse = performRestGet(listObjectsRequest.getBucketName(), null, result.getParams(),
                result.getHeaders(), listObjectsRequest.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        XmlResponsesSaxParser.ListObjectsHandler listObjectsHandler = getXmlResponseSaxParser().parse(
                new HttpMethodReleaseInputStream(httpResponse), XmlResponsesSaxParser.ListObjectsHandler.class, true);

        ObjectListing objList = new ObjectListing.Builder()
                .objectSummaries(listObjectsHandler.getObjects())
                .commonPrefixes(listObjectsHandler.getCommonPrefixes())
                .bucketName(listObjectsHandler.getBucketName() == null
                        ? listObjectsRequest.getBucketName() : listObjectsHandler.getBucketName())
                .truncated(listObjectsHandler.isListingTruncated())
                .prefix(listObjectsHandler.getRequestPrefix() == null
                        ? listObjectsRequest.getPrefix() : listObjectsHandler.getRequestPrefix())
                .marker(listObjectsHandler.getRequestMarker() == null
                        ? listObjectsRequest.getMarker() : listObjectsHandler.getRequestMarker())
                .maxKeys(listObjectsHandler.getRequestMaxKeys())
                .delimiter(listObjectsHandler.getRequestDelimiter() == null
                        ? listObjectsRequest.getDelimiter() : listObjectsHandler.getRequestDelimiter())
                .nextMarker(listObjectsHandler.getMarkerForNextListing())
                .location(httpResponse.header(this.getIHeaders(listObjectsRequest.getBucketName())
                        .bucketRegionHeader()))
                .extendCommonPrefixes(listObjectsHandler.getExtendCommonPrefixes())
                .builder();

        setHeadersAndStatus(objList, httpResponse);
        return objList;
    }

    protected BucketMetadataInfoResult optionsImpl(String bucketName, String objectName, OptionsInfoRequest option)
            throws ServiceException {
        Map<String, String> metadata = new IdentityHashMap<>();

        if (ServiceUtils.isValid(option.getOrigin())) {
            metadata.put(CommonHeaders.ORIGIN, option.getOrigin().trim());
        }

        for (int i = 0; option.getRequestMethod() != null && i < option.getRequestMethod().size(); i++) {
            metadata.put(new String(new StringBuilder(CommonHeaders.ACCESS_CONTROL_REQUEST_METHOD)),
                    option.getRequestMethod().get(i));
        }
        for (int i = 0; option.getRequestHeaders() != null && i < option.getRequestHeaders().size(); i++) {
            metadata.put(new String(new StringBuilder(CommonHeaders.ACCESS_CONTROL_REQUEST_HEADERS)),
                    option.getRequestHeaders().get(i));
        }
        transRequestPaymentHeaders(option.isRequesterPays(), metadata, this.getIHeaders(bucketName));

        Response result = performRestOptions(bucketName, objectName, metadata, null, true);
        return getOptionInfoResult(bucketName, result);

    }

    protected BucketVersioningConfiguration getBucketVersioningImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.VERSIONING.getOriginalStringCode(), "");

        Response response = performRestGet(request.getBucketName(), null, requestParams,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(response);

        BucketVersioningConfiguration ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(response),
                XmlResponsesSaxParser.BucketVersioningHandler.class, false).getVersioningStatus();
        setHeadersAndStatus(ret, response);
        return ret;
    }
}
