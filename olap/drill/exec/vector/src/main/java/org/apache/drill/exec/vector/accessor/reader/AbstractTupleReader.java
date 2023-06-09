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

import java.util.ArrayList;
import java.util.List;

import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;
import org.apache.drill.exec.vector.accessor.DictReader;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.accessor.VariantReader;

/**
 * Reader for a tuple (a row or a map.) Provides access to each
 * column using either a name or a numeric index.
 */

public abstract class AbstractTupleReader implements TupleReader, ReaderEvents {

  public static class TupleObjectReader extends AbstractObjectReader {

    protected final AbstractTupleReader tupleReader;

    public TupleObjectReader(AbstractTupleReader tupleReader) {
      this.tupleReader = tupleReader;
    }

    @Override
    public TupleReader tuple() {
      return tupleReader;
    }

    @Override
    public Object getObject() {
      return tupleReader.getObject();
    }

    @Override
    public String getAsString() {
      return tupleReader.getAsString();
    }

    @Override
    public ReaderEvents events() { return tupleReader; }

    @Override
    public ColumnReader reader() { return tupleReader; }
  }

  protected final AbstractObjectReader[] readers;
  protected NullStateReader nullStateReader;

  protected AbstractTupleReader(AbstractObjectReader[] readers) {
    this.readers = readers;
  }

  @Override
  public ObjectType type() { return ObjectType.TUPLE; }

  @Override
  public void bindIndex(ColumnReaderIndex index) {
    for (AbstractObjectReader reader : readers) {
      reader.events().bindIndex(index);
    }
    nullStateReader.bindIndex(index);
  }

  @Override
  public void bindNullState(NullStateReader nullStateReader) {
    this.nullStateReader = nullStateReader;
  }

  @Override
  public void bindBuffer() {
    for (AbstractObjectReader reader : readers) {
      reader.events().bindBuffer();
    }
    nullStateReader.bindBuffer();
  }

  @Override
  public NullStateReader nullStateReader() { return nullStateReader; }

  @Override
  public boolean isNull() { return nullStateReader.isNull(); }

  @Override
  public int columnCount() { return tupleSchema().size(); }

  @Override
  public ObjectReader column(int colIndex) {
    return readers[colIndex];
  }

  @Override
  public ObjectReader column(String colName) {
    int index = tupleSchema().index(colName);
    if (index == -1) {
      return null;
    }
    return readers[index];
  }

  @Override
  public ObjectType type(int colIndex) {
    return column(colIndex).type();
  }

  @Override
  public ObjectType type(String colName) {
    return column(colName).type();
  }

  @Override
  public ScalarReader scalar(int colIndex) {
    return column(colIndex).scalar();
  }

  @Override
  public ScalarReader scalar(String colName) {
    return column(colName).scalar();
  }

  @Override
  public TupleReader tuple(int colIndex) {
    return column(colIndex).tuple();
  }

  @Override
  public TupleReader tuple(String colName) {
    return column(colName).tuple();
  }

  @Override
  public ArrayReader array(int colIndex) {
    return column(colIndex).array();
  }

  @Override
  public ArrayReader array(String colName) {
    return column(colName).array();
  }

  @Override
  public VariantReader variant(int colIndex) {
    return column(colIndex).variant();
  }

  @Override
  public VariantReader variant(String colName) {
    return column(colName).variant();
  }

  @Override
  public DictReader dict(int colIndex) {
    return column(colIndex).dict();
  }

  @Override
  public DictReader dict(String colName) {
    return column(colName).dict();
  }

  @Override
  public void reposition() {
    for (AbstractObjectReader reader : readers) {
      reader.events().reposition();
    }
  }

  @Override
  public Object getObject() {
    List<Object> elements = new ArrayList<>();
    for (AbstractObjectReader reader : readers) {
      elements.add(reader.getObject());
    }
    return elements;
  }

  @Override
  public String getAsString() {
    StringBuilder buf = new StringBuilder();
    buf.append("{");
    for (int i = 0; i < columnCount(); i++) {
      if (i > 0) {
        buf.append( ", " );
      }
      buf.append(readers[i].getAsString());
    }
    buf.append("}");
    return buf.toString();
  }
}
