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

package com.netease.arctic.flink.read.hybrid.enumerator;

import com.netease.arctic.flink.read.FlinkSplitPlanner;
import com.netease.arctic.flink.read.hybrid.assigner.ShuffleSplitAssigner;
import com.netease.arctic.flink.read.hybrid.assigner.ShuffleSplitAssignerTest;
import com.netease.arctic.flink.read.hybrid.assigner.Split;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.TemporalJoinSplits;
import org.apache.flink.api.connector.source.SplitEnumeratorContext;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class ArcticSourceEnumStateSerializerTest extends ShuffleSplitAssignerTest {
  private final static Logger LOG = LoggerFactory.getLogger(ArcticSourceEnumStateSerializerTest.class);

  @Test
  public void testArcticEnumState() throws IOException {
    ShuffleSplitAssigner shuffleSplitAssigner = instanceSplitAssigner(3);

    List<ArcticSplit> splitList = FlinkSplitPlanner.planFullTable(testKeyedTable, new AtomicInteger());
    shuffleSplitAssigner.onDiscoveredSplits(splitList);
    TemporalJoinSplits splits = new TemporalJoinSplits(splitList, null);

    ArcticSourceEnumState expect = new ArcticSourceEnumState(
        shuffleSplitAssigner.state(),
        null,
        shuffleSplitAssigner.serializePartitionIndex(),
        splits);

    ArcticSourceEnumStateSerializer arcticSourceEnumStateSerializer = new ArcticSourceEnumStateSerializer();
    byte[] ser = arcticSourceEnumStateSerializer.serialize(expect);

    Assert.assertNotNull(ser);

    ArcticSourceEnumState actual = arcticSourceEnumStateSerializer.deserialize(1, ser);

    Assert.assertEquals(expect.pendingSplits().size(), actual.pendingSplits().size());
    Assert.assertEquals(
        Objects.requireNonNull(expect.shuffleSplitRelation()).length,
        Objects.requireNonNull(actual.shuffleSplitRelation()).length);

    SplitEnumeratorContext<ArcticSplit> splitEnumeratorContext = new InternalSplitEnumeratorContext(3);
    ShuffleSplitAssigner actualAssigner = new ShuffleSplitAssigner(splitEnumeratorContext,
        actual.pendingSplits(), actual.shuffleSplitRelation());

    List<ArcticSplit> actualSplits = new ArrayList<>();

    int subtaskId = 2;
    while (subtaskId >= 0) {
      Split splitOpt = actualAssigner.getNext(subtaskId);
      if (splitOpt.isAvailable()) {
        actualSplits.add(splitOpt.split());
      } else {
        LOG.info("subtask id {}, splits {}.\n {}", subtaskId, actualSplits.size(), actualSplits);
        --subtaskId;
      }
    }

    Assert.assertEquals(splitList.size(), actualSplits.size());

    TemporalJoinSplits temporalJoinSplits = actual.temporalJoinSplits();
    Assert.assertEquals(expect.temporalJoinSplits(), temporalJoinSplits);
  }
}