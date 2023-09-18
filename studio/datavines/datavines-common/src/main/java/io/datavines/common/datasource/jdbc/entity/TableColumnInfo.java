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

package io.datavines.common.datasource.jdbc.entity;

import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

@Data
public class TableColumnInfo {

    private String database;

    private String table;

    private List<String> primaryKeys;

    private List<ColumnInfo> columns;

    public TableColumnInfo(String table, List<String> primaryKeys, List<ColumnInfo> columns) {
        this(null, table, primaryKeys, columns);
    }

    public TableColumnInfo(String database, String table, List<String> primaryKeys, List<ColumnInfo> columns) {
        this.database = database;
        this.table = table;
        this.primaryKeys = primaryKeys;
        this.columns = columns;

        if (CollectionUtils.isNotEmpty(columns) && CollectionUtils.isNotEmpty(primaryKeys)) {
            columns.forEach(columnInfo -> {
                if (primaryKeys.contains(columnInfo.getName())) {
                    columnInfo.setPrimaryKey(true);
                }
            });
        }
    }
}
