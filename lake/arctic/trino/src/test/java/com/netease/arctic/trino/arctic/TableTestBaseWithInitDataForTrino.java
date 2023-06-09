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

package com.netease.arctic.trino.arctic;

import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.writer.GenericBaseTaskWriter;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public abstract class TableTestBaseWithInitDataForTrino extends TableTestBaseForTrino {

  protected List<Record> baseRecords() {
    GenericRecord record = GenericRecord.create(TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of("id", 1, "name$name", "john", "op_time",
        LocalDateTime.of(2022, 1, 1, 12, 0, 0))));
    builder.add(record.copy(ImmutableMap.of("id", 2, "name$name", "lily", "op_time",
        LocalDateTime.of(2022, 1, 2, 12, 0, 0))));
    builder.add(record.copy(ImmutableMap.of("id", 3, "name$name", "jake", "op_time",
        LocalDateTime.of(2022, 1, 3, 12, 0, 0))));
    builder.add(record.copy(ImmutableMap.of("id", 4, "name$name", "sam", "op_time",
        LocalDateTime.of(2022, 1, 4, 12, 0, 0))));

    return builder.build();
  }

  protected List<Record> changeInsertRecords() {
    GenericRecord record = GenericRecord.create(TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of("id", 5, "name$name", "mary", "op_time",
        LocalDateTime.of(2022, 1, 1, 12, 0, 0))));
    return builder.build();
  }

  protected List<Record> changeSparkInsertRecords() {
    GenericRecord record = GenericRecord.create(TABLE_SCHEMA);

    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of("id", 6, "name$name", "mack", "op_time",
        LocalDateTime.of(2022, 1, 1, 12, 0, 0))));
    return builder.build();
  }

  protected List<Record> changeDeleteRecords() {
    GenericRecord record = GenericRecord.create(TABLE_SCHEMA);
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(record.copy(ImmutableMap.of("id", 5, "name$name", "mary", "op_time",
        LocalDateTime.of(2022, 1, 1, 12, 0, 0))));
    return builder.build();
  }

  protected DataFile dataFileForPositionDelete;

  protected DeleteFile deleteFileOfPositionDelete;

  protected void initData() throws IOException {
    long currentSequenceNumber  = testKeyedTable.beginTransaction(null);
    //write base
    {
      GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(testKeyedTable)
          .withTransactionId(currentSequenceNumber).buildBaseWriter();

      for (Record record : baseRecords()) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      AppendFiles baseAppend = testKeyedTable.baseTable().newAppend();
      dataFileForPositionDelete = Arrays.stream(result.dataFiles())
          .filter(s -> s.path().toString().contains("op_time_day=2022-01-04")).findAny().get();
      Arrays.stream(result.dataFiles()).forEach(baseAppend::appendFile);
      baseAppend.commit();
    }

    // write position delete
    {
      SortedPosDeleteWriter<Record> writer = GenericTaskWriters.builderFor(testKeyedTable)
          .withTransactionId(currentSequenceNumber).buildBasePosDeleteWriter(3, 3, dataFileForPositionDelete.partition());
      writer.delete(dataFileForPositionDelete.path().toString(), 0);
      DeleteFile posDeleteFiles = writer.complete().stream().findAny().get();
      this.deleteFileOfPositionDelete = posDeleteFiles;
      testKeyedTable.baseTable().newRowDelta().addDeletes(posDeleteFiles).commit();
    }

    //write change insert
    {
      GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(testKeyedTable)
          .buildChangeWriter();
      for (Record record : changeInsertRecords()) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      AppendFiles changeAppend = testKeyedTable.changeTable().newAppend();
      Arrays.stream(result.dataFiles())
              .forEach(changeAppend::appendFile);
      changeAppend.commit();
    }

    //begin spark insert
    currentSequenceNumber = testKeyedTable.beginTransaction(null);

    //write change delete
    {
      GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(testKeyedTable)
          .withChangeAction(ChangeAction.DELETE).buildChangeWriter();
      for (Record record : changeDeleteRecords()) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      AppendFiles changeAppend = testKeyedTable.changeTable().newAppend();
      Arrays.stream(result.dataFiles())
          .forEach(changeAppend::appendFile);
      changeAppend.commit();
    }

    //spark insert
    {
      GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(testKeyedTable)
          .withTransactionId(currentSequenceNumber)
          .buildChangeWriter();
      for (Record record : changeSparkInsertRecords()) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      AppendFiles changeAppend = testKeyedTable.changeTable().newAppend();
      Arrays.stream(result.dataFiles())
          .forEach(changeAppend::appendFile);
      changeAppend.commit();
    }
  }
}
