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

import java.util.ArrayList;
import java.util.List;

/**
 * Parameters in a request for combining parts
 */
public class CompleteMultipartUploadRequest extends AbstractMultipartRequest {

    {
        httpMethod = HttpMethodEnum.POST;
    }
    
    private List<PartEtag> partEtag;

    private String encodingType;

    private Callback callback;

    public CompleteMultipartUploadRequest() {
    }

    public CompleteMultipartUploadRequest(String bucketName) {
        this.bucketName = bucketName;
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
     * @param partEtag
     *            List of parts to be combined
     */
    public CompleteMultipartUploadRequest(String bucketName, String objectKey, String uploadId,
                                          List<PartEtag> partEtag) {
        super();
        this.setUploadId(uploadId);
        this.setBucketName(bucketName);
        this.setObjectKey(objectKey);
        this.partEtag = partEtag;
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
     * @param partEtag
     *            List of parts to be combined
     * @param encodingType
     *            Use this encoding type to encode keys that contains invalid characters, the value could be "url"
     */
    public CompleteMultipartUploadRequest(String bucketName, String objectKey, String uploadId,
                                          List<PartEtag> partEtag, String encodingType) {
        super();
        this.setUploadId(uploadId);
        this.setBucketName(bucketName);
        this.setObjectKey(objectKey);
        this.partEtag = partEtag;
        this.encodingType = encodingType;
    }

    /**
     * Obtain the to-be-combined part list.
     *
     * @return List of parts to be combined
     */
    public List<PartEtag> getPartEtag() {
        if (this.partEtag == null) {
            this.partEtag = new ArrayList<PartEtag>();
        }
        return this.partEtag;
    }

    /**
     * Set the to-be-combined part list.
     *
     * @param partEtags
     *            List of parts to be combined
     */
    public void setPartEtag(List<PartEtag> partEtags) {
        this.partEtag = partEtags;
    }

    /**
     * Set encoding type to encode objectkeys, the value could be url
     *
     * @param encodingType
     *            encoding type for encoding
     */
    public void setEncodingType(String encodingType) {
        this.encodingType = encodingType;
    }

    /**
     * Get encoding type to encode objectkeys
     * @return encoding type for encoding
     */
    public String getEncodingType() {
        return encodingType;
    }

    public Callback getCallback() {
        return callback;
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    @Override
    public String toString() {
        return "CompleteMultipartUploadRequest [uploadId=" + this.getUploadId()
                + ", bucketName=" + this.getBucketName() + ", objectKey="
                + this.getObjectKey() + ", partEtag=" + partEtag + ", encodingType=" + encodingType + "]";
    }

}
