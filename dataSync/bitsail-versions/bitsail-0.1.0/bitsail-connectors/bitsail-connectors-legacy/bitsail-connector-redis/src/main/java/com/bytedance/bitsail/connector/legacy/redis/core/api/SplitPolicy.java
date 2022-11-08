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

package com.bytedance.bitsail.connector.legacy.redis.core.api;

import com.bytedance.bitsail.connector.legacy.redis.core.Command;

import java.util.ArrayList;
import java.util.List;

public interface SplitPolicy {

  /**
   * Split a pipeline of commands.
   *
   * @param requests A list of commands to split into groups.
   * @return A group of command lists.
   */
  List<List<Command>> split(List<Command> requests, int splitGroups);

  /**
   * Default no split policy.
   */
  default List<List<Command>> noSplit(List<Command> requests) {
    List<List<Command>> splitRequests = new ArrayList<>(1);
    splitRequests.add(requests);
    return splitRequests;
  }
}