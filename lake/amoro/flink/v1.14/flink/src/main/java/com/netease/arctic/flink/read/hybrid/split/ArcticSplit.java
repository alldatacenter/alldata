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

import com.netease.arctic.data.DataTreeNode;
import org.apache.flink.api.connector.source.SourceSplit;

import java.io.Serializable;

/**
 * An abstract arctic source split.
 */
public abstract class ArcticSplit implements SourceSplit, Serializable, Comparable<ArcticSplit> {
  private static final long serialVersionUID = 1L;

  public abstract Integer taskIndex();

  public DataTreeNode dataTreeNode() {
    throw new UnsupportedOperationException("This operation is not supported right now.");
  }

  public void modifyTreeNode(DataTreeNode expectedNode) {
    throw new UnsupportedOperationException("This operation is not supported right now.");
  }

  /**
   * Checks whether this split is a snapshot split.
   */
  public final boolean isSnapshotSplit() {
    return getClass() == SnapshotSplit.class;
  }

  /**
   * Checks whether this split is a changelog split.
   */
  public final boolean isChangelogSplit() {
    return getClass() == ChangelogSplit.class;
  }

  public final boolean isMergeOnReadSplit() {
    return getClass() == MergeOnReadSplit.class;
  }

  /**
   * Casts this split into a {@link SnapshotSplit}.
   */
  public final SnapshotSplit asSnapshotSplit() {
    return (SnapshotSplit) this;
  }

  /**
   * Casts this split into a {@link ChangelogSplit}.
   */
  public final ChangelogSplit asChangelogSplit() {
    return (ChangelogSplit) this;
  }

  public final MergeOnReadSplit asMergeOnReadSplit() {
    return (MergeOnReadSplit) this;
  }

  /**
   * update split current file offset and record offset
   * if this split is {@link SnapshotSplit} recordOffsets means [insertFileOffset, insertRecordOffset]
   * if this split is {@link ChangelogSplit} recordOffsets means [insertFileOffset, insertRecordOffset,
   * deleteFileOffset, deleteRecordOffset, ]
   *
   * @param recordOffsets [insertFileOffset, insertRecordOffset]
   */
  public abstract void updateOffset(Object[] recordOffsets);

  @Override
  public int compareTo(ArcticSplit that) {
    return this.taskIndex().compareTo(that.taskIndex());
  }

  public abstract ArcticSplit copy();

}
