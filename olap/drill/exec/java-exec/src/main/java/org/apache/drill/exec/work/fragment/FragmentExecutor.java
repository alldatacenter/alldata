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
package org.apache.drill.exec.work.fragment;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.drill.common.DeferredException;
import org.apache.drill.common.EventProcessor;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.coord.ClusterCoordinator;
import org.apache.drill.exec.exception.OutOfMemoryException;
import org.apache.drill.exec.ops.ExecutorFragmentContext;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.QueryCancelledException;
import org.apache.drill.exec.physical.base.FragmentRoot;
import org.apache.drill.exec.physical.impl.ImplCreator;
import org.apache.drill.exec.physical.impl.RootExec;
import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.BitControl.PlanFragment;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared.FragmentState;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.server.FailureUtils;
import org.apache.drill.exec.testing.ControlsInjector;
import org.apache.drill.exec.testing.ControlsInjectorFactory;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.exec.work.foreman.DrillbitStatusListener;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.drill.exec.server.FailureUtils.EXIT_CODE_HEAP_OOM;

/**
 * Runs a single fragment on a single Drillbit.
 * Listens/responds to status request and cancellation messages.
 * </p>
 * <h2>Theory of Operation</h2>
 * <p>
 * The {@link FragmentExecutor} runs a fragment's {@link RootExec} in the
 * {@link FragmentExecutor#run()} method in a single thread. While a fragment is
 * running it may be subject to termination requests. The
 * {@link FragmentExecutor} is responsible for gracefully handling termination
 * requests for the {@link RootExec}. There are two types of termination
 * messages:
 * <ol>
 * <li><b>Cancellation Request:</b> This signals that the fragment and therefore
 * the {@link RootExec} need to terminate immediately.</li>
 * <li><b>Receiver Finished:</b> This signals that a downstream receiver no
 * longer needs anymore data. A fragment may receive multiple receiver finished
 * requests (one for each downstream receiver). The {@link RootExec} will only
 * terminate once it has received
 * {@link FragmentExecutor.EventType#RECEIVER_FINISHED} messages for all
 * downstream receivers.</li>
 * </ol>
 * </p>
 * <p>
 * The {@link FragmentExecutor} processes termination requests appropriately for
 * the {@link RootExec}. A <b>Cancellation Request</b> is signaled when
 * {@link FragmentExecutor#cancel()} is called. A <b>Receiver Finished</b> event
 * is signaled when
 * {@link FragmentExecutor#receivingFragmentFinished(FragmentHandle)} is called.
 * The way in which these signals are handled is the following:
 * </p>
 * <h3>Cancellation Request</h3>
 * <p>
 * There are two ways in which a cancellation request can be handled when
 * {@link FragmentExecutor#cancel()} is called.
 * <ol>
 * <li>The Cancellation Request is received before the {@link RootExec} for the
 * fragment is even started. In this case we can cleanup resources allocated for
 * the fragment and never start a {@link RootExec}</li>
 * <li>The Cancellation Request is receive after the {@link RootExec} for the
 * fragment is started. In this the cancellation request is sent to the
 * {@link FragmentEventProcessor}. If this is not the first cancellation request
 * it is ignored. If this is the first cancellation request the {@link RootExec}
 * for this fragment is terminated by interrupting it. Then the
 * {@link FragmentExecutor#run()} thread proceeds to cleanup resources
 * normally</li>
 * </ol>
 * </p>
 * <h3>Receiver Finished</h3>
 * <p>
 * When {@link FragmentExecutor#receivingFragmentFinished(FragmentHandle)} is
 * called, the message is passed to the {@link FragmentEventProcessor} if we did
 * not already receive a Cancellation request. Then the finished message is
 * queued in {@link FragmentExecutor#receiverFinishedQueue}. The
 * {@link FragmentExecutor#run()} polls
 * {@link FragmentExecutor#receiverFinishedQueue} and signals the
 * {@link RootExec} with
 * {@link RootExec#receivingFragmentFinished(FragmentHandle)} appropriately.
 * </p>
 * <h2>Possible Design Flaws / Poorly Defined Behavior</h2>
 * <p>
 * There are still a few aspects of the {@link FragmentExecutor} design that are
 * not clear.
 * <ol>
 * <li>If we get a <b>Receiver Finished</b> message for one downstream receiver,
 * will we eventually get one from every downstream receiver?</li>
 * <li>What happens when we process a <b>Receiver Finished</b> message for some
 * (but not all) downstream receivers and then we cancel the fragment?</li>
 * <li>What happens when we process a <b>Receiver Finished</b> message for some
 * (but not all) downstream receivers and then we run out of data from the
 * upstream?</li>
 * </ol>
 * </p>
 */
