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
package org.apache.drill.exec.store.enumerable;

import org.apache.calcite.DataContext;
import org.apache.calcite.jdbc.JavaTypeFactoryImpl;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.util.BuiltInMethod;
import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.compile.ClassBuilder;
import org.apache.drill.exec.exception.ClassTransformationException;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.impl.scan.framework.ManagedReader;
import org.apache.drill.exec.physical.impl.scan.framework.SchemaNegotiator;
import org.apache.drill.exec.physical.resultSet.ResultSetLoader;
import org.apache.drill.exec.physical.resultSet.RowSetLoader;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.record.ColumnConverter;
import org.apache.drill.exec.record.ColumnConverterFactory;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.record.metadata.TupleSchema;
import org.apache.drill.shaded.guava.com.google.common.base.Throwables;
import org.codehaus.commons.compiler.CompileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * {@link ManagedReader} implementation that compiles and executes specified code,
 * calls the method on it for obtaining the values, and reads the results using column converters.
 */
public class EnumerableRecordReader implements ManagedReader<SchemaNegotiator> {

  private static final Logger logger = LoggerFactory.getLogger(EnumerableRecordReader.class);

  private static final String CLASS_NAME = "Baz";

  private final List<SchemaPath> columns;

  private final Map<String, Integer> fieldsMap;

  private final String code;

  private final String schemaPath;

  private final ColumnConverterFactoryProvider factoryProvider;

  private ColumnConverter converter;

  private Iterator<Map<String, Object>> records;

  private ResultSetLoader loader;

  public EnumerableRecordReader(List<SchemaPath> columns, Map<String, Integer> fieldsMap,
    String code, String schemaPath, ColumnConverterFactoryProvider factoryProvider) {
    this.columns = columns;
    this.fieldsMap = fieldsMap;
    this.code = code;
    this.schemaPath = schemaPath;
    this.factoryProvider = factoryProvider;
  }

  @SuppressWarnings("unchecked")
  private void setup(OperatorContext context) {
    SchemaPlus rootSchema = context.getFragmentContext().getFullRootSchema();
    DataContext root = new DrillDataContext(
        schemaPath != null ? SchemaUtilites.searchSchemaTree(rootSchema, SchemaUtilites.getSchemaPathAsList(schemaPath)) : rootSchema,
        new JavaTypeFactoryImpl(),
        Collections.emptyMap());

    try {
      Class<?> implementationClass = ClassBuilder.getCompiledClass(code, CLASS_NAME,
          context.getFragmentContext().getConfig(), context.getFragmentContext().getOptions());
      Iterable<?> iterable =
          (Iterable<Map<String, Object>>) implementationClass.getMethod(BuiltInMethod.BINDABLE_BIND.method.getName(), DataContext.class)
              .invoke(implementationClass.newInstance(), root);
      if (fieldsMap.keySet().size() == 1) {
        // for the case of projecting single column, its value is returned
        records = StreamSupport.stream(iterable.spliterator(), false)
            .map(this::wrap)
            .iterator();
      } else {
        // for the case when all columns were projected, array is returned
        records = StreamSupport.stream(iterable.spliterator(), false)
            .map(row -> wrap((Object[]) row))
            .iterator();
      }
    } catch (CompileException | IOException | ClassTransformationException | ReflectiveOperationException e) {
      logger.error("Exception happened when executing generated code", e);
      Throwable rootCause = Throwables.getRootCause(e);
      throw new DrillRuntimeException(rootCause.getMessage(), rootCause);
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> wrap(Object[] values) {
    Map<String, Object> row = new HashMap<>();
    columns.stream()
        .map(SchemaPath::getRootSegmentPath)
        .forEach(fieldName -> {
          if (fieldName.equals(SchemaPath.DYNAMIC_STAR)) {
            row.putAll((Map<? extends String, ?>) values[fieldsMap.get(fieldName)]);
          } else {
            row.put(fieldName, values[fieldsMap.get(fieldName)]);
          }
        });
    return row;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> wrap(Object value) {
    SchemaPath schemaPath = columns.iterator().next();
    if (schemaPath.equals(SchemaPath.STAR_COLUMN)) {
      return (Map<String, Object>) value;
    }
    return Collections.singletonMap(schemaPath.getRootSegmentPath(), value);
  }

  @Override
  public boolean open(SchemaNegotiator negotiator) {
    TupleMetadata providedSchema = negotiator.providedSchema();
    loader = negotiator.build();
    setup(negotiator.context());
    ColumnConverterFactory factory = factoryProvider.getFactory(providedSchema);
    converter = factory.getRootConverter(providedSchema, new TupleSchema(), loader.writer());
    return true;
  }

  @Override
  public boolean next() {
    RowSetLoader rowWriter = loader.writer();
    while (!rowWriter.isFull()) {
      if (records.hasNext()) {
        processRecord(rowWriter, records.next());
      } else {
        return false;
      }
    }
    return true;
  }

  private void processRecord(RowSetLoader writer, Map<String, Object> record) {
    writer.start();
    converter.convert(record);
    writer.save();
  }

  @Override
  public void close() {
    loader.close();
  }
}
