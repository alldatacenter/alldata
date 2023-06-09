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
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.apache.drill.common.config.ConfigConstants;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.util.ActionOnFile;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loads the set of bootstrap plugin configurations for new systems.
 * Also handles upgrades to new and existing systems. Each set is given
 * by a Jackson-serialized JSON file with the list of plugin (configurations).
 * <p>
 * The files are given via indirections in the config system to allow easier
 * testing when "non-stock" configurations.
 *
 * <h4>Bootstrap Files</h4>
 *
 * When a new system starts (boots), Drill populates the persistent store
 * with a "starter" (bootstrap) set of storage plugin configurations as
 * provided by each connector locator. The classic locator uses one or
 * more bootstrap files, serialized to JSON, and visible at the root of the
 * class path. Each plugin implementation, if packaged in its own project,
 * can provide a bootstrap file ({@code bootstrap-storage-plugins.json}).
 * <p>
 * The core plugins share a single file in the exec module which has a list of
 * storage plugins used as default on fresh start up. Then we have contrib module
 * where some plugins and formats reside. We want them to be included in default
 * plugins as well during fresh start up which is done by reading each bootstrap
 * file in the classpath.
 *
 * <h4>Format Plugin Bootstrap</h4>
 *
 * All works fine for the plugins, since each plugin also has a
 * {@code bootstrap-storage-plugins.json} loaded via this process. However, format
 * plugins are defined <i>inside</i> storage plugins. This presents a problem when
 * the format is defined outside of the core (such as in the contrib module). In
 * this case, when new format was added, the developer had to modify the
 * {@code bootstrap-storage-plugins.json} in exec module to ensure that the new format
 * was included.
 * <p>
 * The {@code bootstrap-format-plugins.json} solves this problem: it allows non-core
 * modules to include formats during bootstrap. The file must still indicate which
 * plugin (config) to modify, and must list the same format for each supported file
 * system plugin such as {@code dfs} and {@ cp}. Despite this limitation, the additional
 * file makes the non-core format plugins independent of the core files.
 * <p>
 * The resulting bootstrap process is to 1) gather the storage plugins, and 2)
 * merge any format bootstrap into these plugins.
 *
 * <h4>Upgrade Files</h4>
 *
 * Bootstrap files populate a new system. There are times when a user upgrades
 * their Drill install and we would like to push out configurations for newly
 * added plugins. This is done via the optional upgrade file.
 * <p>
 * If the system is new, configurations from the upgrade file replace
 * (override) values from the bootstrap file. The net effect, bootstrap plus
 * upgrades, are written into the persistent store.
 * <p>
 * If the system is upgraded, then the current technique uses a highly
 * unreliable system: an upgrade file exists. Any upgrades are applied on top
 * of the user's stored values. The upgrade file is then deleted to avoid
 * applying the same upgrades multiple times. Not idea, but it is what it
 * is for now.
 *
 * <h4>Format Plugins</h4>
 *
 * Earlier versions of this code provided support for format plugins. However,
 * this code never worked. The original design of Drill has format plugins
 * as a set of attributes of the file system plugin config. Additional work
 * is needed to allow file system configs to share plugins. Such work is a
 * good idea, so the ill-fated format plugin code still appears, but does
 * nothing.
 *
 * <h4>Config Properties</h4>
 *
 * <dl>
 * <dt>{@code ExecConstants.BOOTSTRAP_STORAGE_PLUGINS_FILE}
 * ({@code drill.exec.storage.bootstrap.storage})</dt>
 * <dd>The name of the bootstrap file, normally
 * {@code bootstrap-storage-plugins.json}.</dd>
 * <dt>{@code ExecConstants.UPGRADE_STORAGE_PLUGINS_FILE}
 * ({@code drill.exec.storage.upgrade.storage})</dt>
 * <dd>The name of the upgrade file, normally
 * {@code storage-plugins-override.conf}. Unlike th bootstrap
 * file, only one upgrade file can appear in the class path.
 * The upgrade file is optional.</dd>
 * </dl>
 */
public class PluginBootstrapLoaderImpl implements PluginBootstrapLoader {
  private static final Logger logger = LoggerFactory.getLogger(PluginBootstrapLoaderImpl.class);

  private final PluginRegistryContext context;

  /**
   * The file read the first time a Drillbit starts up, and deleted
   * afterwards. A poor-man's way of handling upgrades. Will be
   * non-null when an upgrade is needed, null thereafter until a
   * new release.
   */
  private URL pluginsOverrideFileUrl;

