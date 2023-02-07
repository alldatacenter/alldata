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
package org.apache.drill.exec.store.jdbc;

import java.util.HashMap;
import java.util.Map;
import org.apache.drill.categories.JdbcStorageTest;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.expr.fn.impl.DateUtility;

import org.apache.drill.exec.store.enumerable.plan.EnumMockPlugin;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.h2.tools.RunScript;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.FileReader;
import java.net.URL;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.TimeZone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * JDBC storage plugin tests against H2.
 */
@Category(JdbcStorageTest.class)
public class TestJdbcPluginWithH2IT extends ClusterTest {

  private static final String TABLE_PATH = "jdbcmulti/";
  private static final String TABLE_NAME = String.format("%s.`%s`", StoragePluginTestUtils.DFS_PLUGIN_NAME, TABLE_PATH);
  private static TimeZone defaultTimeZone;

  @BeforeClass
  public static void init() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    // Force timezone to UTC for these tests.
    defaultTimeZone = TimeZone.getDefault();
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    dirTestWatcher.copyResourceToRoot(Paths.get(TABLE_PATH));
    Class.forName("org.h2.Driver");
    String connString = "jdbc:h2:" + dirTestWatcher.getTmpDir().getCanonicalPath();
    URL scriptFile = TestJdbcPluginWithH2IT.class.getClassLoader().getResource("h2-test-data.sql");
    assertNotNull("Script for test tables generation 'h2-test-data.sql' cannot be found in test resources", scriptFile);
    try (Connection connection = DriverManager.getConnection(connString, "root", "root");
         FileReader fileReader = new FileReader(scriptFile.getFile())) {
      RunScript.execute(connection, fileReader);
    }
    Map<String, Object> sourceParameters =  new HashMap<>();
    sourceParameters.put("minimumIdle", 1);
    JdbcStorageConfig jdbcStorageConfig = new JdbcStorageConfig("org.h2.Driver", connString,
        "root", "root", true, false, sourceParameters, null, 10000);
    jdbcStorageConfig.setEnabled(true);
    cluster.defineStoragePlugin("h2", jdbcStorageConfig);
    cluster.defineStoragePlugin("h2o", jdbcStorageConfig);

