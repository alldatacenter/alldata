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
package org.apache.drill.exec.store;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.exec.record.metadata.schema.SchemaProviderFactory;
import org.apache.drill.exec.store.table.function.TableParamDef;
import org.apache.drill.exec.store.table.function.TableSignature;
import org.apache.drill.exec.store.table.function.WithOptionsTableMacro;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.calcite.linq4j.tree.DefaultExpression;
import org.apache.calcite.linq4j.tree.Expression;
import org.apache.calcite.rel.type.RelProtoDataType;
import org.apache.calcite.schema.Function;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.SchemaVersion;
import org.apache.calcite.schema.Table;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.dotdrill.View;
import org.apache.drill.exec.planner.logical.CreateTableEntry;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;

public abstract class AbstractSchema implements Schema, SchemaPartitionExplorer, AutoCloseable {
  static final Logger logger = LoggerFactory.getLogger(AbstractSchema.class);

  private static final Expression EXPRESSION = new DefaultExpression(Object.class);

  private static final String SCHEMA_PARAMETER_NAME = "schema";

  /**
   * Schema parameter for table function which creates schema provider based on
   * given parameter value.
   */
  private static final TableParamDef SCHEMA_PARAMETER = TableParamDef.optional(
    SCHEMA_PARAMETER_NAME, String.class, (drillTable, value) -> {
      if (value == null) {
        return;
      }

      SchemaProvider schemaProvider;
      try {
        schemaProvider = SchemaProviderFactory.create(String.valueOf(value));
        // since we path schema here as table parameter
        // read schema to ensure that schema is valid before query execution
        schemaProvider.read();
      } catch (IOException | IllegalArgumentException e) {
        throw UserException.validationError(e)
          .message(e.getMessage())
          .addContext("Schema parameter value [%s]", value)
          .build(logger);
      }

      drillTable.getMetadataProviderManager().setSchemaProvider(schemaProvider);
    });

  public static final String SCHEMA_SEPARATOR = ".";

  protected final List<String> schemaPath;
  protected final String name;

  public AbstractSchema(List<String> parentSchemaPath, String name) {
    name = name == null ? null : name.toLowerCase();
    schemaPath = new ArrayList<>();
    schemaPath.addAll(parentSchemaPath);
    schemaPath.add(name);
    this.name = name;
  }

  @Override
  public Iterable<String> getSubPartitions(String table,
                                           List<String> partitionColumns,
                                           List<String> partitionValues
                                          ) throws PartitionNotFoundException {
    throw new UnsupportedOperationException(
        String.format("Schema of type: %s " +
                      "does not support retrieving sub-partition information.",
                      this.getClass().getSimpleName()));
  }

  public String getName() {
    return name;
  }

  public List<String> getSchemaPath() {
    return schemaPath;
  }

  public String getFullSchemaName() {
    return Joiner.on(SCHEMA_SEPARATOR).join(schemaPath);
  }

  @Override
  public final String toString() {
    return getFullSchemaName();
  }

  /**
   * Returns string describing schema type which shows where the schema came from.
   * Good practice here is to return json type name of storage plugin's config.
   *
   * @return schema type name
   */
  public abstract String getTypeName();

  /**
   * The schema can be a top level schema which doesn't have its own tables, but refers
   * to one of the default sub schemas for table look up.
   * <p>
   * Default implementation returns itself.
   * <p>
   * Ex. "dfs" schema refers to the tables in "default" workspace when querying for
   * tables in "dfs" schema.
   *
   * @return Return the default schema where tables are created or retrieved from.
   */
  public Schema getDefaultSchema() {
    return this;
  }

  /**
   * Create a new view given definition.
   * @param view View info including name, definition etc.
   * @return Returns true if an existing view is replaced with the given view. False otherwise.
   * @throws IOException in case of error creating a view
   */
  public boolean createView(View view) throws IOException {
    throw UserException.unsupportedError()
        .message("Creating new view is not supported in schema [%s]", getSchemaPath())
        .build(logger);
  }

