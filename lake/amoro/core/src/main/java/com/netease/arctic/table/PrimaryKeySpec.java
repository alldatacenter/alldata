/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.table;

import org.apache.commons.lang.StringUtils;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableList;
import org.apache.iceberg.types.Types;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Represents the primary key specification of an {@link KeyedTable}, consist of some fields from table's {@link Schema}
 */
public class PrimaryKeySpec implements Serializable {
  public static final String PRIMARY_KEY_COLUMN_JOIN_DELIMITER = ",";

  private final Schema schema;
  private final ImmutableList<PrimaryKeyField> pkFields;

  private PrimaryKeySpec(Schema schema, List<PrimaryKeyField> pkFields) {
    this.schema = schema;
    this.pkFields = ImmutableList.copyOf(pkFields);
  }

  public Schema getSchema() {
    return schema;
  }

  public Schema getPkSchema() {
    return schema.select(pkFields.stream().map(PrimaryKeyField::fieldName).collect(Collectors.toList()));
  }

  public static Builder builderFor(Schema schema) {
    return new Builder(schema);
  }

  public List<PrimaryKeyField> fields() {
    return pkFields;
  }

  public PrimaryKeyField field(int index) {
    return pkFields.get(index);
  }

  public Types.StructType primaryKeyStruct() {
    return Types.StructType.of(pkFields.stream().map(field -> schema.findField(field.fieldName()))
        .collect(Collectors.toList()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrimaryKeySpec that = (PrimaryKeySpec) o;
    return pkFields.equals(that.pkFields);
  }

  @Override
  public int hashCode() {
    return Objects.hash(pkFields);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("primary key(");
    for (PrimaryKeyField field : pkFields) {
      sb.append(field).append(",");
    }
    if (pkFields.size() > 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    sb.append(")");
    return sb.toString();
  }

  public String description() {
    return pkFields.stream().map(PrimaryKeyField::fieldName)
        .collect(Collectors.joining(PRIMARY_KEY_COLUMN_JOIN_DELIMITER));
  }


  public List<String> fieldNames() {
    return pkFields.stream().map(PrimaryKeyField::fieldName).collect(Collectors.toList());
  }

  public static class Builder {
    private final Schema schema;
    private List<PrimaryKeyField> pkFields = new ArrayList<>();

    private Builder(Schema schema) {
      this.schema = schema;
    }

    public Builder addDescription(String columnDescription) {
      Arrays.stream(columnDescription.split(PRIMARY_KEY_COLUMN_JOIN_DELIMITER))
          .filter(StringUtils::isNotBlank).forEach(this::addColumn);
      return this;
    }

    public Builder addColumn(String columnName) {
      Types.NestedField sourceColumn = schema.findField(columnName);
      Preconditions.checkArgument(sourceColumn != null,
          "Cannot find source column: %s", columnName);
      addColumn(sourceColumn);
      return this;
    }

    public Builder addColumn(Integer columnIndex) {
      Types.NestedField sourceColumn = schema.findField(columnIndex);
      Preconditions.checkArgument(sourceColumn != null,
          "Cannot find source column by id: %s", columnIndex);
      addColumn(sourceColumn);
      return this;
    }

    public Builder addColumn(Types.NestedField sourceColumn) {
      pkFields.add(new PrimaryKeyField(sourceColumn.name()));
      return this;
    }

    public PrimaryKeySpec build() {
      if (pkFields.size() > 0) {
        return new PrimaryKeySpec(schema, pkFields);
      } else {
        return PrimaryKeySpec.noPrimaryKey();
      }
    }
  }

  public static class PrimaryKeyField implements Serializable {
    private final String fieldName;

    PrimaryKeyField(String fieldName) {
      this.fieldName = fieldName;
    }

    public String fieldName() {
      return fieldName;
    }

    @Override
    public String toString() {
      return fieldName;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      PrimaryKeyField that = (PrimaryKeyField) o;
      return Objects.equals(fieldName, that.fieldName);
    }

    @Override
    public int hashCode() {
      return Objects.hash(fieldName);
    }
  }

  public boolean primaryKeyExisted() {
    return !NO_PRIMARY_KEY_SPEC.equals(this);
  }

  private static final PrimaryKeySpec NO_PRIMARY_KEY_SPEC =
      new PrimaryKeySpec(new Schema(), ImmutableList.of());

  public static PrimaryKeySpec noPrimaryKey() {
    return NO_PRIMARY_KEY_SPEC;
  }
}
