/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.base.connector.reader.v1;

import com.bytedance.bitsail.common.typeinfo.TypeInfo;

import java.io.Serializable;
import java.util.List;

public interface SourceReader<T, SplitT extends SourceSplit> extends Serializable, AutoCloseable {

  void start();

  void pollNext(SourcePipeline<T> pipeline) throws Exception;

  void addSplits(List<SplitT> splits);

  /**
   * Check source reader has more elements or not.
   */
  boolean hasMoreElements();

  /**
   * There will no more split will send to this source reader.
   * Source reader could be exited after process all assigned split.
   */
  default void notifyNoMoreSplits() {

  }

  /**
   * Process all events which from {@link SourceSplitCoordinator}.
   */
  default void handleSourceEvent(SourceEvent sourceEvent) {
  }

  /**
   * Store the split to the external system to recover when task failed.
   */
  List<SplitT> snapshotState(long checkpointId);

  /**
   * When all tasks finished snapshot, notify checkpoint complete will be invoked.
   */
  default void notifyCheckpointComplete(long checkpointId) throws Exception {

  }

  interface Context {

    TypeInfo<?>[] getTypeInfos();

    String[] getFieldNames();

    int getIndexOfSubtask();

    void sendSplitRequest();
  }
}
