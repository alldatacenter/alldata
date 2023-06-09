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
package org.apache.drill.hbase;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.exec.ZookeeperTestUtil;
import org.apache.drill.hbase.test.Drill2130StorageHBaseHamcrestConfigurationTest;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HBaseTestingUtility;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
  Drill2130StorageHBaseHamcrestConfigurationTest.class,
  HBaseRecordReaderTest.class,
  TestHBaseCFAsJSONString.class,
  TestHBaseConnectionManager.class,
  TestHBaseFilterPushDown.class,
  TestHBaseProjectPushDown.class,
  TestHBaseQueries.class,
  TestHBaseRegexParser.class,
  TestHBaseRegionScanAssignments.class,
  TestHBaseTableProvider.class,
  TestOrderedBytesConvertFunctions.class
})
public class HBaseTestsSuite extends BaseTest {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(HBaseTestsSuite.class);

  private static final boolean IS_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

  protected static final TableName TEST_TABLE_1 = TableName.valueOf("TestTable1");
  protected static final TableName TEST_TABLE_2 = TableName.valueOf("TestTable2");
  protected static final TableName TEST_TABLE_3 = TableName.valueOf("TestTable3");
  protected static final TableName TEST_TABLE_MULTI_CF_DIFFERENT_CASE = TableName.valueOf("TestTableMultiCF");
  protected static final TableName TEST_TABLE_COMPOSITE_DATE = TableName.valueOf("TestTableCompositeDate");
  protected static final TableName TEST_TABLE_COMPOSITE_TIME = TableName.valueOf("TestTableCompositeTime");
  protected static final TableName TEST_TABLE_COMPOSITE_INT = TableName.valueOf("TestTableCompositeInt");
  protected static final TableName TEST_TABLE_DOUBLE_OB = TableName.valueOf("TestTableDoubleOB");
  protected static final TableName TEST_TABLE_FLOAT_OB = TableName.valueOf("TestTableFloatOB");
  protected static final TableName TEST_TABLE_BIGINT_OB = TableName.valueOf("TestTableBigIntOB");
  protected static final TableName TEST_TABLE_INT_OB = TableName.valueOf("TestTableIntOB");
  protected static final TableName TEST_TABLE_DOUBLE_OB_DESC = TableName.valueOf("TestTableDoubleOBDesc");
  protected static final TableName TEST_TABLE_FLOAT_OB_DESC = TableName.valueOf("TestTableFloatOBDesc");
  protected static final TableName TEST_TABLE_BIGINT_OB_DESC = TableName.valueOf("TestTableBigIntOBDesc");
  protected static final TableName TEST_TABLE_INT_OB_DESC = TableName.valueOf("TestTableIntOBDesc");
  protected static final TableName TEST_TABLE_NULL_STR = TableName.valueOf("TestTableNullStr");

  private static Configuration conf;

  private static Connection conn;
  private static Admin admin;

  private static HBaseTestingUtility UTIL;

  private static volatile AtomicInteger initCount = new AtomicInteger(0);

  /**
   * This flag controls whether {@link HBaseTestsSuite} starts a mini HBase cluster to run the unit test.
   */
  private static boolean manageHBaseCluster = System.getProperty("drill.hbase.tests.manageHBaseCluster", "true").equalsIgnoreCase("true");
  private static boolean hbaseClusterCreated = false;

  private static boolean createTables = System.getProperty("drill.hbase.tests.createTables", "true").equalsIgnoreCase("true");
  private static boolean tablesCreated = false;

