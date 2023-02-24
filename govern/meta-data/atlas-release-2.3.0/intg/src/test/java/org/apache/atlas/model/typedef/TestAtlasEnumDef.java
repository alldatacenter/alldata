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

import java.util.ArrayList;
import java.util.List;

import org.apache.atlas.model.ModelTestUtil;
import org.apache.atlas.model.typedef.AtlasEnumDef.AtlasEnumElementDef;
import org.apache.atlas.type.AtlasType;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;


public class TestAtlasEnumDef {

    @Test
    public void testEnumDefSerDeEmpty() {
        AtlasEnumDef enumDef1 = new AtlasEnumDef();

        String jsonString = AtlasType.toJson(enumDef1);

        AtlasEnumDef enumDef2 = AtlasType.fromJson(jsonString, AtlasEnumDef.class);

        assertEquals(enumDef1, enumDef2, "Incorrect serialization/deserialization of AtlasEnumDef");
    }

    @Test
    public void testEnumDefSerDe() {
        AtlasEnumDef enumDef = ModelTestUtil.getEnumDef();

        String jsonString = AtlasType.toJson(enumDef);

        AtlasEnumDef enumDef2 = AtlasType.fromJson(jsonString, AtlasEnumDef.class);

        assertEquals(enumDef, enumDef2, "Incorrect serialization/deserialization of AtlasEnumDef");
    }

    @Test
    public void testEnumDefHasElement() {
        AtlasEnumDef enumDef = ModelTestUtil.getEnumDef();

        for (AtlasEnumElementDef elementDef : enumDef.getElementDefs()) {
            assertTrue(enumDef.hasElement(elementDef.getValue()));
        }

        assertFalse(enumDef.hasElement("01234-xyzabc-;''-)("));
    }

    @Test
    public void testEnumDefAddElement() {
        AtlasEnumDef enumDef = ModelTestUtil.newEnumDef();

        String newElement = "newElement-abcd-1234";
        enumDef.addElement(new AtlasEnumElementDef(newElement, "A new element", enumDef.getElementDefs().size()));
        assertTrue(enumDef.hasElement(newElement));
    }

    @Test
    public void testEnumDefRemoveElement() {
        AtlasEnumDef enumDef = ModelTestUtil.newEnumDef();

        if (enumDef.getElementDefs().size() > 0) {
            String elementValue = enumDef.getElementDefs().get(0).getValue();

            assertTrue(enumDef.hasElement(elementValue));

            enumDef.removeElement(elementValue);
            assertFalse(enumDef.hasElement(elementValue));
        }
    }

    @Test
    public void testEnumDefSetElementDefs() {
        AtlasEnumDef enumDef = ModelTestUtil.newEnumDef();

        List<AtlasEnumElementDef> oldElements = enumDef.getElementDefs();
        List<AtlasEnumElementDef> newElements = new ArrayList<>();

        newElements.add(new AtlasEnumElementDef("newElement", "new Element", 100));

        enumDef.setElementDefs(newElements);

        for (AtlasEnumElementDef elementDef : oldElements) {
            assertFalse(enumDef.hasElement(elementDef.getValue()));
        }

        for (AtlasEnumElementDef elementDef : newElements) {
            assertTrue(enumDef.hasElement(elementDef.getValue()));
        }
    }
}
