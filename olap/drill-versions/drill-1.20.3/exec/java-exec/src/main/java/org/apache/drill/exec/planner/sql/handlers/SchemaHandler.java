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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.calcite.sql.SqlNode;
import org.apache.commons.io.IOUtils;
import org.apache.drill.common.exceptions.UserException;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.physical.PhysicalPlan;
import org.apache.drill.exec.planner.sql.DirectPlan;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.sql.parser.SqlCreateType;
import org.apache.drill.exec.planner.sql.parser.SqlSchema;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.exec.record.metadata.schema.InlineSchemaProvider;
import org.apache.drill.exec.record.metadata.schema.PathSchemaProvider;
import org.apache.drill.exec.record.metadata.schema.SchemaContainer;
import org.apache.drill.exec.record.metadata.schema.SchemaProvider;
import org.apache.drill.exec.record.metadata.schema.SchemaProviderFactory;
import org.apache.drill.exec.record.metadata.schema.StorageProperties;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.StorageStrategy;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Parent class for CREATE / DROP / DESCRIBE / ALTER SCHEMA handlers.
 * Contains common logic on how extract workspace, output error result.
 */
public abstract class SchemaHandler extends DefaultSqlHandler {

  static final Logger logger = LoggerFactory.getLogger(SchemaHandler.class);

  SchemaHandler(SqlHandlerConfig config) {
    super(config);
  }

  public WorkspaceSchemaFactory.WorkspaceSchema getWorkspaceSchema(List<String> tableSchema, String tableName) {
    SchemaPlus defaultSchema = config.getConverter().getDefaultSchema();
    AbstractSchema temporarySchema = SchemaUtilites.resolveToTemporarySchema(tableSchema, defaultSchema, context.getConfig());

    if (context.getSession().isTemporaryTable(temporarySchema, context.getConfig(), tableName)) {
      produceErrorResult(String.format("Indicated table [%s] is temporary table", tableName), true);
    }

    AbstractSchema drillSchema = SchemaUtilites.resolveToMutableDrillSchema(defaultSchema, tableSchema);
    Table table = SqlHandlerUtil.getTableFromSchema(drillSchema, tableName);
    if (table == null || table.getJdbcTableType() != Schema.TableType.TABLE) {
      produceErrorResult(String.format("Table [%s] was not found", tableName), true);
    }

    if (!(drillSchema instanceof WorkspaceSchemaFactory.WorkspaceSchema)) {
      produceErrorResult(String.format("Table [`%s`.`%s`] must belong to file storage plugin",
        drillSchema.getFullSchemaName(), tableName), true);
    }

    Preconditions.checkState(drillSchema instanceof WorkspaceSchemaFactory.WorkspaceSchema);
    return (WorkspaceSchemaFactory.WorkspaceSchema) drillSchema;
  }

  PhysicalPlan produceErrorResult(String message, boolean doFail) {
    if (doFail) {
      throw UserException.validationError().message(message).build(logger);
    } else {
      return DirectPlan.createDirectPlan(context, false, message);
    }
  }

  /**
   * Provides storage strategy which will create schema file
   * with same permission as used for persistent tables.
   *
   * @return storage strategy
   */
  StorageStrategy getStorageStrategy() {
    return new StorageStrategy(context.getOption(
      ExecConstants.PERSISTENT_TABLE_UMASK).string_val, false);
  }

  /**
   * CREATE SCHEMA command handler.
   */
  public static class Create extends SchemaHandler {

    public Create(SqlHandlerConfig config) {
      super(config);
    }

    @Override
    public PhysicalPlan getPlan(SqlNode sqlNode) {

      SqlSchema.Create sqlCall = ((SqlSchema.Create) sqlNode);

      String schemaString = getSchemaString(sqlCall);
      String schemaSource = sqlCall.hasTable() ? sqlCall.getTable().toString() : sqlCall.getPath();
      try {

        SchemaProvider schemaProvider = SchemaProviderFactory.create(sqlCall, this);
        if (schemaProvider.exists()) {
          if (SqlCreateType.OR_REPLACE == sqlCall.getSqlCreateType()) {
            schemaProvider.delete();
          } else {
            return produceErrorResult(String.format("Schema already exists for [%s]", schemaSource), true);
          }
        }

        StorageProperties storageProperties = StorageProperties.builder()
          .storageStrategy(getStorageStrategy())
          .overwrite(false)
          .build();

        schemaProvider.store(schemaString, sqlCall.getProperties(), storageProperties);

        return DirectPlan.createDirectPlan(context, true, String.format("Created schema for [%s]", schemaSource));
      } catch (IOException e) {
        throw UserException.resourceError(e)
          .message(e.getMessage())
          .addContext("Error while preparing / creating schema for [%s]", schemaSource)
          .build(logger);
      } catch (IllegalArgumentException e) {
        throw UserException.validationError(e)
          .message(e.getMessage())
          .addContext("Error while preparing / creating schema for [%s]", schemaSource)
          .build(logger);
      }
    }

