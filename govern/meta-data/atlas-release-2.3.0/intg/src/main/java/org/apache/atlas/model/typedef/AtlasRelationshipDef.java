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

import org.apache.atlas.model.TypeCategory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * AtlasRelationshipDef is a TypeDef that defines a relationship.
 * <p>
 * As with other typeDefs the AtlasRelationshipDef has a name. Once created the RelationshipDef has a guid.
 * The name and the guid are the 2 ways that the RelationshipDef is identified.
 * <p>
 * RelationshipDefs have 2 ends, each of which specify cardinality, an EntityDef type name and name and optionally
 * whether the end is a container.
 * <p>
 * RelationshipDefs can have AttributeDefs - though only primitive types are allowed. <br>
 * RelationshipDefs have a relationshipCategory specifying the UML type of relationship required <br>
 * RelationshipDefs also have a PropogateTag - indicating which way tags could flow over the relationships.
 * <p>
 * The way EntityDefs and RelationshipDefs are intended to be used is that EntityDefs will define AttributeDefs these AttributeDefs
 * will not specify an EntityDef type name as their types.
 * <p>
 * RelationshipDefs introduce new attributes to the entity instances. For example
 * <p>
 * EntityDef A might have attributes attr1,attr2,attr3 <br>
 * EntityDef B might have attributes attr4,attr5,attr6 <br>
 * RelationshipDef AtoB might define 2 ends <br>
 *
 * <pre>
 *   end1:  type A, name attr7
 *   end2:  type B, name attr8  </pre>
 *
 * <p>
 * When an instance of EntityDef A is created, it will have attributes attr1,attr2,attr3,attr7 <br>
 * When an instance of EntityDef B is created, it will have attributes attr4,attr5,attr6,attr8
 * <p>
 * In this way relationshipDefs can be authored separately from entityDefs and can inject relationship attributes into
 * the entity instances
 *
 */
