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

import io.netty.buffer.DrillBuf;
import java.nio.ByteBuffer;

import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.VarLenColumnBulkInputCallback;

/** Abstract class for sub-classes implementing several algorithms for loading a Bulk Entry */
abstract class VarLenAbstractEntryReader {

  /** byte buffer used for buffering page data */
  protected final ByteBuffer buffer;
  /** Bulk entry */
  protected final VarLenColumnBulkEntry entry;
  /** A callback to allow bulk readers interact with their container */
  private final VarLenColumnBulkInputCallback containerCallback;

  /**
   * CTOR.
   * @param buffer byte buffer for data buffering (within CPU cache)
   * @param entry reusable bulk entry object
   * @param containerCallback callback to allow a bulk reader to interact with its parent
   */
  VarLenAbstractEntryReader(ByteBuffer buffer,
    VarLenColumnBulkEntry entry,
    VarLenColumnBulkInputCallback containerCallback) {

    this.buffer = buffer;
    this.entry = entry;
    this.containerCallback = containerCallback;
  }

  /**
   * @param valsToReadWithinPage maximum values to read within the current page
   * @return a bulk entry object
   */
  abstract VarLenColumnBulkEntry getEntry(int valsToReadWithinPage);

  /**
   * @param newBitsMemory new "bits" memory size
   * @param newOffsetsMemory new "offsets" memory size
   * @param newDataMemory new "data" memory size
   * @return true if the new payload ("bits", "offsets", "data") will trigger a constraint violation; false
   *         otherwise
   */
  protected boolean batchMemoryConstraintsReached(int newBitsMemory, int newOffsetsMemory, int newDataMemory) {
    return containerCallback.batchMemoryConstraintsReached(newBitsMemory, newOffsetsMemory, newDataMemory);
  }

  /**
   * @param buff source buffer
   * @param pos start position
   * @return an integer encoded as a low endian
   */
  static final int getInt(final byte[] buff, final int pos) {
    return DrillBuf.getInt(buff, pos);
  }

  /**
   * Copy data; expects 8bytes PADDING for source and target
   *
   * @param src source buffer
   * @param srcIndex source index
   * @param dest destination buffer
   * @param destIndex destination buffer
   * @param length length to copy (in bytes)
   */
  static void vlCopy(byte[] src, int srcIndex, byte[] dest, int destIndex, int length) {
    if (length <= 8) {
      DrillBuf.putLong(src, srcIndex, dest, destIndex);
    } else {
      vlCopyGTLongWithPadding(src, srcIndex, dest, destIndex, length);
    }
  }

  /**
   * Copy data; no PADDING needed for source and target (though less efficient than the copy with
   * padding as more comparisons need to be made).
   *
   * @param src source buffer
   * @param srcIndex source index
   * @param dest destination buffer
   * @param destIndex destination buffer
   * @param length length to copy (in bytes)
   */
  static void vlCopyNoPadding(byte[] src, int srcIndex, byte[] dest, int destIndex, int length) {
    if (length <= 8) {
      vlCopyLELongNoPadding(src, srcIndex, dest, destIndex, length);
    } else {
      vlCopyGTLongNoPadding(src, srcIndex, dest, destIndex, length);
    }
  }

  private static void vlCopyGTLongWithPadding(byte[] src, int srcIndex, byte[] dest, int destIndex, int length) {
    final int bulkCopyThreshold = 24;
    if (length < bulkCopyThreshold) {
      _vlCopyGTLongWithPadding(src, srcIndex, dest, destIndex, length);

    } else {
      System.arraycopy(src, srcIndex, dest, destIndex, length);
    }
  }

  private static void _vlCopyGTLongWithPadding(byte[] src, int srcIndex, byte[] dest, int destIndex, int length) {
    final int numLongEntries = length >> 3;
    assert numLongEntries < 3;

    final int remaining = length & 0x7;
    int prevCopied      = 0;

    if (numLongEntries == 1) {
      DrillBuf.putLong(src, srcIndex, dest, destIndex);
      prevCopied = 1 << 3;

    } else {
      DrillBuf.putLong(src, srcIndex, dest, destIndex);
      DrillBuf.putLong(src, srcIndex + DrillBuf.LONG_NUM_BYTES, dest, destIndex + DrillBuf.LONG_NUM_BYTES);
      prevCopied = 1 << 4;
    }

    if (remaining > 0) {
      final int srcPos  = srcIndex  + prevCopied;
      final int destPos = destIndex + prevCopied;

      DrillBuf.putLong(src, srcPos, dest, destPos);
    }
  }

