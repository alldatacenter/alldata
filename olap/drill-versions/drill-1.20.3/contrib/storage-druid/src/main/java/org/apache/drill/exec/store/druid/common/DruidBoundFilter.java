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
package org.apache.drill.exec.store.druid.common;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.commons.lang3.StringUtils;

@JsonDeserialize(as = DruidBoundFilter.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({ "type", "dimension", "lower", "upper", "lowerStrict", "upperStrict", "ordering" })
public class DruidBoundFilter extends DruidFilterBase {
  private final String type = DruidCompareOp.TYPE_BOUND.getCompareOp();
  private final String dimension;
  private final String lower;
  private final String upper;
  private final Boolean lowerStrict;
  private final Boolean upperStrict;
  private final String ordering;

  @JsonCreator
  public DruidBoundFilter(@JsonProperty("dimension") String dimension,
                          @JsonProperty("lower") String lower,
                          @JsonProperty("upper") String upper,
                          @JsonProperty("lowerStrict") Boolean lowerStrict,
                          @JsonProperty("upperStrict") Boolean upperStrict) {
    this.dimension = dimension;
    this.lower = lower;
    this.upper= upper;
    this.lowerStrict = lowerStrict;
    this.upperStrict = upperStrict;
    this.ordering = getCompareOrdering();
  }

  public String getType() {
    return type;
  }

  public String getDimension() {
    return dimension;
  }

  public String getLower() {
    return lower;
  }

  public String getUpper() {
    return upper;
  }

  public Boolean getLowerStrict() {
    return lowerStrict;
  }

  public Boolean getUpperStrict() {
    return upperStrict;
  }

  public String getOrdering() { return ordering; }

  @JsonIgnore
  private String getCompareOrdering() {
    if (StringUtils.isNotEmpty(lower) && StringUtils.isNumeric(lower)
        || StringUtils.isNotEmpty(upper) && StringUtils.isNumeric(upper)) {
      return "numeric";
    } else {
      return "lexicographic";
    }
  }
}
