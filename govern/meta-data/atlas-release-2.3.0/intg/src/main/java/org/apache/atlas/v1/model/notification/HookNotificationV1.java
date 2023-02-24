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
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.model.notification.HookNotification;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.typedef.TypesDef;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Contains the structure of messages transferred from hooks to atlas.
 */
public class HookNotificationV1 {

    /**
     * Hook message for create type definitions.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class TypeRequest extends HookNotification implements Serializable {
        private static final long serialVersionUID = 1L;

        private TypesDef typesDef;

        public TypeRequest() {
        }

        public TypeRequest(HookNotificationType type, TypesDef typesDef, String user) {
            super(type, user);
            this.typesDef = typesDef;
        }

        public TypesDef getTypesDef() {
            return typesDef;
        }

        public void setTypesDef(TypesDef typesDef) {
            this.typesDef = typesDef;
        }

        @Override
        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("TypeRequest{");
            super.toString(sb);
            sb.append("typesDef=");
            if (typesDef != null) {
                typesDef.toString(sb);
            }
            sb.append("}");

            return sb;
        }
    }

    /**
     * Hook message for creating new entities.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class EntityCreateRequest extends HookNotification implements Serializable {
        private static final long serialVersionUID = 1L;

        private List<Referenceable> entities;

        public EntityCreateRequest() {
        }

        public EntityCreateRequest(String user, Referenceable... entities) {
            this(HookNotificationType.ENTITY_CREATE, Arrays.asList(entities), user);
        }

        public EntityCreateRequest(String user, List<Referenceable> entities) {
            this(HookNotificationType.ENTITY_CREATE, entities, user);
        }

        protected EntityCreateRequest(HookNotificationType type, List<Referenceable> entities, String user) {
            super(type, user);

            this.entities = entities;
        }

        public List<Referenceable> getEntities() {
            return entities;
        }

        public void setEntities(List<Referenceable> entities) {
            this.entities = entities;
        }

        @Override
        public void normalize() {
            super.normalize();

            if (entities != null) {
                for (Referenceable entity : entities) {
                    if (entity != null) {
                        entity.normalize();
                    }
                }
            }
        }

        @Override
        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("EntityCreateRequest{");
            super.toString(sb);
            sb.append("entities=[");
            AtlasBaseTypeDef.dumpObjects(getEntities(), sb);
            sb.append("]");
            sb.append("}");

            return sb;
        }
    }

    /**
     * Hook message for updating entities(full update).
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class EntityUpdateRequest extends EntityCreateRequest implements Serializable {
        private static final long serialVersionUID = 1L;

        public EntityUpdateRequest() {
        }

        public EntityUpdateRequest(String user, Referenceable... entities) {
            this(user, Arrays.asList(entities));
        }

        public EntityUpdateRequest(String user, List<Referenceable> entities) {
            super(HookNotificationType.ENTITY_FULL_UPDATE, entities, user);
        }

        @Override
        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("EntityUpdateRequest{");
            super.toString(sb);
            sb.append("entities=[");
            AtlasBaseTypeDef.dumpObjects(getEntities(), sb);
            sb.append("]");
            sb.append("}");

            return sb;
        }
    }

    /**
     * Hook message for updating entities(partial update).
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class EntityPartialUpdateRequest extends HookNotification {
        private static final long serialVersionUID = 1L;

        private String        typeName;
        private String        attribute;
        private String        attributeValue;
        private Referenceable entity;

        public EntityPartialUpdateRequest() {
        }

        public EntityPartialUpdateRequest(String user, String typeName, String attribute, String attributeValue, Referenceable entity) {
            super(HookNotificationType.ENTITY_PARTIAL_UPDATE, user);

            this.typeName       = typeName;
            this.attribute      = attribute;
            this.attributeValue = attributeValue;
            this.entity         = entity;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        public String getAttributeValue() {
            return attributeValue;
        }

        public void setAttributeValue(String attributeValue) {
            this.attributeValue = attributeValue;
        }

        public Referenceable getEntity() {
            return entity;
        }

        public void setEntity(Referenceable entity) {
            this.entity = entity;
        }

        @Override
        public void normalize() {
            super.normalize();

            if (entity != null) {
                entity.normalize();
            }
        }

        @Override
        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("EntityPartialUpdateRequest{");
            super.toString(sb);
            sb.append("typeName=").append(typeName);
            sb.append("attribute=").append(attribute);
            sb.append("attributeValue=").append(attributeValue);
            sb.append("entity=");
            if (entity != null) {
                entity.toString(sb);
            }
            sb.append("}");

            return sb;
        }
    }

    /**
     * Hook message for entity delete.
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class EntityDeleteRequest extends HookNotification implements Serializable {
        private static final long serialVersionUID = 1L;

        private String typeName;
        private String attribute;
        private String attributeValue;

        public EntityDeleteRequest() {
        }

        public EntityDeleteRequest(String user, String typeName, String attribute, String attributeValue) {
            this(HookNotificationType.ENTITY_DELETE, user, typeName, attribute, attributeValue);
        }

        protected EntityDeleteRequest(HookNotificationType type, String user, String typeName, String attribute, String attributeValue) {
            super(type, user);

            this.typeName       = typeName;
            this.attribute      = attribute;
            this.attributeValue = attributeValue;
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(String attribute) {
            this.attribute = attribute;
        }

        public String getAttributeValue() {
            return attributeValue;
        }

        public void setAttributeValue(String attributeValue) {
            this.attributeValue = attributeValue;
        }

        @Override
        public StringBuilder toString(StringBuilder sb) {
            if (sb == null) {
                sb = new StringBuilder();
            }

            sb.append("EntityDeleteRequest{");
            super.toString(sb);
            sb.append("typeName=").append(typeName);
            sb.append("attribute=").append(attribute);
            sb.append("attributeValue=").append(attributeValue);
            sb.append("}");

            return sb;
        }
    }
}
