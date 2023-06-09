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
 * Reader for a Drill Map type. Maps are actually tuples, just like rows.
 */

public class MapReader extends AbstractTupleReader {

  protected final ColumnMetadata schema;

  /**
   * Accessor for the map vector. This class does not use the map vector
   * directory. However, in the case of a map hyper-vector, we need to
   * tell the vector which batch to use. (For an array, the array does
   * this work and the map accessor is null.)
   */

  private final VectorAccessor mapAccessor;

  protected MapReader(ColumnMetadata schema, AbstractObjectReader[] readers) {
    this(schema, null, readers);
  }

  protected MapReader(ColumnMetadata schema,
      VectorAccessor mapAccessor, AbstractObjectReader[] readers) {
    super(readers);
    this.schema = schema;
    this.mapAccessor = mapAccessor;
  }

  public static TupleObjectReader build(ColumnMetadata schema,
      VectorAccessor mapAccessor,
      AbstractObjectReader[] readers) {
    MapReader mapReader = new MapReader(schema, mapAccessor, readers);
    mapReader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);
    return new TupleObjectReader(mapReader);
  }

  public static AbstractObjectReader build(ColumnMetadata schema,
      VectorAccessor mapAccessor,
      List<AbstractObjectReader> readers) {
    AbstractObjectReader[] readerArray = new AbstractObjectReader[readers.size()];
    return build(schema, mapAccessor, readers.toArray(readerArray));
  }

  @Override
  public void bindIndex(ColumnReaderIndex index) {
    if (mapAccessor != null) {
      mapAccessor.bind(index);
    }
    super.bindIndex(index);
  }

  @Override
  public ColumnMetadata schema() { return schema; }

  @Override
  public TupleMetadata tupleSchema() { return schema.tupleSchema(); }
}
