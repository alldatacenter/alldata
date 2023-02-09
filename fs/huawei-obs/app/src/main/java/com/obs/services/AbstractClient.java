/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */


package com.obs.services;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.obs.log.ILogger;
import com.obs.log.InterfaceLogBean;
import com.obs.log.LoggerBuilder;
import com.obs.services.exception.ObsException;
import com.obs.services.internal.Constants;
import com.obs.services.internal.ObsConstraint;
import com.obs.services.internal.ObsProperties;
import com.obs.services.internal.ObsService;
import com.obs.services.internal.ServiceException;
import com.obs.services.internal.security.ProviderCredentials;
import com.obs.services.internal.utils.AccessLoggerUtils;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.internal.xml.OBSXMLBuilder;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.HttpMethodEnum;
import com.obs.services.model.PolicyConditionItem;
import com.obs.services.model.PolicyTempSignatureRequest;
import com.obs.services.model.PostSignatureRequest;
import com.obs.services.model.PostSignatureResponse;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;
import com.obs.services.model.V4PostSignatureRequest;
import com.obs.services.model.V4PostSignatureResponse;
import com.obs.services.model.V4TemporarySignatureRequest;
import com.obs.services.model.V4TemporarySignatureResponse;
import static com.jamesmurty.utils.BaseXMLBuilder.failIfExternalEntityParsingCannotBeConfigured;

public abstract class AbstractClient extends ObsService implements Closeable, IObsClient, IFSClient {
    private static final ILogger ILOG = LoggerBuilder.getLogger(AbstractClient.class);

    static {
        failIfExternalEntityParsingCannotBeConfigured = false;
    }
    
