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

import org.apache.commons.lang3.StringUtils;
import org.apache.flink.connector.jdbc.internal.converter.JdbcRowConverter;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.RowType;
import org.apache.inlong.sort.jdbc.converter.sqlserver.SqlServerRowConvert;
import org.apache.inlong.sort.jdbc.table.AbstractJdbcDialect;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JDBC dialect for SqlServerDialect.
 */
public class SqlServerDialect extends AbstractJdbcDialect {

    private static final long serialVersionUID = 5401176426209040158L;

    // Note:The timestamp syntax is deprecated. This feature will be removed
    // in a future version of Microsoft SQL Server. Avoid using this feature
    // in new development work, and plan to modify applications that currently
    // use this feature.
    // https://docs.microsoft.com/en-us/sql/t-sql/data-types/rowversion-transact-sql?redirectedfrom=MSDN&view=sql-server-ver15
    // Define MAX/MIN precision of TIMESTAMP type according to SqlServer docs:
    // https://docs.microsoft.com/en-us/sql/t-sql/data-types/data-types-transact-sql?view=sql-server-ver15
    private static final int MAX_TIMESTAMP_PRECISION = 3;
    private static final int MIN_TIMESTAMP_PRECISION = 0;

    // Define MAX/MIN precision of DECIMAL type according to SqlServer docs:
    // https://docs.microsoft.com/en-us/sql/t-sql/data-types/decimal-and-numeric-transact-sql?view=sql-server-ver15
    private static final int MAX_DECIMAL_PRECISION = 38;
    private static final int MIN_DECIMAL_PRECISION = 1;

    @Override
    public String dialectName() {
        return "SqlServer";
    }

    @Override
    public boolean canHandle(String url) {
        return url.startsWith("jdbc:sqlserver:");
    }

    @Override
    public JdbcRowConverter getRowConverter(RowType rowType) {
        return new SqlServerRowConvert(rowType);
    }

    @Override
    public String getLimitClause(long limit) {
        return "LIMIT " + limit;
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
        // The data types used in Mysql are list at:
        // https://docs.microsoft.com/en-us/sql/t-sql/data-types/data-types-transact-sql?view=sql-server-ver15
        return Arrays.asList(
                LogicalTypeRoot.TIMESTAMP_WITH_LOCAL_TIME_ZONE,
                LogicalTypeRoot.TIMESTAMP_WITH_TIME_ZONE,
                LogicalTypeRoot.INTERVAL_YEAR_MONTH,
                LogicalTypeRoot.INTERVAL_DAY_TIME,
                LogicalTypeRoot.ARRAY,
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
    public Optional<String> defaultDriverName() {
        return Optional.of("com.microsoft.sqlserver.jdbc.SQLServerDriver");
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return identifier;
    }

    /**
     * SqlServer merge data.
     * https://docs.microsoft.com/en-us/sql/t-sql/statements/merge-transact-sql?view=sql-server-ver15
     */
    @Override
    public Optional<String> getUpsertStatement(
            String tableName, String[] fieldNames, String[] uniqueKeyFields) {
        StringBuilder sb = new StringBuilder();
        sb.append("MERGE INTO ")
                .append(tableName)
                .append(" T1 USING (")
                .append(buildQueryStatement(fieldNames))
                .append(") T2 ON (")
                .append(buildConditions(uniqueKeyFields))
                .append(") ");
        String updateSql = buildUpdateConnection(fieldNames, uniqueKeyFields);
        if (StringUtils.isNotEmpty(updateSql)) {
            sb.append(" WHEN MATCHED THEN UPDATE SET ")
                    .append(updateSql);
        }
        sb.append(" WHEN NOT MATCHED THEN " + "INSERT (")
                .append(
                        Arrays
                                .stream(fieldNames)
                                .map(this::quoteIdentifier)
                                .collect(Collectors.joining(",")))
                .append(") VALUES (")
                .append(
                        Arrays
                                .stream(fieldNames)
                                .map(col -> "T2." + quoteIdentifier(col))
                                .collect(Collectors.joining(",")))
                .append(");");

        return Optional.of(sb.toString());
    }

    private String buildQueryStatement(String[] column) {
        StringBuilder sb = new StringBuilder("SELECT ");
        String collect =
                Arrays
                        .stream(column)
                        .map(col -> " :".concat(col).concat(" ") + quoteIdentifier(col))
                        .collect(Collectors.joining(", "));
        sb.append(collect);
        return sb.toString();
    }

    private String buildConditions(String[] uniqueKeyFields) {
        return Arrays
                .stream(uniqueKeyFields)
                .map(col -> "T1." + quoteIdentifier(col) + "=T2." + quoteIdentifier(col))
                .collect(Collectors.joining(","));
    }

    private String buildUpdateConnection(String[] fieldNames, String[] uniqueKeyFields) {
        List<String> uniqueKeyList = Arrays.asList(uniqueKeyFields);
        return Arrays
                .stream(fieldNames)
                .filter(col -> !uniqueKeyList.contains(col))
                .map(col -> quoteIdentifier("T1") + "." + quoteIdentifier(col)
                        + " =ISNULL(" + quoteIdentifier("T2") + "." + quoteIdentifier(col)
                        + "," + quoteIdentifier("T1") + "." + quoteIdentifier(col) + ")")
                .collect(Collectors.joining(","));
    }

    @Override
    public PreparedStatement setQueryPrimaryKeySql(Connection conn,
            String tableIdentifier) throws SQLException {
        return null;
    }

}
