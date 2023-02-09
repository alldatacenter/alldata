/*
 * Copyright 2010-2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.

 * According to cos feature, we modify some class，comment, field name, etc.
 */


package com.qcloud.cos;

import static com.qcloud.cos.internal.LengthCheckInputStream.EXCLUDE_SKIPPED_BYTES;
import static com.qcloud.cos.internal.LengthCheckInputStream.INCLUDE_SKIPPED_BYTES;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSCredentialsProvider;
import com.qcloud.cos.auth.COSSessionCredentials;
import com.qcloud.cos.auth.COSSigner;
import com.qcloud.cos.auth.COSStaticCredentialsProvider;
import com.qcloud.cos.endpoint.CIRegionEndpointBuilder;
import com.qcloud.cos.endpoint.EndpointBuilder;
import com.qcloud.cos.endpoint.RegionEndpointBuilder;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.exception.CosServiceException.ErrorType;
import com.qcloud.cos.exception.MultiObjectDeleteException;
import com.qcloud.cos.exception.Throwables;
import com.qcloud.cos.http.CosHttpClient;
import com.qcloud.cos.http.CosHttpRequest;
import com.qcloud.cos.http.DefaultCosHttpClient;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.http.HttpResponseHandler;
import com.qcloud.cos.internal.BucketNameUtils;
import com.qcloud.cos.internal.CIServiceRequest;
import com.qcloud.cos.internal.CIWorkflowServiceRequest;
import com.qcloud.cos.internal.COSDefaultAclHeaderHandler;
import com.qcloud.cos.internal.COSObjectResponseHandler;
import com.qcloud.cos.internal.COSStringResponseHandler;
import com.qcloud.cos.internal.COSVersionHeaderHandler;
import com.qcloud.cos.internal.COSXmlResponseHandler;
import com.qcloud.cos.internal.Constants;
import com.qcloud.cos.internal.CosMetadataResponseHandler;
import com.qcloud.cos.internal.CosServiceRequest;
import com.qcloud.cos.internal.CosServiceResponse;
import com.qcloud.cos.internal.DeleteObjectTaggingHeaderHandler;
import com.qcloud.cos.internal.DeleteObjectsResponse;
import com.qcloud.cos.internal.DigestValidationInputStream;
import com.qcloud.cos.internal.GetObjectTaggingResponseHeaderHandler;
import com.qcloud.cos.internal.InputSubstream;
import com.qcloud.cos.internal.LengthCheckInputStream;
import com.qcloud.cos.internal.MD5DigestCalculatingInputStream;
import com.qcloud.cos.internal.MultiObjectDeleteXmlFactory;
import com.qcloud.cos.internal.ObjectExpirationHeaderHandler;
import com.qcloud.cos.internal.ReleasableInputStream;
import com.qcloud.cos.internal.RequestXmlFactory;
import com.qcloud.cos.internal.ResettableInputStream;
import com.qcloud.cos.internal.ResponseHeaderHandlerChain;
import com.qcloud.cos.internal.SdkFilterInputStream;
import com.qcloud.cos.internal.ServerSideEncryptionHeaderHandler;
import com.qcloud.cos.internal.ServiceClientHolderInputStream;
import com.qcloud.cos.internal.SetObjectTaggingResponseHeaderHandler;
import com.qcloud.cos.internal.SkipMd5CheckStrategy;
import com.qcloud.cos.internal.Unmarshaller;
import com.qcloud.cos.internal.Unmarshallers;
import com.qcloud.cos.internal.VIDResultHandler;
import com.qcloud.cos.internal.VoidCosResponseHandler;
import com.qcloud.cos.internal.XmlResponsesSaxParser.CompleteMultipartUploadHandler;
import com.qcloud.cos.internal.XmlResponsesSaxParser.CopyObjectResultHandler;
import com.qcloud.cos.model.*;
import com.qcloud.cos.model.ciModel.auditing.AudioAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.AudioAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.BatchImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.DocumentAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.ImageAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.TextAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.VideoAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.VideoAuditingResponse;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingRequest;
import com.qcloud.cos.model.ciModel.auditing.WebpageAuditingResponse;
import com.qcloud.cos.model.ciModel.bucket.DocBucketRequest;
import com.qcloud.cos.model.ciModel.bucket.DocBucketResponse;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketRequest;
import com.qcloud.cos.model.ciModel.bucket.MediaBucketResponse;
import com.qcloud.cos.model.ciModel.common.ImageProcessRequest;
import com.qcloud.cos.model.ciModel.common.MediaOutputObject;
import com.qcloud.cos.model.ciModel.image.ImageLabelRequest;
import com.qcloud.cos.model.ciModel.image.ImageLabelResponse;
import com.qcloud.cos.model.ciModel.image.ImageLabelV2Request;
import com.qcloud.cos.model.ciModel.image.ImageLabelV2Response;
import com.qcloud.cos.model.ciModel.job.DocHtmlRequest;
import com.qcloud.cos.model.ciModel.job.DocJobListRequest;
import com.qcloud.cos.model.ciModel.job.DocJobListResponse;
import com.qcloud.cos.model.ciModel.job.DocJobRequest;
import com.qcloud.cos.model.ciModel.job.DocJobResponse;
import com.qcloud.cos.model.ciModel.job.MediaJobObject;
import com.qcloud.cos.model.ciModel.job.MediaJobResponse;
import com.qcloud.cos.model.ciModel.job.MediaJobsRequest;
import com.qcloud.cos.model.ciModel.job.MediaListJobResponse;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoRequest;
import com.qcloud.cos.model.ciModel.mediaInfo.MediaInfoResponse;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;
import com.qcloud.cos.model.ciModel.queue.DocListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.DocQueueRequest;
import com.qcloud.cos.model.ciModel.queue.MediaListQueueResponse;
import com.qcloud.cos.model.ciModel.queue.MediaQueueRequest;
import com.qcloud.cos.model.ciModel.queue.MediaQueueResponse;
import com.qcloud.cos.model.ciModel.snapshot.SnapshotRequest;
import com.qcloud.cos.model.ciModel.snapshot.SnapshotResponse;
import com.qcloud.cos.model.ciModel.template.MediaListTemplateResponse;
import com.qcloud.cos.model.ciModel.template.MediaTemplateRequest;
import com.qcloud.cos.model.ciModel.template.MediaTemplateResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowExecutionResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowExecutionsResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowListRequest;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowListResponse;
import com.qcloud.cos.model.ciModel.workflow.MediaWorkflowRequest;
import com.qcloud.cos.model.inventory.InventoryConfiguration;
import com.qcloud.cos.model.transform.ObjectTaggingXmlFactory;
import com.qcloud.cos.region.Region;
import com.qcloud.cos.utils.Base64;
import com.qcloud.cos.utils.BinaryUtils;
import com.qcloud.cos.utils.DateUtils;
import com.qcloud.cos.utils.Jackson;
import com.qcloud.cos.utils.Md5Utils;
import com.qcloud.cos.utils.ServiceUtils;
import com.qcloud.cos.utils.StringUtils;
import com.qcloud.cos.utils.UrlEncoderUtils;

