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

package com.netease.arctic.optimizer.operator;

import com.netease.arctic.ams.api.ErrorMessage;
import com.netease.arctic.ams.api.JobId;
import com.netease.arctic.ams.api.JobType;
import com.netease.arctic.ams.api.OptimizeStatus;
import com.netease.arctic.ams.api.OptimizeTask;
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.api.properties.OptimizeTaskProperties;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.file.ContentFileWithSequence;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.TaskWrapper;
import com.netease.arctic.optimizer.operator.executor.Executor;
import com.netease.arctic.optimizer.operator.executor.ExecutorFactory;
import com.netease.arctic.optimizer.operator.executor.NodeTask;
import com.netease.arctic.optimizer.operator.executor.OptimizeTaskResult;
import com.netease.arctic.optimizer.operator.executor.TableIdentificationInfo;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.SerializationUtils;
import com.netease.arctic.utils.TableTypeUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Execute task.
 */
public class BaseTaskExecutor implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(BaseTaskExecutor.class);

  private final OptimizerConfig config;

  private final ExecuteListener listener;

  public BaseTaskExecutor(OptimizerConfig config) {
    this(config, null);
  }

  public BaseTaskExecutor(
      OptimizerConfig config,
      ExecuteListener listener) {
    this.config = config;
    this.listener = listener;
  }

  private static ArcticTable buildTable(TableIdentificationInfo tableIdentifierInfo) {
    String amsUrl = tableIdentifierInfo.getAmsUrl();
    amsUrl = amsUrl.trim();
    if (!amsUrl.endsWith("/")) {
      amsUrl = amsUrl + "/";
    }
    ArcticCatalog arcticCatalog = CatalogLoader.load(amsUrl + tableIdentifierInfo.getTableIdentifier().getCatalog());
    return arcticCatalog.loadTable(tableIdentifierInfo.getTableIdentifier());
  }

  private static DataTreeNode toTreeNode(com.netease.arctic.ams.api.TreeNode treeNode) {
    if (treeNode == null) {
      return null;
    }
    return DataTreeNode.of(treeNode.getMask(), treeNode.getIndex());
  }

  public static com.netease.arctic.table.TableIdentifier toTableIdentifier(
      TableIdentifier tableIdentifier) {
    return com.netease.arctic.table.TableIdentifier.of(tableIdentifier.getCatalog(),
        tableIdentifier.getDatabase(), tableIdentifier.getTableName());
  }

  /**
   * Execute task.
   *
   * @param sourceTask -
   * @return task execute result
   */
  public OptimizeTaskStat execute(TaskWrapper sourceTask) {
    TableIdentifier tableIdentifier = sourceTask.getTask().getTableIdentifier();
    OptimizeTaskId taskId = sourceTask.getTask().getTaskId();

    long startTime = System.currentTimeMillis();
    LOG.info("start execute {} of {}", taskId, tableIdentifier);
    Executor optimize = null;
    try {
      String amsUrl = config.getAmsUrl();
      ArcticTable table = buildTable(
          new TableIdentificationInfo(amsUrl, toTableIdentifier(sourceTask.getTask().getTableIdentifier())));
      NodeTask task = constructTask(table, sourceTask.getTask(), sourceTask.getAttemptId());
      onTaskStart(task.files());
      optimize = ExecutorFactory.constructOptimize(task, table, startTime, config);
      OptimizeTaskResult result = optimize.execute();
      onTaskFinish(result.getTargetFiles());
      return result.getOptimizeTaskStat();
    } catch (Throwable t) {
      LOG.error("failed to execute task {} of {}", taskId, tableIdentifier, t);
      onTaskFailed(t);
      return constructFailedResult(tableIdentifier, taskId, config.getOptimizerId(), sourceTask.getAttemptId(),
          startTime, t);
    } finally {
      if (optimize != null) {
        optimize.close();
      }
    }
  }

  private void onTaskStart(Iterable<ContentFile<?>> inputFiles) {
    if (listener != null) {
      listener.onTaskStart(inputFiles);
    }
  }

  private void onTaskFinish(Iterable<? extends ContentFile<?>> outputFiles) {
    List<ContentFile<?>> targetFiles = new ArrayList<>();
    for (ContentFile<?> outputFile : outputFiles) {
      targetFiles.add(outputFile);
    }
    if (listener != null) {
      listener.onTaskFinish(targetFiles);
    }
  }

  private void onTaskFailed(Throwable t) {
    if (listener != null) {
      listener.onTaskFailed(t);
    }
  }

  private static OptimizeTaskStat constructFailedResult(TableIdentifier tableIdentifier,
                                                        OptimizeTaskId taskId,
                                                        String optimizerId,
                                                        int attemptId,
                                                        long startTime,
                                                        Throwable t) {
    JobId jobId = new JobId();
    jobId.setId(optimizerId);
    jobId.setType(JobType.Optimize);
    long now = System.currentTimeMillis();

    OptimizeTaskStat optimizeTaskStat = new OptimizeTaskStat();
    optimizeTaskStat.setJobId(jobId);
    optimizeTaskStat.setTableIdentifier(tableIdentifier);
    optimizeTaskStat.setAttemptId(attemptId + "");
    optimizeTaskStat.setTaskId(taskId);
    optimizeTaskStat.setStatus(OptimizeStatus.Failed);
    optimizeTaskStat.setFiles(Collections.emptyList());
    optimizeTaskStat.setErrorMessage(new ErrorMessage(System.currentTimeMillis(), buildErrorMessage(t, 3)));
    optimizeTaskStat.setNewFileSize(0);
    optimizeTaskStat.setReportTime(now);
    optimizeTaskStat.setCostTime(now - startTime);
    return optimizeTaskStat;
  }

  private static String buildErrorMessage(Throwable t, int deep) {
    StringBuilder message = new StringBuilder();
    Throwable error = t;
    int i = 0;
    while (i++ < deep && error != null) {
      if (i > 1) {
        message.append(". caused by ");
      }
      message.append(error.getMessage());
      error = error.getCause();
    }
    String result = message.toString();
    return result.length() > 4000 ? result.substring(0, 4000) : result;
  }

  private NodeTask constructTask(ArcticTable table, OptimizeTask task, int attemptId) {
    NodeTask nodeTask;
    if (TableTypeUtil.isIcebergTableFormat(table)) {
      List<ContentFileWithSequence<?>> base =
          task.getBaseFiles().stream().map(SerializationUtils::toIcebergContentFile).collect(Collectors.toList());
      List<ContentFileWithSequence<?>> insert =
          task.getInsertFiles().stream().map(SerializationUtils::toIcebergContentFile).collect(Collectors.toList());
      List<ContentFileWithSequence<?>> eqDelete =
          task.getDeleteFiles().stream().map(SerializationUtils::toIcebergContentFile).collect(Collectors.toList());
      List<ContentFileWithSequence<?>> posDelete =
          task.getPosDeleteFiles().stream().map(SerializationUtils::toIcebergContentFile).collect(Collectors.toList());
      nodeTask = new NodeTask(base, insert, eqDelete, posDelete, false);
    } else {
      List<ContentFileWithSequence<?>> base =
          task.getBaseFiles().stream().map(SerializationUtils::toInternalTableFile).collect(Collectors.toList());
      List<ContentFileWithSequence<?>> insert =
          task.getInsertFiles().stream().map(SerializationUtils::toInternalTableFile).collect(Collectors.toList());
      List<ContentFileWithSequence<?>> eqDelete =
          task.getDeleteFiles().stream().map(SerializationUtils::toInternalTableFile).collect(Collectors.toList());
      List<ContentFileWithSequence<?>> posDelete =
          task.getPosDeleteFiles().stream().map(SerializationUtils::toInternalTableFile).collect(Collectors.toList());
      nodeTask = new NodeTask(base, insert, eqDelete, posDelete, true);
    }

    if (nodeTask.files().size() > 0) {
      nodeTask.setPartition(nodeTask.files().get(0).partition());
    }

    if (CollectionUtils.isNotEmpty(task.getSourceNodes())) {
      nodeTask.setSourceNodes(
          task.getSourceNodes().stream().map(BaseTaskExecutor::toTreeNode).collect(Collectors.toSet()));
    }
    nodeTask.setTableIdentifier(toTableIdentifier(task.getTableIdentifier()));
    nodeTask.setTaskId(task.getTaskId());
    nodeTask.setAttemptId(attemptId);

    Map<String, String> properties = task.getProperties();
    if (properties != null) {
      String allFileCnt = properties.get(OptimizeTaskProperties.ALL_FILE_COUNT);
      int fileCnt = nodeTask.baseFiles().size() + nodeTask.insertFiles().size() +
          nodeTask.deleteFiles().size() + nodeTask.posDeleteFiles().size() +
          nodeTask.allIcebergDataFiles().size() + nodeTask.allIcebergDeleteFiles().size();
      if (allFileCnt != null && Integer.parseInt(allFileCnt) != fileCnt) {
        LOG.error("{} check file cnt error, expected {}, actual {}, {}, value = {}", task.getTaskId(), allFileCnt,
            fileCnt, nodeTask, task);
        throw new IllegalStateException("check file cnt error");
      }

      String customHiveSubdirectory = properties.get(OptimizeTaskProperties.CUSTOM_HIVE_SUB_DIRECTORY);
      nodeTask.setCustomHiveSubdirectory(customHiveSubdirectory);

      Long maxExecuteTime = PropertyUtil.propertyAsLong(properties,
          OptimizeTaskProperties.MAX_EXECUTE_TIME, TableProperties.SELF_OPTIMIZING_EXECUTE_TIMEOUT_DEFAULT);
      nodeTask.setMaxExecuteTime(maxExecuteTime);
    }

    return nodeTask;
  }

  public interface ExecuteListener {
    default void onTaskStart(Iterable<ContentFile<?>> inputFiles) {
    }

    default void onTaskFinish(Iterable<ContentFile<?>> outputFiles) {
    }

    default void onTaskFailed(Throwable t) {
    }
  }
}