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

import com.netease.arctic.flink.read.hybrid.split.ArcticSplitState;
import org.apache.flink.api.connector.source.SourceOutput;
import org.apache.flink.connector.base.source.reader.RecordEmitter;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.TimestampData;
import org.apache.flink.table.data.utils.JoinedRowData;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Emitter that emit {@link T} to the next flink operator and update the record offset of {@link T} into split state.
 */
public class ArcticRecordEmitter<T> implements RecordEmitter<ArcticRecordWithOffset<T>, T, ArcticSplitState> {

  public static final Logger LOGGER = LoggerFactory.getLogger(ArcticRecordEmitter.class);

  /**
   * It signifies whether the Long.MIN_VALUE need to be set into RowData.
   */
  public boolean populateRowTime;

  public ArcticRecordEmitter(boolean populateRowTime) {
    this.populateRowTime = populateRowTime;
  }

  @Override
  public void emitRecord(
      ArcticRecordWithOffset<T> element,
      SourceOutput<T> sourceOutput,
      ArcticSplitState split) throws Exception {
    T record = element.record();
    if (!populateRowTime) {
      sourceOutput.collect(record);
    } else {
      Preconditions.checkArgument(record instanceof RowData,
          "Custom watermark strategy doesn't support %s, except RowData for now.",
          record.getClass());
      RowData rowData = new JoinedRowData((RowData) record,
          GenericRowData.of(TimestampData.fromEpochMillis(Long.MIN_VALUE)));
      rowData.setRowKind(((RowData) record).getRowKind());
      sourceOutput.collect((T) rowData);
    }
    split.updateOffset(new Object[]{element.insertFileOffset(), element.insertRecordOffset(),
        element.deleteFileOffset(), element.deleteRecordOffset()});
  }

}
