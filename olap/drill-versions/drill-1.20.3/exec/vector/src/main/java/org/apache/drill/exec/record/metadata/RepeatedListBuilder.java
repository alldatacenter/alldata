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
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;

/**
 * Builder for a repeated list. Drill's metadata represents a repeated
 * list as a chain of materialized fields and that is the pattern used
 * here. It would certainly be cleaner to have a single field, with the
 * number of dimensions as a property, but that is not how Drill evolved.
 * <p/>
 * An instance can be created with and without parent container.
 * In the first case, a column is added to the parent container during creation
 * and all {@code resumeXXX} methods return qualified parent container.
 * In the second case column is created without parent container as a stand-alone entity.
 * The {@code resumeXXX} methods do not produce any action and return null.
 * To access built column {@link #buildColumn()} should be used.
 */
public class RepeatedListBuilder implements SchemaContainer {

  private final SchemaContainer parent;
  private final String name;
  private ColumnMetadata child;

  public RepeatedListBuilder(String name) {
    this(null, name);
  }

  public RepeatedListBuilder(SchemaContainer parent, String name) {
    this.parent = parent;
    this.name = name;
  }

  public RepeatedListBuilder addDimension() {
    return new RepeatedListBuilder(this, name);
  }

  public MapBuilder addMapArray() {
    // Existing code uses the repeated list name as the name of
    // the vector within the list.
    return new MapBuilder(this, name, DataMode.REPEATED);
  }

  public DictBuilder addDictArray() {
    // Existing code uses the repeated list name as the name of
    // the vector within the list.
    return new DictBuilder(this, name, DataMode.REPEATED);
  }

  public RepeatedListBuilder addArray(MinorType type) {
    // Existing code uses the repeated list name as the name of
    // the vector within the list.
    if (type == MinorType.UNION) {
      addColumn(VariantColumnMetadata.list(name));
    } else {
      addColumn(MetadataUtils.newScalar(name, type, DataMode.REPEATED));
    }
    return this;
  }

  public RepeatedListBuilder addArray(MinorType type, int width) {
    // Existing code uses the repeated list name as the name of
    // the vector within the list.
    TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
        .setMinorType(type)
        .setMode(DataMode.REPEATED)
        .setPrecision(width)
        .build();

    addColumn(MetadataUtils.newScalar(name, majorType));
    return this;
  }

  public RepeatedListBuilder addArray(MinorType type, int precision, int scale) {
    // Existing code uses the repeated list name as the name of
    // the vector within the list.
    TypeProtos.MajorType majorType = TypeProtos.MajorType.newBuilder()
        .setMinorType(type)
        .setMode(DataMode.REPEATED)
        .setPrecision(precision)
        .setScale(scale)
        .build();

    addColumn(MetadataUtils.newScalar(name, majorType));
    return this;
  }

  public UnionBuilder addList() {
    return new UnionBuilder(this, name, MinorType.LIST);
  }

  public RepeatedListColumnMetadata buildColumn() {
    return MetadataUtils.newRepeatedList(name, child);
  }

  public void build() {
    if (parent != null) {
      parent.addColumn(buildColumn());
    }
  }

  public RepeatedListBuilder resumeList() {
    build();
    return (RepeatedListBuilder) parent;
  }

  public SchemaBuilder resumeSchema() {
    build();
    return (SchemaBuilder) parent;
  }

  public UnionBuilder resumeUnion() {
    build();
    return (UnionBuilder) parent;
  }

  public MapBuilder resumeMap() {
    build();
    return (MapBuilder) parent;
  }

  public DictBuilder resumeDict() {
    build();
    return (DictBuilder) parent;
  }

  @Override
  public void addColumn(ColumnMetadata column) {
    assert child == null;
    child = column;
  }
}
