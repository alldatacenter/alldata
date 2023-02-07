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
 * Internal tuple builder shared by the schema and map builders.
 * Those two classes can't inherit from this class because their
 * versions of the "add" methods return themselves to allow fluent
 * construction.
 */
public class TupleBuilder implements SchemaContainer {

  private final TupleSchema schema = new TupleSchema();

  @Override
  public void addColumn(ColumnMetadata column) {
    schema.add(column);
  }

  public void add(String name, MajorType type) {
    add(MaterializedField.create(name, type));
  }

  public void add(MaterializedField col) {
    schema.add(col);
  }

  public void add(String name, MinorType type, DataMode mode) {
    add(SchemaBuilder.columnSchema(name, type, mode));
  }

  public void add(String name, MinorType type) {
    add(name, type, DataMode.REQUIRED);
  }

  public void add(String name, MinorType type, int width) {
    MaterializedField field = new ColumnBuilder(name, type)
        .setMode(DataMode.REQUIRED)
        .setWidth(width)
        .build();
    add(field);
  }

  public void addNullable(String name, MinorType type) {
    add(name, type, DataMode.OPTIONAL);
  }

  public void addNullable(String name, MinorType type, int width) {
    MaterializedField field = new ColumnBuilder(name, type)
        .setMode(DataMode.OPTIONAL)
        .setWidth(width)
        .build();
    add(field);
  }

  public void addArray(String name, MinorType type) {
    add(name, type, DataMode.REPEATED);
  }

  public void addDecimal(String name, MinorType type, DataMode mode, int precision, int scale) {
    MaterializedField field = new ColumnBuilder(name, type)
        .setMode(mode)
        .setPrecisionAndScale(precision, scale)
        .build();
    add(field);
  }

  /**
   * Add a multi-dimensional array, implemented as a repeated vector
   * along with 0 or more repeated list vectors.
   *
   * @param name column name
   * @param type base data type
   * @param dims number of dimensions, 1 or more
   */
  public void addArray(String name, MinorType type, int dims) {
    assert dims >= 1;
    if (dims == 1) {
      addArray(name, type);
      return;
    }
    RepeatedListBuilder listBuilder = addRepeatedList(this, name);
    buildMultiDimArray(listBuilder, type, dims - 1);
    listBuilder.build();
  }

  private void buildMultiDimArray(RepeatedListBuilder listBuilder,
      MinorType type, int dims) {
    if (dims == 1) {
      listBuilder.addArray(type);
    } else {
      RepeatedListBuilder childBuilder = listBuilder.addDimension();
      buildMultiDimArray(childBuilder, type, dims - 1);
      childBuilder.build();
    }
  }

  /**
   * Add a map column. The returned schema builder is for the nested
   * map. Building that map, using {@link MapBuilder#resumeSchema()},
   * will return the original schema builder.
   *
   * @param parent schema container
   * @param name the name of the map column
   * @return a builder for the map
   */
  public MapBuilder addMap(SchemaContainer parent, String name) {
    return new MapBuilder(parent, name, DataMode.REQUIRED);
  }

  public MapBuilder addMapArray(SchemaContainer parent, String name) {
    return new MapBuilder(parent, name, DataMode.REPEATED);
  }

  public DictBuilder addDict(SchemaContainer parent, String name) {
    return new DictBuilder(parent, name, DataMode.REQUIRED);
  }

  public DictBuilder addDictArray(SchemaContainer parent, String name) {
    return new DictBuilder(parent, name, DataMode.REPEATED);
  }

  public UnionBuilder addUnion(SchemaContainer parent, String name) {
    return new UnionBuilder(parent, name, MinorType.UNION);
  }

  public UnionBuilder addList(SchemaContainer parent, String name) {
    return new UnionBuilder(parent, name, MinorType.LIST);
  }

  public RepeatedListBuilder addRepeatedList(SchemaContainer parent, String name) {
    return new RepeatedListBuilder(parent, name);
  }

  public TupleSchema schema() {
    return schema;
  }
}
