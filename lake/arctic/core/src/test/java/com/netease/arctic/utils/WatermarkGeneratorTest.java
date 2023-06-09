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

import com.netease.arctic.TableTestHelpers;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.catalog.TableTestBase;
import com.netease.arctic.table.TableProperties;
import com.netease.arctic.table.WatermarkGenerator;
import org.apache.iceberg.DataFile;
import org.apache.iceberg.Metrics;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.types.Conversions;
import org.apache.iceberg.types.Types;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

public class WatermarkGeneratorTest extends TableTestBase {

  public WatermarkGeneratorTest() {
    super(TableFormat.MIXED_ICEBERG, false, false);
  }

  @Test
  public void testDefaultEventTime() {
    long start = System.currentTimeMillis();
    WatermarkGenerator watermarkGenerator = WatermarkGenerator.forTable(getArcticTable());
    Assert.assertEquals(-1, watermarkGenerator.watermark());
    watermarkGenerator.addFile(TableTestHelpers.getFile(1));
    Assert.assertTrue(watermarkGenerator.watermark() >= start);
  }

  @Test
  public void testTimestampEventTime() {
    long start = System.currentTimeMillis();
    getArcticTable().asUnkeyedTable().updateProperties().set(TableProperties.TABLE_EVENT_TIME_FIELD, "op_time")
        .set(TableProperties.TABLE_WATERMARK_ALLOWED_LATENESS, "10").commit();
    WatermarkGenerator watermarkGenerator = WatermarkGenerator.forTable(getArcticTable());

    Map<Integer, ByteBuffer> lowerBounds = Maps.newHashMap();
    Map<Integer, ByteBuffer> upperBounds = Maps.newHashMap();
    lowerBounds.put(4, Conversions.toByteBuffer(Types.TimestampType.withoutZone(), start - 30000));
    upperBounds.put(4, Conversions.toByteBuffer(Types.TimestampType.withoutZone(), start - 10000));

    Metrics metrics = new Metrics(2L, Maps.newHashMap(), Maps.newHashMap(),
        Maps.newHashMap(), null, lowerBounds, upperBounds);

    DataFile file1 = TableTestHelpers.getFile("/watermark", 1,
        PartitionSpec.unpartitioned(), null, metrics, false);

    watermarkGenerator.addFile(file1);
    Assert.assertEquals(start - 20000, watermarkGenerator.watermark());

    lowerBounds.put(4, Conversions.toByteBuffer(Types.TimestampType.withoutZone(), start));
    upperBounds.put(4, Conversions.toByteBuffer(Types.TimestampType.withoutZone(), start));
    metrics = new Metrics(2L, Maps.newHashMap(), Maps.newHashMap(),
        Maps.newHashMap(), null, lowerBounds, upperBounds);

    DataFile file2 = TableTestHelpers.getFile("/watermark", 2,
        PartitionSpec.unpartitioned(), null, metrics, false);
    watermarkGenerator.addFile(file2);
    Assert.assertEquals(start - 10000, watermarkGenerator.watermark());
  }

  @Test
  public void testLongEventTime() {
    long start = System.currentTimeMillis();
    getArcticTable().asUnkeyedTable().updateProperties().set(TableProperties.TABLE_EVENT_TIME_FIELD, "ts")
        .set(TableProperties.TABLE_WATERMARK_ALLOWED_LATENESS, "5")
        .set(TableProperties.TABLE_EVENT_TIME_NUMBER_FORMAT, "TIMESTAMP_S").commit();
    WatermarkGenerator watermarkGenerator = WatermarkGenerator.forTable(getArcticTable());

    Map<Integer, ByteBuffer> lowerBounds = Maps.newHashMap();
    Map<Integer, ByteBuffer> upperBounds = Maps.newHashMap();
    lowerBounds.put(3, Conversions.toByteBuffer(Types.LongType.get(), start / 1000 - 30));
    upperBounds.put(3, Conversions.toByteBuffer(Types.LongType.get(), start / 1000 - 10));

    Metrics metrics = new Metrics(2L, Maps.newHashMap(), Maps.newHashMap(),
        Maps.newHashMap(), null, lowerBounds, upperBounds);

    DataFile file1 = TableTestHelpers.getFile("/watermark", 1,
        PartitionSpec.unpartitioned(), null, metrics, false);
    watermarkGenerator.addFile(file1);
    Assert.assertEquals((start / 1000 * 1000) - 15000, watermarkGenerator.watermark());

    lowerBounds.put(3, Conversions.toByteBuffer(Types.LongType.get(), start / 1000));
    upperBounds.put(3, Conversions.toByteBuffer(Types.LongType.get(), start / 1000));
    metrics = new Metrics(2L, Maps.newHashMap(), Maps.newHashMap(),
        Maps.newHashMap(), null, lowerBounds, upperBounds);

    DataFile file2 = TableTestHelpers.getFile("/watermark", 2,
        PartitionSpec.unpartitioned(), null, metrics, false);
    watermarkGenerator.addFile(file2);
    Assert.assertEquals((start / 1000 * 1000) - 5000, watermarkGenerator.watermark());
  }