    protected void init(String accessKey, String secretKey, String securityToken, ObsConfiguration config) {
        InterfaceLogBean reqBean = new InterfaceLogBean("ObsClient", config.getEndPoint(), "");
        ProviderCredentials credentials = new ProviderCredentials(accessKey, secretKey, securityToken);
        ObsProperties obsProperties = ServiceUtils.changeFromObsConfiguration(config);
        credentials.setAuthType(config.getAuthType());
        credentials.setLocalAuthTypeCacheCapacity(config.getLocalAuthTypeCacheCapacity());
        this.obsProperties = obsProperties;
        this.credentials = credentials;
        this.keyManagerFactory = config.getKeyManagerFactory();
        this.trustManagerFactory = config.getTrustManagerFactory();
        if (this.isAuthTypeNegotiation()) {
            this.getProviderCredentials().setIsAuthTypeNegotiation(true);
        }
        this.initHttpClient(config.getHttpDispatcher());
        OBSXMLBuilder.setXmlDocumentBuilderFactoryClass(config.getXmlDocumentBuilderFactoryClass());
        reqBean.setRespTime(new Date());
        reqBean.setResultCode(Constants.RESULTCODE_SUCCESS);
        if (ILOG.isInfoEnabled()) {
            ILOG.info(reqBean);
        }

        if (ILOG.isWarnEnabled()) {
            StringBuilder sb = new StringBuilder("[OBS SDK Version=");
            sb.append(Constants.OBS_SDK_VERSION);
            sb.append("];");
            sb.append("[Endpoint=");
            String ep;
            if (this.getHttpsOnly()) {
                ep = "https://" + this.getEndpoint() + ":" + this.getHttpsPort() + "/";
            } else {
                ep = "http://" + this.getEndpoint() + ":" + this.getHttpPort() + "/";
            }
            sb.append(ep);
            sb.append("];");
            sb.append("[Access Mode=");
            sb.append(this.isPathStyle() ? "Path" : "Virtul Hosting");
            sb.append("]");
            ILOG.warn(sb);
        }
    }
    
    
    /**
     * Create a temporarily authorized URL.
     *
     * @param method
     *            HTTP request method
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param specialParam
     *            Special operator
     * @param expiryTime
     *            Time when the temporary authentication expires
     * @param headers
     *            Header information
     * @param queryParams
     *            Query parameter information
     * @return Temporarily authorized URL
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    @Deprecated
    public String createSignedUrl(HttpMethodEnum method, String bucketName, String objectKey,
            SpecialParamEnum specialParam, Date expiryTime, Map<String, String> headers,
            Map<String, Object> queryParams) throws ObsException {
        return this.createSignedUrl(method, bucketName, objectKey, specialParam, expiryTime == null
                ? ObsConstraint.DEFAULT_EXPIRE_SECONEDS : (expiryTime.getTime() - System.currentTimeMillis()) / 1000,
                headers, queryParams);
    }

    /**
     * Create a temporarily authorized URL.
     *
     * @param method
     *            HTTP request method
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param specialParam
     *            Special operator
     * @param expires
     *            Time when the temporary authentication expires. The unit is
     *            second and the default value is 300.
     * @param headers
     *            Header information
     * @param queryParams
     *            Query parameter information
     * @return Temporarily authorized URL
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    @Deprecated
    public String createSignedUrl(HttpMethodEnum method, String bucketName, String objectKey,
            SpecialParamEnum specialParam, long expires, Map<String, String> headers, Map<String, Object> queryParams) {
        TemporarySignatureRequest request = new TemporarySignatureRequest();
        request.setMethod(method);
        request.setBucketName(bucketName);
        request.setObjectKey(objectKey);
        request.setSpecialParam(specialParam);
        request.setHeaders(headers);
        request.setQueryParams(queryParams);
        if (expires > 0) {
            request.setExpires(expires);
        }
        return createTemporarySignature(request).getSignedUrl();
    }

    @Deprecated
    public V4TemporarySignatureResponse createV4TemporarySignature(V4TemporarySignatureRequest request) {
        ServiceUtils.assertParameterNotNull(request, "V4TemporarySignatureRequest is null");
        InterfaceLogBean reqBean = new InterfaceLogBean("createV4TemporarySignature", this.getEndpoint(), "");
        try {
            TemporarySignatureResponse response = this.createV4TemporarySignature((TemporarySignatureRequest) request);
            V4TemporarySignatureResponse res = new V4TemporarySignatureResponse(response.getSignedUrl());
            res.getActualSignedRequestHeaders().putAll(response.getActualSignedRequestHeaders());
            return res;
        } catch (Exception e) {
            reqBean.setRespTime(new Date());
            if (ILOG.isErrorEnabled()) {
                ILOG.error(reqBean);
            }
            throw new ObsException(e.getMessage(), e);
        }
    }

    @Deprecated
    public V4PostSignatureResponse createV4PostSignature(String acl, String contentType, long expires,
            String bucketName, String objectKey) throws ObsException {
        V4PostSignatureRequest request = new V4PostSignatureRequest(expires, new Date(), bucketName, objectKey);
        request.getFormParams().put("acl", acl);
        request.getFormParams().put("content-type", contentType);
        return this.createV4PostSignature(request);
    }

    @Deprecated
    public V4PostSignatureResponse createV4PostSignature(long expires, String bucketName, String objectKey)
            throws ObsException {
        V4PostSignatureRequest request = new V4PostSignatureRequest(expires, new Date(), bucketName, objectKey);
        return this.createV4PostSignature(request);
    }

    @Deprecated
    public V4PostSignatureResponse createV4PostSignature(V4PostSignatureRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "V4PostSignatureRequest is null");
        InterfaceLogBean reqBean = new InterfaceLogBean("createV4PostSignature", this.getEndpoint(), "");
        return (V4PostSignatureResponse)createPostSignature(request, reqBean, true);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#createTemporarySignature(com.obs.services.
     * model.TemporarySignatureRequest)
     */
    @Override
    public TemporarySignatureResponse createTemporarySignature(TemporarySignatureRequest request) {
        ServiceUtils.assertParameterNotNull(request, "TemporarySignatureRequest is null");
        InterfaceLogBean reqBean = new InterfaceLogBean("createTemporarySignature", this.getEndpoint(), "");
        try {
            return this.getProviderCredentials().getLocalAuthType(request.getBucketName()) == AuthTypeEnum.V4
                    ? this.createV4TemporarySignature(request) : this.createTemporarySignatureResponse(request);
        } catch (Exception e) {
            reqBean.setRespTime(new Date());
            if (ILOG.isErrorEnabled()) {
                ILOG.error(reqBean);
            }
            throw new ObsException(e.getMessage(), e);
        }
    }

