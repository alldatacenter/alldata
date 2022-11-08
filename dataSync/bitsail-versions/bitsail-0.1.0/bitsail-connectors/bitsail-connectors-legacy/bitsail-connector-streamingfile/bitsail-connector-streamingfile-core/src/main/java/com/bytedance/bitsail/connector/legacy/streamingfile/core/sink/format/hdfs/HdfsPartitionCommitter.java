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

package com.bytedance.bitsail.connector.legacy.streamingfile.core.sink.format.hdfs;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.common.util.TimeUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionMapping;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.file.AbstractPartitionCommitter;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filestate.FileStateCollector;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.TableMetaStoreFactory;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.directory.PartitionDirectoryManager;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemSinkOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.apache.flink.annotation.Internal;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TimeZone;
import java.util.stream.Collectors;

import static com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.PartitionPathUtils.searchPaths;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_JSON;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_MSGPACK;
import static com.bytedance.bitsail.connector.legacy.streamingfile.common.validator.StreamingFileSystemValidator.HDFS_DUMP_TYPE_TEXT;

/**
 * <p>This provide one interface to load:
 * 1.{@link #commitAndUpdateJobTime}: add success files and update job commit time
 */
@Internal
public class HdfsPartitionCommitter extends AbstractPartitionCommitter {
  private static final Logger LOG = LoggerFactory.getLogger(HdfsPartitionCommitter.class);
  private static final String DEFAULT_EMPTY_FILE_PREFIX = "dorado";

  private final boolean addEmptyFile;
  private final boolean hdfsDynamicAddEmptyFileEnable;
  private final String dumpType;
  private final PartitionDirectoryManager.DirectoryType directoryType;

  private int dayIndex;
  private PartitionInfo dayPartitionInfo;
  private PartitionInfo hourPartitionInfo;
  private Map<String, PartitionInfo> partitionInfoMap;

  /**
   * collections for locations.
   * In split mode, BitSail Dump have multi location at the same time,
   * so comitter need to add add success tag for every location path.
   */
  private Set<Path> locationPaths;

  public HdfsPartitionCommitter(BitSailConfiguration jobConf, TableMetaStoreFactory factory, FileStateCollector fileStateCollector) {
    super(jobConf, factory, fileStateCollector);
    dumpType = jobConf.get(FileSystemSinkOptions.HDFS_DUMP_TYPE);
    hdfsDynamicAddEmptyFileEnable = jobConf.get(FileSystemCommonOptions.CommitOptions.HDFS_ADD_EMPTY_ON_DYNAMIC_ENABLE);
    directoryType = PartitionDirectoryManager.getDirectoryType(jobConf);
    addEmptyFile = checkNeedAddEmptyFile();
    initializeStreamMode();
  }

  /**
   * initialize only in streaming mode.
   */
  private void initializeStreamMode() {
    LOG.info("HDFS Committer run in streaming mode.");
    List<PartitionInfo> timePartSpecs = PartitionUtils.filterPartSpec(partitionKeys,
        partSpec -> PartitionType.TIME.equals(partSpec.getType()));
    dayPartitionInfo = timePartSpecs.get(0);
    hourPartitionInfo = timePartSpecs.get(1);
    dayIndex = partitionKeys.indexOf(dayPartitionInfo);
    locationPaths = Sets.newHashSet(locationPath);
    partitionInfoMap = PartitionUtils.getPartitionInfoMap(jobConf);

    String hdfsSplitPartitionConfig = jobConf.get(FileSystemCommonOptions.CommitOptions.HDFS_SPLIT_PARTITION_MAPPINGS);
    if (StringUtils.isNotEmpty(hdfsSplitPartitionConfig)) {
      List<PartitionMapping> partitionMappings = JsonSerializer
          .parseToList(hdfsSplitPartitionConfig, PartitionMapping.class);
      locationPaths.addAll(partitionMappings
          .stream()
          .map(PartitionMapping::getLocation)
          .map(Path::new)
          .collect(Collectors.toSet()));
    }
  }

