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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.qcloud.cos.internal.CosServiceRequest;

public class GetObjectRequest extends CosServiceRequest
        implements SSECustomerKeyProvider, Serializable {

    /** The name of the bucket containing the object to retrieve */
    private String bucketName;

    /** The key under which the desired object is stored */
    private String key;

    /**
     * Optional version ID specifying which version of the object to download. If not specified, the
     * most recent version will be downloaded.
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link COS#setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     */
    private String versionId;

    /** Optional member indicating the byte range of data to retrieve */
    private long[] range;

    /**
     * traffic limit speed in second, the unit is bit/s
     */
    private int trafficLimit = 0;

    /**
     * Optional list of ETag values that constrain this request to only be executed if the object's
     * ETag matches one of the specified ETag values.
     */
    private List<String> matchingETagConstraints = new ArrayList<String>();

    /**
     * Optional list of ETag values that constrain this request to only be executed if the object's
     * ETag does not match any of the specified ETag constraint values.
     */
    private List<String> nonmatchingEtagConstraints = new ArrayList<String>();

    /**
     * Optional field that constrains this request to only be executed if the object has not been
     * modified since the specified date.
     */
    private Date unmodifiedSinceConstraint;

    /**
     * Optional field that constrains this request to only be executed if the object has been
     * modified since the specified date.
     */
    private Date modifiedSinceConstraint;

    /**
     * Optional field that overrides headers on the response.
     */
    private ResponseHeaderOverrides responseHeaders;

    /**
     * The optional customer-provided server-side encryption key to use to decrypt this object.
     */
    private SSECustomerKey sseCustomerKey;

    public GetObjectRequest(COSObjectId cosObjectId) {
        this(cosObjectId.getBucket(), cosObjectId.getKey(), cosObjectId.getVersionId());
    }

    /**
     * Constructs a new {@link GetObjectRequest} with all the required parameters.
     *
     * @param bucketName The name of the bucket containing the desired object.
     * @param key The key in the specified bucket under which the object is stored.
     *
     * @see GetObjectRequest#GetObjectRequest(String, String, String)
     */
    public GetObjectRequest(String bucketName, String key) {
        this(bucketName, key, null);
    }

    /**
     * Constructs a new {@link GetObjectRequest} with all the required parameters.
     *
     * @param bucketName The name of the bucket containing the desired object.
     * @param key The key in the specified bucket under which the object is stored.
     * @param versionId The Qcloud COS version ID specifying a specific version of the object to
     *        download.
     *
     * @see GetObjectRequest#GetObjectRequest(String, String)
     */
    public GetObjectRequest(String bucketName, String key, String versionId) {
        setBucketName(bucketName);
        setKey(key);
        setVersionId(versionId);
    }

    /**
     * Gets the name of the bucket containing the object to be downloaded.
     *
     * @return The name of the bucket containing the object to be downloaded.
     *
     * @see GetObjectRequest#setBucketName(String)
     * @see GetObjectRequest#withBucketName(String)
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Sets the name of the bucket containing the object to be downloaded.
     *
     * @param bucketName The name of the bucket containing the object to be downloaded.
     *
     * @see GetObjectRequest#getBucketName()
     * @see GetObjectRequest#withBucketName(String)
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Sets the name of the bucket containing the object to be downloaded. Returns this
     * {@link GetObjectRequest}, enabling additional method calls to be chained together.
     *
     * @param bucketName The name of the bucket containing the object to be downloaded.
     *
     * @return This {@link GetObjectRequest}, enabling additional method calls to be chained
     *         together.
     *
     * @see GetObjectRequest#getBucketName()
     * @see GetObjectRequest#setBucketName(String)
     */
    public GetObjectRequest withBucketName(String bucketName) {
        setBucketName(bucketName);
        return this;
    }

    /**
     * Gets the key under which the object to be downloaded is stored.
     *
     * @return The key under which the object to be downloaded is stored.
     *
     * @see GetObjectRequest#setKey(String)
     * @see GetObjectRequest#withKey(String)
     */
    public String getKey() {
        return key;
    }

    /**
     * Sets the key under which the object to be downloaded is stored.
     *
     * @param key The key under which the object to be downloaded is stored.
     *
     * @see GetObjectRequest#getKey()
     * @see GetObjectRequest#withKey(String)
     */
    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Sets the key under which the object to be downloaded is stored. Returns this
     * {@link GetObjectRequest}, enabling additional method calls to be chained together.
     *
     * @param key The key under which the object to be downloaded is stored.
     *
     * @return This {@link GetObjectRequest}, enabling additional method calls to be chained
     *         together.
     *
     * @see GetObjectRequest#getKey()
     * @see GetObjectRequest#setKey(String)
     */
    public GetObjectRequest withKey(String key) {
        setKey(key);
        return this;
    }

    /**
     * <p>
     * Gets the optional version ID specifying which version of the object to download. If not
     * specified, the most recent version will be downloaded.
     * </p>
     * <p>
     * Objects created before versioning was enabled or when versioning is suspended are given the
     * default <code>null</code> version ID (see {@link Constants#NULL_VERSION_ID}). Note that the
     * <code>null</code> version ID is a valid version ID and is not the same as not having a
     * version ID.
     * </p>
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link COS#setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     *
     * @return The optional version ID specifying which version of the object to download. If not
     *         specified, the most recent version will be downloaded.
     *
     * @see GetObjectRequest#setVersionId(String)
     * @see GetObjectRequest#withVersionId(String)
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets the optional version ID specifying which version of the object to download. If not
     * specified, the most recent version will be downloaded.
     * <p>
     * Objects created before versioning was enabled or when versioning is suspended will be given
     * the default <code>null</code> version ID (see {@link Constants#NULL_VERSION_ID}). Note that
     * the <code>null</code> version ID is a valid version ID and is not the same as not having a
     * version ID.
     * </p>
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link COS#setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     *
     * @param versionId The optional version ID specifying which version of the object to download.
     *
     * @see GetObjectRequest#getVersionId()
     * @see GetObjectRequest#withVersionId(String)
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * <p>
     * Sets the optional version ID specifying which version of the object to download and returns
     * this object, enabling additional method calls to be chained together. If not specified, the
     * most recent version will be downloaded.
     * </p>
     * <p>
     * Objects created before versioning was enabled or when versioning is suspended will be given
     * the default or <code>null</code> version ID (see {@link Constants#NULL_VERSION_ID}). Note
     * that the <code>null</code> version ID is a valid version ID and is not the same as not having
     * a version ID.
     * </p>
     * <p>
     * For more information about enabling versioning for a bucket, see
     * {@link COS#setBucketVersioningConfiguration(SetBucketVersioningConfigurationRequest)}.
     * </p>
     *
     * @param versionId The optional version ID specifying which version of the object to download.
     *
     * @return The updated request object, enabling additional method calls to be chained together.
     *
     * @see GetObjectRequest#getVersionId()
     * @see GetObjectRequest#setVersionId(String)
     */
    public GetObjectRequest withVersionId(String versionId) {
        setVersionId(versionId);
        return this;
    }


    /*
     * Optional Request Parameters
     */

    /**
     * <p>
     * Gets the optional inclusive byte range within the desired object that will be downloaded by
     * this request.
     * </p>
     * <p>
     * The range is returned as a two element array, containing the start and end index of the byte
     * range. If no byte range has been specified, the entire object is downloaded and this method
     * returns <code>null</code>.
     * </p>
     * 
     * @return A two element array indicating the inclusive start index and end index within the
     *         object being downloaded by this request. Returns <code>null</code> if no range has
     *         been specified, and the whole object is to be downloaded.
     *
     * @see GetObjectMetadataRequest#setRange(long, long)
     * @see GetObjectRequest#withRange(long, long)
     */
    public long[] getRange() {
        return range;
    }

    /**
     * <p>
     * Sets the optional inclusive byte range within the desired object that will be downloaded by
     * this request.
     * </p>
     * <p>
     * The first byte in an object has position 0; as an example, the first ten bytes of an object
     * can be downloaded by specifying a range of 0 to 9.
     * </p>
     * <p>
     * If no byte range is specified, this request downloads the entire object from Qcloud COS.
     * </p>
     *
     * @param start The start of the inclusive byte range to download.
     * @param end The end of the inclusive byte range to download.
     *
     * @see GetObjectMetadataRequest#getRange()
     * @see GetObjectRequest#withRange(long, long)
     */
    public void setRange(long start, long end) {
        range = new long[] {start, end};
    }

    /**
     * <p>
     * Sets the optional inclusive byte range within the desired object that will be downloaded by
     * this request. Returns this {@link GetObjectRequest}, enabling additional method calls to be
     * chained together.
     * </p>
     * <p>
     * The first byte in an object has position 0; as an example, the first ten bytes of an object
     * can be downloaded by specifying a range of 0 to 9.
     * </p>
     * <p>
     * If no byte range is specified, this request downloads the entire object from Qcloud COS.
     * </p>
     *
     * @param start The start of the inclusive byte range to download.
     * @param end The end of the inclusive byte range to download.
     *
     * @return This {@link GetObjectRequest}, enabling additional method calls to be chained
     *         together.
     *
     * @see GetObjectRequest#getRange()
     * @see GetObjectRequest#setRange(long, long)
     */
    public GetObjectRequest withRange(long start, long end) {
        setRange(start, end);
        return this;
    }

    /**
     * Gets the optional list of ETag constraints that, when present, <b>must</b> include a match
     * for the object's current ETag in order for this request to be executed. Only one ETag in the
     * list needs to match for this request to be executed by Qcloud COS.
     *
     * @return The optional list of ETag constraints that when present <b>must</b> include a match
     *         for the object's current ETag in order for this request to be executed.
     *
     * @see GetObjectRequest#setMatchingETagConstraints(List)
     * @see GetObjectRequest#withMatchingETagConstraint(String)
     */
    public List<String> getMatchingETagConstraints() {
        return matchingETagConstraints;
    }

    /**
     * Sets the optional list of ETag constraints that when present <b>must</b> include a match for
     * the object's current ETag in order for this request to be executed. If none of the specified
     * ETags match the object's current ETag, this request will not be executed. Only one ETag in
     * the list needs to match for the request to be executed by Qcloud COS.
     *
     * @param eTagList The optional list of ETag constraints that <b>must</b> include a match for
     *        the object's current ETag in order for this request to be executed.
     *
     * @see GetObjectRequest#getMatchingETagConstraints()
     * @see GetObjectRequest#withMatchingETagConstraint(String)
     */
    public void setMatchingETagConstraints(List<String> eTagList) {
        this.matchingETagConstraints = eTagList;
    }

    /**
     * Sets a single ETag constraint to this request. Returns this {@link GetObjectRequest},
     * enabling additional method calls to be chained together.
     * <p>
     * Multiple ETag constraints can be added to a request, but one must match the object's current
     * ETag in order for this request to be executed. If none of the ETag constraints added to this
     * request match the object's current ETag, this request will not be executed by Qcloud COS.
     * </p>
     *
     * @param eTag The matching ETag constraint to add to this request.
     *
     * @return This {@link GetObjectRequest}, enabling additional method calls to be chained
     *         together.
     *
     * @see GetObjectRequest#getMatchingETagConstraints()
     * @see GetObjectRequest#setMatchingETagConstraints(List)
     */
    public GetObjectRequest withMatchingETagConstraint(String eTag) {
        this.matchingETagConstraints.add(eTag);
        return this;
    }

    /**
     * Gets the optional list of ETag constraints that when present, <b>must</b> not include a match
     * for the object's current ETag in order for this request to be executed. If any entry in the
     * non-matching ETag constraint list matches the object's current ETag, this request <b>will
     * not</b> be executed by Qcloud COS.
     *
     * @return The optional list of ETag constraints that when present, <b>must</b> not include a
     *         match for the object's current ETag in order for this request to be executed.
     *
     * @see GetObjectRequest#setNonmatchingETagConstraints(List)
     * @see GetObjectRequest#withNonmatchingETagConstraint(String)
     */
    public List<String> getNonmatchingETagConstraints() {
        return nonmatchingEtagConstraints;
    }

    /**
     * Sets the optional list of ETag constraints that when present <b>must</b> not include a match
     * for the object's current ETag in order for this request to be executed. If any entry in the
     * non-matching ETag constraint list matches the object's current ETag, this request <b>will
     * not</b> be executed by Qcloud COS.
     *
     * @param eTagList The list of ETag constraints that, when present, <b>must not</b> include a
     *        match for the object's current ETag in order for this request to be executed.
     *
     * @see GetObjectRequest#getNonmatchingETagConstraints()
     * @see GetObjectRequest#withNonmatchingETagConstraint(String)
     */
    public void setNonmatchingETagConstraints(List<String> eTagList) {
        this.nonmatchingEtagConstraints = eTagList;
    }

    /**
     * Sets a single ETag constraint to this request. Returns this {@link GetObjectRequest},
     * enabling additional method calls to be chained together.
     * <p>
     * Multiple ETag constraints can be added to a request, but all ETag constraints <b>must not</b>
     * match the object's current ETag in order for this request to be executed. If any entry in the
     * non-matching ETag constraint list matches the object's current ETag, this request <b>will
     * not</b> be executed by Qcloud COS.
     * </p>
     *
     * @param eTag The non-matching ETag constraint to add to this request.
     *
     * @return This {@link GetObjectRequest}, enabling additional method calls to be chained
     *         together.
     *
     * @see GetObjectRequest#getNonmatchingETagConstraints()
     * @see GetObjectRequest#setNonmatchingETagConstraints(List)
     */
    public GetObjectRequest withNonmatchingETagConstraint(String eTag) {
        this.nonmatchingEtagConstraints.add(eTag);
        return this;
    }

    /**
     * Gets the optional unmodified constraint that restricts this request to executing only if the
     * object has <b>not</b> been modified after the specified date.
     *
     * @return The optional unmodified constraint that restricts this request to executing only if
     *         the object has <b>not</b> been modified after the specified date.
     *
     * @see GetObjectRequest#setUnmodifiedSinceConstraint(Date)
     * @see GetObjectRequest#withUnmodifiedSinceConstraint(Date)
     */
    public Date getUnmodifiedSinceConstraint() {
        return unmodifiedSinceConstraint;
    }

    /**
     * Sets the optional unmodified constraint that restricts this request to executing only if the
     * object has <b>not</b> been modified after the specified date.
     * <p>
     * Note that Qcloud COS will ignore any dates occurring in the future.
     *
     * @param date The unmodified constraint that restricts this request to executing only if the
     *        object has <b>not</b> been modified after this date.
     *
     * @see GetObjectRequest#getUnmodifiedSinceConstraint()
     * @see GetObjectRequest#withUnmodifiedSinceConstraint(Date)
     */
    public void setUnmodifiedSinceConstraint(Date date) {
        this.unmodifiedSinceConstraint = date;
    }

    /**
     * Sets the optional unmodified constraint that restricts this request to executing only if the
     * object has <b>not</b> been modified after the specified date. Returns this
     * {@link GetObjectRequest}, enabling additional method calls to be chained together.
     * <p>
     * Note that Qcloud COS will ignore any dates occurring in the future.
     *
     * @param date The unmodified constraint that restricts this request to executing only if the
     *        object has <b>not</b> been modified after this date.
     *
     * @return This {@link GetObjectRequest}, enabling additional method calls to be chained
     *         together.
     *
     * @see GetObjectRequest#getUnmodifiedSinceConstraint()
     * @see GetObjectRequest#setUnmodifiedSinceConstraint(Date)
     */
    public GetObjectRequest withUnmodifiedSinceConstraint(Date date) {
        setUnmodifiedSinceConstraint(date);
        return this;
    }

    /**
     * Gets the optional modified constraint that restricts this request to executing only if the
     * object <b>has</b> been modified after the specified date.
     *
     * @return The optional modified constraint that restricts this request to executing only if the
     *         object <b>has</b> been modified after the specified date.
     *
     * @see GetObjectRequest#setModifiedSinceConstraint(Date)
     * @see GetObjectRequest#withModifiedSinceConstraint(Date)
     */
    public Date getModifiedSinceConstraint() {
        return modifiedSinceConstraint;
    }

    /**
     * Sets the optional modified constraint that restricts this request to executing only if the
     * object <b>has</b> been modified after the specified date.
     * <p>
     * Note that Qcloud COS will ignore any dates occurring in the future.
     * </p>
     *
     * @param date The modified constraint that restricts this request to executing only if the
     *        object <b>has</b> been modified after the specified date.
     *
     * @see GetObjectRequest#getModifiedSinceConstraint()
     * @see GetObjectRequest#withModifiedSinceConstraint(Date)
     */
    public void setModifiedSinceConstraint(Date date) {
        this.modifiedSinceConstraint = date;
    }

    /**
     * Sets the optional modified constraint that restricts this request to executing only if the
     * object <b>has</b> been modified after the specified date. Returns this
     * {@link GetObjectRequest}, enabling additional method calls to be chained together.
     * <p>
     * Note that Qcloud COS will ignore any dates occurring in the future.
     *
     * @param date The modified constraint that restricts this request to executing only if the
     *        object <b>has</b> been modified after the specified date.
     *
     * @return This {@link GetObjectRequest}, enabling additional method calls to be chained
     *         together.
     *
     * @see GetObjectRequest#getModifiedSinceConstraint()
     * @see GetObjectRequest#setModifiedSinceConstraint(Date)
     */
    public GetObjectRequest withModifiedSinceConstraint(Date date) {
        setModifiedSinceConstraint(date);
        return this;
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
     * @param responseHeaders The headers to be overridden in the service response.
     */
    public void setResponseHeaders(ResponseHeaderOverrides responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

    /**
     * Sets the headers to be overridden in the service response and returns this object, for method
     * chaining.
     *
     * @param responseHeaders The headers to be overridden in the service response.
     *
     * @return This {@link GetObjectRequest} for method chaining.
     */
    public GetObjectRequest withResponseHeaders(ResponseHeaderOverrides responseHeaders) {
        setResponseHeaders(responseHeaders);
        return this;
    }

    @Override
    public SSECustomerKey getSSECustomerKey() {
        return sseCustomerKey;
    }

    /**
     * Sets the optional customer-provided server-side encryption key to use to decrypt this object.
     *
     * @param sseKey The optional customer-provided server-side encryption key to use to decrypt
     *        this object.
     */
    public void setSSECustomerKey(SSECustomerKey sseKey) {
        this.sseCustomerKey = sseKey;
    }

    /**
     * Sets the optional customer-provided server-side encryption key to use to decrypt this object,
     * and returns the updated GetObjectRequest so that additional method calls may be chained
     * together.
     *
     * @param sseKey The optional customer-provided server-side encryption key to use to decrypt
     *        this object.
     *
     * @return The optional customer-provided server-side encryption key to use to decrypt this
     *         object.
     */
    public GetObjectRequest withSSECustomerKey(SSECustomerKey sseKey) {
        setSSECustomerKey(sseKey);
        return this;
    }
    
    public COSObjectId getCOSObjectId() {
        return new COSObjectId(bucketName, key, versionId);
    }

    public int getTrafficLimit() {
        return trafficLimit;
    }

    public void setTrafficLimit(int trafficLimit) {
        this.trafficLimit = trafficLimit;
    }
}
