/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import com.google.common.collect.Sets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Temporary implementation
 */
public class RepairProperty {

  public static final String MAX_FIND_SNAPSHOT_NUM = "max_find_snapshot_number";

  public static final String MAX_ROLLBACK_SNAPSHOT_NUM = "max_rollback_snapshot_number";

  private static final Set<String> coordinator = Sets.newHashSet(MAX_ROLLBACK_SNAPSHOT_NUM, MAX_ROLLBACK_SNAPSHOT_NUM);

  private Map<String, String> property = new HashMap<>();

  {
    property.put(MAX_FIND_SNAPSHOT_NUM, "100");
    property.put(MAX_ROLLBACK_SNAPSHOT_NUM, "10");
  }

  public void set(String name, String value) {
    if (!coordinator.contains(name)) {
      throw new IllegalArgumentException("Unsupported property");
    }
    property.put(name, value);
  }

  public Object get(String name) {
    return property.get(name);
  }

  public Integer getInt(String name) {
    String value = property.get(name);
    return value == null ? null : Integer.parseInt(value);
  }
}
