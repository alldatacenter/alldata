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

package com.netease.arctic.catalog;

import com.netease.arctic.AmsClient;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.ArcticFileIOs;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.BasicUnkeyedTable;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableMetaStore;
import com.netease.arctic.table.blocker.BasicTableBlockerManager;
import com.netease.arctic.table.blocker.TableBlockerManager;
import com.netease.arctic.utils.CatalogUtil;
import org.apache.iceberg.CatalogProperties;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.SupportsNamespaces;
import org.apache.thrift.TException;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A wrapper class around {@link Catalog} and implement {@link ArcticCatalog}.
 */
public class BasicIcebergCatalog implements ArcticCatalog {

  private CatalogMeta meta;
  private AmsClient client;
  private Pattern databaseFilterPattern;
  private transient TableMetaStore tableMetaStore;
  private transient Catalog icebergCatalog;

  @Override
  public String name() {
    return meta.getCatalogName();
  }

  @Override
  public void initialize(
      AmsClient client, CatalogMeta meta, Map<String, String> properties) {
    this.client = client;
    this.meta = meta;
    meta.putToCatalogProperties(
        org.apache.iceberg.CatalogUtil.ICEBERG_CATALOG_TYPE,
        meta.getCatalogType());
    tableMetaStore = CatalogUtil.buildMetaStore(meta);
    if (meta.getCatalogProperties().containsKey(CatalogProperties.CATALOG_IMPL)) {
      meta.getCatalogProperties().remove("type");
    }
    icebergCatalog = tableMetaStore.doAs(() -> org.apache.iceberg.CatalogUtil.buildIcebergCatalog(name(),
        meta.getCatalogProperties(), tableMetaStore.getConfiguration()));
    if (meta.getCatalogProperties().containsKey(CatalogMetaProperties.KEY_DATABASE_FILTER_REGULAR_EXPRESSION)) {
      String databaseFilter =
          meta.getCatalogProperties().get(CatalogMetaProperties.KEY_DATABASE_FILTER_REGULAR_EXPRESSION);
      databaseFilterPattern = Pattern.compile(databaseFilter);
    }
  }

  @Override
  public List<String> listDatabases() {
    if (!(icebergCatalog instanceof SupportsNamespaces)) {
      throw new UnsupportedOperationException(String.format(
          "Iceberg catalog: %s doesn't implement SupportsNamespaces",
          icebergCatalog.getClass().getName()));
    }

    List<String> databases =
        tableMetaStore.doAs(() ->
            ((SupportsNamespaces) icebergCatalog).listNamespaces(Namespace.empty())
                .stream()
                .map(namespace -> namespace.level(0))
                .distinct()
                .collect(Collectors.toList())
        );
    return databases.stream()
        .filter(database -> databaseFilterPattern == null || databaseFilterPattern.matcher(database).matches())
        .collect(Collectors.toList());
  }

  @Override
  public void createDatabase(String databaseName) {
    if (icebergCatalog instanceof SupportsNamespaces) {
      tableMetaStore.doAs(() -> {
        ((SupportsNamespaces) icebergCatalog).createNamespace(Namespace.of(databaseName));
        return null;
      });
    } else {
      throw new UnsupportedOperationException(String.format(
          "Iceberg catalog: %s doesn't implement SupportsNamespaces",
          icebergCatalog.getClass().getName()));
    }
  }

  @Override
  public void dropDatabase(String databaseName) {
    if (icebergCatalog instanceof SupportsNamespaces) {
      tableMetaStore.doAs(() -> {
        ((SupportsNamespaces) icebergCatalog).dropNamespace(Namespace.of(databaseName));
        return null;
      });
    } else {
      throw new UnsupportedOperationException(String.format(
          "Iceberg catalog: %s doesn't implement SupportsNamespaces",
          icebergCatalog.getClass().getName()));
    }
  }

  @Override
  public List<TableIdentifier> listTables(String database) {
    return tableMetaStore.doAs(() -> icebergCatalog.listTables(Namespace.of(database)).stream()
        .filter(tableIdentifier -> tableIdentifier.namespace().levels().length == 1)
        .map(tableIdentifier -> TableIdentifier.of(name(), database, tableIdentifier.name()))
        .collect(Collectors.toList()));
  }

  @Override
  public ArcticTable loadTable(TableIdentifier tableIdentifier) {
    Table icebergTable = tableMetaStore.doAs(() -> icebergCatalog
        .loadTable(toIcebergTableIdentifier(tableIdentifier)));
    ArcticFileIO arcticFileIO = ArcticFileIOs.buildHadoopFileIO(tableMetaStore);
    return new BasicIcebergTable(tableIdentifier, CatalogUtil.useArcticTableOperations(icebergTable,
        icebergTable.location(), arcticFileIO, tableMetaStore.getConfiguration()), arcticFileIO,
        meta.getCatalogProperties());
  }

  @Override
  public void renameTable(TableIdentifier from, String newTableName) {
    tableMetaStore.doAs(() -> {
      icebergCatalog.renameTable(
          toIcebergTableIdentifier(from),
          org.apache.iceberg.catalog.TableIdentifier.of(
              Namespace.of(from.getDatabase()),
              newTableName));
      return null;
    });
  }

  @Override
  public boolean dropTable(TableIdentifier tableIdentifier, boolean purge) {
    return tableMetaStore.doAs(() -> icebergCatalog.dropTable(toIcebergTableIdentifier(tableIdentifier), purge));
  }

  @Override
  public TableBuilder newTableBuilder(
      TableIdentifier identifier, Schema schema) {
    throw new UnsupportedOperationException("unsupported new iceberg table for now.");
  }

  @Override
  public void refresh() {
    try {
      this.meta = client.getCatalog(meta.getCatalogName());
    } catch (TException e) {
      throw new IllegalStateException(String.format("failed load catalog %s.", meta.getCatalogName()), e);
    }
  }

  @Override
  public TableBlockerManager getTableBlockerManager(TableIdentifier tableIdentifier) {
    return BasicTableBlockerManager.build(tableIdentifier, client);
  }

  @Override
  public Map<String, String> properties() {
    return meta.getCatalogProperties();
  }

  private org.apache.iceberg.catalog.TableIdentifier toIcebergTableIdentifier(TableIdentifier tableIdentifier) {
    return org.apache.iceberg.catalog.TableIdentifier.of(
        Namespace.of(tableIdentifier.getDatabase()),
        tableIdentifier.getTableName());
  }

  public static class BasicIcebergTable extends BasicUnkeyedTable {

    public BasicIcebergTable(
        TableIdentifier tableIdentifier,
        Table icebergTable,
        ArcticFileIO arcticFileIO,
        Map<String, String> catalogProperties) {
      super(tableIdentifier, icebergTable, arcticFileIO, null, catalogProperties);
    }
  }
}