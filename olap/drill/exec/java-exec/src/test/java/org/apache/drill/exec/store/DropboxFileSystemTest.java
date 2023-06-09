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

package org.apache.drill.exec.store;

import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.physical.rowSet.RowSet;
import org.apache.drill.exec.record.metadata.SchemaBuilder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;
import org.apache.drill.exec.store.easy.json.JSONFormatPlugin.JSONFormatConfig;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin.TextFormatConfig;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.apache.drill.test.rowSet.RowSetComparison;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Ignore("Please create a Dropbox API key and run these tests manually")
public class DropboxFileSystemTest extends ClusterTest {

  private static final String ACCESS_TOKEN = "<Your Dropbox Access Token Here>";

  /*
   All test files can be found in java-exec/src/test/resources/dropboxTestFiles

  Instructions for running Dropbox Unit Tests
  1.  Get your Dropbox API key as explained in the Drill docs and paste it above into the ACCESS_TOKEN variable.
  2.  In your dropbox account, create a folder called 'csv' and upload the file hdf-test.csvh into that folder
  3.  In your dropbox account, upload the file http-pcap.json to the root directory of your dropbox account
  4.  In the testListFiles test, you will have to update the modified dates
  5.  Run tests.
   */

  @BeforeClass
  public static void setup() throws Exception {
    assertTrue(! ACCESS_TOKEN.equalsIgnoreCase("<Your Dropbox Access Token Here>"));
    ClusterTest.startCluster(ClusterFixture.builder(dirTestWatcher));

    Map<String, String> dropboxConfigVars = new HashMap<>();
    dropboxConfigVars.put("dropboxAccessToken", ACCESS_TOKEN);

    // Create workspaces
    WorkspaceConfig rootWorkspace = new WorkspaceConfig("/", false, null, false);
    WorkspaceConfig csvWorkspace = new WorkspaceConfig("/csv", false, null, false);
    Map<String, WorkspaceConfig> workspaces = new HashMap<>();
    workspaces.put("root", rootWorkspace);
    workspaces.put("csv", csvWorkspace);

    // Add formats
    Map<String, FormatPluginConfig> formats = new HashMap<>();
    List<String> jsonExtensions = new ArrayList<>();
    jsonExtensions.add("json");
    FormatPluginConfig jsonFormatConfig = new JSONFormatConfig(jsonExtensions);

    // CSV Format
    List<String> csvExtensions = new ArrayList<>();
    csvExtensions.add("csv");
    csvExtensions.add("csvh");
    FormatPluginConfig csvFormatConfig = new TextFormatConfig(csvExtensions, "\n", ",", "\"", null, null, false, true);


    StoragePluginConfig dropboxConfig = new FileSystemConfig("dropbox:///", dropboxConfigVars, workspaces, formats, null);
    dropboxConfig.setEnabled(true);

    cluster.defineStoragePlugin("dropbox_test", dropboxConfig);
    cluster.defineFormat("dropbox_test", "json", jsonFormatConfig);
    cluster.defineFormat("dropbox_test", "csv", csvFormatConfig);
  }

