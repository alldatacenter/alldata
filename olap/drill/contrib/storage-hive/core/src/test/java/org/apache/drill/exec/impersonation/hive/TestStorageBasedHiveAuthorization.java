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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.calcite.schema.Schema.TableType;
import org.apache.drill.categories.HiveStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hive.ql.Driver;
import org.apache.hadoop.hive.ql.security.HadoopDefaultMetastoreAuthenticator;
import org.apache.hadoop.hive.ql.security.authorization.AuthorizationPreEventListener;
import org.apache.hadoop.hive.ql.security.authorization.StorageBasedAuthorizationProvider;
import org.apache.hadoop.hive.ql.session.SessionState;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.drill.exec.hive.HiveTestUtilities.executeQuery;
import static org.apache.drill.shaded.guava.com.google.common.collect.Lists.newArrayList;
import static org.apache.hadoop.fs.FileSystem.FS_DEFAULT_NAME_KEY;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.DYNAMICPARTITIONINGMODE;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_CBO_ENABLED;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_METASTORE_AUTHENTICATOR_MANAGER;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_METASTORE_AUTHORIZATION_AUTH_READS;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_METASTORE_AUTHORIZATION_MANAGER;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.HIVE_SERVER2_ENABLE_DOAS;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTOREURIS;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTORE_AUTO_CREATE_ALL;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTORE_EXECUTE_SET_UGI;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTORE_PRE_EVENT_LISTENERS;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTORE_SCHEMA_VERIFICATION;

@Category({SlowTest.class, HiveStorageTest.class})
public class TestStorageBasedHiveAuthorization extends BaseTestHiveImpersonation {

  // DB whose warehouse directory has permissions 755, available everyone to read
  private static final String db_general = "db_general";

  // Tables in "db_general"
  private static final String g_student_u0_700 = "student_u0_700";
  private static final String g_vw_g_student_u0_700 = "vw_u0_700_student_u0_700";
  private static final String g_student_u0g0_750 = "student_u0g0_750";
  private static final String g_student_all_755 = "student_all_755";
  private static final String g_voter_u1_700 = "voter_u1_700";
  private static final String g_voter_u2g1_750 = "voter_u2g1_750";
  private static final String g_voter_all_755 = "voter_all_755";
  private static final String g_partitioned_student_u0_700 = "partitioned_student_u0_700";

  private static final List<String> all_tables_in_db_general = ImmutableList.of(
      g_student_u0_700,
      g_vw_g_student_u0_700,
      g_student_u0g0_750,
      g_student_all_755,
      g_voter_u1_700,
      g_voter_u2g1_750,
      g_voter_all_755,
      g_partitioned_student_u0_700
  );

  private static final List<TableType> all_tables_type_in_db_general = ImmutableList.of(
      TableType.TABLE,
      TableType.VIEW,
      TableType.TABLE,
      TableType.TABLE,
      TableType.TABLE,
      TableType.TABLE,
      TableType.TABLE,
      TableType.TABLE
  );


  // DB whose warehouse directory has permissions 700 and owned by user0
  private static final String db_u0_only = "db_u0_only";

  // Tables in "db_u0_only"
  private static final String u0_student_all_755 = "student_all_755";
  private static final String u0_voter_all_755 = "voter_all_755";
  private static final String u0_vw_voter_all_755 = "vw_voter_all_755";

  private static final List<String> all_tables_in_db_u0_only = ImmutableList.of(
      u0_student_all_755,
      u0_voter_all_755,
      u0_vw_voter_all_755
  );

  private static final List<TableType> all_tables_type_in_db_u0_only = ImmutableList.of(
      TableType.TABLE,
      TableType.TABLE,
      TableType.VIEW
  );

  // DB whose warehouse directory has permissions 750 and owned by user1 and group1
  private static final String db_u1g1_only = "db_u1g1_only";

  // Tables in "db_u1g1_only"
  private static final String u1g1_student_all_755 = "student_all_755";
  private static final String u1g1_student_u1_700 = "student_u1_700";
  private static final String u1g1_voter_all_755 = "voter_all_755";
  private static final String u1g1_voter_u1_700 = "voter_u1_700";

  private static final List<String> all_tables_in_db_u1g1_only = ImmutableList.of(
      u1g1_student_all_755,
      u1g1_student_u1_700,
      u1g1_voter_all_755,
      u1g1_voter_u1_700
  );

