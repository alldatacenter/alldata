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

package com.netease.arctic.ams.server.service;

import com.netease.arctic.ams.api.MetaException;
import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.table.TableIdentifier;

import java.util.List;
import java.util.Map;

public interface IMetaService {
  /**
   * create table metadata
   * @param tableMeta   table metadata info
   * @throws MetaException when the table metadata is not match
   */
  void createTable(TableMetadata tableMeta) throws MetaException;

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
   * @param tableIdentifier     table id
   * @param internalTableService external data service
   * @param deleteData          if delete the external table
   * @throws MetaException when table metadata is not match
   */
  void dropTableMetadata(TableIdentifier tableIdentifier,
                         IInternalTableService internalTableService,
                         boolean deleteData) throws MetaException;

  /**
   * update the arctic table properties
   *
   * @param tableIdentifier table id
   * @param properties      arctic table properties
   */
  void updateTableProperties(TableIdentifier tableIdentifier, Map<String, String> properties);

  /**
   * load arctic databases name
   *
   * @return databases name list
   */
  List<String> listDatabases(String catalogName);

  /**
   * create arctic database
   *
   */
  void createDatabase(String catalogName, String dbName);

  /**
   * drop arctic database
   *
   */
  void dropDatabase(String catalogName, String dbName);

  /**
   * load all arctic table metadata
   *
   * @return table metadata list
   */
  List<TableMetadata> listTables();

  /**
   * load arctic table metadata
   *
   * @return table metadata list
   */
  List<TableMetadata> getTables(String catalogName, String database);

  /**
   * check the table is existed
   *
   * @param tableIdentifier table id
   * @return True when the table is existed.
   */
  boolean isExist(TableIdentifier tableIdentifier);

  /**
   * get talbe count in catalog catalogName
   * @param catalogName
   * @return
   */
  Integer getTableCountInCatalog(String catalogName);

}
