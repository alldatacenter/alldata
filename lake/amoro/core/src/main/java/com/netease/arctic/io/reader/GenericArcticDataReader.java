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

package com.netease.arctic.io.reader;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.iceberg.InternalRecordWrapper;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.parquet.GenericParquetReaders;
import org.apache.iceberg.parquet.ParquetValueReader;
import org.apache.iceberg.types.Type;
import org.apache.parquet.schema.MessageType;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Implementation of {@link AbstractArcticDataReader} with record type {@link Record}.
 */
public class GenericArcticDataReader extends AbstractArcticDataReader<Record> {

  public GenericArcticDataReader(
      ArcticFileIO fileIO,
      Schema tableSchema,
      Schema projectedSchema,
      PrimaryKeySpec primaryKeySpec,
      String nameMapping,
      boolean caseSensitive,
      BiFunction<Type, Object, Object> convertConstant) {
    super(fileIO, tableSchema, projectedSchema, primaryKeySpec, nameMapping, caseSensitive, convertConstant, false);
  }

  public GenericArcticDataReader(
      ArcticFileIO fileIO,
      Schema tableSchema,
      Schema projectedSchema,
      PrimaryKeySpec primaryKeySpec,
      String nameMapping,
      boolean caseSensitive,
      BiFunction<Type, Object, Object> convertConstant,
      Set<DataTreeNode> sourceNodes, boolean reuseContainer,
      StructLikeCollections structLikeCollections) {
    super(fileIO, tableSchema, projectedSchema, primaryKeySpec,
        nameMapping, caseSensitive, convertConstant, sourceNodes, reuseContainer, structLikeCollections);
  }

  public GenericArcticDataReader(
      ArcticFileIO fileIO,
      Schema tableSchema,
      Schema projectedSchema,
      PrimaryKeySpec primaryKeySpec,
      String nameMapping,
      boolean caseSensitive,
      BiFunction<Type, Object, Object> convertConstant,
      Set<DataTreeNode> sourceNodes, boolean reuseContainer) {
    super(fileIO, tableSchema, projectedSchema, primaryKeySpec,
        nameMapping, caseSensitive, convertConstant, sourceNodes, reuseContainer);
  }

  @Override
  protected Function<MessageType, ParquetValueReader<?>> getNewReaderFunction(
      Schema projectSchema,
      Map<Integer, ?> idToConstant) {
    return fileSchema -> GenericParquetReaders.buildReader(projectSchema, fileSchema, idToConstant);
  }

  @Override
  protected Function<Schema, Function<Record, StructLike>> toStructLikeFunction() {
    return schema -> record -> new InternalRecordWrapper(schema.asStruct()).wrap(record);
  }
}
