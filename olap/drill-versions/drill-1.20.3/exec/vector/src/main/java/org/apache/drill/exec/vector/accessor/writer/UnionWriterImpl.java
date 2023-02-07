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
package org.apache.drill.exec.vector.accessor.writer;

import java.math.BigDecimal;

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnWriter;
import org.apache.drill.exec.vector.accessor.ColumnWriterIndex;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.VariantReader;
import org.apache.drill.exec.vector.accessor.VariantWriter;
import org.apache.drill.exec.vector.accessor.WriterPosition;
import org.apache.drill.exec.vector.accessor.impl.HierarchicalFormatter;
import org.apache.drill.exec.vector.complex.UnionVector;
import org.joda.time.Period;

/**
 * Writer to a union vector.
 */

public class UnionWriterImpl implements VariantWriter, WriterEvents {

  public interface UnionShim extends WriterEvents {
    void bindWriter(UnionWriterImpl writer);
    void setNull();
    boolean hasType(MinorType type);

    /**
     * Return an existing writer for the given type, or create a new one
     * if needed.
     *
     * @param type desired variant type
     * @return a writer for that type
     */

    ObjectWriter member(MinorType type);
    void setType(MinorType type);
    @Override
    int lastWriteIndex();
    @Override
    int rowStartIndex();
    AbstractObjectWriter addMember(ColumnMetadata colSchema);
    AbstractObjectWriter addMember(MinorType type);
    void addMember(AbstractObjectWriter colWriter);
  }

  public static class VariantObjectWriter extends AbstractObjectWriter {

    private final UnionWriterImpl writer;

    public VariantObjectWriter(UnionWriterImpl writer) {
      this.writer = writer;
    }

    @Override
    public ColumnWriter writer() { return writer; }

    @Override
    public VariantWriter variant() { return writer; }

    @Override
    public WriterEvents events() { return writer; }

    @Override
    public void dump(HierarchicalFormatter format) {
      writer.dump(format);
    }
  }

  /**
   * The result set loader requires information about the child positions
   * of the array that this list represents. Since the children are mutable,
   * we cannot simply ask for the child writer as with most arrays. Instead,
   * we use a proxy that will route the request to the current shim. This
   * way the proxy persists even as the shims change. Just another nasty
   * side-effect of the overly-complex list structure...
   * <p>
   * This class is needed because both the child and this array writer
   * need to implement the same methods, so we can't just implement these
   * methods on the union writer itself.
   */

  private class ElementPositions implements WriterPosition {

    @Override
    public int rowStartIndex() { return shim.rowStartIndex(); }

    @Override
    public int lastWriteIndex() { return shim.lastWriteIndex(); }

    @Override
    public int writeIndex() {
      return index.vectorIndex();
    }
  }

  private final ColumnMetadata schema;
  private UnionShim shim;
  private ColumnWriterIndex index;
  private State state = State.IDLE;
  private VariantWriterListener listener;
  private final WriterPosition elementPosition = new ElementPositions();

  public UnionWriterImpl(ColumnMetadata schema) {
    this.schema = schema;
  }

  public UnionWriterImpl(ColumnMetadata schema, UnionVector vector,
      AbstractObjectWriter variants[]) {
    this(schema);
    bindShim(new UnionVectorShim(vector, variants));
  }

  @Override
  public void bindIndex(ColumnWriterIndex index) {
    this.index = index;
    shim.bindIndex(index);
  }

  public void bindListener(VariantWriterListener listener) {
    this.listener = listener;
  }

  // Unions are complex: listeners should bind to the components as they
  // are created.

  @Override
  public void bindListener(ColumnWriterListener listener) { }

  // The following are for coordinating with the shim.

  public State state() { return state; }
  public ColumnWriterIndex index() { return index; }
  public VariantWriterListener listener() { return listener; }
  public UnionShim shim() { return shim; }
  public WriterPosition elementPosition() { return elementPosition; }

  public void bindShim(UnionShim shim) {
    this.shim = shim;
    shim.bindWriter(this);
    if (state != State.IDLE) {
      shim.startWrite();
      if (state == State.IN_ROW) {
        shim.startRow();
      }
    }
  }

  @Override
  public ObjectType type() { return ObjectType.VARIANT; }

  @Override
  public boolean nullable() { return true; }

  @Override
  public ColumnMetadata schema() { return schema; }

  @Override
  public VariantMetadata variantSchema() { return schema.variantSchema(); }

  @Override
  public int size() { return variantSchema().size(); }

  @Override
  public boolean hasType(MinorType type) {
    return shim.hasType(type);
  }

  @Override
  public void setNull() {
    shim.setNull();
  }

  @Override
  public ObjectWriter memberWriter(MinorType type) {
    return shim.member(type);
  }

  @Override
  public ObjectWriter member(MinorType type) {

    // Get the writer first, which may trigger the single-to-union
    // conversion. Then set the type because, if the conversion is
    // done, the type vector exists only after creating the member.

    final ObjectWriter writer = shim.member(type);
    setType(type);
    return writer;
  }

