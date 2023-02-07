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
package org.apache.drill.exec.physical.impl.common;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * A helper class used by {@link HashTableTemplate} in order to pre-allocate {@link HashTableTemplate.BatchHolder}s of the appropriate size.
 *
 * <b>Note:</b> This class will be obsolete once the key vectors are removed from the hash table.
 */
class HashTableAllocationTracker
{
  private enum State {
    NO_ALLOCATION_IN_PROGRESS,
    ALLOCATION_IN_PROGRESS
  }

  private final HashTableConfig config;
  private State state = State.NO_ALLOCATION_IN_PROGRESS;
  private int remainingCapacity;

  protected HashTableAllocationTracker(final HashTableConfig config)
  {
    this.config = Preconditions.checkNotNull(config);
    remainingCapacity = config.getInitialCapacity();
  }

  public int getNextBatchHolderSize(int batchSize) {
    state = State.ALLOCATION_IN_PROGRESS;

    if (!config.getInitialSizeIsFinal()) {
      // We don't know the final size of the hash table, so just return the batch size.
      return batchSize;
    } else {
      // We know the final size of the hash table so we need to compute the next batch holder size.
      Preconditions.checkState(remainingCapacity > 0);
      return computeNextBatchHolderSize(batchSize);
    }
  }

  private int computeNextBatchHolderSize(int batchSize) {
    return Math.min(batchSize, remainingCapacity);
  }

  public void commit(int batchSize) {
    Preconditions.checkState(state.equals(State.ALLOCATION_IN_PROGRESS));
    remainingCapacity -= batchSize;
    state = State.NO_ALLOCATION_IN_PROGRESS;
  }
}
