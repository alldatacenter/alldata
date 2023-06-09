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
package org.apache.drill.exec.record;

import org.apache.drill.exec.physical.impl.scan.convert.StandardConversions;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.ColumnConverter.ArrayColumnConverter;
import org.apache.drill.exec.record.ColumnConverter.DictColumnConverter;
import org.apache.drill.exec.record.ColumnConverter.DummyColumnConverter;
import org.apache.drill.exec.record.ColumnConverter.MapColumnConverter;
import org.apache.drill.exec.record.ColumnConverter.ScalarColumnConverter;
import org.apache.drill.exec.vector.accessor.ArrayWriter;
import org.apache.drill.exec.vector.accessor.DictWriter;
import org.apache.drill.exec.vector.accessor.ObjectWriter;
import org.apache.drill.exec.vector.accessor.ScalarWriter;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;
import org.apache.drill.exec.vector.complex.DictVector;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ColumnConverterFactory {

  private final StandardConversions standardConversions;

  public ColumnConverterFactory(TupleMetadata providedSchema) {
    if (providedSchema == null) {
      standardConversions = null;
    } else {
      standardConversions = StandardConversions.builder().withSchema(providedSchema).build();
    }
  }

  public ColumnConverter getRootConverter(TupleMetadata providedSchema,
      TupleMetadata readerSchema, TupleWriter tupleWriter) {
    return getMapConverter(providedSchema, readerSchema, tupleWriter);
  }

  /**
   * Based on column type, creates corresponding column converter
   * which holds conversion logic and appropriate writer to set converted data into.
   * For columns which are not projected, {@link DummyColumnConverter} is used.
   *
   * @param readerSchema column metadata
   * @param writer column writer
   * @return column converter
   */
  public ColumnConverter getConverter(TupleMetadata providedSchema,
      ColumnMetadata readerSchema, ObjectWriter writer) {
    if (!writer.isProjected()) {
      return DummyColumnConverter.INSTANCE;
    }

    if (readerSchema.isArray()) {
      return getArrayConverter(providedSchema,
          readerSchema, writer.array());
    }

    if (readerSchema.isMap()) {
      return getMapConverter(
          providedChildSchema(providedSchema, readerSchema),
          readerSchema.tupleSchema(), writer.tuple());
    }

    if (readerSchema.isDict()) {
      return getDictConverter(
          providedChildSchema(providedSchema, readerSchema),
          readerSchema.tupleSchema(), writer.dict());
    }

    return getScalarConverter(readerSchema, writer.scalar());
  }

  private TupleMetadata providedChildSchema(TupleMetadata providedSchema,
      ColumnMetadata readerSchema) {
    return Optional.ofNullable(providedSchema)
      .map(schema -> providedSchema.metadata(readerSchema.name()))
      .map(ColumnMetadata::tupleSchema)
      .orElse(null);
  }

  private ColumnConverter getArrayConverter(TupleMetadata providedSchema,
      ColumnMetadata readerSchema, ArrayWriter arrayWriter) {
    ObjectWriter valueWriter = arrayWriter.entry();
    ColumnConverter valueConverter;
    if (readerSchema.isMap()) {
      valueConverter = getMapConverter(providedSchema,
          readerSchema.tupleSchema(), valueWriter.tuple());
    } else if (readerSchema.isDict()) {
      valueConverter = getDictConverter(providedSchema,
          readerSchema.tupleSchema(), valueWriter.dict());
    } else if (readerSchema.isMultiList()) {
      valueConverter = getConverter(null, readerSchema.childSchema(), valueWriter);
    } else {
      valueConverter = getScalarConverter(readerSchema, valueWriter.scalar());
    }
    return new ArrayColumnConverter(arrayWriter, valueConverter);
  }

  protected ColumnConverter getMapConverter(TupleMetadata providedSchema,
      TupleMetadata readerSchema, TupleWriter tupleWriter) {
    Map<String, ColumnConverter> converters = StreamSupport.stream(readerSchema.spliterator(), false)
        .collect(Collectors.toMap(
            ColumnMetadata::name,
            columnMetadata ->
                getConverter(providedSchema, columnMetadata, tupleWriter.column(columnMetadata.name()))));

    return new MapColumnConverter(this, providedSchema, tupleWriter, converters);
  }

  private ColumnConverter getDictConverter(TupleMetadata providedSchema,
      TupleMetadata readerSchema, DictWriter dictWriter) {
    ColumnConverter keyConverter = getScalarConverter(
        readerSchema.metadata(DictVector.FIELD_KEY_NAME), dictWriter.keyWriter());
    ColumnConverter valueConverter = getConverter(providedSchema,
        readerSchema.metadata(DictVector.FIELD_VALUE_NAME), dictWriter.valueWriter());
    return new DictColumnConverter(dictWriter, keyConverter, valueConverter);
  }

  private ColumnConverter getScalarConverter(ColumnMetadata readerSchema, ScalarWriter scalarWriter) {
    ValueWriter valueWriter;
    if (standardConversions == null) {
      valueWriter = scalarWriter;
    } else {
      valueWriter = standardConversions.converterFor(scalarWriter, readerSchema);
    }
    return buildScalar(readerSchema, valueWriter);
  }

  public  ScalarColumnConverter buildScalar(ColumnMetadata readerSchema, ValueWriter writer) {
    return new ScalarColumnConverter(writer::setValue);
  }
}
