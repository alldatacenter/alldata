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

package com.netease.arctic.spark.source;

import com.netease.arctic.spark.reader.ArcticKeyedTableScan;
import com.netease.arctic.spark.reader.ArcticUnkeyedTableScan;
import com.netease.arctic.spark.writer.ArcticKeyedSparkOverwriteWriter;
import com.netease.arctic.spark.writer.ArcticUnkeyedSparkOverwriteWriter;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.TableIdentifier;
import org.apache.spark.sql.sources.v2.DataSourceOptions;
import org.apache.spark.sql.sources.v2.reader.DataSourceReader;
import org.apache.spark.sql.sources.v2.writer.DataSourceWriter;
import org.apache.spark.sql.types.StructType;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ArcticSparkTable implements DataSourceTable {

  private static final Set<String> RESERVED_PROPERTIES = Sets.newHashSet("provider", "format", "current-snapshot-id");
  private final TableIdentifier identifier;
  private final ArcticTable arcticTable;
  private final StructType requestedSchema;
  private final boolean refreshEagerly;
  private StructType lazyTableSchema = null;
  private static SparkSession lazySpark = null;

  public static ArcticSparkTable ofArcticTable(TableIdentifier identifier, ArcticTable table) {
    return new ArcticSparkTable(table, identifier, null, false);
  }

  public ArcticSparkTable(ArcticTable arcticTable, TableIdentifier identifier, StructType requestedSchema,
                          boolean refreshEagerly) {
    this.arcticTable = arcticTable;
    this.requestedSchema = requestedSchema;
    this.refreshEagerly = refreshEagerly;
    if (requestedSchema != null) {
      // convert the requested schema to throw an exception if any requested fields are unknown
      SparkSchemaUtil.convert(arcticTable.schema(), requestedSchema);
    }
    this.identifier = identifier;
  }

  private SparkSession sparkSession() {
    if (lazySpark == null) {
      this.lazySpark = SparkSession.builder().getOrCreate();
    }
    return lazySpark;
  }

  public String name() {
    return arcticTable.id().toString();
  }

  public TableIdentifier identifier() {
    return this.identifier;
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

  public ArcticTable table() {
    return arcticTable;
  }

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
  public DataSourceReader createReader(DataSourceOptions options) {
    return createReaderWithTable(arcticTable, options, sparkSession());
  }

  @Override
  public Optional<DataSourceWriter> createWriter(String jobId, StructType schema,
                                                 SaveMode mode, DataSourceOptions options) {
    return createWriterWithTable(arcticTable, jobId, schema, mode, options);
  }

  public static DataSourceReader createReaderWithTable(ArcticTable table, DataSourceOptions options,
                                                       SparkSession spark) {
    if (table.isKeyedTable()) {
      return new ArcticKeyedTableScan(spark, table.asKeyedTable());
    } else {
      return new ArcticUnkeyedTableScan(spark, table.asUnkeyedTable());
    }
  }

  public static Optional<DataSourceWriter> createWriterWithTable(ArcticTable table, String jobId, StructType schema,
                                                                 SaveMode mode, DataSourceOptions options) {
    if (table.isKeyedTable()) {
      if (mode == SaveMode.Overwrite) {
        return Optional.of(new ArcticKeyedSparkOverwriteWriter(table.asKeyedTable(), schema, options));
      } else if (mode == SaveMode.Append) {
        // TODO: support keyed append
        throw new UnsupportedOperationException("Not support now!");
      }
    } else if (table.isUnkeyedTable()) {
      if (mode == SaveMode.Overwrite) {
        return Optional.of(new ArcticUnkeyedSparkOverwriteWriter(table.asUnkeyedTable(), schema, options));
      } else if (mode == SaveMode.Append) {
        // TODO: support unkeyed append
        throw new UnsupportedOperationException("Not support now!");
      }
    } else {
      throw new UnsupportedOperationException("Illegal table type!");
    }
    return Optional.empty();
  }
}
