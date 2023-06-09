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

public enum HashJoinState {
  INITIALIZING,
  /**
   * In this state, the build side of the join operation is partitioned. Each partition is
   * kept in memory. If we are able to fit all the partitions in memory and we have completely
   * consumed the build side then we move to the {@link HashJoinState#POST_BUILD_CALCULATIONS}. If we
   * run out of memory and we still have not consumed all of the build side, we start evicting
   * partitions from memory to free memory. Then resume processing the build side. We repeat
   * this process until the entire build side is consumed. After the build side is consumed we
   * proceed to the {@link HashJoinState#POST_BUILD_CALCULATIONS} state.
   */
  BUILD_SIDE_PARTITIONING,
  /**
   * In this state, the probe side is consumed. If data in the probe side matches a build side partition
   * kept in memory, it is joined and sent out. If data in the probe side does not match a build side
   * partition, then it is spilled to disk. After all the probe side data is consumed processing moves
   * on to the {@link HashJoinState#POST_BUILD_CALCULATIONS} state if build side partitions are small enough
   * to fit into memory. If build side partitions can't fit into memory processing moves to the
   * {@link HashJoinState#POST_BUILD_CALCULATIONS}
   * state.
   */
  POST_BUILD_CALCULATIONS
}
