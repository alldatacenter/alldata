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

import java.io.InputStream;

/**
 * Response to a request for combining parts
 */
public class CompleteMultipartUploadResult extends HeaderResponse {
    private String bucketName;

    private String objectKey;

    private String etag;

    private String location;

    private String versionId;

    private String objectUrl;

    private String encodingType;

    private InputStream callbackResponseBody;

    public CompleteMultipartUploadResult(String bucketName, String objectKey, String etag, String location,
            String versionId, String objectUrl) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.etag = etag;
        this.location = location;
        this.versionId = versionId;
        this.objectUrl = objectUrl;
    }

    /**
     * Obtain the name of the bucket to which the multipart upload belongs.
     * 
     * @return Name of the bucket to which the multipart upload belongs
     */
    public String getBucketName() {
        return bucketName;
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
     * Obtain the ETag of the object involved in the multipart upload.
     * 
     * @return ETag of the object involved in the multipart upload
     */
    public String getEtag() {
        return etag;
    }

    /**
     * Obtain the URI of the object after part combination.
     * 
     * @return URI of the object obtained after part combination
     */
    public String getLocation() {
        return location;
    }

    /**
     * Obtain the version ID of the object after part combination.
     * 
     * @return Version ID of the object after part combination
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Obtain the full path to the object after part combination.
     * 
     * @return Full path to the object
     */
    public String getObjectUrl() {
        return objectUrl;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    /**
     *  Set encoding type for objectKey in xml
     * @param encodingType encoding type for objectKey
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     *  Get encoding type for objectKey in xml
     * @return encodingType
     */
    public String getEncodingType() {
        return encodingType;
    }

    public InputStream getCallbackResponseBody() {
        return callbackResponseBody;
    }

    public void setCallbackResponseBody(InputStream callbackResponseBody) {
        this.callbackResponseBody = callbackResponseBody;
    }

    @Override
    public String toString() {
        return "CompleteMultipartUploadResult [bucketName=" + bucketName + ", objectKey=" + objectKey + ", etag=" + etag
                + ", location=" + location + ", versionId=" + versionId + ", objectUrl=" + objectUrl + "]";
    }

}
