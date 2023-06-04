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

package com.bytedance.bitsail.base.connector.writer.v1.comittable;

import com.google.common.collect.Lists;
import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

/**
 * Created 2022/6/17
 */
@Getter
public class CommittableState<CommitT> {

  private final long checkpointId;

  private final List<CommitT> committables;

  public CommittableState(long checkpointId, List<CommitT> committables) {
    this.checkpointId = checkpointId;
    this.committables = committables;
  }

  /**
   * Transform commit information of multi checkpoint into a list of commit state.
   *
   * @param committablesPerCheckpoint Commit information from multi checkpoint.
   * @param <CommitT>                 Commit information type.
   * @return A list of commit state.
   */
  public static <CommitT> List<CommittableState<CommitT>> fromNavigableMap(
      NavigableMap<Long, List<CommitT>> committablesPerCheckpoint) {

    List<CommittableState<CommitT>> committableStates = Lists.newArrayList();
    for (Map.Entry<Long, List<CommitT>> entry : committablesPerCheckpoint.entrySet()) {
      committableStates.add(new CommittableState<>(entry.getKey(), entry.getValue()));
    }

    return committableStates;
  }
}