  @BeforeClass
  public static void initCluster() throws Exception {
    ZookeeperTestUtil.setJaasTestConfigFile();

    if (initCount.get() == 0) {
      synchronized (HBaseTestsSuite.class) {
        if (initCount.get() == 0) {
          conf = HBaseConfiguration.create();
          conf.set(HConstants.HBASE_CLIENT_INSTANCE_ID, "drill-hbase-unit-tests-client");
          if (IS_DEBUG) {
            conf.set("hbase.client.scanner.timeout.period","10000000");
          }

          if (manageHBaseCluster) {
            logger.info("Starting HBase mini cluster.");
            UTIL = new HBaseTestingUtility(conf);
            UTIL.startMiniZKCluster();
            String old_home = System.getProperty("user.home");
            System.setProperty("user.home", UTIL.getDataTestDir().toString());
            UTIL.startMiniHBaseCluster(1, 1);
            System.setProperty("user.home", old_home);
            hbaseClusterCreated = true;
            logger.info("HBase mini cluster started. Zookeeper port: '{}'", getZookeeperPort());
          }

          conn = ConnectionFactory.createConnection(conf);
          admin = conn.getAdmin();

          if (createTables || !tablesExist()) {
            createTestTables();
            tablesCreated = true;
          }
          initCount.incrementAndGet();
          return;
        }
      }
    }
    initCount.incrementAndGet();
  }

  @AfterClass
  public static void tearDownCluster() throws Exception {
    synchronized (HBaseTestsSuite.class) {
      if (initCount.decrementAndGet() == 0) {
        if (createTables && tablesCreated) {
          cleanupTestTables();
        }

        if (admin != null) {
          admin.close();
        }

        if (hbaseClusterCreated) {
          logger.info("Shutting down HBase mini cluster.");
          UTIL.shutdownMiniCluster();
          logger.info("HBase mini cluster stopped.");
        }
      }
    }
  }

  public static Configuration getConf() {
    return conf;
  }

  public static HBaseTestingUtility getHBaseTestingUtility() {
    return UTIL;
  }

  private static boolean tablesExist() throws IOException {
    return admin.tableExists(TEST_TABLE_1) && admin.tableExists(TEST_TABLE_2)
           && admin.tableExists(TEST_TABLE_3)
           && admin.tableExists(TEST_TABLE_MULTI_CF_DIFFERENT_CASE)
           && admin.tableExists(TEST_TABLE_COMPOSITE_DATE)
           && admin.tableExists(TEST_TABLE_COMPOSITE_TIME)
           && admin.tableExists(TEST_TABLE_COMPOSITE_INT)
           && admin.tableExists(TEST_TABLE_DOUBLE_OB)
           && admin.tableExists(TEST_TABLE_FLOAT_OB)
           && admin.tableExists(TEST_TABLE_BIGINT_OB)
           && admin.tableExists(TEST_TABLE_INT_OB)
           && admin.tableExists(TEST_TABLE_DOUBLE_OB_DESC)
           && admin.tableExists(TEST_TABLE_FLOAT_OB_DESC)
           && admin.tableExists(TEST_TABLE_BIGINT_OB_DESC)
           && admin.tableExists(TEST_TABLE_INT_OB_DESC)
           && admin.tableExists(TEST_TABLE_NULL_STR);
  }

  private static void createTestTables() throws Exception {
    // TODO(DRILL-3954):  Change number of regions from 1 to multiple for other
    // tables and remaining problems not addressed by DRILL-2288 fixes.
    /*
     * We are seeing some issues with (Drill) Filter operator if a group scan span
     * multiple fragments. Hence the number of regions in the HBase table is set to 1.
     * Will revert to multiple region once the issue is resolved.
     */
    TestTableGenerator.generateHBaseDataset1(conn, admin, TEST_TABLE_1, 2);
    TestTableGenerator.generateHBaseDatasetSingleSchema(conn, admin, TEST_TABLE_2, 1);
    TestTableGenerator.generateHBaseDataset3(conn, admin, TEST_TABLE_3, 1);
    TestTableGenerator.generateHBaseDatasetMultiCF(conn, admin, TEST_TABLE_MULTI_CF_DIFFERENT_CASE, 1);
    TestTableGenerator.generateHBaseDatasetCompositeKeyDate(conn, admin, TEST_TABLE_COMPOSITE_DATE, 1);
    TestTableGenerator.generateHBaseDatasetCompositeKeyTime(conn, admin, TEST_TABLE_COMPOSITE_TIME, 1);
    TestTableGenerator.generateHBaseDatasetCompositeKeyInt(conn, admin, TEST_TABLE_COMPOSITE_INT, 1);
    TestTableGenerator.generateHBaseDatasetDoubleOB(conn, admin, TEST_TABLE_DOUBLE_OB, 1);
    TestTableGenerator.generateHBaseDatasetFloatOB(conn, admin, TEST_TABLE_FLOAT_OB, 1);
    TestTableGenerator.generateHBaseDatasetBigIntOB(conn, admin, TEST_TABLE_BIGINT_OB, 1);
    TestTableGenerator.generateHBaseDatasetIntOB(conn, admin, TEST_TABLE_INT_OB, 1);
    TestTableGenerator.generateHBaseDatasetDoubleOBDesc(conn, admin, TEST_TABLE_DOUBLE_OB_DESC, 1);
    TestTableGenerator.generateHBaseDatasetFloatOBDesc(conn, admin, TEST_TABLE_FLOAT_OB_DESC, 1);
    TestTableGenerator.generateHBaseDatasetBigIntOBDesc(conn, admin, TEST_TABLE_BIGINT_OB_DESC, 1);
    TestTableGenerator.generateHBaseDatasetIntOBDesc(conn, admin, TEST_TABLE_INT_OB_DESC, 1);
    TestTableGenerator.generateHBaseDatasetNullStr(conn, admin, TEST_TABLE_NULL_STR, 1);
  }

