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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Set;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.store.PluginHandle.PluginType;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.drill.test.OperatorFixture;
import org.junit.Test;

public class TestPluginsMap extends BasePluginRegistryTest {

  private ConnectorHandle fixtureConnector() {
    return ConnectorHandle.configuredConnector(new LocatorFixture(),
        StoragePluginFixtureConfig.class);
  }

  @Test
  public void testEntry() {

    ConnectorHandle connector = fixtureConnector();
    assertSame(StoragePluginFixture.class, connector.connectorClass());
    assertSame(StoragePluginFixtureConfig.class, connector.configClass());
    assertFalse(connector.isIntrinsic());

    StoragePluginFixtureConfig config = new StoragePluginFixtureConfig("ok1");
    PluginHandle entry = new PluginHandle("p1", config, connector);
    assertEquals("p1", entry.name());
    assertSame(config, entry.config());
    assertFalse(entry.hasInstance());

    // Create the plugin instance

    StoragePlugin plugin = entry.plugin();
    assertNotNull(plugin);
    assertTrue(plugin instanceof StoragePluginFixture);
    StoragePluginFixture fixture = (StoragePluginFixture) plugin;
    assertTrue(entry.hasInstance());
    assertEquals(entry.name(), fixture.getName());
    assertSame(entry.config(), fixture.getConfig());
    assertEquals(0, fixture.closeCount());

    // Close the plugin

    entry.close();
    assertEquals(1, fixture.closeCount());
    assertNotNull(plugin);

    // OK to close twice

    entry.close();
    assertEquals(1, fixture.closeCount());
    assertNotNull(plugin);
  }

  @Test
  public void testEntryFailures() {
    ConnectorHandle connector = fixtureConnector();

    // Failure on constructor
    StoragePluginFixtureConfig config1 = new StoragePluginFixtureConfig("crash-ctor");
    PluginHandle entry1 = new PluginHandle("p1", config1, connector);
    try {
      entry1.plugin();
      fail();
    } catch (UserException e) {
      // Expected
    }
    assertFalse(entry1.hasInstance());
    entry1.close(); // No-op

    // Failure on start
    StoragePluginFixtureConfig config2 = new StoragePluginFixtureConfig("crash-start");
    PluginHandle entry2 = new PluginHandle("p2", config2, connector);
    try {
      entry2.plugin();
      fail();
    } catch (UserException e) {
      // Expected
    }
    assertFalse(entry2.hasInstance());
    entry2.close(); // No-op

    // Failure on close
    StoragePluginFixtureConfig config3 = new StoragePluginFixtureConfig("crash-close");
    PluginHandle entry3 = new PluginHandle("p3", config3, connector);
    StoragePlugin plugin3 = entry3.plugin();

    // Fails silently
    entry3.close();
    StoragePluginFixture fixture3 = (StoragePluginFixture) plugin3;
    assertEquals(1, fixture3.closeCount());
  }

  @Test
  public void testBasics() throws PluginException {
    ConnectorHandle connector = fixtureConnector();

    StoragePluginFixtureConfig config1a = new StoragePluginFixtureConfig("ok1");
    StoragePluginFixtureConfig config1b = new StoragePluginFixtureConfig("ok1");
    StoragePluginFixtureConfig config2 = new StoragePluginFixtureConfig("ok2");

    // Sanity check that compare-by-value works for configs
    assertTrue(config1a.equals(config1b));
    assertFalse(config1a.equals(config2));

    // Get with empty map
    StoragePluginMap map = new StoragePluginMap();
    assertNull(map.get("plugin1"));
    assertNull(map.get(config1a));

    PluginHandle entry1 = new PluginHandle("plugin1", config1a, connector);
    assertNull(map.put(entry1));
    assertSame(entry1, map.get(entry1.name()));
    assertSame(entry1, map.get(entry1.config()));
    assertEquals(1, map.configs().size());

    // Put twice, no effect
    assertNull(map.put(entry1));
    assertEquals(1, map.configs().size());

    // Config lookup is by value
    assertSame(entry1, map.get(config1b));

    // Add second entry
    PluginHandle entry2 = new PluginHandle("plugin2", config2, connector);
    assertNull(map.put(entry2));

    // Accessors
    Set<String> names = map.getNames();
    assertEquals(2, names.size());
    assertTrue(names.contains(entry1.name()));
    assertTrue(names.contains(entry2.name()));

    Collection<PluginHandle> plugins = map.plugins();
    assertEquals(2, plugins.size());
    assertTrue(plugins.contains(entry1));
    assertTrue(plugins.contains(entry2));

    Set<StoragePluginConfig> configs = map.configs();
    assertEquals(2, configs.size());
    assertTrue(configs.contains(entry1.config()));
    assertTrue(configs.contains(entry2.config()));

    map.remove(entry1.name());
    assertNull(map.get(entry1.name()));
    assertNull(map.get(entry1.config()));

    map.remove(entry2.name());

    assertTrue(map.getNames().isEmpty());
    assertTrue(map.plugins().isEmpty());
    assertTrue(map.configs().isEmpty());

    map.close();
  }

