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

import com.google.common.base.Preconditions;

/**
 * Pack for encode/decode id of partition Location for id of partitionLocation attemptId
 * raw_partitionId <br>
 * (upper 8 bits = attemptId) (lower 24 bits = raw id) <br>
 * (0000 0000) (0000 0000 0000 0000 0000 0000)<br>
 *
 * @see org.apache.celeborn.common.protocol.PartitionLocation#id
 */
public class PackedPartitionId {

  /** The maximum partition identifier that can be encoded. Note that partition ids start from 0. */
  static final int MAXIMUM_PARTITION_ID = (1 << 24) - 1; // 16777215

  /** The maximum partition attempt id that can be encoded. Note that attempt ids start from 0. */
  static final int MAXIMUM_ATTEMPT_ID = (1 << 8) - 1; // 255

  static final int MASK_INT_LOWER_24_BITS = (int) (1L << 24) - 1;

  public static int packedPartitionId(int partitionRawId, int attemptId) {
    Preconditions.checkArgument(
        partitionRawId <= MAXIMUM_PARTITION_ID,
        "packedPartitionId called with invalid partitionRawId: " + partitionRawId);
    Preconditions.checkArgument(
        attemptId <= MAXIMUM_ATTEMPT_ID,
        "packedPartitionId called with invalid attemptId: " + attemptId);

    return (attemptId << 24) | partitionRawId;
  }

  public static int getRawPartitionId(int packedPartitionId) {
    return packedPartitionId & MASK_INT_LOWER_24_BITS;
  }

  public static int getAttemptId(int packedPartitionId) {
    return packedPartitionId >>> 24;
  }
}
