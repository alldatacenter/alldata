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

import java.util.Collection;

import org.apache.drill.exec.store.sys.SystemTablePlugin;
import org.apache.drill.exec.store.sys.SystemTablePluginConfig;
import org.apache.drill.test.OperatorFixture;
import org.junit.Test;

public class TestSystemPluginLocator extends BasePluginRegistryTest {

  // Uses a null Drillbit context. This is benign for the current
  // set of system plugins. If this test fails, check if a system
  // plugin uses the Drillbit context. Eventually, we'll use a different
  // context for plugins. But, if the crash happens sooner, change this
  // to a cluster test so a DrillbitContext is available.
  @Test
  public void testSystemLocator() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      ConnectorLocator locator = new SystemPluginLocator(context);
      locator.init();

      // No bootstrap or upgrade configs
      assertNull(locator.bootstrapPlugins());
      assertNull(locator.updatedPlugins());

       // No user-creatable configs
      assertNull(locator.configClasses());

      // Test intrinsic plugins
      assertNotNull(locator.get("sys"));

      Collection<StoragePlugin> intrinsics = locator.intrinsicPlugins();
      assertNotNull(intrinsics);
      assertTrue(intrinsics.contains(locator.get("sys")));

      // System plugins are not storable
      assertFalse(locator.storable());

      // Map from config to impl class
      assertSame(SystemTablePlugin.class, locator.connectorClassFor(SystemTablePluginConfig.class));

      // No-op
      locator.close();
    }
  }
}
