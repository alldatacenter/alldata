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

package org.apache.inlong.sort.cdc.postgres.manager;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.inlong.sort.cdc.postgres.connection.PostgreSQLJdbcConnectionProvider;
import org.apache.inlong.sort.cdc.postgres.table.PostgreSQLDataType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JDBC query tool to query meta info or data from PostgreSQL Server
 */
public class PostgreSQLQueryVisitor implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(PostgreSQLQueryVisitor.class);

    private final PostgreSQLJdbcConnectionProvider jdbcConnProvider;

    public PostgreSQLQueryVisitor(PostgreSQLJdbcConnectionProvider jdbcConnProvider) {
        this.jdbcConnProvider = jdbcConnProvider;
    }

    /**
     * query PostgreSQL table meta data, such as column name, data type, constraint type, etc.
     *
     * @param schema
     * @param table
     * @return
     */
    public List<Map<String, Object>> getTableColumnsMetaData(String schema, String table) {
        try {
            String query = "SELECT ordinal_position, tab_columns.column_name, data_type, character_maximum_length,\n"
                    + "numeric_precision, is_nullable, tab_constraints.constraint_type,\n"
                    + "col_constraints.constraint_name,col_check_constraints.check_clause\n"
                    + "FROM information_schema.columns AS tab_columns\n"
                    + "LEFT OUTER JOIN\n"
                    + "information_schema.constraint_column_usage AS col_constraints\n"
                    + "ON tab_columns.table_name = col_constraints.table_name AND\n"
                    + "tab_columns.column_name = col_constraints.column_name\n"
                    + "LEFT OUTER JOIN\n"
                    + "information_schema.table_constraints AS tab_constraints\n"
                    + "ON tab_constraints.constraint_name = col_constraints.constraint_name\n"
                    + "LEFT OUTER JOIN\n"
                    + "information_schema.check_constraints AS col_check_constraints\n"
                    + "ON col_check_constraints.constraint_name = tab_constraints.constraint_name\n"
                    + "WHERE tab_columns.table_schema = ? \n"
                    + "AND tab_columns.table_name = ? \n"
                    + "ORDER BY ordinal_position";
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Executing query '%s'", query));
            }
            return executeQuery(query, schema, table);
        } catch (ClassNotFoundException se) {
            throw new IllegalArgumentException("Failed to find jdbc driver." + se.getMessage(), se);
        } catch (SQLException se) {
            throw new IllegalArgumentException("Failed to get table schema info from StarRocks. " + se.getMessage(),
                    se);
        }
    }

    /**
     * get a map which key is column name and value is {@link PostgreSQLDataType}
     *
     * @param schema
     * @param table
     * @return
     */
    public Map<String, PostgreSQLDataType> getFieldMapping(String schema, String table) {
        List<Map<String, Object>> columns = getTableColumnsMetaData(schema, table);

        Map<String, PostgreSQLDataType> mapping = new LinkedHashMap<>();
        for (Map<String, Object> column : columns) {
            mapping.put(column.get("COLUMN_NAME").toString(),
                    PostgreSQLDataType.fromString(column.get("DATA_TYPE").toString()));
        }

        return mapping;
    }

    /**
     * query PostgreSQL server version
     *
     * @return
     */
    public String getPostgreSQLVersion() {
        final String query = "select version() as ver;";
        List<Map<String, Object>> rows;
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Executing query '%s'", query));
            }
            rows = executeQuery(query);
            if (rows.isEmpty()) {
                return "";
            }
            String version = rows.get(0).get("ver").toString();
            LOG.info(String.format("PostgreSQL version: [%s].", version));
            return version;
        } catch (ClassNotFoundException se) {
            throw new IllegalArgumentException("Failed to find jdbc driver." + se.getMessage(), se);
        } catch (SQLException se) {
            throw new IllegalArgumentException("Failed to get PostgreSQL version. " + se.getMessage(), se);
        }
    }

    /**
     * execute sql and return result set
     *
     * @param query
     * @param args
     * @return
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private List<Map<String, Object>> executeQuery(String query, String... args)
            throws ClassNotFoundException, SQLException {
        PreparedStatement stmt = jdbcConnProvider.getConnection()
                .prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        for (int i = 0; i < args.length; i++) {
            stmt.setString(i + 1, args[i]);
        }
        ResultSet rs = stmt.executeQuery();
        rs.next();
        ResultSetMetaData meta = rs.getMetaData();
        int columns = meta.getColumnCount();
        List<Map<String, Object>> list = new ArrayList<>();
        int currRowIndex = rs.getRow();
        rs.beforeFirst();
        while (rs.next()) {
            Map<String, Object> row = new HashMap<>(columns);
            for (int i = 1; i <= columns; ++i) {
                row.put(meta.getColumnName(i), rs.getObject(i));
            }
            list.add(row);
        }
        rs.absolute(currRowIndex);
        rs.close();
        jdbcConnProvider.close();
        return list;
    }

    /**
     * execute sql query and return result count
     *
     * @param sql
     * @return
     */
    public Long getQueryCount(String sql) {
        Long count = 0L;
        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Executing query '%s'", sql));
            }
            List<Map<String, Object>> data = executeQuery(sql);
            Object opCount = data.get(0).values().stream().findFirst().orElse(null);
            if (null == opCount) {
                throw new RuntimeException("Faild to get data count from PostgreSQL. ");
            }
            count = (Long) opCount;
        } catch (ClassNotFoundException se) {
            throw new IllegalArgumentException("Failed to find jdbc driver." + se.getMessage(), se);
        } catch (SQLException se) {
            throw new IllegalArgumentException("Failed to get data count from PostgreSQL. " + se.getMessage(), se);
        }
        return count;
    }
}
