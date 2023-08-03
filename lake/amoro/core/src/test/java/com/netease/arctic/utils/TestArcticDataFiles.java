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

package com.netease.arctic.utils;

import com.netease.arctic.iceberg.InternalRecordWrapper;
import com.netease.arctic.iceberg.StructLikeWrapper;
import org.apache.iceberg.PartitionKey;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.Schema;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.Test;

import java.time.LocalDateTime;

public class TestArcticDataFiles {

  private static final Schema SCHEMA = new Schema(
      Types.NestedField.required(1, "dt", Types.TimestampType.withoutZone())
  );

  @Test
  public void testMonthPartition() {
    PartitionSpec spec = PartitionSpec.builderFor(SCHEMA).month("dt").build();
    PartitionKey partitionKey = new PartitionKey(spec, SCHEMA);
    GenericRecord record = GenericRecord.create(SCHEMA);
    InternalRecordWrapper internalRecordWrapper = new InternalRecordWrapper(SCHEMA.asStruct());
    partitionKey.partition(internalRecordWrapper.wrap(record.copy(
        "dt",
        LocalDateTime.parse("2022-11-11T11:00:00"))));
    String partitionPath = spec.partitionToPath(partitionKey);
    StructLike partitionData = ArcticDataFiles.data(spec, partitionPath);
    StructLikeWrapper p1 = StructLikeWrapper.forType(spec.partitionType());
    p1.set(partitionKey);
    StructLikeWrapper p2 = StructLikeWrapper.forType(spec.partitionType());
    p2.set(partitionData);
    Assert.assertEquals(p1, p2);
  }

  @Test
  public void testDaysPartition() {
    PartitionSpec spec = PartitionSpec.builderFor(SCHEMA).day("dt").build();
    PartitionKey partitionKey = new PartitionKey(spec, SCHEMA);
    GenericRecord record = GenericRecord.create(SCHEMA);
    InternalRecordWrapper internalRecordWrapper = new InternalRecordWrapper(SCHEMA.asStruct());
    partitionKey.partition(internalRecordWrapper.wrap(record.copy(
        "dt",
        LocalDateTime.parse("2022-11-11T11:00:00"))));
    String partitionPath = spec.partitionToPath(partitionKey);
    StructLike partitionData = ArcticDataFiles.data(spec, partitionPath);
    StructLikeWrapper p1 = StructLikeWrapper.forType(spec.partitionType());
    p1.set(partitionKey);
    StructLikeWrapper p2 = StructLikeWrapper.forType(spec.partitionType());
    p2.set(partitionData);
    Assert.assertEquals(p1, p2);
  }

  @Test
  public void testHoursPartition() {
    PartitionSpec spec = PartitionSpec.builderFor(SCHEMA).hour("dt").build();
    PartitionKey partitionKey = new PartitionKey(spec, SCHEMA);
    GenericRecord record = GenericRecord.create(SCHEMA);
    InternalRecordWrapper internalRecordWrapper = new InternalRecordWrapper(SCHEMA.asStruct());
    partitionKey.partition(internalRecordWrapper.wrap(record.copy(
        "dt",
        LocalDateTime.parse("2022-11-11T11:00:00"))));
    String partitionPath = spec.partitionToPath(partitionKey);
    StructLike partitionData = ArcticDataFiles.data(spec, partitionPath);
    StructLikeWrapper p1 = StructLikeWrapper.forType(spec.partitionType());
    p1.set(partitionKey);
    StructLikeWrapper p2 = StructLikeWrapper.forType(spec.partitionType());
    p2.set(partitionData);
    Assert.assertEquals(p1, p2);
  }

  @Test
  public void testBucketPartition() {
    Schema schema = new Schema(
        Types.NestedField.required(1, "dt", Types.IntegerType.get())
    );
    PartitionSpec spec = PartitionSpec.builderFor(schema).bucket("dt", 2).build();
    PartitionKey partitionKey = new PartitionKey(spec, schema);
    GenericRecord record = GenericRecord.create(schema);
    InternalRecordWrapper internalRecordWrapper = new InternalRecordWrapper(schema.asStruct());
    partitionKey.partition(internalRecordWrapper.wrap(record.copy("dt", 1)));
    String partitionPath = spec.partitionToPath(partitionKey);
    StructLike partitionData = ArcticDataFiles.data(spec, partitionPath);
    StructLikeWrapper p1 = StructLikeWrapper.forType(spec.partitionType());
    p1.set(partitionKey);
    StructLikeWrapper p2 = StructLikeWrapper.forType(spec.partitionType());
    p2.set(partitionData);
    Assert.assertEquals(p1, p2);
  }

  @Test
  public void testTruncatePartition() {
    Schema schema = new Schema(
        Types.NestedField.required(1, "dt", Types.IntegerType.get())
    );
    PartitionSpec spec = PartitionSpec.builderFor(schema).truncate("dt", 2).build();
    PartitionKey partitionKey = new PartitionKey(spec, schema);
    GenericRecord record = GenericRecord.create(schema);
    InternalRecordWrapper internalRecordWrapper = new InternalRecordWrapper(schema.asStruct());
    partitionKey.partition(internalRecordWrapper.wrap(record.copy("dt", 1)));
    String partitionPath = spec.partitionToPath(partitionKey);
    StructLike partitionData = ArcticDataFiles.data(spec, partitionPath);
    StructLikeWrapper p1 = StructLikeWrapper.forType(spec.partitionType());
    p1.set(partitionKey);
    StructLikeWrapper p2 = StructLikeWrapper.forType(spec.partitionType());
    p2.set(partitionData);
    Assert.assertEquals(p1, p2);
  }

  @Test
  public void testNullPartition() {
    Schema schema = new Schema(
        Types.NestedField.required(1, "name", Types.StringType.get())
    );
    PartitionSpec spec = PartitionSpec.builderFor(schema).identity("name").build();
    PartitionKey partitionKey = new PartitionKey(spec, schema);
    String partitionPath = "name=null";
    StructLike partitionData = ArcticDataFiles.data(spec, partitionPath);
    Assert.assertNull(partitionData.get(0, Types.StringType.get().typeId().javaClass()));
  }

  @Test
  public void testSpecialCharactersPartition() {
    Schema schema = new Schema(
        Types.NestedField.required(1, "day", Types.StringType.get()),
        Types.NestedField.required(2, "name", Types.StringType.get())
    );
    PartitionSpec spec = PartitionSpec.builderFor(schema).identity("day").identity("name").build();
    PartitionKey partitionKey = new PartitionKey(spec, schema);
    GenericRecord record = GenericRecord.create(schema);
    InternalRecordWrapper internalRecordWrapper = new InternalRecordWrapper(schema.asStruct());
    partitionKey.partition(internalRecordWrapper.wrap(record.copy("day", "2023-01-01")
        .copy("name", "AAA BBB/CCC=_*-\\%")));

    String partitionToPath = spec.partitionToPath(partitionKey);
    Assert.assertEquals("day=2023-01-01/name=AAA+BBB%2FCCC%3D_*-%5C%25", partitionToPath);
    GenericRecord partitionData = ArcticDataFiles.data(spec, partitionToPath);
    StructLikeWrapper p1 = StructLikeWrapper.forType(spec.partitionType());
    p1.set(partitionKey);
    StructLikeWrapper p2 = StructLikeWrapper.forType(spec.partitionType());
    p2.set(partitionData);

    Assert.assertEquals(p1, p2);
  }
}
