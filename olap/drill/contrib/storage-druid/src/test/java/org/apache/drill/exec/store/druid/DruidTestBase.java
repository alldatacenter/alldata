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

package org.apache.drill.exec.store.druid;

import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DruidTestBase extends ClusterTest implements DruidTestConstants {
  private static final Logger logger = LoggerFactory.getLogger(DruidTestBase.class);
  private static StoragePluginRegistry pluginRegistry;

  @BeforeClass
  public static void setUpBeforeClass() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    pluginRegistry = cluster.drillbit().getContext().getStorage();

    DruidTestSuite.initDruid();
    initDruidStoragePlugin();
  }

  private static void initDruidStoragePlugin() throws Exception {
    pluginRegistry
      .put(
        DruidStoragePluginConfig.NAME,
        DruidTestSuite.getDruidStoragePluginConfig());
  }

  @AfterClass
  public static void tearDownDruidTestBase()
      throws StoragePluginRegistry.PluginException {
    if (pluginRegistry != null) {
      pluginRegistry.remove(DruidStoragePluginConfig.NAME);
    } else {
      logger.warn("Plugin Registry was null");
    }
  }
}
