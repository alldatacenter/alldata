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

package com.bytedance.bitsail.entry.flink.deployment.yarn;

import com.bytedance.bitsail.client.api.command.BaseCommandArgs;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.exception.CommonErrorCode;
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.entry.flink.command.FlinkRunCommandArgs;
import com.bytedance.bitsail.entry.flink.deployment.DeploymentSupplier;

import java.util.List;

/**
 * Created 2022/8/8
 */
public class YarnDeploymentSupplier implements DeploymentSupplier {

  private FlinkRunCommandArgs flinkCommandArgs;

  private BitSailConfiguration jobConfiguration;

  private String deploymentMode;

  public YarnDeploymentSupplier(FlinkRunCommandArgs flinkCommandArgs, BitSailConfiguration jobConfiguration) {
    this.flinkCommandArgs = flinkCommandArgs;
    this.jobConfiguration = jobConfiguration;
    this.deploymentMode = flinkCommandArgs.getDeploymentMode();
  }

  @Override
  public void addDeploymentCommands(BaseCommandArgs baseCommandArgs, List<String> flinkCommands) {
    flinkCommands.add("-t");
    flinkCommands.add(deploymentMode);

    baseCommandArgs.getProperties()
        .put("yarn.application.name", jobConfiguration.getNecessaryOption(
            CommonOptions.JOB_NAME, CommonErrorCode.CONFIG_ERROR));

    baseCommandArgs.getProperties()
        .put("yarn.application.queue", flinkCommandArgs.getQueue());

    baseCommandArgs.getProperties()
        .put("yarn.application.priority", String.valueOf(flinkCommandArgs.getPriority()));
  }
}