    /**
     * Generate temporary authorization parameters for GET requests based on the
     * object name prefix and validity period.
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param prefix
     *            Object name prefix
     * @param expiryDate
     *            Expiration date (ISO 8601 UTC)
     * @param headers
     *            Header information
     * @param queryParams
     *            Query parameter information
     * @return Response to the request for temporary access authorization
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public TemporarySignatureResponse createGetTemporarySignature(String bucketName, String objectKey, String prefix,
            Date expiryDate, Map<String, String> headers, Map<String, Object> queryParams) {
        try {
            PolicyTempSignatureRequest request = createPolicyGetRequest(bucketName, objectKey, prefix, headers,
                    queryParams);
            request.setExpiryDate(expiryDate);
            return this.createTemporarySignatureResponse(request);
        } catch (Exception e) {
            throw new ObsException(e.getMessage(), e);
        }
    }

    /**
     * Generate temporary authorization parameters for GET requests based on the
     * object name prefix and validity period.
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param prefix
     *            Object name prefix
     * @param expires
     *            Validity period (seconds)
     * @param headers
     *            Header information
     * @param queryParams
     *            Query parameter information
     * @return Response to the request for temporary access authorization
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public TemporarySignatureResponse createGetTemporarySignature(String bucketName, String objectKey, String prefix,
            long expires, Map<String, String> headers, Map<String, Object> queryParams) {
        try {
            PolicyTempSignatureRequest request = createPolicyGetRequest(bucketName, objectKey, prefix, headers,
                    queryParams);
            request.setExpires(expires);
            return this.createTemporarySignatureResponse(request);
        } catch (Exception e) {
            throw new ObsException(e.getMessage(), e);
        }
    }

    /**
     * Generate parameters for browser-based authorized access.
     *
     * @param acl
     *            Object ACL
     * @param contentType
     *            MIME type of the object
     * @param expires
     *            Validity period (in seconds)
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @return Response to the V4 browser-based authorized access
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public PostSignatureResponse createPostSignature(String acl, String contentType, long expires, String bucketName,
            String objectKey) throws ObsException {
        PostSignatureRequest request = new PostSignatureRequest(expires, new Date(), bucketName, objectKey);
        request.getFormParams().put(
                this.getProviderCredentials().getLocalAuthType(bucketName)
                        == AuthTypeEnum.V4 ? "acl" : this.getIHeaders(bucketName).aclHeader(), acl);
        request.getFormParams().put(com.obs.services.internal.Constants.CommonHeaders.CONTENT_TYPE, contentType);
        return this.createPostSignature(request);
    }

    /**
     * Generate parameters for browser-based authorized access.
     *
     * @param expires
     *            Validity period (in seconds)
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @return Response to the V4 browser-based authorized access
     * @throws ObsException
     *             OBS SDK self-defined exception, thrown when the interface
     *             fails to be called or access to OBS fails
     */
    public PostSignatureResponse createPostSignature(long expires, String bucketName, String objectKey)
            throws ObsException {
        PostSignatureRequest request = new PostSignatureRequest(expires, new Date(), bucketName, objectKey);
        return this.createPostSignature(request);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.obs.services.IObsClient#createPostSignature(com.obs.services.model.
     * PostSignatureRequest)
     */
    @Override
    public PostSignatureResponse createPostSignature(PostSignatureRequest request) throws ObsException {
        ServiceUtils.assertParameterNotNull(request, "PostSignatureRequest is null");
        InterfaceLogBean reqBean = new InterfaceLogBean("createPostSignature", this.getEndpoint(), "");
        return createPostSignature(request, reqBean, this.getProviderCredentials()
                .getLocalAuthType(request.getBucketName()) == AuthTypeEnum.V4);
    }

    protected abstract class ActionCallbackWithResult<T> {

        public abstract T action() throws ServiceException;

        void authTypeNegotiate(String bucketName) throws ServiceException {
            AuthTypeEnum authTypeEnum = AbstractClient.this.getProviderCredentials()
                    .getLocalAuthType().get(bucketName);
            if (authTypeEnum == null) {
                authTypeEnum = AbstractClient.this.getApiVersion(bucketName);
                AbstractClient.this.getProviderCredentials().setLocalAuthType(bucketName, authTypeEnum);
            }
        }
    }

