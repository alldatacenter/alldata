/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.properties.OptimizeTaskProperties;
import com.netease.arctic.ams.server.model.BasicOptimizeTask;
import com.netease.arctic.ams.server.model.FilesStatistics;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.model.TaskConfig;
import com.netease.arctic.ams.server.utils.FilesStatisticsBuilder;
import com.netease.arctic.data.file.DataFileWithSequence;
import com.netease.arctic.data.file.DeleteFileWithSequence;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.SequenceNumberFetcher;
import com.netease.arctic.utils.SerializationUtils;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.util.BinPacking;
import org.apache.iceberg.util.PropertyUtil;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * only used for native iceberg
 */
public abstract class AbstractIcebergOptimizePlan extends AbstractOptimizePlan {

  protected List<FileScanTask> fileScanTasks;
  protected SequenceNumberFetcher sequenceNumberFetcher;

  public AbstractIcebergOptimizePlan(ArcticTable arcticTable, TableOptimizeRuntime tableOptimizeRuntime,
                                     List<FileScanTask> fileScanTasks,
                                     int queueId, long currentTime, long currentSnapshotId) {
    super(arcticTable, tableOptimizeRuntime, queueId, currentTime, currentSnapshotId);
    this.fileScanTasks = fileScanTasks;
  }

  protected List<FileScanTask> filterRepeatFileScanTask(Collection<FileScanTask> fileScanTasks) {
    Set<String> dataFilesPath = new HashSet<>();
    List<FileScanTask> finalFileScanTasks = new ArrayList<>();
    for (FileScanTask fileScanTask : fileScanTasks) {
      if (!dataFilesPath.contains(fileScanTask.file().path().toString())) {
        finalFileScanTasks.add(fileScanTask);
        dataFilesPath.add(fileScanTask.file().path().toString());
      }
    }

    return finalFileScanTasks;
  }

  protected List<List<FileScanTask>> binPackFileScanTask(List<FileScanTask> fileScanTasks) {
    long targetFileSize = getTargetSize();

    Long sum = fileScanTasks.stream()
        .map(fileScanTask -> fileScanTask.file().fileSizeInBytes()).reduce(0L, Long::sum);
    int taskCnt = (int) (sum / targetFileSize) + 1;

    return new BinPacking.ListPacker<FileScanTask>(targetFileSize, taskCnt, true)
        .pack(fileScanTasks, fileScanTask -> fileScanTask.file().fileSizeInBytes());
  }