import org.apache.commons.codec.DecoderException;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class COSClient implements COS {

    private static final Logger log = LoggerFactory.getLogger(COSClient.class);

    private final SkipMd5CheckStrategy skipMd5CheckStrategy = SkipMd5CheckStrategy.INSTANCE;
    private final VoidCosResponseHandler voidCosResponseHandler = new VoidCosResponseHandler();

    private volatile COSCredentialsProvider credProvider;

    protected ClientConfig clientConfig;

    private CosHttpClient cosHttpClient;

    public COSClient(COSCredentials cred, ClientConfig clientConfig) {
        this(new COSStaticCredentialsProvider(cred), clientConfig);
    }

    public COSClient(COSCredentialsProvider credProvider, ClientConfig clientConfig) {
        super();
        this.credProvider = credProvider;
        this.clientConfig = clientConfig;
        this.cosHttpClient = new DefaultCosHttpClient(clientConfig);
    }

    public void shutdown() {
        this.cosHttpClient.shutdown();
    }

    public void setCOSCredentials(COSCredentials cred) {
        rejectNull(cred, "cred must not be null");
        this.credProvider = new COSStaticCredentialsProvider(cred);
    }

    public void setCOSCredentialsProvider(COSCredentialsProvider credProvider) {
        rejectNull(credProvider, "credProvider must not be null");
        this.credProvider = credProvider;
    }


    @Override
    public ClientConfig getClientConfig() {
        return clientConfig;
    }

    private COSCredentials fetchCredential() throws CosClientException {
        if (credProvider == null) {
            throw new CosClientException(
                    "credentials Provider is null, you must set legal Credentials info when init cosClient.");
        }

        COSCredentials cred = credProvider.getCredentials();
        if (cred == null) {
            throw new CosClientException(
                    "credentials from Provider is null. please check your credentials provider");
        }
        return cred;
    }

    /**
     * <p>
     * Asserts that the specified parameter value is not <code>null</code> and if it is, throws an
     * <code>IllegalArgumentException</code> with the specified error message.
     * </p>
     *
     * @param parameterValue The parameter value being checked.
     * @param errorMessage The error message to include in the IllegalArgumentException if the
     *        specified parameter is null.
     */
    private void rejectNull(Object parameterValue, String errorMessage) {
        if (parameterValue == null)
            throw new IllegalArgumentException(errorMessage);
    }

    private void rejectEmpty(String parameterValue, String errorMessage) {
        if (parameterValue.isEmpty())
            throw new IllegalArgumentException(errorMessage);
    }

    private void rejectEmpty(Map parameterValue, String errorMessage) {
        if (parameterValue.isEmpty())
            throw new IllegalArgumentException(errorMessage);
    }

    private void rejectStartWith(String value, String startStr, String errorMessage) {
        if (value != null && !value.isEmpty() && startStr != null) {
            if (!value.startsWith(startStr))
                throw new IllegalArgumentException(errorMessage);
        }
    }

    protected <X extends CosServiceRequest> CosHttpRequest<X> createRequest(String bucketName,
            String key, X originalRequest, HttpMethodName httpMethod) {
        CosHttpRequest<X> httpRequest = new CosHttpRequest<X>(originalRequest);
        httpRequest.setHttpMethod(httpMethod);
        httpRequest.addHeader(Headers.USER_AGENT, clientConfig.getUserAgent());
        if (originalRequest instanceof ListBucketsRequest) {
            buildUrlAndHost(httpRequest, bucketName, key, true);
        } else {
            rejectNull(clientConfig.getRegion(),
                    "region is missing, you must set region when init clientConfig for the api.");
            buildUrlAndHost(httpRequest, bucketName, key, false);
        }
        httpRequest.setProgressListener(originalRequest.getGeneralProgressListener());
        return httpRequest;
    }

    private void addAclHeaders(CosHttpRequest<? extends CosServiceRequest> request,
            AccessControlList acl) {
        List<Grant> grants = acl.getGrantsAsList();
        Map<Permission, Collection<Grantee>> grantsByPermission =
                new HashMap<Permission, Collection<Grantee>>();
        for (Grant grant : grants) {
            if (!grantsByPermission.containsKey(grant.getPermission())) {
                grantsByPermission.put(grant.getPermission(), new LinkedList<Grantee>());
            }
            grantsByPermission.get(grant.getPermission()).add(grant.getGrantee());
        }
        for (Permission permission : Permission.values()) {
            if (grantsByPermission.containsKey(permission)) {
                Collection<Grantee> grantees = grantsByPermission.get(permission);
                boolean seenOne = false;
                StringBuilder granteeString = new StringBuilder();
                for (Grantee grantee : grantees) {
                    if (!seenOne)
                        seenOne = true;
                    else
                        granteeString.append(", ");
                    granteeString.append(grantee.getTypeIdentifier()).append("=").append("\"")
                            .append(grantee.getIdentifier()).append("\"");
                }
                request.addHeader(permission.getHeaderName(), granteeString.toString());
            }
        }
    }

    /**
     * <p>
     * Populates the specified request object with the appropriate headers from the
     * {@link ObjectMetadata} object.
     * </p>
     *
     * @param request The request to populate with headers.
     * @param metadata The metadata containing the header information to include in the request.
     */
    protected static void populateRequestMetadata(CosHttpRequest<?> request,
            ObjectMetadata metadata) {
        Map<String, Object> rawMetadata = metadata.getRawMetadata();
        if (rawMetadata != null) {
            for (Entry<String, Object> entry : rawMetadata.entrySet()) {
                request.addHeader(entry.getKey(), entry.getValue().toString());
            }
        }

        Date httpExpiresDate = metadata.getHttpExpiresDate();
        if (httpExpiresDate != null) {
            request.addHeader(Headers.EXPIRES, DateUtils.formatRFC822Date(httpExpiresDate));
        }

        Map<String, String> userMetadata = metadata.getUserMetadata();
        if (userMetadata != null) {
            for (Entry<String, String> entry : userMetadata.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null)
                    key = key.trim();
                if (value != null)
                    value = value.trim();
                request.addHeader(Headers.COS_USER_METADATA_PREFIX + key, value);
            }
        }
    }

    private void populateRequestWithCopyObjectParameters(
            CosHttpRequest<? extends CosServiceRequest> request,
            CopyObjectRequest copyObjectRequest) {
        Region sourceRegion = copyObjectRequest.getSourceBucketRegion();
        EndpointBuilder srcEndpointBuilder = null;
        // 如果用户没有设置源region, 则默认和目的region一致
        if (sourceRegion == null) {
            srcEndpointBuilder = this.clientConfig.getEndpointBuilder();
        } else {
            srcEndpointBuilder = new RegionEndpointBuilder(sourceRegion);
        }
        if (copyObjectRequest.getSourceEndpointBuilder() != null) {
            srcEndpointBuilder = copyObjectRequest.getSourceEndpointBuilder();
        }

        String sourceKey = formatKey(copyObjectRequest.getSourceKey());
        String sourceBucket =
                formatBucket(copyObjectRequest.getSourceBucketName(),
                        (copyObjectRequest.getSourceAppid() != null)
                                ? copyObjectRequest.getSourceAppid()
                                : fetchCredential().getCOSAppId());
        String copySourceHeader =
                String.format("%s%s", srcEndpointBuilder.buildGeneralApiEndpoint(sourceBucket),
                        UrlEncoderUtils.encodeEscapeDelimiter(sourceKey));
        if (copyObjectRequest.getSourceVersionId() != null) {
            copySourceHeader += "?versionId=" + copyObjectRequest.getSourceVersionId();
        }
        request.addHeader("x-cos-copy-source", copySourceHeader);

        addDateHeader(request, Headers.COPY_SOURCE_IF_MODIFIED_SINCE,
                copyObjectRequest.getModifiedSinceConstraint());
        addDateHeader(request, Headers.COPY_SOURCE_IF_UNMODIFIED_SINCE,
                copyObjectRequest.getUnmodifiedSinceConstraint());

        addStringListHeader(request, Headers.COPY_SOURCE_IF_MATCH,
                copyObjectRequest.getMatchingETagConstraints());
        addStringListHeader(request, Headers.COPY_SOURCE_IF_NO_MATCH,
                copyObjectRequest.getNonmatchingETagConstraints());

        if (copyObjectRequest.getAccessControlList() != null) {
            addAclHeaders(request, copyObjectRequest.getAccessControlList());
        } else if (copyObjectRequest.getCannedAccessControlList() != null) {
            request.addHeader(Headers.COS_CANNED_ACL,
                    copyObjectRequest.getCannedAccessControlList().toString());
        }

        if (copyObjectRequest.getStorageClass() != null) {
            request.addHeader(Headers.STORAGE_CLASS, copyObjectRequest.getStorageClass());
        }

        if (copyObjectRequest.getRedirectLocation() != null) {
            request.addHeader(Headers.REDIRECT_LOCATION, copyObjectRequest.getRedirectLocation());
        }

        ObjectMetadata newObjectMetadata = copyObjectRequest.getNewObjectMetadata();

        if(copyObjectRequest.getMetadataDirective() != null) {
            request.addHeader(Headers.METADATA_DIRECTIVE, copyObjectRequest.getMetadataDirective());
        } else if (newObjectMetadata != null) {
            request.addHeader(Headers.METADATA_DIRECTIVE, "REPLACE");
        }

        if(newObjectMetadata != null) {
            populateRequestMetadata(request, newObjectMetadata);
        }

        // Populate the SSE-C parameters for the source and destination object
        populateSSE_C(request, copyObjectRequest.getDestinationSSECustomerKey());
        populateSourceSSE_C(request, copyObjectRequest.getSourceSSECustomerKey());
        populateSSE_KMS(request, copyObjectRequest.getSSECOSKeyManagementParams());
    }

    private void populateRequestWithCopyPartParameters(
            CosHttpRequest<? extends CosServiceRequest> request, CopyPartRequest copyPartRequest) {
        Region sourceRegion = copyPartRequest.getSourceBucketRegion();
        EndpointBuilder srcEndpointBuilder = null;
        // 如果用户没有设置源region, 则默认和目的region一致
        if (sourceRegion == null) {
            srcEndpointBuilder = this.clientConfig.getEndpointBuilder();
        } else {
            srcEndpointBuilder = new RegionEndpointBuilder(sourceRegion);
        }
        if (copyPartRequest.getSourceEndpointBuilder() != null) {
            srcEndpointBuilder = copyPartRequest.getSourceEndpointBuilder();
        }
        String sourceKey = formatKey(copyPartRequest.getSourceKey());

        String sourceBucket =
                formatBucket(copyPartRequest.getSourceBucketName(),
                        (copyPartRequest.getSourceAppid() != null)
                                ? copyPartRequest.getSourceAppid()
                                : fetchCredential().getCOSAppId());
        String copySourceHeader =
                String.format("%s%s", srcEndpointBuilder.buildGeneralApiEndpoint(sourceBucket),
                        UrlEncoderUtils.encodeEscapeDelimiter(sourceKey));
        if (copyPartRequest.getSourceVersionId() != null) {
            copySourceHeader += "?versionId=" + copyPartRequest.getSourceVersionId();
        }
        request.addHeader("x-cos-copy-source", copySourceHeader);

        addDateHeader(request, Headers.COPY_SOURCE_IF_MODIFIED_SINCE,
                copyPartRequest.getModifiedSinceConstraint());
        addDateHeader(request, Headers.COPY_SOURCE_IF_UNMODIFIED_SINCE,
                copyPartRequest.getUnmodifiedSinceConstraint());

        addStringListHeader(request, Headers.COPY_SOURCE_IF_MATCH,
                copyPartRequest.getMatchingETagConstraints());
        addStringListHeader(request, Headers.COPY_SOURCE_IF_NO_MATCH,
                copyPartRequest.getNonmatchingETagConstraints());

        if (copyPartRequest.getFirstByte() != null && copyPartRequest.getLastByte() != null) {
            String range =
                    "bytes=" + copyPartRequest.getFirstByte() + "-" + copyPartRequest.getLastByte();
            request.addHeader(Headers.COPY_PART_RANGE, range);
        }

        // Populate the SSE-C parameters for the source and destination object
        populateSSE_C(request, copyPartRequest.getDestinationSSECustomerKey());
        populateSourceSSE_C(request, copyPartRequest.getSourceSSECustomerKey());

    }

    private String formatKey(String key) {
        if (key == null) {
            return "/";
        }
        if (!key.startsWith("/")) {
            key = "/" + key;
        }
        return key;
    }

    // 格式化一些路径, 去掉开始时的分隔符/, 比如list prefix.
    // 因为COS V4的prefix是以/开始的,这里SDK需要坐下兼容
    private String leftStripPathDelimiter(String path) {
        if (path == null) {
            return path;
        }
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }

    // 格式化bucket, 是bucket返回带appid
    private String formatBucket(String bucketName, String appid) throws CosClientException {
        BucketNameUtils.validateBucketName(bucketName);
        if (appid == null) {
            if (!bucketName.trim().isEmpty()) {
                return bucketName;
            } else {
                throw new CosClientException(
                        "please make sure bucket name must contain legal appid when appid is missing. example: music-1251122334");
            }
        }

        String appidSuffix = "-" + appid;
        if (bucketName.endsWith(appidSuffix)) {
            return bucketName;
        } else {
            return bucketName + appidSuffix;
        }
    }

    private <X extends CosServiceRequest> void buildUrlAndHost(CosHttpRequest<X> request,
                                                               String bucket, String key, boolean isServiceRequest) throws CosClientException {
        boolean isCIRequest = request.getOriginalRequest() instanceof CIServiceRequest;
        key = formatKey(key);
        request.setResourcePath(key);
        String endpoint = "";
        String endpointAddr = "";
        if (isServiceRequest) {
            endpoint = clientConfig.getEndpointBuilder().buildGetServiceApiEndpoint();
            endpointAddr =
                    clientConfig.getEndpointResolver().resolveGetServiceApiEndpoint(endpoint);
        } else {
            bucket = formatBucket(bucket, fetchCredential().getCOSAppId());
            if (isCIRequest) {
                endpoint = new CIRegionEndpointBuilder(clientConfig.getRegion()).buildGeneralApiEndpoint(bucket);
            } else {
                endpoint = clientConfig.getEndpointBuilder().buildGeneralApiEndpoint(bucket);
            }
            endpointAddr = clientConfig.getEndpointResolver().resolveGeneralApiEndpoint(endpoint);
        }

        if (endpoint == null) {
            throw new CosClientException("endpoint is null, please check your endpoint builder");
        }
        if (endpointAddr == null) {
            throw new CosClientException(
                    "endpointAddr is null, please check your endpoint resolver");
        }

        request.addHeader(Headers.HOST, endpoint);
        if (isCIRequest && !clientConfig.getCiSpecialRequest()) {
            //万象请求只支持https
            request.setProtocol(HttpProtocol.https);
        } else {
            request.setProtocol(clientConfig.getHttpProtocol());
        }
        String fixedEndpointAddr = request.getOriginalRequest().getFixedEndpointAddr();
        if (fixedEndpointAddr != null) {
            request.setEndpoint(fixedEndpointAddr);
        } else {
            request.setEndpoint(endpointAddr);
        }

        request.setResourcePath(key);
    }

    private <X, Y extends CosServiceRequest> X invoke(CosHttpRequest<Y> request,
            Unmarshaller<X, InputStream> unmarshaller)
            throws CosClientException, CosServiceException {
        return invoke(request, new COSXmlResponseHandler<X>(unmarshaller));
    }

    private <X, Y extends CosServiceRequest> X invoke(CosHttpRequest<Y> request,
            HttpResponseHandler<CosServiceResponse<X>> responseHandler)
            throws CosClientException, CosServiceException {

        COSSigner cosSigner = clientConfig.getCosSigner();
        COSCredentials cosCredentials;
        CosServiceRequest cosServiceRequest = request.getOriginalRequest();
        if(cosServiceRequest != null && cosServiceRequest.getCosCredentials() != null) {
            cosCredentials = cosServiceRequest.getCosCredentials();
        } else {
            cosCredentials = fetchCredential();
        }
        Date expiredTime = new Date(System.currentTimeMillis() + clientConfig.getSignExpired() * 1000);
        boolean isCIWorkflowRequest = cosServiceRequest instanceof  CIWorkflowServiceRequest;
        cosSigner.setCIWorkflowRequest(isCIWorkflowRequest);
        cosSigner.sign(request, cosCredentials, expiredTime);
        return this.cosHttpClient.exeute(request, responseHandler);
    }

    private static PutObjectResult createPutObjectResult(ObjectMetadata metadata) {
        final PutObjectResult result = new PutObjectResult();
        result.setRequestId((String) metadata.getRawMetadataValue(Headers.REQUEST_ID));
        result.setDateStr((String) metadata.getRawMetadataValue(Headers.DATE));
        result.setVersionId(metadata.getVersionId());
        result.setETag(metadata.getETag());
        result.setExpirationTime(metadata.getExpirationTime());
        result.setSSEAlgorithm(metadata.getSSEAlgorithm());
        result.setSSECustomerAlgorithm(metadata.getSSECustomerAlgorithm());
        result.setSSECustomerKeyMd5(metadata.getSSECustomerKeyMd5());
        result.setCrc64Ecma(metadata.getCrc64Ecma());
        result.setMetadata(metadata);
        result.setCiUploadResult(metadata.getCiUploadResult());
        return result;
    }

    private static AppendObjectResult createAppendObjectResult(ObjectMetadata metadata) {
        final AppendObjectResult result = new AppendObjectResult();
        result.setNextAppendPosition(Long.valueOf(
                (String)metadata.getRawMetadataValue(Headers.APPEND_OBJECT_NEXT_POSISTION)));
        result.setMetadata(metadata);
        return result;
    }


    /**
     * Adds the specified parameter to the specified request, if the parameter value is not null.
     *
     * @param request The request to add the parameter to.
     * @param paramName The parameter name.
     * @param paramValue The parameter value.
     */
    private static void addParameterIfNotNull(CosHttpRequest<?> request, String paramName,
            String paramValue) {
        if (paramValue != null) {
            request.addParameter(paramName, paramValue);
        }
    }

    /**
     * Adds the specified header to the specified request, if the header value is not null.
     *
     * @param request The request to add the header to.
     * @param header The header name.
     * @param value The header value.
     */
    private static void addHeaderIfNotNull(CosHttpRequest<?> request, String header, String value) {
        if (value != null) {
            request.addHeader(header, value);
        }
    }

    /**
     * <p>
     * Adds the specified date header in RFC 822 date format to the specified request. This method
     * will not add a date header if the specified date value is <code>null</code>.
     * </p>
     *
     * @param request The request to add the header to.
     * @param header The header name.
     * @param value The header value.
     */
    private static void addDateHeader(CosHttpRequest<?> request, String header, Date value) {
        if (value != null) {
            request.addHeader(header, DateUtils.formatRFC822Date(value));
        }
    }

    /**
     * <p>
     * Adds the specified string list header, joined together separated with commas, to the
     * specified request. This method will not add a string list header if the specified values are
     * <code>null</code> or empty.
     * </p>
     *
     * @param request The request to add the header to.
     * @param header The header name.
     * @param values The list of strings to join together for the header value.
     */
    private static void addStringListHeader(CosHttpRequest<?> request, String header,
            List<String> values) {
        if (values != null && !values.isEmpty()) {
            request.addHeader(header, StringUtils.join(values));
        }
    }

    private void setZeroContentLength(CosHttpRequest<?> req) {
        req.addHeader(Headers.CONTENT_LENGTH, String.valueOf(0));
    }

    private boolean shouldRetryCompleteMultipartUpload(CosServiceRequest originalRequest,
            CosClientException exception, int retriesAttempted) {
        return false;
    }

    /**
     * <p>
     * Adds response headers parameters to the request given, if non-null.
     * </p>
     *
     * @param request The request to add the response header parameters to.
     * @param responseHeaders The full set of response headers to add, or null for none.
     */
    private static void addResponseHeaderParameters(CosHttpRequest<?> request,
            ResponseHeaderOverrides responseHeaders) {
        if (responseHeaders != null) {
            if (responseHeaders.getCacheControl() != null) {
                request.addParameter(ResponseHeaderOverrides.RESPONSE_HEADER_CACHE_CONTROL,
                        responseHeaders.getCacheControl());
            }
            if (responseHeaders.getContentDisposition() != null) {
                request.addParameter(ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_DISPOSITION,
                        responseHeaders.getContentDisposition());
            }
            if (responseHeaders.getContentEncoding() != null) {
                request.addParameter(ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_ENCODING,
                        responseHeaders.getContentEncoding());
            }
            if (responseHeaders.getContentLanguage() != null) {
                request.addParameter(ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_LANGUAGE,
                        responseHeaders.getContentLanguage());
            }
            if (responseHeaders.getContentType() != null) {
                request.addParameter(ResponseHeaderOverrides.RESPONSE_HEADER_CONTENT_TYPE,
                        responseHeaders.getContentType());
            }
            if (responseHeaders.getExpires() != null) {
                request.addParameter(ResponseHeaderOverrides.RESPONSE_HEADER_EXPIRES,
                        responseHeaders.getExpires());
            }
        }
    }

    /**
     * <p>
     * Populates the specified request with the numerous attributes available in
     * <code>SSEWithCustomerKeyRequest</code>.
     * </p>
     *
     * @param request The request to populate with headers to represent all the options expressed in
     *        the <code>ServerSideEncryptionWithCustomerKeyRequest</code> object.
     * @param sseKey The request object for an COS operation that allows server-side encryption
     *        using customer-provided keys.
     */
    private static void populateSSE_C(CosHttpRequest<?> request, SSECustomerKey sseKey) {
        if (sseKey == null)
            return;

        addHeaderIfNotNull(request, Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                sseKey.getAlgorithm());
        addHeaderIfNotNull(request, Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY, sseKey.getKey());
        addHeaderIfNotNull(request, Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                sseKey.getMd5());
        // Calculate the MD5 hash of the encryption key and fill it in the
        // header, if the user didn't specify it in the metadata
        if (sseKey.getKey() != null && sseKey.getMd5() == null) {
            String encryptionKey_b64 = sseKey.getKey();
            byte[] encryptionKey = Base64.decode(encryptionKey_b64);
            request.addHeader(Headers.SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                    Md5Utils.md5AsBase64(encryptionKey));
        }
    }

    private static void populateTrafficLimit(CosHttpRequest<?> request, int trafficLimit) {
        if(trafficLimit > 0) {
            request.addHeader(Headers.COS_TRAFFIC_LIMIT, String.valueOf(trafficLimit));
        }
    }

    private static void populateSourceSSE_C(CosHttpRequest<?> request, SSECustomerKey sseKey) {
        if (sseKey == null)
            return;

        // Populate the SSE-C parameters for the source object
        addHeaderIfNotNull(request, Headers.COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_ALGORITHM,
                sseKey.getAlgorithm());
        addHeaderIfNotNull(request, Headers.COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY,
                sseKey.getKey());
        addHeaderIfNotNull(request, Headers.COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                sseKey.getMd5());
        // Calculate the MD5 hash of the encryption key and fill it in the
        // header, if the user didn't specify it in the metadata
        if (sseKey.getKey() != null && sseKey.getMd5() == null) {
            String encryptionKey_b64 = sseKey.getKey();
            byte[] encryptionKey = Base64.decode(encryptionKey_b64);
            request.addHeader(Headers.COPY_SOURCE_SERVER_SIDE_ENCRYPTION_CUSTOMER_KEY_MD5,
                    Md5Utils.md5AsBase64(encryptionKey));
        }
    }

    private static void populateSSE_KMS(CosHttpRequest<?> request,
            SSECOSKeyManagementParams sseParams) {
        if (sseParams != null) {
            addHeaderIfNotNull(request, Headers.SERVER_SIDE_ENCRYPTION, sseParams.getEncryption());
            addHeaderIfNotNull(request, Headers.SERVER_SIDE_ENCRYPTION_COS_KMS_KEY_ID,
                    sseParams.getCOSKmsKeyId());
            addHeaderIfNotNull(request, Headers.SERVER_SIDE_ENCRYPTION_CONTEXT,
                    sseParams.getEncryptionContext());
        }
    }

    @Override
    public PutObjectResult putObject(PutObjectRequest putObjectRequest)
            throws CosClientException, CosServiceException {
        ObjectMetadata returnedMetadata = uploadObjectInternal(UploadMode.PUT_OBJECT, putObjectRequest);
        PutObjectResult result = createPutObjectResult(returnedMetadata);
        result.setContentMd5(returnedMetadata.getETag());
        return result;
    }

    @Override
    public AppendObjectResult appendObject(AppendObjectRequest appendObjectRequest)
            throws CosServiceException, CosClientException {
        rejectNull(appendObjectRequest, "The append object request must be specified");
        rejectNull(appendObjectRequest.getPosition(), "The position parameter must be specified");
        ObjectMetadata returnedMetadata = uploadObjectInternal(UploadMode.APPEND_OBJECT, appendObjectRequest);
        return createAppendObjectResult(returnedMetadata);
    }

    @Override
    public void rename(RenameRequest renameRequest)
            throws CosServiceException, CosClientException {
        rejectNull(renameRequest, "The request must not be null");
        rejectNull(renameRequest.getBucketName(), "The bucket name parameter must be specified when rename");
        rejectNull(renameRequest.getSrcObject(), "The src object parameter must be specified when rename");
        rejectNull(renameRequest.getDstObject(), "The dst object parameter must be specified when rename");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when rename");
        rejectEmpty(renameRequest.getSrcObject(), "The length of the src key must be greater than 0");
        rejectEmpty(renameRequest.getDstObject(), "The length of the dst key must be greater than 0");
        CosHttpRequest<RenameRequest> request = createRequest(renameRequest.getBucketName(),
                renameRequest.getDstObject(), renameRequest, HttpMethodName.PUT);
        request.addParameter("rename", null);
        request.addHeader("x-cos-rename-source", renameRequest.getSrcObject());
        invoke(request, voidCosResponseHandler);
    }

    protected <UploadObjectRequest extends PutObjectRequest>
        ObjectMetadata uploadObjectInternal(UploadMode uploadMode, UploadObjectRequest uploadObjectRequest)
            throws CosClientException, CosServiceException {
        rejectNull(uploadObjectRequest,
                "The PutObjectRequest parameter must be specified when uploading an object");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when uploading an object");

        final File file = uploadObjectRequest.getFile();
        final InputStream isOrig = uploadObjectRequest.getInputStream();
        final String bucketName = uploadObjectRequest.getBucketName();
        final String key = uploadObjectRequest.getKey();
        ObjectMetadata metadata = uploadObjectRequest.getMetadata();
        InputStream input = isOrig;
        if (metadata == null)
            metadata = new ObjectMetadata();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when uploading an object");
        rejectNull(key, "The key parameter must be specified when uploading an object");
        // If a file is specified for upload, we need to pull some additional
        // information from it to auto-configure a few options
        if (file == null) {
            // When input is a FileInputStream, this wrapping enables
            // unlimited mark-and-reset
            if (input != null)
                input = ReleasableInputStream.wrap(input);
        } else {
            // Always set the content length, even if it's already set
            metadata.setContentLength(file.length());
            final long maxAllowdSingleFileSize = 5 * 1024L * 1024L * 1024L;
            if (file.length() > maxAllowdSingleFileSize) {
                throw new CosClientException(
                        "max size 5GB is allowed by putObject Method, your filesize is "
                                + file.length()
                                + ", please use transferManager to upload big file!");
            }
            final boolean calculateMD5 = metadata.getContentMD5() == null;

            if (calculateMD5 && !skipMd5CheckStrategy.skipServerSideValidation(uploadObjectRequest)) {
                try {
                    String contentMd5_b64 = Md5Utils.md5AsBase64(file);
                    metadata.setContentMD5(contentMd5_b64);
                } catch (Exception e) {
                    throw new CosClientException("Unable to calculate MD5 hash: " + e.getMessage(),
                            e);
                }
            }
            input = ResettableInputStream.newResettableInputStream(file,
                    "Unable to find file to upload");
        }

        final ObjectMetadata returnedMetadata;
        MD5DigestCalculatingInputStream md5DigestStream = null;
        try {
            CosHttpRequest<UploadObjectRequest> request = null;
            if(uploadMode.equals(UploadMode.PUT_OBJECT)) {
                request = createRequest(bucketName, key, uploadObjectRequest, HttpMethodName.PUT);
            } else if(uploadMode.equals(UploadMode.APPEND_OBJECT)){
                request = createRequest(bucketName, key, uploadObjectRequest, HttpMethodName.POST);
                AppendObjectRequest appendObjectRequest = (AppendObjectRequest)uploadObjectRequest;
                String positionStr = String.valueOf(appendObjectRequest.getPosition());
                request.addParameter("append", null);
                request.addParameter("position", positionStr);
            }
            if (uploadObjectRequest.getAccessControlList() != null) {
                addAclHeaders(request, uploadObjectRequest.getAccessControlList());
            } else if (uploadObjectRequest.getCannedAcl() != null) {
                request.addHeader(Headers.COS_CANNED_ACL,
                        uploadObjectRequest.getCannedAcl().toString());
            }

            if (uploadObjectRequest.getStorageClass() != null) {
                request.addHeader(Headers.STORAGE_CLASS, uploadObjectRequest.getStorageClass());
            }

            if (uploadObjectRequest.getRedirectLocation() != null) {
                request.addHeader(Headers.REDIRECT_LOCATION,
                        uploadObjectRequest.getRedirectLocation());
                if (input == null) {
                    input = new ByteArrayInputStream(new byte[0]);
                }
            }

            // Populate the SSE-C parameters to the request header
            populateSSE_C(request, uploadObjectRequest.getSSECustomerKey());

            // Populate the SSE KMS parameters to the request header
            populateSSE_KMS(request, uploadObjectRequest.getSSECOSKeyManagementParams());

            // Populate the traffic limit parameter to the request header
            populateTrafficLimit(request, uploadObjectRequest.getTrafficLimit());
            // Use internal interface to differentiate 0 from unset.
            final Long contentLength = (Long) metadata.getRawMetadataValue(Headers.CONTENT_LENGTH);
            if (contentLength == null) {
                /*
                 * There's nothing we can do except for let the HTTP client buffer the input stream
                 * contents if the caller doesn't tell us how much data to expect in a stream since
                 * we have to explicitly tell how much we're sending before we start sending any of
                 * it.
                 */
                log.warn("No content length specified for stream data.  "
                        + "Stream contents will be buffered in memory and could result in "
                        + "out of memory errors.");
            } else {
                final long expectedLength = contentLength.longValue();
                final long maxAllowdSingleFileSize = 5 * 1024L * 1024L * 1024L;
                if (expectedLength > maxAllowdSingleFileSize) {
                    throw new CosClientException(
                            "max size 5GB is allowed by putObject Method, your filesize is "
                                    + expectedLength
                                    + ", please use transferManager to upload big file!");
                }
                if (expectedLength >= 0) {
                    // Performs length check on the underlying data stream.
                    // For COS encryption client, the underlying data stream here
                    // refers to the cipher-text data stream (ie not the underlying
                    // plain-text data stream which in turn may have been wrapped
                    // with it's own length check input stream.)
                    LengthCheckInputStream lcis = new LengthCheckInputStream(input, expectedLength, // expected
                                                                                                    // data
                                                                                                    // length
                                                                                                    // to
                                                                                                    // be
                                                                                                    // uploaded
                            EXCLUDE_SKIPPED_BYTES);
                    input = lcis;
                }
            }

            if (metadata.getContentMD5() == null
                    && !skipMd5CheckStrategy.skipClientSideValidationPerRequest(uploadObjectRequest)) {
                /*
                 * If the user hasn't set the content MD5, then we don't want to buffer the whole
                 * stream in memory just to calculate it. Instead, we can calculate it on the fly
                 * and validate it with the returned ETag from the object upload.
                 */
                input = md5DigestStream = new MD5DigestCalculatingInputStream(input);
            }

            populateRequestMetadata(request, metadata);
            request.setContent(input);
            try {
                if(uploadObjectRequest.getPicOperations() != null) {
                    request.addHeader(Headers.PIC_OPERATIONS, Jackson.toJsonString(uploadObjectRequest.getPicOperations()));
                    returnedMetadata = invoke(request, new ResponseHeaderHandlerChain<ObjectMetadata>(
                            new Unmarshallers.ImagePersistenceUnmarshaller(), new CosMetadataResponseHandler()));
                } else {
                    returnedMetadata = invoke(request, new CosMetadataResponseHandler());
                }
            } catch (Throwable t) {
                throw Throwables.failure(t);
            }
        } finally {
            CosDataSource.Utils.cleanupDataSource(uploadObjectRequest, file, isOrig, input, log);
        }

        String contentMd5 = metadata.getContentMD5();
        if (md5DigestStream != null) {
            contentMd5 = Base64.encodeAsString(md5DigestStream.getMd5Digest());
        }

        final String etag = returnedMetadata.getETag();
        if (contentMd5 != null && uploadMode.equals(UploadMode.PUT_OBJECT)
                && !skipMd5CheckStrategy.skipClientSideValidationPerPutResponse(returnedMetadata) ) {
            byte[] clientSideHash = BinaryUtils.fromBase64(contentMd5);
            byte[] serverSideHash = null;
            try {
                serverSideHash = BinaryUtils.fromHex(etag);
            } catch (DecoderException e) {
                throw new CosClientException("Unable to verify integrity of data upload.  "
                        + "Client calculated content hash (contentMD5: " + contentMd5
                        + " in base 64) didn't match hash (etag: " + etag
                        + " in hex) calculated by COS .  "
                        + "You may need to delete the data stored in COS . (metadata.contentMD5: "
                        + metadata.getContentMD5() + ", bucketName: " + bucketName + ", key: " + key
                        + ")");
            }

            if (!Arrays.equals(clientSideHash, serverSideHash)) {
                throw new CosClientException("Unable to verify integrity of data upload.  "
                        + "Client calculated content hash (contentMD5: " + contentMd5
                        + " in base 64) didn't match hash (etag: " + etag
                        + " in hex) calculated by COS .  "
                        + "You may need to delete the data stored in COS . (metadata.contentMD5: "
                        + metadata.getContentMD5() + ", bucketName: " + bucketName + ", key: " + key
                        + ")");
            }
        }
        return returnedMetadata;
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, File file)
            throws CosClientException, CosServiceException {
        return putObject(
                new PutObjectRequest(bucketName, key, file).withMetadata(new ObjectMetadata()));
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, InputStream input,
            ObjectMetadata metadata) throws CosClientException, CosServiceException {
        return putObject(new PutObjectRequest(bucketName, key, input, metadata));
    }

    @Override
    public PutObjectResult putObject(String bucketName, String key, String content)
            throws CosClientException, CosServiceException {
        rejectNull(bucketName,
                "The bucket name parameter must be specified when uploading an object");
        rejectNull(key, "The key parameter must be specified when uploading an object");
        rejectNull(content,
                "The content with utf-8 encoding must be specified when uploading an object");

        byte[] contentByteArray = content.getBytes(StringUtils.UTF8);
        String contentMd5 = Md5Utils.md5AsBase64(contentByteArray);

        InputStream contentInput = new ByteArrayInputStream(contentByteArray);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentType("text/plain");
        metadata.setContentLength(contentByteArray.length);
        metadata.setContentMD5(contentMd5);

        return putObject(new PutObjectRequest(bucketName, key, contentInput, metadata));
    }

    @Override
    public COSObject getObject(String bucketName, String key)
            throws CosClientException, CosServiceException {
        return getObject(new GetObjectRequest(bucketName, key));
    }

    @Override
    public COSObject getObject(GetObjectRequest getObjectRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getObjectRequest,
                "The GetObjectRequest parameter must be specified when requesting an object");
        rejectNull(getObjectRequest.getBucketName(),
                "The bucket name parameter must be specified when requesting an object");
        rejectNull(getObjectRequest.getKey(),
                "The key parameter must be specified when requesting an object");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when requesting an object");


        CosHttpRequest<GetObjectRequest> request = createRequest(getObjectRequest.getBucketName(),
                getObjectRequest.getKey(), getObjectRequest, HttpMethodName.GET);
        addParameterIfNotNull(request, "versionId", getObjectRequest.getVersionId());

        // Range
        long[] range = getObjectRequest.getRange();
        if (range != null) {
            request.addHeader(Headers.RANGE,
                    "bytes=" + Long.toString(range[0]) + "-" + Long.toString(range[1]));
        }
        addResponseHeaderParameters(request, getObjectRequest.getResponseHeaders());

        addDateHeader(request, Headers.GET_OBJECT_IF_MODIFIED_SINCE,
                getObjectRequest.getModifiedSinceConstraint());
        addDateHeader(request, Headers.GET_OBJECT_IF_UNMODIFIED_SINCE,
                getObjectRequest.getUnmodifiedSinceConstraint());
        addStringListHeader(request, Headers.GET_OBJECT_IF_MATCH,
                getObjectRequest.getMatchingETagConstraints());
        addStringListHeader(request, Headers.GET_OBJECT_IF_NONE_MATCH,
                getObjectRequest.getNonmatchingETagConstraints());

        // Populate the SSE-C parameters to the request header
        populateSSE_C(request, getObjectRequest.getSSECustomerKey());

        // Populate the traffic limit parameter to the request header
        populateTrafficLimit(request, getObjectRequest.getTrafficLimit());
        try {
            COSObject cosObject = invoke(request, new COSObjectResponseHandler());
            cosObject.setBucketName(getObjectRequest.getBucketName());
            cosObject.setKey(getObjectRequest.getKey());
            InputStream is = cosObject.getObjectContent();
            HttpRequestBase httpRequest = cosObject.getObjectContent().getHttpRequest();

            is = new ServiceClientHolderInputStream(is, this);

            // The Etag header contains a server-side MD5 of the object. If
            // we're downloading the whole object, by default we wrap the
            // stream in a validator that calculates an MD5 of the downloaded
            // bytes and complains if what we received doesn't match the Etag.
            if (!skipMd5CheckStrategy.skipClientSideValidation(getObjectRequest,
                    cosObject.getObjectMetadata())) {
                try {
                    byte[] serverSideHash =
                            BinaryUtils.fromHex(cosObject.getObjectMetadata().getETag());
                    // No content length check is performed when the
                    // MD5 check is enabled, since a correct MD5 check would
                    // imply a correct content length.
                    MessageDigest digest = MessageDigest.getInstance("MD5");
                    is = new DigestValidationInputStream(is, digest, serverSideHash);
                } catch (NoSuchAlgorithmException e) {
                    log.warn("No MD5 digest algorithm available.  Unable to calculate "
                            + "checksum and verify data integrity.", e);
                } catch (DecoderException e) {
                    log.warn("BinaryUtils.fromHex error. Unable to calculate "
                            + "checksum and verify data integrity. etag:"
                            + cosObject.getObjectMetadata().getETag(), e);
                }
            } else {
                // Ensures the data received from COS has the same length as the
                // expected content-length
                is = new LengthCheckInputStream(is,
                        cosObject.getObjectMetadata().getContentLength(), // expected length
                        INCLUDE_SKIPPED_BYTES); // bytes received from cos are all included even if
                                                // skipped
            }
            cosObject.setObjectContent(new COSObjectInputStream(is, httpRequest));
            return cosObject;
        } catch (CosServiceException cse) {
            /*
             * If the request failed because one of the specified constraints was not met (ex:
             * matching ETag, modified since date, etc.), then return null, so that users don't have
             * to wrap their code in try/catch blocks and check for this status code if they want to
             * use constraints.
             */
            if (cse.getStatusCode() == 412 || cse.getStatusCode() == 304) {
                return null;
            }
            throw cse;
        }
    }

    @Override
    public ObjectMetadata getObject(final GetObjectRequest getObjectRequest, File destinationFile)
            throws CosClientException, CosServiceException {
        rejectNull(getObjectRequest,
                "The GetObjectRequest parameter must be specified when requesting an object");
        rejectNull(destinationFile,
                "The destination file parameter must be specified when downloading an object directly to a file");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when downloading an object directly to a file");

        COSObject cosObject = ServiceUtils.retryableDownloadCOSObjectToFile(destinationFile,
                new ServiceUtils.RetryableCOSDownloadTask() {

                    @Override
                    public boolean needIntegrityCheck() {
                        return !skipMd5CheckStrategy
                                .skipClientSideValidationPerRequest(getObjectRequest);
                    }

                    @Override
                    public COSObject getCOSObjectStream() {
                        return getObject(getObjectRequest);
                    }
                }, ServiceUtils.OVERWRITE_MODE);

        if (cosObject == null)
            return null;

        return cosObject.getObjectMetadata();
    }

    @Override
    public boolean doesObjectExist(String bucketName, String objectName)
            throws CosClientException, CosServiceException {
        try {
            getObjectMetadata(bucketName, objectName);
            return true;
        } catch (CosServiceException cse) {
            if (cse.getStatusCode() == 404) {
                return false;
            }
            throw cse;
        }
    }

    @Override
    public ObjectMetadata getObjectMetadata(String bucketName, String key)
            throws CosClientException, CosServiceException {
        return getObjectMetadata(new GetObjectMetadataRequest(bucketName, key));
    }

    @Override
    public ObjectMetadata getObjectMetadata(GetObjectMetadataRequest getObjectMetadataRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getObjectMetadataRequest,
                "The GetObjectMetadataRequest parameter must be specified when requesting an object's metadata");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when requesting an object's metadata");

        String bucketName = getObjectMetadataRequest.getBucketName();
        String key = getObjectMetadataRequest.getKey();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when requesting an object's metadata");
        rejectNull(key, "The key parameter must be specified when requesting an object's metadata");

        CosHttpRequest<GetObjectMetadataRequest> request =
                createRequest(bucketName, key, getObjectMetadataRequest, HttpMethodName.HEAD);
        addParameterIfNotNull(request, "versionId", getObjectMetadataRequest.getVersionId());
        // Populate the SSE-C parameters to the request header
        populateSSE_C(request, getObjectMetadataRequest.getSSECustomerKey());
        return invoke(request, new CosMetadataResponseHandler());
    }

    @Override
    public void deleteObject(String bucketName, String key)
            throws CosClientException, CosServiceException {
        deleteObject(new DeleteObjectRequest(bucketName, key));
    }

    @Override
    public void deleteObject(DeleteObjectRequest deleteObjectRequest)
            throws CosClientException, CosServiceException {
        rejectNull(deleteObjectRequest,
                "The delete object request must be specified when deleting an object");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when deleting an object");

        rejectNull(deleteObjectRequest.getBucketName(),
                "The bucket name must be specified when deleting an object");
        rejectNull(deleteObjectRequest.getKey(),
                "The key must be specified when deleting an object");

        rejectEmpty(deleteObjectRequest.getKey(),
                                "The length of the key must be greater than 0");
        CosHttpRequest<DeleteObjectRequest> request =
                createRequest(deleteObjectRequest.getBucketName(), deleteObjectRequest.getKey(),
                        deleteObjectRequest, HttpMethodName.DELETE);
        if (deleteObjectRequest.isRecursive()) {
            request.addParameter("recursive", null);
        }
        invoke(request, voidCosResponseHandler);
    }

    @Override
    public DeleteObjectsResult deleteObjects(DeleteObjectsRequest deleteObjectsRequest)
            throws MultiObjectDeleteException, CosClientException, CosServiceException {
        rejectNull(deleteObjectsRequest,
                "The DeleteObjectsRequest parameter must be specified when deleting objects");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when deleting objects");

        CosHttpRequest<DeleteObjectsRequest> request =
                createRequest(deleteObjectsRequest.getBucketName(), null, deleteObjectsRequest,
                        HttpMethodName.POST);
        request.addParameter("delete", null);

        byte[] content =
                new MultiObjectDeleteXmlFactory().convertToXmlByteArray(deleteObjectsRequest);
        request.addHeader("Content-Length", String.valueOf(content.length));
        request.addHeader("Content-Type", "application/xml");
        request.setContent(new ByteArrayInputStream(content));
        try {
            byte[] md5 = Md5Utils.computeMD5Hash(content);
            String md5Base64 = BinaryUtils.toBase64(md5);
            request.addHeader("Content-MD5", md5Base64);
        } catch (Exception e) {
            throw new CosClientException("Couldn't compute md5 sum", e);
        }

        @SuppressWarnings("unchecked")
        ResponseHeaderHandlerChain<DeleteObjectsResponse> responseHandler =
                new ResponseHeaderHandlerChain<DeleteObjectsResponse>(
                        new Unmarshallers.DeleteObjectsResultUnmarshaller());

        DeleteObjectsResponse response = invoke(request, responseHandler);

        /*
         * If the result was only partially successful, throw an exception
         */
        if (!response.getErrors().isEmpty()) {
            Map<String, String> headers = responseHandler.getResponseHeaders();

            MultiObjectDeleteException ex = new MultiObjectDeleteException(response.getErrors(),
                    response.getDeletedObjects());

            ex.setStatusCode(200);
            ex.setRequestId(headers.get(Headers.REQUEST_ID));

            throw ex;
        }
        DeleteObjectsResult result = new DeleteObjectsResult(response.getDeletedObjects());

        return result;
    }

    @Override
    public void deleteVersion(String bucketName, String key, String versionId)
            throws CosClientException, CosServiceException {
        deleteVersion(new DeleteVersionRequest(bucketName, key, versionId));
    }

    @Override
    public void deleteVersion(DeleteVersionRequest deleteVersionRequest)
            throws CosClientException, CosServiceException {
        rejectNull(deleteVersionRequest,
                "The DeleteVersionRequest parameter must be specified when deleting a version");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when deleting a version");


        String bucketName = deleteVersionRequest.getBucketName();
        String key = deleteVersionRequest.getKey();
        String versionId = deleteVersionRequest.getVersionId();

        rejectNull(bucketName, "The bucket name must be specified when deleting a version");
        rejectNull(key, "The key must be specified when deleting a version");
        rejectNull(versionId, "The version ID must be specified when deleting a version");

        CosHttpRequest<DeleteVersionRequest> request =
                createRequest(bucketName, key, deleteVersionRequest, HttpMethodName.DELETE);
        request.addParameter("versionId", versionId);

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public Bucket createBucket(String bucketName) throws CosClientException, CosServiceException {
        return createBucket(new CreateBucketRequest(bucketName));
    }

    @Override
    public Bucket createBucket(CreateBucketRequest createBucketRequest)
            throws CosClientException, CosServiceException {
        rejectNull(createBucketRequest,
                "The CreateBucketRequest parameter must be specified when creating a bucket");

        String bucketName = createBucketRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when creating a bucket");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when creating a bucket");

        bucketName = bucketName.trim();
        BucketNameUtils.validateBucketName(bucketName);

        CosHttpRequest<CreateBucketRequest> request =
                createRequest(bucketName, "/", createBucketRequest, HttpMethodName.PUT);

        if (createBucketRequest.getAccessControlList() != null) {
            addAclHeaders(request, createBucketRequest.getAccessControlList());
        } else if (createBucketRequest.getCannedAcl() != null) {
            request.addHeader(Headers.COS_CANNED_ACL,
                    createBucketRequest.getCannedAcl().toString());
        }

        invoke(request, voidCosResponseHandler);

        return new Bucket(bucketName);
    }

    @Override
    public void deleteBucket(String bucketName) throws CosClientException, CosServiceException {
        deleteBucket(new DeleteBucketRequest(bucketName));
    }

    @Override
    public void deleteBucket(DeleteBucketRequest deleteBucketRequest)
            throws CosClientException, CosServiceException {
        rejectNull(deleteBucketRequest,
                "The DeleteBucketRequest parameter must be specified when deleting a bucket");

        String bucketName = deleteBucketRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when deleting a bucket");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when deleting a bucket");

        CosHttpRequest<DeleteBucketRequest> request =
                createRequest(bucketName, "/", deleteBucketRequest, HttpMethodName.DELETE);
        invoke(request, voidCosResponseHandler);
    }

    @Override
    public boolean doesBucketExist(String bucketName)
            throws CosClientException, CosServiceException {
        try {
            headBucket(new HeadBucketRequest(bucketName));
            return true;
        } catch (CosServiceException cse) {
            if (cse.getStatusCode() == Constants.NO_SUCH_BUCKET_STATUS_CODE) {
                return false;
            }
            throw cse;
        }
    }

    @Override
    public HeadBucketResult headBucket(HeadBucketRequest headBucketRequest)
            throws CosClientException, CosServiceException {
        rejectNull(headBucketRequest,
                "The HeadBucketRequest parameter must be specified when head a bucket");
        String bucketName = headBucketRequest.getBucketName();

        rejectNull(bucketName, "The bucketName parameter must be specified.");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when querying a bucket");

        CosHttpRequest<HeadBucketRequest> request =
                createRequest(bucketName, null, headBucketRequest, HttpMethodName.HEAD);

        return invoke(request, new HeadBucketResultHandler());
    }

    @Override
    public List<Bucket> listBuckets() throws CosClientException, CosServiceException {
        return listBuckets(new ListBucketsRequest());
    }

    @Override
    public List<Bucket> listBuckets(ListBucketsRequest listBucketsRequest)
            throws CosClientException, CosServiceException {
        rejectNull(listBucketsRequest,
                "The request object parameter listBucketsRequest must be specified.");
        CosHttpRequest<ListBucketsRequest> request =
                createRequest(null, null, listBucketsRequest, HttpMethodName.GET);
        return invoke(request, new Unmarshallers.ListBucketsUnmarshaller());
    }

    @Override
    public String getBucketLocation(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketLocation(new GetBucketLocationRequest(bucketName));
    }

    @Override
    public String getBucketLocation(GetBucketLocationRequest getBucketLocationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketLocationRequest,
                "The request parameter must be specified when requesting a bucket's location");
        String bucketName = getBucketLocationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when requesting a bucket's location");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when requesting a bucket's location");

        CosHttpRequest<GetBucketLocationRequest> request =
                createRequest(bucketName, null, getBucketLocationRequest, HttpMethodName.GET);
        request.addParameter("location", null);

        return invoke(request, new Unmarshallers.BucketLocationUnmarshaller());
    }

    @Override
    public InitiateMultipartUploadResult initiateMultipartUpload(
            InitiateMultipartUploadRequest initiateMultipartUploadRequest)
            throws CosClientException, CosServiceException {
        rejectNull(initiateMultipartUploadRequest,
                "The request parameter must be specified when initiating a multipart upload");
        rejectNull(initiateMultipartUploadRequest.getBucketName(),
                "The bucket name parameter must be specified when initiating a multipart upload");
        rejectNull(initiateMultipartUploadRequest.getKey(),
                "The key parameter must be specified when initiating a multipart upload");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when initiating a multipart upload");

        CosHttpRequest<InitiateMultipartUploadRequest> request =
                createRequest(initiateMultipartUploadRequest.getBucketName(),
                        initiateMultipartUploadRequest.getKey(), initiateMultipartUploadRequest,
                        HttpMethodName.POST);
        request.addParameter("uploads", null);

        if (initiateMultipartUploadRequest.getStorageClass() != null)
            request.addHeader(Headers.STORAGE_CLASS,
                    initiateMultipartUploadRequest.getStorageClass().toString());

        if (initiateMultipartUploadRequest.getRedirectLocation() != null) {
            request.addHeader(Headers.REDIRECT_LOCATION,
                    initiateMultipartUploadRequest.getRedirectLocation());
        }

        if (initiateMultipartUploadRequest.getAccessControlList() != null) {
            addAclHeaders(request, initiateMultipartUploadRequest.getAccessControlList());
        } else if (initiateMultipartUploadRequest.getCannedACL() != null) {
            request.addHeader(Headers.COS_CANNED_ACL,
                    initiateMultipartUploadRequest.getCannedACL().toString());
        }

        if (initiateMultipartUploadRequest.objectMetadata != null) {
            populateRequestMetadata(request, initiateMultipartUploadRequest.objectMetadata);
        }

        // Populate the SSE-C parameters to the request header
        populateSSE_C(request, initiateMultipartUploadRequest.getSSECustomerKey());

        // Populate the SSE KMS parameters to the request header
        populateSSE_KMS(request, initiateMultipartUploadRequest.getSSECOSKeyManagementParams());

        // init upload body length is zero
        request.addHeader(Headers.CONTENT_LENGTH, String.valueOf(0));

        @SuppressWarnings("unchecked")
        ResponseHeaderHandlerChain<InitiateMultipartUploadResult> responseHandler =
                new ResponseHeaderHandlerChain<InitiateMultipartUploadResult>(
                        // xml payload unmarshaller
                        new Unmarshallers.InitiateMultipartUploadResultUnmarshaller(),
                        // header handlers
                        new ServerSideEncryptionHeaderHandler<InitiateMultipartUploadResult>());
        return invoke(request, responseHandler);
    }

    @Override
    public UploadPartResult uploadPart(UploadPartRequest uploadPartRequest)
            throws CosClientException, CosServiceException {
        rejectNull(uploadPartRequest,
                "The request parameter must be specified when uploading a part");
        final File fileOrig = uploadPartRequest.getFile();
        final InputStream isOrig = uploadPartRequest.getInputStream();
        final String bucketName = uploadPartRequest.getBucketName();
        final String key = uploadPartRequest.getKey();
        final String uploadId = uploadPartRequest.getUploadId();
        final int partNumber = uploadPartRequest.getPartNumber();
        final long partSize = uploadPartRequest.getPartSize();
        rejectNull(bucketName, "The bucket name parameter must be specified when uploading a part");
        rejectNull(key, "The key parameter must be specified when uploading a part");
        rejectNull(uploadId, "The upload ID parameter must be specified when uploading a part");
        rejectNull(partNumber, "The part number parameter must be specified when uploading a part");
        rejectNull(partSize, "The part size parameter must be specified when uploading a part");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when uploading a part");
        CosHttpRequest<UploadPartRequest> request =
                createRequest(bucketName, key, uploadPartRequest, HttpMethodName.PUT);
        request.addParameter("uploadId", uploadId);
        request.addParameter("partNumber", Integer.toString(partNumber));

        final ObjectMetadata objectMetadata = uploadPartRequest.getObjectMetadata();
        if (objectMetadata != null)
            populateRequestMetadata(request, objectMetadata);

        addHeaderIfNotNull(request, Headers.CONTENT_MD5, uploadPartRequest.getMd5Digest());
        request.addHeader(Headers.CONTENT_LENGTH, Long.toString(partSize));

        // Populate the SSE-C parameters to the request header
        populateSSE_C(request, uploadPartRequest.getSSECustomerKey());

        // Populate the traffic limit parameter to the request header
        populateTrafficLimit(request, uploadPartRequest.getTrafficLimit());
        InputStream isCurr = isOrig;
        try {
            if (fileOrig == null) {
                if (isOrig == null) {
                    throw new IllegalArgumentException(
                            "A File or InputStream must be specified when uploading part");
                } else {
                    // When isCurr is a FileInputStream, this wrapping enables
                    // unlimited mark-and-reset
                    isCurr = ReleasableInputStream.wrap(isCurr);
                }
            } else {
                try {
                    isCurr = new ResettableInputStream(fileOrig);
                } catch (IOException e) {
                    throw new IllegalArgumentException("Failed to open file " + fileOrig, e);
                }
            }
            isCurr = new InputSubstream(isCurr, uploadPartRequest.getFileOffset(), partSize,
                    uploadPartRequest.isLastPart());
            MD5DigestCalculatingInputStream md5DigestStream = null;
            if (uploadPartRequest.getMd5Digest() == null && !skipMd5CheckStrategy
                    .skipClientSideValidationPerRequest(uploadPartRequest)) {
                /*
                 * If the user hasn't set the content MD5, then we don't want to buffer the whole
                 * stream in memory just to calculate it. Instead, we can calculate it on the fly
                 * and validate it with the returned ETag from the object upload.
                 */
                isCurr = md5DigestStream = new MD5DigestCalculatingInputStream(isCurr);
            }
            return doUploadPart(bucketName, key, uploadId, partNumber, partSize, request, isCurr,
                    md5DigestStream);
        } finally {
            CosDataSource.Utils.cleanupDataSource(uploadPartRequest, fileOrig, isOrig, isCurr, log);
        }
    }

    private UploadPartResult doUploadPart(final String bucketName, final String key,
            final String uploadId, final int partNumber, final long partSize,
            CosHttpRequest<UploadPartRequest> request, InputStream inputStream,
            MD5DigestCalculatingInputStream md5DigestStream) {
        try {
            request.setContent(inputStream);
            ObjectMetadata metadata = invoke(request, new CosMetadataResponseHandler());
            final String etag = metadata.getETag();


            if (md5DigestStream != null && !skipMd5CheckStrategy
                    .skipClientSideValidationPerUploadPartResponse(metadata)) {
                byte[] clientSideHash = md5DigestStream.getMd5Digest();
                byte[] serverSideHash = BinaryUtils.fromHex(etag);

                if (!Arrays.equals(clientSideHash, serverSideHash)) {
                    final String info = "bucketName: " + bucketName + ", key: " + key
                            + ", uploadId: " + uploadId + ", partNumber: " + partNumber
                            + ", partSize: " + partSize;
                    throw new CosClientException("Unable to verify integrity of data upload.  "
                            + "Client calculated content hash (contentMD5: "
                            + BinaryUtils.toHex(clientSideHash)
                            + " in hex) didn't match hash (etag: " + etag
                            + " in hex) calculated by Qcloud COS.  "
                            + "You may need to delete the data stored in Qcloud COS. " + "(" + info
                            + ")");
                }
            }

            UploadPartResult result = new UploadPartResult();
            result.setETag(etag);
            result.setPartNumber(partNumber);
            result.setSSEAlgorithm(metadata.getSSEAlgorithm());
            result.setSSECustomerAlgorithm(metadata.getSSECustomerAlgorithm());
            result.setSSECustomerKeyMd5(metadata.getSSECustomerKeyMd5());
            result.setCrc64Ecma(metadata.getCrc64Ecma());

            return result;
        } catch (Throwable t) {
            throw Throwables.failure(t);
        }
    }

    @Override
    public PartListing listParts(ListPartsRequest listPartsRequest)
            throws CosClientException, CosServiceException {
        rejectNull(listPartsRequest, "The request parameter must be specified when listing parts");

        rejectNull(listPartsRequest.getBucketName(),
                "The bucket name parameter must be specified when listing parts");
        rejectNull(listPartsRequest.getKey(),
                "The key parameter must be specified when listing parts");
        rejectNull(listPartsRequest.getUploadId(),
                "The upload ID parameter must be specified when listing parts");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when listing parts");


        CosHttpRequest<ListPartsRequest> request = createRequest(listPartsRequest.getBucketName(),
                listPartsRequest.getKey(), listPartsRequest, HttpMethodName.GET);
        request.addParameter("uploadId", listPartsRequest.getUploadId());

        if (listPartsRequest.getMaxParts() != null)
            request.addParameter("max-parts", listPartsRequest.getMaxParts().toString());
        if (listPartsRequest.getPartNumberMarker() != null)
            request.addParameter("part-number-marker",
                    listPartsRequest.getPartNumberMarker().toString());
        if (listPartsRequest.getEncodingType() != null)
            request.addParameter("encoding-type", listPartsRequest.getEncodingType());

        return invoke(request, new Unmarshallers.ListPartsResultUnmarshaller());
    }

    @Override
    public void abortMultipartUpload(AbortMultipartUploadRequest abortMultipartUploadRequest)
            throws CosClientException, CosServiceException {
        rejectNull(abortMultipartUploadRequest,
                "The request parameter must be specified when aborting a multipart upload");
        rejectNull(abortMultipartUploadRequest.getBucketName(),
                "The bucket name parameter must be specified when aborting a multipart upload");
        rejectNull(abortMultipartUploadRequest.getKey(),
                "The key parameter must be specified when aborting a multipart upload");
        rejectNull(abortMultipartUploadRequest.getUploadId(),
                "The upload ID parameter must be specified when aborting a multipart upload");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when aborting a multipart uploads");

        String bucketName = abortMultipartUploadRequest.getBucketName();
        String key = abortMultipartUploadRequest.getKey();

        CosHttpRequest<AbortMultipartUploadRequest> request =
                createRequest(bucketName, key, abortMultipartUploadRequest, HttpMethodName.DELETE);
        request.addParameter("uploadId", abortMultipartUploadRequest.getUploadId());

        invoke(request, voidCosResponseHandler);

    }



    @Override
    public CompleteMultipartUploadResult completeMultipartUpload(
            CompleteMultipartUploadRequest completeMultipartUploadRequest)
            throws CosClientException, CosServiceException {
        rejectNull(completeMultipartUploadRequest,
                "The request parameter must be specified when completing a multipart upload");

        String bucketName = completeMultipartUploadRequest.getBucketName();
        String key = completeMultipartUploadRequest.getKey();
        String uploadId = completeMultipartUploadRequest.getUploadId();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when completing a multipart upload");
        rejectNull(key, "The key parameter must be specified when completing a multipart upload");
        rejectNull(uploadId,
                "The upload ID parameter must be specified when completing a multipart upload");
        rejectNull(completeMultipartUploadRequest.getPartETags(),
                "The part ETags parameter must be specified when completing a multipart upload");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when completing a multipart uploads");


        int retries = 0;
        CompleteMultipartUploadHandler handler;
        do {
            CosHttpRequest<CompleteMultipartUploadRequest> request = createRequest(bucketName, key,
                    completeMultipartUploadRequest, HttpMethodName.POST);
            request.addParameter("uploadId", uploadId);

            byte[] xml = RequestXmlFactory
                    .convertToXmlByteArray(completeMultipartUploadRequest.getPartETags());

            request.addHeader("Content-Type", "application/xml");
            request.addHeader("Content-Length", String.valueOf(xml.length));
            ObjectMetadata objectMetadata = completeMultipartUploadRequest.getObjectMetadata();
            if(objectMetadata != null) {
                populateRequestMetadata(request, objectMetadata);
            }
            request.setContent(new ByteArrayInputStream(xml));
            if(completeMultipartUploadRequest.getPicOperations() != null) {
                request.addHeader(Headers.PIC_OPERATIONS, Jackson.toJsonString(
                        completeMultipartUploadRequest.getPicOperations()));
            }
            @SuppressWarnings("unchecked")
            ResponseHeaderHandlerChain<CompleteMultipartUploadHandler> responseHandler =
                    new ResponseHeaderHandlerChain<CompleteMultipartUploadHandler>(
                            // xml payload unmarshaller
                            new Unmarshallers.CompleteMultipartUploadResultUnmarshaller(),
                            // header handlers
                            new ServerSideEncryptionHeaderHandler<CompleteMultipartUploadHandler>(),
                            new ObjectExpirationHeaderHandler<CompleteMultipartUploadHandler>(),
                            new VIDResultHandler<CompleteMultipartUploadHandler>());
            handler = invoke(request, responseHandler);
            if (handler.getCompleteMultipartUploadResult() != null) {
                Map<String, String> responseHeaders = responseHandler.getResponseHeaders();
                String versionId = responseHeaders.get(Headers.COS_VERSION_ID);
                String crc64Ecma = responseHeaders.get(Headers.COS_HASH_CRC64_ECMA);
                handler.getCompleteMultipartUploadResult().setVersionId(versionId);
                handler.getCompleteMultipartUploadResult().setCrc64Ecma(crc64Ecma);
                // if ci request, set ciUploadResult to CompleteMultipartUploadResult
                if(completeMultipartUploadRequest.getPicOperations() != null) {
                    handler.getCompleteMultipartUploadResult().setCiUploadResult(handler.getCiUploadResult());
                }
                return handler.getCompleteMultipartUploadResult();
            }
        } while (shouldRetryCompleteMultipartUpload(completeMultipartUploadRequest,
                handler.getCOSException(), retries++));

        throw handler.getCOSException();
    }

    @Override
    public MultipartUploadListing listMultipartUploads(
            ListMultipartUploadsRequest listMultipartUploadsRequest)
            throws CosClientException, CosServiceException {
        rejectNull(listMultipartUploadsRequest,
                "The request parameter must be specified when listing multipart uploads");

        rejectNull(listMultipartUploadsRequest.getBucketName(),
                "The bucket name parameter must be specified when listing multipart uploads");

        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when listing multipart uploads");

        CosHttpRequest<ListMultipartUploadsRequest> request =
                createRequest(listMultipartUploadsRequest.getBucketName(), null,
                        listMultipartUploadsRequest, HttpMethodName.GET);
        request.addParameter("uploads", null);

        if (listMultipartUploadsRequest.getKeyMarker() != null)
            request.addParameter("key-marker", listMultipartUploadsRequest.getKeyMarker());
        if (listMultipartUploadsRequest.getMaxUploads() != null)
            request.addParameter("max-uploads",
                    listMultipartUploadsRequest.getMaxUploads().toString());
        if (listMultipartUploadsRequest.getUploadIdMarker() != null)
            request.addParameter("upload-id-marker",
                    listMultipartUploadsRequest.getUploadIdMarker());
        if (listMultipartUploadsRequest.getDelimiter() != null)
            request.addParameter("delimiter", listMultipartUploadsRequest.getDelimiter());
        if (listMultipartUploadsRequest.getPrefix() != null)
            request.addParameter("prefix", listMultipartUploadsRequest.getPrefix());
        if (listMultipartUploadsRequest.getEncodingType() != null)
            request.addParameter("encoding-type", listMultipartUploadsRequest.getEncodingType());

        return invoke(request, new Unmarshallers.ListMultipartUploadsResultUnmarshaller());
    }

    @Override
    public ObjectListing listObjects(String bucketName)
            throws CosClientException, CosServiceException {
        return listObjects(new ListObjectsRequest(bucketName, null, null, null, null));
    }

    @Override
    public ObjectListing listObjects(String bucketName, String prefix)
            throws CosClientException, CosServiceException {
        return listObjects(new ListObjectsRequest(bucketName, prefix, null, null, null));
    }

    @Override
    public ObjectListing listObjects(ListObjectsRequest listObjectsRequest)
            throws CosClientException, CosServiceException {
        rejectNull(listObjectsRequest,
                "The ListObjectsRequest parameter must be specified when listing objects in a bucket");
        rejectNull(listObjectsRequest.getBucketName(),
                "The bucket name parameter must be specified when listing objects in a bucket");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when listing objects in a bucket");

        final boolean shouldSDKDecodeResponse = listObjectsRequest.getEncodingType() == null;

        CosHttpRequest<ListObjectsRequest> request = createRequest(
                listObjectsRequest.getBucketName(), "/", listObjectsRequest, HttpMethodName.GET);

        // 兼容prefix以/开始, 以为COS V4的prefix等可以以斜杠开始
        addParameterIfNotNull(request, "prefix",
                leftStripPathDelimiter(listObjectsRequest.getPrefix()));
        addParameterIfNotNull(request, "marker", listObjectsRequest.getMarker());
        addParameterIfNotNull(request, "delimiter", listObjectsRequest.getDelimiter());
        request.addParameter("encoding-type", shouldSDKDecodeResponse ? Constants.URL_ENCODING
                : listObjectsRequest.getEncodingType());
        if (listObjectsRequest.getMaxKeys() != null
                && listObjectsRequest.getMaxKeys().intValue() >= 0)
            request.addParameter("max-keys", listObjectsRequest.getMaxKeys().toString());
        COSXmlResponseHandler<ObjectListing> handler =
                new COSXmlResponseHandler(new Unmarshallers.ListObjectsUnmarshaller(shouldSDKDecodeResponse));
        return invoke(request, handler);
    }

    @Override
    public ObjectListing listNextBatchOfObjects(ObjectListing previousObjectListing)
            throws CosClientException, CosServiceException {
        return listNextBatchOfObjects(new ListNextBatchOfObjectsRequest(previousObjectListing));
    }

    @Override
    public ObjectListing listNextBatchOfObjects(
            ListNextBatchOfObjectsRequest listNextBatchOfObjectsRequest)
            throws CosClientException, CosServiceException {
        rejectNull(listNextBatchOfObjectsRequest,
                "The request object parameter must be specified when listing the next batch of objects in a bucket");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when listing the next batch of objects  in a bucket");

        ObjectListing previousObjectListing =
                listNextBatchOfObjectsRequest.getPreviousObjectListing();

        if (!previousObjectListing.isTruncated()) {
            ObjectListing emptyListing = new ObjectListing();
            emptyListing.setBucketName(previousObjectListing.getBucketName());
            emptyListing.setDelimiter(previousObjectListing.getDelimiter());
            emptyListing.setMarker(previousObjectListing.getNextMarker());
            emptyListing.setMaxKeys(previousObjectListing.getMaxKeys());
            emptyListing.setPrefix(previousObjectListing.getPrefix());
            emptyListing.setEncodingType(previousObjectListing.getEncodingType());
            emptyListing.setTruncated(false);

            return emptyListing;
        }
        return listObjects(listNextBatchOfObjectsRequest.toListObjectsRequest());
    }

    @Override
    public VersionListing listVersions(String bucketName, String prefix)
            throws CosClientException, CosServiceException {
        return listVersions(new ListVersionsRequest(bucketName, prefix, null, null, null, null));
    }

    @Override
    public VersionListing listVersions(String bucketName, String prefix, String keyMarker,
            String versionIdMarker, String delimiter, Integer maxResults)
            throws CosClientException, CosServiceException {
        ListVersionsRequest request = new ListVersionsRequest().withBucketName(bucketName)
                .withPrefix(prefix).withDelimiter(delimiter).withKeyMarker(keyMarker)
                .withVersionIdMarker(versionIdMarker).withMaxResults(maxResults);
        return listVersions(request);
    }

    @Override
    public VersionListing listVersions(ListVersionsRequest listVersionsRequest)
            throws CosClientException, CosServiceException {
        rejectNull(listVersionsRequest,
                "The ListVersionsRequest parameter must be specified when listing versions in a bucket");
        rejectNull(listVersionsRequest.getBucketName(),
                "The bucket name parameter must be specified when listing versions in a bucket");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when listing versions in a bucket");

        final boolean shouldSDKDecodeResponse = listVersionsRequest.getEncodingType() == null;

        CosHttpRequest<ListVersionsRequest> request = createRequest(
                listVersionsRequest.getBucketName(), null, listVersionsRequest, HttpMethodName.GET);
        request.addParameter("versions", null);

        addParameterIfNotNull(request, "prefix", listVersionsRequest.getPrefix());
        addParameterIfNotNull(request, "key-marker", listVersionsRequest.getKeyMarker());
        addParameterIfNotNull(request, "version-id-marker",
                listVersionsRequest.getVersionIdMarker());
        addParameterIfNotNull(request, "delimiter", listVersionsRequest.getDelimiter());
        request.addParameter("encoding-type", shouldSDKDecodeResponse ? Constants.URL_ENCODING
                : listVersionsRequest.getEncodingType());

        if (listVersionsRequest.getMaxResults() != null && listVersionsRequest.getMaxResults() >= 0)
            request.addParameter("max-keys", listVersionsRequest.getMaxResults().toString());

        return invoke(request, new Unmarshallers.VersionListUnmarshaller(shouldSDKDecodeResponse));
    }

    @Override
    public VersionListing listNextBatchOfVersions(VersionListing previousVersionListing)
            throws CosClientException, CosServiceException {
        return listNextBatchOfVersions(new ListNextBatchOfVersionsRequest(previousVersionListing));
    }

    @Override
    public VersionListing listNextBatchOfVersions(
            ListNextBatchOfVersionsRequest listNextBatchOfVersionsRequest)
            throws CosClientException, CosServiceException {
        rejectNull(listNextBatchOfVersionsRequest,
                "The request object parameter must be specified when listing the next batch of versions in a bucket");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when listing the next batch of versions in a bucket");

        VersionListing previousVersionListing =
                listNextBatchOfVersionsRequest.getPreviousVersionListing();

        if (!previousVersionListing.isTruncated()) {
            VersionListing emptyListing = new VersionListing();
            emptyListing.setBucketName(previousVersionListing.getBucketName());
            emptyListing.setDelimiter(previousVersionListing.getDelimiter());
            emptyListing.setKeyMarker(previousVersionListing.getNextKeyMarker());
            emptyListing.setVersionIdMarker(previousVersionListing.getNextVersionIdMarker());
            emptyListing.setMaxKeys(previousVersionListing.getMaxKeys());
            emptyListing.setPrefix(previousVersionListing.getPrefix());
            emptyListing.setEncodingType(previousVersionListing.getEncodingType());
            emptyListing.setTruncated(false);

            return emptyListing;
        }

        return listVersions(listNextBatchOfVersionsRequest.toListVersionsRequest());
    }

    @Override
    public CopyObjectResult copyObject(String sourceBucketName, String sourceKey,
            String destinationBucketName, String destinationKey)
            throws CosClientException, CosServiceException {
        return copyObject(new CopyObjectRequest(sourceBucketName, sourceKey, destinationBucketName,
                destinationKey));
    }

    @Override
    public CopyObjectResult copyObject(CopyObjectRequest copyObjectRequest)
            throws CosClientException, CosServiceException {
        rejectNull(copyObjectRequest,
                "The CopyObjectRequest parameter must be specified when copying an object");
        rejectNull(copyObjectRequest.getSourceBucketName(),
                "The source bucket name must be specified when copying an object");
        rejectNull(copyObjectRequest.getSourceKey(),
                "The source object key must be specified when copying an object");
        rejectNull(copyObjectRequest.getDestinationBucketName(),
                "The destination bucket name must be specified when copying an object");
        rejectNull(copyObjectRequest.getDestinationKey(),
                "The destination object key must be specified when copying an object");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when copying an object");

        String destinationKey = copyObjectRequest.getDestinationKey();
        String destinationBucketName = copyObjectRequest.getDestinationBucketName();

        CosHttpRequest<CopyObjectRequest> request = createRequest(destinationBucketName,
                destinationKey, copyObjectRequest, HttpMethodName.PUT);

        populateRequestWithCopyObjectParameters(request, copyObjectRequest);

        /*
         * We can't send a non-zero length Content-Length header if the user specified it, otherwise
         * it messes up the HTTP connection when the remote server thinks there's more data to pull.
         */
        setZeroContentLength(request);
        CopyObjectResultHandler copyObjectResultHandler = null;
        try {
            @SuppressWarnings("unchecked")
            ResponseHeaderHandlerChain<CopyObjectResultHandler> handler =
                    new ResponseHeaderHandlerChain<CopyObjectResultHandler>(
                            // xml payload unmarshaller
                            new Unmarshallers.CopyObjectUnmarshaller(),
                            // header handlers
                            new ServerSideEncryptionHeaderHandler<CopyObjectResultHandler>(),
                            new ObjectExpirationHeaderHandler<CopyObjectResultHandler>(),
                            new VIDResultHandler<CopyObjectResultHandler>());
            copyObjectResultHandler = invoke(request, handler);
        } catch (CosServiceException cse) {
            /*
             * If the request failed because one of the specified constraints was not met (ex:
             * matching ETag, modified since date, etc.), then return null, so that users don't have
             * to wrap their code in try/catch blocks and check for this status code if they want to
             * use constraints.
             */
            if (cse.getStatusCode() == Constants.FAILED_PRECONDITION_STATUS_CODE) {
                return null;
            }

            throw cse;
        }

        /*
         * CopyObject has two failure modes: 1 - An HTTP error code is returned and the error is
         * processed like any other error response. 2 - An HTTP 200 OK code is returned, but the
         * response content contains an XML error response.
         *
         * This makes it very difficult for the client runtime to cleanly detect this case and
         * handle it like any other error response. We could extend the runtime to have a more
         * flexible/customizable definition of success/error (per request), but it's probably
         * overkill for this one special case.
         */
        if (copyObjectResultHandler.getErrorCode() != null) {
            String errorCode = copyObjectResultHandler.getErrorCode();
            String errorMessage = copyObjectResultHandler.getErrorMessage();
            String requestId = copyObjectResultHandler.getErrorRequestId();

            CosServiceException cse = new CosServiceException(errorMessage);
            cse.setErrorCode(errorCode);
            cse.setRequestId(requestId);
            cse.setStatusCode(200);

            throw cse;
        }

        CopyObjectResult copyObjectResult = new CopyObjectResult();
        copyObjectResult.setETag(copyObjectResultHandler.getETag());
        copyObjectResult.setLastModifiedDate(copyObjectResultHandler.getLastModified());
        copyObjectResult.setVersionId(copyObjectResultHandler.getVersionId());
        copyObjectResult.setSSEAlgorithm(copyObjectResultHandler.getSSEAlgorithm());
        copyObjectResult.setSSECustomerAlgorithm(copyObjectResultHandler.getSSECustomerAlgorithm());
        copyObjectResult.setSSECustomerKeyMd5(copyObjectResultHandler.getSSECustomerKeyMd5());
        copyObjectResult.setExpirationTime(copyObjectResultHandler.getExpirationTime());
        copyObjectResult.setExpirationTimeRuleId(copyObjectResultHandler.getExpirationTimeRuleId());
        copyObjectResult.setDateStr(copyObjectResultHandler.getDateStr());
        copyObjectResult.setCrc64Ecma(copyObjectResultHandler.getCrc64Ecma());
        copyObjectResult.setRequestId(copyObjectResultHandler.getRequestId());

        return copyObjectResult;
    }

    @Override
    public CopyPartResult copyPart(CopyPartRequest copyPartRequest)
            throws CosClientException, CosServiceException {
        rejectNull(copyPartRequest,
                "The CopyPartRequest parameter must be specified when copying a part");
        rejectNull(copyPartRequest.getSourceBucketName(),
                "The source bucket name must be specified when copying a part");
        rejectNull(copyPartRequest.getSourceKey(),
                "The source object key must be specified when copying a part");
        rejectNull(copyPartRequest.getDestinationBucketName(),
                "The destination bucket name must be specified when copying a part");
        rejectNull(copyPartRequest.getUploadId(),
                "The upload id must be specified when copying a part");
        rejectNull(copyPartRequest.getDestinationKey(),
                "The destination object key must be specified when copying a part");
        rejectNull(copyPartRequest.getPartNumber(),
                "The part number must be specified when copying a part");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when copying a part");

        String destinationKey = copyPartRequest.getDestinationKey();
        String destinationBucketName = copyPartRequest.getDestinationBucketName();

        CosHttpRequest<CopyPartRequest> request = createRequest(destinationBucketName,
                destinationKey, copyPartRequest, HttpMethodName.PUT);

        populateRequestWithCopyPartParameters(request, copyPartRequest);

        request.addParameter("uploadId", copyPartRequest.getUploadId());
        request.addParameter("partNumber", Integer.toString(copyPartRequest.getPartNumber()));

        /*
         * We can't send a non-zero length Content-Length header if the user
         * specified it, otherwise it messes up the HTTP connection when the
         * remote server thinks there's more data to pull.
         */
        setZeroContentLength(request);
        CopyObjectResultHandler copyObjectResultHandler = null;
        try {
            @SuppressWarnings("unchecked")
            ResponseHeaderHandlerChain<CopyObjectResultHandler> handler =
                    new ResponseHeaderHandlerChain<CopyObjectResultHandler>(
                            // xml payload unmarshaller
                            new Unmarshallers.CopyObjectUnmarshaller(),
                            // header handlers
                            new ServerSideEncryptionHeaderHandler<CopyObjectResultHandler>(),
                            new COSVersionHeaderHandler());
            copyObjectResultHandler = invoke(request, handler);
        } catch (CosServiceException cse) {
            /*
             * If the request failed because one of the specified constraints
             * was not met (ex: matching ETag, modified since date, etc.), then
             * return null, so that users don't have to wrap their code in
             * try/catch blocks and check for this status code if they want to
             * use constraints.
             */
            if (cse.getStatusCode() == Constants.FAILED_PRECONDITION_STATUS_CODE) {
                return null;
            }

            throw cse;
        }

        /*
         * CopyPart has two failure modes: 1 - An HTTP error code is returned
         * and the error is processed like any other error response. 2 - An HTTP
         * 200 OK code is returned, but the response content contains an XML
         * error response.
         *
         * This makes it very difficult for the client runtime to cleanly detect
         * this case and handle it like any other error response. We could
         * extend the runtime to have a more flexible/customizable definition of
         * success/error (per request), but it's probably overkill for this one
         * special case.
         */
        if (copyObjectResultHandler.getErrorCode() != null) {
            String errorCode = copyObjectResultHandler.getErrorCode();
            String errorMessage = copyObjectResultHandler.getErrorMessage();
            String requestId = copyObjectResultHandler.getErrorRequestId();

            CosServiceException cse = new CosServiceException(errorMessage);
            cse.setErrorCode(errorCode);
            cse.setErrorType(ErrorType.Service);
            cse.setRequestId(requestId);
            cse.setStatusCode(200);

            throw cse;
        }

        CopyPartResult copyPartResult = new CopyPartResult();
        copyPartResult.setETag(copyObjectResultHandler.getETag());
        copyPartResult.setPartNumber(copyPartRequest.getPartNumber());
        copyPartResult.setLastModifiedDate(copyObjectResultHandler.getLastModified());
        copyPartResult.setVersionId(copyObjectResultHandler.getVersionId());
        copyPartResult.setSSEAlgorithm(copyObjectResultHandler.getSSEAlgorithm());
        copyPartResult.setSSECustomerAlgorithm(copyObjectResultHandler.getSSECustomerAlgorithm());
        copyPartResult.setSSECustomerKeyMd5(copyObjectResultHandler.getSSECustomerKeyMd5());
        copyPartResult.setCrc64Ecma(copyObjectResultHandler.getCrc64Ecma());

        return copyPartResult;
    }

    @Override
    public void setBucketLifecycleConfiguration(String bucketName,
            BucketLifecycleConfiguration bucketLifecycleConfiguration)
            throws CosClientException, CosServiceException {
        setBucketLifecycleConfiguration(new SetBucketLifecycleConfigurationRequest(bucketName,
                bucketLifecycleConfiguration));
    }

    @Override
    public void setBucketLifecycleConfiguration(
            SetBucketLifecycleConfigurationRequest setBucketLifecycleConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketLifecycleConfigurationRequest,
                "The set bucket lifecycle configuration request object must be specified.");


        String bucketName = setBucketLifecycleConfigurationRequest.getBucketName();
        BucketLifecycleConfiguration bucketLifecycleConfiguration =
                setBucketLifecycleConfigurationRequest.getLifecycleConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting bucket lifecycle configuration.");
        rejectNull(bucketLifecycleConfiguration,
                "The lifecycle configuration parameter must be specified when setting bucket lifecycle configuration.");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when setting bucket lifecycle configuration");

        CosHttpRequest<SetBucketLifecycleConfigurationRequest> request = createRequest(bucketName,
                null, setBucketLifecycleConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("lifecycle", null);

        byte[] content = new BucketConfigurationXmlFactory()
                .convertToXmlByteArray(bucketLifecycleConfiguration);
        request.addHeader("Content-Length", String.valueOf(content.length));
        request.addHeader("Content-Type", "application/xml");
        request.setContent(new ByteArrayInputStream(content));
        try {
            byte[] md5 = Md5Utils.computeMD5Hash(content);
            String md5Base64 = BinaryUtils.toBase64(md5);
            request.addHeader("Content-MD5", md5Base64);
        } catch (Exception e) {
            throw new CosClientException("Couldn't compute md5 sum", e);
        }

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public BucketLifecycleConfiguration getBucketLifecycleConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketLifecycleConfiguration(
                new GetBucketLifecycleConfigurationRequest(bucketName));
    }

    @Override
    public BucketLifecycleConfiguration getBucketLifecycleConfiguration(
            GetBucketLifecycleConfigurationRequest getBucketLifecycleConfigurationRequest) {
        rejectNull(getBucketLifecycleConfigurationRequest,
                "The request object pamameter getBucketLifecycleConfigurationRequest must be specified.");
        String bucketName = getBucketLifecycleConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name must be specifed when retrieving the bucket lifecycle configuration.");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when retrieving lifecycle configuration");

        CosHttpRequest<GetBucketLifecycleConfigurationRequest> request = createRequest(bucketName,
                null, getBucketLifecycleConfigurationRequest, HttpMethodName.GET);
        request.addParameter("lifecycle", null);

        try {
            return invoke(request, new Unmarshallers.BucketLifecycleConfigurationUnmarshaller());
        } catch (CosServiceException cse) {
            switch (cse.getStatusCode()) {
                case 404:
                    return null;
                default:
                    throw cse;
            }
        }
    }

    @Override
    public void deleteBucketLifecycleConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        deleteBucketLifecycleConfiguration(
                new DeleteBucketLifecycleConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketLifecycleConfiguration(
            DeleteBucketLifecycleConfigurationRequest deleteBucketLifecycleConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(deleteBucketLifecycleConfigurationRequest,
                "The delete bucket lifecycle configuration request object must be specified.");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when deleting lifecycle configuration");

        String bucketName = deleteBucketLifecycleConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when deleting bucket lifecycle configuration.");

        CosHttpRequest<DeleteBucketLifecycleConfigurationRequest> request = createRequest(
                bucketName, null, deleteBucketLifecycleConfigurationRequest, HttpMethodName.DELETE);
        request.addParameter("lifecycle", null);

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void setBucketVersioningConfiguration(
            SetBucketVersioningConfigurationRequest setBucketVersioningConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketVersioningConfigurationRequest,
                "The SetBucketVersioningConfigurationRequest object must be specified when setting versioning configuration");

        String bucketName = setBucketVersioningConfigurationRequest.getBucketName();
        BucketVersioningConfiguration versioningConfiguration =
                setBucketVersioningConfigurationRequest.getVersioningConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting versioning configuration");
        rejectNull(versioningConfiguration,
                "The bucket versioning parameter must be specified when setting versioning configuration");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when setting versioning configuration");

        CosHttpRequest<SetBucketVersioningConfigurationRequest> request = createRequest(bucketName,
                null, setBucketVersioningConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("versioning", null);


        byte[] bytes =
                new BucketConfigurationXmlFactory().convertToXmlByteArray(versioningConfiguration);
        request.setContent(new ByteArrayInputStream(bytes));

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public BucketVersioningConfiguration getBucketVersioningConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketVersioningConfiguration(
                new GetBucketVersioningConfigurationRequest(bucketName));
    }

    @Override
    public BucketVersioningConfiguration getBucketVersioningConfiguration(
            GetBucketVersioningConfigurationRequest getBucketVersioningConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketVersioningConfigurationRequest,
                "The request object parameter getBucketVersioningConfigurationRequest must be specified.");
        String bucketName = getBucketVersioningConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when querying versioning configuration");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when querying versioning configuration");

        CosHttpRequest<GetBucketVersioningConfigurationRequest> request = createRequest(bucketName,
                null, getBucketVersioningConfigurationRequest, HttpMethodName.GET);
        request.addParameter("versioning", null);

        return invoke(request, new Unmarshallers.BucketVersioningConfigurationUnmarshaller());
    }

    @Override
    public void setObjectAcl(String bucketName, String key, AccessControlList acl)
            throws CosClientException, CosServiceException {
        setObjectAcl(new SetObjectAclRequest(bucketName, key, acl));
    }

    @Override
    public void setObjectAcl(String bucketName, String key, CannedAccessControlList acl)
            throws CosClientException, CosServiceException {
        setObjectAcl(new SetObjectAclRequest(bucketName, key, acl));
    }

    @Override
    public void setObjectAcl(SetObjectAclRequest setObjectAclRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setObjectAclRequest, "The request must not be null.");
        rejectNull(setObjectAclRequest.getBucketName(),
                "The bucket name parameter must be specified when setting an object's ACL");
        rejectNull(setObjectAclRequest.getKey(),
                "The key parameter must be specified when setting an object's ACL");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when setting an object acl");

        if (setObjectAclRequest.getAcl() != null && setObjectAclRequest.getCannedAcl() != null) {
            throw new IllegalArgumentException(
                    "Only one of the ACL and CannedACL parameters can be specified, not both.");
        }

        if (setObjectAclRequest.getAcl() != null) {
            setAcl(setObjectAclRequest.getBucketName(), setObjectAclRequest.getKey(), null,
                    setObjectAclRequest.getAcl(), setObjectAclRequest);

        } else if (setObjectAclRequest.getCannedAcl() != null) {
            setAcl(setObjectAclRequest.getBucketName(), setObjectAclRequest.getKey(),
                    setObjectAclRequest.getVersionId(), setObjectAclRequest.getCannedAcl(),
                    setObjectAclRequest);

        } else {
            throw new IllegalArgumentException(
                    "At least one of the ACL and CannedACL parameters should be specified");
        }
    }

    @Override
    public AccessControlList getObjectAcl(String bucketName, String key)
            throws CosClientException, CosServiceException {
        return getObjectAcl(new GetObjectAclRequest(bucketName, key));
    }

    @Override
    public AccessControlList getObjectAcl(GetObjectAclRequest getObjectAclRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getObjectAclRequest,
                "The request parameter must be specified when requesting an object's ACL");
        rejectNull(getObjectAclRequest.getBucketName(),
                "The bucket name parameter must be specified when requesting an object's ACL");
        rejectNull(getObjectAclRequest.getKey(),
                "The key parameter must be specified when requesting an object's ACL");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when requesting an object acl");

        return getAcl(getObjectAclRequest.getBucketName(), getObjectAclRequest.getKey(),
                getObjectAclRequest.getVersionId(), getObjectAclRequest);
    }

    @Override
    public void setBucketAcl(String bucketName, AccessControlList acl)
            throws CosClientException, CosServiceException {
        setBucketAcl(new SetBucketAclRequest(bucketName, acl));
    }

    @Override
    public void setBucketAcl(String bucketName, CannedAccessControlList acl)
            throws CosClientException, CosServiceException {
        setBucketAcl(new SetBucketAclRequest(bucketName, acl));
    }

    @Override
    public void setBucketAcl(SetBucketAclRequest setBucketAclRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketAclRequest,
                "The SetBucketAclRequest parameter must be specified when setting a bucket's ACL");
        String bucketName = setBucketAclRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting a bucket's ACL");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when setting a bucket acl");

        AccessControlList acl = setBucketAclRequest.getAcl();
        CannedAccessControlList cannedAcl = setBucketAclRequest.getCannedAcl();

        if (acl == null && cannedAcl == null) {
            throw new IllegalArgumentException(
                    "The ACL parameter must be specified when setting a bucket's ACL");
        }
        if (acl != null && cannedAcl != null) {
            throw new IllegalArgumentException(
                    "Only one of the acl and cannedAcl parameter can be specified, not both.");
        }

        if (acl != null) {
            setAcl(bucketName, null, null, acl, setBucketAclRequest);
        } else {
            setAcl(bucketName, null, null, cannedAcl, setBucketAclRequest);
        }
    }

    @Override
    public AccessControlList getBucketAcl(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketAcl(new GetBucketAclRequest(bucketName));
    }

    @Override
    public AccessControlList getBucketAcl(GetBucketAclRequest getBucketAclRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketAclRequest,
                "The bucket name parameter must be specified when requesting a bucket's ACL");
        String bucketName = getBucketAclRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when requesting a bucket's ACL");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when requesting a bucket acl");

        return getAcl(bucketName, null, null, getBucketAclRequest);
    }

    /**
     * <p>
     * Gets the AccessControlList for the specified resource. (bucket if only the bucketName
     * parameter is specified, otherwise the object with the specified key in the bucket).
     * </p>
     *
     * @param bucketName The name of the bucket whose ACL should be returned if the key parameter is
     *        not specified, otherwise the bucket containing the specified key.
     * @param key The object key whose ACL should be retrieve. If not specified, the bucket's ACL is
     *        returned.
     * @param versionId The version ID of the object version whose ACL is being retrieved.
     * @param originalRequest The original, user facing request object.
     *
     * @return The ACL for the specified resource.
     */
    private AccessControlList getAcl(String bucketName, String key, String versionId,
            CosServiceRequest originalRequest) {
        if (originalRequest == null)
            originalRequest = new GenericBucketRequest(bucketName);

        CosHttpRequest<CosServiceRequest> request =
                createRequest(bucketName, key, originalRequest, HttpMethodName.GET);
        request.addParameter("acl", null);
        if (versionId != null) {
            request.addParameter("versionId", versionId);
        }

        @SuppressWarnings("unchecked")
        ResponseHeaderHandlerChain<AccessControlList> responseHandler =
                new ResponseHeaderHandlerChain<AccessControlList>(
                        new Unmarshallers.AccessControlListUnmarshaller(), new COSDefaultAclHeaderHandler());

        return invoke(request, responseHandler);
    }


    /**
     * Sets the ACL for the specified resource in COS. If only bucketName is specified, the ACL will
     * be applied to the bucket, otherwise if bucketName and key are specified, the ACL will be
     * applied to the object.
     *
     * @param bucketName The name of the bucket containing the specified key, or if no key is
     *        listed, the bucket whose ACL will be set.
     * @param key The optional object key within the specified bucket whose ACL will be set. If not
     *        specified, the bucket ACL will be set.
     * @param versionId The version ID of the object version whose ACL is being set.
     * @param acl The ACL to apply to the resource.
     * @param originalRequest The original, user facing request object.
     */
    private void setAcl(String bucketName, String key, String versionId, AccessControlList acl,
            CosServiceRequest originalRequest) {
        if (originalRequest == null)
            originalRequest = new GenericBucketRequest(bucketName);

        CosHttpRequest<CosServiceRequest> request =
                createRequest(bucketName, key, originalRequest, HttpMethodName.PUT);
        request.addParameter("acl", null);
        if (versionId != null)
            request.addParameter("versionId", versionId);

        byte[] aclAsXml = new AclXmlFactory().convertToXmlByteArray(acl);
        request.addHeader("Content-Type", "application/xml");
        request.addHeader("Content-Length", String.valueOf(aclAsXml.length));
        request.setContent(new ByteArrayInputStream(aclAsXml));

        invoke(request, voidCosResponseHandler);
    }

    private void setAcl(String bucketName, String key, String versionId,
            CannedAccessControlList cannedAcl, CosServiceRequest originalRequest)
            throws CosClientException, CosServiceException {
        if (originalRequest == null)
            originalRequest = new GenericBucketRequest(bucketName);

        CosHttpRequest<CosServiceRequest> request =
                createRequest(bucketName, key, originalRequest, HttpMethodName.PUT);
        request.addParameter("acl", null);
        request.addHeader(Headers.COS_CANNED_ACL, cannedAcl.toString());
        if (versionId != null)
            request.addParameter("versionId", versionId);

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketCrossOriginConfiguration(
                new GetBucketCrossOriginConfigurationRequest(bucketName));
    }

    @Override
    public BucketCrossOriginConfiguration getBucketCrossOriginConfiguration(
            GetBucketCrossOriginConfigurationRequest getBucketCrossOriginConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketCrossOriginConfigurationRequest,
                "The request object parameter getBucketCrossOriginConfigurationRequest must be specified.");
        String bucketName = getBucketCrossOriginConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name must be specified when retrieving the bucket cross origin configuration.");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when retrieving the bucket cross origin configuration");

        CosHttpRequest<GetBucketCrossOriginConfigurationRequest> request = createRequest(bucketName,
                null, getBucketCrossOriginConfigurationRequest, HttpMethodName.GET);
        request.addParameter("cors", null);

        try {
            return invoke(request, new Unmarshallers.BucketCrossOriginConfigurationUnmarshaller());
        } catch (CosServiceException cse) {
            switch (cse.getStatusCode()) {
                case 404:
                    return null;
                default:
                    throw cse;
            }
        }
    }

    @Override
    public void setBucketCrossOriginConfiguration(String bucketName,
            BucketCrossOriginConfiguration bucketCrossOriginConfiguration)
            throws CosClientException, CosServiceException {
        setBucketCrossOriginConfiguration(new SetBucketCrossOriginConfigurationRequest(bucketName,
                bucketCrossOriginConfiguration));;
    }

    @Override
    public void setBucketCrossOriginConfiguration(
            SetBucketCrossOriginConfigurationRequest setBucketCrossOriginConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketCrossOriginConfigurationRequest,
                "The set bucket cross origin configuration request object must be specified.");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when setting bucket cross origin configuration");

        String bucketName = setBucketCrossOriginConfigurationRequest.getBucketName();
        BucketCrossOriginConfiguration bucketCrossOriginConfiguration =
                setBucketCrossOriginConfigurationRequest.getCrossOriginConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting bucket cross origin configuration.");
        rejectNull(bucketCrossOriginConfiguration,
                "The cross origin configuration parameter must be specified when setting bucket cross origin configuration.");

        CosHttpRequest<SetBucketCrossOriginConfigurationRequest> request = createRequest(bucketName,
                null, setBucketCrossOriginConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("cors", null);

        byte[] content = new BucketConfigurationXmlFactory()
                .convertToXmlByteArray(bucketCrossOriginConfiguration);
        request.addHeader("Content-Length", String.valueOf(content.length));
        request.addHeader("Content-Type", "application/xml");
        request.setContent(new ByteArrayInputStream(content));
        try {
            byte[] md5 = Md5Utils.computeMD5Hash(content);
            String md5Base64 = BinaryUtils.toBase64(md5);
            request.addHeader("Content-MD5", md5Base64);
        } catch (Exception e) {
            throw new CosClientException("Couldn't compute md5 sum", e);
        }

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void deleteBucketCrossOriginConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        deleteBucketCrossOriginConfiguration(
                new DeleteBucketCrossOriginConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketCrossOriginConfiguration(
            DeleteBucketCrossOriginConfigurationRequest deleteBucketCrossOriginConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(deleteBucketCrossOriginConfigurationRequest,
                "The delete bucket cross origin configuration request object must be specified.");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when deleting bucket cross origin configuration");

        String bucketName = deleteBucketCrossOriginConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when deleting bucket cross origin configuration.");

        CosHttpRequest<DeleteBucketCrossOriginConfigurationRequest> request =
                createRequest(bucketName, null, deleteBucketCrossOriginConfigurationRequest,
                        HttpMethodName.DELETE);
        request.addParameter("cors", null);
        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void setBucketReplicationConfiguration(String bucketName,
            BucketReplicationConfiguration configuration)
            throws CosClientException, CosServiceException {
        setBucketReplicationConfiguration(
                new SetBucketReplicationConfigurationRequest(bucketName, configuration));
    }

    @Override
    public void setBucketReplicationConfiguration(
            SetBucketReplicationConfigurationRequest setBucketReplicationConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketReplicationConfigurationRequest,
                "The set bucket replication configuration request object must be specified.");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when setting bucket replication configuration");

        final String bucketName = setBucketReplicationConfigurationRequest.getBucketName();

        final BucketReplicationConfiguration bucketReplicationConfiguration =
                setBucketReplicationConfigurationRequest.getReplicationConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting replication configuration.");
        rejectNull(bucketReplicationConfiguration,
                "The replication configuration parameter must be specified when setting replication configuration.");

        CosHttpRequest<SetBucketReplicationConfigurationRequest> request = createRequest(bucketName,
                null, setBucketReplicationConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("replication", null);

        final byte[] bytes = new BucketConfigurationXmlFactory()
                .convertToXmlByteArray(bucketReplicationConfiguration);

        request.addHeader("Content-Length", String.valueOf(bytes.length));
        request.addHeader("Content-Type", "application/xml");
        request.setContent(new ByteArrayInputStream(bytes));


        try {
            request.addHeader("Content-MD5", BinaryUtils.toBase64(Md5Utils.computeMD5Hash(bytes)));
        } catch (Exception e) {
            throw new CosClientException(
                    "Not able to compute MD5 of the replication rule configuration. Exception Message : "
                            + e.getMessage(),
                    e);
        }
        invoke(request, voidCosResponseHandler);
    }

    @Override
    public BucketReplicationConfiguration getBucketReplicationConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketReplicationConfiguration(
                new GetBucketReplicationConfigurationRequest(bucketName));
    }

    @Override
    public BucketReplicationConfiguration getBucketReplicationConfiguration(
            GetBucketReplicationConfigurationRequest getBucketReplicationConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketReplicationConfigurationRequest,
                "The bucket request parameter must be specified when retrieving replication configuration");
        String bucketName = getBucketReplicationConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket request must specify a bucket name when retrieving replication configuration");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when retrieving replication configuration");

        CosHttpRequest<GetBucketReplicationConfigurationRequest> request = createRequest(bucketName,
                null, getBucketReplicationConfigurationRequest, HttpMethodName.GET);
        request.addParameter("replication", null);

        return invoke(request, new Unmarshallers.BucketReplicationConfigurationUnmarshaller());
    }

    @Override
    public void deleteBucketReplicationConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        deleteBucketReplicationConfiguration(
                new DeleteBucketReplicationConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketReplicationConfiguration(
            DeleteBucketReplicationConfigurationRequest deleteBucketReplicationConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(deleteBucketReplicationConfigurationRequest,
                "The DeleteBucketReplicationConfigurationRequest parameter must be specified when deleting replication configuration");
        final String bucketName = deleteBucketReplicationConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when deleting replication configuration");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when deleting replication configuration");

        CosHttpRequest<DeleteBucketReplicationConfigurationRequest> request =
                createRequest(bucketName, null, deleteBucketReplicationConfigurationRequest,
                        HttpMethodName.DELETE);
        request.addParameter("replication", null);

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration, HttpMethodName method) throws CosClientException {
        return generatePresignedUrl(bucketName, key, expiration, method, new HashMap<String, String>(), new HashMap<String, String>(), false, true);
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration,
            HttpMethodName method, Map<String, String> headers, Map<String, String> params) throws CosClientException {
        return generatePresignedUrl(bucketName, key, expiration, method, headers, params, false, true);
    }

    @Override
    public URL generatePresignedUrl(String bucketName, String key, Date expiration,
            HttpMethodName method, Map<String, String> headers, Map<String, String> params, Boolean signPrefixMode,
            Boolean signHost) throws CosClientException {

        GeneratePresignedUrlRequest request =
                new GeneratePresignedUrlRequest(bucketName, key, method);
        request.setExpiration(expiration);

        for (Entry<String, String> entry : params.entrySet()) {
            request.addRequestParameter(entry.getKey(), entry.getValue());
        }

        if (signHost) {
            request.putCustomRequestHeader(Headers.HOST, this.clientConfig.getEndpointBuilder().buildGeneralApiEndpoint(bucketName));
        }

        for (Entry<String, String> entry : headers.entrySet()) {
            request.putCustomRequestHeader(entry.getKey(), entry.getValue());
        }

        request.setSignPrefixMode(signPrefixMode);

        return generatePresignedUrl(request, signHost);
    }

    @Override
    public URL generatePresignedUrl(GeneratePresignedUrlRequest req) throws CosClientException {
        return generatePresignedUrl(req, true);
    }

    @Override
    public URL generatePresignedUrl(GeneratePresignedUrlRequest req, Boolean signHost) throws CosClientException {
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when generating a pre-signed URL");
        rejectNull(req, "The request parameter must be specified when generating a pre-signed URL");
        req.rejectIllegalArguments();

        final String bucketName = req.getBucketName();
        final String key = req.getKey();

        if (req.getExpiration() == null) {
            req.setExpiration(new Date(
                    System.currentTimeMillis() + this.clientConfig.getSignExpired() * 1000));
        }

        HttpMethodName httpMethod = req.getMethod();

        CosHttpRequest<GeneratePresignedUrlRequest> request =
                createRequest(bucketName, key, req, httpMethod);

        addParameterIfNotNull(request, "versionId", req.getVersionId());

        for (Entry<String, String> entry : req.getRequestParameters().entrySet()) {
            request.addParameter(entry.getKey(), entry.getValue());
        }

        addHeaderIfNotNull(request, Headers.CONTENT_TYPE, req.getContentType());
        addHeaderIfNotNull(request, Headers.CONTENT_MD5, req.getContentMd5());

        // Custom headers that open up the possibility of supporting unexpected
        // cases.
        Map<String, String> customHeaders = req.getCustomRequestHeaders();
        if (customHeaders != null) {
            for (Map.Entry<String, String> e : customHeaders.entrySet()) {
                request.addHeader(e.getKey(), e.getValue());
            }
        }

        addResponseHeaderParameters(request, req.getResponseHeaders());

        COSSigner cosSigner = new COSSigner();
        COSCredentials cred = fetchCredential();
        String authStr =
                cosSigner.buildAuthorizationStr(request.getHttpMethod(), request.getResourcePath(),
                        request.getHeaders(), request.getParameters(), cred, req.getExpiration(), signHost);
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(clientConfig.getHttpProtocol().toString()).append("://");
        strBuilder.append(clientConfig.getEndpointBuilder()
                .buildGeneralApiEndpoint(formatBucket(bucketName, cred.getCOSAppId())));
        strBuilder.append(UrlEncoderUtils.encodeUrlPath(formatKey(key)));

        boolean hasAppendFirstParameter = false;
        if (authStr != null) {
            if(req.isSignPrefixMode()) {
                strBuilder.append("?sign=").append(UrlEncoderUtils.encode(authStr));
            } else {

                // urlencode auth string key & value
                String[] authParts = authStr.split("&");
                String[] encodeAuthParts = new String[authParts.length];

                for (int i = 0; i < authParts.length; i++) {
                    String[] kv = authParts[i].split("=", 2);
                    if (kv.length == 2) {
                        encodeAuthParts[i] = StringUtils.join("=", UrlEncoderUtils.encode(kv[0]), UrlEncoderUtils.encode(kv[1]));
                    } else if (kv.length == 1) {
                        encodeAuthParts[i] = StringUtils.join("=", UrlEncoderUtils.encode(kv[0]));
                    }
                }

                authStr = StringUtils.join("&", encodeAuthParts);

                strBuilder.append("?").append(authStr);
            }
            if (cred instanceof COSSessionCredentials) {
                strBuilder.append("&").append(Headers.SECURITY_TOKEN).append("=").append(
                        UrlEncoderUtils.encode(((COSSessionCredentials) cred).getSessionToken()));
            }
            hasAppendFirstParameter = true;
        }

        for (Entry<String, String> entry : request.getParameters().entrySet()) {
            String paramKey = entry.getKey();
            String paramValue = entry.getValue();

            if (!hasAppendFirstParameter) {
                strBuilder.append("?");
                hasAppendFirstParameter = true;
            } else {
                strBuilder.append("&");
            }
            strBuilder.append(UrlEncoderUtils.encode(paramKey));
            if (paramValue != null) {
                strBuilder.append("=").append(UrlEncoderUtils.encode(paramValue));
            }
        }

        try {
            return new URL(strBuilder.toString());
        } catch (MalformedURLException e) {
            throw new CosClientException(e.toString());
        }
    }

    @Override
    public void restoreObject(String bucketName, String key, int expirationInDays)
            throws CosClientException, CosServiceException {
        restoreObject(new RestoreObjectRequest(bucketName, key, expirationInDays));
    }

    @Override
    public void restoreObject(RestoreObjectRequest restoreObjectRequest)
            throws CosClientException, CosServiceException {
        rejectNull(restoreObjectRequest,
                "The RestoreObjectRequest parameter must be specified when restore a object.");
        String bucketName = restoreObjectRequest.getBucketName();
        String key = restoreObjectRequest.getKey();
        String versionId = restoreObjectRequest.getVersionId();
        int expirationIndays = restoreObjectRequest.getExpirationInDays();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when copying a cas object");
        rejectNull(key, "The key parameter must be specified when copying a cas object");
        if (expirationIndays == -1) {
            throw new IllegalArgumentException(
                    "The expiration in days parameter must be specified when copying a cas object");
        }

        CosHttpRequest<RestoreObjectRequest> request =
                createRequest(bucketName, key, restoreObjectRequest, HttpMethodName.POST);
        request.addParameter("restore", null);
        addParameterIfNotNull(request, "versionId", versionId);


        byte[] content = RequestXmlFactory.convertToXmlByteArray(restoreObjectRequest);
        request.addHeader("Content-Length", String.valueOf(content.length));
        request.addHeader("Content-Type", "application/xml");
        request.setContent(new ByteArrayInputStream(content));
        try {
            byte[] md5 = Md5Utils.computeMD5Hash(content);
            String md5Base64 = BinaryUtils.toBase64(md5);
            request.addHeader("Content-MD5", md5Base64);
        } catch (Exception e) {
            throw new CosClientException("Couldn't compute md5 sum", e);
        }

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void updateObjectMetaData(String bucketName, String key, ObjectMetadata objectMetadata)
            throws CosClientException, CosServiceException {
        CopyObjectRequest copyObjectRequest =
                new CopyObjectRequest(bucketName, key, bucketName, key);
        copyObjectRequest.setNewObjectMetadata(objectMetadata);
        copyObject(copyObjectRequest);
    }

    @Override
    public void setBucketPolicy(String bucketName, String policyText)
            throws CosClientException, CosServiceException {
        setBucketPolicy(new SetBucketPolicyRequest(bucketName, policyText));
    }

    @Override
    public BucketLoggingConfiguration getBucketLoggingConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketLoggingConfiguration(new GetBucketLoggingConfigurationRequest(bucketName));
    }

    @Override
    public BucketLoggingConfiguration getBucketLoggingConfiguration(GetBucketLoggingConfigurationRequest getBucketLoggingConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketLoggingConfigurationRequest, "The request object parameter getBucketLoggingConfigurationRequest must be specifed.");
        String bucketName = getBucketLoggingConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when requesting a bucket's logging status");

        CosHttpRequest<GetBucketLoggingConfigurationRequest> request = createRequest(bucketName, null, getBucketLoggingConfigurationRequest, HttpMethodName.GET);
        request.addParameter("logging", null);

        return invoke(request, new Unmarshallers.BucketLoggingConfigurationnmarshaller());
    }

    @Override
    public void setBucketLoggingConfiguration(SetBucketLoggingConfigurationRequest setBucketLoggingConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketLoggingConfigurationRequest,
                "The set bucket logging configuration request object must be specified when enabling server access logging");

        String bucketName = setBucketLoggingConfigurationRequest.getBucketName();
        BucketLoggingConfiguration loggingConfiguration = setBucketLoggingConfigurationRequest.getLoggingConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when enabling server access logging");
        rejectNull(loggingConfiguration,
                "The logging configuration parameter must be specified when enabling server access logging");

        CosHttpRequest<SetBucketLoggingConfigurationRequest> request = createRequest(bucketName, null, setBucketLoggingConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("logging", null);

        byte[] bytes = new BucketConfigurationXmlFactory().convertToXmlByteArray(loggingConfiguration);
        request.setContent(new ByteArrayInputStream(bytes));

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void setBucketPolicy(SetBucketPolicyRequest setBucketPolicyRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketPolicyRequest,
                "The request object must be specified when setting a bucket policy");

        String bucketName = setBucketPolicyRequest.getBucketName();
        String policyText = setBucketPolicyRequest.getPolicyText();

        rejectNull(bucketName, "The bucket name must be specified when setting a bucket policy");
        rejectNull(policyText, "The policy text must be specified when setting a bucket policy");

        CosHttpRequest<SetBucketPolicyRequest> request =
                createRequest(bucketName, null, setBucketPolicyRequest, HttpMethodName.PUT);
        request.addParameter("policy", null);
        request.setContent(new ByteArrayInputStream(policyText.getBytes(StringUtils.UTF8)));

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public BucketPolicy getBucketPolicy(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketPolicy(new GetBucketPolicyRequest(bucketName));
    }

    @Override
    public BucketPolicy getBucketPolicy(GetBucketPolicyRequest getBucketPolicyRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketPolicyRequest,
                "The request object must be specified when getting a bucket policy");

        String bucketName = getBucketPolicyRequest.getBucketName();
        rejectNull(bucketName, "The bucket name must be specified when getting a bucket policy");

        CosHttpRequest<GetBucketPolicyRequest> request =
                createRequest(bucketName, null, getBucketPolicyRequest, HttpMethodName.GET);
        request.addParameter("policy", null);

        BucketPolicy result = new BucketPolicy();
        try {
            String policyText = invoke(request, new COSStringResponseHandler());
            result.setPolicyText(policyText);
            return result;
        } catch (CosServiceException cse) {
            if (cse.getErrorCode().equals("NoSuchBucketPolicy"))
                return result;
            throw cse;
        }
    }


    @Override
    public void deleteBucketPolicy(String bucketName)
            throws CosClientException, CosServiceException {
        deleteBucketPolicy(new DeleteBucketPolicyRequest(bucketName));
    }

    @Override
    public void deleteBucketPolicy(DeleteBucketPolicyRequest deleteBucketPolicyRequest)
            throws CosClientException, CosServiceException {
        rejectNull(deleteBucketPolicyRequest,
                "The request object must be specified when deleting a bucket policy");

        String bucketName = deleteBucketPolicyRequest.getBucketName();
        rejectNull(bucketName, "The bucket name must be specified when deleting a bucket policy");

        CosHttpRequest<DeleteBucketPolicyRequest> request =
                createRequest(bucketName, null, deleteBucketPolicyRequest, HttpMethodName.DELETE);
        request.addParameter("policy", null);

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketWebsiteConfiguration(new GetBucketWebsiteConfigurationRequest(bucketName));
    }

    @Override
    public BucketWebsiteConfiguration getBucketWebsiteConfiguration(GetBucketWebsiteConfigurationRequest getBucketWebsiteConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketWebsiteConfigurationRequest, "The request object parameter getBucketWebsiteConfigurationRequest must be specified.");
        String bucketName = getBucketWebsiteConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when requesting a bucket's website configuration");

        CosHttpRequest<GetBucketWebsiteConfigurationRequest> request = createRequest(bucketName, null, getBucketWebsiteConfigurationRequest, HttpMethodName.GET);
        request.addParameter("website", null);
        request.addHeader("Content-Type", "application/xml");

        try {
            return invoke(request, new Unmarshallers.BucketWebsiteConfigurationUnmarshaller());
        } catch (CosServiceException ase) {
            if (ase.getStatusCode() == 404) return null;
            throw ase;
        }
    }

    @Override
    public void setBucketWebsiteConfiguration(String bucketName, BucketWebsiteConfiguration configuration)
            throws CosClientException, CosServiceException {
        setBucketWebsiteConfiguration(new SetBucketWebsiteConfigurationRequest(bucketName, configuration));
    }

    @Override
    public void setBucketWebsiteConfiguration(SetBucketWebsiteConfigurationRequest setBucketWebsiteConfigurationRequest)
            throws CosClientException, CosServiceException {
        String bucketName = setBucketWebsiteConfigurationRequest.getBucketName();
        BucketWebsiteConfiguration configuration = setBucketWebsiteConfigurationRequest.getConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting a bucket's website configuration");
        rejectNull(configuration,
                "The bucket website configuration parameter must be specified when setting a bucket's website configuration");
        if (configuration.getRedirectAllRequestsTo() == null) {
            rejectNull(configuration.getIndexDocumentSuffix(),
                    "The bucket website configuration parameter must specify the index document suffix when setting a bucket's website configuration");
        }

        CosHttpRequest<SetBucketWebsiteConfigurationRequest> request = createRequest(bucketName, null, setBucketWebsiteConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("website", null);
        request.addHeader("Content-Type", "application/xml");

        byte[] bytes = new BucketConfigurationXmlFactory().convertToXmlByteArray(configuration);
        request.setContent(new ByteArrayInputStream(bytes));

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void deleteBucketWebsiteConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        deleteBucketWebsiteConfiguration(new DeleteBucketWebsiteConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketWebsiteConfiguration(DeleteBucketWebsiteConfigurationRequest deleteBucketWebsiteConfigurationRequest)
            throws CosClientException, CosServiceException {
        String bucketName = deleteBucketWebsiteConfigurationRequest.getBucketName();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when deleting a bucket's website configuration");

        CosHttpRequest<DeleteBucketWebsiteConfigurationRequest> request = createRequest(bucketName, null, deleteBucketWebsiteConfigurationRequest, HttpMethodName.DELETE);
        request.addParameter("website", null);
        request.addHeader("Content-Type", "application/xml");

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void deleteBucketDomainConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        deleteBucketDomainConfiguration(new DeleteBucketDomainConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketDomainConfiguration(DeleteBucketDomainConfigurationRequest deleteBucketDomainConfigurationRequest) {
        String bucketName = deleteBucketDomainConfigurationRequest.getBucketName();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when deleting a bucket's domain configuration");

        CosHttpRequest<DeleteBucketDomainConfigurationRequest> request = createRequest(bucketName, null, deleteBucketDomainConfigurationRequest, HttpMethodName.DELETE);
        request.addParameter("domain", null);

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void setBucketDomainConfiguration(String bucketName, BucketDomainConfiguration configuration)
            throws CosClientException, CosServiceException {
        setBucketDomainConfiguration(new SetBucketDomainConfigurationRequest(bucketName, configuration));
    }

    @Override
    public void setBucketDomainConfiguration(SetBucketDomainConfigurationRequest setBucketDomainConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketDomainConfigurationRequest,
                "The request object parameter setBucketDomainConfigurationRequest must be specified.");
        String bucketName = setBucketDomainConfigurationRequest.getBucketName();
        BucketDomainConfiguration configuration = setBucketDomainConfigurationRequest.getConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting a bucket's domain configuration");
        rejectNull(configuration,
                "The bucket domain configuration parameter must be specified when setting a bucket's domain configuration");
        rejectNull(configuration.getDomainRules(),
                "The bucket domain rules must specify the index document suffix when setting a bucket's domain configuration");

        CosHttpRequest<SetBucketDomainConfigurationRequest> request = createRequest(bucketName,
                null, setBucketDomainConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("domain", null);
        request.addHeader("Content-Type", "application/xml");

        byte[] bytes = new BucketConfigurationXmlFactory().convertToXmlByteArray(configuration);
        request.setContent(new ByteArrayInputStream(bytes));

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public BucketDomainConfiguration getBucketDomainConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketDomainConfiguration(new GetBucketDomainConfigurationRequest(bucketName));
    }

    @Override
    public BucketDomainConfiguration getBucketDomainConfiguration(GetBucketDomainConfigurationRequest getBucketDomainConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketDomainConfigurationRequest,
                "The request object parameter getBucketDomainConfigurationRequest must be specified.");
        String bucketName = getBucketDomainConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name must be specified when retrieving the bucket domain configuration.");

        CosHttpRequest<GetBucketDomainConfigurationRequest> request = createRequest(bucketName,
                null, getBucketDomainConfigurationRequest, HttpMethodName.GET);
        request.addParameter("domain", null);

        try {
            return invoke(request, new Unmarshallers.BucketDomainConfigurationUnmarshaller());
        } catch (CosServiceException cse) {
            switch (cse.getStatusCode()) {
                case 404:
                    return null;
                default:
                    throw cse;
            }
        }
    }

    @Override
    public void setBucketRefererConfiguration(String bucketName, BucketRefererConfiguration configuration)
            throws CosClientException, CosServiceException {
        setBucketRefererConfiguration(new SetBucketRefererConfigurationRequest(bucketName, configuration));
    }

    @Override
    public void setBucketRefererConfiguration(SetBucketRefererConfigurationRequest setBucketRefererConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketRefererConfigurationRequest,
                "The request object parameter setBucketRefererConfigurationRequest must be specified.");
        String bucketName = setBucketRefererConfigurationRequest.getBucketName();
        BucketRefererConfiguration configuration = setBucketRefererConfigurationRequest.getConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting a bucket's referer configuration");
        rejectNull(configuration,
                "The bucket domain configuration parameter must be specified when setting a bucket's referer configuration");

        CosHttpRequest<SetBucketRefererConfigurationRequest> request = createRequest(bucketName,
                null, setBucketRefererConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("referer", null);
        request.addHeader("Content-Type", "application/xml");

        byte[] bytes = new BucketConfigurationXmlFactory().convertToXmlByteArray(configuration);
        request.setContent(new ByteArrayInputStream(bytes));

        try {
            byte[] md5 = Md5Utils.computeMD5Hash(bytes);
            String md5Base64 = BinaryUtils.toBase64(md5);
            request.addHeader("Content-MD5", md5Base64);
        } catch ( Exception e ) {
            throw new CosClientException("Couldn't compute md5 sum", e);
        }

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public BucketRefererConfiguration getBucketRefererConfiguration(String bucketName)
            throws CosClientException, CosServiceException {
        return getBucketRefererConfiguration(new GetBucketRefererConfigurationRequest(bucketName));
    }

    @Override
    public BucketRefererConfiguration getBucketRefererConfiguration(GetBucketRefererConfigurationRequest getBucketRefererConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketRefererConfigurationRequest,
                "The request object parameter getBucketRefererConfigurationRequest must be specified.");
        String bucketName = getBucketRefererConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name must be specified when retrieving the bucket domain configuration.");

        CosHttpRequest<GetBucketRefererConfigurationRequest> request = createRequest(bucketName,
                null, getBucketRefererConfigurationRequest, HttpMethodName.GET);
        request.addParameter("referer", null);

        try {
            return invoke(request, new Unmarshallers.BucketRefererConfigurationUnmarshaller());
        } catch (CosServiceException cse) {
            switch (cse.getStatusCode()) {
                case 404:
                    return null;
                default:
                    throw cse;
            }
        }
    }

    @Override
    public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration(
            String bucketName, String id) throws CosClientException, CosServiceException {
        return deleteBucketInventoryConfiguration(
                new DeleteBucketInventoryConfigurationRequest(bucketName, id));
    }

    @Override
    public DeleteBucketInventoryConfigurationResult deleteBucketInventoryConfiguration(
            DeleteBucketInventoryConfigurationRequest deleteBucketInventoryConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(deleteBucketInventoryConfigurationRequest, "The request cannot be null");
        rejectNull(deleteBucketInventoryConfigurationRequest.getBucketName(), "The bucketName cannot be null");
        rejectNull(deleteBucketInventoryConfigurationRequest.getId(), "The id cannot be null");
        String bucketName = deleteBucketInventoryConfigurationRequest.getBucketName();
        String id = deleteBucketInventoryConfigurationRequest.getId();

        CosHttpRequest<DeleteBucketInventoryConfigurationRequest> request = createRequest(bucketName, null, deleteBucketInventoryConfigurationRequest, HttpMethodName.DELETE);
        request.addParameter("inventory", null);
        request.addParameter("id", id);

        return invoke(request, new Unmarshallers.DeleteBucketInventoryConfigurationUnmarshaller());
    }

    @Override
    public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(
            String bucketName, String id) throws CosClientException, CosServiceException {
        return getBucketInventoryConfiguration(
                new GetBucketInventoryConfigurationRequest(bucketName, id));
    }

    @Override
    public GetBucketInventoryConfigurationResult getBucketInventoryConfiguration(
            GetBucketInventoryConfigurationRequest getBucketInventoryConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(getBucketInventoryConfigurationRequest, "The request cannot be null");
        rejectNull(getBucketInventoryConfigurationRequest.getBucketName(), "The bucketName cannot be null");
        rejectNull(getBucketInventoryConfigurationRequest.getId(), "The id cannot be null");
        String bucketName = getBucketInventoryConfigurationRequest.getBucketName();
        String id = getBucketInventoryConfigurationRequest.getId();

        CosHttpRequest<GetBucketInventoryConfigurationRequest> request = createRequest(bucketName, null, getBucketInventoryConfigurationRequest, HttpMethodName.GET);
        request.addParameter("inventory", null);
        request.addParameter("id", id);

        return invoke(request, new Unmarshallers.GetBucketInventoryConfigurationUnmarshaller());
    }

    @Override
    public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration(
            String bucketName, InventoryConfiguration inventoryConfiguration)
            throws CosClientException, CosServiceException {
        return setBucketInventoryConfiguration(
                new SetBucketInventoryConfigurationRequest(bucketName, inventoryConfiguration));
    }

    @Override
    public SetBucketInventoryConfigurationResult setBucketInventoryConfiguration(
            SetBucketInventoryConfigurationRequest setBucketInventoryConfigurationRequest)
            throws CosClientException, CosServiceException {
        rejectNull(setBucketInventoryConfigurationRequest, "The request cannot be null");
        rejectNull(setBucketInventoryConfigurationRequest.getBucketName(), "The bucketName cannot be null");
        rejectNull(setBucketInventoryConfigurationRequest.getInventoryConfiguration(), "The inventoryConfiguration cannot be null");
        rejectNull(setBucketInventoryConfigurationRequest.getInventoryConfiguration().getId(), "The inventoryConfiguration.id cannot be null");
        final String bucketName = setBucketInventoryConfigurationRequest.getBucketName();
        final InventoryConfiguration inventoryConfiguration = setBucketInventoryConfigurationRequest.getInventoryConfiguration();
        final String id = inventoryConfiguration.getId();

        CosHttpRequest<SetBucketInventoryConfigurationRequest> request = createRequest(bucketName, null, setBucketInventoryConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("inventory", null);
        request.addParameter("id", id);

        final byte[] bytes = new BucketConfigurationXmlFactory().convertToXmlByteArray(inventoryConfiguration);
        request.addHeader("Content-Length", String.valueOf(bytes.length));
        request.addHeader("Content-Type", "application/xml");
        request.setContent(new ByteArrayInputStream(bytes));

        return invoke(request, new Unmarshallers.SetBucketInventoryConfigurationUnmarshaller());
    }

    @Override
    public ListBucketInventoryConfigurationsResult listBucketInventoryConfigurations(ListBucketInventoryConfigurationsRequest listBucketInventoryConfigurationsRequest)
            throws CosClientException, CosServiceException {
        rejectNull(listBucketInventoryConfigurationsRequest, "The request cannot be null");
        rejectNull(listBucketInventoryConfigurationsRequest.getBucketName(), "The bucketName cannot be null");
        final String bucketName = listBucketInventoryConfigurationsRequest.getBucketName();

        CosHttpRequest<ListBucketInventoryConfigurationsRequest> request = createRequest(bucketName, null, listBucketInventoryConfigurationsRequest, HttpMethodName.GET);
        request.addParameter("inventory", null);
        addParameterIfNotNull(request, "continuation-token", listBucketInventoryConfigurationsRequest.getContinuationToken());

        return invoke(request, new Unmarshallers.ListBucketInventoryConfigurationsUnmarshaller());
    }
    @Override
    public BucketTaggingConfiguration getBucketTaggingConfiguration(String bucketName) {
        return getBucketTaggingConfiguration(new GetBucketTaggingConfigurationRequest(bucketName));
    }

    @Override
    public BucketTaggingConfiguration getBucketTaggingConfiguration(GetBucketTaggingConfigurationRequest getBucketTaggingConfigurationRequest) {
        rejectNull(getBucketTaggingConfigurationRequest, "The request object parameter getBucketTaggingConfigurationRequest must be specifed.");
        String bucketName = getBucketTaggingConfigurationRequest.getBucketName();
        rejectNull(bucketName, "The bucket name must be specified when retrieving the bucket tagging configuration.");

        CosHttpRequest<GetBucketTaggingConfigurationRequest> request = createRequest(bucketName, null, getBucketTaggingConfigurationRequest, HttpMethodName.GET);
        request.addParameter("tagging", null);

        try {
            return invoke(request, new Unmarshallers.BucketTaggingConfigurationUnmarshaller());
        } catch (CosServiceException ase) {
            switch (ase.getStatusCode()) {
                case 404:
                    return null;
                default:
                    throw ase;
            }
        }
    }

    @Override
    public void setBucketTaggingConfiguration(String bucketName, BucketTaggingConfiguration bucketTaggingConfiguration) {
        setBucketTaggingConfiguration(new SetBucketTaggingConfigurationRequest(bucketName, bucketTaggingConfiguration));
    }

    @Override
    public void setBucketTaggingConfiguration(
            SetBucketTaggingConfigurationRequest setBucketTaggingConfigurationRequest) {
        rejectNull(setBucketTaggingConfigurationRequest,
                "The set bucket tagging configuration request object must be specified.");

        String bucketName = setBucketTaggingConfigurationRequest.getBucketName();
        BucketTaggingConfiguration bucketTaggingConfiguration = setBucketTaggingConfigurationRequest.getTaggingConfiguration();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when setting bucket tagging configuration.");
        rejectNull(bucketTaggingConfiguration,
                "The tagging configuration parameter must be specified when setting bucket tagging configuration.");

        CosHttpRequest<SetBucketTaggingConfigurationRequest> request = createRequest(bucketName, null, setBucketTaggingConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("tagging", null);

        byte[] content = new BucketConfigurationXmlFactory().convertToXmlByteArray(bucketTaggingConfiguration);
        request.addHeader("Content-Length", String.valueOf(content.length));
        request.addHeader("Content-Type", "application/xml");
        request.setContent(new ByteArrayInputStream(content));
        try {
            byte[] md5 = Md5Utils.computeMD5Hash(content);
            String md5Base64 = BinaryUtils.toBase64(md5);
            request.addHeader("Content-MD5", md5Base64);
        } catch ( Exception e ) {
            throw new CosClientException("Couldn't compute md5 sum", e);
        }

        invoke(request, voidCosResponseHandler);
    }

    @Override
    public void deleteBucketTaggingConfiguration(String bucketName) {
        deleteBucketTaggingConfiguration(new DeleteBucketTaggingConfigurationRequest(bucketName));
    }

    @Override
    public void deleteBucketTaggingConfiguration(
            DeleteBucketTaggingConfigurationRequest deleteBucketTaggingConfigurationRequest) {
        rejectNull(deleteBucketTaggingConfigurationRequest,
                "The delete bucket tagging configuration request object must be specified.");

        String bucketName = deleteBucketTaggingConfigurationRequest.getBucketName();
        rejectNull(bucketName,
                "The bucket name parameter must be specified when deleting bucket tagging configuration.");

        CosHttpRequest<DeleteBucketTaggingConfigurationRequest> request = createRequest(bucketName, null, deleteBucketTaggingConfigurationRequest, HttpMethodName.DELETE);
        request.addParameter("tagging", null);

        invoke(request, voidCosResponseHandler);
    }

    private void setContent(CosHttpRequest<?> request, byte[] content, String contentType, boolean setMd5) {
        request.setContent(new ByteArrayInputStream(content));
        request.addHeader("Content-Length", Integer.toString(content.length));
        request.addHeader("Content-Type", contentType);
        if (setMd5) {
            try {
                byte[] md5 = Md5Utils.computeMD5Hash(content);
                String md5Base64 = BinaryUtils.toBase64(md5);
                request.addHeader("Content-MD5", md5Base64);
            } catch (Exception e) {
                throw new CosClientException("Couldn't compute md5 sum", e);
            }
        }
    }

    @Override
    public SelectObjectContentResult selectObjectContent(SelectObjectContentRequest selectRequest) throws CosClientException, CosServiceException {
        rejectNull(selectRequest, "The request parameter must be specified");

        rejectNull(selectRequest.getBucketName(), "The bucket name parameter must be specified when selecting object content.");
        rejectNull(selectRequest.getKey(), "The key parameter must be specified when selecting object content.");

        CosHttpRequest<SelectObjectContentRequest> request = createRequest(selectRequest.getBucketName(), selectRequest.getKey(), selectRequest, HttpMethodName.POST);
        request.addParameter("select", null);
        request.addParameter("select-type", "2");

        populateSSE_C(request, selectRequest.getSSECustomerKey());

        setContent(request, RequestXmlFactory.convertToXmlByteArray(selectRequest), ContentType.APPLICATION_XML.toString(), true);

        COSObject result = invoke(request, new COSObjectResponseHandler());

        // Hold a reference to this client while the InputStream is still
        // around - otherwise a finalizer in the HttpClient may reset the
        // underlying TCP connection out from under us.
        SdkFilterInputStream resultStream = new ServiceClientHolderInputStream(result.getObjectContent(), this);

        return new SelectObjectContentResult().withPayload(new SelectObjectContentEventStream(resultStream));
    }

    @Override
    public GetObjectTaggingResult getObjectTagging(GetObjectTaggingRequest getObjectTaggingRequest) {
        rejectNull(getObjectTaggingRequest,
                "The request parameter must be specified when getting the object tags");
        rejectNull(getObjectTaggingRequest.getBucketName(),
                "The request bucketName must be specified when getting the object tags");
        rejectNull(getObjectTaggingRequest.getKey(),
                "The request key must be specified when getting the object tags");

        CosHttpRequest<GetObjectTaggingRequest> request = createRequest(getObjectTaggingRequest.getBucketName(),
                getObjectTaggingRequest.getKey(), getObjectTaggingRequest, HttpMethodName.GET);
        request.addParameter("tagging", null);
        addParameterIfNotNull(request, "versionId", getObjectTaggingRequest.getVersionId());

        ResponseHeaderHandlerChain<GetObjectTaggingResult> handlerChain = new ResponseHeaderHandlerChain<GetObjectTaggingResult>(
                new Unmarshallers.GetObjectTaggingResponseUnmarshaller(),
                new GetObjectTaggingResponseHeaderHandler()
        );

        return invoke(request, handlerChain);
    }

    @Override
    public SetObjectTaggingResult setObjectTagging(SetObjectTaggingRequest setObjectTaggingRequest) {
        rejectNull(setObjectTaggingRequest,
                "The request parameter must be specified setting the object tags");
        rejectNull(setObjectTaggingRequest.getBucketName(),
                "The request bucketName must be specified setting the object tags");
        rejectNull(setObjectTaggingRequest.getKey(),
                "The request key must be specified setting the object tags");
        rejectNull(setObjectTaggingRequest.getTagging(),
                "The request tagging must be specified setting the object tags");

        CosHttpRequest<SetObjectTaggingRequest> request = createRequest(setObjectTaggingRequest.getBucketName(),
                setObjectTaggingRequest.getKey(), setObjectTaggingRequest, HttpMethodName.PUT);
        request.addParameter("tagging", null);
        addParameterIfNotNull(request, "versionId", setObjectTaggingRequest.getVersionId());
        byte[] content = new ObjectTaggingXmlFactory().convertToXmlByteArray(setObjectTaggingRequest.getTagging());
        setContent(request, content, "application/xml", true);

        ResponseHeaderHandlerChain<SetObjectTaggingResult> handlerChain = new ResponseHeaderHandlerChain<SetObjectTaggingResult>(
                new Unmarshallers.SetObjectTaggingResponseUnmarshaller(),
                new SetObjectTaggingResponseHeaderHandler()
        );

        return invoke(request, handlerChain);
    }

    @Override
    public DeleteObjectTaggingResult deleteObjectTagging(DeleteObjectTaggingRequest deleteObjectTaggingRequest) {
        rejectNull(deleteObjectTaggingRequest, "The request parameter must be specified when delete the object tags");
        rejectNull(deleteObjectTaggingRequest.getBucketName(),
                "The request bucketName must be specified setting the object tags");
        rejectNull(deleteObjectTaggingRequest.getKey(),
                "The request key must be specified setting the object tags");

        CosHttpRequest<DeleteObjectTaggingRequest> request = createRequest(deleteObjectTaggingRequest.getBucketName(),
                deleteObjectTaggingRequest.getKey(), deleteObjectTaggingRequest, HttpMethodName.DELETE);
        request.addParameter("tagging", null);
        addParameterIfNotNull(request, "versionId", deleteObjectTaggingRequest.getVersionId());

        ResponseHeaderHandlerChain<DeleteObjectTaggingResult> handlerChain = new ResponseHeaderHandlerChain<DeleteObjectTaggingResult>(
                new Unmarshallers.DeleteObjectTaggingResponseUnmarshaller(),
                new DeleteObjectTaggingHeaderHandler()
        );

        return invoke(request, handlerChain);
    }

    @Override
    public BucketIntelligentTierConfiguration getBucketIntelligentTierConfiguration(GetBucketIntelligentTierConfigurationRequest getBucketIntelligentTierConfigurationRequest) {
        rejectNull(getBucketIntelligentTierConfigurationRequest, "The request cannot be null");
        rejectNull(getBucketIntelligentTierConfigurationRequest.getBucketName(), "The bucketName cannot be null");
        final String bucketName = getBucketIntelligentTierConfigurationRequest.getBucketName();

        CosHttpRequest<GetBucketIntelligentTierConfigurationRequest> request = createRequest(bucketName, null, getBucketIntelligentTierConfigurationRequest, HttpMethodName.GET);
        request.addParameter("intelligenttiering", null);
        return invoke(request, new Unmarshallers.GetBucketIntelligentTierConfigurationsUnmarshaller());
    }

    @Override
    public BucketIntelligentTierConfiguration getBucketIntelligentTierConfiguration(String bucketName) {
        return getBucketIntelligentTierConfiguration(new GetBucketIntelligentTierConfigurationRequest(bucketName));
    }

    @Override
    public void setBucketIntelligentTieringConfiguration(SetBucketIntelligentTierConfigurationRequest setBucketIntelligentTierConfigurationRequest) {
        rejectNull(setBucketIntelligentTierConfigurationRequest, "The request cannot be null");
        rejectNull(setBucketIntelligentTierConfigurationRequest.getBucketName(), "The bucketName cannot be null");
        BucketIntelligentTierConfiguration bucketIntelligentTierConfiguration = setBucketIntelligentTierConfigurationRequest.getBucketIntelligentTierConfiguration();
        rejectNull(bucketIntelligentTierConfiguration, "Bucket intelligent tier configuration cannot be null");
        final String bucketName = setBucketIntelligentTierConfigurationRequest.getBucketName();

        CosHttpRequest<SetBucketIntelligentTierConfigurationRequest> request = createRequest(bucketName, null, setBucketIntelligentTierConfigurationRequest, HttpMethodName.PUT);
        request.addParameter("intelligenttiering", null);

        byte[] content = new BucketConfigurationXmlFactory().convertToXmlByteArray(bucketIntelligentTierConfiguration);
        request.addHeader("Content-Length", String.valueOf(content.length));
        request.addHeader("Content-Type", "application/xml");
        request.setContent(new ByteArrayInputStream(content));
        invoke(request, voidCosResponseHandler);
    }

    @Override
    public MediaJobResponse createMediaJobs(MediaJobsRequest req) throws UnsupportedEncodingException {
        this.checkCIRequestCommon(req);
        rejectNull(req.getTag(),
                "The tag parameter must be specified setting the object tags");
        rejectNull(req.getQueueId(),
                "The queueId parameter must be specified setting the object tags");
        rejectNull(req.getInput().getObject(),
                "The input parameter must be specified setting the object tags");
        this.checkRequestOutput(req.getOperation().getOutput());
        this.rejectStartWith(req.getCallBack(),"http","The CallBack parameter mush start with http or https");
        CosHttpRequest<MediaJobsRequest> request = createRequest(req.getBucketName(), "/jobs", req, HttpMethodName.POST);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(req), "application/xml", false);
        return invoke(request, new Unmarshallers.JobCreatUnmarshaller());
    }

    @Override
    public Boolean cancelMediaJob(MediaJobsRequest req) {
        this.checkCIRequestCommon(req);
        rejectNull(req.getJobId(),
                "The jobId parameter must be specified setting the object tags");
        CosHttpRequest<MediaJobsRequest> request = createRequest(req.getBucketName(), "/jobs/" + req.getJobId(), req, HttpMethodName.PUT);
        invoke(request, voidCosResponseHandler);
        return true;
    }

    @Override
    public MediaJobResponse describeMediaJob(MediaJobsRequest req) {
        this.checkCIRequestCommon(req);
        rejectNull(req.getJobId(),
                "The jobId parameter must be specified setting the object tags");
        CosHttpRequest<MediaJobsRequest> request = createRequest(req.getBucketName(), "/jobs/" + req.getJobId(), req, HttpMethodName.GET);
        return invoke(request, new Unmarshallers.JobUnmarshaller());

    }

    @Override
    public MediaListJobResponse describeMediaJobs(MediaJobsRequest req) {
        this.checkCIRequestCommon(req);
        rejectNull(req.getQueueId(),
                "The queueId parameter must be specified setting the object tags");
        rejectNull(req.getTag(),
                "The tag parameter must be specified setting the object tags");
        CosHttpRequest<MediaJobsRequest> request = createRequest(req.getBucketName(), "/jobs", req, HttpMethodName.GET);
        addParameterIfNotNull(request, "queueId", req.getQueueId());
        addParameterIfNotNull(request, "tag", req.getTag());
        addParameterIfNotNull(request, "orderByTime", req.getOrderByTime());
        addParameterIfNotNull(request, "nextToken", req.getNextToken());
        addParameterIfNotNull(request, "size", req.getSize().toString());
        addParameterIfNotNull(request, "states", req.getStates());
        addParameterIfNotNull(request, "startCreationTime", req.getStartCreationTime());
        addParameterIfNotNull(request, "endCreationTime", req.getEndCreationTime());
        MediaListJobResponse response = invoke(request, new Unmarshallers.ListJobUnmarshaller());
        this.checkMediaListJobResponse(response);
        return response;
    }

    @Override
    public MediaListQueueResponse describeMediaQueues(MediaQueueRequest req) {
        this.checkCIRequestCommon(req);
        CosHttpRequest<MediaQueueRequest> request = createRequest(req.getBucketName(), "/queue", req, HttpMethodName.GET);
        addParameterIfNotNull(request, "queueIds", req.getQueueId());
        addParameterIfNotNull(request, "state", req.getState());
        addParameterIfNotNull(request, "pageNumber", req.getPageNumber());
        addParameterIfNotNull(request, "pageSize", req.getPageSize());
        return invoke(request, new Unmarshallers.ListQueueUnmarshaller());
    }

    @Override
    public MediaQueueResponse updateMediaQueue(MediaQueueRequest mediaQueueRequest) {
        this.checkCIRequestCommon(mediaQueueRequest);
        rejectNull(mediaQueueRequest.getQueueId(),
                "The queueId parameter must be specified setting the object tags");
        rejectNull(mediaQueueRequest.getName(),
                "The name parameter must be specified setting the object tags");
        rejectNull(mediaQueueRequest.getState(),
                "The state parameter must be specified setting the object tags");
        CosHttpRequest<MediaQueueRequest> request = createRequest(mediaQueueRequest.getBucketName(), "/queue/" + mediaQueueRequest.getQueueId(), mediaQueueRequest, HttpMethodName.PUT);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(mediaQueueRequest), "application/xml", false);
        return invoke(request, new Unmarshallers.QueueUnmarshaller());
    }

    @Override
    public MediaBucketResponse describeMediaBuckets(MediaBucketRequest mediaBucketRequest) {
        this.checkCIRequestCommon(mediaBucketRequest);
        CosHttpRequest<MediaBucketRequest> request = createRequest(mediaBucketRequest.getBucketName(), "/mediabucket", mediaBucketRequest, HttpMethodName.GET);
        addParameterIfNotNull(request, "regions", mediaBucketRequest.getRegions());
        addParameterIfNotNull(request, "bucketNames", mediaBucketRequest.getBucketNames());
        addParameterIfNotNull(request, "bucketName", mediaBucketRequest.getBucketName());
        addParameterIfNotNull(request, "pageNumber", mediaBucketRequest.getPageNumber());
        addParameterIfNotNull(request, "pageSize", mediaBucketRequest.getPageSize());
        return invoke(request, new Unmarshallers.ListBucketUnmarshaller());
    }

    @Override
    public MediaTemplateResponse createMediaTemplate(MediaTemplateRequest templateRequest) {
        rejectNull(templateRequest,
                "The request parameter must be specified setting the object tags");
        rejectNull(templateRequest.getTag(),
                "The tag parameter must be specified setting the object tags");
        rejectNull(templateRequest.getName(),
                "The name parameter must be specified setting the object tags");
        CosHttpRequest<MediaTemplateRequest> request = this.createRequest(templateRequest.getBucketName(), "/template", templateRequest, HttpMethodName.POST);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(templateRequest), "application/xml", false);
        return this.invoke(request, new Unmarshallers.TemplateUnmarshaller());
    }

    @Override
    public Boolean deleteMediaTemplate(MediaTemplateRequest request) {
        this.checkCIRequestCommon(request);
        rejectNull(request.getTemplateId(),
                "The templateId parameter must be specified setting the object tags");
        CosHttpRequest<MediaTemplateRequest> httpRequest = this.createRequest(request.getBucketName(), "/template/" + request.getTemplateId(), request, HttpMethodName.DELETE);
        this.invoke(httpRequest, voidCosResponseHandler);
        return true;
    }

    @Override
    public MediaListTemplateResponse describeMediaTemplates(MediaTemplateRequest request) {
        this.checkCIRequestCommon(request);
        CosHttpRequest<MediaTemplateRequest> httpRequest = this.createRequest(request.getBucketName(), "/template", request, HttpMethodName.GET);
        addParameterIfNotNull(httpRequest, "tag", request.getTag());
        addParameterIfNotNull(httpRequest, "category", request.getCategory());
        addParameterIfNotNull(httpRequest, "ids", request.getIds());
        addParameterIfNotNull(httpRequest, "name", request.getName());
        addParameterIfNotNull(httpRequest, "pageNumber", request.getPageNumber());
        addParameterIfNotNull(httpRequest, "pageSize", request.getPageSize());
        return this.invoke(httpRequest, new Unmarshallers.ListTemplateUnmarshaller());
    }

    @Override
    public Boolean updateMediaTemplate(MediaTemplateRequest request) {
        this.checkCIRequestCommon(request);
        rejectNull(request.getTag(),
                "The tag parameter must be specified setting the object tags");
        rejectNull(request.getName(),
                "The name parameter must be specified setting the object tags");
        CosHttpRequest<MediaTemplateRequest> httpRequest = this.createRequest(request.getBucketName(), "/template/" + request.getTemplateId(), request, HttpMethodName.PUT);
        this.setContent(httpRequest, RequestXmlFactory.convertToXmlByteArray(request), "application/xml", false);
        this.invoke(httpRequest, voidCosResponseHandler);
        return true;
    }

    @Override
    public SnapshotResponse generateSnapshot(SnapshotRequest request) {
        this.checkCIRequestCommon(request);
        rejectNull(request.getTime(),
                "The time parameter must be specified setting the object tags");
        rejectNull(request.getInput().getObject(),
                "The input.object parameter must be specified setting the object tags");
        CosHttpRequest<SnapshotRequest> httpRequest = this.createRequest(request.getBucketName(), "/snapshot", request, HttpMethodName.POST);
        this.setContent(httpRequest, RequestXmlFactory.convertToXmlByteArray(request), "application/xml", false);
        return this.invoke(httpRequest, new Unmarshallers.SnapshotUnmarshaller());
    }

    @Override
    public MediaInfoResponse generateMediainfo(MediaInfoRequest request) {
        this.checkCIRequestCommon(request);
        rejectNull(request.getInput().getObject(),
                "The input.object parameter must be specified setting the object tags");
        CosHttpRequest<MediaInfoRequest> httpRequest = this.createRequest(request.getBucketName(), "/mediainfo", request, HttpMethodName.POST);
        this.setContent(httpRequest, RequestXmlFactory.convertToXmlByteArray(request), "application/xml", false);
        return this.invoke(httpRequest, new Unmarshallers.MediaInfoUnmarshaller());
    }

    @Override
    public Boolean deleteWorkflow(MediaWorkflowListRequest request) {
        this.checkCIRequestCommon(request);
        rejectNull(request.getWorkflowId(), "The request parameter must be specified when delete the object tags");
        CosHttpRequest<MediaWorkflowListRequest> httpRequest = this.createRequest(request.getBucketName(), "/workflow/" + request.getWorkflowId(), request, HttpMethodName.DELETE);
        this.invoke(httpRequest, voidCosResponseHandler);
        return true;
    }

    @Override
    public MediaWorkflowListResponse describeWorkflow(MediaWorkflowListRequest request) {
        this.checkCIRequestCommon(request);
        CosHttpRequest<MediaWorkflowListRequest> httpRequest = this.createRequest(request.getBucketName(), "/workflow", request, HttpMethodName.GET);
        addParameterIfNotNull(httpRequest, "ids", request.getIds());
        addParameterIfNotNull(httpRequest, "name", request.getName());
        addParameterIfNotNull(httpRequest, "pageNumber", request.getPageNumber());
        addParameterIfNotNull(httpRequest, "pageSize", request.getPageSize());
        return this.invoke(httpRequest, new Unmarshallers.WorkflowListUnmarshaller());
    }

    @Override
    public MediaWorkflowExecutionResponse describeWorkflowExecution(MediaWorkflowListRequest request) {
        this.checkCIRequestCommon(request);
        CosHttpRequest<MediaWorkflowListRequest> httpRequest = this.createRequest(request.getBucketName(), "/workflowexecution/" + request.getRunId(), request, HttpMethodName.GET);
        return this.invoke(httpRequest, new Unmarshallers.WorkflowExecutionUnmarshaller());
    }

    @Override
    public MediaWorkflowExecutionsResponse describeWorkflowExecutions(MediaWorkflowListRequest request) {
        this.checkCIRequestCommon(request);
        CosHttpRequest<MediaWorkflowListRequest> httpRequest = this.createRequest(request.getBucketName(), "/workflowexecution", request, HttpMethodName.GET);
        addParameterIfNotNull(httpRequest, "workflowId", request.getWorkflowId());
        addParameterIfNotNull(httpRequest, "name", request.getName());
        addParameterIfNotNull(httpRequest, "orderByTime", request.getOrderByTime());
        addParameterIfNotNull(httpRequest, "size", request.getSize());
        addParameterIfNotNull(httpRequest, "states", request.getStates());
        addParameterIfNotNull(httpRequest, "startCreationTime", request.getStartCreationTime());
        addParameterIfNotNull(httpRequest, "endCreationTime", request.getEndCreationTime());
        addParameterIfNotNull(httpRequest, "nextToken", request.getNextToken());
        return this.invoke(httpRequest, new Unmarshallers.WorkflowExecutionsUnmarshaller());
    }

    @Override
    public DocJobResponse createDocProcessJobs(DocJobRequest request) {
        this.checkCIRequestCommon(request);
        CosHttpRequest<DocJobRequest> httpRequest = this.createRequest(request.getBucketName(), "/doc_jobs", request, HttpMethodName.POST);
        this.setContent(httpRequest, RequestXmlFactory.convertToXmlByteArray(request), "application/xml", false);
        return this.invoke(httpRequest, new Unmarshallers.DocProcessJobUnmarshaller());
    }

    @Override
    public DocJobResponse describeDocProcessJob(DocJobRequest request) {
        this.checkCIRequestCommon(request);
        CosHttpRequest<DocJobRequest> httpRequest = this.createRequest(request.getBucketName(), "/doc_jobs/" + request.getJobId(), request, HttpMethodName.GET);
        return this.invoke(httpRequest, new Unmarshallers.DescribeDocJobUnmarshaller());
    }

    @Override
    public DocJobListResponse describeDocProcessJobs(DocJobListRequest request) {
        this.checkCIRequestCommon(request);
        CosHttpRequest<DocJobListRequest> httpRequest = this.createRequest(request.getBucketName(), "/doc_jobs", request, HttpMethodName.GET);
        addParameterIfNotNull(httpRequest, "queueId", request.getQueueId());
        addParameterIfNotNull(httpRequest, "tag", request.getTag());
        addParameterIfNotNull(httpRequest, "orderByTime", request.getOrderByTime());
        addParameterIfNotNull(httpRequest, "nextToken", request.getNextToken());
        addParameterIfNotNull(httpRequest, "size", request.getSize().toString());
        addParameterIfNotNull(httpRequest, "states", request.getStates());
        addParameterIfNotNull(httpRequest, "startCreationTime", request.getStartCreationTime());
        addParameterIfNotNull(httpRequest, "endCreationTime", request.getEndCreationTime());
        return this.invoke(httpRequest, new Unmarshallers.DescribeDocJobsUnmarshaller());
    }

    @Override
    public DocListQueueResponse describeDocProcessQueues(DocQueueRequest docRequest) {
        this.checkCIRequestCommon(docRequest);
        CosHttpRequest<DocQueueRequest> request = createRequest(docRequest.getBucketName(), "/docqueue", docRequest, HttpMethodName.GET);
        addParameterIfNotNull(request, "queueIds", docRequest.getQueueId());
        addParameterIfNotNull(request, "state", docRequest.getState());
        addParameterIfNotNull(request, "pageNumber", docRequest.getPageNumber());
        addParameterIfNotNull(request, "pageSize", docRequest.getPageSize());
        return invoke(request, new Unmarshallers.DocListQueueUnmarshaller());
    }

    @Override
    public boolean updateDocProcessQueue(DocQueueRequest docRequest) {
        this.checkCIRequestCommon(docRequest);
        rejectNull(docRequest.getQueueId(),
                "The queueId parameter must be specified setting the object tags");
        rejectNull(docRequest.getName(),
                "The name parameter must be specified setting the object tags");
        rejectNull(docRequest.getState(),
                "The state parameter must be specified setting the object tags");
        CosHttpRequest<DocQueueRequest> request = createRequest(docRequest.getBucketName(), "/docqueue/" + docRequest.getQueueId(), docRequest, HttpMethodName.PUT);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(docRequest), "application/xml", false);
        invoke(request, voidCosResponseHandler);
        return true;
    }

    @Override
    public DocBucketResponse describeDocProcessBuckets(DocBucketRequest docRequest) {
        this.checkCIRequestCommon(docRequest);
        CosHttpRequest<DocBucketRequest> request = createRequest(docRequest.getBucketName(), "/docbucket", docRequest, HttpMethodName.GET);
        addParameterIfNotNull(request, "regions", docRequest.getRegions());
        addParameterIfNotNull(request, "bucketNames", docRequest.getBucketNames());
        addParameterIfNotNull(request, "pageNumber", docRequest.getPageNumber());
        addParameterIfNotNull(request, "pageSize", docRequest.getPageSize());
        return invoke(request, new Unmarshallers.DocListBucketUnmarshaller());
    }

    @Override
    public CIUploadResult processImage(ImageProcessRequest imageProcessRequest) {
        rejectNull(imageProcessRequest,
                "The ImageProcessRequest parameter must be specified when requesting an object's metadata");
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when requesting an object's metadata");

        String bucketName = imageProcessRequest.getBucketName();
        String key = imageProcessRequest.getKey();

        rejectNull(bucketName,
                "The bucket name parameter must be specified when requesting an object's metadata");
        rejectNull(key, "The key parameter must be specified when requesting an object's metadata");

        CosHttpRequest<ImageProcessRequest> request =
                createRequest(bucketName, key, imageProcessRequest, HttpMethodName.POST);
        request.addParameter("image_process", null);
        request.addHeader(Headers.PIC_OPERATIONS, Jackson.toJsonString(imageProcessRequest.getPicOperations()));
        ObjectMetadata returnedMetadata = invoke(request, new ResponseHeaderHandlerChain<>(
                new Unmarshallers.ImagePersistenceUnmarshaller(), new CosMetadataResponseHandler()));
        return returnedMetadata.getCiUploadResult();
    }

    @Override
    public ImageAuditingResponse imageAuditing(ImageAuditingRequest imageAuditingRequest) {
        rejectNull(imageAuditingRequest,
                "The imageAuditingRequest parameter must be specified setting the object tags");
        rejectNull(imageAuditingRequest.getBucketName(),
                "The bucketName parameter must be specified setting the object tags");
        String detectType = imageAuditingRequest.getDetectType();
        rejectNull(detectType, "The detectType parameter must be specified setting the object tags");
        CosHttpRequest<ImageAuditingRequest> request = createRequest(imageAuditingRequest.getBucketName(), imageAuditingRequest.getObjectKey(), imageAuditingRequest, HttpMethodName.GET);
        request.addParameter("ci-process", "sensitive-content-recognition");
        if ("all".equalsIgnoreCase(detectType))
            detectType = "porn,terrorist,politics,ads";
        addParameterIfNotNull(request, "detect-type", detectType);
        addParameterIfNotNull(request, "interval", Integer.toString(imageAuditingRequest.getInterval()));
        addParameterIfNotNull(request, "max-frames", Integer.toString(imageAuditingRequest.getMaxFrames()));
        addParameterIfNotNull(request, "biz-type", imageAuditingRequest.getBizType());
        addParameterIfNotNull(request, "detect-url", imageAuditingRequest.getDetectUrl());
        return invoke(request, new Unmarshallers.ImageAuditingUnmarshaller());
    }

    @Override
    public VideoAuditingResponse createVideoAuditingJob(VideoAuditingRequest videoAuditingRequest) {
        this.checkCIRequestCommon(videoAuditingRequest);
        this.rejectStartWith(videoAuditingRequest.getConf().getCallback(), "http", "The Conf.CallBack parameter mush start with http or https");
        CosHttpRequest<VideoAuditingRequest> request = createRequest(videoAuditingRequest.getBucketName(), "/video/auditing", videoAuditingRequest, HttpMethodName.POST);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(videoAuditingRequest), "application/xml", false);
        return invoke(request, new Unmarshallers.VideoAuditingUnmarshaller());
    }

    @Override
    public VideoAuditingResponse describeAuditingJob(VideoAuditingRequest videoAuditingRequest) {
        this.checkCIRequestCommon(videoAuditingRequest);
        rejectNull(videoAuditingRequest.getJobId(),
                "The jobId parameter must be specified setting the object tags");
        CosHttpRequest<VideoAuditingRequest> request = createRequest(videoAuditingRequest.getBucketName(), "/video/auditing/" + videoAuditingRequest.getJobId(), videoAuditingRequest, HttpMethodName.GET);
        return invoke(request, new Unmarshallers.VideoAuditingJobUnmarshaller());
    }

    @Override
    public AudioAuditingResponse createAudioAuditingJobs(AudioAuditingRequest audioAuditingRequest) {
        this.checkCIRequestCommon(audioAuditingRequest);
        this.rejectStartWith(audioAuditingRequest.getConf().getCallback(), "http", "The Conf.CallBack parameter mush start with http or https");
        CosHttpRequest<AudioAuditingRequest> request = createRequest(audioAuditingRequest.getBucketName(), "/audio/auditing", audioAuditingRequest, HttpMethodName.POST);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(audioAuditingRequest), "application/xml", false);
        return invoke(request, new Unmarshallers.AudioAuditingUnmarshaller());
    }

    @Override
    public AudioAuditingResponse describeAudioAuditingJob(AudioAuditingRequest audioAuditingRequest) {
        this.checkCIRequestCommon(audioAuditingRequest);
        rejectNull(audioAuditingRequest.getJobId(),
                "The jobId parameter must be specified setting the object tags");
        CosHttpRequest<AudioAuditingRequest> request = createRequest(audioAuditingRequest.getBucketName(), "/audio/auditing/" + audioAuditingRequest.getJobId(), audioAuditingRequest, HttpMethodName.GET);
        return invoke(request, new Unmarshallers.AudioAuditingJobUnmarshaller());
    }

    @Override
    public ImageLabelResponse getImageLabel(ImageLabelRequest imageLabelRequest) {
        rejectNull(imageLabelRequest,
                "The imageLabelRequest parameter must be specified setting the object tags");
        rejectNull(imageLabelRequest.getBucketName(),
                "The imageLabelRequest.bucketName parameter must be specified setting the object tags");
        CosHttpRequest<ImageLabelRequest> request = createRequest(imageLabelRequest.getBucketName(), imageLabelRequest.getObjectKey(), imageLabelRequest, HttpMethodName.GET);
        request.addParameter("ci-process", "detect-label");
        return invoke(request, new Unmarshallers.ImageLabelUnmarshaller());
    }

    @Override
    public ImageLabelV2Response getImageLabelV2(ImageLabelV2Request imageLabelV2Request) {
        rejectNull(imageLabelV2Request,
                "The imageAuditingRequest parameter must be specified setting the object tags");
        rejectNull(imageLabelV2Request.getBucketName(),
                "The bucketName parameter must be specified setting the object tags");
        CosHttpRequest<ImageLabelV2Request> request = createRequest(imageLabelV2Request.getBucketName(), imageLabelV2Request.getObjectKey(), imageLabelV2Request, HttpMethodName.GET);
        request.addParameter("ci-process", "content-analysis");
        addParameterIfNotNull(request, "scenes", imageLabelV2Request.getScenes());
        System.out.println(request.getEndpoint());
        return invoke(request, new Unmarshallers.ImageLabelV2Unmarshaller());
    }

    @Override
    public TextAuditingResponse createAuditingTextJobs(TextAuditingRequest textAuditingRequest) {
        this.checkCIRequestCommon(textAuditingRequest);
        this.rejectStartWith(textAuditingRequest.getConf().getCallback(), "http", "The Conf.CallBack parameter mush start with http or https");
        CosHttpRequest<TextAuditingRequest> request = createRequest(textAuditingRequest.getBucketName(), "/text/auditing", textAuditingRequest, HttpMethodName.POST);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(textAuditingRequest), "application/xml", false);
        return invoke(request, new Unmarshallers.TextAuditingJobUnmarshaller());
    }

    @Override
    public TextAuditingResponse describeAuditingTextJob(TextAuditingRequest textAuditingRequest) {
        this.checkCIRequestCommon(textAuditingRequest);
        rejectNull(textAuditingRequest.getJobId(),
                "The jobId parameter must be specified setting the object tags");
        CosHttpRequest<TextAuditingRequest> request = createRequest(textAuditingRequest.getBucketName(), "/text/auditing/" + textAuditingRequest.getJobId(), textAuditingRequest, HttpMethodName.GET);
        return invoke(request, new Unmarshallers.TextAuditingDescribeJobUnmarshaller());
    }

    @Override
    public DocumentAuditingResponse createAuditingDocumentJobs(DocumentAuditingRequest documentAuditingRequest) {
        this.checkCIRequestCommon(documentAuditingRequest);
        this.rejectStartWith(documentAuditingRequest.getConf().getCallback(), "http", "The Conf.CallBack parameter mush start with http or https");
        CosHttpRequest<DocumentAuditingRequest> request = createRequest(documentAuditingRequest.getBucketName(), "/document/auditing", documentAuditingRequest, HttpMethodName.POST);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(documentAuditingRequest), "application/xml", false);
        return invoke(request, new Unmarshallers.DocumentAuditingJobUnmarshaller());
    }

    @Override
    public DocumentAuditingResponse describeAuditingDocumentJob(DocumentAuditingRequest documentAuditingRequest) {
        this.checkCIRequestCommon(documentAuditingRequest);
        rejectNull(documentAuditingRequest.getJobId(),
                "The jobId parameter must be specified setting the object tags");
        CosHttpRequest<DocumentAuditingRequest> request = createRequest(documentAuditingRequest.getBucketName(), "/document/auditing/" + documentAuditingRequest.getJobId(), documentAuditingRequest, HttpMethodName.GET);
        return invoke(request, new Unmarshallers.DocumentAuditingDescribeJobUnmarshaller());
    }

    @Override
    public BatchImageAuditingResponse batchImageAuditing(BatchImageAuditingRequest batchImageAuditingRequest) {
        this.checkCIRequestCommon(batchImageAuditingRequest);
        this.rejectStartWith(batchImageAuditingRequest.getConf().getCallback(), "http", "The Conf.CallBack parameter mush start with http or https");
        CosHttpRequest<BatchImageAuditingRequest> request = createRequest(batchImageAuditingRequest.getBucketName(), "/image/auditing", batchImageAuditingRequest, HttpMethodName.POST);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(batchImageAuditingRequest), "application/xml", false);
        return invoke(request, new Unmarshallers.BatchImageAuditingJobUnmarshaller());
    }

    @Override
    public Boolean createDocProcessBucket(DocBucketRequest docBucketRequest) {
        this.checkCIRequestCommon(docBucketRequest);
        CosHttpRequest<DocBucketRequest> request = createRequest(docBucketRequest.getBucketName(), "/docbucket", docBucketRequest, HttpMethodName.POST);
        this.invoke(request, voidCosResponseHandler);
        return true;
    }

    @Override
    public String GenerateDocPreviewUrl(DocHtmlRequest docJobRequest) throws URISyntaxException {
        rejectNull(docJobRequest,
                "The request parameter must be specified setting the object tags");
        rejectNull(docJobRequest.getBucketName(),
                "The bucketName parameter must be specified setting the object tags");
        rejectNull(docJobRequest.getObjectKey(),
                "The objectKey parameter must be specified setting the object tags");
        CosHttpRequest<DocHtmlRequest> request = createRequest(docJobRequest.getBucketName(), docJobRequest.getObjectKey(), docJobRequest, HttpMethodName.GET);
        return buildDocPreview(request);
    }

    @Override
    public WebpageAuditingResponse createWebpageAuditingJob(WebpageAuditingRequest webpageAuditingRequest) {
        this.checkCIRequestCommon(webpageAuditingRequest);
        this.rejectStartWith(webpageAuditingRequest.getInput().getUrl(), "http", "The Conf.CallBack parameter mush start with http or https");
        CosHttpRequest<WebpageAuditingRequest> request = createRequest(webpageAuditingRequest.getBucketName(), "/webpage/auditing", webpageAuditingRequest, HttpMethodName.POST);
        this.setContent(request, RequestXmlFactory.convertToXmlByteArray(webpageAuditingRequest), "application/xml", false);
        return invoke(request, new Unmarshallers.WebpageAuditingJobUnmarshaller());
    }

    @Override
    public WebpageAuditingResponse describeWebpageAuditingJob(WebpageAuditingRequest webpageAuditingRequest) {
        this.checkCIRequestCommon(webpageAuditingRequest);
        rejectNull(webpageAuditingRequest.getJobId(),
                "The jobId parameter must be specified setting the object tags");
        CosHttpRequest<WebpageAuditingRequest> request = createRequest(webpageAuditingRequest.getBucketName(), "/webpage/auditing/" + webpageAuditingRequest.getJobId(), webpageAuditingRequest, HttpMethodName.GET);
        return invoke(request, new Unmarshallers.WebpageAuditingDescribeJobUnmarshaller());
    }


    private String buildDocPreview(CosHttpRequest<DocHtmlRequest> request) throws URISyntaxException {
        String urlStr = request.getProtocol().toString() + "://" + request.getEndpoint() + request.getResourcePath();
        URIBuilder uriBuilder = new URIBuilder(urlStr);
        COSSigner cosSigner = clientConfig.getCosSigner();
        Date expiredTime = new Date(System.currentTimeMillis() + clientConfig.getSignExpired() * 1000);
        String authoriationStr = cosSigner.buildAuthorizationStr(request.getHttpMethod(), request.getResourcePath(),
                request.getHeaders(), request.getParameters(), fetchCredential(), expiredTime, true);
        DocHtmlRequest originalRequest = request.getOriginalRequest();
        uriBuilder.addParameter("ci-process", "doc-preview");
        uriBuilder.addParameter("dsttype", originalRequest.getType().toString());
        uriBuilder.addParameter("srcType", originalRequest.getSrcType());
        uriBuilder.addParameter("page", originalRequest.getPage());
        uriBuilder.addParameter("ImageParams", originalRequest.getImageParams());
        uriBuilder.addParameter("sheet", originalRequest.getSheet());
        uriBuilder.addParameter("password", originalRequest.getPassword());
        uriBuilder.addParameter("comment", originalRequest.getComment());
        uriBuilder.addParameter("excelPaperDirection", originalRequest.getExcelPaperDirection());
        uriBuilder.addParameter("quality", originalRequest.getQuality());
        uriBuilder.addParameter("scale", originalRequest.getScale());
        return uriBuilder.build().toString() + "&" + authoriationStr;
    }

    private void checkAuditingRequest(ImageAuditingRequest request) {
        rejectNull(request.getDetectType(), "The detectType parameter must be specified setting the object tags");
        rejectNull(request.getObjectKey(), "The objectKey parameter must be specified setting the object tags");
    }

    private void checkWorkflowParameter(MediaWorkflowRequest request) {
        rejectNull(request.getName(),
                "The name parameter must be specified setting the object tags");
        rejectNull(request.getTopology(),
                "The topology parameter must be specified setting the object tags");
        rejectEmpty(request.getTopology().getMediaWorkflowNodes(),
                "The Nodes parameter must be specified setting the object tags");
        rejectEmpty(request.getTopology().getMediaWorkflowDependency(),
                "The Dependency parameter must be specified setting the object tags");
    }

    private void checkCIRequestCommon(CIServiceRequest request) {
        rejectNull(request,
                "The request parameter must be specified setting the object tags");
        rejectNull(request.getBucketName(),
                "The bucketName parameter must be specified setting the object tags");
    }

    private void checkRequestOutput(MediaOutputObject output) {
        rejectNull(output.getBucket(),
                "The output.bucket parameter must be specified setting the object tags");
        rejectNull(output.getRegion(),
                "The output.region parameter must be specified setting the object tags");
        rejectNull(output.getObject(),
                "The output.object parameter must be specified setting the object tags");
    }


    private void checkMediaListJobResponse(MediaListJobResponse response) {
        List<MediaJobObject> jobsDetailList = response.getJobsDetailList();
        if (jobsDetailList.size() == 1) {
            MediaJobObject mediaJobObject = jobsDetailList.get(0);
            if (mediaJobObject.getQueueId() == null && mediaJobObject.getJobId() == null && mediaJobObject.getCode() == null) {
                jobsDetailList.clear();
            }
        }
    }

    public URL getObjectUrl(String bucketName, String key) {
        return getObjectUrl(new GetObjectRequest(bucketName, key));
    }

    public URL getObjectUrl(String bucketName, String key, String versionId) {
        return getObjectUrl(new GetObjectRequest(bucketName, key, versionId));
    }

    public URL getObjectUrl(GetObjectRequest getObjectRequest) {
        rejectNull(clientConfig.getRegion(),
                "region is null, region in clientConfig must be specified when generating a pre-signed URL");
        rejectNull(getObjectRequest, "The request parameter must be specified when generating a pre-signed URL");

        final String bucketName = getObjectRequest.getBucketName();
        final String key = getObjectRequest.getKey();

        CosHttpRequest<GetObjectRequest> request =
                createRequest(bucketName, key, getObjectRequest, HttpMethodName.GET);

        addParameterIfNotNull(request, "versionId", getObjectRequest.getVersionId());

        COSCredentials cred = fetchCredential();
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append(clientConfig.getHttpProtocol().toString()).append("://");
        strBuilder.append(clientConfig.getEndpointBuilder()
                .buildGeneralApiEndpoint(formatBucket(bucketName, cred.getCOSAppId())));
        strBuilder.append(UrlEncoderUtils.encodeUrlPath(formatKey(key)));

        boolean hasAppendFirstParameter = false;
        for (Entry<String, String> entry : request.getParameters().entrySet()) {
            String paramKey = entry.getKey();
            String paramValue = entry.getValue();

            if (!hasAppendFirstParameter) {
                strBuilder.append("?");
                hasAppendFirstParameter = true;
            } else {
                strBuilder.append("&");
            }
            strBuilder.append(UrlEncoderUtils.encode(paramKey));
            if (paramValue != null) {
                strBuilder.append("=").append(UrlEncoderUtils.encode(paramValue));
            }
        }

        try {
            return new URL(strBuilder.toString());
        } catch (MalformedURLException e) {
            throw new CosClientException(e.toString());
        }
    }
}

