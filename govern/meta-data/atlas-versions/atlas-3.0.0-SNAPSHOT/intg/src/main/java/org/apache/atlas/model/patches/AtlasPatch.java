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
package org.apache.atlas.model.patches;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Display all atlas patches.
 */
@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasPatch implements Serializable {
    private String      id;
    private String      description;
    private String      type;
    private String      action;
    private String      updatedBy;
    private String      createdBy;
    private long        createdTime;
    private long        updatedTime;
    private PatchStatus status;

    public enum PatchStatus { UNKNOWN, APPLIED, SKIPPED, FAILED }

    public AtlasPatch() { }

    public AtlasPatch(String id, String patchName, String type, String action, PatchStatus status,
                      String updatedBy, String createdBy, long createdTime, long updatedTime) {
        this.id          = id;
        this.description = patchName;
        this.type        = type;
        this.action      = action;
        this.status      = status;
        this.updatedBy   = updatedBy;
        this.createdBy   = createdBy;
        this.createdTime = createdTime;
        this.updatedTime = updatedTime;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public PatchStatus getStatus() {
        return status;
    }

    public void setStatus(PatchStatus status) {
        this.status = status;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public long getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(long createdTime) {
        this.createdTime = createdTime;
    }

    public long getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(long updatedTime) {
        this.updatedTime = updatedTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtlasPatch that = (AtlasPatch) o;
        return createdTime == that.createdTime &&
                updatedTime == that.updatedTime &&
                Objects.equals(id, that.id) &&
                Objects.equals(description, that.description) &&
                Objects.equals(type, that.type) &&
                Objects.equals(action, that.action) &&
                Objects.equals(updatedBy, that.updatedBy) &&
                Objects.equals(createdBy, that.createdBy) &&
                status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, description, type, action, updatedBy, createdBy, createdTime, updatedTime, status);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AtlasPatch{");

        sb.append("id=").append(id);
        sb.append(", description='").append(description).append('\'');
        sb.append(", type='").append(type).append('\'');
        sb.append(", action='").append(action).append('\'');
        sb.append(", updatedBy='").append(updatedBy).append('\'');
        sb.append(", createdBy='").append(createdBy).append('\'');
        sb.append(", createdTime=").append(createdTime);
        sb.append(", updatedTime=").append(updatedTime);
        sb.append(", status=").append(status);
        sb.append('}');

        return sb.toString();
    }

    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasPatches implements Serializable {
        private List<AtlasPatch> patches;

        public AtlasPatches(List<AtlasPatch> patches) {
            this.patches = patches;
        }

        public AtlasPatches() {
        }

        public List<AtlasPatch> getPatches() {
            return patches;
        }

        public void setPatches(List<AtlasPatch> patches) {
            this.patches = patches;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            AtlasPatches that = (AtlasPatches) o;
            return Objects.equals(patches, that.patches);
        }

        @Override
        public int hashCode() {
            return Objects.hash(patches);
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("AtlasPatches{");
            sb.append("patches=").append(patches);
            sb.append('}');
            return sb.toString();
        }
    }
}