  @Test
  public void testStringEventTime() throws ParseException {
    getArcticTable().asUnkeyedTable().updateProperties().set(TableProperties.TABLE_EVENT_TIME_FIELD, "name")
        .set(TableProperties.TABLE_WATERMARK_ALLOWED_LATENESS, "1").commit();
    WatermarkGenerator watermarkGenerator = WatermarkGenerator.forTable(getArcticTable());

    Map<Integer, ByteBuffer> lowerBounds = Maps.newHashMap();
    Map<Integer, ByteBuffer> upperBounds = Maps.newHashMap();
    lowerBounds.put(2, Conversions.toByteBuffer(Types.StringType.get(), "2022-11-11 00:00:00"));
    upperBounds.put(2, Conversions.toByteBuffer(Types.StringType.get(), "2022-11-11 00:01:00"));

    DateFormat df = new SimpleDateFormat(TableProperties.TABLE_EVENT_TIME_STRING_FORMAT_DEFAULT);

    Metrics metrics = new Metrics(2L, Maps.newHashMap(), Maps.newHashMap(),
        Maps.newHashMap(), null, lowerBounds, upperBounds);

    DataFile file1 = TableTestHelpers.getFile("/watermark", 1,
        PartitionSpec.unpartitioned(), null, metrics, false);
    watermarkGenerator.addFile(file1);
    Assert.assertEquals(df.parse("2022-11-11 00:00:59").getTime(), watermarkGenerator.watermark());

    lowerBounds.put(2, Conversions.toByteBuffer(Types.StringType.get(), "2022-11-11 00:00:00"));
    upperBounds.put(2, Conversions.toByteBuffer(Types.StringType.get(), "2022-11-11 00:02:00"));
    metrics = new Metrics(2L, Maps.newHashMap(), Maps.newHashMap(),
        Maps.newHashMap(), null, lowerBounds, upperBounds);

    DataFile file2 = TableTestHelpers.getFile("/watermark", 2,
        PartitionSpec.unpartitioned(), null, metrics, false);
    watermarkGenerator.addFile(file2);
    Assert.assertEquals(df.parse("2022-11-11 00:01:59").getTime(), watermarkGenerator.watermark());
  }

  @Test
  public void testWithWrongConfigs() {
    getArcticTable().asUnkeyedTable().updateProperties().set(TableProperties.TABLE_EVENT_TIME_FIELD, "name")
        .set(TableProperties.TABLE_WATERMARK_ALLOWED_LATENESS, "1").commit();
    WatermarkGenerator watermarkGenerator = WatermarkGenerator.forTable(getArcticTable());

    Map<Integer, ByteBuffer> lowerBounds = Maps.newHashMap();
    Map<Integer, ByteBuffer> upperBounds = Maps.newHashMap();
    lowerBounds.put(2, Conversions.toByteBuffer(Types.StringType.get(), "2022-11-11"));
    upperBounds.put(2, Conversions.toByteBuffer(Types.StringType.get(), "2022-11-11"));

    Metrics metrics = new Metrics(2L, Maps.newHashMap(), Maps.newHashMap(),
        Maps.newHashMap(), null, lowerBounds, upperBounds);

    DataFile file1 = TableTestHelpers.getFile("/watermark", 1,
        PartitionSpec.unpartitioned(), null, metrics, false);
    watermarkGenerator.addFile(file1);
    Assert.assertEquals(-1, watermarkGenerator.watermark());
  }
}
