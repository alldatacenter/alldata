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
package org.apache.drill.exec.resourcemgr.config;

/**
 * Interface which defines an implementation for managing queue configuration of a leaf {@link ResourcePool}
 */
public interface QueryQueueConfig {
  String getQueueId();

  String getQueueName();

  /**
   * Given number of available nodes in the cluster what is the total memory resource share of this queue cluster wide
   * @param numClusterNodes number of available cluster nodes
   * @return Queue's total cluster memory resource share
   */
  long getQueueTotalMemoryInMB(int numClusterNodes);

  /**
   * @return Maximum query memory (in MB) that a query in this queue can consume on a node
   */
  long getMaxQueryMemoryInMBPerNode();

  /**
   * Given number of available nodes in the cluster what is the max memory in MB a query in this queue can be assigned
   * cluster wide
   * @param numClusterNodes number of available cluster nodes
   * @return Maximum query memory (in MB) in this queue
   */
  long getMaxQueryTotalMemoryInMB(int numClusterNodes);

  /**
   * Determines if admitted queries in this queue should wait in the queue if resources on the preferred assigned
   * nodes of a query determined by planner is unavailable.
   * @return <tt>true</tt> indicates an admitted query to wait until resources on all the preferred nodes are available or
   * wait until timeout is reached, <tt>false</tt> indicates an admitted query to not wait and find other nodes with
   * available resources if resources on a preferred node is unavailable.
   */
  boolean waitForPreferredNodes();

  /**
   * @return Maximum number of queries that can be admitted in the queue
   */
  int getMaxAdmissibleQueries();

  /**
   * @return Maximum number of queries that will be allowed to wait in the queue before failing a query right away
   */
  int getMaxWaitingQueries();

  /**
   * @return Maximum time in milliseconds for which a query can be in waiting state inside a queue
   */
  int getWaitTimeoutInMs();
}
