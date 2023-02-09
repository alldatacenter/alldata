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
 * Response to an appendable upload request
 *
 */
public class AppendObjectResult extends HeaderResponse {

    private String bucketName;

    private String objectKey;

    private String etag;

    private long nextPosition = -1;

    private StorageClassEnum storageClass;

    private String objectUrl;

    public AppendObjectResult(String bucketName, String objectKey, String etag, long nextPosition,
            StorageClassEnum storageClass, String objectUrl) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.nextPosition = nextPosition;
        this.etag = etag;
        this.storageClass = storageClass;
        this.objectUrl = objectUrl;
    }

    /**
     * Obtain the position from which the next appending starts.
     * 
     * @return Position from which the next appending starts
     */
    public long getNextPosition() {
        return nextPosition;
    }

    /**
     * Obtain the ETag of the appended data.
     * 
     * @return ETag of the appended data
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
        return "AppendObjectResult [bucketName=" + bucketName + ", objectKey=" + objectKey + ", etag=" + etag
                + ", nextPosition=" + nextPosition + ", storageClass=" + storageClass + ", objectUrl=" + objectUrl
                + "]";
    }

}
