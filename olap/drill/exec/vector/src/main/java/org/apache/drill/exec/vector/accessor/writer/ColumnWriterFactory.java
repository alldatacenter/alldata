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
package org.apache.drill.exec.vector.accessor.writer;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.vector.NullableVector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.accessor.ColumnAccessorUtils;
import org.apache.drill.exec.vector.accessor.writer.AbstractArrayWriter.ArrayObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.AbstractScalarWriterImpl.ScalarObjectWriter;
import org.apache.drill.exec.vector.accessor.writer.dummy.DummyArrayWriter;
import org.apache.drill.exec.vector.accessor.writer.dummy.DummyScalarWriter;
import org.apache.drill.exec.vector.complex.RepeatedValueVector;

/**
 * Gather generated writer classes into a set of class tables to allow rapid
 * run-time creation of writers. Builds the writer and its object writer
 * wrapper which binds the vector to the writer.
 * <p>
 * Compared to the reader factory, the writer factor is a bit more complex
 * as it must handle both the projected ("real" writer) and unprojected
 * ("dummy" writer) cases. Because of the way the various classes interact,
 * it is cleaner to put the factory methods here rather than in the various
 * writers, as is done in the case of the readers.
 */
@SuppressWarnings("unchecked")
public class ColumnWriterFactory {

  private static final int typeCount = MinorType.values().length;
  private static final Class<? extends BaseScalarWriter> requiredWriters[] = new Class[typeCount];

  static {
    ColumnAccessorUtils.defineRequiredWriters(requiredWriters);
  }

  public static AbstractObjectWriter buildColumnWriter(ColumnMetadata schema,
      ValueVector vector) {
    if (vector == null) {
      return buildDummyColumnWriter(schema);
    }

    // Build a writer for a materialized column.
    assert schema.type() == vector.getField().getType().getMinorType();
    assert schema.mode() == vector.getField().getType().getMode();

    switch (schema.type()) {
      case GENERIC_OBJECT:
      case LATE:
      case NULL:
      case LIST:
      case MAP:
      case DICT:
      case UNION:
        throw new UnsupportedOperationException(schema.type().toString());
      default:
        switch (schema.mode()) {
        case OPTIONAL:
          return nullableScalarWriter(schema, (NullableVector) vector);
        case REQUIRED:
          return requiredScalarWriter(schema, vector);
        case REPEATED:
          return repeatedScalarWriter(schema, (RepeatedValueVector) vector);
        default:
          throw new UnsupportedOperationException(schema.mode().toString());
      }
    }
  }

  private static ScalarObjectWriter requiredScalarWriter(
      ColumnMetadata schema, ValueVector vector) {
    final BaseScalarWriter baseWriter = newWriter(vector);
    baseWriter.bindSchema(schema);
    return new ScalarObjectWriter(baseWriter);
  }

  private static ScalarObjectWriter nullableScalarWriter(
      ColumnMetadata schema, NullableVector vector) {
    final BaseScalarWriter baseWriter = newWriter(vector.getValuesVector());
    baseWriter.bindSchema(schema);
    return NullableScalarWriter.build(schema, vector, baseWriter);
  }

  private static AbstractObjectWriter repeatedScalarWriter(
      ColumnMetadata schema, RepeatedValueVector vector) {
    final BaseScalarWriter baseWriter = newWriter(vector.getDataVector());
    baseWriter.bindSchema(schema);
    return ScalarArrayWriter.build(schema, vector, baseWriter);
  }

  /**
   * Build a writer for a non-projected column.
   * @param schema schema of the column
   * @return a "dummy" writer for the column
   */
  public static AbstractObjectWriter buildDummyColumnWriter(ColumnMetadata schema) {
    switch (schema.type()) {
      case GENERIC_OBJECT:
      case LATE:
      case LIST:
      case MAP:
      case DICT:
      case UNION:
        throw new UnsupportedOperationException(schema.type().toString());
      default:
        final ScalarObjectWriter scalarWriter = new ScalarObjectWriter(
            new DummyScalarWriter(schema));
        switch (schema.mode()) {
        case OPTIONAL:
        case REQUIRED:
          return scalarWriter;
        case REPEATED:
          return new ArrayObjectWriter(
              new DummyArrayWriter(schema,
                scalarWriter));
        default:
          throw new UnsupportedOperationException(schema.mode().toString());
      }
    }
  }

  public static BaseScalarWriter newWriter(ValueVector vector) {
    final MajorType major = vector.getField().getType();
    final MinorType type = major.getMinorType();
    try {
      final Class<? extends BaseScalarWriter> accessorClass = requiredWriters[type.ordinal()];
      if (accessorClass == null) {
        throw new UnsupportedOperationException(type.toString());
      }
      final Constructor<? extends BaseScalarWriter> ctor = accessorClass.getConstructor(ValueVector.class);
      return ctor.newInstance(vector);
    } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
             SecurityException | IllegalArgumentException | InvocationTargetException e) {
      throw new IllegalStateException(e);
    }
  }
}
