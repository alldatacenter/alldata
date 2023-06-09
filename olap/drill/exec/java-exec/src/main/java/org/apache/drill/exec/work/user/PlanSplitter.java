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
package org.apache.drill.exec.work.user;

import java.util.List;

import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.planner.fragment.Fragment;
import org.apache.drill.exec.planner.fragment.MakeFragmentsVisitor;
import org.apache.drill.exec.planner.fragment.SimpleParallelizer;
import org.apache.drill.exec.planner.fragment.contrib.SplittingParallelizer;
import org.apache.drill.exec.planner.sql.DrillSqlWorker;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.UserProtos.GetQueryPlanFragments;
import org.apache.drill.exec.proto.UserProtos.QueryPlanFragments;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.util.Pointer;
import org.apache.drill.exec.work.QueryWorkUnit;
import org.apache.drill.exec.work.foreman.rm.QueryResourceAllocator;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * Helper class to return PlanFragments based on the query plan
 * or based on split query plan
 * As of now it is only invoked once per query and therefore cheap to create PlanSplitter object
 * on heap.
 */
public class PlanSplitter {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PlanSplitter.class);


  /**
   * Method to plan the query and return list of fragments
   * it will return query plan "as is" or split plans based on the req setting: split_plan
   * @param dContext
   * @param queryId
   * @param req
   * @param connection
   * @return
   */
  public QueryPlanFragments planFragments(DrillbitContext dContext, QueryId queryId,
      GetQueryPlanFragments req, UserClientConnection connection) {
    QueryPlanFragments.Builder responseBuilder = QueryPlanFragments.newBuilder();
    final QueryContext queryContext = new QueryContext(connection.getSession(), dContext, queryId);

    responseBuilder.setQueryId(queryId);

    try {
      responseBuilder.addAllFragments(getFragments(dContext, req, queryContext, queryId));
      responseBuilder.setStatus(QueryState.COMPLETED);
    } catch (Exception e) {
      final String errorMessage = String.format("Failed to produce PlanFragments for query id \"%s\" with "
          + "request to %s plan", queryId, (req.getSplitPlan() ? "split" : "no split"));
      DrillPBError error = DrillPBError.newBuilder().setMessage(errorMessage).setErrorType(DrillPBError.ErrorType.PLAN).build();

      responseBuilder.setStatus(QueryState.FAILED);
      responseBuilder.setError(error);
    }

    try {
      queryContext.close();
    } catch (Exception e) {
      logger.error("Error closing QueryContext when getting plan fragments for query {}.",
        QueryIdHelper.getQueryId(queryId), e);
    }

    return responseBuilder.build();
  }

  private List<PlanFragment> getFragments(final DrillbitContext dContext, final GetQueryPlanFragments req,
      final QueryContext queryContext, final QueryId queryId) throws Exception {
    final PhysicalPlan plan;
    final String query = req.getQuery();
    switch(req.getType()) {
    case SQL:
      final Pointer<String> textPlan = new Pointer<>();
      plan = DrillSqlWorker.getPlan(queryContext, query, textPlan);
      break;
    case PHYSICAL:
      plan = dContext.getPlanReader().readPhysicalPlan(query);
      break;
    default:
      throw new IllegalStateException("Planning fragments supports only SQL or PHYSICAL QueryType");
    }

    QueryResourceAllocator planner = dContext.getResourceManager().newResourceAllocator(queryContext);
    planner.visitAbstractPlan(plan);

    final PhysicalOperator rootOperator = plan.getSortedOperators(false).iterator().next();

    final Fragment rootFragment = rootOperator.accept(MakeFragmentsVisitor.INSTANCE, null);
    final SimpleParallelizer parallelizer = new SplittingParallelizer(plan.getProperties().hasResourcePlan, queryContext);

    List<PlanFragment> fragments = Lists.newArrayList();

    if ( req.getSplitPlan() ) {
      final List<QueryWorkUnit> queryWorkUnits = parallelizer.getSplitFragments(
          queryContext.getOptions().getOptionList(), queryContext.getCurrentEndpoint(),
          queryId, queryContext.getActiveEndpoints(), dContext.getPlanReader(), rootFragment,
          queryContext.getSession(), queryContext.getQueryContextInfo());

      for (QueryWorkUnit queryWorkUnit : queryWorkUnits) {
        planner.visitPhysicalPlan(queryWorkUnit);
        queryWorkUnit.applyPlan(dContext.getPlanReader());
        fragments.add(queryWorkUnit.getRootFragment());

        List<PlanFragment> childFragments = queryWorkUnit.getFragments();
        if (!childFragments.isEmpty()) {
          throw new IllegalStateException("Split plans can not have more then one fragment");
        }
      }
    } else {
      final QueryWorkUnit queryWorkUnit = parallelizer.generateWorkUnit(queryContext.getOptions().getOptionList(), queryContext.getCurrentEndpoint(),
          queryId, queryContext.getActiveEndpoints(), rootFragment,
          queryContext.getSession(), queryContext.getQueryContextInfo());
      planner.visitPhysicalPlan(queryWorkUnit);
      queryWorkUnit.applyPlan(dContext.getPlanReader());
      fragments.add(queryWorkUnit.getRootFragment());
      fragments.addAll(queryWorkUnit.getFragments());
    }
    return fragments;
  }

}
