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
package org.apache.drill.exec.store.pcap.dto;

import org.apache.drill.exec.store.pcap.schema.PcapTypes;

import java.util.Objects;

public class ColumnDto {

  private final String columnName;
  private final PcapTypes columnType;

  public ColumnDto(String columnName, PcapTypes columnType) {
    this.columnName = columnName;
    this.columnType = columnType;
  }

  public String getColumnName() {
    return columnName;
  }

  public PcapTypes getColumnType() {
    return columnType;
  }

  public boolean isNullable() {
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ColumnDto columnDto = (ColumnDto) o;
    return Objects.equals(columnName, columnDto.columnName) &&
        columnType == columnDto.columnType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(columnName, columnType);
  }
}
