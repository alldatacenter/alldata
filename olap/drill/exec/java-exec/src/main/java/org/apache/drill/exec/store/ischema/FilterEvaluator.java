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
package org.apache.drill.exec.store.ischema;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableMap;

import java.util.Map;

import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.CATS_COL_CATALOG_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.COLS_COL_COLUMN_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_ROOT_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.FILES_COL_WORKSPACE_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.IS_CATALOG_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SCHS_COL_SCHEMA_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_NAME;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.SHRD_COL_TABLE_SCHEMA;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.TBLS_COL_TABLE_TYPE;

/**
 * Evaluates information schema for the given condition.
 * Evaluation can be done with or without filter.
 */
public interface FilterEvaluator {

  /**
   * Visit the catalog. Drill has only one catalog.
   *
   * @return whether to continue exploring the contents of the catalog or not.
   *         Contents are schema/schema tree.
   */
  boolean shouldVisitCatalog();

  /**
   * Prune the given schema.
   *
   * @param schemaName name of the schema
   * @param schema schema object
   * @return whether to prune this schema and all its descendants from the
   * search tree.
   */
  boolean shouldPruneSchema(String schemaName);

  /**
   * Visit the given schema.
   *
   * @param schemaName name of the schema
   * @param schema schema object
   * @return whether to continue exploring the contents of the schema or not.
   *         Contents are tables within the schema.
   */
  boolean shouldVisitSchema(String schemaName, SchemaPlus schema);

  /**
   * Visit the tables in the given schema.
   *
   * @param schemaName name of the schema
   * @param tableName name of the table
   * @param tableType type of the table
   * @return whether to continue exploring the contents of the table or not.
   *         Contents are tables attributes and columns.
   */
  boolean shouldVisitTable(String schemaName, String tableName, Schema.TableType tableType);

  /**
   * Visit the columns in the given schema and table.
   *
   * @param schemaName name of the schema
   * @param tableName name of the table
   * @param columnName type of the table
   * @return whether to continue exploring the contents of the column or not.
   *         Contents are columns attributes.
   */
  boolean shouldVisitColumn(String schemaName, String tableName, String columnName);

  /**
   * Visit the files in the given schema.
   *
   * @param schemaName name of the schema
   * @param schema schema object
   * @return whether to continue exploring the files in the schema
   */
  boolean shouldVisitFiles(String schemaName, SchemaPlus schema);


  /**
   * Evaluates necessity to visit certain type of information_schema data based
   * on given schema type.
   */
  class NoFilterEvaluator implements FilterEvaluator {

    public static final FilterEvaluator INSTANCE = new NoFilterEvaluator();

    @Override
    public boolean shouldVisitCatalog() {
      return true;
    }

    @Override
    public boolean shouldPruneSchema(String schemaName) {
      return false;
    }

    @Override
    public boolean shouldVisitSchema(String schemaName, SchemaPlus schema) {
      try {
        AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);
        return drillSchema.showInInformationSchema();
      } catch (ClassCastException e) {
        return false;
      }
    }

    @Override
    public boolean shouldVisitTable(String schemaName, String tableName, Schema.TableType tableType) {
      return true;
    }

    @Override
    public boolean shouldVisitColumn(String schemaName, String tableName, String columnName) {
      return true;
    }

    @Override
    public boolean shouldVisitFiles(String schemaName, SchemaPlus schema) {
      try {
        AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);
        return drillSchema instanceof WorkspaceSchemaFactory.WorkspaceSchema;
      } catch (ClassCastException e) {
        return false;
      }
    }
  }

  /**
   * Evaluates necessity to visit certain type of information_schema data using provided filter.
   */
  class InfoSchemaFilterEvaluator extends NoFilterEvaluator {

    private final InfoSchemaFilter filter;

    public InfoSchemaFilterEvaluator(InfoSchemaFilter filter) {
      this.filter = filter;
    }

    @Override
    public boolean shouldVisitCatalog() {
      Map<String, String> recordValues = ImmutableMap.of(CATS_COL_CATALOG_NAME, IS_CATALOG_NAME);

      return filter.evaluate(recordValues) != InfoSchemaFilter.Result.FALSE;
    }

    @Override
    public boolean shouldPruneSchema(String schemaName) {
      Map<String, String> recordValues = ImmutableMap.of(
        CATS_COL_CATALOG_NAME, IS_CATALOG_NAME,
        SHRD_COL_TABLE_SCHEMA, schemaName,
        SCHS_COL_SCHEMA_NAME, schemaName);

      return filter.evaluate(recordValues, true) == InfoSchemaFilter.Result.FALSE;
    }

    @Override
    public boolean shouldVisitSchema(String schemaName, SchemaPlus schema) {
      if (!super.shouldVisitSchema(schemaName, schema)) {
        return false;
      }

      Map<String, String> recordValues = ImmutableMap.of(
        CATS_COL_CATALOG_NAME, IS_CATALOG_NAME,
        SHRD_COL_TABLE_SCHEMA, schemaName,
        SCHS_COL_SCHEMA_NAME, schemaName);

      return filter.evaluate(recordValues) != InfoSchemaFilter.Result.FALSE;
    }

    @Override
    public boolean shouldVisitTable(String schemaName, String tableName, Schema.TableType tableType) {
      Map<String, String> recordValues = ImmutableMap.of(
        CATS_COL_CATALOG_NAME, IS_CATALOG_NAME,
        SHRD_COL_TABLE_SCHEMA, schemaName,
        SCHS_COL_SCHEMA_NAME, schemaName,
        SHRD_COL_TABLE_NAME, tableName,
        TBLS_COL_TABLE_TYPE, tableType.jdbcName);

      return filter.evaluate(recordValues) != InfoSchemaFilter.Result.FALSE;
    }

    @Override
    public boolean shouldVisitColumn(String schemaName, String tableName, String columnName) {
      Map<String, String> recordValues = ImmutableMap.of(
        CATS_COL_CATALOG_NAME, IS_CATALOG_NAME,
        SHRD_COL_TABLE_SCHEMA, schemaName,
        SCHS_COL_SCHEMA_NAME, schemaName,
        SHRD_COL_TABLE_NAME, tableName,
        COLS_COL_COLUMN_NAME, columnName);

      return filter.evaluate(recordValues) != InfoSchemaFilter.Result.FALSE;
    }

    @Override
    public boolean shouldVisitFiles(String schemaName, SchemaPlus schema) {
      if (!super.shouldVisitFiles(schemaName, schema)) {
        return false;
      }

      AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);
      WorkspaceSchemaFactory.WorkspaceSchema wsSchema = (WorkspaceSchemaFactory.WorkspaceSchema) drillSchema;

      Map<String, String> recordValues = ImmutableMap.of(
        FILES_COL_SCHEMA_NAME, schemaName,
        FILES_COL_ROOT_SCHEMA_NAME, wsSchema.getSchemaPath().get(0),
        FILES_COL_WORKSPACE_NAME, wsSchema.getName());

      return filter.evaluate(recordValues) != InfoSchemaFilter.Result.FALSE;
    }
  }
}
