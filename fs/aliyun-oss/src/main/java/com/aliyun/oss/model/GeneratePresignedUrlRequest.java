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

package com.aliyun.oss.model;

import java.util.*;

import com.aliyun.oss.HttpMethod;

/**
 * This class wraps all the information needed to generate a presigned URl. And
 * it's not the real "request" class.
 */
public class GeneratePresignedUrlRequest {

    /**
     * The HTTP method (GET, PUT, DELETE, HEAD) to be used in this request and
     * when the pre-signed URL is used
     */
    private HttpMethod method;

    /** The name of the bucket involved in this request */
    private String bucketName;

    /** The key of the object involved in this request */
    private String key;

    /** Content-Type to url sign */
    private String contentType;

    /** Content-MD5 */
    private String contentMD5;

    /** process */
    private String process;

    /**
     * An optional expiration date at which point the generated pre-signed URL
     * will no longer be accepted by OSS. If not specified, a default value will
     * be supplied.
     */
    private Date expiration;

    // The response headers to override.
    private ResponseHeaderOverrides responseHeaders = new ResponseHeaderOverrides();

    // User's customized metadata, which are the http headers start with the
    // x-oos-meta-.
    private Map<String, String> userMetadata = new HashMap<String, String>();

    private Map<String, String> queryParam = new HashMap<String, String>();

    private Map<String, String> headers = new HashMap<String, String>();

    private Set<String> additionalHeaderNames = new HashSet<String>();

    // Traffic limit speed, its uint is bit/s 
    private int trafficLimit;

    /**
     * Constructor with GET as the httpMethod
     *
     * @param bucketName
     *            Bucket name.
     * @param key
     *            Object key.
     */
    public GeneratePresignedUrlRequest(String bucketName, String key) {
        this(bucketName, key, HttpMethod.GET);
    }

    /**
     * Constructor.
     *
     * @param bucketName
     *            Bucket name.
     * @param key
     *            Object key.
     * @param method
     *            {@link HttpMethod#GET}。
     */
    public GeneratePresignedUrlRequest(String bucketName, String key, HttpMethod method) {
        this.bucketName = bucketName;
        this.key = key;
        this.method = method;
    }

    /**
     * Gets Http method.
     *
     * @return HTTP method.
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Sets Http method.
     *
     * @param method
     *            HTTP method.
     */
    public void setMethod(HttpMethod method) {
        if (method != HttpMethod.GET && method != HttpMethod.PUT)
            throw new IllegalArgumentException("Only GET or PUT is supported!");

        this.method = method;
    }

    /**
     * Gets {@link Bucket} name
     *
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the {@link Bucket} name.
     *
     * @param bucketName
     *            {@link Bucket} name.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Gets the {@link OSSObject} key.
     *
     * @return Object key.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets {@link OSSObject} key.
     *
     * @param key
     *            {@link OSSObject} key.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Gets the expiration time of the Url
     *
     * @return The expiration time of the Url.
     */
    public Date getExpiration() {
        return expiration;
    }

    /**
     * Sets the expiration time of the Url
     *
     * @param expiration
     *            The expiration time of the Url.
     */
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    /**
     * Sets the content-type header which indicates the file's type.
     *
     * @param contentType
     *            The file's content type.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Gets the content type header.
     *
     * @return Content-Type Header
     */
    public String getContentType() {
        return this.contentType;
    }

    /**
     * Sets the file's MD5 value.
     *
     * @param contentMD5
     *            The target file's MD5 value.
     */
    public void setContentMD5(String contentMD5) {
        this.contentMD5 = contentMD5;
    }

    /**
     * Gets the file's MD5 value.
     *
     * @return Content-MD5
     */
    public String getContentMD5() {
        return this.contentMD5;
    }

