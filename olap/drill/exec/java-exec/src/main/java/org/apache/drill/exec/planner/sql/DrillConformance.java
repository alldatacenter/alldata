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
package org.apache.drill.exec.planner.sql;

import org.apache.calcite.sql.validate.SqlConformanceEnum;
import org.apache.calcite.sql.validate.SqlDelegatingConformance;

/**
 * Drill's SQL conformance is SqlConformanceEnum.DEFAULT with a couple of deviations.
 */
public class DrillConformance extends SqlDelegatingConformance {

  public DrillConformance() {
    super(SqlConformanceEnum.DEFAULT);
  }

  public DrillConformance(SqlConformanceEnum flavor) {
    super(flavor);
  }

  /**
   * Drill allows OUTER APPLY and CROSS APPLY to allow each row from left child of Join
   * to join with output of right side (sub-query or table function that will be invoked for each row).
   * Refer to DRILL-5999 for more information.
   */
  @Override
  public boolean isApplyAllowed() {
    return true;
  }

  @Override
  public boolean isGroupByOrdinal() {
    return true;
  }

  @Override
  public boolean isGroupByAlias() {
    return true;
  }

  @Override
  public boolean isHavingAlias() {
    return true;
  }
}
