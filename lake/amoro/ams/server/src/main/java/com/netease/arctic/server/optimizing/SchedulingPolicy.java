package com.netease.arctic.server.optimizing;

import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.resource.ResourceGroup;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableRuntime;
import org.apache.commons.lang3.StringUtils;
import org.apache.iceberg.relocated.com.google.common.annotations.VisibleForTesting;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

public class SchedulingPolicy {

  private static final String SCHEDULING_POLICY_PROPERTY_NAME = "scheduling-policy";
  private static final String QUOTA = "quota";
  private static final String BALANCED = "balanced";

  private final Map<ServerTableIdentifier, TableRuntime> tableRuntimeMap = new HashMap<>();
  private final Comparator<TableRuntime> tableSorter;
  private final Lock tableLock = new ReentrantLock();

  public SchedulingPolicy(ResourceGroup group) {
    String schedulingPolicy = Optional.ofNullable(group.getProperties())
            .orElseGet(Maps::newHashMap)
            .get(SCHEDULING_POLICY_PROPERTY_NAME);
    if (StringUtils.isBlank(schedulingPolicy) || schedulingPolicy.equalsIgnoreCase(QUOTA)) {
      tableSorter = new QuotaOccupySorter();
    } else if (schedulingPolicy.equalsIgnoreCase(BALANCED)) {
      tableSorter = new BalancedSorter();
    } else {
      throw new IllegalArgumentException("Illegal scheduling policy: " + schedulingPolicy);
    }
  }

  public List<TableRuntime> scheduleTables() {
    tableLock.lock();
    try {
      return tableRuntimeMap.values().stream()
          .filter(tableRuntime -> tableRuntime.getOptimizingStatus() == OptimizingStatus.PENDING &&
              (tableRuntime.getLastOptimizedSnapshotId() != tableRuntime.getCurrentSnapshotId() ||
                  tableRuntime.getLastOptimizedChangeSnapshotId() != tableRuntime.getCurrentChangeSnapshotId()))
          .sorted(tableSorter)
          .collect(Collectors.toList());
    } finally {
      tableLock.unlock();
    }
  }

  public void addTable(TableRuntime tableRuntime) {
    tableLock.lock();
    try {
      tableRuntimeMap.put(tableRuntime.getTableIdentifier(), tableRuntime);
    } finally {
      tableLock.unlock();
    }
  }

  public void removeTable(TableRuntime tableRuntime) {
    tableLock.lock();
    try {
      tableRuntimeMap.remove(tableRuntime.getTableIdentifier());
    } finally {
      tableLock.unlock();
    }
  }

  public boolean containsTable(ServerTableIdentifier tableIdentifier) {
    return tableRuntimeMap.containsKey(tableIdentifier);
  }

  @VisibleForTesting
  Map<ServerTableIdentifier, TableRuntime> getTableRuntimeMap() {
    return tableRuntimeMap;
  }

  private static class QuotaOccupySorter implements Comparator<TableRuntime> {

    private final Map<TableRuntime, Double> tableWeightMap = Maps.newHashMap();

    @Override
    public int compare(TableRuntime one, TableRuntime another) {
      return Double.compare(
          tableWeightMap.computeIfAbsent(one, TableRuntime::calculateQuotaOccupy),
          tableWeightMap.computeIfAbsent(another, TableRuntime::calculateQuotaOccupy));
    }
  }

  private static class BalancedSorter implements Comparator<TableRuntime> {
    @Override
    public int compare(TableRuntime one, TableRuntime another) {
      return Long.compare(
          Math.max(
              one.getLastFullOptimizingTime(),
              Math.max(
                  one.getLastMinorOptimizingTime(),
                  one.getLastMajorOptimizingTime())),
          Math.max(
              another.getLastFullOptimizingTime(),
              Math.max(
                  another.getLastMinorOptimizingTime(),
                  another.getLastMajorOptimizingTime()))
      );
    }
  }
}
