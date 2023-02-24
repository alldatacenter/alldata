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

import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.TypeCategory;
import org.apache.atlas.model.typedef.AtlasBaseTypeDef;
import org.apache.atlas.model.typedef.AtlasBusinessMetadataDef;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasEnumDef;
import org.apache.atlas.model.typedef.AtlasRelationshipDef;
import org.apache.atlas.model.typedef.AtlasStructDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef;
import org.apache.atlas.model.typedef.AtlasStructDef.AtlasConstraintDef;
import org.apache.atlas.model.typedef.AtlasTypeDefHeader;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasTypeUtil;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.apache.atlas.AtlasErrorCode.TYPE_NAME_NOT_FOUND;
import static org.apache.atlas.model.typedef.AtlasStructDef.AtlasAttributeDef.Cardinality;
import static org.apache.atlas.type.AtlasTypeUtil.createClassTypeDef;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Integration test for types jersey resource.
 */
public class TypedefsJerseyResourceIT extends BaseResourceIT {

    private AtlasTypesDef typeDefinitions;

    private AtlasClientV2 clientV2;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();

        typeDefinitions = createHiveTypesV2();

        if (!AuthenticationUtil.isKerberosAuthenticationEnabled()) {
            clientV2 = new AtlasClientV2(atlasUrls, new String[]{"admin", "admin"});
        } else {
            clientV2 = new AtlasClientV2(atlasUrls);
        }
    }

    @AfterClass
    public void tearDown() throws Exception {
        emptyTypeDefs(typeDefinitions);
    }

    @Test
    public void testCreate() throws Exception {
        createType(typeDefinitions);

        // validate if all types created successfully
        for (AtlasEnumDef enumDef : typeDefinitions.getEnumDefs()) {
            checkIfTypeExists(enumDef.getName());
        }

        for (AtlasStructDef structDef : typeDefinitions.getStructDefs()) {
            checkIfTypeExists(structDef.getName());
        }

        for (AtlasClassificationDef classificationDef : typeDefinitions.getClassificationDefs()) {
            checkIfTypeExists(classificationDef.getName());
        }

        for (AtlasEntityDef entityDef : typeDefinitions.getEntityDefs()) {
            checkIfTypeExists(entityDef.getName());
        }

        for (AtlasRelationshipDef relationshipDef : typeDefinitions.getRelationshipDefs()) {
            checkIfTypeExists(relationshipDef.getName());
        }

        for (AtlasBusinessMetadataDef businessMetadataDef : typeDefinitions.getBusinessMetadataDefs()) {
            checkIfTypeExists(businessMetadataDef.getName());
        }
    }

    @Test
    public void testGetHeaders() throws Exception {
        MultivaluedMap<String, String> filterParams = new MultivaluedMapImpl();
        filterParams.add(SearchFilter.PARAM_TYPE, "ENTITY");
        List<AtlasTypeDefHeader> headers = clientV2.getAllTypeDefHeaders(new SearchFilter(filterParams));
        assertNotNull(headers);
    }

    @Test(dependsOnMethods = "testGetDefinition")
    public void testDeleteAtlasTypeByName() throws Exception {
        String typeName = "table";
        boolean typeExists = atlasClientV2.typeWithNameExists(typeName);
        if (typeExists) {
            clientV2.deleteTypeByName(typeName);
            boolean afterDelete = atlasClientV2.typeWithNameExists(typeName);
            assertEquals(afterDelete, false);
        }
    }

    @Test
    public void testDuplicateCreate() throws Exception {
        AtlasEntityDef type = createClassTypeDef(randomString(),
                Collections.<String>emptySet(), AtlasTypeUtil.createUniqueRequiredAttrDef("name", "string"));
        AtlasTypesDef typesDef = new AtlasTypesDef();
        typesDef.getEntityDefs().add(type);

        AtlasTypesDef created = clientV2.createAtlasTypeDefs(typesDef);
        assertNotNull(created);

        try {
            created = clientV2.createAtlasTypeDefs(typesDef);
            fail("Expected 409");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.CONFLICT.getStatusCode());
        }
    }

    @Test
    public void testUpdate() throws Exception {
        String entityType = randomString();
        AtlasEntityDef typeDefinition =
                createClassTypeDef(entityType, Collections.<String>emptySet(),
                        AtlasTypeUtil.createUniqueRequiredAttrDef("name", "string"));

        AtlasTypesDef atlasTypesDef = new AtlasTypesDef();
        atlasTypesDef.getEntityDefs().add(typeDefinition);

        AtlasTypesDef createdTypeDefs = clientV2.createAtlasTypeDefs(atlasTypesDef);
        assertNotNull(createdTypeDefs);
        assertEquals(createdTypeDefs.getEntityDefs().size(), atlasTypesDef.getEntityDefs().size());

        //Add attribute description
        typeDefinition = createClassTypeDef(typeDefinition.getName(),
                Collections.<String>emptySet(),
                AtlasTypeUtil.createUniqueRequiredAttrDef("name", "string"),
                AtlasTypeUtil.createOptionalAttrDef("description", "string"));

        emptyTypeDefs(atlasTypesDef);

        atlasTypesDef.getEntityDefs().add(typeDefinition);

        AtlasTypesDef updatedTypeDefs = clientV2.updateAtlasTypeDefs(atlasTypesDef);
        assertNotNull(updatedTypeDefs);
        assertEquals(updatedTypeDefs.getEntityDefs().size(), atlasTypesDef.getEntityDefs().size());
        assertEquals(updatedTypeDefs.getEntityDefs().get(0).getName(), atlasTypesDef.getEntityDefs().get(0).getName());

        MultivaluedMap<String, String> filterParams = new MultivaluedMapImpl();
        filterParams.add(SearchFilter.PARAM_TYPE, "ENTITY");
        AtlasTypesDef allTypeDefs = clientV2.getAllTypeDefs(new SearchFilter(filterParams));
        assertNotNull(allTypeDefs);
        Boolean entityDefFound = false;
        for (AtlasEntityDef atlasEntityDef : allTypeDefs.getEntityDefs()){
            if (atlasEntityDef.getName().equals(typeDefinition.getName())) {
                assertEquals(atlasEntityDef.getAttributeDefs().size(), 2);
                entityDefFound = true;
                break;
            }
        }
        assertTrue(entityDefFound, "Required entityDef not found.");
    }

    @Test(dependsOnMethods = "testCreate")
    public void testGetDefinition() throws Exception {
        if (CollectionUtils.isNotEmpty(typeDefinitions.getEnumDefs())) {
            for (AtlasEnumDef atlasEnumDef : typeDefinitions.getEnumDefs()) {
                verifyByNameAndGUID(atlasEnumDef);
            }
        }

        if (CollectionUtils.isNotEmpty(typeDefinitions.getStructDefs())) {
            for (AtlasStructDef structDef : typeDefinitions.getStructDefs()) {
                verifyByNameAndGUID(structDef);
            }
        }

        if (CollectionUtils.isNotEmpty(typeDefinitions.getClassificationDefs())) {
            for (AtlasClassificationDef classificationDef : typeDefinitions.getClassificationDefs()) {
                verifyByNameAndGUID(classificationDef);
            }
        }

        if (CollectionUtils.isNotEmpty(typeDefinitions.getEntityDefs())) {
            for (AtlasEntityDef entityDef : typeDefinitions.getEntityDefs()) {
                verifyByNameAndGUID(entityDef);
            }
        }

        if (CollectionUtils.isNotEmpty(typeDefinitions.getRelationshipDefs())) {
            for (AtlasRelationshipDef relationshipDef : typeDefinitions.getRelationshipDefs()) {
                verifyByNameAndGUID(relationshipDef);
            }
        }

        if (CollectionUtils.isNotEmpty(typeDefinitions.getBusinessMetadataDefs())) {
            for (AtlasBusinessMetadataDef businessMetadataDef : typeDefinitions.getBusinessMetadataDefs()) {
                verifyByNameAndGUID(businessMetadataDef);
            }
        }
    }

    @Test()
    public void testInvalidGets() throws Exception {
        try {
            AtlasEnumDef byName = clientV2.getEnumDefByName("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasEnumDef byGuid = clientV2.getEnumDefByGuid("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasStructDef byName = clientV2.getStructDefByName("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasStructDef byGuid = clientV2.getStructDefByGuid("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasClassificationDef byName = clientV2.getClassificationDefByName("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasClassificationDef byGuid = clientV2.getClassificationDefByGuid("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasEntityDef byName = clientV2.getEntityDefByName("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasEntityDef byGuid = clientV2.getEntityDefByGuid("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasRelationshipDef byName = clientV2.getRelationshipDefByName("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasRelationshipDef byGuid = clientV2.getRelationshipDefByGuid("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasBusinessMetadataDef byName = clientV2.getBusinessMetadataDefByName("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

        try {
            AtlasBusinessMetadataDef byGuid = clientV2.getBusinessMetadataDefGuid("blah");
            fail("Get for invalid name should have reported a failure");
        } catch (AtlasServiceException e) {
            assertEquals(e.getStatus().getStatusCode(), Response.Status.NOT_FOUND.getStatusCode(),
                    "Should've returned a 404");
        }

    }

    @Test
    public void testListTypesByFilter() throws Exception {
        AtlasAttributeDef attr = AtlasTypeUtil.createOptionalAttrDef("attr", "string");
        AtlasAttributeDef attr1 = AtlasTypeUtil.createOptionalAttrDef("attr1", "string");
        AtlasEntityDef classDefA = AtlasTypeUtil.createClassTypeDef("A" + randomString(), Collections.<String>emptySet(), attr);
        AtlasEntityDef classDefA1 = AtlasTypeUtil.createClassTypeDef("A1" + randomString(), Collections.singleton(classDefA.getName()));
        AtlasEntityDef classDefB = AtlasTypeUtil.createClassTypeDef("B" + randomString(), Collections.<String>emptySet(), attr1);
        AtlasEntityDef classDefC = AtlasTypeUtil.createClassTypeDef("C" + randomString(), new HashSet<>(Arrays.asList(classDefB.getName(), classDefA.getName())));

        AtlasTypesDef atlasTypesDef = new AtlasTypesDef();
        atlasTypesDef.getEntityDefs().add(classDefA);
        atlasTypesDef.getEntityDefs().add(classDefA1);
        atlasTypesDef.getEntityDefs().add(classDefB);
        atlasTypesDef.getEntityDefs().add(classDefC);

        AtlasTypesDef created = clientV2.createAtlasTypeDefs(atlasTypesDef);
        assertNotNull(created);
        assertEquals(created.getEntityDefs().size(), atlasTypesDef.getEntityDefs().size());

        MultivaluedMap<String, String> searchParams = new MultivaluedMapImpl();
        searchParams.add(SearchFilter.PARAM_TYPE, "CLASS");
        searchParams.add(SearchFilter.PARAM_SUPERTYPE, classDefA.getName());
        SearchFilter searchFilter = new SearchFilter(searchParams);
        AtlasTypesDef searchDefs = clientV2.getAllTypeDefs(searchFilter);
        assertNotNull(searchDefs);
        assertEquals(searchDefs.getEntityDefs().size(), 2);

        searchParams.add(SearchFilter.PARAM_NOT_SUPERTYPE, classDefB.getName());
        searchFilter = new SearchFilter(searchParams);
        searchDefs = clientV2.getAllTypeDefs(searchFilter);
        assertNotNull(searchDefs);
        assertEquals(searchDefs.getEntityDefs().size(), 1);
    }

    private AtlasTypesDef createHiveTypesV2() throws Exception {
        AtlasTypesDef atlasTypesDef = new AtlasTypesDef();

        AtlasEntityDef databaseTypeDefinition =
                createClassTypeDef("database", Collections.<String>emptySet(),
                        AtlasTypeUtil.createUniqueRequiredAttrDef("name", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("description", "string"));
        atlasTypesDef.getEntityDefs().add(databaseTypeDefinition);

        AtlasEntityDef tableTypeDefinition =
                createClassTypeDef("table", Collections.<String>emptySet(),
                        AtlasTypeUtil.createUniqueRequiredAttrDef("name", "string"),
                        AtlasTypeUtil.createRequiredAttrDef("description", "string"),
                        AtlasTypeUtil.createOptionalAttrDef("columnNames", AtlasBaseTypeDef.getArrayTypeName("string")),
                        AtlasTypeUtil.createOptionalAttrDef("created", "date"),
                        AtlasTypeUtil.createOptionalAttrDef("parameters",
                                AtlasBaseTypeDef.getMapTypeName("string", "string")),
                        AtlasTypeUtil.createRequiredAttrDef("type", "string"),
                        new AtlasAttributeDef("database", "database",
                                false,
                                Cardinality.SINGLE, 1, 1,
                                true, true, false,
                                Collections.<AtlasConstraintDef>emptyList()));
        atlasTypesDef.getEntityDefs().add(tableTypeDefinition);

        AtlasClassificationDef fetlTypeDefinition = AtlasTypeUtil
                .createTraitTypeDef("fetl", Collections.<String>emptySet(),
                        AtlasTypeUtil.createRequiredAttrDef("level", "int"));
        atlasTypesDef.getClassificationDefs().add(fetlTypeDefinition);

        return atlasTypesDef;
    }

    private void verifyByNameAndGUID(AtlasBaseTypeDef typeDef) {
        try {
            AtlasBaseTypeDef byName = null;
            if (typeDef.getCategory() == TypeCategory.ENUM) {
                byName = clientV2.getEnumDefByName(typeDef.getName());
            } else if (typeDef.getCategory() == TypeCategory.ENTITY) {
                byName = clientV2.getEntityDefByName(typeDef.getName());
            } else if (typeDef.getCategory() == TypeCategory.CLASSIFICATION) {
                byName = clientV2.getClassificationDefByName(typeDef.getName());
            } else if (typeDef.getCategory() == TypeCategory.STRUCT) {
                byName = clientV2.getStructDefByName(typeDef.getName());
            }  else if (typeDef.getCategory() == TypeCategory.RELATIONSHIP) {
                byName = clientV2.getRelationshipDefByName(typeDef.getName());
            }  else if (typeDef.getCategory() == TypeCategory.BUSINESS_METADATA) {
                byName = clientV2.getBusinessMetadataDefByName(typeDef.getName());
            }
            assertNotNull(byName);
        } catch (AtlasServiceException e) {
            fail("Get byName should've succeeded", e);
        }
        if (StringUtils.isNotBlank(typeDef.getGuid())) {
            try {
                AtlasBaseTypeDef byGuid = null;
                if (typeDef.getCategory() == TypeCategory.ENUM) {
                    byGuid = clientV2.getEnumDefByGuid(typeDef.getGuid());
                } else if (typeDef.getCategory() == TypeCategory.ENTITY) {
                    byGuid = clientV2.getEntityDefByGuid(typeDef.getGuid());
                } else if (typeDef.getCategory() == TypeCategory.CLASSIFICATION) {
                    byGuid = clientV2.getClassificationDefByGuid(typeDef.getGuid());
                } else if (typeDef.getCategory() == TypeCategory.STRUCT) {
                    byGuid = clientV2.getStructDefByGuid(typeDef.getGuid());
                } else if (typeDef.getCategory() == TypeCategory.RELATIONSHIP) {
                    byGuid = clientV2.getRelationshipDefByGuid(typeDef.getGuid());
                } else if (typeDef.getCategory() == TypeCategory.BUSINESS_METADATA) {
                    byGuid = clientV2.getBusinessMetadataDefGuid(typeDef.getGuid());
                }
                assertNotNull(byGuid);
            } catch (AtlasServiceException e) {
                fail("Get byGuid should've succeeded", e);
            }
        }
    }

    private void emptyTypeDefs(AtlasTypesDef def) {
        def.getEnumDefs().clear();
        def.getStructDefs().clear();
        def.getClassificationDefs().clear();
        def.getEntityDefs().clear();
        def.getRelationshipDefs().clear();
        def.getBusinessMetadataDefs().clear();
    }

    private void checkIfTypeExists(String typeName) throws Exception {
        int retryCount = 0;
        int maxRetries = 3;
        int sleepTime  = 5000;

        while (true) {
            try {
                boolean typeExists = atlasClientV2.typeWithNameExists(typeName);

                if (!typeExists) {
                    throw new AtlasBaseException(TYPE_NAME_NOT_FOUND, typeName);
                } else {
                    break;
                }
            } catch (AtlasBaseException e) {
                Thread.sleep(sleepTime);

                if (++retryCount == maxRetries) throw e;
            }
        }
    }
}
