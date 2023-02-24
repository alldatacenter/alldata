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
import org.apache.atlas.model.impexp.AtlasExportResult;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.ITestContext;
import org.testng.annotations.Test;
import org.testng.annotations.Guice;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;

import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.getZipSource;
import static org.apache.atlas.utils.TestLoadModelUtils.loadModelFromJson;
import static org.apache.atlas.repository.impexp.ZipFileResourceTestUtils.runImportWithNoParameters;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public class RelationshipAttributesExtractorTest extends AtlasTestBase {

    private static final String EXPORT_FULL      = "full";
    private static final String EXPORT_CONNECTED = "connected";
    private static final String ENTITIES_SUB_DIR = "entities";

    private static final String QUALIFIED_NAME_DB                = "db_test_1@02052019";
    private static final String QUALIFIED_NAME_TABLE_LINEAGE     = "db_test_1.test_tbl_ctas_2@02052019";
    private static final String QUALIFIED_NAME_TABLE_NON_LINEAGE = "db_test_1.test_tbl_1@02052019";

    private static final String GUID_DB           = "f0b72ab4-7452-4e42-ac74-2aee7728cce4";
    private static final String GUID_TABLE_1      = "4d5adf00-2c9b-4877-ad23-c41fd7319150";
    private static final String GUID_TABLE_2      = "8d0b834c-61ce-42d8-8f66-6fa51c36bccb";
    private static final String GUID_TABLE_CTAS_2 = "eaec545b-3ac7-4e1b-a497-bd4a2b6434a2";
    private static final String GUID_HIVE_PROCESS = "bd3138b2-f29e-4226-b859-de25eaa1c18b";

    private static final String DB1                 = "db1";
    private static final String DB2                 = "db2";
    private static final String TBL1                = "table1";
    private static final String TBL2                = "table2";
    private static final String HIVE_PROCESS        = "table-lineage";
    private static final String HIVE_COLUMN_LINEAGE = "column-lineage";

    private static final String GUID_DB1            = "1c4e939e-ff6b-4229-92a4-b60c00deb547";
    private static final String GUID_DB2            = "77c3bccf-ca3f-42e7-b2dd-f5a35f63eea6";
    private static final String GUID_TBL1           = "3f6c02be-61e8-4dae-a7b8-cc37f289ce6e";
    private static final String GUID_TBL2           = "b8cbc39f-4467-429b-a7fe-4ba2c28cceca";
    private static final String GUID_PROCESS        = "caf7f40a-b334-4f9e-9bf2-f24ce43db47f";
    private static final String GUID_COLUMN_LINEAGE = "d4cf482b-423c-4c88-9bd1-701477ed6fd8";

    @Inject
    private ImportService importService;

    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    private ExportService exportService;

    @Inject
    private AtlasEntityStoreV2 entityStore;

    @BeforeClass
    public void setup() throws Exception {
        super.initialize();

        loadBaseModel();
        loadHiveModel();
    }

    @BeforeTest
    public void setupTest() {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @DataProvider(name = "hiveDb")
    public static Object[][] getData(ITestContext context) throws IOException, AtlasBaseException {
        return getZipSource("hive_db_lineage.zip");
    }

    @Test(dataProvider = "hiveDb")
    public void importHiveDb(InputStream inputStream) throws AtlasBaseException, IOException {
        runImportWithNoParameters(importService, inputStream);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportDBFull() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveDb(QUALIFIED_NAME_DB, EXPORT_FULL, false));
        verifyDBFull(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportDBFullSkipLineageFull() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveDb(QUALIFIED_NAME_DB, EXPORT_FULL, true));
        verifyDBFullSkipLineageFull(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableWithLineageFull() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_LINEAGE, EXPORT_FULL, false));
        verifyTableWithLineageFull(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableWithLineageSkipLineageFull() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_LINEAGE, EXPORT_FULL, true));
        verifyTableWithLineageSkipLineageFull(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableWithoutLineageFull() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_NON_LINEAGE, EXPORT_FULL, false));
        verifyTableWithoutLineageFull(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableWithoutLineageSkipLineageFull() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_NON_LINEAGE, EXPORT_FULL, true));
        verifyTableWithoutLineageSkipLineageFull(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportDBConn() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveDb(QUALIFIED_NAME_DB, EXPORT_CONNECTED, false));
        verifyDBConn(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportDBSkipLineageConn() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveDb(QUALIFIED_NAME_DB, EXPORT_CONNECTED, true));
        verifyDBSkipLineageConn(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableWithLineageConn() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_LINEAGE, EXPORT_CONNECTED, false));
        verifyTableWithLineageConn(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableWithLineageSkipLineageConn() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_LINEAGE, EXPORT_CONNECTED, true));
        verifyTableWithLineageSkipLineageConn(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableWithoutLineageConn() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_NON_LINEAGE, EXPORT_CONNECTED, false));
        verifyTableWithoutLineageConn(source);
    }

    @Test(dependsOnMethods = "importHiveDb")
    public void exportTableWithoutLineageSkipLineageConn() throws Exception {
        ZipSource source = runExport(getExportRequestForHiveTable(QUALIFIED_NAME_TABLE_NON_LINEAGE, EXPORT_CONNECTED, true));
        verifyTableWithoutLineageSkipLineageConn(source);
    }

    @Test
    public void interDbLineageConnectedExportTest() throws Exception {
        setupInterDbLineageData();

        ZipSource source = runExport(getExportRequestForHiveTable("db_1.table_1@cl1", EXPORT_CONNECTED, false));
        assertInterDbLineageConnectedExport(source);
    }

    private void setupInterDbLineageData() {
        RequestContext.get().setImportInProgress(true);
        createEntities(entityStore, ENTITIES_SUB_DIR, new String[]{DB1, DB2, TBL1, TBL2, HIVE_PROCESS, HIVE_COLUMN_LINEAGE});
        final String[] entityGuids = {GUID_DB1, GUID_DB2, GUID_TBL1, GUID_TBL2, GUID_PROCESS, GUID_COLUMN_LINEAGE};
        verifyCreatedEntities(entityStore, entityGuids, 6);
        RequestContext.get().setImportInProgress(false);
    }

    private void loadHiveModel() throws IOException, AtlasBaseException {
        loadModelFromJson("1000-Hadoop/1030-hive_model.json", typeDefStore, typeRegistry);
    }

    private void loadBaseModel() throws IOException, AtlasBaseException {
        loadModelFromJson("0000-Area0/0010-base_model.json", typeDefStore, typeRegistry);
    }

    private AtlasExportRequest getExportRequestForHiveDb(String hiveDbName, String fetchType, boolean skipLineage) {
        AtlasExportRequest request = new AtlasExportRequest();

        List<AtlasObjectId> itemsToExport = new ArrayList<>();
        itemsToExport.add(new AtlasObjectId("hive_db", "qualifiedName", hiveDbName));
        request.setItemsToExport(itemsToExport);
        request.setOptions(getOptionsMap(fetchType, skipLineage));

        return request;
    }

    private AtlasExportRequest getExportRequestForHiveTable(String hiveTableName, String fetchType, boolean skipLineage) {
        AtlasExportRequest request = new AtlasExportRequest();

        List<AtlasObjectId> itemsToExport = new ArrayList<>();
        itemsToExport.add(new AtlasObjectId("hive_table", "qualifiedName", hiveTableName));
        request.setItemsToExport(itemsToExport);
        request.setOptions(getOptionsMap(fetchType, skipLineage));

        return request;
    }

    private Map<String, Object> getOptionsMap(String fetchType, boolean skipLineage){
        Map<String, Object> optionsMap = new HashMap<>();
        optionsMap.put("fetchType", fetchType.isEmpty() ? "full" : fetchType );
        optionsMap.put("skipLineage", skipLineage);

        return optionsMap;
    }

    private ZipSource runExport(AtlasExportRequest request) throws AtlasBaseException, IOException {
        final String requestingIP = "1.0.0.0";
        final String hostName = "localhost";
        final String userName = "admin";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipSink zipSink = new ZipSink(baos);
        AtlasExportResult result = exportService.run(zipSink, request, userName, hostName, requestingIP);

        zipSink.close();

        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
        ZipSource zipSource = new ZipSource(bis);
        return zipSource;
    }

    private void verifyDBFull(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 5);

        assertTrue(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1, GUID_TABLE_2, GUID_TABLE_CTAS_2, GUID_HIVE_PROCESS);
    }

    private void verifyDBFullSkipLineageFull(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 4);

        assertFalse(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1, GUID_TABLE_2, GUID_TABLE_CTAS_2);
    }

    private void verifyTableWithLineageFull(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 5);

        assertTrue(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1, GUID_TABLE_2, GUID_TABLE_CTAS_2, GUID_HIVE_PROCESS);
    }

    private void verifyTableWithLineageSkipLineageFull(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 4);

        assertFalse(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1, GUID_TABLE_2, GUID_TABLE_CTAS_2);
    }

    private void verifyTableWithoutLineageFull(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 5);

        assertTrue(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1, GUID_TABLE_2, GUID_TABLE_CTAS_2,GUID_HIVE_PROCESS);
    }

    private void verifyTableWithoutLineageSkipLineageFull(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 4);

        assertFalse(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1, GUID_TABLE_2, GUID_TABLE_CTAS_2);
    }


    private void verifyDBConn(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 5);

        assertTrue(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1, GUID_TABLE_2, GUID_TABLE_CTAS_2, GUID_HIVE_PROCESS);
    }

    private void verifyDBSkipLineageConn(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 4);

        assertFalse(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1, GUID_TABLE_2, GUID_TABLE_CTAS_2);
    }

    private void verifyTableWithLineageConn(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 4);

        assertTrue(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_2, GUID_TABLE_CTAS_2, GUID_HIVE_PROCESS);
    }

    private void verifyTableWithLineageSkipLineageConn(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(),2);

        assertFalse(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_CTAS_2);;
    }

    private void verifyTableWithoutLineageConn(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 2);

        assertFalse(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1);
    }

    private void verifyTableWithoutLineageSkipLineageConn(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 2);;

        assertFalse(zipSource.getCreationOrder().contains(GUID_HIVE_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB, GUID_TABLE_1);
    }

    private void assertInterDbLineageConnectedExport(ZipSource zipSource) {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 5);

        assertTrue(zipSource.getCreationOrder().contains(GUID_PROCESS));
        verifyExpectedEntities(getFileNames(zipSource), GUID_DB1, GUID_DB2, GUID_TBL1, GUID_TBL2, GUID_PROCESS);
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
}
