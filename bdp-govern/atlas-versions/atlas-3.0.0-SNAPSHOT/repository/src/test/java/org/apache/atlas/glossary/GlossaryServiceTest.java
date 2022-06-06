/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.atlas.glossary;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.SortOrder;
import org.apache.atlas.TestModules;
import org.apache.atlas.bulkimport.BulkImportResponse;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.glossary.AtlasGlossary;
import org.apache.atlas.model.glossary.AtlasGlossaryCategory;
import org.apache.atlas.model.glossary.AtlasGlossaryTerm;
import org.apache.atlas.model.glossary.enums.AtlasTermRelationshipStatus;
import org.apache.atlas.model.glossary.relations.AtlasGlossaryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedCategoryHeader;
import org.apache.atlas.model.glossary.relations.AtlasRelatedTermHeader;
import org.apache.atlas.model.glossary.relations.AtlasTermCategorizationHeader;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasRelatedObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.model.typedef.AtlasClassificationDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.store.graph.AtlasEntityStore;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.apache.atlas.util.FileUtils;
import org.apache.atlas.utils.AtlasJson;
import org.apache.atlas.utils.TestLoadModelUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Guice(modules = TestModules.TestOnlyModule.class)
public class GlossaryServiceTest {
    private static final Logger LOG = LoggerFactory.getLogger(GlossaryServiceTest.class);

    @Inject
    private GlossaryService   glossaryService;
    @Inject
    private AtlasTypeDefStore typeDefStore;
    @Inject
    private AtlasTypeRegistry typeRegistry;
    @Inject
    private AtlasEntityStore entityStore;

    private AtlasGlossary     bankGlossary, creditUnionGlossary;
    private AtlasGlossaryTerm checkingAccount, savingsAccount, fixedRateMortgage, adjustableRateMortgage;
    private AtlasGlossaryCategory customerCategory, accountCategory, mortgageCategory;

    private AtlasRelatedObjectId relatedObjectId;

    public static final String CSV_FILES   = "/csvFiles/";
    public static final String EXCEL_FILES = "/excelFiles/";

    @DataProvider
    public static Object[][] getGlossaryTermsProvider() {
        return new Object[][]{
                // offset, limit, expected
                {0, -1, 7},
                {0, 2, 2},
                {2, 6, 5},
        };
    }

    @BeforeClass
    public void setupSampleGlossary() {
        try {
            TestLoadModelUtils.loadAllModels("0000-Area0", typeDefStore, typeRegistry);
        } catch (AtlasBaseException | IOException e) {
            throw new SkipException("SubjectArea model loading failed");
        }

        try {
            AtlasClassificationDef classificationDef = new AtlasClassificationDef("TestClassification", "Test only classification");
            AtlasTypesDef          typesDef          = new AtlasTypesDef();
            typesDef.setClassificationDefs(Arrays.asList(classificationDef));
            typeDefStore.createTypesDef(typesDef);
        } catch (AtlasBaseException e) {
            throw new SkipException("Test classification creation failed");
        }

        // Glossary
        bankGlossary = new AtlasGlossary();
        bankGlossary.setQualifiedName("testBankingGlossary");
        bankGlossary.setName("Banking glossary");
        bankGlossary.setShortDescription("Short description");
        bankGlossary.setLongDescription("Long description");
        bankGlossary.setUsage("N/A");
        bankGlossary.setLanguage("en-US");

        creditUnionGlossary = new AtlasGlossary();
        creditUnionGlossary.setQualifiedName("testCreditUnionGlossary");
        creditUnionGlossary.setName("Credit union glossary");
        creditUnionGlossary.setShortDescription("Short description");
        creditUnionGlossary.setLongDescription("Long description");
        creditUnionGlossary.setUsage("N/A");
        creditUnionGlossary.setLanguage("en-US");

        // Category
        accountCategory = new AtlasGlossaryCategory();
        accountCategory.setName("Account categorization");
        accountCategory.setShortDescription("Short description");
        accountCategory.setLongDescription("Long description");

        customerCategory = new AtlasGlossaryCategory();
        customerCategory.setQualifiedName("customer@testBankingGlossary");
        customerCategory.setName("Customer category");
        customerCategory.setShortDescription("Short description");
        customerCategory.setLongDescription("Long description");

        mortgageCategory = new AtlasGlossaryCategory();
        mortgageCategory.setName("Mortgage categorization");
        mortgageCategory.setShortDescription("Short description");
        mortgageCategory.setLongDescription("Long description");

        // Terms
        checkingAccount = new AtlasGlossaryTerm();
        checkingAccount.setName("A checking account");
        checkingAccount.setShortDescription("Short description");
        checkingAccount.setLongDescription("Long description");
        checkingAccount.setAbbreviation("CHK");
        checkingAccount.setExamples(Arrays.asList("Personal", "Joint"));
        checkingAccount.setUsage("N/A");

        savingsAccount = new AtlasGlossaryTerm();
        savingsAccount.setQualifiedName("sav_acc@testBankingGlossary");
        savingsAccount.setName("A savings account");
        savingsAccount.setShortDescription("Short description");
        savingsAccount.setLongDescription("Long description");
        savingsAccount.setAbbreviation("SAV");
        savingsAccount.setExamples(Arrays.asList("Personal", "Joint"));
        savingsAccount.setUsage("N/A");

        fixedRateMortgage = new AtlasGlossaryTerm();
        fixedRateMortgage.setName("Conventional mortgage");
        fixedRateMortgage.setShortDescription("Short description");
        fixedRateMortgage.setLongDescription("Long description");
        fixedRateMortgage.setAbbreviation("FMTG");
        fixedRateMortgage.setExamples(Arrays.asList("15-yr", "30-yr"));
        fixedRateMortgage.setUsage("N/A");

        adjustableRateMortgage = new AtlasGlossaryTerm();
        adjustableRateMortgage.setQualifiedName("arm_mtg@testBankingGlossary");
        adjustableRateMortgage.setName("ARM loans");
        adjustableRateMortgage.setShortDescription("Short description");
        adjustableRateMortgage.setLongDescription("Long description");
        adjustableRateMortgage.setAbbreviation("ARMTG");
        adjustableRateMortgage.setExamples(Arrays.asList("5/1", "7/1", "10/1"));
        adjustableRateMortgage.setUsage("N/A");


    }