    /**
     * If raw schema was present in create schema command, returns schema from command,
     * otherwise loads raw schema from the given file.
     *
     * @param sqlCall sql create schema call
     * @return string representation of raw schema (column names, types and nullability)
     */
    private String getSchemaString(SqlSchema.Create sqlCall) {
      if (sqlCall.hasSchema()) {
        return sqlCall.getSchema();
      }

      Path path = new Path(sqlCall.getLoad());
      try {
        FileSystem rawFs = path.getFileSystem(new Configuration());
        FileSystem fs = ImpersonationUtil.createFileSystem(ImpersonationUtil.getProcessUserName(), rawFs.getConf());

        if (!fs.exists(path)) {
          throw UserException.resourceError()
            .message("File with raw schema [%s] does not exist", path.toUri().getPath())
            .build(logger);
        }

        try (InputStream stream = fs.open(path)) {
          return IOUtils.toString(stream);
        }

      } catch (IOException e) {
        throw UserException.resourceError(e)
          .message("Unable to load raw schema from file %s", path.toUri().getPath())
          .build(logger);
      }
    }
  }

  /**
   * DROP SCHEMA command handler.
   */
  public static class Drop extends SchemaHandler {

    public Drop(SqlHandlerConfig config) {
      super(config);
    }

    @Override
    public PhysicalPlan getPlan(SqlNode sqlNode) {
      SqlSchema.Drop sqlCall = ((SqlSchema.Drop) sqlNode);

      try {
        SchemaProvider schemaProvider = SchemaProviderFactory.create(sqlCall, this);

        if (!schemaProvider.exists()) {
          return produceErrorResult(String.format("Schema [%s] does not exist in table [%s] root directory",
            SchemaProvider.DEFAULT_SCHEMA_NAME, sqlCall.getTable()), !sqlCall.ifExists());
        }

        schemaProvider.delete();

        return DirectPlan.createDirectPlan(context, true,
          String.format("Dropped schema for table [%s]", sqlCall.getTable()));

      } catch (IOException e) {
        throw UserException.resourceError(e)
          .message(e.getMessage())
          .addContext("Error while accessing table location or deleting schema for [%s]", sqlCall.getTable())
          .build(logger);
      }
    }
  }

  /**
   * DESCRIBE SCHEMA FOR TABLE command handler.
   */
  public static class Describe extends SchemaHandler {

    public Describe(SqlHandlerConfig config) {
      super(config);
    }

    @Override
    public PhysicalPlan getPlan(SqlNode sqlNode) {
      SqlSchema.Describe sqlCall = ((SqlSchema.Describe) sqlNode);

      try {
        SchemaProvider schemaProvider = SchemaProviderFactory.create(sqlCall, this);

        if (schemaProvider.exists()) {
          SchemaContainer schemaContainer = schemaProvider.read();

          String schema;
          switch (sqlCall.getFormat()) {
            case JSON:
              schema = PathSchemaProvider.WRITER.writeValueAsString(schemaContainer);
              break;
            case STATEMENT:
              TupleMetadata metadata = schemaContainer.getSchema();
              StringBuilder builder = new StringBuilder("CREATE OR REPLACE SCHEMA \n");

              List<ColumnMetadata> columnsMetadata = metadata.toMetadataList();
              if (columnsMetadata.isEmpty()) {
                builder.append("() \n");
              } else {
                builder.append("(\n");

                builder.append(columnsMetadata.stream()
                  .map(ColumnMetadata::columnString)
                  .collect(Collectors.joining(", \n")));

                builder.append("\n) \n");
              }

              builder.append("FOR TABLE ").append(schemaContainer.getTable()).append(" \n");

              Map<String, String> properties = metadata.properties();
              if (!properties.isEmpty()) {
                builder.append("PROPERTIES (\n");

                builder.append(properties.entrySet().stream()
                  .map(e -> String.format("'%s' = '%s'", e.getKey(), e.getValue()))
                  .collect(Collectors.joining(", \n")));
                builder.append("\n)");
              }

              schema = builder.toString();
              break;
            default:
              throw UserException.validationError()
                .message("Unsupported describe schema format: [%s]", sqlCall.getFormat())
                .build(logger);
          }

          return DirectPlan.createDirectPlan(context, new SchemaResult(schema));
        }

        return DirectPlan.createDirectPlan(context, false,
          String.format("Schema for table [%s] is absent", sqlCall.getTable()));

      } catch (IOException e) {
        throw UserException.resourceError(e)
          .message(e.getMessage())
          .addContext("Error while accessing schema for table [%s]", sqlCall.getTable())
          .build(logger);
      }
    }

    /**
     * Wrapper to output schema in a form of table with one column named `schema`.
     */
    public static class SchemaResult {

      public String schema;

      public SchemaResult(String schema) {
        this.schema = schema;
      }
    }
  }

  /**
   * ALTER SCHEMA ADD command handler.
   */
  public static class Add extends SchemaHandler {

    public Add(SqlHandlerConfig config) {
      super(config);
    }

