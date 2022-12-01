/**
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
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.model.impexp;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.AtlasBaseModelObject;

import java.io.Serializable;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExportImportAuditEntry extends AtlasBaseModelObject implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final String OPERATION_EXPORT = "EXPORT";
    public static final String OPERATION_IMPORT = "IMPORT";
    public static final String OPERATION_IMPORT_DELETE_REPL = "IMPORT_DELETE_REPL";

    private String userName;
    private String operation;
    private String operationParams;
    private long startTime;
    private long endTime;
    private String resultSummary;
    private String sourceClusterName;
    private String targetClusterName;

    public ExportImportAuditEntry() {

    }

    public ExportImportAuditEntry(String sourceClusterName, String operation) {
        this.sourceClusterName = sourceClusterName;
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return this.userName;
    }
    public void setOperationParams(String operationParams) {
        this.operationParams = operationParams;
    }

    public String getOperationParams() {
        return this.operationParams;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public long getEndTime() {
        return this.endTime;
    }

    public String getTargetServerName() {
        return this.targetClusterName;
    }

    public String getSourceServerName() {
        return this.sourceClusterName;
    }

    public void setSourceServerName(String sourceClusterName) {
        this.sourceClusterName = sourceClusterName;
    }

    public void setTargetServerName(String targetClusterName) {
        this.targetClusterName = targetClusterName;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(", userName: ").append(userName);
        sb.append(", operation: ").append(operation);
        sb.append(", operationParams: ").append(operationParams);
        sb.append(", sourceClusterName: ").append(sourceClusterName);
        sb.append(", targetClusterName: ").append(targetClusterName);
        sb.append(", startTime: ").append(startTime);
        sb.append(", endTime: ").append(endTime);
        sb.append(", resultSummary: ").append(resultSummary);

        return sb;
    }
}
