package com.netease.arctic.server.table.executor;

import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.table.TableManager;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.table.ArcticTable;

import java.util.Optional;

public class OptimizingCommitExecutor extends BaseTableExecutor {

  private static final long INTERVAL = 60 * 1000L; // 1min

  public OptimizingCommitExecutor(TableManager tableRuntimes, int poolSize) {
    super(tableRuntimes, poolSize);
  }

  @Override
  protected long getNextExecutingTime(TableRuntime tableRuntime) {
    return INTERVAL;
  }

  @Override
  protected boolean enabled(TableRuntime tableRuntime) {
    return tableRuntime.getOptimizingStatus() == OptimizingStatus.COMMITTING;
  }

  @Override
  protected void execute(TableRuntime tableRuntime) {
    Optional.ofNullable(tableRuntime.getOptimizingProcess())
        .orElseThrow(() -> new IllegalStateException("OptimizingProcess is null while committing:" + tableRuntime))
        .commit();
  }

  @Override
  public void handleStatusChanged(TableRuntime tableRuntime, OptimizingStatus originalStatus) {
    scheduleIfNecessary(tableRuntime, getStartDelay());
  }

  @Override
  public void handleTableAdded(ArcticTable table, TableRuntime tableRuntime) {
  }

  protected long getStartDelay() {
    return 0;
  }
}
