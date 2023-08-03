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

import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.TemporalJoinSplits;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class TestTemporalJoinSplitsThreadSafe {

  @Test
  public void testTemporalJoinSplits() {
    List<String> allSplit = new LinkedList<>();
    for (int i = 0; i < 100; i++) {
      allSplit.add(UUID.randomUUID().toString());
    }

    Collection<ArcticSplit> arcticSplits = allSplit.stream().map(TestArcticSplit::of).collect(Collectors.toList());

    for (int i = 0; i < 2; i++) {
      round(allSplit, arcticSplits);
    }
  }

  public void round(List<String> allSplit, Collection<ArcticSplit> arcticSplits) {
    TemporalJoinSplits temporalJoinSplits = new TemporalJoinSplits(arcticSplits, null);
    int n = allSplit.size();

    List<String> s1 = new ArrayList<>(allSplit.subList(0, (int) (2.0 / 3 * n))),
        s2 = new ArrayList<>(allSplit.subList((int) (1.0 / 3 * n), n));
    Collections.shuffle(s1);
    Collections.shuffle(s2);

    List<ArcticSplit> as = new ArrayList<>(arcticSplits);
    Collections.shuffle(as);
    int an = as.size();
    List<ArcticSplit> as1 = new ArrayList<>(as.subList(0, (int) (2.0 / 3 * an)));
    List<ArcticSplit> as2 = new ArrayList<>(as.subList((int) (1.0 / 3 * an), an));
    CompletableFuture<Void> f1 = CompletableFuture.runAsync(() ->
        temporalJoinSplits.removeAndReturnIfAllFinished(s1)
    );
    CompletableFuture<Void> f2 = CompletableFuture.runAsync(() ->
        temporalJoinSplits.addSplitsBack(as1)
    );
    CompletableFuture<Void> f3 = CompletableFuture.runAsync(() -> temporalJoinSplits.removeAndReturnIfAllFinished(s2));
    CompletableFuture<Void> f4 = CompletableFuture.runAsync(() -> temporalJoinSplits.addSplitsBack(as2));
    CompletableFuture.allOf(f1, f2, f3, f4).join();
    Assert.assertTrue(temporalJoinSplits.removeAndReturnIfAllFinished(allSplit));
  }

  static class TestArcticSplit extends ArcticSplit {
    private final String splitId;

    public TestArcticSplit(String splitId) {
      this.splitId = splitId;
    }

    public static TestArcticSplit of(String splitId) {
      return new TestArcticSplit(splitId);
    }

    @Override
    public Integer taskIndex() {
      return null;
    }

    @Override
    public void updateOffset(Object[] recordOffsets) {
    }

    @Override
    public ArcticSplit copy() {
      return new TestArcticSplit(splitId);
    }

    @Override
    public String splitId() {
      return splitId;
    }
  }
}