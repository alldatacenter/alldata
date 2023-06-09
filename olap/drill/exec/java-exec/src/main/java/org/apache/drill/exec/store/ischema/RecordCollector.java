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

import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeField;
import org.apache.calcite.schema.Schema;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.schema.Table;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.planner.logical.DrillViewInfoProvider;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.exec.store.AbstractSchema;
import org.apache.drill.exec.store.dfs.WorkspaceSchemaFactory;
import org.apache.drill.exec.util.FileSystemUtil;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.Metastore;
import org.apache.drill.metastore.components.tables.BasicTablesRequests;
import org.apache.drill.metastore.components.tables.BasicTablesTransformer;
import org.apache.drill.metastore.components.tables.TableMetadataUnit;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.BaseTableMetadata;
import org.apache.drill.metastore.metadata.MetadataInfo;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;

import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.IS_CATALOG_CONNECT;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.IS_CATALOG_DESCRIPTION;
import static org.apache.drill.exec.store.ischema.InfoSchemaConstants.IS_CATALOG_NAME;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides methods to collect various information_schema data.
 */
public interface RecordCollector {

  /**
   * Collects catalogs data for information_schema.
   *
   * @param schemaPath schema name
   * @param schema schema instance
   * @return list of catalog records
   */
  List<Records.Catalog> catalogs(String schemaPath, SchemaPlus schema);

  /**
   * Collects schemas data for information_schema.
   *
   * @param schemaPath schema name
   * @param schema schema instance
   * @return list of schema records
   */
  List<Records.Schema> schemas(String schemaPath, SchemaPlus schema);

  /**
   * Collects tables data for information_schema.
   *
   * @param schemaPath schema name
   * @param schema schema instance
   * @return list of table records
   */
  List<Records.Table> tables(String schemaPath, SchemaPlus schema);

  /**
   * Collects views data for information_schema.
   *
   * @param schemaPath schema name
   * @param schema schema instance
   * @return list of view records
   */
  List<Records.View> views(String schemaPath, SchemaPlus schema);

  /**
   * Collects columns data for information_schema.
   *
   * @param schemaPath schema name
   * @param schema schema instance
   * @return list of column records
   */
  List<Records.Column> columns(String schemaPath, SchemaPlus schema);

  /**
   * Collects partitions data for information_schema.
   *
   * @param schemaPath schema name
   * @param schema schema instance
   * @return list of partition records
   */
  List<Records.Partition> partitions(String schemaPath, SchemaPlus schema);

  /**
   * Collects files data for information_schema.
   *
   * @param schemaPath schema name
   * @param schema schema instance
   * @return list of file records
   */
  List<Records.File> files(String schemaPath, SchemaPlus schema);

  /**
   * Provides information_schema data based on information stored in {@link AbstractSchema}.
   */
  class BasicRecordCollector implements RecordCollector {

    private static final String DEFAULT_OWNER = "<owner>";

    private final FilterEvaluator filterEvaluator;
    private final OptionManager optionManager;

    public BasicRecordCollector(FilterEvaluator filterEvaluator, OptionManager optionManager) {
      this.filterEvaluator = filterEvaluator;
      this.optionManager = optionManager;
    }

    @Override
    public List<Records.Catalog> catalogs(String schemaPath, SchemaPlus schema) {
      return Collections.singletonList(new Records.Catalog(IS_CATALOG_NAME, IS_CATALOG_DESCRIPTION, IS_CATALOG_CONNECT));
    }

    @Override
    public List<Records.Schema> schemas(String schemaPath, SchemaPlus schema) {
      AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);

      return Collections.singletonList(new Records.Schema(IS_CATALOG_NAME, schemaPath,
        DEFAULT_OWNER, drillSchema.getTypeName(), drillSchema.isMutable()));
    }

    @Override
    public List<Records.Table> tables(String schemaPath, SchemaPlus schema) {
      AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);

