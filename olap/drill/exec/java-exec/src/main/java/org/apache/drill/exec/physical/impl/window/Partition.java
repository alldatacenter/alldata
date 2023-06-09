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
package org.apache.drill.exec.physical.impl.window;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used internally to keep track of partitions and frames.<p>
 * A partition can be partial, which means we don't know "yet" the total number
 * of records that are part of this partition. Even for partial partitions, we
 * know the number of rows that are part of current frame
 */
public class Partition {
  private static final Logger logger = LoggerFactory.getLogger(Partition.class);

  private boolean partial; // true if we don't know yet the full length of this partition
  private long length; // size of this partition (if partial is true, then this is a partial length of the partition)
  private long remaining; // remaining non-processed rows in this partition

  private long peers; // remaining non-processed peers in current frame

  // we keep these attributes public because the generated code needs to access them
  public int row_number = 1;
  public int rank;
  public int dense_rank;
  public double percent_rank;
  public double cume_dist;

  /**
   * @return number of rows not yet aggregated in this partition
   */
  public long getRemaining() {
    return remaining;
  }

  public long getLength() {
    return length;
  }

  /**
   * @param length
   *          number of rows in this partition
   * @param partial
   *          if true, then length is not the full length of the partition but
   *          just the number of rows in the current batch
   */
  public void updateLength(long length, boolean partial) {
    this.length += length;
    this.partial = partial;
    remaining += length;
  }

  public void rowAggregated() {
    remaining--;
    peers--;
    row_number++;
  }

  public void newFrame(long peers) {
    this.peers = peers;

    rank = row_number; // rank = row number of 1st peer
    dense_rank++;
    percent_rank = length > 1 ? (double) (rank - 1) / (length - 1) : 0;
    cume_dist = (double)(rank + peers - 1) / length;
  }

  public boolean isDone() {
    return !partial && remaining == 0;
  }

  public int ntile(int numTiles) {
    long mod = length % numTiles;
    double ceil = Math.ceil((double) length / numTiles);

    int out;
    if (row_number <= mod * ceil) {
      out = (int) Math.ceil(row_number / ceil);
    } else {
      double floor = Math.floor((double) length / numTiles);
      out = (int) Math.ceil((row_number - mod) / floor);
    }

    logger.trace("NTILE(row_number = {}, nt = {}, ct = {}) = {}", row_number, numTiles, length, out);
    return out;
  }

  public boolean isFrameDone() {
    return peers == 0;
  }

  @Override
  public String toString() {
    return String.format("{length: %d, remaining partition: %d, remaining peers: %d}", length, remaining, peers);
  }
}
