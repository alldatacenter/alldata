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

package org.apache.atlas.repository.store.graph.v2.bulkimport;


import com.google.common.annotations.VisibleForTesting;
import org.apache.atlas.AtlasConfiguration;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.RequestContext;
import org.apache.atlas.annotation.GraphTransaction;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasSchemaViolationException;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStreamForImport;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.BulkImporterImpl;
import org.apache.atlas.repository.store.graph.v2.EntityGraphRetriever;
import org.apache.atlas.repository.store.graph.v2.EntityImportStream;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.apache.atlas.repository.Constants.HISTORICAL_GUID_PROPERTY_KEY;
import static org.apache.atlas.repository.store.graph.v2.BulkImporterImpl.updateImportProgress;

public class RegularImport extends ImportStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(RegularImport.class);
    private static final int MAX_ATTEMPTS = 3;

    private final AtlasGraph graph;
    private final AtlasEntityStore entityStore;
    private final AtlasTypeRegistry typeRegistry;
    private final EntityGraphRetriever entityGraphRetriever;
    private boolean directoryBasedImportConfigured;

    public RegularImport(AtlasGraph graph, AtlasEntityStore entityStore, AtlasTypeRegistry typeRegistry) {
        this.graph       = graph;
        this.entityStore = entityStore;
        this.typeRegistry = typeRegistry;
        this.entityGraphRetriever = new EntityGraphRetriever(graph, typeRegistry);
        this.directoryBasedImportConfigured = StringUtils.isNotEmpty(AtlasConfiguration.IMPORT_TEMP_DIRECTORY.getString());
    }

    @Override
    public EntityMutationResponse run(EntityImportStream entityStream, AtlasImportResult importResult) throws AtlasBaseException {
        if (LOG.isDebugEnabled()) {
            LOG.debug("==> bulkImport()");
        }

        if (entityStream == null || !entityStream.hasNext()) {
            throw new AtlasBaseException(AtlasErrorCode.INVALID_PARAMETERS, "no entities to create/update.");
        }

        EntityMutationResponse ret = new EntityMutationResponse();
        ret.setGuidAssignments(new HashMap<>());

        Set<String> processedGuids = new HashSet<>();
        float        currentPercent = 0f;
        List<String> residualList   = new ArrayList<>();

        EntityImportStreamWithResidualList entityImportStreamWithResidualList = new EntityImportStreamWithResidualList(entityStream, residualList);

        while (entityImportStreamWithResidualList.hasNext()) {
            AtlasEntityWithExtInfo entityWithExtInfo = entityImportStreamWithResidualList.getNextEntityWithExtInfo();
            AtlasEntity            entity            = entityWithExtInfo != null ? entityWithExtInfo.getEntity() : null;

            if (entity == null) {
                continue;
            }

            for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
                try {
                    AtlasEntityStreamForImport oneEntityStream = new AtlasEntityStreamForImport(entityWithExtInfo, null);
                    EntityMutationResponse resp = entityStore.createOrUpdateForImport(oneEntityStream);

                    if (resp.getGuidAssignments() != null) {
                        ret.getGuidAssignments().putAll(resp.getGuidAssignments());
                    }

                    currentPercent = updateImportMetrics(entityWithExtInfo, resp, importResult, processedGuids,
                            entityStream.getPosition(),
                            entityImportStreamWithResidualList.getStreamSize(),
                            currentPercent);

                    entityStream.onImportComplete(entity.getGuid());
                    break;
                } catch (AtlasBaseException e) {
                    if (!updateResidualList(e, residualList, entityWithExtInfo.getEntity().getGuid())) {
                        throw e;
                    }
                    break;
                } catch (AtlasSchemaViolationException e) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Entity: {}", entity.getGuid(), e);
                    }

                    if (attempt == 0) {
                        updateVertexGuid(entityWithExtInfo);
                    } else {
                        LOG.error("Guid update failed: {}", entityWithExtInfo.getEntity().getGuid());
                        throw e;
                    }
                } catch (Throwable e) {
                    AtlasBaseException abe = new AtlasBaseException(e);
                    if (!updateResidualList(abe, residualList, entityWithExtInfo.getEntity().getGuid())) {
                        throw abe;
                    }

                    LOG.warn("Exception: {}", entity.getGuid(), e);
                    break;
                } finally {
                    RequestContext.get().clearCache();
                }
            }
        }

        importResult.getProcessedEntities().addAll(processedGuids);
        LOG.info("bulkImport(): done. Total number of entities (including referred entities) imported: {}", processedGuids.size());

        return ret;
    }

    @GraphTransaction
    public void updateVertexGuid(AtlasEntityWithExtInfo entityWithExtInfo) {
        updateVertexGuid(entityWithExtInfo.getEntity());
        if (MapUtils.isEmpty(entityWithExtInfo.getReferredEntities())) {
            return;
        }
        for (AtlasEntity entity : entityWithExtInfo.getReferredEntities().values()) {
            updateVertexGuid(entity);
        }
    }
    public void updateVertexGuid(AtlasEntity entity) {
        String entityGuid = entity.getGuid();
        AtlasObjectId objectId = entityGraphRetriever.toAtlasObjectIdWithoutGuid(entity);

        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entity.getTypeName());
        String vertexGuid = null;
        try {
            vertexGuid = AtlasGraphUtilsV2.getGuidByUniqueAttributes(this.graph, entityType, objectId.getUniqueAttributes());
        } catch (AtlasBaseException e) {
            LOG.warn("Entity: {}: Does not exist!", objectId);
            return;
        }

        if (StringUtils.isEmpty(vertexGuid) || vertexGuid.equals(entityGuid)) {
            return;
        }

        AtlasVertex v = AtlasGraphUtilsV2.findByGuid(this.graph, vertexGuid);
        if (v == null) {
            return;
        }

        addHistoricalGuid(v, vertexGuid);
        AtlasGraphUtilsV2.setProperty(v, Constants.GUID_PROPERTY_KEY, entityGuid);

        LOG.warn("GUID Updated: Entity: {}: from: {}: to: {}", objectId, vertexGuid, entity.getGuid());
    }

    private void addHistoricalGuid(AtlasVertex v, String vertexGuid) {
        String existingJson = AtlasGraphUtilsV2.getProperty(v, HISTORICAL_GUID_PROPERTY_KEY, String.class);

        AtlasGraphUtilsV2.setProperty(v, HISTORICAL_GUID_PROPERTY_KEY, getJsonArray(existingJson, vertexGuid));
    }

    @VisibleForTesting
    static String getJsonArray(String json, String vertexGuid) {
        String quotedGuid = String.format("\"%s\"", vertexGuid);
        if (StringUtils.isEmpty(json)) {
            json = String.format("[%s]", quotedGuid);
        } else {
            json = json.replace("]", "").concat(",").concat(quotedGuid).concat("]");
        }
        return json;
    }

    private boolean updateResidualList(AtlasBaseException e, List<String> lineageList, String guid) {
        if (!e.getAtlasErrorCode().getErrorCode().equals(AtlasErrorCode.INVALID_OBJECT_ID.getErrorCode())) {
            return false;
        }

        lineageList.add(guid);

        return true;
    }

    private float updateImportMetrics(AtlasEntity.AtlasEntityWithExtInfo currentEntity,
                                      EntityMutationResponse             resp,
                                      AtlasImportResult                  importResult,
                                      Set<String>                        processedGuids,
                                      int currentIndex, int streamSize, float currentPercent) {
        if (!directoryBasedImportConfigured) {
            BulkImporterImpl.updateImportMetrics("entity:%s:created", resp.getCreatedEntities(), processedGuids, importResult);
            BulkImporterImpl.updateImportMetrics("entity:%s:updated", resp.getUpdatedEntities(), processedGuids, importResult);
            BulkImporterImpl.updateImportMetrics("entity:%s:deleted", resp.getDeletedEntities(), processedGuids, importResult);
        }

        String lastEntityImported = String.format("entity:last-imported:%s:[%s]:(%s)", currentEntity.getEntity().getTypeName(), currentIndex, currentEntity.getEntity().getGuid());

        return updateImportProgress(LOG, currentIndex, streamSize, currentPercent, lastEntityImported);
    }

    private static class EntityImportStreamWithResidualList {
        private final EntityImportStream stream;
        private final List<String>       residualList;
        private       boolean            navigateResidualList;
        private       int                currentResidualListIndex;


        public EntityImportStreamWithResidualList(EntityImportStream stream, List<String> residualList) {
            this.stream                   = stream;
            this.residualList             = residualList;
            this.navigateResidualList     = false;
            this.currentResidualListIndex = 0;
        }

        public AtlasEntity.AtlasEntityWithExtInfo getNextEntityWithExtInfo() {
            if (navigateResidualList == false) {
                return stream.getNextEntityWithExtInfo();
            } else {
                stream.setPositionUsingEntityGuid(residualList.get(currentResidualListIndex++));
                return stream.getNextEntityWithExtInfo();
            }
        }

        public boolean hasNext() {
            if (!navigateResidualList) {
                boolean streamHasNext = stream.hasNext();
                navigateResidualList = (streamHasNext == false);
                return streamHasNext ? streamHasNext : (currentResidualListIndex < residualList.size());
            } else {
                return (currentResidualListIndex < residualList.size());
            }
        }

        public int getStreamSize() {
            return stream.size() + residualList.size();
        }
    }
}
