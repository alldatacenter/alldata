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

package org.apache.drill.exec.store.excel;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.QueryBuilder;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZoneOffset;

import static org.apache.drill.test.QueryTestUtil.ConvertDateToLong;
import static org.apache.drill.test.QueryTestUtil.generateCompressedFile;
import static org.apache.drill.test.rowSet.RowSetUtilities.strArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(RowSetTests.class)
public class TestExcelFormat extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    // Needed for compressed file unit test
    dirTestWatcher.copyResourceToRoot(Paths.get("excel/"));
  }

  @Test
  public void testStarQuery() throws Exception {
    String sql = "SELECT * FROM cp.`excel/test_data.xlsx`";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("id", "first_name", "last_name", "email", "gender", "birthdate", "balance", "order_count", "average_order")
      .baselineValues(1.0, "Cornelia", "Matej", "cmatej0@mtv.com", "Female", "10/31/1974", 735.29, 22.0, 33.42227272727273)
      .baselineValues(2.0, "Nydia", "Heintsch", "nheintsch1@godaddy.com", "Female", "12/10/1966", 784.14, 22.0, 35.64272727272727)
      .baselineValues(3.0, "Waiter", "Sherel", "wsherel2@utexas.edu", "Male", "3/12/1961", 172.36, 17.0, 10.138823529411766)
      .baselineValues(4.0, "Cicely", "Lyver", "clyver3@mysql.com", "Female", "5/4/2000", 987.39, 6.0, 164.565)
      .baselineValues(5.0, "Dorie", "Doe", "ddoe4@spotify.com", "Female", "12/28/1955", 852.48, 17.0, 50.14588235294118)
      .go();
  }

  @Test
  public void testStarWithProvidedSchema() throws Exception {
    String sql = "SELECT * FROM table(dfs.`excel/schema_provisioning.xlsx` " +
      "(schema => 'inline=(`col1` INTEGER, `col2` FLOAT, `col3` VARCHAR)'" +
      "))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("col1", MinorType.INT)
      .addNullable("col2", MinorType.FLOAT4)
      .addNullable("col3", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1, null, null)
      .addRow(2, 3.0, null)
      .addRow(4, 5.0, "six")
      .addRow(7, 8.0, "nine")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitWithProvidedSchema() throws Exception {
    String sql = "SELECT col1, col2, col3 FROM table(dfs.`excel/schema_provisioning.xlsx` " +
      "(schema => 'inline=(`col1` INTEGER, `col2` FLOAT, `col3` VARCHAR)'" +
      "))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("col1", MinorType.INT)
      .addNullable("col2", MinorType.FLOAT4)
      .addNullable("col3", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1, null, null)
      .addRow(2, 3.0, null)
      .addRow(4, 5.0, "six")
      .addRow(7, 8.0, "nine")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testProvidedSchemaWithMetadata() throws Exception {
    String sql =
      "SELECT col1, col2, col3, _category, _content_status, _content_type, _creator, _description, _identifier, _keywords, _last_modified_by_user, _revision, _subject, _title, " +
        "_created," +
        "_last_printed, _modified " +
        "FROM table(dfs.`excel/schema_provisioning.xlsx` (schema => 'inline=(`col1` INTEGER, `col2` FLOAT, `col3` VARCHAR)')) LIMIT 1";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("col1", MinorType.INT)
      .addNullable("col2", MinorType.FLOAT4)
      .addNullable("col3", MinorType.VARCHAR)
      .addNullable("_category", MinorType.VARCHAR)
      .addNullable("_content_status", MinorType.VARCHAR)
      .addNullable("_content_type", MinorType.VARCHAR)
      .addNullable("_creator", MinorType.VARCHAR)
      .addNullable("_description", MinorType.VARCHAR)
      .addNullable("_identifier", MinorType.VARCHAR)
      .addNullable("_keywords", MinorType.VARCHAR)
      .addNullable("_last_modified_by_user", MinorType.VARCHAR)
      .addNullable("_revision", MinorType.VARCHAR)
      .addNullable("_subject", MinorType.VARCHAR)
      .addNullable("_title", MinorType.VARCHAR)
      .addNullable("_created", MinorType.TIMESTAMP)
      .addNullable("_last_printed", MinorType.TIMESTAMP)
      .addNullable("_modified", MinorType.TIMESTAMP)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1, null, null, null, null, null, "Microsoft Office User", null, null, null, "Microsoft Office User",
        null, null, null, ConvertDateToLong("2021-10-27T11:35:00Z"), null, ConvertDateToLong("2021-10-28T13:25:51Z"))
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testProvidedSchemaWithNonDefaultSheet() throws Exception {
    String sql = "SELECT col1, col2, col3 FROM table(dfs.`excel/schema_provisioning.xlsx` " +
      "(type => 'excel', sheetName => 'SecondSheet', schema => 'inline=(`col1` INTEGER, `col2` FLOAT, `col3` VARCHAR)'" +
      "))";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("col1", MinorType.INT)
      .addNullable("col2", MinorType.FLOAT4)
      .addNullable("col3", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1, 4.0, "Seven")
      .addRow(2, 5.0, "Eight")
      .addRow(3, 6.0, "Nine")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitAllQuery() throws RpcException {
    String sql = "SELECT id, first_name, last_name, email, gender, birthdate, balance, order_count, average_order FROM cp.`excel/test_data.xlsx`";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.FLOAT8)
      .addNullable("first_name", TypeProtos.MinorType.VARCHAR)
      .addNullable("last_name", TypeProtos.MinorType.VARCHAR)
      .addNullable("email", TypeProtos.MinorType.VARCHAR)
      .addNullable("gender", TypeProtos.MinorType.VARCHAR)
      .addNullable("birthdate", TypeProtos.MinorType.VARCHAR)
      .addNullable("balance", TypeProtos.MinorType.FLOAT8)
      .addNullable("order_count", TypeProtos.MinorType.FLOAT8)
      .addNullable("average_order", TypeProtos.MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1.0, "Cornelia", "Matej", "cmatej0@mtv.com", "Female", "10/31/1974", 735.29, 22.0, 33.42227273)
      .addRow(2.0, "Nydia", "Heintsch", "nheintsch1@godaddy.com", "Female", "12/10/1966", 784.14, 22.0, 35.64272727)
      .addRow(3.0, "Waiter", "Sherel", "wsherel2@utexas.edu", "Male", "3/12/1961", 172.36, 17.0, 10.13882353)
      .addRow(4.0, "Cicely", "Lyver", "clyver3@mysql.com", "Female", "5/4/2000", 987.39, 6.0, 164.565)
      .addRow(5.0, "Dorie", "Doe", "ddoe4@spotify.com", "Female", "12/28/1955", 852.48, 17.0, 50.14588235)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }


  @Test
  public void testExplicitMetadataQuery() throws RpcException {
    String sql =
      "SELECT _category, _content_status, _content_type, _creator, _description, _identifier, _keywords, _last_modified_by_user, _revision, _subject, _title, _created," +
        "_last_printed, _modified FROM cp.`excel/test_data.xlsx` LIMIT 1";

    QueryBuilder q = client.queryBuilder().sql(sql);
    RowSet results = q.rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("_category", TypeProtos.MinorType.VARCHAR)
      .addNullable("_content_status", TypeProtos.MinorType.VARCHAR)
      .addNullable("_content_type", TypeProtos.MinorType.VARCHAR)
      .addNullable("_creator", TypeProtos.MinorType.VARCHAR)
      .addNullable("_description", TypeProtos.MinorType.VARCHAR)
      .addNullable("_identifier", TypeProtos.MinorType.VARCHAR)
      .addNullable("_keywords", TypeProtos.MinorType.VARCHAR)
      .addNullable("_last_modified_by_user", TypeProtos.MinorType.VARCHAR)
      .addNullable("_revision", TypeProtos.MinorType.VARCHAR)
      .addNullable("_subject", TypeProtos.MinorType.VARCHAR)
      .addNullable("_title", TypeProtos.MinorType.VARCHAR)
      .addNullable("_created", TypeProtos.MinorType.TIMESTAMP)
      .addNullable("_last_printed", TypeProtos.MinorType.TIMESTAMP)
      .addNullable("_modified", TypeProtos.MinorType.TIMESTAMP)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("test_category", null, null, "test_author", null, null, "test_keywords", "Microsoft Office User", null, "test_subject", "test_title",
        1571602578000L, null,1633358966000L)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitSomeQuery() throws RpcException {
    String sql = "SELECT id, first_name, order_count FROM cp.`excel/test_data.xlsx`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.FLOAT8)
      .addNullable("first_name", TypeProtos.MinorType.VARCHAR)
      .addNullable("order_count", TypeProtos.MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1.0, "Cornelia", 22.0)
      .addRow(2.0, "Nydia", 22.0)
      .addRow(3.0, "Waiter", 17.0)
      .addRow(4.0, "Cicely", 6.0)
      .addRow(5.0, "Dorie", 17.0)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testHandleMissingRows() throws RpcException {
    String sql = "SELECT id, first_name, order_count FROM table(cp.`excel/blank_rows.xlsx` (type => 'excel', sheetName => 'data', headerRow => 3))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.FLOAT8)
      .addNullable("first_name", TypeProtos.MinorType.VARCHAR)
      .addNullable("order_count", TypeProtos.MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1.0, "Cornelia", 22.0)
      .addRow(2.0, "Nydia", 22.0)
      .addRow(3.0, "Waiter", 17.0)
      .addRow(4.0, "Cicely", 6.0)
      .addRow(5.0, "Dorie", 17.0)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitWithSpacesInColHeader() throws RpcException {
    String sql = "SELECT col1, col2 FROM table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'spaceInColHeader'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("col1", MinorType.FLOAT8)
      .addNullable("col2", MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1,2)
      .addRow(3,4)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testNonDefaultSheetQuery() throws RpcException {
    String sql = "SELECT * FROM  table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'secondSheet'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("event_date", TypeProtos.MinorType.VARCHAR)
      .addNullable("ip_address", TypeProtos.MinorType.VARCHAR)
      .addNullable("user_agent", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("2019-02-17 11:21:45", "166.11.144.176", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru-RU) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4")
        .addRow("2019-03-03 04:10:31", "203.221.176.215", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.215 Safari/535.1")
        .addRow("2018-04-05 08:17:17", "11.134.119.132", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.813.0 Safari/535.1")
        .addRow("2018-12-05 05:36:10", "68.145.168.82", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.8 (KHTML, like Gecko) Chrome/17.0.940.0 Safari/535.8")
        .addRow("2018-04-01 16:25:18", "21.12.166.184", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; it-it) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27")
        .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitNonDefaultSheetQuery() throws RpcException {
    String sql = "SELECT event_date, ip_address, user_agent FROM  table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'secondSheet'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("event_date", TypeProtos.MinorType.VARCHAR)
      .addNullable("ip_address", TypeProtos.MinorType.VARCHAR)
      .addNullable("user_agent", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("2019-02-17 11:21:45", "166.11.144.176", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru-RU) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4")
      .addRow("2019-03-03 04:10:31", "203.221.176.215", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.215 Safari/535.1")
      .addRow("2018-04-05 08:17:17", "11.134.119.132", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.813.0 Safari/535.1")
      .addRow("2018-12-05 05:36:10", "68.145.168.82", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.8 (KHTML, like Gecko) Chrome/17.0.940.0 Safari/535.8")
      .addRow("2018-04-01 16:25:18", "21.12.166.184", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; it-it) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  /**
   * This test verifies that when a user attempts to open a sheet that doesn't exist, the plugin will throw an error
   */
  @Test
  public void testInvalidSheetQuery() throws Exception {
    String sql = "SELECT * FROM  table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'noSuchSheet')) LIMIT 1";
    try {
      run(sql);
      fail();
    } catch (DrillRuntimeException e) {
      assertTrue(e.getMessage().contains("Could not open sheet "));
    }
  }

  @Test
  public void testDefineColumnsQuery() throws RpcException {
    String sql = "SELECT * FROM  table(cp.`excel/test_data.xlsx` (type => 'excel', firstColumn => 2, lastColumn => 5))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("first_name", TypeProtos.MinorType.VARCHAR)
      .addNullable("last_name", TypeProtos.MinorType.VARCHAR)
      .addNullable("email", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("Cornelia", "Matej", "cmatej0@mtv.com")
      .addRow("Nydia", "Heintsch", "nheintsch1@godaddy.com")
      .addRow("Waiter", "Sherel", "wsherel2@utexas.edu")
      .addRow("Cicely", "Lyver", "clyver3@mysql.com")
      .addRow("Dorie", "Doe", "ddoe4@spotify.com")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testLastRowQuery() throws RpcException {
    String sql = "SELECT event_date, ip_address, user_agent FROM table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'secondSheet', lastRow => 5))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("event_date", TypeProtos.MinorType.VARCHAR)
      .addNullable("ip_address", TypeProtos.MinorType.VARCHAR)
      .addNullable("user_agent", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("2019-02-17 11:21:45", "166.11.144.176", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru-RU) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4")
      .addRow("2019-03-03 04:10:31", "203.221.176.215", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.215 Safari/535.1")
      .addRow("2018-04-05 08:17:17", "11.134.119.132", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.813.0 Safari/535.1")
      .addRow("2018-12-05 05:36:10", "68.145.168.82", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.8 (KHTML, like Gecko) Chrome/17.0.940.0 Safari/535.8")
      .addRow("2018-04-01 16:25:18", "21.12.166.184", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; it-it) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testStarNoFieldNamesQuery() throws RpcException {
    String sql = "SELECT * FROM  table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'thirdSheet', headerRow => -1))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("field_1", TypeProtos.MinorType.VARCHAR)
      .addNullable("field_2", TypeProtos.MinorType.VARCHAR)
      .addNullable("field_3", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("2019-02-17 11:21:45", "166.11.144.176", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru-RU) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4")
      .addRow("2019-03-03 04:10:31", "203.221.176.215", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.215 Safari/535.1")
      .addRow("2018-04-05 08:17:17", "11.134.119.132", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.813.0 Safari/535.1")
      .addRow("2018-12-05 05:36:10", "68.145.168.82", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.8 (KHTML, like Gecko) Chrome/17.0.940.0 Safari/535.8")
      .addRow("2018-04-01 16:25:18", "21.12.166.184", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; it-it) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitNoFieldNamesQuery() throws RpcException {
    String sql = "SELECT field_1, field_2, field_3 FROM  table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'thirdSheet', headerRow => -1))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("field_1", TypeProtos.MinorType.VARCHAR)
      .addNullable("field_2", TypeProtos.MinorType.VARCHAR)
      .addNullable("field_3", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("2019-02-17 11:21:45", "166.11.144.176", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru-RU) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4")
      .addRow("2019-03-03 04:10:31", "203.221.176.215", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.215 Safari/535.1")
      .addRow("2018-04-05 08:17:17", "11.134.119.132", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.813.0 Safari/535.1")
      .addRow("2018-12-05 05:36:10", "68.145.168.82", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.8 (KHTML, like Gecko) Chrome/17.0.940.0 Safari/535.8")
      .addRow("2018-04-01 16:25:18", "21.12.166.184", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; it-it) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testBlankRowsInFileQuery() throws RpcException {
    String sql = "SELECT * FROM  table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'fourthSheet'))";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("event_date", TypeProtos.MinorType.VARCHAR)
      .addNullable("ip_address", TypeProtos.MinorType.VARCHAR)
      .addNullable("user_agent", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("2019-02-17 11:21:45", "166.11.144.176", "Mozilla/5.0 (Windows; U; Windows NT 5.1; ru-RU) AppleWebKit/533.19.4 (KHTML, like Gecko) Version/5.0.3 Safari/533.19.4")
      .addRow("2019-03-03 04:10:31", "203.221.176.215", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_2) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/13.0.782.215 Safari/535.1")
      .addRow("2018-04-05 08:17:17", "11.134.119.132", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.813.0 Safari/535.1")
      .addRow("2018-12-05 05:36:10", "68.145.168.82", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.8 (KHTML, like Gecko) Chrome/17.0.940.0 Safari/535.8")
      .addRow("2018-04-01 16:25:18", "21.12.166.184", "Mozilla/5.0 (Macintosh; U; Intel Mac OS X 10_6_6; it-it) AppleWebKit/533.20.25 (KHTML, like Gecko) Version/5.0.4 Safari/533.20.27")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  /**
   * This tests to what happens if you attempt to query a completely empty sheet
   * The result should be an empty data set.
   *
   * @throws RpcException Throws exception if unable to connect to Drill cluster.
   */
  @Test
  public void testEmptySheetQuery() throws RpcException {
    String sql = "SELECT * " + "FROM table(cp.`excel/test_data.xlsx` (type => 'excel', sheetName => 'emptySheet'))";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    assertNull(results);
  }

  @Test
  public void testMissingDataQuery() throws Exception {
    String sql = "SELECT * FROM table(cp.`excel/test_data.xlsx` (type=> 'excel', sheetName => 'missingDataSheet'))";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("col1", "col2", "col3")
      .baselineValues(1.0,2.0,null)
      .baselineValues(2.0,4.0,null)
      .baselineValues(3.0,null,null)
      .baselineValues(null,6.0,null)
      .baselineValues(null,8.0,null)
      .baselineValues(4.0,null,null)
      .baselineValues(5.0,10.0,null)
      .baselineValues(6.0,12.0,null)
      .go();
  }

  @Test
  public void testInconsistentDataQuery() throws Exception {
    String sql = "SELECT * FROM table(cp.`excel/test_data.xlsx` (type=> 'excel', sheetName => 'inconsistentData', allTextMode => true))";

    testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("col1", "col2")
      .baselineValues("1", "Bob")
      .baselineValues("2", "Steve")
      .baselineValues("3", "Anne")
      .baselineValues("Bob", "3")
      .go();
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "SELECT COUNT(*) as cnt FROM table(cp.`excel/test_data.xlsx` (type=> 'excel', sheetName => 'inconsistentData', allTextMode => true))";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match",4L, cnt);
  }

  @Test
  public void testExplicitSomeQueryWithCompressedFile() throws Exception {
    generateCompressedFile("excel/test_data.xlsx", "zip", "excel/test_data.xlsx.zip" );

    String sql = "SELECT id, first_name, order_count FROM dfs.`excel/test_data.xlsx.zip`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", TypeProtos.MinorType.FLOAT8)
      .addNullable("first_name", TypeProtos.MinorType.VARCHAR)
      .addNullable("order_count", TypeProtos.MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1.0, "Cornelia", 22.0)
      .addRow(2.0, "Nydia", 22.0)
      .addRow(3.0, "Waiter", 17.0)
      .addRow(4.0, "Cicely", 6.0)
      .addRow(5.0, "Dorie", 17.0)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testFileWithDoubleDates() throws Exception {
    String sql = "SELECT `Close Date`, `Type` FROM table(cp.`excel/test_data.xlsx` (type=> 'excel', sheetName => 'comps')) WHERE style='Contemporary'";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("Close Date", TypeProtos.MinorType.TIMESTAMP)
      .addNullable("Type", TypeProtos.MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1412294400000L, "Hi Rise")
      .addRow(1417737600000L, "Hi Rise")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testTextFormula() throws Exception {
    String sql = "SELECT * FROM cp.`excel/text-formula.xlsx`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("Grade", MinorType.VARCHAR)
      .addNullable("Gender", MinorType.VARCHAR)
      .addNullable("Combined", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("Seventh Grade", "Girls", "Seventh Grade Girls")
      .addRow("Sixth Grade", "Girls", "Sixth Grade Girls")
      .addRow("Fourth Grade", "Girls", "Fourth Grade Girls")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testNumericFormula() throws Exception {
    String sql = "SELECT * FROM cp.`excel/numeric-formula.xlsx`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("col1", MinorType.FLOAT8)
      .addNullable("col2", MinorType.FLOAT8)
      .addNullable("calc", MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(2.0, 8.0, 256.0)
      .addRow(4.0, 6.0, 4096.0)
      .addRow(6.0, 4.0, 1296.0)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testLimitPushdown() throws Exception {
    String sql = "SELECT id, first_name, order_count FROM cp.`excel/test_data.xlsx` LIMIT 5";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "limit=5")
      .match();
  }

  @Test
  public void testBlankColumnFix() throws Exception {
    String sql = "SELECT * FROM dfs.`excel/zips-small.xlsx`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("zip", MinorType.FLOAT8)
      .addNullable("lat", MinorType.FLOAT8)
      .addNullable("lng", MinorType.FLOAT8)
      .addNullable("city", MinorType.VARCHAR)
      .addNullable("state_id", MinorType.VARCHAR)
      .addNullable("state_name", MinorType.VARCHAR)
      .addNullable("zcta", MinorType.VARCHAR)
      .addNullable("parent_zcta", MinorType.FLOAT8)
      .addNullable("population", MinorType.FLOAT8)
      .addNullable("density", MinorType.FLOAT8)
      .addNullable("county_fips", MinorType.FLOAT8)
      .addNullable("county_name", MinorType.VARCHAR)
      .addNullable("county_weights", MinorType.VARCHAR)
      .addNullable("county_names_all", MinorType.VARCHAR)
      .addNullable("county_fips_all", MinorType.VARCHAR)
      .addNullable("imprecise", MinorType.VARCHAR)
      .addNullable("military", MinorType.VARCHAR)
      .addNullable("timezone", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(601.0, 18.18004, -66.75218, "Adjuntas", "PR", "Puerto Rico", "TRUE", 0.0, 17242.0, 111.4, 72001.0, "Adjuntas", "{'72001':99.43,'72141':0.57}", "Adjuntas|Utuado",
      "72001|72141", "FALSE", "FALSE", "America/Puerto_Rico")
      .addRow(602.0, 18.36073, -67.17517, "Aguada", "PR", "Puerto Rico", "TRUE", 0.0, 38442.0, 523.5, 72003.0, "Aguada", "{'72003':100}", "Aguada", "72003", "FALSE", "FALSE", "America" +
    "/Puerto_Rico")
      .addRow(603.0, 18.45439, -67.12202, "Aguadilla", "PR", "Puerto Rico", "TRUE", 0.0, 48814.0, 667.9, 72005.0, "Aguadilla", "{'72005':100}", "Aguadilla", "72005", "FALSE", "FALSE",
    "America/Puerto_Rico")
      .addRow(606.0, 18.16724, -66.93828, "Maricao", "PR", "Puerto Rico", "TRUE", 0.0, 6437.0, 60.4, 72093.0, "Maricao", "{'72093':94.88,'72121':1.35,'72153':3.78}", "Maricao|Yauco" +
    "|Sabana Grande", "72093|72153|72121", "FALSE", "FALSE", "America/Puerto_Rico")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testGetSheetNames() throws RpcException {
    String sql = "SELECT _sheets FROM dfs.`excel/test_data.xlsx` LIMIT 1";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addArray("_sheets", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow((Object)strArray("data", "secondSheet", "thirdSheet", "fourthSheet", "emptySheet", "missingDataSheet", "inconsistentData", "comps", "spaceInColHeader"))
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void test1904BasedDates() throws RpcException {
    String sql = "SELECT * FROM dfs.`excel/1904Dates.xlsx`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("playerId", MinorType.VARCHAR)
      .addNullable("birthYear", MinorType.FLOAT8)
      .addNullable("birthMonth", MinorType.FLOAT8)
      .addNullable("birthDay", MinorType.FLOAT8)
      .addNullable("known", MinorType.FLOAT8)
      .addNullable("date", MinorType.TIMESTAMP)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("foo", 1991, 10, 14, 1, LocalDate.parse("1991-10-14").atStartOfDay().toInstant(ZoneOffset.UTC))
      .addRow("bar", 1989, 12, 16, 1, LocalDate.parse("1989-12-16").atStartOfDay().toInstant(ZoneOffset.UTC))
      .addRow("baz", 1994, 3, 10, 0, LocalDate.parse("1994-03-10").atStartOfDay().toInstant(ZoneOffset.UTC))
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testMissingData() throws RpcException {
    String sql = "SELECT * FROM dfs.`excel/missing_data.xlsx`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("original_id", MinorType.VARCHAR)
      .addNullable("original_nameFirst", MinorType.VARCHAR)
      .addNullable("original_nameLast", MinorType.VARCHAR)
      .addNullable("original_emailWork", MinorType.VARCHAR)
      .addNullable("original_cityHome", MinorType.VARCHAR)
      .addNullable("original_zipcodeHome", MinorType.FLOAT8)
      .addNullable("original_countryHome", MinorType.VARCHAR)
      .addNullable("original_birthday", MinorType.TIMESTAMP)
      .addNullable("original_stateHome", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("XXXX00000001", "James", "Kushner", null, null, 10235, "US", LocalDate.parse("1957-04-18").atStartOfDay().toInstant(ZoneOffset.UTC), "NY")
      .addRow("XXXX00000002", "Steve", "Hecht", null, null, 11213, "US", LocalDate.parse("1982-08-10").atStartOfDay().toInstant(ZoneOffset.UTC), "NY")
      .addRow("XXXX00000003", "Ethan", "Stein", null, null, 10028, "US", LocalDate.parse("1991-04-11").atStartOfDay().toInstant(ZoneOffset.UTC), "NY")
      .addRow("XXXX00000004", "Mohammed", "Fatima", null, "Baltimore", 21202, "US", LocalDate.parse("1990-05-15").atStartOfDay().toInstant(ZoneOffset.UTC), "MD")
      .addRow("XXXX00000005", "Yakov", "Borodin", null, "Teaneck", 7666, "US", LocalDate.parse("1986-12-20").atStartOfDay().toInstant(ZoneOffset.UTC), "NJ")
      .addRow("XXXX00000006", "Akhil", "Chavda", null, null, null, "US", null, null)
      .addRow("XXXX00000007", "Mark", "Rahman", null, "Ellicott City", 21043, null, LocalDate.parse("1974-06-13").atStartOfDay().toInstant(ZoneOffset.UTC), "MD")
      .addRow("XXXX00000008", "Henry", "Smith", "xxxx@gmail.com", null, null, null, null, null)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testDuplicateColumnNames() throws Exception {
    String sql = "SELECT * FROM cp.`excel/dup_col_names.xlsx`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("Col1", MinorType.FLOAT8)
      .addNullable("Col1_1", MinorType.FLOAT8)
      .addNullable("Col1_2", MinorType.FLOAT8)
      .addNullable("Col1_2_1", MinorType.FLOAT8)
      .addNullable("column1", MinorType.VARCHAR)
      .addNullable("COLUMN1_1", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1.0, 5.0, 9.0, 13.0, "a", "e")
      .addRow(2.0, 6.0, 10.0, 14.0, "b", "f")
      .addRow(3.0, 7.0, 11.0, 15.0, "c", "g")
      .addRow(4.0, 9.0, 12.0, 16.0, "d", "h")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testDuplicateColumnNamesWithExplicitColumnNames() throws Exception {
    String sql = "SELECT Col1, Col1_1, Col1_2, Col1_2_1 FROM cp.`excel/dup_col_names.xlsx`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("Col1", MinorType.FLOAT8)
      .addNullable("Col1_1", MinorType.FLOAT8)
      .addNullable("Col1_2", MinorType.FLOAT8)
      .addNullable("Col1_2_1", MinorType.FLOAT8)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1.0, 5.0, 9.0, 13.0)
      .addRow(2.0, 6.0, 10.0, 14.0)
      .addRow(3.0, 7.0, 11.0, 15.0)
      .addRow(4.0, 9.0, 12.0, 16.0)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  // DRILL-8182
  @Test
  public void testTableFuncsThatDifferOnlyByFormatConfig() throws Exception {
    String sql = "WITH prod AS (" +
      " SELECT id, name FROM table(cp.`excel/test_cross_sheet_join.xlsx` (type=> 'excel', sheetName => 'products'))" +
      "), cust AS (" +
      " SELECT id, name FROM table(cp.`excel/test_cross_sheet_join.xlsx` (type=> 'excel', sheetName => 'customers'))" +
      ")" +
      "SELECT prod.*, cust.* from prod JOIN cust ON prod.id = cust.id";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("id", MinorType.FLOAT8)
      .addNullable("name", MinorType.VARCHAR)
      .addNullable("id0", MinorType.FLOAT8)
      .addNullable("name0", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(1.0, "Doughnut", 1.0, "Alice")
      .addRow(2.0, "Coffee", 2.0, "Bob")
      .addRow(3.0, "Coke", 3.0, "Carol")
      .addRow(4.0, "Cheesecake", 4.0, "Dave")
      .addRow(5.0, "Popsicle", 5.0, "Eve")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }
}