public class FragmentExecutor implements Runnable {
  private static final Logger logger = LoggerFactory.getLogger(FragmentExecutor.class);
  private static final ControlsInjector injector = ControlsInjectorFactory.getInjector(FragmentExecutor.class);

  private final String fragmentName;
  private final ExecutorFragmentContext fragmentContext;
  private final FragmentStatusReporter statusReporter;
  private final DeferredException deferredException = new DeferredException();
  private final PlanFragment fragment;
  private final FragmentRoot rootOperator;

  private volatile RootExec root;
  private final AtomicReference<FragmentState> fragmentState = new AtomicReference<>(FragmentState.AWAITING_ALLOCATION);

  /**
   * Holds all of the messages sent by downstream receivers that have finished. The {@link FragmentExecutor#run()} thread reads from this queue and passes the
   * finished messages to the fragment's {@link RootExec} via the {@link RootExec#receivingFragmentFinished(FragmentHandle)} method.
   */
  private final Queue<FragmentHandle> receiverFinishedQueue = new ConcurrentLinkedQueue<>();
  private final FragmentEventProcessor eventProcessor = new FragmentEventProcessor();

  // Thread that is currently executing the Fragment. Value is null if the fragment hasn't started running or finished
  private final AtomicReference<Thread> myThreadRef = new AtomicReference<>(null);

  /**
   * Create a FragmentExecutor where we need to parse and materialize the root operator.
   *
   * @param context
   * @param fragment
   * @param statusReporter
   */
  public FragmentExecutor(final ExecutorFragmentContext context, final PlanFragment fragment,
                          final FragmentStatusReporter statusReporter) {
    this(context, fragment, statusReporter, null);
  }

  /**
   * Create a FragmentExecutor where we already have a root operator in memory.
   *
   * @param context
   * @param fragment
   * @param statusReporter
   * @param rootOperator
   */
  public FragmentExecutor(final ExecutorFragmentContext context, final PlanFragment fragment,
                          final FragmentStatusReporter statusReporter, final FragmentRoot rootOperator) {
    this.fragmentContext = context;
    this.statusReporter = statusReporter;
    this.fragment = fragment;
    this.rootOperator = rootOperator;
    this.fragmentName = QueryIdHelper.getQueryIdentifier(context.getHandle());

    context.setExecutorState(new ExecutorStateImpl());
  }

  @Override
  public String toString() {
    final StringBuilder builder = new StringBuilder();
    builder.append("FragmentExecutor [fragmentContext=");
    builder.append(fragmentContext);
    builder.append(", fragmentState=");
    builder.append(fragmentState);
    builder.append("]");
    return builder.toString();
  }

  /**
   * Returns the current fragment status if the fragment is running. Otherwise, returns no status.
   *
   * @return FragmentStatus or null.
   */
  public FragmentStatus getStatus() {
    /*
     * If the query is not in a running state, the operator tree is still being constructed and
     * there is no reason to poll for intermediate results.
     *
     * Previously the call to get the operator stats with the AbstractStatusReporter was happening
     * before this check. This caused a concurrent modification exception as the list of operator
     * stats is iterated over while collecting info, and added to while building the operator tree.
     */
    if (fragmentState.get() == FragmentState.RUNNING) {
      return statusReporter.getStatus(FragmentState.RUNNING);
    } else {
      return null;
    }
  }

  /**
   * <p>
   * Cancel the execution of this fragment is in an appropriate state. Messages come from external.
   * </p>
   * <p>
   * <b>Note:</b> This will be called from threads <b>Other</b> than the one running this runnable(),
   * so we need to be careful about the state transitions that can result.
   * </p>
   */
  public void cancel() {
    final boolean thisIsOnlyThread = myThreadRef.compareAndSet(null, Thread.currentThread());

    if (thisIsOnlyThread) {
      eventProcessor.cancelAndFinish();
      eventProcessor.start(); // start immediately as we are the first thread accessing this fragment
    } else {
      eventProcessor.cancel();
    }
  }

  private void cleanup(FragmentState state) {
    closeOutResources();

    updateState(state);
    // send the final state of the fragment. only the main execution thread can send the final state and it can
    // only be sent once.
    sendFinalState();
  }

  /**
   * Resume all the pauses within the current context. Note that this method
   * will be called from threads *other* than the one running this runnable().
   * Also, this method can be called multiple times.
   */
  public synchronized void unpause() {
    fragmentContext.getExecutionControls().unpauseAll();
  }