  /**
   * Drop the view with given name.
   *
   * @param viewName view name
   * @throws IOException in case of error dropping the view
   */
  public void dropView(String viewName) throws IOException {
    throw UserException.unsupportedError()
        .message("Dropping a view is supported in schema [%s]", getSchemaPath())
        .build(logger);
  }

  /**
   * Creates table entry using table name, list of partition columns
   * and storage strategy used to create table folder and files
   *
   * @param tableName : new table name.
   * @param partitionColumns : list of partition columns. Empty list if there is no partition columns.
   * @param storageStrategy : storage strategy used to create table folder and files
   * @return create table entry
   */
  public CreateTableEntry createNewTable(String tableName, List<String> partitionColumns, StorageStrategy storageStrategy) {
    throw UserException.unsupportedError()
        .message("Creating new tables is not supported in schema [%s]", getSchemaPath())
        .build(logger);
  }

  /**
   * Creates table entry using table name and list of partition columns if any.
   * Table folder and files will be created using persistent storage strategy.
   *
   * @param tableName : new table name.
   * @param partitionColumns : list of partition columns. Empty list if there is no partition columns.
   * @return create table entry
   */
  public CreateTableEntry createNewTable(String tableName, List<String> partitionColumns) {
    return createNewTable(tableName, partitionColumns, StorageStrategy.DEFAULT);
  }

  /**
   * Create stats table entry for given <i>tableName</i>.
   * @param tableName table name
   * @return instance of create table entry
   */
  public CreateTableEntry createStatsTable(String tableName) {
    throw UserException.unsupportedError()
        .message("Statistics tables are not supported in schema [%s]", getSchemaPath())
        .build(logger);
  }

  /**
   * Create an append statistics table entry for given <i>tableName</i>. If there is not existing
   * statistics table, a new one is created.
   * @param tableName table name
   * @return instance of create table entry
   */
  public CreateTableEntry appendToStatsTable(String tableName) {
    throw UserException.unsupportedError()
        .message("Statistics tables are not supported in schema [%s]", getSchemaPath())
        .build(logger);
  }

  /**
   * Get the statistics table for given <i>tableName</i>
   * @param tableName table name
   * @return instance of statistics table
   */
  public Table getStatsTable(String tableName) {
    throw UserException.unsupportedError()
        .message("Statistics tables are not supported in schema [%s]", getSchemaPath())
        .build(logger);
  }

  /**
   * Reports whether to show items from this schema in INFORMATION_SCHEMA
   * tables.
   * (Controls ... TODO:  Doc.:  Mention what this typically controls or
   * affects.)
   * <p>
   *   This base implementation returns {@code true}.
   * </p>
   */
  public boolean showInInformationSchema() {
    return true;
  }

  /**
   * For the given table names returns list of acceptable table functions
   * which are common for all Drill schemas. When overriding this method,
   * parent functions must be included first to be evaluated first.
   * If not included, parent functions won't be taken into account when creating table instance.
   *
   * @param name table name
   * @return list of table functions
   */
  @Override
  public Collection<Function> getFunctions(String name) {
    List<TableParamDef> parameters = getFunctionParameters();
    TableSignature signature = TableSignature.of(name, parameters);
    WithOptionsTableMacro function = new WithOptionsTableMacro(signature, arguments -> {
      Table table = getTable(name);
      if (table instanceof DrillTable) {
        return applyFunctionParameters((DrillTable) table, parameters, arguments);
      } else if (table == null) {
        return null;
      }
      throw new DrillRuntimeException(String.format("Table [%s] is not of Drill table instance. " +
        "Given instance is of [%s].", name, table.getClass().getName()));
    });
    return Collections.singletonList(function);
  }

  /**
   * Returns of common table function parameters that can be used all Drill schema implementations.
   *
   * @return list of table function parameters
   */
  public List<TableParamDef> getFunctionParameters() {
    return Collections.singletonList(SCHEMA_PARAMETER);
  }

