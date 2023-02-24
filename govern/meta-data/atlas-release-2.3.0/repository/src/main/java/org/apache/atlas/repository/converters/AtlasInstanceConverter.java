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
package org.apache.atlas.repository.converters;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.AtlasException;
import org.apache.atlas.CreateUpdateEntitiesResult;
import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.RequestContext;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations.EntityOperation;
import org.apache.atlas.model.instance.GuidMapping;
import org.apache.atlas.model.legacy.EntityResult;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.repository.converters.AtlasFormatConverter.ConverterContext;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Singleton
@Component
public class AtlasInstanceConverter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasInstanceConverter.class);

    private final AtlasTypeRegistry     typeRegistry;
    private final AtlasFormatConverters instanceFormatters;
    private final EntityGraphRetriever  entityGraphRetriever;
    private final EntityGraphRetriever  entityGraphRetrieverIgnoreRelationshipAttrs;

    @Inject
    public AtlasInstanceConverter(AtlasGraph graph, AtlasTypeRegistry typeRegistry, AtlasFormatConverters instanceFormatters) {
        this.typeRegistry                                = typeRegistry;
        this.instanceFormatters                          = instanceFormatters;
        this.entityGraphRetriever                        = new EntityGraphRetriever(graph, typeRegistry);
        this.entityGraphRetrieverIgnoreRelationshipAttrs = new EntityGraphRetriever(graph, typeRegistry, true);
    }

    public Referenceable[] getReferenceables(Collection<AtlasEntity> entities) throws AtlasBaseException {
        Referenceable[] ret = new Referenceable[entities.size()];

        AtlasFormatConverter.ConverterContext ctx = new AtlasFormatConverter.ConverterContext();

        for(Iterator<AtlasEntity> i = entities.iterator(); i.hasNext(); ) {
            ctx.addEntity(i.next());
        }

        Iterator<AtlasEntity> entityIterator = entities.iterator();
        for (int i = 0; i < entities.size(); i++) {
            ret[i] = getReferenceable(entityIterator.next(), ctx);
        }

        return ret;
    }

    public Referenceable getReferenceable(AtlasEntity entity) throws AtlasBaseException {
        return getReferenceable(entity, new ConverterContext());
    }

    public Referenceable getReferenceable(String guid) throws AtlasBaseException {
        AtlasEntityWithExtInfo entity = getAndCacheEntityExtInfo(guid);

        return getReferenceable(entity);
    }

    public Referenceable getReferenceable(AtlasEntityWithExtInfo entity) throws AtlasBaseException {
        AtlasFormatConverter.ConverterContext ctx = new AtlasFormatConverter.ConverterContext();

        ctx.addEntity(entity.getEntity());
        for(Map.Entry<String, AtlasEntity> entry : entity.getReferredEntities().entrySet()) {
            ctx.addEntity(entry.getValue());
        }

        return getReferenceable(entity.getEntity(), ctx);
    }

    public Referenceable getReferenceable(AtlasEntity entity, final ConverterContext ctx) throws AtlasBaseException {
        AtlasFormatConverter converter  = instanceFormatters.getConverter(TypeCategory.ENTITY);
        AtlasType            entityType = typeRegistry.getType(entity.getTypeName());
        Referenceable        ref        = (Referenceable) converter.fromV2ToV1(entity, entityType, ctx);

        return ref;
    }

    public Struct getTrait(AtlasClassification classification) throws AtlasBaseException {
        AtlasFormatConverter converter          = instanceFormatters.getConverter(TypeCategory.CLASSIFICATION);
        AtlasType            classificationType = typeRegistry.getType(classification.getTypeName());
        Struct               trait               = (Struct)converter.fromV2ToV1(classification, classificationType, new ConverterContext());

        return trait;
    }

    public AtlasClassification toAtlasClassification(Struct classification) throws AtlasBaseException {
        AtlasFormatConverter    converter          = instanceFormatters.getConverter(TypeCategory.CLASSIFICATION);
        AtlasClassificationType classificationType = typeRegistry.getClassificationTypeByName(classification.getTypeName());

        if (classificationType == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_INVALID, TypeCategory.CLASSIFICATION.name(), classification.getTypeName());
        }

        AtlasClassification  ret = (AtlasClassification)converter.fromV1ToV2(classification, classificationType, new AtlasFormatConverter.ConverterContext());

        return ret;
    }

    public AtlasEntitiesWithExtInfo toAtlasEntity(Referenceable referenceable) throws AtlasBaseException {
        AtlasEntityFormatConverter converter  = (AtlasEntityFormatConverter) instanceFormatters.getConverter(TypeCategory.ENTITY);
        AtlasEntityType            entityType = typeRegistry.getEntityTypeByName(referenceable.getTypeName());

        if (entityType == null) {
            throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_INVALID, TypeCategory.ENTITY.name(), referenceable.getTypeName());
        }

        ConverterContext ctx    = new ConverterContext();
        AtlasEntity      entity = converter.fromV1ToV2(referenceable, entityType, ctx);

        ctx.addEntity(entity);

        return ctx.getEntities();
    }


    public AtlasEntity.AtlasEntitiesWithExtInfo toAtlasEntities(List<Referenceable> referenceables) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> toAtlasEntities({})", referenceables);
        }

        AtlasFormatConverter.ConverterContext context = new AtlasFormatConverter.ConverterContext();

        for (Referenceable referenceable : referenceables) {
            AtlasEntity entity = fromV1toV2Entity(referenceable, context);

            context.addEntity(entity);
        }

        AtlasEntity.AtlasEntitiesWithExtInfo ret = context.getEntities();

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== toAtlasEntities({}): ret=", referenceables, ret);
        }

        return ret;
    }

    public AtlasEntitiesWithExtInfo toAtlasEntities(String[] jsonEntities) throws AtlasBaseException, AtlasException {
        Referenceable[] referenceables = new Referenceable[jsonEntities.length];

        for (int i = 0; i < jsonEntities.length; i++) {
            referenceables[i] = AtlasType.fromV1Json(jsonEntities[i], Referenceable.class);
        }

        AtlasEntityFormatConverter converter = (AtlasEntityFormatConverter) instanceFormatters.getConverter(TypeCategory.ENTITY);
        ConverterContext           context   = new ConverterContext();

        for (Referenceable referenceable : referenceables) {
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(referenceable.getTypeName());

            if (entityType == null) {
                throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_INVALID, TypeCategory.ENTITY.name(), referenceable.getTypeName());
            }

            AtlasEntity entity = converter.fromV1ToV2(referenceable, entityType, context);

            context.addEntity(entity);
        }

        AtlasEntitiesWithExtInfo ret = context.getEntities();

        return ret;
    }

    private AtlasEntity fromV1toV2Entity(Referenceable referenceable, AtlasFormatConverter.ConverterContext context) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> fromV1toV2Entity({})", referenceable);
        }

        AtlasEntityFormatConverter converter = (AtlasEntityFormatConverter) instanceFormatters.getConverter(TypeCategory.ENTITY);

        AtlasEntity entity = converter.fromV1ToV2(referenceable, typeRegistry.getType(referenceable.getTypeName()), context);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== fromV1toV2Entity({}): {}", referenceable, entity);
        }

        return entity;
    }

    public CreateUpdateEntitiesResult toCreateUpdateEntitiesResult(EntityMutationResponse reponse) {
        CreateUpdateEntitiesResult ret = null;

        if (reponse != null) {
            Map<EntityOperation, List<AtlasEntityHeader>> mutatedEntities = reponse.getMutatedEntities();
            Map<String, String>                           guidAssignments = reponse.getGuidAssignments();

            ret = new CreateUpdateEntitiesResult();

            if (MapUtils.isNotEmpty(guidAssignments)) {
                ret.setGuidMapping(new GuidMapping(guidAssignments));
            }

            if (MapUtils.isNotEmpty(mutatedEntities)) {
                EntityResult entityResult = new EntityResult();

                for (Map.Entry<EntityOperation, List<AtlasEntityHeader>> e : mutatedEntities.entrySet()) {
                    switch (e.getKey()) {
                        case CREATE:
                            List<AtlasEntityHeader> createdEntities = mutatedEntities.get(EntityOperation.CREATE);
                            if (CollectionUtils.isNotEmpty(createdEntities)) {
                                Collections.reverse(createdEntities);
                                entityResult.set(EntityResult.OP_CREATED, getGuids(createdEntities));
                            }
                            break;
                        case UPDATE:
                            List<AtlasEntityHeader> updatedEntities = mutatedEntities.get(EntityOperation.UPDATE);
                            if (CollectionUtils.isNotEmpty(updatedEntities)) {
                                Collections.reverse(updatedEntities);
                                entityResult.set(EntityResult.OP_UPDATED, getGuids(updatedEntities));
                            }
                            break;
                        case PARTIAL_UPDATE:
                            List<AtlasEntityHeader> partialUpdatedEntities = mutatedEntities.get(EntityOperation.PARTIAL_UPDATE);
                            if (CollectionUtils.isNotEmpty(partialUpdatedEntities)) {
                                Collections.reverse(partialUpdatedEntities);
                                entityResult.set(EntityResult.OP_UPDATED, getGuids(partialUpdatedEntities));
                            }
                            break;
                        case DELETE:
                            List<AtlasEntityHeader> deletedEntities = mutatedEntities.get(EntityOperation.DELETE);
                            if (CollectionUtils.isNotEmpty(deletedEntities)) {
                                Collections.reverse(deletedEntities);
                                entityResult.set(EntityResult.OP_DELETED, getGuids(deletedEntities));
                            }
                            break;
                    }

                }

                ret.setEntityResult(entityResult);
            }
        }

        return ret;
    }

    public List<String> getGuids(List<AtlasEntityHeader> entities) {
        List<String> ret = null;

        if (CollectionUtils.isNotEmpty(entities)) {
            ret = new ArrayList<>();
            for (AtlasEntityHeader entity : entities) {
                ret.add(entity.getGuid());
            }
        }

        return ret;
    }

    public AtlasEntity getAndCacheEntity(String guid) throws AtlasBaseException {
        return getAndCacheEntity(guid, false);
    }

    public AtlasEntity getAndCacheEntity(String guid, boolean ignoreRelationshipAttributes) throws AtlasBaseException {
        RequestContext context = RequestContext.get();
        AtlasEntity    entity  = context.getEntity(guid);

        if (entity == null) {
            if (ignoreRelationshipAttributes) {
                entity = entityGraphRetrieverIgnoreRelationshipAttrs.toAtlasEntity(guid);
            } else {
                entity = entityGraphRetriever.toAtlasEntity(guid);
            }

            if (entity != null) {
                context.cache(entity);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cache miss -> GUID = {}", guid);
                }
            }
        }

        return entity;
    }


    public AtlasEntityWithExtInfo getAndCacheEntityExtInfo(String guid) throws AtlasBaseException {
        RequestContext         context           = RequestContext.get();
        AtlasEntityWithExtInfo entityWithExtInfo = context.getEntityWithExtInfo(guid);

        if (entityWithExtInfo == null) {
            entityWithExtInfo = entityGraphRetriever.toAtlasEntityWithExtInfo(guid);

            if (entityWithExtInfo != null) {
                context.cache(entityWithExtInfo);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Cache miss -> GUID = {}", guid);
                }
            }
        }

        return entityWithExtInfo;
    }

    public EntityAuditEvent toV1AuditEvent(EntityAuditEventV2 v2Event) throws AtlasBaseException {
        EntityAuditEvent ret = new EntityAuditEvent();

        ret.setEntityId(v2Event.getEntityId());
        ret.setTimestamp(v2Event.getTimestamp());
        ret.setUser(v2Event.getUser());
        ret.setDetails(v2Event.getDetails());
        ret.setEventKey(v2Event.getEventKey());

        ret.setAction(getV1AuditAction(v2Event.getAction()));
        ret.setEntityDefinition(getReferenceable(v2Event.getEntityId()));

        return ret;
    }

    public EntityAuditEventV2 toV2AuditEvent(EntityAuditEvent v1Event) throws AtlasBaseException {
        EntityAuditEventV2 ret = new EntityAuditEventV2();

        ret.setEntityId(v1Event.getEntityId());
        ret.setTimestamp(v1Event.getTimestamp());
        ret.setUser(v1Event.getUser());
        ret.setDetails(v1Event.getDetails());
        ret.setEventKey(v1Event.getEventKey());
        ret.setAction(getV2AuditAction(v1Event.getAction()));

        AtlasEntitiesWithExtInfo entitiesWithExtInfo = toAtlasEntity(v1Event.getEntityDefinition());

        if (entitiesWithExtInfo != null && CollectionUtils.isNotEmpty(entitiesWithExtInfo.getEntities())) {
            // there will only one source entity
            AtlasEntity entity = entitiesWithExtInfo.getEntities().get(0);

            ret.setEntity(entity);
        }

        return ret;
    }

    private EntityAuditEvent.EntityAuditAction getV1AuditAction(EntityAuditEventV2.EntityAuditActionV2 v2AuditAction) {
        switch (v2AuditAction) {
            case ENTITY_CREATE:
                return EntityAuditEvent.EntityAuditAction.ENTITY_CREATE;
            case ENTITY_UPDATE:
            case BUSINESS_ATTRIBUTE_UPDATE:
            case CUSTOM_ATTRIBUTE_UPDATE:
                return EntityAuditEvent.EntityAuditAction.ENTITY_UPDATE;
            case ENTITY_DELETE:
                return EntityAuditEvent.EntityAuditAction.ENTITY_DELETE;
            case ENTITY_IMPORT_CREATE:
                return EntityAuditEvent.EntityAuditAction.ENTITY_IMPORT_CREATE;
            case ENTITY_IMPORT_UPDATE:
                return EntityAuditEvent.EntityAuditAction.ENTITY_IMPORT_UPDATE;
            case ENTITY_IMPORT_DELETE:
                return EntityAuditEvent.EntityAuditAction.ENTITY_IMPORT_DELETE;
            case CLASSIFICATION_ADD:
                return EntityAuditEvent.EntityAuditAction.TAG_ADD;
            case CLASSIFICATION_DELETE:
                return EntityAuditEvent.EntityAuditAction.TAG_DELETE;
            case CLASSIFICATION_UPDATE:
                return EntityAuditEvent.EntityAuditAction.TAG_UPDATE;
            case PROPAGATED_CLASSIFICATION_ADD:
                return EntityAuditEvent.EntityAuditAction.PROPAGATED_TAG_ADD;
            case PROPAGATED_CLASSIFICATION_DELETE:
                return EntityAuditEvent.EntityAuditAction.PROPAGATED_TAG_DELETE;
            case PROPAGATED_CLASSIFICATION_UPDATE:
                return EntityAuditEvent.EntityAuditAction.PROPAGATED_TAG_UPDATE;
            case LABEL_ADD:
                return EntityAuditEvent.EntityAuditAction.LABEL_ADD;
            case LABEL_DELETE:
                return EntityAuditEvent.EntityAuditAction.LABEL_DELETE;
            case TERM_ADD:
                return EntityAuditEvent.EntityAuditAction.TERM_ADD;
            case TERM_DELETE:
                return EntityAuditEvent.EntityAuditAction.TERM_DELETE;
        }

        return null;
    }

    private EntityAuditEventV2.EntityAuditActionV2 getV2AuditAction(EntityAuditEvent.EntityAuditAction v1AuditAction) {
        switch (v1AuditAction) {
            case ENTITY_CREATE:
                return EntityAuditEventV2.EntityAuditActionV2.ENTITY_CREATE;
            case ENTITY_UPDATE:
                return EntityAuditEventV2.EntityAuditActionV2.ENTITY_UPDATE;
            case ENTITY_DELETE:
                return EntityAuditEventV2.EntityAuditActionV2.ENTITY_DELETE;
            case ENTITY_IMPORT_CREATE:
                return EntityAuditEventV2.EntityAuditActionV2.ENTITY_IMPORT_CREATE;
            case ENTITY_IMPORT_UPDATE:
                return EntityAuditEventV2.EntityAuditActionV2.ENTITY_IMPORT_UPDATE;
            case ENTITY_IMPORT_DELETE:
                return EntityAuditEventV2.EntityAuditActionV2.ENTITY_IMPORT_DELETE;
            case TAG_ADD:
                return EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_ADD;
            case TAG_DELETE:
                return EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_DELETE;
            case TAG_UPDATE:
                return EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_UPDATE;
            case PROPAGATED_TAG_ADD:
                return EntityAuditEventV2.EntityAuditActionV2.PROPAGATED_CLASSIFICATION_ADD;
            case PROPAGATED_TAG_DELETE:
                return EntityAuditEventV2.EntityAuditActionV2.PROPAGATED_CLASSIFICATION_DELETE;
            case PROPAGATED_TAG_UPDATE:
                return EntityAuditEventV2.EntityAuditActionV2.PROPAGATED_CLASSIFICATION_UPDATE;
            case TERM_ADD:
                return EntityAuditEventV2.EntityAuditActionV2.TERM_ADD;
            case TERM_DELETE:
                return EntityAuditEventV2.EntityAuditActionV2.TERM_DELETE;
        }

        return null;
    }
}