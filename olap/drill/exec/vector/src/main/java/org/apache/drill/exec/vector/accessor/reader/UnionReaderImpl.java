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

import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.VariantMetadata;
import org.apache.drill.exec.vector.accessor.ArrayReader;
import org.apache.drill.exec.vector.accessor.ColumnAccessors.UInt1ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnReader;
import org.apache.drill.exec.vector.accessor.ColumnReaderIndex;
import org.apache.drill.exec.vector.accessor.ObjectReader;
import org.apache.drill.exec.vector.accessor.ObjectType;
import org.apache.drill.exec.vector.accessor.ScalarReader;
import org.apache.drill.exec.vector.accessor.TupleReader;
import org.apache.drill.exec.vector.accessor.VariantReader;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessors.SingleVectorAccessor;
import org.apache.drill.exec.vector.accessor.reader.VectorAccessors.UnionTypeHyperVectorAccessor;
import org.apache.drill.exec.vector.complex.UnionVector;

/**
 * Reader for a union vector. The union vector presents itself as a
 * variant. This is an important, if subtle distinction. The current union
 * vector format is highly inefficient (and buggy and not well supported
 * in Drill's operators.) If the union concept is needed, then it should
 * be redesigned, perhaps as a variable-width vector in which each entry
 * consists of a type/value pair. (For variable-width values such as
 * strings, the implementation would be a triple of (type, length,
 * value). The API here is designed to abstract away the implementation
 * and should work equally well for the current "union" implementation and
 * the possible "variant" implementation. As a result, when changing the
 * API, avoid introducing methods that assume an implementation.
 */

public class UnionReaderImpl implements VariantReader, ReaderEvents {

  public static class UnionObjectReader extends AbstractObjectReader {

    private final UnionReaderImpl reader;

    public UnionObjectReader(UnionReaderImpl reader) {
      this.reader = reader;
    }

    @Override
    public VariantReader variant() { return reader; }

    @Override
    public Object getObject() {
      return reader.getObject();
    }

    @Override
    public String getAsString() {
      return reader.getAsString();
    }

    @Override
    public ReaderEvents events() { return reader; }

    @Override
    public ColumnReader reader() { return reader; }
  }

  private final ColumnMetadata schema;
  private final VectorAccessor unionAccessor;
  private final VectorAccessor typeAccessor;
  private final UInt1ColumnReader typeReader;
  private final AbstractObjectReader[] variants;
  protected NullStateReader nullStateReader;

  public UnionReaderImpl(ColumnMetadata schema, VectorAccessor va, AbstractObjectReader[] variants) {
    this.schema = schema;
    this.unionAccessor = va;
    typeReader = new UInt1ColumnReader();
    typeReader.bindNullState(NullStateReaders.REQUIRED_STATE_READER);
    if (va.isHyper()) {
      typeAccessor = new UnionTypeHyperVectorAccessor(va);
    } else {
      UnionVector unionVector = va.vector();
      typeAccessor = new SingleVectorAccessor(unionVector.getTypeVector());
    }
    typeReader.bindVector(null, typeAccessor);
    nullStateReader = new NullStateReaders.TypeVectorStateReader(typeReader);
    assert variants != null  &&  variants.length == MinorType.values().length;
    this.variants = variants;
    rebindMemberNullState();
  }

  /**
   * Rebind the null state reader to include the union's own state.
   */

  private void rebindMemberNullState() {
    for (int i = 0; i < variants.length; i++) {
      AbstractObjectReader objReader = variants[i];
      if (objReader == null) {
        continue;
      }
      NullStateReader nullReader;
      MinorType type = MinorType.values()[i];
      switch(type) {
      case DICT:
      case MAP:
      case LIST:
        nullReader = new NullStateReaders.ComplexMemberStateReader(typeReader, type);
        break;
      default:
        nullReader =
            new NullStateReaders.MemberNullStateReader(nullStateReader,
                objReader.events().nullStateReader());
      }
      objReader.events().bindNullState(nullReader);
    }
  }

  public static AbstractObjectReader build(ColumnMetadata schema, VectorAccessor va, AbstractObjectReader[] variants) {
    return new UnionObjectReader(
        new UnionReaderImpl(schema, va, variants));
  }

  @Override
  public void bindNullState(NullStateReader nullStateReader) { }

  @Override
  public NullStateReader nullStateReader() { return nullStateReader; }

  @Override
  public void bindIndex(ColumnReaderIndex index) {
    unionAccessor.bind(index);
    typeAccessor.bind(index);
    typeReader.bindIndex(index);
    nullStateReader.bindIndex(index);
    for (AbstractObjectReader variant : variants) {
      if (variant != null) {
        variant.events().bindIndex(index);
      }
    }
  }

  @Override
  public ObjectType type() { return ObjectType.VARIANT; }

  @Override
  public ColumnMetadata schema() { return schema; }

  @Override
  public VariantMetadata variantSchema() { return schema.variantSchema(); }

  @Override
  public int size() { return variantSchema().size(); }

  @Override
  public boolean hasType(MinorType type) {

    // Probe the reader because we can't tell if the union has a type
    // without probing for the underlying storage vector.
    // Might be able to probe the MajorType.

    return variants[type.ordinal()] != null;
  }

  @Override
  public void reposition() {
    for (AbstractObjectReader variantReader : variants) {
      if (variantReader != null) {
        variantReader.events().reposition();
      }
    }
  }

  @Override
  public void bindBuffer() {
    for (AbstractObjectReader variantReader : variants) {
      if (variantReader != null) {
        variantReader.events().bindBuffer();
      }
    }
    nullStateReader.bindBuffer();
  }

  @Override
  public boolean isNull() {
    return nullStateReader.isNull();
  }

  @Override
  public MinorType dataType() {
    int typeCode = typeReader.getInt();
    if (typeCode == UnionVector.NULL_MARKER) {
      return null;
    }
    return MinorType.forNumber(typeCode);
  }

  @Override
  public ObjectReader member(MinorType type) {
    return variants[type.ordinal()];
  }

  private ObjectReader requireReader(MinorType type) {
    ObjectReader reader = member(type);
    if (reader == null) {
      throw new IllegalArgumentException("Union does not include type " + type.toString());
    }
    return reader;
  }

  @Override
  public ScalarReader scalar(MinorType type) {
    return requireReader(type).scalar();
  }

  @Override
  public ObjectReader member() {
    MinorType type = dataType();
    if (type == null) {
      return null;
    }
    return member(type);
  }

  @Override
  public ScalarReader scalar() {
    ObjectReader reader = member();
    if (reader == null) {
      return null;
    }
    return reader.scalar();
  }

  @Override
  public TupleReader tuple() {
    return requireReader(MinorType.MAP).tuple();
  }

  @Override
  public ArrayReader array() {
    return requireReader(MinorType.LIST).array();
  }

  @Override
  public Object getObject() {
    MinorType type = dataType();
    if (type == null) {
      return null;
    }
    return requireReader(type).getObject();
  }

  @Override
  public String getAsString() {
    MinorType type = dataType();
    if (type == null) {
      return "null";
    }
    return requireReader(type).getAsString();
  }
}
