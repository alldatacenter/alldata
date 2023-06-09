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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.util.DrillStringUtils;
import org.apache.drill.common.util.function.CheckedConsumer;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.ops.QueryContext;
import org.apache.drill.exec.physical.MinorFragmentEndpoint;
import org.apache.drill.exec.physical.PhysicalOperatorSetupException;
import org.apache.drill.exec.physical.base.AbstractPhysicalVisitor;
import org.apache.drill.exec.physical.base.Exchange.ParallelizationDependency;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.Receiver;
import org.apache.drill.exec.planner.PhysicalPlanReader;
import org.apache.drill.exec.planner.fragment.Fragment.ExchangeFragmentPair;
import org.apache.drill.exec.planner.fragment.Materializer.IndexedFragmentNode;
import org.apache.drill.exec.proto.BitControl.Collector;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.BitControl.QueryContextInformation;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.rpc.user.UserSession;
import org.apache.drill.exec.server.options.OptionList;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.work.QueryWorkUnit;
import org.apache.drill.exec.work.QueryWorkUnit.MinorFragmentDefn;
import org.apache.drill.exec.work.foreman.ForemanSetupException;


/**
 * The simple parallelizer determines the level of parallelization of a plan
 * based on the cost of the underlying operations. It doesn't take into account
 * system load or other factors. Based on the cost of the query, the
 * parallelization for each major fragment will be determined. Once the amount
 * of parallelization is done, assignment is done based on round robin
 * assignment ordered by operator affinity (locality) to available execution
 * Drillbits.
 */
public abstract class SimpleParallelizer implements QueryParallelizer {
  static final Logger logger = LoggerFactory.getLogger(SimpleParallelizer.class);

  private final long parallelizationThreshold;
  private final int maxWidthPerNode;
  private final int maxGlobalWidth;
  private final double affinityFactor;
  private boolean enableDynamicFC;

  protected SimpleParallelizer(QueryContext context) {
    OptionManager optionManager = context.getOptions();
    long sliceTarget = optionManager.getOption(ExecConstants.SLICE_TARGET_OPTION);
    this.parallelizationThreshold = sliceTarget > 0 ? sliceTarget : 1;
    double cpu_load_average = optionManager.getOption(ExecConstants.CPU_LOAD_AVERAGE);
    final long maxWidth = optionManager.getOption(ExecConstants.MAX_WIDTH_PER_NODE);
    // compute the maxwidth
    this.maxWidthPerNode = ExecConstants.MAX_WIDTH_PER_NODE.computeMaxWidth(cpu_load_average, maxWidth);
    this.maxGlobalWidth = optionManager.getOption(ExecConstants.MAX_WIDTH_GLOBAL_KEY).num_val.intValue();
    this.affinityFactor = optionManager.getOption(ExecConstants.AFFINITY_FACTOR_KEY).float_val.intValue();
    this.enableDynamicFC = optionManager.getBoolean(ExecConstants.ENABLE_DYNAMIC_CREDIT_BASED_FC);
  }

  protected SimpleParallelizer(long parallelizationThreshold, int maxWidthPerNode, int maxGlobalWidth, double affinityFactor) {
    this.parallelizationThreshold = parallelizationThreshold;
    this.maxWidthPerNode = maxWidthPerNode;
    this.maxGlobalWidth = maxGlobalWidth;
    this.affinityFactor = affinityFactor;
  }

  @Override
  public long getSliceTarget() {
    return parallelizationThreshold;
  }

  @Override
  public int getMaxWidthPerNode() {
    return maxWidthPerNode;
  }

  @Override
  public int getMaxGlobalWidth() {
    return maxGlobalWidth;
  }

  @Override
  public double getAffinityFactor() {
    return affinityFactor;
  }

