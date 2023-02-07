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
import java.util.List;
import java.util.Set;

import org.apache.calcite.plan.RelOptRule;
import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.exec.ops.OptimizerRulesContext;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.server.options.SessionOptionManager;
import org.apache.drill.exec.store.dfs.FormatPlugin;

/** Interface for all implementations of the storage plugins. Different implementations of the storage
 * formats will implement methods that indicate if Drill can write or read its tables from that format,
 * if there are optimizer rules specific for the format, getting a storage config. etc.
 */
public interface StoragePlugin extends SchemaFactory, AutoCloseable {

  String getName();

  /**
   * Initialize the storage plugin. The storage plugin will not be used until this method is called.
   */
  void start() throws IOException;

  /**
   * Indicates if Drill can read the table from this format.
  */
  boolean supportsRead();

  /**
   * Indicates if Drill can write a table to this format (e.g. as JSON, csv, etc.).
   */
  boolean supportsWrite();

  /**
   * Method returns a Jackson serializable object that extends a StoragePluginConfig.
   *
   * @return an extension of StoragePluginConfig
  */
  StoragePluginConfig getConfig();

  /**
   * An implementation of this method will return one or more specialized rules that Drill query
   * optimizer can leverage in <i>physical</i> space. Otherwise, it should return an empty set.
   * @return an empty set or a set of plugin specific physical optimizer rules.
   */
  @Deprecated
  Set<? extends RelOptRule> getOptimizerRules(OptimizerRulesContext optimizerContext);

  /**
   * Get the physical scan operator for the particular GroupScan (read) node.
   *
   * @param userName User whom to impersonate when when reading the contents as part of Scan.
   * @param selection The configured storage engine specific selection.
   * @return The physical scan operator for the particular GroupScan (read) node.
   */
  AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection) throws IOException;

  /**
   * Get the physical scan operator for the particular GroupScan (read) node.
   *
   * @param userName User whom to impersonate when when reading the contents as part of Scan.
   * @param selection The configured storage engine specific selection.
   * @param options (optional) session options
   * @return The physical scan operator for the particular GroupScan (read) node.
   */
  AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      SessionOptionManager options) throws IOException;

  /**
   * Get the physical scan operator for the particular GroupScan (read) node.
   *
   * @param userName        User whom to impersonate when when reading the contents as part of Scan.
   * @param selection       The configured storage engine specific selection.
   * @param options         (optional) session options
   * @param providerManager manager for handling metadata providers
   * @return The physical scan operator for the particular GroupScan (read) node.
   */
  AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      SessionOptionManager options, MetadataProviderManager providerManager) throws IOException;

  /**
   * Get the physical scan operator for the particular GroupScan (read) node.
   *
   * @param userName User whom to impersonate when when reading the contents as part of Scan.
   * @param selection The configured storage engine specific selection.
   * @param columns (optional) The list of column names to scan from the data source.
   * @return The physical scan operator for the particular GroupScan (read) node.
  */
  AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      List<SchemaPath> columns) throws IOException;

  /**
   * Get the physical scan operator for the particular GroupScan (read) node.
   *
   * @param userName User whom to impersonate when when reading the contents as part of Scan.
   * @param selection The configured storage engine specific selection.
   * @param columns (optional) The list of column names to scan from the data source.
   * @param options (optional) session options
   * @return The physical scan operator for the particular GroupScan (read) node.
   */
  AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      List<SchemaPath> columns, SessionOptionManager options) throws IOException;

  /**
   * Get the physical scan operator for the particular GroupScan (read) node.
   *
   * @param userName        User whom to impersonate when when reading the contents as part of Scan.
   * @param selection       The configured storage engine specific selection.
   * @param columns         (optional) The list of column names to scan from the data source.
   * @param options         (optional) session options
   * @param providerManager manager for handling metadata providers
   * @return The physical scan operator for the particular GroupScan (read) node.
   */
  AbstractGroupScan getPhysicalScan(String userName, JSONOptions selection,
      List<SchemaPath> columns, SessionOptionManager options, MetadataProviderManager providerManager) throws IOException;

  /**
   * Allows to get the format plugin for current storage plugin based on appropriate format plugin config usage.
   *
   * @param config format plugin config
   * @return format plugin instance
   * @throws UnsupportedOperationException, if storage plugin doesn't support format plugins.
   */
  FormatPlugin getFormatPlugin(FormatPluginConfig config);
}
