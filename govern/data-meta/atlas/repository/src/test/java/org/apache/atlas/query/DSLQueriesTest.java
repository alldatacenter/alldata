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
package org.apache.atlas.query;

import org.apache.atlas.BasicTestSetup;
import org.apache.atlas.TestModules;
import org.apache.atlas.discovery.EntityDiscoveryService;
import org.apache.atlas.exception.AtlasBaseException;
import org.apache.atlas.model.discovery.AtlasSearchResult;
import org.apache.atlas.model.instance.AtlasEntityHeader;
import org.apache.atlas.repository.graph.AtlasGraphProvider;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.SkipException;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Guice;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Guice(modules = TestModules.TestOnlyModule.class)
public class DSLQueriesTest extends BasicTestSetup {
    private static final Logger LOG = LoggerFactory.getLogger(DSLQueriesTest.class);

    private final int DEFAULT_LIMIT = 25;
    private final int DEFAULT_OFFSET = 0;

    @Inject
    private EntityDiscoveryService discoveryService;

    @BeforeClass
    public void setup() throws Exception {
        super.initialize();

        setupTestData();

        pollForData();
    }

    private void pollForData() throws InterruptedException {
        Object[][] basicVerificationQueries = new Object[][] {
                {"hive_db", 3},
                {"hive_process", 7},
                {"hive_table", 10},
                {"hive_column", 17},
                {"hive_storagedesc", 1},
                {"Manager", 2},
                {"Employee", 4},
        };

        int     pollingAttempts = 5;
        int     pollingBackoff  = 0; // in msecs
        boolean success;

        for (int attempt = 0; attempt < pollingAttempts; attempt++, pollingBackoff += attempt * 5000) {
            LOG.debug("Polling -- Attempt {}, Backoff {}", attempt, pollingBackoff);

            success = false;

            for (Object[] verificationQuery : basicVerificationQueries) {
                String query    = (String) verificationQuery[0];
                int    expected = (int) verificationQuery[1];

                try {
                    AtlasSearchResult result = discoveryService.searchUsingDslQuery(query, 25, 0);

                    if (result.getEntities() == null || result.getEntities().isEmpty()) {
                        LOG.warn("DSL {} returned no entities", query);

                        success = false;
                    } else if (result.getEntities().size() != expected) {
                        LOG.warn("DSL {} returned unexpected number of entities. Expected {} Actual {}", query, expected, result.getEntities().size());

                        success = false;
                    } else {
                        success = true;
                    }
                } catch (AtlasBaseException e) {
                    LOG.error("Got exception for DSL {}, errorCode: {}", query, e.getAtlasErrorCode());

                    waitOrBailout(pollingAttempts, pollingBackoff, attempt);
                }
            }

            // DSL queries were successful
            if (success) {
                LOG.info("Polling was success");

                break;
            } else {
                waitOrBailout(pollingAttempts, pollingBackoff, attempt);
            }
        }
    }

    private void waitOrBailout(final int pollingAttempts, final int pollingBackoff, final int attempt) throws InterruptedException {
        if (attempt == pollingAttempts - 1) {
            LOG.error("Polling failed after {} attempts", pollingAttempts);

            throw new SkipException("Polling for test data was unsuccessful");
        } else {
            LOG.warn("Waiting for {} before polling again", pollingBackoff);

            Thread.sleep(pollingBackoff);
        }
    }

    @AfterClass
    public void teardown() throws Exception {
        AtlasGraphProvider.cleanup();

        super.cleanup();
    }

    @DataProvider(name = "comparisonQueriesProvider")
    private Object[][] comparisonQueriesProvider() {
        return new Object[][] {
                {"Person where (name = \"Julius\" )", 1},
                {"Person where (name like \"Jul*\" )", 1},
                {"Person where (name like \"J*\" )", 3},
                {"Person where (name like \"*us\" )", 1},
                {"Person where (name like \"*uli*\" )", 1},
                {"Person where (name like \"Julius\" )", 1},
                {"Person where (name like \"Jul\" )", 0},

                {"Person where (birthday < \"1950-01-01T02:35:58.440Z\" )", 0},
                {"Person where (birthday > \"1975-01-01T02:35:58.440Z\" )", 2},
                {"Person where (birthday >= \"1975-01-01T02:35:58.440Z\" )", 2},
                {"Person where (birthday <= \"1950-01-01T02:35:58.440Z\" )", 0},
                {"Person where (birthday = \"1975-01-01T02:35:58.440Z\" )", 0},
                {"Person where (birthday != \"1975-01-01T02:35:58.440Z\" )", 4},

                {"Person where (hasPets = true)", 2},
                {"Person where (hasPets = false)", 2},
                {"Person where (hasPets != false)", 2},
                {"Person where (hasPets != true)", 2},

                {"Person where (numberOfCars > 0)", 2},
                {"Person where (numberOfCars > 1)", 1},
                {"Person where (numberOfCars >= 1)", 2},
                {"Person where (numberOfCars < 2)", 3},
                {"Person where (numberOfCars <= 2)", 4},
                {"Person where (numberOfCars = 2)", 1},
                {"Person where (numberOfCars != 2)", 3},

                {"Person where (houseNumber > 0)", 2},
                {"Person where (houseNumber > 17)", 1},
                {"Person where (houseNumber >= 17)", 2},
                {"Person where (houseNumber < 153)", 3},
                {"Person where (houseNumber <= 153)", 4},
                {"Person where (houseNumber =  17)", 1},
                {"Person where houseNumber >= 17 or numberOfCars = 2", 2},
                {"Person where (houseNumber != 17)", 3},

                {"Person where (carMileage > 0)", 2},
                {"Person where (carMileage > 13)", 1},
                {"Person where (carMileage >= 13)", 2},
                {"Person where (carMileage < 13364)", 3},
                {"Person where (carMileage <= 13364)", 4},
                {"Person where (carMileage =  13)", 1},
                {"Person where (carMileage != 13)", 3},

                {"Person where (age > 36)", 1},
                {"Person where (age > 49)", 1},
                {"Person where (age >= 49)", 1},
                {"Person where (age < 50)", 3},
                {"Person where (age <= 35)", 2},
                {"Person where (age =  35)", 0},
                {"Person where (age != 35)", 4},
                {String.format("Person where (age <= %f)", Float.MAX_VALUE), 4},
                {"Person where (approximationOfPi > -3.4028235e+38)", 4},
        };
    }

