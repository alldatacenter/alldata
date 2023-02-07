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
package org.apache.drill.exec.physical.rowSet;

import java.util.List;

import org.apache.drill.exec.physical.resultSet.model.ReaderIndex;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.reader.AbstractObjectReader;
import org.apache.drill.exec.vector.accessor.reader.AbstractTupleReader;
import org.apache.drill.exec.vector.accessor.reader.NullStateReaders;

/**
 * Reader implementation for a row set.
 */

public class RowSetReaderImpl extends AbstractTupleReader implements RowSetReader {

  private final TupleMetadata schema;
  protected final ReaderIndex readerIndex;

  public RowSetReaderImpl(TupleMetadata schema, ReaderIndex index, AbstractObjectReader[] readers) {
    super(readers);
    this.schema = schema;
    this.readerIndex = index;
    bindNullState(NullStateReaders.REQUIRED_STATE_READER);
    bindIndex(index);
  }

  public RowSetReaderImpl(TupleMetadata schema, ReaderIndex index,
      List<AbstractObjectReader> readers) {
    this(schema, index,
        readers.toArray(new AbstractObjectReader[0]));
  }

  @Override
  public boolean next() {
    if (!readerIndex.next()) {
      return false;
    }
    reposition();
    return true;
  }

  @Override
  public boolean hasNext() {
    return readerIndex.hasNext();
  }

  @Override
  public int logicalIndex() { return readerIndex.logicalIndex(); }

  @Override
  public int rowCount() { return readerIndex.size(); }

  @Override
  public int offset() { return readerIndex.offset(); }

  @Override
  public int hyperVectorIndex() { return readerIndex.hyperVectorIndex(); }

  @Override
  public void setPosition(int index) {
    readerIndex.set(index);
    reposition();
  }

  @Override
  public ColumnMetadata schema() { return null; }

  @Override
  public TupleMetadata tupleSchema() { return schema; }

  @Override
  public void rewind() { setPosition(-1); }

  @Override
  public void newBatch() {
    bindBuffer();
    rewind();
  }
}