  /**
   * For the given list of parameters definitions executes action for the corresponding value.
   *
   * @param drillTable Drill table instance
   * @param paramDefs parameter definitions
   * @param values parameter values
   * @return updated Drill table instance
   */
  public DrillTable applyFunctionParameters(DrillTable drillTable, List<TableParamDef> paramDefs, List<Object> values) {
    IntStream.range(0, paramDefs.size())
      .forEach(i -> paramDefs.get(i).apply(drillTable, values.get(i)));
    return drillTable;
  }

  /**
   * Returns a map of types in this schema by name.
   *
   * <p>The implementations of {@link #getTypeNames()}
   * and {@link #getType(String)} depend on this map.
   * The default implementation of this method returns the empty map.
   * Override this method to change their behavior.</p>
   *
   * @return Map of types in this schema by name
   */
  protected Map<String, RelProtoDataType> getTypeMap() {
    return ImmutableMap.of();
  }

  @Override
  public Set<String> getTypeNames() {
    return getTypeMap().keySet();
  }

  @Override
  public RelProtoDataType getType(String name) {
    return getTypeMap().get(name);
  }

  @Override
  public Set<String> getFunctionNames() {
    return Collections.emptySet();
  }

  @Override
  public Schema getSubSchema(String name) {
    return null;
  }

  @Override
  public Set<String> getSubSchemaNames() {
    return Collections.emptySet();
  }

  @Override
  public boolean isMutable() {
    return false;
  }

  @Override
  public Table getTable(String name){
    return null;
  }

  @Override
  public Set<String> getTableNames() {
    return Collections.emptySet();
  }

  @Override
  public Expression getExpression(SchemaPlus parentSchema, String name) {
    return EXPRESSION;
  }

  @Override
  public void close() throws Exception {
    // no-op: default implementation for most implementations.
  }

  @Override
  public Schema snapshot(SchemaVersion version) {
    return this;
  }

  public void dropTable(String tableName) {
    throw UserException.unsupportedError()
        .message("Dropping tables is not supported in schema [%s]", getSchemaPath())
        .build(logger);
  }

  /**
   * Get the collection of {@link Table} tables specified in the tableNames.
   *
   * @param  tableNames the requested tables, specified by the table names
   * @return the collection of requested tables
   */
  public List<Pair<String, ? extends Table>> getTablesByNames(final Set<String> tableNames) {
    return tableNames.stream()
        .map(tableName -> Pair.of(tableName, getTable(tableName)))
        .filter(pair -> Objects.nonNull(pair.getValue())) // Schema may return NULL for table if user doesn't have permissions to load the table.
        .collect(Collectors.toList());
  }

  /**
   * Used by {@link org.apache.drill.exec.store.ischema.InfoSchemaRecordGenerator.Tables}
   * for getting all table objects along with type for every requested schema. It's desired
   * for this method to work fast because it impacts SHOW TABLES query.
   *
   * @return collection of table names and types
   */
  public Collection<Map.Entry<String, TableType>> getTableNamesAndTypes() {
    return getTablesByNames(getTableNames()).stream()
        .map(nameAndTable -> Pair.of(nameAndTable.getKey(), nameAndTable.getValue().getJdbcTableType()))
        .collect(Collectors.toList());
  }

  /**
   * Indicates if table names in schema are case sensitive. By default they are.
   * If schema implementation claims its table names are case insensitive,
   * it is responsible for making case insensitive look up by table name.
   *
   * @return true if table names are case sensitive
   */
  public boolean areTableNamesCaseSensitive() {
    return true;
  }

  /**
   * @param impersonated User whom to impersonate. Usually {@link SchemaConfig#getUserName()}
   * @param notImpersonated Regular user (table owner for Views or Drillbit process user for Tables)
   * @return endUser
   */
  public String getUser(String impersonated, String notImpersonated) {
    return notImpersonated;
  }

  /**
   * Does Drill needs to impersonate as user connected to Drill when reading data from DataSource?
   * @return True when both Drill impersonation and DataSource impersonation are enabled.
   */
  public boolean needToImpersonateReadingData() {
    return false;
  }
}
