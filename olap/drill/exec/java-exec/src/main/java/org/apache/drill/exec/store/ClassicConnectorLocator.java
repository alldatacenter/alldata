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

import static org.apache.drill.shaded.guava.com.google.common.base.Preconditions.checkNotNull;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plugin locator for the "classic" class-path method of locating connectors.
 * Connectors appear somewhere on the class path, and are subclasses of
 * {@link StoragePlugin}. The connector and its configuration class must
 * reside in Drill's default class loader.
 * <p>
 * Handles "classic" storage plugin classes which ship with Drill, or are
 * added to Drill's class path.
 * <p>
 * The plugin registry supports access from multiple threads concurrently.
 * This locator is therefore immutable after the {@code init()} call.
 * The set of plugin instances and configurations (managed by the registry)
 * changes during a run, but the set of known plugin classes is fixed.
 * <p>
 * We sometimes need to add special plugins for testing. Since we cannot
 * add them on the fly, we must add them at (test) startup time via
 * config properties.
 *
 * <h4>Plugin Implementations</h4>
 *
 * This class manages plugin implementation classes (AKA "connectors")
 * which must derive from {@code StoragePlugin}. Each must be configured
 * via a class derived from {@code StoragePluginConfig}. No two connectors
 * can share a configuration class. Each connector must have exactly one
 * associated configuration. (Actually, a single connector might handle
 * multiple configurations, but that seems an obscure use case.)
 * <p>
 * The constructor of the connector associates the two classes, and must
 * be of the form:<pre><code>
 * public SomePlugin(SomePluginConfig config,
 *                   DrillbitContext context,
 *                   String pluginName) {</code></pre>
 * <p>
 * Classes must be on the class path. Drill often caches the class path:
 * creating it at build time, then storing it in a file, to be read at
 * run time. If you are developing a plugin in an IDE, and this class
 * refuses to find the plugin, you can temporarly force a runtime
 * class path scan via setting the
 * {@code ClassPathScanner.IMPLEMENTATIONS_SCAN_CACHE} config property
 * to {@code false}. The various test "fixtures" provide an easy way
 * to set config properties per-test.
 * <p>
 * This locator ignores four categories of {@code StoragePluginConfig}
 * classes:
 * <ul>
 * <li>Interfaces or abstract classes</li>
 * <li>Those that do not have the constructor described above.</li>
 * <li>System plugins with the {@code SystemPlugin} annotation.</li>
 * <li>Private test plugins with the {@code PrivatePlugin}
 * annotation.</li>
 * </ul>
 *
 * <h4>Config Properties</h4>
 *
 * <dl>
 * <dt>{@code ExecConstants.PRIVATE_CONNECTORS}
 * ({@code drill.exec.storage.private_connectors})</dt>
 * <dd>An optional list of private plugin class names. Private plugins
 * are valid instances of {@code StoragePlugin} which have the
 * {@code PrivatePlugin} annotation and so are not automatically
 * loaded.</dd>
 * </dl>
 */
public class ClassicConnectorLocator implements ConnectorLocator {
  private static final Logger logger = LoggerFactory.getLogger(ClassicConnectorLocator.class);

  private final PluginRegistryContext context;
  // Here "plugin" means storage plugin class
  private final Map<Class<? extends StoragePluginConfig>,
                    Constructor<? extends StoragePlugin>> availablePlugins = new IdentityHashMap<>();
  private PluginBootstrapLoader bootstrapLoader;

  public ClassicConnectorLocator(PluginRegistryContext context) {
    this.context = checkNotNull(context);
    this.bootstrapLoader = new PluginBootstrapLoaderImpl(context);
  }

  // TODO: Provide a list of plugin classes to avoid doing
  // the scan twice.
  @Override
  public void init() {

    // Build the list of all available storage plugin class constructors.
    final Collection<Class<? extends StoragePlugin>> pluginClasses =
        context.classpathScan().getImplementations(StoragePlugin.class);
    final String lineBrokenList =
        pluginClasses.size() == 0
            ? "" : "\n\t- " + Joiner.on("\n\t- ").join(pluginClasses);
    logger.debug("Found {} storage plugin configuration classes: {}.",
        pluginClasses.size(), lineBrokenList);
    for (Class<? extends StoragePlugin> plugin : pluginClasses) {

      // Skip system and private plugins
      if (!plugin.isAnnotationPresent(SystemPlugin.class) &&
          !plugin.isAnnotationPresent(PrivatePlugin.class)) {
        registerPlugin(plugin);
      }
    }

    // Any private connectors to load?
    // Expected to be in the same class loader as this class.
    DrillConfig config = context.config();
    if (config.hasPath(ExecConstants.PRIVATE_CONNECTORS)) {
      List<String> privateConfigs = config.getStringList(ExecConstants.PRIVATE_CONNECTORS);
      for (String privateName : privateConfigs) {
        registerPrivatePlugin(privateName);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void registerPrivatePlugin(String pluginName) {
    Class<?> pluginClass;
    try {
      ClassLoader cl = getClass().getClassLoader();
      pluginClass = cl.loadClass(pluginName);
    } catch (ClassNotFoundException e) {
      throw new IllegalArgumentException("Private plugin class not found: " + pluginName, e);
    }
    if (!StoragePlugin.class.isAssignableFrom(pluginClass)) {
      throw new IllegalArgumentException("Private plugin class does not extend StoragePlugin: " + pluginName);
    }
    if (!registerPlugin((Class<? extends StoragePlugin>) pluginClass)) {
      throw new IllegalArgumentException("Private plugin class not valid, see logs: " + pluginName);
    }
  }

  private boolean registerPlugin(Class<? extends StoragePlugin> plugin) {
    Map<Class<? extends StoragePluginConfig>, Constructor<? extends StoragePlugin>> ctors = constuctorsFor(plugin);
    if (ctors.isEmpty()) {
      logger.debug("Skipping registration of StoragePlugin {} as it doesn't have a constructor with the parameters "
          + "of (StoragePluginConfig, Config)", plugin.getCanonicalName());
      return false;
    } else {
      for (Entry<Class<? extends StoragePluginConfig>, Constructor<? extends StoragePlugin>> ctor : ctors.entrySet()) {
        if (availablePlugins.containsKey(ctor.getKey())) {
          logger.warn(String.format("Two storage plugins cannot use the same config class. " +
              "Found conflict %s and %s both use %s. Only the first added to registry.",
              availablePlugins.get(ctor.getKey()).getDeclaringClass().getName(),
              ctor.getValue().getDeclaringClass().getName(),
              ctor.getKey().getName()));
        } else {
          availablePlugins.put(ctor.getKey(), ctor.getValue());
        }
      }
      return true;
    }
  }

  @SuppressWarnings("unchecked")
  public static Map<Class<? extends StoragePluginConfig>, Constructor<? extends StoragePlugin>>
      constuctorsFor(Class<? extends StoragePlugin> plugin) {
    Map<Class<? extends StoragePluginConfig>, Constructor<? extends StoragePlugin>> ctors = new IdentityHashMap<>();
    for (Constructor<?> c : plugin.getConstructors()) {
      Class<?>[] params = c.getParameterTypes();
      if (params.length != 3
          || !StoragePluginConfig.class.isAssignableFrom(params[0])
          || params[1] != DrillbitContext.class
          || params[2] != String.class) {
        logger.debug("Skipping StoragePlugin constructor {} for plugin class {} since it doesn't implement a "
            + "constructor(StoragePluginConfig, DrillbitContext, String)", c, plugin);
        continue;
      }
      Class<? extends StoragePluginConfig> configClass = (Class<? extends StoragePluginConfig>) params[0];
      ctors.put(configClass, (Constructor<? extends StoragePlugin>) c);
    }
    return ctors;
  }

  @Override
  @VisibleForTesting
  public Set<Class<? extends StoragePluginConfig>> configClasses() {
    return availablePlugins.keySet();
  }

  /**
   * Classic storage plugins do not provide default configurations.
   */
  @Override
  public StoragePlugin get(String name) {
    return null;
  }

  @Override
  public List<StoragePlugin> intrinsicPlugins() {
    return null;
  }

  /**
   * Read bootstrap storage plugins
   * {@link ExecConstants#BOOTSTRAP_STORAGE_PLUGINS_FILE} and format plugins
   * {@link ExecConstants#BOOTSTRAP_FORMAT_PLUGINS_FILE} files for the first
   * fresh install of Drill.
   *
   * @return bootstrap storage plugins
   * @throws IOException if a read error occurs
   */
  @Override
  public StoragePlugins bootstrapPlugins() throws IOException {
    return bootstrapLoader.bootstrapPlugins();
  }

  @Override
  public StoragePlugins updatedPlugins() {
    return bootstrapLoader.updatedPlugins();
  }

  @Override
  public void onUpgrade() {
    bootstrapLoader.onUpgrade();
    bootstrapLoader = null;
  }

  /**
   * Creates plugin instance with the given {@code name} and configuration {@code pluginConfig}.
   * The plugin need to be present in a list of available plugins and be enabled in the configuration
   *
   * @param name name of the plugin
   * @param pluginConfig plugin configuration
   * @return plugin client or {@code null} if plugin is disabled
   */
  @Override
  public StoragePlugin create(String name, StoragePluginConfig pluginConfig) throws ExecutionSetupException {
    StoragePlugin plugin;
    Constructor<? extends StoragePlugin> constructor = availablePlugins.get(pluginConfig.getClass());
    if (constructor == null) {
      throw new ExecutionSetupException(String.format("Failure finding StoragePlugin constructor for config %s",
          pluginConfig.getClass().getName()));
    }
    try {
      plugin = constructor.newInstance(pluginConfig, context.drillbitContext(), name);
      plugin.start();
      return plugin;
    } catch (ReflectiveOperationException | IOException e) {
      Throwable t = e instanceof InvocationTargetException ? ((InvocationTargetException) e).getTargetException() : e;
      if (t instanceof ExecutionSetupException) {
        throw ((ExecutionSetupException) t);
      }
      throw new ExecutionSetupException(String.format(
          "Failure setting up new storage plugin configuration for config %s",
          pluginConfig.getClass().getSimpleName()), t);
    }
  }

  @Override
  public boolean storable() {
    return true;
  }

  @Override
  public Class<? extends StoragePlugin> connectorClassFor(
      Class<? extends StoragePluginConfig> configClass) {
    Constructor<? extends StoragePlugin> constructor =
        availablePlugins.get(configClass);
    return constructor == null ? null : constructor.getDeclaringClass();
  }

  @Override
  public void close() { }
}
