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

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.ModelTestUtil;
import org.apache.atlas.model.typedef.AtlasRelationshipDef.RelationshipCategory;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;
import org.apache.atlas.type.AtlasType;
import org.testng.annotations.Test;

import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;

public class TestAtlasRelationshipDef {

    private static final String PREFIX_ATTRIBUTE_NAME = "reltests-";
    private List<AtlasStructDef.AtlasAttributeDef> attributeDefs;

    @Test
    public void testRelationshipDefSerDeEmpty() throws AtlasBaseException {

        AtlasRelationshipEndDef ep1 = new AtlasRelationshipEndDef("typeA", "attr1", Cardinality.SINGLE);
        AtlasRelationshipEndDef ep2 = new AtlasRelationshipEndDef("typeB", "attr2", Cardinality.SINGLE);
        AtlasRelationshipDef relationshipDef = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                RelationshipCategory.ASSOCIATION, AtlasRelationshipDef.PropagateTags.ONE_TO_TWO, ep1, ep2);

        String jsonString = AtlasType.toJson(relationshipDef);
        System.out.println(jsonString);
        assertNotNull(jsonString);

        AtlasRelationshipDef relationshipDef2 = AtlasType.fromJson(jsonString, AtlasRelationshipDef.class);
        String jsonString2 = AtlasType.toJson(relationshipDef2);

        assertEquals(jsonString, jsonString2);
        assertEquals(relationshipDef2, relationshipDef,
                "Incorrect serialization/deserialization of AtlasRelationshipDef");
    }

    @Test
    public void testRelationshipDefSerDeAttributes() throws AtlasBaseException {

        AtlasRelationshipEndDef ep1 = new AtlasRelationshipEndDef("typeA", "attr1", Cardinality.SINGLE);
        AtlasRelationshipEndDef ep2 = new AtlasRelationshipEndDef("typeB", "attr2", Cardinality.SINGLE);
        AtlasRelationshipDef relationshipDef = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                RelationshipCategory.ASSOCIATION, AtlasRelationshipDef.PropagateTags.ONE_TO_TWO, ep1, ep2);
        relationshipDef.setAttributeDefs(
                ModelTestUtil.newAttributeDefsWithAllBuiltInTypesForRelationship(PREFIX_ATTRIBUTE_NAME));
        String jsonString = AtlasType.toJson(relationshipDef);
        assertNotNull(jsonString);

        AtlasRelationshipDef relationshipDef2 = AtlasType.fromJson(jsonString, AtlasRelationshipDef.class);
        String jsonString2 = AtlasType.toJson(relationshipDef2);

        assertEquals(jsonString, jsonString2);
        assertEquals(relationshipDef2, relationshipDef,
                "Incorrect serialization/deserialization of AtlasRelationshipDef");
    }
    @Test
    public void testRelationshipEquals() throws AtlasBaseException {

        AtlasRelationshipEndDef ep1 = new AtlasRelationshipEndDef("typeA", "attr1", Cardinality.SINGLE);
        AtlasRelationshipEndDef ep2 = new AtlasRelationshipEndDef("typeB", "attr2", Cardinality.SINGLE);
        AtlasRelationshipDef relationshipDef1 = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                RelationshipCategory.ASSOCIATION, AtlasRelationshipDef.PropagateTags.ONE_TO_TWO, ep1, ep2);
        List<AtlasStructDef.AtlasAttributeDef> attributeDefs = ModelTestUtil.newAttributeDefsWithAllBuiltInTypesForRelationship(PREFIX_ATTRIBUTE_NAME);
        relationshipDef1.setAttributeDefs(attributeDefs);
        AtlasRelationshipDef relationshipDef2 = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                RelationshipCategory.ASSOCIATION, AtlasRelationshipDef.PropagateTags.ONE_TO_TWO, ep1, ep2);
        relationshipDef2.setAttributeDefs(attributeDefs);
        assertEquals(relationshipDef1,relationshipDef2);

        AtlasRelationshipDef relationshipDef3 = new AtlasRelationshipDef("emptyRelationshipDef", "desc 1", "version1",
                RelationshipCategory.ASSOCIATION, AtlasRelationshipDef.PropagateTags.ONE_TO_TWO, ep1, ep2);

        assertNotEquals(relationshipDef1,relationshipDef3);
    }
}