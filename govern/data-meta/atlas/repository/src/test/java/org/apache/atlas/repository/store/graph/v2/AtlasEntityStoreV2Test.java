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
package org.apache.atlas.repository.store.graph.v2;

import com.google.common.collect.ImmutableSet;
import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.GraphTransactionInterceptor;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.bulkimport.BulkImportResponse;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasStruct;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.instance.EntityMutations;
import org.apache.atlas.model.instance.EntityMutations.EntityOperation;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.util.FileUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.apache.atlas.AtlasConfiguration.STORE_DIFFERENTIAL_AUDITS;
import static org.apache.atlas.AtlasErrorCode.INVALID_CUSTOM_ATTRIBUTE_KEY_CHARACTERS;
import static org.apache.atlas.AtlasErrorCode.INVALID_CUSTOM_ATTRIBUTE_KEY_LENGTH;
import static org.apache.atlas.AtlasErrorCode.INVALID_CUSTOM_ATTRIBUTE_VALUE;
import static org.apache.atlas.AtlasErrorCode.INVALID_LABEL_CHARACTERS;
import static org.apache.atlas.AtlasErrorCode.INVALID_LABEL_LENGTH;
import static org.apache.atlas.TestUtilsV2.COLUMNS_ATTR_NAME;
import static org.apache.atlas.TestUtilsV2.COLUMN_TYPE;
import static org.apache.atlas.TestUtilsV2.NAME;
import static org.apache.atlas.TestUtilsV2.TABLE_TYPE;
import static org.apache.atlas.TestUtilsV2.getFile;
import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasEntityStoreV2Test extends AtlasEntityTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasEntityStoreV2Test.class);
    public static final String CSV_FILES   = "/csvFiles/";

    private AtlasEntitiesWithExtInfo deptEntity;
    private AtlasEntityWithExtInfo   dbEntity;
    private AtlasEntityWithExtInfo   tblEntity;
    private AtlasEntityWithExtInfo   nestedCollectionAttrEntity;
    private AtlasEntityWithExtInfo   primitiveEntity;

    AtlasEntityChangeNotifier mockChangeNotifier = mock(AtlasEntityChangeNotifier.class);

    @Inject
    private EntityGraphMapper graphMapper;

    @Inject
    private String dbEntityGuid;
    private String tblEntityGuid;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        AtlasTypesDef[] testTypesDefs = new AtlasTypesDef[] { TestUtilsV2.defineDeptEmployeeTypes(),
                                                              TestUtilsV2.defineHiveTypes(),
                                                              TestUtilsV2.defineTypeWithNestedCollectionAttributes(),
                                                              TestUtilsV2.defineEnumTypes(),
                                                              TestUtilsV2.defineBusinessMetadataTypes()
                                                            };
        createTypesDef(testTypesDefs);

        deptEntity                 = TestUtilsV2.createDeptEg2();
        dbEntity                   = TestUtilsV2.createDBEntityV2();
        tblEntity                  = TestUtilsV2.createTableEntityV2(dbEntity.getEntity());
        nestedCollectionAttrEntity = TestUtilsV2.createNestedCollectionAttrEntity();
        primitiveEntity            = TestUtilsV2.createprimitiveEntityV2();

        AtlasTypesDef typesDef11         = new AtlasTypesDef();
        List          primitiveEntityDef = new ArrayList<AtlasEntityDef>();

        primitiveEntityDef.add(TestUtilsV2.createPrimitiveEntityDef());
        typesDef11.setEntityDefs(primitiveEntityDef);

        typeDefStore.createTypesDef(typesDef11);
    }
    @BeforeTest
    public void init() throws Exception {
        entityStore = new AtlasEntityStoreV2(graph, deleteDelegate, typeRegistry, mockChangeNotifier, graphMapper);
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);

        LOG.debug("RequestContext: activeCount={}, earliestActiveRequestTime={}", RequestContext.getActiveRequestsCount(), RequestContext.earliestActiveRequestTime());
    }

    @Test
    public void testDefaultValueForPrimitiveTypes() throws Exception  {
        init();

        EntityMutationResponse  response                   = entityStore.createOrUpdate(new AtlasEntityStream(primitiveEntity), false);
        List<AtlasEntityHeader> entitiesCreatedResponse    = response.getEntitiesByOperation(EntityOperation.CREATE);
        List<AtlasEntityHeader> entitiesCreatedwithdefault = response.getMutatedEntities().get(EntityOperation.CREATE);
        AtlasEntity             entityCreated              = getEntityFromStore(entitiesCreatedResponse.get(0));

        Map     attributesMap = entityCreated.getAttributes();
        String  description   = (String) attributesMap.get("description");
        String  check         = (String) attributesMap.get("check");
        String  sourceCode    = (String) attributesMap.get("sourcecode");
        float   diskUsage     = (float) attributesMap.get("diskUsage");
        boolean isstoreUse    = (boolean) attributesMap.get("isstoreUse");
        int     cost          = (int) attributesMap.get("Cost");

        assertEquals(description,"test");
        assertEquals(check,"check");

        //defaultValue
        assertEquals(diskUsage,70.5f);
        assertEquals(isstoreUse,true);
        assertEquals(sourceCode,"Hello World");
        assertEquals(cost,30);
    }

    @Test(priority = -1)
    public void testCreate() throws Exception {
        init();

        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(deptEntity), false);
        validateMutationResponse(response, EntityOperation.CREATE, 5);

        AtlasEntityHeader dept1 = response.getFirstCreatedEntityByTypeName(TestUtilsV2.DEPARTMENT_TYPE);
        validateEntity(deptEntity, getEntityFromStore(dept1), deptEntity.getEntities().get(0));

        final Map<EntityOperation, List<AtlasEntityHeader>> entitiesMutated = response.getMutatedEntities();
        List<AtlasEntityHeader> entitiesCreated = entitiesMutated.get(EntityOperation.CREATE);

        assertTrue(entitiesCreated.size() >= deptEntity.getEntities().size());

        for (int i = 0; i < deptEntity.getEntities().size(); i++) {
            AtlasEntity expected = deptEntity.getEntities().get(i);
            AtlasEntity actual   = getEntityFromStore(entitiesCreated.get(i));

            validateEntity(deptEntity, actual, expected);
        }

        //Create DB
        init();
        EntityMutationResponse dbCreationResponse = entityStore.createOrUpdate(new AtlasEntityStream(dbEntity), false);
        validateMutationResponse(dbCreationResponse, EntityOperation.CREATE, 1);
        dbEntityGuid = dbCreationResponse.getCreatedEntities().get(0).getGuid();

        AtlasEntityHeader db1 = dbCreationResponse.getFirstCreatedEntityByTypeName(TestUtilsV2.DATABASE_TYPE);
        validateEntity(dbEntity, getEntityFromStore(db1));

        //Create Table
        //Update DB guid
        AtlasObjectId dbObjectId = (AtlasObjectId) tblEntity.getEntity().getAttribute("database");
        dbObjectId.setGuid(db1.getGuid());
        tblEntity.addReferredEntity(dbEntity.getEntity());

        init();
        EntityMutationResponse tableCreationResponse = entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);
        validateMutationResponse(tableCreationResponse, EntityOperation.CREATE, 1);
        tblEntityGuid = tableCreationResponse.getCreatedEntities().get(0).getGuid();

        AtlasEntityHeader tableEntity = tableCreationResponse.getFirstCreatedEntityByTypeName(TABLE_TYPE);
        validateEntity(tblEntity, getEntityFromStore(tableEntity));

        //Create nested-collection attribute entity
        init();
        EntityMutationResponse entityMutationResponse = entityStore.createOrUpdate(new AtlasEntityStream(nestedCollectionAttrEntity), false);
        validateMutationResponse(entityMutationResponse, EntityOperation.CREATE, 1);

        AtlasEntityHeader createdEntity = entityMutationResponse.getFirstCreatedEntityByTypeName(TestUtilsV2.ENTITY_TYPE_WITH_NESTED_COLLECTION_ATTR);
        validateEntity(nestedCollectionAttrEntity, getEntityFromStore(createdEntity));
    }

    @Test(dependsOnMethods = "testCreate")
    public void testArrayOfEntityUpdate() throws Exception {
        AtlasEntity              tableEntity  = new AtlasEntity(tblEntity.getEntity());
        List<AtlasObjectId>      columns      = new ArrayList<>();
        AtlasEntitiesWithExtInfo entitiesInfo = new AtlasEntitiesWithExtInfo(tableEntity);

        AtlasEntity col1 = TestUtilsV2.createColumnEntity(tableEntity);
        col1.setAttribute(NAME, "col1");

        AtlasEntity col2 = TestUtilsV2.createColumnEntity(tableEntity);
        col2.setAttribute(NAME, "col2");

        columns.add(AtlasTypeUtil.getAtlasObjectId(col1));
        columns.add(AtlasTypeUtil.getAtlasObjectId(col2));

        tableEntity.setAttribute(COLUMNS_ATTR_NAME, columns);

        entitiesInfo.addReferredEntity(dbEntity.getEntity());
        entitiesInfo.addReferredEntity(col1);
        entitiesInfo.addReferredEntity(col2);

        init();
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);

        AtlasEntityHeader updatedTableHeader = response.getFirstUpdatedEntityByTypeName(tableEntity.getTypeName());
        validateEntity(entitiesInfo, getEntityFromStore(updatedTableHeader));

        //Complete update. Add  array elements - col3,col4
        AtlasEntity col3 = TestUtilsV2.createColumnEntity(tableEntity);
        col3.setAttribute(NAME, "col3");

        AtlasEntity col4 = TestUtilsV2.createColumnEntity(tableEntity);
        col4.setAttribute(NAME, "col4");

        columns.add(AtlasTypeUtil.getAtlasObjectId(col3));
        columns.add(AtlasTypeUtil.getAtlasObjectId(col4));
        tableEntity.setAttribute(COLUMNS_ATTR_NAME, columns);

        entitiesInfo.addReferredEntity(col3);
        entitiesInfo.addReferredEntity(col4);

        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);

        updatedTableHeader = response.getFirstUpdatedEntityByTypeName(tableEntity.getTypeName());
        validateEntity(entitiesInfo, getEntityFromStore(updatedTableHeader));

        //Swap elements
        columns.clear();
        columns.add(AtlasTypeUtil.getAtlasObjectId(col4));
        columns.add(AtlasTypeUtil.getAtlasObjectId(col3));
        tableEntity.setAttribute(COLUMNS_ATTR_NAME, columns);

        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);

        updatedTableHeader = response.getFirstUpdatedEntityByTypeName(tableEntity.getTypeName());
        AtlasEntity updatedEntity = getEntityFromStore(updatedTableHeader);
        // deleted columns are also included in "columns" attribute
        Assert.assertTrue(((List<AtlasObjectId>) updatedEntity.getAttribute(COLUMNS_ATTR_NAME)).size() >= 2);

        assertEquals(response.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE).size(), 2);  // col1, col2 are deleted

        //Update array column to null
        tableEntity.setAttribute(COLUMNS_ATTR_NAME, null);

        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);

        updatedTableHeader = response.getFirstUpdatedEntityByTypeName(tableEntity.getTypeName());
        validateEntity(entitiesInfo, getEntityFromStore(updatedTableHeader));
        assertEquals(response.getEntitiesByOperation(EntityMutations.EntityOperation.DELETE).size(), 2);
    }

    @Test(dependsOnMethods = "testCreate")
    public void testUpdateEntityWithMap() throws Exception {
        AtlasEntity              tableEntity  = new AtlasEntity(tblEntity.getEntity());
        AtlasEntitiesWithExtInfo entitiesInfo = new AtlasEntitiesWithExtInfo(tableEntity);
        Map<String, AtlasStruct> partsMap     = new HashMap<>();
        partsMap.put("part0", new AtlasStruct(TestUtilsV2.PARTITION_STRUCT_TYPE, NAME, "test"));

        tableEntity.setAttribute("partitionsMap", partsMap);

        init();
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);

        AtlasEntityHeader tableDefinition1 = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        AtlasEntity updatedTableDef1 = getEntityFromStore(tableDefinition1);
        validateEntity(entitiesInfo, updatedTableDef1);

        Assert.assertTrue(partsMap.get("part0").equals(((Map<String, AtlasStruct>) updatedTableDef1.getAttribute("partitionsMap")).get("part0")));

        //update map - add a map key
        partsMap.put("part1", new AtlasStruct(TestUtilsV2.PARTITION_STRUCT_TYPE, NAME, "test1"));
        tableEntity.setAttribute("partitionsMap", partsMap);

        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);

        AtlasEntityHeader tableDefinition2 = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        AtlasEntity updatedTableDef2 = getEntityFromStore(tableDefinition2);
        validateEntity(entitiesInfo, updatedTableDef2);

        assertEquals(((Map<String, AtlasStruct>) updatedTableDef2.getAttribute("partitionsMap")).size(), 2);
        Assert.assertTrue(partsMap.get("part1").equals(((Map<String, AtlasStruct>) updatedTableDef2.getAttribute("partitionsMap")).get("part1")));

        //update map - remove a key and add another key
        partsMap.remove("part0");
        partsMap.put("part2", new AtlasStruct(TestUtilsV2.PARTITION_STRUCT_TYPE, NAME, "test2"));
        tableEntity.setAttribute("partitionsMap", partsMap);

        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);

        AtlasEntityHeader tableDefinition3 = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        AtlasEntity updatedTableDef3 = getEntityFromStore(tableDefinition3);
        validateEntity(entitiesInfo, updatedTableDef3);

        assertEquals(((Map<String, AtlasStruct>) updatedTableDef3.getAttribute("partitionsMap")).size(), 2);
        Assert.assertNull(((Map<String, AtlasStruct>) updatedTableDef3.getAttribute("partitionsMap")).get("part0"));
        Assert.assertTrue(partsMap.get("part2").equals(((Map<String, AtlasStruct>) updatedTableDef3.getAttribute("partitionsMap")).get("part2")));

        //update struct value for existing map key
        AtlasStruct partition2 = partsMap.get("part2");
        partition2.setAttribute(NAME, "test2Updated");

        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);

        AtlasEntityHeader tableDefinition4 = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        AtlasEntity updatedTableDef4 = getEntityFromStore(tableDefinition4);
        validateEntity(entitiesInfo, updatedTableDef4);

        assertEquals(((Map<String, AtlasStruct>) updatedTableDef4.getAttribute("partitionsMap")).size(), 2);
        Assert.assertNull(((Map<String, AtlasStruct>) updatedTableDef4.getAttribute("partitionsMap")).get("part0"));
        Assert.assertTrue(partsMap.get("part2").equals(((Map<String, AtlasStruct>) updatedTableDef4.getAttribute("partitionsMap")).get("part2")));

        //Test map pointing to a class

        AtlasEntity col0 = new AtlasEntity(COLUMN_TYPE, NAME, "test1");
        col0.setAttribute("type", "string");
        col0.setAttribute("table", AtlasTypeUtil.getAtlasObjectId(tableEntity));

        AtlasEntityWithExtInfo col0WithExtendedInfo = new AtlasEntityWithExtInfo(col0);
        col0WithExtendedInfo.addReferredEntity(tableEntity);
        col0WithExtendedInfo.addReferredEntity(dbEntity.getEntity());

        init();
        entityStore.createOrUpdate(new AtlasEntityStream(col0WithExtendedInfo), false);

        AtlasEntity col1 = new AtlasEntity(COLUMN_TYPE, NAME, "test2");
        col1.setAttribute("type", "string");
        col1.setAttribute("table", AtlasTypeUtil.getAtlasObjectId(tableEntity));

        AtlasEntityWithExtInfo col1WithExtendedInfo = new AtlasEntityWithExtInfo(col1);
        col1WithExtendedInfo.addReferredEntity(tableEntity);
        col1WithExtendedInfo.addReferredEntity(dbEntity.getEntity());

        init();
        entityStore.createOrUpdate(new AtlasEntityStream(col1WithExtendedInfo), false);

        Map<String, AtlasObjectId> columnsMap = new HashMap<String, AtlasObjectId>();
        columnsMap.put("col0", AtlasTypeUtil.getAtlasObjectId(col0));
        columnsMap.put("col1", AtlasTypeUtil.getAtlasObjectId(col1));

        tableEntity.setAttribute(TestUtilsV2.COLUMNS_MAP, columnsMap);

        entitiesInfo.addReferredEntity(col0);
        entitiesInfo.addReferredEntity(col1);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        AtlasEntityHeader tableDefinition5 = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(tableDefinition5));

        //Swap elements
        columnsMap.clear();
        columnsMap.put("col0", AtlasTypeUtil.getAtlasObjectId(col1));
        columnsMap.put("col1", AtlasTypeUtil.getAtlasObjectId(col0));

        tableEntity.setAttribute(TestUtilsV2.COLUMNS_MAP, columnsMap);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        AtlasEntityHeader tableDefinition6 = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(tableDefinition6));

        Assert.assertEquals(entityStore.getById(col0.getGuid()).getEntity().getStatus(), AtlasEntity.Status.ACTIVE);
        Assert.assertEquals(entityStore.getById(col1.getGuid()).getEntity().getStatus(), AtlasEntity.Status.ACTIVE);

        //Drop the first key and change the class type as well to col0
        columnsMap.clear();
        columnsMap.put("col0", AtlasTypeUtil.getAtlasObjectId(col0));
        tableEntity.setAttribute(TestUtilsV2.COLUMNS_MAP, columnsMap);
        init();

        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        AtlasEntityHeader tableDefinition7 = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(tableDefinition7));

        //Clear state
        tableEntity.setAttribute(TestUtilsV2.COLUMNS_MAP, null);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        AtlasEntityHeader tableDefinition8 = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(tableDefinition8));
    }

    @Test(dependsOnMethods = "testCreate")
    public void testMapOfPrimitivesUpdate() throws Exception {
        AtlasEntity              tableEntity  = new AtlasEntity(tblEntity.getEntity());
        AtlasEntitiesWithExtInfo entitiesInfo = new AtlasEntitiesWithExtInfo(tableEntity);

        entitiesInfo.addReferredEntity(tableEntity);

        //Add a new entry
        Map<String, String> paramsMap = (Map<String, String>) tableEntity.getAttribute("parametersMap");
        paramsMap.put("newParam", "value");
        init();
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        validateMutationResponse(response, EntityMutations.EntityOperation.UPDATE, 1);
        AtlasEntityHeader updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));

        //Remove an entry
        paramsMap.remove("key1");
        tableEntity.setAttribute("parametersMap", paramsMap);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        validateMutationResponse(response, EntityMutations.EntityOperation.UPDATE, 1);
        updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));
    }

    @Test(dependsOnMethods = "testCreate")
    public void testArrayOfStructs() throws Exception {
        //Modify array of structs
        AtlasEntity              tableEntity  = new AtlasEntity(tblEntity.getEntity());
        AtlasEntitiesWithExtInfo entitiesInfo = new AtlasEntitiesWithExtInfo(tableEntity);

        List<AtlasStruct> partitions = new ArrayList<AtlasStruct>(){{
            add(new AtlasStruct(TestUtilsV2.PARTITION_STRUCT_TYPE, NAME, "part1"));
            add(new AtlasStruct(TestUtilsV2.PARTITION_STRUCT_TYPE, NAME, "part2"));
        }};
        tableEntity.setAttribute("partitions", partitions);

        init();
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        AtlasEntityHeader updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));

        //add a new element to array of struct
        partitions.add(new AtlasStruct(TestUtilsV2.PARTITION_STRUCT_TYPE, NAME, "part3"));
        tableEntity.setAttribute("partitions", partitions);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));

        //remove one of the struct values
        partitions.remove(1);
        tableEntity.setAttribute("partitions", partitions);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));

        //Update struct value within array of struct
        partitions.get(0).setAttribute(NAME, "part4");
        tableEntity.setAttribute("partitions", partitions);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));


        //add a repeated element to array of struct
        partitions.add(new AtlasStruct(TestUtilsV2.PARTITION_STRUCT_TYPE, NAME, "part4"));
        tableEntity.setAttribute("partitions", partitions);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));

        // Remove all elements. Should set array attribute to null
        partitions.clear();
        tableEntity.setAttribute("partitions", partitions);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));
    }


    @Test(dependsOnMethods = "testCreate")
    public void testStructs() throws Exception {
        AtlasEntity              databaseEntity = dbEntity.getEntity();
        AtlasEntity              tableEntity    = new AtlasEntity(tblEntity.getEntity());
        AtlasEntitiesWithExtInfo entitiesInfo   = new AtlasEntitiesWithExtInfo(tableEntity);

        AtlasStruct serdeInstance = new AtlasStruct(TestUtilsV2.SERDE_TYPE, NAME, "serde1Name");
        serdeInstance.setAttribute("serde", "test");
        serdeInstance.setAttribute("description", "testDesc");
        tableEntity.setAttribute("serde1", serdeInstance);
        tableEntity.setAttribute("database", new AtlasObjectId(databaseEntity.getTypeName(), NAME, databaseEntity.getAttribute(NAME)));

        init();
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        AtlasEntityHeader updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));

        //update struct attribute
        serdeInstance.setAttribute("serde", "testUpdated");
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));

        //set to null
        tableEntity.setAttribute("description", null);
        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        updatedTable = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        Object updatedDescription = ObjectUtils.defaultIfNull(updatedTable.getAttribute("description"), StringUtils.EMPTY);
        Assert.assertTrue(StringUtils.isEmpty(updatedDescription.toString()));
        validateEntity(entitiesInfo, getEntityFromStore(updatedTable));
    }

    @Test(dependsOnMethods = "testCreate")
    public void testClassUpdate() throws Exception {

        init();
        //Create new db instance
        final AtlasEntity databaseInstance = TestUtilsV2.createDBEntity();

        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(databaseInstance), false);
        final AtlasEntityHeader dbCreated = response.getFirstCreatedEntityByTypeName(TestUtilsV2.DATABASE_TYPE);

        init();
        Map<String, AtlasEntity> tableCloneMap = new HashMap<>();
        AtlasEntity tableClone = new AtlasEntity(tblEntity.getEntity());
        tableClone.setAttribute("database", new AtlasObjectId(dbCreated.getGuid(), TestUtilsV2.DATABASE_TYPE));

        tableCloneMap.put(dbCreated.getGuid(), databaseInstance);
        tableCloneMap.put(tableClone.getGuid(), tableClone);

        response = entityStore.createOrUpdate(new InMemoryMapEntityStream(tableCloneMap), false);
        final AtlasEntityHeader tableDefinition = response.getFirstUpdatedEntityByTypeName(TABLE_TYPE);
        AtlasEntity updatedTableDefinition = getEntityFromStore(tableDefinition);
        assertNotNull(updatedTableDefinition.getAttribute("database"));
        Assert.assertEquals(((AtlasObjectId) updatedTableDefinition.getAttribute("database")).getGuid(), dbCreated.getGuid());
    }

    @Test
    public void testCheckOptionalAttrValueRetention() throws Exception {

        AtlasEntity dbEntity = TestUtilsV2.createDBEntity();

        init();
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(dbEntity), false);
        AtlasEntity firstEntityCreated = getEntityFromStore(response.getFirstCreatedEntityByTypeName(TestUtilsV2.DATABASE_TYPE));

        //The optional boolean attribute should have a non-null value
        final String isReplicatedAttr = "isReplicated";
        final String paramsAttr = "parameters";
        assertNotNull(firstEntityCreated.getAttribute(isReplicatedAttr));
        Assert.assertEquals(firstEntityCreated.getAttribute(isReplicatedAttr), Boolean.FALSE);
        Assert.assertNull(firstEntityCreated.getAttribute(paramsAttr));

        //Update to true
        dbEntity.setAttribute(isReplicatedAttr, Boolean.TRUE);
        //Update array
        final HashMap<String, String> params = new HashMap<String, String>() {{ put("param1", "val1"); put("param2", "val2"); }};
        dbEntity.setAttribute(paramsAttr, params);
        //Complete update

        init();
        response = entityStore.createOrUpdate(new AtlasEntityStream(dbEntity), false);
        AtlasEntity firstEntityUpdated = getEntityFromStore(response.getFirstUpdatedEntityByTypeName(TestUtilsV2.DATABASE_TYPE));

        assertNotNull(firstEntityUpdated);
        assertNotNull(firstEntityUpdated.getAttribute(isReplicatedAttr));
        Assert.assertEquals(firstEntityUpdated.getAttribute(isReplicatedAttr), Boolean.TRUE);
        Assert.assertEquals(firstEntityUpdated.getAttribute(paramsAttr), params);

        //TODO - enable test after GET API is ready
