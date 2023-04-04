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

package com.netease.arctic.spark;

import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.spark.sql.catalyst.analysis.NamespaceAlreadyExistsException;
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.catalyst.analysis.TableAlreadyExistsException;
import org.apache.spark.sql.connector.catalog.CatalogExtension;
import org.apache.spark.sql.connector.catalog.CatalogPlugin;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.catalog.NamespaceChange;
import org.apache.spark.sql.connector.catalog.SupportsNamespaces;
import org.apache.spark.sql.connector.catalog.Table;
import org.apache.spark.sql.connector.catalog.TableCatalog;
import org.apache.spark.sql.connector.catalog.TableChange;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


/**
 * A Spark catalog that can also load non-Iceberg tables.
 *
 * @param <T> CatalogPlugin class to avoid casting to TableCatalog and SupportsNamespaces.
 */
public class ArcticSparkSessionCatalog<T extends TableCatalog & SupportsNamespaces>
    implements SupportsNamespaces, CatalogExtension {
  private static final Logger LOG = LoggerFactory.getLogger(ArcticSparkSessionCatalog.class);
  private static final String[] DEFAULT_NAMESPACE = new String[]{"default"};

  private String catalogName = null;
  private ArcticSparkCatalog arcticCatalog = null;
  private T sessionCatalog = null;

  private CaseInsensitiveStringMap options = null;

  /**
   * Build a {@link ArcticSparkCatalog} to be used for Iceberg operations.
   * <p>
   * The default implementation creates a new ArcticSparkCatalog with the session catalog's name and options.
   *
   * @param name    catalog name
   * @param options catalog options
   * @return a ArcticSparkCatalog to be used for Iceberg tables
   */
  protected ArcticSparkCatalog buildSparkCatalog(String name, CaseInsensitiveStringMap options) {
    ArcticSparkCatalog newCatalog = new ArcticSparkCatalog();
    newCatalog.initialize(name, options);
    return newCatalog;
  }

  @Override
  public String[] defaultNamespace() {
    return DEFAULT_NAMESPACE;
  }

  @Override
  public String[][] listNamespaces() throws NoSuchNamespaceException {
    return getSessionCatalog().listNamespaces();
  }

  @Override
  public String[][] listNamespaces(String[] namespace) throws NoSuchNamespaceException {
    return getSessionCatalog().listNamespaces(namespace);
  }

  @Override
  public Map<String, String> loadNamespaceMetadata(String[] namespace) throws NoSuchNamespaceException {
    return getSessionCatalog().loadNamespaceMetadata(namespace);
  }

  @Override
  public void createNamespace(String[] namespace, Map<String, String> metadata) throws NamespaceAlreadyExistsException {
    getSessionCatalog().createNamespace(namespace, metadata);
  }

  @Override
  public void alterNamespace(String[] namespace, NamespaceChange... changes) throws NoSuchNamespaceException {
    getSessionCatalog().alterNamespace(namespace, changes);
  }

  @Override
  public boolean dropNamespace(String[] namespace) throws NoSuchNamespaceException {
    return getSessionCatalog().dropNamespace(namespace);
  }

  @Override
  public Identifier[] listTables(String[] namespace) throws NoSuchNamespaceException {
    // delegate to the session catalog because all tables share the same namespace
    return getSessionCatalog().listTables(namespace);
  }

  @Override
  public Table loadTable(Identifier ident) throws NoSuchTableException {
    Table table = getSessionCatalog().loadTable(ident);
    if (isArcticTable(table)) {
      return getArcticCatalog().loadTable(ident);
    }
    return table;
  }

  @Override
  public Table createTable(
      Identifier ident, StructType schema, Transform[] partitions,
      Map<String, String> properties)
      throws TableAlreadyExistsException, NoSuchNamespaceException {
    String provider = properties.get("provider");
    if (useArctic(provider)) {
      return getArcticCatalog().createTable(ident, schema, partitions, properties);
    } else {
      // delegate to the session catalog
      return getSessionCatalog().createTable(ident, schema, partitions, properties);
    }
  }

  @Override
  public Table alterTable(Identifier ident, TableChange... changes) throws NoSuchTableException {
    Table table = getSessionCatalog().loadTable(ident);
    if (isArcticTable(table)) {
      return getArcticCatalog().alterTable(ident, changes);
    } else {
      return getSessionCatalog().alterTable(ident, changes);
    }
  }

  @Override
  public boolean dropTable(Identifier ident) {
    // no need to check table existence to determine which catalog to use. if a table doesn't exist then both are
    // required to return false.
    try {
      Table table = getSessionCatalog().loadTable(ident);
      if (isArcticTable(table)) {
        return getArcticCatalog().dropTable(ident) || getSessionCatalog().dropTable(ident);
      } else {
        return getSessionCatalog().dropTable(ident);
      }
    } catch (NoSuchTableException e) {
      return getSessionCatalog().dropTable(ident);
    }
  }

  @Override
  public void renameTable(Identifier from, Identifier to) throws NoSuchTableException, TableAlreadyExistsException {
    // rename is not supported by HadoopCatalog. to avoid UnsupportedOperationException for session catalog tables,
    // check table existence first to ensure that the table belongs to the Iceberg catalog.
    Table table = getSessionCatalog().loadTable(from);
    if (isArcticTable(table)) {
      getArcticCatalog().renameTable(from, to);
    } else {
      getSessionCatalog().renameTable(from, to);
    }
  }

  @Override
  public final void initialize(String name, CaseInsensitiveStringMap options) {
    this.catalogName = name;
    this.options = options;
    try {
      this.arcticCatalog = buildSparkCatalog(name, options);
    } catch (Exception e) {
      this.arcticCatalog = null;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public void setDelegateCatalog(CatalogPlugin sparkSessionCatalog) {
    if (sparkSessionCatalog instanceof TableCatalog) {
      this.sessionCatalog = (T) sparkSessionCatalog;
    } else {
      throw new IllegalArgumentException("Invalid session catalog: " + sparkSessionCatalog);
    }
  }

  @Override
  public String name() {
    return catalogName;
  }

  private boolean useArctic(String provider) {
    if ("arctic".equalsIgnoreCase(provider)) {
      return true;
    }
    return false;
  }

  private T getSessionCatalog() {
    Preconditions.checkNotNull(sessionCatalog, "Delegated SessionCatalog is missing. " +
        "Please make sure your are replacing Spark's default catalog, named 'spark_catalog'.");
    return sessionCatalog;
  }

  private ArcticSparkCatalog getArcticCatalog() {
    if (arcticCatalog == null) {
      this.arcticCatalog = buildSparkCatalog(this.catalogName, this.options);
    }
    return this.arcticCatalog;

  }

  private boolean isArcticTable(Table table) {
    return table.properties().containsKey("arctic.enabled") &&
        table.properties().get("arctic.enabled").equals("true");
  }
}
