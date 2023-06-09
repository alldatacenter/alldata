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

import java.util.Map;
import org.apache.drill.categories.SecurityTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.dotdrill.DotDrillType;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.security.UserGroupInformation;
import static org.hamcrest.core.StringContains.containsString;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;

/**
 * Tests impersonation on metadata related queries as SHOW FILES, SHOW TABLES, CREATE VIEW, CREATE TABLE and DROP TABLE
 */
@Category({SlowTest.class, SecurityTest.class})
public class TestImpersonationMetadata extends BaseTestImpersonation {
  private static final String user1 = "drillTestUser1";
  private static final String user2 = "drillTestUser2";

  private static final String group0 = "drill_test_grp_0";
  private static final String group1 = "drill_test_grp_1";

  static {
    UserGroupInformation.createUserForTesting(user1, new String[]{ group1, group0 });
    UserGroupInformation.createUserForTesting(user2, new String[]{ group1 });
  }

  @Rule
  public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setup() throws Exception {
    startMiniDfsCluster(TestImpersonationMetadata.class.getSimpleName());
    startDrillCluster(true);
    addMiniDfsBasedStorage(createTestWorkspaces());
  }

  private static Map<String, WorkspaceConfig> createTestWorkspaces() throws Exception {
    // Create "/tmp" folder and set permissions to "777"
    final Path tmpPath = new Path("/tmp");
    fs.delete(tmpPath, true);
    FileSystem.mkdirs(fs, tmpPath, new FsPermission((short)0777));

    Map<String, WorkspaceConfig> workspaces = Maps.newHashMap();

    // Create /drill_test_grp_0_700 directory with permissions 700 (owned by user running the tests)
    createAndAddWorkspace("drill_test_grp_0_700", "/drill_test_grp_0_700", (short)0700, processUser, group0, workspaces);

    // Create /drill_test_grp_0_750 directory with permissions 750 (owned by user running the tests)
    createAndAddWorkspace("drill_test_grp_0_750", "/drill_test_grp_0_750", (short)0750, processUser, group0, workspaces);

    // Create /drill_test_grp_0_755 directory with permissions 755 (owned by user running the tests)
    createAndAddWorkspace("drill_test_grp_0_755", "/drill_test_grp_0_755", (short)0755, processUser, group0, workspaces);

    // Create /drill_test_grp_0_770 directory with permissions 770 (owned by user running the tests)
    createAndAddWorkspace("drill_test_grp_0_770", "/drill_test_grp_0_770", (short)0770, processUser, group0, workspaces);

    // Create /drill_test_grp_0_777 directory with permissions 777 (owned by user running the tests)
    createAndAddWorkspace("drill_test_grp_0_777", "/drill_test_grp_0_777", (short)0777, processUser, group0, workspaces);

    // Create /drill_test_grp_1_700 directory with permissions 700 (owned by user1)
    createAndAddWorkspace("drill_test_grp_1_700", "/drill_test_grp_1_700", (short)0700, user1, group1, workspaces);

    // create /user2_workspace1 with 775 permissions (owner by user1)
    createAndAddWorkspace("user2_workspace1", "/user2_workspace1", (short)0775, user2, group1, workspaces);

    // create /user2_workspace with 755 permissions (owner by user1)
    createAndAddWorkspace("user2_workspace2", "/user2_workspace2", (short)0755, user2, group1, workspaces);

    return workspaces;
  }

  @Test
  public void testDropTable() throws Exception {

    // create tables as user2
    updateClient(user2);
    test("use `%s.user2_workspace1`", MINI_DFS_STORAGE_PLUGIN_NAME);
    // create a table that can be dropped by another user in a different group
    test("create table parquet_table_775 as select * from cp.`employee.json`");

    // create a table that cannot be dropped by another user
    test("use `%s.user2_workspace2`", MINI_DFS_STORAGE_PLUGIN_NAME);
    test("create table parquet_table_700 as select * from cp.`employee.json`");

    // Drop tables as user1
    updateClient(user1);
    test("use `%s.user2_workspace1`", MINI_DFS_STORAGE_PLUGIN_NAME);
    testBuilder()
        .sqlQuery("drop table parquet_table_775")
        .unOrdered()
        .baselineColumns("ok", "summary")
        .baselineValues(true, String.format("Table [%s] dropped", "parquet_table_775"))
        .go();

    test("use `%s.user2_workspace2`", MINI_DFS_STORAGE_PLUGIN_NAME);
    boolean dropFailed = false;
    try {
      test("drop table parquet_table_700");
    } catch (UserException e) {
      assertTrue(e.getMessage().contains("PERMISSION ERROR"));
      dropFailed = true;
    }
    assertTrue("Permission checking failed during drop table", dropFailed);
  }

