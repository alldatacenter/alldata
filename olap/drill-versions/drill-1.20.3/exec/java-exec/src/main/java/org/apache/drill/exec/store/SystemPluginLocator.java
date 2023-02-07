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
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.common.scanner.persistence.AnnotatedClassDescriptor;
import org.apache.drill.exec.planner.logical.StoragePlugins;
import org.apache.drill.exec.server.DrillbitContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Locates system storage plugins. These are special in that they take
 * no configuration: the configuration and connector names are the same so
 * that the configuration name can be resolved directly to the connector.
 * <p>
 * System plugins are defined in the Drill core, reside on the default class
 * path, and are annotated with {@code @SystemPlugin}.
 */
public class SystemPluginLocator implements ConnectorLocator {
  private static final Logger logger = LoggerFactory.getLogger(SystemPluginLocator.class);

  private final PluginRegistryContext context;
  private final Map<String, StoragePlugin> plugins = CaseInsensitiveMap.newHashMap();

  public SystemPluginLocator(PluginRegistryContext context) {
    this.context = context;
  }

  /**
   * Dynamically loads system plugins annotated with {@link SystemPlugin}.
   * Will skip plugin initialization if no matching constructor, incorrect
   * class implementation, name absence are detected.
   *
   * @return map with system plugins stored by name
   */
  @Override
  public void init() {
    List<AnnotatedClassDescriptor> annotatedClasses =
        context.classpathScan().getAnnotatedClasses(SystemPlugin.class.getName());
    logger.trace("Found {} annotated classes with SystemPlugin annotation: {}.",
        annotatedClasses.size(), annotatedClasses);

    for (AnnotatedClassDescriptor annotatedClass : annotatedClasses) {
      try {
        loadPlugin(annotatedClass);
      } catch (ReflectiveOperationException e) {
        logger.warn("Error during system plugin {} initialization. Plugin initialization will be skipped.",
            annotatedClass.getClassName(), e);
      }
    }
    logger.trace("The following system plugins have been initialized: {}.", plugins.keySet());
  }

  private void loadPlugin(AnnotatedClassDescriptor annotatedClass) throws ReflectiveOperationException {
    Class<?> aClass = Class.forName(annotatedClass.getClassName());

    for (Constructor<?> constructor : aClass.getConstructors()) {
      Class<?>[] parameterTypes = constructor.getParameterTypes();

      if (parameterTypes.length != 1 || parameterTypes[0] != DrillbitContext.class) {
        logger.trace("Not matching constructor for {}. Expecting constructor with one parameter for DrillbitContext class.",
            annotatedClass.getClassName());
        continue;
      }

      Object instance = constructor.newInstance(context.drillbitContext());
      if (!(instance instanceof StoragePlugin)) {
        logger.debug("Created instance of {} does not implement StoragePlugin interface.", annotatedClass.getClassName());
        continue;
      }

      StoragePlugin storagePlugin = (StoragePlugin) instance;
      String name = storagePlugin.getName();
      if (name == null) {
        logger.debug("Storage plugin name {} is not defined. Skipping plugin initialization.", annotatedClass.getClassName());
        continue;
      }
      storagePlugin.getConfig().setEnabled(true);
      plugins.put(name, storagePlugin);
      return;
    }
    logger.debug("Skipping plugin registration for {}, did not find matching constructor or initialized object of wrong type.",
        aClass.getName());
  }

  @Override
  public StoragePlugins bootstrapPlugins() throws IOException {
    // System plugins are not stored, so no bootstrap
    return null;
  }

  @Override
  public StoragePlugins updatedPlugins() {
    // ... and no upgrades
    return null;
  }

  @Override
  public void onUpgrade() { }

  @Override
  public StoragePlugin get(String name) {
    return plugins.get(name);
  }

  @Override
  public Collection<StoragePlugin> intrinsicPlugins() {
    return plugins.values();
  }

  @Override
  public StoragePlugin create(String name, StoragePluginConfig pluginConfig) {
    throw new IllegalStateException("Should not create instances of system plugins");
  }

  @Override
  public Set<Class<? extends StoragePluginConfig>> configClasses() {
    return null;
  }

  @Override
  public boolean storable() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public Class<? extends StoragePlugin> connectorClassFor(
      Class<? extends StoragePluginConfig> configClass) {

    // Not very efficient, but this method is generally for testing
    // and their are only a few system plugins. Not worth adding a map.
    for (StoragePlugin plugin : plugins.values()) {
      if (configClass.isInstance(plugin.getConfig())) {
        return plugin.getClass();
      }
    }
    return null;
  }

  @Override
  public void close() { }
}
