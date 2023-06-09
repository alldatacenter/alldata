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
package org.apache.drill.exec.impersonation.hive;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.security.SessionStateConfigUserAuthenticator;
import org.apache.hadoop.hive.ql.security.SessionStateUserAuthenticator;
import org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdConfOnlyAuthorizerFactory;
import org.apache.hadoop.hive.ql.security.authorization.plugin.sqlstd.SQLStdHiveAuthorizerFactory;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashMap;
import java.util.Map;

import static org.apache.drill.exec.hive.HiveTestUtilities.executeQuery;
import static org.apache.hadoop.fs.FileSystem.FS_DEFAULT_NAME_KEY;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_AUTHENTICATOR_MANAGER;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_AUTHORIZATION_ENABLED;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_AUTHORIZATION_MANAGER;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_CBO_ENABLED;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_SERVER2_ENABLE_DOAS;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTOREURIS;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTORE_AUTO_CREATE_ALL;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTORE_EXECUTE_SET_UGI;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTORE_SCHEMA_VERIFICATION;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestSqlStdBasedAuthorization extends BaseTestHiveImpersonation {

  private static final String db_general = "db_general";

  // Tables in "db_general"
  private static final String g_student_user0 = "student_user0";

  private static final String vw_student_user0 = "vw_student_user0";

  private static final String g_voter_role0 = "voter_role0";

  private static final String vw_voter_role0 = "vw_voter_role0";

  private static final String g_student_user2 = "student_user2";

  private static final String vw_student_user2 = "vw_student_user2";

  // Create a view on "g_student_user0". View is owned by user0:group0 and has permissions 750
  private static final String v_student_u0g0_750 = "v_student_u0g0_750";

  // Create a view on "v_student_u0g0_750". View is owned by user1:group1 and has permissions 750
  private static final String v_student_u1g1_750 = "v_student_u1g1_750";

  // Role for testing purpose
  private static final String test_role0 = "role0";

  @BeforeClass
  public static void setup() throws Exception {
    startMiniDfsCluster(TestSqlStdBasedAuthorization.class.getSimpleName());
    prepHiveConfAndData();
    setSqlStdBasedAuthorizationInHiveConf();
    startHiveMetaStore();
    startDrillCluster(true);
    addHiveStoragePlugin(getHivePluginConfig());
    addMiniDfsBasedStorage(new HashMap<>());
    generateTestData();
  }

  private static void setSqlStdBasedAuthorizationInHiveConf() {
    hiveConf.set(ConfVars.HIVE_AUTHORIZATION_ENABLED.varname, "true");
    hiveConf.set(HIVE_AUTHENTICATOR_MANAGER.varname, SessionStateConfigUserAuthenticator.class.getName());
    hiveConf.set(HIVE_AUTHORIZATION_MANAGER.varname, SQLStdConfOnlyAuthorizerFactory.class.getName());
    hiveConf.set(ConfVars.HIVE_SERVER2_ENABLE_DOAS.varname, "false");
    hiveConf.set(ConfVars.METASTORE_EXECUTE_SET_UGI.varname, "false");
    hiveConf.set(ConfVars.USERS_IN_ADMIN_ROLE.varname, processUser);
  }

  private static Map<String, String> getHivePluginConfig() {
    final Map<String, String> hiveConfig = Maps.newHashMap();
    hiveConfig.put(METASTOREURIS.varname, hiveConf.get(METASTOREURIS.varname));
    hiveConfig.put(FS_DEFAULT_NAME_KEY, dfsConf.get(FS_DEFAULT_NAME_KEY));
    hiveConfig.put(HIVE_SERVER2_ENABLE_DOAS.varname, hiveConf.get(HIVE_SERVER2_ENABLE_DOAS.varname));
    hiveConfig.put(METASTORE_EXECUTE_SET_UGI.varname, hiveConf.get(METASTORE_EXECUTE_SET_UGI.varname));
    hiveConfig.put(HIVE_AUTHORIZATION_ENABLED.varname, hiveConf.get(HIVE_AUTHORIZATION_ENABLED.varname));
    hiveConfig.put(HIVE_AUTHENTICATOR_MANAGER.varname, SessionStateUserAuthenticator.class.getName());
    hiveConfig.put(HIVE_AUTHORIZATION_MANAGER.varname, SQLStdHiveAuthorizerFactory.class.getName());
    hiveConfig.put(METASTORE_SCHEMA_VERIFICATION.varname, hiveConf.get(METASTORE_SCHEMA_VERIFICATION.varname));
    hiveConfig.put(METASTORE_AUTO_CREATE_ALL.varname, hiveConf.get(METASTORE_AUTO_CREATE_ALL.varname));
    hiveConfig.put(HIVE_CBO_ENABLED.varname, hiveConf.get(HIVE_CBO_ENABLED.varname));
    return hiveConfig;
  }


  /*
   * Generating database objects with permissions:
   * <p>
   * |                                         | org1Users[0] | org1Users[1] | org1Users[2]
   * ---------------------------------------------------------------------------------------
   * db_general.g_student_user0                |      +       |      -       |      -       |
   * db_general.g_voter_role0                  |      -       |      +       |      +       |
   * db_general.g_student_user2                |      -       |      -       |      +       |
   * |                                         |              |              |              |
   * mini_dfs_plugin.tmp.v_student_u0g0_750    |      +       |      +       |      -       |
   * mini_dfs_plugin.tmp.v_student_u1g1_750    |      -       |      +       |      +       |
   * |                                         |              |              |              |
   * db_general.vw_student_user0               |      +       |      -       |      -       |
   * db_general.vw_voter_role0                 |      -       |      +       |      +       |
   * db_general.vw_student_user2               |      -       |      -       |      +       |
   * ---------------------------------------------------------------------------------------
   *
   * @throws Exception - if view creation failed
   */
  private static void generateTestData() throws Exception {
    final SessionState ss = new SessionState(hiveConf);
    SessionState.start(ss);
    final Driver driver = new Driver(hiveConf);

    executeQuery(driver, "CREATE DATABASE " + db_general);
    createTable(driver, db_general, g_student_user0, studentDef, studentData);
    createTable(driver, db_general, g_voter_role0, voterDef, voterData);
    createTable(driver, db_general, g_student_user2, studentDef, studentData);

    createHiveView(driver, db_general, vw_student_user0, g_student_user0);
    createHiveView(driver, db_general, vw_voter_role0, g_voter_role0);
    createHiveView(driver, db_general, vw_student_user2, g_student_user2);

    executeQuery(driver, "SET ROLE admin");
    executeQuery(driver, "CREATE ROLE " + test_role0);
    executeQuery(driver, "GRANT ROLE " + test_role0 + " TO USER " + org1Users[1]);
    executeQuery(driver, "GRANT ROLE " + test_role0 + " TO USER " + org1Users[2]);

    executeQuery(driver, String.format("GRANT SELECT ON db_general.%s TO USER %s",
        g_student_user0, org1Users[0]));
    executeQuery(driver, String.format("GRANT SELECT ON db_general.%s TO USER %s",
        vw_student_user0, org1Users[0]));

    executeQuery(driver, String.format("GRANT SELECT ON db_general.%s TO ROLE %s",
        g_voter_role0, test_role0));
    executeQuery(driver, String.format("GRANT SELECT ON db_general.%s TO ROLE %s",
        vw_voter_role0, test_role0));

    executeQuery(driver, String.format("GRANT SELECT ON db_general.%s TO USER %s",
        g_student_user2, org1Users[2]));
    executeQuery(driver, String.format("GRANT SELECT ON db_general.%s TO USER %s",
        vw_student_user2, org1Users[2]));

    createView(org1Users[0], org1Groups[0], v_student_u0g0_750,
        String.format("SELECT rownum, name, age, studentnum FROM %s.%s.%s",
            hivePluginName, db_general, g_student_user0));

    createView(org1Users[1], org1Groups[1], v_student_u1g1_750,
        String.format("SELECT rownum, name, age FROM %s.%s.%s", MINI_DFS_STORAGE_PLUGIN_NAME, "tmp", v_student_u0g0_750));
  }

  // Irrespective of each db permissions, all dbs show up in "SHOW SCHEMAS"
  @Test
  @Ignore //todo: enable after fix of DRILL-6923
  public void showSchemas() throws Exception {
    testBuilder()
        .sqlQuery("SHOW SCHEMAS LIKE 'hive.%'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues("hive.db_general")
        .baselineValues("hive.default")
        .go();
  }

  @Test
  public void user0_showTables() throws Exception {
    updateClient(org1Users[0]);
    showTablesHelper(db_general,
        // Users are expected to see all tables in a database even if they don't have permissions to read from tables.
        ImmutableList.of(
            g_student_user0,
            g_student_user2,
            g_voter_role0,
            vw_student_user0,
            vw_voter_role0,
            vw_student_user2
        ));
  }

  @Test
  public void user0_allowed_g_student_user0() throws Exception {
    // SELECT on "student_user0" table is granted to user "user0"
    updateClient(org1Users[0]);
    test("USE " + hivePluginName + "." + db_general);
    test(String.format("SELECT * FROM %s ORDER BY name LIMIT 2", g_student_user0));
  }

  @Test
  public void user0_allowed_vw_student_user0() throws Exception {
    queryHiveView(org1Users[0], vw_student_user0);
  }

  @Test
  public void user0_forbidden_g_voter_role0() throws Exception {
    // SELECT on table "student_user0" is NOT granted to user "user0" directly or indirectly through role "role0" as
    // user "user0" is not part of role "role0"
    updateClient(org1Users[0]);
    test("USE " + hivePluginName + "." + db_general);
    final String query = String.format("SELECT * FROM %s ORDER BY name LIMIT 2", g_voter_role0);
    errorMsgTestHelper(query, "Principal [name=user0_1, type=USER] does not have following privileges for " +
        "operation QUERY [[SELECT] on Object [type=TABLE_OR_VIEW, name=db_general.voter_role0]]\n");
  }

  @Test
  public void user0_forbidden_vw_voter_role0() throws Exception {
    queryHiveViewNotAuthorized(org1Users[0], vw_voter_role0);
  }

  @Test
  public void user0_forbidden_v_student_u1g1_750() throws Exception {
    updateClient(org1Users[0]);
    queryViewNotAuthorized(v_student_u1g1_750);
  }

  @Test
  public void user0_allowed_v_student_u0g0_750() throws Exception {
    updateClient(org1Users[0]);
    queryView(v_student_u0g0_750);
  }

  @Test
  public void user1_showTables() throws Exception {
    updateClient(org1Users[1]);
    showTablesHelper(db_general,
        // Users are expected to see all tables in a database even if they don't have permissions to read from tables.
        ImmutableList.of(
            g_student_user0,
            g_student_user2,
            g_voter_role0,
            vw_student_user0,
            vw_voter_role0,
            vw_student_user2
        ));
  }

  @Test
  public void user1_forbidden_g_student_user0() throws Exception {
    // SELECT on table "student_user0" is NOT granted to user "user1"
    updateClient(org1Users[1]);
    test("USE " + hivePluginName + "." + db_general);
    final String query = String.format("SELECT * FROM %s ORDER BY name LIMIT 2", g_student_user0);
    errorMsgTestHelper(query, "Principal [name=user1_1, type=USER] does not have following privileges for " +
        "operation QUERY [[SELECT] on Object [type=TABLE_OR_VIEW, name=db_general.student_user0]]\n");
  }

  @Test
  public void user1_forbidden_vw_student_user0() throws Exception {
    queryHiveViewNotAuthorized(org1Users[1], vw_student_user0);
  }

  @Test
  public void user1_allowed_g_voter_role0() throws Exception {
    // SELECT on "voter_role0" table is granted to role "role0" and user "user1" is part the role "role0"
    updateClient(org1Users[1]);
    test("USE " + hivePluginName + "." + db_general);
    test(String.format("SELECT * FROM %s ORDER BY name LIMIT 2", g_voter_role0));
  }

  @Test
  public void user1_allowed_vw_voter_role0() throws Exception {
    queryHiveView(org1Users[1], vw_voter_role0);
  }

  @Test
  public void user1_allowed_g_voter_role0_but_forbidden_g_student_user2() throws Exception {
    // SELECT on "voter_role0" table is granted to role "role0" and user "user1" is part the role "role0"
    // SELECT on "student_user2" table is NOT granted to either role "role0" or user "user1"
    updateClient(org1Users[1]);
    test("USE " + hivePluginName + "." + db_general);
    final String query =
        String.format("SELECT * FROM %s v JOIN %s s on v.name = s.name limit 2;", g_voter_role0, g_student_user2);
    errorMsgTestHelper(query, "Principal [name=user1_1, type=USER] does not have following privileges for " +
        "operation QUERY [[SELECT] on Object [type=TABLE_OR_VIEW, name=db_general.student_user2]]");
  }

  @Test
  public void user1_allowed_vw_voter_role0_but_forbidden_vw_student_user2() throws Exception {
    // SELECT on "vw_voter_role0" table is granted to role "role0" and user "user1" is part the role "role0"
    // SELECT on "vw_student_user2" table is NOT granted to either role "role0" or user "user1"
    updateClient(org1Users[1]);
    test("USE " + hivePluginName + "." + db_general);
    final String query =
        String.format("SELECT * FROM %s v JOIN %s s on v.name = s.name limit 2;", vw_voter_role0, vw_student_user2);
    errorMsgTestHelper(query, "Principal [name=user1_1, type=USER] does not have following privileges for " +
        "operation QUERY [[SELECT] on Object [type=TABLE_OR_VIEW, name=db_general.vw_student_user2]]");
  }

  @Test
  public void user1_allowed_v_student_u0g0_750() throws Exception {
    updateClient(org1Users[1]);
    queryView(v_student_u0g0_750);
  }

  @Test
  public void user1_allowed_v_student_u1g1_750() throws Exception {
    updateClient(org1Users[1]);
    queryView(v_student_u1g1_750);
  }

  @Test
  public void user2_allowed_g_voter_role0() throws Exception {
    // SELECT on "voter_role0" table is granted to role "role0" and user "user2" is part the role "role0"
    updateClient(org1Users[2]);
    test("USE " + hivePluginName + "." + db_general);
    test(String.format("SELECT * FROM %s ORDER BY name LIMIT 2", g_voter_role0));
  }

  @Test
  public void user2_allowed_vw_voter_role0() throws Exception {
    queryHiveView(org1Users[2], vw_voter_role0);
  }

  @Test
  public void user2_allowed_g_student_user2() throws Exception {
    // SELECT on "student_user2" table is granted to user "user2"
    updateClient(org1Users[2]);
    test("USE " + hivePluginName + "." + db_general);
    test(String.format("SELECT * FROM %s ORDER BY name LIMIT 2", g_student_user2));
  }

  @Test
  public void user2_allowed_vw_student_user2() throws Exception {
    queryHiveView(org1Users[2], vw_student_user2);
  }

  @Test
  public void user2_allowed_g_voter_role0_and_g_student_user2() throws Exception {
    // SELECT on "voter_role0" table is granted to role "role0" and user "user2" is part the role "role0"
    // SELECT on "student_user2" table is granted to user "user2"
    updateClient(org1Users[2]);
    test("USE " + hivePluginName + "." + db_general);
    test(String.format("SELECT * FROM %s v JOIN %s s on v.name = s.name limit 2;", g_voter_role0, g_student_user2));
  }

  @Test
  public void user2_allowed_vw_voter_role0_and_vw_student_user2() throws Exception {
    updateClient(org1Users[2]);
    test("USE " + hivePluginName + "." + db_general);
    test(String.format("SELECT * FROM %s v JOIN %s s on v.name = s.name limit 2;", vw_voter_role0, vw_student_user2));
  }

  @Test
  public void user2_forbidden_v_student_u0g0_750() throws Exception {
    updateClient(org1Users[2]);
    queryViewNotAuthorized(v_student_u0g0_750);
  }

  @Test
  public void user2_allowed_v_student_u1g1_750() throws Exception {
    updateClient(org1Users[2]);
    queryView(v_student_u1g1_750);
  }

  @AfterClass
  public static void shutdown() throws Exception {
    stopMiniDfsCluster();
    stopHiveMetaStore();
  }

  private static void queryHiveView(String usr, String viewName) throws Exception {
    String query = String.format("SELECT COUNT(*) AS rownum FROM %s.%s.%s",
        hivePluginName, db_general, viewName);
    updateClient(usr);
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("rownum")
        .baselineValues(1L)
        .go();
  }

  private static void queryHiveViewNotAuthorized(String usr, String viewName) throws Exception {
    final String query = String.format("SELECT * FROM %s.%s.%s", hivePluginName, db_general, viewName);
    final String expectedError = String.format("Principal [name=%s, type=USER] does not have following privileges for " +
            "operation QUERY [[SELECT] on Object [type=TABLE_OR_VIEW, name=db_general.%s]]\n",
        usr, viewName);

    updateClient(usr);
    errorMsgTestHelper(query, expectedError);
  }

  private static void createHiveView(Driver driver, String db, String viewName, String tblName) {
    String viewFullName = db + "." + viewName;
    String tblFullName = db + "." + tblName;
    executeQuery(driver, String.format("CREATE OR REPLACE VIEW %s AS SELECT * FROM %s LIMIT 1", viewFullName, tblFullName));
  }

}
