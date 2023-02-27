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
package org.apache.atlas.repository.userprofile;

import org.apache.atlas.AtlasErrorCode;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.SearchFilter;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.profile.AtlasUserProfile;
import org.apache.atlas.model.profile.AtlasUserSavedSearch;
import org.apache.atlas.model.typedef.AtlasEntityDef;
import org.apache.atlas.model.typedef.AtlasTypesDef;
import org.apache.atlas.repository.AtlasTestBase;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.util.FilterUtil;
import org.apache.atlas.store.AtlasTypeDefStore;
import org.apache.atlas.type.AtlasType;
import org.apache.atlas.type.AtlasTypeRegistry;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.apache.atlas.model.profile.AtlasUserSavedSearch.SavedSearchType.BASIC;
import static org.apache.atlas.utils.TestLoadModelUtils.loadModelFromJson;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

@Guice(modules = TestModules.TestOnlyModule.class)
public class UserProfileServiceTest extends AtlasTestBase {
    private UserProfileService userProfileService;
    private AtlasTypeDefStore  typeDefStore;

    private static final int NUM_USERS    = 2;
    private static final int NUM_SEARCHES = 4;

    @Inject
    public void UserProfileServiceTest(AtlasTypeRegistry  typeRegistry,
                                       AtlasTypeDefStore  typeDefStore,
                                       UserProfileService userProfileService) throws IOException, AtlasBaseException {
        this.typeDefStore       = typeDefStore;
        this.userProfileService = userProfileService;

        loadModelFromJson("0010-base_model.json", typeDefStore, typeRegistry);
    }

    @BeforeClass
    public void initialize() throws Exception {
        super.initialize();
    }

    @AfterClass
    public void clear() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @Test
    public void filterInternalType() throws AtlasBaseException {
        SearchFilter searchFilter = new SearchFilter();

        FilterUtil.addParamsToHideInternalType(searchFilter);

        AtlasTypesDef filteredTypeDefs = typeDefStore.searchTypesDef(searchFilter);

        assertNotNull(filteredTypeDefs);

        Optional<AtlasEntityDef> anyInternal = filteredTypeDefs.getEntityDefs().stream().filter(e -> e.getSuperTypes().contains("__internal")).findAny();

        assertFalse(anyInternal.isPresent());
    }

    @Test(dependsOnMethods = "filterInternalType")
    public void createsNewProfile() throws AtlasBaseException {
        for (int i = 0; i < NUM_USERS; i++) {
            AtlasUserProfile expected = getAtlasUserProfile(i);
            AtlasUserProfile actual   = userProfileService.saveUserProfile(expected);

            assertNotNull(actual);
            assertEquals(expected.getName(), actual.getName());
            assertEquals(expected.getFullName(), actual.getFullName());
            assertNotNull(actual.getGuid());
        }
    }

    @Test(dependsOnMethods = "createsNewProfile")
    public void saveSearchesForUser() throws AtlasBaseException {
        String           userName         = getIndexBasedUserName(0);
        SearchParameters searchParameters = getActualSearchParameters();

        for (int i = 0; i < NUM_SEARCHES; i++) {
            userProfileService.addSavedSearch(getDefaultSavedSearch(userName, getIndexBasedQueryName(i), searchParameters));
        }

        for (int i = 0; i < NUM_SEARCHES; i++) {
            AtlasUserSavedSearch savedSearch = userProfileService.getSavedSearch(userName, getIndexBasedQueryName(i));

            assertEquals(savedSearch.getName(), getIndexBasedQueryName(i));
            assertEquals(savedSearch.getSearchParameters(), searchParameters);
        }
    }

    @Test(dependsOnMethods = "saveSearchesForUser", expectedExceptions = AtlasBaseException.class)
    public void attemptToAddExistingSearch() throws AtlasBaseException {
        String           userName                = getIndexBasedUserName(0);
        SearchParameters expectedSearchParameter = getActualSearchParameters();

        for (int j = 0; j < NUM_SEARCHES; j++) {
            String               queryName = getIndexBasedQueryName(j);
            AtlasUserSavedSearch expected  = getDefaultSavedSearch(userName, queryName, expectedSearchParameter);
            AtlasUserSavedSearch actual    = userProfileService.addSavedSearch(expected);

            assertNotNull(actual);
            assertNotNull(actual.getGuid());
            assertEquals(actual.getOwnerName(), expected.getOwnerName());
            assertEquals(actual.getName(), expected.getName());
            assertEquals(actual.getSearchType(), expected.getSearchType());
            assertEquals(actual.getSearchParameters(), expected.getSearchParameters());
        }
    }

    @Test(dependsOnMethods = "attemptToAddExistingSearch")
    public void verifySavedSearchesForUser() throws AtlasBaseException {
        String                     userName = getIndexBasedUserName(0);
        List<AtlasUserSavedSearch> searches = userProfileService.getSavedSearches(userName);
        List<String>               names    = getIndexBasedQueryNamesList();

        for (int i = 0; i < names.size(); i++) {
            assertTrue(names.contains(searches.get(i).getName()), searches.get(i).getName() + " failed!");
        }
    }