    @Test(groups = "Glossary.CREATE")
    public void testCreateGlossary() {
        try {
            AtlasGlossary created = glossaryService.createGlossary(bankGlossary);
            bankGlossary.setGuid(created.getGuid());
            created = glossaryService.createGlossary(creditUnionGlossary);
            creditUnionGlossary.setGuid(created.getGuid());
        } catch (AtlasBaseException e) {
            fail("Glossary creation should've succeeded", e);
        }

        // Duplicate create calls should fail with 409 Conflict
        try {
            glossaryService.createGlossary(bankGlossary);
            fail("Glossary duplicate creation should've failed");
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.GLOSSARY_ALREADY_EXISTS);
        }
        try {
            glossaryService.createGlossary(creditUnionGlossary);
            fail("Glossary duplicate creation should've failed");
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.GLOSSARY_ALREADY_EXISTS);
        }

        // Retrieve the glossary and see ensure no terms or categories are linked

        try {
            List<AtlasRelatedCategoryHeader> glossaryCategories = glossaryService.getGlossaryCategoriesHeaders(bankGlossary.getGuid(), 0, 10, SortOrder.ASCENDING);
            assertNotNull(glossaryCategories);
            assertEquals(glossaryCategories.size(), 0);

            glossaryCategories = glossaryService.getGlossaryCategoriesHeaders(creditUnionGlossary.getGuid(), 0, 10, SortOrder.ASCENDING);
            assertNotNull(glossaryCategories);
            assertEquals(glossaryCategories.size(), 0);
        } catch (AtlasBaseException e) {
            fail("Get glossary categories calls should've succeeded", e);
        }

        try {
            List<AtlasRelatedTermHeader> glossaryCategories = glossaryService.getGlossaryTermsHeaders(bankGlossary.getGuid(), 0, 10, SortOrder.ASCENDING);
            assertNotNull(glossaryCategories);
            assertEquals(glossaryCategories.size(), 0);

            glossaryCategories = glossaryService.getGlossaryTermsHeaders(creditUnionGlossary.getGuid(), 0, 10, SortOrder.ASCENDING);
            assertNotNull(glossaryCategories);
            assertEquals(glossaryCategories.size(), 0);
        } catch (AtlasBaseException e) {
            fail("Get glossary categories calls should've succeeded", e);
        }

        // Glossary anchor
        AtlasGlossaryHeader glossaryId = new AtlasGlossaryHeader();
        glossaryId.setGlossaryGuid(bankGlossary.getGuid());

        // Create terms
        checkingAccount.setAnchor(glossaryId);
        savingsAccount.setAnchor(glossaryId);
        fixedRateMortgage.setAnchor(glossaryId);

        adjustableRateMortgage.setAnchor(glossaryId);

        // Create glossary categories
        accountCategory.setAnchor(glossaryId);
        customerCategory.setAnchor(glossaryId);
        mortgageCategory.setAnchor(glossaryId);
    }

    @Test(groups = "Glossary.CREATE" , dependsOnMethods = "testCategoryCreation")
    public void testTermCreationWithoutAnyRelations() {
        try {
            checkingAccount = glossaryService.createTerm(checkingAccount);
            assertNotNull(checkingAccount);
            assertNotNull(checkingAccount.getGuid());
        } catch (AtlasBaseException e) {
            fail("Term creation should've succeeded", e);
        }
    }

    @Test(groups = "Glossary.CREATE" , dependsOnMethods = "testTermCreationWithoutAnyRelations")
    public void testTermCreateWithRelation() {
        try {
            AtlasRelatedTermHeader relatedTermHeader = new AtlasRelatedTermHeader();
            relatedTermHeader.setTermGuid(checkingAccount.getGuid());
            relatedTermHeader.setDescription("test description");
            relatedTermHeader.setExpression("test expression");
            relatedTermHeader.setSource("UT");
            relatedTermHeader.setSteward("UT");
            relatedTermHeader.setStatus(AtlasTermRelationshipStatus.ACTIVE);
            savingsAccount.setSeeAlso(Collections.singleton(relatedTermHeader));

            savingsAccount = glossaryService.createTerm(savingsAccount);
            assertNotNull(savingsAccount);
            assertNotNull(savingsAccount.getGuid());
        } catch (AtlasBaseException e) {
            fail("Term creation with relation should've succeeded", e);
        }
    }

    @Test(groups = "Glossary.CREATE" , dependsOnMethods = "testCategoryCreation")
    public void testTermCreationWithCategory() {
        try {
            AtlasTermCategorizationHeader termCategorizationHeader = new AtlasTermCategorizationHeader();
            termCategorizationHeader.setCategoryGuid(mortgageCategory.getGuid());
            termCategorizationHeader.setDescription("Test description");
            termCategorizationHeader.setStatus(AtlasTermRelationshipStatus.DRAFT);

            fixedRateMortgage.setCategories(Collections.singleton(termCategorizationHeader));
            adjustableRateMortgage.setCategories(Collections.singleton(termCategorizationHeader));

            List<AtlasGlossaryTerm> terms = glossaryService.createTerms(Arrays.asList(fixedRateMortgage, adjustableRateMortgage));
            fixedRateMortgage.setGuid(terms.get(0).getGuid());
            adjustableRateMortgage.setGuid(terms.get(1).getGuid());
        } catch (AtlasBaseException e) {
            fail("Term creation should've succeeded", e);
        }
    }

    @Test(groups = "Glossary.CREATE" , dependsOnMethods = "testCreateGlossary")
    public void testCategoryCreation() {
        try {
            customerCategory = glossaryService.createCategory(customerCategory);

            AtlasRelatedCategoryHeader parentHeader = new AtlasRelatedCategoryHeader();
            parentHeader.setCategoryGuid(customerCategory.getGuid());

            // Test parent relation
            accountCategory.setParentCategory(parentHeader);
            List<AtlasGlossaryCategory> categories = glossaryService.createCategories(Arrays.asList(accountCategory, mortgageCategory));

            accountCategory.setGuid(categories.get(0).getGuid());
            assertNotNull(accountCategory.getParentCategory());
            assertEquals(accountCategory.getParentCategory().getCategoryGuid(), customerCategory.getGuid());
            assertTrue(accountCategory.getQualifiedName().endsWith(customerCategory.getQualifiedName()));

            mortgageCategory.setGuid(categories.get(1).getGuid());
            assertNull(mortgageCategory.getParentCategory());
        } catch (AtlasBaseException e) {
            fail("Category creation should've succeeded", e);
        }
    }


    @DataProvider
    public Object[][] getAllGlossaryDataProvider() {
        return new Object[][]{
                // limit, offset, sortOrder, expected
                {1, 0, SortOrder.ASCENDING, 1},
                {5, 0, SortOrder.ASCENDING, 2},
                {10, 0, SortOrder.ASCENDING, 2},
                {1, 1, SortOrder.ASCENDING, 1},
                {5, 1, SortOrder.ASCENDING, 1},
                {10, 1, SortOrder.ASCENDING, 1},
                {1, 2, SortOrder.ASCENDING, 0},
                {5, 2, SortOrder.ASCENDING, 0},
                {10, 2, SortOrder.ASCENDING, 0},
        };
    }

    @Test(dataProvider = "getAllGlossaryDataProvider", groups = "Glossary.GET", dependsOnGroups = "Glossary.CREATE")
    public void testGetAllGlossaries(int limit, int offset, SortOrder sortOrder, int expected) {
        try {
            List<AtlasGlossary> glossaries = glossaryService.getGlossaries(limit, offset, sortOrder);
            assertEquals(glossaries.size(), expected);
        } catch (AtlasBaseException e) {
            fail("Get glossaries should've succeeded", e);
        }
    }

    @Test(groups = "Glossary.UPDATE", dependsOnGroups = "Glossary.CREATE")
    public void testUpdateGlossary() {
        try {
            bankGlossary = glossaryService.getGlossary(bankGlossary.getGuid());
            bankGlossary.setShortDescription("Updated short description");
            bankGlossary.setLongDescription("Updated long description");

            AtlasGlossary updatedGlossary = glossaryService.updateGlossary(bankGlossary);
            assertNotNull(updatedGlossary);
            assertEquals(updatedGlossary.getGuid(), bankGlossary.getGuid());
//            assertEquals(updatedGlossary.getCategories(), bankGlossary.getCategories());
//            assertEquals(updatedGlossary.getTerms(), bankGlossary.getTerms());
//            assertEquals(updatedGlossary, bankGlossary);

            // There's some weirdness around the equality check of HashSet, hence the conversion to ArrayList
            ArrayList<AtlasRelatedCategoryHeader> a = new ArrayList<>(updatedGlossary.getCategories());
            ArrayList<AtlasRelatedCategoryHeader> b = new ArrayList<>(bankGlossary.getCategories());
            assertEquals(a, b);
        } catch (AtlasBaseException e) {
            fail("Glossary fetch/update should've succeeded", e);
        }
    }

    @Test(dependsOnGroups = {"Glossary.MIGRATE"})
    public void testInvalidFetches() {
        try {
            glossaryService.getGlossary(mortgageCategory.getGuid());
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.UNEXPECTED_TYPE);
        }

        try {
            glossaryService.getTerm(bankGlossary.getGuid());
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.UNEXPECTED_TYPE);
        }

        try {
            glossaryService.getCategory(savingsAccount.getGuid());
        } catch (AtlasBaseException e) {
            assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.UNEXPECTED_TYPE);
        }
    }

    @Test(dependsOnMethods = "testInvalidFetches") // Should be the last test
    public void testDeleteGlossary() {
        try {
            glossaryService.deleteGlossary(bankGlossary.getGuid());
            // Fetch deleted glossary
            try {
                glossaryService.getGlossary(bankGlossary.getGuid());
            } catch (AtlasBaseException e) {
                assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }

            // Fetch delete terms
            try {
                glossaryService.getTerm(fixedRateMortgage.getGuid());
            } catch (AtlasBaseException e) {
                assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }
            try {
                glossaryService.getTerm(adjustableRateMortgage.getGuid());
            } catch (AtlasBaseException e) {
                assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }
            try {
                glossaryService.getTerm(savingsAccount.getGuid());
            } catch (AtlasBaseException e) {
                assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }
            try {
                glossaryService.getTerm(checkingAccount.getGuid());
            } catch (AtlasBaseException e) {
                assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }

            // Fetch deleted categories
            try {
                glossaryService.getCategory(customerCategory.getGuid());
            } catch (AtlasBaseException e) {
                assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }
            try {
                glossaryService.getCategory(accountCategory.getGuid());
            } catch (AtlasBaseException e) {
                assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }
            try {
                glossaryService.getCategory(mortgageCategory.getGuid());
            } catch (AtlasBaseException e) {
                assertEquals(e.getAtlasErrorCode(), AtlasErrorCode.INSTANCE_GUID_NOT_FOUND);
            }
        } catch (AtlasBaseException e) {
            fail("Glossary delete should've succeeded", e);
        }
    }

    @Test(groups = "Glossary.UPDATE", dependsOnGroups = "Glossary.CREATE")
    public void testUpdateGlossaryTerm() {
        List<AtlasGlossaryTerm> glossaryTerms = new ArrayList<>();
        AtlasClassification classification = new AtlasClassification("TestClassification");
        for (AtlasGlossaryTerm term : Arrays.asList(checkingAccount, savingsAccount, fixedRateMortgage, adjustableRateMortgage)) {
            try {
                glossaryTerms.add(glossaryService.getTerm(term.getGuid()));
            } catch (AtlasBaseException e) {
                fail("Fetch of GlossaryTerm should've succeeded", e);
            }
        }
        for (AtlasGlossaryTerm t : glossaryTerms) {
            try {
                t.setShortDescription("Updated short description");
                t.setLongDescription("Updated long description");

                entityStore.addClassifications(t.getGuid(), Arrays.asList(classification));

                AtlasGlossaryTerm updatedTerm = glossaryService.updateTerm(t);
                assertNotNull(updatedTerm);
                assertEquals(updatedTerm.getGuid(), t.getGuid());
                assertNotNull(updatedTerm.getClassifications());
                assertEquals(updatedTerm.getClassifications().size(), 1);
            } catch (AtlasBaseException e) {
                fail("Glossary term fetch/update should've succeeded", e);
            }
        }
    }

    @Test(groups = "Glossary.UPDATE", dependsOnGroups = "Glossary.CREATE")
    public void testUpdateGlossaryCategory() {
        List<AtlasGlossaryCategory> glossaryCategories = new ArrayList<>();
        for (AtlasGlossaryCategory glossaryCategory : Arrays.asList(customerCategory, accountCategory, mortgageCategory)) {
            try {
                glossaryCategories.add(glossaryService.getCategory(glossaryCategory.getGuid()));
            } catch (AtlasBaseException e) {
                fail("Category fetch should've succeeded", e);
            }
        }

        for (AtlasGlossaryCategory c : glossaryCategories) {
            try {
                c.setShortDescription("Updated short description");
                c.setLongDescription("Updated long description");

                AtlasGlossaryCategory updatedCategory = glossaryService.updateCategory(c);
                assertNotNull(updatedCategory);
                assertEquals(updatedCategory.getGuid(), c.getGuid());
            } catch (AtlasBaseException e) {
                fail("Glossary category fetching should've succeeded", e);
            }
        }

        // Unlink children
        try {
            customerCategory = glossaryService.getCategory(customerCategory.getGuid());
            customerCategory.setChildrenCategories(null);
            customerCategory = glossaryService.updateCategory(customerCategory);
            assertNotNull(customerCategory);
            assertNull(customerCategory.getChildrenCategories());

            accountCategory = glossaryService.getCategory(accountCategory.getGuid());
            assertNull(accountCategory.getParentCategory());
            assertTrue(accountCategory.getQualifiedName().endsWith(bankGlossary.getQualifiedName()));

            mortgageCategory = glossaryService.getCategory(mortgageCategory.getGuid());
            assertNull(mortgageCategory.getParentCategory());
            assertTrue(mortgageCategory.getQualifiedName().endsWith(bankGlossary.getQualifiedName()));



        } catch (AtlasBaseException e) {
            fail("Customer category fetch should've succeeded");
        }
    }

    @Test(groups = "Glossary.MIGRATE", dependsOnGroups = "Glossary.GET.postUpdate")
    public void testTermMigration() {
        assertNotNull(creditUnionGlossary);

        AtlasGlossaryHeader newGlossaryHeader = new AtlasGlossaryHeader();
        newGlossaryHeader.setGlossaryGuid(creditUnionGlossary.getGuid());

        try {
            checkingAccount = glossaryService.getTerm(checkingAccount.getGuid());
            savingsAccount = glossaryService.getTerm(savingsAccount.getGuid());

            checkingAccount.setAnchor(newGlossaryHeader);
            checkingAccount.setSeeAlso(null);
            savingsAccount.setAnchor(newGlossaryHeader);
            savingsAccount.setSeeAlso(null);

        } catch (AtlasBaseException e) {
            fail("Term fetch for migration should've succeeded", e);
        }

        try {
            checkingAccount = glossaryService.updateTerm(checkingAccount);
            assertNotNull(checkingAccount);
            assertTrue(CollectionUtils.isEmpty(checkingAccount.getSeeAlso()));
            savingsAccount = glossaryService.updateTerm(savingsAccount);
            assertNotNull(savingsAccount);
            assertTrue(CollectionUtils.isEmpty(savingsAccount.getSeeAlso()));
        } catch (AtlasBaseException e) {
            fail("Term anchor change should've succeeded", e);
        }

        try {
            List<AtlasRelatedTermHeader> terms = glossaryService.getGlossaryTermsHeaders(creditUnionGlossary.getGuid(), 0, 5, SortOrder.ASCENDING);
            assertNotNull(terms);
            assertEquals(terms.size(), 2);
        } catch (AtlasBaseException e) {
            fail("Term fetch for glossary should've succeeded", e);
        }
    }

    @Test(groups = "Glossary.MIGRATE", dependsOnGroups = "Glossary.GET.postUpdate")
    public void testCategoryMigration() {
        assertNotNull(creditUnionGlossary);

        AtlasGlossaryHeader newGlossaryHeader = new AtlasGlossaryHeader();
        newGlossaryHeader.setGlossaryGuid(creditUnionGlossary.getGuid());

        try {
            customerCategory = glossaryService.getCategory(customerCategory.getGuid());
            mortgageCategory = glossaryService.getCategory(mortgageCategory.getGuid());
            accountCategory = glossaryService.getCategory(accountCategory.getGuid());
        } catch (AtlasBaseException e) {
            fail("Category fetch for migration should've succeeded");
        }

        customerCategory.setAnchor(newGlossaryHeader);
        mortgageCategory.setAnchor(newGlossaryHeader);
        accountCategory.setAnchor(newGlossaryHeader);

        try {
            customerCategory = glossaryService.updateCategory(customerCategory);
            mortgageCategory = glossaryService.updateCategory(mortgageCategory);
            accountCategory = glossaryService.updateCategory(accountCategory);

            assertTrue(customerCategory.getQualifiedName().endsWith(creditUnionGlossary.getQualifiedName()));
            assertEquals(customerCategory.getAnchor().getGlossaryGuid(), newGlossaryHeader.getGlossaryGuid());
            assertTrue(accountCategory.getQualifiedName().endsWith(creditUnionGlossary.getQualifiedName()));
            assertEquals(accountCategory.getAnchor().getGlossaryGuid(), newGlossaryHeader.getGlossaryGuid());
            assertTrue(mortgageCategory.getQualifiedName().endsWith(creditUnionGlossary.getQualifiedName()));
            assertEquals(mortgageCategory.getAnchor().getGlossaryGuid(), newGlossaryHeader.getGlossaryGuid());
        } catch (AtlasBaseException e) {
            fail("Category anchor change should've succeeded");
        }

        try {
            List<AtlasRelatedCategoryHeader> categories = glossaryService.getGlossaryCategoriesHeaders(creditUnionGlossary.getGuid(), 0, 5, SortOrder.ASCENDING);
            assertNotNull(categories);
            assertEquals(categories.size(), 3);
        } catch (AtlasBaseException e) {
            fail("Category migration should've succeeded", e);
        }

        // Move the entire hierarchy back to the original glossary
        AtlasRelatedCategoryHeader child1 = new AtlasRelatedCategoryHeader();
        child1.setCategoryGuid(accountCategory.getGuid());

        AtlasRelatedCategoryHeader child2 = new AtlasRelatedCategoryHeader();
        child2.setCategoryGuid(mortgageCategory.getGuid());

        customerCategory.addChild(child1);
        customerCategory.addChild(child2);

        try {
            customerCategory = glossaryService.updateCategory(customerCategory);
            assertTrue(CollectionUtils.isNotEmpty(customerCategory.getChildrenCategories()));
        } catch (AtlasBaseException e) {
            fail("Children addition to Category should've succeeded");
        }

        customerCategory.setAnchor(newGlossaryHeader);
        newGlossaryHeader.setGlossaryGuid(bankGlossary.getGuid());
        try {
            customerCategory = glossaryService.getCategory(customerCategory.getGuid());
            assertTrue(CollectionUtils.isNotEmpty(customerCategory.getChildrenCategories()));
        } catch (AtlasBaseException e) {
            fail("Category fetch should've succeeded");
        }

        try {
            accountCategory = glossaryService.getCategory(accountCategory.getGuid());
            assertEquals(accountCategory.getAnchor().getGlossaryGuid(), customerCategory.getAnchor().getGlossaryGuid());
        } catch (AtlasBaseException e) {
            fail("Category fetch should've succeeded");
        }

        try {
            mortgageCategory = glossaryService.getCategory(mortgageCategory.getGuid());
            assertEquals(mortgageCategory.getAnchor().getGlossaryGuid(), customerCategory.getAnchor().getGlossaryGuid());
        } catch (AtlasBaseException e) {
            fail("Category fetch should've succeeded");
        }
    }

    @Test(groups = "Glossary.UPDATE", dependsOnGroups = "Glossary.CREATE")
    public void testAddTermsToCategory() {
        assertNotNull(accountCategory);
        try {
            accountCategory = glossaryService.getCategory(accountCategory.getGuid());
            assertTrue(CollectionUtils.isEmpty(accountCategory.getTerms()));
        } catch (AtlasBaseException e) {
            fail("Fetch of accountCategory should've succeeded", e);
        }
        for (AtlasGlossaryTerm term : Arrays.asList(checkingAccount, savingsAccount)) {
            try {
                AtlasGlossaryTerm      termEntry     = glossaryService.getTerm(term.getGuid());
                AtlasRelatedTermHeader relatedTermId = new AtlasRelatedTermHeader();
                relatedTermId.setTermGuid(termEntry.getGuid());
                relatedTermId.setStatus(AtlasTermRelationshipStatus.ACTIVE);
                relatedTermId.setSteward("UT");
                relatedTermId.setSource("UT");
                relatedTermId.setExpression("N/A");
                relatedTermId.setDescription("Categorization under account category");
                accountCategory.addTerm(relatedTermId);
            } catch (AtlasBaseException e) {
                fail("Term fetching should've succeeded", e);
            }
        }

        try {
            AtlasGlossaryCategory updated = glossaryService.updateCategory(accountCategory);
            assertNotNull(updated.getTerms());
            assertEquals(updated.getTerms().size(), 2);
            accountCategory = updated;
        } catch (AtlasBaseException e) {
            fail("Glossary category update should've succeeded", e);
        }

        assertNotNull(accountCategory);
        try {
            accountCategory = glossaryService.getCategory(accountCategory.getGuid());
        } catch (AtlasBaseException e) {
            fail("Fetch of accountCategory should've succeeded", e);
        }

        for (AtlasGlossaryTerm term : Arrays.asList(fixedRateMortgage, adjustableRateMortgage)) {
            try {
                AtlasGlossaryTerm      termEntry     = glossaryService.getTerm(term.getGuid());
                AtlasRelatedTermHeader relatedTermId = new AtlasRelatedTermHeader();
                relatedTermId.setTermGuid(termEntry.getGuid());
                relatedTermId.setStatus(AtlasTermRelationshipStatus.ACTIVE);
                relatedTermId.setSteward("UT");
                relatedTermId.setSource("UT");
                relatedTermId.setExpression("N/A");
                relatedTermId.setDescription("Categorization under mortgage category");

                mortgageCategory.addTerm(relatedTermId);
            } catch (AtlasBaseException e) {
                fail("Term fetching should've succeeded", e);
            }
        }

        try {
            AtlasGlossaryCategory updated = glossaryService.updateCategory(mortgageCategory);
            assertNotNull(updated.getTerms());
            assertEquals(updated.getTerms().size(), 2);
            mortgageCategory = updated;
        } catch (AtlasBaseException e) {
            fail("Glossary category update should've succeeded", e);
        }

    }

    @Test(groups = "Glossary.UPDATE", dependsOnGroups = "Glossary.CREATE")
    public void testAddGlossaryCategoryChildren() {
        assertNotNull(customerCategory);
        try {
            customerCategory = glossaryService.getCategory(customerCategory.getGuid());
            assertNull(customerCategory.getParentCategory());
        } catch (AtlasBaseException e) {
            fail("Fetch of accountCategory should've succeeded", e);
        }

        AtlasRelatedCategoryHeader id = new AtlasRelatedCategoryHeader();
        id.setCategoryGuid(mortgageCategory.getGuid());
        id.setDescription("Sub-category of customer");
        customerCategory.addChild(id);

        try {
            AtlasGlossaryCategory updateGlossaryCategory = glossaryService.updateCategory(customerCategory);
            assertNull(updateGlossaryCategory.getParentCategory());
            assertNotNull(updateGlossaryCategory.getChildrenCategories());
            LOG.debug(AtlasJson.toJson(updateGlossaryCategory));
        } catch (AtlasBaseException e) {
            fail("Sub category addition should've succeeded", e);
        }

        for (AtlasGlossaryCategory childCategory : Arrays.asList(accountCategory, mortgageCategory)) {
            try {
                AtlasGlossaryCategory child = glossaryService.getCategory(childCategory.getGuid());
                assertNotNull(child);
                assertNotNull(child.getParentCategory());
            } catch (AtlasBaseException e) {
                fail("Category fetch should've been a success", e);
            }
        }
    }

    @Test(groups = "Glossary.UPDATE", dependsOnGroups = "Glossary.CREATE")
    public void testTermAssignmentAndDissociation() {
        AtlasEntity assetEntity = new AtlasEntity("Asset");
        assetEntity.setAttribute("qualifiedName", "testAsset");
        assetEntity.setAttribute("name", "testAsset");

        try {
            EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(assetEntity), false);
            AtlasEntityHeader      firstEntityCreated = response.getFirstEntityCreated();
            relatedObjectId = new AtlasRelatedObjectId();
            relatedObjectId.setGuid(firstEntityCreated.getGuid());
            relatedObjectId.setTypeName(firstEntityCreated.getTypeName());
            assertNotNull(relatedObjectId);
        } catch (AtlasBaseException e) {
            fail("Entity creation should've succeeded", e);
        }

        try {
            glossaryService.assignTermToEntities(fixedRateMortgage.getGuid(), Arrays.asList(relatedObjectId));
        } catch (AtlasBaseException e) {
            fail("Term assignment to asset should've succeeded", e);
        }

        try {
            List<AtlasRelatedObjectId> assignedEntities = glossaryService.getAssignedEntities(fixedRateMortgage.getGuid(), 0, 1, SortOrder.ASCENDING);
            assertNotNull(assignedEntities);
            assertEquals(assignedEntities.size(), 1);
            String relationshipGuid = assignedEntities.get(0).getRelationshipGuid();
            assertNotNull(relationshipGuid);
            relatedObjectId.setRelationshipGuid(relationshipGuid);
        } catch (AtlasBaseException e) {
            fail("Term fetch should've succeeded",e);
        }

        // Dissociate term from entities
        try {
            glossaryService.removeTermFromEntities(fixedRateMortgage.getGuid(), Arrays.asList(relatedObjectId));
            AtlasGlossaryTerm term = glossaryService.getTerm(fixedRateMortgage.getGuid());
            assertNotNull(term);
            assertNull(term.getAssignedEntities());
        } catch (AtlasBaseException e) {
            fail("Term update should've succeeded", e);
        }

        try {
            entityStore.deleteById(relatedObjectId.getGuid());
        } catch (AtlasBaseException e) {
            fail("Entity delete should've succeeded");
        }
    }

    @Test(groups = "Glossary.UPDATE", dependsOnGroups = "Glossary.CREATE")
    public void testTermRelation() {
        AtlasRelatedTermHeader relatedTerm = new AtlasRelatedTermHeader();
        relatedTerm.setTermGuid(savingsAccount.getGuid());
        relatedTerm.setStatus(AtlasTermRelationshipStatus.DRAFT);
        relatedTerm.setSteward("UT");
        relatedTerm.setSource("UT");
        relatedTerm.setExpression("N/A");
        relatedTerm.setDescription("Related term");

        assertNotNull(checkingAccount);
        try {
            checkingAccount = glossaryService.getTerm(checkingAccount.getGuid());
        } catch (AtlasBaseException e) {
            fail("Glossary term fetch should've been a success", e);
        }

        checkingAccount.setSeeAlso(new HashSet<>(Arrays.asList(relatedTerm)));

        try {
            checkingAccount = glossaryService.updateTerm(checkingAccount);
            assertNotNull(checkingAccount.getSeeAlso());
            assertEquals(checkingAccount.getSeeAlso().size(), 1);
        } catch (AtlasBaseException e) {
            fail("RelatedTerm association should've succeeded", e);
        }

        relatedTerm.setTermGuid(fixedRateMortgage.getGuid());

        assertNotNull(adjustableRateMortgage);
        try {
            adjustableRateMortgage = glossaryService.getTerm(adjustableRateMortgage.getGuid());
        } catch (AtlasBaseException e) {
            fail("Glossary term fetch should've been a success", e);
        }

        adjustableRateMortgage.setSeeAlso(new HashSet<>(Arrays.asList(relatedTerm)));

        try {
            adjustableRateMortgage = glossaryService.updateTerm(adjustableRateMortgage);
            assertNotNull(adjustableRateMortgage.getSeeAlso());
            assertEquals(adjustableRateMortgage.getSeeAlso().size(), 1);
        } catch (AtlasBaseException e) {
            fail("RelatedTerm association should've succeeded", e);
        }
    }

    @Test(dataProvider = "getGlossaryTermsProvider" , groups = "Glossary.GET.postUpdate", dependsOnGroups = "Glossary.UPDATE")
    public void testGetGlossaryTerms(int offset, int limit, int expected) {
        String    guid      = bankGlossary.getGuid();
        SortOrder sortOrder = SortOrder.ASCENDING;

        try {
            List<AtlasRelatedTermHeader> glossaryTerms = glossaryService.getGlossaryTermsHeaders(guid, offset, limit, sortOrder);
            assertNotNull(glossaryTerms);
            assertEquals(glossaryTerms.size(), expected);
        } catch (AtlasBaseException e) {
            fail("Glossary term fetching should've succeeded", e);
        }
    }

    @DataProvider
    public Object[][] getGlossaryCategoriesProvider() {
        return new Object[][]{
                // offset, limit, expected
                {0, -1, 3},
                {0, 2, 2},
                {2, 5, 1},
        };
    }

    @Test(dataProvider = "getGlossaryCategoriesProvider" , groups = "Glossary.GET.postUpdate", dependsOnGroups = "Glossary.UPDATE")
    public void testGetGlossaryCategories(int offset, int limit, int expected) {
        String    guid      = bankGlossary.getGuid();
        SortOrder sortOrder = SortOrder.ASCENDING;

        try {
            List<AtlasRelatedCategoryHeader> glossaryCategories = glossaryService.getGlossaryCategoriesHeaders(guid, offset, limit, sortOrder);
            assertNotNull(glossaryCategories);
            assertEquals(glossaryCategories.size(), expected);
        } catch (AtlasBaseException e) {
            fail("Glossary term fetching should've succeeded");
        }
    }

    @DataProvider
    public Object[][] getCategoryTermsProvider() {
        return new Object[][]{
                // offset, limit, expected
                {0, -1, 2},
                {0, 2, 2},
                {1, 5, 1},
                {2, 5, 0},
        };
    }

    @Test(dataProvider = "getCategoryTermsProvider",  dependsOnGroups = "Glossary.CREATE")
    public void testGetCategoryTerms(int offset, int limit, int expected) {
        for (AtlasGlossaryCategory c : Arrays.asList(accountCategory, mortgageCategory)) {
            try {
                List<AtlasRelatedTermHeader> categoryTerms = glossaryService.getCategoryTerms(c.getGuid(), offset, limit, SortOrder.ASCENDING);
                assertNotNull(categoryTerms);
                assertEquals(categoryTerms.size(), expected);
            } catch (AtlasBaseException e) {
                fail("Category term retrieval should've been a success", e);
            }
        }
    }

    @Test
    public void testGetTemplate(){
        try {
            String glossaryTermHeaderListAsString = GlossaryTermUtils.getGlossaryTermHeaders();

            assertNotNull(glossaryTermHeaderListAsString);
            assertEquals(glossaryTermHeaderListAsString,"GlossaryName, TermName, ShortDescription, LongDescription, Examples, Abbreviation, Usage, AdditionalAttributes, TranslationTerms, ValidValuesFor, Synonyms, ReplacedBy, ValidValues, ReplacementTerms, SeeAlso, TranslatedTerms, IsA, Antonyms, Classifies, PreferredToTerms, PreferredTerms");
        } catch (Exception e) {
            fail("The Template for Glossary Term should've been a success",e);
        }
    }

    @Test( dependsOnGroups = "Glossary.CREATE" )
    public void testImportGlossaryData(){
        try {
            InputStream             inputStream   = getFile(CSV_FILES,"template_1.csv");
            BulkImportResponse bulkImportResponse = glossaryService.importGlossaryData(inputStream,"template_1.csv");

            assertNotNull(bulkImportResponse);
            assertEquals(bulkImportResponse.getSuccessImportInfoList().size(), 1);

            InputStream             inputStream1   = getFile(EXCEL_FILES,"template_1.xlsx");
            BulkImportResponse bulkImportResponse1 = glossaryService.importGlossaryData(inputStream1,"template_1.xlsx");

            assertNotNull(bulkImportResponse1);
            assertEquals(bulkImportResponse1.getSuccessImportInfoList().size(), 1);

            // With circular dependent relations
            InputStream             inputStream2   = getFile(CSV_FILES,"template_with_circular_relationship.csv");
            BulkImportResponse bulkImportResponse2 = glossaryService.importGlossaryData(inputStream2,"template_with_circular_relationship.csv");

            assertNotNull(bulkImportResponse2);
            assertEquals(bulkImportResponse2.getSuccessImportInfoList().size(), 3);
            assertEquals(bulkImportResponse2.getFailedImportInfoList().size(), 0);
        } catch (AtlasBaseException e){
            fail("The GlossaryTerm should have been created "+e);
        }
    }

    @Test
    public void testEmptyFileException() {
        InputStream inputStream = getFile(CSV_FILES, "empty.csv");

        try {
            glossaryService.importGlossaryData(inputStream, "empty.csv");
            fail("Error occurred : Failed to recognize the empty file.");
        } catch (AtlasBaseException e) {
            assertEquals(e.getMessage(),"No data found in the uploaded file");
        }
    }

    @Test
    public void testInvalidFileException() {
        InputStream inputStream = getFile(EXCEL_FILES, "invalid_xls.xls");

        try {
            BulkImportResponse bulkImportResponse = glossaryService.importGlossaryData(inputStream, "invalid_xls.xls");
            fail("Error occurred : Failed to recognize the invalid xls file.");
        } catch (AtlasBaseException e) {
            assertEquals(e.getMessage(),"Invalid XLS file");
        }
    }

    @Test
    public void testFileExtension() throws IOException {
        InputStream inputStream = getFile(CSV_FILES, "incorrectEXT.py");
        final String userDir  = System.getProperty("user.dir");
        String       filePath = getTestFilePath(userDir, CSV_FILES, "incorrectEXT.py");
        File         f          = new File(filePath);
        try {
            FileUtils.readFileData("incorrectEXT.py", inputStream);
            fail("Error occurred : Incorrect file extension.");
        } catch (AtlasBaseException e) {
            assertEquals(e.getMessage(),"The provided file type: " + f.getName() + " is not supported. Expected file formats are .csv and .xls.");
        }
    }

    @Test
    public void testIncorrectFileException() {
        InputStream inputStream = getFile(CSV_FILES, "incorrectFile.csv");

        try {
            BulkImportResponse bulkImportResponse = glossaryService.importGlossaryData(inputStream, "incorrectFile.csv");

            assertEquals(bulkImportResponse.getSuccessImportInfoList().size(),1);

            //Due to invalid Relation we get Failed message even the import succeeded for the term
            assertEquals(bulkImportResponse.getFailedImportInfoList().size(),1);
        } catch (AtlasBaseException e) {
            fail("The incorrect file exception should have handled "+e);
        }
    }

    private static InputStream getFile(String subDir, String fileName){
        final String userDir  = System.getProperty("user.dir");
        String       filePath = getTestFilePath(userDir, subDir, fileName);
        File         f        = new File(filePath);
        InputStream  fs       = null;

        try {
            fs = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            LOG.error("File could not be found at: {}", filePath, e);
        }

        return fs;
    }

    private static String getTestFilePath(String startPath, String subDir, String fileName) {
        if (StringUtils.isNotEmpty(subDir)) {
            return startPath + "/src/test/resources/" + subDir + "/" + fileName;
        } else {
            return startPath + "/src/test/resources/" + fileName;
        }
    }
}
