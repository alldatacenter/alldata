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

package com.netease.arctic.server.optimizing.plan;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.TableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.catalog.CatalogTestHelper;
import com.netease.arctic.data.ChangeAction;
import com.netease.arctic.server.optimizing.OptimizingTestHelpers;
import com.netease.arctic.server.optimizing.scan.KeyedTableFileScanHelper;
import com.netease.arctic.server.optimizing.scan.TableFileScanHelper;
import com.netease.arctic.table.KeyedTable;
import com.netease.arctic.table.TableProperties;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collections;
import java.util.List;

@RunWith(Parameterized.class)
public class TestKeyedPartitionPlan extends MixedTablePlanTestBase {

  public TestKeyedPartitionPlan(CatalogTestHelper catalogTestHelper,
                                TableTestHelper tableTestHelper) {
    super(catalogTestHelper, tableTestHelper);
  }

  @Parameterized.Parameters(name = "{0}, {1}")
  public static Object[][] parameters() {
    return new Object[][] {
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, true)},
        {new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
            new BasicTableTestHelper(true, false)}};
  }

  @Test
  public void testFragmentFiles() {
    updateBaseHashBucket(1);
    testFragmentFilesBase();
  }

  @Test
  public void testSegmentFiles() {
    testSegmentFilesBase();
  }

  @Test
  public void testOnlyOneFragmentFile() {
    updateBaseHashBucket(1);
    testOnlyOneFragmentFileBase();
  }

  @Test
  public void testWithDeleteFiles() {
    testWithDeleteFilesBase();
  }

  @Test
  public void testOnlyOneChangeFiles() {
    updateChangeHashBucket(1);
    closeFullOptimizingInterval();
    // write fragment file
    List<Record> newRecords = OptimizingTestHelpers.generateRecord(tableTestHelper(), 1, 4, "2022-01-01T12:00:00");
    long transactionId = beginTransaction();
    List<DataFile> dataFiles = OptimizingTestHelpers.appendChange(getArcticTable(),
        tableTestHelper().writeChangeStore(getArcticTable(), transactionId, ChangeAction.INSERT,
            newRecords, false));

    List<TaskDescriptor> taskDescriptors = planWithCurrentFiles();

    Assert.assertEquals(1, taskDescriptors.size());

    assertTask(taskDescriptors.get(0), dataFiles, Collections.emptyList(), Collections.emptyList(),
        Collections.emptyList());
  }

  @Test
  public void testChangeFilesWithDelete() {
    updateChangeHashBucket(1);
    closeFullOptimizingInterval();
    List<Record> newRecords;
    long transactionId;
    List<DataFile> dataFiles = Lists.newArrayList();
    // write fragment file
    newRecords = OptimizingTestHelpers.generateRecord(tableTestHelper(), 1, 4, "2022-01-01T12:00:00");
    transactionId = beginTransaction();
    dataFiles.addAll(OptimizingTestHelpers.appendChange(getArcticTable(),
        tableTestHelper().writeChangeStore(getArcticTable(), transactionId, ChangeAction.INSERT,
            newRecords, false)));
    Snapshot fromSnapshot = getArcticTable().changeTable().currentSnapshot();

    newRecords = OptimizingTestHelpers.generateRecord(tableTestHelper(), 1, 4, "2022-01-01T12:00:00");
    transactionId = beginTransaction();
    List<DataFile> deleteFiles = OptimizingTestHelpers.appendChange(getArcticTable(),
        tableTestHelper().writeChangeStore(getArcticTable(), transactionId, ChangeAction.DELETE,
            newRecords, false));

    newRecords = OptimizingTestHelpers.generateRecord(tableTestHelper(), 1, 4, "2022-01-01T12:00:00");
    transactionId = beginTransaction();
    dataFiles.addAll(OptimizingTestHelpers.appendChange(getArcticTable(),
        tableTestHelper().writeChangeStore(getArcticTable(), transactionId, ChangeAction.INSERT,
            newRecords, false)));
    Snapshot toSnapshot = getArcticTable().changeTable().currentSnapshot();

    AbstractPartitionPlan plan = buildPlanWithCurrentFiles();
    Assert.assertEquals(fromSnapshot.sequenceNumber(), plan.getFromSequence());
    Assert.assertEquals(toSnapshot.sequenceNumber(), plan.getToSequence());

    List<TaskDescriptor> taskDescriptors = plan.splitTasks(0);

    Assert.assertEquals(1, taskDescriptors.size());

    assertTask(taskDescriptors.get(0), dataFiles, Collections.emptyList(), Collections.emptyList(),
        deleteFiles);
  }

  @Test
  public void testMinorOptimizingWithBaseDelay() {
    updateChangeHashBucket(4);
    closeFullOptimizingInterval();
    closeMinorOptimizingInterval();
    List<Record> newRecords;
    long transactionId;
    List<DataFile> dataFiles = Lists.newArrayList();
    // write fragment file
    newRecords = OptimizingTestHelpers.generateRecord(tableTestHelper(), 1, 4, "2022-01-01T12:00:00");
    transactionId = beginTransaction();
    dataFiles.addAll(OptimizingTestHelpers.appendChange(getArcticTable(),
        tableTestHelper().writeChangeStore(getArcticTable(), transactionId, ChangeAction.INSERT,
            newRecords, false)));
    StructLike partition = dataFiles.get(0).partition();

    // not trigger optimize
    Assert.assertEquals(0, planWithCurrentFiles().size());

    // update base delay
    updateTableProperty(TableProperties.BASE_REFRESH_INTERVAL, 1 + "");
    Assert.assertEquals(4, planWithCurrentFiles().size());
    updatePartitionProperty(partition, TableProperties.PARTITION_BASE_OPTIMIZED_TIME,
        (System.currentTimeMillis() - 10) + "");
    Assert.assertEquals(4, planWithCurrentFiles().size());
    updatePartitionProperty(partition, TableProperties.PARTITION_BASE_OPTIMIZED_TIME,
        (System.currentTimeMillis() + 1000000) + "");
    Assert.assertEquals(0, planWithCurrentFiles().size());
  }

  @Override
  protected KeyedTable getArcticTable() {
    return super.getArcticTable().asKeyedTable();
  }

  @Override
  protected AbstractPartitionPlan getPartitionPlan() {
    return new MixedIcebergPartitionPlan(getTableRuntime(), getArcticTable(), getPartition(),
        System.currentTimeMillis());
  }

  @Override
  protected TableFileScanHelper getTableFileScanHelper() {
    return new KeyedTableFileScanHelper(getArcticTable(),
        OptimizingTestHelpers.getCurrentKeyedTableSnapshot(getArcticTable()));
  }
}
