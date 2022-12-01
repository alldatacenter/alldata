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


import com.google.common.collect.ImmutableList;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasExportRequest;
import org.apache.atlas.model.impexp.AtlasImportRequest;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.impexp.AtlasServer;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.store.graph.v2.EntityGraphMapper;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.apache.commons.io.IOUtils;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.apache.atlas.model.impexp.AtlasExportRequest.OPTION_KEY_REPLICATED_TO;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runExportWithParameters;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runImportWithParameters;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public class ReplicationEntityAttributeTest extends AtlasTestBase {
    private final String ENTITIES_SUB_DIR = "stocksDB-Entities";
    private final String EXPORT_REQUEST_FILE = "export-replicatedTo";
    private final String IMPORT_REQUEST_FILE = "import-replicatedFrom";

    private  String REPLICATED_TO_CLUSTER_NAME = "";
    private  String REPLICATED_FROM_CLUSTER_NAME = "";

    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private EntityGraphMapper graphMapper;

    @Inject
    ExportService exportService;

    @Inject
    ImportService importService;

    @Inject
    AtlasServerService atlasServerService;

    @Inject
    private AtlasEntityStoreV2 entityStore;

    private InputStream inputStream;

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        basicSetup(typeDefStore, typeRegistry);
        createEntities(entityStore, ENTITIES_SUB_DIR, new String[]{"db", "table-columns"});

        AtlasType refType = typeRegistry.getType("Referenceable");
        AtlasEntityDef entityDef = (AtlasEntityDef) typeDefStore.getByName(refType.getTypeName());
        assertNotNull(entityDef);
    }

    @BeforeMethod
    public void setupTest() {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
    }

    @Test
    public void exportWithReplicationToOption_AddsClusterObjectIdToReplicatedFromAttribute() throws AtlasBaseException, IOException {
        final int expectedEntityCount = 2;

        AtlasExportRequest request = getUpdateMetaInfoUpdateRequest();
        InputStream inputStream = runExportWithParameters(exportService, request);

        assertNotNull(inputStream);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(inputStream, baos);
        this.inputStream = new ByteArrayInputStream(baos.toByteArray());

        ZipSource zipSource = new ZipSource(new ByteArrayInputStream(baos.toByteArray()));
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), expectedEntityCount);

        assertCluster(
                AuditsWriter.getServerNameFromFullName(REPLICATED_TO_CLUSTER_NAME),
                REPLICATED_TO_CLUSTER_NAME, null);

        assertReplicationAttribute(Constants.ATTR_NAME_REPLICATED_TO);
    }

    @Test
    public void fullServerName() {
        final String expectedClusterName = "cl1";

        assertEquals(AuditsWriter.getServerNameFromFullName(""), "");
        assertEquals(AuditsWriter.getServerNameFromFullName(expectedClusterName), expectedClusterName);
        assertEquals(AuditsWriter.getServerNameFromFullName("SFO$cl1"), expectedClusterName);
        assertEquals(AuditsWriter.getServerNameFromFullName("cl1$"), expectedClusterName);
        assertEquals(AuditsWriter.getServerNameFromFullName("$cl1"), expectedClusterName);
    }


    @Test(dependsOnMethods = "exportWithReplicationToOption_AddsClusterObjectIdToReplicatedFromAttribute")
    public void importWithReplicationFromOption_AddsClusterObjectIdToReplicatedFromAttribute() throws AtlasBaseException, IOException {
        AtlasImportRequest request = getImportRequestWithReplicationOption();
        AtlasImportResult importResult = runImportWithParameters(importService, request, inputStream);

        assertCluster(
                AuditsWriter.getServerNameFromFullName(REPLICATED_FROM_CLUSTER_NAME),
                REPLICATED_FROM_CLUSTER_NAME, importResult);
        assertReplicationAttribute(Constants.ATTR_NAME_REPLICATED_FROM);
    }

    @Test
    public void replKeyGuidFinder() {
        String expectedDBQualifiedName = "largedb@cl1";

        assertEquals(AuditsWriter.ReplKeyGuidFinder.extractHiveDBQualifiedName("largedb.testtable_0.col101@cl1"), expectedDBQualifiedName);
        assertEquals(AuditsWriter.ReplKeyGuidFinder.extractHiveDBQualifiedName("largedb.testtable_0@cl1"), expectedDBQualifiedName);
    }

    private void assertReplicationAttribute(String attrNameReplication) throws AtlasBaseException {
        pauseForIndexCreation();
        AtlasEntity.AtlasEntitiesWithExtInfo entities = entityStore.getByIds(ImmutableList.of(DB_GUID, TABLE_GUID));
        for (AtlasEntity e : entities.getEntities()) {
            Object ex = e.getAttribute(attrNameReplication);
            assertNotNull(ex);

            List<String> attrValue = (List) ex;
            assertEquals(attrValue.size(), 1);
        }
    }

    private void assertCluster(String name, String fullName, AtlasImportResult importResult) throws AtlasBaseException {
        AtlasServer actual = atlasServerService.get(new AtlasServer(name, fullName));

        assertNotNull(actual);
        assertEquals(actual.getName(), name);
        assertEquals(actual.getFullName(), fullName);

        if(importResult != null) {
            assertClusterAdditionalInfo(actual, importResult);
        }
    }

    private void assertClusterAdditionalInfo(AtlasServer cluster, AtlasImportResult importResult) throws AtlasBaseException {
        AtlasExportRequest request = importResult.getExportResult().getRequest();
        AtlasEntityType type = (AtlasEntityType) typeRegistry.getType(request.getItemsToExport().get(0).getTypeName());
        AtlasEntity.AtlasEntityWithExtInfo entity = entityStore.getByUniqueAttributes(type, request.getItemsToExport().get(0).getUniqueAttributes());
        long actualLastModifiedTimestamp = (long) cluster.getAdditionalInfoRepl(entity.getEntity().getGuid());

        assertTrue(cluster.getAdditionalInfo().size() > 0);
        assertEquals(actualLastModifiedTimestamp, importResult.getExportResult().getChangeMarker());
    }

    private AtlasExportRequest getUpdateMetaInfoUpdateRequest() {
        AtlasExportRequest request = getExportRequestWithReplicationOption();
        request.getOptions().put(AtlasExportRequest.OPTION_KEY_REPLICATED_TO, REPLICATED_TO_CLUSTER_NAME);

        return request;
    }

    private AtlasExportRequest getExportRequestWithReplicationOption() {
        try {
            AtlasExportRequest request = TestResourceFileUtils.readObjectFromJson(ENTITIES_SUB_DIR, EXPORT_REQUEST_FILE, AtlasExportRequest.class);
            REPLICATED_TO_CLUSTER_NAME = (String) request.getOptions().get(OPTION_KEY_REPLICATED_TO);
            return request;
        } catch (IOException e) {
            throw new SkipException(String.format("getExportRequestWithReplicationOption: '%s' could not be loaded.", EXPORT_REQUEST_FILE));
        }
    }

    private AtlasImportRequest getImportRequestWithReplicationOption() {
        try {
            AtlasImportRequest request = TestResourceFileUtils.readObjectFromJson(ENTITIES_SUB_DIR, IMPORT_REQUEST_FILE, AtlasImportRequest.class);
            REPLICATED_FROM_CLUSTER_NAME = request.getOptions().get(AtlasImportRequest.OPTION_KEY_REPLICATED_FROM);
            return request;
        } catch (IOException e) {
            throw new SkipException(String.format("getExportRequestWithReplicationOption: '%s' could not be loaded.", IMPORT_REQUEST_FILE));
        }
    }
}
