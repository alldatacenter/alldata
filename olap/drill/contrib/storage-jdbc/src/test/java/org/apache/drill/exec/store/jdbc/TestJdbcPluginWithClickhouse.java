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
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.fn.impl.DateUtility;
import org.apache.drill.exec.physical.rowSet.DirectRowSet;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.testcontainers.containers.ClickHouseContainer;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

/**
 * JDBC storage plugin tests against Clickhouse.
 */
@Category(JdbcStorageTest.class)
public class TestJdbcPluginWithClickhouse extends ClusterTest {
  private static final String DOCKER_IMAGE_CLICKHOUSE_X86 = "yandex" +
    "/clickhouse-server:21.8.4.51";
  private static final String DOCKER_IMAGE_CLICKHOUSE_ARM = "lunalabsltd" +
    "/clickhouse-server:21.7.2.7-arm";
  private static JdbcDatabaseContainer<?> jdbcContainer;

  @BeforeClass
  public static void initClickhouse() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    String osName = System.getProperty("os.name").toLowerCase();
    DockerImageName imageName;
    if (osName.startsWith("linux") && "aarch64".equals(System.getProperty("os.arch"))) {
      imageName = DockerImageName.parse(DOCKER_IMAGE_CLICKHOUSE_ARM)
        .asCompatibleSubstituteFor("yandex/clickhouse-server");
    } else {
      imageName = DockerImageName.parse(DOCKER_IMAGE_CLICKHOUSE_X86);
    }

    jdbcContainer = new ClickHouseContainer(imageName)
      .withInitScript("clickhouse-test-data.sql");
    jdbcContainer.start();

