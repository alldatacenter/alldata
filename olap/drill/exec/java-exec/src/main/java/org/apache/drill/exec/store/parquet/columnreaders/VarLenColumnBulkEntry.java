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

import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.ColumnPrecisionInfo;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.VarLenBulkEntry;

import io.netty.buffer.DrillBuf;

/** Implements the {@link VarLenBulkEntry} interface to optimize data copy */
final class VarLenColumnBulkEntry implements VarLenBulkEntry {
  private static final int PADDING = 1 << 6; // 128bytes padding to allow for access optimizations

  /** start data offset */
  private int startOffset;
  /** aggregate data length */
  private int totalLength;
  /** number of values contained within this bulk entry object */
  private int numValues;
  /** number of non-null values contained within this bulk entry object */
  private int numNonValues;
  /** a fixed length array that hosts value lengths */
  private final int[] lengths;
  /** internal byte array for data caching (small precision values) */
  private final byte[] internalArray;
  /** external byte array for data caching */
  private byte[] externalArray;
  /** reference to page reader drill buffer (larger precision values) */
  private DrillBuf externalDrillBuf;
  /** indicator on whether the current entry is array backed */
  private boolean arrayBacked;
  /** indicator on whether the current data buffer is externally or internally owned */
  private boolean internalDataBuf;
  /** indicates whether the entry was read from the overflow data or page data */
  private boolean readFromPage;

  VarLenColumnBulkEntry(ColumnPrecisionInfo columnPrecInfo) {
    this(columnPrecInfo, VarLenBulkPageReader.BUFF_SZ);
  }

  VarLenColumnBulkEntry(ColumnPrecisionInfo columnPrecInfo, int buffSz) {

    // For variable length data, we need to handle a) maximum number of entries
    // and b) max entry length. Note that we don't optimize for fixed length
    // columns as the reader can notice a false-positive (that is, the first
    // values were fixed but not the rest).
    final int largestDataLen = buffSz - UInt4Vector.VALUE_WIDTH;
    final int maxNumValues = buffSz / UInt4Vector.VALUE_WIDTH;
    final int lengthSz = maxNumValues;
    final int dataSz = largestDataLen + PADDING;

    this.lengths = new int[lengthSz];
    this.internalArray = new byte[dataSz];
  }

  /** @inheritDoc */
  @Override
  public int getTotalLength() {
    return totalLength;
  }

  /** @inheritDoc */
  @Override
  public boolean arrayBacked() {
    return arrayBacked;
  }

  /** @inheritDoc */
  @Override
  public byte[] getArrayData() {
    return internalDataBuf ? internalArray : externalArray;
  }

  /** @inheritDoc */
  @Override
  public DrillBuf getData() {
    return externalDrillBuf;
  }

  /** @inheritDoc */
  @Override
  public int getDataStartOffset() {
    return startOffset;
  }

  /** @inheritDoc */
  @Override
  public int[] getValuesLength() {
    return lengths;
  }

  /** @inheritDoc */
  @Override
  public int getNumValues() {
    return numValues;
  }

  /** @inheritDoc */
  @Override
  public int getNumNonNullValues() {
    return numNonValues;
  }

  @Override
  public boolean hasNulls() {
    return numNonValues < numValues;
  }

  public byte[] getInternalDataArray() {
    return internalArray;
  }

  void set(int startOffset, int totalLength, int numValues, int nonNullValues) {
    this.startOffset = startOffset;
    this.totalLength = totalLength;
    this.numValues = numValues;
    this.numNonValues = nonNullValues;
    this.arrayBacked = true;
    this.internalDataBuf = true;
    this.externalArray = null;
    this.externalDrillBuf = null;
  }

  void set(int startOffset, int totalLength, int numValues, int nonNullValues, byte[] externalArray) {
    this.startOffset = startOffset;
    this.totalLength = totalLength;
    this.numValues = numValues;
    this.numNonValues = nonNullValues;
    this.arrayBacked = true;
    this.internalDataBuf = false;
    this.externalArray = externalArray;
    this.externalDrillBuf = null;
  }

  void set(int startOffset, int totalLength, int numValues, int nonNullValues, DrillBuf externalDrillBuf) {
    this.startOffset = startOffset;
    this.totalLength = totalLength;
    this.numValues = numValues;
    this.numNonValues = nonNullValues;
    this.arrayBacked = false;
    this.internalDataBuf = false;
    this.externalArray = null;
    this.externalDrillBuf = externalDrillBuf;
  }

  int getMaxEntries() {
    return lengths.length;
  }

  /**
   * @return the readFromPage
   */
  boolean isReadFromPage() {
    return readFromPage;
  }

  /**
   * @param readFromPage the readFromPage to set
   */
  void setReadFromPage(boolean readFromPage) {
    this.readFromPage = readFromPage;
  }

}