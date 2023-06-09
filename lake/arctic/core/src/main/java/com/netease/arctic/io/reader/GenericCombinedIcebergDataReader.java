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

import com.netease.arctic.data.file.DataFileWithSequence;
import com.netease.arctic.iceberg.CombinedDeleteFilter;
import com.netease.arctic.iceberg.optimize.InternalRecordWrapper;
import com.netease.arctic.io.ArcticFileIO;
import com.netease.arctic.scan.CombinedIcebergScanTask;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.avro.Avro;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.avro.DataReader;
import org.apache.iceberg.data.orc.GenericOrcReader;
import org.apache.iceberg.data.parquet.GenericParquetReaders;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.types.Type;

import java.util.Map;
import java.util.function.BiFunction;

/**
 * Read data by {@link CombinedIcebergScanTask} for optimizer of native iceberg.
 */
public class GenericCombinedIcebergDataReader {

  protected final Schema tableSchema;
  protected final Schema projectedSchema;
  protected final String nameMapping;
  protected final boolean caseSensitive;
  protected final ArcticFileIO fileIO;
  protected final BiFunction<Type, Object, Object> convertConstant;
  protected final boolean reuseContainer;

  protected StructLikeCollections structLikeCollections = StructLikeCollections.DEFAULT;

  public GenericCombinedIcebergDataReader(
      ArcticFileIO fileIO, Schema tableSchema, Schema projectedSchema,
      String nameMapping, boolean caseSensitive, BiFunction<Type, Object, Object> convertConstant,
      boolean reuseContainer,
      StructLikeCollections structLikeCollections) {
    this(fileIO, tableSchema, projectedSchema, nameMapping, caseSensitive, convertConstant, reuseContainer);
    this.structLikeCollections = structLikeCollections;
  }

  public GenericCombinedIcebergDataReader(
      ArcticFileIO fileIO, Schema tableSchema, Schema projectedSchema,
      String nameMapping, boolean caseSensitive, BiFunction<Type, Object, Object> convertConstant,
      boolean reuseContainer) {
    this.tableSchema = tableSchema;
    this.projectedSchema = projectedSchema;
    this.nameMapping = nameMapping;
    this.caseSensitive = caseSensitive;
    this.fileIO = fileIO;
    this.convertConstant = convertConstant;
    this.reuseContainer = reuseContainer;
  }

  public CloseableIterable<Record> readData(CombinedIcebergScanTask task) {
    CombinedDeleteFilter<Record> deleteFilter =
        new GenericDeleteFilter(task, tableSchema, projectedSchema, structLikeCollections);

    CloseableIterable<Record> concat = CloseableIterable.concat(CloseableIterable.transform(
        CloseableIterable.withNoopClose(task.getDataFiles()),
        s -> openFile(s,
            task.getPartitionSpec(), deleteFilter.requiredSchema())));

    CloseableIterable<Record> iterable = deleteFilter.filter(concat);
    return iterable;
  }

  public CloseableIterable<Record> readDeleteData(CombinedIcebergScanTask task) {
    CombinedDeleteFilter<Record> deleteFilter =
        new GenericDeleteFilter(task, tableSchema, projectedSchema, structLikeCollections);

    CloseableIterable<Record> concat = CloseableIterable.concat(CloseableIterable.transform(
        CloseableIterable.withNoopClose(task.getDataFiles()),
        s -> openFile(s,
            task.getPartitionSpec(), deleteFilter.requiredSchema())));

    CloseableIterable<Record> iterable = deleteFilter.filterNegate(concat);
    return iterable;
  }

  private CloseableIterable<Record> openFile(
      DataFileWithSequence dataFileWithSequence,
      PartitionSpec spec, Schema require) {
    Map<Integer, ?> idToConstant = DataReaderCommon.getIdToConstant(dataFileWithSequence, projectedSchema, spec,
        convertConstant);

    return openFile(dataFileWithSequence, require, idToConstant);
  }

  private CloseableIterable<Record> openFile(DataFile dataFile, Schema fileProjection, Map<Integer, ?> idToConstant) {
    InputFile input = fileIO.newInputFile(dataFile.path().toString());

    switch (dataFile.format()) {
      case AVRO:
        Avro.ReadBuilder avro = Avro.read(input)
            .project(fileProjection)
            .createReaderFunc(
                avroSchema -> DataReader.create(fileProjection, avroSchema, idToConstant));

        if (reuseContainer) {
          avro.reuseContainers();
        }

        return avro.build();

      case PARQUET:
        Parquet.ReadBuilder parquet = Parquet.read(input)
            .project(fileProjection)
            .createReaderFunc(fileSchema -> GenericParquetReaders.buildReader(fileProjection, fileSchema,
                idToConstant));

        if (reuseContainer) {
          parquet.reuseContainers();
        }

        return parquet.build();

      case ORC:
        org.apache.iceberg.orc.ORC.ReadBuilder orc = org.apache.iceberg.orc.ORC.read(input)
            .project(projectedSchema)
            .createReaderFunc(fileSchema -> GenericOrcReader.buildReader(fileProjection, fileSchema, idToConstant));
        return orc.build();

      default:
        throw new UnsupportedOperationException(String.format("Cannot read %s file: %s",
            dataFile.format().name(), dataFile.path()));
    }
  }

  protected class GenericDeleteFilter extends CombinedDeleteFilter<Record> {

    private InternalRecordWrapper internalRecordWrapper;

    protected GenericDeleteFilter(
        CombinedIcebergScanTask task,
        Schema tableSchema,
        Schema requestedSchema,
        StructLikeCollections structLikeCollections) {
      super(task, tableSchema, requestedSchema, structLikeCollections);
      internalRecordWrapper = new InternalRecordWrapper(requiredSchema().asStruct());
    }

    protected GenericDeleteFilter(
        CombinedIcebergScanTask task,
        Schema tableSchema,
        Schema requestedSchema) {
      super(task, tableSchema, requestedSchema);
      internalRecordWrapper = new InternalRecordWrapper(requiredSchema().asStruct());
    }

    @Override
    protected StructLike asStructLike(Record row) {
      return internalRecordWrapper.copyFor(row);
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
