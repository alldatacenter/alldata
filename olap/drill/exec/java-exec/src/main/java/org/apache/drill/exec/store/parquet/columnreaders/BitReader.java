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

import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.vector.BitVector;
import org.apache.parquet.column.ColumnDescriptor;
import org.apache.parquet.format.SchemaElement;
import org.apache.parquet.hadoop.metadata.ColumnChunkMetaData;

final class BitReader extends ColumnReader<BitVector> {

  BitReader(ParquetRecordReader parentReader, ColumnDescriptor descriptor, ColumnChunkMetaData columnChunkMetaData,
            boolean fixedLength, BitVector v, SchemaElement schemaElement) throws ExecutionSetupException {
    super(parentReader, descriptor, columnChunkMetaData, fixedLength, v, schemaElement);
  }

  @Override
  protected void readField(long recordsToReadInThisPass) {

    recordsReadInThisIteration = Math.min(pageReader.pageValueCount
        - pageReader.valuesRead, recordsToReadInThisPass - valuesReadInCurrentPass);

    // A more optimized reader for bit columns was removed to fix the bug
    // DRILL-2031. It attempted to copy large runs of values directly from the
    // decompressed parquet stream into a BitVector. This was complicated by
    // parquet not always breaking a page on a row number divisible by 8. In
    // this case the batch would have to be cut off early or we would have to
    // copy the next page byte-by-byte with a bit shift to move the values into
    // the correct position (to make the value vector one contiguous buffer of
    // data). As page boundaries do not line up across columns, cutting off a
    // batch at every page boundary of a bit column could be costly with many
    // such pages, so we opted to try to shift the bits when necessary.
    //
    // In the end, this was too much complexity for not enough performance
    // benefit, for now this reader has been moved to use the higher level value
    // by value reader provided by the parquet library.
    for (int i = 0; i < recordsReadInThisIteration; i++){
      valueVec.getMutator().setSafe(i + valuesReadInCurrentPass,
            pageReader.getValueReader().readBoolean() ? 1 : 0 );
    }
  }
}
