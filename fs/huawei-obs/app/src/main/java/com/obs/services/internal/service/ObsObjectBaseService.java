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

import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.internal.Constants;
import com.obs.services.internal.Constants.CommonHeaders;
import com.obs.services.internal.Constants.ObsRequestParams;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.ProgressManager;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.SimpleProgressManager;
import com.obs.services.internal.handler.XmlResponsesSaxParser;
import com.obs.services.internal.io.HttpMethodReleaseInputStream;
import com.obs.services.internal.io.ProgressInputStream;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.RestUtils;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.CopyObjectRequest;
import com.obs.services.model.CopyObjectResult;
import com.obs.services.model.DeleteObjectRequest;
import com.obs.services.model.DeleteObjectResult;
import com.obs.services.model.DeleteObjectsRequest;
import com.obs.services.model.DeleteObjectsResult;
import com.obs.services.model.GetObjectAclRequest;
import com.obs.services.model.GetObjectMetadataRequest;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.ObsObject;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.SetObjectAclRequest;
import com.obs.services.model.SetObjectMetadataRequest;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.fs.DropFileResult;
import com.obs.services.model.fs.ObsFSAttribute;
import com.obs.services.model.fs.ObsFSFile;
import com.obs.services.model.fs.ReadFileResult;
import com.obs.services.model.select.SelectObjectRequest;
import com.obs.services.model.select.SelectObjectResult;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public abstract class ObsObjectBaseService extends ObsBucketAdvanceService {
    private static final ILogger log = LoggerBuilder.getLogger(ObsObjectBaseService.class);

    protected boolean doesObjectExistImpl(GetObjectMetadataRequest request) throws ServiceException {
        Map<String, String> headers = new HashMap<>();
        this.transSseCHeaders(request.getSseCHeader(), headers, this.getIHeaders(request.getBucketName()));
        this.transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        Map<String, String> params = new HashMap<>();
        if (request.getVersionId() != null) {
            params.put(ObsRequestParams.VERSION_ID, request.getVersionId());
        }
        boolean doesObjectExist = false;
        try {
            Response response = performRestHead(request.getBucketName(), request.getObjectKey(), params, headers,
                    new HashMap<>(), request.isEncodeHeaders());
            if (200 == response.code()) {
                doesObjectExist = true;
            }
        } catch (ServiceException ex) {
            if (!(404 == ex.getResponseCode())) {
                throw ex;
            }
        }
        return doesObjectExist;
    }

    protected ObsFSFile putObjectImpl(PutObjectRequest request) throws ServiceException {

        TransResult result = null;
        Response response;
        boolean isExtraAclPutRequired;
        AccessControlList acl = request.getAcl();
        NewTransResult newTransResult;
        try {
            result = this.transPutObjectRequest(request);
            isExtraAclPutRequired = !prepareRESTHeaderAcl(request.getBucketName(), result.getHeaders(), acl);
            // todo prepareRESTHeaderAcl 也会操作头域，下次重构可以将其合并
            newTransResult = transObjectRequestWithResult(result, request);
            response = performRequest(newTransResult);
        } finally {
            if (result != null && result.getBody() != null && request.isAutoClose()) {
                if (result.getBody() instanceof Closeable) {
                    ServiceUtils.closeStream((Closeable) result.getBody());
                }
            }
        }

        ObsFSFile ret = new ObsFSFile(request.getBucketName(), request.getObjectKey(),
                response.header(CommonHeaders.ETAG),
                response.header(this.getIHeaders(request.getBucketName()).versionIdHeader()),
                StorageClassEnum.getValueFromCode(response.header(this.getIHeaders(request.getBucketName())
                        .storageClassHeader())), this.getObjectUrl(request.getBucketName(), request.getObjectKey(),
                request.getIsIgnorePort()));

        setHeadersAndStatus(ret, response);
        if (isExtraAclPutRequired && acl != null) {
            try {
                putAclImpl(request.getBucketName(), request.getObjectKey(), acl, null, request.isRequesterPays());
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Try to set object acl error", e);
                }
            }
        }
        return ret;
    }

    protected ObsObject getObjectImpl(GetObjectRequest request) throws ServiceException {
        return (ObsObject) this.getObjectImpl((GetObjectMetadataRequest) request);
    }

    protected Object getObjectImpl(GetObjectMetadataRequest request) throws ServiceException {
        Response response;
        GetObjectRequest getRequest = null;
        if (!(request instanceof GetObjectRequest)) {
            Map<String, String> headers = new HashMap<>();
            this.transSseCHeaders(request.getSseCHeader(), headers, this.getIHeaders(request.getBucketName()));
            this.transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

            Map<String, String> params = new HashMap<>();
            if (request.getVersionId() != null) {
                params.put(ObsRequestParams.VERSION_ID, request.getVersionId());
            }
            response = performRestHead(request.getBucketName(), request.getObjectKey(),
                    params, headers, request.getUserHeaders(), request.isEncodeHeaders());
        } else {
            getRequest = (GetObjectRequest) request;
            TransResult result = this.transGetObjectRequest(getRequest);
            if (getRequest.getRequestParameters() != null) {
                result.getParams().putAll(getRequest.getRequestParameters());
            }
            response = performRestGet(request.getBucketName(), request.getObjectKey(), result.getParams(),
                    result.getHeaders(), request.getUserHeaders(), false, request.isEncodeHeaders());
        }

        ObsFSAttribute objMetadata = this.getObsFSAttributeFromResponse(request.getBucketName(),
                response, request.isEncodeHeaders());

        if (!(request instanceof GetObjectRequest)) {
            response.close();
            return objMetadata;
        }
        ReadFileResult obsObject = new ReadFileResult();
        obsObject.setObjectKey(request.getObjectKey());
        obsObject.setBucketName(request.getBucketName());
        obsObject.setMetadata(objMetadata);
        // pmd error message: CloseResource - Ensure that resources like this
        // InputStream object are closed after use
        // 该接口是下载对象，需要将流返回给客户（调用方），我们不能关闭这个流
        InputStream input = response.body().byteStream(); // NOPMD
        if (getRequest.getProgressListener() != null) {
            ProgressManager progressManager = new SimpleProgressManager(objMetadata.getContentLength(), 0,
                    getRequest.getProgressListener(),
                    getRequest.getProgressInterval() > 0
                            ? getRequest.getProgressInterval() : ObsConstraint.DEFAULT_PROGRESS_INTERVAL);
            input = new ProgressInputStream(input, progressManager);
        }

        int readBufferSize = obsProperties.getIntProperty(ObsConstraint.READ_BUFFER_SIZE,
                ObsConstraint.DEFAULT_READ_BUFFER_STREAM);
        if (readBufferSize > 0) {
            input = new BufferedInputStream(input, readBufferSize);
        }

        obsObject.setObjectContent(input);
        return obsObject;
    }

    protected SelectObjectResult selectObjectContentImpl(SelectObjectRequest selectRequest)
        throws ServiceException {
        Map<String, String> httpHeaders = new HashMap<>();
        httpHeaders.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        final String bucket = selectRequest.getBucketName();

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("select", null);
        requestParams.put("select-type", "2");

        String xml = selectRequest.convertToXml();

        if (log.isDebugEnabled()) {
            log.debug(
                String.format(
                    "selectObjectContentImpl: bucket: %s; object: %s; request XML: %n%s%n",
                    bucket,
                    selectRequest.getKey(),
                    xml));
        }
        NewTransResult transResult = new NewTransResult();
        transResult.setHttpMethod(HttpMethodEnum.POST);
        transResult.setBucketName(bucket);
        transResult.setObjectKey(selectRequest.getKey());
        transResult.setParams(requestParams);
        transResult.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Request.Builder builder = setupConnection(transResult, false, false);

        renameMetadataKeys(bucket, builder, httpHeaders);

        Response response = performRequest(
            builder.build(), requestParams, bucket, true, false);

        ResponseBody responseBody = response.body();
        return new SelectObjectResult(responseBody.byteStream());
    }

    protected DeleteObjectsResult deleteObjectsImpl(DeleteObjectsRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.DELETE.getOriginalStringCode(), "");

        String xml = this.getIConvertor(request.getBucketName()).transKeyAndVersion(request.getKeyAndVersions(),
                request.isQuiet(), request.getEncodingType());

        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_LENGTH, String.valueOf(xml.length()));
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);

        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));
        NewTransResult transResult = transRequest(request);
        transResult.setParams(requestParams);
        transResult.setHeaders(headers);
        transResult.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(transResult, true, false, false, false);
        this.verifyResponseContentType(response);

        DeleteObjectsResult ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(response),
                XmlResponsesSaxParser.DeleteObjectsHandler.class, true).getMultipleDeleteResult();

        setHeadersAndStatus(ret, response);
        return ret;
    }

    protected DeleteObjectResult deleteObjectImpl(DeleteObjectRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        if (request.getVersionId() != null) {
            requestParameters.put(ObsRequestParams.VERSION_ID, request.getVersionId());
        }

        Response response = performRestDelete(request.getBucketName(), request.getObjectKey(), requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        DropFileResult result = new DropFileResult(Boolean.valueOf(response.header(this.getIHeaders(
                request.getBucketName()).deleteMarkerHeader())), request.getObjectKey(),
                response.header(this.getIHeaders(request.getBucketName()).versionIdHeader()));
        setHeadersAndStatus(result, response);
        return result;
    }

    protected CopyObjectResult copyObjectImpl(CopyObjectRequest request) throws ServiceException {

        TransResult result = this.transCopyObjectRequest(request);

        AccessControlList acl = request.getAcl();
        // todo prepareRESTHeaderAcl 也会操作头域，下次重构可以将其合并
        boolean isExtraAclPutRequired = !prepareRESTHeaderAcl(request.getBucketName(), result.getHeaders(), acl);
        NewTransResult newTransResult = transObjectRequestWithResult(result, request);

        Response response = performRequest(newTransResult, true, false, false, false);

        this.verifyResponseContentType(response);

        XmlResponsesSaxParser.CopyObjectResultHandler handler = getXmlResponseSaxParser().parse(
                new HttpMethodReleaseInputStream(response), XmlResponsesSaxParser.CopyObjectResultHandler.class, false);
        CopyObjectResult copyRet = new CopyObjectResult(handler.getETag(), handler.getLastModified(),
                response.header(this.getIHeaders(request.getBucketName()).versionIdHeader()),
                response.header(this.getIHeaders(request.getBucketName()).copySourceVersionIdHeader()),
                StorageClassEnum.getValueFromCode(response.header(
                        this.getIHeaders(request.getBucketName()).storageClassHeader())));

        setHeadersAndStatus(copyRet, response);
        if (isExtraAclPutRequired && acl != null) {
            if (log.isDebugEnabled()) {
                log.debug("Creating object with a non-canned ACL using REST, so an extra ACL Put is required");
            }
            try {
                putAclImpl(request.getDestinationBucketName(), request.getDestinationObjectKey(), acl, null,
                        request.isRequesterPays());
            } catch (Exception e) {
                if (log.isWarnEnabled()) {
                    log.warn("Try to set object acl error", e);
                }
            }
        }

        return copyRet;
    }

    protected ObjectMetadata setObjectMetadataImpl(SetObjectMetadataRequest request) {
        TransResult result = this.transSetObjectMetadataRequest(request);
        NewTransResult newTransResult = transObjectRequestWithResult(result, request);
        Response response = performRequest(newTransResult);
        return this.getObsFSAttributeFromResponse(request.getBucketName(), response, request.isEncodeHeaders());
    }

    protected ObsFSAttribute getObjectMetadataImpl(GetObjectMetadataRequest request) throws ServiceException {
        return (ObsFSAttribute) this.getObjectImpl(request);
    }

    protected HeaderResponse setObjectAclImpl(SetObjectAclRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.ACL.getOriginalStringCode(), "");
        if (request.getVersionId() != null) {
            requestParams.put(ObsRequestParams.VERSION_ID, request.getVersionId());
        }
        RequestBody entity = null;
        if (ServiceUtils.isValid(request.getCannedACL())) {
            request.setAcl(this.getIConvertor(request.getBucketName()).transCannedAcl(request.getCannedACL().trim()));
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        boolean isExtraAclPutRequired = !prepareRESTHeaderAclObject(request.getBucketName(), headers, request.getAcl());
        if (isExtraAclPutRequired) {
            String xml = request.getAcl() == null ? ""
                    : this.getIConvertor(request.getBucketName()).transAccessControlList(request.getAcl(), false);
            headers.put(CommonHeaders.CONTENT_LENGTH, String.valueOf(xml.length()));
            headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
            entity = createRequestBody(Mimetypes.MIMETYPE_XML, xml);
        }

        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));
        NewTransResult result = transObjectRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(entity);
        Response response = performRequest(result);
        return build(response);
    }

    protected AccessControlList getObjectAclImpl(GetObjectAclRequest getObjectAclRequest) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.ACL.getOriginalStringCode(), "");
        if (ServiceUtils.isValid(getObjectAclRequest.getVersionId())) {
            requestParameters.put(ObsRequestParams.VERSION_ID, getObjectAclRequest.getVersionId().trim());
        }

        Response httpResponse = performRestGet(getObjectAclRequest.getBucketName(), getObjectAclRequest.getObjectKey(),
                requestParameters, transRequestPaymentHeaders(getObjectAclRequest, null,
                        this.getIHeaders(getObjectAclRequest.getBucketName())), getObjectAclRequest.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        AccessControlList ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.AccessControlListHandler.class, false).getAccessControlList();
        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected String getObjectUrl(String bucketName, String objectKey, boolean isIgnorePort) {
        boolean pathStyle = this.isPathStyle();
        boolean https = this.getHttpsOnly();
        boolean isCname = this.isCname();
        String port = "";
        if (!isIgnorePort) {
            port = ":" + (https ? this.getHttpsPort() : this.getHttpPort());
        }
        return (https ? "https://" : "http://") + (pathStyle || isCname ? "" : bucketName + ".")
                + this.getEndpoint() + port + "/" + (pathStyle ? bucketName + "/" : "")
                + RestUtils.uriEncode(objectKey, false);
    }

    private ObsFSAttribute getObsFSAttributeFromResponse(String bucketName, Response response, boolean needDecode) {
        Date lastModifiedDate = null;
        String lastModified = response.header(CommonHeaders.LAST_MODIFIED);
        if (lastModified != null) {
            try {
                lastModifiedDate = ServiceUtils.parseRfc822Date(lastModified);
            } catch (ParseException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Response last-modified is not well-format", e);
                }
            }
        }
        ObsFSAttribute objMetadata = new ObsFSAttribute();
        objMetadata.setLastModified(lastModifiedDate);
        objMetadata.setContentEncoding(response.header(CommonHeaders.CONTENT_ENCODING));
        objMetadata.setContentType(response.header(CommonHeaders.CONTENT_TYPE));

        objMetadata.setContentDisposition(response.header(CommonHeaders.CONTENT_DISPOSITION));
        objMetadata.setContentLanguage(response.header(CommonHeaders.CONTENT_LANGUAGE));
        objMetadata.setCacheControl(response.header(CommonHeaders.CACHE_CONTROL));
        objMetadata.setExpires(response.header(CommonHeaders.EXPIRES));
        objMetadata.setCrc64(response.header(CommonHeaders.HASH_CRC64ECMA));

        String contentLength = response.header(CommonHeaders.CONTENT_LENGTH);
        if (contentLength != null) {
            try {
                objMetadata.setContentLength(Long.parseLong(contentLength));
            } catch (NumberFormatException e) {
                if (log.isWarnEnabled()) {
                    log.warn("Response content-length is not well-format", e);
                }
            }
        }
        String fsMode;
        if ((fsMode = response.header(this.getIHeaders(bucketName).fsModeHeader())) != null) {
            objMetadata.setMode(Integer.parseInt(fsMode));
        }
        objMetadata.setWebSiteRedirectLocation(response.header(this.getIHeaders(bucketName)
                .websiteRedirectLocationHeader()));
        objMetadata.setObjectStorageClass(
                StorageClassEnum.getValueFromCode(response.header(this.getIHeaders(bucketName).storageClassHeader())));

        String etag = response.header(CommonHeaders.ETAG);
        objMetadata.setEtag(etag);
        if (etag != null && !etag.contains("-")) {
            String md5 = etag;
            if (md5.startsWith("\"")) {
                md5 = md5.substring(1);
            }
            if (md5.endsWith("\"")) {
                md5 = md5.substring(0, md5.length() - 1);
            }
            try {
                objMetadata.setContentMd5(ServiceUtils.toBase64(ServiceUtils.fromHex(md5)));
            } catch (Exception e) {
                if (log.isDebugEnabled()) {
                    log.debug(e.getMessage(), e);
                }
            }
        }

        objMetadata.setAppendable("Appendable".equals(response.header(this.getIHeaders(bucketName)
                .objectTypeHeader())));
        String nextPosition = response.header(this.getIHeaders(bucketName).nextPositionHeader(), "-1");
        objMetadata.setNextPosition(Long.parseLong(nextPosition));
        if (objMetadata.getNextPosition() == -1L) {
            objMetadata.setNextPosition(Long.parseLong(response.header(Constants.CommonHeaders.CONTENT_LENGTH, "-1")));
        }
        setHeadersAndStatus(objMetadata, response, needDecode);
        objMetadata.setUserMetadata(ServiceUtils.cleanUserMetadata(objMetadata.getOriginalHeaders(), needDecode));
        return objMetadata;
    }
}
