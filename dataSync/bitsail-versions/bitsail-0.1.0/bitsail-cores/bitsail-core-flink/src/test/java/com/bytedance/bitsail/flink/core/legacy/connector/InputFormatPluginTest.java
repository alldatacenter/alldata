/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.flink.core.legacy.connector;

import com.bytedance.bitsail.common.BitSailException;

import org.apache.flink.api.common.io.statistics.BaseStatistics;
import org.apache.flink.core.io.GenericInputSplit;
import org.apache.flink.core.io.InputSplit;
import org.apache.flink.core.io.InputSplitAssigner;
import org.apache.flink.types.Row;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class InputFormatPluginTest {

  @Test
  public void createInputSplits() throws IOException {
    AtomicInteger splitsCreateCalledTime = new AtomicInteger(0);

    final InputFormatPlugin input = new InputFormatPlugin<Row, InputSplit>() {
      @Override
      public BaseStatistics getStatistics(BaseStatistics cachedStatistics) {
        return null;
      }

      @Override
      public InputSplitAssigner getInputSplitAssigner(InputSplit[] inputSplits) {
        return null;
      }

      @Override
      public void open(InputSplit split) {
      }

      @Override
      public void close() {
      }

      @Override
      public void initPlugin() {
      }

      @Override
      public String getType() {
        return null;
      }

      @Override
      public Row buildRow(Row reuse, String mandatoryEncoding) throws BitSailException {
        return null;
      }

      @Override
      public boolean isSplitEnd() {
        return false;
      }

      @Override
      public InputSplit[] createSplits(int minNumSplits) {
        splitsCreateCalledTime.getAndIncrement();
        final GenericInputSplit[] splits = new GenericInputSplit[10];
        for (int i = 0; i < splits.length; i++) {
          splits[i] = new GenericInputSplit(i, splits.length);
        }
        return splits;
      }
    };

    // first time, create
    final InputSplit[] inputSplits = input.createInputSplits(1);

    // second time, from memory cache
    final InputSplit[] cachedInputSplits = input.createInputSplits(1);
    assertArrayEquals(inputSplits, cachedInputSplits);

    // clear the memory cache, should recover from compressed bytes
    input.cachedInputSplits = null;
    final InputSplit[] recoveredInputSplits = input.createInputSplits(1);
    assertArrayEquals(cachedInputSplits, recoveredInputSplits);

    // actual create should only be called once
    assertEquals(splitsCreateCalledTime.get(), 1);
  }
}