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
import datart.data.provider.calcite.SqlParserUtils;
import datart.data.provider.calcite.SqlVariableVisitor;
import datart.data.provider.script.VariablePlaceholder;
import lombok.extern.slf4j.Slf4j;
import org.apache.calcite.sql.SqlDialect;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParseException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
public class SqlParserVariableResolver {

    public static List<VariablePlaceholder> resolve(SqlDialect sqlDialect, String srcSql, Map<String, ScriptVariable> variableMap) throws SqlParseException {
        if (StringUtils.isBlank(srcSql) || CollectionUtils.isEmpty(variableMap)) {
            return Collections.emptyList();
        }
        SqlNode sqlNode = SqlParserUtils.createParser(srcSql, sqlDialect).parseQuery();
        SqlVariableVisitor visitor = new SqlVariableVisitor(sqlDialect, srcSql, Const.DEFAULT_VARIABLE_QUOTE, variableMap);
        sqlNode.accept(visitor);
        return visitor.getVariablePlaceholders();
    }

}
