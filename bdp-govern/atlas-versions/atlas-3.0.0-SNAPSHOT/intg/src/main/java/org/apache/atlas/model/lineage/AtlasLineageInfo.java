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
package org.apache.atlas.model.lineage;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.instance.AtlasEntityHeader;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasLineageInfo implements Serializable {
    private String                         baseEntityGuid;
    private LineageDirection               lineageDirection;
    private int                            lineageDepth;
    private Map<String, AtlasEntityHeader> guidEntityMap;
    private Set<LineageRelation>           relations;

    public AtlasLineageInfo() {}

    public enum LineageDirection { INPUT, OUTPUT, BOTH }

    /**
     * Captures lineage information for an entity instance like hive_table

     * @param baseEntityGuid guid of the lineage entity .
     * @param lineageDirection direction of lineage, can be INPUT, OUTPUT or INPUT_AND_OUTPUT
     * @param lineageDepth  lineage depth to be fetched.
     * @param guidEntityMap map of entity guid to AtlasEntityHeader (minimal entity info)
     * @param relations list of lineage relations for the entity (fromEntityId -> toEntityId)
     */
    public AtlasLineageInfo(String baseEntityGuid, Map<String, AtlasEntityHeader> guidEntityMap,
                            Set<LineageRelation> relations, LineageDirection lineageDirection, int lineageDepth) {
        this.baseEntityGuid   = baseEntityGuid;
        this.lineageDirection = lineageDirection;
        this.lineageDepth     = lineageDepth;
        this.guidEntityMap    = guidEntityMap;
        this.relations        = relations;
    }

    public String getBaseEntityGuid() {
        return baseEntityGuid;
    }

    public void setBaseEntityGuid(String baseEntityGuid) {
        this.baseEntityGuid = baseEntityGuid;
    }

    public Map<String, AtlasEntityHeader> getGuidEntityMap() {
        return guidEntityMap;
    }

    public void setGuidEntityMap(Map<String, AtlasEntityHeader> guidEntityMap) {
        this.guidEntityMap = guidEntityMap;
    }

    public Set<LineageRelation> getRelations() {
        return relations;
    }

    public void setRelations(Set<LineageRelation> relations) {
        this.relations = relations;
    }

    public LineageDirection getLineageDirection() {
        return lineageDirection;
    }

    public void setLineageDirection(LineageDirection lineageDirection) {
        this.lineageDirection = lineageDirection;
    }

    public int getLineageDepth() {
        return lineageDepth;
    }

    public void setLineageDepth(int lineageDepth) {
        this.lineageDepth = lineageDepth;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AtlasLineageInfo that = (AtlasLineageInfo) o;
        return lineageDepth == that.lineageDepth &&
                Objects.equals(baseEntityGuid, that.baseEntityGuid) &&
                lineageDirection == that.lineageDirection &&
                Objects.equals(guidEntityMap, that.guidEntityMap) &&
                Objects.equals(relations, that.relations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(baseEntityGuid, lineageDirection, lineageDepth, guidEntityMap, relations);
    }

    @Override
    public String toString() {
        return "AtlasLineageInfo{" +
                "baseEntityGuid=" + baseEntityGuid +
                ", guidEntityMap=" + guidEntityMap +
                ", relations=" + relations +
                ", lineageDirection=" + lineageDirection +
                ", lineageDepth=" + lineageDepth +
                '}';
    }

    @JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
    @JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown = true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class LineageRelation {
        private String fromEntityId;
        private String toEntityId;
        private String relationshipId;

        public LineageRelation() { }

        public LineageRelation(String fromEntityId, String toEntityId, final String relationshipId) {
            this.fromEntityId = fromEntityId;
            this.toEntityId   = toEntityId;
            this.relationshipId = relationshipId;
        }

        public String getFromEntityId() {
            return fromEntityId;
        }

        public void setFromEntityId(String fromEntityId) {
            this.fromEntityId = fromEntityId;
        }

        public String getToEntityId() {
            return toEntityId;
        }

        public void setToEntityId(String toEntityId) {
            this.toEntityId = toEntityId;
        }

        public String getRelationshipId() {
            return relationshipId;
        }

        public void setRelationshipId(final String relationshipId) {
            this.relationshipId = relationshipId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LineageRelation that = (LineageRelation) o;
            return Objects.equals(fromEntityId, that.fromEntityId) &&
                    Objects.equals(toEntityId, that.toEntityId) &&
                    Objects.equals(relationshipId, that.relationshipId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(fromEntityId, toEntityId, relationshipId);
        }

        @Override
        public String toString() {
            return "LineageRelation{" +
                    "fromEntityId='" + fromEntityId + '\'' +
                    ", toEntityId='" + toEntityId + '\'' +
                    ", relationshipId='" + relationshipId + '\'' +
                    '}';
        }
    }

}