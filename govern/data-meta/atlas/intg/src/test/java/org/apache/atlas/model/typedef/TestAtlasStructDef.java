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
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.type.AtlasType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class TestAtlasStructDef {

    @Test
    public void testStructDefSerDeEmpty() {
        AtlasStructDef structDef = new AtlasStructDef("emptyStructDef");

        String jsonString = AtlasType.toJson(structDef);

        AtlasStructDef structDef2 = AtlasType.fromJson(jsonString, AtlasStructDef.class);

        assertEquals(structDef2, structDef, "Incorrect serialization/deserialization of AtlasStructDef");
    }

    @Test
    public void testStructDefSerDe() {
        AtlasStructDef structDef = ModelTestUtil.getStructDef();

        String jsonString = AtlasType.toJson(structDef);

        AtlasStructDef structDef2 = AtlasType.fromJson(jsonString, AtlasStructDef.class);

        assertEquals(structDef2, structDef, "Incorrect serialization/deserialization of AtlasStructDef");
    }

    @Test
    public void testStructDefHasAttribute() {
        AtlasStructDef structDef = ModelTestUtil.getStructDef();

        for (AtlasAttributeDef attributeDef : structDef.getAttributeDefs()) {
            assertTrue(structDef.hasAttribute(attributeDef.getName()));
        }

        assertFalse(structDef.hasAttribute("01234-xyzabc-;''-)("));
    }

    @Test
    public void testStructDefAddAttribute() {
        AtlasStructDef structDef = ModelTestUtil.newStructDef();

        structDef.addAttribute(new AtlasAttributeDef("newAttribute", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        assertTrue(structDef.hasAttribute("newAttribute"));
    }

    @Test
    public void testStructDefRemoveAttribute() {
        AtlasStructDef structDef = ModelTestUtil.newStructDef();

        String attrName = structDef.getAttributeDefs().get(0).getName();
        assertTrue(structDef.hasAttribute(attrName));

        structDef.removeAttribute(attrName);
        assertFalse(structDef.hasAttribute(attrName));
    }

    @Test
    public void testStructDefSetAttributeDefs() {
        AtlasStructDef structDef = ModelTestUtil.newStructDef();

        List<AtlasAttributeDef> oldAttributes = structDef.getAttributeDefs();
        List<AtlasAttributeDef> newttributes = ModelTestUtil.newAttributeDefsWithAllBuiltInTypes("newAttributes");

        structDef.setAttributeDefs(newttributes);

        for (AtlasAttributeDef attributeDef : oldAttributes) {
            assertFalse(structDef.hasAttribute(attributeDef.getName()));
        }

        for (AtlasAttributeDef attributeDef : newttributes) {
            assertTrue(structDef.hasAttribute(attributeDef.getName()));
        }
    }
}
