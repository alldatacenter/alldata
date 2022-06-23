/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.model.typedef;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.collections.CollectionUtils;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasTypesDef {
    private List<AtlasEnumDef>             enumDefs;
    private List<AtlasStructDef>           structDefs;
    private List<AtlasClassificationDef>   classificationDefs;
    private List<AtlasEntityDef>           entityDefs;
    private List<AtlasRelationshipDef>     relationshipDefs;
    private List<AtlasBusinessMetadataDef> businessMetadataDefs;

    public AtlasTypesDef() {
        enumDefs             = new ArrayList<>();
        structDefs           = new ArrayList<>();
        classificationDefs   = new ArrayList<>();
        entityDefs           = new ArrayList<>();
        relationshipDefs     = new ArrayList<>();
        businessMetadataDefs = new ArrayList<>();
    }

    /**
     * tolerate typeDef creations that do not contain relationshipDefs, so that
     * the older calls will still work.
     * @param enumDefs
     * @param structDefs
     * @param classificationDefs
     * @param entityDefs
     */
    public AtlasTypesDef(List<AtlasEnumDef> enumDefs, List<AtlasStructDef> structDefs,
                         List<AtlasClassificationDef> classificationDefs, List<AtlasEntityDef> entityDefs) {
       this(enumDefs, structDefs, classificationDefs, entityDefs, new ArrayList<>(), new ArrayList<>());
    }
    /**
     * Create the TypesDef. This created definitions for each of the types.
     * @param enumDefs
     * @param structDefs
     * @param classificationDefs
     * @param entityDefs
     * @param relationshipDefs
     */
    public AtlasTypesDef(List<AtlasEnumDef>           enumDefs,
                         List<AtlasStructDef>         structDefs,
                         List<AtlasClassificationDef> classificationDefs,
                         List<AtlasEntityDef>         entityDefs,
                         List<AtlasRelationshipDef>   relationshipDefs) {
        this(enumDefs, structDefs, classificationDefs, entityDefs, relationshipDefs, new ArrayList<>());
    }

    public AtlasTypesDef(List<AtlasEnumDef>             enumDefs,
                         List<AtlasStructDef>           structDefs,
                         List<AtlasClassificationDef>   classificationDefs,
                         List<AtlasEntityDef>           entityDefs,
                         List<AtlasRelationshipDef>     relationshipDefs,
                         List<AtlasBusinessMetadataDef> businessMetadataDefs) {
        this.enumDefs             = enumDefs;
        this.structDefs           = structDefs;
        this.classificationDefs   = classificationDefs;
        this.entityDefs           = entityDefs;
        this.relationshipDefs     = relationshipDefs;
        this.businessMetadataDefs = businessMetadataDefs;
    }

    public List<AtlasEnumDef> getEnumDefs() {
        return enumDefs;
    }
    public void setEnumDefs(List<AtlasEnumDef> enumDefs) {
        this.enumDefs = enumDefs;
    }

    public List<AtlasStructDef> getStructDefs() {
        return structDefs;
    }

    public void setStructDefs(List<AtlasStructDef> structDefs) {
        this.structDefs = structDefs;
    }

    public List<AtlasClassificationDef> getClassificationDefs() {
        return classificationDefs;
    }

    public List<AtlasEntityDef> getEntityDefs() {
        return entityDefs;
    }

    public void setEntityDefs(List<AtlasEntityDef> entityDefs) {
        this.entityDefs = entityDefs;
    }

    public void setClassificationDefs(List<AtlasClassificationDef> classificationDefs) {
        this.classificationDefs = classificationDefs;
    }
    public List<AtlasRelationshipDef> getRelationshipDefs() {
        return relationshipDefs;
    }

    public void setRelationshipDefs(List<AtlasRelationshipDef> relationshipDefs) {
        this.relationshipDefs = relationshipDefs;
    }

    public void setBusinessMetadataDefs(List<AtlasBusinessMetadataDef> businessMetadataDefs) {
        this.businessMetadataDefs = businessMetadataDefs;
    }

    public List<AtlasBusinessMetadataDef> getBusinessMetadataDefs() {
        return businessMetadataDefs;
    }

    public boolean hasClassificationDef(String name) {
        return hasTypeDef(classificationDefs, name);
    }

    public boolean hasEnumDef(String name) {
        return hasTypeDef(enumDefs, name);
    }

    public boolean hasStructDef(String name) {
        return hasTypeDef(structDefs, name);
    }

    public boolean hasEntityDef(String name) {
        return hasTypeDef(entityDefs, name);
    }
    public boolean hasRelationshipDef(String name) {
        return hasTypeDef(relationshipDefs, name);
    }

    public boolean hasBusinessMetadataDef(String name) {
        return hasTypeDef(businessMetadataDefs, name);
    }

    private <T extends AtlasBaseTypeDef> boolean hasTypeDef(Collection<T> typeDefs, String name) {
        if (CollectionUtils.isNotEmpty(typeDefs)) {
            for (T typeDef : typeDefs) {
                if (typeDef.getName().equals(name)) {
                    return true;
                }
            }
        }

        return false;
    }

    @JsonIgnore
    public boolean isEmpty() {
        return CollectionUtils.isEmpty(enumDefs) &&
                CollectionUtils.isEmpty(structDefs) &&
                CollectionUtils.isEmpty(classificationDefs) &&
                CollectionUtils.isEmpty(entityDefs) &&
                CollectionUtils.isEmpty(relationshipDefs) &&
                CollectionUtils.isEmpty(businessMetadataDefs);
    }

    public void clear() {
        if (enumDefs != null) {
            enumDefs.clear();
        }

        if (structDefs != null) {
            structDefs.clear();
        }

        if (classificationDefs != null) {
            classificationDefs.clear();
        }

        if (entityDefs != null) {
            entityDefs.clear();
        }
        if (relationshipDefs != null) {
            relationshipDefs.clear();
        }

        if (businessMetadataDefs != null) {
            businessMetadataDefs.clear();
        }
    }
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasTypesDef{");
        sb.append("enumDefs={");
        AtlasBaseTypeDef.dumpObjects(enumDefs, sb);
        sb.append("}");
        sb.append("structDefs={");
        AtlasBaseTypeDef.dumpObjects(structDefs, sb);
        sb.append("}");
        sb.append("classificationDefs={");
        AtlasBaseTypeDef.dumpObjects(classificationDefs, sb);
        sb.append("}");
        sb.append("entityDefs={");
        AtlasBaseTypeDef.dumpObjects(entityDefs, sb);
        sb.append("}");
        sb.append("relationshipDefs={");
        AtlasBaseTypeDef.dumpObjects(relationshipDefs, sb);
        sb.append("businessMetadataDefs={");
        AtlasBaseTypeDef.dumpObjects(businessMetadataDefs, sb);
        sb.append("}");

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
