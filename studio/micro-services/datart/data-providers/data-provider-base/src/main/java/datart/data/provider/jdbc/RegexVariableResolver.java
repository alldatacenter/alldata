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

import datart.core.base.consts.Const;
import datart.core.data.provider.ScriptVariable;
import datart.data.provider.calcite.SqlNodeUtils;
import datart.data.provider.script.VariablePlaceholder;
import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexVariableResolver {

    public static final String REG_VARIABLE_EXPRESSION_TEMPLATE = "\\S+\\s*(IN|NOT\\s+IN|IS\\s+NULL|NOT\\s+NULL|LIKE|NOT\\s+LIKE|EXISTS|>|<|=|!=|<>|>=|<=){1}\\s*\\S*\\({0,1}(%s){1}\\){0,1}";

    public static List<VariablePlaceholder> resolve(SqlDialect sqlDialect, String srcSql, Map<String, ScriptVariable> variableMap) {

        if (StringUtils.isBlank(srcSql) || CollectionUtils.isEmpty(variableMap)) {
            return Collections.emptyList();
        }

        Matcher matcher = Const.VARIABLE_PATTERN.matcher(srcSql);
        Map<String, ScriptVariable> variablePlaceholderMap = new HashMap<>();
        while (matcher.find()) {
            String group = matcher.group();
            ScriptVariable scriptVariable = variableMap.get(group);
            if (scriptVariable != null) {
                variablePlaceholderMap.put(group, scriptVariable);
            }
        }
        if (variablePlaceholderMap.isEmpty()) {
            return Collections.emptyList();
        }


        List<VariablePlaceholder> placeholders = new LinkedList<>();
        for (Map.Entry<String, ScriptVariable> entry : variablePlaceholderMap.entrySet()) {
            placeholders.addAll(createPlaceholder(sqlDialect, srcSql, entry.getKey(), entry.getValue()));
        }
        return placeholders;

    }

    private static List<VariablePlaceholder> createPlaceholder(SqlDialect sqlDialect, String sql, String variableFragment, ScriptVariable variable) {

        List<VariablePlaceholder> placeholders = new LinkedList<>();

        List<String> variableExpressions = tryMatchVariableExpression(sql, variableFragment);

        if (!CollectionUtils.isEmpty(variableExpressions)) {
            for (String expression : variableExpressions) {
                SqlCall sqlCall = parseAsSqlCall(expression, variableFragment);
                if (sqlCall != null) {
                    placeholders.add(new VariablePlaceholder(Collections.singletonList(variable), sqlDialect, sqlCall, expression));
                } else {
                    placeholders.add(new SimpleVariablePlaceholder(variable, sqlDialect, variableFragment));
                }
            }
        } else {
            placeholders.add(new SimpleVariablePlaceholder(variable, sqlDialect, variableFragment));
        }
        return placeholders;
    }


    private static List<String> tryMatchVariableExpression(String sql, String variableFragment) {
        String reg = String.format(REG_VARIABLE_EXPRESSION_TEMPLATE, variableFragment.replace("$", "\\$"));
        Pattern pattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(sql);

        List<String> expressions = new LinkedList<>();

        while (matcher.find()) {
            expressions.add(matcher.group());
        }
        return expressions;
    }

    public static SqlCall parseAsSqlCall(String variableExpression, String variableFragment) {
        if (StringUtils.isBlank(variableExpression)) {
            return null;
        }
        String arg = null;
        String val = null;
        List<SqlOperatorReg> opTypes = new ArrayList<>();
        for (SqlOperatorReg type : SqlOperatorReg.values()) {
            Matcher matcher = type.getPattern().matcher(variableExpression);
            if (matcher.find()) {
                if (matcher.groupCount() > 1) {
                    break;
                }
                String[] split = type.getReplace().split(variableExpression);
                if (split.length != 2) {
                    break;
                }
                if (!split[0].contains(Const.DEFAULT_VARIABLE_QUOTE)) {
                    val = split[0].trim();
                    arg = split[1].trim();
                } else {
                    val = split[1].trim();
                    arg = split[0].trim();
                }
                opTypes.add(type);
            }
        }
        if (opTypes.size() != 1 || !variableFragment.equalsIgnoreCase(arg)) {
            return null;
        }
        return createSqlCall(opTypes.get(0), val, arg);
    }

    private static SqlCall createSqlCall(SqlOperatorReg sqlOperator, String var1, String var2) {
        SqlOperator operator = null;
        switch (sqlOperator) {
            case EQ:
                operator = SqlStdOperatorTable.EQUALS;
                break;
            case GT:
                operator = SqlStdOperatorTable.GREATER_THAN;
                break;
            case IN:
                operator = SqlStdOperatorTable.IN;
                break;
            case LT:
                operator = SqlStdOperatorTable.LESS_THAN;
                break;
            case GTE:
                operator = SqlStdOperatorTable.GREATER_THAN_OR_EQUAL;
                break;
            case LTE:
                operator = SqlStdOperatorTable.LESS_THAN_OR_EQUAL;
                break;
            case NEQ:
                operator = SqlStdOperatorTable.NOT_EQUALS;
                break;
            case NIN:
                operator = SqlStdOperatorTable.NOT_IN;
                break;
            case LIKE:
                operator = SqlStdOperatorTable.LIKE;
                break;
            case UNLIKE:
                operator = SqlStdOperatorTable.NOT_LIKE;
                break;
            default:
        }
        if (operator == null) {
            return null;
        }
        return SqlNodeUtils.createSqlBasicCall(operator, Arrays.asList(SqlNodeUtils.createSqlIdentifier(var1), SqlNodeUtils.createSqlIdentifier(var2)));
    }

}