    JdbcStorageConfig jdbcStorageConfig =
      new JdbcStorageConfig("ru.yandex.clickhouse.ClickHouseDriver",
        jdbcContainer.getJdbcUrl(), jdbcContainer.getUsername(), null,
        true, false,null, null, 0);
    jdbcStorageConfig.setEnabled(true);
    cluster.defineStoragePlugin("clickhouse", jdbcStorageConfig);
  }

  @AfterClass
  public static void stopClickhouse() {
    if (jdbcContainer != null) {
      jdbcContainer.stop();
    }
  }

  @Test
  public void validateResult() throws Exception {
    testBuilder()
        .sqlQuery(
            "select person_id, first_name, last_name, address, city, state, zip, " +
              "json, bigint_field, smallint_field, decimal_field, boolean_field, " +
              "double_field, float_field, date_field, datetime_field, enum_field " +
            "from clickhouse.`default`.person order by person_id")
        .ordered()
        .baselineColumns("person_id", "first_name", "last_name", "address",
          "city", "state", "zip", "json", "bigint_field", "smallint_field",
          "decimal_field", "boolean_field", "double_field", "float_field",
          "date_field", "datetime_field", "enum_field")
        .baselineValues(1, "first_name_1", "last_name_1", "1401 John F Kennedy Blvd",
          "Philadelphia", "PA", 19107, "{ a : 5, b : 6 }", 123456789L, 1,
          new BigDecimal("123.32"), 0, 1.0, 1.1,
          DateUtility.parseLocalDate("2012-02-29"),
          DateUtility.parseLocalDateTime("2012-02-29 13:00:01.0"), "XXX")
        .baselineValues(2, "first_name_2", "last_name_2", "One Ferry Building",
          "San Francisco", "CA", 94111, "{ z : [ 1, 2, 3 ] }", 45456767L, 3,
          null, 1, 3.0, 3.1,
          DateUtility.parseLocalDate("2011-10-30"),
          DateUtility.parseLocalDateTime("2011-10-30 11:34:21.0"), "YYY")
        .baselineValues(3, "first_name_3", "last_name_3", "176 Bowery",
          "New York", "NY", 10012, "{ [ a, b, c ] }", 123090L, -3,
          null, 0, 5.0, 5.1,
          DateUtility.parseLocalDate("2015-06-01"),
          DateUtility.parseLocalDateTime("2015-09-22 15:46:10.0"), "ZZZ")
        .baselineValues(4, null, null, null, null, null, null, null, null, null,
            null, null, null, null, null, null, "XXX")
        .go();
  }

  @Test
  public void pushDownJoin() throws Exception {
    String query = "select x.person_id from (select person_id from clickhouse.`default`.person) x "
            + "join (select person_id from clickhouse.`default`.person) y on x.person_id = y.person_id";
    queryBuilder()
        .sql(query)
        .planMatcher()
        .exclude("Join")
        .match();
  }

  @Test
  public void pushDownJoinAndFilterPushDown() throws Exception {
    String query = "select * from " +
            "clickhouse.`default`.person e " +
            "INNER JOIN " +
            "clickhouse.`default`.person s " +
            "ON e.first_name = s.first_name " +
            "WHERE e.last_name > 'hello'";

    queryBuilder()
        .sql(query)
        .planMatcher()
        .exclude("Join", "Filter")
        .match();
  }

  @Test
  public void pushDownAggWithDecimal() throws Exception {
    String query = "SELECT sum(decimal_field * smallint_field) AS `order_total`\n" +
        "FROM clickhouse.`default`.person e";

    DirectRowSet results = queryBuilder().sql(query).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
        .addNullable("order_total", TypeProtos.MinorType.VARDECIMAL, 38, 2)
        .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
        .addRow(123.32)
        .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testPhysicalPlanSubmission() throws Exception {
    String query = "select * from clickhouse.`default`.person";
    String plan = queryBuilder().sql(query).explainJson();
    assertEquals(4, queryBuilder().physical(plan).run().recordCount());
  }

  @Test
  public void emptyOutput() {
    String query = "select * from clickhouse.`default`.person e limit 0";

    testBuilder()
        .sqlQuery(query)
        .expectsEmptyResultSet();
  }

  @Test
  public void testExpressionsWithoutAlias() throws Exception {
    String query = "select count(*), 1+1+2+3+5+8+13+21+34, (1+sqrt(5))/2\n" +
        "from clickhouse.`default`.person";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("EXPR$0", "EXPR$1", "EXPR$2")
        .baselineValues(4L, 88L, 1.618033988749895)
        .go();
  }

  @Test
  public void testExpressionsWithoutAliasesPermutations() throws Exception {
    String query = "select EXPR$1, EXPR$0, EXPR$2\n" +
        "from (select 1+1+2+3+5+8+13+21+34, (1+sqrt(5))/2, count(*) from clickhouse.`default`.person)";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("EXPR$1", "EXPR$0", "EXPR$2")
        .baselineValues(1.618033988749895, 88L, 4L)
        .go();
  }

  @Test
  public void testExpressionsWithAliases() throws Exception {
    String query = "select person_id as ID, 1+1+2+3+5+8+13+21+34 as FIBONACCI_SUM, (1+sqrt(5))/2 as golden_ratio\n" +
        "from clickhouse.`default`.person limit 2";

    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("ID", "FIBONACCI_SUM", "golden_ratio")
        .baselineValues(1, 88L, 1.618033988749895)
        .baselineValues(2, 88L, 1.618033988749895)
        .go();
  }

  @Test
  public void testJoinStar() throws Exception {
    String query = "select * from (select person_id from clickhouse.`default`.person) t1 join " +
        "(select person_id from clickhouse.`default`.person) t2 on t1.person_id = t2.person_id";

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
        "select person_id from clickhouse.`default`.person t1\n" +
            "where exists (" +
                "select person_id from clickhouse.`default`.person\n" +
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
        "where table_schema like 'clickhouse%'";
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("table_type")
        .baselineValuesForSingleColumn("TABLE", "VIEW")
        .go();
  }

  @Test
  public void testLimitPushDown() throws Exception {
    String query = "select person_id, first_name, last_name from clickhouse.`default`.person limit 100";
    queryBuilder()
        .sql(query)
        .planMatcher()
        .include("Jdbc\\(.*LIMIT 100")
        .exclude("Limit\\(")
        .match();
  }

  @Test
  public void testLimitPushDownWithOrderBy() throws Exception {
    String query = "select person_id from clickhouse.`default`.person order by first_name limit 100";
    queryBuilder()
        .sql(query)
        .planMatcher()
        .include("Jdbc\\(.*ORDER BY `first_name`.*LIMIT 100")
        .exclude("Limit\\(")
        .match();
  }

  @Test
  public void testLimitPushDownWithOffset() throws Exception {
    String query = "select person_id, first_name from clickhouse.`default`.person limit 100 offset 10";
    queryBuilder()
        .sql(query)
        .planMatcher()
        .include("Jdbc\\(.*LIMIT 10, 100")
        .exclude("Limit\\(")
        .match();
  }

  @Test
  public void testLimitPushDownWithConvertFromJson() throws Exception {
    String query = "select convert_fromJSON(first_name)['ppid'] from clickhouse.`default`.person LIMIT 100";
    queryBuilder()
        .sql(query)
        .planMatcher()
        .include("Jdbc\\(.*LIMIT 100")
        .exclude("Limit\\(")
        .match();
  }
}
