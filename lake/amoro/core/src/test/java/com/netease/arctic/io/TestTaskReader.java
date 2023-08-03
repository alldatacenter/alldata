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
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.io.reader.BaseIcebergPosDeleteReader;
import com.netease.arctic.scan.CombinedScanTask;
import com.netease.arctic.scan.KeyedTableScanTask;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.io.DeleteSchemaUtil;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@RunWith(Parameterized.class)
public class TestTaskReader extends TableDataTestBase {

  private final boolean useDiskMap;

  @Parameterized.Parameters(name = "useDiskMap = {2}")
  public static Object[] parameters() {
    return new Object[][] {{new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true), false},
                           {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
                            new BasicTableTestHelper(true, true), true}};
  }

  public TestTaskReader(CatalogTestHelper catalogTestHelper, TableTestHelper tableTestHelper,
      boolean useDiskMap) {
    super(catalogTestHelper, tableTestHelper);
    this.useDiskMap = useDiskMap;
  }

  @Test
  public void testMergeOnRead() {
    Set<Record> records = Sets.newHashSet(tableTestHelper().readKeyedTable(getArcticTable().asKeyedTable(),
        Expressions.alwaysTrue(), null, useDiskMap, false));
    //expect: (id=1),(id=2),(id=3),(id=6)
    Set<Record> expectRecords = Sets.newHashSet();
    expectRecords.add(allRecords.get(0));
    expectRecords.add(allRecords.get(1));
    expectRecords.add(allRecords.get(2));
    expectRecords.add(allRecords.get(5));
    Assert.assertEquals(expectRecords, records);
  }

  @Test
  public void testMergeOnReadFilterLongType() {
    // where id = 1
    Expression filter = Expressions.equal("id", 1);
    Set<Record> records = Sets.newHashSet(tableTestHelper().readKeyedTable(getArcticTable().asKeyedTable(),
        filter, null, useDiskMap, false));

    List<KeyedTableScanTask> readTasks = planReadTask(filter);
    Assert.assertEquals(2, readTasks.size());

    //expect: (id=1),(id=6), change store can only be filtered by partition expression now.
    Set<Record> expectRecords = Sets.newHashSet();
    expectRecords.add(allRecords.get(0));
    expectRecords.add(allRecords.get(5));
    Assert.assertEquals(expectRecords, records);

    // where id = 1 or id = 3
    filter = Expressions.or(Expressions.equal("id", 1), Expressions.equal("id", 3));
    readTasks = planReadTask(filter);
    Assert.assertEquals(3, readTasks.size());
    records = Sets.newHashSet(tableTestHelper().readKeyedTable(getArcticTable().asKeyedTable(),
        filter, null, useDiskMap, false));
    //expect: (id=1),(id=3),(id=6), change store can only be filtered by partition expression now.
    expectRecords.clear();
    expectRecords.add(allRecords.get(0));
    expectRecords.add(allRecords.get(2));
    expectRecords.add(allRecords.get(5));
    Assert.assertEquals(expectRecords, records);
  }

  @Test
  public void testMergeOnReadFilterTimestampType() {
    // where op_time = '2022-01-01T12:00:00'
    Expression filter = Expressions.equal("op_time", "2022-01-01T12:00:00");
    List<KeyedTableScanTask> readTasks = planReadTask(filter);
    Assert.assertEquals(2, readTasks.size());
    Set<Record> records = Sets.newHashSet(tableTestHelper().readKeyedTable(getArcticTable().asKeyedTable(),
        filter, null, useDiskMap, false));
    //expect: (id=1),(id=6), change store can only be filtered by partition expression now.
    Set<Record> expectRecords = Sets.newHashSet();
    expectRecords.add(allRecords.get(0));
    expectRecords.add(allRecords.get(5));
    Assert.assertEquals(expectRecords, records);
  }

  @Test
  public void testMergeOnReadFilterPartitionValue() {
    // where op_time > '2022-01-10T12:00:00'
    Expression filter = Expressions.greaterThan("op_time", "2022-01-10T12:00:00");
    List<KeyedTableScanTask> readTasks = planReadTask(filter);
    Assert.assertEquals(0, readTasks.size());
    Set<Record> records = Sets.newHashSet(tableTestHelper().readKeyedTable(getArcticTable().asKeyedTable(),
        filter, null, useDiskMap, false));
    //expect: empty, change store can only be filtered by partition expression now.
    Set<Record> expectRecords = Sets.newHashSet();
    Assert.assertEquals(expectRecords, records);
  }

  @Test
  public void testReadChange() {
    Set<Record> records = Sets.newHashSet(tableTestHelper().readChangeStore(getArcticTable().asKeyedTable(),
        Expressions.alwaysTrue(), null, useDiskMap));
    //expect: +(id=5),+(id=6),-(id=5)
    Set<Record> expectRecords = Sets.newHashSet();
    expectRecords.add(DataTestHelpers.appendMetaColumnValues(allRecords.get(4), 2, 1, ChangeAction.INSERT));
    expectRecords.add(DataTestHelpers.appendMetaColumnValues(allRecords.get(5), 2, 2, ChangeAction.INSERT));
    expectRecords.add(DataTestHelpers.appendMetaColumnValues(allRecords.get(4), 3, 1, ChangeAction.DELETE));
    Assert.assertEquals(expectRecords, records);
  }

  @Test
  public void testReadPosDelete() {
    Assume.assumeFalse(useDiskMap);
    BaseIcebergPosDeleteReader baseIcebergPosDeleteReader =
        new BaseIcebergPosDeleteReader(
            getArcticTable().asKeyedTable().io(),
            Collections.singletonList(deleteFileOfPositionDelete));
    ImmutableList.Builder<Record> builder = ImmutableList.builder();
    baseIcebergPosDeleteReader.readDeletes().forEach(record -> builder.add(record.copy()));
    List<Record> readRecords = builder.build();
    List<Record> expectRecords = Lists.newArrayListWithCapacity(1);
    GenericRecord r = GenericRecord.create(DeleteSchemaUtil.pathPosSchema());
    r.set(0, dataFileForPositionDelete.path().toString());
    r.set(1, 0L);
    expectRecords.add(r);

    Assert.assertEquals(expectRecords, readRecords);
  }

  @Test
  public void testReadDeletedData() {
    Set<Record> records = Sets.newHashSet(tableTestHelper().readKeyedTable(getArcticTable().asKeyedTable(),
        Expressions.alwaysTrue(), null, useDiskMap, true));
    //expect: (id=4,id=5)
    Set<Record> expectRecords = Sets.newHashSet();
    expectRecords.add(allRecords.get(3));
    expectRecords.add(allRecords.get(4));
    Assert.assertEquals(expectRecords, records);
  }

  protected boolean isUseDiskMap() {
    return useDiskMap;
  }

  protected List<KeyedTableScanTask> planReadTask(Expression filter) {
    List<KeyedTableScanTask> scanTasks = Lists.newArrayList();
    try (CloseableIterable<CombinedScanTask> combinedScanTasks =
        getArcticTable().asKeyedTable().newScan().filter(filter).planTasks()) {
      combinedScanTasks.forEach(combinedScanTask -> scanTasks.addAll(combinedScanTask.tasks()));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return scanTasks;
  }
}
