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

package com.netease.arctic.io;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.data.FileNameRules;
import com.netease.arctic.io.reader.AbstractArcticDataReader;
import com.netease.arctic.io.reader.AbstractIcebergDataReader;
import com.netease.arctic.io.reader.GenericArcticDataReader;
import com.netease.arctic.io.reader.GenericIcebergDataReader;
import com.netease.arctic.io.writer.GenericBaseTaskWriter;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.ChangeTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.MetadataColumns;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.ArcticTableUtil;
import com.netease.arctic.utils.map.StructLikeCollections;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.Files;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.TestHelpers;
import org.apache.iceberg.avro.Avro;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.IdentityPartitionConverters;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.data.avro.DataReader;
import org.apache.iceberg.data.orc.GenericOrcReader;
import org.apache.iceberg.data.parquet.GenericParquetReaders;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Literal;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.InputFile;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.orc.ORC;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.transforms.Transform;
import org.apache.iceberg.transforms.Transforms;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

public class DataTestHelpers {

  public static Record createRecord(int id, String name, long ts, String opTime) {
    return createRecord(BasicTableTestHelper.TABLE_SCHEMA, id, name, ts, opTime);
  }

  public static Record createRecord(Schema recordSchema, Object... values) {
    Preconditions.checkArgument(
        recordSchema.columns().size() == values.length,
        "The number of values in the record does not match the number of fields in the schema");
    GenericRecord record = GenericRecord.create(recordSchema);
    for (int i = 0; i < recordSchema.columns().size(); i++) {
      record.set(i, adaptRecordValueByType(recordSchema.columns().get(i).type(), values[i]));
    }
    return record;
  }

  private static Object adaptRecordValueByType(Type type, Object value) {
    if (Types.TimestampType.withoutZone().equals(type) && value instanceof String) {
      return LocalDateTime.parse((String) value);
    } else if (Types.TimestampType.withZone().equals(type) && value instanceof String) {
      return OffsetDateTime.parse((String) value);
    }
    return value;
  }

  public static StructLike recordPartition(String opTime) {
    Transform<Long, Integer> day = Transforms.day(Types.TimestampType.withoutZone());

    Literal<Long> ts = Literal.of(opTime).to(Types.TimestampType.withoutZone());
    Object tsDay = day.apply(ts.value());

    return TestHelpers.Row.of(tsDay);
  }

