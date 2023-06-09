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


import com.netease.arctic.ams.api.MockArcticMetastoreServer;
import com.netease.arctic.ams.api.OptimizeStatus;
import com.netease.arctic.ams.api.OptimizeTask;
import com.netease.arctic.ams.api.OptimizeTaskId;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.api.properties.OptimizeTaskProperties;
import com.netease.arctic.optimizer.OptimizerConfig;
import com.netease.arctic.optimizer.TaskWrapper;
import com.netease.arctic.optimizer.operator.executor.TestIcebergExecutorBase;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class BaseTaskExecutorTest extends TestIcebergExecutorBase {
  private static final String JOB_ID = "testOptimizerId";

  private OptimizerConfig config;

  @Before
  public void before() {
    String amsUrl = MockArcticMetastoreServer.getInstance().getServerUrl();
    this.config = new OptimizerConfig();
    this.config.setAmsUrl(amsUrl);
    this.config.setOptimizerId(JOB_ID);
  }

  @Test
  public void testExecuteSuccess() {
    BaseTaskExecutor baseTaskExecutor = new BaseTaskExecutor(this.config);
    TableIdentifier tableIdentifier = icebergTable.id().buildTableIdentifier();
    OptimizeTaskId taskId = new OptimizeTaskId(OptimizeType.FullMajor, UUID.randomUUID().toString());
    OptimizeTask optimizeTask = buildOptimizeTask(tableIdentifier, taskId);
    int attemptId = 100;
    TaskWrapper taskWrapper = new TaskWrapper(optimizeTask, attemptId);

    OptimizeTaskStat optimizeTaskStat = baseTaskExecutor.execute(taskWrapper);
    assertOptimizeResult(tableIdentifier, taskId, attemptId, optimizeTaskStat, OptimizeStatus.Prepared);
  }

  @Test
  public void testExecuteFailed() {
    BaseTaskExecutor baseTaskExecutor = new BaseTaskExecutor(this.config);
    TableIdentifier tableIdentifier = new TableIdentifier("fake", "fake", "fake");
    OptimizeTaskId taskId = new OptimizeTaskId(OptimizeType.FullMajor, UUID.randomUUID().toString());
    OptimizeTask optimizeTask = buildOptimizeTask(tableIdentifier, taskId);
    int attemptId = 100;
    TaskWrapper taskWrapper = new TaskWrapper(optimizeTask, attemptId);

    OptimizeTaskStat optimizeTaskStat = baseTaskExecutor.execute(taskWrapper);
    assertOptimizeResult(tableIdentifier, taskId, attemptId, optimizeTaskStat, OptimizeStatus.Failed);
  }

  private void assertOptimizeResult(TableIdentifier tableIdentifier, OptimizeTaskId taskId, int attemptId,
                                    OptimizeTaskStat optimizeTaskStat, OptimizeStatus status) {
    Assert.assertEquals(status, optimizeTaskStat.getStatus());
    Assert.assertEquals(taskId, optimizeTaskStat.getTaskId());
    Assert.assertEquals(tableIdentifier, optimizeTaskStat.getTableIdentifier());
    Assert.assertEquals(attemptId + "", optimizeTaskStat.getAttemptId());
    Assert.assertEquals(0, optimizeTaskStat.getFiles().size());
    Assert.assertEquals(0, optimizeTaskStat.getNewFileSize());
  }

  private OptimizeTask buildOptimizeTask(TableIdentifier tableIdentifier, OptimizeTaskId taskId) {
    Map<String, String> properties = new HashMap<>();
    properties.put(OptimizeTaskProperties.ALL_FILE_COUNT, "0");

    OptimizeTask optimizeTask = new OptimizeTask();
    optimizeTask.setTableIdentifier(tableIdentifier);
    optimizeTask.setTaskId(taskId);
    optimizeTask.setInsertFiles(Collections.emptyList());
    optimizeTask.setDeleteFiles(Collections.emptyList());
    optimizeTask.setBaseFiles(Collections.emptyList());
    optimizeTask.setPosDeleteFiles(Collections.emptyList());
    optimizeTask.setProperties(properties);
    return optimizeTask;
  }
}