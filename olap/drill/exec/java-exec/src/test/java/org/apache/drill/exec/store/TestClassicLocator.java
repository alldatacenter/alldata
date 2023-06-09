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
package org.apache.drill.exec.store;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.Set;

import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.logical.AbstractSecuredStoragePluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.sys.SystemTablePluginConfig;
import org.apache.drill.exec.util.StoragePluginTestUtils;
import org.apache.drill.test.OperatorFixture;
import org.junit.Test;

public class TestClassicLocator extends BasePluginRegistryTest {

  @Test
  public void testClassList() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);

    // This test uses a cached class path. If you change the plugins
    // on the class path, and run this test without a full build, it may
    // fail. Uncomment the following line to (slowly) rebuild the class
    // path scan on each run.
    // builder.configBuilder().put(ClassPathScanner.IMPLEMENTATIONS_SCAN_CACHE, false);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      ConnectorLocator locator = new ClassicConnectorLocator(context);
      locator.init();
      Set<Class<? extends StoragePluginConfig>> result = locator.configClasses();

      // Sanity check of known good class

      assertTrue(result.contains(FileSystemConfig.class));

      // System plugins do not appear
      assertFalse(result.contains(SystemTablePluginConfig.class));

      // Abstract classes do not appear
      assertFalse(result.contains(StoragePluginConfig.class));
      assertFalse(result.contains(AbstractSecuredStoragePluginConfig.class));

      // The private plugin class does not appear
      assertFalse(result.contains(StoragePluginFixtureConfig.class));

      // No intrinsic plugins
      assertNull(locator.get("dfs"));
      assertNull(locator.intrinsicPlugins());

      // Storable
      assertTrue(locator.storable());

      // Lookup by config
      assertSame(FileSystemPlugin.class, locator.connectorClassFor(FileSystemConfig.class));
      assertNull(locator.connectorClassFor(StoragePluginFixtureConfig.class));

      // No-op
      locator.close();
    }
  }

  @Test
  public void testPrivateConnector() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);

    // See note above.
    // builder.configBuilder().put(ClassPathScanner.IMPLEMENTATIONS_SCAN_CACHE, false);
    builder.configBuilder().put(ExecConstants.PRIVATE_CONNECTORS,
        Collections.singletonList(StoragePluginFixture.class.getName()));
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      ConnectorLocator locator = new ClassicConnectorLocator(context);
      locator.init();
      Set<Class<? extends StoragePluginConfig>> result = locator.configClasses();

      // Now the private connector does appear.
      assertTrue(result.contains(StoragePluginFixtureConfig.class));

      // Create a plugin instance given a config

      StoragePluginFixtureConfig config = new StoragePluginFixtureConfig("some-mode");
      StoragePlugin plugin = locator.create("myplugin", config);
      assertNotNull(plugin);
      assertTrue(plugin instanceof StoragePluginFixture);

      // No-op
      locator.close();
    }
  }

  @Test
  public void testBootstrap() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      ConnectorLocator locator = new ClassicConnectorLocator(context);
      locator.init();
      StoragePlugins plugins = locator.bootstrapPlugins();

      // Sanity test. Change this if the bootstrap file changes.
      // No need to test contents; here we assume serialization works.
      // See FormatPluginSerDeTest

      assertNotNull(plugins.getConfig("dfs"));
      assertNotNull(plugins.getConfig("s3"));
      assertNotNull(plugins.getConfig(StoragePluginTestUtils.CP_PLUGIN_NAME));

      // No-op
      locator.close();
    }
  }
}
