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

package com.netease.arctic.utils;

import com.netease.arctic.table.MetadataColumns;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.TypeUtil;
import org.apache.iceberg.types.Types;

import java.util.List;
import java.util.Set;

public class SchemaUtil {

  public static Schema changeWriteSchema(Schema changeTableSchema) {
    Schema changeWriteMetaColumnsSchema = new Schema(
        MetadataColumns.FILE_OFFSET_FILED
    );
    return TypeUtil.join(changeTableSchema, changeWriteMetaColumnsSchema);
  }

  /**
   * Convert an Iceberg Schema {@link Schema} to a {@link Schema} based on the given schema.
   * <p>
   * This fill-up does not assign new ids; it uses ids from the base schema.
   * <p>
   * If the fromSchema does contain the identifierFields of the based schema, it will fill-up the identifierFields to
   * a new schema.
   *
   * @param baseSchema a Schema on which loading is based
   * @param fromSchema a Schema on which compared to
   * @return a new Schema on which contain the identifier fields of the base Schema and column fields of the fromSchema
   */
  public static Schema fillUpIdentifierFields(Schema baseSchema, Schema fromSchema, PrimaryKeySpec primaryKeySpec) {
    int schemaId = fromSchema.schemaId();
    Types.StructType struct = fromSchema.asStruct();
    List<Types.NestedField> fields = Lists.newArrayList(struct.fields());
    Set<Integer> identifierFieldIds = Sets.newHashSet(baseSchema.identifierFieldIds());
    primaryKeySpec.fields().stream()
        .map(PrimaryKeySpec.PrimaryKeyField::fieldName)
        .forEach(p -> identifierFieldIds.add(baseSchema.findField(p).fieldId()));

    identifierFieldIds.forEach(fieldId -> {
      if (struct.field(fieldId) == null) {
        fields.add(baseSchema.findField(fieldId));
      }
    });

    return new Schema(schemaId, fields, identifierFieldIds);
  }
}
