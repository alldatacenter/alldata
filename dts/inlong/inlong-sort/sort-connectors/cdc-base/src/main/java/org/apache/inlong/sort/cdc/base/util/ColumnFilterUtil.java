/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.cdc.base.util;

import io.debezium.config.Configuration;
import io.debezium.relational.ColumnFilterMode;
import io.debezium.relational.Table;
import io.debezium.relational.TableEditor;
import io.debezium.relational.Tables;
import io.debezium.relational.history.TableChanges;

import static io.debezium.relational.RelationalDatabaseConnectorConfig.COLUMN_BLACKLIST;
import static io.debezium.relational.RelationalDatabaseConnectorConfig.COLUMN_WHITELIST;
import static io.debezium.relational.RelationalDatabaseConnectorConfig.COLUMN_EXCLUDE_LIST;
import static io.debezium.relational.RelationalDatabaseConnectorConfig.COLUMN_INCLUDE_LIST;

/**
 * Utility class to Combining column.exclude.list/column.blacklist, column.include.list/column.whitelist parameters
 * to generate new TableChange object.
 */
public class ColumnFilterUtil {

    public static Tables.ColumnNameFilter createColumnFilter(
            Configuration configuration, ColumnFilterMode columnFilterMode) {

        String columnExcludeList = configuration.getFallbackStringProperty(COLUMN_EXCLUDE_LIST, COLUMN_BLACKLIST);
        String columnIncludeList = configuration.getFallbackStringProperty(COLUMN_INCLUDE_LIST, COLUMN_WHITELIST);

        Tables.ColumnNameFilter columnFilter;
        if (columnIncludeList != null) {
            columnFilter = Tables.ColumnNameFilterFactory.createIncludeListFilter(columnIncludeList, columnFilterMode);
        } else {
            columnFilter = Tables.ColumnNameFilterFactory.createExcludeListFilter(columnExcludeList, columnFilterMode);
        }

        return columnFilter;
    }

    public static TableChanges.TableChange createTableChange(
            TableChanges.TableChange oldTableChange, Tables.ColumnNameFilter columnNameFilter) {
        Table table = oldTableChange.getTable();
        TableEditor tableEditor = table.edit();
        table.columns()
                .stream()
                .filter(column -> !columnNameFilter.matches(
                        table.id().catalog(),
                        table.id().schema(),
                        table.id().table(),
                        column.name()))
                .forEach(column -> tableEditor.removeColumn(column.name()));

        return new TableChanges.TableChange(oldTableChange.getType(), tableEditor.create());
    }
}
