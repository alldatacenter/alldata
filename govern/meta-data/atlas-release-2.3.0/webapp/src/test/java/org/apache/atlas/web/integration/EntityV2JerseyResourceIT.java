/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.bulkimport.BulkImportResponse;
import org.apache.atlas.model.TimeBoundary;
import org.apache.atlas.model.audit.AtlasAuditEntry;
import org.apache.atlas.model.audit.AuditSearchParameters;
import org.apache.atlas.model.audit.EntityAuditEventV2;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasClassification.AtlasClassifications;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.ClassificationAssociateRequest;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.hadoop.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Integration tests for Entity Jersey Resource.
 */
public class EntityV2JerseyResourceIT extends BaseResourceIT {

    private static final Logger LOG = LoggerFactory.getLogger(EntityV2JerseyResourceIT.class);

    private static final String ENTITY_NOTIFICATION_VERSION_PROPERTY = "atlas.notification.entity.version";

    private final String DATABASE_NAME = "db" + randomString();
    private final String TABLE_NAME = "table" + randomString();
    private String traitName;
    private String createdDBName;
    private String createdTableQualifiedName;

    private AtlasEntity dbEntity;
    private AtlasEntity tableEntity;
    private AtlasClassificationDef piiTrait;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        createTypeDefinitionsV2();

    }

    @Test
    public void testSubmitEntity() throws Exception {
        TypesUtil.Pair dbAndTable = createDBAndTable();
        assertNotNull(dbAndTable);
        assertNotNull(dbAndTable.left);
        assertNotNull(dbAndTable.right);
        // Writing created table data to a file for import test.
        createImportFile();
    }

    @Test
    public void testCreateNestedEntities() throws Exception {
        AtlasEntity.AtlasEntitiesWithExtInfo entities = new AtlasEntity.AtlasEntitiesWithExtInfo();

        AtlasEntity databaseInstance = new AtlasEntity(DATABASE_TYPE_V2, "name", "db1");
        databaseInstance.setAttribute("name", "db1");
        databaseInstance.setAttribute("description", "foo database");
        databaseInstance.setAttribute("owner", "user1");
        databaseInstance.setAttribute("locationUri", "/tmp");
        databaseInstance.setAttribute("createTime",1000);
        entities.addEntity(databaseInstance);

        int nTables = 5;
        int colsPerTable=3;

        for(int i = 0; i < nTables; i++) {
            String tableName = "db1-table-" + i;

            AtlasEntity tableInstance = new AtlasEntity(HIVE_TABLE_TYPE_V2, "name", tableName);
            tableInstance.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, tableName);
            tableInstance.setAttribute("db", AtlasTypeUtil.getAtlasObjectId(databaseInstance));
            tableInstance.setAttribute("description", tableName + " table");
            entities.addEntity(tableInstance);

            List<AtlasObjectId> columns = new ArrayList<>();
            for(int j = 0; j < colsPerTable; j++) {
                AtlasEntity columnInstance = new AtlasEntity(COLUMN_TYPE_V2);
                columnInstance.setAttribute("name", tableName + "-col-" + j);
                columnInstance.setAttribute("dataType", "String");
                columnInstance.setAttribute("comment", "column " + j + " for table " + i);

                columns.add(AtlasTypeUtil.getAtlasObjectId(columnInstance));

                entities.addReferredEntity(columnInstance);
            }
            tableInstance.setAttribute("columns", columns);
        }

        //Create the tables.  The database and columns should be created automatically, since
        //the tables reference them.

        EntityMutationResponse response = atlasClientV2.createEntities(entities);
        Assert.assertNotNull(response);

        Map<String,String> guidsCreated = response.getGuidAssignments();
        assertEquals(guidsCreated.size(), nTables * colsPerTable + nTables + 1);
        assertNotNull(guidsCreated.get(databaseInstance.getGuid()));

        for(AtlasEntity r : entities.getEntities()) {
            assertNotNull(guidsCreated.get(r.getGuid()));
        }

        for(AtlasEntity r : entities.getReferredEntities().values()) {
            assertNotNull(guidsCreated.get(r.getGuid()));
        }
    }

    @Test
    public void testRequestUser() throws Exception {
        AtlasEntity hiveDBInstanceV2 = createHiveDB(randomString());
        List<EntityAuditEvent> events = atlasClientV1.getEntityAuditEvents(hiveDBInstanceV2.getGuid(), (short) 10);
        assertEquals(events.size(), 1);
        assertEquals(events.get(0).getUser(), "admin");
    }

    @Test
    public void testEntityDeduping() throws Exception {
        ArrayNode results = searchByDSL(String.format("%s where name='%s'", DATABASE_TYPE_V2, DATABASE_NAME));
        assertEquals(results.size(), 1);

        final AtlasEntity hiveDBInstanceV2 = createHiveDB();

        results = searchByDSL(String.format("%s where name='%s'", DATABASE_TYPE_V2, DATABASE_NAME));
        assertEquals(results.size(), 1);

        //Test the same across references
        final String tableName = randomString();
        AtlasEntity hiveTableInstanceV2 = createHiveTableInstanceV2(hiveDBInstanceV2, tableName);
        hiveTableInstanceV2.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, tableName);

        EntityMutationResponse entity = atlasClientV2.createEntity(new AtlasEntityWithExtInfo(hiveTableInstanceV2));
        assertNotNull(entity);
        assertNotNull(entity.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE));
        results = searchByDSL(String.format("%s where name='%s'", DATABASE_TYPE_V2, DATABASE_NAME));
        assertEquals(results.size(), 1);
    }

    @Test
    public void testEntityDefinitionAcrossTypeUpdate() throws Exception {
        //create type
        AtlasEntityDef entityDef = AtlasTypeUtil
                .createClassTypeDef(randomString(),
                        Collections.<String>emptySet(),
                        AtlasTypeUtil.createUniqueRequiredAttrDef("name", "string")
                );
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getEntityDefs().add(entityDef);

        AtlasTypesDef created = atlasClientV2.createAtlasTypeDefs(typesDef);
        assertNotNull(created);
        assertNotNull(created.getEntityDefs());
        assertEquals(created.getEntityDefs().size(), 1);

        //create entity for the type
        AtlasEntity instance = new AtlasEntity(entityDef.getName());
        instance.setAttribute("name", randomString());
        EntityMutationResponse mutationResponse = atlasClientV2.createEntity(new AtlasEntityWithExtInfo(instance));
        assertNotNull(mutationResponse);
        assertNotNull(mutationResponse.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE));
        assertEquals(mutationResponse.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE).size(),1 );
        String guid = mutationResponse.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE).get(0).getGuid();

        //update type - add attribute
        entityDef = AtlasTypeUtil.createClassTypeDef(entityDef.getName(), Collections.<String>emptySet(),
                AtlasTypeUtil.createUniqueRequiredAttrDef("name", "string"),
                AtlasTypeUtil.createOptionalAttrDef("description", "string"));

        typesDef = new AtlasTypesDef();
        typesDef.getEntityDefs().add(entityDef);

        AtlasTypesDef updated = atlasClientV2.updateAtlasTypeDefs(typesDef);
        assertNotNull(updated);
        assertNotNull(updated.getEntityDefs());
        assertEquals(updated.getEntityDefs().size(), 1);

        //Get definition after type update - new attributes should be null
        AtlasEntity entityByGuid = getEntityByGuid(guid);
        assertNull(entityByGuid.getAttribute("description"));
        assertEquals(entityByGuid.getAttribute("name"), instance.getAttribute("name"));
    }

    @Test
    public void testEntityInvalidValue() throws Exception {
        AtlasEntity databaseInstance = new AtlasEntity(DATABASE_TYPE_V2);
        String dbName = randomString();
        String nullString = null;
        String emptyString = "";
        databaseInstance.setAttribute("name", dbName);
        databaseInstance.setAttribute("description", nullString);
        AtlasEntityHeader created = createEntity(databaseInstance);

        // null valid value for required attr - description
        assertNull(created);

        databaseInstance.setAttribute("description", emptyString);
        created = createEntity(databaseInstance);

        // empty string valid value for required attr
        assertNotNull(created);

        databaseInstance.setGuid(created.getGuid());
        databaseInstance.setAttribute("owner", nullString);
        databaseInstance.setAttribute("locationUri", emptyString);

        created = updateEntity(databaseInstance);

        // null/empty string valid value for optional attr
        assertNotNull(created);
    }

    @Test
    public void testGetEntityByAttribute() throws Exception {
        AtlasEntity hiveDB = createHiveDB();
        String qualifiedName = (String) hiveDB.getAttribute(NAME);
        //get entity by attribute

        AtlasEntity byAttribute = atlasClientV2.getEntityByAttribute(DATABASE_TYPE_V2, toMap(NAME, qualifiedName)).getEntity();
        assertEquals(byAttribute.getTypeName(), DATABASE_TYPE_V2);
        assertEquals(byAttribute.getAttribute(NAME), qualifiedName);
    }

    @Test
    public void testGetEntitiesByAttribute() throws Exception {
        AtlasEntity hiveDB1 = createHiveDB();
        AtlasEntity hiveDB2 = createHiveDB();

        String qualifiedNameDB1 = (String) hiveDB1.getAttribute(NAME);
        String qualifiedNameDB2 = (String) hiveDB2.getAttribute(NAME);
        List<Map<String, String>> list = new ArrayList<>();
        list.add(toMap(NAME, qualifiedNameDB1));
        list.add(toMap(NAME, qualifiedNameDB2));

        AtlasEntity.AtlasEntitiesWithExtInfo info = atlasClientV2.getEntitiesByAttribute(DATABASE_TYPE_V2, list);
        List<AtlasEntity> entityList = info.getEntities();
        assertEquals(entityList.size(), 2);
        assertEquals(entityList.get(0).getTypeName(), DATABASE_TYPE_V2);
        assertEquals(entityList.get(1).getTypeName(), DATABASE_TYPE_V2);
    }

    @Test
    public void testSubmitEntityWithBadDateFormat() throws Exception {
        AtlasEntity       hiveDBEntity = createHiveDBInstanceV2("db" + randomString());
        AtlasEntityHeader hiveDBHeader = createEntity(hiveDBEntity);
        hiveDBEntity.setGuid(hiveDBHeader.getGuid());

        AtlasEntity tableInstance = createHiveTableInstanceV2(hiveDBEntity, "table" + randomString());
        //Dates with an invalid format are simply nulled out.  This does not produce
        //an error.  See AtlasBuiltInTypes.AtlasDateType.getNormalizedValue().
        tableInstance.setAttribute("lastAccessTime", 1107201407);
        AtlasEntityHeader tableEntityHeader = createEntity(tableInstance);
        assertNotNull(tableEntityHeader);
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testAddProperty() throws Exception {
        //add property
        String description = "bar table - new desc";
        addProperty(createHiveTable().getGuid(), "description", description);

        AtlasEntity entityByGuid = getEntityByGuid(createHiveTable().getGuid());
        Assert.assertNotNull(entityByGuid);

        entityByGuid.setAttribute("description", description);

        // TODO: This behavior should've been consistent across APIs
//        //invalid property for the type
//        try {
//            addProperty(table.getGuid(), "invalid_property", "bar table");
//            Assert.fail("Expected AtlasServiceException");
//        } catch (AtlasServiceException e) {
//            assertNotNull(e.getStatus());
//            assertEquals(e.getStatus(), ClientResponse.Status.BAD_REQUEST);
//        }

        //non-string property, update
        Object currentTime = new Date(System.currentTimeMillis());
        addProperty(createHiveTable().getGuid(), "createTime", currentTime);

        entityByGuid = getEntityByGuid(createHiveTable().getGuid());
        Assert.assertNotNull(entityByGuid);
    }

    @Test
    public void testAddNullPropertyValue() throws Exception {
        // FIXME: Behavior has changed between v1 and v2
        //add property
//        try {
            addProperty(createHiveTable().getGuid(), "description", null);
//            Assert.fail("Expected AtlasServiceException");
//        } catch(AtlasServiceException e) {
//            Assert.assertEquals(e.getStatus().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
//        }
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testGetInvalidEntityDefinition() throws Exception {
        getEntityByGuid("blah");
    }

    @Test(dependsOnMethods = "testSubmitEntity", enabled = false)
    public void testGetEntityList() throws Exception {
        // TODO: Can only be done when there's a search API exposed from entity REST
    }

    @Test(enabled = false)
    public void testGetEntityListForBadEntityType() throws Exception {
        // FIXME: Complete test when search interface is in place
    }

    @Test(enabled = false)
    public void testGetEntityListForNoInstances() throws Exception {
        // FIXME: Complete test when search interface is in place
        /*
        String typeName = "";

        ClientResponse clientResponse =
                service.path(ENTITIES).queryParam("type", typeName).accept(Servlets.JSON_MEDIA_TYPE)
                        .type(Servlets.JSON_MEDIA_TYPE).method(HttpMethod.GET, ClientResponse.class);
        Assert.assertEquals(clientResponse.getStatus(), Response.Status.OK.getStatusCode());

        String responseAsString = clientResponse.getEntity(String.class);
        Assert.assertNotNull(responseAsString);

        JSONObject response = new JSONObject(responseAsString);
        Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));

        final JSONArray list = response.getJSONArray(AtlasClient.RESULTS);
        Assert.assertEquals(list.length(), 0);
         */
    }

    private String addNewType() throws Exception {
        String typeName = "test" + randomString();
        AtlasEntityDef classTypeDef = AtlasTypeUtil
                .createClassTypeDef(typeName, Collections.<String>emptySet(),
                        AtlasTypeUtil.createRequiredAttrDef("name", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("description", "string"));
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getEntityDefs().add(classTypeDef);
        createType(typesDef);
        return typeName;
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testGetTraitNames() throws Exception {
        AtlasClassifications classifications = atlasClientV2.getClassifications(createHiveTable().getGuid());
        assertNotNull(classifications);
        assertTrue(classifications.getList().size() > 0);
        assertEquals(classifications.getList().size(), 9);
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testCommonAttributes() throws Exception{
        AtlasEntity entity = getEntityByGuid(createHiveTable().getGuid());
        Assert.assertNotNull(entity.getStatus());
        Assert.assertNotNull(entity.getVersion());
        Assert.assertNotNull(entity.getCreatedBy());
        Assert.assertNotNull(entity.getCreateTime());
        Assert.assertNotNull(entity.getUpdatedBy());
        Assert.assertNotNull(entity.getUpdateTime());
    }

    @Test(dependsOnMethods = "testGetTraitNames")
    public void testAddTrait() throws Exception {
        traitName = "PII_Trait" + randomString();
        AtlasClassificationDef piiTrait =
                AtlasTypeUtil.createTraitTypeDef(traitName, Collections.<String>emptySet());
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getClassificationDefs().add(piiTrait);
        createType(typesDef);

        atlasClientV2.addClassifications(createHiveTable().getGuid(), Collections.singletonList(new AtlasClassification(piiTrait.getName())));

        assertEntityAudit(createHiveTable().getGuid(), EntityAuditEvent.EntityAuditAction.TAG_ADD);
        AtlasClassifications classifications = atlasClientV2.getEntityClassifications(createHiveTable().getGuid(), piiTrait.getName());
        assertNotNull(classifications);
    }

    @Test(dependsOnMethods = "testAddTrait")
    public void testAddLabels() throws Exception {
        Set<String> set = new HashSet<>();
        set.add("lable");
        atlasClientV2.addLabels(createHiveTable().getGuid(), set);
        AtlasEntityWithExtInfo info = atlasClientV2.getEntityByGuid(createHiveTable().getGuid(), false, true);
        assertNotNull(info);
        assertNotNull(info.getEntity().getLabels());
        assertEquals(info.getEntity().getLabels().size(), 1);
    }

    @Test(dependsOnMethods = "testAddLabels")
    public void testSetLabels() throws Exception {
        Set<String> setNet = new HashSet<>();
        setNet.add("labelNext");
        atlasClientV2.setLabels(createHiveTable().getGuid(), setNet);
        AtlasEntityWithExtInfo infoForSet = atlasClientV2.getEntityByGuid(createHiveTable().getGuid(), false, true);
        assertNotNull(infoForSet);
        assertNotNull(infoForSet.getEntity().getLabels());
        assertEquals(infoForSet.getEntity().getLabels().size(), 1);
    }

    @Test(dependsOnMethods = "testSetLabels")
    public void testDeleteLabels() throws Exception {
        Set<String> set = new HashSet<>();
        set.add("testNext");
        atlasClientV2.removeLabels(createHiveTable().getGuid(), set);
        AtlasEntityWithExtInfo info = atlasClientV2.getEntityByGuid(createHiveTable().getGuid(), false, true);
        assertNotNull(info);
        assertNotNull(info.getEntity().getLabels());
        assertEquals(info.getEntity().getLabels().size(), 1);
    }

    @Test(dependsOnMethods = "testGetTraitNames")
    public void testAddTraitWithValidityPeriod() throws Exception {
        traitName = "PII_Trait" + randomString();

        AtlasClassificationDef piiTrait = AtlasTypeUtil.createTraitTypeDef(traitName, Collections.<String>emptySet());
        AtlasTypesDef          typesDef = new AtlasTypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait), Collections.emptyList());

        createType(typesDef);

        String              tableGuid      = createHiveTable().getGuid();
        AtlasClassification classification = new AtlasClassification(piiTrait.getName());
        TimeBoundary        validityPeriod = new TimeBoundary("2018/03/01 00:00:00", "2018/04/01 00:00:00", "GMT");

        classification.setEntityGuid(tableGuid);
        classification.addValityPeriod(validityPeriod);
        classification.setPropagate(true);
        classification.setRemovePropagationsOnEntityDelete(true);

        atlasClientV2.addClassifications(tableGuid, Collections.singletonList(classification));

        assertEntityAudit(tableGuid, EntityAuditEvent.EntityAuditAction.TAG_ADD);

        AtlasClassifications classifications = atlasClientV2.getClassifications(tableGuid);

        assertNotNull(classifications);
        assertNotNull(classifications.getList());
        assertTrue(classifications.getList().size() > 1);

        boolean foundClassification = false;
        for (AtlasClassification entityClassification : classifications.getList()) {
            if (StringUtils.equalsIgnoreCase(entityClassification.getTypeName(), piiTrait.getName())) {
                foundClassification = true;

                assertEquals(entityClassification.getTypeName(), piiTrait.getName());
                assertNotNull(entityClassification.getValidityPeriods());
                assertEquals(entityClassification.getValidityPeriods().size(), 1);
                assertEquals(entityClassification.getValidityPeriods().get(0), validityPeriod);
                assertEquals(entityClassification, classification);

                break;
            }
        }

        assertTrue(foundClassification, "classification '" + piiTrait.getName() + "' is missing for entity '" + tableGuid + "'");
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testGetTraitDefinitionForEntity() throws Exception{
        traitName = "PII_Trait" + randomString();
        AtlasClassificationDef piiTrait =
                AtlasTypeUtil.createTraitTypeDef(traitName, Collections.<String>emptySet());
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getClassificationDefs().add(piiTrait);
        createType(typesDef);

        AtlasClassificationDef classificationByName = atlasClientV2.getClassificationDefByName(traitName);
        assertNotNull(classificationByName);

        AtlasEntity hiveTable = createHiveTable();
        assertEquals(hiveTable.getClassifications().size(), 7);

        AtlasClassification piiClassification = new AtlasClassification(piiTrait.getName());

        atlasClientV2.addClassifications(hiveTable.getGuid(), Lists.newArrayList(piiClassification));

        AtlasClassifications classifications = atlasClientV2.getClassifications(hiveTable.getGuid());
        assertNotNull(classifications);
        assertTrue(classifications.getList().size() > 0);
        assertEquals(classifications.getList().size(), 9);
    }

    @Test(dependsOnMethods = "testGetTraitNames")
    public void testAddTraitWithAttribute() throws Exception {
        final String traitName = "PII_Trait" + randomString();
        AtlasClassificationDef piiTrait = AtlasTypeUtil
                .createTraitTypeDef(traitName, Collections.<String>emptySet(),
                        AtlasTypeUtil.createRequiredAttrDef("type", "string"));
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getClassificationDefs().add(piiTrait);
        createType(typesDef);

        AtlasClassification traitInstance = new AtlasClassification(traitName);
        traitInstance.setAttribute("type", "SSN");

        final String guid = createHiveTable().getGuid();
        atlasClientV2.addClassifications(guid, Collections.singletonList(traitInstance));

        // verify the response
        AtlasEntity withAssociationByGuid = atlasClientV2.getEntityByGuid(guid).getEntity();
        assertNotNull(withAssociationByGuid);
        assertFalse(withAssociationByGuid.getClassifications().isEmpty());

        boolean found = false;
        for (AtlasClassification atlasClassification : withAssociationByGuid.getClassifications()) {
            String attribute = (String)atlasClassification.getAttribute("type");
            if (attribute != null && attribute.equals("SSN")) {
                found = true;
                break;
            }
        }
        assertTrue(found);
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testAddTraitWithNoRegistration() throws Exception {
        final String traitName = "PII_Trait" + randomString();
        AtlasTypeUtil.createTraitTypeDef(traitName, Collections.<String>emptySet());

        AtlasClassification traitInstance = new AtlasClassification(traitName);

        atlasClientV2.addClassifications("random", Collections.singletonList(traitInstance));
    }

    @Test(dependsOnMethods = "testAddTrait")
    public void testDeleteTrait() throws Exception {
        final String guid = createHiveTable().getGuid();

        try {
            atlasClientV2.deleteClassification(guid, traitName);
        } catch (AtlasServiceException ex) {
            fail("Deletion should've succeeded");
        }
        assertEntityAudit(guid, EntityAuditEvent.EntityAuditAction.TAG_DELETE);
    }

    @Test
    public void testDeleteTraitNonExistent() throws Exception {
        final String traitName = "blah_trait";

        try {
            atlasClientV2.deleteClassification("random", traitName);
            fail("Deletion for bogus names shouldn't have succeeded");
        } catch (AtlasServiceException ex) {
            assertNotNull(ex.getStatus());
//            assertEquals(ex.getStatus(), ClientResponse.Status.NOT_FOUND);
            assertEquals(ex.getStatus(), ClientResponse.Status.BAD_REQUEST);
            // Should it be a 400 or 404
        }
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testDeleteExistentTraitNonExistentForEntity() throws Exception {

        final String guid = createHiveTable().getGuid();
        final String traitName = "PII_Trait" + randomString();
        AtlasClassificationDef piiTrait = AtlasTypeUtil
                .createTraitTypeDef(traitName, Collections.<String>emptySet(),
                        AtlasTypeUtil.createRequiredAttrDef("type", "string"));
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getClassificationDefs().add(piiTrait);
        createType(typesDef);

        try {
            atlasClientV2.deleteClassification(guid, traitName);
            fail("Deletion should've failed for non-existent trait association");
        } catch (AtlasServiceException ex) {
            Assert.assertNotNull(ex.getStatus());
            assertEquals(ex.getStatus(), ClientResponse.Status.BAD_REQUEST);
        }
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testGetEntityHeaderByGuid() throws Exception {
        AtlasEntityHeader header = atlasClientV2.getEntityHeaderByGuid(createHiveTable().getGuid());
        assertNotNull(header);
        assertEquals(header.getGuid(), createHiveTable().getGuid());
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testGetEntityHeaderByAttribute() throws Exception {
        AtlasEntity hiveDB = createHiveDB();
        String qualifiedName = (String) hiveDB.getAttribute(NAME);
        AtlasEntityHeader header = atlasClientV2.getEntityHeaderByAttribute(DATABASE_TYPE_V2, toMap(NAME, qualifiedName));
        assertNotNull(header);
        assertEquals(header.getTypeName(), DATABASE_TYPE_V2);
        assertEquals(header.getAttribute(NAME), qualifiedName);
    }

    @Test
    public void testUTF8() throws Exception {
        String classType = randomString();
        String attrName = randomUTF8();
        String attrValue = randomUTF8();

        AtlasEntityDef classTypeDef = AtlasTypeUtil
                .createClassTypeDef(classType, Collections.<String>emptySet(),
                        AtlasTypeUtil.createUniqueRequiredAttrDef(attrName, "string"));
        AtlasTypesDef atlasTypesDef = new AtlasTypesDef();
        atlasTypesDef.getEntityDefs().add(classTypeDef);
        createType(atlasTypesDef);

        AtlasEntity instance = new AtlasEntity(classType);
        instance.setAttribute(attrName, attrValue);
        AtlasEntityHeader entity = createEntity(instance);
        assertNotNull(entity);
        assertNotNull(entity.getGuid());

        AtlasEntity entityByGuid = getEntityByGuid(entity.getGuid());
        assertEquals(entityByGuid.getAttribute(attrName), attrValue);
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testPartialUpdate() throws Exception {
        final List<AtlasEntity> columns = new ArrayList<>();
        Map<String, Object> values = new HashMap<>();
        values.put("name", "col1");
        values.put(NAME, "qualifiedName.col1");
        values.put("type", "string");
        values.put("comment", "col1 comment");

        AtlasEntity colEntity = new AtlasEntity(BaseResourceIT.COLUMN_TYPE_V2, values);
        columns.add(colEntity);
        AtlasEntity hiveTable = createHiveTable();
        AtlasEntity tableUpdated = hiveTable;

        hiveTable.setAttribute("columns", AtlasTypeUtil.toObjectIds(columns));

        AtlasEntityWithExtInfo entityInfo = new AtlasEntityWithExtInfo(tableUpdated);
        entityInfo.addReferredEntity(colEntity);

        LOG.debug("Full Update entity= " + tableUpdated);
        EntityMutationResponse updateResult = atlasClientV2.updateEntity(entityInfo);
        assertNotNull(updateResult);
        assertNotNull(updateResult.getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE));
        assertTrue(updateResult.getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE).size() > 0);

        String guid = hiveTable.getGuid();
        AtlasEntity entityByGuid1 = getEntityByGuid(guid);
        assertNotNull(entityByGuid1);
        entityByGuid1.getAttribute("columns");

        values.put("type", "int");
        colEntity = new AtlasEntity(BaseResourceIT.COLUMN_TYPE_V2, values);
        columns.clear();
        columns.add(colEntity);

        tableUpdated = new AtlasEntity(HIVE_TABLE_TYPE_V2, "name", entityByGuid1.getAttribute("name"));
        tableUpdated.setGuid(entityByGuid1.getGuid());
        tableUpdated.setAttribute("columns", AtlasTypeUtil.toObjectIds(columns));

        // tableUpdated = hiveTable;
        // tableUpdated.setAttribute("columns", AtlasTypeUtil.toObjectIds(columns));

        LOG.debug("Partial Update entity by unique attributes= " + tableUpdated);
        Map<String, String> uniqAttributes = new HashMap<>();
        uniqAttributes.put(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, (String) hiveTable.getAttribute("name"));

        entityInfo = new AtlasEntityWithExtInfo(tableUpdated);
        entityInfo.addReferredEntity(colEntity);

        EntityMutationResponse updateResponse = atlasClientV2.updateEntityByAttribute(BaseResourceIT.HIVE_TABLE_TYPE_V2, uniqAttributes, entityInfo);

        assertNotNull(updateResponse);
        assertNotNull(updateResponse.getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE));
        assertTrue(updateResponse.getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE).size() > 0);

        AtlasEntity entityByGuid2 = getEntityByGuid(guid);
        assertNotNull(entityByGuid2);
    }

    private AtlasEntity getEntityByGuid(String guid) throws AtlasServiceException {
        return atlasClientV2.getEntityByGuid(guid).getEntity();
    }

    @Test(dependsOnMethods = "testSubmitEntity")
    public void testCompleteUpdate() throws Exception {
        final List<AtlasEntity> columns = new ArrayList<>();
        Map<String, Object> values1 = new HashMap<>();
        values1.put("name", "col3");
        values1.put(NAME, "qualifiedName.col3");
        values1.put("type", "string");
        values1.put("comment", "col3 comment");

        Map<String, Object> values2 = new HashMap<>();
        values2.put("name", "col4");
        values2.put(NAME, "qualifiedName.col4");
        values2.put("type", "string");
        values2.put("comment", "col4 comment");

        AtlasEntity colEntity1 = new AtlasEntity(BaseResourceIT.COLUMN_TYPE_V2, values1);
        AtlasEntity colEntity2 = new AtlasEntity(BaseResourceIT.COLUMN_TYPE_V2, values2);
        columns.add(colEntity1);
        columns.add(colEntity2);
        AtlasEntity hiveTable = createHiveTable();
        hiveTable.setAttribute("columns", AtlasTypeUtil.toObjectIds(columns));

        AtlasEntityWithExtInfo entityInfo = new AtlasEntityWithExtInfo(hiveTable);
        entityInfo.addReferredEntity(colEntity1);
        entityInfo.addReferredEntity(colEntity2);

        EntityMutationResponse updateEntityResult = atlasClientV2.updateEntity(entityInfo);
        assertNotNull(updateEntityResult);
        assertNotNull(updateEntityResult.getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE));
        assertNotNull(updateEntityResult.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE));
        //2 columns are being created, and 1 hiveTable is being updated
        assertEquals(updateEntityResult.getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE).size(), 1);
        assertEquals(updateEntityResult.getEntitiesByOperation(EntityMutations.EntityOperation.CREATE).size(), 2);

        AtlasEntity entityByGuid = getEntityByGuid(hiveTable.getGuid());
        List<AtlasObjectId> refs = (List<AtlasObjectId>) entityByGuid.getAttribute("columns");
        assertEquals(refs.size(), 2);
    }

    @Test
    public void testDeleteEntities() throws Exception {
        // Create 2 database entities
        AtlasEntityHeader entity1Header = createRandomDatabaseEntity();
        AtlasEntityHeader entity2Header = createRandomDatabaseEntity();

        // Delete the database entities
        EntityMutationResponse deleteResponse = atlasClientV2.deleteEntitiesByGuids(Arrays.asList(entity1Header.getGuid(), entity2Header.getGuid()));

        // Verify that deleteEntities() response has database entity guids
        assertNotNull(deleteResponse);
        assertNotNull(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE));
        assertEquals(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE).size(), 2);

        // Verify entities were deleted from the repository.
    }

    @Test
    public void testPurgeEntities() throws Exception {
        // Create 2 database entities
        AtlasEntityHeader entity1Header = createRandomDatabaseEntity();
        AtlasEntityHeader entity2Header = createRandomDatabaseEntity();

        ApplicationProperties.get().setProperty(ENTITY_NOTIFICATION_VERSION_PROPERTY, "v2");

        // Delete the database entities
        EntityMutationResponse deleteResponse = atlasClientV2.deleteEntitiesByGuids(Arrays.asList(entity1Header.getGuid(), entity2Header.getGuid()));

        // Verify that deleteEntities() response has database entity guids
        assertNotNull(deleteResponse);
        assertNotNull(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE));
        assertEquals(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE).size(), 2);

        //Wait for delete operation
        Thread.sleep(5000);

        // Purge the database entities
        Set<String> guids = Stream.of(entity1Header.getGuid(), entity2Header.getGuid()).collect(Collectors.toSet());
        EntityMutationResponse purgeResponse = atlasClientV2.purgeEntitiesByGuids(guids);

        // Verify that purgeEntities() response has database entity guids
        assertNotNull(purgeResponse);
        assertNotNull(purgeResponse.getEntitiesByOperation(EntityMutations.EntityOperation.PURGE));
        assertEquals(purgeResponse.getEntitiesByOperation(EntityMutations.EntityOperation.PURGE).size(), 2);

        AuditSearchParameters auditSearchParameters = TestResourceFileUtils.readObjectFromJson("audit-search-parameter-purge",
                AuditSearchParameters.class);
        List<AtlasAuditEntry> res = atlasClientV2.getAtlasAuditByOperation(auditSearchParameters);
        // Verify that the audit entry is set
        assertNotNull(res);
    }

    @Test
    public void testPurgeEntitiesWithoutDelete() throws Exception {
        // Create 2 database entities
        AtlasEntityHeader entity1Header = createRandomDatabaseEntity();
        AtlasEntityHeader entity2Header = createRandomDatabaseEntity();

        // Purge the database entities without delete
        Set<String> guids = Stream.of(entity1Header.getGuid(), entity2Header.getGuid()).collect(Collectors.toSet());
        EntityMutationResponse purgeResponse = atlasClientV2.purgeEntitiesByGuids(guids);

        // Verify that purgeEntities() response has database entity guids
        assertNotNull(purgeResponse);
        assertNull(purgeResponse.getEntitiesByOperation(EntityMutations.EntityOperation.PURGE));
    }

    @Test
    public void testDeleteEntityByUniqAttribute() throws Exception {
        // Create database entity
        AtlasEntity hiveDB = createHiveDB(DATABASE_NAME + randomUTF8());

        // Delete the database entity
        EntityMutationResponse deleteResponse = atlasClientV2.deleteEntityByAttribute(DATABASE_TYPE_V2, toMap(NAME, (String) hiveDB.getAttribute(NAME)));

        // Verify that deleteEntities() response has database entity guids
        assertNotNull(deleteResponse);
        assertNotNull(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE));
        assertEquals(deleteResponse.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE).size(), 1);

        // Verify entities were deleted from the repository.
    }

    @Test
    public void testPartialUpdateEntityByGuid() throws Exception {
        EntityMutationResponse updateResponse = atlasClientV2.partialUpdateEntityByGuid(createHiveTable().getGuid(), Collections.singletonMap("key1", "value1"), "description");
        assertNotNull(updateResponse);
        assertNotNull(updateResponse.getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE));
        assertTrue(updateResponse.getEntitiesByOperation(EntityMutations.EntityOperation.PARTIAL_UPDATE).size() > 0);
    }

    @Test(dependsOnMethods = "testGetTraitNames")
    public void testAddClassificationsByUniqueAttribute() throws Exception {
        traitName = "PII_Trait" + randomString();
        piiTrait =
                AtlasTypeUtil.createTraitTypeDef(traitName, Collections.<String>emptySet());
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getClassificationDefs().add(piiTrait);
        createType(typesDef);

        createdTableQualifiedName = (String) createHiveTable().getAttribute(QUALIFIED_NAME);
        atlasClientV2.addClassifications(createHiveTable().getTypeName(), toMap(QUALIFIED_NAME, createdTableQualifiedName), Collections.singletonList(new AtlasClassification(piiTrait.getName())));
        assertEntityAudit(createHiveTable().getGuid(), EntityAuditEvent.EntityAuditAction.TAG_ADD);
    }

    @Test(dependsOnMethods = "testAddClassificationsByUniqueAttribute")
    public void testUpdateClassifications() throws Exception {
        atlasClientV2.updateClassifications(createHiveTable().getGuid(), Collections.singletonList(new AtlasClassification(piiTrait.getName())));
        assertEntityAudit(createHiveTable().getGuid(), EntityAuditEvent.EntityAuditAction.TAG_UPDATE);
    }

    @Test(dependsOnMethods = "testUpdateClassifications")
    public void testUpdateClassificationsByUniqueAttribute() throws Exception {
        createdTableQualifiedName = (String) createHiveTable().getAttribute(QUALIFIED_NAME);
        atlasClientV2.updateClassifications(createHiveTable().getTypeName(), toMap(QUALIFIED_NAME, createdTableQualifiedName), Collections.singletonList(new AtlasClassification(piiTrait.getName())));
        assertEntityAudit(createHiveTable().getGuid(), EntityAuditEvent.EntityAuditAction.TAG_ADD);
    }

    @Test(dependsOnMethods = "testUpdateClassificationsByUniqueAttribute")
    public void testRemoveEntityClassification() throws Exception {
        createdTableQualifiedName = (String) createHiveTable().getAttribute(QUALIFIED_NAME);
        atlasClientV2.removeClassification(createHiveTable().getTypeName(), toMap(QUALIFIED_NAME, createdTableQualifiedName), piiTrait.getName());
        assertEntityAuditV2(createHiveTable().getGuid(), EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_DELETE);
    }

    @Test
    public void testAddOrUpdateBusinessAttributes() throws Exception {
        Map<String, Map<String, Object>> businessAttributesMap = new HashMap<>();
        Map<String, Object> bmAttrMapReq = new HashMap<>();
        bmAttrMapReq.put("attr8", "01234567890123456789");
        businessAttributesMap.put("bmWithAllTypes", bmAttrMapReq);
        atlasClientV2.addOrUpdateBusinessAttributes(createHiveTable().getGuid(), false, businessAttributesMap);
        AtlasEntity.AtlasEntityWithExtInfo info = atlasClientV2.getEntityByGuid(createHiveTable().getGuid());
        assertNotNull(info);
        Map<String, Map<String, Object>> outputMap = info.getEntity().getBusinessAttributes();
        assertNotNull(outputMap);
        assertEquals(outputMap.get("bmWithAllTypes").size(), 1);
    }

    @Test(dependsOnMethods = "testAddOrUpdateBusinessAttributes")
    public void testRemoveBusinessAttributes() throws Exception {
        Map<String, Map<String, Object>> businessAttributesMap = new HashMap<>();
        Map<String, Object> bmAttrMapReq = new HashMap<>();
        bmAttrMapReq.put("attr8", "01234567890123456789");
        businessAttributesMap.put("bmWithAllTypes", bmAttrMapReq);
        atlasClientV2.removeBusinessAttributes(createHiveTable().getGuid(), businessAttributesMap);
        AtlasEntity.AtlasEntityWithExtInfo info = atlasClientV2.getEntityByGuid(createHiveTable().getGuid());
        assertNotNull(info);
        Map<String, Map<String, Object>> outputMap = info.getEntity().getBusinessAttributes();
        assertNull(outputMap);
    }

    // TODO Enable this test case after fixing addOrUpdateBusinessAttributesByBName API.
    @Test(enabled = false)
    public void testAddOrUpdateBusinessAttributesByBName() throws Exception {
        Map<String, Map<String, Object>> businessAttributesMap = new HashMap<>();
        Map<String, Object> bmAttrMapReq = new HashMap<>();
        bmAttrMapReq.put("attr8", "01234567890123456789");
        businessAttributesMap.put("bmWithAllTypes", bmAttrMapReq);
        atlasClientV2.addOrUpdateBusinessAttributes(createHiveTable().getGuid(), "bmWithAllTypes", businessAttributesMap);
        AtlasEntity.AtlasEntityWithExtInfo info = atlasClientV2.getEntityByGuid(createHiveTable().getGuid());
        assertNotNull(info);
        Map<String, Map<String, Object>> outputMap = info.getEntity().getBusinessAttributes();
        assertNotNull(outputMap);
        assertEquals(outputMap.get("bmWithAllTypes").size(), 1);
    }

    // TODO Enable this test case after fixing addOrUpdateBusinessAttributesByBName API.
    @Test(enabled = false, dependsOnMethods = "testAddOrUpdateBusinessAttributesByBName")
    public void testRemoveBusinessAttributesByBName() throws Exception {
        Map<String, Map<String, Object>> businessAttributesMap = new HashMap<>();
        Map<String, Object> bmAttrMapReq = new HashMap<>();
        bmAttrMapReq.put("attr8", "01234567890123456789");
        businessAttributesMap.put("bmWithAllTypes", bmAttrMapReq);
        atlasClientV2.removeBusinessAttributes(createHiveTable().getGuid(), "bmWithAllTypes", businessAttributesMap);
        AtlasEntity.AtlasEntityWithExtInfo info = atlasClientV2.getEntityByGuid(createHiveTable().getGuid());
        assertNotNull(info);
        Map<String, Map<String, Object>> outputMap = info.getEntity().getBusinessAttributes();
        assertNull(outputMap);
    }

    @Test
    public void testAddLabelsByTypeName() throws Exception {
        createdDBName = (String) createHiveDB().getAttribute(NAME);
        Set<String> labels = new HashSet<>();
        labels.add("labelByTypeName");
        atlasClientV2.addLabels(createHiveDB().getTypeName(), toMap(NAME, createdDBName), labels);
        AtlasEntityWithExtInfo info = atlasClientV2.getEntityByGuid(createHiveDB().getGuid(), false, true);
        assertNotNull(info);
        assertNotNull(info.getEntity().getLabels());
        assertEquals(info.getEntity().getLabels().size(), 1);
    }

    @Test(dependsOnMethods = "testAddLabelsByTypeName")
    public void testSetLabelsByTypeName() throws Exception {
        createdDBName = (String) createHiveDB().getAttribute(NAME);
        Set<String> labels = new HashSet<>();
        labels.add("labelByTypeNameNext");
        atlasClientV2.setLabels(createHiveDB().getTypeName(), toMap(NAME, createdDBName), labels);
        AtlasEntityWithExtInfo infoForSet = atlasClientV2.getEntityByGuid(createHiveDB().getGuid(), false, true);
        assertNotNull(infoForSet);
        assertNotNull(infoForSet.getEntity().getLabels());
        assertEquals(infoForSet.getEntity().getLabels().size(), 2);
    }


    @Test(dependsOnMethods = "testSetLabelsByTypeName")
    public void testDeleteLabelsByTypeName() throws Exception {
        Set<String> labels = new HashSet<>();
        labels.add("labelByTypeNameNext");
        createdDBName = (String) createHiveDB().getAttribute(NAME);
        atlasClientV2.removeLabels(createHiveDB().getTypeName(), toMap(NAME, createdDBName), labels);
        AtlasEntityWithExtInfo info = atlasClientV2.getEntityByGuid(createHiveDB().getGuid(), false, true);
        assertNotNull(info);
        assertNotNull(info.getEntity().getLabels());
        assertEquals(info.getEntity().getLabels().size(), 1);
    }

    @Test()
    public void testAddClassification() throws Exception {
        traitName = "PII_Trait" + randomString();
        AtlasClassificationDef piiTrait =
                AtlasTypeUtil.createTraitTypeDef(traitName, Collections.<String>emptySet());
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getClassificationDefs().add(piiTrait);
        createType(typesDef);
        ClassificationAssociateRequest request = new ClassificationAssociateRequest();
        request.setEntityGuids(Arrays.asList(createHiveTable().getGuid(), createHiveDB().getGuid()));
        request.setClassification(new AtlasClassification(piiTrait.getName()));

        atlasClientV2.addClassification(request);

        assertEntityAuditV2(createHiveTable().getGuid(), EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_ADD);
        assertEntityAuditV2(createHiveDB().getGuid(), EntityAuditEventV2.EntityAuditActionV2.CLASSIFICATION_ADD);
        AtlasClassifications classificationsTable = atlasClientV2.getEntityClassifications(createHiveTable().getGuid(), piiTrait.getName());
        assertNotNull(classificationsTable);
        AtlasClassifications classificationsDB = atlasClientV2.getEntityClassifications(createHiveDB().getGuid(), piiTrait.getName());
        assertNotNull(classificationsDB);
    }

    @Test()
    public void testDeleteClassifications() throws Exception {
        final String guid = createHiveTable().getGuid();
        try {
            atlasClientV2.deleteClassifications(guid, Arrays.asList(new AtlasClassification(getAndAddClassification().getName()), new AtlasClassification(getAndAddClassification().getName())));
        } catch (AtlasServiceException ex) {
            fail("Deletion should've succeeded");
        }
        assertEntityAudit(guid, EntityAuditEvent.EntityAuditAction.TAG_DELETE);
    }

    @Test()
    public void testRemoveEntityClassificationByGuid() throws Exception {
        final String guid = createHiveTable().getGuid();
        try {
            String name = getAndAddClassification().getName();
            atlasClientV2.removeClassification(guid, name, guid);
        } catch (AtlasServiceException ex) {
            fail("Deletion should've succeeded");
        }
        assertEntityAudit(guid, EntityAuditEvent.EntityAuditAction.TAG_DELETE);
    }

    @Test()
    public void testProduceTemplate() {
        try {
            String template = atlasClientV2.getTemplateForBulkUpdateBusinessAttributes();
            assertNotNull(template);
        } catch (AtlasServiceException ex) {
            fail("Deletion should've succeeded");
        }
    }

    //TODO Enable this test after fixing the BulkImportResponse Deserialization issue.
    @Test(dependsOnMethods = "testSubmitEntity", enabled = false)
    public void testImportBMAttributes() throws AtlasServiceException {
        BulkImportResponse response = atlasClientV2.bulkUpdateBusinessAttributes(TestResourceFileUtils.getTestFilePath("template_metadata.csv"));
        assertNotNull(response);
    }

    private void createImportFile() throws Exception {
        try {
            String filePath = TestResourceFileUtils.getTestFilePath("template_metadata.csv");
            String   dbName = (String) createHiveTable().getAttribute("name");
            String   header = "TypeName,UniqueAttributeValue,BusinessAttributeName,BusinessAttributeValue,UniqueAttributeName[optional]";
            String   values = "hive_table_v2," + dbName + ",bmWithAllTypes.attr8,\"Awesome Attribute 1\",qualifiedName";
            File   tempFile = new File(filePath);
            FileUtils.writeLines(tempFile, Arrays.asList(header, values));
        } catch (IOException e) {
            fail("Should have created file");
            throw new AtlasServiceException(e);
        }
    }

    private void assertEntityAudit(String dbid, EntityAuditEvent.EntityAuditAction auditAction)
            throws Exception {
        List<EntityAuditEvent> events = atlasClientV1.getEntityAuditEvents(dbid, (short) 100);
        for (EntityAuditEvent event : events) {
            if (event.getAction() == auditAction) {
                return;
            }
        }
        fail("Expected audit event with action = " + auditAction);
    }

    private void assertEntityAuditV2(String guid, EntityAuditEventV2.EntityAuditActionV2 auditAction)
            throws Exception {
        // Passing auditAction as "null" as this feature is not added for InMemoryEntityRepository for IT testing.
        List<EntityAuditEventV2> events = atlasClientV2.getAuditEvents(guid, "", null, (short) 100);
        assertNotNull(events);
        assertNotEquals(events.size(), 0);
        ObjectMapper mapper = new ObjectMapper();

        List<EntityAuditEventV2> auditEventV2s = mapper.convertValue(
                events,
                new TypeReference<List<EntityAuditEventV2>>() {
                });
        for (EntityAuditEventV2 event : auditEventV2s) {
            if (event.getAction() == auditAction) {
                return;
            }
        }
        fail("Expected audit event with action = " + auditAction);
    }

    private void addProperty(String guid, String property, Object value) throws AtlasServiceException {

        AtlasEntity entityByGuid = getEntityByGuid(guid);
        entityByGuid.setAttribute(property, value);
        EntityMutationResponse response = atlasClientV2.updateEntity(new AtlasEntityWithExtInfo(entityByGuid));
        assertNotNull(response);
        assertNotNull(response.getEntitiesByOperation(EntityMutations.EntityOperation.UPDATE));
    }

    private AtlasEntity createHiveDB() {
        if (dbEntity == null) {
            dbEntity = createHiveDB(DATABASE_NAME);
        }
        return dbEntity;
    }

    private AtlasEntity createHiveDB(String dbName) {
        AtlasEntity hiveDBInstanceV2 = createHiveDBInstanceV2(dbName);
        AtlasEntityHeader entityHeader = createEntity(hiveDBInstanceV2);
        assertNotNull(entityHeader);
        assertNotNull(entityHeader.getGuid());
        hiveDBInstanceV2.setGuid(entityHeader.getGuid());
        return hiveDBInstanceV2;
    }

    private TypesUtil.Pair<AtlasEntity, AtlasEntity> createDBAndTable() throws Exception {
        AtlasEntity dbInstanceV2 = createHiveDB();
        AtlasEntity hiveTableInstanceV2 = createHiveTable();
        return TypesUtil.Pair.of(dbInstanceV2, hiveTableInstanceV2);
    }

    private AtlasEntity createHiveTable() throws Exception {
        if (tableEntity == null) {
            tableEntity = createHiveTable(createHiveDB(), TABLE_NAME);
        }
        return tableEntity;
    }

    private AtlasEntity createHiveTable(AtlasEntity dbInstanceV2, String tableName) throws Exception {
        AtlasEntity hiveTableInstanceV2 = createHiveTableInstanceV2(dbInstanceV2, tableName);
        AtlasEntityHeader createdHeader = createEntity(hiveTableInstanceV2);
        assertNotNull(createdHeader);
        assertNotNull(createdHeader.getGuid());
        hiveTableInstanceV2.setGuid(createdHeader.getGuid());
        tableEntity = hiveTableInstanceV2;
        return hiveTableInstanceV2;
    }

    private AtlasClassificationDef getAndAddClassification() throws Exception {
        String traitNameNext = "PII_Trait" + randomString();
        AtlasClassificationDef piiTrait =
                AtlasTypeUtil.createTraitTypeDef(traitNameNext, Collections.<String>emptySet());
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getClassificationDefs().add(piiTrait);
        createType(typesDef);

        atlasClientV2.addClassifications(createHiveTable().getGuid(), Collections.singletonList(new AtlasClassification(piiTrait.getName())));

        assertEntityAudit(createHiveTable().getGuid(), EntityAuditEvent.EntityAuditAction.TAG_ADD);
        AtlasClassifications classifications = atlasClientV2.getEntityClassifications(createHiveTable().getGuid(), piiTrait.getName());
        assertNotNull(classifications);
        return piiTrait;
    }

    private Map<String, String> toMap(final String name, final String value) {
        return new HashMap<String, String>() {{
            put(name, value);
        }};
    }

    private AtlasEntityHeader createRandomDatabaseEntity() {
        AtlasEntity db = new AtlasEntity(DATABASE_TYPE_V2);
        String dbName = randomString();
        db.setAttribute("name", dbName);
        db.setAttribute(NAME, dbName);
        db.setAttribute("clusterName", randomString());
        db.setAttribute("description", randomString());
        return createEntity(db);
    }

    private String random() {
        return RandomStringUtils.random(10);
    }

    private String randomUTF8() throws Exception {
        String ret = random();

        if (!StandardCharsets.UTF_8.equals(Charset.defaultCharset())) {
            ret = new String(ret.getBytes(), StandardCharsets.UTF_8.name());
        }

        return ret;
    }
}
