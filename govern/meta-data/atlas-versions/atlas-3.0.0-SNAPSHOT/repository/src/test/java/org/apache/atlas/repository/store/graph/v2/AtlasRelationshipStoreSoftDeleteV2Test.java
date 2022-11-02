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
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.DeleteType;

import java.util.List;

import static org.apache.atlas.type.AtlasTypeUtil.getAtlasObjectId;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;


/**
 * Inverse reference update test with SoftDeleteHandlerV1
 */
public class AtlasRelationshipStoreSoftDeleteV2Test extends AtlasRelationshipStoreV2Test {

    public AtlasRelationshipStoreSoftDeleteV2Test() {
        super(DeleteType.SOFT);
    }

    @Override
    protected void verifyRelationshipAttributeUpdate_NonComposite_OneToMany(AtlasEntity jane) throws Exception {
        // Max is still in the subordinates list, as the edge still exists with state DELETED
        verifyRelationshipAttributeList(jane, "subordinates", ImmutableList.of(employeeNameIdMap.get("John"), employeeNameIdMap.get("Max")));
    }

    @Override
    protected void verifyRelationshipAttributeUpdate_NonComposite_ManyToOne(AtlasEntity a1, AtlasEntity a2,
                                                                            AtlasEntity a3, AtlasEntity b) {

        verifyRelationshipAttributeValue(a1, "oneB", b.getGuid());

        verifyRelationshipAttributeValue(a2, "oneB", b.getGuid());

        verifyRelationshipAttributeList(b, "manyA", ImmutableList.of(getAtlasObjectId(a1), getAtlasObjectId(a2), getAtlasObjectId(a3)));
    }

    @Override
    protected void verifyRelationshipAttributeUpdate_NonComposite_OneToOne(AtlasEntity a1, AtlasEntity b) {
        verifyRelationshipAttributeValue(a1, "b", b.getGuid());
    }

    @Override
    protected void verifyRelationshipAttributeUpdate_ManyToMany_Friends(AtlasEntity max, AtlasEntity julius, AtlasEntity mike, AtlasEntity john) throws Exception {
        AtlasObjectId johnId   = employeeNameIdMap.get("John");
        AtlasObjectId mikeId   = employeeNameIdMap.get("Mike");
        AtlasObjectId juliusId = employeeNameIdMap.get("Julius");
        AtlasObjectId maxId    = employeeNameIdMap.get("Max");

        // Max's updated friends: [Julius, John, Mike(soft deleted)]
        List<AtlasObjectId> maxFriendsIds = toAtlasObjectIds(max.getRelationshipAttribute("friends"));
        assertNotNull(maxFriendsIds);
        assertEquals(maxFriendsIds.size(), 3);
        assertObjectIdsContains(maxFriendsIds, johnId);
        assertObjectIdsContains(maxFriendsIds, juliusId);
        assertObjectIdsContains(maxFriendsIds, mikeId);

        // Julius's updated friends: [Max]
        List<AtlasObjectId> juliusFriendsIds = toAtlasObjectIds(julius.getRelationshipAttribute("friends"));
        assertNotNull(juliusFriendsIds);
        assertEquals(juliusFriendsIds.size(), 1);
        assertObjectIdsContains(juliusFriendsIds, maxId);

        // Mike's updated friends: [John, Max(soft deleted)]
        List<AtlasObjectId> mikeFriendsIds = toAtlasObjectIds(mike.getRelationshipAttribute("friends"));
        assertNotNull(mikeFriendsIds);
        assertEquals(mikeFriendsIds.size(), 2);
        assertObjectIdsContains(mikeFriendsIds, johnId);
        assertObjectIdsContains(mikeFriendsIds, maxId);

        // John's updated friends: [Max, Mike]
        List<AtlasObjectId> johnFriendsIds = toAtlasObjectIds(john.getRelationshipAttribute("friends"));
        assertNotNull(johnFriendsIds);
        assertEquals(johnFriendsIds.size(), 2);
        assertObjectIdsContains(johnFriendsIds, maxId);
        assertObjectIdsContains(johnFriendsIds, mikeId);
    }

    protected void verifyRelationshipAttributeUpdate_OneToOne_Sibling(AtlasEntity julius, AtlasEntity jane, AtlasEntity mike) throws Exception {
        AtlasObjectId juliusId = employeeNameIdMap.get("Julius");
        AtlasObjectId mikeId   = employeeNameIdMap.get("Mike");

        // Julius sibling updated to Mike
        AtlasObjectId juliusSiblingId = toAtlasObjectId(julius.getRelationshipAttribute("sibling"));
        assertNotNull(juliusSiblingId);
        assertObjectIdEquals(juliusSiblingId, mikeId);

        // Mike's sibling is Julius
        AtlasObjectId mikeSiblingId = toAtlasObjectId(mike.getRelationshipAttribute("sibling"));
        assertNotNull(mikeSiblingId);
        assertObjectIdEquals(mikeSiblingId, juliusId);

        // Jane's sibling is still Julius (soft delete)
        AtlasObjectId janeSiblingId = toAtlasObjectId(jane.getRelationshipAttribute("sibling"));
        assertNotNull(janeSiblingId);
        assertObjectIdEquals(janeSiblingId, juliusId);
    }
}