  /**
   * Inform this fragment that one of its downstream partners no longer needs
   * additional records. This is most commonly called in the case that a limit
   * query is executed.
   *
   * @param handle
   *          The downstream FragmentHandle of the Fragment that needs no more
   *          records from this Fragment.
   */
  public void receivingFragmentFinished(final FragmentHandle handle) {
    eventProcessor.receiverFinished(handle);
  }

  @Override
  public void run() {
    final Thread myThread = Thread.currentThread();

    if (!myThreadRef.compareAndSet(null, myThread)) {
      return;
    }

    final String originalThreadName = myThread.getName();
    final FragmentHandle fragmentHandle = fragmentContext.getHandle();
    final ClusterCoordinator clusterCoordinator = fragmentContext.getClusterCoordinator();
    final DrillbitStatusListener drillbitStatusListener = new FragmentDrillbitStatusListener();
    final String newThreadName = QueryIdHelper.getExecutorThreadName(fragmentHandle);

    try {
      myThread.setName(newThreadName);

      // if we didn't get the root operator when the executor was created, create it now.
      final FragmentRoot rootOperator = this.rootOperator != null ? this.rootOperator :
          fragmentContext.getPlanReader().readFragmentRoot(fragment.getFragmentJson());

      root = ImplCreator.getExec(fragmentContext, rootOperator);
      if (root == null) {
        return;
      }

      clusterCoordinator.addDrillbitStatusListener(drillbitStatusListener);
      updateState(FragmentState.RUNNING);

      eventProcessor.start();
      injector.injectPause(fragmentContext.getExecutionControls(), "fragment-running", logger);

      final DrillbitEndpoint endpoint = fragmentContext.getEndpoint();
      logger.debug("Starting fragment {}:{} on {}:{}",
          fragmentHandle.getMajorFragmentId(), fragmentHandle.getMinorFragmentId(),
          endpoint.getAddress(), endpoint.getUserPort());

      final UserGroupInformation queryUserUgi = fragmentContext.isImpersonationEnabled() ?
          ImpersonationUtil.createProxyUgi(fragmentContext.getQueryUserName()) :
          ImpersonationUtil.getProcessUserUGI();

      queryUserUgi.doAs((PrivilegedExceptionAction<Void>) () -> {
        injector.injectChecked(fragmentContext.getExecutionControls(), "fragment-execution", IOException.class);

        while (shouldContinue()) {
          // Fragment is not cancelled

          for (FragmentHandle fragmentHandle1; (fragmentHandle1 = receiverFinishedQueue.poll()) != null;) {
            // See if we have any finished requests. If so execute them.
            root.receivingFragmentFinished(fragmentHandle1);
          }

          if (!root.next()) {
            // Fragment has processed all of its data
            break;
          }
        }

        return null;
      });

    } catch (QueryCancelledException e) {
      // Ignore: indicates query cancelled by this executor
    } catch (OutOfMemoryError | OutOfMemoryException e) {
      if (FailureUtils.isDirectMemoryOOM(e)) {
        root.dumpBatches(e);
        fail(UserException.memoryError(e).build(logger));
      } else {
        // we have a heap out of memory error. The JVM is unstable, exit.
        FailureUtils.unrecoverableFailure(e, "Unable to handle out of memory condition in FragmentExecutor.", EXIT_CODE_HEAP_OOM);
      }
    } catch (InterruptedException e) {
      // Swallow interrupted exceptions since we intentionally interrupt the root when cancelling a query
      logger.trace("Interrupted root: {}", root, e);
    } catch (Throwable t) {
      if (root != null) {
        root.dumpBatches(t);
      }
      fail(t);
    } finally {

      // Don't process any more termination requests, we are done.
      eventProcessor.terminate();
      // Clear the interrupt flag if it is set.
      Thread.interrupted();

      // here we could be in FAILED, RUNNING, or CANCELLATION_REQUESTED
      // FAILED state will be because of any Exception in execution loop root.next()
      // CANCELLATION_REQUESTED because of a CANCEL request received by Foreman.
      // ELSE will be in FINISHED state.
      cleanup(FragmentState.FINISHED);

      clusterCoordinator.removeDrillbitStatusListener(drillbitStatusListener);

      myThread.setName(originalThreadName);
    }
  }

  /**
   * Utility method to check where we are in a no terminal state.
   *
   * @return Whether or not execution should continue.
   */
  private boolean shouldContinue() {
    return !isCompleted() && FragmentState.CANCELLATION_REQUESTED != fragmentState.get();
  }

