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
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.io.writer.GenericBaseTaskWriter;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.utils.ManifestEntryFields;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.Table;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.hadoop.HadoopTables;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class TaskWriterTest extends TableTestBase {

  public TaskWriterTest() {
    super(TableFormat.MIXED_ICEBERG, true, true);
  }

  @Test
  public void testBaseWriter() throws IOException {
    GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
        .withTransactionId(1L).buildBaseWriter();

    for (Record record : writeRecords()) {
      writer.write(record);
    }
    WriteResult result = writer.complete();
    Assert.assertEquals(4, result.dataFiles().length);
  }

  @Test
  public void testBasePosDeleteWriter() throws IOException {
    DataFile dataFile = TableTestHelpers.getFile(1, "op_time_day=2020-01-01");
    SortedPosDeleteWriter<Record> writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
        .withTransactionId(1L).buildBasePosDeleteWriter(2, 1, dataFile.partition());

    writer.delete(dataFile.path(), 1);
    writer.delete(dataFile.path(), 3);
    writer.delete(dataFile.path(), 5);
    List<DeleteFile> result = writer.complete();
    Assert.assertEquals(1, result.size());
    RowDelta rowDelta = getArcticTable().asKeyedTable().baseTable().newRowDelta();
    result.forEach(rowDelta::addDeletes);
    rowDelta.commit();

    // check lower bounds and upper bounds of file_path
    HadoopTables tables = new HadoopTables();
    Table entriesTable = tables.load(getArcticTable().asKeyedTable().baseTable().location() + "#ENTRIES");
    IcebergGenerics.read(entriesTable)
        .build()
        .forEach(record -> {
          GenericRecord fileRecord = (GenericRecord) record.get(ManifestEntryFields.DATA_FILE_ID);
          Map<Integer, ByteBuffer> lowerBounds =
              (Map<Integer, ByteBuffer>) fileRecord.getField(DataFile.LOWER_BOUNDS.name());
          String pathLowerBounds = new String(lowerBounds.get(MetadataColumns.DELETE_FILE_PATH.fieldId()).array());
          Map<Integer, ByteBuffer> upperBounds =
              (Map<Integer, ByteBuffer>) fileRecord.getField(DataFile.UPPER_BOUNDS.name());
          String pathUpperBounds = new String(upperBounds.get(MetadataColumns.DELETE_FILE_PATH.fieldId()).array());

          Assert.assertEquals(dataFile.path().toString(), pathLowerBounds);
          Assert.assertEquals(dataFile.path().toString(), pathUpperBounds);
        });
  }

  @Test
  public void testChangeWriter() throws IOException {
    GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
        .withTransactionId(1L).buildChangeWriter();

    for (Record record : writeRecords()) {
      writer.write(record);
    }

    WriteResult result = writer.complete();
    Assert.assertEquals(4, result.dataFiles().length);
  }


  @Test
  public void testOrderedWriterBase() throws IOException {
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(DataTestHelpers.createRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(3, "jake", 0, "2022-02-01T23:00:00"));
    builder.add(DataTestHelpers.createRecord(4, "sam", 0, "2022-02-01T06:00:00"));
    builder.add(DataTestHelpers.createRecord(5, "john", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(6, "lily", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(7, "jake", 0, "2022-02-01T23:00:00"));
    builder.add(DataTestHelpers.createRecord(8, "sam", 0, "2022-02-01T06:00:00"));
    builder.add(DataTestHelpers.createRecord(9, "john", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(10, "lily", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(11, "jake", 0, "2022-02-01T23:00:00"));
    builder.add(DataTestHelpers.createRecord(12, "sam", 0, "2022-02-01T06:00:00"));
    builder.add(DataTestHelpers.createRecord(13, "john", 0, "2022-01-01T12:00:00"));


    GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
        .withOrdered()
        .withTransactionId(1L)
        .buildBaseWriter();

    Assert.assertThrows(IllegalStateException.class, () -> {
      for (Record record : builder.build()) {
        writer.write(record);
      }
    });

  }

  private List<Record> writeRecords() {
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(DataTestHelpers.createRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(3, "jake", 0, "2022-02-01T23:00:00"));
    builder.add(DataTestHelpers.createRecord(4, "sam", 0, "2022-02-01T06:00:00"));
    builder.add(DataTestHelpers.createRecord(5, "john", 0, "2022-01-01T12:00:00"));

    return builder.build();
  }
}
