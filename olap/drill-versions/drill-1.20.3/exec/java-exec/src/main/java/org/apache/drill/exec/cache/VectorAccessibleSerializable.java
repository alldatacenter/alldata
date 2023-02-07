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
package org.apache.drill.exec.cache;

import io.netty.buffer.DrillBuf;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.metrics.DrillMetrics;
import org.apache.drill.exec.proto.UserBitShared;
import org.apache.drill.exec.proto.UserBitShared.RecordBatchDef;
import org.apache.drill.exec.proto.UserBitShared.SerializedField;
import org.apache.drill.exec.record.BatchSchema;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.WritableBatch;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.drill.exec.vector.ValueVector;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;

/**
 * A wrapper around a VectorAccessible. Will serialize a VectorAccessible and
 * write to an OutputStream, or can read from an InputStream and construct a new
 * VectorContainer.
 */
public class VectorAccessibleSerializable extends AbstractStreamSerializable {
  static final MetricRegistry metrics = DrillMetrics.getRegistry();
  static final String WRITER_TIMER = MetricRegistry.name(VectorAccessibleSerializable.class, "writerTime");

  private VectorContainer va;
  private WritableBatch batch;
  private final BufferAllocator allocator;
  private int recordCount = -1;
  private BatchSchema.SelectionVectorMode svMode = BatchSchema.SelectionVectorMode.NONE;
  private SelectionVector2 sv2;
  private long timeNs;

  private boolean retain = false;

  public VectorAccessibleSerializable(BufferAllocator allocator) {
    this.allocator = allocator;
    va = new VectorContainer();
  }

  public VectorAccessibleSerializable(WritableBatch batch, BufferAllocator allocator) {
    this(batch, null, allocator);
  }

  /**
   * Creates a wrapper around batch and sv2 for writing to a stream. sv2 will
   * never be released by this class, and ownership is maintained by caller.
   *
   * @param batch
   * @param sv2
   * @param allocator
   */
  public VectorAccessibleSerializable(WritableBatch batch, SelectionVector2 sv2, BufferAllocator allocator) {
    this.allocator = allocator;
    this.batch = batch;
    if (sv2 != null) {
      this.sv2 = sv2;
      svMode = BatchSchema.SelectionVectorMode.TWO_BYTE;
    }
  }

  /**
   * Reads from an InputStream and parses a RecordBatchDef. From this, we
   * construct a SelectionVector2 if it exits and construct the vectors and add
   * them to a vector container
   *
   * @param input
   *          the InputStream to read from
   * @throws IOException
   */
  @Override
  public void readFromStream(InputStream input) throws IOException {
    final UserBitShared.RecordBatchDef batchDef = UserBitShared.RecordBatchDef.parseDelimitedFrom(input);
    recordCount = batchDef.getRecordCount();
    if (batchDef.hasCarriesTwoByteSelectionVector() && batchDef.getCarriesTwoByteSelectionVector()) {
      readSv2(input);
    }
    readVectors(input, batchDef);
  }

  private void readSv2(InputStream input) throws IOException {
    if (sv2 != null) {
      sv2.clear();
    }
    final int dataLength = recordCount * SelectionVector2.RECORD_SIZE;
    svMode = BatchSchema.SelectionVectorMode.TWO_BYTE;
    DrillBuf buf = allocator.read(dataLength, input);
    sv2 = new SelectionVector2(allocator, buf, recordCount);
    buf.release(); // SV2 now owns the buffer
  }

  private void readVectors(InputStream input, RecordBatchDef batchDef) throws IOException {
    final VectorContainer container = new VectorContainer();
    final List<ValueVector> vectorList = Lists.newArrayList();
    final List<SerializedField> fieldList = batchDef.getFieldList();
    for (SerializedField metaData : fieldList) {
      final int dataLength = metaData.getBufferLength();
      final MaterializedField field = MaterializedField.create(metaData);
      final DrillBuf buf = allocator.read(dataLength, input);
      final ValueVector vector = TypeHelper.getNewVector(field, allocator);
      vector.load(metaData, buf);
      buf.release(); // Vector now owns the buffer
      vectorList.add(vector);
    }
    container.addCollection(vectorList);
    container.buildSchema(svMode);
    container.setRecordCount(recordCount);
    va = container;
  }

