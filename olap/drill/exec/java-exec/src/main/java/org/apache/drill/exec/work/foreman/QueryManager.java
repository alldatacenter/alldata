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
package org.apache.drill.exec.work.foreman;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.netty.buffer.ByteBuf;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.coord.store.TransientStore;
import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.GeneralRPCProtos.Ack;
import org.apache.drill.exec.proto.UserBitShared.FragmentState;
import org.apache.drill.exec.proto.UserBitShared.MajorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.UserBitShared.QueryInfo;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile;
import org.apache.drill.exec.proto.UserBitShared.QueryProfile.Builder;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.proto.UserProtos.RunQuery;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.rpc.RpcException;
import org.apache.drill.exec.rpc.control.Controller;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.server.options.OptionList;
import org.apache.drill.exec.store.sys.PersistentStore;
import org.apache.drill.exec.store.sys.PersistentStoreProvider;
import org.apache.drill.exec.work.EndpointListener;

import com.carrotsearch.hppc.IntObjectHashMap;
import com.carrotsearch.hppc.predicates.IntObjectPredicate;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Each Foreman holds its own QueryManager. This manages the events associated
 * with execution of a particular query across all fragments.
 */
public class QueryManager implements AutoCloseable {
  private static final Logger logger = LoggerFactory.getLogger(QueryManager.class);

  private final Map<DrillbitEndpoint, NodeTracker> nodeMap = Maps.newHashMap();
  private final QueryId queryId;
  private final String stringQueryId;
  private final RunQuery runQuery;
  private final Foreman foreman;

  /*
   * Doesn't need to be thread safe as fragmentDataMap is generated in a single thread and then
   * accessed by multiple threads for reads only.
   */
  private final IntObjectHashMap<IntObjectHashMap<FragmentData>> fragmentDataMap =
      new IntObjectHashMap<>();
  private final List<FragmentData> fragmentDataSet = Lists.newArrayList();

  private final PersistentStore<QueryProfile> completedProfileStore;
  private final TransientStore<QueryInfo> runningProfileStore;

  // the following mutable variables are used to capture ongoing query status
  private String planText;
  private long startTime = System.currentTimeMillis();
  private long endTime;
  private long planningEndTime;
  private long queueWaitEndTime;

  // How many nodes have finished their execution.  Query is complete when all nodes are complete.
  private final AtomicInteger finishedNodes = new AtomicInteger(0);

  // How many fragments have finished their execution.
  private final AtomicInteger finishedFragments = new AtomicInteger(0);

  // Is the query saved in transient store
  private boolean inTransientStore;

  /**
   * Total query cost. This value is used to place the query into a queue
   * and so has meaning to the user who wants to predict queue placement.
   */

  private double totalCost;

  private String queueName;

  public QueryManager(final QueryId queryId, final RunQuery runQuery, final PersistentStoreProvider storeProvider,
      final ClusterCoordinator coordinator, final Foreman foreman) {
    this.queryId =  queryId;
    this.runQuery = runQuery;
    this.foreman = foreman;

    stringQueryId = QueryIdHelper.getQueryId(queryId);

    this.completedProfileStore = foreman.getQueryContext().getProfileStoreContext().getCompletedProfileStore();
    this.runningProfileStore = foreman.getQueryContext().getProfileStoreContext().getRunningProfileStore();
  }

  private static boolean isTerminal(final FragmentState state) {
    return state == FragmentState.FAILED
        || state == FragmentState.FINISHED
        || state == FragmentState.CANCELLED;
  }

  private boolean updateFragmentStatus(final FragmentStatus fragmentStatus) {
    final FragmentHandle fragmentHandle = fragmentStatus.getHandle();
    final int majorFragmentId = fragmentHandle.getMajorFragmentId();
    final int minorFragmentId = fragmentHandle.getMinorFragmentId();
    final FragmentData data = fragmentDataMap.get(majorFragmentId).get(minorFragmentId);

    final FragmentState oldState = data.getState();
    final boolean inTerminalState = isTerminal(oldState);
    final FragmentState currentState = fragmentStatus.getProfile().getState();

    if (inTerminalState || (oldState == FragmentState.CANCELLATION_REQUESTED && !isTerminal(currentState))) {
      // Already in a terminal state, or invalid state transition from CANCELLATION_REQUESTED. This shouldn't happen.
      logger.warn(String.format("Received status message for fragment %s after fragment was in state %s. New state was %s",
        QueryIdHelper.getQueryIdentifier(fragmentHandle), oldState, currentState));
      return false;
    }

    data.setStatus(fragmentStatus);
    return oldState != currentState;
  }

