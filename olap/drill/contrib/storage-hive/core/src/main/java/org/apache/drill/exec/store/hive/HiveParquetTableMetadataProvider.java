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
package org.apache.drill.exec.store.hive;

import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.store.dfs.ReadEntryWithPath;
import org.apache.drill.exec.store.parquet.BaseParquetMetadataProvider;
import org.apache.drill.exec.store.parquet.metadata.Metadata;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.hadoop.hive.ql.io.parquet.ProjectionPusher;
import org.apache.hadoop.mapred.FileSplit;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * This is Metadata provider for Hive Parquet tables, which are read by Drill native reader
 */
public class HiveParquetTableMetadataProvider extends BaseParquetMetadataProvider {

  private final HiveStoragePlugin hiveStoragePlugin;
  private final HivePartitionHolder hivePartitionHolder;

  private HiveParquetTableMetadataProvider(Builder builder) throws IOException {
    super(builder);

    this.hiveStoragePlugin = builder.hiveStoragePlugin;
    this.hivePartitionHolder = builder.hivePartitionHolder != null
        ? builder.hivePartitionHolder
        : new HivePartitionHolder();

    if (builder.logicalInputSplits != null) {
      for (HiveMetadataProvider.LogicalInputSplit logicalInputSplit : builder.logicalInputSplits) {
        Iterator<InputSplit> iterator = logicalInputSplit.getInputSplits().iterator();
        // logical input split contains list of splits by files
        // we need to read path of only one to get file path
        assert iterator.hasNext();
        InputSplit split = iterator.next();
        assert split instanceof FileSplit;
        FileSplit fileSplit = (FileSplit) split;
        Path finalPath = fileSplit.getPath();
        Path pathString = Path.getPathWithoutSchemeAndAuthority(finalPath);
        entries.add(new ReadEntryWithPath(pathString));

        // store partition values per path
        Partition partition = logicalInputSplit.getPartition();
        if (partition != null) {
          hivePartitionHolder.add(pathString, partition.getValues());
        }
      }
    }
    init((BaseParquetMetadataProvider) builder.metadataProviderManager().getTableMetadataProvider());
  }

  public HivePartitionHolder getHivePartitionHolder() {
    return hivePartitionHolder;
  }

  @Override
  protected void initInternal() throws IOException {
    Map<FileStatus, FileSystem> fileStatusConfMap = new LinkedHashMap<>();
    for (ReadEntryWithPath entry : entries) {
      Path path = entry.getPath();
      Configuration conf = new ProjectionPusher().pushProjectionsAndFilters(
          new JobConf(hiveStoragePlugin.getHiveConf()),
          path.getParent());
      FileSystem fs = path.getFileSystem(conf);
      fileStatusConfMap.put(fs.getFileStatus(Path.getPathWithoutSchemeAndAuthority(path)), fs);
    }
    parquetTableMetadata = Metadata.getParquetTableMetadata(fileStatusConfMap, readerConfig);
  }

  public static class Builder extends BaseParquetMetadataProvider.Builder<Builder> {
    private HiveStoragePlugin hiveStoragePlugin;
    private HivePartitionHolder hivePartitionHolder;
    private List<HiveMetadataProvider.LogicalInputSplit> logicalInputSplits;

    public Builder(MetadataProviderManager source) {
      super(source);
    }

    protected Builder withHiveStoragePlugin(HiveStoragePlugin hiveStoragePlugin) {
      this.hiveStoragePlugin = hiveStoragePlugin;
      return self();
    }

    protected Builder withHivePartitionHolder(HivePartitionHolder hivePartitionHolder) {
      this.hivePartitionHolder = hivePartitionHolder;
      return self();
    }

    protected Builder withLogicalInputSplits(List<HiveMetadataProvider.LogicalInputSplit> logicalInputSplits) {
      this.logicalInputSplits = logicalInputSplits;
      return self();
    }

    @Override
    protected Builder self() {
      return this;
    }

    @Override
    public HiveParquetTableMetadataProvider build() throws IOException {
      return new HiveParquetTableMetadataProvider(this);
    }
  }
}
