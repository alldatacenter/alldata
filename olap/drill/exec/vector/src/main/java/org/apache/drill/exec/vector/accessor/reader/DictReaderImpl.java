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

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.KeyAccessor;
import org.apache.drill.exec.vector.accessor.KeyAccessors;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.ValueType;
import org.apache.drill.exec.vector.accessor.DictReader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DictReaderImpl extends ArrayReaderImpl implements DictReader, ReaderEvents {

  public static class DictObjectReader extends ArrayObjectReader {

    public DictObjectReader(DictReaderImpl dictReader) {
      super(dictReader);
    }

    @Override
    public DictReader dict() {
      return (DictReader) array();
    }
  }

  private final ScalarReader keyReader;
  private final AbstractObjectReader valueReader;
  private final KeyAccessor keyAccessor;

  public DictReaderImpl(ColumnMetadata metadata, VectorAccessor va, AbstractTupleReader.TupleObjectReader entryObjectReader) {
    super(metadata, va, entryObjectReader);
    DictEntryReader reader = (DictEntryReader) entryObjectReader.reader();
    this.keyReader = reader.keyReader().scalar();
    this.valueReader = reader.valueReader();
    keyAccessor = KeyAccessors.getAccessor(this, keyReader);
  }

  public static DictObjectReader build(ColumnMetadata schema, VectorAccessor dictAccessor,
                                       List<AbstractObjectReader> readers) {
    AbstractTupleReader.TupleObjectReader entryReader = DictEntryReader.build(schema, dictAccessor, readers);
    DictReaderImpl dictReader = new DictReaderImpl(schema, dictAccessor, entryReader);
    dictReader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);
    return new DictObjectReader(dictReader);
  }

  @Override
  public KeyAccessor keyAccessor() {
    return keyAccessor;
  }

  @Override
  public ObjectReader valueReader() {
    return valueReader;
  }

  @Override
  public ValueType keyColumnType() {
    return keyReader.valueType();
  }

  @Override
  public ObjectType valueColumnType() {
    return valueReader.type();
  }

  @Override
  public Map<Object, Object> getObject() {
    rewind();
    Map<Object, Object> map = new HashMap<>();
    while (next()) {
      map.put(keyReader.getObject(), valueReader.getObject());
    }
    return map;
  }

  @Override
  public String getAsString() {
    rewind();
    StringBuilder buf = new StringBuilder();
    buf.append("{");
    boolean comma = false;
    while (next()) {
      if (comma) {
        buf.append(", ");
      }
      buf.append(keyReader.getAsString())
          .append(':')
          .append(valueReader.getAsString());
      comma = true;
    }
    buf.append("}");
    return buf.toString();
  }
}
