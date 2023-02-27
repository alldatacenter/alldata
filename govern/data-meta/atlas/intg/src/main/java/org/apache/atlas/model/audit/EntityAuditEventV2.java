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
package org.apache.atlas.model.audit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.apache.atlas.model.Clearable;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.type.AtlasType;
import org.apache.commons.lang.StringUtils;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.PUBLIC_ONLY;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditType.ENTITY_AUDIT_V2;

/**
 * Structure of v2 entity audit event
 */
@JsonAutoDetect(getterVisibility=PUBLIC_ONLY, setterVisibility=PUBLIC_ONLY, fieldVisibility=NONE)
@JsonSerialize(include=JsonSerialize.Inclusion.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown=true)
@XmlRootElement
@XmlAccessorType(XmlAccessType.PROPERTY)
public class EntityAuditEventV2 implements Serializable, Clearable {
    public static final String SORT_COLUMN_USER      = "user";
    public static final String SORT_COLUMN_ACTION    = "action";
    public static final String SORT_COLUMN_TIMESTAMP = "timestamp";

    public enum EntityAuditType { ENTITY_AUDIT_V1, ENTITY_AUDIT_V2 }

    public enum EntityAuditActionV2 {
        ENTITY_CREATE, ENTITY_UPDATE, ENTITY_DELETE,
        ENTITY_IMPORT_CREATE, ENTITY_IMPORT_UPDATE, ENTITY_IMPORT_DELETE,
        CLASSIFICATION_ADD, CLASSIFICATION_DELETE, CLASSIFICATION_UPDATE,
        PROPAGATED_CLASSIFICATION_ADD, PROPAGATED_CLASSIFICATION_DELETE, PROPAGATED_CLASSIFICATION_UPDATE,
        TERM_ADD, TERM_DELETE, LABEL_ADD, LABEL_DELETE, ENTITY_PURGE,
        BUSINESS_ATTRIBUTE_UPDATE, CUSTOM_ATTRIBUTE_UPDATE;

        public static EntityAuditActionV2 fromString(String strValue) {
            switch (strValue) {
                case "ENTITY_CREATE":
                    return ENTITY_CREATE;
                case "ENTITY_UPDATE":
                    return ENTITY_UPDATE;
                case "ENTITY_DELETE":
                    return ENTITY_DELETE;
                case "ENTITY_PURGE":
                    return ENTITY_PURGE;
                case "ENTITY_IMPORT_CREATE":
                    return ENTITY_IMPORT_CREATE;
                case "ENTITY_IMPORT_UPDATE":
                    return ENTITY_IMPORT_UPDATE;
                case "ENTITY_IMPORT_DELETE":
                    return ENTITY_IMPORT_DELETE;
                case "CLASSIFICATION_ADD":
                case "TAG_ADD":
                    return CLASSIFICATION_ADD;
                case "CLASSIFICATION_DELETE":
                case "TAG_DELETE":
                    return CLASSIFICATION_DELETE;
                case "CLASSIFICATION_UPDATE":
                case "TAG_UPDATE":
                    return CLASSIFICATION_UPDATE;
                case "PROPAGATED_CLASSIFICATION_ADD":
                    return PROPAGATED_CLASSIFICATION_ADD;
                case "PROPAGATED_CLASSIFICATION_DELETE":
                    return PROPAGATED_CLASSIFICATION_DELETE;
                case "PROPAGATED_CLASSIFICATION_UPDATE":
                    return PROPAGATED_CLASSIFICATION_UPDATE;
                case "TERM_ADD":
                    return TERM_ADD;
                case "TERM_DELETE":
                    return TERM_DELETE;
                case "LABEL_ADD":
                    return LABEL_ADD;
                case "LABEL_DELETE":
                    return LABEL_DELETE;
                case "BUSINESS_ATTRIBUTE_UPDATE":
                    return BUSINESS_ATTRIBUTE_UPDATE;
                case "CUSTOM_ATTRIBUTE_UPDATE":
                    return CUSTOM_ATTRIBUTE_UPDATE;
            }

            throw new IllegalArgumentException("No enum constant " + EntityAuditActionV2.class.getCanonicalName() + "." + strValue);
        }
    }

    private String              entityId;
    private long                timestamp;
    private String              user;
    private EntityAuditActionV2 action;
    private String              details;
    private String              eventKey;
    private AtlasEntity         entity;
    private EntityAuditType     type;

    public EntityAuditEventV2() { }

    public EntityAuditEventV2(String entityId, long timestamp, String user, EntityAuditActionV2 action, String details,
                              AtlasEntity entity) {
        this(entityId, timestamp, user, action, details, entity, ENTITY_AUDIT_V2);
    }

