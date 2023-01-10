/*
 * Copyright (2022) Bytedance Ltd. and/or its affiliates
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <Parsers/ParserTEALimit.h>
#include <Parsers/ASTTEALimit.h>
#include <Parsers/ExpressionListParsers.h>
#include <Parsers/ExpressionElementParsers.h>

namespace DB
{

bool ParserTEALimitClause::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_tealimit("TEALIMIT");
    ParserKeyword s_group("GROUP");
    ParserKeyword s_order("ORDER");
    ParserToken s_comma(TokenType::Comma);
    ParserNumber num(dt);
    ParserNotEmptyExpressionList expr_list(false, dt);
    ParserOrderByExpressionList order_list(dt);

    if (!s_tealimit.ignore(pos, expected))
        return false;
    auto tea_limit = std::make_shared<ASTTEALimit>();

    if (!num.parse(pos, tea_limit->limit_value, expected ))
        return false;

    if (s_comma.ignore(pos, expected))
    {
        tea_limit->limit_offset = tea_limit->limit_value;
        if (!num.parse(pos, tea_limit->limit_value, expected))
            return false;
    }

    if (!s_group.ignore(pos, expected)) return false;
    if (!expr_list.parse(pos, tea_limit->group_expr_list, expected))
        return false;

    if (!s_order.ignore(pos, expected)) return false;
    if (!order_list.parse(pos, tea_limit->order_expr_list, expected))
        return false;

    if (tea_limit->limit_offset)
        tea_limit->children.push_back(tea_limit->limit_offset);
    if (tea_limit->limit_value)
        tea_limit->children.push_back(tea_limit->limit_value);
    if (tea_limit->group_expr_list)
        tea_limit->children.push_back(tea_limit->group_expr_list);
    if (tea_limit->order_expr_list)
        tea_limit->children.push_back(tea_limit->order_expr_list);

    node = std::move(tea_limit);
    return true;
}

}
