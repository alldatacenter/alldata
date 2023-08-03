/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.table;

import org.apache.iceberg.util.StructLikeMap;

public class KeyedTableSnapshot implements TableSnapshot {
  private final long baseSnapshotId;
  private final long changeSnapshotId;
  private final StructLikeMap<Long> partitionOptimizedSequence;
  private final StructLikeMap<Long> legacyPartitionMaxTransactionId;

  public KeyedTableSnapshot(long baseSnapshotId, long changeSnapshotId,
                            StructLikeMap<Long> partitionOptimizedSequence,
                            StructLikeMap<Long> legacyPartitionMaxTransactionId) {
    this.baseSnapshotId = baseSnapshotId;
    this.changeSnapshotId = changeSnapshotId;
    this.partitionOptimizedSequence = partitionOptimizedSequence;
    this.legacyPartitionMaxTransactionId = legacyPartitionMaxTransactionId;
  }

  public long baseSnapshotId() {
    return baseSnapshotId;
  }

  public long changeSnapshotId() {
    return changeSnapshotId;
  }

  public StructLikeMap<Long> partitionOptimizedSequence() {
    return partitionOptimizedSequence;
  }

  public StructLikeMap<Long> legacyPartitionMaxTransactionId() {
    return legacyPartitionMaxTransactionId;
  }

  @Override
  public long snapshotId() {
    return baseSnapshotId;
  }
}
