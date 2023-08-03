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

package com.netease.arctic.trino.iceberg;

import com.netease.arctic.catalog.BasicArcticCatalog;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableMetaStore;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.BaseTable;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.SortOrder;
import org.apache.iceberg.Table;
import org.apache.iceberg.Transaction;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.hadoop.HadoopCatalog;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestBasicArcticCatalog extends BasicArcticCatalog {

  private String location;

  private HadoopCatalog catalog;

  public TestBasicArcticCatalog(String location) {
    this.location = location;
    Configuration conf = new Configuration();
    this.catalog = new HadoopCatalog(conf, location);
    //创建catalog.db，便于测试
    if (!catalog.namespaceExists(Namespace.of(catalog.name(), "tpch"))) {
      catalog.createNamespace(Namespace.of(catalog.name(), "tpch"));
    }
  }

  @Override
  public String name() {
    return catalog.name();
  }

  @Override
  public List<String> listDatabases() {
    return catalog.listNamespaces(Namespace.of(catalog.name())).stream()
        .map(s -> s.level(1)).collect(Collectors.toList());
  }

  @Override
  public List<TableIdentifier> listTables(String database) {
    return catalog.listTables(Namespace.of(catalog.name(), database))
        .stream()
        .map(s -> TableIdentifier.of(s.namespace().level(0),
            s.namespace().level(1), s.name()))
        .collect(Collectors.toList());
  }

  @Override
  public ArcticTable loadTable(TableIdentifier identifier) {
    Table table = catalog.loadTable(
        org.apache.iceberg.catalog.TableIdentifier.of(identifier.getCatalog(),
            identifier.getDatabase(), identifier.getTableName()));
    return new TestArcticTable((BaseTable) table, identifier);
  }

  @Override
  public boolean dropTable(TableIdentifier identifier, boolean purge) {
    return catalog.dropTable(org.apache.iceberg.catalog.TableIdentifier.of(identifier.getCatalog(),
        identifier.getDatabase(), identifier.getTableName()), purge);
  }

  @Override
  public TableMetaStore getTableMetaStore() {
    return TableMetaStore.EMPTY;
  }

  @Override
  public boolean tableExists(TableIdentifier tableIdentifier) {
    return catalog.tableExists(org.apache.iceberg.catalog.TableIdentifier.of(tableIdentifier.getCatalog(),
        tableIdentifier.getDatabase(), tableIdentifier.getTableName()));
  }

  @Override
  public TableBuilder newTableBuilder(TableIdentifier identifier, Schema schema) {
    return new TestTableBuilder(identifier, schema);
  }

  protected class TestTableBuilder implements TableBuilder {
    protected TableIdentifier identifier;
    protected Schema schema;
    protected PartitionSpec partitionSpec;
    protected SortOrder sortOrder;
    protected Map<String, String> properties = new HashMap<>();
    protected PrimaryKeySpec primaryKeySpec = PrimaryKeySpec.noPrimaryKey();
    protected String location;

    public TestTableBuilder(TableIdentifier identifier, Schema schema) {
      this.identifier = identifier;
      this.schema = schema;
    }

    @Override
    public TableBuilder withPartitionSpec(PartitionSpec partitionSpec) {
      this.partitionSpec = partitionSpec;
      return this;
    }

    @Override
    public TableBuilder withSortOrder(SortOrder sortOrder) {
      this.sortOrder = sortOrder;
      return this;
    }

    @Override
    public TableBuilder withProperties(Map<String, String> properties) {
      this.properties.putAll(properties);
      return this;
    }

    @Override
    public TableBuilder withProperty(String key, String value) {
      this.properties.put(key, value);
      return this;
    }

    @Override
    public TableBuilder withPrimaryKeySpec(PrimaryKeySpec primaryKeySpec) {
      this.primaryKeySpec = primaryKeySpec;
      return this;
    }

    @Override
    public ArcticTable create() {
      return null;
    }

    public Transaction newCreateTableTransaction() {
      return catalog.newCreateTableTransaction(org.apache.iceberg.catalog.TableIdentifier.of(identifier.getCatalog(),
          identifier.getDatabase(), identifier.getTableName()), schema, partitionSpec, location, properties);
    }
  }
}
