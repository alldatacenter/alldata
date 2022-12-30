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

package com.bytedance.bitsail.connector.legacy.streamingfile.common.tools;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.JsonSerializer;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionInfo;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionMapping;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.datasink.common.PartitionType;
import com.bytedance.bitsail.connector.legacy.streamingfile.common.option.FileSystemCommonOptions;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Created 2020/3/29.
 */
public class PartitionUtils {
  private static final int PARTITION_DAILY_SIZE = 1;
  private static final int PARTITION_HOURLY_SIZE = 2;

  public static boolean hasDynamicPartition(List<PartitionInfo> partitionInfos) {
    if (CollectionUtils.isEmpty(partitionInfos)) {
      return false;
    }
    return partitionInfos.stream()
        .anyMatch(partitionInfo -> PartitionType.DYNAMIC.equals(partitionInfo.getType()));
  }

  public static List<PartitionInfo> filterPartSpec(List<PartitionInfo> partitionInfos,
                                                   Predicate<PartitionInfo> predicate) {
    return partitionInfos.stream()
        .filter(predicate)
        .collect(Collectors.toList());
  }

  public static boolean hasHourPartition(List<PartitionInfo> partitionInfos) {
    if (CollectionUtils.isEmpty(partitionInfos)) {
      return false;
    }
    return partitionInfos.stream()
        .filter(partitionInfo -> PartitionType.TIME.equals(partitionInfo.getType()))
        .count() > PARTITION_DAILY_SIZE;
  }

  public static Map<String, PartitionInfo> getPartitionInfoMap(BitSailConfiguration jobConf) {
    List<PartitionInfo> partitionInfo = getPartitionInfo(jobConf);
    return partitionInfo.stream()
        .collect(Collectors.toMap(PartitionInfo::getName, Function.identity()));
  }

  public static List<PartitionInfo> getPartitionInfo(BitSailConfiguration jobConf) {
    String partitionInfoStr = jobConf.get(FileSystemCommonOptions.PartitionOptions.PARTITION_INFOS);
    String[] partitionFormat = new String[2];
    String dateFormat = jobConf.get(FileSystemCommonOptions.PartitionOptions.PARTITION_DATE_FORMAT);
    String hourFormat = jobConf.get(FileSystemCommonOptions.PartitionOptions.PARTITION_HOUR_FORMAT);
    partitionFormat[0] = dateFormat;
    partitionFormat[1] = hourFormat;

    List<PartitionInfo> partSpecInfos;
    if (StringUtils.isNotEmpty(partitionInfoStr)) {
      partSpecInfos = setFormatForTimePartition(JsonSerializer.parseToList(partitionInfoStr, PartitionInfo.class), partitionFormat);
    } else {
      throw new IllegalArgumentException("Parameter [partition_infos] can not be null.");
    }
    validatePartitionInfo(partSpecInfos);
    return partSpecInfos;
  }

  private static List<PartitionInfo> setFormatForTimePartition(List<PartitionInfo> partitionInfos,
                                                               String[] partitionFormat) {
    if (CollectionUtils.isNotEmpty(partitionInfos)) {
      int size = 0;
      for (int index = 0; index < partitionInfos.size() && size < PARTITION_HOURLY_SIZE; index++) {
        if (PartitionType.TIME.equals(partitionInfos.get(index).getType())
            && StringUtils.isEmpty(partitionInfos.get(index).getValue())) {
          partitionInfos.get(index).setValue(partitionFormat[size]);
          size++;
        }
      }
    }
    return partitionInfos;
  }

  public static int getHourPartitionIndex(List<PartitionInfo> partitionInfos) {
    if (CollectionUtils.size(partitionInfos) < PARTITION_HOURLY_SIZE) {
      return -1;
    }
    int indexCounter = 0;
    for (int hourIndex = 0; hourIndex < partitionInfos.size(); hourIndex++) {
      if (PartitionType.TIME.equals(partitionInfos.get(hourIndex).getType())) {
        indexCounter++;
      }
      if (PARTITION_HOURLY_SIZE == indexCounter) {
        return hourIndex;
      }
    }
    return -1;
  }

  /**
   * Validate partition rule correct.
   */
  private static void validatePartitionInfo(List<PartitionInfo> partitionInfos) {
    Preconditions.checkNotNull(partitionInfos, "Part spec can't be null.");
    for (int index = 0; index < partitionInfos.size(); index++) {
      PartitionInfo partitionInfo = partitionInfos.get(index);
      if (PartitionType.SPLIT.equals(partitionInfo.getType())) {
        Preconditions.checkArgument(index == 0,
            "Split part spec only support in first index.");
        Preconditions.checkNotNull(partitionInfo.getValue(),
            "Split part spec must has an default value.");
      }
    }
  }

  public static Map<String, PartitionMapping> getPartitionMapping(BitSailConfiguration jobConf) {
    Map<String, PartitionMapping> partSpecMapping = Maps.newHashMap();
    List<PartitionMapping> partitionMappings = JsonSerializer.parseToList(
        jobConf.get(FileSystemCommonOptions.CommitOptions.HDFS_SPLIT_PARTITION_MAPPINGS), PartitionMapping.class);
    if (CollectionUtils.isNotEmpty(partitionMappings)) {
      partSpecMapping = partitionMappings
          .stream()
          .collect(Collectors.toMap(PartitionMapping::getPartitionValue, Function.identity()));
    }
    return partSpecMapping;
  }
}
