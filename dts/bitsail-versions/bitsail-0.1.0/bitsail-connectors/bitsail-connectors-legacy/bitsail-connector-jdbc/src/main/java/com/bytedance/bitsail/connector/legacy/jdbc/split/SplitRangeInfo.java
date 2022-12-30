/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.jdbc.split;

import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @desc:
 */
@NoArgsConstructor
public class SplitRangeInfo<K> implements Serializable {

  private TableRangeInfo<K> tableRange;
  private String dbURL;
  private long rangeId;

  public SplitRangeInfo(long rangeId, TableRangeInfo<K> tableRange, String dbURL) {
    this.rangeId = rangeId;
    this.tableRange = tableRange;
    this.dbURL = dbURL;
  }

  public void setTableRange(TableRangeInfo<K> tableRange) {
    this.tableRange = tableRange;
  }

  public TableRangeInfo<K> getRange() {
    return tableRange;
  }

  public String getDbURL() {
    return dbURL;
  }

  public void setDbURL(String dbURL) {
    this.dbURL = dbURL;
  }

  public K getBeginPos() {
    return tableRange.getSplitKeyRange().getFirst();
  }

  public String getQuoteTableWithSchema() {
    return tableRange.getQuoteTableWithSchema();
  }

  public K getEndPos() {
    return tableRange.getSplitKeyRange().getSecond();
  }

  public long getRangeId() {
    return rangeId;
  }

  public void setRangeId(long rangeId) {
    this.rangeId = rangeId;
  }

  @Override
  public String toString() {
    return "SplitRangeInfo{" +
        "tableRange=" + tableRange +
        ", dbURL='" + dbURL + '\'' +
        ", rangeId=" + rangeId +
        '}';
  }
}
