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
package org.apache.drill.exec.store.sys;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.drill.categories.FlakyTest;
import org.apache.drill.categories.SlowTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.util.DrillFileUtils;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.TestWithZookeeper;
import org.apache.drill.exec.coord.zk.PathUtils;
import org.apache.drill.exec.coord.zk.ZookeeperClient;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.server.options.PersistedOptionValue;
import org.apache.drill.exec.store.sys.store.ZookeeperPersistentStore;
import org.apache.drill.exec.store.sys.store.provider.LocalPersistentStoreProvider;
import org.apache.drill.exec.store.sys.store.provider.ZookeeperPersistentStoreProvider;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.ClientFixture;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.zookeeper.CreateMode;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;

import static org.junit.Assert.assertTrue;

@Category({SlowTest.class, FlakyTest.class})
public class TestPStoreProviders extends TestWithZookeeper {
  @Rule
  public BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  @Test
  public void verifyLocalStore() throws Exception {
    try(LocalPersistentStoreProvider provider = new LocalPersistentStoreProvider(DrillConfig.create())){
      PStoreTestUtil.test(provider);
    }
  }

  @Test
  public void verifyZkStore() throws Exception {
    try(CuratorFramework curator = createCurator()) {
      curator.start();
      PersistentStoreProvider provider = new ZookeeperPersistentStoreProvider(zkHelper.getConfig(), curator);
      PStoreTestUtil.test(provider);
    }
  }

  /**
   * DRILL-5809
   * Note: If this test breaks you are probably breaking backward and forward compatibility. Verify with the community
   * that breaking compatibility is acceptable and planned for.
   * @throws Exception
   */
  @Test
  public void localBackwardCompatabilityTest() throws Exception {
    localLoadTestHelper("src/test/resources/options/store/local/old/sys.options");
  }

  private void localLoadTestHelper(String propertiesDir) throws Exception {
    File localOptionsResources = new File(propertiesDir);

    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher).
      configProperty(ExecConstants.SYS_STORE_PROVIDER_CLASS, LocalPersistentStoreProvider.class.getCanonicalName()).
      configProperty(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, true);

    File optionsDir = new File(dirTestWatcher.getStoreDir(), "sys.options");
    optionsDir.mkdirs();
    org.apache.commons.io.FileUtils.copyDirectory(localOptionsResources, optionsDir);

    try (ClusterFixture cluster = builder.build();
         ClientFixture client = cluster.clientFixture()) {
      String parquetPushdown = client.queryBuilder().
        sql("SELECT val FROM sys.%s where name='%s'",
          SystemTable.OPTIONS.getTableName(),
          PlannerSettings.PARQUET_ROWGROUP_FILTER_PUSHDOWN_PLANNING_THRESHOLD_KEY).
        singletonString();

      String plannerWidth = client.queryBuilder().
        sql("SELECT val FROM sys.%s where name='%s'",
          SystemTable.OPTIONS.getTableName(),
          ExecConstants.MAX_WIDTH_GLOBAL_KEY).
        singletonString();

      Assert.assertEquals("30000", parquetPushdown);
      Assert.assertEquals("3333", plannerWidth);
    }
  }

  /**
   * DRILL-5809
   * Note: If this test breaks you are probably breaking backward and forward compatibility. Verify with the community
   * that breaking compatibility is acceptable and planned for.
   * @throws Exception
   */
  @Test
  public void zkBackwardCompatabilityTest() throws Exception {
    final String oldName = "myOldOption";

    try (CuratorFramework curator = createCurator()) {
      curator.start();

      PersistentStoreConfig<PersistedOptionValue> storeConfig =
        PersistentStoreConfig.newJacksonBuilder(new ObjectMapper(), PersistedOptionValue.class).name("sys.test").build();

      try (ZookeeperClient zkClient = new ZookeeperClient(curator,
        PathUtils.join("/", storeConfig.getName()), CreateMode.PERSISTENT)) {
        zkClient.start();
        String oldOptionJson = DrillFileUtils.getResourceAsString("/options/old_booleanopt.json");
        zkClient.put(oldName, oldOptionJson.getBytes(), null);
      }

      try (ZookeeperPersistentStoreProvider provider =
        new ZookeeperPersistentStoreProvider(zkHelper.getConfig(), curator)) {
        PersistentStore<PersistedOptionValue> store = provider.getOrCreateStore(storeConfig);
        assertTrue(store instanceof ZookeeperPersistentStore);

        PersistedOptionValue oldOptionValue = ((ZookeeperPersistentStore<PersistedOptionValue>)store).get(oldName, null);
        PersistedOptionValue expectedValue = new PersistedOptionValue("true");

        Assert.assertEquals(expectedValue, oldOptionValue);
      }
    }
  }
}
