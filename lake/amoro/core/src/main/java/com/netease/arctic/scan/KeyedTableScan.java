/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.scan;

import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.io.CloseableIterable;

/**
 * API for configuring a {@link com.netease.arctic.table.KeyedTable} scan.
 */
public interface KeyedTableScan {

  /**
   * Config this scan with filter by the {@link Expression}.
   *
   * @param expr a filter expression
   * @return scan based on this with results filtered by the expression
   */
  KeyedTableScan filter(Expression expr);

  /**
   * Plan the {@link CombinedScanTask tasks} for this scan.
   *
   * @return an Iterable of tasks for this scan
   */
  CloseableIterable<CombinedScanTask> planTasks();

  /**
   * To enable the feature of splitting tasks based on the delete ratio,
   * If the delete ratio under a node is less than splitTaskByDeleteRatio value,
   * the plan will be split into as many tasks as possible during execution.
   */
  KeyedTableScan enableSplitTaskByDeleteRatio(double splitTaskByDeleteRatio);

}