    public EntityAuditEventV2(String entityId, long timestamp, String user, EntityAuditActionV2 action, String details,
                              AtlasEntity entity, EntityAuditType auditType) {
        setEntityId(entityId);
        setTimestamp(timestamp);
        setUser(user);
        setAction(action);
        setDetails(details);
        setEntity(entity);
        setType(auditType);
    }

    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public EntityAuditActionV2 getAction() {
        return action;
    }

    public void setAction(EntityAuditActionV2 action) {
        this.action = action;
    }

    public String getDetails() {
        return details;
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

    public AtlasEntity getEntity() {
        return entity;
    }

    public void setEntity(AtlasEntity entity) {
        this.entity = entity;
    }

    public EntityAuditType getType() {
        return type;
    }

    public void setType(EntityAuditType type) {
        this.type = type;
    }

    @JsonIgnore
    public String getEntityDefinitionString() {
        if (entity != null) {
            return AtlasType.toJson(entity);
        }

        return null;
    }

    @JsonIgnore
    public void setEntityDefinition(String entityDefinition) {
        this.entity = AtlasType.fromJson(entityDefinition, AtlasEntity.class);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        EntityAuditEventV2 that = (EntityAuditEventV2) o;

        return timestamp == that.timestamp &&
               Objects.equals(entityId, that.entityId) &&
               Objects.equals(user, that.user) &&
               action == that.action &&
               Objects.equals(details, that.details) &&
               Objects.equals(eventKey, that.eventKey) &&
               Objects.equals(entity, that.entity) &&
               Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
        return Objects.hash(entityId, timestamp, user, action, details, eventKey, entity, type);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("EntityAuditEventV2{");

        sb.append("entityId='").append(entityId).append('\'');
        sb.append(", timestamp=").append(timestamp);
        sb.append(", user='").append(user).append('\'');
        sb.append(", action=").append(action);
        sb.append(", details='").append(details).append('\'');
        sb.append(", eventKey='").append(eventKey).append('\'');
        sb.append(", entity=").append(entity);
        sb.append(", type=").append(type);
        sb.append('}');

        return sb.toString();
    }

    @JsonIgnore
    public AtlasEntityHeader getEntityHeader() {
        AtlasEntityHeader ret = null;
        String jsonPartFromDetails = getJsonPartFromDetails();
        if(StringUtils.isNotEmpty(jsonPartFromDetails)) {
            ret = AtlasType.fromJson(jsonPartFromDetails, AtlasEntityHeader.class);
        }
        return ret;
    }

    @JsonIgnore
    @Override
    public void clear() {
        entityId = null;
        timestamp = 0L;
        user = null;
        action = null;
        details = null;
        eventKey = null;
        entity = null;
        type = null;
    }

    private String getJsonPartFromDetails() {
        String ret = null;
        if(StringUtils.isNotEmpty(details)) {
            int bracketStartPosition = details.indexOf("{");
            if(bracketStartPosition != -1) {
                ret = details.substring(bracketStartPosition);
            }
        }

        return ret;
    }

    public static class UserComparator implements Comparator<EntityAuditEventV2> {
        @Override
        public int compare(EntityAuditEventV2 me, EntityAuditEventV2 other) {
            int ret = String.CASE_INSENSITIVE_ORDER.compare(me.getUser(), other.getUser());

            if (ret == 0) {
                ret = Long.compare(me.getTimestamp(), other.getTimestamp());
            }

            return ret;
        }
    }

    public static class ActionComparator implements Comparator<EntityAuditEventV2> {
        @Override
        public int compare(EntityAuditEventV2 me, EntityAuditEventV2 other) {
            int ret = String.CASE_INSENSITIVE_ORDER.compare(me.getAction().toString(), other.getAction().toString());

            if (ret == 0) {
                ret = Long.compare(me.getTimestamp(), other.getTimestamp());
            }

            return ret;
        }
    }

    public static class TimestampComparator implements Comparator<EntityAuditEventV2> {
        @Override
        public int compare(EntityAuditEventV2 me, EntityAuditEventV2 other) {
            return Long.compare(me.getTimestamp(), other.getTimestamp());
        }
    }

    public static void sortEvents(List<EntityAuditEventV2> events, String sortByColumn, boolean sortOrderDesc) {
        final Comparator<EntityAuditEventV2> comparator;

        switch (sortByColumn.toLowerCase()) {
            case SORT_COLUMN_USER:
                comparator = new EntityAuditEventV2.UserComparator();
                break;

            case SORT_COLUMN_ACTION:
                comparator = new EntityAuditEventV2.ActionComparator();
                break;

            case SORT_COLUMN_TIMESTAMP:
            default:
                comparator = new EntityAuditEventV2.TimestampComparator();
                break;
        }

        events.sort(sortOrderDesc ? comparator.reversed() : comparator);
    }
}