    EnumMockPlugin.EnumMockStoragePluginConfig config = new EnumMockPlugin.EnumMockStoragePluginConfig();
    config.setEnabled(true);
    cluster.defineStoragePlugin("mocked_enum", config);
  }

  @AfterClass
  public static void cleanUp() {
    TimeZone.setDefault(defaultTimeZone);
  }

  @Test
  public void testCrossSourceMultiFragmentJoin() throws Exception {
    try {
      client.alterSession(ExecConstants.SLICE_TARGET, 1);
      run("select x.person_id, y.salary from h2.tmp.drill_h2_test.person x "
              + "join %s y on x.person_id = y.person_id ", TABLE_NAME);
    } finally {
      client.resetSession(ExecConstants.SLICE_TARGET);
    }
  }

  @Test
  public void validateResult() throws Exception {
    testBuilder()
        .sqlQuery(
            "select person_id, first_name, last_name, address, city, state, zip, json, bigint_field, smallint_field, " +
                "numeric_field, boolean_field, double_field, float_field, real_field, time_field, timestamp_field, " +
                "date_field, clob_field from h2.tmp.`drill_h2_test`.person")
        .ordered()
        .baselineColumns("person_id", "first_name", "last_name", "address", "city", "state", "zip", "json",
            "bigint_field", "smallint_field", "numeric_field", "boolean_field", "double_field", "float_field",
            "real_field", "time_field", "timestamp_field", "date_field", "clob_field")
        .baselineValues(1, "first_name_1", "last_name_1", "1401 John F Kennedy Blvd",   "Philadelphia",     "PA", 19107,
            "{ a : 5, b : 6 }", 123456L, 1, 10.01, false, 1.0, 1.1f, 111.00,
            DateUtility.parseLocalTime("13:00:01.0"), DateUtility.parseLocalDateTime("2012-02-29 13:00:01.0"),
            DateUtility.parseLocalDate("2012-02-29"), "some clob data 1")
        .baselineValues(2, "first_name_2", "last_name_2", "One Ferry Building", "San Francisco", "CA", 94111,
            "{ foo : \"abc\" }", 95949L, 2, 20.02, true, 2.0, 2.1f, 222.00,
            DateUtility.parseLocalTime("23:59:59.0"),  DateUtility.parseLocalDateTime("1999-09-09 23:59:59.0"),
            DateUtility.parseLocalDate("1999-09-09"), "some more clob data")
        .baselineValues(3, "first_name_3", "last_name_3", "176 Bowery", "New York", "NY", 10012, "{ z : [ 1, 2, 3 ] }",
            45456L, 3, 30.04, true, 3.0, 3.1f, 333.00, DateUtility.parseLocalTime("11:34:21.0"),
            DateUtility.parseLocalDateTime("2011-10-30 11:34:21.0"), DateUtility.parseLocalDate("2011-10-30"), "clobber")
        .baselineValues(4, null, null, "2 15th St NW", "Washington", "DC", 20007, "{ z : { a : 1, b : 2, c : 3 } }",
            -67L, 4, 40.04, false, 4.0, 4.1f, 444.00, DateUtility.parseLocalTime("16:00:01.0"),
            DateUtility.parseLocalDateTime("2015-06-01 16:00:01.0"),  DateUtility.parseLocalDate("2015-06-01"), "xxx")
        .baselineValues(5, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
            null, null, null, null)
        .go();
  }

  @Test
  public void pushDownJoin() throws Exception {
    String query = "select x.person_id from (select person_id from h2.tmp.drill_h2_test.person) x "
            + "join (select person_id from h2.tmp.drill_h2_test.person) y on x.person_id = y.person_id ";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .exclude("Join")
        .match();
  }

  @Test
  public void pushDownJoinAndFilterPushDown() throws Exception {
    String query = "select * from \n" +
        "h2.tmp.drill_h2_test.person e\n" +
        "INNER JOIN \n" +
        "h2.tmp.drill_h2_test.person s\n" +
        "ON e.FIRST_NAME = s.FIRST_NAME\n" +
        "WHERE e.LAST_NAME > 'hello'";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .exclude("Join")
        .exclude("Filter")
        .match();
  }

  @Test
  public void pushDownAggregation() throws Exception {
    String query = "select count(*) from h2.tmp.drill_h2_test.person";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .exclude("Aggregate")
        .match();
  }

  @Test
  public void pushDownDoubleJoinAndFilter() throws Exception {
    String query = "select * from \n" +
        "h2.tmp.drill_h2_test.person e\n" +
        "INNER JOIN \n" +
        "h2.tmp.drill_h2_test.person s\n" +
        "ON e.person_ID = s.person_ID\n" +
        "INNER JOIN \n" +
        "h2.tmp.drill_h2_test.person ed\n" +
        "ON e.person_ID = ed.person_ID\n" +
        "WHERE s.first_name > 'abc' and ed.first_name > 'efg'";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .exclude("Join")
        .exclude("Filter")
        .match();
  }

  @Test // DRILL-7340
  public void twoPluginsPredicatesPushDown() throws Exception {
    String query = "SELECT * " +
        "FROM h2.tmp.drill_h2_test.person l " +
        "INNER JOIN h2o.tmp.drill_h2_test.person r " +
        "ON l.person_id = r.person_id " +
        "WHERE l.first_name = 'first_name_1' AND r.last_name = 'last_name_1'";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .exclude("Filter")
        .match();
  }

  @Test
  public void showTablesDefaultSchema() throws Exception {
    run("use h2.tmp.drill_h2_test");
    assertEquals(1, queryBuilder().sql("show tables like 'PERSON'").run().recordCount());

    // check table names case insensitivity
    assertEquals(1, queryBuilder().sql("show tables like 'person'").run().recordCount());
  }

  @Test
  public void describe() throws Exception {
    run("use h2.tmp.drill_h2_test");
    assertEquals(19, queryBuilder().sql("describe PERSON").run().recordCount());

    // check table names case insensitivity
    assertEquals(19, queryBuilder().sql("describe person").run().recordCount());
  }

  @Test
  public void ensureDrillFunctionsAreNotPushedDown() throws Exception {
    // This should verify that we're not trying to push CONVERT_FROM into the JDBC storage plugin. If were pushing
    // this function down, the SQL query would fail.
    run("select CONVERT_FROM(JSON, 'JSON') from h2.tmp.drill_h2_test.person where person_ID = 4");
  }

  @Test
  public void pushDownFilter() throws Exception {
    String query = "select * from h2.tmp.drill_h2_test.person where person_ID = 1";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .exclude("Filter")
        .match();
  }

  @Test
  public void testCaseInsensitiveTableNames() throws Exception {
    assertEquals(5, queryBuilder().sql("select * from h2.tmp.drill_h2_test.PeRsOn").run().recordCount());
    assertEquals(5, queryBuilder().sql("select * from h2.tmp.drill_h2_test.PERSON").run().recordCount());
    assertEquals(5, queryBuilder().sql("select * from h2.tmp.drill_h2_test.person").run().recordCount());
  }

  @Test
  public void testJdbcStoragePluginSerDe() throws Exception {
    String query = "select * from h2.tmp.drill_h2_test.PeRsOn";

    String plan = queryBuilder().sql(query).explainJson();
    assertEquals(5, queryBuilder().physical(plan).run().recordCount());
  }

  @Test // DRILL-7415
  public void showTablesForPluginDefaultSchema() throws Exception {
    run("USE h2");
    String sql = "SHOW TABLES";
    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
        .baselineValues("h2.tmp.drill_h2_test_1", "PERSON")
        .go();
  }

  @Test // DRILL-7415
  public void showTablesForInformationSchema() throws Exception {
    run("USE h2.tmp.`INFORMATION_SCHEMA`");
    String sql = "SHOW TABLES";
    testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .expectsNumRecords(35)
        .csvBaselineFile("h2_information_schema_tables.csv")
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
        .go();
  }

  @Test
  public void testJdbcTableTypes() throws Exception {
    String query = "select distinct table_type from information_schema.`tables`";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("table_type")
        .baselineValuesForSingleColumn("SYSTEM TABLE", "TABLE", "VIEW", "OTHER")
        .go();
  }

  @Test // DRILL-7972
  public void testJdbcIntermConvRuleConvention() throws Exception {
    String query = "select t1.person_ID from h2.tmp.drill_h2_test.person t1 " +
        "join mocked_enum.mock_enum_table t2 on t1.person_ID = t2.a";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .include("mocked_enum")
        .match();
  }
}