    /**
     * Sets the response headers to override.
     *
     * @param responseHeaders
     *            The response headers to override.
     */
    public void setResponseHeaders(ResponseHeaderOverrides responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * Gets the response headers to override.
     *
     * @return The response headers to override.
     */
    public ResponseHeaderOverrides getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * <p>
     * Gets user's customized metadata.
     * </p>
     * <p>
     * OSS uses x-oss-meta- as the prefix in the http headers to transfer the
     * user's customized metadata. However the key value returned by
     * getUserMetadata does not have the prefix---the prefix is added by SDK
     * automatically. The key is case insensitive and will always be in low case
     * when it's returned from OSS. For example, if the key is MyUserMeta，the
     * key returned by this method will be myusermeta.
     * </p>
     *
     * @return A {@link Map} instance that contains the user's customized
     *         metadata.
     */
    public Map<String, String> getUserMetadata() {
        return userMetadata;
    }

    /**
     * Gets user's customized metadata. They will be represented in x-oss-meta*
     * headers.
     *
     * @param userMetadata
     *            User's metadata
     */
    public void setUserMetadata(Map<String, String> userMetadata) {
        if (userMetadata == null) {
            throw new NullPointerException("The argument 'userMeta' is null.");
        }
        this.userMetadata = userMetadata;
    }

    /**
     * Add a user's customized metadata.
     *
     * @param key
     *            The metadata key. Note: this key should not have prefix of
     *            'x-oss-meta-'.
     * @param value
     *            the metadata's value.
     */
    public void addUserMetadata(String key, String value) {
        this.userMetadata.put(key, value);
    }

    /**
     * Gets the query parameters.
     *
     * @return Query parameters.
     */
    public Map<String, String> getQueryParameter() {
        return this.queryParam;
    }

    /**
     * Sets the query parameters.
     *
     * @param queryParam
     *            Query parameters.
     */
    public void setQueryParameter(Map<String, String> queryParam) {
        if (queryParam == null) {
            throw new NullPointerException("The argument 'queryParameter' is null.");
        }
        this.queryParam = queryParam;
    }

    /**
     * @param key query parameter key
     * @param value query parameter value
     */
    public void addQueryParameter(String key, String value) {
        this.queryParam.put(key, value);
    }

    /**
     * Gets the process header.
     *
     * @return The process header.
     */
    public String getProcess() {
        return process;
    }

    /**
     * Sets the process header.
     *
     * @param process
     *            The process header.
     */
    public void setProcess(String process) {
        this.process = process;
    }

    /**
     * Gets HTTP Headers
     *
     * @return HTTP Headers
     */
    public Map<String, String> getHeaders() {
        return this.headers;
    }

    /**
     * Sets Http headers.
     *
     * @param headers
     *            HTTP Headers。
     */
    public void setHeaders(Map<String, String> headers) {
        if (headers == null) {
            throw new NullPointerException("The argument 'queryParameter' is null.");
        }
        this.headers = headers;
    }

    /**
     * @param key header key
     * @param value header value
     */
    public void addHeader(String key, String value) {
        this.headers.put(key, value);
    }

    /**
     * Gets additional HTTP header names.
     *
     * @return Additional HTTP header names.
     */
    public Set<String> getAdditionalHeaderNames() {
        return additionalHeaderNames;
    }

    /**
     * Sets additional HTTP header names
     *
     * @param additionalHeaderNames
     *              additional http header names.
     */
    public void setAdditionalHeaderNames(Set<String> additionalHeaderNames) {
        this.additionalHeaderNames = additionalHeaderNames;
    }

    public void addAdditionalHeaderName(String name) {
        this.additionalHeaderNames.add(name);
    }

    /**
     * Sets traffic limit speed , its unit is bit/s
     *
     * @param trafficLimit
     *            traffic limit
     */
    public void setTrafficLimit(int trafficLimit) {
        this.trafficLimit = trafficLimit;
    }

    /**
     * Gets traffic limit speed , its unit is bit/s
     * @return traffic limit speed
     */
    public int getTrafficLimit() {
        return trafficLimit;
    }
}
