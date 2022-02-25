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
package org.apache.atlas.util;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.*;

import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.Constants;
import org.apache.atlas.repository.converters.TypeConverterUtil;
import org.apache.atlas.repository.graphdb.AtlasVertex;
import org.apache.atlas.repository.store.graph.v2.AtlasGraphUtilsV2;
import org.apache.atlas.repository.store.graph.v2.AtlasStructDefStoreV2;
import org.apache.atlas.repository.store.graph.v2.AtlasTypeDefGraphStoreV2;
import org.apache.atlas.type.AtlasEntityType;
import org.apache.atlas.type.AtlasStructType;
import org.apache.atlas.type.AtlasStructType.AtlasAttribute;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.type.AtlasTypeRegistry.AtlasTransientTypeRegistry;
import org.apache.atlas.typesystem.types.DataTypes.TypeCategory;
import org.apache.atlas.v1.model.typedef.*;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Validates that conversion from V1 to legacy types (and back) is consistent.  This also tests
 * that the conversion logic in AtlasStructDefStoreV1 is consistent with the conversion logic
 * in RestUtils.  This tests particularly focuses on composite attributes, since a defect was
 * found in that area.
 */
public class RestUtilsTest {

    @Test
    // "containingDatabase"
    // in tables attribute in "database" type is lost.  See ATLAS-1528.
    public void testBidirectonalCompositeMappingConsistent() throws AtlasBaseException {

        ClassTypeDefinition dbV1Type = TypesUtil.createClassTypeDef("database", "", Collections.emptySet(),
                                           new AttributeDefinition("tables", AtlasBaseTypeDef.getArrayTypeName("table"),
                                                                   Multiplicity.OPTIONAL, true, "containingDatabase"));

        ClassTypeDefinition tableV1Type = TypesUtil.createClassTypeDef("table", "", Collections.emptySet(),
                                            new AttributeDefinition("containingDatabase", "database",
                                                                    Multiplicity.OPTIONAL, false, "tables"));

        testV1toV2toV1Conversion(Arrays.asList(dbV1Type, tableV1Type), new boolean[] { true, false });
    }

    @Test
    // "containingDatabase" is lost
    // in "table" attribute in "database".  See ATLAS-1528.
    public void testBidirectonalNonCompositeMappingConsistent() throws AtlasBaseException {
        ClassTypeDefinition dbV1Type = TypesUtil.createClassTypeDef("database", "", Collections.emptySet(),
                                        new AttributeDefinition("tables", AtlasBaseTypeDef.getArrayTypeName("table"),
                                                                Multiplicity.OPTIONAL, false, "containingDatabase"));

        ClassTypeDefinition tableV1Type = TypesUtil.createClassTypeDef("table", "", Collections.emptySet(),
                                           new AttributeDefinition("containingDatabase", "database",
                                                                   Multiplicity.OPTIONAL, false, "tables"));

        testV1toV2toV1Conversion(Arrays.asList(dbV1Type, tableV1Type), new boolean[] { false, false });
    }

    private AtlasTypeDefGraphStoreV2 makeTypeStore(AtlasTypeRegistry reg) {
        AtlasTypeDefGraphStoreV2 result = mock(AtlasTypeDefGraphStoreV2.class);

        for (AtlasEntityType type : reg.getAllEntityTypes()) {
            String      typeName = type.getTypeName();
            AtlasVertex typeVertex = mock(AtlasVertex.class);

            when(result.isTypeVertex(eq(typeVertex), any(TypeCategory.class))).thenReturn(true);
            when(typeVertex.getProperty(eq(Constants.TYPE_CATEGORY_PROPERTY_KEY), eq(TypeCategory.class))).thenReturn(TypeCategory.CLASS);

            String attributeListPropertyKey = AtlasGraphUtilsV2.getTypeDefPropertyKey(typeName);

            when(typeVertex.getProperty(eq(attributeListPropertyKey), eq(List.class))).thenReturn(new ArrayList<>(type.getAllAttributes().keySet()));

            for (AtlasAttribute attribute : type.getAllAttributes().values()) {
                String attributeDefPropertyKey = AtlasGraphUtilsV2.getTypeDefPropertyKey(typeName, attribute.getName());
                String attributeJson           = AtlasStructDefStoreV2.toJsonFromAttribute(attribute);

                when(typeVertex.getProperty(eq(attributeDefPropertyKey), eq(String.class))).thenReturn(attributeJson);
            }

            when(result.findTypeVertexByName(eq(typeName))).thenReturn(typeVertex);
        }

        return result;
    }

