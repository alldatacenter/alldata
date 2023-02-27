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
package org.apache.atlas.discovery;

import org.apache.atlas.ApplicationProperties;
import org.apache.atlas.AtlasClient;
import org.apache.atlas.BasicTestSetup;
import org.apache.atlas.SortOrder;
import org.apache.atlas.TestModules;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasQuickSearchResult;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.discovery.QuickSearchParameters;
import org.apache.atlas.model.discovery.RelationshipSearchParameters;
import org.apache.atlas.model.discovery.SearchParameters;
import org.apache.atlas.model.discovery.AtlasAggregationEntry;
import org.apache.atlas.model.instance.AtlasClassification;
import org.apache.atlas.model.instance.AtlasEntity;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.model.instance.AtlasObjectId;
import org.apache.atlas.model.instance.EntityMutationResponse;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.atlas.repository.store.graph.v2.AtlasEntityStream;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.testng.Assert;
import org.testng.annotations.*;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.apache.atlas.model.discovery.SearchParameters.*;
import static org.testng.Assert.*;
import static org.testng.Assert.assertNotNull;

@Guice(modules = TestModules.TestOnlyModule.class)
public class AtlasDiscoveryServiceTest extends BasicTestSetup {

    @Inject
    private AtlasDiscoveryService discoveryService;

    String salesFactGuid = null;

    @BeforeClass
    public void setup() throws Exception {
        super.initialize();

        ApplicationProperties.get().setProperty(ApplicationProperties.ENABLE_FREETEXT_SEARCH_CONF, true);
        setupTestData();
        createDimensionalTaggedEntity("sales");
        createSpecialCharTestEntities();
        setupRelationshipTestData();
    }

    /*  TermSearchProcessor(TSP),
        FreeTextSearchProcessor(FSP),
        ClassificationSearchProcessor(CSP),
        EntitySearchProcessor(ESP)  */

    @Test
    public void term() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTermName(SALES_TERM+"@"+SALES_GLOSSARY);

