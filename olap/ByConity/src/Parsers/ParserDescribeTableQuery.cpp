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

#include <Parsers/TablePropertiesQueriesASTs.h>

#include <Parsers/CommonParsers.h>
#include <Parsers/ParserDescribeTableQuery.h>
#include <Parsers/ParserTablesInSelectQuery.h>

#include <Common/typeid_cast.h>


namespace DB
{


bool ParserDescribeTableQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_describe("DESCRIBE");
    ParserKeyword s_desc("DESC");
    ParserKeyword s_table("TABLE");
    ParserToken s_dot(TokenType::Dot);
    ParserIdentifier name_p;

    ASTPtr database;
    ASTPtr table;

    if (!s_describe.ignore(pos, expected) && !s_desc.ignore(pos, expected))
        return false;

    auto query = std::make_shared<ASTDescribeQuery>();

    s_table.ignore(pos, expected);

    ASTPtr table_expression;
    if (!ParserTableExpression(dt).parse(pos, table_expression, expected))
        return false;

    query->table_expression = table_expression;

    node = query;

    return true;
}


}
