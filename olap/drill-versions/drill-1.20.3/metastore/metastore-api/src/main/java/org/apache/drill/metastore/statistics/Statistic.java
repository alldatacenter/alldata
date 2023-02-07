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

/*
 * Base Statistics class - all statistics classes should extend this class
 */
public interface Statistic {
  /*
   * The lifecycle states for statistics
   */
  enum State {INIT, CONFIG, MERGE, COMPLETE}

  long NO_COLUMN_STATS = -1;
  /*
   * List of statistics used in Drill.
   */
  String COLNAME = "column";
  String COLTYPE = "majortype";
  String SCHEMA = "schema";
  String COMPUTED = "computed";
  String ROWCOUNT = "rowcount";
  String NNROWCOUNT = "nonnullrowcount";
  String NDV = "approx_count_distinct";
  String HLL_MERGE = "hll_merge";
  String HLL = "hll";
  String AVG_WIDTH = "avg_width";
  String SUM_WIDTH = "sum_width";
  String CNT_DUPS = "approx_count_dups";
  String SUM_DUPS = "sum";
  String TDIGEST = "tdigest";
  String TDIGEST_MERGE = "tdigest_merge";
}
