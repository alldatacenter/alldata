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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.drill.common.types.Types;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

public class VariantSchema implements VariantMetadata {

  private final Map<MinorType, ColumnMetadata> types = new HashMap<>();
  private VariantColumnMetadata parent;
  private boolean isSimple;

  protected void bind(VariantColumnMetadata parent) {
    this.parent = parent;
  }

  public static ColumnMetadata memberMetadata(MinorType type) {
    String name = Types.typeKey(type);
    switch (type) {
    case LIST:
      return VariantColumnMetadata.list(name);
    case MAP:
      // Although maps do not have a bits vector, when used in a
      // union the map must be marked as optional since the union as a
      // whole can be null, implying that the map is null by implication.
      // (In fact, the readers have a special mechanism to work out the
      // null state in this case.

      return new MapColumnMetadata(name, DataMode.OPTIONAL, null);
    case UNION:
      throw new IllegalArgumentException("Cannot add a union to a union");
    default:
      return new PrimitiveColumnMetadata(
          MaterializedField.create(
              name,
              Types.optional(type)));
    }
  }

  @Override
  public ColumnMetadata addType(MinorType type) {
    checkType(type);
    ColumnMetadata dummyCol = memberMetadata(type);
    types.put(type, dummyCol);
    return dummyCol;
  }

  @Override
  public void addType(ColumnMetadata col) {
    checkType(col.type());
    Preconditions.checkArgument(col.name().equals(Types.typeKey(col.type())));
    switch (col.type()) {
    case UNION:
      throw new IllegalArgumentException("Cannot add a union to a union");
    case LIST:
      if (col.mode() == DataMode.REQUIRED) {
        throw new IllegalArgumentException("List type column must be OPTIONAL or REPEATED");
      }
      break;
    default:
      if (col.mode() != DataMode.OPTIONAL) {
        throw new IllegalArgumentException("Type column must be OPTIONAL");
      }
      break;
    }
    types.put(col.type(), col);
  }

  private void checkType(MinorType type) {
    if (types.containsKey(type)) {
      throw new IllegalArgumentException("Variant already contains type: " + type);
    }
  }

  @Override
  public int size() { return types.size(); }

  @Override
  public boolean hasType(MinorType type) {
    return types.containsKey(type);
  }

  @Override
  public ColumnMetadata member(MinorType type) {
    return types.get(type);
  }

  @Override
  public ColumnMetadata parent() { return parent; }

  @Override
  public Collection<MinorType> types() {
    return types.keySet();
  }

  @Override
  public Collection<ColumnMetadata> members() {
    return types.values();
  }

  public void addMap(MapColumnMetadata mapCol) {
    Preconditions.checkArgument(! mapCol.isArray());
    Preconditions.checkState(! isSimple);
    checkType(MinorType.MAP);
    types.put(MinorType.MAP, mapCol);
  }

  public void addList(VariantColumnMetadata listCol) {
    Preconditions.checkArgument(listCol.isArray());
    Preconditions.checkState(! isSimple);
    checkType(MinorType.LIST);
    types.put(MinorType.LIST, listCol);
  }

  public ColumnMetadata addType(MaterializedField field) {
    Preconditions.checkState(! isSimple);
    MinorType type = field.getType().getMinorType();
    checkType(type);
    ColumnMetadata col;
    switch (type) {
    case LIST:
      col = new VariantColumnMetadata(field);
      break;
    case MAP:
      col = new MapColumnMetadata(field);
      break;
    case UNION:
      throw new IllegalArgumentException("Cannot add a union to a union");
    default:
      col = new PrimitiveColumnMetadata(field);
      break;
    }
    types.put(type, col);
    return col;
  }

  @Override
  public boolean isSingleType() {
    return types.size() == 1;
  }

  @Override
  public ColumnMetadata listSubtype() {
    if (isSingleType()) {
      return types.values().iterator().next();
    }

    // At the metadata level, a list always holds a union. But, at the
    // implementation layer, a union of a single type is collapsed out
    // to leave just a list of that single type.
    //
    // Make up a synthetic union column to be used when building
    // a reader.
    return VariantColumnMetadata.unionOf("$data", this);
  }

  @Override
  public void becomeSimple() {
    Preconditions.checkState(types.size() == 1);
    isSimple = true;
  }

  @Override
  public boolean isSimple() {
    return isSimple;
  }

  @Override
  public String toString() {
    return new StringBuilder()
        .append("[")
        .append(getClass().getSimpleName())
        .append(types.toString())
        .append(", simple: ")
        .append(isSimple)
        .append("]")
        .toString();
  }

  public VariantSchema cloneEmpty() {
    VariantSchema copy = new VariantSchema();
    copy.isSimple = isSimple;
    return copy;
  }

  public VariantSchema copy() {
    VariantSchema copy = new VariantSchema();
    copy.isSimple = isSimple;
    for (ColumnMetadata type : types.values()) {
      copy.addType(type);
    }
    return copy;
  }

  public boolean isEquivalent(VariantSchema other) {
    // Ignore isSimple, it is a derived attribute
    // Can't use equals(), do this the hard way.
    if (types.size() != other.types.size()) {
      return false;
    }
    if (!types.keySet().equals(other.types.keySet())) {
      return false;
    }
    for (Entry<MinorType, ColumnMetadata> entry : types.entrySet()) {
      ColumnMetadata otherType = other.types.get(entry.getKey());
      if (otherType == null) {
        return false;
      }
      if (!entry.getValue().isEquivalent(otherType)) {
        return false;
      }
    }
    return true;
  }
}