  private static final List<TableType> all_tables_type_db_u1g1_only = ImmutableList.of(
      TableType.TABLE,
      TableType.TABLE,
      TableType.TABLE,
      TableType.TABLE
  );


  // Create a view on "student_u0_700". View is owned by user0:group0 and has permissions 750
  private static final String v_student_u0g0_750 = "v_student_u0g0_750";

  // Create a view on "v_student_u0g0_750". View is owned by user1:group1 and has permissions 750
  private static final String v_student_u1g1_750 = "v_student_u1g1_750";

  // Create a view on "partitioned_student_u0_700". View is owned by user0:group0 and has permissions 750
  private static final String v_partitioned_student_u0g0_750 = "v_partitioned_student_u0g0_750";

  // Create a view on "v_partitioned_student_u0g0_750". View is owned by user1:group1 and has permissions 750
  private static final String v_partitioned_student_u1g1_750 = "v_partitioned_student_u1g1_750";

  // rwx  -   -
  // 1. Only owning user have read, write and execute rights
  private static final short _700 = (short) 0700;

  // rwx  r-x -
  // 1. Owning user have read, write and execute rights
  // 2. Owning group have read and execute rights
  private static final short _750 = (short) 0750;

  // rwx  r-x r-x
  // 1. Owning user have read, write and execute rights
  // 2. Owning group have read and execute rights
  // 3. Others have read and execute rights
  private static final short _755 = (short) 0755;

  @BeforeClass
  public static void setup() throws Exception {
    startMiniDfsCluster(TestStorageBasedHiveAuthorization.class.getName());
    prepHiveConfAndData();
    setStorabaseBasedAuthorizationInHiveConf();
    startHiveMetaStore();
    startDrillCluster(true);
    addHiveStoragePlugin(getHivePluginConfig());
    addMiniDfsBasedStorage(new HashMap<>());
    generateTestData();
  }

  private static void setStorabaseBasedAuthorizationInHiveConf() {
    // Turn on metastore-side authorization
    hiveConf.set(METASTORE_PRE_EVENT_LISTENERS.varname, AuthorizationPreEventListener.class.getName());
    hiveConf.set(HIVE_METASTORE_AUTHENTICATOR_MANAGER.varname, HadoopDefaultMetastoreAuthenticator.class.getName());
    hiveConf.set(HIVE_METASTORE_AUTHORIZATION_MANAGER.varname, StorageBasedAuthorizationProvider.class.getName());
    hiveConf.set(HIVE_METASTORE_AUTHORIZATION_AUTH_READS.varname, "true");
    hiveConf.set(METASTORE_EXECUTE_SET_UGI.varname, "true");
    hiveConf.set(DYNAMICPARTITIONINGMODE.varname, "nonstrict");
  }

  private static Map<String, String> getHivePluginConfig() {
    final Map<String, String> hiveConfig = Maps.newHashMap();
    hiveConfig.put(METASTOREURIS.varname, hiveConf.get(METASTOREURIS.varname));
    hiveConfig.put(FS_DEFAULT_NAME_KEY, dfsConf.get(FS_DEFAULT_NAME_KEY));
    hiveConfig.put(HIVE_SERVER2_ENABLE_DOAS.varname, hiveConf.get(HIVE_SERVER2_ENABLE_DOAS.varname));
    hiveConfig.put(METASTORE_EXECUTE_SET_UGI.varname, hiveConf.get(METASTORE_EXECUTE_SET_UGI.varname));
    hiveConfig.put(METASTORE_SCHEMA_VERIFICATION.varname, hiveConf.get(METASTORE_SCHEMA_VERIFICATION.varname));
    hiveConfig.put(METASTORE_AUTO_CREATE_ALL.varname, hiveConf.get(METASTORE_AUTO_CREATE_ALL.varname));
    hiveConfig.put(HIVE_CBO_ENABLED.varname, hiveConf.get(HIVE_CBO_ENABLED.varname));
    return hiveConfig;
  }

