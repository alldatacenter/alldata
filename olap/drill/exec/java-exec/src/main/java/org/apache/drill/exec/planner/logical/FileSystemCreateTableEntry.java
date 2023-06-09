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
package org.apache.drill.exec.planner.logical;

import java.io.IOException;
import java.util.List;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.logical.FormatPluginConfig;
import org.apache.drill.exec.physical.base.AbstractWriter;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.physical.base.Writer;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.apache.drill.exec.store.dfs.FileSystemConfig;
import org.apache.drill.exec.store.dfs.FormatPlugin;

import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

/**
 * Implements <code>CreateTableEntry</code> interface to create new tables in FileSystem storage.
 */
@JsonTypeName("filesystem")
public class FileSystemCreateTableEntry implements CreateTableEntry {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FileSystemCreateTableEntry.class);

  private final FileSystemConfig storageConfig;
  private final FormatPlugin formatPlugin;
  private final String location;
  private final List<String> partitionColumns;
  private final StorageStrategy storageStrategy;

  @JsonCreator
  public FileSystemCreateTableEntry(@JsonProperty("storageConfig") FileSystemConfig storageConfig,
                                    @JsonProperty("formatConfig") FormatPluginConfig formatConfig,
                                    @JsonProperty("location") String location,
                                    @JsonProperty("partitionColumn") List<String> partitionColumns,
                                    @JsonProperty("storageStrategy") StorageStrategy storageStrategy,
                                    @JacksonInject StoragePluginRegistry engineRegistry)
      throws ExecutionSetupException {
    this.storageConfig = storageConfig;
    this.formatPlugin = engineRegistry.resolveFormat(storageConfig, formatConfig, FormatPlugin.class);
    this.location = location;
    this.partitionColumns = partitionColumns;
    this.storageStrategy = storageStrategy;
  }

  public FileSystemCreateTableEntry(FileSystemConfig storageConfig,
                                    FormatPlugin formatPlugin,
                                    String location,
                                    List<String> partitionColumns,
                                    StorageStrategy storageStrategy) {
    this.storageConfig = storageConfig;
    this.formatPlugin = formatPlugin;
    this.location = location;
    this.partitionColumns = partitionColumns;
    this.storageStrategy = storageStrategy;
  }

  @JsonProperty("storageConfig")
  public FileSystemConfig getStorageConfig() {
    return storageConfig;
  }

  @JsonProperty("formatConfig")
  public FormatPluginConfig getFormatConfig() {
    return formatPlugin.getConfig();
  }

  @Override
  public Writer getWriter(PhysicalOperator child) throws IOException {
    if (!(formatPlugin.supportsAutoPartitioning() ||
        partitionColumns == null || partitionColumns.size() == 0)) {
      throw UserException.unsupportedError().message(String.format("%s format does not support auto-partitioning.",
          formatPlugin.getName())).build(logger);
    }

    AbstractWriter writer = formatPlugin.getWriter(child, location, partitionColumns);
    writer.setStorageStrategy(storageStrategy);
    return writer;
  }

  @Override
  public List<String> getPartitionColumns() {
    return partitionColumns;
  }

}
