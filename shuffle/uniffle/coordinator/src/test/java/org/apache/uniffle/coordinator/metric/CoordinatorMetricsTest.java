/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.coordinator.metric;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.config.RssBaseConf;
import org.apache.uniffle.common.metrics.TestUtils;
import org.apache.uniffle.coordinator.CoordinatorConf;
import org.apache.uniffle.coordinator.CoordinatorServer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CoordinatorMetricsTest {

  private static final String SERVER_METRICS_URL = "http://127.0.0.1:12345/metrics/server";
  private static final String SERVER_JVM_URL = "http://127.0.0.1:12345/metrics/jvm";
  private static final String SERVER_GRPC_URL = "http://127.0.0.1:12345/metrics/grpc";
  private static CoordinatorServer coordinatorServer;

  @BeforeAll
  public static void setUp() throws Exception {
    String remotePath1 = "hdfs://path1";
    File cfgFile = Files.createTempFile("coordinatorMetricsTest", ".conf").toFile();
    cfgFile.deleteOnExit();
    writeRemoteStorageConf(cfgFile, remotePath1);

    CoordinatorConf coordinatorConf = new CoordinatorConf();
    coordinatorConf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_UPDATE_INTERVAL_SEC, 10);
    coordinatorConf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_PATH, cfgFile.toURI().toString());
    coordinatorConf.set(CoordinatorConf.COORDINATOR_DYNAMIC_CLIENT_CONF_ENABLED, true);
    coordinatorConf.set(RssBaseConf.JETTY_HTTP_PORT, 12345);
    coordinatorConf.set(RssBaseConf.JETTY_CORE_POOL_SIZE, 128);
    coordinatorConf.set(RssBaseConf.RPC_SERVER_PORT, 12346);
    coordinatorServer = new CoordinatorServer(coordinatorConf);
    coordinatorServer.start();
  }

  @AfterAll
  public static void tearDown() throws Exception {
    coordinatorServer.stopServer();
  }

  @Test
  public void testDynamicMetrics() throws Exception {
    String content = TestUtils.httpGetMetrics(SERVER_METRICS_URL);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode metricsNode = mapper.readTree(content).get("metrics");
    String remoteStorageMetricsName = CoordinatorMetrics.REMOTE_STORAGE_IN_USED_PREFIX + "path1";
    boolean bingo = false;
    for (int i = 0; i < metricsNode.size(); i++) {
      JsonNode metricsName = metricsNode.get(i).get("name");
      if (remoteStorageMetricsName.equals(metricsName.textValue())) {
        bingo = true;
        break;
      }
    }
    assertTrue(bingo);
  }

  @Test
  public void testCoordinatorMetrics() throws Exception {
    String content = TestUtils.httpGetMetrics(SERVER_METRICS_URL);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualObj = mapper.readTree(content);
    assertEquals(2, actualObj.size());
    assertEquals(10, actualObj.get("metrics").size());
  }

  @Test
  public void testJvmMetrics() throws Exception {
    String content = TestUtils.httpGetMetrics(SERVER_JVM_URL);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualObj = mapper.readTree(content);
    assertEquals(2, actualObj.size());
  }

  @Test
  public void testGrpcMetrics() throws Exception {
    String content = TestUtils.httpGetMetrics(SERVER_GRPC_URL);
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualObj = mapper.readTree(content);
    assertEquals(2, actualObj.size());
    assertEquals(9, actualObj.get("metrics").size());
  }

  private static void writeRemoteStorageConf(File cfgFile, String value) throws Exception {
    FileWriter fileWriter = new FileWriter(cfgFile);
    PrintWriter printWriter = new PrintWriter(fileWriter);
    printWriter.println(CoordinatorConf.COORDINATOR_REMOTE_STORAGE_PATH.key() + " " + value);
    printWriter.flush();
    printWriter.close();
  }
}
