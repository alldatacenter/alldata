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
package org.apache.drill.exec.store.parquet.columnreaders.batchsizing;

import io.netty.buffer.DrillBuf;
import java.util.List;
import java.util.Map;
import org.apache.drill.common.map.CaseInsensitiveMap;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchOverflow.FieldOverflowDefinition;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchOverflow.FieldOverflowEntry;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchOverflow.RecordOverflowContainer;
import org.apache.drill.exec.store.parquet.columnreaders.batchsizing.RecordBatchOverflow.RecordOverflowDefinition;
import org.apache.drill.exec.util.record.RecordBatchStats;
import org.apache.drill.exec.util.record.RecordBatchStats.RecordBatchStatsContext;
import org.apache.drill.exec.vector.UInt1Vector;
import org.apache.drill.exec.vector.UInt4Vector;

/**
 * Field overflow SERDE utility; note that overflow data is serialized as a way to minimize
 * memory usage. This information is deserialized back to ValueVectors when it is needed in
 * the next batch.
 *
 * <p><b>NOTE -</b>We use a specialized implementation for overflow SERDE (instead of reusing
 * existing ones) because of the following reasons:
 * <ul>
 * <li>We want to only serialize a subset of the VV data
 * <li>Other SERDE methods will not copy the data contiguously and instead rely on the
 *     RPC layer to write the drill buffers in the correct order so that they are
 *     de-serialized as a single contiguous drill buffer
 * </ul>
 */
final class OverflowSerDeUtil {

  /**
   * Serializes a collection of overflow fields into a memory buffer:
   * <ul>
   * <li>Serialization logic can handle a subset of values (should be contiguous)
   * <li>Serialized data is copied into a single DrillBuf
   * <li>Currently, only variable length data is supported
   * </ul>
   *
   * @param fieldOverflowEntries input collection of field overflow entries
   * @param allocator buffer allocator
   * @param batchStatsContext batch statistics context object
   * @return record overflow container; null if the input buffer is empty
   */
  static RecordOverflowContainer serialize(List<FieldOverflowEntry> fieldOverflowEntries,
    BufferAllocator allocator,
    RecordBatchStatsContext batchStatsContext) {

    if (fieldOverflowEntries == null || fieldOverflowEntries.isEmpty()) {
      return null;
    }

    // We need to:
    // - Construct a map of VLVectorSerDe for each overflow field
    // - Compute the total space required for efficient serialization of all overflow data
    final Map<String, VLVectorSerializer> fieldSerDeMap = CaseInsensitiveMap.newHashMap();
    int bufferLength = 0;

    for (FieldOverflowEntry fieldOverflowEntry : fieldOverflowEntries) {
      final VLVectorSerializer fieldVLSerDe = new VLVectorSerializer(fieldOverflowEntry);
      fieldSerDeMap.put(fieldOverflowEntry.vector.getField().getName(), fieldVLSerDe);

      bufferLength += fieldVLSerDe.getBytesUsed(fieldOverflowEntry.firstValueIdx, fieldOverflowEntry.numValues);
    }
    assert bufferLength >= 0;

    // Allocate the required memory to serialize the overflow fields
    final DrillBuf buffer = allocator.buffer(bufferLength);

    RecordBatchStats.logRecordBatchStats(batchStatsContext,
      "Allocated a buffer of length [%d] to handle overflow", bufferLength);

    // Create the result object
    final RecordOverflowContainer recordOverflowContainer = new RecordOverflowContainer();
    final RecordOverflowDefinition recordOverflowDef = recordOverflowContainer.recordOverflowDef;

    // Now serialize field overflow into the drill buffer
    int bufferOffset = 0;
    FieldSerializerContainer fieldSerializerContainer = new FieldSerializerContainer();

    for (FieldOverflowEntry fieldOverflowEntry : fieldOverflowEntries) {
      fieldSerializerContainer.clear();

      // Serialize the field overflow data into the buffer
      VLVectorSerializer fieldSerDe = fieldSerDeMap.get(fieldOverflowEntry.vector.getField().getName());
      assert fieldSerDe != null;

      fieldSerDe.copyValueVector(fieldOverflowEntry.firstValueIdx,
        fieldOverflowEntry.numValues,
        buffer,
        bufferOffset,
        fieldSerializerContainer);

      // Create a view DrillBuf for isolating this field overflow data
      DrillBuf fieldBuffer = buffer.slice(bufferOffset, fieldSerializerContainer.totalByteLength);
      fieldBuffer.retain(1); // Increase the reference count
      fieldBuffer.writerIndex(fieldSerializerContainer.totalByteLength);

      // Enqueue a field overflow definition object for the current field
      FieldOverflowDefinition fieldOverflowDef = new FieldOverflowDefinition(
        fieldOverflowEntry.vector.getField(),
        fieldOverflowEntry.numValues,
        fieldSerializerContainer.dataByteLen,
        fieldBuffer);

      recordOverflowDef.getFieldOverflowDefs().put(fieldOverflowEntry.vector.getField().getName(), fieldOverflowDef);

      // Update this drill buffer current offset
      bufferOffset += fieldSerializerContainer.totalByteLength;
    }

    // Finally, release the original buffer
    boolean isReleased = buffer.release();
    assert !isReleased; // the reference count should still be higher than zero

    return recordOverflowContainer;
  }

