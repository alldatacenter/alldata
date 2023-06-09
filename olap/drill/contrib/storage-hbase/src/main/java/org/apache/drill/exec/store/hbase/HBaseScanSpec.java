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
package org.apache.drill.exec.store.hbase;


import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.exec.planner.logical.DrillTableSelection;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.filter.Filter;
import org.apache.hadoop.hbase.util.Bytes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class HBaseScanSpec implements DrillTableSelection {

  protected String tableName;
  protected byte[] startRow;
  protected byte[] stopRow;

  protected Filter filter;

  @JsonCreator
  public HBaseScanSpec(@JsonProperty("tableName") String tableName,
                       @JsonProperty("startRow") byte[] startRow,
                       @JsonProperty("stopRow") byte[] stopRow,
                       @JsonProperty("serializedFilter") byte[] serializedFilter,
                       @JsonProperty("filterString") String filterString) {
    if (serializedFilter != null && filterString != null) {
      throw new IllegalArgumentException("The parameters 'serializedFilter' or 'filterString' cannot be specified at the same time.");
    }
    this.tableName = tableName;
    this.startRow = startRow;
    this.stopRow = stopRow;
    if (filterString != null) {
      this.filter = HBaseUtils.parseFilterString(filterString);
    } else {
      this.filter = HBaseUtils.deserializeFilter(serializedFilter);
    }
  }

  public HBaseScanSpec(String tableName, byte[] startRow, byte[] stopRow, Filter filter) {
    this.tableName = tableName;
    this.startRow = startRow;
    this.stopRow = stopRow;
    this.filter = filter;
  }

  public HBaseScanSpec(String tableName) {
    this.tableName = tableName;
  }

  public String getTableName() {
    return tableName;
  }

  public byte[] getStartRow() {
    return startRow == null ? HConstants.EMPTY_START_ROW : startRow;
  }

  public byte[] getStopRow() {
    return stopRow == null ? HConstants.EMPTY_START_ROW : stopRow;
  }

  @JsonIgnore
  public Filter getFilter() {
    return this.filter;
  }

  public byte[] getSerializedFilter() {
    return (this.filter != null) ? HBaseUtils.serializeFilter(this.filter) : null;
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("tableName", tableName)
      .field("startRow", startRow == null ? null : Bytes.toStringBinary(startRow))
      .field("stopRow", stopRow == null ? null : Bytes.toStringBinary(stopRow))
      .field("filter", filter == null ? null : filter.toString())
      .toString();
  }

  @Override
  public String digest() {
    return toString();
  }
}
