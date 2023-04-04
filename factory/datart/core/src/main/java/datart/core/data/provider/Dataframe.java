/*
 * Datart
 * <p>
 * Copyright 2021
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package datart.core.data.provider;

import datart.core.base.PageInfo;
import datart.core.common.UUIDGenerator;
import lombok.Data;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;


@Data
public class Dataframe implements Serializable {

    private final String id;

    private String name;

    private String vizType;

    private String vizId;

    private List<Column> columns;

    private List<List<Object>> rows;

    private PageInfo pageInfo;

    private String script;

    public Dataframe() {
        this.id = "DF" + UUIDGenerator.generate();

    }

    public Dataframe(String id) {
        this.id = id;
    }

    public static Dataframe empty() {
        Dataframe dataframe = new Dataframe();
        dataframe.setColumns(Collections.emptyList());
        dataframe.setRows(Collections.emptyList());
        return dataframe;
    }

    // 按照指定的列定义，将数据集按照表名称进行分割，以还原原始表结构
    public Dataframes splitByTable(Map<String, Column> newSchema) {
        Map<Integer, String> tableColumnIndex = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            Column schemaColumn = newSchema.get(column.columnKey());
            tableColumnIndex.put(i, schemaColumn.tableName());
        }
        Map<String, List<List<Object>>> tableRows = newSchema
                .values()
                .stream()
                .map(Column::tableName)
                .distinct()
                .collect(Collectors.toMap(k -> k, v -> new ArrayList()));
        for (List<Object> row : rows) {
            int i = 0;
            Map<String, List<Object>> tableRowMap = new HashMap<>();
            for (Object item : row) {
                String tableName = tableColumnIndex.get(i);
                tableRowMap.computeIfAbsent(tableName, v -> new ArrayList<>()).add(item);
                i++;
            }
            for (String key : tableRowMap.keySet()) {
                tableRows.get(key).add(tableRowMap.get(key));
            }
        }
        Map<String, List<Column>> tableColumns = new HashMap<>();
        for (int i = 0; i < columns.size(); i++) {
            Column column = columns.get(i);
            Column newColumn = newSchema.get(column.columnKey());
            String tableName = newColumn.tableName();
            newColumn.setName(newColumn.columnName());
            tableColumns.computeIfAbsent(tableName, v -> new ArrayList<>())
                    .add(newColumn);
        }
        Dataframe[] dataframes = tableColumns.keySet().stream()
                .map(tableName -> {
                    Dataframe df = new Dataframe();
                    df.setName(tableName);
                    df.setColumns(tableColumns.get(tableName));
                    df.setRows(tableRows.get(tableName));
                    return df;
                }).toArray(Dataframe[]::new);
        return Dataframes.of(id, dataframes);
    }


}