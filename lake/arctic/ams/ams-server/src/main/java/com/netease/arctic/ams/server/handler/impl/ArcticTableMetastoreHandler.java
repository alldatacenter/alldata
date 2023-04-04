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

package com.netease.arctic.ams.server.handler.impl;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.AlreadyExistsException;
import com.netease.arctic.ams.api.ArcticTableMetastore;
import com.netease.arctic.ams.api.BlockableOperation;
import com.netease.arctic.ams.api.Blocker;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.NoSuchObjectException;
import com.netease.arctic.ams.api.NotSupportedException;
import com.netease.arctic.ams.api.OperationConflictException;
import com.netease.arctic.ams.api.OperationErrorException;
import com.netease.arctic.ams.api.TableCommitMeta;
import com.netease.arctic.ams.api.TableIdentifier;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.server.model.TableBlocker;
import com.netease.arctic.ams.server.model.TableMetadata;
import com.netease.arctic.ams.server.service.IMetaService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.CatalogMetadataService;
import com.netease.arctic.ams.server.service.impl.DDLTracerService;
import com.netease.arctic.ams.server.service.impl.FileInfoCacheService;
import com.netease.arctic.ams.server.utils.ArcticMetaValidator;
import org.apache.commons.collections.CollectionUtils;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ArcticTableMetastoreHandler implements AmsClient, ArcticTableMetastore.Iface {
  public static final Logger LOG = LoggerFactory.getLogger(ArcticTableMetastoreHandler.class);

  private final IMetaService metaService;
  private final CatalogMetadataService catalogMetadataService;
  private final FileInfoCacheService fileInfoCacheService;
  private final DDLTracerService ddlTracerService;

  public ArcticTableMetastoreHandler(IMetaService metaService) {
    this.metaService = metaService;
    this.catalogMetadataService = ServiceContainer.getCatalogMetadataService();
    this.fileInfoCacheService = ServiceContainer.getFileInfoCacheService();
    this.ddlTracerService = ServiceContainer.getDdlTracerService();
  }

  @Override
  public void ping() throws TException {

  }

  @Override
  public List<CatalogMeta> getCatalogs() {
    return catalogMetadataService.getCatalogs();
  }

  @Override
  public CatalogMeta getCatalog(String name) throws TException {
    Optional<CatalogMeta> c = catalogMetadataService.getCatalog(name);
    if (!c.isPresent()) {
      throw new NoSuchObjectException("can't find catalog with name: " + name);
    }
    return c.get();
  }

  @Override
  public List<String> getDatabases(String catalogName) throws TException {
    getCatalog(catalogName);
    return metaService.listDatabases(catalogName);
  }

  @Override
  public void createDatabase(String catalogName, String database) throws TException {
    LOG.info("handle create database: {}.{}", catalogName, database);
    Optional<CatalogMeta> c = catalogMetadataService.getCatalog(catalogName);
    if (!c.isPresent()) {
      throw new NoSuchObjectException("can't find catalog with name: " + catalogName);
    }
    if (metaService.listDatabases(catalogName).contains(database)) {
      throw new AlreadyExistsException("database already exist in catalog" + catalogName);
    }
    metaService.createDatabase(catalogName, database);
  }

  @Override
  public void dropDatabase(String catalogName, String database) throws TException {
    LOG.info("handle drop database: {}.{}", catalogName, database);
    Optional<CatalogMeta> c = catalogMetadataService.getCatalog(catalogName);
    if (!c.isPresent()) {
      throw new NoSuchObjectException("can't find catalog with name: " + catalogName);
    }
    if (CollectionUtils.isNotEmpty(listTables(catalogName, database))) {
      throw new NotSupportedException("can't drop database when there exist table in database");
    }
    metaService.dropDatabase(catalogName, database);
  }

  @Override
  public void createTableMeta(TableMeta tableMeta)
      throws TException {
    LOG.info("handle create table meta: {}", tableMeta);
    if (tableMeta == null) {
      throw new NoSuchObjectException("table meta should not be null");
    }
    ArcticMetaValidator.metaValidator(tableMeta.getTableIdentifier());
    CatalogMeta catalogMeta = getCatalog(tableMeta.getTableIdentifier().getCatalog());

    com.netease.arctic.table.TableIdentifier tableIdentifier =
        new com.netease.arctic.table.TableIdentifier(tableMeta.getTableIdentifier());
    ArcticMetaValidator.alreadyExistValidator(metaService, tableIdentifier);
    TableMetadata metadata = new TableMetadata(tableMeta, catalogMeta);
    metaService.createTable(metadata);
  }

  @Override
  public List<TableMeta> listTables(String catalogName, String database) throws NoSuchObjectException, TException {
    LOG.debug("ams start load tables");
    long start = System.currentTimeMillis();
    List<TableMetadata> metadataList = metaService.getTables(catalogName, database);
    List<TableMeta> collect = metadataList.stream()
        .map(TableMetadata::buildTableMeta)
        .collect(Collectors.toList());
    LOG.debug("finish load and build arctic table meta {}, cost {} ms", metadataList.size(),
        System.currentTimeMillis() - start);
    return collect;
  }

  @Override
  public TableMeta getTable(TableIdentifier tableIdentifier) throws TException {
    if (tableIdentifier == null) {
      throw new NoSuchObjectException("table identifier should not be null");
    }
    TableMetadata metadata = metaService.loadTableMetadata(
        com.netease.arctic.table.TableIdentifier.of(tableIdentifier));
    if (metadata == null) {
      throw new NoSuchObjectException();
    }
    return metadata.buildTableMeta();
  }

  @Override
  public void removeTable(TableIdentifier tableIdentifier, boolean deleteData)
      throws TException {
    LOG.info("handle remove table {}", tableIdentifier);
    if (tableIdentifier == null) {
      throw new NoSuchObjectException("table identifier should not be null");
    }
    com.netease.arctic.table.TableIdentifier identifier =
        new com.netease.arctic.table.TableIdentifier(tableIdentifier);
    ArcticMetaValidator.nuSuchObjectValidator(metaService, identifier);

    metaService.dropTableMetadata(
        identifier,
        null,
        false);
  }

  @Override
  public void tableCommit(TableCommitMeta commit) throws TException {
    LOG.info("handle table commit {}", commit);
    if (commit == null) {
      throw new NoSuchObjectException("table commit meta should not be null");
    }
    ArcticMetaValidator.metaValidator(commit.getTableIdentifier());
    com.netease.arctic.table.TableIdentifier identifier =
        com.netease.arctic.table.TableIdentifier.of(commit.getTableIdentifier());
    if (commit.getProperties() != null) {
      metaService.updateTableProperties(identifier, commit.getProperties());
    }
    if (commit.getSchemaUpdateMeta() != null) {
      ddlTracerService.commit(commit.getTableIdentifier(), commit.getSchemaUpdateMeta());
    }
    try {
      fileInfoCacheService.commitCacheFileInfo(commit);
    } catch (Exception e) {
      LOG.warn("commit file cache failed", e);
    }
  }

  @Override
  public long allocateTransactionId(TableIdentifier tableIdentifier, String transactionSignature) throws TException {
    LOG.info("handle allocate transaction id {}", tableIdentifier);
    if (tableIdentifier == null) {
      throw new NoSuchObjectException("table identifier should not be null");
    }
    return ServiceContainer.getArcticTransactionService().allocateTransactionId(
        tableIdentifier,
        transactionSignature);
  }

  @Override
  public Blocker block(
      TableIdentifier tableIdentifier, List<BlockableOperation> operations,
      Map<String, String> properties)
      throws OperationConflictException {
    TableBlocker block = ServiceContainer.getTableBlockerService()
        .block(com.netease.arctic.table.TableIdentifier.of(tableIdentifier), operations, properties);
    return block.buildBlocker();
  }

  @Override
  public void releaseBlocker(TableIdentifier tableIdentifier, String blockerId) {
    ServiceContainer.getTableBlockerService()
        .release(com.netease.arctic.table.TableIdentifier.of(tableIdentifier), blockerId);
  }

  @Override
  public long renewBlocker(TableIdentifier tableIdentifier, String blockerId) throws NoSuchObjectException {
    return ServiceContainer.getTableBlockerService()
        .renew(com.netease.arctic.table.TableIdentifier.of(tableIdentifier), blockerId);
  }

  @Override
  public List<Blocker> getBlockers(TableIdentifier tableIdentifier) {
    return ServiceContainer.getTableBlockerService()
        .getBlockers(com.netease.arctic.table.TableIdentifier.of(tableIdentifier))
        .stream().map(TableBlocker::buildBlocker).collect(Collectors.toList());
  }

  @Override
  public void refreshTable(TableIdentifier tableIdentifier) throws OperationErrorException, TException {
    try {
      ServiceContainer.getFileInfoCacheService()
          .deleteTableCache(com.netease.arctic.table.TableIdentifier.of(tableIdentifier));
    } catch (Exception e) {
      throw new OperationErrorException(e.getMessage());
    }
  }
}
