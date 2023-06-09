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

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

public class TestHashJoinHelperSizeCalculatorImpl extends BaseTest {
  @Test
  public void simpleCalculateSize() {
    final long intSize =
      ((long) TypeHelper.getSize(TypeProtos.MajorType.newBuilder().setMinorType(TypeProtos.MinorType.INT).build()));

    // Account for the overhead of a selection vector
    long expected = intSize * RecordBatch.MAX_BATCH_ROW_COUNT;
    // Account for sv4 vector for batches
    expected += intSize * 3500;

    PartitionStatImpl partitionStat = new PartitionStatImpl();
    partitionStat.add(new HashJoinMemoryCalculator.BatchStat(1000, 2000));
    partitionStat.add(new HashJoinMemoryCalculator.BatchStat(2500, 5000));

    long actual = HashJoinHelperSizeCalculatorImpl.INSTANCE.calculateSize(partitionStat, 1.0);
    Assert.assertEquals(expected, actual);

    long shouldBeZero = HashJoinHelperUnusedSizeImpl.INSTANCE.calculateSize(partitionStat, 1.0);
    Assert.assertEquals(0, shouldBeZero);
  }
}
