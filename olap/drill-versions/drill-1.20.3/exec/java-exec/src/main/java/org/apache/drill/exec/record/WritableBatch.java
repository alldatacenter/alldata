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
package org.apache.drill.exec.record;

import io.netty.buffer.DrillBuf;

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.proto.UserBitShared.RecordBatchDef;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.BatchSchema.SelectionVectorMode;
import org.apache.drill.exec.vector.ValueVector;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * A specialized version of record batch that can moves out buffers and preps
 * them for writing.
 */
public class WritableBatch implements AutoCloseable {

  private final RecordBatchDef def;
  private final DrillBuf[] buffers;
  private boolean cleared = false;

  private WritableBatch(RecordBatchDef def, List<DrillBuf> buffers) {
    this.def = def;
    this.buffers = buffers.toArray(new DrillBuf[buffers.size()]);
  }

  private WritableBatch(RecordBatchDef def, DrillBuf[] buffers) {
    super();
    this.def = def;
    this.buffers = buffers;
  }

  public WritableBatch transfer(BufferAllocator allocator) {
    List<DrillBuf> newBuffers = new ArrayList<>();
    for (DrillBuf buf : buffers) {
      int writerIndex = buf.writerIndex();
      DrillBuf newBuf = buf.transferOwnership(allocator).buffer;
      newBuf.writerIndex(writerIndex);
      newBuffers.add(newBuf);
    }
    clear();
    return new WritableBatch(def, newBuffers);
  }

  public RecordBatchDef getDef() {
    return def;
  }

  public DrillBuf[] getBuffers() {
    return buffers;
  }

  public void reconstructContainer(BufferAllocator allocator, VectorContainer container) {
    Preconditions.checkState(!cleared,
        "Attempted to reconstruct a container from a WritableBatch after it had been cleared");
    // If we have DrillBuf's associated with value vectors
    if (buffers.length > 0) {
      int len = 0;
      for (DrillBuf b : buffers) {
        len += b.capacity();
      }

      DrillBuf newBuf = allocator.buffer(len);
      try {
        // Copy data from each buffer into the compound buffer
        int offset = 0;
        for (DrillBuf buf : buffers) {
          newBuf.setBytes(offset, buf);
          offset += buf.capacity();
          buf.release();
        }

        List<SerializedField> fields = def.getFieldList();

        int bufferOffset = 0;

        // For each value vector slice up the appropriate size from the
        // compound buffer and load it into the value vector

        int vectorIndex = 0;

        for (VectorWrapper<?> vv : container) {
          SerializedField fmd = fields.get(vectorIndex);
          ValueVector v = vv.getValueVector();
          DrillBuf bb = newBuf.slice(bufferOffset, fmd.getBufferLength());
          v.load(fmd, bb);
          vectorIndex++;
          bufferOffset += fmd.getBufferLength();
        }
      } finally {
        // Any vectors that loaded material from newBuf slices above will retain those.
        newBuf.release(1);
      }
    }

    SelectionVectorMode svMode;
    if (def.hasCarriesTwoByteSelectionVector() && def.getCarriesTwoByteSelectionVector()) {
      svMode = SelectionVectorMode.TWO_BYTE;
    } else {
      svMode = SelectionVectorMode.NONE;
    }
    container.buildSchema(svMode);
    container.setValueCount(def.getRecordCount());
  }

  public void clear() {
    if (cleared) {
      return;
    }
    for (DrillBuf buf : buffers) {
      buf.release();
    }
    cleared = true;
  }

  public static WritableBatch getBatchNoHVWrap(int recordCount, Iterable<VectorWrapper<?>> vws, boolean isSV2) {
    List<ValueVector> vectors = new ArrayList<>();
    for (VectorWrapper<?> vw : vws) {
      Preconditions.checkArgument(!vw.isHyper());
      vectors.add(vw.getValueVector());
    }
    return getBatchNoHV(recordCount, vectors, isSV2);
  }

  public static WritableBatch getBatchNoHV(int recordCount, Iterable<ValueVector> vectors, boolean isSV2) {
    List<DrillBuf> buffers = new ArrayList<>();
    List<SerializedField> metadata = new ArrayList<>();

    for (ValueVector vv : vectors) {
      metadata.add(vv.getMetadata());

      // don't try to get the buffers if we don't have any records. It is possible the buffers are dead buffers.
      if (recordCount == 0) {
        vv.clear();
        continue;
      }

      for (DrillBuf b : vv.getBuffers(true)) {
        buffers.add(b);
      }
      // remove vv access to buffers.
      vv.clear();
    }

    RecordBatchDef batchDef = RecordBatchDef.newBuilder()
        .addAllField(metadata)
        .setRecordCount(recordCount)
        .setCarriesTwoByteSelectionVector(isSV2)
        .build();
    WritableBatch b = new WritableBatch(batchDef, buffers);
    return b;
  }

  public static WritableBatch get(VectorAccessible batch) {
    if (batch.getSchema() != null && batch.getSchema().getSelectionVectorMode() == SelectionVectorMode.FOUR_BYTE) {
      throw new UnsupportedOperationException("Only batches without hyper selections vectors are writable.");
    }

    boolean sv2 = (batch.getSchema().getSelectionVectorMode() == SelectionVectorMode.TWO_BYTE);
    return getBatchNoHVWrap(batch.getRecordCount(), batch, sv2);
  }

  public void retainBuffers(final int increment) {
    for (final DrillBuf buf : buffers) {
      buf.retain(increment);
    }
  }

  @Override
  public void close() {
    for(final DrillBuf drillBuf : buffers) {
      drillBuf.release(1);
    }
  }
}
