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
 * Parameters in a request for obtaining object ACLs
 * 
 * @since 3.20.3
 */
public class GetObjectAclRequest extends BaseObjectRequest {

    {
        httpMethod = HttpMethodEnum.GET;
    }
    
    private String versionId;

    public GetObjectAclRequest() {

    }

    /**
     * Constructor
     * 
     * @param bucketName
     *            Bucket name
     * @param objectKey
     *            Object name
     * @param versionId
     *            Object version ID
     */
    public GetObjectAclRequest(String bucketName, String objectKey, String versionId) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.versionId = versionId;
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
     *            Object version ID
     */
    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    /**
     * Obtain the name of the object.
     * 
     * @return Object name
     */
    public String getObjectKey() {
        return objectKey;
    }

    /**
     * Set a name for an object.
     * 
     * @param objectKey
     *            Object name
     */
    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    @Override
    public String toString() {
        return "AbortMultipartUploadRequest [bucketName=" + bucketName + ", objectKey="
                + objectKey + ", versionId=" + versionId + "]";
    }

}
