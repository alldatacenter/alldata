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

import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.writer.GenericBaseTaskWriter;
import com.netease.arctic.io.writer.GenericChangeTaskWriter;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.io.WriteResult;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.junit.Before;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class TableDataTestBase extends TableTestBase {

  protected static final List<Record> BASE_RECORDS;
  protected static final List<Record> CHANGE_DELETE_RECORDS;
  protected static final List<Record> CHANGE_INSERT_RECORDS;

  static {
    BASE_RECORDS = baseRecords();
    CHANGE_DELETE_RECORDS = changeDeleteRecords();
    CHANGE_INSERT_RECORDS = changeInsertRecords();
  }

  protected DataFile dataFileForPositionDelete;

  protected DeleteFile deleteFileOfPositionDelete;

  public TableDataTestBase() {
    super(TableFormat.MIXED_ICEBERG, true, true);
  }

  private static List<Record> baseRecords() {
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(DataTestHelpers.createRecord(1, "john", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    builder.add(DataTestHelpers.createRecord(3, "jake", 0, "2022-01-03T12:00:00"));
    builder.add(DataTestHelpers.createRecord(4, "sam", 0, "2022-01-04T12:00:00"));

    return builder.build();
  }

  private static List<Record> changeInsertRecords() {
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(DataTestHelpers.createRecord(5, "mary", 0, "2022-01-01T12:00:00"));
    builder.add(DataTestHelpers.createRecord(6, "mack", 0, "2022-01-01T12:00:00"));
    return builder.build();
  }

  private static List<Record> changeDeleteRecords() {
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    builder.add(DataTestHelpers.createRecord(5, "mary", 0, "2022-01-01T12:00:00"));
    return builder.build();
  }

  @Before
  public void initData() throws IOException {

    //write base
    {
      GenericBaseTaskWriter writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
          .withTransactionId(1L).buildBaseWriter();

      for (Record record : BASE_RECORDS) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      AppendFiles baseAppend = getArcticTable().asKeyedTable().baseTable().newAppend();
      dataFileForPositionDelete = Arrays.stream(result.dataFiles())
          .filter(s -> s.path().toString().contains("op_time_day=2022-01-04")).findAny().get();
      Arrays.stream(result.dataFiles()).forEach(baseAppend::appendFile);
      baseAppend.commit();
    }

    // write position delete
    {
      SortedPosDeleteWriter<Record> writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
          .withTransactionId(4L).buildBasePosDeleteWriter(3, 3, dataFileForPositionDelete.partition());
      writer.delete(dataFileForPositionDelete.path().toString(), 0);
      DeleteFile posDeleteFiles = writer.complete().stream().findAny().get();
      this.deleteFileOfPositionDelete = posDeleteFiles;
      getArcticTable().asKeyedTable().baseTable().newRowDelta().addDeletes(posDeleteFiles).commit();
    }

    //write change insert
    {
      GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
          .withTransactionId(2L).buildChangeWriter();
      for (Record record : CHANGE_INSERT_RECORDS) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      AppendFiles changeAppend = getArcticTable().asKeyedTable().changeTable().newAppend();
      Arrays.stream(result.dataFiles())
          .forEach(changeAppend::appendFile);
      changeAppend.commit();
    }

    //write change delete
    {
      GenericChangeTaskWriter writer = GenericTaskWriters.builderFor(getArcticTable().asKeyedTable())
          .withTransactionId(3L).withChangeAction(ChangeAction.DELETE).buildChangeWriter();
      for (Record record : CHANGE_DELETE_RECORDS) {
        writer.write(record);
      }
      WriteResult result = writer.complete();
      AppendFiles changeAppend = getArcticTable().asKeyedTable().changeTable().newAppend();
      Arrays.stream(result.dataFiles())
          .forEach(changeAppend::appendFile);
      changeAppend.commit();
    }
  }
}
