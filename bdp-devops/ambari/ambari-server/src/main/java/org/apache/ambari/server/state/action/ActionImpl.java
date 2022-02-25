/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.state.action;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;
import org.apache.ambari.server.state.fsm.SingleArcTransition;
import org.apache.ambari.server.state.fsm.StateMachine;
import org.apache.ambari.server.state.fsm.StateMachineFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActionImpl implements Action {

  private static final Logger LOG = LoggerFactory.getLogger(ActionImpl.class);

  private final Lock readLock;
  private final Lock writeLock;

  private ActionId id;

  private long startTime;
  private long lastUpdateTime;
  private long completionTime;

  // TODO
  // need to add action report

  private static final StateMachineFactory
    <ActionImpl, ActionState, ActionEventType, ActionEvent>
      stateMachineFactory
        = new StateMachineFactory<ActionImpl, ActionState,
          ActionEventType, ActionEvent>
            (ActionState.INIT)

    // define the state machine of a Action

    .addTransition(ActionState.INIT, ActionState.IN_PROGRESS,
        ActionEventType.ACTION_IN_PROGRESS, new ActionProgressUpdateTransition())
    .addTransition(ActionState.INIT, ActionState.COMPLETED,
        ActionEventType.ACTION_COMPLETED, new ActionCompletedTransition())
    .addTransition(ActionState.INIT, ActionState.FAILED,
        ActionEventType.ACTION_FAILED, new ActionFailedTransition())
    .addTransition(ActionState.INIT, ActionState.IN_PROGRESS,
        ActionEventType.ACTION_IN_PROGRESS, new ActionProgressUpdateTransition())
    .addTransition(ActionState.IN_PROGRESS, ActionState.IN_PROGRESS,
        ActionEventType.ACTION_IN_PROGRESS, new ActionProgressUpdateTransition())
    .addTransition(ActionState.IN_PROGRESS, ActionState.COMPLETED,
        ActionEventType.ACTION_COMPLETED, new ActionCompletedTransition())
    .addTransition(ActionState.IN_PROGRESS, ActionState.FAILED,
        ActionEventType.ACTION_FAILED, new ActionFailedTransition())
    .addTransition(ActionState.COMPLETED, ActionState.INIT,
        ActionEventType.ACTION_INIT, new NewActionTransition())
    .addTransition(ActionState.FAILED, ActionState.INIT,
        ActionEventType.ACTION_INIT, new NewActionTransition())
    .installTopology();

  private final StateMachine<ActionState, ActionEventType, ActionEvent>
      stateMachine;

  public ActionImpl(ActionId id, long startTime) {
    super();
    this.id = id;
    this.stateMachine = stateMachineFactory.make(this);
    ReadWriteLock rwLock = new ReentrantReadWriteLock();
    this.readLock = rwLock.readLock();
    this.writeLock = rwLock.writeLock();
    this.startTime = startTime;
    this.lastUpdateTime = -1;
    this.completionTime = -1;
  }

  private void reset() {
    try {
      writeLock.lock();
      this.startTime = -1;
      this.lastUpdateTime = -1;
      this.completionTime = -1;
    }
    finally {
      writeLock.unlock();
    }
  }

  static class NewActionTransition
     implements SingleArcTransition<ActionImpl, ActionEvent> {

    @Override
    public void transition(ActionImpl action, ActionEvent event) {
      ActionInitEvent e = (ActionInitEvent) event;
      // TODO audit logs
      action.reset();
      action.setId(e.getActionId());
      action.setStartTime(e.getStartTime());
      LOG.info("Launching a new Action"
          + ", actionId=" + action.getId()
          + ", startTime=" + action.getStartTime());
    }
  }

  static class ActionProgressUpdateTransition
      implements SingleArcTransition<ActionImpl, ActionEvent> {

    @Override
    public void transition(ActionImpl action, ActionEvent event) {
      ActionProgressUpdateEvent e = (ActionProgressUpdateEvent) event;
      action.setLastUpdateTime(e.getProgressUpdateTime());
      if (LOG.isDebugEnabled()) {
        LOG.debug("Progress update for Action, actionId={}, startTime={}, lastUpdateTime={}",
          action.getId(), action.getStartTime(), action.getLastUpdateTime());
      }
    }
  }

  static class ActionCompletedTransition
     implements SingleArcTransition<ActionImpl, ActionEvent> {

    @Override
    public void transition(ActionImpl action, ActionEvent event) {
      // TODO audit logs
      ActionCompletedEvent e = (ActionCompletedEvent) event;
      action.setCompletionTime(e.getCompletionTime());
      action.setLastUpdateTime(e.getCompletionTime());

      LOG.info("Action completed successfully"
          + ", actionId=" + action.getId()
          + ", startTime=" + action.getStartTime()
          + ", completionTime=" + action.getCompletionTime());
    }
  }

  static class ActionFailedTransition
      implements SingleArcTransition<ActionImpl, ActionEvent> {

    @Override
    public void transition(ActionImpl action, ActionEvent event) {
      // TODO audit logs
      ActionFailedEvent e = (ActionFailedEvent) event;
      action.setCompletionTime(e.getCompletionTime());
      action.setLastUpdateTime(e.getCompletionTime());
      LOG.info("Action failed to complete"
          + ", actionId=" + action.getId()
          + ", startTime=" + action.getStartTime()
          + ", completionTime=" + action.getCompletionTime());
    }
  }


  @Override
  public ActionState getState() {
    try {
      readLock.lock();
      return stateMachine.getCurrentState();
    }
    finally {
      readLock.unlock();
    }
  }

  @Override
  public void setState(ActionState state) {
    try {
      writeLock.lock();
      stateMachine.setCurrentState(state);
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public void handleEvent(ActionEvent event)
      throws InvalidStateTransitionException {
    if (LOG.isDebugEnabled()) {
      LOG.debug("Handling Action event, eventType={}, event={}", event.getType().name(), event);
    }
    ActionState oldState = getState();
    try {
      writeLock.lock();
      try {
        stateMachine.doTransition(event.getType(), event);
      } catch (InvalidStateTransitionException e) {
        LOG.error("Can't handle Action event at current state"
            + ", actionId=" + this.getId()
            + ", currentState=" + oldState
            + ", eventType=" + event.getType()
            + ", event=" + event);
        throw e;
      }
    }
    finally {
      writeLock.unlock();
    }
    if (oldState != getState()) {
      if (LOG.isDebugEnabled()) {
        LOG.debug("Action transitioned to a new state, actionId={}, oldState={}, currentState={}, eventType={}, event={}",
          getId(), oldState, getState(), event.getType().name(), event);
      }
    }
  }

  @Override
  public ActionId getId() {
    try {
      readLock.lock();
      return id;
    }
    finally {
      readLock.unlock();
    }
  }

  private void setId(ActionId id) {
    try {
      writeLock.lock();
      this.id = id;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getStartTime() {
    try {
      readLock.lock();
      return startTime;
    }
    finally {
      readLock.unlock();
    }
  }

  public void setStartTime(long startTime) {
    try {
      writeLock.lock();
      this.startTime = startTime;
    }
    finally {
      writeLock.unlock();
    }
  }

  @Override
  public long getLastUpdateTime() {
    try {
      readLock.lock();
      return lastUpdateTime;
    }
    finally {
      readLock.unlock();
    }
  }

  public void setLastUpdateTime(long lastUpdateTime) {
    try {
      writeLock.lock();
      this.lastUpdateTime = lastUpdateTime;
    }
    finally {
      writeLock.unlock();
    }

  }

  @Override
  public long getCompletionTime() {
    try {
      readLock.lock();
      return completionTime;
    }
    finally {
      readLock.unlock();
    }
  }

  public void setCompletionTime(long completionTime) {
    try {
      writeLock.lock();
      this.completionTime = completionTime;
    }
    finally {
      writeLock.unlock();
    }
  }


}