@JsonAutoDetect(getterVisibility = PUBLIC_ONLY, setterVisibility = PUBLIC_ONLY, fieldVisibility = NONE)
@JsonSerialize(include = JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class AtlasRelationshipDef extends AtlasStructDef implements java.io.Serializable {
    private static final long serialVersionUID = 1L;


    /**
     * The Relationship category determines the style of relationship around containment and lifecycle.
     * UML terminology is used for the values.
     * <p>
     * ASSOCIATION is a relationship with no containment. <br>
     * COMPOSITION and AGGREGATION are containment relationships.
     * <p>
     * The difference being in the lifecycles of the container and its children. In the COMPOSITION case,
     * the children cannot exist without the container. For AGGREGATION, the life cycles
     * of the container and children are totally independant.
     */
    public enum RelationshipCategory {
        ASSOCIATION, AGGREGATION, COMPOSITION
    };

    /**
     * PropagateTags indicates whether tags should propagate across the relationship instance.
     * <p>
     * Tags can propagate:
     * <p>
     * NONE - not at all <br>
     * ONE_TO_TWO - from end 1 to 2 <br>
     * TWO_TO_ONE - from end 2 to 1  <br>
     * BOTH - both ways
     * <p>
     * Care needs to be taken when specifying. The use cases we are aware of where this flag is useful:
     * <p>
     * - propagating confidentiality classifications from a table to columns - ONE_TO_TWO could be used here <br>
     * - propagating classifications around Glossary synonyms - BOTH could be used here.
     * <p>
     * There is an expectation that further enhancements will allow more granular control of tag propagation and will
     * address how to resolve conflicts.
     */
    public enum PropagateTags {
        NONE, ONE_TO_TWO, TWO_TO_ONE, BOTH
    };

    private RelationshipCategory    relationshipCategory;
    private String                  relationshipLabel;
    private PropagateTags           propagateTags;
    private AtlasRelationshipEndDef endDef1;
    private AtlasRelationshipEndDef endDef2;

    /**
     * AtlasRelationshipDef contructor
     */
    public AtlasRelationshipDef()  {
        this(null, null, null, null,null, null, null);
    }

    /**
     * Create a relationshipDef without attributeDefs
     * @param name
     *            - the name of the relationship type
     * @param description
     *            - an optional description
     * @param typeVersion
     *            - version - that defaults to 1.0
     * @param relationshipCategory
     *            - there are 3 sorts of relationship category ASSOCIATION, COMPOSITION
     *            and AGGREGATION
     * @param propagatetags
     *            -
     * @param endDef1
     *            - first end. An end specifies an entity type and an attribute name. the attribute name then appears in
     *            the relationship instance
     * @param endDef2
     *            - second end. An end specifies an entity type and an attribute name. the attribute name then appears in
     *            the relationship instance
     *
     *            The ends are defined as 1 and 2 to avoid implying a direction. So we do not use to and from.
     *
     */
    public AtlasRelationshipDef(String name, String description, String typeVersion,
                                RelationshipCategory relationshipCategory,
                                PropagateTags propagatetags,
                                AtlasRelationshipEndDef endDef1,
                                AtlasRelationshipEndDef endDef2) {
        this(name, description, typeVersion, relationshipCategory,propagatetags, endDef1, endDef2, new ArrayList<>());
    }

    /**
     * Create a relationshipDef without attributeDefs
     * @param name
     *            - the name of the relationship type
     * @param description
     *            - an optional description
     * @param typeVersion
     *            - version - that defaults to 1.0
     * @param serviceType
     *            - the serviceType
     * @param relationshipCategory
     *            - there are 3 sorts of relationship category ASSOCIATION, COMPOSITION
     *            and AGGREGATION
     * @param propagatetags
     *            -
     * @param endDef1
     *            - first end. An end specifies an entity type and an attribute name. the attribute name then appears in
     *            the relationship instance
     * @param endDef2
     *            - second end. An end specifies an entity type and an attribute name. the attribute name then appears in
     *            the relationship instance
     *
     *            The ends are defined as 1 and 2 to avoid implying a direction. So we do not use to and from.
     */
    public AtlasRelationshipDef(String name, String description, String typeVersion, String serviceType,
                                RelationshipCategory relationshipCategory,
                                PropagateTags propagatetags,
                                AtlasRelationshipEndDef endDef1,
                                AtlasRelationshipEndDef endDef2)  {
        this(name, description, typeVersion, serviceType, relationshipCategory,propagatetags, endDef1, endDef2, new ArrayList<>());
    }


    /**
     * Create a relationshipDef with attributeDefs
     * @param name
     *            - the name of the relationship type
     * @param description
     *            - an optional description
     * @param typeVersion
     *            - version - that defaults to 1.0
     * @param relationshipCategory
     *            - there are 3 sorts of relationship category ASSOCIATION, COMPOSITION
     *            and AGGREGATION
     * @param propagatetags
     *            -
     * @param endDef1
     *            - First end. As end specifies an entity
     *            type and an attribute name. the attribute name then appears in
     *            the relationship instance
     * @param endDef2
     *            - Second end. The ends are defined as 1
     *            ad 2 to avoid implying a direction. So we do not use to and
     *            from.
     * @param attributeDefs
     *            - these are the attributes on the relationship itself.
     */
    public AtlasRelationshipDef(String name, String description, String typeVersion,
                                RelationshipCategory relationshipCategory,
                                PropagateTags propagatetags, AtlasRelationshipEndDef endDef1,
                                AtlasRelationshipEndDef endDef2, List<AtlasAttributeDef> attributeDefs) {
        this(name, description, typeVersion, null, relationshipCategory, propagatetags, endDef1, endDef2, attributeDefs);
    }

    /**
     * Create a relationshipDef with attributeDefs
     * @param name
     *            - the name of the relationship type
     * @param description
     *            - an optional description
     * @param typeVersion
     *            - version - that defaults to 1.0
     * @param serviceType
     *            - the serviceType
     * @param relationshipCategory
     *            - there are 3 sorts of relationship category ASSOCIATION, COMPOSITION
     *            and AGGREGATION
     * @param propagatetags
     *            -
     * @param endDef1
     *            - First end. As end specifies an entity
     *            type and an attribute name. the attribute name then appears in
     *            the relationship instance
     * @param endDef2
     *            - Second end. The ends are defined as 1
     *            ad 2 to avoid implying a direction. So we do not use to and
     *            from.
     * @param attributeDefs
     *            - these are the attributes on the relationship itself.
     */
    public AtlasRelationshipDef(String name, String description, String typeVersion, String serviceType,
                                RelationshipCategory relationshipCategory,
                                PropagateTags propagatetags, AtlasRelationshipEndDef endDef1,
                                AtlasRelationshipEndDef endDef2, List<AtlasAttributeDef> attributeDefs) {
        super(TypeCategory.RELATIONSHIP, name, description, typeVersion, attributeDefs, serviceType, null);

        setRelationshipCategory(relationshipCategory);
        setRelationshipLabel(null);
        setPropagateTags(propagatetags);
        setEndDef1(endDef1);
        setEndDef2(endDef2);
    }

    public AtlasRelationshipDef(AtlasRelationshipDef other) {
        super(other);

        if (other != null) {
            setRelationshipCategory(other.getRelationshipCategory());
            setRelationshipLabel(other.getRelationshipLabel());
            setPropagateTags(other.getPropagateTags());
            setEndDef1(other.getEndDef1());
            setEndDef2(other.getEndDef2());
        }
    }

    public void setRelationshipCategory(RelationshipCategory relationshipCategory) {
        this.relationshipCategory = relationshipCategory;
    }

    public RelationshipCategory getRelationshipCategory() {
        return this.relationshipCategory;
    }

    public void setRelationshipLabel(String relationshipLabel) {
        this.relationshipLabel = relationshipLabel;
    }

    public String getRelationshipLabel() {
        return relationshipLabel;
    }

    public void setPropagateTags(PropagateTags propagateTags) {
        this.propagateTags=propagateTags;
    }

    public PropagateTags getPropagateTags() {
        return this.propagateTags;
    }

    public void setEndDef1(AtlasRelationshipEndDef endDef1) {
        this.endDef1 = endDef1;
    }

    public AtlasRelationshipEndDef getEndDef1() {
        return this.endDef1;
    }

    public void setEndDef2(AtlasRelationshipEndDef endDef2) {
        this.endDef2 = endDef2;
    }

    public AtlasRelationshipEndDef getEndDef2() {
        return this.endDef2;
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("AtlasRelationshipDef{");
        super.toString(sb);
        sb.append(',');
        sb.append(this.relationshipCategory);
        sb.append(',');
        sb.append(this.relationshipLabel);
        sb.append(',');
        sb.append(this.propagateTags);
        sb.append(',');
        if (this.endDef1 != null) {
            sb.append(this.endDef1.toString());
        }
        else {
            sb.append(" end1 is null!");
        }
        sb.append(',');
        if (this.endDef2 != null) {
            sb.append(this.endDef2.toString());
        }
        else {
            sb.append(" end2 is null!");
        }
        sb.append('}');
        return sb;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        //AttributeDefs are checked in the super
        if (!super.equals(o))
            return false;
        AtlasRelationshipDef that = (AtlasRelationshipDef) o;
        if (!Objects.equals(relationshipCategory, that.getRelationshipCategory()))
            return false;
        if (!Objects.equals(relationshipLabel, that.getRelationshipLabel()))
            return false;
        if (!Objects.equals(propagateTags, that.getPropagateTags()))
            return false;
        if (!Objects.equals(endDef1, that.getEndDef1()))
            return false;
        return (Objects.equals(endDef2, that.getEndDef2()));
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), relationshipCategory, relationshipLabel, propagateTags, endDef1, endDef2);
    }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }
}
