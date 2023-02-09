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

/**
 * Contains information returned by COS for a completed copy operation.
 * <p>
 * See {@link com.qcloud.cos.transfer.TransferManager} for more information about creating transfers.
 *
 * @see com.qcloud.cos.transfer.TransferManager#copy(String, String, String, String)
 * @see com.qcloud.cos.transfer.TransferManager#copy(CopyObjectRequest)
 */
public class CopyResult {

    /** x-cos-requestid **/
    private String requestId;

    /** date **/
    private String dateStr;
    
    /** The name of the bucket containing the object to be copied */
    private String sourceBucketName;

    /**
     * The key in the source bucket under which the object to be copied is
     * stored
     */
    private String sourceKey;

    /** The name of the bucket to contain the copy of the source object */
    private String destinationBucketName;

    /**
     * The key in the destination bucket under which the source object will be
     * copied
     */
    private String destinationKey;

    /**
     * The entity tag identifying the new object. An entity tag is an opaque
     * string that changes if and only if an object's data changes.
     */
    private String eTag;

    /**
     * The version ID of the new object, only present if versioning has been
     * enabled for the bucket.
     */
    private String versionId;

    /** The crc64ecma value for this object */
    private String crc64Ecma;

    /**
     * get requestid for this upload
     * 
     * @return requestid
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * set requestId for this upload
     * 
     * @param requestId the requestId for the upload
     */

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    /**
     * get date header for this upload
     * 
     * @return date str
     */
    public String getDateStr() {
        return dateStr;
    }

    /**
     * set date str for this upload
     * 
     * @param dateStr date str header
     */
    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    /**
     * Gets the name of the bucket containing the source object to be copied.
     *
     * @return The name of the bucket containing the source object to be copied.
     *
     * @see CopyResult#setSourceBucketName(String sourceBucketName)
     */
    public String getSourceBucketName() {
        return sourceBucketName;
    }

    /**
     * Sets the name of the bucket containing the source object to be copied.
     *
     * @param sourceBucketName
     *            The name of the bucket containing the source object to be
     *            copied.
     * @see CopyResult#getSourceBucketName()
     */
    public void setSourceBucketName(String sourceBucketName) {
        this.sourceBucketName = sourceBucketName;
    }

    /**
     * Gets the source bucket key under which the source object to be copied is
     * stored.
     *
     * @return The source bucket key under which the source object to be copied
     *         is stored.
     *
     * @see CopyResult#setSourceKey(String sourceKey)
     */
    public String getSourceKey() {
        return sourceKey;
    }

    /**
     * Sets the source bucket key under which the source object to be copied is
     * stored.
     *
     * @param sourceKey
     *            The source bucket key under which the source object to be
     *            copied is stored.
     *
     * @see CopyResult#setSourceKey(String sourceKey)
     */
    public void setSourceKey(String sourceKey) {
        this.sourceKey = sourceKey;
    }

    /**
     * Gets the destination bucket name which will contain the new, copied
     * object.
     *
     * @return The name of the destination bucket which will contain the new,
     *         copied object.
     *
     * @see CopyResult#setDestinationBucketName(String destinationBucketName)
     */
    public String getDestinationBucketName() {
        return destinationBucketName;
    }

    /**
     * Sets the destination bucket name which will contain the new, copied
     * object.
     *
     * @param destinationBucketName
     *            The name of the destination bucket which will contain the new,
     *            copied object.
     *
     * @see CopyResult#getDestinationBucketName()
     */
    public void setDestinationBucketName(String destinationBucketName) {
        this.destinationBucketName = destinationBucketName;
    }

    /**
     * Gets the destination bucket key under which the new, copied object will
     * be stored.
     *
     * @return The destination bucket key under which the new, copied object
     *         will be stored.
     *
     * @see CopyResult#setDestinationKey(String destinationKey)
     */
    public String getDestinationKey() {
        return destinationKey;
    }

    /**
     * Sets the destination bucket key under which the new, copied object will
     * be stored.
     *
     * @param destinationKey
     *            The destination bucket key under which the new, copied object
     *            will be stored.
     *
     * @see CopyResult#getDestinationKey()
     */
    public void setDestinationKey(String destinationKey) {
        this.destinationKey = destinationKey;
    }

    /**
     * Returns the entity tag identifying the new object. An entity tag is an
     * opaque string that changes if and only if an object's data changes.
     *
     * @return An opaque string that changes if and only if an object's data
     *         changes.
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the entity tag identifying the new object. An entity tag is an
     * opaque string that changes if and only if an object's data changes.
     *
     * @param etag
     *            The entity tag.
     */
    public void setETag(String etag) {
        this.eTag = etag;
    }

    /**
     * Returns the version ID of the new object. The version ID is only set if
     * versioning has been enabled for the bucket.
     *
     * @return The version ID of the new object. The version ID is only set if
     *         versioning has been enabled for the bucket.
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Sets the version ID of the new object, only present if versioning has
     * been enabled for the bucket.
     *
     * @param versionId
     *            The version ID of the new object, only present if versioning
     *            has been enabled for the bucket.
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getCrc64Ecma() {
        return crc64Ecma;
    }

    public void setCrc64Ecma(String crc64Ecma) {
        this.crc64Ecma = crc64Ecma;
    }
}

