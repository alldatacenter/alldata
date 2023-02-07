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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.drill.common.collections.ImmutableEntry;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.PluginHandle.PluginType;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.FormatPlugin;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheBuilder;
import org.apache.drill.shaded.guava.com.google.common.cache.CacheLoader;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;
import org.apache.drill.shaded.guava.com.google.common.cache.RemovalListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

/**
 * Plugin registry. Caches plugin instances which correspond to configurations
 * stored in persistent storage. Synchronizes the instances and storage.
 * <p>
 * Allows multiple "locators" to provide plugin classes such as the "classic"
 * version for classes in the same class loader, the "system" version for
 * system-defined plugins.
 * <p>
 * provides multiple layers of abstraction:
 * <ul>
 * <li>A plugin config/implementation pair (called a "connector" here)
 * is located by</li>
 * <li>A connector locator, which also provides bootstrap plugins and can
 * create a plugin instance from a configuration, which are cached in</li>
 * <li>The plugin cache, which holds stored, system and ad-hoc plugins. The
 * stored plugins are backed by</li>
 * <li>A persistent store: the file system for tests and embedded, ZK for
 * a distibuted server, or</li>
 * <li>An ephemeral cache for unnamed configs, such as those created by
 * a table function.</li>
 * </ul>
 * <p>
 * The idea is to push most functionality into the above abstractions,
 * leaving overall coordination here.
 * <p>
 * Plugins themselves have multiple levels of definitions:
 * <ul>
 * <li>The config and plugin classes, provided by the locator.</li>
 * <li>The {@link ConnectorHandle} which defines the config class and
 * the locator which can create instances of that class.</li>
 * <li>A config instance which is typically deserialized from JSON
 * independent of the implementation class.</li>
 * <li>A {@link PluginHandle} which pairs the config with a name as
 * the unit that the user thinks of as a "plugin." The plugin entry
 * links to the {@code ConnectorEntry} to create the instance lazily
 * when first requested.</li>
 * <li>The plugin class instance, which provides long-term state and
 * which provides the logic for the plugin.</li>
 * </ul>
 *
 * <h4>Concurrency</h4>
 *
 * Drill is a concurrent system; multiple users can attempt to add, remove
 * and update plugin configurations at the same time. The only good
 * solution would be to version the plugin configs. Instead, we rely on
 * the fact that configs change infrequently.
 * <p>
 * The code syncs the in-memory cache with the persistent store on each
 * access (which is actually inefficient and should be reviewed.)
 * <p>
 * During refresh, it could be that another thread is doing exactly
 * the same thing, or even fighting us by changing the config. It is
 * impossible to ensure a totally consistent answer. The goal is to
 * make sure that the cache ends up agreeing with the persistent store
 * as it was at some point in time.
 * <p>
 * The {@link PluginsMap} class provides in-memory synchronization of the
 * name and config maps. Careful coding is needed when handling refresh
 * since another thread could make the same changes.
 * <p>
 * Once the planner obtains a plugin, another user could come along and
 * change the config for that plugin. Drill treats that change as another
 * plugin: the original one continues to be used by the planner (but see
 * below), while new queries use the new version.
 * <p>
 * Since the config on remote servers may have changed relative to the one
 * this Foreman used for planning, the plan includes the plugin config
 * itself (not just a reference to the config.) This works because the
 * config is usually small.
 *
 * <h4>Ephemeral Plugins</h4>
 *
 * An ephemeral plugin handles table functions which create a temporary,
 * unnamed configuration that is needed only for the execution of a
 * single query, but which may be used across many threads. If the same
 * table function is used multiple times, then the same ephemeral plugin
 * will be used across queries. Ephemeral plugins are are based on the
 * same connectors as stored plugins, but are not visible to the planner.
 * They will expire after some time or number.
 * <p>
 * The ephemeral store also acts as a graveyard for deleted or changed
 * plugins. When removing a plugin, the old plugin is moved to ephemeral
 * storage to allow running queries to locate it. Similarly, when a
 * new configuration is stored, the corresponding plugin is retrieved
 * from ephemeral storage, if it exists. This avoids odd cases where
 * the same plugin exists in both normal and ephemeral storage.
 *
 * <h4>Caveats</h4>
 *
 * The main problem with synchronization at present is that plugins
 * provide a {@link close()} method that, if used, could render the
 * plugin unusable. Suppose a Cassandra plugin, say, maintains a connection
 * to a server used across multiple queries and threads. Any change to
 * the config immediately calls {@code close()} on the plugin, even though
 * it may be in use in planning a query on another thread. Random failures
 * will result.
 * <p>
 * The same issue can affect ephemeral plugins: if the number in the cache
 * reaches the limit, the registry will start closing old ones, without
 * knowning if that plugin is actually in use.
 * <p>
 * The workaround is to not actually honor the {@code close()} call. Longer
 * term, a reference count is needed.
 *
 * <h4>Error Handling</h4>
 *
 * Error handling needs review. Those problems that result from user actions
 * should be raised as a {@code UserException}. Those that violate invariants
 * as other forms of exception.
 */
