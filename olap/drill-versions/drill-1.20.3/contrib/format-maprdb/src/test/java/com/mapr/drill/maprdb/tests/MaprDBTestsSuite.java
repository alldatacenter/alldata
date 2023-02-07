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
package com.mapr.drill.maprdb.tests;

import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.hbase.HBaseTestsSuite;
import org.apache.drill.test.BaseTest;
import org.apache.hadoop.conf.Configuration;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import com.mapr.db.Admin;
import com.mapr.db.MapRDB;
import com.mapr.drill.maprdb.tests.binary.TestMapRDBFilterPushDown;
import com.mapr.drill.maprdb.tests.binary.TestMapRDBSimple;
import com.mapr.drill.maprdb.tests.json.TestScanRanges;
import com.mapr.drill.maprdb.tests.json.TestSimpleJson;

@RunWith(Suite.class)
@SuiteClasses({
  TestMapRDBSimple.class,
  TestMapRDBFilterPushDown.class,
  TestSimpleJson.class,
  TestScanRanges.class
})
public class MaprDBTestsSuite extends BaseTest {
  public static final int INDEX_FLUSH_TIMEOUT = 60000;

  private static final boolean IS_DEBUG = ManagementFactory.getRuntimeMXBean().getInputArguments().toString().indexOf("-agentlib:jdwp") > 0;

  private static volatile AtomicInteger initCount = new AtomicInteger(0);
  private static volatile Configuration conf;

  private static Admin admin;

  @BeforeClass
  public static void setupTests() throws Exception {
    if (initCount.get() == 0) {
      synchronized (MaprDBTestsSuite.class) {
        if (initCount.get() == 0) {
          HBaseTestsSuite.configure(false /*manageHBaseCluster*/, true /*createTables*/);
          HBaseTestsSuite.initCluster();

          // Sleep to allow table data to be flushed to tables.
          // Without this, the row count stats to return 0,
          // causing the planner to reject optimized plans.
          Thread.sleep(5000);

          admin = MapRDB.newAdmin();
          conf = HBaseTestsSuite.getConf();
          initCount.incrementAndGet(); // must increment while inside the synchronized block
          return;
        }
      }
    }
    initCount.incrementAndGet();
    return;
  }

  @AfterClass
  public static void cleanupTests() throws Exception {
    synchronized (MaprDBTestsSuite.class) {
      if (initCount.decrementAndGet() == 0) {
        HBaseTestsSuite.tearDownCluster();
        admin.close();
      }
    }
  }

  private static volatile boolean pluginsUpdated;

  public static Configuration createPluginAndGetConf(DrillbitContext ctx) throws Exception {
    if (!pluginsUpdated) {
      synchronized (MaprDBTestsSuite.class) {
        if (!pluginsUpdated) {
          StoragePluginRegistry pluginRegistry = ctx.getStorage();

          String pluginConfStr = "{" +
              "  \"type\": \"file\"," +
              "  \"enabled\": true," +
              "  \"connection\": \"maprfs:///\"," +
              "  \"workspaces\": {" +
              "    \"default\": {" +
              "      \"location\": \"/tmp\"," +
              "      \"writable\": false," +
              "      \"defaultInputFormat\": \"maprdb\"" +
              "    }," +
              "    \"tmp\": {" +
              "      \"location\": \"/tmp\"," +
              "      \"writable\": true," +
              "      \"defaultInputFormat\": \"parquet\"" +
              "    }," +
              "    \"root\": {" +
              "      \"location\": \"/\"," +
              "      \"writable\": false," +
              "      \"defaultInputFormat\": \"maprdb\"" +
              "    }" +
              "  }," +
              "  \"formats\": {" +
              "   \"maprdb\": {" +
              "      \"type\": \"maprdb\"," +
              "      \"allTextMode\": false," +
              "      \"readAllNumbersAsDouble\": false," +
              "      \"enablePushdown\": true" +
              "    }," +
              "   \"parquet\": {" +
              "      \"type\": \"parquet\"" +
              "    }," +
              "   \"streams\": {" +
              "      \"type\": \"streams\"" +
              "    }" +
              "  }" +
              "}";

          FileSystemConfig pluginConfig = ctx.getLpPersistence().getMapper().readValue(pluginConfStr, FileSystemConfig.class);
          // create the plugin with "hbase" name so that we can run HBase unit tests against them
          pluginRegistry.put("hbase", pluginConfig);
        }
      }
    }
    return conf;
  }

  public static boolean isDebug() {
    return IS_DEBUG;
  }

  public static Admin getAdmin() {
    return admin;
  }

  public static InputStream getJsonStream(String resourceName) {
    return MaprDBTestsSuite.class.getResourceAsStream(resourceName);
  }

}
