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

import org.apache.drill.categories.JdbcStorageTest;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.testcontainers.containers.MSSQLServerContainer;

import java.math.BigDecimal;
import java.util.TimeZone;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * JDBC storage plugin tests against MSSQL. Note that there is no mssql container
 * available on aarch64 so these tests must be disabled on that arch.
 */
@Category(JdbcStorageTest.class)
public class TestJdbcPluginWithMSSQL extends ClusterTest {

  private static MSSQLServerContainer<?> jdbcContainer;

  @BeforeClass
  public static void initMSSQL() throws Exception {
    Assume.assumeTrue(System.getProperty("os.arch").matches("(amd64|x86_64)"));

    startCluster(ClusterFixture.builder(dirTestWatcher));
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

    jdbcContainer = new MSSQLServerContainer<>()
      .withPassword("B!llyG0atGruff") // must meet mssql's complexity requirements

      .withInitScript("mssql-test-data.ms.sql")
      .withUrlParam("trustServerCertificate", "true")
      .acceptLicense();

    jdbcContainer.start();

    Map<String, String> credentials = ImmutableMap.<String, String>builder()
        .put("username", jdbcContainer.getUsername())
        .put("password", jdbcContainer.getPassword())
        .build();
    PlainCredentialsProvider credentialsProvider = new PlainCredentialsProvider(credentials);

    // To test for unimplemented Connection::getSchema (DRILL-8227), we use the jTDS driver
    // in this class. The jTDS driver is only at JDBC v3 and so does not support isValid.
    // To satisfy Hikari we must therefore specify a connection test query.
    Map<String, Object> sourceParms = ImmutableMap.<String, Object>builder()
      .put("connectionTestQuery", "select 1")
      .build();

    JdbcStorageConfig jdbcStorageConfig = new JdbcStorageConfig(
      "net.sourceforge.jtds.jdbc.Driver",
      jdbcContainer.getJdbcUrl().replaceFirst("jdbc:", "jdbc:jtds:"), // mangle the reported URL for jTDS
      null,
      null,
      true,
      false,
      sourceParms,
      credentialsProvider,
      100000
    );
    jdbcStorageConfig.setEnabled(true);
    cluster.defineStoragePlugin("mssql", jdbcStorageConfig);
  }

  @AfterClass
  public static void stopMSSQL() {
    if (jdbcContainer != null) {
      jdbcContainer.stop();
    }
  }

