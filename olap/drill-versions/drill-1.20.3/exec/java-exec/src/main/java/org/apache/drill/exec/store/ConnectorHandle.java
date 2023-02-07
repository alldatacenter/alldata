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

import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.store.PluginHandle.PluginType;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Defines a storage connector: a storage plugin config along with the
 * locator which can create a plugin instance given an instance of the
 * config.
 */
public class ConnectorHandle {

  private static final Logger logger = LoggerFactory.getLogger(ConnectorHandle.class);

  private final ConnectorLocator locator;
  private final Class<? extends StoragePluginConfig> configClass;

  /**
   * Intrinsic (system) plugins are created at start-up time and do
   * not allow additional plugin instances.
   */
  private final boolean isIntrinsic;

  private ConnectorHandle(ConnectorLocator locator,
      Class<? extends StoragePluginConfig> configClass) {
    this.locator = locator;
    this.configClass = configClass;
    this.isIntrinsic = false;
  }

  private ConnectorHandle(ConnectorLocator locator, StoragePlugin plugin) {
    this.locator = locator;
    this.configClass = plugin.getConfig().getClass();
    this.isIntrinsic = true;
  }

  /**
   * Construct a handle for a "normal" connector which takes a plugin config
   * and constructs a plugin instance based on that config.
   */
  public static ConnectorHandle configuredConnector(ConnectorLocator locator,
      Class<? extends StoragePluginConfig> configClass) {
    return new ConnectorHandle(locator, configClass);
  }

  /**
   * Construct a handle for an intrinsic (system) connector which always
   * uses a single config: the one created along with the plugin instance
   * itself.
   */
  public static ConnectorHandle intrinsicConnector(ConnectorLocator locator,
      StoragePlugin plugin) {
    return new ConnectorHandle(locator, plugin);
  }

  /**
   * An intrinsic connector is one defined for the life of the Drillbit.
   * It cannot be configured, changed or removed. It is not stored.
   */
  public boolean isIntrinsic() { return isIntrinsic; }
  public boolean isStored() { return ! isIntrinsic() && locator.storable(); }
  public ConnectorLocator locator() { return locator; }

  public Class<? extends StoragePluginConfig> configClass() {
    return configClass;
  }

  public Class<? extends StoragePlugin> connectorClass() {
    return locator.connectorClassFor(configClass);
  }

  public PluginHandle pluginEntryFor(String name, StoragePluginConfig config, PluginType type) {
    Preconditions.checkArgument(configClass.isInstance(config));
    Preconditions.checkArgument(type != PluginType.INTRINSIC || isIntrinsic());
    return new PluginHandle(name, config, this, type);
  }

  public StoragePlugin newInstance(String name, StoragePluginConfig config) throws Exception {
    Preconditions.checkNotNull(name);
    Preconditions.checkNotNull(config);
    Preconditions.checkArgument(configClass.isInstance(config));
    return locator.create(name, config);
  }
}
