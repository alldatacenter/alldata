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

package org.apache.atlas.model.audit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.AtlasBaseModelObject;

import java.io.Serializable;
import java.util.Date;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AtlasAuditEntry extends AtlasBaseModelObject implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum AuditOperation {
        PURGE("PURGE"),
        EXPORT("EXPORT"),
        IMPORT("IMPORT"),
        IMPORT_DELETE_REPL("IMPORT_DELETE_REPL"),
        TYPE_DEF_CREATE("TYPE_DEF_CREATE"),
        TYPE_DEF_UPDATE("TYPE_DEF_UPDATE"),
        TYPE_DEF_DELETE("TYPE_DEF_DELETE"),
        SERVER_START("SERVER_START"),
        SERVER_STATE_ACTIVE("SERVER_STATE_ACTIVE");

        private final String type;

        AuditOperation(String type) {
            this.type = type;
        }

        public EntityAuditEventV2.EntityAuditActionV2 toEntityAuditActionV2() throws AtlasBaseException {
            switch (this.type) {
                case "PURGE":
                    return EntityAuditEventV2.EntityAuditActionV2.ENTITY_PURGE;
                default:
                    try {
                        return EntityAuditEventV2.EntityAuditActionV2.fromString(this.type);
                    } catch (IllegalArgumentException e) {
                        throw new AtlasBaseException("Invalid operation for Entity Audit Event V2: " + this.type);
                    }
            }
        }

        public String getType() {
            return type;
        }
    }

    private String userName;
    private AuditOperation operation;
    private String params;
    private Date startTime;
    private Date endTime;
    private String clientId;
    private String result;
    private long resultCount;

    public AtlasAuditEntry() {
    }

    public AtlasAuditEntry(AuditOperation operation, String userName, String clientId) {
        this.operation = operation;
        this.userName = userName;
        this.clientId = clientId;
    }

    public AuditOperation getOperation() {
        return operation;
    }

    public void setOperation(AuditOperation operation) {
        this.operation = operation;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return this.userName;
    }

    public void setParams(String params) {
        this.params = params;
    }

    public String getParams() {
        return this.params;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getEndTime() {
        return this.endTime;
    }

    public String getClientId() {
        return this.clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public long getResultCount() {
        return resultCount;
    }

    public void setResultCount(long resultCount) {
        this.resultCount = resultCount;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        sb.append(", userName: ").append(userName);
        sb.append(", operation: ").append(operation);
        sb.append(", params: ").append(params);
        sb.append(", clientId: ").append(clientId);
        sb.append(", startTime: ").append(startTime);
        sb.append(", endTime: ").append(endTime);
        sb.append(", result: ").append(result);
        sb.append(", resultCount: ").append(resultCount);

        return sb;
    }
}
