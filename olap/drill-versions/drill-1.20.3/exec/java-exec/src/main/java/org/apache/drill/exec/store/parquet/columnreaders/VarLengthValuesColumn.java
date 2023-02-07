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

import java.io.IOException;
import java.util.Collections;

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.BulkReaderState;
import org.apache.drill.exec.store.parquet.columnreaders.VarLenColumnBulkInput.ColumnPrecisionType;
import org.apache.drill.exec.vector.VarLenBulkEntry;
import org.apache.drill.exec.vector.VarLenBulkInput;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VariableWidthVector;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.io.api.Binary;

import io.netty.buffer.DrillBuf;

@SuppressWarnings("unchecked")
public abstract class VarLengthValuesColumn<V extends ValueVector> extends VarLengthColumn {

  Binary currLengthDeterminingDictVal;
  Binary currDecodedValToWrite;
  VariableWidthVector variableWidthVector;

  /** Bulk read operation state that needs to be maintained across batch calls */
  protected final BulkReaderState bulkReaderState = new BulkReaderState();

  VarLengthValuesColumn(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                        ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, V v,
                        SchemaElement schemaElement) throws ExecutionSetupException {

    super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
    variableWidthVector = (VariableWidthVector) valueVec;

    if (!Collections.disjoint(columnChunkMetaData.getEncodings(), ColumnReader.DICTIONARY_ENCODINGS)) {
      usingDictionary = true;
      // We didn't implement the fixed length optimization when a Parquet Dictionary is used; as there are
      // no data point about this use-case. Will also enable bulk processing by default since early data
      // profiling (for detecting the best processing strategy to use) is disabled when the column precision
      // is already set.
      bulkReaderState.columnPrecInfo.columnPrecisionType = ColumnPrecisionType.DT_PRECISION_IS_VARIABLE;
      bulkReaderState.columnPrecInfo.bulkProcess         = true;
    }
    else {
      usingDictionary = false;
    }
  }

  /**
   * Store a variable length entry if there is enough memory.
   *
   * @param index entry's index
   * @param bytes byte array container
   * @param start start offset
   * @param length entry's length
   * @return true if the entry was successfully inserted; false otherwise
   */
  public abstract boolean setSafe(int index, DrillBuf bytes, int start, int length);

  /**
   * Store a set of variable entries in bulk; this method will automatically extend the underlying
   * value vector if needed.
   *
   * @param bulkInput set of variable length entries
   */
  protected abstract void setSafe(VarLenBulkInput<VarLenBulkEntry> bulkInput);

  /**
   * @return new variable bulk input object
   */
  protected abstract VarLenColumnBulkInput<V> newVLBulkInput(int recordsToRead) throws IOException;

  /** {@inheritDoc} */
  @Override
  protected final int readRecordsInBulk(int recordsToRead) throws IOException {
    final VarLenColumnBulkInput<V> bulkInput = newVLBulkInput(recordsToRead);

    // Process this batch
    setSafe(bulkInput);

    // Somehow the Batch Reader uses this variable to propagate the batch-size (picks the first column and
    // then reads the "valuesReadInCurrentPass" variable value).
    valuesReadInCurrentPass = bulkInput.getReadBatchFields();

    return valuesReadInCurrentPass;
  }

  @Override
  protected void readField(long recordToRead) {
    dataTypeLengthInBits = variableWidthVector.getAccessor().getValueLength(valuesReadInCurrentPass);
    // again, I am re-purposing the unused field here, it is a length n BYTES, not bits
    boolean success = setSafe((int) valuesReadInCurrentPass, pageReader.pageData,
        (int) pageReader.readPosInBytes + 4, dataTypeLengthInBits);
    assert success;
    updatePosition();
  }

  @Override
  public void updateReadyToReadPosition() {
    pageReader.readyToReadPosInBytes += dataTypeLengthInBits + 4;
    pageReader.valuesReadyToRead++;
    currLengthDeterminingDictVal = null;
  }

  @Override
  public void updatePosition() {
    pageReader.readPosInBytes += dataTypeLengthInBits + 4;
    bytesReadInCurrentPass += dataTypeLengthInBits;
    valuesReadInCurrentPass++;
  }

  @Override
  public boolean skipReadyToReadPositionUpdate() {
    return false;
  }

  @Override
  protected boolean readAndStoreValueSizeInformation() throws IOException {
    // re-purposing this field here for length in BYTES to prevent repetitive multiplication/division
    if (usingDictionary) {
      if (currLengthDeterminingDictVal == null) {
        currLengthDeterminingDictVal = pageReader.getDictionaryLengthDeterminingReader().readBytes();
      }
      currDecodedValToWrite = currLengthDeterminingDictVal;
      // re-purposing  this field here for length in BYTES to prevent repetitive multiplication/division
      dataTypeLengthInBits = currLengthDeterminingDictVal.length();
    } else {
      // re-purposing  this field here for length in BYTES to prevent repetitive multiplication/division
      dataTypeLengthInBits = pageReader.pageData.getInt((int) pageReader.readyToReadPosInBytes);
    }

    // this should not fail
    variableWidthVector.getMutator().setValueLengthSafe((int) valuesReadInCurrentPass + pageReader.valuesReadyToRead,
        dataTypeLengthInBits);
    return false;
  }

}
