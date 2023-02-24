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

import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.AtlasGremlin3QueryProvider;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.repository.impexp.StartEntityFetchByExportRequest.BINDING_PARAMETER_ATTR_NAME;
import static org.apache.atlas.repository.impexp.StartEntityFetchByExportRequest.BINDING_PARAMETER_TYPENAME;
import static org.apache.atlas.repository.impexp.StartEntityFetchByExportRequest.BINDING_PARAMTER_ATTR_VALUE;
import static org.testng.Assert.assertEquals;

@Guice(modules = TestModules.TestOnlyModule.class)
public class StartEntityFetchByExportRequestTest extends AtlasTestBase {

    @Inject
    private AtlasGraph atlasGraph;

    @Inject
    private AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    private AtlasGremlin3QueryProvider atlasGremlin3QueryProvider;
    private StartEntityFetchByExportRequestSpy startEntityFetchByExportRequestSpy;

    private class StartEntityFetchByExportRequestSpy extends StartEntityFetchByExportRequest {
        String generatedQuery;
        Map<String, Object> suppliedBindingsMap;

        public StartEntityFetchByExportRequestSpy(AtlasGraph atlasGraph, AtlasTypeRegistry typeRegistry) {
            super(atlasGraph, typeRegistry, atlasGremlin3QueryProvider);
        }

        @Override
        List<String> executeGremlinQuery(String query, Map<String, Object> bindings) {
            this.generatedQuery = query;
            this.suppliedBindingsMap = bindings;

            return Collections.EMPTY_LIST;
        }

        public String getGeneratedQuery() {
            return generatedQuery;
        }

        public Map<String, Object> getSuppliedBindingsMap() {
            return suppliedBindingsMap;
        }
    }

    @BeforeClass void setup() throws IOException, AtlasBaseException {
        super.basicSetup(typeDefStore, typeRegistry);
        atlasGremlin3QueryProvider = new AtlasGremlin3QueryProvider();
        startEntityFetchByExportRequestSpy = new StartEntityFetchByExportRequestSpy(atlasGraph, typeRegistry);
    }

    @Test
    public void fetchTypeGuid() {
        String exportRequestJson = "{ \"itemsToExport\": [ { \"typeName\": \"hive_db\", \"guid\": \"111-222-333\" } ]}";
        AtlasExportRequest exportRequest = AtlasType.fromJson(exportRequestJson, AtlasExportRequest.class);

        List<AtlasObjectId> objectGuidMap = startEntityFetchByExportRequestSpy.get(exportRequest);

        assertEquals(objectGuidMap.get(0).getGuid(), "111-222-333");
    }

    @Test
    public void fetchTypeUniqueAttributes() {
        String exportRequestJson = "{ \"itemsToExport\": [ { \"typeName\": \"hive_db\", \"uniqueAttributes\": {\"qualifiedName\": \"stocks@cl1\"} } ]}";
        AtlasExportRequest exportRequest = AtlasType.fromJson(exportRequestJson, AtlasExportRequest.class);

        startEntityFetchByExportRequestSpy.get(exportRequest);
        assertEquals(startEntityFetchByExportRequestSpy.getGeneratedQuery(), startEntityFetchByExportRequestSpy.getQueryTemplateForMatchType(exportRequest.getMatchTypeOptionValue()));
        assertEquals(startEntityFetchByExportRequestSpy.getSuppliedBindingsMap().get(BINDING_PARAMETER_TYPENAME), "hive_db");
        assertEquals(startEntityFetchByExportRequestSpy.getSuppliedBindingsMap().get(BINDING_PARAMETER_ATTR_NAME), "Referenceable.qualifiedName");
        assertEquals(startEntityFetchByExportRequestSpy.getSuppliedBindingsMap().get(BINDING_PARAMTER_ATTR_VALUE), "stocks@cl1");
    }
}