  private boolean checkNeedAddEmptyFile() {
    if (HDFS_DUMP_TYPE_JSON.equalsIgnoreCase(dumpType)
        || HDFS_DUMP_TYPE_TEXT.equalsIgnoreCase(dumpType)
        || HDFS_DUMP_TYPE_MSGPACK.equalsIgnoreCase(dumpType)) {
      return !hasDynamicPartition || hdfsDynamicAddEmptyFileEnable;
    }
    return false;
  }

  @Override
  protected void commitMultiPartitions(List<Long> pendingCommitPartitions) throws Exception {
    for (Long commitTimestamp : pendingCommitPartitions) {
      addSuccessFile(commitTimestamp, SuccessFileType.HOUR_SUCCESS_File);
    }
  }

  @Override
  protected void commitSinglePartition(Long beginCommitPartitionTime, Long endCommitPartitionTime) throws Exception {
    Long lastDaySuccessTagTime = TimeUtils.roundDownTimeStampDate(beginCommitPartitionTime);

    LOG.info("last daily tag = {}, begin commit time = {}, end commit time = {}.",
        lastDaySuccessTagTime,
        beginCommitPartitionTime,
        endCommitPartitionTime);
    while (lastDaySuccessTagTime + DAY_MILLIS <= endCommitPartitionTime) {
      addSuccessFile(lastDaySuccessTagTime, SuccessFileType.DAY_SUCCESS_File);
      lastDaySuccessTagTime += DAY_MILLIS;
    }
  }

  @Override
  public void commitPartition(LinkedHashMap<String, String> partSpec) throws Exception {
    FileSystem fileSystem = locationPath.getFileSystem();
    LinkedHashMap<String, String> partSpecToDay = new LinkedHashMap<>(partSpec.size());
    int i = 0;
    for (Map.Entry<String, String> entry : partSpec.entrySet()) {
      partSpecToDay.put(entry.getKey(), entry.getValue());
      if (!partitionInfoMap.get(entry.getKey()).getType().equals(PartitionType.SPLIT)) {
        i++;
      }
      if (i > dayIndex) {
        break;
      }
    }
    try (TableMetaStoreFactory.TableMetaStore metaStore = factory.createTableMetaStore()) {
      Path commitPath = metaStore.getPartitionPath(partSpecToDay);
      String hourStr = partSpec.get(hourPartitionInfo.getName());
      String dateStr = partSpec.get(dayPartitionInfo.getName());
      addEmptyFile(fileSystem, ImmutableList.of(commitPath), dateStr, hourStr);
      Path successFile = new Path(commitPath, "_" + hourStr + "_SUCCESS");
      createFile(fileSystem, successFile);
      LOG.info("Add Success File:{}", successFile.toUri());
    }
  }

  private void addSuccessFile(Long timeStamp, SuccessFileType fileType) throws Exception {
    FileSystem fileSystem = locationPath.getFileSystem();
    LocalDateTime commitTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(timeStamp),
        TimeZone.getDefault().toZoneId());

    String dateStr = getOrCreateTimeFormatter(dayPartitionInfo).format(commitTime);
    String hourStr = getOrCreateTimeFormatter(hourPartitionInfo).format(commitTime);

    List<Tuple2<Path, List<Path>>> detectSuccessPaths = detectSuccessPaths(fileSystem, commitTime, fileType);
    List<Path> afterAddSuccessFiles = Lists.newArrayList();

    for (Tuple2<Path, List<Path>> beforeAddSuccessFile : detectSuccessPaths) {
      Path successFile = beforeAddSuccessFile.f0;
      if (fileType == SuccessFileType.DAY_SUCCESS_File) {
        successFile = new Path(successFile, dateStr + "/" + "_SUCCESS");
      } else {
        successFile = new Path(successFile, dateStr + "/" + "_" + hourStr + "_SUCCESS");
      }
      afterAddSuccessFiles.add(successFile);
    }

