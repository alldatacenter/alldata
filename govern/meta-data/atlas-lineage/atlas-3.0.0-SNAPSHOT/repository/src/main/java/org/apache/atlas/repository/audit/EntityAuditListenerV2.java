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
package org.apache.atlas.repository.audit;

import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.EntityAuditEvent.EntityAuditAction;
import org.apache.atlas.RequestContext;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.listener.EntityChangeListenerV2;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.repository.converters.AtlasInstanceConverter;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.AtlasPerfMetrics.MetricRecorder;
import org.apache.atlas.utils.FixedBufferList;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.atlas.AtlasConfiguration.STORE_DIFFERENTIAL_AUDITS;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.BUSINESS_ATTRIBUTE_UPDATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_ADD;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_DELETE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_UPDATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.ENTITY_CREATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.ENTITY_DELETE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.ENTITY_PURGE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.ENTITY_IMPORT_CREATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.ENTITY_IMPORT_DELETE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.ENTITY_IMPORT_UPDATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.ENTITY_UPDATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.CUSTOM_ATTRIBUTE_UPDATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.LABEL_ADD;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.LABEL_DELETE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.PROPAGATED_CLASSIFICATION_ADD;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.PROPAGATED_CLASSIFICATION_DELETE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.PROPAGATED_CLASSIFICATION_UPDATE;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.TERM_ADD;
import static org.apache.atlas.model.audit.EntityAuditEventV2.EntityAuditActionV2.TERM_DELETE;

@Component
public class EntityAuditListenerV2 implements EntityChangeListenerV2 {
    private static final Logger LOG = LoggerFactory.getLogger(EntityAuditListenerV2.class);
    private static final ThreadLocal<FixedBufferList<EntityAuditEventV2>> AUDIT_EVENTS_BUFFER =
            ThreadLocal.withInitial(() -> new FixedBufferList<>(EntityAuditEventV2.class,
                    AtlasConfiguration.NOTIFICATION_FIXED_BUFFER_ITEMS_INCREMENT_COUNT.getInt()));

    private final EntityAuditRepository  auditRepository;
    private final AtlasTypeRegistry      typeRegistry;
    private final AtlasInstanceConverter instanceConverter;

    @Inject
    public EntityAuditListenerV2(EntityAuditRepository auditRepository, AtlasTypeRegistry typeRegistry, AtlasInstanceConverter instanceConverter) {
        this.auditRepository   = auditRepository;
        this.typeRegistry      = typeRegistry;
        this.instanceConverter = instanceConverter;
    }

