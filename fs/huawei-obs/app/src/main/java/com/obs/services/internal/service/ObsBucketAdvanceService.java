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

import com.fasterxml.jackson.databind.JsonNode;
import com.obs.log.ILogger;
import com.obs.log.LoggerBuilder;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.Constants;
import com.obs.services.internal.Constants.CommonHeaders;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.handler.XmlResponsesSaxParser;
import com.obs.services.internal.io.HttpMethodReleaseInputStream;
import com.obs.services.internal.trans.NewTransResult;
import com.obs.services.internal.utils.JSONChange;
import com.obs.services.internal.utils.Mimetypes;
import com.obs.services.internal.utils.RestUtils;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.internal.xml.OBSXMLBuilder;
import com.obs.services.model.AccessControlList;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.BaseBucketRequest;
import com.obs.services.model.BucketCors;
import com.obs.services.model.BucketCustomDomainInfo;
import com.obs.services.model.BucketDirectColdAccess;
import com.obs.services.model.BucketEncryption;
import com.obs.services.model.BucketLoggingConfiguration;
import com.obs.services.model.BucketMetadataInfoRequest;
import com.obs.services.model.BucketNotificationConfiguration;
import com.obs.services.model.BucketQuota;
import com.obs.services.model.BucketTagInfo;
import com.obs.services.model.CreateBucketRequest;
import com.obs.services.model.CreateVirtualBucketRequest;
import com.obs.services.model.CreateVirtualBucketResult;
import com.obs.services.model.DeleteBucketCustomDomainRequest;
import com.obs.services.model.GetBucketCustomDomainRequest;
import com.obs.services.model.GrantAndPermission;
import com.obs.services.model.GroupGrantee;
import com.obs.services.model.HeaderResponse;
import com.obs.services.model.HistoricalObjectReplicationEnum;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.LifecycleConfiguration;
import com.obs.services.model.ListBucketAliasResult;
import com.obs.services.model.Permission;
import com.obs.services.model.ReplicationConfiguration;
import com.obs.services.model.RequestPaymentConfiguration;
import com.obs.services.model.RuleStatusEnum;
import com.obs.services.model.SetBucketAclRequest;
import com.obs.services.model.SetBucketCorsRequest;
import com.obs.services.model.SetBucketCustomDomainRequest;
import com.obs.services.model.SetBucketDirectColdAccessRequest;
import com.obs.services.model.SetBucketEncryptionRequest;
import com.obs.services.model.SetBucketLifecycleRequest;
import com.obs.services.model.SetBucketLoggingRequest;
import com.obs.services.model.SetBucketNotificationRequest;
import com.obs.services.model.SetBucketQuotaRequest;
import com.obs.services.model.SetBucketReplicationRequest;
import com.obs.services.model.SetBucketRequestPaymentRequest;
import com.obs.services.model.SetBucketTaggingRequest;
import com.obs.services.model.SetBucketVersioningRequest;
import com.obs.services.model.SetBucketWebsiteRequest;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.StorageClassEnum;
import com.obs.services.model.WebsiteConfiguration;
import com.obs.services.model.fs.GetBucketFSStatusResult;
import com.obs.services.model.http.HttpStatusCode;
import com.oef.services.model.RequestParamEnum;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class ObsBucketAdvanceService extends ObsBucketBaseService {
    private static final ILogger log = LoggerBuilder.getLogger(ObsBucketAdvanceService.class);

    protected HeaderResponse setBucketVersioningImpl(SetBucketVersioningRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.VERSIONING.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();

        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        String xml = this.getIConvertor(request.getBucketName()).transVersioningConfiguration(request.getBucketName(),
                request.getStatus() != null ? request.getStatus().getCode() : null);
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return this.build(response);
    }

    protected HeaderResponse setBucketRequestPaymentImpl(SetBucketRequestPaymentRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.REQUEST_PAYMENT.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        String xml = this.getIConvertor(request.getBucketName())
                .transRequestPaymentConfiguration(request.getBucketName(),
                        request.getPayer() != null ? request.getPayer().getCode() : null);

        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return this.build(response);
    }

    protected RequestPaymentConfiguration getBucketRequestPaymentImpl(BaseBucketRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.REQUEST_PAYMENT.getOriginalStringCode(), "");

        Response response = performRestGet(request.getBucketName(), null, requestParams,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(response);

        RequestPaymentConfiguration ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(response),
                XmlResponsesSaxParser.RequestPaymentHandler.class, false).getRequestPaymentConfiguration();
        setHeadersAndStatus(ret, response);
        return ret;
    }

    protected HeaderResponse setBucketNotificationImpl(SetBucketNotificationRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.NOTIFICATION.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        String xml = this.getIConvertor(request.getBucketName())
                .transBucketNotificationConfiguration(request.getBucketNotificationConfiguration());

        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);

        return build(response);
    }

    protected BucketNotificationConfiguration getBucketNotificationConfigurationImpl(BaseBucketRequest request)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.NOTIFICATION.getOriginalStringCode(), "");
        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketNotificationConfiguration result = getXmlResponseSaxParser()
                .parse(new HttpMethodReleaseInputStream(httpResponse),
                        XmlResponsesSaxParser.BucketNotificationConfigurationHandler.class, false)
                .getBucketNotificationConfiguration();
        setHeadersAndStatus(result, httpResponse);
        return result;
    }

    protected HeaderResponse setBucketWebsiteConfigurationImpl(SetBucketWebsiteRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.WEBSITE.getOriginalStringCode(), "");

        Map<String, String> headers = new HashMap<>();
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        String xml = this.getIConvertor(request.getBucketName()).transWebsiteConfiguration(request.getWebsiteConfig());

        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected WebsiteConfiguration getBucketWebsiteConfigurationImpl(BaseBucketRequest request)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.WEBSITE.getOriginalStringCode(), "");

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        WebsiteConfiguration ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.BucketWebsiteConfigurationHandler.class, false).getWebsiteConfig();

        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected HeaderResponse deleteBucketWebsiteConfigurationImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.WEBSITE.getOriginalStringCode(), "");
        Response response = performRestDelete(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return build(response);
    }

    protected HeaderResponse setBucketLifecycleConfigurationImpl(SetBucketLifecycleRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.LIFECYCLE.getOriginalStringCode(), "");

        Map<String, String> headers = new HashMap<>();
        String xml = this.getIConvertor(request.getBucketName())
                .transLifecycleConfiguration(request.getLifecycleConfig());
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected LifecycleConfiguration getBucketLifecycleConfigurationImpl(BaseBucketRequest request)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.LIFECYCLE.getOriginalStringCode(), "");

        Response response = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(response);

        LifecycleConfiguration ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(response),
                XmlResponsesSaxParser.BucketLifecycleConfigurationHandler.class, false).getLifecycleConfig();
        setHeadersAndStatus(ret, response);
        return ret;
    }

    protected HeaderResponse deleteBucketLifecycleConfigurationImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.LIFECYCLE.getOriginalStringCode(), "");
        Response response = performRestDelete(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return build(response);
    }

    protected HeaderResponse setBucketTaggingImpl(SetBucketTaggingRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.TAGGING.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();

        String xml = this.getIConvertor(request.getBucketName()).transBucketTagInfo(request.getBucketTagInfo());

        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);

        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected BucketTagInfo getBucketTaggingImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.TAGGING.getOriginalStringCode(), "");
        Response httpResponse = this.performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketTagInfo result = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.BucketTagInfoHandler.class, false).getBucketTagInfo();
        setHeadersAndStatus(result, httpResponse);
        return result;
    }

    protected HeaderResponse deleteBucketTaggingImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.TAGGING.getOriginalStringCode(), "");
        Response response = performRestDelete(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return build(response);
    }

    protected HeaderResponse setBucketEncryptionImpl(SetBucketEncryptionRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.ENCRYPTION.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);

        String xml = request.getBucketEncryption() == null ? ""
                : this.getIConvertor(request.getBucketName()).transBucketEcryption(request.getBucketEncryption());
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected BucketEncryption getBucketEncryptionImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.ENCRYPTION.getOriginalStringCode(), "");

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketEncryption ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.BucketEncryptionHandler.class, false).getEncryption();

        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected HeaderResponse deleteBucketEncryptionImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.ENCRYPTION.getOriginalStringCode(), "");
        Response response = performRestDelete(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return build(response);
    }

    protected HeaderResponse setBucketReplicationConfigurationImpl(SetBucketReplicationRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.REPLICATION.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();

        String xml = this.getIConvertor(request.getBucketName())
                .transReplicationConfiguration(request.getReplicationConfiguration());

        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected ReplicationConfiguration getBucketReplicationConfigurationImpl(BaseBucketRequest request)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.REPLICATION.getOriginalStringCode(), "");
        Response httpResponse = this.performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        ReplicationConfiguration result = getXmlResponseSaxParser()
                .parse(new HttpMethodReleaseInputStream(httpResponse),
                        XmlResponsesSaxParser.BucketReplicationConfigurationHandler.class, false)
                .getReplicationConfiguration();

        setHeadersAndStatus(result, httpResponse);
        return result;
    }

    protected HeaderResponse deleteBucketReplicationConfigurationImpl(BaseBucketRequest request)
            throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.REPLICATION.getOriginalStringCode(), "");
        Response response = performRestDelete(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return build(response);
    }

    protected HeaderResponse setBucketCorsImpl(SetBucketCorsRequest request) throws ServiceException {
        String xml = request.getBucketCors() == null ? ""
                : this.getIConvertor(request.getBucketName()).transBucketCors(request.getBucketCors());

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.CORS.getOriginalStringCode(), "");

        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));

        headers.put(CommonHeaders.CONTENT_LENGTH, String.valueOf(xml.length()));
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected BucketCors getBucketCorsImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.CORS.getOriginalStringCode(), "");
        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);
        BucketCors ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.BucketCorsHandler.class, false).getConfiguration();
        setHeadersAndStatus(ret, httpResponse);
        return ret;

    }

    protected HeaderResponse deleteBucketCorsImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.CORS.getOriginalStringCode(), "");

        Response response = performRestDelete(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return build(response);
    }

    protected HeaderResponse setBucketQuotaImpl(SetBucketQuotaRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.QUOTA.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);

        String xml = request.getBucketQuota() == null ? ""
                : this.getIConvertor(request.getBucketName()).transBucketQuota(request.getBucketQuota());
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

    protected BucketQuota getBucketQuotaImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.QUOTA.getOriginalStringCode(), "");

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketQuota ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.BucketQuotaHandler.class, false).getQuota();
        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected HeaderResponse setBucketAclImpl(SetBucketAclRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.ACL.getOriginalStringCode(), "");
        RequestBody entity = null;
        if (ServiceUtils.isValid(request.getCannedACL())) {
            request.setAcl(this.getIConvertor(request.getBucketName()).transCannedAcl(request.getCannedACL().trim()));
        }
        Map<String, String> headers = new HashMap<>();
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        boolean isExtraAclPutRequired = !prepareRESTHeaderAcl(request.getBucketName(), headers, request.getAcl());
        if (isExtraAclPutRequired) {
            String xml = request.getAcl() == null ? ""
                    : this.getIConvertor(request.getBucketName()).transAccessControlList(request.getAcl(), true);
            headers.put(CommonHeaders.CONTENT_LENGTH, String.valueOf(xml.length()));
            headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
            entity = createRequestBody(Mimetypes.MIMETYPE_XML, xml);
        }

        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));
        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(entity);
        Response response = performRequest(result);
        return build(response);
    }

    protected AccessControlList getBucketAclImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.ACL.getOriginalStringCode(), "");

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        AccessControlList ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.AccessControlListHandler.class, false).getAccessControlList();
        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected HeaderResponse setBucketLoggingConfigurationImpl(SetBucketLoggingRequest request)
            throws ServiceException {
        if (request.getLoggingConfiguration().isLoggingEnabled() && request.isUpdateTargetACLifRequired()
                && this.getProviderCredentials().getLocalAuthType(request.getBucketName()) != AuthTypeEnum.OBS) {
            boolean isSetLoggingGroupWrite = false;
            boolean isSetLoggingGroupReadACP = false;
            String groupIdentifier = Constants.LOG_DELIVERY_URI;

            BaseBucketRequest getBucketAclRequest = new BaseBucketRequest(
                    request.getLoggingConfiguration().getTargetBucketName());
            getBucketAclRequest.setRequesterPays(request.isRequesterPays());
            AccessControlList logBucketACL = getBucketAclImpl(getBucketAclRequest);

            for (GrantAndPermission gap : logBucketACL.getGrantAndPermissions()) {
                if (gap.getGrantee() instanceof GroupGrantee) {
                    GroupGrantee grantee = (GroupGrantee) gap.getGrantee();
                    if (groupIdentifier.equals(this.getIConvertor(request.getBucketName())
                            .transGroupGrantee(grantee.getGroupGranteeType()))) {
                        if (Permission.PERMISSION_WRITE.equals(gap.getPermission())) {
                            isSetLoggingGroupWrite = true;
                        } else if (Permission.PERMISSION_READ_ACP.equals(gap.getPermission())) {
                            isSetLoggingGroupReadACP = true;
                        }
                    }
                }
            }

            if (!isSetLoggingGroupWrite || !isSetLoggingGroupReadACP) {
                if (log.isWarnEnabled()) {
                    log.warn("Target logging bucket '" + request.getLoggingConfiguration().getTargetBucketName()
                            + "' does not have the necessary ACL settings, updating ACL now");
                }
                if (logBucketACL.getOwner() != null) {
                    logBucketACL.getOwner().setDisplayName(null);
                }
                logBucketACL.grantPermission(GroupGrantee.LOG_DELIVERY, Permission.PERMISSION_WRITE);
                logBucketACL.grantPermission(GroupGrantee.LOG_DELIVERY, Permission.PERMISSION_READ_ACP);

                SetBucketAclRequest aclReqeust = new SetBucketAclRequest(request.getBucketName(), logBucketACL);
                aclReqeust.setRequesterPays(request.isRequesterPays());
                setBucketAclImpl(aclReqeust);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("Target logging bucket '" + request.getLoggingConfiguration().getTargetBucketName()
                            + "' has the necessary ACL settings");
                }
            }
        }

        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.LOGGING.getOriginalStringCode(), "");

        Map<String, String> headers = new HashMap<>();

        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        String xml = request.getLoggingConfiguration() == null ? ""
                : this.getIConvertor(request.getBucketName())
                .transBucketLoggingConfiguration(request.getLoggingConfiguration());
        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected BucketLoggingConfiguration getBucketLoggingConfigurationImpl(BaseBucketRequest request)
            throws ServiceException {

        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.LOGGING.getOriginalStringCode(), "");

        Response httpResponse = performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketLoggingConfiguration ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.BucketLoggingHandler.class, false).getBucketLoggingStatus();

        setHeadersAndStatus(ret, httpResponse);
        return ret;
    }

    protected HeaderResponse setBucketDirectColdAccessImpl(SetBucketDirectColdAccessRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.DIRECTCOLDACCESS.getOriginalStringCode(), "");
        Map<String, String> headers = new HashMap<>();

        String xml = this.getIConvertor(request.getBucketName()).transBucketDirectColdAccess(request.getAccess());

        headers.put(CommonHeaders.CONTENT_MD5, ServiceUtils.computeMD5(xml));
        headers.put(CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);
        transRequestPaymentHeaders(request, headers, this.getIHeaders(request.getBucketName()));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        result.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xml));
        Response response = performRequest(result);
        return build(response);
    }

    protected BucketDirectColdAccess getBucketDirectColdAccessImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.DIRECTCOLDACCESS.getOriginalStringCode(), "");
        Response httpResponse = this.performRestGet(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(httpResponse);

        BucketDirectColdAccess result = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(httpResponse),
                XmlResponsesSaxParser.BucketDirectColdAccessHandler.class, false).getBucketDirectColdAccess();
        setHeadersAndStatus(result, httpResponse);
        return result;
    }

    protected HeaderResponse deleteBucketDirectColdAccessImpl(BaseBucketRequest request) throws ServiceException {
        Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(SpecialParamEnum.DIRECTCOLDACCESS.getOriginalStringCode(), "");
        Response response = performRestDelete(request.getBucketName(), null, requestParameters,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        return build(response);
    }

    protected BucketCustomDomainInfo getBucketCustomDomainImpl(GetBucketCustomDomainRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.CUSTOMDOMAIN.getOriginalStringCode(), "");

        Response response = performRestGet(request.getBucketName(), null, requestParams,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());

        this.verifyResponseContentType(response);

        BucketCustomDomainInfo ret = getXmlResponseSaxParser().parse(new HttpMethodReleaseInputStream(response),
                XmlResponsesSaxParser.BucketCustomDomainHandler.class, true).getBucketTagInfo();
        setHeadersAndStatus(ret, response);
        return ret;
    }

    protected HeaderResponse setBucketCustomDomainImpl(SetBucketCustomDomainRequest request) throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.CUSTOMDOMAIN.getOriginalStringCode(), request.getDomainName());
        Map<String, String> headers = transRequestPaymentHeaders(request, null,
                this.getIHeaders(request.getBucketName()));

        NewTransResult result = transRequest(request);
        result.setParams(requestParams);
        result.setHeaders(headers);
        Response response = performRequest(result);
        return this.build(response);
    }

    protected HeaderResponse deleteBucketCustomDomainImpl(DeleteBucketCustomDomainRequest request)
            throws ServiceException {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put(SpecialParamEnum.CUSTOMDOMAIN.getOriginalStringCode(), request.getDomainName());

        Response response = performRestDelete(request.getBucketName(), null, requestParams,
                transRequestPaymentHeaders(request, null, this.getIHeaders(request.getBucketName())),
                request.getUserHeaders());
        return this.build(response);
    }

    protected CreateVirtualBucketResult createVirtualBucketImpl(CreateVirtualBucketRequest request) throws ServiceException {
        // step 1: 列举指定region下的集群信息
        List<String> azTextIds = listAvailableZoneInfo(request.getRegionId(), request.getToken());
        // step 2: 指定集群id创桶
        createBucketWithClusterId(request, azTextIds);
        // step 3: 创建一次别名
        createBucketAliasImpl(request);
        // step 4: 绑定两次别名，为两个真实桶分别绑定
        bindBucketAliasImpl(request.getBucketName1(), request.getBucketAlias());
        bindBucketAliasImpl(request.getBucketName2(), request.getBucketAlias());
        // step 5: 双向配置复制关系，并开启历史对象复制
        setVirtualReplication(request.getAgencyId(), request.getBucketName1(), request.getBucketName2());
        setVirtualReplication(request.getAgencyId(), request.getBucketName2(), request.getBucketName1());

        CreateVirtualBucketResult result = new CreateVirtualBucketResult();
        result.setBucketName1(request.getBucketName1());
        result.setBucketName2(request.getBucketName2());
        result.setVirtualBucketName(request.getBucketAlias());
        result.setMessage("create virtual bucket success, virtualBucketName: " + request.getBucketAlias()
                + "bucketName1: " + request.getBucketName1() + ", bucketName2: " + request.getBucketName2());
        result.setStatusCode(HttpStatusCode.HTTP_OK.getCode());
        return result;
    }

    protected List<String> listAvailableZoneInfo(String regionId, String token) {
        Map<String, String> requestParams = new HashMap<>();
        requestParams.put("regionId", regionId);
        Map<String, String> headers = new HashMap<>();
        headers.put(Constants.CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_JSON);
        NewTransResult result = new NewTransResult();
        result.setParams(requestParams);
        result.setHttpMethod(HttpMethodEnum.GET);
        result.setHeaders(headers);
        Map<String, String> userHeaders = new HashMap<>();
        userHeaders.put(Constants.CommonHeaders.X_AUTH_TOKEN, token);
        result.setUserHeaders(userHeaders);
        result.setEncodeUrl(false);
        result.setObjectKey(RequestParamEnum.SERVICES_CLUSTERS.getOriginalStringCode());
        Response response = performRequest(result, true, false, false, true);
        this.verifyResponseContentTypeForJson(response);

        List<JsonNode> azIds = JSONChange.readNodeFromJson(RestUtils.readBodyFromResponse(response))
                .get("infos").findValues("id");
        if (azIds.size() != 2) {
            throw new ServiceException("az info is not 2az.");
        }
        List<String> azTextIds = new ArrayList<>();
        azTextIds.add(azIds.get(0).asText());
        azTextIds.add(azIds.get(1).asText());

        return azTextIds;
    }

    protected ListBucketAliasResult listAliasBucketsImpl() {
        try {
            Map<String, String> requestParams = new HashMap<>();
            requestParams.put(SpecialParamEnum.OBSBUCKETALIAS.getOriginalStringCode(), "");
            Map<String, String> headers = new HashMap<>();
            headers.put(Constants.CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_JSON);
            Response response = performRestGetForListBuckets("", null, requestParams, headers);
            this.verifyResponseContentType(response);

            XmlResponsesSaxParser.ListBucketAliasHandler handler = getXmlResponseSaxParser().parse(
                    new HttpMethodReleaseInputStream(response), XmlResponsesSaxParser.ListBucketAliasHandler.class,
                    true);

            ListBucketAliasResult listBucketAliasResult = new ListBucketAliasResult(handler.getListBucketAlias(), handler.getBucketAliasOwner());
            setHeadersAndStatus(listBucketAliasResult, response);
            return listBucketAliasResult;
        } catch (Exception e) {
            throw new ObsException("get alias buckets  failed ", e.getMessage());
        }
    }

    protected void createBucketWithClusterId(CreateVirtualBucketRequest request, List<String> azIds) {
        String locationClusterGroupIdHeader = this.getProviderCredentials().getLocalAuthType("")
                != AuthTypeEnum.OBS ? Constants.V2_HEADER_PREFIX : Constants.OBS_HEADER_PREFIX
                + CommonHeaders.LOCATION_CLUSTERGROUP_ID;
        BucketMetadataInfoRequest bucketMetadataInfoRequest = new BucketMetadataInfoRequest();
        bucketMetadataInfoRequest.setBucketName(request.getBucketName1());
        String bucket1Id = "";
        String bucket2Id = "";
        try {
            GetBucketFSStatusResult bucket1Metadata = getBucketMetadataImpl(bucketMetadataInfoRequest);
            bucket1Id = bucket1Metadata.getOriginalHeaders().get(locationClusterGroupIdHeader).toString();
        } catch (ServiceException e) {
            if (e.getResponseCode() == 404) {
                log.warn("Bucket is existed when create " + request.getBucketName1());
            } else {
                throw e;
            }
        }

        try {
            bucketMetadataInfoRequest.setBucketName(request.getBucketName2());
            GetBucketFSStatusResult bucket2Metadata = getBucketMetadataImpl(bucketMetadataInfoRequest);
            bucket2Id = bucket2Metadata.getOriginalHeaders().get(locationClusterGroupIdHeader).toString();
        } catch (ServiceException e) {
            if (e.getResponseCode() == 404) {
                log.warn("Bucket is existed when create " + request.getBucketName2());
            } else {
                throw e;
            }
        }

        //two bucket is not existed
        if (bucket1Id.isEmpty() && bucket2Id.isEmpty()) {
            createBucketWithClusterGroupId(locationClusterGroupIdHeader, request.getBucketName1(),
                    request.getRegionId(), azIds.get(0));
            createBucketWithClusterGroupId(locationClusterGroupIdHeader, request.getBucketName2(),
                    request.getRegionId(), azIds.get(1));
        } else if (bucket1Id.isEmpty() && !bucket2Id.isEmpty()) {
            createBucketWithClusterGroupId(locationClusterGroupIdHeader, request.getBucketName1(),
                    request.getRegionId(), azIds.get(0) == bucket2Id ? azIds.get(1) : azIds.get(0));
        } else if (!bucket1Id.isEmpty() && bucket2Id.isEmpty()) {
            createBucketWithClusterGroupId(locationClusterGroupIdHeader, request.getBucketName2(),
                    request.getRegionId(), azIds.get(0) == bucket1Id ? azIds.get(1) : azIds.get(0));
        } else if (bucket1Id == bucket2Id) {
            throw new ObsException("the two bucket is in same az");
        }
    }

    private void createBucketWithClusterGroupId(String locationClusterGroupIdHeader, String bucketName, String regionId,
                                                String azId) {
        CreateBucketRequest request = new CreateBucketRequest();
        request.setBucketName(bucketName);
        request.addUserHeaders(locationClusterGroupIdHeader, azId);
        request.setLocation(regionId);
        createBucketImpl(request);
    }

    protected void bindBucketAliasImpl(String bucketName, String bucketAlias) {
        try {
            Map<String, String> httpHeaders = new HashMap<>();
            httpHeaders.put(Constants.CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);

            Map<String, String> requestParams = new HashMap<>();
            requestParams.put(SpecialParamEnum.OBSALIAS.getOriginalStringCode(), "");
            NewTransResult transResult = new NewTransResult();
            transResult.setHttpMethod(HttpMethodEnum.PUT);
            transResult.setParams(requestParams);
            transResult.setBucketName(bucketName);
            OBSXMLBuilder xmlBuilder = OBSXMLBuilder.create("AliasList");
            xmlBuilder.elem("Alias").t(bucketAlias);
            transResult.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xmlBuilder.asString()));
            Request.Builder builder = setupConnection(transResult, false, false);

            renameMetadataKeys(bucketName, builder, httpHeaders);

            Response response = performRequest(
                    builder.build(), requestParams, bucketName, true, true);
        } catch (Exception e) {
            throw new ObsException("bind bucket alias ", e.getMessage());
        }

    }

    protected void createBucketAliasImpl(CreateVirtualBucketRequest request) {
        try {
            Map<String, String> httpHeaders = new HashMap<>();
            httpHeaders.put(Constants.CommonHeaders.CONTENT_TYPE, Mimetypes.MIMETYPE_XML);

            Map<String, String> requestParams = new HashMap<>();
            requestParams.put(SpecialParamEnum.OBSBUCKETALIAS.getOriginalStringCode(), "");
            NewTransResult transResult = new NewTransResult();
            transResult.setHttpMethod(HttpMethodEnum.PUT);
            transResult.setParams(requestParams);
            transResult.setBucketName(request.getBucketAlias());

            OBSXMLBuilder xmlBuilder = OBSXMLBuilder.create("CreateBucketAlias");
            xmlBuilder = xmlBuilder.elem("BucketList");
            xmlBuilder.elem("Bucket").t(request.getBucketName1());
            xmlBuilder.elem("Bucket").t(request.getBucketName2());
            transResult.setBody(createRequestBody(Mimetypes.MIMETYPE_XML, xmlBuilder.asString()));
            Request.Builder builder = setupConnection(transResult, false, false);

            renameMetadataKeys(request.getBucketAlias(), builder, httpHeaders);

            Response response = performRequest(
                    builder.build(), requestParams, request.getBucketAlias(), true, false);
        } catch (Exception e) {
            throw new ObsException("create bucket alias ", e.getMessage());
        }
    }

    protected void setVirtualReplication(String agencyId, String sourceBucketName, String destBucketName) {
        ReplicationConfiguration replicationConfiguration = new ReplicationConfiguration();
        ReplicationConfiguration.Rule rule = new ReplicationConfiguration.Rule();
        rule.setId(sourceBucketName + "_to_" + destBucketName);
        rule.setStatus(RuleStatusEnum.ENABLED);
        rule.setHistoricalObjectReplication(HistoricalObjectReplicationEnum.ENABLED);
        ReplicationConfiguration.Destination destination = new ReplicationConfiguration.Destination();
        destination.setBucket(destBucketName);
        destination.setObjectStorageClass(StorageClassEnum.STANDARD);
        rule.setDestination(destination);
        List<ReplicationConfiguration.Rule> rules = new ArrayList<>();
        rules.add(rule);
        replicationConfiguration.setRules(rules);
        replicationConfiguration.setAgency(agencyId);
        SetBucketReplicationRequest request = new SetBucketReplicationRequest(sourceBucketName, replicationConfiguration);
        setBucketReplicationConfigurationImpl(request);
    }
}
