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

import org.apache.drill.common.EventProcessor;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.proto.UserBitShared.QueryResult.QueryState;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.work.foreman.Foreman.ForemanResult;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Is responsible for query transition from one state to another,
 * incrementing / decrementing query statuses counters.
 */
public class QueryStateProcessor implements AutoCloseable {

  private static final Logger logger = LoggerFactory.getLogger(QueryStateProcessor.class);

  private final StateSwitch stateSwitch = new StateSwitch();

  private final String queryIdString;
  private final QueryManager queryManager;
  private final DrillbitContext drillbitContext;
  private final ForemanResult foremanResult;

  private volatile QueryState state;

  public QueryStateProcessor(String queryIdString, QueryManager queryManager, DrillbitContext drillbitContext, ForemanResult foremanResult) {
    this.queryIdString = queryIdString;
    this.queryManager = queryManager;
    this.drillbitContext = drillbitContext;
    this.foremanResult = foremanResult;
    // initial query state is PREPARING
    this.state = QueryState.PREPARING;
  }

  /**
   * @return current query state
   */
  public QueryState getState() {
    return state;
  }

  /**
   * Moves one query state to another, will fail when requested query state transition is not allowed.
   *
   * @param newState new query state
   * @param exception exception if failure occurred
   */
  public synchronized void moveToState(QueryState newState, Exception exception) {
    logger.debug(queryIdString + ": State change requested {} --> {}", state, newState);

    switch (state) {
      case PREPARING:
        preparing(newState, exception);
        return;
      case PLANNING:
        planning(newState, exception);
        return;
      case ENQUEUED:
        enqueued(newState, exception);
        return;
      case STARTING:
        starting(newState, exception);
        return;
      case RUNNING:
        running(newState, exception);
        return;
      case CANCELLATION_REQUESTED:
        cancellationRequested(newState, exception);
        return;
      case CANCELED:
      case COMPLETED:
      case FAILED:
        logger.warn("Dropping request to move to {} state as query is already at {} state (which is terminal).", newState, state);
        return;
    }

    throw new IllegalStateException(String.format("Failure trying to change states: %s --> %s", state.name(), newState.name()));
  }

  /**
   * Directly moves query from one state to another and updates ephemeral query store.
   *
   * @param newState new query state
   */
  public void recordNewState(final QueryState newState) {
    state = newState;
    queryManager.updateEphemeralState(newState);
  }

  /**
   * Transition query to {@link QueryState#CANCELLATION_REQUESTED CANCELLATION_REQUESTED} state if it is
   * not already in the terminal states {@link QueryState#CANCELED, CANCELED}, {@link QueryState#COMPLETED, COMPLETED} or
   * {@link QueryState#FAILED, FAILED}. See the implementation of {@link #moveToState(QueryState, Exception)} for details.
   *
   * Note this can be called from outside of run() on another thread, or after run() completes
   */
  public void cancel() {
    switch (state) {
      case PREPARING:
      case PLANNING:
      case ENQUEUED:
      case STARTING:
      case RUNNING:
        moveToState(QueryState.CANCELLATION_REQUESTED, null);
        break;

      case CANCELLATION_REQUESTED:
      case CANCELED:
      case COMPLETED:
      case FAILED:
        // nothing to do
        break;

      default:
        throw new IllegalStateException("Unable to cancel the query. Unexpected query state -> " + state);
    }
  }

  /**
   * Tells the foreman to move to a new state.<br>
   * This will be added to the end of the event queue and will be processed once the foreman is ready
   * to accept external events.
   *
   * @param newState the state to move to
   * @param exception if not null, the exception that drove this state transition (usually a failure)
   */
  public void addToEventQueue(final QueryState newState, final Exception exception) {
    stateSwitch.addEvent(newState, exception);
  }

  /**
   * Starts processing all events that were enqueued while all fragments were sending out.
   */
  public void startProcessingEvents() {
    try {
      stateSwitch.start();
    } catch (Exception ex) {
      moveToState(QueryState.FAILED, ex);
    }
  }

  /**
   * On close set proper increment / decrement counters depending on final query state.
   */
  @Override
  public void close() {
    queryManager.markEndTime();

    switch (state) {
      case FAILED:
        drillbitContext.getCounters().getFailedQueries().inc();
        break;
      case CANCELED:
        drillbitContext.getCounters().getCanceledQueries().inc();
        break;
      case COMPLETED:
        drillbitContext.getCounters().getSucceededQueries().inc();
        break;
    }

    drillbitContext.getCounters().getRunningQueries().dec();
    drillbitContext.getCounters().getCompletedQueries().inc();
  }


