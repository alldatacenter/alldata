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

package com.netease.arctic.optimizer.util;

import com.netease.arctic.ams.api.OptimizeManager;
import com.netease.arctic.ams.api.OptimizerDescriptor;
import com.netease.arctic.ams.api.OptimizerRegisterInfo;
import com.netease.arctic.ams.api.client.OptimizeManagerClientPools;
import com.netease.arctic.optimizer.OptimizerConfig;
import org.apache.iceberg.exceptions.ValidationException;

public class OptimizerUtil {

  public static void register(OptimizerConfig config) {
    ValidationException.check(config.getAmsUrl() != null && !config.getAmsUrl().isEmpty(), "ams url is empty");
    ValidationException.check(
        config.getQueueName() != null && !config.getQueueName().isEmpty(),
        "optimizer name is empty");
    ValidationException.check(config.getExecutorParallel() > 0, "optimizer executor parallel must be greater than 0");
    ValidationException.check(config.getExecutorMemory() > 0, "optimizer executor memory must be greater than 0");

    OptimizeManager.Iface client = OptimizeManagerClientPools.getClient(config.getAmsUrl());
    OptimizerRegisterInfo info = new OptimizerRegisterInfo();
    info.setOptimizerGroupName(config.getQueueName());
    info.setCoreNumber(config.getExecutorParallel());
    info.setMemorySize(config.getExecutorMemory());
    try {
      OptimizerDescriptor descriptor = client.registerOptimizer(info);
      config.setOptimizerId(String.valueOf(descriptor.getOptimizerId()));
      config.setQueueId(descriptor.getGroupId());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