  /** Disabling object instantiation */
  private  OverflowSerDeUtil() {
    // NOOP
  }

// ----------------------------------------------------------------------------
// Internal Data Structure
// ----------------------------------------------------------------------------

  /** Container class to store the result of field overflow serialization */
  private static final class FieldSerializerContainer {
    /** Data byte length */
    int dataByteLen;
    /** Total byte length */
    int totalByteLength;

    void clear() {
      dataByteLen = 0;
      totalByteLength = 0;
    }
  }

  /**
   * Helper class for handling variable length {@link ValueVector} overflow serialization logic
   */
  private static final class VLVectorSerializer {
    private static final int BYTE_VALUE_WIDTH = UInt1Vector.VALUE_WIDTH;
    private static final int INT_VALUE_WIDTH  = UInt4Vector.VALUE_WIDTH;

    /** Field overflow entry */
    private final FieldOverflowEntry fieldOverflowEntry;

    /** Set of DrillBuf's that make up the underlying ValueVector. Only nullable
     * (VarChar or VarBinary) vectors have three entries. The format is
     * ["bits-vector"] "offsets-vector" "values-vector"
     */
    private final DrillBuf[] buffers;

    /**
     * Constructor.
     * @param fieldOverflowEntry field overflow entry
     */
    private VLVectorSerializer(FieldOverflowEntry fieldOverflowEntry) {
      this.fieldOverflowEntry = fieldOverflowEntry;
      this.buffers = this.fieldOverflowEntry.vector.getBuffers(false);
    }

    /**
     * The number of bytes used (by the {@link ValueVector}) to store a value range delimited
     * by the parameters "firstValueIdx" and "numValues".
     *
     * @param firstValueIdx start index of the first value to copy
     * @param numValues number of values to copy
     */
    private int getBytesUsed(int firstValueIdx, int numValues) {
      int bytesUsed = 0;

      // Only nullable (VarChar or VarBinary) vectors have three entries. The format is
      // ["bits-vector"] "offsets-vector" "values-vector"
      if (isNullable()) {
        bytesUsed += numValues * BYTE_VALUE_WIDTH;
      }

      // Add the length of the "offsets" vector for the requested range
      bytesUsed += (numValues + 1) * INT_VALUE_WIDTH;

      // Add the length of the "values" vector for the requested range
      bytesUsed += getDataLen(firstValueIdx, numValues);

      return bytesUsed;
    }

    private void copyValueVector(int firstValueIdx,
      int numValues,
      DrillBuf targetBuffer,
      int targetStartIdx,
      FieldSerializerContainer fieldSerializerContainer) {

      int len = 0;
      int totalLen = 0;

      // First copy the "bits" vector
      len = copyBitsVector(firstValueIdx, numValues, targetBuffer, targetStartIdx);
      assert len >= 0;

      // Then copy the "offsets" vector
      totalLen += len;
      len = copyOffsetVector(firstValueIdx, numValues, targetBuffer, targetStartIdx + totalLen);
      assert len >= 0;

      // Then copy the "values" vector
      totalLen += len;
      len = copyValuesVector(firstValueIdx, numValues, targetBuffer, targetStartIdx + totalLen);
      assert len >= 0;

      // Finally, update the field serializer container
      fieldSerializerContainer.dataByteLen = len;
      fieldSerializerContainer.totalByteLength = (totalLen + len);
    }

