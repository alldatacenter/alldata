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

import java.util.ArrayList;
import java.util.List;

/**
 * Response to a request for listing uploaded parts
 */
public class ListPartsResult extends HeaderResponse {
    private String bucket;

    private String key;

    private String uploadId;

    private Owner initiator;

    private Owner owner;

    private StorageClassEnum storageClass;

    private List<Multipart> multipartList;

    private Integer maxParts;

    private boolean isTruncated;

    private String partNumberMarker;

    private String nextPartNumberMarker;

    @Deprecated
    //CHECKSTYLE:OFF
    public ListPartsResult(String bucket, String key, String uploadId, Owner initiator, Owner owner,
            StorageClassEnum storageClass, List<Multipart> multipartList, Integer maxParts, boolean isTruncated,
            String partNumberMarker, String nextPartNumberMarker) {
        super();
        this.bucket = bucket;
        this.key = key;
        this.uploadId = uploadId;
        this.initiator = initiator;
        this.owner = owner;
        this.storageClass = storageClass;
        this.multipartList = multipartList;
        this.maxParts = maxParts;
        this.isTruncated = isTruncated;
        this.partNumberMarker = partNumberMarker;
        this.nextPartNumberMarker = nextPartNumberMarker;
    }

    private ListPartsResult(Builder builder) {
        super();
        this.bucket = builder.bucket;
        this.key = builder.key;
        this.uploadId = builder.uploadId;
        this.initiator = builder.initiator;
        this.owner = builder.owner;
        this.storageClass = builder.storageClass;
        this.multipartList = builder.multipartList;
        this.maxParts = builder.maxParts;
        this.isTruncated = builder.isTruncated;
        this.partNumberMarker = builder.partNumberMarker;
        this.nextPartNumberMarker = builder.nextPartNumberMarker;
    }
    
    public static final class Builder {
        private String bucket;
        private String key;
        private String uploadId;
        private Owner initiator;
        private Owner owner;
        private StorageClassEnum storageClass;
        private List<Multipart> multipartList;
        private Integer maxParts;
        private boolean isTruncated;
        private String partNumberMarker;
        private String nextPartNumberMarker;
        
        public Builder bucket(String bucket) {
            this.bucket = bucket;
            return this;
        }
        
        public Builder key(String key) {
            this.key = key;
            return this;
        }
        
        public Builder uploadId(String uploadId) {
            this.uploadId = uploadId;
            return this;
        }
        
        public Builder initiator(Owner initiator) {
            this.initiator = initiator;
            return this;
        }
        
        public Builder owner(Owner owner) {
            this.owner = owner;
            return this;
        }
        
        public Builder storageClass(StorageClassEnum storageClass) {
            this.storageClass = storageClass;
            return this;
        }
        
        public Builder multipartList(List<Multipart> multipartList) {
            this.multipartList = multipartList;
            return this;
        }
        
        public Builder maxParts(Integer maxParts) {
            this.maxParts = maxParts;
            return this;
        }
        
        public Builder isTruncated(boolean isTruncated) {
            this.isTruncated = isTruncated;
            return this;
        }
        
        public Builder partNumberMarker(String partNumberMarker) {
            this.partNumberMarker = partNumberMarker;
            return this;
        }
        
        public Builder nextPartNumberMarker(String nextPartNumberMarker) {
            this.nextPartNumberMarker = nextPartNumberMarker;
            return this;
        }
        
        public ListPartsResult builder() {
            return new ListPartsResult(this);
        }
    }
    
    /**
     * Obtain the name of the bucket to which the multipart upload belongs.
     * 
     * @return Name of the bucket to which the multipart upload belongs
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Obtain the name of the object involved in the multipart upload.
     * 
     * @return Name of the object involved in the multipart upload
     */
    public String getKey() {
        return key;
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
     * Obtain the storage class of the object involved in the multipart upload.
     * 
     * @return Storage class of the object involved in the multipart upload
     */
    @Deprecated
    public String getStorageClass() {
        return this.storageClass == null ? null : this.storageClass.getCode();
    }

    /**
     * Obtain the storage class of the object involved in the multipart upload.
     * 
     * @return Storage class of the object involved in the multipart upload
     */
    public StorageClassEnum getObjectStorageClass() {
        return storageClass;
    }

    /**
     * Obtain the maximum number of uploaded parts to be listed.
     * 
     * @return Maximum number of uploaded parts to be listed
     */
    public Integer getMaxParts() {
        return maxParts;
    }

    /**
     * Obtain the list of uploaded parts.
     * 
     * @return List of uploaded parts
     */
    public List<Multipart> getMultipartList() {
        if (this.multipartList == null) {
            this.multipartList = new ArrayList<Multipart>();
        }
        return multipartList;
    }

    /**
     * Check whether the query result list is truncated. Value "true" indicates
     * that the results are incomplete while value "false" indicates that the
     * results are complete.
     * 
     * @return Truncation identifier
     */
    public boolean isTruncated() {
        return isTruncated;
    }

    /**
     * Obtain the start position for listing parts.
     * 
     * @return Start position for listing parts
     */
    public String getPartNumberMarker() {
        return partNumberMarker;
    }

    /**
     * Obtain the start position for next listing.
     * 
     * @return Start position for next listing
     */
    public String getNextPartNumberMarker() {
        return nextPartNumberMarker;
    }

    @Override
    public String toString() {
        return "ListPartsResult [bucket=" + bucket + ", key=" + key + ", uploadId=" + uploadId + ", initiator="
                + initiator + ", owner=" + owner + ", storageClass=" + storageClass + ", multipartList=" + multipartList
                + ", maxParts=" + maxParts + ", isTruncated=" + isTruncated + ", partNumberMarker=" + partNumberMarker
                + ", nextPartNumberMarker=" + nextPartNumberMarker + "]";
    }

}
