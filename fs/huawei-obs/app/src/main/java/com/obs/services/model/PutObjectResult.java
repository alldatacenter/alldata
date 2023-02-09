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

import java.util.Map;

/**
 * Response to an object upload request
 */
public class PutObjectResult extends HeaderResponse {

    private String bucketName;

    private String objectKey;

    private String etag;

    private String versionId;

    private StorageClassEnum storageClass;

    private String objectUrl;

    public PutObjectResult(String bucketName, String objectKey, String etag, String versionId,
            StorageClassEnum storageClass, String objectUrl) {
        super();
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.etag = etag;
        this.versionId = versionId;
        this.storageClass = storageClass;
        this.objectUrl = objectUrl;
    }

    public PutObjectResult(String bucketName, String objectKey, String etag, String versionId, String objectUrl,
            Map<String, Object> responseHeaders, int statusCode) {
        super();
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.etag = etag;
        this.versionId = versionId;
        this.objectUrl = objectUrl;
        this.responseHeaders = responseHeaders;
        this.statusCode = statusCode;
    }

    /**
     * Obtain the ETag of the object.
     * 
     * @return ETag of the object
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Obtain the name of the bucket to which the object belongs.
     * 
     * @return Name of the bucket to which the object belongs
     */
    public String getBucketName() {
        return bucketName;
    }

    /**
     * Obtain the object name.
     * 
     * @return Object name
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * Obtain the object version ID.
     * 
     * @return Version ID of the object
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Obtain the object storage class.
     * 
     * @return Object storage class
     */
    public StorageClassEnum getObjectStorageClass() {
        return storageClass;
    }

    /**
     * Obtain the full path to the object.
     * 
     * @return Full path to the object
     */
    public String getObjectUrl() {
        return objectUrl;
    }

    @Override
    public String toString() {
        return "PutObjectResult [bucketName=" + bucketName + ", objectKey=" + objectKey + ", etag=" + etag
                + ", versionId=" + versionId + ", storageClass=" + storageClass + ", objectUrl=" + objectUrl + "]";
    }

}
