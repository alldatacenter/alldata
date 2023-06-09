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

import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ops.OperatorStats;
import org.apache.drill.exec.physical.impl.join.HashJoinBatch;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.LinkedList;
import java.util.Queue;

/**
 * Manages the spilling information for an operator.
 * @param <T> The class holding spilled partition metadata.
 *
 * <h4>Lifecycle</h4>
 * <ol>
 *   <li>Create a {@link SpilledState} instance.</li>
 *   <li>Call {@link SpilledState#initialize(int)}</li>
 *   <li>Call {@link SpilledState#addPartition(SpilledPartitionMetadata)} (SpilledPartitionMetadata)}, {@link SpilledState#getNextSpilledPartition()}, or
 *           {@link SpilledState#updateCycle(OperatorStats, SpilledPartitionMetadata, Updater)}</li>
 * </ol>
 *
 * <p>
 *  <ul>
 *   <li>A user can call {@link SpilledState#getCycle()} at any time.</li>
 *  </ul>
 * </p>
 */
public class SpilledState<T extends SpilledPartitionMetadata> {
  public static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SpilledState.class);

  private int numPartitions;
  private int partitionMask;
  private int bitsInMask;

  private int cycle = 0;
  private Queue<T> queue = new LinkedList<>();
  private boolean initialized = false;

  public SpilledState() {
  }

  /**
   * Initializes the number of partitions to use for the spilled state.
   * @param numPartitions The number of partitions to use for the spilled state.
   */
  public void initialize(int numPartitions) {
    Preconditions.checkState(!initialized);
    Preconditions.checkArgument(numPartitions >= 1); // Numpartitions must be positive
    Preconditions.checkArgument((numPartitions & (numPartitions - 1)) == 0); // Make sure it's a power of two

    this.numPartitions = numPartitions;
    initialized = true;
    partitionMask = numPartitions - 1;
    bitsInMask = Integer.bitCount(partitionMask);
  }

  /**
   * Gets the number of partitions.
   * @return The number of partitions.
   */
  public int getNumPartitions() {
    return numPartitions;
  }

  /**
   * True if this is the first cycle (0).
   * @return True if this is the first cycle (0).
   */
  public boolean isFirstCycle() {
    return cycle == 0;
  }

  public int getPartitionMask() {
    return partitionMask;
  }

  public int getBitsInMask() {
    return bitsInMask;
  }

  /**
   * Add the partition metadata to the end of the queue to be processed.
   * @param spilledPartition The partition metadata to process.
   * @return
   */
  public boolean addPartition(T spilledPartition) {
    Preconditions.checkState(initialized);
    return queue.offer(spilledPartition);
  }

  /**
   * Get the next spilled partition to process.
   * @return The metadata for the next spilled partition to process.
   */
  public T getNextSpilledPartition() {
    Preconditions.checkState(initialized);
    return queue.poll();
  }

  /**
   * True if there are no spilled partitions.
   * @return True if there are no spilled partitions.
   */
  public boolean isEmpty() {
    return queue.isEmpty();
  }

  /**
   * Update the current spill cycle.
   * @param operatorStats Current operator stats.
   * @param spilledPartition The next spilled partition metadata to process.
   * @param updater The updater implementation that executes custom logic if a spill cycle is incremented.
   */
  public void updateCycle(final OperatorStats operatorStats,
                          final T spilledPartition,
                          final Updater updater) {
    Preconditions.checkState(initialized);
    Preconditions.checkNotNull(operatorStats);
    Preconditions.checkNotNull(spilledPartition);
    Preconditions.checkNotNull(updater);

    if (logger.isDebugEnabled()) {
      logger.debug(spilledPartition.makeDebugString());
    }

    if (cycle == spilledPartition.getCycle()) {
      // Update the cycle num if needed.
      // The current cycle num should always be one larger than in the spilled partition.

      cycle = 1 + spilledPartition.getCycle();
      operatorStats.setLongStat(HashJoinBatch.Metric.SPILL_CYCLE, cycle); // update stats

      if (logger.isDebugEnabled()) {
        // report first spill or memory stressful situations
        if (cycle == 1) {
          logger.debug("Started reading spilled records ");
        } else if (cycle == 2) {
          logger.debug("SECONDARY SPILLING ");
        } else if (cycle == 3) {
          logger.debug("TERTIARY SPILLING ");
        } else if (cycle == 4) {
          logger.debug("QUATERNARY SPILLING ");
        } else if (cycle == 5) {
          logger.debug("QUINARY SPILLING ");
        }
      }

      if (updater.hasPartitionLimit() && cycle * bitsInMask > 20) {
        queue.offer(spilledPartition); // so cleanup() would delete the curr spill files
        updater.cleanup();
        throw UserException
          .unsupportedError()
          .message("%s.\n On cycle num %d mem available %d num partitions %d.", updater.getFailureMessage(), cycle, updater.getMemLimit(), numPartitions)
          .build(logger);
      }
    }

    if (logger.isDebugEnabled()) {
      logger.debug(spilledPartition.makeDebugString());
    }
  }

  /**
   * Gets the current spill cycle.
   * @return The current spill cycle.
   */
  public int getCycle() {
    return cycle;
  }

  /**
   * This is a class that is used to do updates of the spilled state.
   */
  public interface Updater {
    /**
     * Does any necessary cleanup if we've spilled too much and abort the query.
     */
    void cleanup();

    /**
     * Gets the failure message in the event that we spilled to far.
     * @return The failure message in the event that we spilled to far.
     */
    String getFailureMessage();

    /**
     * Gets the current memory limit.
     * @return The current memory limit.
     */
    long getMemLimit();

    /**
     * True if there is a limit to the number of times we can partition.
     * @return True if there is a limit to the number of times we can partition.
     */
    boolean hasPartitionLimit();
  }
}
