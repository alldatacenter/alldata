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

package com.netease.arctic.flink.read.hybrid.assigner;

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.flink.read.FlinkSplitPlanner;
import com.netease.arctic.flink.read.hybrid.reader.RowDataReaderFunction;
import com.netease.arctic.flink.read.hybrid.reader.TestRowDataReaderFunction;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.source.DataIterator;
import org.apache.flink.api.connector.source.ReaderInfo;
import org.apache.flink.api.connector.source.SourceEvent;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.apache.flink.api.connector.source.SplitsAssignment;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.metrics.groups.SplitEnumeratorMetricGroup;
import org.apache.flink.table.data.RowData;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

public class TestShuffleSplitAssigner extends TestRowDataReaderFunction {
  private static final Logger LOG = LoggerFactory.getLogger(TestShuffleSplitAssigner.class);

  @Test
  public void testSingleParallelism() {
    ShuffleSplitAssigner shuffleSplitAssigner = instanceSplitAssigner(1);

    List<ArcticSplit> splitList = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger());
    shuffleSplitAssigner.onDiscoveredSplits(splitList);
    List<ArcticSplit> actual = new ArrayList<>();

    while (true) {
      Split splitOpt = shuffleSplitAssigner.getNext(0);
      if (splitOpt.isAvailable()) {
        actual.add(splitOpt.split());
      } else {
        break;
      }
    }

    Assert.assertEquals(splitList.size(), actual.size());
  }

  @Test
  public void testMultiParallelism() {
    ShuffleSplitAssigner shuffleSplitAssigner = instanceSplitAssigner(3);

    List<ArcticSplit> splitList = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger());
    shuffleSplitAssigner.onDiscoveredSplits(splitList);
    List<ArcticSplit> actual = new ArrayList<>();

    int subtaskId = 2;
    while (subtaskId >= 0) {
      Split splitOpt = shuffleSplitAssigner.getNext(subtaskId);
      if (splitOpt.isAvailable()) {
        actual.add(splitOpt.split());
      } else {
        LOG.info("subtask id {}, splits {}.\n {}", subtaskId, actual.size(), actual);
        --subtaskId;
      }
    }

    Assert.assertEquals(splitList.size(), actual.size());
  }

  @Test
  public void testTreeNodeMaskUpdate() {
    ShuffleSplitAssigner shuffleSplitAssigner = instanceSplitAssigner(3);
    long[][] treeNodes = new long[][]{{3, 0}, {3, 1}, {3, 2}, {3, 3}, {7, 0}, {7, 1}, {7, 2}, {7, 3}, {7, 4},
        {1, 0}, {1, 1}, {0, 0}, {7, 7}, {15, 15}};
    long[][] expectNodes = new long[][]{{3, 0}, {3, 1}, {3, 2}, {3, 3}, {3, 0}, {3, 1}, {3, 2}, {3, 3}, {3, 0},
        {3, 0}, {3, 2}, {3, 1}, {3, 3}, {3, 0}, {3, 2}, {3, 1}, {3, 3}, {3, 3}, {3, 3}};

    List<DataTreeNode> actualNodes = new ArrayList<>();

    for (long[] node : treeNodes) {
      ArcticSplit arcticSplit = new ArcticSplit() {
        DataTreeNode dataTreeNode = DataTreeNode.of(node[0], node[1]);

        @Override
        public Integer taskIndex() {
          return null;
        }

        @Override
        public void updateOffset(Object[] recordOffsets) {
        }

        @Override
        public ArcticSplit copy() {
          return null;
        }

        @Override
        public DataTreeNode dataTreeNode() {
          return this.dataTreeNode;
        }

        @Override
        public void modifyTreeNode(DataTreeNode expected) {
          this.dataTreeNode = expected;
        }

        @Override
        public String splitId() {
          return null;
        }

        @Override
        public String toString() {
          return dataTreeNode.toString();
        }
      };
      List<DataTreeNode> exactTreeNodes = shuffleSplitAssigner.getExactlyTreeNodes(arcticSplit);
      actualNodes.addAll(exactTreeNodes);
    }
    long[][] result = actualNodes.stream().map(treeNode -> new long[]{treeNode.mask(), treeNode.index()}).toArray(value -> new long[actualNodes.size()][]);

    Assert.assertArrayEquals(expectNodes, result);
  }

  @Test
  public void testNodeUpMoved() throws IOException {
    writeUpdateWithSpecifiedMaskOne();
    List<ArcticSplit> arcticSplits = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger(0));
    int totalParallelism = 3;
    ShuffleSplitAssigner assigner = instanceSplitAssigner(totalParallelism);
    assigner.onDiscoveredSplits(arcticSplits);
    RowDataReaderFunction rowDataReaderFunction = new RowDataReaderFunction(
        new Configuration(),
        testKeyedTable.schema(),
        testKeyedTable.schema(),
        testKeyedTable.primaryKeySpec(),
        null,
        true,
        testKeyedTable.io()
    );
    int subtaskId = 0;
    Split split;
    List<RowData> actual = new ArrayList<>();
    LOG.info("subtaskId={}...", subtaskId);
    do {
      split = assigner.getNext(subtaskId);
      if (split.isAvailable()) {
        DataIterator<RowData> dataIterator = rowDataReaderFunction.createDataIterator(split.split());
        while (dataIterator.hasNext()) {
          RowData rowData = dataIterator.next();
          LOG.info("{}", rowData);
          actual.add(rowData);
        }
      } else {
        subtaskId = subtaskId + 1;
        LOG.info("subtaskId={}...", subtaskId);
      }
    } while (subtaskId < totalParallelism);


    List<RowData> excepts = exceptsCollection();
    excepts.addAll(generateRecords());
    RowData[] array = excepts.stream().sorted(Comparator.comparing(RowData::toString))
        .collect(Collectors.toList())
        .toArray(new RowData[excepts.size()]);
    assertArrayEquals(array, actual);
  }

  protected ShuffleSplitAssigner instanceSplitAssigner(int parallelism) {
    SplitEnumeratorContext<ArcticSplit> splitEnumeratorContext = new InternalSplitEnumeratorContext(parallelism);
    return new ShuffleSplitAssigner(splitEnumeratorContext);
  }

  protected static class InternalSplitEnumeratorContext implements SplitEnumeratorContext<ArcticSplit> {
    private final int parallelism;

    public InternalSplitEnumeratorContext(int parallelism) {
      this.parallelism = parallelism;
    }

    @Override
    public SplitEnumeratorMetricGroup metricGroup() {
      return null;
    }

    @Override
    public void sendEventToSourceReader(int subtaskId, SourceEvent event) {

    }

    @Override
    public int currentParallelism() {
      return parallelism;
    }

    @Override
    public Map<Integer, ReaderInfo> registeredReaders() {
      return null;
    }

    @Override
    public void assignSplits(SplitsAssignment<ArcticSplit> newSplitAssignments) {

    }

    @Override
    public void signalNoMoreSplits(int subtask) {

    }

    @Override
    public <T> void callAsync(Callable<T> callable, BiConsumer<T, Throwable> handler) {

    }

    @Override
    public <T> void callAsync(Callable<T> callable, BiConsumer<T, Throwable> handler, long initialDelay, long period) {

    }

    @Override
    public void runInCoordinatorThread(Runnable runnable) {

    }
  }
}