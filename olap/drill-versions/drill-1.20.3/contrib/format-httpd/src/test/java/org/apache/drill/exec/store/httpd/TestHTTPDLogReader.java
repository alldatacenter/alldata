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

package org.apache.drill.exec.store.httpd;

import org.apache.drill.categories.RowSetTests;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.stream.Collectors;

import static org.apache.drill.test.QueryTestUtil.generateCompressedFile;
import static org.junit.Assert.assertEquals;
import static org.apache.drill.test.rowSet.RowSetUtilities.mapArray;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


@Category(RowSetTests.class)
public class TestHTTPDLogReader extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    // Needed for compressed file unit test
    dirTestWatcher.copyResourceToRoot(Paths.get("httpd/"));
  }

  @Test
  public void testDateField() throws RpcException {
    String sql = "SELECT `request_receive_time` FROM cp.`httpd/hackers-access-small.httpd` LIMIT 5";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("request_receive_time", MinorType.TIMESTAMP)
      .build();
    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(1445742685000L)
      .addRow(1445742686000L)
      .addRow(1445742687000L)
      .addRow(1445743471000L)
      .addRow(1445743472000L)
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testDateEpochField() throws RpcException {
    String sql = "SELECT `request_receive_time`, `request_receive_time_epoch` FROM cp.`httpd/hackers-access-small.httpd` LIMIT 5";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("request_receive_time", MinorType.TIMESTAMP)
      .addNullable("request_receive_time_epoch", MinorType.TIMESTAMP)
      .build();
    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow(1445742685000L, 1445742685000L)
      .addRow(1445742686000L, 1445742686000L)
      .addRow(1445742687000L, 1445742687000L )
      .addRow(1445743471000L, 1445743471000L)
      .addRow(1445743472000L, 1445743472000L)
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testCount() throws Exception {
    String sql = "SELECT COUNT(*) FROM cp.`httpd/hackers-access-small.httpd`";
    long result = client.queryBuilder().sql(sql).singletonLong();
    assertEquals(10L, result);
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "SELECT COUNT(*) AS cnt FROM cp.`httpd/hackers-access-small.httpd`";
    String plan = queryBuilder().sql(sql).explainJson();
    long cnt = queryBuilder().physical(plan).singletonLong();
    assertEquals("Counts should match",10L, cnt);
  }

  @Test
  public void testFlattenMap() throws Exception {
    String sql = "SELECT request_firstline_original_uri_query_came__from " +
      "FROM  table(cp.`httpd/hackers-access-small.httpd` (type => 'httpd', logFormat => '%h %l %u %t \\\"%r\\\" %s %b \\\"%{Referer}i\\\" " +
      "\\\"%{User-agent}i\\\"', " +
      "flattenWildcards => true)) WHERE `request_firstline_original_uri_query_came__from` IS NOT NULL";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("columns=\\[`request_firstline_original_uri_query_came__from`\\]")
      .match();

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("request_firstline_original_uri_query_came__from", MinorType.VARCHAR)
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow("http://howto.basjes.nl/join_form")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testLimitPushdown() throws Exception {
    String sql = "SELECT * FROM cp.`httpd/hackers-access-small.httpd` LIMIT 5";

    queryBuilder()
      .sql(sql)
      .planMatcher()
      .include("Limit", "limit=5")
      .match();
  }

  @Test
  public void testMapField() throws Exception {
    String sql = "SELECT data.`request_firstline_original_uri_query_$`.aqb AS aqb, data.`request_firstline_original_uri_query_$`.t AS data_time " +
      "FROM cp.`httpd/example1.httpd` AS data";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("aqb", MinorType.VARCHAR)
      .addNullable("data_time", MinorType.VARCHAR)
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow("1", "19/5/2012 23:51:27 2 -120")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testSingleExplicitColumn() throws Exception {
    String sql = "SELECT request_referer FROM cp.`httpd/hackers-access-small.httpd`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("request_referer", MinorType.VARCHAR)
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow("http://howto.basjes.nl/")
      .addRow("http://howto.basjes.nl/")
      .addRow("http://howto.basjes.nl/join_form")
      .addRow("http://howto.basjes.nl/")
      .addRow("http://howto.basjes.nl/join_form")
      .addRow("http://howto.basjes.nl/join_form")
      .addRow("http://howto.basjes.nl/")
      .addRow("http://howto.basjes.nl/login_form")
      .addRow("http://howto.basjes.nl/")
      .addRow("http://howto.basjes.nl/")
      .build();

    assertEquals(results.rowCount(), 10);
    new RowSetComparison(expected).verifyAndClearAll(results);
  }


  @Test
  public void testImplicitColumn() throws Exception {
    String sql = "SELECT _raw FROM cp.`httpd/hackers-access-small.httpd`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("_raw", MinorType.VARCHAR)
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow("195.154.46.135 - - [25/Oct/2015:04:11:25 +0100] \"GET /linux/doing-pxe-without-dhcp-control HTTP/1.1\" 200 24323 \"http://howto.basjes.nl/\" \"Mozilla/5.0 (Windows NT 5.1; rv:35.0) Gecko/20100101 Firefox/35.0\"")
      .addRow("23.95.237.180 - - [25/Oct/2015:04:11:26 +0100] \"GET /join_form HTTP/1.0\" 200 11114 \"http://howto.basjes.nl/\" \"Mozilla/5.0 (Windows NT 5.1; rv:35.0) Gecko/20100101 Firefox/35.0\"")
      .addRow("23.95.237.180 - - [25/Oct/2015:04:11:27 +0100] \"POST /join_form HTTP/1.1\" 302 9093 \"http://howto.basjes.nl/join_form\" \"Mozilla/5.0 (Windows NT 5.1; rv:35.0) " +
        "Gecko/20100101 Firefox/35.0\"")
      .addRow("158.222.5.157 - - [25/Oct/2015:04:24:31 +0100] \"GET /join_form HTTP/1.0\" 200 11114 \"http://howto.basjes.nl/\" \"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0 AlexaToolbar/alxf-2.21\"")
      .addRow("158.222.5.157 - - [25/Oct/2015:04:24:32 +0100] \"POST /join_form HTTP/1.1\" 302 9093 \"http://howto.basjes.nl/join_form\" \"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0 AlexaToolbar/alxf-2.21\"")
      .addRow("158.222.5.157 - - [25/Oct/2015:04:24:37 +0100] \"GET /acl_users/credentials_cookie_auth/require_login?came_from=http%3A//howto.basjes.nl/join_form HTTP/1.1\" 200 10716 \"http://howto.basjes.nl/join_form\" \"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0 AlexaToolbar/alxf-2.21\"")
      .addRow("158.222.5.157 - - [25/Oct/2015:04:24:39 +0100] \"GET /login_form HTTP/1.1\" 200 10543 \"http://howto.basjes.nl/\" \"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0 AlexaToolbar/alxf-2.21\"")
      .addRow("158.222.5.157 - - [25/Oct/2015:04:24:41 +0100] \"POST /login_form HTTP/1.1\" 200 16810 \"http://howto.basjes.nl/login_form\" \"Mozilla/5.0 (Windows NT 6.3; WOW64; rv:34.0) Gecko/20100101 Firefox/34.0 AlexaToolbar/alxf-2.21\"")
      .addRow("5.39.5.5 - - [25/Oct/2015:04:32:22 +0100] \"GET /join_form HTTP/1.1\" 200 11114 \"http://howto.basjes.nl/\" \"Mozilla/5.0 (Windows NT 5.1; rv:34.0) Gecko/20100101 Firefox/34.0\"")
      .addRow("180.180.64.16 - - [25/Oct/2015:04:34:37 +0100] \"GET /linux/doing-pxe-without-dhcp-control HTTP/1.1\" 200 24323 \"http://howto.basjes.nl/\" \"Mozilla/5.0 (Windows NT 5.1; rv:35.0) Gecko/20100101 Firefox/35.0\"")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitSomeQuery() throws Exception {
    String sql = "SELECT request_referer_ref, request_receive_time_last_time, request_firstline_uri_protocol FROM cp.`httpd/hackers-access-small.httpd`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("request_referer_ref", MinorType.VARCHAR)
      .addNullable("request_receive_time_last_time", MinorType.TIME)
      .addNullable("request_firstline_uri_protocol", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(null, LocalTime.parse("04:11:25"), null)
      .addRow(null, LocalTime.parse("04:11:26"), null)
      .addRow(null, LocalTime.parse("04:11:27"), null)
      .addRow(null, LocalTime.parse("04:24:31"), null)
      .addRow(null, LocalTime.parse("04:24:32"), null)
      .addRow(null, LocalTime.parse("04:24:37"), null)
      .addRow(null, LocalTime.parse("04:24:39"), null)
      .addRow(null, LocalTime.parse("04:24:41"), null)
      .addRow(null, LocalTime.parse("04:32:22"), null)
      .addRow(null, LocalTime.parse("04:34:37"), null)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitSomeQueryWithCompressedFile() throws Exception {
    generateCompressedFile("httpd/hackers-access-small.httpd", "zip", "httpd/hackers-access-small.httpd.zip" );

    String sql = "SELECT request_referer_ref, request_receive_time_last_time, request_firstline_uri_protocol FROM dfs.`httpd/hackers-access-small.httpd.zip`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("request_referer_ref", MinorType.VARCHAR)
      .addNullable("request_receive_time_last_time", MinorType.TIME)
      .addNullable("request_firstline_uri_protocol", MinorType.VARCHAR)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow(null, LocalTime.parse("04:11:25"), null)
      .addRow(null, LocalTime.parse("04:11:26"), null)
      .addRow(null, LocalTime.parse("04:11:27"), null)
      .addRow(null, LocalTime.parse("04:24:31"), null)
      .addRow(null, LocalTime.parse("04:24:32"), null)
      .addRow(null, LocalTime.parse("04:24:37"), null)
      .addRow(null, LocalTime.parse("04:24:39"), null)
      .addRow(null, LocalTime.parse("04:24:41"), null)
      .addRow(null, LocalTime.parse("04:32:22"), null)
      .addRow(null, LocalTime.parse("04:34:37"), null)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  private TupleMetadata expectedAllFieldsSchema() {
    return new SchemaBuilder()
            .addNullable("connection_client_host", MinorType.VARCHAR)
            .addNullable("connection_client_host_last", MinorType.VARCHAR)
            .addNullable("connection_client_logname", MinorType.BIGINT)
            .addNullable("connection_client_logname_last", MinorType.BIGINT)
            .addNullable("connection_client_user", MinorType.VARCHAR)
            .addNullable("connection_client_user_last", MinorType.VARCHAR)
            .addNullable("request_firstline", MinorType.VARCHAR)
            .addNullable("request_firstline_method", MinorType.VARCHAR)
            .addNullable("request_firstline_original", MinorType.VARCHAR)
            .addNullable("request_firstline_original_method", MinorType.VARCHAR)
            .addNullable("request_firstline_original_protocol", MinorType.VARCHAR)
            .addNullable("request_firstline_original_protocol_version", MinorType.VARCHAR)
            .addNullable("request_firstline_original_uri", MinorType.VARCHAR)
            .addNullable("request_firstline_original_uri_host", MinorType.VARCHAR)
            .addNullable("request_firstline_original_uri_path", MinorType.VARCHAR)
            .addNullable("request_firstline_original_uri_port", MinorType.BIGINT)
            .addNullable("request_firstline_original_uri_protocol", MinorType.VARCHAR)
            .addNullable("request_firstline_original_uri_query", MinorType.VARCHAR)
            .addNullable("request_firstline_original_uri_ref", MinorType.VARCHAR)
            .addNullable("request_firstline_original_uri_userinfo", MinorType.VARCHAR)
            .addNullable("request_firstline_protocol", MinorType.VARCHAR)
            .addNullable("request_firstline_protocol_version", MinorType.VARCHAR)
            .addNullable("request_firstline_uri", MinorType.VARCHAR)
            .addNullable("request_firstline_uri_host", MinorType.VARCHAR)
            .addNullable("request_firstline_uri_path", MinorType.VARCHAR)
            .addNullable("request_firstline_uri_port", MinorType.BIGINT)
            .addNullable("request_firstline_uri_protocol", MinorType.VARCHAR)
            .addNullable("request_firstline_uri_query", MinorType.VARCHAR)
            .addNullable("request_firstline_uri_ref", MinorType.VARCHAR)
            .addNullable("request_firstline_uri_userinfo", MinorType.VARCHAR)
            .addNullable("request_receive_time", MinorType.TIMESTAMP)
            .addNullable("request_receive_time_date", MinorType.DATE)
            .addNullable("request_receive_time_date__utc", MinorType.DATE)
            .addNullable("request_receive_time_day", MinorType.BIGINT)
            .addNullable("request_receive_time_day__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_epoch", MinorType.TIMESTAMP)
            .addNullable("request_receive_time_hour", MinorType.BIGINT)
            .addNullable("request_receive_time_hour__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last", MinorType.TIMESTAMP)
            .addNullable("request_receive_time_last_date", MinorType.DATE)
            .addNullable("request_receive_time_last_date__utc", MinorType.DATE)
            .addNullable("request_receive_time_last_day", MinorType.BIGINT)
            .addNullable("request_receive_time_last_day__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_epoch", MinorType.TIMESTAMP)
            .addNullable("request_receive_time_last_hour", MinorType.BIGINT)
            .addNullable("request_receive_time_last_hour__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_microsecond", MinorType.BIGINT)
            .addNullable("request_receive_time_last_microsecond__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_millisecond", MinorType.BIGINT)
            .addNullable("request_receive_time_last_millisecond__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_minute", MinorType.BIGINT)
            .addNullable("request_receive_time_last_minute__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_month", MinorType.BIGINT)
            .addNullable("request_receive_time_last_month__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_monthname", MinorType.VARCHAR)
            .addNullable("request_receive_time_last_monthname__utc", MinorType.VARCHAR)
            .addNullable("request_receive_time_last_nanosecond", MinorType.BIGINT)
            .addNullable("request_receive_time_last_nanosecond__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_second", MinorType.BIGINT)
            .addNullable("request_receive_time_last_second__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_time", MinorType.TIME)
            .addNullable("request_receive_time_last_time__utc", MinorType.TIME)
            .addNullable("request_receive_time_last_timezone", MinorType.VARCHAR)
            .addNullable("request_receive_time_last_weekofweekyear", MinorType.BIGINT)
            .addNullable("request_receive_time_last_weekofweekyear__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_weekyear", MinorType.BIGINT)
            .addNullable("request_receive_time_last_weekyear__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_last_year", MinorType.BIGINT)
            .addNullable("request_receive_time_last_year__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_microsecond", MinorType.BIGINT)
            .addNullable("request_receive_time_microsecond__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_millisecond", MinorType.BIGINT)
            .addNullable("request_receive_time_millisecond__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_minute", MinorType.BIGINT)
            .addNullable("request_receive_time_minute__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_month", MinorType.BIGINT)
            .addNullable("request_receive_time_month__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_monthname", MinorType.VARCHAR)
            .addNullable("request_receive_time_monthname__utc", MinorType.VARCHAR)
            .addNullable("request_receive_time_nanosecond", MinorType.BIGINT)
            .addNullable("request_receive_time_nanosecond__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_second", MinorType.BIGINT)
            .addNullable("request_receive_time_second__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_time", MinorType.TIME)
            .addNullable("request_receive_time_time__utc", MinorType.TIME)
            .addNullable("request_receive_time_timezone", MinorType.VARCHAR)
            .addNullable("request_receive_time_weekofweekyear", MinorType.BIGINT)
            .addNullable("request_receive_time_weekofweekyear__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_weekyear", MinorType.BIGINT)
            .addNullable("request_receive_time_weekyear__utc", MinorType.BIGINT)
            .addNullable("request_receive_time_year", MinorType.BIGINT)
            .addNullable("request_receive_time_year__utc", MinorType.BIGINT)
            .addNullable("request_referer", MinorType.VARCHAR)
            .addNullable("request_referer_host", MinorType.VARCHAR)
            .addNullable("request_referer_last", MinorType.VARCHAR)
            .addNullable("request_referer_last_host", MinorType.VARCHAR)
            .addNullable("request_referer_last_path", MinorType.VARCHAR)
            .addNullable("request_referer_last_port", MinorType.BIGINT)
            .addNullable("request_referer_last_protocol", MinorType.VARCHAR)
            .addNullable("request_referer_last_query", MinorType.VARCHAR)
            .addNullable("request_referer_last_ref", MinorType.VARCHAR)
            .addNullable("request_referer_last_userinfo", MinorType.VARCHAR)
            .addNullable("request_referer_path", MinorType.VARCHAR)
            .addNullable("request_referer_port", MinorType.BIGINT)
            .addNullable("request_referer_protocol", MinorType.VARCHAR)
            .addNullable("request_referer_query", MinorType.VARCHAR)
            .addNullable("request_referer_ref", MinorType.VARCHAR)
            .addNullable("request_referer_userinfo", MinorType.VARCHAR)
            .addNullable("request_status_last", MinorType.VARCHAR)
            .addNullable("request_user-agent", MinorType.VARCHAR)
            .addNullable("request_user-agent_last", MinorType.VARCHAR)
            .addNullable("response_body_bytes", MinorType.BIGINT)
            .addNullable("response_body_bytes_last", MinorType.BIGINT)
            .addNullable("response_body_bytesclf", MinorType.BIGINT)
            .add("request_firstline_original_uri_query_$", MinorType.MAP)
            .add("request_firstline_uri_query_$", MinorType.MAP)
            .add("request_referer_last_query_$", MinorType.MAP)
            .add("request_referer_query_$", MinorType.MAP)
            .build();
  }

  private RowSet expectedAllFieldsRowSet(TupleMetadata expectedSchema) {
    return client
            .rowSetBuilder(expectedSchema)
            .addRow("195.154.46.135", "195.154.46.135", null, null, null, null,
                    "GET /linux/doing-pxe-without-dhcp-control HTTP/1.1", "GET",
                    "GET /linux/doing-pxe-without-dhcp-control HTTP/1.1", "GET",
                    "HTTP/1.1", "1.1", "/linux/doing-pxe-without-dhcp-control", null, "/linux/doing-pxe-without-dhcp-control", null, null, null, null, null,
                    "HTTP/1.1", "1.1", "/linux/doing-pxe-without-dhcp-control", null, "/linux/doing-pxe-without-dhcp-control", null, null, null, null, null,
                    1445742685000L, LocalDate.parse("2015-10-25"), LocalDate.parse("2015-10-25"), 25, 25, 1445742685000L, 4, 3,
                    1445742685000L, LocalDate.parse("2015-10-25"), LocalDate.parse("2015-10-25"), 25, 25, 1445742685000L, 4, 3,
                    0, 0, 0, 0, 11, 11, 10, 10, "October", "October", 0, 0, 25, 25, LocalTime.parse("04:11:25"), LocalTime.parse("03:11:25"), "+01:00", 43, 43, 2015, 2015, 2015, 2015,
                    0, 0, 0, 0, 11, 11, 10, 10, "October", "October", 0, 0, 25, 25, LocalTime.parse("04:11:25"), LocalTime.parse("03:11:25"), "+01:00", 43, 43, 2015, 2015, 2015, 2015,
                    "http://howto.basjes.nl/", "howto.basjes.nl",
                    "http://howto.basjes.nl/", "howto.basjes.nl",
                    "/", null, "http", null, null, null,
                    "/", null, "http", null, null, null,
                    "200",
                    "Mozilla/5.0 (Windows NT 5.1; rv:35.0) Gecko/20100101 Firefox/35.0",
                    "Mozilla/5.0 (Windows NT 5.1; rv:35.0) Gecko/20100101 Firefox/35.0",
                    24323, 24323, 24323, mapArray(), mapArray(), mapArray(), mapArray())
            .build();
  }

  @Test
  public void testStarRowSet() throws Exception {
    String sql = "SELECT * FROM cp.`httpd/hackers-access-really-small.httpd`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = expectedAllFieldsSchema();

    RowSet expectedRowSet = expectedAllFieldsRowSet(expectedSchema);
    new RowSetComparison(expectedRowSet).verifyAndClearAll(results);
  }

  @Test
  public void testExplicitAllFields() throws Exception {
    TupleMetadata expectedSchema = expectedAllFieldsSchema();

    // To avoid typos we generate the SQL from the schema.
    String sql = "SELECT `" +
            expectedSchema
                    .toFieldList()
                    .stream()
                    .map(MaterializedField::getName)
                    .collect(Collectors.joining("`, `")) +
            "` FROM cp.`httpd/hackers-access-really-small.httpd`";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    RowSet expectedRowSet = expectedAllFieldsRowSet(expectedSchema);
    new RowSetComparison(expectedRowSet).verifyAndClearAll(results);
  }

  @Test
  public void testInvalidFormat() throws Exception {
    String sql = "SELECT * FROM cp.`httpd/dfs-bootstrap.httpd`";
    try {
      run(sql);
      fail();
    } catch (DrillRuntimeException e) {
      assertTrue(e.getMessage().contains("Error reading HTTPD file "));
    }
  }
}
