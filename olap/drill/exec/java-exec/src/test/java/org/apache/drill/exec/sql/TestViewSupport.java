/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.sql;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.drill.categories.SqlTest;
import org.apache.drill.categories.UnlikelyTest;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.drill.exec.util.StoragePluginTestUtils.DFS_TMP_SCHEMA;
import static org.apache.drill.exec.util.StoragePluginTestUtils.TMP_SCHEMA;

@Category(SqlTest.class)
public class TestViewSupport extends TestBaseViewSupport {

  @BeforeClass
  public static void setupTestFiles() {
    dirTestWatcher.copyResourceToRoot(Paths.get("nation"));
    dirTestWatcher.copyResourceToRoot(Paths.get("avro", "map_string_to_long.avro"));
  }

  @Test
  public void referToSchemaInsideAndOutsideView() throws Exception {
    String use = "use dfs.tmp;";
    String selectInto = "create table monkey as select c_custkey, c_nationkey from cp.`tpch/customer.parquet`";
    String createView = "create or replace view myMonkeyView as select c_custkey, c_nationkey from monkey";
    String selectInside = "select * from myMonkeyView;";
    String use2 = "use cp;";
    String selectOutside = "select * from dfs.tmp.myMonkeyView;";

    test(use);
    test(selectInto);
    test(createView);
    test(selectInside);
    test(use2);
    test(selectOutside);
  }

  /**
   * DRILL-2342 This test is for case where output columns are nullable. Existing tests already cover the case
   * where columns are required.
   */
  @Test
  public void nullabilityPropertyInViewPersistence() throws Exception {
    final String viewName = "testNullabilityPropertyInViewPersistence";
    try {

      test("USE dfs.tmp");
      test(String.format("CREATE OR REPLACE VIEW %s AS SELECT " +
          "CAST(customer_id AS BIGINT) as cust_id, " +
          "CAST(fname AS VARCHAR(25)) as fname, " +
          "CAST(country AS VARCHAR(20)) as country " +
          "FROM cp.`customer.json` " +
          "ORDER BY customer_id " +
          "LIMIT 1;", viewName));

      testBuilder()
          .sqlQuery(String.format("DESCRIBE %s", viewName))
          .unOrdered()
          .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
          .baselineValues("cust_id", "BIGINT", "YES")
          .baselineValues("fname", "CHARACTER VARYING", "YES")
          .baselineValues("country", "CHARACTER VARYING", "YES")
          .go();

      testBuilder()
          .sqlQuery(String.format("SELECT * FROM %s", viewName))
          .ordered()
          .baselineColumns("cust_id", "fname", "country")
          .baselineValues(1L, "Sheri", "Mexico")
          .go();
    } finally {
      test("drop view " + viewName + ";");
    }
  }

