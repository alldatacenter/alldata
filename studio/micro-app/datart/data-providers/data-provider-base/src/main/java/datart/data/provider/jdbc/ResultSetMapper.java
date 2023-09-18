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

package datart.data.provider.jdbc;

import datart.core.base.consts.ValueType;
import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ResultSetMapper {

    public static List<Column> getColumns(ResultSet rs) throws SQLException {
        ArrayList<Column> columns = new ArrayList<>();
        for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
            int columnType = rs.getMetaData().getColumnType(i);
            String columnName = rs.getMetaData().getColumnLabel(i);
            ValueType valueType = DataTypeUtils.jdbcType2DataType(columnType);
            columns.add(Column.of(valueType, columnName));
        }
        return columns;
    }

    public static Dataframe mapToTableData(ResultSet rs) throws SQLException {
        return mapToTableData(rs, Long.MAX_VALUE);
    }

    public static Dataframe mapToTableData(ResultSet rs, long count) throws SQLException {
        Dataframe dataframe = new Dataframe();
        List<Column> columns = getColumns(rs);
        ArrayList<List<Object>> rows = new ArrayList<>();
        int c = 0;
        while (rs.next()) {
            ArrayList<Object> row = new ArrayList<>();
            rows.add(row);
            for (int i = 1; i < columns.size() + 1; i++) {
                row.add(rs.getObject(i));
            }
            c++;
            if (c >= count) {
                break;
            }
        }
        dataframe.setColumns(columns);
        dataframe.setRows(rows);
        return dataframe;
    }

}
