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

package com.netease.arctic.spark.table;

import com.netease.arctic.table.UnkeyedTable;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.spark.source.SparkTable;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.analysis.NoSuchPartitionException;
import org.apache.spark.sql.catalyst.analysis.PartitionAlreadyExistsException;
import org.apache.spark.sql.connector.catalog.SupportsPartitionManagement;
import org.apache.spark.sql.types.StructType;

import java.util.Map;

public class ArcticIcebergSparkTable extends SparkTable implements SupportsPartitionManagement {
  private final UnkeyedTable unkeyedTable;

  public ArcticIcebergSparkTable(UnkeyedTable unkeyedTable, boolean refreshEagerly) {
    super(unkeyedTable, refreshEagerly);
    this.unkeyedTable = unkeyedTable;
  }

  @Override
  public UnkeyedTable table() {
    return unkeyedTable;
  }

  @Override
  public Map<String, String> properties() {
    Map<String, String> properties = Maps.newHashMap();
    properties.putAll(super.properties());
    properties.put("provider", "arctic");
    return properties;
  }

  @Override
  public StructType partitionSchema() {
    return SparkSchemaUtil.convert(new Schema(table().spec().partitionType().fields()));
  }

  @Override
  public void createPartition(InternalRow ident, Map<String, String> properties) throws PartitionAlreadyExistsException,
      UnsupportedOperationException {
    throw new UnsupportedOperationException("not supported create partition");
  }

  @Override
  public boolean dropPartition(InternalRow ident) {
    return false;
  }

  @Override
  public void replacePartitionMetadata(InternalRow ident, Map<String, String> properties)
      throws NoSuchPartitionException, UnsupportedOperationException {
    throw new UnsupportedOperationException("not supported replace partition");
  }

  @Override
  public Map<String, String> loadPartitionMetadata(InternalRow ident) throws UnsupportedOperationException {
    return null;
  }

  @Override
  public InternalRow[] listPartitionIdentifiers(String[] names, InternalRow ident) {
    return new InternalRow[0];
  }
}