    @Override
    public void onEntitiesAdded(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException {
        MetricRecorder metric = RequestContext.get().startMetricRecord("entityAudit");

        FixedBufferList<EntityAuditEventV2> entitiesAdded = getAuditEventsList();
        for (AtlasEntity entity : entities) {
            createEvent(entitiesAdded.next(), entity, isImport ? ENTITY_IMPORT_CREATE : ENTITY_CREATE);
        }

        auditRepository.putEventsV2(entitiesAdded.toList());

        RequestContext.get().endMetricRecord(metric);
    }

    @Override
    public void onEntitiesUpdated(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException {
        RequestContext                      reqContext    = RequestContext.get();
        MetricRecorder                      metric        = reqContext.startMetricRecord("entityAudit");
        FixedBufferList<EntityAuditEventV2> updatedEvents = getAuditEventsList();
        Collection<AtlasEntity>             updatedEntites;

        if (STORE_DIFFERENTIAL_AUDITS.getBoolean()) {
            updatedEntites = reqContext.getDifferentialEntities();
        } else {
            updatedEntites = entities;
        }

        for (AtlasEntity entity : updatedEntites) {
            final EntityAuditActionV2 action;

            if (isImport) {
                action = ENTITY_IMPORT_UPDATE;
            } else if (reqContext.checkIfEntityIsForCustomAttributeUpdate(entity.getGuid())) {
                action = CUSTOM_ATTRIBUTE_UPDATE;
            } else if (reqContext.checkIfEntityIsForBusinessAttributeUpdate(entity.getGuid())) {
                action = BUSINESS_ATTRIBUTE_UPDATE;
            } else {
                action = ENTITY_UPDATE;
            }

            createEvent(updatedEvents.next(), entity, action);
        }

        auditRepository.putEventsV2(updatedEvents.toList());

        RequestContext.get().endMetricRecord(metric);
    }

    @Override
    public void onEntitiesDeleted(List<AtlasEntity> entities, boolean isImport) throws AtlasBaseException {
        MetricRecorder metric = RequestContext.get().startMetricRecord("entityAudit");

        FixedBufferList<EntityAuditEventV2> deletedEntities = getAuditEventsList();
        for (AtlasEntity entity : entities) {
            createEvent(deletedEntities.next(), entity, isImport ? ENTITY_IMPORT_DELETE : ENTITY_DELETE, "Deleted entity");
        }

        auditRepository.putEventsV2(deletedEntities.toList());

        RequestContext.get().endMetricRecord(metric);
    }

    @Override
    public void onEntitiesPurged(List<AtlasEntity> entities) throws AtlasBaseException {
        MetricRecorder metric = RequestContext.get().startMetricRecord("entityAudit");

        FixedBufferList<EntityAuditEventV2> eventsPurged = getAuditEventsList();
        for (AtlasEntity entity : entities) {
            createEvent(eventsPurged.next(), entity, ENTITY_PURGE);
        }

        auditRepository.putEventsV2(eventsPurged.toList());

        RequestContext.get().endMetricRecord(metric);
    }

    @Override
    public void onClassificationsAdded(AtlasEntity entity, List<AtlasClassification> classifications) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(classifications)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("entityAudit");

            FixedBufferList<EntityAuditEventV2> classificationsAdded = getAuditEventsList();
            for (AtlasClassification classification : classifications) {
                if (entity.getGuid().equals(classification.getEntityGuid())) {
                    createEvent(classificationsAdded.next(), entity, CLASSIFICATION_ADD, "Added classification: " + AtlasType.toJson(classification));
                } else {
                    createEvent(classificationsAdded.next(), entity, PROPAGATED_CLASSIFICATION_ADD, "Added propagated classification: " + AtlasType.toJson(classification));
                }
            }

            auditRepository.putEventsV2(classificationsAdded.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onClassificationsAdded(List<AtlasEntity> entities, List<AtlasClassification> classifications) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(classifications)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("entityAudit");
            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();

            for (AtlasClassification classification : classifications) {
                for (AtlasEntity entity : entities) {
                    if (entity.getGuid().equals(classification.getEntityGuid())) {
                        createEvent(events.next(), entity, CLASSIFICATION_ADD, "Added classification: " + AtlasType.toJson(classification));
                    } else {
                        createEvent(events.next(), entity, PROPAGATED_CLASSIFICATION_ADD, "Added propagated classification: " + AtlasType.toJson(classification));
                    }
                }
            }

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onClassificationsUpdated(AtlasEntity entity, List<AtlasClassification> classifications) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(classifications)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("entityAudit");

            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();
            String guid = entity.getGuid();

            for (AtlasClassification classification : classifications) {
                if (guid.equals(classification.getEntityGuid())) {
                    createEvent(events.next(), entity, CLASSIFICATION_UPDATE, "Updated classification: " + AtlasType.toJson(classification));
                } else {
                    if (isPropagatedClassificationAdded(guid, classification)) {
                        createEvent(events.next(), entity, PROPAGATED_CLASSIFICATION_ADD, "Added propagated classification: " + AtlasType.toJson(classification));
                    } else if (isPropagatedClassificationDeleted(guid, classification)) {
                        createEvent(events.next(), entity, PROPAGATED_CLASSIFICATION_DELETE, "Deleted propagated classification: " + classification.getTypeName());
                    } else {
                        createEvent(events.next(), entity, PROPAGATED_CLASSIFICATION_UPDATE, "Updated propagated classification: " + AtlasType.toJson(classification));
                    }
                }
            }

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onClassificationsDeleted(AtlasEntity entity, List<AtlasClassification> classifications) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(classifications)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("onClassificationsDeleted");

            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();

            for (AtlasClassification classification : classifications) {
                if (StringUtils.equals(entity.getGuid(), classification.getEntityGuid())) {
                    createEvent(events.next(), entity, CLASSIFICATION_DELETE, "Deleted classification: " + classification.getTypeName());
                } else {
                    createEvent(events.next(), entity, PROPAGATED_CLASSIFICATION_DELETE, "Deleted propagated classification: " + classification.getTypeName());
                }
            }

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onClassificationsDeleted(List<AtlasEntity> entities, List<AtlasClassification> classifications) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(classifications) && CollectionUtils.isNotEmpty(entities)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("onClassificationsDeleted");
            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();

            for (AtlasClassification classification : classifications) {
                for (AtlasEntity entity : entities) {
                    if (StringUtils.equals(entity.getGuid(), classification.getEntityGuid())) {
                        createEvent(events.next(), entity, CLASSIFICATION_DELETE, "Deleted classification: " + classification.getTypeName());
                    } else {
                        createEvent(events.next(), entity, PROPAGATED_CLASSIFICATION_DELETE, "Deleted propagated classification: " + classification.getTypeName());
                    }
                }
            }

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onTermAdded(AtlasGlossaryTerm term, List<AtlasRelatedObjectId> entities) throws AtlasBaseException {
        if (term != null && CollectionUtils.isNotEmpty(entities)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("onTermAdded");

            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();

            for (AtlasRelatedObjectId relatedObjectId : entities) {
                AtlasEntity entity = instanceConverter.getAndCacheEntity(relatedObjectId.getGuid());

                if (entity != null) {
                    createEvent(events.next(), entity, TERM_ADD, "Added term: " + term.toAuditString());
                }
            }

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onTermDeleted(AtlasGlossaryTerm term, List<AtlasRelatedObjectId> entities) throws AtlasBaseException {
        if (term != null && CollectionUtils.isNotEmpty(entities)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("onTermDeleted");

            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();

            for (AtlasRelatedObjectId relatedObjectId : entities) {
                AtlasEntity entity = instanceConverter.getAndCacheEntity(relatedObjectId.getGuid());

                if (entity != null) {
                    createEvent(events.next(), entity, TERM_DELETE, "Deleted term: " + term.toAuditString());
                }
            }

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onLabelsAdded(AtlasEntity entity, Set<String> labels) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(labels)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("onLabelsAdded");

            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();

            String addedLabels = StringUtils.join(labels, " ");

            createEvent(events.next(), entity, LABEL_ADD, "Added labels: " + addedLabels);

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onLabelsDeleted(AtlasEntity entity, Set<String> labels) throws AtlasBaseException {
        if (CollectionUtils.isNotEmpty(labels)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("onLabelsDeleted");

            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();

            String deletedLabels = StringUtils.join(labels, " ");

            createEvent(events.next(), entity, LABEL_DELETE, "Deleted labels: " + deletedLabels);

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }

    @Override
    public void onRelationshipsAdded(List<AtlasRelationship> relationships, boolean isImport) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("New relationship(s) added to repository(" + relationships.size() + ")");
        }
    }

    @Override
    public void onRelationshipsUpdated(List<AtlasRelationship> relationships, boolean isImport) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Relationship(s) updated(" + relationships.size() + ")");
        }
    }

    @Override
    public void onRelationshipsDeleted(List<AtlasRelationship> relationships, boolean isImport) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Relationship(s) deleted from repository(" + relationships.size() + ")");
        }
    }

    @Override
    public void onRelationshipsPurged(List<AtlasRelationship> relationships) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Relationship(s) purged from repository(" + relationships.size() + ")");
        }
    }