//        init();
//        //Complete update without setting the attribute
//        AtlasEntity newEntity = TestUtilsV2.createDBEntity();
//        //Reset name to the current DB name
//        newEntity.setAttribute(AtlasClient.NAME, firstEntityCreated.getAttribute(AtlasClient.NAME));
//        response = entityStore.createOrUpdate(newEntity);
//
//        firstEntityUpdated = response.getFirstEntityUpdated();
//        Assert.assertNotNull(firstEntityUpdated.getAttribute(isReplicatedAttr));
//        Assert.assertEquals(firstEntityUpdated.getAttribute(isReplicatedAttr), Boolean.TRUE);
//        Assert.assertEquals(firstEntityUpdated.getAttribute(paramsAttr), params);
    }

    @Test(enabled = false)
    //Titan doesn't allow some reserved chars in property keys. Verify that atlas encodes these
    //See GraphHelper.encodePropertyKey()
    //TODO : Failing in typedef creation
    public void testSpecialCharacters() throws Exception {
        //Verify that type can be created with reserved characters in typename, attribute name
        final String typeName = TestUtilsV2.randomString(10);
        String strAttrName = randomStrWithReservedChars();
        String arrayAttrName = randomStrWithReservedChars();
        String mapAttrName = randomStrWithReservedChars();

        AtlasEntityDef typeDefinition =
            AtlasTypeUtil.createClassTypeDef(typeName, "Special chars test type", ImmutableSet.<String>of(),
                AtlasTypeUtil.createOptionalAttrDef(strAttrName, "string"),
                AtlasTypeUtil.createOptionalAttrDef(arrayAttrName, "array<string>"),
                AtlasTypeUtil.createOptionalAttrDef(mapAttrName, "map<string,string>"));

        AtlasTypesDef atlasTypesDef = new AtlasTypesDef(null, null, null, Arrays.asList(typeDefinition));
        typeDefStore.createTypesDef(atlasTypesDef);

        //verify that entity can be created with reserved characters in string value, array value and map key and value
        AtlasEntity entity = new AtlasEntity();
        entity.setAttribute(strAttrName, randomStrWithReservedChars());
        entity.setAttribute(arrayAttrName, new String[]{randomStrWithReservedChars()});
        entity.setAttribute(mapAttrName, new HashMap<String, String>() {{
            put(randomStrWithReservedChars(), randomStrWithReservedChars());
        }});

        AtlasEntityWithExtInfo entityWithExtInfo = new AtlasEntityWithExtInfo(entity);

        final EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entityWithExtInfo), false);
        final AtlasEntityHeader firstEntityCreated = response.getFirstEntityCreated();
        validateEntity(entityWithExtInfo, getEntityFromStore(firstEntityCreated));


        //Verify that search with reserved characters works - for string attribute
