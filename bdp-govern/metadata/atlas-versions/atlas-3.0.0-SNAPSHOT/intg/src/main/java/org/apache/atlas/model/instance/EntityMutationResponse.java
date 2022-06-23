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


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.instance.EntityMutations.EntityOperation;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.*;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class EntityMutationResponse {

    private Map<EntityOperation, List<AtlasEntityHeader>> mutatedEntities;
    private Map<String, String>                           guidAssignments;

    public EntityMutationResponse() {
    }

    public EntityMutationResponse(final Map<EntityOperation, List<AtlasEntityHeader>> mutatedEntities) {
        this.mutatedEntities = mutatedEntities;
    }

    public Map<EntityOperation, List<AtlasEntityHeader>> getMutatedEntities() {
        return mutatedEntities;
    }

    public void setMutatedEntities(final Map<EntityOperation, List<AtlasEntityHeader>> mutatedEntities) {
        this.mutatedEntities = mutatedEntities;
    }

    public void setGuidAssignments(Map<String,String> guidAssignments) {
        this.guidAssignments = guidAssignments;
    }

    public Map<String,String> getGuidAssignments() {
        return guidAssignments;
    }


    @JsonIgnore
    public List<AtlasEntityHeader> getEntitiesByOperation(EntityOperation op) {
        if ( mutatedEntities != null) {
            return mutatedEntities.get(op);
        }
        return null;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getCreatedEntities() {
        if ( mutatedEntities != null) {
            return mutatedEntities.get(EntityOperation.CREATE);
        }
        return null;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getUpdatedEntities() {
        if ( mutatedEntities != null) {
            return mutatedEntities.get(EntityOperation.UPDATE);
        }
        return null;
    }

    public List<AtlasEntityHeader> getPartialUpdatedEntities() {
        if ( mutatedEntities != null) {
            return mutatedEntities.get(EntityOperation.PARTIAL_UPDATE);
        }
        return null;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getDeletedEntities() {
        if ( mutatedEntities != null) {
            return mutatedEntities.get(EntityOperation.DELETE);
        }
        return null;
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getPurgedEntities() {
        if ( mutatedEntities != null) {
            return mutatedEntities.get(EntityOperation.PURGE);
        }
        return null;
    }

    @JsonIgnore
    public String getPurgedEntitiesIds() {
        String                  ret = null;
        List<AtlasEntityHeader> purgedEntities = getPurgedEntities();

        if (CollectionUtils.isNotEmpty(purgedEntities)) {
            List<String> entityIds = purgedEntities.stream().map(entity -> entity.getGuid()).collect(Collectors.toList());

            ret = String.join(",", entityIds);
        }

        return  ret;
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstEntityCreated() {
        final List<AtlasEntityHeader> entitiesByOperation = getEntitiesByOperation(EntityOperation.CREATE);
        if ( entitiesByOperation != null && entitiesByOperation.size() > 0) {
            return entitiesByOperation.get(0);
        }

        return null;
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstEntityUpdated() {
        final List<AtlasEntityHeader> entitiesByOperation = getEntitiesByOperation(EntityOperation.UPDATE);

        if ( entitiesByOperation != null && entitiesByOperation.size() > 0) {
            return entitiesByOperation.get(0);
        }

        return null;
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstEntityPartialUpdated() {
        final List<AtlasEntityHeader> entitiesByOperation = getEntitiesByOperation(EntityOperation.PARTIAL_UPDATE);
        if ( entitiesByOperation != null && entitiesByOperation.size() > 0) {
            return entitiesByOperation.get(0);
        }

        return null;
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstCreatedEntityByTypeName(String typeName) {
        return getFirstEntityByType(getEntitiesByOperation(EntityOperation.CREATE), typeName);
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstDeletedEntityByTypeName(String typeName) {
        return getFirstEntityByType(getEntitiesByOperation(EntityOperation.DELETE), typeName);
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getCreatedEntitiesByTypeName(String typeName) {
        return getEntitiesByType(getEntitiesByOperation(EntityOperation.CREATE), typeName);
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getPartialUpdatedEntitiesByTypeName(String typeName) {
        return getEntitiesByType(getEntitiesByOperation(EntityOperation.PARTIAL_UPDATE), typeName);
    }

    @JsonIgnore
    public AtlasEntityHeader getCreatedEntityByTypeNameAndAttribute(String typeName, String attrName, String attrVal) {
        return getEntityByTypeAndUniqueAttribute(getEntitiesByOperation(EntityOperation.CREATE), typeName, attrName, attrVal);
    }

    @JsonIgnore

    public AtlasEntityHeader getUpdatedEntityByTypeNameAndAttribute(String typeName, String attrName, String attrVal) {
        return getEntityByTypeAndUniqueAttribute(getEntitiesByOperation(EntityOperation.UPDATE), typeName, attrName, attrVal);
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getUpdatedEntitiesByTypeName(String typeName) {
        return getEntitiesByType(getEntitiesByOperation(EntityOperation.UPDATE), typeName);
    }

    @JsonIgnore
    public List<AtlasEntityHeader> getDeletedEntitiesByTypeName(String typeName) {
        return getEntitiesByType(getEntitiesByOperation(EntityOperation.DELETE), typeName);
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstUpdatedEntityByTypeName(String typeName) {
        return getFirstEntityByType(getEntitiesByOperation(EntityOperation.UPDATE), typeName);
    }

    @JsonIgnore
    public AtlasEntityHeader getFirstPartialUpdatedEntityByTypeName(String typeName) {
        return getFirstEntityByType(getEntitiesByOperation(EntityOperation.PARTIAL_UPDATE), typeName);
    }

    @JsonIgnore
    public void addEntity(EntityOperation op, AtlasEntityHeader header) {
        // if an entity is already included in CREATE, update the header, to capture propagated classifications
        if (op == EntityOperation.UPDATE || op == EntityOperation.PARTIAL_UPDATE) {
            if (entityHeaderExists(getCreatedEntities(), header.getGuid())) {
                op = EntityOperation.CREATE;
            }
        }

        if (mutatedEntities == null) {
            mutatedEntities = new HashMap<>();
        }

        List<AtlasEntityHeader> opEntities = mutatedEntities.get(op);

        if (opEntities == null) {
            opEntities = new ArrayList<>();
            mutatedEntities.put(op, opEntities);
        }

        if (!entityHeaderExists(opEntities, header.getGuid())) {
            opEntities.add(header);
        }
    }

    private boolean entityHeaderExists(List<AtlasEntityHeader> entityHeaders, String guid) {
        boolean ret = false;

        if (CollectionUtils.isNotEmpty(entityHeaders) && guid != null) {
            for (AtlasEntityHeader entityHeader : entityHeaders) {
                if (StringUtils.equals(entityHeader.getGuid(), guid)) {
                    ret = true;
                    break;
                }
            }
        }

        return ret;
    }

    public StringBuilder toString(StringBuilder sb) {
        if ( sb == null) {
            sb = new StringBuilder();
        }

        AtlasBaseTypeDef.dumpObjects(mutatedEntities, sb);

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityMutationResponse that = (EntityMutationResponse) o;
        return Objects.equals(mutatedEntities, that.mutatedEntities) &&
               Objects.equals(guidAssignments, that.guidAssignments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mutatedEntities, guidAssignments);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    private AtlasEntityHeader getFirstEntityByType(List<AtlasEntityHeader> entitiesByOperation, String typeName) {
        if ( entitiesByOperation != null && entitiesByOperation.size() > 0) {
            for (AtlasEntityHeader header : entitiesByOperation) {
                if ( header.getTypeName().equals(typeName)) {
                    return header;
                }
            }
        }
        return null;
    }

    private List<AtlasEntityHeader> getEntitiesByType(List<AtlasEntityHeader> entitiesByOperation, String typeName) {
        List<AtlasEntityHeader> ret = new ArrayList<>();

        if ( entitiesByOperation != null && entitiesByOperation.size() > 0) {
            for (AtlasEntityHeader header : entitiesByOperation) {
                if ( header.getTypeName().equals(typeName)) {
                    ret.add(header);
                }
            }
        }
        return ret;
    }

    private AtlasEntityHeader getEntityByTypeAndUniqueAttribute(List<AtlasEntityHeader> entitiesByOperation, String typeName, String attrName, String attrVal) {
        if (entitiesByOperation != null && entitiesByOperation.size() > 0) {
            for (AtlasEntityHeader header : entitiesByOperation) {
                if (header.getTypeName().equals(typeName)) {
                    if (attrVal != null && attrVal.equals(header.getAttribute(attrName))) {
                        return header;
                    }
                }
            }
        }
        return null;
    }
}
