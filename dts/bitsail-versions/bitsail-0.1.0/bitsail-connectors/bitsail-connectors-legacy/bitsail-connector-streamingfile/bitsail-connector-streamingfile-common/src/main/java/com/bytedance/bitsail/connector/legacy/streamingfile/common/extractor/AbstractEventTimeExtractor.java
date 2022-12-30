/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.legacy.streamingfile.common.extractor;

import com.bytedance.bitsail.base.metrics.MetricManager;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.FieldPathUtils;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.MetricsFactory;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.List;

/**
 * Created 2020-02-11.
 */
@SuppressWarnings("checkstyle:MagicNumber")
public abstract class AbstractEventTimeExtractor implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(AbstractEventTimeExtractor.class);

  private static final long serialVersionUID = -2273582195718185330L;
  private static final int UNIX_TIMESTAMP_LENGTH = 10;

  protected BitSailConfiguration jobConf;
  protected String pattern;
  protected boolean isEventTime;

  protected transient MetricManager metrics;
  protected transient Integer taskId;

  public AbstractEventTimeExtractor(BitSailConfiguration jobConf) {
    this.jobConf = jobConf;
    this.pattern = jobConf.get(FileSystemCommonOptions.ArchiveOptions.EVENT_TIME_PATTERN);
    this.isEventTime = jobConf.get(FileSystemCommonOptions.ArchiveOptions.ENABLE_EVENT_TIME);
  }

  protected static long timeToMs(long eventTime) {
    if (eventTime < 0) {
      throw new IllegalArgumentException("event time must be positive");
    }
    int size = String.valueOf(eventTime).length();
    if (size < UNIX_TIMESTAMP_LENGTH) {
      throw new IllegalArgumentException("event time be unix timestamps");
    } else {
      long base = 1L;
      int shift = size - UNIX_TIMESTAMP_LENGTH - 3;
      if (shift < 0) {
        for (int i = shift; i < 0; i++) {
          base *= 10;
        }
        return eventTime * base;
      } else if (shift == 0) {
        return eventTime;
      } else {
        for (int i = shift; i > 0; i--) {
          base *= 10;
        }
        return eventTime / base;
      }
    }
  }

  public boolean isBinlog() {
    return false;
  }

  protected void initialize() throws Exception {
    //empty body
  }

  public abstract Object parse(byte[] record) throws Exception;

  public String getField(Object record, FieldPathUtils.PathInfo pathInfo, String defaultValue) {
    throw new UnsupportedOperationException("Unsupported get field action.");
  }

  public long extractEventTimestamp(Object record, long defaultTimestamp) {
    try {
      long evenTime = extract(record, defaultTimestamp);
      return evenTime <= 0 ? defaultTimestamp : evenTime;
    } catch (Exception e) {
      LOG.error("extractor event time failed.", e);
      return defaultTimestamp;
    }
  }

  public long convertEventTimestamp(Object recordTimestamp, long defaultTimestamp) {
    try {
      if (recordTimestamp instanceof Number) {
        return timeToMs(((Number) recordTimestamp).longValue());
      }
      String eventTimeStr = String.valueOf(recordTimestamp);
      if (!StringUtils.isEmpty(pattern)) {
        return parsePatternEventTime(eventTimeStr, pattern, defaultTimestamp);
      }
      return timeToMs(NumberUtils.createBigDecimal(eventTimeStr)
          .longValue());
    } catch (Exception e) {
      LOG.error("convert event time failed.", e);
    }
    return defaultTimestamp;
  }

  public List<FieldPathUtils.PathInfo> getEventTimeFields() {
    throw new UnsupportedOperationException();
  }

  protected abstract long extract(Object record, long defaultTimestamp) throws Exception;

  protected long getEventTime(JsonNode node, long defaultTimestamp) {
    if (StringUtils.isEmpty(pattern)) {
      return node.asLong(defaultTimestamp);
    }
    return parsePatternEventTime(node.asText(), pattern, defaultTimestamp);
  }

  private long parsePatternEventTime(String eventTimeStr,
                                     String pattern,
                                     long defaultTimestamp) {
    try {
      DateTimeFormatter formatter = DateTimeFormat.forPattern(pattern);
      DateTime parseEventTime = formatter.parseDateTime(eventTimeStr);
      return parseEventTime.getMillis();
    } catch (Exception e) {
      LOG.error("parse event time: {} from patten: {} failed.", eventTimeStr, pattern, e);
      return defaultTimestamp;
    }
  }

  public void startContext(Integer taskId) {
    this.taskId = taskId;
    this.metrics = MetricsFactory.getInstanceMetricsManager(jobConf, taskId);
  }
}