//        String query =
//            String.format("`%s` where `%s` = '%s'", typeName, strAttrName, entity.getAttribute(strAttrName));
//        String responseJson = discoveryService.searchByDSL(query, new QueryParams(1, 0));
//        JSONObject response = new JSONObject(responseJson);
//        assertEquals(response.getJSONArray("rows").length(), 1);
    }

    @Test(expectedExceptions = AtlasBaseException.class)
    public void testCreateRequiredAttrNull() throws Exception {
        //Update required attribute
        Map<String, AtlasEntity> tableCloneMap = new HashMap<>();
        AtlasEntity tableEntity = new AtlasEntity(TABLE_TYPE);
        tableEntity.setAttribute(NAME, "table_" + TestUtilsV2.randomString());
        tableCloneMap.put(tableEntity.getGuid(), tableEntity);

        entityStore.createOrUpdate(new InMemoryMapEntityStream(tableCloneMap), false);
        Assert.fail("Expected exception while creating with required attribute null");
    }

    @Test
    public void testPartialUpdateAttr() throws Exception {
        //Update optional attribute

        init();

        AtlasEntity dbEntity = new AtlasEntity(TestUtilsV2.DATABASE_TYPE);
        dbEntity.setAttribute("name", TestUtilsV2.randomString(10));
        dbEntity.setAttribute("description", "us db");
        dbEntity.setAttribute("isReplicated", false);
        dbEntity.setAttribute("created", "09081988");
        dbEntity.setAttribute("namespace", "db namespace");
        dbEntity.setAttribute("cluster", "Fenton_Cluster");
        dbEntity.setAttribute("colo", "10001");

        EntityStream           dbStream        = new AtlasEntityStream(new AtlasEntitiesWithExtInfo(dbEntity));
        EntityMutationResponse response        = entityStore.createOrUpdate(dbStream, false);
        AtlasEntityHeader      dbHeader        = response.getFirstEntityCreated();
        AtlasEntity            createdDbEntity = getEntityFromStore(dbHeader);

        // update the db entity
        dbEntity = new AtlasEntity(TestUtilsV2.DATABASE_TYPE);
        dbEntity.setGuid(createdDbEntity.getGuid());
        // dbEntity.setAttribute("name", createdDbEntity.getAttribute("name"));
        // dbEntity.setAttribute("description", "another db"); // required attr
        dbEntity.setAttribute("created", "08151947");       // optional attr
        dbEntity.setAttribute("isReplicated", true);       // optional attr

        dbStream = new AtlasEntityStream(new AtlasEntitiesWithExtInfo(dbEntity));

        // fail full update if required attributes are not specified.
        try {
            entityStore.createOrUpdate(dbStream, false);
        } catch (AtlasBaseException ex) {
            Assert.assertEquals(ex.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_CRUD_INVALID_PARAMS);
        }

        // do partial update without providing required attributes
        dbStream.reset();
        response = entityStore.createOrUpdate(dbStream, true);
        dbHeader = response.getFirstEntityPartialUpdated();
        AtlasEntity updatedDbEntity = getEntityFromStore(dbHeader);

        assertEquals(updatedDbEntity.getAttribute("name"), createdDbEntity.getAttribute("name"));
        assertEquals(updatedDbEntity.getAttribute("description"), createdDbEntity.getAttribute("description"));
        assertEquals(updatedDbEntity.getAttribute("isReplicated"), true);
        assertEquals(updatedDbEntity.getAttribute("created"), "08151947");
        assertEquals(updatedDbEntity.getAttribute("namespace"), createdDbEntity.getAttribute("namespace"));
        assertEquals(updatedDbEntity.getAttribute("cluster"), createdDbEntity.getAttribute("cluster"));
        assertEquals(updatedDbEntity.getAttribute("colo"), createdDbEntity.getAttribute("colo"));

        // create a new table type
        AtlasEntity tblEntity = new AtlasEntity(TABLE_TYPE);
        tblEntity.setAttribute("name", TestUtilsV2.randomString(10));
        tblEntity.setAttribute("type", "type");
        tblEntity.setAttribute("tableType", "MANAGED");
        tblEntity.setAttribute("database", AtlasTypeUtil.getAtlasObjectId(updatedDbEntity));

        // create new column entity
        AtlasEntity col1 = TestUtilsV2.createColumnEntity(tblEntity);
        AtlasEntity col2 = TestUtilsV2.createColumnEntity(tblEntity);
        col1.setAttribute(NAME, "col1");
        col2.setAttribute(NAME, "col2");

        List<AtlasObjectId> columns = new ArrayList<>();
        columns.add(AtlasTypeUtil.getAtlasObjectId(col1));
        columns.add(AtlasTypeUtil.getAtlasObjectId(col2));

        tblEntity.setAttribute(COLUMNS_ATTR_NAME, columns);

        AtlasEntitiesWithExtInfo tableEntityInfo = new AtlasEntitiesWithExtInfo(tblEntity);
        tableEntityInfo.addReferredEntity(col1.getGuid(), col1);
        tableEntityInfo.addReferredEntity(col2.getGuid(), col2);

        EntityStream tblStream = new AtlasEntityStream(tableEntityInfo);
        response = entityStore.createOrUpdate(tblStream, false);
        AtlasEntityHeader tblHeader = response.getFirstEntityCreated();
        AtlasEntity createdTblEntity = getEntityFromStore(tblHeader);

        columns = (List<AtlasObjectId>) createdTblEntity.getAttribute(COLUMNS_ATTR_NAME);
        assertEquals(columns.size(), 2);

        // update - add 2 more columns to table
        AtlasEntity col3 = TestUtilsV2.createColumnEntity(createdTblEntity);
        col3.setAttribute(NAME, "col3");
        col3.setAttribute("description", "description col3");

        AtlasEntity col4 = TestUtilsV2.createColumnEntity(createdTblEntity);
        col4.setAttribute(NAME, "col4");
        col4.setAttribute("description", "description col4");

        columns.clear();
        columns.add(AtlasTypeUtil.getAtlasObjectId(col3));
        columns.add(AtlasTypeUtil.getAtlasObjectId(col4));

        tblEntity = new AtlasEntity(TABLE_TYPE);
        tblEntity.setGuid(createdTblEntity.getGuid());
        tblEntity.setAttribute(COLUMNS_ATTR_NAME, columns);

        tableEntityInfo = new AtlasEntitiesWithExtInfo(tblEntity);
        tableEntityInfo.addReferredEntity(col3.getGuid(), col3);
        tableEntityInfo.addReferredEntity(col4.getGuid(), col4);

        tblStream = new AtlasEntityStream(tableEntityInfo);
        response  = entityStore.createOrUpdate(tblStream, true);
        tblHeader = response.getFirstEntityPartialUpdated();
        AtlasEntity updatedTblEntity = getEntityFromStore(tblHeader);

        columns = (List<AtlasObjectId>) updatedTblEntity.getAttribute(COLUMNS_ATTR_NAME);
        // deleted columns are included in the attribute; hence use >=
        assertTrue(columns.size() >= 2);
    }

    @Test
    public void testPartialUpdateArrayAttr() throws Exception {
        // Create a table entity, with 3 reference column entities
        init();
        final AtlasEntity      dbEntity           = TestUtilsV2.createDBEntity();
        EntityMutationResponse dbCreationResponse = entityStore.createOrUpdate(new AtlasEntityStream(dbEntity), false);

        final AtlasEntity        tableEntity      = TestUtilsV2.createTableEntity(dbEntity);
        AtlasEntitiesWithExtInfo entitiesInfo     = new AtlasEntitiesWithExtInfo(tableEntity);

        final AtlasEntity columnEntity1 = TestUtilsV2.createColumnEntity(tableEntity);
        columnEntity1.setAttribute("description", "desc for col1");
        entitiesInfo.addReferredEntity(columnEntity1);

        final AtlasEntity columnEntity2 = TestUtilsV2.createColumnEntity(tableEntity);
        columnEntity2.setAttribute("description", "desc for col2");
        entitiesInfo.addReferredEntity(columnEntity2);

        final AtlasEntity columnEntity3 = TestUtilsV2.createColumnEntity(tableEntity);
        columnEntity3.setAttribute("description", "desc for col3");
        entitiesInfo.addReferredEntity(columnEntity3);

        tableEntity.setAttribute(COLUMNS_ATTR_NAME, Arrays.asList(AtlasTypeUtil.getAtlasObjectId(columnEntity1),
                                                                  AtlasTypeUtil.getAtlasObjectId(columnEntity2),
                                                                  AtlasTypeUtil.getAtlasObjectId(columnEntity3)));

        init();

        final EntityMutationResponse tblCreationResponse = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), false);
        final AtlasEntityHeader      createdTblHeader    = tblCreationResponse.getCreatedEntityByTypeNameAndAttribute(TABLE_TYPE, NAME, (String) tableEntity.getAttribute(NAME));
        final AtlasEntity            createdTblEntity    = getEntityFromStore(createdTblHeader);

        final AtlasEntityHeader column1Created = tblCreationResponse.getCreatedEntityByTypeNameAndAttribute(COLUMN_TYPE, NAME, (String) columnEntity1.getAttribute(NAME));
        final AtlasEntityHeader column2Created = tblCreationResponse.getCreatedEntityByTypeNameAndAttribute(COLUMN_TYPE, NAME, (String) columnEntity2.getAttribute(NAME));
        final AtlasEntityHeader column3Created = tblCreationResponse.getCreatedEntityByTypeNameAndAttribute(COLUMN_TYPE, NAME, (String) columnEntity3.getAttribute(NAME));

        // update only description attribute of all 3 columns
        AtlasEntity col1 = new AtlasEntity(COLUMN_TYPE);
        col1.setGuid(column1Created.getGuid());
        col1.setAttribute("description", "desc for col1:updated");

        AtlasEntity col2 = new AtlasEntity(COLUMN_TYPE);
        col2.setGuid(column2Created.getGuid());
        col2.setAttribute("description", "desc for col2:updated");

        AtlasEntity col3 = new AtlasEntity(COLUMN_TYPE);
        col3.setGuid(column3Created.getGuid());
        col3.setAttribute("description", "desc for col3:updated");

        final AtlasEntity tableEntity1 = new AtlasEntity(TABLE_TYPE);
        tableEntity1.setGuid(createdTblHeader.getGuid());
        tableEntity1.setAttribute(COLUMNS_ATTR_NAME, Arrays.asList(AtlasTypeUtil.getAtlasObjectId(col1),
                AtlasTypeUtil.getAtlasObjectId(col2),
                AtlasTypeUtil.getAtlasObjectId(col3)));
        AtlasEntitiesWithExtInfo tableInfo = new AtlasEntitiesWithExtInfo(tableEntity1);
        tableInfo.addReferredEntity(col1.getGuid(), col1);
        tableInfo.addReferredEntity(col2.getGuid(), col2);
        tableInfo.addReferredEntity(col3.getGuid(), col3);

        init();

        final EntityMutationResponse tblUpdateResponse = entityStore.createOrUpdate(new AtlasEntityStream(tableInfo), true);
        final AtlasEntityHeader      updatedTblHeader  = tblUpdateResponse.getFirstEntityPartialUpdated();
        final AtlasEntity            updatedTblEntity2 = getEntityFromStore(updatedTblHeader);
        List<AtlasEntityHeader>      updatedColHeaders = tblUpdateResponse.getPartialUpdatedEntitiesByTypeName(COLUMN_TYPE);

        final AtlasEntity updatedCol1Entity = getEntityFromStore(updatedColHeaders.get(0));
        final AtlasEntity updatedCol2Entity = getEntityFromStore(updatedColHeaders.get(1));
        final AtlasEntity updatedCol3Entity = getEntityFromStore(updatedColHeaders.get(2));

        assertEquals(col1.getAttribute("description"), updatedCol1Entity.getAttribute("description"));
        assertEquals(col2.getAttribute("description"), updatedCol2Entity.getAttribute("description"));
        assertEquals(col3.getAttribute("description"), updatedCol3Entity.getAttribute("description"));
    }

    @Test
    public void testDifferentialEntitiesOnUpdate () throws Exception {

        ApplicationProperties.get().setProperty(STORE_DIFFERENTIAL_AUDITS.getPropertyName(), true);
        init();
        // test for entity attributes

        AtlasEntity dbEntity = new AtlasEntity(TestUtilsV2.DATABASE_TYPE);
        dbEntity.setAttribute("name", TestUtilsV2.randomString(10));
        dbEntity.setAttribute("description", "us db");
        dbEntity.setAttribute("namespace", "db namespace");
        dbEntity.setAttribute("cluster", "Fenton_Cluster");

        EntityStream           dbStream        = new AtlasEntityStream(new AtlasEntitiesWithExtInfo(dbEntity));
        EntityMutationResponse response        = entityStore.createOrUpdate(dbStream, false);
        AtlasEntityHeader      dbHeader        = response.getFirstEntityCreated();
        AtlasEntity            createdDbEntity = getEntityFromStore(dbHeader);

        assertEquals(RequestContext.get().getDifferentialEntities().size(), 0);

        // update the db entity
        dbEntity = new AtlasEntity(TestUtilsV2.DATABASE_TYPE);
        dbEntity.setGuid(createdDbEntity.getGuid());
        dbEntity.setAttribute("description", "new description");

        dbStream = new AtlasEntityStream(new AtlasEntitiesWithExtInfo(dbEntity));

        response = entityStore.createOrUpdate(dbStream, true);
        dbHeader = response.getFirstEntityPartialUpdated();
        AtlasEntity updatedDbEntity = getEntityFromStore(dbHeader);

        assertEquals(RequestContext.get().getDifferentialEntities().size(), 1);
        AtlasEntity diffEntity = RequestContext.get().getDifferentialEntity(updatedDbEntity.getGuid());
        assertNotNull(diffEntity.getAttribute("description"));
        Assert.assertNull(diffEntity.getAttribute("namespace"));
        Assert.assertNull(diffEntity.getAttribute("name"));
        ApplicationProperties.get().setProperty(STORE_DIFFERENTIAL_AUDITS.getPropertyName(), false);
    }

    @Test
    public void testSetObjectIdAttrToNull() throws Exception {
        final AtlasEntity dbEntity  = TestUtilsV2.createDBEntity();
        final AtlasEntity db2Entity = TestUtilsV2.createDBEntity();

        entityStore.createOrUpdate(new AtlasEntityStream(dbEntity), false);
        entityStore.createOrUpdate(new AtlasEntityStream(db2Entity), false);
        GraphTransactionInterceptor.clearCache();

        final AtlasEntity tableEntity = TestUtilsV2.createTableEntity(dbEntity);

        tableEntity.setAttribute("databaseComposite", AtlasTypeUtil.getAtlasObjectId(db2Entity));

        final EntityMutationResponse tblCreationResponse = entityStore.createOrUpdate(new AtlasEntityStream(tableEntity), false);
        final AtlasEntityHeader      createdTblHeader    = tblCreationResponse.getCreatedEntityByTypeNameAndAttribute(TABLE_TYPE, NAME, (String) tableEntity.getAttribute(NAME));
        final AtlasEntity            createdTblEntity    = getEntityFromStore(createdTblHeader);

        init();

        createdTblEntity.setAttribute("databaseComposite", null);

        final EntityMutationResponse tblUpdateResponse = entityStore.createOrUpdate(new AtlasEntityStream(createdTblEntity), true);
        GraphTransactionInterceptor.clearCache();

        final AtlasEntityHeader      updatedTblHeader  = tblUpdateResponse.getFirstEntityPartialUpdated();
        final AtlasEntity            updatedTblEntity  = getEntityFromStore(updatedTblHeader);
        final AtlasEntity            deletedDb2Entity  = getEntityFromStore(db2Entity.getGuid());

        assertEquals(deletedDb2Entity.getStatus(), AtlasEntity.Status.DELETED);
    }

    @Test
    public void testTagAssociationAfterRedefinition(){
        AtlasTypesDef typesDef = new AtlasTypesDef();
        getTagWithName(typesDef,"testTag","int");

        try {
            typeDefStore.createTypesDef(typesDef);
        } catch (AtlasBaseException e) {
            fail("Tag creation should've succeeded");
        }

        try {
            typeDefStore.deleteTypesDef(typesDef);
        } catch (AtlasBaseException e) {
            fail("Tag deletion should've succeeded");
        }

        AtlasClassificationDef aTag = getTagWithName(typesDef, "testTag", "string");

        try {
            typeDefStore.createTypesDef(typesDef);
        } catch (AtlasBaseException e) {
            fail("Tag re-creation should've succeeded");
        }

        final AtlasEntity dbEntity  = TestUtilsV2.createDBEntity();

        try {
            EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(dbEntity), false);
            List<AtlasEntityHeader> createdEntity = response.getCreatedEntities();
            assertTrue(CollectionUtils.isNotEmpty(createdEntity));
            String guid = createdEntity.get(0).getGuid();
            entityStore.addClassification(Arrays.asList(guid), new AtlasClassification(aTag.getName(), "testAttribute", "test-string"));
        } catch (AtlasBaseException e) {
            fail("DB entity creation should've succeeded, e.getMessage() => " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testCreate")
    public void associateMultipleTagsToOneEntity() throws AtlasBaseException {
        final String TAG_NAME              = "tag_xy";
        final String TAG_NAME_2            = TAG_NAME + "_2";
        final String TAG_ATTRIBUTE_NAME    = "testAttribute";
        final String TAG_ATTRIBUTE_VALUE   = "test-string";
        final String TAG_ATTRIBUTE_VALUE_2 = TAG_ATTRIBUTE_VALUE + "-2";

        createTag(TAG_NAME, "string");
        createTag(TAG_NAME_2, "string");

        List<AtlasClassification> addedClassifications = new ArrayList<>();
        addedClassifications.add(new AtlasClassification(TAG_NAME, TAG_ATTRIBUTE_NAME, TAG_ATTRIBUTE_VALUE));
        addedClassifications.add(new AtlasClassification(TAG_NAME_2, TAG_ATTRIBUTE_NAME, TAG_ATTRIBUTE_VALUE_2));
        entityStore.addClassifications(dbEntityGuid, addedClassifications);

        AtlasEntity dbEntityFromDb = getEntityFromStore(dbEntityGuid);

        List<String> actualClassifications = (List<String>) CollectionUtils.collect(dbEntityFromDb.getClassifications(), o -> ((AtlasClassification) o).getTypeName());
        Set<String> classificationSet = new HashSet<>(actualClassifications);
        assertTrue(classificationSet.contains(TAG_NAME));
        assertTrue(classificationSet.contains(TAG_NAME_2));

        for (String actualClassificationName : actualClassifications) {
            entityStore.deleteClassification(dbEntityGuid, actualClassificationName);
        }
    }

    @Test
    public void testCreateWithDuplicateGuids() throws Exception {
        init();
        AtlasEntityWithExtInfo tblEntity2 = TestUtilsV2.createTableEntityDuplicatesV2(dbEntity.getEntity());
        EntityMutationResponse response   = entityStore.createOrUpdate(new AtlasEntityStream(tblEntity2), false);

        List<AtlasEntityHeader> createdEntities = response.getCreatedEntities();
        assertTrue(CollectionUtils.isNotEmpty(createdEntities));
        assertEquals(createdEntities.size(), 2);

        String tableGuid = createdEntities.get(0).getGuid();
        AtlasEntityWithExtInfo tableEntity  = entityStore.getById(tableGuid);
        assertEquals(tableEntity.getReferredEntities().size(), 1);

        List<AtlasObjectId> columns = (List<AtlasObjectId>) tableEntity.getEntity().getAttribute("columns");
        assertEquals(columns.size(), 1);

        Set<AtlasObjectId> uniqueColumns = new HashSet<>(columns);
        assertTrue(uniqueColumns.size() == 1);
    }

    @Test(dependsOnMethods = "testCreate")
    public void associateSameTagToMultipleEntities() throws AtlasBaseException {
        final String TAG_NAME            = "tagx";
        final String TAG_ATTRIBUTE_NAME  = "testAttribute";
        final String TAG_ATTRIBUTE_VALUE = "test-string";

        createTag(TAG_NAME, "string");
        List<AtlasClassification> addedClassifications = new ArrayList<>();
        addedClassifications.add(new AtlasClassification(TAG_NAME, TAG_ATTRIBUTE_NAME, TAG_ATTRIBUTE_VALUE));

        entityStore.addClassifications(dbEntityGuid, addedClassifications);
        entityStore.addClassifications(tblEntityGuid, addedClassifications);

        AtlasEntity dbEntityFromDb  = getEntityFromStore(dbEntityGuid);
        AtlasEntity tblEntityFromDb = getEntityFromStore(tblEntityGuid);

        Set<String> actualDBClassifications  = new HashSet<>(CollectionUtils.collect(dbEntityFromDb.getClassifications(), o -> ((AtlasClassification) o).getTypeName()));
        Set<String> actualTblClassifications = new HashSet<>(CollectionUtils.collect(tblEntityFromDb.getClassifications(), o -> ((AtlasClassification) o).getTypeName()));

        assertTrue(actualDBClassifications.contains(TAG_NAME));
        assertTrue(actualTblClassifications.contains(TAG_NAME));

        Set<String> actualDBAssociatedEntityGuid  = new HashSet<>(CollectionUtils.collect(dbEntityFromDb.getClassifications(), o -> ((AtlasClassification) o).getEntityGuid()));
        Set<String> actualTblAssociatedEntityGuid = new HashSet<>(CollectionUtils.collect(tblEntityFromDb.getClassifications(), o -> ((AtlasClassification) o).getEntityGuid()));

        assertTrue(actualDBAssociatedEntityGuid.contains(dbEntityGuid));
        assertTrue(actualTblAssociatedEntityGuid.contains(tblEntityGuid));

        entityStore.deleteClassification(dbEntityGuid, TAG_NAME);
        entityStore.deleteClassification(tblEntityGuid, TAG_NAME);
    }

    @Test (dependsOnMethods = "testCreate")
    public void addCustomAttributesToEntity() throws Exception {

        ApplicationProperties.get().setProperty(STORE_DIFFERENTIAL_AUDITS.getPropertyName(), true);
        init();
        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("key1", "val1");
        customAttributes.put("key2", "val2");
        customAttributes.put("key3", "val3");
        customAttributes.put("key4", "val4");
        customAttributes.put("key5", "val5");

        tblEntity.setCustomAttributes(customAttributes);

        entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);
        assertEquals(RequestContext.get().getDifferentialEntities().size(), 1);

        tblEntity = getEntityFromStore(tblEntityGuid);

        assertEquals(customAttributes, tblEntity.getCustomAttributes());

        ApplicationProperties.get().setProperty(STORE_DIFFERENTIAL_AUDITS.getPropertyName(), false);
    }

    @Test (dependsOnMethods = "addCustomAttributesToEntity")
    public void updateCustomAttributesToEntity() throws AtlasBaseException {
        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        // update custom attributes, remove key3, key4 and key5
        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("key1", "val1");
        customAttributes.put("key2", "val2");

        tblEntity.setCustomAttributes(customAttributes);

        entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);

        tblEntity = getEntityFromStore(tblEntityGuid);

        assertEquals(customAttributes, tblEntity.getCustomAttributes());
    }

    @Test (dependsOnMethods = "updateCustomAttributesToEntity")
    public void deleteCustomAttributesToEntity() throws AtlasBaseException {
        AtlasEntity         tblEntity             = getEntityFromStore(tblEntityGuid);
        Map<String, String> emptyCustomAttributes = new HashMap<>();

        // remove all custom attributes
        tblEntity.setCustomAttributes(emptyCustomAttributes);

        entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);

        tblEntity = getEntityFromStore(tblEntityGuid);

        assertEquals(emptyCustomAttributes, tblEntity.getCustomAttributes());
    }

    @Test (dependsOnMethods = "deleteCustomAttributesToEntity")
    public void nullCustomAttributesToEntity() throws AtlasBaseException {
        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        Map<String, String> customAttributes = new HashMap<>();
        customAttributes.put("key1", "val1");
        customAttributes.put("key2", "val2");

        tblEntity.setCustomAttributes(customAttributes);

        entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);

        // assign custom attributes to null
        tblEntity.setCustomAttributes(null);

        entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);

        tblEntity = getEntityFromStore(tblEntityGuid);

        assertEquals(customAttributes, tblEntity.getCustomAttributes());
    }

    @Test (dependsOnMethods = "nullCustomAttributesToEntity")
    public void addInvalidKeysToEntityCustomAttributes() throws AtlasBaseException {
        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        // key should contain 1 to 50 alphanumeric characters, '_' or '-'
        Map<String, String> invalidCustomAttributes = new HashMap<>();
        invalidCustomAttributes.put("key0_65765-6565", "val0");
        invalidCustomAttributes.put("key1-aaa_bbb-ccc", "val1");
        invalidCustomAttributes.put("key2!@#$%&*()", "val2"); // invalid key characters

        tblEntity.setCustomAttributes(invalidCustomAttributes);

        try {
            entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);
        } catch (AtlasBaseException ex) {
            assertEquals(ex.getAtlasErrorCode(), INVALID_CUSTOM_ATTRIBUTE_KEY_CHARACTERS);
        }

        invalidCustomAttributes = new HashMap<>();
        invalidCustomAttributes.put("bigValue_lengthEquals_50", randomAlphanumeric(50));
        invalidCustomAttributes.put("bigValue_lengthEquals_51", randomAlphanumeric(51));

        tblEntity.setCustomAttributes(invalidCustomAttributes);

        try {
            entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);
        } catch (AtlasBaseException ex) {
            assertEquals(ex.getAtlasErrorCode(), INVALID_CUSTOM_ATTRIBUTE_KEY_LENGTH);
        }
    }

    @Test (dependsOnMethods = "addInvalidKeysToEntityCustomAttributes")
    public void addInvalidValuesToEntityCustomAttributes() throws AtlasBaseException {
        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        // value length is greater than 500
        Map<String, String> invalidCustomAttributes = new HashMap<>();
        invalidCustomAttributes.put("key1", randomAlphanumeric(500));
        invalidCustomAttributes.put("key2", randomAlphanumeric(501));

        tblEntity.setCustomAttributes(invalidCustomAttributes);

        try {
            entityStore.createOrUpdate(new AtlasEntityStream(tblEntity), false);
        } catch (AtlasBaseException ex) {
            assertEquals(ex.getAtlasErrorCode(), INVALID_CUSTOM_ATTRIBUTE_VALUE);
        }
    }

    @Test(dependsOnMethods = "testCreate")
    public void addLabelsToEntity() throws AtlasBaseException {
        Set<String> labels = new HashSet<>();
        labels.add("label_1");
        labels.add("label_2");
        labels.add("label_3");
        labels.add("label_4");
        labels.add("label_5");

        entityStore.setLabels(tblEntityGuid, labels);

        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        assertEquals(labels, tblEntity.getLabels());
    }

    @Test (dependsOnMethods = "addLabelsToEntity")
    public void updateLabelsToEntity() throws AtlasBaseException {
        Set<String> labels = new HashSet<>();
        labels.add("label_1_update");
        labels.add("label_2_update");
        labels.add("label_3_update");

        entityStore.setLabels(tblEntityGuid, labels);

        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        assertEquals(labels, tblEntity.getLabels());
    }

    @Test (dependsOnMethods = "updateLabelsToEntity")
    public void clearLabelsToEntity() throws AtlasBaseException {
        HashSet<String> emptyLabels = new HashSet<>();

        entityStore.setLabels(tblEntityGuid, emptyLabels);

        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        Assert.assertTrue(tblEntity.getLabels().isEmpty());
    }

    @Test (dependsOnMethods = "clearLabelsToEntity")
    public void emptyLabelsToEntity() throws AtlasBaseException {
        entityStore.setLabels(tblEntityGuid, null);

        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);

        Assert.assertTrue(tblEntity.getLabels().isEmpty());
    }

    @Test (dependsOnMethods = "emptyLabelsToEntity")
    public void invalidLabelLengthToEntity() throws AtlasBaseException {
        Set<String> labels = new HashSet<>();
        labels.add(randomAlphanumeric(50));
        labels.add(randomAlphanumeric(51));

        try {
            entityStore.setLabels(tblEntityGuid, labels);
        } catch (AtlasBaseException ex) {
            assertEquals(ex.getAtlasErrorCode(), INVALID_LABEL_LENGTH);
        }
    }

    @Test (dependsOnMethods = "invalidLabelLengthToEntity")
    public void invalidLabelCharactersToEntity() {
        Set<String> labels = new HashSet<>();
        labels.add("label-1_100_45");
        labels.add("LABEL-1_200-55");
        labels.add("LaBeL-1-)(*U&%^%#$@!~");

        try {
            entityStore.setLabels(tblEntityGuid, labels);
        } catch (AtlasBaseException ex) {
            assertEquals(ex.getAtlasErrorCode(), INVALID_LABEL_CHARACTERS);
        }
    }

    @Test (dependsOnMethods = "invalidLabelCharactersToEntity")
    public void addMoreLabelsToEntity() throws AtlasBaseException {
        Set<String> labels = new HashSet<>();
        labels.add("label_1_add");
        labels.add("label_2_add");
        labels.add("label_3_add");

        entityStore.addLabels(tblEntityGuid, labels);
        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);
        Assert.assertTrue(tblEntity.getLabels().containsAll(labels));

        tblEntity.setAttribute("description", "tbl for labels");
        AtlasEntitiesWithExtInfo entitiesInfo = new AtlasEntitiesWithExtInfo(tblEntity);
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesInfo), true);
        validateMutationResponse(response, EntityOperation.PARTIAL_UPDATE, 1);
        tblEntity = getEntityFromStore(response.getFirstEntityPartialUpdated());
        Assert.assertEquals(tblEntity.getLabels(), labels );
    }

    @Test (dependsOnMethods = "addMoreLabelsToEntity")
    public void deleteLabelsToEntity() throws AtlasBaseException {
        Set<String> labels = new HashSet<>();
        labels.add("label_1_add");
        labels.add("label_2_add");

        entityStore.removeLabels(tblEntityGuid, labels);
        AtlasEntity tblEntity = getEntityFromStore(tblEntityGuid);
        assertNotNull(tblEntity.getLabels());
        Assert.assertEquals(tblEntity.getLabels().size(), 1);

        labels.clear();
        labels.add("label_4_add");
        entityStore.removeLabels(tblEntityGuid, labels);
        tblEntity = getEntityFromStore(tblEntityGuid);
        assertNotNull(tblEntity.getLabels());
        Assert.assertEquals(tblEntity.getLabels().size(), 1);

        labels.clear();
        labels.add("label_3_add");
        entityStore.removeLabels(tblEntityGuid, labels);
        tblEntity = getEntityFromStore(tblEntityGuid);
        Assert.assertTrue(tblEntity.getLabels().isEmpty());
    }

    @Test
    public void testAddBusinessAttributesStringMaxLengthCheck() throws Exception {
        Map<String, Map<String, Object>> bmMapReq = new HashMap<>();
        Map<String, Object> bmAttrMapReq = new HashMap<>();
        bmAttrMapReq.put("attr8", "01234567890123456789");
        bmMapReq.put("bmWithAllTypes", bmAttrMapReq);
        entityStore.addOrUpdateBusinessAttributes(dbEntity.getEntity().getGuid(), bmMapReq, false);
        AtlasEntityWithExtInfo entity = entityStore.getById(dbEntity.getEntity().getGuid());
        Map<String, Map<String, Object>> bmMapRes = entity.getEntity().getBusinessAttributes();
        Assert.assertEquals(bmMapReq, bmMapRes);
    }

    @Test
    public void testAddBusinessAttributesStringMaxLengthCheck_2() throws Exception {
        Map<String, Map<String, Object>> bmMapReq = new HashMap<>();
        Map<String, Object> bmAttrMapReq = new HashMap<>();
        bmAttrMapReq.put("attr8", "012345678901234567890");
        bmMapReq.put("bmWithAllTypes", bmAttrMapReq);
        try {
            entityStore.addOrUpdateBusinessAttributes(dbEntity.getEntity().getGuid(), bmMapReq, false);
        } catch (AtlasBaseException e) {
            Assert.assertEquals(AtlasErrorCode.INSTANCE_CRUD_INVALID_PARAMS, e.getAtlasErrorCode());
            return;
        }
        Assert.fail();
    }

    @Test(dependsOnMethods = "testAddBusinessAttributesStringMaxLengthCheck")
    public void testUpdateBusinessAttributesStringMaxLengthCheck() throws Exception {
        Map<String, Map<String, Object>> bmMapReq = new HashMap<>();
        Map<String, Object> bmAttrMapReq = new HashMap<>();
        bmAttrMapReq.put("attr8", "0123456789");
        bmMapReq.put("bmWithAllTypes", bmAttrMapReq);

        entityStore.addOrUpdateBusinessAttributes(dbEntity.getEntity().getGuid(), bmMapReq, true);
        AtlasEntityWithExtInfo entity = entityStore.getById(dbEntity.getEntity().getGuid());
        Map<String, Map<String, Object>> bmMapRes = entity.getEntity().getBusinessAttributes();
        Assert.assertEquals(bmMapReq, bmMapRes);
    }

    @Test(dependsOnMethods = "testAddBusinessAttributesStringMaxLengthCheck")
    public void testUpdateBusinessAttributesStringMaxLengthCheck_2() throws Exception {
        Map<String, Map<String, Object>> bmMapReq = new HashMap<>();
        Map<String, Object> bmAttrMapReq = new HashMap<>();
        bmAttrMapReq.put("attr8", "012345678901234567890");
        bmMapReq.put("bmWithAllTypes", bmAttrMapReq);
        try {
            entityStore.addOrUpdateBusinessAttributes(dbEntity.getEntity().getGuid(), bmMapReq, true);
        } catch (AtlasBaseException e) {
            Assert.assertEquals(AtlasErrorCode.INSTANCE_CRUD_INVALID_PARAMS, e.getAtlasErrorCode());
            return;
        }
        Assert.fail();
    }

    @Test(dependsOnMethods = "testAddBusinessAttributesStringMaxLengthCheck")
    public void testUpdateBusinessAttributesStringMaxLengthCheck_3() throws Exception {
        Map<String, Map<String, Object>> bmAttrMapReq = new HashMap<>();
        Map<String, Object> attrValueMapReq = new HashMap<>();
        List<String> stringList = new ArrayList<>();
        stringList.add("0123456789");
        stringList.add("0123456789");
        attrValueMapReq.put("attr18", stringList);
        bmAttrMapReq.put("bmWithAllTypesMV", attrValueMapReq);
        entityStore.addOrUpdateBusinessAttributes(dbEntity.getEntity().getGuid(), bmAttrMapReq, true);

        AtlasEntityWithExtInfo entity = entityStore.getById(dbEntity.getEntity().getGuid());
        Map<String, Map<String, Object>> bmAttrMapRes = entity.getEntity().getBusinessAttributes();
        Assert.assertEquals(bmAttrMapReq, bmAttrMapRes);
    }

    @Test(dependsOnMethods = "testAddBusinessAttributesStringMaxLengthCheck")
    public void testUpdateBusinessAttributesStringMaxLengthCheck_4() throws Exception {
        Map<String, Map<String, Object>> bmAttrMapReq = new HashMap<>();
        Map<String, Object> attrValueMapReq = new HashMap<>();
        List<String> stringList = new ArrayList<>();
        stringList.add("0123456789");
        stringList.add("012345678901234567890");
        attrValueMapReq.put("attr18", stringList);
        bmAttrMapReq.put("bmWithAllTypesMV", attrValueMapReq);
        try {
            entityStore.addOrUpdateBusinessAttributes(dbEntity.getEntity().getGuid(), bmAttrMapReq, true);
        } catch (AtlasBaseException e) {
            Assert.assertEquals(AtlasErrorCode.INSTANCE_CRUD_INVALID_PARAMS, e.getAtlasErrorCode());
            return;
        }
        Assert.fail();
    }

    @Test
    public void testGetTemplate() {
        try {
            String bMHeaderListAsString = FileUtils.getBusinessMetadataHeaders();

            assertNotNull(bMHeaderListAsString);
            assertEquals(bMHeaderListAsString, "EntityType,EntityUniqueAttributeValue,BusinessAttributeName,BusinessAttributeValue,EntityUniqueAttributeName[optional]");
        } catch (Exception e) {
            fail("The Template for BussinessMetadata Attributes should've been a success : ", e);
        }
    }

    @Test
    public void testEmptyFileException() {
        InputStream inputStream = getFile(CSV_FILES, "empty.csv");

        try {
            entityStore.bulkCreateOrUpdateBusinessAttributes(inputStream, "empty.csv");
            fail("Error occurred : Failed to recognize the empty file.");
        } catch (AtlasBaseException e) {
            assertEquals(e.getMessage(), "No data found in the uploaded file");
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Test(dependsOnMethods = "testCreate")
    public void testBulkAddOrUpdateBusinessAttributes() {
        try {
            AtlasEntity hive_db_1 = getEntityFromStore(dbEntityGuid);
            String dbName = (String) hive_db_1.getAttribute("name");
            String data = TestUtilsV2.getFileData(CSV_FILES, "template_2.csv");
            data = data.replaceAll("hive_db_1", dbName);
            InputStream inputStream1 = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
            BulkImportResponse bulkImportResponse = entityStore.bulkCreateOrUpdateBusinessAttributes(inputStream1, "template_2.csv");
            assertEquals(CollectionUtils.isEmpty(bulkImportResponse.getSuccessImportInfoList()), false);
            assertEquals(CollectionUtils.isEmpty(bulkImportResponse.getFailedImportInfoList()), true);

        } catch (Exception e) {
            fail("The BusinessMetadata Attribute should have been assigned " +e);
        }
    }
}