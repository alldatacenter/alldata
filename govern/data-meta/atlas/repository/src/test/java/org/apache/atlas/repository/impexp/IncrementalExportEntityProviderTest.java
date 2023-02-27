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

import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.util.UniqueList;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.script.ScriptEngine;
import java.io.IOException;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public class IncrementalExportEntityProviderTest extends AtlasTestBase {
    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private AtlasEntityStoreV2 entityStore;

    @Inject
    private AtlasGraph atlasGraph;

    private IncrementalExportEntityProvider incrementalExportEntityProvider;
    private ScriptEngine gremlinScriptEngine;

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        basicSetup(typeDefStore, typeRegistry);
        RequestContext.get().setImportInProgress(true);
        createEntities(entityStore, ENTITIES_SUB_DIR, new String[] { "db", "table-columns"});
        final String[] entityGuids = {DB_GUID, TABLE_GUID};
        verifyCreatedEntities(entityStore, entityGuids, 2);

        gremlinScriptEngine = atlasGraph.getGremlinScriptEngine();
        incrementalExportEntityProvider = new IncrementalExportEntityProvider(atlasGraph);
    }

    @AfterClass
    public void tearDown() {
        if(gremlinScriptEngine != null) {
            atlasGraph.releaseGremlinScriptEngine(gremlinScriptEngine);
        }
    }

    @Test
    public void verify() {
        executeQueries(0L, 1);
        executeQueries(1L, 9);
    }

    private void executeQueries(long timeStamp, int expectedEntityCount) {
        UniqueList<String> uniqueList = new UniqueList<>();
        incrementalExportEntityProvider.populate(DB_GUID, timeStamp, uniqueList);

        for (String g : uniqueList.getList()) {
            assertTrue(g instanceof String);
        }

        assertEquals(uniqueList.size(), expectedEntityCount);
    }
}
