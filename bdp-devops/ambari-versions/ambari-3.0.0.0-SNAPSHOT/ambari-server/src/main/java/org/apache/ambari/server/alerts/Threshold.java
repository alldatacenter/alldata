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
package org.apache.ambari.server.alerts;

import javax.annotation.Nullable;

import org.apache.ambari.server.state.AlertState;

/**
 * I'm a 3 level threshold where each level corresponds to ok, warn and critical levels.
 * The levels are either increasing or decreasing numerical sequences.
 * My main responsibility is to map incoming values to {@link AlertState} based on the threshold levels.
 */
public class Threshold {
  private final Double okValue;
  private final double warnValue;
  private final double critValue;

  public Threshold(@Nullable Double okValue, double warnValue, double critValue) {
    this.okValue = okValue;
    this.warnValue = warnValue;
    this.critValue = critValue;
  }

  public AlertState state(double value) {
    return directionUp() ? stateWhenDirectionUp(value) : stateWhenDirectionDown(value);
  }

  private boolean directionUp() {
    return critValue >= warnValue;
  }

  private AlertState stateWhenDirectionUp(double value) {
    if (value >= critValue) {
      return AlertState.CRITICAL;
    }
    if (value >= warnValue) {
      return AlertState.WARNING;
    }
    if (okValue == null || value >= okValue) {
      return AlertState.OK;
    }
    return AlertState.UNKNOWN;
  }

  private AlertState stateWhenDirectionDown(double value) {
    if (value <= critValue) {
      return AlertState.CRITICAL;
    }
    if (value <= warnValue) {
      return AlertState.WARNING;
    }
    if (okValue == null || value <= okValue) {
      return AlertState.OK;
    }
    return AlertState.UNKNOWN;
  }
}
