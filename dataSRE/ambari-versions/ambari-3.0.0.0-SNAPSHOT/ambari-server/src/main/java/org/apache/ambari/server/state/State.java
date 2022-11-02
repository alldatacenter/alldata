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

package org.apache.ambari.server.state;

import org.apache.ambari.server.AmbariException;

public enum State {
  /**
   * Initial/Clean state.
   */
  INIT,
  /**
   * In the process of installing.
   */
  INSTALLING,
  /**
   * Install failed.
   */
  INSTALL_FAILED,
  /**
   * State when install completed successfully.
   */
  INSTALLED,
  /**
   * In the process of starting.
   */
  STARTING,
  /**
   * State when start completed successfully.
   */
  STARTED,
  /**
   * In the process of stopping.
   */
  STOPPING,
  /**
   * In the process of uninstalling.
   */
  UNINSTALLING,
  /**
   * State when uninstall completed successfully.
   */
  UNINSTALLED,
  /**
   * In the process of wiping out the install.
   */
  WIPING_OUT,
  /**
   * In the process of upgrading the host component deployed bits.
   * Valid only for host component state
   */
  UPGRADING,
  /**
   * Disabled master's backup state
   */
  DISABLED,
  /**
   * State could not be determined.
   */
  UNKNOWN;

  /**
   * Indicates whether or not it is a valid desired state.
   *
   * @return true if this is a valid desired state.
   */
  public boolean isValidDesiredState() {
    switch (this) {
      case INIT:
      case INSTALLED:
      case STARTED:
      case UNINSTALLED:
      case DISABLED:
        return true;
      default:
        return false;
    }
  }

  /**
   * Indicates whether or not it is a valid state for the client component.
   *
   * @return true if this is a valid state for a client component.
   */
  public boolean isValidClientComponentState() {
    switch (this) {
      case STARTING:
      case STARTED:
      case STOPPING:
        return false;
      default:
        return true;
    }
  }

  /**
   * Indicates whether or not the resource with this state can be removed.
   *
   * @return true if this is a removable state
   */
  public boolean isRemovableState() {
    switch (this) {
      case INIT:
      case INSTALLING:
      case INSTALLED:
      case INSTALL_FAILED:
      case UNINSTALLED:
      case UNKNOWN:
      case DISABLED:
        return true;
      default:
        return false;
    }
  }

  /**
   * Utility method to determine whether or not a valid transition can be made from the given states.
   *
   * @param startState    the starting state
   * @param desiredState  the desired state
   *
   * @return true iff a valid transition can be made from the starting state to the desired state
   */
  public static boolean isValidStateTransition(State startState, State desiredState) {
    switch(desiredState) {
      case INSTALLED:
        if (startState == State.INIT
            || startState == State.UNINSTALLED
            || startState == State.INSTALLED
            || startState == State.INSTALLING
            || startState == State.STARTED
            || startState == State.INSTALL_FAILED
            || startState == State.UPGRADING
            || startState == State.STOPPING
            || startState == State.UNKNOWN
            || startState == State.DISABLED) {
          return true;
        }
        break;
      case STARTED:
        if (startState == State.INSTALLED
            || startState == State.STARTING
            || startState == State.STARTED) {
          return true;
        }
        break;
      case UNINSTALLED:
        if (startState == State.INSTALLED
            || startState == State.UNINSTALLED
            || startState == State.UNINSTALLING) {
          return true;
        }
        break;
      case INIT:
        if (startState == State.UNINSTALLED
            || startState == State.INIT
            || startState == State.WIPING_OUT) {
          return true;
        }
        break;
      case DISABLED:
        if (startState == State.INSTALLED
            || startState == State.INSTALL_FAILED
            || startState == State.UNKNOWN) {
          return true;
        }
        break;
    }
    return false;
  }

  /**
   * Utility method to determine whether or not the given desired state is valid for the given starting state.
   *
   * @param startState    the starting state
   * @param desiredState  the desired state
   *
   * @return true iff the given desired state is valid for the given starting state
   */
  public static boolean isValidDesiredStateTransition(State startState, State desiredState) {
    switch(desiredState) {
      case INSTALLED:
        if (startState == State.INIT
            || startState == State.UNINSTALLED
            || startState == State.INSTALLED
            || startState == State.STARTED
            || startState == State.STOPPING) {
          return true;
        }
        break;
      case STARTED:
        if (startState == State.INSTALLED
            || startState == State.STARTED) {
          return true;
        }
        break;
    }
    return false;
  }

  /**
   * Determine whether or not it is safe to update the configuration of the given service
   * component host for the given states.
   *
   * @param serviceComponentHost  the service component host
   * @param currentState          the current state
   * @param desiredState          the desired state
   *
   * @throws AmbariException if the changing of configuration is not supported
   */
  public static void checkUpdateConfiguration(
      ServiceComponentHost serviceComponentHost,
      State currentState, State desiredState)
      throws AmbariException {

    if (desiredState != null) {
      if (!(desiredState == State.INIT
          || desiredState == State.INSTALLED
          || desiredState == State.STARTED)) {
        throw new AmbariException("Changing of configs not supported"
            + " for this transition"
            + ", clusterName=" + serviceComponentHost.getClusterName()
            + ", serviceName=" + serviceComponentHost.getServiceName()
            + ", componentName=" + serviceComponentHost.getServiceComponentName()
            + ", hostname=" + serviceComponentHost.getHostName()
            + ", currentState=" + currentState
            + ", newDesiredState=" + desiredState);
      }
    }
  }

}
