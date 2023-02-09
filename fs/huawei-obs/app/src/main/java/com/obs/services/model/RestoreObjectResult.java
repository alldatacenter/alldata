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
 * Response to the request for restoring Archive objects
 */
public class RestoreObjectResult extends HeaderResponse {

    private String bucketName;
    private String objectKey;
    private String versionId;


    public RestoreObjectResult(String bucketName, String objectKey, String versionId) {
        this.bucketName = bucketName;
        this.objectKey = objectKey;
        this.versionId = versionId;
    }
    
    public String getBucketName() {
        return bucketName;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public String getVersionId() {
        return versionId;
    }    
}
