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

package com.netease.arctic.flink.read.hybrid.reader;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.IcebergFileEntry;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DefaultKeyedFile;
import com.netease.arctic.flink.read.FlinkSplitPlanner;
import com.netease.arctic.flink.read.hybrid.enumerator.TestContinuousSplitPlannerImpl;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ChangelogSplit;
import com.netease.arctic.flink.read.source.DataIterator;
import com.netease.arctic.scan.ArcticFileScanTask;
import com.netease.arctic.scan.BasicArcticFileScanTask;
import com.netease.arctic.scan.TableEntriesScan;
import com.netease.arctic.table.KeyedTable;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.types.RowKind;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.io.CloseableIterator;
import org.apache.iceberg.io.TaskWriter;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class TestRowDataReaderFunction extends TestContinuousSplitPlannerImpl {
  private static final Logger LOG = LoggerFactory.getLogger(TestRowDataReaderFunction.class);
  private static final AtomicInteger splitCount = new AtomicInteger();

  public TestRowDataReaderFunction() {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
      new BasicTableTestHelper(true, true));
  }

  @Test
  public void testReadChangelog() throws IOException {

    List<ArcticSplit> arcticSplits = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger(0));

    RowDataReaderFunction rowDataReaderFunction = new RowDataReaderFunction(
        new Configuration(),
        testKeyedTable.schema(),
        testKeyedTable.schema(),
        testKeyedTable.primaryKeySpec(),
        null,
        true,
        testKeyedTable.io()
    );

    List<RowData> actual = new ArrayList<>();
    arcticSplits.forEach(split -> {
      LOG.info("ArcticSplit {}.", split);
      DataIterator<RowData> dataIterator = rowDataReaderFunction.createDataIterator(split);
      while (dataIterator.hasNext()) {
        RowData rowData = dataIterator.next();
        LOG.info("{}", rowData);
        actual.add(rowData);
      }
    });

    assertArrayEquals(excepts(), actual);

    long snapshotId = testKeyedTable.changeTable().currentSnapshot().snapshotId();
    writeUpdate();

    testKeyedTable.changeTable().refresh();
    long nowSnapshotId = testKeyedTable.changeTable().currentSnapshot().snapshotId();

    TableEntriesScan entriesScan = TableEntriesScan.builder(testKeyedTable.changeTable())
        .useSnapshot(nowSnapshotId)
        .includeFileContent(FileContent.DATA)
        .build();
    Snapshot snapshot = testKeyedTable.changeTable().snapshot(snapshotId);
    long fromSequence = snapshot.sequenceNumber();

    CloseableIterator<IcebergFileEntry> iterator = entriesScan.entries().iterator();
    Set<ArcticFileScanTask> appendLogTasks = new HashSet<>();
    Set<ArcticFileScanTask> deleteLogTasks = new HashSet<>();
    while (iterator.hasNext()) {
      IcebergFileEntry entry = iterator.next();
      if (entry.getSequenceNumber() <= fromSequence) {
        continue;
      }
      DefaultKeyedFile keyedFile =
          DefaultKeyedFile.parseChange((DataFile) entry.getFile(), entry.getSequenceNumber());
      BasicArcticFileScanTask task =
          new BasicArcticFileScanTask(keyedFile, null, testKeyedTable.changeTable().spec(), null);
      if (task.fileType().equals(DataFileType.INSERT_FILE)) {
        appendLogTasks.add(task);
      } else if (task.fileType().equals(DataFileType.EQ_DELETE_FILE)) {
        deleteLogTasks.add(task);
      } else {
        throw new IllegalArgumentException(
            String.format(
                "DataFileType %s is not supported during change log reading period.",
                task.fileType()));
      }
    }
    ChangelogSplit changelogSplit = new ChangelogSplit(appendLogTasks, deleteLogTasks, splitCount.incrementAndGet());
    LOG.info("ArcticSplit {}.", changelogSplit);
    actual.clear();
    DataIterator<RowData> dataIterator = rowDataReaderFunction.createDataIterator(changelogSplit);
    while (dataIterator.hasNext()) {
      RowData rowData = dataIterator.next();
      LOG.info("{}", rowData);
      actual.add(rowData);
    }
    assertArrayEquals(excepts2(), actual);

  }

  @Test
  public void testReadNodesUpMoved() throws IOException {
    writeUpdateWithSpecifiedMaskOne();
    List<ArcticSplit> arcticSplits = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger(0));

    RowDataReaderFunction rowDataReaderFunction = new RowDataReaderFunction(
        new Configuration(),
        testKeyedTable.schema(),
        testKeyedTable.schema(),
        testKeyedTable.primaryKeySpec(),
        null,
        true,
        testKeyedTable.io()
    );

    List<RowData> actual = new ArrayList<>();
    arcticSplits.forEach(split -> {
      LOG.info("ArcticSplit {}.", split);
      DataIterator<RowData> dataIterator = rowDataReaderFunction.createDataIterator(split);
      while (dataIterator.hasNext()) {
        RowData rowData = dataIterator.next();
        LOG.info("{}", rowData);
        actual.add(rowData);
      }
    });

    List<RowData> excepts = exceptsCollection();
    excepts.addAll(generateRecords());
    RowData[] array = excepts.stream().sorted(Comparator.comparing(RowData::toString))
        .collect(Collectors.toList())
        .toArray(new RowData[excepts.size()]);
    assertArrayEquals(array, actual);
  }

  protected void assertArrayEquals(RowData[] excepts, List<RowData> actual) {
    Assert.assertArrayEquals(excepts, sortRowDataCollection(actual));
  }

  protected RowData[] sortRowDataCollection(Collection<RowData> records) {
    return records.stream().sorted(
            Comparator
                .comparing(
                    RowData::toString))
        .collect(Collectors.toList())
        .toArray(new RowData[records.size()]);
  }

  protected void writeUpdate() throws IOException {
    //write change update
    writeUpdate(updateRecords());
  }

  protected void writeUpdate(List<RowData> input) throws IOException {
    writeUpdate(input, testKeyedTable);
  }

  protected void writeUpdateWithSpecifiedMaskOne() throws IOException {
    List<RowData> excepts = generateRecords();

    writeUpdateWithSpecifiedMask(excepts, testKeyedTable, 1);
  }

  protected void writeUpdateWithSpecifiedMask(List<RowData> input, KeyedTable table, long mask) throws IOException {
    // write change update
    TaskWriter<RowData> taskWriter = createKeyedTaskWriter(table, ROW_TYPE, false, mask);

    for (RowData record : input) {
      taskWriter.write(record);
    }
    commit(table, taskWriter.complete(), false);
  }

  protected void writeUpdate(List<RowData> input, KeyedTable table) throws IOException {
    //write change update
    TaskWriter<RowData> taskWriter = createKeyedTaskWriter(table, ROW_TYPE, false);

    for (RowData record : input) {
      taskWriter.write(record);
    }
    commit(table, taskWriter.complete(), false);
  }

  protected List<RowData> generateRecords() {
    List<RowData> excepts = new ArrayList<>();
    excepts.add(GenericRowData.ofKind(RowKind.INSERT, 7, StringData.fromString("syan"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.UPDATE_BEFORE, 2, StringData.fromString("lily"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.UPDATE_AFTER, 2, StringData.fromString("daniel"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.UPDATE_BEFORE, 7, StringData.fromString("syan"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.UPDATE_AFTER, 7, StringData.fromString("syan2"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    return excepts;
  }

  protected List<RowData> updateRecords() {
    List<RowData> excepts = new ArrayList<>();
    excepts.add(GenericRowData.ofKind(RowKind.UPDATE_BEFORE, 5, StringData.fromString("lind"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.UPDATE_AFTER, 5, StringData.fromString("lina"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    return excepts;
  }

  protected RowData[] excepts2() {
    List<RowData> excepts = updateRecords();

    return updateRecords().stream().sorted(Comparator.comparing(RowData::toString))
        .collect(Collectors.toList())
        .toArray(new RowData[excepts.size()]);
  }

  protected RowData[] excepts() {
    List<RowData> excepts = exceptsCollection();

    return excepts.stream().sorted(Comparator.comparing(RowData::toString))
        .collect(Collectors.toList())
        .toArray(new RowData[excepts.size()]);
  }

  protected List<RowData> exceptsCollection() {
    List<RowData> excepts = new ArrayList<>();
    excepts.add(GenericRowData.ofKind(RowKind.INSERT, 1, StringData.fromString("john"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.INSERT, 2, StringData.fromString("lily"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.INSERT, 3, StringData.fromString("jake"), ldt.plusDays(1).toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt.plusDays(1))));
    excepts.add(GenericRowData.ofKind(RowKind.INSERT, 4, StringData.fromString("sam"), ldt.plusDays(1).toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt.plusDays(1))));
    excepts.add(GenericRowData.ofKind(RowKind.INSERT, 5, StringData.fromString("mary"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.INSERT, 6, StringData.fromString("mack"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.DELETE, 5, StringData.fromString("mary"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    excepts.add(GenericRowData.ofKind(RowKind.INSERT, 5, StringData.fromString("lind"), ldt.toEpochSecond(ZoneOffset.UTC), TimestampData.fromLocalDateTime(ldt)));
    return excepts;
  }
}