  public static List<DataFile> writeChangeStore(
      KeyedTable keyedTable, Long txId, ChangeAction action,
      List<Record> records, boolean orderedWrite) {
    GenericTaskWriters.Builder builder = GenericTaskWriters.builderFor(keyedTable)
        .withChangeAction(action)
        .withTransactionId(txId);
    if (orderedWrite) {
      builder.withOrdered();
    }
    try (GenericChangeTaskWriter writer = builder.buildChangeWriter()) {
      return writeRecords(writer, records);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<DataFile> writeRecords(
      TaskWriter<Record> taskWriter, List<Record> records) {
    try {
      records.forEach(d -> {
        try {
          taskWriter.write(d);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });

      WriteResult result = taskWriter.complete();
      return Arrays.asList(result.dataFiles());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<DataFile> writeBaseStore(
      ArcticTable table, long txId, List<Record> records,
      boolean orderedWrite) {
    GenericTaskWriters.Builder builder = GenericTaskWriters.builderFor(table);
    if (table.isKeyedTable()) {
      builder.withTransactionId(txId);
    }
    if (orderedWrite) {
      builder.withOrdered();
    }
    try (GenericBaseTaskWriter writer = builder.buildBaseWriter()) {
      return writeRecords(writer, records);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<DataFile> writeIceberg(Table table, List<Record> records) {
    try (TaskWriter<Record> writer = IcebergTaskWriters.buildFor(table)) {
      return writeRecords(writer, records);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<DeleteFile> writeBaseStorePosDelete(
      ArcticTable table, long txId, DataFile dataFile, List<Long> pos) {
    GenericTaskWriters.Builder builder = GenericTaskWriters.builderFor(table);
    DataTreeNode node = FileNameRules.parseFileNodeFromFileName(dataFile.path().toString());
    if (table.isKeyedTable()) {
      builder.withTransactionId(txId);
    }
    try (SortedPosDeleteWriter<Record> writer = builder.buildBasePosDeleteWriter(node.mask(), node.index(),
        dataFile.partition())) {
      for (Long p : pos) {
        writer.delete(dataFile.path().toString(), p);
      }
      return writer.complete();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<DataFile> writeAndCommitBaseStore(
      ArcticTable table, long txId, List<Record> records,
      boolean orderedWrite) {
    List<DataFile> dataFiles = writeBaseStore(table, txId, records, orderedWrite);
    AppendFiles appendFiles;
    if (table.isKeyedTable()) {
      appendFiles = table.asKeyedTable().baseTable().newAppend();
    } else {
      appendFiles = table.asUnkeyedTable().newAppend();
    }
    dataFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return dataFiles;
  }

  public static List<DataFile> writeAndCommitChangeStore(
      KeyedTable keyedTable, Long txId, ChangeAction action,
      List<Record> records) {
    List<DataFile> writeFiles = writeChangeStore(keyedTable, txId, action, records, false);
    AppendFiles appendFiles = keyedTable.changeTable().newAppend();
    writeFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return writeFiles;
  }

  public static List<Record> readKeyedTable(KeyedTable keyedTable, Expression expression) {
    return readKeyedTable(keyedTable, expression, null, false, false);
  }

  public static List<Record> readKeyedTable(
      KeyedTable keyedTable, Expression expression,
      Schema projectSchema, boolean useDiskMap, boolean readDeletedData) {
    GenericArcticDataReader reader;
    if (projectSchema == null) {
      projectSchema = keyedTable.schema();
    }
    if (useDiskMap) {
      reader = new GenericArcticDataReader(
          keyedTable.io(),
          keyedTable.schema(),
          projectSchema,
          keyedTable.primaryKeySpec(),
          null,
          true,
          IdentityPartitionConverters::convertConstant,
          null, false, new StructLikeCollections(true, 0L)
      );
    } else {
      reader = new GenericArcticDataReader(
          keyedTable.io(),
          keyedTable.schema(),
          projectSchema,
          keyedTable.primaryKeySpec(),
          null,
          true,
          IdentityPartitionConverters::convertConstant
      );
    }

    return readKeyedTable(keyedTable, reader, expression, projectSchema, readDeletedData);
  }

  public static List<Record> readKeyedTable(
      KeyedTable keyedTable,
      AbstractArcticDataReader<Record> reader, Expression expression,
      Schema projectSchema, boolean readDeletedData) {

    List<Record> result = Lists.newArrayList();
    final Schema expectSchema = projectSchema;
    try (CloseableIterable<CombinedScanTask> combinedScanTasks = keyedTable.newScan().filter(expression).planTasks()) {
      combinedScanTasks.forEach(combinedTask -> combinedTask.tasks().forEach(scTask -> {
        CloseableIterator<Record> records;
        if (readDeletedData) {
          records = reader.readDeletedData(scTask);
        } else {
          records = reader.readData(scTask);
        }
        try {
          while (records.hasNext()) {
            Record record = projectMetadataRecord(records.next(), expectSchema);
            result.add(record);
          }
        } finally {
          if (records != null) {
            try {
              records.close();
            } catch (IOException e) {
              // ignore
            }
          }
        }
      }));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return result;
  }

  public static List<Record> readChangeStore(
      KeyedTable keyedTable, Expression expression, Schema projectSchema,
      boolean useDiskMap) {
    if (projectSchema == null) {
      projectSchema = keyedTable.schema();
    }
    Schema expectTableSchema = MetadataColumns.appendChangeStoreMetadataColumns(keyedTable.schema());
    Schema expectProjectSchema = MetadataColumns.appendChangeStoreMetadataColumns(projectSchema);

    GenericIcebergDataReader reader;
    if (useDiskMap) {
      reader = new GenericIcebergDataReader(
          keyedTable.asKeyedTable().io(),
          expectTableSchema,
          expectProjectSchema,
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false,
          new StructLikeCollections(true, 0L));
    } else {
      reader = new GenericIcebergDataReader(
          keyedTable.asKeyedTable().io(),
          expectTableSchema,
          expectProjectSchema,
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false
      );
    }

    return readChangeStore(keyedTable, reader, expression);
  }

  public static List<Record> readChangeStore(
      KeyedTable keyedTable, AbstractIcebergDataReader<Record> reader,
      Expression expression) {

    ChangeTable changeTable = keyedTable.asKeyedTable().changeTable();
    CloseableIterable<FileScanTask> fileScanTasks = changeTable.newScan().filter(expression).planFiles();
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (FileScanTask fileScanTask : fileScanTasks) {
      builder.addAll(reader.readData(fileScanTask));
    }
    return builder.build();
  }

  public static List<Record> readBaseStore(
      ArcticTable table, Expression expression, Schema projectSchema,
      boolean useDiskMap) {
    if (projectSchema == null) {
      projectSchema = table.schema();
    }

    GenericIcebergDataReader reader;
    if (useDiskMap) {
      reader = new GenericIcebergDataReader(
          table.io(),
          table.schema(),
          projectSchema,
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false,
          new StructLikeCollections(true, 0L));
    } else {
      reader = new GenericIcebergDataReader(
          table.io(),
          table.schema(),
          projectSchema,
          null,
          false,
          IdentityPartitionConverters::convertConstant,
          false
      );
    }

    return readBaseStore(table, reader, expression);
  }

  public static List<Record> readBaseStore(
      ArcticTable table, AbstractIcebergDataReader<Record> reader,
      Expression expression) {

    UnkeyedTable baseStore = ArcticTableUtil.baseStore(table);
    CloseableIterable<FileScanTask> fileScanTasks = baseStore.newScan().filter(expression).planFiles();
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    for (FileScanTask fileScanTask : fileScanTasks) {
      builder.addAll(reader.readData(fileScanTask));
    }
    return builder.build();
  }

  private static Record projectMetadataRecord(Record record, Schema projectSchema) {
    GenericRecord projectRecord = GenericRecord.create(projectSchema);
    projectSchema.columns().forEach(nestedField ->
        projectRecord.setField(nestedField.name(), record.getField(nestedField.name())));
    return projectRecord;
  }

  public static List<Record> readDataFile(FileFormat format, Schema schema, CharSequence path) throws IOException {
    CloseableIterable<Record> iterable;

    InputFile inputFile = Files.localInput(path.toString());
    switch (format) {
      case PARQUET:
        iterable =
            Parquet.read(inputFile)
                .project(schema)
                .createReaderFunc(
                    fileSchema -> GenericParquetReaders.buildReader(schema, fileSchema))
                .build();
        break;

      case AVRO:
        iterable =
            Avro.read(inputFile).project(schema).createReaderFunc(DataReader::create).build();
        break;

      case ORC:
        iterable =
            ORC.read(inputFile)
                .project(schema)
                .createReaderFunc(fileSchema -> GenericOrcReader.buildReader(schema, fileSchema))
                .build();
        break;

      default:
        throw new UnsupportedOperationException("Unsupported file format: " + format);
    }

    try (CloseableIterable<Record> closeableIterable = iterable) {
      return Lists.newArrayList(closeableIterable);
    }
  }

  public static Record appendMetaColumnValues(
      Record sourceRecord, long transactionId, long offset,
      ChangeAction action) {
    Schema sourceSchema = new Schema(sourceRecord.struct().fields());
    Record expectRecord = GenericRecord.create(com.netease.arctic.table.MetadataColumns
        .appendChangeStoreMetadataColumns(sourceSchema));
    sourceRecord.struct().fields().forEach(nestedField ->
        expectRecord.setField(nestedField.name(), sourceRecord.getField(nestedField.name())));
    expectRecord.setField(com.netease.arctic.table.MetadataColumns.TRANSACTION_ID_FILED_NAME, transactionId);
    expectRecord.setField(com.netease.arctic.table.MetadataColumns.FILE_OFFSET_FILED_NAME, offset);
    expectRecord.setField(com.netease.arctic.table.MetadataColumns.CHANGE_ACTION_NAME, action.toString());
    return expectRecord;
  }
}
