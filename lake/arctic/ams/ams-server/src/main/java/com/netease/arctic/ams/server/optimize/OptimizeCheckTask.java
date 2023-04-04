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

package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.utils.ScheduledTasks;
import com.netease.arctic.table.TableIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizeCheckTask implements ScheduledTasks.Task {
  private static final Logger LOG = LoggerFactory.getLogger(OptimizeCheckTask.class);

  private final TableIdentifier tableIdentifier;

  public OptimizeCheckTask(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  @Override
  public void run() {
    long startTime = System.currentTimeMillis();
    TableOptimizeItem tableOptimize;
    try {
      tableOptimize = ServiceContainer.getOptimizeService().getTableOptimizeItem(tableIdentifier);
    } catch (Throwable t) {
      LOG.warn("optimize checker failed to get table " + tableIdentifier, t);
      return;
    }
    try {
      check(tableOptimize);
    } catch (Exception e) {
      LOG.error("unexpected check error " + tableOptimize.getTableIdentifier(), e);
    }
    LOG.info("{} optimize checker total cost {} ms", tableIdentifier, System.currentTimeMillis() - startTime);
  }

  private void check(TableOptimizeItem tableOptimize) {
    LOG.info("{} start check", tableOptimize.getTableIdentifier());
    tableOptimize.checkTaskExecuteTimeout();
    tableOptimize.checkOptimizeGroup();
    tableOptimize.clearFailedTasks();
    tableOptimize.tryTriggerCommit();
    tableOptimize.updateTableOptimizeStatus();
  }
}
