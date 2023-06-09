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

package com.netease.arctic.flink.read.hybrid.reader;

import com.netease.arctic.flink.read.hybrid.split.ArcticSplit;
import com.netease.arctic.flink.read.hybrid.split.ChangelogSplit;
import com.netease.arctic.flink.read.hybrid.split.SnapshotSplit;
import org.apache.flink.api.connector.source.SourceReaderContext;
import org.apache.flink.connector.base.source.reader.RecordsBySplits;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.flink.connector.base.source.reader.splitreader.SplitReader;
import org.apache.flink.connector.base.source.reader.splitreader.SplitsAddition;
import org.apache.flink.connector.base.source.reader.splitreader.SplitsChange;
import org.apache.iceberg.io.CloseableIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Queue;

/**
 * A hybrid source split reader that could read {@link SnapshotSplit} and {@link ChangelogSplit}.
 */
public class HybridSplitReader<T> implements SplitReader<ArcticRecordWithOffset<T>, ArcticSplit> {
  private static final Logger LOG = LoggerFactory.getLogger(HybridSplitReader.class);

  private final ReaderFunction<T> openSplitFunction;
  private final int indexOfSubtask;
  private final Queue<ArcticSplit> splits;

  private CloseableIterator<RecordsWithSplitIds<ArcticRecordWithOffset<T>>> currentReader;
  private String currentSplitId;

  public HybridSplitReader(ReaderFunction<T> openSplitFunction,
                           SourceReaderContext context) {
    this.openSplitFunction = openSplitFunction;
    this.indexOfSubtask = context.getIndexOfSubtask();
    this.splits = new ArrayDeque<>();
  }

  @Override
  public RecordsWithSplitIds<ArcticRecordWithOffset<T>> fetch() throws IOException {
    if (currentReader == null) {
      if (splits.isEmpty()) {
        return new RecordsBySplits<>(Collections.emptyMap(), Collections.emptySet());
      }
      ArcticSplit arcticSplit = splits.poll();
      currentReader = openSplitFunction.apply(arcticSplit);
      currentSplitId = arcticSplit.splitId();
    }
    if (currentReader.hasNext()) {
      // Because Iterator#next() doesn't support checked exception,
      // we need to wrap and unwrap the checked IOException with UncheckedIOException
      try {
        return currentReader.next();
      } catch (UncheckedIOException e) {
        throw e.getCause();
      }
    } else {
      return finishSplit();
    }
  }

  @Override
  public void handleSplitsChanges(SplitsChange<ArcticSplit> splitsChange) {
    if (!(splitsChange instanceof SplitsAddition)) {
      throw new UnsupportedOperationException(
          String.format(
              "The SplitChange type of %s is not supported.",
              splitsChange.getClass()));
    }
    LOG.info("Handling a split change {}.", splitsChange);

    splitsChange.splits().forEach(arcticSplit -> {
      if (arcticSplit instanceof SnapshotSplit || arcticSplit instanceof ChangelogSplit) {
        splits.add(arcticSplit);
      } else {
        throw new IllegalArgumentException(
            String.format(
                "As of now, The %s of SourceSplit type is unsupported, available source splits are %s, %s.",
                arcticSplit.getClass().getSimpleName(),
                SnapshotSplit.class.getSimpleName(),
                ChangelogSplit.class.getSimpleName()));
      }
    });
  }

  @Override
  public void wakeUp() {
  }

  @Override
  public void close() throws Exception {
    currentSplitId = null;
    if (currentReader != null) {
      currentReader.close();
    }
  }

  private RecordsWithSplitIds<ArcticRecordWithOffset<T>> finishSplit() throws IOException {
    if (currentReader != null) {
      currentReader.close();
      currentReader = null;
    }
    ArrayBatchRecords<T> finishRecords = ArrayBatchRecords.finishedSplit(currentSplitId);
    LOG.info("Split reader {} finished split: {}", indexOfSubtask, currentSplitId);
    currentSplitId = null;
    return finishRecords;
  }
}
