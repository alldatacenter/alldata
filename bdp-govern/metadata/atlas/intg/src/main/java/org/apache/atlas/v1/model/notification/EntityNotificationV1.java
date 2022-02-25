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
package org.apache.atlas.v1.model.notification;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.notification.EntityNotification;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Entity notification
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class EntityNotificationV1 extends EntityNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum OperationType {
        ENTITY_CREATE,
        ENTITY_UPDATE,
        ENTITY_DELETE,
        TRAIT_ADD,
        TRAIT_DELETE,
        TRAIT_UPDATE
    }

    private Referenceable entity;
    private OperationType operationType;
    private List<Struct>  traits;


    // ----- Constructors ------------------------------------------------------

    /**
     * No-arg constructor for serialization.
     */
    public EntityNotificationV1() {
    }

    /**
     * Construct an EntityNotificationV1.
     *
     * @param entity            the entity subject of the notification
     * @param operationType     the type of operation that caused the notification
     * @param traits            the traits for the given entity
     */
    public EntityNotificationV1(Referenceable entity, OperationType operationType, List<Struct> traits) {
        this.entity        = entity;
        this.operationType = operationType;
        this.traits        = traits;
    }

    /**
     * Construct an EntityNotificationV1.
     *
     * @param entity         the entity subject of the notification
     * @param operationType  the type of operation that caused the notification
     * @param typeRegistry     the Atlas type system
     */
    public EntityNotificationV1(Referenceable entity, OperationType operationType, AtlasTypeRegistry typeRegistry) {
        this(entity, operationType, getAllTraits(entity, typeRegistry));
    }

    public Referenceable getEntity() {
        return entity;
    }

    public void setEntity(Referenceable entity) {
        this.entity = entity;
    }

    public OperationType getOperationType() {
        return operationType;
    }

    public void setOperationType(OperationType operationType) {
        this.operationType = operationType;
    }

    public List<Struct> getTraits() {
        return traits;
    }

    public void setTraits(List<Struct> traits) {
        this.traits = traits;
    }

    @JsonIgnore
    public List<Struct> getAllTraits() {
        return traits;
    }

    public void normalize() {
        super.normalize();

        if (entity != null) {
            entity.normalize();
        }

        if (traits != null) {
            for (Struct trait : traits) {
                if (trait != null) {
                    trait.normalize();
                }
            }
        }
    }

    // ----- Object overrides --------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityNotificationV1 that = (EntityNotificationV1) o;
        return Objects.equals(entity, that.entity) &&
                operationType == that.operationType &&
                Objects.equals(traits, that.traits);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity, operationType, traits);
    }

    @Override
    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("EntityNotificationV1{");
        super.toString(sb);
        sb.append(", entity=");
        if (entity != null) {
            entity.toString(sb);
        } else {
            sb.append(entity);
        }
        sb.append(", operationType=").append(operationType);
        sb.append(", traits=[");
        AtlasBaseTypeDef.dumpObjects(traits, sb);
        sb.append("]");
        sb.append("}");

        return sb;
    }


    // ----- helper methods ----------------------------------------------------

    private static List<Struct> getAllTraits(Referenceable entityDefinition, AtlasTypeRegistry typeRegistry) {
        List<Struct> ret = new LinkedList<>();

        for (String traitName : entityDefinition.getTraitNames()) {
            Struct                  trait          = entityDefinition.getTrait(traitName);
            AtlasClassificationType traitType      = typeRegistry.getClassificationTypeByName(traitName);
            Set<String>             superTypeNames = traitType != null ? traitType.getAllSuperTypes() : null;

            ret.add(trait);

            if (CollectionUtils.isNotEmpty(superTypeNames)) {
                for (String superTypeName : superTypeNames) {
                    Struct superTypeTrait = new Struct(superTypeName);

                    if (MapUtils.isNotEmpty(trait.getValues())) {
                        AtlasClassificationType superType = typeRegistry.getClassificationTypeByName(superTypeName);

                        if (superType != null && MapUtils.isNotEmpty(superType.getAllAttributes())) {
                            Map<String, Object> superTypeTraitAttributes = new HashMap<>();

                            for (Map.Entry<String, Object> attrEntry : trait.getValues().entrySet()) {
                                String attrName = attrEntry.getKey();

                                if (superType.getAllAttributes().containsKey(attrName)) {
                                    superTypeTraitAttributes.put(attrName, attrEntry.getValue());
                                }
                            }

                            superTypeTrait.setValues(superTypeTraitAttributes);
                        }
                    }

                    ret.add(superTypeTrait);
                }
            }
        }

        return ret;
    }
}
