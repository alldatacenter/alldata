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
package org.apache.drill.exec.vector.accessor.reader;

import java.util.List;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;

/**
 * Reader for a Dict entry. The entry is a special kind of tuple.
 */
public class DictEntryReader extends AbstractTupleReader {

  private final ColumnMetadata schema;
  private final VectorAccessor accessor;

  protected DictEntryReader(ColumnMetadata schema, VectorAccessor accessor,
                            AbstractObjectReader[] readers) {
    super(readers);
    assert readers.length == 2;
    this.schema = schema;
    this.accessor = accessor;
  }

  public static TupleObjectReader build(ColumnMetadata schema,
                                        VectorAccessor accessor,
                                        AbstractObjectReader[] readers) {
    DictEntryReader entryReader = new DictEntryReader(schema, accessor, readers);
    entryReader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);
    return new TupleObjectReader(entryReader);
  }

  public static TupleObjectReader build(ColumnMetadata schema,
                                           VectorAccessor accessor,
                                           List<AbstractObjectReader> readers) {
    AbstractObjectReader[] readerArray = new AbstractObjectReader[readers.size()];
    return build(schema, accessor, readers.toArray(readerArray));
  }

  public AbstractObjectReader keyReader() {
    return (AbstractObjectReader) column(0);
  }

  public AbstractObjectReader valueReader() {
    return (AbstractObjectReader) column(1);
  }

  @Override
  public void bindIndex(ColumnReaderIndex index) {
    if (accessor != null) {
      accessor.bind(index);
    }
    super.bindIndex(index);
  }

  @Override
  public ColumnMetadata schema() {
    return schema;
  }

  @Override
  public TupleMetadata tupleSchema() {
    return schema.tupleSchema();
  }
}
