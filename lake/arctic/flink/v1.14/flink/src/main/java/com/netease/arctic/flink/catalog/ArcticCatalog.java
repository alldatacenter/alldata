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

package com.netease.arctic.flink.catalog;

import com.google.common.base.Objects;
import com.netease.arctic.NoSuchDatabaseException;
import com.netease.arctic.flink.InternalCatalogBuilder;
import com.netease.arctic.flink.catalog.factories.ArcticCatalogFactoryOptions;
import com.netease.arctic.flink.table.DynamicTableFactory;
import com.netease.arctic.flink.table.descriptors.ArcticValidator;
import com.netease.arctic.flink.util.ArcticUtils;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableBuilder;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.utils.CompatiblePropertyUtil;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.WatermarkSpec;
import org.apache.flink.table.catalog.AbstractCatalog;
import org.apache.flink.table.catalog.CatalogBaseTable;
import org.apache.flink.table.catalog.CatalogDatabase;
import org.apache.flink.table.catalog.CatalogFunction;
import org.apache.flink.table.catalog.CatalogPartition;
import org.apache.flink.table.catalog.CatalogPartitionSpec;
import org.apache.flink.table.catalog.CatalogTable;
import org.apache.flink.table.catalog.CatalogTableImpl;
import org.apache.flink.table.catalog.ObjectPath;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.table.catalog.exceptions.DatabaseAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.DatabaseNotExistException;
import org.apache.flink.table.catalog.exceptions.FunctionNotExistException;
import org.apache.flink.table.catalog.exceptions.TableAlreadyExistException;
import org.apache.flink.table.catalog.exceptions.TableNotExistException;
import org.apache.flink.table.catalog.stats.CatalogColumnStatistics;
import org.apache.flink.table.catalog.stats.CatalogTableStatistics;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.factories.Factory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.logical.RowType;
import org.apache.iceberg.PartitionField;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.exceptions.AlreadyExistsException;
import org.apache.iceberg.flink.FlinkSchemaUtil;
import org.apache.iceberg.flink.util.FlinkCompatibilityUtil;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.netease.arctic.flink.FlinkSchemaUtil.toSchema;
import static org.apache.flink.table.factories.FactoryUtil.CONNECTOR;

/**
 * Catalogs for arctic data lake.
 */
public class ArcticCatalog extends AbstractCatalog {
  public static final String DEFAULT_DB = "default";

  /**
   * To distinguish 'CREATE TABLE LIKE' by checking stack
   * {@link org.apache.flink.table.planner.operations.SqlCreateTableConverter#lookupLikeSourceTable}
   */
  public static final String SQL_LIKE_METHOD = "lookupLikeSourceTable";

  private final InternalCatalogBuilder catalogBuilder;

  private com.netease.arctic.catalog.ArcticCatalog internalCatalog;

  public ArcticCatalog(
      String name,
      String defaultDatabase,
      InternalCatalogBuilder catalogBuilder) {
    super(name, defaultDatabase);
    this.catalogBuilder = catalogBuilder;
  }

  public ArcticCatalog(ArcticCatalog copy) {
    this(copy.getName(), copy.getDefaultDatabase(), copy.catalogBuilder);
  }

  @Override
  public void open() throws CatalogException {
    internalCatalog = catalogBuilder.build();
  }

  @Override
  public void close() throws CatalogException {
  }

  @Override
  public List<String> listDatabases() throws CatalogException {
    return internalCatalog.listDatabases();
  }

  @Override
  public CatalogDatabase getDatabase(String databaseName) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean databaseExists(String databaseName) throws CatalogException {
    return listDatabases().stream().anyMatch(db -> db.equalsIgnoreCase(databaseName));
  }

  @Override
  public void createDatabase(String name, CatalogDatabase database, boolean ignoreIfExists) throws CatalogException,
      DatabaseAlreadyExistException {
    try {
      internalCatalog.createDatabase(name);
    } catch (AlreadyExistsException e) {
      if (!ignoreIfExists) {
        throw new DatabaseAlreadyExistException(getName(), name, e);
      }
    }
  }

  @Override
  public void dropDatabase(String name, boolean ignoreIfNotExists, boolean cascade) throws CatalogException,
      DatabaseNotExistException {
    try {
      internalCatalog.dropDatabase(name);
    } catch (NoSuchDatabaseException e) {
      if (!ignoreIfNotExists) {
        throw new DatabaseNotExistException(getName(), name);
      }
    }
  }

