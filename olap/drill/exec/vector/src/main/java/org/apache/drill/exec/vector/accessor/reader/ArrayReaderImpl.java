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

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.accessor.VariantReader;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

/**
 * Reader for an array-valued column. This reader provides access to specific
 * array members via an array index. This class implements all arrays. The
 * behavior for specific array types (scalar, map, lists, etc.) is provided
 * through composition.
 */

public class ArrayReaderImpl implements ArrayReader, ReaderEvents {

  /**
   * Object representation of an array reader.
   */

  public static class ArrayObjectReader extends AbstractObjectReader {

    private final ArrayReaderImpl arrayReader;

    public ArrayObjectReader(ArrayReaderImpl arrayReader) {
       this.arrayReader = arrayReader;
    }

    @Override
    public ArrayReader array() {
      return arrayReader;
    }

    @Override
    public Object getObject() {
      return arrayReader.getObject();
    }

    @Override
    public String getAsString() {
      return arrayReader.getAsString();
    }

    @Override
    public ReaderEvents events() { return arrayReader; }

    @Override
    public ColumnReader reader() { return arrayReader; }
  }

  /**
   * Index into the vector of elements for a repeated vector.
   * Indexes elements relative to the array. That is, if an array
   * has five elements, the index here tracks elements 0..4.
   * The actual vector index is given as the start offset plus the
   * offset into the array.
   * <p>
   * Indexes allow random or sequential access. Random access is more
   * typical for scalar arrays, while sequential access can be more convenient
   * for tuple arrays.
   */

  public static class ElementReaderIndex implements ColumnReaderIndex {
    protected final ColumnReaderIndex base;
    protected int startOffset;
    protected int length;
    protected int position;

    public ElementReaderIndex(ColumnReaderIndex base) {
      this.base = base;
    }

    @Override
    public int hyperVectorIndex() { return 0; }

    /**
     * Reposition this array index for a new array given the array start
     * offset and length.
     *
     * @param startOffset first location within the array's
     * offset vector
     * @param length number of offset vector locations associated with this
     * array
     */

    public void reset(int startOffset, int length) {
      assert length >= 0;
      assert startOffset >= 0;
      this.startOffset = startOffset;
      this.length = length;
      position = -1;
    }

    public void rewind() {
      position = -1;
    }

    @Override
    public int size() { return length; }

    /**
     * Given a 0-based index relative to the current array, return an absolute offset
     * vector location for the array value.
     *
     * @return absolute offset vector location for the array value
     */

    @Override
    public int offset() {
      Preconditions.checkElementIndex(position, length);
      return startOffset + position;
    }

    @Override
    public boolean next() {
      if (++position < length) {
        return true;
      }
      position = length;
      return false;
    }

    @Override
    public boolean hasNext() {
      return position + 1 < length;
    }

    /**
     * Set the current iterator location to the given index offset.
     *
     * @param index 0-based index into the current array
     */

    public void set(int index) {
      Preconditions.checkPositionIndex(index, length);
      position = index;
    }

    @Override
    public int logicalIndex() { return position; }
  }

  protected final AbstractObjectReader elementReader;
  protected ElementReaderIndex elementIndex;
  protected NullStateReader nullStateReader;
  private final ColumnMetadata schema;
  private final VectorAccessor arrayAccessor;
  private final OffsetVectorReader offsetReader;

  public ArrayReaderImpl(ColumnMetadata schema, VectorAccessor va,
      AbstractObjectReader elementReader) {
    this.schema = schema;
    arrayAccessor = va;
    this.elementReader = elementReader;
    offsetReader = new OffsetVectorReader(
        VectorAccessors.arrayOffsetVectorAccessor(va));
  }

  /**
   * Build a scalar array for a Repeated type. Such arrays are not nullable.
   *
   * @param arrayAccessor vector accessor for the repeated vector that holds
   * the scalar values
   * @param elementReader scalar reader used to decode each scalar value
   * @return object reader which wraps the scalar array reader
   */

  public static ArrayObjectReader buildScalar(ColumnMetadata schema,
      VectorAccessor arrayAccessor,
      BaseScalarReader elementReader) {

    // Reader is bound to the values vector inside the nullable vector.

    elementReader.bindVector(schema,
        VectorAccessors.arrayDataAccessor(arrayAccessor));

    // The scalar array element can't be null.

    elementReader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);

    // Create the array, giving it an offset vector reader based on the
    // repeated vector's offset vector.

    ArrayReaderImpl arrayReader = new ArrayReaderImpl(schema, arrayAccessor,
        new AbstractScalarReader.ScalarObjectReader(elementReader));

    // The array itself can't be null.

