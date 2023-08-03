package com.netease.arctic.server.table;

import com.netease.arctic.server.optimizing.OptimizingStatus;
import com.netease.arctic.table.ArcticTable;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public abstract class RuntimeHandlerChain {
  
  private static final Logger LOG = LoggerFactory.getLogger(RuntimeHandlerChain.class);

  private RuntimeHandlerChain next;

  private boolean initialized;

  protected void appendNext(RuntimeHandlerChain handler) {
    Preconditions.checkNotNull(handler);
    Preconditions.checkArgument(!Objects.equals(handler, this),
        "Cannot add the same runtime handler:{} twice", handler.getClass().getSimpleName());
    if (next == null) {
      next = handler;
    } else {
      next.appendNext(handler);
    }
  }

  public final void initialize(List<TableRuntimeMeta> tableRuntimeMetaList) {
    initHandler(tableRuntimeMetaList);
    initialized = true;
    if (next != null) {
      next.initialize(tableRuntimeMetaList);
    }
  }

  public final void fireStatusChanged(TableRuntime tableRuntime, OptimizingStatus originalStatus) {
    if (!initialized) return;

    doSilently(() -> handleStatusChanged(tableRuntime, originalStatus));
    if (next != null) {
      next.fireStatusChanged(tableRuntime, originalStatus);
    }
  }

  public final void fireConfigChanged(TableRuntime tableRuntime, TableConfiguration originalConfig) {
    if (!initialized) return;

    doSilently(() -> handleConfigChanged(tableRuntime, originalConfig));
    if (next != null) {
      next.fireConfigChanged(tableRuntime, originalConfig);
    }
  }

  public final void fireTableAdded(ArcticTable table, TableRuntime tableRuntime) {
    if (!initialized) return;

    doSilently(() -> handleTableAdded(table, tableRuntime));
    if (next != null) {
      next.fireTableAdded(table, tableRuntime);
    }
  }

  public final void fireTableRemoved(TableRuntime tableRuntime) {
    if (!initialized) return;

    if (next != null) {
      next.fireTableRemoved(tableRuntime);
    }
    doSilently(() -> handleTableRemoved(tableRuntime));
  }

  public final void dispose() {
    if (next != null) {
      next.doDispose();
    }
    doSilently(this::doDispose);
  }

  private void doSilently(Runnable runnable) {
    try {
      runnable.run();
    } catch (Throwable t) {
      LOG.error("failed to handle, ignore and continue", t);
    }
  }

  protected abstract void handleStatusChanged(TableRuntime tableRuntime, OptimizingStatus originalStatus);

  protected abstract void handleConfigChanged(TableRuntime tableRuntime, TableConfiguration originalConfig);

  protected abstract void handleTableAdded(ArcticTable table, TableRuntime tableRuntime);

  protected abstract void handleTableRemoved(TableRuntime tableRuntime);

  protected abstract void initHandler(List<TableRuntimeMeta> tableRuntimeMetaList);

  protected abstract void doDispose();
}
