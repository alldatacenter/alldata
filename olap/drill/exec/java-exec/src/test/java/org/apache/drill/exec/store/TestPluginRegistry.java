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

import static org.apache.drill.exec.util.StoragePluginTestUtils.CP_PLUGIN_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.store.BasePluginRegistryTest.StoragePluginFixture;
import org.apache.drill.exec.store.BasePluginRegistryTest.StoragePluginFixtureConfig;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginFilter;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.FileSystemPlugin;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin.TextFormatConfig;
import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;

/**
 * Tests the storage plugin registry. Plugins are (at present)
 * tightly coupled to the Drillbit context so we need to start
 * a Drillbit per tests to ensure each test works from a clean,
 * known registry.
 * <p>
 * Tests are somewhat large because each needs a running
 * Drillbit.
 * This is several big tests because of the setup cost of
 * starting the Drillbits in the needed config.
 */
public class TestPluginRegistry extends BaseTest {

  @ClassRule
  public static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  private static final String SYS_PLUGIN_NAME = "sys";
  private static final String S3_PLUGIN_NAME = "s3";

  // Mixed-case name used to verify that names are forced to lower case.
  private static final String MY_PLUGIN_NAME = "myPlugin";

  // Lower-case form after insertion into the registry.
  private static final String MY_PLUGIN_KEY = MY_PLUGIN_NAME.toLowerCase();

  @After
  public void cleanup() throws Exception {
    FileUtils.cleanDirectory(dirTestWatcher.getStoreDir());
  }

  private FileSystemConfig myConfig1() {
    FileSystemConfig config = new FileSystemConfig("myConn",
        new HashMap<>(), new HashMap<>(), new HashMap<>(),
        PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    config.setEnabled(true);
    return config;
  }

  private FileSystemConfig myConfig2() {
    Map<String, String> props = new HashMap<>();
    props.put("foo", "bar");
    FileSystemConfig config = new FileSystemConfig("myConn",
        props, new HashMap<>(), new HashMap<>(),
        PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    config.setEnabled(true);
    return config;
  }

  @Test
  public void testLifecycle() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build()) {
      StoragePluginRegistry registry = cluster.storageRegistry();

      // Bootstrap file loaded.
      assertNotNull(registry.getPlugin(CP_PLUGIN_NAME)); // Normal
      assertNotNull(registry.getPlugin(SYS_PLUGIN_NAME)); // System
      assertNull(registry.getStoredConfig(SYS_PLUGIN_NAME)); // Not editable

      assertNull(registry.getPlugin("bogus"));

      // Enabled plugins
      Map<String, StoragePluginConfig> configMap = registry.enabledConfigs();
      assertTrue(configMap.containsKey(CP_PLUGIN_NAME));
      assertFalse(configMap.containsKey(S3_PLUGIN_NAME)); // Disabled, but still appears
      assertFalse(configMap.containsKey(SYS_PLUGIN_NAME));

      assertNotNull(registry.getDefinedConfig(CP_PLUGIN_NAME));
      assertNull(registry.getDefinedConfig(S3_PLUGIN_NAME));
      assertNotNull(registry.getDefinedConfig(SYS_PLUGIN_NAME));

      // All stored plugins, including disabled
      configMap = registry.storedConfigs();
      assertTrue(configMap.containsKey(CP_PLUGIN_NAME));
      assertTrue(configMap.containsKey(S3_PLUGIN_NAME)); // Disabled, but still appears
      assertNotNull(configMap.get(S3_PLUGIN_NAME));
      assertSame(registry.getStoredConfig(S3_PLUGIN_NAME), configMap.get(S3_PLUGIN_NAME));
      assertFalse(configMap.containsKey(SYS_PLUGIN_NAME));
      int bootstrapCount = configMap.size();

      // Enabled only
      configMap = registry.storedConfigs(PluginFilter.ENABLED);
      assertTrue(configMap.containsKey(CP_PLUGIN_NAME));
      assertFalse(configMap.containsKey(S3_PLUGIN_NAME));

      // Disabled only
      configMap = registry.storedConfigs(PluginFilter.DISABLED);
      assertFalse(configMap.containsKey(CP_PLUGIN_NAME));
      assertTrue(configMap.containsKey(S3_PLUGIN_NAME));

      // Create a new plugin
      FileSystemConfig pConfig1 = myConfig1();
      registry.put(MY_PLUGIN_NAME, pConfig1);
      StoragePlugin plugin1 = registry.getPlugin(MY_PLUGIN_NAME);
      assertNotNull(plugin1);
      assertSame(plugin1, registry.getPluginByConfig(pConfig1));
      configMap = registry.storedConfigs();

      // Names converted to lowercase in persistent storage
      assertTrue(configMap.containsKey(MY_PLUGIN_KEY));
      assertEquals(bootstrapCount + 1, configMap.size());

      // Names are case-insensitive
      assertSame(plugin1, registry.getPlugin(MY_PLUGIN_KEY));
      assertSame(plugin1, registry.getPlugin(MY_PLUGIN_NAME.toUpperCase()));

      // Update the plugin
      FileSystemConfig pConfig2 = myConfig2();
      registry.put(MY_PLUGIN_NAME, pConfig2);
      StoragePlugin plugin2 = registry.getPlugin(MY_PLUGIN_NAME);
      assertNotSame(plugin1, plugin2);
      assertTrue(plugin2 instanceof FileSystemPlugin);
      FileSystemPlugin fsStorage = (FileSystemPlugin) plugin2;
      assertSame(pConfig2, fsStorage.getConfig());
      assertSame(plugin2, registry.getPluginByConfig(pConfig2));

      // Cannot create/update a plugin with null or blank name

      FileSystemConfig pConfig3 = myConfig1();
      try {
        registry.put(null, pConfig3);
        fail();
      } catch (PluginException e) {
        // Expected
      }
      try {
        registry.put("  ", pConfig3);
        fail();
      } catch (PluginException e) {
        // Expected
      }
    }
  }

