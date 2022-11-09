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

package com.bytedance.bitsail.flink.core.execution.configurer;

import com.bytedance.bitsail.base.runtime.RuntimePlugin;
import com.bytedance.bitsail.flink.core.FlinkJobMode;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class BitSailRuntimePluginConfigurerTest {

  @Test
  public void testGetRuntimePlugins() {
    BitSailRuntimePluginConfigurer pluginConfigurer =
        new BitSailRuntimePluginConfigurer(FlinkJobMode.BATCH);
    List<RuntimePlugin> pluginList = pluginConfigurer.getRuntimePlugins();
    assertEquals(pluginList.size(), 2);

    pluginConfigurer = new BitSailRuntimePluginConfigurer(FlinkJobMode.STREAMING);
    pluginList = pluginConfigurer.getRuntimePlugins();
    assertEquals(pluginList.size(), 1);
  }
}
