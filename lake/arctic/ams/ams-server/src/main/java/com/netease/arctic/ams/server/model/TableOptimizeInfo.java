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
import com.netease.arctic.table.TableProperties;

/**
 * Current optimize state of an ArcticTable.
 */
public class TableOptimizeInfo {

  public TableOptimizeInfo(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
    this.tableName = tableIdentifier.getCatalog().concat(".").concat(tableIdentifier.getDatabase())
        .concat(".").concat(tableIdentifier.getTableName());
  }

  private final TableIdentifier tableIdentifier;
  private String tableName;
  private String optimizeStatus = TableOptimizeRuntime.OptimizeStatus.Idle.displayValue();
  private long duration = 0;
  private long fileCount = 0;
  private long fileSize = 0;
  private double quota = 0.0;
  private double quotaOccupation = 0.0;

  private String groupName = TableProperties.SELF_OPTIMIZING_GROUP_DEFAULT;

  public TableIdentifier getTableIdentifier() {
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
    return "TableOptimizeInfo{" +
        "tableIdentifier=" + tableIdentifier +
        ", tableName='" + tableName + '\'' +
        ", optimizeStatus=" + optimizeStatus +
        ", duration=" + duration +
        ", fileCount=" + fileCount +
        ", fileSize=" + fileSize +
        ", quota=" + quota +
        ", quotaOccupation=" + quotaOccupation +
        ", groupName='" + groupName + '\'' +
        '}';
  }
}
