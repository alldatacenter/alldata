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

package com.netease.arctic.flink;

import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.commons.collections.CollectionUtils;
import org.apache.flink.table.api.TableColumn;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.api.ValidationException;
import org.apache.flink.table.api.WatermarkSpec;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.utils.TypeConversions;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.flink.table.runtime.typeutils.TypeCheckUtils.isTimestamp;
import static org.apache.flink.table.types.logical.utils.LogicalTypeChecks.getPrecision;

/**
 * An util that converts flink table schema.
 */
public class FlinkSchemaUtil {

  private static final Logger LOG = LoggerFactory.getLogger(FlinkSchemaUtil.class);

  /**
   * Convert a {@link RowType} to a {@link TableSchema}.
   *
   * @param rowType     a RowType
   * @param primaryKeys
   * @return Flink TableSchema
   */
  public static TableSchema toSchema(RowType rowType, List<String> primaryKeys) {
    TableSchema.Builder builder = TableSchema.builder();
    for (RowType.RowField field : rowType.getFields()) {
      builder.field(field.getName(), TypeConversions.fromLogicalToDataType(field.getType()));
    }
    if (CollectionUtils.isNotEmpty(primaryKeys)) {
      builder.primaryKey(primaryKeys.toArray(new String[0]));
    }
    return builder.build();
  }

  /**
   * Add watermark info to help {@link com.netease.arctic.flink.table.FlinkSource}
   * and {@link com.netease.arctic.flink.table.ArcticDynamicSource} distinguish the watermark field.
   * For now, it only be used in the case of Arctic as dim-table.
   */
  public static TableSchema getPhysicalSchema(TableSchema tableSchema, boolean addWatermark) {
    if (!addWatermark) {
      return tableSchema;
    }
    TableSchema.Builder builder = filter(tableSchema, TableColumn::isPhysical);
    tableSchema.getWatermarkSpecs().forEach(builder::watermark);
    return builder.build();
  }

  /**
   * filter watermark due to watermark is a virtual field for now, not in arctic physical table.
   */
  public static TableSchema filterWatermark(TableSchema tableSchema) {
    List<WatermarkSpec> watermarkSpecs = tableSchema.getWatermarkSpecs();
    if (watermarkSpecs.isEmpty()) {
      return tableSchema;
    }

    Function<TableColumn, Boolean> filter = (tableColumn) -> {
      boolean isWatermark = false;
      for (WatermarkSpec spec : watermarkSpecs) {
        if (spec.getRowtimeAttribute().equals(tableColumn.getName())) {
          isWatermark = true;
          final LogicalType timeFieldType = tableColumn.getType().getLogicalType();
          validateWatermarkExpression(timeFieldType);
          break;
        }
      }
      return !isWatermark;
    };
    return filter(tableSchema, filter).build();
  }

  /**
   * If filter result is true, keep the column; otherwise, remove the column.
   */
  public static TableSchema.Builder filter(TableSchema tableSchema, Function<TableColumn, Boolean> filter) {
    TableSchema.Builder builder = TableSchema.builder();

    tableSchema
        .getTableColumns()
        .forEach(
            tableColumn -> {
              if (!filter.apply(tableColumn)) {
                return;
              }
              builder.field(tableColumn.getName(), tableColumn.getType());
            });
    tableSchema
        .getPrimaryKey()
        .ifPresent(
            uniqueConstraint ->
                builder.primaryKey(
                    uniqueConstraint.getName(),
                    uniqueConstraint.getColumns().toArray(new String[0])));
    return builder;
  }

  public static RowType toRowType(TableSchema tableSchema) {
    LogicalType[] fields = new LogicalType[tableSchema.getFieldCount()];

    for (int i = 0; i < fields.length; i++) {
      TableColumn tableColumn = tableSchema.getTableColumn(i).get();
      fields[i] = tableColumn.getType().getLogicalType();
    }
    return RowType.of(fields);
  }

  /**
   * Primary keys are the required fields to guarantee that readers can read keyed table in right order, due to the
   * automatic scaling in/out of nodes. The required fields should be added even though projection push down
   */
  @Deprecated
  public static List<Types.NestedField> addPrimaryKey(
      List<Types.NestedField> projectedColumns, ArcticTable table) {
    List<String> primaryKeys = table.isUnkeyedTable() ? Collections.EMPTY_LIST
        : table.asKeyedTable().primaryKeySpec().fields().stream()
        .map(PrimaryKeySpec.PrimaryKeyField::fieldName).collect(Collectors.toList());

    List<Types.NestedField> columns = new ArrayList<>(projectedColumns);
    Set<String> projectedNames = new HashSet<>();

    projectedColumns.forEach(c -> projectedNames.add(c.name()));

    primaryKeys.forEach(pk -> {
      if (!projectedNames.contains(pk)) {
        columns.add(table.schema().findField(pk));
      }
    });

    LOG.info("Projected Columns after addPrimaryKey, columns:{}", columns);
    return columns;
  }

  /**
   * Primary keys are the required fields to guarantee that readers can read keyed table in right order, due to the
   * automatic scaling in/out of nodes. The required fields should be added even though projection push down
   */
  @Deprecated
  public static void addPrimaryKey(
      TableSchema.Builder builder, ArcticTable table, TableSchema tableSchema, String[] projectedColumns) {
    Set<String> projectedNames = Arrays.stream(projectedColumns).collect(Collectors.toSet());

    if (!table.isKeyedTable()) {
      return;
    }

    List<String> pks = table.asKeyedTable().primaryKeySpec().fieldNames();
    pks.forEach(pk -> {
      if (projectedNames.contains(pk)) {
        return;
      }
      builder.field(pk, tableSchema.getFieldDataType(pk)
          .orElseThrow(() -> new ValidationException("Arctic primary key should be declared in table")));
    });
  }

  private static void validateWatermarkExpression(LogicalType watermarkType) {
    if (!isTimestamp(watermarkType) || getPrecision(watermarkType) > 3) {
      throw new ValidationException(
        String.format(
          "Invalid data type of expression for watermark definition. " +
            "The field must be of type TIMESTAMP(p)," +
            " the supported precision 'p' is from 0 to 3, but the watermark expression type is %s",
          watermarkType));
    }
  }

}
