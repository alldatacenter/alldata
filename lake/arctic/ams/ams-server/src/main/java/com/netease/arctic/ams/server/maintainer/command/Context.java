/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.ams.server.maintainer.command;

import com.netease.arctic.table.TableIdentifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class Context {

  private String catalog;

  private String db;

  private Map<TableIdentifier, TableAnalyzeResult> tableAvailableResultMap =
      new LinkedHashMap<TableIdentifier, TableAnalyzeResult>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<TableIdentifier, TableAnalyzeResult> eldest) {
          return size() > 10;
        }
  };

  private RepairProperty property = new RepairProperty();

  public String getCatalog() {
    return catalog;
  }

  public void setCatalog(String catalog) {
    this.catalog = catalog;
  }

  public String getDb() {
    return db;
  }

  public void setDb(String db) {
    this.db = db;
  }

  public void setTableAvailableResult(TableAnalyzeResult result) {
    tableAvailableResultMap.put(result.getIdentifier(), result);
  }

  public TableAnalyzeResult getTableAvailableResult(TableIdentifier identifier) {
    return tableAvailableResultMap.get(identifier);
  }

  public void clean(TableIdentifier identifier) {
    tableAvailableResultMap.remove(identifier);
  }

  public void setProperty(String name, String value) {
    property.set(name, value);
  }

  public Object getProperty(String name) {
    return property.get(name);
  }

  public Integer getIntProperty(String name) {
    return property.getInt(name);
  }
}
