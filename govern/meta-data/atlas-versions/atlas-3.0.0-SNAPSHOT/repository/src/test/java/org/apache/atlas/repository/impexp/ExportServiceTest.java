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
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStoreV2;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public class ExportServiceTest extends AtlasTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(ExportServiceTest.class);

    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    private AtlasTypeDefStore typeDefStore;

    @Inject
    ExportService exportService;

    @Inject
    private ExportImportAuditService auditService;

    @Inject
    private AtlasEntityStoreV2 entityStore;

    @BeforeTest
    public void setupTest() throws IOException, AtlasBaseException {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
        basicSetup(typeDefStore, typeRegistry);
    }

    @BeforeClass
    public void setupSampleData() throws Exception {
        super.initialize();

        AtlasTypesDef sampleTypes = TestUtilsV2.defineDeptEmployeeTypes();
        AtlasTypesDef typesToCreate = AtlasTypeDefStoreInitializer.getTypesToCreate(sampleTypes, typeRegistry);

        if (!typesToCreate.isEmpty()) {
            typeDefStore.createTypesDef(typesToCreate);
        }

        AtlasEntity.AtlasEntitiesWithExtInfo  deptEg2 = TestUtilsV2.createDeptEg2();
        AtlasEntityStream entityStream = new AtlasEntityStream(deptEg2);
        EntityMutationResponse emr = entityStore.createOrUpdate(entityStream, false);
        assertNotNull(emr);
        assertNotNull(emr.getCreatedEntities());
        assertTrue(emr.getCreatedEntities().size() > 0);
    }

    @AfterClass
    public void clear() throws Exception {
        Thread.sleep(1000);
        assertExportImportAuditEntry(auditService);
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    private AtlasExportRequest getRequestForFullFetch() {
        AtlasExportRequest request = new AtlasExportRequest();

        List<AtlasObjectId> itemsToExport = new ArrayList<>();
        itemsToExport.add(new AtlasObjectId("hive_db", "qualifiedName", "default@cl1"));
        request.setItemsToExport(itemsToExport);

        return request;
    }

    private AtlasExportRequest getRequestForDept(boolean addFetchType, String fetchTypeValue, boolean addMatchType, String matchTypeValue) {
        AtlasExportRequest request = new AtlasExportRequest();

        List<AtlasObjectId> itemsToExport = new ArrayList<>();
        itemsToExport.add(new AtlasObjectId("Department", "name", "hr"));
        request.setItemsToExport(itemsToExport);

        setOptionsMap(request, addFetchType, fetchTypeValue, addMatchType, matchTypeValue);
        return request;
    }

    private AtlasExportRequest getRequestForEmployee() {
        AtlasExportRequest request = new AtlasExportRequest();

        List<AtlasObjectId> itemsToExport = new ArrayList<>();
        itemsToExport.add(new AtlasObjectId("Employee", "name", "Max"));
        request.setItemsToExport(itemsToExport);

        setOptionsMap(request, true, "CONNECTED", false, "");
        return request;
    }

    private void setOptionsMap(AtlasExportRequest request,
                               boolean addFetchType, String fetchTypeValue, boolean addMatchType, String matchTypeValue) {
        Map<String, Object> optionsMap = null;
        if(addFetchType) {
            if(optionsMap == null) {
                optionsMap = new HashMap<>();
            }

            optionsMap.put("fetchType", fetchTypeValue);
            request.setOptions(optionsMap);
        }

        if(addMatchType) {
            if(optionsMap == null) {
                optionsMap = new HashMap<>();
            }

            optionsMap.put("matchType", matchTypeValue);
        }

        if(optionsMap != null) {
            request.setOptions(optionsMap);
        }
    }

    private ZipSource runExportWithParameters(AtlasExportRequest request) throws AtlasBaseException, IOException {
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

    @Test
    public void exportType() throws AtlasBaseException {
        String requestingIP = "1.0.0.0";
        String hostName = "root";

        AtlasExportRequest request = getRequestForFullFetch();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipSink zipSink = new ZipSink(baos);
        AtlasExportResult result = exportService.run(zipSink, request, "admin", hostName, requestingIP);

        assertNotNull(exportService);
        assertEquals(result.getHostName(), hostName);
        assertEquals(result.getClientIpAddress(), requestingIP);
        assertEquals(request, result.getRequest());
        assertNotNull(result.getSourceClusterName());
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void requestingEntityNotFound_NoData() throws AtlasBaseException, IOException {
        String requestingIP = "1.0.0.0";
        String hostName = "root";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipSink zipSink = new ZipSink(baos);
        AtlasExportResult result = exportService.run(
                zipSink, getRequestForFullFetch(), "admin", hostName, requestingIP);

        Assert.assertNull(result.getData());

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        new ZipSource(bais);
    }

    @Test
    public void requestingEntityFoundDefaultFetch_ContainsData() throws Exception {
        ZipSource source = runExportWithParameters(
                getRequestForDept(false, "", false, ""));
        verifyExportForHrData(source);
    }

    @Test
    public void requestingHrEntityWithMatchTypeContains_ContainsData() throws Exception {
        ZipSource source = runExportWithParameters(
                getRequestForDept(false, "", true, "CONTAINS"));
        verifyExportForHrData(source);
    }

    @Test
    public void requestingHrEntityWithMatchTypeEndsWith_ContainsData() throws Exception {
        ZipSource source = runExportWithParameters(
                getRequestForDept(false, "", true, "ENDSWITH"));
        verifyExportForHrData(source);
    }

    @Test
    public void requestingDeptEntityFoundFullFetch_ContainsData() throws Exception {
        ZipSource source = runExportWithParameters(
                getRequestForDept(true, "FULL", false, ""));
        verifyExportForHrData(source);
    }

    @Test
    public void requestingDeptEntityFoundConnectedFetch_ContainsData() throws Exception {
        ZipSource source = runExportWithParameters(
                getRequestForDept(true, "CONNECTED", false, ""));
        verifyExportForHrDataForConnected(source);
    }

    @Test
    public void requestingEmployeeEntityFoundConnectedFetch_ContainsData() throws Exception {
        ZipSource zipSource = runExportWithParameters(getRequestForEmployee());
        verifyExportForEmployeeData(zipSource);
    }

    @Test
    public void verifyOverallStatus() {
        assertEquals(AtlasExportResult.OperationStatus.FAIL, exportService.getOverallOperationStatus());

        assertEquals(AtlasExportResult.OperationStatus.SUCCESS, exportService.getOverallOperationStatus(AtlasExportResult.OperationStatus.SUCCESS));

        assertEquals(AtlasExportResult.OperationStatus.SUCCESS, exportService.getOverallOperationStatus(
                                AtlasExportResult.OperationStatus.SUCCESS,
                                AtlasExportResult.OperationStatus.SUCCESS,
                                AtlasExportResult.OperationStatus.SUCCESS));

        assertEquals(AtlasExportResult.OperationStatus.PARTIAL_SUCCESS, exportService.getOverallOperationStatus(
                AtlasExportResult.OperationStatus.FAIL,
                AtlasExportResult.OperationStatus.PARTIAL_SUCCESS,
                AtlasExportResult.OperationStatus.SUCCESS));

        assertEquals(AtlasExportResult.OperationStatus.PARTIAL_SUCCESS, exportService.getOverallOperationStatus(
                AtlasExportResult.OperationStatus.FAIL,
                AtlasExportResult.OperationStatus.FAIL,
                AtlasExportResult.OperationStatus.PARTIAL_SUCCESS));

        assertEquals(AtlasExportResult.OperationStatus.FAIL, exportService.getOverallOperationStatus(
                AtlasExportResult.OperationStatus.FAIL,
                AtlasExportResult.OperationStatus.FAIL,
                AtlasExportResult.OperationStatus.FAIL));
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void requestingExportOfNonExistentEntity_ReturnsFailure() throws Exception {
        AtlasExportRequest request = getRequestForEmployee();
        tamperEmployeeRequest(request);
        runExportWithParameters(request);
    }

    @Test
    public void requestForTypeFull() {
        AtlasExportRequest req = getRequestForTypeFull("Department,Employee");

        assertNotNull(req);
        assertEquals(req.getItemsToExport().size(), 1);
        assertEquals(req.getOptions().get(AtlasExportRequest.OPTION_ATTR_MATCH_TYPE), "forType");
    }

    @Test
    public void verifyTypeFull() throws AtlasBaseException, IOException {
        ZipSource zipSource = runExportWithParameters(getRequestForTypeFull("Department,Employee,Manager"));
        verifyExportForFullEmployeeData(zipSource);
    }

    private AtlasExportRequest getRequestForTypeFull(String type) {
        String jsonRequest = "{ \"itemsToExport\": [ { \"typeName\": \"%s\" } ], \"options\": {  \"fetchType\": \"FULL\", \"matchType\": \"forType\"} }";
        return AtlasType.fromJson(String.format(jsonRequest, type), AtlasExportRequest.class);
    }

    private void tamperEmployeeRequest(AtlasExportRequest request) {
        AtlasObjectId objectId = request.getItemsToExport().get(0);
        objectId.getUniqueAttributes().remove("name");
        objectId.getUniqueAttributes().put("qualifiedName", "XXX@121");
    }

    private void verifyExportForEmployeeData(ZipSource zipSource) throws AtlasBaseException {
        final List<String> expectedEntityTypes = Arrays.asList(new String[]{"Manager", "Employee", "Department"});

        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 2);
        assertTrue(zipSource.hasNext());

        while (zipSource.hasNext()) {
            AtlasEntity entity = zipSource.next();

            assertNotNull(entity);
            assertEquals(AtlasEntity.Status.ACTIVE, entity.getStatus());
            assertTrue(expectedEntityTypes.contains(entity.getTypeName()));
        }

        verifyTypeDefs(zipSource);
    }

    private void verifyExportForFullEmployeeData(ZipSource zipSource) throws AtlasBaseException {
        final List<String> expectedEntityTypes = Arrays.asList(new String[]{"Manager", "Employee", "Department"});

        assertNotNull(zipSource.getCreationOrder());
        assertTrue(zipSource.hasNext());

        while (zipSource.hasNext()) {
            AtlasEntity entity = zipSource.next();

            assertNotNull(entity);
            assertEquals(AtlasEntity.Status.ACTIVE, entity.getStatus());
            assertTrue(expectedEntityTypes.contains(entity.getTypeName()));
        }

        verifyTypeDefs(zipSource);
    }

    private void verifyExportForHrData(ZipSource zipSource) throws IOException, AtlasBaseException {
        assertNotNull(zipSource.getCreationOrder());
        assertTrue(zipSource.getCreationOrder().size() == 1);
        assertTrue(zipSource.hasNext());

        AtlasEntity entity = zipSource.next();

        assertNotNull(entity);
        assertTrue(entity.getTypeName().equals("Department"));
        assertEquals(entity.getStatus(), AtlasEntity.Status.ACTIVE);
        verifyTypeDefs(zipSource);
    }

    private void verifyExportForHrDataForConnected(ZipSource zipSource) throws IOException, AtlasBaseException {
        assertNotNull(zipSource.getCreationOrder());
        assertEquals(zipSource.getCreationOrder().size(), 1);
        assertTrue(zipSource.hasNext());

        AtlasEntity entity = zipSource.next();

        assertNotNull(entity);
        assertTrue(entity.getTypeName().equals("Department"));
        assertEquals(entity.getStatus(), AtlasEntity.Status.ACTIVE);
        verifyTypeDefs(zipSource);
    }

    private void verifyTypeDefs(ZipSource zipSource) throws AtlasBaseException {
        assertEquals(zipSource.getTypesDef().getEnumDefs().size(), 1);
        assertEquals(zipSource.getTypesDef().getClassificationDefs().size(), 0);
        assertEquals(zipSource.getTypesDef().getStructDefs().size(), 1);
        assertEquals(zipSource.getTypesDef().getEntityDefs().size(), 4);
    }
}
