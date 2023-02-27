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
package org.apache.atlas.model.typedef;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;


import org.apache.atlas.model.PList;
import org.apache.atlas.model.SearchFilter.SortType;
import org.apache.atlas.model.TypeCategory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * class that captures details of a entity-type.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasEntityDef extends AtlasStructDef implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    public static final String OPTION_DISPLAY_TEXT_ATTRIBUTE = "displayTextAttribute";

    private Set<String> superTypes;

    // this is a read-only field, any value provided during create & update operation is ignored
    // the value of this field is derived from 'superTypes' specified in all AtlasEntityDef
    private Set<String> subTypes;

    // this is a read-only field, any value provided during create & update operation is ignored
    // the value of this field is derived from all the relationshipDefs this entityType is referenced in
    private List<AtlasRelationshipAttributeDef> relationshipAttributeDefs;

    // this is a read-only field, any value provided during create & update operation is ignored
    // the value of this field is derived from all the businessMetadataDefs this entityType is referenced in
    private Map<String, List<AtlasAttributeDef>> businessAttributeDefs;


    public AtlasEntityDef() {
        this(null, null, null, null, null, null, null);
    }

    public AtlasEntityDef(String name) {
        this(name, null, null, null, null, null, null);
    }

    public AtlasEntityDef(String name, String description) {
        this(name, description, null, null, null, null, null);
    }

    public AtlasEntityDef(String name, String description, String typeVersion) {
        this(name, description, typeVersion, null, null, null, null);
    }
    
    public AtlasEntityDef(String name, String description, String typeVersion, String serviceType) {
        this(name, description, typeVersion, serviceType, null, null, null);
    }


    public AtlasEntityDef(String name, String description, String typeVersion, List<AtlasAttributeDef> attributeDefs) {
        this(name, description, typeVersion, attributeDefs, null);
    }
    
    public AtlasEntityDef(String name, String description, String typeVersion, String serviceType, List<AtlasAttributeDef> attributeDefs) {
        this(name, description, typeVersion, serviceType, attributeDefs, null, null);
    }

    public AtlasEntityDef(String name, String description, String typeVersion, List<AtlasAttributeDef> attributeDefs,
                          Set<String> superTypes) {
        this(name, description, typeVersion, attributeDefs, superTypes, null);
    }
    
    public AtlasEntityDef(String name, String description, String typeVersion, String serviceType, List<AtlasAttributeDef> attributeDefs,
            Set<String> superTypes) {
    	this(name, description, typeVersion, serviceType, attributeDefs, superTypes, null);
    }


    public AtlasEntityDef(String name, String description, String typeVersion, List<AtlasAttributeDef> attributeDefs,
                          Set<String> superTypes, Map<String, String> options) {
        super(TypeCategory.ENTITY, name, description, typeVersion, attributeDefs, options);

        setSuperTypes(superTypes);
    }
    
    public AtlasEntityDef(String name, String description, String typeVersion, String serviceType, List<AtlasAttributeDef> attributeDefs,
            Set<String> superTypes, Map<String, String> options) {
    	super(TypeCategory.ENTITY, name, description, typeVersion, attributeDefs, serviceType, options);

		setSuperTypes(superTypes);
	}


    public AtlasEntityDef(AtlasEntityDef other) {
        super(other);

        if (other != null) {
            setSuperTypes(other.getSuperTypes());
            setSubTypes(other.getSubTypes());
            setRelationshipAttributeDefs(other.getRelationshipAttributeDefs());
            setBusinessAttributeDefs(other.getBusinessAttributeDefs());
        }
    }



	public Set<String> getSuperTypes() {
        return superTypes;
    }

    public void setSuperTypes(Set<String> superTypes) {
        if (superTypes != null && this.superTypes == superTypes) {
            return;
        }

        if (CollectionUtils.isEmpty(superTypes)) {
            this.superTypes = new HashSet<>();
        } else {
            this.superTypes = new HashSet<>(superTypes);
        }
    }

    public Set<String> getSubTypes() {
        return subTypes;
    }

    public void setSubTypes(Set<String> subTypes) {
        this.subTypes = subTypes;
    }

    public List<AtlasRelationshipAttributeDef> getRelationshipAttributeDefs() {
        return relationshipAttributeDefs;
    }

    public void setRelationshipAttributeDefs(List<AtlasRelationshipAttributeDef> relationshipAttributeDefs) {
        this.relationshipAttributeDefs = relationshipAttributeDefs;
    }

    public Map<String, List<AtlasAttributeDef>> getBusinessAttributeDefs() {
        return businessAttributeDefs;
    }

    public void setBusinessAttributeDefs(Map<String, List<AtlasAttributeDef>> businessAttributeDefs) {
        this.businessAttributeDefs = businessAttributeDefs;
    }

    public boolean hasSuperType(String typeName) {
        return hasSuperType(superTypes, typeName);
    }

    public void addSuperType(String typeName) {
        Set<String> s = this.superTypes;

        if (!hasSuperType(s, typeName)) {
            s = new HashSet<>(s);

            s.add(typeName);

            this.superTypes = s;
        }
    }

    public void removeSuperType(String typeName) {
        Set<String> s = this.superTypes;

        if (hasSuperType(s, typeName)) {
            s = new HashSet<>(s);

            s.remove(typeName);

            this.superTypes = s;
        }
    }

    private static boolean hasSuperType(Set<String> superTypes, String typeName) {
        return superTypes != null && typeName != null && superTypes.contains(typeName);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasEntityDef{");
        super.toString(sb);
        sb.append(", superTypes=[");
        dumpObjects(superTypes, sb);
        sb.append("]");
        sb.append(", relationshipAttributeDefs=[");
        if (CollectionUtils.isNotEmpty(relationshipAttributeDefs)) {
            int i = 0;
            for (AtlasRelationshipAttributeDef attributeDef : relationshipAttributeDefs) {
                if (i > 0) {
                    sb.append(", ");
                }

                attributeDef.toString(sb);

                i++;
            }
        }
        sb.append(']');
        sb.append(", businessAttributeDefs={");
        if (MapUtils.isNotEmpty(businessAttributeDefs)) {
            int nsIdx = 0;

            for (Map.Entry<String, List<AtlasAttributeDef>> entry : businessAttributeDefs.entrySet()) {
                String                  nsName  = entry.getKey();
                List<AtlasAttributeDef> nsAttrs = entry.getValue();

                if (nsIdx > 0) {
                    sb.append(", ");
                }

                sb.append(nsName).append("=[");

                int attrIdx = 0;
                for (AtlasAttributeDef attributeDef : nsAttrs) {
                    if (attrIdx > 0) {
                        sb.append(", ");
                    }

                    attributeDef.toString(sb);

                    attrIdx++;
                }
                sb.append(']');

                nsIdx++;
            }
        }
        sb.append('}');
        sb.append('}');

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        AtlasEntityDef that = (AtlasEntityDef) o;
        return Objects.equals(superTypes, that.superTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), superTypes);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    /**
     * class that captures details of a struct-attribute.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include= JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class AtlasRelationshipAttributeDef extends AtlasAttributeDef implements Serializable {
        private static final long serialVersionUID = 1L;

        private String  relationshipTypeName;
        private boolean isLegacyAttribute;

        public AtlasRelationshipAttributeDef() { }

        public AtlasRelationshipAttributeDef(String relationshipTypeName, boolean isLegacyAttribute, AtlasAttributeDef attributeDef) {
            super(attributeDef);

            this.relationshipTypeName = relationshipTypeName;
            this.isLegacyAttribute    = isLegacyAttribute;
        }

        public String getRelationshipTypeName() {
            return relationshipTypeName;
        }

        public void setRelationshipTypeName(String relationshipTypeName) {
            this.relationshipTypeName = relationshipTypeName;
        }

        public boolean getIsLegacyAttribute() {
            return isLegacyAttribute;
        }

        public void setIsLegacyAttribute(boolean isLegacyAttribute) {
            this.isLegacyAttribute = isLegacyAttribute;
        }

        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("AtlasRelationshipAttributeDef{");
            super.toString(sb);
            sb.append(", relationshipTypeName='").append(relationshipTypeName).append('\'');
            sb.append(", isLegacyAttribute='").append(isLegacyAttribute).append('\'');
            sb.append('}');

            return sb;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;

            if (o == null || getClass() != o.getClass()) return false;

            AtlasRelationshipAttributeDef that = (AtlasRelationshipAttributeDef) o;

            return super.equals(that) &&
                   isLegacyAttribute == that.isLegacyAttribute &&
                   Objects.equals(relationshipTypeName, that.relationshipTypeName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), relationshipTypeName, isLegacyAttribute);
        }

        @Override
        public String toString() {
            return toString(new StringBuilder()).toString();
        }
    }


    /**
     * REST serialization friendly list.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlSeeAlso(AtlasEntityDef.class)
    public static class AtlasEntityDefs extends PList<AtlasEntityDef> {
        private static final long serialVersionUID = 1L;

        public AtlasEntityDefs() {
            super();
        }

        public AtlasEntityDefs(List<AtlasEntityDef> list) {
            super(list);
        }

        public AtlasEntityDefs(List list, long startIndex, int pageSize, long totalCount,
                               SortType sortType, String sortBy) {
            super(list, startIndex, pageSize, totalCount, sortType, sortBy);
        }
    }
}
