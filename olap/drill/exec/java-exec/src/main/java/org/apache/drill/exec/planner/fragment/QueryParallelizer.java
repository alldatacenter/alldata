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

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.proto.BitControl.QueryContextInformation;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.options.OptionList;
import org.apache.drill.exec.work.QueryWorkUnit;

import java.util.Collection;

/**
 * Parallelizes the query plan. Once the optimizer finishes its job by producing a
 * optimized plan, it is the job of this parallelizer to generate a parallel plan out of the
 * optimized physical plan. It does so by using the optimizers estimates for row count etc.
 * There are two kinds of parallelizers as explained below. Currently the difference in
 * both of these parallelizers is only in the memory assignment for the physical operators.
 * <p>
 * a) Default Parallelizer: It optimistically assumes that the whole cluster is running only the
 *    current query and based on heuristics assigns the optimal memory to the buffered operators.
 * <p>
 * b) Queue Parallelizer: This parallelizer computes the memory that can be allocated at best based
 *    on the current cluster state(as to how much memory is available) and also the configuration
 *    of the queue that it can run on.
 */
public interface QueryParallelizer extends ParallelizationParameters {

  /**
   * This is the only function exposed to the consumer of this parallelizer (currently Foreman) to parallelize
   * the plan. The caller transforms the plan to a fragment tree and supplies the required information for
   * the parallelizer to do its job.
   * @param options List of all options that are set for the current session.
   * @param foremanNode Endpoint information of the foreman node.
   * @param queryId Unique ID of the query.
   * @param activeEndpoints Currently active endpoints on which the plan can run.
   * @param rootFragment root of the fragment tree of the transformed physical plan
   * @param session user session object.
   * @param queryContextInfo query context.
   * @return Executable query plan which contains all the information like minor frags and major frags.
   * @throws ExecutionSetupException
   */
  QueryWorkUnit generateWorkUnit(OptionList options, DrillbitEndpoint foremanNode, QueryId queryId,
                                 Collection<DrillbitEndpoint> activeEndpoints, Fragment rootFragment,
                                 UserSession session, QueryContextInformation queryContextInfo) throws ExecutionSetupException;
}
