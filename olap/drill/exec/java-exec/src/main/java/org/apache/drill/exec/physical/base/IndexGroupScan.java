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
package org.apache.drill.exec.physical.base;

import com.fasterxml.jackson.annotation.JsonIgnore;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rex.RexNode;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.planner.index.Statistics;


import java.util.List;

/**
 * An IndexGroupScan operator represents the scan associated with an Index.
 */
public interface IndexGroupScan extends GroupScan {

  /**
   * Get the column ordinal of the rowkey column from the output schema of the IndexGroupScan
   * @return column ordinal number
   */
  @JsonIgnore
  int getRowKeyOrdinal();

  /**
   * Set the artificial row count after applying the {@link RexNode} condition
   * Mainly used for debugging
   * @param condition filter to apply
   * @param count right index row count
   * @param capRowCount row count limit
   */
  @JsonIgnore
  void setRowCount(RexNode condition, double count, double capRowCount);

  /**
   * Get the row count after applying the {@link RexNode} condition
   * @param condition filter to apply
   * @param scanRel the current scan rel
   * @return row count post filtering
   */
  @JsonIgnore
  double getRowCount(RexNode condition, RelNode scanRel);

  /**
   * Set the statistics for {@link IndexGroupScan}
   * @param statistics which is necessary for index planning
   */
  @JsonIgnore
  void setStatistics(Statistics statistics);

  @JsonIgnore
  void setColumns(List<SchemaPath> columns);

  @JsonIgnore
  List<SchemaPath> getColumns();

  @JsonIgnore
  void setParallelizationWidth(int width);
}
