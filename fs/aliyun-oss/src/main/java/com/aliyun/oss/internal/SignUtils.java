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

import static com.aliyun.oss.common.utils.CodingUtils.assertTrue;
import static com.aliyun.oss.internal.RequestParameters.*;

import java.util.Arrays;
import java.util.Map;
import static com.aliyun.oss.internal.OSSConstants.DEFAULT_CHARSET_NAME;
import static com.aliyun.oss.internal.OSSUtils.populateResponseHeaderParameters;
import static com.aliyun.oss.internal.RequestParameters.SIGNATURE;
import static com.aliyun.oss.internal.SignParameters.AUTHORIZATION_PREFIX;

import java.net.URI;
import java.util.*;
import java.util.Map.Entry;

import com.aliyun.oss.ClientConfiguration;
import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.common.auth.Credentials;
import com.aliyun.oss.common.auth.ServiceSignature;
import com.aliyun.oss.common.comm.RequestMessage;
import com.aliyun.oss.common.utils.HttpHeaders;
import com.aliyun.oss.common.utils.HttpUtil;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;

public class SignUtils {

    public static String composeRequestAuthorization(String accessKeyId, String signature) {
        return AUTHORIZATION_PREFIX + accessKeyId + ":" + signature;
    }

    public static String buildCanonicalString(String method, String resourcePath, RequestMessage request,
            String expires) {

        StringBuilder canonicalString = new StringBuilder();
        canonicalString.append(method).append(SignParameters.NEW_LINE);

        Map<String, String> headers = request.getHeaders();
        TreeMap<String, String> headersToSign = new TreeMap<String, String>();

        if (headers != null) {
            for (Entry<String, String> header : headers.entrySet()) {
                if (header.getKey() == null) {
                    continue;
                }

                String lowerKey = header.getKey().toLowerCase();
                if (lowerKey.equals(HttpHeaders.CONTENT_TYPE.toLowerCase())
                        || lowerKey.equals(HttpHeaders.CONTENT_MD5.toLowerCase())
                        || lowerKey.equals(HttpHeaders.DATE.toLowerCase())
                        || lowerKey.startsWith(OSSHeaders.OSS_PREFIX)) {
                    headersToSign.put(lowerKey, header.getValue().trim());
                }
            }
        }

        if (!headersToSign.containsKey(HttpHeaders.CONTENT_TYPE.toLowerCase())) {
            headersToSign.put(HttpHeaders.CONTENT_TYPE.toLowerCase(), "");
        }
        if (!headersToSign.containsKey(HttpHeaders.CONTENT_MD5.toLowerCase())) {
            headersToSign.put(HttpHeaders.CONTENT_MD5.toLowerCase(), "");
        }

        // Append all headers to sign to canonical string
        for (Map.Entry<String, String> entry : headersToSign.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (key.startsWith(OSSHeaders.OSS_PREFIX)) {
                canonicalString.append(key).append(':').append(value);
            } else {
                canonicalString.append(value);
            }

            canonicalString.append(SignParameters.NEW_LINE);
        }

        // Append canonical resource to canonical string
        canonicalString.append(buildCanonicalizedResource(resourcePath, request.getParameters()));

        return canonicalString.toString();
    }

    public static String buildRtmpCanonicalString(String canonicalizedResource, RequestMessage request,
            String expires) {

        StringBuilder canonicalString = new StringBuilder();

        // Append expires
        canonicalString.append(expires + SignParameters.NEW_LINE);

        // Append canonicalized parameters
        for (Map.Entry<String, String> entry : request.getParameters().entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            canonicalString.append(key).append(':').append(value);
            canonicalString.append(SignParameters.NEW_LINE);
        }

        // Append canonicalized resource
        canonicalString.append(canonicalizedResource);

        return canonicalString.toString();
    }