  protected BasicOptimizeTask buildOptimizeTask(List<DataFile> insertFiles,
                                                List<DataFile> baseFiles,
                                                List<DeleteFile> eqDeleteFiles,
                                                List<DeleteFile> posDeleteFiles,
                                                TaskConfig taskConfig) {
    // build task
    BasicOptimizeTask optimizeTask = new BasicOptimizeTask();
    optimizeTask.setTaskCommitGroup(taskConfig.getCommitGroup());
    optimizeTask.setTaskPlanGroup(taskConfig.getPlanGroup());
    optimizeTask.setCreateTime(taskConfig.getCreateTime());

    List<ByteBuffer> baseFileBytesList =
        baseFiles.stream().map(dataFile -> {
          DataFileWithSequence contentFileWithSequenceNumber =
              new DataFileWithSequence(dataFile, seqNumberFetcher().sequenceNumberOf(dataFile.path().toString()));
          return SerializationUtils.toByteBuffer(contentFileWithSequenceNumber);
        }).collect(Collectors.toList());
    List<ByteBuffer> insertFileBytesList =
        insertFiles.stream().map(dataFile -> {
          DataFileWithSequence contentFileWithSequenceNumber =
              new DataFileWithSequence(dataFile, seqNumberFetcher().sequenceNumberOf(dataFile.path().toString()));
          return SerializationUtils.toByteBuffer(contentFileWithSequenceNumber);
        }).collect(Collectors.toList());
    List<ByteBuffer> eqDeleteFileBytesList =
        eqDeleteFiles.stream().map(deleteFile -> {
          DeleteFileWithSequence contentFileWithSequenceNumber =
              new DeleteFileWithSequence(deleteFile, seqNumberFetcher().sequenceNumberOf(deleteFile.path().toString()));
          return SerializationUtils.toByteBuffer(contentFileWithSequenceNumber);
        }).collect(Collectors.toList());
    List<ByteBuffer> posDeleteFileBytesList =
        posDeleteFiles.stream().map(deleteFile -> {
          DeleteFileWithSequence contentFileWithSequenceNumber =
              new DeleteFileWithSequence(deleteFile, seqNumberFetcher().sequenceNumberOf(deleteFile.path().toString()));
          return SerializationUtils.toByteBuffer(contentFileWithSequenceNumber);
        }).collect(Collectors.toList());
    optimizeTask.setBaseFiles(baseFileBytesList);
    optimizeTask.setInsertFiles(insertFileBytesList);
    optimizeTask.setDeleteFiles(eqDeleteFileBytesList);
    optimizeTask.setPosDeleteFiles(posDeleteFileBytesList);

    FilesStatisticsBuilder baseFb = new FilesStatisticsBuilder();
    FilesStatisticsBuilder insertFb = new FilesStatisticsBuilder();
    FilesStatisticsBuilder deleteFb = new FilesStatisticsBuilder();
    FilesStatisticsBuilder posDeleteFb = new FilesStatisticsBuilder();
    baseFiles.stream().map(DataFile::fileSizeInBytes)
        .forEach(baseFb::addFile);
    insertFiles.stream().map(DataFile::fileSizeInBytes)
        .forEach(insertFb::addFile);
    eqDeleteFiles.stream().map(DeleteFile::fileSizeInBytes)
        .forEach(deleteFb::addFile);
    posDeleteFiles.stream().map(DeleteFile::fileSizeInBytes)
        .forEach(posDeleteFb::addFile);

    FilesStatistics baseFs = baseFb.build();
    FilesStatistics insertFs = insertFb.build();
    FilesStatistics deleteFs = deleteFb.build();
    FilesStatistics posDeleteFs = posDeleteFb.build();

    // file size
    optimizeTask.setBaseFileSize(baseFs.getTotalSize());
    optimizeTask.setInsertFileSize(insertFs.getTotalSize());
    optimizeTask.setDeleteFileSize(deleteFs.getTotalSize());
    optimizeTask.setPosDeleteFileSize(posDeleteFs.getTotalSize());

    // file count
    optimizeTask.setBaseFileCnt(baseFs.getFileCnt());
    optimizeTask.setInsertFileCnt(insertFs.getFileCnt());
    optimizeTask.setDeleteFileCnt(deleteFs.getFileCnt());
    optimizeTask.setPosDeleteFileCnt(posDeleteFs.getFileCnt());

    optimizeTask.setPartition(taskConfig.getPartition());
    optimizeTask.setQueueId(queueId);
    optimizeTask.setTaskId(new OptimizeTaskId(taskConfig.getOptimizeType(), UUID.randomUUID().toString()));
    optimizeTask.setTableIdentifier(arcticTable.id().buildTableIdentifier());

    // table ams url
    Map<String, String> properties = new HashMap<>();
    properties.put(OptimizeTaskProperties.ALL_FILE_COUNT, (optimizeTask.getBaseFiles().size() +
        optimizeTask.getInsertFiles().size() + optimizeTask.getDeleteFiles().size()) +
        optimizeTask.getPosDeleteFiles().size() + "");
    optimizeTask.setProperties(properties);
    return optimizeTask;
  }

  protected SequenceNumberFetcher seqNumberFetcher() {
    if (null == sequenceNumberFetcher) {
      sequenceNumberFetcher = new SequenceNumberFetcher(arcticTable.asUnkeyedTable(), getCurrentSnapshotId());
    }
    return sequenceNumberFetcher;
  }

  private long getTargetSize() {
    return PropertyUtil.propertyAsLong(arcticTable.properties(),
        TableProperties.SELF_OPTIMIZING_TARGET_SIZE,
        TableProperties.SELF_OPTIMIZING_TARGET_SIZE_DEFAULT);
  }

  protected void getOptimizeFile(List<FileScanTask> fileScanTasks,
                               List<DataFile> dataFiles,
                               List<DeleteFile> eqDeleteFiles,
                               List<DeleteFile> posDeleteFiles) {
    Set<String> dataFilesPath = new HashSet<>();
    Set<String> deleteFilesPath = new HashSet<>();

    for (FileScanTask fileScanTask : fileScanTasks) {
      // filter repeat data files
      if (!dataFilesPath.contains(fileScanTask.file().path().toString())) {
        dataFiles.add(fileScanTask.file());
        dataFilesPath.add(fileScanTask.file().path().toString());
      }
      for (DeleteFile delete : fileScanTask.deletes()) {
        // filter repeat delete files
        if (!deleteFilesPath.contains(delete.path().toString())) {
          if (delete.content() == FileContent.POSITION_DELETES) {
            posDeleteFiles.add(delete);
          } else {
            eqDeleteFiles.add(delete);
          }
          deleteFilesPath.add(delete.path().toString());
        }
      }
    }
  }
}