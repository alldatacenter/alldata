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

import com.netease.arctic.ams.api.JobId;
import com.netease.arctic.ams.api.JobType;
import com.netease.arctic.ams.api.OptimizeStatus;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.PrimaryKeyedFile;
import com.netease.arctic.data.file.FileNameGenerator;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.exception.TimeoutException;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.SerializationUtils;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DeleteFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalLong;
import java.util.stream.Collectors;

public abstract class AbstractExecutor implements Executor {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractExecutor.class);
  protected static final int SAMPLE_DATA_INTERVAL = 100000;

  protected final NodeTask task;
  protected final ArcticTable table;
  protected final long startTime;
  protected final OptimizerConfig config;
  protected double factor = 0.9;

  protected final StructLikeCollections structLikeCollections;

  public AbstractExecutor(NodeTask task, ArcticTable table, long startTime, OptimizerConfig config) {
    this.task = task;
    this.table = table;
    this.startTime = startTime;
    this.config = config;
    this.structLikeCollections = new StructLikeCollections(Boolean.parseBoolean(config.getEnableSpillMap()),
          config.getMaxInMemorySize() * 1024 * 1024, config.getRocksDBBasePath());
  }

  protected Map<DataTreeNode, List<PrimaryKeyedFile>> groupDataFilesByNode(List<PrimaryKeyedFile> dataFiles) {
    return new HashMap<>(dataFiles.stream().collect(Collectors.groupingBy(dataFile ->
        dataFile.node())));
  }

  protected Map<DataTreeNode, List<DeleteFile>> groupDeleteFilesByNode(List<? extends DeleteFile> deleteFiles) {
    return new HashMap<>(deleteFiles.stream().collect(Collectors.groupingBy(deleteFile ->
        FileNameGenerator.parseFileNodeFromFileName(deleteFile.path().toString()))));
  }

  protected long getMaxTransactionId(List<PrimaryKeyedFile> dataFiles) {
    OptionalLong maxTransactionId = dataFiles.stream()
        .mapToLong(PrimaryKeyedFile::transactionId).max();
    if (maxTransactionId.isPresent()) {
      return maxTransactionId.getAsLong();
    }

    return 0;
  }

  protected OptimizeTaskResult buildOptimizeResult(Iterable<? extends ContentFile<?>> targetFiles) {
    long totalFileSize = 0;
    List<ByteBuffer> baseFileBytesList = new ArrayList<>();
    for (ContentFile<?> targetFile : targetFiles) {
      totalFileSize += targetFile.fileSizeInBytes();
      baseFileBytesList.add(SerializationUtils.toByteBuffer(targetFile));
    }
    JobId jobId = new JobId();
    jobId.setId(config.getOptimizerId());
    jobId.setType(JobType.Optimize);

    OptimizeTaskStat optimizeTaskStat = new OptimizeTaskStat();

    optimizeTaskStat.setJobId(jobId);
    optimizeTaskStat.setTableIdentifier(task.getTableIdentifier().buildTableIdentifier());
    optimizeTaskStat.setAttemptId(task.getAttemptId() + "");
    optimizeTaskStat.setTaskId(task.getTaskId());
    optimizeTaskStat.setStatus(OptimizeStatus.Prepared);
    optimizeTaskStat.setFiles(baseFileBytesList);
    optimizeTaskStat.setErrorMessage(null);
    optimizeTaskStat.setNewFileSize(totalFileSize);
    optimizeTaskStat.setReportTime(System.currentTimeMillis());
    optimizeTaskStat.setCostTime(System.currentTimeMillis() - startTime);

    OptimizeTaskResult result = new OptimizeTaskResult();
    result.setTargetFiles(targetFiles);
    result.setOptimizeTaskStat(optimizeTaskStat);
    return result;
  }

  protected void checkIfTimeout(Closeable writer) throws Exception {
    long maxExecuteTime = task.getMaxExecuteTime() != null ?
        task.getMaxExecuteTime() : TableProperties.SELF_OPTIMIZING_EXECUTE_TIMEOUT_DEFAULT;
    long actualExecuteTime = System.currentTimeMillis() - startTime;
    if (actualExecuteTime > maxExecuteTime * factor) {
      writer.close();
      LOG.error("table {} execute task {} timeout, actual execute time is {}ms, max execute time is {}ms",
          table.id(), task.getTaskId(), actualExecuteTime, task.getMaxExecuteTime());
      throw new TimeoutException(String.format("optimizer execute timeout, " +
          "actual execute time is %sms, max execute time is %sms, factor is %s",
          actualExecuteTime, task.getMaxExecuteTime(), factor));
    }
  }
}
