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

import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.hive.catalog.ArcticHiveCatalog;
import com.netease.arctic.spark.util.ArcticSparkUtil;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.spark.sql.RuntimeConfig;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.TableIdentifier;
import org.apache.spark.sql.catalyst.analysis.NoSuchDatabaseException;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.catalyst.catalog.CatalogTable;
import org.apache.spark.sql.catalyst.catalog.SessionCatalog;
import org.apache.spark.sql.internal.StaticSQLConf$;
import org.apache.spark.sql.sources.DataSourceRegister;
import org.apache.spark.sql.sources.v2.DataSourceOptions;
import org.apache.spark.sql.sources.v2.DataSourceV2;
import org.apache.spark.sql.sources.v2.ReadSupport;
import org.apache.spark.sql.sources.v2.WriteSupport;
import org.apache.spark.sql.sources.v2.reader.DataSourceReader;
import org.apache.spark.sql.sources.v2.writer.DataSourceWriter;
import org.apache.spark.sql.types.StructType;
import scala.Option;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ArcticSource implements DataSourceRegister, DataSourceV2, TableSupport,
    WriteSupport, ReadSupport {
  @Override
  public String shortName() {
    return "arctic";
  }

  @Override
  public ArcticSparkTable createTable(
      TableIdentifier identifier, StructType schema, List<String> partitions, Map<String, String> properties) {
    SparkSession spark = SparkSession.getActiveSession().get();
    ArcticCatalog catalog = catalog(spark.conf());
    ArcticTable arcticTable;
    Schema arcticSchema = SparkSchemaUtil.convert(schema, false);
    PartitionSpec spec = toPartitionSpec(partitions, arcticSchema);
    TableBuilder tableBuilder = catalog.newTableBuilder(com.netease.arctic.table.TableIdentifier
        .of(catalog.name(), identifier.database().get(), identifier.table()), arcticSchema);
    if (properties.containsKey("primary.keys")) {
      PrimaryKeySpec primaryKeySpec = PrimaryKeySpec.builderFor(arcticSchema)
          .addDescription(properties.get("primary.keys"))
          .build();
      arcticTable = tableBuilder.withPartitionSpec(spec)
          .withPrimaryKeySpec(primaryKeySpec)
          .withProperties(properties)
          .create();
    } else {
      arcticTable = tableBuilder.withPartitionSpec(spec)
          .withProperties(properties)
          .create();
    }
    return ArcticSparkTable.ofArcticTable(identifier, arcticTable);
  }

  private static PartitionSpec toPartitionSpec(List<String> partitionKeys, Schema icebergSchema) {
    PartitionSpec.Builder builder = PartitionSpec.builderFor(icebergSchema);
    partitionKeys.forEach(builder::identity);
    return builder.build();
  }

  @Override
  public ArcticSparkTable loadTable(TableIdentifier identifier) {
    SparkSession spark = SparkSession.getActiveSession().get();
    ArcticCatalog catalog = catalog(spark.conf());
    com.netease.arctic.table.TableIdentifier tableId = com.netease.arctic.table.TableIdentifier.of(
        catalog.name(), identifier.database().get(), identifier.table());
    ArcticTable arcticTable = catalog.loadTable(tableId);
    return ArcticSparkTable.ofArcticTable(identifier, arcticTable);
  }

  @Override
  public boolean tableExists(TableIdentifier identifier) {
    SparkSession spark = SparkSession.getActiveSession().get();
    ArcticCatalog catalog = catalog(spark.conf());
    com.netease.arctic.table.TableIdentifier tableId = com.netease.arctic.table.TableIdentifier.of(
        catalog.name(), identifier.database().get(), identifier.table());
    return catalog.tableExists(tableId);
  }

  @Override
  public boolean dropTable(TableIdentifier identifier, boolean purge) {
    SparkSession spark = SparkSession.getActiveSession().get();
    ArcticHiveCatalog catalog = (ArcticHiveCatalog) catalog(spark.conf());
    return catalog.dropTable(com.netease.arctic.table.TableIdentifier
        .of(catalog.name(), identifier.database().get(), identifier.table()), purge);
  }

  /**
   * delegate table to arctic if all condition matched
   * 1. spark catalog-impl is hive
   * 2. spark conf 'arctic.sql.delegate-hive-table = true'
   * 3. table.provider in (hive, parquet)
   * 4. arctic catalog have table with same identifier with given table
   */
  public boolean isDelegateTable(CatalogTable tableDesc) {
    SparkSession spark = SparkSession.getActiveSession().get();
    String sparkCatalogImpl = spark.conf().get(StaticSQLConf$.MODULE$.CATALOG_IMPLEMENTATION());
    if (!"hive".equalsIgnoreCase(sparkCatalogImpl)) {
      return false;
    }
    boolean delegateHiveTable = ArcticSparkUtil.delegateHiveTable(spark.conf());
    if (!delegateHiveTable) {
      return false;
    }
    if (!tableDesc.provider().isDefined()) {
      return false;
    }
    String provider = tableDesc.provider().get();
    if (!"hive".equalsIgnoreCase(provider) &&
        !"parquet".equalsIgnoreCase(provider) &&
        !"arctic".equalsIgnoreCase(provider)) {
      return false;
    }
    TableIdentifier identifier = tableDesc.identifier();
    ArcticCatalog catalog = catalog(spark.conf());
    com.netease.arctic.table.TableIdentifier tableId = com.netease.arctic.table.TableIdentifier.of(
        catalog.name(), identifier.database().get(), identifier.table());
    return catalog.tableExists(tableId);
  }

  public boolean isDelegateDropTable(TableIdentifier identifier, boolean isView) {
    if (isView) {
      return false;
    }
    SparkSession spark = SparkSession.getActiveSession().get();
    SessionCatalog catalog = spark.sessionState().catalog();
    if (catalog.isTemporaryTable(identifier) ||
        identifier.database().isEmpty() ||
        !catalog.tableExists(identifier)) {
      return false;
    }
    try {
      CatalogTable tableDesc = catalog.getTableMetadata(identifier);
      return isDelegateTable(tableDesc);
    } catch (NoSuchTableException | NoSuchDatabaseException e) {
      return false;
    }
  }

  private ArcticCatalog catalog(RuntimeConfig conf) {
    String url = ArcticSparkUtil.catalogUrl(conf);
    return CatalogLoader.load(url);
  }

  @Override
  public DataSourceReader createReader(DataSourceOptions options) {
    ArcticTable arcticTable = getTableWithPath(options);
    SparkSession spark = SparkSession.getActiveSession().get();
    return ArcticSparkTable.createReaderWithTable(arcticTable, options, spark);
  }

  @Override
  public Optional<DataSourceWriter> createWriter(String jobId, StructType schema,
                                                 SaveMode mode, DataSourceOptions options) {
    ArcticTable arcticTable = getOrCreateTableWithPath(options, schema);
    return ArcticSparkTable.createWriterWithTable(arcticTable, jobId, schema, mode, options);
  }

  public ArcticTable getOrCreateTableWithPath(DataSourceOptions options, StructType schema) {
    Preconditions.checkArgument(
        options.asMap().containsKey("path"),
        "Cannot open table: path is not set");
    String path = options.get("path").get();
    Preconditions.checkArgument(!path.contains("/"),
        "invalid table identifier %s, contain '/'", path);
    List<String> nameParts = Lists.newArrayList(path.split("\\."));
    SparkSession spark = SparkSession.getActiveSession().get();
    ArcticCatalog catalog = catalog(spark.conf());
    com.netease.arctic.table.TableIdentifier tableId = com.netease.arctic.table.TableIdentifier.of(
        catalog.name(), nameParts.get(0), nameParts.get(1));
    if (!catalog.tableExists(tableId)) {
      return createTable(new TableIdentifier(tableId.getTableName(), Option.apply(tableId.getDatabase())),
          schema,
          Lists.newArrayList(options.get("partition.keys").get()),
          options.asMap()).table();
    } else {
      return catalog.loadTable(tableId);
    }
  }

  public ArcticTable getTableWithPath(DataSourceOptions options) {
    Preconditions.checkArgument(
        options.asMap().containsKey("path"),
        "Cannot open table: path is not set");
    String path = options.get("path").get();
    Preconditions.checkArgument(!path.contains("/"),
        "invalid table identifier %s, contain '/'", path);
    List<String> nameParts = Lists.newArrayList(path.split("\\."));
    SparkSession spark = SparkSession.getActiveSession().get();
    ArcticCatalog catalog = catalog(spark.conf());
    com.netease.arctic.table.TableIdentifier tableId = com.netease.arctic.table.TableIdentifier.of(
        catalog.name(), nameParts.get(0), nameParts.get(1));
    return catalog.loadTable(tableId);
  }
}
