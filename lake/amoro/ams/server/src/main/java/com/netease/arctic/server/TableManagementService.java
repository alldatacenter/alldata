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

package com.netease.arctic.server;

import com.netease.arctic.ams.api.ArcticTableMetastore;
import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.Blocker;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.OperationConflictException;
import com.netease.arctic.ams.api.TableCommitMeta;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.server.table.ServerTableIdentifier;
import com.netease.arctic.server.table.TableMetadata;
import com.netease.arctic.server.table.TableService;
import org.apache.thrift.TException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class TableManagementService implements ArcticTableMetastore.Iface {

  private final TableService tableService;

  public TableManagementService(TableService tableService) {
    this.tableService = tableService;
  }

  @Override
  public void ping() {
  }

  @Override
  public List<CatalogMeta> getCatalogs() {
    return tableService.listCatalogMetas();
  }

  @Override
  public CatalogMeta getCatalog(String name) {
    return tableService.getCatalogMeta(name);
  }

  @Override
  public List<String> getDatabases(String catalogName) {
    return tableService.listDatabases(catalogName);
  }

  @Override
  public void createDatabase(String catalogName, String database) {
    tableService.createDatabase(catalogName, database);
  }

  @Override
  public void dropDatabase(String catalogName, String database) {
    tableService.dropDatabase(catalogName, database);
  }

  @Override
  public void createTableMeta(TableMeta tableMeta) {
    if (tableMeta == null) {
      throw new IllegalArgumentException("table meta should not be null");
    }
    ServerTableIdentifier identifier = ServerTableIdentifier.of(tableMeta.getTableIdentifier());
    CatalogMeta catalogMeta = getCatalog(identifier.getCatalog());
    TableMetadata tableMetadata = new TableMetadata(identifier, tableMeta, catalogMeta);
    tableService.createTable(tableMeta.tableIdentifier.getCatalog(), tableMetadata);
  }

  @Override
  public List<TableMeta> listTables(String catalogName, String database) {
    List<TableMetadata> tableMetadataList = tableService.listTableMetas(catalogName, database);
    return tableMetadataList.stream()
            .map(TableMetadata::buildTableMeta)
            .collect(Collectors.toList());
  }

  @Override
  public TableMeta getTable(TableIdentifier tableIdentifier) {
    return tableService.loadTableMetadata(tableIdentifier).buildTableMeta();
  }

  @Override
  public void removeTable(TableIdentifier tableIdentifier, boolean deleteData) {
    tableService.dropTableMetadata(tableIdentifier, deleteData);
  }

  @Override
  public void tableCommit(TableCommitMeta commit) {
  }

  @Override
  public long allocateTransactionId(TableIdentifier tableIdentifier, String transactionSignature) throws TException {
    throw new UnsupportedOperationException("allocate TransactionId from AMS is not supported now");
  }

  @Override
  public Blocker block(
      TableIdentifier tableIdentifier, List<BlockableOperation> operations, Map<String, String> properties)
      throws OperationConflictException {
    return tableService.block(tableIdentifier, operations, properties);
  }

  @Override
  public void releaseBlocker(TableIdentifier tableIdentifier, String blockerId) {
    tableService.releaseBlocker(tableIdentifier, blockerId);
  }

  @Override
  public long renewBlocker(TableIdentifier tableIdentifier, String blockerId) throws NoSuchObjectException {
    return tableService.renewBlocker(tableIdentifier, blockerId);
  }

  @Override
  public List<Blocker> getBlockers(TableIdentifier tableIdentifier) {
    return tableService.getBlockers(tableIdentifier);
  }
}
