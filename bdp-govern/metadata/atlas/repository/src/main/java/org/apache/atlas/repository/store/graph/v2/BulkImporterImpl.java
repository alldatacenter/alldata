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
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.BulkImporter;
import org.apache.atlas.repository.store.graph.v2.bulkimport.ImportStrategy;
import org.apache.atlas.repository.store.graph.v2.bulkimport.MigrationImport;
import org.apache.atlas.repository.store.graph.v2.bulkimport.RegularImport;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.Constants;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

import static org.apache.atlas.repository.Constants.HISTORICAL_GUID_PROPERTY_KEY;

@Component
public class BulkImporterImpl implements BulkImporter {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasEntityStoreV2.class);

    private final AtlasEntityStore entityStore;
    private final AtlasGraph atlasGraph;
    private AtlasTypeRegistry typeRegistry;

    @Inject
    public BulkImporterImpl(AtlasGraph atlasGraph, AtlasEntityStore entityStore, AtlasTypeRegistry typeRegistry) {
        this.atlasGraph = atlasGraph;
        this.entityStore = entityStore;
        this.typeRegistry = typeRegistry;
    }

    @Override
    public EntityMutationResponse bulkImport(EntityImportStream entityStream, AtlasImportResult importResult) throws AtlasBaseException {
        ImportStrategy importStrategy = null;

        if (importResult.getRequest().getOptions() != null &&
                importResult.getRequest().getOptions().containsKey(AtlasImportRequest.OPTION_KEY_MIGRATION)) {
            importStrategy = new MigrationImport(this.atlasGraph, new AtlasGraphProvider(), this.typeRegistry);
        } else {
            importStrategy = new RegularImport(this.atlasGraph, this.entityStore, this.typeRegistry);
        }

        LOG.info("BulkImportImpl: {}", importStrategy.getClass().getSimpleName());
        return importStrategy.run(entityStream, importResult);
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

    @VisibleForTesting
    public static float updateImportProgress(Logger log, long currentIndex, long streamSize, float currentPercent, String additionalInfo) {
        final double tolerance   = 0.000001;
        final int    MAX_PERCENT = 100;

        long     maxSize        = (currentIndex <= streamSize) ? streamSize : currentIndex;
        if (maxSize <= 0) {
            return currentPercent;
        }

        float   percent        = (float) ((currentIndex * MAX_PERCENT) / maxSize);
        boolean updateLog      = Double.compare(percent, currentPercent) > tolerance;
        float   updatedPercent = (MAX_PERCENT < maxSize) ? percent : ((updateLog) ? ++currentPercent : currentPercent);

        if (updateLog) {
            log.info("bulkImport(): progress: {}% (of {}) - {}", (int) Math.ceil(percent), maxSize, additionalInfo);
        }

        return updatedPercent;
    }

    public static void updateImportMetrics(String prefix, List<AtlasEntityHeader> list, Set<String> processedGuids, AtlasImportResult importResult) {
        if (list == null) {
            return;
        }

        for (AtlasEntityHeader h : list) {
            if (processedGuids.contains(h.getGuid())) {
                continue;
            }

            processedGuids.add(h.getGuid());
            importResult.incrementMeticsCounter(String.format(prefix, h.getTypeName()));
        }
    }

    public static void updateVertexGuid(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry, EntityGraphRetriever entityGraphRetriever, AtlasEntity entity) {
        String entityGuid = entity.getGuid();
        AtlasObjectId objectId = entityGraphRetriever.toAtlasObjectIdWithoutGuid(entity);

        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(entity.getTypeName());
        String vertexGuid = null;
        try {
            vertexGuid = AtlasGraphUtilsV2.getGuidByUniqueAttributes(atlasGraph, entityType, objectId.getUniqueAttributes());
        } catch (AtlasBaseException e) {
            LOG.warn("Entity: {}: Does not exist!", objectId);
            return;
        }

        if (StringUtils.isEmpty(vertexGuid) || vertexGuid.equals(entityGuid)) {
            return;
        }

        AtlasVertex v = AtlasGraphUtilsV2.findByGuid(atlasGraph, vertexGuid);
        if (v == null) {
            return;
        }

        addHistoricalGuid(v, vertexGuid);
        AtlasGraphUtilsV2.setProperty(v, Constants.GUID_PROPERTY_KEY, entityGuid);

        LOG.warn("GUID Updated: Entity: {}: from: {}: to: {}", objectId, vertexGuid, entity.getGuid());
    }

    public static void addHistoricalGuid(AtlasVertex v, String vertexGuid) {
        String existingJson = AtlasGraphUtilsV2.getProperty(v, HISTORICAL_GUID_PROPERTY_KEY, String.class);

        AtlasGraphUtilsV2.setProperty(v, HISTORICAL_GUID_PROPERTY_KEY, getJsonArray(existingJson, vertexGuid));
    }
}
