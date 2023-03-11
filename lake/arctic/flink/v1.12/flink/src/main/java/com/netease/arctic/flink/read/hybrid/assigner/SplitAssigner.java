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

package com.netease.arctic.flink.read.hybrid.assigner;

import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplitState;

import java.io.Closeable;
import java.util.Collection;
import java.util.Optional;

/**
 * An interface SplitAssigner for {@link ArcticSplit}
 */
public interface SplitAssigner extends Closeable {

  default void open() {
  }

  Optional<ArcticSplit> getNext();

  Optional<ArcticSplit> getNext(int subtaskId);

  /**
   * Add new splits discovered by enumerator
   */
  void onDiscoveredSplits(Collection<ArcticSplit> splits);

  /**
   * Forward addSplitsBack event (for failed reader) to assigner
   */
  void onUnassignedSplits(Collection<ArcticSplit> splits);

  /**
   * Some assigner (like event time alignment) may rack in-progress splits
   * to advance watermark upon completed splits
   */
  default void onCompletedSplits(Collection<String> completedSplitIds) {
  }

  Collection<ArcticSplitState> state();
}
