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

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.EntityAuditEvent;
import org.apache.atlas.model.legacy.EntityResult;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.v1.model.instance.Id;
import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.v1.model.typedef.*;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.sun.jersey.api.client.ClientResponse.Status.BAD_REQUEST;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;


/**
 * Integration tests for Entity Jersey Resource.
 */
public class EntityJerseyResourceIT extends BaseResourceIT {

    private static final Logger LOG = LoggerFactory.getLogger(EntityJerseyResourceIT.class);

    private static final String TRAITS = "traits";

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        createTypeDefinitionsV1();

    }

    @Test
    public void testCreateNestedEntities() throws Exception {
        Referenceable databaseInstance = new Referenceable(DATABASE_TYPE);
        databaseInstance.set("name", "db_"+ randomString());
        databaseInstance.set("description", "foo database");

        int nTables = 5;
        int colsPerTable=3;
        List<Referenceable> tables = new ArrayList<>();
        List<Referenceable> allColumns = new ArrayList<>();

        for(int i = 0; i < nTables; i++) {
            String tableName = "db1-table-" + i + randomString();

            Referenceable tableInstance = new Referenceable(HIVE_TABLE_TYPE);
            tableInstance.set("name", tableName);
            tableInstance.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, tableName);
            tableInstance.set("db", databaseInstance);
            tableInstance.set("description", tableName + " table");
            tables.add(tableInstance);

            List<Referenceable> columns = new ArrayList<>();
            for(int j = 0; j < colsPerTable; j++) {
                Referenceable columnInstance = new Referenceable(COLUMN_TYPE);
                columnInstance.set("name", tableName + "-col-" + j + randomString());
                columnInstance.set("dataType", "String");
                columnInstance.set("comment", "column " + j + " for table " + i);
                allColumns.add(columnInstance);
                columns.add(columnInstance);
            }
            tableInstance.set("columns", columns);
        }

        //Create the tables.  The database and columns should be created automatically, since
        //the tables reference them.
        List<String> entityGUIDs = atlasClientV1.createEntity(tables);
        assertNotNull(entityGUIDs);
        assertEquals(entityGUIDs.size(), nTables * (colsPerTable + 1) + 1);
    }


    @Test
    public void testSubmitEntity() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable referenceable = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(referenceable);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }
    }

    @Test
    public void testRequestUser() throws Exception {
        Referenceable entity = new Referenceable(DATABASE_TYPE_BUILTIN);
        String dbName = randomString();
        entity.set("name", dbName);
        entity.set(QUALIFIED_NAME, dbName);
        entity.set("clusterName", randomString());
        entity.set("description", randomString());
        entity.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName);
        entity.set("owner", "user1");
        entity.set("clusterName", "cl1");
        entity.set("parameters", Collections.EMPTY_MAP);
        entity.set("location", "/tmp");


        String user = "admin";
        AtlasClient localClient = null;
        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            localClient = new AtlasClient(atlasUrls, new String[]{"admin", "admin"});
        } else {
            localClient = new AtlasClient(atlasUrls);
        }
        String entityId = localClient.createEntity(entity).get(0);

        List<EntityAuditEvent> events = atlasClientV1.getEntityAuditEvents(entityId, (short) 10);
        assertEquals(events.size(), 1);
        assertEquals(events.get(0).getUser(), user);
    }

    @Test
    //API should accept single entity (or jsonarray of entities)
    public void testSubmitSingleEntity() throws Exception {
        Referenceable databaseInstance = new Referenceable(DATABASE_TYPE_BUILTIN);
        String dbName = randomString();
        databaseInstance.set("name", dbName);
        databaseInstance.set(QUALIFIED_NAME, dbName);
        databaseInstance.set("clusterName", randomString());
        databaseInstance.set("description", randomString());
        databaseInstance.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName);
        databaseInstance.set("owner", "user1");
        databaseInstance.set("clusterName", "cl1");
        databaseInstance.set("parameters", Collections.EMPTY_MAP);
        databaseInstance.set("location", "/tmp");

        ObjectNode response = atlasClientV1.callAPIWithBody(AtlasClient.API_V1.CREATE_ENTITY, AtlasType.toV1Json(databaseInstance));
        assertNotNull(response);
        Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));

        EntityResult entityResult = EntityResult.fromString(response.toString());
        assertEquals(entityResult.getCreatedEntities().size(), 1);
        assertNotNull(entityResult.getCreatedEntities().get(0));
    }

    @Test
    public void testEntityDeduping() throws Exception {
        final String dbName = "db" + randomString();
        Referenceable HiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbIdReference = createInstance(HiveDBInstance);
        final String dbId = dbIdReference._getId();

        assertEntityAudit(dbId, EntityAuditEvent.EntityAuditAction.ENTITY_CREATE);

//        Disabling DSL tests until v2 DSL implementation is ready

//        JSONArray results = searchByDSL(String.format("%s where qualifiedName='%s'", DATABASE_TYPE_BUILTIN, dbName));
//        assertEquals(results.length(), 1);
//
//        //create entity again shouldn't create another instance with same unique attribute value
//        List<String> entityResults = atlasClientV1.createEntity(HiveDBInstance);
//        assertEquals(entityResults.size(), 0);
//
//        results = searchByDSL(String.format("%s where qualifiedName='%s'", DATABASE_TYPE_BUILTIN, dbName));
//        assertEquals(results.length(), 1);
//
//        //Test the same across references
//        Referenceable table = new Referenceable(HIVE_TABLE_TYPE_BUILTIN);
//        final String tableName = randomString();
//        Referenceable tableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbIdReference);
//        atlasClientV1.createEntity(tableInstance);
//        results = searchByDSL(String.format("%s where qualifiedName='%s'", DATABASE_TYPE_BUILTIN, dbName));
//        assertEquals(results.length(), 1);
    }

    private void assertEntityAudit(String dbid, EntityAuditEvent.EntityAuditAction auditAction) throws Exception {
        List<EntityAuditEvent> events = atlasClientV1.getEntityAuditEvents(dbid, (short) 100);

        for (EntityAuditEvent event : events) {
            if (event.getAction() == auditAction) {
                return;
            }
        }
        fail("Expected audit event with action = " + auditAction);
    }

    @Test
    public void testEntityDefinitionAcrossTypeUpdate() throws Exception {
        //create type
        ClassTypeDefinition typeDefinition = TypesUtil
                .createClassTypeDef(randomString(), null, Collections.<String>emptySet(),
                        TypesUtil.createUniqueRequiredAttrDef("name", AtlasBaseTypeDef.ATLAS_TYPE_STRING));

        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.singletonList(typeDefinition));

        atlasClientV1.createType(AtlasType.toV1Json(typesDef));

        //create entity for the type
        Referenceable instance = new Referenceable(typeDefinition.getTypeName());
        instance.set("name", randomString());
        String guid = atlasClientV1.createEntity(instance).get(0);

        //update type - add attribute
        typeDefinition = TypesUtil.createClassTypeDef(typeDefinition.getTypeName(), null, Collections.<String>emptySet(),
                TypesUtil.createUniqueRequiredAttrDef("name", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                TypesUtil.createOptionalAttrDef("description", AtlasBaseTypeDef.ATLAS_TYPE_STRING));
        TypesDef typeDef = new TypesDef(Collections.<EnumTypeDefinition>emptyList(),
                Collections.<StructTypeDefinition>emptyList(), Collections.<TraitTypeDefinition>emptyList(),
                Arrays.asList(typeDefinition));
        atlasClientV1.updateType(typeDef);

        //Get definition after type update - new attributes should be null
        Referenceable entity = atlasClientV1.getEntity(guid);
        Assert.assertNull(entity.get("description"));
        Assert.assertEquals(entity.get("name"), instance.get("name"));
    }

    @DataProvider
    public Object[][] invalidAttrValues() {
        return new Object[][]{{null}, {""}};
    }

    @Test(dataProvider = "invalidAttrValues")
    public void testEntityInvalidValue(String value) throws Exception {
        Referenceable databaseInstance = new Referenceable(DATABASE_TYPE_BUILTIN);
        databaseInstance.set("name", randomString());
        databaseInstance.set("description", value);

        try {
            createInstance(databaseInstance);
            Assert.fail("Expected AtlasServiceException");
        } catch (AtlasServiceException e) {
            Assert.assertEquals(e.getStatus(), BAD_REQUEST);
        }
    }

    @Test
    public void testGetEntityByAttribute() throws Exception {
        Referenceable db1 = new Referenceable(DATABASE_TYPE_BUILTIN);
        String dbName = randomString();
        String qualifiedName = dbName + "@cl1";
        db1.set(NAME, dbName);
        db1.set(DESCRIPTION, randomString());
        db1.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qualifiedName);
        db1.set("owner", "user1");
        db1.set(CLUSTER_NAME, "cl1");
        db1.set("parameters", Collections.EMPTY_MAP);
        db1.set("location", "/tmp");
        createInstance(db1);

        //get entity by attribute
        Referenceable referenceable = atlasClientV1.getEntity(DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, qualifiedName);
        Assert.assertEquals(referenceable.getTypeName(), DATABASE_TYPE_BUILTIN);
        Assert.assertEquals(referenceable.get(QUALIFIED_NAME), dbName + "@" + "cl1");
    }

    @Test
    public void testSubmitEntityWithBadDateFormat() {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = null;
        try {
            dbId = createInstance(hiveDBInstance);
            Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
            hiveTableInstance.set("lastAccessTime", "2014-07-11");
            createInstance(hiveTableInstance);
        } catch (AtlasServiceException e) {
            // Should catch the exception
            assertEquals(e.getStatus().getStatusCode(), BAD_REQUEST.getStatusCode());
        } catch (Exception e) {
            // ignore
        }

    }

    @Test
    public void testAddProperty() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable referenceable = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(referenceable);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }
        //add property
        String description = "bar table - new desc";
        addProperty(guid, "description", description);

        ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.GET_ENTITY, null, guid);
        Assert.assertNotNull(response);

        referenceable.set("description", description);

        //invalid property for the type
        try {
            addProperty(guid, "invalid_property", "bar table");
            Assert.fail("Expected AtlasServiceException");
        } catch (AtlasServiceException e) {
            Assert.assertEquals(e.getStatus().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
        }

        String currentTime = String.valueOf(new DateTime());

        // updating date attribute as string not supported in v2
        // addProperty(guid, "createTime", currentTime);

        response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.GET_ENTITY, null, guid);
        Assert.assertNotNull(response);

        referenceable.set("createTime", currentTime);
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testAddNullProperty() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        //add property
        addProperty(guid, null, "foo bar");
        Assert.fail();
    }

    @Test(enabled = false)
    public void testAddNullPropertyValue() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        //add property
        try {
            addProperty(guid, "description", null);
            Assert.fail("Expected AtlasServiceException");
        } catch(AtlasServiceException e) {
            Assert.assertEquals(e.getStatus().getStatusCode(), Response.Status.BAD_REQUEST.getStatusCode());
        }
    }

    @Test
    public void testAddReferenceProperty() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        //Create new db instance
        dbName = "db" + randomString();
        Referenceable databaseInstance = new Referenceable(DATABASE_TYPE_BUILTIN);
        databaseInstance.set(NAME, dbName);
        databaseInstance.set(QUALIFIED_NAME, dbName);
        databaseInstance.set(CLUSTER_NAME, randomString());
        databaseInstance.set("description", "new database");
        databaseInstance.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName);
        databaseInstance.set("owner", "user1");
        databaseInstance.set(CLUSTER_NAME, "cl1");
        databaseInstance.set("parameters", Collections.EMPTY_MAP);
        databaseInstance.set("location", "/tmp");

        Id dbInstance = createInstance(databaseInstance);
        String newDBId = dbInstance._getId();

        //Add reference property
        EntityResult entityResult = atlasClientV1.updateEntityAttribute(guid, "db", newDBId);
        assertEquals(entityResult.getUpdateEntities().size(), 2);
        assertEquals(entityResult.getUpdateEntities().get(0), newDBId);
        assertEquals(entityResult.getUpdateEntities().get(1), guid);
    }

    @Test
    public void testGetEntityDefinition() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        Referenceable entity = atlasClientV1.getEntity(guid);
        Assert.assertNotNull(entity);
    }

    private void addProperty(String guid, String property, String value) throws AtlasServiceException {
        EntityResult entityResult = atlasClientV1.updateEntityAttribute(guid, property, value);
        assertEquals(entityResult.getUpdateEntities().size(), 1);
        assertEquals(entityResult.getUpdateEntities().get(0), guid);
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testGetInvalidEntityDefinition() throws Exception {

        ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.GET_ENTITY, null, "blah");

        Assert.assertNotNull(response);

        Assert.assertNotNull(response.get(AtlasClient.ERROR));
    }

    @Test
    public void testGetEntityList() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        List<String> entities = atlasClientV1.listEntities(HIVE_TABLE_TYPE_BUILTIN);
        Assert.assertNotNull(entities);
        Assert.assertTrue(entities.contains(guid));
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testGetEntityListForBadEntityType() throws Exception {
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("type", "blah");

        ObjectNode response = atlasClientV1.callAPIWithQueryParams(AtlasClient.API_V1.GET_ENTITY, queryParams);
        assertNotNull(response);
        Assert.assertNotNull(response.get(AtlasClient.ERROR));
    }


    @Test
    public void testGetEntityListForNoInstances() throws Exception {
        String typeName = addNewType();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("type", typeName);

        ObjectNode response = atlasClientV1.callAPIWithQueryParams(AtlasClient.API_V1.GET_ENTITY, queryParams);
        assertNotNull(response);
        Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));

        final ArrayNode list = (ArrayNode) response.get(AtlasClient.RESULTS);
        Assert.assertEquals(list.size(), 0);
    }

    private String addNewType() throws Exception {
        String typeName = "test" + randomString();
        ClassTypeDefinition testTypeDefinition = TypesUtil
                .createClassTypeDef(typeName, null, Collections.<String>emptySet(),
                        TypesUtil.createRequiredAttrDef("name", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        TypesUtil.createRequiredAttrDef("description", AtlasBaseTypeDef.ATLAS_TYPE_STRING));

        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.singletonList(testTypeDefinition));

        createType(AtlasType.toV1Json(typesDef));
        return typeName;
    }

    @Test
    public void testGetTraitNames() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        List<String> traits = atlasClientV1.listTraits(guid);
        assertNotNull(traits);
        Assert.assertEquals(traits.size(), 7);
    }

    @Test
    public void testAddTrait() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        String traitName = "PII_Trait" + randomString();
        TraitTypeDefinition piiTrait =
                TypesUtil.createTraitTypeDef(traitName, null, Collections.<String>emptySet());
        String traitDefinitionAsJSON = AtlasType.toV1Json(piiTrait);
        LOG.debug("traitDefinitionAsJSON = {}", traitDefinitionAsJSON);

        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait), Collections.emptyList());

        createType(typesDef);

        Struct traitInstance = new Struct(traitName);

        atlasClientV1.addTrait(guid, traitInstance);
        assertEntityAudit(guid, EntityAuditEvent.EntityAuditAction.TAG_ADD);
    }

    @Test
    public void testGetTraitDefinitionForEntity() throws Exception{
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        String traitName = "PII_Trait" + randomString();
        TraitTypeDefinition piiTrait = TypesUtil.createTraitTypeDef(traitName, null, Collections.<String>emptySet());

        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait), Collections.emptyList());
        String traitDefinitionAsJSON = AtlasType.toV1Json(typesDef);
        LOG.debug("traitDefinitionAsJSON = {}", traitDefinitionAsJSON);
        createType(AtlasType.toV1Json(typesDef));

        Struct traitInstance = new Struct(traitName);
        atlasClientV1.addTrait(guid, traitInstance);
        Struct traitDef = atlasClientV1.getTraitDefinition(guid, traitName);
        Assert.assertEquals(traitDef.getTypeName(), traitName);

        List<Struct> allTraitDefs = atlasClientV1.listTraitDefinitions(guid);
        System.out.println(allTraitDefs.toString());
        Assert.assertEquals(allTraitDefs.size(), 8);
    }

    @Test
    public void testAddExistingTrait() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        String traitName = "PII_Trait" + randomString();
        TraitTypeDefinition piiTrait =
                TypesUtil.createTraitTypeDef(traitName, null, Collections.<String>emptySet());
        String traitDefinitionAsJSON = AtlasType.toV1Json(piiTrait);
        LOG.debug("traitDefinitionAsJSON = {}", traitDefinitionAsJSON);

        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait), Collections.emptyList());

        createType(typesDef);

        Struct traitInstance = new Struct(traitName);
        atlasClientV1.addTrait(guid, traitInstance);

        try {
            atlasClientV1.addTrait(guid, traitInstance);
            fail("Duplicate trait addition should've failed");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus(), BAD_REQUEST);
        }
    }

    @Test
    public void testAddTraitWithAttribute() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        final String traitName = "PII_Trait" + randomString();
        TraitTypeDefinition piiTrait = TypesUtil
                .createTraitTypeDef(traitName, null, Collections.<String>emptySet(),
                        TypesUtil.createRequiredAttrDef("type", AtlasBaseTypeDef.ATLAS_TYPE_STRING));
        String traitDefinitionAsJSON = AtlasType.toV1Json(piiTrait);
        LOG.debug("traitDefinitionAsJSON = {}", traitDefinitionAsJSON);

        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait), Collections.emptyList());

        createType(typesDef);

        Struct traitInstance = new Struct(traitName);
        traitInstance.set("type", "SSN");
        atlasClientV1.addTrait(guid, traitInstance);

        // verify the response
        Referenceable entity = atlasClientV1.getEntity(guid);
        Assert.assertNotNull(entity);
        Assert.assertEquals(entity.getId()._getId(), guid);

        assertNotNull(entity.getTrait(traitName));
        assertEquals(entity.getTrait(traitName).get("type"), traitInstance.get("type"));
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testAddTraitWithNoRegistration() throws Exception {
        final String traitName = "PII_Trait" + randomString();
        TraitTypeDefinition piiTrait =
                TypesUtil.createTraitTypeDef(traitName, null, Collections.<String>emptySet());
        String traitDefinitionAsJSON = AtlasType.toV1Json(piiTrait);
        LOG.debug("traitDefinitionAsJSON = {}", traitDefinitionAsJSON);

        Struct traitInstance = new Struct(traitName);
        String traitInstanceAsJSON = AtlasType.toV1Json(traitInstance);
        LOG.debug("traitInstanceAsJSON = {}", traitInstanceAsJSON);

        atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.CREATE_ENTITY, traitInstanceAsJSON, "random", TRAITS);
    }

    @Test
    public void testDeleteTrait() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        String traitName = "PII_Trait" + randomString();
        TraitTypeDefinition piiTrait =
                TypesUtil.createTraitTypeDef(traitName, null, Collections.<String>emptySet());
        String traitDefinitionAsJSON = AtlasType.toV1Json(piiTrait);
        LOG.debug("traitDefinitionAsJSON = {}", traitDefinitionAsJSON);

        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait), Collections.emptyList());

        createType(typesDef);

        Struct traitInstance = new Struct(traitName);

        atlasClientV1.addTrait(guid, traitInstance);
        assertEntityAudit(guid, EntityAuditEvent.EntityAuditAction.TAG_ADD);

        atlasClientV1.deleteTrait(guid, traitName);

        try {
            atlasClientV1.getTraitDefinition(guid, traitName);
            fail("Deleted trait definition shouldn't exist");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus(), ClientResponse.Status.NOT_FOUND);
            assertEntityAudit(guid, EntityAuditEvent.EntityAuditAction.TAG_DELETE);
        }
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testDeleteTraitNonExistent() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        final String traitName = "blah_trait";
        atlasClientV1.deleteTrait(guid, traitName);
        fail("trait=" + traitName + " should be defined in type system before it can be deleted");
    }

    @Test
    public void testDeleteExistentTraitNonExistentForEntity() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id id = createInstance(hiveTableInstance);

        final String guid = id._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        final String traitName = "PII_Trait" + randomString();
        TraitTypeDefinition piiTrait = TypesUtil
                .createTraitTypeDef(traitName, null, Collections.<String>emptySet(),
                        TypesUtil.createRequiredAttrDef("type", AtlasBaseTypeDef.ATLAS_TYPE_STRING));

        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(piiTrait), Collections.emptyList());

        createType(AtlasType.toV1Json(typesDef));

        try {
            atlasClientV1.deleteTrait(guid, traitName);
            fail("Call should've failed for deletion of invalid trait");
        } catch (AtlasServiceException e) {
            assertNotNull(e);
            assertNotNull(e.getStatus());
            assertEquals(e.getStatus(), ClientResponse.Status.BAD_REQUEST);
        }
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

    @Test
    public void testUTF8() throws Exception {
        String              attrName            = randomUTF8();
        String              attrValue           = randomUTF8();
        String              classType           = randomString(); //Type names cannot be arbitrary UTF8 characters. See org.apache.atlas.type.AtlasTypeUtil#validateType()
        ClassTypeDefinition classTypeDefinition = TypesUtil.createClassTypeDef(classType, null, Collections.<String>emptySet(), TypesUtil.createUniqueRequiredAttrDef(attrName, AtlasBaseTypeDef.ATLAS_TYPE_STRING));
        TypesDef            typesDef            = new TypesDef(Collections.<EnumTypeDefinition>emptyList(), Collections.<StructTypeDefinition>emptyList(), Collections.<TraitTypeDefinition>emptyList(), Collections.singletonList(classTypeDefinition));

        createType(typesDef);

        Referenceable entityToCreate  = new Referenceable(classType, Collections.singletonMap(attrName, attrValue));
        Id            guid            = createInstance(entityToCreate);
        ObjectNode    response        = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.GET_ENTITY, null, guid._getId());
        Object        objResponse     = response.get(AtlasClient.DEFINITION);
        String        jsonResponse    = AtlasType.toJson(objResponse);
        Referenceable createdEntity   = AtlasType.fromV1Json(jsonResponse, Referenceable.class);
        Object        entityAttrValue = createdEntity.get(attrName);

        Assert.assertEquals(entityAttrValue, attrValue,
                            "attrName=" + attrName + "; attrValue=" + attrValue + "; entityToCreate=" + entityToCreate + "; entityId=" + guid + "; getEntityResponse_Obj=" + objResponse + "; getEntityResponse_Json=" + jsonResponse + "; getEntityResponse_Entity=" + createdEntity);
    }


    @Test
    public void testPartialUpdateByGuid() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id tableId = createInstance(hiveTableInstance);

        final String guid = tableId._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        String colName = "col1"+randomString();
        final List<Referenceable> columns = new ArrayList<>();
        Map<String, Object> values = new HashMap<>();
        values.put(NAME, colName);
        values.put("comment", "col1 comment");
        values.put(QUALIFIED_NAME, "default.table.col1@"+colName);
        values.put("comment", "col1 comment");
        values.put("type", "string");
        values.put("owner", "user1");
        values.put("position", 0);
        values.put("description", "col1");
        values.put("table", tableId); //table is a required reference, can't be null
        values.put("userDescription", null);
        values.put("displayName", null);

        Referenceable ref = new Referenceable(BaseResourceIT.COLUMN_TYPE_BUILTIN, values);
        columns.add(ref);
        Referenceable tableUpdated = new Referenceable(BaseResourceIT.HIVE_TABLE_TYPE_BUILTIN, new HashMap<String, Object>() {{
            put("columns", columns);
        }});

        LOG.debug("Updating entity= {}", tableUpdated);
        EntityResult entityResult = atlasClientV1.updateEntity(guid, tableUpdated);
        assertEquals(entityResult.getUpdateEntities().size(), 1);
        assertEquals(entityResult.getUpdateEntities().get(0), guid);

        Referenceable entity = atlasClientV1.getEntity(guid);
        List<Referenceable> refs = (List<Referenceable>) entity.get("columns");

        Referenceable column = refs.get(0);

        assertEquals(columns.get(0).getValues(), column.getValues());
        assertEquals(columns.get(0).getTypeName(), column.getTypeName());
        assertEquals(columns.get(0).getTraits(), column.getTraits());
        assertEquals(columns.get(0).getTraitNames(), column.getTraitNames());
    }

    @Test
    public void testPartialUpdateByUniqueAttributes() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id tableId = createInstance(hiveTableInstance);

        final String guid = tableId._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        String colName = "col1"+randomString();
        final List<Referenceable> columns = new ArrayList<>();
        Map<String, Object> values = new HashMap<>();
        values.put(NAME, colName);
        values.put("comment", "col1 comment");
        values.put(QUALIFIED_NAME, "default.table.col1@"+colName);
        values.put("comment", "col1 comment");
        values.put("type", "string");
        values.put("owner", "user1");
        values.put("position", 0);
        values.put("description", "col1");
        values.put("table", tableId); //table is a required reference, can't be null
        values.put("userDescription", null);
        values.put("displayName", null);

        Referenceable ref = new Referenceable(BaseResourceIT.COLUMN_TYPE_BUILTIN, values);
        columns.add(ref);

        //Update by unique attribute
        values.put("type", "int");
        ref = new Referenceable(BaseResourceIT.COLUMN_TYPE_BUILTIN, values);
        columns.set(0, ref);
        Referenceable tableUpdated = new Referenceable(BaseResourceIT.HIVE_TABLE_TYPE_BUILTIN, new HashMap<String, Object>() {{
            put("columns", columns);
        }});

        LOG.debug("Updating entity= {}", tableUpdated);
        EntityResult entityResult = atlasClientV1.updateEntity(BaseResourceIT.HIVE_TABLE_TYPE_BUILTIN, AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME,
                (String) hiveTableInstance.get(QUALIFIED_NAME), tableUpdated);
        assertEquals(entityResult.getUpdateEntities().size(), 1);
        assertEquals(entityResult.getUpdateEntities().get(0), guid);

        Referenceable entity = atlasClientV1.getEntity(guid);
        List<Referenceable> refs = (List<Referenceable>) entity.get("columns");

        Assert.assertTrue(refs.get(0).getValuesMap().equals(values));
        Assert.assertEquals(refs.get(0).get("type"), "int");
    }

    @Test
    public void testCompleteUpdate() throws Exception {
        String dbName = "db" + randomString();
        String tableName = "table" + randomString();
        Referenceable hiveDBInstance = createHiveDBInstanceBuiltIn(dbName);
        Id dbId = createInstance(hiveDBInstance);
        Referenceable hiveTableInstance = createHiveTableInstanceBuiltIn(dbName, tableName, dbId);
        Id tableId = createInstance(hiveTableInstance);

        final String guid = tableId._getId();
        try {
            Assert.assertNotNull(UUID.fromString(guid));
        } catch (IllegalArgumentException e) {
            Assert.fail("Response is not a guid, " + guid);
        }

        final List<Referenceable> columns = new ArrayList<>();
        Map<String, Object> values1 = new HashMap<>();
        values1.put(NAME, "col3");
        values1.put(QUALIFIED_NAME, "default.table.col3@cl1");
        values1.put("comment", "col3 comment");
        values1.put("type", "string");
        values1.put("owner", "user1");
        values1.put("position", 0);
        values1.put("description", "col3");
        values1.put("table", tableId);
        values1.put("userDescription", null);
        values1.put("displayName", null);

        Map<String, Object> values2 = new HashMap<>();
        values2.put(NAME, "col4");
        values2.put(QUALIFIED_NAME, "default.table.col4@cl1");
        values2.put("comment", "col4 comment");
        values2.put("type", "string");
        values2.put("owner", "user2");
        values2.put("position", 1);
        values2.put("description", "col4");
        values2.put("table", tableId);
        values2.put("userDescription", null);
        values2.put("displayName", null);

        Referenceable ref1 = new Referenceable(BaseResourceIT.COLUMN_TYPE_BUILTIN, values1);
        Referenceable ref2 = new Referenceable(BaseResourceIT.COLUMN_TYPE_BUILTIN, values2);
        columns.add(ref1);
        columns.add(ref2);
        hiveTableInstance.set("columns", columns);
        LOG.debug("Replacing entity= {}", hiveTableInstance);

        EntityResult updateEntity = atlasClientV1.updateEntities(hiveTableInstance);

        assertNotNull(updateEntity.getUpdateEntities());

        hiveTableInstance = atlasClientV1.getEntity(guid);
        List<Referenceable> refs = (List<Referenceable>) hiveTableInstance.get("columns");
        Assert.assertEquals(refs.size(), 2);

        Referenceable col3 = getReferenceable(refs, "col3");
        Referenceable col4 = getReferenceable(refs, "col4");

        Assert.assertEquals(col3.getValuesMap(), values1);
        Assert.assertEquals(col4.getValuesMap(), values2);
    }

    @Test
    public void testDeleteEntitiesViaRestApi() throws Exception {
        // Create 2 database entities
        Referenceable db1 = new Referenceable(DATABASE_TYPE_BUILTIN);
        String dbName = randomString();
        db1.set(NAME, dbName);
        db1.set(DESCRIPTION, randomString());
        db1.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName);
        db1.set("owner", "user1");
        db1.set(CLUSTER_NAME, "cl1");
        db1.set("parameters", Collections.EMPTY_MAP);
        db1.set("location", "/tmp");
        Id db1Id = createInstance(db1);

        Referenceable db2 = new Referenceable(DATABASE_TYPE_BUILTIN);
        String dbName2 = randomString();
        db2.set(NAME, dbName2);
        db2.set(QUALIFIED_NAME, dbName2);
        db2.set(CLUSTER_NAME, randomString());
        db2.set(DESCRIPTION, randomString());
        db2.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName2);
        db2.set("owner", "user2");
        db2.set(CLUSTER_NAME, "cl1");
        db2.set("parameters", Collections.EMPTY_MAP);
        db2.set("location", "/tmp");
        Id db2Id = createInstance(db2);

        // Delete the database entities
        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add(AtlasClient.GUID.toLowerCase(), db1Id._getId());
        queryParams.add(AtlasClient.GUID.toLowerCase(), db2Id._getId());

        ObjectNode response = atlasClientV1.callAPIWithQueryParams(AtlasClient.API_V1.DELETE_ENTITIES, queryParams);
        List<String> deletedGuidsList = EntityResult.fromString(response.toString()).getDeletedEntities();
        Assert.assertTrue(deletedGuidsList.contains(db1Id._getId()));
        Assert.assertTrue(deletedGuidsList.contains(db2Id._getId()));

        // Verify entities were deleted from the repository.
        for (String guid : deletedGuidsList) {
            Referenceable entity = atlasClientV1.getEntity(guid);
            assertEquals(entity.getId().getState(), Id.EntityState.DELETED);
        }
    }

    @Test
    public void testDeleteEntitiesViaClientApi() throws Exception {
        // Create 2 database entities
        Referenceable db1 = new Referenceable(DATABASE_TYPE_BUILTIN);
        String dbName = randomString();
        db1.set("name", dbName);
        db1.set("description", randomString());
        db1.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName);
        db1.set("owner", "user1");
        db1.set(CLUSTER_NAME, "cl1");
        db1.set("parameters", Collections.EMPTY_MAP);
        db1.set("location", "/tmp");
        Id db1Id = createInstance(db1);
        Referenceable db2 = new Referenceable(DATABASE_TYPE_BUILTIN);
        String dbName2 = randomString();
        db2.set("name", dbName2);
        db2.set(QUALIFIED_NAME, dbName2);
        db2.set(CLUSTER_NAME, randomString());
        db2.set("description", randomString());
        db2.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, dbName2);
        db2.set("owner", "user2");
        db2.set("clusterName", "cl1");
        db2.set("parameters", Collections.EMPTY_MAP);
        db2.set("location", "/tmp");
        Id db2Id = createInstance(db2);

        // Delete the database entities
        List<String> deletedGuidsList = atlasClientV1.deleteEntities(db1Id._getId(), db2Id._getId()).getDeletedEntities();

        // Verify that deleteEntities() response has database entity guids
        Assert.assertEquals(deletedGuidsList.size(), 2);
        Assert.assertTrue(deletedGuidsList.contains(db1Id._getId()));
        Assert.assertTrue(deletedGuidsList.contains(db2Id._getId()));

        // Verify entities were deleted from the repository.
        for (String guid : deletedGuidsList) {
            Referenceable entity = atlasClientV1.getEntity(guid);
            assertEquals(entity.getId().getState(), Id.EntityState.DELETED);
        }
    }

    @Test
    public void testDeleteEntityByUniqAttribute() throws Exception {
        // Create database entity
        Referenceable db1 = new Referenceable(DATABASE_TYPE_BUILTIN);
        String dbName = randomString();
        String qualifiedName = dbName + "@cl1";
        db1.set(NAME, dbName);
        db1.set(QUALIFIED_NAME, qualifiedName);
        db1.set(CLUSTER_NAME, randomString());
        db1.set(DESCRIPTION, randomString());
        db1.set(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qualifiedName);
        db1.set("owner", "user1");
        db1.set(CLUSTER_NAME, "cl1");
        db1.set("parameters", Collections.EMPTY_MAP);
        db1.set("location", "/tmp");
        Id db1Id = createInstance(db1);

        // Delete the database entity
        List<String> deletedGuidsList = atlasClientV1.deleteEntity(DATABASE_TYPE_BUILTIN, QUALIFIED_NAME, qualifiedName).getDeletedEntities();

        // Verify that deleteEntities() response has database entity guids
        Assert.assertEquals(deletedGuidsList.size(), 1);
        Assert.assertTrue(deletedGuidsList.contains(db1Id._getId()));

        // Verify entities were deleted from the repository.
        for (String guid : deletedGuidsList) {
            Referenceable entity = atlasClientV1.getEntity(guid);
            assertEquals(entity.getId().getState(), Id.EntityState.DELETED);
        }
    }

    private Referenceable getReferenceable(List<Referenceable> refs, String name) {
        Referenceable ret = null;

        for (Referenceable ref : refs) {
            Map<String, Object> values     = ref.getValuesMap();
            String              entityName = (String) values.get("name");

            if (StringUtils.equalsIgnoreCase(name, entityName)) {
                ret = ref;
                break;
            }
        }

        return ret;
    }
}
