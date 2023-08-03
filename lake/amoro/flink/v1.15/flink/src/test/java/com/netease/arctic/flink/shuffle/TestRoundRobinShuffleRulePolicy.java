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

package com.netease.arctic.flink.shuffle;

import com.netease.arctic.BasicTableTestHelper;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.catalog.BasicCatalogTestHelper;
import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.flink.FlinkTestBase;
import org.apache.flink.api.common.functions.Partitioner;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.table.data.RowData;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Map;
import java.util.Set;

@RunWith(Parameterized.class)
public class TestRoundRobinShuffleRulePolicy extends FlinkTestBase {

  public TestRoundRobinShuffleRulePolicy(boolean keyedTable,
                                         boolean partitionedTable) {
    super(new BasicCatalogTestHelper(TableFormat.MIXED_ICEBERG),
      new BasicTableTestHelper(keyedTable, partitionedTable));
  }

  @Parameterized.Parameters(name = "keyedTable = {0}, partitionedTable = {1}")
  public static Object[][] parameters() {
    return new Object[][] {
      {true, true},
      {true, false},
      {false, true},
      {false, false}};
  }

  @Test
  public void testPrimaryKeyPartitionedTable() throws Exception {
    Assume.assumeTrue(isKeyedTable());
    Assume.assumeTrue(isPartitionedTable());
    ShuffleHelper helper = ShuffleHelper.build(getArcticTable(), getArcticTable().schema(), FLINK_ROW_TYPE);
    RoundRobinShuffleRulePolicy policy =
        new RoundRobinShuffleRulePolicy(helper, 5, 2);
    Map<Integer, Set<DataTreeNode>> subTaskTreeNodes = policy.getSubtaskTreeNodes();
    Assert.assertEquals(subTaskTreeNodes.size(), 5);
    subTaskTreeNodes.values().forEach(nodes -> {
      Assert.assertEquals(nodes.size(), 2);
      Assert.assertTrue(nodes.contains(DataTreeNode.of(1, 0)));
      Assert.assertTrue(nodes.contains(DataTreeNode.of(1, 1)));
    });

    KeySelector<RowData, ShuffleKey> keySelector = policy.generateKeySelector();
    Partitioner<ShuffleKey> partitioner = policy.generatePartitioner();
    Assert.assertEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(1, "hello2", "2022-10-11T10:10:11.0")), 5));

    Assert.assertNotEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(1, "hello2", "2022-10-12T10:10:11.0")), 5));

    Assert.assertNotEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(2, "hello2", "2022-10-11T10:10:11.0")), 5));
  }

  @Test
  public void testPrimaryKeyTableWithoutPartition() throws Exception {
    Assume.assumeTrue(isKeyedTable());
    Assume.assumeFalse(isPartitionedTable());
    ShuffleHelper helper =
      ShuffleHelper.build(getArcticTable(), getArcticTable().schema(), FLINK_ROW_TYPE);
    RoundRobinShuffleRulePolicy policy =
        new RoundRobinShuffleRulePolicy(helper, 5, 2);
    Map<Integer, Set<DataTreeNode>> subTaskTreeNodes = policy.getSubtaskTreeNodes();
    Assert.assertEquals(subTaskTreeNodes.size(), 5);
    Assert.assertEquals(subTaskTreeNodes.get(0), Sets.newHashSet(
        DataTreeNode.of(7, 0), DataTreeNode.of(7, 5)));
    Assert.assertEquals(subTaskTreeNodes.get(1), Sets.newHashSet(
        DataTreeNode.of(7, 1), DataTreeNode.of(7, 6)));
    Assert.assertEquals(subTaskTreeNodes.get(2), Sets.newHashSet(
        DataTreeNode.of(7, 2), DataTreeNode.of(7, 7)));
    Assert.assertEquals(subTaskTreeNodes.get(3), Sets.newHashSet(
        DataTreeNode.of(7, 3)));
    Assert.assertEquals(subTaskTreeNodes.get(4), Sets.newHashSet(
        DataTreeNode.of(7, 4)));

    KeySelector<RowData, ShuffleKey> keySelector = policy.generateKeySelector();
    Partitioner<ShuffleKey> partitioner = policy.generatePartitioner();
    Assert.assertEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(1, "hello2", "2022-10-11T10:10:11.0")), 5));

    Assert.assertEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(1, "hello2", "2022-10-12T10:10:11.0")), 5));

    Assert.assertNotEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(2, "hello2", "2022-10-11T10:10:11.0")), 5));
  }

  @Test
  public void testPartitionedTableWithoutPrimaryKey() throws Exception {
    Assume.assumeFalse(isKeyedTable());
    Assume.assumeTrue(isPartitionedTable());
    ShuffleHelper helper =
      ShuffleHelper.build(getArcticTable(), getArcticTable().schema(), FLINK_ROW_TYPE);
    RoundRobinShuffleRulePolicy policy =
        new RoundRobinShuffleRulePolicy(helper, 5, 2);
    Map<Integer, Set<DataTreeNode>> subTaskTreeNodes = policy.getSubtaskTreeNodes();
    Assert.assertEquals(subTaskTreeNodes.size(), 5);
    subTaskTreeNodes.values().forEach(nodes -> {
      Assert.assertEquals(nodes.size(), 1);
      Assert.assertTrue(nodes.contains(DataTreeNode.of(0, 0)));
    });

    KeySelector<RowData, ShuffleKey> keySelector = policy.generateKeySelector();
    Partitioner<ShuffleKey> partitioner = policy.generatePartitioner();
    Assert.assertEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(1, "hello2", "2022-10-11T10:10:11.0")), 5));

    Assert.assertEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(2, "hello2", "2022-10-11T10:10:11.0")), 5));

    Assert.assertNotEquals(partitioner.partition(keySelector.getKey(
            createRowData(1, "hello", "2022-10-11T10:10:11.0")), 5),
        partitioner.partition(keySelector.getKey(
            createRowData(1, "hello2", "2022-10-12T10:10:11.0")), 5));
  }
}
