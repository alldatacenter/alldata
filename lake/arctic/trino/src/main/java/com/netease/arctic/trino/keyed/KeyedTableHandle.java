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

package com.netease.arctic.trino.keyed;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.trino.util.ObjectSerializerUtil;
import io.trino.plugin.iceberg.IcebergColumnHandle;
import io.trino.plugin.iceberg.IcebergTableHandle;
import io.trino.spi.connector.ConnectorTableHandle;

import java.util.Set;

/**
 * ConnectorTableHandle for Keyed Table, beside contain primary beside IcebergTableHandle
 */
public class KeyedTableHandle implements ConnectorTableHandle {

  private IcebergTableHandle icebergTableHandle;

  private transient PrimaryKeySpec primaryKeySpec;

  private byte[] primaryKeySpecBytes;

  @JsonCreator
  public KeyedTableHandle(
      @JsonProperty("icebergTableHandle") IcebergTableHandle icebergTableHandle,
      @JsonProperty("primaryKeySpecBytes") byte[] primaryKeySpecBytes) {
    this.icebergTableHandle = icebergTableHandle;
    this.primaryKeySpecBytes = primaryKeySpecBytes;
  }

  @JsonProperty
  public IcebergTableHandle getIcebergTableHandle() {
    return icebergTableHandle;
  }

  @JsonProperty
  public byte[] getPrimaryKeySpecBytes() {
    return primaryKeySpecBytes;
  }

  public PrimaryKeySpec getPrimaryKeySpec() {
    if (primaryKeySpec == null) {
      this.primaryKeySpec = ObjectSerializerUtil.read(primaryKeySpecBytes, PrimaryKeySpec.class);
    }
    return primaryKeySpec;
  }

  public KeyedTableHandle withProjectedColumns(Set<IcebergColumnHandle> projectedColumns) {
    IcebergTableHandle newIcebergTableHandle = icebergTableHandle.withProjectedColumns(projectedColumns);
    return new KeyedTableHandle(newIcebergTableHandle, primaryKeySpecBytes);
  }
}
