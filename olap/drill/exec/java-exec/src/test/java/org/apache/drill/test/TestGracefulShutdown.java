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
package org.apache.drill.test;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.server.Drillbit;
import org.apache.drill.exec.server.rest.WebServer;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category({SlowTest.class})
public class TestGracefulShutdown extends ClusterTest {

  @Rule
  public final TestRule TIMEOUT = TestTools.getTimeoutRule(120_000);

  @BeforeClass
  public static void setUpTestData() throws Exception {
    for (int i = 0; i < 300; i++) {
      setupFile(i);
    }
  }

  private static ClusterFixtureBuilder builderWithEnabledWebServer() {
    return builderWithEnabledPortHunting()
        .configProperty(ExecConstants.HTTP_ENABLE, true)
        .configProperty(ExecConstants.HTTP_PORT_HUNT, true)
        .configProperty(ExecConstants.SLICE_TARGET, 10);
  }

  private static ClusterFixtureBuilder builderWithEnabledPortHunting() {
    return ClusterFixture.builder(dirTestWatcher)
        .configProperty(ExecConstants.DRILL_PORT_HUNT, true)
        .configProperty(ExecConstants.GRACE_PERIOD, 500)
        .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true);
  }

  /*
  Start multiple drillbits and then shutdown a drillbit. Query the online
  endpoints and check if the drillbit still exists.
   */
  @Test
  public void testOnlineEndPoints() throws Exception {

    String[] drillbits = {"db1", "db2", "db3"};
    ClusterFixtureBuilder builder = builderWithEnabledPortHunting()
        .withLocalZk()
        .withBits(drillbits);

    try (ClusterFixture cluster = builder.build()) {

      Drillbit drillbit = cluster.drillbit("db2");
      int zkRefresh = drillbit.getContext().getConfig().getInt(ExecConstants.ZK_REFRESH);
      DrillbitEndpoint drillbitEndpoint = drillbit.getRegistrationHandle().getEndPoint();
      cluster.closeDrillbit("db2");

      while (true) {
        Collection<DrillbitEndpoint> drillbitEndpoints = cluster.drillbit()
            .getContext()
            .getClusterCoordinator()
            .getOnlineEndPoints();

        if (!drillbitEndpoints.contains(drillbitEndpoint)) {
          // Success
          return;
        }

        Thread.sleep(zkRefresh);
      }
    }
  }

  /*
   Test shutdown through RestApi
   */
  @Test
  public void testRestApi() throws Exception {

    String[] drillbits = {"db1", "db2", "db3"};
    ClusterFixtureBuilder builder = builderWithEnabledWebServer()
        .withLocalZk()
        .withBits(drillbits);

    QueryBuilder.QuerySummaryFuture listener;
    final String sql = "select * from dfs.root.`.`";
    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      Drillbit drillbit = cluster.drillbit("db1");
      int port = drillbit.getWebServerPort();
      int zkRefresh = drillbit.getContext().getConfig().getInt(ExecConstants.ZK_REFRESH);
      listener = client.queryBuilder().sql(sql).futureSummary();
      URL url = new URL("http://localhost:" + port + "/gracefulShutdown");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      if (conn.getResponseCode() != 200) {
        throw new RuntimeException("Failed : HTTP error code : "
                + conn.getResponseCode());
      }
      while (!listener.isDone()) {
        Thread.sleep(100L);
      }

      if (waitAndAssertDrillbitCount(cluster, zkRefresh, drillbits.length)) {
        return;
      }
      Assert.fail("Timed out");
    }
  }

  /*
   Test default shutdown through RestApi
   */
  @Test
  public void testRestApiShutdown() throws Exception {

    String[] drillbits = {"db1", "db2", "db3"};
    ClusterFixtureBuilder builder = builderWithEnabledWebServer().withLocalZk().withBits(drillbits);

    QueryBuilder.QuerySummaryFuture listener;
    final String sql = "select * from dfs.root.`.`";
    try (ClusterFixture cluster = builder.build();
         final ClientFixture client = cluster.clientFixture()) {
      Drillbit drillbit = cluster.drillbit("db1");
      int port = drillbit.getWebServerPort();
      int zkRefresh = drillbit.getContext().getConfig().getInt(ExecConstants.ZK_REFRESH);
      listener = client.queryBuilder().sql(sql).futureSummary();
      while (!listener.isDone()) {
        Thread.sleep(100L);
      }
      URL url = new URL("http://localhost:" + port + "/shutdown");
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("POST");
      if (conn.getResponseCode() != 200) {
        throw new RuntimeException("Failed : HTTP error code : "
                + conn.getResponseCode());
      }
      if (waitAndAssertDrillbitCount(cluster, zkRefresh, drillbits.length)) {
        return;
      }
      Assert.fail("Timed out");
    }
  }

  @Test // DRILL-6912
  public void testDrillbitWithSamePortContainsShutdownThread() throws Exception {
    ClusterFixtureBuilder fixtureBuilder = ClusterFixture.builder(dirTestWatcher)
        .withLocalZk()
        .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
        .configProperty(ExecConstants.INITIAL_USER_PORT, QueryTestUtil.getFreePortNumber(31170, 300))
        .configProperty(ExecConstants.INITIAL_BIT_PORT, QueryTestUtil.getFreePortNumber(31180, 300));

    try (ClusterFixture fixture = fixtureBuilder.build();
         Drillbit drillbitWithSamePort = new Drillbit(fixture.config(),
             fixtureBuilder.configBuilder().getDefinitions(), fixture.serviceSet())) {
      // Assert preconditions :
      //      1. First drillbit instance should be started normally
      //      2. Second instance startup should fail, because ports are occupied by the first one
      assertNotNull("First drillbit instance should be initialized", fixture.drillbit());
      try {
        drillbitWithSamePort.run();
        fail("Invocation of 'drillbitWithSamePort.run()' should throw UserException");
      } catch (UserException e) {
        assertThat(e.getMessage(), containsString("RESOURCE ERROR: Drillbit could not bind to port"));
        // Ensure that drillbit with failed startup may be safely closed
        assertNotNull("Drillbit.gracefulShutdownThread shouldn't be null, otherwise close() may throw NPE (if so, check suppressed exception).",
            drillbitWithSamePort.getGracefulShutdownThread());
      }
    }
  }

  @Test // DRILL-7056
  public void testDrillbitTempDir() throws Exception {
    File originalDrillbitTempDir;
    ClusterFixtureBuilder fixtureBuilder = ClusterFixture.builder(dirTestWatcher).withLocalZk()
        .configProperty(ExecConstants.ALLOW_LOOPBACK_ADDRESS_BINDING, true)
        .configProperty(ExecConstants.INITIAL_USER_PORT, QueryTestUtil.getFreePortNumber(31170, 300))
        .configProperty(ExecConstants.INITIAL_BIT_PORT, QueryTestUtil.getFreePortNumber(31180, 300));

    try (ClusterFixture fixture = fixtureBuilder.build();
         Drillbit twinDrillbitOnSamePort = new Drillbit(fixture.config(),
            fixtureBuilder.configBuilder().getDefinitions(), fixture.serviceSet())) {
      // Assert preconditions :
      //      1. First drillbit instance should be started normally
      //      2. Second instance startup should fail, because ports are occupied by the first one
      Drillbit originalDrillbit = fixture.drillbit();
      assertNotNull("First drillbit instance should be initialized", originalDrillbit);
      originalDrillbitTempDir = getWebServerTempDirPath(originalDrillbit);
      assertTrue("First drillbit instance should have a temporary Javascript dir initialized", originalDrillbitTempDir.exists());
      try {
        twinDrillbitOnSamePort.run();
        fail("Invocation of 'twinDrillbitOnSamePort.run()' should throw UserException");
      } catch (UserException userEx) {
        assertThat(userEx.getMessage(), containsString("RESOURCE ERROR: Drillbit could not bind to port"));
      }
    }
    // Verify deletion
    assertFalse("First drillbit instance should have a temporary Javascript dir deleted", originalDrillbitTempDir.exists());
  }

  private File getWebServerTempDirPath(Drillbit drillbit) throws IllegalAccessException {
    Field webServerField = FieldUtils.getField(drillbit.getClass(), "webServer", true);
    WebServer webServerHandle = (WebServer) FieldUtils.readField(webServerField, drillbit, true);
    return webServerHandle.getOrCreateTmpJavaScriptDir();
  }

  private boolean waitAndAssertDrillbitCount(ClusterFixture cluster, int zkRefresh, int bitsNum)
      throws InterruptedException {

    while (true) {
      Collection<DrillbitEndpoint> drillbitEndpoints = cluster.drillbit()
              .getContext()
              .getClusterCoordinator()
              .getAvailableEndpoints();
      if (drillbitEndpoints.size() == bitsNum - 1) {
        return true;
      }
      Thread.sleep(zkRefresh);
    }
  }

  private static void setupFile(int file_num) throws Exception {
    String file = "employee" + file_num + ".json";
    Path path = dirTestWatcher.getRootDir().toPath().resolve(file);
    StringBuilder stringBuilder = new StringBuilder();
    int rowsCount = 7;
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(TestGracefulShutdown.class.getResourceAsStream("/employee.json")))) {
      for (int i = 0; i < rowsCount; i++) {
        stringBuilder.append(reader.readLine());
      }
    }
    String content = stringBuilder.toString();
    try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path.toFile(), true)))) {
      out.println(content);
    }
  }

}
