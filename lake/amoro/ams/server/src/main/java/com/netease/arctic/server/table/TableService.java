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

package com.netease.arctic.server.table;

import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.Blocker;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.server.catalog.CatalogService;

import java.util.List;
import java.util.Map;

public interface TableService extends CatalogService, TableManager {

  void initialize();

  /**
   * create table metadata
   *
   * @param tableMeta table metadata info
   */
  void createTable(String catalogName, TableMetadata tableMeta);

  /**
   * load the table metadata
   *
   * @param tableIdentifier table id
   * @return table metadata info
   */
  TableMetadata loadTableMetadata(TableIdentifier tableIdentifier);

  /**
   * delete the table metadata
   *
   * @param tableIdentifier table id
   * @param deleteData      if delete the external table
   */
  void dropTableMetadata(TableIdentifier tableIdentifier, boolean deleteData);

  /**
   * load arctic databases name
   *
   * @return databases name list
   */
  List<String> listDatabases(String catalogName);

  /**
   * Load all managed tables.
   * Managed tables means the tables which are managed by AMS, AMS will watch their change and make them health.
   *
   * @return {@link ServerTableIdentifier} list
   */
  List<ServerTableIdentifier> listManagedTables();

  /**
   * Load all managed tables.
   * Managed tables means the tables which are managed by AMS, AMS will watch their change and make them health.
   *
   * @return {@link ServerTableIdentifier} list
   */
  List<ServerTableIdentifier> listManagedTables(String catalogName);

  /**
   * Load table identifiers by server catalog
   *
   * @return {@link TableIdentifier} list
   */
  List<TableIdentifier> listTables(String catalogName, String dbName);

  /**
   * create arctic database
   */
  void createDatabase(String catalogName, String dbName);

  /**
   * drop arctic database
   */
  void dropDatabase(String catalogName, String dbName);

  /**
   * load all arctic table metadata
   *
   * @return table metadata list
   */
  List<TableMetadata> listTableMetas();

  /**
   * load arctic table metadata
   *
   * @return table metadata list
   */
  List<TableMetadata> listTableMetas(String catalogName, String database);

  /**
   * check the table is existed
   *
   * @return True when the table is existed.
   */
  boolean tableExist(TableIdentifier tableIdentifier);

  /**
   * blocker operations
   *
   * @return the created blocker
   */
  Blocker block(TableIdentifier tableIdentifier, List<BlockableOperation> operations, Map<String, String> properties);

  /**
   * release the blocker
   */
  void releaseBlocker(TableIdentifier tableIdentifier, String blockerId);

  /**
   * renew the blocker
   *
   * @return expiration time
   */
  long renewBlocker(TableIdentifier tableIdentifier, String blockerId);

  /**
   * getRuntime blockers of table
   *
   * @return block list
   */
  List<Blocker> getBlockers(TableIdentifier tableIdentifier);
}
