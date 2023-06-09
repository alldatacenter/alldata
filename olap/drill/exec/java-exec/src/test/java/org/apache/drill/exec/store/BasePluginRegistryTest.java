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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.test.BaseDirTestWatcher;
import org.apache.drill.test.BaseTest;
import org.apache.drill.test.OperatorFixture;
import org.junit.ClassRule;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BasePluginRegistryTest extends BaseTest {

  @ClassRule
  public static final BaseDirTestWatcher dirTestWatcher = new BaseDirTestWatcher();

  protected static final String RESOURCE_BASE = "plugins/";

  protected static class PluginRegistryContextFixture implements PluginRegistryContext {

    private final DrillConfig drillConfig;
    private final ScanResult classpathScan;
    private final ObjectMapper mapper;

    public PluginRegistryContextFixture(OperatorFixture opFixture) {
      drillConfig = opFixture.config();
      classpathScan = ClassPathScanner.fromPrescan(drillConfig);
      LogicalPlanPersistence lpPersistence = new LogicalPlanPersistence(drillConfig, classpathScan);

      mapper = lpPersistence.getMapper();
    }
    @Override
    public DrillConfig config() { return drillConfig; }

    @Override
    public ObjectMapper mapper() { return mapper; }

    @Override
    public ScanResult classpathScan() { return classpathScan; }

    // Not ideal, but we don't want to start the entire Drillbit
    // for these tests.
    @Override
    public DrillbitContext drillbitContext() { return null; }

    @Override
    public ObjectMapper hoconMapper() { return mapper; }
  }

  public static class StoragePluginFixtureConfig extends StoragePluginConfig {

    private final String mode;

    @JsonCreator
    public StoragePluginFixtureConfig(@JsonProperty("mode") String mode) {
      this.mode = mode;
    }

    @JsonProperty("mode")
    public String mode() { return mode; }

    @Override
    public boolean equals(Object o) {
      if (o == this) {
        return true;
      }
      if (o == null || o.getClass() != getClass()) {
        return false;
      }
      StoragePluginFixtureConfig other = (StoragePluginFixtureConfig) o;
      return Objects.equals(mode, other.mode);
    }

    @Override
    public int hashCode() {
      return Objects.hash(mode);
    }
  }

  @PrivatePlugin
  public static class StoragePluginFixture extends AbstractStoragePlugin {

    private final StoragePluginFixtureConfig config;
    private int closeCount;

    public StoragePluginFixture(StoragePluginFixtureConfig config, DrillbitContext inContext, String inName) {
      super(inContext, inName);
      this.config = config;
      if (config.mode().equals("crash-ctor")) {
        throw new IllegalStateException();
      }
    }

    @Override
    public void start() {
      if (config.mode().equals("crash-start")) {
        throw new IllegalStateException();
      }
    }

    @Override
    public StoragePluginConfig getConfig() {
      return config;
    }

    @Override
    public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent)
        throws IOException {
      assert false;
    }

    @Override
    public void close() {
      closeCount++;
      if (config.mode().equals("crash-close")) {
        throw new IllegalStateException();
      }
    }

    public int closeCount() { return closeCount; }
  }

  protected static class LocatorFixture implements ConnectorLocator {

    private final Constructor<? extends StoragePlugin> ctor;

    public LocatorFixture() {
      Map<Class<? extends StoragePluginConfig>, Constructor<? extends StoragePlugin>> ctors =
          ClassicConnectorLocator.constuctorsFor(StoragePluginFixture.class);
      assertEquals(1, ctors.size());
      assertTrue(ctors.containsKey(StoragePluginFixtureConfig.class));
      ctor = ctors.get(StoragePluginFixtureConfig.class);
    }

    @Override
    public void init() { }

    @Override
    public StoragePlugins bootstrapPlugins() {
      return null;
    }

    @Override
    public StoragePlugins updatedPlugins() { return null; }

    @Override
    public void onUpgrade() { }

    @Override
    public Collection<StoragePlugin> intrinsicPlugins() {
      return null;
    }

    @Override
    public StoragePlugin get(String name) { return null; }

    @Override
    public Set<Class<? extends StoragePluginConfig>> configClasses() {
      return null;
    }

    @Override
    public StoragePlugin create(String name, StoragePluginConfig config)
        throws Exception {
      return ctor.newInstance(config, null, name);
    }

    @Override
    public boolean storable() { return false; }

    @Override
    public Class<? extends StoragePlugin> connectorClassFor(
        Class<? extends StoragePluginConfig> configClass) {
      return ctor.getDeclaringClass();
    }

    @Override
    public void close() { }
  }
}
