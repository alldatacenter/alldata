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

/**
 * This is the request class to list objects under a bucket.
 */
public class ListObjectsV2Request extends GenericRequest {

    /**
     * Optional parameter that causes keys that contain the same string between
     * the prefix and the first occurrence of the delimiter to be rolled up into
     * a single result element in the {@link ListObjectsV2Result#getCommonPrefixes()} list.
     * The most commonly used delimiter is "/", which simulates a hierarchical organization similar to
     * a file system directory structure.
     */
    private String delimiter;

    /**
     * Optional parameter indicating the encoding method to be applied on the
     * response. An object key can contain any Unicode character; however, XML
     * 1.0 parser cannot parse some characters, such as characters with an ASCII
     * value from 0 to 10. you can add this parameter to request that OSS encode the keys in the
     * response.
     */
    private String encodingType;

    /**
     * Optional parameter indicating the maximum number of keys to include in
     * the response.
     */
    private Integer maxKeys;

    /**
     * Optional parameter restricting the response to keys which begin with the
     * specified prefix.
     */
    private String prefix;

    /**
     * Optional parameter which allows list to be continued from a specific point.
     * ContinuationToken is provided in truncated list results.
     */
    private String continuationToken;

    /**
     * The owner field is not present in ListObjectsV2 results by default. If this flag is
     * set to true the owner field in {@link OSSObjectSummary} will be included.
     */
    private boolean fetchOwner;

    /**
     * Optional parameter indicating where you want OSS to start the object listing
     * from.
     */
    private String startAfter;

    public ListObjectsV2Request() {
    }

    /**
     * Constructs a new {@link ListObjectsRequest} object and initializes the bucketName parameter.
     *
     * @param bucketName
     *              The bucket name.
     */
    public ListObjectsV2Request(String bucketName) {
        this.setBucketName(bucketName);
    }

    /**
     * Constructs a new {@link ListObjectsRequest} object and
     * initializes the bucketName and prefix parameters.
     *
     * @param bucketName
     *              The bucket name.
     * @param prefix
     *              The prefix restricting the objects listing.
     */
    public ListObjectsV2Request(String bucketName, String prefix) {
        this.setBucketName(bucketName);
        this.prefix = prefix;
    }

    /**
     * Constructs a new {@link ListObjectsRequest} object and initializes all optional parameters.
     *
     * @param bucketName
     *              The bucket name.
     * @param prefix
     *              The prefix restricting the objects listing.
     * @param continuationToken
     *             The continuation token allows list to be continued from a specific point.
     *             It values the last result {@link ListObjectsV2Result#getNextContinuationToken()}.
     * @param startAfter
     *              Where you want oss to start the object listing from.
     * @param delimiter
     *              The delimiter for condensing common prefixes in the returned listing results.
     * @param maxKeys
     *              The maximum number of results to return.
     * @param encodingType
     *              the encoding method to be applied on the response.
     * @param fetchOwner
     *              Whether to get the owner filed in the response or not.
     */
    public ListObjectsV2Request(String bucketName, String prefix, String continuationToken,
                                String startAfter, String delimiter, Integer maxKeys,
                                String encodingType, boolean fetchOwner) {
        this.setBucketName(bucketName);
        this.prefix = prefix;
        this.continuationToken = continuationToken;
        this.startAfter = startAfter;
        this.delimiter = delimiter;
        this.maxKeys = maxKeys;
        this.encodingType = encodingType;
        this.fetchOwner = fetchOwner;
    }

