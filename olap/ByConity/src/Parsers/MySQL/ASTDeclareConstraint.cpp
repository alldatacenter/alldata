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

#include <Parsers/MySQL/ASTDeclareConstraint.h>

#include <Parsers/ASTIdentifier.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ExpressionListParsers.h>

namespace DB
{

namespace MySQLParser
{

ASTPtr ASTDeclareConstraint::clone() const
{
    auto res = std::make_shared<ASTDeclareConstraint>(*this);
    res->children.clear();

    if (check_expression)
    {
        res->check_expression = check_expression->clone();
        res->children.emplace_back(res->check_expression);
    }

    return res;
}

bool ParserDeclareConstraint::parseImpl(IParser::Pos & pos, ASTPtr & node, Expected & expected)
{
    bool enforced = true;
    ASTPtr constraint_symbol;
    ASTPtr index_check_expression;
    ParserExpression p_expression(ParserSettings::CLICKHOUSE);

    if (ParserKeyword("CONSTRAINT").ignore(pos, expected))
    {
        if (!ParserKeyword("CHECK").checkWithoutMoving(pos, expected))
            ParserIdentifier().parse(pos, constraint_symbol, expected);
    }


    if (!ParserKeyword("CHECK").ignore(pos, expected))
        return false;

    if (!p_expression.parse(pos, index_check_expression, expected))
        return false;

    if (ParserKeyword("NOT").ignore(pos, expected))
    {
        if (!ParserKeyword("ENFORCED").ignore(pos, expected))
            return false;

        enforced = false;
    }
    else
    {
        enforced = true;
        ParserKeyword("ENFORCED").ignore(pos, expected);
    }

    auto declare_constraint = std::make_shared<ASTDeclareConstraint>();
    declare_constraint->enforced = enforced;
    declare_constraint->check_expression = index_check_expression;

    if (constraint_symbol)
        declare_constraint->constraint_name = constraint_symbol->as<ASTIdentifier>()->name();

    node = declare_constraint;
    return true;
}

}

}
