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

package com.netease.arctic.flink.metric;

import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.iceberg.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * A generator that generates the latency metrics of the writing operators in flink applications.
 */
public class MetricsGenerator implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(MetricsGenerator.class);
  private long currentLatency;
  private final boolean latencyEnable;
  private final boolean metricEnable;
  private final Schema schema;
  private final RowType flinkSchema;
  private RowData.FieldGetter modifyTimeGetter;
  private boolean findColumn = false;

  private MetricsGenerator(boolean latencyEnable,
                           Schema schema,
                           RowType flinkSchema,
                           String modifyTimeColumn,
                           boolean metricEnable) {
    this.latencyEnable = latencyEnable;
    this.schema = schema;
    this.metricEnable = metricEnable;
    this.flinkSchema = flinkSchema;
    checkColumnExist(modifyTimeColumn);
  }

  private void checkColumnExist(String modifyTimeColumn) {
    if (!this.latencyEnable) {
      return;
    }
    if (modifyTimeColumn == null || this.schema.findField(modifyTimeColumn) == null) {
      LOG.warn("can't find event time column " + modifyTimeColumn);
      findColumn = false;
    } else {
      findColumn = true;
      int modifyTimeColumnIndex = flinkSchema.getFieldIndex(modifyTimeColumn);
      LogicalType type = flinkSchema.getTypeAt(modifyTimeColumnIndex);
      LOG.info("event latency with column {}, index {}, type {}", modifyTimeColumn, modifyTimeColumnIndex, type);
      modifyTimeGetter = RowData.createFieldGetter(type, modifyTimeColumnIndex);
    }
  }

  public static MetricsGenerator empty(boolean metricEnable) {
    return new MetricsGenerator(false, null, null, null, metricEnable);
  }

  public static MetricsGenerator newGenerator(Schema schema,
                                              RowType flinkSchema,
                                              String modifyTimeColumn,
                                              boolean metricEnable) {
    return new MetricsGenerator(true, schema, flinkSchema, modifyTimeColumn, metricEnable);
  }

  public boolean enable() {
    return latencyEnable;
  }


  public boolean isMetricEnable() {
    return metricEnable;
  }

  public void recordLatency(StreamRecord<RowData> element) {
    if (latencyEnable) {
      if (findColumn) {
        RowData rowData = element.getValue();
        if (rowData.getRowKind() == RowKind.UPDATE_BEFORE || rowData.getRowKind() == RowKind.DELETE) {
          return;
        }

        Object value = modifyTimeGetter.getFieldOrNull(rowData);
        if (value == null) {
          return;
        }
        if (value instanceof LocalDateTime) {
          LocalDateTime localDateTime = (LocalDateTime) value;
          long eventTime =
              localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
          this.currentLatency = System.currentTimeMillis() - eventTime;
        } else if (value instanceof Long) {
          this.currentLatency = System.currentTimeMillis() - (Long) value;
        } else {
          LOG.warn("eventTimeColumn is not LocalDateTime/Long, " + value.getClass());
        }
      } else if (element.hasTimestamp()) {
        this.currentLatency = System.currentTimeMillis() - element.getTimestamp();
      }
    }
  }

  public long getCurrentLatency() {
    return currentLatency;
  }

}
