/**
 * Copyright 2019 Huawei Technologies Co.,Ltd.
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use
 * this file except in compliance with the License.  You may obtain a copy of the
 * License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed
 * under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 **/

package com.obs.services.model;

/**
 * Parameters in a request for listing uploaded parts
 */
public class ListPartsRequest extends BaseObjectRequest {

    {
        httpMethod = HttpMethodEnum.GET;
    }

    private String uploadId;

    private Integer maxParts;

    private Integer partNumberMarker;

    private String encodingType;

    public ListPartsRequest() {

    }

    /**
     * Constructor
     *
     * @param bucketName Name of the bucket to which the multipart upload belongs
     * @param objectKey        Name of the object involved in the multipart upload
     */
    public ListPartsRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Constructor
     *
     * @param bucketName Name of the bucket to which the multipart upload belongs
     * @param objectKey        Name of the object involved in the multipart upload
     * @param uploadId   Multipart upload ID
     */
    public ListPartsRequest(String bucketName, String objectKey, String uploadId) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
    }

    /**
     * Constructor
     *
     * @param bucketName Name of the bucket to which the multipart upload belongs
     * @param objectKey        Name of the object involved in the multipart upload
     * @param uploadId   Multipart upload ID
     * @param maxParts   Maximum number of uploaded parts that can be listed
     */
    public ListPartsRequest(String bucketName, String objectKey, String uploadId, Integer maxParts) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.maxParts = maxParts;
    }

    /**
     * Constructor
     *
     * @param bucketName       Name of the bucket to which the multipart upload belongs
     * @param objectKey              Name of the object involved in the multipart upload
     * @param uploadId         Multipart upload ID
     * @param maxParts         Maximum number of uploaded parts that can be listed
     * @param partNumberMarker Start position for listing parts
     */
    public ListPartsRequest(String bucketName, String objectKey, String uploadId, Integer maxParts,
                            Integer partNumberMarker) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.maxParts = maxParts;
        this.partNumberMarker = partNumberMarker;
    }

    /**
     * Constructor
     *
     * @param bucketName       Name of the bucket to which the multipart upload belongs
     * @param objectKey              Name of the object involved in the multipart upload
     * @param uploadId         Multipart upload ID
     * @param maxParts         Maximum number of uploaded parts that can be listed
     * @param partNumberMarker Start position for listing parts
     * @param encodingType     Use this encoding type to encode keys that contains invalid characters,
     *                         the value could be "url"
     */
    public ListPartsRequest(String bucketName, String objectKey, String uploadId, Integer maxParts,
                            Integer partNumberMarker, String encodingType) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.uploadId = uploadId;
        this.maxParts = maxParts;
        this.partNumberMarker = partNumberMarker;
        this.encodingType = encodingType;
    }

    @Deprecated
    public String getKey() {
        return objectKey;
    }

    @Deprecated
    public void setKey(String objectKey) {
        this.objectKey = objectKey;
    }

    /**
     * Obtain the multipart upload ID.
     *
     * @return Multipart upload ID
     */
    public String getUploadId() {
        return uploadId;
    }

    /**
     * Set the multipart upload ID.
     *
     * @param uploadId Multipart upload ID
     */
    public void setUploadId(String uploadId) {
        this.uploadId = uploadId;
    }

    /**
     * Obtain the maximum number of uploaded parts that can be listed.
     *
     * @return Maximum number of uploaded parts that can be listed
     */
    public Integer getMaxParts() {
        return maxParts;
    }

    /**
     * Set the maximum number of uploaded parts that can be listed.
     *
     * @param maxParts Maximum number of uploaded parts that can be listed
     */
    public void setMaxParts(Integer maxParts) {
        this.maxParts = maxParts;
    }

    /**
     * Obtain the start position for listing parts.
     *
     * @return Start position for listing parts
     */
    public Integer getPartNumberMarker() {
        return partNumberMarker;
    }

    /**
     * Set the start position for listing parts.
     *
     * @param partNumberMarker Start position for listing parts
     */
    public void setPartNumberMarker(Integer partNumberMarker) {
        this.partNumberMarker = partNumberMarker;
    }

    /**
     * Set encoding type to encode objectkeys, the value could be url
     *
     * @param encodingType encoding type for encoding
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     * Get encoding type to encode objectkeys
     *
     * @return encoding type for encoding
     */
    public String getEncodingType() {
        return encodingType;
    }

    @Override
    public String toString() {
        return "ListPartsRequest [bucketName=" + bucketName + ", objectKey=" + objectKey + ", uploadId="
                + uploadId + ", maxParts=" + maxParts + ", partNumberMarker=" + partNumberMarker
                + ", encodingType=" + encodingType + "]";
    }
}
