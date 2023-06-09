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
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.complex.DictVector;

import java.util.stream.Collectors;

public class DictColumnMetadata extends AbstractMapColumnMetadata {

  /**
   * Build a new dict column from the field provided
   *
   * @param schema materialized field description of the map
   */
  public DictColumnMetadata(MaterializedField schema) {
    this(schema, null);
  }

  public DictColumnMetadata(String name, TypeProtos.DataMode mode) {
    this(name, mode, null);
  }

  public DictColumnMetadata(DictColumnMetadata from) {
    super(from);
  }

  /**
   * Build a dict column metadata by cloning the type information (but not
   * the children) of the materialized field provided.
   *
   * @param schema the schema to use
   * @param tupleSchema parent schema
   */
  DictColumnMetadata(MaterializedField schema, TupleSchema tupleSchema) {
    super(schema, tupleSchema);
  }

  DictColumnMetadata(String name, TypeProtos.DataMode mode, TupleSchema tupleSchema) {
    super(name, TypeProtos.MinorType.DICT, mode, tupleSchema);
  }

  public ColumnMetadata keyColumnMetadata() {
    return schema.metadata(DictVector.FIELD_KEY_NAME);
  }

  public ColumnMetadata valueColumnMetadata() {
    return schema.metadata(DictVector.FIELD_VALUE_NAME);
  }

  @Override
  public ColumnMetadata copy() {
    return new DictColumnMetadata(this);
  }

  @Override
  public ColumnMetadata cloneEmpty() {
    ColumnMetadata colMeta = new DictColumnMetadata(name, mode, new TupleSchema());
    colMeta.setProperties(this.properties());
    return colMeta;
  }

  @Override
  public boolean isDict() {
    return true;
  }

  @Override
  protected String internalTypeString() {
    StringBuilder builder = new StringBuilder()
      .append("MAP<");

    ColumnMetadata key = keyColumnMetadata();
    ColumnMetadata value = valueColumnMetadata();

    // sometimes dict key and value are added after creating metadata class,
    // and if `typeString` method was called prematurely, for example, in case of error
    // add whatever was added in a form of columns with key / value names
    if (key == null || value == null) {
      builder.append(tupleSchema().toMetadataList().stream()
        .map(ColumnMetadata::columnString)
        .collect(Collectors.joining(", ")));
    } else {
      builder.append(key.typeString())
        .append(", ")
        .append(value.typeString());

      if (TypeProtos.DataMode.REQUIRED == value.mode()) {
        builder.append(" NOT NULL");
      }
    }

    builder.append(">");
    return builder.toString();
  }

  @Override
  public StructureType structureType() {
    return StructureType.DICT;
  }
}
