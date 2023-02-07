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

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.resourcemgr.NodeResources;
import org.apache.drill.exec.resourcemgr.config.exception.QueueSelectionException;
import org.apache.drill.exec.resourcemgr.config.selectionpolicy.QueueSelectionPolicy;

import java.util.Map;

/**
 * Interface which defines the implementation of a hierarchical configuration for all the ResourcePool that will be
 * used for ResourceManagement
 */
public interface ResourcePoolTree {

  ResourcePool getRootPool();

  Map<String, QueryQueueConfig> getAllLeafQueues();

  double getResourceShare();

  QueueAssignmentResult selectAllQueues(QueryContext queryContext);

  QueryQueueConfig selectOneQueue(QueryContext queryContext, NodeResources queryMaxNodeResource)
    throws QueueSelectionException;

  QueueSelectionPolicy getSelectionPolicyInUse();
}
