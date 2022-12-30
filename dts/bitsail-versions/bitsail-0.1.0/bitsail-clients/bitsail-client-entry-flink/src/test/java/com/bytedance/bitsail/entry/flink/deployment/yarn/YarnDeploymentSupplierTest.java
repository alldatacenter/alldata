/*
 * Copyright 2022 ByteDance and/or its affiliates
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
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
import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.entry.flink.command.FlinkRunCommandArgs;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class YarnDeploymentSupplierTest {

  @Test
  public void testAddDeploymentCommands() {
    String deploymentMode = "yarn-per-job";
    FlinkRunCommandArgs flinkRunCommandArgs = new FlinkRunCommandArgs();
    flinkRunCommandArgs.setQueue("test");
    flinkRunCommandArgs.setDeploymentMode(deploymentMode);

    BitSailConfiguration conf = BitSailConfiguration.newDefault();
    conf.set(CommonOptions.JOB_NAME, "test");

    YarnDeploymentSupplier deploymentSupplier = new YarnDeploymentSupplier(flinkRunCommandArgs, conf);
    BaseCommandArgs baseCommandArgs = new BaseCommandArgs();
    List<String> flinkCommands = new ArrayList<>();
    deploymentSupplier.addDeploymentCommands(baseCommandArgs, flinkCommands);
    assertEquals(flinkCommands.size(), 2);
    assertEquals(flinkCommands.get(1), deploymentMode);

    Map<String, String> properties = baseCommandArgs.getProperties();
    assertEquals(properties.size(), 3);
  }
}