  public Set<Wrapper> getRootFragments(PlanningSet planningSet) {
    // Gets the root fragment by removing all the dependent fragments on which
    // root fragments depend upon. This is fine because the later parallelizer
    // code traverses from these root fragments to their respective dependent
    // fragments.
    final Set<Wrapper> roots = Sets.newHashSet();
    for (Wrapper w : planningSet) {
      roots.add(w);
    }

    // Roots will be left over with the fragments which are not depended upon by any other fragments.
    for (Wrapper wrapper : planningSet) {
      final List<Wrapper> fragmentDependencies = wrapper.getFragmentDependencies();
      if (fragmentDependencies != null && fragmentDependencies.size() > 0) {
        for (Wrapper dependency : fragmentDependencies) {
          if (roots.contains(dependency)) {
            roots.remove(dependency);
          }
        }
      }
    }

    return roots;
  }

  public PlanningSet prepareFragmentTree(Fragment rootFragment) {
    PlanningSet planningSet = new PlanningSet();

    initFragmentWrappers(rootFragment, planningSet);

    constructFragmentDependencyGraph(rootFragment, planningSet);

    return planningSet;
  }

  /**
   * Traverse all the major fragments and parallelize each major fragment based on
   * collected stats. The children fragments are parallelized before a parent
   * fragment.
   * @param planningSet Set of all major fragments and their context.
   * @param roots Root nodes of the plan.
   * @param activeEndpoints currently active drillbit endpoints.
   * @throws PhysicalOperatorSetupException
   */
  public void collectStatsAndParallelizeFragments(PlanningSet planningSet, Set<Wrapper> roots,
                                                  Collection<DrillbitEndpoint> activeEndpoints) throws PhysicalOperatorSetupException {
    for (Wrapper wrapper : roots) {
      traverse(wrapper, CheckedConsumer.throwingConsumerWrapper((Wrapper fragmentWrapper) -> {
        // If this fragment is already parallelized then no need do it again.
        // This happens in the case of fragments which have MUX operators.
        if (fragmentWrapper.isEndpointsAssignmentDone()) {
          return;
        }
        fragmentWrapper.getNode().getRoot().accept(new StatsCollector(planningSet), fragmentWrapper);
        fragmentWrapper.getStats()
                       .getDistributionAffinity()
                       .getFragmentParallelizer()
                       .parallelizeFragment(fragmentWrapper, this, activeEndpoints);
        //consolidate the cpu resources required by this major fragment per drillbit.
        fragmentWrapper.computeCpuResources();
      }));
    }
  }

  public abstract void adjustMemory(PlanningSet planningSet, Set<Wrapper> roots,
                                    Collection<DrillbitEndpoint> activeEndpoints) throws PhysicalOperatorSetupException;

  /**
   * The starting function for the whole parallelization and memory computation logic.
   * 1) Initially a fragment tree is prepared which contains a wrapper for each fragment.
   *    The topology of this tree is same as that of the major fragment tree.
   * 2) Traverse this fragment tree to collect stats for each major fragment and then
   *    parallelize each fragment. At this stage minor fragments are not created but all
   *    the required information to create minor fragment are computed.
   * 3) Memory is computed for each operator and for the minor fragment.
   * 4) Lastly all the above computed information is used to create the minor fragments
   *    for each major fragment.
   *
   * @param options List of options set by the user.
   * @param foremanNode foreman node for this query plan.
   * @param queryId  Query ID.
   * @param activeEndpoints currently active endpoints on which this plan will run.
   * @param rootFragment Root major fragment.
   * @param session session context.
   * @param queryContextInfo query context.
   * @return
   * @throws ExecutionSetupException
   */
  @Override
  public final QueryWorkUnit generateWorkUnit(OptionList options, DrillbitEndpoint foremanNode, QueryId queryId,
                                              Collection<DrillbitEndpoint> activeEndpoints, Fragment rootFragment,
                                              UserSession session, QueryContextInformation queryContextInfo) throws ExecutionSetupException {
    PlanningSet planningSet = prepareFragmentTree(rootFragment);

    Set<Wrapper> rootFragments = getRootFragments(planningSet);

    collectStatsAndParallelizeFragments(planningSet, rootFragments, activeEndpoints);

    adjustMemory(planningSet, rootFragments, activeEndpoints);

    return generateWorkUnit(options, foremanNode, queryId, rootFragment, planningSet, session, queryContextInfo);
  }

