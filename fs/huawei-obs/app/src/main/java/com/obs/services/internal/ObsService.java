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

package com.obs.services.internal;

import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.obs.services.internal.Constants.CommonHeaders;
import com.obs.services.internal.security.BasicSecurityKey;
import com.obs.services.internal.service.ObsExtensionService;
import com.obs.services.internal.task.BlockRejectedExecutionHandler;
import com.obs.services.internal.task.DefaultTaskProgressStatus;
import com.obs.services.internal.utils.AbstractAuthentication;
import com.obs.services.internal.utils.RestUtils;
import com.obs.services.internal.utils.ServiceUtils;
import com.obs.services.internal.utils.V2Authentication;
import com.obs.services.internal.utils.V4Authentication;
import com.obs.services.model.AbstractBulkRequest;
import com.obs.services.model.AbstractTemporarySignatureRequest;
import com.obs.services.model.AuthTypeEnum;
import com.obs.services.model.DeleteObjectResult;
import com.obs.services.model.PolicyTempSignatureRequest;
import com.obs.services.model.PostSignatureRequest;
import com.obs.services.model.PostSignatureResponse;
import com.obs.services.model.SpecialParamEnum;
import com.obs.services.model.TaskCallback;
import com.obs.services.model.TaskProgressListener;
import com.obs.services.model.TemporarySignatureRequest;
import com.obs.services.model.TemporarySignatureResponse;
import com.obs.services.model.V4PostSignatureResponse;

public class ObsService extends ObsExtensionService {
    protected ObsService() {

    }

    protected TemporarySignatureResponse createTemporarySignatureResponse(AbstractTemporarySignatureRequest request)
            throws Exception {
        String hostname = ServiceUtils.generateHostnameForBucket(request.getBucketName(), 
                this.isPathStyle(), this.getEndpoint());
        String virtualBucketPath = "";
        String uriPath = "";
        String objectKeyPath = (request.getObjectKey() != null) 
                ? RestUtils.encodeUrlPath(request.getObjectKey(), "/") : "";
        if (!this.getEndpoint().equals(hostname)) {
            int subdomainOffset = hostname.lastIndexOf("." + this.getEndpoint());
            if (subdomainOffset > 0) {
                virtualBucketPath = hostname.substring(0, subdomainOffset) + "/";
            }
            uriPath = objectKeyPath;
        } else {
            uriPath = (((!ServiceUtils.isValid(request.getBucketName())) 
                    ? "" : request.getBucketName().trim()) + "/" + objectKeyPath);
        }

        if (this.isCname()) {
            hostname = this.getEndpoint();
            uriPath = objectKeyPath;
            virtualBucketPath = this.getEndpoint() + "/";
        }

        uriPath += "?";
        if (request.getSpecialParam() != null) {
            if (request.getSpecialParam() == SpecialParamEnum.STORAGECLASS
                    || request.getSpecialParam() == SpecialParamEnum.STORAGEPOLICY) {
                request.setSpecialParam(this.getSpecialParamForStorageClass(request.getBucketName()));
            }
            uriPath += request.getSpecialParam().getOriginalStringCode() + "&";
        }

        BasicSecurityKey securityKey = this.getProviderCredentials().getSecurityKey();
        
        uriPath = appendAccessKey(request.getBucketName(), uriPath, securityKey);

        String expiresOrPolicy = getExpiresParams(request);
        uriPath = appendExpiresParams(request, uriPath, expiresOrPolicy);

        uriPath = appendQueryParams(request, uriPath, securityKey);

        String resource = "";
        if (request instanceof TemporarySignatureRequest) {
            resource = "/" + virtualBucketPath + uriPath;
        }

        AbstractAuthentication authentication = 
                Constants.AUTHTICATION_MAP.get(this.getProviderCredentials().getLocalAuthType(request.getBucketName()));
        if (authentication == null) {
            authentication = V2Authentication.getInstance();
        }
        
        Map<String, String> headers = new HashMap<String, String>();
        headers.putAll(request.getHeaders());
        headers.put(CommonHeaders.HOST,
                hostname + ":" + (this.getHttpsOnly() ? this.getHttpsPort() : this.getHttpPort()));
        String requestMethod = request.getMethod() != null ? request.getMethod().getOperationType() : "GET";
        
        Map<String, String> actualSignedRequestHeaders = convertHeader(request.getBucketName(), headers, requestMethod);
        String canonicalString = authentication.makeServiceCanonicalString(requestMethod, resource,
                actualSignedRequestHeaders, expiresOrPolicy, Constants.ALLOWED_RESOURCE_PARAMTER_NAMES);

        uriPath = appendSignature(uriPath, securityKey, canonicalString);

        String signedUrl;
        if (this.getHttpsOnly()) {
            signedUrl = "https://";
        } else {
            signedUrl = "http://";
        }
        signedUrl += headers.get(CommonHeaders.HOST) + "/" + uriPath;
        TemporarySignatureResponse response = new TemporarySignatureResponse(signedUrl);
        response.getActualSignedRequestHeaders().putAll(actualSignedRequestHeaders);
        return response;
    }

