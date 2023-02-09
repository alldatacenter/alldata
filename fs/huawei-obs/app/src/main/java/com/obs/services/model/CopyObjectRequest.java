/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.obs.services.model;

import java.util.Date;

import com.obs.services.internal.utils.ServiceUtils;

/**
 * Parameters in a request for copying an object
 */
public class CopyObjectRequest extends PutObjectBasicRequest {
    private String sourceBucketName;

    private String sourceObjectKey;

    private ObjectMetadata newObjectMetadata;

    private boolean replaceMetadata;

    private Date ifModifiedSince;

    private Date ifUnmodifiedSince;

    private String ifMatchTag;

    private String ifNoneMatchTag;

    private String versionId;

    private SseCHeader sseCHeaderSource;

    public CopyObjectRequest() {
    }

    /**
     * Constructor
     * 
     * @param sourceBucketName
     *            Source bucket name
     * @param sourceObjectKey
     *            Source object name
     * @param destinationBucketName
     *            Destination bucket name
     * @param destinationObjectKey
     *            Destination object name
     */
    public CopyObjectRequest(String sourceBucketName, String sourceObjectKey, String destinationBucketName,
            String destinationObjectKey) {
        this.sourceBucketName = sourceBucketName;
        this.sourceObjectKey = sourceObjectKey;
        this.bucketName = destinationBucketName;
        this.objectKey = destinationObjectKey;
    }

    /**
     * Obtain SSE-C decryption headers of the source object.
     * 
     * @return SSE-C decryption headers of the source object
     */
    public SseCHeader getSseCHeaderSource() {
        return sseCHeaderSource;
    }

    /**
     * Set SSE-C decryption headers of the source object.
     * 
     * @param sseCHeaderSource
     *            SSE-C decryption headers of the source object
     */
    public void setSseCHeaderSource(SseCHeader sseCHeaderSource) {
        this.sseCHeaderSource = sseCHeaderSource;
    }

    /**
     * Obtain SSE-C encryption headers of the destination object.
     * 
     * @return SSE-C encryption headers
     */
    @Deprecated
    public SseCHeader getSseCHeaderDestination() {
        return this.sseCHeader;
    }

    /**
     * Set SSE-C encryption headers for the destination object.
     * 
     * @param sseCHeaderDestination
     *            SSE-C encryption headers
     */
    @Deprecated
    public void setSseCHeaderDestination(SseCHeader sseCHeaderDestination) {
        this.sseCHeader = sseCHeaderDestination;
    }

    /**
     * Obtain the time condition for copying the object: Only when the source
     * object is modified after the point in time specified by this parameter,
     * it can be copied; otherwise, "412 Precondition Failed" will be returned.
     * 
     * @return Time condition set for copying the object
     */
    public Date getIfModifiedSince() {
        return ServiceUtils.cloneDateIgnoreNull(this.ifModifiedSince);
    }

    /**
     * Set the time condition for copying the object: Only when the source
     * object is modified after the point in time specified by this parameter,
     * it can be copied; otherwise, "412 Precondition Failed" will be returned.
     * 
     * @param ifModifiedSince
     *            Time condition set for copying the object
     * 
     */
    public void setIfModifiedSince(Date ifModifiedSince) {
        this.ifModifiedSince = ServiceUtils.cloneDateIgnoreNull(ifModifiedSince);
    }

    /**
     * Obtain the time condition for copying the object: Only when the source
     * object remains unchanged after the point in time specified by this
     * parameter, it can be copied; otherwise, "412 Precondition Failed" will be
     * returned.
     * 
     * @return Time condition set for copying the object
     */
    public Date getIfUnmodifiedSince() {
        return ServiceUtils.cloneDateIgnoreNull(this.ifUnmodifiedSince);
    }

    /**
     * Set the time condition for copying the object: Only when the source
     * object remains unchanged after the point in time specified by this
     * parameter, it can be copied; otherwise, "412 Precondition Failed" will be
     * returned.
     * 
     * @param ifUnmodifiedSince
     *            Time condition set for copying the object
     */
    public void setIfUnmodifiedSince(Date ifUnmodifiedSince) {
        this.ifUnmodifiedSince = ServiceUtils.cloneDateIgnoreNull(ifUnmodifiedSince);
    }

    /**
     * Obtain the ETag verification condition for copying the object: Only when
     * the ETag of the source object is the same as that specified by this
     * parameter, the object can be copied. Otherwise, "412 Precondition Failed"
     * will be returned.
     * 
     * @return ETag verification condition set for copying the object
     */
    public String getIfMatchTag() {
        return ifMatchTag;
    }

    /**
     * Set the ETag verification condition for copying the object: Only when the
     * ETag of the source object is the same as that specified by this
     * parameter, the object can be copied. Otherwise, "412 Precondition Failed"
     * will be returned.
     * 
     * @param ifMatchTag
     *            ETag verification condition set for copying the object
     */
    public void setIfMatchTag(String ifMatchTag) {
        this.ifMatchTag = ifMatchTag;
    }

