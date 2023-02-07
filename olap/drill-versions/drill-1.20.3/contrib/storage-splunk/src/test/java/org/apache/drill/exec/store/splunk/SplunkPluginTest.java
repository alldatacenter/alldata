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
package org.apache.drill.exec.store.splunk;

import com.splunk.Service;
import com.splunk.ServiceArgs;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.physical.rowSet.RowSetBuilder;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.test.rowSet.RowSetUtilities;
import org.junit.FixMethodOrder;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.apache.drill.exec.store.splunk.SplunkTestSuite.SPLUNK_STORAGE_PLUGIN_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.times;

@FixMethodOrder(MethodSorters.JVM)
@Category({SlowTest.class})
public class SplunkPluginTest extends SplunkBaseTest {

  @Test
  public void verifyPluginConfig() throws Exception {
    String sql = "SELECT SCHEMA_NAME, TYPE FROM INFORMATION_SCHEMA.`SCHEMATA` WHERE TYPE='splunk'\n" +
      "ORDER BY SCHEMA_NAME";

    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("SCHEMA_NAME", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("TYPE", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("splunk", "splunk")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void verifyIndexes() throws Exception {
    String sql = "SHOW TABLES IN `splunk`";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("TABLE_SCHEMA", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("TABLE_NAME", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("splunk", "summary")
      .addRow("splunk", "splunklogger")
      .addRow("splunk", "_thefishbucket")
      .addRow("splunk", "_audit")
      .addRow("splunk", "_internal")
      .addRow("splunk", "_introspection")
      .addRow("splunk", "main")
      .addRow("splunk", "history")
      .addRow("splunk", "spl")
      .addRow("splunk", "_telemetry")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  @Ignore("timestamp parsing error in antlr generated code")
  public void testStarQuery() throws Exception {
    String sql = "SELECT * FROM splunk._telemetry LIMIT 1";
    client.testBuilder()
      .sqlQuery(sql)
      .baselineColumns("acceleration_id", "action", "add_offset", "add_timestamp", "apiEndTime", "apiStartTime",
              "api_et", "api_lt", "app", "autojoin", "available_count", "buckets", "cache_size", "clientip",
              "considered_events", "data_format", "decompressed_slices", "drop_count", "duration_command_search_index",
              "duration_command_search_index_bucketcache_hit", "duration_command_search_index_bucketcache_miss",
              "duration_command_search_rawdata", "duration_command_search_rawdata_bucketcache_hit",
              "duration_command_search_rawdata_bucketcache_miss", "earliest", "elapsed_ms", "eliminated_buckets",
              "enable_lookups", "event_count", "eventtype", "exec_time", "extra_fields", "field1", "format",
              "fully_completed_search", "has_error_msg", "host", "index", "info",
              "invocations_command_search_index_bucketcache_error", "invocations_command_search_index_bucketcache_hit",
              "invocations_command_search_index_bucketcache_miss", "invocations_command_search_rawdata_bucketcache_error",
              "invocations_command_search_rawdata_bucketcache_hit", "invocations_command_search_rawdata_bucketcache_miss",
              "is_realtime", "latest", "linecount", "max_count", "maxtime", "mode", "multiValueField", "object",
              "operation", "provenance", "reaso", "result_count", "roles", "savedsearch_name", "scan_count", "search",
              "search_et", "search_id", "search_lt", "search_startup_time", "searched_buckets", "segmentation", "session",
               "source", "sourcetype", "sourcetype_count__audittrail", "sourcetype_count__first_install_too_small",
              "sourcetype_count__http_event_collector_metrics", "sourcetype_count__kvstore", "sourcetype_count__mongod",
              "sourcetype_count__scheduler", "sourcetype_count__search_telemetry", "sourcetype_count__splunk_resource_usage",
              "sourcetype_count__splunk_version", "sourcetype_count__splunk_web_access", "sourcetype_count__splunk_web_service",
              "sourcetype_count__splunkd", "sourcetype_count__splunkd_access", "sourcetype_count__splunkd_conf",
              "sourcetype_count__splunkd_stderr", "sourcetype_count__splunkd_ui_access", "splunk_server",
              "splunk_server_group", "subsecond", "timestamp", "total_run_time", "total_slices", "ttl", "user", "useragent",
              "_bkt", "_cd", "_eventtype_color", "_indextime", "_kv", "_raw", "_serial", "_si", "_sourcetype", "_subsecond",
              "_time")
      .expectsNumRecords(1)
      .go();
  }

  @Test
  @Ignore("the result is not consistent on system tables")
  public void testRawSPLQuery() throws Exception {
    String sql = "SELECT * FROM splunk.spl WHERE spl = 'search index=_internal earliest=1 latest=now | fieldsummary'";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("field", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("count", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("distinct_count", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("is_exact", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("max", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("mean", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("min", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("numeric_count", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("stdev", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("values", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("index", "0", "0", "1", null, null, null, "0", null, "[]")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testExplictFieldsQuery() throws Exception {
    String sql = "SELECT acceleration_id, action, add_offset, add_timestamp FROM splunk._audit LIMIT 2";

    client.testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns("acceleration_id", "action", "add_offset", "add_timestamp")
      .expectsNumRecords(2)
      .go();
  }

  @Test
  public void testExplicitFieldsWithLimitQuery() throws Exception {
    String sql = "SELECT action, _sourcetype, _subsecond, _time FROM splunk._audit LIMIT 3";
    client.testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns( "acceleration_id", "action", "add_offset", "add_timestamp")
      .expectsNumRecords(3)
      .go();
  }

  @Test
  @Ignore("the result is not consistent on system tables")
  public void testExplicitFieldsWithSourceType() throws Exception {
    String sql = "SELECT action, _sourcetype, _subsecond, _time FROM splunk._audit WHERE sourcetype='audittrail' LIMIT 5";
    client.testBuilder()
      .sqlQuery(sql)
      .unOrdered()
      .baselineColumns( "acceleration_id", "action", "add_offset", "add_timestamp")
      .expectsNumRecords(5)
      .go();
  }

  @Test
  public void testExplicitFieldsWithOneFieldLimitQuery() throws Exception {
    String sql = "SELECT `component` FROM splunk.`_introspection` ORDER BY `component` LIMIT 2";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("component", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("Dispatch")
      .addRow("Fishbucket")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  @Ignore("the result is not consistent on system tables. The table may be empty before test running")
  public void testSingleEqualityFilterQuery() throws Exception {
    String sql = "SELECT action, _sourcetype FROM splunk._audit where action='edit'";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("action", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("_sourcetype", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("edit", "audittrail")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  @Ignore("the result is not consistent on system tables")
  public void testMultipleEqualityFilterQuery() throws Exception {
    String sql = "SELECT _time, clientip, file, host FROM splunk.main WHERE file='cart.do' AND clientip='217.15.20.146'";
    client.testBuilder()
      .sqlQuery(sql)
      .ordered()
      .expectsNumRecords(164)
      .go();
  }

  @Test
  public void testFilterOnUnProjectedColumnQuery() throws Exception {
    String sql = "SELECT action, _sourcetype, _subsecond, _time FROM splunk._audit WHERE sourcetype='audittrail' LIMIT 5";
    client.testBuilder()
        .sqlQuery(sql)
        .unOrdered()
        .baselineColumns( "acceleration_id", "action", "add_offset", "add_timestamp")
        .expectsNumRecords(5)
        .go();
  }

  @Test
  @Ignore("the result is not consistent on system tables")
  public void testGreaterThanFilterQuery() throws Exception {
    String sql = "SELECT clientip, file, bytes FROM splunk.main WHERE bytes > 40000";
    client.testBuilder()
      .sqlQuery(sql)
      .ordered()
      .expectsNumRecords(235)
      .go();
  }

  @Test
  public void testArbitrarySPL() throws Exception {
    String sql = "SELECT field1, _mkv_child, multiValueField FROM splunk.spl WHERE spl='|noop| makeresults | eval field1 = \"abc def ghi jkl mno pqr stu vwx yz\" | makemv field1 | mvexpand " +
      "field1 | eval " +
      "multiValueField = \"cat dog bird\" | makemv multiValueField' LIMIT 10\n";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("field1", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("_mkv_child", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .add("multiValueField", TypeProtos.MinorType.VARCHAR, TypeProtos.DataMode.OPTIONAL)
      .build();

    RowSet expected = new RowSetBuilder(client.allocator(), expectedSchema)
      .addRow("abc", "0", "cat dog bird")
      .addRow("def", "1", "cat dog bird")
      .addRow("ghi", "2", "cat dog bird")
      .addRow("jkl", "3", "cat dog bird")
      .addRow("mno", "4", "cat dog bird")
      .addRow("pqr", "5", "cat dog bird")
      .addRow("stu", "6", "cat dog bird")
      .addRow("vwx", "7", "cat dog bird")
      .addRow("yz", "8", "cat dog bird")
      .build();

    RowSetUtilities.verify(expected, results);
  }

  @Test
  public void testSPLQueryWithMissingSPL() throws Exception {
    String sql = "SELECT * FROM splunk.spl";
    try {
      client.queryBuilder().sql(sql).rowSet();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("SPL cannot be empty when querying spl table"));
    }
  }

  @Test
  public void testSerDe() throws Exception {
    String sql = "select min(linecount) from splunk._audit";
    String plan = queryBuilder().sql(sql).explainJson();
    int cnt = queryBuilder().physical(plan).singletonInt();
    assertEquals("Counts should match", 1, cnt);
  }

  @Test
  public void testReconnectRetries() {
    try (MockedStatic<Service> splunk = Mockito.mockStatic(Service.class)) {
      ServiceArgs loginArgs = new ServiceArgs();
      loginArgs.setHost(SPLUNK_STORAGE_PLUGIN_CONFIG.getHostname());
      loginArgs.setPort(SPLUNK_STORAGE_PLUGIN_CONFIG.getPort());
      loginArgs.setPassword(SPLUNK_STORAGE_PLUGIN_CONFIG.getPassword());
      loginArgs.setUsername(SPLUNK_STORAGE_PLUGIN_CONFIG.getUsername());
      splunk.when(() -> Service.connect(loginArgs))
          .thenThrow(new RuntimeException("Fail first connection to Splunk"))
          .thenThrow(new RuntimeException("Fail second connection to Splunk"))
          .thenThrow(new RuntimeException("Fail third connection to Splunk"))
          .thenReturn(new Service(loginArgs)); // fourth connection is successful
      new SplunkConnection(SPLUNK_STORAGE_PLUGIN_CONFIG); // it will fail, in case "reconnectRetries": 1 is specified in configs
      splunk.verify(
          () -> Service.connect(loginArgs),
          times(4)
      );
    }
  }
}