    private String appendExpiresParams(AbstractTemporarySignatureRequest request, String uriPath,
            String expiresOrPolicy) {
        if (request instanceof TemporarySignatureRequest) {
            uriPath += "&Expires=" + expiresOrPolicy;
        } else if (request instanceof PolicyTempSignatureRequest) {
            uriPath += "&Policy=" + expiresOrPolicy;
        }
        return uriPath;
    }

    private String appendSignature(String uriPath, BasicSecurityKey securityKey, String canonicalString) {
        String signedCanonical = ServiceUtils.signWithHmacSha1(securityKey.getSecretKey(), canonicalString);
        String encodedCanonical = RestUtils.encodeUrlString(signedCanonical);
        uriPath += "&Signature=" + encodedCanonical;
        return uriPath;
    }

    private String appendAccessKey(String bucketName, String uriPath, BasicSecurityKey securityKey) {
        String accessKeyIdPrefix = this.getProviderCredentials().getLocalAuthType(bucketName)
                == AuthTypeEnum.OBS ? "AccessKeyId=" : "AWSAccessKeyId=";
        uriPath += accessKeyIdPrefix + securityKey.getAccessKey();
        return uriPath;
    }

    private String getExpiresParams(AbstractTemporarySignatureRequest request) {
        String expiresOrPolicy = "";
        if (request instanceof TemporarySignatureRequest) {
            TemporarySignatureRequest tempRequest = (TemporarySignatureRequest) request;
            long secondsSinceEpoch = tempRequest.getExpires() <= 0 ? ObsConstraint.DEFAULT_EXPIRE_SECONEDS
                    : tempRequest.getExpires();
            secondsSinceEpoch += System.currentTimeMillis() / 1000;
            expiresOrPolicy = String.valueOf(secondsSinceEpoch);
        } else if (request instanceof PolicyTempSignatureRequest) {
            PolicyTempSignatureRequest policyRequest = (PolicyTempSignatureRequest) request;
            String policy = policyRequest.generatePolicy();
            expiresOrPolicy = ServiceUtils.toBase64(policy.getBytes(StandardCharsets.UTF_8));
        }
        
        return expiresOrPolicy;
    }
    
    private Map<String, String> convertHeader(String bucketName, Map<String, String> headers, String requestMethod) {
        Map<String, String> actualSignedRequestHeaders = new TreeMap<String, String>();
        
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (ServiceUtils.isValid(entry.getKey())) {
                String key = entry.getKey().toLowerCase().trim();
                key = formatHeaderKey(bucketName, requestMethod, key);
                
                if (null != key) {
                    String value = entry.getValue() == null ? "" : entry.getValue().trim();
                    if (key.startsWith(this.getRestMetadataPrefix(bucketName))) {
                        value = RestUtils.uriEncode(value, true);
                    }
                    actualSignedRequestHeaders.put(entry.getKey().trim(), value);
                }
            }
        }
        
