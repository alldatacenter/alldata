/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.inlong.sort.jdbc.dialect;

import org.apache.flink.connector.jdbc.internal.converter.JdbcRowConverter;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.RowType;
import org.apache.inlong.sort.jdbc.converter.clickhouse.ClickHouseRowConverter;
import org.apache.inlong.sort.jdbc.table.AbstractJdbcDialect;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.lang.String.format;

/**
 * JDBC dialect for ClickHouse SQL.
 */
public class ClickHouseDialect extends AbstractJdbcDialect {

    // Define MAX/MIN precision of TIMESTAMP type according to ClickHouse docs:
    // https://clickhouse.com/docs/zh/sql-reference/data-types/datetime64
    private static final int MAX_TIMESTAMP_PRECISION = 8;
    private static final int MIN_TIMESTAMP_PRECISION = 0;

    // Define MAX/MIN precision of DECIMAL type according to ClickHouse docs:
    // https://clickhouse.com/docs/zh/sql-reference/data-types/decimal/
    private static final int MAX_DECIMAL_PRECISION = 128;
    private static final int MIN_DECIMAL_PRECISION = 32;

    @Override
    public String dialectName() {
        return "ClickHouse";
    }

    @Override
    public boolean canHandle(String url) {
        return url.startsWith("jdbc:clickhouse:");
    }

    @Override
    public JdbcRowConverter getRowConverter(RowType rowType) {
        return new ClickHouseRowConverter(rowType);
    }

    @Override
    public String getLimitClause(long limit) {
        return "LIMIT " + limit;
    }

    @Override
    public Optional<String> defaultDriverName() {
        return Optional.of("ru.yandex.clickhouse.ClickHouseDriver");
    }

    @Override
    public String quoteIdentifier(String identifier) {
        return "`" + identifier + "`";
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

    /**
     * Defines the unsupported types for the dialect.
     *
     * @return a list of logical type roots.
     */
    public List<LogicalTypeRoot> unsupportedTypes() {
        // ClickHouse support data type in
        // https://clickhouse.com/docs/en/sql-reference/data-types/
        return Arrays.asList(
                LogicalTypeRoot.BINARY,
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

    /**
     * Get update one row statement by condition fields
     */
    @Override
    public String getUpdateStatement(
            String tableName, String[] fieldNames, String[] conditionFields) {
        String setClause =
                Arrays.stream(fieldNames)
                        .map(f -> format("%s = :%s", quoteIdentifier(f), f))
                        .collect(Collectors.joining(", "));
        String conditionClause =
                Arrays.stream(conditionFields)
                        .map(f -> format("%s = :%s", quoteIdentifier(f), f))
                        .collect(Collectors.joining(" AND "));
        return "ALTER TABLE "
                + quoteIdentifier(tableName)
                + " UPDATE "
                + setClause
                + " WHERE "
                + conditionClause;
    }

    /**
     * Get delete one row statement by condition fields
     */
    @Override
    public String getDeleteStatement(String tableName, String[] conditionFields) {
        String conditionClause =
                Arrays.stream(conditionFields)
                        .map(f -> format("%s = :%s", quoteIdentifier(f), f))
                        .collect(Collectors.joining(" AND "));
        return "ALTER TABLE " + quoteIdentifier(tableName) + " DELETE WHERE " + conditionClause;
    }
}