public class StoragePluginRegistryImpl implements StoragePluginRegistry {
  private static final Logger logger = LoggerFactory.getLogger(StoragePluginRegistryImpl.class);

  private final PluginRegistryContext context;

  /**
   * Cache of enabled, stored plugins, as well as system and ad-hoc
   * plugins. Plugins live in the cache until Drillbit exit, or
   * (except for system plugins) explicitly removed.
   */
  private final StoragePluginMap pluginCache;
  private final DrillSchemaFactory schemaFactory;
  private final StoragePluginStore pluginStore;

  /**
   * Cache of unnamed plugins typically resulting from table functions.
   * Ephemeral plugins timeout after some time, or some max number of
   * plugins.
   */
  private final LoadingCache<StoragePluginConfig, PluginHandle> ephemeralPlugins;

  /**
   * Set of locators which provide connector implementations.
   */
  private final List<ConnectorLocator> locators = new ArrayList<>();

  /**
   * Map of config (as deserialized from the persistent store or UI)
   * to the connector which can instantiate a connector for that config.
   */
  private final Map<Class<? extends StoragePluginConfig>, ConnectorHandle> connectors =
      new IdentityHashMap<>();

  public StoragePluginRegistryImpl(DrillbitContext context) {
    this.context = new DrillbitPluginRegistryContext(context);
    this.pluginCache = new StoragePluginMap();
    this.schemaFactory = new DrillSchemaFactory(null);
    locators.add(new ClassicConnectorLocator(this.context));
    locators.add(new SystemPluginLocator(this.context));
    this.pluginStore = new StoragePluginStoreImpl(context);
    this.ephemeralPlugins = CacheBuilder.newBuilder()
        .expireAfterAccess(24, TimeUnit.HOURS)
        .maximumSize(250)
        .removalListener(
            (RemovalListener<StoragePluginConfig, PluginHandle>) notification -> notification.getValue().close())
        .build(new CacheLoader<StoragePluginConfig, PluginHandle>() {
          @Override
          public PluginHandle load(StoragePluginConfig config) throws Exception {
            return createPluginEntry("$$ephemeral$$", config, PluginType.EPHEMERAL);
          }
        });
  }

  @Override
  public void init() {
    locators.stream().forEach(loc -> loc.init());
    try {
      loadIntrinsicPlugins();
    } catch (PluginException e) {
      // Should only occur for a programming error
      throw new IllegalStateException("Failed to load system plugins", e);
    }
    defineConnectors();
    prepareStore();
  }

