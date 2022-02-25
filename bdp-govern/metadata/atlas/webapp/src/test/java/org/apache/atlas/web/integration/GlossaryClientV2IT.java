/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.atlas.web.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.atlas.AtlasClientV2;
import org.apache.atlas.AtlasServiceException;
import org.apache.atlas.bulkimport.BulkImportResponse;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.enums.AtlasTermRelationshipStatus;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.utils.AuthenticationUtil;
import org.apache.atlas.utils.TestResourceFileUtils;
import org.apache.atlas.web.TestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;

public class GlossaryClientV2IT extends BaseResourceIT {
    private static final Logger LOG = LoggerFactory.getLogger(GlossaryClientV2IT.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private AtlasTypesDef typeDefinitions;
    private AtlasClientV2 clientV2;
    private AtlasGlossary educationGlossary, healthCareGlossary;
    private AtlasGlossaryTerm educationTerm, schoolEducationTerm;
    private AtlasGlossaryCategory educationCategory;
    private List<AtlasRelatedObjectId> relatedObjectIds;
    private AtlasEntityHeader entityHeader;

    @BeforeClass
    public void setUp() throws Exception {
        super.setUp();
        typeDefinitions = setForGlossary();
        createType(typeDefinitions);

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
    public void testCreateGlossary() throws Exception {
        educationGlossary = createAndGetGlossary("Education");
        assertNotNull(educationGlossary);
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testGetGlossaryByGuid() throws Exception {
        healthCareGlossary = createAndGetGlossary("HealthCare");
        AtlasGlossary atlasGlossaryByGuid = atlasClientV2.getGlossaryByGuid(healthCareGlossary.getGuid());
        assertNotNull(atlasGlossaryByGuid);
        assertEquals(healthCareGlossary.getGuid(), atlasGlossaryByGuid.getGuid());
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testGetDetailGlossary() throws Exception {
        AtlasGlossary.AtlasGlossaryExtInfo extInfo = atlasClientV2.getGlossaryExtInfo(educationGlossary.getGuid());
        assertNotNull(extInfo);
        assertEquals(educationGlossary.getGuid(), extInfo.getGuid());
    }

    @Test(dependsOnMethods = "testGetGlossaryByGuid")
    public void getAllGlossary() throws Exception {
        List<AtlasGlossary> list = atlasClientV2.getAllGlossaries("ASC", 5, 0);
        assertNotNull(list);
        assertEquals(list.size(), 3);
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void updateGlossaryByGuid() throws Exception {
        AtlasGlossary newGlossary = new AtlasGlossary();
        newGlossary.setLanguage("English");
        newGlossary.setName("updateGlossary");
        AtlasGlossary updated = atlasClientV2.updateGlossaryByGuid(educationGlossary.getGuid(), newGlossary);
        assertNotNull(updated);
        assertEquals(updated.getGuid(), educationGlossary.getGuid());
        assertEquals(updated.getLanguage(), newGlossary.getLanguage());
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testCreateTerm() throws Exception {
        AtlasGlossaryTerm term = new AtlasGlossaryTerm();
        AtlasGlossaryHeader header = new AtlasGlossaryHeader();
        header.setGlossaryGuid(educationGlossary.getGuid());
        header.setDisplayText(educationGlossary.getName());
        term.setAnchor(header);
        term.setName("termForEducation");
        educationTerm = atlasClientV2.createGlossaryTerm(term);
        assertNotNull(educationTerm);
        assertNotNull(educationTerm.getGuid());
    }

    @Test(dependsOnMethods = "testCreateTerm")
    public void testGetGlossaryTerm() throws Exception {
        AtlasGlossaryTerm term = atlasClientV2.getGlossaryTerm(educationTerm.getGuid());
        assertNotNull(term);
        assertEquals(term.getGuid(), educationTerm.getGuid());
    }

    @Test(dependsOnMethods = "testCreateTerm")
    public void testGetGlossaryTerms() throws Exception {
        AtlasRelatedTermHeader relatedTermHeader = new AtlasRelatedTermHeader();
        relatedTermHeader.setTermGuid(educationTerm.getGuid());
        relatedTermHeader.setDescription("test description");
        relatedTermHeader.setExpression("test expression");
        relatedTermHeader.setSource("School");
        relatedTermHeader.setSteward("School");
        relatedTermHeader.setStatus(AtlasTermRelationshipStatus.ACTIVE);
        schoolEducationTerm = new AtlasGlossaryTerm();
        AtlasGlossaryHeader header = new AtlasGlossaryHeader();
        header.setGlossaryGuid(educationGlossary.getGuid());
        header.setDisplayText(educationGlossary.getName());
        schoolEducationTerm.setAnchor(header);
        schoolEducationTerm.setName("termForSchool");
        schoolEducationTerm.setSeeAlso(Collections.singleton(relatedTermHeader));

        schoolEducationTerm = clientV2.createGlossaryTerm(schoolEducationTerm);
        assertNotNull(schoolEducationTerm);
        assertNotNull(schoolEducationTerm.getGuid());

        //Getting multiple terms
        List<AtlasGlossaryTerm> terms = atlasClientV2.getGlossaryTerms(educationGlossary.getGuid(), "ASC", 2, 0);
        assertNotNull(terms);
        assertEquals(terms.size(), 2);
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testCreateGlossaryCategory() throws Exception {
        AtlasGlossaryCategory category = new AtlasGlossaryCategory();
        AtlasGlossaryHeader header = new AtlasGlossaryHeader();
        header.setGlossaryGuid(educationGlossary.getGuid());
        header.setDisplayText(educationGlossary.getName());
        category.setAnchor(header);
        category.setName("categoryForEducation");
        educationCategory = atlasClientV2.createGlossaryCategory(category);
        assertNotNull(educationCategory);
        assertNotNull(educationCategory.getGuid());
    }

    @Test(dependsOnMethods = "testCreateGlossaryCategory")
    public void testCreateGlossaryCategories() throws Exception {
        List<AtlasGlossaryCategory> glossaryCategories = new ArrayList<>();

        AtlasGlossaryCategory category1 = new AtlasGlossaryCategory();
        AtlasGlossaryHeader header1 = new AtlasGlossaryHeader();
        header1.setGlossaryGuid(healthCareGlossary.getGuid());
        header1.setDisplayText(healthCareGlossary.getName());
        category1.setAnchor(header1);
        category1.setName("category1ForEducation");
        glossaryCategories.add(category1);
        //Setting different category
        AtlasGlossaryCategory category2 = new AtlasGlossaryCategory();
        category2.setAnchor(header1);
        category2.setName("category2ForEducation");
        glossaryCategories.add(category2);

        List<AtlasGlossaryCategory> list = atlasClientV2.createGlossaryCategories(glossaryCategories);
        assertNotNull(list);
        assertEquals(list.size(), 2);
    }

    @Test(dependsOnMethods = "testCreateGlossaryCategory")
    public void testGetGlossaryByCategory() throws Exception {
        AtlasGlossaryCategory atlasGlossaryCategory = atlasClientV2.getGlossaryCategory(educationCategory.getGuid());
        assertNotNull(atlasGlossaryCategory);
        assertEquals(atlasGlossaryCategory.getGuid(), educationCategory.getGuid());
    }

    @Test(dependsOnMethods = "testGetGlossaryByGuid")
    public void testCreateGlossaryTerms() throws Exception {
        List<AtlasGlossaryTerm> list = new ArrayList<>();
        int index = 0;
        List<AtlasGlossary> glossaries = atlasClientV2.getAllGlossaries("ASC", 5, 0);
        List<AtlasGlossary> glossaryList = mapper.convertValue(
                glossaries,
                new TypeReference<List<AtlasGlossary>>() {
                });

        for (AtlasGlossary glossary : glossaryList) {
            AtlasGlossaryTerm term = new AtlasGlossaryTerm();
            AtlasGlossaryHeader header = new AtlasGlossaryHeader();
            header.setGlossaryGuid(glossary.getGuid());
            header.setDisplayText(glossary.getName());
            term.setAnchor(header);
            term.setName("termName" + index);
            list.add(term);
            index++;
        }
        List<AtlasGlossaryTerm> termList = atlasClientV2.createGlossaryTerms(list);
        assertNotNull(termList);
        assertEquals(termList.size(), 3);
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testPartialUpdateGlossaryByGuid() throws Exception {
        Map<String, String> partialUpdates = new HashMap<>();
        partialUpdates.put("shortDescription", "shortDescription");
        partialUpdates.put("longDescription", "longDescription");
        AtlasGlossary atlasGlossary = atlasClientV2.partialUpdateGlossaryByGuid(educationGlossary.getGuid(), partialUpdates);
        assertNotNull(atlasGlossary);
        assertEquals(atlasGlossary.getShortDescription(), "shortDescription");
        assertEquals(atlasGlossary.getLongDescription(), "longDescription");
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testUpdateGlossaryTermByGuid() throws Exception {
        AtlasGlossaryTerm term = new AtlasGlossaryTerm(educationTerm);
        term.setAbbreviation("trm");
        AtlasGlossaryTerm responseTerm = atlasClientV2.updateGlossaryTermByGuid(educationTerm.getGuid(), term);
        assertNotNull(responseTerm);
        assertEquals(responseTerm.getAbbreviation(), term.getAbbreviation());
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testUpdateGlossaryCategoryByGuid() throws Exception {
        AtlasGlossaryCategory category = new AtlasGlossaryCategory(educationCategory);
        category.setLongDescription("this is about category");
        AtlasGlossaryCategory responseCategory = atlasClientV2.updateGlossaryCategoryByGuid(educationCategory.getGuid(), category);
        assertNotNull(responseCategory);
        assertEquals(responseCategory.getLongDescription(), category.getLongDescription());
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testPartialUpdateTermByGuid() throws Exception {
        Map<String, String> partialUpdates = new HashMap<>();
        partialUpdates.put("shortDescription", "shortDescriptionTerm");
        partialUpdates.put("longDescription", "longDescriptionTerm");

        AtlasGlossaryTerm term = atlasClientV2.partialUpdateTermByGuid(educationTerm.getGuid(), partialUpdates);
        assertNotNull(term);
        assertEquals(term.getShortDescription(), "shortDescriptionTerm");
        assertEquals(term.getLongDescription(), "longDescriptionTerm");
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testPartialUpdateCategoryByGuid() throws Exception {
        Map<String, String> partialUpdates = new HashMap<>();
        partialUpdates.put("shortDescription", "shortDescriptionCategory");
        partialUpdates.put("longDescription", "longDescriptionCategory");

        AtlasGlossaryCategory category = atlasClientV2.partialUpdateCategoryByGuid(educationCategory.getGuid(), partialUpdates);
        assertNotNull(category);
        assertEquals(category.getShortDescription(), "shortDescriptionCategory");
        assertEquals(category.getLongDescription(), "longDescriptionCategory");
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testGetGlossaryTermHeadersByGuid() throws Exception {
        List<AtlasRelatedTermHeader> list = atlasClientV2.getGlossaryTermHeaders(educationGlossary.getGuid(), "ASC", 2, 0);
        assertNotNull(list);
        assertEquals(list.size(), 1);
    }

    @Test(dependsOnMethods = "testCreateGlossaryCategories")
    public void testGetGlossaryCategoriesByGuid() throws Exception {
        List<AtlasGlossaryCategory> list = atlasClientV2.getGlossaryCategories(healthCareGlossary.getGuid(), "ASC", 2, 0);
        assertNotNull(list);
        assertEquals(list.size(), 2);
    }

    @Test(dependsOnMethods = "testCreateGlossaryCategories")
    public void testGetGlossaryCategoryHeaders() throws Exception {
        List<AtlasRelatedCategoryHeader> list = atlasClientV2.getGlossaryCategoryHeaders(healthCareGlossary.getGuid(), "ASC", 2, 0);
        assertNotNull(list);
        assertEquals(list.size(), 2);
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testGetCategoryTerms() throws Exception {
        List<AtlasRelatedTermHeader> list = atlasClientV2.getCategoryTerms(educationCategory.getGuid(), "ASC", 2, 0);
        assertNotNull(list);
        assertEquals(list.size(), 0);
    }

    @Test(dependsOnMethods = "testCreateGlossary")
    public void testGetAllRelatedTerms() throws Exception {
        Map<AtlasGlossaryTerm.Relation, Set<AtlasRelatedTermHeader>> map = atlasClientV2.getRelatedTerms(educationTerm.getGuid(), "ASC", 2, 0);
        assertNotNull(map);
    }

    @Test(dependsOnMethods = "testCreateTerm")
    public void testAssignTermToEntities() throws Exception {
        try {
            AtlasEntity entity = new AtlasEntity("Asset");
            entity.setAttribute("qualifiedName", "testAsset");
            entity.setAttribute("name", "testAsset");
            if (entityHeader == null) {
                entityHeader = createEntity(entity);
            }
            AtlasRelatedObjectId relatedObjectId = new AtlasRelatedObjectId();
            relatedObjectId.setGuid(entityHeader.getGuid());
            relatedObjectId.setTypeName(entityHeader.getTypeName());
            assertNotNull(relatedObjectId);
            relatedObjectIds = new ArrayList<>();
            relatedObjectIds.add(relatedObjectId);

            atlasClientV2.assignTermToEntities(educationTerm.getGuid(), relatedObjectIds);
            List<AtlasRelatedObjectId> assignedEntities = atlasClientV2.getEntitiesAssignedWithTerm(educationTerm.getGuid(), "ASC", 2, 0);
            assertNotNull(assignedEntities);
            assertEquals(assignedEntities.size(), 1);
            List<AtlasRelatedObjectId> entityList = mapper.convertValue(
                    assignedEntities,
                    new TypeReference<List<AtlasRelatedObjectId>>() {
                    });
            String relationshipGuid = entityList.get(0).getRelationshipGuid();
            assertNotNull(relationshipGuid);
            relatedObjectId.setRelationshipGuid(relationshipGuid);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Test(dependsOnMethods = "testAssignTermToEntities")
    public void testDisassociateTermAssignmentFromEntities() throws Exception {
        atlasClientV2.disassociateTermFromEntities(educationTerm.getGuid(), relatedObjectIds);
        AtlasGlossaryTerm term = atlasClientV2.getGlossaryTerm(educationTerm.getGuid());
        atlasClientV2.deleteEntityByGuid(entityHeader.getGuid());
        assertNotNull(term);
        assertNull(term.getAssignedEntities());
    }

    @Test(dependsOnMethods = "testCreateGlossaryCategory")
    public void testGetRelatedCategories() throws Exception {
        Map<String, List<AtlasRelatedCategoryHeader>> map = atlasClientV2.getRelatedCategories(educationCategory.getGuid(), "ASC", 1, 0);
        assertEquals(map.size(), 0);
    }

    @Test(dependsOnMethods = "testDeleteGlossaryTerm")
    public void testDeleteGlossary() throws Exception {
        emptyTypeDefs(typeDefinitions);
        atlasClientV2.deleteGlossaryByGuid(educationGlossary.getGuid());
        atlasClientV2.deleteGlossaryByGuid(healthCareGlossary.getGuid());
        try {
            atlasClientV2.getGlossaryByGuid(healthCareGlossary.getGuid());
        } catch (AtlasServiceException ex) {
            assertNotNull(ex.getStatus());
            assertEquals(ex.getStatus(), ClientResponse.Status.NOT_FOUND);
        }
        try {
            atlasClientV2.getGlossaryByGuid(educationGlossary.getGuid());
        } catch (AtlasServiceException ex) {
            assertNotNull(ex.getStatus());
            assertEquals(ex.getStatus(), ClientResponse.Status.NOT_FOUND);
        }
    }

    @Test(dependsOnMethods = "testDisassociateTermAssignmentFromEntities")
    public void testDeleteGlossaryTerm() throws Exception {
        atlasClientV2.deleteGlossaryTermByGuid(educationTerm.getGuid());
        try {
            atlasClientV2.getGlossaryTerm(educationTerm.getGuid());
        } catch (AtlasServiceException ex) {
            assertNotNull(ex.getStatus());
            assertEquals(ex.getStatus(), ClientResponse.Status.NOT_FOUND);
        }
    }

    @Test(dependsOnMethods = "testGetRelatedCategories")
    public void testDeleteGlossaryCategory() throws Exception {
        atlasClientV2.deleteGlossaryCategoryByGuid(educationCategory.getGuid());
        try {
            atlasClientV2.getGlossaryCategory(educationCategory.getGuid());
        } catch (AtlasServiceException ex) {
            assertNotNull(ex.getStatus());
            assertEquals(ex.getStatus(), ClientResponse.Status.NOT_FOUND);
        }
    }

    @Test()
    public void testProduceTemplate() {
        try {
            String template = atlasClientV2.getGlossaryImportTemplate();

            assertNotNull(template);
        } catch (AtlasServiceException ex) {
            fail("Deletion should've succeeded");
        }
    }

    @Test()
    public void testImportGlossaryData() {
        try {
            String             filePath = TestResourceFileUtils.getTestFilePath("template.csv");
            BulkImportResponse terms    = atlasClientV2.importGlossary(filePath);

            assertNotNull(terms);

            assertEquals(terms.getSuccessImportInfoList().size(), 1);

        } catch (AtlasServiceException ex) {
            fail("Import GlossaryData should've succeeded : "+ex);
        }
    }

    private AtlasGlossary createAndGetGlossary(String name) throws Exception {
        AtlasGlossary atlasGlossary = new AtlasGlossary();
        atlasGlossary.setName(name);
        return atlasClientV2.createGlossary(atlasGlossary);
    }

    private void emptyTypeDefs(AtlasTypesDef def) {
        def.getEnumDefs().clear();
        def.getStructDefs().clear();
        def.getClassificationDefs().clear();
        def.getEntityDefs().clear();
        def.getRelationshipDefs().clear();
        def.getBusinessMetadataDefs().clear();
    }

    private AtlasTypesDef setForGlossary() throws IOException {
        String filePath = TestUtils.getGlossaryType();
        String json = FileUtils.readFileToString(new File(filePath));
        return AtlasType.fromJson(json, AtlasTypesDef.class);
    }

    protected String randomString() {
        //names cannot start with a digit
        return RandomStringUtils.randomAlphabetic(1) + RandomStringUtils.randomAlphanumeric(9);
    }

}
