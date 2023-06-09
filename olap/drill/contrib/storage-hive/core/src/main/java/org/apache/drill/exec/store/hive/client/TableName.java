/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.hive.client;

import java.util.Objects;

/**
 * Combination of database and table names used
 * to represent key for getting table data from cache.
 */
final class TableName {

  private final String dbName;
  private final String tableName;

  private TableName(String dbName, String tableName) {
    this.dbName = dbName;
    this.tableName = tableName;
  }

  public static TableName of(String dbName, String tableName) {
    return new TableName(dbName, tableName);
  }

  public String getDbName() {
    return dbName;
  }

  public String getTableName() {
    return tableName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TableName)) {
      return false;
    }

    TableName other = (TableName) o;
    return Objects.equals(dbName, other.dbName)
        && Objects.equals(tableName, other.tableName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(dbName, tableName);
  }

  @Override
  public String toString() {
    return String.format("dbName:%s, tableName:%s", dbName, tableName);
  }

}
