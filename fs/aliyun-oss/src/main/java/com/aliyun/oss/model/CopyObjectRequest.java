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
import java.util.Date;
import java.util.List;

/**
 * The request class that is used to copy an object. It wraps all parameters
 * needed to copy an object.
 */
public class CopyObjectRequest extends WebServiceRequest {

    // Source bucket name.
    private String sourceBucketName;

    // Source object key.
    private String sourceKey;
    
    // Optional version Id specifying which version of the source object to
    // copy. If not specified, the most recent version of the source object will
    // be copied.
    private String sourceVersionId;

    // Target bucket name.
    private String destinationBucketName;

    // Target object key.
    private String destinationKey;

    // Target server's encryption algorithm.
    private String serverSideEncryption;

    // Target server's encryption key ID.
    private String serverSideEncryptionKeyID;

    // Target object's metadata information.
    private ObjectMetadata newObjectMetadata;

    // ETag matching Constraints. The copy only happens when source object's
    // ETag matches the specified one.
    // If not matches, return 412.
    // It's optional.
    private List<String> matchingETagConstraints = new ArrayList<String>();

    // ETag non-matching Constraints. The copy only happens when source object's
    // ETag does not match the specified one.
    // If matches, return 412.
    // It's optional.
    private List<String> nonmatchingEtagConstraints = new ArrayList<String>();

    // If the specified time is same or later than the actual last modified
    // time, copy the file.
    // Otherwise return 412.
    // It's optional.
    private Date unmodifiedSinceConstraint;

    // If the specified time is earlier than the actual last modified time, copy
    // the file.
    // Otherwise return 412. It's optional.
    private Date modifiedSinceConstraint;
    
    // The one who pays for the request
    private Payer payer;

    /**
     * Constructor
     * 
     * @param sourceBucketName
     *            Source bucket name.
     * @param sourceKey
     *            Source key.
     * @param destinationBucketName
     *            Target bucket name.
     * @param destinationKey
     *            Target key.
     */
    public CopyObjectRequest(String sourceBucketName, String sourceKey, String destinationBucketName,
            String destinationKey) {
        setSourceBucketName(sourceBucketName);
        setSourceKey(sourceKey);
        setDestinationBucketName(destinationBucketName);
        setDestinationKey(destinationKey);
    }
    
    /**
     * <p>
     * Constructs a new {@link CopyObjectRequest} with basic options, providing
     * an OSS version ID identifying the specific version of the source object
     * to copy.
     * </p>
     *
     * @param sourceBucketName
     *            The name of the OSS bucket containing the object to copy.
     * @param sourceKey
     *            The key in the source bucket under which the object to copy is
     *            stored.
     * @param sourceVersionId
     *            The OSS version ID which uniquely identifies a specific version
     *            of the source object to copy.
     * @param destinationBucketName
     *            The name of the OSS bucket in which the new object will be
     *            copied.
     * @param destinationKey
     *            The key in the destination bucket under which the new object
     *            will be copied.
     */
    public CopyObjectRequest(String sourceBucketName, String sourceKey, String sourceVersionId,
        String destinationBucketName, String destinationKey) {
        setSourceBucketName(sourceBucketName);
        setSourceKey(sourceKey);
        setSourceVersionId(sourceVersionId);
        setDestinationBucketName(destinationBucketName);
        setDestinationKey(destinationKey);
    }

    /**
     * Gets the source bucket name.
     * 
     * @return Source bucket name
     */
    public String getSourceBucketName() {
        return sourceBucketName;
    }

    /**
     * Sets the source bucket name.
     * 
     * @param sourceBucketName
     *            Source bucket name.
     */
    public void setSourceBucketName(String sourceBucketName) {
        this.sourceBucketName = sourceBucketName;
    }

    /**
     * Gets the source object key.
     * 
     * @return Source object key.
     */
    public String getSourceKey() {
        return sourceKey;
    }

