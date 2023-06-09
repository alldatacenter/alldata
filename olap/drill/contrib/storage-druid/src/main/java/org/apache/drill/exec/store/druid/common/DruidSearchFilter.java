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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(as = DruidSearchFilter.class)
@JsonPropertyOrder({ "type", "dimension", "query" })
public class DruidSearchFilter extends DruidFilterBase {

  private final String type = DruidCompareOp.TYPE_SEARCH.getCompareOp();
  private final String dimension;
  private final DruidSearchQuerySpec query;

  @JsonCreator
  public DruidSearchFilter(@JsonProperty("dimension") String dimension,
                            @JsonProperty("query") DruidSearchQuerySpec query) {
    this.dimension = dimension;
    this.query = query;
  }

  public String getType() {
    return type;
  }

  public String getDimension() {
    return dimension;
  }

  public DruidSearchQuerySpec getQuery() { return query; }
}
