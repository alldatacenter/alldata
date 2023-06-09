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
package org.apache.drill.exec.store.store;

import org.apache.drill.categories.SqlTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterTest;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.drill.exec.util.StoragePluginTestUtils.CP_PLUGIN_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

@Category(SqlTest.class)
public class TestDisabledPlugin extends ClusterTest {
  private static StoragePluginRegistry pluginRegistry;
  private static FileSystemConfig pluginConfig;

  @BeforeClass
  public static void setup() throws Exception {
    startCluster(ClusterFixture.builder(dirTestWatcher));
    pluginRegistry = cluster.drillbit().getContext().getStorage();
    pluginConfig = (FileSystemConfig) pluginRegistry.getPlugin(CP_PLUGIN_NAME).getConfig();
    pluginConfig.setEnabled(false);
    pluginRegistry.put(CP_PLUGIN_NAME, pluginConfig);
  }

  @AfterClass
  public static void restore() throws Exception {
    pluginConfig.setEnabled(true);
    pluginRegistry.put(CP_PLUGIN_NAME, pluginConfig);
  }

  @Test
  public void testDisabledPluginQuery() throws Exception {
    try {
      run("SELECT * FROM cp.`employee.json` LIMIT 10");
      fail("Query should have failed!");
    } catch (UserRemoteException e) {
      assertEquals(UserBitShared.DrillPBError.ErrorType.VALIDATION, e.getErrorType());
      assertTrue("Incorrect error message",
        e.getMessage().contains("VALIDATION ERROR: Schema"));
    }
  }

  @Test
  public void testUseStatement() throws Exception {
    try {
      run("use cp");
      fail("Query should have failed!");
    } catch (UserRemoteException e) {
      assertEquals(UserBitShared.DrillPBError.ErrorType.VALIDATION, e.getErrorType());
      assertTrue("Incorrect error message",
        e.getMessage().contains("VALIDATION ERROR: Schema"));
    }
  }

  @Test
  public void testDisabledPluginGet() throws Exception {
    assertNull(pluginRegistry.getPlugin(CP_PLUGIN_NAME));
  }
}
