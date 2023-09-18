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

package datart.data.provider.script;

import com.google.common.collect.Iterables;
import datart.core.base.consts.ValueType;
import datart.core.base.exception.Exceptions;
import datart.core.common.ReflectUtils;
import datart.core.data.provider.ScriptVariable;
import datart.data.provider.base.DataProviderException;
import datart.data.provider.jdbc.SqlSplitter;
import jdk.nashorn.internal.parser.TokenType;
import org.apache.calcite.avatica.util.Quoting;
import org.apache.calcite.config.Lex;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.advise.SqlSimpleParser;
import org.apache.calcite.sql.parser.SqlParser;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlStringUtils {

    public static final String REG_SQL_SINGLE_LINE_COMMENT = "-{2,}.*([\r\n])";

    public static final String REG_SQL_MULTI_LINE_COMMENT = "/\\*+[\\s\\S]*\\*+/";

    public static final String REG_WITH_SQL_FRAGMENT = "((?i)WITH[\\s\\S]+(?i)AS?\\s*\\([\\s\\S]+\\))\\s*(?i)SELECT";

    /**
     * 替换脚本中的表达式类型变量
     *
     * @param sql       原始SQL
     * @param variables 变量
     * @return 替换后的SQL
     */
    public static String replaceFragmentVariables(String sql, List<ScriptVariable> variables) {
        if (CollectionUtils.isEmpty(variables)) {
            return sql;
        }
        for (ScriptVariable variable : variables) {
            if (ValueType.FRAGMENT.equals(variable.getValueType())) {
                int size = Iterables.size(variable.getValues());
                if (size != 1) {
                    Exceptions.tr(DataProviderException.class, "message.provider.variable.expression.size", size + ":" + variable.getValues());
                }
                sql = sql.replace(variable.getNameWithQuote(), Iterables.get(variable.getValues(), 0));
            }
        }
        return sql;
    }

    /**
     * 移除SQL末尾的分号
     *
     * @param sql 原始SQL
     * @return 移除末尾分号的SQL
     */
    public static String removeEndDelimiter(String sql) {
        if (StringUtils.isBlank(sql)) {
            return sql;
        }
        sql = sql.trim();
        sql = StringUtils.removeEnd(sql, SqlSplitter.DEFAULT_DELIMITER + "");
        sql = sql.trim();
        if (sql.endsWith(SqlSplitter.DEFAULT_DELIMITER + "")) {
            return removeEndDelimiter(sql);
        } else {
            return sql;
        }
    }

    public static String cleanupSql(String sql) {
        //sql = sql.replaceAll(REG_SQL_SINGLE_LINE_COMMENT, " ");
        //sql = sql.replaceAll(REG_SQL_MULTI_LINE_COMMENT, " ");
        sql = sql.replace(CharUtils.CR, CharUtils.toChar(" "));
        sql = sql.replace(CharUtils.LF, CharUtils.toChar(" "));
        return sql.trim();
    }

    public static String cleanupSqlComments(String sql, SqlDialect sqlDialect) {
        Quoting quoting = Lex.MYSQL.quoting;
        if (sqlDialect != null) {
            quoting = sqlDialect.configureParser(SqlParser.Config.DEFAULT).quoting();
        }
        List<SqlParserPos> posList = new ArrayList<>();

        SqlSimpleParser.Tokenizer tokenizer = new SqlSimpleParser.Tokenizer(sql, "", quoting);
        int currPos = 0;
        while (true) {
            SqlSimpleParser.Token token = tokenizer.nextToken();
            if (token == null) {
                break;
            } else {
                Object tokenType = ReflectUtils.getFieldValue(token, "type");
                Integer endIndex = (Integer) ReflectUtils.getFieldValue(tokenizer, "pos");
                if (tokenType.toString().equals(TokenType.COMMENT.name())) {
                    posList.add(new SqlParserPos(0, currPos, 0, endIndex));
                }
                currPos = endIndex;
            }
        }
        int removeLength = 0;
        for (SqlParserPos pos : posList) {
            String pattern = sql.substring(pos.getColumnNum() - removeLength, pos.getEndColumnNum() - removeLength);
            sql = StringUtils.replaceOnce(sql, pattern, "");
            removeLength = removeLength + pattern.length();
        }
        return sql.trim();
    }

    /**
     * 处理sql with语句
     *
     * @param sql
     * @return
     */
    public static String rebuildSqlWithFragment(String sql) {
        if (!sql.toLowerCase().startsWith("with")) {
            Matcher matcher = Pattern.compile(REG_WITH_SQL_FRAGMENT).matcher(sql);
            if (matcher.find()) {
                String withFragment = matcher.group();
                if (!StringUtils.isEmpty(withFragment)) {
                    if (withFragment.length() > 6) {
                        int lastSelectIndex = withFragment.length() - 6;
                        sql = sql.replace(withFragment, withFragment.substring(lastSelectIndex));
                        withFragment = withFragment.substring(0, lastSelectIndex);
                    }
                    String space = " ";
                    sql = withFragment + space + sql;
                    sql = sql.replaceAll(space + "{2,}", space);
                }
            }
        }
        return sql;
    }

    public static char[] findMissedParentheses(String str) {
        Stack<Integer> stack = new Stack<>();
        Stack<Integer> toRemove = new Stack<>();
        for (int i = 0; i < str.length(); i++) {
            char chr = str.charAt(i);
            if ('(' == chr) {
                stack.push(i);
            } else if (')' == chr) {
                if (stack.isEmpty()) {
                    toRemove.push(i);
                } else {
                    stack.pop();
                }
            }
        }
        while (!stack.isEmpty()) {
            toRemove.add(stack.pop());
        }
        if (toRemove.isEmpty()) {
            return new char[0];
        }
        char[] missedParentheses = new char[toRemove.size()];
        for (int i = 0; i < toRemove.size(); i++) {
            char c = str.charAt(toRemove.get(i));
            if (c == '(') {
                missedParentheses[i] = ')';
            } else {
                missedParentheses[i] = '(';
            }
        }
        return missedParentheses;
    }

}
