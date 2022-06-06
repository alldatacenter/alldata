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
package org.apache.atlas.model.typedef;

import org.apache.atlas.model.ModelTestUtil;
import org.apache.atlas.type.AtlasType;
import org.testng.annotations.Test;

import java.util.HashSet;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;


public class TestAtlasEntityDef {

    @Test
    public void testEntityDefSerDeEmpty() {
        AtlasEntityDef entityDef = new AtlasEntityDef("emptyEntityDef");

        String jsonString = AtlasType.toJson(entityDef);

        AtlasEntityDef entityDef2 = AtlasType.fromJson(jsonString, AtlasEntityDef.class);

        assertEquals(entityDef2, entityDef, "Incorrect serialization/deserialization of AtlasEntityDef");
    }

    @Test
    public void testEntityDefSerDe() {
        AtlasEntityDef entityDef = ModelTestUtil.getEntityDef();

        String jsonString = AtlasType.toJson(entityDef);

        AtlasEntityDef entityDef2 = AtlasType.fromJson(jsonString, AtlasEntityDef.class);

        assertEquals(entityDef2, entityDef, "Incorrect serialization/deserialization of AtlasEntityDef");
    }

    @Test
    public void testEntityDefSerDeWithSuperType() {
        AtlasEntityDef entityDef = ModelTestUtil.getEntityDefWithSuperType();

        String jsonString = AtlasType.toJson(entityDef);

        AtlasEntityDef entityDef2 = AtlasType.fromJson(jsonString, AtlasEntityDef.class);

        assertEquals(entityDef2, entityDef, "Incorrect serialization/deserialization of AtlasEntityDef with superType");
    }

    @Test
    public void testEntityDefSerDeWithSuperTypes() {
        AtlasEntityDef entityDef = ModelTestUtil.getEntityDefWithSuperTypes();

        String jsonString = AtlasType.toJson(entityDef);

        AtlasEntityDef entityDef2 = AtlasType.fromJson(jsonString, AtlasEntityDef.class);

        assertEquals(entityDef2, entityDef,
                     "Incorrect serialization/deserialization of AtlasEntityDef with superTypes");
    }

    @Test
    public void testEntityDefAddSuperType() {
        AtlasEntityDef entityDef = ModelTestUtil.newEntityDef();

        String newSuperType = "newType-abcd-1234";
        entityDef.addSuperType(newSuperType);

        assertTrue(entityDef.hasSuperType(newSuperType));

        entityDef.removeSuperType(newSuperType);
    }

    @Test
    public void testEntityDefRemoveElement() {
        AtlasEntityDef entityDef = ModelTestUtil.newEntityDefWithSuperTypes();

        for (String superType : entityDef.getSuperTypes()) {
            entityDef.removeSuperType(superType);
            assertFalse(entityDef.hasSuperType(superType));
        }
    }

    @Test
    public void testEntityDefSetSuperTypes() {
        AtlasEntityDef entityDef = ModelTestUtil.newEntityDefWithSuperTypes();

        Set<String> oldSuperTypes = entityDef.getSuperTypes();
        Set<String> newSuperTypes = new HashSet<>();

        newSuperTypes.add("newType-abcd-1234");

        entityDef.setSuperTypes(newSuperTypes);

        for (String superType : oldSuperTypes) {
            assertFalse(entityDef.hasSuperType(superType));
        }

        for (String superType : newSuperTypes) {
            assertTrue(entityDef.hasSuperType(superType));
        }

        // restore old sypertypes
        entityDef.setSuperTypes(oldSuperTypes);
    }

    @Test
    public void testEntityDefHasSuperTypeWithNoSuperType() {
        AtlasEntityDef entityDef = ModelTestUtil.getEntityDef();

        for (String superType : entityDef.getSuperTypes()) {
            assertTrue(entityDef.hasSuperType(superType));
        }

        assertFalse(entityDef.hasSuperType("01234-xyzabc-;''-)("));
    }

    @Test
    public void testEntityDefHasSuperTypeWithNoSuperTypes() {
        AtlasEntityDef entityDef = ModelTestUtil.getEntityDefWithSuperTypes();

        for (String superType : entityDef.getSuperTypes()) {
            assertTrue(entityDef.hasSuperType(superType));
        }

        assertFalse(entityDef.hasSuperType("01234-xyzabc-;''-)("));
    }
}