  private void fragmentDone(final FragmentStatus status) {
    final boolean stateChanged = updateFragmentStatus(status);

    if (stateChanged) {
      // since we're in the fragment done clause and this was a change from previous
      final NodeTracker node = nodeMap.get(status.getProfile().getEndpoint());
      node.fragmentComplete();
      finishedFragments.incrementAndGet();
    }
  }

  private void addFragment(final FragmentData fragmentData) {
    final FragmentHandle fragmentHandle = fragmentData.getHandle();
    final int majorFragmentId = fragmentHandle.getMajorFragmentId();
    final int minorFragmentId = fragmentHandle.getMinorFragmentId();

    IntObjectHashMap<FragmentData> minorMap = fragmentDataMap.get(majorFragmentId);
    if (minorMap == null) {
      minorMap = new IntObjectHashMap<>();
      fragmentDataMap.put(majorFragmentId, minorMap);
    }
    minorMap.put(minorFragmentId, fragmentData);
    fragmentDataSet.add(fragmentData);
  }

  public String getFragmentStatesAsString() {
    return fragmentDataMap.toString();
  }

  void addFragmentStatusTracker(final PlanFragment fragment, final boolean isRoot) {
    final DrillbitEndpoint assignment = fragment.getAssignment();

    NodeTracker tracker = nodeMap.get(assignment);
    if (tracker == null) {
      tracker = new NodeTracker(assignment);
      nodeMap.put(assignment, tracker);
    }

    tracker.addFragment();
    addFragment(new FragmentData(fragment.getHandle(), assignment, isRoot));
  }

  /**
   * Stop all fragments with currently *known* active status (active as in SENDING, AWAITING_ALLOCATION, RUNNING).
   *
   * For the actual cancel calls for intermediate and leaf fragments, see
   * {@link org.apache.drill.exec.work.batch.ControlMessageHandler#cancelFragment}
   * (1) Root fragment: pending or running, send the cancel signal through a tunnel.
   * (2) Intermediate fragment: pending or running, send the cancel signal through a tunnel (for local and remote
   *    fragments). The actual cancel is done by delegating the cancel to the work bus.
   * (3) Leaf fragment: running, send the cancel signal through a tunnel. The cancel is done directly.
   */
  void cancelExecutingFragments(final DrillbitContext drillbitContext) {
    final Controller controller = drillbitContext.getController();
    for(final FragmentData data : fragmentDataSet) {
      switch(data.getState()) {
      case SENDING:
      case AWAITING_ALLOCATION:
      case RUNNING:
        final FragmentHandle handle = data.getHandle();
        final DrillbitEndpoint endpoint = data.getEndpoint();
        // TODO is the CancelListener redundant? Does the FragmentStatusListener get notified of the same?
        controller.getTunnel(endpoint).cancelFragment(new SignalListener(endpoint, handle,
            SignalListener.Signal.CANCEL), handle);
        break;

      case FINISHED:
      case CANCELLATION_REQUESTED:
      case CANCELLED:
      case FAILED:
        // nothing to do
        break;
      }
    }
  }

  /**
   * Sends a resume signal to all fragments, regardless of their state, since the fragment might have paused before
   * sending any message. Resume all fragments through the control tunnel.
   */
  void unpauseExecutingFragments(final DrillbitContext drillbitContext) {
    final Controller controller = drillbitContext.getController();
    for(final FragmentData data : fragmentDataSet) {
      final DrillbitEndpoint endpoint = data.getEndpoint();
      final FragmentHandle handle = data.getHandle();
      controller.getTunnel(endpoint).unpauseFragment(new SignalListener(endpoint, handle,
        SignalListener.Signal.UNPAUSE), handle);
    }
  }