  @Test
  @Ignore("Please create a Dropbox API key and run this test manually")
  public void testListFiles() throws Exception {
    String sql = "SHOW FILES IN dropbox_test.root";
    RowSet results = client.queryBuilder().sql(sql).rowSet();
    assertEquals(3, results.rowCount());

    TupleMetadata expectedSchema = new SchemaBuilder()
      .addNullable("name", MinorType.VARCHAR)
      .add("isDirectory", MinorType.BIT)
      .add("isFile", MinorType.BIT)
      .add("length", MinorType.BIGINT)
      .add("owner", MinorType.VARCHAR, DataMode.OPTIONAL)
      .add("group", MinorType.VARCHAR, DataMode.OPTIONAL)
      .add("permissions", MinorType.VARCHAR, DataMode.OPTIONAL)
      .add("accessTime", MinorType.TIMESTAMP, DataMode.OPTIONAL)
      .add("modificationTime", MinorType.TIMESTAMP, DataMode.OPTIONAL)
      .buildSchema();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow("csv", true, false, 0, "", "", "rwxrwxrwx", -18000000L, -18000000L)
      .addRow("http-pcap.json", false, true, 22898, "", "", "rw-rw-rw-", -18000000L, 1614644137000L)
      .addRow("icon.png", false, true, 111808, "", "", "rw-rw-rw-", -18000000L, 1615112058000L)
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);

  }

  @Test
  @Ignore("Please create a Dropbox API key and run this test manually")
  public void testJSONQuery() throws Exception {
    String sql = "SELECT `time` AS packet_time, Ethernet, " +
      "`timestamp` AS packet_timestamp, " +
      "IP, TCP, UDP, DNS " +
      "FROM dropbox_test.`/http-pcap.json` LIMIT 5";

    RowSet results = client.queryBuilder().sql(sql).rowSet();
    assertEquals(5, results.rowCount());
    assertEquals(7,results.batchSchema().getFieldCount());
  }

  @Test
  @Ignore("Please create a Dropbox API key and run this test manually")
  public void testCSVQuery() throws Exception {
    String sql = "select * from dropbox_test.`csv/hdf-test.csv` LIMIT 5";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("id", MinorType.VARCHAR)
      .add("first_name", MinorType.VARCHAR)
      .add("last_name", MinorType.VARCHAR)
      .add("email", MinorType.VARCHAR)
      .add("gender", MinorType.VARCHAR)
      .add("birthday", MinorType.VARCHAR)
      .add("order_count", MinorType.VARCHAR)
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow("1", "Wandie", "Osburn", "wosburn0@phoca.cz", "Female", "1971-03-27", "45")
      .addRow("2", "Paddie", "Cosans", "pcosans1@gov.uk", "Male", "1960-09-21", "97")
      .addRow("3", "Melisande", "Targett", "mtargett2@networksolutions.com", "Female", "1952-02-28", "61")
      .addRow( "4", "Kristofor", "Levi", "klevi3@sciencedaily.com", "Male", "1985-07-14", "72")
      .addRow("5", "Heinrik", "Emanuelli", "hemanuelli4@mapquest.com", "Male", "2009-05-28", "29")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }

  @Test
  @Ignore("Please create a Dropbox API key and run this test manually")
  public void testCSVQueryWithWorkspace() throws Exception {
    String sql = "select * from dropbox_test.csv.`hdf-test.csv` LIMIT 5";
    RowSet results = client.queryBuilder().sql(sql).rowSet();

    TupleMetadata expectedSchema = new SchemaBuilder()
      .add("id", MinorType.VARCHAR)
      .add("first_name", MinorType.VARCHAR)
      .add("last_name", MinorType.VARCHAR)
      .add("email", MinorType.VARCHAR)
      .add("gender", MinorType.VARCHAR)
      .add("birthday", MinorType.VARCHAR)
      .add("order_count", MinorType.VARCHAR)
      .build();

    RowSet expected = client.rowSetBuilder(expectedSchema)
      .addRow("1", "Wandie", "Osburn", "wosburn0@phoca.cz", "Female", "1971-03-27", "45")
      .addRow("2", "Paddie", "Cosans", "pcosans1@gov.uk", "Male", "1960-09-21", "97")
      .addRow("3", "Melisande", "Targett", "mtargett2@networksolutions.com", "Female", "1952-02-28", "61")
      .addRow( "4", "Kristofor", "Levi", "klevi3@sciencedaily.com", "Male", "1985-07-14", "72")
      .addRow("5", "Heinrik", "Emanuelli", "hemanuelli4@mapquest.com", "Male", "2009-05-28", "29")
      .build();

    new RowSetComparison(expected).verifyAndClearAll(results);
  }
}
