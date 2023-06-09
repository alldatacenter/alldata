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
package org.apache.drill.exec.store.pojo;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.exec.physical.impl.OutputMutator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.StdConverter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dynamically reads values from the given list of records.
 * Creates writers based on given schema.
 *
 * @param <T> type of given values, if contains various types, use Object class
 */
@JsonTypeName("dynamic-pojo-record-reader")
public class DynamicPojoRecordReader<T> extends AbstractPojoRecordReader<List<T>> {

  @JsonProperty
  private LinkedHashMap<String, Class<?>> schema;

  public DynamicPojoRecordReader(LinkedHashMap<String, Class<?>> schema, List<List<T>> records) {
    super(records);
    validateAndSetSchema(schema);
  }

  public DynamicPojoRecordReader(LinkedHashMap<String, Class<?>> schema, List<List<T>> records, int maxRecordsToRead) {
    super(records, maxRecordsToRead);
    validateAndSetSchema(schema);
  }

  /**
   * Initiates writers based on given schema which contains field name and its type.
   *
   * @param output output mutator
   * @return list of pojo writers
   */
  @Override
  protected List<PojoWriter> setupWriters(OutputMutator output) throws ExecutionSetupException {
    List<PojoWriter> writers = new ArrayList<>();
    for (Map.Entry<String, Class<?>> field : schema.entrySet()) {
      writers.add(initWriter(field.getValue(), field.getKey(), output));
    }
    return writers;
  }

  @Override
  protected Object getFieldValue(List<T> row, int fieldPosition) {
    return row.get(fieldPosition);
  }

  @Override
  public String toString() {
    return "DynamicPojoRecordReader{" +
        "records = " + records +
        "}";
  }

  private void validateAndSetSchema(LinkedHashMap<String, Class<?>> schema) {
    Preconditions.checkState(schema != null && !schema.isEmpty(), "Undefined schema is not allowed.");
    this.schema = schema;
  }

  /**
   * An utility class that converts from {@link com.fasterxml.jackson.databind.JsonNode}
   * to DynamicPojoRecordReader during physical plan fragment deserialization.
   */
  public static class Converter extends StdConverter<JsonNode, DynamicPojoRecordReader> {
    private static final TypeReference<LinkedHashMap<String, Class<?>>> schemaType =
        new TypeReference<LinkedHashMap<String, Class<?>>>() {};

    private final ObjectMapper mapper;

    public Converter(ObjectMapper mapper) {
      this.mapper = mapper;
    }

    @Override
    public DynamicPojoRecordReader convert(JsonNode value) {
      LinkedHashMap<String, Class<?>> schema = mapper.convertValue(value.get("schema"), schemaType);
      List<List<?>> records = new ArrayList<>();

      JsonNode serializedRecords = value.get("records");
      for (JsonNode serializedRecord : serializedRecords) {
        List<Object> record = new ArrayList<>(schema.size());
        Iterator<JsonNode> recordsIterator = serializedRecord.elements();
        for (Class<?> fieldType : schema.values()) {
          record.add(mapper.convertValue(recordsIterator.next(), fieldType));
        }
        records.add(record);
      }

      int maxRecordsToRead = value.get("recordsPerBatch").asInt();
      return new DynamicPojoRecordReader(schema, records, maxRecordsToRead);
    }
  }
}