  /*
   * User       Groups
   * <br/>
   * user0  |   group0
   * user1  |   group0, group1
   * user2  |   group1, group2
   *
   * Generating database objects with permissions:
   * <p>
   * |                                         | org1Users[0] | org1Users[1] | org1Users[2]
   * ---------------------------------------------------------------------------------------
   * db_general                                |      +       |      +       |      +       |
   * db_general.g_student_u0_700               |      +       |      -       |      -       |
   * db_general.g_student_u0g0_750             |      +       |      +       |      -       |
   * db_general.g_student_all_755              |      +       |      +       |      +       |
   * db_general.g_voter_u1_700                 |      -       |      +       |      -       |
   * db_general.g_voter_u2g1_750               |      -       |      +       |      +       |
   * db_general.g_voter_all_755                |      +       |      +       |      +       |
   * db_general.g_partitioned_student_u0_700   |      +       |      -       |      -       |
   * db_general.g_vw_g_student_u0_700          |      +       |      -       |      -       |
   * |                                         |              |              |              |
   * db_u0_only                                |      +       |      -       |      -       |
   * db_u0_only.u0_student_all_755             |      +       |      -       |      -       |
   * db_u0_only.u0_voter_all_755               |      +       |      -       |      -       |
   * db_u0_only.u0_vw_voter_all_755            |      +       |      -       |      -       |
   * |                                         |              |              |              |
   * db_u1g1_only                              |      -       |      +       |      +       |
   * db_u1g1_only.u1g1_student_all_755         |      -       |      +       |      +       |
   * db_u1g1_only.u1g1_student_u1_700          |      -       |      +       |      -       |
   * db_u1g1_only.u1g1_voter_all_755           |      -       |      +       |      +       |
   * db_u1g1_only.u1g1_voter_u1_700            |      -       |      +       |      -       |
   * ---------------------------------------------------------------------------------------
   *
   * @throws Exception - if view creation failed
   */
  private static void generateTestData() throws Exception {

    // Generate Hive test tables
    final SessionState ss = new SessionState(hiveConf);
    SessionState.start(ss);
    final Driver driver = new Driver(hiveConf);

    executeQuery(driver, "CREATE DATABASE " + db_general);
    createTableWithStoragePermissions(driver,
        db_general, g_student_u0_700,
        studentDef, studentData,
        org1Users[0], org1Groups[0],
        _700);
    createHiveView(driver, db_general,
        g_vw_g_student_u0_700, g_student_u0_700);

    createTableWithStoragePermissions(driver,
        db_general, g_student_u0g0_750,
        studentDef, studentData,
        org1Users[0], org1Groups[0],
        _750);
    createTableWithStoragePermissions(driver,
        db_general, g_student_all_755,
        studentDef, studentData,
        org1Users[2], org1Groups[2],
        _755);
    createTableWithStoragePermissions(driver,
        db_general, g_voter_u1_700,
        voterDef, voterData,
        org1Users[1], org1Groups[1],
        _700);
    createTableWithStoragePermissions(driver,
        db_general, g_voter_u2g1_750,
        voterDef, voterData,
        org1Users[2], org1Groups[1],
        _750);
    createTableWithStoragePermissions(driver,
        db_general, g_voter_all_755,
        voterDef, voterData,
        org1Users[1], org1Groups[1],
        _755);

    createPartitionedTable(driver,
        org1Users[0], org1Groups[0]
    );

    changeDBPermissions(db_general, _755, org1Users[0], org1Groups[0]);

    executeQuery(driver, "CREATE DATABASE " + db_u1g1_only);

    createTableWithStoragePermissions(driver,
        db_u1g1_only, u1g1_student_all_755,
        studentDef, studentData,
        org1Users[1], org1Groups[1],
        _755);
    createTableWithStoragePermissions(driver,
        db_u1g1_only, u1g1_student_u1_700,
        studentDef, studentData,
        org1Users[1], org1Groups[1],
        _700);
    createTableWithStoragePermissions(driver,
        db_u1g1_only, u1g1_voter_all_755,
        voterDef, voterData,
        org1Users[1], org1Groups[1],
        _755);
    createTableWithStoragePermissions(driver,
        db_u1g1_only, u1g1_voter_u1_700,
        voterDef, voterData,
        org1Users[1], org1Groups[1],
        _700);

    changeDBPermissions(db_u1g1_only, _750, org1Users[1], org1Groups[1]);


    executeQuery(driver, "CREATE DATABASE " + db_u0_only);
    createTableWithStoragePermissions(driver,
        db_u0_only, u0_student_all_755,
        studentDef, studentData,
        org1Users[0], org1Groups[0],
        _755);
    createTableWithStoragePermissions(driver,
        db_u0_only, u0_voter_all_755,
        voterDef, voterData,
        org1Users[0], org1Groups[0],
        _755);
    createHiveView(driver, db_u0_only, u0_vw_voter_all_755, u0_voter_all_755);
    changeDBPermissions(db_u0_only, _700, org1Users[0], org1Groups[0]);

    createView(org1Users[0], org1Groups[0], v_student_u0g0_750,
        String.format("SELECT rownum, name, age, studentnum FROM %s.%s.%s",
            hivePluginName, db_general, g_student_u0_700));

    createView(org1Users[1], org1Groups[1], v_student_u1g1_750,
        String.format("SELECT rownum, name, age FROM %s.%s.%s", MINI_DFS_STORAGE_PLUGIN_NAME, "tmp", v_student_u0g0_750));

    createView(org1Users[0], org1Groups[0], v_partitioned_student_u0g0_750,
        String.format("SELECT rownum, name, age, studentnum FROM %s.%s.%s",
            hivePluginName, db_general, g_partitioned_student_u0_700));

    createView(org1Users[1], org1Groups[1], v_partitioned_student_u1g1_750,
        String.format("SELECT rownum, name, age FROM %s.%s.%s", MINI_DFS_STORAGE_PLUGIN_NAME, "tmp", v_partitioned_student_u0g0_750));
  }

