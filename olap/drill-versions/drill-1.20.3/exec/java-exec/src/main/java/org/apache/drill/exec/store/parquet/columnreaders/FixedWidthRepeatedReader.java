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

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.vector.BaseDataValueVector;
import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;

public class FixedWidthRepeatedReader extends VarLengthColumn<RepeatedValueVector> {

  ColumnReader<?> dataReader;
  int dataTypeLengthInBytes;
  // we can do a vector copy of the data once we figure out how much we need to copy
  // this tracks the number of values to transfer (the dataReader will translate this to a number
  // of bytes to transfer and re-use the code from the non-repeated types)
  int valuesToRead;
  int repeatedGroupsReadInCurrentPass;
  int repeatedValuesInCurrentList;
  // empty lists are notated by definition levels, to stop reading at the correct time, we must keep
  // track of the number of empty lists as well as the length of all of the defined lists together
  int definitionLevelsRead;
  // parquet currently does not restrict lists reaching across pages for repeated values, this necessitates
  // tracking when this happens to stop some of the state updates until we know the full length of the repeated
  // value for the current record
  boolean notFishedReadingList;
  byte[] leftOverBytes;

  FixedWidthRepeatedReader(ParquetRecordReader parentReader, ColumnReader<?> dataReader, int dataTypeLengthInBytes,
                           ColumnDescriptor descriptor, ColumnChunkMetaData columnChunkMetaData, boolean fixedLength,
                           RepeatedValueVector valueVector, SchemaElement schemaElement) throws ExecutionSetupException {
    super(parentReader, descriptor, columnChunkMetaData, fixedLength, valueVector, schemaElement);
    this.dataTypeLengthInBytes = dataTypeLengthInBytes;
    this.dataReader = dataReader;
    this.dataReader.pageReader.clear();
    this.dataReader.pageReader = this.pageReader;
    // this is not in the reset method because it needs to be initialized only for the very first page read
    // in all other cases if a read ends at a page boundary we will need to keep track of this flag and not
    // clear it at the start of the next read loop
    notFishedReadingList = false;
  }

  @Override
  public void reset() {
    bytesReadInCurrentPass = 0;
    valuesReadInCurrentPass = 0;
    pageReader.valuesReadyToRead = 0;
    dataReader.vectorData = BaseDataValueVector.class.cast(valueVec.getDataVector()).getBuffer();
    dataReader.valuesReadInCurrentPass = 0;
    repeatedGroupsReadInCurrentPass = 0;
  }

  @Override
  public int getRecordsReadInCurrentPass() {
    return repeatedGroupsReadInCurrentPass;
  }

  @Override
  protected void readField(long recordsToRead) {
    //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public boolean skipReadyToReadPositionUpdate() {
    return false;
  }

  @Override
  public void updateReadyToReadPosition() {
    valuesToRead += repeatedValuesInCurrentList;
    pageReader.valuesReadyToRead += repeatedValuesInCurrentList;
    repeatedGroupsReadInCurrentPass++;
    currDictVal = null;
    if ( ! notFishedReadingList) {
      repeatedValuesInCurrentList = -1;
    }
  }

  @Override
  public void updatePosition() {
    pageReader.readPosInBytes += dataTypeLengthInBits;
    bytesReadInCurrentPass += dataTypeLengthInBits;
    valuesReadInCurrentPass++;
  }

  @Override
  public void hitRowGroupEnd() {
    pageReader.valuesReadyToRead = 0;
    definitionLevelsRead = 0;
  }

  @Override
  public void postPageRead() {
    super.postPageRead();
    // this is no longer correct as we figured out that lists can reach across pages
    if ( ! notFishedReadingList) {
      repeatedValuesInCurrentList = -1;
    }
    definitionLevelsRead = 0;
  }

