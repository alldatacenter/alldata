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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Map;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Reference to an object-instance of AtlasEntity type used in relationship attribute values
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasRelatedObjectId extends AtlasObjectId implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String KEY_RELATIONSHIP_TYPE       = "relationshipType";
    public static final String KEY_RELATIONSHIP_GUID       = "relationshipGuid";
    public static final String KEY_RELATIONSHIP_STATUS     = "relationshipStatus";
    public static final String KEY_RELATIONSHIP_ATTRIBUTES = "relationshipAttributes";

    private AtlasEntity.Status       entityStatus           = null;
    private String                   displayText            = null;
    private String                   relationshipType       = null;
    private String                   relationshipGuid       = null;
    private AtlasRelationship.Status relationshipStatus     = null;
    private AtlasStruct              relationshipAttributes = null;

    public AtlasRelatedObjectId() { }

    public AtlasRelatedObjectId(String guid, String typeName, AtlasEntity.Status entityStatus, String relationshipGuid,
                                AtlasRelationship.Status relationshipStatus, AtlasStruct relationshipAttributes) {
        super(guid, typeName);

        setEntityStatus(entityStatus);
        setRelationshipGuid(relationshipGuid);
        setRelationshipStatus(relationshipStatus);
        setRelationshipAttributes(relationshipAttributes);
    }

    public AtlasRelatedObjectId(String guid, String typeName, AtlasEntity.Status entityStatus, Map<String, Object> uniqueAttributes, String displayText,
                                String relationshipGuid, AtlasRelationship.Status relationshipStatus,
                                AtlasStruct relationshipAttributes) {
        super(guid, typeName, uniqueAttributes);

        setEntityStatus(entityStatus);
        setRelationshipGuid(relationshipGuid);
        setRelationshipStatus(relationshipStatus);
        setDisplayText(displayText);
        setRelationshipAttributes(relationshipAttributes);
    }

    public AtlasRelatedObjectId(AtlasObjectId other) {
        super(other);
    }

    public AtlasRelatedObjectId(AtlasObjectId objId, String relationshipType) {
        this(objId);

        setRelationshipType(relationshipType);
    }

    public AtlasRelatedObjectId(Map objIdMap) {
        super(objIdMap);

        if (objIdMap != null) {
            Object g = objIdMap.get(KEY_RELATIONSHIP_GUID);
            Object t = objIdMap.get(KEY_RELATIONSHIP_TYPE);
            Object a = objIdMap.get(KEY_RELATIONSHIP_ATTRIBUTES);
            Object s = objIdMap.get(KEY_RELATIONSHIP_STATUS);

            if (g != null) {
                setRelationshipGuid(g.toString());
            }

            if (a instanceof Map) {
                setRelationshipAttributes(new AtlasStruct((Map) a));
            } else if (a instanceof AtlasStruct) {
                setRelationshipAttributes(new AtlasStruct((AtlasStruct) a));
            }

            if (t != null) {
                setRelationshipType(t.toString());
            }

            if (s != null) {
                setRelationshipStatus(AtlasRelationship.Status.valueOf(s.toString()));
            }
        }
    }

    public AtlasEntity.Status getEntityStatus() {
        return entityStatus;
    }

    public void setEntityStatus(AtlasEntity.Status entityStatus) {
        this.entityStatus = entityStatus;
    }

    public String getDisplayText() { return displayText; }

    public void setDisplayText(String displayText) { this.displayText = displayText; }

    public String getRelationshipType() { return relationshipType; }

    public void setRelationshipType(String relationshipType) { this.relationshipType = relationshipType; }

    public String getRelationshipGuid() { return relationshipGuid; }

    public void setRelationshipGuid(String relationshipGuid) { this.relationshipGuid = relationshipGuid; }

    public AtlasRelationship.Status getRelationshipStatus() {
        return relationshipStatus;
    }

    public void setRelationshipStatus(final AtlasRelationship.Status relationshipStatus) {
        this.relationshipStatus = relationshipStatus;
    }

    public AtlasStruct getRelationshipAttributes() { return relationshipAttributes; }

    public void setRelationshipAttributes(AtlasStruct relationshipAttributes) {
        this.relationshipAttributes = relationshipAttributes;

        if (relationshipAttributes != null && relationshipAttributes.getTypeName() != null) {
            setRelationshipType(relationshipAttributes.getTypeName());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }
        AtlasRelatedObjectId that = (AtlasRelatedObjectId) o;
        return Objects.equals(entityStatus, that.entityStatus) &&
               Objects.equals(displayText, that.displayText) &&
               Objects.equals(relationshipType, that.relationshipType) &&
               Objects.equals(relationshipGuid, that.relationshipGuid) &&
               Objects.equals(relationshipStatus, that.relationshipStatus) &&
               Objects.equals(relationshipAttributes, that.relationshipAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), displayText, relationshipType, relationshipGuid, relationshipStatus, relationshipAttributes);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasRelatedObjectId{");
        super.toString(sb);
        sb.append("entityStatus='").append(entityStatus).append('\'');
        sb.append(", displayText='").append(displayText).append('\'');
        sb.append(", relationshipType='").append(relationshipType).append('\'');
        sb.append(", relationshipGuid='").append(relationshipGuid).append('\'');
        sb.append(", relationshipStatus='").append(relationshipStatus).append('\'');
        sb.append(", relationshipAttributes=").append(relationshipAttributes);
        sb.append('}');

        return sb;
    }
}