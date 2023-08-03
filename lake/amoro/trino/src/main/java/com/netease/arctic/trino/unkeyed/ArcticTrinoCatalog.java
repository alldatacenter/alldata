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

package com.netease.arctic.trino.unkeyed;

import com.google.common.collect.ImmutableList;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import io.trino.plugin.hive.util.HiveUtil;
import io.trino.plugin.iceberg.ColumnIdentity;
import io.trino.plugin.iceberg.catalog.TrinoCatalog;
import io.trino.spi.TrinoException;
import io.trino.spi.connector.CatalogSchemaTableName;
import io.trino.spi.connector.ConnectorMaterializedViewDefinition;
import io.trino.spi.connector.ConnectorSession;
import io.trino.spi.connector.ConnectorViewDefinition;
import io.trino.spi.connector.SchemaTableName;
import io.trino.spi.security.TrinoPrincipal;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.trino.plugin.hive.util.HiveUtil.isHiveSystemSchema;
import static io.trino.spi.StandardErrorCode.NOT_SUPPORTED;
import static java.util.Locale.ENGLISH;

/**
 * A TrinoCatalog for Arctic, this is in order to reuse iceberg code
 */
public class ArcticTrinoCatalog implements TrinoCatalog {

  private ArcticCatalog arcticCatalog;

  public ArcticTrinoCatalog(ArcticCatalog arcticCatalog) {
    this.arcticCatalog = arcticCatalog;
  }

  @Override
  public boolean namespaceExists(ConnectorSession session, String namespace) {
    if (!namespace.equals(namespace.toLowerCase(ENGLISH))) {
      // Currently, Trino schemas are always lowercase, so this one cannot exist (https://github.com/trinodb/trino/issues/17)
      return false;
    }
    if (HiveUtil.isHiveSystemSchema(namespace)) {
      return false;
    }
    return listNamespaces(session).contains(namespace);
  }

  @Override
  public List<String> listNamespaces(ConnectorSession session) {
    return arcticCatalog.listDatabases();
  }

  private List<String> listNamespaces(ConnectorSession session, Optional<String> namespace) {
    if (namespace.isPresent()) {
      if (isHiveSystemSchema(namespace.get())) {
        return ImmutableList.of();
      }
      return ImmutableList.of(namespace.get());
    }
    return listNamespaces(session);
  }

