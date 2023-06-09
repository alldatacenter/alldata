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

package org.apache.drill.exec.store.sas;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.Assert.assertEquals;
import static org.apache.drill.test.QueryTestUtil.generateCompressedFile;


@Category(RowSetTests.class)
public class TestSasReader extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    // Needed for compressed file unit test
    dirTestWatcher.copyResourceToRoot(Paths.get("sas/"));
  }

  @Test
  public void testStarQuery() throws Exception {
    String sql = "SELECT * FROM cp.`sas/mixed_data_two.sas7bdat` WHERE x1 = 1";
    RowSet results  = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("x1", MinorType.FLOAT8)
      .addNullable("x2", MinorType.FLOAT8)
      .addNullable("x3", MinorType.VARCHAR)
      .addNullable("x4", MinorType.FLOAT8)
      .addNullable("x5", MinorType.FLOAT8)
      .addNullable("x6", MinorType.FLOAT8)
      .addNullable("x7", MinorType.FLOAT8)
      .addNullable("x8", MinorType.FLOAT8)
      .addNullable("x9", MinorType.FLOAT8)
      .addNullable("x10", MinorType.FLOAT8)
      .addNullable("x11", MinorType.FLOAT8)
      .addNullable("x12", MinorType.FLOAT8)
      .addNullable("x13", MinorType.FLOAT8)
      .addNullable("x14", MinorType.FLOAT8)
      .addNullable("x15", MinorType.FLOAT8)
      .addNullable("x16", MinorType.FLOAT8)
      .addNullable("x17", MinorType.FLOAT8)
      .addNullable("x18", MinorType.FLOAT8)
      .addNullable("x19", MinorType.FLOAT8)
      .addNullable("x20", MinorType.FLOAT8)
      .addNullable("x21", MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 1.1, "AAAAAAAA", 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 31626061L, 31625961L, 31627061L, 31616061L, 31636061L, 31526061L, 31726061L)
      .addRow(1L, 1.1, "AAAAAAAA", 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 31626061L, 31625961L, 31627061L, 31616061L, 31636061L, 31526061L, 31726061L)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testMetadataColumns() throws Exception {
    String sql = "SELECT _compression_method, _file_label, _file_type, " +
      "_os_name, _os_type, _sas_release, _session_encoding, _server_type, " +
      "_date_created, _date_modified FROM cp.`sas/date_formats.sas7bdat`";
    RowSet results  = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("_compression_method", MinorType.VARCHAR)
      .addNullable("_file_label", MinorType.VARCHAR)
      .addNullable("_file_type", MinorType.VARCHAR)
      .addNullable("_os_name", MinorType.VARCHAR)
      .addNullable("_os_type", MinorType.VARCHAR)
      .addNullable("_sas_release", MinorType.VARCHAR)
      .addNullable("_session_encoding", MinorType.VARCHAR)
      .addNullable("_server_type", MinorType.VARCHAR)
      .addNullable("_date_created", MinorType.DATE)
      .addNullable("_date_modified", MinorType.DATE)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(null, "DATA", null, null, "9.0401M4", null, "X64_7PRO", null,
        LocalDate.parse("2017-03-14"), LocalDate.parse("2017-03-14"))
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testCompressedFile() throws Exception {
    generateCompressedFile("sas/mixed_data_two.sas7bdat", "zip", "sas/mixed_data_two.sas7bdat.zip" );

    String sql = "SELECT x1, x2, x3 FROM dfs.`sas/mixed_data_two.sas7bdat.zip` WHERE x1 = 1";
    RowSet results  = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("x1", MinorType.FLOAT8)
      .addNullable("x2", MinorType.FLOAT8)
      .addNullable("x3", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1L, 1.1, "AAAAAAAA")
      .addRow(1L, 1.1, "AAAAAAAA")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testDates() throws Exception {
    String sql = "SELECT b8601da, e8601da, `date` FROM cp.`sas/date_formats.sas7bdat`";
    RowSet results  = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("b8601da", MinorType.DATE)
      .addNullable("e8601da", MinorType.DATE)
      .addNullable("date", MinorType.DATE)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(LocalDate.parse("2017-03-14"), LocalDate.parse("2017-03-14"), LocalDate.parse("2017-03-14"))
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testTimes() throws Exception {
    String sql = "SELECT * FROM cp.`sas/time_formats.sas7bdat`";
    RowSet results  = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("E8601LZ", MinorType.TIME)
      .addNullable("E8601TM", MinorType.TIME)
      .addNullable("HHMM", MinorType.TIME)
      .addNullable("HOUR", MinorType.TIME)
      .addNullable("MMSS", MinorType.TIME)
      .addNullable("TIME", MinorType.TIME)
      .addNullable("TIMEAMPM", MinorType.TIME)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(LocalTime.parse("10:10:10"), LocalTime.parse("10:10:10"), LocalTime.parse("10:10:10"),
        LocalTime.parse("10:10:10"), LocalTime.parse("10:10:10"), LocalTime.parse("10:10:10"), LocalTime.parse("10:10:10"))
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "SELECT COUNT(*) as cnt FROM cp.`sas/mixed_data_two.sas7bdat` ";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match", 50L, cnt);
  }

  @Test
  public void testLimitPushdown() throws Exception {
    String sql = "SELECT * FROM cp.`sas/mixed_data_one.sas7bdat` LIMIT 5";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "limit=5")
      .match();
  }
}