  private static void createPartitionedTable(final Driver hiveDriver, final String user, final String group) throws Exception {
    executeQuery(hiveDriver, String.format(partitionStudentDef, db_general, g_partitioned_student_u0_700));
    executeQuery(hiveDriver, String.format("INSERT OVERWRITE TABLE %s.%s PARTITION(age) SELECT rownum, name, age, gpa, studentnum FROM %s.%s",
        db_general, g_partitioned_student_u0_700, db_general, g_student_all_755));
    final Path p = getWhPathForHiveObject(TestStorageBasedHiveAuthorization.db_general, TestStorageBasedHiveAuthorization.g_partitioned_student_u0_700);
    fs.setPermission(p, new FsPermission(TestStorageBasedHiveAuthorization._700));
    fs.setOwner(p, user, group);
  }

  private static void changeDBPermissions(final String db, final short perm, final String u, final String g) throws Exception {
    Path p = getWhPathForHiveObject(db, null);
    fs.setPermission(p, new FsPermission(perm));
    fs.setOwner(p, u, g);
  }


  private static void  createHiveView(Driver driver, String db, String viewName, String tableName) throws IOException {
    executeQuery(driver, String.format("CREATE OR REPLACE VIEW %s.%s AS SELECT * FROM %s.%s LIMIT 1",
        db, viewName, db, tableName));
  }

  // Irrespective of each db permissions, all dbs show up in "SHOW SCHEMAS"
  @Test
  public void showSchemas() throws Exception {
    testBuilder()
        .sqlQuery("SHOW SCHEMAS LIKE 'hive.%'")
        .unOrdered()
        .baselineColumns("SCHEMA_NAME")
        .baselineValues("hive.db_general")
        .baselineValues("hive.db_u0_only")
        .baselineValues("hive.db_u1g1_only")
        .baselineValues("hive.default")
        .go();
  }

  /**
   * Should only contain the tables that the user
   * has access to read.
   *
   * @throws Exception
   */
  @Test
  public void user0_db_general_showTables() throws Exception {
    updateClient(org1Users[0]);
    showTablesHelper(db_general, all_tables_in_db_general);
  }

  @Test
  public void user0_db_u0_only_showTables() throws Exception {
    updateClient(org1Users[0]);
    showTablesHelper(db_u0_only, all_tables_in_db_u0_only);
  }

  /**
   * If the user has no read access to the db, the list will be always empty even if the user has
   * read access to the tables inside the db.
   */
  @Test
  public void user0_db_u1g1_only_showTables() throws Exception {
    updateClient(org1Users[0]);
    showTablesHelper(db_u1g1_only, all_tables_in_db_u1g1_only);
  }

  @Test
  public void user0_db_general_infoSchema() throws Exception {
    updateClient(org1Users[0]);
    fromInfoSchemaHelper(db_general,
        all_tables_in_db_general,
        all_tables_type_in_db_general);
  }

  @Test
  public void user0_db_u0_only_infoSchema() throws Exception {
    updateClient(org1Users[0]);
    fromInfoSchemaHelper(db_u0_only,
        all_tables_in_db_u0_only,
        all_tables_type_in_db_u0_only);
  }

