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

import com.alibaba.fastjson.JSON;
import datart.core.base.exception.Exceptions;
import datart.core.data.provider.QueryScript;
import datart.data.provider.script.JoinCondition;
import datart.data.provider.script.StructScript;
import datart.data.provider.script.TableJoin;
import org.apache.calcite.sql.*;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.calcite.sql.parser.SqlParserPos;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;

public class StructScriptProcessor implements QueryScriptProcessor {
    @Override
    public QueryScriptProcessResult process(QueryScript queryScript) {
        StructScript structScript = JSON.parseObject(queryScript.getScript(), StructScript.class);

        if (structScript.getTable() == null || structScript.getTable().length == 0) {
            Exceptions.msg("Join table can not be empty!");
        }

        SqlNode sqlJoin = SqlNodeUtils.createSqlIdentifier(structScript.getTable());

        QueryScriptProcessResult result = new QueryScriptProcessResult();
        result.setWithDefaultPrefix(false);
        if (CollectionUtils.isEmpty(structScript.getJoins())) {
            result.setFrom(sqlJoin);
            return result;
        }
        for (TableJoin tableJoin : structScript.getJoins()) {
            SqlNode conditionNode = null;
            if (!CollectionUtils.isEmpty(tableJoin.getConditions())) {
                for (JoinCondition joinCondition : tableJoin.getConditions()) {
                    if (!joinCondition.isValid()) {
                        continue;
                    }
                    SqlBasicCall condition = new SqlBasicCall(SqlStdOperatorTable.EQUALS
                            , new SqlNode[]{SqlNodeUtils.createSqlIdentifier(joinCondition.getLeft())
                            , SqlNodeUtils.createSqlIdentifier(joinCondition.getRight())}
                            , SqlParserPos.ZERO);
                    if (conditionNode == null) {
                        conditionNode = condition;
                    } else {
                        conditionNode = SqlNodeUtils.createSqlBasicCall(SqlStdOperatorTable.AND, Arrays.asList(conditionNode, condition));
                    }
                }
            }
            sqlJoin = new SqlJoin(SqlParserPos.ZERO
                    , sqlJoin
                    , SqlLiteral.createBoolean(false, SqlParserPos.ZERO)
                    , tableJoin.getJoinType().symbol(SqlParserPos.ZERO)
                    , SqlNodeUtils.createSqlIdentifier(tableJoin.getTable())
                    , SqlLiteral.createSymbol(JoinConditionType.ON, SqlParserPos.ZERO)
                    , conditionNode
            );
        }
        result.setFrom(sqlJoin);
        return result;
    }

}
