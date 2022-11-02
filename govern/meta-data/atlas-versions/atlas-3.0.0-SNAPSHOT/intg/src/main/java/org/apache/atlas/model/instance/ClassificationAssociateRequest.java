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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.commons.collections.CollectionUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ClassificationAssociateRequest {
    private AtlasClassification         classification;
    private List<String>                entityGuids;
    private List<Map<String, Object>>   entitiesUniqueAttributes;
    private String                      entityTypeName;

    public ClassificationAssociateRequest() {
        this(null, null);
    }

    public ClassificationAssociateRequest(List<String> entityGuids, AtlasClassification classification) {
        setEntityGuids(entityGuids);
        setClassification(classification);
    }

    public ClassificationAssociateRequest(String entityTypeName, List<Map<String, Object>> entitiesUniqueAttributes, AtlasClassification classification) {
        setEntitiesUniqueAttributes(entitiesUniqueAttributes);
        setClassification(classification);
        setEntityTypeName(entityTypeName);
    }

    public ClassificationAssociateRequest(List<String> entityGuids, String entityTypeName, List<Map<String, Object>> entitiesUniqueAttributes, AtlasClassification classification) {
        setEntityGuids(entityGuids);
        setEntitiesUniqueAttributes(entitiesUniqueAttributes);
        setClassification(classification);
        setEntityTypeName(entityTypeName);
    }

    public String getEntityTypeName() {
        return entityTypeName;
    }

    public void setEntityTypeName(String entityTypeName) {
        this.entityTypeName = entityTypeName;
    }

    public List<Map<String, Object>> getEntitiesUniqueAttributes() {
        return entitiesUniqueAttributes;
    }

    public void setEntitiesUniqueAttributes(List<Map<String, Object>> entitiesUniqueAttributes) {
        this.entitiesUniqueAttributes = entitiesUniqueAttributes;
    }

    public AtlasClassification getClassification() { return classification; }

    public void setClassification(AtlasClassification classification) { this.classification = classification; }

    public List<String> getEntityGuids() { return entityGuids; }

    public void setEntityGuids(List<String> entityGuids) { this.entityGuids = entityGuids; }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }

        if (o == null || getClass() != o.getClass()) { return false; }

        ClassificationAssociateRequest that = (ClassificationAssociateRequest) o;

        return Objects.equals(classification, that.classification) && Objects.equals(entityGuids, that.entityGuids) && CollectionUtils.isEqualCollection(entitiesUniqueAttributes, that.entitiesUniqueAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classification, entityGuids, entitiesUniqueAttributes);
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("ClassificationAssociateRequest{");
        sb.append("classification='");
        if (classification != null) {
            classification.toString(sb);
        }
        sb.append("', entityGuids=[");
        AtlasBaseTypeDef.dumpObjects(entityGuids, sb);
        sb.append("]");
        sb.append(", entityTypeName=[");
        if (entityTypeName != null) {
            sb.append(entityTypeName);
        }
        sb.append("]");
        sb.append(", entitiesUniqueAttributes=[");
        AtlasBaseTypeDef.dumpObjects(entitiesUniqueAttributes, sb);
        sb.append("]");
        sb.append('}');

        return sb;
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
