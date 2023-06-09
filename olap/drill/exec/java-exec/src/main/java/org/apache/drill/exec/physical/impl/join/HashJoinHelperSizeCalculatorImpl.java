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
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.RecordBatchSizer;
import org.apache.drill.exec.vector.IntVector;

public class HashJoinHelperSizeCalculatorImpl implements HashJoinHelperSizeCalculator {
  public static final HashJoinHelperSizeCalculatorImpl INSTANCE = new HashJoinHelperSizeCalculatorImpl();

  private HashJoinHelperSizeCalculatorImpl() {
    // Do nothing
  }

  @Override
  public long calculateSize(HashJoinMemoryCalculator.PartitionStat partitionStat, double fragmentationFactor) {
    Preconditions.checkArgument(!partitionStat.isSpilled());

    // Account for the size of the SV4 in a hash join helper
    long joinHelperSize = IntVector.VALUE_WIDTH * RecordBatch.MAX_BATCH_ROW_COUNT;

    // Account for the SV4 for each batch that holds links for each batch
    for (HashJoinMemoryCalculator.BatchStat batchStat: partitionStat.getInMemoryBatches()) {
      // Note we don't have to round up to nearest power of 2, since we allocate the exact
      // space needed for the value vector.
      joinHelperSize += batchStat.getNumRecords() * IntVector.VALUE_WIDTH;
    }

    // Note the BitSets of the HashJoin helper are stored on heap, so we don't account for them here.
    // TODO move BitSets to direct memory

    return RecordBatchSizer.multiplyByFactor(joinHelperSize, fragmentationFactor);
  }
}