  private void preparing(final QueryState newState, final Exception exception) {
    switch (newState) {
      case PLANNING:
        queryManager.markStartTime();
        drillbitContext.getCounters().getRunningQueries().inc();

        recordNewState(newState);
        drillbitContext.getCounters().getPlanningQueries().inc();
        return;
      case CANCELLATION_REQUESTED:
        wrapUpCancellation();
        return;
    }
    checkCommonStates(newState, exception);
  }

  private void planning(final QueryState newState, final Exception exception) {
    drillbitContext.getCounters().getPlanningQueries().dec();
    queryManager.markPlanningEndTime();
    switch (newState) {
      case ENQUEUED:
        recordNewState(newState);
        drillbitContext.getCounters().getEnqueuedQueries().inc();
        return;
      case CANCELLATION_REQUESTED:
        wrapUpCancellation();
        return;
    default:
      break;
    }
    checkCommonStates(newState, exception);
  }

  private void enqueued(final QueryState newState, final Exception exception) {
    drillbitContext.getCounters().getEnqueuedQueries().dec();
    queryManager.markQueueWaitEndTime();
    switch (newState) {
      case STARTING:
        recordNewState(newState);
        return;
      case CANCELLATION_REQUESTED:
        wrapUpCancellation();
        return;
    default:
      break;
    }
    checkCommonStates(newState, exception);
  }

  private void starting(final QueryState newState, final Exception exception) {
    switch (newState) {
      case RUNNING:
        recordNewState(QueryState.RUNNING);
        return;
      case COMPLETED:
        wrapUpCompletion();
        return;
      case CANCELLATION_REQUESTED:
        // since during starting state fragments are sent to the remote nodes,
        // we don't want to cancel until they all are sent out
        assert exception == null;
        wrapUpCancellation();
        return;
    }

    checkCommonStates(newState, exception);
  }

  private void running(final QueryState newState, final Exception exception) {
      /*
       * For cases that cancel executing fragments, we have to record the new
       * state first, because the cancellation of the local root fragment will
       * cause this to be called recursively.
       */
    switch (newState) {
      case CANCELLATION_REQUESTED:
        assert exception == null;
        wrapUpCancellation();
        return;
      case COMPLETED:
        wrapUpCompletion();
        return;
    }
    checkCommonStates(newState, exception);
  }

  private void cancellationRequested(final QueryState newState, final Exception exception) {
    switch (newState) {
      case FAILED:
        if (drillbitContext.getConfig().getBoolean(ExecConstants.RETURN_ERROR_FOR_FAILURE_IN_CANCELLED_FRAGMENTS)) {
          assert exception != null;
          recordNewState(QueryState.FAILED);
          foremanResult.setForceFailure(exception);
        }

        // proceed

      case CANCELED:
      case COMPLETED:
        /*
         * These amount to a completion of the cancellation requests' cleanup;
         * now we can clean up and send the result.
         */
        foremanResult.close();
        return;
    }

    throw new IllegalStateException(String.format("Failure trying to change states: %s --> %s", state.name(), newState.name()));
  }

  private void wrapUpCancellation() {
    recordNewState(QueryState.CANCELLATION_REQUESTED);
    queryManager.cancelExecutingFragments(drillbitContext);
    foremanResult.setCompleted(QueryState.CANCELED);
    /*
     * We don't close the foremanResult until we've gotten
     * acknowledgments, which happens below in the case for current state
     * == CANCELLATION_REQUESTED.
     */
  }

  private void wrapUpCompletion() {
    recordNewState(QueryState.COMPLETED);
    foremanResult.setCompleted(QueryState.COMPLETED);
    foremanResult.close();
  }

  private void checkCommonStates(final QueryState newState, final Exception exception) {
    switch (newState) {
      case FAILED:
        assert exception != null;
        recordNewState(QueryState.FAILED);
        queryManager.cancelExecutingFragments(drillbitContext);
        foremanResult.setFailed(exception);
        foremanResult.close();
        return;
    default:
      break;
    }

    throw new IllegalStateException(String.format("Failure trying to change states: %s --> %s", state.name(), newState.name()));
  }

  private class StateEvent {
    final QueryState newState;
    final Exception exception;

    StateEvent(final QueryState newState, final Exception exception) {
      this.newState = newState;
      this.exception = exception;
    }
  }

  private class StateSwitch extends EventProcessor<StateEvent> {
    public void addEvent(final QueryState newState, final Exception exception) {
      sendEvent(new StateEvent(newState, exception));
    }

    @Override
    protected void processEvent(final StateEvent event) {
      moveToState(event.newState, event.exception);
    }
  }

}
