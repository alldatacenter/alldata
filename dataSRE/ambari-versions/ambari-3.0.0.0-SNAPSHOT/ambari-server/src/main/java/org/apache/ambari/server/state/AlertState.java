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

import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Represents the state of an alert.
 */
public enum AlertState {
  /**
   * Alert does not need to be distributed.  Normal Operation.
   */
  OK(0),

  /**
   * Alert indicates there may be an issue.  The component may be operating
   * normally but may be in danger of becoming <code>CRITICAL</code>.
   */
  WARNING(2),

  /**
   * Indicates there is a critical situation that needs to be addressed.
   */
  CRITICAL(3),

  /**
   * The state of the alert is not known.
   */
  UNKNOWN(1),

  /**
   * Indicates that the state of the alert should be discarded, but the alert
   * timestamps should be updated so that it is not considered stale.
   */
  SKIPPED(4);

  public static final Set<AlertState> RECALCULATE_AGGREGATE_ALERT_STATES = Sets.immutableEnumSet(CRITICAL, WARNING);

  private final int intValue;

  public int getIntValue() {
    return intValue;
  }

  AlertState(int i) {
    this.intValue = i;
  }
}
