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
package org.apache.atlas.model.instance;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.ModelTestUtil;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;


public class TestAtlasEntity {

    @Test
    public void testEntitySerDe() throws AtlasBaseException {
        AtlasEntityDef    entityDef    = ModelTestUtil.getEntityDef();
        AtlasTypeRegistry typeRegistry = ModelTestUtil.getTypesRegistry();
        AtlasEntityType   entityType   = typeRegistry.getEntityTypeByName(entityDef.getName());

        assertNotNull(entityType);

        AtlasEntity ent1 =  entityType.createDefaultValue();

        String jsonString = AtlasType.toJson(ent1);

        AtlasEntity ent2 = AtlasType.fromJson(jsonString, AtlasEntity.class);

        entityType.normalizeAttributeValues(ent2);

        assertEquals(ent2, ent1, "Incorrect serialization/deserialization of AtlasEntity");
    }

    @Test
    public void testEntitySerDeWithSuperType() throws AtlasBaseException {
        AtlasEntityDef    entityDef    = ModelTestUtil.getEntityDefWithSuperType();
        AtlasTypeRegistry typeRegistry = ModelTestUtil.getTypesRegistry();
        AtlasEntityType   entityType   = typeRegistry.getEntityTypeByName(entityDef.getName());

        assertNotNull(entityType);

        AtlasEntity ent1 =  entityType.createDefaultValue();

        String jsonString = AtlasType.toJson(ent1);

        AtlasEntity ent2 = AtlasType.fromJson(jsonString, AtlasEntity.class);

        entityType.normalizeAttributeValues(ent2);

        assertEquals(ent2, ent1, "Incorrect serialization/deserialization of AtlasEntity with superType");
    }

    @Test
    public void testEntitySerDeWithSuperTypes() throws AtlasBaseException {
        AtlasEntityDef    entityDef    = ModelTestUtil.getEntityDefWithSuperTypes();
        AtlasTypeRegistry typeRegistry = ModelTestUtil.getTypesRegistry();
        AtlasEntityType   entityType   = typeRegistry.getEntityTypeByName(entityDef.getName());

        assertNotNull(entityType);

        AtlasEntity ent1 =  entityType.createDefaultValue();

        String jsonString = AtlasType.toJson(ent1);

        AtlasEntity ent2 = AtlasType.fromJson(jsonString, AtlasEntity.class);

        entityType.normalizeAttributeValues(ent2);

        assertEquals(ent2, ent1, "Incorrect serialization/deserialization of AtlasEntity with superTypes");
    }
}
