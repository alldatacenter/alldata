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

import com.netease.arctic.flink.read.hybrid.enumerator.ArcticEnumeratorOffset;
import com.netease.arctic.flink.read.hybrid.enumerator.ContinuousEnumerationResult;
import com.netease.arctic.flink.read.hybrid.enumerator.ContinuousSplitPlanner;
import com.netease.arctic.flink.read.hybrid.reader.DataIteratorReaderFunction;
import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.hive.io.reader.AbstractAdaptHiveArcticDataReader;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.io.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicReference;

/**
 * This is a mixed-format table(mixed iceberg, mixed-hive) incremental loader.
 */
public class MixedIncrementalLoader<T> implements AutoCloseable {
  private static final Logger LOG = LoggerFactory.getLogger(MixedIncrementalLoader.class);
  private final ContinuousSplitPlanner continuousSplitPlanner;
  private DataIteratorReaderFunction<T> readerFunction;
  private AbstractAdaptHiveArcticDataReader<T> flinkArcticMORDataReader;
  private final List<Expression> filters;
  private final AtomicReference<ArcticEnumeratorOffset> enumeratorPosition;
  private final Queue<ArcticSplit> splitQueue;

  public MixedIncrementalLoader(
      ContinuousSplitPlanner continuousSplitPlanner,
      AbstractAdaptHiveArcticDataReader<T> flinkArcticMORDataReader,
      DataIteratorReaderFunction<T> readerFunction,
      List<Expression> filters) {
    this.continuousSplitPlanner = continuousSplitPlanner;
    this.flinkArcticMORDataReader = flinkArcticMORDataReader;
    this.readerFunction = readerFunction;
    this.filters = filters;
    this.enumeratorPosition = new AtomicReference<>();
    this.splitQueue = new ArrayDeque<>();
  }

  public MixedIncrementalLoader(
      ContinuousSplitPlanner continuousSplitPlanner,
      DataIteratorReaderFunction<T> readerFunction,
      List<Expression> filters) {
    this.continuousSplitPlanner = continuousSplitPlanner;
    this.readerFunction = readerFunction;
    this.filters = filters;
    this.enumeratorPosition = new AtomicReference<>();
    this.splitQueue = new ArrayDeque<>();
  }

  public boolean hasNext() {
    if (splitQueue.isEmpty()) {
      ContinuousEnumerationResult planResult =
          continuousSplitPlanner.planSplits(enumeratorPosition.get(), filters);
      if (!planResult.isEmpty()) {
        planResult.splits().forEach(split -> LOG.info("Putting this split into queue: {}.", split));
        splitQueue.addAll(planResult.splits());
      }
      if (!planResult.toOffset().isEmpty()) {
        enumeratorPosition.set(planResult.toOffset());
      }
      LOG.info("Currently, queue contain {} splits, scan position is {}.", splitQueue.size(), enumeratorPosition.get());
      return !splitQueue.isEmpty();
    }
    return true;
  }

  public CloseableIterator<T> next() {
    ArcticSplit split = splitQueue.poll();
    if (split == null) {
      throw new IllegalStateException("next() called, but no more valid splits");
    }

    LOG.info("Fetching data by this split:{}.", split);
    if (split.isMergeOnReadSplit()) {
      return flinkArcticMORDataReader.readData(split.asMergeOnReadSplit().keyedTableScanTask());
    }
    return readerFunction.createDataIterator(split);
  }

  @Override
  public void close() throws Exception {
    continuousSplitPlanner.close();
  }
}
