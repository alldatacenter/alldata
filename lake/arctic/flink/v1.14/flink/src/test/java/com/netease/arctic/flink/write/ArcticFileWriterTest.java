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

package com.netease.arctic.flink.write;

import com.netease.arctic.flink.FlinkTestBase;
import com.netease.arctic.flink.table.ArcticTableLoader;
import com.netease.arctic.flink.util.OneInputStreamOperatorInternTest;
import com.netease.arctic.flink.util.TestGlobalAggregateManager;
import com.netease.arctic.table.ArcticTable;
import org.apache.flink.streaming.util.OneInputStreamOperatorTestHarness;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.SerializableTable;
import org.apache.iceberg.Table;
import org.apache.iceberg.flink.sink.RowDataTaskWriterFactory;
import org.apache.iceberg.flink.sink.TaskWriterFactory;
import org.apache.iceberg.io.TaskWriter;
import org.apache.iceberg.io.WriteResult;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.List;

import static com.netease.arctic.flink.table.descriptors.ArcticValidator.SUBMIT_EMPTY_SNAPSHOTS;

@RunWith(Parameterized.class)
public class ArcticFileWriterTest extends FlinkTestBase {

  public static final long TARGET_FILE_SIZE = 128 * 1024 * 1024;
  public ArcticTableLoader tableLoader;
  private final boolean submitEmptySnapshots;

  @Parameterized.Parameters(name = "submitEmptySnapshots = {0}")
  public static Object[][] parameters() {
    return new Object[][]{
        {false},
        {true}
    };
  }

  public ArcticFileWriterTest(boolean submitEmptySnapshots) {
    this.submitEmptySnapshots = submitEmptySnapshots;
  }

  public static OneInputStreamOperatorTestHarness<RowData, WriteResult> createArcticStreamWriter(
      ArcticTableLoader tableLoader) throws Exception {
    return createArcticStreamWriter(tableLoader, true, null);
  }

  public static OneInputStreamOperatorTestHarness<RowData, WriteResult> createArcticStreamWriter(
      ArcticTableLoader tableLoader, boolean submitEmptySnapshots, Long restoredCheckpointId) throws Exception {
    OneInputStreamOperatorTestHarness<RowData, WriteResult> harness =
        doCreateArcticStreamWriter(tableLoader, submitEmptySnapshots, restoredCheckpointId);

    harness.setup();
    harness.open();

    return harness;
  }

  public static OneInputStreamOperatorTestHarness<RowData, WriteResult> doCreateArcticStreamWriter(
      ArcticTableLoader tableLoader, boolean submitEmptySnapshots, Long restoredCheckpointId) throws Exception {
    tableLoader.open();
    ArcticTable arcticTable = tableLoader.loadArcticTable();
    arcticTable.properties().put(SUBMIT_EMPTY_SNAPSHOTS.key(), String.valueOf(submitEmptySnapshots));

    ArcticFileWriter streamWriter = FlinkSink.createFileWriter(arcticTable,
        null,
        false,
        (RowType) FLINK_SCHEMA.toRowDataType().getLogicalType(),
        tableLoader);
    OneInputStreamOperatorInternTest<RowData, WriteResult> harness =
        new OneInputStreamOperatorInternTest<>(
            streamWriter, 1, 1, 0, restoredCheckpointId,
            new TestGlobalAggregateManager());

    return harness;
  }

  public static TaskWriter<RowData> createUnkeyedTaskWriter(Table table, long targetFileSize, FileFormat format,
                                                            RowType rowType) {
    TaskWriterFactory<RowData> taskWriterFactory = new RowDataTaskWriterFactory(
        SerializableTable.copyOf(table), rowType, targetFileSize, format, null, false);
    taskWriterFactory.initialize(1, 1);
    return taskWriterFactory.create();
  }

