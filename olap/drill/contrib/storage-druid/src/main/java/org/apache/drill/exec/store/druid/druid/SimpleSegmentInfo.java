/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.drill.exec.store.druid.druid;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleSegmentInfo {
  private final String maxTime;
  private final long size;
  private final String minTime;
  private final int count;

  public SimpleSegmentInfo(@JsonProperty("maxTime") String maxTime,
                           @JsonProperty("size") long size,
                           @JsonProperty("minTime") String minTime,
                           @JsonProperty("count") int count) {
    this.maxTime = maxTime;
    this.size = size;
    this.minTime = minTime;
    this.count = count;
  }

  public String getMaxTime() {
    return maxTime;
  }

  public String getMinTime() {
    return minTime;
  }

  public long getSize() {
    return size;
  }

  public int getCount() {
    return count;
  }
}
