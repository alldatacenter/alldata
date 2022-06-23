/**
 * Licensed to the Apache Software Foundation (ASF) under oneØ
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
package org.apache.atlas.model.typedef;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * The relationshipEndDef represents an end of the relationship. The end of the relationship is defined by a type, an
 * attribute name, cardinality and whether it  is the container end of the relationship.
 */
@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasRelationshipEndDef implements Serializable {
    private static final long serialVersionUID = 1L;
    /**
     * The type associated with the end.
     */
    private String type;
    /**
     * The name of the attribute for this end
     */
    private String name;

    /**
     * When set this indicates that this end is the container end
     */
    private boolean isContainer;
    /**
     * This is the cardinality of the end
     */
    private Cardinality cardinality;
    /**
     * When set this indicates that this end is a legacy attribute
     */
    private boolean isLegacyAttribute;
    /**
     * Description of the end
     */
    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Base constructor
     */
    public AtlasRelationshipEndDef() {
        this(null, null, Cardinality.SINGLE, false);
    }

    /**
     *
     * @param typeName
     *   - The name of an entityDef type
     * @param name
     *   - The name of the new attribute that the entity instance will pick up.
     * @param cardinality
     *   - this indicates whether the end is SINGLE (1) or SET (many)
     */
    public AtlasRelationshipEndDef(String typeName, String name, Cardinality cardinality) {
        this(typeName, name, cardinality, false);
    }

    /**
     *
     * @param typeName
     *   - The name of an entityDef type
     * @param name
     *   - The name of the new attribute that the entity instance will pick up.
     * @param cardinality
     *   - whether the end is SINGLE (1) or SET (many)
     * @param isContainer
     *   - whether the end is a container or not
     */
    public AtlasRelationshipEndDef(String typeName, String name, Cardinality cardinality, boolean isContainer) {
        this(typeName, name, cardinality, isContainer, false);
    }
    /**
     *
     * @param typeName
     *   - The name of an entityDef type
     * @param name
     *   - The name of the new attribute that the entity instance will pick up.
     * @param cardinality
     *   - whether the end is SINGLE (1) or SET (many)
     * @param isContainer
     *   - whether the end is a container or not
     * @param isLegacyAttribute
     *   - whether this is a legacy attribute
     */
    public AtlasRelationshipEndDef(String typeName, String name, Cardinality cardinality, boolean isContainer, boolean isLegacyAttribute) {
        this(typeName, name, cardinality, isContainer, isLegacyAttribute, null);
    }
    /**
     *
     * @param typeName
     *   - The name of an entityDef type
     * @param name
     *   - The name of the new attribute that the entity instance will pick up.
     * @param cardinality
     *   - whether the end is SINGLE (1) or SET (many)
     * @param isContainer
     *   - whether the end is a container or not
     * @param isLegacyAttribute
     *   - whether this is a legacy attribute
     * @param description
     *   - The description of this end of the relationship.
     */
    public AtlasRelationshipEndDef(String typeName, String name, Cardinality cardinality, boolean isContainer, boolean isLegacyAttribute, String description) {
        setType(typeName);
        setName(name);
        setCardinality(cardinality);
        setIsContainer(isContainer);
        setIsLegacyAttribute(isLegacyAttribute);
        setDescription(description);
    }

    /**
     * Construct using an existing AtlasRelationshipEndDef
     * @param other
     */
    public AtlasRelationshipEndDef(AtlasRelationshipEndDef other) {
        if (other != null) {
            setType(other.getType());
            setName(other.getName());
            setIsContainer(other.getIsContainer());
            setCardinality(other.getCardinality());
            setIsLegacyAttribute(other.isLegacyAttribute);
            setDescription(other.description);
        }
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * set whether this end is a container or not.
     * @param isContainer
     */
    public void setIsContainer(boolean isContainer) {
        this.isContainer = isContainer;
    }

    public boolean getIsContainer() {
        return isContainer;
    }

    /**
     * set the cardinality SINGLE or SET on the end.
     * @param cardinality
     */
    public void setCardinality(AtlasStructDef.AtlasAttributeDef.Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    /**
     *
     * @return the cardinality
     */
    public Cardinality getCardinality() {
        return this.cardinality;
    }

    public boolean getIsLegacyAttribute() { return isLegacyAttribute; }

    public void setIsLegacyAttribute(boolean legacyAttribute) { isLegacyAttribute = legacyAttribute; }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasRelationshipEndDef{");
        sb.append("type='").append(type).append('\'');
        sb.append(", name==>'").append(name).append('\'');
        sb.append(", description==>'").append(description).append('\'');
        sb.append(", isContainer==>'").append(isContainer).append('\'');
        sb.append(", cardinality==>'").append(cardinality).append('\'');
        sb.append(", isLegacyAttribute==>'").append(isLegacyAttribute).append('\'');
        sb.append('}');

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AtlasRelationshipEndDef that = (AtlasRelationshipEndDef) o;

        return Objects.equals(type, that.type) &&
               Objects.equals(name, that.name) &&
               Objects.equals(description, that.description) &&
               isContainer == that.isContainer &&
               cardinality == that.cardinality &&
               isLegacyAttribute == that.isLegacyAttribute;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, getName(), description, isContainer, cardinality, isLegacyAttribute);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
