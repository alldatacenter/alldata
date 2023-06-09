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

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.planner.logical.StoragePlugins;

/**
 * Locates storage plugins. Allows multiple ways of finding plugins.
 * <p>
 * Terminology is a bit tortured. Drill overuses the term "plugin."
 * Here we adopt the following conventions:
 * <dl>
 * <dt>(storage) plugin</dt>
 * <dd>The user-visible concept of a storage plugin: a configuration
 * with our without its corresponding connector instance. Either an
 * instance of a storage plugin config class, or the
 * same information serialized as JSON. Also, confusingly, "plugin"
 * can also mean the config wrapped in an instance of the connector.
 * </dd>
 * <dl>
 * <p>
 * Connectors can be storable in the persistent store. All "normal"
 * connectors have storable configurations. System plugins, however
 * have a fixed config that is not storable.
 * <p>
 * Connectors can define bootstrap or upgrade plugin sets. "Normal"
 * plugins usually provide bootstrap configs, system plugins do not.
 * <p>
 * This class instantiates a connector given a configuration and a
 * name. The plugin registry caches the instance for the duration of
 * the Drillbit run, or until the config changes.
 */
// TODO: Sort out error handling. Some method throw IOException, some
// throw unchecked exceptions.
public interface ConnectorLocator {

  /**
   * Initialize the locator. Must be called before the locator is used.
   */
  void init();

  /**
   * When starting a new installation, called to load bootstrap
   * plugins (configurations) that come "out-of-the-box."
   *
   * @return the set of bootstrap plugins, or {@code null} if this locator
   * does not provide bootstrap plugins
   * @throws IOException
   */
  StoragePlugins bootstrapPlugins() throws IOException;

  /**
   * Identify plugins to be added to an existing system, typically
   * on the first run after an upgrade.
   * <p>
   * TODO: The current mechanism depends on deleting a file after the
   * first run, which is unreliable. It won't, for example, correctly
   * handle a restored ZK. A better mechanism would store a version
   * number in the persistent store, and pass that version number into
   * this method.
   *
   * @return the set of plugin configurations to refresh in the
   * persistent store, or null if none to update
   * @throws IOException for errors
   */
  StoragePlugins updatedPlugins();

  /**
   * If {@code updatedPlugins()} returned non-null, then the
   * registry will call this method after successful update of
   * the persistent store. This method can do any post-update
   * cleanup, such as deleting the file mentioned above.
   */
  void onUpgrade();

  /**
   * Enumerate the intrinsic plugins. An intrinsic plugin is one
   * which takes no configuration and which therefore cannot be
   * disabled, and thus is always available. Example: Drill's
   * system plugins. For an intrinsic plugin, the plugin name is
   * also the name of the configuration.
   *
   * @return map of intrinsic plugins which require no configuration
   */
  Collection<StoragePlugin> intrinsicPlugins();

  /**
   * Retrieve an instance of the named connector with default configuration.
   * Typically used for connectors with no configuration, such as system
   * storage plugins.
   *
   * @param name the name of a <i>connector class</i> (not the name of
   * a plugin (configuration)
   * @return a plugin with default configuration, or null if this locator does
   * not support such plugins
   */
  StoragePlugin get(String name);

  /**
   * Return the set of known storage plugin configuration classes for which
   * the user can create configs. Excludes system plugin configs. Used
   * to map config classes to this locator to create plugin instances.
   *
   * @return the unuordered set of storage plugin configuration classes
   * available from this locator. Can be null if this locator offers
   * only system plugins
   */
  Set<Class<? extends StoragePluginConfig>> configClasses();

  /**
   * Create a connector instance given a named configuration. The configuration
   * and/or name is used to locate the connector class.
   *
   * @param name name of the storage plugin (configuration).
   * @param pluginConfig the deserialized Java configuration object.
   * @return a connector of the proper class that matches the configuration or
   * name, initialized with the configuration
   * @throws ExecutionSetupException for all errors
   */
  StoragePlugin create(String name, StoragePluginConfig pluginConfig) throws Exception;

  /**
   * @return true if configs for this locator should be persisted, false if
   * these are ad-hoc or otherwise per-run connectors
   */
  boolean storable();

  /**
   * Given a configuration class, return the corresponding connector
   * (plugin) class.
   */
  Class<? extends StoragePlugin> connectorClassFor(
      Class<? extends StoragePluginConfig> configClass);

  void close();
}