    @Test(dependsOnMethods = "verifySavedSearchesForUser")
    public void verifyQueryConversionFromJSON() throws AtlasBaseException {
        List<AtlasUserSavedSearch> list = userProfileService.getSavedSearches("first-0");

        for (int i = 0; i < NUM_SEARCHES; i++) {
            SearchParameters sp   = list.get(i).getSearchParameters();
            String           json = AtlasType.toJson(sp);

            assertEquals(AtlasType.toJson(getActualSearchParameters()).replace("\n", "").replace(" ", ""), json);
        }
    }

    @Test(dependsOnMethods = "verifyQueryConversionFromJSON")
    public void addAdditionalSearchesForUser() throws AtlasBaseException {
        SearchParameters expectedSearchParameter = getActualSearchParameters();

        for (int i = 0; i < NUM_USERS; i++) {
            String userName = getIndexBasedUserName(i);

            for (int j = 0; j < 6; j++) {
                String               queryName = getIndexBasedQueryName(NUM_SEARCHES + j);
                AtlasUserSavedSearch actual    = userProfileService.addSavedSearch(getDefaultSavedSearch(userName, queryName, expectedSearchParameter));

                assertNotNull(actual);

                AtlasUserSavedSearch savedSearch = userProfileService.getSavedSearch(userName, queryName);

                assertNotNull(savedSearch);
                assertEquals(savedSearch.getSearchParameters(), expectedSearchParameter);
            }
        }
    }

    @Test(dependsOnMethods = { "addAdditionalSearchesForUser"})
    public void updateSearch() throws AtlasBaseException {
        String               userName  = getIndexBasedUserName(0);
        String               queryName = getIndexBasedQueryName(0);
        AtlasUserSavedSearch expected  = userProfileService.getSavedSearch(userName, queryName);

        assertNotNull(expected);

        SearchParameters sp = expected.getSearchParameters();

        sp.setClassification("new-classification");

        AtlasUserSavedSearch actual = userProfileService.updateSavedSearch(expected);

        assertNotNull(actual);
        assertNotNull(actual.getSearchParameters());
        assertEquals(actual.getSearchParameters().getClassification(), expected.getSearchParameters().getClassification());
    }

    @Test(dependsOnMethods = { "updateSearch" })
    public void deleteUsingGuid() throws AtlasBaseException {
        String               userName  = getIndexBasedUserName(0);
        String               queryName = getIndexBasedQueryName(1);
        AtlasUserSavedSearch expected  = userProfileService.getSavedSearch(userName, queryName);

        assertNotNull(expected);

        userProfileService.deleteSavedSearch(expected.getGuid());

        try {
            userProfileService.getSavedSearch(userName, queryName);
        } catch (AtlasBaseException ex) {
            assertEquals(ex.getAtlasErrorCode().name(), AtlasErrorCode.INSTANCE_BY_UNIQUE_ATTRIBUTE_NOT_FOUND.name());
        }
    }

    @Test(dependsOnMethods = { "deleteUsingGuid" })
    public void deleteSavedQuery() throws AtlasBaseException {
        String           userName = getIndexBasedUserName(0);
        AtlasUserProfile expected = userProfileService.getUserProfile(userName);

        assertNotNull(expected);

        int    searchCount          = expected.getSavedSearches().size();
        String queryNameToBeDeleted = getIndexBasedQueryName(NUM_SEARCHES - 2);

        userProfileService.deleteSearchBySearchName(userName, queryNameToBeDeleted);

        List<AtlasUserSavedSearch> savedSearchList = userProfileService.getSavedSearches(userName);

        assertEquals(savedSearchList.size(), searchCount - 1);
    }

    @Test(dependsOnMethods = { "deleteSavedQuery" })
    void deleteUser() throws AtlasBaseException {
        String           userName    = getIndexBasedUserName(0);
        AtlasUserProfile userProfile = userProfileService.getUserProfile(userName);

        if (userProfile.getSavedSearches() != null)  {
            for (AtlasUserSavedSearch savedSearch : userProfile.getSavedSearches()) {
                userProfileService.deleteSavedSearch(savedSearch.getGuid());
            }
        }

        userProfileService.deleteUserProfile(userName);

        try {
            userProfileService.getUserProfile(userName);
        } catch (AtlasBaseException ex) {
            assertEquals(ex.getAtlasErrorCode().name(), AtlasErrorCode.INSTANCE_BY_UNIQUE_ATTRIBUTE_NOT_FOUND.name());
        }
    }


    private static AtlasUserProfile getAtlasUserProfile(Integer s) {
        return new AtlasUserProfile(getIndexBasedUserName(s), String.format("first-%s last-%s", s, s));
    }

    private static String getIndexBasedUserName(Integer i) {
        return String.format("first-%s", i.toString());
    }

    private static String getIndexBasedQueryName(Integer i) {
        return String.format("testQuery-%s", i.toString());
    }

    private SearchParameters getActualSearchParameters() {
        SearchParameters sp = new SearchParameters();

        sp.setClassification("test-classification");
        sp.setQuery("g.v().has('__guid').__guid.toList()");
        sp.setLimit(10);
        sp.setTypeName("some-type");

        return sp;
    }

    private AtlasUserSavedSearch getDefaultSavedSearch(String userName, String queryName, SearchParameters expectedSearchParam) {
        return new AtlasUserSavedSearch(userName, queryName, BASIC, expectedSearchParam);
    }

    private List<String> getIndexBasedQueryNamesList() {
        List<String> list = new ArrayList<>();

        for (int i = 0; i < NUM_SEARCHES; i++) {
            list.add(getIndexBasedQueryName(i));
        }

        return list;
    }
}
