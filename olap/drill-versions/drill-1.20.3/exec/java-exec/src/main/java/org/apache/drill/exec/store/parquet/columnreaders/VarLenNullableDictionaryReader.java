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
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.ValuesReaderWrapper;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.PageDataInfo;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.VarLenColumnBulkInputCallback;
import org.apache.parquet.io.api.Binary;

/** Handles nullable variable data types using a dictionary */
final class VarLenNullableDictionaryReader extends VarLenAbstractPageEntryReader {

  VarLenNullableDictionaryReader(ByteBuffer buffer,
    PageDataInfo pageInfo,
    ColumnPrecisionInfo columnPrecInfo,
    VarLenColumnBulkEntry entry,
    VarLenColumnBulkInputCallback containerCallback) {

    super(buffer, pageInfo, columnPrecInfo, entry, containerCallback);
  }

  /** {@inheritDoc} */
  @Override
  final VarLenColumnBulkEntry getEntry(int valuesToRead) {
    assert valuesToRead > 0;

    // Bulk processing is effecting for smaller precisions
    if (bulkProcess()) {
      return getEntryBulk(valuesToRead);
    }
    return getEntrySingle(valuesToRead);
  }

  private final VarLenColumnBulkEntry getEntryBulk(int valuesToRead) {
    final ValuesReaderWrapper valueReader = pageInfo.encodedValueReader;
    final int[] valueLengths = entry.getValuesLength();
    final int readBatch = Math.min(entry.getMaxEntries(), valuesToRead);
    Preconditions.checkState(readBatch > 0, "Read batch count [%s] should be greater than zero", readBatch);

    final byte[] tgtBuff = entry.getInternalDataArray();
    final int tgtLen = tgtBuff.length;

    // Counters
    int numValues = 0;
    int numNulls = 0;
    int tgtPos = 0;

    // Initialize the reader if needed
    pageInfo.definitionLevels.readFirstIntegerIfNeeded();

    for (int idx = 0; idx < readBatch; ++idx ) {
      if (pageInfo.definitionLevels.readCurrInteger() == 1) {
        final Binary currEntry = valueReader.getEntry();
        final int dataLen = currEntry.length();

        if (tgtLen < (tgtPos + dataLen)) {
          valueReader.pushBack(currEntry); // push back this value since we're exiting from the loop
          break;
        }

        valueLengths[numValues++] = dataLen;

        if (dataLen > 0) {
          vlCopyNoPadding(currEntry.getBytes(), 0, tgtBuff, tgtPos, dataLen);

          // Update the counters
          tgtPos += dataLen;
        }

      } else {
        valueLengths[numValues++] = -1;
        ++numNulls;
      }

      // read the next definition-level value since we know the current entry has been processed
      pageInfo.definitionLevels.nextIntegerIfNotEOF();
    }

    // We're here either because a) the Parquet metadata is wrong (advertises more values than the real count)
    // or the first value being processed ended up to be too long for the buffer.
    if (numValues == 0) {
      return getEntrySingle(valuesToRead);
    }

    entry.set(0, tgtPos, numValues, numValues - numNulls);

    return entry;
  }

  private final VarLenColumnBulkEntry getEntrySingle(int valsToReadWithinPage) {
    final int[] valueLengths = entry.getValuesLength();
    final ValuesReaderWrapper valueReader = pageInfo.encodedValueReader;

    // Initialize the reader if needed
    pageInfo.definitionLevels.readFirstIntegerIfNeeded();

    if (pageInfo.definitionLevels.readCurrInteger() == 1) {
      final Binary currEntry = valueReader.getEntry();
      final int dataLen = currEntry.length();

      // Is there enough memory to handle this large value?
      if (batchMemoryConstraintsReached(1, 4, dataLen)) {
        entry.set(0, 0, 0, 0); // no data to be consumed
        return entry;
      }

      // Set the value length
      valueLengths[0] = dataLen;

      // Now set the bulk entry
      entry.set(0, dataLen, 1, 1, currEntry.getBytes());

    } else {
      valueLengths[0] = -1;

      // Now set the bulk entry
      entry.set(0, 0, 1, 0);
    }

    // read the next definition-level value since we know the current entry has been processed
    pageInfo.definitionLevels.nextIntegerIfNotEOF();

    return entry;
  }
}
