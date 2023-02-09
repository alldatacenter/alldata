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


package com.qcloud.cos.model;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.internal.CosServiceRequest;

public class GeneratePresignedUrlRequest extends CosServiceRequest {
    /** The HTTP method (GET, PUT, DELETE, HEAD) to be used in this request and when the pre-signed URL is used */
    private HttpMethodName method;

    /** The name of the bucket involved in this request */
    private String bucketName;

    /** The key of the object involved in this request */
    private String key;

    /**
     * The version ID of the object, only present if versioning has been
     * enabled for the bucket.
     */
    private String versionId;

    /** The optional Content-Type header that will be sent when the presigned URL is accessed */
    private String contentType;

    /** The optional Content-MD5 header that will be sent when the presigned URL is accessed */
    private String contentMd5;

    /**
     * An optional expiration date at which point the generated pre-signed URL
     * will no longer be accepted by COS. If not specified, a default
     * value will be supplied.
     */
    private Date expiration;

    /** the optional signPrefixMode decide the presigned url whether start with 'sign=' and encode value  */
    private boolean signPrefixMode = true;

    /**
     * An optional map of additional parameters to include in the pre-signed
     * URL. Adding additional request parameters enables more advanced
     * pre-signed URLs, such as accessing COS's torrent resource for an
     * object, or for specifying a version ID when accessing an object.
     */
    private Map<String, String> requestParameters = new HashMap<String, String>();

    /**
     * Optional field that overrides headers on the response.
     */
    private ResponseHeaderOverrides responseHeaders;
    
    /**
     * Creates a new request for generating a pre-signed URL that can be used as
     * part of an HTTP GET request to access the COS object stored under
     * the specified key in the specified bucket.
     *
     * @param bucketName
     *            The name of the bucket containing the desired COS
     *            object.
     * @param key
     *            The key under which the desired COS object is stored.
     */
    public GeneratePresignedUrlRequest(String bucketName, String key) {
        this(bucketName, key, HttpMethodName.GET);
    }

    /**
     * <p>
     * Creates a new request for generating a pre-signed URL that can be used as
     * part of an HTTP request to access the specified COS resource.
     * </p>
     * <p>
     * When specifying an HTTP method, you <b>must</b> send the pre-signed URL
     * with the same HTTP method in order to successfully use the pre-signed
     * URL.
     * </p>
     *
     * @param bucketName
     *            The name of the COS bucket involved in the operation.
     * @param key
     *            The key of the COS object involved in the operation.
     * @param method
     *            The HTTP method (GET, PUT, DELETE, HEAD) to be used in the
     *            request when the pre-signed URL is used.
     */
    public GeneratePresignedUrlRequest(String bucketName, String key, HttpMethodName method) {
        this.bucketName = bucketName;
        this.key = key;
        this.method = method;
    }

    /**
     * The HTTP method (GET, PUT, DELETE, HEAD) to be used in this request. The
     * same HTTP method <b>must</b> be used in the request when the pre-signed
     * URL is used.
     *
     * @return The HTTP method (GET, PUT, DELETE, HEAD) to be used in this
     *         request and when the pre-signed URL is used.
     */
    public HttpMethodName getMethod() {
        return method;
    }

    /**
     * Sets the HTTP method (GET, PUT, DELETE, HEAD) to be used in this request.
     * The same HTTP method <b>must</b> be used in the request when the
     * pre-signed URL is used.
     *
     * @param method
     *            The HTTP method (GET, PUT, DELETE, HEAD) to be used in this
     *            request.
     */
    public void setMethod(HttpMethodName method) {
        this.method = method;
    }

    /**
     * Sets the HTTP method (GET, PUT, DELETE, HEAD) to be used in this request,
     * and returns this request object to enable additional method calls to be
     * chained together.
     * <p>
     * The same HTTP method <b>must</b> be used in the request when the
     * pre-signed URL is used.
     *
     * @param method
     *            The HTTP method (GET, PUT, DELETE, HEAD) to be used in this
     *            request.
     *
     * @return The updated request object, so that additional method calls can
     *         be chained together.
     */
    public GeneratePresignedUrlRequest withMethod(HttpMethodName method) {
        setMethod(method);
        return this;
    }

    /**
     * Returns the name of the bucket involved in this request.
     *
     * @return the name of the bucket involved in this request.
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket involved in this request.
     *
     * @param bucketName
     *            the name of the bucket involved in this request.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket involved in this request, and returns this
     * request object to enable additional method calls to be chained together.
     *
     * @param bucketName
     *            the name of the bucket involved in this request.
     *
     * @return The updated request object, so that additional method calls can
     *         be chained together.
     */
    public GeneratePresignedUrlRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Returns the key of the object involved in this request.
     *
     * @return The key of the object involved in this request.
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key of the object involved in this request.
     *
     * @param key
     *            the key of the object involved in this request.
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the key of the object involved in this request, and returns this
     * request object to enable additional method calls to be chained together.
     *
     * @param key
     *            the key of the object involved in this request.
     *
     * @return The updated request object, so that additional method calls can
     *         be chained together.
     */
    public GeneratePresignedUrlRequest withKey(String key) {
        setKey(key);
        return this;
    }

