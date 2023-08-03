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

import com.netease.arctic.spark.reader.SparkScanBuilder;
import com.netease.arctic.table.BasicUnkeyedTable;
import com.netease.arctic.table.MetadataColumns;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableSet;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.spark.source.SparkTable;
import org.apache.iceberg.types.Types;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.connector.catalog.TableCapability;
import org.apache.spark.sql.connector.read.ScanBuilder;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ArcticSparkChangeTable extends SparkTable {

  private final BasicUnkeyedTable basicUnkeyedTable;

  private SparkSession lazySpark = null;

  private static final Set<TableCapability> CAPABILITIES = ImmutableSet.of(
      TableCapability.BATCH_READ
  );

  public ArcticSparkChangeTable(BasicUnkeyedTable basicUnkeyedTable, boolean refreshEagerly) {
    super(basicUnkeyedTable, refreshEagerly);
    this.basicUnkeyedTable = basicUnkeyedTable;
  }

  private SparkSession sparkSession() {
    if (lazySpark == null) {
      this.lazySpark = SparkSession.active();
    }

    return lazySpark;
  }

  public Set<TableCapability> capabilities() {
    return CAPABILITIES;
  }

  @Override
  public ScanBuilder newScanBuilder(CaseInsensitiveStringMap options) {
    return new SparkScanBuilder(sparkSession(), basicUnkeyedTable, options, buildSchema(basicUnkeyedTable));
  }

  public Schema buildSchema(BasicUnkeyedTable table) {
    Schema schema = table.schema();
    List<Types.NestedField> columns = schema.columns().stream().collect(Collectors.toList());
    columns.add(MetadataColumns.TRANSACTION_ID_FILED);
    columns.add(MetadataColumns.FILE_OFFSET_FILED);
    columns.add(MetadataColumns.CHANGE_ACTION_FIELD);
    return new Schema(columns);
  }

  @Override
  public StructType schema() {
    return SparkSchemaUtil.convert(buildSchema(basicUnkeyedTable));
  }
}
