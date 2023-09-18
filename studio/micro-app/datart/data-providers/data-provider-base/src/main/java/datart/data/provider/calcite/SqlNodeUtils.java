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

import datart.core.base.exception.Exceptions;
import datart.core.data.provider.ScriptVariable;
import datart.core.data.provider.SingleTypedValue;
import datart.data.provider.calcite.custom.SqlSimpleStringLiteral;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.commons.collections4.CollectionUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class SqlNodeUtils {

    public static SqlBasicCall createSqlBasicCall(SqlOperator sqlOperator, List<SqlNode> sqlNodes) {
        if (sqlNodes == null) {
            return null;
        }
        return new SqlBasicCall(sqlOperator, sqlNodes.toArray(new SqlNode[0]), SqlParserPos.ZERO);
    }

    public static SqlBasicCall createSqlBasicCall(SqlOperator sqlOperator,
                                                  List<SqlNode> sqlNodes,
                                                  SqlLiteral functionQualifier) {
        return new SqlBasicCall(sqlOperator, sqlNodes.toArray(new SqlNode[0]),
                SqlParserPos.ZERO,
                false,
                functionQualifier);
    }

    public static SqlIdentifier createSqlIdentifier(String... names) {
        return new SqlIdentifier(Arrays.asList(names), SqlParserPos.ZERO);
    }

    public static SqlIdentifier createSqlIdentifier(String name, boolean addNamePrefix, String namePrefix) {
        List<String> nms;
        if (addNamePrefix) {
            nms = Arrays.asList(name, namePrefix);
        } else {
            nms = Collections.singletonList(name);
        }
        return new SqlIdentifier(nms, SqlParserPos.ZERO);
    }


    /**
     * create sql alias with quoting
     */
    public static SqlNode createAliasNode(SqlNode node, String alias) {
        return createSqlBasicCall(SqlStdOperatorTable.AS, Arrays.asList(node, new SqlIdentifier(alias, SqlParserPos.ZERO.withQuoting(true))));
    }

    public static SqlNode toSingleSqlLiteral(ScriptVariable variable, SqlParserPos sqlParserPos) {
        List<SqlNode> sqlLiterals = createSqlNodes(variable, sqlParserPos);
        if (sqlLiterals.size() == 1) {
            return sqlLiterals.get(0);
        } else {
            return new SqlNodeList(sqlLiterals, sqlParserPos);
        }
    }

    public static List<SqlNode> createSqlNodes(ScriptVariable variable, SqlParserPos sqlParserPos) {
        if (CollectionUtils.isEmpty(variable.getValues())) {
            return Collections.singletonList(SqlLiteral.createNull(sqlParserPos));
        }
        switch (variable.getValueType()) {
            case STRING:
                return variable.getValues().stream()
                        .map(SqlSimpleStringLiteral::new)
                        .collect(Collectors.toList());
            case NUMERIC:
                return variable.getValues().stream().map(v ->
                        SqlLiteral.createExactNumeric(v, sqlParserPos)).collect(Collectors.toList());
            case BOOLEAN:
                return variable.getValues().stream().map(v ->
                        SqlLiteral.createBoolean(Boolean.parseBoolean(v), sqlParserPos)).collect(Collectors.toList());
            case DATE:
                return variable.getValues().stream().map(v ->
                        createDateSqlNode(v, variable.getFormat()))
                        .collect(Collectors.toList());
            case FRAGMENT:
                return variable.getValues().stream().map(SqlFragment::new).collect(Collectors.toList());
            default:
                Exceptions.msg("error data type " + variable.getValueType());
        }
        return null;
    }

    public static SqlNode createSqlNode(SingleTypedValue value) {
        switch (value.getValueType()) {
            case STRING:
                return new SqlSimpleStringLiteral(value.getValue().toString());
            case NUMERIC:
                return SqlLiteral.createExactNumeric(value.getValue().toString(), SqlParserPos.ZERO);
            case BOOLEAN:
                return SqlLiteral.createBoolean(Boolean.parseBoolean(value.getValue().toString()), SqlParserPos.ZERO);
            case DATE:
                return createDateSqlNode(value.getValue().toString(), value.getFormat());
            case FRAGMENT:
                return new SqlFragment(value.getValue().toString());
            case IDENTIFIER:
                return createSqlIdentifier((String[]) value.getValue());
            default:
                Exceptions.msg("message.provider.sql.variable", value.getValueType().name());
        }
        return null;
    }

    private static SqlNode createDateSqlNode(String value, String format) {
        // After 1.0.0-RC.1, date type parameters are treated as strings directly
        return new SqlSimpleStringLiteral(value);
    }

    /**
     * SQL 输出时，字段名称要默认加上引号，否则对于特殊字段名称无法处理，以及pg数据库无法正常执行等问题。
     * 但是在oracle数据库中，加上引号的字段小写会导致列名无法识别的问题，此时需要用户SQL中使用大写列名，或者可在jdbc-driver文件中配置为默认不加引号。
     */
    public static String toSql(SqlNode sqlNode, SqlDialect dialect, boolean quoteIdentifiers) {
        return sqlNode.toSqlString(
                config -> config.withDialect(dialect)
                        .withQuoteAllIdentifiers(quoteIdentifiers)
                        .withAlwaysUseParentheses(false)
                        .withSelectListItemsOnSeparateLines(false)
                        .withUpdateSetListNewline(false)
                        .withIndentation(0)).getSql();
    }

}