  /**
   * Returns true if the fragment is in a terminal state
   *
   * @return Whether this state is in a terminal state.
   */
  private boolean isCompleted() {
    return isTerminal(fragmentState.get());
  }

  private void sendFinalState() {
    final FragmentState outcome = fragmentState.get();
    if (outcome == FragmentState.FAILED) {
      final FragmentHandle handle = getContext().getHandle();
      final UserException uex = UserException.systemError(deferredException.getAndClear())
          .addIdentity(getContext().getEndpoint())
          .addContext("Fragment", handle.getMajorFragmentId() + ":" + handle.getMinorFragmentId())
          .build(logger);
      statusReporter.fail(uex);
    } else {
      statusReporter.stateChanged(outcome);
    }
    statusReporter.close();
  }

  private void closeOutResources() {

    // first close the operators and release all memory.
    try {
      // Say executor was cancelled before setup. Now when executor actually runs, root is not initialized, but this
      // method is called in finally. So root can be null.
      if (root != null) {
        root.close();
      }
    } catch (final Exception e) {
      fail(e);
    }

    // then close the fragment context.
    fragmentContext.close();
  }

  private void warnStateChange(final FragmentState current, final FragmentState target) {
    logger.warn(fragmentName + ": Ignoring unexpected state transition {} --> {}", current.name(), target.name());
  }

  private void errorStateChange(final FragmentState current, final FragmentState target) {
    final String msg = "%s: Invalid state transition %s --> %s";
    throw new StateTransitionException(String.format(msg, fragmentName, current.name(), target.name()));
  }

  private synchronized boolean updateState(FragmentState target) {
    final FragmentState current = fragmentState.get();
    logger.info(fragmentName + ": State change requested {} --> {}", current, target);
    switch (target) {
    case CANCELLATION_REQUESTED:
      switch (current) {
      case SENDING:
      case AWAITING_ALLOCATION:
      case RUNNING:
        fragmentState.set(target);
        statusReporter.stateChanged(target);
        return true;

      default:
        warnStateChange(current, target);
        return false;
      }

    case FINISHED:
      if(current == FragmentState.CANCELLATION_REQUESTED){
        target = FragmentState.CANCELLED;
      } else if (current == FragmentState.FAILED) {
        target = FragmentState.FAILED;
      }
      // fall-through
    case FAILED:
      if(!isTerminal(current)){
        fragmentState.set(target);
        // don't notify reporter until we finalize this terminal state.
        return true;
      } else if (current == FragmentState.FAILED) {
        // no warn since we can call fail multiple times.
        return false;
      } else if (current == FragmentState.CANCELLED && target == FragmentState.FAILED) {
        fragmentState.set(FragmentState.FAILED);
        return true;
      }else{
        warnStateChange(current, target);
        return false;
      }

    case RUNNING:
      if(current == FragmentState.AWAITING_ALLOCATION){
        fragmentState.set(target);
        statusReporter.stateChanged(target);
        return true;
      }else{
        errorStateChange(current, target);
      }

      // these should never be requested.
    case CANCELLED:
    case SENDING:
    case AWAITING_ALLOCATION:
    default:
      errorStateChange(current, target);
    }

    // errorStateChange() throw should mean this is never executed
    throw new IllegalStateException();
  }

  private boolean isTerminal(final FragmentState state) {
    return state == FragmentState.CANCELLED
        || state == FragmentState.FAILED
        || state == FragmentState.FINISHED;
  }

  /**
   * Capture an exception and add store it. Update state to failed status (if not already there). Does not immediately
   * report status back to Foreman. Only the original thread can return status to the Foreman.
   *
   * @param excep
   *          The failure that occurred.
   */
  private void fail(final Throwable excep) {
    deferredException.addThrowable(excep);
    updateState(FragmentState.FAILED);
  }

  public ExecutorFragmentContext getContext() {
    return fragmentContext;
  }

  private class ExecutorStateImpl implements FragmentContext.ExecutorState {
    @Override
    public boolean shouldContinue() {
      return FragmentExecutor.this.shouldContinue();
    }

    @Override
    public void fail(final Throwable t) {
      FragmentExecutor.this.fail(t);
    }

    @Override
    public boolean isFailed() {
      return fragmentState.get() == FragmentState.FAILED;
    }
    @Override
    public Throwable getFailureCause(){
      return deferredException.getException();
    }

    @Override
    public void checkContinue() {
      if (!shouldContinue()) {
        throw new QueryCancelledException();
      }
    }
  }

  private class FragmentDrillbitStatusListener implements DrillbitStatusListener {
    @Override
    public void drillbitRegistered(final Set<CoordinationProtos.DrillbitEndpoint> registeredDrillbits) {
    }

