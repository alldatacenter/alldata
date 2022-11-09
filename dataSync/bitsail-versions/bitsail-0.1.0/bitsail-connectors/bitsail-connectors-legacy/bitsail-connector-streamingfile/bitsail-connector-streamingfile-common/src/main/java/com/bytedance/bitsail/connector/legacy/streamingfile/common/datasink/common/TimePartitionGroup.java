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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common;

import com.bytedance.bitsail.connector.legacy.streamingfile.common.tools.PartitionUtils;

import lombok.Getter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created 2020/9/15.
 */
public class TimePartitionGroup implements Serializable {
  private static final Logger LOGGER =
      LoggerFactory.getLogger(TimePartitionGroup.class);

  @Getter
  private PartitionInfo dayPartSpec;

  @Getter
  private PartitionInfo hourPartSpec;

  private transient org.joda.time.format.DateTimeFormatter formatter;

  public TimePartitionGroup(List<PartitionInfo> partitionInfos) {
    if (CollectionUtils.isNotEmpty(partitionInfos)) {
      List<PartitionInfo> filtered = partitionInfos.stream()
          .filter(partSpec -> PartitionType.TIME.equals(partSpec.getType()))
          .collect(Collectors.toList());

      if (PartitionUtils.hasHourPartition(partitionInfos)) {
        hourPartSpec = filtered.get(1);
      }
      dayPartSpec = filtered.get(0);
    }
  }

  public static DateTimeFormatter getOrCreateFormatter(PartitionInfo partitionInfo) {
    if (PartitionType.TIME.equals(partitionInfo.getType())
        && Objects.isNull(partitionInfo.getFormatter())) {
      partitionInfo.createFormatter();
    }
    return partitionInfo.getFormatter();
  }

  private org.joda.time.format.DateTimeFormatter getOrCreateFormatter() {
    if (Objects.nonNull(formatter)) {
      return formatter;
    }

    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder()
        .appendPattern(dayPartSpec.getValue());
    if (Objects.nonNull(hourPartSpec)) {
      builder.appendPattern(hourPartSpec.getValue());
    }

    formatter = builder.toFormatter();
    return formatter;
  }

  public long getCommitTimeFromPartSpecs(LinkedHashMap<String, String> partSpecs, long jobMinTimestamp) {
    StringBuilder builder = new StringBuilder();

    if (Objects.nonNull(dayPartSpec) && partSpecs.containsKey(dayPartSpec.getName())) {
      builder.append(partSpecs.get(dayPartSpec.getName()));
    }

    if (Objects.nonNull(hourPartSpec) && partSpecs.containsKey(hourPartSpec.getName())) {
      builder.append(partSpecs.get(hourPartSpec.getName()));
    }

    String newCommitTimeStr = builder.toString();
    if (StringUtils.isEmpty(newCommitTimeStr)) {
      return jobMinTimestamp;
    }

    try {

      DateTime dateTime = getOrCreateFormatter()
          .parseDateTime(newCommitTimeStr);

      return dateTime.toInstant().getMillis();
    } catch (Exception e) {
      LOGGER.error("Failed to parse commit time str {}.", newCommitTimeStr, e);
    }
    return jobMinTimestamp;
  }

  public DateTimeFormatter getMatchedFormatter(String name) {
    if (dayPartSpec.getName().equals(name)) {
      return getOrCreateFormatter(dayPartSpec);
    }
    if (Objects.nonNull(name) && hourPartSpec.getName().equals(name)) {
      return getOrCreateFormatter(hourPartSpec);
    }
    return null;
  }
}
