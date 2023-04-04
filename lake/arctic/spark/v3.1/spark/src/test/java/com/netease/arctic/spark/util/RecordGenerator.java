/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.util;

import com.google.common.collect.Maps;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.types.Type;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.RandomUtil;

import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RecordGenerator {

  private final Schema schema;

  private final ArrayList<Types.NestedField> columns;
  private final Map<String, ValueGenerator> fieldGenerator = Maps.newHashMap();

  public RecordGenerator(Schema schema, Map<String, ValueGenerator> fieldGenerator, long seed) {
    this.schema = schema;
    this.columns = Lists.newArrayList(schema.columns());
    for (Types.NestedField f : columns) {
      ValueGenerator generator = fieldGenerator.get(f.name());
      if (generator == null) {
        generator = new RandomValueGenerator(seed);
      }
      this.fieldGenerator.put(f.name(), generator);
    }
  }

  public GenericRecord newRecord() {
    GenericRecord record = GenericRecord.create(schema);
    for (int i = 0; i < columns.size(); i++) {
      Types.NestedField f = columns.get(i);
      ValueGenerator generator = fieldGenerator.get(f.name());
      record.set(i, generator.get(f.type()));
    }
    return record;
  }

  public List<GenericRecord> records(int size) {
    return IntStream.range(0, size).boxed()
        .map(x -> this.newRecord())
        .collect(Collectors.toList());
  }

  public static Builder buildFor(Schema schema) {
    return new Builder(schema);
  }

  public static class Builder {
    final Schema schema;
    final Map<String, ValueGenerator> valueGeneratorMap = Maps.newHashMap();

    long seed = 0;

    protected Builder(Schema schema) {
      this.schema = schema;
    }

    public Builder withSequencePrimaryKey(PrimaryKeySpec keySpec) {
      for (String col : keySpec.fieldNames()) {
        Types.NestedField f = schema.findField(col);
        switch (f.type().typeId()) {
          case LONG:
          case INTEGER:
            valueGeneratorMap.put(f.name(), new SequenceValueGenerator());
        }
      }
      return this;
    }

    public Builder withRandomDate(String cols) {
      valueGeneratorMap.put(cols, new RandomDateString(this.seed));
      return this;
    }

    public Builder withSeed(long seed) {
      this.seed = seed;
      return this;
    }

    public RecordGenerator build() {
      return new RecordGenerator(schema, valueGeneratorMap, this.seed);
    }
  }

  public interface ValueGenerator {
    Object get(Type type);
  }

  static class RandomValueGenerator implements ValueGenerator {
    private static final OffsetDateTime EPOCH = Instant.ofEpochSecond(0).atOffset(ZoneOffset.UTC);

    private final Random random;

    public RandomValueGenerator(long seed) {
      this.random = new Random(seed);
    }

    @Override
    public Object get(Type type) {
      Object value = RandomUtil.generatePrimitive(type.asPrimitiveType(), random);

      switch (type.typeId()) {
        case TIME:
          return LocalTime.ofNanoOfDay((long) value * 1000);
        case TIMESTAMP:
          if (((Types.TimestampType) type).shouldAdjustToUTC()) {
            return EPOCH.plus((long) value, ChronoUnit.MICROS);
          } else {
            return EPOCH.plus((long) value, ChronoUnit.MICROS).toLocalDateTime();
          }
        case DATE:
          return EPOCH.plus((int) value, ChronoUnit.SECONDS).toLocalDate();
      }
      return value;
    }
  }

  static class SequenceValueGenerator extends RandomValueGenerator {
    AtomicLong value = new AtomicLong(0);

    public SequenceValueGenerator() {
      super(0);
    }

    @Override
    public Object get(Type type) {

      switch (type.typeId()) {
        case LONG:
          return value.incrementAndGet();
        case INTEGER:
          return (int) value.incrementAndGet();
      }
      return super.get(type);
    }
  }

  static class RandomDateString extends RandomValueGenerator {

    public RandomDateString(long seed) {
      super(seed);
    }

    @Override
    public Object get(Type type) {
      return super.get(Types.DateType.get()).toString();
    }
  }
}
