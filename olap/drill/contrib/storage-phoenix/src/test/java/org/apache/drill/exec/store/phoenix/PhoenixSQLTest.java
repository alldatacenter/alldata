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
package org.apache.drill.exec.store.phoenix;

import static org.junit.Assert.assertEquals;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.JVM)
@Category({ SlowTest.class, RowSetTests.class })
public class PhoenixSQLTest extends PhoenixBaseTest {

  @Test
  public void testStarQuery() throws Exception {
    String sql = "select * from phoenix123.v1.nation";
    queryBuilder().sql(sql).run();
  }

  @Test
  public void testExplicitQuery() throws Exception {
    String sql = "select n_nationkey, n_regionkey, n_name from phoenix123.v1.nation";
    QueryBuilder builder = queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("n_nationkey", MinorType.BIGINT)
        .addNullable("n_regionkey", MinorType.BIGINT)
        .addNullable("n_name", MinorType.VARCHAR)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(0, 0, "ALGERIA")
        .addRow(1, 1, "ARGENTINA")
        .addRow(2, 1, "BRAZIL")
        .addRow(3, 1, "CANADA")
        .addRow(4, 4, "EGYPT")
        .addRow(5, 0, "ETHIOPIA")
        .addRow(6, 3, "FRANCE")
        .addRow(7, 3, "GERMANY")
        .addRow(8, 2, "INDIA")
        .addRow(9, 2, "INDONESIA")
        .addRow(10, 4, "IRAN")
        .addRow(11, 4, "IRAQ")
        .addRow(12, 2, "JAPAN")
        .addRow(13, 4, "JORDAN")
        .addRow(14, 0, "KENYA")
        .addRow(15, 0, "MOROCCO")
        .addRow(16, 0, "MOZAMBIQUE")
        .addRow(17, 1, "PERU")
        .addRow(18, 2, "CHINA")
        .addRow(19, 3, "ROMANIA")
        .addRow(20, 4, "SAUDI ARABIA")
        .addRow(21, 2, "VIETNAM")
        .addRow(22, 3, "RUSSIA")
        .addRow(23, 3, "UNITED KINGDOM")
        .addRow(24, 1, "UNITED STATES")
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testLimitPushdown() throws Exception {
    String sql = "select n_name, n_regionkey from phoenix123.v1.nation limit 20 offset 10";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    builder.planMatcher()
        .exclude("Limit")
        .include("OFFSET .* ROWS FETCH NEXT .* ROWS ONLY")
        .match();

    assertEquals(15, sets.rowCount());

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("n_name", MinorType.VARCHAR)
        .addNullable("n_regionkey", MinorType.BIGINT)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("IRAN", 4)
        .addRow("IRAQ", 4)
        .addRow("JAPAN", 2)
        .addRow("JORDAN", 4)
        .addRow("KENYA", 0)
        .addRow("MOROCCO", 0)
        .addRow("MOZAMBIQUE", 0)
        .addRow("PERU", 1)
        .addRow("CHINA", 2)
        .addRow("ROMANIA", 3)
        .addRow("SAUDI ARABIA", 4)
        .addRow("VIETNAM", 2)
        .addRow("RUSSIA", 3)
        .addRow("UNITED KINGDOM", 3)
        .addRow("UNITED STATES", 1)
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testFilterPushdown() throws Exception {
    String sql = "select * from phoenix123.v1.region where r_name = 'ASIA'";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    builder.planMatcher()
        .exclude("Filter")
        .include("WHERE .* = 'ASIA'")
        .match();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("R_REGIONKEY", MinorType.BIGINT)
        .addNullable("R_NAME", MinorType.VARCHAR)
        .addNullable("R_COMMENT", MinorType.VARCHAR)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(2, "ASIA", "ges. thinly even pinto beans ca")
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "select count(*) as total from phoenix123.v1.nation";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match", 25, cnt);
  }

  @Test
  public void testJoinPushdown() throws Exception {
    String sql = "select a.n_name, b.r_name from phoenix123.v1.nation a join phoenix123.v1.region b "
        + "on a.n_regionkey = b.r_regionkey";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    builder.planMatcher()
        .exclude("Join")
        .include("Phoenix\\(.* INNER JOIN")
        .match();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("n_name", MinorType.VARCHAR)
        .addNullable("r_name", MinorType.VARCHAR)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("ALGERIA", "AFRICA")
        .addRow("ARGENTINA", "AMERICA")
        .addRow("BRAZIL", "AMERICA")
        .addRow("CANADA", "AMERICA")
        .addRow("EGYPT", "MIDDLE EAST")
        .addRow("ETHIOPIA", "AFRICA")
        .addRow("FRANCE", "EUROPE")
        .addRow("GERMANY", "EUROPE")
        .addRow("INDIA", "ASIA")
        .addRow("INDONESIA", "ASIA")
        .addRow("IRAN", "MIDDLE EAST")
        .addRow("IRAQ", "MIDDLE EAST")
        .addRow("JAPAN", "ASIA")
        .addRow("JORDAN", "MIDDLE EAST")
        .addRow("KENYA", "AFRICA")
        .addRow("MOROCCO", "AFRICA")
        .addRow("MOZAMBIQUE", "AFRICA")
        .addRow("PERU", "AMERICA")
        .addRow("CHINA", "ASIA")
        .addRow("ROMANIA", "EUROPE")
        .addRow("SAUDI ARABIA", "MIDDLE EAST")
        .addRow("VIETNAM", "ASIA")
        .addRow("RUSSIA", "EUROPE")
        .addRow("UNITED KINGDOM", "EUROPE")
        .addRow("UNITED STATES", "AMERICA")
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testCrossJoin() throws Exception {
    String sql = "select a.n_name, b.n_comment from phoenix123.v1.nation a cross join phoenix123.v1.nation b";
    client.alterSession(PlannerSettings.NLJOIN_FOR_SCALAR.getOptionName(), false);
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    builder.planMatcher().exclude("Join").match();

    assertEquals("Counts should match", 625, sets.rowCount());
    sets.clear();
  }

  @Ignore("use the remote query server directly without minicluster")
  @Test
  public void testJoinWithFilterPushdown() throws Exception {
    String sql = "select 10 as DRILL, a.n_name, b.r_name from phoenix123.v1.nation a join phoenix123.v1.region b "
        + "on a.n_regionkey = b.r_regionkey where b.r_name = 'ASIA'";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    builder.planMatcher()
        .exclude("Join")
        .exclude("Filter")
        .include("Phoenix\\(.* INNER JOIN .* WHERE")
        .match();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("DRILL", MinorType.INT)
        .addNullable("n_name", MinorType.VARCHAR)
        .addNullable("r_name", MinorType.VARCHAR)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(10, "INDIA", "ASIA")
        .addRow(10, "INDONESIA", "ASIA")
        .addRow(10, "JAPAN", "ASIA")
        .addRow(10, "CHINA", "ASIA")
        .addRow(10, "VIETNAM", "ASIA")
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testGroupByPushdown() throws Exception {
    String sql = "select n_regionkey, count(1) as total from phoenix123.v1.nation group by n_regionkey";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    builder.planMatcher()
        .exclude("Aggregate")
        .include("Phoenix\\(.* GROUP BY")
        .match();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("n_regionkey", MinorType.BIGINT)
        .addNullable("total", MinorType.BIGINT)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(0, 5)
        .addRow(1, 5)
        .addRow(2, 5)
        .addRow(3, 5)
        .addRow(4, 5)
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testDistinctPushdown() throws Exception {
    String sql = "select distinct n_name from phoenix123.v1.nation"; // auto convert to group-by
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    builder.planMatcher()
        .exclude("Aggregate")
        .include("Phoenix\\(.* GROUP BY \"N_NAME")
        .match();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("n_name", MinorType.VARCHAR)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow("ALGERIA")
        .addRow("ARGENTINA")
        .addRow("BRAZIL")
        .addRow("CANADA")
        .addRow("CHINA")
        .addRow("EGYPT")
        .addRow("ETHIOPIA")
        .addRow("FRANCE")
        .addRow("GERMANY")
        .addRow("INDIA")
        .addRow("INDONESIA")
        .addRow("IRAN")
        .addRow("IRAQ")
        .addRow("JAPAN")
        .addRow("JORDAN")
        .addRow("KENYA")
        .addRow("MOROCCO")
        .addRow("MOZAMBIQUE")
        .addRow("PERU")
        .addRow("ROMANIA")
        .addRow("RUSSIA")
        .addRow("SAUDI ARABIA")
        .addRow("UNITED KINGDOM")
        .addRow("UNITED STATES")
        .addRow("VIETNAM")
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }

  @Test
  public void testHavingPushdown() throws Exception {
    String sql = "select n_regionkey, max(n_nationkey) from phoenix123.v1.nation group by n_regionkey having max(n_nationkey) > 20";
    QueryBuilder builder = client.queryBuilder().sql(sql);
    RowSet sets = builder.rowSet();

    builder.planMatcher()
        .exclude("Aggregate")
        .include("Phoenix\\(.* GROUP BY .* HAVING MAX")
        .match();

    TupleMetadata schema = new SchemaBuilder()
        .addNullable("n_regionkey", MinorType.BIGINT)
        .addNullable("EXPR$1", MinorType.BIGINT)
        .build();

    RowSet expected = new RowSetBuilder(client.allocator(), schema)
        .addRow(1, 24)
        .addRow(2, 21)
        .addRow(3, 23)
        .build();

    new RowSetComparison(expected).verifyAndClearAll(sets);
  }
}
