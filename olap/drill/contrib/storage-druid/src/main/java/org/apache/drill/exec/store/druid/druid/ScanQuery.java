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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigInteger;
import java.util.List;

@JsonPropertyOrder({ "queryType", "dataSource", "descending", "dimensions", "filter", "granularity", "intervals", "pagingSpec" })
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanQuery {

  @JsonProperty
  private static final String granularity = "all";

  @JsonProperty
  private static final String queryType = "scan";

  @JsonProperty
  private static final boolean descending = false;

  private final String dataSource;
  private final List<String> columns;
  private final ObjectNode filter;
  private final List<String> intervals;
  private final BigInteger offset;
  private final int limit;

  public ScanQuery(@JsonProperty("dataSource") String dataSource,
                   @JsonProperty("columns") List<String> columns,
                   @JsonProperty("filter") ObjectNode filter,
                   @JsonProperty("intervals") List<String> intervals,
                   @JsonProperty("offset") BigInteger offset,
                   @JsonProperty("limit") int limit) {
    this.dataSource = dataSource;
    this.columns = columns;
    this.filter = filter;
    this.intervals = intervals;
    this.offset = offset;
    this.limit = limit;
  }

  public String getQueryType() {
    return queryType;
  }

  public boolean isDescending() {
    return descending;
  }

  public String getDataSource() {
    return dataSource;
  }

  public List<String> getColumns() {
    return columns;
  }

  public String getGranularity() {
    return granularity;
  }

  public List<String> getIntervals() {
    return intervals;
  }

  public ObjectNode getFilter() {
    return filter;
  }

  public BigInteger getOffset() {
    return offset;
  }

  public int getLimit() {
    return limit;
  }
}