    private AtlasAttributeDef convertToJsonAndBack(AtlasTypeRegistry registry, AtlasStructDef structDef, AtlasAttributeDef attributeDef, boolean compositeExpected) throws AtlasBaseException {
        AtlasTypeDefGraphStoreV2 typeDefStore = makeTypeStore(registry);
        AtlasStructType          structType   = (AtlasStructType) registry.getType(structDef.getName());
        AtlasAttribute           attribute    = structType.getAttribute(attributeDef.getName());
        String                   attribJson   = AtlasStructDefStoreV2.toJsonFromAttribute(attribute);
        Map                      attrInfo     = AtlasType.fromJson(attribJson, Map.class);

        Assert.assertEquals(attrInfo.get("isComposite"), compositeExpected);

        return AtlasStructDefStoreV2.toAttributeDefFromJson(structDef, attrInfo, typeDefStore);
    }

    private void testV1toV2toV1Conversion(List<ClassTypeDefinition> typesToTest, boolean[] compositeExpected) throws AtlasBaseException {
        List<AtlasEntityDef> convertedEntityDefs = convertV1toV2(typesToTest);
        AtlasTypeRegistry    registry            = createRegistry(convertedEntityDefs);

        for(int i = 0 ; i < convertedEntityDefs.size(); i++) {
            AtlasEntityDef def =  convertedEntityDefs.get(i);

            for (AtlasAttributeDef attrDef : def.getAttributeDefs()) {
                AtlasAttributeDef converted = convertToJsonAndBack(registry, def, attrDef, compositeExpected[i]);

                Assert.assertEquals(converted, attrDef);
            }
        }

        List<ClassTypeDefinition> convertedBackTypeDefs = convertV2toV1(convertedEntityDefs);

        for (int i = 0; i < typesToTest.size(); i++) {
            ClassTypeDefinition convertedBack = convertedBackTypeDefs.get(i);

            Assert.assertEquals(convertedBack, typesToTest.get(i));

            List<AttributeDefinition> attributeDefinitions = convertedBack.getAttributeDefinitions();

            if (attributeDefinitions.size() > 0) {
                Assert.assertEquals(attributeDefinitions.get(0).getIsComposite(), compositeExpected[i]);
            }
        }
    }

    private List<ClassTypeDefinition> convertV2toV1(List<AtlasEntityDef> toConvert) throws AtlasBaseException {
        AtlasTypeRegistry         reg    = createRegistry(toConvert);
        List<ClassTypeDefinition> result = new ArrayList<>(toConvert.size());

        for (int i = 0; i < toConvert.size(); i++) {
            AtlasEntityDef      entityDef = toConvert.get(i);
            AtlasEntityType     entity    = reg.getEntityTypeByName(entityDef.getName());
            ClassTypeDefinition converted = TypeConverterUtil.toTypesDef(entity, reg).getClassTypes().get(0);

            result.add(converted);
        }

        return result;
    }

    private AtlasTypeRegistry createRegistry(List<AtlasEntityDef> toConvert) throws AtlasBaseException {
        AtlasTypeRegistry          reg = new AtlasTypeRegistry();
        AtlasTransientTypeRegistry tmp = reg.lockTypeRegistryForUpdate();

        tmp.addTypes(toConvert);
        reg.releaseTypeRegistryForUpdate(tmp, true);

        return reg;
    }

    private List<AtlasEntityDef> convertV1toV2(List<ClassTypeDefinition> types) throws AtlasBaseException {
        List<ClassTypeDefinition> classTypeList       = new ArrayList(types);
        TypesDef                  toConvert           = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), classTypeList);
        String                    json                = AtlasType.toV1Json(toConvert);
        AtlasTypeRegistry         emptyRegistry       = new AtlasTypeRegistry();
        AtlasTypesDef             converted           = TypeConverterUtil.toAtlasTypesDef(json, emptyRegistry);
        List<AtlasEntityDef>      convertedEntityDefs = converted.getEntityDefs();

        return convertedEntityDefs;
    }
}
