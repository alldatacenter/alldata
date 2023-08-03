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

package com.netease.arctic.table;

import org.apache.iceberg.ContentFile;
import org.apache.iceberg.types.Conversions;
import org.apache.iceberg.types.Types;
import org.apache.iceberg.util.PropertyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;

public class WatermarkGenerator {

  private static final Logger LOG = LoggerFactory.getLogger(WatermarkGenerator.class);

  public static final String INGEST_TIME = "_ingest_time";
  public static final String EVENT_TIME_TIMESTAMP_MS = "TIMESTAMP_MS";
  public static final String EVENT_TIME_TIMESTAMP_S = "TIMESTAMP_S";

  private static final Types.NestedField INGEST_TIME_FIELD = Types.NestedField.required(Integer.MAX_VALUE - 2000,
      INGEST_TIME, Types.LongType.get(), "virtual ingest time field ");

  private long watermark = -1;                            // timestamp in milliseconds
  private final Types.NestedField eventTimeField;
  private final long lateness;                       // in milliseconds
  private final SimpleDateFormat eventTimeStringFormat;
  private final String eventTimeNumberFormat;

  public static WatermarkGenerator forTable(ArcticTable table) {
    Types.NestedField eventTimeField = INGEST_TIME_FIELD;
    String eventTimeName = table.properties().getOrDefault(TableProperties.TABLE_EVENT_TIME_FIELD,
        TableProperties.TABLE_EVENT_TIME_FIELD_DEFAULT);
    if (!eventTimeName.equals(INGEST_TIME)) {
      eventTimeField = table.schema().findField(eventTimeName);
      if (eventTimeField == null) {
        throw new IllegalStateException("Unknown event time field " + eventTimeName + " in table " + table.id());
      }
    }
    long lateness = PropertyUtil.propertyAsLong(table.properties(), TableProperties.TABLE_WATERMARK_ALLOWED_LATENESS,
        TableProperties.TABLE_WATERMARK_ALLOWED_LATENESS_DEFAULT) * 1000;
    return new WatermarkGenerator(eventTimeField, lateness,
        table.properties().getOrDefault(TableProperties.TABLE_EVENT_TIME_STRING_FORMAT,
            TableProperties.TABLE_EVENT_TIME_STRING_FORMAT_DEFAULT),
        table.properties().getOrDefault(TableProperties.TABLE_EVENT_TIME_NUMBER_FORMAT,
            TableProperties.TABLE_EVENT_TIME_NUMBER_FORMAT_DEFAULT));
  }

  private WatermarkGenerator(Types.NestedField eventTimeField, long lateness, String eventTimeStringFormat,
      String eventTimeNumberFormat) {
    this.eventTimeField = eventTimeField;
    this.lateness = lateness;
    this.eventTimeStringFormat = new SimpleDateFormat(eventTimeStringFormat);
    this.eventTimeNumberFormat = eventTimeNumberFormat;
  }

  public long watermark() {
    return watermark;
  }

  public <F> void addFile(ContentFile<F> file) {
    try {
      Map<Integer, ByteBuffer> upperBounds = file.upperBounds();
      if (upperBounds != null && upperBounds.containsKey(eventTimeField.fieldId())) {
        ByteBuffer byteBuffer = upperBounds.get(eventTimeField.fieldId());
        long maxEventTime = -1L;
        switch (eventTimeField.type().typeId()) {
          case INTEGER:
            Integer eventTimeIntegerValue = Conversions.fromByteBuffer(eventTimeField.type(), byteBuffer);
            maxEventTime = eventTimeFromInteger(eventTimeIntegerValue);
            break;
          case LONG:
            Long eventTimeLongValue = Conversions.fromByteBuffer(eventTimeField.type(), byteBuffer);
            maxEventTime = eventTimeFromLong(eventTimeLongValue);
            break;
          case DECIMAL:
            BigDecimal eventTimeDecimalValue = Conversions.fromByteBuffer(eventTimeField.type(), byteBuffer);
            maxEventTime = eventTimeFromDecimal(eventTimeDecimalValue);
            break;
          case TIMESTAMP:
            Long eventTimeTimestampValue = Conversions.fromByteBuffer(eventTimeField.type(), byteBuffer);
            maxEventTime = eventTimeFromTimestamp(eventTimeTimestampValue);
            break;
          case STRING:
            String eventTimeStringValue = Conversions.fromByteBuffer(eventTimeField.type(), byteBuffer).toString();
            maxEventTime = eventTimeFromString(eventTimeStringValue);
            break;
          default:
            throw new UnsupportedOperationException("Unsupported event time type:" + eventTimeField.type());
        }
        long watermark = maxEventTime - lateness;
        this.watermark = Math.max(watermark, this.watermark);
      } else if (eventTimeField.equals(INGEST_TIME_FIELD)) {
        long watermark = System.currentTimeMillis() - lateness;
        this.watermark = Math.max(watermark, this.watermark);
      }
    } catch (Exception e) {
      LOG.warn("Failed to calculate watermark from date file", e);
    }
  }

  private long eventTimeFromInteger(Integer integerValue) {
    if (eventTimeNumberFormat.equals(EVENT_TIME_TIMESTAMP_S)) {
      return integerValue * 1000;
    } else {
      throw new IllegalArgumentException("Illegal datetime number format " + eventTimeNumberFormat + " for int type");
    }
  }

  private long eventTimeFromLong(Long longValue) {
    if (eventTimeNumberFormat.equals(EVENT_TIME_TIMESTAMP_S)) {
      return longValue * 1000;
    } else if (eventTimeNumberFormat.equals(EVENT_TIME_TIMESTAMP_MS)) {
      return longValue;
    } else {
      throw new IllegalArgumentException("Illegal datetime number format " + eventTimeNumberFormat + " for long type");
    }
  }

  private long eventTimeFromDecimal(BigDecimal decimalValue) {
    if (eventTimeNumberFormat.equals(EVENT_TIME_TIMESTAMP_S)) {
      return decimalValue.longValue() * 1000;
    } else if (eventTimeNumberFormat.equals(EVENT_TIME_TIMESTAMP_MS)) {
      return decimalValue.longValue();
    } else {
      throw new IllegalArgumentException("Illegal datetime number format " + eventTimeNumberFormat + " for decimal " +
          "type");
    }
  }

  private long eventTimeFromTimestamp(Long longValue) {
    return longValue;
  }

  private long eventTimeFromString(String stringValue) {
    try {
      return eventTimeStringFormat.parse(stringValue).getTime();
    } catch (ParseException e) {
      throw new RuntimeException("Fail to parse event time for string value:" + stringValue +
          ", with format: " + eventTimeStringFormat.toPattern());
    }
  }
}
