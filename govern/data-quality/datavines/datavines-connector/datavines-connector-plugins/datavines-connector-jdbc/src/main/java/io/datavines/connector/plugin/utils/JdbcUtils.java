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
package io.datavines.connector.plugin.utils;

import io.datavines.connector.api.Dialect;
import io.datavines.connector.api.TypeConverter;
import io.datavines.connector.plugin.entity.JdbcOptions;
import io.datavines.connector.plugin.entity.StructField;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class JdbcUtils {

    public static boolean tableExists(Connection connection, JdbcOptions options, Dialect dialect) {
        PreparedStatement statement = null;
        try {
            statement = connection.prepareStatement(dialect.getTableExistsQuery(options.getTableName()));
            statement.setQueryTimeout(options.getQueryTimeout());
            statement.execute();
        } catch (SQLException e) {
            return false;
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (SQLException e){
                    log.error("close statement error : ", e);
                }
            }
        }

        return true;
    }

    public static String getCreateTableStatement(String table, List<StructField> fields, Dialect dialect, TypeConverter typeConverter) {
        if (CollectionUtils.isNotEmpty(fields)) {
            String columns = String.join(",", fields.stream().map(field -> {
                return dialect.quoteIdentifier(field.getName()) + " " + typeConverter.convertToOriginType(field.getDataType());
            }).collect(Collectors.toList()));

            return String.format("CREATE TABLE IF NOT EXISTS %s (%s)", table, columns);
        }

        return null;
    }

    public static String getInsertStatement(String table, List<StructField> fields, Dialect dialect) {
        if (CollectionUtils.isNotEmpty(fields)) {
            String columns = String.join(",", fields.stream().map(field -> {
                return dialect.quoteIdentifier(field.getName());
            }).collect(Collectors.toList()));

            String placeholders = String.join(",",fields.stream().map(field -> {
                return "?";
            }).collect(Collectors.toList()));

            return String.format("INSERT INTO %s (%s) VALUES (%s)", table, columns, placeholders);
        }

        return null;
    }

    public static List<StructField> getSchema(ResultSet resultSet, Dialect dialect, TypeConverter typeConverter) throws SQLException{
        ResultSetMetaData metaData =  resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        List<StructField> fields = new ArrayList<>(columnCount);
        for (int i=0; i<columnCount; i++) {
            String columnName = metaData.getColumnName(i+1);
            int type = metaData.getColumnType(i+1);
            String typeName = metaData.getColumnTypeName(i+1);
            int fieldSize = metaData.getPrecision(i + 1);
            int fieldScale = metaData.getScale(i + 1);
            boolean isSigned = false;
            try {
                isSigned = metaData.isSigned(i + 1);
            } catch (SQLException e) {
                log.error("isSigned method : {}", e);
            }
            boolean isNullable = metaData.isNullable(i + 1) != ResultSetMetaData.columnNoNulls;

            StructField field = new StructField();
            field.setName(columnName);
            field.setDataType(typeConverter.convert(typeName));
            field.setNullable(isNullable);
            field.setComment("");

            fields.add(field);
        }

        return fields;
    }

}