  @Override
  public void close() throws Exception { }

  /*
   * This assumes that the FragmentStatusListener implementation takes action when it hears
   * that the target fragment has acknowledged the signal. As a result, this listener doesn't do anything
   * but log messages.
   */
  private static class SignalListener extends EndpointListener<Ack, FragmentHandle> {
    /**
     * An enum of possible signals that {@link SignalListener} listens to.
     */
    public static enum Signal { CANCEL, UNPAUSE }

    private final Signal signal;

    public SignalListener(final DrillbitEndpoint endpoint, final FragmentHandle handle, final Signal signal) {
      super(endpoint, handle);
      this.signal = signal;
    }

    @Override
    public void failed(final RpcException ex) {
      logger.error("Failure while attempting to {} fragment {} on endpoint {} with {}.", signal, value, endpoint, ex);
    }

    @Override
    public void success(final Ack ack, final ByteBuf buf) {
      if (!ack.getOk()) {
        logger.warn("Remote node {} responded negative on {} request for fragment {} with {}.", endpoint, signal, value,
          ack);
      }
    }

    @Override
    public void interrupted(final InterruptedException ex) {
      logger.error("Interrupted while waiting for RPC outcome of action fragment {}. " +
          "Endpoint {}, Fragment handle {}", signal, endpoint, value, ex);
    }
  }

  void updateEphemeralState(final QueryState queryState) {
    // If query is already in zk transient store, ignore the transient state update option.
    // Else, they will not be removed from transient store upon completion.
    if (!inTransientStore && !foreman.getQueryContext().getOptions().getOption(ExecConstants.QUERY_TRANSIENT_STATE_UPDATE)) {
      return;
    }

    switch (queryState) {
      case PREPARING:
      case PLANNING:
      case ENQUEUED:
      case STARTING:
      case RUNNING:
      case CANCELLATION_REQUESTED:
        runningProfileStore.put(stringQueryId, getQueryInfo());  // store as ephemeral query profile.
        inTransientStore = true;
        break;
      case COMPLETED:
      case CANCELED:
      case FAILED:
        try {
          runningProfileStore.remove(stringQueryId);
          inTransientStore = false;
        } catch (final Exception e) {
          logger.warn("Failure while trying to delete the stored profile for the query [{}]", stringQueryId, e);
        }
        break;

      default:
        throw new IllegalStateException("unrecognized queryState " + queryState);
    }
  }

  void writeFinalProfile(UserException ex) {
    try {
      // TODO(DRILL-2362) when do these ever get deleted?
      completedProfileStore.put(stringQueryId, getQueryProfile(ex));
    } catch (Exception e) {
      logger.error("Failure while storing Query Profile", e);
    }
  }

  private QueryInfo getQueryInfo() {
    final String queryText = foreman.getQueryText();
    QueryInfo.Builder queryInfoBuilder = QueryInfo.newBuilder()
        .setState(foreman.getState())
        .setUser(foreman.getQueryContext().getQueryUserName())
        .setForeman(foreman.getQueryContext().getCurrentEndpoint())
        .setStart(startTime)
        .setTotalCost(totalCost)
        .setQueueName(queueName == null ? "-" : queueName)
        .setOptionsJson(getQueryOptionsAsJson());

    if (queryText != null) {
      queryInfoBuilder.setQuery(queryText);
    }

    return queryInfoBuilder.build();
  }

  public QueryProfile getQueryProfile() {
    return getQueryProfile(null);
  }

