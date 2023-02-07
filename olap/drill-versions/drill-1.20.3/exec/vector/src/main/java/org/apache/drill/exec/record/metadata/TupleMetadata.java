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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.drill.exec.record.MaterializedField;

/**
 * Metadata description of the schema of a row or a map.
 * In Drill, both rows and maps are
 * tuples: both are an ordered collection of values, defined by a
 * schema. Each tuple has a schema that defines the column ordering
 * for indexed access. Each tuple also provides methods to get column
 * accessors by name or index.
 * <p>
 * Models the physical schema of a row set showing the logical hierarchy of fields
 * with map fields as first-class fields. Map members appear as children
 * under the map, much as they appear in the physical value-vector
 * implementation.
 * <ul>
 * <li>Provides fast lookup by name or index.</li>
 * <li>Provides a nested schema, in this same form, for maps.</li>
 * </ul>
 * This form is useful when performing semantic analysis and when
 * working with vectors.
 * <p>
 * In the future, this structure will also gather metadata useful
 * for vector processing such as expected widths and so on.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = TupleSchema.class)
@JsonSubTypes({
  @JsonSubTypes.Type(value = TupleSchema.class, name = TupleSchema.TYPE)
})
public interface TupleMetadata extends Propertied, Iterable<ColumnMetadata> {

  ObjectWriter WRITER = new ObjectMapper().writerFor(TupleMetadata.class);
  ObjectReader READER = new ObjectMapper().readerFor(TupleMetadata.class);

  String IS_STRICT_SCHEMA_PROP = DRILL_PROP_PREFIX + "strict";

  /**
   * Add a new column to the schema.
   *
   * @param field materialized field
   * @return the index of the new column
   */
  ColumnMetadata add(MaterializedField field);
  int addColumn(ColumnMetadata column);

  int size();
  boolean isEmpty();
  int index(String name);
  ColumnMetadata metadata(int index);
  ColumnMetadata metadata(String name);
  MaterializedField column(int index);
  MaterializedField column(String name);
  boolean isEquivalent(TupleMetadata other);
  ColumnMetadata parent();

  /**
   * Return the schema as a list of <tt>MaterializedField</tt> objects
   * which can be used to create other schemas. Not valid for a
   * flattened schema.
   *
   * @return a list of the top-level fields. Maps contain their child
   * fields
   */
  List<MaterializedField> toFieldList();

  /**
   * Returns schema as list of <tt>ColumnMetadata</tt> objects
   * which can be used to create JSON schema object.
   *
   * @return a list of metadata for each column
   */
  List<ColumnMetadata> toMetadataList();

  /**
   * Full name of the column. Note: this name cannot be used to look up
   * the column because of ambiguity. The name "a.b.c" may mean a single
   * column with that name, or may mean maps "a", and "b" with column "c",
   * etc.
   *
   * @return full, dotted, column name
   */

  String fullName(ColumnMetadata column);
  String fullName(int index);
  TupleMetadata copy();
  void replace(ColumnMetadata replaceWith);

  /**
   * Converts current {@link TupleMetadata} implementation into JSON string representation.
   *
   * @return tuple metadata in JSON string representation
   * @throws IllegalStateException if unable to convert current instance into JSON string
   */
  default String jsonString() {
    try {
      return WRITER.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Unable to convert tuple metadata into JSON string: " + toString(), e);
    }
  }

  /**
   * Converts given JSON string into {@link TupleMetadata} instance.
   * {@link TupleMetadata} implementation is determined by present type property. For example: "type":"tuple_schema".
   * If given JSON string is untyped, used default implementation: {@link TupleSchema}.
   *
   * @param jsonString tuple metadata in JSON string representation
   * @return {@link TupleMetadata} instance, null if given JSON string is null or empty
   * @throws IllegalArgumentException if unable to deserialize given JSON string
   */
  static TupleMetadata of(String jsonString) {
    if (jsonString == null || jsonString.trim().isEmpty()) {
      return null;
    }
    try {
      return READER.readValue(jsonString);
    } catch (IOException e) {
      throw new IllegalArgumentException("Unable to deserialize given JSON string into tuple metadata: " + jsonString, e);
    }
  }
}
