/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.repository.store.graph.v2;


import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.DeleteType;
import org.apache.atlas.GraphTransactionInterceptor;
import org.apache.atlas.RequestContext;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.authorize.AtlasAdminAccessRequest;
import org.apache.atlas.authorize.AtlasAuthorizationUtils;
import org.apache.atlas.authorize.AtlasEntityAccessRequest;
import org.apache.atlas.authorize.AtlasEntityAccessRequest.AtlasEntityAccessRequestBuilder;
import org.apache.atlas.authorize.AtlasPrivilege;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.instance.AtlasCheckStateRequest;
import org.apache.atlas.model.instance.AtlasCheckStateResult;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.Status;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasEntityHeaders;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.repository.graph.GraphHelper;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.EntityGraphDiscovery;
import org.apache.atlas.repository.store.graph.EntityGraphDiscoveryContext;
import org.apache.atlas.repository.store.graph.v1.DeleteHandlerDelegate;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityComparator.AtlasEntityDiffResult;
import org.apache.atlas.type.AtlasArrayType;
import org.apache.atlas.type.AtlasBusinessMetadataType.AtlasBusinessAttribute;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasEnumType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.bulkimport.BulkImportResponse;
import org.apache.atlas.bulkimport.BulkImportResponse.ImportInfo;
import org.apache.atlas.util.FileUtils;
import org.apache.atlas.utils.AtlasEntityUtil;
import org.apache.atlas.utils.AtlasPerfMetrics.MetricRecorder;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.lang.Boolean.FALSE;
import static org.apache.atlas.AtlasConfiguration.STORE_DIFFERENTIAL_AUDITS;
import static org.apache.atlas.bulkimport.BulkImportResponse.ImportStatus.FAILED;
import static org.apache.atlas.model.instance.EntityMutations.EntityOperation.DELETE;
import static org.apache.atlas.model.instance.EntityMutations.EntityOperation.PURGE;
import static org.apache.atlas.model.instance.EntityMutations.EntityOperation.UPDATE;
import static org.apache.atlas.repository.Constants.IS_INCOMPLETE_PROPERTY_KEY;
import static org.apache.atlas.repository.graph.GraphHelper.getTypeName;
import static org.apache.atlas.repository.graph.GraphHelper.isEntityIncomplete;
import static org.apache.atlas.repository.store.graph.v2.EntityGraphMapper.validateLabels;


@Component
public class AtlasEntityStoreV2 implements AtlasEntityStore {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasEntityStoreV2.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("store.EntityStore");

    private final AtlasGraph                 graph;
    private final DeleteHandlerDelegate      deleteDelegate;
    private final AtlasTypeRegistry          typeRegistry;
    private final IAtlasEntityChangeNotifier entityChangeNotifier;
    private final EntityGraphMapper          entityGraphMapper;
    private final EntityGraphRetriever       entityRetriever;
    private       boolean                    storeDifferentialAudits;

    @Inject
    public AtlasEntityStoreV2(AtlasGraph graph, DeleteHandlerDelegate deleteDelegate, AtlasTypeRegistry typeRegistry,
                              IAtlasEntityChangeNotifier entityChangeNotifier, EntityGraphMapper entityGraphMapper) {
        this.graph                = graph;
        this.deleteDelegate       = deleteDelegate;
        this.typeRegistry         = typeRegistry;
        this.entityChangeNotifier = entityChangeNotifier;
        this.entityGraphMapper    = entityGraphMapper;
        this.entityRetriever      = new EntityGraphRetriever(graph, typeRegistry);
        this.storeDifferentialAudits = STORE_DIFFERENTIAL_AUDITS.getBoolean();
    }

    @VisibleForTesting
    public void setStoreDifferentialAudits(boolean val) {
        this.storeDifferentialAudits = val;
    }