  private static void cleanupTestTables() throws IOException {
    admin.disableTable(TEST_TABLE_1);
    admin.deleteTable(TEST_TABLE_1);
    admin.disableTable(TEST_TABLE_2);
    admin.deleteTable(TEST_TABLE_2);
    admin.disableTable(TEST_TABLE_3);
    admin.deleteTable(TEST_TABLE_3);
    admin.disableTable(TEST_TABLE_MULTI_CF_DIFFERENT_CASE);
    admin.deleteTable(TEST_TABLE_MULTI_CF_DIFFERENT_CASE);
    admin.disableTable(TEST_TABLE_COMPOSITE_DATE);
    admin.deleteTable(TEST_TABLE_COMPOSITE_DATE);
    admin.disableTable(TEST_TABLE_COMPOSITE_TIME);
    admin.deleteTable(TEST_TABLE_COMPOSITE_TIME);
    admin.disableTable(TEST_TABLE_COMPOSITE_INT);
    admin.deleteTable(TEST_TABLE_COMPOSITE_INT);
    admin.disableTable(TEST_TABLE_DOUBLE_OB);
    admin.deleteTable(TEST_TABLE_DOUBLE_OB);
    admin.disableTable(TEST_TABLE_FLOAT_OB);
    admin.deleteTable(TEST_TABLE_FLOAT_OB);
    admin.disableTable(TEST_TABLE_BIGINT_OB);
    admin.deleteTable(TEST_TABLE_BIGINT_OB);
    admin.disableTable(TEST_TABLE_INT_OB);
    admin.deleteTable(TEST_TABLE_INT_OB);
    admin.disableTable(TEST_TABLE_DOUBLE_OB_DESC);
    admin.deleteTable(TEST_TABLE_DOUBLE_OB_DESC);
    admin.disableTable(TEST_TABLE_FLOAT_OB_DESC);
    admin.deleteTable(TEST_TABLE_FLOAT_OB_DESC);
    admin.disableTable(TEST_TABLE_BIGINT_OB_DESC);
    admin.deleteTable(TEST_TABLE_BIGINT_OB_DESC);
    admin.disableTable(TEST_TABLE_INT_OB_DESC);
    admin.deleteTable(TEST_TABLE_INT_OB_DESC);
    admin.disableTable(TEST_TABLE_NULL_STR);
    admin.deleteTable(TEST_TABLE_NULL_STR);
  }

  public static int getZookeeperPort() {
    return getConf().getInt(HConstants.ZOOKEEPER_CLIENT_PORT, 2181);
  }

  public static void configure(boolean manageHBaseCluster, boolean createTables) {
    HBaseTestsSuite.manageHBaseCluster = manageHBaseCluster;
    HBaseTestsSuite.createTables = createTables;
  }

  public static Admin getAdmin() {
    return admin;
  }

  public static Connection getConnection() {
    return conn;
  }

}
