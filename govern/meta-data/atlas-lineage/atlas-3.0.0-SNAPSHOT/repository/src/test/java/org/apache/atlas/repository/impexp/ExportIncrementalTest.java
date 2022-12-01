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
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.util.UniqueList;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.apache.commons.io.IOUtils;
import org.testng.ITestContext;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.model.impexp.AtlasExportRequest.FETCH_TYPE_INCREMENTAL_CHANGE_MARKER;
import static org.apache.atlas.utils.TestLoadModelUtils.createTypes;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getEntities;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getZipSource;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runExportWithParameters;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runImportWithNoParameters;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public class ExportIncrementalTest extends AtlasTestBase {
    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    ExportService exportService;

    @Inject
    private ImportService importService;

    @Inject
    private AtlasEntityStoreV2 entityStore;

    private final String EXPORT_REQUEST_INCREMENTAL = "export-incremental";
    private final String EXPORT_REQUEST_CONNECTED = "export-connected";
    private AtlasClassificationType classificationTypeT1;
    private long nextTimestamp;

    private static final String EXPORT_INCREMENTAL = "incremental";
    private static final String QUALIFIED_NAME_DB = "db_test_1@02052019";
    private static final String QUALIFIED_NAME_TABLE_LINEAGE = "db_test_1.test_tbl_ctas_2@02052019";


    private static final String GUID_DB = "f0b72ab4-7452-4e42-ac74-2aee7728cce4";
    private static final String GUID_TABLE_2 = "8d0b834c-61ce-42d8-8f66-6fa51c36bccb";
    private static final String GUID_TABLE_CTAS_2 = "eaec545b-3ac7-4e1b-a497-bd4a2b6434a2";

    @BeforeClass
    public void setup() throws IOException, AtlasBaseException {
        basicSetup(typeDefStore, typeRegistry);
        RequestContext.get().setImportInProgress(true);
        classificationTypeT1 = createNewClassification();

        createEntities(entityStore, ENTITIES_SUB_DIR, new String[] { "db", "table-columns"});
        final String[] entityGuids = {DB_GUID, TABLE_GUID};
        verifyCreatedEntities(entityStore, entityGuids, 2);
    }

    @BeforeMethod
    public void setupTest() {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
    }

    @Test
    public void atT0_ReturnsAllEntities() throws AtlasBaseException, IOException {
        final int expectedEntityCount = 2;

        AtlasExportRequest request = getIncrementalRequest(0);
        InputStream inputStream = runExportWithParameters(exportService, request);

        ZipSource source = getZipSourceFromInputStream(inputStream);
        AtlasEntity.AtlasEntityWithExtInfo entities = getEntities(source, expectedEntityCount);

        int count = 0;
        for (Map.Entry<String, AtlasEntity> entry : entities.getReferredEntities().entrySet()) {
            assertNotNull(entry.getValue());
            count++;
        }

        nextTimestamp = updateTimesampForNextIncrementalExport(source);
        assertEquals(count, expectedEntityCount);
    }

    private long updateTimesampForNextIncrementalExport(ZipSource source) throws AtlasBaseException {
        return source.getExportResult().getChangeMarker();
    }

    @Test(dependsOnMethods = "atT0_ReturnsAllEntities")
    public void atT1_NewClassificationAttachedToTable_ReturnsChangedTable() throws AtlasBaseException, IOException {
        final int expectedEntityCount = 1;

        entityStore.addClassifications(TABLE_GUID, ImmutableList.of(classificationTypeT1.createDefaultValue()));

        AtlasExportRequest request = getIncrementalRequest(nextTimestamp);
        InputStream inputStream = runExportWithParameters(exportService, request);

        ZipSource source = getZipSourceFromInputStream(inputStream);
        AtlasEntity.AtlasEntityWithExtInfo entities = getEntities(source, expectedEntityCount);

        AtlasEntity entity = null;
        for (Map.Entry<String, AtlasEntity> entry : entities.getReferredEntities().entrySet()) {
            entity = entry.getValue();
            assertNotNull(entity);
            break;
        }

        nextTimestamp = updateTimesampForNextIncrementalExport(source);
        assertEquals(entity.getGuid(),TABLE_GUID);
    }

    private AtlasClassificationType createNewClassification() {
        createTypes(typeDefStore, ENTITIES_SUB_DIR,"typesdef-new-classification");
        return typeRegistry.getClassificationTypeByName("T1");
    }

    @Test(dependsOnMethods = "atT1_NewClassificationAttachedToTable_ReturnsChangedTable")
    public void atT2_NewClassificationAttachedToColumn_ReturnsChangedColumn() throws AtlasBaseException, IOException {
        final int expectedEntityCount = 1;

        AtlasEntity.AtlasEntityWithExtInfo tableEntity = entityStore.getById(TABLE_GUID);
        long preExportTableEntityTimestamp = tableEntity.getEntity().getUpdateTime().getTime();

        entityStore.addClassifications(COLUMN_GUID_HIGH, ImmutableList.of(typeRegistry.getClassificationTypeByName("T1").createDefaultValue()));

        InputStream inputStream = runExportWithParameters(exportService, getIncrementalRequest(nextTimestamp));

        ZipSource source = getZipSourceFromInputStream(inputStream);
        AtlasEntity.AtlasEntityWithExtInfo entities = getEntities(source, expectedEntityCount);

        for (Map.Entry<String, AtlasEntity> entry : entities.getReferredEntities().entrySet()) {
            AtlasEntity entity = entry.getValue();
            assertNotNull(entity.getGuid());
            break;
        }

        long postUpdateTableEntityTimestamp = tableEntity.getEntity().getUpdateTime().getTime();
        assertEquals(preExportTableEntityTimestamp, postUpdateTableEntityTimestamp);
    }

    private ZipSource getZipSourceFromInputStream(InputStream inputStream) {
        try {
            return new ZipSource(inputStream);
        } catch (IOException | AtlasBaseException e) {
            return null;
        }
    }

    @Test(dependsOnMethods = "atT2_NewClassificationAttachedToColumn_ReturnsChangedColumn")
    public void exportingWithSameParameters_Succeeds() {
        InputStream inputStream = runExportWithParameters(exportService, getIncrementalRequest(nextTimestamp));

        assertNotNull(getZipSourceFromInputStream(inputStream));
    }

    @Test
    public void connectedExport() {
        InputStream inputStream = runExportWithParameters(exportService, getConnected());

        ZipSource source = getZipSourceFromInputStream(inputStream);
        UniqueList<String> creationOrder = new UniqueList<>();
        List<String> zipCreationOrder = source.getCreationOrder();
        creationOrder.addAll(zipCreationOrder);
        assertNotNull(source);
        assertEquals(creationOrder.size(), zipCreationOrder.size());
    }

    @DataProvider(name = "hiveDb")
    public static Object[][] getData(ITestContext context) throws IOException, AtlasBaseException {
        return getZipSource("hive_db_lineage.zip");
    }

    @Test(dataProvider = "hiveDb")
    public void importHiveDb(InputStream stream) throws AtlasBaseException, IOException {
        runImportWithNoParameters(importService, stream);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableInrementalConnected() throws AtlasBaseException, IOException {
        InputStream source = runExportWithParameters(exportService, getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_LINEAGE, EXPORT_INCREMENTAL, 0, true));

        ZipSource sourceCopy = getZipSourceCopy(source);
        verifyExpectedEntities(getFileNames(sourceCopy), GUID_DB, GUID_TABLE_CTAS_2);

        nextTimestamp = updateTimesampForNextIncrementalExport(sourceCopy);

        try {
            source = runExportWithParameters(exportService, getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_LINEAGE, EXPORT_INCREMENTAL, nextTimestamp, true));
        } catch (SkipException e) {
            throw e;
        }

        entityStore.addClassifications(GUID_TABLE_CTAS_2, ImmutableList.of(classificationTypeT1.createDefaultValue()));

        source = runExportWithParameters(exportService, getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_LINEAGE, EXPORT_INCREMENTAL, nextTimestamp, true));
        verifyExpectedEntities(getFileNames(getZipSourceCopy(source)), GUID_TABLE_CTAS_2);
    }


    private AtlasExportRequest getIncrementalRequest(long timestamp) {
        try {
            AtlasExportRequest request = TestResourceFileUtils.readObjectFromJson(ENTITIES_SUB_DIR, EXPORT_REQUEST_INCREMENTAL, AtlasExportRequest.class);
            request.getOptions().put(FETCH_TYPE_INCREMENTAL_CHANGE_MARKER, timestamp);

            return request;
        } catch (IOException e) {
            throw new SkipException(String.format("getIncrementalRequest: '%s' could not be loaded.", EXPORT_REQUEST_INCREMENTAL));
        }
    }

    private AtlasExportRequest getConnected() {
        try {
            return TestResourceFileUtils.readObjectFromJson(ENTITIES_SUB_DIR, EXPORT_REQUEST_CONNECTED, AtlasExportRequest.class);
        } catch (IOException e) {
            throw new SkipException(String.format("getIncrementalRequest: '%s' could not be loaded.", EXPORT_REQUEST_CONNECTED));
        }
    }

    private AtlasExportRequest getExportRequestForHiveTable(String name, String fetchType, long changeMarker, boolean skipLineage) {
        AtlasExportRequest request = new AtlasExportRequest();

        List<AtlasObjectId> itemsToExport = new ArrayList<>();
        itemsToExport.add(new AtlasObjectId("hive_table", "qualifiedName", name));
        request.setItemsToExport(itemsToExport);
        request.setOptions(getOptionsMap(fetchType, changeMarker, skipLineage));

        return request;
    }

    private Map<String, Object> getOptionsMap(String fetchType, long changeMarker, boolean skipLineage){
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("fetchType", fetchType.isEmpty() ? "full" : fetchType );
        optionsMap.put( "changeMarker", changeMarker);
        optionsMap.put("skipLineage", skipLineage);

        return optionsMap;
    }

    private void verifyExpectedEntities(List<String> fileNames, String... guids){
        assertEquals(fileNames.size(), guids.length);
        for (String guid : guids) {
            assertTrue(fileNames.contains(guid.toLowerCase()));
        }
    }

    private List<String> getFileNames(ZipSource zipSource){
        List<String> ret = new ArrayList<>();
        assertTrue(zipSource.hasNext());

        while (zipSource.hasNext()){
            AtlasEntity atlasEntity = zipSource.next();
            assertNotNull(atlasEntity);
            ret.add(atlasEntity.getGuid());
        }
        return ret;
    }

    private ZipSource getZipSourceCopy(InputStream is) throws IOException, AtlasBaseException {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copy(is, baos);

        return new ZipSource(new ByteArrayInputStream(baos.toByteArray()));
    }
}
