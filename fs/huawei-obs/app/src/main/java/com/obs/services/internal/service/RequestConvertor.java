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
import com.obs.services.internal.IConvertor;
import com.obs.services.internal.IHeaders;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.ProgressManager;
import com.obs.services.internal.RepeatableRequestEntity;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.SimpleProgressManager;
import com.obs.services.internal.io.ProgressInputStream;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.RestUtils;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.internal.xml.OBSXMLBuilder;
import com.obs.services.model.AppendObjectRequest;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.BucketTypeEnum;
import com.obs.services.model.CopyObjectRequest;
import com.obs.services.model.CopyPartRequest;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.ExtensionBucketPermissionEnum;
import com.obs.services.model.ExtensionObjectPermissionEnum;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.InitiateMultipartUploadRequest;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ListVersionsRequest;
import com.obs.services.model.ModifyObjectRequest;
import com.obs.services.model.ObjectMetadata;
import com.obs.services.model.PutObjectBasicRequest;
import com.obs.services.model.PutObjectRequest;
import com.obs.services.model.RestoreObjectRequest.RestoreObjectStatus;
import com.obs.services.model.RestoreObjectResult;
import com.obs.services.model.SetObjectMetadataRequest;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.SseCHeader;
import com.obs.services.model.SseKmsHeader;
import com.obs.services.model.UploadPartRequest;
import com.obs.services.model.fs.ContentSummaryFsRequest;
import com.obs.services.model.fs.ListContentSummaryFsRequest;
import com.obs.services.model.fs.ListContentSummaryRequest;
import com.obs.services.model.fs.NewBucketRequest;
import com.obs.services.model.fs.WriteFileRequest;
import okhttp3.RequestBody;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public abstract class RequestConvertor extends AclHeaderConvertor {
    private static final ILogger log = LoggerBuilder.getLogger("com.obs.services.ObsClient");

    protected TransResult transListVersionsRequest(ListVersionsRequest request) {
        Map<String, String> params = new HashMap<String, String>();
        params.put(SpecialParamEnum.VERSIONS.getOriginalStringCode(), "");
        if (request.getPrefix() != null) {
            params.put(ObsRequestParams.PREFIX, request.getPrefix());
        }
        if (request.getDelimiter() != null) {
            params.put(ObsRequestParams.DELIMITER, request.getDelimiter());
        }
        if (request.getMaxKeys() > 0) {
            params.put(ObsRequestParams.MAX_KEYS, String.valueOf(request.getMaxKeys()));
        }
        if (request.getKeyMarker() != null) {
            params.put(ObsRequestParams.KEY_MARKER, request.getKeyMarker());
        }
        if (request.getVersionIdMarker() != null) {
            params.put(ObsRequestParams.VERSION_ID_MARKER, request.getVersionIdMarker());
        }
        if (request.getEncodingType() != null) {
            params.put(ObsRequestParams.ENCODING_TYPE, request.getEncodingType());
        }
        Map<String, String> headers = new HashMap<String, String>();
        if (request.getListTimeout() > 0) {
            putHeader(headers, this.getIHeaders(request.getBucketName()).listTimeoutHeader(),
                    String.valueOf(request.getListTimeout()));
        }

        this.transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        return new TransResult(headers, params, null);
    }

    protected TransResult transInitiateMultipartUploadRequest(InitiateMultipartUploadRequest request)
            throws ServiceException {
        Map<String, String> headers = new HashMap<String, String>();
        IHeaders iheaders = this.getIHeaders(request.getBucketName());
        IConvertor iconvertor = this.getIConvertor(request.getBucketName());

        ObjectMetadata objectMetadata = request.getMetadata() == null ? new ObjectMetadata() : request.getMetadata();

        selectAllowedHeader(headers, objectMetadata);

        if (objectMetadata.getObjectStorageClass() != null) {
            putHeader(headers, iheaders.storageClassHeader(),
                    iconvertor.transStorageClass(objectMetadata.getObjectStorageClass()));
        }

        if (ServiceUtils.isValid(objectMetadata.getWebSiteRedirectLocation())) {
            putHeader(headers, iheaders.websiteRedirectLocationHeader(), objectMetadata.getWebSiteRedirectLocation());
        }

        if (ServiceUtils.isValid(request.getSuccessRedirectLocation())) {
            putHeader(headers, iheaders.successRedirectLocationHeader(), request.getSuccessRedirectLocation());
        }

        if (request.getExpires() > 0) {
            putHeader(headers, iheaders.expiresHeader(), String.valueOf(request.getExpires()));
        }

        setBaseHeaderFromMetadata(request.getBucketName(), headers, objectMetadata);

        transRequestPaymentHeaders(request, headers, iheaders);

        transExtensionPermissions(request, headers);

        transSseHeaders(request, headers, iheaders);

        Object contentType = objectMetadata.getContentType() == null
                ? objectMetadata.getValue(CommonHeaders.CONTENT_TYPE) : objectMetadata.getContentType();
        if (contentType == null) {
            contentType = Mimetypes.getInstance().getMimetype(request.getObjectKey());
        }

        String contentTypeStr = contentType.toString().trim();
        headers.put(CommonHeaders.CONTENT_TYPE, contentTypeStr);

        Map<String, String> params = new HashMap<String, String>();
        params.put(SpecialParamEnum.UPLOADS.getOriginalStringCode(), "");

        if (request.getEncodingType() != null) {
            params.put(ObsRequestParams.ENCODING_TYPE, request.getEncodingType());
        }

        return new TransResult(headers, params, null);
    }

    private void selectAllowedHeader(Map<String, String> headers, ObjectMetadata objectMetadata) {
        for (Map.Entry<String, Object> entry : objectMetadata.getAllMetadata().entrySet()) {
            String key = entry.getKey();
            if (!ServiceUtils.isValid(key)) {
                continue;
            }
            key = key.trim();
            if ((CAN_USE_STANDARD_HTTP_HEADERS.get() == null || (CAN_USE_STANDARD_HTTP_HEADERS.get() != null
                    && !CAN_USE_STANDARD_HTTP_HEADERS.get()))
                    && Constants.ALLOWED_REQUEST_HTTP_HEADER_METADATA_NAMES.contains(key.toLowerCase())) {
                continue;
            }
            headers.put(key, entry.getValue() == null ? "" : entry.getValue().toString());
        }
    }

    protected void transExtensionPermissions(PutObjectBasicRequest request, Map<String, String> headers) {
        Set<ExtensionObjectPermissionEnum> extensionPermissionEnums = request.getAllGrantPermissions();
        if (!extensionPermissionEnums.isEmpty()) {
            for (ExtensionObjectPermissionEnum extensionPermissionEnum : extensionPermissionEnums) {
                Set<String> domainIds = request.getDomainIdsByGrantPermission(extensionPermissionEnum);
                List<String> domainIdList = new ArrayList<String>(domainIds.size());
                for (String domainId : domainIds) {
                    domainIdList.add("id=" + domainId);
                }
                putHeader(headers, getHeaderByMethodName(request.getBucketName(), extensionPermissionEnum.getCode()),
                        ServiceUtils.join(domainIdList, ","));
            }
        }
    }

    protected void transSseHeaders(PutObjectBasicRequest request, Map<String, String> headers, IHeaders iheaders)
            throws ServiceException {
        if (null != request.getSseCHeader()) {
            this.transSseCHeaders(request.getSseCHeader(), headers, iheaders);
        } else if (null != request.getSseKmsHeader()) {
            this.transSseKmsHeaders(request.getSseKmsHeader(), headers, iheaders, request.getBucketName());
        }
    }

    protected void transSseCHeaders(SseCHeader ssecHeader, Map<String, String> headers, IHeaders iheaders)
            throws ServiceException {
        if (ssecHeader == null) {
            return;
        }

        String sseCAlgorithm = ssecHeader.getSSEAlgorithm().getCode();

        putHeader(headers, iheaders.sseCHeader(), ServiceUtils.toValid(sseCAlgorithm));
        if (ssecHeader.getSseCKeyBase64() != null) {
            try {
                putHeader(headers, iheaders.sseCKeyHeader(), ssecHeader.getSseCKeyBase64());
                putHeader(headers, iheaders.sseCKeyMd5Header(), ServiceUtils
                        .toBase64(ServiceUtils.computeMD5Hash(ServiceUtils.fromBase64(ssecHeader.getSseCKeyBase64()))));
            } catch (IOException e) {
                throw new IllegalStateException("fail to read sseCkey", e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("fail to read sseCkey", e);
            }
        } else if (null != ssecHeader.getSseCKey()
                && ssecHeader.getSseCKey().length > 0) {
            try {
                byte[] data = ssecHeader.getSseCKey();
                putHeader(headers, iheaders.sseCKeyHeader(), ServiceUtils.toBase64(data));
                putHeader(headers, iheaders.sseCKeyMd5Header(),
                        ServiceUtils.toBase64(ServiceUtils.computeMD5Hash(data)));
            } catch (IOException e) {
                throw new IllegalStateException("fail to read sseCkey", e);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("fail to read sseCkey", e);
            }
        }
    }

    protected TransResult transCreateBucketRequest(CreateBucketRequest request) throws ServiceException {
        Map<String, String> headers = new HashMap<String, String>();
        IConvertor convertor = this.getIConvertor(request.getBucketName());

        if (request.getBucketStorageClass() != null) {
            putHeader(headers, getIHeaders(request.getBucketName()).defaultStorageClassHeader(),
                    convertor.transStorageClass(request.getBucketStorageClass()));
        }

        if (request.getEpid() != null) {
            putHeader(headers, getIHeaders(request.getBucketName()).epidHeader(), request.getEpid());
        }

        if (request instanceof NewBucketRequest) {
            putHeader(headers, getIHeaders(request.getBucketName()).fsFileInterfaceHeader(), Constants.ENABLED);
        }

        if (null != request.getBucketType() && BucketTypeEnum.PFS == request.getBucketType()) {
            putHeader(headers, getIHeaders(request.getBucketName()).fsFileInterfaceHeader(), Constants.ENABLED);
        }

        if (request.getAvailableZone() != null) {
            putHeader(headers, getIHeaders(request.getBucketName()).azRedundancyHeader(),
                    request.getAvailableZone().getCode());
        }

        Set<ExtensionBucketPermissionEnum> extensionPermissionEnums = request.getAllGrantPermissions();
        if (!extensionPermissionEnums.isEmpty()) {
            for (ExtensionBucketPermissionEnum extensionPermissionEnum : extensionPermissionEnums) {
                Set<String> domainIds = request.getDomainIdsByGrantPermission(extensionPermissionEnum);
                List<String> domainIdList = new ArrayList<String>(domainIds.size());
                for (String domainId : domainIds) {
                    domainIdList.add("id=" + domainId);
                }
                putHeader(headers, getHeaderByMethodName(request.getBucketName(), extensionPermissionEnum.getCode()),
                        ServiceUtils.join(domainIdList, ","));
            }
        }

        if (request.getExtensionHeaderMap() != null) {
            for (Entry<String, String> kv : request.getExtensionHeaderMap().entrySet()) {
                putHeader(headers, kv.getKey(), kv.getValue());
            }
        }

        String contentType = Mimetypes.MIMETYPE_XML;
        headers.put(Constants.CommonHeaders.CONTENT_TYPE, contentType);
        TransResult result = new TransResult(headers);
        if (ServiceUtils.isValid(request.getLocation())) {
            String configXml = convertor.transBucketLoction(request.getLocation());
            headers.put(Constants.CommonHeaders.CONTENT_LENGTH, String.valueOf(configXml.length()));
            RequestBody requestEntity = createRequestBody(contentType, configXml);
            result.setBody(requestEntity);
        }
        return result;
    }

    protected RestoreObjectStatus transRestoreObjectResultToRestoreObjectStatus(RestoreObjectResult result) {
        RestoreObjectStatus ret = RestoreObjectStatus.valueOf(result.getStatusCode());
        ret.setResponseHeaders(result.getResponseHeaders());
        ret.setStatusCode(result.getStatusCode());

        return ret;
    }

    protected TransResult transPutObjectRequest(PutObjectRequest request) throws ServiceException {
        Map<String, String> headers = new HashMap<String, String>();
        IHeaders iheaders = this.getIHeaders(request.getBucketName());

        ObjectMetadata objectMetadata = request.getMetadata() == null ? new ObjectMetadata() : request.getMetadata();

        setBaseHeaderFromMetadata(request.getBucketName(), headers, objectMetadata);

        if (request.getExpires() >= 0) {
            putHeader(headers, iheaders.expiresHeader(), String.valueOf(request.getExpires()));
        }

        if (request.getSuccessRedirectLocation() != null) {
            putHeader(headers, iheaders.successRedirectLocationHeader(), request.getSuccessRedirectLocation());
        }

        transRequestPaymentHeaders(request, headers, iheaders);

        transExtensionPermissions(request, headers);

        transSseHeaders(request, headers, iheaders);

        Object contentType = objectMetadata.getContentType() == null
                ? objectMetadata.getValue(CommonHeaders.CONTENT_TYPE) : objectMetadata.getContentType();
        if (contentType == null) {
            contentType = Mimetypes.getInstance().getMimetype(request.getObjectKey());
        }
        Object contentLength = objectMetadata.getContentLength();

        if (contentLength == null) {
            contentLength = objectMetadata.getValue(CommonHeaders.CONTENT_LENGTH);
        }

        long contentLengthValue = contentLength == null ? -1L : Long.parseLong(contentLength.toString());

        if (request.getFile() != null) {
            if (Mimetypes.MIMETYPE_OCTET_STREAM.equals(contentType)) {
                contentType = Mimetypes.getInstance().getMimetype(request.getFile());
            }

            long fileSize = request.getFile().length();
            try {
                request.setInput(new FileInputStream(request.getFile()));
            } catch (FileNotFoundException e) {
                throw new IllegalArgumentException("File doesnot exist");
            }

            contentLengthValue = getContentLengthFromFile(request, contentLengthValue, fileSize);
        }

        String contentTypeStr = contentType.toString().trim();
        headers.put(CommonHeaders.CONTENT_TYPE, contentTypeStr);

        if (contentLengthValue > -1) {
            this.putHeader(headers, CommonHeaders.CONTENT_LENGTH, String.valueOf(contentLengthValue));
        }

        if (request.getInput() != null && request.getProgressListener() != null) {
            ProgressManager progressManager = new SimpleProgressManager(contentLengthValue, 0,
                    request.getProgressListener(), request.getProgressInterval() > 0 ? request.getProgressInterval()
                    : ObsConstraint.DEFAULT_PROGRESS_INTERVAL);
            request.setInput(new ProgressInputStream(request.getInput(), progressManager));
        }

        RequestBody body = request.getInput() == null ? null
                : new RepeatableRequestEntity(request.getInput(), contentTypeStr, contentLengthValue,
                this.obsProperties);

        return new TransResult(headers, body);
    }

    private long getContentLengthFromFile(PutObjectRequest request, long contentLengthValue, long fileSize) {
        if (request.getOffset() > 0 && request.getOffset() < fileSize) {
            contentLengthValue = (contentLengthValue > 0
                    && contentLengthValue <= fileSize - request.getOffset())
                    ? contentLengthValue : fileSize - request.getOffset();
            try {
                long skipByte = request.getInput().skip(request.getOffset());
                if (log.isDebugEnabled()) {
                    log.debug("Skip " + skipByte + " bytes; offset : " + request.getOffset());
                }
            } catch (IOException e) {
                ServiceUtils.closeStream(request.getInput());
                throw new ServiceException(e);
            }
        } else if (contentLengthValue < 0 || contentLengthValue > fileSize) {
            contentLengthValue = fileSize;
        }
        return contentLengthValue;
    }

    private void setBaseHeaderFromMetadata(String bucketName, Map<String, String> headers,
                                           ObjectMetadata objectMetadata) {
        IConvertor iconvertor = this.getIConvertor(bucketName);
        IHeaders iheaders = this.getIHeaders(bucketName);

        selectAllowedHeader(headers, objectMetadata);

        if (ServiceUtils.isValid(objectMetadata.getContentMd5())) {
            headers.put(CommonHeaders.CONTENT_MD5, objectMetadata.getContentMd5().trim());
        }

        if (ServiceUtils.isValid(objectMetadata.getContentEncoding())) {
            headers.put(CommonHeaders.CONTENT_ENCODING, objectMetadata.getContentEncoding().trim());
        }

        if (ServiceUtils.isValid(objectMetadata.getContentDisposition())) {
            headers.put(CommonHeaders.CONTENT_DISPOSITION, objectMetadata.getContentDisposition().trim());
        }

        if (ServiceUtils.isValid(objectMetadata.getCacheControl())) {
            headers.put(CommonHeaders.CACHE_CONTROL, objectMetadata.getCacheControl().trim());
        }

        if (ServiceUtils.isValid(objectMetadata.getContentLanguage())) {
            headers.put(CommonHeaders.CONTENT_LANGUAGE, objectMetadata.getContentLanguage().trim());
        }

        if (ServiceUtils.isValid(objectMetadata.getExpires())) {
            headers.put(CommonHeaders.EXPIRES, objectMetadata.getExpires().trim());
        }

        if (ServiceUtils.isValid(objectMetadata.getCrc64())) {
            headers.put(this.getProviderCredentials().getLocalAuthType(bucketName) != AuthTypeEnum.OBS
                            ? Constants.V2_HEADER_PREFIX : Constants.OBS_HEADER_PREFIX + CommonHeaders.HASH_CRC64ECMA,
                    objectMetadata.getCrc64().trim());
        }

        if (objectMetadata.getObjectStorageClass() != null) {
            putHeader(headers, iheaders.storageClassHeader(),
                    iconvertor.transStorageClass(objectMetadata.getObjectStorageClass()));
        }

        if (objectMetadata.getWebSiteRedirectLocation() != null) {
            putHeader(headers, iheaders.websiteRedirectLocationHeader(), objectMetadata.getWebSiteRedirectLocation());
        }
    }

    protected TransResult transWriteFileRequest(WriteFileRequest request) throws ServiceException {
        TransResult result = this.transPutObjectRequest(request);
        if (request.getPosition() > 0) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(SpecialParamEnum.MODIFY.getOriginalStringCode(), "");
            params.put(ObsRequestParams.POSITION, String.valueOf(request.getPosition()));
            result.setParams(params);
        }
        return result;
    }

    protected TransResult transModifyObjectRequest(ModifyObjectRequest request) throws ServiceException {
        TransResult result = this.transPutObjectRequest(request);
        if (request.getPosition() > 0) {
            Map<String, String> params = new HashMap<String, String>();
            params.put(SpecialParamEnum.MODIFY.getOriginalStringCode(), "");
            params.put(ObsRequestParams.POSITION, String.valueOf(request.getPosition()));
            result.setParams(params);
        }
        return result;
    }

    protected TransResult transAppendObjectRequest(AppendObjectRequest request) throws ServiceException {
        TransResult result = this.transPutObjectRequest(request);
        Map<String, String> params = new HashMap<String, String>();
        params.put(SpecialParamEnum.APPEND.getOriginalStringCode(), "");
        params.put(ObsRequestParams.POSITION, String.valueOf(request.getPosition()));
        result.setParams(params);
        return result;
    }

    protected TransResult transCopyObjectRequest(CopyObjectRequest request) throws ServiceException {
        Map<String, String> headers = new HashMap<String, String>();
        IConvertor iconvertor = this.getIConvertor(request.getBucketName());
        IHeaders iheaders = this.getIHeaders(request.getBucketName());

        ObjectMetadata objectMetadata = request.getNewObjectMetadata() == null ? new ObjectMetadata()
                : request.getNewObjectMetadata();

        putHeader(headers, iheaders.metadataDirectiveHeader(),
                request.isReplaceMetadata() ? Constants.DERECTIVE_REPLACE : Constants.DERECTIVE_COPY);
        if (request.isReplaceMetadata()) {
            objectMetadata.getAllMetadata().remove(iheaders.requestIdHeader());
            objectMetadata.getAllMetadata().remove(iheaders.requestId2Header());
            for (Map.Entry<String, Object> entry : objectMetadata.getAllMetadata().entrySet()) {
                String key = entry.getKey();
                if (!ServiceUtils.isValid(key)) {
                    continue;
                }
                key = key.trim();
                if (Constants.ALLOWED_REQUEST_HTTP_HEADER_METADATA_NAMES.contains(key.toLowerCase())) {
                    continue;
                }
                headers.put(key, entry.getValue() == null ? "" : entry.getValue().toString());
            }
        }

        setBaseHeaderFromMetadata(request.getBucketName(), headers, objectMetadata);

        if (objectMetadata.getObjectStorageClass() != null) {
            putHeader(headers, iheaders.storageClassHeader(),
                    iconvertor.transStorageClass(objectMetadata.getObjectStorageClass()));
        }

        if (objectMetadata.getWebSiteRedirectLocation() != null) {
            putHeader(headers, iheaders.websiteRedirectLocationHeader(), objectMetadata.getWebSiteRedirectLocation());
        }

        if (request.getSuccessRedirectLocation() != null) {
            putHeader(headers, iheaders.successRedirectLocationHeader(), request.getSuccessRedirectLocation());
        }

        this.transRequestPaymentHeaders(request, headers, iheaders);
        this.transExtensionPermissions(request, headers);
        this.transSseHeaders(request, headers, iheaders);

        transSseCSourceHeaders(request.getSseCHeaderSource(), headers, iheaders);

        transConditionCopyHeaders(request, headers, iheaders);

        String sourceKey = RestUtils.encodeUrlString(request.getSourceBucketName()) + "/"
                + RestUtils.encodeUrlString(request.getSourceObjectKey());
        if (ServiceUtils.isValid(request.getVersionId())) {
            sourceKey += "?versionId=" + request.getVersionId().trim();
        }
        putHeader(headers, iheaders.copySourceHeader(), sourceKey);

        return new TransResult(headers);
    }

    protected void transSseCSourceHeaders(SseCHeader sseCHeader, Map<String, String> headers, IHeaders iheaders)
            throws ServiceException {
        if (sseCHeader != null) {
            String algorithm = sseCHeader.getSSEAlgorithm().getCode();
            putHeader(headers, iheaders.copySourceSseCHeader(), ServiceUtils.toValid(algorithm));
            if (sseCHeader.getSseCKeyBase64() != null) {
                try {
                    putHeader(headers, iheaders.copySourceSseCKeyHeader(), sseCHeader.getSseCKeyBase64());
                    putHeader(headers, iheaders.copySourceSseCKeyMd5Header(), ServiceUtils.toBase64(
                            ServiceUtils.computeMD5Hash(ServiceUtils.fromBase64(sseCHeader.getSseCKeyBase64()))));
                } catch (IOException e) {
                    throw new IllegalStateException("fail to read sseCkey", e);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("fail to read sseCkey", e);
                }
            } else if (null != sseCHeader.getSseCKey()
                    && sseCHeader.getSseCKey().length > 0) {
                try {
                    byte[] data = sseCHeader.getSseCKey();
                    putHeader(headers, iheaders.copySourceSseCKeyHeader(), ServiceUtils.toBase64(data));
                    putHeader(headers, iheaders.copySourceSseCKeyMd5Header(),
                            ServiceUtils.toBase64(ServiceUtils.computeMD5Hash(data)));
                } catch (IOException e) {
                    throw new IllegalStateException("fail to read sseCkey", e);
                } catch (NoSuchAlgorithmException e) {
                    throw new IllegalStateException("fail to read sseCkey", e);
                }
            }
        }
    }

    protected void transConditionCopyHeaders(CopyObjectRequest request, Map<String, String> headers,
                                             IHeaders iheaders) {
        if (request.getIfModifiedSince() != null) {
            putHeader(headers, iheaders.copySourceIfModifiedSinceHeader(),
                    ServiceUtils.formatRfc822Date(request.getIfModifiedSince()));
        }
        if (request.getIfUnmodifiedSince() != null) {
            putHeader(headers, iheaders.copySourceIfUnmodifiedSinceHeader(),
                    ServiceUtils.formatRfc822Date(request.getIfUnmodifiedSince()));
        }
        if (ServiceUtils.isValid(request.getIfMatchTag())) {
            putHeader(headers, iheaders.copySourceIfMatchHeader(), request.getIfMatchTag().trim());
        }
        if (ServiceUtils.isValid(request.getIfNoneMatchTag())) {
            putHeader(headers, iheaders.copySourceIfNoneMatchHeader(), request.getIfNoneMatchTag().trim());
        }
    }

    protected TransResult transGetObjectRequest(GetObjectRequest request) throws ServiceException {
        Map<String, String> headers = new HashMap<String, String>();
        this.transSseCHeaders(request.getSseCHeader(), headers, this.getIHeaders(request.getBucketName()));
        this.transConditionGetObjectHeaders(request, headers);

        this.transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));
        transRangeHeader(request, headers);

        Map<String, String> params = new HashMap<String, String>();
        this.transGetObjectParams(request, params);

        return new TransResult(headers, params, null);
    }

    /**
     *
     * @param request
     * @param headers
     */
    protected void transRangeHeader(GetObjectRequest request, Map<String, String> headers) {
        String start = "";
        String end = "";

        if (null != request.getRangeStart()) {
            ServiceUtils.assertParameterNotNegative(request.getRangeStart().longValue(),
                    "start range should not be negative.");
            start = String.valueOf(request.getRangeStart());
        }

        if (null != request.getRangeEnd()) {
            ServiceUtils.assertParameterNotNegative(request.getRangeEnd().longValue(),
                    "end range should not be negative.");
            end = String.valueOf(request.getRangeEnd());
        }

        if (null != request.getRangeStart() && null != request.getRangeEnd()) {
            if (request.getRangeStart().longValue() > request.getRangeEnd().longValue()) {
                throw new IllegalArgumentException("start must be less than end.");
            }
        }

        if (!"".equals(start) || !"".equals(end)) {
            String range = String.format("bytes=%s-%s", start, end);
            headers.put(CommonHeaders.RANGE, range);
        }
    }

    protected void transGetObjectParams(GetObjectRequest request, Map<String, String> params) {
        if (null != request.getReplaceMetadata()) {
            if (ServiceUtils.isValid(request.getReplaceMetadata().getCacheControl())) {
                params.put(ObsRequestParams.RESPONSE_CACHE_CONTROL, request.getReplaceMetadata().getCacheControl());
            }
            if (ServiceUtils.isValid(request.getReplaceMetadata().getContentDisposition())) {
                params.put(ObsRequestParams.RESPONSE_CONTENT_DISPOSITION,
                        request.getReplaceMetadata().getContentDisposition());
            }
            if (ServiceUtils.isValid(request.getReplaceMetadata().getContentEncoding())) {
                params.put(ObsRequestParams.RESPONSE_CONTENT_ENCODING,
                        request.getReplaceMetadata().getContentEncoding());
            }
            if (ServiceUtils.isValid(request.getReplaceMetadata().getContentLanguage())) {
                params.put(ObsRequestParams.RESPONSE_CONTENT_LANGUAGE,
                        request.getReplaceMetadata().getContentLanguage());
            }
            if (ServiceUtils.isValid(request.getReplaceMetadata().getContentType())) {
                params.put(ObsRequestParams.RESPONSE_CONTENT_TYPE, request.getReplaceMetadata().getContentType());
            }
            if (ServiceUtils.isValid(request.getReplaceMetadata().getExpires())) {
                params.put(ObsRequestParams.RESPONSE_EXPIRES, request.getReplaceMetadata().getExpires());
            }
        }
        if (ServiceUtils.isValid(request.getImageProcess())) {
            params.put(ObsRequestParams.X_IMAGE_PROCESS, request.getImageProcess());
        }
        if (request.getVersionId() != null) {
            params.put(ObsRequestParams.VERSION_ID, request.getVersionId());
        }
        if (request.getCacheOption() != null) {
            String cacheControl = request.getCacheOption().getCode() + ", ttl=" + request.getTtl();
            params.put(ObsRequestParams.X_CACHE_CONTROL, cacheControl);
        }
    }

    protected void transConditionGetObjectHeaders(GetObjectRequest request, Map<String, String> headers) {
        if (request.getIfModifiedSince() != null) {
            headers.put(CommonHeaders.IF_MODIFIED_SINCE, ServiceUtils.formatRfc822Date(request.getIfModifiedSince()));
        }
        if (request.getIfUnmodifiedSince() != null) {
            headers.put(CommonHeaders.IF_UNMODIFIED_SINCE,
                    ServiceUtils.formatRfc822Date(request.getIfUnmodifiedSince()));
        }
        if (ServiceUtils.isValid(request.getIfMatchTag())) {
            headers.put(CommonHeaders.IF_MATCH, request.getIfMatchTag().trim());
        }
        if (ServiceUtils.isValid(request.getIfNoneMatchTag())) {
            headers.put(CommonHeaders.IF_NONE_MATCH, request.getIfNoneMatchTag().trim());
        }
        if (!request.isAutoUnzipResponse()) {
            headers.put(CommonHeaders.ACCETP_ENCODING, "identity");
        }
    }

    protected TransResult transSetObjectMetadataRequest(SetObjectMetadataRequest request) throws ServiceException {
        Map<String, String> headers = new HashMap<String, String>();
        IHeaders iheaders = this.getIHeaders(request.getBucketName());
        IConvertor iconvertor = this.getIConvertor(request.getBucketName());

        for (Map.Entry<String, String> entry : request.getAllUserMetadata().entrySet()) {
            String key = entry.getKey();
            if (!ServiceUtils.isValid(key)) {
                continue;
            }
            key = key.trim();
            headers.put(key, entry.getValue() == null ? "" : entry.getValue());
        }

        if (request.getObjectStorageClass() != null) {
            putHeader(headers, iheaders.storageClassHeader(),
                    iconvertor.transStorageClass(request.getObjectStorageClass()));
        }

        if (request.getWebSiteRedirectLocation() != null) {
            putHeader(headers, iheaders.websiteRedirectLocationHeader(), request.getWebSiteRedirectLocation());
        }

        if (request.getContentDisposition() != null) {
            putHeader(headers, Constants.CommonHeaders.CONTENT_DISPOSITION, request.getContentDisposition());
        }

        if (request.getContentEncoding() != null) {
            putHeader(headers, Constants.CommonHeaders.CONTENT_ENCODING, request.getContentEncoding());
        }

        if (request.getContentLanguage() != null) {
            putHeader(headers, Constants.CommonHeaders.CONTENT_LANGUAGE, request.getContentLanguage());
        }

        if (request.getContentType() != null) {
            putHeader(headers, Constants.CommonHeaders.CONTENT_TYPE, request.getContentType());
        }

        if (request.getCacheControl() != null) {
            putHeader(headers, Constants.CommonHeaders.CACHE_CONTROL, request.getCacheControl());
        }

        if (request.getExpires() != null) {
            putHeader(headers, Constants.CommonHeaders.EXPIRES, request.getExpires());
        }

        this.transRequestPaymentHeaders(request, headers, iheaders);
        putHeader(headers, iheaders.metadataDirectiveHeader(),
                request.isRemoveUnset() ? Constants.DERECTIVE_REPLACE : Constants.DERECTIVE_REPLACE_NEW);

        Map<String, String> params = new HashMap<String, String>();
        params.put(SpecialParamEnum.METADATA.getOriginalStringCode(), "");
        if (request.getVersionId() != null) {
            params.put(ObsRequestParams.VERSION_ID, request.getVersionId());
        }

        return new TransResult(headers, params, null);
    }

    protected TransResult transCopyPartRequest(CopyPartRequest request) throws ServiceException {
        Map<String, String> params = new HashMap<String, String>();
        params.put(ObsRequestParams.PART_NUMBER, String.valueOf(request.getPartNumber()));
        params.put(ObsRequestParams.UPLOAD_ID, request.getUploadId());

        Map<String, String> headers = new HashMap<String, String>();
        IHeaders iheaders = this.getIHeaders(request.getBucketName());

        String sourceKey = RestUtils.encodeUrlString(request.getSourceBucketName()) + "/"
                + RestUtils.encodeUrlString(request.getSourceObjectKey());
        if (ServiceUtils.isValid(request.getVersionId())) {
            sourceKey += "?versionId=" + request.getVersionId().trim();
        }
        putHeader(headers, iheaders.copySourceHeader(), sourceKey);

        if (request.getByteRangeStart() != null) {
            String rangeEnd = request.getByteRangeEnd() != null ? String.valueOf(request.getByteRangeEnd()) : "";
            String range = String.format("bytes=%s-%s", request.getByteRangeStart(), rangeEnd);
            putHeader(headers, iheaders.copySourceRangeHeader(), range);
        }

        this.transRequestPaymentHeaders(request, headers, iheaders);
        this.transSseCHeaders(request.getSseCHeaderDestination(), headers, iheaders);
        this.transSseCSourceHeaders(request.getSseCHeaderSource(), headers, iheaders);

        return new TransResult(headers, params, null);
    }

    protected TransResult transListObjectsRequest(ListObjectsRequest listObjectsRequest) {
        Map<String, String> params = new HashMap<String, String>();
        if (listObjectsRequest.getPrefix() != null) {
            params.put(ObsRequestParams.PREFIX, listObjectsRequest.getPrefix());
        }
        if (listObjectsRequest.getDelimiter() != null) {
            params.put(ObsRequestParams.DELIMITER, listObjectsRequest.getDelimiter());
        }
        if (listObjectsRequest.getMaxKeys() > 0) {
            params.put(ObsRequestParams.MAX_KEYS, String.valueOf(listObjectsRequest.getMaxKeys()));
        }
        if (listObjectsRequest.getMarker() != null) {
            params.put(ObsRequestParams.MARKER, listObjectsRequest.getMarker());
        }
        if (listObjectsRequest.getEncodingType() != null) {
            params.put(ObsRequestParams.ENCODING_TYPE, listObjectsRequest.getEncodingType());
        }

        Map<String, String> headers = new HashMap<String, String>();
        transRequestPaymentHeaders(listObjectsRequest, headers, this.getIHeaders(listObjectsRequest.getBucketName()));
        if (listObjectsRequest.getListTimeout() > 0) {
            putHeader(headers, this.getIHeaders(listObjectsRequest.getBucketName()).listTimeoutHeader(),
                    String.valueOf(listObjectsRequest.getListTimeout()));
        }

        return new TransResult(headers, params, null);
    }

    protected TransResult transListContentSummaryRequest(ListContentSummaryRequest listContentSummaryRequest) {
        Map<String, String> params = new HashMap<String, String>();
        if (listContentSummaryRequest.getPrefix() != null) {
            params.put(ObsRequestParams.PREFIX, listContentSummaryRequest.getPrefix());
        }
        if (listContentSummaryRequest.getDelimiter() != null) {
            params.put(ObsRequestParams.DELIMITER, listContentSummaryRequest.getDelimiter());
        }
        if (listContentSummaryRequest.getMaxKeys() > 0) {
            params.put(ObsRequestParams.MAX_KEYS, String.valueOf(listContentSummaryRequest.getMaxKeys()));
        }

        if (listContentSummaryRequest.getMarker() != null) {
            params.put(ObsRequestParams.MARKER, listContentSummaryRequest.getMarker());
        }

        params.put(SpecialParamEnum.LISTCONTENTSUMMARY.getOriginalStringCode(), "");

        Map<String, String> headers = new HashMap<String, String>();
        transRequestPaymentHeaders(listContentSummaryRequest, headers,
                this.getIHeaders(listContentSummaryRequest.getBucketName()));
        if (listContentSummaryRequest.getListTimeout() > 0) {
            putHeader(headers, this.getIHeaders(listContentSummaryRequest.getBucketName()).listTimeoutHeader(),
                    String.valueOf(listContentSummaryRequest.getListTimeout()));
        }
        return new TransResult(headers, params, null);
    }

    protected TransResult transListContentSummaryFsRequest(ListContentSummaryFsRequest listContentSummaryFsRequest) {
        Map<String, String> params = new HashMap<>();
        params.put(SpecialParamEnum.LISTCONTENTSUMMARYFS.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<String, String>();
        String sbXml = "";
        try {
            OBSXMLBuilder builder = OBSXMLBuilder.create("MultiListContentSummary");
            builder.elem("MaxKeys").text(String.valueOf(listContentSummaryFsRequest.getMaxKeys()));
            if (listContentSummaryFsRequest.getDirLayers().size() > 0) {
                for (ListContentSummaryFsRequest.DirLayer dir : listContentSummaryFsRequest.getDirLayers()) {
                    OBSXMLBuilder builderDir = OBSXMLBuilder.create("Dir");
                    builderDir.elem("Key").text(dir.getKey());
                    if (ServiceUtils.isValid(dir.getMarker())) {
                        builderDir.elem("Marker").text(dir.getMarker());
                    }
                    if (dir.getInode() != 0) {
                        builderDir.elem("Inode").text(String.valueOf(dir.getInode()));
                    }
                    builder.importXMLBuilder(builderDir);
                }
            }
            sbXml = builder.asString();
        } catch (ParserConfigurationException | FactoryConfigurationError | TransformerException e) {
            throw new ServiceException("Failed to build XML document for MultiListContentSummary", e);
        }

        putHeader(headers, (this.getProviderCredentials().getLocalAuthType(listContentSummaryFsRequest.getBucketName())
                        != AuthTypeEnum.OBS ? Constants.V2_HEADER_PREFIX : Constants.OBS_HEADER_PREFIX)
                        + Constants.FS_SUMMARY_DIR_LIST,
                ServiceUtils.toBase64(sbXml.getBytes(StandardCharsets.UTF_8)));
        transRequestPaymentHeaders(listContentSummaryFsRequest, headers,
                this.getIHeaders(listContentSummaryFsRequest.getBucketName()));
        return new TransResult(headers, params, null);
    }

    protected TransResult transGetContentSummaryFs(ContentSummaryFsRequest contentSummaryFsRequest) {
        Map<String, String> params = new HashMap<>();
        params.put(SpecialParamEnum.GETCONTENTSUMMARY.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();
        transRequestPaymentHeaders(contentSummaryFsRequest, headers,
                this.getIHeaders(contentSummaryFsRequest.getBucketName()));
        return new TransResult(headers, params, null);
    }

    protected TransResult transUploadPartRequest(UploadPartRequest request) throws ServiceException {
        Map<String, String> params = new HashMap<String, String>();
        params.put(ObsRequestParams.PART_NUMBER, String.valueOf(request.getPartNumber()));
        params.put(ObsRequestParams.UPLOAD_ID, request.getUploadId());

        Map<String, String> headers = new HashMap<String, String>();
        IHeaders iheaders = this.getIHeaders(request.getBucketName());

        if (ServiceUtils.isValid(request.getContentMd5())) {
            headers.put(CommonHeaders.CONTENT_MD5, request.getContentMd5().trim());
        }

        this.transRequestPaymentHeaders(request, headers, iheaders);
        this.transSseCHeaders(request.getSseCHeader(), headers, iheaders);

        long contentLength = -1L;
        if (null != request.getFile()) {
            long fileSize = request.getFile().length();
            long offset = (request.getOffset() >= 0 && request.getOffset() < fileSize) ? request.getOffset() : 0;
            long partSize = (request.getPartSize() != null && request.getPartSize() > 0
                    && request.getPartSize() <= (fileSize - offset)) ? request.getPartSize() : fileSize - offset;
            contentLength = partSize;

            try {
                if (request.isAttachMd5() && !ServiceUtils.isValid(request.getContentMd5())) {
                    headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.toBase64(
                            ServiceUtils.computeMD5Hash(new FileInputStream(request.getFile()), partSize, offset)));
                }
                request.setInput(new FileInputStream(request.getFile()));
                long skipByte = request.getInput().skip(offset);
                if (log.isDebugEnabled()) {
                    log.debug("Skip " + skipByte + " bytes; offset : " + offset);
                }
            } catch (Exception e) {
                ServiceUtils.closeStream(request.getInput());
                throw new ServiceException(e);
            }
        } else if (null != request.getInput()) {
            if (request.getPartSize() != null && request.getPartSize() > 0) {
                contentLength = request.getPartSize();
            }
        }

        if (request.getInput() != null && request.getProgressListener() != null) {
            ProgressManager progressManager = new SimpleProgressManager(contentLength, 0, request.getProgressListener(),
                    request.getProgressInterval() > 0 ? request.getProgressInterval()
                            : ObsConstraint.DEFAULT_PROGRESS_INTERVAL);
            request.setInput(new ProgressInputStream(request.getInput(), progressManager));
        }

        String contentType = Mimetypes.getInstance().getMimetype(request.getObjectKey());
        headers.put(CommonHeaders.CONTENT_TYPE, contentType);

        if (contentLength > -1) {
            this.putHeader(headers, CommonHeaders.CONTENT_LENGTH, String.valueOf(contentLength));
        }
        RequestBody body = request.getInput() == null ? null
                : new RepeatableRequestEntity(request.getInput(), contentType, contentLength, this.obsProperties);
        return new TransResult(headers, params, body);
    }

    protected void transSseKmsHeaders(SseKmsHeader kmsHeader, Map<String, String> headers, IHeaders iheaders,
                                      String bucketName) {
        if (kmsHeader == null) {
            return;
        }

        String sseKmsEncryption = this.getProviderCredentials().getLocalAuthType(bucketName) != AuthTypeEnum.OBS
                ? "aws:" + kmsHeader.getSSEAlgorithm().getCode() : kmsHeader.getSSEAlgorithm().getCode();
        putHeader(headers, iheaders.sseKmsHeader(), ServiceUtils.toValid(sseKmsEncryption));
        if (ServiceUtils.isValid(kmsHeader.getKmsKeyId())) {
            putHeader(headers, iheaders.sseKmsKeyHeader(), kmsHeader.getKmsKeyId());
        }

        if (ServiceUtils.isValid(kmsHeader.getProjectId())) {
            putHeader(headers, iheaders.sseKmsProjectIdHeader(), kmsHeader.getProjectId());
        }
    }
}
