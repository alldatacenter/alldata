/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.optimizing.flow;

import org.apache.iceberg.PartitionField;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RandomRecordGenerator {

  private final Random random;

  private final Schema primary;

  private final Set<Integer> primaryIds = new HashSet<>();

  private List<Map<Integer, Object>> partitionValues;

  private final Map<Integer, Map<Integer, Object>> primaryRelationWithPartition;

  private final Set<Integer> partitionIds = new HashSet<>();

  private final Schema schema;

  public RandomRecordGenerator(
      Schema schema,
      PartitionSpec spec,
      Schema primary,
      int partitionCount,
      @Nullable Map<Integer, Map<Integer, Object>> primaryRelationWithPartition,
      Long seed) {
    this.schema = schema;
    this.primary = primary;
    if (this.primary != null) {
      for (Types.NestedField field : primary.columns()) {
        Preconditions.checkState(field.type().typeId() == Type.TypeID.INTEGER, "primary key must be int type");
        primaryIds.add(field.fieldId());
      }
    }

    this.random = seed == null ? new Random() : new Random(seed);

    if (primaryRelationWithPartition != null && !primaryRelationWithPartition.isEmpty()) {
      this.primaryRelationWithPartition = primaryRelationWithPartition;
      this.partitionValues = primaryRelationWithPartition
          .values().stream().distinct().collect(Collectors.toList());
    } else {
      this.primaryRelationWithPartition = new HashMap<>();
      if (!spec.isUnpartitioned()) {
        partitionValues = new ArrayList<>();
        for (int i = 0; i < partitionCount; i++) {
          Map<Integer, Object> map = new HashMap<>();
          for (PartitionField field : spec.fields()) {
            partitionIds.add(field.sourceId());
            map.put(field.sourceId(), generateObject(schema.findType(field.sourceId())));
          }
          partitionValues.add(map);
        }
      }
    }
  }

  public List<Record> range(int lowerBound, int upperBound) {
    Preconditions.checkNotNull(primary, "This method is only for primary table");
    Preconditions.checkState(lowerBound <= upperBound);
    List<Record> list = new ArrayList<>();
    for (int i = lowerBound; i <= upperBound; i++) {
      list.add(randomRecord(i));
    }
    return list;
  }

  public List<Record> scatter(int[] primaries) {
    Preconditions.checkNotNull(primary, "This method is only for primary table");
    Preconditions.checkNotNull(primaries);
    List<Record> list = new ArrayList<>();
    for (int j : primaries) {
      list.add(randomRecord(j));
    }
    return list;
  }

  public Record randomRecord(int primaryValue) {
    Record record = GenericRecord.create(schema);
    Random random = new Random();
    List<Types.NestedField> columns = schema.columns();
    Map<Integer, Object> partitionValue = null;
    if (partitionValues != null) {
      partitionValue =
          primaryRelationWithPartition.computeIfAbsent(
              primaryValue,
              p -> partitionValues.get(random.nextInt(partitionValues.size())));
    }
    for (int i = 0; i < columns.size(); i++) {
      Types.NestedField field = columns.get(i);

      if (primaryIds.contains(field.fieldId())) {
        record.set(i, primaryValue);
        continue;
      }

      if (partitionIds.contains(field.fieldId())) {
        record.set(i, partitionValue.get(field.fieldId()));
        continue;
      }

      record.set(i, generateObject(field.type()));
    }
    return record;
  }

  public Record randomRecord() {
    Record record = GenericRecord.create(schema);
    Random random = new Random();
    List<Types.NestedField> columns = schema.columns();
    Map<Integer, Object> partitionValue = null;
    if (partitionValues != null) {
      partitionValue = partitionValues.get(random.nextInt(partitionValues.size()));
    }
    for (int i = 0; i < columns.size(); i++) {
      Types.NestedField field = columns.get(i);
      if (partitionIds.contains(field.fieldId())) {
        record.set(i, partitionValue.get(field.fieldId()));
        continue;
      }
      record.set(i, generateObject(field.type()));
    }
    return record;
  }

  private Object generateObject(Type type) {
    switch (type.typeId()) {
      case INTEGER:
        return random.nextInt();
      case STRING:
        return UUID.randomUUID().toString();
      case LONG:
        return random.nextLong();
      case FLOAT:
        return random.nextFloat();
      case DOUBLE:
        return random.nextDouble();
      case BOOLEAN:
        return random.nextBoolean();
      case DATE:
        return LocalDate.now().minusDays(random.nextInt(10000));
      case TIMESTAMP:
        Types.TimestampType timestampType = (Types.TimestampType) type;
        if (timestampType.shouldAdjustToUTC()) {
          return OffsetDateTime.now().minusDays(random.nextInt(10000));
        } else {
          return LocalDateTime.now().minusDays(random.nextInt(10000));
        }
      case DECIMAL:
        Types.DecimalType decimalType = (Types.DecimalType) type;
        return BigDecimal.valueOf(Double.valueOf(random.nextDouble()).longValue(), decimalType.scale());
      default:
        throw new RuntimeException("Unsupported type you can add them in code");
    }
  }
}
