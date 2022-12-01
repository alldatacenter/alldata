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
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.util.UniqueList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IncrementalExportEntityProvider implements ExtractStrategy {
    private static final Logger LOG = LoggerFactory.getLogger(IncrementalExportEntityProvider.class);

    private static final String QUERY_PARAMETER_START_GUID = "startGuid";
    private static final String QUERY_PARAMETER_MODIFICATION_TIMESTAMP = "modificationTimestamp";

    private AtlasGraph atlasGraph;

    private static final String QUERY_DB = "g.V().has('__guid', startGuid)";
    private static final String QUERY_TABLE = QUERY_DB + ".in('__hive_table.db')";
    private static final String QUERY_SD = QUERY_TABLE + ".out('__hive_table.sd')";
    private static final String QUERY_COLUMN = QUERY_TABLE + ".out('__hive_table.columns')";
    private static final String TRANSFORM_CLAUSE = ".project('__guid').by('__guid').dedup().toList()";
    private static final String TIMESTAMP_CLAUSE = ".has('__modificationTimestamp', gt(modificationTimestamp))";

    private static final String QUERY_TABLE_DB = QUERY_DB + ".out('__hive_table.db')";
    private static final String QUERY_TABLE_SD = QUERY_DB + ".out('__hive_table.sd')";
    private static final String QUERY_TABLE_COLUMNS = QUERY_DB + ".out('__hive_table.columns')";

    private ScriptEngine scriptEngine;

    @Inject
    public IncrementalExportEntityProvider(AtlasGraph atlasGraph) {
        this.atlasGraph = atlasGraph;
        try {
            this.scriptEngine = atlasGraph.getGremlinScriptEngine();
        } catch (AtlasBaseException e) {
            LOG.error("Error instantiating script engine.", e);
        }
    }

    @Override
    public void fullFetch(AtlasEntity entity, ExportService.ExportContext context) {
        populate(entity.getGuid(), context.changeMarker, context.guidsToProcess);
    }

    @Override
    public void connectedFetch(AtlasEntity entity, ExportService.ExportContext context) {
        //starting entity is hive_table
        context.guidsToProcess.addAll(fetchGuids(entity.getGuid(), QUERY_TABLE_DB, context.changeMarker));
        context.guidsToProcess.addAll(fetchGuids(entity.getGuid(), QUERY_TABLE_SD, context.changeMarker));
        context.guidsToProcess.addAll(fetchGuids(entity.getGuid(), QUERY_TABLE_COLUMNS, context.changeMarker));
    }

    public void populate(String dbEntityGuid, long timeStamp, UniqueList<String> guidsToProcess) {
        if(timeStamp == 0L) {
            full(dbEntityGuid, guidsToProcess);
        } else {
            partial(dbEntityGuid, timeStamp, guidsToProcess);
        }
    }

    @Override
    public void close() {
        if (scriptEngine != null) {
            atlasGraph.releaseGremlinScriptEngine(scriptEngine);
        }
    }

    private void partial(String dbEntityGuid, long timeStamp, UniqueList<String> guidsToProcess) {
        guidsToProcess.addAll(fetchGuids(dbEntityGuid, QUERY_TABLE, timeStamp));
        guidsToProcess.addAll(fetchGuids(dbEntityGuid, QUERY_SD, timeStamp));
        guidsToProcess.addAll(fetchGuids(dbEntityGuid, QUERY_COLUMN, timeStamp));
    }

    private void full(String dbEntityGuid, UniqueList<String> guidsToProcess) {
        guidsToProcess.addAll(fetchGuids(dbEntityGuid, QUERY_TABLE, 0L));
    }

    private List<String> fetchGuids(final String dbEntityGuid, String query, long timeStamp) {
        Map<String, Object> bindings = new HashMap<String, Object>() {{
            put(QUERY_PARAMETER_START_GUID, dbEntityGuid);
        }};

        String queryWithClause = query;
        if(timeStamp > 0L) {
            bindings.put(QUERY_PARAMETER_MODIFICATION_TIMESTAMP, timeStamp);
            queryWithClause = queryWithClause.concat(TIMESTAMP_CLAUSE);
        }

        return executeGremlinQuery(queryWithClause, bindings);
    }

    private List<String> executeGremlinQuery(String query, Map<String, Object> bindings) {
        try {
            List<String> guids = new ArrayList<>();
            String queryWithTransform = query + TRANSFORM_CLAUSE;
            List<Map<String, Object>> result = (List<Map<String, Object>>)
                    atlasGraph.executeGremlinScript(scriptEngine, bindings, queryWithTransform, false);
            if (result == null) {
                return guids;
            }

            for (Map<String, Object> item : result) {
                guids.add((String) item.get(EntitiesExtractor.PROPERTY_GUID));
            }

            return guids;

        } catch (ScriptException e) {
            LOG.error("error executing query: {}: bindings: {}", query, bindings, e);
            return null;
        }
    }
}