  @Test
  public void validateResult() throws Exception {
    String sql = "SELECT person_id, first_name, last_name, address, city, state, zip, " +
      "json, bigint_field, smallint_field, decimal_field, bit_field, " +
      "double_field, float_field, datetime_field " +
      "FROM mssql.dbo.person ORDER BY person_id";

    DirectRowSet results = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("person_id", MinorType.INT, 10)
      .addNullable("first_name", MinorType.VARCHAR, 38)
      .addNullable("last_name", MinorType.VARCHAR, 38)
      .addNullable("address", MinorType.VARCHAR, 38)
      .addNullable("city", MinorType.VARCHAR, 38)
      .addNullable("state", MinorType.VARCHAR, 2)
      .addNullable("zip", MinorType.INT, 10)
      .addNullable("json", MinorType.VARCHAR, 38)
      .addNullable("bigint_field", MinorType.BIGINT, 19)
      .addNullable("smallint_field", MinorType.INT, 5)
      .addNullable("decimal_field", MinorType.VARDECIMAL, 15, 2)
      .addNullable("bit_field", MinorType.BIT, 1)
      .addNullable("double_field", MinorType.FLOAT8, 15)
      .addNullable("float_field", MinorType.FLOAT8, 15)
      // TODO: these two types are mapped to VARCHARS instead of date/time types
      //.addNullable("date_field", MinorType.VARCHAR, 10)
      //.addNullable("datetime2_field", MinorType.TIMESTAMP, 23, 3)
      .addNullable("datetime_field", MinorType.TIMESTAMP, 23, 3)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(1, "first_name_1", "last_name_1", "1401 John F Kennedy Blvd",
        "Philadelphia", "PA", 19107, "{ a : 5, b : 6 }", 123456789L, 1,
        new BigDecimal("123.32"), 1, 1.0, 1.1,
        1330520401000L)
      .addRow(2, "first_name_2", "last_name_2", "One Ferry Building",
        "San Francisco", "CA", 94111, "{ z : [ 1, 2, 3 ] }", 45456767L, 3,
        null, 0, 3.0, 3.1,
        1319974461000L)
      .addRow(3, "first_name_3", "last_name_3", "176 Bowery",
        "New York", "NY", 10012, "{ [ a, b, c ] }", 123090L, -3,
        null, 1, 5.0, 5.1,
        1442936770000L)
      .addRow(4, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null)
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void pushDownJoin() throws Exception {
    String query = "select x.person_id from (select person_id from mssql.dbo.person) x "
      + "join (select person_id from mssql.dbo.person) y on x.person_id = y.person_id";
    queryBuilder()
      .sql(query)
      .planMatcher()
      .exclude("Join")
      .match();
  }

  @Test
  public void pushDownJoinAndFilterPushDown() throws Exception {
    String query = "select * from " +
      "mssql.dbo.person e " +
      "INNER JOIN " +
      "mssql.dbo.person s " +
      "ON e.first_name = s.first_name " +
      "WHERE e.last_name > 'hello'";

    queryBuilder()
      .sql(query)
      .planMatcher()
      .exclude("Join", "Filter")
      .match();
  }

  @Test
  public void testPhysicalPlanSubmission() throws Exception {
    String query = "select * from mssql.dbo.person";
    String plan = queryBuilder().sql(query).explainJson();
    assertEquals(4, queryBuilder().physical(plan).run().recordCount());
  }

  @Test
  public void emptyOutput() {
    String query = "select * from mssql.dbo.person e limit 0";

    testBuilder()
      .sqlQuery(query)
      .expectsEmptyResultSet();
  }

  @Test
  public void testExpressionsWithoutAlias() throws Exception {
    String sql = "select count(*), 1+1+2+3+5+8+13+21+34, (1+sqrt(5))/2\n" +
      "from mssql.dbo.person";

    DirectRowSet results = queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("EXPR$0", MinorType.INT, 10)
      .addNullable("EXPR$1", MinorType.INT, 10)
      .addNullable("EXPR$2", MinorType.FLOAT8, 15)
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(4L, 88L, 1.618033988749895)
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testExpressionsWithoutAliasesPermutations() throws Exception {
    String query = "select EXPR$1, EXPR$0, EXPR$2\n" +
      "from (select 1+1+2+3+5+8+13+21+34, (1+sqrt(5))/2, count(*) from mssql.dbo.person)";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("EXPR$1", "EXPR$0", "EXPR$2")
      .baselineValues(1.618033988749895, 88, 4)
      .go();
  }

  @Test
  public void testExpressionsWithAliases() throws Exception {
    String query = "SELECT person_id AS ID, 1+1+2+3+5+8+13+21+34 as FIBONACCI_SUM, (1+sqrt(5))/2 as golden_ratio\n" +
      "FROM mssql.dbo.person limit 2";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("ID", "FIBONACCI_SUM", "golden_ratio")
      .baselineValues(1, 88, 1.618033988749895)
      .baselineValues(2, 88, 1.618033988749895)
      .go();
  }

  @Test
  public void testJoinStar() throws Exception {
    String query = "select * from (select person_id from mssql.dbo.person) t1 join " +
      "(select person_id from mssql.dbo.person) t2 on t1.person_id = t2.person_id";

    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("person_id", "person_id0")
      .baselineValues(1, 1)
      .baselineValues(2, 2)
      .baselineValues(3, 3)
      .baselineValues(4, 4)
      .go();
  }

  @Test
  public void testSemiJoin() throws Exception {
    String query =
      "select person_id from mssql.dbo.person t1\n" +
        "where exists (" +
        "select person_id from mssql.dbo.person\n" +
        "where t1.person_id = person_id)";
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("person_id")
      .baselineValuesForSingleColumn(1, 2, 3, 4)
      .go();
  }

  @Test
  public void testInformationSchemaViews() throws Exception {
    String query = "select * from information_schema.`views`";
    run(query);
  }

  @Test
  public void testJdbcTableTypes() throws Exception {
    String query = "select distinct table_type from information_schema.`tables` " +
      "where table_schema like 'mssql%'";
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("table_type")
      .baselineValuesForSingleColumn("TABLE", "VIEW")
      .go();
  }

  // DRILL-8090
  @Test
  public void testLimitPushDown() throws Exception {
    String query = "select person_id, first_name, last_name from mssql.dbo.person limit 100";
    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("Jdbc\\(.*SELECT TOP \\(100\\)")
      .exclude("Limit\\(")
      .match();
  }

  @Test
  public void testLimitPushDownWithOrderBy() throws Exception {
    String query = "select person_id from mssql.dbo.person order by first_name limit 100";
    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("Jdbc\\(.*SELECT TOP \\(100\\).*ORDER BY.*\"first_name\"")
      .exclude("Limit\\(")
      .match();
  }

  @Test
  public void testLimitPushDownWithOffset() throws Exception {
    String query = "select person_id, first_name from mssql.dbo.person limit 100 offset 10";
    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("Jdbc\\(.*SELECT TOP \\(110\\)")
      .include("Limit\\(")
      .match();
  }

  @Test
  public void testLimitPushDownWithConvertFromJson() throws Exception {
    String query = "select convert_fromJSON(first_name)['ppid'] from mssql.dbo.person LIMIT 100";
    queryBuilder()
      .sql(query)
      .planMatcher()
      .include("Jdbc\\(.*SELECT TOP \\(100\\)")
      .exclude("Limit\\(")
      .match();
  }
}
