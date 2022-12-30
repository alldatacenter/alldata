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

package com.bytedance.bitsail.entry.flink.deployment.local;

import com.bytedance.bitsail.client.api.command.BaseCommandArgs;
import com.bytedance.bitsail.entry.flink.command.FlinkRunCommandArgs;
import com.bytedance.bitsail.entry.flink.deployment.DeploymentSupplier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LocalDeploymentSupplier implements DeploymentSupplier {
  private static final Logger LOG = LoggerFactory.getLogger(LocalDeploymentSupplier.class);

  private final FlinkRunCommandArgs flinkCommandArgs;

  public LocalDeploymentSupplier(FlinkRunCommandArgs flinkCommandArgs) {
    this.flinkCommandArgs = flinkCommandArgs;
  }

  @Override
  public void addDeploymentCommands(BaseCommandArgs baseCommandArgs, List<String> flinkCommands) {
    String jobManagerAddress = flinkCommandArgs.getJobManagerAddress();
    if (StringUtils.isNotEmpty(jobManagerAddress)) {
      flinkCommands.add("-m");
      flinkCommands.add(jobManagerAddress);
    } else {
      LOG.info("Job manager is not specified. Job will be submit to default job manager.");
    }
  }
}
