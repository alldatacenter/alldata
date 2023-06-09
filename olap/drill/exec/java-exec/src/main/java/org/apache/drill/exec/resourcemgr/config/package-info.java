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
/**
 * This package contains the configuration components of ResourceManagement feature in Drill. ResourceManagement will
 * have it's own configuration file supporting the similar hierarchy of files as supported by Drill's current
 * configuration and supports HOCON format. All the supported files for ResourceManagement is listed in
 * {@link org.apache.drill.common.config.ConfigConstants}. However whether the feature is enabled/disabled is still
 * controlled by a configuration {@link org.apache.drill.exec.ExecConstants#RM_ENABLED} available in the Drill's main
 * configuration file. The rm config files will be parsed and loaded only when the feature is enabled. The
 * configuration is a hierarchical tree {@link org.apache.drill.exec.resourcemgr.config.ResourcePoolTree} of
 * {@link org.apache.drill.exec.resourcemgr.config.ResourcePool}. At the top will be the root pool which represents
 * the entire resources (only memory in version 1) which is available to ResourceManager to use for admitting queries.
 * It is assumed that all the nodes in the Drill cluster is homogeneous and given same amount of memory resources.
 * The root pool can be further divided into child ResourcePools to divide the resources among multiple child pools.
 * Each child pool get's a resource share from it's parent resource pool. In theory there is no limit on the number
 * of ResourcePools that can be configured to divide the cluster resources.
 * <p>
 *   In addition to other parameters defined later root ResourcePool also supports a configuration
 * {@link org.apache.drill.exec.resourcemgr.config.ResourcePoolTreeImpl#ROOT_POOL_QUEUE_SELECTION_POLICY_KEY} which
 * helps to select exactly one leaf pool out of all the possible options available for a query. For details please
 * see package-info.java of {@link org.apache.drill.exec.resourcemgr.config.selectionpolicy.QueueSelectionPolicy}.
 * {@link org.apache.drill.exec.resourcemgr.config.ResourcePoolTree#selectOneQueue(org.apache.drill.exec.ops.QueryContext,
 * org.apache.drill.exec.resourcemgr.NodeResources)} method is used by parallelizer to get a queue which will be used
 * to admit a query. The selected queue resource constraints are used by parallelizer to allocate proper resources
 * to a query so that it remains within the bounds.
 * </p>
 * <p>
 *   The ResourcePools falls under 2 category:
 * <ul>
 *   <li>Intermediate Pool: As the name suggests all the pools between root and leaf pool falls under this
 *   category. It helps to navigate a query through the ResourcePoolTree hierarchy to find leaf pools using selectors.
 *   The intermediate ResourcePool help to subdivide a parent resource pool resource and doesn't have an actual queue
 *   associated with it. A query will only be executed in a queue associated with a ResourcePool not the ResourcePool
 *   itself.
 *   </li>
 *   <li>Leaf Pool: All the ResourcePools which doesn't have any child pools associated with it are leaf
 *   ResourcePools. All the leaf pools should have a unique name associated with it and should always have exactly one
 *   queue configured with it. The queue of a leaf pool is where the queries will be admitted and a resource slice will
 *   be given to it. All the leaf ResourcePools will collectively comprise of all the resource share available to
 *   Drill's ResourceManager to allocate to all the queries.
 *   </li>
 * </ul>
 * Configurations Supported by ResourcePool:
 *   <ul>
 *     <li>{@link org.apache.drill.exec.resourcemgr.config.ResourcePoolImpl#POOL_MEMORY_SHARE_KEY}: Percentage of
 *     memory share of parent ResourcePool assigned to this pool</li>
 *     <li>{@link org.apache.drill.exec.resourcemgr.config.ResourcePoolImpl#POOL_SELECTOR_KEY}: A selector assigned
 *     to this pool. For details please see package-info.java of
 *     {@link org.apache.drill.exec.resourcemgr.config.selectors.ResourcePoolSelector}
 *     </li>
 *     <li>{@link org.apache.drill.exec.resourcemgr.config.ResourcePoolImpl#POOL_QUEUE_KEY}: Queue configuration
 *     associated with this pool. It should always be configured for a leaf pool only. If configured with an
 *     intermediate pool then it will be ignored.
 *     </li>
 *   </ul>
 * </p>
 * <p>
 *   A queue always have 1:1 relationship with a leaf pool. Queries are admitted and executed with a resource slice
 *   from the queue. It supports following configurations:
 *   <ul>
 *     <li>{@link org.apache.drill.exec.resourcemgr.config.QueryQueueConfigImpl#MAX_ADMISSIBLE_KEY}: Upper bound on the
 *     total number of queries that can be admitted inside a queue. After this limit is reached all the queries
 *     will be moved to waiting state.</li>
 *     <li>{@link org.apache.drill.exec.resourcemgr.config.QueryQueueConfigImpl#MAX_WAITING_KEY}: Limits the
 *     total number of queries that can be in waiting state inside a queue. After this limit is reached all the new
 *     queries will be failed immediately.</li>
 *     <li> {@link org.apache.drill.exec.resourcemgr.config.QueryQueueConfigImpl#MAX_QUERY_MEMORY_PER_NODE_KEY}:
 *     Limits the maximum memory any query in this queue can consume on any node in the cluster. This is to limit a
 *     query from a queue to consume all the resources on a node so that other queues query can also have some
 *     resources available for it. Ideally it's advised that sum of value of this parameter for all queues should not
 *     exceed the total memory on a node.
 *     </li>
 *     <li> {@link org.apache.drill.exec.resourcemgr.config.QueryQueueConfigImpl#WAIT_FOR_PREFERRED_NODES_KEY}: This
 *     configuration helps to decide if an admitted query in a queue should wait until it has available resources on all
 *     the nodes assigned to it by planner for its execution. By default it's true. When set to false then for the nodes
 *     which doesn't have available resources for a query will be replaced with another node with enough resources.
 *     </li>
 *   </ul>
 * </p>
 * Once all the configuration are parsed an in-memory structures are created then for each query planner will select
 * a queue where a query can be admitted. The queue selection process happens by traversing the ResourcePoolTree. During
 * traversal process the query metadata is evaluated against assigned selector of a ResourcePool. If the selector
 * returns true then traversal continues to it's child pools otherwise it stops there and tries another pool. With
 * the traversal it finds all the leaf pools which are eligible for admitting the query and store that information in
 * {@link org.apache.drill.exec.resourcemgr.config.QueueAssignmentResult}. Later the selected pools are passed to
 * configured QueueSelectionPolicy to select one queue for the query. Planner uses that selected queue's max query
 * memory per node parameter to limit resource assignment to all the fragments of a query on a node. After a query is
 * planned with resource constraints it is sent to leader of that queue to ask for admission. If admitted the query
 * required resources are reserved in global state store and query is executed on the cluster. For details please see
 * the design document and functional spec linked in <a href="https://issues.apache.org/jira/browse/DRILL-7026">
 * DRILL-7026</a>
 */
package org.apache.drill.exec.resourcemgr.config;