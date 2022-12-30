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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.filesystem.partitionstrategy.partitionfirst;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class PartitionCache {
  private final Set<String> partitionSet;
  private final Queue<String> partitionQueue;
  private final int capacity;

  public PartitionCache(int capacity) {
    this.capacity = capacity;
    this.partitionSet = new HashSet<>(capacity);
    this.partitionQueue = new ArrayDeque<>(capacity);
  }

  public boolean contains(String partition) {
    return partitionSet.contains(partition);
  }

  public boolean add(String partition) {
    if (!partitionSet.contains(partition)) {
      if (partitionQueue.size() >= capacity) {
        String head = partitionQueue.poll();
        partitionSet.remove(head);
      }
      partitionSet.add(partition);
      partitionQueue.add(partition);
      return true;
    }
    return false;
  }

  public void addAll(Set<String> partitions) {
    for (String partition : partitions) {
      add(partition);
    }
  }

  public void clear() {
    this.partitionSet.clear();
    this.partitionQueue.clear();
  }

}