  @Test
  public void testInsertWrite() throws Exception {
    tableLoader = ArcticTableLoader.of(PK_TABLE_ID, catalogBuilder);
    long checkpointId = 1L;
    try (
        OneInputStreamOperatorTestHarness<RowData, WriteResult> testHarness = createArcticStreamWriter(
            tableLoader)) {
      ArcticFileWriter fileWriter = (ArcticFileWriter) testHarness.getOneInputOperator();
      Assert.assertNotNull(fileWriter.getWriter());
      // The first checkpoint
      testHarness.processElement(createRowData(1, "hello", "2020-10-11T10:10:11.0"), 1);
      testHarness.processElement(createRowData(2, "hello", "2020-10-12T10:10:11.0"), 1);
      testHarness.processElement(createRowData(3, "hello", "2020-10-13T10:10:11.0"), 1);

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      Assert.assertNull(fileWriter.getWriter());
      Assert.assertEquals(1, testHarness.extractOutputValues().size());
      Assert.assertEquals(3, testHarness.extractOutputValues().get(0).dataFiles().length);

      checkpointId = checkpointId + 1;

      // The second checkpoint
      testHarness.processElement(createRowData(1, "hello", "2020-10-12T10:10:11.0"), 1);
      Assert.assertNotNull(fileWriter.getWriter());
      testHarness.processElement(createRowData(2, "hello", "2020-10-12T10:10:11.0"), 1);
      testHarness.processElement(createRowData(3, "hello", "2020-10-12T10:10:11.0"), 1);

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      // testHarness.extractOutputValues() calculates the cumulative value
      List<WriteResult> completedFiles = testHarness.extractOutputValues();
      Assert.assertEquals(2, completedFiles.size());
      Assert.assertEquals(3, completedFiles.get(1).dataFiles().length);
    }
  }

  @Test
  public void testSnapshotMultipleTimes() throws Exception {
    long checkpointId = 1;
    long timestamp = 1;

    tableLoader = ArcticTableLoader.of(PK_TABLE_ID, catalogBuilder);
    try (OneInputStreamOperatorTestHarness<RowData, WriteResult> testHarness = createArcticStreamWriter(tableLoader)) {
      testHarness.processElement(createRowData(1, "hello", "2020-10-11T10:10:11.0"), timestamp++);
      testHarness.processElement(createRowData(2, "hello", "2020-10-12T10:10:11.0"), timestamp);
      testHarness.processElement(createRowData(3, "hello", "2020-10-13T10:10:11.0"), timestamp);

      testHarness.prepareSnapshotPreBarrier(checkpointId++);
      long expectedDataFiles = 3;
      WriteResult result = WriteResult.builder().addAll(testHarness.extractOutputValues()).build();
      Assert.assertEquals(0, result.deleteFiles().length);
      Assert.assertEquals(expectedDataFiles, result.dataFiles().length);

      // snapshot again immediately.
      for (int i = 0; i < 5; i++) {
        testHarness.prepareSnapshotPreBarrier(checkpointId++);

        result = WriteResult.builder().addAll(testHarness.extractOutputValues()).build();
        Assert.assertEquals(0, result.deleteFiles().length);
        Assert.assertEquals(expectedDataFiles, result.dataFiles().length);
      }
    }
  }

  @Test
  public void testInsertWriteWithoutPk() throws Exception {
    tableLoader = ArcticTableLoader.of(TABLE_ID, catalogBuilder);
    long checkpointId = 1L;
    try (
        OneInputStreamOperatorTestHarness<RowData, WriteResult> testHarness = createArcticStreamWriter(
            tableLoader)) {
      // The first checkpoint
      testHarness.processElement(createRowData(1, "hello", "2020-10-11T10:10:11.0"), 1);
      testHarness.processElement(createRowData(2, "hello", "2020-10-12T10:10:11.0"), 1);
      testHarness.processElement(createRowData(3, "hello", "2020-10-13T10:10:11.0"), 1);

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      Assert.assertEquals(1, testHarness.extractOutputValues().size());
      Assert.assertEquals(3, testHarness.extractOutputValues().get(0).dataFiles().length);

      checkpointId = checkpointId + 1;

      // The second checkpoint
      testHarness.processElement(createRowData(1, "hello", "2020-10-12T10:10:11.0"), 1);
      testHarness.processElement(createRowData(2, "hello", "2020-10-12T10:10:11.0"), 1);
      testHarness.processElement(createRowData(3, "hello", "2020-10-12T10:10:11.0"), 1);

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      // testHarness.extractOutputValues() calculates the cumulative value
      List<WriteResult> completedFiles = testHarness.extractOutputValues();
      Assert.assertEquals(2, completedFiles.size());
      Assert.assertEquals(1, completedFiles.get(1).dataFiles().length);
    }
  }

