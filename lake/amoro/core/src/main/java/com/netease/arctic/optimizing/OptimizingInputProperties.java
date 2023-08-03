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

package com.netease.arctic.optimizing;

import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.Map;

public class OptimizingInputProperties {

  public static final String ENABLE_SPILL_MAP = "enable_spill_map";

  public static final String MAX_IN_MEMORY_SIZE_IN_BYTES = "max_size_in_memory";

  public static final String SPILL_MAP_PATH = "sill_map_path";

  public static final String OUTPUT_DIR = "output_location";

  public static final String MOVE_FILE_TO_HIVE_LOCATION = "move-files-to-hive-location";

  public static final String TASK_EXECUTOR_FACTORY_IMPL = "task-executor-factory-impl";

  private Map<String, String> properties;

  private OptimizingInputProperties(Map<String, String> properties) {
    this.properties = Maps.newHashMap(properties);
  }

  public OptimizingInputProperties() {
    properties = new HashMap<>();
  }

  public static OptimizingInputProperties parse(Map<String, String> properties) {
    return new OptimizingInputProperties(properties);
  }

  public OptimizingInputProperties enableSpillMap() {
    properties.put(ENABLE_SPILL_MAP, "true");
    return this;
  }

  public OptimizingInputProperties setMaxSizeInMemory(long maxSizeInMemory) {
    properties.put(MAX_IN_MEMORY_SIZE_IN_BYTES, String.valueOf(maxSizeInMemory));
    return this;
  }

  public OptimizingInputProperties setSpillMapPath(String path) {
    properties.put(SPILL_MAP_PATH, path);
    return this;
  }

  public OptimizingInputProperties setOutputDir(String outputDir) {
    properties.put(OUTPUT_DIR, outputDir);
    return this;
  }

  public OptimizingInputProperties setExecutorFactoryImpl(String executorFactoryImpl) {
    properties.put(TASK_EXECUTOR_FACTORY_IMPL, executorFactoryImpl);
    return this;
  }

  public OptimizingInputProperties needMoveFile2HiveLocation() {
    properties.put(MOVE_FILE_TO_HIVE_LOCATION, "true");
    return this;
  }

  public StructLikeCollections getStructLikeCollections() {
    String enableSpillMapStr = properties.get(ENABLE_SPILL_MAP);
    boolean enableSpillMap = enableSpillMapStr == null ? false : Boolean.parseBoolean(enableSpillMapStr);

    String maxInMemoryStr = properties.get(MAX_IN_MEMORY_SIZE_IN_BYTES);
    Long maxInMemory = maxInMemoryStr == null ? null : Long.parseLong(maxInMemoryStr);

    String spillMapPath = properties.get(SPILL_MAP_PATH);

    return new StructLikeCollections(enableSpillMap, maxInMemory, spillMapPath);
  }

  public String getOutputDir() {
    return properties.get(OUTPUT_DIR);
  }

  public String getExecutorFactoryImpl() {
    return properties.get(TASK_EXECUTOR_FACTORY_IMPL);
  }

  public boolean getMoveFile2HiveLocation() {
    String s = properties.get(MOVE_FILE_TO_HIVE_LOCATION);
    if (StringUtils.isBlank(s)) {
      return false;
    }
    return Boolean.parseBoolean(s);
  }

  public Map<String, String> getProperties() {
    return properties;
  }
}
