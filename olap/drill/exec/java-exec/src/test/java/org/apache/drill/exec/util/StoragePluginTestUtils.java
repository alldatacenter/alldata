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
package org.apache.drill.exec.util;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.drill.common.logical.security.PlainCredentialsProvider;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.WorkspaceConfig;

import org.apache.drill.exec.store.easy.sequencefile.SequenceFileFormatConfig;
import org.apache.drill.exec.store.easy.text.TextFormatPlugin.TextFormatConfig;

/**
 * Utility methods to speed up tests.
 */
public class StoragePluginTestUtils {
  public static final String CP_PLUGIN_NAME = "cp";
  public static final String DFS_PLUGIN_NAME = "dfs";

  public static final String TMP_SCHEMA = "tmp";
  public static final String ROOT_SCHEMA = "root";

  public static final String DFS_TMP_SCHEMA = DFS_PLUGIN_NAME + "." + TMP_SCHEMA;

  /**
   * Update the workspace locations for a plugin.
   *
   * @param pluginName The plugin to update.
   * @param pluginRegistry A plugin registry.
   * @param tmpDirPath The directory to use.
   */
  public static void updateSchemaLocation(final String pluginName,
                                          final StoragePluginRegistry pluginRegistry,
                                          final File tmpDirPath,
                                          String... schemas) throws PluginException {
    final FileSystemConfig pluginConfig = (FileSystemConfig) pluginRegistry.getStoredConfig(pluginName);

    Map<String, WorkspaceConfig> newWorkspaces = new HashMap<>();
    Optional.ofNullable(pluginConfig.getWorkspaces())
      .ifPresent(newWorkspaces::putAll);

    if (schemas.length == 0) {
      schemas = new String[]{TMP_SCHEMA};
    }

    for (String schema : schemas) {
      WorkspaceConfig workspaceConfig = newWorkspaces.get(schema);
      String inputFormat = workspaceConfig == null ? null : workspaceConfig.getDefaultInputFormat();
      WorkspaceConfig newWorkspaceConfig = new WorkspaceConfig(tmpDirPath.getAbsolutePath(), true, inputFormat, false);
      newWorkspaces.put(schema, newWorkspaceConfig);
    }

    FileSystemConfig newPluginConfig = new FileSystemConfig(
        pluginConfig.getConnection(),
        pluginConfig.getConfig(),
        newWorkspaces,
        pluginConfig.getFormats(),
        PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    newPluginConfig.setEnabled(pluginConfig.isEnabled());
    pluginRegistry.put(pluginName, newPluginConfig);
  }

  public static void configureFormatPlugins(StoragePluginRegistry pluginRegistry) throws PluginException {
    configureFormatPlugins(pluginRegistry, CP_PLUGIN_NAME);
    configureFormatPlugins(pluginRegistry, DFS_PLUGIN_NAME);
  }

  public static void configureFormatPlugins(StoragePluginRegistry pluginRegistry, String storagePlugin) throws PluginException {
    FileSystemConfig fileSystemConfig = (FileSystemConfig) pluginRegistry.getStoredConfig(storagePlugin);

    Map<String, FormatPluginConfig> newFormats = new HashMap<>();
    Optional.ofNullable(fileSystemConfig.getFormats())
      .ifPresent(newFormats::putAll);

    newFormats.put("txt", new TextFormatConfig(
        ImmutableList.of("txt"), null, "\u0000", null, null, null, null, null));

    newFormats.put("ssv", new TextFormatConfig(
        ImmutableList.of("ssv"), null, " ", null, null, null, null, null));

    newFormats.put("psv", new TextFormatConfig(
        ImmutableList.of("tbl"), null, "|", null, null, null, null, null));

    SequenceFileFormatConfig seqConfig = new SequenceFileFormatConfig(ImmutableList.of("seq"));
    newFormats.put("sequencefile", seqConfig);

    newFormats.put("csvh-test", new TextFormatConfig(
        ImmutableList.of("csvh-test"), null, ",", null, null, null, true, true));

    FileSystemConfig newFileSystemConfig = new FileSystemConfig(
        fileSystemConfig.getConnection(),
        fileSystemConfig.getConfig(),
        fileSystemConfig.getWorkspaces(),
        newFormats,
        PlainCredentialsProvider.EMPTY_CREDENTIALS_PROVIDER);
    newFileSystemConfig.setEnabled(fileSystemConfig.isEnabled());

    pluginRegistry.put(storagePlugin, newFileSystemConfig);
  }
}
