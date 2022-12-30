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

package com.bytedance.bitsail.connector.doris.partition;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class DorisPartition implements Serializable {

  private static final String TEMP_PARTITION_PREFIX = "_bitsail_doris_temp_partition_";

  @JsonProperty(value = "name", required = true)
  private String name;

  @JsonProperty(value = "start_range", required = true)
  private List<String> startRange;

  @JsonProperty(value = "end_range", required = true)
  private List<String> endRange;

  public String getTempName() {
    return TEMP_PARTITION_PREFIX + this.name;
  }

  public String getStartRange() {
    return startRange.stream().map(range -> "\"" + range + "\"").collect(Collectors.joining(","));
  }

  public String getEndRange() {
    return endRange.stream().map(range -> "\"" + range + "\"").collect(Collectors.joining(","));
  }
}
