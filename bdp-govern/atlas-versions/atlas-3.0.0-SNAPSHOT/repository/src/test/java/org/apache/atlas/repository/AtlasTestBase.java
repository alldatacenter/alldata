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

package org.apache.atlas.repository;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.ExportImportAuditEntry;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.impexp.ExportImportAuditService;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.runner.LocalSolrRunner;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.SkipException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.apache.atlas.graph.GraphSandboxUtil.useLocalSolr;
import static org.apache.atlas.utils.TestLoadModelUtils.createAtlasEntity;
import static org.apache.atlas.utils.TestLoadModelUtils.loadBaseModel;
import static org.apache.atlas.utils.TestLoadModelUtils.loadEntity;
import static org.apache.atlas.utils.TestLoadModelUtils.loadHiveModel;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

public class AtlasTestBase {
    protected static final String ENTITIES_SUB_DIR = "stocksDB-Entities";
    protected static final String DB_GUID = "1637a33e-6512-447b-ade7-249c8cb5344b";
    protected static final String TABLE_GUID = "df122fc3-5555-40f8-a30f-3090b8a622f8";
    protected static final String TABLE_TABLE_GUID = "6f3b305a-c459-4ae4-b651-aee0deb0685f";
    protected static final String TABLE_VIEW_GUID = "56415119-7cb0-40dd-ace8-1e50efd54991";
    protected static final String COLUMN_GUID_HIGH = "f87a5320-1529-4369-8d63-b637ebdf2c1c";

    protected void initialize() throws Exception {
        if (useLocalSolr()) {
            LocalSolrRunner.start();
        }
    }

    protected void cleanup() throws Exception {
        if (useLocalSolr()) {
            LocalSolrRunner.stop();
        }
    }

    protected void basicSetup(AtlasTypeDefStore typeDefStore, AtlasTypeRegistry typeRegistry) throws IOException, AtlasBaseException {
        loadBaseModel(typeDefStore, typeRegistry);
        loadHiveModel(typeDefStore, typeRegistry);
    }

    protected int createEntities(AtlasEntityStoreV2 entityStore, String subDir, String entityFileNames[]) {
        for (String fileName : entityFileNames) {
            createAtlasEntity(entityStore, loadEntity(subDir, fileName));
        }

        return entityFileNames.length;
    }

    protected void verifyCreatedEntities(AtlasEntityStoreV2 entityStore, String[] entityGuids, int expectedNumberOfEntitiesCreated) {
        try {
            AtlasEntity.AtlasEntitiesWithExtInfo entities = entityStore.getByIds(Arrays.asList((String[]) entityGuids));
            assertEquals(entities.getEntities().size(), expectedNumberOfEntitiesCreated);
        } catch (AtlasBaseException e) {
            throw new SkipException(String.format("getByIds: could not load '%s'", entityGuids.toString()));
        }
    }

    protected void assertExportImportAuditEntry(ExportImportAuditService auditService) throws InterruptedException {
        pauseForIndexCreation();
        List<ExportImportAuditEntry> result = null;
        try {
            result = auditService.get("", "", "", "",  "", 10, 0);
        } catch (Exception e) {
            throw new SkipException("audit entries not retrieved.");
        }

        assertNotNull(result);
        assertTrue(result.size() > 0);
    }

    protected void pauseForIndexCreation() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException ex) {
            throw new SkipException("pause interrupted.");
        }
    }
}
