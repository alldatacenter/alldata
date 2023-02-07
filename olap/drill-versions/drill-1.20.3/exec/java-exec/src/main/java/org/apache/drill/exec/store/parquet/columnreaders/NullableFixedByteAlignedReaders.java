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
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.expr.holders.NullableTimeStampHolder;
import org.apache.drill.exec.store.parquet.ParquetReaderUtility;
import org.apache.drill.exec.vector.NullableBigIntVector;
import org.apache.drill.exec.vector.NullableDateVector;
import org.apache.drill.exec.vector.NullableFloat4Vector;
import org.apache.drill.exec.vector.NullableFloat8Vector;
import org.apache.drill.exec.vector.NullableIntVector;
import org.apache.drill.exec.vector.NullableIntervalVector;
import org.apache.drill.exec.vector.NullableTimeStampVector;
import org.apache.drill.exec.vector.NullableTimeVector;
import org.apache.drill.exec.vector.NullableUInt4Vector;
import org.apache.drill.exec.vector.NullableUInt8Vector;
import org.apache.drill.exec.vector.NullableVarBinaryVector;
import org.apache.drill.exec.vector.NullableVarDecimalVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.shaded.guava.com.google.common.primitives.Ints;
import org.apache.drill.shaded.guava.com.google.common.primitives.Longs;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.column.values.ValuesReader;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.io.api.Binary;
import org.joda.time.DateTimeConstants;

import java.nio.ByteBuffer;

import static org.apache.drill.exec.store.parquet.ParquetReaderUtility.NanoTimeUtils.getDateTimeValueFromBinary;

public class NullableFixedByteAlignedReaders {

  static class NullableFixedByteAlignedReader<V extends ValueVector> extends NullableColumnReader<V> {
    protected DrillBuf bytebuf;

    NullableFixedByteAlignedReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                      ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, V v, SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      this.bytebuf = pageReader.pageData;