  @Test
  public void viewWithStarInDef_StarInQuery() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        null,
        "SELECT * FROM cp.`region.json` ORDER BY `region_id`",
        "SELECT * FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 1",
        baselineColumns("region_id", "sales_city", "sales_state_province", "sales_district",
                        "sales_region", "sales_country", "sales_district_id"),
        baselineRows(row(0L, "None", "None", "No District",
                        "No Region", "No Country", 0L))
    );
  }

  @Test
  public void viewWithSelectFieldsInDef_StarInQuery() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        null,
        "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id`",
        "SELECT * FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 2",
        baselineColumns("region_id", "sales_city"),
        baselineRows(row(0L, "None"), row(1L, "San Francisco"))
    );
  }

  @Test
  public void viewWithSelectFieldsInDef_SelectFieldsInView_StarInQuery() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        "(regionid, salescity)",
        "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id`",
        "SELECT * FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 2",
        baselineColumns("regionid", "salescity"),
        baselineRows(row(0L, "None"), row(1L, "San Francisco"))
    );
  }

  @Test
  public void viewWithStarInDef_SelectFieldsInQuery() throws Exception{
    testViewHelper(
        DFS_TMP_SCHEMA,
        null,
        "SELECT * FROM cp.`region.json` ORDER BY `region_id`",
        "SELECT region_id, sales_city FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 2",
        baselineColumns("region_id", "sales_city"),
        baselineRows(row(0L, "None"), row(1L, "San Francisco"))
    );
  }

  @Test
  public void viewWithSelectFieldsInDef_SelectFieldsInQuery1() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        null,
        "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id`",
        "SELECT region_id, sales_city FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 2",
        baselineColumns("region_id", "sales_city"),
        baselineRows(row(0L, "None"), row(1L, "San Francisco"))
    );
  }

  @Test
  public void viewWithSelectFieldsInDef_SelectFieldsInQuery2() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        null,
        "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id`",
        "SELECT sales_city FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 2",
        baselineColumns("sales_city"),
        baselineRows(row("None"), row("San Francisco"))
    );
  }

  @Test
  public void viewWithSelectFieldsInDef_SelectFieldsInView_SelectFieldsInQuery1() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        "(regionid, salescity)",
        "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id` LIMIT 2",
        "SELECT regionid, salescity FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 2",
        baselineColumns("regionid", "salescity"),
        baselineRows(row(0L, "None"), row(1L, "San Francisco"))
    );
  }

  @Test
  public void viewWithSelectFieldsInDef_SelectFieldsInView_SelectFieldsInQuery2() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        "(regionid, salescity)",
        "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id` DESC",
        "SELECT regionid FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 2",
        baselineColumns("regionid"),
        baselineRows(row(109L), row(108L))
    );
  }

  @Test
  @Ignore("DRILL-1921")
  public void viewWithUnionWithSelectFieldsInDef_StarInQuery() throws Exception{
    testViewHelper(
        DFS_TMP_SCHEMA,
        null,
        "SELECT region_id FROM cp.`region.json` UNION SELECT employee_id FROM cp.`employee.json`",
        "SELECT regionid FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 2",
        baselineColumns("regionid"),
        baselineRows(row(110L), row(108L))
    );
  }

  @Test
  public void viewCreatedFromAnotherView() throws Exception {
    final String innerView = generateViewName();
    final String outerView = generateViewName();

    try {
      createViewHelper(DFS_TMP_SCHEMA, innerView, DFS_TMP_SCHEMA, null,
          "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id`");

      createViewHelper(DFS_TMP_SCHEMA, outerView, DFS_TMP_SCHEMA, null,
          String.format("SELECT region_id FROM %s.`%s`", DFS_TMP_SCHEMA, innerView));

      queryViewHelper(
          String.format("SELECT region_id FROM %s.`%s` LIMIT 1", DFS_TMP_SCHEMA, outerView),
          baselineColumns("region_id"),
          baselineRows(row(0L))
      );
    } finally {
      dropViewHelper(DFS_TMP_SCHEMA, outerView, DFS_TMP_SCHEMA);
      dropViewHelper(DFS_TMP_SCHEMA, innerView, DFS_TMP_SCHEMA);
    }
  }

  @Test // DRILL-1015
  @Category(UnlikelyTest.class)
  public void viewWithCompoundIdentifiersInDef() throws Exception{
    final String viewDef = "SELECT " +
        "cast(columns[0] AS int) n_nationkey, " +
        "cast(columns[1] AS CHAR(25)) n_name, " +
        "cast(columns[2] AS INT) n_regionkey, " +
        "cast(columns[3] AS VARCHAR(152)) n_comment " +
        "FROM dfs.`nation`";

    testViewHelper(
        DFS_TMP_SCHEMA,
        null,
        viewDef,
        "SELECT * FROM TEST_SCHEMA.TEST_VIEW_NAME LIMIT 1",
        baselineColumns("n_nationkey", "n_name", "n_regionkey", "n_comment"),
        baselineRows(row(0, "ALGERIA", 0, " haggle. carefully final deposits detect slyly agai"))
    );
  }

  @Test
  public void createViewWhenViewAlreadyExists() throws Exception {
    final String viewName = generateViewName();

    try {
      final String viewDef1 = "SELECT region_id, sales_city FROM cp.`region.json`";

      // Create the view
      createViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA, null, viewDef1);

      // Try to create the view with same name in same schema.
      final String createViewSql = String.format("CREATE VIEW %s.`%s` AS %s", DFS_TMP_SCHEMA, viewName, viewDef1);
      errorMsgTestHelper(createViewSql,
          String.format("A view with given name [%s] already exists in schema [%s]", viewName, DFS_TMP_SCHEMA));

      // Try creating the view with same name in same schema, but with CREATE OR REPLACE VIEW clause
      final String viewDef2 = "SELECT sales_state_province FROM cp.`region.json` ORDER BY `region_id`";

      testBuilder()
          .sqlQuery("CREATE OR REPLACE VIEW %s.`%s` AS %s", DFS_TMP_SCHEMA, viewName, viewDef2)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true,
              String.format("View '%s' replaced successfully in '%s' schema", viewName, DFS_TMP_SCHEMA))
          .go();

      // Make sure the new view created returns the data expected.
      queryViewHelper(String.format("SELECT * FROM %s.`%s` LIMIT 1", DFS_TMP_SCHEMA, viewName),
          baselineColumns("sales_state_province"),
          baselineRows(row("None"))
      );
    } finally {
      dropViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA);
    }
  }

  @Test // DRILL-5952
  public void createViewIfNotExistsWhenTableAlreadyExists() throws Exception {
    final String tableName = generateViewName();

    try {
      final String tableDef = "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id` LIMIT 2";

      test("CREATE TABLE %s.%s as %s", DFS_TMP_SCHEMA, tableName, tableDef);

      // Try to create the view with same name in same schema with if not exists clause.
      final String createViewSql = String.format("CREATE VIEW IF NOT EXISTS %s.`%s` AS %s", DFS_TMP_SCHEMA, tableName, tableDef);

      testBuilder()
        .sqlQuery(createViewSql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false,
          String.format("A table or view with given name [%s] already exists in schema [%s]", tableName, DFS_TMP_SCHEMA))
        .go();

    } finally {
      FileUtils.deleteQuietly(new File(dirTestWatcher.getDfsTestTmpDir(), tableName));
    }
  }

  @Test // DRILL-5952
  public void createViewIfNotExistsWhenViewAlreadyExists() throws Exception {
    final String viewName = generateViewName();

    try {
      final String viewDef1 = "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id` LIMIT 2";

      // Create the view
      createViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA, null, viewDef1);

      // Try to create the view with same name in same schema with if not exists clause.
      final String viewDef2 = "SELECT sales_state_province FROM cp.`region.json` ORDER BY `region_id`";
      final String createViewSql = String.format("CREATE VIEW IF NOT EXISTS %s.`%s` AS %s", DFS_TMP_SCHEMA, viewName, viewDef2);

      testBuilder()
        .sqlQuery(createViewSql)
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(false,
          String.format("A table or view with given name [%s] already exists in schema [%s]", viewName, DFS_TMP_SCHEMA))
        .go();

      // Make sure the view created returns the data expected.
      queryViewHelper(String.format("SELECT * FROM %s.`%s` LIMIT 1", DFS_TMP_SCHEMA, viewName),
          baselineColumns("region_id", "sales_city"),
          baselineRows(row(0L, "None"))
      );
    } finally {
      dropViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA);
    }
  }

  @Test // DRILL-5952
  public void testCreateViewIfNotExists() throws Exception {
    final String viewName = generateViewName();

    try {
      final String viewDef = "SELECT region_id, sales_city FROM cp.`region.json` ORDER BY `region_id` LIMIT 2";

      final String createViewSql = String.format("CREATE VIEW IF NOT EXISTS %s.`%s` AS %s", DFS_TMP_SCHEMA, viewName, viewDef);

      test(createViewSql);

      // Make sure the view created returns the data expected.
      queryViewHelper(String.format("SELECT * FROM %s.`%s` LIMIT 1", DFS_TMP_SCHEMA, viewName),
          baselineColumns("region_id", "sales_city"),
          baselineRows(row(0L, "None"))
      );
    } finally {
      dropViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA);
    }
  }

  @Test // DRILL-5952
  public void createViewWithBothOrReplaceAndIfNotExists() throws Exception {
    final String viewName = generateViewName();

    final String viewDef = "SELECT region_id, sales_city FROM cp.`region.json`";

    // Try to create the view with both <or replace> and <if not exists> clause.
    final String createViewSql = String.format("CREATE OR REPLACE VIEW IF NOT EXISTS %s.`%s` AS %s", DFS_TMP_SCHEMA, viewName, viewDef);

    errorMsgTestHelper(createViewSql, "Create view statement cannot have both <OR REPLACE> and <IF NOT EXISTS> clause");
  }

  @Test // DRILL-2422
  @Category(UnlikelyTest.class)
  public void createViewWhenATableWithSameNameAlreadyExists() throws Exception {
    final String tableName = generateViewName();

    try {
      final String tableDef1 = "SELECT region_id, sales_city FROM cp.`region.json`";

      test("CREATE TABLE %s.%s as %s", DFS_TMP_SCHEMA, tableName, tableDef1);

      // Try to create the view with same name in same schema.
      final String createViewSql = String.format("CREATE VIEW %s.`%s` AS %s", DFS_TMP_SCHEMA, tableName, tableDef1);
      errorMsgTestHelper(createViewSql,
          String.format("A non-view table with given name [%s] already exists in schema [%s]", tableName, DFS_TMP_SCHEMA));

      // Try creating the view with same name in same schema, but with CREATE OR REPLACE VIEW clause
      final String viewDef2 = "SELECT sales_state_province FROM cp.`region.json` ORDER BY `region_id`";
      errorMsgTestHelper(String.format("CREATE OR REPLACE VIEW %s.`%s` AS %s", DFS_TMP_SCHEMA, tableName, viewDef2),
          String.format("A non-view table with given name [%s] already exists in schema [%s]", tableName, DFS_TMP_SCHEMA));
    } finally {
      FileUtils.deleteQuietly(new File(dirTestWatcher.getDfsTestTmpDir(), tableName));
    }
  }

  @Test
  public void infoSchemaWithView() throws Exception {
    final String viewName = generateViewName();

    try {
      test("USE " + DFS_TMP_SCHEMA);
      createViewHelper(null /*pass no schema*/, viewName, DFS_TMP_SCHEMA, null,
          "SELECT cast(`employee_id` as integer) employeeid FROM cp.`employee.json`");

      // Test SHOW TABLES on view
      testBuilder()
          .sqlQuery("SHOW TABLES like '%s'", viewName)
          .unOrdered()
          .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
          .baselineValues(DFS_TMP_SCHEMA, viewName)
          .go();

      // Test record in INFORMATION_SCHEMA.VIEWS
      testBuilder()
          .sqlQuery("SELECT * FROM INFORMATION_SCHEMA.VIEWS WHERE TABLE_NAME = '%s'", viewName)
          .unOrdered()
          .baselineColumns("TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "VIEW_DEFINITION")
          .baselineValues("DRILL", DFS_TMP_SCHEMA, viewName,
              "SELECT CAST(`employee_id` AS INTEGER) AS `employeeid`\nFROM `cp`.`employee.json`")
          .go();

      // Test record in INFORMATION_SCHEMA.TABLES
      testBuilder()
          .sqlQuery("SELECT TABLE_CATALOG, TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE" +
            " FROM INFORMATION_SCHEMA.`TABLES` WHERE TABLE_NAME = '%s'", viewName)
          .unOrdered()
          .baselineColumns("TABLE_CATALOG", "TABLE_SCHEMA", "TABLE_NAME", "TABLE_TYPE")
          .baselineValues("DRILL", DFS_TMP_SCHEMA, viewName, "VIEW")
          .go();

      // Test DESCRIBE view
      testBuilder()
          .sqlQuery("DESCRIBE `%s`", viewName)
          .unOrdered()
          .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
          .baselineValues("employeeid", "INTEGER", "YES")
          .go();
    } finally {
      dropViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA);
    }
  }

  @Test
  public void viewWithPartialSchemaIdentifier() throws Exception {
    final String viewName = generateViewName();

    try {
      // Change default schema to just "dfs". View is actually created in "dfs.tmp" schema.
      test("USE dfs");

      // Create a view with with "tmp" schema identifier
      createViewHelper("tmp", viewName, DFS_TMP_SCHEMA, null,
          "SELECT CAST(`employee_id` AS INTEGER) AS `employeeid`\n" + "FROM `cp`.`employee.json`");

      final String[] baselineColumns = baselineColumns("employeeid");
      final List<Object[]> baselineValues = baselineRows(row(1156));

      // Query view from current schema "dfs" by referring to the view using "tmp.viewName"
      queryViewHelper(
          String.format("SELECT * FROM %s.`%s` ORDER BY `employeeid` DESC LIMIT 1", "tmp", viewName),
          baselineColumns, baselineValues);

      // Change the default schema to "dfs.tmp" and query view by referring to it using "viewName"
      test("USE dfs.tmp");
      queryViewHelper(
          String.format("SELECT * FROM `%s` ORDER BY `employeeid` DESC LIMIT 1", viewName),
          baselineColumns, baselineValues);

      // Change the default schema to "cp" and query view by referring to it using "dfs.tmp.viewName";
      test("USE cp");
      queryViewHelper(
          String.format("SELECT * FROM %s.`%s` ORDER BY `employeeid` DESC LIMIT 1", "dfs.tmp", viewName),
          baselineColumns, baselineValues);

    } finally {
      dropViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA);
    }
  }

  @Test // DRILL-1114
  @Category(UnlikelyTest.class)
  public void viewResolvingTablesInWorkspaceSchema() throws Exception {
    final String viewName = generateViewName();

    try {
      // Change default schema to "cp"
      test("USE cp");

      // Create a view with full schema identifier and refer the "region.json" as without schema.
      createViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA, null, "SELECT region_id, sales_city FROM `region.json`");

      final String[] baselineColumns = baselineColumns("region_id", "sales_city");
      final List<Object[]> baselineValues = baselineRows(row(109L, "Santa Fe"));

      // Query the view
      queryViewHelper(
          String.format("SELECT * FROM %s.`%s` ORDER BY region_id DESC LIMIT 1", DFS_TMP_SCHEMA, viewName),
          baselineColumns, baselineValues);

      // Change default schema to "dfs" and query by referring to the view using "tmp.viewName"
      test("USE dfs");
      queryViewHelper(
          String.format("SELECT * FROM %s.`%s` ORDER BY region_id DESC LIMIT 1", TMP_SCHEMA, viewName),
          baselineColumns, baselineValues);

    } finally {
      dropViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA);
    }
  }

  // DRILL-2341, View schema verification where view's field is not specified is already tested in
  // TestViewSupport.infoSchemaWithView.
  @Test
  @Category(UnlikelyTest.class)
  public void viewSchemaWhenSelectFieldsInDef_SelectFieldsInView() throws Exception {
    final String viewName = generateViewName();

    try {
      test("use %s", DFS_TMP_SCHEMA);
      createViewHelper(null, viewName, DFS_TMP_SCHEMA, "(id, name, bday)",
          "SELECT " +
              "cast(`region_id` as integer), " +
              "cast(`full_name` as varchar(100)), " +
              "cast(`birth_date` as date) " +
              "FROM cp.`employee.json`");

      // Test DESCRIBE view
      testBuilder()
          .sqlQuery("DESCRIBE `%s`", viewName)
          .unOrdered()
          .baselineColumns("COLUMN_NAME", "DATA_TYPE", "IS_NULLABLE")
          .baselineValues("id", "INTEGER", "YES")
          .baselineValues("name", "CHARACTER VARYING", "YES")
          .baselineValues("bday", "DATE", "YES")
          .go();
    } finally {
      dropViewHelper(DFS_TMP_SCHEMA, viewName, DFS_TMP_SCHEMA);
    }
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWithDuplicateColumnsInDef1() throws Exception {
    createViewErrorTestHelper(
        "CREATE VIEW %s.%s AS SELECT region_id, region_id FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "region_id")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWithDuplicateColumnsInDef2() throws Exception {
    createViewErrorTestHelper("CREATE VIEW %s.%s AS SELECT region_id, sales_city, sales_city FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "sales_city")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWithDuplicateColumnsInDef3() throws Exception {
    createViewErrorTestHelper(
        "CREATE VIEW %s.%s(regionid, regionid) AS SELECT region_id, sales_city FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "regionid")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWithDuplicateColumnsInDef4() throws Exception {
    createViewErrorTestHelper(
        "CREATE VIEW %s.%s(regionid, salescity, salescity) " +
            "AS SELECT region_id, sales_city, sales_city FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "salescity")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWithDuplicateColumnsInDef5() throws Exception {
    createViewErrorTestHelper(
        "CREATE VIEW %s.%s(regionid, salescity, SalesCity) " +
            "AS SELECT region_id, sales_city, sales_city FROM cp.`region.json`",
        String.format("Duplicate column name [%s]", "SalesCity")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWithDuplicateColumnsInDef6() throws Exception {
    createViewErrorTestHelper(
        "CREATE VIEW %s.%s " +
            "AS SELECT t1.region_id, t2.region_id FROM cp.`region.json` t1 JOIN cp.`region.json` t2 " +
            "ON t1.region_id = t2.region_id LIMIT 1",
        String.format("Duplicate column name [%s]", "region_id")
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWithUniqueColsInFieldListDuplicateColsInQuery1() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        "(regionid1, regionid2)",
        "SELECT region_id, region_id FROM cp.`region.json` LIMIT 1",
        "SELECT * FROM TEST_SCHEMA.TEST_VIEW_NAME",
        baselineColumns("regionid1", "regionid2"),
        baselineRows(row(0L, 0L))
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWithUniqueColsInFieldListDuplicateColsInQuery2() throws Exception {
    testViewHelper(
        DFS_TMP_SCHEMA,
        "(regionid1, regionid2)",
        "SELECT t1.region_id, t2.region_id FROM cp.`region.json` t1 JOIN cp.`region.json` t2 " +
            "ON t1.region_id = t2.region_id LIMIT 1",
        "SELECT * FROM TEST_SCHEMA.TEST_VIEW_NAME",
        baselineColumns("regionid1", "regionid2"),
        baselineRows(row(0L, 0L))
    );
  }

  @Test // DRILL-2589
  @Category(UnlikelyTest.class)
  public void createViewWhenInEqualColumnCountInViewDefVsInViewQuery() throws Exception {
    createViewErrorTestHelper(
        "CREATE VIEW %s.%s(regionid, salescity) " +
            "AS SELECT region_id, sales_city, sales_region FROM cp.`region.json`",
        "view's field list and the view's query field list have different counts."
    );
  }

  @Test // DRILL-2589
  public void createViewWhenViewQueryColumnHasStarAndViewFiledListIsSpecified() throws Exception {
    createViewErrorTestHelper(
        "CREATE VIEW %s.%s(regionid, salescity) " +
            "AS SELECT region_id, * FROM cp.`region.json`",
        "view's query field list has a '*', which is invalid when view's field list is specified."
    );
  }

  private static void createViewErrorTestHelper(final String viewSql, final String expErrorMsg) throws Exception {
    final String createViewSql = String.format(viewSql, DFS_TMP_SCHEMA, "duplicateColumnsInViewDef");
    errorMsgTestHelper(createViewSql, expErrorMsg);
  }

  @Test // DRILL-2423
  @Category(UnlikelyTest.class)
  public void showProperMsgWhenDroppingNonExistentView() throws Exception{
    errorMsgTestHelper("DROP VIEW dfs.tmp.nonExistentView",
        "Unknown view [nonExistentView] in schema [dfs.tmp].");
  }

  @Test // DRILL-2423
  @Category(UnlikelyTest.class)
  public void showProperMsgWhenTryingToDropAViewInImmutableSchema() throws Exception{
    errorMsgTestHelper("DROP VIEW cp.nonExistentView",
        "Unable to create or drop objects. Schema [cp] is immutable.");
  }

  @Test // DRILL-2423
  @Category(UnlikelyTest.class)
  public void showProperMsgWhenTryingToDropANonViewTable() throws Exception{
    final String testTableName = "testTableShowErrorMsg";
    try {
      test("CREATE TABLE %s.%s AS SELECT c_custkey, c_nationkey from cp.`tpch/customer.parquet`",
        DFS_TMP_SCHEMA, testTableName);

      errorMsgTestHelper(String.format("DROP VIEW %s.%s", DFS_TMP_SCHEMA, testTableName),
          "[testTableShowErrorMsg] is not a VIEW in schema [dfs.tmp]");
    } finally {
      File tblPath = new File(dirTestWatcher.getDfsTestTmpDir(), testTableName);
      FileUtils.deleteQuietly(tblPath);
    }
  }

  @Test // DRILL-4673
  public void dropViewIfExistsWhenViewExists() throws Exception {
    final String existentViewName = generateViewName();

    // successful dropping of existent view
    createViewHelper(DFS_TMP_SCHEMA, existentViewName, DFS_TMP_SCHEMA, null,
        "SELECT c_custkey, c_nationkey from cp.`tpch/customer.parquet`");
    dropViewIfExistsHelper(DFS_TMP_SCHEMA, existentViewName, DFS_TMP_SCHEMA, true);
  }

  @Test // DRILL-4673
  public void dropViewIfExistsWhenViewDoesNotExist() throws Exception {
    final String nonExistentViewName = generateViewName();

    // dropping of non existent view without error
    dropViewIfExistsHelper(DFS_TMP_SCHEMA, nonExistentViewName, DFS_TMP_SCHEMA, false);
  }

  @Test // DRILL-4673
  public void dropViewIfExistsWhenItIsATable() throws Exception {
    final String tableName = "table_name";
    try{
      // dropping of non existent view without error if the table with such name is existed
      test("CREATE TABLE %s.%s as SELECT region_id, sales_city FROM cp.`region.json`",
        DFS_TMP_SCHEMA, tableName);
      dropViewIfExistsHelper(DFS_TMP_SCHEMA, tableName, DFS_TMP_SCHEMA, false);
    } finally {
      test("DROP TABLE IF EXISTS %s.%s ", DFS_TMP_SCHEMA, tableName);
    }
  }

  @Test
  public void selectFromViewCreatedOnCalcite1_4() throws Exception {
    testBuilder()
        .sqlQuery("select store_type from cp.`view/view_from_calcite_1_4.view.drill`")
        .unOrdered()
        .baselineColumns("store_type")
        .baselineValues("HeadQuarters")
        .go();
  }

  @Test
  public void testDropViewNameStartsWithSlash() throws Exception {
    String viewName = "view_name_starts_with_slash_drop";
    try {
      test("CREATE VIEW `%s`.`%s` AS SELECT * FROM cp.`region.json`", DFS_TMP_SCHEMA, viewName);
      testBuilder()
          .sqlQuery("DROP VIEW `%s`.`%s`", DFS_TMP_SCHEMA, "/" + viewName)
          .unOrdered()
          .baselineColumns("ok", "summary")
          .baselineValues(true,
              String.format("View [%s] deleted successfully from schema [%s].", viewName, DFS_TMP_SCHEMA))
          .go();
    } finally {
      test("DROP VIEW IF EXISTS `%s`.`%s`", DFS_TMP_SCHEMA, viewName);
    }
  }

  @Test
  public void testViewIsCreatedWithinWorkspace() throws Exception {
    String viewName = "view_created_within_workspace";
    try {
      test("CREATE VIEW `%s`.`%s` AS SELECT * FROM cp.`region.json`", DFS_TMP_SCHEMA, "/" + viewName);
      testBuilder()
          .sqlQuery("SELECT region_id FROM `%s`.`%s` LIMIT 1", DFS_TMP_SCHEMA, viewName)
          .unOrdered()
          .baselineColumns("region_id")
          .baselineValues(0L)
          .go();
    } finally {
      test("DROP VIEW IF EXISTS `%s`.`%s`", DFS_TMP_SCHEMA, viewName);
    }
  }

  @Test
  public void testViewIsFoundWithinWorkspaceWhenNameStartsWithSlash() throws Exception {
    String viewName = "view_found_within_workspace";
    try {
      test("CREATE VIEW `%s`.`%s` AS SELECT * FROM cp.`region.json`", DFS_TMP_SCHEMA, viewName);
      testBuilder()
          .sqlQuery("SELECT region_id FROM `%s`.`%s` LIMIT 1", DFS_TMP_SCHEMA, "/" + viewName)
          .unOrdered()
          .baselineColumns("region_id")
          .baselineValues(0L)
          .go();
    } finally {
      test("DROP VIEW IF EXISTS `%s`.`%s`", DFS_TMP_SCHEMA, viewName);
    }
  }

  @Test // DRILL-6944
  public void testSelectMapColumnOfNewlyCreatedView() throws Exception {
    try {
      test("CREATE VIEW dfs.tmp.`mapf_view` AS SELECT `mapf` FROM dfs.`avro/map_string_to_long.avro`");
      test("SELECT * FROM dfs.tmp.`mapf_view`");
      testBuilder()
          .sqlQuery("SELECT `mapf`['ki'] as ki FROM dfs.tmp.`mapf_view`")
          .unOrdered()
          .baselineColumns("ki")
          .baselineValues(1L)
          .go();
    } finally {
      test("DROP VIEW IF EXISTS dfs.tmp.`mapf_view`");
    }
  }

  @Test // DRILL-6944
  public void testMapTypeTreatedAsAnyInNewlyCreatedView() throws Exception {
    try {
      test("CREATE VIEW dfs.tmp.`mapf_view` AS SELECT `mapf` FROM dfs.`avro/map_string_to_long.avro`");
      testPlanWithAttributesMatchingPatterns("SELECT * FROM dfs.tmp.`mapf_view`", new String[]{
          "Screen : rowType = RecordType\\(ANY mapf\\)",
          "Project\\(mapf=\\[\\$0\\]\\) : rowType = RecordType\\(ANY mapf\\)",
          "Scan.*avro/map_string_to_long.avro.*rowType = RecordType\\(ANY mapf\\)"
      }, null);
    } finally {
      test("DROP VIEW IF EXISTS dfs.tmp.`mapf_view`");
    }
  }

  @Test // DRILL-6944
  public void testMapColumnOfOlderViewWithUntypedMap() throws Exception {
    test("SELECT * FROM cp.`view/vw_before_drill_6944.view.drill`");
    testBuilder()
        .sqlQuery("SELECT `mapf`['ki'] as ki FROM cp.`view/vw_before_drill_6944.view.drill`")
        .unOrdered()
        .baselineColumns("ki")
        .baselineValues(1L)
        .go();
  }

  @Test // DRILL-6944
  public void testMapTypeTreatedAsAnyInOlderViewWithUntypedMap() throws Exception {
    testPlanWithAttributesMatchingPatterns("SELECT * FROM cp.`view/vw_before_drill_6944.view.drill`", new String[]{
        "Screen : rowType = RecordType\\(ANY mapf\\)",
        "Project.mapf=.CAST\\(\\$0\\):ANY NOT NULL.*"
    }, null);
  }

  @Test
  @Category(UnlikelyTest.class)
  public void testViewContainingWithClauseAndUnionAllInDefinition() throws Exception{
    String viewName = "test_view";
    try {
      String viewDefinition = "create or replace view %s.`%s` as " +
          "with tbl_un as " +
          "(" +
          "(select c_name from cp.`tpch/customer.parquet` limit 1)" +
          "union all " +
          "(select c_name from cp.`tpch/customer.parquet` limit 1 offset 1) " +
          ") " +
          "select * from tbl_un";
      test(viewDefinition, DFS_TMP_SCHEMA, viewName);
      testBuilder()
          .sqlQuery("select * from %s.`%s`", DFS_TMP_SCHEMA, viewName)
          .unOrdered()
          .baselineColumns("c_name")
          .baselineValuesForSingleColumn("Customer#000000001", "Customer#000000002")
          .go();
    } finally {
      test("drop view if exists %s.`%s`", DFS_TMP_SCHEMA, viewName);
    }
  }
}
