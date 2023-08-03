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
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.scan.KeyedTableScanTask;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.mapping.NameMappingParser;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.parquet.ParquetValueReader;
import org.apache.iceberg.types.Type;
import org.apache.parquet.schema.MessageType;

import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Abstract implementation of arctic data reader consuming {@link KeyedTableScanTask}, return records
 * after filtering with {@link ArcticDeleteFilter}.
 *
 * @param <T> to indicate the record data type.
 */
public abstract class AbstractArcticDataReader<T> {

  protected final ArcticFileIO fileIO;
  protected final Schema tableSchema;
  protected final Schema projectedSchema;
  protected final String nameMapping;
  protected final boolean caseSensitive;
  protected final Set<DataTreeNode> sourceNodes;
  protected final BiFunction<Type, Object, Object> convertConstant;
  protected final PrimaryKeySpec primaryKeySpec;
  protected final boolean reuseContainer;
  protected StructLikeCollections structLikeCollections = StructLikeCollections.DEFAULT;

  public AbstractArcticDataReader(
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
    this(fileIO, tableSchema, projectedSchema, primaryKeySpec, nameMapping, caseSensitive,
        convertConstant, sourceNodes, reuseContainer);
    this.structLikeCollections = structLikeCollections;
  }

  public AbstractArcticDataReader(
      ArcticFileIO fileIO,
      Schema tableSchema,
      Schema projectedSchema,
      PrimaryKeySpec primaryKeySpec,
      String nameMapping,
      boolean caseSensitive,
      BiFunction<Type, Object, Object> convertConstant,
      boolean reuseContainer) {
    this(fileIO, tableSchema, projectedSchema, primaryKeySpec, nameMapping, caseSensitive,
        convertConstant, null, reuseContainer);
  }

  public AbstractArcticDataReader(
      ArcticFileIO fileIO,
      Schema tableSchema,
      Schema projectedSchema,
      PrimaryKeySpec primaryKeySpec,
      String nameMapping,
      boolean caseSensitive,
      BiFunction<Type, Object, Object> convertConstant,
      Set<DataTreeNode> sourceNodes, boolean reuseContainer) {
    this.fileIO = fileIO;
    this.tableSchema = tableSchema;
    this.projectedSchema = projectedSchema;
    this.primaryKeySpec = primaryKeySpec;
    this.nameMapping = nameMapping;
    this.caseSensitive = caseSensitive;
    this.convertConstant = convertConstant;
    this.sourceNodes = sourceNodes;
    this.reuseContainer = reuseContainer;
  }

  public CloseableIterator<T> readData(KeyedTableScanTask keyedTableScanTask) {
    ArcticDeleteFilter<T> arcticDeleteFilter =
        createArcticDeleteFilter(keyedTableScanTask, tableSchema, projectedSchema, primaryKeySpec,
            sourceNodes, structLikeCollections);
    Schema newProjectedSchema = arcticDeleteFilter.requiredSchema();

    CloseableIterable<T> dataIterable = arcticDeleteFilter.filter(
        CloseableIterable.concat(
            CloseableIterable.transform(
                CloseableIterable.withNoopClose(keyedTableScanTask.dataTasks()),
                fileScanTask -> newParquetIterable(
                    fileScanTask,
                    newProjectedSchema,
                    DataReaderCommon.getIdToConstant(fileScanTask, newProjectedSchema, convertConstant)))));
    return dataIterable.iterator();
  }

  //TODO Return deleted record produced by equality delete file only now, should refactor the reader to return all
  // deleted records.
  public CloseableIterator<T> readDeletedData(KeyedTableScanTask keyedTableScanTask) {
    boolean hasDeleteFile = keyedTableScanTask.arcticEquityDeletes().size() > 0 ||
        keyedTableScanTask.dataTasks().stream().anyMatch(arcticFileScanTask -> arcticFileScanTask.deletes().size() > 0);
    if (hasDeleteFile) {
      ArcticDeleteFilter<T> arcticDeleteFilter =
          createArcticDeleteFilter(keyedTableScanTask, tableSchema, projectedSchema, primaryKeySpec,
              sourceNodes, structLikeCollections);

      Schema newProjectedSchema = arcticDeleteFilter.requiredSchema();

      CloseableIterable<T> dataIterable = arcticDeleteFilter.filterNegate(
          CloseableIterable.concat(
              CloseableIterable.transform(
                  CloseableIterable.withNoopClose(keyedTableScanTask.dataTasks()),
                  fileScanTask ->
                      newParquetIterable(
                          fileScanTask,
                          newProjectedSchema,
                          DataReaderCommon.getIdToConstant(fileScanTask, newProjectedSchema, convertConstant)))));
      return dataIterable.iterator();
    } else {
      return CloseableIterator.empty();
    }
  }

  protected ArcticDeleteFilter<T> createArcticDeleteFilter(
      KeyedTableScanTask keyedTableScanTask, Schema tableSchema,
      Schema projectedSchema, PrimaryKeySpec primaryKeySpec,
      Set<DataTreeNode> sourceNodes, StructLikeCollections structLikeCollections
  ) {
    return new GenericArcticDeleteFilter(keyedTableScanTask, tableSchema, projectedSchema,
        primaryKeySpec, sourceNodes, structLikeCollections);
  }

  protected CloseableIterable<T> newParquetIterable(
      FileScanTask task, Schema schema, Map<Integer, ?> idToConstant) {
    Parquet.ReadBuilder builder = Parquet.read(fileIO.newInputFile(task.file().path().toString()))
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

    return fileIO.doAs(builder::build);
  }

  private class GenericArcticDeleteFilter extends ArcticDeleteFilter<T> {

    protected Function<T, StructLike> asStructLike;

    protected GenericArcticDeleteFilter(
        KeyedTableScanTask keyedTableScanTask,
        Schema tableSchema, Schema requestedSchema, PrimaryKeySpec primaryKeySpec) {
      super(keyedTableScanTask, tableSchema, requestedSchema, primaryKeySpec);
      this.asStructLike = AbstractArcticDataReader.this.toStructLikeFunction().apply(requiredSchema());
    }

    protected GenericArcticDeleteFilter(
        KeyedTableScanTask keyedTableScanTask,
        Schema tableSchema,
        Schema requestedSchema,
        PrimaryKeySpec primaryKeySpec,
        Set<DataTreeNode> sourceNodes,
        StructLikeCollections structLikeCollections) {
      super(keyedTableScanTask, tableSchema, requestedSchema, primaryKeySpec,
          sourceNodes, structLikeCollections);
      this.asStructLike = AbstractArcticDataReader.this.toStructLikeFunction().apply(requiredSchema());
    }

    protected GenericArcticDeleteFilter(
        KeyedTableScanTask keyedTableScanTask,
        Schema tableSchema,
        Schema requestedSchema,
        PrimaryKeySpec primaryKeySpec,
        Set<DataTreeNode> sourceNodes) {
      super(keyedTableScanTask, tableSchema, requestedSchema, primaryKeySpec, sourceNodes);
      this.asStructLike = AbstractArcticDataReader.this.toStructLikeFunction().apply(requiredSchema());
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

  protected abstract Function<MessageType, ParquetValueReader<?>> getNewReaderFunction(
      Schema projectSchema,
      Map<Integer, ?> idToConstant);

  protected abstract Function<Schema, Function<T, StructLike>> toStructLikeFunction();
}
