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
package org.apache.drill.exec.opt;

import org.apache.drill.common.JSONOptions;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.logical.LogicalPlan;
import org.apache.drill.common.logical.PlanProperties;
import org.apache.drill.common.logical.StoragePluginConfig;
import org.apache.drill.common.logical.data.Filter;
import org.apache.drill.common.logical.data.GroupingAggregate;
import org.apache.drill.common.logical.data.Join;
import org.apache.drill.common.logical.data.LogicalOperator;
import org.apache.drill.common.logical.data.Order;
import org.apache.drill.common.logical.data.Order.Ordering;
import org.apache.drill.common.logical.data.Project;
import org.apache.drill.common.logical.data.Scan;
import org.apache.drill.common.logical.data.SinkOperator;
import org.apache.drill.common.logical.data.Store;
import org.apache.drill.common.logical.data.Unnest;
import org.apache.drill.common.logical.data.Window;
import org.apache.drill.common.logical.data.visitors.AbstractLogicalVisitor;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.exception.OptimizerException;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.config.Limit;
import org.apache.drill.exec.physical.config.MergeJoinPOP;
import org.apache.drill.exec.physical.config.Screen;
import org.apache.drill.exec.physical.config.SelectionVectorRemover;
import org.apache.drill.exec.physical.config.Sort;
import org.apache.drill.exec.physical.config.StreamingAggregate;
import org.apache.drill.exec.physical.config.UnnestPOP;
import org.apache.drill.exec.physical.config.WindowPOP;
import org.apache.drill.exec.planner.common.DrillUnnestRelBase;
import org.apache.drill.exec.rpc.UserClientConnection;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.StoragePlugin;
import org.apache.drill.exec.store.StoragePluginRegistry.PluginException;
import org.apache.calcite.rel.RelFieldCollation.Direction;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class BasicOptimizer extends Optimizer {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BasicOptimizer.class);

  private final QueryContext queryContext;
  private final UserClientConnection userSession;

  public BasicOptimizer(final QueryContext queryContext, final UserClientConnection userSession) {
    this.queryContext = queryContext;
    this.userSession = userSession;
  }

  @Override
  public void init(final DrillConfig config) {
  }

  @Override
  public PhysicalPlan optimize(final OptimizationContext context, final LogicalPlan plan)
      throws OptimizerException {
    final Object obj = new Object();
    final Collection<SinkOperator> roots = plan.getGraph().getRoots();
    final List<PhysicalOperator> physOps = new ArrayList<>(roots.size());
    final LogicalConverter converter = new LogicalConverter(plan);

    for (SinkOperator op : roots) {
      final PhysicalOperator pop  = op.accept(converter, obj);
      physOps.add(pop);
    }

    final PlanProperties logicalProperties = plan.getProperties();
    final PlanProperties props = PlanProperties.builder()
        .type(PlanProperties.PlanType.APACHE_DRILL_PHYSICAL)
        .version(logicalProperties.version)
        .generator(logicalProperties.generator)
        .options(new JSONOptions(context.getOptions().getOptionList())).build();
    return new PhysicalPlan(props, physOps);
  }

  public static class BasicOptimizationContext implements OptimizationContext {
    private final OptionManager ops;
    public BasicOptimizationContext(final QueryContext c) {
      ops = c.getOptions();
    }

    @Override
    public int getPriority() {
      return 1;
    }

    @Override
    public OptionManager getOptions() {
      return ops;
    }
  }

  private class LogicalConverter extends AbstractLogicalVisitor<PhysicalOperator, Object, OptimizerException> {

    /*
     * Store a reference to the plan for access to other elements outside of the query graph
     * such as the storage engine configs.
     */
    private final LogicalPlan logicalPlan;

    LogicalConverter(final LogicalPlan logicalPlan) {
      this.logicalPlan = logicalPlan;
    }

    @Override
    public PhysicalOperator visitGroupingAggregate(GroupingAggregate groupBy, Object value) throws OptimizerException {
      PhysicalOperator input = groupBy.getInput().accept(this, value);

      if (groupBy.getKeys().size() > 0) {
        List<Ordering> orderDefs = groupBy.getKeys().stream()
            .map(e -> new Ordering(Direction.ASCENDING, e.getExpr(), NullDirection.FIRST))
            .collect(Collectors.toList());
        input = new Sort(input, orderDefs, false);
      }

      return new StreamingAggregate(input, groupBy.getKeys(), groupBy.getExprs());
    }

    @Override
    public PhysicalOperator visitWindow(final Window window, final Object value) throws OptimizerException {
      PhysicalOperator input = window.getInput().accept(this, value);
      final List<Ordering> ods = new ArrayList<>();

      input = new Sort(input, ods, false);

      return new WindowPOP(input, window.getWithins(), window.getAggregations(),
          window.getOrderings(), false, null, null);
    }

    @Override
    public PhysicalOperator visitOrder(final Order order, final Object value) throws OptimizerException {
      final PhysicalOperator input = order.getInput().accept(this, value);
      final List<Ordering> ods = new ArrayList<>(order.getOrderings());
      return new SelectionVectorRemover(new Sort(input, ods, false));
    }

    @Override
    public PhysicalOperator visitLimit(final org.apache.drill.common.logical.data.Limit limit,
        final Object value) throws OptimizerException {
      final PhysicalOperator input = limit.getInput().accept(this, value);
      return new SelectionVectorRemover(new Limit(input, limit.getFirst(), limit.getLast()));
    }

    @Override
    public PhysicalOperator visitJoin(final Join join, final Object value) throws OptimizerException {
      PhysicalOperator leftOp = join.getLeft().accept(this, value);

      List<Ordering> leftOrderDefs = join.getConditions().stream()
          .map(jc -> new Ordering(Direction.ASCENDING, jc.getLeft()))
          .collect(Collectors.toList());

      leftOp = new Sort(leftOp, leftOrderDefs, false);
      leftOp = new SelectionVectorRemover(leftOp);

      PhysicalOperator rightOp = join.getRight().accept(this, value);

      List<Ordering> rightOrderDefs = join.getConditions().stream()
          .map(jc -> new Ordering(Direction.ASCENDING, jc.getRight()))
          .collect(Collectors.toList());

      rightOp = new Sort(rightOp, rightOrderDefs, false);
      rightOp = new SelectionVectorRemover(rightOp);

      final MergeJoinPOP mjp = new MergeJoinPOP(leftOp, rightOp, join.getConditions(), join.getJoinType());
      return new SelectionVectorRemover(mjp);
    }

    @Override
    public PhysicalOperator visitScan(final Scan scan, final Object obj) throws OptimizerException {
      final StoragePluginConfig config = logicalPlan.getStorageEngineConfig(scan.getStorageEngine());
      if(config == null) {
        throw new OptimizerException(
            String.format("Logical plan referenced the storage engine config %s but the logical plan didn't have that available as a config.",
                scan.getStorageEngine()));
      }
      try {
        final StoragePlugin storagePlugin = queryContext.getStorage().getPluginByConfig(config);
        final String user = userSession.getSession().getCredentials().getUserName();
        return storagePlugin.getPhysicalScan(user, scan.getSelection(), userSession.getSession().getOptions());
      } catch (IOException | PluginException e) {
        throw new OptimizerException("Failure while attempting to retrieve storage engine.", e);
      }
    }

    @Override
    public PhysicalOperator visitStore(final Store store, final Object obj) throws OptimizerException {
      LogicalOperator input = store.getInput();
      if (input == null) {
        throw new OptimizerException("Store node in logical plan does not have a child.");
      }
      return new Screen(store.getInput().accept(this, obj), queryContext.getCurrentEndpoint());
    }

    @Override
    public PhysicalOperator visitProject(final Project project, final Object obj) throws OptimizerException {
      return new org.apache.drill.exec.physical.config.Project(
          project.getSelections(), project.getInput().accept(this, obj));
    }

    @Override
    public PhysicalOperator visitFilter(final Filter filter, final Object obj) throws OptimizerException {
      final TypeProtos.MajorType.Builder b = TypeProtos.MajorType.getDefaultInstance().newBuilderForType();
      b.setMode(DataMode.REQUIRED);
      b.setMinorType(MinorType.BIGINT);
      final PhysicalOperator child = filter.getInput().accept(this, obj);
      return new SelectionVectorRemover(new org.apache.drill.exec.physical.config.Filter(child, filter.getExpr(), 1.0f));
    }

    @Override
    public PhysicalOperator visitUnnest(final Unnest unnest, final Object obj) {
      return new UnnestPOP(null, unnest.getColumn(), DrillUnnestRelBase.IMPLICIT_COLUMN);
    }
  }
}
