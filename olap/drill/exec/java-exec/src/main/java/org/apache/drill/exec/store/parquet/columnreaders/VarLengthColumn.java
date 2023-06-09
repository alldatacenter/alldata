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
import org.apache.drill.exec.vector.ValueVector;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;
import org.apache.parquet.io.api.Binary;

public abstract class VarLengthColumn<V extends ValueVector> extends ColumnReader<V> {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(VarLengthColumn.class);

  Binary currDictVal;

  VarLengthColumn(ParquetRecordReader parentReader, ColumnDescriptor descriptor,
                  ColumnChunkMetaData columnChunkMetaData, boolean fixedLength, V v,
                  SchemaElement schemaElement) throws ExecutionSetupException {
    super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
      if (!Collections.disjoint(columnChunkMetaData.getEncodings(), ColumnReader.DICTIONARY_ENCODINGS)) {
        usingDictionary = true;
      }
      else {
        usingDictionary = false;
      }
  }

  @Override
  protected boolean processPageData(int recordsToReadInThisPass) throws IOException {
    return readAndStoreValueSizeInformation();
  }

  @Override
  public void reset() {
    super.reset();
    pageReader.valuesReadyToRead = 0;
  }

  protected abstract boolean readAndStoreValueSizeInformation() throws IOException;

  public abstract boolean skipReadyToReadPositionUpdate();

}