  private void loadIntrinsicPlugins() throws PluginException {
    for (ConnectorLocator locator : locators) {
      Collection<StoragePlugin> intrinsicPlugins = locator.intrinsicPlugins();
      if (intrinsicPlugins == null) {
        continue;
      }
      for (StoragePlugin sysPlugin : intrinsicPlugins) {
        // Enforce lower case names. Since the name of a system plugin
        // is "hard coded", we can't adjust it if it is not already
        // lower case. All we can do is fail to tell the developer that
        // something is wrong.
        String origName = sysPlugin.getName();
        String lcName = sysPlugin.getName().toLowerCase();
        if (!origName.equals(lcName)) {
          throw new IllegalStateException(String.format(
              "Plugin names must be in lower case but system plugin name `%s` is not",
              origName));
        }
        ConnectorHandle connector = ConnectorHandle.intrinsicConnector(locator, sysPlugin);
        defineConnector(connector);
        pluginCache.put(new PluginHandle(sysPlugin, connector, PluginType.INTRINSIC));
      }
    }
  }

  private void defineConnector(ConnectorHandle connector) {
    ConnectorHandle prev = connectors.put(connector.configClass(), connector);
    if (prev != null) {
      String msg = String.format("Two connectors defined for the same config: " +
          "%s -> %s and %s -> %s",
          connector.configClass().getName(), connector.locator().getClass().getName(),
          prev.configClass().getName(), prev.locator().getClass().getName());
      logger.error(msg);
      throw new IllegalStateException(msg);
    }
  }

  private void defineConnectors() {
    for (ConnectorLocator locator : locators) {
      Set<Class<? extends StoragePluginConfig>> nonIntrinsicConfigs = locator.configClasses();
      if (nonIntrinsicConfigs == null) {
        continue;
      }
      for (Class<? extends StoragePluginConfig> configClass : nonIntrinsicConfigs) {
        defineConnector(ConnectorHandle.configuredConnector(locator, configClass));
      }
    }
  }

  private void prepareStore() {
    if (loadEnabledPlugins()) {
      upgradeStore();
    } else {
      initStore();
    }
  }

