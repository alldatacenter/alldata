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

package com.netease.arctic.ams.server.handler.impl;

import com.netease.arctic.ams.api.JobId;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OptimizeManager;
import com.netease.arctic.ams.api.OptimizeTask;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.ams.api.OptimizerDescriptor;
import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import com.netease.arctic.ams.api.OptimizerStateReport;
import com.netease.arctic.ams.server.service.ServiceContainer;
import org.apache.thrift.TException;

public class OptimizeManagerHandler implements OptimizeManager.Iface {

  @Override
  public void ping() throws TException {

  }

  @Override
  public OptimizeTask pollTask(int queueId, JobId jobId, String attemptId, long waitTime)
      throws TException {
    return ServiceContainer.getOptimizeQueueService().pollTask(queueId, jobId, attemptId, waitTime);
  }

  @Override
  public void reportOptimizeResult(OptimizeTaskStat optimizeTaskStat) throws NoSuchObjectException {
    ServiceContainer.getOptimizeService().handleOptimizeResult(optimizeTaskStat);
  }

  @Override
  public void reportOptimizerState(OptimizerStateReport reportData) throws TException {
    ServiceContainer.getOptimizerService().updateOptimizerState(reportData);
  }

  @Override
  public OptimizerDescriptor registerOptimizer(OptimizerRegisterInfo registerInfo) throws TException {
    return ServiceContainer.getOptimizerService().registerOptimizer(registerInfo);
  }
}
