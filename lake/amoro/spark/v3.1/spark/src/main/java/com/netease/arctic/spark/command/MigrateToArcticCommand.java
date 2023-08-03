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

package com.netease.arctic.spark.command;

import com.netease.arctic.spark.ArcticSparkCatalog;
import com.netease.arctic.spark.ArcticSparkSessionCatalog;
import com.netease.arctic.spark.table.ArcticIcebergSparkTable;
import com.netease.arctic.spark.table.ArcticSparkTable;
import com.netease.arctic.spark.util.ArcticSparkUtils;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.MetricsConfig;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.TableMigrationUtil;
import org.apache.iceberg.hadoop.Util;
import org.apache.iceberg.relocated.com.google.common.base.Joiner;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.spark.SparkSchemaUtil;
import org.apache.iceberg.spark.SparkTableUtil;
import org.apache.spark.sql.AnalysisException;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.RowFactory;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.catalyst.TableIdentifier;
import org.apache.spark.sql.catalyst.analysis.NoSuchDatabaseException;
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException;
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException;
import org.apache.spark.sql.catalyst.analysis.TableAlreadyExistsException;
import org.apache.spark.sql.catalyst.catalog.CatalogTable;
import org.apache.spark.sql.connector.catalog.CatalogManager;
import org.apache.spark.sql.connector.catalog.Identifier;
import org.apache.spark.sql.connector.catalog.SupportsNamespaces;
import org.apache.spark.sql.connector.catalog.Table;
import org.apache.spark.sql.connector.catalog.TableCatalog;
import org.apache.spark.sql.connector.catalog.V1Table;
import org.apache.spark.sql.connector.expressions.Transform;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.Metadata;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import scala.Option;
import scala.Some;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * migrate a v1 table to arctic table.
 * will reuse file in v1 table , but delete metadata in session catalog
 */
public class MigrateToArcticCommand implements ArcticSparkCommand {
  private static final Logger LOG = LoggerFactory.getLogger(MigrateToArcticCommand.class);

  private static final String V1TABLE_BACKUP_SUFFIX = "_BAK_ARCTIC_";
  protected static final List<String> EXCLUDED_PROPERTIES =
      ImmutableList.of("path", "transient_lastDdlTime", "serialization.format");

  private static final StructType OUTPUT_TYPE = new StructType(
      new StructField[] {
          new StructField("partition", DataTypes.StringType, false, Metadata.empty()),
          new StructField("file_counts", DataTypes.IntegerType, false, Metadata.empty())
      }
  );

  private final SparkSession spark;
  private final TableCatalog sourceCatalog;
  private final Identifier sourceIdentifier;
  private final Identifier backupV1TableIdentifier;
  private final TableCatalog targetCatalog;
  private final Identifier targetIdentifier;

  protected MigrateToArcticCommand(
      TableCatalog sourceCatalog, Identifier sourceIdentifier,
      TableCatalog catalog, Identifier identifier,
      SparkSession spark
  ) {
    this.spark = spark;
    this.sourceCatalog = sourceCatalog;
    this.targetCatalog = catalog;
    this.targetIdentifier = identifier;
    this.sourceIdentifier = sourceIdentifier;
    String backupName = sourceIdentifier.name();
    backupV1TableIdentifier = Identifier.of(sourceIdentifier.namespace(), backupName);
  }

  @Override
  public String name() {
    return "MigrateToArctic";
  }

  @Override
  public StructType outputType() {
    return OUTPUT_TYPE;
  }