  @Test
  public void testRemoveByName() throws PluginException {
    ConnectorHandle connector = fixtureConnector();

    StoragePluginMap map = new StoragePluginMap();
    StoragePluginFixtureConfig config1 = new StoragePluginFixtureConfig("ok1");
    PluginHandle entry1 = new PluginHandle("plugin1", config1, connector);
    map.put(entry1);
    StoragePluginFixture fixture1 = (StoragePluginFixture) entry1.plugin();
    assertEquals(0, fixture1.closeCount());

    // Missing entry
    assertNull(map.remove("foo"));

    // Entry is present: closed by caller
    assertSame(entry1, map.remove("plugin1"));
    assertEquals(0, fixture1.closeCount());
    // Simulate caller
    fixture1.close();
    assertEquals(1, fixture1.closeCount());

    map.close();
    assertEquals(1, fixture1.closeCount());
  }

  @Test
  public void testSafePutRemove() throws PluginException {
    ConnectorHandle connector = fixtureConnector();

    StoragePluginMap map = new StoragePluginMap();
    StoragePluginFixtureConfig config1 = new StoragePluginFixtureConfig("ok1");
    PluginHandle entry1 = new PluginHandle("plugin1", config1, connector);
    map.put(entry1);

    // Replacing returns original
    StoragePluginFixtureConfig config2 = new StoragePluginFixtureConfig("ok2");
    PluginHandle entry2 = new PluginHandle("plugin1", config2, connector);
    assertSame(entry1, map.put(entry2));
    assertSame(entry2, map.get(entry1.name()));

    // Put if absent
    StoragePluginFixtureConfig config3 = new StoragePluginFixtureConfig("ok3");
    PluginHandle entry3 = new PluginHandle("plugin2", config3, connector);
    assertSame(entry3, map.putIfAbsent(entry3));

    StoragePluginFixtureConfig config4 = new StoragePluginFixtureConfig("ok4");
    PluginHandle entry4 = new PluginHandle("plugin2", config4, connector);
    assertSame(entry3, map.putIfAbsent(entry4));

    // Remove
    assertSame(entry2, map.remove(entry2.name())); // currently in map
    assertSame(entry3, map.remove(entry3.name()));
    assertNull(map.remove(entry4.name()));

    assertTrue(map.getNames().isEmpty());
    assertTrue(map.plugins().isEmpty());
    assertTrue(map.configs().isEmpty());

    map.close();
  }

  @Test
  public void testReplace() throws PluginException {
    ConnectorHandle connector = fixtureConnector();

    StoragePluginMap map = new StoragePluginMap();
    StoragePluginFixtureConfig config1 = new StoragePluginFixtureConfig("ok1");
    PluginHandle entry1 = new PluginHandle("plugin1", config1, connector);
    map.put(entry1);

    // Replace existing item
    StoragePluginFixtureConfig config2 = new StoragePluginFixtureConfig("ok2");
    PluginHandle entry2 = new PluginHandle("plugin1", config2, connector);
    assertTrue(map.replace(entry1, entry2));

    // Replace non-existing entry
    StoragePluginFixtureConfig config3 = new StoragePluginFixtureConfig("ok3");
    PluginHandle entry3 = new PluginHandle("plugin1", config3, connector);
    assertFalse(map.replace(entry1, entry3));
    assertSame(entry2, map.get(entry1.name()));
    assertNull(map.get(entry1.config()));
    assertSame(entry2, map.get(entry2.config()));
    assertNull(map.get(entry3.config()));

    assertEquals(1, map.getNames().size());
    assertEquals(1, map.plugins().size());
    assertEquals(1, map.configs().size());
    map.close();
  }