    /**
     * Copy the "bits" vector if the {@link ValueVector} is nullable; NOOP otherwise.
     *
     * @param firstValueIdx start index of the first value to copy
     * @param numValues number of values to copy
     * @param targetBuffer target buffer
     * @param targetStartIdx target buffer start index
     *
     * @return number of bytes written
     */
    private int copyBitsVector(int firstValueIdx, int numValues, DrillBuf targetBuffer, int targetStartIdx) {
      int bytesCopied = 0;

      if (!isNullable()) {
        return bytesCopied;
      }

      DrillBuf srcBuffer = getBitsBuffer();
      assert srcBuffer != null;

      bytesCopied = numValues * BYTE_VALUE_WIDTH;

      // Now copy the bits data into the target buffer
      srcBuffer.getBytes(firstValueIdx * BYTE_VALUE_WIDTH,
        targetBuffer,
        targetStartIdx,
        bytesCopied);

      return bytesCopied;
    }

    /**
     * Copy the "offset" vector if the {@link ValueVector}; note that no adjustment of the offsets will be done.
     * This task will be done during overflow data de-serialization.
     *
     * @param firstValueIdx start index of the first value to copy
     * @param numValues number of values to copy
     * @param targetBuffer target buffer
     * @param targetStartIdx target buffer start index
     *
     * @return number of bytes written
     */
    private int copyOffsetVector(int firstValueIdx,
      int numValues,
      DrillBuf targetBuffer,
      int targetStartIdx) {

      final int bytesCopied = (numValues + 1) * INT_VALUE_WIDTH;

      DrillBuf srcBuffer = getOffsetsBuffer();
      assert srcBuffer != null;

      // Now copy the bits data into the target buffer
      srcBuffer.getBytes(firstValueIdx * INT_VALUE_WIDTH,
        targetBuffer,
        targetStartIdx,
        bytesCopied);

      return bytesCopied;
    }

    /**
     * Copy the "values" vector if the {@link ValueVector}.
     *
     * @param firstValueIdx start index of the first value to copy
     * @param numValues number of values to copy
     * @param targetBuffer target buffer
     * @param targetStartIdx target buffer start index
     *
     * @return number of bytes written
     */
    private int copyValuesVector(int firstValueIdx,
      int numValues,
      DrillBuf targetBuffer,
      int targetStartIdx) {

      final DrillBuf offsets = getOffsetsBuffer();
      final int startDataOffset = offsets.getInt(firstValueIdx * INT_VALUE_WIDTH);
      final int bytesCopied = getDataLen(firstValueIdx, numValues);
      final DrillBuf srcBuffer = getValuesBuffer();
      assert srcBuffer  != null;

      // Now copy the bits data into the target buffer
      srcBuffer.getBytes(startDataOffset,
        targetBuffer,
        targetStartIdx,
        bytesCopied);

      return bytesCopied;
    }

    /**
     *
     * @param firstValueIdx start index of the first value to copy
     * @param numValues number of values to copy
     *
     * @return data length contained in the range [first-val, (first-val+range-len)]
     */
    private int getDataLen(int firstValueIdx, int numValues) {
      // Data values for a range can be copied by the following formula:
      // offsets[firstValueIdx+numValues] - offsets[firstValueIdx]
      final DrillBuf offsets = getOffsetsBuffer();
      final int endDataOffset = offsets.getInt((firstValueIdx + numValues) * INT_VALUE_WIDTH);
      final int startDataOffset = offsets.getInt(firstValueIdx * INT_VALUE_WIDTH);

      return endDataOffset - startDataOffset;

    }

    /** @return "bits" buffer; null if the associated {@link ValueVector} is not nullable */
    private DrillBuf getBitsBuffer() {
      return buffers.length == 3 ? buffers[0] : null;
    }

    /** @return "offsets" buffer */
    private DrillBuf getOffsetsBuffer() {
      return buffers.length == 3 ? buffers[1] : buffers[0];
    }

    /** @return "values" buffer */
    private DrillBuf getValuesBuffer() {
      return buffers.length == 3 ? buffers[2] : buffers[1];
    }

    /** @return whether this vector is nullable */
    private boolean isNullable() {
      return getBitsBuffer() != null;
    }

  } // End of VLVectorSerDe class

}