    @Override
    public void onBusinessAttributesUpdated(AtlasEntity entity, Map<String, Map<String, Object>> updatedBusinessAttributes) throws AtlasBaseException {
        if (MapUtils.isNotEmpty(updatedBusinessAttributes)) {
            MetricRecorder metric = RequestContext.get().startMetricRecord("entityAudit");

            FixedBufferList<EntityAuditEventV2> events = getAuditEventsList();

            for (Map.Entry<String, Map<String, Object>> entry : updatedBusinessAttributes.entrySet()) {
                String              bmName     = entry.getKey();
                Map<String, Object> attributes = entry.getValue();
                String              details    = AtlasJson.toJson(new AtlasStruct(bmName, attributes));

                createEvent(events.next(), entity, BUSINESS_ATTRIBUTE_UPDATE, "Updated business attributes: " + details);
            }

            auditRepository.putEventsV2(events.toList());

            RequestContext.get().endMetricRecord(metric);
        }
    }


    private EntityAuditEventV2 createEvent(EntityAuditEventV2 entityAuditEventV2, AtlasEntity entity, EntityAuditActionV2 action, String details) {
        entityAuditEventV2.setEntityId(entity.getGuid());
        entityAuditEventV2.setTimestamp(System.currentTimeMillis());
        entityAuditEventV2.setUser(RequestContext.get().getUser());
        entityAuditEventV2.setAction(action);
        entityAuditEventV2.setDetails(details);
        entityAuditEventV2.setEntity(entity);

        return entityAuditEventV2;
    }