    for (int index = 0; index < afterAddSuccessFiles.size(); index++) {
      if (!fileSystem.exists(afterAddSuccessFiles.get(index))) {
        if (fileType == SuccessFileType.HOUR_SUCCESS_File) {
          addEmptyFile(fileSystem,
              detectSuccessPaths.get(index).f1,
              dateStr, hourStr);
        }
        createFile(fileSystem, afterAddSuccessFiles.get(index));
        LOG.info("Add Success File:{}", afterAddSuccessFiles.get(index).toUri());
      }
    }

  }

  private List<Tuple2<Path, List<Path>>> detectSuccessPaths(FileSystem fileSystem,
                                                            LocalDateTime commitTime,
                                                            SuccessFileType fileType) {

    List<Path> successFilePaths = Lists.newArrayList();
    for (Path locationPath : locationPaths) {
      successFilePaths.addAll(listPartitionPathRecurse(fileSystem,
          locationPath, 0,
          dayIndex,
          commitTime));
    }

    List<Tuple2<Path, List<Path>>> successAndEmptyPaths = Lists.newArrayList();

    for (Path parentPath : successFilePaths) {
      if (SuccessFileType.DAY_SUCCESS_File.equals(fileType)) {
        successAndEmptyPaths.add(Tuple2.of(parentPath, null));
      } else {
        List<Path> emptyParentPath = Lists.newArrayList();
        if (addEmptyFile) {
          emptyParentPath = listPartitionPathRecurse(fileSystem,
              parentPath,
              dayIndex,
              directoryType.getPartitionSize(partitionKeys),
              commitTime);
        }
        successAndEmptyPaths.add(Tuple2.of(parentPath, emptyParentPath));
      }
    }
    return successAndEmptyPaths;
  }

  private List<Path> listPartitionPathRecurse(FileSystem fileSystem,
                                              Path parentPath,
                                              int startPartitionIndex,
                                              int endPartitionIndex,
                                              LocalDateTime partitionDateTime) {
    Queue<Path> childPartitionPaths = Queues.newLinkedBlockingDeque();
    childPartitionPaths.add(parentPath);
    for (int index = startPartitionIndex; index < endPartitionIndex; index++) {
      int queueSize = childPartitionPaths.size();

      for (int queueStartIndex = 0; queueStartIndex < queueSize; queueStartIndex++) {
        Path currentParentPath = childPartitionPaths.poll();

        if (PartitionType.DYNAMIC.equals(partitionKeys.get(index).getType())) {
          childPartitionPaths.addAll(searchPaths(fileSystem, currentParentPath, 1));
        }

        if (PartitionType.TIME.equals(partitionKeys.get(index).getType())) {
          currentParentPath = currentParentPath.suffix("/" + getOrCreateTimeFormatter(partitionKeys.get(index)).format(partitionDateTime));
          childPartitionPaths.add(currentParentPath);
        }

        if (PartitionType.STATIC.equals(partitionKeys.get(index).getType())) {
          currentParentPath = currentParentPath.suffix("/" + partitionKeys.get(index).getValue());
          childPartitionPaths.add(currentParentPath);
        }
      }
    }
    return Lists.newArrayList(childPartitionPaths);
  }

  private void addEmptyFile(FileSystem fileSystem,
                            List<Path> emptyParentPaths,
                            String dateStr,
                            String hourStr) throws Exception {
    if (addEmptyFile) {
      for (Path emptyParentPath : emptyParentPaths) {
        Path emptyFilePath = directoryType.getEmptyFilePath(emptyParentPath, dateStr, hourStr, emptyFileName());
        if (!fileSystem.exists(emptyFilePath)) {
          createFile(fileSystem, emptyFilePath);
          LOG.info("Add Empty File:{}", emptyFilePath.toUri());
        }
      }
    }
  }

  private String emptyFileName() {
    return DEFAULT_EMPTY_FILE_PREFIX +
        "_" +
        jobId +
        ".empty." +
        System.currentTimeMillis();
  }

  private void createFile(FileSystem fileSystem, Path file) throws IOException {
    try (FSDataOutputStream outputStream = fileSystem.create(file, FileSystem.WriteMode.OVERWRITE)) {
      //empty
    }
  }

  /**
   * add output success file.
   */
  public void addOutputSuccessFile() throws Exception {
    FileSystem fileSystem = locationPath.getFileSystem();
    Path successFile = new Path(locationPath, "_SUCCESS");
    if (!fileSystem.exists(successFile)) {
      createFile(fileSystem, successFile);
      LOG.info("Add Output Success File:{}", successFile.toUri());
    }
  }

  public enum SuccessFileType {
    /**
     * Success file type
     */
    DAY_SUCCESS_File,
    HOUR_SUCCESS_File
  }
}