    @Override
    public PhysicalPlan getPlan(SqlNode sqlNode) {
      SqlSchema.Add addCall = ((SqlSchema.Add) sqlNode);
      String schemaSource = addCall.hasTable() ? addCall.getTable().toString() : addCall.getPath();

      try {
        SchemaProvider schemaProvider = SchemaProviderFactory.create(addCall, this);

        if (!schemaProvider.exists()) {
          throw UserException.resourceError()
            .message("Schema does not exist for [%s]", schemaSource)
            .addContext("Command: ALTER SCHEMA ADD")
            .build(logger);
        }

        TupleMetadata currentSchema = schemaProvider.read().getSchema();
        TupleMetadata newSchema = new TupleSchema();

        if (addCall.hasSchema()) {
          InlineSchemaProvider inlineSchemaProvider = new InlineSchemaProvider(addCall.getSchema());
          TupleMetadata providedSchema = inlineSchemaProvider.read().getSchema();

          if (addCall.isReplace()) {
            Map<String, ColumnMetadata> columnsMap = Stream.concat(currentSchema.toMetadataList().stream(), providedSchema.toMetadataList().stream())
              .collect(Collectors.toMap(
                ColumnMetadata::name,
                Function.identity(),
                (o, n) -> n, // replace existing columns
                LinkedHashMap::new)); // preserve initial order of the columns
            columnsMap.values().forEach(newSchema::addColumn);
          } else {
            Stream.concat(currentSchema.toMetadataList().stream(), providedSchema.toMetadataList().stream())
              .forEach(newSchema::addColumn);
          }
        } else {
          currentSchema.toMetadataList().forEach(newSchema::addColumn);
        }

        if (addCall.hasProperties()) {
          if (addCall.isReplace()) {
            newSchema.setProperties(currentSchema.properties());
            newSchema.setProperties(addCall.getProperties());
          } else {
            Map<String, String> newProperties = Stream.concat(currentSchema.properties().entrySet().stream(), addCall.getProperties().entrySet().stream())
              .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue)); // no merge strategy is provided to fail on duplicate
            newSchema.setProperties(newProperties);
          }
        } else {
          newSchema.setProperties(currentSchema.properties());
        }

        String schemaString = newSchema.toMetadataList().stream()
          .map(ColumnMetadata::columnString)
          .collect(Collectors.joining(", "));

        StorageProperties storageProperties = StorageProperties.builder()
          .storageStrategy(getStorageStrategy())
          .overwrite()
          .build();

        schemaProvider.store(schemaString, newSchema.properties(), storageProperties);
        return DirectPlan.createDirectPlan(context, true, String.format("Schema for [%s] was updated", schemaSource));

      } catch (IOException e) {
        throw UserException.resourceError(e)
          .message("Error while accessing / modifying schema for [%s]: %s", schemaSource, e.getMessage())
          .build(logger);
      } catch (IllegalArgumentException | IllegalStateException e) {
        throw UserException.validationError(e)
          .message(e.getMessage())
          .addContext("Error while preparing / creating schema for [%s]", schemaSource)
          .build(logger);
      }
    }
  }

  /**
   * ALTER SCHEMA REMOVE command handler.
   */
  public static class Remove extends SchemaHandler {

    public Remove(SqlHandlerConfig config) {
      super(config);
    }

    @Override
    public PhysicalPlan getPlan(SqlNode sqlNode) {
      SqlSchema.Remove removeCall = ((SqlSchema.Remove) sqlNode);
      String schemaSource = removeCall.hasTable() ? removeCall.getTable().toString() : removeCall.getPath();

      try {
        SchemaProvider schemaProvider = SchemaProviderFactory.create(removeCall, this);

        if (!schemaProvider.exists()) {
          throw UserException.resourceError()
            .message("Schema does not exist for [%s]", schemaSource)
            .addContext("Command: ALTER SCHEMA REMOVE")
            .build(logger);
        }

        TupleMetadata currentSchema = schemaProvider.read().getSchema();
        TupleMetadata newSchema = new TupleSchema();

        List<String> columns = removeCall.getColumns();

        currentSchema.toMetadataList().stream()
          .filter(column -> columns == null || !columns.contains(column.name()))
          .forEach(newSchema::addColumn);

        newSchema.setProperties(currentSchema.properties());
        if (removeCall.hasProperties()) {
          removeCall.getProperties().forEach(newSchema::removeProperty);
        }

        StorageProperties storageProperties = StorageProperties.builder()
          .storageStrategy(getStorageStrategy())
          .overwrite()
          .build();

        String schemaString = newSchema.toMetadataList().stream()
          .map(ColumnMetadata::columnString)
          .collect(Collectors.joining(", "));

        schemaProvider.store(schemaString, newSchema.properties(), storageProperties);
        return DirectPlan.createDirectPlan(context, true, String.format("Schema for [%s] was updated", schemaSource));

      } catch (IOException e) {
        throw UserException.resourceError(e)
          .message("Error while accessing / modifying schema for [%s]: %s", schemaSource, e.getMessage())
          .build(logger);
      } catch (IllegalArgumentException e) {
        throw UserException.validationError(e)
          .message(e.getMessage())
          .addContext("Error while preparing / creating schema for [%s]", schemaSource)
          .build(logger);
      }
    }
  }
}
