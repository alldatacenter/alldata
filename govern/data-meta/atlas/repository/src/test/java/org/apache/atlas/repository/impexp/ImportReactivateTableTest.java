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
package org.apache.atlas.repository.impexp;

import com.google.inject.Inject;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.AtlasRelationship;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.atlas.AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getInputStreamFrom;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getDefaultImportRequest;
import static org.apache.atlas.utils.TestLoadModelUtils.loadFsModel;
import static org.apache.atlas.utils.TestLoadModelUtils.loadHiveModel;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runImportWithParameters;
import static org.apache.atlas.type.AtlasTypeUtil.toAtlasRelatedObjectId;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class ImportReactivateTableTest extends AtlasTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(ImportReactivateTableTest.class);

    private static final String ENTITY_TYPE_COL = "hive_column";

    private static final String ENTITY_GUID_TABLE_WITH_REL_ATTRS = "e19e5683-d9ae-436a-af1e-0873582d0f1e";
    private static final String ENTITY_GUID_TABLE_WITHOUT_REL_ATTRS = "027a987e-867a-4c98-ac1e-c5ded41130d3";

    private static final String REPL_FROM = "cl1";
    private static final String REPL_TRANSFORMER = "[{\"conditions\":{\"__entity\":\"topLevel: \"}," +
            "\"action\":{\"__entity\":\"ADD_CLASSIFICATION: cl1_replicated\"}}," +
            "{\"action\":{\"__entity.replicatedTo\":\"CLEAR:\",\"__entity.replicatedFrom\":\"CLEAR:\"}}," +
            "{\"conditions\":{\"hive_db.clusterName\":\"EQUALS: cl1\"},\"action\":{\"hive_db.clusterName\":\"SET: cl2\"}}," +
            "{\"conditions\":{\"hive_db.location\":\"STARTS_WITH_IGNORE_CASE: file:///\"}," +
            "\"action\":{\"hive_db.location\":\"REPLACE_PREFIX: = :file:///=file:///\"}}," +
            "{\"conditions\":{\"hive_storagedesc.location\":\"STARTS_WITH_IGNORE_CASE: file:///\"}," +
            "\"action\":{\"hive_storagedesc.location\":\"REPLACE_PREFIX: = :file:///=file:///\"}}]";

    @Inject
    private ImportService importService;

    @Inject
    private AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasEntityStore entityStore;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @BeforeClass
    public void initialize() throws Exception {
        super.initialize();
    }

    @BeforeTest
    public void setup() throws IOException, AtlasBaseException {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
        basicSetup(typeDefStore, typeRegistry);
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    private void importSeedData() throws AtlasBaseException, IOException {
        loadFsModel(typeDefStore, typeRegistry);
        loadHiveModel(typeDefStore, typeRegistry);
        AtlasImportRequest atlasImportRequest = getDefaultImportRequest();

        atlasImportRequest.setOption("replicatedFrom", REPL_FROM);
        atlasImportRequest.setOption("transformers", REPL_TRANSFORMER);

        runImportWithParameters(importService, atlasImportRequest, getDataWithoutRelationshipAttrs());
        runImportWithParameters(importService, atlasImportRequest, getDataWithRelationshipAttrs());
    }

    public static InputStream getDataWithRelationshipAttrs() {
        return getInputStreamFrom("repl_exp_1.zip");
    }

    public static InputStream getDataWithoutRelationshipAttrs() {
        return getInputStreamFrom("stocks.zip");
    }

    @Test
    public void testWithRelationshipAttr() throws AtlasBaseException, IOException {
        testReactivation(ENTITY_GUID_TABLE_WITH_REL_ATTRS, 2);
    }

    @Test
    public void testWithoutRelationshipAttr() throws AtlasBaseException, IOException {
        testReactivation(ENTITY_GUID_TABLE_WITHOUT_REL_ATTRS, 7);
    }

    public void testReactivation(String tableEntityGuid, int columnCount) throws AtlasBaseException, IOException {
        importSeedData();

        AtlasEntity.AtlasEntityWithExtInfo entity = entityStore.getById(tableEntityGuid);
        EntityMutationResponse response = createColumn(entity.getEntity());
        String columnGuid = response.getCreatedEntities().get(0).getGuid();
        assertNotNull(columnGuid);
        columnCount++;

        entityStore.deleteById(tableEntityGuid);
        entity = entityStore.getById(tableEntityGuid);
        assertEquals(entity.getEntity().getStatus(), AtlasEntity.Status.DELETED);

        importSeedData();

        AtlasEntity atlasEntity = entityStore.getById(tableEntityGuid).getEntity();

        assertEquals(atlasEntity.getStatus(), AtlasEntity.Status.ACTIVE);
        List<AtlasRelatedObjectId> columns = (List<AtlasRelatedObjectId>) atlasEntity.getRelationshipAttribute("columns");
        assertEquals(columns.size(), columnCount);

        int activeColumnCount = 0;
        int deletedColumnCount = 0;
        for (AtlasRelatedObjectId column : columns) {
            if (column.getGuid().equals(columnGuid)){
                assertEquals(column.getEntityStatus(), AtlasEntity.Status.DELETED);
                assertEquals(column.getRelationshipStatus(), AtlasRelationship.Status.DELETED);
                deletedColumnCount++;
            }else{
                assertEquals(column.getEntityStatus(), AtlasEntity.Status.ACTIVE);
                assertEquals(column.getRelationshipStatus(), AtlasRelationship.Status.ACTIVE);
                activeColumnCount++;
            }
        }
        assertEquals(activeColumnCount, --columnCount);
        assertEquals(deletedColumnCount, 1);
    }

    private EntityMutationResponse createColumn(AtlasEntity tblEntity) throws AtlasBaseException {
        AtlasEntity ret = new AtlasEntity(ENTITY_TYPE_COL);
        String name = "new_column";

        ret.setAttribute("name", name);
        ret.setAttribute(REFERENCEABLE_ATTRIBUTE_NAME, name + REPL_FROM);
        ret.setAttribute("type", "int");
        ret.setAttribute("comment", name);

        ret.setRelationshipAttribute("table", toAtlasRelatedObjectId(tblEntity));
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(ret), false);

        return response;
    }
}
