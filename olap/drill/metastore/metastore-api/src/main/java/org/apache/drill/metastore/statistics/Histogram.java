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
package org.apache.drill.metastore.statistics;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.calcite.rex.RexNode;

/**
 * A column specific histogram
 */
@JsonTypeInfo(
  use = JsonTypeInfo.Id.NAME,
  property = "category")
public interface Histogram {

  /**
   * For a filter condition, estimate the selectivity (matching rows/total rows) for this histogram.
   *
   * @return estimated selectivity or NULL if it could not be estimated for any reason
   */
  Double estimatedSelectivity(final RexNode filter, final long totalRowCount, final long ndv);
}
