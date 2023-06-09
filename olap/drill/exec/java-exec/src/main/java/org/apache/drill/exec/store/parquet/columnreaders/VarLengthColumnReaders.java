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
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.NullableVarCharVector;
import org.apache.drill.exec.vector.NullableVarDecimalVector;
import org.apache.drill.exec.vector.VarLenBulkEntry;
import org.apache.drill.exec.vector.VarLenBulkInput;
import org.apache.drill.exec.vector.VarBinaryVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.exec.vector.VarDecimalVector;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.values.ValuesReader;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;

public final class VarLengthColumnReaders {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VarLengthColumnReaders.class);

  public static class VarDecimalColumn extends VarLengthValuesColumn<VarDecimalVector> {

    protected VarDecimalVector varDecimalVector;
    protected VarDecimalVector.Mutator mutator;

    VarDecimalColumn(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                    ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, VarDecimalVector v,
                    SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
      this.varDecimalVector = v;
      this.mutator = v.getMutator();
    }

    @Override
    public boolean setSafe(int index, DrillBuf value, int start, int length) {
      if (index >= varDecimalVector.getValueCapacity()) {
        return false;
      }
      if (recordsRequireDecoding()) {
        ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
        currDecodedValToWrite = valReader.readBytes();
        ByteBuffer buf = currDecodedValToWrite.toByteBuffer();
        mutator.setSafe(index, buf, buf.position(), currDecodedValToWrite.length());
      } else {
        mutator.setSafe(index, start, start + length, value);
      }
      return true;
    }

    @Override
    public int capacity() {
      return varDecimalVector.getBuffer().capacity();
    }

    /** {@inheritDoc} */
    @Override
    protected void setSafe(VarLenBulkInput<VarLenBulkEntry> bulkInput) {
      mutator.setSafe(bulkInput);
  }

    /** {@inheritDoc} */
    @Override
    protected VarLenColumnBulkInput<VarDecimalVector> newVLBulkInput(int recordsToRead) throws IOException {
      return new VarLenColumnBulkInput<VarDecimalVector>(this, recordsToRead, bulkReaderState);
    }
  }

  public static class NullableVarDecimalColumn extends NullableVarLengthValuesColumn<NullableVarDecimalVector> {

    protected NullableVarDecimalVector nullableVarDecimalVector;
    protected NullableVarDecimalVector.Mutator mutator;

    NullableVarDecimalColumn(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                            ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableVarDecimalVector v,
                            SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
      nullableVarDecimalVector = v;
      this.mutator = v.getMutator();
    }

    @Override
    public boolean setSafe(int index, DrillBuf value, int start, int length) {
      if (index >= nullableVarDecimalVector.getValueCapacity()) {
        return false;
      }
      if (recordsRequireDecoding()) {
        ByteBuffer buf = currDecodedValToWrite.toByteBuffer();
        mutator.setSafe(index, buf, buf.position(), currDecodedValToWrite.length());
      } else {
        mutator.setSafe(index, 1, start, start + length, value);
      }
      return true;
    }

    @Override
    public int capacity() {
      return nullableVarDecimalVector.getBuffer().capacity();
    }

    /** {@inheritDoc} */
    @Override
    protected void setSafe(VarLenBulkInput<VarLenBulkEntry> bulkInput) {
      mutator.setSafe(bulkInput);
  }

    /** {@inheritDoc} */
    @Override
    protected VarLenColumnBulkInput<NullableVarDecimalVector> newVLBulkInput(int recordsToRead) throws IOException {
      return new VarLenColumnBulkInput<NullableVarDecimalVector>(this, recordsToRead, bulkReaderState);
    }
  }

  public final static class VarCharColumn extends VarLengthValuesColumn<VarCharVector> {

    // store a hard reference to the vector (which is also stored in the superclass) to prevent repetitive casting
    private final VarCharVector.Mutator mutator;
    private final VarCharVector varCharVector;

    VarCharColumn(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                  ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, VarCharVector v,
                  SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
      this.varCharVector = v;
      this.mutator       = v.getMutator();
    }

    @Override
    public boolean setSafe(int index, DrillBuf bytebuf, int start, int length) {
      if (index >= varCharVector.getValueCapacity()) {
        return false;
      }

      if (recordsRequireDecoding()) {
        ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
        currDecodedValToWrite = valReader.readBytes();
        ByteBuffer buf = currDecodedValToWrite.toByteBuffer();
        mutator.setSafe(index, buf, buf.position(), currDecodedValToWrite.length());
      } else {
        mutator.setSafe(index, start, start + length, bytebuf);
      }
      return true;
    }

    @Override
    public int capacity() {
      return varCharVector.getBuffer().capacity();
    }

    /** {@inheritDoc} */
    @Override
    protected void setSafe(VarLenBulkInput<VarLenBulkEntry> bulkInput) {
      mutator.setSafe(bulkInput);
  }

    /** {@inheritDoc} */
    @Override
    protected VarLenColumnBulkInput<VarCharVector> newVLBulkInput(int recordsToRead) throws IOException {
      return new VarLenColumnBulkInput<VarCharVector>(this, recordsToRead, bulkReaderState);
    }
  }

  public final static class NullableVarCharColumn extends NullableVarLengthValuesColumn<NullableVarCharVector> {

    // store a hard reference to the vector (which is also stored in the superclass) to prevent repetitive casting
    protected final NullableVarCharVector.Mutator mutator;
    private final NullableVarCharVector vector;

    NullableVarCharColumn(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                          ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableVarCharVector v,
                          SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
      this.vector  = v;
      this.mutator = vector.getMutator();
    }

    @Override
    public boolean setSafe(int index, DrillBuf value, int start, int length) {
      if (index >= vector.getValueCapacity()) {
        return false;
      }

      if (recordsRequireDecoding()) {
        ByteBuffer buf = currDecodedValToWrite.toByteBuffer();
        mutator.setSafe(index, buf, buf.position(), currDecodedValToWrite.length());
      } else {
        mutator.setSafe(index, 1, start, start + length, value);
      }
      return true;
    }

    @Override
    public int capacity() {
      return vector.getBuffer().capacity();
    }

    /** {@inheritDoc} */
    @Override
    protected void setSafe(VarLenBulkInput<VarLenBulkEntry> bulkInput) {
      mutator.setSafe(bulkInput);
  }

    /** {@inheritDoc} */
    @Override
    protected VarLenColumnBulkInput<NullableVarCharVector> newVLBulkInput(int recordsToRead) throws IOException {
      return new VarLenColumnBulkInput<NullableVarCharVector>(this, recordsToRead, bulkReaderState);
    }
  }

  public final static class VarBinaryColumn extends VarLengthValuesColumn<VarBinaryVector> {

    // store a hard reference to the vector (which is also stored in the superclass) to prevent repetitive casting
    private final VarBinaryVector varBinaryVector;
    private final VarBinaryVector.Mutator mutator;

    VarBinaryColumn(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                    ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, VarBinaryVector v,
                    SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);

      this.varBinaryVector = v;
      this.mutator         = v.getMutator();
    }

    @Override
    public final boolean setSafe(int index, DrillBuf value, int start, int length) {
      if (index >= varBinaryVector.getValueCapacity()) {
        return false;
      }

      if (recordsRequireDecoding()) {
        ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
        currDecodedValToWrite = valReader.readBytes();
        ByteBuffer buf = currDecodedValToWrite.toByteBuffer();
        mutator.setSafe(index, buf, buf.position(), currDecodedValToWrite.length());
      } else {
        mutator.setSafe(index, start, start + length, value);
      }
      return true;
    }

    @Override
    public int capacity() {
      return varBinaryVector.getBuffer().capacity();
    }

    /** {@inheritDoc} */
    @Override
    protected void setSafe(VarLenBulkInput<VarLenBulkEntry> bulkInput) {
      mutator.setSafe(bulkInput);
  }

    /** {@inheritDoc} */
    @Override
    protected VarLenColumnBulkInput<VarBinaryVector> newVLBulkInput(int recordsToRead) throws IOException {
      return new VarLenColumnBulkInput<VarBinaryVector>(this, recordsToRead, bulkReaderState);
    }
  }

  public final static class NullableVarBinaryColumn extends NullableVarLengthValuesColumn<NullableVarBinaryVector> {

    // store a hard reference to the vector (which is also stored in the superclass) to prevent repetitive casting
    private final NullableVarBinaryVector nullableVarBinaryVector;
    private final NullableVarBinaryVector.Mutator mutator;

    NullableVarBinaryColumn(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                            ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableVarBinaryVector v,
                            SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
      this.nullableVarBinaryVector = v;
      this.mutator                 = v.getMutator();
    }

    @Override
    public boolean setSafe(int index, DrillBuf value, int start, int length) {
      if (index >= nullableVarBinaryVector.getValueCapacity()) {
        return false;
      }

      if (recordsRequireDecoding()) {
        ByteBuffer buf = currDecodedValToWrite.toByteBuffer();
        mutator.setSafe(index, buf, buf.position(), currDecodedValToWrite.length());
      } else {
        mutator.setSafe(index, 1, start, start + length, value);
      }
      return true;
    }

    @Override
    public int capacity() {
      return nullableVarBinaryVector.getBuffer().capacity();
    }

    /** {@inheritDoc} */
    @Override
    protected void setSafe(VarLenBulkInput<VarLenBulkEntry> bulkInput) {
      mutator.setSafe(bulkInput);
    }

    /** {@inheritDoc} */
    @Override
    protected VarLenColumnBulkInput<NullableVarBinaryVector> newVLBulkInput(int recordsToRead) throws IOException {
      return new VarLenColumnBulkInput<NullableVarBinaryVector>(this, recordsToRead, bulkReaderState);
    }
  }

}
