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
**/

package com.obs.services.model;

/**
 * Parameters in the request for copying a part
 */
public class CopyPartRequest extends AbstractMultipartRequest {

    {
        httpMethod = HttpMethodEnum.PUT;
    }

    private String sourceBucketName;

    private String sourceObjectKey;

    private String destinationBucketName;

    private String destinationObjectKey;

    private Long byteRangeStart;

    private Long byteRangeEnd;

    private SseCHeader sseCHeaderSource;

    private SseCHeader sseCHeaderDestination;

    private String versionId;

    private int partNumber;

    public CopyPartRequest() {

    }

    /**
     * Constructor
     * 
     * @param uploadId
     *            Multipart upload ID
     * @param sourceBucketName
     *            Source bucket name
     * @param sourceObjectKey
     *            Source object name
     * @param destinationBucketName
     *            Destination bucket name
     * @param destinationObjectKey
     *            Destination object name
     * @param partNumber
     *            Part number
     */
    public CopyPartRequest(String uploadId, String sourceBucketName, String sourceObjectKey,
            String destinationBucketName, String destinationObjectKey, int partNumber) {
        this.uploadId = uploadId;
        this.sourceBucketName = sourceBucketName;
        this.sourceObjectKey = sourceObjectKey;
        this.destinationBucketName = destinationBucketName;
        this.destinationObjectKey = destinationObjectKey;
        this.partNumber = partNumber;
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
     * @return SSE-C encryption headers of the destination object
     */
    public SseCHeader getSseCHeaderDestination() {
        return sseCHeaderDestination;
    }

    /**
     * Set SSE-C encryption headers for the destination object.
     * 
     * @param sseCHeaderDestination
     *            SSE-C encryption headers of the destination object
     */
    public void setSseCHeaderDestination(SseCHeader sseCHeaderDestination) {
        this.sseCHeaderDestination = sseCHeaderDestination;
    }

    /**
     * Obtain the start position for copying.
     * 
     * @return Start position for copying
     */
    public Long getByteRangeStart() {
        return byteRangeStart;
    }

    /**
     * Set the start position for copying.
     * 
     * @param byteRangeStart
     *            Start position for copying
     * 
     */
    public void setByteRangeStart(Long byteRangeStart) {
        this.byteRangeStart = byteRangeStart;
    }

    /**
     * Obtain the end position for copying.
     * 
     * @return End position for copying
     */
    public Long getByteRangeEnd() {
        return byteRangeEnd;
    }

    /**
     * Set the end position for copying.
     * 
     * @param byteRangeEnd
     *            End position for copying
     * 
     */
    public void setByteRangeEnd(Long byteRangeEnd) {
        this.byteRangeEnd = byteRangeEnd;
    }

    /**
     * Obtain the part number of the to-be-copied part.
     * 
     * @return Part number
     */
    public int getPartNumber() {
        return partNumber;
    }

    /**
     * Set the part number of the to-be-copied part.
     * 
     * @param partNumber
     *            Part number
     * 
     */
    public void setPartNumber(int partNumber) {
        this.partNumber = partNumber;
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
     * @param bucketName
     *            Source bucket name
     * 
     */
    public void setSourceBucketName(String bucketName) {
        this.sourceBucketName = bucketName;
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
     * @param objectKey
     *            Source object name
     * 
     */
    public void setSourceObjectKey(String objectKey) {
        this.sourceObjectKey = objectKey;
    }

    /**
     * Obtain the name of the bucket (destination bucket) to which the multipart
     * upload belongs.
     * 
     * @return Name of the bucket to which the multipart upload belongs
     */
    public String getDestinationBucketName() {
        return destinationBucketName;
    }

    /**
     * Set the name of the bucket (destination bucket) to which the multipart
     * upload belongs.
     * 
     * @param destBucketName
     *            Name of the bucket to which the multipart upload belongs
     * 
     */
    public void setDestinationBucketName(String destBucketName) {
        this.destinationBucketName = destBucketName;
    }

    /**
     * Obtain the name of the object (destination object) involved in the
     * multipart upload.
     * 
     * @return Name of the object involved in the multipart upload
     * 
     */
    public String getDestinationObjectKey() {
        return destinationObjectKey;
    }

    /**
     * Set the name of the object (destination object) involved in the multipart
     * upload.
     * 
     * @param destObjectKey
     *            Name of the object involved in the multipart upload
     * 
     */
    public void setDestinationObjectKey(String destObjectKey) {
        this.destinationObjectKey = destObjectKey;
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
     * Obtain the name of the bucket (destination bucket) to which the multipart
     * upload belongs.
     * 
     * @return Name of the bucket to which the multipart upload belongs
     */
    @Override
    public String getBucketName() {
        return this.destinationBucketName;
    }

    /**
     * Set the name of the bucket (destination bucket) to which the multipart
     * upload belongs.
     * 
     * @param destBucketName
     *            Name of the bucket to which the multipart upload belongs
     * 
     */
    @Override
    public void setBucketName(String bucketName) {
        this.destinationBucketName = bucketName;
    }

    /**
     * Obtain the name of the object (destination object) involved in the
     * multipart upload.
     * 
     * @return Name of the object involved in the multipart upload
     * 
     */
    @Override
    public void setObjectKey(String objectKey) {
        this.destinationObjectKey = objectKey;
    }

    /**
     * Obtain the name of the object (destination object) involved in the
     * multipart upload.
     * 
     * @return Name of the object involved in the multipart upload
     * 
     */
    @Override
    public String getObjectKey() {
        return this.destinationObjectKey;
    }

    @Override
    public String toString() {
        return "CopyPartRequest [uploadId=" + uploadId + ", sourceBucketName=" + sourceBucketName + ", sourceObjectKey="
                + sourceObjectKey + ", destinationBucketName=" + destinationBucketName + ", destinationObjectKey="
                + destinationObjectKey + ", byteRangeStart=" + byteRangeStart + ", byteRangeEnd=" + byteRangeEnd
                + ", sseCHeaderSource=" + sseCHeaderSource + ", sseCHeaderDestination=" + sseCHeaderDestination
                + ", versionId=" + versionId + ", partNumber=" + partNumber + "]";
    }

}
