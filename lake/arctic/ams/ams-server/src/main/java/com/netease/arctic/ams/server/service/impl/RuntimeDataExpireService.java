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

package com.netease.arctic.ams.server.service.impl;

import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.ams.server.model.TableOptimizeRuntime;
import com.netease.arctic.ams.server.optimize.IOptimizeService;
import com.netease.arctic.ams.server.service.IMetaService;
import com.netease.arctic.ams.server.service.ITableTaskHistoryService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.table.TableIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RuntimeDataExpireService {
  private static final Logger LOG = LoggerFactory.getLogger(RuntimeDataExpireService.class);

  private final ArcticTransactionService transactionService;
  private final IMetaService metaService;
  private final IOptimizeService optimizeService;
  private final ITableTaskHistoryService tableTaskHistoryService;
  private final TableBlockerService tableBlockerService;

  // 1 days
  Long txDataExpireInterval = 24 * 60 * 60 * 1000L;
  // 7 days
  Long taskHistoryDataExpireInterval = 7 * 24 * 60 * 60 * 1000L;
  // 30 days
  Long optimizeHistoryDataExpireInterval = 30 * 24 * 60 * 60 * 1000L;


  public RuntimeDataExpireService() {
    this.transactionService = ServiceContainer.getArcticTransactionService();
    this.metaService = ServiceContainer.getMetaService();
    this.tableTaskHistoryService = ServiceContainer.getTableTaskHistoryService();
    this.optimizeService = ServiceContainer.getOptimizeService();
    this.tableBlockerService = ServiceContainer.getTableBlockerService();
  }

  public void doExpire() {
    try {
      expire();
    } catch (Throwable t) {
      LOG.error("unexpected expire error", t);
    }
  }

  private void expire() {
    List<TableMetadata> tableMetadata = metaService.listTables();

    // expire and clear table_task_history table
    tableMetadata.forEach(meta -> {
      TableIdentifier identifier = meta.getTableIdentifier();
      try {
        TableOptimizeRuntime tableOptimizeRuntime =
            optimizeService.getTableOptimizeItem(identifier).getTableOptimizeRuntime();
        tableTaskHistoryService.expireTaskHistory(identifier,
            tableOptimizeRuntime.getLatestTaskPlanGroup(),
            System.currentTimeMillis() - this.taskHistoryDataExpireInterval);
      } catch (Exception e) {
        LOG.error("{} failed to expire and clear table_task_history table", identifier, e);
      }
    });

    // expire and clear optimize_history table
    tableMetadata.forEach(meta -> {
      TableIdentifier identifier = meta.getTableIdentifier();
      try {
        optimizeService.expireOptimizeHistory(identifier,
            System.currentTimeMillis() - this.optimizeHistoryDataExpireInterval);
      } catch (Exception e) {
        LOG.error("{} failed to expire and clear optimize_history table", identifier, e);
      }
    });

    // expire and clean table_blocker
    tableMetadata.forEach(meta -> {
      TableIdentifier identifier = meta.getTableIdentifier();
      try {
        tableBlockerService.expireBlockers(identifier);
      } catch (Exception e) {
        LOG.error("{} failed to expire and clear table_blocker", identifier, e);
      }
    });
  }
}
