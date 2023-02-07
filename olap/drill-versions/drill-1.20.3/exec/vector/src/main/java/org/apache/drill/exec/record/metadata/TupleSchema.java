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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.apache.drill.exec.record.MaterializedField;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Defines the schema of a tuple: either the top-level row or a nested
 * "map" (really structure). A schema is a collection of columns (backed
 * by vectors in the loader itself.) Columns are accessible by name or
 * index. New columns may be added at any time; the new column takes the
 * next available index.
 */
@JsonAutoDetect(
  fieldVisibility = JsonAutoDetect.Visibility.NONE,
  getterVisibility = JsonAutoDetect.Visibility.NONE,
  isGetterVisibility = JsonAutoDetect.Visibility.NONE,
  setterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@JsonPropertyOrder({"columns", "properties"})
public class TupleSchema extends AbstractPropertied implements TupleMetadata {

  public static final String TYPE = "tuple_schema";

  private AbstractMapColumnMetadata parentMap;
  private final TupleNameSpace<ColumnMetadata> nameSpace = new TupleNameSpace<>();

  public TupleSchema() { }

  @JsonCreator
  public TupleSchema(@JsonProperty("columns") List<AbstractColumnMetadata> columns,
                     @JsonProperty("properties") Map<String, String> properties) {
    if (columns != null) {
      columns.forEach(this::addColumn);
    }
    setProperties(properties);
  }

  public void bind(AbstractMapColumnMetadata parentMap) {
    this.parentMap = parentMap;
  }

  @Override
  public TupleSchema copy() {
    TupleSchema tuple = new TupleSchema();
    for (ColumnMetadata md : this) {
      tuple.addColumn(md.copy());
    }
    tuple.setProperties(properties());
    return tuple;
  }

  @Override
  public ColumnMetadata add(MaterializedField field) {
    ColumnMetadata md = MetadataUtils.fromField(field);
    add(md);
    return md;
  }

  public ColumnMetadata addView(MaterializedField field) {
    ColumnMetadata md = MetadataUtils.fromView(field);
    add(md);
    return md;
  }

  /**
   * Add a column metadata column created by the caller. Used for specialized
   * cases beyond those handled by {@link #add(MaterializedField)}.
   *
   * @param md the custom column metadata which must have the correct
   * index set (from {@link #size()}
   */
  public void add(ColumnMetadata md) {
    md.bind(this);
    nameSpace.add(md.name(), md);
  }

  @Override
  public int addColumn(ColumnMetadata column) {
    add(column);
    return size() - 1;
  }

  @Override
  public MaterializedField column(String name) {
    ColumnMetadata md = metadata(name);
    return md == null ? null : md.schema();
  }

  @Override
  public ColumnMetadata metadata(String name) {
    return nameSpace.get(name);
  }

  @Override
  public int index(String name) {
    return nameSpace.indexOf(name);
  }

  @Override
  public MaterializedField column(int index) {
    return metadata(index).schema();
  }

  @Override
  public ColumnMetadata metadata(int index) {
    return nameSpace.get(index);
  }

  @Override
  public AbstractMapColumnMetadata parent() { return parentMap; }

  @Override
  public int size() { return nameSpace.count(); }

  @Override
  public boolean isEmpty() { return nameSpace.count( ) == 0; }

  @Override
  public Iterator<ColumnMetadata> iterator() {
    return nameSpace.iterator();
  }

  @Override
  public boolean isEquivalent(TupleMetadata other) {
    TupleSchema otherSchema = (TupleSchema) other;
    if (nameSpace.count() != otherSchema.nameSpace.count()) {
      return false;
    }
    for (int i = 0; i < nameSpace.count(); i++) {
      if (! nameSpace.get(i).isEquivalent(otherSchema.nameSpace.get(i))) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean equals(Object o) {
    if (o == this) {
      return true;
    }
    if (o == null || o.getClass() != getClass()) {
      return false;
    }
    return isEquivalent((TupleMetadata) o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(parentMap, nameSpace);
  }

  @Override
  public List<MaterializedField> toFieldList() {
    List<MaterializedField> cols = new ArrayList<>();
    for (ColumnMetadata md : nameSpace) {
      cols.add(md.schema());
    }
    return cols;
  }

  @JsonProperty("columns")
  @Override
  public List<ColumnMetadata> toMetadataList() {
    return new ArrayList<>(nameSpace.entries());
  }

  @Override
  public String fullName(int index) {
    return fullName(metadata(index));
  }

  @Override
  public String fullName(ColumnMetadata column) {
    String quotedName = column.name();
    if (quotedName.contains(".")) {
      quotedName = "`" + quotedName + "`";
    }
    if (isRoot()) {
      return column.name();
    } else {
      return fullName() + "." + quotedName;
    }
  }

  public String fullName() {
    if (isRoot()) {
      return "<root>";
    } else {
      return parentMap.parentTuple().fullName(parentMap);
    }
  }

  public boolean isRoot() { return parentMap == null; }

  @Override
  public void replace(ColumnMetadata replaceWith) {
    nameSpace.replace(replaceWith.name(), replaceWith);
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder()
        .append("[")
        .append(getClass().getSimpleName())
        .append(" ");

    builder.append(nameSpace.entries().stream()
      .map(ColumnMetadata::toString)
      .collect(Collectors.joining(", ")));

    if (hasProperties()) {
      if (!nameSpace.entries().isEmpty()) {
        builder.append(", ");
      }
      builder.append("properties: ").append(properties());
    }

    builder.append("]");
    return builder.toString();
  }

  @JsonProperty("properties")
  @Override
  public Map<String, String> properties() {
    return super.properties();
  }
}
