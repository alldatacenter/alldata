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
package org.apache.drill.exec.store.iceberg.read;

import org.apache.drill.exec.physical.impl.scan.v3.FixedReceiver;
import org.apache.drill.exec.record.ColumnConverter;
import org.apache.drill.exec.record.ColumnConverterFactory;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.iceberg.data.Record;

import java.util.HashMap;
import java.util.Map;

public class MapColumnConverter implements ColumnConverter {

  private final ColumnConverterFactory factory;

  private final TupleMetadata providedSchema;

  private final TupleWriter tupleWriter;

  private final Map<String, ColumnConverter> converters;

  public MapColumnConverter(ColumnConverterFactory factory,
    TupleMetadata providedSchema,
    TupleWriter tupleWriter, Map<String, ColumnConverter> converters) {
    this.factory = factory;
    this.providedSchema = providedSchema;
    this.tupleWriter = tupleWriter;
    this.converters = new HashMap<>(converters);
  }

  @Override
  public void convert(Object value) {
    if (value == null) {
      return;
    }

    Record record = (Record) value;

    if (converters.isEmpty()) {
      buildMapMembers(record, providedSchema, tupleWriter, converters);
    }

    record.struct().fields()
      .forEach(field -> processValue(field.name(), record.getField(field.name())));
  }

  private void processValue(String name, Object columnValue) {
    ColumnConverter columnConverter = converters.get(name);
    if (columnConverter != null) {
      columnConverter.convert(columnValue);
    }
  }

  public void buildMapMembers(Record record, TupleMetadata providedSchema,
    TupleWriter tupleWriter, Map<String, ColumnConverter> converters) {
    TupleMetadata readerSchema = IcebergColumnConverterFactory.convertSchema(record.struct());
    TupleMetadata tableSchema = FixedReceiver.Builder.mergeSchemas(providedSchema, readerSchema);
    tableSchema.toMetadataList().forEach(tupleWriter::addColumn);

    for (ColumnMetadata columnMetadata : tableSchema) {
      String name = columnMetadata.name();
      converters.put(name, factory.getConverter(providedSchema,
        readerSchema.metadata(name), tupleWriter.column(name)));
    }
  }
}
