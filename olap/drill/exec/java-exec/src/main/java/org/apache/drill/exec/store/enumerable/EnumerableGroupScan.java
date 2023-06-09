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
package org.apache.drill.exec.store.enumerable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.physical.base.AbstractGroupScan;
import org.apache.drill.exec.physical.base.PhysicalOperator;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.base.SubScan;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public class EnumerableGroupScan extends AbstractGroupScan {
  private final String code;
  private final String schemaPath;
  private final Map<String, Integer> fieldsMap;
  private final List<SchemaPath> columns;
  private final double rows;
  private final TupleMetadata schema;
  private final ColumnConverterFactoryProvider converterFactoryProvider;

  @JsonCreator
  public EnumerableGroupScan(
      @JsonProperty("sql") String code,
      @JsonProperty("columns") List<SchemaPath> columns,
      @JsonProperty("fieldsMap") Map<String, Integer> fieldsMap,
      @JsonProperty("rows") double rows,
      @JsonProperty("schema") TupleMetadata schema,
      @JsonProperty("schemaPath") String schemaPath,
      @JsonProperty("converterFactoryProvider") ColumnConverterFactoryProvider converterFactoryProvider) {
    super("");
    this.code = code;
    this.columns = columns;
    this.fieldsMap = fieldsMap;
    this.rows = rows;
    this.schema = schema;
    this.schemaPath = schemaPath;
    this.converterFactoryProvider = converterFactoryProvider;
  }

  @Override
  public void applyAssignments(List<CoordinationProtos.DrillbitEndpoint> endpoints) {
  }

  @Override
  public SubScan getSpecificScan(int minorFragmentId) {
    return new EnumerableSubScan(code, columns, fieldsMap, schema, schemaPath, converterFactoryProvider);
  }

  @Override
  public int getMaxParallelizationWidth() {
    return 1;
  }

  @Override
  public ScanStats getScanStats() {
    return new ScanStats(
        ScanStats.GroupScanProperty.NO_EXACT_ROW_COUNT,
        (long) Math.max(rows, 1),
        1,
        1);
  }

  public String getCode() {
    return code;
  }

  @Override
  public List<SchemaPath> getColumns() {
    return columns;
  }

  public Map<String, Integer> getFieldsMap() {
    return fieldsMap;
  }

  public double getRows() {
    return rows;
  }

  public TupleMetadata getSchema() {
    return schema;
  }

  public String getSchemaPath() {
    return schemaPath;
  }

  public ColumnConverterFactoryProvider getConverterFactoryProvider() {
    return converterFactoryProvider;
  }

  @Override
  public String getDigest() {
    return toString();
  }

  @Override
  public PhysicalOperator getNewWithChildren(List<PhysicalOperator> children) {
    Preconditions.checkArgument(children.isEmpty());
    return new EnumerableGroupScan(code, columns, fieldsMap, rows, schema, schemaPath, converterFactoryProvider);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", EnumerableGroupScan.class.getSimpleName() + "[", "]")
        .add("code='" + code + "'")
        .add("columns=" + columns)
        .add("fieldsMap=" + fieldsMap)
        .add("rows=" + rows)
        .toString();
  }
}
