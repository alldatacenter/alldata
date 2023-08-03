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

import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;

import java.util.HashMap;
import java.util.Map;

public abstract class BaseOptimizingInput implements TableOptimizing.OptimizingInput {

  private final Map<String, String> options = new HashMap<>();

  @Override
  public void option(String name, String value) {
    options.put(name, value);
  }

  @Override
  public void options(Map<String, String> options) {
    options.putAll(options);
  }

  @Override
  public Map<String, String> getOptions() {
    return options;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("options", options)
        .toString();
  }
}
