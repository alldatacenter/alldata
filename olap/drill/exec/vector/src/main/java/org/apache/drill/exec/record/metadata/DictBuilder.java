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
package org.apache.drill.exec.record.metadata;

import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.common.types.Types;
import org.apache.drill.exec.vector.complex.DictVector;

/**
 * Internal structure for building a dict. Dict is an array of key-value pairs
 * with defined types for key and value (key and value fields are defined within {@link TupleSchema}).
 * Key can be {@link org.apache.drill.common.types.TypeProtos.DataMode#REQUIRED} primitive,
 * while value can be primitive or complex.
 * <p>Column is added to the parent container during creation
 * and all <tt>resumeXXX</tt> methods return qualified parent container.</p>
 *
 * @see DictVector
 */
public class DictBuilder implements SchemaContainer {

  /**
   * Schema containing key and value fields' definition.
   */
  private final TupleSchema schema = new TupleSchema();
  private final SchemaContainer parent;
  private final String name;
  private final TypeProtos.DataMode mode;

  public DictBuilder(SchemaContainer parent, String name, TypeProtos.DataMode mode) {
    this.parent = parent;
    this.name = name;
    this.mode = mode;
  }

  @Override
  public void addColumn(ColumnMetadata column) {
    // As dict does not support complex key, this method is used to
    // nest a complex value only
    if (!DictVector.FIELD_VALUE_NAME.equals(column.name())) {
      String message = String.format(
          "Expected column with name '%s'. Found: '%s'.", DictVector.FIELD_VALUE_NAME, column.name());
      throw new IllegalArgumentException(message);
    }

    if (isFieldSet(column.name())) {
      String message = String.format("Field '%s' is already defined in dict.", column.name());
      throw new IllegalArgumentException(message);
    }

    schema.addColumn(column);
  }

  public DictBuilder key(TypeProtos.MinorType type) {
    TypeProtos.MajorType keyType = Types.withMode(type, TypeProtos.DataMode.REQUIRED);
    return key(keyType);
  }

  /**
   * Use this method to set types with width or scale and precision,
   * e.g. {@link org.apache.drill.common.types.TypeProtos.MinorType#VARDECIMAL} with scale and precision or
   * {@link org.apache.drill.common.types.TypeProtos.MinorType#VARCHAR} etc.
   *
   * @param type desired type for key
   * @return {@code this} builder
   * @throws IllegalStateException if key field is already set
   * @throws IllegalArgumentException if {@code type} is not supported (either complex or nullable)
   */
  public DictBuilder key(TypeProtos.MajorType type) {
    final String fieldName = DictVector.FIELD_KEY_NAME;
    if (isFieldSet(fieldName)) {
      throw new IllegalStateException(String.format("Filed '%s' is already defined.", fieldName));
    }

    if (!isSupportedKeyType(type)) {
      throw new IllegalArgumentException(
          String.format("'%s' in dict should be non-nullable primitive. Found: %s", fieldName, type));
    }

    addField(fieldName, type);
    return this;
  }

  /**
   * Checks if the field identified by name was already set.
   *
   * @param name name of the field
   * @return {@code true} if the schema contains field with the {@code name}; {@code false} otherwise.
   */
  private boolean isFieldSet(String name) {
    return schema.index(name) != -1;
  }

  public DictBuilder value(TypeProtos.MinorType type) {
    return value(type, TypeProtos.DataMode.REQUIRED);
  }

  public DictBuilder nullableValue(TypeProtos.MinorType type) {
    return value(type, TypeProtos.DataMode.OPTIONAL);
  }

  public DictBuilder repeatedValue(TypeProtos.MinorType type) {
    return value(type, TypeProtos.DataMode.REPEATED);
  }

  private DictBuilder value(TypeProtos.MinorType type, TypeProtos.DataMode mode) {
    TypeProtos.MajorType valueType = Types.withMode(type, mode);
    return value(valueType);
  }

