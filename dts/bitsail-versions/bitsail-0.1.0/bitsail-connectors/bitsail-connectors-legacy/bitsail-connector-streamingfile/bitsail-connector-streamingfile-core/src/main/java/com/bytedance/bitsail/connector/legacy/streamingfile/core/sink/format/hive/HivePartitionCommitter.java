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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hive;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filestate.FileStateCollector;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionPathUtils.searchPartSpecAndPaths;

/**
 * Loader to temporary files to final output path and meta store. According to overwrite,
 * the loader will delete the previous data.
 *
 * <p>This provide one interface to load:
 * 1.{@link #commitAndUpdateJobTime}: add hive partition if it is new partition
 * will create partition to meta store.
 */
@Internal
public class HivePartitionCommitter extends AbstractPartitionCommitter {
  private static final Logger LOG = LoggerFactory.getLogger(HivePartitionCommitter.class);

  private final Integer partitionSearchSize;

  public HivePartitionCommitter(BitSailConfiguration jobConf, TableMetaStoreFactory factory, FileStateCollector fileStateCollector) {
    super(jobConf, factory, fileStateCollector);

    this.partitionSearchSize = hasHourPartition ? partitionKeys.size() - 2 : partitionKeys.size() - 1;
  }

  /**
   * add hive partition before commit timestamp.
   */
  @Override
  protected void commitMultiPartitions(List<Long> pendingCommitPartitions) throws Exception {
    try (TableMetaStoreFactory.TableMetaStore metaStore = factory.createTableMetaStore()) {
      for (long commitTimestamp : pendingCommitPartitions) {
        List<Tuple2<LinkedHashMap<String, String>, Path>> specAndPaths = detectPartSpecAndPaths(commitTimestamp);
        if (CollectionUtils.isEmpty(specAndPaths) && !hasDynamicPartition) {
          createEmptyPartition(FileSystem.get(locationPath.toUri()), metaStore, commitTimestamp);
          continue;
        }

        for (Tuple2<LinkedHashMap<String, String>, Path> specAndPath : specAndPaths) {
          LinkedHashMap<String, String> partSpec = specAndPath.f0;
          Path path = specAndPath.f1;

          createPartition(metaStore, partSpec, path);
        }
      }
    }
  }

  @Override
  public void commitPartition(LinkedHashMap<String, String> partSpec) throws Exception {
    try (TableMetaStoreFactory.TableMetaStore metaStore = factory.createTableMetaStore()) {
      Path commitHivePath = metaStore.getPartitionPath(partSpec);
      FileSystem fileSystem = locationPath.getFileSystem();
      if (!fileSystem.exists(commitHivePath)) {
        fileSystem.mkdirs(commitHivePath);
        LOG.info("Commit path: {} not exists. We will create it", commitHivePath);
      }
      createPartition(metaStore, partSpec, commitHivePath);
    }
  }

  private void createEmptyPartition(FileSystem fileSystem,
                                    TableMetaStoreFactory.TableMetaStore metaStore,
                                    Long commitTimestamp) throws Exception {
    LocalDateTime partitionDateTime = LocalDateTime.ofInstant(
        Instant.ofEpochMilli(commitTimestamp), TimeZone.getDefault().toZoneId());

    LinkedHashMap<String, String> partSpec = Maps.newLinkedHashMap();
    Path commitHivePath = locationPath;
    for (PartitionInfo partitionInfo : partitionKeys) {
      if (PartitionType.TIME.equals(partitionInfo.getType())) {
        String formatTimeStr = getOrCreateTimeFormatter(partitionInfo).format(partitionDateTime);
        partSpec.put(partitionInfo.getName(), formatTimeStr);
        commitHivePath = commitHivePath.suffix("/" + partitionInfo.getName() + "=" + formatTimeStr);
      } else {
        partSpec.put(partitionInfo.getName(), partitionInfo.getValue());
        commitHivePath = commitHivePath.suffix("/" + partitionInfo.getName() + "=" + partitionInfo.getValue());
      }
    }
    LOG.info("Add Empty partition {}.", commitHivePath.toUri());
    if (!fileSystem.exists(commitHivePath)) {
      fileSystem.mkdirs(commitHivePath);
    }
    createPartition(metaStore, partSpec, commitHivePath);
  }

  protected void createPartition(TableMetaStoreFactory.TableMetaStore metaStore,
                                 LinkedHashMap<String, String> partSpec,
                                 Path path) throws Exception {
    Optional<Path> pathFromMeta = metaStore.getPartition(partSpec);
    if (!pathFromMeta.isPresent()) {
      metaStore.createPartition(partSpec, path);
    }
  }

  private List<Tuple2<LinkedHashMap<String, String>, Path>> detectPartSpecAndPaths(long partitionTime) throws IOException {
    LocalDateTime partitionDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(partitionTime), TimeZone.getDefault().toZoneId());

    Path commitHivePath = locationPath;

    for (PartitionInfo partitionInfo : partitionKeys) {
      if (PartitionType.TIME.equals(partitionInfo.getType())) {
        commitHivePath = commitHivePath.suffix("/" + partitionInfo.getName() + "=" + getOrCreateTimeFormatter(partitionInfo).format(partitionDateTime));
      }
    }

    FileSystem fileSystem = locationPath.getFileSystem();
    if (!fileSystem.exists(commitHivePath)) {
      LOG.info("Commit path: {} not exists.", commitHivePath);
      return Lists.newArrayList();
    }
    return searchPartSpecAndPaths(locationPath.getFileSystem(), commitHivePath, partitionSearchSize);
  }
}
