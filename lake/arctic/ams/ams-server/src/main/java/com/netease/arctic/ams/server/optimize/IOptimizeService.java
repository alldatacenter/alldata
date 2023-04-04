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

import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OptimizeTaskStat;
import com.netease.arctic.ams.server.model.OptimizeHistory;
import com.netease.arctic.table.TableIdentifier;

import java.util.List;

public interface IOptimizeService {

  boolean isInited();

  /**
   * Check optimize check tasks, add tasks of new tables, and clean tasks of removed table.
   */
  void checkOptimizeCheckTasks(long checkInterval);

  /**
   * List cached tables in OptimizeService.
   *
   * @return table id list
   */
  List<TableIdentifier> listCachedTables();

  /**
   * List and refresh cached tables in OptimizeService.
   *
   * @return table id list
   */
  List<TableIdentifier> refreshAndListTables();

  /**
   * Get TableOptimizeItem in OptimizeService.
   *
   * @param tableIdentifier table id
   * @return ArcticTableItem
   * @throws NoSuchObjectException if table not exists
   */
  TableOptimizeItem getTableOptimizeItem(TableIdentifier tableIdentifier) throws NoSuchObjectException;

  /**
   * Handle OptimizeTask execute result, success or failed.
   *
   * @param optimizeTaskStat -
   * @throws NoSuchObjectException if table not exists
   */
  void handleOptimizeResult(OptimizeTaskStat optimizeTaskStat) throws NoSuchObjectException;

  /**
   * Get optimize history of an ArcticTable.
   *
   * @param tableIdentifier -
   * @return list of OptimizeHistory
   */
  List<OptimizeHistory> getOptimizeHistory(TableIdentifier tableIdentifier);

  /**
   * Get the latest commit time of a table
   * @param identifier
   * @return
   */
  Long getLatestCommitTime(TableIdentifier identifier);

  /**
   * Get max optimize history id.
   * @return max optimize history id
   */
  long maxOptimizeHistoryId();

  /**
   * Trigger table to commit, async.
   * @param tableOptimizeItem -
   * @return return true if trigger success
   */
  boolean triggerOptimizeCommit(TableOptimizeItem tableOptimizeItem);

  /**
   * Take Table to commit, wait if no table is ready.
   * @return TableOptimizeItem -
   */
  TableOptimizeItem takeTableToCommit() throws InterruptedException;

  /**
   * Expire and clean optimize history record
   * @param tableIdentifier -
   * @param expireTime min timestamp which record need to retain
   */
  void expireOptimizeHistory(TableIdentifier tableIdentifier, long expireTime);

  /**
   * Add new table into cache
   * @param toAddTable -
   */
  void addNewTable(TableIdentifier toAddTable);

  /**
   * Clear removed table from cache
   * @param toRemoveTable -
   */
  void clearRemovedTable(TableIdentifier toRemoveTable);

  /**
   * Start table optimize
   * @param tableIdentifier -
   */
  void startOptimize(TableIdentifier tableIdentifier) throws NoSuchObjectException;

  /**
   * Stop table optimize
   * @param tableIdentifier -
   */
  void stopOptimize(TableIdentifier tableIdentifier) throws NoSuchObjectException;
}
