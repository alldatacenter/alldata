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

package com.netease.arctic.op;

import com.netease.arctic.io.DataTestHelpers;
import com.netease.arctic.io.TableDataTestBase;
import com.netease.arctic.utils.TablePropertyUtil;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.util.StructLikeMap;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Set;

public class TestOverwriteBaseFile extends TableDataTestBase {

  /**
   * overwrite all partition, add new data files
   */
  @Test
  public void testOverwriteAllPartition() {
    long txId = getArcticTable().asKeyedTable().beginTransaction(System.currentTimeMillis() + "");
    List<Record> newRecords = Lists.newArrayList(
        DataTestHelpers.createRecord(7, "777", 0, "2022-01-01T12:00:00"),
        DataTestHelpers.createRecord(8, "888", 0, "2022-01-01T12:00:00"),
        DataTestHelpers.createRecord(9, "999", 0, "2022-01-01T12:00:00")
    );
    long before = System.currentTimeMillis();
    List<DataFile> newFiles = DataTestHelpers.writeBaseStore(getArcticTable().asKeyedTable(), txId, newRecords, false);
    OverwriteBaseFiles overwrite = getArcticTable().asKeyedTable().newOverwriteBaseFiles();
    newFiles.forEach(overwrite::addFile);
    overwrite.overwriteByRowFilter(Expressions.alwaysTrue())
        .updateOptimizedSequenceDynamically(txId)
        .commit();
    long after = System.currentTimeMillis();
    // overwrite all partition and add new data file

    StructLikeMap<Long> partitionOptimizedSequence =
        TablePropertyUtil.getPartitionOptimizedSequence(getArcticTable().asKeyedTable());
    // expect result: all partition with new txId
    Assert.assertEquals(
        txId,
        partitionOptimizedSequence.get(DataTestHelpers.recordPartition("2022-01-01T12:00:00")).longValue());
    Assert.assertEquals(
        txId,
        partitionOptimizedSequence.get(DataTestHelpers.recordPartition("2022-01-02T12:00:00")).longValue());
    Assert.assertEquals(
        txId,
        partitionOptimizedSequence.get(DataTestHelpers.recordPartition("2022-01-03T12:00:00")).longValue());
    Assert.assertEquals(
        txId,
        partitionOptimizedSequence.get(DataTestHelpers.recordPartition("2022-01-04T12:00:00")).longValue());

    StructLikeMap<Long> partitionOptimizedTime =
        TablePropertyUtil.getPartitionBaseOptimizedTime(getArcticTable().asKeyedTable());
    // expect result: all partition with new optimized time
    assertRange(before, after,
        partitionOptimizedTime.get(DataTestHelpers.recordPartition("2022-01-01T12:00:00")));
    assertRange(before, after,
        partitionOptimizedTime.get(DataTestHelpers.recordPartition("2022-01-02T12:00:00")));
    assertRange(before, after,
        partitionOptimizedTime.get(DataTestHelpers.recordPartition("2022-01-03T12:00:00")));
    assertRange(before, after,
        partitionOptimizedTime.get(DataTestHelpers.recordPartition("2022-01-04T12:00:00")));

    List<Record> rows = DataTestHelpers.readKeyedTable(getArcticTable().asKeyedTable(), Expressions.alwaysTrue());
    // partition1 -> base[7,8,9]
    Assert.assertEquals(3, rows.size());

    Set<Integer> resultIdSet = Sets.newHashSet();
    rows.forEach(r -> resultIdSet.add((Integer) r.get(0)));
    Assert.assertTrue(resultIdSet.contains(7));
    Assert.assertTrue(resultIdSet.contains(8));
    Assert.assertTrue(resultIdSet.contains(9));
  }

  private void assertRange(long from, long to, long actual) {
    Assert.assertTrue(actual >= from);
    Assert.assertTrue(actual <= to);
  }

  @Test
  public void testOverwritePartitionByExpression() {
    long txId = getArcticTable().asKeyedTable().beginTransaction(System.currentTimeMillis() + "");
    List<Record> newRecords = Lists.newArrayList(
        DataTestHelpers.createRecord(7, "777", 0, "2022-01-01T12:00:00"),
        DataTestHelpers.createRecord(8, "888", 0, "2022-01-01T12:00:00"),
        DataTestHelpers.createRecord(9, "999", 0, "2022-01-01T12:00:00")
    );
    List<DataFile> newFiles = DataTestHelpers.writeBaseStore(getArcticTable().asKeyedTable(), txId, newRecords, false);
    long before = System.currentTimeMillis();
    OverwriteBaseFiles overwrite = getArcticTable().asKeyedTable().newOverwriteBaseFiles();
    newFiles.forEach(overwrite::addFile);
    overwrite.updateOptimizedSequenceDynamically(txId);
    overwrite.overwriteByRowFilter(
        Expressions.or(
            Expressions.or(
                Expressions.equal("op_time", "2022-01-01T12:00:00"),
                Expressions.equal("op_time", "2022-01-02T12:00:00")
            ),
            Expressions.equal("op_time", "2022-01-04T12:00:00")
        )

    );
    overwrite.commit();
    long after = System.currentTimeMillis();

    StructLikeMap<Long> partitionOptimizedSequence =
        TablePropertyUtil.getPartitionOptimizedSequence(getArcticTable().asKeyedTable());
    // expect result: 1,2,4 partition with new txId, 3 partition is null
    Assert.assertEquals(
        txId,
        partitionOptimizedSequence.get(DataTestHelpers.recordPartition("2022-01-01T12:00:00")).longValue());
    Assert.assertEquals(
        txId,
        partitionOptimizedSequence.get(DataTestHelpers.recordPartition("2022-01-02T12:00:00")).longValue());
    Assert.assertNull(partitionOptimizedSequence.get(DataTestHelpers.recordPartition("2022-01-03T12:00:00")));
    Assert.assertEquals(
        txId,
        partitionOptimizedSequence.get(DataTestHelpers.recordPartition("2022-01-02T12:00:00")).longValue());

    StructLikeMap<Long> partitionOptimizedTime =
        TablePropertyUtil.getPartitionBaseOptimizedTime(getArcticTable().asKeyedTable());
    // expect result: 1,2,4 partition with new optimized time, 3 partition is null
    assertRange(before, after,
        partitionOptimizedTime.get(DataTestHelpers.recordPartition("2022-01-01T12:00:00")));
    assertRange(before, after,
        partitionOptimizedTime.get(DataTestHelpers.recordPartition("2022-01-02T12:00:00")));
    Assert.assertNull(partitionOptimizedTime.get(DataTestHelpers.recordPartition("2022-01-03T12:00:00")));
    assertRange(before, after,
        partitionOptimizedTime.get(DataTestHelpers.recordPartition("2022-01-04T12:00:00")));

    List<Record> rows = DataTestHelpers.readKeyedTable(getArcticTable().asKeyedTable(), Expressions.alwaysTrue());
    // partition1 -> base[7,8,9]
    // partition3 -> base[3]
    Assert.assertEquals(4, rows.size());

    Set<Integer> resultIdSet = Sets.newHashSet();
    rows.forEach(r -> resultIdSet.add((Integer) r.get(0)));
    Assert.assertTrue(resultIdSet.contains(7));
    Assert.assertTrue(resultIdSet.contains(8));
    Assert.assertTrue(resultIdSet.contains(9));

    Assert.assertTrue(resultIdSet.contains(3));
  }
}
