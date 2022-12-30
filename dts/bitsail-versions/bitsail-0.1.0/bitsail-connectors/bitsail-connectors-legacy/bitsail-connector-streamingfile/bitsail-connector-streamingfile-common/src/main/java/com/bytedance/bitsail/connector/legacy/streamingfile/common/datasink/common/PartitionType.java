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

import com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created 2020/3/30.
 */
public enum PartitionType implements Serializable {

  /**
   * static partition value, such as app = 1
   */
  STATIC,

  /**
   * Time partition value.
   */
  TIME,

  /**
   * dynamic partition value which from row column.
   */
  DYNAMIC,

  /**
   * Same as {@link  PartitionType#DYNAMIC} in write phrase,
   * but different in rename phrase, because partition type split will not exists
   * in the target path.
   */
  SPLIT;

  private static final Map<String, PartitionType> NAME_MAP = Arrays
      .stream(PartitionType.values())
      .collect(Collectors.toMap(partitionType -> partitionType.name().toLowerCase(),
          partitionType -> partitionType));

  /**
   * A static deserialization method.
   *
   * @param name of partition type
   * @return deserialized {@link PartitionType}
   */
  @JsonCreator
  private static PartitionType fromName(String name) {
    return NAME_MAP.get(StringUtils.lowerCase(name));
  }
}