  @Override
  protected int totalValuesReadAndReadyToReadInPage() {
    // we need to prevent the page reader from getting rid of the current page in the case where we have a repeated
    // value split across a page boundary
    if (notFishedReadingList) {
      return definitionLevelsRead - repeatedValuesInCurrentList;
    }
    return definitionLevelsRead;
  }

  @Override
  protected boolean checkVectorCapacityReached() {
    boolean doneReading = super.checkVectorCapacityReached();
    if (doneReading) {
      return true;
    }
    if (valuesReadInCurrentPass + pageReader.valuesReadyToRead + repeatedValuesInCurrentList >= valueVec.getValueCapacity()) {
      return true;
    } else {
      return false;
    }
  }

  @Override
  protected boolean readAndStoreValueSizeInformation() {
    int numLeftoverVals = 0;
    if (notFishedReadingList) {
      numLeftoverVals = repeatedValuesInCurrentList;
      readRecords(numLeftoverVals);
      notFishedReadingList = false;
      pageReader.valuesReadyToRead = 0;
      try {
        boolean stopReading = readPage();
        if (stopReading) {
          // hit the end of a row group
          return false;
        }
      } catch (IOException e) {
        throw new RuntimeException("Unexpected error reading parquet repeated column.", e);
      }
    }
    if ( currDefLevel == -1 ) {
      currDefLevel = pageReader.definitionLevels.nextInt();
      definitionLevelsRead++;
    }
    int repLevel;
    if ( columnDescriptor.getMaxDefinitionLevel() == currDefLevel) {
      if (repeatedValuesInCurrentList == -1 || notFishedReadingList) {
        repeatedValuesInCurrentList = 1;
        do {
          repLevel = pageReader.repetitionLevels.nextInt();
          if (repLevel > 0) {
            repeatedValuesInCurrentList++;
            currDefLevel = pageReader.definitionLevels.nextInt();
            definitionLevelsRead++;

            // we hit the end of this page, without confirmation that we reached the end of the current record
            if (definitionLevelsRead == pageReader.pageValueCount) {
              // check that we have not hit the end of the row group (in which case we will not find the repetition level indicating
              // the end of this record as there is no next page to check, we have read all the values in this repetition so it is okay
              // to add it to the read )
              if (totalValuesRead + pageReader.valuesReadyToRead + repeatedValuesInCurrentList != columnChunkMetaData.getValueCount()) {
                notFishedReadingList = true;
                // if we hit this case, we cut off the current batch at the previous value, these extra values as well
                // as those that spill into the next page will be added to the next batch
                return true;
              }
            }
          }
        } while (repLevel != 0);
      }
    } else {
      repeatedValuesInCurrentList = 0;
    }
    // this should not fail
    final UInt4Vector offsets = valueVec.getOffsetVector();
    offsets.getMutator().setSafe(repeatedGroupsReadInCurrentPass + 1, offsets.getAccessor().get(repeatedGroupsReadInCurrentPass));
    // This field is being referenced in the superclass determineSize method, so we need to set it here
    // again going to make this the length in BYTES to avoid repetitive multiplication/division
    dataTypeLengthInBits = repeatedValuesInCurrentList * dataTypeLengthInBytes;
    return false;
  }

  @Override
  protected void readRecords(int valuesToRead) {
    if (valuesToRead == 0) {
      return;
    }
    // TODO - validate that this works in all cases, it fixes a bug when reading from multiple pages into
    // a single vector
    dataReader.valuesReadInCurrentPass = 0;
    dataReader.readValues(valuesToRead);
    valuesReadInCurrentPass += valuesToRead;
    valueVec.getMutator().setValueCount(repeatedGroupsReadInCurrentPass);
    valueVec.getDataVector().getMutator().setValueCount(valuesReadInCurrentPass);
  }

  @Override
  public int capacity() {
    return BaseDataValueVector.class.cast(valueVec.getDataVector()).getBuffer().capacity();
  }

  @Override
  public void clear() {
    super.clear();
    dataReader.clear();
  }

}