    @Override
    public void drillbitUnregistered(final Set<CoordinationProtos.DrillbitEndpoint> unregisteredDrillbits) {
      // if the defunct Drillbit was running our Foreman, then cancel the query
      final DrillbitEndpoint foremanEndpoint = FragmentExecutor.this.fragmentContext.getForemanEndpoint();
      if (unregisteredDrillbits.contains(foremanEndpoint)) {
        logger.warn("Foreman {} no longer active.  Cancelling fragment {}.",
                    foremanEndpoint.getAddress(),
                    QueryIdHelper.getQueryIdentifier(fragmentContext.getHandle()));
        statusReporter.close();
        FragmentExecutor.this.cancel();
      }
    }
  }

  private enum EventType {
    CANCEL,
    CANCEL_AND_FINISH,
    RECEIVER_FINISHED
  }

  private class FragmentEvent {
    private final EventType type;
    private final FragmentHandle handle;

    FragmentEvent(EventType type, FragmentHandle handle) {
      this.type = type;
      this.handle = handle;
    }
  }

  /**
   * Implementation of EventProcessor to handle fragment cancellation and early terminations
   * without relying on a latch, thus avoiding to block the rpc control thread.<br>
   * This is especially important as fragments can take longer to start
   */
  private class FragmentEventProcessor extends EventProcessor<FragmentEvent> {
    private final AtomicBoolean terminate = new AtomicBoolean(false);

    void cancel() {
      sendEvent(new FragmentEvent(EventType.CANCEL, null));
    }

    void cancelAndFinish() {
      sendEvent(new FragmentEvent(EventType.CANCEL_AND_FINISH, null));
    }

    void receiverFinished(FragmentHandle handle) {
      sendEvent(new FragmentEvent(EventType.RECEIVER_FINISHED, handle));
    }

    /**
     * Tell the {@link FragmentEventProcessor} not to process anymore events. This keeps stray cancellation requests
     * from being processed after the root has finished running and interrupts in the root thread have been cleared.
     */
    public void terminate() {
      terminate.set(true);
    }

    @Override
    protected void processEvent(FragmentEvent event) {
      if (event.type.equals(EventType.RECEIVER_FINISHED)) {
        // Finish request
        if (terminate.get()) {
          // We have already recieved a cancellation or we have terminated the event processor. Do not process anymore finish requests.
          return;
        }
      } else {
        // Cancel request
        if (!terminate.compareAndSet(false, true)) {
          // We have already received a cancellation or we have terminated the event processor. Do not process anymore cancellation requests.
          // This prevents the root thread from being interrupted at an inappropriate time.
          return;
        }
      }

      switch (event.type) {
        case CANCEL:
          // We set the cancel requested flag but the actual cancellation is managed by the run() loop, if called.
          updateState(FragmentState.CANCELLATION_REQUESTED);
          // The root was started so we have to interrupt it in case it is performing a blocking operation.
          killThread();
          break;
        case CANCEL_AND_FINISH:
          // In this case the root was never started so we do not have to interrupt the thread.
          updateState(FragmentState.CANCELLATION_REQUESTED);
          // The FragmentExecutor#run() loop will not execute in this case so we have to cleanup resources here
          cleanup(FragmentState.FINISHED);
          break;
        case RECEIVER_FINISHED:
          assert event.handle != null : "RECEIVER_FINISHED event must have a handle";
          if (root != null) {
            logger.info("Applying request for early sender termination for {} -> {}.",
              QueryIdHelper.getQueryIdentifier(getContext().getHandle()),
              QueryIdHelper.getFragmentId(event.handle));

            receiverFinishedQueue.add(event.handle);
          } else {
            logger.warn("Dropping request for early fragment termination for path {} -> {} as no root exec exists.",
              QueryIdHelper.getFragmentId(getContext().getHandle()), QueryIdHelper.getFragmentId(event.handle));
          }
          // Note we do not terminate the event processor in this case since we can recieve multiple RECEIVER_FINISHED
          // events. One for each downstream receiver.
          break;
      }
    }

    /*
     * Interrupt the thread so that it exits from any blocking operation it could be executing currently. We
     * synchronize here to ensure we don't accidentally create a race condition where we interrupt the close out
     * procedure of the main thread.
    */
    private void killThread() {
      // myThreadRef must contain a non-null reference at this point
      final Thread myThread = myThreadRef.get();
      logger.debug("Interrupting fragment thread {}", myThread.getName());
      myThread.interrupt();
    }
  }
}
