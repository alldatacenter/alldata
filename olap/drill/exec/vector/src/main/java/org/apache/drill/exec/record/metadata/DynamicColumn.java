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
import org.apache.drill.exec.record.MaterializedField;

/**
 * A dynamic column has a name but not a type. The column may be
 * a map, array or scalar: we don't yet know. A dynamic column is
 * the equivalent of an item in a name-only project list. This type
 * can also represent a wildcard. A dynamic column is not a concrete
 * data description: it must be resolved to an actual type before
 * it can be used to create vectors, readers, writers, etc. The
 * dynamic column allows the tuple metadata to be used to represent
 * all phases of a schema lifecycle, including Drill's "dynamic"
 * schema before a reader resolves the column to some actual
 * type.
 */
public class DynamicColumn extends AbstractColumnMetadata {

  // Same as SchemaPath.DYNAMIC_STAR, but SchemaPath is not visible here.
  public static final String WILDCARD = "**";
  public static final DynamicColumn WILDCARD_COLUMN = new DynamicColumn(WILDCARD);

  public DynamicColumn(String name) {
    super(name, MinorType.LATE, DataMode.REQUIRED);
  }

  @Override
  public StructureType structureType() { return StructureType.DYNAMIC; }

  @Override
  public boolean isDynamic() { return true; }

  @Override
  public MaterializedField schema() {
    return MaterializedField.create(name, majorType());
  }

  @Override
  public MaterializedField emptySchema() {
    return schema();
  }

  @Override
  public ColumnMetadata cloneEmpty() {
    return copy();
  }

  @Override
  public ColumnMetadata copy() {
    return new DynamicColumn(name);
  }

  @Override
  public boolean isEquivalent(ColumnMetadata o) {
    if (o == this) {
      return true;
    }

    // For a dynamic column, only the name is important.
    // Type and mode must be LATE, REQUIRED, so we need not
    // compare them.
    DynamicColumn other = (DynamicColumn) o;
    return name.equalsIgnoreCase(other.name);
  }

  @Override
  public String columnString() {
    // Dynamic columns carry no type information, just the
    // "DYNAMIC" marker.
    return new StringBuilder()
      .append("`")
      .append(escapeSpecialSymbols(name()))
      .append("`")
      .append(" ")
      .append(typeString())
      .toString();
  }

  @Override
  public String typeString() {
    return "DYNAMIC";
  }
}