  /**
   * Create multiple physical plans from original query planning, it will allow execute them eventually independently
   * @param options
   * @param foremanNode
   * @param queryId
   * @param activeEndpoints
   * @param reader
   * @param rootFragment
   * @param session
   * @param queryContextInfo
   * @return The {@link QueryWorkUnit}s.
   * @throws ExecutionSetupException
   */
  public List<QueryWorkUnit> getSplitFragments(OptionList options, DrillbitEndpoint foremanNode, QueryId queryId,
      Collection<DrillbitEndpoint> activeEndpoints, PhysicalPlanReader reader, Fragment rootFragment,
      UserSession session, QueryContextInformation queryContextInfo) throws ExecutionSetupException {
    // no op
    throw new UnsupportedOperationException("Use children classes");
  }

  // For every fragment, create a Wrapper in PlanningSet.
  @VisibleForTesting
  public void initFragmentWrappers(Fragment rootFragment, PlanningSet planningSet) {
    planningSet.get(rootFragment);

    for (ExchangeFragmentPair fragmentPair : rootFragment) {
      initFragmentWrappers(fragmentPair.getNode(), planningSet);
    }
  }

  /**
   * Based on the affinity of the Exchange that separates two fragments, setup fragment dependencies.
   *
   * @param planningSet
   * @return Returns a list of leaf fragments in fragment dependency graph.
   */
  private void constructFragmentDependencyGraph(Fragment rootFragment, PlanningSet planningSet) {

    // Set up dependency of fragments based on the affinity of exchange that separates the fragments.
    for (Wrapper currentFragment : planningSet) {
      ExchangeFragmentPair sendingXchgForCurrFrag = currentFragment.getNode().getSendingExchangePair();
      if (sendingXchgForCurrFrag != null) {
        ParallelizationDependency dependency = sendingXchgForCurrFrag.getExchange().getParallelizationDependency();
        Wrapper receivingFragmentWrapper = planningSet.get(sendingXchgForCurrFrag.getNode());

        // Mostly Receivers of the current fragment depend on the sender of the child fragments.
        // However there is a special case for DeMux Exchanges where the Sender of the current
        // fragment depends on the receiver of the parent fragment.
        if (dependency == ParallelizationDependency.RECEIVER_DEPENDS_ON_SENDER) {
          receivingFragmentWrapper.addFragmentDependency(currentFragment);
        } else if (dependency == ParallelizationDependency.SENDER_DEPENDS_ON_RECEIVER) {
          currentFragment.addFragmentDependency(receivingFragmentWrapper);
        }
      }
    }
    planningSet.findRootWrapper(rootFragment);
  }

  /**
   * Call operation on each fragment. Traversal calls operation
   * on child fragments before calling it on the parent fragment.
   */
  protected void traverse(Wrapper fragmentWrapper, Consumer<Wrapper> operation) throws PhysicalOperatorSetupException {

    final List<Wrapper> fragmentDependencies = fragmentWrapper.getFragmentDependencies();
    if (fragmentDependencies != null && fragmentDependencies.size() > 0) {
      for (Wrapper dependency : fragmentDependencies) {
        traverse(dependency, operation);
      }
    }
    operation.accept(fragmentWrapper);
  }

  // A function which returns the memory assigned for a particular physical operator.
  protected abstract BiFunction<DrillbitEndpoint, PhysicalOperator, Long> getMemory();