    /**
     * Returns the version ID of the object, only present if versioning has
     * been enabled for the bucket.
     *
     * @return The version ID of the object, only present if versioning has
     *         been enabled for the bucket.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets the version ID of the object, only present if versioning has
     * been enabled for the bucket.
     *
     * @param versionId
     *            The version ID of the object, only present if versioning
     *            has been enabled for the bucket.
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Sets the version ID of the object, only present if versioning has
     * been enabled for the bucket. Returns the {@link GeneratePresignedUrlRequest}
     * object for method chanining.
     *
     * @param versionId
     *            The version ID of the object, only present if versioning
     *            has been enabled for the bucket.
     *
     * @return This object for method chaining.
     */
    public GeneratePresignedUrlRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }

    /**
     * The expiration date at which point the new pre-signed URL will no longer
     * be accepted by COS. If not specified, a default value will be
     * supplied.
     *
     * @return The expiration date at which point the new pre-signed URL will no
     *         longer be accepted by COS.
     */
    public Date getExpiration() {
        return expiration;
    }

    /**
     * Sets the expiration date at which point the new pre-signed URL will no
     * longer be accepted by COS. If not specified, a default value will
     * be supplied.
     *
     * @param expiration
     *            The expiration date at which point the new pre-signed URL will
     *            no longer be accepted by COS.
     */
    public void setExpiration(Date expiration) {
        this.expiration = expiration;
    }

    /**
     * Sets the expiration date at which point the new pre-signed URL will no
     * longer be accepted by COS, and returns this request object to
     * enable additional method calls to be chained together.
     * <p>
     * If not specified, a default value will be supplied.
     *
     * @param expiration
     *            The expiration date at which point the new pre-signed URL will
     *            no longer be accepted by COS.
     *
     * @return The updated request object, so that additional method calls can
     *         be chained together.
     */
    public GeneratePresignedUrlRequest withExpiration(Date expiration) {
        setExpiration(expiration);
        return this;
    }

    /**
     * Adds an additional request parameter to be included in the pre-signed
     * URL. 
     *
     * @param key
     *            The name of the request parameter, as it appears in the URL's
     *            query string (e.g. versionId).
     * @param value
     *            The (optional) value of the request parameter being added.
     */
    public void addRequestParameter(String key, String value) {
        requestParameters.put(key, value);
    }

    /**
     * Returns the complete map of additional request parameters to be included
     * in the pre-signed URL.
     *
     * @return The complete map of additional request parameters to be included
     *         in the pre-signed URL.
     */
    public Map<String, String> getRequestParameters() {
        return requestParameters;
    }

    /**
     * Returns the headers to be overridden in the service response.
     *
     * @return the headers to be overridden in the service response.
     */
    public ResponseHeaderOverrides getResponseHeaders() {
        return responseHeaders;
    }

    /**
     * Sets the headers to be overridden in the service response.
     *
     * @param responseHeaders
     *            The headers to be overridden in the service response.
     */
    public void setResponseHeaders(ResponseHeaderOverrides responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * Sets the headers to be overridden in the service response and returns
     * this object, for method chaining.
     *
     * @param responseHeaders
     *            The headers to be overridden in the service response.
     *
     *
     * @return This {@link GeneratePresignedUrlRequest} for method chaining.
     */
    public GeneratePresignedUrlRequest withResponseHeaders(ResponseHeaderOverrides responseHeaders) {
        setResponseHeaders(responseHeaders);
        return this;
    }

    /**
     * Gets the expected content-type of the request. The content-type is included in
     * the signature.
     *
     * @return The expected content-type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Sets the expected content-type of the request. The content-type is included in
     * the signature.
     * @param contentType
     *            The expected content-type
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    /**
     * Sets the expected content-type of the request and returns
     * this object, for method chaining.
     *
     * @param contentType
     *            The expected content-type
     *
     *
     * @return This {@link GeneratePresignedUrlRequest} for method chaining.
     */
    public GeneratePresignedUrlRequest withContentType(String contentType) {
        setContentType(contentType);
        return this;
    }

    /**
     * Gets the expected content-md5 header of the request. This header value
     * will be included when calculating the signature, and future requests must
     * include the same content-md5 header value to access the presigned URL.
     *
     * @return The expected content-md5 header value.
     */
    public String getContentMd5() {
        return contentMd5;
    }

    /**
     * Sets the expected content-md5 header of the request. This header value
     * will be included when calculating the signature, and future requests must
     * include the same content-md5 header value to access the presigned URL.

     * @param contentMd5
     *            The expected content-md5 header value.
     */
    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    /**
     * Sets the expected content-md5 header of the request and returns this
     * object, for method chaining.
     *
     * @param contentMd5
     *            The expected content-md5 header value.
     *
     * @return This {@link GeneratePresignedUrlRequest} for method chaining.
     */
    public GeneratePresignedUrlRequest withContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
        return this;
    }
    
    /**
     * Rejects any illegal input (as attributes of this request) by the user.
     *
     * @throws IllegalArgumentException if there is illegal input from the user.
     */
    public void rejectIllegalArguments() {
        if (bucketName == null) {
            throw new IllegalArgumentException(
                    "The bucket name parameter must be specified when generating a pre-signed URL");
        }
        if (this.method == null) {
            throw new IllegalArgumentException(
                    "The HTTP method request parameter must be specified when generating a pre-signed URL");
        }
    }

    public boolean isSignPrefixMode() {
        return signPrefixMode;
    }

    public void setSignPrefixMode(boolean signPrefixMode) {
        this.signPrefixMode = signPrefixMode;
    }
}
