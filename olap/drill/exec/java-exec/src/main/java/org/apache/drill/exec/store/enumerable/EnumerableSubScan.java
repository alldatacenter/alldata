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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractSubScan;
import org.apache.drill.exec.record.metadata.TupleMetadata;

import java.util.List;
import java.util.Map;

public class EnumerableSubScan extends AbstractSubScan {

  public static final String OPERATOR_TYPE = "ENUMERABLE_SUB_SCAN";

  private final String code;
  private final String schemaPath;
  private final List<SchemaPath> columns;
  private final Map<String, Integer> fieldsMap;
  private final TupleMetadata schema;
  private final ColumnConverterFactoryProvider converterFactoryProvider;

  @JsonCreator
  public EnumerableSubScan(
      @JsonProperty("code") String code,
      @JsonProperty("columns") List<SchemaPath> columns,
      @JsonProperty("fieldsMap") Map<String, Integer> fieldsMap,
      @JsonProperty("schema") TupleMetadata schema,
      @JsonProperty("schemaPath") String schemaPath,
      @JsonProperty("converterFactoryProvider") ColumnConverterFactoryProvider converterFactoryProvider) {
    super("");
    this.code = code;
    this.columns = columns;
    this.fieldsMap = fieldsMap;
    this.schema = schema;
    this.schemaPath = schemaPath;
    this.converterFactoryProvider = converterFactoryProvider;
  }

  @Override
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  public TupleMetadata getSchema() {
    return schema;
  }

  public String getCode() {
    return code;
  }

  public List<SchemaPath> getColumns() {
    return columns;
  }

  public Map<String, Integer> getFieldsMap() {
    return fieldsMap;
  }

  public String getSchemaPath() {
    return schemaPath;
  }

  public ColumnConverterFactoryProvider getConverterFactoryProvider() {
    return converterFactoryProvider;
  }
}
