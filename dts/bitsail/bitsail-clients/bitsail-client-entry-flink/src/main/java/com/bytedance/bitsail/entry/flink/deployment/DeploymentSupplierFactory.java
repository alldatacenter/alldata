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

package com.bytedance.bitsail.entry.flink.deployment;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.entry.flink.command.FlinkRunCommandArgs;
import com.bytedance.bitsail.entry.flink.deployment.local.LocalDeploymentSupplier;
import com.bytedance.bitsail.entry.flink.deployment.yarn.YarnDeploymentSupplier;

/**
 * Created 2022/8/8
 */
public class DeploymentSupplierFactory {

  private static final String DEPLOYMENT_LOCAL = "local";
  private static final String DEPLOYMENT_REMOTE = "remote";
  private static final String DEPLOYMENT_YARN_PER_JOB = "yarn-per-job";
  private static final String DEPLOYMENT_YARN_SESSION = "yarn-session";
  private static final String DEPLOYMENT_YARN_APPLICATION = "yarn-application";

  public DeploymentSupplier getDeploymentSupplier(FlinkRunCommandArgs flinkCommandArgs, BitSailConfiguration jobConfiguration) {
    String deploymentMode = flinkCommandArgs.getDeploymentMode().toLowerCase().trim();

    switch (deploymentMode) {
      case DEPLOYMENT_LOCAL:
      case DEPLOYMENT_REMOTE:
        return new LocalDeploymentSupplier(flinkCommandArgs);
      case DEPLOYMENT_YARN_PER_JOB:
      case DEPLOYMENT_YARN_SESSION:
      case DEPLOYMENT_YARN_APPLICATION:
        return new YarnDeploymentSupplier(flinkCommandArgs, jobConfiguration);
      default:
        throw new UnsupportedOperationException("Unsupported deployment mode: " + deploymentMode);
    }
  }
}
