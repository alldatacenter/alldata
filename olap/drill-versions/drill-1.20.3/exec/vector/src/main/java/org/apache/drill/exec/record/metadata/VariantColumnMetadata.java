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

import java.util.List;

import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.MaterializedField;

public class VariantColumnMetadata extends AbstractColumnMetadata {

  private final VariantSchema variantSchema;

  public VariantColumnMetadata(MaterializedField schema) {
    super(schema);
    variantSchema = new VariantSchema();
    variantSchema.bind(this);
    List<MinorType> types;
    if (type() == MinorType.UNION) {
      types = schema.getType().getSubTypeList();
    } else {
      assert type() == MinorType.LIST;
      MaterializedField child;
      MinorType childType;
      if (schema.getChildren().isEmpty()) {
        child = null;
        childType = MinorType.LATE;
      } else {
        child = schema.getChildren().iterator().next();
        childType = child.getType().getMinorType();
      }
      switch (childType) {
      case UNION:

        // List contains a union.

        types = child.getType().getSubTypeList();
        break;

      case LATE:

        // List has no type.

        return;

      default:

        // List contains a single non-null type.

        variantSchema.addType(MetadataUtils.fromField(child));
        return;
      }
    }
    if (types == null) {
      return;
    }
    for (MinorType type : types) {
      variantSchema.addType(type);
    }
  }

  public VariantColumnMetadata(MaterializedField schema, VariantSchema variantSchema) {
    super(schema);
    this.variantSchema = variantSchema;
  }

  private VariantColumnMetadata(String name, MinorType type, DataMode mode,
      VariantSchema variantSchema) {
    super(name, type, mode);
    this.variantSchema = variantSchema;
    this.variantSchema.bind(this);
  }

  public static VariantColumnMetadata union(String name) {
    return unionOf(name, null);
  }

  public static VariantColumnMetadata unionOf(String name, VariantSchema variantSchema) {
    return new VariantColumnMetadata(name, MinorType.UNION, DataMode.OPTIONAL,
        variantSchemaFor(variantSchema));
  }

  public static VariantColumnMetadata unionOf(MaterializedField schema, VariantSchema variantSchema) {
    return new VariantColumnMetadata(schema, variantSchemaFor(variantSchema));
  }

  public static VariantColumnMetadata list(String name) {
    return new VariantColumnMetadata(name, MinorType.LIST, DataMode.OPTIONAL, new VariantSchema());
  }

  public static VariantColumnMetadata listOf(String name, VariantSchema variantSchema) {
    return new VariantColumnMetadata(name, MinorType.LIST, DataMode.OPTIONAL,
        variantSchemaFor(variantSchema));
  }

  public static VariantColumnMetadata variantOf(String name, MinorType type, VariantSchema variantSchema) {
    switch (type) {
      case UNION:
        return unionOf(name, variantSchema);
      case LIST:
        return listOf(name, variantSchema);
      default:
        throw new IllegalArgumentException(type.name());
    }
  }

  private static VariantSchema variantSchemaFor(VariantSchema variantSchema) {
    return variantSchema == null ? new VariantSchema() : variantSchema;
  }

  @Override
  public StructureType structureType() {
    return StructureType.VARIANT;
  }

  @Override
  public boolean isVariant() { return true; }

  @Override
  public boolean isArray() {
    return super.isArray() || type() == MinorType.LIST;
  }

  @Override
  public ColumnMetadata cloneEmpty() {
    VariantColumnMetadata colMeta = new VariantColumnMetadata(name, type, mode, new VariantSchema());
    colMeta.setProperties(this.properties());
    return colMeta;
  }

  @Override
  public ColumnMetadata copy() {
    return new VariantColumnMetadata(name, type, mode, variantSchema.copy());
  }

  @Override
  public VariantMetadata variantSchema() {
    return variantSchema;
  }

  @Override
  public String typeString() {
    StringBuilder builder = new StringBuilder();
    if (isArray()) {
      builder.append("ARRAY<");
    }
    builder.append("UNION");
    if (isArray()) {
      builder.append(">");
    }
    return builder.toString();
  }

  @Override
  public MaterializedField schema() {
    return MaterializedField.create(name,
        MajorType.newBuilder()
          .setMinorType(type)
          .setMode(DataMode.OPTIONAL)
          .addAllSubType(variantSchema.types())
          .build());
  }

  @Override
  public MaterializedField emptySchema() {
    return MaterializedField.create(name,
        MajorType.newBuilder()
          .setMinorType(type)
          .setMode(DataMode.OPTIONAL)
          .build());
  }

  @Override
  protected void appendContents(StringBuilder buf) {
    buf.append(", variant: ")
       .append(variantSchema().toString());
  }

  @Override
  public boolean isEquivalent(ColumnMetadata o) {
    if (!super.isEquivalent(o)) {
      return false;
    }
    VariantColumnMetadata other = (VariantColumnMetadata) o;
    return variantSchema.isEquivalent(other.variantSchema);
  }
}
