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

package com.netease.arctic.optimizer.operator.executor;

import com.netease.arctic.table.TableIdentifier;

import java.io.Serializable;
import java.util.Objects;

public class TableIdentificationInfo implements Serializable {

  private final String amsUrl;
  private TableIdentifier tableIdentifier;

  public TableIdentificationInfo(String amsUrl, TableIdentifier tableIdentifier) {
    this.amsUrl = amsUrl;
    this.tableIdentifier = tableIdentifier;
  }

  public String getAmsUrl() {
    return amsUrl;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  @Override
  public int hashCode() {
    return Objects.hash(amsUrl, tableIdentifier);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TableIdentificationInfo tableIdentificationInfo = (TableIdentificationInfo) o;
    return amsUrl.equals(tableIdentificationInfo.getAmsUrl()) &&
        tableIdentifier.equals(tableIdentificationInfo.getTableIdentifier());
  }

  @Override
  public String toString() {
    return String.format("TableIdentificationInfo(%s,%s)", amsUrl, tableIdentifier);
  }
}
