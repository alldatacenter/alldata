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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;

import org.apache.drill.exec.store.AbstractSchemaFactory;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.planner.logical.CreateTableEntry;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.PartitionNotFoundException;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory.WorkspaceSchema;

import org.apache.drill.exec.util.DrillFileSystemUtil;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;

/**
 * This is the top level schema that responds to root level path requests. Also supports
 */
public class FileSystemSchemaFactory extends AbstractSchemaFactory {

  public static final String LOCAL_FS_SCHEME = "file";

  private List<WorkspaceSchemaFactory> factories;
  protected FileSystemPlugin plugin;

  public FileSystemSchemaFactory(String schemaName, List<WorkspaceSchemaFactory> factories) {
    super(schemaName);
    // when the correspondent FileSystemPlugin is not passed in, we dig into ANY workspace factory to get it.
    if (factories.size() > 0) {
      this.plugin = factories.get(0).getPlugin();
    }
    this.factories = factories;
  }

  public FileSystemSchemaFactory(FileSystemPlugin plugin, String schemaName, List<WorkspaceSchemaFactory> factories) {
    super(schemaName);
    this.plugin = plugin;
    this.factories = factories;
  }

  @Override
  public void registerSchemas(SchemaConfig schemaConfig, SchemaPlus parent) throws IOException {
    FileSystemSchema schema = new FileSystemSchema(getName(), schemaConfig);
    SchemaPlus plusOfThis = parent.add(schema.getName(), schema);
    schema.setPlus(plusOfThis);
  }

  public class FileSystemSchema extends AbstractSchema {

    private final WorkspaceSchema defaultSchema;
    private final Map<String, WorkspaceSchema> schemaMap = new HashMap<>();

    public FileSystemSchema(String name, SchemaConfig schemaConfig) throws IOException {
      super(Collections.emptyList(), name);
      final DrillFileSystem fs = ImpersonationUtil.createFileSystem(schemaConfig.getUserName(), plugin.getFsConf());
      for(WorkspaceSchemaFactory f :  factories){
        WorkspaceSchema s = f.createSchema(getSchemaPath(), schemaConfig, fs);
        if (s != null) {
          schemaMap.put(s.getName(), s);
        }
      }

      defaultSchema = schemaMap.get(DEFAULT_WS_NAME);
    }

    void setPlus(SchemaPlus plusOfThis){
      for(WorkspaceSchema s : schemaMap.values()){
        plusOfThis.add(s.getName(), s);
      }
    }

    @Override
    public Iterable<String> getSubPartitions(String table,
                                             List<String> partitionColumns,
                                             List<String> partitionValues
                                            ) throws PartitionNotFoundException {
      List<FileStatus> fileStatuses;
      try {
        fileStatuses = DrillFileSystemUtil.listDirectories(defaultSchema.getFS(), new Path(defaultSchema.getDefaultLocation(), table), false);
      } catch (IOException e) {
        throw new PartitionNotFoundException("Error finding partitions for table " + table, e);
      }
      return new SubDirectoryList(fileStatuses);
    }

    @Override
    public boolean showInInformationSchema() {
      return false;
    }

    @Override
    public String getTypeName() {
      return FileSystemConfig.NAME;
    }

    @Override
    public Table getTable(String name) {
      return defaultSchema.getTable(name);
    }

    @Override
    public Collection<Function> getFunctions(String name) {
      return defaultSchema.getFunctions(name);
    }

    @Override
    public Set<String> getFunctionNames() {
      return defaultSchema.getFunctionNames();
    }

    @Override
    public AbstractSchema getSubSchema(String name) {
      return schemaMap.get(name);
    }

    @Override
    public Set<String> getSubSchemaNames() {
      return schemaMap.keySet();
    }

    @Override
    public Set<String> getTableNames() {
      return defaultSchema.getTableNames();
    }

    @Override
    public boolean isMutable() {
      return defaultSchema.isMutable();
    }

    @Override
    public CreateTableEntry createNewTable(String tableName, List<String> partitionColumns, StorageStrategy storageStrategy) {
      return defaultSchema.createNewTable(tableName, partitionColumns, storageStrategy);
    }

    @Override
    public CreateTableEntry createStatsTable(String tableName) {
      return defaultSchema.createStatsTable(tableName);
    }

    @Override
    public CreateTableEntry appendToStatsTable(String tableName) {
      return defaultSchema.appendToStatsTable(tableName);
    }

    @Override
    public Table getStatsTable(String tableName) {
      return defaultSchema.getStatsTable(tableName);
    }

    @Override
    public AbstractSchema getDefaultSchema() {
      return defaultSchema;
    }
  }
}
