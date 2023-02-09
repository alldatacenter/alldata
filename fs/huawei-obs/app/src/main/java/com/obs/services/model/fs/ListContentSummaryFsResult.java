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
 */

package com.obs.services.model.fs;

import com.obs.services.model.HeaderResponse;

import java.util.List;

/**
 * Response to a request for obtaining fs folder contentSummary
 *
 */
public class ListContentSummaryFsResult extends HeaderResponse {
    private List<DirContentSummary> dirContentSummaries;

    private List<ErrorResult> errorResults;

    public List<DirContentSummary> getDirContentSummaries() {
        return dirContentSummaries;
    }

    public void setDirContentSummaries(List<DirContentSummary> dirContentSummaries) {
        this.dirContentSummaries = dirContentSummaries;
    }

    public List<ErrorResult> getErrorResults() {
        return errorResults;
    }

    public void setErrorResults(List<ErrorResult> errorResults) {
        this.errorResults = errorResults;
    }

    /**
     * Results returned if the deletion fails
     */
    public static class ErrorResult {
        private String key;

        private String statusCode;

        private String errorCode;

        private String message;

        private long inode;

        /**
         * Dir returned after a deletion failure
         *
         * @return Dir returned after a deletion failure
         */
        public String getKey() {
            return key;
        }

        /**
         * Error code returned after a deletion failure
         *
         * @return Error code returned after a deletion failure
         */
        public String getErrorCode() {
            return errorCode;
        }

        public String getStatusCode() {
            return statusCode;
        }

        /**
         * Obtain the error description returned after a deletion failure.
         *
         * @return Error information returned after a deletion failure
         */
        public String getMessage() {
            return message;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public void setStatusCode(String statusCode) {
            this.statusCode = statusCode;
        }

        public void setErrorCode(String errorCode) {
            this.errorCode = errorCode;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public long getInode() {
            return inode;
        }

        public void setInode(long inode) {
            this.inode = inode;
        }

        @Override
        public String toString() {
            return "ErrorResult{" +
                    "key='" + key + '\'' +
                    ", statusCode='" + statusCode + '\'' +
                    ", errorCode='" + errorCode + '\'' +
                    ", message='" + message + '\'' +
                    ", inode=" + inode +
                    '}';
        }
    }
}
