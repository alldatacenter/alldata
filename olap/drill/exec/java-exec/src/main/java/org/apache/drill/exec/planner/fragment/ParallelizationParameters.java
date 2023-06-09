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
package org.apache.drill.exec.planner.fragment;

/**
 * Interface to implement for passing parameters to {@link FragmentParallelizer}.
 */
public interface ParallelizationParameters {

  /**
   * @return Configured max width per slice of work.
   */
  long getSliceTarget();

  /**
   * @return Configured maximum allowed number of parallelization units per node.
   */
  int getMaxWidthPerNode();

  /**
   * @return Configured maximum allowed number of parallelization units per all nodes in the cluster.
   */
  int getMaxGlobalWidth();

  /**
   * @return Factor by which a node with endpoint affinity will be favored while creating assignment.
   */
  double getAffinityFactor();
}
