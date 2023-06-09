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

package org.apache.drill.exec.store.spss;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.apache.drill.test.QueryTestUtil.generateCompressedFile;

@Category(RowSetTests.class)
public class TestSpssReader extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    // Needed for compressed file unit test
    dirTestWatcher.copyResourceToRoot(Paths.get("spss/"));
  }

  @Test
  public void testStarQuery() throws Exception {
    String sql = "SELECT * FROM dfs.`spss/testdata.sav` WHERE d16=4";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("ID", TypeProtos.MinorType.FLOAT8)
      .addNullable("Urban", TypeProtos.MinorType.FLOAT8)
      .addNullable("Urban_value", TypeProtos.MinorType.VARCHAR)
      .addNullable("District", TypeProtos.MinorType.FLOAT8)
      .addNullable("District_value", TypeProtos.MinorType.VARCHAR)
      .addNullable("Province", TypeProtos.MinorType.FLOAT8)
      .addNullable("Province_value", TypeProtos.MinorType.VARCHAR)
      .addNullable("Interviewer", TypeProtos.MinorType.FLOAT8)
      .addNullable("Date", TypeProtos.MinorType.FLOAT8)
      .addNullable("d6_1", TypeProtos.MinorType.FLOAT8)
      .addNullable("d6_1_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("d6_2", TypeProtos.MinorType.FLOAT8)
      .addNullable("d6_2_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("d6_3", TypeProtos.MinorType.FLOAT8)
      .addNullable("d6_3_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("d6_4", TypeProtos.MinorType.FLOAT8)
      .addNullable("d6_4_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("s_1", TypeProtos.MinorType.VARCHAR)
      .addNullable("d6_5", TypeProtos.MinorType.FLOAT8)
      .addNullable("d6_5_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("d6_6", TypeProtos.MinorType.FLOAT8)
      .addNullable("d6_6_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("d6_7", TypeProtos.MinorType.FLOAT8)
      .addNullable("d6_7_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("q1", TypeProtos.MinorType.FLOAT8)
      .addNullable("q1_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("q2", TypeProtos.MinorType.FLOAT8)
      .addNullable("q2_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("d7a", TypeProtos.MinorType.FLOAT8)
      .addNullable("d7a_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("d7b", TypeProtos.MinorType.FLOAT8)
      .addNullable("d7b_Value", TypeProtos.MinorType.VARCHAR)
      .addNullable("d16", TypeProtos.MinorType.FLOAT8)
      .addNullable("Stratum", TypeProtos.MinorType.FLOAT8)
      .addNullable("S1_IP", TypeProtos.MinorType.FLOAT8)
      .addNullable("S2_IP", TypeProtos.MinorType.FLOAT8)
      .addNullable("Sample_Weight", TypeProtos.MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(47.0, 1.0, "Urban", 101.0, "Kabul", 1.0, "Kabul", 151.0, 1.34557632E10, 1.0, "Yes", 2.0, "No", 2.0, "No", 2.0, "No", "", 2.0, "No", 2.0, "No", 2.0, "No", 1.0, "Good", 2.0, "The same", 5.0, "Housewife (not working outside of the home)", 97.0, "Not Asked", 4.0, 121.0, 0.007463305415042708, 0.006666666666666667, 20098.33333333333)
      .addRow(53.0, 1.0, "Urban", 101.0, "Kabul", 1.0, "Kabul", 151.0, 1.34557632E10, 1.0, "Yes", 2.0, "No", 2.0, "No", 2.0, "No", "", 2.0, "No", 2.0, "No", 2.0, "No", 1.0, "Good", 2.0, "The same", 5.0, "Housewife (not working outside of the home)", 97.0, "Not Asked", 4.0, 121.0, 0.007463305415042708, 0.006666666666666667, 20098.33333333333)
      .addRow(66.0, 1.0, "Urban", 101.0, "Kabul", 1.0, "Kabul", 774.0, 1.34556768E10, 2.0, "No", 1.0, "Yes", 1.0, "Yes", 2.0, "No", "", 2.0, "No", 2.0, "No", 2.0, "No", 1.0, "Good", 1.0, "Better", 1.0, "Working full time", 13.0, "Private Business Sole Proprietor", 4.0, 111.0, 0.017389288198469743, 0.006666666666666667, 8626.0)
      .build();

    assertEquals(3, results.rowCount());

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitQuery() throws Exception {
    String sql = "SELECT ID, Urban, Urban_value FROM dfs.`spss/testdata.sav` WHERE d16=4";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("ID", TypeProtos.MinorType.FLOAT8)
      .addNullable("Urban", TypeProtos.MinorType.FLOAT8)
      .addNullable("Urban_value", TypeProtos.MinorType.VARCHAR)
      .buildSchema();


    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(47.0, 1.0, "Urban").addRow(53.0, 1.0, "Urban")
      .addRow(66.0, 1.0, "Urban")
      .build();

    assertEquals(3, results.rowCount());

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "SELECT COUNT(*) FROM dfs.`spss/testdata.sav`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match", 25L, cnt);
  }

  @Test
  public void testExplicitQueryWithCompressedFile() throws Exception {
    generateCompressedFile("spss/testdata.sav", "zip", "spss/testdata.sav.zip");

    String sql = "SELECT ID, Urban, Urban_value FROM dfs.`spss/testdata.sav.zip`  WHERE d16=4";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("ID", TypeProtos.MinorType.FLOAT8)
      .addNullable("Urban", TypeProtos.MinorType.FLOAT8)
      .addNullable("Urban_value", TypeProtos.MinorType.VARCHAR)
      .buildSchema();


    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(47.0, 1.0, "Urban").addRow(53.0, 1.0, "Urban")
      .addRow(66.0, 1.0, "Urban")
      .build();

    assertEquals(3, results.rowCount());

    new RowSetComparison(expected).verifyAndClearAll(results);
  }
}
