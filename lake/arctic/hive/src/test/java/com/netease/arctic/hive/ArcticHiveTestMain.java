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

package com.netease.arctic.hive;

import com.netease.arctic.hive.catalog.HiveBasedCatalogTest;
import com.netease.arctic.hive.catalog.HiveCatalogLoaderTest;
import com.netease.arctic.hive.catalog.IcebergHiveCatalogTest;
import com.netease.arctic.hive.catalog.MixedHiveCatalogTest;
import com.netease.arctic.hive.io.KeyedTimeStampTest;
import com.netease.arctic.hive.io.TestAdaptHiveReader;
import com.netease.arctic.hive.io.TestAdaptHiveWriter;
import com.netease.arctic.hive.io.TestImpalaParquet;
import com.netease.arctic.hive.op.AutoSyncHiveTest;
import com.netease.arctic.hive.op.TestHiveSchemaUpdate;
import com.netease.arctic.hive.op.TestOverwriteFiles;
import com.netease.arctic.hive.op.TestRewriteFiles;
import com.netease.arctic.hive.op.TestRewritePartitions;
import com.netease.arctic.hive.utils.CompatibleHivePropertyUtilTest;
import com.netease.arctic.hive.utils.HiveMetaSynchronizerTest;
import com.netease.arctic.hive.utils.HiveSchemaUtilTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    TestImpalaParquet.class,
    TestRewritePartitions.class,
    TestOverwriteFiles.class,
    TestRewriteFiles.class,
    TestHiveSchemaUpdate.class,
    HiveMetaSynchronizerTest.class,
    TestAdaptHiveWriter.class,
    AutoSyncHiveTest.class,
    HiveBasedCatalogTest.class,
    HiveCatalogLoaderTest.class,
    IcebergHiveCatalogTest.class,
    MixedHiveCatalogTest.class,
    CompatibleHivePropertyUtilTest.class,
    HiveSchemaUtilTest.class,
    TestAdaptHiveReader.class,
    KeyedTimeStampTest.class
})
public class ArcticHiveTestMain {

  @BeforeClass
  public static void setup() throws Exception {
    System.out.println("================== setup arctic hiveMetastore env ==================");
    HiveTableTestBase.startMetastore();
    System.out.println("================== setup arctic hiveMetastore env completed ==================");

  }

  @AfterClass
  public static void cleanDown() {
    System.out.println("================== clean arctic hiveMetastore env ===================");
    HiveTableTestBase.stopMetastore();
    System.out.println("================== clean arctic hiveMetastore env completed ===================");
  }
}