  private QueryProfile getQueryProfile(UserException ex) {
    final QueryProfile.Builder profileBuilder = QueryProfile.newBuilder()
        .setUser(foreman.getQueryContext().getQueryUserName())
        .setType(runQuery.getType())
        .setId(queryId)
        .setQueryId(QueryIdHelper.getQueryId(queryId))
        .setState(foreman.getState())
        .setForeman(foreman.getQueryContext().getCurrentEndpoint())
        .setStart(startTime)
        .setEnd(endTime)
        .setPlanEnd(planningEndTime)
        .setQueueWaitEnd(queueWaitEndTime)
        .setTotalFragments(fragmentDataSet.size())
        .setFinishedFragments(finishedFragments.get())
        .setTotalCost(totalCost)
        .setQueueName(queueName == null ? "-" : queueName)
        .setOptionsJson(getQueryOptionsAsJson());

    if (ex != null) {
      profileBuilder.setError(ex.getMessage(false));
      profileBuilder.setVerboseError(ex.getVerboseMessage(false));
      profileBuilder.setErrorId(ex.getErrorId());
      if (ex.getErrorLocation() != null) {
        profileBuilder.setErrorNode(ex.getErrorLocation());
      }
    }

    if (planText != null) {
      profileBuilder.setPlan(planText);
    }

    final String queryText = foreman.getQueryText();
    if (queryText != null) {
      profileBuilder.setQuery(queryText);
    }

    int autoLimitRowCount = foreman.getQueryContext().getOptions().getOption(ExecConstants.QUERY_MAX_ROWS).num_val.intValue();
    if (autoLimitRowCount > 0) {
      profileBuilder.setAutoLimit(autoLimitRowCount);
      logger.debug("The query's resultset was limited to {} rows", autoLimitRowCount);
    }

    fragmentDataMap.forEach(new OuterIter(profileBuilder));

    return profileBuilder.build();
  }

  private String getQueryOptionsAsJson() {
    try {
      OptionList optionList = foreman.getQueryContext().getOptions().getOptionList();
      return foreman.getQueryContext().getLpPersistence().getMapper().writeValueAsString(optionList);
    } catch (JsonProcessingException e) {
      throw new DrillRuntimeException("Error while trying to convert option list to json string", e);
    }
  }

  private class OuterIter implements IntObjectPredicate<IntObjectHashMap<FragmentData>> {
    private final QueryProfile.Builder profileBuilder;

    public OuterIter(Builder profileBuilder) {
      this.profileBuilder = profileBuilder;
    }

    @Override
    public boolean apply(final int majorFragmentId, final IntObjectHashMap<FragmentData> minorMap) {
      final MajorFragmentProfile.Builder builder = MajorFragmentProfile.newBuilder().setMajorFragmentId(majorFragmentId);
      minorMap.forEach(new InnerIter(builder));
      profileBuilder.addFragmentProfile(builder);
      return true;
    }
  }

  private class InnerIter implements IntObjectPredicate<FragmentData> {
    private final MajorFragmentProfile.Builder builder;

    public InnerIter(MajorFragmentProfile.Builder fb) {
      this.builder = fb;
    }

    @Override
    public boolean apply(int key, FragmentData data) {
      builder.addMinorFragmentProfile(data.getProfile());
      return true;
    }
  }

  void setPlanText(final String planText) {
    this.planText = planText;
  }

  void markStartTime() {
    startTime = System.currentTimeMillis();
  }

  void markEndTime() {
    endTime = System.currentTimeMillis();
  }

  void markPlanningEndTime() {
    planningEndTime = System.currentTimeMillis();
  }

  void markQueueWaitEndTime() {
    queueWaitEndTime = System.currentTimeMillis();
  }

  public void setTotalCost(double totalCost) {
    this.totalCost = totalCost;
  }

  public void setQueueName(String queueName) {
    this.queueName = queueName;
  }

  /**
   * Internal class used to track the number of pending completion messages required from particular node. This allows
   * to know for each node that is part of this query, what portion of fragments are still outstanding. In the case that
   * there is a node failure, we can then correctly track how many outstanding messages will never arrive.
   */
  private class NodeTracker {
    private final AtomicInteger totalFragments = new AtomicInteger(0);
    private final AtomicInteger completedFragments = new AtomicInteger(0);

    public NodeTracker(final DrillbitEndpoint endpoint) { }

    /**
     * Increments the number of fragment this node is running.
     */
    public void addFragment() {
      totalFragments.incrementAndGet();
    }

    /**
     * Increments the number of fragments completed on this node.  Once the number of fragments completed
     * equals the number of fragments running, this will be marked as a finished node and result in the finishedNodes being incremented.
     *
     * If the number of remaining nodes has been decremented to zero, this will allow the query to move to a completed state.
     */
    public void fragmentComplete() {
      if (totalFragments.get() == completedFragments.incrementAndGet()) {
        nodeComplete();
      }
    }

