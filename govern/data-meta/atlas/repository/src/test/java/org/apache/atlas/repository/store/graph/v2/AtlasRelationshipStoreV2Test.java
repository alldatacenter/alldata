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
import org.apache.atlas.RequestContext;
import org.apache.atlas.TestModules;
import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntitiesWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntity.AtlasEntityWithExtInfo;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.graph.GraphBackedSearchIndexer;
import org.apache.atlas.repository.graphdb.AtlasGraph;
import org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.AtlasRelationshipStore;
import org.apache.atlas.repository.store.graph.v1.DeleteHandlerDelegate;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.DeleteType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.commons.collections.CollectionUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.TestRelationshipUtilsV2.EMPLOYEE_TYPE;
import static org.apache.atlas.TestRelationshipUtilsV2.getDepartmentEmployeeInstances;
import static org.apache.atlas.TestRelationshipUtilsV2.getDepartmentEmployeeTypes;
import static org.apache.atlas.TestRelationshipUtilsV2.getInverseReferenceTestTypes;
import static org.apache.atlas.TestUtilsV2.NAME;
import static org.apache.atlas.type.AtlasTypeUtil.getAtlasObjectId;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public abstract class AtlasRelationshipStoreV2Test extends AtlasTestBase {

    @Inject
    AtlasTypeRegistry typeRegistry;

    @Inject
    AtlasTypeDefStore typeDefStore;

    @Inject
    DeleteHandlerDelegate deleteDelegate;

    @Inject
    EntityGraphMapper graphMapper;

    @Inject
    AtlasEntityChangeNotifier entityNotifier;

    @Inject
    AtlasGraph atlasGraph;

    AtlasEntityStore          entityStore;
    AtlasRelationshipStore    relationshipStore;
    AtlasEntityChangeNotifier mockChangeNotifier = mock(AtlasEntityChangeNotifier.class);
    private final DeleteType  deleteType;

    protected Map<String, AtlasObjectId> employeeNameIdMap = new HashMap<>();

    protected AtlasRelationshipStoreV2Test(DeleteType delteType) {
        this.deleteType = delteType;
    }

    @BeforeClass
    public void setUp() throws Exception {
        super.initialize();

        new GraphBackedSearchIndexer(typeRegistry);

        // create employee relationship types
        AtlasTypesDef employeeTypes = getDepartmentEmployeeTypes();
        typeDefStore.createTypesDef(employeeTypes);

        AtlasEntitiesWithExtInfo employeeInstances = getDepartmentEmployeeInstances();
        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(employeeInstances), false);

        for (AtlasEntityHeader entityHeader : response.getCreatedEntities()) {
            employeeNameIdMap.put((String) entityHeader.getAttribute(NAME), getAtlasObjectId(entityHeader));
        }

        init();
        AtlasTypesDef typesDef = getInverseReferenceTestTypes();

        AtlasTypesDef typesToCreate = AtlasTypeDefStoreInitializer.getTypesToCreate(typesDef, typeRegistry);

        if (!typesToCreate.isEmpty()) {
            typeDefStore.createTypesDef(typesToCreate);
        }
    }

    @BeforeTest
    public void init() throws Exception {
        entityStore       = new AtlasEntityStoreV2(atlasGraph, deleteDelegate, typeRegistry, mockChangeNotifier, graphMapper);
        relationshipStore = new AtlasRelationshipStoreV2(atlasGraph, typeRegistry, deleteDelegate, entityNotifier);

        RequestContext.clear();
        RequestContext.get().setUser(TestUtilsV2.TEST_USER, null);
        RequestContext.get().setDeleteType(deleteType);
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @Test
    public void testDepartmentEmployeeEntitiesUsingRelationship() throws Exception  {
        AtlasObjectId hrId     = employeeNameIdMap.get("hr");
        AtlasObjectId maxId    = employeeNameIdMap.get("Max");
        AtlasObjectId johnId   = employeeNameIdMap.get("John");
        AtlasObjectId juliusId = employeeNameIdMap.get("Julius");
        AtlasObjectId janeId   = employeeNameIdMap.get("Jane");
        AtlasObjectId mikeId   = employeeNameIdMap.get("Mike");

        AtlasEntity hrDept = getEntityFromStore(hrId.getGuid());
        AtlasEntity max    = getEntityFromStore(maxId.getGuid());
        AtlasEntity john   = getEntityFromStore(johnId.getGuid());
        AtlasEntity julius = getEntityFromStore(juliusId.getGuid());
        AtlasEntity jane   = getEntityFromStore(janeId.getGuid());
        AtlasEntity mike   = getEntityFromStore(mikeId.getGuid());

        // Department relationship attributes
        List<AtlasObjectId> deptEmployees = toAtlasObjectIds(hrDept.getRelationshipAttribute("employees"));
        assertNotNull(deptEmployees);
        assertEquals(deptEmployees.size(), 5);
        assertObjectIdsContains(deptEmployees, maxId);
        assertObjectIdsContains(deptEmployees, johnId);
        assertObjectIdsContains(deptEmployees, juliusId);
        assertObjectIdsContains(deptEmployees, janeId);
        assertObjectIdsContains(deptEmployees, mikeId);

        // Max employee validation
        AtlasObjectId maxDepartmentId = toAtlasObjectId(max.getRelationshipAttribute("department"));
        assertNotNull(maxDepartmentId);
        assertObjectIdEquals(maxDepartmentId, hrId);

        AtlasObjectId maxManagerId = toAtlasObjectId(max.getRelationshipAttribute("manager"));
        assertNotNull(maxManagerId);
        assertObjectIdEquals(maxManagerId, janeId);

        List<AtlasObjectId> maxMentorsId = toAtlasObjectIds(max.getRelationshipAttribute("mentors"));
        assertNotNull(maxMentorsId);
        assertEquals(maxMentorsId.size(), 1);
        assertObjectIdEquals(maxMentorsId.get(0), juliusId);

        List<AtlasObjectId> maxMenteesId = toAtlasObjectIds(max.getRelationshipAttribute("mentees"));
        assertNotNull(maxMenteesId);
        assertEquals(maxMenteesId.size(), 1);
        assertObjectIdEquals(maxMenteesId.get(0), johnId);

        List<AtlasObjectId> maxFriendsIds = toAtlasObjectIds(max.getRelationshipAttribute("friends"));
        assertNotNull(maxFriendsIds);
        assertEquals(maxFriendsIds.size(), 2);
        assertObjectIdsContains(maxFriendsIds, mikeId);
        assertObjectIdsContains(maxFriendsIds, johnId);

        // John Employee validation
        AtlasObjectId johnDepartmentId = toAtlasObjectId(john.getRelationshipAttribute("department"));
        assertNotNull(johnDepartmentId);
        assertObjectIdEquals(johnDepartmentId, hrId);

        AtlasObjectId johnManagerId = toAtlasObjectId(john.getRelationshipAttribute("manager"));
        assertNotNull(johnManagerId);
        assertObjectIdEquals(johnManagerId, janeId);

        List<AtlasObjectId> johnMentorIds = toAtlasObjectIds(john.getRelationshipAttribute("mentors"));
        assertNotNull(johnMentorIds);
        assertEquals(johnMentorIds.size(), 2);
        assertObjectIdsContains(johnMentorIds, maxId);
        assertObjectIdsContains(johnMentorIds, juliusId);

        List<AtlasObjectId> johnMenteesId = toAtlasObjectIds(john.getRelationshipAttribute("mentees"));
        assertEmpty(johnMenteesId);

        List<AtlasObjectId> johnFriendsIds = toAtlasObjectIds(john.getRelationshipAttribute("friends"));
        assertNotNull(johnFriendsIds);
        assertEquals(johnFriendsIds.size(), 2);
        assertObjectIdsContains(johnFriendsIds, mikeId);
        assertObjectIdsContains(johnFriendsIds, maxId);

        // Mike Employee validation
        AtlasObjectId mikeDepartmentId = toAtlasObjectId(mike.getRelationshipAttribute("department"));
        assertNotNull(mikeDepartmentId);
        assertObjectIdEquals(mikeDepartmentId, hrId);

        AtlasObjectId mikeManagerId = toAtlasObjectId(mike.getRelationshipAttribute("manager"));
        assertNotNull(mikeManagerId);
        assertObjectIdEquals(mikeManagerId, juliusId);

        List<AtlasObjectId> mikeMentorIds = toAtlasObjectIds(mike.getRelationshipAttribute("mentors"));
        assertEmpty(mikeMentorIds);

        List<AtlasObjectId> mikeMenteesId = toAtlasObjectIds(mike.getRelationshipAttribute("mentees"));
        assertEmpty(mikeMenteesId);

        List<AtlasObjectId> mikeFriendsIds = toAtlasObjectIds(mike.getRelationshipAttribute("friends"));
        assertNotNull(mikeFriendsIds);
        assertEquals(mikeFriendsIds.size(), 2);
        assertObjectIdsContains(mikeFriendsIds, maxId);
        assertObjectIdsContains(mikeFriendsIds, johnId);

        // Jane Manager validation
        AtlasObjectId janeDepartmentId = toAtlasObjectId(jane.getRelationshipAttribute("department"));
        assertNotNull(janeDepartmentId);
        assertObjectIdEquals(janeDepartmentId, hrId);

        AtlasObjectId janeManagerId = toAtlasObjectId(jane.getRelationshipAttribute("manager"));
        assertNull(janeManagerId);

        List<AtlasObjectId> janeMentorIds = toAtlasObjectIds(jane.getRelationshipAttribute("mentors"));
        assertEmpty(janeMentorIds);

        List<AtlasObjectId> janeMenteesId = toAtlasObjectIds(jane.getRelationshipAttribute("mentees"));
        assertEmpty(janeMenteesId);

        List<AtlasObjectId> janeSubordinateIds = toAtlasObjectIds(jane.getRelationshipAttribute("subordinates"));
        assertNotNull(janeSubordinateIds);
        assertEquals(janeSubordinateIds.size(), 2);
        assertObjectIdsContains(janeSubordinateIds, maxId);
        assertObjectIdsContains(janeSubordinateIds, johnId);

        List<AtlasObjectId> janeFriendsIds = toAtlasObjectIds(jane.getRelationshipAttribute("friends"));
        assertEmpty(janeFriendsIds);

        AtlasObjectId janeSiblingId = toAtlasObjectId(jane.getRelationshipAttribute("sibling"));
        assertNotNull(janeSiblingId);
        assertObjectIdEquals(janeSiblingId, juliusId);

        // Julius Manager validation
        AtlasObjectId juliusDepartmentId = toAtlasObjectId(julius.getRelationshipAttribute("department"));
        assertNotNull(juliusDepartmentId);
        assertObjectIdEquals(juliusDepartmentId, hrId);

        AtlasObjectId juliusManagerId = toAtlasObjectId(julius.getRelationshipAttribute("manager"));
        assertNull(juliusManagerId);

        List<AtlasObjectId> juliusMentorIds = toAtlasObjectIds(julius.getRelationshipAttribute("mentors"));
        assertEmpty(juliusMentorIds);

        List<AtlasObjectId> juliusMenteesId = toAtlasObjectIds(julius.getRelationshipAttribute("mentees"));
        assertNotNull(juliusMenteesId);
        assertEquals(juliusMenteesId.size(), 2);
        assertObjectIdsContains(juliusMenteesId, maxId);
        assertObjectIdsContains(juliusMenteesId, johnId);

        List<AtlasObjectId> juliusSubordinateIds = toAtlasObjectIds(julius.getRelationshipAttribute("subordinates"));
        assertNotNull(juliusSubordinateIds);
        assertEquals(juliusSubordinateIds.size(), 1);
        assertObjectIdsContains(juliusSubordinateIds, mikeId);

        List<AtlasObjectId> juliusFriendsIds = toAtlasObjectIds(julius.getRelationshipAttribute("friends"));
        assertEmpty(juliusFriendsIds);

        AtlasObjectId juliusSiblingId = toAtlasObjectId(julius.getRelationshipAttribute("sibling"));
        assertNotNull(juliusSiblingId);
        assertObjectIdEquals(juliusSiblingId, janeId);
    }

    // Seeing intermittent failures with janus profile, disabling it until its fixed.
    @Test(enabled = false)
    public void testRelationshipAttributeUpdate_NonComposite_OneToMany() throws Exception {
        AtlasObjectId maxId    = employeeNameIdMap.get("Max");
        AtlasObjectId juliusId = employeeNameIdMap.get("Julius");
        AtlasObjectId janeId   = employeeNameIdMap.get("Jane");
        AtlasObjectId mikeId   = employeeNameIdMap.get("Mike");
        AtlasObjectId johnId   = employeeNameIdMap.get("John");

        // Change Max's Employee.manager reference to Julius and apply the change as a partial update.
        // This should also update Julius to add Max to the inverse Manager.subordinates reference.
        AtlasEntity maxEntityForUpdate = new AtlasEntity(EMPLOYEE_TYPE);
        maxEntityForUpdate.setRelationshipAttribute("manager", juliusId);

        AtlasEntityType        employeeType   = typeRegistry.getEntityTypeByName(EMPLOYEE_TYPE);
        Map<String, Object>    uniqAttributes = Collections.<String, Object>singletonMap("name", "Max");
        EntityMutationResponse updateResponse = entityStore.updateByUniqueAttributes(employeeType, uniqAttributes , new AtlasEntityWithExtInfo(maxEntityForUpdate));

        List<AtlasEntityHeader> partialUpdatedEntities = updateResponse.getPartialUpdatedEntities();
        assertEquals(partialUpdatedEntities.size(), 3);
        // 3 entities should have been updated:
        // * Max to change the Employee.manager reference
        // * Julius to add Max to Manager.subordinates
        // * Jane to remove Max from Manager.subordinates

        AtlasEntitiesWithExtInfo updatedEntities = entityStore.getByIds(ImmutableList.of(maxId.getGuid(), juliusId.getGuid(), janeId.getGuid()));

        // Max's manager updated as Julius
        AtlasEntity maxEntity = updatedEntities.getEntity(maxId.getGuid());
        verifyRelationshipAttributeValue(maxEntity, "manager", juliusId.getGuid());

        // Max added to the subordinate list of Julius, existing subordinate is Mike
        AtlasEntity juliusEntity = updatedEntities.getEntity(juliusId.getGuid());
        verifyRelationshipAttributeList(juliusEntity, "subordinates", ImmutableList.of(maxId, mikeId));

        // Max removed from the subordinate list of Julius
        AtlasEntity janeEntity = updatedEntities.getEntity(janeId.getGuid());

        // Jane's subordinates list includes John and Max for soft delete
        // Jane's subordinates list includes only John for hard delete
        verifyRelationshipAttributeUpdate_NonComposite_OneToMany(janeEntity);

        // Remove Mike from Max's friends list
        // Max's current friends: [Mike, John]
        // Max's updated friends: [Julius, John]
        maxEntityForUpdate = new AtlasEntity(EMPLOYEE_TYPE);
        maxEntityForUpdate.setRelationshipAttribute("friends", ImmutableList.of(johnId, juliusId));

        init();
        updateResponse = entityStore.updateByUniqueAttributes(employeeType, uniqAttributes , new AtlasEntityWithExtInfo(maxEntityForUpdate));

        partialUpdatedEntities = updateResponse.getPartialUpdatedEntities();
        assertEquals(partialUpdatedEntities.size(), 3);
        // 3 entities should have been updated:
        // * Max added Julius and removed Mike from Employee.friends
        // * Mike removed Max from Employee.friends
        // * Julius added Max in Employee.friends

        updatedEntities = entityStore.getByIds(ImmutableList.of(maxId.getGuid(), mikeId.getGuid(), johnId.getGuid(), juliusId.getGuid()));

        maxEntity    = updatedEntities.getEntity(maxId.getGuid());
        juliusEntity = updatedEntities.getEntity(juliusId.getGuid());
        AtlasEntity mikeEntity = updatedEntities.getEntity(mikeId.getGuid());
        AtlasEntity johnEntity = updatedEntities.getEntity(johnId.getGuid());

        verifyRelationshipAttributeUpdate_ManyToMany_Friends(maxEntity, juliusEntity, mikeEntity, johnEntity);

        // Remove Julius from Jane's sibling and add Mike as new sibling
        AtlasEntity juliusEntityForUpdate = new AtlasEntity(EMPLOYEE_TYPE);
        juliusEntityForUpdate.setRelationshipAttribute("sibling", mikeId);

        init();
        updateResponse = entityStore.updateByUniqueAttributes(employeeType, Collections.<String, Object>singletonMap("name", "Julius") , new AtlasEntityWithExtInfo(juliusEntityForUpdate));
        partialUpdatedEntities = updateResponse.getPartialUpdatedEntities();
        assertEquals(partialUpdatedEntities.size(), 3);

        updatedEntities = entityStore.getByIds(ImmutableList.of(juliusId.getGuid(), janeId.getGuid(), mikeId.getGuid()));

        juliusEntity = updatedEntities.getEntity(juliusId.getGuid());
        janeEntity   = updatedEntities.getEntity(janeId.getGuid());
        mikeEntity   = updatedEntities.getEntity(mikeId.getGuid());

        verifyRelationshipAttributeUpdate_OneToOne_Sibling(juliusEntity, janeEntity, mikeEntity);
    }

    @Test
    public void testRelationshipAttributeUpdate_NonComposite_ManyToOne() throws Exception {
        AtlasEntity a1 = new AtlasEntity("A");
        a1.setAttribute(NAME, "a1_name");

        AtlasEntity a2 = new AtlasEntity("A");
        a2.setAttribute(NAME, "a2_name");

        AtlasEntity a3 = new AtlasEntity("A");
        a3.setAttribute(NAME, "a3_name");

        AtlasEntity b = new AtlasEntity("B");
        b.setAttribute(NAME, "b_name");

        AtlasEntitiesWithExtInfo entitiesWithExtInfo = new AtlasEntitiesWithExtInfo();
        entitiesWithExtInfo.addEntity(a1);
        entitiesWithExtInfo.addEntity(a2);
        entitiesWithExtInfo.addEntity(a3);
        entitiesWithExtInfo.addEntity(b);
        entityStore.createOrUpdate(new AtlasEntityStream(entitiesWithExtInfo) , false);

        AtlasEntity bPartialUpdate = new AtlasEntity("B");
        bPartialUpdate.setRelationshipAttribute("manyA", ImmutableList.of(getAtlasObjectId(a1), getAtlasObjectId(a2)));

        init();
        EntityMutationResponse response = entityStore.updateByUniqueAttributes(typeRegistry.getEntityTypeByName("B"),
                                                                               Collections.singletonMap(NAME, b.getAttribute(NAME)),
                                                                               new AtlasEntityWithExtInfo(bPartialUpdate));
        // Verify 3 entities were updated:
        // * set b.manyA reference to a1 and a2
        // * set inverse a1.oneB reference to b
        // * set inverse a2.oneB reference to b
        assertEquals(response.getPartialUpdatedEntities().size(), 3);
        AtlasEntitiesWithExtInfo updatedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), a2.getGuid(), b.getGuid()));

        AtlasEntity a1Entity = updatedEntities.getEntity(a1.getGuid());
        verifyRelationshipAttributeValue(a1Entity, "oneB", b.getGuid());

        AtlasEntity a2Entity = updatedEntities.getEntity(a2.getGuid());
        verifyRelationshipAttributeValue(a2Entity, "oneB", b.getGuid());

        AtlasEntity bEntity = updatedEntities.getEntity(b.getGuid());
        verifyRelationshipAttributeList(bEntity, "manyA", ImmutableList.of(getAtlasObjectId(a1), getAtlasObjectId(a2)));


        bPartialUpdate.setRelationshipAttribute("manyA", ImmutableList.of(getAtlasObjectId(a3)));
        init();
        response = entityStore.updateByUniqueAttributes(typeRegistry.getEntityTypeByName("B"),
                                                        Collections.singletonMap(NAME, b.getAttribute(NAME)),
                                                        new AtlasEntityWithExtInfo(bPartialUpdate));
        // Verify 4 entities were updated:
        // * set b.manyA reference to a3
        // * set inverse a3.oneB reference to b
        // * disconnect inverse a1.oneB reference to b
        // * disconnect inverse a2.oneB reference to b
        assertEquals(response.getPartialUpdatedEntities().size(), 4);
        init();

        updatedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), a2.getGuid(), a3.getGuid(), b.getGuid()));
        a1Entity        = updatedEntities.getEntity(a1.getGuid());
        a2Entity        = updatedEntities.getEntity(a2.getGuid());
        bEntity         = updatedEntities.getEntity(b.getGuid());

        AtlasEntity a3Entity = updatedEntities.getEntity(a3.getGuid());
        verifyRelationshipAttributeValue(a3Entity, "oneB", b.getGuid());

        verifyRelationshipAttributeUpdate_NonComposite_ManyToOne(a1Entity, a2Entity, a3Entity, bEntity);
    }

    @Test
    public void testRelationshipAttributeUpdate_NonComposite_OneToOne() throws Exception {
        AtlasEntity a1 = new AtlasEntity("A");
        a1.setAttribute(NAME, "a1_name");

        AtlasEntity a2 = new AtlasEntity("A");
        a2.setAttribute(NAME, "a2_name");

        AtlasEntity b = new AtlasEntity("B");
        b.setAttribute(NAME, "b_name");

        AtlasEntitiesWithExtInfo entitiesWithExtInfo = new AtlasEntitiesWithExtInfo();
        entitiesWithExtInfo.addEntity(a1);
        entitiesWithExtInfo.addEntity(a2);
        entitiesWithExtInfo.addEntity(b);

        EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(entitiesWithExtInfo) , false);

        AtlasEntity partialUpdateB = new AtlasEntity("B");
        partialUpdateB.setRelationshipAttribute("a", getAtlasObjectId(a1));

        init();
        AtlasEntityType bType = typeRegistry.getEntityTypeByName("B");

        response = entityStore.updateByUniqueAttributes(bType, Collections.singletonMap(NAME, b.getAttribute(NAME)), new AtlasEntityWithExtInfo(partialUpdateB));
        List<AtlasEntityHeader> partialUpdatedEntitiesHeader = response.getPartialUpdatedEntities();
        // Verify 2 entities were updated:
        // * set b.a reference to a1
        // * set inverse a1.b reference to b
        assertEquals(partialUpdatedEntitiesHeader.size(), 2);
        AtlasEntitiesWithExtInfo partialUpdatedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), b.getGuid()));

        AtlasEntity a1Entity = partialUpdatedEntities.getEntity(a1.getGuid());
        verifyRelationshipAttributeValue(a1Entity, "b", b.getGuid());

        AtlasEntity bEntity = partialUpdatedEntities.getEntity(b.getGuid());
        verifyRelationshipAttributeValue(bEntity, "a", a1.getGuid());

        init();

        // Update b.a to reference a2.
        partialUpdateB.setRelationshipAttribute("a", getAtlasObjectId(a2));
        response = entityStore.updateByUniqueAttributes(bType, Collections.<String, Object>singletonMap(NAME, b.getAttribute(NAME)), new AtlasEntityWithExtInfo(partialUpdateB));
        partialUpdatedEntitiesHeader = response.getPartialUpdatedEntities();
        // Verify 3 entities were updated:
        // * set b.a reference to a2
        // * set a2.b reference to b
        // * disconnect a1.b reference
        assertEquals(partialUpdatedEntitiesHeader.size(), 3);
        partialUpdatedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), a2.getGuid(), b.getGuid()));

        bEntity = partialUpdatedEntities.getEntity(b.getGuid());
        verifyRelationshipAttributeValue(bEntity, "a", a2.getGuid());

        AtlasEntity a2Entity = partialUpdatedEntities.getEntity(a2.getGuid());
        verifyRelationshipAttributeValue(a2Entity, "b", b.getGuid());

        a1Entity = partialUpdatedEntities.getEntity(a1.getGuid());
        verifyRelationshipAttributeUpdate_NonComposite_OneToOne(a1Entity, bEntity);
    }

    @Test
    public void testRelationshipAttributeUpdate_NonComposite_ManyToMany() throws Exception {
        AtlasEntity a1 = new AtlasEntity("A");
        a1.setAttribute(NAME, "a1_name");

        AtlasEntity a2 = new AtlasEntity("A");
        a2.setAttribute(NAME, "a2_name");

        AtlasEntity a3 = new AtlasEntity("A");
        a3.setAttribute(NAME, "a3_name");

        AtlasEntity b1 = new AtlasEntity("B");
        b1.setAttribute(NAME, "b1_name");

        AtlasEntity b2 = new AtlasEntity("B");
        b2.setAttribute(NAME, "b2_name");

        AtlasEntitiesWithExtInfo entitiesWithExtInfo = new AtlasEntitiesWithExtInfo();
        entitiesWithExtInfo.addEntity(a1);
        entitiesWithExtInfo.addEntity(a2);
        entitiesWithExtInfo.addEntity(a3);
        entitiesWithExtInfo.addEntity(b1);
        entitiesWithExtInfo.addEntity(b2);
        entityStore.createOrUpdate(new AtlasEntityStream(entitiesWithExtInfo) , false);

        AtlasEntity b1PartialUpdate = new AtlasEntity("B");
        b1PartialUpdate.setRelationshipAttribute("manyToManyA", ImmutableList.of(getAtlasObjectId(a1), getAtlasObjectId(a2)));

        init();
        EntityMutationResponse response = entityStore.updateByUniqueAttributes(typeRegistry.getEntityTypeByName("B"),
                                                                               Collections.singletonMap(NAME, b1.getAttribute(NAME)),
                                                                               new AtlasEntityWithExtInfo(b1PartialUpdate));

        List<AtlasEntityHeader> updatedEntityHeaders = response.getPartialUpdatedEntities();
        assertEquals(updatedEntityHeaders.size(), 3);

        AtlasEntitiesWithExtInfo updatedEntities = entityStore.getByIds(ImmutableList.of(a1.getGuid(), a2.getGuid(), b1.getGuid()));

        AtlasEntity b1Entity = updatedEntities.getEntity(b1.getGuid());
        verifyRelationshipAttributeList(b1Entity, "manyToManyA", ImmutableList.of(getAtlasObjectId(a1), getAtlasObjectId(a2)));

        AtlasEntity a1Entity = updatedEntities.getEntity(a1.getGuid());
        verifyRelationshipAttributeList(a1Entity, "manyB", ImmutableList.of(getAtlasObjectId(b1)));

        AtlasEntity a2Entity = updatedEntities.getEntity(a2.getGuid());
        verifyRelationshipAttributeList(a2Entity, "manyB", ImmutableList.of(getAtlasObjectId(b1)));
    }

    @Test
    public void testRelationshipAttributeOnPartialUpdate() throws Exception {
        AtlasObjectId maxId = employeeNameIdMap.get("Max");
        AtlasObjectId janeId = employeeNameIdMap.get("Jane");
        AtlasObjectId mikeId = employeeNameIdMap.get("Mike");
        AtlasObjectId johnId = employeeNameIdMap.get("John");

        // Partial Update Max's Employee.friends reference with Jane and apply the change as a partial update.
        // This should also update friends list of Max and Jane.
        AtlasEntity maxEntityForUpdate = new AtlasEntity(EMPLOYEE_TYPE);
        maxEntityForUpdate.setRelationshipAttribute("friends", Arrays.asList(janeId));

        AtlasEntityType employeeType = typeRegistry.getEntityTypeByName(EMPLOYEE_TYPE);
        Map<String, Object> uniqAttributes = Collections.<String, Object>singletonMap("name", "Max");

        init();
        EntityMutationResponse updateResponse = entityStore.updateByUniqueAttributes(employeeType, uniqAttributes, new AtlasEntityWithExtInfo(maxEntityForUpdate));

        List<AtlasEntityHeader> partialUpdatedEntities = updateResponse.getPartialUpdatedEntities();
        assertEquals(partialUpdatedEntities.size(), 2);
        // 2 entities should have been updated:
        // * Max to add  Employee.friends reference
        // * Jane to add Max from Employee.friends
        AtlasEntitiesWithExtInfo updatedEntities = entityStore.getByIds(ImmutableList.of(maxId.getGuid(), janeId.getGuid()));

        AtlasEntity maxEntity = updatedEntities.getEntity(maxId.getGuid());
        verifyRelationshipAttributeList(maxEntity, "friends", ImmutableList.of(mikeId, johnId, janeId));

        AtlasEntity janeEntity = updatedEntities.getEntity(janeId.getGuid());
        verifyRelationshipAttributeList(janeEntity, "friends", ImmutableList.of(maxId));
    }

    protected abstract void verifyRelationshipAttributeUpdate_NonComposite_OneToOne(AtlasEntity a1, AtlasEntity b);

    protected abstract void verifyRelationshipAttributeUpdate_NonComposite_OneToMany(AtlasEntity entity) throws Exception;

    protected abstract void verifyRelationshipAttributeUpdate_NonComposite_ManyToOne(AtlasEntity a1, AtlasEntity a2, AtlasEntity a3, AtlasEntity b);

    protected abstract void verifyRelationshipAttributeUpdate_ManyToMany_Friends(AtlasEntity e1, AtlasEntity e2, AtlasEntity e3, AtlasEntity e4) throws Exception;

    protected abstract void verifyRelationshipAttributeUpdate_OneToOne_Sibling(AtlasEntity e1, AtlasEntity e2, AtlasEntity e3) throws Exception;

    protected static void assertObjectIdsContains(List<AtlasObjectId> objectIds, AtlasObjectId objectId) {
        assertTrue(CollectionUtils.isNotEmpty(objectIds));
        assertTrue(objectIds.contains(objectId));
    }

    protected static void assertObjectIdEquals(AtlasObjectId objId1, AtlasObjectId objId2) {
        assertTrue(objId1.equals(objId2));
    }

    private static void assertEmpty(List collection) {
        assertTrue(collection != null && collection.isEmpty());
    }

    protected static List<AtlasObjectId> toAtlasObjectIds(Object object) {
        List<AtlasObjectId> ret = new ArrayList<>();

        if (object instanceof List) {
            List<?> objectIds = (List) object;

            if (CollectionUtils.isNotEmpty(objectIds)) {
                for (Object obj : objectIds) {
                    if (obj instanceof AtlasRelatedObjectId) {
                        AtlasRelatedObjectId relatedObjectId = (AtlasRelatedObjectId) obj;
                        ret.add(new AtlasObjectId(relatedObjectId.getGuid(), relatedObjectId.getTypeName(), relatedObjectId.getUniqueAttributes()));
                    }
                }
            }
        }

        return ret;
    }

    protected static AtlasObjectId toAtlasObjectId(Object object) {
        if (object instanceof AtlasRelatedObjectId) {
            AtlasRelatedObjectId relatedObjectId = (AtlasRelatedObjectId) object;
            return new AtlasObjectId(relatedObjectId.getGuid(), relatedObjectId.getTypeName(), relatedObjectId.getUniqueAttributes());
        }

        return null;
    }

    private AtlasEntity getEntityFromStore(String guid) throws AtlasBaseException {
        AtlasEntityWithExtInfo entity = guid != null ? entityStore.getById(guid) : null;

        return entity != null ? entity.getEntity() : null;
    }

    protected static void verifyRelationshipAttributeList(AtlasEntity entity, String relationshipAttrName, List<AtlasObjectId> expectedValues) {
        Object refValue = entity.getRelationshipAttribute(relationshipAttrName);
        assertTrue(refValue instanceof List);

        List<AtlasObjectId> refList = toAtlasObjectIds(refValue);
        assertEquals(refList.size(), expectedValues.size());

        if (expectedValues.size() > 0) {
            assertTrue(refList.containsAll(expectedValues));
        }
    }

    protected static void verifyRelationshipAttributeValue(AtlasEntity entity, String relationshipAttrName, String expectedGuid) {
        Object refValue = entity.getRelationshipAttribute(relationshipAttrName);
        if (expectedGuid == null) {
            assertNull(refValue);
        }
        else {
            assertTrue(refValue instanceof AtlasObjectId);
            AtlasObjectId referencedObjectId = (AtlasObjectId) refValue;
            assertEquals(referencedObjectId.getGuid(), expectedGuid);
        }
    }
}