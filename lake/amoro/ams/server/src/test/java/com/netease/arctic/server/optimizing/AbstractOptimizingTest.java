/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.optimizing;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.server.AmsEnvironment;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.table.WriteOperationKind;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.data.FileHelpers;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.OutputFileFactory;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.util.Pair;
import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public abstract class AbstractOptimizingTest {
  protected static AmsEnvironment amsEnv = AmsEnvironment.getIntegrationInstances();

  @BeforeAll
  public static void before() throws Exception {
    amsEnv.start();
    amsEnv.startOptimizer();
  }

  @AfterAll
  public static void after() throws IOException {
    amsEnv.stop();
  }

  private static final Logger LOG = LoggerFactory.getLogger(AbstractOptimizingTest.class);

  protected static OffsetDateTime ofDateWithZone(int year, int mon, int day, int hour) {
    LocalDateTime dateTime = LocalDateTime.of(year, mon, day, hour, 0);
    return OffsetDateTime.of(dateTime, ZoneOffset.ofHours(0));
  }

  protected static OffsetDateTime quickDateWithZone(int day) {
    return ofDateWithZone(2022, 1, day, 0);
  }

  protected static Record newRecord(Schema schema, Object... val) {
    GenericRecord record = GenericRecord.create(schema);
    for (int i = 0; i < schema.columns().size(); i++) {
      record.set(i, val[i]);
    }
    return record;
  }

  protected static DataFile insertDataFile(Table table, List<Record> records, StructLike partitionData) throws
      IOException {
    OutputFileFactory outputFileFactory = OutputFileFactory.builderFor(table, 0, 1)
        .format(FileFormat.PARQUET).build();
    DataFile dataFile = FileHelpers.writeDataFile(
        table,
        outputFileFactory.newOutputFile(partitionData).encryptingOutputFile(),
        partitionData,
        records);

    AppendFiles baseAppend = table.newAppend();
    baseAppend.appendFile(dataFile);
    baseAppend.commit();

    return dataFile;
  }

  protected static long getDataFileSize(Table table) {
    table.refresh();
    try (CloseableIterable<FileScanTask> fileScanTasks = table.newScan().planFiles()) {
      DataFile file = fileScanTasks.iterator().next().file();
      LOG.info("get file size {} of {}", file.fileSizeInBytes(), file.path());
      return file.fileSizeInBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to close table scan of " + table.name(), e);
    }
  }

  protected static void updateProperties(Table table, String key, String value) {
    UpdateProperties updateProperties = table.updateProperties();
    updateProperties.set(key, value);
    updateProperties.commit();
  }

  protected static void updateProperties(ArcticTable table, String key, String value) {
    UpdateProperties updateProperties = table.updateProperties();
    updateProperties.set(key, value);
    updateProperties.commit();
  }

  protected static void rowDelta(Table table, List<Record> insertRecords, List<Record> deleteRecords,
                                 StructLike partitionData)
      throws IOException {
    // DataFile dataFile = writeNewDataFile(table, insertRecords, partitionData);
    OutputFileFactory outputFileFactory = OutputFileFactory.builderFor(table, 0, 1)
        .format(FileFormat.PARQUET).build();
    DataFile dataFile = FileHelpers.writeDataFile(
        table,
        outputFileFactory.newOutputFile(partitionData).encryptingOutputFile(),
        partitionData,
        insertRecords);

    Schema eqDeleteRowSchema = table.schema().select("id");
    DeleteFile deleteFile = FileHelpers.writeDeleteFile(
        table,
        outputFileFactory.newOutputFile(partitionData).encryptingOutputFile(),
        partitionData,
        deleteRecords,
        eqDeleteRowSchema);

    // DeleteFile deleteFile = writeEqDeleteFile(table, deleteRecords, partitionData);
    RowDelta rowDelta = table.newRowDelta();
    rowDelta.addRows(dataFile);
    rowDelta.addDeletes(deleteFile);
    rowDelta.validateFromSnapshot(table.currentSnapshot().snapshotId());
    rowDelta.commit();
  }

  protected static DeleteFile insertEqDeleteFiles(Table table, List<Record> records, StructLike partitionData)
      throws IOException {
    Schema eqDeleteRowSchema = table.schema().select("id");
    OutputFileFactory outputFileFactory = OutputFileFactory.builderFor(table, 0, 1)
        .format(FileFormat.PARQUET).build();
    DeleteFile deleteFile = FileHelpers.writeDeleteFile(
        table,
        outputFileFactory.newOutputFile(partitionData).encryptingOutputFile(),
        partitionData,
        records,
        eqDeleteRowSchema);
    // DeleteFile result = writeEqDeleteFile(table, records, partitionData);

    RowDelta rowDelta = table.newRowDelta();
    rowDelta.addDeletes(deleteFile);
    rowDelta.commit();
    return deleteFile;
  }

  protected static void rowDeltaWithPos(Table table, List<Record> insertRecords, List<Record> deleteRecords,
                                        StructLike partitionData) throws IOException {
    OutputFileFactory outputFileFactory = OutputFileFactory.builderFor(table, 0, 1)
        .format(FileFormat.PARQUET).build();

    DataFile dataFile = FileHelpers.writeDataFile(
        table,
        outputFileFactory.newOutputFile(partitionData).encryptingOutputFile(),
        partitionData,
        insertRecords);

    Schema eqDeleteRowSchema = table.schema().select("id");
    DeleteFile deleteFile = FileHelpers.writeDeleteFile(
        table,
        outputFileFactory.newOutputFile(partitionData).encryptingOutputFile(),
        partitionData,
        deleteRecords,
        eqDeleteRowSchema);

    List<Pair<CharSequence, Long>> file2Positions = Lists.newArrayList();
    file2Positions.add(Pair.of(dataFile.path().toString(), 0L));

    DeleteFile posDeleteFile = FileHelpers.writeDeleteFile(
        table,
        outputFileFactory.newOutputFile(partitionData).encryptingOutputFile(),
        partitionData,
        file2Positions).first();
    RowDelta rowDelta = table.newRowDelta();
    rowDelta.addRows(dataFile);
    rowDelta.addDeletes(deleteFile);
    rowDelta.addDeletes(posDeleteFile);
    rowDelta.validateFromSnapshot(table.currentSnapshot().snapshotId());
    rowDelta.commit();
  }

  protected static void writeChange(KeyedTable table, List<Record> insertRows, List<Record> deleteRows) {
    List<DataFile> insertFiles = write(insertRows, table, ChangeAction.INSERT, null);
    List<DataFile> deleteFiles = write(deleteRows, table, ChangeAction.DELETE, null);
    AppendFiles appendFiles = table.changeTable().newAppend();
    insertFiles.forEach(appendFiles::appendFile);
    deleteFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
  }

  protected static void writeChangeWithTxId(KeyedTable table, List<Record> insertRows, List<Record> deleteRows,
                                            long txId) {
    DataTestHelpers.writeChangeStore(table, txId, ChangeAction.INSERT, insertRows, false);
    // DataTestHelpers.writeChangeStore(table, txId, ChangeAction.DELETE, deleteRows, false);
    List<DataFile> insertFiles = write(insertRows, table, ChangeAction.INSERT, txId);
    List<DataFile> deleteFiles = write(deleteRows, table, ChangeAction.DELETE, txId);
    AppendFiles appendFiles = table.changeTable().newAppend();
    insertFiles.forEach(appendFiles::appendFile);
    deleteFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
  }

  protected static List<DataFile> writeBase(ArcticTable table, List<Record> insertRows) {
    UnkeyedTable baseTable;
    if (table.isUnkeyedTable()) {
      baseTable = table.asUnkeyedTable();
    } else {
      baseTable = table.asKeyedTable().baseTable();
    }
    List<DataFile> insertFiles = write(baseTable, insertRows);
    AppendFiles appendFiles = baseTable.newAppend();
    insertFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
    return insertFiles;
  }

  protected static List<DataFile> write(UnkeyedTable table, List<Record> rows) {
    if (rows != null && !rows.isEmpty()) {
      try (TaskWriter<Record> writer = AdaptHiveGenericTaskWriterBuilder.builderFor(table)
          .withChangeAction(ChangeAction.INSERT)
          .buildWriter(WriteOperationKind.APPEND)) {
        rows.forEach(row -> {
          try {
            writer.write(row);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
        return Arrays.asList(writer.complete().dataFiles());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Collections.emptyList();
  }

  protected static List<DataFile> write(List<Record> rows, KeyedTable table, ChangeAction action, Long txId) {
    if (rows != null && !rows.isEmpty()) {
      try (TaskWriter<Record> writer = AdaptHiveGenericTaskWriterBuilder.builderFor(table)
          .withChangeAction(action)
          .withTransactionId(txId)
          .buildWriter(WriteOperationKind.APPEND)) {
        rows.forEach(row -> {
          try {
            writer.write(row);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
        return Arrays.asList(writer.complete().dataFiles());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return Collections.emptyList();
  }

  protected static List<Record> readRecords(Table table) {
    List<Record> result = new ArrayList<>();
    try (CloseableIterable<Record> records = IcebergGenerics.read(table).select("id").build()) {
      for (Record record : records) {
        result.add(record);
      }
    } catch (IOException e) {
      LOG.warn("{} failed to close reader", table.name(), e);
    }
    return result;
  }

  protected static List<Record> readRecords(KeyedTable keyedTable) {
    return DataTestHelpers.readKeyedTable(keyedTable, null);
  }

  protected static void assertIds(List<Record> actualRows, Object... expectIds) {
    assertRecordValues(actualRows, 0, expectIds);
  }

  protected static void assertIdRange(List<Record> actualRows, int from, int to) {
    assertRecordValues(actualRows, 0, range(from,to).toArray());
  }

  protected static List<Integer> range(int from, int to) {
    List<Integer> ids = new ArrayList<>();
    for (int i = from; i <= to; i++) {
      ids.add(i);
    }
    return ids;
  }

  protected static void assertNames(List<Record> actualRows, Object... expectNames) {
    assertRecordValues(actualRows, 1, expectNames);
  }

  protected static void assertRecordValues(List<Record> actualRows, int index, Object... expectValues) {
    Set<Object> actualValueSet = Sets.newHashSet();
    int cnt = 0;
    for (Record r : actualRows) {
      actualValueSet.add(r.get(index));
      cnt++;
    }
    Assert.assertEquals(cnt, expectValues.length);
    for (Object id : expectValues) {
      if (!actualValueSet.contains(id)) {
        throw new AssertionError("assert id contain " + id + ", but not found");
      }
    }
  }
}