  @Test // DRILL-3037
  @Category(UnlikelyTest.class)
  public void testImpersonatingProcessUser() throws Exception {
    updateClient(processUser);

    // Process user start the mini dfs, he has read/write permissions by default
    final String viewName = String.format("%s.drill_test_grp_0_700.testView", MINI_DFS_STORAGE_PLUGIN_NAME);
    try {
      test("CREATE VIEW " + viewName + " AS SELECT * FROM cp.`region.json`");
      test("SELECT * FROM " + viewName + " LIMIT 2");
    } finally {
      test("DROP VIEW " + viewName);
    }
  }

  @Test
  public void testShowFilesInWSWithUserAndGroupPermissionsForQueryUser() throws Exception {
    updateClient(user1);

    // Try show tables in schema "drill_test_grp_1_700" which is owned by "user1"
    int count = testSql(String.format("SHOW FILES IN %s.drill_test_grp_1_700", MINI_DFS_STORAGE_PLUGIN_NAME));
    assertTrue(count > 0);

    // Try show tables in schema "drill_test_grp_0_750" which is owned by "processUser" and has group permissions for "user1"
    count = testSql(String.format("SHOW FILES IN %s.drill_test_grp_0_750", MINI_DFS_STORAGE_PLUGIN_NAME));
    assertTrue(count > 0);
  }

  @Test
  public void testShowFilesInWSWithOtherPermissionsForQueryUser() throws Exception {
    updateClient(user2);
    // Try show tables in schema "drill_test_grp_0_755" which is owned by "processUser" and group0. "user2" is not part of the "group0"
    int count = testSql(String.format("SHOW FILES IN %s.drill_test_grp_0_755", MINI_DFS_STORAGE_PLUGIN_NAME));
    assertTrue(count > 0);
  }

  @Test
  public void testShowFilesInWSWithNoPermissionsForQueryUser() throws Exception {
    updateClient(user2);
    // Try show tables in schema "drill_test_grp_1_700" which is owned by "user1"
    int count = testSql(String.format("SHOW FILES IN %s.drill_test_grp_1_700", MINI_DFS_STORAGE_PLUGIN_NAME));
    assertEquals(0, count);
  }

