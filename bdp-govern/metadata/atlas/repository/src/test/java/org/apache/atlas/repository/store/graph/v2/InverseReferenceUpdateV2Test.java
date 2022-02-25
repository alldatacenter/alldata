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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.DeleteType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeUtil;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import static org.apache.atlas.TestUtilsV2.NAME;

/**
 * Test automatic inverse reference updating in V1 (V2?) code path.
 *
 */
@Guice(modules = TestModules.TestOnlyModule.class)
public abstract class InverseReferenceUpdateV2Test extends AtlasTestBase {
    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    AtlasTypeDefStore typeDefStore;

    @Inject
    AtlasEntityStore entityStore;

    private AtlasEntitiesWithExtInfo deptEntity;
    private final DeleteType         deleteType;

    protected Map<String, AtlasObjectId> nameIdMap = new HashMap<>();

    protected InverseReferenceUpdateV2Test(DeleteType deleteType) {
        this.deleteType = deleteType;
    }

    @BeforeClass
    public void setUp() throws Exception {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);

        super.initialize();

        AtlasTypesDef[] testTypesDefs = new AtlasTypesDef[] { TestUtilsV2.defineDeptEmployeeTypes(),
                                                              TestUtilsV2.defineInverseReferenceTestTypes()
                                                            };

        for (AtlasTypesDef typesDef : testTypesDefs) {
            AtlasTypesDef typesToCreate = AtlasTypeDefStoreInitializer.getTypesToCreate(typesDef, typeRegistry);

            if (!typesToCreate.isEmpty()) {
                typeDefStore.createTypesDef(typesToCreate);
            }
        }

