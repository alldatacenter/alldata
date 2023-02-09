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

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * This is the request class that is used to download an object from OSS. It
 * wraps all the information needed to download an object.
 */
public class GetObjectRequest extends GenericRequest {

    private List<String> matchingETagConstraints = new ArrayList<String>();
    private List<String> nonmatchingEtagConstraints = new ArrayList<String>();
    private Date unmodifiedSinceConstraint;
    private Date modifiedSinceConstraint;
    private String process;

    private long[] range;

    private ResponseHeaderOverrides responseHeaders;

    /**
     * Fields releated with getobject operation by using url signature.
     */
    private URL absoluteUrl;
    private boolean useUrlSignature = false;


    // Traffic limit speed, its uint is bit/s
    private int trafficLimit;

    /**
     * Constructs a new {@link GetObjectRequest} with all the required parameters.
     *
     * @param bucketName
     *            The name of the bucket containing the desired object.
     * @param key
     *            The key in the specified bucket under which the object is
     *            stored.
     */
    public GetObjectRequest(String bucketName, String key) {
        super(bucketName, key);
    }
    
    /**
     * Constructs a new {@link GetObjectRequest} with all the required parameters.
     *
     * @param bucketName
     *            The name of the bucket containing the desired object.
     * @param key
     *            The key in the specified bucket under which the object is
     *            stored.
     * @param versionId
     *            The OSS version ID specifying a specific version of the
     *            object to download.
     */
    public GetObjectRequest(String bucketName, String key, String versionId) {
        super(bucketName, key);
        setVersionId(versionId);
    }

    /**
     * Constructor with presigned Url and user's custom headers.
     * 
     * @param absoluteUrl
     *            Signed url.
     * @param requestHeaders
     *            Request headers.
     */
    public GetObjectRequest(URL absoluteUrl, Map<String, String> requestHeaders) {
        this.absoluteUrl = absoluteUrl;
        this.useUrlSignature = true;
        this.getHeaders().clear();
        if (requestHeaders != null && !requestHeaders.isEmpty()) {
            this.getHeaders().putAll(requestHeaders);
        }
    }

    /**
     * Gets the range of the object to download. The range is in the form of
     * {start, end}---start and end is position of the object's content.
     * 
     * @return The range of the object to download.
     */
    public long[] getRange() {
        return range;
    }

    /**
     * Sets the range of the object to download (optional).
     * 
     * @param start
     *            <p>
     *            Start position
     *            </p>
     *            <p>
     *            When the start is non-negative, it means the starting position
     *            to download. When the start is -1, it means the range is
     *            determined by the end only and the end could not be -1. For
     *            example, when start is -1 and end is 100. It means the
     *            download range will be the last 100 bytes.
     *            </p>
     * @param end
     *            <p>
     *            End position
     *            </p>
     *            <p>
     *            When the end is non-negative, it means the ending position to
     *            download. When the end is -1, it means the range is determined
     *            by the start only and the start could not be -1. For example,
     *            when end is -1 and start is 100. It means the download range
     *            will be all exception first 100 bytes.
     *            </p>
     */
    public void setRange(long start, long end) {
        range = new long[] { start, end };
    }

    /**
     * Sets the range of the object to download and return current
     * GetObjectRequest instance (this).
     *
     * @param start
     *            <p>
     *            Start position
     *            </p>
     *            <p>
     *            When the start is non-negative, it means the starting position
     *            to download. When the start is -1, it means the range is
     *            determined by the end only and the end could not be -1. For
     *            example, when start is -1 and end is 100. It means the
     *            download range will be the last 100 bytes.
     *            </p>
     * @param end
     *            <p>
     *            End position
     *            </p>
     *            <p>
     *            When the end is non-negative, it means the ending position to
     *            download. When the end is -1, it means the range is determined
     *            by the start only and the start could not be -1. For example,
     *            when end is -1 and start is 100. It means the download range
     *            will be all exception first 100 bytes.
     *            </p>
     *
     * @return  The {@link GetObjectRequest} instance.
     */
    public GetObjectRequest withRange(long start, long end) {
        setRange(start, end);
        return this;
    }

    /**
     * Gets the matching Etag constraints. If the first ETag returned matches
     * the object's ETag, the file would be downloaded. Currently OSS only
     * supports one ETag. Otherwise, return precondition failure (412).
     * 
     * @return The list of expected ETags.
     */
    public List<String> getMatchingETagConstraints() {
        return matchingETagConstraints;
    }

