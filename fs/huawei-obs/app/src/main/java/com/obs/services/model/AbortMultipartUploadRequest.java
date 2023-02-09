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

/**
 * Parameters in a request for aborting an multipart upload
 */
public class AbortMultipartUploadRequest extends AbstractMultipartRequest {
    public AbortMultipartUploadRequest() {
        super();
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param uploadId
     *            Multipart upload ID
     */
    public AbortMultipartUploadRequest(String bucketName, String objectKey, String uploadId) {
        super();
        this.setBucketName(bucketName);
        this.setObjectKey(objectKey);
        this.setUploadId(uploadId);
    }

    @Override
    public String toString() {
        return "AbortMultipartUploadRequest [uploadId=" + this.getUploadId() + ", bucketName=" 
                + this.getBucketName() + ", objectKey="
                + this.getObjectKey() + "]";
    }

}