    arrayReader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);

    // Wrap it all in an object reader.

    return new ArrayObjectReader(arrayReader);
  }

  /**
   * Build a repeated map reader.
   *
   * @param arrayAccessor vector accessor for the repeated map vector
   * @param elementReader tuple reader that provides access to each
   * tuple in the array
   * @return object reader that wraps the map array reader
   */

  public static AbstractObjectReader buildTuple(ColumnMetadata schema,
      VectorAccessor arrayAccessor,
      AbstractObjectReader elementReader) {

    // Create the array reader over the map vector.

    ArrayReaderImpl arrayReader = new ArrayReaderImpl(schema, arrayAccessor, elementReader);

    // The array itself can't be null.

    arrayReader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);

    // Wrap it all in an object reader.

    return new ArrayObjectReader(arrayReader);
  }

  /**
   * Build a list reader. Lists entries can be null. The reader can be a simple
   * scalar, a map, a union, or another list.
   *
   * @param listAccessor
   * @param elementReader
   * @return
   */

  public static AbstractObjectReader buildList(ColumnMetadata schema,
      VectorAccessor listAccessor,
      AbstractObjectReader elementReader) {

    // Create the array over whatever it is that the list holds.

    ArrayReaderImpl arrayReader = new ArrayReaderImpl(schema, listAccessor, elementReader);

    // The list carries a "bits" vector to allow list entries to be null.

    NullStateReader arrayNullState = new NullStateReaders.ListIsSetVectorStateReader(
        VectorAccessors.listBitsAccessor(listAccessor));
    arrayReader.bindNullState(arrayNullState);

    // The nullability of each list entry depends on both the list's null
    // value, and the null state of the entry. But, if the list entry itself is
    // null, then we can't iterate over the elements. So, no need to doctor up
    // the entry's null state.

    // Wrap it all in an object reader.

    return new ArrayObjectReader(arrayReader);
  }

  /**
   * Build a 2+D array reader. The backing vector must be a repeated
   * list. The element reader must itself be a repeated list, or a
   * repeated of some other type.
   *
   * @param schema
   * @param listAccessor
   * @param elementReader
   * @return
   */

  public static AbstractObjectReader buildRepeatedList(ColumnMetadata schema,
      VectorAccessor listAccessor,
      AbstractObjectReader elementReader) {

    assert schema.isArray();
    assert listAccessor.type().getMinorType() == MinorType.LIST;
    assert listAccessor.type().getMode() == DataMode.REPEATED;
    assert elementReader.type() == ObjectType.ARRAY;

    // Create the array over whatever it is that the list holds.

    ArrayReaderImpl arrayReader = new ArrayReaderImpl(schema, listAccessor, elementReader);

    // The repeated list has no bits vector, all values are non-null.

    arrayReader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);

    // Wrap it all in an object reader.

    return new ArrayObjectReader(arrayReader);
  }

  @Override
  public void bindIndex(ColumnReaderIndex index) {
    arrayAccessor.bind(index);
    offsetReader.bindIndex(index);
    nullStateReader.bindIndex(index);
    elementIndex = new ElementReaderIndex(index);
    elementReader.events().bindIndex(elementIndex);
  }

  @Override
  public void bindNullState(NullStateReader nullStateReader) {
    this.nullStateReader = nullStateReader;
  }

  @Override
  public ObjectType type() { return ObjectType.ARRAY; }

  @Override
  public ColumnMetadata schema() { return schema; }

  @Override
  public NullStateReader nullStateReader() { return nullStateReader; }

  @Override
  public boolean isNull() { return nullStateReader.isNull(); }

  @Override
  public void reposition() {
    long entry = offsetReader.getEntry();
    elementIndex.reset((int) (entry >> 32), (int) (entry & 0xFFFF_FFFF));
  }

  @Override
  public boolean next() {
    if (! elementIndex.next()) {
      return false;
    }
    elementReader.events().reposition();
    return true;
  }

  public ColumnReaderIndex elementIndex() { return elementIndex; }

  @Override
  public int size() { return elementIndex.size(); }

  @Override
  public void setPosn(int posn) {
    elementIndex.set(posn);
    elementReader.events().reposition();
  }

  @Override
  public void rewind() {
    elementIndex.rewind();
  }

  @Override
  public void bindBuffer() {
    elementReader.events().bindBuffer();
    nullStateReader.bindBuffer();
  }

  @Override
  public ObjectReader entry() { return elementReader; }

  @Override
  public ObjectType entryType() { return elementReader.type(); }

  @Override
  public ScalarReader scalar() {
    return entry().scalar();
  }

  @Override
  public TupleReader tuple() {
    return entry().tuple();
  }

  @Override
  public ArrayReader array() {
    return entry().array();
  }

  @Override
  public VariantReader variant() {
    return entry().variant();
  }

  @Override
  public Object getObject() {

    // Simple: return elements as an object list.
    // If really needed, could return as a typed array, but that
    // is a bit of a hassle.

    rewind();
    List<Object> elements = new ArrayList<>();
    while (next()) {
      elements.add(elementReader.getObject());
    }
    return elements;
  }

  @Override
  public String getAsString() {
    if (isNull()) {
      return "null";
    }
    rewind();
    StringBuilder buf = new StringBuilder();
    buf.append("[");
    int i = 0;
    while (next()) {
      if (i++ > 0) {
        buf.append( ", " );
      }
      buf.append(elementReader.getAsString());
    }
    buf.append("]");
    return buf.toString();
  }
}
