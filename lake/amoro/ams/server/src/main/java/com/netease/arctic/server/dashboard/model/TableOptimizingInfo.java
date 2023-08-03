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

package com.netease.arctic.server.dashboard.model;

import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;

/**
 * Current optimize state of an ArcticTable.
 */
public class TableOptimizingInfo {

  public TableOptimizingInfo(ServerTableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
    this.tableName = tableIdentifier.getCatalog().concat(".").concat(tableIdentifier.getDatabase())
        .concat(".").concat(tableIdentifier.getTableName());
  }

  private final ServerTableIdentifier tableIdentifier;
  private String tableName;
  private String optimizeStatus = OptimizingStatus.IDLE.displayValue();
  private long duration = 0;
  private long fileCount = 0;
  private long fileSize = 0;
  private double quota = 0.0;
  private double quotaOccupation = 0.0;

  private String groupName = TableProperties.SELF_OPTIMIZING_GROUP_DEFAULT;

  public ServerTableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public String getTableName() {
    return tableName;
  }

  public String getOptimizeStatus() {
    return optimizeStatus;
  }

  public void setOptimizeStatus(String optimizeStatus) {
    this.optimizeStatus = optimizeStatus;
  }

  public long getDuration() {
    return duration;
  }

  public void setDuration(long duration) {
    this.duration = duration;
  }

  public long getFileCount() {
    return fileCount;
  }

  public void setFileCount(long fileCount) {
    this.fileCount = fileCount;
  }

  public long getFileSize() {
    return fileSize;
  }

  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  public double getQuota() {
    return quota;
  }

  public void setQuota(double quota) {
    this.quota = quota;
  }

  public double getQuotaOccupation() {
    return quotaOccupation;
  }

  public void setQuotaOccupation(double quotaOccupation) {
    this.quotaOccupation = quotaOccupation;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("tableIdentifier", tableIdentifier)
        .add("tableName", tableName)
        .add("optimizeStatus", optimizeStatus)
        .add("duration", duration)
        .add("fileCount", fileCount)
        .add("fileSize", fileSize)
        .add("quota", quota)
        .add("quotaOccupation", quotaOccupation)
        .add("groupName", groupName)
        .toString();
  }
}
