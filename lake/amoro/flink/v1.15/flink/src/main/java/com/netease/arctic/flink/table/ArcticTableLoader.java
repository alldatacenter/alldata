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

package com.netease.arctic.flink.table;

import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.flink.InternalCatalogBuilder;
import com.netease.arctic.flink.interceptor.FlinkTablePropertiesInvocationHandler;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableIdentifier;
import org.apache.iceberg.Table;
import org.apache.iceberg.flink.TableLoader;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.netease.arctic.flink.catalog.factories.ArcticCatalogFactoryOptions.METASTORE_URL;

/**
 * load a proxy table contains both arctic table properties and flink table properties
 */
public class ArcticTableLoader implements TableLoader {

  private static final long serialVersionUID = 1L;

  protected final InternalCatalogBuilder catalogBuilder;
  protected final TableIdentifier tableIdentifier;
  protected final Map<String, String> flinkTableProperties;
  /**
   * The mark of loading internal table, base or change table. For compatible with iceberg committer.
   */
  protected boolean loadBaseForKeyedTable;
  protected transient ArcticCatalog arcticCatalog;

  public static ArcticTableLoader of(TableIdentifier tableIdentifier, InternalCatalogBuilder catalogBuilder) {
    return of(tableIdentifier, catalogBuilder, new HashMap<>());
  }

  public static ArcticTableLoader of(
      TableIdentifier tableIdentifier, InternalCatalogBuilder catalogBuilder,
      Map<String, String> flinkTableProperties) {
    return new ArcticTableLoader(tableIdentifier, catalogBuilder, flinkTableProperties);
  }

  public static ArcticTableLoader of(TableIdentifier tableIdentifier, Map<String, String> flinkTableProperties) {
    String metastoreUrl = flinkTableProperties.get(METASTORE_URL.key());
    return new ArcticTableLoader(tableIdentifier,
        InternalCatalogBuilder.builder().metastoreUrl(metastoreUrl), flinkTableProperties);
  }

  public static ArcticTableLoader of(
      TableIdentifier tableIdentifier,
      String metastoreUrl,
      Map<String, String> flinkTableProperties) {
    return new ArcticTableLoader(tableIdentifier,
        InternalCatalogBuilder.builder().metastoreUrl(metastoreUrl), flinkTableProperties);
  }

  protected ArcticTableLoader(
      TableIdentifier tableIdentifier,
      InternalCatalogBuilder catalogBuilder,
      Map<String, String> flinkTableProperties) {
    this(tableIdentifier, catalogBuilder, flinkTableProperties, null);
  }

  protected ArcticTableLoader(
      TableIdentifier tableIdentifier,
      InternalCatalogBuilder catalogBuilder,
      Map<String, String> flinkTableProperties,
      Boolean loadBaseForKeyedTable) {
    this.catalogBuilder = catalogBuilder;
    this.tableIdentifier = tableIdentifier;
    this.flinkTableProperties = flinkTableProperties;
    this.loadBaseForKeyedTable = loadBaseForKeyedTable == null || loadBaseForKeyedTable;
  }

  @Override
  public void open() {
    arcticCatalog = catalogBuilder.build();
  }

  public ArcticTable loadArcticTable() {
    return ((ArcticTable) new FlinkTablePropertiesInvocationHandler(
        flinkTableProperties,
        arcticCatalog.loadTable(tableIdentifier)).getProxy());
  }

  public void switchLoadInternalTableForKeyedTable(boolean loadBaseForKeyedTable) {
    this.loadBaseForKeyedTable = loadBaseForKeyedTable;
  }

  @Override
  public Table loadTable() {
    ArcticTable table = loadArcticTable();

    if (table.isKeyedTable()) {
      if (loadBaseForKeyedTable) {
        return table.asKeyedTable().baseTable();
      } else {
        return table.asKeyedTable().changeTable();
      }
    }
    if (!(table instanceof Table)) {
      throw new UnsupportedOperationException(String.format("table type mismatched. It's %s", table.getClass()));
    }
    return (Table) table;
  }

  @Override
  public void close() throws IOException {
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("tableIdentifier", tableIdentifier)
        .toString();
  }
}