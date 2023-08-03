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

#include <Parsers/ParserUndropQuery.h>

#include <Parsers/ASTIdentifier.h>
#include <Parsers/ASTUndropQuery.h>
#include <Parsers/CommonParsers.h>
#include <Parsers/ExpressionElementParsers.h>
#include <IO/ReadHelpers.h>

namespace DB
{
namespace ErrorCodes
{
    extern const int SYNTAX_ERROR;
    extern const int LOGICAL_ERROR;
}

bool ParserUndropQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_undrop("UNDROP");
    ParserKeyword s_database("DATABASE");
    ParserKeyword s_table("TABLE");
    ParserKeyword s_uuid("WITH UUID");
    ParserToken s_dot(TokenType::Dot);
    ParserIdentifier name_p;

    if (!s_undrop.ignore(pos, expected))
        return false;

    ASTPtr database;
    ASTPtr table;
    ASTPtr uuid;

    if (s_database.ignore(pos, expected))
    {
        if (!name_p.parse(pos, database, expected))
            return false;
    }
    else if (s_table.ignore(pos, expected))
    {
        if (!name_p.parse(pos, table, expected))
            return false;

        if (s_dot.ignore(pos, expected))
        {
            database = table;
            if (!name_p.parse(pos, table, expected))
                return false;
        }
    }
    else
    {
        return false;
    }

    if (s_uuid.ignore(pos, expected))
    {
        if (!name_p.parse(pos, uuid, expected))
            return false;
    }

    auto query = std::make_shared<ASTUndropQuery>();
    node = query;
    tryGetIdentifierNameInto(database, query->database);
    tryGetIdentifierNameInto(table, query->table);
    if (uuid)
        query->uuid = stringToUUID(getIdentifierName(uuid));

    return true;
}

}
