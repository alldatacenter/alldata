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

package com.netease.arctic.ams.server.optimize;

import com.netease.arctic.TableTestBase;
import com.netease.arctic.ams.api.OptimizeType;
import com.netease.arctic.ams.server.model.OptimizeHistory;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.hive.io.writer.AdaptHiveGenericTaskWriterBuilder;
import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.table.ArcticTable;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.table.WriteOperationKind;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileScanTask;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.Table;
import org.apache.iceberg.UpdateProperties;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.relocated.com.google.common.collect.Multimap;
import org.apache.iceberg.relocated.com.google.common.collect.Multimaps;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.junit.Assert;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import static com.netease.arctic.TableTestBase.writeEqDeleteFile;
import static com.netease.arctic.TableTestBase.writeNewDataFile;
import static com.netease.arctic.TableTestBase.writePosDeleteFile;

public abstract class AbstractOptimizingTest {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractOptimizingTest.class);
  private static final long WAIT_SUCCESS_TIMEOUT = 30_000;
  private static final long CHECK_TIMEOUT = 1_000;

  protected static OffsetDateTime ofDateWithZone(int year, int mon, int day, int hour) {
    LocalDateTime dateTime = LocalDateTime.of(year, mon, day, hour, 0);
    return OffsetDateTime.of(dateTime, ZoneOffset.ofHours(0));
  }

  protected static OffsetDateTime quickDateWithZone(int day) {
    return ofDateWithZone(2022, 1, day, 0);
  }

  protected static Record newRecord(Schema schema, Object... val) {
    return TableTestBase.newGenericRecord(schema, val);
  }

  protected static DataFile insertDataFile(Table table, List<Record> records, StructLike partitionData) throws
      IOException {
    DataFile result = writeNewDataFile(table, records, partitionData);

    AppendFiles baseAppend = table.newAppend();
    baseAppend.appendFile(result);
    baseAppend.commit();

    return result;
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
    DataFile dataFile = writeNewDataFile(table, insertRecords, partitionData);

    DeleteFile deleteFile = writeEqDeleteFile(table, deleteRecords, partitionData);
    RowDelta rowDelta = table.newRowDelta();
    rowDelta.addRows(dataFile);
    rowDelta.addDeletes(deleteFile);
    rowDelta.validateFromSnapshot(table.currentSnapshot().snapshotId());
    rowDelta.commit();
  }

  protected static DeleteFile insertEqDeleteFiles(Table table, List<Record> records, StructLike partitionData)
      throws IOException {
    DeleteFile result = writeEqDeleteFile(table, records, partitionData);

    RowDelta rowDelta = table.newRowDelta();
    rowDelta.addDeletes(result);
    rowDelta.commit();
    return result;
  }

  protected static void rowDeltaWithPos(Table table, List<Record> insertRecords, List<Record> deleteRecords,
                                        StructLike partitionData) throws IOException {
    DataFile dataFile = writeNewDataFile(table, insertRecords, partitionData);

    DeleteFile deleteFile = writeEqDeleteFile(table, deleteRecords, partitionData);
    Multimap<String, Long>
        file2Positions = Multimaps.newListMultimap(Maps.newHashMap(), Lists::newArrayList);
    file2Positions.put(dataFile.path().toString(), 0L);
    DeleteFile posDeleteFile = writePosDeleteFile(table, file2Positions, partitionData);
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
    List<DataFile> insertFiles = write(insertRows, table, ChangeAction.INSERT, txId);
    List<DataFile> deleteFiles = write(deleteRows, table, ChangeAction.DELETE, txId);
    AppendFiles appendFiles = table.changeTable().newAppend();
    insertFiles.forEach(appendFiles::appendFile);
    deleteFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();
  }

  protected static void writeBase(ArcticTable table, List<Record> insertRows) {
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

  protected static void assertOptimizeHistory(OptimizeHistory optimizeHistory,
                                              OptimizeType optimizeType,
                                              int fileCntBefore,
                                              int fileCntAfter) {
    Assert.assertNotNull(optimizeHistory);
    Assert.assertEquals(optimizeType, optimizeHistory.getOptimizeType());
    Assert.assertEquals(fileCntBefore, optimizeHistory.getTotalFilesStatBeforeOptimize().getFileCnt());
    Assert.assertEquals(fileCntAfter, optimizeHistory.getTotalFilesStatAfterOptimize().getFileCnt());
  }

  protected static OptimizeHistory waitOptimizeResult(TableIdentifier tableIdentifier, long expectRecordId) {
    boolean success;
    try {
      success = waitUntilFinish(() -> {
        List<OptimizeHistory> optimizeHistory =
            ServiceContainer.getOptimizeService().getOptimizeHistory(tableIdentifier);
        if (optimizeHistory == null || optimizeHistory.isEmpty()) {
          LOG.info("optimize history is empty");
          return Status.RUNNING;
        }
        Optional<OptimizeHistory> any =
            optimizeHistory.stream().filter(p -> p.getRecordId() == expectRecordId).findAny();

        if (any.isPresent()) {
          return Status.SUCCESS;
        } else {
          LOG.info("optimize history max recordId {}",
              optimizeHistory.stream().map(OptimizeHistory::getRecordId).max(Comparator.naturalOrder()).get());
          return Status.RUNNING;
        }
      }, WAIT_SUCCESS_TIMEOUT);
    } catch (TimeoutException e) {
      throw new IllegalStateException("wait optimize result timeout expectRecordId " + expectRecordId, e);
    }

    if (success) {
      List<OptimizeHistory> optimizeHistory = ServiceContainer.getOptimizeService().getOptimizeHistory(tableIdentifier);
      return optimizeHistory.stream().filter(p -> p.getRecordId() == expectRecordId).findAny().orElse(null);
    } else {
      return null;
    }
  }

  protected static void assertOptimizeHangUp(TableIdentifier tableIdentifier, long notExpectRecordId) {
    try {
      Thread.sleep(CHECK_TIMEOUT);
    } catch (InterruptedException e) {
      throw new IllegalStateException("waiting result was interrupted");
    }
    List<OptimizeHistory> optimizeHistory =
        ServiceContainer.getOptimizeService().getOptimizeHistory(tableIdentifier);
    if (optimizeHistory == null || optimizeHistory.isEmpty()) {
      return;
    }
    Optional<OptimizeHistory> any =
        optimizeHistory.stream().filter(p -> p.getRecordId() >= notExpectRecordId).findAny();
    any.ifPresent(h -> LOG.error("{} get unexpected optimize history {} {}", tableIdentifier, notExpectRecordId, h));
    Assert.assertFalse("optimize is not stopped", any.isPresent());
  }

  protected static boolean waitUntilFinish(Supplier<Status> statusSupplier, final long timeout)
      throws TimeoutException {
    long start = System.currentTimeMillis();
    while (true) {
      long duration = System.currentTimeMillis() - start;
      if (duration > timeout) {
        throw new TimeoutException("wait exceed timeout, " + duration + "ms > " + timeout + "ms");
      }
      Status status = statusSupplier.get();
      if (status == Status.FAILED) {
        return false;
      } else if (status == Status.RUNNING) {
        try {
          Thread.sleep(1000);
        } catch (InterruptedException e) {
          throw new IllegalStateException("waiting result was interrupted");
        }
      } else {
        return true;
      }
    }
  }

  private enum Status {
    SUCCESS,
    FAILED,
    RUNNING
  }
}
