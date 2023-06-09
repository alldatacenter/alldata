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
package org.apache.drill.exec.ops;

import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.physical.impl.OperatorCreatorRegistry;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.rpc.control.WorkEventBus;
import org.apache.drill.exec.rpc.user.UserServer;
import org.apache.drill.exec.server.QueryProfileStoreContext;
import org.apache.drill.exec.work.batch.IncomingBuffers;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * The context that is used by a Drillbit in classes like the
 * {@link org.apache.drill.exec.work.fragment.FragmentExecutor}.
 */
public interface ExecutorFragmentContext extends RootFragmentContext {

  /**
   * Returns the root allocator for the Drillbit.
   */
  BufferAllocator getRootAllocator();

  PhysicalPlanReader getPlanReader();

  ClusterCoordinator getClusterCoordinator();

  CoordinationProtos.DrillbitEndpoint getForemanEndpoint();

  CoordinationProtos.DrillbitEndpoint getEndpoint();

  Collection<CoordinationProtos.DrillbitEndpoint> getBits();

  OperatorCreatorRegistry getOperatorCreatorRegistry();

  void setBuffers(final IncomingBuffers buffers);

  QueryProfileStoreContext getProfileStoreContext();

  WorkEventBus getWorkEventBus();

  Set<Map.Entry<UserServer.BitToUserConnection, UserServer.BitToUserConnectionConfig>> getUserConnections();

  boolean isUserAuthenticationEnabled();
}
