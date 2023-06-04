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

package datart.data.provider.sql.common;

import com.google.common.collect.Lists;
import datart.core.base.PageInfo;
import datart.core.base.consts.ValueType;
import datart.core.base.consts.VariableTypeEnum;
import datart.core.common.UUIDGenerator;
import datart.core.data.provider.*;
import datart.core.data.provider.sql.AggregateOperator;
import datart.core.data.provider.sql.FunctionColumn;
import datart.core.data.provider.sql.GroupByOperator;
import datart.core.data.provider.sql.OrderOperator;
import org.apache.commons.compress.utils.Sets;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

public class ParamFactory {

    private final static List<ScriptVariable> variables = Lists.newArrayList(
            variable("部门", ValueType.STRING, VariableTypeEnum.PERMISSION, false, "", "销售部")
            , variable("age", ValueType.NUMERIC, VariableTypeEnum.QUERY, false, "", "20")
            , variable("max", ValueType.NUMERIC, VariableTypeEnum.QUERY, false, "", "100")
            , variable("min", ValueType.NUMERIC, VariableTypeEnum.QUERY, false, "", "0")
            , variable("str", ValueType.STRING, VariableTypeEnum.QUERY, false, "", "content")
            , variable("datetime", ValueType.STRING, VariableTypeEnum.QUERY, false, "", "2020-01-01 00:00:00")
            , variable("date", ValueType.DATE, VariableTypeEnum.QUERY, false, "yyyy-MM-dd HH:mm:ss", "2020-01-01 00:00:00")
            , variable("where", ValueType.FRAGMENT, VariableTypeEnum.QUERY, false, "", "1=1")
    );

    public static QueryScript getQueryScriptExample(String script) {
        return QueryScript.builder().script(script)
                .sourceId(UUIDGenerator.generate())
                .variables(new LinkedList<>(variables))
                .viewId(UUIDGenerator.generate())
                .test(false)
                .scriptType(ScriptType.SQL)
                .build();
    }

    public static ExecuteParam getExecuteScriptExample() {
        List<SelectColumn> columns = new ArrayList<>();
        columns.add(SelectColumn.of("name", "name"));
        columns.add(SelectColumn.of("age","age"));

        List<FunctionColumn> functionColumns = new ArrayList<>();
        FunctionColumn functionColumn = new FunctionColumn();
        functionColumn.setAlias("ageNum");
        functionColumn.setSnippet("MAX(age)");
        functionColumns.add(functionColumn);

        List<AggregateOperator> aggregateOperators = new ArrayList<>();
        AggregateOperator aggregateOperator = new AggregateOperator();
        aggregateOperator.setColumn("val");
        aggregateOperator.setSqlOperator(AggregateOperator.SqlOperator.SUM);
        aggregateOperator.setAlias("SUM(val)");
        aggregateOperators.add(aggregateOperator);

        List<GroupByOperator> groupByOperators = new ArrayList<>();
        GroupByOperator group = new GroupByOperator();
        group.setColumn("id");
        group.setAlias("id");
        groupByOperators.add(group);

        List<OrderOperator> orderOperators = new ArrayList<>();
        OrderOperator orderOperator = new OrderOperator();
        orderOperator.setColumn("age");
        orderOperator.setOperator(OrderOperator.SqlOperator.DESC);
        orderOperator.setAggOperator(AggregateOperator.SqlOperator.COUNT);
        orderOperators.add(orderOperator);

        PageInfo pageInfo = new PageInfo();
        pageInfo.setPageNo(1);
        pageInfo.setPageSize(100);
        pageInfo.setTotal(234);
        pageInfo.setCountTotal(false);

        return ExecuteParam.builder()
                .columns(columns)
                .functionColumns(functionColumns)
                .aggregators(aggregateOperators)
                .groups(groupByOperators)
                .orders(orderOperators)
                .includeColumns(new HashSet<>(columns))
                .pageInfo(pageInfo)
                .concurrencyOptimize(false)
                .serverAggregate(false)
                .build();
    }

    private static ScriptVariable variable(String name, ValueType type, VariableTypeEnum variableType, boolean expression, String fmt, String... values) {
        ScriptVariable scriptVariable = new ScriptVariable();
        scriptVariable.setName(name);
        scriptVariable.setValueType(type);
        scriptVariable.setType(variableType);
        scriptVariable.setExpression(expression);
        scriptVariable.setFormat(fmt);
        scriptVariable.setValues(Sets.newHashSet(values));
        return scriptVariable;
    }

}
