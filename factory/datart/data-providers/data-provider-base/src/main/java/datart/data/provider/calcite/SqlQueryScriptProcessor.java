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
import datart.core.data.provider.QueryScript;
import datart.core.data.provider.ScriptVariable;
import datart.data.provider.base.DataProviderException;
import datart.data.provider.freemarker.FreemarkerContext;
import datart.data.provider.jdbc.SqlSplitter;
import datart.data.provider.script.SqlStringUtils;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SqlQueryScriptProcessor implements QueryScriptProcessor {

    private static final String T = "DATART_VTABLE";

    private final boolean enableSpecialSQL;

    private final SqlDialect sqlDialect;

    public SqlQueryScriptProcessor(boolean enableSpecialSQL, SqlDialect sqlDialect) {
        this.enableSpecialSQL = enableSpecialSQL;
        this.sqlDialect = sqlDialect;
    }

    @Override
    public QueryScriptProcessResult process(QueryScript queryScript) {

        String script;

        //用freemarker处理脚本中的条件表达式
        Map<String, ?> dataMap = queryScript.getVariables()
                .stream()
                .collect(Collectors.toMap(ScriptVariable::getName,
                        variable -> {
                            if (CollectionUtils.isEmpty(variable.getValues())) {
                                return "";
                            } else if (variable.getValues().size() == 1) {
                                return variable.getValues().iterator().next();
                            } else return variable.getValues();
                        }));

        script = FreemarkerContext.process(queryScript.getScript(), dataMap);

        script = SqlStringUtils.cleanupSqlComments(script, sqlDialect);

        script = SqlStringUtils.replaceFragmentVariables(script, queryScript.getVariables());

        script = StringUtils.appendIfMissing(script, " ", " ");
        script = StringUtils.prependIfMissing(script, " ", " ");


        final String selectSql0 = parseSelectSql(script);

        if (StringUtils.isEmpty(selectSql0)) {
            Exceptions.tr(DataProviderException.class, "message.no.valid.sql");
        }

        String selectSql = SqlStringUtils.cleanupSql(selectSql0);


        if (StringUtils.isNotBlank(selectSql)) {
            selectSql = SqlStringUtils.removeEndDelimiter(selectSql);
        }
        selectSql = StringUtils.appendIfMissing(selectSql, " ", " ");
        selectSql = StringUtils.prependIfMissing(selectSql, " ", " ");

        SqlBasicCall sqlBasicCall = new SqlBasicCall(SqlStdOperatorTable.AS
                , new SqlNode[]{new SqlFragment("(" + selectSql + ")"), new SqlIdentifier(T, SqlParserPos.ZERO)}
                , SqlParserPos.ZERO);
        QueryScriptProcessResult result = new QueryScriptProcessResult();
        result.setFrom(sqlBasicCall);
        result.setTablePrefix(T);
        result.setWithDefaultPrefix(true);
        return result;
    }

    private String parseSelectSql(String script) {
        String selectSql = null;
        List<String> sqls = SqlSplitter.splitEscaped(script, SqlSplitter.DEFAULT_DELIMITER);
        for (String sql : sqls) {
            SqlNode sqlNode;
            try {
                sqlNode = SqlParserUtils.createParser(sql, sqlDialect).parseQuery();
            } catch (Exception e) {
                if (SqlValidateUtils.validateQuery(sql, enableSpecialSQL)) {
                    if (selectSql != null) {
                        Exceptions.tr(DataProviderException.class, "message.provider.sql.multi.query");
                    }
                    selectSql = sql;
                }
                continue;
            }
            if (SqlValidateUtils.validateQuery(sqlNode, enableSpecialSQL)) {
                if (selectSql != null) {
                    Exceptions.tr(DataProviderException.class, "message.provider.sql.multi.query");
                }
                selectSql = sql;
            }
        }

        if (selectSql == null) {
            selectSql = sqls.get(sqls.size() - 1);
        }

        return selectSql;
    }


}
