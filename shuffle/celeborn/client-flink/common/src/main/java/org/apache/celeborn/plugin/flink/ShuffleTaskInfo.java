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

package org.apache.celeborn.plugin.flink;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.celeborn.common.util.JavaUtils;

public class ShuffleTaskInfo {
  private static final Logger LOG = LoggerFactory.getLogger(ShuffleTaskInfo.class);

  private int currentShuffleIndex = 0;
  // map attemptId index
  private ConcurrentHashMap<Integer, ConcurrentHashMap<Integer, AtomicInteger>>
      shuffleIdMapAttemptIdIndex = JavaUtils.newConcurrentHashMap();
  // task shuffle id -> celeborn shuffle id
  private ConcurrentHashMap<String, Integer> taskShuffleIdToShuffleId =
      JavaUtils.newConcurrentHashMap();
  // celeborn shuffle id -> task shuffle id
  private ConcurrentHashMap<Integer, String> shuffleIdToTaskShuffleId =
      JavaUtils.newConcurrentHashMap();

  private ConcurrentHashMap<Integer, AtomicInteger> shuffleIdPartitionIdIndex =
      JavaUtils.newConcurrentHashMap();

  public int getShuffleId(String taskShuffleId) {
    synchronized (taskShuffleIdToShuffleId) {
      if (taskShuffleIdToShuffleId.containsKey(taskShuffleId)) {
        return taskShuffleIdToShuffleId.get(taskShuffleId);
      } else {
        taskShuffleIdToShuffleId.put(taskShuffleId, currentShuffleIndex);
        shuffleIdToTaskShuffleId.put(currentShuffleIndex, taskShuffleId);
        shuffleIdMapAttemptIdIndex.put(currentShuffleIndex, JavaUtils.newConcurrentHashMap());
        shuffleIdPartitionIdIndex.put(currentShuffleIndex, new AtomicInteger(0));
        int tempShuffleIndex = currentShuffleIndex;
        currentShuffleIndex = currentShuffleIndex + 1;
        return tempShuffleIndex;
      }
    }
  }

  public int genAttemptId(int shuffleId, int mapId) {
    AtomicInteger currentAttemptIndex =
        shuffleIdMapAttemptIdIndex
            .get(shuffleId)
            .computeIfAbsent(mapId, (id) -> new AtomicInteger(0));
    return currentAttemptIndex.getAndIncrement();
  }

  public int genPartitionId(int shuffleId) {
    return shuffleIdPartitionIdIndex.get(shuffleId).getAndIncrement();
  }

  public void removeExpiredShuffle(int shuffleId) {
    if (shuffleIdToTaskShuffleId.containsKey(shuffleId)) {
      shuffleIdPartitionIdIndex.remove(shuffleId);
      shuffleIdMapAttemptIdIndex.remove(shuffleId);
      String taskShuffleId = shuffleIdToTaskShuffleId.remove(shuffleId);
      taskShuffleIdToShuffleId.remove(taskShuffleId);
    }
  }

  public ShuffleResourceDescriptor genShuffleResourceDescriptor(
      String taskShuffleId, int mapId, String taskAttemptId) {
    int shuffleId = this.getShuffleId(taskShuffleId);
    int attemptId = this.genAttemptId(shuffleId, mapId);
    int partitionId = this.genPartitionId(shuffleId);
    LOG.info(
        "Assign for ({}, {}, {}) resource ({}, {}, {}, {})",
        taskShuffleId,
        mapId,
        taskAttemptId,
        shuffleId,
        mapId,
        attemptId,
        partitionId);
    return new ShuffleResourceDescriptor(shuffleId, mapId, attemptId, partitionId);
  }
}
