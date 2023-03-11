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

package com.netease.arctic.ams.server.model;

import com.netease.arctic.table.TableIdentifier;
import org.apache.commons.collections.MapUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TableOptimizeRuntime implements Cloneable {
  public static final long INVALID_SNAPSHOT_ID = -1L;

  private TableIdentifier tableIdentifier;
  // for unKeyedTable or base table
  private long currentSnapshotId = INVALID_SNAPSHOT_ID;
  // for change table
  private long currentChangeSnapshotId = INVALID_SNAPSHOT_ID;
  private OptimizeStatus optimizeStatus = OptimizeStatus.Idle;
  private long optimizeStatusStartTime = -1;

  private final Map<String, Long> latestMajorOptimizeTime = new HashMap<>();
  private final Map<String, Long> latestFullOptimizeTime = new HashMap<>();
  private final Map<String, Long> latestMinorOptimizeTime = new HashMap<>();
  private String latestTaskPlanGroup;

  public TableOptimizeRuntime() {
  }

  public TableOptimizeRuntime(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public TableOptimizeRuntime(String catalog, String database, String tableName) {
    this.tableIdentifier = TableIdentifier.of(catalog, database, tableName);
  }

  @Override
  public TableOptimizeRuntime clone() {
    TableOptimizeRuntime newTableOptimizeRuntime = new TableOptimizeRuntime(this.tableIdentifier.getCatalog(),
        this.tableIdentifier.getDatabase(), this.tableIdentifier.getTableName());
    newTableOptimizeRuntime.currentSnapshotId = this.currentSnapshotId;
    newTableOptimizeRuntime.currentChangeSnapshotId = this.currentChangeSnapshotId;
    newTableOptimizeRuntime.optimizeStatus = this.optimizeStatus;
    newTableOptimizeRuntime.optimizeStatusStartTime = this.optimizeStatusStartTime;
    newTableOptimizeRuntime.latestMajorOptimizeTime.putAll(this.latestMajorOptimizeTime);
    newTableOptimizeRuntime.latestFullOptimizeTime.putAll(this.latestFullOptimizeTime);
    newTableOptimizeRuntime.latestMinorOptimizeTime.putAll(this.latestMinorOptimizeTime);
    newTableOptimizeRuntime.latestTaskPlanGroup = this.latestTaskPlanGroup;
    return newTableOptimizeRuntime;
  }

  public void restoreTableOptimizeRuntime(TableOptimizeRuntime old) {
    this.tableIdentifier = TableIdentifier.of(old.tableIdentifier.getCatalog(),
        old.tableIdentifier.getDatabase(), old.tableIdentifier.getTableName());
    this.currentSnapshotId = old.currentSnapshotId;
    this.currentChangeSnapshotId = old.currentChangeSnapshotId;
    this.optimizeStatus = old.optimizeStatus;
    this.optimizeStatusStartTime = old.optimizeStatusStartTime;
    this.latestMajorOptimizeTime.clear();
    this.latestMajorOptimizeTime.putAll(old.latestMajorOptimizeTime);
    this.latestFullOptimizeTime.clear();
    this.latestFullOptimizeTime.putAll(old.latestFullOptimizeTime);
    this.latestMinorOptimizeTime.clear();
    this.latestMinorOptimizeTime.putAll(old.latestMinorOptimizeTime);
    this.latestTaskPlanGroup = old.latestTaskPlanGroup;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public long getCurrentSnapshotId() {
    return currentSnapshotId;
  }

  public void setCurrentSnapshotId(long currentSnapshotId) {
    this.currentSnapshotId = currentSnapshotId;
  }

  public void putLatestMajorOptimizeTime(String partition, long time) {
    Long oldValue = latestMajorOptimizeTime.putIfAbsent(partition, time);
    if (oldValue != null) {
      if (time > oldValue) {
        latestMajorOptimizeTime.put(partition, time);
      }
    }
  }

  public void putLatestFullOptimizeTime(String partition, long time) {
    Long oldValue = latestFullOptimizeTime.putIfAbsent(partition, time);
    if (oldValue != null) {
      if (time > oldValue) {
        latestFullOptimizeTime.put(partition, time);
      }
    }
  }

  public OptimizeStatus getOptimizeStatus() {
    return optimizeStatus;
  }

  public void setOptimizeStatus(
      OptimizeStatus optimizeStatus) {
    this.optimizeStatus = optimizeStatus;
  }

  public long getOptimizeStatusStartTime() {
    return optimizeStatusStartTime;
  }

  public void setOptimizeStatusStartTime(long optimizeStatusStartTime) {
    this.optimizeStatusStartTime = optimizeStatusStartTime;
  }

  public long getLatestMajorOptimizeTime(String partition) {
    Long time = latestMajorOptimizeTime.get(partition);
    return time == null ? -1 : time;
  }

  public long getLatestFullOptimizeTime(String partition) {
    Long time = latestFullOptimizeTime.get(partition);
    return time == null ? -1 : time;
  }

  public void putLatestMinorOptimizeTime(String partition, long time) {
    Long oldValue = latestMinorOptimizeTime.putIfAbsent(partition, time);
    if (oldValue != null) {
      if (time > oldValue) {
        latestMinorOptimizeTime.put(partition, time);
      }
    }
  }

  public long getLatestMinorOptimizeTime(String partition) {
    Long time = latestMinorOptimizeTime.get(partition);
    return time == null ? -1 : time;
  }

  public Set<String> getPartitions() {
    Set<String> result = new HashSet<>();
    if (MapUtils.isNotEmpty(latestMajorOptimizeTime)) {
      result.addAll(latestMajorOptimizeTime.keySet());
    }
    if (MapUtils.isNotEmpty(latestFullOptimizeTime)) {
      result.addAll(latestFullOptimizeTime.keySet());
    }
    if (MapUtils.isNotEmpty(latestMinorOptimizeTime)) {
      result.addAll(latestMinorOptimizeTime.keySet());
    }

    return result;
  }

  public long getCurrentChangeSnapshotId() {
    return currentChangeSnapshotId;
  }

  public void setCurrentChangeSnapshotId(long currentChangeSnapshotId) {
    this.currentChangeSnapshotId = currentChangeSnapshotId;
  }

  public String getLatestTaskPlanGroup() {
    return latestTaskPlanGroup;
  }

  public void setLatestTaskPlanGroup(String latestTaskPlanGroup) {
    this.latestTaskPlanGroup = latestTaskPlanGroup;
  }

  @Override
  public String toString() {
    return "TableOptimizeRuntime{" +
        "tableIdentifier=" + tableIdentifier +
        ", currentSnapshotId=" + currentSnapshotId +
        ", currentChangeSnapshotId=" + currentChangeSnapshotId +
        ", optimizeStatus=" + optimizeStatus +
        ", optimizeStatusStartTime=" + optimizeStatusStartTime +
        ", latestMajorOptimizeTime=" + latestMajorOptimizeTime +
        ", latestFullOptimizeTime=" + latestFullOptimizeTime +
        ", latestMinorOptimizeTime=" + latestMinorOptimizeTime +
        ", latestTaskPlanGroup='" + latestTaskPlanGroup + '\'' +
        '}';
  }

  public enum OptimizeStatus {
    FullOptimizing("full"),
    MajorOptimizing("major"),
    MinorOptimizing("minor"),
    Pending("pending"),
    Idle("idle");

    private String displayValue;

    OptimizeStatus(String displayValue) {
      this.displayValue = displayValue;
    }

    public String displayValue() {
      return displayValue;
    }
  }
}