  /**
   * Define non-complex value type. For complex types use {@link #mapValue()}, {@link #mapArrayValue()} etc.
   *
   * @param type desired non-complex type for value.
   * @return {@code this} builder
   * @throws IllegalStateException if value is already set
   * @throws IllegalArgumentException if {@code type} is either {@code MAP},
   *                                  {@code LIST}, {@code DICT} or {@code UNION}.
   * @see #mapValue() method to define value as {@code MAP}
   * @see #mapArrayValue() method to define value as {@code REPEATED MAP}
   * @see #listValue() method to define value as {@code LIST}
   * @see #unionValue() method to define value as {@code UNION}
   * @see #dictValue() method to define value as {@code DICT}
   * @see #dictArrayValue() method to define value as {@code REPEATED DICT}
   */
  public DictBuilder value(TypeProtos.MajorType type) {
    final String fieldName = DictVector.FIELD_VALUE_NAME;
    if (isFieldSet(fieldName)) {
      throw new IllegalStateException(String.format("Field '%s' is already defined.", fieldName));
    }

    if (Types.isComplex(type) || Types.isUnion(type)) {
      String msg = String.format("Complex type found %s when defining '%s'. " +
          "Use mapValue(), listValue() etc. in case of complex value type.", fieldName, type);
      throw new IllegalArgumentException(msg);
    }

    addField(fieldName, type);
    return this;
  }

  /**
   * Adds field (either key or value) after validation to the schema.
   *
   * @param name name of the field
   * @param type type of the field
   */
  private void addField(String name, TypeProtos.MajorType type) {
    ColumnBuilder builder = new ColumnBuilder(name, type.getMinorType())
        .setMode(type.getMode());

    if (type.hasScale()) {
      builder.setPrecisionAndScale(type.getPrecision(), type.getScale());
    } else if (type.hasPrecision()) {
      builder.setPrecision(type.getPrecision());
    }
    if (type.hasWidth()) {
      builder.setWidth(type.getWidth());
    }

    if (Types.isRepeated(type)) {
      schema.add(SchemaBuilder.columnSchema(name, type.getMinorType(), type.getMode()));
      return;
    }

    schema.add(builder.build());
  }

  public MapBuilder mapValue() {
    return new MapBuilder(this, DictVector.FIELD_VALUE_NAME, TypeProtos.DataMode.REQUIRED);
  }

  public MapBuilder mapArrayValue() {
    return new MapBuilder(this, DictVector.FIELD_VALUE_NAME, TypeProtos.DataMode.REPEATED);
  }

  public DictBuilder dictValue() {
    return new DictBuilder(this, DictVector.FIELD_VALUE_NAME, TypeProtos.DataMode.REQUIRED);
  }

  public DictBuilder dictArrayValue() {
    return new DictBuilder(this, DictVector.FIELD_VALUE_NAME, TypeProtos.DataMode.REPEATED);
  }

  public UnionBuilder unionValue() {
    return new UnionBuilder(this, DictVector.FIELD_VALUE_NAME, TypeProtos.MinorType.UNION);
  }

  public UnionBuilder listValue() {
    return new UnionBuilder(this, DictVector.FIELD_VALUE_NAME, TypeProtos.MinorType.LIST);
  }

  public RepeatedListBuilder repeatedListValue() {
    return new RepeatedListBuilder(this, DictVector.FIELD_VALUE_NAME);
  }

  public DictColumnMetadata buildColumn() {
    validateKeyValuePresent();
    return new DictColumnMetadata(name, mode, schema);
  }

  private void validateKeyValuePresent() {
    for (String fieldName : DictVector.fieldNames) {
      ColumnMetadata columnMetadata = schema.metadata(fieldName);
      if (columnMetadata == null) {
        throw new IllegalStateException(String.format("Field %s is absent in DICT.", fieldName));
      }
    }
  }

  public void build() {
    if (parent != null) {
      parent.addColumn(buildColumn());
    }
  }

  public SchemaBuilder resumeSchema() {
    build();
    return (SchemaBuilder) parent;
  }

  public MapBuilder resumeMap() {
    build();
    return (MapBuilder) parent;
  }

  public RepeatedListBuilder resumeList() {
    build();
    return (RepeatedListBuilder) parent;
  }

  public UnionBuilder resumeUnion() {
    build();
    return (UnionBuilder) parent;
  }

  public DictBuilder resumeDict() {
    build();
    return (DictBuilder) parent;
  }

  private boolean isSupportedKeyType(TypeProtos.MajorType type) {
    return !Types.isComplex(type)
        && !Types.isUnion(type)
        && type.getMode() == TypeProtos.DataMode.REQUIRED;
  }
}
