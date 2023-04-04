/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark;

import com.netease.arctic.spark.hive.TestCreateTableDDL;
import com.netease.arctic.spark.hive.TestHiveTableDropPartitions;
import com.netease.arctic.spark.hive.TestHiveTableMergeOnRead;
import com.netease.arctic.spark.hive.TestHiveTableTruncate;
import com.netease.arctic.spark.hive.TestKeyedHiveInsertOverwriteDynamic;
import com.netease.arctic.spark.hive.TestKeyedHiveInsertOverwriteStatic;
import com.netease.arctic.spark.hive.TestKeyedTableDml;
import com.netease.arctic.spark.hive.TestKeyedTableMergeInto;
import com.netease.arctic.spark.hive.TestMigrateHiveTable;
import com.netease.arctic.spark.hive.TestUnKeyedTableMergeInto;
import com.netease.arctic.spark.hive.TestUnkeyedHiveInsertOverwriteDynamic;
import com.netease.arctic.spark.hive.TestUnkeyedHiveInsertOverwriteStatic;
import com.netease.arctic.spark.hive.TestUnkeyedTableDml;
import com.netease.arctic.spark.source.TestKeyedTableDataFrameAPI;
import com.netease.arctic.spark.source.TestUnKeyedTableDataFrameAPI;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import java.io.IOException;
import java.util.Map;

/**
 * Test suite for the arctic-spark library. all tests share same ams and hms and spark session
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
    TestImpalaParquet.class,
    TestKeyedHiveInsertOverwriteDynamic.class,
    TestKeyedHiveInsertOverwriteStatic.class,
    TestUnkeyedHiveInsertOverwriteDynamic.class,
    TestUnkeyedHiveInsertOverwriteStatic.class,
    TestCreateTableDDL.class,
    TestMigrateHiveTable.class,
    TestHiveTableMergeOnRead.class,
    TestAlterKeyedTable.class,
    TestKeyedTableDDL.class,
    TestKeyedTableDml.class,
    TestKeyedTableDML.class,
    TestKeyedTableDMLInsertOverwriteDynamic.class,
    TestKeyedTableDMLInsertOverwriteStatic.class,
    TestInsertOverwritePartitionTransform.class,
    TestUnKeyedTableDDL.class,
    TestMigrateNonHiveTable.class,
    TestOptimizeWrite.class,
    TestUnKeyedTableDML.class,
    TestUnkeyedTableDml.class,
    TestKeyedTableDataFrameAPI.class,
    TestUnKeyedTableDataFrameAPI.class,
    TestCreateKeyedTableAsSelect.class,
    com.netease.arctic.spark.hive.TestKeyedTableDml.class,
    TestDropPartitions.class,
    TestTruncate.class,
    TestMergeInto.class,
    TestHiveTableDropPartitions.class,
    TestHiveTableTruncate.class,
    TestKeyedTableMergeInto.class,
    TestUnKeyedTableMergeInto.class,
    TestUpsert.class
})
public class ArcticSparkCatalogTestGroup {

  @BeforeClass
  public static void suiteSetup() throws IOException, ClassNotFoundException {
    Map<String, String> configs = Maps.newHashMap();
    Map<String, String> arcticConfigs = SparkTestContext.setUpTestDirAndArctic();
    Map<String, String> hiveConfigs = SparkTestContext.setUpHMS();
    configs.putAll(arcticConfigs);
    configs.putAll(hiveConfigs);
    SparkTestContext.setUpSparkSession(configs);
  }

  @AfterClass
  public static void suiteTeardown() {
    SparkTestContext.cleanUpAms();
    SparkTestContext.cleanUpHive();
    SparkTestContext.cleanUpSparkSession();
  }
}
