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
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.ColumnPrecisionInfo;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.PageDataInfo;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.VarLenColumnBulkInputCallback;

/** Handles variable data types. */
final class VarLenNullableEntryReader extends VarLenAbstractPageEntryReader {

  VarLenNullableEntryReader(ByteBuffer buffer,
      PageDataInfo pageInfo,
      ColumnPrecisionInfo columnPrecInfo,
      VarLenColumnBulkEntry entry,
      VarLenColumnBulkInputCallback containerCallback) {

    super(buffer, pageInfo, columnPrecInfo, entry, containerCallback);
  }

  /** {@inheritDoc} */
  @Override
  VarLenColumnBulkEntry getEntry(int valuesToRead) {
    assert valuesToRead > 0;

    // Bulk processing is effective for smaller precisions
    if (bulkProcess()) {
      return getEntryBulk(valuesToRead);
    }
    return getEntrySingle(valuesToRead);
  }

  VarLenColumnBulkEntry getEntryBulk(int valuesToRead) {

    load(true); // load new data to process

    final int[] valueLengths = entry.getValuesLength();
    final int readBatch = Math.min(entry.getMaxEntries(), valuesToRead);
    Preconditions.checkState(readBatch > 0, "Read batch count [%s] should be greater than zero", readBatch);

    final byte[] tgtBuff = entry.getInternalDataArray();
    final byte[] srcBuff = buffer.array();
    final int srcLen = buffer.remaining();
    final int tgtLen = tgtBuff.length;

    // Counters
    int numValues = 0;
    int numNulls = 0;
    int tgtPos = 0;
    int srcPos = 0;

    // Initialize the reader if needed
    pageInfo.definitionLevels.readFirstIntegerIfNeeded();

    for (; numValues < readBatch; ) {
      // Non-null entry
      if (pageInfo.definitionLevels.readCurrInteger() == 1) {
        if (srcPos > srcLen - 4) {
          break;
        }

        final int dataLen = getInt(srcBuff, srcPos);
        srcPos += 4;

        if (srcLen < (srcPos + dataLen)
         || tgtLen < (tgtPos + dataLen)) {

          break;
        }

        valueLengths[numValues++] = dataLen;

        if (dataLen > 0) {
          vlCopy(srcBuff, srcPos, tgtBuff, tgtPos, dataLen);

          // Update the counters
          srcPos += dataLen;
          tgtPos += dataLen;
        }

      } else { // Null value
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

    // Update the page data buffer offset
    final int numNonNullValues = numValues - numNulls;
    pageInfo.pageDataOff += (numNonNullValues * 4 + tgtPos);

    if (remainingPageData() < 0) {
      final String message = String.format("Invalid Parquet page data offset [%d]..", pageInfo.pageDataOff);
      throw new DrillRuntimeException(message);
    }

    // Now set the bulk entry
    entry.set(0, tgtPos, numValues, numNonNullValues);

    return entry;
  }

  VarLenColumnBulkEntry getEntrySingle(int valuesToRead) {

    // Initialize the reader if needed
    pageInfo.definitionLevels.readFirstIntegerIfNeeded();

    final int[] valueLengths = entry.getValuesLength();

    if (pageInfo.definitionLevels.readCurrInteger() == 1) {

      if (remainingPageData() < 4) {
        final String message = String.format("Invalid Parquet page metadata; cannot process advertised page count..");
        throw new DrillRuntimeException(message);
      }

      final int dataLen = pageInfo.pageData.getInt(pageInfo.pageDataOff);

      if (remainingPageData() < (4 + dataLen)) {
        final String message = String.format("Invalid Parquet page metadata; cannot process advertised page count..");
        throw new DrillRuntimeException(message);
      }

      // Is there enough memory to handle this large value?
      if (batchMemoryConstraintsReached(1, 4, dataLen)) {
        entry.set(0, 0, 0, 0); // no data to be consumed
        return entry;
      }

      // Register the length
      valueLengths[0] = dataLen;

      // Now set the bulk entry
      entry.set(pageInfo.pageDataOff + 4, dataLen, 1, 1, pageInfo.pageData);

      // Update the page data buffer offset
      pageInfo.pageDataOff += (dataLen + 4);

    } else { // Null value
      valueLengths[0] = -1;

      // Now set the bulk entry
      entry.set(0, 0, 1, 0);
    }

    // read the next definition-level value since we know the current entry has been processed
    pageInfo.definitionLevels.nextIntegerIfNotEOF();

    return entry;
  }
}
