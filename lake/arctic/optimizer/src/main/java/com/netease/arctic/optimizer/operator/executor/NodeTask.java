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

package com.netease.arctic.optimizer.operator.executor;

import com.google.common.collect.Iterables;
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.data.file.DataFileWithSequence;
import com.netease.arctic.data.file.DeleteFileWithSequence;
import com.netease.arctic.table.TableIdentifier;
import org.apache.commons.compress.utils.Lists;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NodeTask {
  private static final Logger LOG = LoggerFactory.getLogger(NodeTask.class);
  private List<ContentFile<?>> allFiles;
  private List<PrimaryKeyedFile> dataFiles;
  private final List<PrimaryKeyedFile> baseFiles = new ArrayList<>();
  private final List<PrimaryKeyedFile> insertFiles = new ArrayList<>();
  private final List<PrimaryKeyedFile> deleteFiles = new ArrayList<>();
  private final List<DeleteFileWithSequence> posDeleteFiles = new ArrayList<>();
  private final List<DataFileWithSequence> icebergDataFiles = new ArrayList<>();
  private final List<DataFileWithSequence> icebergSmallDataFiles = new ArrayList<>();
  private final List<DeleteFileWithSequence> icebergEqDeleteFiles = new ArrayList<>();
  private final List<DeleteFileWithSequence> icebergPosDeleteFiles = new ArrayList<>();
  private Set<DataTreeNode> sourceNodes;
  private StructLike partition;
  private OptimizeTaskId taskId;
  private TableIdentifier tableIdentifier;
  private int attemptId;
  private String customHiveSubdirectory;
  private Long maxExecuteTime;

  public NodeTask(List<ContentFileWithSequence<?>> baseFiles,
      List<ContentFileWithSequence<?>> insertFiles,
      List<ContentFileWithSequence<?>> eqDeletes,
      List<ContentFileWithSequence<?>> posDeletes, boolean isMixed) {
    if (isMixed) {
      if (baseFiles != null) {
        baseFiles.forEach(s -> addFile(s, DataFileType.BASE_FILE));
      }
      if (insertFiles != null) {
        insertFiles.forEach(s -> addFile(s, DataFileType.INSERT_FILE));
      }
      if (eqDeletes != null) {
        eqDeletes.forEach(s -> addFile(s, DataFileType.EQ_DELETE_FILE));
      }
      if (posDeletes != null) {
        posDeletes.forEach(s -> addFile(s, DataFileType.POS_DELETE_FILE));
      }
    } else {
      if (baseFiles != null) {
        baseFiles.forEach(s -> addIcebergFile(s, DataFileType.BASE_FILE));
      }
      if (insertFiles != null) {
        insertFiles.forEach(s -> addIcebergFile(s, DataFileType.INSERT_FILE));
      }
      if (eqDeletes != null) {
        eqDeletes.forEach(s -> addIcebergFile(s, DataFileType.EQ_DELETE_FILE));
      }
      if (posDeletes != null) {
        posDeletes.forEach(s -> addIcebergFile(s, DataFileType.POS_DELETE_FILE));
      }
    }
  }

  public StructLike getPartition() {
    return partition;
  }

  public void setPartition(StructLike partition) {
    this.partition = partition;
  }

  private void addFile(ContentFileWithSequence<?> file, DataFileType fileType) {
    if (fileType == null) {
      LOG.warn("file type is null");
      return;
    }

    switch (fileType) {
      case BASE_FILE:
        baseFiles.add(DefaultKeyedFile.parseBase((DataFileWithSequence) file));
        break;
      case INSERT_FILE:
        insertFiles.add(DefaultKeyedFile.parseChange((DataFileWithSequence) file, file.getSequenceNumber()));
        break;
      case EQ_DELETE_FILE:
        deleteFiles.add(DefaultKeyedFile.parseChange((DataFileWithSequence) file, file.getSequenceNumber()));
        break;
      case POS_DELETE_FILE:
        posDeleteFiles.add((DeleteFileWithSequence) file);
        break;
      default:
        LOG.warn("file type is {}, not add in node", fileType);
        // ignore the object
    }
  }

  private void addIcebergFile(ContentFileWithSequence contentFile, DataFileType fileType) {
    if (fileType == null) {
      LOG.warn("file type is null");
      return;
    }

    switch (fileType) {
      case BASE_FILE:
        icebergDataFiles.add((DataFileWithSequence) contentFile);
        break;
      case INSERT_FILE:
        icebergSmallDataFiles.add((DataFileWithSequence) contentFile);
        break;
      case ICEBERG_EQ_DELETE_FILE:
      case EQ_DELETE_FILE:
        icebergEqDeleteFiles.add((DeleteFileWithSequence) contentFile);
        break;
      case POS_DELETE_FILE:
        icebergPosDeleteFiles.add((DeleteFileWithSequence) contentFile);
        break;
      default:
        LOG.warn("file type is {}, not add in node", fileType);
        // ignore the object
    }
  }

  public List<PrimaryKeyedFile> dataFiles() {
    if (dataFiles == null) {
      dataFiles = Lists.newArrayList();
      Iterables.addAll(dataFiles, baseFiles);
      Iterables.addAll(dataFiles, insertFiles);
    }
    return dataFiles;
  }

  public List<ContentFile<?>> files() {
    if (allFiles == null) {
      allFiles = Lists.newArrayList();
      Iterables.addAll(allFiles, baseFiles);
      Iterables.addAll(allFiles, insertFiles);
      Iterables.addAll(allFiles, deleteFiles);
      Iterables.addAll(allFiles, posDeleteFiles);
      for (ContentFileWithSequence<?> icebergDataFile : icebergDataFiles) {
        allFiles.add(icebergDataFile);
      }
      for (ContentFileWithSequence<?> icebergDataFile : icebergSmallDataFiles) {
        allFiles.add(icebergDataFile);
      }
      for (ContentFileWithSequence<?> icebergDataFile : icebergEqDeleteFiles) {
        allFiles.add(icebergDataFile);
      }
      for (ContentFileWithSequence<?> icebergDataFile : icebergPosDeleteFiles) {
        allFiles.add(icebergDataFile);
      }
    }
    return allFiles;
  }

  public List<PrimaryKeyedFile> baseFiles() {
    return baseFiles;
  }

  public List<PrimaryKeyedFile> insertFiles() {
    return insertFiles;
  }

  public List<PrimaryKeyedFile> deleteFiles() {
    return deleteFiles;
  }

  public List<DeleteFileWithSequence> posDeleteFiles() {
    return posDeleteFiles;
  }

  public List<DataFileWithSequence> icebergDataFiles() {
    return icebergDataFiles;
  }

  public List<DataFileWithSequence> allIcebergDataFiles() {
    List<DataFileWithSequence> allIcebergDataFiles = Lists.newArrayList();
    allIcebergDataFiles.addAll(icebergDataFiles);
    allIcebergDataFiles.addAll(icebergSmallDataFiles);
    return allIcebergDataFiles;
  }

  public List<DeleteFileWithSequence> allIcebergDeleteFiles() {
    List<DeleteFileWithSequence> allIcebergDeleteFiles = Lists.newArrayList();
    allIcebergDeleteFiles.addAll(icebergEqDeleteFiles);
    allIcebergDeleteFiles.addAll(icebergPosDeleteFiles);
    return allIcebergDeleteFiles;
  }

  public Set<DataTreeNode> getSourceNodes() {
    return sourceNodes;
  }

  public void setSourceNodes(Set<DataTreeNode> sourceNodes) {
    this.sourceNodes = sourceNodes;
  }

  public OptimizeTaskId getTaskId() {
    return taskId;
  }

  public void setTaskId(OptimizeTaskId taskId) {
    this.taskId = taskId;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public int getAttemptId() {
    return attemptId;
  }

  public void setAttemptId(int attemptId) {
    this.attemptId = attemptId;
  }

  public String getCustomHiveSubdirectory() {
    return customHiveSubdirectory;
  }

  public void setCustomHiveSubdirectory(String customHiveSubdirectory) {
    this.customHiveSubdirectory = customHiveSubdirectory;
  }

  public Long getMaxExecuteTime() {
    return maxExecuteTime;
  }

  public void setMaxExecuteTime(Long maxExecuteTime) {
    this.maxExecuteTime = maxExecuteTime;
  }

  public OptimizeType getOptimizeType() {
    return taskId.getType();
  }


  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("taskId", taskId)
        .add("attemptId", attemptId)
        .add("tableIdentifier", tableIdentifier)
        .add("baseFiles", baseFiles.size())
        .add("insertFiles", insertFiles.size())
        .add("deleteFiles", deleteFiles.size())
        .add("posDeleteFiles", posDeleteFiles.size())
        .add("icebergDataFiles", icebergDataFiles.size())
        .add("icebergSmallDataFiles", icebergSmallDataFiles.size())
        .add("icebergEqDeleteFiles", icebergEqDeleteFiles.size())
        .add("icebergPosDeleteFiles", icebergPosDeleteFiles.size())
        .add("customHiveSubdirectory", customHiveSubdirectory)
        .add("maxExecuteTime", maxExecuteTime)
        .toString();
  }
}