  public PluginBootstrapLoaderImpl(PluginRegistryContext context) {
    this.context = context;
  }

  @Override
  public StoragePlugins bootstrapPlugins() throws IOException {
    Map<String, URL> pluginURLMap = new HashMap<>();
    StoragePlugins bootstrapPlugins = loadBootstrapPlugins(pluginURLMap);

    // Upgrade the bootstrap plugins with any updates. Seems odd,
    // but this is how Drill 1.17 works, so keeping the code.
    // Uses the enabled status from the updates if different than
    // the bootstrap version.
    StoragePlugins updatedPlugins = updatedPlugins();
    if (updatedPlugins != null) {
      bootstrapPlugins.putAll(updatedPlugins);
    }
    applyFormatPlugins(bootstrapPlugins, pluginURLMap);
    return bootstrapPlugins;
  }

  @VisibleForTesting
  protected StoragePlugins loadBootstrapPlugins(Map<String, URL> pluginURLMap) throws IOException {
    // bootstrap load the config since no plugins are stored.
    String storageBootstrapFileName = context.config().getString(
        ExecConstants.BOOTSTRAP_STORAGE_PLUGINS_FILE);
    Set<URL> storageUrls = ClassPathScanner.forResource(storageBootstrapFileName, false);
    if (storageUrls == null || storageUrls.isEmpty()) {
      throw new IOException("Cannot find storage plugin boostrap file: " + storageBootstrapFileName);
    }
    logger.info("Loading the storage plugin configs from URLs {}.", storageUrls);
    StoragePlugins bootstrapPlugins = new StoragePlugins();
    for (URL url : storageUrls) {
      try {
        loadStoragePlugins(url, bootstrapPlugins, pluginURLMap);
      } catch (IOException e) {
        throw new IOException("Failed to load bootstrap plugins from " + url.toString(), e );
      }
    }

    return bootstrapPlugins;
  }

  /**
   * Given a set of bootstrap plugins, updated selected plugins with the list
   * of format plugins. Since the plugins do not yet reside in the registry,
   * they can be modified in place without making copies.
   * <p>
   * Only one module (core) should have defined the file system plugin
   * configs. Only one module should define each format plugin. As a result,
   * order should not matter when applying the format configs.
   * @throws IOException for load failures
   */
  private void applyFormatPlugins(StoragePlugins bootstrapPlugins,
      Map<String, URL> pluginURLMap) throws IOException {
    String formatBootstrapFileName = context.config().getString(
        ExecConstants.BOOTSTRAP_FORMAT_PLUGINS_FILE);
    Set<URL> formatUrls = ClassPathScanner.forResource(formatBootstrapFileName, false);
    if (formatUrls == null) {
      return;
    }
    for (URL url : formatUrls) {
      logger.info("Loading format plugin configs from {}.", url);
      loadFormatPlugins(url, bootstrapPlugins, pluginURLMap);
    }
  }

  /**
   * Get the new storage plugins from the
   * {@link ConfigConstants#STORAGE_PLUGINS_OVERRIDE_CONF} file if it exists,
   * null otherwise
   */
  @Override
  public StoragePlugins updatedPlugins() {
    String upgradeFileName = context.config().getString(
        ExecConstants.UPGRADE_STORAGE_PLUGINS_FILE);
    Set<URL> urlSet = ClassPathScanner.forResource(upgradeFileName, false);
    if (urlSet.isEmpty()) {
      logger.trace(
          "The {} file is absent. Proceed without updating of the storage plugins configs",
          upgradeFileName);
      return null;
    }
    if (urlSet.size() != 1) {
      throw DrillRuntimeException.create(
          "More than one %s file is placed in Drill's classpath: %s",
          upgradeFileName, urlSet);
    }
    pluginsOverrideFileUrl = urlSet.iterator().next();
    try (InputStream is = pluginsOverrideFileUrl.openStream();) {
      return context.hoconMapper().readValue(is, StoragePlugins.class);
    } catch (IOException e) {
      logger.error("Failures are obtained while loading file: '{}'. Proceeding without update.",
          upgradeFileName, e);
      return null;
    }
  }

