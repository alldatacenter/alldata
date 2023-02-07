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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.calcite.schema.Schema.TableType;
import org.apache.drill.exec.hive.HiveTestUtilities;
import org.apache.drill.exec.impersonation.BaseTestImpersonation;
import org.apache.drill.exec.store.hive.HiveStoragePluginConfig;
import org.apache.drill.test.TestBuilder;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.permission.FsPermission;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.conf.HiveConf.ConfVars;
import org.apache.hadoop.hive.ql.Driver;
import org.junit.BeforeClass;

import static org.apache.drill.exec.hive.HiveTestUtilities.createDirWithPosixPermissions;
import static org.apache.drill.exec.hive.HiveTestUtilities.executeQuery;
import static org.apache.hadoop.fs.FileSystem.FS_DEFAULT_NAME_KEY;
import static org.apache.hadoop.hive.conf.HiveConf.ConfVars.METASTOREURIS;

public class BaseTestHiveImpersonation extends BaseTestImpersonation {
  protected static final String hivePluginName = "hive";

  protected static HiveConf hiveConf;
  protected static String whDir;

  protected static String studentData;
  protected static String voterData;

  protected static final String studentDef = "CREATE TABLE %s.%s" +
      "(rownum int, name string, age int, gpa float, studentnum bigint) " +
      "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE";

  protected static final String voterDef = "CREATE TABLE %s.%s" +
      "(voter_id int,name varchar(30), age tinyint, registration string, " +
      "contributions double,voterzone smallint,create_time timestamp) " +
      "ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE";

  protected static final String partitionStudentDef = "CREATE TABLE %s.%s" +
      "(rownum INT, name STRING, gpa FLOAT, studentnum BIGINT) " +
      "partitioned by (age INT) ROW FORMAT DELIMITED FIELDS TERMINATED BY ',' STORED AS TEXTFILE";

  @BeforeClass
  public static void setUp() {
    HiveTestUtilities.assumeJavaVersion();
  }

  protected static void prepHiveConfAndData() throws Exception {
    hiveConf = new HiveConf();

    File scratchDir = createDirWithPosixPermissions(dirTestWatcher.getRootDir(), "scratch_dir");
    File localScratchDir = createDirWithPosixPermissions(dirTestWatcher.getRootDir(), "local_scratch_dir");
    File metaStoreDBDir = new File(dirTestWatcher.getRootDir(), "metastore_db");

    // Configure metastore persistence db location on local filesystem
    final String dbUrl = String.format("jdbc:derby:;databaseName=%s;create=true",  metaStoreDBDir.getAbsolutePath());
    hiveConf.set(ConfVars.METASTORECONNECTURLKEY.varname, dbUrl);

    hiveConf.set(ConfVars.SCRATCHDIR.varname, "file://" + scratchDir.getAbsolutePath());
    hiveConf.set(ConfVars.LOCALSCRATCHDIR.varname, localScratchDir.getAbsolutePath());
    hiveConf.set(ConfVars.METASTORE_SCHEMA_VERIFICATION.varname, "false");
    hiveConf.set(ConfVars.METASTORE_AUTO_CREATE_ALL.varname, "true");
    hiveConf.set(ConfVars.HIVE_CBO_ENABLED.varname, "false");
    hiveConf.set(ConfVars.HIVESTATSAUTOGATHER.varname, "false");
    hiveConf.set(ConfVars.HIVESTATSCOLAUTOGATHER.varname, "false");
    hiveConf.set(ConfVars.HIVESESSIONSILENT.varname, "true");

    // Set MiniDFS conf in HiveConf
    hiveConf.set(FS_DEFAULT_NAME_KEY, dfsConf.get(FS_DEFAULT_NAME_KEY));

    whDir = hiveConf.get(ConfVars.METASTOREWAREHOUSE.varname);
    FileSystem.mkdirs(fs, new Path(whDir), new FsPermission((short) 0777));

    studentData = getPhysicalFileFromResource("student.txt");
    voterData = getPhysicalFileFromResource("voter.txt");
  }

  protected static void startHiveMetaStore() throws Exception {
    Class<?> metaStoreUtilsClass;
    Class<?> hadoopThriftAuthBridgeClass;
    Class<?> confClass;
    Object hadoopThriftAuthBridge;
    // TODO: remove reflection stuff when all supported profiles will be switched to Hive 3+ version
    try {
      metaStoreUtilsClass = Class.forName("org.apache.hadoop.hive.metastore.utils.MetaStoreUtils");
      hadoopThriftAuthBridgeClass = Class.forName("org.apache.hadoop.hive.metastore.security.HadoopThriftAuthBridge");
      hadoopThriftAuthBridge = hadoopThriftAuthBridgeClass.getDeclaredMethod("getBridge").invoke(null);
      confClass = Configuration.class;
    } catch (ClassNotFoundException e) {
      metaStoreUtilsClass = Class.forName("org.apache.hadoop.hive.metastore.MetaStoreUtils");
      hadoopThriftAuthBridgeClass = Class.forName("org.apache.hadoop.hive.thrift.HadoopThriftAuthBridge");
      hadoopThriftAuthBridge = Class.forName("org.apache.hadoop.hive.shims.ShimLoader")
          .getDeclaredMethod("getHadoopThriftAuthBridge").invoke(null);
      confClass = HiveConf.class;
    }
    final int port = (int) metaStoreUtilsClass.getDeclaredMethod("findFreePort").invoke(null);

    hiveConf.set(METASTOREURIS.varname, "thrift://localhost:" + port);

    metaStoreUtilsClass.getDeclaredMethod("startMetaStore", int.class, hadoopThriftAuthBridgeClass, confClass)
        .invoke(null, port, hadoopThriftAuthBridge, hiveConf);
  }

