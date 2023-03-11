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

package com.netease.arctic.table;

import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import java.io.Serializable;
import java.util.Objects;

/**
 * Unique table identifier, consist of catalog, database, table name
 */
public class TableIdentifier implements Serializable {

  private String catalog;

  private String database;

  private String tableName;

  public TableIdentifier() {
  }

  private TableIdentifier(String catalog, String database, String tableName) {
    this.catalog = Preconditions.checkNotNull(catalog, "Catalog name must not be null.");
    this.database = Preconditions.checkNotNull(database, "Database name must not be null.");
    this.tableName = Preconditions.checkNotNull(tableName, "Table name must not be null.");
  }

  public static TableIdentifier of(String catalog, String database, String tableName) {
    return new TableIdentifier(catalog, database, tableName);
  }

  public static TableIdentifier of(com.netease.arctic.ams.api.TableIdentifier identifier) {
    return new TableIdentifier(identifier.getCatalog(), identifier.getDatabase(), identifier.getTableName());
  }

  public TableIdentifier(com.netease.arctic.ams.api.TableIdentifier tableIdentifier) {
    this(tableIdentifier.getCatalog(), tableIdentifier.getDatabase(), tableIdentifier.getTableName());
  }

  public com.netease.arctic.ams.api.TableIdentifier buildTableIdentifier() {
    return new com.netease.arctic.ams.api.TableIdentifier(
        catalog, database, tableName
    );
  }

  public String getCatalog() {
    return catalog;
  }

  public String getDatabase() {
    return database;
  }

  public String getTableName() {
    return tableName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    TableIdentifier that = (TableIdentifier) o;

    if (!Objects.equals(catalog, that.catalog)) {
      return false;
    }
    if (!Objects.equals(database, that.database)) {
      return false;
    }
    return Objects.equals(tableName, that.tableName);
  }

  @Override
  public int hashCode() {
    int result = catalog != null ? catalog.hashCode() : 0;
    result = 31 * result + (database != null ? database.hashCode() : 0);
    result = 31 * result + (tableName != null ? tableName.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return String.format("%s.%s.%s", catalog, database, tableName);
  }

  public void setCatalog(String catalog) {
    this.catalog = catalog;
  }

  public void setDatabase(String database) {
    this.database = database;
  }

  public void setTableName(String tableName) {
    this.tableName = tableName;
  }

  public TableIdentifier toLowCaseIdentifier() {
    return new TableIdentifier(catalog, database.toLowerCase(), tableName.toLowerCase());
  }
}
