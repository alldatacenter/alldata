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

package com.bytedance.bitsail.flink.core.execution.configurer;

import com.bytedance.bitsail.base.runtime.RuntimePlugin;
import com.bytedance.bitsail.flink.core.FlinkJobMode;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@AllArgsConstructor
public class BitSailRuntimePluginConfigurer {
  private static final Logger LOG = LoggerFactory.getLogger(BitSailRuntimePluginConfigurer.class);

  private final FlinkJobMode flinkJobMode;

  public List<RuntimePlugin> getRuntimePlugins() {
    List<Class> runtimePluginClasses = flinkJobMode.getRuntimePluginClasses();
    return runtimePluginClasses.stream().map(pluginClass -> {
      try {
        return (RuntimePlugin) pluginClass.newInstance();
      } catch (Exception e) {
        LOG.error("failed to init runtime plugin: {}", pluginClass.getName());
        return null;
      }
    }).filter(Objects::nonNull).collect(Collectors.toList());
  }
}
