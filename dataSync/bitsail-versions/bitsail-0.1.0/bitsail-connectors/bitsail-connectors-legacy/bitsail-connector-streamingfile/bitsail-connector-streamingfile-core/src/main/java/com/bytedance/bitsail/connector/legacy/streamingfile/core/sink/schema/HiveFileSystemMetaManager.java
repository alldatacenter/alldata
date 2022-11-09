/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.schema;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMeta;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.schema.FileSystemMetaManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.syncer.AbstractMetaSyncer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSchemaOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.parser.FileSystemRowBuilder;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive.HiveFileSystemFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive.HiveTableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.syncer.HiveMetaInfoSyncer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.constants.StreamingFileSystemMetricsNames.SCHEMA_UPDATE_SUCCESS;

/**
 * Created 2020/11/17.
 */
public class HiveFileSystemMetaManager extends FileSystemMetaManager {
  private static final Logger LOG =
      LoggerFactory.getLogger(HiveFileSystemMetaManager.class);

  private final HiveTableMetaStoreFactory factory;

  private final List<PartitionInfo> partitionInfos;

  protected long schemaDiscoveryInterval;

  protected boolean schemaDiscoveryEnable;

  private HiveMeta serializationHiveMeta;

  private String formatDumpType;

  /**
   * Dynamic Parameter
   */
  @Getter
  private transient List<ColumnInfo> sourceColumnInfos;

  @Getter
  private transient List<ColumnInfo> sinkColumnInfos;

  @Getter
  private transient HiveMeta hiveMeta;

  @Getter
  private transient FileSystemRowBuilder rowBuilder;

  @Getter
  private transient int[] normalColumnInfoIndices;

  @Getter
  private transient LinkedHashMap<PartitionInfo, Integer> partitionColumnMap;

  public HiveFileSystemMetaManager(BitSailConfiguration jobConf) {
    super(jobConf);

    this.factory = HiveFileSystemFactory.getHiveMetaFromTable(jobConf);
    this.partitionInfos = PartitionUtils.getPartitionInfo(jobConf);
    this.serializationHiveMeta = HiveTableMetaStoreFactory.createHiveMeta(factory);

    boolean formatDiscovery = false;
    formatDumpType = jobConf.get(FileSystemSinkOptions.HDFS_DUMP_TYPE);
    String formatType = jobConf.get(FileSystemCommonOptions.DUMP_FORMAT_TYPE);
    // JSON Format enable discover default
    if (StreamingFileSystemValidator.HIVE_FORMAT_TYPE_VALUE.equalsIgnoreCase(formatType)
        && formatDumpType.equalsIgnoreCase(StreamingFileSystemValidator.HDFS_DUMP_TYPE_JSON)) {
      if (!PartitionUtils.hasDynamicPartition(partitionInfos)) {
        formatDiscovery = jobConf.get(FileSystemSchemaOptions.SCHEMA_DISCOVER_ENABLED);
      }
    }
    this.schemaDiscoveryEnable = formatDiscovery;
    this.schemaDiscoveryInterval = jobConf.get(FileSystemSchemaOptions.SCHEMA_DISCOVER_INTERVAL);
  }

  @Override
  public void open() {
    long schemaExpireInterval = jobConf.get(FileSystemSchemaOptions.SCHEMA_EXPIRE_INTERVAL);
    if (System.currentTimeMillis() - serializationHiveMeta.getCreateTime() <= schemaExpireInterval) {
      LOG.info("Hive filesystem open use history serialization meta.");
      updateBaseParameter(serializationHiveMeta);
    } else {
      LOG.info("Hive filesystem open use current meta.");
      updateBaseParameter(HiveTableMetaStoreFactory.createHiveMeta(factory));
    }
  }

  private void updateBaseParameter(HiveMeta newHiveMeta) {
    this.sourceColumnInfos = getColumnsFromConfOrMetastore(
        jobConf.get(FileSystemSchemaOptions.SOURCE_SCHEMA), newHiveMeta);

    this.sinkColumnInfos = getColumnsFromConfOrMetastore(
        jobConf.get(FileSystemSchemaOptions.SINK_SCHEMA), newHiveMeta);

    this.rowBuilder = createHiveRowBuilder(sourceColumnInfos);

    this.partitionColumnMap = createPartitionMap(sinkColumnInfos);

    this.normalColumnInfoIndices = createNormalInfoIndices(sinkColumnInfos);

    this.hiveMeta = newHiveMeta;
  }