    /**
     * Obtain the ETag verification condition for copying the object: Only when
     * the ETag of the source object is different from that specified by this
     * parameter, the object will be copied. Otherwise,
     * "412 Precondition Failed" will be returned.
     * 
     * @return ETag verification condition set for copying the object
     */
    public String getIfNoneMatchTag() {
        return ifNoneMatchTag;
    }

    /**
     * Set the ETag verification condition for copying the object: Only when the
     * ETag of the source object is different from that specified by this
     * parameter, the object will be copied. Otherwise,
     * "412 Precondition Failed" will be returned.
     * 
     * @param ifNoneMatchTag
     *            ETag verification condition set for copying the object
     * 
     */
    public void setIfNoneMatchTag(String ifNoneMatchTag) {
        this.ifNoneMatchTag = ifNoneMatchTag;
    }

    /**
     * Obtain the version ID of the source object.
     * 
     * @return Version ID of the source object
     * 
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Set the version ID for the source object.
     * 
     * @param versionId
     *            Version ID of the source object
     * 
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Obtain the source bucket name.
     * 
     * @return Source bucket name
     */
    public String getSourceBucketName() {
        return sourceBucketName;
    }

    /**
     * Set the source bucket name.
     * 
     * @param sourceBucketName
     *            Source bucket name
     */
    public void setSourceBucketName(String sourceBucketName) {
        this.sourceBucketName = sourceBucketName;
    }

    /**
     * Obtain the source object name.
     * 
     * @return Source object name
     */
    public String getSourceObjectKey() {
        return sourceObjectKey;
    }

    /**
     * Set the source object name.
     * 
     * @param sourceObjectKey
     *            Source object name
     */
    public void setSourceObjectKey(String sourceObjectKey) {
        this.sourceObjectKey = sourceObjectKey;
    }

    /**
     * Obtain the destination bucket name.
     * 
     * @return Destination bucket name
     */
    public String getDestinationBucketName() {
        return this.bucketName;
    }

    /**
     * Set the destination bucket name.
     * 
     * @param destinationBucketName
     *            Destination bucket name
     */
    public void setDestinationBucketName(String destinationBucketName) {
        this.bucketName = destinationBucketName;
    }

    /**
     * Obtain the destination object name.
     * 
     * @return Destination object name
     */
    public String getDestinationObjectKey() {
        return this.objectKey;
    }

    /**
     * Set the destination object name.
     * 
     * @param destinationObjectKey
     *            Destination object name
     */
    public void setDestinationObjectKey(String destinationObjectKey) {
        this.objectKey = destinationObjectKey;
    }

    /**
     * Obtain the properties, including customized metadata, of the destination
     * object.
     * 
     * @return ObjectMetadata Properties of the destination object
     */
    public ObjectMetadata getNewObjectMetadata() {
        return newObjectMetadata;
    }

    /**
     * Set the properties, including customized metadata, of the destination
     * object.
     * 
     * @param newObjectMetadata
     *            Properties of the destination object
     */
    public void setNewObjectMetadata(ObjectMetadata newObjectMetadata) {
        this.newObjectMetadata = newObjectMetadata;
    }

    /**
     * Obtain the identifier specifying whether to replace properties of the
     * destination object. "true" indicates that properties will be replaced
     * (used together with "setNewObjectMetadata") and "false" indicates that
     * the destination object inherits properties from the source object.
     * 
     * @return Identifier specifying whether to replace the properties of the
     *         destination object
     */
    public boolean isReplaceMetadata() {
        return replaceMetadata;
    }

    /**
     * Set the identifier specifying whether to replace properties of the
     * destination object. "true" indicates that properties will be replaced
     * (used together with "setNewObjectMetadata") and "false" indicates that
     * the destination object inherits properties from the source object.
     * 
     * @param replaceMetadata
     *            Identifier specifying whether to replace the properties of the
     *            destination object
     * 
     */
    public void setReplaceMetadata(boolean replaceMetadata) {
        this.replaceMetadata = replaceMetadata;
    }

    @Override
    public String toString() {
        return "CopyObjectRequest [sourceBucketName=" + sourceBucketName + ", sourceObjectKey=" + sourceObjectKey
                + ", destinationBucketName=" + bucketName + ", destinationObjectKey=" + objectKey
                + ", newObjectMetadata=" + newObjectMetadata + ", replaceMetadata=" + replaceMetadata
                + ", isEncodeHeaders=" + encodeHeaders + ", ifModifiedSince=" + ifModifiedSince
                + ", ifUnmodifiedSince=" + ifUnmodifiedSince + ", ifMatchTag=" + ifMatchTag
                + ", ifNoneMatchTag=" + ifNoneMatchTag + ", versionId=" + versionId + ", sseKmsHeader="
                + sseKmsHeader + ", sseCHeaderSource=" + sseCHeaderSource + ", sseCHeaderDestination=" + sseCHeader
                + ", acl=" + acl + ", successRedirectLocation=" + successRedirectLocation + "]";
    }

}
