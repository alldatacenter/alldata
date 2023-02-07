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
package org.apache.drill.exec.expr.fn.impl;

import io.netty.buffer.DrillBuf;
import org.apache.drill.exec.expr.DrillAggFunc;
import org.apache.drill.exec.expr.annotations.FunctionTemplate;
import org.apache.drill.exec.expr.annotations.Output;
import org.apache.drill.exec.expr.annotations.Param;
import org.apache.drill.exec.expr.annotations.Workspace;
import org.apache.drill.exec.expr.holders.NullableVarCharHolder;
import org.apache.drill.exec.expr.holders.ObjectHolder;
import org.apache.drill.exec.expr.holders.VarCharHolder;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.complex.reader.FieldReader;

import javax.inject.Inject;

public class SchemaFunctions {

  /**
   * Aggregate function which infers schema from incoming data and returns string representation of {@link TupleMetadata}
   * with incoming schema.
   */
  @FunctionTemplate(name = "schema",
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                    isInternal = true,
                    isVarArg = true)
  public static class SchemaFunction implements DrillAggFunc {

    @Param FieldReader[] inputs;
    @Output NullableVarCharHolder out;
    @Inject DrillBuf buf;
    @Workspace ObjectHolder columnsHolder;

    @Override
    public void setup() {
      columnsHolder = new ObjectHolder();
    }

    @Override
    public void add() {
      java.util.Map<String, org.apache.drill.exec.record.MaterializedField> columns;
      if (columnsHolder.obj == null) {
        // Janino does not support diamond operator for this case :(
        columnsHolder.obj = new java.util.LinkedHashMap<String, org.apache.drill.exec.record.MaterializedField>();
      }

      columns = (java.util.Map<String, org.apache.drill.exec.record.MaterializedField>) columnsHolder.obj;

      for (int i = 0; i < inputs.length; i += 2) {
        String columnName = inputs[i].readObject().toString();
        // Janino cannot infer type
        org.apache.drill.exec.record.MaterializedField materializedField =
            (org.apache.drill.exec.record.MaterializedField) columns.get(columnName);
        org.apache.drill.common.types.TypeProtos.MajorType type = inputs[i + 1].getType();
        if (materializedField != null && !materializedField.getType().equals(type)) {
          org.apache.drill.common.types.TypeProtos.MinorType leastRestrictiveType =
              org.apache.drill.exec.resolver.TypeCastRules.getLeastRestrictiveType(
                  java.util.Arrays.asList(materializedField.getType().getMinorType(), type.getMinorType()));
          org.apache.drill.common.types.TypeProtos.DataMode leastRestrictiveMode =
              org.apache.drill.exec.resolver.TypeCastRules.getLeastRestrictiveDataMode(
                  java.util.Arrays.asList(materializedField.getType().getMode(), type.getMode()));

          org.apache.drill.exec.record.MaterializedField clone = materializedField.clone();
          clone.replaceType(materializedField.getType().toBuilder()
              .setMinorType(leastRestrictiveType)
              .setMode(leastRestrictiveMode)
              .build());
          columns.put(columnName, clone);
        } else {
          if (type.getMinorType() == org.apache.drill.common.types.TypeProtos.MinorType.MAP) {
            columns.put(columnName, inputs[i + 1].getField());
          } else {
            columns.put(columnName, org.apache.drill.exec.record.MaterializedField.create(columnName, type));
          }
        }
      }
    }

    @Override
    public void output() {
      org.apache.drill.exec.record.metadata.SchemaBuilder schemaBuilder =
          new org.apache.drill.exec.record.metadata.SchemaBuilder();

      java.util.Map<String, org.apache.drill.exec.record.MaterializedField> columns =
          (java.util.Map<String, org.apache.drill.exec.record.MaterializedField>) columnsHolder.obj;

      if (columns == null) {
        return;
      }

      for (org.apache.drill.exec.record.MaterializedField materializedField : columns.values()) {
        // Janino compiler cannot infer types from generics :(
        schemaBuilder.add((org.apache.drill.exec.record.MaterializedField) materializedField);
      }

      byte[] type = schemaBuilder.build().jsonString().getBytes();
      buf = buf.reallocIfNeeded(type.length);
      buf.setBytes(0, type);
      out.buffer = buf;
      out.start = 0;
      out.end = type.length;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      columnsHolder.obj = null;
    }
  }

