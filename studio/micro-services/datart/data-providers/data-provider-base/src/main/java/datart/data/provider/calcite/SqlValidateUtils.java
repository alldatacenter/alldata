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
package datart.data.provider.calcite;

import com.google.common.collect.Sets;
import datart.core.base.exception.Exceptions;
import datart.data.provider.base.DataProviderException;
import datart.data.provider.script.SqlStringUtils;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlKind;
import org.apache.calcite.sql.SqlNode;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;
import java.util.Set;

public class SqlValidateUtils {

    /**
     * SQL expressions that return bool values and can be replaced as 1=1 or 1=1 during processing
     */
    private static final Set<SqlKind> logicOperator = EnumSet.of(
            SqlKind.IN, SqlKind.NOT_IN,
            SqlKind.EQUALS, SqlKind.NOT_EQUALS,
            SqlKind.LESS_THAN, SqlKind.GREATER_THAN,
            SqlKind.GREATER_THAN_OR_EQUAL, SqlKind.LESS_THAN_OR_EQUAL,
            SqlKind.LIKE,
            SqlKind.BETWEEN);

    private static final Set<SqlKind> disabledSqlKind = EnumSet.of(
            SqlKind.INSERT, SqlKind.DELETE, SqlKind.UPDATE, SqlKind.MERGE,
            SqlKind.COMMIT, SqlKind.ROLLBACK, SqlKind.ALTER_SESSION,
            SqlKind.CREATE_SCHEMA, SqlKind.CREATE_FOREIGN_SCHEMA, SqlKind.DROP_SCHEMA,
            SqlKind.CREATE_TABLE, SqlKind.ALTER_TABLE, SqlKind.DROP_TABLE,
            SqlKind.CREATE_FUNCTION, SqlKind.DROP_FUNCTION,
            SqlKind.CREATE_VIEW, SqlKind.ALTER_VIEW, SqlKind.DROP_VIEW,
            SqlKind.CREATE_MATERIALIZED_VIEW, SqlKind.ALTER_MATERIALIZED_VIEW,
            SqlKind.DROP_MATERIALIZED_VIEW,
            SqlKind.CREATE_SEQUENCE, SqlKind.ALTER_SEQUENCE, SqlKind.DROP_SEQUENCE,
            SqlKind.CREATE_INDEX, SqlKind.ALTER_INDEX, SqlKind.DROP_INDEX,
            SqlKind.CREATE_TYPE, SqlKind.DROP_TYPE,
            SqlKind.SET_OPTION, SqlKind.OTHER_DDL
    );

    private static final Set<String> QUERY_SQL = Sets.newHashSet(
            "SELECT", "WITH"
    );

    private static final Set<String> DISABLED_SQL = Sets.newHashSet(
            "CREATE", "DROP", "ALTER", "COMMIT", "ROLLBACK", "INSERT", "DELETE", "UPDATE", "MERGE"
    );

    /**
     * Validate SqlNode. Only query statements can pass validation
     */
    public static boolean validateQuery(SqlNode sqlCall, boolean enableSpecialSQL) {
        // check select sql
        if (sqlCall.getKind().belongsTo(SqlKind.QUERY)) {
            return true;
        }
        // check dml or ddl
        if (sqlCall.getKind().belongsTo(disabledSqlKind)) {
            Exceptions.tr(DataProviderException.class, "message.sql.op.forbidden", sqlCall.getKind() + ":" + sqlCall);
        }
        // special sql
        if (!enableSpecialSQL) {
            Exceptions.tr(DataProviderException.class, "message.sql.op.forbidden", sqlCall.getKind() + ":" + sqlCall);
        }
        return false;
    }

    /**
     * filter DDL and DML sql operators
     * <p>
     * throw sql exception if sql is one kind of dml or ddl
     */
    public static boolean validateQuery(String sql, boolean enableSpecialSQL) {
        if (StringUtils.isBlank(sql)) {
            return false;
        }
        String firstWord = firstWord(sql);
        // check select sql
        if (QUERY_SQL.stream().anyMatch(item -> item.equalsIgnoreCase(firstWord))) {
            return true;
        }
        // check dml or ddl
        if (DISABLED_SQL.stream().anyMatch(item -> item.equalsIgnoreCase(firstWord))) {
            Exceptions.tr(DataProviderException.class, "message.sql.op.forbidden", sql);
        }
        // special sql
        if (!enableSpecialSQL) {
            Exceptions.tr(DataProviderException.class, "message.sql.op.forbidden", sql);
        }
        return false;

    }

    public static boolean isLogicExpressionSqlCall(SqlCall sqlCall) {
        return sqlCall.getOperator().getKind().belongsTo(logicOperator);
    }

    private static String firstWord(String src) {
        if (src == null) {
            return null;
        }
        src = SqlStringUtils.cleanupSql(src);
        return src.trim().split("\\s", 2)[0];
    }

}
