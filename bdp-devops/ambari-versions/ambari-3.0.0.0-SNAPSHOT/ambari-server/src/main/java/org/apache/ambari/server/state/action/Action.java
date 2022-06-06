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

import org.apache.ambari.server.state.fsm.InvalidStateTransitionException;

public interface Action {

  /**
   * Get the Action ID for the action
   * @return ActionId
   */
  ActionId getId();

  // TODO requires some form of ActionType to ensure only one running
  // action per action type
  // There may be gotchas such as de-commissioning should be allowed to happen
  // on more than one host at a time


  /**
   * Get Start Time of the action
   * @return Start time as a unix timestamp
   */
  long getStartTime();

  /**
   * Get the last update time of the Action when its progress status
   * was updated
   * @return Last Update Time as a unix timestamp
   */
  long getLastUpdateTime();

  /**
   * Time when the Action completed
   * @return Completion Time as a unix timestamp
   */
  long getCompletionTime();

  /**
   * Get the current state of the Action
   * @return ActionState
   */
  ActionState getState();

  /**
   * Set the State of the Action
   * @param state ActionState
   */
  void setState(ActionState state);

  /**
   * Send a ActionEvent to the Action's StateMachine
   * @param event ActionEvent
   * @throws InvalidStateTransitionException
   */
  void handleEvent(ActionEvent event)
      throws InvalidStateTransitionException;
}
