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

package com.netease.arctic.flink.read;


import com.netease.arctic.flink.read.hybrid.reader.TestRowDataReaderFunction;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.scan.TableEntriesScan;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Snapshot;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestFlinkSplitPlanner extends TestRowDataReaderFunction {

  @Test
  public void testPlanSplitFromKeyedTable() {
    testKeyedTable.baseTable().refresh();
    testKeyedTable.changeTable().refresh();
    List<ArcticSplit> splitList = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger());
    Assert.assertEquals(7, splitList.size());
  }

  @Test
  public void testIncrementalChangelog() throws IOException {
    testKeyedTable.baseTable().refresh();
    testKeyedTable.changeTable().refresh();
    List<ArcticSplit> splitList = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger());

    Assert.assertEquals(7, splitList.size());

    long startSnapshotId = testKeyedTable.changeTable().currentSnapshot().snapshotId();
    writeUpdate();
    testKeyedTable.changeTable().refresh();
    long nowSnapshotId = testKeyedTable.changeTable().currentSnapshot().snapshotId();
    TableEntriesScan entriesScan = TableEntriesScan.builder(testKeyedTable.changeTable())
        .useSnapshot(nowSnapshotId)
        .includeFileContent(FileContent.DATA)
        .build();
    PartitionSpec spec = testKeyedTable.changeTable().spec();
    Snapshot snapshot = testKeyedTable.changeTable().snapshot(startSnapshotId);
    long fromSequence = snapshot.sequenceNumber();

    List<ArcticSplit> changeSplits =
        FlinkSplitPlanner.planChangeTable(entriesScan, fromSequence, spec, new AtomicInteger());

    Assert.assertEquals(1, changeSplits.size());
  }

}