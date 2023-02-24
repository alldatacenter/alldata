/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.bulkimport;

import org.apache.atlas.model.annotation.AtlasJSON;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.apache.atlas.bulkimport.BulkImportResponse.ImportStatus.SUCCESS;

@AtlasJSON
public class BulkImportResponse implements Serializable {
    private List<ImportInfo> failedImportInfoList = new ArrayList<>();
    private List<ImportInfo> successImportInfoList = new ArrayList<>();

    public BulkImportResponse() {}

    public List<ImportInfo> getFailedImportInfoList() {
        return failedImportInfoList;
    }

    public void setFailedImportInfoList(List<ImportInfo> failedImportInfoList) {
        this.failedImportInfoList = failedImportInfoList;
    }

    public void addToFailedImportInfoList(ImportInfo importInfo) {
        if (this.failedImportInfoList == null) {
            this.failedImportInfoList = new ArrayList<>();
        }

        this.failedImportInfoList.add(importInfo);
    }

    public List<ImportInfo> getSuccessImportInfoList() {
        return successImportInfoList;
    }

    public void setSuccessImportInfoList(List<ImportInfo> successImportInfoList) {
        this.successImportInfoList = successImportInfoList;
    }

    public void addToSuccessImportInfoList(ImportInfo importInfo) {
        if (successImportInfoList == null) {
            successImportInfoList = new ArrayList<>();
        }

        successImportInfoList.add(importInfo);
    }

    public enum ImportStatus {
        SUCCESS, FAILED
    }

    @Override
    public String toString() {
        return "BulkImportResponse{" +
                "failedImportInfoList=" + failedImportInfoList +
                ", successImportInfoList=" + successImportInfoList +
                '}';
    }


    public static class ImportInfo {
        private String parentObjectName;
        private String childObjectName;
        private ImportStatus importStatus;
        private String remarks;
        private Integer rowNumber;

        public ImportInfo(){ }

        public ImportInfo(String parentObjectName, String childObjectName, ImportStatus importStatus, String remarks, Integer rowNumber) {
            this.parentObjectName = parentObjectName;
            this.childObjectName = childObjectName;
            this.importStatus = importStatus;
            this.remarks = remarks;
            this.rowNumber = rowNumber;
        }

        public ImportInfo(String parentObjectName, String childObjectName, ImportStatus importStatus) {
            this(parentObjectName, childObjectName, importStatus, "", -1);
        }

        public ImportInfo(ImportStatus importStatus, String remarks) {
            this("", "", importStatus, remarks, -1);
        }

        public ImportInfo(ImportStatus importStatus, String remarks, Integer rowNumber) {
            this("", "", importStatus, remarks, rowNumber);
        }

        public ImportInfo(String parentObjectName, String childObjectName) {
            this(parentObjectName, childObjectName, SUCCESS, "", -1);
        }

        public ImportInfo(String parentObjectName, String childObjectName, ImportStatus importStatus, String remarks) {
            this(parentObjectName, childObjectName, importStatus, remarks, -1);
        }

        public String getParentObjectName() {
            return parentObjectName;
        }

        public void setParentObjectName(String parentObjectName) {
            this.parentObjectName = parentObjectName;
        }

        public String getChildObjectName() {
            return childObjectName;
        }

        public void setChildObjectName(String childObjectName) {
            this.childObjectName = childObjectName;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public ImportStatus getImportStatus() {
            return importStatus;
        }

        public void setImportStatus(ImportStatus importStatus) {
            this.importStatus = importStatus;
        }

        @Override
        public String toString() {
            return "ImportInfo{" +
                    "parentObjectName='" + parentObjectName + '\'' +
                    ", childObjectName='" + childObjectName + '\'' +
                    ", remarks='" + remarks + '\'' +
                    ", importStatus=" + importStatus +
                    ", rowNumber=" + rowNumber +
                    '}';
        }
    }
}