  // Like above, only preserve the original container and list of value-vectors
  public void readFromStreamWithContainer(VectorContainer myContainer, InputStream input) throws IOException {
    final VectorContainer container = new VectorContainer();
    final UserBitShared.RecordBatchDef batchDef = UserBitShared.RecordBatchDef.parseDelimitedFrom(input);
    recordCount = batchDef.getRecordCount();
    if (batchDef.hasCarriesTwoByteSelectionVector() && batchDef.getCarriesTwoByteSelectionVector()) {

      if (sv2 == null) {
        sv2 = new SelectionVector2(allocator);
      }
      sv2.allocateNew(recordCount * SelectionVector2.RECORD_SIZE);
      sv2.getBuffer().setBytes(0, input, recordCount * SelectionVector2.RECORD_SIZE);
      svMode = BatchSchema.SelectionVectorMode.TWO_BYTE;
    }
    final List<ValueVector> vectorList = Lists.newArrayList();
    final List<SerializedField> fieldList = batchDef.getFieldList();
    for (SerializedField metaData : fieldList) {
      final int dataLength = metaData.getBufferLength();
      final MaterializedField field = MaterializedField.create(metaData);
      final DrillBuf buf = allocator.buffer(dataLength);
      final ValueVector vector;
      try {
        buf.writeBytes(input, dataLength);
        vector = TypeHelper.getNewVector(field, allocator);
        vector.load(metaData, buf);
      } finally {
        buf.release();
      }
      vectorList.add(vector);
    }
    container.addCollection(vectorList);
    container.setRecordCount(recordCount);
    myContainer.transferIn(container); // transfer the vectors
    myContainer.buildSchema(svMode);
    myContainer.setRecordCount(recordCount);
    /*
    // for debugging -- show values from the first row
    Object tmp0 = (myContainer).getValueAccessorById(NullableVarCharVector.class, 0).getValueVector();
    Object tmp1 = (myContainer).getValueAccessorById(NullableVarCharVector.class, 1).getValueVector();
    Object tmp2 = (myContainer).getValueAccessorById(NullableBigIntVector.class, 2).getValueVector();
    if (tmp0 != null && tmp1 != null && tmp2 != null) {
      NullableVarCharVector vv0 = ((NullableVarCharVector) tmp0);
      NullableVarCharVector vv1 = ((NullableVarCharVector) tmp1);
      NullableBigIntVector vv2 = ((NullableBigIntVector) tmp2);

      try {
        logger.info("HASH AGG: Got a row = {} , {} , {}", vv0.getAccessor().get(0), vv1.getAccessor().get(0), vv2.getAccessor().get(0));
      } catch (Exception e) { logger.info("HASH AGG: Got an exception = {}",e); }
    }
    else { logger.info("HASH AGG: got nulls !!!"); }
    */
    va = myContainer;
  }

  public void writeToStreamAndRetain(OutputStream output) throws IOException {
    retain = true;
    writeToStream(output);
  }

  /**
   * Serializes the VectorAccessible va and writes it to an output stream
   * @param output the OutputStream to write to
   * @throws IOException
   */
  @Override
  public void writeToStream(OutputStream output) throws IOException {
    Preconditions.checkNotNull(output);
    final Timer.Context timerContext = metrics.timer(WRITER_TIMER).time();

    final DrillBuf[] incomingBuffers = batch.getBuffers();
    final UserBitShared.RecordBatchDef batchDef = batch.getDef();

    try {
      /* Write the metadata to the file */
      batchDef.writeDelimitedTo(output);

      /* If we have a selection vector, dump it to file first */
      if (svMode == BatchSchema.SelectionVectorMode.TWO_BYTE) {
        recordCount = sv2.getCount();
        final int dataLength = recordCount * SelectionVector2.RECORD_SIZE;
        allocator.write(sv2.getBuffer(false), dataLength, output);
      }

      /* Dump the array of ByteBuf's associated with the value vectors */
      for (DrillBuf buf : incomingBuffers) {
        /* dump the buffer into the OutputStream */
        allocator.write(buf, output);
      }

      timeNs += timerContext.stop();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      clear();
    }
  }

  public void clear() {
    if (!retain) {
      batch.clear();
      if (sv2 != null) {
        sv2.clear();
      }
    }
  }

  public VectorContainer get() { return va; }

  public SelectionVector2 getSv2() { return sv2; }

  public long getTimeNs() { return timeNs; }
}
