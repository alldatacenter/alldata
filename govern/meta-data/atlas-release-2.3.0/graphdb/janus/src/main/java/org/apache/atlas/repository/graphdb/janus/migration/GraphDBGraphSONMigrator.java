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

package org.apache.atlas.repository.graphdb.janus.migration;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.MigrationStatus;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.graphdb.GraphDBMigrator;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.AtlasPerfTracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

import static org.apache.atlas.repository.graphdb.janus.AtlasJanusGraphDatabase.getBulkLoadingGraphInstance;
import static org.apache.atlas.repository.graphdb.janus.AtlasJanusGraphDatabase.getGraphInstance;

@Component
public class GraphDBGraphSONMigrator implements GraphDBMigrator {
    private static final Logger LOG      = LoggerFactory.getLogger(GraphDBMigrator.class);
    private static final Logger PERF_LOG = AtlasPerfTracer.getPerfLogger("GraphDBMigrator");

    private final TypesDefScrubber typesDefStrubberForMigrationImport = new TypesDefScrubber();

    @Override
    public AtlasTypesDef getScrubbedTypesDef(String jsonStr) {
        AtlasTypesDef typesDef = AtlasType.fromJson(jsonStr, AtlasTypesDef.class);

        return typesDefStrubberForMigrationImport.scrub(typesDef);
    }

    @Override
    public void importData(AtlasTypeRegistry typeRegistry, InputStream fs) throws AtlasBaseException {
        AtlasPerfTracer perf = null;

        try {
            LOG.info("Starting loadLegacyGraphSON...");

            if (AtlasPerfTracer.isPerfTraceEnabled(PERF_LOG)) {
                perf = AtlasPerfTracer.getPerfTracer(PERF_LOG, "loadLegacyGraphSON");
            }

            AtlasGraphSONReader legacyGraphSONReader = AtlasGraphSONReader.build().
                    relationshipCache(new ElementProcessors(typeRegistry, typesDefStrubberForMigrationImport)).
                    schemaDB(getGraphInstance()).
                    bulkLoadingDB(getBulkLoadingGraphInstance()).
                    create();

            legacyGraphSONReader.readGraph(fs);
        } catch (Exception ex) {
            LOG.error("Error loading loadLegacyGraphSON2", ex);

            throw new AtlasBaseException(ex);
        } finally {
            AtlasPerfTracer.log(perf);

            LOG.info("Done! loadLegacyGraphSON.");
        }
    }

    @Override
    public MigrationStatus getMigrationStatus() {
        return ReaderStatusManager.get(getGraphInstance());
    }
}
