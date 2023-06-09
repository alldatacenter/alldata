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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.test.OperatorFixture;
import org.junit.Test;

/**
 * Tests {@code PluginBootstrapLoader} and its implementation
 * {@code PluginBootstrapLoaderImpl}.
 */
public class TestBootstrapLoader extends BasePluginRegistryTest {

  @Test
  public void testBootstrapLoader() throws Exception {
    try (OperatorFixture fixture = OperatorFixture.standardFixture(dirTestWatcher)) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      PluginBootstrapLoaderImpl loader = new PluginBootstrapLoaderImpl(context);
      Map<String, URL> pluginURLMap = new HashMap<>();
      StoragePlugins plugins = loader.loadBootstrapPlugins(pluginURLMap);

      // Sanity test. Change this if the bootstrap file changes.
      // No need to test contents; here we assume serialization works.
      // See FormatPluginSerDeTest
      assertNotNull(plugins.getConfig("dfs"));
      assertNotNull(plugins.getConfig("s3"));
      assertNotNull(plugins.getConfig("cp"));

      // Cannot test contrib plugins here: they are not yet
      // available when this test is run. We'll trust the
      // classpath scanner.
    }
  }

  @Test
  public void testMissingBootstrapFile() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    // Note: file does not actually exist, which is intentional.
    String bootstrapFile = RESOURCE_BASE + "missing-bootstrap.json";
    builder.configBuilder().put(ExecConstants.BOOTSTRAP_STORAGE_PLUGINS_FILE, bootstrapFile);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      PluginBootstrapLoaderImpl loader = new PluginBootstrapLoaderImpl(context);
      try {
        loader.loadBootstrapPlugins(new HashMap<>());
        fail();
      } catch (IOException e) {
        assertTrue(e.getMessage().contains("Cannot find"));
        assertTrue(e.getMessage().contains(bootstrapFile));
      }
    }
  }

  /**
   * Few things are as frustrating as tracking down plugin errors. Here we ensure
   * that the bootstrap loader explains where to look by naming the failed file
   * if bootstrap fails.
   */
  @Test
  public void testFailedBootstrapLoad() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    String bootstrapFile = RESOURCE_BASE + "bogus-bootstrap.json";
    builder.configBuilder().put(ExecConstants.BOOTSTRAP_STORAGE_PLUGINS_FILE, bootstrapFile);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      PluginBootstrapLoaderImpl loader = new PluginBootstrapLoaderImpl(context);
      try {
        loader.loadBootstrapPlugins(new HashMap<>());
        fail();
      } catch (IOException e) {
        // Resource URL
        assertTrue(e.getMessage().contains(bootstrapFile));
        // Jackson-provided bad key
        assertTrue(e.getCause().getMessage().contains("imABadBoy"));
      }
    }
  }

  @Test
  public void testDuplicateBootstrapEntries() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    String bootstrapFile = RESOURCE_BASE + "dup-bootstrap.json";
    builder.configBuilder().put(ExecConstants.BOOTSTRAP_STORAGE_PLUGINS_FILE, bootstrapFile);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      PluginBootstrapLoaderImpl loader = new PluginBootstrapLoaderImpl(context);
      StoragePlugins plugins = loader.loadBootstrapPlugins(new HashMap<>());

      // Duplicates noted in log; last one wins.

      StoragePluginConfig pluginConfig = plugins.getConfig("cp");
      assertNotNull(pluginConfig);
      assertTrue(pluginConfig instanceof FileSystemConfig);
      FileSystemConfig cpConfig = (FileSystemConfig) pluginConfig;
      assertNotNull(cpConfig.getFormats().get("tsv"));
    }
  }

  @Test
  public void testMissingBootstrapUpgrades() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    // Note: file does not actually exist, which is intentional.
    String bootstrapFile = RESOURCE_BASE + "missing-plugin-upgrade.json";
    builder.configBuilder().put(ExecConstants.UPGRADE_STORAGE_PLUGINS_FILE, bootstrapFile);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      PluginBootstrapLoader loader = new PluginBootstrapLoaderImpl(context);
      StoragePlugins plugins = loader.updatedPlugins();
      assertNull(plugins);
    }
  }

  @Test
  public void testBootstrapUpgrades() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    String bootstrapFile = RESOURCE_BASE + "mock-plugin-upgrade.json";
    builder.configBuilder().put(ExecConstants.UPGRADE_STORAGE_PLUGINS_FILE, bootstrapFile);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      PluginBootstrapLoader loader = new PluginBootstrapLoaderImpl(context);
      StoragePlugins plugins = loader.updatedPlugins();
      assertNotNull(plugins);

      // dfs: removed psv
      StoragePluginConfig pluginConfig = plugins.getConfig("dfs");
      assertNotNull(pluginConfig);
      FileSystemConfig dfs = (FileSystemConfig) pluginConfig;
      assertNull(dfs.getFormats().get("psv"));
      assertNotNull(dfs.getFormats().get("csv"));

      // local added
      assertNotNull(plugins.getConfig("local"));

      // S3, bsv added
      pluginConfig = plugins.getConfig("s3");
      assertNotNull(pluginConfig);
      FileSystemConfig s3 = (FileSystemConfig) pluginConfig;
      assertNotNull(s3.getFormats().get("bsv"));

      // cp, left unchanged (not in upgrade file)
      assertNull(plugins.getConfig("cp"));
    }
  }

  @Test
  public void testBootstrapLoaderWithUpgrades() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    String bootstrapFile = RESOURCE_BASE + "mock-plugin-upgrade.json";
    builder.configBuilder().put(ExecConstants.UPGRADE_STORAGE_PLUGINS_FILE, bootstrapFile);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      PluginBootstrapLoader loader = new PluginBootstrapLoaderImpl(context);
      StoragePlugins plugins = loader.bootstrapPlugins();

      // dfs: removed psv
      StoragePluginConfig pluginConfig = plugins.getConfig("dfs");
      assertNotNull(pluginConfig);
      FileSystemConfig dfs = (FileSystemConfig) pluginConfig;
      assertNull(dfs.getFormats().get("psv"));
      assertNotNull(dfs.getFormats().get("csv"));

      // local added
      assertNotNull(plugins.getConfig("local"));

      // S3, bsv added
      pluginConfig = plugins.getConfig("s3");
      assertNotNull(pluginConfig);
      FileSystemConfig s3 = (FileSystemConfig) pluginConfig;
      assertNotNull(s3.getFormats().get("bsv"));

      // cp, left unchanged (not in upgrade file)
      assertNotNull(plugins.getConfig("cp"));
    }
  }

  /**
   * Test a format bootstrap with a mock file. Can't use a real
   * file because those appear in modules not yet available when
   * this test runs.
   */
  @Test
  public void testBootstrapLoaderWithFormats() throws Exception {
    OperatorFixture.Builder builder = OperatorFixture.builder(dirTestWatcher);
    String bootstrapFile = RESOURCE_BASE + "mock-format-bootstrap.json";
    builder.configBuilder().put(ExecConstants.BOOTSTRAP_FORMAT_PLUGINS_FILE, bootstrapFile);
    try (OperatorFixture fixture = builder.build()) {
      PluginRegistryContextFixture context = new PluginRegistryContextFixture(fixture);
      PluginBootstrapLoader loader = new PluginBootstrapLoaderImpl(context);
      StoragePlugins plugins = loader.bootstrapPlugins();

      // bsv added to dfs
      StoragePluginConfig pluginConfig = plugins.getConfig("dfs");
      assertNotNull(pluginConfig);
      FileSystemConfig dfs = (FileSystemConfig) pluginConfig;
      assertNotNull(dfs.getFormats().get("bsv"));
    }
  }
}
