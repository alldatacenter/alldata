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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a storage plugin, defined by a (name, config) pair. The config
 * implies a connector definition, including a way to create the plugin
 * instance ({@code StoragePlugin}. Storage plugins are created lazily to avoid
 * long Drillbit start times. Plugin creation is synchronized as is closing.
 * <p>
 * A handle has a type used to determine which operations are allowed on
 * the handle. For example, inrinsic (system) plugins cannot be deleted or
 * disabled.
 *
 * <h4>Caveats</h4>
 *
 * Note that race conditions are still possible:
 * <ul>
 * <li>User 1 submits a query that refers to plugin p. The registry creates
 * an instance of plugin p and returns it. The planner proceeds to use it.</li>
 * <li>User 2 disables p, causing its entry to be updated in persistent storage
 * (but not yet in this entry)</li>
 * <li>User 3 submits a query that refers to plugin p. The registry notices that
 * the plugin is now disabled, removes it from the plugin list and closes the
 * plugin.</li>
 * <li>User 1 suffers a failure when the planner references the now-closed
 * plugin p.</li>
 * </ul>
 * This issue has existed for some time and cannot be fixed here. The right
 * solution is to introduce a reference count: each query which uses the plugin
 * should hold a reference count until the completion of the plan or fragment.
 * If we implement such a reference count, this is the place to maintain the count,
 * and to close the plugin when the count goes to zero.
 */
public class PluginHandle {
  private static final Logger logger = LoggerFactory.getLogger(PluginHandle.class);

  enum PluginType {

    /**
     * System or similar plugin that uses a single config
     * created along with the plugin implementation instance
     * at Drillbit startup time. Not stored in the persistent
     * store. The handle always contains an instance of the
     * plugin which lives for the lifetime of the Drillbit.
     */
    INTRINSIC,

    /**
     * Normal plugin defined by a (name, config) pair, stored in
     * persistent storage. Plugin instance created on demand on
     * first use (not first access, since some accesses only need
     * the name and config.) Lifetime is the lifetime of the Drillbit,
     * or sooner if the plugin config is changed, in which chase the
     * plugin becomes {@code EPHEMERAL.}
     */
    STORED,

    /**
     * Plugin which was either a) {@code STORED} at some point, but
     * the user then changed the config so that the old config (plus
     * its plugin) became {@code EPHEMERAL}, or b) created in response
     * to a table function that defined an ad-hoc, single-query plugin.
     * In either case, the config defines a plugin shared across fragments
     * when running a query. Ephemeral plugins eventually timeout. Or,
     * if the user changes a config back to match an ephemeral plugin,
     * the plugin moves back to the {@code STORED} state.
     */
    EPHEMERAL }

  private final String name;
  private final StoragePluginConfig config;
  private final ConnectorHandle connector;
  private final PluginType type;
  private StoragePlugin plugin;

  public PluginHandle(String name, StoragePluginConfig config,
      ConnectorHandle connector) {
    this.name = name;
    this.config = config;
    this.connector = connector;
    this.type = connector.isIntrinsic() ? PluginType.INTRINSIC : PluginType.STORED;
  }

  public PluginHandle(String name, StoragePluginConfig config,
      ConnectorHandle connector, PluginType type) {
    this.name = name;
    this.config = config;
    this.connector = connector;
    this.type = type;
  }

  public PluginHandle(StoragePlugin plugin, ConnectorHandle connector, PluginType type) {
    this.name = plugin.getName();
    this.config = plugin.getConfig();
    this.connector = connector;
    this.plugin = plugin;
    this.type = type;
  }

  public String name() { return name; }
  public StoragePluginConfig config() { return config; }
  public boolean isStored() { return type == PluginType.STORED; }
  public boolean isIntrinsic() { return type == PluginType.INTRINSIC; }

  /**
   * Retrieve the storage plugin instance, creating it if needed. Creation can take
   * time if the plugin creates a connection to another system, especially if that system
   * suffers timeouts.
   *
   * @return the initialized storage plugin
   * @throws UserException if the storage plugin creation failed due to class errors
   * (unlikely) or external system errors (more likely)
   */
  public synchronized StoragePlugin plugin() {
    if (plugin != null) {
      return plugin;
    }
    logger.info("Creating storage plugin for {}", name);
    try {
      plugin = connector.newInstance(name, config);
    } catch (UserException e) {
      throw e;
    } catch (Exception e) {
      throw UserException.internalError(e)
        .addContext("Plugin name", name)
        .addContext("Plugin class", connector.connectorClass().getName())
        .build(logger);
    }
    try {
      plugin.start();
    } catch (UserException e) {
      plugin = null;
      throw e;
    } catch (Exception e) {
      plugin = null;
      throw UserException.dataReadError(e)
        .addContext("Failed to start storage plugin")
        .addContext("Plugin name", name)
        .addContext("Plugin class", connector.connectorClass().getName())
        .build(logger);
    }
    return plugin;
  }

  @VisibleForTesting
  public synchronized boolean hasInstance() { return plugin != null; }

  /**
   * Close the plugin. Can occur when the handle is evicted from the loading
   * cache where we must not throw an exception. Also called on shutdown.
   */
  public synchronized void close() {
    try {
      if (plugin != null) {
        plugin.close();
      }
    } catch (Exception e) {
      logger.warn("Exception while shutting down storage plugin: {}",
          name == null ? "ephemeral" : config.getClass().getSimpleName(), e);
    } finally {
      plugin = null;
    }
  }

  /**
   * Atomically transfer the plugin instance, if any, to a new handle
   * of the given type. Avoids race conditions when transferring a plugin
   * from/to ephemeral storage and the plugin cache, since those two
   * caches are not synchronized as a whole. Ensures that only one of the
   * threads in a race condition will transfer the actual plugin instance.
   * <p>
   * By definition, a plugin becomes disabled if it moves to ephemeral,
   * enabled if it moves from ephemeral into stored status.
   */
  public synchronized PluginHandle transfer(PluginType type) {
    if (plugin == null) {
      return new PluginHandle(name, config, connector, type);
    } else {
      PluginHandle newHandle = new PluginHandle(plugin, connector, type);
      plugin = null;
      return newHandle;
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder()
        .append(getClass().getSimpleName())
        .append("[")
        .append("name=")
        .append(name)
        .append(", config=")
        .append(config.toString())
        .append(", provider=")
        .append(connector.getClass().getSimpleName())
        .append(", plugin=");
    if (plugin == null) {
      buf.append("null");
    } else {
      buf.append(plugin.getClass().getSimpleName())
         .append(" (")
         .append(System.identityHashCode(plugin) % 1000)
         .append(")");
    }
    return buf.append("]").toString();
  }
}
