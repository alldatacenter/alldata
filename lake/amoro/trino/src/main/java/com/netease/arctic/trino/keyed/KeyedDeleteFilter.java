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

package com.netease.arctic.trino.keyed;

import com.netease.arctic.hive.io.reader.AdaptHiveArcticDeleteFilter;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.trino.delete.TrinoRow;
import io.trino.plugin.iceberg.IcebergColumnHandle;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.FileIO;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.types.Types;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static io.trino.plugin.iceberg.TypeConverter.toIcebergType;

/**
 * KeyedDeleteFilter is used to do MOR for Keyed Table
 */
public class KeyedDeleteFilter extends AdaptHiveArcticDeleteFilter<TrinoRow> {

  private FileIO fileIO;

  protected KeyedDeleteFilter(
      KeyedTableScanTask keyedTableScanTask,
      Schema tableSchema,
      List<IcebergColumnHandle> requestedSchema,
      PrimaryKeySpec primaryKeySpec,
      FileIO fileIO) {
    super(keyedTableScanTask, tableSchema, filterSchema(tableSchema, requestedSchema), primaryKeySpec);
    this.fileIO = fileIO;
  }

  @Override
  protected StructLike asStructLike(TrinoRow record) {
    return record;
  }

  @Override
  protected InputFile getInputFile(String location) {
    return fileIO.newInputFile(location);
  }

  private static Schema filterSchema(Schema tableSchema, List<IcebergColumnHandle> requestedColumns) {
    return new Schema(filterFieldList(tableSchema.columns(), requestedColumns));
  }

  private static List<Types.NestedField> filterFieldList(
      List<Types.NestedField> fields,
      List<IcebergColumnHandle> requestedSchemas) {
    return requestedSchemas.stream()
        .map(id -> filterField(id, fields))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toImmutableList());
  }

  private static Optional<Types.NestedField> filterField(
      IcebergColumnHandle requestedSchema, List<Types.NestedField> fields) {
    for (Types.NestedField nestedField: fields) {
      if (nestedField.fieldId() == requestedSchema.getId()) {
        return Optional.of(nestedField);
      }
      if (nestedField.type().isStructType()) {
        Optional<Types.NestedField> optional = filterField(requestedSchema, nestedField.type().asStructType().fields());
        if (optional.isPresent()) {
          return optional;
        }
      }
    }
    return Optional.of(Types.NestedField.optional(requestedSchema.getId(), requestedSchema.getName(),
        toIcebergType(requestedSchema.getType())));
  }
}
