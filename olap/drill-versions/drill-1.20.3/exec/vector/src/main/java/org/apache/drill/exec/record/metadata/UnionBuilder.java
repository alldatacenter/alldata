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
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.common.types.Types;

/**
 * Builds unions or (non-repeated) lists (which implicitly contain
 * unions.)
 * <p/>
 * Class can be created with and without parent container.
 * In the first case, column is added to the parent container during creation
 * and all <tt>resumeXXX</tt> methods return qualified parent container.
 * In the second case column is created without parent container as standalone entity.
 * All <tt>resumeXXX</tt> methods do not produce any action and return null.
 * To access built column {@link #buildColumn()} should be used.
 */
public class UnionBuilder implements SchemaContainer {

  private final SchemaContainer parent;
  private final String name;
  private final MinorType type;
  private final VariantSchema union;

  public UnionBuilder(String name, MinorType type) {
    this(null, name, type);
  }

  public UnionBuilder(SchemaContainer parent, String name, MinorType type) {
    this.parent = parent;
    this.name = name;
    this.type = type;
    this.union = new VariantSchema();
  }

  private void checkType(MinorType type) {
    if (union.hasType(type)) {
      throw new IllegalArgumentException("Duplicate type: " + type);
    }
  }

  @Override
  public void addColumn(ColumnMetadata column) {
    assert column.name().equals(Types.typeKey(column.type()));
    union.addType(column);
  }

  public UnionBuilder addType(MinorType type) {
    checkType(type);
    union.addType(type);
    return this;
  }

  public MapBuilder addMap() {
    checkType(MinorType.MAP);
    return new MapBuilder(this, Types.typeKey(MinorType.MAP), DataMode.OPTIONAL);
  }

  public UnionBuilder addList() {
    checkType(MinorType.LIST);
    return new UnionBuilder(this, Types.typeKey(MinorType.LIST), MinorType.LIST);
  }

  public RepeatedListBuilder addRepeatedList() {
    checkType(MinorType.LIST);
    return new RepeatedListBuilder(this, Types.typeKey(MinorType.LIST));
  }

  public DictBuilder addDict() {
    checkType(MinorType.DICT);
    return new DictBuilder(this, Types.typeKey(MinorType.DICT), DataMode.OPTIONAL);
  }

  public VariantColumnMetadata buildColumn() {
    return VariantColumnMetadata.variantOf(name, type, union);
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

  public UnionBuilder resumeUnion() {
    build();
    return (UnionBuilder) parent;
  }

  public RepeatedListBuilder resumeList() {
    build();
    return (RepeatedListBuilder) parent;
  }

  public DictBuilder resumeDict() {
    build();
    return (DictBuilder) parent;
  }

  public RepeatedListBuilder resumeRepeatedList() {
    build();
    return (RepeatedListBuilder) parent;
  }
}
