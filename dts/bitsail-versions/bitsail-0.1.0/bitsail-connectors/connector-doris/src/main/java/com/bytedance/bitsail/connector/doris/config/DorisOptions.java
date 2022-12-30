/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.connector.doris.config;

import com.bytedance.bitsail.common.model.ColumnInfo;
import com.bytedance.bitsail.connector.doris.partition.DorisPartition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.List;

/**
 * Options for the Doris connector.
 */
@Builder
@AllArgsConstructor
@Data
public class DorisOptions implements Serializable {

  private static final long serialVersionUID = 1L;

  private String databaseName;

  private String tableName;

  private String tmpTableName;

  /**
   * Doris supports AGGREGATE, DUPLICATE and UNIQUE model.<br/>
   *
   * Currently, we only support UNIQUE table writer.
   */
  private TableModel tableModel;

  private String fieldDelimiter;

  private String lineDelimiter;

  private List<ColumnInfo> columnInfos;

  private String feNodes;

  private boolean tableHasPartitions;

  private List<DorisPartition> partitions;

  private String mysqlNodes;

  private String username;

  private String password;

  private DorisOptions.LOAD_CONTENT_TYPE loadDataFormat;

  public enum LOAD_CONTENT_TYPE {
    JSON,
    CSV
  }

  /**
   * Doris Table Model
   */
  public enum TableModel {
    /**
     * Doris AGGREGATE Table
     */
    AGGREGATE("AGGREGATE"),

    /**
     * Doris UNIQUE Table
     */
    UNIQUE("UNIQUE"),

    /**
     * Doris DUPLICATE Table
     */
    DUPLICATE("DUPLICATE");

    private final String model;

    TableModel(String model) {
      this.model = model;
    }

    public String getModel() {
      return this.model;
    }

    public TableModel of(@NonNull String model) {

      // compatible with internal doris-client
      if (StringUtils.equalsIgnoreCase(model, "UNI")) {
        return UNIQUE;
      }
      return TableModel.valueOf(model.toUpperCase());
    }
  }
}