        return actualSignedRequestHeaders;
    }

    /**
     * 拼接query参数
     * @param request
     * @param uriPath
     * @param securityKey
     * @return
     */
    private String appendQueryParams(AbstractTemporarySignatureRequest request, String uriPath,
            BasicSecurityKey securityKey) {
        StringBuilder temp = new StringBuilder(uriPath);
        Map<String, Object> queryParams = new TreeMap<String, Object>();
        queryParams.putAll(request.getQueryParams());
        String securityToken = securityKey.getSecurityToken();
        if (!queryParams.containsKey(this.getIHeaders(request.getBucketName()).securityTokenHeader())) {
            if (ServiceUtils.isValid(securityToken)) {
                queryParams.put(this.getIHeaders(request.getBucketName()).securityTokenHeader(), securityToken);
            }
        }
        
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null) {
                String key = RestUtils.uriEncode(entry.getKey(), false);

                temp.append("&");
                temp.append(key);
                temp.append("=");
                String value = RestUtils.uriEncode(entry.getValue().toString(), false);
                temp.append(value);
            }
        }
        return temp.toString();
    }

    protected PostSignatureResponse createPostSignatureResponse(PostSignatureRequest request, boolean isV4)
            throws Exception {
        BasicSecurityKey securityKey = this.getProviderCredentials().getSecurityKey();
        String expiration = getFormatExpiration(request);

        StringBuilder originPolicy = new StringBuilder();
        originPolicy.append("{\"expiration\":").append("\"").append(expiration).append("\",")
                .append("\"conditions\":[");

        Date requestDate = request.getRequestDate() != null ? request.getRequestDate() : new Date();
        String shortDate = ServiceUtils.getShortDateFormat().format(requestDate);
        String longDate = ServiceUtils.getLongDateFormat().format(requestDate);
        String credential = this.getCredential(shortDate, securityKey.getAccessKey());
        if (request.getConditions() != null && !request.getConditions().isEmpty()) {
            originPolicy.append(ServiceUtils.join(request.getConditions(), ",")).append(",");
        } else {
            appendOriginPolicy(request, isV4, securityKey, originPolicy, longDate, credential);
        }

        if (originPolicy.length() > 0 && originPolicy.charAt(originPolicy.length() - 1) == ',') {
            originPolicy.deleteCharAt(originPolicy.length() - 1);
        }

        originPolicy.append("]}");
        String policy = ServiceUtils.toBase64(originPolicy.toString().getBytes(StandardCharsets.UTF_8));

        if (isV4) {
            String signature = V4Authentication.caculateSignature(policy, shortDate, securityKey.getSecretKey());
            return new V4PostSignatureResponse(policy, originPolicy.toString(), Constants.V4_ALGORITHM, credential,
                    longDate, signature, expiration);
        } else {
            String signature = AbstractAuthentication.calculateSignature(policy, securityKey.getSecretKey());
            return new PostSignatureResponse(policy, originPolicy.toString(), 
                    signature, expiration, securityKey.getAccessKey());
        }

    }

    private void appendOriginPolicy(PostSignatureRequest request, boolean isV4, BasicSecurityKey securityKey,
            StringBuilder originPolicy, String longDate, String credential) {
        Map<String, Object> params = prepareSignatureParameters(request, isV4, 
                securityKey.getSecurityToken(), longDate, credential);

        boolean matchAnyBucket = true;
        boolean matchAnyKey = true;

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            if (ServiceUtils.isValid(entry.getKey())) {
                String key = entry.getKey().toLowerCase().trim();

                if (key.equals("bucket")) {
                    matchAnyBucket = false;
                } else if (key.equals("key")) {
                    matchAnyKey = false;
                }

                if (!Constants.ALLOWED_REQUEST_HTTP_HEADER_METADATA_NAMES.contains(key)
                        && !key.startsWith(this.getRestHeaderPrefix(request.getBucketName()))
                        && !key.startsWith(Constants.OBS_HEADER_PREFIX) && !key.equals("acl")
                        && !key.equals("bucket") && !key.equals("key") && !key.equals("success_action_redirect")
                        && !key.equals("redirect") && !key.equals("success_action_status")) {
                    continue;
                }
                String value = entry.getValue() == null ? "" : entry.getValue().toString();
                originPolicy.append("{\"").append(key).append("\":").append("\"").append(value).append("\"},");
            }
        }

        if (matchAnyBucket) {
            originPolicy.append("[\"starts-with\", \"$bucket\", \"\"],");
        }

        if (matchAnyKey) {
            originPolicy.append("[\"starts-with\", \"$key\", \"\"],");
        }
    }

    private String getFormatExpiration(PostSignatureRequest request) {
        Date requestDate = request.getRequestDate() != null ? request.getRequestDate() : new Date();
        SimpleDateFormat expirationDateFormat = ServiceUtils.getExpirationDateFormat();
        Date expiryDate = request.getExpiryDate() == null ? new Date(requestDate.getTime()
                + (request.getExpires() <= 0 ? ObsConstraint.DEFAULT_EXPIRE_SECONEDS : request.getExpires()) * 1000)
                : request.getExpiryDate();

        String expiration = expirationDateFormat.format(expiryDate);
        return expiration;
    }

    private Map<String, Object> prepareSignatureParameters(PostSignatureRequest request, boolean isV4,
            String securityToken, String longDate, String credential) {
        Map<String, Object> params = new TreeMap<String, Object>();
        if (isV4) {
            params.put(Constants.V2_HEADER_PREFIX_CAMEL + "Algorithm", Constants.V4_ALGORITHM);
            params.put(Constants.V2_HEADER_PREFIX_CAMEL + "Date", longDate);
            params.put(Constants.V2_HEADER_PREFIX_CAMEL + "Credential", credential);
        }

        params.putAll(request.getFormParams());

        if (!params.containsKey(this.getIHeaders(request.getBucketName()).securityTokenHeader())) {
            if (ServiceUtils.isValid(securityToken)) {
                params.put(this.getIHeaders(request.getBucketName()).securityTokenHeader(), securityToken);
            }
        }

        if (ServiceUtils.isValid(request.getBucketName())) {
            params.put("bucket", request.getBucketName());
        }

        if (ServiceUtils.isValid(request.getObjectKey())) {
            params.put("key", request.getObjectKey());
        }
        return params;
    }

    protected TemporarySignatureResponse createV4TemporarySignature(TemporarySignatureRequest request)
            throws Exception {
        StringBuilder canonicalUri = new StringBuilder();
        String bucketName = request.getBucketName();
        String endpoint = this.getEndpoint();
        String objectKey = request.getObjectKey();

        if (!this.isCname()) {
            if (ServiceUtils.isValid(bucketName)) {
                if (this.isPathStyle() || !ServiceUtils.isBucketNameValidDNSName(bucketName)) {
                    canonicalUri.append("/").append(bucketName.trim());
                } else {
                    endpoint = bucketName.trim() + "." + endpoint;
                }
                if (ServiceUtils.isValid(objectKey)) {
                    canonicalUri.append("/").append(RestUtils.uriEncode(objectKey, false));
                }
            }
        } else {
            if (ServiceUtils.isValid(objectKey)) {
                canonicalUri.append("/").append(RestUtils.uriEncode(objectKey, false));
            }
        }

        if (this.isCname()) {
            endpoint = this.getEndpoint();
        }

        String requestMethod = request.getMethod() != null ? request.getMethod().getOperationType() : "GET";
        Map<String, String> actualSignedRequestHeaders = new TreeMap<String, String>();
        
        Map<String, String> headers = createBaseRequestHeaders(request, endpoint);
        
        StringBuilder signedHeaders = new StringBuilder();
        StringBuilder canonicalHeaders = new StringBuilder();
        concatHeaderString(bucketName, requestMethod, actualSignedRequestHeaders,
                headers, signedHeaders, canonicalHeaders);

        Date requestDate = request.getRequestDate();
        if (requestDate == null) {
            requestDate = new Date();
        }
        String shortDate = ServiceUtils.getShortDateFormat().format(requestDate);
        String longDate = ServiceUtils.getLongDateFormat().format(requestDate);
        
        BasicSecurityKey securityKey = this.getProviderCredentials().getSecurityKey();
        Map<String, Object> queryParams = createBaseQueryParams(request, signedHeaders.toString(), shortDate, 
                longDate, securityKey.getAccessKey(), securityKey.getSecurityToken());

        StringBuilder signedUrl = createBaseSignedUrl(canonicalUri, endpoint);
        StringBuilder canonicalQueryString = new StringBuilder();
        concatQueryString(queryParams, signedUrl, canonicalQueryString);

        StringBuilder canonicalRequest = new StringBuilder(requestMethod).append("\n")
                .append(canonicalUri.length() == 0 ? "/" : canonicalUri).append("\n").append(canonicalQueryString)
                .append("\n").append(canonicalHeaders).append("\n").append(signedHeaders).append("\n")
                .append("UNSIGNED-PAYLOAD");

        StringBuilder stringToSign = new StringBuilder(Constants.V4_ALGORITHM).append("\n").append(longDate)
                .append("\n").append(shortDate).append("/").append(ObsConstraint.DEFAULT_BUCKET_LOCATION_VALUE)
                .append("/").append(Constants.SERVICE).append("/").append(Constants.REQUEST_TAG).append("\n")
                .append(V4Authentication.byteToHex((V4Authentication.sha256encode(canonicalRequest.toString()))));
        signedUrl.append("&").append(Constants.V2_HEADER_PREFIX_CAMEL).append("Signature=")
                .append(V4Authentication.caculateSignature(stringToSign.toString(), shortDate, 
                        securityKey.getSecretKey()));
        TemporarySignatureResponse response = new TemporarySignatureResponse(signedUrl.toString());
        response.getActualSignedRequestHeaders().putAll(actualSignedRequestHeaders);
        return response;
    }

    private StringBuilder createBaseSignedUrl(StringBuilder canonicalUri, String endpoint) {
        StringBuilder signedUrl = new StringBuilder();
        if (this.getHttpsOnly()) {
            String securePortStr = this.getHttpsPort() == 443 ? "" : ":" + this.getHttpsPort();
            signedUrl.append("https://").append(endpoint).append(securePortStr);
        } else {
            String insecurePortStr = this.getHttpPort() == 80 ? "" : ":" + this.getHttpPort();
            signedUrl.append("http://").append(endpoint).append(insecurePortStr);
        }
        signedUrl.append(canonicalUri).append("?");
        return signedUrl;
    }

    private void concatQueryString(Map<String, Object> queryParams, StringBuilder signedUrl,
            StringBuilder canonicalQueryString) {
        int index = 0;
        for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
            if (ServiceUtils.isValid(entry.getKey())) {
                String key = RestUtils.uriEncode(entry.getKey(), false);

                canonicalQueryString.append(key).append("=");
                signedUrl.append(key);
                if (entry.getValue() != null) {
                    String value = RestUtils.uriEncode(entry.getValue().toString(), false);
                    canonicalQueryString.append(value);
                    signedUrl.append("=").append(value);
                } else {
                    canonicalQueryString.append("");
                }
                if (index++ != queryParams.size() - 1) {
                    canonicalQueryString.append("&");
                    signedUrl.append("&");
                }
            }
        }
    }

    private String formatHeaderKey(String bucketName, String requestMethod, String originalKey) {
        if (Constants.ALLOWED_REQUEST_HTTP_HEADER_METADATA_NAMES.contains(originalKey)
                || originalKey.startsWith(this.getRestHeaderPrefix(bucketName))
                || originalKey.startsWith(Constants.OBS_HEADER_PREFIX)) {
            return originalKey;
        } else if (requestMethod.equals("PUT") || requestMethod.equals("POST")) {
            return this.getRestMetadataPrefix(bucketName) + originalKey;
        }
        
        return null;
    }

    private void concatHeaderString(String bucketName, String requestMethod,
                                    Map<String, String> actualSignedRequestHeaders, Map<String, String> headers,
                                    StringBuilder signedHeaders, StringBuilder canonicalHeaders) {
        int index = 0;
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            if (ServiceUtils.isValid(entry.getKey())) {
                String key = entry.getKey().toLowerCase().trim();
                key = formatHeaderKey(bucketName, requestMethod, key);
                
                if (null != key) {
                    String value = entry.getValue() == null ? "" : entry.getValue().trim();
                    if (key.startsWith(this.getRestMetadataPrefix(bucketName))) {
                        value = RestUtils.uriEncode(value, true);
                    }
                    signedHeaders.append(key);
                    canonicalHeaders.append(key).append(":").append(value).append("\n");
                    if (index++ != headers.size() - 1) {
                        signedHeaders.append(";");
                    }
                    actualSignedRequestHeaders.put(entry.getKey().trim(), value);
                }
            }
        }
    }

    private Map<String, String> createBaseRequestHeaders(TemporarySignatureRequest request, String endpoint) {
        Map<String, String> headers = new TreeMap<String, String>();
        headers.putAll(request.getHeaders());
        if ((this.getHttpsOnly() && this.getHttpsPort() == 443) || (!this.getHttpsOnly() && this.getHttpPort() == 80)) {
            headers.put(CommonHeaders.HOST, endpoint);
        } else {
            headers.put(CommonHeaders.HOST,
                    endpoint + ":" + (this.getHttpsOnly() ? this.getHttpsPort() : this.getHttpPort()));
        }
        return headers;
    }

    private Map<String, Object> createBaseQueryParams(TemporarySignatureRequest request, String signedHeaders,
            String shortDate, String longDate, String accessKey, String securityToken) {
        Map<String, Object> queryParams = new TreeMap<String, Object>();
        queryParams.putAll(request.getQueryParams());
        if (!queryParams.containsKey(this.getIHeaders(request.getBucketName()).securityTokenHeader())) {
            if (ServiceUtils.isValid(securityToken)) {
                queryParams.put(this.getIHeaders(request.getBucketName()).securityTokenHeader(), securityToken);
            }
        }
        
        queryParams.put(Constants.V2_HEADER_PREFIX_CAMEL + "Algorithm", Constants.V4_ALGORITHM);
        queryParams.put(Constants.V2_HEADER_PREFIX_CAMEL + "Credential", 
                this.getCredential(shortDate, accessKey));
        queryParams.put(Constants.V2_HEADER_PREFIX_CAMEL + "Date", longDate);
        queryParams.put(Constants.V2_HEADER_PREFIX_CAMEL + "Expires",
                request.getExpires() <= 0 ? ObsConstraint.DEFAULT_EXPIRE_SECONEDS : request.getExpires());
        queryParams.put(Constants.V2_HEADER_PREFIX_CAMEL + "SignedHeaders", signedHeaders);
        
        if (request.getSpecialParam() != null) {
            if (request.getSpecialParam() == SpecialParamEnum.STORAGECLASS
                    || request.getSpecialParam() == SpecialParamEnum.STORAGEPOLICY) {
                request.setSpecialParam(this.getSpecialParamForStorageClass(request.getBucketName()));
            }
            queryParams.put(request.getSpecialParam().getOriginalStringCode(), null);
        }
        
        return queryParams;
    }

    protected ThreadPoolExecutor initThreadPool(AbstractBulkRequest request) {
        int taskThreadNum = request.getTaskThreadNum();
        int workQueenLength = request.getTaskQueueNum();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(taskThreadNum, taskThreadNum, 0, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(workQueenLength));
        executor.setRejectedExecutionHandler(new BlockRejectedExecutionHandler());
        return executor;
    }

    protected void recordBulkTaskStatus(DefaultTaskProgressStatus progressStatus,
            TaskCallback<DeleteObjectResult, String> callback, TaskProgressListener listener, int interval) {

        progressStatus.execTaskIncrement();
        if (listener != null) {
            if (progressStatus.getExecTaskNum() % interval == 0) {
                listener.progressChanged(progressStatus);
            }
            if (progressStatus.getExecTaskNum() == progressStatus.getTotalTaskNum()) {
                listener.progressChanged(progressStatus);
            }
        }
    }
}