  @Override
  public Row[] execute() throws AnalysisException {
    List<DataFile> dataFiles;
    TableIdentifier ident;
    PartitionSpec spec;
    Schema schema;
    LOG.info("start to migrate {} to {}, using temp backup table {}",
        sourceIdentifier, targetIdentifier, backupV1TableIdentifier);
    V1Table sourceTable = loadV1Table(sourceCatalog, backupV1TableIdentifier);
    ident = new TableIdentifier(
        backupV1TableIdentifier.name(),
        Some.apply(backupV1TableIdentifier.namespace()[0]));
    dataFiles = loadDataFiles(ident);
    UnkeyedTable table = createUnkeyedTable(sourceTable);

    spec = table.spec();

    AppendFiles appendFiles = table.newAppend();
    dataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();

    LOG.info("migrate table {} finished, remove metadata of backup {} table",
        targetIdentifier, backupV1TableIdentifier);

    if (PartitionSpec.unpartitioned().equals(spec)) {
      return new Row[] {RowFactory.create("ALL", dataFiles.size())};
    }

    Map<String, List<DataFile>> partitions = Maps.newHashMap();
    dataFiles.forEach(d -> {
      String partition = spec.partitionToPath(d.partition());
      List<DataFile> df = partitions.computeIfAbsent(partition, p -> Lists.newArrayList());
      df.add(d);
    });
    return partitions.keySet().stream()
        .sorted()
        .map(p -> RowFactory.create(p, partitions.get(p).size()))
        .toArray(Row[]::new);
  }

  private List<DataFile> loadDataFiles(TableIdentifier ident) throws AnalysisException {
    PartitionSpec spec  = SparkSchemaUtil.specForTable(spark, ident.database().get() + "." + ident.table());

    if (spec.equals(PartitionSpec.unpartitioned())) {
      return listUnPartitionedSparkTable(spark, ident);
    } else {
      List<SparkTableUtil.SparkPartition> sparkPartitions = SparkTableUtil.getPartitions(spark, ident,
          Maps.newHashMap());
      Preconditions.checkArgument(!sparkPartitions.isEmpty(),
          "Cannot find any partitions in table %s", ident);
      return listPartitionDataFiles(spark, sparkPartitions, spec);
    }
  }

  private UnkeyedTable createUnkeyedTable(V1Table sourceTable)
      throws TableAlreadyExistsException, NoSuchNamespaceException {
    Map<String, String> properties = Maps.newHashMap();
    properties.putAll(sourceTable.properties());
    EXCLUDED_PROPERTIES.forEach(properties::remove);
    properties.put(TableCatalog.PROP_PROVIDER, "arctic");
    properties.put("migrated", "true");

    StructType schema = sourceTable.schema();
    Transform[] partitions = sourceTable.partitioning();
    boolean threw = true;
    Table table = null;
    try {
      table = targetCatalog.createTable(
          targetIdentifier, schema, partitions, properties
      );
      if (table instanceof ArcticIcebergSparkTable) {
        threw = false;
        return ((ArcticIcebergSparkTable) table).table();
      } else if (table instanceof ArcticSparkTable) {
        threw = false;
        return ((ArcticSparkTable)table).table().asUnkeyedTable();
      }
      throw new IllegalStateException("target table must be un-keyed table");
    } finally {
      if (threw && table != null) {
        try {
          targetCatalog.dropTable(targetIdentifier);
        } catch (Exception e) {
          LOG.warn("error when rollback table", e);
        }
      }
    }
  }


  private static V1Table loadV1Table(TableCatalog catalog, Identifier identifier) throws NoSuchTableException {
    Table table = catalog.loadTable(identifier);
    Preconditions.checkArgument(table instanceof V1Table, "source table must be V1Table");
    return (V1Table) table;
  }

  private static List<DataFile> listUnPartitionedSparkTable(
      SparkSession spark, TableIdentifier sourceTableIdent
  ) throws NoSuchDatabaseException, NoSuchTableException {
    CatalogTable sourceTable = spark.sessionState().catalog().getTableMetadata(sourceTableIdent);
    Option<String> format =
        sourceTable.storage().serde().nonEmpty() ? sourceTable.storage().serde() : sourceTable.provider();
    Preconditions.checkArgument(format.nonEmpty(), "Could not determine table format");

    Map<String, String> partition = Collections.emptyMap();
    PartitionSpec spec = PartitionSpec.unpartitioned();
    Configuration conf = spark.sessionState().newHadoopConf();
    MetricsConfig metricsConfig = MetricsConfig.getDefault();
    return TableMigrationUtil.listPartition(
        partition, Util.uriToString(sourceTable.location()),
        format.get(), spec, conf, metricsConfig, null);
  }

