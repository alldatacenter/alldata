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

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;


/**
 * class that captures details of a classification-type.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasClassificationDef extends AtlasStructDef implements java.io.Serializable {
    private static final long serialVersionUID = 1L;

    private Set<String> superTypes;
    private Set<String> entityTypes;

    // subTypes field below is derived from 'superTypes' specified in all AtlasClassificationDef
    // this value is ignored during create & update operations
    private Set<String> subTypes;


    public AtlasClassificationDef() {
        this(null, null, null, null, null, null);
    }

    public AtlasClassificationDef(String name) {
        this(name, null, null, null, null, null);
    }

    public AtlasClassificationDef(String name, String description) {
        this(name, description, null, null, null, null);
    }

    public AtlasClassificationDef(String name, String description, String typeVersion) {
        this(name, description, typeVersion, null, null, null);
    }

    public AtlasClassificationDef(String name, String description, String typeVersion,
                                  List<AtlasAttributeDef> attributeDefs) {
        this(name, description, typeVersion, attributeDefs, null, null);
    }

    public AtlasClassificationDef(String name, String description, String typeVersion,
                                  List<AtlasAttributeDef> attributeDefs, Set<String> superTypes) {
        this(name, description, typeVersion, attributeDefs, superTypes, null);
    }

    public AtlasClassificationDef(String name, String description, String typeVersion,
                                  List<AtlasAttributeDef> attributeDefs, Set<String> superTypes,
                                  Map<String, String> options) {
        this(name, description, typeVersion, attributeDefs, superTypes, null, options);
    }

    public AtlasClassificationDef(String name, String description, String typeVersion,
                                  List<AtlasAttributeDef> attributeDefs, Set<String> superTypes,
                                  Set<String> entityTypes, Map<String, String> options) {
        super(TypeCategory.CLASSIFICATION, name, description, typeVersion, attributeDefs, options);

        setSuperTypes(superTypes);
        setEntityTypes(entityTypes);
    }

    public AtlasClassificationDef(AtlasClassificationDef other) {
        super(other);

        setSuperTypes(other != null ? other.getSuperTypes() : null);
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

    /**
     * Specifying a list of entityType names in the classificationDef, ensures that classifications can
     * only be applied to those entityTypes.
     * <ul>
     * <li>Any subtypes of the entity types inherit the restriction</li>
     * <li>Any classificationDef subtypes inherit the parents entityTypes restrictions</li>
     * <li>Any classificationDef subtypes can further restrict the parents entityTypes restrictions by specifying a subset of the entityTypes</li>
     * <li>An empty entityTypes list when there are no parent restrictions means there are no restrictions</li>
     * <li>An empty entityTypes list when there are parent restrictions means that the subtype picks up the parents restrictions</li>
     * <li>If a list of entityTypes are supplied, where one inherits from another, this will be rejected. This should encourage cleaner classificationsDefs</li>
     * </ul>
     */
    public Set<String> getEntityTypes() {
        return entityTypes;
    }

    public void setEntityTypes(Set<String> entityTypes) {
        if (entityTypes != null && this.entityTypes == entityTypes) {
            return;
        }

        if (CollectionUtils.isEmpty(entityTypes)) {
            this.entityTypes = new HashSet<>();
        } else {
            this.entityTypes = new HashSet<>(entityTypes);
        }
    }

    public boolean hasEntityType(String typeName) {
        return hasEntityType(entityTypes, typeName);
    }

    public void addEntityType(String typeName) {
        Set<String> s = this.entityTypes;

        if (!hasEntityType(s, typeName)) {
            s = new HashSet<>(s);

            s.add(typeName);

            this.entityTypes = s;
        }
    }

    public void removeEntityType(String typeName) {
        Set<String> s = this.entityTypes;

        if (hasEntityType(s, typeName)) {
            s = new HashSet<>(s);

            s.remove(typeName);

            this.entityTypes = s;
        }
    }

    private static boolean hasEntityType(Set<String> entityTypes, String typeName) {
        return entityTypes != null && typeName != null && entityTypes.contains(typeName);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasClassificationDef{");
        super.toString(sb);
        sb.append(", superTypes=[");
        dumpObjects(superTypes, sb);
        sb.append("], entityTypes=[");
        dumpObjects(entityTypes, sb);
        sb.append("]");
        sb.append('}');

        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        if (!super.equals(o)) { return false; }

        AtlasClassificationDef that = (AtlasClassificationDef) o;

        return Objects.equals(superTypes, that.superTypes) && Objects.equals(entityTypes,that.entityTypes);
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
     * REST serialization friendly list.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    @XmlSeeAlso(AtlasClassificationDef.class)
    public static class AtlasClassificationDefs extends PList<AtlasClassificationDef> {
        private static final long serialVersionUID = 1L;

        public AtlasClassificationDefs() {
            super();
        }

        public AtlasClassificationDefs(List<AtlasClassificationDef> list) {
            super(list);
        }

        public AtlasClassificationDefs(List list, long startIndex, int pageSize, long totalCount,
                                       SortType sortType, String sortBy) {
            super(list, startIndex, pageSize, totalCount, sortType, sortBy);
        }
    }

}
