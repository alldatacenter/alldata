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

import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.utils.CatalogUtil;
import com.netease.arctic.ams.server.utils.ScheduledTasks;
import com.netease.arctic.ams.server.utils.ThreadPool;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.io.TableTrashManager;
import com.netease.arctic.io.TableTrashManagers;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import org.apache.commons.lang3.RandomUtils;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Set;

/**
 * Service to clean trash periodically.
 */
public class TrashCleanService {
  private static final Logger LOG = LoggerFactory.getLogger(TrashCleanService.class);

  private static final long CHECK_INTERVAL = 24 * 60 * 60 * 1000;  // 1 days

  private ScheduledTasks<TableIdentifier, TableTrashCleanTask> cleanTasks;

  public synchronized void checkTrashCleanTasks() {
    LOG.info("Schedule Trash Cleaner");
    if (cleanTasks == null) {
      cleanTasks = new ScheduledTasks<>(ThreadPool.Type.TRASH_CLEAN);
    }

    Set<TableIdentifier> tableIds = CatalogUtil.loadTablesFromCatalog();
    cleanTasks.checkRunningTask(tableIds,
        () -> RandomUtils.nextLong(0, CHECK_INTERVAL),
        () -> CHECK_INTERVAL,
        TableTrashCleanTask::new,
        true);
    LOG.info("Schedule Trash Cleaner finished with {} tasks", tableIds.size());
  }


  public static class TableTrashCleanTask implements ScheduledTasks.Task {
    private final TableIdentifier tableIdentifier;

    public TableTrashCleanTask(TableIdentifier tableIdentifier) {
      this.tableIdentifier = tableIdentifier;
    }

    @Override
    public void run() {
      try {
        LOG.info("{} start clean trash", tableIdentifier);
        ArcticCatalog catalog =
            CatalogLoader.load(ServiceContainer.getTableMetastoreHandler(), tableIdentifier.getCatalog());
        clean(catalog.loadTable(tableIdentifier));
      } catch (Throwable t) {
        LOG.error("{} clean trash unexpected error", tableIdentifier, t);
      }
    }
  }

  static void clean(ArcticTable arcticTable) {
    int keepDays = PropertyUtil.propertyAsInt(arcticTable.properties(), TableProperties.TABLE_TRASH_KEEP_DAYS,
        TableProperties.TABLE_TRASH_KEEP_DAYS_DEFAULT);
    LocalDate expirationDate = LocalDate.now().minusDays(keepDays);

    TableTrashManager tableTrashManager = TableTrashManagers.build(arcticTable);

    LOG.info("{} clean trash, keepDays={}", arcticTable.id(), keepDays);
    tableTrashManager.cleanFiles(expirationDate);
    LOG.info("{} clean trash finished", arcticTable.id());
  }
}