    protected <T> T doActionWithResult(String action, String bucketName, ActionCallbackWithResult<T> callback)
            throws ObsException {
        if (!this.isCname()) {
            ServiceUtils.assertParameterNotNull(bucketName, "bucketName is null");
        }
        InterfaceLogBean reqBean = new InterfaceLogBean(action, this.getEndpoint(), "");
        try {
            long start = System.currentTimeMillis();
            if (this.isAuthTypeNegotiation()) {
                callback.authTypeNegotiate(bucketName);
            }
            T ret = callback.action();
            reqBean.setRespTime(new Date());
            reqBean.setResultCode(Constants.RESULTCODE_SUCCESS);
            if (ILOG.isInfoEnabled()) {
                ILOG.info(reqBean);
                ILOG.info("ObsClient [" + action + "] cost " + (System.currentTimeMillis() - start) + " ms");
            }
            return ret;
        } catch (ServiceException e) {

            ObsException ex = ServiceUtils.changeFromServiceException(e);
            if (ex.getResponseCode() >= 400 && ex.getResponseCode() < 500) {
                if (ILOG.isWarnEnabled()) {
                    reqBean.setRespTime(new Date());
                    reqBean.setResultCode(String.valueOf(e.getResponseCode()));
                    ILOG.warn(reqBean);
                }
            } else if (ILOG.isErrorEnabled()) {
                reqBean.setRespTime(new Date());
                reqBean.setResultCode(String.valueOf(ex.getResponseCode()));
                ILOG.error(reqBean);
            }
            throw ex;
        } finally {
            AccessLoggerUtils.printLog();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#refresh(java.lang.String,
     * java.lang.String, java.lang.String)
     */
    @Override
    public void refresh(String accessKey, String secretKey, String securityToken) {
        ProviderCredentials credentials = new ProviderCredentials(accessKey, secretKey, securityToken);
        credentials.setIsAuthTypeNegotiation(this.credentials.getIsAuthTypeNegotiation());
        credentials.setAuthType(this.credentials.getAuthType());
        credentials.setLocalAuthType(this.credentials.getLocalAuthType());
        this.setProviderCredentials(credentials);
    }
    
    /*
     * (non-Javadoc)
     * 
     * @see com.obs.services.IObsClient#close()
     */
    @Override
    public void close() throws IOException {
        this.shutdown();
    }
    
    public String base64Md5(InputStream is, long length, long offset) throws NoSuchAlgorithmException, IOException {
        return ServiceUtils.toBase64(ServiceUtils.computeMD5Hash(is, length, offset));
    }

    public String base64Md5(InputStream is) throws NoSuchAlgorithmException, IOException {
        return ServiceUtils.toBase64(ServiceUtils.computeMD5Hash(is));
    }
    
    private PolicyTempSignatureRequest createPolicyGetRequest(String bucketName, String objectKey, String prefix,
            Map<String, String> headers, Map<String, Object> queryParams) {
        PolicyTempSignatureRequest request = new PolicyTempSignatureRequest(HttpMethodEnum.GET, bucketName, objectKey);
        List<PolicyConditionItem> conditions = new ArrayList<PolicyConditionItem>();
        PolicyConditionItem keyCondition = 
                new PolicyConditionItem(com.obs.services.model.PolicyConditionItem.ConditionOperator.STARTS_WITH,
                        "key", prefix);
        String bucket = this.isCname() ? this.getEndpoint() : bucketName;
        PolicyConditionItem bucketCondition = 
                new PolicyConditionItem(com.obs.services.model.PolicyConditionItem.ConditionOperator.EQUAL,
                        "bucket", bucket);
        conditions.add(keyCondition);
        conditions.add(bucketCondition);
        request.setConditions(conditions);
        request.setHeaders(headers);
        request.setQueryParams(queryParams);
        return request;
    }
    
    private PostSignatureResponse createPostSignature(PostSignatureRequest request, 
            InterfaceLogBean reqBean,
            boolean isV4) {
        try {
            PostSignatureResponse response = this.createPostSignatureResponse(request,
                    isV4);
            reqBean.setRespTime(new Date());
            reqBean.setResultCode(Constants.RESULTCODE_SUCCESS);
            if (ILOG.isInfoEnabled()) {
                ILOG.info(reqBean);
            }
            return response;
        } catch (Exception e) {
            reqBean.setRespTime(new Date());
            if (ILOG.isErrorEnabled()) {
                ILOG.error(reqBean);
            }
            throw new ObsException(e.getMessage(), e);
        }
    }
}
