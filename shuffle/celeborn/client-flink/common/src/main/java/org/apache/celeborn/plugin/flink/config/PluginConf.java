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

package org.apache.celeborn.plugin.flink.config;

import org.apache.flink.configuration.Configuration;

import org.apache.celeborn.common.CelebornConf;

public enum PluginConf {
  MIN_MEMORY_PER_PARTITION("remote-shuffle.job.min.memory-per-partition", "", "8m"),
  MIN_MEMORY_PER_GATE("remote-shuffle.job.min.memory-per-gate", "", "8m"),
  NUM_CONCURRENT_READINGS(
      "remote-shuffle.job.concurrent-readings-per-gate", "", String.valueOf(Integer.MAX_VALUE)),
  MEMORY_PER_RESULT_PARTITION("remote-shuffle.job.memory-per-partition", "", "64m"),
  MEMORY_PER_INPUT_GATE("remote-shuffle.job.memory-per-gate", "", "32m"),
  ENABLE_DATA_COMPRESSION("remote-shuffle.job.enable-data-compression", "", "true"),
  REMOTE_SHUFFLE_COMPRESSION_CODEC(
      "remote-shuffle.job.compression.codec",
      CelebornConf.SHUFFLE_COMPRESSION_CODEC().key(),
      "LZ4"),
  ;

  public String name;
  public String alterName;
  public String defaultValue;

  PluginConf(String name, String alterName, String defaultValue) {
    this.name = name;
    this.alterName = alterName;
    this.defaultValue = defaultValue;
  }

  public static String getValue(Configuration flinkConf, PluginConf conf) {
    return flinkConf.getString(conf.name, conf.defaultValue);
  }
}
