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

package org.apache.atlas.web.integration;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.v1.model.typedef.*;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.typesystem.types.DataTypes;
import org.apache.atlas.v1.typesystem.types.utils.TypesUtil;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.apache.atlas.v1.typesystem.types.utils.TypesUtil.createOptionalAttrDef;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.fail;

/**
 * Integration test for types jersey resource.
 */
public class TypesJerseyResourceIT extends BaseResourceIT {

    private List<HierarchicalTypeDefinition> typeDefinitions;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        typeDefinitions = createHiveTypes();
    }

    @AfterClass
    public void tearDown() throws Exception {
        typeDefinitions.clear();
    }

    @Test
    public void testSubmit() throws Exception {
        for (HierarchicalTypeDefinition typeDefinition : typeDefinitions) {
            try{
                atlasClientV1.getType(typeDefinition.getTypeName());
            } catch (AtlasServiceException ase){
                TypesDef typesDef = null;

                if (typeDefinition instanceof ClassTypeDefinition) {
                    typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(),
                            Collections.emptyList(), Collections.singletonList((ClassTypeDefinition) typeDefinition));
                } else if (typeDefinition instanceof TraitTypeDefinition) {
                    typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(),
                            Collections.singletonList((TraitTypeDefinition) typeDefinition), Collections.emptyList());
                }

                String typesAsJSON = AtlasType.toV1Json(typesDef);

                System.out.println("typesAsJSON = " + typesAsJSON);

                ObjectNode response = atlasClientV1.callAPIWithBody(AtlasClient.API_V1.CREATE_TYPE, typesAsJSON);
                Assert.assertNotNull(response);


                ArrayNode typesAdded = (ArrayNode) response.get(AtlasClient.TYPES);
                assertEquals(typesAdded.size(), 1);
                assertEquals(typesAdded.get(0).get(NAME).asText(), typeDefinition.getTypeName());
                Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));}
        }
    }

    @Test
    public void testDuplicateSubmit() throws Exception {
        ClassTypeDefinition type = TypesUtil.createClassTypeDef(randomString(), null,
                Collections.<String>emptySet(), TypesUtil.createUniqueRequiredAttrDef(NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING));
        TypesDef typesDef =
                new TypesDef(Collections.<EnumTypeDefinition>emptyList(), Collections.<StructTypeDefinition>emptyList(),
                        Collections.<TraitTypeDefinition>emptyList(), Collections.singletonList(type));
        atlasClientV1.createType(typesDef);

        try {
            atlasClientV1.createType(typesDef);
            fail("Expected 409");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.CONFLICT.getStatusCode());
        }
    }

    @Test
    public void testUpdate() throws Exception {
        ClassTypeDefinition classTypeDef = TypesUtil
                .createClassTypeDef(randomString(), null, "1.0", Collections.<String>emptySet(),
                        TypesUtil.createUniqueRequiredAttrDef(NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING));

        TypesDef typesDef = new TypesDef(Collections.<EnumTypeDefinition>emptyList(), Collections.<StructTypeDefinition>emptyList(), Collections.<TraitTypeDefinition>emptyList(), Collections.singletonList(classTypeDef));

        List<String> typesCreated = atlasClientV1.createType(AtlasType.toV1Json(typesDef));
        assertEquals(typesCreated.size(), 1);
        assertEquals(typesCreated.get(0), classTypeDef.getTypeName());

        //Add attribute description
        classTypeDef = TypesUtil.createClassTypeDef(classTypeDef.getTypeName(), null, "2.0",
                Collections.<String>emptySet(),
                TypesUtil.createUniqueRequiredAttrDef(NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                createOptionalAttrDef(DESCRIPTION, AtlasBaseTypeDef.ATLAS_TYPE_STRING));
        TypesDef typeDef = new TypesDef(Collections.<EnumTypeDefinition>emptyList(), Collections.<StructTypeDefinition>emptyList(), Collections.<TraitTypeDefinition>emptyList(), Collections.singletonList(classTypeDef));
        List<String> typesUpdated = atlasClientV1.updateType(typeDef);
        assertEquals(typesUpdated.size(), 1);
        Assert.assertTrue(typesUpdated.contains(classTypeDef.getTypeName()));

        TypesDef updatedTypeDef = atlasClientV1.getType(classTypeDef.getTypeName());
        assertNotNull(updatedTypeDef);

        ClassTypeDefinition updatedType = updatedTypeDef.getClassTypes().get(0);
        assertEquals(updatedType.getAttributeDefinitions().size(), 2);
    }

    @Test(dependsOnMethods = "testSubmit")
    public void testGetDefinition() throws Exception {
        for (HierarchicalTypeDefinition typeDefinition : typeDefinitions) {
            System.out.println("typeName = " + typeDefinition.getTypeName());

            ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.LIST_TYPES, null, typeDefinition.getTypeName());

            Assert.assertNotNull(response);
            Assert.assertNotNull(response.get(AtlasClient.DEFINITION));
            Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));

            TypesDef typesDef = AtlasType.fromV1Json(AtlasType.toJson(response.get(AtlasClient.DEFINITION)), TypesDef.class);

            List<? extends HierarchicalTypeDefinition> hierarchicalTypeDefs = Collections.emptyList();

            if (typeDefinition instanceof ClassTypeDefinition) {
                hierarchicalTypeDefs = typesDef.getClassTypes();
            } else if (typeDefinition instanceof TraitTypeDefinition) {
                hierarchicalTypeDefs = typesDef.getTraitTypes();
            }

            for (HierarchicalTypeDefinition hierarchicalTypes : hierarchicalTypeDefs) {
                for (AttributeDefinition attrDef : hierarchicalTypes.getAttributeDefinitions()) {
                    if (NAME.equals(attrDef.getName())) {
                        assertEquals(attrDef.getIsIndexable(), true);
                        assertEquals(attrDef.getIsUnique(), true);
                    }
                }
            }
        }
    }

    @Test(expectedExceptions = AtlasServiceException.class)
    public void testGetDefinitionForNonexistentType() throws Exception {
        ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.LIST_TYPES, null, "blah");
    }

    @Test(dependsOnMethods = "testSubmit")
    public void testGetTypeNames() throws Exception {
        ObjectNode response = atlasClientV1.callAPIWithBodyAndParams(AtlasClient.API_V1.LIST_TYPES, null, (String[]) null);
        Assert.assertNotNull(response);

        Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));

        final ArrayNode list = (ArrayNode) response.get(AtlasClient.RESULTS);
        Assert.assertNotNull(list);

        //Verify that primitive and core types are not returned
        String typesString = list.toString();
        Assert.assertFalse(typesString.contains(" \"__IdType\" "));
        Assert.assertFalse(typesString.contains(" \"string\" "));
    }

    @Test
    public void testGetTraitNames() throws Exception {
        String[] traitsAdded = addTraits();

        MultivaluedMap<String, String> queryParams = new MultivaluedMapImpl();
        queryParams.add("type", DataTypes.TypeCategory.TRAIT.name());

        ObjectNode response = atlasClientV1.callAPIWithQueryParams(AtlasClient.API_V1.LIST_TYPES, queryParams);
        Assert.assertNotNull(response);

        Assert.assertNotNull(response.get(AtlasClient.REQUEST_ID));

        final ArrayNode list = (ArrayNode) response.get(AtlasClient.RESULTS);
        Assert.assertNotNull(list);
        Assert.assertTrue(list.size() >= traitsAdded.length);
    }

    @Test
    public void testListTypesByFilter() throws Exception {
        AttributeDefinition attr = TypesUtil.createOptionalAttrDef("attr", AtlasBaseTypeDef.ATLAS_TYPE_STRING);
        AttributeDefinition attr1 = TypesUtil.createOptionalAttrDef("attr1", AtlasBaseTypeDef.ATLAS_TYPE_STRING);

        ClassTypeDefinition classTypeDef = TypesUtil.createClassTypeDef("A" + randomString(), null, Collections.emptySet(), attr);
        TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.singletonList(classTypeDef));
        String a = createType(AtlasType.toV1Json(typesDef)).get(0);

        classTypeDef = TypesUtil.createClassTypeDef("A1" + randomString(), null, Collections.singleton(a));
        typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.singletonList(classTypeDef));
        String a1 = createType(AtlasType.toV1Json(typesDef)).get(0);

        classTypeDef = TypesUtil.createClassTypeDef("B" + randomString(), null, Collections.<String>emptySet(), attr1);
        typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.singletonList(classTypeDef));
        String b = createType(AtlasType.toV1Json(typesDef)).get(0);

        classTypeDef = TypesUtil.createClassTypeDef("C" + randomString(), null, new HashSet<>(Arrays.asList(a, b)));
        typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), Collections.singletonList(classTypeDef));
        String c = createType(AtlasType.toV1Json(typesDef)).get(0);

        List<String> results = atlasClientV1.listTypes(DataTypes.TypeCategory.CLASS, a, b);
        assertEquals(results, Arrays.asList(a1), "Results: " + results);
    }

    private String[] addTraits() throws Exception {
        String[] traitNames = {"class_trait", "secure_trait", "pii_trait", "ssn_trait", "salary_trait", "sox_trait",};

        for (String traitName : traitNames) {
            TraitTypeDefinition traitTypeDef =
                    TypesUtil.createTraitTypeDef(traitName, null, Collections.<String>emptySet());
            TypesDef typesDef = new TypesDef(Collections.emptyList(), Collections.emptyList(), Collections.singletonList(traitTypeDef), Collections.emptyList());

            String json = AtlasType.toV1Json(typesDef);
            createType(json);
        }

        return traitNames;
    }

    private List<HierarchicalTypeDefinition> createHiveTypes() throws Exception {
        ArrayList<HierarchicalTypeDefinition> typeDefinitions = new ArrayList<>();

        ClassTypeDefinition databaseTypeDefinition = TypesUtil
                .createClassTypeDef("database", null, Collections.<String>emptySet(),
                        TypesUtil.createUniqueRequiredAttrDef(NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        TypesUtil.createRequiredAttrDef(DESCRIPTION, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        TypesUtil.createRequiredAttrDef(QUALIFIED_NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING));
        typeDefinitions.add(databaseTypeDefinition);

        ClassTypeDefinition tableTypeDefinition = TypesUtil
                .createClassTypeDef("table", null, Collections.<String>emptySet(),
                        TypesUtil.createUniqueRequiredAttrDef(NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        TypesUtil.createRequiredAttrDef(DESCRIPTION, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        TypesUtil.createRequiredAttrDef(QUALIFIED_NAME, AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        createOptionalAttrDef("columnNames", AtlasBaseTypeDef.getArrayTypeName(AtlasBaseTypeDef.ATLAS_TYPE_STRING)),
                        createOptionalAttrDef("created", AtlasBaseTypeDef.ATLAS_TYPE_DATE),
                        createOptionalAttrDef("parameters",
                                AtlasBaseTypeDef.getMapTypeName(AtlasBaseTypeDef.ATLAS_TYPE_STRING, AtlasBaseTypeDef.ATLAS_TYPE_STRING)),
                        TypesUtil.createRequiredAttrDef("type", AtlasBaseTypeDef.ATLAS_TYPE_STRING),
                        new AttributeDefinition("database", "database", Multiplicity.REQUIRED, false, null));
        typeDefinitions.add(tableTypeDefinition);

        TraitTypeDefinition fetlTypeDefinition = TypesUtil
                .createTraitTypeDef("fetl", null, Collections.<String>emptySet(),
                        TypesUtil.createRequiredAttrDef("level", AtlasBaseTypeDef.ATLAS_TYPE_INT));
        typeDefinitions.add(fetlTypeDefinition);

        return typeDefinitions;
    }
}
