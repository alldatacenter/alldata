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
package org.apache.drill.exec.impersonation;

import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.drill.exec.store.avro.AvroDataGenerator;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;
import org.apache.drill.categories.SlowTest;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

import static org.hamcrest.core.StringContains.containsString;
import static org.junit.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertNull;

/**
 * Test queries involving direct impersonation and multilevel impersonation including join queries where each side is
 * a nested view.
 */
@Category({SlowTest.class, SecurityTest.class})
public class TestImpersonationQueries extends BaseTestImpersonation {
  @BeforeClass
  public static void setup() throws Exception {
    startMiniDfsCluster(TestImpersonationQueries.class.getSimpleName());
    startDrillCluster(true);
    addMiniDfsBasedStorage(createTestWorkspaces());
    createTestData();
  }

  private static void createTestData() throws Exception {
    // Create test tables/views

    // Create copy of "lineitem" table in /user/user0_1 owned by user0_1:group0_1 with permissions 750. Only user0_1
    // has access to data in created "lineitem" table.
    createTestTable(org1Users[0], org1Groups[0], "lineitem");

    // Create copy of "orders" table in /user/user0_2 owned by user0_2:group0_2 with permissions 750. Only user0_2
    // has access to data in created "orders" table.
    createTestTable(org2Users[0], org2Groups[0], "orders");

    createNestedTestViewsOnLineItem();
    createNestedTestViewsOnOrders();
    createRecordReadersData(org1Users[0], org1Groups[0]);
  }

  private static Map<String, WorkspaceConfig> createTestWorkspaces() throws Exception {
    // Create "/tmp" folder and set permissions to "777"
    final Path tmpPath = new Path("/tmp");
    fs.delete(tmpPath, true);
    FileSystem.mkdirs(fs, tmpPath, new FsPermission((short)0777));

    Map<String, WorkspaceConfig> workspaces = Maps.newHashMap();

    // create user directory (ex. "/user/user0_1", with ownership "user0_1:group0_1" and perms 755) for every user.
    for (int i = 0; i < org1Users.length; i++) {
      final String user = org1Users[i];
      final String group = org1Groups[i];
      createAndAddWorkspace(user, getUserHome(user), (short)0755, user, group, workspaces);
    }

    // create user directory (ex. "/user/user0_2", with ownership "user0_2:group0_2" and perms 755) for every user.
    for (int i = 0; i < org2Users.length; i++) {
      final String user = org2Users[i];
      final String group = org2Groups[i];
      createAndAddWorkspace(user, getUserHome(user), (short)0755, user, group, workspaces);
    }

    return workspaces;
  }

  private static void createTestTable(String user, String group, String tableName) throws Exception {
    updateClient(user);
    test("USE " + getWSSchema(user));
    test("CREATE TABLE %s as SELECT * FROM cp.`tpch/%s.parquet`", tableName, tableName);

    // Change the ownership and permissions manually. Currently there is no option to specify the default permissions
    // and ownership for new tables.
    final Path tablePath = new Path(getUserHome(user), tableName);

    fs.setOwner(tablePath, user, group);
    fs.setPermission(tablePath, new FsPermission((short)0750));
  }

  private static void createNestedTestViewsOnLineItem() throws Exception {
    // Input table "lineitem"
    // /user/user0_1     lineitem      750    user0_1:group0_1

    // Create a view on top of lineitem table
    // /user/user1_1    u1_lineitem    750    user1_1:group1_1
    createView(org1Users[1], org1Groups[1], (short)0750, "u1_lineitem", getWSSchema(org1Users[0]), "lineitem");

    // Create a view on top of u1_lineitem view
    // /user/user2_1    u2_lineitem    750    user2_1:group2_1
    createView(org1Users[2], org1Groups[2], (short) 0750, "u2_lineitem", getWSSchema(org1Users[1]), "u1_lineitem");

    // Create a view on top of u2_lineitem view
    // /user/user2_1    u22_lineitem    750    user2_1:group2_1
    createView(org1Users[2], org1Groups[2], (short) 0750, "u22_lineitem", getWSSchema(org1Users[2]), "u2_lineitem");

    // Create a view on top of u22_lineitem view
    // /user/user3_1    u3_lineitem    750    user3_1:group3_1
    createView(org1Users[3], org1Groups[3], (short)0750, "u3_lineitem", getWSSchema(org1Users[2]), "u22_lineitem");

    // Create a view on top of u3_lineitem view
    // /user/user4_1    u4_lineitem    755    user4_1:group4_1
    createView(org1Users[4], org1Groups[4], (short) 0755, "u4_lineitem", getWSSchema(org1Users[3]), "u3_lineitem");
  }

