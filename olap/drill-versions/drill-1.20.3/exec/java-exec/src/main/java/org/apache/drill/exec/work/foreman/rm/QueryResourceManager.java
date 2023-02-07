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

import org.apache.drill.exec.planner.fragment.QueryParallelizer;
import org.apache.drill.exec.work.foreman.rm.QueryQueue.QueryQueueException;
import org.apache.drill.exec.work.foreman.rm.QueryQueue.QueueTimeoutException;

/**
 * Extends a {@link QueryResourceAllocator} to provide queueing support.
 */

public interface QueryResourceManager extends QueryResourceAllocator {

  /**
   * Hint that this resource manager queues. Allows the Foreman
   * to short-circuit expensive logic if no queuing will actually
   * be done. This is a static attribute per Drillbit run.
   */

  boolean hasQueue();

  /**
   * For some cases the foreman does not have a full plan, just a cost. In
   * this case, this object will not plan memory, but still needs the cost
   * to place the job into the correct queue.
   * @param cost
   */

  void setCost(double cost);

  /**
   * Create a parallelizer to parallelize each major fragment of the query into
   * many minor fragments. The parallelizer encapsulates the logic of how much
   * memory and parallelism is required for the query.
   * @param memoryPlanning memory planning needs to be done during parallelization
   * @return
   */
  QueryParallelizer getParallelizer(boolean memoryPlanning);

  /**
   * Admit the query into the cluster. Blocks until the query
   * can run. (Later revisions may use a more thread-friendly
   * approach.)
   * @throws QueryQueueException if something goes wrong with the
   * queue mechanism
   * @throws QueueTimeoutException if the query timed out waiting to
   * be admitted.
   */

  void admit() throws QueueTimeoutException, QueryQueueException;

  /**
   * Returns the name of the queue (if any) on which the query was
   * placed. Valid only after the query is admitted.
   *
   * @return queue name, or null if queuing is not enabled.
   */

  String queueName();

  /**
   * Mark the query as completing, giving up its slot in the
   * cluster. Releases any lease that may be held for a system with queues.
   */

  void exit();
}