  protected QueryWorkUnit generateWorkUnit(OptionList options, DrillbitEndpoint foremanNode, QueryId queryId,
                                           Fragment rootNode, PlanningSet planningSet, UserSession session,
                                           QueryContextInformation queryContextInfo) throws ExecutionSetupException {
    List<MinorFragmentDefn> fragmentDefns = new ArrayList<>( );

    MinorFragmentDefn rootFragmentDefn = null;
    FragmentRoot rootOperator = null;
    // Generate all the individual plan fragments and associated assignments. Note, we need all endpoints
    // assigned before we can materialize.
    for (Wrapper wrapper : planningSet) {
      Fragment node = wrapper.getNode();
      final PhysicalOperator physicalOperatorRoot = node.getRoot();
      boolean isRootNode = rootNode == node;

      if (isRootNode && wrapper.getWidth() != 1) {
        throw new ForemanSetupException(String.format("Failure while trying to setup fragment. " +
                "The root fragment must always have parallelization one. In the current case, the width was set to %d.",
                wrapper.getWidth()));
      }
      // A fragment is self-driven if it doesn't rely on any other exchanges.
      boolean isLeafFragment = node.getReceivingExchangePairs().size() == 0;
      // Create a minorFragment for each major fragment.
      for (int minorFragmentId = 0; minorFragmentId < wrapper.getWidth(); minorFragmentId++) {
        IndexedFragmentNode iNode = new IndexedFragmentNode(minorFragmentId, wrapper,
          (fragmentWrapper, minorFragment) -> fragmentWrapper.getAssignedEndpoint(minorFragment), getMemory());
        wrapper.resetAllocation();
        PhysicalOperator op = physicalOperatorRoot.accept(Materializer.INSTANCE, iNode);
        Preconditions.checkArgument(op instanceof FragmentRoot);
        FragmentRoot root = (FragmentRoot) op;

        FragmentHandle handle = FragmentHandle
            .newBuilder()
            .setMajorFragmentId(wrapper.getMajorFragmentId())
            .setMinorFragmentId(minorFragmentId)
            .setQueryId(queryId)
            .build();

        PlanFragment fragment = PlanFragment.newBuilder()
            .setForeman(foremanNode)
            .setHandle(handle)
            .setAssignment(wrapper.getAssignedEndpoint(minorFragmentId))
            .setLeafFragment(isLeafFragment)
            .setContext(queryContextInfo)
            .setMemInitial(wrapper.getInitialAllocation())
            .setMemMax(wrapper.getMaxAllocation())
            .setCredentials(session.getCredentials())
            .addAllCollector(CountRequiredFragments.getCollectors(root, enableDynamicFC))
            .build();

        MinorFragmentDefn fragmentDefn = new MinorFragmentDefn(fragment, root, options);

        if (isRootNode) {
          logger.debug("Root fragment:\n {}", DrillStringUtils.unescapeJava(fragment.toString()));
          rootFragmentDefn = fragmentDefn;
          rootOperator = root;
        } else {
          logger.debug("Remote fragment:\n {}", DrillStringUtils.unescapeJava(fragment.toString()));
          fragmentDefns.add(fragmentDefn);
        }
      }
    }
    Wrapper rootWrapper = planningSet.getRootWrapper();
    return new QueryWorkUnit(rootOperator, rootFragmentDefn, fragmentDefns, rootWrapper);
  }

  /**
   * Designed to setup initial values for arriving fragment accounting.
   */

  protected static class CountRequiredFragments extends AbstractPhysicalVisitor<Void, List<Collector>, RuntimeException> {
    private boolean enableDynamicFC;

    CountRequiredFragments(boolean enableDynamicFC) {
      this.enableDynamicFC = enableDynamicFC;
    }

    public static List<Collector> getCollectors(PhysicalOperator root, boolean enableDynamicFC) {
      List<Collector> collectors = Lists.newArrayList();
      CountRequiredFragments countRequiredFragments = new CountRequiredFragments(enableDynamicFC);
      root.accept(countRequiredFragments, collectors);
      return collectors;
    }

    @Override
    public Void visitReceiver(Receiver receiver, List<Collector> collectors) throws RuntimeException {
      List<MinorFragmentEndpoint> endpoints = receiver.getProvidingEndpoints();
      List<Integer> list = new ArrayList<>(endpoints.size());
      for (MinorFragmentEndpoint ep : endpoints) {
        list.add(ep.getId());
      }

      collectors.add(Collector.newBuilder()
        .setIsSpooling(receiver.isSpooling())
        .setOppositeMajorFragmentId(receiver.getOppositeMajorFragmentId())
        .setSupportsOutOfOrder(receiver.supportsOutOfOrderExchange())
        .setEnableDynamicFc(enableDynamicFC)
          .addAllIncomingMinorFragment(list)
          .build());
      return null;
    }

    @Override
    public Void visitOp(PhysicalOperator op, List<Collector> collectors) throws RuntimeException {
      for (PhysicalOperator o : op) {
        o.accept(this, collectors);
      }
      return null;
    }
  }
}