  @Test
  public void user0_db_u1g1_only_infoSchema() throws Exception {
    updateClient(org1Users[0]);
    fromInfoSchemaHelper(db_u1g1_only, all_tables_in_db_u1g1_only, all_tables_type_db_u1g1_only);
  }

  /**
   * user0 is 700 owner
   */
  @Test
  public void user0_allowed_g_student_u0_700() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_general, g_student_u0_700);
  }

  @Test
  public void user0_allowed_g_vw_u0_700_over_g_student_u0_700() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_general, g_vw_g_student_u0_700);
  }

  @Test
  public void user1_forbidden_g_vw_u0_700_over_g_student_u0_700() throws Exception {
    updateClient(org1Users[1]);
    queryHiveViewFailed(db_general, g_vw_g_student_u0_700);
  }

  @Test
  public void user2_forbidden_g_vw_u0_700_over_g_student_u0_700() throws Exception {
    updateClient(org1Users[2]);
    queryHiveViewFailed(db_general, g_vw_g_student_u0_700);
  }

  @Test
  public void user0_allowed_u0_vw_voter_all_755() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_u0_only, u0_vw_voter_all_755);
  }

  @Test
  public void user1_forbidden_u0_vw_voter_all_755() throws Exception {
    updateClient(org1Users[1]);
    queryHiveViewFailed(db_u0_only, u0_vw_voter_all_755);
  }

  @Test
  public void user2_forbidden_u0_vw_voter_all_755() throws Exception {
    updateClient(org1Users[2]);
    queryHiveViewFailed(db_u0_only, u0_vw_voter_all_755);
  }

  private void queryHiveViewFailed(String db, String viewName) throws Exception {
    errorMsgTestHelper(
        String.format("SELECT * FROM hive.%s.%s LIMIT 2", db, viewName),
        "Failure validating a view your query is dependent upon.");
  }

  /**
   * user0 is 750 owner
   */
  @Test
  public void user0_allowed_g_student_u0g0_750() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_general, g_student_u0g0_750);
  }

  /**
   * table owned by user2 and group2,
   * but user0 can access because Others allowed to read and execute
   */
  @Test
  public void user0_allowed_g_student_all_755() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_general, g_student_all_755);
  }

  /**
   * user0 can't access because, user1 is 700 owner
   */
  @Test
  public void user0_forbidden_g_voter_u1_700() throws Exception{
    updateClient(org1Users[0]);
    queryTableNotFound(db_general, g_voter_u1_700);
  }

  /**
   * user0 can't access, because only user2 and group1 members
   */
  @Test
  public void user0_forbidden_g_voter_u2g1_750() throws Exception{
    updateClient(org1Users[0]);
    queryTableNotFound(db_general, g_voter_u2g1_750);
  }

  /**
   * user0 allowed because others have r-x access. Despite
   * of user1 and group1 ownership over the table.
   */
  @Test
  public void user0_allowed_g_voter_all_755() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_general, g_voter_all_755);
  }

  /**
   * user0 is 755 owner
   */
  @Test
  public void user0_allowed_u0_student_all_755() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_u0_only, u0_student_all_755);
  }

  /**
   * user0 is 755 owner
   */
  @Test
  public void user0_allowed_u0_voter_all_755() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_u0_only, u0_voter_all_755);
  }

  /**
   * user0 is 700 owner
   */
  @Test
  public void user0_allowed_g_partitioned_student_u0_700() throws Exception {
    updateClient(org1Users[0]);
    queryHiveTableOrView(db_general, g_partitioned_student_u0_700);
  }

  /**
   * user0 doesn't have access to database db_u1g1_only
   */
  @Test
  public void user0_forbidden_u1g1_student_all_755() throws Exception {
    updateClient(org1Users[0]);
    queryTableNotFound(db_u1g1_only, u1g1_student_all_755);
  }

  @Test
  public void user0_allowed_v_student_u0g0_750() throws Exception {
    updateClient(org1Users[0]);
    queryView(v_student_u0g0_750);
  }

  @Test
  public void user0_forbidden_v_student_u1g1_750() throws Exception {
    updateClient(org1Users[0]);
    queryViewNotAuthorized(v_student_u1g1_750);
  }

  @Test
  public void user0_allowed_v_partitioned_student_u0g0_750() throws Exception {
    updateClient(org1Users[0]);
    queryView(v_partitioned_student_u0g0_750);
  }

  @Test
  public void user0_forbidden_v_partitioned_student_u1g1_750() throws Exception {
    updateClient(org1Users[0]);
    queryViewNotAuthorized(v_partitioned_student_u1g1_750);
  }

  @Test
  public void user1_db_general_showTables() throws Exception {
    updateClient(org1Users[1]);
    showTablesHelper(db_general, all_tables_in_db_general);
  }

  @Test
  public void user1_db_u1g1_only_showTables() throws Exception {
    updateClient(org1Users[1]);
    showTablesHelper(db_u1g1_only, all_tables_in_db_u1g1_only);
  }

  @Test
  public void user1_db_u0_only_showTables() throws Exception {
    updateClient(org1Users[1]);
    showTablesHelper(db_u0_only, all_tables_in_db_u0_only);
  }

  @Test
  public void user1_db_general_infoSchema() throws Exception {
    updateClient(org1Users[1]);
    fromInfoSchemaHelper(db_general,
        all_tables_in_db_general,
        all_tables_type_in_db_general);
  }

  @Test
  public void user1_db_u1g1_only_infoSchema() throws Exception {
    updateClient(org1Users[1]);
    fromInfoSchemaHelper(db_u1g1_only,
        ImmutableList.of(
            u1g1_student_all_755,
            u1g1_student_u1_700,
            u1g1_voter_all_755,
            u1g1_voter_u1_700
        ),
        ImmutableList.of(
            TableType.TABLE,
            TableType.TABLE,
            TableType.TABLE,
            TableType.TABLE
        ));
  }

  @Test
  public void user1_db_u0_only_infoSchema() throws Exception {
    updateClient(org1Users[1]);
    fromInfoSchemaHelper(db_u0_only,
        newArrayList(u0_vw_voter_all_755, u0_student_all_755, u0_voter_all_755),
        newArrayList(TableType.VIEW, TableType.TABLE, TableType.TABLE));
  }

  /**
   * user1 can't access, because user0 is 700 owner
   */
  @Test
  public void user1_forbidden_g_student_u0_700() throws Exception {
    updateClient(org1Users[1]);
    queryTableNotFound(db_general, g_student_u0_700);
  }

  /**
   * user1 allowed because he's a member of group0
   */
  @Test
  public void user1_allowed_g_student_u0g0_750() throws Exception {
    updateClient(org1Users[1]);
    queryHiveTableOrView(db_general, g_student_u0g0_750);
  }

  /**
   * user1 allowed because Others have r-x access
   */
  @Test
  public void user1_allowed_g_student_all_755() throws Exception {
    updateClient(org1Users[1]);
    queryHiveTableOrView(db_general, g_student_all_755);
  }

  /**
   * user1 is 700 owner
   */
  @Test
  public void user1_allowed_g_voter_u1_700() throws Exception {
    updateClient(org1Users[1]);
    queryHiveTableOrView(db_general, g_voter_u1_700);
  }

  /**
   * user1 allowed because he's member of group1
   */
  @Test
  public void user1_allowed_g_voter_u2g1_750() throws Exception {
    updateClient(org1Users[1]);
    queryHiveTableOrView(db_general, g_voter_u2g1_750);
  }

  /**
   * user1 is 755 owner
   */
  @Test
  public void user1_allowed_g_voter_all_755() throws Exception {
    updateClient(org1Users[1]);
    queryHiveTableOrView(db_general, g_voter_all_755);
  }

  /**
   * here access restricted at db level, only user0 can access  db_u0_only
   */
  @Test
  public void user1_forbidden_u0_student_all_755() throws Exception {
    updateClient(org1Users[1]);
    queryTableNotFound(db_u0_only, u0_student_all_755);
  }

  /**
   * here access restricted at db level, only user0 can access db_u0_only
   */
  @Test
  public void user1_forbidden_u0_voter_all_755() throws Exception {
    updateClient(org1Users[1]);
    queryTableNotFound(db_u0_only, u0_voter_all_755);
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
  public void user1_allowed_v_partitioned_student_u0g0_750() throws Exception {
    updateClient(org1Users[1]);
    queryView(v_partitioned_student_u0g0_750);
  }

  @Test
  public void user1_allowed_v_partitioned_student_u1g1_750() throws Exception {
    updateClient(org1Users[1]);
    queryView(v_partitioned_student_u1g1_750);
  }

  @Test
  public void user2_db_general_showTables() throws Exception {
    updateClient(org1Users[2]);
    showTablesHelper(db_general, all_tables_in_db_general);
  }

  @Test
  public void user2_db_u1g1_only_showTables() throws Exception {
    updateClient(org1Users[2]);
    showTablesHelper(db_u1g1_only, all_tables_in_db_u1g1_only);
  }

  @Test
  public void user2_db_u0_only_showTables() throws Exception {
    updateClient(org1Users[2]);
    showTablesHelper(db_u0_only, all_tables_in_db_u0_only);
  }

  @Test
  public void user2_db_general_infoSchema() throws Exception {
    updateClient(org1Users[2]);
    fromInfoSchemaHelper(db_general,
        all_tables_in_db_general,
        all_tables_type_in_db_general);
  }

  @Test
  public void user2_db_u1g1_only_infoSchema() throws Exception {
    updateClient(org1Users[2]);
    fromInfoSchemaHelper(db_u1g1_only,
        all_tables_in_db_u1g1_only,
        all_tables_type_db_u1g1_only);
  }

  @Test
  public void user2_db_u0_only_infoSchema() throws Exception {
    updateClient(org1Users[2]);
    fromInfoSchemaHelper(db_u0_only,
        newArrayList(all_tables_in_db_u0_only),
        newArrayList(all_tables_type_in_db_u0_only));
  }

  /**
   * user2 can't access, because user0 is 700 owner
   */
  @Test
  public void user2_forbidden_g_student_u0_700() throws Exception {
    updateClient(org1Users[2]);
    queryTableNotFound(db_general, g_student_u0_700);
  }

  /**
   * user2 can't access, only user0 and group0 members have access
   */
  @Test
  public void user2_forbidden_g_student_u0g0_750() throws Exception {
    updateClient(org1Users[2]);
    queryTableNotFound(db_general, g_student_u0_700);
  }

  /**
   * user2 is 755 owner
   */
  @Test
  public void user2_allowed_g_student_all_755() throws Exception {
    updateClient(org1Users[2]);
    queryHiveTableOrView(db_general, g_student_all_755);
  }

  /**
   * user2 can't access, because user1 is 700 owner
   */
  @Test
  public void user2_forbidden_g_voter_u1_700() throws Exception {
    updateClient(org1Users[2]);
    queryTableNotFound(db_general, g_voter_u1_700);
  }

  /**
   * user2 is 750 owner
   */
  @Test
  public void user2_allowed_g_voter_u2g1_750() throws Exception {
    updateClient(org1Users[2]);
    queryHiveTableOrView(db_general, g_voter_u2g1_750);
  }

  /**
   * user2 is member of group1
   */
  @Test
  public void user2_allowed_g_voter_all_755() throws Exception {
    updateClient(org1Users[2]);
    queryHiveTableOrView(db_general, g_voter_all_755);
  }

  /**
   * here access restricted at db level, only user0 can access db_u0_only
   */
  @Test
  public void user2_forbidden_u0_student_all_755() throws Exception {
    updateClient(org1Users[2]);
    queryTableNotFound(db_u0_only, u0_student_all_755);
  }

  /**
   * here access restricted at db level, only user0 can access db_u0_only
   */
  @Test
  public void user2_forbidden_u0_voter_all_755() throws Exception {
    updateClient(org1Users[2]);
    queryTableNotFound(db_u0_only, u0_voter_all_755);
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

  @Test
  public void user2_forbidden_v_partitioned_student_u0g0_750() throws Exception {
    updateClient(org1Users[2]);
    queryViewNotAuthorized(v_partitioned_student_u0g0_750);
  }

  @Test
  public void user2_allowed_v_partitioned_student_u1g1_750() throws Exception {
    updateClient(org1Users[2]);
    queryView(v_partitioned_student_u1g1_750);
  }

  @AfterClass
  public static void shutdown() throws Exception {
    stopMiniDfsCluster();
    stopHiveMetaStore();
  }

  private static void queryHiveTableOrView(String db, String table) throws Exception {
    test(String.format("SELECT * FROM hive.%s.%s LIMIT 2", db, table));
  }

  private static void queryTableNotFound(String db, String table) throws Exception {
    errorMsgTestHelper(
        String.format("SELECT * FROM hive.%s.%s LIMIT 2", db, table),
        String.format("Object '%s' not found within 'hive.%s'", table, db));
  }

}
