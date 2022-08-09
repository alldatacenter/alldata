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
package org.apache.atlas.model.instance;

import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * Result of Atlas state check run.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasCheckStateResult implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum State { OK, FIXED, PARTIALLY_FIXED, NOT_FIXED}

    private int                           entitiesScanned        = 0;
    private int                           entitiesOk             = 0;
    private int                           entitiesFixed          = 0;
    private int                           entitiesPartiallyFixed = 0;
    private int                           entitiesNotFixed       = 0;
    private State                         state                  = State.OK;
    private Map<String, AtlasEntityState> entities               = null;


    public AtlasCheckStateResult() {
    }

    public int getEntitiesScanned() {
        return entitiesScanned;
    }

    public void setEntitiesScanned(int entitiesScanned) {
        this.entitiesScanned = entitiesScanned;
    }

    public void incrEntitiesScanned() { entitiesScanned++; }

    public int getEntitiesOk() {
        return entitiesOk;
    }

    public void setEntitiesOk(int entitiesOk) {
        this.entitiesOk = entitiesOk;
    }

    public void incrEntitiesOk() { entitiesOk++; }

    public int getEntitiesFixed() {
        return entitiesFixed;
    }

    public void setEntitiesFixed(int entitiesFixed) {
        this.entitiesFixed = entitiesFixed;
    }

    public void incrEntitiesFixed() { entitiesFixed++; }

    public int getEntitiesPartiallyFixed() {
        return entitiesPartiallyFixed;
    }

    public void setEntitiesPartiallyFixed(int entitiesPartiallyFixed) {
        this.entitiesPartiallyFixed = entitiesPartiallyFixed;
    }

    public void incrEntitiesPartiallyFixed() { entitiesPartiallyFixed++; }

    public int getEntitiesNotFixed() {
        return entitiesNotFixed;
    }

    public void setEntitiesNotFixed(int entitiesNotFixed) {
        this.entitiesNotFixed = entitiesNotFixed;
    }

    public void incrEntitiesNotFixed() { entitiesNotFixed++; }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Map<String, AtlasEntityState> getEntities() {
        return entities;
    }

    public void setEntities(Map<String, AtlasEntityState> entities) {
        this.entities = entities;
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasCheckStateResult{");
        sb.append("entitiesScanned='").append(entitiesScanned);
        sb.append(", entitiesFixed=").append(entitiesFixed);
        sb.append(", entitiesPartiallyFixed=").append(entitiesPartiallyFixed);
        sb.append(", entitiesNotFixed=").append(entitiesNotFixed);
        sb.append(", state=").append(state);

        sb.append("entities=[");
        if (entities != null) {
            boolean isFirst = true;
            for (Map.Entry<String, AtlasEntityState> entry : entities.entrySet()) {
                if (isFirst) {
                    isFirst = false;
                } else {
                    sb.append(",");
                }

                sb.append(entry.getKey()).append(":");
                entry.getValue().toString(sb);
            }
        }
        sb.append("]");

        sb.append("}");

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasEntityState implements Serializable {
        private static final long serialVersionUID = 1L;

        private String             guid;
        private String             typeName;
        private String             name;
        private AtlasEntity.Status status;
        private State              state = State.OK;
        private List<String>       issues;


        public AtlasEntityState() {
        }

        public String getGuid() {
            return guid;
        }

        public void setGuid(String guid) {
            this.guid = guid;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public AtlasEntity.Status getStatus() {
            return status;
        }

        public void setStatus(AtlasEntity.Status status) {
            this.status = status;
        }


        public State getState() {
            return state;
        }

        public void setState(State state) {
            this.state = state;
        }

        public List<String> getIssues() {
            return issues;
        }

        public void setIssues(List<String> issues) {
            this.issues = issues;
        }

        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("AtlasEntityState{");
            sb.append("guid=").append(guid);
            sb.append(", typeName=").append(typeName);
            sb.append(", name=").append(name);
            sb.append(", status=").append(status);
            sb.append(", state=").append(state);
            sb.append(", issues=[");
            AtlasBaseTypeDef.dumpObjects(issues, sb);
            sb.append("]");
            sb.append("}");

            return sb;
        }

        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }
    }
}
