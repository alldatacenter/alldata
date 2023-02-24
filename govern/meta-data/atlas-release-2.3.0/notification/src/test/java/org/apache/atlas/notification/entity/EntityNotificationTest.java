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

package org.apache.atlas.notification.entity;

import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.v1.model.notification.EntityNotificationV1;
import org.apache.atlas.v1.model.notification.EntityNotificationV1.OperationType;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * EntityNotificationV1 tests.
 */
public class EntityNotificationTest {

    @Test
    public void testGetEntity() throws Exception {
        Referenceable        entity             = getEntity("id");
        EntityNotificationV1 entityNotification = new EntityNotificationV1(entity, OperationType.ENTITY_CREATE, Collections.<Struct>emptyList());

        assertEquals(entity, entityNotification.getEntity());
    }

    @Test
    public void testGetOperationType() throws Exception {
        Referenceable        entity             = getEntity("id");
        EntityNotificationV1 entityNotification = new EntityNotificationV1(entity, OperationType.ENTITY_CREATE, Collections.<Struct>emptyList());

        assertEquals(EntityNotificationV1.OperationType.ENTITY_CREATE, entityNotification.getOperationType());
    }

    @Test
    public void testGetAllTraits() throws Exception {
        Referenceable entity    = getEntity("id");
        String        traitName = "MyTrait";
        List<Struct>  traitInfo = Collections.singletonList(new Struct(traitName, Collections.<String, Object>emptyMap()));

        EntityNotificationV1 entityNotification = new EntityNotificationV1(entity, OperationType.TRAIT_ADD, traitInfo);

        assertEquals(traitInfo, entityNotification.getAllTraits());
    }

    @Test
    public void testGetAllTraitsSuperTraits() throws Exception {
        AtlasTypeRegistry       typeRegistry        = mock(AtlasTypeRegistry.class);
        String                  traitName           = "MyTrait";
        Struct                  myTrait             = new Struct(traitName);
        String                  superTraitName      = "MySuperTrait";
        AtlasClassificationType traitType           = mock(AtlasClassificationType.class);
        Set<String>             superTypeNames      = Collections.singleton(superTraitName);
        AtlasClassificationType superTraitType      = mock(AtlasClassificationType.class);
        Set<String>             superSuperTypeNames = Collections.emptySet();
        Referenceable           entity              = getEntity("id", myTrait);

        when(typeRegistry.getClassificationTypeByName(traitName)).thenReturn(traitType);
        when(typeRegistry.getClassificationTypeByName(superTraitName)).thenReturn(superTraitType);

        when(traitType.getAllSuperTypes()).thenReturn(superTypeNames);
        when(superTraitType.getAllSuperTypes()).thenReturn(superSuperTypeNames);

        EntityNotificationV1 entityNotification = new EntityNotificationV1(entity, OperationType.TRAIT_ADD, typeRegistry);

        List<Struct> allTraits = entityNotification.getAllTraits();

        assertEquals(2, allTraits.size());

        for (Struct trait : allTraits) {
            String typeName = trait.getTypeName();

            assertTrue(typeName.equals(traitName) || typeName.equals(superTraitName));
        }
    }

    @Test
    public void testEquals() throws Exception {
        Referenceable        entity              = getEntity("id");
        EntityNotificationV1 entityNotification2 = new EntityNotificationV1(entity, OperationType.ENTITY_CREATE, Collections.<Struct>emptyList());
        EntityNotificationV1 entityNotification  = new EntityNotificationV1(entity, OperationType.ENTITY_CREATE, Collections.<Struct>emptyList());

        assertTrue(entityNotification.equals(entityNotification2));
        assertTrue(entityNotification2.equals(entityNotification));
    }

    public static Referenceable getEntity(String id, Struct... traits) {
        String              typeName   = "typeName";
        List<String>        traitNames = new LinkedList<>();
        Map<String, Struct> traitMap   = new HashMap<>();

        for (Struct trait : traits) {
            String traitName = trait.getTypeName();

            traitNames.add(traitName);
            traitMap.put(traitName, trait);
        }

        return new Referenceable(id, typeName, new HashMap<String, Object>(), traitNames, traitMap);
    }
}