  private static void createNestedTestViewsOnOrders() throws Exception {
    // Input table "orders"
    // /user/user0_2     orders      750    user0_2:group0_2

    // Create a view on top of orders table
    // /user/user1_2    u1_orders    750    user1_2:group1_2
    createView(org2Users[1], org2Groups[1], (short)0750, "u1_orders", getWSSchema(org2Users[0]), "orders");

    // Create a view on top of u1_orders view
    // /user/user2_2    u2_orders    750    user2_2:group2_2
    createView(org2Users[2], org2Groups[2], (short)0750, "u2_orders", getWSSchema(org2Users[1]), "u1_orders");

    // Create a view on top of u2_orders view
    // /user/user2_2    u22_orders    750    user2_2:group2_2
    createView(org2Users[2], org2Groups[2], (short)0750, "u22_orders", getWSSchema(org2Users[2]), "u2_orders");

    // Create a view on top of u22_orders view (permissions of this view (755) are different from permissions of the
    // corresponding view in "lineitem" nested views to have a join query of "lineitem" and "orders" nested views
    // without exceeding the maximum number of allowed user hops in chained impersonation.
    // /user/user3_2    u3_orders    750    user3_2:group3_2
    createView(org2Users[3], org2Groups[3], (short)0755, "u3_orders", getWSSchema(org2Users[2]), "u22_orders");

    // Create a view on top of u3_orders view
    // /user/user4_2    u4_orders    755    user4_2:group4_2
    createView(org2Users[4], org2Groups[4], (short)0755, "u4_orders", getWSSchema(org2Users[3]), "u3_orders");
  }

  private static void createRecordReadersData(String user, String group) throws Exception {
    // copy sequence file
    updateClient(user);
    Path localFile = new Path(DrillFileUtils.getResourceAsFile("/sequencefiles/simple.seq").toURI().toString());
    Path dfsFile = new Path(getUserHome(user), "simple.seq");
    fs.copyFromLocalFile(localFile, dfsFile);
    fs.setOwner(dfsFile, user, group);
    fs.setPermission(dfsFile, new FsPermission((short) 0700));

    AvroDataGenerator avroDataGenerator = new AvroDataGenerator(dirTestWatcher);
    localFile = new Path(avroDataGenerator.generateSimplePrimitiveSchema_NoNullValues().getFilePath());
    dfsFile = new Path(getUserHome(user), "simple.avro");
    fs.copyFromLocalFile(localFile, dfsFile);
    fs.setOwner(dfsFile, user, group);
    fs.setPermission(dfsFile, new FsPermission((short) 0700));
  }

  @Test
  public void testDirectImpersonation_HasUserReadPermissions() throws Exception {
    // Table lineitem is owned by "user0_1:group0_1" with permissions 750. Try to read the table as "user0_1". We
    // shouldn't expect any errors.
    updateClient(org1Users[0]);
    test("SELECT * FROM %s.lineitem ORDER BY l_orderkey LIMIT 1", getWSSchema(org1Users[0]));
  }

  @Test
  public void testDirectImpersonation_HasGroupReadPermissions() throws Exception {
    // Table lineitem is owned by "user0_1:group0_1" with permissions 750. Try to read the table as "user1_1". We
    // shouldn't expect any errors as "user1_1" is part of the "group0_1"
    updateClient(org1Users[1]);
    test("SELECT * FROM %s.lineitem ORDER BY l_orderkey LIMIT 1", getWSSchema(org1Users[0]));
  }

  @Test
  public void testDirectImpersonation_NoReadPermissions() throws Exception {
    UserRemoteException ex = null;
    try {
      // Table lineitem is owned by "user0_1:group0_1" with permissions 750. Now try to read the table as "user2_1". We
      // should expect a permission denied error as "user2_1" is not part of the "group0_1"
      updateClient(org1Users[2]);
      test("SELECT * FROM %s.lineitem ORDER BY l_orderkey LIMIT 1", getWSSchema(org1Users[0]));
    } catch(UserRemoteException e) {
      ex = e;
    }

    assertNotNull("UserRemoteException is expected", ex);
    assertThat(ex.getMessage(), containsString("PERMISSION ERROR: " +
      String.format("Not authorized to read table [lineitem] in schema [%s.user0_1]", MINI_DFS_STORAGE_PLUGIN_NAME)));
  }

  @Test
  public void testMultiLevelImpersonationEqualToMaxUserHops() throws Exception {
    updateClient(org1Users[4]);
    test("SELECT * from %s.u4_lineitem LIMIT 1;", getWSSchema(org1Users[4]));
  }

