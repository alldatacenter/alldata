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

import org.apache.drill.metastore.iceberg.exceptions.IcebergMetastoreException;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.types.Types;

import java.lang.invoke.MethodHandle;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Converts list of into Metastore component units into {@link WriteData} holder.
 *
 * @param <T> Metastore component unit type
 */
public class InputDataTransformer<T> {

  private final Schema tableSchema;
  private final Schema partitionSpecSchema;
  private final Map<String, MethodHandle> unitGetters;
  private final List<T> units = new ArrayList<>();

  public InputDataTransformer(Schema tableSchema,
                              Schema partitionSpecSchema,
                              Map<String, MethodHandle> unitGetters) {
    this.tableSchema = tableSchema;
    this.partitionSpecSchema = partitionSpecSchema;
    this.unitGetters = unitGetters;
  }

  public InputDataTransformer<T> units(List<T> units) {
    this.units.addAll(units);
    return this;
  }

  public WriteData execute() {
    List<Record> records = new ArrayList<>();
    Set<Record> partitions = new HashSet<>();
    for (T unit : units) {
      partitions.add(getPartition(unit, partitionSpecSchema, unitGetters));

      Record record = GenericRecord.create(tableSchema);
      for (Types.NestedField column : tableSchema.columns()) {
        String name = column.name();
        MethodHandle methodHandle = unitGetters.get(name);
        if (methodHandle == null) {
          // ignore absent getters
          continue;
        }
        try {
          record.setField(name, methodHandle.invoke(unit));
        } catch (Throwable e) {
          throw new IcebergMetastoreException(
            String.format("Unable to invoke getter for column [%s] using [%s]", name, methodHandle), e);
        }
      }
      records.add(record);
    }

    if (partitions.size() > 1) {
      throw new IcebergMetastoreException(String.format(
        "Partition keys values must be the same for all records in the partition. " +
          "Partition schema: [%s]. Received partition values: %s", partitionSpecSchema, partitions));
    }

    return new WriteData(records, partitions.isEmpty() ? null : partitions.iterator().next());
  }

  /**
   * Generates record with partition values based on given partition schema
   * and Metastore component unit instance.
   *
   * @param unit specific Metastore component unit
   * @param schema partition schema
   * @param unitGetters specific Metastore component unit getters
   * @return {@link Record} with partition values
   * @throws IcebergMetastoreException if getter to partition column is absent or
   *         partition column value is null
   */
  private Record getPartition(T unit, Schema schema, Map<String, MethodHandle> unitGetters) {
    Record partitionRecord = GenericRecord.create(schema);
    for (Types.NestedField column : schema.columns()) {
      String name = column.name();
      MethodHandle methodHandle = unitGetters.get(name);
      if (methodHandle == null) {
        throw new IcebergMetastoreException(
          String.format("Getter for partition key [%s::%s] must be declared in [%s] class",
            name, column.type(), unit.getClass().getSimpleName()));
      }
      Object value;
      try {
        value = methodHandle.invoke(unit);
      } catch (Throwable e) {
        throw new IcebergMetastoreException(
          String.format("Unable to invoke getter for column [%s] using [%s]", name, methodHandle), e);
      }

      if (value == null) {
        throw new IcebergMetastoreException(
          String.format("Partition key [%s::%s] value must be set", name, column.type()));
      }
      partitionRecord.setField(name, value);
    }
    return partitionRecord;
  }
}
