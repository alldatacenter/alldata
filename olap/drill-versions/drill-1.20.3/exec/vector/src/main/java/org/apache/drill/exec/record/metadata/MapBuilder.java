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

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;

/**
 * Internal structure for building a map. A map is just a schema,
 * but one that is part of a parent column.
 * <p/>
 * Class can be created with and without parent container.
 * In the first case, column is added to the parent container during creation
 * and all <tt>resumeXXX</tt> methods return qualified parent container.
 * In the second case column is created without parent container as standalone entity.
 * All <tt>resumeXXX</tt> methods do not produce any action and return null.
 * To access built column {@link #buildColumn()} should be used.
 */
public class MapBuilder implements SchemaContainer {
  private final SchemaContainer parent;
  private final TupleBuilder tupleBuilder = new TupleBuilder();
  private final String memberName;
  private final DataMode mode;

  public MapBuilder(String memberName, DataMode mode) {
    this(null, memberName, mode);
  }

  public MapBuilder(SchemaContainer parent, String memberName, DataMode mode) {
    this.parent = parent;
    this.memberName = memberName;
    this.mode = mode;
  }

  @Override
  public void addColumn(ColumnMetadata column) {
    tupleBuilder.addColumn(column);
  }

  public MapBuilder add(String name, MajorType type) {
    return add(MaterializedField.create(name, type));
  }

  public MapBuilder add(MaterializedField col) {
    tupleBuilder.add(col);
    return this;
  }

  public MapBuilder add(String name, MinorType type, DataMode mode) {
    tupleBuilder.add(name, type, mode);
    return this;
  }

  public MapBuilder add(String name, MinorType type) {
    tupleBuilder.add(name, type);
    return this;
  }

  public MapBuilder add(String name, MinorType type, int width) {
    tupleBuilder.add(name, type, width);
    return this;
  }

  public MapBuilder add(String name, MinorType type, int precision, int scale) {
    return addDecimal(name, type, DataMode.REQUIRED, precision, scale);
  }

  public MapBuilder addNullable(String name, MinorType type) {
    tupleBuilder.addNullable(name,  type);
    return this;
  }

  public MapBuilder addNullable(String name, MinorType type, int width) {
    tupleBuilder.addNullable(name, type, width);
    return this;
  }

  public MapBuilder addNullable(String name, MinorType type, int precision, int scale) {
    return addDecimal(name, type, DataMode.OPTIONAL, precision, scale);
  }

  public MapBuilder addArray(String name, MinorType type) {
    tupleBuilder.addArray(name, type);
    return this;
  }

  public MapBuilder addArray(String name, MinorType type, int dims) {
    tupleBuilder.addArray(name,  type, dims);
    return this;
  }

  public MapBuilder addArray(String name, MinorType type, int precision, int scale) {
    return addDecimal(name, type, DataMode.REPEATED, precision, scale);
  }

  public MapBuilder addDynamic(String name) {
    tupleBuilder.addColumn(MetadataUtils.newDynamic(name));
    return this;
  }

  public MapBuilder addDecimal(String name, MinorType type,
      DataMode mode, int precision, int scale) {
    tupleBuilder.addDecimal(name, type, mode, precision, scale);
    return this;
  }

  /**
   * Add a map column. The returned schema builder is for the nested
   * map. Building that map, using {@link MapBuilder#resumeSchema()},
   * will return the original schema builder.
   *
   * @param name the name of the map column
   * @return a builder for the map
   */
  public MapBuilder addMap(String name) {
    return tupleBuilder.addMap(this, name);
  }

  public MapBuilder addMapArray(String name) {
    return tupleBuilder.addMapArray(this, name);
  }

  public UnionBuilder addUnion(String name) {
    return tupleBuilder.addUnion(this, name);
  }

  public UnionBuilder addList(String name) {
    return tupleBuilder.addList(this, name);
  }

  public RepeatedListBuilder addRepeatedList(String name) {
    return tupleBuilder.addRepeatedList(this, name);
  }

  public DictBuilder addDict(String name, MinorType keyType) {
    return tupleBuilder.addDict(this, name).key(keyType);
  }

  public DictBuilder addDict(String name, MajorType keyType) {
    return tupleBuilder.addDict(this, name).key(keyType);
  }

  public DictBuilder addDictArray(String name, MinorType keyType) {
    return tupleBuilder.addDictArray(this, name).key(keyType);
  }

  public DictBuilder addDictArray(String name, MajorType keyType) {
    return tupleBuilder.addDictArray(this, name).key(keyType);
  }

  public MapColumnMetadata buildColumn() {
    return new MapColumnMetadata(memberName, mode, tupleBuilder.schema());
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
    // TODO: Use the map schema directly rather than
    // rebuilding it as is done here.

    build();
    return (UnionBuilder) parent;
  }

  public DictBuilder resumeDict() {
    build();
    return (DictBuilder) parent;
  }
}