    /**
     * Sets the matching Etag constraints. If the first ETag returned matches
     * the object's ETag, the file would be downloaded. Otherwise, return
     * precondition failure (412).
     * 
     * @param eTagList
     *            The expected ETag list. OSS only supports one ETag.
     */
    public void setMatchingETagConstraints(List<String> eTagList) {
        this.matchingETagConstraints.clear();
        if (eTagList != null && !eTagList.isEmpty()) {
            this.matchingETagConstraints.addAll(eTagList);
        }
    }

    public void clearMatchingETagConstraints() {
        this.matchingETagConstraints.clear();
    }

    /**
     * Gets the non-matching Etag constraints. If the first ETag returned does
     * not match the object's ETag, the file would be downloaded. Currently OSS
     * only supports one ETag. Otherwise, return precondition failure (412).
     * 
     * @return The list of expected ETags (only the first one matters though).
     */
    public List<String> getNonmatchingETagConstraints() {
        return nonmatchingEtagConstraints;
    }

    /**
     * Sets the non-matching Etag constraints. If the first ETag returned does
     * not match the object's ETag, the file would be downloaded. Currently OSS
     * only supports one ETag. Otherwise, returns precondition failure (412).
     * 
     * @param eTagList
     *            The list of expected ETags (only the first one matters
     *            though).
     */
    public void setNonmatchingETagConstraints(List<String> eTagList) {
        this.nonmatchingEtagConstraints.clear();
        if (eTagList != null && !eTagList.isEmpty()) {
            this.nonmatchingEtagConstraints.addAll(eTagList);
        }
    }

    public void clearNonmatchingETagConstraints() {
        this.nonmatchingEtagConstraints.clear();
    }

    /**
     * Gets the unmodified since constraints. If the timestamp returned is equal
     * or later than the actual file's modified time, the file would be
     * downloaded. Otherwise, the download API returns precondition failure
     * (412).
     * 
     * @return The timestamp.
     */
    public Date getUnmodifiedSinceConstraint() {
        return unmodifiedSinceConstraint;
    }

    /**
     * Sets the unmodified since constraints. If the timestamp specified in date
     * parameter is equal or later than the actual file's modified time, the
     * file would be downloaded. Otherwise, the download API returns
     * precondition failure (412).
     * 
     * @param date
     *            The timestamp.
     */
    public void setUnmodifiedSinceConstraint(Date date) {
        this.unmodifiedSinceConstraint = date;
    }

    /**
     * Gets the modified since constraints. If the timestamp returned is earlier
     * than the actual file's modified time, the file would be downloaded.
     * Otherwise, the download API returns precondition failure (412).
     * 
     * @return “If-Modified-Since” timestamp.
     */
    public Date getModifiedSinceConstraint() {
        return modifiedSinceConstraint;
    }

    /**
     * Sets the modified since constraints. If the timestamp returned is earlier
     * than the actual file's modified time, the file would be downloaded.
     * Otherwise, the download API returns precondition failure (412).
     * 
     * @param date
     *            “If-Modified-Since” parameter.
     */
    public void setModifiedSinceConstraint(Date date) {
        this.modifiedSinceConstraint = date;
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
     * Sets the response headers to override (optional).
     * 
     * @param responseHeaders
     *            The response headers to override.
     */
    public void setResponseHeaders(ResponseHeaderOverrides responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    public URL getAbsoluteUri() {
        return absoluteUrl;
    }

    public void setAbsoluteUri(URL absoluteUri) {
        this.absoluteUrl = absoluteUri;
    }

    public boolean isUseUrlSignature() {
        return useUrlSignature;
    }

    public void setUseUrlSignature(boolean useUrlSignature) {
        this.useUrlSignature = useUrlSignature;
    }

    public String getProcess() {
        return process;
    }

    public void setProcess(String process) {
        this.process = process;
    }

    /**
     * Sets traffic limit speed, its unit is bit/s
     *
     * @param trafficLimit
     *            traffic limit.
     */
    public void setTrafficLimit(int trafficLimit) {
        this.trafficLimit = trafficLimit;
    }

    /**
     * Gets traffic limit speed, its unit is bit/s
     * @return traffic limit speed
     */
    public int getTrafficLimit() {
        return trafficLimit;
    }

}