    private EntityAuditEventV2 createEvent(EntityAuditEventV2 event, AtlasEntity entity, EntityAuditActionV2 action) {
        String detail = getAuditEventDetail(entity, action);

        return createEvent(event, entity, action, detail);
    }

    private String getAuditEventDetail(AtlasEntity entity, EntityAuditActionV2 action) {
        Map<String, Object> prunedAttributes = pruneEntityAttributesForAudit(entity);

        String auditPrefix  = getV2AuditPrefix(action);
        String auditString  = auditPrefix + AtlasType.toJson(entity);
        byte[] auditBytes   = auditString.getBytes(StandardCharsets.UTF_8);
        long   auditSize    = auditBytes != null ? auditBytes.length : 0;
        long   auditMaxSize = auditRepository.repositoryMaxSize();

        if (auditMaxSize >= 0 && auditSize > auditMaxSize) { // don't store attributes in audit
            LOG.warn("audit record too long: entityType={}, guid={}, size={}; maxSize={}. entity attribute values not stored in audit",
                    entity.getTypeName(), entity.getGuid(), auditSize, auditMaxSize);

            Map<String, Object> attrValues    = entity.getAttributes();
            Map<String, Object> relAttrValues = entity.getRelationshipAttributes();

            entity.setAttributes(null);
            entity.setRelationshipAttributes(null);

            auditString = auditPrefix + AtlasType.toJson(entity);
            auditBytes  = auditString.getBytes(StandardCharsets.UTF_8); // recheck auditString size
            auditSize   = auditBytes != null ? auditBytes.length : 0;

            if (auditMaxSize >= 0 && auditSize > auditMaxSize) { // don't store classifications and meanings as well
                LOG.warn("audit record still too long: entityType={}, guid={}, size={}; maxSize={}. audit will have only summary details",
                        entity.getTypeName(), entity.getGuid(), auditSize, auditMaxSize);

                AtlasEntity shallowEntity = new AtlasEntity();

                shallowEntity.setGuid(entity.getGuid());
                shallowEntity.setTypeName(entity.getTypeName());
                shallowEntity.setCreateTime(entity.getCreateTime());
                shallowEntity.setUpdateTime(entity.getUpdateTime());
                shallowEntity.setCreatedBy(entity.getCreatedBy());
                shallowEntity.setUpdatedBy(entity.getUpdatedBy());
                shallowEntity.setStatus(entity.getStatus());
                shallowEntity.setVersion(entity.getVersion());

                auditString = auditPrefix + AtlasType.toJson(shallowEntity);
            }

            entity.setAttributes(attrValues);
            entity.setRelationshipAttributes(relAttrValues);
        }

        restoreEntityAttributes(entity, prunedAttributes);

        return auditString;
    }

    private boolean isPropagatedClassificationAdded(String guid, AtlasClassification classification) {
        Map<String, List<AtlasClassification>> addedPropagations = RequestContext.get().getAddedPropagations();

        return hasPropagatedEntry(addedPropagations, guid, classification);
    }

    private boolean isPropagatedClassificationDeleted(String guid, AtlasClassification classification) {
        Map<String, List<AtlasClassification>> removedPropagations = RequestContext.get().getRemovedPropagations();

        return hasPropagatedEntry(removedPropagations, guid, classification);
    }

    private boolean hasPropagatedEntry(Map<String, List<AtlasClassification>> propagationsMap, String guid, AtlasClassification classification) {
        boolean ret = false;

        if (MapUtils.isNotEmpty(propagationsMap) && propagationsMap.containsKey(guid) && CollectionUtils.isNotEmpty(propagationsMap.get(guid))) {
            List<AtlasClassification> classifications    = propagationsMap.get(guid);
            String                    classificationName = classification.getTypeName();
            String                    entityGuid         = classification.getEntityGuid();

            for (AtlasClassification c : classifications) {
                if (StringUtils.equals(c.getTypeName(), classificationName) && StringUtils.equals(c.getEntityGuid(), entityGuid)) {
                    ret = true;
                    break;
                }
            }
        }

        return ret;
    }