    public static String buildSignedURL(GeneratePresignedUrlRequest request, Credentials currentCreds, ClientConfiguration config, URI endpoint) {
        String bucketName = request.getBucketName();
        String accessId = currentCreds.getAccessKeyId();
        String accessKey = currentCreds.getSecretAccessKey();
        boolean useSecurityToken = currentCreds.useSecurityToken();
        HttpMethod method = request.getMethod() != null ? request.getMethod() : HttpMethod.GET;

        String expires = String.valueOf(request.getExpiration().getTime() / 1000L);
        String key = request.getKey();
        String resourcePath = OSSUtils.determineResourcePath(bucketName, key, config.isSLDEnabled());

        RequestMessage requestMessage = new RequestMessage(bucketName, key);
        requestMessage.setEndpoint(OSSUtils.determineFinalEndpoint(endpoint, bucketName, config));
        requestMessage.setMethod(method);
        requestMessage.setResourcePath(resourcePath);
        requestMessage.setHeaders(request.getHeaders());

        requestMessage.addHeader(HttpHeaders.DATE, expires);
        if (request.getContentType() != null && !request.getContentType().trim().equals("")) {
            requestMessage.addHeader(HttpHeaders.CONTENT_TYPE, request.getContentType());
        }
        if (request.getContentMD5() != null && !request.getContentMD5().trim().equals("")) {
            requestMessage.addHeader(HttpHeaders.CONTENT_MD5, request.getContentMD5());
        }
        for (Map.Entry<String, String> h : request.getUserMetadata().entrySet()) {
            requestMessage.addHeader(OSSHeaders.OSS_USER_METADATA_PREFIX + h.getKey(), h.getValue());
        }

        Map<String, String> responseHeaderParams = new HashMap<String, String>();
        populateResponseHeaderParameters(responseHeaderParams, request.getResponseHeaders());
        populateTrafficLimitParams(responseHeaderParams, request.getTrafficLimit());
        if (responseHeaderParams.size() > 0) {
            requestMessage.setParameters(responseHeaderParams);
        }

        if (request.getQueryParameter() != null && request.getQueryParameter().size() > 0) {
            for (Map.Entry<String, String> entry : request.getQueryParameter().entrySet()) {
                requestMessage.addParameter(entry.getKey(), entry.getValue());
            }
        }

        if (request.getProcess() != null && !request.getProcess().trim().equals("")) {
            requestMessage.addParameter(RequestParameters.SUBRESOURCE_PROCESS, request.getProcess());
        }

        if (useSecurityToken) {
            requestMessage.addParameter(SECURITY_TOKEN, currentCreds.getSecurityToken());
        }

        String canonicalResource = "/" + ((bucketName != null) ? bucketName : "") + ((key != null ? "/" + key : ""));
        String canonicalString = buildCanonicalString(method.toString(), canonicalResource, requestMessage,
                expires);
        String signature = ServiceSignature.create().computeSignature(accessKey, canonicalString);

        Map<String, String> params = new LinkedHashMap<String, String>();
        params.put(HttpHeaders.EXPIRES, expires);
        params.put(OSS_ACCESS_KEY_ID, accessId);
        params.put(SIGNATURE, signature);
        params.putAll(requestMessage.getParameters());

        String queryString = HttpUtil.paramToQueryString(params, DEFAULT_CHARSET_NAME);

        /* Compse HTTP request uri. */
        String url = requestMessage.getEndpoint().toString();
        if (!url.endsWith("/")) {
            url += "/";
        }
        url += resourcePath + "?" + queryString;
        return url;
    }

    public static String buildCanonicalizedResource(String resourcePath, Map<String, String> parameters) {
        assertTrue(resourcePath.startsWith("/"), "Resource path should start with slash character");

        StringBuilder builder = new StringBuilder();
        builder.append(resourcePath);

        if (parameters != null) {
            String[] parameterNames = parameters.keySet().toArray(new String[parameters.size()]);
            Arrays.sort(parameterNames);

            char separator = '?';
            for (String paramName : parameterNames) {
                if (!SignParameters.SIGNED_PARAMTERS.contains(paramName)) {
                    continue;
                }

                builder.append(separator);
                builder.append(paramName);
                String paramValue = parameters.get(paramName);
                if (paramValue != null) {
                    builder.append("=").append(paramValue);
                }

                separator = '&';
            }
        }

        return builder.toString();
    }

    public static String buildSignature(String secretAccessKey, String httpMethod, String resourcePath, RequestMessage request) {
        String canonicalString = buildCanonicalString(httpMethod, resourcePath, request, null);
        return ServiceSignature.create().computeSignature(secretAccessKey, canonicalString);
    }

    private static void populateTrafficLimitParams(Map<String, String> params, int limit) {
        if (limit > 0) {
            params.put(OSS_TRAFFIC_LIMIT, String.valueOf(limit));
        }
    }
}
