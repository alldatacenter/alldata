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
package org.apache.atlas.model.notification;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasRelationshipHeader;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
import static org.apache.atlas.model.notification.EntityNotification.EntityNotificationType.ENTITY_NOTIFICATION_V2;

/**
 * Base type of hook message.
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class EntityNotification implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * Type of the hook message.
     */
    public enum EntityNotificationType {
        ENTITY_NOTIFICATION_V1, ENTITY_NOTIFICATION_V2
    }

    protected EntityNotificationType type;

    public EntityNotification() {
        this.type = EntityNotificationType.ENTITY_NOTIFICATION_V1;
    }

    public EntityNotification(EntityNotificationType type) {
        this.type = type;
    }

    public EntityNotificationType getType() {
        return type;
    }

    public void setType(EntityNotificationType type) {
        this.type = type;
    }

    public void normalize() { }

    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    public StringBuilder toString(StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }

        sb.append("EntityNotification{");
        sb.append("type=").append(type);
        sb.append("}");

        return sb;
    }

    /**
     * Entity v2 notification
     */
    @JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
    @JsonSerialize(include=JsonSerialize.Inclusion.NON_NULL)
    @JsonIgnoreProperties(ignoreUnknown=true)
    @XmlRootElement
    @XmlAccessorType(XmlAccessType.PROPERTY)
    public static class EntityNotificationV2 extends EntityNotification implements Serializable {
        private static final long serialVersionUID = 1L;

        public enum OperationType {
            ENTITY_CREATE, ENTITY_UPDATE, ENTITY_DELETE,
            CLASSIFICATION_ADD, CLASSIFICATION_DELETE, CLASSIFICATION_UPDATE,
            RELATIONSHIP_CREATE, RELATIONSHIP_UPDATE, RELATIONSHIP_DELETE
        }

        private AtlasEntityHeader entity;
        private AtlasRelationshipHeader relationship;
        private OperationType     operationType;
        private long              eventTime;

        public EntityNotificationV2() {
            super(ENTITY_NOTIFICATION_V2);

            setEntity(null);
            setOperationType(null);
            setEventTime(System.currentTimeMillis());
        }

        public EntityNotificationV2(AtlasEntityHeader entity, OperationType operationType) {
            this(entity, operationType, System.currentTimeMillis());
        }

        public EntityNotificationV2(AtlasEntityHeader entity, OperationType operationType, long eventTime) {
            super(ENTITY_NOTIFICATION_V2);

            setEntity(entity);
            setOperationType(operationType);
            setEventTime(eventTime);
        }

        public EntityNotificationV2(AtlasRelationshipHeader relationship, OperationType operationType, long eventTime) {
            super(ENTITY_NOTIFICATION_V2);

            setRelationship(relationship);
            setOperationType(operationType);
            setEventTime(eventTime);
        }

        public AtlasEntityHeader getEntity() {
            return entity;
        }

        public void setEntity(AtlasEntityHeader entity) {
            this.entity = entity;
        }

        public AtlasRelationshipHeader getRelationship() {
            return relationship;
        }

        public void setRelationship(AtlasRelationshipHeader relationship) {
            this.relationship = relationship;
        }

        public OperationType getOperationType() {
            return operationType;
        }

        public void setOperationType(OperationType operationType) {
            this.operationType = operationType;
        }

        public long getEventTime() {
            return eventTime;
        }

        public void setEventTime(long eventTime) {
            this.eventTime = eventTime;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) { return true; }
            if (o == null || getClass() != o.getClass()) { return false; }
            EntityNotificationV2 that = (EntityNotificationV2) o;
            return Objects.equals(type, that.type) &&
                   Objects.equals(entity, that.entity) &&
                   operationType == that.operationType;
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, entity, operationType);
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
            sb.append(", eventTime=").append(eventTime);
            sb.append("}");

            return sb;
        }
    }
}
