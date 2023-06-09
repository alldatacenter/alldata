/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.hive.io.reader;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.io.reader.AbstractArcticDataReader;
import com.netease.arctic.io.reader.ArcticDeleteFilter;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.mapping.NameMappingParser;
import org.apache.iceberg.parquet.AdaptHiveParquet;
import org.apache.iceberg.types.Type;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * AdaptHive can read all Data.
 */
public abstract class AbstractAdaptHiveArcticDataReader<T> extends AbstractArcticDataReader<T> {

  public AbstractAdaptHiveArcticDataReader(
      ArcticFileIO fileIO,
      Schema tableSchema,
      Schema projectedSchema,
      PrimaryKeySpec primaryKeySpec,
      String nameMapping,
      boolean caseSensitive,
      BiFunction<Type, Object, Object> convertConstant,
      Set<DataTreeNode> sourceNodes,
      boolean reuseContainer,
      StructLikeCollections structLikeCollections) {
    super(
        fileIO,
        tableSchema,
        projectedSchema,
        primaryKeySpec,
        nameMapping,
        caseSensitive,
        convertConstant,
        sourceNodes,
        reuseContainer,
        structLikeCollections);
  }

  public AbstractAdaptHiveArcticDataReader(
      ArcticFileIO fileIO,
      Schema tableSchema,
      Schema projectedSchema,
      PrimaryKeySpec primaryKeySpec,
      String nameMapping,
      boolean caseSensitive,
      BiFunction<Type, Object, Object> convertConstant,
      boolean reuseContainer) {
    super(
        fileIO,
        tableSchema,
        projectedSchema,
        primaryKeySpec,
        nameMapping,
        caseSensitive,
        convertConstant,
        reuseContainer);
  }

  public AbstractAdaptHiveArcticDataReader(
      ArcticFileIO fileIO,
      Schema tableSchema,
      Schema projectedSchema,
      PrimaryKeySpec primaryKeySpec,
      String nameMapping,
      boolean caseSensitive,
      BiFunction<Type, Object, Object> convertConstant,
      Set<DataTreeNode> sourceNodes,
      boolean reuseContainer) {
    super(
        fileIO,
        tableSchema,
        projectedSchema,
        primaryKeySpec,
        nameMapping,
        caseSensitive,
        convertConstant,
        sourceNodes,
        reuseContainer);
  }

  @Override
  protected ArcticDeleteFilter<T> createArcticDeleteFilter(
      KeyedTableScanTask keyedTableScanTask, Schema tableSchema,
      Schema projectedSchema, PrimaryKeySpec primaryKeySpec,
      Set<DataTreeNode> sourceNodes, StructLikeCollections structLikeCollections
  ) {
    return new AdaptHiveGenericArcticDeleteFilter(keyedTableScanTask, tableSchema, projectedSchema,
        primaryKeySpec, sourceNodes, structLikeCollections);
  }

  @Override
  protected CloseableIterable<T> newParquetIterable(
      FileScanTask task, Schema schema, Map<Integer, ?> idToConstant) {
    AdaptHiveParquet.ReadBuilder builder = AdaptHiveParquet.read(fileIO.newInputFile(task.file().path().toString()))
        .split(task.start(), task.length())
        .project(schema)
        .createReaderFunc(getNewReaderFunction(schema, idToConstant))
        .filter(task.residual())
        .caseSensitive(caseSensitive);

    if (reuseContainer) {
      builder.reuseContainers();
    }
    if (nameMapping != null) {
      builder.withNameMapping(NameMappingParser.fromJson(nameMapping));
    }

    return builder.build();
  }

  private class AdaptHiveGenericArcticDeleteFilter extends AdaptHiveArcticDeleteFilter<T> {

    protected Function<T, StructLike> asStructLike;

    protected AdaptHiveGenericArcticDeleteFilter(
        KeyedTableScanTask keyedTableScanTask,
        Schema tableSchema, Schema requestedSchema, PrimaryKeySpec primaryKeySpec) {
      super(keyedTableScanTask, tableSchema, requestedSchema, primaryKeySpec);
      this.asStructLike = AbstractAdaptHiveArcticDataReader.this.toStructLikeFunction().apply(requiredSchema());
    }

    protected AdaptHiveGenericArcticDeleteFilter(
        KeyedTableScanTask keyedTableScanTask,
        Schema tableSchema,
        Schema requestedSchema,
        PrimaryKeySpec primaryKeySpec,
        Set<DataTreeNode> sourceNodes,
        StructLikeCollections structLikeCollections) {
      super(keyedTableScanTask, tableSchema, requestedSchema, primaryKeySpec,
          sourceNodes, structLikeCollections);
      this.asStructLike = AbstractAdaptHiveArcticDataReader.this.toStructLikeFunction().apply(requiredSchema());
    }

    protected AdaptHiveGenericArcticDeleteFilter(
        KeyedTableScanTask keyedTableScanTask,
        Schema tableSchema,
        Schema requestedSchema,
        PrimaryKeySpec primaryKeySpec,
        Set<DataTreeNode> sourceNodes) {
      super(keyedTableScanTask, tableSchema, requestedSchema, primaryKeySpec, sourceNodes);
      this.asStructLike = AbstractAdaptHiveArcticDataReader.this.toStructLikeFunction().apply(requiredSchema());
    }

    @Override
    protected StructLike asStructLike(T record) {
      return asStructLike.apply(record);
    }

    @Override
    protected InputFile getInputFile(String location) {
      return fileIO.newInputFile(location);
    }

    @Override
    protected ArcticFileIO getArcticFileIo() {
      return fileIO;
    }
  }
}
