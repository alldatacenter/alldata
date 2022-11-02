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

package org.apache.atlas.notification;

import org.apache.atlas.v1.model.instance.Referenceable;
import org.apache.atlas.v1.model.instance.Struct;
import org.apache.atlas.type.AtlasClassificationType;
import org.apache.atlas.type.AtlasTypeRegistry;
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

public class NotificationEntityChangeListenerTest {
    @Test
    public void testGetAllTraitsSuperTraits() throws Exception {

        AtlasTypeRegistry typeSystem = mock(AtlasTypeRegistry.class);

        String traitName = "MyTrait";
        Struct myTrait = new Struct(traitName);

        String superTraitName = "MySuperTrait";

        AtlasClassificationType traitDef = mock(AtlasClassificationType.class);
        Set<String> superTypeNames = Collections.singleton(superTraitName);

        AtlasClassificationType superTraitDef = mock(AtlasClassificationType.class);
        Set<String> superSuperTypeNames = Collections.emptySet();

        Referenceable entity = getEntity("id", myTrait);

        when(typeSystem.getClassificationTypeByName(traitName)).thenReturn(traitDef);
        when(typeSystem.getClassificationTypeByName(superTraitName)).thenReturn(superTraitDef);

        when(traitDef.getAllSuperTypes()).thenReturn(superTypeNames);
        when(superTraitDef.getAllSuperTypes()).thenReturn(superSuperTypeNames);

        List<Struct> allTraits = NotificationEntityChangeListener.getAllTraits(entity, typeSystem);

        assertEquals(2, allTraits.size());

        for (Struct trait : allTraits) {
            String typeName = trait.getTypeName();
            assertTrue(typeName.equals(traitName) || typeName.equals(superTraitName));
        }
    }

    private Referenceable getEntity(String id, Struct... traits) {
        String typeName = "typeName";
        Map<String, Object> values = new HashMap<>();

        List<String> traitNames = new LinkedList<>();
        Map<String, Struct> traitMap = new HashMap<>();

        for (Struct trait : traits) {
            String traitName = trait.getTypeName();

            traitNames.add(traitName);
            traitMap.put(traitName, trait);
        }
        return new Referenceable(id, typeName, values, traitNames, traitMap);
    }
}