  @Test
  public void testSafeRemove() throws PluginException {
    ConnectorHandle connector = fixtureConnector();

    StoragePluginMap map = new StoragePluginMap();
    StoragePluginFixtureConfig config1 = new StoragePluginFixtureConfig("ok1");
    PluginHandle entry1 = new PluginHandle("plugin1", config1, connector);
    map.put(entry1);

    // Wrong name, config OK
    assertNull(map.remove("plugin2", config1));

    // Name OK, wrong config
    StoragePluginFixtureConfig config2 = new StoragePluginFixtureConfig("ok2");
    assertNull(map.remove("plugin1", config2));
    assertEquals(1, map.getNames().size());

    // Name and config match, removed
    StoragePluginFixtureConfig config3 = new StoragePluginFixtureConfig("ok1");
    PluginHandle ret = map.remove("plugin1", config3);
    assertSame(entry1, ret);

    assertTrue(map.getNames().isEmpty());

    map.close();
  }

  public void testIntrinsic() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      ConnectorLocator locator = new SystemPluginLocator(context);
      locator.init();

      Collection<StoragePlugin> sysPlugins = locator.intrinsicPlugins();
      assertTrue(!sysPlugins.isEmpty());
      StoragePlugin sysPlugin = sysPlugins.iterator().next();
      ConnectorHandle connector = ConnectorHandle.intrinsicConnector(locator, sysPlugin);
      assertTrue(connector.isIntrinsic());

      StoragePluginMap map = new StoragePluginMap();
      PluginHandle sysEntry = new PluginHandle(sysPlugin, connector, PluginType.INTRINSIC);
      assertNull(map.put(sysEntry));
      assertSame(sysEntry, map.get(sysPlugin.getName()));

      // Second request to put the same (system) plugin is ignored
      assertNull(map.put(sysEntry));

      // Attempt to overwrite a system plugin is forcefully denied.
      // Users can make this mistake, so a UserException is thrown
      StoragePluginFixtureConfig config1 = new StoragePluginFixtureConfig("ok1");
      PluginHandle entry1 = new PluginHandle(sysPlugin.getName(), config1, connector);
      try {
        map.put(entry1);
        fail();
      } catch (UserException e) {
        // Expected
      }
      assertSame(sysEntry, map.get(sysPlugin.getName()));

      // putIfAbsent does not replace an existing plugin
      assertSame(sysEntry, map.putIfAbsent(entry1));
      assertSame(sysEntry, map.get(sysPlugin.getName()));

      // Replace fails. Caller should have checked if the entry
      // is intrinsic.
      try {
        map.replace(sysEntry, entry1);
        fail();
      } catch (IllegalArgumentException e) {
        // Expected
      }
      assertSame(sysEntry, map.get(sysPlugin.getName()));

      // Remove by entry fails for the same reasons as above.
      try {
        map.remove(sysEntry.name());
        fail();
      } catch (PluginException e) {
        // Expected
      }
      assertSame(sysEntry, map.get(sysPlugin.getName()));

      // Request to remove by name is ignored
      // Caller can't be expected to know the meaning of the name
      // Request to remove an intrinsic plugin by name is treated the
      // same as a request to remove a non-existent plugin
      assertNull(map.remove(sysPlugin.getName()));
      assertSame(sysEntry, map.get(sysPlugin.getName()));

      // Request to remove by name and config fails
      // as above.

      assertNull(map.remove(sysPlugin.getName(), sysPlugin.getConfig()));
      assertSame(sysEntry, map.get(sysPlugin.getName()));

      // Close does close intrinsic plugins, but no way to check
      // it without elaborate mocking
      map.close();
    }
  }

  public void testClose() throws PluginException {
    ConnectorHandle connector = fixtureConnector();

    StoragePluginMap map = new StoragePluginMap();
    StoragePluginFixtureConfig config1 = new StoragePluginFixtureConfig("ok1");
    PluginHandle entry1 = new PluginHandle("plugin1", config1, connector);
    map.put(entry1);

    // Create the plugin instance
    StoragePlugin plugin1 = entry1.plugin();
    assertNotNull(plugin1);
    assertTrue(entry1.hasInstance());

    // Second, no instance
    StoragePluginFixtureConfig config2 = new StoragePluginFixtureConfig("ok2");
    PluginHandle entry2 = new PluginHandle("plugin2", config2, connector);
    map.put(entry2);
    assertFalse(entry2.hasInstance());

    // Close the map
    map.close();

    // Everything closed
    assertFalse(entry1.hasInstance());
    assertFalse(entry2.hasInstance());
    StoragePluginFixture fixture1 = (StoragePluginFixture) plugin1;
    assertEquals(1, fixture1.closeCount());
  }
}
