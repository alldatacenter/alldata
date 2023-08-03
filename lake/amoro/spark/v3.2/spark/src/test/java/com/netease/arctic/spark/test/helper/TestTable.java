/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.spark.test.helper;

import com.clearspring.analytics.util.Lists;
import com.netease.arctic.ams.api.TableFormat;
import com.netease.arctic.hive.utils.HiveSchemaUtil;
import com.netease.arctic.table.PrimaryKeySpec;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.types.Types;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TestTable {

  public final Schema schema;
  public final PrimaryKeySpec keySpec;
  public final PartitionSpec ptSpec;
  public final List<FieldSchema> hiveSchema;
  public final List<FieldSchema> hivePartitions;

  public final TableFormat format;
  private final RecordGenerator.Builder dataGenBuilder;

  public TestTable(
      TableFormat format,
      Schema schema, PrimaryKeySpec keySpec, PartitionSpec ptSpec,
      List<FieldSchema> hiveSchema, List<FieldSchema> hivePartitions,
      RecordGenerator.Builder dataGenBuilder) {
    this.format = format;
    this.schema = schema;
    this.keySpec = keySpec;
    this.ptSpec = ptSpec;
    this.hiveSchema = hiveSchema;
    this.hivePartitions = hivePartitions;
    this.dataGenBuilder = dataGenBuilder;
  }

  public RecordGenerator newDateGen() {
    return dataGenBuilder.build();
  }

  public static Builder format(TableFormat format, Types.NestedField... fields) {
    return new Builder(format, fields);
  }

  public static class Builder {

    final TableFormat format;

    private Schema schema;
    private PrimaryKeySpec keySpec = PrimaryKeySpec.noPrimaryKey();
    private PartitionSpec ptSpec = PartitionSpec.unpartitioned();
    private final RecordGenerator.Builder datagenBuilder;

    public Builder(TableFormat format, Types.NestedField... fields) {
      this.format = format;
      AtomicInteger id = new AtomicInteger(1);
      List<Types.NestedField> cols = Arrays.stream(fields).map(f -> Types.NestedField.of(
          id.getAndIncrement(),
          f.isOptional(), f.name(),
          f.type(),
          f.doc())
      ).collect(Collectors.toList());
      this.schema = new Schema(cols);
      this.datagenBuilder = RecordGenerator.buildFor(schema);
    }

    public Builder pk(String... columns) {
      Set<String> pks = Sets.newHashSet(columns);
      List<Types.NestedField> fields = this.schema.columns().stream().map(
          f -> {
            if (pks.contains(f.name())) {
              return f.asRequired();
            } else {
              return f;
            }
          }
      ).collect(Collectors.toList());
      this.schema = new Schema(fields);
      PrimaryKeySpec.Builder builder = PrimaryKeySpec.builderFor(this.schema);
      Arrays.stream(columns).forEach(builder::addColumn);
      this.keySpec = builder.build();
      return this;
    }


    public Builder timestampWithoutZoneInCreateTable() {
      List<Types.NestedField> fields = this.schema.columns().stream().map(
          f -> {
            if (f.type().equals(Types.TimestampType.withZone())) {
              return Types.NestedField.of(
                  f.fieldId(), f.isOptional(), f.name(), Types.TimestampType.withoutZone(), f.doc());
            } else {
              return f;
            }
          }
      ).collect(Collectors.toList());
      this.schema = new Schema(fields);
      return this;
    }

    public Builder pt(String... cols) {
      PartitionSpec.Builder builder = PartitionSpec.builderFor(this.schema);
      Arrays.stream(cols).forEach(builder::identity);
      this.ptSpec = builder.build();
      return this;
    }

    public Builder transformPt(Function<PartitionSpec.Builder, PartitionSpec.Builder> partitionTransform) {
      PartitionSpec.Builder builder = PartitionSpec.builderFor(this.schema);
      builder = partitionTransform.apply(builder);
      this.ptSpec = builder.build();
      return this;
    }

    public TestTable build() {
      List<FieldSchema> hiveSchemas = Lists.newArrayList();
      List<FieldSchema> hivePartition = Lists.newArrayList();
      if (TableFormat.MIXED_HIVE.equals(format)) {
        this.timestampWithoutZoneInCreateTable();
        hiveSchemas = HiveSchemaUtil.hiveTableFields(this.schema, this.ptSpec);
        hivePartition = HiveSchemaUtil.hivePartitionFields(this.schema, this.ptSpec);
      }

      RecordGenerator.Builder builder =  RecordGenerator.buildFor(this.schema)
          .withSequencePrimaryKey(keySpec);
      return new TestTable(
          format, this.schema, this.keySpec, this.ptSpec,
          hiveSchemas, hivePartition, builder
      );
    }
  }




}
