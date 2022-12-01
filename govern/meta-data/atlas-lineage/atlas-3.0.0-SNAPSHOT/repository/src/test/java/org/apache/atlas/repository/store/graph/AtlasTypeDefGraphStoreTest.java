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
package org.apache.atlas.repository.store.graph;

import com.google.inject.Inject;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.typedef.*;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.HashSet;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import static org.testng.Assert.*;

@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasTypeDefGraphStoreTest extends AtlasTestBase {
    private static final Logger LOG = LoggerFactory.getLogger(AtlasTypeDefGraphStoreTest.class);

    @Inject
    private
    AtlasTypeDefStore typeDefStore;

    @BeforeTest
    public void setupTest() {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
    }

    @BeforeClass
    public void initialize() throws Exception {
        super.initialize();
    }

    @AfterClass
    public void cleanup() throws Exception {
        super.cleanup();
    }

    @Test
    public void testGet() {
        try {
            AtlasTypesDef typesDef = typeDefStore.searchTypesDef(new SearchFilter());
            assertNotNull(typesDef.getEnumDefs());
            assertEquals(typesDef.getStructDefs().size(), 0);
            assertNotNull(typesDef.getStructDefs());
            assertEquals(typesDef.getClassificationDefs().size(), 0);
            assertNotNull(typesDef.getClassificationDefs());
            assertEquals(typesDef.getEntityDefs().size(), 0);
            assertNotNull(typesDef.getEntityDefs());
        } catch (AtlasBaseException e) {
            fail("Search of types shouldn't have failed");
        }
    }

    @Test(dataProvider = "invalidGetProvider", dependsOnMethods = "testGet")
    public void testInvalidGet(String name, String guid){
        try {
            assertNull(typeDefStore.getEnumDefByName(name));
            fail("Exception expected for invalid name");
        } catch (AtlasBaseException e) {
        }

        try {
            assertNull(typeDefStore.getEnumDefByGuid(guid));
            fail("Exception expected for invalid guid");
        } catch (AtlasBaseException e) {
        }

        try {
            assertNull(typeDefStore.getStructDefByName(name));
            fail("Exception expected for invalid name");
        } catch (AtlasBaseException e) {
        }

        try {
            assertNull(typeDefStore.getStructDefByGuid(guid));
            fail("Exception expected for invalid guid");
        } catch (AtlasBaseException e) {
        }

        try {
            assertNull(typeDefStore.getClassificationDefByName(name));
            fail("Exception expected for invalid name");
        } catch (AtlasBaseException e) {
        }

        try {
            assertNull(typeDefStore.getClassificationDefByGuid(guid));
            fail("Exception expected for invalid guid");
        } catch (AtlasBaseException e) {
        }

        try {
            assertNull(typeDefStore.getEntityDefByName(name));
            fail("Exception expected for invalid name");
        } catch (AtlasBaseException e) {
        }

        try {
            assertNull(typeDefStore.getEntityDefByGuid(guid));
            fail("Exception expected for invalid guid");
        } catch (AtlasBaseException e) {
        }
    }

    @DataProvider
    public Object[][] invalidGetProvider(){
        return new Object[][] {
                {"name1", "guid1"},
                {"", ""},
                {null, null}
        };
    }

    @DataProvider
    public Object[][] validCreateDeptTypes(){
        return new Object[][] {
                {TestUtilsV2.defineDeptEmployeeTypes()}
        };
    }

    @DataProvider
    public Object[][] validUpdateDeptTypes(){
        AtlasTypesDef typesDef = TestUtilsV2.defineValidUpdatedDeptEmployeeTypes();
        return new Object[][] {
                {typesDef}
        };
    }

    @DataProvider
    public Object[][] allCreatedTypes(){
        // Capture all the types that are getting created or updated here.
        AtlasTypesDef updatedTypeDefs = TestUtilsV2.defineValidUpdatedDeptEmployeeTypes();
        AtlasTypesDef allTypeDefs = new AtlasTypesDef();
        allTypeDefs.getEnumDefs().addAll(updatedTypeDefs.getEnumDefs());
        allTypeDefs.getStructDefs().addAll(updatedTypeDefs.getStructDefs());
        allTypeDefs.getClassificationDefs().addAll(updatedTypeDefs.getClassificationDefs());
        allTypeDefs.getEntityDefs().addAll(updatedTypeDefs.getEntityDefs());
        allTypeDefs.getEntityDefs().addAll(TestUtilsV2.getEntityWithValidSuperType());
        return new Object[][] {{allTypeDefs}};
    }

    @DataProvider
    public Object[][] invalidCreateTypes(){
        // TODO: Create invalid type in TestUtilsV2
        return new Object[][] {
        };
    }

    @DataProvider
    public Object[][] invalidUpdateTypes(){
        return new Object[][] {
                {TestUtilsV2.defineInvalidUpdatedDeptEmployeeTypes()}
        };
    }

    @Test(dependsOnMethods = {"testGet"}, dataProvider = "validCreateDeptTypes")
    public void testCreateDept(AtlasTypesDef atlasTypesDef) {
        AtlasTypesDef existingTypesDef = null;
        try {
            existingTypesDef = typeDefStore.searchTypesDef(new SearchFilter());
        } catch (AtlasBaseException e) {
            // ignore
        }

        assertNotEquals(atlasTypesDef, existingTypesDef, "Types to be created already exist in the system");
        AtlasTypesDef createdTypesDef = null;
        try {
            createdTypesDef = typeDefStore.createTypesDef(atlasTypesDef);
            assertNotNull(createdTypesDef);
            assertTrue(createdTypesDef.getEnumDefs().containsAll(atlasTypesDef.getEnumDefs()), "EnumDefs create failed");
            assertTrue(createdTypesDef.getClassificationDefs().containsAll(atlasTypesDef.getClassificationDefs()), "ClassificationDef create failed");
            assertTrue(createdTypesDef.getStructDefs().containsAll(atlasTypesDef.getStructDefs()), "StructDef creation failed");
            Assert.assertEquals(createdTypesDef.getEntityDefs(), atlasTypesDef.getEntityDefs());

        } catch (AtlasBaseException e) {
            fail("Creation of Types should've been a success", e);
        }
    }

    @Test(dependsOnMethods = {"testCreateDept"}, dataProvider = "validUpdateDeptTypes")
    public void testUpdate(AtlasTypesDef atlasTypesDef){
        try {
            AtlasTypesDef updatedTypesDef = typeDefStore.updateTypesDef(atlasTypesDef);
            assertNotNull(updatedTypesDef);

            assertEquals(updatedTypesDef.getEnumDefs().size(), atlasTypesDef.getEnumDefs().size(), "EnumDefs update failed");
            assertEquals(updatedTypesDef.getClassificationDefs().size(), atlasTypesDef.getClassificationDefs().size(), "ClassificationDef update failed");
            assertEquals(updatedTypesDef.getStructDefs().size(), atlasTypesDef.getStructDefs().size(), "StructDef update failed");
            assertEquals(updatedTypesDef.getEntityDefs().size(), atlasTypesDef.getEntityDefs().size(), "EntityDef update failed");

            // Try another update round by name and GUID
            for (AtlasEnumDef enumDef : updatedTypesDef.getEnumDefs()) {
                AtlasEnumDef updated = typeDefStore.updateEnumDefByGuid(enumDef.getGuid(), enumDef);
                assertNotNull(updated);
            }
            for (AtlasEnumDef enumDef : atlasTypesDef.getEnumDefs()) {
                AtlasEnumDef updated = typeDefStore.updateEnumDefByName(enumDef.getName(), enumDef);
                assertNotNull(updated);
            }

            // Try another update round by name and GUID
            for (AtlasClassificationDef classificationDef : updatedTypesDef.getClassificationDefs()) {
                AtlasClassificationDef updated = typeDefStore.updateClassificationDefByGuid(classificationDef.getGuid(), classificationDef);
                assertNotNull(updated);
            }
            for (AtlasClassificationDef classificationDef : atlasTypesDef.getClassificationDefs()) {
                AtlasClassificationDef updated = typeDefStore.updateClassificationDefByName(classificationDef.getName(), classificationDef);
                assertNotNull(updated);
            }

            // Try another update round by name and GUID
            for (AtlasStructDef structDef : updatedTypesDef.getStructDefs()) {
                AtlasStructDef updated = typeDefStore.updateStructDefByGuid(structDef.getGuid(), structDef);
                assertNotNull(updated);
            }
            for (AtlasStructDef structDef : atlasTypesDef.getStructDefs()) {
                AtlasStructDef updated = typeDefStore.updateStructDefByName(structDef.getName(), structDef);
                assertNotNull(updated);
            }

            // Try another update round by name and GUID
            for (AtlasEntityDef entityDef : updatedTypesDef.getEntityDefs()) {
                AtlasEntityDef updated = typeDefStore.updateEntityDefByGuid(entityDef.getGuid(), entityDef);
                assertNotNull(updated);
            }
            for (AtlasEntityDef entityDef : atlasTypesDef.getEntityDefs()) {
                AtlasEntityDef updated = typeDefStore.updateEntityDefByName(entityDef.getName(), entityDef);
                assertNotNull(updated);
            }

        } catch (AtlasBaseException e) {
            fail("TypeDef updates should've succeeded");
        }
    }

    @Test(enabled = false, dependsOnMethods = {"testCreateDept"})
    public void testUpdateWithMandatoryFields(){
        AtlasTypesDef atlasTypesDef = TestUtilsV2.defineInvalidUpdatedDeptEmployeeTypes();
        List<AtlasEnumDef> enumDefsToUpdate = atlasTypesDef.getEnumDefs();
        List<AtlasClassificationDef> classificationDefsToUpdate = atlasTypesDef.getClassificationDefs();
        List<AtlasStructDef> structDefsToUpdate = atlasTypesDef.getStructDefs();
        List<AtlasEntityDef> entityDefsToUpdate = atlasTypesDef.getEntityDefs();

        AtlasTypesDef onlyEnums = new AtlasTypesDef(enumDefsToUpdate,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        AtlasTypesDef onlyStructs = new AtlasTypesDef(Collections.EMPTY_LIST,
                structDefsToUpdate, Collections.EMPTY_LIST, Collections.EMPTY_LIST);

        AtlasTypesDef onlyClassification = new AtlasTypesDef(Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, classificationDefsToUpdate, Collections.EMPTY_LIST);

        AtlasTypesDef onlyEntities = new AtlasTypesDef(Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, entityDefsToUpdate);

        try {
            AtlasTypesDef updated = typeDefStore.updateTypesDef(onlyEnums);
            assertNotNull(updated);
        } catch (AtlasBaseException ignored) {}

        try {
            AtlasTypesDef updated = typeDefStore.updateTypesDef(onlyClassification);
            assertNotNull(updated);
            assertEquals(updated.getClassificationDefs().size(), 0, "Updates should've failed");
        } catch (AtlasBaseException ignored) {}

        try {
            AtlasTypesDef updated = typeDefStore.updateTypesDef(onlyStructs);
            assertNotNull(updated);
            assertEquals(updated.getStructDefs().size(), 0, "Updates should've failed");
        } catch (AtlasBaseException ignored) {}

        try {
            AtlasTypesDef updated = typeDefStore.updateTypesDef(onlyEntities);
            assertNotNull(updated);
            assertEquals(updated.getEntityDefs().size(), 0, "Updates should've failed");
        } catch (AtlasBaseException ignored) {}
    }

    // This should run after all the update calls
    @Test(dependsOnMethods = {"testUpdate"}, dataProvider = "allCreatedTypes")
    public void testDelete(AtlasTypesDef atlasTypesDef){
        try {
            typeDefStore.deleteTypesDef(atlasTypesDef);
        } catch (AtlasBaseException e) {
            fail("Deletion should've succeeded");
        }
    }

    @Test
    public void deleteTypeByName() throws IOException {
        try {
            final String HIVEDB_v2_JSON = "hiveDBv2";
            final String hiveDB2 = "hive_db_v2";
            final String relationshipDefName = "cluster_hosts_relationship";
            final String hostEntityDef = "host";
            final String clusterEntityDef = "cluster";
            AtlasTypesDef typesDef = TestResourceFileUtils.readObjectFromJson(".", HIVEDB_v2_JSON, AtlasTypesDef.class);
            typeDefStore.createTypesDef(typesDef);
            typeDefStore.deleteTypeByName(hiveDB2);
            typeDefStore.deleteTypeByName(relationshipDefName);
            typeDefStore.deleteTypeByName(hostEntityDef);
            typeDefStore.deleteTypeByName(clusterEntityDef);
        } catch (AtlasBaseException e) {
            fail("Deletion should've succeeded");
        }
    }

    @Test(dependsOnMethods = "testGet")
    public void testCreateWithValidAttributes(){
        AtlasTypesDef hiveTypes = TestUtilsV2.defineHiveTypes();
        try {
            AtlasTypesDef createdTypes = typeDefStore.createTypesDef(hiveTypes);
            assertEquals(hiveTypes.getEnumDefs(), createdTypes.getEnumDefs(), "Data integrity issue while persisting");
            assertEquals(hiveTypes.getStructDefs(), createdTypes.getStructDefs(), "Data integrity issue while persisting");
            assertEquals(hiveTypes.getClassificationDefs(), createdTypes.getClassificationDefs(), "Data integrity issue while persisting");
            assertEquals(hiveTypes.getEntityDefs(), createdTypes.getEntityDefs(), "Data integrity issue while persisting");
        } catch (AtlasBaseException e) {
            fail("Hive Type creation should've succeeded");
        }
    }

    @Test(dependsOnMethods = "testGet")
    public void testCreateWithNestedContainerAttributes() {
        AtlasTypesDef typesDef = TestUtilsV2.defineTypeWithNestedCollectionAttributes();

        try {
            AtlasTypesDef createdTypes = typeDefStore.createTypesDef(typesDef);
            assertEquals(typesDef.getEnumDefs(), createdTypes.getEnumDefs(), "Data integrity issue while persisting");
            assertEquals(typesDef.getStructDefs(), createdTypes.getStructDefs(), "Data integrity issue while persisting");
            assertEquals(typesDef.getClassificationDefs(), createdTypes.getClassificationDefs(), "Data integrity issue while persisting");
            assertEquals(typesDef.getEntityDefs(), createdTypes.getEntityDefs(), "Data integrity issue while persisting");
        } catch (AtlasBaseException e) {
            fail("creation of type with nested-container attributes should've succeeded");
        }
    }

    @Test(enabled = false)
    public void testCreateWithInvalidAttributes(){
    }

    @Test(dependsOnMethods = "testGet")
    public void testCreateWithValidSuperTypes(){
        // Test Classification with supertype
        List<AtlasClassificationDef> classificationDefs = TestUtilsV2.getClassificationWithValidSuperType();

        AtlasTypesDef toCreate = new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(),
                Collections.<AtlasStructDef>emptyList(),
                classificationDefs,
                Collections.<AtlasEntityDef>emptyList());
        try {
            AtlasTypesDef created = typeDefStore.createTypesDef(toCreate);
            assertEquals(created.getClassificationDefs(), toCreate.getClassificationDefs(),
                    "Classification creation with valid supertype should've succeeded");
        } catch (AtlasBaseException e) {
            fail("Classification creation with valid supertype should've succeeded");
        }

        // Test Entity with supertype
        List<AtlasEntityDef> entityDefs = TestUtilsV2.getEntityWithValidSuperType();
        toCreate = new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(),
                Collections.<AtlasStructDef>emptyList(),
                Collections.<AtlasClassificationDef>emptyList(),
                entityDefs);
        try {
            AtlasTypesDef created = typeDefStore.createTypesDef(toCreate);
            assertEquals(created.getEntityDefs(), toCreate.getEntityDefs(),
                    "Entity creation with valid supertype should've succeeded");
        } catch (AtlasBaseException e) {
            fail("Entity creation with valid supertype should've succeeded");
        }
    }

    @Test(dependsOnMethods = "testGet")
    public void testCreateWithInvalidSuperTypes(){
        AtlasTypesDef typesDef;

        // Test Classification with supertype
        AtlasClassificationDef classificationDef = TestUtilsV2.getClassificationWithInvalidSuperType();
        typesDef = new AtlasTypesDef();
        typesDef.getClassificationDefs().add(classificationDef);
        try {
            AtlasTypesDef created = typeDefStore.createTypesDef(typesDef);
            fail("Classification creation with invalid supertype should've failed");
        } catch (AtlasBaseException e) {
            typesDef = null;
        }

        // Test Entity with supertype
        AtlasEntityDef entityDef = TestUtilsV2.getEntityWithInvalidSuperType();
        typesDef = new AtlasTypesDef();
        typesDef.getEntityDefs().add(entityDef);
        try {
            AtlasTypesDef created = typeDefStore.createTypesDef(typesDef);
            fail("Entity creation with invalid supertype should've failed");
        } catch (AtlasBaseException e) {}

    }

    @Test(dependsOnMethods = "testGet")
    public void testCreateClassificationDefWithValidEntityType(){
        final String entityTypeName ="testCreateClassificationDefWithValidEntityTypeEntity1";
        final String classificationTypeName ="testCreateClassificationDefWithValidEntityTypeClassification1";

        List<AtlasEntityDef> entityDefs = TestUtilsV2.getEntityWithName(entityTypeName);

        // Test Classification with entitytype
        List<AtlasClassificationDef> classificationDefs = TestUtilsV2.getClassificationWithName(classificationTypeName);

        Set<String> entityTypeNames =  new HashSet<String>();
        entityTypeNames.add(entityTypeName);
        classificationDefs.get(0).setEntityTypes(entityTypeNames);
        AtlasTypesDef toCreate = new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(),
                Collections.<AtlasStructDef>emptyList(),
                classificationDefs,
                entityDefs);
        try {
            AtlasTypesDef created = typeDefStore.createTypesDef(toCreate);
            assertEquals(created.getClassificationDefs(), toCreate.getClassificationDefs(),
                    "Classification creation with valid entitytype should've succeeded");
        } catch (AtlasBaseException e) {
            fail("Classification creation with valid entitytype should've succeeded. Failed with " + e.getMessage());
        }
    }

    @Test(dependsOnMethods = "testGet")
    public void testCreateWithInvalidEntityType(){
        final String classificationTypeName ="testCreateClassificationDefWithInvalidEntityTypeClassification1";
        // Test Classification with entitytype
        List<AtlasClassificationDef> classificationDefs = TestUtilsV2.getClassificationWithName(classificationTypeName);

        Set<String> entityTypeNames =  new HashSet<String>();
        entityTypeNames.add("cccc");
        classificationDefs.get(0).setEntityTypes(entityTypeNames);
        AtlasTypesDef toCreate = new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(),
                Collections.<AtlasStructDef>emptyList(),
                classificationDefs,
                Collections.<AtlasEntityDef>emptyList());
        try {
            AtlasTypesDef created = typeDefStore.createTypesDef(toCreate);
            fail("Classification creation with invalid entitytype should've failed");
        } catch (AtlasBaseException e) {

        }
    }

    /**
     * test that specifying an entitytype in a child classificationDef when then parent has unrestricted entityTypes fails.
     */
    @Test(dependsOnMethods = "testGet")
    public void testCreateWithInvalidEntityType2(){
        final String classificationTypeName1 ="testCreateClassificationDefWithInvalidEntityType2Classification1";
        final String classificationTypeName2 ="testCreateClassificationDefWithInvalidEntityType2Classification2";
        final String entityTypeName1 ="testCreateClassificationDefWithInvalidEntityType2Entity1";

        // Test Classification with entitytype
        AtlasClassificationDef classificationDef1 = TestUtilsV2.getSingleClassificationWithName(classificationTypeName1);
        AtlasClassificationDef classificationDef2 = TestUtilsV2.getSingleClassificationWithName(classificationTypeName2);
        List<AtlasEntityDef> entityDefs = TestUtilsV2.getEntityWithName(entityTypeName1);


        Set<String> entityTypeNames =  new HashSet<String>();
        entityTypeNames.add(entityTypeName1);

        Set<String> superTypes =  new HashSet<String>();
        superTypes.add(classificationTypeName1);

        classificationDef2.setSuperTypes(superTypes);
        classificationDef1.setEntityTypes(entityTypeNames);

        TestUtilsV2.populateSystemAttributes(classificationDef1);
        TestUtilsV2.populateSystemAttributes(classificationDef2);

        List<AtlasClassificationDef>  classificationDefs = Arrays.asList(classificationDef1,classificationDef2);

        AtlasTypesDef toCreate = new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(),
                Collections.<AtlasStructDef>emptyList(),
                classificationDefs,
                Collections.<AtlasEntityDef>emptyList());
        try {
            AtlasTypesDef created = typeDefStore.createTypesDef(toCreate);
            fail("Classification creation with invalid entitytype should've failed");
        } catch (AtlasBaseException e) {

        }
    }

    /**
     * test that specifying an entitytype in a child classificationDef which is not in the parent fails
     */
    @Test(dependsOnMethods = "testGet")
    public void testCreateWithInvalidEntityType3(){
        final String classificationTypeName1 ="testCreateClassificationDefWithInvalidEntityType3Classification1";
        final String classificationTypeName2 ="testCreateClassificationDefWithInvalidEntityType3Classification2";
        final String entityTypeName1 ="testCreateClassificationDefWithInvalidEntityType3Entity1";
        final String entityTypeName2 ="testCreateClassificationDefWithInvalidEntityType3Entity2";


        // Test Classification with entitytype
        AtlasClassificationDef classificationDef1 = TestUtilsV2.getSingleClassificationWithName(classificationTypeName1);
        AtlasClassificationDef classificationDef2 = TestUtilsV2.getSingleClassificationWithName(classificationTypeName2);
        AtlasEntityDef entityDef1 = TestUtilsV2.getSingleEntityWithName(entityTypeName1);
        AtlasEntityDef entityDef2 = TestUtilsV2.getSingleEntityWithName(entityTypeName2);

        Set<String> entityTypeNames1 =  new HashSet<String>();
        entityTypeNames1.add(entityTypeName1);

        Set<String> entityTypeNames2 =  new HashSet<String>();
        entityTypeNames2.add(entityTypeName2);

        Set<String> superTypes =  new HashSet<String>();
        superTypes.add(classificationTypeName1);

        classificationDef1.setEntityTypes(entityTypeNames1);


        classificationDef2.setSuperTypes(superTypes);
        classificationDef2.setEntityTypes(entityTypeNames2);

        TestUtilsV2.populateSystemAttributes(classificationDef1);
        TestUtilsV2.populateSystemAttributes(classificationDef2);
        TestUtilsV2.populateSystemAttributes(entityDef1);
        TestUtilsV2.populateSystemAttributes(entityDef2);

        List<AtlasClassificationDef>  classificationDefs = Arrays.asList(classificationDef1,classificationDef2);
        List<AtlasEntityDef>  entityDefs = Arrays.asList(entityDef1,entityDef2);

        AtlasTypesDef toCreate = new AtlasTypesDef(Collections.<AtlasEnumDef>emptyList(),
                Collections.<AtlasStructDef>emptyList(),
                classificationDefs,
                entityDefs);
        try {
            AtlasTypesDef created = typeDefStore.createTypesDef(toCreate);
            fail("Classification creation with invalid entitytype should've failed");
        } catch (AtlasBaseException e) {

        }
    }

    @Test(dependsOnMethods = "testGet")
    public void testSearchFunctionality() {
        SearchFilter searchFilter = new SearchFilter();
        searchFilter.setParam(SearchFilter.PARAM_SUPERTYPE, "Person");

        try {
            AtlasTypesDef typesDef = typeDefStore.searchTypesDef(searchFilter);
            assertNotNull(typesDef);
            assertNotNull(typesDef.getEntityDefs());
            assertEquals(typesDef.getEntityDefs().size(), 3);
        } catch (AtlasBaseException e) {
            fail("Search should've succeeded", e);
        }
    }

    @Test(dependsOnMethods = "testGet")
    public void testTypeDeletionAndRecreate() {
        AtlasClassificationDef aTag = new AtlasClassificationDef("testTag");
        AtlasAttributeDef attributeDef = new AtlasAttributeDef("testAttribute", "string", true,
                AtlasAttributeDef.Cardinality.SINGLE, 0, 1,
                false, true, false,
                Collections.<AtlasStructDef.AtlasConstraintDef>emptyList());
        aTag.addAttribute(attributeDef);

        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.setClassificationDefs(Arrays.asList(aTag));

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

        aTag = new AtlasClassificationDef("testTag");
        attributeDef = new AtlasAttributeDef("testAttribute", "int", true,
                AtlasAttributeDef.Cardinality.SINGLE, 0, 1,
                false, true, false,
                Collections.<AtlasStructDef.AtlasConstraintDef>emptyList());
        aTag.addAttribute(attributeDef);
        typesDef.setClassificationDefs(Arrays.asList(aTag));

        try {
            typeDefStore.createTypesDef(typesDef);
        } catch (AtlasBaseException e) {
            fail("Tag re-creation should've succeeded");
        }
    }

    @Test(dependsOnMethods = "testGet")
    public void testTypeRegistryIsUpdatedAfterGraphStorage() throws AtlasBaseException {
      String classificationDef = "{"
          + "\"name\":\"test_classification_11\","
          + "\"description\":\"\","
          + "\"createdBy\":\"admin\","
          + "\"superTypes\":[],"
          + "\"attributeDefs\":[{"
          + "\"name\":\"test_class_11\","
          + "\"typeName\":\"string\","
          + "\"isOptional\":true,"
          + "\"isUnique\":true,"
          + "\"isIndexable\":true,"
          + "\"cardinality\":\"SINGLE\","
          + "\"valuesMinCount\":0,"
          + "\"valuesMaxCount\":1}]}";

      String jsonStr = "{"
          + "\"classificationDefs\":[" + classificationDef + "],"
          + "\"entityDefs\":[],"
          + "\"enumDefs\":[],"
          + "\"structDefs\":[]}";

      // create type from json string
      AtlasTypesDef testTypesDefFromJson = AtlasType.fromJson(jsonStr, AtlasTypesDef.class);
      AtlasTypesDef createdTypesDef = typeDefStore.createTypesDef(testTypesDefFromJson);
      // check returned type
      assertEquals("test_classification_11", createdTypesDef.getClassificationDefs().get(0).getName());
      assertTrue(createdTypesDef.getClassificationDefs().get(0).getAttributeDefs().get(0).getIsIndexable());
      // save guid
      String guid = createdTypesDef.getClassificationDefs().get(0).getGuid();
      Date createdTime = createdTypesDef.getClassificationDefs().get(0).getCreateTime();

      // get created type and check again
      AtlasClassificationDef getBackFromCache = typeDefStore.getClassificationDefByName("test_classification_11");
      assertEquals("test_classification_11", getBackFromCache.getName());
      assertTrue(getBackFromCache.getAttributeDefs().get(0).getIsIndexable());
      assertEquals(guid, getBackFromCache.getGuid());
      assertNotNull(getBackFromCache.getCreatedBy());
      assertEquals(createdTime, getBackFromCache.getCreateTime());

      // update type, change isIndexable, check the update result
      testTypesDefFromJson = AtlasType.fromJson(jsonStr, AtlasTypesDef.class);
      testTypesDefFromJson.getClassificationDefs().get(0).getAttributeDefs().get(0).setIsIndexable(false);
      AtlasTypesDef updatedTypesDef = typeDefStore.updateTypesDef(testTypesDefFromJson);
      assertEquals("test_classification_11", updatedTypesDef.getClassificationDefs().get(0).getName());
      assertFalse(updatedTypesDef.getClassificationDefs().get(0).getAttributeDefs().get(0).getIsIndexable());
      assertEquals(guid, updatedTypesDef.getClassificationDefs().get(0).getGuid());
      assertEquals(createdTime, updatedTypesDef.getClassificationDefs().get(0).getCreateTime());

      // get updated type (both by name and guid) and check again
      getBackFromCache = typeDefStore.getClassificationDefByName("test_classification_11");
      assertEquals("test_classification_11", getBackFromCache.getName());
      assertFalse(getBackFromCache.getAttributeDefs().get(0).getIsIndexable());
      assertEquals(guid, getBackFromCache.getGuid());
      assertEquals(createdTime, getBackFromCache.getCreateTime());
      getBackFromCache = typeDefStore.getClassificationDefByGuid(guid);
      assertEquals("test_classification_11", getBackFromCache.getName());
      assertFalse(getBackFromCache.getAttributeDefs().get(0).getIsIndexable());
      assertEquals(guid, getBackFromCache.getGuid());
      assertEquals(createdTime, getBackFromCache.getCreateTime());
    }

    @Test
    public void testGetOnAllEntityTypes() throws AtlasBaseException {
        AtlasEntityDef entityDefByName = typeDefStore.getEntityDefByName("_ALL_ENTITY_TYPES");

        assertNotNull(entityDefByName);
        assertEquals(entityDefByName, AtlasEntityType.getEntityRoot().getEntityDef());
    }

    @Test
    public void testGetOnAllClassificationTypes() throws AtlasBaseException {
        AtlasClassificationDef classificationTypeDef = typeDefStore.getClassificationDefByName("_ALL_CLASSIFICATION_TYPES");

        assertNotNull(classificationTypeDef);
        assertEquals(classificationTypeDef, AtlasClassificationType.getClassificationRoot().getClassificationDef());
    }
}