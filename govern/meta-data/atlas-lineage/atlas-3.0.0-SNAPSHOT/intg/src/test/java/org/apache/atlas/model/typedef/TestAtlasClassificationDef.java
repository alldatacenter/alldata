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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;


public class TestAtlasClassificationDef {

    @Test
    public void testClassificationDefSerDeEmpty() {
        AtlasClassificationDef classificationDef = new AtlasClassificationDef("emptyClassificationDef");

        String jsonString = AtlasType.toJson(classificationDef);

        AtlasClassificationDef classificationDef2 = AtlasType.fromJson(jsonString, AtlasClassificationDef.class);

        assertEquals(classificationDef2, classificationDef,
                     "Incorrect serialization/deserialization of AtlasClassificationDef");
    }

    @Test
    public void testClassificationDefSerDe() {
        AtlasClassificationDef classificationDef = ModelTestUtil.getClassificationDef();

        String jsonString = AtlasType.toJson(classificationDef);

        AtlasClassificationDef classificationDef2 = AtlasType.fromJson(jsonString, AtlasClassificationDef.class);

        assertEquals(classificationDef2, classificationDef,
                     "Incorrect serialization/deserialization of AtlasClassificationDef");
    }

    @Test
    public void testClassificationDefSerDeWithSuperType() {
        AtlasClassificationDef classificationDef = ModelTestUtil.getClassificationDefWithSuperType();

        String jsonString = AtlasType.toJson(classificationDef);

        AtlasClassificationDef classificationDef2 = AtlasType.fromJson(jsonString, AtlasClassificationDef.class);

        assertEquals(classificationDef2, classificationDef,
                     "Incorrect serialization/deserialization of AtlasClassificationDef with superType");
    }

    @Test
    public void testClassificationDefSerDeWithSuperTypes() {
        AtlasClassificationDef classificationDef = ModelTestUtil.getClassificationDefWithSuperTypes();

        String jsonString = AtlasType.toJson(classificationDef);

        AtlasClassificationDef classificationDef2 = AtlasType.fromJson(jsonString, AtlasClassificationDef.class);

        assertEquals(classificationDef2, classificationDef,
                     "Incorrect serialization/deserialization of AtlasClassificationDef with superTypes");
    }

    @Test
    public void testClassificationDefHasSuperTypeWithNoSuperType() {
        AtlasClassificationDef classificationDef = ModelTestUtil.getClassificationDef();

        for (String superType : classificationDef.getSuperTypes()) {
            assertTrue(classificationDef.hasSuperType(superType));
        }

        assertFalse(classificationDef.hasSuperType("01234-xyzabc-;''-)("));
    }

    @Test
    public void testClassificationDefHasSuperTypeWithSuperType() {
        AtlasClassificationDef classificationDef = ModelTestUtil.getClassificationDefWithSuperTypes();

        for (String superType : classificationDef.getSuperTypes()) {
            assertTrue(classificationDef.hasSuperType(superType));
        }

        assertFalse(classificationDef.hasSuperType("01234-xyzabc-;''-)("));
    }
}
