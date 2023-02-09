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

import java.util.ArrayList;
import java.util.List;

/**
 * This is the result of the listing objects request.
 */
public class ListObjectsV2Result extends GenericResult {

    /**
     * A list of summary information describing the objects stored in the bucket
     */
    private List<OSSObjectSummary> objectSummaries = new ArrayList<OSSObjectSummary>();

    /**
     * A list of the common prefixes included in this object listing - common
     * prefixes will only be populated for requests that specified a delimiter
     */
    private List<String> commonPrefixes = new ArrayList<String>();

    /** The name of the bucket */
    private String bucketName;

    /** KeyCount is the number of keys returned with this response */
    private int keyCount;

    /**
     * Optional parameter which allows list to be continued from a specific point.
     * ContinuationToken is provided in truncated list results.
     */
    private String continuationToken;

    /**
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more keys in the bucket that can be listed.
     */
    private String nextContinuationToken;

    /**
     * Optional parameter indicating where you want OSS to start the object listing
     * from.  This can be any key in the bucket.
     */
    private String startAfter;

    /**
     * Indicates if this is a complete listing, or if the caller needs to make
     * additional requests to OSS to see the full object listing.
     */
    private boolean isTruncated;

    /**
     * The prefix parameter originally specified by the caller when this object
     * listing was returned
     */
    private String prefix;

    /**
     * The maxKeys parameter originally specified by the caller when this object
     * listing was returned
     */
    private int maxKeys;

    /**
     * The delimiter parameter originally specified by the caller when this
     * object listing was returned
     */
    private String delimiter;

    /**
     * The encodingType parameter originally specified by the caller when this
     * object listing was returned.
     */
    private String encodingType;

    /**
     * Gets the list of object summaries describing the objects stored in the bucket.
     * @return The {@link OSSObjectSummary} instance.
     */
    public List<OSSObjectSummary> getObjectSummaries() {
        return objectSummaries;
    }

    /**
     * Add the object summary to the list of the object summaries
     *
     * @param objectSummary
     *            The {@link OSSObjectSummary} instance.
     */
    public void addObjectSummary(OSSObjectSummary objectSummary) {
        this.objectSummaries.add(objectSummary);
    }

    /**
     * Gets the common prefixes included in this object listing. Common
     * prefixes are only present if a delimiter was specified in the original
     * request.
     *
     * For example, consider a bucket that contains the following objects:
     * "fun/test.jpg", "fun/movie/001.avi", "fun/movie/007.avi".
     * if calling the prefix="fun/" and delimiter="/", the returned
     * {@link ListObjectsV2Result} object will contain the common prefix of "fun/movie/".
     *
     * @return The list of common prefixes included in this object listing,
     *         which might be an empty list if no common prefixes were found.
     */
    public List<String> getCommonPrefixes() {
        return commonPrefixes;
    }

    /**
     * adds a common prefix element to the common prefixes list
     *
     * @param commonPrefix
     *            prefix element to the common prefixes list.
     */
    public void addCommonPrefix(String commonPrefix) {
        this.commonPrefixes.add(commonPrefix);
    }

    /**
     * Gets the bucket name that containing the objects listing
     * @return the bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the bucket name that containing the objects listing
     *
     * @param bucketName
     *            bucket name that containing the objects listing.
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * The prefix parameter originally specified by the caller when this object
     * listing was returned
     *
     * @return  return prefix parameter.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Sets the prefix parameter
     *
     * @param prefix
     *            The prefix parameter originally used to request this object
     *            listing.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the the maximum number of keys to include in the response.
     *
     * @return The optional parameter indicating the maximum number of keys to
     *         include in the response.
     */
    public int getMaxKeys() {
        return maxKeys;
    }

    /**
     * Sets the the maximum number of keys to include in the response.
     *
     * @param maxKeys
     *            The optional parameter indicating the maximum number of keys
     *            to include in the response.
     */
    public void setMaxKeys(int maxKeys) {
        this.maxKeys = maxKeys;
    }

    /**
     * Gets the delimiter parameter that you have specified.
     *
     * @return The delimiter parameter originally used to request this object
     *         listing. Returns null if no delimiter was specified.
     */
    public String getDelimiter() {
        return delimiter;
    }

    /**
     * Sets the delimiter parameter.
     *
     * @param delimiter
     *         The delimiter parameter originally used to request this object
     *         listing. Returns null if no delimiter was specified.
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    /**
     * Gets the encoding type used by OSS to encode object key names in
     * the XML response. If you specify encodingType request
     * parameter, OSS includes this element in the response, and returns
     * encoded key name values in the following response elements:
     * Delimiter, StartAfter, Prefix, NextConfigurationToken, Key.
     *
     * @return null if encodingType is not specified in the request parameter.
     */
    public String getEncodingType() {
        return encodingType;
    }

    /**
     * Sets the encode type that responded.
     *
     * @param encodingType
     *            the encode type that the response contains.
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     * Gets whether or not this object listing is complete.
     *
     * @return the value true if the listing is truncated
     *          and return false if the listing is complete
     */
    public boolean isTruncated() {
        return isTruncated;
    }

    /**
     * Sets whether or not this object listing is complete.
     *
     * @param isTruncated
     *          the value true if the listing is truncated
     *          and return false if the listing is complete.
     */
    public void setTruncated(boolean isTruncated) {
        this.isTruncated = isTruncated;
    }

    /**
     * Gets the number of keys returned with this response.
     *
     * @return number of keys returned with this response.
     */
    public int getKeyCount() {
        return keyCount;
    }

    /**
     * Sets the number of keys returned with this response.
     *
     * @param keyCount
     *           The number of keys that were returned with this response.
     */
    public void setKeyCount(int keyCount) {
        this.keyCount = keyCount;
    }

    /**
     * Gets the continuation token.
     *
     * @return The continuation token that you have specified in the request.
     */
    public String getContinuationToken() {
        return continuationToken;
    }

    /**
     * Sets the continuation token.
     *
     * @param continuationToken
     *              The parameter should be set with the value of
     *              {@link ListObjectsV2Result#getNextContinuationToken()}
     */
    public void setContinuationToken(String continuationToken) {
        this.continuationToken = continuationToken;
    }

    /**
     * Gets the nextContinuationToken.
     * NextContinuationToken is sent when isTruncated is true meaning there are
     * more keys in the bucket that can be listed.
     *
     * @return The optional nextContinuationToken that can be used for the next request.
     */
    public String getNextContinuationToken() {
        return nextContinuationToken;
    }

    /**
     * Sets the nextContinuationToken.
     * nextContinuationToken is sent when isTruncated is true meaning there are
     * more keys in the bucket that can be listed.
     *
     * @param nextContinuationToken
     *              The optional NextContinuationToken returned and can be used for the next request.
     */
    public void setNextContinuationToken(String nextContinuationToken) {
        this.nextContinuationToken = nextContinuationToken;
    }

    /**
     * Returns optional parameter indicating where you want OSS to start the object
     * listing from.  This can be any key in the bucket.
     *
     * @return the optional startAfter parameter
     */
    public String getStartAfter() {
        return startAfter;
    }

    /**
     * Sets the optional parameter indicating where you want OSS to start the object
     * listing from.  This can be any key in the bucket.
     *
     * @param startAfter
     *            The optional startAfter parameter.
     */
    public void setStartAfter(String startAfter) {
        this.startAfter = startAfter;
    }

}
