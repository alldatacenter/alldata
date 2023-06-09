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

import java.nio.ByteBuffer;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.VarLenColumnBulkInputCallback;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.ColumnPrecisionInfo;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.PageDataInfo;

/** Abstract class for sub-classes implementing several strategies for loading a Bulk Entry from a Parquet page */
abstract class VarLenAbstractPageEntryReader extends VarLenAbstractEntryReader {

  protected final PageDataInfo pageInfo;
  /** expected precision type: fixed or variable length */
  protected final ColumnPrecisionInfo columnPrecInfo;

  /**
   * CTOR.
   * @param _buffer byte buffer for data buffering (within CPU cache)
   * @param _pageInfo page being processed information
   * @param _columnPrecInfo column precision information
   * @param _entry reusable bulk entry object
   */
  VarLenAbstractPageEntryReader(ByteBuffer _buffer,
    PageDataInfo _pageInfo,
    ColumnPrecisionInfo _columnPrecInfo,
    VarLenColumnBulkEntry _entry,
    VarLenColumnBulkInputCallback _containerCallback) {

    super(_buffer, _entry, _containerCallback);

    this.pageInfo = _pageInfo;
    this.columnPrecInfo    = _columnPrecInfo;
  }

  /**
   * Indicates whether to use bulk processing
   */
  protected final boolean bulkProcess() {
    return columnPrecInfo.bulkProcess;
  }

  /**
   * Loads new data into the buffer if empty or the force flag is set.
   *
   * @param force flag to force loading new data into the buffer
   */
  protected final boolean load(boolean force) {

    if (!force && buffer.hasRemaining()) {
      return true; // NOOP
    }

    // We're here either because the buffer is empty or we want to force a new load operation.
    // In the case of force, there might be unprocessed data (still in the buffer) which is fine
    // since the caller updates the page data buffer's offset only for the data it has consumed; this
    // means unread data will be loaded again but this time will be positioned in the beginning of the
    // buffer. This can happen only for the last entry in the buffer when either of its length or value
    // is incomplete.
    buffer.clear();

    int remaining = remainingPageData();
    int bufferCapacity = buffer.capacity() - VarLenBulkPageReader.PADDING;
    int toCopy = remaining > bufferCapacity ? bufferCapacity : remaining;

    buffer.limit(toCopy); // Update the limit regardless to indicate the number of bytes available for reading

    if (toCopy == 0) {
      return false;
    }

    pageInfo.pageData.getBytes(pageInfo.pageDataOff, buffer.array(), buffer.position(), toCopy);

    // At this point the buffer position is 0 and its limit set to the number of bytes copied.

    return true;
  }

  /**
   * @return remaining data in current page
   */
  protected final int remainingPageData() {
    return pageInfo.pageDataLen - pageInfo.pageDataOff;
  }

  /**
   * Fixed length readers calculate up front the maximum number of entries to process as entry length
   * are known.
   * @param valuesToRead requested number of values to read
   * @param entrySz sizeof(integer) + column's precision
   * @return maximum entries to read within each call (based on the bulk entry, entry size, and requested
   *         number of entries to read)
   */
  protected final int getFixedLengthMaxRecordsToRead(int valuesToRead, int entrySz) {
    // Let's start with bulk's entry and requested values-to-read constraints
    int numEntriesToRead = Math.min(entry.getMaxEntries(), valuesToRead);

    // The goal is to ensure that a) we're not returning more than what is requested and
    // b) ensure that we don't overflow while accessing the buffer
    final int bufferCapacity = buffer.capacity() - VarLenBulkPageReader.PADDING;
    numEntriesToRead = Math.min(numEntriesToRead, bufferCapacity / entrySz);

    return numEntriesToRead;
  }
}
