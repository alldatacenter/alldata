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
 * Defines all the Selectors which can be assigned to a ResourcePool in the ResourceManagement configuration. A
 * selector helps to evaluate that given a query and it's metadata if that query can be admitted inside associated
 * ResourcePool or not. Selectors are associated with both intermediate and leaf level ResourcePools. The intermediate
 * pool selectors helps to navigate the ResourcePool hierarchy to reach a leaf level ResourcePool where query will
 * actually be admitted in the queue associated with a leaf pool. Whereas leaf pool selector will help to choose all
 * the leaf pools which can be considered to admit a query. A selector can be configured for a ResourcePool
 * using the {@link org.apache.drill.exec.resourcemgr.config.ResourcePoolImpl#POOL_SELECTOR_KEY} configuration. If the
 * selector configuration is missing for a ResourcePool then it is associated with a Default Selector making it
 * a Default ResourcePool. Selectors are configured as a key value pair where key represents it's type and
 * value is what it uses to evaluate a query. Currently there are 6 different types of supported selectors. In future
 * more selectos can be supported by implementing
 * {@link org.apache.drill.exec.resourcemgr.config.selectors.ResourcePoolSelector} interface.
 * <ul>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectors.DefaultSelector}: It acts as a sink and will always return
 *   true for all the queries. It is associated with a ResourcePool which is not assigned any selector in the
 *   configuration making it a default pool.
 *   </li>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectors.TagSelector}: A selector which has a tag (String) value
 *   associated with it. It evaluates all the tags associated with a query to see if there is a match or not. A query is
 *   only selected by this selector if it has a tag same as that configured for this selector.
 *   </li>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectors.AclSelector}: A selector which has users/groups policies
 *   value associated with it. It evaluates these policies against the users/groups of query session to check if the
 *   query can be selected or not. It supports long/short form syntax to configure the acl policies. It also supports *
 *   as wildcard character to allow/deny all users/groups in its policies. Please see AclSelector class javadoc for more
 *   details.
 *   </li>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectors.OrSelector}: A selector which can have lists of 2 or more
 *   other selectors configured as it's value except for Default selector. It performs || operation on the selection
 *   result of all other configured selectors.</li>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectors.AndSelector}: A selector which can have lists of 2 or more
 *   other selectors configured as it's value except for Default selector. It performs && operation on the selection
 *   result of all other configured selectors.</li>
 *   <li>{@link org.apache.drill.exec.resourcemgr.config.selectors.NotEqualSelector}: A selector which can have any other
 *   selector defined above configured as it's value except for Default selector. It will compare the query metadata
 *   against the configured selector and will return ! of that as a selection result. For example: if a TagSelector
 *   with value "sales" is configured for this NotSelector then it will select queries whose doesn't have sales tag
 *   associated with it.</li>
 * </ul>
 */
package org.apache.drill.exec.resourcemgr.config.selectors;