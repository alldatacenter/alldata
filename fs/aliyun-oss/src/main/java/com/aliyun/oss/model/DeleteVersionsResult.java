/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Successful response to {@link com.aliyun.oss.OSS#deleteObjects(DeleteObjectsRequest)}.
 *
 * @see com.aliyun.oss.OSS#deleteVersions(DeleteVersionsRequest)
 */
public class DeleteVersionsResult extends GenericResult {

    /**
     * A successfully deleted object.
     */
    static public class DeletedVersion implements Serializable {

        private static final long serialVersionUID = 4306380535649706669L;

        private String key;
        private String versionId;
        private boolean deleteMarker;
        private String deleteMarkerVersionId;

        /**
         * Gets the key that was successfully deleted.
         * @return the object name.
         */
        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        /**
         * Gets the version deleted, or null for unversioned objects.
         * @return the version id.
         */
        public String getVersionId() {
            return versionId;
        }

        public void setVersionId(String versionId) {
            this.versionId = versionId;
        }

        /**
         * Gets whether the object deleted was a delete marker.
         * @return true if it is delete marker.
         */
        public boolean isDeleteMarker() {
            return deleteMarker;
        }

        public void setDeleteMarker(boolean deleteMarker) {
            this.deleteMarker = deleteMarker;
        }

        /**
         * Gets the versionId for the delete marker that was created when
         * doing a non-versioned delete in a versioned bucket.
         * @return the the versionId of the delete marker
         */
        public String getDeleteMarkerVersionId() {
            return deleteMarkerVersionId;
        }

        public void setDeleteMarkerVersionId(String deleteMarkerVersionId) {
            this.deleteMarkerVersionId = deleteMarkerVersionId;
        }

    }

    private final List<DeletedVersion> deletedVersions = new ArrayList<DeletedVersion>();

    public DeleteVersionsResult(List<DeletedVersion> deletedVersions) {
        if (deletedVersions != null) {
            this.deletedVersions.addAll(deletedVersions);
        }
    }

    /**
     * Gets the list of successfully deleted objects from this request. If
     * {@link DeleteObjectsRequest#isQuiet()}  is true, only error responses
     * will be returned from OSS, so this list will be empty.
     *
     * @return  The {@link DeletedVersion} instance.
     */
    public List<DeletedVersion> getDeletedVersions() {
        return deletedVersions;
    }

}