      // fill in data.
      vectorData.writeBytes(bytebuf, (int) readStartInBytes, (int) readLength);
    }
  }

  /**
   * Class for reading the fixed length byte array type in parquet. Currently Drill does not have
   * a fixed length binary type, so this is read into a varbinary with the same size recorded for
   * each value.
   */
  static class NullableFixedBinaryReader extends NullableFixedByteAlignedReader<NullableVarBinaryVector> {
    NullableFixedBinaryReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                   ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableVarBinaryVector v, SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    @Override
    protected void readField(long recordsToReadInThisPass) {
      this.bytebuf = pageReader.pageData;
      if (recordsRequireDecoding()) {
        ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
        NullableVarBinaryVector.Mutator mutator =  valueVec.getMutator();
        Binary currDictValToWrite;
        for (int i = 0; i < recordsToReadInThisPass; i++) {
          currDictValToWrite = valReader.readBytes();
          ByteBuffer buf = currDictValToWrite.toByteBuffer();
          mutator.setSafe(valuesReadInCurrentPass + i, buf, buf.position(), currDictValToWrite.length());
        }
      } else {
        super.readField(recordsToReadInThisPass);
        // TODO - replace this with fixed binary type in drill
        // for now we need to write the lengths of each value
        int byteLength = dataTypeLengthInBits / 8;
        for (int i = 0; i < recordsToReadInThisPass; i++) {
          valueVec.getMutator().setValueLengthSafe(valuesReadInCurrentPass + i, byteLength);
        }
      }
    }
  }

  /**
   * Class for reading parquet fixed binary type INT96, which is used for storing hive,
   * impala timestamp values with nanoseconds precision (12 bytes). It reads such values as a drill timestamp (8 bytes).
   */
  static class NullableFixedBinaryAsTimeStampReader extends NullableFixedByteAlignedReader<NullableTimeStampVector> {
    NullableFixedBinaryAsTimeStampReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                              ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableTimeStampVector v, SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);

      // The width of each element of the TimeStampVector is 8 bytes (64 bits) instead of 12 bytes.
      dataTypeLengthInBits = NullableTimeStampHolder.WIDTH * 8;
    }

    @Override
    protected void readField(long recordsToReadInThisPass) {
      this.bytebuf = pageReader.pageData;
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++) {
        Binary binaryTimeStampValue = valReader.readBytes();
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, getDateTimeValueFromBinary(binaryTimeStampValue, true));
      }
    }
  }

  static class NullableDictionaryIntReader extends NullableColumnReader<NullableIntVector> {

    NullableDictionaryIntReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableIntVector v,
                                SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++){
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readInteger());
      }
    }
  }

  static class NullableDictionaryUInt4Reader extends NullableColumnReader<NullableUInt4Vector> {

    NullableDictionaryUInt4Reader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                  ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableUInt4Vector v,
                                  SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++){
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readInteger());
      }
    }
  }

  static class NullableDictionaryTimeReader extends NullableColumnReader<NullableTimeVector> {

    NullableDictionaryTimeReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                     ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableTimeVector v,
                                     SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++){
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readInteger());
      }
    }
  }

  static class NullableDictionaryTimeMicrosReader extends NullableColumnReader<NullableTimeVector> {

    NullableDictionaryTimeMicrosReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
      ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableTimeVector v,
      SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++) {
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readInteger() / 1000);
      }
    }
  }

  static class NullableDictionaryBigIntReader extends NullableColumnReader<NullableBigIntVector> {

    NullableDictionaryBigIntReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                   ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableBigIntVector v,
                                   SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++){
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readLong());
      }
    }
  }

  static class NullableDictionaryUInt8Reader extends NullableColumnReader<NullableUInt8Vector> {

    NullableDictionaryUInt8Reader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                  ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableUInt8Vector v,
                                  SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++){
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readLong());
      }
    }
  }

  static class NullableDictionaryTimeStampReader extends NullableColumnReader<NullableTimeStampVector> {

    NullableDictionaryTimeStampReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                   ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableTimeStampVector v,
                                   SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++){
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readLong());
      }
    }
  }

  static class NullableDictionaryTimeStampMicrosReader extends NullableColumnReader<NullableTimeStampVector> {

    NullableDictionaryTimeStampMicrosReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
      ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableTimeStampVector v,
      SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++) {
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readLong() / 1000);
      }
    }
  }

  static class NullableDictionaryVarDecimalReader extends NullableColumnReader<NullableVarDecimalVector> {

    NullableDictionaryVarDecimalReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
        ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableVarDecimalVector v,
        SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      switch (columnDescriptor.getPrimitiveType().getPrimitiveTypeName()) {
        case INT32:
          if (usingDictionary) {
            for (int i = 0; i < recordsToReadInThisPass; i++) {
              byte[] bytes = Ints.toByteArray(pageReader.getDictionaryValueReader().readInteger());
              setValueBytes(i, bytes);
            }
          } else {
            for (int i = 0; i < recordsToReadInThisPass; i++) {
              byte[] bytes = Ints.toByteArray(pageReader.getValueReader().readInteger());
              setValueBytes(i, bytes);
            }
          }
          break;
        case INT64:
          if (usingDictionary) {
            for (int i = 0; i < recordsToReadInThisPass; i++) {
              byte[] bytes = Longs.toByteArray(pageReader.getDictionaryValueReader().readLong());
              setValueBytes(i, bytes);
            }
          } else {
            for (int i = 0; i < recordsToReadInThisPass; i++) {
              byte[] bytes = Longs.toByteArray(pageReader.getValueReader().readLong());
              setValueBytes(i, bytes);
            }
          }
          break;
        case FIXED_LEN_BYTE_ARRAY:
        case BINARY:
          if (usingDictionary) {
            recordsReadInThisIteration = Math.min(pageReader.pageValueCount
                - pageReader.valuesRead, recordsToReadInThisPass - valuesReadInCurrentPass);
            NullableVarDecimalVector.Mutator mutator = valueVec.getMutator();
            for (int i = 0; i < recordsReadInThisIteration; i++) {
              Binary currDictValToWrite = pageReader.getDictionaryValueReader().readBytes();
              mutator.setSafe(valuesReadInCurrentPass + i, currDictValToWrite.toByteBuffer().slice(), 0,
                  currDictValToWrite.length());
            }
            // Set the write Index. The next page that gets read might be a page that does not use dictionary encoding
            // and we will go into the else condition below. The readField method of the parent class requires the
            // writer index to be set correctly.
            int writerIndex = valueVec.getBuffer().writerIndex();
            valueVec.getBuffer().setIndex(0, writerIndex + (int) readLength);
          } else {
            for (int i = 0; i < recordsToReadInThisPass; i++) {
              Binary valueToWrite = pageReader.getValueReader().readBytes();
              valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valueToWrite.toByteBuffer().slice(), 0,
                  valueToWrite.length());
            }
          }
      }
    }

    private void setValueBytes(int i, byte[] bytes) {
      valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, bytes, 0, bytes.length);
    }
  }

  static class NullableDictionaryFloat4Reader extends NullableColumnReader<NullableFloat4Vector> {

    NullableDictionaryFloat4Reader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                   ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableFloat4Vector v,
                                   SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++){
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readFloat());
      }
    }
  }

  static class NullableDictionaryFloat8Reader extends NullableColumnReader<NullableFloat8Vector> {

    NullableDictionaryFloat8Reader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                                  ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, NullableFloat8Vector v,
                                  SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    // this method is called by its superclass during a read loop
    @Override
    protected void readField(long recordsToReadInThisPass) {
      ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
      for (int i = 0; i < recordsToReadInThisPass; i++){
        valueVec.getMutator().setSafe(valuesReadInCurrentPass + i, valReader.readDouble());
      }
    }
  }

  static abstract class NullableConvertedReader<V extends ValueVector> extends NullableFixedByteAlignedReader<V> {

    protected int dataTypeLengthInBytes;

    NullableConvertedReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                            ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, V v, SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    @Override
    protected void readField(long recordsToReadInThisPass) {

      this.bytebuf = pageReader.pageData;

      dataTypeLengthInBytes = (int) Math.ceil(dataTypeLengthInBits / 8.0);
      for (int i = 0; i < recordsToReadInThisPass; i++) {
        addNext((int) readStartInBytes + i * dataTypeLengthInBytes, i + valuesReadInCurrentPass);
      }
    }

    abstract void addNext(int start, int index);
  }

  public static class NullableDateReader extends NullableConvertedReader<NullableDateVector> {
    NullableDateReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor, ColumnChunkMetaData columnChunkMetaData,
                       boolean fixedLength, NullableDateVector v, SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    @Override
    void addNext(int start, int index) {
      int intValue;
      if (recordsRequireDecoding()) {
        ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
        intValue =  valReader.readInteger();
      } else {
        intValue = readIntLittleEndian(bytebuf, start);
      }

      valueVec.getMutator().set(index, intValue * (long) DateTimeConstants.MILLIS_PER_DAY);
    }

  }

  /**
   * Old versions of Drill were writing a non-standard format for date. See DRILL-4203
   */
  public static class NullableCorruptDateReader extends NullableConvertedReader<NullableDateVector> {

    NullableCorruptDateReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor, ColumnChunkMetaData columnChunkMetaData,
                       boolean fixedLength, NullableDateVector v, SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    @Override
    void addNext(int start, int index) {
      int intValue;
      if (recordsRequireDecoding()) {
        ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
        intValue =  valReader.readInteger();
      } else {
        intValue = readIntLittleEndian(bytebuf, start);
      }

      valueVec.getMutator().set(index, (intValue - ParquetReaderUtility.CORRECT_CORRUPT_DATE_SHIFT) * DateTimeConstants.MILLIS_PER_DAY);
    }

  }

  /**
   * Old versions of Drill were writing a non-standard format for date. See DRILL-4203
   *
   * For files that lack enough metadata to determine if the dates are corrupt, we must just
   * correct values when they look corrupt during this low level read.
   */
  public static class CorruptionDetectingNullableDateReader extends NullableConvertedReader<NullableDateVector> {

    CorruptionDetectingNullableDateReader(ParquetRecordReader parentReader,
                                          ColumnDescriptor descriptor, ColumnChunkMetaData columnChunkMetaData,
                                          boolean fixedLength, NullableDateVector v, SchemaElement schemaElement)
        throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    @Override
    void addNext(int start, int index) {
      int intValue;
      if (recordsRequireDecoding()) {
        ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
        intValue =  valReader.readInteger();
      } else {
        intValue = readIntLittleEndian(bytebuf, start);
      }

      if (intValue > ParquetReaderUtility.DATE_CORRUPTION_THRESHOLD) {
        valueVec.getMutator().set(index, (intValue - ParquetReaderUtility.CORRECT_CORRUPT_DATE_SHIFT) * DateTimeConstants.MILLIS_PER_DAY);
      } else {
        valueVec.getMutator().set(index, intValue * (long) DateTimeConstants.MILLIS_PER_DAY);
      }
    }
  }

  public static class NullableIntervalReader extends NullableConvertedReader<NullableIntervalVector> {
    NullableIntervalReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor, ColumnChunkMetaData columnChunkMetaData,
                   boolean fixedLength, NullableIntervalVector v, SchemaElement schemaElement) throws ExecutionSetupException {
      super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    }

    @Override
    void addNext(int start, int index) {
      if (recordsRequireDecoding()) {
        ValuesReader valReader = usingDictionary ? pageReader.getDictionaryValueReader() : pageReader.getValueReader();
        byte[] input = valReader.readBytes().getBytes();
        valueVec.getMutator().setSafe(index, 1,
            ParquetReaderUtility.getIntFromLEBytes(input, 0),
            ParquetReaderUtility.getIntFromLEBytes(input, 4),
            ParquetReaderUtility.getIntFromLEBytes(input, 8));
      } else {
        valueVec.getMutator().set(index, 1, bytebuf.getInt(start), bytebuf.getInt(start + 4), bytebuf.getInt(start + 8));
      }
    }
  }
}
