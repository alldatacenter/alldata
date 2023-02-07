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
 * Describes a base column type for map, dict, repeated map and repeated dict. All are tuples that have a tuple
 * schema as part of the column definition.
 */
public abstract class AbstractMapColumnMetadata extends AbstractColumnMetadata {

  protected TupleMetadata parentTuple;
  protected final TupleSchema schema;

  /**
   * Build a new map column from the field provided
   *
   * @param schema materialized field description of the map
   */
  public AbstractMapColumnMetadata(MaterializedField schema) {
    this(schema, null);
  }

  /**
   * Build column metadata by cloning the type information (but not
   * the children) of the materialized field provided.
   *
   * @param schema the schema to use
   * @param tupleSchema parent schema
   */
  AbstractMapColumnMetadata(MaterializedField schema, TupleSchema tupleSchema) {
    super(schema);
    if (tupleSchema == null) {
      this.schema = new TupleSchema();
    } else {
      this.schema = tupleSchema;
    }
    this.schema.bind(this);
  }

  public AbstractMapColumnMetadata(AbstractMapColumnMetadata from) {
    super(from);
    schema = from.schema.copy();
  }

  public AbstractMapColumnMetadata(String name, MinorType type, DataMode mode, TupleSchema schema) {
    super(name, type, mode);
    if (schema == null) {
      this.schema = new TupleSchema();
    } else {
      this.schema = schema;
    }
  }

  @Override
  public void bind(TupleMetadata parentTuple) {
    this.parentTuple = parentTuple;
  }

  @Override
  public ColumnMetadata.StructureType structureType() {
    return ColumnMetadata.StructureType.TUPLE;
  }

  @Override
  public TupleMetadata tupleSchema() {
    return schema;
  }

  @Override
  public int expectedWidth() {
    return 0;
  }

  public TupleMetadata parentTuple() {
    return parentTuple;
  }

  @Override
  public MaterializedField schema() {
    MaterializedField field = emptySchema();
    schema.toFieldList().forEach(field::addChild);
    return field;
  }

  @Override
  public MaterializedField emptySchema() {
    return MaterializedField.create(name,
      MajorType.newBuilder()
        .setMinorType(type)
        .setMode(mode)
        .build());
  }

  @Override
  public String typeString() {
    String typeString = internalTypeString();
    return isArray() ? "ARRAY<" + typeString + ">" : typeString;
  }

  /**
   * Returns specific type string representation of the type that extends this class.
   *
   * @return type string representation
   */
  protected abstract String internalTypeString();

  @Override
  protected void appendContents(StringBuilder buf) {
    buf.append(", schema: ")
       .append(tupleSchema().toString());
  }

  @Override
  public boolean isEquivalent(ColumnMetadata o) {
    if (!super.isEquivalent(o)) {
      return false;
    }
    AbstractMapColumnMetadata other = (AbstractMapColumnMetadata) o;
    return schema.equals(other.schema);
  }
}