        deptEntity = TestUtilsV2.createDeptEg2();
        init();
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(deptEntity), false);
        for (AtlasEntityHeader entityHeader : response.getCreatedEntities()) {
            nameIdMap.put((String)entityHeader.getAttribute(NAME), AtlasTypeUtil.getAtlasObjectId(entityHeader));
        }
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @BeforeMethod
    public void init() throws Exception {
        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
        RequestContext.get().setDeleteType(deleteType);
    }

    @Test
    public void testInverseReferenceAutoUpdate_NonComposite_OneToMany() throws Exception {
        AtlasObjectId juliusId = nameIdMap.get("Julius");

        // Change Max's Employee.manager reference to Julius and apply the change as a partial update.
        // This should also update Julius to add Max to the inverse Manager.subordinates reference.
        AtlasEntity maxEntityForUpdate = new AtlasEntity(TestUtilsV2.EMPLOYEE_TYPE);
        maxEntityForUpdate.setAttribute("manager", juliusId);
        AtlasEntityType employeeType = typeRegistry.getEntityTypeByName(TestUtilsV2.EMPLOYEE_TYPE);
        Map<String, Object> uniqAttributes = Collections.<String, Object>singletonMap("name", "Max");
        EntityMutationResponse updateResponse = entityStore.updateByUniqueAttributes(employeeType, uniqAttributes , new AtlasEntityWithExtInfo(maxEntityForUpdate));
        List<AtlasEntityHeader> partialUpdatedEntities = updateResponse.getPartialUpdatedEntities();
        // 3 entities should have been updated:
        // * Max to change the Employee.manager reference
        // * Julius to add Max to Manager.subordinates
        // * Jane to remove Max from Manager.subordinates
        assertEquals(partialUpdatedEntities.size(), 3);

        AtlasObjectId maxId = nameIdMap.get("Max");
        String janeGuid = nameIdMap.get("Jane").getGuid();
        AtlasEntitiesWithExtInfo storedEntities = entityStore.getByIds(ImmutableList.of(maxId.getGuid(), juliusId.getGuid(), janeGuid));
        AtlasEntity storedEntity = storedEntities.getEntity(maxId.getGuid());
        verifyReferenceValue(storedEntity, "manager", juliusId.getGuid());
        storedEntity = storedEntities.getEntity(juliusId.getGuid());
        verifyReferenceList(storedEntity, "subordinates", ImmutableList.of(maxId));
        storedEntity = storedEntities.getEntity(janeGuid);
        verify_testInverseReferenceAutoUpdate_NonComposite_OneToMany(storedEntity);
    }

    protected abstract void verify_testInverseReferenceAutoUpdate_NonComposite_OneToMany(AtlasEntity jane) throws Exception;

    @Test
    public void testInverseReferenceAutoUpdate_NonCompositeManyToOne() throws Exception {
        AtlasEntityType bType = typeRegistry.getEntityTypeByName("B");
        AtlasEntity a1 = new AtlasEntity("A");
        a1.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity a2 = new AtlasEntity("A");
        a2.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity a3 = new AtlasEntity("A");
        a3.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity b = new AtlasEntity("B");

        b.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntitiesWithExtInfo atlasEntitiesWithExtInfo = new AtlasEntitiesWithExtInfo();
        atlasEntitiesWithExtInfo.addEntity(a1);
        atlasEntitiesWithExtInfo.addEntity(a2);
        atlasEntitiesWithExtInfo.addEntity(a3);
        atlasEntitiesWithExtInfo.addEntity(b);
        AtlasEntityStream entityStream = new AtlasEntityStream(atlasEntitiesWithExtInfo);
        EntityMutationResponse response = entityStore.createOrUpdate(entityStream , false);

        AtlasEntity bForPartialUpdate = new AtlasEntity("B");
        bForPartialUpdate.setAttribute("manyA", ImmutableList.of(AtlasTypeUtil.getAtlasObjectId(a1), AtlasTypeUtil.getAtlasObjectId(a2)));
        init();
        response = entityStore.updateByUniqueAttributes(bType, Collections.<String, Object>singletonMap(NAME, b.getAttribute(NAME)), new AtlasEntityWithExtInfo(bForPartialUpdate));
        List<AtlasEntityHeader> partialUpdatedEntities = response.getPartialUpdatedEntities();
        // Verify 3 entities were updated:
        // * set b.manyA reference to a1 and a2
        // * set inverse a1.oneB reference to b
        // * set inverse a2.oneB reference to b
        assertEquals(partialUpdatedEntities.size(), 3);
        AtlasEntitiesWithExtInfo storedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), a2.getGuid(), b.getGuid()));
        AtlasEntity storedEntity = storedEntities.getEntity(a1.getGuid());
        verifyReferenceValue(storedEntity, "oneB", b.getGuid());

        storedEntity = storedEntities.getEntity(a2.getGuid());
        verifyReferenceValue(storedEntity, "oneB", b.getGuid());

        storedEntity = storedEntities.getEntity(b.getGuid());
        verifyReferenceList(storedEntity, "manyA", ImmutableList.of(AtlasTypeUtil.getAtlasObjectId(a1), AtlasTypeUtil.getAtlasObjectId(a2)));

        bForPartialUpdate.setAttribute("manyA", ImmutableList.of(AtlasTypeUtil.getAtlasObjectId(a3)));
        init();
        response = entityStore.updateByUniqueAttributes(bType, Collections.<String, Object>singletonMap(NAME, b.getAttribute(NAME)), new AtlasEntityWithExtInfo(bForPartialUpdate));
        partialUpdatedEntities = response.getPartialUpdatedEntities();
        // Verify 4 entities were updated:
        // * set b.manyA reference to a3
        // * set inverse a3.oneB reference to b
        // * disconnect inverse a1.oneB reference to b
        // * disconnect inverse a2.oneB reference to b
        assertEquals(partialUpdatedEntities.size(), 4);

        init();
        storedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), a2.getGuid(), a3.getGuid(), b.getGuid()));
        verifyReferenceValue(storedEntities.getEntity(a3.getGuid()), "oneB", b.getGuid());

        verify_testInverseReferenceAutoUpdate_NonCompositeManyToOne(storedEntities.getEntity(a1.getGuid()), storedEntities.getEntity(a2.getGuid()),
            storedEntities.getEntity(a3.getGuid()), storedEntities.getEntity(b.getGuid()));
    }

    protected abstract void verify_testInverseReferenceAutoUpdate_NonCompositeManyToOne(AtlasEntity a1, AtlasEntity a2, AtlasEntity a3, AtlasEntity b);

    @Test
    public void testInverseReferenceAutoUpdate_NonComposite_OneToOne() throws Exception {
        AtlasEntityType bType = typeRegistry.getEntityTypeByName("B");
        AtlasEntity a1 = new AtlasEntity("A");
        a1.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity a2 = new AtlasEntity("A");
        a2.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity b = new AtlasEntity("B");
        b.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntitiesWithExtInfo atlasEntitiesWithExtInfo = new AtlasEntitiesWithExtInfo();
        atlasEntitiesWithExtInfo.addEntity(a1);
        atlasEntitiesWithExtInfo.addEntity(a2);
        atlasEntitiesWithExtInfo.addEntity(b);
        AtlasEntityStream entityStream = new AtlasEntityStream(atlasEntitiesWithExtInfo);
        EntityMutationResponse response = entityStore.createOrUpdate(entityStream , false);

        AtlasEntity bForPartialUpdate = new AtlasEntity("B");
        bForPartialUpdate.setAttribute("a", AtlasTypeUtil.getAtlasObjectId(a1));
        init();
        response = entityStore.updateByUniqueAttributes(bType, Collections.<String, Object>singletonMap(NAME, b.getAttribute(NAME)), new AtlasEntityWithExtInfo(bForPartialUpdate));
        List<AtlasEntityHeader> partialUpdatedEntities = response.getPartialUpdatedEntities();
        // Verify 2 entities were updated:
        // * set b.a reference to a1
        // * set inverse a1.b reference to b
        assertEquals(partialUpdatedEntities.size(), 2);
        AtlasEntitiesWithExtInfo storedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), b.getGuid()));
        AtlasEntity storedEntity = storedEntities.getEntity(a1.getGuid());
        verifyReferenceValue(storedEntity, "b", b.getGuid());
        storedEntity = storedEntities.getEntity(b.getGuid());
        verifyReferenceValue(storedEntity, "a", a1.getGuid());

        // Update b.a to reference a2.
        bForPartialUpdate.setAttribute("a", AtlasTypeUtil.getAtlasObjectId(a2));
        init();
        response = entityStore.updateByUniqueAttributes(bType, Collections.<String, Object>singletonMap(NAME, b.getAttribute(NAME)), new AtlasEntityWithExtInfo(bForPartialUpdate));
        partialUpdatedEntities = response.getPartialUpdatedEntities();
        // Verify 3 entities were updated:
        // * set b.a reference to a2
        // * set a2.b reference to b
        // * disconnect a1.b reference
        assertEquals(partialUpdatedEntities.size(), 3);
        storedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), a2.getGuid(), b.getGuid()));
        storedEntity = storedEntities.getEntity(a2.getGuid());
        verifyReferenceValue(storedEntity, "b", b.getGuid());
        storedEntity = storedEntities.getEntity(b.getGuid());
        verifyReferenceValue(storedEntity, "a", a2.getGuid());
        storedEntity = storedEntities.getEntity(a1.getGuid());
        Object refValue = storedEntity.getAttribute("b");
        verify_testInverseReferenceAutoUpdate_NonComposite_OneToOne(storedEntities.getEntity(a1.getGuid()), storedEntities.getEntity(b.getGuid()));
    }

    protected abstract void verify_testInverseReferenceAutoUpdate_NonComposite_OneToOne(AtlasEntity a1, AtlasEntity b);

    @Test
    public void testInverseReferenceAutoUpdate_NonComposite_ManyToMany() throws Exception {
        AtlasEntityType bType = typeRegistry.getEntityTypeByName("B");
        AtlasEntity a1 = new AtlasEntity("A");
        a1.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity a2 = new AtlasEntity("A");
        a2.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity a3 = new AtlasEntity("A");
        a3.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity b1 = new AtlasEntity("B");
        b1.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity b2 = new AtlasEntity("B");
        b2.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntitiesWithExtInfo atlasEntitiesWithExtInfo = new AtlasEntitiesWithExtInfo();
        atlasEntitiesWithExtInfo.addEntity(a1);
        atlasEntitiesWithExtInfo.addEntity(a2);
        atlasEntitiesWithExtInfo.addEntity(a3);
        atlasEntitiesWithExtInfo.addEntity(b1);
        atlasEntitiesWithExtInfo.addEntity(b2);
        AtlasEntityStream entityStream = new AtlasEntityStream(atlasEntitiesWithExtInfo);
        EntityMutationResponse response = entityStore.createOrUpdate(entityStream , false);

        AtlasEntity b1ForPartialUpdate = new AtlasEntity("B");
        b1ForPartialUpdate.setAttribute("manyToManyA", ImmutableList.of(AtlasTypeUtil.getAtlasObjectId(a1), AtlasTypeUtil.getAtlasObjectId(a2)));
        init();
        response = entityStore.updateByUniqueAttributes(bType, Collections.<String, Object>singletonMap(NAME, b1.getAttribute(NAME)), new AtlasEntityWithExtInfo(b1ForPartialUpdate));
        List<AtlasEntityHeader> partialUpdatedEntities = response.getPartialUpdatedEntities();
        assertEquals(partialUpdatedEntities.size(), 3);
        AtlasEntitiesWithExtInfo storedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), a2.getGuid(), b1.getGuid()));
        AtlasEntity storedEntity = storedEntities.getEntity(b1.getGuid());
        verifyReferenceList(storedEntity, "manyToManyA", ImmutableList.of(AtlasTypeUtil.getAtlasObjectId(a1), AtlasTypeUtil.getAtlasObjectId(a2)));
        storedEntity = storedEntities.getEntity(a1.getGuid());
        verifyReferenceList(storedEntity, "manyB", ImmutableList.of(AtlasTypeUtil.getAtlasObjectId(b1)));
        storedEntity = storedEntities.getEntity(a2.getGuid());
        verifyReferenceList(storedEntity, "manyB", ImmutableList.of(AtlasTypeUtil.getAtlasObjectId(b1)));
    }

    @Test
    public void testInverseReferenceAutoUpdate_Map() throws Exception {
        AtlasEntity a1 = new AtlasEntity("A");
        a1.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity b1 = new AtlasEntity("B");
        b1.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity b2 = new AtlasEntity("B");
        b2.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntity b3 = new AtlasEntity("B");
        b3.setAttribute(NAME, TestUtilsV2.randomString());
        AtlasEntitiesWithExtInfo atlasEntitiesWithExtInfo = new AtlasEntitiesWithExtInfo();
        atlasEntitiesWithExtInfo.addEntity(a1);
        atlasEntitiesWithExtInfo.addEntity(b1);
        atlasEntitiesWithExtInfo.addEntity(b2);
        atlasEntitiesWithExtInfo.addEntity(b3);
        AtlasEntityStream entityStream = new AtlasEntityStream(atlasEntitiesWithExtInfo);
        EntityMutationResponse response = entityStore.createOrUpdate(entityStream , false);

        AtlasEntityType aType = typeRegistry.getEntityTypeByName("A");
        AtlasEntity aForPartialUpdate = new AtlasEntity("A");
        aForPartialUpdate.setAttribute("mapToB", ImmutableMap.<String, AtlasObjectId>of("b1", AtlasTypeUtil.getAtlasObjectId(b1), "b2", AtlasTypeUtil.getAtlasObjectId(b2)));
        init();
        response = entityStore.updateByUniqueAttributes(aType, Collections.<String, Object>singletonMap(NAME, a1.getAttribute(NAME)), new AtlasEntityWithExtInfo(aForPartialUpdate));
        List<AtlasEntityHeader> partialUpdatedEntities = response.getPartialUpdatedEntities();
        // Verify 3 entities were updated:
        // * set a1.mapToB to "b1"->b1, "b2"->b2
        // * set b1.mappedFromA to a1
        // * set b2.mappedFromA to a1
        assertEquals(partialUpdatedEntities.size(), 3);
        AtlasEntitiesWithExtInfo storedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), b2.getGuid(), b1.getGuid()));
        AtlasEntity storedEntity = storedEntities.getEntity(a1.getGuid());
        Object value = storedEntity.getAttribute("mapToB");
        assertTrue(value instanceof Map);
        Map<String, AtlasObjectId> refMap = (Map<String, AtlasObjectId>) value;
        assertEquals(refMap.size(), 2);
        AtlasObjectId referencedEntityId = refMap.get("b1");
        assertEquals(referencedEntityId, AtlasTypeUtil.getAtlasObjectId(b1));
        referencedEntityId = refMap.get("b2");
        assertEquals(referencedEntityId, AtlasTypeUtil.getAtlasObjectId(b2));
        storedEntity = storedEntities.getEntity(b1.getGuid());
        verifyReferenceValue(storedEntity, "mappedFromA", a1.getGuid());
        storedEntity = storedEntities.getEntity(b2.getGuid());
        verifyReferenceValue(storedEntity, "mappedFromA", a1.getGuid());

        aForPartialUpdate.setAttribute("mapToB", ImmutableMap.<String, AtlasObjectId>of("b3", AtlasTypeUtil.getAtlasObjectId(b3)));
        init();
        response = entityStore.updateByUniqueAttributes(aType, Collections.<String, Object>singletonMap(NAME, a1.getAttribute(NAME)), new AtlasEntityWithExtInfo(aForPartialUpdate));
        partialUpdatedEntities = response.getPartialUpdatedEntities();
        // Verify 4 entities were updated:
        // * set a1.mapToB to "b3"->b3
        // * set b3.mappedFromA to a1
        // * disconnect b1.mappedFromA
        // * disconnect b2.mappedFromA
        assertEquals(partialUpdatedEntities.size(), 4);
        storedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), b2.getGuid(), b1.getGuid(), b3.getGuid()));
        AtlasEntity storedB3 = storedEntities.getEntity(b3.getGuid());
        verifyReferenceValue(storedB3, "mappedFromA", a1.getGuid());
        verify_testInverseReferenceAutoUpdate_Map(storedEntities.getEntity(a1.getGuid()), storedEntities.getEntity(b1.getGuid()), storedEntities.getEntity(b2.getGuid()), storedB3);
    }

    protected abstract void verify_testInverseReferenceAutoUpdate_Map(AtlasEntity a1, AtlasEntity b1, AtlasEntity b2, AtlasEntity b3);

    protected void verifyReferenceValue(AtlasEntity entity, String refName, String expectedGuid) {
        Object refValue = entity.getAttribute(refName);
        if (expectedGuid == null) {
            assertNull(refValue);
        }
        else {
            assertTrue(refValue instanceof AtlasObjectId);
            AtlasObjectId referencedObjectId = (AtlasObjectId) refValue;
            assertEquals(referencedObjectId.getGuid(), expectedGuid);
        }
    }

    protected void verifyReferenceList(AtlasEntity entity, String refName, List<AtlasObjectId> expectedValues) {
        Object refValue = entity.getAttribute(refName);
        assertTrue(refValue instanceof List);
        List<AtlasObjectId> refList = (List<AtlasObjectId>) refValue;
        assertEquals(refList.size(), expectedValues.size());
        if (expectedValues.size() > 0) {
            assertTrue(refList.containsAll(expectedValues));
        }
    }
}
