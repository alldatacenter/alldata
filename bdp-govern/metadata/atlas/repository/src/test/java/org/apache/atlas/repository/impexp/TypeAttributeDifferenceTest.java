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
package org.apache.atlas.repository.impexp;

import org.apache.atlas.TestUtilsV2;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.impexp.AtlasImportResult;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.store.bootstrap.AtlasTypeDefStoreInitializer;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.utils.TestLoadModelUtils;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

public class TypeAttributeDifferenceTest {
    private TypeAttributeDifference typeAttributeDifference;

    @Mock
    AtlasTypeDefStore typeDefStore;

    @Mock
    AtlasTypeRegistry typeRegistry;


    @BeforeClass
    public void setup() {
        MockitoAnnotations.initMocks(this);

        typeAttributeDifference = new TypeAttributeDifference(typeDefStore, typeRegistry);
    }

    private List<AtlasEnumDef.AtlasEnumElementDef> getEnumElementDefs(int startIndex, String... names) {
        int i = startIndex;
        List<AtlasEnumDef.AtlasEnumElementDef> list = new ArrayList<>();
        for (String s: names) {
            list.add(new AtlasEnumDef.AtlasEnumElementDef(s, s, i++));
        }

        return list;
    }

    private List<AtlasStructDef.AtlasAttributeDef> getAtlasAttributeDefs(String... names) {
        List<AtlasStructDef.AtlasAttributeDef> list = new ArrayList<>();
        for (String s : names) {
            list.add(new AtlasStructDef.AtlasAttributeDef(s, AtlasBaseTypeDef.ATLAS_TYPE_STRING));
        }

        return list;
    }

    private AtlasEntityDef getAtlasEntityDefWithAttributes(String... attributeNames) {
        AtlasEntityDef e = new AtlasEntityDef();
        for (AtlasStructDef.AtlasAttributeDef a : getAtlasAttributeDefs(attributeNames)) {
            e.addAttribute(a);
        }

        return e;
    }

    @Test
    public void entityDefWithNoAttributes() throws Exception {
        AtlasEntityDef existing = new AtlasEntityDef();
        AtlasEntityDef incoming = new AtlasEntityDef();
        List<AtlasStructDef.AtlasAttributeDef> expectedAttributes = new ArrayList<>();
        List<AtlasStructDef.AtlasAttributeDef> actualAttributes = invokeGetAttributesAbsentInExisting(existing, incoming);

        Assert.assertEquals(actualAttributes, expectedAttributes);
    }

    private List<AtlasStructDef.AtlasAttributeDef> invokeGetAttributesAbsentInExisting(AtlasStructDef existing, AtlasStructDef incoming) throws Exception {
        return typeAttributeDifference.getElementsAbsentInExisting(existing, incoming);
    }

    private List<AtlasEnumDef.AtlasEnumElementDef> invokeGetAttributesAbsentInExisting(AtlasEnumDef existing, AtlasEnumDef incoming) throws Exception {
        return typeAttributeDifference.getElementsAbsentInExisting(existing, incoming);
    }

    private AtlasEnumDef getAtlasEnumWithAttributes(String... elements) {
        AtlasEnumDef enumDef = new AtlasEnumDef();
        for (AtlasEnumDef.AtlasEnumElementDef ed : getEnumElementDefs(0, elements)) {
            enumDef.addElement(ed);
        }

        return enumDef;
    }

    @Test
    public void bothSame_DifferenceIsEmptyList() throws Exception {
        AtlasEntityDef existing = getAtlasEntityDefWithAttributes("name", "qualifiedName");
        AtlasEntityDef incoming = getAtlasEntityDefWithAttributes("name", "qualifiedName");

        List<AtlasStructDef.AtlasAttributeDef> expectedAttributes = getAtlasAttributeDefs();
        List<AtlasStructDef.AtlasAttributeDef> actualAttributes = invokeGetAttributesAbsentInExisting(existing, incoming);

        Assert.assertEquals(actualAttributes, expectedAttributes);
    }

    @Test
    public void different_ReturnsDifference() throws Exception {
        AtlasEntityDef existing = getAtlasEntityDefWithAttributes("name");
        AtlasEntityDef incoming = getAtlasEntityDefWithAttributes("name", "qualifiedName");
        List<AtlasStructDef.AtlasAttributeDef> expectedAttributes = getAtlasAttributeDefs( "qualifiedName");

        List<AtlasStructDef.AtlasAttributeDef> actualAttributes = invokeGetAttributesAbsentInExisting(existing, incoming);
        Assert.assertEquals(actualAttributes, expectedAttributes);
    }

    @Test
    public void differentSubset_ReturnsDifference() throws Exception {
        AtlasEntityDef existing = getAtlasEntityDefWithAttributes("name", "qualifiedName");
        AtlasEntityDef incoming = getAtlasEntityDefWithAttributes("name");
        List<AtlasStructDef.AtlasAttributeDef> actualAttributes = invokeGetAttributesAbsentInExisting(existing, incoming);

        List<AtlasStructDef.AtlasAttributeDef> expectedAttributes = getAtlasAttributeDefs();
        Assert.assertEquals(actualAttributes, expectedAttributes);
    }

    @Test
    public void differentEnumDef_ReturnsDifference () throws Exception {
        AtlasEnumDef existing = getAtlasEnumWithAttributes("Alpha", "Bravo");
        AtlasEnumDef incoming = getAtlasEnumWithAttributes("Alpha", "Bravo", "Delta", "Echo");
        List<AtlasEnumDef.AtlasEnumElementDef> actualAttributes = invokeGetAttributesAbsentInExisting(existing, incoming);

        List<AtlasEnumDef.AtlasEnumElementDef> expectedAttributes = getEnumElementDefs(2, "Delta", "Echo");
        Assert.assertEquals(actualAttributes, expectedAttributes);
    }

    @Test
    public void differentEnumDefs_ReturnsDifference () throws Exception {
        AtlasEnumDef existing = getAtlasEnumWithAttributes("Alpha", "Bravo");
        AtlasEnumDef incoming = getAtlasEnumWithAttributes("Alpha", "Bravo", "Delta", "Echo");
        boolean ret = invokeUpdate(existing, incoming);

        List<AtlasEnumDef.AtlasEnumElementDef> expectedAttributes = getEnumElementDefs(0, "Alpha", "Bravo", "Delta", "Echo");

        Assert.assertTrue(ret, "Update took place");
        Assert.assertEquals(existing.getElementDefs(), expectedAttributes);
    }

    private boolean invokeUpdate(AtlasEnumDef existing, AtlasEnumDef incoming) throws Exception {
        return typeAttributeDifference.addElements(existing, incoming);
    }

    @Test
    public void t1() throws IOException, AtlasBaseException {
        AtlasTypesDef typesDef = TestResourceFileUtils.readObjectFromJson(".", "typesDef-bm", AtlasTypesDef.class);

        AtlasImportResult result = new AtlasImportResult();
        AtlasTypesDef typesToCreate = AtlasTypeDefStoreInitializer.getTypesToCreate(typesDef, this.typeRegistry);
        typeDefStore.createTypesDef(typesToCreate);
        typeAttributeDifference.updateTypes(typesDef, result);
        assertNotNull(typesDef);
    }
}
