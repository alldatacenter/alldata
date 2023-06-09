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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.drill.exec.store.openTSDB.Constants.AGGREGATOR_PARAM;
import static org.apache.drill.exec.store.openTSDB.Constants.DEFAULT_TIME;
import static org.apache.drill.exec.store.openTSDB.Constants.METRIC_PARAM;
import static org.apache.drill.exec.store.openTSDB.Constants.SUM_AGGREGATOR;
import static org.apache.drill.exec.store.openTSDB.Constants.TIME_PARAM;
import static org.apache.drill.exec.store.openTSDB.Util.getValidTableName;
import static org.apache.drill.exec.store.openTSDB.client.Schema.DefaultColumns.AGGREGATED_VALUE;
import static org.apache.drill.exec.store.openTSDB.client.Schema.DefaultColumns.AGGREGATE_TAGS;
import static org.apache.drill.exec.store.openTSDB.client.Schema.DefaultColumns.METRIC;
import static org.apache.drill.exec.store.openTSDB.client.Schema.DefaultColumns.TIMESTAMP;

/**
 * Abstraction for representing structure of openTSDB table
 */
public class Schema {

  private static final Logger log =
      LoggerFactory.getLogger(Schema.class);

  private final List<ColumnDTO> columns = new ArrayList<>();
  private final Service db;
  private final String name;

  public Schema(Service db, String name) {
    this.db = db;
    this.name = name;
    setupStructure();
  }

  private void setupStructure() {
    columns.add(new ColumnDTO(METRIC.toString(), OpenTSDBTypes.STRING));
    columns.add(new ColumnDTO(AGGREGATE_TAGS.toString(), OpenTSDBTypes.STRING));
    columns.add(new ColumnDTO(TIMESTAMP.toString(), OpenTSDBTypes.TIMESTAMP));
    columns.add(new ColumnDTO(AGGREGATED_VALUE.toString(), OpenTSDBTypes.DOUBLE));
    columns.addAll(db.getUnfixedColumns(getParamsForQuery()));
  }

  /**
   * Return list with all columns names and its types
   *
   * @return List<ColumnDTO>
   */
  public List<ColumnDTO> getColumns() {
    return Collections.unmodifiableList(columns);
  }

  /**
   * Number of columns in table
   *
   * @return number of table columns
   */
  public int getColumnCount() {
    return columns.size();
  }

  /**
   * @param columnIndex index of required column in table
   * @return ColumnDTO
   */
  public ColumnDTO getColumnByIndex(int columnIndex) {
    return columns.get(columnIndex);
  }

  // Create map with required params, for querying metrics.
  // Without this params, we cannot make API request to db.
  private Map<String, String> getParamsForQuery() {
    HashMap<String, String> params = new HashMap<>();
    params.put(METRIC_PARAM, getValidTableName(name));
    params.put(AGGREGATOR_PARAM, SUM_AGGREGATOR);
    params.put(TIME_PARAM, DEFAULT_TIME);
    return params;
  }

  /**
   * Structure with constant openTSDB columns
   */
  enum DefaultColumns {

    METRIC("metric"),
    TIMESTAMP("timestamp"),
    AGGREGATE_TAGS("aggregate tags"),
    AGGREGATED_VALUE("aggregated value");

    private String columnName;

    DefaultColumns(String name) {
      this.columnName = name;
    }

    @Override
    public String toString() {
      return columnName;
    }
  }
}
