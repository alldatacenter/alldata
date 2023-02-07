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
 * Defines all the selection policy implementation which can be configured with Resource Management. The
 * configuration which is used to specify a policy is
 * {@link org.apache.drill.exec.resourcemgr.config.ResourcePoolTreeImpl#ROOT_POOL_QUEUE_SELECTION_POLICY_KEY}. Selection Policy
 * helps to select a single leaf ResourcePool out of all the eligible pools whose queue can be used to admit this query.
 * Currently there are 3 types of supported policies. In future more policies can be supported by implementing
 * {@link org.apache.drill.exec.resourcemgr.config.selectionpolicy.QueueSelectionPolicy} interface.
 * <ul>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectionpolicy.DefaultQueueSelection}: Out of all the eligible pools
 *   this policy will choose a default pool in the list. If there are multiple default pools present in the list then
 *   it will return the first default pool. If there is no default pool present then it throws
 *   {@link org.apache.drill.exec.resourcemgr.config.exception.QueueSelectionException}
 *   </li>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectionpolicy.RandomQueueSelection}: Out of all the eligible pools
 *   this policy will choose a pool at random. If there are no pools to select from then it throws
 *   {@link org.apache.drill.exec.resourcemgr.config.exception.QueueSelectionException}
 *   </li>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectionpolicy.BestFitQueueSelection}: Out of all the eligible pools
 *   this policy will choose a pool whose queue configuration
 *   {@link org.apache.drill.exec.resourcemgr.config.QueryQueueConfigImpl#MAX_QUERY_MEMORY_PER_NODE_KEY} value is closest to
 *   the max memory on a node required by the query. It tries to find a pool whose value for MAX_QUERY_MEMORY_PER_NODE
 *   is equal to queries max memory per node requirement. If there is no such pool then find the pool with config value
 *   just greater than queries max memory per node. Otherwise find a pool with config value just less than queries
 *   max memory per node.
 *   </li>
 * </ul>
 */
package org.apache.drill.exec.resourcemgr.config.selectionpolicy;