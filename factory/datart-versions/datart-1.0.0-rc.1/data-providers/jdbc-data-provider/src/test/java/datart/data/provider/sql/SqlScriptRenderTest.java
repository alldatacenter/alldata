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

package datart.data.provider.sql;

import datart.core.base.exception.Exceptions;
import datart.core.data.provider.QueryScript;
import datart.data.provider.DataProviderTestApplication;
import datart.data.provider.base.DataProviderException;
import datart.data.provider.calcite.SqlFragment;
import datart.data.provider.jdbc.SqlScriptRender;
import datart.data.provider.script.SqlStringUtils;
import datart.data.provider.sql.common.ParamFactory;
import datart.data.provider.sql.entity.SqlTestEntity;
import datart.data.provider.sql.examples.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlBasicCall;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest(classes = DataProviderTestApplication.class)
@Slf4j
@Disabled
public class SqlScriptRenderTest {

    private static final String T = "DATART_VTABLE";

    @Test
    public void testNormalSqlTest() throws SqlParseException {
        validateTestSql(NormalSqlExamples.sqlList, false);
        log.info("NormalSqlScripts validate passed");
    }

    @Test
    public void testSqlWithExecParam() throws SqlParseException {
        validateTestSql(ExecParamSqlExamples.sqlList, false);
        log.info("SqlWithExecParamSqlScripts validate passed");
    }

    @Test
    public void testVariableSql() throws SqlParseException {
        validateTestSql(VariableSqlExamples.sqlList, false);
        log.info("VariableSqlScripts validate passed");
    }

    @Test
    public void testFallbackSql() throws SqlParseException {
        validateTestSql(FallbackSqlExamples.sqlList, false);
        log.info("FallbackSqlScripts validate passed");
    }

    @Test
    public void testForbiddenSql() throws SqlParseException {
        validateForbiddenSql(ForbiddenSqlExamples.sqlList, false);
        log.info("ForbiddenSqlScripts validate passed");
    }

    @Test
    public void testSpecialSql() throws SqlParseException {
        validateTestSql(SpecialSqlExamples.sqlList, true);
        log.info("SpecialSqlScripts validate passed");
    }

    private void validateTestSql(List<SqlTestEntity> list, boolean enableSpecialSql) throws SqlParseException {
        for (SqlTestEntity sqlTest : list) {
            QueryScript queryScript = ParamFactory.getQueryScriptExample(sqlTest.getSql());
            SqlScriptRender render = new SqlScriptRender(queryScript, sqlTest.getExecuteParam(), sqlTest.getSqlDialect(), enableSpecialSql);
            boolean withExecParam = sqlTest.getExecuteParam() != null;
            String parsedSql = render.render(withExecParam, false, false);
            if (sqlTest.getExecuteParam() == null) {
                sqlTest.setDesireSql(covertDesiredSql(sqlTest.getDesireSql(), sqlTest.getSqlDialect()));
            } else {
                parsedSql = SqlStringUtils.cleanupSql(parsedSql);
            }
            boolean result = parsedSql.equalsIgnoreCase(sqlTest.getDesireSql());
            if (!result) {
                Exceptions.msg("sql validate failed! \n" + sqlTest +
                        " the parsed sql: " + parsedSql);
            }
        }
    }

    private void validateForbiddenSql(List<SqlTestEntity> list, boolean enableSpecialSql) throws SqlParseException {
        for (SqlTestEntity sqlTest : list) {
            QueryScript queryScript = ParamFactory.getQueryScriptExample(sqlTest.getSql());
            SqlScriptRender render = new SqlScriptRender(queryScript, null, sqlTest.getSqlDialect(), enableSpecialSql);
            try {
                render.render(false, false, false);
            } catch (DataProviderException e) {
                if ("message.sql.op.forbidden".equals(e.getMessage())) {
                    continue;
                }
                Exceptions.e(e);
            }
            Exceptions.msg("The forbidden sql test passed, should be forbid, enableSpecialSqlState: " + enableSpecialSql + "\n" + sqlTest);
        }
    }

    private String covertDesiredSql(String sql, SqlDialect sqlDialect) {
        SqlBasicCall sqlBasicCall = new SqlBasicCall(SqlStdOperatorTable.AS
                , new SqlNode[]{new SqlFragment("SELECT *  FROM  ( " + sql + " )"), new SqlIdentifier(T, SqlParserPos.ZERO.withQuoting(true))}
                , SqlParserPos.ZERO);
        return sqlBasicCall.toSqlString(sqlDialect).getSql().trim();
    }

}
