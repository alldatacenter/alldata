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

#include <IO/Operators.h>
#include <Parsers/ParserShowWarehousesQuery.h>
#include <Parsers/ASTShowWarehousesQuery.h>
#include <Parsers/ExpressionElementParsers.h>
#include <Parsers/ASTLiteral.h>
#include <Parsers/CommonParsers.h>

namespace DB
{

bool ParserShowWarehousesQuery::parseImpl(Pos & pos, ASTPtr & node, Expected & expected)
{
    ParserKeyword s_like("LIKE");
    if (!ParserKeyword{"SHOW WAREHOUSES"}.ignore(pos, expected))
        return false;
    ParserStringLiteral like_p;
    ASTPtr like;

    if (s_like.ignore(pos, expected))
    {
        if (!like_p.parse(pos, like, expected))
            return false;
    }


    auto query = std::make_shared<ASTShowWarehousesQuery>();
    if (like)
       query->like = safeGet<const String &>(like->as<ASTLiteral &>().value);
    node = query;
    return true;
}

}