  @Override
  public void dropNamespace(ConnectorSession session, String namespace) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported drop db");
  }

  @Override
  public Map<String, Object> loadNamespaceMetadata(ConnectorSession session, String namespace) {
    return Collections.EMPTY_MAP;
  }

  @Override
  public Optional<TrinoPrincipal> getNamespacePrincipal(ConnectorSession session, String namespace) {
    return Optional.empty();
  }

  @Override
  public void createNamespace(
      ConnectorSession session,
      String namespace,
      Map<String, Object> properties,
      TrinoPrincipal owner) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported drop db");
  }

  @Override
  public void setNamespacePrincipal(ConnectorSession session, String namespace, TrinoPrincipal principal) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported drop db");
  }

  @Override
  public void renameNamespace(ConnectorSession session, String source, String target) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported drop db");
  }

  @Override
  public List<SchemaTableName> listTables(ConnectorSession session, Optional<String> namespace) {
    return listNamespaces(session, namespace)
        .stream()
        .flatMap(s -> arcticCatalog.listTables(s).stream())
        .map(s -> new SchemaTableName(s.getDatabase(), s.getTableName()))
        .collect(Collectors.toList());
  }

  @Override
  public Transaction newCreateTableTransaction(
      ConnectorSession session, SchemaTableName schemaTableName,
      Schema schema, PartitionSpec partitionSpec,
      String location, Map<String, String> properties) {
    return arcticCatalog.newTableBuilder(getTableIdentifier(schemaTableName), schema)
            .withPartitionSpec(partitionSpec)
            .withProperties(properties).newCreateTableTransaction();
  }

  @Override
  public void registerTable(
      ConnectorSession session,
      SchemaTableName tableName,
      String tableLocation,
      String metadataLocation) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported create table");
  }

  @Override
  public void dropTable(ConnectorSession session, SchemaTableName schemaTableName) {
    arcticCatalog.dropTable(TableIdentifier.of(arcticCatalog.name(),
        schemaTableName.getSchemaName(), schemaTableName.getTableName()), true);
  }

  @Override
  public void renameTable(ConnectorSession session, SchemaTableName from, SchemaTableName to) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported rename db");
  }

  @Override
  public Table loadTable(ConnectorSession session, SchemaTableName schemaTableName) {
    ArcticTable arcticTable = arcticCatalog.loadTable(getTableIdentifier(schemaTableName));
    if (arcticTable instanceof Table) {
      return (Table) arcticTable;
    }
    throw new TrinoException(NOT_SUPPORTED, "table is not standard no key table");
  }

  @Override
  public void updateTableComment(ConnectorSession session, SchemaTableName schemaTableName, Optional<String> comment) {
    throw new TrinoException(NOT_SUPPORTED, "UnSupport update table comment");
  }

  @Override
  public void updateViewComment(ConnectorSession session, SchemaTableName schemaViewName, Optional<String> comment) {
    throw new TrinoException(NOT_SUPPORTED, "UnSupport update table comment");
  }

  @Override
  public void updateViewColumnComment(
      ConnectorSession session,
      SchemaTableName schemaViewName,
      String columnName, Optional<String> comment) {
    throw new TrinoException(NOT_SUPPORTED, "UnSupport update table comment");
  }

  @Override
  public String defaultTableLocation(ConnectorSession session, SchemaTableName schemaTableName) {
    //不会使用
    return null;
  }

  @Override
  public void setTablePrincipal(ConnectorSession session, SchemaTableName schemaTableName, TrinoPrincipal principal) {
    throw new TrinoException(NOT_SUPPORTED, "UnSupport set table principal");
  }

  @Override
  public void createView(
      ConnectorSession session,
      SchemaTableName schemaViewName,
      ConnectorViewDefinition definition,
      boolean replace) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public void renameView(ConnectorSession session, SchemaTableName source, SchemaTableName target) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public void setViewPrincipal(ConnectorSession session, SchemaTableName schemaViewName, TrinoPrincipal principal) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public void dropView(ConnectorSession session, SchemaTableName schemaViewName) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public List<SchemaTableName> listViews(ConnectorSession session, Optional<String> namespace) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public Map<SchemaTableName, ConnectorViewDefinition> getViews(ConnectorSession session, Optional<String> namespace) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public Optional<ConnectorViewDefinition> getView(ConnectorSession session, SchemaTableName viewIdentifier) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public List<SchemaTableName> listMaterializedViews(ConnectorSession session, Optional<String> namespace) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public void createMaterializedView(
      ConnectorSession session,
      SchemaTableName schemaViewName,
      ConnectorMaterializedViewDefinition definition,
      boolean replace,
      boolean ignoreExisting) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public void dropMaterializedView(ConnectorSession session, SchemaTableName schemaViewName) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public Optional<ConnectorMaterializedViewDefinition> getMaterializedView(
      ConnectorSession session,
      SchemaTableName schemaViewName) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public void renameMaterializedView(ConnectorSession session, SchemaTableName source, SchemaTableName target) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported view");
  }

  @Override
  public void updateColumnComment(
      ConnectorSession session,
      SchemaTableName schemaTableName,
      ColumnIdentity columnIdentity,
      Optional<String> comment) {
    throw new TrinoException(NOT_SUPPORTED, "Unsupported update column comment");
  }

  @Override
  public Optional<CatalogSchemaTableName> redirectTable(ConnectorSession session, SchemaTableName tableName) {
    return Optional.empty();
  }

  private TableIdentifier getTableIdentifier(SchemaTableName schemaTableName) {
    return TableIdentifier.of(arcticCatalog.name(),
        schemaTableName.getSchemaName(), schemaTableName.getTableName());
  }
}
