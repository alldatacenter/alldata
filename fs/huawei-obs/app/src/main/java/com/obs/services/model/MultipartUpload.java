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

import java.util.Date;

import com.obs.services.internal.utils.ServiceUtils;

/**
 * Multipart upload
 */
public class MultipartUpload {
    private String uploadId;

    private String bucketName;

    private String objectKey;

    private Date initiatedDate;

    private StorageClassEnum storageClass;

    private Owner owner;

    private Owner initiator;

    public MultipartUpload(String uploadId, String objectKey, Date initiatedDate, StorageClassEnum storageClass,
            Owner owner, Owner initiator) {
        super();
        this.uploadId = uploadId;
        this.objectKey = objectKey;
        this.initiatedDate = ServiceUtils.cloneDateIgnoreNull(initiatedDate);
        this.storageClass = storageClass;
        this.owner = owner;
        this.initiator = initiator;
    }

    public MultipartUpload(String uploadId, String bucketName, String objectKey, Date initiatedDate,
            StorageClassEnum storageClass, Owner owner, Owner initiator) {
        super();
        this.uploadId = uploadId;
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.initiatedDate = ServiceUtils.cloneDateIgnoreNull(initiatedDate);
        this.storageClass = storageClass;
        this.owner = owner;
        this.initiator = initiator;
    }

    /**
     * Creator of the multipart upload
     * 
     * @return Creator of the multipart upload
     */
    public Owner getInitiator() {
        return initiator;
    }

    /**
     * Query the creator of the multipart upload.
     * 
     * @return Owner of the multipart upload
     */
    public Owner getOwner() {
        return owner;
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
     * Obtain the name of the bucket to which the multipart upload belongs.
     * 
     * @return Name of the bucket to which the multipart upload belongs
     */
    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Obtain the name of the object involved in the multipart upload.
     * 
     * @return Name of the object involved in the multipart upload
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * Obtain the storage class of the object generated via the multipart
     * upload.
     * 
     * @return Storage class of the object generated via the multipart upload
     */
    @Deprecated
    public String getStorageClass() {
        return storageClass != null ? storageClass.getCode() : null;
    }

    /**
     * Obtain the storage class of the object generated via the multipart
     * upload.
     * 
     * @return Storage class of the object generated via the multipart upload
     */
    public StorageClassEnum getObjectStorageClass() {
        return storageClass;
    }

    /**
     * Obtain the creation time of the multipart upload.
     * 
     * @return Creation time of the multipart upload
     */
    public Date getInitiatedDate() {
        return ServiceUtils.cloneDateIgnoreNull(this.initiatedDate);
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

}
