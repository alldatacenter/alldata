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
package org.apache.drill.exec.store.dfs;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.util.ConstructorChecker;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.hadoop.conf.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Responsible for instantiating format plugins
 */
public class FormatCreator {
  private static final Logger logger = LoggerFactory.getLogger(FormatCreator.class);

  private static final ConstructorChecker FORMAT_BASED = new ConstructorChecker(String.class, DrillbitContext.class,
      Configuration.class, StoragePluginConfig.class, FormatPluginConfig.class);
  private static final ConstructorChecker DEFAULT_BASED = new ConstructorChecker(String.class, DrillbitContext.class,
      Configuration.class, StoragePluginConfig.class);

  /**
   * Returns a Map from the FormatPlugin Config class to the constructor of the format plugin that accepts it.
   * This is used to create a format plugin instance from its configuration.
   * @param pluginClasses the FormatPlugin classes to index on their config class
   * @return a map of type to constructor that taks the config
   */
  private static Map<Class<?>, Constructor<?>> initConfigConstructors(Collection<Class<? extends FormatPlugin>> pluginClasses) {
    Map<Class<?>, Constructor<?>> constructors = new HashMap<>();
    for (Class<? extends FormatPlugin> pluginClass: pluginClasses) {
      for (Constructor<?> c : pluginClass.getConstructors()) {
        try {
          if (!FORMAT_BASED.check(c)) {
            continue;
          }
          Class<?> configClass = c.getParameterTypes()[4];
          constructors.put(configClass, c);
        } catch (Exception e) {
          logger.warn(String.format("Failure while trying instantiate FormatPlugin %s.", pluginClass.getName()), e);
        }
      }
    }
    return constructors;
  }


  private final DrillbitContext context;
  private final Configuration fsConf;
  private final FileSystemConfig storageConfig;

  /** format plugins initialized from the drill config, indexed by name */
  private final Map<String, FormatPlugin> configuredPlugins;
  /** The format plugin classes retrieved from classpath scanning */
  private final Collection<Class<? extends FormatPlugin>> pluginClasses;
  /** a Map from the FormatPlugin Config class to the constructor of the format plugin that accepts it.*/
  private final Map<Class<?>, Constructor<?>> configConstructors;

  FormatCreator(
      DrillbitContext context,
      Configuration fsConf,
      FileSystemConfig storageConfig) {
    this.context = context;
    this.fsConf = fsConf;
    this.storageConfig = storageConfig;
    this.pluginClasses = context.getClasspathScan().getImplementations(FormatPlugin.class);
    this.configConstructors = initConfigConstructors(pluginClasses);

    Map<String, FormatPlugin> plugins = new HashMap<>();
    if (storageConfig.getFormats() == null || storageConfig.getFormats().isEmpty()) {
      for (Class<? extends FormatPlugin> pluginClass: pluginClasses) {
        for (Constructor<?> c : pluginClass.getConstructors()) {
          try {
            if (!DEFAULT_BASED.check(c)) {
              continue;
            }
            FormatPlugin plugin = (FormatPlugin) c.newInstance(null, context, fsConf, storageConfig);
            plugins.put(plugin.getName(), plugin);
          } catch (Exception e) {
            logger.warn(String.format("Failure while trying instantiate FormatPlugin %s.", pluginClass.getName()), e);
          }
        }
      }
    } else {
      for (Map.Entry<String, FormatPluginConfig> e : storageConfig.getFormats().entrySet()) {
        Constructor<?> c = configConstructors.get(e.getValue().getClass());
        if (c == null) {
          logger.warn("Unable to find constructor for storage config named '{}' of type '{}'.", e.getKey(), e.getValue().getClass().getName());
          continue;
        }
        try {
          plugins.put(e.getKey(), (FormatPlugin) c.newInstance(e.getKey(), context, fsConf, storageConfig, e.getValue()));
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
          logger.warn("Failure initializing storage config named '{}' of type '{}'.", e.getKey(), e.getValue().getClass().getName(), e1);
        }
      }
    }
    this.configuredPlugins = Collections.unmodifiableMap(plugins);
  }

  /**
   * @param name the name of the formatplugin instance in the drill config
   * @return The configured FormatPlugin for this name
   */
  FormatPlugin getFormatPluginByName(String name) {
    return configuredPlugins.get(name);
  }

  /**
   * @return all the format plugins from the Drill config
   */
  Collection<FormatPlugin> getConfiguredFormatPlugins() {
    return configuredPlugins.values();
  }

  /**
   * Instantiate a new format plugin instance from the provided config object
   * @param fpconfig the conf for the plugin
   * @return the newly created instance of a FormatPlugin based on provided config
   */
  FormatPlugin newFormatPlugin(FormatPluginConfig fpconfig) {
    Constructor<?> c = configConstructors.get(fpconfig.getClass());
    if (c == null) {
      throw UserException.dataReadError()
        .message(
            "Unable to find constructor for storage config of type %s",
            fpconfig.getClass().getName())
        .build(logger);
    }
    try {
      return (FormatPlugin) c.newInstance(null, context, fsConf, storageConfig, fpconfig);
    } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
      throw UserException.dataReadError(e)
        .message(
            "Failure initializing storage config of type %s",
            fpconfig.getClass().getName())
        .build(logger);
    }
  }
}
