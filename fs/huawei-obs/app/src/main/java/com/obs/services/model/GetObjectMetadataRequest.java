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
 * Parameters in a request for obtaining the properties of an object
 */
public class GetObjectMetadataRequest extends BaseObjectRequest {

    {
        httpMethod = HttpMethodEnum.HEAD;
    }

    protected String versionId;

    protected SseCHeader sseCHeader;

    public GetObjectMetadataRequest() {
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     */
    public GetObjectMetadataRequest(String bucketName, String objectKey) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Version ID of the object
     */
    public GetObjectMetadataRequest(String bucketName, String objectKey, String versionId) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.versionId = versionId;
    }

    /**
     * Obtain SSE-C decryption headers.
     * 
     * @return SSE-C decryption headers
     */
    public SseCHeader getSseCHeader() {
        return sseCHeader;
    }

    /**
     * Set SSE-C decryption headers.
     * 
     * @param sseCHeader
     *            SSE-C decryption headers
     */
    public void setSseCHeader(SseCHeader sseCHeader) {
        this.sseCHeader = sseCHeader;
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
     * Set the object name.
     * 
     * @param objectKey
     *            Object name
     */
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
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
     * Set the version ID of the object.
     * 
     * @param versionId
     *            Version ID of the object
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    @Override
    public String toString() {
        return "GetObjectMetadataRequest [bucketName=" + bucketName + ", objectKey=" + objectKey + ", versionId="
                + ", isEncodeHeaders=" + encodeHeaders + versionId + ", sseCHeader=" + sseCHeader + "]";
    }

}