    @Test(dataProvider = "comparisonQueriesProvider")
    public void comparison(String query, int expected) throws AtlasBaseException {
        AtlasSearchResult searchResult = discoveryService.searchUsingDslQuery(query, DEFAULT_LIMIT, 0);

        assertSearchResult(searchResult, expected, query);

        AtlasSearchResult searchResult2 = discoveryService.searchUsingDslQuery(query.replace("where", " "), DEFAULT_LIMIT, 0);

        assertSearchResult(searchResult2, expected, query);
    }

    @DataProvider(name = "glossaryTermQueries")
    private Object[][] glossaryTermQueries() {
        return new Object[][]{
                {"hive_table hasTerm modernTrade", 2, new ListValidator("logging_fact_monthly_mv", "time_dim")},
                {"hive_table hasTerm \"modernTrade@salesGlossary\"", 2, new ListValidator("logging_fact_monthly_mv", "time_dim")},
                {"hive_table hasTerm \"modernTrade@salesGlossary\" where hive_table.name = \"time_dim\"", 1, new ListValidator("time_dim")},
                {"hive_table hasTerm \"modernTrade@salesGlossary\" select name", 2, null},
                {"hive_table hasTerm \"modernTrade@salesGlossary\" limit 1", 1, null},
                {"hive_table hasTerm \"modernTrade@salesGlossary\" or hive_table hasTerm \"ecommerce@salesGlossary\"", 3, new ListValidator("logging_fact_monthly_mv", "time_dim", "product_dim")},
                {"hive_table hasTerm \"modernTrade@salesGlossary\" and hive_table isA Dimension",1, new ListValidator( "time_dim")},
                {"hive_table hasTerm \"modernTrade@salesGlossary\" and db.name = \"Sales\" or (hive_table.name = \"sales_fact_monthly_mv\")", 2, new ListValidator("sales_fact_monthly_mv", "time_dim")},
                {"hive_table where hive_table hasTerm \"modernTrade@salesGlossary\"", 2, new ListValidator("logging_fact_monthly_mv", "time_dim")},
                {"hive_table where (name = \"product_dim\" and hive_table hasTerm \"ecommerce@salesGlossary\")", 1, new ListValidator("product_dim")},
                {"hive_table where (name = 'product_dim' and hive_table hasTerm 'ecommerce@salesGlossary')", 1, new ListValidator("product_dim")}
        };
    }

    @DataProvider(name = "classificationQueries")
    private Object[][] classificationQueries() {
        return new Object[][] {
                {"hive_table isA Dimension", 5, new ListValidator("product_dim", "time_dim", "customer_dim", "sales_fact_monthly_mv", "sales_fact_daily_mv")},
                {"hive_table where hive_table isA Dimension", 5,  new ListValidator("product_dim", "time_dim", "customer_dim", "sales_fact_monthly_mv", "sales_fact_daily_mv")},
                {"hive_table where name = 'time_dim' and hive_table isA Dimension", 1,  new ListValidator("time_dim")},
                {"Dimension where Dimension.timeAttr = 'timeValue'", 5,  new ListValidator("loadSalesMonthly", "loadSalesDaily", "time_dim", "sales_fact_monthly_mv", "sales_fact_daily_mv")},
                {"Dimension as d where d.productAttr = 'productValue'", 2, new ListValidator("product_dim", "product_dim_view")},
                {"hive_table where hive_table isA Dimension and Dimension.timeAttr = 'timeValue'", 3, new ListValidator("time_dim", "sales_fact_monthly_mv", "sales_fact_daily_mv")},
                {"hive_table where Dimension.timeAttr = 'timeValue'", 3, new ListValidator("time_dim", "sales_fact_monthly_mv", "sales_fact_daily_mv")},
                {"hive_table where (Dimension.timeAttr = 'timeValue')", 3, new ListValidator("time_dim", "sales_fact_monthly_mv", "sales_fact_daily_mv")},
                {"hive_table where (name = 'time_dim' and Dimension.timeAttr = 'timeValue')", 1, new ListValidator("time_dim")},
                {"hive_table hasTerm \"modernTrade@salesGlossary\" and Dimension.timeAttr = 'timeValue' and db.name = 'Sales'", 1, new ListValidator("time_dim")},
        };
    }