  private static final void vlCopyLELongNoPadding(byte[] src, int srcIndex, byte[] dest, int destIndex, int length) {
    if (length == 1) {
      dest[destIndex] = src[srcIndex];

    } else if (length == 2) {
      DrillBuf.putShort(src, srcIndex, dest, destIndex);

    } else if (length == 3) {
      dest[destIndex] = src[srcIndex];
      DrillBuf.putShort(src, srcIndex+1, dest, destIndex+1);

    } else if (length == 4) {
      DrillBuf.putInt(src, srcIndex, dest, destIndex);

    } else if (length == 5) {
      dest[destIndex] = src[srcIndex];
      DrillBuf.putInt(src, srcIndex+1, dest, destIndex+1);

    } else if (length == 6) {
      DrillBuf.putShort(src, srcIndex, dest, destIndex);
      DrillBuf.putInt(src, srcIndex+2, dest, destIndex+2);

    } else if (length == 7) {
      dest[destIndex] = src[srcIndex];
      DrillBuf.putShort(src, srcIndex+1, dest, destIndex+1);
      DrillBuf.putInt(src, srcIndex+3, dest, destIndex+3);

    } else {
      DrillBuf.putLong(src, srcIndex, dest, destIndex);
    }
  }

  private static final void vlCopyGTLongNoPadding(byte[] src, int srcIndex, byte[] dest, int destIndex, int length) {
    final int numLongEntries = length >> 3;
    final int remaining      = length & 0x7;

    if (numLongEntries == 1) {
      DrillBuf.putLong(src, srcIndex, dest, destIndex);

    } else if (numLongEntries == 2) {
      DrillBuf.putLong(src, srcIndex, dest, destIndex);
      DrillBuf.putLong(src, srcIndex + DrillBuf.LONG_NUM_BYTES, dest, destIndex + DrillBuf.LONG_NUM_BYTES);

    } else if (numLongEntries == 3) {
      DrillBuf.putLong(src, srcIndex, dest, destIndex);
      DrillBuf.putLong(src, srcIndex + DrillBuf.LONG_NUM_BYTES, dest, destIndex + DrillBuf.LONG_NUM_BYTES);
      DrillBuf.putLong(src, srcIndex + 2 * DrillBuf.LONG_NUM_BYTES, dest, destIndex + 2 * DrillBuf.LONG_NUM_BYTES);

    } else if (numLongEntries == 4) {
      DrillBuf.putLong(src, srcIndex, dest, destIndex);
      DrillBuf.putLong(src, srcIndex + DrillBuf.LONG_NUM_BYTES, dest, destIndex + DrillBuf.LONG_NUM_BYTES);
      DrillBuf.putLong(src, srcIndex + 2 * DrillBuf.LONG_NUM_BYTES, dest, destIndex + 2 * DrillBuf.LONG_NUM_BYTES);
      DrillBuf.putLong(src, srcIndex + 3 * DrillBuf.LONG_NUM_BYTES, dest, destIndex + 3 * DrillBuf.LONG_NUM_BYTES);

    } else {
      for (int idx = 0; idx < numLongEntries; ++idx) {
        DrillBuf.putLong(src, srcIndex + idx * DrillBuf.LONG_NUM_BYTES, dest, destIndex + idx * DrillBuf.LONG_NUM_BYTES);
      }
    }

    if (remaining > 0) {
      final int srcPos  = srcIndex  + numLongEntries * DrillBuf.LONG_NUM_BYTES;
      final int destPos = destIndex + numLongEntries * DrillBuf.LONG_NUM_BYTES;

      if (srcPos + 7 < src.length) {
        DrillBuf.putLong(src, srcPos, dest, destPos);

      } else {
        vlCopyLELongNoPadding(src, srcPos, dest, destPos, remaining);
      }
    }
  }

}