        assertSearchProcessorWithoutMarker(params, 10);
    }

    // TSP execute and CSP,ESP filter
    @Test
    public void termTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTermName(SALES_TERM+"@"+SALES_GLOSSARY);
        params.setClassification(METRIC_CLASSIFICATION);

        assertSearchProcessorWithoutMarker(params, 4);
    }

    @Test
    public void termEntity() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTermName(SALES_TERM+"@"+SALES_GLOSSARY);
        params.setTypeName(HIVE_TABLE_TYPE);

        assertSearchProcessorWithoutMarker(params, 10);
    }

    @Test
    public void termEntityTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTermName(SALES_TERM+"@"+SALES_GLOSSARY);
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setClassification(DIMENSIONAL_CLASSIFICATION);

        List<AtlasEntityHeader> entityHeaders = discoveryService.searchWithParameters(params).getEntities();
        Assert.assertTrue(CollectionUtils.isEmpty(entityHeaders));
    }

    //FSP execute and CSP,ESP filter
    @Test
    public void queryALLTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(ALL_CLASSIFICATION_TYPES);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 5);
    }

    @Test
    public void queryALLTagTagFilter() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(ALL_CLASSIFICATION_TYPES);
        //typeName will check for only classification name not propogated classification
        SearchParameters.FilterCriteria fc = getSingleFilterCondition("__typeName", Operator.NOT_CONTAINS, METRIC_CLASSIFICATION);
        params.setTagFilters(fc);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 4);
    }

    @Test
    public void queryNOTCLASSIFIEDTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(NO_CLASSIFICATIONS);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 11);
    }


    @Test
    public void queryALLWildcardTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification("*");
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 5);
    }

    @Test
    public void queryWildcardTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification("Dimen*on");
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 2);
    }

    @Test
    public void queryTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(METRIC_CLASSIFICATION);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 3);
    }

    @Test
    public void queryTagTagFilter() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setClassification(METRIC_CLASSIFICATION);
        SearchParameters.FilterCriteria fc = getSingleFilterCondition("__timestamp", SearchParameters.Operator.LT, String.valueOf(System.currentTimeMillis()));
        params.setTagFilters(fc);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 3);
    }

    @Test
    public void queryEntity() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 4);
    }

    @Test
    public void queryEntityEntityFilter() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria fc = getSingleFilterCondition("tableType", Operator.NOT_NULL, "null");
        params.setEntityFilters(fc);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 3);
    }

    @Test
    public void queryEntityEntityFilterTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria fc = getSingleFilterCondition("tableType", Operator.IS_NULL, "null");
        params.setEntityFilters(fc);
        params.setClassification(DIMENSIONAL_CLASSIFICATION);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 1);
    }

    @Test
    public void queryEntityEntityFilterTagTagFilter() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria fcE = getSingleFilterCondition("tableType", Operator.IS_NULL, "null");
        params.setEntityFilters(fcE);
        params.setClassification(DIMENSIONAL_CLASSIFICATION);
        params.setQuery("sales");
        SearchParameters.FilterCriteria fcC = getSingleFilterCondition("attr1", Operator.EQ, "value1");
        params.setTagFilters(fcC);

        assertSearchProcessorWithoutMarker(params, 1);
    }

    @Test
    public void queryEntityTagTagFilter() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setClassification(METRIC_CLASSIFICATION);
        SearchParameters.FilterCriteria fc = getSingleFilterCondition("__timestamp", SearchParameters.Operator.LT, String.valueOf(System.currentTimeMillis()));
        params.setTagFilters(fc);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 2);
    }

    @Test
    public void queryEntityTag() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setClassification(METRIC_CLASSIFICATION);
        params.setQuery("sales");

        assertSearchProcessorWithoutMarker(params, 2);
    }

    // CSP Execute and ESP filter
    @Test
    public void entityEntityFilterTagTagFilter() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria fcE = getSingleFilterCondition("tableType", Operator.EQ, "Managed");
        params.setEntityFilters(fcE);
        params.setClassification(METRIC_CLASSIFICATION);
        SearchParameters.FilterCriteria fcC = getSingleFilterCondition("__timestamp", SearchParameters.Operator.LT, String.valueOf(System.currentTimeMillis()));
        params.setTagFilters(fcC);

        assertSearchProcessorWithoutMarker(params, 4);
    }

    @Test
    public void entityTagTagFilter() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setClassification(METRIC_CLASSIFICATION);
        SearchParameters.FilterCriteria fc = getSingleFilterCondition("__timestamp", SearchParameters.Operator.LT, String.valueOf(System.currentTimeMillis()));
        params.setTagFilters(fc);

        assertSearchProcessorWithoutMarker(params, 4);
    }

    @Test
    public void searchWith0offsetMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setOffset(0);
        params.setMarker(SearchContext.MarkerUtil.MARKER_START);
        params.setLimit(5);

        assertSearchProcessorWithMarker(params, 5);
    }

    @Test
    public void searchWithNoOffsetMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setMarker(SearchContext.MarkerUtil.MARKER_START);
        params.setLimit(5);

        assertSearchProcessorWithMarker(params, 5);
    }

    @Test
    public void searchWithGreaterThan0OffsetBlankMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setOffset(1);
        params.setMarker("");
        params.setLimit(5);

        assertSearchProcessorWithoutMarker(params, 5);
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "Marker can be used only if offset=0.")
    public void searchWithGreaterThan0OffsetMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setOffset(1);
        params.setMarker(SearchContext.MarkerUtil.MARKER_START);
        params.setLimit(5);
        List<AtlasEntityHeader> entityHeaders = discoveryService.searchWithParameters(params).getEntities();

        assertNotNull(entityHeaders);
    }

    @Test
    public void searchWithMarkerSet() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setMarker(SearchContext.MarkerUtil.MARKER_START);
        params.setLimit(5);
        AtlasSearchResult searchResult        = discoveryService.searchWithParameters(params);
        List<AtlasEntityHeader> entityHeaders = searchResult.getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(entityHeaders));
        assertEquals(entityHeaders.size(), 5);
        Assert.assertTrue(StringUtils.isNotEmpty(searchResult.getNextMarker()));

        //get next marker and set in marker of subsequent request
        params.setMarker(searchResult.getNextMarker());
        AtlasSearchResult nextsearchResult        = discoveryService.searchWithParameters(params);
        List<AtlasEntityHeader> nextentityHeaders = nextsearchResult.getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(nextentityHeaders));
        Assert.assertTrue(StringUtils.isNotEmpty(nextsearchResult.getNextMarker()));

        if (entityHeaders.size() < params.getLimit()) {
            Assert.assertTrue(nextsearchResult.getNextMarker() == String.valueOf(-1));
        }
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "Invalid marker!")
    public void searchWithInvalidMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setMarker(SearchContext.MarkerUtil.MARKER_START);
        params.setLimit(5);
        AtlasSearchResult searchResult        = discoveryService.searchWithParameters(params);
        List<AtlasEntityHeader> entityHeaders = searchResult.getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(entityHeaders));
        assertEquals(entityHeaders.size(), 5);
        Assert.assertTrue(StringUtils.isNotEmpty(searchResult.getNextMarker()));

        //get next marker and set in marker of subsequent request
        params.setMarker(searchResult.getNextMarker()+"abc");
        AtlasSearchResult nextsearchResult = discoveryService.searchWithParameters(params);

    }

    @Test
    public void searchWithLastPageMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setExcludeDeletedEntities(true);
        params.setMarker(SearchContext.MarkerUtil.MARKER_START);
        params.setLimit(5);
        AtlasSearchResult searchResult        = discoveryService.searchWithParameters(params);
        List<AtlasEntityHeader> entityHeaders = searchResult.getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(entityHeaders));
        assertEquals(entityHeaders.size(), 5);
        Assert.assertTrue(StringUtils.isNotEmpty(searchResult.getNextMarker()));

        long maxEntities = searchResult.getApproximateCount();

        //get next marker and set in marker of subsequent request
        params.setMarker(SearchContext.MarkerUtil.MARKER_START);
        params.setLimit((int)maxEntities + 10);
        AtlasSearchResult nextsearchResult = discoveryService.searchWithParameters(params);

        Assert.assertTrue(nextsearchResult.getNextMarker().equals("-1"));
    }


    @Test //marker functionality is not supported
    public void termMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTermName(SALES_TERM+"@"+SALES_GLOSSARY);
        params.setMarker("*");

        assertSearchProcessorWithoutMarker(params, 10);

    }

    @Test
    public void queryEntityTagMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setClassification(METRIC_CLASSIFICATION);
        params.setQuery("sales");
        params.setMarker("*");
        params.setLimit(5);

        assertSearchProcessorWithMarker(params, 2);
    }

    // CSP Execute and ESP filter
    @Test
    public void entityEntityFilterTagTagFilterMarker() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria fcE = getSingleFilterCondition("tableType", Operator.EQ, "Managed");
        params.setEntityFilters(fcE);
        params.setClassification(METRIC_CLASSIFICATION);
        SearchParameters.FilterCriteria fcC = getSingleFilterCondition("__timestamp", SearchParameters.Operator.LT, String.valueOf(System.currentTimeMillis()));
        params.setTagFilters(fcC);
        params.setMarker("*");
        assertSearchProcessorWithoutMarker(params, 4);
    }


    String spChar1   = "default.test_dot_name";
    String spChar2   = "default.test_dot_name@db.test_db";
    String spChar3   = "default.test_dot_name_12.col1@db1";
    String spChar4   = "default_.test_dot_name";

    String spChar5   = "default.test_colon_name:test_db";
    String spChar6   = "default.test_colon_name:-test_db";
    String spChar7   = "crn:def:default:name-76778-87e7-23@test";
    String spChar8   = "default.:test_db_name";

    String spChar9   = "default.customer's_name";
    String spChar10  = "default.customers'_data_name";

    String spChar11  = "search_with space@name";
    String spChar12  = "search_with space 123@name";

    //SearchProcessor.isIndexQuerySpecialChar
    String spChar13  = "search_with_special-char#having$and%inthename=attr";
    String spChar14  = "search_with_specialChar!name";
    String spChar15  = "search_with_star*in_name";
    String spChar16  = "search_with_star5.*5_inname";
    String spChar17  = "search_quest?n_name";

    String spChar18  = "/warehouse/tablespace/external/hive/name/hortonia_bank";
    String spChar19  = "/warehouse/tablespace/external/hive/name/exis_bank";

    String spChar20  = "search_name_with nameblank@namecluster";

    @DataProvider(name = "specialCharSearchContains")
    private Object[][] specialCharSearchContains() {
        return new Object[][]{
                {"name",Operator.CONTAINS,"test_dot",4},
                {"name",Operator.CONTAINS,"test_dot_name_",1},
                {"name",Operator.CONTAINS,"test_colon_name",2},
                {"name",Operator.CONTAINS,"def:default:name",1},
                {"name",Operator.CONTAINS,"space 12",1},
                {"name",Operator.CONTAINS,"with space",2},
                {"name",Operator.CONTAINS,"Char!name",1},
                {"name",Operator.CONTAINS,"with_star",2},
                {"name",Operator.CONTAINS,"/external/hive/name/",2},

                {"name",Operator.CONTAINS,"test_dot_name@db",1},
                {"name",Operator.CONTAINS,"name@db",1},
                {"name",Operator.CONTAINS,"def:default:name-",1},
                {"name",Operator.CONTAINS,"star*in",1},
                {"name",Operator.CONTAINS,"Char!na",1},
                {"name",Operator.CONTAINS,"ith spac",2},
                {"name",Operator.CONTAINS,"778-87",1},

                {"qualifiedName",Operator.CONTAINS,"test_dot",4},
                {"qualifiedName",Operator.CONTAINS,"test_dot_qf_",1},
                {"qualifiedName",Operator.CONTAINS,"test_colon_qf",2},
                {"qualifiedName",Operator.CONTAINS,"def:default:qf",1},
                {"qualifiedName",Operator.CONTAINS,"space 12",1},
                {"qualifiedName",Operator.CONTAINS,"with space",2},
                {"qualifiedName",Operator.CONTAINS,"Char!qf",1},
                {"qualifiedName",Operator.CONTAINS,"with_star",2},
                {"qualifiedName",Operator.CONTAINS,"/external/hive/qf/",2},

                {"qualifiedName",Operator.CONTAINS,"test_dot_qf@db",1},
                {"qualifiedName",Operator.CONTAINS,"qf@db",1},
                {"qualifiedName",Operator.CONTAINS,"def:default:qf-",1},
                {"qualifiedName",Operator.CONTAINS,"star*in",1},
                {"qualifiedName",Operator.CONTAINS,"Char!q",1},
                {"qualifiedName",Operator.CONTAINS,"ith spac",2},
                {"qualifiedName",Operator.CONTAINS,"778-87",1},
        };
    }

    @DataProvider(name = "specialCharSearchName")
    private Object[][] specialCharSearchName() {
        return new Object[][]{

                {"name",Operator.STARTS_WITH,"default.test_dot_",3},

                {"name",Operator.STARTS_WITH,"default.test_dot_name@db.test",1},
                {"name",Operator.STARTS_WITH,"default.test_dot_name@db.",1},
                {"name",Operator.STARTS_WITH,"default.test_dot_name",3},
                {"name",Operator.ENDS_WITH,"test_db",3},

                {"name",Operator.STARTS_WITH,"default.test_dot_name_12.col1@db",1},
                {"name",Operator.STARTS_WITH,"default.test_dot_name_12.col1@",1},
                {"name",Operator.STARTS_WITH,"default.test_dot_name_12.col1",1},
                {"name",Operator.STARTS_WITH,"default.test_dot_name_12.col",1},
                {"name",Operator.STARTS_WITH,"default.test_dot_name_12.",1},
                {"name",Operator.STARTS_WITH,"default.test_dot_name_12",1},
                {"name",Operator.ENDS_WITH,"col1@db1",1},

                {"name",Operator.STARTS_WITH,"default_.test_dot",1},
                {"name",Operator.ENDS_WITH,"test_dot_name",2},

                {"name",Operator.STARTS_WITH,"default.test_colon_name:test_",1},

                {"name",Operator.STARTS_WITH,"default.test_colon_name:-test_",1},
                {"name",Operator.STARTS_WITH,"default.test_colon_name:-",1},
                {"name",Operator.STARTS_WITH,"default.test_colon",2},

                {"name",Operator.STARTS_WITH,"crn:def:default:name-76778-87e7-23@",1},
                {"name",Operator.STARTS_WITH,"crn:def:default:name-76778-87e7-",1},
                {"name",Operator.STARTS_WITH,"crn:def:default:",1},

                {"name",Operator.STARTS_WITH,"default.:test_db",1},
                {"name",Operator.ENDS_WITH,"test_db_name",1},

                {"name",Operator.STARTS_WITH,"default.customer's",1},
                {"name",Operator.ENDS_WITH,"mer's_name",1},

                {"name",Operator.STARTS_WITH,"default.customers'_data",1},
                {"name",Operator.ENDS_WITH,"customers'_data_name",1},

                {"name",Operator.STARTS_WITH,"search_with space",2},
                {"name",Operator.STARTS_WITH,"search_with space ",1},
                {"name",Operator.STARTS_WITH,"search_with space 123@",1},
                {"name",Operator.STARTS_WITH,"search_with space 1",1},

                {"name",Operator.STARTS_WITH,"search_with_special-char#having$and%inthename=",1},
                {"name",Operator.STARTS_WITH,"search_with_special-char#having$and%in",1},
                {"name",Operator.STARTS_WITH,"search_with_special-char#having$",1},
                {"name",Operator.STARTS_WITH,"search_with_special-char#h",1},
                {"name",Operator.STARTS_WITH,"search_with_special",2},
                {"name",Operator.STARTS_WITH,"search_with_spe",2},

                {"name",Operator.STARTS_WITH,"search_with_specialChar!",1},

                {"name",Operator.STARTS_WITH,"search_with_star*in",1},

                {"name",Operator.ENDS_WITH,"5.*5_inname",1},
                {"name",Operator.STARTS_WITH,"search_with_star5.*5_",1},

                {"name",Operator.STARTS_WITH,"search_quest?n_",1},

                {"name",Operator.STARTS_WITH,"/warehouse/tablespace/external/hive/name/hortonia",1},
                {"name",Operator.STARTS_WITH,"/warehouse/tablespace/external/hive/name/",2},

        };
    }

    @DataProvider(name = "specialCharSearchQFName")
    private Object[][] specialCharSearchQFName() {
        return new Object[][]{

                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_",3},

                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf@db.test",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf@db.",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf",3},
                {"qualifiedName",Operator.ENDS_WITH,"test_db",3},

                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf_12.col1@db",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf_12.col1@",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf_12.col1",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf_12.col",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf_12.",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_dot_qf_12",1},
                {"qualifiedName",Operator.ENDS_WITH,"col1@db1",1},

                {"qualifiedName",Operator.STARTS_WITH,"default_.test_dot",1},
                {"qualifiedName",Operator.ENDS_WITH,"test_dot_qf",2},

                {"qualifiedName",Operator.STARTS_WITH,"default.test_colon_qf:test_",1},

                {"qualifiedName",Operator.STARTS_WITH,"default.test_colon_qf:-test_",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_colon_qf:-",1},
                {"qualifiedName",Operator.STARTS_WITH,"default.test_colon",2},

                {"qualifiedName",Operator.STARTS_WITH,"crn:def:default:qf-76778-87e7-23@",1},
                {"qualifiedName",Operator.STARTS_WITH,"crn:def:default:qf-76778-87e7-",1},
                {"qualifiedName",Operator.STARTS_WITH,"crn:def:default:",1},

                {"qualifiedName",Operator.STARTS_WITH,"default.:test_db",1},
                {"qualifiedName",Operator.ENDS_WITH,"test_db_qf",1},

                {"qualifiedName",Operator.STARTS_WITH,"default.customer's",1},
                {"qualifiedName",Operator.ENDS_WITH,"mer's_qf",1},

                {"qualifiedName",Operator.STARTS_WITH,"default.customers'_data",1},
                {"qualifiedName",Operator.ENDS_WITH,"customers'_data_qf",1},

                {"qualifiedName",Operator.STARTS_WITH,"search_with space",2},
                {"qualifiedName",Operator.STARTS_WITH,"search_with space ",1},
                {"qualifiedName",Operator.STARTS_WITH,"search_with space 123@",1},
                {"qualifiedName",Operator.STARTS_WITH,"search_with space 1",1},

                {"qualifiedName",Operator.STARTS_WITH,"search_with_special-char#having$and%intheqf=",1},
                {"qualifiedName",Operator.STARTS_WITH,"search_with_special-char#having$and%in",1},
                {"qualifiedName",Operator.STARTS_WITH,"search_with_special-char#having$",1},
                {"qualifiedName",Operator.STARTS_WITH,"search_with_special-char#h",1},
                {"qualifiedName",Operator.STARTS_WITH,"search_with_special",2},
                {"qualifiedName",Operator.STARTS_WITH,"search_with_spe",2},

                {"qualifiedName",Operator.STARTS_WITH,"search_with_specialChar!",1},

                {"qualifiedName",Operator.STARTS_WITH,"search_with_star*in",1},

                {"qualifiedName",Operator.ENDS_WITH,"5.*5_inqf",1},
                {"qualifiedName",Operator.STARTS_WITH,"search_with_star5.*5_",1},

                {"qualifiedName",Operator.STARTS_WITH,"search_quest?n_",1},

                {"qualifiedName",Operator.STARTS_WITH,"/warehouse/tablespace/external/hive/qf/hortonia",1},
                {"qualifiedName",Operator.STARTS_WITH,"/warehouse/tablespace/external/hive/qf/",2},

        };
    }


    @DataProvider(name = "specialCharSearchEQ")
    private Object[][] specialCharSearch() {
        return new Object[][]{
                {"name",Operator.EQ,spChar1,1},
                {"name",Operator.EQ,spChar2,1},
                {"name",Operator.EQ,spChar3,1},
                {"name",Operator.EQ,spChar4,1},
                {"name",Operator.EQ,spChar5,1},
                {"name",Operator.EQ,spChar6,1},
                {"name",Operator.EQ,spChar7,1},
                {"name",Operator.EQ,spChar8,1},
                {"name",Operator.EQ,spChar9,1},
                {"name",Operator.EQ,spChar10,1},
                {"name",Operator.EQ,spChar11,1},
                {"name",Operator.EQ,spChar12,1},
                {"name",Operator.EQ,spChar13,1},
                {"name",Operator.EQ,spChar14,1},
                {"name",Operator.EQ,spChar15,1},
                {"name",Operator.EQ,spChar16,1},
                {"name",Operator.EQ,spChar17,1},
                {"name",Operator.EQ,spChar18,1},
                {"name",Operator.EQ,spChar19,1},

                {"qualifiedName",Operator.EQ,spChar1.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar2.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar3.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar4.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar5.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar6.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar7.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar8.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar9.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar10.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar11.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar12.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar13.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar14.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar15.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar16.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar17.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar18.replace("name","qf"),1},
                {"qualifiedName",Operator.EQ,spChar19.replace("name","qf"),1},
        };
    }

    @DataProvider(name = "specialCharQuickSearch")
    private Object[][] specialCharQuickSearch() {
        return new Object[][]{
                {"default.test_dot_name", 3},
                {"default.test_dot_name*", 3},
                {"test_dot_name", 0},
                {"*test_dot_name", 2},
                {"*test_dot_name*", 4},
                {"default.test_dot_name\\*", 0},

                {"default.test_dot_qf", 3},
                {"default.test_dot_qf*", 3},
                {"test_dot_qf", 1},
                {"*test_dot_qf", 3},
                {"*test_dot_qf*", 4},
                {"default.test_dot_qf\\*", 2},

                {"default.test_dot_name_12.col1", 1},
                {"default.test_dot_name_12.col1*", 1},
                {"default.test_dot_name_12.col", 0},
                {"default.test_dot_name_12.col*", 1},
                {"default.test_dot_qf_12.col1", 1},
                {"default.test_dot_qf_12.col1*", 0},

                {"default.test_dot_name_12*", 1},
                {"default.test_dot_qf_12*", 1},

                //space gets tokenized, hence whenever user searches for 'STRING' type attribute, space needs to be escaped
                {"search_name_with nameblank@namecluster", 0},
                //{"search_name_with nameblank@name", 0}, commenting as there are some entities created from other class with same name
                {"search_name_with nameblank*", 0},
                {"search_name_with\\ nameblank@namecluster", 1},
                {"search_name_with\\ nameblank*", 1},

                //if we escape it will consider as single string, hence * will act as character not as wildcard
                {"search_qf_with\\ qfblank*", 0},
                {"search_qf_with\\ qfblank@qfcluster", 1},
                {"search_qf_with qfblank@qfcluster", 1},
                {"search_qf_with qfblank@q", 1},
                {"search_qf_with qfblank*", 1},
        };
    }


    public void createSpecialCharTestEntities() throws AtlasBaseException {

        List<String> nameList = Arrays.asList(spChar1,spChar2,spChar3,spChar4,spChar5,spChar6,spChar7,spChar8,spChar9,spChar10,spChar11,spChar12,spChar13,spChar14,spChar15,spChar16,spChar17,spChar18,spChar19,spChar20);
        for (String nameStr : nameList) {
            AtlasEntity entityToDelete = new AtlasEntity(HIVE_TABLE_TYPE);
            entityToDelete.setAttribute("name", nameStr);
            entityToDelete.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, "qualifiedName"+System.currentTimeMillis());

            //create entity
            EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(new AtlasEntity.AtlasEntitiesWithExtInfo(entityToDelete)), false);
        }

        List<String> qfList = nameList;

        for (String qfStr : qfList) {
            qfStr = qfStr.replace("name","qf");
            AtlasEntity entityToDelete = new AtlasEntity(HIVE_TABLE_TYPE);
            entityToDelete.setAttribute("name", "name"+System.currentTimeMillis());
            entityToDelete.setAttribute(AtlasClient.REFERENCEABLE_ATTRIBUTE_NAME, qfStr);

            //create entity
            EntityMutationResponse response = entityStore.createOrUpdate(new AtlasEntityStream(new AtlasEntity.AtlasEntitiesWithExtInfo(entityToDelete)), false);

        }
    }

    @Test(dataProvider = "specialCharSearchEQ")
    public void specialCharSearchAssertEq(String attrName, SearchParameters.Operator operator, String attrValue, int expected) throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition(attrName,operator, attrValue);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

       AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        assertSearchResult(searchResult,expected, attrValue);
    }

    @Test(dataProvider = "specialCharSearchContains")
    public void specialCharSearchAssertContains(String attrName, SearchParameters.Operator operator, String attrValue, int expected) throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition(attrName,operator, attrValue);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        assertSearchResult(searchResult,expected, attrValue);
    }

    @Test(dataProvider = "specialCharSearchContains")
    public void specialCharQuickSearchAssertContains(String attrName, SearchParameters.Operator operator, String attrValue, int expected) throws AtlasBaseException {
        QuickSearchParameters params = new QuickSearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition(attrName,operator, attrValue);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        AtlasQuickSearchResult searchResult = discoveryService.quickSearch(params);
        assertSearchResult(searchResult.getSearchResults(), expected, attrValue);

        List<String> failedCases = Arrays.asList("def:default:qf-","star*in","ith spac","778-87");
        if (attrName.equals("qualifiedName") && failedCases.contains(attrValue)) {
            expected = 0;
        }

        if (expected > 0)  {
            assertAggregationMetrics(searchResult);
        }
    }

    @Test(dataProvider = "specialCharSearchName")
    public void specialCharSearchAssertName(String attrName, SearchParameters.Operator operator, String attrValue, int expected) throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition(attrName,operator, attrValue);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        assertSearchResult(searchResult,expected, attrValue);
    }

    @Test(dataProvider = "specialCharSearchName")
    public void specialCharQuickSearchAssertName(String attrName, SearchParameters.Operator operator, String attrValue, int expected) throws AtlasBaseException {
        QuickSearchParameters params = new QuickSearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition(attrName,operator, attrValue);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        AtlasQuickSearchResult searchResult = discoveryService.quickSearch(params);
        assertSearchResult(searchResult.getSearchResults(), expected, attrValue);
        if (expected > 0) {
            assertAggregationMetrics(searchResult);
        }
    }

    @Test(dataProvider = "specialCharSearchQFName")
    public void specialCharSearchAssertQFName(String attrName, SearchParameters.Operator operator, String attrValue, int expected) throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition(attrName,operator, attrValue);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        assertSearchResult(searchResult, expected, attrValue);
    }

    @Test(dataProvider = "specialCharSearchQFName")
    public void specialCharQuickSearchAssertQFName(String attrName, SearchParameters.Operator operator, String attrValue, int expected) throws AtlasBaseException {
        QuickSearchParameters params = new QuickSearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition(attrName,operator, attrValue);
        params.setEntityFilters(filterCriteria);
        params.setLimit(20);

        AtlasQuickSearchResult searchResult = discoveryService.quickSearch(params);
        assertSearchResult(searchResult.getSearchResults(), expected, attrValue);
        if (expected > 0) {
            assertAggregationMetrics(searchResult);
        }
    }

    @Test(dataProvider = "specialCharQuickSearch")
    public void specialCharQuickSearch(String searchValue, int expected) throws AtlasBaseException {
        QuickSearchParameters params = new QuickSearchParameters();
        params.setQuery(searchValue);
        params.setLimit(5);
        params.setOffset(0);

        AtlasQuickSearchResult searchResult = discoveryService.quickSearch(params);
        assertSearchResult(searchResult.getSearchResults(), expected, searchValue);
        if (expected > 0) {
            assertAggregationMetrics(searchResult);
        }
    }

    @Test
    public void searchWithEntityQuickSearchSortAsc() throws AtlasBaseException {
        QuickSearchParameters params = new QuickSearchParameters();
        params.setTypeName("hive_table");
        params.setQuery("sales");
        params.setExcludeDeletedEntities(true);
        params.setLimit(3);
        params.setSortBy("owner");
        params.setSortOrder(SortOrder.ASCENDING);

        AtlasQuickSearchResult searchResult = discoveryService.quickSearch(params);
        List<AtlasEntityHeader> list = searchResult.getSearchResults().getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(list));
        Assert.assertTrue(list.size() == 3);
        Assert.assertTrue(list.get(0).getAttribute("owner").toString().equalsIgnoreCase("Jane BI"));
        Assert.assertTrue(list.get(1).getAttribute("owner").toString().equalsIgnoreCase("Joe"));
    }

    @Test
    public void searchWithEntityQuickSearchSortDesc() throws AtlasBaseException {
        QuickSearchParameters params = new QuickSearchParameters();
        params.setTypeName("hive_table");
        params.setQuery("sales");
        params.setExcludeDeletedEntities(true);
        params.setLimit(3);
        params.setSortBy("name");
        params.setSortOrder(SortOrder.DESCENDING);

        AtlasQuickSearchResult searchResult = discoveryService.quickSearch(params);
        List<AtlasEntityHeader> list = searchResult.getSearchResults().getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(list));
        Assert.assertTrue(list.size() == 3);
        Assert.assertTrue(list.get(0).getDisplayText().equalsIgnoreCase("time_dim"));
        Assert.assertTrue(list.get(1).getDisplayText().equalsIgnoreCase("sales_fact_monthly_mv"));
    }

    @Test
    public void searchRelatedEntitiesSortAsc() throws AtlasBaseException {
        String guid = gethiveTableSalesFactGuid();

        SearchParameters params = new SearchParameters();
        params.setLimit(10);
        AtlasSearchResult relResult  = discoveryService.searchRelatedEntities(guid, "__hive_table.columns", false, params);
        List<AtlasEntityHeader> list = relResult.getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(list));
        Assert.assertTrue(list.size() == 6);
        Assert.assertTrue(list.get(0).getDisplayText().equalsIgnoreCase("customer_id"));
        Assert.assertTrue(list.get(5).getDisplayText().equalsIgnoreCase("time_id"));

    }

    @Test
    public void searchRelatedEntitiesSortDesc() throws AtlasBaseException {
        String guid = gethiveTableSalesFactGuid();

        SearchParameters params = new SearchParameters();
        params.setLimit(10);
        params.setSortOrder(SortOrder.DESCENDING);

        AtlasSearchResult relResult  = discoveryService.searchRelatedEntities(guid, "columns", false, params);
        List<AtlasEntityHeader> list = relResult.getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(list));
        Assert.assertTrue(list.size() == 6);
        Assert.assertTrue(list.get(5).getDisplayText().equalsIgnoreCase("customer_id"));
        Assert.assertTrue(list.get(0).getDisplayText().equalsIgnoreCase("time_id"));
    }

    //test excludeHeaderAttributes
    @Test
    public void excludeHeaderAttributesStringAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setExcludeHeaderAttributes(true);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("tableType", Operator.EQ, "Managed");
        params.setEntityFilters(filterCriteria);
        params.setSortBy("name");
        params.setAttributes(new HashSet<String>() {{ add("name");}});
        params.setLimit(1);

        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        AtlasSearchResult.AttributeSearchResult expected = new AtlasSearchResult.AttributeSearchResult();
        expected.setName(Arrays.asList("name"));
        expected.setValues(Arrays.asList(Arrays.asList("log_fact_daily_mv")));
        assertSearchResult(searchResult,expected);
    }

    @Test
    public void excludeHeaderAttributesRelationAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setExcludeHeaderAttributes(true);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("name", Operator.EQ, "time_dim");
        params.setEntityFilters(filterCriteria);
        params.setAttributes(new HashSet<String>() {{ add("name"); add("db");}});
        params.setLimit(1);

        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);

        assertNotNull(searchResult);
        assertNotNull(searchResult.getEntities());
        assertNotNull(searchResult.getReferredEntities());
    }

    @Test
    public void excludeHeaderAttributesSystemAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setExcludeHeaderAttributes(true);
        params.setAttributes(new HashSet<String>() {{ add("name"); add("__state");}});
        params.setLimit(1);
        SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("tableType", Operator.EQ, "Managed");
        params.setEntityFilters(filterCriteria);
        params.setSortBy("name");

        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        AtlasSearchResult.AttributeSearchResult expected = new AtlasSearchResult.AttributeSearchResult();
        expected.setName(Arrays.asList("name","__state"));
        expected.setValues(Arrays.asList(Arrays.asList("log_fact_daily_mv","ACTIVE")));
        assertSearchResult(searchResult,expected);
    }

    @Test
    public void excludeHeaderAttributesAllEntityTypeSysAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE+","+ALL_ENTITY_TYPES);
        params.setExcludeHeaderAttributes(true);
        params.setAttributes(new HashSet<String>() {{ add("__state");}});
        params.setLimit(2);

        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        AtlasSearchResult.AttributeSearchResult expected = new AtlasSearchResult.AttributeSearchResult();
        expected.setName(Arrays.asList("__state"));
        expected.setValues(Arrays.asList(Arrays.asList("ACTIVE"), Arrays.asList("ACTIVE")));
        assertSearchResult(searchResult,expected);
    }
    @Test
    public void excludeHeaderAttributesAllEntityTypeSysAttrs() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE+","+ALL_ENTITY_TYPES);
        params.setExcludeHeaderAttributes(true);
        params.setAttributes(new HashSet<String>() {{ add("__state"); add("__guid");}});
        params.setLimit(2);

        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        assertEquals(searchResult.getAttributes().getValues().size(), 2);
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "Attribute name not found for type __ENTITY_ROOT")
    public void excludeHeaderAttributesAllEntityType() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE+","+ALL_ENTITY_TYPES);
        params.setExcludeHeaderAttributes(true);
        params.setAttributes(new HashSet<String>() {{ add("name");}});
        params.setLimit(1);

        discoveryService.searchWithParameters(params);
    }

    @Test(expectedExceptions = AtlasBaseException.class, expectedExceptionsMessageRegExp = "Attribute name1 not found for type hive_table")
    public void excludeHeaderAttributesInvalidAttr() throws AtlasBaseException {
        SearchParameters params = new SearchParameters();
        params.setTypeName(HIVE_TABLE_TYPE);
        params.setExcludeHeaderAttributes(true);
        params.setAttributes(new HashSet<String>() {{ add("name1");}});
        params.setLimit(1);

        discoveryService.searchWithParameters(params);
    }

    @Test
    public void searchRelations() throws AtlasBaseException {
        RelationshipSearchParameters sp = new RelationshipSearchParameters();
        sp.setRelationshipName("highlight_post");
        sp.setMarker("*");
        sp.setLimit(10);
        sp.setRelationshipFilters(getSingleFilterCondition("highlight_name", Operator.EQ, "year2021@Ajay"));

        AtlasSearchResult sr = discoveryService.searchRelationsWithParameters(sp);
        assertEquals(sr.getRelations().size(), 4);
    }

    private String gethiveTableSalesFactGuid() throws AtlasBaseException {
        if (salesFactGuid == null) {
            SearchParameters params = new SearchParameters();
            params.setTypeName(HIVE_TABLE_TYPE);
            SearchParameters.FilterCriteria filterCriteria = getSingleFilterCondition("name",Operator.EQ, "sales_fact");
            params.setEntityFilters(filterCriteria);
            AtlasSearchResult result = discoveryService.searchWithParameters(params);

            if (result != null && CollectionUtils.isNotEmpty(result.getEntities())) {
                salesFactGuid = result.getEntities().get(0).getGuid();
            }
        }
        return salesFactGuid;
    }

    private void assertSearchResult(AtlasSearchResult searchResult, int expected, String query) {
        assertNotNull(searchResult);
        if(expected == 0) {
            assertTrue(searchResult.getAttributes() == null || CollectionUtils.isEmpty(searchResult.getAttributes().getValues()));
            assertNull(searchResult.getEntities(), query);
        } else if(searchResult.getEntities() != null) {
            assertEquals(searchResult.getEntities().size(), expected, query);
        } else {
            assertNotNull(searchResult.getAttributes());
            assertNotNull(searchResult.getAttributes().getValues());
            assertEquals(searchResult.getAttributes().getValues().size(), expected, query);
        }
    }

    private void assertSearchResult(AtlasSearchResult searchResult, AtlasSearchResult.AttributeSearchResult expected) {
        assertNotNull(searchResult);
        AtlasSearchResult.AttributeSearchResult result = searchResult.getAttributes();
        assertNotNull(result);
        assertTrue(result.getName().containsAll(expected.getName()));
        int i = 0;
        for (List<Object> value : result.getValues()) {
            assertTrue(value.containsAll(expected.getValues().get(i)));
            i++;
        }
    }

    private void assertAggregationMetrics(AtlasQuickSearchResult searchResult) {
        Map<String, List<AtlasAggregationEntry>> agg =  searchResult.getAggregationMetrics();
        Assert.assertTrue(CollectionUtils.isNotEmpty(agg.get("__typeName")));

        AtlasAggregationEntry entry = agg.get("__typeName").get(0);
        Assert.assertTrue(entry!=null && entry.getCount() > 0);
    }

    private void createDimensionalTaggedEntity(String name) throws AtlasBaseException {
        EntityMutationResponse resp = createDummyEntity(name, HIVE_TABLE_TYPE);
        AtlasEntityHeader entityHeader = resp.getCreatedEntities().get(0);
        String guid = entityHeader.getGuid();
        HashMap<String,Object> attr = new HashMap<>();
        attr.put("attr1","value1");
        entityStore.addClassification(Arrays.asList(guid), new AtlasClassification(DIMENSIONAL_CLASSIFICATION, attr));
    }


    private void assertSearchProcessorWithoutMarker(SearchParameters params, int expected) throws AtlasBaseException {
        assertSearchProcessor(params, expected, false);
    }

    private void assertSearchProcessorWithMarker(SearchParameters params, int expected) throws AtlasBaseException {
        assertSearchProcessor(params, expected, true);
    }

    private void assertSearchProcessor(SearchParameters params, int expected, boolean checkMarker) throws AtlasBaseException {
        AtlasSearchResult searchResult = discoveryService.searchWithParameters(params);
        List<AtlasEntityHeader> entityHeaders = searchResult.getEntities();

        Assert.assertTrue(CollectionUtils.isNotEmpty(entityHeaders));
        assertEquals(entityHeaders.size(), expected);

        if (checkMarker) {
            Assert.assertTrue(StringUtils.isNotEmpty(searchResult.getNextMarker()));
        } else {
            Assert.assertTrue(StringUtils.isEmpty(searchResult.getNextMarker()));
        }
    }

    @AfterClass
    public void teardown() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }
}