    /**
     * Sets the source object key.
     * 
     * @param sourceKey
     *            Source object key.
     */
    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }
    
    /**
     * <p>
     * Gets the version ID specifying which version of the source
     * object to copy. If not specified, the most recent version of the source
     * object will be copied.
     * </p>
     * <p>
     * Objects created before enabling versioning or when versioning is
     * suspended are given the default <code>null</code> version ID (see
     * {@link com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID}). Note that the
     * <code>null</code> version ID is a valid version ID and is not the
     * same as not having a version ID.
     * </p>
     *
     * @return The version ID specifying which version of the source
     *         object to copy.
     *
     * @see com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID
     * @see CopyObjectRequest#setSourceVersionId(String sourceVersionId)
     */
    public String getSourceVersionId() {
        return sourceVersionId;
    }

    /**
     * <p>
     * Sets the optional version ID specifying which version of the source
     * object to copy. If not specified, the most recent version of the source
     * object will be copied.
     * </p>
     * <p>
     * Objects created before enabling versioning or when versioning is
     * suspended are given the default <code>null</code> version ID (see
     * {@link com.aliyun.oss.internal.OSSConstants#NULL_VERSION_ID}). Note that the
     * <code>null</code> version ID is a valid version ID and is not the
     * same as not having a version ID.
     * </p>
     *
     * @param sourceVersionId
     *            The optional version ID specifying which version of the
     *            source object to copy.
     */
    public void setSourceVersionId(String sourceVersionId) {
        this.sourceVersionId = sourceVersionId;
    }

    /**
     * Gets the target bucket name.
     * 
     * @return Target bucket name.
     */
    public String getDestinationBucketName() {
        return destinationBucketName;
    }

    /**
     * Sets the target bucket name.
     * 
     * @param destinationBucketName
     *            Target bucket name.
     */
    public void setDestinationBucketName(String destinationBucketName) {
        this.destinationBucketName = destinationBucketName;
    }

    /**
     * Gets the target object key.
     * 
     * @return Target object key.
     */
    public String getDestinationKey() {
        return destinationKey;
    }

    /**
     * Gets the target object key.
     * 
     * @param destinationKey
     *            Target object key.
     */
    public void setDestinationKey(String destinationKey) {
        this.destinationKey = destinationKey;
    }

    /**
     * Gets target object's {@link ObjectMetadata}.
     * 
     * @return Target Object {@link ObjectMetadata}.
     */
    public ObjectMetadata getNewObjectMetadata() {
        return newObjectMetadata;
    }

    /**
     * Sets target object's {@link ObjectMetadata}. Optional.
     * 
     * @param newObjectMetadata
     *            Target Object {@link ObjectMetadata}.
     */
    public void setNewObjectMetadata(ObjectMetadata newObjectMetadata) {
        this.newObjectMetadata = newObjectMetadata;
    }

    /**
     * Gets the ETag matching constraints.
     * 
     * @return ETag matching constraints
     */
    public List<String> getMatchingETagConstraints() {
        return matchingETagConstraints;
    }

    /**
     * Sets the ETag matching constraints.
     * 
     * @param matchingETagConstraints
     *            ETag matching constraints.
     */
    public void setMatchingETagConstraints(List<String> matchingETagConstraints) {
        this.matchingETagConstraints.clear();
        if (matchingETagConstraints != null && !matchingETagConstraints.isEmpty()) {
            this.matchingETagConstraints.addAll(matchingETagConstraints);
        }
    }

    public void clearMatchingETagConstraints() {
        this.matchingETagConstraints.clear();
    }

    /**
     * Gets the ETag non-matching constraints.
     * 
     * @return ETag non-matching constraints。
     */
    public List<String> getNonmatchingEtagConstraints() {
        return nonmatchingEtagConstraints;
    }

    /**
     * Sets the ETag non-matching constraints.
     * 
     * @param nonmatchingEtagConstraints
     *            ETag non-matching sontraints.
     */
    public void setNonmatchingETagConstraints(List<String> nonmatchingEtagConstraints) {
        this.nonmatchingEtagConstraints.clear();
        if (nonmatchingEtagConstraints != null && !nonmatchingEtagConstraints.isEmpty()) {
            this.nonmatchingEtagConstraints.addAll(nonmatchingEtagConstraints);
        }
    }

    public void clearNonmatchingETagConstraints() {
        this.nonmatchingEtagConstraints.clear();
    }

    /**
     * Gets the unmodified since constraint.
     * 
     * @return The time threshold. If it's same or later than the actual
     *         modified time, copy the file.
     */
    public Date getUnmodifiedSinceConstraint() {
        return unmodifiedSinceConstraint;
    }

    /**
     * Sets the unmodified since constraint (optional).
     * 
     * @param unmodifiedSinceConstraint
     *            The time threshold. If it's same or later than the actual
     *            modified time, copy the file.
     */
    public void setUnmodifiedSinceConstraint(Date unmodifiedSinceConstraint) {
        this.unmodifiedSinceConstraint = unmodifiedSinceConstraint;
    }

    /**
     * Gets the modified since constraint.
     * 
     * @return The time threshold. If it's earlier than the actual modified
     *         time, copy the file.
     */
    public Date getModifiedSinceConstraint() {
        return modifiedSinceConstraint;
    }

    /**
     * Sets the modified since constraint.
     * 
     * @param modifiedSinceConstraint
     *            The time threshold. If it's earlier than the actual modified
     *            time, copy the file.
     */
    public void setModifiedSinceConstraint(Date modifiedSinceConstraint) {
        this.modifiedSinceConstraint = modifiedSinceConstraint;
    }

    /**
     * Gets the target object's server side encryption algorithm.
     * 
     * @return Server side encryption algorithm，null if no encryption.
     */
    public String getServerSideEncryption() {
        return this.serverSideEncryption;
    }

    /**
     * Sets the target object's server side encryption algorithm.
     * 
     * @param serverSideEncryption
     *            Server side encryption algorithm，null if no encryption.
     */
    public void setServerSideEncryption(String serverSideEncryption) {
        this.serverSideEncryption = serverSideEncryption;
    }

    /**
     * Gets the target object's server side encryption key ID.
     *
     * @return Server side encryption key ID，null if no encryption key ID.
     */
    public String getServerSideEncryptionKeyId() {
        return this.serverSideEncryptionKeyID;
    }

    /**
     * Sets the target object's server side encryption key ID.
     *
     * @param serverSideEncryptionKeyId
     *            Server side encryption key ID，null if no encryption key ID.
     */
    public void setServerSideEncryptionKeyId(String serverSideEncryptionKeyId) {
        this.serverSideEncryptionKeyID = serverSideEncryptionKeyId;
    }

    /**
     * * <p>
     * Sets the one who pays for the request
     * The Third party should set request payer when requesting resources.
     * </p>
     * @param payer
     *            The one who pays for the request
     * */
    public void setRequestPayer(Payer payer) {
        this.payer = payer;
    }
    
    /**
     * * <p>
     * Gets the one who pays for the request
     * </p>
     * @return The one who pays for the request
     * */
    public Payer getRequestPayer() {
        return payer;
    }
}