    /**
     * Increments the number of fragments completed on this node until we mark this node complete. Note that this uses
     * the internal fragmentComplete() method so whether we have failure or success, the nodeComplete event will only
     * occur once. (Two threads could be decrementing the fragment at the same time since this will likely come from an
     * external event).
     *
     * @return true if the node has fragments that are pending (non-terminal state); false if all fragments running on
     * this node have already terminated.
     */
    public boolean nodeDead() {
      if (completedFragments.get() == totalFragments.get()) {
        return false;
      }
      while (completedFragments.get() < totalFragments.get()) {
        fragmentComplete();
      }
      return true;
    }
  }

  /**
   * Increments the number of currently complete nodes and returns the number of completed nodes. If the there are no
   * more pending nodes, moves the query to a terminal state.
   */
  private void nodeComplete() {
    final int finishedNodes = this.finishedNodes.incrementAndGet();
    final int totalNodes = nodeMap.size();
    Preconditions.checkArgument(finishedNodes <= totalNodes, "The finished node count exceeds the total node count");
    final int remaining = totalNodes - finishedNodes;
    if (remaining == 0) {
      // this target state may be adjusted in moveToState() based on current FAILURE/CANCELLATION_REQUESTED status
      foreman.addToEventQueue(QueryState.COMPLETED, null);
    } else {
      logger.debug("Foreman is still waiting for completion message from {} nodes containing {} fragments", remaining,
          this.fragmentDataSet.size() - finishedFragments.get());
    }
  }

  public FragmentStatusListener getFragmentStatusListener(){
    return fragmentStatusListener;
  }

  private final FragmentStatusListener fragmentStatusListener = new FragmentStatusListener() {
    @Override
    public void statusUpdate(final FragmentStatus status) {
      logger.debug("New fragment status was provided to QueryManager of {}", status);
      switch(status.getProfile().getState()) {
      case AWAITING_ALLOCATION:
      case RUNNING:
      case CANCELLATION_REQUESTED:
        updateFragmentStatus(status);
        break;

      case FAILED:
        foreman.addToEventQueue(QueryState.FAILED, new UserRemoteException(status.getProfile().getError()));
        // fall-through.
      case FINISHED:
      case CANCELLED:
        fragmentDone(status);
        break;

      default:
        throw new UnsupportedOperationException(String.format("Received status of %s", status));
      }
    }
  };

  public DrillbitStatusListener getDrillbitStatusListener() {
    return drillbitStatusListener;
  }

  private final DrillbitStatusListener drillbitStatusListener = new DrillbitStatusListener() {

    @Override
    public void drillbitRegistered(final Set<DrillbitEndpoint> registeredDrillbits) {
    }

    @Override
    public void drillbitUnregistered(final Set<DrillbitEndpoint> unregisteredDrillbits) {
      final StringBuilder failedNodeList = new StringBuilder();
      boolean atLeastOneFailure = false;

      for (final DrillbitEndpoint ep : unregisteredDrillbits) {
        final NodeTracker tracker = nodeMap.get(ep);
        if (tracker == null) {
          continue; // fragments were not assigned to this Drillbit
        }

        // mark node as dead.
        if (!tracker.nodeDead()) {
          continue; // fragments assigned to this Drillbit completed
        }

        // fragments were running on the Drillbit, capture node name for exception or logging message
        if (atLeastOneFailure) {
          failedNodeList.append(", ");
        } else {
          atLeastOneFailure = true;
        }
        failedNodeList.append(ep.getAddress());
        failedNodeList.append(":");
        failedNodeList.append(ep.getUserPort());
      }

      if (atLeastOneFailure) {
        logger.warn("Drillbits [{}] no longer registered in cluster.  Canceling query {}",
            failedNodeList, QueryIdHelper.getQueryId(queryId));
        foreman.addToEventQueue(QueryState.FAILED,
            new ForemanException(String.format("One more more nodes lost connectivity during query.  Identified nodes were [%s].",
                failedNodeList)));
      }
    }
  };
}