  @Override
  public void updateFileSystemMeta(FileSystemMeta fileSystemMeta, int taskId) {
    if (schemaDiscoveryEnable) {
      HiveMeta newHiveMeta = (HiveMeta) fileSystemMeta;

      if (Objects.nonNull(newHiveMeta) && !hiveMeta.equals(newHiveMeta)) {
        LOG.info("Subtask {} update hive schema, before: {}, after: {}.", taskId,
            hiveMeta, newHiveMeta);

        updateBaseParameter(newHiveMeta);

        setUpdateValue(true);

        MetricsFactory.getInstanceMetricsManager(jobConf, taskId).recordCounter(SCHEMA_UPDATE_SUCCESS);

      }
    }
  }

  private List<ColumnInfo> getColumnsFromConfOrMetastore(String columnStr, HiveMeta hiveMeta) {
    if (!schemaDiscoveryEnable) {
      return JsonSerializer.parseToList(columnStr, ColumnInfo.class);
    }
    String columns = hiveMeta.getColumns();
    String columnTypes = hiveMeta.getColumnTypes();
    if (StringUtils.isEmpty(columns) || StringUtils.isEmpty(columnTypes)) {
      throw new IllegalArgumentException("Hive meta columns or columns type is empty.");
    }

    String[] splitColumns = StringUtils.split(columns, ",");
    String[] splitColumnTypes = StringUtils.split(columnTypes, ":");

    List<ColumnInfo> columnInfos = Lists.newArrayList();
    for (int index = 0; index < splitColumns.length; index++) {
      columnInfos.add(new ColumnInfo(splitColumns[index], splitColumnTypes[index]));
    }
    return columnInfos;
  }

  private FileSystemRowBuilder createHiveRowBuilder(List<ColumnInfo> sourceColumnInfos) {
    try {
      return new FileSystemRowBuilder(jobConf, sourceColumnInfos, formatDumpType);
    } catch (Exception e) {
      LOG.error("Hive row builder create failed.", e);
      throw new IllegalArgumentException(e);
    }
  }

  private LinkedHashMap<PartitionInfo, Integer> createPartitionMap(List<ColumnInfo> sinkColumnInfos) {
    List<String> sinkColumnNameInfos = sinkColumnInfos
        .stream()
        .map(ColumnInfo::getName)
        .collect(Collectors.toList());

    LinkedHashMap<PartitionInfo, Integer> partitionColumnMap = Maps.newLinkedHashMap();
    for (PartitionInfo partitionInfo : partitionInfos) {
      int index = sinkColumnNameInfos.indexOf(partitionInfo.getName());

      if (index >= 0 && !PartitionType.DYNAMIC.equals(partitionInfo.getType())) {
        // Partition column in sink columns must be dynamic.

        throw new IllegalArgumentException(String.format("schema can not contain partition name: %s",
            partitionInfo.getName()));

      } else if (index < 0 && PartitionType.DYNAMIC.equals(partitionInfo.getType())) {
        // Dynamic partition must in sink columns.

        throw new IllegalArgumentException(String.format("schema should contain dynamic partition name: %s",
            partitionInfo.getName()));
      }
      partitionColumnMap.put(partitionInfo, index);
    }
    return partitionColumnMap;
  }

  private int[] createNormalInfoIndices(List<ColumnInfo> sinkColumnInfos) {
    Collection<Integer> values = partitionColumnMap.values();
    return IntStream.range(0, sinkColumnInfos.size())
        .filter(c -> !values.contains(c))
        .toArray();
  }

  @Override
  public boolean shouldScheduleUpdate() {
    return schemaDiscoveryEnable;
  }

  @Override
  public AbstractMetaSyncer createMetaSyncer() {
    return new HiveMetaInfoSyncer(factory,
        hiveMeta,
        schemaDiscoveryInterval);
  }
}
