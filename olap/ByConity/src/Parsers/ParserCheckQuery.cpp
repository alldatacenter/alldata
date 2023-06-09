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

#include <Parsers/ParserCheckQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ASTIdentifier.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ASTCheckQuery.h>
#include <Parsers/ParserPartition.h>


namespace DB
{

bool ParserCheckQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_check_table("CHECK TABLE");
    ParserKeyword s_partition("PARTITION");
    ParserToken s_dot(TokenType::Dot);

    ParserIdentifier table_parser;
    ParserPartition partition_parser(dt);

    ASTPtr table;
    ASTPtr database;

    if (!s_check_table.ignore(pos, expected))
        return false;
    if (!table_parser.parse(pos, database, expected))
        return false;

    auto query = std::make_shared<ASTCheckQuery>();
    if (s_dot.ignore(pos))
    {
        if (!table_parser.parse(pos, table, expected))
            return false;

        tryGetIdentifierNameInto(database, query->database);
        tryGetIdentifierNameInto(table, query->table);
    }
    else
    {
        table = database;
        tryGetIdentifierNameInto(table, query->table);
    }

    if (s_partition.ignore(pos, expected))
    {
        if (!partition_parser.parse(pos, query->partition, expected))
            return false;
    }

    node = query;
    return true;
}

}
