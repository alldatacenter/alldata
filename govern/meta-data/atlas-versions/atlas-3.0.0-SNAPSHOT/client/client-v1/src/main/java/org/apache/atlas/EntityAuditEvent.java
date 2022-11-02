/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.type.AtlasType;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;

/**
 * Structure of entity audit event
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class EntityAuditEvent implements Serializable {
    public enum EntityAuditAction {
        ENTITY_CREATE, ENTITY_UPDATE, ENTITY_DELETE, TAG_ADD, TAG_DELETE, TAG_UPDATE,
        PROPAGATED_TAG_ADD, PROPAGATED_TAG_DELETE, PROPAGATED_TAG_UPDATE,
        ENTITY_IMPORT_CREATE, ENTITY_IMPORT_UPDATE, ENTITY_IMPORT_DELETE,
        TERM_ADD, TERM_DELETE, LABEL_ADD, LABEL_DELETE;

        public static EntityAuditAction fromString(String strValue) {
            switch (strValue) {
                case "ENTITY_CREATE":
                    return ENTITY_CREATE;
                case "ENTITY_UPDATE":
                    return ENTITY_UPDATE;
                case "ENTITY_DELETE":
                    return ENTITY_DELETE;
                case "ENTITY_IMPORT_CREATE":
                    return ENTITY_IMPORT_CREATE;
                case "ENTITY_IMPORT_UPDATE":
                    return ENTITY_IMPORT_UPDATE;
                case "ENTITY_IMPORT_DELETE":
                    return ENTITY_IMPORT_DELETE;
                case "CLASSIFICATION_ADD":
                case "TAG_ADD":
                    return TAG_ADD;
                case "CLASSIFICATION_DELETE":
                case "TAG_DELETE":
                    return TAG_DELETE;
                case "CLASSIFICATION_UPDATE":
                case "TAG_UPDATE":
                    return TAG_UPDATE;
                case "PROPAGATED_TAG_ADD":
                    return PROPAGATED_TAG_ADD;
                case "PROPAGATED_TAG_DELETE":
                    return PROPAGATED_TAG_DELETE;
                case "PROPAGATED_TAG_UPDATE":
                    return PROPAGATED_TAG_UPDATE;
                case "TERM_ADD":
                    return TERM_ADD;
                case "TERM_DELETE":
                    return TERM_DELETE;
                case "LABEL_ADD":
                    return LABEL_ADD;
                case "LABEL_DELETE":
                    return LABEL_DELETE;
            }

            throw new IllegalArgumentException("No enum constant " + EntityAuditAction.class.getCanonicalName() + "." + strValue);
        }
    }

    private String entityId;
    private long timestamp;
    private String user;
    private EntityAuditAction action;
    private String details;
    private String eventKey;
    private Referenceable entityDefinition;

    public EntityAuditEvent() {
    }

    public EntityAuditEvent(String entityId, Long ts, String user, EntityAuditAction action, String details,
                            Referenceable entityDefinition) throws AtlasException {
        this.entityId = entityId;
        this.timestamp = ts;
        this.user = user;
        this.action = action;
        this.details = details;
        this.entityDefinition = entityDefinition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityAuditEvent that = (EntityAuditEvent) o;
        return timestamp == that.timestamp &&
                Objects.equals(entityId, that.entityId) &&
                Objects.equals(user, that.user) &&
                action == that.action &&
                Objects.equals(details, that.details) &&
                Objects.equals(eventKey, that.eventKey) &&
                Objects.equals(entityDefinition, that.entityDefinition);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, timestamp, user, action, details, eventKey, entityDefinition);
    }

    @Override
    public String toString() {
        return AtlasType.toV1Json(this);
    }

    public String getEntityId() {
        return entityId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }

    public EntityAuditAction getAction() {
        return action;
    }

    public String getDetails() {
        return details;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public void setAction(EntityAuditAction action) {
        this.action = action;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public Referenceable getEntityDefinition() {
        return entityDefinition;
    }

    public void setEntityDefinition(Referenceable entityDefinition) {
        this.entityDefinition = entityDefinition;
    }

    @JsonIgnore
    public String getEntityDefinitionString() {
        if (entityDefinition != null) {
            return AtlasType.toV1Json(entityDefinition);
        }
        return null;
    }

    @JsonIgnore
    public void setEntityDefinition(String entityDefinition) {
        this.entityDefinition = AtlasType.fromV1Json(entityDefinition, Referenceable.class);
    }

    @JsonIgnore
    public static EntityAuditEvent fromString(String eventString) {
        return AtlasType.fromV1Json(eventString, EntityAuditEvent.class);
    }
}
