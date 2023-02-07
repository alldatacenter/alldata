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

import java.util.stream.Collectors;

/**
 * Describes a map and repeated map. Both are tuples that have a tuple
 * schema as part of the column definition.
 */
public class MapColumnMetadata extends AbstractMapColumnMetadata {

  /**
   * Build a new map column from the field provided
   *
   * @param schema materialized field description of the map
   */
  public MapColumnMetadata(MaterializedField schema) {
    this(schema, null);
  }

  /**
   * Build a map column metadata by cloning the type information (but not
   * the children) of the materialized field provided.
   *
   * @param schema the schema to use
   * @param tupleSchema parent schema
   */
  MapColumnMetadata(MaterializedField schema, TupleSchema tupleSchema) {
    super(schema, tupleSchema);
  }

  public MapColumnMetadata(MapColumnMetadata from) {
    super(from);
  }

  public MapColumnMetadata(String name, DataMode mode, TupleSchema tupleSchema) {
    super(name, MinorType.MAP, mode, tupleSchema);
  }

  @Override
  public ColumnMetadata copy() {
    return new MapColumnMetadata(this);
  }

  @Override
  public ColumnMetadata cloneEmpty() {
    ColumnMetadata colMeta = new MapColumnMetadata(name, mode, new TupleSchema());
    colMeta.setProperties(this.properties());
    return colMeta;
  }

  @Override
  public boolean isMap() {
    return true;
  }

  @Override
  protected String internalTypeString() {
    return "STRUCT<"
      + tupleSchema().toMetadataList().stream()
          .map(ColumnMetadata::columnString)
          .collect(Collectors.joining(", "))
      + ">";
  }
}
