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
package org.apache.drill.metastore.iceberg.transform;

import org.apache.iceberg.data.Record;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Base class to convert list of {@link Record}
 * into Metastore component units for the given list of column names.
 *
 * @param <T> Metastore component unit type
 */
public abstract class OutputDataTransformer<T> {

  private final Map<String, MethodHandle> unitSetters;
  private final List<String> columns = new ArrayList<>();
  private final List<Record> records = new ArrayList<>();

  protected OutputDataTransformer(Map<String, MethodHandle> unitSetters) {
    this.unitSetters = unitSetters;
  }

  public OutputDataTransformer<T> columns(List<String> columns) {
    this.columns.addAll(columns);
    return this;
  }

  public OutputDataTransformer<T> columns(String... columns) {
    return columns(Arrays.asList(columns));
  }

  public OutputDataTransformer<T> records(List<Record> records) {
    this.records.addAll(records);
    return this;
  }

  /**
   * Converts given list of {@link Record} into Metastore component units.
   * Specific for each Metastore component.
   *
   * @return list of Metastore component units
   */
  public abstract List<T> execute();

  /**
   * For each given record prepares specific methods handler and its value
   * to be set into Metastore specific component unit.
   * Ignores absent setters for columns and null values.
   *
   * @return list of methods handlers and values to set
   */
  protected List<Map<MethodHandle, Object>> valuesToSet() {
    return records.stream()
      .map(record -> columns.stream()
        .filter(column -> unitSetters.get(column) != null)
        .filter(column -> record.getField(column) != null)
        .collect(Collectors.toMap(
          unitSetters::get,
          record::getField,
          (o, n) -> n)))
      .collect(Collectors.toList());
  }
}
