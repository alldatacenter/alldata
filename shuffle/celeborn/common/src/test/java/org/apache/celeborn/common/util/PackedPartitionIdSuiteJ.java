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

package org.apache.celeborn.common.util;

import org.junit.Assert;
import org.junit.Test;

public class PackedPartitionIdSuiteJ {

  @Test
  public void testNormalPackedPartitionId() {
    assertTest(0, 0);
    assertTest(555, 1);
    assertTest(888, 1);
    assertTest(10001, 100);

    // testUseMaxPartitionId or MaxAttemptId
    assertTest(PackedPartitionId.MAXIMUM_PARTITION_ID, 11);
    assertTest(100, PackedPartitionId.MAXIMUM_ATTEMPT_ID);
    assertTest(PackedPartitionId.MAXIMUM_PARTITION_ID, PackedPartitionId.MAXIMUM_ATTEMPT_ID);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testAttemptIdGreaterThanMaximumAttemptId() {
    PackedPartitionId.packedPartitionId(0, PackedPartitionId.MAXIMUM_ATTEMPT_ID + 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testPartitionIdGreaterThanMaximumPartitionId() {
    PackedPartitionId.packedPartitionId(PackedPartitionId.MAXIMUM_PARTITION_ID + 1, 1);
  }

  private void assertTest(int partitionRawId, int attemptId) {
    int packedPartitionId = PackedPartitionId.packedPartitionId(partitionRawId, attemptId);
    Assert.assertTrue(partitionRawId == PackedPartitionId.getRawPartitionId(packedPartitionId));
    Assert.assertTrue(attemptId == PackedPartitionId.getAttemptId(packedPartitionId));
  }
}