    private Map<String, Object> pruneEntityAttributesForAudit(AtlasEntity entity) {
        Map<String, Object> ret               = null;
        Map<String, Object> entityAttributes  = entity.getAttributes();
        List<String>        excludeAttributes = auditRepository.getAuditExcludeAttributes(entity.getTypeName());
        AtlasEntityType     entityType        = typeRegistry.getEntityTypeByName(entity.getTypeName());

        if (CollectionUtils.isNotEmpty(excludeAttributes) && MapUtils.isNotEmpty(entityAttributes) && entityType != null) {
            for (AtlasAttribute attribute : entityType.getAllAttributes().values()) {
                String attrName  = attribute.getName();
                Object attrValue = entityAttributes.get(attrName);

                if (excludeAttributes.contains(attrName)) {
                    if (ret == null) {
                        ret = new HashMap<>();
                    }

                    ret.put(attrName, attrValue);
                    entityAttributes.remove(attrName);
                }
            }
        }

        return ret;
    }

    private void restoreEntityAttributes(AtlasEntity entity, Map<String, Object> prunedAttributes) {
        if (MapUtils.isEmpty(prunedAttributes)) {
            return;
        }

        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entity.getTypeName());

        if (entityType != null && MapUtils.isNotEmpty(entityType.getAllAttributes())) {
            for (AtlasAttribute attribute : entityType.getAllAttributes().values()) {
                String attrName = attribute.getName();

                if (prunedAttributes.containsKey(attrName)) {
                    entity.setAttribute(attrName, prunedAttributes.get(attrName));
                }
            }
        }
    }

    private String getV1AuditPrefix(EntityAuditAction action) {
        final String ret;

        switch (action) {
            case ENTITY_CREATE:
                ret = "Created: ";
                break;
            case ENTITY_UPDATE:
                ret = "Updated: ";
                break;
            case ENTITY_DELETE:
                ret = "Deleted: ";
                break;
            case TAG_ADD:
                ret = "Added classification: ";
                break;
            case TAG_DELETE:
                ret = "Deleted classification: ";
                break;
            case TAG_UPDATE:
                ret = "Updated classification: ";
                break;
            case ENTITY_IMPORT_CREATE:
                ret = "Created by import: ";
                break;
            case ENTITY_IMPORT_UPDATE:
                ret = "Updated by import: ";
                break;
            case ENTITY_IMPORT_DELETE:
                ret = "Deleted by import: ";
                break;
            case TERM_ADD:
                ret = "Added term: ";
                break;
            case TERM_DELETE:
                ret = "Deleted term: ";
                break;
            default:
                ret = "Unknown: ";
        }

        return ret;
    }

    private String getV2AuditPrefix(EntityAuditActionV2 action) {
        final String ret;

        switch (action) {
            case ENTITY_CREATE:
                ret = "Created: ";
                break;
            case ENTITY_UPDATE:
                ret = "Updated: ";
                break;
            case CUSTOM_ATTRIBUTE_UPDATE:
                ret = "Updated custom attribute: ";
                break;
            case BUSINESS_ATTRIBUTE_UPDATE:
                ret = "Updated business attribute: ";
                break;
            case ENTITY_DELETE:
                ret = "Deleted: ";
                break;
            case ENTITY_PURGE:
                ret = "Purged: ";
                break;
            case CLASSIFICATION_ADD:
                ret = "Added classification: ";
                break;
            case CLASSIFICATION_DELETE:
                ret = "Deleted classification: ";
                break;
            case CLASSIFICATION_UPDATE:
                ret = "Updated classification: ";
                break;
            case ENTITY_IMPORT_CREATE:
                ret = "Created by import: ";
                break;
            case ENTITY_IMPORT_UPDATE:
                ret = "Updated by import: ";
                break;
            case ENTITY_IMPORT_DELETE:
                ret = "Deleted by import: ";
                break;
            case TERM_ADD:
                ret = "Added term: ";
                break;
            case TERM_DELETE:
                ret = "Deleted term: ";
                break;
            default:
                ret = "Unknown: ";
        }

        return ret;
    }

    private FixedBufferList<EntityAuditEventV2> getAuditEventsList() {
        FixedBufferList<EntityAuditEventV2> ret = AUDIT_EVENTS_BUFFER.get();
        ret.reset();
        return ret;

    }
}