  @Override
  public void alterDatabase(String name, CatalogDatabase newDatabase, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> listTables(String databaseName) throws CatalogException {
    return internalCatalog.listTables(databaseName)
        .stream().map(TableIdentifier::getTableName)
        .collect(Collectors.toList());
  }

  @Override
  public List<String> listViews(String databaseName) throws CatalogException {
    return Collections.emptyList();
  }

  @Override
  public CatalogBaseTable getTable(ObjectPath tablePath) throws TableNotExistException, CatalogException {
    TableIdentifier tableIdentifier = getTableIdentifier(tablePath);
    if (!internalCatalog.tableExists(tableIdentifier)) {
      throw new TableNotExistException(this.getName(), tablePath);
    }
    ArcticTable table = internalCatalog.loadTable(tableIdentifier);
    Schema arcticSchema = table.schema();

    RowType rowType = FlinkSchemaUtil.convert(arcticSchema);
    Map<String, String> arcticProperties = Maps.newHashMap(table.properties());
    fillTableProperties(arcticProperties);
    fillTableMetaPropertiesIfLookupLike(arcticProperties, tableIdentifier);

    List<String> partitionKeys = toPartitionKeys(table.spec(), table.schema());
    return new CatalogTableImpl(
        toSchema(rowType, ArcticUtils.getPrimaryKeys(table)),
        partitionKeys,
        arcticProperties,
        null);
  }

  /**
   * For now, 'CREATE TABLE LIKE' would be treated as the case which users want to add watermark in temporal join,
   * as an alternative of lookup join, and use Arctic table as build table, i.e. right table.
   * So the properties those required in temporal join will be put automatically.
   * <p>
   * If you don't want the properties, 'EXCLUDING ALL' is what you need.
   * More details @see <a href="https://nightlies.apache.org/flink/flink-docs-master/docs/dev/table/sql/create/#like">LIKE</a>
   */
  private void fillTableMetaPropertiesIfLookupLike(Map<String, String> properties, TableIdentifier tableIdentifier) {
    StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
    boolean isLookupLike = false;
    for (StackTraceElement stackTraceElement : stackTraceElements) {
      if (Objects.equal(SQL_LIKE_METHOD, stackTraceElement.getMethodName())) {
        isLookupLike = true;
        break;
      }
    }

    if (!isLookupLike) {
      return;
    }

    properties.put(CONNECTOR.key(), DynamicTableFactory.IDENTIFIER);
    properties.put(ArcticValidator.ARCTIC_CATALOG.key(), tableIdentifier.getCatalog());
    properties.put(ArcticValidator.ARCTIC_TABLE.key(), tableIdentifier.getTableName());
    properties.put(ArcticValidator.ARCTIC_DATABASE.key(), tableIdentifier.getDatabase());
    properties.put(ArcticCatalogFactoryOptions.METASTORE_URL.key(), catalogBuilder.getMetastoreUrl());
  }

  private static List<String> toPartitionKeys(PartitionSpec spec, Schema icebergSchema) {
    List<String> partitionKeys = Lists.newArrayList();
    for (PartitionField field : spec.fields()) {
      if (field.transform().isIdentity()) {
        partitionKeys.add(icebergSchema.findColumnName(field.sourceId()));
      } else {
        // Not created by Flink SQL.
        // For compatibility with iceberg tables, return empty.
        // TODO modify this after Flink support partition transform.
        return Collections.emptyList();
      }
    }
    return partitionKeys;
  }

  private void fillTableProperties(Map<String, String> tableProperties) {
    boolean enableStream = CompatiblePropertyUtil.propertyAsBoolean(tableProperties,
        TableProperties.ENABLE_LOG_STORE, TableProperties.ENABLE_LOG_STORE_DEFAULT);
    if (enableStream) {
      tableProperties.putIfAbsent(FactoryUtil.FORMAT.key(), tableProperties.getOrDefault(
          TableProperties.LOG_STORE_DATA_FORMAT,
          TableProperties.LOG_STORE_DATA_FORMAT_DEFAULT));
    }
  }

  private TableIdentifier getTableIdentifier(ObjectPath tablePath) {
    return TableIdentifier.of(internalCatalog.name(), tablePath.getDatabaseName(), tablePath.getObjectName());
  }

  @Override
  public boolean tableExists(ObjectPath tablePath) throws CatalogException {
    return internalCatalog.tableExists(getTableIdentifier(tablePath));
  }

  @Override
  public void dropTable(ObjectPath tablePath, boolean ignoreIfNotExists) throws CatalogException {
    internalCatalog.dropTable(getTableIdentifier(tablePath), true);
  }

  @Override
  public void renameTable(ObjectPath tablePath, String newTableName, boolean ignoreIfNotExists)
      throws CatalogException {
    internalCatalog.renameTable(
        getTableIdentifier(tablePath),
        newTableName);
  }

  @Override
  public void createTable(ObjectPath tablePath, CatalogBaseTable table, boolean ignoreIfExists)
      throws CatalogException, TableAlreadyExistException {
    validateFlinkTable(table);

    TableSchema tableSchema = table.getSchema();
    TableSchema.Builder flinkSchemaBuilder = TableSchema.builder();

    tableSchema.getTableColumns().forEach(c -> {
      List<WatermarkSpec> ws = tableSchema.getWatermarkSpecs();
      for (WatermarkSpec w : ws) {
        if (w.getRowtimeAttribute().equals(c.getName())) {
          return;
        }
      }
      flinkSchemaBuilder.field(c.getName(), c.getType());
    });
    if (tableSchema.getPrimaryKey().isPresent()) {
      flinkSchemaBuilder.primaryKey(tableSchema.getPrimaryKey().get().getColumns().toArray(new String[0]));
    }
    TableSchema tableSchemaWithoutWatermark = flinkSchemaBuilder.build();

    Schema icebergSchema = FlinkSchemaUtil.convert(tableSchemaWithoutWatermark);

    TableBuilder tableBuilder = internalCatalog.newTableBuilder(
        getTableIdentifier(tablePath), icebergSchema);

    tableSchema.getPrimaryKey().ifPresent(k -> {
          PrimaryKeySpec.Builder builder = PrimaryKeySpec.builderFor(icebergSchema);
          k.getColumns().forEach(builder::addColumn);
          tableBuilder.withPrimaryKeySpec(builder.build());
        }
    );

    PartitionSpec spec = toPartitionSpec(((CatalogTable) table).getPartitionKeys(), icebergSchema);
    tableBuilder.withPartitionSpec(spec);

    tableBuilder.withProperties(table.getOptions());

    try {
      tableBuilder.create();
    } catch (AlreadyExistsException e) {
      if (!ignoreIfExists) {
        throw new TableAlreadyExistException(getName(), tablePath, e);
      }
    }
  }

  private static PartitionSpec toPartitionSpec(List<String> partitionKeys, Schema icebergSchema) {
    PartitionSpec.Builder builder = PartitionSpec.builderFor(icebergSchema);
    partitionKeys.forEach(builder::identity);
    return builder.build();
  }

  private static void validateFlinkTable(CatalogBaseTable table) {
    Preconditions.checkArgument(table instanceof CatalogTable, "The Table should be a CatalogTable.");

    TableSchema schema = table.getSchema();
    schema.getTableColumns().forEach(column -> {
      if (!FlinkCompatibilityUtil.isPhysicalColumn(column)) {
        throw new UnsupportedOperationException("Creating table with computed columns is not supported yet.");
      }
    });

  }

  @Override
  public void alterTable(ObjectPath tablePath, CatalogBaseTable newTable, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CatalogPartitionSpec> listPartitions(ObjectPath tablePath) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CatalogPartitionSpec> listPartitions(ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<CatalogPartitionSpec> listPartitionsByFilter(ObjectPath tablePath, List<Expression> filters)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CatalogPartition getPartition(ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean partitionExists(ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void createPartition(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec, CatalogPartition partition,
      boolean ignoreIfExists) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dropPartition(ObjectPath tablePath, CatalogPartitionSpec partitionSpec, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartition(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec, CatalogPartition newPartition,
      boolean ignoreIfNotExists) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<String> listFunctions(String dbName) throws CatalogException {
    return Collections.emptyList();
  }

  @Override
  public CatalogFunction getFunction(ObjectPath functionPath) throws FunctionNotExistException, CatalogException {
    throw new FunctionNotExistException(getName(), functionPath);
  }

  @Override
  public boolean functionExists(ObjectPath functionPath) throws CatalogException {
    return false;
  }

  @Override
  public void createFunction(ObjectPath functionPath, CatalogFunction function, boolean ignoreIfExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterFunction(ObjectPath functionPath, CatalogFunction newFunction, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void dropFunction(ObjectPath functionPath, boolean ignoreIfNotExists) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public CatalogTableStatistics getTableStatistics(ObjectPath tablePath) throws CatalogException {
    return CatalogTableStatistics.UNKNOWN;
  }

  @Override
  public CatalogColumnStatistics getTableColumnStatistics(ObjectPath tablePath) throws CatalogException {
    return CatalogColumnStatistics.UNKNOWN;
  }

  @Override
  public CatalogTableStatistics getPartitionStatistics(ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws CatalogException {
    return CatalogTableStatistics.UNKNOWN;
  }

  @Override
  public CatalogColumnStatistics getPartitionColumnStatistics(ObjectPath tablePath, CatalogPartitionSpec partitionSpec)
      throws CatalogException {
    return CatalogColumnStatistics.UNKNOWN;
  }

  @Override
  public void alterTableStatistics(
      ObjectPath tablePath, CatalogTableStatistics tableStatistics,
      boolean ignoreIfNotExists) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterTableColumnStatistics(
      ObjectPath tablePath, CatalogColumnStatistics columnStatistics,
      boolean ignoreIfNotExists) throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartitionStatistics(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec,
      CatalogTableStatistics partitionStatistics, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void alterPartitionColumnStatistics(
      ObjectPath tablePath, CatalogPartitionSpec partitionSpec,
      CatalogColumnStatistics columnStatistics, boolean ignoreIfNotExists)
      throws CatalogException {
    throw new UnsupportedOperationException();
  }

  @Override
  public Optional<Factory> getFactory() {
    return Optional.of(new DynamicTableFactory(this, catalogBuilder, internalCatalog.name()));
  }
}