  /**
   * Tests the dedicated setEnabled() method to cleanly update the
   * enabled status of a plugin by name.
   */
  @Test
  public void testEnabledState() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build()) {
      StoragePluginRegistry registry = cluster.storageRegistry();

      // Disable an enabled plugin
      StoragePluginConfig config = registry.getStoredConfig(CP_PLUGIN_NAME);
      assertTrue(config.isEnabled());

      // Enable/disable has traditionally been done by changing
      // the plugin outside of the registry, which leads to obvious
      // race conditions.
      // Tests synchronized version.
      registry.setEnabled(CP_PLUGIN_NAME, true);
      StoragePluginConfig savedConfig = registry.getStoredConfig(CP_PLUGIN_NAME);
      assertEquals(config, savedConfig);
      assertTrue(savedConfig.isEnabled());

      registry.setEnabled(CP_PLUGIN_NAME, false);
      savedConfig = registry.getStoredConfig(CP_PLUGIN_NAME);
      assertEquals(config, savedConfig);
      assertFalse(savedConfig.isEnabled());

      // OK to disable twice
      registry.setEnabled(CP_PLUGIN_NAME, false);
      savedConfig = registry.getStoredConfig(CP_PLUGIN_NAME);
      assertEquals(config, savedConfig);
      assertFalse(savedConfig.isEnabled());

      // Disabled plugins appear in the stored config map
      Map<String, StoragePluginConfig> configMap = registry.storedConfigs();
      assertTrue(configMap.containsKey(CP_PLUGIN_NAME));
      assertEquals(config, configMap.get(CP_PLUGIN_NAME));

      // Re-enable
      registry.setEnabled(CP_PLUGIN_NAME, true);
      savedConfig = registry.getStoredConfig(CP_PLUGIN_NAME);
      assertEquals(config, savedConfig);
      assertTrue(savedConfig.isEnabled());

      // Error if plugin does not exist
      try {
        registry.setEnabled("foo", true);
        fail();
      } catch (PluginException e) {
        // expected
      }
      try {
        registry.setEnabled("foo", false);
        fail();
      } catch (PluginException e) {
        // expected
      }

      // Error to mess with a system plugins
      try {
        registry.setEnabled(SYS_PLUGIN_NAME, true);
        fail();
      } catch (PluginException e) {
        // expected
      }
      try {
        registry.setEnabled(SYS_PLUGIN_NAME, false);
        fail();
      } catch (PluginException e) {
        // expected
      }
    }
  }

  /**
   * Tests the other way to enable/disabled plugins: make a **COPY** of the
   * config and set the enable/disable status. Note: race conditions happen
   * if a client modifies a stored config. Old code would do that, but the
   * results are undefined. Either use setEnabled(), or make a copy of the
   * config. This case also models where the user edits the config by hand
   * in the Web Console to disable the plugin: the deserialize/serialize
   * steps make the copy.
   */
  @Test
  public void testEnableWithPut() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build()) {
      StoragePluginRegistry registry = cluster.storageRegistry();

      FileSystemConfig pConfig1 = myConfig1();
      registry.put(MY_PLUGIN_NAME, pConfig1);
      assertTrue(registry.getStoredConfig(MY_PLUGIN_NAME).isEnabled());

      // Enable the same plugin using a different, but equal, config.
      // The original config remains in place.
      StoragePluginConfig pConfig2 = registry.copyConfig(MY_PLUGIN_NAME);
      pConfig2.setEnabled(true);
      assertEquals(pConfig1, pConfig2);
      registry.put(MY_PLUGIN_NAME, pConfig2);
      StoragePluginConfig savedConfig = registry.getStoredConfig(MY_PLUGIN_NAME);
      assertEquals(pConfig1, savedConfig);
      assertTrue(savedConfig.isEnabled());

      // Force resolution of the plugin so there is something to cache
      StoragePlugin plugin = registry.getPlugin(MY_PLUGIN_NAME);
      assertNotNull(plugin);

      // Disable an enabled plugin. The old plugin lives in ephemeral
      // store, but is not visible by name. If requested, the
      // registry obtains a new copy from persistent storage.
      StoragePluginConfig pConfig3 = registry.copyConfig(MY_PLUGIN_NAME);
      pConfig3.setEnabled(false);
      registry.put(MY_PLUGIN_NAME, pConfig3);
      savedConfig = registry.getStoredConfig(MY_PLUGIN_NAME);
      assertEquals(pConfig1, savedConfig);
      assertFalse(savedConfig.isEnabled());

      // OK to disable twice
      StoragePluginConfig pConfig4 = registry.copyConfig(MY_PLUGIN_NAME);
      pConfig4.setEnabled(false);
      registry.put(MY_PLUGIN_NAME, pConfig4);
      savedConfig = registry.getStoredConfig(MY_PLUGIN_NAME);
      assertEquals(pConfig1, savedConfig);
      assertFalse(savedConfig.isEnabled());

      // Disabled plugins appear in the stored config map
      Map<String, StoragePluginConfig> configMap = registry.storedConfigs();
      assertTrue(configMap.containsKey(MY_PLUGIN_KEY));
      assertEquals(pConfig3, configMap.get(MY_PLUGIN_KEY));

      // Re-enable, the original plugin instance reappears.
      StoragePluginConfig pConfig5 = registry.copyConfig(MY_PLUGIN_NAME);
      pConfig5.setEnabled(true);
      registry.put(MY_PLUGIN_NAME, pConfig5);
      assertSame(plugin, registry.getPlugin(MY_PLUGIN_NAME));
      assertTrue(plugin.getConfig().isEnabled());
    }
  }

  /**
   * Test the ephemeral cache where plugin instances live after their
   * configs are changed or disabled so that any running queries see
   * the prior plugin config an instance.
   * <p>
   * Note that each call to update a plugin must work with a copy of
   * a config. Results are undefined if a client changes a stored
   * config.
   */
  @Test
  public void testEphemeralLifecycle() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build()) {
      StoragePluginRegistry registry = cluster.storageRegistry();

      // Create a plugin
      FileSystemConfig pConfig1 = myConfig1();
      registry.put(MY_PLUGIN_NAME, pConfig1);
      StoragePlugin plugin1 = registry.getPlugin(MY_PLUGIN_NAME);

      // Update the plugin
      FileSystemConfig pConfig2 = myConfig2();
      registry.put(MY_PLUGIN_NAME, pConfig2);
      StoragePlugin plugin2 = registry.getPlugin(MY_PLUGIN_NAME);
      assertNotSame(plugin1, plugin2);
      assertTrue(plugin2 instanceof FileSystemPlugin);
      FileSystemPlugin fsStorage = (FileSystemPlugin) plugin2;
      assertSame(pConfig2, fsStorage.getConfig());
      assertSame(plugin2, registry.getPluginByConfig(pConfig2));

      // Suppose a query was planned with plugin1 and now starts
      // to execute. Plugin1 has been replaced with plugin2. However
      // the registry moved the old plugin to ephemeral storage where
      // it can still be found by configuration.
      FileSystemConfig pConfig1a = myConfig1();
      StoragePlugin ePlugin1 = registry.getPluginByConfig(pConfig1a);
      assertSame(plugin1, ePlugin1);
      assertNotSame(plugin2, ePlugin1);

      // Change the stored plugin back to the first config.
      FileSystemConfig pConfig1b = myConfig1();
      registry.put(MY_PLUGIN_NAME, pConfig1b);

      // Now, lets suppose thread 3 starts to execute. It sees the original plugin
      assertSame(plugin1, registry.getPlugin(MY_PLUGIN_NAME));

      // But, the ephemeral plugin lives on. Go back to the second
      // config.
      FileSystemConfig pConfig2b = myConfig2();
      registry.put(MY_PLUGIN_NAME, pConfig2b);
      assertSame(plugin2, registry.getPlugin(MY_PLUGIN_NAME));

      // Thread 4, using the first config from planning in thread 3,
      // still sees the first plugin.
      assertSame(plugin1, registry.getPluginByConfig(pConfig1b));

      // Disable
      registry.setEnabled(MY_PLUGIN_NAME, false);
      assertNull(registry.getPlugin(MY_PLUGIN_NAME));

      // Though disabled, a running query will create an ephemeral
      // plugin for the config.
      assertSame(plugin1, registry.getPluginByConfig(pConfig1b));
      assertSame(plugin2, registry.getPluginByConfig(pConfig2b));

      // Enable. We notice the config is in the ephemeral store and
      // so we restore it.
      registry.setEnabled(MY_PLUGIN_NAME, true);
      assertSame(plugin2, registry.getPlugin(MY_PLUGIN_NAME));
      assertSame(plugin2, registry.getPluginByConfig(pConfig2b));
      assertTrue(registry.storedConfigs().containsKey(MY_PLUGIN_KEY));
      assertTrue(registry.enabledConfigs().containsKey(MY_PLUGIN_KEY));

      // Delete the plugin
      registry.remove(MY_PLUGIN_NAME);
      assertNull(registry.getPlugin(MY_PLUGIN_NAME));

      // Again a running query will retrieve the plugin from ephemeral storage
      assertSame(plugin1, registry.getPluginByConfig(pConfig1));
      assertSame(plugin2, registry.getPluginByConfig(pConfig2));

      // Delete again, no-op
      registry.remove(MY_PLUGIN_NAME);

      // The retrieve-from-ephemeral does not kick in if we create
      // a new plugin with the same config but a different name.
      FileSystemConfig pConfig1c = myConfig1();
      pConfig1c.setEnabled(true);
      registry.put("alias", pConfig1c);
      StoragePlugin plugin4 = registry.getPlugin("alias");
      assertNotNull(plugin4);
      assertNotSame(plugin1, plugin4);

      // Delete the second name. The config is the same as one already
      // in ephemeral store, so the second is closed. The first will
      // be returned on subsequent queries.
      registry.remove("alias");
      assertNull(registry.getPlugin("alias"));
      assertSame(plugin1, registry.getPluginByConfig(pConfig1));

      // Try to change a system plugin
      StoragePlugin sysPlugin = registry.getPlugin(SYS_PLUGIN_NAME);
      assertNotNull(sysPlugin);
      FileSystemConfig pConfig3 = myConfig2();
      try {
        registry.put(SYS_PLUGIN_NAME, pConfig3);
        fail();
      } catch (PluginException e) {
        // Expected
      }
      pConfig3.setEnabled(false);
      try {
        registry.put(SYS_PLUGIN_NAME, pConfig3);
        fail();
      } catch (PluginException e) {
        // Expected
      }
      assertSame(sysPlugin, registry.getPlugin(SYS_PLUGIN_NAME));

      // Try to delete a system plugin
      try {
        registry.remove(SYS_PLUGIN_NAME);
        fail();
      } catch (PluginException e) {
        // Expected
      }
    }
  }

  @Test
  public void testEphemeralWithoutInstance() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build()) {
      StoragePluginRegistry registry = cluster.storageRegistry();

      // Create a plugin
      // Since we've created no plugin instance, the configs come from
      // the persistent store, there is no guarantee we get the same
      // instance on each retrieval.
      FileSystemConfig pConfig1 = myConfig1();
      registry.put(MY_PLUGIN_NAME, pConfig1);
      StoragePluginConfig savedConfig = registry.getStoredConfig(MY_PLUGIN_NAME);
      assertEquals(pConfig1, savedConfig);
      assertTrue(savedConfig.isEnabled());
      assertEquals(pConfig1, registry.getDefinedConfig(MY_PLUGIN_NAME));

      // Do not refer to the instance. As a result, no reason to
      // cache the plugin in ephemeral cache.

      // Change config
      FileSystemConfig pConfig1b = myConfig2();
      registry.put(MY_PLUGIN_NAME, pConfig1b);
      assertEquals(pConfig1b, registry.getDefinedConfig(MY_PLUGIN_NAME));

      // Put it back
      FileSystemConfig pConfig1c = myConfig1();
      registry.put(MY_PLUGIN_NAME, pConfig1c);
      assertEquals(pConfig1c, registry.getDefinedConfig(MY_PLUGIN_NAME));
      assertEquals(pConfig1c, registry.getPlugin(MY_PLUGIN_NAME).getConfig());

      // Odd case. Some thread refers to the config while not in
      // the store which forces an instance which later reappears.
      FileSystemConfig pConfig2a = myConfig1();
      registry.put("myplugin2", pConfig2a);

      // Didn't instantiate. Change
      FileSystemConfig pConfig2b = myConfig2();
      registry.put("myplugin2", pConfig2b);

      // Force a resync
      assertEquals(pConfig2b, registry.getDefinedConfig("myplugin2"));

      // Refer by original config. We didn't cache the original config
      // because there was no plugin instance. Must make up a new plugin
      // Which goes into the ephemeral cache.
      FileSystemConfig pConfig2c = myConfig1();
      StoragePlugin plugin2 = registry.getPluginByConfig(pConfig2c);
      assertEquals(pConfig2c, plugin2.getConfig());

      // Put the original config into the persistent store and local cache.
      // Should not dredge up the ephemeral version to reuse since that
      // version had an unknown name.
      // It is unfortunate that we have to instances with the same config,
      // but different names. But, since the name is immutable, and not
      // known above, the two-instance situation is the least bad option.
      FileSystemConfig pConfig2d = myConfig1();
      registry.put("myplugin2", pConfig2d);
      assertEquals(pConfig2d, registry.getPlugin("myplugin2").getConfig());
    }
  }

  @Test
  public void testStoreSync() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher)
        .withBits("bit1", "bit2");

    // We want a non-buffered, local file system store, in a known location
    // so that the two Drillbits will coordinate roughly he same way they
    // will when using the ZK store in distributed mode.
    builder.configBuilder()
      .put(ExecConstants.SYS_STORE_PROVIDER_LOCAL_ENABLE_WRITE, true)
      .put(ExecConstants.SYS_STORE_PROVIDER_LOCAL_PATH,
          dirTestWatcher.getStoreDir().getAbsolutePath());
    try (ClusterFixture cluster = builder.build()) {
      StoragePluginRegistry registry1 = cluster.storageRegistry("bit1");
      StoragePluginRegistry registry2 = cluster.storageRegistry("bit2");

      // Define a plugin in Drillbit 1
      FileSystemConfig pConfig1 = myConfig1();
      registry1.put(MY_PLUGIN_NAME, pConfig1);
      StoragePlugin plugin1 = registry1.getPlugin(MY_PLUGIN_NAME);
      assertNotNull(plugin1);

      // Should appear in Drillbit 2
      assertTrue(registry2.storedConfigs().containsKey(MY_PLUGIN_KEY));
      StoragePlugin plugin2 = registry2.getPlugin(MY_PLUGIN_NAME);
      assertNotNull(plugin2);
      assertEquals(pConfig1, plugin2.getConfig());

      // Change in Drillbit 1
      FileSystemConfig pConfig3 = myConfig2();
      registry1.put(MY_PLUGIN_NAME, pConfig3);
      plugin1 = registry1.getPlugin(MY_PLUGIN_NAME);
      assertEquals(pConfig3, plugin1.getConfig());

      // Change should appear in Drillbit 2
      assertTrue(registry2.storedConfigs().containsValue(pConfig3));
      plugin2 = registry2.getPlugin(MY_PLUGIN_NAME);
      assertNotNull(plugin2);
      assertEquals(pConfig3, plugin1.getConfig());

      // Delete in Drillbit 2
      registry2.remove(MY_PLUGIN_NAME);

      // Should not be available in Drillbit 1
      assertFalse(registry1.storedConfigs().containsKey(MY_PLUGIN_KEY));
      assertNull(registry1.getPlugin(MY_PLUGIN_NAME));
    }
  }

  @Test
  public void testFormatPlugin() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    try (ClusterFixture cluster = builder.build()) {
      StoragePluginRegistry registry = cluster.storageRegistry();

      StoragePluginConfig config = registry.getStoredConfig(CP_PLUGIN_NAME);
      FileSystemConfig fsConfig = (FileSystemConfig) config;
      assertFalse(fsConfig.getFormats().containsKey("bsv"));

      // Add a new format
      TextFormatConfig bsv = new TextFormatConfig(
          null,
          null, // line delimiter
          "!",  // field delimiter
          null,  // quote
          null,  // escape
          null,  // comment
          false, // skip first line
          false  // extract header
          );
      registry.putFormatPlugin(CP_PLUGIN_NAME, "bsv", bsv);

      config = registry.getStoredConfig(CP_PLUGIN_NAME);
      fsConfig = (FileSystemConfig) config;
      assertTrue(fsConfig.getFormats().containsKey("bsv"));
      assertSame(bsv, fsConfig.getFormats().get("bsv"));

      // Remove the format
      registry.putFormatPlugin(CP_PLUGIN_NAME, "bsv", null);
      config = registry.getStoredConfig(CP_PLUGIN_NAME);
      fsConfig = (FileSystemConfig) config;
      assertFalse(fsConfig.getFormats().containsKey("bsv"));

      // Undefined plugin
      try {
        registry.putFormatPlugin("bogus", "bsv", bsv);
        fail();
      } catch (PluginException e) {
        // Expected
      }
      // Try to set a non-FS plugin
      try {
        registry.putFormatPlugin(SYS_PLUGIN_NAME, "bsv", bsv);
        fail();
      } catch (PluginException e) {
        // Expected
      }
    }
  }

  /**
   * Test to illustrate problems discussed in DRILL-7624
   */
  @Test
  public void testBadPlugin() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    builder.configBuilder().put(ExecConstants.PRIVATE_CONNECTORS,
        Collections.singletonList(StoragePluginFixture.class.getName()));
    try (ClusterFixture cluster = builder.build()) {
      StoragePluginRegistry registry = cluster.storageRegistry();

      // Create a config that causes a crash because the plugin
      // is not created on update.
      StoragePluginFixtureConfig badConfig = new StoragePluginFixtureConfig("crash-ctor");
      badConfig.setEnabled(true);

      // Use the validated put to catch and reject errors at the cost of
      // instantiating the plugin.
      try {
        registry.validatedPut("bad", badConfig);
        fail();
      } catch (PluginException e) {
        // Expected
      }
      assertNull(registry.getStoredConfig("bad"));
      assertFalse(registry.availablePlugins().contains("bad"));

      // Try the same with JSON
      String json = registry.encode(badConfig);
      try {
        registry.putJson("bad", json);
        fail();
      } catch (PluginException e) {
        // Expected
      }
      assertFalse(registry.availablePlugins().contains("bad"));

      // Now, lets pretend the plugin was valid when we did the above,
      // but later the external system failed.
      registry.put("bad", badConfig);
      assertEquals(badConfig, registry.getStoredConfig("bad"));
      assertTrue(registry.availablePlugins().contains("bad"));

      // Ask for the actual plugin. Now will fail.
      try {
        registry.getPlugin("bad");
        fail();
      } catch (UserException e) {
        assertTrue(e.getMessage().contains("bad"));
      }
      assertTrue(registry.availablePlugins().contains("bad"));

      // No plugin created. Will fail the next time also.
      try {
        registry.getPlugin("bad");
        fail();
      } catch (UserException e) {
        // Expected
      }
      assertTrue(registry.availablePlugins().contains("bad"));

      // The iterator used to find planning rules will skip the failed
      // plugin. (That the planner uses all rules is, itself, a bug.)
      int n = registry.availablePlugins().size();
      int count = 0;
      for (@SuppressWarnings("unused") Entry<String, StoragePlugin> entry : registry) {
        count++;
      }
      assertEquals(n - 1, count);

      // Reset to known good state
      registry.remove("bad");

      // Get tricky. Create a good plugin, then replace with
      // a disabled bad one.
      StoragePluginFixtureConfig goodConfig = new StoragePluginFixtureConfig("ok");
      goodConfig.setEnabled(true);
      json = registry.encode(goodConfig);
      registry.putJson("test", json);
      assertTrue(registry.availablePlugins().contains("test"));
      assertEquals(goodConfig, registry.getPlugin("test").getConfig());

      // Replace with a disabled bad plugin
      badConfig = new StoragePluginFixtureConfig("crash-ctor");
      badConfig.setEnabled(false);
      json = registry.encode(badConfig);
      registry.putJson("test", json);
      assertFalse(registry.availablePlugins().contains("test"));
      assertNull(registry.getPlugin("test"));
      assertNotNull(registry.getStoredConfig("test"));
      assertEquals(badConfig, registry.getStoredConfig("test"));

      // Attempt to disable a disabled plugin. Should be OK.
      registry.setEnabled("test", false);

      // But, can't use this as a back door to enable an
      // invalid plugin. (If the problem is due to an external
      // system, fix that system first.)
      try {
        registry.setEnabled("test", true);
        fail();
      } catch (PluginException e) {
        // Expected
      }
      assertFalse(registry.availablePlugins().contains("test"));
    }
  }
}
