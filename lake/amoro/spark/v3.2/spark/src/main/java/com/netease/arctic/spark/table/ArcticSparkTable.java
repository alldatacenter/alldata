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

import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.hive.table.SupportHive;
import com.netease.arctic.spark.reader.SparkScanBuilder;
import com.netease.arctic.spark.writer.ArcticSparkWriteBuilder;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableSet;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.spark.Spark3Util;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.InternalRow;
import org.apache.spark.sql.catalyst.analysis.NoSuchPartitionException;
import org.apache.spark.sql.catalyst.analysis.PartitionAlreadyExistsException;
import org.apache.spark.sql.connector.catalog.SupportsPartitionManagement;
import org.apache.spark.sql.connector.catalog.SupportsRead;
import org.apache.spark.sql.connector.catalog.SupportsWrite;
import org.apache.spark.sql.connector.catalog.Table;
import org.apache.spark.sql.connector.catalog.TableCapability;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.connector.read.ScanBuilder;
import org.apache.spark.sql.connector.write.LogicalWriteInfo;
import org.apache.spark.sql.connector.write.WriteBuilder;
import org.apache.spark.sql.types.StructType;
import org.apache.spark.sql.util.CaseInsensitiveStringMap;

import java.util.Map;
import java.util.Set;

public class ArcticSparkTable implements Table, SupportsRead, SupportsWrite,
    SupportsRowLevelOperator, SupportsPartitionManagement {
  private static final Set<String> RESERVED_PROPERTIES = Sets.newHashSet("provider", "format", "current-snapshot-id");
  private static final Set<TableCapability> CAPABILITIES = ImmutableSet.of(
      TableCapability.BATCH_READ,
      TableCapability.BATCH_WRITE,
      TableCapability.STREAMING_WRITE,
      TableCapability.OVERWRITE_BY_FILTER,
      TableCapability.OVERWRITE_DYNAMIC);

  private final ArcticTable arcticTable;
  private final StructType requestedSchema;
  private final boolean refreshEagerly;
  private StructType lazyTableSchema = null;
  private SparkSession lazySpark = null;
  private final ArcticCatalog catalog;

  public static Table ofArcticTable(ArcticTable table, ArcticCatalog catalog) {
    if (table.isUnkeyedTable()) {
      if (!(table instanceof SupportHive)) {
        return new ArcticIcebergSparkTable(table.asUnkeyedTable(), false);
      }
    }
    return new ArcticSparkTable(table, false, catalog);
  }

  public ArcticSparkTable(ArcticTable arcticTable, boolean refreshEagerly, ArcticCatalog catalog) {
    this(arcticTable, null, refreshEagerly, catalog);
  }

  public ArcticSparkTable(ArcticTable arcticTable,
                          StructType requestedSchema,
                          boolean refreshEagerly,
                          ArcticCatalog catalog) {
    this.arcticTable = arcticTable;
    this.requestedSchema = requestedSchema;
    this.refreshEagerly = refreshEagerly;
    this.catalog = catalog;

    if (requestedSchema != null) {
      // convert the requested schema to throw an exception if any requested fields are unknown
      SparkSchemaUtil.convert(arcticTable.schema(), requestedSchema);
    }
  }

  private SparkSession sparkSession() {
    if (lazySpark == null) {
      this.lazySpark = SparkSession.active();
    }

    return lazySpark;
  }

  public ArcticTable table() {
    return arcticTable;
  }

  @Override
  public String name() {
    return arcticTable.id().toString();
  }

  @Override
  public StructType schema() {
    if (lazyTableSchema == null) {
      Schema tableSchema = arcticTable.schema();
      if (requestedSchema != null) {
        Schema prunedSchema = SparkSchemaUtil.prune(tableSchema, requestedSchema);
        this.lazyTableSchema = SparkSchemaUtil.convert(prunedSchema);
      } else {
        this.lazyTableSchema = SparkSchemaUtil.convert(tableSchema);
      }
    }

    return lazyTableSchema;
  }

  @Override
  public Transform[] partitioning() {
    // return toTransforms(arcticTable.spec());
    return Spark3Util.toTransforms(arcticTable.spec());
  }

  @Override
  public Map<String, String> properties() {
    ImmutableMap.Builder<String, String> propsBuilder = ImmutableMap.builder();

    String baseFileFormat = arcticTable.properties()
        .getOrDefault(TableProperties.BASE_FILE_FORMAT, TableProperties.BASE_FILE_FORMAT_DEFAULT);
    String deltaFileFormat = arcticTable.properties()
        .getOrDefault(TableProperties.CHANGE_FILE_FORMAT, TableProperties.CHANGE_FILE_FORMAT_DEFAULT);
    propsBuilder.put("base.write.format", baseFileFormat);
    propsBuilder.put("delta.write.format", deltaFileFormat);
    propsBuilder.put("provider", "arctic");

    arcticTable.properties().entrySet().stream()
        .filter(entry -> !RESERVED_PROPERTIES.contains(entry.getKey()))
        .forEach(propsBuilder::put);

    return propsBuilder.build();
  }

  @Override
  public Set<TableCapability> capabilities() {
    return CAPABILITIES;
  }

  @Override
  public String toString() {
    return arcticTable.toString();
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    } else if (other == null || getClass() != other.getClass()) {
      return false;
    }

    // use only name in order to correctly invalidate Spark cache
    ArcticSparkTable that = (ArcticSparkTable) other;
    return arcticTable.id().equals(that.arcticTable.id());
  }

  @Override
  public int hashCode() {
    // use only name in order to correctly invalidate Spark cache
    return arcticTable.id().hashCode();
  }

  @Override
  public ScanBuilder newScanBuilder(CaseInsensitiveStringMap options) {
    SparkScanBuilder scanBuilder = new SparkScanBuilder(sparkSession(), arcticTable, options);

    if (requestedSchema != null) {
      scanBuilder.pruneColumns(requestedSchema);
    }

    return scanBuilder;
  }

  @Override
  public WriteBuilder newWriteBuilder(LogicalWriteInfo info) {
    return new ArcticSparkWriteBuilder(arcticTable, info, catalog);
  }


  @Override
  public SupportsExtendIdentColumns newUpsertScanBuilder(CaseInsensitiveStringMap options) {
    return new SparkScanBuilder(sparkSession(), arcticTable, options);
  }

  @Override
  public boolean requireAdditionIdentifierColumns() {
    return true;
  }

  @Override
  public boolean appendAsUpsert() {
    return arcticTable.isKeyedTable() &&
        Boolean.parseBoolean(arcticTable.properties().getOrDefault(
            TableProperties.UPSERT_ENABLED, "false"));
  }

  @Override
  public StructType partitionSchema() {
    return SparkSchemaUtil.convert(new Schema(table().spec().partitionType().fields()));
  }

  @Override
  public void createPartition(InternalRow ident, Map<String, String> properties)
      throws PartitionAlreadyExistsException, UnsupportedOperationException {
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