    /**
     * @param bucketName
     *              The name of the bucket that you want to listing.
     *
     * @return This {@link ListObjectsRequest}, enabling additional method
     *         calls to be chained together.
     */
    public ListObjectsV2Request withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Gets the optional delimiter parameter that causes keys that contain
     * the same string between the prefix and the first occurrence of the
     * delimiter to be combined into a single result element in the
     * {@link ListObjectsV2Result#getCommonPrefixes()} list. These combined keys
     * are not returned elsewhere in the response. The most commonly used
     * delimiter is "/", which simulates a hierarchical organization similar to
     * a file system directory structure.
     *
     * @return The optional delimiter parameter that causes keys that contain
     *         the same string between the prefix and the first occurrence of
     *         the delimiter to be combined into a single result element in the
     *         {@link ListObjectsV2Result#getCommonPrefixes()} list.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Sets the optional delimiter parameter that causes keys that contain the
     * same string between the prefix and the first occurrence of the delimiter
     * to be combined into a single result element in the
     * {@link ListObjectsV2Result#getCommonPrefixes()} list.
     *
     * @param delimiter
     *            The optional delimiter parameter that causes keys that contain
     *            the same string between the prefix and the first occurrence of
     *            the delimiter to be combined into a single result element in
     *            the {@link ListObjectsV2Result#getCommonPrefixes()} list.
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Sets the optional delimiter parameter that causes keys that contain the
     * same string between the prefix and the first occurrence of the delimiter
     * to be rolled up into a single result element in the
     * {@link ListObjectsV2Result#getCommonPrefixes()} list.
     * Returns this {@link ListObjectsRequest}, enabling additional method
     * calls to be chained together.
     *
     * @param delimiter
     *            The optional delimiter parameter that causes keys that contain
     *            the same string between the prefix and the first occurrence of
     *            the delimiter to be rolled up into a single result element in
     *            the {@link ListObjectsV2Result#getCommonPrefixes()} list.
     *
     * @return This {@link ListObjectsRequest}, enabling additional method
     *         calls to be chained together.
     */
    public ListObjectsV2Request withDelimiter(String delimiter) {
        setDelimiter(delimiter);
        return this;
    }

    /**
     * Gets the optional encodingType parameter indicating the
     * encoding method to be applied on the response.
     *
     * @return The encoding method to be applied on the response.
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Sets the optional parameter indicating the
     * encoding method to be applied on the response. An object key can contain
     * any Unicode character; however, XML 1.0 parser cannot parse some
     * characters, such as characters with an ASCII value from 0 to 10. For
     * characters that are not supported in XML 1.0, you can add this parameter
     * to request that OSS encode the keys in the response.
     *
     * @param encodingType
     *            The encoding method to be applied on the response. Valid
     *            values: null (not encoded) or "url".
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     * Sets the optional parameter indicating the
     * encoding method to be applied on the response. An object key can contain
     * any Unicode character; however, XML 1.0 parser cannot parse some
     * characters, such as characters with an ASCII value from 0 to 10. For
     * characters that are not supported in XML 1.0, you can add this parameter
     * to request that OSS encode the keys in the response.
     * Returns this {@link ListObjectsV2Request}, enabling additional method calls
     * to be chained together.
     *
     * @param encodingType
     *            The encoding method to be applied on the response. Valid
     *            values: null (not encoded) or "url".
     *
     * @return  The {@link ListObjectsV2Request} instance.
     */
    public ListObjectsV2Request withEncodingType(String encodingType) {
        setEncodingType(encodingType);
        return this;
    }


    /**
     * Gets the optional parameter indicating the maximum number of keys to
     * include in the response. OSS might return fewer keys than specified, but will
     * never return more. Even if the optional parameter is not specified,
     * OSS will limit the number of results in the response.
     *
     * @return The optional parameter indicating the maximum number of keys to
     *         include in the response.
     */
    public Integer getMaxKeys() {
        return maxKeys;
    }

    /**
     * Sets the optional parameter indicating the maximum number of keys to
     * include in the response.
     *
     * @param maxKeys
     *            The optional parameter indicating the maximum number of keys
     *            to include in the response.
     */
    public void setMaxKeys(Integer maxKeys) {
        this.maxKeys = maxKeys;
    }

    /**
     * Sets the optional parameter indicating the maximum number of keys to
     * include in the response.
     * Returns this {@link ListObjectsV2Request}, enabling additional method
     * calls to be chained together.
     *
     * @param maxKeys
     *            The optional parameter indicating the maximum number of keys
     *            to include in the response.
     *
     * @return This {@link ListObjectsV2Request}, enabling additional method
     *         calls to be chained together.
     *
     * @see ListObjectsV2Request#getMaxKeys()
     * @see ListObjectsV2Request#setMaxKeys(Integer)
     */
    public ListObjectsV2Request withMaxKeys(Integer maxKeys) {
        setMaxKeys(maxKeys);
        return this;
    }

    /**
     * Gets the optional prefix parameter and restricts the response to keys
     * that begin with the specified prefix. Use prefixes to separate a
     * bucket into different sets of keys, similar to how a file system organizes files
     * into directories.
     *
     * @return The optional prefix parameter restricting the response to keys
     *         that begin with the specified prefix.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the optional prefix parameter, restricting the response to keys that
     * begin with the specified prefix.
     *
     * @param prefix
     *            The optional prefix parameter, restricting the response to keys
     *            that begin with the specified prefix.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Sets the optional prefix parameter restricting the response to keys that
     * begin with the specified prefix.
     * Returns this {@link ListObjectsV2Request}, enabling additional method
     * calls to be chained together.
     *
     * @param prefix
     *            The optional prefix parameter restricting the response to keys
     *            that begin with the specified prefix.
     *
     * @return This {@link ListObjectsV2Request}, enabling additional method
     *         calls to be chained together.
     */
    public ListObjectsV2Request withPrefix(String prefix) {
        setPrefix(prefix);
        return this;
    }

    /**
     * Gets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. ContinuationToken is provided in truncated list results.
     *
     * @return The optional continuation token associated with this request.
     */
    public String getContinuationToken() { return continuationToken; }

    /**
     * Sets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. ContinuationToken is provided in truncated list results.
     *
     * @param continuationToken
     *           The optional continuation token to associate with this request.
     */
    public void setContinuationToken(String continuationToken) { this.continuationToken = continuationToken; }

    /**
     * Sets the optional continuation token.  Continuation token allows a list to be
     * continued from a specific point. ContinuationToken is provided in truncated list results.
     *
     * @param continuationToken
     *            The optional continuation token to associate with this request.
     *
     * @return This {@link ListObjectsV2Request}, enabling additional method
     *         calls to be chained together.
     */
    public ListObjectsV2Request withContinuationToken(String continuationToken) {
        setContinuationToken(continuationToken);
        return this;
    }

    /**
     * Returns if fetch owner is set.  The owner field is not present in ListObjectsV2
     * results by default.  If this flag is set to true the owner field will be included.

     * @return whether fetchOwner is set
     */
    public boolean isFetchOwner() { return fetchOwner; }

    /**
     * Sets the optional fetch owner flag.  The owner field is not present in ListObjectsV2
     * results by default.  If this flag is set to true the owner field will be included.

     * @param fetchOwner
     *           Set to true if the owner field should be included in results
     */
    public void setFetchOwner(boolean fetchOwner) { this.fetchOwner = fetchOwner; }

    /**
     * Sets the optional fetch owner flag.  The owner field is not present in ListObjectsV2
     * results by default.  If this flag is set to true the owner field will be included in
     * {@link OSSObjectSummary}.
     *
     * @param fetchOwner
     *           Set to true if the owner field should be included in results
     *
     * @return This {@link ListObjectsV2Request}, enabling additional method
     *         calls to be chained together.
     */
    public ListObjectsV2Request withFetchOwner(boolean fetchOwner) {
        setFetchOwner(fetchOwner);
        return this;
    }

    /**
     * Returns optional parameter indicating where you want OSS to start the object
     * listing from.  This can be any key in the bucket.
     *
     * @return the optional startAfter parameter
     */
    public String getStartAfter() { return startAfter; }

    /**
     * Sets the optional parameter indicating where you want OSS to start the object
     * listing from.  This can be any key in the bucket.
     *
     * @param startAfter
     *                The optional startAfter parameter.  This can be any key in the bucket.
     */
    public void setStartAfter(String startAfter) { this.startAfter = startAfter; }

    /**
     * Sets the optional parameter indicating where you want OSS to start the object
     * listing from.  This can be any key in the bucket.
     *
     * @param startAfter
     *            The optional startAfter parameter.
     *
     * @return This {@link ListObjectsV2Request}, enabling additional method
     *         calls to be chained together.
     */
    public ListObjectsV2Request withStartAfter(String startAfter) {
        setStartAfter(startAfter);
        return this;
    }

}