  /**
   * Aggregate function which accepts VarChar column with string representations of {@link TupleMetadata}
   * and returns string representation of {@link TupleMetadata} with merged schema.
   */
  @FunctionTemplate(name = "merge_schema",
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                    isInternal = true)
  public static class MergeNullableSchemaFunction implements DrillAggFunc {

    @Param NullableVarCharHolder input;
    @Output NullableVarCharHolder out;
    @Inject DrillBuf buf;
    @Workspace ObjectHolder schemaHolder;

    @Override
    public void setup() {
      schemaHolder = new ObjectHolder();
    }

    @Override
    public void add() {
      if (input.isSet == 0) {
        return;
      }

      org.apache.drill.exec.record.metadata.TupleMetadata currentSchema =
        org.apache.drill.exec.expr.fn.impl.SchemaFunctions.getTupleMetadata(
          org.apache.drill.exec.expr.fn.impl.StringFunctionHelpers.toStringFromUTF8(input.start, input.end, input.buffer));

      if (schemaHolder.obj == null) {
        schemaHolder.obj = currentSchema;
        return;
      }

      org.apache.drill.exec.record.metadata.TupleMetadata resolvedSchema =
          (org.apache.drill.exec.record.metadata.TupleMetadata) schemaHolder.obj;

      if (!resolvedSchema.isEquivalent(currentSchema)) {
        throw new UnsupportedOperationException("merge_schema function does not support schema changes.");
      }
    }

    @Override
    public void output() {
      org.apache.drill.exec.record.metadata.TupleMetadata resolvedSchema =
          (org.apache.drill.exec.record.metadata.TupleMetadata) schemaHolder.obj;

      byte[] type = resolvedSchema.jsonString().getBytes();
      buf = buf.reallocIfNeeded(type.length);
      buf.setBytes(0, type);
      out.buffer = buf;
      out.start = 0;
      out.end = type.length;
      out.isSet = 1;
    }

    @Override
    public void reset() {
      schemaHolder.obj = null;
    }

  }

  @FunctionTemplate(name = "merge_schema",
                    scope = FunctionTemplate.FunctionScope.POINT_AGGREGATE,
                    isInternal = true)
  public static class MergeSchemaFunction implements DrillAggFunc {

    @Param VarCharHolder input;
    @Output VarCharHolder out;
    @Inject DrillBuf buf;
    @Workspace ObjectHolder schemaHolder;

    @Override
    public void setup() {
      schemaHolder = new ObjectHolder();
    }

    @Override
    public void add() {
      org.apache.drill.exec.record.metadata.TupleMetadata currentSchema = org.apache.drill.exec.expr.fn.impl.SchemaFunctions.getTupleMetadata(
          org.apache.drill.common.util.DrillStringUtils.toBinaryString(input.buffer, input.start, input.end));
      if (schemaHolder.obj == null) {
        schemaHolder.obj = currentSchema;
        return;
      }

      org.apache.drill.exec.record.metadata.TupleMetadata resolvedSchema =
          (org.apache.drill.exec.record.metadata.TupleMetadata) schemaHolder.obj;

      if (!resolvedSchema.isEquivalent(currentSchema)) {
        throw new UnsupportedOperationException("merge_schema function does not support schema changes.");
      }
    }

    @Override
    public void output() {
      org.apache.drill.exec.record.metadata.TupleMetadata resolvedSchema =
          (org.apache.drill.exec.record.metadata.TupleMetadata) schemaHolder.obj;

      byte[] type = resolvedSchema.jsonString().getBytes();
      buf = buf.reallocIfNeeded(type.length);
      buf.setBytes(0, type);
      out.buffer = buf;
      out.start = 0;
      out.end = type.length;
    }

    @Override
    public void reset() {
      schemaHolder.obj = null;
    }
  }

  /**
   * Wraps static method from TupleMetadata to avoid {@link IncompatibleClassChangeError} for JDK 9+.
   * {@see JDK-8147755}.
   *
   * @param serialized tuple metadata in JSON string representation
   * @return {@link TupleMetadata} instance
   */
  public static TupleMetadata getTupleMetadata(String serialized) {
    return TupleMetadata.of(serialized);
  }
}
