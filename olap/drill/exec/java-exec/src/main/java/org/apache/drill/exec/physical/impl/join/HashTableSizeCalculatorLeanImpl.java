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
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.vector.IntVector;

import java.util.Map;

import static org.apache.drill.exec.physical.impl.join.BatchSizePredictorImpl.computeValueVectorSize;

public class HashTableSizeCalculatorLeanImpl implements HashTableSizeCalculator {
  public static final String TYPE = "LEAN";
  private final int maxNumRecords;
  private final double hashTableDoublingFactor;

  public HashTableSizeCalculatorLeanImpl(int maxNumRecords, double hashTableDoublingFactor) {
    this.maxNumRecords = maxNumRecords;
    this.hashTableDoublingFactor = hashTableDoublingFactor;
  }

  @Override
  public long calculateSize(final HashJoinMemoryCalculator.PartitionStat partitionStat,
                            final Map<String, Long> keySizes,
                            final double loadFactor,
                            final double safetyFactor,
                            final double fragmentationFactor) {
    Preconditions.checkArgument(!keySizes.isEmpty());
    Preconditions.checkArgument(!partitionStat.isSpilled());
    Preconditions.checkArgument(partitionStat.getNumInMemoryRecords() > 0);

    // The number of entries in the global index array. Note this array may not be completed populated and the degree of population depends on the load factor.
    long numBuckets = (long) (((double) partitionStat.getNumInMemoryRecords()) * (1.0 / loadFactor));
    // The number of pairs in the hash table
    long numEntries = partitionStat.getNumInMemoryRecords();
    // The number of Batch Holders in the hash table. Note that entries are tightly packed in the Batch Holders (no gaps).
    long numBatchHolders = (numEntries + maxNumRecords - 1) / maxNumRecords;

    // Include the size of the buckets array
    long hashTableSize = RecordBatchSizer.multiplyByFactors(computeValueVectorSize(numBuckets, IntVector.VALUE_WIDTH), hashTableDoublingFactor);
    // Each Batch Holder has an int vector of max size for holding links and hash values
    hashTableSize += numBatchHolders * 2L * IntVector.VALUE_WIDTH * ((long) maxNumRecords);

    long numFullBatchHolders = numEntries % maxNumRecords == 0? numBatchHolders: numBatchHolders - 1;
    // Compute the size of the value vectors holding keys in each full bucket
    hashTableSize += numFullBatchHolders * computeVectorSizes(keySizes, maxNumRecords, safetyFactor);

    if (numFullBatchHolders != numBatchHolders) {
      // The last bucket is a partial bucket
      long partialNumEntries = numEntries % maxNumRecords;
      hashTableSize += computeVectorSizes(keySizes, partialNumEntries, safetyFactor);
      long maxVectorSize = computeMaxVectorSize(keySizes, partialNumEntries, safetyFactor);
      hashTableSize -= maxVectorSize;
      hashTableSize += RecordBatchSizer.multiplyByFactors(maxVectorSize, hashTableDoublingFactor);
    }

    return RecordBatchSizer.multiplyByFactors(hashTableSize, fragmentationFactor);
  }

  @Override
  public double getDoublingFactor() {
    return hashTableDoublingFactor;
  }

  @Override
  public String getType() {
    return TYPE;
  }

  public static long computeVectorSizes(final Map<String, Long> vectorSizes,
                                        final long numRecords,
                                        final double safetyFactor) {
    long totalKeySize = 0L;

    for (Map.Entry<String, Long> entry: vectorSizes.entrySet()) {
      totalKeySize += computeValueVectorSize(numRecords, entry.getValue(), safetyFactor);
    }

    return totalKeySize;
  }

  public static long computeMaxVectorSize(final Map<String, Long> vectorSizes,
                                        final long numRecords,
                                        final double safetyFactor) {
    long maxVectorSize = 0L;

    for (Map.Entry<String, Long> entry: vectorSizes.entrySet()) {
      maxVectorSize = Math.max(maxVectorSize, computeValueVectorSize(numRecords, entry.getValue(), safetyFactor));
    }

    return maxVectorSize;
  }
}
