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

package com.bytedance.bitsail.entry.flink.configuration;

import com.bytedance.bitsail.common.option.ConfigOption;

import com.alibaba.fastjson.TypeReference;

import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;

/**
 * Created 2022/8/5
 */
public interface FlinkRunnerConfigOptions {

  String FLINK_RUNNER = "sys.flink.";

  ConfigOption<String> FLINK_HOME =
      key(FLINK_RUNNER + "flink_home")
          .noDefaultValue(String.class);

  ConfigOption<String> FLINK_CHECKPOINT_DIR =
      key(FLINK_RUNNER + "checkpoint_dir")
          .noDefaultValue(String.class);

  ConfigOption<Map<String, String>> FLINK_DEFAULT_PROPERTIES =
      key(FLINK_RUNNER + "flink_default_properties")
          .onlyReference(new TypeReference<Map<String, String>>() {
          });
}
