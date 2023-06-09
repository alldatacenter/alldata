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
package org.apache.drill.exec.store.elasticsearch;

import org.apache.drill.exec.record.ColumnConverter;
import org.apache.drill.exec.record.ColumnConverterFactory;
import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.vector.accessor.TupleWriter;
import org.apache.drill.exec.vector.accessor.ValueWriter;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ElasticsearchColumnConverterFactory extends ColumnConverterFactory {

  public ElasticsearchColumnConverterFactory(TupleMetadata providedSchema) {
    super(providedSchema);
  }

  @Override
  public ColumnConverter.ScalarColumnConverter buildScalar(ColumnMetadata readerSchema, ValueWriter writer) {
    switch (readerSchema.type()) {
      case BIT:
        return new ColumnConverter.ScalarColumnConverter(value -> writer.setBoolean((Boolean) value));
      default:
        return super.buildScalar(readerSchema, writer);
    }
  }

  @Override
  protected ColumnConverter getMapConverter(TupleMetadata providedSchema,
    TupleMetadata readerSchema, TupleWriter tupleWriter) {
    Map<String, ColumnConverter> converters = StreamSupport.stream(readerSchema.spliterator(), false)
      .collect(Collectors.toMap(
        ColumnMetadata::name,
        columnMetadata ->
          getConverter(providedSchema, columnMetadata, tupleWriter.column(columnMetadata.name()))));

    return new ElasticMapColumnConverter(this, providedSchema, tupleWriter, converters);
  }

  private static class ElasticMapColumnConverter extends ColumnConverter.MapColumnConverter {

    public ElasticMapColumnConverter(ColumnConverterFactory factory, TupleMetadata providedSchema, TupleWriter tupleWriter, Map<String, ColumnConverter> converters) {
      super(factory, providedSchema, tupleWriter, converters);
    }
  }
}
