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
import java.util.HashMap;
import java.util.Map;

import com.obs.services.internal.utils.ServiceUtils;


public class S3Bucket extends HeaderResponse {

    @Deprecated
    public static final String STANDARD = "STANDARD";

    @Deprecated
    public static final String STANDARD_IA = "STANDARD_IA";

    @Deprecated
    public static final String GLACIER = "GLACIER";

    protected String bucketName;

    protected Owner owner;

    protected Date creationDate;

    protected String location;

    protected StorageClassEnum storageClass;

    protected Map<String, Object> metadata = new HashMap<String, Object>();

    protected AccessControlList acl;

    protected BucketTypeEnum bucketTypeEnum;

    public S3Bucket() {

    }

    public S3Bucket(String bucketName, String location) {
        this.bucketName = bucketName;
        this.location = location;
    }

    public String getBucketName() {
        return bucketName;
    }

    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    public Owner getOwner() {
        return owner;
    }

    public void setOwner(Owner bucketOwner) {
        this.owner = bucketOwner;
    }

    public Date getCreationDate() {
        return ServiceUtils.cloneDateIgnoreNull(this.creationDate);
    }

    public void setCreationDate(Date bucketCreationDate) {
        this.creationDate = ServiceUtils.cloneDateIgnoreNull(bucketCreationDate);
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata.putAll(metadata);
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public AccessControlList getAcl() {
        return acl;
    }

    public void setAcl(AccessControlList acl) {
        this.acl = acl;
    }

    @Deprecated
    public String getStorageClass() {
        return this.storageClass != null ? this.storageClass.getCode() : null;
    }

    @Deprecated
    public void setStorageClass(String storageClass) {
        this.storageClass = StorageClassEnum.getValueFromCode(storageClass);
    }

    public StorageClassEnum getBucketStorageClass() {
        return storageClass;
    }

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
