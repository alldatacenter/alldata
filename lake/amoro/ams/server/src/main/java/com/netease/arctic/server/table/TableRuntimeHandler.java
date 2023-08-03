package com.netease.arctic.server.table;

import com.netease.arctic.server.optimizing.OptimizingStatus;

public interface TableRuntimeHandler {

  void addHandlerChain(RuntimeHandlerChain handler);

  void handleTableChanged(TableRuntime tableRuntime, OptimizingStatus originalStatus);

  void handleTableChanged(TableRuntime tableRuntime, TableConfiguration originalConfig);
}
