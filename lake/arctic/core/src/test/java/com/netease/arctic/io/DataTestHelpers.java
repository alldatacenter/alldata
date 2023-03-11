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

import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.reader.GenericArcticDataReader;
import com.netease.arctic.io.writer.GenericBaseTaskWriter;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.table.KeyedTable;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.Files;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
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
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.orc.ORC;
import org.apache.iceberg.parquet.Parquet;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.transforms.Transform;
import org.apache.iceberg.transforms.Transforms;
import org.apache.iceberg.types.Types;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class DataTestHelpers {

  private static final GenericRecord TEST_RECORD = GenericRecord.create(TableTestHelpers.TABLE_SCHEMA);

  public static Record createRecord(int id, String name, long ts, String opTime) {
    Map<String, Object> overwriteValues = Maps.newHashMapWithExpectedSize(4);
    overwriteValues.put("id", id);
    overwriteValues.put("name", name);
    overwriteValues.put("ts", ts);
    overwriteValues.put("op_time", LocalDateTime.parse(opTime));
    return TEST_RECORD.copy(overwriteValues);
  }

  public static StructLike recordPartition(String opTime) {
    Transform<Long, Integer> day = Transforms.day(Types.TimestampType.withoutZone());

    Literal<Long> ts = Literal.of(opTime).to(Types.TimestampType.withoutZone());
    Object tsDay = day.apply(ts.value());

    return TestHelpers.Row.of(tsDay);
  }

  public static List<DataFile> writeChangeStore(
      KeyedTable keyedTable, long txId, ChangeAction action,
      List<Record> records) {
    try (GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(keyedTable)
        .withChangeAction(action)
        .withTransactionId(txId)
        .buildChangeWriter()) {
      records.forEach(d -> {
        try {
          writer.write(d);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });

      WriteResult result = writer.complete();
      return Arrays.asList(result.dataFiles());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  public static List<DataFile> writeBaseStore(KeyedTable keyedTable, long txId, List<Record> records) {
    try (GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(keyedTable)
        .withTransactionId(txId).buildBaseWriter()) {
      records.forEach(d -> {
        try {
          writer.write(d);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });
      WriteResult result = writer.complete();
      return Arrays.asList(result.dataFiles());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static List<DataFile> writeAndCommitChangeStore(
      KeyedTable keyedTable, long txId, ChangeAction action,
      List<Record> records) {
    List<DataFile> writeFiles = writeChangeStore(keyedTable, txId, action, records);
    AppendFiles appendFiles = keyedTable.changeTable().newAppend();
    writeFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return writeFiles;
  }

  public static List<Record> readKeyedTable(KeyedTable keyedTable, Expression expression) {
    GenericArcticDataReader reader = new GenericArcticDataReader(
        keyedTable.io(),
        keyedTable.schema(),
        keyedTable.schema(),
        keyedTable.primaryKeySpec(),
        null,
        true,
        IdentityPartitionConverters::convertConstant
    );
    List<Record> result = Lists.newArrayList();
    try (CloseableIterable<CombinedScanTask> combinedScanTasks = keyedTable.newScan().filter(expression).planTasks()) {
      combinedScanTasks.forEach(combinedTask -> combinedTask.tasks().forEach(scTask -> {
        try (CloseableIterator<Record> records = reader.readData(scTask)) {
          while (records.hasNext()) {
            result.add(records.next());
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      }));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return result;
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
}
