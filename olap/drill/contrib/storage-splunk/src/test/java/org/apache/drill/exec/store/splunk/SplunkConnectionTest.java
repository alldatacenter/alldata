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

import com.splunk.EntityCollection;
import com.splunk.Index;
import org.apache.drill.common.exceptions.UserException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.drill.exec.store.splunk.SplunkTestSuite.SPLUNK_STORAGE_PLUGIN_CONFIG;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SplunkConnectionTest extends SplunkBaseTest {

  @Test
  public void testConnection() {
    SplunkConnection sc = new SplunkConnection(SPLUNK_STORAGE_PLUGIN_CONFIG);
    sc.connect();
  }

  @Test
  public void testConnectionFail() {
    try {
      SplunkPluginConfig invalidSplunkConfig = new SplunkPluginConfig(
              "hacker",
              "hacker",
              SPLUNK_STORAGE_PLUGIN_CONFIG.getHostname(),
              SPLUNK_STORAGE_PLUGIN_CONFIG.getPort(),
              SPLUNK_STORAGE_PLUGIN_CONFIG.getEarliestTime(),
              SPLUNK_STORAGE_PLUGIN_CONFIG.getLatestTime(),
              null,
              SPLUNK_STORAGE_PLUGIN_CONFIG.getReconnectRetries()
      );
      SplunkConnection sc = new SplunkConnection(invalidSplunkConfig);
      sc.connect();
      fail();
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("CONNECTION ERROR: Unable to connect to Splunk"));
    }
  }

  @Test
  public void testGetIndexes() {
    SplunkConnection sc = new SplunkConnection(SPLUNK_STORAGE_PLUGIN_CONFIG);
    EntityCollection<Index> indexes = sc.getIndexes();
    assertEquals(9, indexes.size());

    List<String> expectedIndexNames = new ArrayList<>();
    expectedIndexNames.add("_audit");
    expectedIndexNames.add("_internal");
    expectedIndexNames.add("_introspection");
    expectedIndexNames.add("_telemetry");
    expectedIndexNames.add("_thefishbucket");
    expectedIndexNames.add("history");
    expectedIndexNames.add("main");
    expectedIndexNames.add("splunklogger");
    expectedIndexNames.add("summary");

    List<String> indexNames = new ArrayList<>();
    for (Index index : indexes.values()) {
      indexNames.add(index.getName());
    }

    assertEquals(indexNames, expectedIndexNames);

  }
}