  @Test
  public void testMultiLevelImpersonationExceedsMaxUserHops() throws Exception {
    UserRemoteException ex = null;

    try {
      updateClient(org1Users[5]);
      test("SELECT * from %s.u4_lineitem LIMIT 1;", getWSSchema(org1Users[4]));
    } catch (UserRemoteException e) {
      ex = e;
    }

    assertNotNull("UserRemoteException is expected", ex);
    assertThat(ex.getMessage(),
      containsString("Cannot issue token for view expansion as issuing the token exceeds the maximum allowed number " +
        "of user hops (3) in chained impersonation"));
  }

  @Test
  public void testMultiLevelImpersonationJoinEachSideReachesMaxUserHops() throws Exception {
    updateClient(org1Users[4]);
    test("SELECT * from %s.u4_lineitem l JOIN %s.u3_orders o ON l.l_orderkey = o.o_orderkey LIMIT 1",
      getWSSchema(org1Users[4]), getWSSchema(org2Users[3]));
  }

  @Test
  public void testMultiLevelImpersonationJoinOneSideExceedsMaxUserHops() throws Exception {
    UserRemoteException ex = null;

    try {
      updateClient(org1Users[4]);
      test("SELECT * from %s.u4_lineitem l JOIN %s.u4_orders o ON l.l_orderkey = o.o_orderkey LIMIT 1",
          getWSSchema(org1Users[4]), getWSSchema(org2Users[4]));
    } catch(UserRemoteException e) {
      ex = e;
    }

    assertNotNull("UserRemoteException is expected", ex);
    assertThat(ex.getMessage(),
      containsString("Cannot issue token for view expansion as issuing the token exceeds the maximum allowed number " +
        "of user hops (3) in chained impersonation"));
  }

  @Test
  public void sequenceFileChainedImpersonationWithView() throws Exception {
    // create a view named "simple_seq_view" on "simple.seq". View is owned by user0:group0 and has permissions 750
    createView(org1Users[0], org1Groups[0], "simple_seq_view",
      String.format("SELECT convert_from(t.binary_key, 'UTF8') as k FROM %s.`%s` t", MINI_DFS_STORAGE_PLUGIN_NAME,
        new Path(getUserHome(org1Users[0]), "simple.seq")));
    try {
      updateClient(org1Users[1]);
      test("SELECT k FROM %s.%s.%s", MINI_DFS_STORAGE_PLUGIN_NAME, "tmp", "simple_seq_view");
    } catch (UserRemoteException e) {
      assertNull("This test should pass.", e);
    }
    createView(org1Users[1], org1Groups[1], "simple_seq_view_2",
      String.format("SELECT k FROM %s.%s.%s", MINI_DFS_STORAGE_PLUGIN_NAME, "tmp", "simple_seq_view"));
    try {
      updateClient(org1Users[2]);
      test("SELECT k FROM %s.%s.%s", MINI_DFS_STORAGE_PLUGIN_NAME, "tmp", "simple_seq_view_2");
    } catch (UserRemoteException e) {
      assertNull("This test should pass.", e);
    }
  }

  @Test
  public void avroChainedImpersonationWithView() throws Exception {
    createView(org1Users[0], org1Groups[0], "simple_avro_view",
      String.format("SELECT h_boolean, e_double FROM %s.`%s` t", MINI_DFS_STORAGE_PLUGIN_NAME,
        new Path(getUserHome(org1Users[0]), "simple.avro")));

    updateClient(org1Users[1]);
    test("SELECT h_boolean FROM %s.%s.%s", MINI_DFS_STORAGE_PLUGIN_NAME, "tmp", "simple_avro_view");
  }

  @Test // DRILL-7250
  public void testCTEWithImpersonation() throws Exception {
    // Table lineitem is owned by "user0_1:group0_1" with permissions 750. "user2_1" doesn't have access to it,
    // but query uses CTE with the same name as the table, so query shouldn't look for lineitem table
    updateClient(org1Users[2]);
    test("use %s", getWSSchema(org1Users[0]));
    testBuilder()
        .sqlQuery("with lineitem as (SELECT 1 as a) select * from lineitem")
        .unOrdered()
        .baselineColumns("a")
        .baselineValues(1)
        .go();
  }

  @AfterClass
  public static void removeMiniDfsBasedStorage() throws PluginException {
    getDrillbitContext().getStorage().remove(MINI_DFS_STORAGE_PLUGIN_NAME);
    stopMiniDfsCluster();
  }
}
