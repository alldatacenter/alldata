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
 * Response to an object deletion request
 */
public class DeleteObjectResult extends HeaderResponse {
    private boolean deleteMarker;

    private String objectKey;

    private String versionId;

    public DeleteObjectResult(boolean deleteMarker, String versionId) {
        this.deleteMarker = deleteMarker;
        this.versionId = versionId;
    }

    public DeleteObjectResult(boolean deleteMarker, String objectKey, String versionId) {
        this.deleteMarker = deleteMarker;
        this.objectKey = objectKey;
        this.versionId = versionId;
    }

    /**
     * Check whether a versioning object has been deleted.
     * 
     * @return Identifier indicating whether the versioning object has been
     *         deleted
     */
    public boolean isDeleteMarker() {
        return deleteMarker;
    }

    /**
     * Obtain the version ID of the deleted object.
     * 
     * @return Version ID of the object
     */
    public String getVersionId() {
        return versionId;
    }

    /**
     * Obtain the name of the deleted object.
     * 
     * @return Object name
     */
    public String getObjectKey() {
        return objectKey;
    }

    @Override
    public String toString() {
        return "DeleteObjectResult [deleteMarker=" + deleteMarker + ", objectKey=" + objectKey + ", versionId="
                + versionId + "]";
    }
}
