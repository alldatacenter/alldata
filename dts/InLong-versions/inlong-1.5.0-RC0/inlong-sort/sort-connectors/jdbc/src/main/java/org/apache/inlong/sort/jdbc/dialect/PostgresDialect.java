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

package org.apache.inlong.sort.jdbc.dialect;

import org.apache.flink.connector.jdbc.internal.converter.JdbcRowConverter;
import org.apache.flink.connector.jdbc.internal.converter.PostgresRowConverter;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.RowType;
import org.apache.inlong.sort.jdbc.internal.JdbcMultiBatchingComm;
import org.apache.inlong.sort.jdbc.table.AbstractJdbcDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/** JDBC dialect for PostgreSQL. */
public class PostgresDialect extends AbstractJdbcDialect {

    private static final long serialVersionUID = 1L;

    private static final String QUERY_PRIMARY_KEY_SQL = "SELECT\n" +
            "\tstring_agg (DISTINCT t3.attname, ',') AS " + PRIMARY_KEY_COLUMN + ",\n" +
            "    \tt4.tablename AS tableName\n" +
            "FROM\n" +
            "\tpg_constraint t1\n" +
            "INNER JOIN pg_class t2 ON t1.conrelid = t2.oid\n" +
            "INNER JOIN pg_attribute t3 ON t3.attrelid = t2.oid\n" +
            "AND array_position (t1.conkey, t3.attnum) is not null\n" +
            "INNER JOIN pg_tables t4 on t4.tablename = t2.relname\n" +
            "INNER JOIN pg_index t5 ON t5.indrelid = t2.oid\n" +
            "AND t3.attnum = ANY (t5.indkey)\n" +
            "LEFT JOIN pg_description t6 on t6.objoid = t3.attrelid\n" +
            "and t6.objsubid = t3.attnum\n" +
            "WHERE\n" +
            "\tt1.contype = 'p'\n" +
            "AND length (t3.attname) > 0\n" +
            "AND t2.oid = ?::regclass\n" +
            "group by\n" +
            "\tt4.tablename";

    // Define MAX/MIN precision of TIMESTAMP type according to PostgreSQL docs:
    // https://www.postgresql.org/docs/12/datatype-datetime.html
    private static final int MAX_TIMESTAMP_PRECISION = 6;
    private static final int MIN_TIMESTAMP_PRECISION = 1;

    // Define MAX/MIN precision of DECIMAL type according to PostgreSQL docs:
    // https://www.postgresql.org/docs/12/datatype-numeric.html#DATATYPE-NUMERIC-DECIMAL
    private static final int MAX_DECIMAL_PRECISION = 1000;
    private static final int MIN_DECIMAL_PRECISION = 1;

    @Override
    public boolean canHandle(String url) {
        return url.startsWith("jdbc:postgresql:");
    }

    @Override
    public JdbcRowConverter getRowConverter(RowType rowType) {
        return new PostgresRowConverter(rowType);
    }

    @Override
    public String getLimitClause(long limit) {
        return "LIMIT " + limit;
    }

    @Override
    public Optional<String> defaultDriverName() {
        return Optional.of("org.postgresql.Driver");
    }

    /** Postgres upsert query. It use ON CONFLICT ... DO UPDATE SET.. to replace into Postgres. */
    @Override
    public Optional<String> getUpsertStatement(
            String tableName, String[] fieldNames, String[] uniqueKeyFields) {
        String uniqueColumns =
                Arrays.stream(uniqueKeyFields)
                        .map(this::quoteIdentifier)
                        .collect(Collectors.joining(", "));
        String updateClause =
                Arrays.stream(fieldNames)
                        .map(f -> quoteIdentifier(f) + "=EXCLUDED." + quoteIdentifier(f))
                        .collect(Collectors.joining(", "));
        return Optional.of(
                getInsertIntoStatement(tableName, fieldNames)
                        + " ON CONFLICT ("
                        + uniqueColumns
                        + ")"
                        + " DO UPDATE SET "
                        + updateClause);
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return identifier;
    }

    @Override
    public String dialectName() {
        return "PostgreSQL";
    }

    @Override
    public int maxDecimalPrecision() {
        return MAX_DECIMAL_PRECISION;
    }

    @Override
    public int minDecimalPrecision() {
        return MIN_DECIMAL_PRECISION;
    }

    @Override
    public int maxTimestampPrecision() {
        return MAX_TIMESTAMP_PRECISION;
    }

    @Override
    public int minTimestampPrecision() {
        return MIN_TIMESTAMP_PRECISION;
    }

    @Override
    public List<LogicalTypeRoot> unsupportedTypes() {
        // The data types used in PostgreSQL are list at:
        // https://www.postgresql.org/docs/12/datatype.html

        // TODO: We can't convert BINARY data type to
        // PrimitiveArrayTypeInfo.BYTE_PRIMITIVE_ARRAY_TYPE_INFO in
        // LegacyTypeInfoDataTypeConverter.
        return Arrays.asList(
                LogicalTypeRoot.BINARY,
                LogicalTypeRoot.TIMESTAMP_WITH_TIME_ZONE,
                LogicalTypeRoot.INTERVAL_YEAR_MONTH,
                LogicalTypeRoot.INTERVAL_DAY_TIME,
                LogicalTypeRoot.MULTISET,
                LogicalTypeRoot.MAP,
                LogicalTypeRoot.ROW,
                LogicalTypeRoot.DISTINCT_TYPE,
                LogicalTypeRoot.STRUCTURED_TYPE,
                LogicalTypeRoot.NULL,
                LogicalTypeRoot.RAW,
                LogicalTypeRoot.SYMBOL,
                LogicalTypeRoot.UNRESOLVED);
    }

    @Override
    public PreparedStatement setQueryPrimaryKeySql(Connection conn,
            String tableIdentifier) throws SQLException {
        PreparedStatement st = conn.prepareStatement(QUERY_PRIMARY_KEY_SQL);
        st.setString(1, JdbcMultiBatchingComm.getTableNameFromIdentifier(tableIdentifier));
        return st;
    }
}