  @Test
  public void testDeleteWrite() throws Exception {
    tableLoader = ArcticTableLoader.of(PK_TABLE_ID, catalogBuilder);
    long checkpointId = 1L;
    try (
        OneInputStreamOperatorTestHarness<RowData, WriteResult> testHarness = createArcticStreamWriter(tableLoader)) {
      // The first checkpoint
      testHarness.processElement(createRowData(1, "hello", "2020-10-11T10:10:11.0", RowKind.INSERT), 1);
      testHarness.processElement(createRowData(2, "hello", "2020-10-12T10:10:11.0", RowKind.INSERT), 1);
      testHarness.processElement(createRowData(1, "hello", "2020-10-11T10:10:11.0", RowKind.DELETE), 1);
      testHarness.processElement(createRowData(1, "hello", "2020-10-11T10:10:11.0", RowKind.DELETE), 1);

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      Assert.assertEquals(1, testHarness.extractOutputValues().size());
      Assert.assertEquals(3, testHarness.extractOutputValues().get(0).dataFiles().length);

      checkpointId = checkpointId + 1;

      // The second checkpoint
      testHarness.processElement(createRowData(1, "hello", "2020-10-12T10:10:11.0", RowKind.INSERT), 1);
      testHarness.processElement(createRowData(2, "hello", "2020-10-12T10:10:11.0", RowKind.DELETE), 1);
      testHarness.processElement(createRowData(3, "hello", "2020-10-12T10:10:11.0", RowKind.DELETE), 1);

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      // testHarness.extractOutputValues() calculates the cumulative value
      Assert.assertEquals(2, testHarness.extractOutputValues().size());
      Assert.assertEquals(3, testHarness.extractOutputValues().get(1).dataFiles().length);
    }
  }

  @Test
  public void testUpdateWrite() throws Exception {
    tableLoader = ArcticTableLoader.of(PK_TABLE_ID, catalogBuilder);
    long checkpointId = 1L;
    try (
        OneInputStreamOperatorTestHarness<RowData, WriteResult> testHarness = createArcticStreamWriter(
            tableLoader)) {
      // The first checkpoint
      testHarness.processElement(
          createRowData(1, "hello", "2020-10-11T10:10:11.0", RowKind.INSERT), 1);
      testHarness.processElement(
          createRowData(1, "hello", "2020-10-11T10:10:11.0", RowKind.UPDATE_BEFORE), 1);
      testHarness.processElement(
          createRowData(1, "hi", "2020-10-11T10:10:11.0", RowKind.UPDATE_AFTER), 1);
      testHarness.processElement(
          createRowData(1, "hello", "2020-10-13T10:10:11.0", RowKind.UPDATE_AFTER), 1);

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      Assert.assertEquals(1, testHarness.extractOutputValues().size());
      Assert.assertEquals(3, testHarness.extractOutputValues().get(0).dataFiles().length);

      checkpointId = checkpointId + 1;

      // The second checkpoint
      testHarness.processElement(
          createRowData(1, "hello", "2020-10-12T10:10:11.0", RowKind.UPDATE_AFTER), 1);
      testHarness.processElement(
          createRowData(2, "h", "2020-10-12T10:10:11.0"), 1);
      testHarness.processElement(
          createRowData(2, "hello", "2020-10-12T10:10:11.0", RowKind.UPDATE_AFTER), 1);
      testHarness.processElement(
          createRowData(2, "hello", "2020-10-12T10:10:11.0", RowKind.DELETE), 1);

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      // testHarness.extractOutputValues() calculates the cumulative value
      Assert.assertEquals(2, testHarness.extractOutputValues().size());
      Assert.assertEquals(3, testHarness.extractOutputValues().get(1).dataFiles().length);
    }
  }

  @Test
  public void testEmitEmptyResults() throws Exception {

    tableLoader = ArcticTableLoader.of(PK_TABLE_ID, catalogBuilder);
    long checkpointId = 1L;
    long excepted = submitEmptySnapshots ? 1 : 0;
    try (
        OneInputStreamOperatorTestHarness<RowData, WriteResult> testHarness = createArcticStreamWriter(
            tableLoader, submitEmptySnapshots, null)) {
      // The first checkpoint

      testHarness.prepareSnapshotPreBarrier(checkpointId);
      Assert.assertEquals(excepted, testHarness.extractOutputValues().size());

      checkpointId = checkpointId + 1;

      // The second checkpoint
      testHarness.prepareSnapshotPreBarrier(checkpointId);
      Assert.assertEquals(excepted, testHarness.extractOutputValues().size());
    }
  }

}