  @Override
  public void setType(MinorType type) {
    shim.setType(type);
  }

  @Override
  public ObjectWriter addMember(ColumnMetadata colSchema) {
    return shim.addMember(colSchema);
  }

  @Override
  public ObjectWriter addMember(MinorType type) {
    return shim.addMember(type);
  }

  /**
   * Add a column writer to an existing union writer. Used for implementations
   * that support "live" schema evolution: column discovery while writing.
   * The corresponding metadata must already have been added to the schema.
   * Called by the shim's <tt>addMember</tt> to do writer-level tasks.
   *
   * @param writer the column writer to add
   */

  protected void addMember(AbstractObjectWriter writer) {
    final MinorType type = writer.schema().type();

    // If the metadata has not yet been added to the variant
    // schema, do so now. (Unfortunately, the default listener
    // does add the schema, while the row set loader does not.)

    if (!variantSchema().hasType(type)) {
      variantSchema().addType(writer.schema());
    }
    writer.events().bindIndex(index);
    if (state != State.IDLE) {
      writer.events().startWrite();
      if (state == State.IN_ROW) {
        writer.events().startRow();
      }
    }
  }

  @Override
  public ScalarWriter scalar(MinorType type) {
    return member(type).scalar();
  }

  @Override
  public TupleWriter tuple() {
    return member(MinorType.MAP).tuple();
  }

  @Override
  public ArrayWriter array() {
    return member(MinorType.LIST).array();
  }

  @Override
  public boolean isProjected() { return true; }

  @Override
  public void startWrite() {
    assert state == State.IDLE;
    state = State.IN_WRITE;
    shim.startWrite();
  }

  @Override
  public void startRow() {
    assert state == State.IN_WRITE;
    state = State.IN_ROW;
    shim.startRow();
  }

  @Override
  public void endArrayValue() {
    shim.endArrayValue();
  }

  @Override
  public void restartRow() {
    assert state == State.IN_ROW;
    shim.restartRow();
  }

  @Override
  public void saveRow() {
    assert state == State.IN_ROW;
    shim.saveRow();
    state = State.IN_WRITE;
  }

  @Override
  public void preRollover() {
    assert state == State.IN_ROW;
    shim.preRollover();
  }

  @Override
  public void postRollover() {
    assert state == State.IN_ROW;
    shim.postRollover();
  }

  @Override
  public void endWrite() {
    assert state != State.IDLE;
    shim.endWrite();
    state = State.IDLE;
  }

  @Override
  public int lastWriteIndex() { return shim.lastWriteIndex(); }

  @Override
  public int rowStartIndex() { return shim.rowStartIndex(); }

  @Override
  public int writeIndex() {
    return index.vectorIndex();
  }

  @Override
  public void copy(ColumnReader from) {
    if (!from.isNull()) {
      VariantReader source = (VariantReader) from;
      member(source.dataType()).copy(source.member());
    }
  }

  @Override
  public void setObject(Object value) {
    if (value == null) {
      setNull();
    } else if (value instanceof Boolean) {
      scalar(MinorType.BIT).setBoolean((Boolean) value);
    } else if (value instanceof Integer) {
      scalar(MinorType.INT).setInt((Integer) value);
    } else if (value instanceof Long) {
      scalar(MinorType.BIGINT).setLong((Long) value);
    } else if (value instanceof String) {
      scalar(MinorType.VARCHAR).setString((String) value);
    } else if (value instanceof BigDecimal) {
      // Can look for exactly one decimal type as is done for Object[] below
      throw new IllegalArgumentException("Decimal is ambiguous, please use scalar(type)");
    } else if (value instanceof Period) {
      // Can look for exactly one period type as is done for Object[] below
      throw new IllegalArgumentException("Period is ambiguous, please use scalar(type)");
    } else if (value instanceof byte[]) {
      final byte[] bytes = (byte[]) value;
      scalar(MinorType.VARBINARY).setBytes(bytes, bytes.length);
    } else if (value instanceof Byte) {
      scalar(MinorType.TINYINT).setInt((Byte) value);
    } else if (value instanceof Short) {
      scalar(MinorType.SMALLINT).setInt((Short) value);
    } else if (value instanceof Double) {
      scalar(MinorType.FLOAT8).setDouble((Double) value);
    } else if (value instanceof Float) {
      scalar(MinorType.FLOAT4).setDouble((Float) value);
    } else if (value instanceof Object[]) {
      if (hasType(MinorType.MAP) && hasType(MinorType.LIST)) {
        throw new UnsupportedOperationException("Union has both a map and a list, so Object[] is ambiguous");
      } else if (hasType(MinorType.MAP)) {
        tuple().setObject(value);
      } else if (hasType(MinorType.LIST)) {
        array().setObject(value);
      } else {
        throw new IllegalArgumentException("Unsupported type " +
            value.getClass().getSimpleName());
      }
    } else {
      throw new IllegalArgumentException("Unsupported type " +
                value.getClass().getSimpleName());
    }
  }

  @Override
  public void dump(HierarchicalFormatter format) {
    // TODO Auto-generated method stub
  }
}
