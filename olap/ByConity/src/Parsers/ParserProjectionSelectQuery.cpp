/*
 * Copyright 2016-2023 ClickHouse, Inc.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * This file may have been modified by Bytedance Ltd. and/or its affiliates (“ Bytedance's Modifications”).
 * All Bytedance's Modifications are Copyright (2023) Bytedance Ltd. and/or its affiliates.
 */

#include <memory>
#include <Parsers/ASTFunction.h>
#include <Parsers/ASTProjectionSelectQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/IParserBase.h>
#include <Parsers/ParserProjectionSelectQuery.h>


namespace DB
{
bool ParserProjectionSelectQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    auto select_query = std::make_shared<ASTProjectionSelectQuery>();
    node = select_query;

    ParserKeyword s_with("WITH");
    ParserKeyword s_select("SELECT");
    ParserKeyword s_group_by("GROUP BY");
    ParserKeyword s_order_by("ORDER BY");

    ParserNotEmptyExpressionList exp_list_for_with_clause(false, dt);
    ParserNotEmptyExpressionList exp_list_for_select_clause(true, dt); /// Allows aliases without AS keyword.
    ParserExpression order_expression_p(dt);

    ASTPtr with_expression_list;
    ASTPtr select_expression_list;
    ASTPtr group_expression_list;
    ASTPtr order_expression;

    /// WITH expr list
    {
        if (s_with.ignore(pos, expected))
        {
            if (!exp_list_for_with_clause.parse(pos, with_expression_list, expected))
                return false;
        }
    }

    /// SELECT [DISTINCT] [TOP N [WITH TIES]] expr list
    {
        if (!s_select.ignore(pos, expected))
            return false;

        if (!exp_list_for_select_clause.parse(pos, select_expression_list, expected))
            return false;
    }

    // If group by is specified, AggregatingMergeTree engine is used, and the group by keys are implied to be order by keys
    if (s_group_by.ignore(pos, expected))
    {
        if (!ParserList(std::make_unique<ParserExpression>(dt), std::make_unique<ParserToken>(TokenType::Comma))
                 .parse(pos, group_expression_list, expected))
            return false;
    }

    if (s_order_by.ignore(pos, expected))
    {
        ASTPtr expr_list;
        if (!ParserList(std::make_unique<ParserExpression>(dt), std::make_unique<ParserToken>(TokenType::Comma)).parse(pos, expr_list, expected))
            return false;

        if (expr_list->children.size() == 1)
        {
            order_expression = expr_list->children.front();
        }
        else
        {
            auto function_node = std::make_shared<ASTFunction>();
            function_node->name = "tuple";
            function_node->arguments = expr_list;
            function_node->children.push_back(expr_list);
            order_expression = function_node;
        }
    }

    select_query->setExpression(ASTProjectionSelectQuery::Expression::WITH, std::move(with_expression_list));
    select_query->setExpression(ASTProjectionSelectQuery::Expression::SELECT, std::move(select_expression_list));
    select_query->setExpression(ASTProjectionSelectQuery::Expression::GROUP_BY, std::move(group_expression_list));
    select_query->setExpression(ASTProjectionSelectQuery::Expression::ORDER_BY, std::move(order_expression));
    return true;
}

}
