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
import com.netease.arctic.flink.read.source.ChangeLogDataIterator;
import com.netease.arctic.flink.read.source.DataIterator;
import org.apache.flink.connector.base.source.reader.RecordsWithSplitIds;
import org.apache.iceberg.io.CloseableIterator;

/**
 * A {@link ReaderFunction} implementation that uses {@link DataIterator}.
 */
public abstract class DataIteratorReaderFunction<T> implements ReaderFunction<T> {
  private final DataIteratorBatcher<T> batcher;

  public DataIteratorReaderFunction(DataIteratorBatcher<T> batcher) {
    this.batcher = batcher;
  }

  public abstract DataIterator<T> createDataIterator(ArcticSplit split);

  @Override
  public CloseableIterator<RecordsWithSplitIds<ArcticRecordWithOffset<T>>> apply(ArcticSplit split) {
    DataIterator<T> inputIterator = createDataIterator(split);
    if (inputIterator instanceof ChangeLogDataIterator) {
      ChangeLogDataIterator<T> changelogInputIterator = (ChangeLogDataIterator<T>) inputIterator;
      ChangelogSplit changelogSplit = split.asChangelogSplit();
      changelogInputIterator.seek(
          changelogSplit.insertFileOffset(),
          changelogSplit.deleteFileOffset(),
          changelogSplit.insertRecordOffset(),
          changelogSplit.deleteRecordOffset()
      );
    } else {
      inputIterator.seek(split.asSnapshotSplit().insertFileOffset(), split.asSnapshotSplit().insertRecordOffset());
    }
    return batcher.batch(split.splitId(), inputIterator);
  }
}
