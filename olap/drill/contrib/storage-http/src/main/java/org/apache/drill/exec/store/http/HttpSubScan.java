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
package org.apache.drill.exec.store.http;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.drill.common.PlanStringBuilder;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractBase;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.PhysicalVisitor;
import org.apache.drill.exec.physical.base.SubScan;

@JsonTypeName("http-sub-scan")
public class HttpSubScan extends AbstractBase implements SubScan {

  public static final String OPERATOR_TYPE = "HTTP_SUB_SCAN";

  private final HttpScanSpec tableSpec;
  private final List<SchemaPath> columns;
  private final Map<String, String> filters;
  private final int maxRecords;

  @JsonCreator
  public HttpSubScan(
    @JsonProperty("tableSpec") HttpScanSpec tableSpec,
    @JsonProperty("columns") List<SchemaPath> columns,
    @JsonProperty("filters") Map<String, String> filters,
    @JsonProperty("maxRecords") int maxRecords
    ) {
    super("user-if-needed");
    this.tableSpec = tableSpec;
    this.columns = columns;
    this.filters = filters;
    this.maxRecords = maxRecords;
  }

  @JsonProperty("tableSpec")
  public HttpScanSpec tableSpec() {
    return tableSpec;
  }

  @JsonProperty("columns")
  public List<SchemaPath> columns() {
    return columns;
  }

  @JsonProperty("filters")
  public Map<String, String> filters() {
    return filters;
  }

  @JsonProperty("maxRecords")
  public int maxRecords() {
    return maxRecords;
  }

 @Override
  public <T, X, E extends Throwable> T accept(
   PhysicalVisitor<T, X, E> physicalVisitor, X value) throws E {
    return physicalVisitor.visitSubScan(this, value);
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    return new HttpSubScan(tableSpec, columns, filters, maxRecords);
  }

  @Override
  @JsonIgnore
  public String getOperatorType() {
    return OPERATOR_TYPE;
  }

  @Override
  public Iterator<PhysicalOperator> iterator() {
    return Collections.emptyIterator();
  }

  @Override
  public String toString() {
    return new PlanStringBuilder(this)
      .field("tableSpec", tableSpec)
      .field("columns", columns)
      .field("filters", filters)
      .field("maxRecords", maxRecords)
      .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(tableSpec, columns, filters);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    HttpSubScan other = (HttpSubScan) obj;
    return Objects.equals(tableSpec, other.tableSpec)
      && Objects.equals(columns, other.columns)
      && Objects.equals(filters, other.filters)
      && Objects.equals(maxRecords, other.maxRecords);
  }
}
