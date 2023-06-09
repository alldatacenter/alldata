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
import java.util.Map;
import java.util.Set;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.store.dfs.FormatPlugin;

import com.fasterxml.jackson.databind.ObjectMapper;

public interface StoragePluginRegistry extends Iterable<Map.Entry<String, StoragePlugin>>, AutoCloseable {
  String PSTORE_NAME = "sys.storage_plugins";

  @SuppressWarnings("serial")
  public static class PluginException extends Exception {
    public PluginException(String msg) {
      super(msg);
    }

    public PluginException(String msg, Throwable e) {
      super(msg, e);
    }

    public static PluginException systemPluginException(String operation, String name) {
      return new PluginException(String.format(
          "Cannot %s a system plugin: `%s`", operation, name));
    }
  }

  /**
   * Indicates the requested plugin was not found.
   */
  @SuppressWarnings("serial")
  public static class PluginNotFoundException extends PluginException {
    public PluginNotFoundException(String name) {
      super("No storage plugin exists with name: `" + name + "`");
    }
  }

  /**
   * Indicates an error when decoding a plugin from JSON.
   */
  @SuppressWarnings("serial")
  public static class PluginEncodingException extends PluginException {
    public PluginEncodingException(String msg, Exception e) {
      super(msg, e);
    }
  }

  /**
   * Initialize the storage plugin registry. Must be called before the registry
   * is used.
   */
  void init();

  /**
   * Store a plugin by name and configuration. If the plugin already exists,
   * update the plugin. This form directly updates persistent storage. The
   * in-memory cache is updated on the next refresh. This form will accept an
   * invalid plugin, which will be disabled upon refresh. Since Drill is
   * distributed, and plugins work with external systems, the external system
   * can become invalid at any moment (not just when pugins are updated), so the
   * model used for {@code put()} mimics normal runtime operation.
   *
   * @param name The name of the plugin
   * @param config The plugin configuration
   * @return The StoragePlugin instance.
   * @throws PluginException if plugin cannot be created
   */
  void put(String name, StoragePluginConfig config) throws PluginException;

  /**
   * Like {@link #put(String, StoragePluginConfig)}, but forces instantiation of the
   * plugin to verify that the configuration is valid at this moment in time.
   */
  void validatedPut(String name, StoragePluginConfig config) throws PluginException;

  /**
   * Set the plugin to the requested enabled state. Does nothing if the plugin
   * is already in the requested state. If a formerly enabled plugin is
   * disabled, moves the plugin from the in-memory cache to the ephemeral
   * store. If a formerly disabled plugin is enabled, verifies that the plugin
   * can be instantiated as for {@link #verifiedPut()}.
   * <p>
   * Use this method when changing state. Do not obtain the config and change
   * the state directly, doing so will make the plugin config inconsistent
   * with the internal state.
   *
   * @param name name of the plugin
   * @param enabled {@code true} to enable the plugin, {@code false} to disable
   * @throws PluginNotFoundException if the plugin is not found
   * @throws PluginException if the plugin name is not valid or
   * if enabling a plugin and the plugin is not valid
   */
  void setEnabled(String name, boolean enabled) throws PluginException;

  /**
   * Get a plugin by name. Create it based on the PStore saved definition if it doesn't exist.
   *
   * @param name The name of the plugin
   * @return The StoragePlugin instance.
   * @throws PluginException if plugin cannot be obtained
   */
  StoragePlugin getPlugin(String name) throws PluginException, UserException;

  /**
   * Get a plugin by configuration. If it doesn't exist, create it.
   *
   * @param config The configuration for the plugin.
   * @return The StoragePlugin instance.
   * @throws PluginException if plugin cannot be obtained
   */
  StoragePlugin getPluginByConfig(StoragePluginConfig config) throws PluginException;

  /**
   * @deprecated use {@link #resolve(StoragePluginConfig, Class)} which provides
   * type safety. Retained for compatibility with older plugins
   */
  @Deprecated
  StoragePlugin getPlugin(StoragePluginConfig config) throws ExecutionSetupException;

  /**
   * Return a plugin from persistent storage. Returns both enabled and
   * disabled stored plugins, but does not return system plugins.
   * Use this to obtain a plugin for editing (rather than
   * for planning or executing a query.)
   */
  StoragePluginConfig getStoredConfig(String name);

  /**
   * Return a config encoded as JSON.
   * @throws PluginException if the plugin is undefined
   */
  String encode(String name) throws PluginException;
  String encode(StoragePluginConfig config);

  /**
   * Return a config decoded from JSON.
   * @throws PluginEncodingException if the JSON is invalid
   */
  StoragePluginConfig decode(String json) throws PluginEncodingException;