  protected static HiveStoragePluginConfig createHiveStoragePlugin(final Map<String, String> hiveConfig) throws Exception {
    HiveStoragePluginConfig pluginConfig = new HiveStoragePluginConfig(hiveConfig);
    pluginConfig.setEnabled(true);
    return pluginConfig;
  }

  protected static Path getWhPathForHiveObject(final String dbName, final String tableName) {
    if (dbName == null) {
      return new Path(whDir);
    }

    if (tableName == null) {
      return new Path(whDir, dbName + ".db");
    }

    return new Path(new Path(whDir, dbName + ".db"), tableName);
  }

  protected static void addHiveStoragePlugin(final Map<String, String> hiveConfig) throws Exception {
    getDrillbitContext().getStorage().put(hivePluginName, createHiveStoragePlugin(hiveConfig));
  }

  protected void showTablesHelper(final String db, List<String> expectedTables) throws Exception {
    final String dbQualified = hivePluginName + "." + db;
    final TestBuilder testBuilder = testBuilder()
        .sqlQuery("SHOW TABLES IN " + dbQualified)
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME");

    if (expectedTables.size() == 0) {
      testBuilder.expectsEmptyResultSet();
    } else {
      for (String tbl : expectedTables) {
        testBuilder.baselineValues(dbQualified, tbl);
      }
    }

    testBuilder.go();
  }

  protected void fromInfoSchemaHelper(final String db, List<String> expectedTables, List<TableType> expectedTableTypes) throws Exception {
    final String dbQualified = hivePluginName + "." + db;
    final TestBuilder testBuilder = testBuilder()
        .sqlQuery("SELECT TABLE_SCHEMA, TABLE_NAME, TABLE_TYPE \n" +
            "FROM INFORMATION_SCHEMA.`TABLES` \n" +
            "WHERE TABLE_SCHEMA = '" + dbQualified + "'")
        .unOrdered()
        .baselineColumns("TABLE_SCHEMA", "TABLE_NAME", "TABLE_TYPE");

    if (expectedTables.size() == 0) {
      testBuilder.expectsEmptyResultSet();
    } else {
      for (int i = 0; i < expectedTables.size(); ++i) {
        testBuilder.baselineValues(dbQualified, expectedTables.get(i), expectedTableTypes.get(i).toString());
      }
    }

    testBuilder.go();
  }

  public static void stopHiveMetaStore() throws Exception {
    // Unfortunately Hive metastore doesn't provide an API to shut it down. It will be exited as part of the test JVM
    // exit. As each metastore server instance is using its own resources and not sharing it with other metastore
    // server instances this should be ok.
  }

  static void queryView(String viewName) throws Exception {
    String query = String.format("SELECT rownum FROM %s.tmp.%s ORDER BY rownum LIMIT 1", MINI_DFS_STORAGE_PLUGIN_NAME, viewName);
    testBuilder()
        .sqlQuery(query)
        .unOrdered()
        .baselineColumns("rownum")
        .baselineValues(1)
        .go();
  }

  static void queryViewNotAuthorized(String viewName) throws Exception {
    String query = String.format("SELECT rownum FROM %s.tmp.%s ORDER BY rownum LIMIT 1", MINI_DFS_STORAGE_PLUGIN_NAME, viewName);
    errorMsgTestHelper(query, String.format(
        "Not authorized to read view [%s] in schema [%s.tmp]", viewName, MINI_DFS_STORAGE_PLUGIN_NAME));
  }

  static void createTableWithStoragePermissions(final Driver hiveDriver, final String db, final String tbl, final String tblDef,
                                                final String tblData, final String user, final String group, final short permissions) throws Exception {
    createTable(hiveDriver, db, tbl, tblDef, tblData);
    setStoragePermissions(db, tbl, user, group, permissions);
  }

  static void setStoragePermissions(String db, String tbl, String user, String group, short permissions) throws IOException {
    final Path p = getWhPathForHiveObject(db, tbl);
    fs.setPermission(p, new FsPermission(permissions));
    fs.setOwner(p, user, group);
  }

  static void createTable(final Driver driver, final String db, final String tbl, final String tblDef,
                          final String data) throws Exception {
    executeQuery(driver, String.format(tblDef, db, tbl));
    executeQuery(driver, String.format("LOAD DATA LOCAL INPATH '%s' INTO TABLE %s.%s", data, db, tbl));
  }

}
