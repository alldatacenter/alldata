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
 * Response to an object batch deletion request
 */
public class DeleteObjectsResult extends HeaderResponse {
    private List<DeleteObjectResult> deletedObjectResults;

    private List<ErrorResult> errorResults;

    public DeleteObjectsResult() {

    }

    public DeleteObjectsResult(List<DeleteObjectResult> deletedObjectResults, List<ErrorResult> errorResults) {
        this.deletedObjectResults = deletedObjectResults;
        this.errorResults = errorResults;
    }

    /**
     * Obtain the list of objects that have been deleted successfully.
     * 
     * @return List of successfully deleted objects
     */
    public List<DeleteObjectResult> getDeletedObjectResults() {
        if (this.deletedObjectResults == null) {
            this.deletedObjectResults = new ArrayList<DeleteObjectResult>();
        }
        return deletedObjectResults;
    }

    /**
     * Obtain the list of objects failed to be deleted.
     * 
     * @return List of objects failed to be deleted
     */
    public List<ErrorResult> getErrorResults() {
        if (this.errorResults == null) {
            this.errorResults = new ArrayList<ErrorResult>();
        }
        return errorResults;
    }

    /**
     * Results returned if the deletion succeeds
     */
    public static class DeleteObjectResult {
        private String objectKey;

        private String version;

        private boolean deleteMarker;

        private String deleteMarkerVersion;

        public DeleteObjectResult(String objectKey, String version, boolean deleteMarker, String deleteMarkerVersion) {
            super();
            this.objectKey = objectKey;
            this.version = version;
            this.deleteMarker = deleteMarker;
            this.deleteMarkerVersion = deleteMarkerVersion;
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
        public String getVersion() {
            return version;
        }

        /**
         * Check whether the deleted object is a delete marker
         * 
         * @return Identifier specifying whether the object is a delete marker
         */
        public boolean isDeleteMarker() {
            return deleteMarker;
        }

        /**
         * Obtain the version ID of the delete marker.
         * 
         * @return Version ID of the delete marker
         */
        public String getDeleteMarkerVersion() {
            return deleteMarkerVersion;
        }

        public void setObjectKey(String objectKey) {
            this.objectKey = objectKey;
        }

        @Override
        public String toString() {
            return "DeleteObjectResult [objectKey=" + objectKey + ", version=" + version + ", deleteMarker="
                    + deleteMarker + ", deleteMarkerVersion=" + deleteMarkerVersion + "]";
        }

    }

    /**
     * Results returned if the deletion fails
     */
    public static class ErrorResult {
        private String objectKey;

        private String version;

        private String errorCode;

        private String message;

        /**
         * Constructor
         *
         * @param objectKey
         *            Name of the object that fails to be deleted
         * @param version
         *            Version ID of the object that fails to be deleted
         * @param errorCode
         *            Error code returned after a deletion failure
         * @param message
         *            Error information returned after a deletion failure
         */
        public ErrorResult(String objectKey, String version, String errorCode, String message) {
            this.objectKey = objectKey;
            this.version = version;
            this.errorCode = errorCode;
            this.message = message;
        }

        /**
         * Obtain the name of the object that fails to be deleted.
         * 
         * @return Name of the object that fails to be deleted
         */
        public String getObjectKey() {
            return objectKey;
        }

        /**
         * Obtain the version ID of the object that fails to be deleted.
         * 
         * @return Version ID of the object that fails to be deleted
         */
        public String getVersion() {
            return version;
        }

        /**
         * Error code returned after a deletion failure
         * 
         * @return Error code returned after a deletion failure
         */
        public String getErrorCode() {
            return errorCode;
        }

        /**
         * Obtain the error description returned after a deletion failure.
         * 
         * @return Error information returned after a deletion failure
         */
        public String getMessage() {
            return message;
        }

        public void setObjectKey(String objectKey) {
            this.objectKey = objectKey;
        }

        @Override
        public String toString() {
            return "ErrorResult [objectKey=" + objectKey + ", version=" + version + ", errorCode=" + errorCode
                    + ", message=" + message + "]";
        }

    }

    @Override
    public String toString() {
        return "DeleteObjectsResult [deletedObjectResults=" + deletedObjectResults + ", errorResults=" + errorResults
                + "]";
    }

}
