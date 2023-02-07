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

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.work.foreman.Foreman;

/**
 * Drillbit-wide resource manager shared by all queries. Manages memory (at
 * present) and CPU (planned). Since queries are the primary consumer of
 * resources, manages resources by throttling queries into the system, and
 * allocating resources to queries in order to control total use. An "null"
 * implementation handles the case of no queuing. Clearly, the null case cannot
 * effectively control resource use.
 */

public interface ResourceManager {

  /**
   * Returns the memory, in bytes, assigned to each node in a Drill cluster.
   * Drill requires that nodes are symmetrical. So, knowing the memory on any
   * one node also gives the memory on all other nodes.
   *
   * @return the memory, in bytes, available in each Drillbit
   */
  long memoryPerNode();

  int cpusPerNode();

  /**
   * Create a resource manager to prepare or describe a query. In this form, no
   * queuing is done, but the plan is created as if queuing had been done. Used
   * when executing EXPLAIN PLAN.
   *
   * @return a resource manager for the query
   */

  QueryResourceAllocator newResourceAllocator(QueryContext queryContext);

  /**
   * Create a resource manager to execute a query.
   *
   * @param foreman
   *          Foreman which manages the execution
   * @return a resource manager for the query
   */

  QueryResourceManager newQueryRM(final Foreman foreman);

  void close();
}