  @Override
  public void onUpgrade() {
    if (pluginsOverrideFileUrl == null) {
      return;
    }
    String fileAction = context.config().getString(
        ExecConstants.ACTION_ON_STORAGE_PLUGINS_OVERRIDE_FILE);
    Optional<ActionOnFile> actionOnFile = Arrays.stream(ActionOnFile.values())
        .filter(action -> action.name().equalsIgnoreCase(fileAction))
        .findFirst();
    actionOnFile.ifPresent(action -> action.action(pluginsOverrideFileUrl));
    // TODO: replace with ifPresentOrElse() once the project will be on Java9
    if (!actionOnFile.isPresent()) {
      logger.warn("Unknown value {} for {} boot option. Nothing will be done with file.",
          fileAction, ExecConstants.ACTION_ON_STORAGE_PLUGINS_OVERRIDE_FILE);
    }
  }

  /**
   * Loads storage plugins from the given URL
   *
   * @param url
   *          URL to the storage plugins bootstrap file
   * @param bootstrapPlugins
   *          a collection where the plugins should be loaded to
   * @param pluginURLMap
   *          a map to store correspondence between storage plugins and
   *          bootstrap files in which they are defined. Used for logging
   * @throws IOException
   *           if failed to retrieve a plugin from a bootstrap file
   */
  private void loadStoragePlugins(URL url, StoragePlugins bootstrapPlugins,
      Map<String, URL> pluginURLMap) throws IOException {
    StoragePlugins plugins = getPluginsFromResource(url);
    plugins.forEach(plugin -> {
      StoragePluginConfig oldPluginConfig = bootstrapPlugins.putIfAbsent(plugin.getKey(), plugin.getValue());
      if (oldPluginConfig != null) {
        logger.warn("Duplicate plugin instance '[{}]' defined in [{}, {}], ignoring the later one.",
            plugin.getKey(), pluginURLMap.get(plugin.getKey()), url);
      } else {
        pluginURLMap.put(plugin.getKey(), url);
      }
    });
  }

  /**
   * Loads format plugins from the given URL and adds the formats to the
   * specified storage plugins
   *
   * @param url
   *          URL to the format plugins bootstrap file
   * @param bootstrapPlugins
   *          a collection with loaded storage plugins. New formats will be
   *          added to them
   * @param pluginURLMap
   *          a map to store correspondence between storage plugins and
   *          bootstrap files in which they are defined. Used for logging
   * @throws IOException
   *           if failed to retrieve a plugin from a bootstrap file
   */
  private void loadFormatPlugins(URL url, StoragePlugins bootstrapPlugins,
      Map<String, URL> pluginURLMap) throws IOException {
    StoragePlugins plugins = getPluginsFromResource(url);
    for (Entry<String, StoragePluginConfig> sourceEntry : plugins) {
      String pluginName = sourceEntry.getKey();
      StoragePluginConfig sourcePlugin = sourceEntry.getValue();
      if (!(sourcePlugin instanceof FileSystemConfig)) {
        logger.warn("Formats are only supported by File System plugins. Source name '{}' is of type '{}'.",
            pluginName, sourcePlugin.getClass().getName());
        continue;
      }
      StoragePluginConfig targetPlugin = bootstrapPlugins.getConfig(pluginName);
      if (targetPlugin == null) {
        logger.warn("No boostrap storage plugin matches the name '{}'", pluginName);
        continue;
      }
      if (!(targetPlugin instanceof FileSystemConfig)) {
        logger.warn("Formats are only supported by File System plugins. Source name '{}' " +
            "is of type '{}' but the bootstrap plugin of that name is of type '{}.",
            pluginName, sourcePlugin.getClass().getName(),
            targetPlugin.getClass().getName());
        continue;
      }
      FileSystemConfig targetFsConfig = (FileSystemConfig) targetPlugin;
      FileSystemConfig sourceFsConfig = (FileSystemConfig) sourcePlugin;
      sourceFsConfig.getFormats().forEach((formatName, formatValue) -> {
        FormatPluginConfig oldPluginConfig = targetFsConfig.getFormats().putIfAbsent(formatName, formatValue);
        if (oldPluginConfig != null) {
          logger.warn("Duplicate format instance '{}' defined in '{}' and '{}', ignoring the later one.",
              formatName, pluginURLMap.get(pluginName), url);
        }
      });
    }
  }

  private StoragePlugins getPluginsFromResource(URL resource) throws IOException {
    String pluginsData = Resources.toString(resource, Charsets.UTF_8);
    return context.hoconMapper().readValue(pluginsData, StoragePlugins.class);
  }
}