    @Test(dataProvider = "classificationQueries")
    public void classificationQueries(String query, int expected, ListValidator lvExpected) throws AtlasBaseException {
        AtlasSearchResult result = queryAssert(query, expected, DEFAULT_LIMIT, 0);

        if (lvExpected == null) {
            return;
        }

        ListValidator.assertLv(ListValidator.from(result), lvExpected);
    }

    @Test(dataProvider = "glossaryTermQueries")
    public void glossaryTerm(String query, int expected, ListValidator lvExpected) throws AtlasBaseException {
        AtlasSearchResult result = queryAssert(query, expected, DEFAULT_LIMIT, 0);

        if (lvExpected == null) {
            return;
        }

        ListValidator.assertLv(ListValidator.from(result), lvExpected);
    }

    @DataProvider(name = "basicProvider")
    private Object[][] basicQueries() {
        return new Object[][]{
                {"hive_column where table.name = \"sales_fact_daily_mv\"", 6},
                {"hive_table where columns.name = \"app_id\"", 2},
                {"from hive_db", 3},
                {"hive_db", 3},
                {"hive_db as d select d", 3},
                {"hive_db where hive_db.name=\"Reporting\"", 1},
                {"hive_db where hive_db.name=\"Reporting\" select name, owner", 1},
                {"hive_column where name='time_id' select name", 1},
                {"hive_db has name", 3},
                {"from hive_table", 10},
                {"hive_table", 10},
                {"hive_table isa Dimension", 5},
                {"hive_column where hive_column isa PII", 4},
                {"hive_column where hive_column isa PII select hive_column.qualifiedName", 4},
                {"hive_column select hive_column.qualifiedName", 21},
                {"hive_column select hive_column.qualifiedName, hive_column.description", 21},
                {"hive_column select qualifiedName", 21},
                {"hive_column select qualifiedName, description", 21},
                {"hive_column where hive_column.name=\"customer_id\"", 2},
                {"hive_column where hive_column.name=\"customer_id\" select qualifiedName, description", 2},
                {"from hive_table select hive_table.qualifiedName", 10},
                {"hive_db where (name = \"Reporting\")", 1},
                {"hive_db where (name = \"Reporting\") select name as _col_0, owner as _col_1", 1},
                {"hive_db where hive_db is JdbcAccess", 0},
                {"hive_db where hive_db has name", 3},
                {"hive_db as db1 hive_table where (db1.name = \"Reporting\")", 0},
                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1", 1},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1", 1},
                {"hive_table where (name = \"sales_fact\" and db.name = \"Sales\") select name, createTime", 1},
                {"hive_table where (name = \"time_dim\" and db.name = \"Sales\") or (name = \"sales_fact\" and db.name = \"Sales\") select name, createTime", 2},
                {"Dimension", 9},
                {"JdbcAccess", 2},
                {"ETL", 10},
                {"Metric", 8},
                {"PII", 4},
                {"`Log Data`", 4},
                {"DataSet where name='sales_fact'", 1},
                {"Asset where name='sales_fact'", 1},
                {"hive_db _NOT_CLASSIFIED", 3},
                {"_CLASSIFIED", 23},
                {"hive_db where name = [\"Reporting\",\"Sales\"]", 2},
                {"hive_db where name = ['Reporting', 'Sales']", 2},
        };
    }

    @Test(dataProvider = "basicProvider")
    public void basic(String query, int expected) throws AtlasBaseException {
        queryAssert(query, expected, DEFAULT_LIMIT, 0);
        queryAssert(query.replace("where", " "), expected, DEFAULT_LIMIT, 0);
    }

    @DataProvider(name = "systemAttributesProvider")
    private Object[][] systemAttributesQueries() {
        return new Object[][]{
                {"hive_db has __state", 3},
                {"hive_db where hive_db has __state", 3},
                {"hive_db as d where d.__state = 'ACTIVE'", 3},
                {"hive_db select __guid", 3},
                {"hive_db where __state = 'ACTIVE' select name, __guid, __state", 3},
                {"hive_db where __isIncomplete=true", 0},
                {"hive_db where __isIncomplete=false", 3},
        };
    }

    @Test(dataProvider = "systemAttributesProvider")
    public void systemAttributes(String query, int expected) throws AtlasBaseException {
        queryAssert(query, expected, DEFAULT_LIMIT, 0);
    }

    private AtlasSearchResult queryAssert(String query, final int expected, final int limit, final int offset) throws AtlasBaseException {
        AtlasSearchResult searchResult = discoveryService.searchUsingDslQuery(query, limit, offset);

        assertSearchResult(searchResult, expected, query);

        return searchResult;
    }

    @DataProvider(name = "limitProvider")
    private Object[][] limitQueries() {
        return new Object[][]{
                {"hive_column", 21, 40, 0},
                {"hive_column limit 10", 10, 50, 0},
                {"hive_column select hive_column.qualifiedName limit 10", 10, 5, 0},
                {"hive_column select hive_column.qualifiedName limit 40 offset 10", 11, 40, 0},
                {"hive_db where name = 'Reporting' limit 10 offset 0", 1, 40, 0},
                {"hive_table where db.name = 'Reporting' limit 10", 4, 1, 0},
                {"hive_column where name = 'test' ", 2, DEFAULT_LIMIT, DEFAULT_OFFSET},
                {"hive_column where name = 'test_limit' ", 2, DEFAULT_LIMIT, DEFAULT_OFFSET},
                {"hive_column where name = 'test_limit' limit 1 ", 1, DEFAULT_LIMIT, DEFAULT_OFFSET},
                {"hive_column where name = 'test_limit' limit 1 offset 1", 1, DEFAULT_LIMIT, DEFAULT_OFFSET},
        };
    }

    @Test(dataProvider = "limitProvider")
    public void limit(String query, int expected, int limit, int offset) throws AtlasBaseException {
        queryAssert(query, expected, limit, offset);
        queryAssert(query.replace("where", " "), expected, limit, offset);
    }

    @DataProvider(name = "syntaxProvider")
    private Object[][] syntaxQueries() {
        return new Object[][]{
                {"hive_column  limit 10 ", 10},
                {"hive_column select hive_column.qualifiedName limit 10 ", 10},
                {"from hive_db", 3},
                {"from hive_db limit 2", 2},
                {"from hive_db limit 2 offset 0", 2},
                {"from hive_db limit 2 offset 1", 2},
                {"from hive_db limit 3 offset 1", 2},
                {"hive_db", 3},
                {"hive_db where hive_db.name=\"Reporting\"", 1},
                {"hive_db where hive_db.name=\"Reporting\" or hive_db.name=\"Sales\" or hive_db.name=\"Logging\" limit 1 offset 1", 1},
                {"hive_db where hive_db.name=\"Reporting\" or hive_db.name=\"Sales\" or hive_db.name=\"Logging\" limit 1 offset 2", 1},
                {"hive_db where hive_db.name=\"Reporting\" or hive_db.name=\"Sales\" or hive_db.name=\"Logging\" limit 2 offset 1", 2},
                {"hive_db where hive_db.name=\"Reporting\" limit 10 ", 1},
                {"hive_db where hive_db.name=\"Reporting\" select name, owner", 1},
                {"hive_db has name", 3},
                {"hive_db has name limit 2 offset 0", 2},
                {"hive_db has name limit 2 offset 1", 2},
                {"hive_db has name limit 10 offset 1", 2},
                {"hive_db has name limit 10 offset 0", 3},

                {"from hive_table", 10},
                {"from hive_table limit 5", 5},
                {"from hive_table limit 5 offset 5", 5},

                {"hive_table", 10},
                {"hive_table limit 5", 5},
                {"hive_table limit 5 offset 5", 5},

                {"hive_table isa Dimension", 5},
                {"hive_table isa Dimension limit 2", 2},
                {"hive_table isa Dimension limit 2 offset 0", 2},
                {"hive_table isa Dimension limit 2 offset 1", 2},
                {"hive_table isa Dimension limit 3 offset 1", 3},
                {"hive_table where db.name='Sales' and db.clusterName='cl1'", 4},
                {"hive_table where name = 'sales_fact_monthly_mv' and db.name = 'Reporting' and columns.name = 'sales'",1},

                {"hive_column where hive_column isa PII", 4},
                {"hive_column where hive_column isa PII limit 5", 4},
                {"hive_column where hive_column isa PII limit 5 offset 1", 3},
                {"hive_column where hive_column isa PII limit 5 offset 5", 0},

                {"hive_column select hive_column.qualifiedName", 21},
                {"hive_column select hive_column.qualifiedName limit 5", 5},
                {"hive_column select hive_column.qualifiedName limit 5 offset 36", 0},

                {"hive_column select qualifiedName", 21},
                {"hive_column select qualifiedName limit 5", 5},
                {"hive_column select qualifiedName limit 5 offset 36 ", 0},

                {"hive_column where hive_column.name=\"customer_id\"", 2},
                {"hive_column where hive_column.name=\"customer_id\" limit 2", 2},
                {"hive_column where hive_column.name=\"customer_id\" limit 2 offset 1", 1},
                {"hive_column where hive_column.name=\"customer_id\" limit 10 offset 3", 0},

                {"from hive_table select hive_table.name", 10},
                {"from hive_table select hive_table.name limit 5", 5},
                {"from hive_table select hive_table.name limit 5 offset 5", 5},

                {"hive_db where (name = \"Reporting\")", 1},
                {"hive_db where (name = \"Reporting\") limit 10", 1},
                {"hive_db where (name = \"Reporting\") select name as _col_0, owner as _col_1", 1},
                {"hive_db where (name = \"Reporting\") select name as _col_0, owner as _col_1 limit 10", 1},
                {"hive_db where hive_db is JdbcAccess", 0}, //Not supposed to work
                {"hive_db where hive_db has name", 3},
                {"hive_db where hive_db has name limit 5", 3},
                {"hive_db where hive_db has name limit 2 offset 0", 2},
                {"hive_db where hive_db has name limit 2 offset 1", 2},

                {"hive_db as db1 hive_table where (db1.name = \"Reporting\")", 0},

                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1", 1},
                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1 limit 10", 1},
                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1 limit 10 offset 0", 1},
                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1 limit 10 offset 5", 0},

                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1", 1},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 limit 10 offset 0", 1},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 limit 10 offset 1", 0},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 limit 10", 1},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 limit 0 offset 1", 0},
                {"hive_db as d where owner = ['John ETL', 'Jane BI']", 2},
                {"hive_db as d where owner = ['John ETL', 'Jane BI'] limit 10", 2},
                {"hive_db as d where owner = ['John ETL', 'Jane BI'] limit 10 offset 1", 1},
                {"hive_db where description != '/apps/warehouse/logging'", 2},
                {"hive_db where (owner = \"John ETL\" and description != Random)", 1},
                {"hive_table where description != ''", 8},
                {"hive_db where (name='Reporting' or ((name='Logging' and owner = 'Jane BI') and (name='Logging' and owner = 'John ETL')))", 1}
        };
    }

    @Test(dataProvider = "syntaxProvider")
    public void syntax(String query, int expected) throws AtlasBaseException {
        queryAssert(query, expected, DEFAULT_LIMIT, 0);
        queryAssert(query.replace("where", " "), expected, DEFAULT_LIMIT, 0);
    }

    @DataProvider(name = "orderByProvider")
    private Object[][] orderByQueries() {
        return new Object[][]{
                {"from hive_db as h orderby h.owner limit 3", 3, "owner", true},
                {"hive_column as c select c.qualifiedName orderby hive_column.qualifiedName ", 21, "qualifiedName", true},
                {"hive_column as c select c.qualifiedName orderby hive_column.qualifiedName limit 5", 5, "qualifiedName", true},
                {"hive_column as c select c.qualifiedName orderby hive_column.qualifiedName desc limit 5", 5, "qualifiedName", false},

                {"from hive_db orderby hive_db.owner limit 3", 3, "owner", true},
                {"hive_column select hive_column.qualifiedName orderby hive_column.qualifiedName ", 21, "qualifiedName", true},
                {"hive_column select hive_column.qualifiedName orderby hive_column.qualifiedName limit 5", 5, "qualifiedName", true},
                {"hive_column select hive_column.qualifiedName orderby hive_column.qualifiedName desc limit 5", 5, "qualifiedName", false},

                {"from hive_db orderby owner limit 3", 3, "owner", true},
                {"hive_column select hive_column.qualifiedName orderby qualifiedName ", 21, "qualifiedName", true},
                {"hive_column select hive_column.qualifiedName orderby qualifiedName limit 5", 5, "qualifiedName", true},
                {"hive_column select hive_column.qualifiedName orderby qualifiedName desc limit 5", 5, "qualifiedName", false},

                {"from hive_db orderby hive_db.owner limit 3", 3, "owner", true},
                {"hive_db where hive_db.name=\"Reporting\" orderby owner", 1, "owner", true},

                {"hive_db where hive_db.name=\"Reporting\" orderby hive_db.owner limit 10 ", 1, "owner", true},
                {"hive_db where hive_db.name=\"Reporting\" select name, owner orderby hive_db.name ", 1, "name", true},
                {"hive_db has name orderby hive_db.owner limit 10 offset 0", 3, "owner", true},

                {"from hive_table select hive_table.owner orderby hive_table.owner", 10, "owner", true},
                {"from hive_table select hive_table.owner orderby hive_table.owner limit 8", 8, "owner", true},
                {"hive_table orderby hive_table.name", 10, "name", true},

                {"hive_table orderby hive_table.owner", 10, "owner", true},
                {"hive_table orderby hive_table.owner limit 8", 8, "owner", true},
                {"hive_table orderby hive_table.owner limit 8 offset 0", 8, "owner", true},
                {"hive_table orderby hive_table.owner desc limit 8 offset 0", 8, "owner", false},

                {"hive_column select hive_column.qualifiedName orderby hive_column.qualifiedName ", 21, "qualifiedName", true},
                {"hive_column select hive_column.qualifiedName orderby hive_column.qualifiedName limit 5", 5, "qualifiedName", true},
                {"hive_column select hive_column.qualifiedName orderby hive_column.qualifiedName desc limit 5", 5, "qualifiedName", false},
                {"hive_column select hive_column.qualifiedName orderby hive_column.qualifiedName limit 5 offset 2", 5, "qualifiedName", true},

                {"hive_column select qualifiedName orderby hive_column.qualifiedName", 21, "qualifiedName", true},
                {"hive_column select qualifiedName orderby hive_column.qualifiedName limit 5", 5, "qualifiedName", true},
                {"hive_column select qualifiedName orderby hive_column.qualifiedName desc", 21, "qualifiedName", false},

                {"hive_column where hive_column.name=\"customer_id\" orderby hive_column.name", 2, "name", true},
                {"hive_column where hive_column.name=\"customer_id\" orderby hive_column.name limit 2", 2, "name", true},
                {"hive_column where hive_column.name=\"customer_id\" orderby hive_column.name limit 2 offset 1", 1, "name", true},

                {"from hive_table select owner orderby hive_table.owner",10, "owner", true},
                {"from hive_table select owner orderby hive_table.owner limit 5", 5, "owner", true},
                {"from hive_table select owner orderby hive_table.owner desc limit 5", 5, "owner", false},
                {"from hive_table select owner orderby hive_table.owner limit 5 offset 5", 5, "owner", true},

                {"hive_db where (name = \"Reporting\") orderby hive_db.name", 1, "name", true},
                {"hive_db where (name = \"Reporting\") orderby hive_db.name limit 10", 1, "name", true},
                {"hive_db where hive_db has name orderby hive_db.owner", 3, "owner", true},
                {"hive_db where hive_db has name orderby hive_db.owner limit 5", 3, "owner", true},
                {"hive_db where hive_db has name orderby hive_db.owner limit 2 offset 0", 2, "owner", true},
                {"hive_db where hive_db has name orderby hive_db.owner limit 2 offset 1", 2, "owner", true},

                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1 orderby createTime ", 1, "createTime", true},
                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1 orderby createTime limit 10 ", 1, "createTime", true},
                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1 orderby createTime limit 10 offset 0", 1, "createTime", true},
                {"hive_table where (name = \"sales_fact\" and createTime > \"2014-01-01\" ) select name as _col_0, createTime as _col_1 orderby createTime limit 10 offset 5", 0, "createTime", true},

                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 orderby name ", 1, "name", true},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 orderby name limit 10 offset 0", 1, "name", true},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 orderby name limit 10 offset 1", 0, "name", true},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 orderby name limit 10", 1, "name", true},
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12-11T02:35:58.440Z\" ) select name as _col_0, createTime as _col_1 orderby name limit 0 offset 1", 0, "name", true},
        };
    }

    @Test(dataProvider = "orderByProvider")
    public void orderBy(String query, int expected, String attributeName, boolean ascending) throws AtlasBaseException {
        AtlasSearchResult  searchResult = queryAssert(query, expected, DEFAULT_LIMIT, 0);

        assertSortOrder(query, attributeName, ascending, searchResult.getEntities());

        searchResult = queryAssert(query.replace("where", " "), expected, DEFAULT_LIMIT, 0);

        assertSortOrder(query, attributeName, ascending, searchResult.getEntities());
    }

    private void assertSortOrder(String query, String attributeName, boolean ascending, List<AtlasEntityHeader> entities) {
        if (entities == null) {
            return;
        }

        AtlasEntityHeader prev = null;

        for (AtlasEntityHeader current : entities) {
            if (prev != null && current.hasAttribute(attributeName)) {
                String lhs        = (String) prev.getAttribute(attributeName);
                String rhs        = (String) current.getAttribute(attributeName);
                int    compResult = lhs.compareTo(rhs);

                if (ascending) {
                    assertTrue(compResult <= 0, query);
                } else {
                    assertTrue(compResult >= 0, query);
                }
            }

            prev = current;
        }
    }

    @DataProvider(name = "likeQueriesProvider")
    private Object[][] likeQueries() {
        return new Object[][]{
                {"hive_table qualifiedName like \"*time_dim*\"", 1, new ListValidator("time_dim")},
                {"hive_db where qualifiedName like \"qualified:R*\"", 1, new ListValidator("Reporting")},
                {"hive_table db.name=\"Sales\"", 4, new ListValidator("customer_dim", "sales_fact", "time_dim", "product_dim")},
                {"hive_table qualifiedName =\"Sales.time_dim\" AND db.name=\"Sales\"", 1, new ListValidator("time_dim")},
                {"hive_table qualifiedName like \"*time_dim*\" AND db.name=\"Sales\"", 1, new ListValidator("time_dim")},
                {"hive_table where name like \"sa?es*\"", 3, new ListValidator("sales_fact", "sales_fact_daily_mv", "sales_fact_monthly_mv")},
                {"hive_db where name like \"R*\"", 1, new ListValidator("Reporting")},
                {"hive_db where hive_db.name like \"R???rt?*\" or hive_db.name like \"S?l?s\" or hive_db.name like\"Log*\"", 3, new ListValidator("Reporting", "Sales", "Logging") },
                {"hive_db where hive_db.name like \"R???rt?*\" and hive_db.name like \"S?l?s\" and hive_db.name like\"Log*\"", 0, new ListValidator()},
                {"hive_table where name like 'sales*' and db.name like 'Sa?es'", 1, new ListValidator("sales_fact")},
                {"hive_table where db.name like \"Sa*\"", 4, new ListValidator("customer_dim", "sales_fact", "time_dim", "product_dim")},
                {"hive_table where db.name like \"Sa*\" and name like \"*dim\"", 3, new ListValidator("customer_dim", "product_dim", "time_dim")},
                //STRING Mapping
                {"hive_db where userDescription like \"*/warehouse/*\"", 3, new ListValidator("Sales","Reporting","Logging")},
                {"hive_db where userDescription like \"/apps/warehouse/*\"", 3, new ListValidator("Sales","Reporting","Logging")},
                //TEXT Mapping
                {"hive_db where description like \"*/warehouse/*\"", 3, new ListValidator("Sales","Reporting","Logging")},
                {"hive_db where description like \"/apps/warehouse/*\"", 3, new ListValidator("Sales","Reporting","Logging")},
                {"hive_table where name like \"table[0-2]\"", 2, new ListValidator("table1", "table2")},
        };
    }

    @Test(dataProvider = "likeQueriesProvider")
    public void likeQueries(String query, int expected, ListValidator lv) throws AtlasBaseException {
        queryAssert(query, expected, DEFAULT_LIMIT, 0, lv);
        queryAssert(query.replace("where", " "), expected, DEFAULT_LIMIT, 0, lv);
    }

    private void queryAssert(String query, int expectedCount, int limit, int offset, ListValidator expected) throws AtlasBaseException {
        AtlasSearchResult result = queryAssert(query, expectedCount, limit, offset);

        ListValidator.assertLv(ListValidator.from(result), expected);
    }

    @DataProvider(name = "minMaxCountProvider")
    private Object[][] minMaxCountQueries() {
        return new Object[][]{
                {"from hive_db select max(name), min(name)",
                        new TableValidator("max(name)", "min(name)")
                                .row("Sales", "Logging")},
                {"from hive_db groupby (owner) select count() ",
                        new TableValidator("count()")
                                .row(1)
                                .row(1)
                                .row(1)},
                {"from hive_db groupby (owner) select owner, name orderby owner",
                        new TableValidator("owner", "name")
                                .row("Jane BI", "Reporting")
                                .row("John ETL", "Sales")
                                .row("Tim ETL", "Logging")},
                {"from hive_db groupby (owner) select Asset.owner, Asset.name, count()",
                        new TableValidator("Asset.owner", "Asset.name", "count()")
                                .row("Jane BI", "Reporting", 1)
                                .row("Tim ETL", "Logging", 1)
                                .row("John ETL", "Sales", 1)},
                {"from hive_db groupby (owner) select count() ",
                        new TableValidator("count()").
                                row(1).
                                row(1).
                                row(1)},
                {"from hive_db groupby (owner) select Asset.owner, count() ",
                        new TableValidator("Asset.owner", "count()")
                                .row("Jane BI", 1)
                                .row("Tim ETL", 1)
                                .row("John ETL", 1)},
                {"from hive_db groupby (owner) select count() ",
                        new TableValidator("count()")
                                .row(1)
                                .row(1)
                                .row(1)},
                {"from hive_db groupby (owner) select Asset.owner, count() ",
                        new TableValidator("Asset.owner", "count()")
                                .row("Jane BI", 1)
                                .row("Tim ETL", 1)
                                .row("John ETL", 1)},

                {"from hive_db groupby (owner) select Asset.owner, max(Asset.name) ",
                        new TableValidator("Asset.owner", "max(Asset.name)")
                                .row("Tim ETL", "Logging")
                                .row("Jane BI", "Reporting")
                                .row("John ETL", "Sales")},

                {"from hive_db groupby (owner) select max(Asset.name) ",
                        new TableValidator("max(Asset.name)")
                                .row("Logging")
                                .row("Reporting")
                                .row("Sales")},

                {"from hive_db groupby (owner) select owner, Asset.name, min(Asset.name)  ",
                        new TableValidator("owner", "Asset.name", "min(Asset.name)")
                                .row("Tim ETL", "Logging", "Logging")
                                .row("Jane BI", "Reporting", "Reporting")
                                .row("John ETL", "Sales", "Sales")},

                {"from hive_db groupby (owner) select owner, min(Asset.name)  ",
                        new TableValidator("owner", "min(Asset.name)")
                                .row("Tim ETL", "Logging")
                                .row("Jane BI", "Reporting")
                                .row("John ETL", "Sales")},

                {"from hive_db groupby (owner) select min(name)  ",
                        new TableValidator("min(name)")
                                .row("Reporting")
                                .row("Logging")
                                .row("Sales")},
                {"from hive_db groupby (owner) select min('name') ",
                        new TableValidator("min('name')")
                                .row("Reporting")
                                .row("Logging")
                                .row("Sales")},
                {"from hive_db select count() ",
                        new TableValidator("count()")
                                .row(3)},
                {"from Person select count() as 'count', max(Person.age) as 'max', min(Person.age) as 'min'",
                        new TableValidator("'count'", "'max'", "'min'")
                                .row(4, 50.0f, 0.0f)},
                {"from Person select count() as 'count', sum(Person.age) as 'sum'",
                        new TableValidator("'count'", "'sum'")
                                .row(4, 86.0)},
                {"from Asset where __isIncomplete = false groupby (__typeName)  select __typeName, count()",
                        new TableValidator("__typeName", "count()")
                                .row("Asset", 1)
                                .row("hive_table", 10)
                                .row("hive_column", 21)
                                .row("hive_db", 3)
                                .row("hive_process", 7)
                }
        };
    }

    @Test(dataProvider = "minMaxCountProvider")
    public void minMaxCount(String query, TableValidator fv) throws AtlasBaseException {
        queryAssert(query, fv);
    }

    @DataProvider(name = "errorQueriesProvider")
    private Object[][] errorQueries() {
        return new Object[][]{
                {"`isa`"}, // Tag doesn't exist in the test data
                {"PIII"},  // same as above
                {"DBBB as d select d"}, // same as above
                {"hive_db has db"}, // same as above
                {"hive_table where (name = \"sales_fact\" and createTime >= \"2014-12\" ) select name as _col_0, createTime as _col_1 orderby name limit 0 offset 1"},
                {"hive_table as t, sd, hive_column as c where t.name=\"sales_fact\" select c.name as colName, c.dataType as colType"},
                {"hive_table isa hive_db"}, // isa should be a trait/classification
                {"hive_table isa FooTag"},  // FooTag doesn't exist
                {"hive_table groupby(db.name)"}, // GroupBy on referred attribute is not supported
                {"hive_table orderby(db.name)"}, // OrderBy on referred attribute is not supported
                {"hive_table select db, columns"}, // Can't select multiple referred attributes/entity
                {"hive_table select min(db.name), columns"}, // Can't do aggregation on referred attribute
                {"hive_table select db.name, columns"}, // Can't select more than one referred attribute
                {"hive_table select owner, columns"}, // Can't select a mix of immediate attribute and referred entity
                {"hive_table select owner, db.name"}, // Same as above
                {"hive_order"}, // From src should be an Entity or Classification
                {"hive_table hasTerm modernTrade@salesGlossary"},//should be encoded with double quotes
                {"Dimension where tagAttr1 = 'tagValue'"}, // prefix for classification attributes is must
                {"Dimension where Dimension.tagAttr1 = 'tagValue' and name = 'time_dim'"},

        };
    }

    @Test
    public void testQuery() {
        try {
            discoveryService.searchUsingDslQuery("hive_table select db", DEFAULT_LIMIT, 0);
        } catch (AtlasBaseException e) {
            fail("Should've been a success");
        }
    }

    @Test(dataProvider = "errorQueriesProvider", expectedExceptions = { AtlasBaseException.class })
    public void errorQueries(String query) throws AtlasBaseException {
        LOG.debug(query);

        discoveryService.searchUsingDslQuery(query, DEFAULT_LIMIT, 0);
    }

    private void queryAssert(String query, TableValidator fv) throws AtlasBaseException {
        AtlasSearchResult searchResult = discoveryService.searchUsingDslQuery(query, DEFAULT_LIMIT, 0);

        assertNotNull(searchResult);
        assertNull(searchResult.getEntities());

        TableValidator.assertFv(TableValidator.from(searchResult.getAttributes()), fv);
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

    private static class TableValidator {
        static class NameValueEntry {
            Map<String, Object> items = new LinkedHashMap<>();

            public void setFieldValue(String string, Object object) {
                items.put(string, object);
            }
        }

        public String[]             fieldNames;
        public List<NameValueEntry> values = new ArrayList<>();

        public TableValidator() {
        }

        public TableValidator(String... fieldNames) {
            header(fieldNames);
        }

        public TableValidator header(String... fieldNames) {
            this.fieldNames = fieldNames;

            return this;
        }

        public TableValidator row(Object... values) {
            NameValueEntry obj = new NameValueEntry();

            for (int i = 0; i < fieldNames.length; i++) {
                obj.setFieldValue(fieldNames[i], values[i]);
            }

            this.values.add(obj);

            return this;
        }

        public static void assertFv(TableValidator actual, TableValidator expected) {
            assertEquals(actual.fieldNames.length, expected.fieldNames.length);
            assertEquals(actual.fieldNames, expected.fieldNames);
            assertEquals(actual.values.size(), expected.values.size());

            Map<String, Object> actualKeyItemsForCompare = new HashMap<>();
            Map<String, Object> expectedItemsForCompare  = new HashMap<>();

            for (int i = 0; i < actual.values.size(); i++) {
                getMapFrom(expectedItemsForCompare, expected.values.get(i).items);
                getMapFrom(actualKeyItemsForCompare, actual.values.get(i).items);
            }

            for (String key : actualKeyItemsForCompare.keySet()) {
                Object actualValue   = actualKeyItemsForCompare.get(key);
                Object expectedValue = expectedItemsForCompare.get(key);

                assertNotNull(actualValue, "Key: " + key + ": Failed!");
                assertEquals(actualValue, expectedValue, "Key: " + key + ": Value compare failed!");
            }
        }

        private static Map<String, Object> getMapFrom(Map<String, Object> valuesMap, Map<String, Object> linkedHashMap) {
            for (Map.Entry<String, Object> entry : linkedHashMap.entrySet()) {
                String key = entry.getValue().toString();

                valuesMap.put(key, linkedHashMap);
                break;
            }

            return valuesMap;
        }

        public static TableValidator from(AtlasSearchResult.AttributeSearchResult searchResult) {
            TableValidator fv = new TableValidator();

            fv.header(searchResult.getName().toArray(new String[]{}));

            for (int i = 0; i < searchResult.getValues().size(); i++) {
                List list = searchResult.getValues().get(i);

                fv.row(list.toArray());
            }

            return fv;
        }
    }

    private static class ListValidator {
        private Set<String> values;
        public ListValidator(String... vals) {
            values = Arrays.stream(vals).collect(Collectors.toSet());
        }

        public static void assertLv(ListValidator actual, ListValidator expected) {
            String errorMessage = String.format("Expected: %s\r\nActual: %s", expected.values, actual.values);

            assertEquals(actual.values.size(), expected.values.size(), errorMessage);

            if (expected.values.size() > 0) {
                for (String expectedVal : expected.values) {
                    assertTrue(actual.values.contains(expectedVal), errorMessage);
                }
            }
        }

        public static ListValidator from(AtlasSearchResult result) {
            ListValidator lv = new ListValidator();

            if (result.getEntities() != null) {
                lv.values.addAll(result.getEntities().stream().map(x -> x.getDisplayText()).collect(Collectors.toSet()));
            }

            return lv;
        }
    }
}