  /**
   * Put a storage plugin config from JSON. Validates the JSON and the
   * resulting storage plugin. Use this form for JSON received from
   * the UI or other external source.
   *
   * @throws IOException if the JSON is invalid
   * @throws PluginException if the underlying
   * {@link #validatedPut(String, StoragePluginConfig)} fails
   */
  void putJson(String name, String json) throws PluginException;

  /**
   * Copy a stored config so that it can be modified.
   * <p>
   * <i><b>Never modify a config stored in the registry!</b></i>
   * Configs are keyed by name and value; getting a config, then
   * modifying it, will cause the value maps to become out of sync.
   *
   * @param name name of the storage plugin config to copy
   * @return a copy of the config
   * @throws PluginException if the name is undefined
   */
  StoragePluginConfig copyConfig(String name) throws PluginException;

  /**
   * Copy the given storage plugin config so it may be modified.
   *
   * @param config the storage plugin config to copy
   * @return the copy
   */
  StoragePluginConfig copyConfig(StoragePluginConfig config);

  /**
   * Retrieve an available configuration. Returns enabled stored plugins
   * and system plugins. These configs are those that can be used to
   * plan a query.
   */
  StoragePluginConfig getDefinedConfig(String name);

  /**
   * Remove a plugin by name
   *
   * @param name The name of the storage plugin to remove
   * @throws PluginException
   */
  void remove(String name) throws PluginException;

  /**
   * Returns a set of all stored plugin configurations,
   * directly from the persistent store. Note: the actual
   * configs may reside in the cache; make a copy before
   * making any changes.
   * @return map of stored plugin configurations
   */
  Map<String, StoragePluginConfig> storedConfigs();

  enum PluginFilter { ALL, ENABLED, DISABLED };

  /**
   * Return a possibly-filtered set of plugins from the persistent
   * store.
   */
  Map<String, StoragePluginConfig> storedConfigs(PluginFilter filter);

  /**
   * Returns a copy of the set of enabled stored plugin configurations.
   * The registry is refreshed against the persistent store prior
   * to building the map.
   * @return map of enabled, stored plugin configurations
   */
  Map<String, StoragePluginConfig> enabledConfigs();

  /**
   * Returns the set of available plugin names.
   * Includes system plugins and enabled stored plugins.
   */
  Set<String> availablePlugins();

  /**
   * Safe way to add or remove a format plugin config from a stored file
   * system configuration. Makes a copy of the config, adds/removes the
   * format plugin, and updates the persistent store with the copy.
   *
   * @param pluginName name of the file system storage plugin config to
   * modify
   * @param formatName name of the format plugin to modify
   * @param formatConfig if null, removes the plugin, if non-null updates
   * the format plugin config with this value
   * @throws PluginException if the storage plugin is undefined or
   * is not a file format plugin
   */
  void putFormatPlugin(String pluginName, String formatName,
      FormatPluginConfig formatConfig) throws PluginException;

  /**
   * Get the Format plugin for the FileSystemPlugin associated with the provided
   * storage config and format config.
   *
   * @param storageConfig
   *          The storage config for the associated FileSystemPlugin
   * @param formatConfig
   *          The format config for the associated FormatPlugin
   * @return A FormatPlugin instance
   * @throws PluginException
   *           if plugin cannot be obtained
   */
  FormatPlugin getFormatPluginByConfig(StoragePluginConfig storageConfig,
      FormatPluginConfig formatConfig) throws PluginException;

  /**
   * @deprecated use
   * {@link #resolveFormat(StoragePluginConfig, FormatPluginConfig, Class)}
   * which provides type safety. Retained for compatibility with older plugins
   */
  @Deprecated
  FormatPlugin getFormatPlugin(StoragePluginConfig storageConfig,
      FormatPluginConfig formatConfig) throws ExecutionSetupException;

  /**
   * Get the Schema factory associated with this storage plugin registry.
   *
   * @return A SchemaFactory that can register the schemas associated with this plugin registry.
   */
  SchemaFactory getSchemaFactory();

  /**
   * Object mapper to read/write the JSON form of a plugin.
   * config.
   */
  ObjectMapper mapper();

  <T extends StoragePlugin> T resolve(
      StoragePluginConfig storageConfig, Class<T> desired);

  /**
   * Resolve a storage plugin given a storage plugin config. Call from
   * within a file storage plugin to resolve the plugin.
   *
   * @param <T> the required type of the plugin
   * @param storageConfig storage plugin config
   * @param formatConfig format plugin config
   * @param desired desired target class
   * @return the storage plugin
   * @throws IllegalStateException if the plugin is unknown or of the wrong
   * format - errors which should never occur in normal operation
   */
  <T extends FormatPlugin> T resolveFormat(
      StoragePluginConfig storageConfig,
      FormatPluginConfig formatConfig, Class<T> desired);
}
