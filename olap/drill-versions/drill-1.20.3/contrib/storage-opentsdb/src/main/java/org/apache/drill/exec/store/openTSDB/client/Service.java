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
package org.apache.drill.exec.store.openTSDB.client;

import org.apache.drill.exec.store.openTSDB.dto.ColumnDTO;
import org.apache.drill.exec.store.openTSDB.dto.MetricDTO;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface Service {
  /**
   *
   * Used for getting all Metrics from openTSDB.
   * Must be present required params: metric, start, aggregator
   *
   * @param queryParam parameters for the API request
   * @return Set<MetricDTO> all metrics
   */
  Set<MetricDTO> getAllMetrics(Map<String, String> queryParam);

  /**
   *
   * Used for getting all metrics names from openTSDB
   *
   * @return Set<String> metric names
   */
  Set<String> getAllMetricNames();

  /**
   *
   * Used for getting all non fixed columns based on tags from openTSDB
   * Must be present required params: metric, start, aggregator
   *
   * @param queryParam parameters for the API request
   * @return List<ColumnDTO> columns based on tags
   */
  List<ColumnDTO> getUnfixedColumns(Map<String, String> queryParam);
}
