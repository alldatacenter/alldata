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
import java.util.Date;

import com.qcloud.cos.internal.ObjectExpirationResult;
import com.qcloud.cos.internal.SSEResultBase;
import com.qcloud.cos.model.ciModel.persistence.CIUploadResult;

public class PutObjectResult extends SSEResultBase implements ObjectExpirationResult, Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -2210897720269365495L;

    /** x-cos-requestid **/
    private String requestId;
    
    /** date **/
    private String dateStr;

    /**
     * The version ID of the new, uploaded object. This field will only be present if object
     * versioning has been enabled for the bucket to which the object was uploaded.
     */
    private String versionId;

    /** The ETag value of the new object */
    private String eTag;

    /** The time this object expires, or null if it has no expiration */
    private Date expirationTime;

    /** The expiration rule for this object */
    private String expirationTimeRuleId;

    /** The content MD5 */
    private String contentMd5;

    /** The metadata returned as a result of PutObject operation. */
    private ObjectMetadata metadata;

    /** The crc64ecma value for this object */
    private String crc64Ecma;

    /** The ci upload result */
    private CIUploadResult ciUploadResult;

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
     * @return date str
     */
    public String getDateStr() {
        return dateStr;
    }

    /**
     * set date str for this upload
     * @param dateStr  date str header
     */
    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }

    /**
     * Gets the optional version ID of the newly uploaded object. This field will be set only if
     * object versioning is enabled for the bucket the object was uploaded to.
     *
     * @return The optional version ID of the newly uploaded object.
     *
     * @see PutObjectResult#setVersionId(String)
     */
    public String getVersionId() {
        return versionId;
    }


    /**
     * Sets the optional version ID of the newly uploaded object.
     *
     * @param versionId The optional version ID of the newly uploaded object.
     *
     * @see PutObjectResult#getVersionId()
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Gets the server-side ETag value for the newly created object.
     *
     * @return The server-side ETag value for the new object.
     *
     * @see PutObjectResult#setETag(String)
     */
    public String getETag() {
        return eTag;
    }

    /**
     * Sets the ETag value for the new object that was created from the associated
     * <code>putObject</code> request.
     *
     * @param eTag The ETag value for the new object.
     *
     * @see PutObjectResult#getETag()
     */
    public void setETag(String eTag) {
        this.eTag = eTag;
    }

    /**
     * Returns the expiration time for this object, or null if it doesn't expire.
     */
    public Date getExpirationTime() {
        return expirationTime;
    }

    /**
     * Sets the expiration time for the object.
     *
     * @param expirationTime The expiration time for the object.
     */
    public void setExpirationTime(Date expirationTime) {
        this.expirationTime = expirationTime;
    }

    /**
     * Returns the {@link BucketLifecycleConfiguration} rule ID for this object's expiration, or
     * null if it doesn't expire.
     */
    public String getExpirationTimeRuleId() {
        return expirationTimeRuleId;
    }

    /**
     * Sets the {@link BucketLifecycleConfiguration} rule ID for this object's expiration
     *
     * @param expirationTimeRuleId The rule ID for this object's expiration
     */
    public void setExpirationTimeRuleId(String expirationTimeRuleId) {
        this.expirationTimeRuleId = expirationTimeRuleId;
    }

    /**
     * Sets the Base64-encoded MD5 hash of the object content that was calculated on the
     * client-side.
     *
     * @param contentMd5 The content MD5
     */
    public void setContentMd5(String contentMd5) {
        this.contentMd5 = contentMd5;
    }

    /**
     * Returns the Base64-encoded MD5 hash of the object content that was calculated on the
     * client-side. This method returns null if the MD5 validation is disabled and the caller didn't
     * provide the MD5 hash in the ObjectMetadata when sending the PutObjectRequest.
     */
    public String getContentMd5() {
        return contentMd5;
    }

    /**
     * Returns the metadata retrieved as a response to {@link COSClient#putObject(PutObjectRequest)}
     * operation.
     */
    public ObjectMetadata getMetadata() {
        return metadata;
    }

    /**
     * Sets the metadata retrieved as a response to {@link COSClient#putObject(PutObjectRequest)}
     * operation.
     */
    public void setMetadata(ObjectMetadata metadata) {
        this.metadata = metadata;
    }

    public String getCrc64Ecma() {
        return crc64Ecma;
    }

    public void setCrc64Ecma(String crc64Ecma) {
        this.crc64Ecma = crc64Ecma;
    }

    public CIUploadResult getCiUploadResult() {
        return ciUploadResult;
    }

    public void setCiUploadResult(CIUploadResult ciUploadResult) {
        this.ciUploadResult = ciUploadResult;
    }
}
