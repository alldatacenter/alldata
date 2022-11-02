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
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Used to return alert summary data out of the database. Alerts that are in
 * maintenance mode will be reported as such instead of their actual values.
 */
public class AlertSummaryDTO {

  @JsonProperty("OK")
  private int okCount;

  @JsonProperty("WARNING")
  private int warningCount;

  @JsonProperty("CRITICAL")
  private int criticalCount;

  @JsonProperty("UNKNOWN")
  private int unknownCount;

  @JsonProperty("MAINTENANCE")
  private int maintenanceCount;

  /**
   * Constructor, used by JPA.  JPA invokes this constructor, even if there
   * are no records in the resultset.  In that case, all arguments are {@code null}.
   */
  public AlertSummaryDTO(Number ok, Number warning, Number critical,
      Number unknown, Number maintenance) {
    okCount = null == ok ? 0 : ok.intValue();
    warningCount = null == warning ? 0 : warning.intValue();
    criticalCount = null == critical ? 0 : critical.intValue();
    unknownCount = null == unknown ? 0 : unknown.intValue();
    maintenanceCount = null == maintenance ? 0 : maintenance.intValue();
  }

  /**
   * @return the count of {@link AlertState#OK} states
   */
  public int getOkCount() {
    return okCount;
  }

  /**
   * @return the count of {@link AlertState#WARNING} states
   */
  public int getWarningCount() {
    return warningCount;
  }

  /**
   * @return the count of {@link AlertState#CRITICAL} states
   */
  public int getCriticalCount() {
    return criticalCount;
  }

  /**
   * @return the count of {@link AlertState#UNKNOWN} states
   */
  public int getUnknownCount() {
    return unknownCount;
  }

  /**
   * @return the count of alerts that are in the maintenance state.
   */
  public int getMaintenanceCount() {
    return maintenanceCount;
  }

  /**
   * Sets the count of {@link AlertState#OK} states.
   *
   * @param okCount
   *          the okCount to set
   */
  public void setOkCount(int okCount) {
    this.okCount = okCount;
  }

  /**
   * Sets the count of {@link AlertState#WARNING} states.
   *
   * @param warningCount
   *          the warningCount to set
   */
  public void setWarningCount(int warningCount) {
    this.warningCount = warningCount;
  }

  /**
   * Sets the count of {@link AlertState#CRITICAL} states.
   *
   * @param criticalCount
   *          the criticalCount to set
   */
  public void setCriticalCount(int criticalCount) {
    this.criticalCount = criticalCount;
  }

  /**
   * Sets the count of {@link AlertState#UNKNOWN} states.
   *
   * @param unknownCount
   *          the unknownCount to set
   */
  public void setUnknownCount(int unknownCount) {
    this.unknownCount = unknownCount;
  }

  /**
   * Sets the count of alerts in maintenance state.
   *
   * @param maintenanceCount
   *          the maintenanceCount to set
   */
  public void setMaintenanceCount(int maintenanceCount) {
    this.maintenanceCount = maintenanceCount;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;

    if (!(o instanceof AlertSummaryDTO)) return false;

    AlertSummaryDTO that = (AlertSummaryDTO) o;

    return new EqualsBuilder()
        .append(okCount, that.okCount)
        .append(warningCount, that.warningCount)
        .append(criticalCount, that.criticalCount)
        .append(unknownCount, that.unknownCount)
        .append(maintenanceCount, that.maintenanceCount)
        .isEquals();
  }

  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(okCount)
        .append(warningCount)
        .append(criticalCount)
        .append(unknownCount)
        .append(maintenanceCount)
        .toHashCode();
  }
}
