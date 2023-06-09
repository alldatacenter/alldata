/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class represents the memory size statistics for an entire partition.
 */
public class PartitionStatImpl implements HashJoinMemoryCalculator.PartitionStat {
  private boolean spilled;
  private long numRecords;
  private long partitionSize;
  private LinkedList<HashJoinMemoryCalculator.BatchStat> batchStats = Lists.newLinkedList();

  public PartitionStatImpl() {
  }

  public void add(HashJoinMemoryCalculator.BatchStat batchStat) {
    Preconditions.checkState(!spilled);
    Preconditions.checkNotNull(batchStat);
    partitionSize += batchStat.getBatchSize();
    numRecords += batchStat.getNumRecords();
    batchStats.addLast(batchStat);
  }

  public void spill() {
    Preconditions.checkState(!spilled);
    spilled = true;
    partitionSize = 0;
    numRecords = 0;
    batchStats.clear();
  }

  public List<HashJoinMemoryCalculator.BatchStat> getInMemoryBatches()
  {
    return Collections.unmodifiableList(batchStats);
  }

  public int getNumInMemoryBatches()
  {
    return batchStats.size();
  }

  public boolean isSpilled()
  {
    return spilled;
  }

  public long getNumInMemoryRecords()
  {
    return numRecords;
  }

  public long getInMemorySize()
  {
    return partitionSize;
  }
}
