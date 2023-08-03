package com.netease.arctic.server.table.executor;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.server.table.RuntimeHandlerChain;
import com.netease.arctic.server.table.TableConfiguration;
import com.netease.arctic.server.table.TableManager;
import com.netease.arctic.server.table.TableRuntime;
import com.netease.arctic.server.table.TableRuntimeMeta;
import com.netease.arctic.table.ArcticTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class BaseTableExecutor extends RuntimeHandlerChain {

  protected final Logger logger = LoggerFactory.getLogger(getClass());

  private static final long START_DELAY = 10 * 1000L;

  private final ScheduledExecutorService executor;
  private final TableManager tableManager;

  protected BaseTableExecutor(TableManager tableManager, int poolSize) {
    this.tableManager = tableManager;
    this.executor = Executors.newScheduledThreadPool(
        poolSize,
        new ThreadFactoryBuilder()
            .setDaemon(false)
            .setNameFormat("ASYNC-" + getThreadName() + "-%d").build());
  }

  @Override
  protected void initHandler(List<TableRuntimeMeta> tableRuntimeMetaList) {
    tableRuntimeMetaList.stream()
        .map(tableRuntimeMeta -> tableRuntimeMeta.getTableRuntime())
        .filter(tableRuntime -> enabled(tableRuntime))
        .forEach(tableRuntime ->
            executor.schedule(
                () -> executeTask(tableRuntime),
                getStartDelay(),
                TimeUnit.MILLISECONDS));
    logger.info("Table executor {} initialized", getClass().getSimpleName());
  }

  private void executeTask(TableRuntime tableRuntime) {
    if (isExecutable(tableRuntime)) {
      try {
        execute(tableRuntime);
      } finally {
        scheduleIfNecessary(
            tableRuntime,
            getNextExecutingTime(tableRuntime));
      }
    }
  }

  protected final void scheduleIfNecessary(TableRuntime tableRuntime, long millisecondsTime) {
    if (isExecutable(tableRuntime)) {
      executor.schedule(
          () -> executeTask(tableRuntime),
          millisecondsTime,
          TimeUnit.MILLISECONDS);
    }
  }

  protected abstract long getNextExecutingTime(TableRuntime tableRuntime);

  protected abstract boolean enabled(TableRuntime tableRuntime);

  protected abstract void execute(TableRuntime tableRuntime);

  protected String getThreadName() {
    return getClass().getSimpleName();
  }

  private boolean isExecutable(TableRuntime tableRuntime) {
    return tableManager.contains(tableRuntime.getTableIdentifier()) && enabled(tableRuntime);
  }

  @Override
  public void handleConfigChanged(TableRuntime tableRuntime, TableConfiguration originalConfig) {
    scheduleIfNecessary(tableRuntime, getStartDelay());
  }

  @Override
  public void handleTableRemoved(TableRuntime tableRuntime) {
    //DO nothing, handling would be canceled when calling executeTable
  }

  @Override
  public void handleStatusChanged(TableRuntime tableRuntime, OptimizingStatus originalStatus) {
  }

  @Override
  public void handleTableAdded(ArcticTable table, TableRuntime tableRuntime) {
    scheduleIfNecessary(tableRuntime, getStartDelay());
  }

  @Override
  protected void doDispose() {
    executor.shutdownNow();
  }

  protected long getStartDelay() {
    return START_DELAY;
  }

  protected ArcticTable loadTable(TableRuntime tableRuntime) {
    return tableManager.loadTable(tableRuntime.getTableIdentifier());
  }
}
