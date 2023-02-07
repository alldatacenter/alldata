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
package org.apache.drill.exec.store.druid;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.drill.exec.store.druid.common.DruidFilter;

public class DruidScanSpec implements DrillTableSelection {

  private final String dataSourceName;
  private final long dataSourceSize;
  private final String dataSourceMinTime;
  private final String dataSourceMaxTime;
  private DruidFilter filter;

  @JsonCreator
  public DruidScanSpec(@JsonProperty("dataSourceName") String dataSourceName,
                       @JsonProperty("dataSourceSize") long dataSourceSize,
                       @JsonProperty("dataSourceMinTime") String dataSourceMinTime,
                       @JsonProperty("dataSourceMaxTime") String dataSourceMaxTime) {
    this.dataSourceName = dataSourceName;
    this.dataSourceSize = dataSourceSize;
    this.dataSourceMinTime = dataSourceMinTime;
    this.dataSourceMaxTime = dataSourceMaxTime;
  }

  public DruidScanSpec(String dataSourceName,
                       DruidFilter filter,
                       long dataSourceSize,
                       String dataSourceMinTime,
                       String dataSourceMaxTime) {
    this.dataSourceName = dataSourceName;
    this.dataSourceSize = dataSourceSize;
    this.dataSourceMinTime = dataSourceMinTime;
    this.dataSourceMaxTime = dataSourceMaxTime;
    this.filter = filter;
  }

  public String getDataSourceName() {
    return this.dataSourceName;
  }

  public long getDataSourceSize() {
    return dataSourceSize;
  }

  public String getDataSourceMinTime() {
    return dataSourceMinTime;
  }

  public String getDataSourceMaxTime() {
    return dataSourceMaxTime;
  }

  public DruidFilter getFilter() {
    return this.filter;
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("dataSourceName", dataSourceName)
      .field("", dataSourceSize)
      .field("", dataSourceMinTime)
      .field("", dataSourceMaxTime)
      .field("filter", filter)
      .toString();
  }

  @Override
  public String digest() {
    return toString();
  }
}
