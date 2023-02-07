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
package org.apache.drill.exec.work.foreman.rm;

import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.work.QueryWorkUnit;

/**
 * Manages resources for an individual query in conjunction with the
 * global {@link ResourceManager}. Handles memory and CPU allocation.
 * Instances of this class handle query planning and are used when the
 * client wants to plan the query, but not execute it. An implementation
 * of {@link QueryResourceManager} is used to both plan the query and
 * queue it for execution.
 * <p>
 * This interface allows a variety of resource management strategies to
 * exist for different purposes.
 * <p>
 * The methods here assume external synchronization: a single query calls
 * the methods at known times; there are no concurrent calls.
 */

public interface QueryResourceAllocator {

  /**
   * Make any needed adjustments to the query plan before parallelization.
   *
   * @param plan physical plan
   */
  void visitAbstractPlan(PhysicalPlan plan);

  /**
   * Provide the manager with the physical plan and node assignments
   * for the query to be run. This class will plan memory for the query.
   *
   * @param work query work unit
   */

  void visitPhysicalPlan(QueryWorkUnit work);
}
