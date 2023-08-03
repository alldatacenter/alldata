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

import com.google.common.collect.Sets;
import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.DataFileTestHelpers;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.writer.GenericTaskWriters;
import com.netease.arctic.io.writer.SortedPosDeleteWriter;
import com.netease.arctic.scan.TableEntriesScan;
import com.netease.arctic.table.UnkeyedTable;
import com.netease.arctic.utils.ArcticTableUtil;
import org.apache.iceberg.AppendFiles;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.MetadataColumns;
import org.apache.iceberg.RowDelta;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RunWith(Parameterized.class)
public class TestTaskWriter extends TableTestBase {

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true)},
                           {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, false)},
                           {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(false, true)},
                           {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(false, false)}};
  }

  public TestTaskWriter(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Test
  public void testBaseWriter() {
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "jake", 0, "2022-01-03T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(4, "sam", 0, "2022-01-04T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(5, "mary", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(6, "mack", 0, "2022-01-01T12:00:00"));

    List<DataFile> files = tableTestHelper().writeBaseStore(getArcticTable(),
        1L, insertRecords, false);
    if (isKeyedTable()) {
      if (isPartitionedTable()) {
        Assert.assertEquals(5, files.size());
      } else {
        Assert.assertEquals(4, files.size());
      }
    } else {
      if (isPartitionedTable()) {
        Assert.assertEquals(4, files.size());
      } else {
        Assert.assertEquals(1, files.size());
      }
    }

    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    AppendFiles appendFiles = baseStore.newAppend();
    files.forEach(appendFiles::appendFile);
    appendFiles.commit();

    List<Record> readRecords = tableTestHelper().readBaseStore(getArcticTable(),
        Expressions.alwaysTrue(), null, false);
    Assert.assertEquals(Sets.newHashSet(insertRecords), Sets.newHashSet(readRecords));
  }

  @Test
  public void testBasePosDeleteWriter() throws IOException {
    DataFile dataFile = DataFileTestHelpers.getFile("/data", 1, getArcticTable().spec(),
        isPartitionedTable() ? "op_time_day=2020-01-01" : null, null, false);
    GenericTaskWriters.Builder builder = GenericTaskWriters.builderFor(getArcticTable());
    if (isKeyedTable()) {
      builder.withTransactionId(1L);
    }
    SortedPosDeleteWriter<Record> writer = builder.buildBasePosDeleteWriter(0, 0, dataFile.partition());

    writer.delete(dataFile.path(), 1);
    writer.delete(dataFile.path(), 3);
    writer.delete(dataFile.path(), 5);
    List<DeleteFile> result = writer.complete();
    Assert.assertEquals(1, result.size());
    UnkeyedTable baseStore = ArcticTableUtil.baseStore(getArcticTable());
    RowDelta rowDelta = baseStore.newRowDelta();
    result.forEach(rowDelta::addDeletes);
    rowDelta.commit();

    // check lower bounds and upper bounds of file_path
    TableEntriesScan entriesScan = TableEntriesScan.builder(baseStore)
        .includeColumnStats()
        .includeFileContent(FileContent.POSITION_DELETES)
        .build();
    AtomicInteger cnt = new AtomicInteger();
    entriesScan.entries().forEach(entry -> {
      cnt.getAndIncrement();
      ContentFile<?> file = entry.getFile();
      Map<Integer, ByteBuffer> lowerBounds = file.lowerBounds();
      Map<Integer, ByteBuffer> upperBounds = file.upperBounds();

      String pathLowerBounds = new String(lowerBounds.get(MetadataColumns.DELETE_FILE_PATH.fieldId()).array());
      String pathUpperBounds = new String(upperBounds.get(MetadataColumns.DELETE_FILE_PATH.fieldId()).array());

      Assert.assertEquals(dataFile.path().toString(), pathLowerBounds);
      Assert.assertEquals(dataFile.path().toString(), pathUpperBounds);
    });
    Assert.assertEquals(1, cnt.get());
  }

  @Test
  public void testChangeWriter() {
    Assume.assumeTrue(isKeyedTable());
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(1, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-02T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "jake", 0, "2022-01-03T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(4, "sam", 0, "2022-01-04T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(5, "mary", 0, "2022-01-01T12:00:00"));

    List<DataFile> insertFiles = tableTestHelper().writeChangeStore(getArcticTable().asKeyedTable(),
        1L, ChangeAction.INSERT, insertRecords, false);
    Assert.assertEquals(4, insertFiles.size());

    List<Record> deleteRecords = Lists.newArrayList();
    deleteRecords.add(insertRecords.get(0));
    deleteRecords.add(insertRecords.get(1));

    List<DataFile> deleteFiles = tableTestHelper().writeChangeStore(getArcticTable().asKeyedTable(),
        2L, ChangeAction.DELETE, deleteRecords, false);
    Assert.assertEquals(2, deleteFiles.size());

    AppendFiles appendFiles = getArcticTable().asKeyedTable().changeTable().newAppend();
    insertFiles.forEach(appendFiles::appendFile);
    deleteFiles.forEach(appendFiles::appendFile);
    appendFiles.commit();

    List<Record> readChangeRecords = tableTestHelper().readChangeStore(getArcticTable().asKeyedTable(),
        Expressions.alwaysTrue(), null, false);
    List<Record> expectRecord = Lists.newArrayList();
    for (int i = 0; i < insertRecords.size(); i++) {
      expectRecord.add(DataTestHelpers.appendMetaColumnValues(insertRecords.get(i), 1L, i + 1, ChangeAction.INSERT));
    }
    for (int i = 0; i < deleteRecords.size(); i++) {
      expectRecord.add(DataTestHelpers.appendMetaColumnValues(deleteRecords.get(i), 2L, i + 1, ChangeAction.DELETE));
    }
    Assert.assertEquals(Sets.newHashSet(expectRecord), Sets.newHashSet(readChangeRecords));
  }


  @Test
  public void testOrderedWriterThrowException()  {
    List<Record> insertRecords = Lists.newArrayList();
    insertRecords.add(tableTestHelper().generateTestRecord(2, "lily", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(3, "jake", 0, "2022-02-01T23:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(4, "sam", 0, "2022-02-01T06:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(5, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(6, "lily", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(7, "jake", 0, "2022-02-01T23:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(8, "sam", 0, "2022-02-01T06:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(9, "john", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(10, "lily", 0, "2022-01-01T12:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(11, "jake", 0, "2022-02-01T23:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(12, "sam", 0, "2022-02-01T06:00:00"));
    insertRecords.add(tableTestHelper().generateTestRecord(13, "john", 0, "2022-01-01T12:00:00"));

    Assert.assertThrows(IllegalStateException.class, () ->
        tableTestHelper().writeBaseStore(getArcticTable().asKeyedTable(), 1L, insertRecords, true));

    Assume.assumeTrue(isKeyedTable());
    Assert.assertThrows(IllegalStateException.class, () ->
        tableTestHelper().writeChangeStore(getArcticTable().asKeyedTable(), 1L, ChangeAction.INSERT,
            insertRecords, true));
  }
}