    @Override
    @GraphTransaction
    public List<String> getEntityGUIDS(final String typename) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getEntityGUIDS({})", typename);
        }

        if (StringUtils.isEmpty(typename) || !typeRegistry.isRegisteredType(typename)) {
            throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_TYPENAME);
        }

        List<String> ret = AtlasGraphUtilsV2.findEntityGUIDsByType(graph, typename);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getEntityGUIDS({})", typename);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityWithExtInfo getById(String guid) throws AtlasBaseException {
        return getById(guid, false, false);
    }

    @Override
    @GraphTransaction
    public AtlasEntityWithExtInfo getById(final String guid, final boolean isMinExtInfo, boolean ignoreRelationships) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getById({}, {})", guid, isMinExtInfo);
        }

        EntityGraphRetriever entityRetriever = new EntityGraphRetriever(graph, typeRegistry, ignoreRelationships);

        AtlasEntityWithExtInfo ret = entityRetriever.toAtlasEntityWithExtInfo(guid, isMinExtInfo);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_READ, new AtlasEntityHeader(ret.getEntity())), "read entity: guid=", guid);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getById({}, {}): {}", guid, isMinExtInfo, ret);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityHeader getHeaderById(final String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getHeaderById({})", guid);
        }

        EntityGraphRetriever entityRetriever = new EntityGraphRetriever(graph, typeRegistry);

        AtlasEntityHeader ret = entityRetriever.toAtlasEntityHeaderWithClassifications(guid);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_READ, ret), "read entity: guid=", guid);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getHeaderById({}): {}", guid, ret);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntitiesWithExtInfo getByIds(List<String> guids) throws AtlasBaseException {
        return getByIds(guids, false, false);
    }

    @Override
    @GraphTransaction
    public AtlasEntitiesWithExtInfo getByIds(List<String> guids, boolean isMinExtInfo, boolean ignoreRelationships) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getByIds({}, {})", guids, isMinExtInfo);
        }

        EntityGraphRetriever entityRetriever = new EntityGraphRetriever(graph, typeRegistry, ignoreRelationships);

        AtlasEntitiesWithExtInfo ret = entityRetriever.toAtlasEntitiesWithExtInfo(guids, isMinExtInfo);

        if(ret != null){
            for(String guid : guids) {
                AtlasEntity entity = ret.getEntity(guid);
                try {
                    AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_READ, new AtlasEntityHeader(entity)), "read entity: guid=", guid);
                } catch (AtlasBaseException e) {
                    if (RequestContext.get().isSkipFailedEntities()) {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("getByIds(): ignoring failure for entity {}: error code={}, message={}", guid, e.getAtlasErrorCode(), e.getMessage());
                        }

                        //Remove from referred entities
                        ret.removeEntity(guid);
                        //Remove from entities
                        ret.removeEntity(entity);

                        continue;
                    }

                    throw e;
                }
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getByIds({}, {}): {}", guids, isMinExtInfo, ret);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntitiesWithExtInfo getEntitiesByUniqueAttributes(AtlasEntityType entityType, List<Map<String, Object>> uniqueAttributes , boolean isMinExtInfo, boolean ignoreRelationships) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getEntitiesByUniqueAttributes({}, {})", entityType.getTypeName(), uniqueAttributes);
        }

        EntityGraphRetriever entityRetriever = new EntityGraphRetriever(graph, typeRegistry, ignoreRelationships);

        AtlasEntitiesWithExtInfo ret = entityRetriever.getEntitiesByUniqueAttributes(entityType.getTypeName(), uniqueAttributes, isMinExtInfo);

        if (ret != null && ret.getEntities() != null) {
            for (AtlasEntity entity : ret.getEntities()) {
                AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_READ, new AtlasEntityHeader(entity)), "read entity: typeName=", entityType.getTypeName(), ", guid=", entity.getGuid());
            }
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getEntitiesByUniqueAttributes({}, {}): {}", entityType.getTypeName(), uniqueAttributes, ret);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityWithExtInfo getByUniqueAttributes(AtlasEntityType entityType, Map<String, Object> uniqAttributes)
            throws AtlasBaseException {
        return getByUniqueAttributes(entityType, uniqAttributes, false, false);
    }

    @Override
    @GraphTransaction
    public AtlasEntityWithExtInfo getByUniqueAttributes(AtlasEntityType entityType, Map<String, Object> uniqAttributes, boolean isMinExtInfo, boolean ignoreRelationships) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getByUniqueAttribute({}, {})", entityType.getTypeName(), uniqAttributes);
        }

        AtlasVertex entityVertex = AtlasGraphUtilsV2.getVertexByUniqueAttributes(graph, entityType, uniqAttributes);

        EntityGraphRetriever entityRetriever = new EntityGraphRetriever(graph, typeRegistry, ignoreRelationships);

        AtlasEntityWithExtInfo ret = entityRetriever.toAtlasEntityWithExtInfo(entityVertex, isMinExtInfo);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_BY_UNIQUE_ATTRIBUTE_NOT_FOUND, entityType.getTypeName(),
                    uniqAttributes.toString());
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_READ, new AtlasEntityHeader(ret.getEntity())), "read entity: typeName=", entityType.getTypeName(), ", uniqueAttributes=", uniqAttributes);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getByUniqueAttribute({}, {}): {}", entityType.getTypeName(), uniqAttributes, ret);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public AtlasEntityHeader getEntityHeaderByUniqueAttributes(AtlasEntityType entityType, Map<String, Object> uniqAttributes) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> getEntityHeaderByUniqueAttributes({}, {})", entityType.getTypeName(), uniqAttributes);
        }

        AtlasVertex entityVertex = AtlasGraphUtilsV2.getVertexByUniqueAttributes(graph, entityType, uniqAttributes);

        EntityGraphRetriever entityRetriever = new EntityGraphRetriever(graph, typeRegistry);

        AtlasEntityHeader ret = entityRetriever.toAtlasEntityHeader(entityVertex);

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_BY_UNIQUE_ATTRIBUTE_NOT_FOUND, entityType.getTypeName(),
                    uniqAttributes.toString());
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_READ, ret), "read entity: typeName=", entityType.getTypeName(), ", uniqueAttributes=", uniqAttributes);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== getEntityHeaderByUniqueAttributes({}, {}): {}", entityType.getTypeName(), uniqAttributes, ret);
        }

        return ret;
    }

    /**
     * Check state of entities in the store
     * @param request AtlasCheckStateRequest
     * @return AtlasCheckStateResult
     * @throws AtlasBaseException
     */
    @Override
    @GraphTransaction
    public AtlasCheckStateResult checkState(AtlasCheckStateRequest request) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> checkState({})", request);
        }

        EntityStateChecker entityStateChecker = new EntityStateChecker(graph, typeRegistry);

        AtlasCheckStateResult ret = entityStateChecker.checkState(request);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== checkState({}, {})", request, ret);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public EntityMutationResponse createOrUpdate(EntityStream entityStream, boolean isPartialUpdate) throws AtlasBaseException {
        return createOrUpdate(entityStream, isPartialUpdate, false, false);
    }

    @Override
    @GraphTransaction(logRollback = false)
    public EntityMutationResponse createOrUpdateForImport(EntityStream entityStream) throws AtlasBaseException {
        return createOrUpdate(entityStream, false, true, true);
    }

    @Override
    public EntityMutationResponse createOrUpdateForImportNoCommit(EntityStream entityStream) throws AtlasBaseException {
        return createOrUpdate(entityStream, false, true, true);
    }

    @Override
    @GraphTransaction
    public EntityMutationResponse updateEntity(AtlasObjectId objectId, AtlasEntityWithExtInfo updatedEntityInfo, boolean isPartialUpdate) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> updateEntity({}, {}, {})", objectId, updatedEntityInfo, isPartialUpdate);
        }

        if (objectId == null || updatedEntityInfo == null || updatedEntityInfo.getEntity() == null) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "null entity-id/entity");
        }

        final String guid;

        if (AtlasTypeUtil.isAssignedGuid(objectId.getGuid())) {
            guid = objectId.getGuid();
        } else {
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(objectId.getTypeName());

            if (entityType == null) {
                throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_TYPENAME, objectId.getTypeName());
            }

            guid = AtlasGraphUtilsV2.getGuidByUniqueAttributes(graph, typeRegistry.getEntityTypeByName(objectId.getTypeName()), objectId.getUniqueAttributes());
        }

        AtlasEntity entity = updatedEntityInfo.getEntity();

        entity.setGuid(guid);

        return createOrUpdate(new AtlasEntityStream(updatedEntityInfo), isPartialUpdate, false, false);
    }

    @Override
    @GraphTransaction
    public EntityMutationResponse updateByUniqueAttributes(AtlasEntityType entityType, Map<String, Object> uniqAttributes,
                                                           AtlasEntityWithExtInfo updatedEntityInfo) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> updateByUniqueAttributes({}, {})", entityType.getTypeName(), uniqAttributes);
        }

        if (updatedEntityInfo == null || updatedEntityInfo.getEntity() == null) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "no entity to update.");
        }

        String      guid   = AtlasGraphUtilsV2.getGuidByUniqueAttributes(graph, entityType, uniqAttributes);
        AtlasEntity entity = updatedEntityInfo.getEntity();

        entity.setGuid(guid);

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_UPDATE, new AtlasEntityHeader(entity)), "update entity ByUniqueAttributes");

        return createOrUpdate(new AtlasEntityStream(updatedEntityInfo), true, false, false);
    }

    @Override
    @GraphTransaction
    public EntityMutationResponse updateEntityAttributeByGuid(String guid, String attrName, Object attrValue)
            throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> updateEntityAttributeByGuid({}, {}, {})", guid, attrName, attrValue);
        }

        AtlasEntityHeader entity     = entityRetriever.toAtlasEntityHeaderWithClassifications(guid);
        AtlasEntityType   entityType = (AtlasEntityType) typeRegistry.getType(entity.getTypeName());
        AtlasAttribute    attr       = entityType.getAttribute(attrName);

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_UPDATE, entity), "update entity ByUniqueAttributes : guid=", guid );

        if (attr == null) {
            attr = entityType.getRelationshipAttribute(attrName, AtlasEntityUtil.getRelationshipType(attrValue));

            if (attr == null) {
                throw new AtlasBaseException(AtlasErrorCode.UNKNOWN_ATTRIBUTE, attrName, entity.getTypeName());
            }
        }

        AtlasType   attrType     = attr.getAttributeType();
        AtlasEntity updateEntity = new AtlasEntity();

        updateEntity.setGuid(guid);
        updateEntity.setTypeName(entity.getTypeName());

        switch (attrType.getTypeCategory()) {
            case PRIMITIVE:
                updateEntity.setAttribute(attrName, attrValue);
                break;
            case OBJECT_ID_TYPE:
                AtlasObjectId objId;

                if (attrValue instanceof String) {
                    objId = new AtlasObjectId((String) attrValue, attr.getAttributeDef().getTypeName());
                } else {
                    objId = (AtlasObjectId) attrType.getNormalizedValue(attrValue);
                }

                updateEntity.setAttribute(attrName, objId);
                break;

            default:
                throw new AtlasBaseException(AtlasErrorCode.ATTRIBUTE_UPDATE_NOT_SUPPORTED, attrName, attrType.getTypeName());
        }

        return createOrUpdate(new AtlasEntityStream(updateEntity), true, false, false);
    }

    @Override
    @GraphTransaction
    public EntityMutationResponse deleteById(final String guid) throws AtlasBaseException {
        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        Collection<AtlasVertex> deletionCandidates = new ArrayList<>();
        AtlasVertex             vertex             = AtlasGraphUtilsV2.findByGuid(graph, guid);

        if (vertex != null) {
            AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(vertex);

            AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_DELETE, entityHeader), "delete entity: guid=", guid);

            deletionCandidates.add(vertex);
        } else {
            if (LOG.isDebugEnabled()) {
                // Entity does not exist - treat as non-error, since the caller
                // wanted to delete the entity and it's already gone.
                LOG.debug("Deletion request ignored for non-existent entity with guid " + guid);
            }
        }

        EntityMutationResponse ret = deleteVertices(deletionCandidates);

        // Notify the change listeners
        entityChangeNotifier.onEntitiesMutated(ret, false);

        return ret;
    }

    @Override
    @GraphTransaction
    public EntityMutationResponse deleteByIds(final List<String> guids) throws AtlasBaseException {
        if (CollectionUtils.isEmpty(guids)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "Guid(s) not specified");
        }

        Collection<AtlasVertex> deletionCandidates = new ArrayList<>();

        for (String guid : guids) {
            AtlasVertex vertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

            if (vertex == null) {
                if (LOG.isDebugEnabled()) {
                    // Entity does not exist - treat as non-error, since the caller
                    // wanted to delete the entity and it's already gone.
                    LOG.debug("Deletion request ignored for non-existent entity with guid " + guid);
                }

                continue;
            }

            AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(vertex);

            AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_DELETE, entityHeader), "delete entity: guid=", guid);

            deletionCandidates.add(vertex);
        }

        if (deletionCandidates.isEmpty()) {
            LOG.info("No deletion candidate entities were found for guids %s", guids);
        }

        EntityMutationResponse ret = deleteVertices(deletionCandidates);

        // Notify the change listeners
        entityChangeNotifier.onEntitiesMutated(ret, false);

        return ret;
    }

    @Override
    @GraphTransaction
    public EntityMutationResponse purgeByIds(Set<String> guids) throws AtlasBaseException {
        if (CollectionUtils.isEmpty(guids)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "Guid(s) not specified");
        }

        AtlasAuthorizationUtils.verifyAccess(new AtlasAdminAccessRequest(AtlasPrivilege.ADMIN_PURGE), "purge entity: guids=", guids);
        Collection<AtlasVertex> purgeCandidates = new ArrayList<>();

        for (String guid : guids) {
            AtlasVertex vertex = AtlasGraphUtilsV2.findDeletedByGuid(graph, guid);

            if (vertex == null) {
                // Entity does not exist - treat as non-error, since the caller
                // wanted to delete the entity and it's already gone.
                LOG.warn("Purge request ignored for non-existent/active entity with guid " + guid);

                continue;
            }

            purgeCandidates.add(vertex);
        }

        if (purgeCandidates.isEmpty()) {
            LOG.info("No purge candidate entities were found for guids: " + guids + " which is already deleted");
        }

        EntityMutationResponse ret = purgeVertices(purgeCandidates);

        // Notify the change listeners
        entityChangeNotifier.onEntitiesMutated(ret, false);

        return ret;
    }

    @Override
    @GraphTransaction
    public EntityMutationResponse deleteByUniqueAttributes(AtlasEntityType entityType, Map<String, Object> uniqAttributes) throws AtlasBaseException {
        if (MapUtils.isEmpty(uniqAttributes)) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_BY_UNIQUE_ATTRIBUTE_NOT_FOUND, uniqAttributes.toString());
        }

        Collection<AtlasVertex> deletionCandidates = new ArrayList<>();
        AtlasVertex             vertex             = AtlasGraphUtilsV2.findByUniqueAttributes(graph, entityType, uniqAttributes);

        if (vertex != null) {
            AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(vertex);

            AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_DELETE, entityHeader), "delete entity: typeName=", entityType.getTypeName(), ", uniqueAttributes=", uniqAttributes);

            deletionCandidates.add(vertex);
        } else {
            if (LOG.isDebugEnabled()) {
                // Entity does not exist - treat as non-error, since the caller
                // wanted to delete the entity and it's already gone.
                LOG.debug("Deletion request ignored for non-existent entity with uniqueAttributes " + uniqAttributes);
            }
        }

        EntityMutationResponse ret = deleteVertices(deletionCandidates);

        // Notify the change listeners
        entityChangeNotifier.onEntitiesMutated(ret, false);

        return ret;
    }

    @Override
    @GraphTransaction
    public String getGuidByUniqueAttributes(AtlasEntityType entityType, Map<String, Object> uniqAttributes) throws AtlasBaseException{
        return AtlasGraphUtilsV2.getGuidByUniqueAttributes(graph, entityType, uniqAttributes);
    }

    @Override
    @GraphTransaction
    public void addClassifications(final String guid, final List<AtlasClassification> classifications) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding classifications={} to entity={}", classifications, guid);
        }

        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "Guid(s) not specified");
        }

        if (CollectionUtils.isEmpty(classifications)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "classifications(s) not specified");
        }

        GraphTransactionInterceptor.lockObjectAndReleasePostCommit(guid);

        AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

        if (entityVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(entityVertex);

        for (AtlasClassification classification : classifications) {
            AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_ADD_CLASSIFICATION, entityHeader, classification),
                                                 "add classification: guid=", guid, ", classification=", classification.getTypeName());
        }

        EntityMutationContext context = new EntityMutationContext();

        context.cacheEntity(guid, entityVertex, typeRegistry.getEntityTypeByName(entityHeader.getTypeName()));


        for (AtlasClassification classification : classifications) {
            validateAndNormalize(classification);
        }

        // validate if entity, not already associated with classifications
        validateEntityAssociations(guid, classifications);

        entityGraphMapper.addClassifications(context, guid, classifications);
    }

    @Override
    @GraphTransaction
    public void updateClassifications(String guid, List<AtlasClassification> classifications) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating classifications={} for entity={}", classifications, guid);
        }

        AtlasPerfTracer perf = null;

        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            AtlasPerfTracer.getPerfTracer(PERF_LOG, "AtlasEntityStoreV2.updateClassification()");
        }

        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "Guid not specified");
        }

        if (CollectionUtils.isEmpty(classifications)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "classifications(s) not specified");
        }

        GraphTransactionInterceptor.lockObjectAndReleasePostCommit(guid);

        AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

        if (entityVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(entityVertex);

        for (AtlasClassification classification : classifications) {
            AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_UPDATE_CLASSIFICATION, entityHeader, classification), "update classification: guid=", guid, ", classification=", classification.getTypeName());
        }

        EntityMutationContext context = new EntityMutationContext();

        context.cacheEntity(guid, entityVertex, typeRegistry.getEntityTypeByName(entityHeader.getTypeName()));


        for (AtlasClassification classification : classifications) {
            validateAndNormalize(classification);
        }

        entityGraphMapper.updateClassifications(context, guid, classifications);

        AtlasPerfTracer.log(perf);
    }

    @Override
    @GraphTransaction
    public void addClassification(final List<String> guids, final AtlasClassification classification) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Adding classification={} to entities={}", classification, guids);
        }

        if (CollectionUtils.isEmpty(guids)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "Guid(s) not specified");
        }

        if (classification == null) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "classification not specified");
        }

        validateAndNormalize(classification);

        EntityMutationContext     context         = new EntityMutationContext();
        List<AtlasClassification> classifications = Collections.singletonList(classification);
        List<String>              validGuids      =  new ArrayList<>();

        GraphTransactionInterceptor.lockObjectAndReleasePostCommit(guids);

        for (String guid : guids) {
            try {
                AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

                if (entityVertex == null) {
                    throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
                }

                AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(entityVertex);

                AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_ADD_CLASSIFICATION, entityHeader, classification),
                        "add classification: guid=", guid, ", classification=", classification.getTypeName());

                validateEntityAssociations(guid, classifications);

                validGuids.add(guid);
                context.cacheEntity(guid, entityVertex, typeRegistry.getEntityTypeByName(entityHeader.getTypeName()));
            } catch (AtlasBaseException abe) {
                if (RequestContext.get().isSkipFailedEntities()) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("addClassification(): ignoring failure for entity {}: error code={}, message={}", guid, abe.getAtlasErrorCode(), abe.getMessage());
                    }

                    continue;
                }

                throw abe;
            }
        }

        for (String guid : validGuids) {
            entityGraphMapper.addClassifications(context, guid, classifications);
        }
    }

    @Override
    @GraphTransaction
    public void deleteClassification(final String guid, final String classificationName) throws AtlasBaseException {
        deleteClassification(guid, classificationName, null);
    }

    @Override
    @GraphTransaction
    public void deleteClassification(final String guid, final String classificationName, final String associatedEntityGuid) throws AtlasBaseException {
        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "Guid(s) not specified");
        }
        if (StringUtils.isEmpty(classificationName)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "classifications not specified");
        }

        GraphTransactionInterceptor.lockObjectAndReleasePostCommit(guid);

        AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(guid);

        // verify authorization only for removal of directly associated classification and not propagated one.
        if (StringUtils.isEmpty(associatedEntityGuid) || guid.equals(associatedEntityGuid)) {
            AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_REMOVE_CLASSIFICATION,
                                                 entityHeader, new AtlasClassification(classificationName)),
                                                 "remove classification: guid=", guid, ", classification=", classificationName);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("Deleting classification={} from entity={}", classificationName, guid);
        }


        entityGraphMapper.deleteClassification(guid, classificationName, associatedEntityGuid);
    }


    @GraphTransaction
    public List<AtlasClassification> retrieveClassifications(String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Retriving classifications for entity={}", guid);
        }

        AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(guid);

        return entityHeader.getClassifications();
    }


    @Override
    @GraphTransaction
    public List<AtlasClassification> getClassifications(String guid) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting classifications for entity={}", guid);
        }

        AtlasEntityHeader entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(guid);

        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_READ, entityHeader), "get classifications: guid=", guid);

        return entityHeader.getClassifications();
    }

    @Override
    @GraphTransaction
    public AtlasClassification getClassification(String guid, String classificationName) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Getting classifications for entities={}", guid);
        }

        AtlasClassification ret          = null;
        AtlasEntityHeader   entityHeader = entityRetriever.toAtlasEntityHeaderWithClassifications(guid);

        if (CollectionUtils.isNotEmpty(entityHeader.getClassifications())) {
            AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_READ, entityHeader), "get classification: guid=", guid, ", classification=", classificationName);

            for (AtlasClassification classification : entityHeader.getClassifications()) {
                if (!StringUtils.equalsIgnoreCase(classification.getTypeName(), classificationName)) {
                    continue;
                }

                if (StringUtils.isEmpty(classification.getEntityGuid()) || StringUtils.equalsIgnoreCase(classification.getEntityGuid(), guid)) {
                    ret = classification;
                    break;
                } else if (ret == null) {
                    ret = classification;
                }
            }
        }

        if (ret == null) {
            throw new AtlasBaseException(AtlasErrorCode.CLASSIFICATION_NOT_FOUND, classificationName);
        }

        return ret;
    }

    @Override
    @GraphTransaction
    public String setClassifications(AtlasEntityHeaders entityHeaders) {
        ClassificationAssociator.Updater associator = new ClassificationAssociator.Updater(graph, typeRegistry, this);
        return associator.setClassifications(entityHeaders.getGuidHeaderMap());
    }

    @Override
    @GraphTransaction
    public void addOrUpdateBusinessAttributes(String guid, Map<String, Map<String, Object>> businessAttrbutes, boolean isOverwrite) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {

            LOG.debug("==> addOrUpdateBusinessAttributes(guid={}, businessAttributes={}, isOverwrite={})", guid, businessAttrbutes, isOverwrite);
        }

        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "guid is null/empty");
        }

        if (MapUtils.isEmpty(businessAttrbutes)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "businessAttributes is null/empty");
        }

        AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

        if (entityVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        String                           typeName                     = getTypeName(entityVertex);
        AtlasEntityType                  entityType                   = typeRegistry.getEntityTypeByName(typeName);
        AtlasEntityHeader                entityHeader                 = entityRetriever.toAtlasEntityHeaderWithClassifications(entityVertex);
        Map<String, Map<String, Object>> currEntityBusinessAttributes = entityRetriever.getBusinessMetadata(entityVertex);
        Set<String>                      updatedBusinessMetadataNames = new HashSet<>();

        for (String bmName : entityType.getBusinessAttributes().keySet()) {
            Map<String, Object> bmAttrs     = businessAttrbutes.get(bmName);
            Map<String, Object> currBmAttrs = currEntityBusinessAttributes != null ? currEntityBusinessAttributes.get(bmName) : null;

            if (bmAttrs == null && !isOverwrite) {
                continue;
            } else if (MapUtils.isEmpty(bmAttrs) && MapUtils.isEmpty(currBmAttrs)) { // no change
                continue;
            } else if (Objects.equals(bmAttrs, currBmAttrs)) { // no change
                continue;
            }

            updatedBusinessMetadataNames.add(bmName);
        }

        AtlasEntityAccessRequestBuilder  requestBuilder = new AtlasEntityAccessRequestBuilder(typeRegistry, AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA, entityHeader);

        for (String bmName : updatedBusinessMetadataNames) {
            requestBuilder.setBusinessMetadata(bmName);

            AtlasAuthorizationUtils.verifyAccess(requestBuilder.build(), "add/update business-metadata: guid=", guid, ", business-metadata-name=", bmName);
        }

        validateBusinessAttributes(entityVertex, entityType, businessAttrbutes, isOverwrite);

        if (isOverwrite) {
            entityGraphMapper.setBusinessAttributes(entityVertex, entityType, businessAttrbutes);
        } else {
            entityGraphMapper.addOrUpdateBusinessAttributes(entityVertex, entityType, businessAttrbutes);
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addOrUpdateBusinessAttributes(guid={}, businessAttributes={}, isOverwrite={})", guid, businessAttrbutes, isOverwrite);
        }
    }

    @Override
    @GraphTransaction
    public void removeBusinessAttributes(String guid, Map<String, Map<String, Object>> businessAttributes) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> removeBusinessAttributes(guid={}, businessAttributes={})", guid, businessAttributes);
        }

        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "guid is null/empty");
        }

        if (MapUtils.isEmpty(businessAttributes)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "businessAttributes is null/empty");
        }

        AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

        if (entityVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        String                          typeName       = getTypeName(entityVertex);
        AtlasEntityType                 entityType     = typeRegistry.getEntityTypeByName(typeName);
        AtlasEntityHeader               entityHeader   = entityRetriever.toAtlasEntityHeaderWithClassifications(entityVertex);
        AtlasEntityAccessRequestBuilder requestBuilder = new AtlasEntityAccessRequestBuilder(typeRegistry, AtlasPrivilege.ENTITY_UPDATE_BUSINESS_METADATA, entityHeader);

        for (String bmName : businessAttributes.keySet()) {
            requestBuilder.setBusinessMetadata(bmName);

            AtlasAuthorizationUtils.verifyAccess(requestBuilder.build(), "remove business-metadata: guid=", guid, ", business-metadata=", bmName);
        }

        entityGraphMapper.removeBusinessAttributes(entityVertex, entityType, businessAttributes);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== removeBusinessAttributes(guid={}, businessAttributes={})", guid, businessAttributes);
        }
    }

    @Override
    @GraphTransaction
    public void setLabels(String guid, Set<String> labels) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> setLabels()");
        }

        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "guid is null/empty");
        }

        AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

        if (entityVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        validateLabels(labels);

        AtlasEntityHeader entityHeader  = entityRetriever.toAtlasEntityHeaderWithClassifications(entityVertex);
        Set<String>       addedLabels   = Collections.emptySet();
        Set<String>       removedLabels = Collections.emptySet();

        if (CollectionUtils.isEmpty(entityHeader.getLabels())) {
            addedLabels = labels;
        } else if (CollectionUtils.isEmpty(labels)) {
            removedLabels = entityHeader.getLabels();
        } else {
            addedLabels   = new HashSet<String>(CollectionUtils.subtract(labels, entityHeader.getLabels()));
            removedLabels = new HashSet<String>(CollectionUtils.subtract(entityHeader.getLabels(), labels));
        }

        if (addedLabels != null) {
            AtlasEntityAccessRequestBuilder requestBuilder = new AtlasEntityAccessRequestBuilder(typeRegistry, AtlasPrivilege.ENTITY_ADD_LABEL, entityHeader);

            for (String label : addedLabels) {
                requestBuilder.setLabel(label);

                AtlasAuthorizationUtils.verifyAccess(requestBuilder.build(), "add label: guid=", guid, ", label=", label);
            }
        }

        if (removedLabels != null) {
            AtlasEntityAccessRequestBuilder requestBuilder = new AtlasEntityAccessRequestBuilder(typeRegistry, AtlasPrivilege.ENTITY_REMOVE_LABEL, entityHeader);

            for (String label : removedLabels) {
                requestBuilder.setLabel(label);

                AtlasAuthorizationUtils.verifyAccess(requestBuilder.build(), "remove label: guid=", guid, ", label=", label);
            }
        }

        entityGraphMapper.setLabels(entityVertex, labels);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== setLabels()");
        }
    }

    @Override
    @GraphTransaction
    public void removeLabels(String guid, Set<String> labels) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> removeLabels()");
        }

        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "guid is null/empty");
        }

        if (CollectionUtils.isEmpty(labels)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "labels is null/empty");
        }

        AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

        if (entityVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        AtlasEntityHeader               entityHeader   = entityRetriever.toAtlasEntityHeaderWithClassifications(entityVertex);
        AtlasEntityAccessRequestBuilder requestBuilder = new AtlasEntityAccessRequestBuilder(typeRegistry, AtlasPrivilege.ENTITY_REMOVE_LABEL, entityHeader);

        for (String label : labels) {
            requestBuilder.setLabel(label);

            AtlasAuthorizationUtils.verifyAccess(requestBuilder.build(), "remove label: guid=", guid, ", label=", label);
        }

        validateLabels(labels);

        entityGraphMapper.removeLabels(entityVertex, labels);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== removeLabels()");
        }
    }

    @Override
    @GraphTransaction
    public void addLabels(String guid, Set<String> labels) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> addLabels()");
        }

        if (StringUtils.isEmpty(guid)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "guid is null/empty");
        }

        if (CollectionUtils.isEmpty(labels)) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "labels is null/empty");
        }

        AtlasVertex entityVertex = AtlasGraphUtilsV2.findByGuid(graph, guid);

        if (entityVertex == null) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_GUID_NOT_FOUND, guid);
        }

        AtlasEntityHeader               entityHeader   = entityRetriever.toAtlasEntityHeaderWithClassifications(entityVertex);
        AtlasEntityAccessRequestBuilder requestBuilder = new AtlasEntityAccessRequestBuilder(typeRegistry, AtlasPrivilege.ENTITY_ADD_LABEL, entityHeader);

        for (String label : labels) {
            requestBuilder.setLabel(label);

            AtlasAuthorizationUtils.verifyAccess(requestBuilder.build(), "add/update label: guid=", guid, ", label=", label);
        }

        validateLabels(labels);

        entityGraphMapper.addLabels(entityVertex, labels);

        if (LOG.isDebugEnabled()) {
            LOG.debug("<== addLabels()");
        }
    }

    private EntityMutationResponse createOrUpdate(EntityStream entityStream, boolean isPartialUpdate, boolean replaceClassifications, boolean replaceBusinessAttributes) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> createOrUpdate()");
        }

        if (entityStream == null || !entityStream.hasNext()) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "no entities to create/update.");
        }

        AtlasPerfTracer perf = null;

        if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
            perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "createOrUpdate()");
        }

        MetricRecorder metric = RequestContext.get().startMetricRecord("createOrUpdate");

        try {
            final EntityMutationContext context = preCreateOrUpdate(entityStream, entityGraphMapper, isPartialUpdate);

            // Check if authorized to create entities
            if (!RequestContext.get().isImportInProgress()) {
                for (AtlasEntity entity : context.getCreatedEntities()) {
                    AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_CREATE, new AtlasEntityHeader(entity)),
                                                         "create entity: type=", entity.getTypeName());
                }
            }

            // for existing entities, skip update if incoming entity doesn't have any change
            if (CollectionUtils.isNotEmpty(context.getUpdatedEntities())) {
                MetricRecorder checkForUnchangedEntities = RequestContext.get().startMetricRecord("checkForUnchangedEntities");

                List<AtlasEntity>     entitiesToSkipUpdate = new ArrayList<>();
                AtlasEntityComparator entityComparator     = new AtlasEntityComparator(typeRegistry, entityRetriever, context.getGuidAssignments(), !replaceClassifications, !replaceBusinessAttributes);
                RequestContext        reqContext           = RequestContext.get();

                for (AtlasEntity entity : context.getUpdatedEntities()) {
                    if (entity.getStatus() == AtlasEntity.Status.DELETED) {// entity status could be updated during import
                        continue;
                    }

                    AtlasVertex           storedVertex = context.getVertex(entity.getGuid());
                    AtlasEntityDiffResult diffResult   = entityComparator.getDiffResult(entity, storedVertex, !storeDifferentialAudits);

                    if (diffResult.hasDifference()) {
                        if (storeDifferentialAudits) {
                            diffResult.getDiffEntity().setGuid(entity.getGuid());
                            reqContext.cacheDifferentialEntity(diffResult.getDiffEntity());
                        }

                        if (diffResult.hasDifferenceOnlyInCustomAttributes()) {
                            reqContext.recordEntityWithCustomAttributeUpdate(entity.getGuid());
                        }

                        if (diffResult.hasDifferenceOnlyInBusinessAttributes()) {
                            reqContext.recordEntityWithBusinessAttributeUpdate(entity.getGuid());
                        }
                    } else {
                        if (LOG.isDebugEnabled()) {
                            LOG.debug("skipping unchanged entity: {}", entity);
                        }

                        entitiesToSkipUpdate.add(entity);
                        reqContext.recordEntityToSkip(entity.getGuid());
                    }
                }

                if (entitiesToSkipUpdate.size() > 0) {
                    // remove entitiesToSkipUpdate from EntityMutationContext
                    context.getUpdatedEntities().removeAll(entitiesToSkipUpdate);
                }

                // Check if authorized to update entities
                if (!reqContext.isImportInProgress()) {
                    for (AtlasEntity entity : context.getUpdatedEntities()) {
                        AtlasAuthorizationUtils.verifyAccess(new AtlasEntityAccessRequest(typeRegistry, AtlasPrivilege.ENTITY_UPDATE, new AtlasEntityHeader(entity)),
                                                             "update entity: type=", entity.getTypeName());
                    }
                }

                reqContext.endMetricRecord(checkForUnchangedEntities);
            }

            EntityMutationResponse ret = entityGraphMapper.mapAttributesAndClassifications(context, isPartialUpdate, replaceClassifications, replaceBusinessAttributes);

            ret.setGuidAssignments(context.getGuidAssignments());

            // Notify the change listeners
            entityChangeNotifier.onEntitiesMutated(ret, RequestContext.get().isImportInProgress());

            if (LOG.isDebugEnabled()) {
                LOG.debug("<== createOrUpdate()");
            }

            return ret;
        } finally {
            RequestContext.get().endMetricRecord(metric);

            AtlasPerfTracer.log(perf);
        }
    }

    private EntityMutationContext preCreateOrUpdate(EntityStream entityStream, EntityGraphMapper entityGraphMapper, boolean isPartialUpdate) throws AtlasBaseException {
        MetricRecorder metric = RequestContext.get().startMetricRecord("preCreateOrUpdate");

        EntityGraphDiscovery        graphDiscoverer  = new AtlasEntityGraphDiscoveryV2(graph, typeRegistry, entityStream, entityGraphMapper);
        EntityGraphDiscoveryContext discoveryContext = graphDiscoverer.discoverEntities();
        EntityMutationContext       context          = new EntityMutationContext(discoveryContext);
        RequestContext              requestContext   = RequestContext.get();

        for (String guid : discoveryContext.getReferencedGuids()) {
            AtlasEntity entity = entityStream.getByGuid(guid);

            if (entity != null) { // entity would be null if guid is not in the stream but referenced by an entity in the stream
                AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entity.getTypeName());

                if (entityType == null) {
                    throw new AtlasBaseException(AtlasErrorCode.TYPE_NAME_INVALID, TypeCategory.ENTITY.name(), entity.getTypeName());
                }

                compactAttributes(entity, entityType);

                AtlasVertex vertex = getResolvedEntityVertex(discoveryContext, entity);

                if (vertex != null) {
                    if (!isPartialUpdate) {
                        graphDiscoverer.validateAndNormalize(entity);

                        // change entity 'isInComplete' to 'false' during full update
                        if (isEntityIncomplete(vertex)) {
                            vertex.removeProperty(IS_INCOMPLETE_PROPERTY_KEY);

                            entity.setIsIncomplete(FALSE);
                        }
                    } else {
                        graphDiscoverer.validateAndNormalizeForUpdate(entity);
                    }

                    String guidVertex = AtlasGraphUtilsV2.getIdFromVertex(vertex);

                    if (!StringUtils.equals(guidVertex, guid)) { // if entity was found by unique attribute
                        entity.setGuid(guidVertex);

                        requestContext.recordEntityGuidUpdate(entity, guid);
                    }

                    context.addUpdated(guid, entity, entityType, vertex);
                } else {
                    graphDiscoverer.validateAndNormalize(entity);

                    //Create vertices which do not exist in the repository
                    if (RequestContext.get().isImportInProgress() && AtlasTypeUtil.isAssignedGuid(entity.getGuid())) {
                        vertex = entityGraphMapper.createVertexWithGuid(entity, entity.getGuid());
                    } else {
                         vertex = entityGraphMapper.createVertex(entity);
                    }

                    discoveryContext.addResolvedGuid(guid, vertex);

                    discoveryContext.addResolvedIdByUniqAttribs(getAtlasObjectId(entity), vertex);

                    String generatedGuid = AtlasGraphUtilsV2.getIdFromVertex(vertex);

                    entity.setGuid(generatedGuid);

                    requestContext.recordEntityGuidUpdate(entity, guid);

                    context.addCreated(guid, entity, entityType, vertex);
                }

                // during import, update the system attributes
                if (RequestContext.get().isImportInProgress()) {
                    Status newStatus = entity.getStatus();

                    if (newStatus != null) {
                        Status currStatus = AtlasGraphUtilsV2.getState(vertex);

                        if (currStatus == Status.ACTIVE && newStatus == Status.DELETED) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("entity-delete via import - guid={}", guid);
                            }

                            context.addEntityToDelete(vertex);
                        } else if (currStatus == Status.DELETED && newStatus == Status.ACTIVE) {
                            LOG.warn("Import is attempting to activate deleted entity (guid={}).", guid);
                            entityGraphMapper.importActivateEntity(vertex, entity);
                            context.addCreated(guid, entity, entityType, vertex);
                        }
                    }

                    entityGraphMapper.updateSystemAttributes(vertex, entity);
                }
            }
        }

        RequestContext.get().endMetricRecord(metric);

        return context;
    }

    private AtlasVertex getResolvedEntityVertex(EntityGraphDiscoveryContext context, AtlasEntity entity) throws AtlasBaseException {
        AtlasObjectId objectId = getAtlasObjectId(entity);
        AtlasVertex   ret      = context.getResolvedEntityVertex(entity.getGuid());

        if (ret != null) {
            context.addResolvedIdByUniqAttribs(objectId, ret);
        } else {
            ret = context.getResolvedEntityVertex(objectId);

            if (ret != null) {
                context.addResolvedGuid(entity.getGuid(), ret);
            }
        }

        return ret;
    }

    private AtlasObjectId getAtlasObjectId(AtlasEntity entity) {
        AtlasObjectId ret = entityRetriever.toAtlasObjectId(entity);

        if (ret != null && !RequestContext.get().isImportInProgress() && MapUtils.isNotEmpty(ret.getUniqueAttributes())) {
            // if uniqueAttributes is not empty, reset guid to null.
            ret.setGuid(null);
        }

        return ret;
    }

    private EntityMutationResponse deleteVertices(Collection<AtlasVertex> deletionCandidates) throws AtlasBaseException {
        EntityMutationResponse response = new EntityMutationResponse();
        RequestContext         req      = RequestContext.get();

        deleteDelegate.getHandler().deleteEntities(deletionCandidates); // this will update req with list of deleted/updated entities

        for (AtlasEntityHeader entity : req.getDeletedEntities()) {
            response.addEntity(DELETE, entity);
        }

        for (AtlasEntityHeader entity : req.getUpdatedEntities()) {
            response.addEntity(UPDATE, entity);
        }

        return response;
    }

    private EntityMutationResponse purgeVertices(Collection<AtlasVertex> purgeCandidates) throws AtlasBaseException {
        EntityMutationResponse response = new EntityMutationResponse();
        RequestContext         req      = RequestContext.get();

        req.setDeleteType(DeleteType.HARD);
        req.setPurgeRequested(true);
        deleteDelegate.getHandler().deleteEntities(purgeCandidates); // this will update req with list of purged entities

        for (AtlasEntityHeader entity : req.getDeletedEntities()) {
            response.addEntity(PURGE, entity);
        }

        return response;
    }

    private void validateAndNormalize(AtlasClassification classification) throws AtlasBaseException {
        AtlasClassificationType type = typeRegistry.getClassificationTypeByName(classification.getTypeName());

        if (type == null) {
            throw new AtlasBaseException(AtlasErrorCode.CLASSIFICATION_NOT_FOUND, classification.getTypeName());
        }

        List<String> messages = new ArrayList<>();

        type.validateValue(classification, classification.getTypeName(), messages);

        if (!messages.isEmpty()) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, messages);
        }

        type.getNormalizedValue(classification);
    }

    /**
     * Validate if classification is not already associated with the entities
     *
     * @param guid            unique entity id
     * @param classifications list of classifications to be associated
     */
    private void validateEntityAssociations(String guid, List<AtlasClassification> classifications) throws AtlasBaseException {
        List<String>    entityClassifications = getClassificationNames(guid);
        String          entityTypeName        = AtlasGraphUtilsV2.getTypeNameFromGuid(graph, guid);
        AtlasEntityType entityType            = typeRegistry.getEntityTypeByName(entityTypeName);

        for (AtlasClassification classification : classifications) {
            String newClassification = classification.getTypeName();

            if (CollectionUtils.isNotEmpty(entityClassifications) && entityClassifications.contains(newClassification)) {
                throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "entity: " + guid +
                        ", already associated with classification: " + newClassification);
            }

            // for each classification, check whether there are entities it should be restricted to
            AtlasClassificationType classificationType = typeRegistry.getClassificationTypeByName(newClassification);

            if (!classificationType.canApplyToEntityType(entityType)) {
                throw new AtlasBaseException(AtlasErrorCode.INVALID_ENTITY_FOR_CLASSIFICATION, guid, entityTypeName, newClassification);
            }
        }
    }

    private List<String> getClassificationNames(String guid) throws AtlasBaseException {
        List<String>              ret             = null;
        List<AtlasClassification> classifications = retrieveClassifications(guid);

        if (CollectionUtils.isNotEmpty(classifications)) {
            ret = new ArrayList<>();

            for (AtlasClassification classification : classifications) {
                String entityGuid = classification.getEntityGuid();

                if (StringUtils.isEmpty(entityGuid) || StringUtils.equalsIgnoreCase(guid, entityGuid)) {
                    ret.add(classification.getTypeName());
                }
            }
        }

        return ret;
    }

    // move/remove relationship-attributes present in 'attributes'
    private void compactAttributes(AtlasEntity entity, AtlasEntityType entityType) {
        if (entity != null) {
            for (String attrName : entityType.getRelationshipAttributes().keySet()) {
                if (entity.hasAttribute(attrName)) { // relationship attribute is present in 'attributes'
                    Object attrValue = entity.removeAttribute(attrName);

                    if (attrValue != null) {
                        // if the attribute doesn't exist in relationshipAttributes, add it
                        Object relationshipAttrValue = entity.getRelationshipAttribute(attrName);

                        if (relationshipAttrValue == null) {
                            entity.setRelationshipAttribute(attrName, attrValue);

                            if (LOG.isDebugEnabled()) {
                                LOG.debug("moved attribute {}.{} from attributes to relationshipAttributes", entityType.getTypeName(), attrName);
                            }
                        } else {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("attribute {}.{} is present in attributes and relationshipAttributes. Removed from attributes", entityType.getTypeName(), attrName);
                            }
                        }
                    }
                }
            }
        }
    }

    private void validateBusinessAttributes(AtlasVertex entityVertex, AtlasEntityType entityType, Map<String, Map<String, Object>> businessAttributes, boolean isOverwrite) throws AtlasBaseException {
        List<String> messages = new ArrayList<>();

        Map<String, Map<String, AtlasBusinessAttribute>> entityTypeBusinessMetadata = entityType.getBusinessAttributes();

        for (String bmName : businessAttributes.keySet()) {
            if (!entityTypeBusinessMetadata.containsKey(bmName)) {
                messages.add(bmName + ": invalid business-metadata for entity type " + entityType.getTypeName());

                continue;
            }

            Map<String, AtlasBusinessAttribute> entityTypeBusinessAttributes = entityTypeBusinessMetadata.get(bmName);
            Map<String, Object>                         entityBusinessAttributes     = businessAttributes.get(bmName);

            for (AtlasBusinessAttribute bmAttribute : entityTypeBusinessAttributes.values()) {
                AtlasType attrType  = bmAttribute.getAttributeType();
                String    attrName  = bmAttribute.getName();
                Object    attrValue = entityBusinessAttributes.get(attrName);
                String    fieldName = entityType.getTypeName() + "." + bmName + "." + attrName;

                if (attrValue != null) {
                    attrType.validateValue(attrValue, fieldName, messages);
                    boolean isValidLength = bmAttribute.isValidLength(attrValue);
                    if (!isValidLength) {
                        messages.add(fieldName + ":  Business attribute-value exceeds maximum length limit");
                    }

                } else if (!bmAttribute.getAttributeDef().getIsOptional()) {
                    final boolean isAttrValuePresent;

                    if (isOverwrite) {
                        isAttrValuePresent = false;
                    } else {
                        Object existingValue = AtlasGraphUtilsV2.getEncodedProperty(entityVertex, bmAttribute.getVertexPropertyName(), Object.class);

                        isAttrValuePresent = existingValue != null;
                    }

                    if (!isAttrValuePresent) {
                        messages.add(fieldName + ": mandatory business-metadata attribute value missing in type " + entityType.getTypeName());
                    }
                }
            }
        }

        if (!messages.isEmpty()) {
            throw new AtlasBaseException(AtlasErrorCode.INSTANCE_CRUD_INVALID_PARAMS, messages);
        }
    }

    @Override
    @GraphTransaction
    public BulkImportResponse bulkCreateOrUpdateBusinessAttributes(InputStream inputStream, String fileName) throws AtlasBaseException {
        BulkImportResponse ret = new BulkImportResponse();

        if (StringUtils.isBlank(fileName)) {
            throw new AtlasBaseException(AtlasErrorCode.FILE_NAME_NOT_FOUND, fileName);
        }

        List<String[]>           fileData              = FileUtils.readFileData(fileName, inputStream);
        Map<String, AtlasEntity> attributesToAssociate = getBusinessMetadataDefList(fileData, ret);

        for (AtlasEntity entity : attributesToAssociate.values()) {
            Map<String, Map<String, Object>> businessAttributes = entity.getBusinessAttributes();
            String                           guid               = entity.getGuid();

            try {
                addOrUpdateBusinessAttributes(guid, businessAttributes, true);

                ret.addToSuccessImportInfoList(new ImportInfo(guid, businessAttributes.toString()));
            } catch (Exception e) {
                LOG.error("Error occurred while updating BusinessMetadata Attributes for Entity " + guid);

                ret.addToFailedImportInfoList(new ImportInfo(guid, businessAttributes.toString(), FAILED, e.getMessage()));
            }
        }

        return ret;
    }

    private Map<String, AtlasEntity> getBusinessMetadataDefList(List<String[]> fileData, BulkImportResponse bulkImportResponse) throws AtlasBaseException {
        Map<String, AtlasEntity> ret           = new HashMap<>();
        Map<String, AtlasVertex> vertexCache   = new HashMap<>();
        List<String>             failedMsgList = new ArrayList<>();

        for (int lineIndex = 0; lineIndex < fileData.size(); lineIndex++) {
            String[] record         = fileData.get(lineIndex);
            int      lineIndexToLog = lineIndex + 2;

            boolean missingFields = record.length < FileUtils.UNIQUE_ATTR_NAME_COLUMN_INDEX ||
                                    StringUtils.isBlank(record[FileUtils.TYPENAME_COLUMN_INDEX]) ||
                                    StringUtils.isBlank(record[FileUtils.UNIQUE_ATTR_VALUE_COLUMN_INDEX]) ||
                                    StringUtils.isBlank(record[FileUtils.BM_ATTR_NAME_COLUMN_INDEX]) ||
                                    StringUtils.isBlank(record[FileUtils.BM_ATTR_VALUE_COLUMN_INDEX]);

            if (missingFields){
                failedMsgList.add("Line #" + lineIndexToLog + ": missing fields. " + Arrays.toString(record));

                continue;
            }

            String          typeName   = record[FileUtils.TYPENAME_COLUMN_INDEX];
            AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);

            if (entityType == null) {
                failedMsgList.add("Line #" + lineIndexToLog + ": invalid entity-type '" + typeName + "'");

                continue;
            }

            String uniqueAttrValue  = record[FileUtils.UNIQUE_ATTR_VALUE_COLUMN_INDEX];
            String bmAttribute      = record[FileUtils.BM_ATTR_NAME_COLUMN_INDEX];
            String bmAttributeValue = record[FileUtils.BM_ATTR_VALUE_COLUMN_INDEX];
            String uniqueAttrName   = AtlasTypeUtil.ATTRIBUTE_QUALIFIED_NAME;

            if (record.length > FileUtils.UNIQUE_ATTR_NAME_COLUMN_INDEX && StringUtils.isNotBlank(record[FileUtils.UNIQUE_ATTR_NAME_COLUMN_INDEX])) {
                uniqueAttrName = record[FileUtils.UNIQUE_ATTR_NAME_COLUMN_INDEX];
            }

            AtlasAttribute uniqueAttribute = entityType.getAttribute(uniqueAttrName);

            if (uniqueAttribute == null) {
                failedMsgList.add("Line #" + lineIndexToLog + ": attribute '" + uniqueAttrName + "' not found in entity-type '" + typeName + "'");

                continue;
            }

            if (!uniqueAttribute.getAttributeDef().getIsUnique()) {
                failedMsgList.add("Line #" + lineIndexToLog + ": attribute '" + uniqueAttrName + "' is not an unique attribute in entity-type '" + typeName + "'");

                continue;
            }

            String      vertexKey = uniqueAttribute.getVertexPropertyName() + "_" + uniqueAttrValue;
            AtlasVertex vertex    = vertexCache.get(vertexKey);

            if (vertex == null) {
                vertex = AtlasGraphUtilsV2.findByTypeAndUniquePropertyName(graph, typeName, uniqueAttribute.getVertexUniquePropertyName(), uniqueAttrValue);

                if (vertex == null) {
                    failedMsgList.add("Line #" + lineIndexToLog + ": no " + typeName + " entity found with " + uniqueAttrName + "=" + uniqueAttrValue);

                    continue;
                }

                vertexCache.put(vertexKey, vertex);
            }

            AtlasBusinessAttribute businessAttribute = entityType.getBusinesAAttribute(bmAttribute);

            if (businessAttribute == null) {
                failedMsgList.add("Line #" + lineIndexToLog + ": invalid business-metadata '"+ bmAttribute + "' for entity type '" + entityType.getTypeName() + "'");

                continue;
            }

            final Object attrValue;

            if (businessAttribute.getAttributeType().getTypeCategory() == TypeCategory.ARRAY) {
                AtlasArrayType arrayType = (AtlasArrayType) businessAttribute.getAttributeType();
                List           arrayValue;

                if (arrayType.getElementType() instanceof AtlasEnumType) {
                    arrayValue = AtlasGraphUtilsV2.assignEnumValues(bmAttributeValue, (AtlasEnumType) arrayType.getElementType(), failedMsgList, lineIndex+1);
                } else {
                    arrayValue = assignMultipleValues(bmAttributeValue, arrayType.getElementTypeName(), failedMsgList, lineIndex+1);
                }

                attrValue = arrayValue;
            } else {
                attrValue = bmAttributeValue;
            }

            if (ret.containsKey(vertexKey)) {
                AtlasEntity entity = ret.get(vertexKey);

                entity.setBusinessAttribute(businessAttribute.getDefinedInType().getTypeName(), businessAttribute.getName(), attrValue);
            } else {
                AtlasEntity                      entity             = new AtlasEntity();
                String                           guid               = GraphHelper.getGuid(vertex);
                Map<String, Map<String, Object>> businessAttributes = entityRetriever.getBusinessMetadata(vertex);

                entity.setGuid(guid);
                entity.setTypeName(typeName);
                entity.setAttribute(uniqueAttribute.getName(), uniqueAttrValue);

                if (businessAttributes == null) {
                    businessAttributes = new HashMap<>();
                }

                entity.setBusinessAttributes(businessAttributes);
                entity.setBusinessAttribute(businessAttribute.getDefinedInType().getTypeName(), businessAttribute.getName(), attrValue);

                ret.put(vertexKey, entity);
            }
        }

        for (String failedMsg : failedMsgList) {
            LOG.error(failedMsg);

            bulkImportResponse.addToFailedImportInfoList(new ImportInfo(FAILED, failedMsg));
        }

        return ret;
    }


    private List assignMultipleValues(String bmAttributeValues, String elementTypeName, List failedTermMsgList, int lineIndex) {

        String[] arr = bmAttributeValues.split(FileUtils.ESCAPE_CHARACTER + FileUtils.PIPE_CHARACTER);
        try {
            switch (elementTypeName) {

                case AtlasBaseTypeDef.ATLAS_TYPE_FLOAT:
                    return AtlasGraphUtilsV2.floatParser(arr, failedTermMsgList, lineIndex);

                case AtlasBaseTypeDef.ATLAS_TYPE_INT:
                    return AtlasGraphUtilsV2.intParser(arr, failedTermMsgList, lineIndex);

                case AtlasBaseTypeDef.ATLAS_TYPE_LONG:
                    return AtlasGraphUtilsV2.longParser(arr, failedTermMsgList, lineIndex);

                case AtlasBaseTypeDef.ATLAS_TYPE_SHORT:
                    return AtlasGraphUtilsV2.shortParser(arr, failedTermMsgList, lineIndex);

                case AtlasBaseTypeDef.ATLAS_TYPE_DOUBLE:
                    return AtlasGraphUtilsV2.doubleParser(arr, failedTermMsgList, lineIndex);

                case AtlasBaseTypeDef.ATLAS_TYPE_DATE:
                    return AtlasGraphUtilsV2.longParser(arr, failedTermMsgList, lineIndex);

                case AtlasBaseTypeDef.ATLAS_TYPE_BOOLEAN:
                    return AtlasGraphUtilsV2.booleanParser(arr, failedTermMsgList, lineIndex);

                default:
                    return Arrays.asList(arr);
            }
        } catch (Exception e) {
            LOG.error("On line index " + lineIndex + "the provided BusinessMetadata AttributeValue " + bmAttributeValues + " are not of type - " + elementTypeName);
            failedTermMsgList.add("On line index " + lineIndex + "the provided BusinessMetadata AttributeValue " + bmAttributeValues + " are not of type - " + elementTypeName);
        }
        return null;
    }

    private boolean missingFieldsCheck(String[] record, BulkImportResponse bulkImportResponse, int lineIndex){
        boolean missingFieldsCheck = (record.length < FileUtils.UNIQUE_ATTR_NAME_COLUMN_INDEX) ||
                    StringUtils.isBlank(record[FileUtils.TYPENAME_COLUMN_INDEX]) ||
                        StringUtils.isBlank(record[FileUtils.UNIQUE_ATTR_VALUE_COLUMN_INDEX]) ||
                            StringUtils.isBlank(record[FileUtils.BM_ATTR_NAME_COLUMN_INDEX]) ||
                                StringUtils.isBlank(record[FileUtils.BM_ATTR_VALUE_COLUMN_INDEX]);

        if(missingFieldsCheck){
            LOG.error("Missing fields: " + Arrays.toString(record) + " at line #" + lineIndex);

            String failedTermMsgs = "Missing fields: " + Arrays.toString(record) + " at line #" + lineIndex;

            bulkImportResponse.addToFailedImportInfoList(new ImportInfo(FAILED, failedTermMsgs, lineIndex));
        }
        return missingFieldsCheck;
    }
}