  private void initStore() {
    logger.info("No storage plugin instances configured in persistent store, loading bootstrap configuration.");
    StoragePlugins bootstrapPlugins = new StoragePlugins();
    try {
      for (ConnectorLocator locator : locators) {
        StoragePlugins locatorPlugins = locator.bootstrapPlugins();
        if (locatorPlugins != null) {
          bootstrapPlugins.putAll(locatorPlugins);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException(
          "Failure initializing the plugin store. Drillbit exiting.", e);
    }
    pluginStore.putAll(bootstrapPlugins);
    locators.stream().forEach(loc -> loc.onUpgrade());
  }

  /**
   * Upgrade an existing persistent plugin config store with
   * updates available from each locator.
   */
  private void upgradeStore() {
    StoragePlugins upgraded = new StoragePlugins();
    for (ConnectorLocator locator : locators) {
      StoragePlugins locatorPlugins = locator.updatedPlugins();
      if (upgraded != null) {
        upgraded.putAll(locatorPlugins);
      }
    }
    if (upgraded.isEmpty()) {
      return;
    }
    for (Map.Entry<String, StoragePluginConfig> newPlugin : upgraded) {
      StoragePluginConfig oldPluginConfig = getStoredConfig(newPlugin.getKey());
      if (oldPluginConfig != null) {
        copyPluginStatus(oldPluginConfig, newPlugin.getValue());
      }
      pluginStore.put(newPlugin.getKey(), newPlugin.getValue());
    }
  }

  /**
   * Identifies the enabled status for new storage plugins
   * config. If this status is absent in the updater file, the status is kept
   * from the configs, which are going to be updated
   *
   * @param oldPluginConfig
   *          current storage plugin config from Persistent Store or bootstrap
   *          config file
   * @param newPluginConfig
   *          new storage plugin config
   */
  protected static void copyPluginStatus(
      StoragePluginConfig oldPluginConfig,
      StoragePluginConfig newPluginConfig) {
    if (!newPluginConfig.isEnabledStatusPresent()) {
      boolean newStatus = oldPluginConfig != null && oldPluginConfig.isEnabled();
      newPluginConfig.setEnabled(newStatus);
    }
  }

  /**
   * Initializes {@link #pluginCache} with currently enabled plugins
   * defined in the persistent store.
   *
   * @return {@code true} if the persistent store contained plugins
   * (and thus was initialized, and should perhaps be upgraded), or
   * {@code false} if no plugins were found and this this is a new store
   * which should be initialized. Avoids the need to check persistent
   * store contents twice
   */
  private boolean loadEnabledPlugins() {
    Iterator<Entry<String, StoragePluginConfig>> allPlugins = pluginStore.load();
    int count = 0;
    while (allPlugins.hasNext()) {
      count++;
      Entry<String, StoragePluginConfig> plugin = allPlugins.next();
      String name = plugin.getKey();
      StoragePluginConfig config = plugin.getValue();
      if (! config.isEnabled()) {
        continue;
      }
      try {
        pluginCache.put(createPluginEntry(name, config, PluginType.STORED));
      } catch (Exception e) {
        logger.error("Failure while setting up StoragePlugin with name: '{}', disabling.", name, e);
        config.setEnabled(false);
        pluginStore.put(name, config);
      }
    }
    // If found at least one entry then this is an existing registry.
    return count > 0;
  }

  @Override
  public void put(String name, StoragePluginConfig config) throws PluginException {
    name = validateName(name);

    // Do not allow overwriting system plugins
    // This same check is done later. However, we want to do this check
    // before writing to the persistent store, which we must do before
    // putting the plugin into the cache (where the second check is done.)
    PluginHandle currentEntry = pluginCache.get(name);
    if (currentEntry != null && currentEntry.isIntrinsic()) {
      throw PluginException.systemPluginException(
          "replace", name);
    }

    // Write to the store. We don't bother to update the cache; we could
    // only update our own cache, not those of other Drillbits. We rely
    // on the cache refresh mechanism to kick in when the Drillbit asks
    // for the plugin instance.
    pluginStore.put(name, config);
  }

  private String validateName(String name) throws PluginException {
    if (name == null) {
      throw new PluginException("Plugin name cannot be null");
    }
    name = name.trim().toLowerCase();
    if (name.isEmpty()) {
      throw new PluginException("Plugin name cannot be null");
    }
    return name;
  }

  @Override
  public void validatedPut(String name, StoragePluginConfig config)
      throws PluginException {

    name = validateName(name);
    PluginHandle oldEntry;
    if (config.isEnabled()) {
      PluginHandle entry = restoreFromEphemeral(name, config);
      try {
        entry.plugin();
      } catch (UserException e) {
        // Provide helpful error messages.
        throw new PluginException(e.getOriginalMessage(), e);
      } catch (Exception e) {
        throw new PluginException(String.format(
            "Invalid plugin config for '%s', "
          + "Please switch to Logs panel from the UI then check the log.", name), e);
      }
      oldEntry = pluginCache.put(entry);
    } else {
      oldEntry = pluginCache.remove(name);
    }
    moveToEphemeral(oldEntry);
    pluginStore.put(name, config);
  }

  @Override
  public void setEnabled(String name, boolean enable) throws PluginException {

    // Works only with the stored config. (Some odd persistent stores do not
    // actually serialize the config; they just cache a copy.) If we change
    // anything, the next request will do a resync to pick up the change.
    name = validateName(name);
    StoragePluginConfig config = requireStoredConfig(name);
    if (config.isEnabled() == enable) {
      return;
    }
    StoragePluginConfig copy = copyConfig(config);
    copy.setEnabled(enable);
    validatedPut(name, copy);
  }

  /**
   * Configs are obtained from the persistent store. This method is
   * called only by the UI to edit a stored plugin; so no benefit to
   * using the cache. We also want a plugin even if it is disabled,
   * and disabled plugins do not reside in the cache.
   * <p>
   * Note that each call (depending on the store implementation)
   * may return a distinct instance of the config. The instance will
   * be equal (unless the stored version changes.) However, other
   * versions of the store may return the same instance as is in
   * the cache. So, <b>do not</b> modify the returned config.
   * To modify the config, call {@link #copyConfig(String)} instead.
   */
  @Override
  public StoragePluginConfig getStoredConfig(String name) {
    return pluginStore.get(name);
  }

  @Override
  public StoragePluginConfig copyConfig(String name) throws PluginException {
    return copyConfig(requireStoredConfig(name));
  }

  private StoragePluginConfig requireStoredConfig(String name) throws PluginException {
    StoragePluginConfig config = getStoredConfig(name);
    if (config == null) {
      throw new PluginNotFoundException(name);
    }
    return config;
  }

  @Override
  public String encode(StoragePluginConfig config) {
    ObjectMapper mapper = context.mapper();
    try {
      return mapper.writer()
          .forType(config.getClass())
          .writeValueAsString(config);
    } catch (IOException e) {
      // We control serialization, so no errors should occur.
      throw new IllegalStateException("Serialize failed", e);
    }
  }

  @Override
  public String encode(String name) throws PluginException {
    return encode(requireStoredConfig(validateName(name)));
  }

  @Override
  public StoragePluginConfig decode(String json) throws PluginEncodingException {

    // We don't control the format of the input JSON, so an
    // error could occur.
    try {
      return context.mapper().reader()
          .forType(StoragePluginConfig.class)
          .readValue(json);
    } catch (InvalidTypeIdException | UnrecognizedPropertyException e) {
      throw new PluginEncodingException(e.getMessage(), e);
    } catch (IOException e) {
      throw new PluginEncodingException("Failure when decoding plugin JSON", e);
    }
  }

  @Override
  public void putJson(String name, String json) throws PluginException {
    validatedPut(name, decode(json));
  }

  @Override
  public StoragePluginConfig copyConfig(StoragePluginConfig orig) {
    try {

      // TODO: Storage plugin configs don't define a "clone" or "copy"
      // method, so use a round-trip to JSON to accomplish the same task.
      return decode(encode(orig));
    } catch (PluginEncodingException e) {
      throw new IllegalStateException("De/serialize failed", e);
    }
  }

  @Override
  public StoragePluginConfig getDefinedConfig(String name) {
    try {
      name = validateName(name);
    } catch (PluginException e) {
      // Name is not valid, so no plugin matches the name.
      return null;
    }
    PluginHandle entry = getEntry(name);
    return entry == null ? null : entry.config();
  }

  // Gets a plugin with the named configuration
  @Override
  public StoragePlugin getPlugin(String name) throws PluginException {
    try {
      name = validateName(name);
    } catch (PluginException e) {
      // Name is not valid, so no plugin matches the name.
      return null;
    }
    PluginHandle entry = getEntry(name);

    // Lazy instantiation: the first call to plugin() creates the
    // actual plugin instance.
    return entry == null ? null : entry.plugin();
  }

  private PluginHandle getEntry(String name) {
    PluginHandle plugin = pluginCache.get(name);
    if (plugin != null && plugin.isIntrinsic()) {
      return plugin;
    }
    StoragePluginConfig config = getStoredConfig(name);
    if (plugin == null) {
      return refresh(name, config);
    } else {
      return refresh(plugin, config);
    }
  }

  // Lazy refresh for a plugin not known on this server.
  private PluginHandle refresh(String name, StoragePluginConfig config) {
    if (config == null || !config.isEnabled()) {
      return null;
    } else {

      // Handles race conditions: some other thread may have just done what
      // we're trying to do. Note: no need to close the new entry if
      // there is a conflict: the plugin instance is created on demand
      // and we've not done so.
      return pluginCache.putIfAbsent(restoreFromEphemeral(name, config));
    }
  }

  // Lazy refresh of a plugin we think we know about.
  private PluginHandle refresh(PluginHandle entry, StoragePluginConfig config) {

    // Deleted or disabled in persistent storage?
    if (config == null || !config.isEnabled()) {

      // Move the old config to the ephemeral store.
      try {
        if (pluginCache.remove(entry.name()) == entry) {
          moveToEphemeral(entry);
        }
        return null;
      } catch (PluginException e) {
        // Should never occur, only if the persistent store where to
        // somehow contain an entry with the same name as a system plugin.
        throw new IllegalStateException("Plugin refresh failed", e);
      }
    }
    // Unchanged?
    if (entry.config().equals(config)) {
      return entry;
    }

    // Plugin changed. Handle race condition on replacement.
    PluginHandle newEntry = restoreFromEphemeral(entry.name(), config);
    try {
      if (pluginCache.replace(entry, newEntry)) {
        moveToEphemeral(entry);
        return newEntry;
      } else {
        return pluginCache.get(entry.name());
      }
    } catch (PluginException e) {
      // Should never occur, only if the persistent store where to
      // somehow contain an entry with the same name as a system plugin.
      throw new IllegalStateException("Plugin refresh failed", e);
    }
  }

  private void refresh() {
    // Iterate through the plugin instances in the persistent store adding
    // any new ones and refreshing those whose configuration has changed
    Iterator<Entry<String, StoragePluginConfig>> allPlugins = pluginStore.load();
    while (allPlugins.hasNext()) {
      Entry<String, StoragePluginConfig> plugin = allPlugins.next();
      refresh(plugin.getKey(), plugin.getValue());
    }
  }

  @Override
  public StoragePlugin getPlugin(StoragePluginConfig config) throws ExecutionSetupException {
    try {
      return getPluginByConfig(config);
    } catch (PluginException e) {
      throw translateException(e);
    }
  }

  private ExecutionSetupException translateException(PluginException e) {
    Throwable cause = e.getCause();
    if (cause != null && cause instanceof ExecutionSetupException) {
      return (ExecutionSetupException) cause;
    }
    return new ExecutionSetupException(e);
  }

  @Override
  public StoragePlugin getPluginByConfig(StoragePluginConfig config) throws PluginException {
    // Try to lookup plugin by configuration
    PluginHandle plugin = pluginCache.get(config);
    if (plugin != null) {
      return plugin.plugin();
    }

    // No named plugin matches the desired configuration, let's create an
    // ephemeral storage plugin (or get one from the cache)
    try {
      return ephemeralPlugins.get(config).plugin();
    } catch (ExecutionException e) {
      Throwable cause = e.getCause();
      if (cause instanceof PluginException) {
        throw (PluginException) cause;
      } else {
        // this shouldn't happen. here for completeness.
        throw new PluginException(
            "Failure while trying to create ephemeral plugin.", cause);
      }
    }
  }

  // This method is not thread-safe: there is no guarantee that the plugin
  // deleted is the same one the user requested: someone else could have deleted
  // the old one and added a new one of the same name.
  // TODO: Fix this
  @Override
  public void remove(String name) throws PluginException {
    name = validateName(name);

    // Removing here allows us to check for system plugins
    moveToEphemeral(pluginCache.remove(name));

    // Must tell store to delete even if not known locally because
    // the store might hold a disabled version
    pluginStore.delete(name);
  }

  /**
   * If there is an ephemeral plugin of this (name, config), pair,
   * transfer that plugin out of ephemeral storage for reuse. Else
   * create a new handle.
   *
   * @param name plugin name
   * @param config plugin config
   * @return a handle for the plugin which may have been retrieved from
   * ephemeral storage
   */
  private PluginHandle restoreFromEphemeral(String name,
      StoragePluginConfig config) {

    // Benign race condition between check and invalidate.
    PluginHandle ephemeralEntry = ephemeralPlugins.getIfPresent(config);
    if (ephemeralEntry == null || !name.equalsIgnoreCase(ephemeralEntry.name())) {
      return createPluginEntry(name, config, PluginType.STORED);
    } else {

      // Transfer the instance to a new handle, then invalidate the
      // cache entry. The transfer ensures that the invalidate will
      // not close the plugin instance
      PluginHandle newHandle = ephemeralEntry.transfer(PluginType.STORED);
      ephemeralPlugins.invalidate(config);
      return newHandle;
    }
  }

  private void moveToEphemeral(PluginHandle handle) {
    if (handle == null) {
      return;
    }

    // No need to move if no instance.
    if (!handle.hasInstance()) {
      return;
    }

    // If already in the ephemeral store, don't replace.
    // Race condition is benign: two threads both doing the put
    // will cause the first handle to be closed when the second hits.
    if (ephemeralPlugins.getIfPresent(handle.config()) == null) {
      ephemeralPlugins.put(handle.config(), handle.transfer(PluginType.EPHEMERAL));
    } else {
      handle.close();
    }
  }

  @Override
  public Map<String, StoragePluginConfig> storedConfigs() {
    return storedConfigs(PluginFilter.ALL);
  }

  @Override
  public Map<String, StoragePluginConfig> storedConfigs(PluginFilter filter) {
    Map<String, StoragePluginConfig> result = new HashMap<>();
    Iterator<Entry<String, StoragePluginConfig>> allPlugins = pluginStore.load();
    while (allPlugins.hasNext()) {
      Entry<String, StoragePluginConfig> plugin = allPlugins.next();
      boolean include;
      switch (filter) {
      case ENABLED:
        include = plugin.getValue().isEnabled();
        break;
      case DISABLED:
        include = !plugin.getValue().isEnabled();
        break;
      default:
        include = true;
      }
      if (include) {
        result.put(plugin.getKey(), plugin.getValue());
      }
    }
    return result;
  }

  @Override
  public Map<String, StoragePluginConfig> enabledConfigs() {
    refresh();
    Map<String, StoragePluginConfig> result = new HashMap<>();
    for (PluginHandle entry : pluginCache) {
      if (entry.isStored()) {
        result.put(entry.name(), entry.config());
      }
    }
    return result;
  }

  @Override
  public void putFormatPlugin(String pluginName, String formatName,
      FormatPluginConfig formatConfig) throws PluginException {
    pluginName = validateName(pluginName);
    formatName = validateName(formatName);
    StoragePluginConfig orig = requireStoredConfig(pluginName);
    if (!(orig instanceof FileSystemConfig)) {
      throw new PluginException(
        "Format plugins can be added only to the file system plugin: " + pluginName);
    }
    FileSystemConfig copy = (FileSystemConfig) copyConfig(orig);
    if (formatConfig == null) {
      copy.getFormats().remove(formatName);
    } else {
      copy.getFormats().put(formatName, formatConfig);
    }
    put(pluginName, copy);
  }

  @Override
  public FormatPlugin getFormatPluginByConfig(StoragePluginConfig storageConfig,
      FormatPluginConfig formatConfig) throws PluginException {
    StoragePlugin storagePlugin = getPluginByConfig(storageConfig);
    return storagePlugin.getFormatPlugin(formatConfig);
  }

  @Override
  public FormatPlugin getFormatPlugin(StoragePluginConfig storageConfig,
      FormatPluginConfig formatConfig) throws ExecutionSetupException {
    try {
      return getFormatPluginByConfig(storageConfig, formatConfig);
    } catch (PluginException e) {
      throw translateException(e);
    }
  }

  @Override
  public SchemaFactory getSchemaFactory() {
    return schemaFactory;
  }

  // TODO: Remove this: it will force plugins to be instantiated
  // unnecessarily
  // This is a bit of a hack. The planner calls this to get rules
  // for queries. If even one plugin has issues, then all queries
  // will fails, even those that don't use the invalid plugin.
  //
  // This hack may result in a delay (such as a timeout) again and
  // again as each query tries to create the plugin. The solution is
  // to disable the plugin, or fix the external system. This solution
  // is more stable than, say, marking the plugin failed since we have
  // no way to show or reset failed plugins.
  private static class PluginIterator implements Iterator<Entry<String, StoragePlugin>> {
    private final Iterator<PluginHandle> base;
    private PluginHandle entry;

    public PluginIterator(Iterator<PluginHandle> base) {
      this.base = base;
    }

    @Override
    public boolean hasNext() {
      while (base.hasNext()) {
        entry = base.next();
        try {
          entry.plugin();
          return true;
        } catch (Exception e) {
          // Skip this one to avoid failing the query
        }
      }
      return false;
    }

    @Override
    public Entry<String, StoragePlugin> next() {
      return new ImmutableEntry<>(entry.name(), entry.plugin());
    }
  }

  @Override
  public Iterator<Entry<String, StoragePlugin>> iterator() {
    refresh();
    return new PluginIterator(pluginCache.iterator());
  }

  @Override
  public synchronized void close() throws Exception {
    ephemeralPlugins.invalidateAll();
    pluginCache.close();
    pluginStore.close();
    locators.stream().forEach(loc -> loc.close());
  }

  /**
   * Creates plugin entry with the given {@code name} and configuration {@code pluginConfig}.
   * Validation for existence, disabled, etc. should have been done by the caller.
   * <p>
   * Uses the config to find the connector, then lets the connector create the plugin
   * entry. Creation of the plugin instance is deferred until first requested.
   * This should speed up Drillbit start, as long as other code only asks for the
   * plugin instance when it is actually needed to plan or execute a query (not just
   * to provide a schema.)
   *
   * @param name name of the plugin
   * @param pluginConfig plugin configuration
   * @return handle the the plugin with metadata and deferred access to
   * the plugin instance
   */
  private PluginHandle createPluginEntry(String name, StoragePluginConfig pluginConfig, PluginType type) {
    ConnectorHandle connector = connectors.get(pluginConfig.getClass());
    if (connector == null) {
      throw UserException.internalError()
        .message("No connector known for plugin configuration")
        .addContext("Plugin name", name)
        .addContext("Config class", pluginConfig.getClass().getName())
        .build(logger);
    }
    return connector.pluginEntryFor(name, pluginConfig, type);
  }

  @Override
  public ObjectMapper mapper() {
    return context.mapper();
  }

  @Override
  public <T extends StoragePlugin> T resolve(
      StoragePluginConfig storageConfig, Class<T> desired) {
    try {
      return desired.cast(getPluginByConfig(storageConfig));
    } catch (PluginException|ClassCastException e) {
      // Should never occur
      throw new IllegalStateException(String.format(
          "Unable to load stroage plugin %s for provided config " +
          "class %s", desired.getName(),
          storageConfig.getClass().getName()), e);
    }
  }

  @Override
  public <T extends FormatPlugin> T resolveFormat(
      StoragePluginConfig storageConfig,
      FormatPluginConfig formatConfig, Class<T> desired) {
    try {
      return desired.cast(getFormatPluginByConfig(storageConfig, formatConfig));
    } catch (PluginException|ClassCastException e) {
      // Should never occur
      throw new IllegalStateException(String.format(
          "Unable to load format plugin %s for provided plugin " +
          "config class %s and format config class %s",
          desired.getName(),
          storageConfig.getClass().getName(),
          formatConfig.getClass().getName()), e);
    }
  }

  @Override
  public Set<String> availablePlugins() {
    refresh();
    return pluginCache.names();
  }
}
