package com.netease.arctic.server.table;

import com.netease.arctic.server.optimizing.OptimizingProcess;
import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.optimizing.OptimizingType;

import java.util.Map;

public class TableRuntimeMeta {
  private long tableId;
  private String catalogName;
  private String dbName;
  private String tableName;
  private long currentSnapshotId;
  private long lastOptimizedSnapshotId;
  private long lastOptimizedChangeSnapshotId;
  private long currentChangeSnapshotId;
  private long lastMajorOptimizingTime;
  private long lastMinorOptimizingTime;
  private long lastFullOptimizingTime;
  private OptimizingStatus tableStatus;
  private long currentStatusStartTime;
  private String optimizerGroup;
  private TableConfiguration tableConfig;
  private long optimizingProcessId = 0;
  private OptimizingProcess.Status processStatus;
  private OptimizingType optimizingType;
  private long targetSnapshotId;
  private long targetChangeSnapshotId;
  private long planTime;
  private long endTime;
  private String failReason;
  private String summary;
  private Map<String, Long> fromSequence;
  private Map<String, Long> toSequence;

  private TableRuntime tableRuntime;

  public TableRuntimeMeta() {
  }

  public TableRuntime constructTableRuntime(TableManager initializer) {
    if (tableRuntime == null) {
      tableRuntime = new TableRuntime(this, initializer);
    }
    return tableRuntime;
  }

  public TableRuntime getTableRuntime() {
    if (tableRuntime == null) {
      throw new IllegalStateException("TableRuntime is not constructed yet.");
    }
    return tableRuntime;
  }

  public long getTargetSnapshotId() {
    return targetSnapshotId;
  }

  public OptimizingType getOptimizingType() {
    return optimizingType;
  }

  public long getLastOptimizedSnapshotId() {
    return lastOptimizedSnapshotId;
  }

  public long getLastOptimizedChangeSnapshotId() {
    return lastOptimizedChangeSnapshotId;
  }

  public long getTargetChangeSnapshotId() {
    return targetChangeSnapshotId;
  }

  public long getTableId() {
    return tableId;
  }

  public String getCatalogName() {
    return catalogName;
  }

  public String getDbName() {
    return dbName;
  }

  public String getTableName() {
    return tableName;
  }

  public long getCurrentSnapshotId() {
    return currentSnapshotId;
  }

  public long getCurrentChangeSnapshotId() {
    return currentChangeSnapshotId;
  }

  public long getLastMajorOptimizingTime() {
    return lastMajorOptimizingTime;
  }

  public long getLastMinorOptimizingTime() {
    return lastMinorOptimizingTime;
  }

  public long getLastFullOptimizingTime() {
    return lastFullOptimizingTime;
  }

  public OptimizingStatus getTableStatus() {
    return tableStatus;
  }

  public long getCurrentStatusStartTime() {
    return currentStatusStartTime;
  }

  public String getOptimizerGroup() {
    return optimizerGroup;
  }

  public TableConfiguration getTableConfig() {
    return tableConfig;
  }

  public long getOptimizingProcessId() {
    return optimizingProcessId;
  }

  public OptimizingProcess.Status getProcessStatus() {
    return processStatus;
  }

  public long getPlanTime() {
    return planTime;
  }

  public String getFailReason() {
    return failReason;
  }

  public long getEndTime() {
    return endTime;
  }

  public String getSummary() {
    return summary;
  }

  public void setTableId(long tableId) {
    this.tableId = tableId;
  }

  public void setCatalogName(String catalogName) {
    this.catalogName = catalogName;
  }

  public void setDbName(String dbName) {
    this.dbName = dbName;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public void setCurrentSnapshotId(long currentSnapshotId) {
    this.currentSnapshotId = currentSnapshotId;
  }

  public void setLastOptimizedSnapshotId(long lastOptimizedSnapshotId) {
    this.lastOptimizedSnapshotId = lastOptimizedSnapshotId;
  }

  public void setLastOptimizedChangeSnapshotId(long lastOptimizedChangeSnapshotId) {
    this.lastOptimizedChangeSnapshotId = lastOptimizedChangeSnapshotId;
  }

  public void setTargetChangeSnapshotId(long targetChangeSnapshotId) {
    this.targetChangeSnapshotId = targetChangeSnapshotId;
  }

  public void setCurrentChangeSnapshotId(long currentChangeSnapshotId) {
    this.currentChangeSnapshotId = currentChangeSnapshotId;
  }

  public void setLastMajorOptimizingTime(long lastMajorOptimizingTime) {
    this.lastMajorOptimizingTime = lastMajorOptimizingTime;
  }

  public void setLastMinorOptimizingTime(long lastMinorOptimizingTime) {
    this.lastMinorOptimizingTime = lastMinorOptimizingTime;
  }

  public void setLastFullOptimizingTime(long lastFullOptimizingTime) {
    this.lastFullOptimizingTime = lastFullOptimizingTime;
  }

  public Map<String, Long> getFromSequence() {
    return fromSequence;
  }

  public void setFromSequence(Map<String, Long> fromSequence) {
    this.fromSequence = fromSequence;
  }

  public Map<String, Long> getToSequence() {
    return toSequence;
  }

  public void setToSequence(Map<String, Long> toSequence) {
    this.toSequence = toSequence;
  }

  public void setTableStatus(OptimizingStatus tableStatus) {
    this.tableStatus = tableStatus;
  }

  public void setCurrentStatusStartTime(long currentStatusStartTime) {
    this.currentStatusStartTime = currentStatusStartTime;
  }

  public void setOptimizerGroup(String optimizerGroup) {
    this.optimizerGroup = optimizerGroup;
  }

  public void setTableConfig(TableConfiguration tableConfig) {
    this.tableConfig = tableConfig;
  }

  public void setOptimizingProcessId(long optimizingProcessId) {
    this.optimizingProcessId = optimizingProcessId;
  }

  public void setProcessStatus(OptimizingProcess.Status processStatus) {
    this.processStatus = processStatus;
  }

  public void setOptimizingType(OptimizingType optimizingType) {
    this.optimizingType = optimizingType;
  }

  public void setTargetSnapshotId(long targetSnapshotId) {
    this.targetSnapshotId = targetSnapshotId;
  }

  public void setPlanTime(long planTime) {
    this.planTime = planTime;
  }

  public void setEndTime(long endTime) {
    this.endTime = endTime;
  }

  public void setFailReason(String failReason) {
    this.failReason = failReason;
  }

  public void setSummary(String summary) {
    this.summary = summary;
  }
}
