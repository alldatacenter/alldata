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
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.graph.v1.DeleteHandlerDelegate;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityChangeNotifier;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import static org.apache.atlas.utils.TestLoadModelUtils.loadBaseModel;
import static org.apache.atlas.utils.TestLoadModelUtils.loadHiveModel;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runExportWithParameters;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.AssertJUnit.fail;

@Guice(modules = TestModules.TestOnlyModule.class)
public class ExportSkipLineageTest extends AtlasTestBase {
    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private EntityGraphMapper graphMapper;

    @Inject
    ExportService exportService;

    @Inject
    AtlasGraph atlasGraph;

    private DeleteHandlerDelegate deleteDelegate = mock(DeleteHandlerDelegate.class);
    private AtlasEntityChangeNotifier mockChangeNotifier = mock(AtlasEntityChangeNotifier.class);
    private AtlasEntityStoreV2 entityStore;

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        loadBaseModel(typeDefStore, typeRegistry);
        loadHiveModel(typeDefStore, typeRegistry);
        RequestContext.get().setImportInProgress(true);

        entityStore = new AtlasEntityStoreV2(atlasGraph, deleteDelegate, typeRegistry, mockChangeNotifier, graphMapper);
        createEntities(entityStore, ENTITIES_SUB_DIR, new String[]{"db", "table-columns", "table-view", "table-table-lineage"});
        final String[] entityGuids = {DB_GUID, TABLE_GUID, TABLE_TABLE_GUID, TABLE_VIEW_GUID};
        verifyCreatedEntities(entityStore, entityGuids, 4);
    }

    @BeforeMethod
    public void setupTest() {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
    }

    @Test
    public void exportWithoutLineage() throws IOException, AtlasBaseException {
        final int expectedEntityCount = 3;

        AtlasExportRequest request = getRequest();
        InputStream inputStream = runExportWithParameters(exportService, request);

        ZipSource source = new ZipSource(inputStream);
        AtlasEntity.AtlasEntityWithExtInfo entities = ZipFileResourceTestUtils.getEntities(source, expectedEntityCount);

        int count = 0;
        for (Map.Entry<String, AtlasEntity> entry : entities.getReferredEntities().entrySet()) {
            assertNotNull(entry.getValue());
            if(entry.getValue().getTypeName().equals("hive_process")) {
                fail("Process entities should not be part of export!");
            }
            count++;
        }

        assertEquals(count, expectedEntityCount);
    }

    private AtlasExportRequest getRequest() {
        final String filename = "export-skip-lineage";
        try {
            AtlasExportRequest request = TestResourceFileUtils.readObjectFromJson(ENTITIES_SUB_DIR, filename, AtlasExportRequest.class);

            return request;
        } catch (IOException e) {
            throw new SkipException(String.format("getRequest: '%s' could not be loaded.", filename));
        }
    }
}
