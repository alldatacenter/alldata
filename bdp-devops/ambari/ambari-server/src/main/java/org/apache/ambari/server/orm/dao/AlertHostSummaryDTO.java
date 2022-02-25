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
package org.apache.ambari.server.orm.dao;

import org.apache.ambari.server.state.AlertState;


/**
 * The {@link AlertHostSummaryDTO} is used to return a summary of the alerts
 * affecting the hosts of the cluster. This will contain an aggregate of the
 * number of hosts that have alerts in various alert states.
 * <p/>
 * The sum of all of the values of this DTO should equal the total number of
 * hosts.
 * <p/>
 * If a host or a host's components are in maintenance mode, then any alerts
 * triggered will not be reported here. Instead, they will be counted as
 * {@link AlertState#OK}.
 */
public class AlertHostSummaryDTO {

  /**
   * The number of hosts whose most critical alert is a WARNING.
   */
  private final int m_warningCount;

  /**
   * The number of hosts whose most critical alert is a CRITICAL.
   */
  private final int m_criticalCount;

  /**
   * The number of hosts whose most critical alert is a UNKNOWN.
   */
  private final int m_unknownCount;

  /**
   * The number of hosts with all OK.
   */
  private final int m_okCount;

  /**
   * Constructor.
   *
   * @param okCount
   * @param unknownCount
   * @param warningCount
   * @param criticalCount
   */
  public AlertHostSummaryDTO(int okCount, int unknownCount, int warningCount,
      int criticalCount) {

    m_okCount = okCount;
    m_unknownCount = unknownCount;
    m_warningCount = warningCount;
    m_criticalCount = criticalCount;
  }

  /**
   * @return the warningCount
   */
  public int getWarningCount() {
    return m_warningCount;
  }

  /**
   * @return the criticalCount
   */
  public int getCriticalCount() {
    return m_criticalCount;
  }

  /**
   * @return the unknownCount
   */
  public int getUnknownCount() {
    return m_unknownCount;
  }

  /**
   * @return the okCount
   */
  public int getOkCount() {
    return m_okCount;
  }
}
