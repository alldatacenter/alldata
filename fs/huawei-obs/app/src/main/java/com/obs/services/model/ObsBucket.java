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
import java.util.HashMap;
import java.util.Map;

import com.obs.services.internal.utils.ServiceUtils;

/**
 * Buckets in OBS
 * 
 */
public class ObsBucket extends S3Bucket {

    public ObsBucket() {

    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param location
     *            Bucket location
     */
    public ObsBucket(String bucketName, String location) {
        this.bucketName = bucketName;
        this.location = location;
    }

    /**
     * Obtain the bucket name.
     * 
     * @return Bucket name
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Set the bucket name. The value can contain only lowercase letters,
     * digits, hyphens (-), and periods (.).
     * 
     * @param bucketName
     *            Bucket name
     */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /**
     * Obtain the owner of the bucket.
     * 
     * @return Owner of the bucket
     */
    public Owner getOwner() {
        return owner;
    }

    /**
     * Set the owner of the bucket.
     * 
     * @param bucketOwner
     *            Owner of the bucket
     */
    public void setOwner(Owner bucketOwner) {
        this.owner = bucketOwner;
    }

    /**
     * Obtain the creation time of the bucket.
     * 
     * @return Creation time of the bucket
     */
    public Date getCreationDate() {
        return ServiceUtils.cloneDateIgnoreNull(this.creationDate);
    }

    /**
     * Set the creation time of the bucket.
     * 
     * @param bucketCreationDate
     *            Creation time of the bucket
     */
    public void setCreationDate(Date bucketCreationDate) {
        this.creationDate = ServiceUtils.cloneDateIgnoreNull(bucketCreationDate);
    }

    /**
     * Obtain bucket properties.
     * 
     * @return Bucket properties
     */
    @Deprecated
    public Map<String, Object> getMetadata() {
        if (this.metadata == null) {
            this.metadata = new HashMap<String, Object>();
        }
        return metadata;
    }

    /**
     * Set bucket properties.
     * 
     * @param metadata
     *            Bucket properties
     */
    @Deprecated
    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    /**
     * Obtain the bucket location.
     * 
     * @return Bucket location
     */
    public String getLocation() {
        return location;
    }

    /**
     * Set the bucket location.
     * 
     * @param location
     *            Bucket location. This parameter is mandatory unless the
     *            endpoint belongs to the default region.
     */
    public void setLocation(String location) {
        this.location = location;
    }

    /**
     * Obtain the bucket ACL.
     * 
     * @return Bucket ACL
     */
    public AccessControlList getAcl() {
        return acl;
    }

    /**
     * Set the bucket ACL.
     * 
     * @param acl
     *            Bucket ACL
     */
    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    /**
     * Obtain the bucket storage class.
     * 
     * @return Bucket storage class
     */
    @Deprecated
    public String getStorageClass() {
        return this.storageClass != null ? this.storageClass.getCode() : null;
    }

    /**
     * Set the bucket storage class.
     * 
     * @param storageClass
     *            Bucket storage class
     */
    @Deprecated
    public void setStorageClass(String storageClass) {
        this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
    }

    public BucketTypeEnum getBucketType() {
        return bucketTypeEnum;
    }

    public void setBucketType(BucketTypeEnum bucketTypeEnum) {
        this.bucketTypeEnum = bucketTypeEnum;
    }

    public StorageClassEnum getBucketStorageClass() {
        return storageClass;
    }

    /**
     * Set the bucket storage class.
     * 
     * @param storageClass
     *            Bucket storage class
     */
    public void setBucketStorageClass(StorageClassEnum storageClass) {
        this.storageClass = storageClass;
    }

    @Override
    public String toString() {
        return "ObsBucket [bucketName=" + bucketName + ", owner=" + owner + ", creationDate=" + creationDate
                + ", location=" + location + ", storageClass=" + storageClass + ", metadata=" + metadata + ", acl="
                + acl + "]";
    }
}