  @Test
  public void testShowSchemasAsUser1() throws Exception {
    // "user1" is part of "group0" and has access to following workspaces
    // drill_test_grp_1_700 (through ownership)
    // drill_test_grp_0_750, drill_test_grp_0_770 (through "group" category permissions)
    // drill_test_grp_0_755, drill_test_grp_0_777 (through "others" category permissions)
    updateClient(user1);
    testBuilder()
        .sqlQuery("SHOW SCHEMAS LIKE '%drill_test%'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues(String.format("%s.drill_test_grp_0_750", MINI_DFS_STORAGE_PLUGIN_NAME))
        .baselineValues(String.format("%s.drill_test_grp_0_755", MINI_DFS_STORAGE_PLUGIN_NAME))
        .baselineValues(String.format("%s.drill_test_grp_0_770", MINI_DFS_STORAGE_PLUGIN_NAME))
        .baselineValues(String.format("%s.drill_test_grp_0_777", MINI_DFS_STORAGE_PLUGIN_NAME))
        .baselineValues(String.format("%s.drill_test_grp_1_700", MINI_DFS_STORAGE_PLUGIN_NAME))
        .go();
  }

  @Test
  public void testShowSchemasAsUser2() throws Exception {
    // "user2" is part of "group0", but part of "group1" and has access to following workspaces
    // drill_test_grp_0_755, drill_test_grp_0_777 (through "others" category permissions)
    updateClient(user2);
    testBuilder()
        .sqlQuery("SHOW SCHEMAS LIKE '%drill_test%'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues(String.format("%s.drill_test_grp_0_755", MINI_DFS_STORAGE_PLUGIN_NAME))
        .baselineValues(String.format("%s.drill_test_grp_0_777", MINI_DFS_STORAGE_PLUGIN_NAME))
        .go();
  }

  @Test
  public void testCreateViewInDirWithUserPermissionsForQueryUser() throws Exception {
    final String viewSchema = MINI_DFS_STORAGE_PLUGIN_NAME + ".drill_test_grp_1_700"; // Workspace dir owned by "user1"
    testCreateViewTestHelper(user1, viewSchema, "view1");
  }

  @Test
  public void testCreateViewInDirWithGroupPermissionsForQueryUser() throws Exception {
    // Workspace dir owned by "processUser", workspace group is "group0" and "user1" is part of "group0"
    final String viewSchema = MINI_DFS_STORAGE_PLUGIN_NAME + ".drill_test_grp_0_770";
    testCreateViewTestHelper(user1, viewSchema, "view1");
  }

  @Test
  public void testCreateViewInDirWithOtherPermissionsForQueryUser() throws Exception {
    // Workspace dir owned by "processUser", workspace group is "group0" and "user2" is not part of "group0"
    final String viewSchema = MINI_DFS_STORAGE_PLUGIN_NAME + ".drill_test_grp_0_777";
    testCreateViewTestHelper(user2, viewSchema, "view1");
  }

  private static void testCreateViewTestHelper(String user, String viewSchema,
      String viewName) throws Exception {
    try {
      updateClient(user);

      test("USE " + viewSchema);

      test("CREATE VIEW " + viewName + " AS SELECT " +
          "c_custkey, c_nationkey FROM cp.`tpch/customer.parquet` ORDER BY c_custkey;");

      testBuilder()
          .sqlQuery("SHOW TABLES")
          .unOrdered()
          .baselineColumns("TABLE_SCHEMA", "TABLE_NAME")
          .baselineValues(viewSchema, viewName)
          .go();

      test("SHOW FILES");

      testBuilder()
          .sqlQuery("SELECT * FROM " + viewName + " LIMIT 1")
          .ordered()
          .baselineColumns("c_custkey", "c_nationkey")
          .baselineValues(1, 15)
          .go();

    } finally {
      test("DROP VIEW " + viewSchema + "." + viewName);
    }
  }

  @Test
  public void testCreateViewInWSWithNoPermissionsForQueryUser() throws Exception {
    // Workspace dir owned by "processUser", workspace group is "group0" and "user2" is not part of "group0"
    final String tableWS = "drill_test_grp_0_755";
    final String viewSchema = MINI_DFS_STORAGE_PLUGIN_NAME + "." + tableWS;
    final String viewName = "view1";

    updateClient(user2);

    test("USE " + viewSchema);

    String expErrorMsg = "PERMISSION ERROR: Permission denied: user=drillTestUser2, access=WRITE, inode=\"/" + tableWS;
    thrown.expect(UserRemoteException.class);
    thrown.expectMessage(containsString(expErrorMsg));

    test("CREATE VIEW %s AS" +
        " SELECT c_custkey, c_nationkey FROM cp.`tpch/customer.parquet` ORDER BY c_custkey", viewName);

    // SHOW TABLES is expected to return no records as view creation fails above.
    testBuilder()
        .sqlQuery("SHOW TABLES")
        .expectsEmptyResultSet()
        .go();

    test("SHOW FILES");
  }

  @Test
  public void testCreateTableInDirWithUserPermissionsForQueryUser() throws Exception {
    final String tableWS = "drill_test_grp_1_700"; // Workspace dir owned by "user1"
    testCreateTableTestHelper(user1, tableWS, "table1");
  }

  @Test
  public void testCreateTableInDirWithGroupPermissionsForQueryUser() throws Exception {
    // Workspace dir owned by "processUser", workspace group is "group0" and "user1" is part of "group0"
    final String tableWS = "drill_test_grp_0_770";
    testCreateTableTestHelper(user1, tableWS, "table1");
  }

  @Test
  public void testCreateTableInDirWithOtherPermissionsForQueryUser() throws Exception {
    // Workspace dir owned by "processUser", workspace group is "group0" and "user2" is not part of "group0"
    final String tableWS = "drill_test_grp_0_777";
    testCreateTableTestHelper(user2, tableWS, "table1");
  }

  private static void testCreateTableTestHelper(String user, String tableWS,
      String tableName) throws Exception {
    try {
      updateClient(user);

      test("USE " + Joiner.on(".").join(MINI_DFS_STORAGE_PLUGIN_NAME, tableWS));

      test("CREATE TABLE " + tableName + " AS SELECT " +
          "c_custkey, c_nationkey FROM cp.`tpch/customer.parquet` ORDER BY c_custkey;");

      test("SHOW FILES");

      testBuilder()
          .sqlQuery("SELECT * FROM " + tableName + " LIMIT 1")
          .ordered()
          .baselineColumns("c_custkey", "c_nationkey")
          .baselineValues(1, 15)
          .go();

    } finally {
      // There is no drop table, we need to delete the table directory through FileSystem object
      final Path tablePath = new Path(Path.SEPARATOR + tableWS + Path.SEPARATOR + tableName);
      if (fs.exists(tablePath)) {
        fs.delete(tablePath, true);
      }
    }
  }

  @Test
  public void testCreateTableInWSWithNoPermissionsForQueryUser() throws Exception {
    // Workspace dir owned by "processUser", workspace group is "group0" and "user2" is not part of "group0"
    String tableWS = "drill_test_grp_0_755";
    String tableName = "table1";

    updateClient(user2);
    test("use %s.`%s`", MINI_DFS_STORAGE_PLUGIN_NAME, tableWS);

    thrown.expect(UserRemoteException.class);
    thrown.expectMessage(containsString("Permission denied: user=drillTestUser2, " +
        "access=WRITE, inode=\"/" + tableWS));

    test("CREATE TABLE %s AS SELECT c_custkey, c_nationkey " +
        "FROM cp.`tpch/customer.parquet` ORDER BY c_custkey", tableName);
  }

  @Test
  public void testRefreshMetadata() throws Exception {
    final String tableName = "nation1";
    final String tableWS = "drill_test_grp_1_700";

    updateClient(user1);
    test("USE " + Joiner.on(".").join(MINI_DFS_STORAGE_PLUGIN_NAME, tableWS));

    test("CREATE TABLE " + tableName + " partition by (n_regionkey) AS SELECT * " +
              "FROM cp.`tpch/nation.parquet`;");

    test( "refresh table metadata " + tableName + ";");

    test("SELECT * FROM " + tableName + ";");

    final Path tablePath = new Path(Path.SEPARATOR + tableWS + Path.SEPARATOR + tableName);
    assertTrue ( fs.exists(tablePath) && fs.isDirectory(tablePath));
    fs.mkdirs(new Path(tablePath, "tmp5"));

    test("SELECT * from " + tableName + ";");

  }

  @Test
  public void testAnalyzeTable() throws Exception {
    final String tableName = "nation1_stats";
    final String tableWS = "drill_test_grp_1_700";

    updateClient(user1);
    test("USE " + Joiner.on(".").join(MINI_DFS_STORAGE_PLUGIN_NAME, tableWS));
    test("ALTER SESSION SET `store.format` = 'parquet'");
    test("CREATE TABLE " + tableName + " AS SELECT * FROM cp.`tpch/nation.parquet`;");
    test("ANALYZE TABLE " + tableName + " COMPUTE STATISTICS;");
    test("SELECT * FROM " + tableName + ";");

    final Path statsFilePath = new Path(Path.SEPARATOR + tableWS + Path.SEPARATOR + tableName
        + Path.SEPARATOR + DotDrillType.STATS.getEnding());
    assertTrue (fs.exists(statsFilePath) && fs.isDirectory(statsFilePath));
    FileStatus status = fs.getFileStatus(statsFilePath);
    // Verify process user is the directory owner
    assert(processUser.equalsIgnoreCase(status.getOwner()));

    fs.mkdirs(new Path(statsFilePath, "tmp5"));

    test("SELECT * from " + tableName + ";");
    test("DROP TABLE " + tableName);
  }

  @AfterClass
  public static void removeMiniDfsBasedStorage() throws PluginException {
    getDrillbitContext().getStorage().remove(MINI_DFS_STORAGE_PLUGIN_NAME);
    stopMiniDfsCluster();
  }
}
