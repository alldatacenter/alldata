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
package org.apache.drill.exec.server;
/*
  State manager to manage the state of drillbit.
 */
public class DrillbitStateManager {

  public enum DrillbitState {
    STARTUP, ONLINE, GRACE, DRAINING, OFFLINE, SHUTDOWN
  }

  private DrillbitState currentState;

  public DrillbitStateManager(DrillbitState currentState) {
    this.currentState = currentState;
  }

  public DrillbitState getState() {
    return currentState;
  }

  public void setState(DrillbitState newState) {
    switch (newState) {
      case ONLINE:
        if (currentState == DrillbitState.STARTUP) {
          currentState = newState;
        } else {
          throw new IllegalStateException("Cannot set drillbit to" + newState + "from" + currentState);
        }
        break;
      case GRACE:
        if (currentState == DrillbitState.ONLINE) {
          currentState = newState;
        } else {
          throw new IllegalStateException("Cannot set drillbit to" + newState + "from" + currentState);
        }
        break;
      case DRAINING:
        if (currentState == DrillbitState.GRACE) {
          currentState = newState;
        } else {
          throw new IllegalStateException("Cannot set drillbit to" + newState + "from" + currentState);
        }
        break;
      case OFFLINE:
        if (currentState == DrillbitState.DRAINING) {
          currentState = newState;
        } else {
          throw new IllegalStateException("Cannot set drillbit to" + newState + "from" + currentState);
        }
        break;
      case SHUTDOWN:
        if (currentState == DrillbitState.OFFLINE) {
          currentState = newState;
        } else {
          throw new IllegalStateException("Cannot set drillbit to" + newState + "from" + currentState);
        }
        break;
      default:
        throw new IllegalArgumentException(newState.name());
    }
  }
}