      return drillSchema.getTableNamesAndTypes().stream()
        .filter(entry -> filterEvaluator.shouldVisitTable(schemaPath, entry.getKey(), entry.getValue()))
        .map(entry -> new Records.Table(IS_CATALOG_NAME, schemaPath, entry.getKey(), entry.getValue().jdbcName))
        .collect(Collectors.toList());
    }

    @Override
    public List<Records.View> views(String schemaPath, SchemaPlus schema) {
      AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);

      return drillSchema.getTablesByNames(schema.getTableNames()).stream()
        .filter(pair -> pair.getValue().getJdbcTableType() == Schema.TableType.VIEW)
        .filter(pair -> filterEvaluator.shouldVisitTable(schemaPath, pair.getKey(), pair.getValue().getJdbcTableType()))
        .map(pair -> new Records.View(IS_CATALOG_NAME, schemaPath, pair.getKey(),
          // View's SQL may not be available for some non-Drill views, for example, JDBC view
          pair.getValue() instanceof DrillViewInfoProvider ? ((DrillViewInfoProvider) pair.getValue()).getViewSql() : ""))
        .collect(Collectors.toList());
    }

    @Override
    public List<Records.Column> columns(String schemaPath, SchemaPlus schema) {
      AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);
      UserGroupInformation ugi = ImpersonationUtil.getProcessUserUGI();
      return drillSchema.needToImpersonateReadingData()
        ? ugi.doAs((PrivilegedAction<List<Records.Column>>) () -> processColumns(schemaPath, schema, drillSchema))
        : processColumns(schemaPath, schema, drillSchema);
    }

    private List<Records.Column> processColumns(String schemaPath, SchemaPlus schema, AbstractSchema drillSchema) {
      List<Records.Column> records = new ArrayList<>();
      for (Pair<String, ? extends Table> tableNameToTable : drillSchema.getTablesByNames(schema.getTableNames())) {
        String tableName = tableNameToTable.getKey();
        Table table = tableNameToTable.getValue();
        Schema.TableType tableType = table.getJdbcTableType();

        if (filterEvaluator.shouldVisitTable(schemaPath, tableName, tableType)) {
          RelDataType tableRow = table.getRowType(new JavaTypeFactoryImpl(DRILL_REL_DATATYPE_SYSTEM));
          for (RelDataTypeField field : tableRow.getFieldList()) {
            if (filterEvaluator.shouldVisitColumn(schemaPath, tableName, field.getName())) {
              records.add(new Records.Column(IS_CATALOG_NAME, schemaPath, tableName, field));
            }
          }
        }
      }
      return records;
    }

    @Override
    public List<Records.Partition> partitions(String schemaPath, SchemaPlus schema) {
      return Collections.emptyList();
    }

    @Override
    public List<Records.File> files(String schemaPath, SchemaPlus schema) {
      if (filterEvaluator.shouldVisitFiles(schemaPath, schema)) {
        try {
          AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);
          if (drillSchema instanceof WorkspaceSchemaFactory.WorkspaceSchema) {
            WorkspaceSchemaFactory.WorkspaceSchema wsSchema = (WorkspaceSchemaFactory.WorkspaceSchema) drillSchema;
            String defaultLocation = wsSchema.getDefaultLocation();
            FileSystem fs = wsSchema.getFS();
            boolean recursive = optionManager.getBoolean(ExecConstants.LIST_FILES_RECURSIVELY);
            // add URI to the path to ensure that directory objects are skipped (see S3AFileSystem.listStatus method)
            return FileSystemUtil.listAllSafe(fs, new Path(fs.getUri().toString(), defaultLocation), recursive).stream()
              .map(fileStatus -> new Records.File(schemaPath, wsSchema, fileStatus))
              .collect(Collectors.toList());
          }
        } catch (ClassCastException | UnsupportedOperationException e) {
          // ignore the exception since either this is not a Drill schema or schema does not support files listing
        }
      }
      return Collections.emptyList();
    }
  }

  /**
   * Provides information_schema data based on information stored in Drill Metastore.
   */
  class MetastoreRecordCollector implements RecordCollector {

    private static final Logger logger = getLogger(MetastoreRecordCollector.class);

    public static final int UNDEFINED_INDEX = -1;

    private final Metastore metastore;
    private final FilterEvaluator filterEvaluator;

    public MetastoreRecordCollector(Metastore metastore, FilterEvaluator filterEvaluator) {
      this.metastore = metastore;
      this.filterEvaluator = filterEvaluator;
    }

    @Override
    public List<Records.Catalog> catalogs(String schemaPath, SchemaPlus schema) {
      return Collections.emptyList();
    }

    @Override
    public List<Records.Schema> schemas(String schemaPath, SchemaPlus schema) {
      return Collections.emptyList();
    }

    @Override
    public List<Records.Table> tables(String schemaPath, SchemaPlus schema) {
      AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);
      List<Records.Table> records = new ArrayList<>();
      List<BaseTableMetadata> baseTableMetadata;
      if (shouldVisitSchema(drillSchema)) {
        try {
          baseTableMetadata = metastore.tables().basicRequests()
            .tablesMetadata(FilterExpression.and(
              FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, drillSchema.getSchemaPath().get(0)),
              FilterExpression.equal(MetastoreColumn.WORKSPACE, drillSchema.getSchemaPath().get(1))));
        } catch (Exception e) {
          // ignore all exceptions related to Metastore data retrieval, return empty result
          logger.warn("Error while retrieving Metastore table data: {}", e.getMessage());
          logger.debug(e.getMessage(), e);
          return records;
        }

        baseTableMetadata.stream()
          .filter(table -> filterEvaluator.shouldVisitTable(schemaPath, table.getTableInfo().name(), Schema.TableType.TABLE))
          .map(table -> new Records.Table(IS_CATALOG_NAME, schemaPath, Schema.TableType.TABLE.toString(), table))
          .forEach(records::add);
      }
      return records;
    }

    @Override
    public List<Records.View> views(String schemaPath, SchemaPlus schema) {
      return Collections.emptyList();
    }

    @Override
    public List<Records.Column> columns(String schemaPath, SchemaPlus schema) {
      AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);
      List<Records.Column> records = new ArrayList<>();
      if (shouldVisitSchema(drillSchema)) {
        List<BaseTableMetadata> baseTableMetadata;
        try {
          baseTableMetadata = metastore.tables().basicRequests()
            .tablesMetadata(FilterExpression.and(
              FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, drillSchema.getSchemaPath().get(0)),
              FilterExpression.equal(MetastoreColumn.WORKSPACE, drillSchema.getSchemaPath().get(1)),
              // exclude tables without schema
              FilterExpression.isNotNull(MetastoreColumn.SCHEMA)));
        } catch (Exception e) {
          // ignore all exceptions related to Metastore data retrieval, return empty result
          logger.warn("Error while retrieving Metastore table data: {}", e.getMessage());
          logger.debug(e.getMessage(), e);
          return records;
        }

        baseTableMetadata.stream()
          .filter(table -> filterEvaluator.shouldVisitTable(schemaPath, table.getTableInfo().name(), Schema.TableType.TABLE))
          .map(table -> columns(schemaPath, table, table.getSchema(), null, UNDEFINED_INDEX, false))
          .forEach(records::addAll);
      }
      return records;
    }

    /**
     * Recursively scan given table schema and provides list of column records.
     * Recursion is used to scan map / struct columns which have nested columns.
     *
     * @param schemaPath schema name
     * @param table table instance
     * @param schema table or column schema
     * @param parentColumnNames list of parent column names if any
     * @param columnIndex column index if any
     * @param isNested indicates if column is nested
     * @return list of column records
     */
    private List<Records.Column> columns(String schemaPath,
                                         BaseTableMetadata table,
                                         TupleMetadata schema,
                                         List<String> parentColumnNames,
                                         int columnIndex,
                                         boolean isNested) {
      List<Records.Column> records = new ArrayList<>();
      schema.toMetadataList().forEach(
        column -> {
          List<String> columnNames = CollectionUtils.isEmpty(parentColumnNames) ? new ArrayList<>() : new ArrayList<>(parentColumnNames);
          columnNames.add(column.name());
          // nested columns have the same index as their parent
          int currentIndex = columnIndex == UNDEFINED_INDEX ? schema.index(column.name()) : columnIndex;
          // if column is a map / struct, recursively scan nested columns
          if (column.isMap()) {
            List<Records.Column> mapRecords =
              columns(schemaPath, table, column.tupleSchema(), columnNames, currentIndex, true);
            records.addAll(mapRecords);
          }

          String tableName = table.getTableInfo().name();

          // concat parent column names to use full column name, i.e. struct_col.nested_col
          String columnPath = String.join(".", columnNames);
          if (filterEvaluator.shouldVisitColumn(schemaPath, tableName, columnPath)) {
            ColumnStatistics<?> columnStatistics =
              table.getColumnStatistics(SchemaPath.getCompoundPath(columnNames.toArray(new String[0])));
            records.add(new Records.Column(IS_CATALOG_NAME, schemaPath, tableName, columnPath,
              column, columnStatistics, currentIndex, isNested));
          }
        });
      return records;
    }

    @Override
    public List<Records.Partition> partitions(String schemaPath, SchemaPlus schema) {
      AbstractSchema drillSchema = schema.unwrap(AbstractSchema.class);
      List<Records.Partition> records = new ArrayList<>();
      if (shouldVisitSchema(drillSchema)) {

        BasicTablesTransformer.MetadataHolder metadataHolder;
        try {
          BasicTablesRequests.RequestMetadata requestMetadata = BasicTablesRequests.RequestMetadata.builder()
            .metadataTypes(MetadataType.SEGMENT, MetadataType.PARTITION)
            .customFilter(FilterExpression.and(
              FilterExpression.equal(MetastoreColumn.STORAGE_PLUGIN, drillSchema.getSchemaPath().get(0)),
              FilterExpression.equal(MetastoreColumn.WORKSPACE, drillSchema.getSchemaPath().get(1)),
              // exclude DEFAULT_SEGMENT (used only for non-partitioned tables)
              FilterExpression.notEqual(MetastoreColumn.METADATA_KEY, MetadataInfo.DEFAULT_SEGMENT_KEY)))
            .build();

          List<TableMetadataUnit> units = metastore.tables().basicRequests().request(requestMetadata);

          metadataHolder = BasicTablesTransformer.all(units);
        } catch (Exception e) {
          // ignore all exceptions related to Metastore data retrieval, return empty result
          logger.warn("Error while retrieving Metastore segment / partition data: {}", e.getMessage());
          logger.debug(e.getMessage(), e);
          return records;
        }

        metadataHolder.segments().stream()
          .filter(segment -> filterEvaluator.shouldVisitTable(schemaPath, segment.getTableInfo().name(), Schema.TableType.TABLE))
          .filter(segmentMetadata -> Objects.nonNull(segmentMetadata.getPartitionValues()))
          .map(segment -> Records.Partition.fromSegment(IS_CATALOG_NAME, schemaPath, segment))
          .forEach(records::addAll);

        metadataHolder.partitions().stream()
          .filter(partition -> filterEvaluator.shouldVisitTable(schemaPath, partition.getTableInfo().name(), Schema.TableType.TABLE))
          .filter(partitionMetadata -> Objects.nonNull(partitionMetadata.getPartitionValues()))
          .map(partition -> Records.Partition.fromPartition(IS_CATALOG_NAME, schemaPath, partition))
          .forEach(records::addAll);
      }
      return records;
    }

    @Override
    public List<Records.File> files(String schemaPath, SchemaPlus schema) {
      return Collections.emptyList();
    }

    /**
     * Checks if given schema should be searched in Drill Metastore.
     * Schema must have to parent with corresponds to storage plugin and
     * actual name which corresponds to workspace name.
     *
     * @param schema schema instance
     * @return true if schema should be visited, false otherwise
     */
    private boolean shouldVisitSchema(AbstractSchema schema) {
      return schema.getSchemaPath().size() == 2;
    }
  }
}