  private static List<DataFile> listPartitionDataFiles(
      SparkSession spark, List<SparkTableUtil.SparkPartition> partitions, PartitionSpec spec) {

    Configuration conf = spark.sessionState().newHadoopConf();
    MetricsConfig metricsConfig = MetricsConfig.getDefault();

    return partitions.stream().map(
        p -> TableMigrationUtil.listPartition(p.getValues(), p.getUri(), p.getFormat(), spec, conf,
            metricsConfig, null)
    ).flatMap(Collection::stream).collect(Collectors.toList());
  }

  public static Builder newBuilder(SparkSession spark) {
    return new Builder(spark);
  }

  public static class Builder {

    List<String> source;
    List<String> target;

    SparkSession spark;

    private Builder(SparkSession spark) {
      this.spark = spark;
    }

    public Builder withSource(List<String> source) {
      this.source = source;
      return this;
    }

    public Builder withTarget(List<String> target) {
      this.target = target;
      return this;
    }

    public MigrateToArcticCommand build() throws NoSuchTableException {
      ArcticSparkUtils.TableCatalogAndIdentifier tableCatalogAndIdentifier
          = ArcticSparkUtils.tableCatalogAndIdentifier(spark, source);
      TableCatalog sourceCatalog = tableCatalogAndIdentifier.catalog();
      Identifier sourceTableIdentifier = tableCatalogAndIdentifier.identifier();

      checkSourceCatalogAndTable(sourceCatalog, sourceTableIdentifier);

      tableCatalogAndIdentifier = ArcticSparkUtils.tableCatalogAndIdentifier(spark, target);
      TableCatalog targetCatalog = tableCatalogAndIdentifier.catalog();
      Identifier targetTableIdentifier = tableCatalogAndIdentifier.identifier();

      checkTargetCatalog(targetCatalog);
      checkTargetTable(targetCatalog, targetTableIdentifier);

      return new MigrateToArcticCommand(
          sourceCatalog, sourceTableIdentifier,
          targetCatalog, targetTableIdentifier,
          spark
      );
    }

    private void checkSourceCatalogAndTable(TableCatalog catalog, Identifier identifier) throws NoSuchTableException {
      Preconditions.checkArgument(
          catalog.name().equalsIgnoreCase(CatalogManager.SESSION_CATALOG_NAME()),
          "source table must in session catalog, current table is %s",
          catalog.name()
      );


      Preconditions.checkArgument(
          catalog.tableExists(identifier),
          "source table %s does not exist in catalog %s",
          Joiner.on(".").join(identifier.namespace()),
          catalog.name());
      loadV1Table(catalog, identifier);
    }

    private void checkTargetCatalog(TableCatalog catalog) {
      Preconditions.checkArgument(
          catalog instanceof ArcticSparkCatalog || catalog instanceof ArcticSparkSessionCatalog,
          "target catalog must be %s",
          ArcticSparkCatalog.class.getName());
    }

    private void checkTargetTable(TableCatalog catalog, Identifier identifier) {
      Preconditions.checkArgument(catalog instanceof SupportsNamespaces,
          "The target catalog must support namespace");
      Preconditions.checkArgument(
          ((SupportsNamespaces)catalog).namespaceExists(identifier.namespace()),
          "database %s does not exist in catalog %s",
          Joiner.on(".").join(identifier.namespace()), catalog.name());

      List<String> nameParts = Lists.newArrayList(identifier.namespace());
      nameParts.add(identifier.name());
      Preconditions.checkArgument(
          !catalog.tableExists(identifier),
          "target table %s already exist in catalog %s",
          Joiner.on(".").join(nameParts), catalog.name());
    }
  }
}
