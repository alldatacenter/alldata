/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.parquet.columnreaders;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import java.nio.ByteBuffer;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.ColumnPrecisionInfo;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.PageDataInfo;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.VarLenColumnBulkInputCallback;

/** Handles nullable fixed data types that have been erroneously tagged as Variable Length. */
final class VarLenNullableFixedEntryReader extends VarLenAbstractPageEntryReader {

  VarLenNullableFixedEntryReader(ByteBuffer buffer,
    PageDataInfo pageInfo,
    ColumnPrecisionInfo columnPrecInfo,
    VarLenColumnBulkEntry entry,
    VarLenColumnBulkInputCallback containerCallback) {

    super(buffer, pageInfo, columnPrecInfo, entry, containerCallback);
    Preconditions.checkArgument(columnPrecInfo.precision >= 0, "Fixed length precision cannot be lower than zero");
  }

  /** {@inheritDoc} */
  @Override
  VarLenColumnBulkEntry getEntry(int valuesToRead) {
    // TODO - We should not use force reload for sparse columns (values with lot of nulls)
    load(true); // load new data to process

    final int expectedDataLen = columnPrecInfo.precision;
    final int entrySz = 4 + columnPrecInfo.precision;
    final int readBatch = getFixedLengthMaxRecordsToRead(valuesToRead, entrySz);
    Preconditions.checkState(readBatch > 0, "Read batch count [%s] should be greater than zero", readBatch);

    final int[] valueLengths = entry.getValuesLength();
    final byte[] tgtBuff = entry.getInternalDataArray();
    final byte[] srcBuff = buffer.array();
    int nonNullValues = 0;
    int idx = 0;

    // Fixed precision processing can directly operate on the raw definition-level reader as no peeking
    // is needed.
    final PageReader.IntIterator definitionLevels = pageInfo.definitionLevels.getUnderlyingReader();

    for ( ; idx < readBatch; ++idx) {
      if (definitionLevels.nextInt() == 1) {

        final int currPos = nonNullValues * entrySz;
        final int dataLen = getInt(srcBuff, currPos);

        if (dataLen != expectedDataLen) {
          return null; // this is a soft error; caller needs to revert to variable length processing
        }

        valueLengths[idx] = dataLen;
        final int tgt_pos = nonNullValues * expectedDataLen;

        if (expectedDataLen > 0) {
          vlCopy(srcBuff, currPos + 4, tgtBuff, tgt_pos, dataLen);
        }

        // Increase the non null values counter
        ++nonNullValues;

      } else { // Null value
        valueLengths[idx] = -1; // to mark a null value
      }
    }

    // Update the page data buffer offset
    pageInfo.pageDataOff += nonNullValues * entrySz;

    // Now set the bulk entry
    entry.set(0, nonNullValues * expectedDataLen, idx, nonNullValues);

    return entry;
  }

}
