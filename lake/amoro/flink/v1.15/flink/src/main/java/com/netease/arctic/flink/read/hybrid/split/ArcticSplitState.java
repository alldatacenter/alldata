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

package com.netease.arctic.flink.read.hybrid.split;

import org.apache.flink.util.FlinkRuntimeException;

/**
 * This is the mutable state for per arctic source split.
 */
public class ArcticSplitState {
  private final ArcticSplit arcticSplit;

  private int currentInsertFileOffset;
  private long currentInsertRecordOffset;
  private int currentDeleteFileOffset;
  private long currentDeleteRecordOffset;

  public ArcticSplitState(ArcticSplit arcticSplit) {
    this.arcticSplit = arcticSplit;
  }

  public ArcticSplit toSourceSplit() {
    if (arcticSplit.isSnapshotSplit()) {
      SnapshotSplit snapshotSplit = (SnapshotSplit) arcticSplit;
      snapshotSplit.updateOffset(new Object[]{currentInsertFileOffset, currentInsertRecordOffset});
      return snapshotSplit;
    } else if (arcticSplit.isChangelogSplit()) {
      ChangelogSplit changelogSplit = (ChangelogSplit) arcticSplit;
      changelogSplit.updateOffset(new Object[]{
          currentInsertFileOffset,
          currentInsertRecordOffset,
          currentDeleteFileOffset,
          currentDeleteRecordOffset
      });
      return changelogSplit;
    }

    throw new FlinkRuntimeException(
        String.format("As of now this source split is unsupported %s, available split are %s, %s",
            arcticSplit.getClass().getSimpleName(),
            SnapshotSplit.class.getSimpleName(),
            ChangelogSplit.class.getSimpleName()));
  }

  public void updateOffset(Object[] offsets) {
    currentInsertFileOffset = (int) offsets[0];
    currentInsertRecordOffset = (long) offsets[1];
    if (arcticSplit.isChangelogSplit()) {
      currentDeleteFileOffset = (int) offsets[2];
      currentDeleteRecordOffset = (long) offsets[3];
    }
  }
}
