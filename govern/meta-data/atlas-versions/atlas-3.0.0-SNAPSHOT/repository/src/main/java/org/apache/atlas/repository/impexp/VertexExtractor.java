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

package org.apache.atlas.repository.impexp;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.util.AtlasGremlinQueryProvider;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.repository.impexp.EntitiesExtractor.PROPERTY_GUID;

public class VertexExtractor implements ExtractStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(VertexExtractor.class);

    private static final String PROPERTY_IS_PROCESS = "isProcess";
    private static final String QUERY_BINDING_START_GUID = "startGuid";

    private final AtlasGremlinQueryProvider gremlinQueryProvider;

    private final Map<String, Object> bindings;
    private AtlasGraph atlasGraph;
    private AtlasTypeRegistry typeRegistry;
    private ScriptEngine scriptEngine;

    public VertexExtractor(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry) {
        this.atlasGraph = atlasGraph;
        this.typeRegistry = typeRegistry;
        try {
            this.scriptEngine = atlasGraph.getGremlinScriptEngine();
        } catch (AtlasBaseException e) {
            LOG.error("Script Engine: Instantiation failed!");
        }
        this.gremlinQueryProvider = AtlasGremlinQueryProvider.INSTANCE;
        this.bindings = new HashMap<>();
    }

    @Override
    public void fullFetch(AtlasEntity entity, ExportService.ExportContext context) {
        if (LOG.isDebugEnabled()){
            LOG.debug("==> fullFetch({}): guidsToProcess {}", AtlasTypeUtil.getAtlasObjectId(entity), context.guidsToProcess.size());
        }

        String query = this.gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_BY_GUID_FULL);

        bindings.clear();
        bindings.put(QUERY_BINDING_START_GUID, entity.getGuid());

        List<Map<String, Object>> result = executeGremlinQuery(query, context);

        if (CollectionUtils.isEmpty(result)) {
            return;
        }

        for (Map<String, Object> hashMap : result) {
            String guid = (String) hashMap.get(PROPERTY_GUID);
            boolean isLineage = (boolean) hashMap.get(PROPERTY_IS_PROCESS);

            if (context.getSkipLineage() && isLineage) continue;

            if (!context.guidsProcessed.contains(guid)) {
                context.addToBeProcessed(isLineage, guid, ExportService.TraversalDirection.BOTH);
            }
        }
    }

    @Override
    public void connectedFetch(AtlasEntity entity, ExportService.ExportContext context) {
        if (LOG.isDebugEnabled()){
            LOG.debug("==> connectedFetch({}): guidsToProcess {}", AtlasTypeUtil.getAtlasObjectId(entity), context.guidsToProcess.size());
        }

        ExportService.TraversalDirection direction = context.guidDirection.get(entity.getGuid());

        if (direction == null || direction == ExportService.TraversalDirection.UNKNOWN) {
            getConnectedEntityGuids(entity, context, ExportService.TraversalDirection.OUTWARD, ExportService.TraversalDirection.INWARD);
        } else {
            if (isProcessEntity(entity)) {
                direction = ExportService.TraversalDirection.OUTWARD;
            }

            getConnectedEntityGuids(entity, context, direction);
        }
    }

    @Override
    public void close() {
        if (scriptEngine != null) {
            atlasGraph.releaseGremlinScriptEngine(scriptEngine);
        }
    }

    private void getConnectedEntityGuids(AtlasEntity entity, ExportService.ExportContext context, ExportService.TraversalDirection... directions) {
        if (directions == null) {
            return;
        }

        for (ExportService.TraversalDirection direction : directions) {
            String query = getQueryForTraversalDirection(direction);

            bindings.clear();
            bindings.put(QUERY_BINDING_START_GUID, entity.getGuid());

            List<Map<String, Object>> result = executeGremlinQuery(query, context);

            if (CollectionUtils.isEmpty(result)) {
                continue;
            }

            for (Map<String, Object> hashMap : result) {
                String guid = (String) hashMap.get(PROPERTY_GUID);
                ExportService.TraversalDirection currentDirection = context.guidDirection.get(guid);
                boolean isLineage = (boolean) hashMap.get(PROPERTY_IS_PROCESS);

                if (context.skipLineage && isLineage) continue;

                if (currentDirection == null) {
                    context.addToBeProcessed(isLineage, guid, direction);

                } else if (currentDirection == ExportService.TraversalDirection.OUTWARD && direction == ExportService.TraversalDirection.INWARD) {
                    // the entity should be reprocessed to get inward entities
                    context.guidsProcessed.remove(guid);
                    context.addToBeProcessed(isLineage, guid, direction);
                }
            }
        }
    }

    private boolean isProcessEntity(AtlasEntity entity) {
        String typeName = entity.getTypeName();
        AtlasEntityType entityType = typeRegistry.getEntityTypeByName(typeName);

        return entityType.isSubTypeOf(AtlasBaseTypeDef.ATLAS_TYPE_PROCESS);
    }

    private String getQueryForTraversalDirection(ExportService.TraversalDirection direction) {
        switch (direction) {
            case INWARD:
                return this.gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_BY_GUID_CONNECTED_IN_EDGE);

            default:
            case OUTWARD:
                return this.gremlinQueryProvider.getQuery(AtlasGremlinQueryProvider.AtlasGremlinQuery.EXPORT_BY_GUID_CONNECTED_OUT_EDGE);
        }
    }

    private List<Map<String, Object>> executeGremlinQuery(String query, ExportService.ExportContext context) {
        try {
            return (List<Map<String, Object>>) atlasGraph.executeGremlinScript(scriptEngine, bindings, query, false);
        } catch (ScriptException e) {
            LOG.error("Script execution failed for query: ", query, e);
            return null;
        }
    }
}
